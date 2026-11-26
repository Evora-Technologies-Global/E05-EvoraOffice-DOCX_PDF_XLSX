package com.evora.technologies.office.engine.cloud

import com.evora.technologies.office.core.model.CloudBrowsePage
import com.evora.technologies.office.core.model.CloudItem
import com.evora.technologies.office.core.model.CloudProvider
import com.microsoft.graph.core.tasks.LargeFileUploadTask
import com.microsoft.graph.drives.item.items.item.createuploadsession.CreateUploadSessionPostRequestBody
import com.microsoft.graph.models.DriveItem
import com.microsoft.graph.models.DriveItemUploadableProperties
import com.microsoft.graph.serviceclient.GraphServiceClient
import com.microsoft.kiota.RequestInformation
import com.microsoft.kiota.authentication.AuthenticationProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Microsoft OneDrive client backed by Microsoft Graph Java SDK v6 (`GraphServiceClient`)
 * and `LargeFileUploadTask` for > 4 MB chunked uploads (`T-020` / `03_PRD.md` §8).
 */
@Singleton
class OneDriveCloudClient @Inject constructor() : CloudRemoteProvider {

    private val httpClient = OkHttpClient.Builder().build()

    private fun buildGraphClient(accessToken: String): GraphServiceClient {
        val authProvider = AuthenticationProvider { request: RequestInformation, _ ->
            request.headers.add("Authorization", "Bearer $accessToken")
        }
        return GraphServiceClient(authProvider)
    }

    override suspend fun listFolder(
        accessToken: String,
        folderId: String,
        pageToken: String?,
        query: String,
    ): CloudBrowsePage {
        val client = buildGraphClient(accessToken)
        val driveId = client.me().drive().get()?.id ?: "me"

        val (driveItems, nextLink) = if (!pageToken.isNullOrBlank()) {
            val page = client.drives().byDriveId(driveId).items().withUrl(pageToken).get()
            page?.value.orEmpty() to page?.odataNextLink
        } else if (query.isNotBlank()) {
            val escaped = query.trim().replace("'", "''")
            val page = client.drives().byDriveId(driveId).items().byDriveItemId("root").searchWithQ(escaped).get { cfg ->
                cfg.queryParameters.top = 50
            }
            page?.value.orEmpty() to page?.odataNextLink
        } else {
            val targetId = if (folderId.isBlank()) "root" else folderId
            val page = client.drives().byDriveId(driveId).items().byDriveItemId(targetId).children().get { cfg ->
                cfg.queryParameters.top = 50
                cfg.queryParameters.orderby = arrayOf("name asc")
            }
            page?.value.orEmpty() to page?.odataNextLink
        }

        val items = driveItems.mapNotNull { item ->
            val id = item.id ?: return@mapNotNull null
            val isFolder = item.folder != null
            val mime = item.file?.mimeType ?: if (isFolder) "inode/directory" else "application/octet-stream"
            CloudItem(
                id = id,
                name = item.name ?: "Item",
                provider = CloudProvider.ONE_DRIVE,
                isFolder = isFolder,
                mimeType = mime,
                sizeBytes = item.size ?: 0L,
                modifiedAtEpochMs = item.lastModifiedDateTime?.toInstant()?.toEpochMilli() ?: 0L,
                parentId = item.parentReference?.id ?: folderId,
                webViewUrl = item.webUrl,
            )
        }
        return CloudBrowsePage(
            items = items,
            nextPageToken = nextLink,
        )
    }

