package com.evora.technologies.office.core.domain.policy

import com.evora.technologies.office.core.model.RemoteConfigSnapshot

/**
 * Pure Kotlin enforcement of E05 anti-churn ad placement & frequency caps (T-024 / US-110):
 * - Zero ads when paid Pro subscription/lifetime is active (`shouldHideAds`).
 * - Zero ads before UMP consent allows ad requests (`canRequestAds`).
 * - Zero ads during external `ACTION_VIEW` launches or on protected Editor/Scanner `onResume`.
 * - Interstitial cooldown >= 60s; App-Open cooldown >= 90s; shared fullscreen guard prevents back-to-back ads.
 * - Inline native list ad strictly at index >= 6 (0 disables).
 */
object AdCooldownPolicy {

    const val MIN_INTERSTITIAL_COOLDOWN_SEC = 60L
    const val MIN_APP_OPEN_COOLDOWN_SEC = 90L
    const val MIN_NATIVE_LIST_INTERVAL = 6

    fun canShowInterstitial(
        shouldHideAds: Boolean,
        canRequestAds: Boolean,
        isProtectedSurfaceActive: Boolean,
        config: RemoteConfigSnapshot,
        nowMs: Long,
        lastFullscreenShownMs: Long,
    ): Boolean {
        if (shouldHideAds || !canRequestAds) return false
        if (config.adsDisableEditorResume && isProtectedSurfaceActive) return false
        if (lastFullscreenShownMs <= 0L) return true
        val effectiveCooldownMs = config.adsInterstitialCooldownSec.coerceAtLeast(MIN_INTERSTITIAL_COOLDOWN_SEC) * 1_000L
        return (nowMs - lastFullscreenShownMs) >= effectiveCooldownMs
    }

    fun canShowAppOpen(
        shouldHideAds: Boolean,
        canRequestAds: Boolean,
        isProtectedSurfaceActive: Boolean,
        openedViaExternalIntent: Boolean,
        config: RemoteConfigSnapshot,
        nowMs: Long,
        lastFullscreenShownMs: Long,
    ): Boolean {
        if (shouldHideAds || !canRequestAds) return false
        if (openedViaExternalIntent) return false
        if (config.adsDisableEditorResume && isProtectedSurfaceActive) return false
        if (lastFullscreenShownMs <= 0L) return true
        val effectiveCooldownMs = config.adsAppOpenCooldownSec.coerceAtLeast(MIN_APP_OPEN_COOLDOWN_SEC) * 1_000L
        return (nowMs - lastFullscreenShownMs) >= effectiveCooldownMs
    }

    /**
     * Returns the item index (>= 6) after which an inline native card should appear,
     * or `null` if inline native ads are disabled or suppressed.
     */
    fun inlineNativeInsertionIndex(
        shouldHideAds: Boolean,
        canRequestAds: Boolean,
        itemCount: Int,
        config: RemoteConfigSnapshot,
    ): Int? {
        if (shouldHideAds || !canRequestAds) return null
        val configured = config.adsNativeListInterval
        if (configured <= 0) return null
        val effectiveIndex = configured.coerceAtLeast(MIN_NATIVE_LIST_INTERVAL)
        return if (itemCount >= effectiveIndex) effectiveIndex else null
    }
}
