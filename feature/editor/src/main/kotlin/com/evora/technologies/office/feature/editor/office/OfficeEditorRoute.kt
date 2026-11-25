package com.evora.technologies.office.feature.editor.office

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.office.core.designsystem.component.ErrorStateCard
import com.evora.technologies.office.core.designsystem.component.EvoraIconButton
import com.evora.technologies.office.core.designsystem.component.launchSystemPdfPrint
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraTheme
import com.evora.technologies.office.core.designsystem.theme.EvoraThemeTokens
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.OfficeKind
import com.evora.technologies.office.core.mvi.CollectEffects
import java.io.File

@Composable
fun OfficeEditorRoute(
    uriString: String,
    displayName: String,
    kind: OfficeKind,
    onBack: () -> Unit,
    onLaunchSlideShow: (uriString: String, displayName: String, startIndex: Int) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OfficeEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(uriString, kind) {
        viewModel.onIntent(OfficeEditorIntent.OpenDocument(uriString, displayName, kind))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.onIntent(OfficeEditorIntent.LifecycleStopped)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            OfficeEditorEffect.NavigateBack -> onBack()
            is OfficeEditorEffect.LaunchSlideShow -> onLaunchSlideShow(effect.uriString, effect.displayName, effect.startSlideIndex)
            is OfficeEditorEffect.LaunchPdfPrint -> launchSystemPdfPrint(context, effect.uriString, effect.title, onShowMessage)
            is OfficeEditorEffect.ShareFile -> shareOfficeFile(context, effect.uriString, effect.displayName, onShowMessage)
            is OfficeEditorEffect.ShowMessage -> onShowMessage(effect.message)
        }
    }

    OfficeEditorScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
internal fun OfficeEditorScreen(
    state: OfficeEditorState,
    onIntent: (OfficeEditorIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val docFormat = when (state.kind) {
        OfficeKind.WORD -> DocFormat.WORD
        OfficeKind.EXCEL -> DocFormat.EXCEL
        OfficeKind.PPT -> DocFormat.PPT
    }
    val palette = EvoraThemeTokens.formats.paletteFor(docFormat)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.background(palette.headerBg)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .safeDrawingPadding()
                        .heightIn(min = 56.dp)
                        .padding(horizontal = EvoraSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EvoraIconButton(onClick = { onIntent(OfficeEditorIntent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = palette.headerContent)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.headerContent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val statusText = when {
                            state.isSaving -> "Saving..."
                            state.lastDraftSavedEpochMs != null -> "Draft Auto-saved"
                            state.isNativeLokLoaded -> "LOK 16KB Active"
                            else -> "Offline OOXML Engine"
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.headerContent.copy(alpha = 0.85f),
                        )
                    }
                    EvoraIconButton(
                        onClick = { onIntent(OfficeEditorIntent.UndoClicked) },
                        enabled = state.canUndo,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Undo, "Undo", tint = palette.headerContent)
                    }
                    EvoraIconButton(
                        onClick = { onIntent(OfficeEditorIntent.RedoClicked) },
                        enabled = state.canRedo,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Redo, "Redo", tint = palette.headerContent)
                    }
                    if (state.kind == OfficeKind.PPT) {
                        EvoraIconButton(onClick = { onIntent(OfficeEditorIntent.LaunchSlideShowClicked) }) {
                            Icon(Icons.Outlined.PlayArrow, "Start SlideShow", tint = palette.headerContent)
                        }
                    }
                    TextButton(
                        onClick = { onIntent(OfficeEditorIntent.SaveClicked) },
                        enabled = !state.isSaving,
                    ) {
                        Text(
                            text = "Done",
                            color = palette.headerContent,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                palette.darkHeaderAccent?.let { HorizontalDivider(thickness = 2.dp, color = it) }
            }
        },
        bottomBar = {
            if (!state.isLoading && state.errorMessage == null) {
                Surface(tonalElevation = 6.dp, color = MaterialTheme.colorScheme.surface) {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = EvoraSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                when (state.kind) {
                                    OfficeKind.WORD -> WordTabStrip(state.wordTab) { onIntent(OfficeEditorIntent.SelectWordTab(it)) }
                                    OfficeKind.EXCEL -> ExcelTabStrip(state.excelTab) { onIntent(OfficeEditorIntent.SelectExcelTab(it)) }
                                    OfficeKind.PPT -> PptTabStrip(state.pptTab) { onIntent(OfficeEditorIntent.SelectPptTab(it)) }
                                }
                            }
                            EvoraIconButton(onClick = { onIntent(OfficeEditorIntent.ToggleBottomShelf) }) {
                                Icon(
                                    imageVector = if (state.isBottomShelfExpanded) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess,
                                    contentDescription = "Toggle toolbar",
                                )
                            }
                        }
                        AnimatedVisibility(
                            visible = state.isBottomShelfExpanded,
                            enter = expandVertically(animationSpec = tween(220)),
                            exit = shrinkVertically(animationSpec = tween(220)),
                        ) {
                            when (state.kind) {
                                OfficeKind.WORD -> WordToolbarShelf(state = state, onIntent = onIntent)
                                OfficeKind.EXCEL -> ExcelToolbarShelf(state = state, onIntent = onIntent)
                                OfficeKind.PPT -> PptToolbarShelf(state = state, onIntent = onIntent)
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.errorMessage != null -> ErrorStateCard(
                    title = "Unable to open document",
                    description = state.errorMessage,
                    actionLabel = "Back",
                    onActionClick = { onIntent(OfficeEditorIntent.BackClicked) },
                    modifier = Modifier.padding(EvoraSpacing.lg),
                )
                else -> when (state.kind) {
                    OfficeKind.WORD -> WordEditorCanvas(state = state, onIntent = onIntent)
                    OfficeKind.EXCEL -> ExcelEditorCanvas(state = state, onIntent = onIntent)
                    OfficeKind.PPT -> PptEditorCanvas(state = state, onIntent = onIntent)
                }
            }
        }
    }

    state.pendingRecoveryDraft?.let { draft ->
        AlertDialog(
            onDismissRequest = { onIntent(OfficeEditorIntent.DiscardDraftClicked) },
            title = { Text("Restore Unsaved Changes?") },
            text = {
                Text("An automatic checkpoint for ${draft.displayName} was recovered. Restore unsaved edits or discard?")
            },
            confirmButton = {
                TextButton(onClick = { onIntent(OfficeEditorIntent.RestoreDraftClicked) }) {
                    Text("Restore Draft")
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(OfficeEditorIntent.DiscardDraftClicked) }) {
                    Text("Discard")
                }
            },
        )
    }
}

private fun shareOfficeFile(
    context: android.content.Context,
    uriString: String,
    displayName: String,
    onShowMessage: (String) -> Unit,
) {
    runCatching {
        val parsed = Uri.parse(uriString)
        val shareUri = if (parsed.scheme == "file" && parsed.path != null) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(parsed.path!!))
        } else {
            parsed
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share $displayName"))
    }.onFailure {
        onShowMessage("Unable to share $displayName")
    }
}

@Preview(showBackground = true)
@Composable
private fun OfficeEditorWordPreview() {
    EvoraTheme {
        OfficeEditorScreen(
            state = OfficeEditorState(isLoading = false, kind = OfficeKind.WORD),
            onIntent = {},
        )
    }
}
