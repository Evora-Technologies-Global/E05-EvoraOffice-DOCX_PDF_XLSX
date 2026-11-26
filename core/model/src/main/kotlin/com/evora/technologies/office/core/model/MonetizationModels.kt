package com.evora.technologies.office.core.model

/**
 * Plaintext Firebase Remote Config schema (03_PRD.md §6).
 */
data class RemoteConfigSnapshot(
    val onboardingShowLanguage: Boolean = true,
    val onboardingShowPaywall: Boolean = true,
    val adsInterstitialCooldownSec: Long = 60L,
    val adsAppOpenCooldownSec: Long = 90L,
    val adsNativeListInterval: Int = 6,
    val adsDisableEditorResume: Boolean = true,
    val paywallDefaultSku: String = SubscriptionPlan.YEARLY.skuId,
    val toolRewardUnlockHours: Int = 24,
    val lokAutosaveIntervalSec: Int = 15,
)

data class ConsentState(
    val canRequestAds: Boolean = false,
    val privacyOptionsRequired: Boolean = false,
    val isInitialized: Boolean = false,
)

sealed interface RewardedAdOutcome {
    data class Earned(val hours: Int) : RewardedAdOutcome
    data object Dismissed : RewardedAdOutcome
    data class Unavailable(val reason: String) : RewardedAdOutcome
}
