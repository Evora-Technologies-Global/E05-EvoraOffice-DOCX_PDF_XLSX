package com.evora.technologies.office.feature.editor.signature

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.office.core.designsystem.component.EvoraIconButton
import com.evora.technologies.office.core.designsystem.component.EvoraOutlinedButton
import com.evora.technologies.office.core.designsystem.component.EvoraPrimaryButton
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraTheme
import com.evora.technologies.office.core.model.SignatureItem
import com.evora.technologies.office.core.mvi.CollectEffects

private val SIGNATURE_INK_COLORS = listOf(
    0xFF0F172AL,
    0xFF1563DBL,
    0xFFC82828L,
)

@Composable
fun SignaturePadRoute(
    onSignatureSelected: (SignatureItem) -> Unit,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignaturePadViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is SignaturePadEffect.SignatureReady -> onSignatureSelected(effect.item)
            SignaturePadEffect.NavigateBack -> onBack()
            is SignaturePadEffect.ShowMessage -> onShowMessage(effect.message)
        }
    }

    SignaturePadScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
internal fun SignaturePadScreen(
    state: SignaturePadState,
    onIntent: (SignaturePadIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeStroke = remember { mutableStateListOf<Pair<Float, Float>>() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .safeDrawingPadding()
                        .heightIn(min = 56.dp)
                        .padding(horizontal = EvoraSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EvoraIconButton(onClick = { onIntent(SignaturePadIntent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                    Text("Handwritten Signature", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    EvoraIconButton(onClick = { onIntent(SignaturePadIntent.UndoStroke) }) {
                        Icon(Icons.AutoMirrored.Outlined.Undo, "Undo stroke")
                    }
                    TextButton(onClick = { onIntent(SignaturePadIntent.ClearCanvas) }) {
                        Text("Clear")
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(EvoraSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(EvoraSpacing.md),
        ) {
            Text(
                "Sign inside the pad below. Transparent PNG saves locally for reuse across PDFs.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(EvoraRadii.lg)
                    .background(Color.White)
                    .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, EvoraRadii.lg)
                    .onSizeChanged { size ->
                        onIntent(SignaturePadIntent.CanvasSized(size.width, size.height))
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { pos ->
                                activeStroke.clear()
                                activeStroke.add(pos.x to pos.y)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                activeStroke.add(change.position.x to change.position.y)
                            },
                            onDragEnd = {
                                if (activeStroke.isNotEmpty()) {
                                    onIntent(SignaturePadIntent.AddStroke(activeStroke.toList()))
                                    activeStroke.clear()
                                }
                            },
                            onDragCancel = { activeStroke.clear() },
                        )
                    },
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawLine(
                        color = Color(0xFFCBD5E1),
                        start = Offset(32f, size.height * 0.78f),
                        end = Offset(size.width - 32f, size.height * 0.78f),
                        strokeWidth = 2f,
                    )
                    val allStrokes = if (activeStroke.isNotEmpty()) state.strokes + listOf(activeStroke.toList()) else state.strokes
                    for (stroke in allStrokes) {
                        if (stroke.isEmpty()) continue
                        val path = Path().apply {
                            moveTo(stroke.first().first, stroke.first().second)
                            for (i in 1 until stroke.size) {
                                lineTo(stroke[i].first, stroke[i].second)
                            }
                        }
                        drawPath(
                            path = path,
                            color = Color(state.selectedColorArgb),
                            style = Stroke(
                                width = state.strokeWidthPx,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.sm)) {
                    SIGNATURE_INK_COLORS.forEach { argb ->
                        val selected = state.selectedColorArgb == argb
                        Surface(
                            shape = CircleShape,
                            color = Color(argb),
                            border = BorderStroke(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            ),
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { onIntent(SignaturePadIntent.SelectColor(argb)) },
                        ) {}
                    }
                }
                Text("Stroke ${state.strokeWidthPx.toInt()}px", style = MaterialTheme.typography.labelMedium)
            }
            Slider(
                value = state.strokeWidthPx,
                onValueChange = { onIntent(SignaturePadIntent.SetStrokeWidth(it)) },
                valueRange = 3f..18f,
            )

            state.errorMessage?.let { err ->
                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
            }

            EvoraPrimaryButton(
                text = "Save & Place Signature",
                onClick = { onIntent(SignaturePadIntent.SaveSignatureClicked) },
                isLoading = state.isSaving,
            )

            if (state.savedSignatures.isNotEmpty()) {
                Text("Saved Signatures (Tap to Place)", style = MaterialTheme.typography.titleSmall)
                LazyRow(
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                ) {
                    items(state.savedSignatures, key = { it.id }) { sig ->
                        val bmp = remember(sig.assetPath) {
                            runCatching { BitmapFactory.decodeFile(sig.assetPath) }.getOrNull()
                        }
                        Surface(
                            shape = EvoraRadii.md,
                            color = Color.White,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .width(156.dp)
                                .height(84.dp)
                                .clip(EvoraRadii.md)
                                .clickable { onIntent(SignaturePadIntent.SelectSavedSignature(sig)) },
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Saved signature",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp),
                                    )
                                }
                                EvoraIconButton(
                                    onClick = { onIntent(SignaturePadIntent.DeleteSavedSignature(sig.id)) },
                                    modifier = Modifier.align(Alignment.TopEnd),
                                ) {
                                    Icon(Icons.Outlined.DeleteOutline, "Delete signature", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SignaturePadPreview() {
    EvoraTheme {
        SignaturePadScreen(
            state = SignaturePadState(),
            onIntent = {},
        )
    }
}
