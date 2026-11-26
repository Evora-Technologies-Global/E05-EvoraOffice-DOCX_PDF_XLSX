package com.evora.technologies.office.feature.library.storage

import com.evora.technologies.office.core.model.BreadcrumbSegment
import com.evora.technologies.office.core.model.CloudProvider
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.StorageEntry
import com.evora.technologies.office.core.model.StorageVolumeRoot
import com.evora.technologies.office.core.mvi.UiEffect
import com.evora.technologies.office.core.mvi.UiIntent
import com.evora.technologies.office.core.mvi.UiState

enum class BatchTransferAction {
    COPY,
    MOVE,
}

data class StorageBrowserState(
    val isLoading: Boolean = true,
    val roots: List<StorageVolumeRoot> = emptyList(),
    val selectedRoot: StorageVolumeRoot? = null,
    val currentPathOrUri: String = "",
    val breadcrumbs: List<BreadcrumbSegment> = emptyList(),
    val entries: List<StorageEntry> = emptyList(),
    val selectedEntryIds: Set<String> = emptySet(),
    val isBatchBusy: Boolean = false,
    val showNewFolderDialog: Boolean = false,
    val newFolderError: String? = null,
    val pendingBatchAction: BatchTransferAction? = null,
    val destinationCandidates: List<StorageEntry> = emptyList(),
    val unsupportedRarName: String? = null,
    val errorMessage: String? = null,
) : UiState {
    val isMultiSelectActive: Boolean get() = selectedEntryIds.isNotEmpty()
    val selectedEntries: List<StorageEntry> get() = entries.filter { it.id in selectedEntryIds }
    val selectedPdfCount: Int
        get() = selectedEntries.count { !it.isDirectory && it.extension.equals("pdf", ignoreCase = true) }
    val canMergeSelectedPdfs: Boolean get() = selectedPdfCount >= 2
}

sealed interface StorageBrowserIntent : UiIntent {
    data object LoadRoots : StorageBrowserIntent
    data class SelectRoot(val root: StorageVolumeRoot) : StorageBrowserIntent
    data class OpenEntry(val entry: StorageEntry) : StorageBrowserIntent
    data class NavigateBreadcrumb(val pathOrUri: String) : StorageBrowserIntent
    data object NavigateUp : StorageBrowserIntent
    data object Refresh : StorageBrowserIntent

    data object OpenNewFolderDialog : StorageBrowserIntent
    data object DismissNewFolderDialog : StorageBrowserIntent
    data class CreateFolderConfirmed(val name: String) : StorageBrowserIntent

    data class ToggleEntrySelection(val entryId: String) : StorageBrowserIntent
    data class EnterMultiSelect(val entryId: String) : StorageBrowserIntent
    data object SelectAll : StorageBrowserIntent
    data object ClearSelection : StorageBrowserIntent

    data class RequestBatchTransfer(val action: BatchTransferAction) : StorageBrowserIntent
    data object DismissBatchTransfer : StorageBrowserIntent
    data class ConfirmBatchTransferTo(val targetPathOrUri: String) : StorageBrowserIntent
    data object BatchTrashClicked : StorageBrowserIntent
    data object BatchShareClicked : StorageBrowserIntent
    data object BatchMergePdfClicked : StorageBrowserIntent

    data object OpenSafFolderPicker : StorageBrowserIntent
    data class SafTreePicked(val treeUriString: String?, val displayLabel: String? = null) : StorageBrowserIntent
    data class OpenCloudProvider(val provider: CloudProvider) : StorageBrowserIntent
    data object DismissRarDialog : StorageBrowserIntent
}

sealed interface StorageBrowserEffect : UiEffect {
    data class OpenDocument(val document: DocumentItem) : StorageBrowserEffect
    data class OpenArchiveInspector(val uriString: String, val displayName: String) : StorageBrowserEffect
    data class OpenCloudBrowser(val provider: CloudProvider) : StorageBrowserEffect
    data class ShareEntries(val entries: List<StorageEntry>) : StorageBrowserEffect
    data class MergePdfUris(val pdfUris: List<String>) : StorageBrowserEffect
    data object LaunchSafTreePicker : StorageBrowserEffect
    data class ShowMessage(val message: String) : StorageBrowserEffect
}
