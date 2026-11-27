package com.evora.technologies.office.feature.library.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.ReportDrawnWhen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AppShortcut
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.office.core.designsystem.component.DocCard
import com.evora.technologies.office.core.designsystem.component.EmptyStateView
import com.evora.technologies.office.core.designsystem.component.EvoraBrandMark
import com.evora.technologies.office.core.designsystem.component.EvoraIconButton
import com.evora.technologies.office.core.designsystem.component.EvoraOutlinedButton
import com.evora.technologies.office.core.designsystem.component.EvoraPrimaryButton
import com.evora.technologies.office.core.designsystem.component.EvoraTemplatesSheet
import com.evora.technologies.office.core.designsystem.component.FormatBadge
import com.evora.technologies.office.core.designsystem.component.FormatFilterChip
import com.evora.technologies.office.core.designsystem.component.ShimmerDocListSkeleton
import com.evora.technologies.office.core.designsystem.component.formatByteSize
import com.evora.technologies.office.core.designsystem.component.formatEpochShort
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraTabularMonoStyle
import com.evora.technologies.office.core.designsystem.theme.EvoraTheme
import com.evora.technologies.office.core.designsystem.theme.EvoraThemeTokens
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.SortOrder
import com.evora.technologies.office.core.mvi.CollectEffects
import java.io.File

private data class FilterChipEntry(
    val format: DocFormat?,
    val label: String,
)

private val HOME_FILTER_CHIPS = listOf(
    FilterChipEntry(null, "All"),
    FilterChipEntry(DocFormat.PDF, "PDF"),
    FilterChipEntry(DocFormat.WORD, "Word"),
    FilterChipEntry(DocFormat.EXCEL, "Excel"),
    FilterChipEntry(DocFormat.PPT, "PPT"),
    FilterChipEntry(DocFormat.ODF, "ODF"),
    FilterChipEntry(DocFormat.HWP, "HWP"),
    FilterChipEntry(DocFormat.EPUB, "EPUB"),
    FilterChipEntry(DocFormat.TEXT, "TXT"),
    FilterChipEntry(DocFormat.ARCHIVE, "ZIP/7z"),
)

