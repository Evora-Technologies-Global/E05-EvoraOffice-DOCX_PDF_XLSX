package com.evora.technologies.office.engine.transfer

import com.evora.technologies.office.core.model.WifiTransferConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Inet4Address
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

/**
 * Lightweight RFC-959 FTP server supporting PASV/EPSV, LIST/NLST, RETR, STOR, MKD,
 * Read-Only mode, PIN authentication, and canonical [TransferPathSandbox] traversal checks.
 */
class EmbeddedFtpServer(
    private val sharedRootDir: File,
    private val uploadRootDir: File,
    private val hostIpProvider: () -> String,
    private val configProvider: () -> WifiTransferConfig,
    private val onActivity: (protocol: String, action: String, targetName: String, bytes: Long) -> Unit,
    private val onFileUploaded: (File) -> Unit,
) {
    private var controlSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private var scope: CoroutineScope? = null
    private val activeClientSockets = Collections.synchronizedSet(mutableSetOf<Socket>())

    fun start(port: Int): Int {
        stop()
        sharedRootDir.mkdirs()
        uploadRootDir.mkdirs()

        val socket = ServerSocket(port)
        controlSocket = socket
        val boundPort = socket.localPort
        val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = serverScope

        acceptJob = serverScope.launch {
            while (isActive && !socket.isClosed) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                activeClientSockets.add(client)
                launch {
                    handleClient(client)
                }
            }
        }
        return boundPort
    }

    fun stop() {
        runCatching { acceptJob?.cancel() }
        runCatching { controlSocket?.close() }
        synchronized(activeClientSockets) {
            activeClientSockets.toList().forEach { sock ->
                runCatching { sock.close() }
            }
            activeClientSockets.clear()
        }
        runCatching { scope?.cancel() }
        acceptJob = null
        controlSocket = null
        scope = null
    }

    private fun handleClient(client: Socket) {
        var passiveServer: ServerSocket? = null
        try {
            client.soTimeout = 120_000
            val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
            val writer = BufferedWriter(OutputStreamWriter(client.getOutputStream(), Charsets.UTF_8))

            fun send(line: String) {
                writer.write("$line\r\n")
                writer.flush()
            }

            val rootCanonical = sharedRootDir.canonicalFile
            var currentDir = rootCanonical
            var authenticated = configProvider().pinCode.isBlank()

            send("220 Evora Office FTP Ready")

            while (true) {
                val rawLine = reader.readLine() ?: break
                val parts = rawLine.trim().split(' ', limit = 2)
                val cmd = parts.firstOrNull()?.uppercase(Locale.US) ?: continue
                val arg = parts.getOrNull(1)?.trim().orEmpty()
                val cfg = configProvider()

                when (cmd) {
                    "USER" -> {
                        if (cfg.pinCode.isBlank()) {
                            authenticated = true
                            send("230 Logged in")
                        } else {
                            send("331 PIN required as password")
                        }
                    }
                    "PASS" -> {
                        val expected = cfg.pinCode.trim()
                        val matches = expected.isEmpty() || MessageDigest.isEqual(
                            arg.toByteArray(Charsets.UTF_8),
                            expected.toByteArray(Charsets.UTF_8),
                        )
                        if (matches) {
                            authenticated = true
                            send("230 Login successful")
                        } else {
                            authenticated = false
                            send("530 Invalid PIN")
                        }
                    }
                    "SYST" -> send("215 UNIX Type: L8")
                    "FEAT" -> send("211-Features:\r\n UTF8\r\n SIZE\r\n EPSV\r\n211 End")
                    "OPTS" -> send("200 Always UTF-8")
                    "TYPE" -> send("200 Type set to $arg")
                    "NOOP" -> send("200 OK")
                    "QUIT" -> {
                        send("221 Goodbye")
                        break
                    }
                    else -> {
                        if (!authenticated) {
                            send("530 Please authenticate with PASS <PIN>")
                            continue
                        }
                        when (cmd) {
                            "PWD", "XPWD" -> {
                                val rel = relativeFtpPath(rootCanonical, currentDir)
                                send("""257 "$rel" is current directory""")
                            }
                            "CWD" -> {
                                val target = resolveFtpTarget(rootCanonical, currentDir, arg)
                                if (target == null) {
                                    send("550 Access denied: invalid path")
                                } else if (!target.exists() || !target.isDirectory) {
                                    send("550 Directory not found")
                                } else {
                                    currentDir = target
                                    onActivity("FTP", "CWD", target.name, 0L)
                                    send("250 Directory changed")
                                }
                            }
                            "CDUP" -> {
                                val parent = currentDir.parentFile?.canonicalFile
                                if (parent != null && (parent.path == rootCanonical.path || parent.path.startsWith(rootCanonical.path + File.separator))) {
                                    currentDir = parent
                                } else {
                                    currentDir = rootCanonical
                                }
                                send("250 Directory changed")
                            }
                            "SIZE" -> {
                                val target = resolveFtpTarget(rootCanonical, currentDir, arg)
                                if (target != null && target.isFile) {
                                    send("213 ${target.length()}")
                                } else {
                                    send("550 File not found")
                                }
                            }
                            "PASV" -> {
                                runCatching { passiveServer?.close() }
                                val dataServer = ServerSocket(0)
                                passiveServer = dataServer
                                val p = dataServer.localPort
                                val ip = resolveClientReachableIp(client)
                                val ipCommas = ip.replace('.', ',')
                                send("227 Entering Passive Mode ($ipCommas,${p / 256},${p % 256})")
                            }
                            "EPSV" -> {
                                runCatching { passiveServer?.close() }
                                val dataServer = ServerSocket(0)
                                passiveServer = dataServer
                                send("229 Entering Extended Passive Mode (|||${dataServer.localPort}|)")
                            }
                            "LIST", "NLST" -> {
                                val dataServer = passiveServer
                                if (dataServer == null) {
                                    send("425 Use PASV or EPSV first")
                                    continue
                                }
                                val target = if (arg.isBlank() || arg.startsWith("-")) currentDir else resolveFtpTarget(rootCanonical, currentDir, arg)
                                if (target == null || !target.exists()) {
                                    send("550 Path unavailable")
                                    continue
                                }
                                send("150 Opening data connection")
                                try {
                                    dataServer.soTimeout = 15_000
                                    dataServer.accept().use { dataSocket ->
                                        val listFiles = if (target.isDirectory) {
                                            target.listFiles()?.filter { !it.name.startsWith(".") }.orEmpty()
                                        } else {
                                            listOf(target)
                                        }
                                        val listing = if (cmd == "NLST") {
                                            listFiles.joinToString("\r\n") { it.name } + if (listFiles.isNotEmpty()) "\r\n" else ""
                                        } else {
                                            formatUnixList(listFiles)
                                        }
                                        dataSocket.getOutputStream().write(listing.toByteArray(Charsets.UTF_8))
                                        dataSocket.getOutputStream().flush()
                                    }
                                    onActivity("FTP", "LIST", target.name, 0L)
                                    send("226 Transfer complete")
                                } finally {
                                    runCatching { dataServer.close() }
                                    passiveServer = null
                                }
                            }
                            "RETR" -> {
                                val dataServer = passiveServer
                                if (dataServer == null) {
                                    send("425 Use PASV or EPSV first")
                                    continue
                                }
                                val target = resolveFtpTarget(rootCanonical, currentDir, arg)
                                if (target == null) {
                                    send("550 Access denied")
                                    continue
                                }
                                if (!target.exists() || !target.isFile) {
                                    send("550 File not found")
                                    continue
                                }
                                send("150 Opening binary mode data connection")
                                try {
                                    dataServer.soTimeout = 15_000
                                    dataServer.accept().use { dataSocket ->
                                        FileInputStream(target).use { fis ->
                                            fis.copyTo(dataSocket.getOutputStream())
                                        }
                                        dataSocket.getOutputStream().flush()
                                    }
                                    onActivity("FTP", "DOWNLOAD", target.name, target.length())
                                    send("226 Transfer complete")
                                } finally {
                                    runCatching { dataServer.close() }
                                    passiveServer = null
                                }
                            }
                            "STOR" -> {
                                if (cfg.readOnly) {
                                    send("550 Read-Only mode enabled")
                                    continue
                                }
                                val dataServer = passiveServer
                                if (dataServer == null) {
                                    send("425 Use PASV or EPSV first")
                                    continue
                                }
                                val safeTarget = runCatching {
                                    val baseDir = when {
                                        arg.startsWith("/") -> rootCanonical
                                        currentDir.path == rootCanonical.path -> uploadRootDir.canonicalFile.apply { mkdirs() }
                                        else -> currentDir
                                    }
                                    val candidate = TransferPathSandbox.resolveInsideRoot(baseDir, arg.trimStart('/'))
                                    val safeLeaf = TransferPathSandbox.sanitizeSingleUploadFilename(candidate.name)
                                    val parent = (candidate.parentFile ?: baseDir).apply { mkdirs() }
                                    TransferPathSandbox.uniqueTargetFile(parent, safeLeaf)
                                }.getOrElse {
                                    runCatching { dataServer.close() }
                                    passiveServer = null
                                    send("550 Invalid target filename")
                                    continue
                                }
                                send("150 Ready to receive file")
                                var received = 0L
                                try {
                                    dataServer.soTimeout = 30_000
                                    dataServer.accept().use { dataSocket ->
                                        FileOutputStream(safeTarget).use { fos ->
                                            val buf = ByteArray(16 * 1024)
                                            val ins = dataSocket.getInputStream()
                                            while (true) {
                                                val r = ins.read(buf)
                                                if (r <= 0) break
                                                received += r
                                                if (received > MAX_UPLOAD_BYTES) {
                                                    throw IllegalArgumentException("Upload exceeds 512 MB limit")
                                                }
                                                fos.write(buf, 0, r)
                                            }
                                        }
                                    }
                                    onActivity("FTP", "UPLOAD", safeTarget.name, received)
                                    onFileUploaded(safeTarget)
                                    send("226 Upload complete")
                                } catch (_: IllegalArgumentException) {
                                    runCatching { safeTarget.delete() }
                                    send("552 Upload exceeds 512 MB limit")
                                } catch (_: Throwable) {
                                    runCatching { safeTarget.delete() }
                                    send("426 Connection closed; transfer aborted")
                                } finally {
                                    runCatching { dataServer.close() }
                                    passiveServer = null
                                }
                            }
                            "MKD", "XMKD" -> {
                                if (cfg.readOnly) {
                                    send("550 Read-Only mode enabled")
                                    continue
                                }
                                val target = resolveFtpTarget(rootCanonical, currentDir, arg)
                                if (target == null) {
                                    send("550 Access denied")
                                } else if (target.mkdirs() || target.isDirectory) {
                                    send("""257 "${relativeFtpPath(rootCanonical, target)}" created""")
                                } else {
                                    send("550 Unable to create directory")
                                }
                            }
                            else -> send("502 Command not implemented ($cmd)")
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            // Client closed or timed out
        } finally {
            activeClientSockets.remove(client)
            runCatching { passiveServer?.close() }
            runCatching { client.close() }
        }
    }

    companion object {
        private const val MAX_UPLOAD_BYTES = 512L * 1024L * 1024L
    }

    private fun resolveFtpTarget(rootCanonical: File, currentDir: File, arg: String): File? {
        if (arg.isBlank()) return currentDir
        val combined = if (arg.startsWith("/")) {
            arg.trimStart('/')
        } else {
            val relCurrent = relativeFtpPath(rootCanonical, currentDir).trimStart('/')
            if (relCurrent.isEmpty()) arg else "$relCurrent/$arg"
        }
        return runCatching { TransferPathSandbox.resolveInsideRoot(rootCanonical, combined) }.getOrNull()
    }

    private fun relativeFtpPath(rootCanonical: File, dir: File): String {
        val rel = dir.canonicalPath.removePrefix(rootCanonical.canonicalPath)
            .replace(File.separatorChar, '/')
        return if (rel.isEmpty()) "/" else if (rel.startsWith("/")) rel else "/$rel"
    }

    private fun resolveClientReachableIp(client: Socket): String {
        val local = client.localAddress
        if (local is Inet4Address && !local.isLoopbackAddress) {
            return local.hostAddress ?: hostIpProvider()
        }
        val host = hostIpProvider()
        return if (host.isBlank()) "127.0.0.1" else host
    }

    private fun formatUnixList(files: List<File>): String {
        val fmt = SimpleDateFormat("MMM dd HH:mm", Locale.US)
        return buildString {
            files.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
                .forEach { file ->
                    val type = if (file.isDirectory) 'd' else '-'
                    val size = if (file.isDirectory) 0L else file.length()
                    val date = fmt.format(Date(file.lastModified()))
                    append("${type}rw-r--r-- 1 evora evora $size $date ${file.name}\r\n")
                }
        }
    }
}
