package com.evora.technologies.office.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.ReaderTheme

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
    surfaceContainerLow = Color(0xFFE9EEF5),
    surfaceContainerHigh = Color(0xFFFFFFFF),
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
    surfaceContainerLow = Color(0xFF232934),
    surfaceContainerHigh = Color(0xFF252B36),
    outline = Color(0xFF68768C),
    outlineVariant = Color(0xFF333C4D),
    error = Color(0xFFFF7878),
    onError = Color(0xFF3B0A0A),
    errorContainer = Color(0xFF3F171A),
    onErrorContainer = Color(0xFFFECACA),
)

@Immutable
data class EvoraExtraColors(
    val brandAccent: Color,
    val onBrandAccent: Color,
    val textMuted: Color,
    val surfaceSubtle: Color,
    val surfaceElevated: Color,
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val proGoldContainer: Color,
    val onProGoldContainer: Color,
    val isDark: Boolean,
)

private val LightExtraColors = EvoraExtraColors(
    brandAccent = Color(0xFF2489FF),
    onBrandAccent = Color(0xFFFFFFFF),
    textMuted = Color(0xFF526075),
    surfaceSubtle = Color(0xFFE9EEF5),
    surfaceElevated = Color(0xFFFFFFFF),
    success = Color(0xFF1E7D5E),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFE3F8F0),
    onSuccessContainer = Color(0xFF125741),
    warning = Color(0xFFB45309),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFEF3C7),
    onWarningContainer = Color(0xFF78350F),
    proGoldContainer = Color(0xFFFFF4DB),
    onProGoldContainer = Color(0xFF7A4B00),
    isDark = false,
)

private val DarkExtraColors = EvoraExtraColors(
    brandAccent = Color(0xFF4DA1FF),
    onBrandAccent = Color(0xFF061D3D),
    textMuted = Color(0xFF94A1B5),
    surfaceSubtle = Color(0xFF232934),
    surfaceElevated = Color(0xFF252B36),
    success = Color(0xFF46D8A9),
    onSuccess = Color(0xFF04261B),
    successContainer = Color(0xFF12382B),
    onSuccessContainer = Color(0xFF86EFCD),
    warning = Color(0xFFFBBF24),
    onWarning = Color(0xFF2D1902),
    warningContainer = Color(0xFF3B2809),
    onWarningContainer = Color(0xFFFDE68A),
    proGoldContainer = Color(0xFF3A2B0D),
    onProGoldContainer = Color(0xFFFCD34D),
    isDark = true,
)

@Immutable
data class FormatColorPalette(
    val headerBg: Color,
    val headerContent: Color,
    val darkHeaderAccent: Color?,
    val tintBg: Color,
    val inkText: Color,
)

@Immutable
data class EvoraFormatColors(
    val word: FormatColorPalette,
    val pdf: FormatColorPalette,
    val excel: FormatColorPalette,
    val ppt: FormatColorPalette,
    val hwp: FormatColorPalette,
    val odf: FormatColorPalette,
    val epub: FormatColorPalette,
    val textArchive: FormatColorPalette,
) {
    fun paletteFor(format: DocFormat): FormatColorPalette = when (format) {
        DocFormat.WORD -> word
        DocFormat.PDF -> pdf
        DocFormat.EXCEL -> excel
        DocFormat.PPT -> ppt
        DocFormat.HWP -> hwp
        DocFormat.ODF -> odf
        DocFormat.EPUB -> epub
        DocFormat.TEXT, DocFormat.ARCHIVE -> textArchive
    }
}

private val LightFormatColors = EvoraFormatColors(
    word = FormatColorPalette(Color(0xFF1563DB), Color.White, null, Color(0xFFEAF2FF), Color(0xFF114FB3)),
    pdf = FormatColorPalette(Color(0xFFD33434), Color.White, null, Color(0xFFFFEBEB), Color(0xFFA81E1E)),
    excel = FormatColorPalette(Color(0xFF1D820E), Color.White, null, Color(0xFFE6F7E4), Color(0xFF146308)),
    ppt = FormatColorPalette(Color(0xFFB85600), Color.White, null, Color(0xFFFFF2E4), Color(0xFF8F4100)),
    hwp = FormatColorPalette(Color(0xFF0074BF), Color.White, null, Color(0xFFE5F5FF), Color(0xFF005894)),
    odf = FormatColorPalette(Color(0xFF8B1ED9), Color.White, null, Color(0xFFF5EAFF), Color(0xFF6C14AD)),
    epub = FormatColorPalette(Color(0xFF5A7D0E), Color.White, null, Color(0xFFF0F8DF), Color(0xFF446107)),
    textArchive = FormatColorPalette(Color(0xFF1474B0), Color.White, null, Color(0xFFE6F5FF), Color(0xFF0D5887)),
)

