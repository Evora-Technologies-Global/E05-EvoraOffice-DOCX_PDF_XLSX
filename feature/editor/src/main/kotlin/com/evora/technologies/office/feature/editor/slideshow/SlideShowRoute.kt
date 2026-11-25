package com.evora.technologies.office.feature.editor.slideshow

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.SpeakerNotes
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.office.core.designsystem.component.EvoraIconButton
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraTabularMonoStyle
import com.evora.technologies.office.core.designsystem.theme.EvoraTheme
import com.evora.technologies.office.core.model.SlideStrokeStyle
import com.evora.technologies.office.core.mvi.CollectEffects
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SlideShowRoute(
    uriString: String,
    startSlideIndex: Int = 0,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SlideShowViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    LaunchedEffect(uriString, startSlideIndex) {
        viewModel.onIntent(SlideShowIntent.OpenPresentation(uriString, startSlideIndex))
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            SlideShowEffect.NavigateBack -> onBack()
        }
    }

    SlideShowScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
internal fun SlideShowScreen(
    state: SlideShowState,
    onIntent: (SlideShowIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeLaser = remember { mutableStateListOf<Pair<Float, Float>>() }
    val pagerState = rememberPagerState(initialPage = state.currentSlide) {
        state.slides.size.coerceAtLeast(1)
    }

    LaunchedEffect(state.currentSlide) {
        if (pagerState.currentPage != state.currentSlide && state.slides.isNotEmpty()) {
            pagerState.animateScrollToPage(state.currentSlide)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (state.slides.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !state.isLaserEnabled,
                modifier = Modifier.fillMaxSize(),
            ) { pageIdx ->
                val slide = state.slides[pageIdx]
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(EvoraSpacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color(slide.backgroundColorArgb)),
                    ) {
                        val wPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                        val hPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                        slide.elements.sortedBy { it.zOrder }.forEach { el ->
                            val shape = if (el.kind == "SHAPE_OVAL") CircleShape else RoundedCornerShape(8.dp)
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset((el.xFraction * wPx).roundToInt(), (el.yFraction * hPx).roundToInt()) }
                                    .size(maxWidth * el.widthFraction, maxHeight * el.heightFraction)
                                    .clip(shape)
                                    .background(Color(el.fillColorArgb))
                                    .border(
                                        width = if (el.strokeStyle == SlideStrokeStyle.NONE) 0.dp else el.strokeWidthDp.dp,
                                        color = Color(el.strokeColorArgb),
                                        shape = shape,
                                    )
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(el.text, color = Color(0xFF0F172A), textAlign = TextAlign.Center)
                            }
                        }

                        // Laser pointer ink overlay
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (state.isLaserEnabled) {
                                        Modifier.pointerInput(Unit) {
                                            detectDragGestures(
                                                onDragStart = { pos ->
                                                    activeLaser.clear()
                                                    activeLaser.add((pos.x / wPx) to (pos.y / hPx))
                                                },
                                                onDrag = { change, _ ->
                                                    change.consume()
                                                    activeLaser.add((change.position.x / wPx) to (change.position.y / hPx))
                                                },
                                                onDragEnd = {
                                                    if (activeLaser.size >= 2) {
                                                        onIntent(SlideShowIntent.AddLaserStroke(activeLaser.toList()))
                                                    }
                                                    activeLaser.clear()
                                                },
                                            )
                                        }
                                    } else {
                                        Modifier
                                    },
                                ),
                        ) {
                            val all = if (activeLaser.size >= 2) state.laserStrokes + listOf(activeLaser.toList()) else state.laserStrokes
                            for (stroke in all) {
                                if (stroke.size < 2) continue
                                val p = Path().apply {
                                    moveTo(stroke.first().first * size.width, stroke.first().second * size.height)
                                    for (i in 1 until stroke.size) {
                                        lineTo(stroke[i].first * size.width, stroke[i].second * size.height)
                                    }
                                }
                                drawPath(p, Color(0xFFFF3B30), style = Stroke(8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            }
                        }
                    }
                }
            }
        }

        if (state.showNotes) {
            val notes = state.slides.getOrNull(state.currentSlide)?.presenterNotes.orEmpty()
            Surface(
                shape = EvoraRadii.md,
                color = Color.Black.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .safeDrawingPadding()
                    .padding(EvoraSpacing.md),
            ) {
                Text(
                    text = notes.ifBlank { "No presenter notes on this slide." },
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(EvoraSpacing.md),
                )
            }
        }

        // Translucent SlideShow HUD
        Surface(
            shape = EvoraRadii.full,
            color = Color.Black.copy(alpha = 0.72f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = EvoraSpacing.md),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = EvoraSpacing.md, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EvoraIconButton(onClick = { onIntent(SlideShowIntent.PrevSlide) }) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous slide", tint = Color.White)
                }
                Text(
                    text = "${state.currentSlide + 1} / ${state.slides.size.coerceAtLeast(1)}",
                    style = EvoraTabularMonoStyle,
                    color = Color.White,
                )
                EvoraIconButton(onClick = { onIntent(SlideShowIntent.NextSlide) }) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next slide", tint = Color.White)
                }
                val mins = state.elapsedSeconds / 60
                val secs = state.elapsedSeconds % 60
                Text(
                    text = String.format(Locale.US, "%02d:%02d", mins, secs),
                    style = EvoraTabularMonoStyle,
                    color = Color.White.copy(alpha = 0.85f),
                )
                EvoraIconButton(onClick = { onIntent(SlideShowIntent.ToggleLaserPointer) }) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Toggle laser pointer",
                        tint = if (state.isLaserEnabled) Color(0xFFFF453A) else Color.White,
                    )
                }
                EvoraIconButton(onClick = { onIntent(SlideShowIntent.ToggleNotes) }) {
                    Icon(Icons.Outlined.SpeakerNotes, "Presenter notes", tint = Color.White)
                }
                EvoraIconButton(onClick = { onIntent(SlideShowIntent.ExitClicked) }) {
                    Icon(Icons.Outlined.Close, "Exit SlideShow", tint = Color.White)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SlideShowPreview() {
    EvoraTheme {
        SlideShowScreen(
            state = SlideShowState(isLoading = false),
            onIntent = {},
        )
    }
}
