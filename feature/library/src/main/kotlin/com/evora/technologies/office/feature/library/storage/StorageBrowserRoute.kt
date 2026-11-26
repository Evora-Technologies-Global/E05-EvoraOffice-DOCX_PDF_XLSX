package com.evora.technologies.office.feature.library.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.office.core.designsystem.R
import com.evora.technologies.office.core.designsystem.component.EmptyStateView
import com.evora.technologies.office.core.designsystem.component.ErrorStateCard
import com.evora.technologies.office.core.designsystem.component.EvoraIconButton
import com.evora.technologies.office.core.designsystem.component.FormatBadge
import com.evora.technologies.office.core.designsystem.component.ShimmerDocListSkeleton
import com.evora.technologies.office.core.designsystem.component.formatByteSize
import com.evora.technologies.office.core.designsystem.component.formatEpochShort
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraTheme
import com.evora.technologies.office.core.designsystem.theme.EvoraThemeTokens
import com.evora.technologies.office.core.model.CloudProvider
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.StorageEntry
import com.evora.technologies.office.core.model.StorageRootKind
import com.evora.technologies.office.core.model.StorageVolumeRoot
import com.evora.technologies.office.core.mvi.CollectEffects
import java.io.File

@Composable
fun StorageBrowserRoute(
    isExpandedTwoPane: Boolean,
    onOpenDocument: (DocumentItem) -> Unit,
    onOpenArchiveInspector: (uriString: String, displayName: String) -> Unit,
    onOpenCloudDrive: (CloudProvider) -> Unit,
    onMergePdfs: (List<String>) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StorageBrowserViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val safTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri ->
        if (treeUri != null) {
            runCatching {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(treeUri, flags)
            }
            viewModel.onIntent(
                StorageBrowserIntent.SafTreePicked(
                    treeUriString = treeUri.toString(),
                    displayLabel = treeUri.lastPathSegment?.substringAfterLast(':'),
                ),
            )
        }
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is StorageBrowserEffect.OpenDocument -> onOpenDocument(effect.document)
            is StorageBrowserEffect.OpenArchiveInspector -> onOpenArchiveInspector(effect.uriString, effect.displayName)
            is StorageBrowserEffect.OpenCloudBrowser -> onOpenCloudDrive(effect.provider)
            is StorageBrowserEffect.ShareEntries -> shareMultipleEntries(context, effect.entries, onShowMessage)
            is StorageBrowserEffect.MergePdfUris -> onMergePdfs(effect.pdfUris)
            StorageBrowserEffect.LaunchSafTreePicker -> safTreeLauncher.launch(null)
            is StorageBrowserEffect.ShowMessage -> onShowMessage(effect.message)
        }
    }

    StorageBrowserScreen(
        state = state,
        isExpandedTwoPane = isExpandedTwoPane,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StorageBrowserScreen(
    state: StorageBrowserState,
    isExpandedTwoPane: Boolean = false,
    onIntent: (StorageBrowserIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = EvoraThemeTokens.colors
    val haptics = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxSize()) {
        // Header bar
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .padding(horizontal = EvoraSpacing.lg, vertical = EvoraSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.xs),
                    ) {
                        if (state.breadcrumbs.size > 1) {
                            EvoraIconButton(onClick = { onIntent(StorageBrowserIntent.NavigateUp) }) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Up")
                            }
                        }
                        Text(
                            text = if (state.isMultiSelectActive) {
                                "${state.selectedEntryIds.size} selected"
                            } else {
                                stringResource(R.string.evora_storage_title)
                            },
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.isMultiSelectActive) {
                            EvoraIconButton(onClick = { onIntent(StorageBrowserIntent.SelectAll) }) {
                                Icon(Icons.Outlined.SelectAll, contentDescription = "Select all")
                            }
                            TextButton(onClick = { onIntent(StorageBrowserIntent.ClearSelection) }) {
                                Text("Clear")
                            }
                        } else {
                            EvoraIconButton(onClick = { onIntent(StorageBrowserIntent.OpenSafFolderPicker) }) {
                                Icon(
                                    Icons.Outlined.FolderOpen,
                                    contentDescription = stringResource(R.string.evora_storage_pick_saf),
                                )
                            }
                            EvoraIconButton(onClick = { onIntent(StorageBrowserIntent.OpenNewFolderDialog) }) {
                                Icon(
                                    Icons.Outlined.CreateNewFolder,
                                    contentDescription = stringResource(R.string.evora_storage_new_folder),
                                )
                            }
                            EvoraIconButton(onClick = { onIntent(StorageBrowserIntent.Refresh) }) {
                                Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                            }
                        }
                    }
                }

                if (!isExpandedTwoPane) {
                    CloudQuickRow(
                        onOpenProvider = { onIntent(StorageBrowserIntent.OpenCloudProvider(it)) },
                    )
                    VolumeChipsRow(
                        roots = state.roots,
                        selectedRoot = state.selectedRoot,
                        onSelectRoot = { onIntent(StorageBrowserIntent.SelectRoot(it)) },
                        onAddSafTree = { onIntent(StorageBrowserIntent.OpenSafFolderPicker) },
                    )
                }
            }
        }

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (isExpandedTwoPane) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight(),
                    tonalElevation = 1.dp,
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(EvoraSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(EvoraSpacing.xs),
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.evora_cloud_title),
                                style = MaterialTheme.typography.labelLarge,
                                color = extra.textMuted,
                                modifier = Modifier.padding(horizontal = EvoraSpacing.sm, vertical = 4.dp),
                            )
                        }
                        item {
                            CloudSideRow(
                                title = stringResource(R.string.evora_cloud_google_drive),
                                tintBg = EvoraThemeTokens.formats.word.tintBg,
                                ink = EvoraThemeTokens.formats.word.inkText,
                                onClick = { onIntent(StorageBrowserIntent.OpenCloudProvider(CloudProvider.GOOGLE_DRIVE)) },
                            )
                        }
                        item {
                            CloudSideRow(
                                title = stringResource(R.string.evora_cloud_onedrive),
                                tintBg = EvoraThemeTokens.formats.hwp.tintBg,
                                ink = EvoraThemeTokens.formats.hwp.inkText,
                                onClick = { onIntent(StorageBrowserIntent.OpenCloudProvider(CloudProvider.ONE_DRIVE)) },
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(EvoraSpacing.sm))
                            Text(
                                text = stringResource(R.string.evora_nav_storage),
                                style = MaterialTheme.typography.labelLarge,
                                color = extra.textMuted,
                                modifier = Modifier.padding(horizontal = EvoraSpacing.sm, vertical = 4.dp),
                            )
                        }
                        items(state.roots, key = { it.id }) { root ->
                            val selected = state.selectedRoot?.id == root.id
                            Surface(
                                shape = EvoraRadii.md,
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(EvoraRadii.md)
                                    .clickable { onIntent(StorageBrowserIntent.SelectRoot(root)) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = EvoraSpacing.md, vertical = EvoraSpacing.sm),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                                ) {
                                    Icon(
                                        imageVector = root.icon(),
                                        contentDescription = null,
                                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = root.localizedLabel(),
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // Breadcrumbs
                if (state.breadcrumbs.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = EvoraSpacing.lg, vertical = EvoraSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        items(state.breadcrumbs.size) { idx ->
                            val crumb = state.breadcrumbs[idx]
                            val isLast = idx == state.breadcrumbs.lastIndex
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = { onIntent(StorageBrowserIntent.NavigateBreadcrumb(crumb.pathOrUri)) },
                                    modifier = Modifier.heightIn(min = EvoraSpacing.minTouchTarget),
                                ) {
                                    Text(
                                        text = crumb.title,
                                        fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (!isLast) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = extra.textMuted,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        state.isLoading -> {
                            Box(modifier = Modifier.padding(EvoraSpacing.lg)) {
                                ShimmerDocListSkeleton(itemCount = 6)
                            }
                        }
                        state.errorMessage != null -> {
                            Column(modifier = Modifier.padding(EvoraSpacing.lg)) {
                                ErrorStateCard(
                                    title = "Storage Access Limited",
                                    description = state.errorMessage,
                                    actionLabel = stringResource(R.string.evora_storage_pick_saf),
                                    onActionClick = { onIntent(StorageBrowserIntent.OpenSafFolderPicker) },
                                    isWarning = true,
                                )
                            }
                        }
                        state.entries.isEmpty() -> {
                            EmptyStateView(
                                title = stringResource(R.string.evora_storage_empty_title),
                                description = stringResource(R.string.evora_storage_empty_desc),
                                icon = Icons.Outlined.FolderOpen,
                                primaryActionLabel = stringResource(R.string.evora_storage_new_folder),
                                onPrimaryAction = { onIntent(StorageBrowserIntent.OpenNewFolderDialog) },
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                        else -> {
                            LazyColumn(
                                contentPadding = PaddingValues(EvoraSpacing.lg),
                                verticalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                            ) {
                                items(state.entries, key = { it.id }) { entry ->
                                    StorageEntryRow(
                                        entry = entry,
                                        isSelectionMode = state.isMultiSelectActive,
                                        isSelected = entry.id in state.selectedEntryIds,
                                        onClick = { onIntent(StorageBrowserIntent.OpenEntry(entry)) },
                                        onLongClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onIntent(StorageBrowserIntent.EnterMultiSelect(entry.id))
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                // Sticky multi-select action bar (Move, Copy, Delete, Share, Merge PDF)
                if (state.isMultiSelectActive) {
                    Surface(
                        tonalElevation = 8.dp,
                        shadowElevation = 6.dp,
                        color = extra.surfaceElevated,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = EvoraSpacing.sm, vertical = EvoraSpacing.xs),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BatchBarAction(
                                icon = Icons.AutoMirrored.Outlined.DriveFileMove,
                                label = stringResource(R.string.evora_storage_move),
                                enabled = !state.isBatchBusy,
                                onClick = { onIntent(StorageBrowserIntent.RequestBatchTransfer(BatchTransferAction.MOVE)) },
                            )
                            BatchBarAction(
                                icon = Icons.Outlined.ContentCopy,
                                label = stringResource(R.string.evora_storage_copy),
                                enabled = !state.isBatchBusy,
                                onClick = { onIntent(StorageBrowserIntent.RequestBatchTransfer(BatchTransferAction.COPY)) },
                            )
                            BatchBarAction(
                                icon = Icons.Outlined.DeleteOutline,
                                label = stringResource(R.string.evora_storage_delete),
                                enabled = !state.isBatchBusy,
                                onClick = { onIntent(StorageBrowserIntent.BatchTrashClicked) },
                            )
                            BatchBarAction(
                                icon = Icons.Outlined.Share,
                                label = stringResource(R.string.evora_storage_share),
                                enabled = !state.isBatchBusy,
                                onClick = { onIntent(StorageBrowserIntent.BatchShareClicked) },
                            )
                            BatchBarAction(
                                icon = Icons.Outlined.MergeType,
                                label = stringResource(R.string.evora_storage_merge_pdf),
                                enabled = !state.isBatchBusy && state.canMergeSelectedPdfs,
                                onClick = { onIntent(StorageBrowserIntent.BatchMergePdfClicked) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.showNewFolderDialog) {
        var folderName by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { onIntent(StorageBrowserIntent.DismissNewFolderDialog) },
            title = { Text(stringResource(R.string.evora_storage_new_folder)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(EvoraSpacing.xs)) {
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("Folder name") },
                        singleLine = true,
                        isError = state.newFolderError != null,
                    )
                    state.newFolderError?.let { err ->
                        Text(err, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onIntent(StorageBrowserIntent.CreateFolderConfirmed(folderName)) }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(StorageBrowserIntent.DismissNewFolderDialog) }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (state.pendingBatchAction != null) {
        val actionTitle = if (state.pendingBatchAction == BatchTransferAction.MOVE) {
            stringResource(R.string.evora_storage_move)
        } else {
            stringResource(R.string.evora_storage_copy)
        }
        ModalBottomSheet(onDismissRequest = { onIntent(StorageBrowserIntent.DismissBatchTransfer) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EvoraSpacing.lg, vertical = EvoraSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(EvoraSpacing.xs),
            ) {
                Text("$actionTitle to Destination", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(EvoraSpacing.xs))
                state.destinationCandidates.forEach { folder ->
                    ListItem(
                        headlineContent = { Text(folder.name) },
                        leadingContent = { Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            onIntent(StorageBrowserIntent.ConfirmBatchTransferTo(folder.pathOrUri))
                        },
                    )
                }
                state.roots.forEach { root ->
                    if (root.pathOrUri != state.currentPathOrUri) {
                        ListItem(
                            headlineContent = { Text(root.localizedLabel()) },
                            supportingContent = { Text(root.pathOrUri, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingContent = { Icon(root.icon(), null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable {
                                onIntent(StorageBrowserIntent.ConfirmBatchTransferTo(root.pathOrUri))
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(EvoraSpacing.xl))
            }
        }
    }

    if (state.unsupportedRarName != null) {
        AlertDialog(
            onDismissRequest = { onIntent(StorageBrowserIntent.DismissRarDialog) },
            title = { Text("Unsupported Archive (${state.unsupportedRarName})") },
            text = {
                Text("Evora Office supports .zip and .7z offline archives. Proprietary .rar archives are not supported.")
            },
            confirmButton = {
                TextButton(onClick = { onIntent(StorageBrowserIntent.DismissRarDialog) }) {
                    Text("OK")
                }
            },
        )
    }
}

@Composable
private fun CloudQuickRow(
    onOpenProvider: (CloudProvider) -> Unit,
) {
    val formats = EvoraThemeTokens.formats
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = EvoraSpacing.lg, vertical = EvoraSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
    ) {
        Surface(
            shape = EvoraRadii.md,
            color = formats.word.tintBg,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clip(EvoraRadii.md)
                .clickable { onOpenProvider(CloudProvider.GOOGLE_DRIVE) },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = EvoraSpacing.md, vertical = EvoraSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
            ) {
                Icon(Icons.Outlined.CloudQueue, contentDescription = null, tint = formats.word.inkText)
                Text(
                    text = stringResource(R.string.evora_cloud_google_drive),
                    style = MaterialTheme.typography.labelLarge,
                    color = formats.word.inkText,
                    maxLines = 1,
                )
            }
        }
        Surface(
            shape = EvoraRadii.md,
            color = formats.hwp.tintBg,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clip(EvoraRadii.md)
                .clickable { onOpenProvider(CloudProvider.ONE_DRIVE) },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = EvoraSpacing.md, vertical = EvoraSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
            ) {
                Icon(Icons.Outlined.CloudQueue, contentDescription = null, tint = formats.hwp.inkText)
                Text(
                    text = stringResource(R.string.evora_cloud_onedrive),
                    style = MaterialTheme.typography.labelLarge,
                    color = formats.hwp.inkText,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun CloudSideRow(
    title: String,
    tintBg: androidx.compose.ui.graphics.Color,
    ink: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = EvoraRadii.md,
        color = tintBg,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(EvoraRadii.md)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = EvoraSpacing.md, vertical = EvoraSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
        ) {
            Icon(Icons.Outlined.CloudQueue, contentDescription = null, tint = ink)
            Text(title, style = MaterialTheme.typography.titleSmall, color = ink)
        }
    }
}

@Composable
private fun VolumeChipsRow(
    roots: List<StorageVolumeRoot>,
    selectedRoot: StorageVolumeRoot?,
    onSelectRoot: (StorageVolumeRoot) -> Unit,
    onAddSafTree: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = EvoraSpacing.lg, vertical = EvoraSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
    ) {
        items(roots, key = { it.id }) { root ->
            FilterChip(
                selected = selectedRoot?.id == root.id,
                onClick = { onSelectRoot(root) },
                label = { Text(root.localizedLabel()) },
                leadingIcon = { Icon(root.icon(), contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.heightIn(min = EvoraSpacing.minTouchTarget),
            )
        }
        item(key = "add_saf_chip") {
            FilterChip(
                selected = false,
                onClick = onAddSafTree,
                label = { Text(stringResource(R.string.evora_storage_pick_saf)) },
                leadingIcon = { Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.heightIn(min = EvoraSpacing.minTouchTarget),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StorageEntryRow(
    entry: StorageEntry,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val extra = EvoraThemeTokens.colors
    Surface(
        shape = EvoraRadii.md,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) extra.brandAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(EvoraRadii.md)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = EvoraSpacing.md, vertical = EvoraSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                )
                Spacer(modifier = Modifier.width(EvoraSpacing.xs))
            }
            if (entry.isDirectory) {
                Surface(
                    shape = EvoraRadii.sm,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            } else {
                FormatBadge(
                    format = entry.format ?: DocFormat.TEXT,
                    extension = entry.extension,
                )
            }
            Spacer(modifier = Modifier.width(EvoraSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = if (entry.isDirectory) {
                    "${entry.childCount} items · ${formatEpochShort(entry.modifiedAtEpochMs)}"
                } else {
                    "${formatByteSize(entry.sizeBytes)} · ${formatEpochShort(entry.modifiedAtEpochMs)}"
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    color = extra.textMuted,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun BatchBarAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (enabled) MaterialTheme.colorScheme.primary else EvoraThemeTokens.colors.textMuted.copy(alpha = 0.45f)
    Column(
        modifier = Modifier
            .heightIn(min = 52.dp)
            .clip(EvoraRadii.sm)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
    }
}

@Composable
private fun StorageVolumeRoot.localizedLabel(): String = when (kind) {
    StorageRootKind.INTERNAL -> stringResource(R.string.evora_storage_internal)
    StorageRootKind.DOWNLOADS -> stringResource(R.string.evora_storage_downloads)
    StorageRootKind.SD_CARD -> stringResource(R.string.evora_storage_sd_card)
    StorageRootKind.SAF_TREE -> title
}

private fun StorageVolumeRoot.icon() = when (kind) {
    StorageRootKind.INTERNAL -> Icons.Outlined.Storage
    StorageRootKind.DOWNLOADS -> Icons.Outlined.FolderOpen
    StorageRootKind.SD_CARD -> Icons.Outlined.SdStorage
    StorageRootKind.SAF_TREE -> Icons.Outlined.Folder
}

private fun shareMultipleEntries(
    context: Context,
    entries: List<StorageEntry>,
    onShowMessage: (String) -> Unit,
) {
    runCatching {
        val uris = ArrayList<Uri>()
        entries.filter { !it.isDirectory }.forEach { entry ->
            val parsed = Uri.parse(entry.uriString)
            val shareUri = if (!entry.isSafEntry && parsed.scheme == "file" && parsed.path != null) {
                val srcFile = File(parsed.path!!)
                runCatching {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", srcFile)
                }.getOrElse {
                    val tempDir = File(context.cacheDir, "shared_temp").apply { mkdirs() }
                    val safeTemp = File(tempDir, srcFile.name.replace(Regex("[^A-Za-z0-9._-]"), "_"))
                    srcFile.copyTo(safeTemp, overwrite = true)
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", safeTemp)
                }
            } else {
                parsed
            }
            uris += shareUri
        }
        if (uris.isEmpty()) {
            onShowMessage("Select files to share")
            return
        }
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        context.startActivity(Intent.createChooser(intent, "Share ${uris.size} file(s)"))
    }.onFailure {
        onShowMessage("Unable to share selected files")
    }
}

@Preview(showBackground = true)
@Composable
private fun StorageBrowserPreview() {
    EvoraTheme {
        StorageBrowserScreen(
            state = StorageBrowserState(
                isLoading = false,
                entries = listOf(
                    StorageEntry("1", "file:///docs/Contracts", "/docs/Contracts", "Contracts", true, 0L, 100L, childCount = 4),
                    StorageEntry("2", "file:///docs/Q4.pdf", "/docs/Q4.pdf", "Q4.pdf", false, 120_000L, 200L, format = DocFormat.PDF, extension = "pdf"),
                ),
            ),
            onIntent = {},
        )
    }
}
