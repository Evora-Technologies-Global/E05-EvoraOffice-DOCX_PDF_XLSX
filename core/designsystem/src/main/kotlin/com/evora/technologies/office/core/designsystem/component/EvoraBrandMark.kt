package com.evora.technologies.office.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evora.technologies.office.core.designsystem.theme.PoppinsFontFamily

/**
 * Temporary geometric "EO" monogram placeholder per CD-06 until final app icon milestone.
 */
@Composable
fun EvoraBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
) {
    val cornerRadius = if (size >= 56.dp) 20.dp else 10.dp
    val fontSize = if (size >= 56.dp) 26.sp else 13.sp
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF2489FF), Color(0xFF186FD4)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "EO",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            color = Color.White,
        )
    }
}
