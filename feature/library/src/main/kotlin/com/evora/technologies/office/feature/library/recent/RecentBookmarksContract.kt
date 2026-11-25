package com.evora.technologies.office.feature.library.recent

import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.TrashItem
import com.evora.technologies.office.core.mvi.UiEffect
import com.evora.technologies.office.core.mvi.UiIntent
import com.evora.technologies.office.core.mvi.UiState

enum class RecentSubTab {
    RECENT,
    FAVORITES,
    TRASH,
}

data class RecentBookmarksState(
    val activeTab: RecentSubTab = RecentSubTab.RECENT,
    val recentDocuments: List<DocumentItem> = emptyList(),
    val favoriteDocuments: List<DocumentItem> = emptyList(),
    val trashItems: List<TrashItem> = emptyList(),
    val isLoading: Boolean = true,
    val showEmptyTrashConfirm: Boolean = false,
) : UiState

sealed interface RecentBookmarksIntent : UiIntent {
    data class SelectTab(val tab: RecentSubTab) : RecentBookmarksIntent
    data class DocumentClicked(val document: DocumentItem) : RecentBookmarksIntent
    data class ToggleFavorite(val uriString: String) : RecentBookmarksIntent
    data class RestoreTrash(val trashId: String) : RecentBookmarksIntent
    data class DeleteForever(val trashId: String) : RecentBookmarksIntent
    data object RequestEmptyTrash : RecentBookmarksIntent
    data object ConfirmEmptyTrash : RecentBookmarksIntent
    data object DismissEmptyTrash : RecentBookmarksIntent
}

sealed interface RecentBookmarksEffect : UiEffect {
    data class OpenDocument(val document: DocumentItem) : RecentBookmarksEffect
    data class OpenArchiveInspector(val document: DocumentItem) : RecentBookmarksEffect
    data class ShowMessage(val message: String) : RecentBookmarksEffect
}
