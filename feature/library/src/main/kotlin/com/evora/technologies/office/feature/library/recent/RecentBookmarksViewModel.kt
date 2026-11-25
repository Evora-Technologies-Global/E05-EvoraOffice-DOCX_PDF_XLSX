package com.evora.technologies.office.feature.library.recent

import androidx.lifecycle.viewModelScope
import com.evora.technologies.office.core.domain.port.DocumentRepository
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class RecentBookmarksViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
) : MviViewModel<RecentBookmarksState, RecentBookmarksIntent, RecentBookmarksEffect>(
    RecentBookmarksState(),
) {

    init {
        combine(
            documentRepository.observeRecentDocuments(),
            documentRepository.observeFavoriteDocuments(),
            documentRepository.observeTrashItems(),
        ) { recents, favs, trash ->
            Triple(recents, favs, trash)
        }
            .onEach { (recents, favs, trash) ->
                setState {
                    copy(
                        recentDocuments = recents,
                        favoriteDocuments = favs,
                        trashItems = trash,
                        isLoading = false,
                    )
                }
            }
            .catch { setState { copy(isLoading = false) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: RecentBookmarksIntent) {
        when (intent) {
            is RecentBookmarksIntent.SelectTab -> setState { copy(activeTab = intent.tab) }
            is RecentBookmarksIntent.DocumentClicked -> {
                if (intent.document.format == DocFormat.ARCHIVE) {
                    sendEffect(RecentBookmarksEffect.OpenArchiveInspector(intent.document))
                } else {
                    sendEffect(RecentBookmarksEffect.OpenDocument(intent.document))
                }
            }
            is RecentBookmarksIntent.ToggleFavorite -> launchSafely {
                documentRepository.toggleFavorite(intent.uriString)
            }
            is RecentBookmarksIntent.RestoreTrash -> launchSafely(
                onError = { err ->
                    sendEffect(RecentBookmarksEffect.ShowMessage(err.message ?: "Restore failed"))
                },
            ) {
                documentRepository.restoreFromTrash(intent.trashId).fold(
                    onSuccess = { sendEffect(RecentBookmarksEffect.ShowMessage("Restored file cleanly")) },
                    onFailure = { err ->
                        sendEffect(RecentBookmarksEffect.ShowMessage(err.message ?: "Restore failed"))
                    },
                )
            }
            is RecentBookmarksIntent.DeleteForever -> launchSafely {
                documentRepository.deleteForeverFromTrash(intent.trashId)
                sendEffect(RecentBookmarksEffect.ShowMessage("Deleted permanently"))
            }
            RecentBookmarksIntent.RequestEmptyTrash -> setState { copy(showEmptyTrashConfirm = true) }
            RecentBookmarksIntent.DismissEmptyTrash -> setState { copy(showEmptyTrashConfirm = false) }
            RecentBookmarksIntent.ConfirmEmptyTrash -> launchSafely {
                setState { copy(showEmptyTrashConfirm = false) }
                val removed = documentRepository.emptyTrash().getOrDefault(0)
                sendEffect(RecentBookmarksEffect.ShowMessage("Emptied $removed item(s) from Trash"))
            }
        }
    }
}
