package com.evora.technologies.office.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Decouples AdMob NativeAd rendering in `:engine:monetization` from `:feature:*` UI modules.
 */
fun interface InlineNativeAdSlotRenderer {
    @Composable
    fun Render(modifier: Modifier)
}

val LocalInlineNativeAdRenderer = staticCompositionLocalOf<InlineNativeAdSlotRenderer?> { null }