    override suspend fun downloadFile(
        accessToken: String,
        item: CloudItem,
        destinationFile: File,
        onProgress: (bytesTransferred: Long, totalBytes: Long) -> Unit,
    ): File {
        destinationFile.parentFile?.mkdirs()
        val url = "https://graph.microsoft.com/v1.0/me/drive/items/${item.id}/content"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()
        val call = httpClient.newCall(request)
        val completionHandle = currentCoroutineContext()[Job]?.invokeOnCompletion {
            call.cancel()
        }
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    error("OneDrive download failed: HTTP ${response.code}")
                }
                val body = response.body ?: error("Empty OneDrive response")
                val total = body.contentLength().takeIf { it > 0L } ?: item.sizeBytes.coerceAtLeast(1L)
                var downloaded = 0L
                body.byteStream().use { input ->
                    FileOutputStream(destinationFile).use { fos ->
                        val buf = ByteArray(16 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buf)
                            if (read <= 0) break
                            fos.write(buf, 0, read)
                            downloaded += read
                            onProgress(downloaded, total)
                        }
                    }
                }
            }
        } finally {
            completionHandle?.dispose()
        }
        return destinationFile
    }

    override suspend fun uploadFile(
        accessToken: String,
        localFile: File,
        mimeType: String,
        parentFolderId: String,
        onProgress: (bytesTransferred: Long, totalBytes: Long) -> Unit,
    ): CloudItem {
        val client = buildGraphClient(accessToken)
        val driveId = client.me().drive().get()?.id ?: "me"
        val targetFolder = parentFolderId.ifBlank { "root" }
        val encodedName = encodeSegment(localFile.name)
        val encodedDriveId = encodeSegment(driveId)
        val itemPathPrefix = if (targetFolder == "root") {
            "https://graph.microsoft.com/v1.0/drives/$encodedDriveId/root:/$encodedName:"
        } else {
            "https://graph.microsoft.com/v1.0/drives/$encodedDriveId/items/${encodeSegment(targetFolder)}:/$encodedName:"
        }
        val fileSize = localFile.length()

        if (fileSize <= SIMPLE_UPLOAD_LIMIT_BYTES) {
            val uploaded = FileInputStream(localFile).use { fis ->
                client.drives()
                    .byDriveId(driveId)
                    .items()
                    .byDriveItemId("root")
                    .content()
                    .withUrl("$itemPathPrefix/content")
                    .put(fis)
            }
            onProgress(fileSize, fileSize.coerceAtLeast(1L))
            return uploaded.toCloudItem(localFile, mimeType, targetFolder)
        }

        // > 4 MB chunked upload via official Microsoft Graph LargeFileUploadTask
        val sessionBody = CreateUploadSessionPostRequestBody().apply {
            item = DriveItemUploadableProperties().apply {
                name = localFile.name
            }
        }
        val uploadSession = client.drives()
            .byDriveId(driveId)
            .items()
            .byDriveItemId("root")
            .createUploadSession()
            .withUrl("$itemPathPrefix/createUploadSession")
            .post(sessionBody)
            ?: error("Unable to create OneDrive upload session")

        val uploaded = FileInputStream(localFile).use { fis ->
            val task = LargeFileUploadTask<DriveItem>(
                client.requestAdapter,
                uploadSession,
                fis,
                fileSize,
                DEFAULT_CHUNK_SIZE,
                DriveItem::createFromDiscriminatorValue,
            )
            val result = task.upload(3) { current, total ->
                onProgress(current, total)
            }
            if (!result.isUploadSuccessful) {
                error("OneDrive chunked upload did not complete")
            }
            result.itemResponse
        }
        return uploaded.toCloudItem(localFile, mimeType, targetFolder)
    }

    private fun DriveItem?.toCloudItem(
        localFile: File,
        mimeType: String,
        parentFolderId: String,
    ): CloudItem = CloudItem(
        id = this?.id ?: localFile.name,
        name = this?.name ?: localFile.name,
        provider = CloudProvider.ONE_DRIVE,
        isFolder = false,
        mimeType = this?.file?.mimeType ?: mimeType,
        sizeBytes = this?.size ?: localFile.length(),
        modifiedAtEpochMs = this?.lastModifiedDateTime?.toInstant()?.toEpochMilli() ?: System.currentTimeMillis(),
        parentId = this?.parentReference?.id ?: parentFolderId,
        webViewUrl = this?.webUrl,
    )

    private fun encodeSegment(segment: String): String =
        URLEncoder.encode(segment, "UTF-8").replace("+", "%20")

    companion object {
        private const val SIMPLE_UPLOAD_LIMIT_BYTES = 4L * 1024L * 1024L
        private const val DEFAULT_CHUNK_SIZE = 320L * 1024L * 4L // 1.25 MB (multiple of 320 KiB)
    }
}
