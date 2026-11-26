package com.evora.technologies.office.feature.cloud.browser

import app.cash.turbine.test
import com.evora.technologies.office.core.domain.port.CloudDrivePort
import com.evora.technologies.office.core.model.CloudAccount
import com.evora.technologies.office.core.model.CloudBrowsePage
import com.evora.technologies.office.core.model.CloudItem
import com.evora.technologies.office.core.model.CloudProvider
import com.evora.technologies.office.core.model.CloudTransferProgress
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeCloudDrivePort : CloudDrivePort {
    val accounts = MutableStateFlow<List<CloudAccount>>(emptyList())
    val progress = MutableStateFlow<CloudTransferProgress?>(null)
    var configuredOneDrive = true

    override fun observeConnectedAccounts(): Flow<List<CloudAccount>> = accounts
    override fun observeTransferProgress(): Flow<CloudTransferProgress?> = progress
    override suspend fun isProviderConfigured(provider: CloudProvider): Boolean =
        provider == CloudProvider.GOOGLE_DRIVE || configuredOneDrive

    override suspend fun connectGoogleDrive(accessToken: String, email: String, displayName: String, avatarUri: String?): Result<CloudAccount> {
        val acc = CloudAccount("g_$email", CloudProvider.GOOGLE_DRIVE, email, displayName, avatarUri, "alias_g")
        accounts.value = accounts.value + acc
        return Result.success(acc)
    }

    override suspend fun connectOneDrive(accessToken: String, accountId: String, email: String, displayName: String): Result<CloudAccount> {
        val acc = CloudAccount(accountId, CloudProvider.ONE_DRIVE, email, displayName, null, "alias_o")
        accounts.value = accounts.value + acc
        return Result.success(acc)
    }

    override suspend fun disconnectAccount(accountId: String): Result<Unit> {
        accounts.value = accounts.value.filterNot { it.accountId == accountId }
        return Result.success(Unit)
    }

    override suspend fun listFolder(account: CloudAccount, folderId: String, pageToken: String?, query: String): Result<CloudBrowsePage> {
        return if (pageToken == null) {
            Result.success(
                CloudBrowsePage(
                    items = listOf(
                        CloudItem("f1", "Finance", account.provider, true, "folder", 0L, 100L, folderId),
                        CloudItem("d1", "Report.docx", account.provider, false, "doc", 2048L, 200L, folderId),
                    ),
                    nextPageToken = "next_1",
                ),
            )
        } else {
            Result.success(
                CloudBrowsePage(
                    items = listOf(CloudItem("d2", "Page2.pdf", account.provider, false, "pdf", 512L, 300L, folderId)),
                    nextPageToken = null,
                ),
            )
        }
    }

    override suspend fun downloadFile(account: CloudAccount, item: CloudItem): Result<DocumentItem> =
        Result.success(DocumentItem(item.id, "file:///cloud/${item.name}", item.name, "docx", DocFormat.WORD, item.sizeBytes, 1L, "Cloud"))

    override suspend fun uploadLocalFile(account: CloudAccount, localUriString: String, targetFolderId: String): Result<CloudItem> =
        Result.success(CloudItem("up1", "Uploaded.pdf", account.provider, false, "pdf", 1024L, 1L, targetFolderId))
}

@OptIn(ExperimentalCoroutinesApi::class)
class CloudDriveBrowserViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `connecting, paginating, downloading, uploading, and disconnecting work cleanly`() = runTest {
        val port = FakeCloudDrivePort()
        val vm = CloudDriveBrowserViewModel(port)
        advanceUntilIdle()

        vm.effects.test {
            vm.onIntent(CloudDriveBrowserIntent.GoogleSignInCompleted("tok", "user@evora.io", "User"))
            advanceUntilIdle()
            val connectedMsg = awaitItem() as CloudDriveBrowserEffect.ShowMessage
            assertTrue(connectedMsg.message.contains("user@evora.io"))
            assertEquals(2, vm.state.value.items.size)
            assertEquals("next_1", vm.state.value.nextPageToken)

            vm.onIntent(CloudDriveBrowserIntent.LoadNextPage)
            advanceUntilIdle()
            assertEquals(3, vm.state.value.items.size)

            val doc = vm.state.value.items.first { !it.isFolder }
            vm.onIntent(CloudDriveBrowserIntent.OpenCloudItem(doc))
            advanceUntilIdle()
            val opened = awaitItem() as CloudDriveBrowserEffect.OpenDownloadedDocument
            assertEquals("Report.docx", opened.document.displayName)

            val accId = vm.state.value.activeAccount!!.accountId
            vm.onIntent(CloudDriveBrowserIntent.DisconnectAccount(accId))
            advanceUntilIdle()
            assertTrue(vm.state.value.items.isEmpty())
            assertTrue(awaitItem() is CloudDriveBrowserEffect.ShowMessage)
        }
    }
}
