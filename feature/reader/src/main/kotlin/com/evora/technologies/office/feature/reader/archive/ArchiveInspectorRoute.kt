package com.evora.technologies.office.feature.reader.archive

import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.office.core.designsystem.component.EmptyStateView
import com.evora.technologies.office.core.designsystem.component.ErrorStateCard
import com.evora.technologies.office.core.designsystem.component.EvoraIconButton
import com.evora.technologies.office.core.designsystem.component.EvoraPrimaryButton
import com.evora.technologies.office.core.designsystem.component.PasswordUnlockDialog
import com.evora.technologies.office.core.designsystem.component.ShimmerDocListSkeleton
import com.evora.technologies.office.core.designsystem.component.formatByteSize
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraTheme
import com.evora.technologies.office.core.designsystem.theme.EvoraThemeTokens
import com.evora.technologies.office.core.mvi.CollectEffects
import java.io.File

@Composable
fun ArchiveInspectorRoute(
    uriString: String,
    displayName: String,
    onBack: () -> Unit,
    onOpenExtractedDocument: (fileUri: String, displayName: String) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArchiveInspectorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uriString) {
        viewModel.onIntent(ArchiveInspectorIntent.LoadArchive(uriString, displayName))
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            ArchiveInspectorEffect.NavigateBack -> onBack()
            is ArchiveInspectorEffect.OpenExtractedDocument -> onOpenExtractedDocument(
                File(effect.fileUri).toURI().toString(),
                effect.displayName,
            )
            is ArchiveInspectorEffect.ShowMessage -> onShowMessage(effect.message)
        }
    }

    val defaultExtractDir = File(
        context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
        "EvoraExtracted/${displayName.substringBeforeLast('.')}",
    ).absolutePath

    ArchiveInspectorScreen(
        state = state,
        defaultExtractDir = defaultExtractDir,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
internal fun ArchiveInspectorScreen(
    state: ArchiveInspectorState,
    defaultExtractDir: String,
    onIntent: (ArchiveInspectorIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = EvoraThemeTokens.colors
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        topBar = {
            Surface(color = EvoraThemeTokens.formats.textArchive.headerBg) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = EvoraSpacing.xs, vertical = EvoraSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EvoraIconButton(onClick = { onIntent(ArchiveInspectorIntent.BackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = EvoraThemeTokens.formats.textArchive.headerContent,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.archiveName,
                            style = MaterialTheme.typography.titleMedium,
                            color = EvoraThemeTokens.formats.textArchive.headerContent,
                        )
                        Text(
                            text = "${state.extension.uppercase()} Archive · ${state.entries.size} entries",
                            style = MaterialTheme.typography.labelMedium,
                            color = EvoraThemeTokens.formats.textArchive.headerContent.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (state.isSupported && !state.isLoading && state.entries.isNotEmpty()) {
                Surface(tonalElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
                    Column(modifier = Modifier.padding(EvoraSpacing.lg)) {
                        val buttonText = if (state.selectedEntryPaths.isEmpty()) {
                            "Extract All (${state.entries.count { !it.isDirectory }})"
                        } else {
                            "Extract Selected (${state.selectedEntryPaths.size})"
                        }
                        EvoraPrimaryButton(
                            text = buttonText,
                            isLoading = state.isExtracting,
                            onClick = {
                                onIntent(
                                    ArchiveInspectorIntent.ExtractEntries(
                                        destinationDirPath = defaultExtractDir,
                                        extractAll = state.selectedEntryPaths.isEmpty(),
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(EvoraSpacing.lg),
                ) {
                    ShimmerDocListSkeleton(itemCount = 5)
                }
            }
            !state.isSupported -> {
                EmptyStateView(
                    title = "Unsupported Archive Format",
                    description = state.unsupportedReason
                        ?: "Evora Office supports ZIP and 7z offline archives.",
                    icon = Icons.Outlined.WarningAmber,
                    primaryActionLabel = "Go Back",
                    onPrimaryAction = { onIntent(ArchiveInspectorIntent.BackClicked) },
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                )
            }
            state.errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(EvoraSpacing.lg),
                ) {
                    ErrorStateCard(
                        title = "Archive Error",
                        description = state.errorMessage,
                        actionLabel = "Go Back",
                        onActionClick = { onIntent(ArchiveInspectorIntent.BackClicked) },
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(EvoraSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                ) {
                    if (state.extractedPaths.isNotEmpty()) {
                        item(key = "extracted_banner") {
                            Surface(
                                shape = EvoraRadii.md,
                                color = extra.successContainer,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(EvoraSpacing.md),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = "Extracted ${state.extractedPaths.size} file(s):",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = extra.onSuccessContainer,
                                    )
                                    state.extractedPaths.take(5).forEach { path ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = EvoraSpacing.minTouchTarget)
                                                .clickable {
                                                    onIntent(ArchiveInspectorIntent.OpenExtractedFile(path))
                                                },
                                            contentAlignment = Alignment.CenterStart,
                                        ) {
                                            Text(
                                                text = "Open ${path.substringAfterLast('/')}",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    items(state.entries, key = { it.path }) { entry ->
                        val isSelected = entry.path in state.selectedEntryPaths
                        Surface(
                            shape = EvoraRadii.md,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !entry.isDirectory) {
                                    onIntent(ArchiveInspectorIntent.ToggleEntrySelection(entry.path))
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(EvoraSpacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (!entry.isDirectory) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            onIntent(ArchiveInspectorIntent.ToggleEntrySelection(entry.path))
                                        },
                                    )
                                }
                                Icon(
                                    imageVector = if (entry.isDirectory) Icons.Outlined.Folder else Icons.Outlined.InsertDriveFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(EvoraSpacing.md))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = if (entry.isDirectory) "Folder · ${entry.path}" else "${formatByteSize(entry.sizeBytes)} · ${entry.path}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = extra.textMuted,
                                    )
                                }
                                if (entry.isEncrypted) {
                                    Icon(Icons.Outlined.Lock, contentDescription = "Encrypted", tint = extra.warning)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.passwordRequired) {
        PasswordUnlockDialog(
            fileName = state.archiveName,
            errorMessage = state.passwordError,
            onUnlock = { onIntent(ArchiveInspectorIntent.SubmitPassword(it)) },
            onDismiss = { onIntent(ArchiveInspectorIntent.DismissPasswordDialog) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ArchiveInspectorPreview() {
    EvoraTheme {
        ArchiveInspectorScreen(
            state = ArchiveInspectorState(
                archiveName = "Invoices_Q4.rar",
                extension = "rar",
                isLoading = false,
                isSupported = false,
                unsupportedReason = "Evora Office supports .zip and .7z offline archives.",
            ),
            defaultExtractDir = "/tmp",
            onIntent = {},
        )
    }
}
