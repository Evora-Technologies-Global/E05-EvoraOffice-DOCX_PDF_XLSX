package com.evora.technologies.office.feature.editor.pdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.office.core.designsystem.component.ErrorStateCard
import com.evora.technologies.office.core.designsystem.component.EvoraIconButton
import com.evora.technologies.office.core.designsystem.component.EvoraOutlinedButton
import com.evora.technologies.office.core.designsystem.component.EvoraPrimaryButton
import com.evora.technologies.office.core.designsystem.component.EvoraRewardGateSheet
import com.evora.technologies.office.core.designsystem.component.PasswordUnlockDialog
import com.evora.technologies.office.core.designsystem.component.launchSystemPdfPrint
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraTabularMonoStyle
import com.evora.technologies.office.core.designsystem.theme.EvoraTheme
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.PdfAnnotationType
import com.evora.technologies.office.core.model.SignatureItem
import com.evora.technologies.office.core.mvi.CollectEffects
import kotlin.math.roundToInt

private val ANNOTATION_SWATCHES = listOf(
    0xFFFFD54FL, // Yellow Highlight
    0xFF69F0AEL, // Green
    0xFF40C4FFL, // Sky Blue
    0xFFFF6E6EL, // Coral
    0xFF0F172AL, // Black Ink
)

