package com.evora.technologies.office.core.model

enum class StorageRootKind {
    INTERNAL,
    DOWNLOADS,
    SD_CARD,
    SAF_TREE,
}

data class StorageVolumeRoot(
    val id: String,
    val kind: StorageRootKind,
    val title: String,
    val pathOrUri: String,
    val freeBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val isWritable: Boolean = true,
)

data class BreadcrumbSegment(
    val title: String,
    val pathOrUri: String,
)

data class StorageEntry(
    val id: String,
    val uriString: String,
    val pathOrUri: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val modifiedAtEpochMs: Long,
    val childCount: Int = 0,
    val format: DocFormat? = null,
    val extension: String = "",
    val isSafEntry: Boolean = false,
)

data class StorageDirectoryListing(
    val root: StorageVolumeRoot,
    val currentPathOrUri: String,
    val breadcrumbs: List<BreadcrumbSegment>,
    val entries: List<StorageEntry>,
    val isSafMode: Boolean = false,
)

data class BatchOperationReport(
    val actionName: String,
    val succeededCount: Int,
    val failedCount: Int,
    val message: String,
)

data class WifiTransferConfig(
    val httpPort: Int = 8998,
    val ftpPort: Int = 8999,
    val ftpEnabled: Boolean = true,
    val readOnly: Boolean = false,
    val pinCode: String = "",
    val idleTimeoutSec: Int = 600,
)

data class WifiTransferLogEntry(
    val id: String,
    val protocol: String,
    val action: String,
    val targetName: String,
    val bytes: Long,
    val timestampEpochMs: Long,
)

data class WifiTransferServerState(
    val isRunning: Boolean = false,
    val ipAddress: String = "",
    val httpUrl: String = "",
    val ftpUrl: String = "",
    val activeHttpPort: Int = 8998,
    val activeFtpPort: Int = 8999,
    val config: WifiTransferConfig = WifiTransferConfig(),
    val idleSecondsRemaining: Int = 600,
    val transferLogs: List<WifiTransferLogEntry> = emptyList(),
    val errorMessage: String? = null,
)

data class CloudItem(
    val id: String,
    val name: String,
    val provider: CloudProvider,
    val isFolder: Boolean,
    val mimeType: String,
    val sizeBytes: Long,
    val modifiedAtEpochMs: Long,
    val parentId: String,
    val webViewUrl: String? = null,
)

data class CloudBrowsePage(
    val items: List<CloudItem>,
    val nextPageToken: String? = null,
)

data class CloudTransferProgress(
    val itemName: String,
    val isUpload: Boolean,
    val bytesTransferred: Long,
    val totalBytes: Long,
    val fraction: Float,
)
