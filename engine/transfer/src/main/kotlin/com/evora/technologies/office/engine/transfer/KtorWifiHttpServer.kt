package com.evora.technologies.office.engine.transfer

import com.evora.technologies.office.core.model.WifiTransferConfig
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.readAvailable
import java.io.File
import java.io.FileOutputStream

/**
 * Embedded Ktor CIO LAN HTTP server (07_TECH_SPEC.md §7.4 / T-019).
 * Serves offline drag-and-drop UI and validates all path/upload inputs against [TransferPathSandbox].
 */
class KtorWifiHttpServer(
    private val sharedRootDir: File,
    private val uploadRootDir: File,
    private val loadIndexHtml: () -> String,
    private val configProvider: () -> WifiTransferConfig,
    private val onActivity: (protocol: String, action: String, targetName: String, bytes: Long) -> Unit,
    private val onFileUploaded: (File) -> Unit,
) {
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

    fun start(host: String = "0.0.0.0", port: Int): Int {
        stop()
        sharedRootDir.mkdirs()
        uploadRootDir.mkdirs()

        val created = embeddedServer(CIO, port = port, host = host) {
            routing {
                get("/") {
                    onActivity("HTTP", "VIEW_UI", "index.html", 0L)
                    call.respondText(loadIndexHtml(), ContentType.Text.Html)
                }

                get("/api/files") {
                    val cfg = configProvider()
                    if (!isPinAuthorized(call, cfg)) {
                        call.respondText("PIN required or invalid", status = HttpStatusCode.Unauthorized)
                        return@get
                    }
                    val reqPath = call.request.queryParameters["path"]
                    val targetDir = try {
                        TransferPathSandbox.resolveInsideRoot(sharedRootDir, reqPath)
                    } catch (sec: PathTraversalException) {
                        call.respondText(sec.message ?: "Forbidden", status = HttpStatusCode.Forbidden)
                        return@get
                    }
                    if (!targetDir.exists() || !targetDir.isDirectory) {
                        call.respondText("Directory not found", status = HttpStatusCode.NotFound)
                        return@get
                    }
                    onActivity("HTTP", "LIST", targetDir.name, 0L)
                    val entries = targetDir.listFiles()
                        ?.filter { !it.name.startsWith(".") }
                        ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
                        .orEmpty()
                    val json = buildFilesJson(
                        root = sharedRootDir.canonicalFile,
                        entries = entries,
                        readOnly = cfg.readOnly,
                    )
                    call.respondText(json, ContentType.Application.Json)
                }

                get("/api/download") {
                    val cfg = configProvider()
                    if (!isPinAuthorized(call, cfg)) {
                        call.respondText("PIN required or invalid", status = HttpStatusCode.Unauthorized)
                        return@get
                    }
                    val reqPath = call.request.queryParameters["path"]
                    val targetFile = try {
                        TransferPathSandbox.resolveInsideRoot(sharedRootDir, reqPath)
                    } catch (sec: PathTraversalException) {
                        call.respondText(sec.message ?: "Forbidden", status = HttpStatusCode.Forbidden)
                        return@get
                    }
                    if (!targetFile.exists() || !targetFile.isFile) {
                        call.respondText("File not found", status = HttpStatusCode.NotFound)
                        return@get
                    }
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment.withParameter(
                            ContentDisposition.Parameters.FileName,
                            targetFile.name,
                        ).toString(),
                    )
                    onActivity("HTTP", "DOWNLOAD", targetFile.name, targetFile.length())
                    call.respondFile(targetFile)
                }

                post("/api/upload") {
                    val cfg = configProvider()
                    if (!isPinAuthorized(call, cfg)) {
                        call.respondText("PIN required or invalid", status = HttpStatusCode.Unauthorized)
                        return@post
                    }
                    if (cfg.readOnly) {
                        call.respondText("Server is in Read-Only mode", status = HttpStatusCode.Forbidden)
                        return@post
                    }

                    val requestedSubPath = call.request.queryParameters["path"]
                    val destDir = try {
                        if (requestedSubPath.isNullOrBlank()) {
                            uploadRootDir.apply { mkdirs() }
                        } else {
                            TransferPathSandbox.resolveInsideRoot(sharedRootDir, requestedSubPath)
                                .apply { mkdirs() }
                        }
                    } catch (sec: PathTraversalException) {
                        call.respondText(sec.message ?: "Forbidden", status = HttpStatusCode.Forbidden)
                        return@post
                    }

                    val uploadedNames = mutableListOf<String>()
                    var currentTargetFile: File? = null
                    try {
                        val multipart = call.receiveMultipart(formFieldLimit = MAX_UPLOAD_BYTES)
                        multipart.forEachPart { part ->
                            try {
                                if (part is PartData.FileItem) {
                                    val rawName = part.originalFileName ?: "upload.bin"
                                    val safeName = TransferPathSandbox.sanitizeSingleUploadFilename(rawName)
                                    val targetFile = TransferPathSandbox.uniqueTargetFile(destDir, safeName)
                                    currentTargetFile = targetFile
                                    val channel = part.provider()
                                    val buffer = ByteArray(16 * 1024)
                                    var totalBytes = 0L
                                    FileOutputStream(targetFile).use { fos ->
                                        while (true) {
                                            val read = channel.readAvailable(buffer, 0, buffer.size)
                                            if (read < 0) break
                                            if (read == 0) continue
                                            totalBytes += read
                                            if (totalBytes > MAX_UPLOAD_BYTES) {
                                                throw IllegalArgumentException("Upload exceeds 512 MB limit")
                                            }
                                            fos.write(buffer, 0, read)
                                        }
                                    }
                                    currentTargetFile = null
                                    uploadedNames += targetFile.name
                                    onActivity("HTTP", "UPLOAD", targetFile.name, totalBytes)
                                    onFileUploaded(targetFile)
                                }
                            } finally {
                                part.dispose()
                            }
                        }
                    } catch (sec: PathTraversalException) {
                        currentTargetFile?.delete()
                        call.respondText(sec.message ?: "Forbidden", status = HttpStatusCode.Forbidden)
                        return@post
                    } catch (limitErr: IllegalArgumentException) {
                        currentTargetFile?.delete()
                        call.respondText(limitErr.message ?: "Payload too large", status = HttpStatusCode.PayloadTooLarge)
                        return@post
                    } catch (t: Throwable) {
                        currentTargetFile?.delete()
                        call.respondText(t.message ?: "Upload failed", status = HttpStatusCode.InternalServerError)
                        return@post
                    }

                    if (uploadedNames.isEmpty()) {
                        call.respondText("No file attached", status = HttpStatusCode.BadRequest)
                    } else {
                        call.respondText("Uploaded ${uploadedNames.joinToString(", ")}", status = HttpStatusCode.OK)
                    }
                }
            }
        }
        created.start(wait = false)
        server = created
        return port
    }

    fun stop() {
        runCatching { server?.stop(gracePeriodMillis = 200, timeoutMillis = 1_000) }
        server = null
    }

    private fun isPinAuthorized(call: ApplicationCall, config: WifiTransferConfig): Boolean {
        val expected = config.pinCode.trim()
        if (expected.isEmpty()) return true
        val headerPin = call.request.headers["X-Evora-Pin"]?.trim()
        val queryPin = call.request.queryParameters["pin"]?.trim()
        return headerPin == expected || queryPin == expected
    }

    private fun buildFilesJson(
        root: File,
        entries: List<File>,
        readOnly: Boolean,
    ): String {
        val rootPath = root.canonicalPath
        val itemsJson = entries.joinToString(",") { file ->
            val rel = file.canonicalPath.removePrefix(rootPath).trimStart(File.separatorChar)
                .replace(File.separatorChar, '/')
            val size = if (file.isDirectory) 0L else file.length()
            """{"name":"${escapeJson(file.name)}","path":"${escapeJson(rel)}","isDirectory":${file.isDirectory},"sizeBytes":$size}"""
        }
        return """{"readOnly":$readOnly,"entries":[$itemsJson]}"""
    }

    private fun escapeJson(raw: String): String = buildString(raw.length) {
        raw.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }

    companion object {
        private const val MAX_UPLOAD_BYTES = 512L * 1024L * 1024L
    }
}
