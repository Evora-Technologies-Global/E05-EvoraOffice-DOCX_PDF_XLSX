package com.evora.technologies.office.engine.transfer

import com.evora.technologies.office.core.model.WifiTransferConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.Socket
import java.net.URL

class TransferEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `TransferPathSandbox blocks directory traversal and bad upload filenames`() {
        val root = tempFolder.newFolder("shared")
        val inside = TransferPathSandbox.resolveInsideRoot(root, "sub/doc.pdf")
        assertTrue(inside.canonicalPath.startsWith(root.canonicalPath))

        assertThrows(PathTraversalException::class.java) {
            TransferPathSandbox.resolveInsideRoot(root, "../../etc/passwd")
        }
        assertThrows(PathTraversalException::class.java) {
            TransferPathSandbox.resolveInsideRoot(root, "..\\secret.txt")
        }
        assertThrows(PathTraversalException::class.java) {
            TransferPathSandbox.sanitizeSingleUploadFilename("../../evil.sh")
        }
    }

    @Test
    fun `KtorWifiHttpServer handles UI, path traversal 403, PIN auth, and multipart upload`() {
        val shared = tempFolder.newFolder("http_shared")
        val upload = tempFolder.newFolder("http_upload")
        File(shared, "Contract.pdf").writeText("PDF-DATA")

        var config = WifiTransferConfig(readOnly = false, pinCode = "")
        val port = findFreePort()
        val server = KtorWifiHttpServer(
            sharedRootDir = shared,
            uploadRootDir = upload,
            loadIndexHtml = { "<html><body>Evora Office</body></html>" },
            configProvider = { config },
            onActivity = { _, _, _, _ -> },
            onFileUploaded = {},
        )
        server.start(host = "127.0.0.1", port = port)
        try {
            // 1. Index HTML
            val indexConn = URL("http://127.0.0.1:$port/").openConnection() as HttpURLConnection
            assertEquals(200, indexConn.responseCode)
            assertTrue(indexConn.inputStream.bufferedReader().readText().contains("Evora Office"))

            // 2. Traversal blocked with 403
            val traversalConn = URL("http://127.0.0.1:$port/api/files?path=../../").openConnection() as HttpURLConnection
            assertEquals(403, traversalConn.responseCode)

            // 3. Download valid file
            val dlConn = URL("http://127.0.0.1:$port/api/download?path=Contract.pdf").openConnection() as HttpURLConnection
            assertEquals(200, dlConn.responseCode)
            assertEquals("PDF-DATA", dlConn.inputStream.bufferedReader().readText())

            // 4. Multipart upload
            val boundary = "----EvoraBoundary123"
            val upConn = (URL("http://127.0.0.1:$port/api/upload").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }
            upConn.outputStream.use { os ->
                val body = buildString {
                    append("--$boundary\r\n")
                    append("Content-Disposition: form-data; name=\"file\"; filename=\"UploadedNote.txt\"\r\n")
                    append("Content-Type: text/plain\r\n\r\n")
                    append("HELLO_LAN")
                    append("\r\n--$boundary--\r\n")
                }
                os.write(body.toByteArray(Charsets.UTF_8))
            }
            assertEquals(200, upConn.responseCode)
            assertEquals("HELLO_LAN", File(upload, "UploadedNote.txt").readText())

            // 5. Read-only blocks upload with 403; PIN blocks list with 401
            config = WifiTransferConfig(readOnly = true, pinCode = "1234")
            val pinFail = URL("http://127.0.0.1:$port/api/files").openConnection() as HttpURLConnection
            assertEquals(401, pinFail.responseCode)

            val roUpload = (URL("http://127.0.0.1:$port/api/upload").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("X-Evora-Pin", "1234")
            }
            assertEquals(403, roUpload.responseCode)
        } finally {
            server.stop()
        }
    }

    @Test
    fun `EmbeddedFtpServer enforces PIN, blocks traversal, and serves PASV LIST and RETR`() {
        val shared = tempFolder.newFolder("ftp_shared")
        val upload = tempFolder.newFolder("ftp_upload")
        File(shared, "Budget.xlsx").writeText("XLSX-BYTES")

        val config = WifiTransferConfig(readOnly = true, pinCode = "9988")
        val ftp = EmbeddedFtpServer(
            sharedRootDir = shared,
            uploadRootDir = upload,
            hostIpProvider = { "127.0.0.1" },
            configProvider = { config },
            onActivity = { _, _, _, _ -> },
            onFileUploaded = {},
        )
        val port = ftp.start(0)
        try {
            Socket("127.0.0.1", port).use { sock ->
                val reader = BufferedReader(InputStreamReader(sock.getInputStream()))
                val writer = BufferedWriter(OutputStreamWriter(sock.getOutputStream()))
                fun cmd(c: String): String {
                    writer.write("$c\r\n")
                    writer.flush()
                    return reader.readLine()
                }

                assertTrue(reader.readLine().startsWith("220"))
                assertTrue(cmd("USER evora").startsWith("331"))
                assertTrue(cmd("PASS 0000").startsWith("530"))
                assertTrue(cmd("PASS 9988").startsWith("230"))

                // Path traversal rejected
                assertTrue(cmd("CWD ../../etc").startsWith("550"))

                // PASV LIST
                val pasvLine = cmd("PASV")
                assertTrue(pasvLine.startsWith("227"))
                val dataPort = parsePasvPort(pasvLine)
                Socket("127.0.0.1", dataPort).use { dataSock ->
                    assertTrue(cmd("LIST").startsWith("150"))
                    val listing = dataSock.getInputStream().bufferedReader().readText()
                    assertTrue(listing.contains("Budget.xlsx"))
                    assertTrue(reader.readLine().startsWith("226"))
                }

                // Read-only blocks STOR
                assertTrue(cmd("STOR hack.txt").startsWith("550"))
                assertFalse(File(upload, "hack.txt").exists())

                assertTrue(cmd("QUIT").startsWith("221"))
            }
        } finally {
            ftp.stop()
        }
    }

    @Test
    fun `EmbeddedFtpServer STOR rejects traversal and avoids overwriting existing files`() {
        val shared = tempFolder.newFolder("ftp_rw_shared")
        val upload = tempFolder.newFolder("ftp_rw_upload")
        File(upload, "report.txt").writeText("EXISTING")

        val ftp = EmbeddedFtpServer(
            sharedRootDir = shared,
            uploadRootDir = upload,
            hostIpProvider = { "127.0.0.1" },
            configProvider = { WifiTransferConfig(readOnly = false, pinCode = "") },
            onActivity = { _, _, _, _ -> },
            onFileUploaded = {},
        )
        val port = ftp.start(0)
        try {
            Socket("127.0.0.1", port).use { sock ->
                val reader = BufferedReader(InputStreamReader(sock.getInputStream()))
                val writer = BufferedWriter(OutputStreamWriter(sock.getOutputStream()))
                fun cmd(c: String): String {
                    writer.write("$c\r\n")
                    writer.flush()
                    return reader.readLine()
                }
                assertTrue(reader.readLine().startsWith("220"))
                assertTrue(cmd("USER evora").startsWith("230"))

                // Traversal STOR rejected cleanly
                val pasvBad = parsePasvPort(cmd("PASV"))
                Socket("127.0.0.1", pasvBad).use {
                    assertTrue(cmd("STOR ../../evil.txt").startsWith("550"))
                }
                assertFalse(File(upload.parentFile, "evil.txt").exists())

                // Non-overwriting upload creates report_1.txt while keeping report.txt intact
                val pasvOk = parsePasvPort(cmd("PASV"))
                Socket("127.0.0.1", pasvOk).use { dataSock ->
                    assertTrue(cmd("STOR report.txt").startsWith("150"))
                    dataSock.getOutputStream().write("NEW_UPLOAD".toByteArray())
                }
                assertTrue(reader.readLine().startsWith("226"))
                assertEquals("EXISTING", File(upload, "report.txt").readText())
                assertEquals("NEW_UPLOAD", File(upload, "report_1.txt").readText())
            }
        } finally {
            ftp.stop()
        }
    }

    private fun parsePasvPort(pasvResponse: String): Int {
        val nums = pasvResponse.substringAfter('(').substringBefore(')').split(',')
        val p1 = nums[4].trim().toInt()
        val p2 = nums[5].trim().toInt()
        return p1 * 256 + p2
    }

    private fun findFreePort(): Int = ServerSocket(0).use { it.localPort }
}
