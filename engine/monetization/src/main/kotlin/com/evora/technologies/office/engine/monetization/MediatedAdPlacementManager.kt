package com.evora.technologies.office.engine.monetization

import android.content.Context
import com.evora.technologies.office.core.data.di.IoDispatcher
import com.evora.technologies.office.core.data.di.MainDispatcher
import com.evora.technologies.office.core.domain.policy.AdCooldownPolicy
import com.evora.technologies.office.core.domain.port.AdPlacementHook
import com.evora.technologies.office.core.domain.port.AnalyticsPort
import com.evora.technologies.office.core.domain.port.ConsentPort
import com.evora.technologies.office.core.domain.port.ProEntitlementPort
import com.evora.technologies.office.core.domain.port.RemoteConfigPort
import com.evora.technologies.office.core.model.RewardedAdOutcome
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.ResponseInfo
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates AdMob Mediation (Open Bidding + AppLovin / Unity / Mintegral) gated strictly
 * behind Google UMP consent, Pro ad-free status, and [AdCooldownPolicy] anti-churn rules.
 */
@Singleton
class MediatedAdPlacementManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val runtimeConfig: MonetizationRuntimeConfig,
    private val activityTracker: ActiveActivityTracker,
    private val proEntitlementPort: ProEntitlementPort,
    private val consentPort: ConsentPort,
    private val remoteConfigPort: RemoteConfigPort,
    private val analyticsPort: AnalyticsPort,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @param:MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) : AdPlacementHook {

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val mobileAdsInitialized = AtomicBoolean(false)

    @Volatile
    private var interstitialAd: InterstitialAd? = null
    @Volatile
    private var appOpenAd: AppOpenAd? = null
    @Volatile
    private var rewardedAd: RewardedAd? = null
    @Volatile
    private var isLoadingInterstitial = false
    @Volatile
    private var isLoadingAppOpen = false
    @Volatile
    private var isLoadingRewarded = false
    @Volatile
    private var isLoadingNative = false
    @Volatile
    private var isShowingFullscreen = false
    @Volatile
    private var isProtectedSurfaceActive = false
    @Volatile
    private var openedViaExternalIntent = false
    @Volatile
    private var lastFullscreenShownMs = 0L

    private val _currentNativeAd = MutableStateFlow<NativeAd?>(null)
    val currentNativeAd: StateFlow<NativeAd?> = _currentNativeAd.asStateFlow()

    init {
        combine(
            proEntitlementPort.proStatus,
            consentPort.consentState,
            remoteConfigPort.config,
        ) { pro, consent, remote ->
            Triple(pro.shouldHideAds, consent.canRequestAds, remote.adsNativeListInterval)
        }.onEach { (hideAds, canRequest, nativeInterval) ->
            if (hideAds) {
                clearAllLoadedAds()
            } else if (canRequest) {
                ensureMobileAdsInitializedAndPreload(nativeInterval > 0)
            }
        }.launchIn(scope)
    }

    override fun setProtectedSurfaceActive(active: Boolean) {
        isProtectedSurfaceActive = active
    }

    override fun markExternalIntentLaunch(external: Boolean) {
        openedViaExternalIntent = external
    }

    override fun onMilestoneCompleted(screenId: String) {
        val pro = proEntitlementPort.proStatus.value
        val consent = consentPort.consentState.value
        val cfg = remoteConfigPort.config.value
        val now = System.currentTimeMillis()
        if (!AdCooldownPolicy.canShowInterstitial(
                shouldHideAds = pro.shouldHideAds,
                canRequestAds = consent.canRequestAds,
                isProtectedSurfaceActive = isProtectedSurfaceActive,
                config = cfg,
                nowMs = now,
                lastFullscreenShownMs = lastFullscreenShownMs,
            )
        ) {
            return
        }
        val ad = interstitialAd ?: run {
            preloadInterstitial()
            return
        }
        val activity = activityTracker.currentActivity() ?: return
        if (isShowingFullscreen) return

        scope.launch(mainDispatcher) {
            if (isShowingFullscreen) return@launch
            isShowingFullscreen = true
            interstitialAd = null
            lastFullscreenShownMs = System.currentTimeMillis()
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    isShowingFullscreen = false
                    lastFullscreenShownMs = System.currentTimeMillis()
                    preloadInterstitial()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    isShowingFullscreen = false
                    preloadInterstitial()
                }
            }
            ad.show(activity)
        }
    }

    override fun onHomeHubEntered() {
        val pro = proEntitlementPort.proStatus.value
        val consent = consentPort.consentState.value
        val cfg = remoteConfigPort.config.value
        val now = System.currentTimeMillis()
        val wasExternal = openedViaExternalIntent
        if (wasExternal) {
            // Reset one-shot external-launch suppression after home is entered once
            openedViaExternalIntent = false
        }
        if (!AdCooldownPolicy.canShowAppOpen(
                shouldHideAds = pro.shouldHideAds,
                canRequestAds = consent.canRequestAds,
                isProtectedSurfaceActive = isProtectedSurfaceActive,
                openedViaExternalIntent = wasExternal,
                config = cfg,
                nowMs = now,
                lastFullscreenShownMs = lastFullscreenShownMs,
            )
        ) {
            return
        }
        val ad = appOpenAd ?: run {
            preloadAppOpen()
            return
        }
        val activity = activityTracker.currentActivity() ?: return
        if (isShowingFullscreen) return

        scope.launch(mainDispatcher) {
            if (isShowingFullscreen) return@launch
            isShowingFullscreen = true
            appOpenAd = null
            lastFullscreenShownMs = System.currentTimeMillis()
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    isShowingFullscreen = false
                    lastFullscreenShownMs = System.currentTimeMillis()
                    preloadAppOpen()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    isShowingFullscreen = false
                    preloadAppOpen()
                }
            }
            ad.show(activity)
        }
    }

    override suspend fun showRewardedPassAd(): RewardedAdOutcome {
        val unlockHours = remoteConfigPort.config.value.toolRewardUnlockHours.coerceIn(1, 168)
        if (proEntitlementPort.proStatus.value.isPro) {
            return RewardedAdOutcome.Earned(unlockHours)
        }
        if (!consentPort.consentState.value.canRequestAds) {
            return RewardedAdOutcome.Unavailable("Consent required before loading rewarded ads")
        }
        val ad = rewardedAd ?: run {
            preloadRewarded()
            return RewardedAdOutcome.Unavailable(
                "Rewarded video is loading. Try again in a moment or upgrade to Evora Pro.",
            )
        }
        val activity = activityTracker.currentActivity()
            ?: return RewardedAdOutcome.Unavailable("No active window to display rewarded video")
        if (isShowingFullscreen) {
            return RewardedAdOutcome.Unavailable("Another ad is currently active")
        }

        return withContext(mainDispatcher) {
            val outcome = CompletableDeferred<RewardedAdOutcome>()
            var earnedReward = false
            isShowingFullscreen = true
            rewardedAd = null
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    isShowingFullscreen = false
                    lastFullscreenShownMs = System.currentTimeMillis()
                    preloadRewarded()
                    if (!outcome.isCompleted) {
                        outcome.complete(
                            if (earnedReward) {
                                RewardedAdOutcome.Earned(unlockHours)
                            } else {
                                RewardedAdOutcome.Dismissed
                            },
                        )
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    isShowingFullscreen = false
                    preloadRewarded()
                    if (!outcome.isCompleted) {
                        outcome.complete(RewardedAdOutcome.Unavailable(adError.message))
                    }
                }
            }
            ad.show(activity) {
                earnedReward = true
            }
            outcome.await()
        }
    }

    private fun ensureMobileAdsInitializedAndPreload(loadNative: Boolean) {
        if (mobileAdsInitialized.compareAndSet(false, true)) {
            scope.launch(ioDispatcher) {
                runCatching {
                    MobileAds.initialize(context) {
                        preloadInterstitial()
                        preloadAppOpen()
                        preloadRewarded()
                        if (loadNative) preloadNative()
                    }
                }.onFailure {
                    mobileAdsInitialized.set(false)
                }
            }
        } else {
            preloadInterstitial()
            preloadAppOpen()
            preloadRewarded()
            if (loadNative && _currentNativeAd.value == null) preloadNative()
        }
    }

    private fun clearAllLoadedAds() {
        interstitialAd = null
        appOpenAd = null
        rewardedAd = null
        val prevNative = _currentNativeAd.value
        _currentNativeAd.value = null
        if (prevNative != null) {
            scope.launch(mainDispatcher) {
                runCatching { prevNative.destroy() }
            }
        }
    }

    private fun preloadInterstitial() {
        if (isLoadingInterstitial || interstitialAd != null) return
        if (proEntitlementPort.proStatus.value.shouldHideAds || !consentPort.consentState.value.canRequestAds) return
        isLoadingInterstitial = true
        scope.launch(mainDispatcher) {
            runCatching {
                InterstitialAd.load(
                    context,
                    runtimeConfig.interstitialUnitId,
                    AdRequest.Builder().build(),
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            isLoadingInterstitial = false
                            ad.setOnPaidEventListener { adValue ->
                                logPaidImpression("interstitial", runtimeConfig.interstitialUnitId, adValue, ad.responseInfo)
                            }
                            interstitialAd = ad
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            isLoadingInterstitial = false
                            interstitialAd = null
                        }
                    },
                )
            }.onFailure {
                isLoadingInterstitial = false
            }
        }
    }

    private fun preloadAppOpen() {
        if (isLoadingAppOpen || appOpenAd != null) return
        if (proEntitlementPort.proStatus.value.shouldHideAds || !consentPort.consentState.value.canRequestAds) return
        isLoadingAppOpen = true
        scope.launch(mainDispatcher) {
            runCatching {
                AppOpenAd.load(
                    context,
                    runtimeConfig.appOpenUnitId,
                    AdRequest.Builder().build(),
                    object : AppOpenAd.AppOpenAdLoadCallback() {
                        override fun onAdLoaded(ad: AppOpenAd) {
                            isLoadingAppOpen = false
                            ad.setOnPaidEventListener { adValue ->
                                logPaidImpression("app_open", runtimeConfig.appOpenUnitId, adValue, ad.responseInfo)
                            }
                            appOpenAd = ad
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            isLoadingAppOpen = false
                            appOpenAd = null
                        }
                    },
                )
            }.onFailure {
                isLoadingAppOpen = false
            }
        }
    }

    private fun preloadRewarded() {
        if (isLoadingRewarded || rewardedAd != null) return
        if (proEntitlementPort.proStatus.value.isPro || !consentPort.consentState.value.canRequestAds) return
        isLoadingRewarded = true
        scope.launch(mainDispatcher) {
            runCatching {
                RewardedAd.load(
                    context,
                    runtimeConfig.rewardedUnitId,
                    AdRequest.Builder().build(),
                    object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedAd) {
                            isLoadingRewarded = false
                            ad.setOnPaidEventListener { adValue ->
                                logPaidImpression("rewarded", runtimeConfig.rewardedUnitId, adValue, ad.responseInfo)
                            }
                            rewardedAd = ad
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            isLoadingRewarded = false
                            rewardedAd = null
                        }
                    },
                )
            }.onFailure {
                isLoadingRewarded = false
            }
        }
    }

    private fun preloadNative() {
        if (isLoadingNative || _currentNativeAd.value != null) return
        if (proEntitlementPort.proStatus.value.shouldHideAds || !consentPort.consentState.value.canRequestAds) return
        if (remoteConfigPort.config.value.adsNativeListInterval <= 0) return
        isLoadingNative = true
        scope.launch(mainDispatcher) {
            runCatching {
                val loader = AdLoader.Builder(context, runtimeConfig.nativeUnitId)
                    .forNativeAd { nativeAd ->
                        isLoadingNative = false
                        nativeAd.setOnPaidEventListener { adValue ->
                            logPaidImpression("native", runtimeConfig.nativeUnitId, adValue, nativeAd.responseInfo)
                        }
                        if (proEntitlementPort.proStatus.value.shouldHideAds) {
                            nativeAd.destroy()
                            return@forNativeAd
                        }
                        val old = _currentNativeAd.value
                        _currentNativeAd.value = nativeAd
                        old?.destroy()
                    }
                    .withAdListener(
                        object : AdListener() {
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                isLoadingNative = false
                            }
                        },
                    )
                    .build()
                loader.loadAd(AdRequest.Builder().build())
            }.onFailure {
                isLoadingNative = false
            }
        }
    }

    private fun logPaidImpression(
        adFormat: String,
        adUnitName: String,
        adValue: AdValue,
        responseInfo: ResponseInfo?,
    ) {
        val platform = responseInfo?.loadedAdapterResponseInfo?.adSourceName
            ?.takeIf { it.isNotBlank() }
            ?: "admob"
        val valueDollars = adValue.valueMicros / 1_000_000.0
        analyticsPort.logAdImpression(
            adPlatform = platform,
            adFormat = adFormat,
            adUnitName = adUnitName,
            currency = adValue.currencyCode,
            value = valueDollars,
            valueMicros = adValue.valueMicros,
        )
    }
}
