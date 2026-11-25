package com.evora.technologies.office.feature.library.recent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.office.core.designsystem.component.DocCard
import com.evora.technologies.office.core.designsystem.component.EmptyStateView
import com.evora.technologies.office.core.designsystem.component.EvoraIconButton
import com.evora.technologies.office.core.designsystem.component.FormatBadge
import com.evora.technologies.office.core.designsystem.component.formatByteSize
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraTheme
import com.evora.technologies.office.core.designsystem.theme.EvoraThemeTokens
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.TrashItem
import com.evora.technologies.office.core.mvi.CollectEffects
import kotlin.math.max

@Composable
fun RecentBookmarksRoute(
    onOpenDocument: (DocumentItem) -> Unit,
    onOpenArchiveInspector: (DocumentItem) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecentBookmarksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is RecentBookmarksEffect.OpenDocument -> onOpenDocument(effect.document)
            is RecentBookmarksEffect.OpenArchiveInspector -> onOpenArchiveInspector(effect.document)
            is RecentBookmarksEffect.ShowMessage -> onShowMessage(effect.message)
        }
    }

    RecentBookmarksScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
internal fun RecentBookmarksScreen(
    state: RecentBookmarksState,
    onIntent: (RecentBookmarksIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = EvoraThemeTokens.colors
    Column(modifier = modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(horizontal = EvoraSpacing.lg, vertical = EvoraSpacing.sm)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Recent & Trash",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (state.activeTab == RecentSubTab.TRASH && state.trashItems.isNotEmpty()) {
                        TextButton(onClick = { onIntent(RecentBookmarksIntent.RequestEmptyTrash) }) {
                            Text("Empty Trash", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = EvoraSpacing.xs),
                ) {
                    RecentSubTab.entries.forEachIndexed { index, tab ->
                        val label = when (tab) {
                            RecentSubTab.RECENT -> "Recent"
                            RecentSubTab.FAVORITES -> "Favorites"
                            RecentSubTab.TRASH -> "Trash (${state.trashItems.size})"
                        }
                        SegmentedButton(
                            selected = state.activeTab == tab,
                            onClick = { onIntent(RecentBookmarksIntent.SelectTab(tab)) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = RecentSubTab.entries.size),
                        ) {
                            Text(label, maxLines = 1)
                        }
                    }
                }
            }
        }

        when (state.activeTab) {
            RecentSubTab.RECENT -> {
                if (state.recentDocuments.isEmpty()) {
                    EmptyStateView(
                        title = "No recent documents",
                        description = "Documents you open appear here with exact page progress.",
                        icon = Icons.Outlined.History,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(EvoraSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                    ) {
                        items(state.recentDocuments, key = { it.uriString }) { doc ->
                            DocCard(
                                document = doc,
                                onClick = { onIntent(RecentBookmarksIntent.DocumentClicked(doc)) },
                                onToggleFavorite = { onIntent(RecentBookmarksIntent.ToggleFavorite(doc.uriString)) },
                                onMoreClick = { onIntent(RecentBookmarksIntent.DocumentClicked(doc)) },
                            )
                        }
                    }
                }
            }
            RecentSubTab.FAVORITES -> {
                if (state.favoriteDocuments.isEmpty()) {
                    EmptyStateView(
                        title = "No favorites yet",
                        description = "Tap the star on any document card to pin it here.",
                        icon = Icons.Outlined.StarBorder,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(EvoraSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                    ) {
                        items(state.favoriteDocuments, key = { it.uriString }) { doc ->
                            DocCard(
                                document = doc,
                                onClick = { onIntent(RecentBookmarksIntent.DocumentClicked(doc)) },
                                onToggleFavorite = { onIntent(RecentBookmarksIntent.ToggleFavorite(doc.uriString)) },
                                onMoreClick = { onIntent(RecentBookmarksIntent.DocumentClicked(doc)) },
                            )
                        }
                    }
                }
            }
            RecentSubTab.TRASH -> {
                if (state.trashItems.isEmpty()) {
                    EmptyStateView(
                        title = "Trash is empty",
                        description = "Deleted documents stay recoverable here for 30 days.",
                        icon = Icons.Outlined.DeleteSweep,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(EvoraSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                    ) {
                        item(key = "trash_policy_banner") {
                            Surface(
                                shape = EvoraRadii.md,
                                color = extra.warningContainer,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "Deleted files stay recoverable for 30 days before auto-purge.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = extra.onWarningContainer,
                                    modifier = Modifier.padding(EvoraSpacing.md),
                                )
                            }
                        }
                        items(state.trashItems, key = { it.trashId }) { item ->
                            TrashItemCard(
                                item = item,
                                onRestore = { onIntent(RecentBookmarksIntent.RestoreTrash(item.trashId)) },
                                onDeleteForever = { onIntent(RecentBookmarksIntent.DeleteForever(item.trashId)) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.showEmptyTrashConfirm) {
        AlertDialog(
            onDismissRequest = { onIntent(RecentBookmarksIntent.DismissEmptyTrash) },
            title = { Text("Empty Trash?") },
            text = { Text("Permanently remove all ${state.trashItems.size} item(s)? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onIntent(RecentBookmarksIntent.ConfirmEmptyTrash) }) {
                    Text("Empty Trash", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(RecentBookmarksIntent.DismissEmptyTrash) }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun TrashItemCard(
    item: TrashItem,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
) {
    val daysLeft = max(
        0,
        ((item.expiresAtEpochMs - System.currentTimeMillis()) / (24L * 60 * 60 * 1000)).toInt(),
    )
    Surface(
        shape = EvoraRadii.md,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(EvoraSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FormatBadge(format = item.format, extension = item.extension)
            Spacer(modifier = Modifier.width(EvoraSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.displayName, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${formatByteSize(item.sizeBytes)} · $daysLeft day(s) left",
                    style = MaterialTheme.typography.labelMedium,
                    color = EvoraThemeTokens.colors.textMuted,
                )
            }
            TextButton(onClick = onRestore) {
                Text("Restore")
            }
            EvoraIconButton(onClick = onDeleteForever) {
                Icon(
                    imageVector = Icons.Outlined.DeleteSweep,
                    contentDescription = "Delete ${item.displayName} forever",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecentBookmarksPreview() {
    EvoraTheme {
        RecentBookmarksScreen(
            state = RecentBookmarksState(),
            onIntent = {},
        )
    }
}
