package com.evora.technologies.office.core.designsystem.component

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraTabularMonoStyle
import kotlin.math.roundToInt

/**
 * Right-edge draggable page scrub pill ("12 / 45") inside a 48dp edge touch track
 * with 32dp vertical rail insets to avoid OS edge-back gesture collisions (05_DESIGN_SYSTEM.md §4.1).
 */
@Composable
fun PageIndicatorPill(
    currentPage: Int,
    totalPages: Int,
    onJumpToPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (totalPages <= 0) return

    val density = LocalDensity.current
    val insetPx = with(density) { 32.dp.toPx() }
    val pillHeightPx = with(density) { 36.dp.toPx() }
    var trackHeightPx by remember { mutableIntStateOf(0) }
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val activeFraction = if (isDragging) {
        dragFraction
    } else if (totalPages <= 1) {
        0f
    } else {
        (currentPage.toFloat() / (totalPages - 1).toFloat()).coerceIn(0f, 1f)
    }

    val usableTravel = (trackHeightPx - insetPx * 2f - pillHeightPx).coerceAtLeast(1f)
    val pillOffsetY = (insetPx + usableTravel * activeFraction).roundToInt()
    val displayPage = if (isDragging) {
        (activeFraction * (totalPages - 1)).roundToInt().coerceIn(0, totalPages - 1) + 1
    } else {
        currentPage.coerceIn(0, totalPages - 1) + 1
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(48.dp)
            .onSizeChanged { trackHeightPx = it.height }
            .pointerInput(totalPages) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragFraction = ((offset.y - insetPx) / usableTravel).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        val target = (dragFraction * (totalPages - 1)).roundToInt().coerceIn(0, totalPages - 1)
                        isDragging = false
                        onJumpToPage(target)
                    },
                    onDragCancel = { isDragging = false },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        dragFraction = ((change.position.y - insetPx) / usableTravel).coerceIn(0f, 1f)
                    },
                )
            },
        contentAlignment = Alignment.TopEnd,
    ) {
        Surface(
            shape = EvoraRadii.full,
            color = Color.Black.copy(alpha = if (isDragging) 0.88f else 0.75f),
            shadowElevation = 4.dp,
            modifier = Modifier
                .offset { IntOffset(x = 0, y = pillOffsetY) }
                .padding(end = 4.dp)
                .heightIn(min = 36.dp)
                .semantics { contentDescription = "Page $displayPage of $totalPages" },
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$displayPage / $totalPages",
                    style = EvoraTabularMonoStyle,
                    color = Color.White,
                    maxLines = 1,
                )
            }
        }
    }
}
