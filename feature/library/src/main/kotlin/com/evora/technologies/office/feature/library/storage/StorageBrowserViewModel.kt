package com.evora.technologies.office.feature.library.storage

import com.evora.technologies.office.core.domain.port.StorageBrowserRepository
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.StorageEntry
import com.evora.technologies.office.core.model.StorageVolumeRoot
import com.evora.technologies.office.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import javax.inject.Inject

@HiltViewModel
class StorageBrowserViewModel @Inject constructor(
    private val storageRepository: StorageBrowserRepository,
) : MviViewModel<StorageBrowserState, StorageBrowserIntent, StorageBrowserEffect>(
    StorageBrowserState(),
) {
    private var loadJob: Job? = null

    init {
        loadRootsAndCurrent()
    }

    override fun onIntent(intent: StorageBrowserIntent) {
        when (intent) {
            StorageBrowserIntent.LoadRoots -> loadRootsAndCurrent()
            is StorageBrowserIntent.SelectRoot -> loadDirectory(intent.root, intent.root.pathOrUri)
            is StorageBrowserIntent.OpenEntry -> handleOpenEntry(intent.entry)
            is StorageBrowserIntent.NavigateBreadcrumb -> {
                val root = currentState.selectedRoot ?: return
                loadDirectory(root, intent.pathOrUri)
            }
            StorageBrowserIntent.NavigateUp -> navigateUp()
            StorageBrowserIntent.Refresh -> {
                val root = currentState.selectedRoot
                if (root == null) {
                    loadRootsAndCurrent()
                } else {
                    loadDirectory(root, currentState.currentPathOrUri)
                }
            }
            StorageBrowserIntent.OpenNewFolderDialog -> setState {
                copy(showNewFolderDialog = true, newFolderError = null)
            }
            StorageBrowserIntent.DismissNewFolderDialog -> setState {
                copy(showNewFolderDialog = false, newFolderError = null)
            }
            is StorageBrowserIntent.CreateFolderConfirmed -> createFolder(intent.name)
            is StorageBrowserIntent.ToggleEntrySelection -> toggleSelection(intent.entryId)
            is StorageBrowserIntent.EnterMultiSelect -> setState {
                copy(selectedEntryIds = selectedEntryIds + intent.entryId)
            }
            StorageBrowserIntent.SelectAll -> setState {
                copy(selectedEntryIds = entries.mapTo(HashSet()) { it.id })
            }
            StorageBrowserIntent.ClearSelection -> setState { copy(selectedEntryIds = emptySet()) }
            is StorageBrowserIntent.RequestBatchTransfer -> {
                if (currentState.selectedEntries.isEmpty()) return
                setState {
                    copy(
                        pendingBatchAction = intent.action,
                        destinationCandidates = entries.filter { it.isDirectory && it.id !in selectedEntryIds },
                    )
                }
            }
            StorageBrowserIntent.DismissBatchTransfer -> setState {
                copy(pendingBatchAction = null, destinationCandidates = emptyList())
            }
            is StorageBrowserIntent.ConfirmBatchTransferTo -> executeBatchTransfer(intent.targetPathOrUri)
            StorageBrowserIntent.BatchTrashClicked -> executeBatchTrash()
            StorageBrowserIntent.BatchShareClicked -> shareSelected()
            StorageBrowserIntent.BatchMergePdfClicked -> mergeSelectedPdfs()
            StorageBrowserIntent.OpenSafFolderPicker -> sendEffect(StorageBrowserEffect.LaunchSafTreePicker)
            is StorageBrowserIntent.SafTreePicked -> handleSafTreePicked(intent.treeUriString, intent.displayLabel)
            is StorageBrowserIntent.OpenCloudProvider -> sendEffect(StorageBrowserEffect.OpenCloudBrowser(intent.provider))
            StorageBrowserIntent.DismissRarDialog -> setState { copy(unsupportedRarName = null) }
        }
    }

    private fun loadRootsAndCurrent() {
        loadJob?.cancel()
        loadJob = launchSafely(
            onError = { err ->
                setState { copy(isLoading = false, errorMessage = err.message ?: "Unable to read storage") }
            },
        ) {
            setState { copy(isLoading = true, errorMessage = null) }
            val roots = storageRepository.getAvailableRoots()
            val targetRoot = currentState.selectedRoot?.let { prev ->
                roots.firstOrNull { it.id == prev.id }
            } ?: roots.firstOrNull()

            if (targetRoot == null) {
                setState { copy(isLoading = false, roots = emptyList(), entries = emptyList()) }
                return@launchSafely
            }

            val listing = storageRepository.listDirectory(targetRoot, targetRoot.pathOrUri).getOrThrow()
            setState {
                copy(
                    isLoading = false,
                    roots = roots,
                    selectedRoot = listing.root,
                    currentPathOrUri = listing.currentPathOrUri,
                    breadcrumbs = listing.breadcrumbs,
                    entries = listing.entries,
                    selectedEntryIds = emptySet(),
                    errorMessage = null,
                )
            }
        }
    }

    private fun loadDirectory(root: StorageVolumeRoot, pathOrUri: String) {
        loadJob?.cancel()
        loadJob = launchSafely(
            onError = { err ->
                setState { copy(isLoading = false, errorMessage = err.message ?: "Unable to open folder") }
            },
        ) {
            setState { copy(isLoading = true, selectedRoot = root, errorMessage = null) }
            val listing = storageRepository.listDirectory(root, pathOrUri).getOrThrow()
            setState {
                copy(
                    isLoading = false,
                    selectedRoot = listing.root,
                    currentPathOrUri = listing.currentPathOrUri,
                    breadcrumbs = listing.breadcrumbs,
                    entries = listing.entries,
                    selectedEntryIds = emptySet(),
                    errorMessage = null,
                )
            }
        }
    }

    private fun handleOpenEntry(entry: StorageEntry) {
        if (currentState.isMultiSelectActive) {
            toggleSelection(entry.id)
            return
        }
        if (entry.isDirectory) {
            val root = currentState.selectedRoot ?: return
            loadDirectory(root, entry.pathOrUri)
            return
        }
        val ext = entry.extension.lowercase()
        if (ext == "rar") {
            setState { copy(unsupportedRarName = entry.name) }
            return
        }
        if (entry.format == DocFormat.ARCHIVE) {
            sendEffect(StorageBrowserEffect.OpenArchiveInspector(entry.uriString, entry.name))
            return
        }
        launchSafely(
            onError = { err ->
                sendEffect(StorageBrowserEffect.ShowMessage(err.message ?: "Unable to open ${entry.name}"))
            },
        ) {
            val doc = storageRepository.materializeEntryForOpen(entry).getOrThrow()
            sendEffect(StorageBrowserEffect.OpenDocument(doc))
        }
    }

    private fun navigateUp() {
        val crumbs = currentState.breadcrumbs
        if (crumbs.size <= 1) return
        val parent = crumbs[crumbs.size - 2]
        val root = currentState.selectedRoot ?: return
        loadDirectory(root, parent.pathOrUri)
    }

    private fun createFolder(name: String) {
        val root = currentState.selectedRoot ?: return
        launchSafely(
            onError = { err ->
                setState { copy(newFolderError = err.message ?: "Unable to create folder") }
            },
        ) {
            storageRepository.createFolder(root, currentState.currentPathOrUri, name).getOrThrow()
            setState { copy(showNewFolderDialog = false, newFolderError = null) }
            sendEffect(StorageBrowserEffect.ShowMessage("Folder created"))
            loadDirectory(root, currentState.currentPathOrUri)
        }
    }

    private fun toggleSelection(entryId: String) {
        setState {
            val next = if (entryId in selectedEntryIds) selectedEntryIds - entryId else selectedEntryIds + entryId
            copy(selectedEntryIds = next)
        }
    }

    private fun executeBatchTransfer(targetPathOrUri: String) {
        val action = currentState.pendingBatchAction ?: return
        val selected = currentState.selectedEntries
        if (selected.isEmpty() || currentState.isBatchBusy) return

        launchSafely(
            onError = { err ->
                setState { copy(isBatchBusy = false, pendingBatchAction = null) }
                sendEffect(StorageBrowserEffect.ShowMessage(err.message ?: "Batch operation failed"))
            },
        ) {
            setState { copy(isBatchBusy = true, pendingBatchAction = null) }
            val report = when (action) {
                BatchTransferAction.COPY -> storageRepository.batchCopy(selected, targetPathOrUri)
                BatchTransferAction.MOVE -> storageRepository.batchMove(selected, targetPathOrUri)
            }.getOrThrow()
            setState { copy(isBatchBusy = false, selectedEntryIds = emptySet()) }
            sendEffect(StorageBrowserEffect.ShowMessage(report.message))
            currentState.selectedRoot?.let { loadDirectory(it, currentState.currentPathOrUri) }
        }
    }

    private fun executeBatchTrash() {
        val selected = currentState.selectedEntries
        if (selected.isEmpty() || currentState.isBatchBusy) return
        launchSafely(
            onError = { err ->
                setState { copy(isBatchBusy = false) }
                sendEffect(StorageBrowserEffect.ShowMessage(err.message ?: "Delete failed"))
            },
        ) {
            setState { copy(isBatchBusy = true) }
            val report = storageRepository.batchMoveToTrash(selected).getOrThrow()
            setState { copy(isBatchBusy = false, selectedEntryIds = emptySet()) }
            sendEffect(StorageBrowserEffect.ShowMessage(report.message))
            currentState.selectedRoot?.let { loadDirectory(it, currentState.currentPathOrUri) }
        }
    }

    private fun shareSelected() {
        val files = currentState.selectedEntries.filter { !it.isDirectory }
        if (files.isEmpty()) {
            sendEffect(StorageBrowserEffect.ShowMessage("Select at least one file to share"))
            return
        }
        sendEffect(StorageBrowserEffect.ShareEntries(files))
    }

    private fun mergeSelectedPdfs() {
        val pdfs = currentState.selectedEntries.filter { !it.isDirectory && it.extension.equals("pdf", ignoreCase = true) }
        if (pdfs.size < 2) {
            sendEffect(StorageBrowserEffect.ShowMessage("Select 2 or more PDF files to merge"))
            return
        }
        setState { copy(selectedEntryIds = emptySet()) }
        sendEffect(StorageBrowserEffect.MergePdfUris(pdfs.map { it.uriString }))
    }

    private fun handleSafTreePicked(treeUriString: String?, displayLabel: String?) {
        if (treeUriString.isNullOrBlank()) return
        launchSafely(
            onError = { err ->
                sendEffect(StorageBrowserEffect.ShowMessage(err.message ?: "Unable to add SAF folder"))
            },
        ) {
            val safRoot = storageRepository.registerSafTreeUri(treeUriString, displayLabel).getOrThrow()
            val roots = storageRepository.getAvailableRoots()
            setState { copy(roots = roots) }
            loadDirectory(safRoot, safRoot.pathOrUri)
        }
    }
}
