package com.evora.technologies.office.core.domain.policy

import com.evora.technologies.office.core.model.RemoteConfigSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdCooldownPolicyTest {

    @Test
    fun `paid Pro suppresses interstitial, app open, and inline native ads globally`() {
        val cfg = RemoteConfigSnapshot()
        assertFalse(
            AdCooldownPolicy.canShowInterstitial(
                shouldHideAds = true,
                canRequestAds = true,
                isProtectedSurfaceActive = false,
                config = cfg,
                nowMs = 200_000L,
                lastFullscreenShownMs = 0L,
            ),
        )
        assertFalse(
            AdCooldownPolicy.canShowAppOpen(
                shouldHideAds = true,
                canRequestAds = true,
                isProtectedSurfaceActive = false,
                openedViaExternalIntent = false,
                config = cfg,
                nowMs = 200_000L,
                lastFullscreenShownMs = 0L,
            ),
        )
        assertNull(
            AdCooldownPolicy.inlineNativeInsertionIndex(
                shouldHideAds = true,
                canRequestAds = true,
                itemCount = 20,
                config = cfg,
            ),
        )
    }

    @Test
    fun `interstitial and app open enforce minimum 60s and 90s cooldowns and protect editors and external intents`() {
        val cfg = RemoteConfigSnapshot(
            adsInterstitialCooldownSec = 30L, // Server trying lower must still clamp >= 60s
            adsAppOpenCooldownSec = 45L,      // Must clamp >= 90s
            adsDisableEditorResume = true,
        )
        assertFalse(
            AdCooldownPolicy.canShowInterstitial(
                shouldHideAds = false,
                canRequestAds = true,
                isProtectedSurfaceActive = false,
                config = cfg,
                nowMs = 150_000L,
                lastFullscreenShownMs = 100_000L, // 50s < 60s
            ),
        )
        assertTrue(
            AdCooldownPolicy.canShowInterstitial(
                shouldHideAds = false,
                canRequestAds = true,
                isProtectedSurfaceActive = false,
                config = cfg,
                nowMs = 160_000L,
                lastFullscreenShownMs = 100_000L, // 60s >= 60s
            ),
        )
        assertFalse(
            AdCooldownPolicy.canShowInterstitial(
                shouldHideAds = false,
                canRequestAds = true,
                isProtectedSurfaceActive = true, // Editor/scanner active
                config = cfg,
                nowMs = 500_000L,
                lastFullscreenShownMs = 0L,
            ),
        )
        assertFalse(
            AdCooldownPolicy.canShowAppOpen(
                shouldHideAds = false,
                canRequestAds = true,
                isProtectedSurfaceActive = false,
                openedViaExternalIntent = true, // ACTION_VIEW must never show startup ad
                config = cfg,
                nowMs = 500_000L,
                lastFullscreenShownMs = 0L,
            ),
        )
        assertFalse(
            AdCooldownPolicy.canShowAppOpen(
                shouldHideAds = false,
                canRequestAds = true,
                isProtectedSurfaceActive = false,
                openedViaExternalIntent = false,
                config = cfg,
                nowMs = 189_000L,
                lastFullscreenShownMs = 100_000L, // 89s < 90s
            ),
        )
        assertTrue(
            AdCooldownPolicy.canShowAppOpen(
                shouldHideAds = false,
                canRequestAds = true,
                isProtectedSurfaceActive = false,
                openedViaExternalIntent = false,
                config = cfg,
                nowMs = 190_000L,
                lastFullscreenShownMs = 100_000L, // 90s >= 90s
            ),
        )
    }

    @Test
    fun `inline native placement requires index at least 6 and respects zero disable flag`() {
        assertNull(
            AdCooldownPolicy.inlineNativeInsertionIndex(
                shouldHideAds = false,
                canRequestAds = true,
                itemCount = 5,
                config = RemoteConfigSnapshot(adsNativeListInterval = 6),
            ),
        )
        assertEquals(
            6,
            AdCooldownPolicy.inlineNativeInsertionIndex(
                shouldHideAds = false,
                canRequestAds = true,
                itemCount = 10,
                config = RemoteConfigSnapshot(adsNativeListInterval = 3), // Clamped >= 6
            ),
        )
        assertNull(
            AdCooldownPolicy.inlineNativeInsertionIndex(
                shouldHideAds = false,
                canRequestAds = true,
                itemCount = 20,
                config = RemoteConfigSnapshot(adsNativeListInterval = 0),
            ),
        )
    }
}
