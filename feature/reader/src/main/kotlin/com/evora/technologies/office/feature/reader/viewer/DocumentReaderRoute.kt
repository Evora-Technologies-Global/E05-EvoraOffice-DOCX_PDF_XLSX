package com.evora.technologies.office.feature.reader.viewer

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.print.PrintAttributes
import android.print.PrintManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.office.core.designsystem.component.ErrorStateCard
import com.evora.technologies.office.core.designsystem.component.EvoraIconButton
import com.evora.technologies.office.core.designsystem.component.FormatFilterChip
import com.evora.technologies.office.core.designsystem.component.PageIndicatorPill
import com.evora.technologies.office.core.designsystem.component.PasswordUnlockDialog
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraTabularMonoStyle
import com.evora.technologies.office.core.designsystem.theme.EvoraTheme
import com.evora.technologies.office.core.designsystem.theme.EvoraThemeTokens
import com.evora.technologies.office.core.designsystem.theme.toPalette
import com.evora.technologies.office.core.domain.port.PdfPageRaster
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.OutlineNode
import com.evora.technologies.office.core.model.ReaderTheme
import com.evora.technologies.office.core.mvi.CollectEffects
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import java.io.ByteArrayInputStream
import java.util.Locale

@Composable
fun DocumentReaderRoute(
    uriString: String,
    displayName: String,
    format: DocFormat,
    onBack: () -> Unit,
    onOpenEditor: (uriString: String, displayName: String, format: DocFormat) -> Unit = { _, _, _ -> },
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DocumentReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var ttsReady by remember { mutableStateOf(false) }
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine.language = Locale.getDefault()
                engine.setOnUtteranceProgressListener(
                    object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) = Unit
                        override fun onDone(utteranceId: String?) {
                            viewModel.onIntent(DocumentReaderIntent.TtsPlaybackFinished)
                        }
                        @Deprecated("Deprecated in API 21")
                        override fun onError(utteranceId: String?) {
                            viewModel.onIntent(DocumentReaderIntent.TtsPlaybackFinished)
                        }
                    },
                )
                ttsReady = true
            }
        }
        ttsInstance = engine
        onDispose {
            engine.stop()
            engine.shutdown()
            ttsInstance = null
        }
    }

    LaunchedEffect(uriString, format) {
        viewModel.onIntent(DocumentReaderIntent.OpenDocument(uriString, displayName, format))
    }

    val listState = rememberLazyListState()
    val pagerState = rememberPagerState(initialPage = state.currentPage.coerceAtLeast(0)) {
        state.totalPages.coerceAtLeast(1)
    }

    LaunchedEffect(state.isLoading, state.isContinuousScroll, state.engineMode, state.totalPages) {
        if (state.isLoading || state.totalPages <= 0) return@LaunchedEffect
        val target = state.currentPage.coerceIn(0, state.totalPages - 1)
        if (state.isContinuousScroll) {
            if (listState.firstVisibleItemIndex != target) {
                listState.scrollToItem(target)
            }
            snapshotFlow { listState.firstVisibleItemIndex }
                .distinctUntilChanged()
                .drop(1)
                .collect { page ->
                    viewModel.onIntent(DocumentReaderIntent.PageChanged(page))
                }
        } else {
            if (pagerState.currentPage != target) {
                pagerState.scrollToPage(target)
            }
            snapshotFlow { pagerState.currentPage }
                .distinctUntilChanged()
                .drop(1)
                .collect { page ->
                    viewModel.onIntent(DocumentReaderIntent.PageChanged(page))
                }
        }
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is DocumentReaderEffect.ScrollToPage -> {
                val safeTarget = effect.pageIndex.coerceIn(0, (state.totalPages - 1).coerceAtLeast(0))
                if (state.isContinuousScroll) {
                    listState.scrollToItem(safeTarget)
                } else {
                    pagerState.scrollToPage(safeTarget)
                }
            }
            is DocumentReaderEffect.SpeakTts -> {
                val tts = ttsInstance
                if (tts != null && ttsReady) {
                    tts.speak(effect.text, TextToSpeech.QUEUE_FLUSH, null, "evora_read_${System.currentTimeMillis()}")
                } else {
                    viewModel.onIntent(DocumentReaderIntent.TtsPlaybackFinished)
                    onShowMessage("Text-to-Speech engine initializing, please tap TTS again.")
                }
            }
            DocumentReaderEffect.StopTts -> {
                ttsInstance?.stop()
            }
            is DocumentReaderEffect.ApplyOrientation -> {
                val activity = context as? Activity
                activity?.requestedOrientation = if (effect.landscape) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
            is DocumentReaderEffect.LaunchSystemPrint -> {
                printHtmlOrText(context, effect.title, effect.content, effect.isHtml, onShowMessage)
            }
            is DocumentReaderEffect.NavigateToEditor -> {
                (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                onOpenEditor(effect.uriString, effect.displayName, effect.format)
            }
            DocumentReaderEffect.NavigateBack -> {
                (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                onBack()
            }
            is DocumentReaderEffect.ShowMessage -> onShowMessage(effect.message)
        }
    }

    DocumentReaderScreen(
        state = state,
        listState = listState,
        pagerState = pagerState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DocumentReaderScreen(
    state: DocumentReaderState,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    pagerState: androidx.compose.foundation.pager.PagerState = rememberPagerState { state.totalPages.coerceAtLeast(1) },
    onIntent: (DocumentReaderIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = state.readerTheme.toPalette()
    val formatPalette = EvoraThemeTokens.formats.paletteFor(state.format)
    val extra = EvoraThemeTokens.colors

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = palette.pageCanvas,
        topBar = {
            if (!state.isFullscreen) {
                Column(modifier = Modifier.background(formatPalette.headerBg)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .safeDrawingPadding()
                            .heightIn(min = 56.dp)
                            .padding(horizontal = EvoraSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        EvoraIconButton(onClick = { onIntent(DocumentReaderIntent.BackClicked) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = formatPalette.headerContent,
                            )
                        }
                        Text(
                            text = state.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = formatPalette.headerContent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (state.outline.isNotEmpty() || state.textChapters.size > 1) {
                            EvoraIconButton(onClick = { onIntent(DocumentReaderIntent.OpenOutlineDrawer) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.FormatListBulleted,
                                    contentDescription = "Table of Contents",
                                    tint = formatPalette.headerContent,
                                )
                            }
                        }
                        EvoraIconButton(onClick = { onIntent(DocumentReaderIntent.ToggleSearchBar) }) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search in document",
                                tint = formatPalette.headerContent,
                            )
                        }
                        if (state.format in setOf(DocFormat.PDF, DocFormat.WORD, DocFormat.EXCEL, DocFormat.PPT, DocFormat.ODF)) {
                            EvoraIconButton(onClick = { onIntent(DocumentReaderIntent.EditClicked) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit document",
                                    tint = formatPalette.headerContent,
                                )
                            }
                        }
                        EvoraIconButton(onClick = { onIntent(DocumentReaderIntent.OpenReadingSettings) }) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = "Reading settings",
                                tint = formatPalette.headerContent,
                            )
                        }
                    }

                    formatPalette.darkHeaderAccent?.let { accent ->
                        HorizontalDivider(thickness = 2.dp, color = accent)
                    }

                    if (state.showSearchBar) {
                        Surface(color = MaterialTheme.colorScheme.surface) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = EvoraSpacing.sm, vertical = EvoraSpacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = state.searchQuery,
                                    onValueChange = { onIntent(DocumentReaderIntent.SearchQueryChanged(it)) },
                                    placeholder = { Text("Find in document...") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { onIntent(DocumentReaderIntent.SubmitInDocSearch) }) {
                                    Text("Find")
                                }
                                if (state.searchMatches.isNotEmpty()) {
                                    Text(
                                        text = "${state.activeMatchIndex + 1}/${state.searchMatches.size}",
                                        style = EvoraTabularMonoStyle,
                                    )
                                    EvoraIconButton(onClick = { onIntent(DocumentReaderIntent.PrevSearchMatch) }) {
                                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous match")
                                    }
                                    EvoraIconButton(onClick = { onIntent(DocumentReaderIntent.NextSearchMatch) }) {
                                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next match")
                                    }
                                }
                                EvoraIconButton(onClick = { onIntent(DocumentReaderIntent.ToggleSearchBar) }) {
                                    Icon(Icons.Outlined.Close, "Close search")
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (!state.isFullscreen) {
                Surface(
                    tonalElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = EvoraSpacing.md, vertical = EvoraSpacing.xs),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FormatFilterChip(
                            label = "Light",
                            selected = state.readerTheme == ReaderTheme.LIGHT,
                            onClick = { onIntent(DocumentReaderIntent.SelectReaderTheme(ReaderTheme.LIGHT)) },
                        )
                        FormatFilterChip(
                            label = "Sepia",
                            selected = state.readerTheme == ReaderTheme.SEPIA,
                            onClick = { onIntent(DocumentReaderIntent.SelectReaderTheme(ReaderTheme.SEPIA)) },
                        )
                        FormatFilterChip(
                            label = "Night",
                            selected = state.readerTheme == ReaderTheme.DARK,
                            onClick = { onIntent(DocumentReaderIntent.SelectReaderTheme(ReaderTheme.DARK)) },
                        )
                        FormatFilterChip(
                            label = if (state.isTtsPlaying) "Stop TTS" else "TTS",
                            selected = state.isTtsPlaying,
                            onClick = { onIntent(DocumentReaderIntent.ToggleTts) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val viewportWidthPx = with(LocalDensity.current) { maxWidth.roundToPx() }

            Column(modifier = Modifier.fillMaxSize()) {
                if (state.isLargeHwpWarning) {
                    Surface(
                        color = extra.warningContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Large HWP (> 50 MB): rendered offline in safe memory mode.",
                            style = MaterialTheme.typography.labelMedium,
                            color = extra.onWarningContainer,
                            modifier = Modifier.padding(horizontal = EvoraSpacing.lg, vertical = EvoraSpacing.sm),
                        )
                    }
                }

                if (state.isTtsPlaying || state.isAutoScrollPlaying) {
                    Surface(
                        shape = EvoraRadii.md,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = EvoraSpacing.lg, vertical = EvoraSpacing.xs),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = EvoraSpacing.md, vertical = EvoraSpacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = if (state.isTtsPlaying) Icons.Outlined.RecordVoiceOver else Icons.Outlined.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = buildString {
                                        if (state.isTtsPlaying) append("TTS Reading Page ${state.currentPage + 1}")
                                        if (state.isTtsPlaying && state.isAutoScrollPlaying) append(" · ")
                                        if (state.isAutoScrollPlaying) append("Auto-Scroll ${state.autoScrollSpeed}x")
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            if (state.isTtsPlaying) {
                                TextButton(onClick = { onIntent(DocumentReaderIntent.ToggleTts) }) {
                                    Text("Stop TTS")
                                }
                            } else {
                                TextButton(onClick = { onIntent(DocumentReaderIntent.ToggleAutoScroll) }) {
                                    Text("Pause")
                                }
                            }
                        }
                    }
                }

                when {
                    state.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    state.errorMessage != null -> {
                        Box(modifier = Modifier.padding(EvoraSpacing.lg)) {
                            ErrorStateCard(
                                title = "Unable to Open Document",
                                description = state.errorMessage,
                                actionLabel = "Go Back",
                                onActionClick = { onIntent(DocumentReaderIntent.BackClicked) },
                            )
                        }
                    }
                    state.isReflowMode -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(EvoraSpacing.lg),
                        ) {
                            Surface(
                                shape = EvoraRadii.md,
                                color = palette.pageSheetBg,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = state.extractedCurrentPageText.ifBlank { state.fullPlainText },
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = (16f * state.zoom).coerceIn(14f, 30f).sp,
                                        lineHeight = (24f * state.zoom).coerceIn(20f, 44f).sp,
                                    ),
                                    color = palette.bodyInk,
                                    modifier = Modifier.padding(EvoraSpacing.lg),
                                )
                            }
                        }
                    }
                    state.engineMode == ReaderEngineMode.HWP_HTML -> {
                        HwpSandboxedWebView(
                            sanitizedHtml = state.hwpSanitizedHtml.orEmpty(),
                            readerTheme = state.readerTheme,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    state.engineMode == ReaderEngineMode.TEXT_EPUB || state.engineMode == ReaderEngineMode.OFFICE_PREVIEW -> {
                        if (state.isContinuousScroll) {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(EvoraSpacing.lg),
                                verticalArrangement = Arrangement.spacedBy(EvoraSpacing.md),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                items(state.textChapters, key = { it.title }) { chapter ->
                                    Surface(
                                        shape = EvoraRadii.md,
                                        color = palette.pageSheetBg,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Column(modifier = Modifier.padding(EvoraSpacing.lg)) {
                                            Text(
                                                text = chapter.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = palette.bodyInk,
                                            )
                                            Spacer(modifier = Modifier.height(EvoraSpacing.sm))
                                            Text(
                                                text = chapter.body,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontSize = (15f * state.zoom).coerceIn(13f, 28f).sp,
                                                ),
                                                color = palette.bodyInk,
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                            ) { index ->
                                val chapter = state.textChapters.getOrNull(index)
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(EvoraSpacing.lg),
                                ) {
                                    Surface(
                                        shape = EvoraRadii.md,
                                        color = palette.pageSheetBg,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Column(modifier = Modifier.padding(EvoraSpacing.lg)) {
                                            Text(
                                                text = chapter?.title.orEmpty(),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = palette.bodyInk,
                                            )
                                            Spacer(modifier = Modifier.height(EvoraSpacing.sm))
                                            Text(
                                                text = chapter?.body.orEmpty(),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = palette.bodyInk,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        // PDF Mode (Continuous Vertical Scroll vs Single-Page Snap + Stable Pinch Zoom/Pan)
                        val latestZoom by rememberUpdatedState(state.zoom)
                        var panOffsetX by remember { mutableFloatStateOf(0f) }
                        var panOffsetY by remember { mutableFloatStateOf(0f) }
                        LaunchedEffect(state.zoom) {
                            if (state.zoom <= 1.02f) {
                                panOffsetX = 0f
                                panOffsetY = 0f
                            }
                        }
                        val zoomModifier = Modifier.pointerInput(state.isContinuousScroll) {
                            detectTransformGestures { _, pan, zoomChange, _ ->
                                val nextZoom = (latestZoom * zoomChange).coerceIn(1.0f, 4.0f)
                                if (zoomChange != 1f) {
                                    onIntent(DocumentReaderIntent.ZoomChanged(nextZoom))
                                }
                                if (nextZoom > 1.02f) {
                                    val maxOffsetX = (size.width * (nextZoom - 1f)) / 2f
                                    val maxOffsetY = (size.height * (nextZoom - 1f)) / 2f
                                    panOffsetX = (panOffsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                    panOffsetY = (panOffsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                }
                            }
                        }
                        if (state.isContinuousScroll) {
                            LazyColumn(
                                state = listState,
                                userScrollEnabled = state.zoom <= 1.05f,
                                contentPadding = PaddingValues(EvoraSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(EvoraSpacing.md),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clipToBounds()
                                    .then(zoomModifier),
                            ) {
                                items(state.totalPages, key = { it }) { pageIdx ->
                                    LaunchedEffect(pageIdx, viewportWidthPx) {
                                        onIntent(DocumentReaderIntent.RequestPageRender(pageIdx, viewportWidthPx))
                                    }
                                    PdfPageSheet(
                                        raster = state.renderedPages[pageIdx],
                                        pageNumber = pageIdx + 1,
                                        readerTheme = state.readerTheme,
                                        zoom = state.zoom,
                                        panOffsetX = if (pageIdx == state.currentPage) panOffsetX else 0f,
                                        panOffsetY = if (pageIdx == state.currentPage) panOffsetY else 0f,
                                    )
                                }
                            }
                        } else {
                            HorizontalPager(
                                state = pagerState,
                                userScrollEnabled = state.zoom <= 1.05f,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clipToBounds()
                                    .padding(EvoraSpacing.md)
                                    .then(zoomModifier),
                            ) { pageIdx ->
                                LaunchedEffect(pageIdx, viewportWidthPx) {
                                    onIntent(DocumentReaderIntent.RequestPageRender(pageIdx, viewportWidthPx))
                                }
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    PdfPageSheet(
                                        raster = state.renderedPages[pageIdx],
                                        pageNumber = pageIdx + 1,
                                        readerTheme = state.readerTheme,
                                        zoom = state.zoom,
                                        panOffsetX = panOffsetX,
                                        panOffsetY = panOffsetY,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.isFullscreen) {
                Surface(
                    shape = EvoraRadii.full,
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .safeDrawingPadding()
                        .padding(EvoraSpacing.md),
                ) {
                    EvoraIconButton(onClick = { onIntent(DocumentReaderIntent.ToggleFullscreen) }) {
                        Icon(
                            imageVector = Icons.Outlined.FullscreenExit,
                            contentDescription = "Exit fullscreen",
                            tint = Color.White,
                        )
                    }
                }
            }

            if (!state.isLoading && state.totalPages > 1) {
                PageIndicatorPill(
                    currentPage = state.currentPage,
                    totalPages = state.totalPages,
                    onJumpToPage = { onIntent(DocumentReaderIntent.JumpToPage(it)) },
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }
    }

    if (state.passwordRequired) {
        PasswordUnlockDialog(
            fileName = state.displayName,
            errorMessage = state.passwordError,
            isUnlocking = state.isUnlocking,
            onUnlock = { onIntent(DocumentReaderIntent.SubmitPassword(it)) },
            onDismiss = { onIntent(DocumentReaderIntent.DismissPasswordDialog) },
        )
    }

    if (state.showReadingSettingsSheet) {
        ReadingSettingsSheet(
            state = state,
            onIntent = onIntent,
        )
    }

    if (state.showOutlineDrawer) {
        ModalBottomSheet(onDismissRequest = { onIntent(DocumentReaderIntent.DismissOutlineDrawer) }) {
            Column(modifier = Modifier.padding(EvoraSpacing.lg)) {
                Text("Table of Contents", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(EvoraSpacing.md))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(EvoraSpacing.xs)) {
                    if (state.outline.isNotEmpty()) {
                        val flattened = flattenOutline(state.outline)
                        items(flattened, key = { "${it.level}_${it.pageIndex}_${it.title}" }) { node ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable { onIntent(DocumentReaderIntent.SelectOutlineNode(node.pageIndex)) }
                                    .padding(start = (node.level * 14).dp, end = EvoraSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(node.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text("p.${node.pageIndex + 1}", style = EvoraTabularMonoStyle, color = extra.textMuted)
                            }
                        }
                    } else {
                        items(state.textChapters.size) { idx ->
                            val chapter = state.textChapters[idx]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable { onIntent(DocumentReaderIntent.SelectOutlineNode(idx)) },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(chapter.title, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfPageSheet(
    raster: PdfPageRaster?,
    pageNumber: Int,
    readerTheme: ReaderTheme,
    zoom: Float,
    panOffsetX: Float = 0f,
    panOffsetY: Float = 0f,
) {
    val palette = readerTheme.toPalette()
    val colorFilter = remember(readerTheme) {
        when (readerTheme) {
            ReaderTheme.LIGHT -> null
            ReaderTheme.DARK -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 240f,
                        0f, -1f, 0f, 0f, 240f,
                        0f, 0f, -1f, 0f, 245f,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                ),
            )
            ReaderTheme.SEPIA -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.96f, 0f, 0f, 0f, 0f,
                        0f, 0.92f, 0f, 0f, 0f,
                        0f, 0f, 0.82f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                ),
            )
        }
    }

    Surface(
        shape = EvoraRadii.xs,
        color = palette.pageSheetBg,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds(),
    ) {
        if (raster == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.707f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Rendering Page $pageNumber...",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.mutedInk,
                )
            }
        } else {
            val bitmap = remember(raster) {
                Bitmap.createBitmap(
                    raster.argbPixels,
                    raster.width,
                    raster.height,
                    Bitmap.Config.ARGB_8888,
                ).asImageBitmap()
            }
            Image(
                bitmap = bitmap,
                contentDescription = "Page $pageNumber",
                contentScale = ContentScale.FillWidth,
                colorFilter = colorFilter,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(
                        scaleX = zoom,
                        scaleY = zoom,
                        translationX = panOffsetX,
                        translationY = panOffsetY,
                    ),
            )
        }
    }
}

@Composable
private fun HwpSandboxedWebView(
    sanitizedHtml: String,
    readerTheme: ReaderTheme,
    modifier: Modifier = Modifier,
) {
    val palette = readerTheme.toPalette()
    val context = LocalContext.current
    val themedHtml = remember(sanitizedHtml, readerTheme) {
        val themeCss = "<style>body{background:${palette.htmlBackgroundHex} !important;color:${palette.htmlTextHex} !important;}</style>"
        if (sanitizedHtml.contains("</head>")) {
            sanitizedHtml.replace("</head>", "$themeCss</head>")
        } else {
            themeCss + sanitizedHtml
        }
    }
    val webView = remember(context) {
        WebView(context).apply {
            settings.javaScriptEnabled = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            @Suppress("DEPRECATION")
            settings.allowFileAccessFromFileURLs = false
            @Suppress("DEPRECATION")
            settings.allowUniversalAccessFromFileURLs = false
            settings.blockNetworkLoads = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): Boolean = true

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): WebResourceResponse? {
                    val scheme = request?.url?.scheme?.lowercase()
                    return if (scheme == "about") {
                        super.shouldInterceptRequest(view, request)
                    } else {
                        WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                    }
                }
            }
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { webView },
        update = { view ->
            if (view.tag != themedHtml) {
                view.tag = themedHtml
                view.loadDataWithBaseURL("about:blank", themedHtml, "text/html", "utf-8", null)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadingSettingsSheet(
    state: DocumentReaderState,
    onIntent: (DocumentReaderIntent) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = { onIntent(DocumentReaderIntent.DismissReadingSettings) }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = EvoraSpacing.xl, vertical = EvoraSpacing.md),
            verticalArrangement = Arrangement.spacedBy(EvoraSpacing.md),
        ) {
            Text("Reading Comfort Settings", style = MaterialTheme.typography.titleLarge)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Continuous Vertical Scroll", style = MaterialTheme.typography.titleSmall)
                    Text("Off switches to Single-Page Horizontal Snap", style = MaterialTheme.typography.labelMedium)
                }
                Switch(
                    checked = state.isContinuousScroll,
                    onCheckedChange = { onIntent(DocumentReaderIntent.ToggleContinuousScroll) },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Reflow Text Mode", style = MaterialTheme.typography.titleSmall)
                Switch(
                    checked = state.isReflowMode,
                    onCheckedChange = { onIntent(DocumentReaderIntent.ToggleReflowMode) },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Landscape Reading", style = MaterialTheme.typography.titleSmall)
                Switch(
                    checked = state.isLandscapePreferred,
                    onCheckedChange = { onIntent(DocumentReaderIntent.ToggleOrientation) },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Auto-Scroll", style = MaterialTheme.typography.titleSmall)
                Switch(
                    checked = state.isAutoScrollPlaying,
                    onCheckedChange = { onIntent(DocumentReaderIntent.ToggleAutoScroll) },
                )
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(1, 2, 3).forEachIndexed { idx, speed ->
                    SegmentedButton(
                        selected = state.autoScrollSpeed == speed,
                        onClick = { onIntent(DocumentReaderIntent.SetAutoScrollSpeed(speed)) },
                        shape = SegmentedButtonDefaults.itemShape(idx, 3),
                    ) {
                        Text("Speed ${speed}x")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Fullscreen Reading", style = MaterialTheme.typography.titleSmall)
                Switch(
                    checked = state.isFullscreen,
                    onCheckedChange = {
                        onIntent(DocumentReaderIntent.DismissReadingSettings)
                        onIntent(DocumentReaderIntent.ToggleFullscreen)
                    },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Print Document", style = MaterialTheme.typography.titleSmall)
                TextButton(
                    onClick = {
                        onIntent(DocumentReaderIntent.DismissReadingSettings)
                        onIntent(DocumentReaderIntent.PrintClicked)
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Print,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Print")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("TTS Read-Aloud", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = { onIntent(DocumentReaderIntent.ToggleTts) }) {
                    Icon(
                        imageVector = if (state.isTtsPlaying) Icons.Outlined.Stop else Icons.Outlined.RecordVoiceOver,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (state.isTtsPlaying) "Stop" else "Read Page")
                }
            }

            Spacer(modifier = Modifier.height(EvoraSpacing.lg))
        }
    }
}

private fun flattenOutline(nodes: List<OutlineNode>): List<OutlineNode> {
    val result = mutableListOf<OutlineNode>()
    fun walk(list: List<OutlineNode>) {
        list.forEach { n ->
            result += n
            walk(n.children)
        }
    }
    walk(nodes)
    return result
}

private fun printHtmlOrText(
    context: Context,
    title: String,
    content: String,
    isHtml: Boolean,
    onShowMessage: (String) -> Unit,
) {
    runCatching {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: error("PrintManager unavailable")
        val webView = WebView(context).apply {
            settings.javaScriptEnabled = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            @Suppress("DEPRECATION")
            settings.allowFileAccessFromFileURLs = false
            @Suppress("DEPRECATION")
            settings.allowUniversalAccessFromFileURLs = false
            settings.blockNetworkLoads = true
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): Boolean = true

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): WebResourceResponse? {
                    val scheme = request?.url?.scheme?.lowercase()
                    return if (scheme == "about") {
                        super.shouldInterceptRequest(view, request)
                    } else {
                        WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    val adapter = createPrintDocumentAdapter(title)
                    printManager.print(title, adapter, PrintAttributes.Builder().build())
                }
            }
        }
        val html = if (isHtml) {
            content
        } else {
            "<html><body><pre style='white-space:pre-wrap;font-family:sans-serif;'>${
                android.text.TextUtils.htmlEncode(content)
            }</pre></body></html>"
        }
        webView.loadDataWithBaseURL("about:blank", html, "text/html", "utf-8", null)
    }.onFailure {
        onShowMessage("System Print unavailable for this file")
    }
}

@Preview(showBackground = true)
@Composable
private fun DocumentReaderPreview() {
    EvoraTheme {
        DocumentReaderScreen(
            state = DocumentReaderState(
                displayName = "Contract_Signed_v2.pdf",
                isLoading = false,
                totalPages = 12,
                currentPage = 2,
                readerTheme = ReaderTheme.SEPIA,
            ),
            onIntent = {},
        )
    }
}
