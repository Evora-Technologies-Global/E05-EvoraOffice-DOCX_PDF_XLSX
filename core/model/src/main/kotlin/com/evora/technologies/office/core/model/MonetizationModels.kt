package com.evora.technologies.office.core.model

/**
 * Plaintext Firebase Remote Config schema (03_PRD.md §6).
 */
data class RemoteConfigSnapshot(
    val onboardingShowLanguage: Boolean = true,
    val onboardingShowPaywall: Boolean = true,
    val paywallDefaultSku: String = SubscriptionPlan.YEARLY.skuId,
    val lokAutosaveIntervalSec: Int = 15,
)