@Composable
fun PdfEditorRoute(
    uriString: String,
    displayName: String,
    newlyCreatedSignature: SignatureItem? = null,
    onNewlyCreatedSignatureConsumed: () -> Unit = {},
    onOpenSignaturePad: () -> Unit,
    onOpenPaywall: () -> Unit,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PdfEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uriString) {
        viewModel.onIntent(PdfEditorIntent.OpenPdf(uriString, displayName))
    }

    LaunchedEffect(newlyCreatedSignature) {
        if (newlyCreatedSignature != null) {
            viewModel.onIntent(PdfEditorIntent.PlaceSavedSignature(newlyCreatedSignature))
            onNewlyCreatedSignatureConsumed()
        }
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            PdfEditorEffect.NavigateBack -> onBack()
            PdfEditorEffect.NavigateToSignaturePad -> onOpenSignaturePad()
            PdfEditorEffect.NavigateToPaywall -> onOpenPaywall()
            is PdfEditorEffect.LaunchPrint -> launchSystemPdfPrint(context, effect.uriString, effect.displayName, onShowMessage)
            is PdfEditorEffect.ShowMessage -> onShowMessage(effect.message)
        }
    }

    PdfEditorScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PdfEditorScreen(
    state: PdfEditorState,
    onIntent: (PdfEditorIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pdfFormat = com.evora.technologies.office.core.designsystem.theme.EvoraThemeTokens.formats.paletteFor(DocFormat.PDF)
    val activeInkPoints = remember { mutableStateListOf<Pair<Float, Float>>() }
    var dragStart by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var dragCurrent by remember { mutableStateOf<Pair<Float, Float>?>(null) }

    var pendingP12Uri by remember { mutableStateOf<String?>(null) }
    val p12ModalPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingP12Uri = uri.toString()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.background(pdfFormat.headerBg)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .safeDrawingPadding()
                        .heightIn(min = 56.dp)
                        .padding(horizontal = EvoraSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EvoraIconButton(onClick = { onIntent(PdfEditorIntent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = pdfFormat.headerContent)
                    }
                    Text(
                        text = state.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = pdfFormat.headerContent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    EvoraIconButton(onClick = { onIntent(PdfEditorIntent.UndoLastAnnotation) }) {
                        Icon(Icons.AutoMirrored.Outlined.Undo, "Undo", tint = pdfFormat.headerContent)
                    }
                    EvoraIconButton(onClick = { onIntent(PdfEditorIntent.SavePdfClicked) }) {
                        Icon(Icons.Outlined.Save, "Save PDF", tint = pdfFormat.headerContent)
                    }
                    EvoraIconButton(onClick = { onIntent(PdfEditorIntent.PrintClicked) }) {
                        Icon(Icons.Outlined.Print, "Print PDF", tint = pdfFormat.headerContent)
                    }
                }
                pdfFormat.darkHeaderAccent?.let { HorizontalDivider(thickness = 2.dp, color = it) }
            }
        },
        bottomBar = {
            Surface(tonalElevation = 6.dp, color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    TabRow(selectedTabIndex = state.activeTab.ordinal) {
                        PdfEditorTab.entries.forEach { tab ->
                            Tab(
                                selected = state.activeTab == tab,
                                onClick = { onIntent(PdfEditorIntent.SelectTab(tab)) },
                                text = { Text(tab.label, maxLines = 1) },
                            )
                        }
                    }
                    when (state.activeTab) {
                        PdfEditorTab.ANNOTATE -> Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(EvoraSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(PdfAnnotationType.entries, key = { it.name }) { tool ->
                                    FilterChip(
                                        selected = state.activeTool == tool,
                                        onClick = { onIntent(PdfEditorIntent.SelectTool(tool)) },
                                        label = {
                                            Text(
                                                when (tool) {
                                                    PdfAnnotationType.HIGHLIGHT -> "Highlight"
                                                    PdfAnnotationType.UNDERLINE -> "Underline"
                                                    PdfAnnotationType.STRIKETHROUGH -> "Strike"
                                                    PdfAnnotationType.INK -> "Ink Draw"
                                                    PdfAnnotationType.STICKY_NOTE -> "Note"
                                                },
                                            )
                                        },
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ANNOTATION_SWATCHES.forEach { argb ->
                                        val selected = state.activeColorArgb == argb
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(argb),
                                            border = BorderStroke(
                                                width = if (selected) 3.dp else 1.dp,
                                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            ),
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .clickable { onIntent(PdfEditorIntent.SelectColor(argb)) },
                                        ) {}
                                    }
                                }
                                TextButton(onClick = { onIntent(PdfEditorIntent.ClearCurrentPageAnnotations) }) {
                                    Text("Clear Page")
                                }
                            }
                        }

                        PdfEditorTab.SIGNATURE -> Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(EvoraSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                EvoraPrimaryButton(
                                    text = "New Signature",
                                    onClick = { onIntent(PdfEditorIntent.OpenSignaturePadClicked) },
                                    modifier = Modifier.weight(1f),
                                )
                                EvoraOutlinedButton(
                                    text = "PKCS#7 Cert",
                                    onClick = { onIntent(PdfEditorIntent.InspectPkcs7Clicked) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (state.savedSignatures.isNotEmpty()) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(state.savedSignatures, key = { it.id }) { sig ->
                                        val bmp = remember(sig.assetPath) { runCatching { BitmapFactory.decodeFile(sig.assetPath) }.getOrNull() }
                                        Surface(
                                            shape = EvoraRadii.sm,
                                            color = Color.White,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier
                                                .width(104.dp)
                                                .height(48.dp)
                                                .clickable { onIntent(PdfEditorIntent.PlaceSavedSignature(sig)) },
                                        ) {
                                            if (bmp != null) {
                                                Image(bmp.asImageBitmap(), "Tap to stamp", modifier = Modifier.padding(4.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        PdfEditorTab.FILE -> Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(EvoraSpacing.sm),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            EvoraOutlinedButton(
                                text = "Lock AES",
                                onClick = { onIntent(PdfEditorIntent.OpenSetPasswordDialog) },
                                modifier = Modifier.weight(1f),
                            )
                            EvoraOutlinedButton(
                                text = "Unlock",
                                onClick = { onIntent(PdfEditorIntent.OpenRemovePasswordDialog) },
                                modifier = Modifier.weight(1f),
                            )
                            EvoraPrimaryButton(
                                text = "Save",
                                onClick = { onIntent(PdfEditorIntent.SavePdfClicked) },
                                isLoading = state.isSaving,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        PdfEditorTab.PAGE -> Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(EvoraSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            EvoraIconButton(
                                onClick = { onIntent(PdfEditorIntent.GoToPage(state.currentPage - 1)) },
                                enabled = state.currentPage > 0,
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous page")
                            }
                            Text(
                                text = "Page ${state.currentPage + 1} / ${state.totalPages.coerceAtLeast(1)}",
                                style = EvoraTabularMonoStyle,
                            )
                            EvoraIconButton(
                                onClick = { onIntent(PdfEditorIntent.GoToPage(state.currentPage + 1)) },
                                enabled = state.currentPage < state.totalPages - 1,
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next page")
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFE2E8F0)),
            contentAlignment = Alignment.Center,
        ) {
            val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
            LaunchedEffect(widthPx, state.currentPage) {
                if (state.totalPages > 0) {
                    onIntent(PdfEditorIntent.RequestRenderPage(widthPx))
                }
            }

            when {
                state.isLoading -> CircularProgressIndicator()
                state.errorMessage != null -> ErrorStateCard(
                    title = "Unable to open PDF",
                    description = state.errorMessage,
                    actionLabel = "Back",
                    onActionClick = { onIntent(PdfEditorIntent.BackClicked) },
                    modifier = Modifier.padding(EvoraSpacing.lg),
                )
                state.pageRaster != null -> {
                    val raster = state.pageRaster
                    val bmp = remember(raster) {
                        Bitmap.createBitmap(raster.width, raster.height, Bitmap.Config.ARGB_8888).apply {
                            setPixels(raster.argbPixels, 0, raster.width, 0, 0, raster.width, raster.height)
                        }
                    }
                    val aspect = raster.width.toFloat() / raster.height.coerceAtLeast(1).toFloat()

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .aspectRatio(aspect)
                            .background(Color.White),
                    ) {
                        val boxW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                        val boxH = constraints.maxHeight.toFloat().coerceAtLeast(1f)

                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "PDF page ${state.currentPage + 1}",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxSize(),
                        )

                        // Annotation drawing & tap overlay
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(state.activeTool, state.currentPage) {
                                    if (state.activeTool == PdfAnnotationType.STICKY_NOTE) {
                                        detectTapGestures { pos ->
                                            onIntent(PdfEditorIntent.OpenStickyNoteAt(pos.x / boxW, pos.y / boxH))
                                        }
                                    } else {
                                        detectDragGestures(
                                            onDragStart = { pos ->
                                                val nx = (pos.x / boxW).coerceIn(0f, 1f)
                                                val ny = (pos.y / boxH).coerceIn(0f, 1f)
                                                if (state.activeTool == PdfAnnotationType.INK) {
                                                    activeInkPoints.clear()
                                                    activeInkPoints.add(nx to ny)
                                                } else {
                                                    dragStart = nx to ny
                                                    dragCurrent = nx to ny
                                                }
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                val nx = (change.position.x / boxW).coerceIn(0f, 1f)
                                                val ny = (change.position.y / boxH).coerceIn(0f, 1f)
                                                if (state.activeTool == PdfAnnotationType.INK) {
                                                    activeInkPoints.add(nx to ny)
                                                } else {
                                                    dragCurrent = nx to ny
                                                }
                                            },
                                            onDragEnd = {
                                                if (state.activeTool == PdfAnnotationType.INK) {
                                                    if (activeInkPoints.size >= 2) {
                                                        onIntent(PdfEditorIntent.AddInkStroke(activeInkPoints.toList()))
                                                    }
                                                    activeInkPoints.clear()
                                                } else {
                                                    val s = dragStart
                                                    val e = dragCurrent
                                                    if (s != null && e != null) {
                                                        onIntent(PdfEditorIntent.AddMarkupBox(s.first, s.second, e.first, e.second))
                                                    }
                                                    dragStart = null
                                                    dragCurrent = null
                                                }
                                            },
                                        )
                                    }
                                },
                        ) {
                            val pageAnnots = state.annotations.filter { it.pageIndex == state.currentPage }
                            for (annot in pageAnnots) {
                                val c = Color(annot.colorArgb)
                                when (annot.type) {
                                    PdfAnnotationType.HIGHLIGHT -> drawRect(
                                        color = c.copy(alpha = 0.35f),
                                        topLeft = Offset(annot.left * size.width, annot.top * size.height),
                                        size = Size((annot.right - annot.left) * size.width, (annot.bottom - annot.top) * size.height),
                                    )
                                    PdfAnnotationType.UNDERLINE -> {
                                        val y = annot.bottom * size.height
                                        drawLine(c, Offset(annot.left * size.width, y), Offset(annot.right * size.width, y), 4f)
                                    }
                                    PdfAnnotationType.STRIKETHROUGH -> {
                                        val y = ((annot.top + annot.bottom) / 2f) * size.height
                                        drawLine(c, Offset(annot.left * size.width, y), Offset(annot.right * size.width, y), 4f)
                                    }
                                    PdfAnnotationType.INK -> if (annot.points.size >= 2) {
                                        val p = Path().apply {
                                            moveTo(annot.points.first().x * size.width, annot.points.first().y * size.height)
                                            for (i in 1 until annot.points.size) {
                                                lineTo(annot.points[i].x * size.width, annot.points[i].y * size.height)
                                            }
                                        }
                                        drawPath(p, c, style = Stroke(annot.strokeWidthDp * 2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                    }
                                    PdfAnnotationType.STICKY_NOTE -> {
                                        drawCircle(c, radius = 16f, center = Offset(annot.left * size.width, annot.top * size.height))
                                    }
                                }
                            }
                            if (activeInkPoints.size >= 2) {
                                val p = Path().apply {
                                    moveTo(activeInkPoints.first().first * size.width, activeInkPoints.first().second * size.height)
                                    for (i in 1 until activeInkPoints.size) {
                                        lineTo(activeInkPoints[i].first * size.width, activeInkPoints[i].second * size.height)
                                    }
                                }
                                drawPath(p, Color(state.activeColorArgb), style = Stroke(state.inkStrokeWidthDp * 2f, cap = StrokeCap.Round))
                            }
                            val s = dragStart
                            val e = dragCurrent
                            if (s != null && e != null) {
                                drawRect(
                                    color = Color(state.activeColorArgb).copy(alpha = 0.35f),
                                    topLeft = Offset(minOf(s.first, e.first) * size.width, minOf(s.second, e.second) * size.height),
                                    size = Size(kotlin.math.abs(e.first - s.first) * size.width, kotlin.math.abs(e.second - s.second) * size.height),
                                )
                            }
                        }

                        // Sticky Note pins
                        state.annotations.filter { it.pageIndex == state.currentPage && it.type == PdfAnnotationType.STICKY_NOTE }.forEach { note ->
                            Icon(
                                imageVector = Icons.Outlined.Comment,
                                contentDescription = note.noteText,
                                tint = Color(0xFFB45309),
                                modifier = Modifier
                                    .offset {
                                        IntOffset((note.left * boxW).roundToInt() - 16, (note.top * boxH).roundToInt() - 16)
                                    }
                                    .size(24.dp),
                            )
                        }

                        // Draggable placed signatures
                        state.signatures.filter { it.pageIndex == state.currentPage }.forEach { sig ->
                            val sigBmp = remember(sig.signatureAssetPath) {
                                runCatching { BitmapFactory.decodeFile(sig.signatureAssetPath) }.getOrNull()
                            }
                            val isSelected = state.selectedSignaturePlacementId == sig.id
                            val sigW = (boxW * sig.widthFraction).roundToInt().coerceAtLeast(80)
                            val sigH = (sigW * 0.45f).roundToInt()
                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            (sig.centerXFraction * boxW - sigW / 2f).roundToInt(),
                                            (sig.centerYFraction * boxH - sigH / 2f).roundToInt(),
                                        )
                                    }
                                    .size(with(LocalDensity.current) { sigW.toDp() }, with(LocalDensity.current) { sigH.toDp() })
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    )
                                    .pointerInput(sig.id) {
                                        detectDragGestures(
                                            onDragStart = { onIntent(PdfEditorIntent.SelectPlacedSignature(sig.id)) },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                onIntent(PdfEditorIntent.MoveSelectedSignature(dragAmount.x / boxW, dragAmount.y / boxH))
                                            },
                                        )
                                    }
                                    .clickable { onIntent(PdfEditorIntent.SelectPlacedSignature(sig.id)) },
                            ) {
                                if (sigBmp != null) {
                                    Image(sigBmp.asImageBitmap(), "Signature", modifier = Modifier.fillMaxSize())
                                }
                                if (isSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(22.dp)
                                            .clickable { onIntent(PdfEditorIntent.RemovePlacedSignature(sig.id)) },
                                    ) {
                                        Icon(Icons.Outlined.Close, "Delete signature", tint = Color.White, modifier = Modifier.padding(3.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showStickyNoteDialog) {
        var noteBody by remember { mutableStateOf("") }
        var author by remember { mutableStateOf("Evora User") }
        AlertDialog(
            onDismissRequest = { onIntent(PdfEditorIntent.DismissStickyNoteDialog) },
            title = { Text("Add Sticky Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("Author") }, singleLine = true)
                    OutlinedTextField(value = noteBody, onValueChange = { noteBody = it }, label = { Text("Note comment") }, minLines = 2)
                }
            },
            confirmButton = {
                TextButton(onClick = { onIntent(PdfEditorIntent.SaveStickyNote(noteBody, author)) }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(PdfEditorIntent.DismissStickyNoteDialog) }) { Text("Cancel") }
            },
        )
    }

    if (state.showCertModal) {
        var p12Pass by remember { mutableStateOf("") }
        var reason by remember { mutableStateOf("Approved") }
        ModalBottomSheet(onDismissRequest = { onIntent(PdfEditorIntent.DismissCertModal) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(EvoraSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(EvoraSpacing.md),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.VerifiedUser, null)
                    Text("PKCS#7 Digital Certificates", style = MaterialTheme.typography.titleLarge)
                }
                if (state.pkcs7Certs.isEmpty()) {
                    Text("No embedded PKCS#7 signatures in this document yet.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    state.pkcs7Certs.forEach { cert ->
                        Surface(shape = EvoraRadii.sm, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(EvoraSpacing.sm)) {
                                Text(cert.signerName, style = MaterialTheme.typography.titleSmall)
                                Text("Issuer: ${cert.issuerName}", style = MaterialTheme.typography.labelMedium)
                                Text("Integrity: ${if (cert.isIntegrityValid) "Valid" else "Invalid"} · Valid Window: ${if (cert.isCurrentlyWithinValidity) "Active" else "Expired"}", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                HorizontalDivider()
                Text("Sign with Local .p12 / .pfx Keystore", style = MaterialTheme.typography.titleSmall)
                EvoraOutlinedButton(
                    text = if (pendingP12Uri == null) "Select .p12 / .pfx File" else "Certificate Selected",
                    onClick = { p12ModalPicker.launch(arrayOf("*/*")) },
                )
                OutlinedTextField(
                    value = p12Pass,
                    onValueChange = { p12Pass = it },
                    label = { Text("Keystore Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                EvoraPrimaryButton(
                    text = "Sign PDF Cryptographically",
                    onClick = {
                        val uri = pendingP12Uri ?: return@EvoraPrimaryButton
                        onIntent(PdfEditorIntent.SignWithP12(uri, p12Pass, reason))
                    },
                )
                Spacer(modifier = Modifier.height(EvoraSpacing.md))
            }
        }
    }

    if (state.showSetPasswordDialog) {
        var pass by remember { mutableStateOf("") }
        var aes256 by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { onIntent(PdfEditorIntent.DismissSecurityDialogs) },
            title = { Text("Protect PDF with Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation())
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("AES-256")
                        Switch(checked = aes256, onCheckedChange = { aes256 = it })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onIntent(PdfEditorIntent.ConfirmSetPassword(pass, aes256)) }) { Text("Lock") }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(PdfEditorIntent.DismissSecurityDialogs) }) { Text("Cancel") }
            },
        )
    }

    if (state.showRemovePasswordDialog) {
        var curPass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { onIntent(PdfEditorIntent.DismissSecurityDialogs) },
            title = { Text("Remove PDF Password") },
            text = {
                OutlinedTextField(value = curPass, onValueChange = { curPass = it }, label = { Text("Current Password") }, visualTransformation = PasswordVisualTransformation())
            },
            confirmButton = {
                TextButton(onClick = { onIntent(PdfEditorIntent.ConfirmRemovePassword(curPass)) }) { Text("Unlock") }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(PdfEditorIntent.DismissSecurityDialogs) }) { Text("Cancel") }
            },
        )
    }

    if (state.passwordRequired) {
        PasswordUnlockDialog(
            fileName = state.displayName,
            errorMessage = state.passwordError,
            onUnlock = { onIntent(PdfEditorIntent.SubmitOpenPassword(it)) },
            onDismiss = { onIntent(PdfEditorIntent.DismissOpenPassword) },
        )
    }

    state.showRewardGateTitle?.let { title ->
        EvoraRewardGateSheet(
            featureTitle = title,
            onWatchRewardedPass = { onIntent(PdfEditorIntent.GrantRewardPassAndProceed) },
            onUpgradePro = { onIntent(PdfEditorIntent.UpgradeProClicked) },
            onDismiss = { onIntent(PdfEditorIntent.DismissRewardGate) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PdfEditorPreview() {
    EvoraTheme {
        PdfEditorScreen(
            state = PdfEditorState(isLoading = false, totalPages = 3),
            onIntent = {},
        )
    }
}