@Composable
fun HomeDocumentsRoute(
    isMediumOrExpanded: Boolean,
    isExpandedTwoPane: Boolean,
    onOpenSearch: () -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenDocument: (DocumentItem) -> Unit,
    onOpenEditor: (DocumentItem) -> Unit = onOpenDocument,
    onConvertDocumentToPdf: (DocumentItem) -> Unit = {},
    onOpenArchiveInspector: (DocumentItem) -> Unit,
    openTemplatesTrigger: Int = 0,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeDocumentsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(openTemplatesTrigger) {
        if (openTemplatesTrigger > 0) {
            viewModel.onIntent(HomeDocumentsIntent.OpenTemplatesSheet)
        }
    }

    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
        viewModel.onIntent(HomeDocumentsIntent.SafDocumentPicked(uri?.toString()))
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            HomeDocumentsEffect.NavigateToSearch -> onOpenSearch()
            HomeDocumentsEffect.NavigateToPaywall -> onOpenPaywall()
            is HomeDocumentsEffect.OpenDocument -> onOpenDocument(effect.document)
            is HomeDocumentsEffect.OpenEditor -> onOpenEditor(effect.document)
            is HomeDocumentsEffect.OpenConvertToPdf -> onConvertDocumentToPdf(effect.document)
            is HomeDocumentsEffect.OpenArchiveInspector -> onOpenArchiveInspector(effect.document)
            is HomeDocumentsEffect.ShareDocumentUri -> {
                shareDocument(context, effect.document, onShowMessage)
            }
            is HomeDocumentsEffect.PinHomeShortcut -> {
                pinShortcut(context, effect.document, onShowMessage)
            }
            HomeDocumentsEffect.LaunchDefaultAppSettings -> {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                runCatching { context.startActivity(intent) }
                    .onFailure { onShowMessage("Open Android Settings -> Default apps to configure.") }
            }
            HomeDocumentsEffect.LaunchSafPicker -> safLauncher.launch(arrayOf("*/*"))
            is HomeDocumentsEffect.ShowMessage -> onShowMessage(effect.message)
        }
    }

    ReportDrawnWhen { !state.isLoading }

    HomeDocumentsScreen(
        state = state,
        isMediumGrid = isMediumOrExpanded && !isExpandedTwoPane,
        isExpandedTwoPane = isExpandedTwoPane,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeDocumentsScreen(
    state: HomeDocumentsState,
    isMediumGrid: Boolean,
    isExpandedTwoPane: Boolean,
    onIntent: (HomeDocumentsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = EvoraThemeTokens.colors
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // Top App Bar (S-06)
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
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                    ) {
                        EvoraBrandMark(size = 32.dp)
                        Text(
                            text = "Evora Office",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EvoraIconButton(onClick = { onIntent(HomeDocumentsIntent.SearchClicked) }) {
                            Icon(Icons.Outlined.Search, contentDescription = "Search documents")
                        }
                        EvoraIconButton(onClick = { onIntent(HomeDocumentsIntent.OpenTemplatesSheet) }) {
                            Icon(Icons.AutoMirrored.Outlined.NoteAdd, contentDescription = "New document from template")
                        }
                        EvoraIconButton(onClick = { onIntent(HomeDocumentsIntent.ImportViaSafClicked) }) {
                            Icon(Icons.Outlined.Add, contentDescription = "Open file with Folder Picker")
                        }
                        EvoraIconButton(onClick = { onIntent(HomeDocumentsIntent.PaywallClicked) }) {
                            Icon(
                                Icons.Outlined.WorkspacePremium,
                                contentDescription = "Evora Pro",
                                tint = extra.onProGoldContainer,
                            )
                        }
                    }
                }

                // 9+ Format Filter Chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = EvoraSpacing.lg, vertical = EvoraSpacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                ) {
                    items(HOME_FILTER_CHIPS, key = { it.label }) { entry ->
                        FormatFilterChip(
                            label = entry.label,
                            selected = state.selectedFilter == entry.format,
                            onClick = { onIntent(HomeDocumentsIntent.SelectFilter(entry.format)) },
                        )
                    }
                }
            }
        }

        // Main Body (Compact list / Medium 2-col grid / Expanded two-pane)
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onIntent(HomeDocumentsIntent.RefreshRequested) },
                modifier = Modifier
                    .let { if (isExpandedTwoPane) it.width(380.dp) else it.weight(1f) }
                    .fillMaxHeight(),
            ) {
                when {
                    state.isLoading -> {
                        Box(modifier = Modifier.padding(EvoraSpacing.lg)) {
                            ShimmerDocListSkeleton(itemCount = 6)
                        }
                    }
                    state.documents.isEmpty() -> {
                        EmptyStateView(
                            title = "No documents found",
                            description = "Pick a file via SAF or grant storage access to index local documents.",
                            icon = Icons.Outlined.FolderOpen,
                            primaryActionLabel = "Open Folder Picker (SAF)",
                            onPrimaryAction = { onIntent(HomeDocumentsIntent.ImportViaSafClicked) },
                            modifier = Modifier
                                .align(Alignment.Center)
                                .testTag("home_documents_empty"),
                        )
                    }
                    isMediumGrid -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.testTag("home_documents_grid"),
                            contentPadding = PaddingValues(EvoraSpacing.lg),
                            horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(EvoraSpacing.md),
                        ) {
                            items(state.documents, key = { it.id }) { doc ->
                                DocCard(
                                    document = doc,
                                    onClick = { onIntent(HomeDocumentsIntent.DocumentClicked(doc, false)) },
                                    onToggleFavorite = { onIntent(HomeDocumentsIntent.ToggleFavorite(doc.uriString)) },
                                    onMoreClick = { onIntent(HomeDocumentsIntent.OpenContextMenu(doc.uriString)) },
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.testTag("home_documents_list"),
                            contentPadding = PaddingValues(EvoraSpacing.lg),
                            verticalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                        ) {
                            if (state.showDefaultAppBanner) {
                                item(key = "default_banner") {
                                    Surface(
                                        shape = EvoraRadii.md,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(EvoraSpacing.md),
                                            verticalArrangement = Arrangement.spacedBy(EvoraSpacing.xs),
                                        ) {
                                            Text(
                                                text = "Set Evora Office as default PDF & DOCX viewer",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                TextButton(onClick = { onIntent(HomeDocumentsIntent.DismissDefaultAppBanner) }) {
                                                    Text("Hide")
                                                }
                                                TextButton(onClick = { onIntent(HomeDocumentsIntent.OpenDefaultAppSettings) }) {
                                                    Text("Set Default")
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item(key = "sort_bar") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box {
                                        TextButton(
                                            onClick = { sortMenuExpanded = true },
                                            modifier = Modifier.heightIn(min = EvoraSpacing.minTouchTarget),
                                        ) {
                                            Icon(Icons.Outlined.Sort, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Sort: ${state.sortOrder.displayLabel()}")
                                        }
                                        DropdownMenu(
                                            expanded = sortMenuExpanded,
                                            onDismissRequest = { sortMenuExpanded = false },
                                        ) {
                                            SortOrder.entries.forEach { order ->
                                                DropdownMenuItem(
                                                    text = { Text(order.displayLabel()) },
                                                    onClick = {
                                                        sortMenuExpanded = false
                                                        onIntent(HomeDocumentsIntent.SelectSortOrder(order))
                                                    },
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${state.documents.size} files",
                                        style = EvoraTabularMonoStyle,
                                        color = extra.textMuted,
                                    )
                                }
                            }

                            itemsIndexed(state.documents, key = { _, doc -> doc.id }) { index, doc ->
                                DocCard(
                                    document = doc,
                                    isSelected = isExpandedTwoPane && state.selectedPreviewDoc?.id == doc.id,
                                    onClick = {
                                        onIntent(
                                            HomeDocumentsIntent.DocumentClicked(
                                                document = doc,
                                                isTwoPaneTablet = isExpandedTwoPane,
                                            ),
                                        )
                                    },
                                    onToggleFavorite = { onIntent(HomeDocumentsIntent.ToggleFavorite(doc.uriString)) },
                                    onMoreClick = { onIntent(HomeDocumentsIntent.OpenContextMenu(doc.uriString)) },
                                )
                            }
                        }
                    }
                }
            }

            if (isExpandedTwoPane && state.selectedPreviewDoc != null) {
                val previewDoc = state.selectedPreviewDoc!!
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(EvoraSpacing.lg),
                    shape = EvoraRadii.lg,
                ) {
                    Column(
                        modifier = Modifier.padding(EvoraSpacing.xl),
                        verticalArrangement = Arrangement.spacedBy(EvoraSpacing.md),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.md),
                        ) {
                            FormatBadge(format = previewDoc.format, extension = previewDoc.extension)
                            Column {
                                Text(
                                    text = previewDoc.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = "${formatByteSize(previewDoc.sizeBytes)} · ${previewDoc.parentFolderName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = extra.textMuted,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(EvoraSpacing.sm))
                        EvoraPrimaryButton(
                            text = "Open in Reader",
                            onClick = { onIntent(HomeDocumentsIntent.OpenDocumentForced(previewDoc)) },
                        )
                        EvoraOutlinedButton(
                            text = "File Actions",
                            onClick = { onIntent(HomeDocumentsIntent.OpenContextMenu(previewDoc.uriString)) },
                        )
                    }
                }
            }
        }
    }

    // 3-dot FileContextMenu Bottom Sheet (US-013)
    state.contextMenuDoc?.let { doc ->
        ModalBottomSheet(onDismissRequest = { onIntent(HomeDocumentsIntent.DismissContextMenu) }) {
            Column(modifier = Modifier.padding(bottom = EvoraSpacing.xl)) {
                Row(
                    modifier = Modifier.padding(horizontal = EvoraSpacing.xl, vertical = EvoraSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.md),
                ) {
                    FormatBadge(format = doc.format, extension = doc.extension)
                    Column {
                        Text(doc.displayName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${formatByteSize(doc.sizeBytes)} · ${formatEpochShort(doc.modifiedAtEpochMs)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = extra.textMuted,
                        )
                    }
                }
                if (doc.format in setOf(DocFormat.PDF, DocFormat.WORD, DocFormat.EXCEL, DocFormat.PPT, DocFormat.ODF)) {
                    ListItem(
                        headlineContent = { Text("Edit / Annotate") },
                        leadingContent = { Icon(Icons.Outlined.Edit, null) },
                        modifier = Modifier.clickableRow { onIntent(HomeDocumentsIntent.EditDocument(doc)) },
                    )
                }
                if (doc.format != DocFormat.PDF && doc.format != DocFormat.ARCHIVE) {
                    ListItem(
                        headlineContent = { Text("Convert to PDF") },
                        leadingContent = { Icon(Icons.Outlined.PictureAsPdf, null) },
                        modifier = Modifier.clickableRow { onIntent(HomeDocumentsIntent.ConvertDocumentToPdf(doc)) },
                    )
                }
                ListItem(
                    headlineContent = { Text("Rename") },
                    leadingContent = { Icon(Icons.Outlined.DriveFileRenameOutline, null) },
                    modifier = Modifier.clickableRow { onIntent(HomeDocumentsIntent.StartRename(doc.uriString)) },
                )
                ListItem(
                    headlineContent = { Text("Share") },
                    leadingContent = { Icon(Icons.Outlined.Share, null) },
                    modifier = Modifier.clickableRow { onIntent(HomeDocumentsIntent.ShareDocument(doc)) },
                )
                ListItem(
                    headlineContent = { Text("Add Home Shortcut") },
                    leadingContent = { Icon(Icons.Outlined.AppShortcut, null) },
                    modifier = Modifier.clickableRow { onIntent(HomeDocumentsIntent.AddHomeShortcut(doc)) },
                )
                ListItem(
                    headlineContent = { Text("Properties") },
                    leadingContent = { Icon(Icons.Outlined.Info, null) },
                    modifier = Modifier.clickableRow { onIntent(HomeDocumentsIntent.OpenProperties(doc.uriString)) },
                )
                ListItem(
                    headlineContent = { Text("Move to Trash", color = MaterialTheme.colorScheme.error) },
                    leadingContent = {
                        Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.error)
                    },
                    modifier = Modifier.clickableRow { onIntent(HomeDocumentsIntent.MoveToTrash(doc.uriString)) },
                )
            }
        }
    }

    // Rename Dialog (US-013)
    state.renameTargetDoc?.let { doc ->
        val initialBase = doc.displayName.substringBeforeLast('.')
        var newName by rememberSaveable(doc.uriString) { mutableStateOf(initialBase) }
        AlertDialog(
            onDismissRequest = { onIntent(HomeDocumentsIntent.DismissRename) },
            title = { Text("Rename Document") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(EvoraSpacing.xs)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("New name (.${doc.extension} preserved)") },
                        singleLine = true,
                        isError = state.renameError != null,
                    )
                    state.renameError?.let { err ->
                        Text(err, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onIntent(HomeDocumentsIntent.ConfirmRename(doc.uriString, newName)) }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(HomeDocumentsIntent.DismissRename) }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Properties Dialog (US-013)
    state.propertiesDoc?.let { doc ->
        AlertDialog(
            onDismissRequest = { onIntent(HomeDocumentsIntent.DismissProperties) },
            title = { Text("Document Properties") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Name: ${doc.displayName}", style = MaterialTheme.typography.bodyMedium)
                    Text("Format: ${doc.format.name} (.${doc.extension})", style = MaterialTheme.typography.bodyMedium)
                    Text("Size: ${formatByteSize(doc.sizeBytes)}", style = MaterialTheme.typography.bodyMedium)
                    Text("Folder: ${doc.parentFolderName}", style = MaterialTheme.typography.bodyMedium)
                    if (doc.lastTotalPages > 0) {
                        Text("Last Page: ${doc.lastPageIndex + 1} / ${doc.lastTotalPages}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onIntent(HomeDocumentsIntent.DismissProperties) }) {
                    Text("Close")
                }
            },
        )
    }

    if (state.showTemplatesSheet) {
        EvoraTemplatesSheet(
            templates = state.templates,
            selectedKind = state.selectedTemplateFilter,
            isCreating = state.isCreatingTemplate,
            onSelectKind = { onIntent(HomeDocumentsIntent.FilterTemplates(it)) },
            onTemplateClicked = { onIntent(HomeDocumentsIntent.CreateFromTemplate(it)) },
            onDismiss = { onIntent(HomeDocumentsIntent.DismissTemplatesSheet) },
        )
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)

private fun SortOrder.displayLabel(): String = when (this) {
    SortOrder.MODIFIED_DESC -> "Newest"
    SortOrder.MODIFIED_ASC -> "Oldest"
    SortOrder.NAME_ASC -> "Name A-Z"
    SortOrder.NAME_DESC -> "Name Z-A"
    SortOrder.SIZE_DESC -> "Largest"
    SortOrder.FORMAT -> "Format"
}

private fun shareDocument(
    context: android.content.Context,
    document: DocumentItem,
    onShowMessage: (String) -> Unit,
) {
    runCatching {
        val parsed = Uri.parse(document.uriString)
        val shareUri = if (parsed.scheme == "file" && parsed.path != null) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(parsed.path!!),
            )
        } else {
            parsed
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${document.displayName}"))
    }.onFailure {
        onShowMessage("Unable to share ${document.displayName}")
    }
}

private fun pinShortcut(
    context: android.content.Context,
    document: DocumentItem,
    onShowMessage: (String) -> Unit,
) {
    if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
        onShowMessage("Home shortcuts are not supported on this launcher")
        return
    }
    runCatching {
        val parsed = Uri.parse(document.uriString)
        val safeUri = if (parsed.scheme == "file" && parsed.path != null) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(parsed.path!!),
            )
        } else {
            parsed
        }
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            action = Intent.ACTION_VIEW
            data = safeUri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } ?: error("Launcher intent missing")

        val shortcut = ShortcutInfoCompat.Builder(context, "doc_${document.id.hashCode()}")
            .setShortLabel(document.displayName.take(24))
            .setLongLabel(document.displayName)
            .setIcon(IconCompat.createWithResource(context, android.R.drawable.ic_menu_edit))
            .setIntent(launchIntent)
            .build()
        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        onShowMessage("Shortcut requested for ${document.displayName}")
    }.onFailure {
        onShowMessage("Unable to create shortcut for ${document.displayName}")
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeDocumentsPreview() {
    EvoraTheme {
        HomeDocumentsScreen(
            state = HomeDocumentsState(isLoading = false),
            isMediumGrid = false,
            isExpandedTwoPane = false,
            onIntent = {},
        )
    }
}
