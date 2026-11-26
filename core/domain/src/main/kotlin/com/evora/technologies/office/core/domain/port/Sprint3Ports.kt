package com.evora.technologies.office.core.domain.port

import com.evora.technologies.office.core.model.BatchOperationReport
import com.evora.technologies.office.core.model.CloudAccount
import com.evora.technologies.office.core.model.CloudBrowsePage
import com.evora.technologies.office.core.model.CloudItem
import com.evora.technologies.office.core.model.CloudProvider
import com.evora.technologies.office.core.model.CloudTransferProgress
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.StorageDirectoryListing
import com.evora.technologies.office.core.model.StorageEntry
import com.evora.technologies.office.core.model.StorageVolumeRoot
import com.evora.technologies.office.core.model.WifiTransferConfig
import com.evora.technologies.office.core.model.WifiTransferServerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface StorageBrowserRepository {
    suspend fun getAvailableRoots(): List<StorageVolumeRoot>

    suspend fun listDirectory(
        root: StorageVolumeRoot,
        pathOrUri: String? = null,
    ): Result<StorageDirectoryListing>

    suspend fun createFolder(
        root: StorageVolumeRoot,
        parentPathOrUri: String,
        folderName: String,
    ): Result<StorageEntry>

    suspend fun batchCopy(
        entries: List<StorageEntry>,
        targetFolderPathOrUri: String,
    ): Result<BatchOperationReport>

    suspend fun batchMove(
        entries: List<StorageEntry>,
        targetFolderPathOrUri: String,
    ): Result<BatchOperationReport>

    suspend fun batchMoveToTrash(
        entries: List<StorageEntry>,
    ): Result<BatchOperationReport>

    suspend fun registerSafTreeUri(
        treeUriString: String,
        displayLabel: String? = null,
    ): Result<StorageVolumeRoot>

    suspend fun removeSafTreeUri(treeUriString: String)

    suspend fun materializeEntryForOpen(entry: StorageEntry): Result<DocumentItem>
}

interface WifiTransferPort {
    val serverState: StateFlow<WifiTransferServerState>

    suspend fun startServer(config: WifiTransferConfig = WifiTransferConfig()): Result<Unit>

    suspend fun stopServer(): Result<Unit>

    suspend fun updateConfig(config: WifiTransferConfig)
}

interface CloudDrivePort {
    fun observeConnectedAccounts(): Flow<List<CloudAccount>>

    fun observeTransferProgress(): Flow<CloudTransferProgress?>

    suspend fun isProviderConfigured(provider: CloudProvider): Boolean

    suspend fun connectGoogleDrive(
        accessToken: String,
        email: String,
        displayName: String,
        avatarUri: String? = null,
    ): Result<CloudAccount>

    suspend fun connectOneDrive(
        accessToken: String,
        accountId: String,
        email: String,
        displayName: String,
    ): Result<CloudAccount>

    suspend fun disconnectAccount(accountId: String): Result<Unit>

    suspend fun listFolder(
        account: CloudAccount,
        folderId: String = "root",
        pageToken: String? = null,
        query: String = "",
    ): Result<CloudBrowsePage>

    suspend fun downloadFile(
        account: CloudAccount,
        item: CloudItem,
    ): Result<DocumentItem>

    suspend fun uploadLocalFile(
        account: CloudAccount,
        localUriString: String,
        targetFolderId: String,
    ): Result<CloudItem>
}

interface WidgetRefreshPort {
    suspend fun refreshAllWidgets()
}
