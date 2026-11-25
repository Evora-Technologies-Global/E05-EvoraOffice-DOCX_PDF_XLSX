package com.evora.technologies.office.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraThemeTokens

/** Ensures any custom interactive icon or touch element enforces >= 48dp hit box. */
@Composable
fun MinTouchTargetBox(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .sizeIn(minWidth = EvoraSpacing.minTouchTarget, minHeight = EvoraSpacing.minTouchTarget),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun EvoraIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .sizeIn(minWidth = EvoraSpacing.minTouchTarget, minHeight = EvoraSpacing.minTouchTarget),
        content = content,
    )
}

@Composable
fun EvoraPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val extra = EvoraThemeTokens.colors
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = EvoraRadii.md,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = extra.surfaceSubtle,
            disabledContentColor = extra.textMuted.copy(alpha = 0.6f),
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = EvoraSpacing.minTouchTarget),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(modifier = Modifier.width(EvoraSpacing.sm))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun EvoraOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = EvoraRadii.md,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = EvoraSpacing.minTouchTarget),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
