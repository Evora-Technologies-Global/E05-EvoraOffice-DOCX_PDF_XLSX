package com.evora.technologies.office.feature.library.storage

import app.cash.turbine.test
import com.evora.technologies.office.core.domain.port.StorageBrowserRepository
import com.evora.technologies.office.core.model.BatchOperationReport
import com.evora.technologies.office.core.model.BreadcrumbSegment
import com.evora.technologies.office.core.model.CloudProvider
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.StorageDirectoryListing
import com.evora.technologies.office.core.model.StorageEntry
import com.evora.technologies.office.core.model.StorageRootKind
import com.evora.technologies.office.core.model.StorageVolumeRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeStorageRepo : StorageBrowserRepository {
    val root = StorageVolumeRoot("r1", StorageRootKind.INTERNAL, "Internal", "/storage/emulated/0")
    val entries = mutableListOf(
        StorageEntry("dir1", "file:///storage/emulated/0/Sub", "/storage/emulated/0/Sub", "Sub", true, 0L, 100L, childCount = 1),
        StorageEntry("pdf1", "file:///storage/emulated/0/A.pdf", "/storage/emulated/0/A.pdf", "A.pdf", false, 500L, 200L, format = DocFormat.PDF, extension = "pdf"),
        StorageEntry("pdf2", "file:///storage/emulated/0/B.pdf", "/storage/emulated/0/B.pdf", "B.pdf", false, 700L, 300L, format = DocFormat.PDF, extension = "pdf"),
        StorageEntry("rar1", "file:///storage/emulated/0/Legacy.rar", "/storage/emulated/0/Legacy.rar", "Legacy.rar", false, 900L, 400L, format = null, extension = "rar"),
        StorageEntry("zip1", "file:///storage/emulated/0/Docs.zip", "/storage/emulated/0/Docs.zip", "Docs.zip", false, 400L, 500L, format = DocFormat.ARCHIVE, extension = "zip"),
    )
    var lastMovedTarget: String? = null
    var lastTrashedCount = 0

    override suspend fun getAvailableRoots(): List<StorageVolumeRoot> = listOf(root)

    override suspend fun listDirectory(root: StorageVolumeRoot, pathOrUri: String?): Result<StorageDirectoryListing> {
        val target = pathOrUri ?: root.pathOrUri
        val crumbs = if (target == root.pathOrUri) {
            listOf(BreadcrumbSegment("Internal", root.pathOrUri))
        } else {
            listOf(BreadcrumbSegment("Internal", root.pathOrUri), BreadcrumbSegment("Sub", target))
        }
        return Result.success(StorageDirectoryListing(root, target, crumbs, entries.toList()))
    }

    override suspend fun createFolder(root: StorageVolumeRoot, parentPathOrUri: String, folderName: String): Result<StorageEntry> {
        if (folderName.contains('/')) return Result.failure(IllegalArgumentException("Invalid folder name"))
        val created = StorageEntry("dir_${folderName}", "file://$parentPathOrUri/$folderName", "$parentPathOrUri/$folderName", folderName, true, 0L, 600L)
        entries.add(0, created)
        return Result.success(created)
    }

    override suspend fun batchCopy(entries: List<StorageEntry>, targetFolderPathOrUri: String): Result<BatchOperationReport> =
        Result.success(BatchOperationReport("Copy", entries.size, 0, "Copied ${entries.size} item(s)"))

    override suspend fun batchMove(entries: List<StorageEntry>, targetFolderPathOrUri: String): Result<BatchOperationReport> {
        lastMovedTarget = targetFolderPathOrUri
        return Result.success(BatchOperationReport("Move", entries.size, 0, "Moved ${entries.size} item(s)"))
    }

    override suspend fun batchMoveToTrash(entries: List<StorageEntry>): Result<BatchOperationReport> {
        lastTrashedCount = entries.size
        val ids = entries.mapTo(HashSet()) { it.id }
        this.entries.removeAll { it.id in ids }
        return Result.success(BatchOperationReport("Trash", entries.size, 0, "Moved ${entries.size} item(s) to Trash"))
    }

    override suspend fun registerSafTreeUri(treeUriString: String, displayLabel: String?): Result<StorageVolumeRoot> =
        Result.success(StorageVolumeRoot("saf", StorageRootKind.SAF_TREE, displayLabel ?: "SAF", treeUriString))

    override suspend fun removeSafTreeUri(treeUriString: String) = Unit

    override suspend fun materializeEntryForOpen(entry: StorageEntry): Result<DocumentItem> =
        Result.success(DocumentItem(entry.id, entry.uriString, entry.name, entry.extension, entry.format ?: DocFormat.TEXT, entry.sizeBytes, entry.modifiedAtEpochMs, "Internal"))
}

