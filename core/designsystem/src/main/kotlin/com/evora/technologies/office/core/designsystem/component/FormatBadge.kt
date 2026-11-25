package com.evora.technologies.office.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraThemeTokens
import com.evora.technologies.office.core.model.DocFormat

@Composable
fun FormatBadge(
    format: DocFormat,
    extension: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val palette = EvoraThemeTokens.formats.paletteFor(format)
    val label = extension.trimStart('.').uppercase().take(4).ifBlank { format.name }
    Box(
        modifier = modifier
            .size(size)
            .clip(EvoraRadii.sm)
            .background(palette.tintBg)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = if (label.length >= 4) 11.sp else 12.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = palette.inkText,
            maxLines = 1,
        )
    }
}
