package com.evora.technologies.office.engine.monetization

data class MonetizationRuntimeConfig(
    val isDebugBuild: Boolean,
    val admobAppId: String,
    val interstitialUnitId: String,
    val appOpenUnitId: String,
    val rewardedUnitId: String,
    val nativeUnitId: String,
    val adjustAppToken: String,
)
