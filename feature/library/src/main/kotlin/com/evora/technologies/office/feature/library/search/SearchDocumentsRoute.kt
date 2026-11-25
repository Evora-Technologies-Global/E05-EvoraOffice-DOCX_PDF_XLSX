package com.evora.technologies.office.feature.library.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.office.core.designsystem.component.EmptyStateView
import com.evora.technologies.office.core.designsystem.component.EvoraIconButton
import com.evora.technologies.office.core.designsystem.component.FormatBadge
import com.evora.technologies.office.core.designsystem.component.FormatFilterChip
import com.evora.technologies.office.core.designsystem.component.formatByteSize
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraTabularMonoStyle
import com.evora.technologies.office.core.designsystem.theme.EvoraTheme
import com.evora.technologies.office.core.designsystem.theme.EvoraThemeTokens
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.mvi.CollectEffects

@Composable
fun SearchDocumentsRoute(
    onBack: () -> Unit,
    onOpenDocument: (DocumentItem) -> Unit,
    onOpenArchiveInspector: (DocumentItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchDocumentsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is SearchDocumentsEffect.OpenDocument -> onOpenDocument(effect.document)
            is SearchDocumentsEffect.OpenArchiveInspector -> onOpenArchiveInspector(effect.document)
        }
    }

    SearchDocumentsScreen(
        state = state,
        onBack = onBack,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
internal fun SearchDocumentsScreen(
    state: SearchDocumentsState,
    onBack: () -> Unit,
    onIntent: (SearchDocumentsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
    ) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(EvoraSpacing.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EvoraIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = { onIntent(SearchDocumentsIntent.QueryChanged(it)) },
                        placeholder = { Text("Search filename or folder...") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                    )
                    if (state.query.isNotEmpty()) {
                        EvoraIconButton(onClick = { onIntent(SearchDocumentsIntent.ClearQuery) }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                        }
                    }
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = EvoraSpacing.sm, vertical = EvoraSpacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                ) {
                    item {
                        FormatFilterChip(
                            label = "All",
                            selected = state.selectedFilter == null,
                            onClick = { onIntent(SearchDocumentsIntent.SelectFilter(null)) },
                        )
                    }
                    items(DocFormat.entries, key = { it.name }) { fmt ->
                        FormatFilterChip(
                            label = fmt.name,
                            selected = state.selectedFilter == fmt,
                            onClick = { onIntent(SearchDocumentsIntent.SelectFilter(fmt)) },
                        )
                    }
                }
            }
        }

        if (state.query.isBlank()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = EvoraSpacing.lg, vertical = EvoraSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
            ) {
                items(state.recentQueries, key = { it }) { term ->
                    FormatFilterChip(
                        label = term,
                        selected = false,
                        onClick = { onIntent(SearchDocumentsIntent.SelectQuickQuery(term)) },
                    )
                }
            }
        }

        if (state.results.isEmpty() && !state.isSearching) {
            EmptyStateView(
                title = "No matching files",
                description = if (state.query.isBlank()) {
                    "Type a filename or folder name to search."
                } else {
                    "No files matched \"${state.query}\"."
                },
                icon = Icons.Outlined.SearchOff,
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(EvoraSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
            ) {
                item(key = "count") {
                    Text(
                        text = "${state.results.size} result(s)",
                        style = EvoraTabularMonoStyle,
                        color = EvoraThemeTokens.colors.textMuted,
                    )
                }
                items(state.results, key = { it.id }) { doc ->
                    HighlightedSearchCard(
                        document = doc,
                        query = state.query,
                        onClick = { onIntent(SearchDocumentsIntent.DocumentClicked(doc)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightedSearchCard(
    document: DocumentItem,
    query: String,
    onClick: () -> Unit,
) {
    val highlightBg = MaterialTheme.colorScheme.primaryContainer
    val highlightInk = MaterialTheme.colorScheme.onPrimaryContainer
    val annotatedTitle = remember(document.displayName, query) {
        buildHighlightedText(document.displayName, query, highlightBg, highlightInk)
    }
    Surface(
        shape = EvoraRadii.md,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(EvoraSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FormatBadge(format = document.format, extension = document.extension)
            Spacer(modifier = Modifier.width(EvoraSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = annotatedTitle, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${formatByteSize(document.sizeBytes)} · ${document.parentFolderName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = EvoraThemeTokens.colors.textMuted,
                )
            }
        }
    }
}

private fun buildHighlightedText(
    source: String,
    query: String,
    highlightBg: androidx.compose.ui.graphics.Color,
    highlightInk: androidx.compose.ui.graphics.Color,
) = buildAnnotatedString {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) {
        append(source)
        return@buildAnnotatedString
    }
    val lowerSource = source.lowercase()
    val lowerQuery = trimmed.lowercase()
    var cursor = 0
    while (cursor < source.length) {
        val idx = lowerSource.indexOf(lowerQuery, startIndex = cursor)
        if (idx < 0) {
            append(source.substring(cursor))
            break
        }
        if (idx > cursor) append(source.substring(cursor, idx))
        withStyle(
            SpanStyle(
                background = highlightBg,
                color = highlightInk,
                fontWeight = FontWeight.Bold,
            ),
        ) {
            append(source.substring(idx, idx + trimmed.length))
        }
        cursor = idx + trimmed.length
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchDocumentsPreview() {
    EvoraTheme {
        SearchDocumentsScreen(
            state = SearchDocumentsState(query = "Contract"),
            onBack = {},
            onIntent = {},
        )
    }
}
