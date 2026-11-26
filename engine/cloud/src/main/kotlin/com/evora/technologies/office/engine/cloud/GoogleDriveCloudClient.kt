package com.evora.technologies.office.engine.cloud

import com.evora.technologies.office.core.model.CloudBrowsePage
import com.evora.technologies.office.core.model.CloudItem
import com.evora.technologies.office.core.model.CloudProvider
import com.google.api.client.googleapis.media.MediaHttpDownloader
import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.http.FileContent
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import com.google.api.services.drive.model.File as DriveApiFile

interface CloudRemoteProvider {
    suspend fun listFolder(
        accessToken: String,
        folderId: String,
        pageToken: String?,
        query: String,
    ): CloudBrowsePage

    suspend fun downloadFile(
        accessToken: String,
        item: CloudItem,
        destinationFile: File,
        onProgress: (bytesTransferred: Long, totalBytes: Long) -> Unit,
    ): File

    suspend fun uploadFile(
        accessToken: String,
        localFile: File,
        mimeType: String,
        parentFolderId: String,
        onProgress: (bytesTransferred: Long, totalBytes: Long) -> Unit,
    ): CloudItem
}

@Singleton
class GoogleDriveCloudClient @Inject constructor() : CloudRemoteProvider {

    private val transport = NetHttpTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()

    private fun buildDriveService(accessToken: String): Drive {
        val initializer = HttpRequestInitializer { request ->
            request.headers.authorization = "Bearer $accessToken"
            request.connectTimeout = 20_000
            request.readTimeout = 60_000
        }
        return Drive.Builder(transport, jsonFactory, initializer)
            .setApplicationName("Evora Office")
            .build()
    }

    override suspend fun listFolder(
        accessToken: String,
        folderId: String,
        pageToken: String?,
        query: String,
    ): CloudBrowsePage {
        val service = buildDriveService(accessToken)
        val safeParent = escapeDriveQueryValue(folderId.ifBlank { "root" })
        val qBuilder = StringBuilder("trashed = false")
        if (query.isNotBlank()) {
            val escaped = escapeDriveQueryValue(query.trim())
            qBuilder.append(" and name contains '").append(escaped).append("'")
        } else {
            qBuilder.append(" and '").append(safeParent).append("' in parents")
        }

        val response = service.files().list()
            .setQ(qBuilder.toString())
            .setPageSize(50)
            .setPageToken(pageToken)
            .setOrderBy("folder,modifiedTime desc")
            .setFields("nextPageToken, files(id, name, mimeType, size, modifiedTime, parents, webViewLink)")
            .execute()

        val items = response.files.orEmpty().map { f ->
            val mime = f.mimeType ?: "application/octet-stream"
            CloudItem(
                id = f.id,
                name = resolveExportableDisplayName(f.name ?: "Document", mime),
                provider = CloudProvider.GOOGLE_DRIVE,
                isFolder = mime == FOLDER_MIME,
                mimeType = mime,
                sizeBytes = f.getSize() ?: 0L,
                modifiedAtEpochMs = f.modifiedTime?.value ?: 0L,
                parentId = f.parents?.firstOrNull() ?: safeParent,
                webViewUrl = f.webViewLink,
            )
        }
        return CloudBrowsePage(
            items = items,
            nextPageToken = response.nextPageToken,
        )
    }

    override suspend fun downloadFile(
        accessToken: String,
        item: CloudItem,
        destinationFile: File,
        onProgress: (bytesTransferred: Long, totalBytes: Long) -> Unit,
    ): File {
        val service = buildDriveService(accessToken)
        destinationFile.parentFile?.mkdirs()

        val exportMime = workspaceExportMime(item.mimeType)
        if (exportMime != null) {
            FileOutputStream(destinationFile).use { fos ->
                service.files().export(item.id, exportMime).executeMediaAndDownloadTo(fos)
            }
            onProgress(destinationFile.length(), destinationFile.length())
            return destinationFile
        }

        val getRequest = service.files().get(item.id)
        val total = item.sizeBytes.coerceAtLeast(1L)
        getRequest.mediaHttpDownloader.apply {
            isDirectDownloadEnabled = false
            chunkSize = MediaHttpDownloader.MAXIMUM_CHUNK_SIZE
            setProgressListener { downloader ->
                onProgress(downloader.numBytesDownloaded, total)
            }
        }
        FileOutputStream(destinationFile).use { fos ->
            getRequest.executeMediaAndDownloadTo(fos)
        }
        onProgress(destinationFile.length(), destinationFile.length())
        return destinationFile
    }

    override suspend fun uploadFile(
        accessToken: String,
        localFile: File,
        mimeType: String,
        parentFolderId: String,
        onProgress: (bytesTransferred: Long, totalBytes: Long) -> Unit,
    ): CloudItem {
        val service = buildDriveService(accessToken)
        val metadata = DriveApiFile().apply {
            name = localFile.name
            if (parentFolderId.isNotBlank() && parentFolderId != "root") {
                parents = listOf(parentFolderId)
            }
        }
        val mediaContent = FileContent(mimeType, localFile)
        val total = localFile.length().coerceAtLeast(1L)
        val createReq = service.files().create(metadata, mediaContent)
            .setFields("id, name, mimeType, size, modifiedTime, parents, webViewLink")
        createReq.mediaHttpUploader.apply {
            isDirectUploadEnabled = total <= 4L * 1024L * 1024L
            chunkSize = MediaHttpUploader.MINIMUM_CHUNK_SIZE * 4
            setProgressListener { uploader ->
                onProgress(uploader.numBytesUploaded, total)
            }
        }
        val uploaded = createReq.execute()
        onProgress(total, total)
        return CloudItem(
            id = uploaded.id,
            name = uploaded.name ?: localFile.name,
            provider = CloudProvider.GOOGLE_DRIVE,
            isFolder = false,
            mimeType = uploaded.mimeType ?: mimeType,
            sizeBytes = uploaded.getSize() ?: localFile.length(),
            modifiedAtEpochMs = uploaded.modifiedTime?.value ?: System.currentTimeMillis(),
            parentId = uploaded.parents?.firstOrNull() ?: parentFolderId,
            webViewUrl = uploaded.webViewLink,
        )
    }

    private fun workspaceExportMime(mime: String): String? = when (mime) {
        "application/vnd.google-apps.document" ->
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "application/vnd.google-apps.spreadsheet" ->
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "application/vnd.google-apps.presentation" ->
            "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        else -> null
    }

    private fun resolveExportableDisplayName(name: String, mime: String): String {
        val ext = when (mime) {
            "application/vnd.google-apps.document" -> ".docx"
            "application/vnd.google-apps.spreadsheet" -> ".xlsx"
            "application/vnd.google-apps.presentation" -> ".pptx"
            else -> return name
        }
        return if (name.endsWith(ext, ignoreCase = true)) name else "$name$ext"
    }

    private fun escapeDriveQueryValue(raw: String): String =
        raw.replace("\\", "\\\\").replace("'", "\\'")

    companion object {
        private const val FOLDER_MIME = "application/vnd.google-apps.folder"
    }
}
