package com.evora.technologies.office.feature.library.search

import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.mvi.UiEffect
import com.evora.technologies.office.core.mvi.UiIntent
import com.evora.technologies.office.core.mvi.UiState

data class SearchDocumentsState(
    val query: String = "",
    val selectedFilter: DocFormat? = null,
    val results: List<DocumentItem> = emptyList(),
    val recentQueries: List<String> = listOf("Contract", "Report", "HWPX", "Roadmap"),
    val isSearching: Boolean = false,
) : UiState

sealed interface SearchDocumentsIntent : UiIntent {
    data class QueryChanged(val query: String) : SearchDocumentsIntent
    data class SelectQuickQuery(val term: String) : SearchDocumentsIntent
    data class SelectFilter(val format: DocFormat?) : SearchDocumentsIntent
    data object ClearQuery : SearchDocumentsIntent
    data class DocumentClicked(val document: DocumentItem) : SearchDocumentsIntent
    data class ToggleFavorite(val uriString: String) : SearchDocumentsIntent
    data object BackClicked : SearchDocumentsIntent
}

sealed interface SearchDocumentsEffect : UiEffect {
    data object NavigateBack : SearchDocumentsEffect
    data class OpenDocument(val document: DocumentItem) : SearchDocumentsEffect
    data class OpenArchiveInspector(val document: DocumentItem) : SearchDocumentsEffect
}