private val DarkFormatColors = EvoraFormatColors(
    word = FormatColorPalette(Color(0xFF162642), Color(0xFFF1F5F9), Color(0xFF4D8DF0), Color(0xFF1B2E4F), Color(0xFF89B7FF)),
    pdf = FormatColorPalette(Color(0xFF38181A), Color(0xFFF1F5F9), Color(0xFFFF7878), Color(0xFF401C1F), Color(0xFFFF9494)),
    excel = FormatColorPalette(Color(0xFF142B16), Color(0xFFF1F5F9), Color(0xFF48C736), Color(0xFF17331A), Color(0xFF75E067)),
    ppt = FormatColorPalette(Color(0xFF36200D), Color(0xFFF1F5F9), Color(0xFFFF9838), Color(0xFF3D2510), Color(0xFFFFB067)),
    hwp = FormatColorPalette(Color(0xFF0F273B), Color(0xFFF1F5F9), Color(0xFF33A5EE), Color(0xFF133047), Color(0xFF66C3FF)),
    odf = FormatColorPalette(Color(0xFF2B183E), Color(0xFFF1F5F9), Color(0xFFBD58FA), Color(0xFF341D4A), Color(0xFFD599FF)),
    epub = FormatColorPalette(Color(0xFF222E10), Color(0xFFF1F5F9), Color(0xFF9FD134), Color(0xFF293814), Color(0xFFB5E34F)),
    textArchive = FormatColorPalette(Color(0xFF13293B), Color(0xFFF1F5F9), Color(0xFF52BCFB), Color(0xFF173247), Color(0xFF72C8FF)),
)

@Immutable
data class ReaderPalette(
    val pageCanvas: Color,
    val bodyInk: Color,
    val mutedInk: Color,
    val pageSheetBg: Color,
    val htmlBackgroundHex: String,
    val htmlTextHex: String,
)

fun ReaderTheme.toPalette(): ReaderPalette = when (this) {
    ReaderTheme.LIGHT -> ReaderPalette(
        pageCanvas = Color(0xFFF2F4F8),
        bodyInk = Color(0xFF0F172A),
        mutedInk = Color(0xFF526075),
        pageSheetBg = Color(0xFFFFFFFF),
        htmlBackgroundHex = "#FFFFFF",
        htmlTextHex = "#0F172A",
    )
    ReaderTheme.DARK -> ReaderPalette(
        pageCanvas = Color(0xFF0F1216),
        bodyInk = Color(0xFFE2E8F0),
        mutedInk = Color(0xFF94A1B5),
        pageSheetBg = Color(0xFF171C24),
        htmlBackgroundHex = "#0F1216",
        htmlTextHex = "#E2E8F0",
    )
    ReaderTheme.SEPIA -> ReaderPalette(
        pageCanvas = Color(0xFFF5ECD8),
        bodyInk = Color(0xFF3B2F20),
        mutedInk = Color(0xFF6E5A42),
        pageSheetBg = Color(0xFFFAF4E6),
        htmlBackgroundHex = "#F5ECD8",
        htmlTextHex = "#3B2F20",
    )
}

val LocalEvoraColors = staticCompositionLocalOf { LightExtraColors }
val LocalEvoraFormatColors = staticCompositionLocalOf { LightFormatColors }

private val EvoraMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

object EvoraThemeTokens {
    val colors: EvoraExtraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalEvoraColors.current

    val formats: EvoraFormatColors
        @Composable
        @ReadOnlyComposable
        get() = LocalEvoraFormatColors.current
}

/**
 * Evora Office Material 3 theme. dynamicColor is locked to false per CD-06 so
 * document format-coded colors remain recognizable across OEM wallpapers.
 */
@Composable
fun EvoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val extraColors = if (darkTheme) DarkExtraColors else LightExtraColors
    val formatColors = if (darkTheme) DarkFormatColors else LightFormatColors

    CompositionLocalProvider(
        LocalEvoraColors provides extraColors,
        LocalEvoraFormatColors provides formatColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = EvoraTypography,
            shapes = EvoraMaterialShapes,
            content = content,
        )
    }
}
