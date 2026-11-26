package com.evora.technologies.office.engine.monetization

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.office.core.designsystem.component.InlineNativeAdSlotRenderer
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Renders a Style B inline AdMob mediated [NativeAd] card inside [InlineNativeAdSlotRenderer]
 * without leaking Play Ads dependencies into `:feature:library`.
 */
@Singleton
class AdMobInlineNativeRenderer @Inject constructor(
    private val adManager: MediatedAdPlacementManager,
) : InlineNativeAdSlotRenderer {

    @Composable
    override fun Render(modifier: Modifier) {
        val nativeAd by adManager.currentNativeAd.collectAsStateWithLifecycle()
        val ad = nativeAd ?: return
        val onSurface = MaterialTheme.colorScheme.onSurface.toArgb()
        val onVariant = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
        val primary = MaterialTheme.colorScheme.primary.toArgb()
        val onPrimary = MaterialTheme.colorScheme.onPrimary.toArgb()

        Surface(
            shape = EvoraRadii.md,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(EvoraSpacing.md)) {
                Text(
                    text = "Sponsored",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = EvoraSpacing.xs),
                )
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { ctx ->
                        val adView = NativeAdView(ctx)
                        val row = LinearLayout(ctx).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                            )
                        }
                        val icon = ImageView(ctx).apply {
                            val sizePx = (44 * ctx.resources.displayMetrics.density).toInt()
                            layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                                marginEnd = (12 * ctx.resources.displayMetrics.density).toInt()
                            }
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                        val textCol = LinearLayout(ctx).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        }
                        val headline = TextView(ctx).apply {
                            textSize = 14f
                            setTextColor(onSurface)
                            maxLines = 1
                        }
                        val body = TextView(ctx).apply {
                            textSize = 12f
                            setTextColor(onVariant)
                            maxLines = 2
                        }
                        textCol.addView(headline)
                        textCol.addView(body)

                        val cta = Button(ctx).apply {
                            textSize = 12f
                            setTextColor(onPrimary)
                            setBackgroundColor(primary)
                            val padH = (12 * ctx.resources.displayMetrics.density).toInt()
                            val padV = (6 * ctx.resources.displayMetrics.density).toInt()
                            setPadding(padH, padV, padH, padV)
                            minimumHeight = (48 * ctx.resources.displayMetrics.density).toInt()
                        }

                        row.addView(icon)
                        row.addView(textCol)
                        row.addView(cta)
                        adView.addView(row)
                        adView.iconView = icon
                        adView.headlineView = headline
                        adView.bodyView = body
                        adView.callToActionView = cta
                        adView
                    },
                    update = { adView ->
                        bindNativeAd(ad, adView)
                    },
                )
            }
        }
    }

    private fun bindNativeAd(ad: NativeAd, adView: NativeAdView) {
        (adView.headlineView as? TextView)?.text = ad.headline.orEmpty()
        val bodyView = adView.bodyView as? TextView
        if (ad.body.isNullOrBlank()) {
            bodyView?.visibility = View.GONE
        } else {
            bodyView?.visibility = View.VISIBLE
            bodyView?.text = ad.body
        }
        val iconView = adView.iconView as? ImageView
        if (ad.icon?.drawable != null) {
            iconView?.visibility = View.VISIBLE
            iconView?.setImageDrawable(ad.icon?.drawable)
        } else {
            iconView?.visibility = View.GONE
        }
        val ctaView = adView.callToActionView as? Button
        if (ad.callToAction.isNullOrBlank()) {
            ctaView?.visibility = View.GONE
        } else {
            ctaView?.visibility = View.VISIBLE
            ctaView?.text = ad.callToAction
        }
        adView.setNativeAd(ad)
    }
}
