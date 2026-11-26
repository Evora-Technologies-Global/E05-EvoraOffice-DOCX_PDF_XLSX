package com.evora.technologies.office.feature.library.search

import androidx.lifecycle.viewModelScope
import com.evora.technologies.office.core.domain.port.DocumentRepository
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchDocumentsViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
) : MviViewModel<SearchDocumentsState, SearchDocumentsIntent, SearchDocumentsEffect>(
    SearchDocumentsState(),
) {
    private val queryFlow = MutableStateFlow("")
    private val filterFlow = MutableStateFlow<DocFormat?>(null)

    init {
        combine(
            queryFlow.debounce(300L),
            filterFlow,
        ) { debouncedQuery, filter -> debouncedQuery to filter }
            .flatMapLatest { (q, filter) ->
                documentRepository.searchDocuments(query = q, filter = filter)
            }
            .onEach { matches ->
                setState { copy(results = matches, isSearching = false) }
            }
            .catch { setState { copy(isSearching = false) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: SearchDocumentsIntent) {
        when (intent) {
            is SearchDocumentsIntent.QueryChanged -> {
                queryFlow.value = intent.query
                setState { copy(query = intent.query, isSearching = true) }
            }
            is SearchDocumentsIntent.SelectQuickQuery -> {
                queryFlow.value = intent.term
                setState { copy(query = intent.term, isSearching = true) }
            }
            is SearchDocumentsIntent.SelectFilter -> {
                filterFlow.value = intent.format
                setState { copy(selectedFilter = intent.format, isSearching = true) }
            }
            SearchDocumentsIntent.ClearQuery -> {
                queryFlow.value = ""
                setState { copy(query = "", isSearching = false) }
            }
            is SearchDocumentsIntent.DocumentClicked -> {
                val q = currentState.query.trim()
                if (q.isNotEmpty() && q !in currentState.recentQueries) {
                    setState { copy(recentQueries = (listOf(q) + recentQueries).take(6)) }
                }
                if (intent.document.format == DocFormat.ARCHIVE) {
                    sendEffect(SearchDocumentsEffect.OpenArchiveInspector(intent.document))
                } else {
                    sendEffect(SearchDocumentsEffect.OpenDocument(intent.document))
                }
            }
            is SearchDocumentsIntent.ToggleFavorite -> launchSafely {
                documentRepository.toggleFavorite(intent.uriString)
            }
            SearchDocumentsIntent.BackClicked -> sendEffect(SearchDocumentsEffect.NavigateBack)
        }
    }
}
