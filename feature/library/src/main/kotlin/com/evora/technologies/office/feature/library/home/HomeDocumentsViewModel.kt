package com.evora.technologies.office.feature.library.home

import androidx.lifecycle.viewModelScope
import com.evora.technologies.office.core.domain.port.DocumentRepository
import com.evora.technologies.office.core.domain.port.PreferencesRepository
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.SortOrder
import com.evora.technologies.office.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeDocumentsViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val preferencesRepository: PreferencesRepository,
) : MviViewModel<HomeDocumentsState, HomeDocumentsIntent, HomeDocumentsEffect>(
    HomeDocumentsState(),
) {
    private val filterFlow = MutableStateFlow<DocFormat?>(null)
    private val sortFlow = MutableStateFlow(SortOrder.MODIFIED_DESC)

    init {
        combine(filterFlow, sortFlow) { filter, sort -> filter to sort }
            .flatMapLatest { (filter, sort) ->
                documentRepository.observeLibraryDocuments(filter = filter, sortOrder = sort)
            }
            .onEach { docs ->
                setState {
                    copy(
                        documents = docs,
                        isLoading = false,
                        isRefreshing = false,
                    )
                }
            }
            .catch { err ->
                setState { copy(isLoading = false, isRefreshing = false, errorMessage = err.message) }
            }
            .launchIn(viewModelScope)

        combine(
            preferencesRepository.externalOpenCount,
            preferencesRepository.defaultAppPromptDismissed,
        ) { count, dismissed -> count >= 2 && !dismissed }
            .onEach { show -> setState { copy(showDefaultAppBanner = show) } }
            .catch { /* non-fatal preference error */ }
            .launchIn(viewModelScope)

        preferencesRepository.defaultSortOrder
            .onEach { savedSort ->
                sortFlow.value = savedSort
                setState { copy(sortOrder = savedSort) }
            }
            .catch { /* keep default sort order */ }
            .launchIn(viewModelScope)

        refreshIndex()
    }

    override fun onIntent(intent: HomeDocumentsIntent) {
        when (intent) {
            is HomeDocumentsIntent.SelectFilter -> {
                filterFlow.value = intent.format
                setState { copy(selectedFilter = intent.format) }
            }
            is HomeDocumentsIntent.SelectSortOrder -> {
                sortFlow.value = intent.sortOrder
                setState { copy(sortOrder = intent.sortOrder) }
                launchSafely { preferencesRepository.setDefaultSortOrder(intent.sortOrder) }
            }
            HomeDocumentsIntent.RefreshRequested -> refreshIndex()
            is HomeDocumentsIntent.DocumentClicked -> handleDocumentClick(intent.document, intent.isTwoPaneTablet)
            is HomeDocumentsIntent.OpenDocumentForced -> openTargetDocument(intent.document)
            is HomeDocumentsIntent.ToggleFavorite -> launchSafely {
                documentRepository.toggleFavorite(intent.uriString)
            }
            is HomeDocumentsIntent.OpenContextMenu -> setState { copy(contextMenuDocId = intent.uriString) }
            HomeDocumentsIntent.DismissContextMenu -> setState { copy(contextMenuDocId = null) }
            is HomeDocumentsIntent.StartRename -> setState {
                copy(contextMenuDocId = null, renameTargetDocId = intent.uriString, renameError = null)
            }
            is HomeDocumentsIntent.ConfirmRename -> performRename(intent.uriString, intent.newBaseName)
            HomeDocumentsIntent.DismissRename -> setState { copy(renameTargetDocId = null, renameError = null) }
            is HomeDocumentsIntent.ShareDocument -> {
                setState { copy(contextMenuDocId = null) }
                sendEffect(HomeDocumentsEffect.ShareDocumentUri(intent.document))
            }
            is HomeDocumentsIntent.MoveToTrash -> performMoveToTrash(intent.uriString)
            is HomeDocumentsIntent.AddHomeShortcut -> {
                setState { copy(contextMenuDocId = null) }
                sendEffect(HomeDocumentsEffect.PinHomeShortcut(intent.document))
            }
            is HomeDocumentsIntent.OpenProperties -> setState {
                copy(contextMenuDocId = null, propertiesDocId = intent.uriString)
            }
            HomeDocumentsIntent.DismissProperties -> setState { copy(propertiesDocId = null) }
            HomeDocumentsIntent.DismissDefaultAppBanner -> launchSafely {
                preferencesRepository.setDefaultAppPromptDismissed(true)
            }
            HomeDocumentsIntent.OpenDefaultAppSettings -> sendEffect(HomeDocumentsEffect.LaunchDefaultAppSettings)
            HomeDocumentsIntent.ImportViaSafClicked -> sendEffect(HomeDocumentsEffect.LaunchSafPicker)
            is HomeDocumentsIntent.SafDocumentPicked -> registerSafDocument(intent.uriString)
        }
    }

    private fun refreshIndex() {
        launchSafely(
            onError = { err ->
                setState { copy(isLoading = false, isRefreshing = false, errorMessage = err.message) }
            },
        ) {
            setState { copy(isRefreshing = true, errorMessage = null) }
            documentRepository.refreshMediaStoreIndex()
                .onFailure { err ->
                    setState { copy(errorMessage = err.message ?: "Unable to scan documents") }
                }
            setState { copy(isLoading = false, isRefreshing = false) }
        }
    }

    private fun handleDocumentClick(document: DocumentItem, isTwoPaneTablet: Boolean) {
        if (isTwoPaneTablet) {
            setState { copy(selectedPreviewDocId = document.id) }
        } else {
            openTargetDocument(document)
        }
    }

    private fun openTargetDocument(document: DocumentItem) {
        if (document.format == DocFormat.ARCHIVE) {
            sendEffect(HomeDocumentsEffect.OpenArchiveInspector(document))
        } else {
            sendEffect(HomeDocumentsEffect.OpenDocument(document))
        }
    }

    private fun performRename(uriString: String, newBaseName: String) {
        launchSafely(
            onError = { err -> setState { copy(renameError = err.message ?: "Rename failed") } },
        ) {
            val result = documentRepository.renameDocument(uriString, newBaseName)
            result.fold(
                onSuccess = { renamed ->
                    setState { copy(renameTargetDocId = null, renameError = null) }
                    sendEffect(HomeDocumentsEffect.ShowMessage("Renamed to ${renamed.displayName}"))
                },
                onFailure = { err ->
                    setState { copy(renameError = err.message ?: "Invalid file name") }
                },
            )
        }
    }

    private fun performMoveToTrash(uriString: String) {
        launchSafely(
            onError = { err ->
                setState { copy(contextMenuDocId = null) }
                sendEffect(HomeDocumentsEffect.ShowMessage(err.message ?: "Unable to move to Trash"))
            },
        ) {
            setState { copy(contextMenuDocId = null) }
            val result = documentRepository.moveToTrash(uriString)
            result.fold(
                onSuccess = { trashed ->
                    sendEffect(HomeDocumentsEffect.ShowMessage("Moved ${trashed.displayName} to Trash"))
                },
                onFailure = { err ->
                    sendEffect(HomeDocumentsEffect.ShowMessage(err.message ?: "Unable to delete file"))
                },
            )
        }
    }

    private fun registerSafDocument(uriString: String?) {
        if (uriString.isNullOrBlank()) return
        launchSafely(
            onError = { err ->
                sendEffect(HomeDocumentsEffect.ShowMessage(err.message ?: "Unable to import SAF file"))
            },
        ) {
            documentRepository.registerSafUri(uriString).fold(
                onSuccess = { item -> openTargetDocument(item) },
                onFailure = { err ->
                    sendEffect(HomeDocumentsEffect.ShowMessage(err.message ?: "Unable to import SAF file"))
                },
            )
        }
    }
}