@OptIn(ExperimentalCoroutinesApi::class)
class StorageBrowserViewModelTest {
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
    fun `directory traversal, new folder validation, rar dialog, and zip archive work`() = runTest {
        val repo = FakeStorageRepo()
        val vm = StorageBrowserViewModel(repo)
        advanceUntilIdle()

        assertEquals(5, vm.state.value.entries.size)
        assertEquals(1, vm.state.value.breadcrumbs.size)

        // Invalid folder name shows error
        vm.onIntent(StorageBrowserIntent.CreateFolderConfirmed("bad/name"))
        advanceUntilIdle()
        assertTrue(vm.state.value.newFolderError != null)

        // Valid folder creates entry & zip archive opens ArchiveInspector
        vm.effects.test {
            vm.onIntent(StorageBrowserIntent.CreateFolderConfirmed("Invoices"))
            advanceUntilIdle()
            assertNull(vm.state.value.newFolderError)
            assertTrue(vm.state.value.entries.any { it.name == "Invoices" })
            assertEquals(StorageBrowserEffect.ShowMessage("Folder created"), awaitItem())

            // Tapping .rar shows honest unsupported dialog
            val rar = repo.entries.first { it.extension == "rar" }
            vm.onIntent(StorageBrowserIntent.OpenEntry(rar))
            assertEquals("Legacy.rar", vm.state.value.unsupportedRarName)
            vm.onIntent(StorageBrowserIntent.DismissRarDialog)
            assertNull(vm.state.value.unsupportedRarName)

            // Tapping .zip opens ArchiveInspector
            val zip = repo.entries.first { it.extension == "zip" }
            vm.onIntent(StorageBrowserIntent.OpenEntry(zip))
            assertEquals(StorageBrowserEffect.OpenArchiveInspector(zip.uriString, zip.name), awaitItem())
        }
    }

    @Test
    fun `multi-select batch merge PDF, move, and trash update state synchronously`() = runTest {
        val repo = FakeStorageRepo()
        val vm = StorageBrowserViewModel(repo)
        advanceUntilIdle()

        vm.onIntent(StorageBrowserIntent.EnterMultiSelect("pdf1"))
        assertFalse(vm.state.value.canMergeSelectedPdfs)
        vm.onIntent(StorageBrowserIntent.ToggleEntrySelection("pdf2"))
        assertTrue(vm.state.value.canMergeSelectedPdfs)

        vm.effects.test {
            vm.onIntent(StorageBrowserIntent.BatchMergePdfClicked)
            val effect = awaitItem() as StorageBrowserEffect.MergePdfUris
            assertEquals(2, effect.pdfUris.size)
            assertFalse(vm.state.value.isMultiSelectActive)
        }

        // Batch move & trash
        vm.onIntent(StorageBrowserIntent.EnterMultiSelect("pdf1"))
        vm.onIntent(StorageBrowserIntent.RequestBatchTransfer(BatchTransferAction.MOVE))
        vm.onIntent(StorageBrowserIntent.ConfirmBatchTransferTo("/storage/emulated/0/Sub"))
        advanceUntilIdle()
        assertEquals("/storage/emulated/0/Sub", repo.lastMovedTarget)

        vm.onIntent(StorageBrowserIntent.EnterMultiSelect("pdf2"))
        vm.onIntent(StorageBrowserIntent.BatchTrashClicked)
        advanceUntilIdle()
        assertEquals(1, repo.lastTrashedCount)
        assertFalse(vm.state.value.entries.any { it.id == "pdf2" })
    }

    @Test
    fun `cloud quick card emits OpenCloudBrowser effect`() = runTest {
        val vm = StorageBrowserViewModel(FakeStorageRepo())
        advanceUntilIdle()
        vm.effects.test {
            vm.onIntent(StorageBrowserIntent.OpenCloudProvider(CloudProvider.ONE_DRIVE))
            assertEquals(StorageBrowserEffect.OpenCloudBrowser(CloudProvider.ONE_DRIVE), awaitItem())
        }
    }
}
