package com.evora.technologies.office.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Style B — Familiar Vibrant Utility palette tokens locked in docs/05_DESIGN_SYSTEM.md (CD-06 / CD-10).
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF186FD4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6F1FF),
    onPrimaryContainer = Color(0xFF0D4A9E),
    background = Color(0xFFF2F4F8),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE9EEF5),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF7C8BA1),
    outlineVariant = Color(0xFFCBD5E1),
    error = Color(0xFFC82828),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEECEC),
    onErrorContainer = Color(0xFF8C1D1D),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4DA1FF),
    onPrimary = Color(0xFF061D3D),
    primaryContainer = Color(0xFF183359),
    onPrimaryContainer = Color(0xFFBFDBFE),
    background = Color(0xFF101318),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1A1E26),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF232934),
    onSurfaceVariant = Color(0xFFB0BAC9),
    outline = Color(0xFF68768C),
    outlineVariant = Color(0xFF333C4D),
    error = Color(0xFFFF7878),
    onError = Color(0xFF3B0A0A),
    errorContainer = Color(0xFF3F171A),
    onErrorContainer = Color(0xFFFECACA),
)

/**
 * Evora Office Material 3 theme. dynamicColor is locked to false per CD-06 so
 * document format-coded colors remain recognizable across OEM wallpapers.
 */
@Composable
fun EvoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content,
    )
}
