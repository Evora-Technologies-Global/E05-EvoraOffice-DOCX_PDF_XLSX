package com.evora.technologies.office.engine.monetization

import android.content.Context
import com.evora.technologies.office.core.data.di.IoDispatcher
import com.evora.technologies.office.core.domain.port.RemoteConfigPort
import com.evora.technologies.office.core.model.RemoteConfigSnapshot
import com.evora.technologies.office.core.model.SubscriptionPlan
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Plaintext Firebase Remote Config adapter mapping exact 03_PRD.md §6 parameters
 * with deterministic local fallback when google-services.json or network is absent.
 */
@Singleton
class FirebaseRemoteConfigManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val runtimeConfig: MonetizationRuntimeConfig,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RemoteConfigPort {

    private val _config = MutableStateFlow(RemoteConfigSnapshot())
    override val config: StateFlow<RemoteConfigSnapshot> = _config.asStateFlow()

    override suspend fun fetchAndActivate(timeoutMs: Long): RemoteConfigSnapshot = withContext(ioDispatcher) {
        if (runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false).not()) {
            return@withContext _config.value
        }

        try {
            val rc = FirebaseRemoteConfig.getInstance()
            val settings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(if (runtimeConfig.isDebugBuild) 0L else PROD_FETCH_INTERVAL_SEC)
                .setFetchTimeoutInSeconds((timeoutMs / 1_000L).coerceIn(1L, 30L))
                .build()
            rc.setConfigSettingsAsync(settings)
            rc.setDefaultsAsync(DEFAULTS_MAP)

            withTimeoutOrNull(timeoutMs.coerceAtLeast(300L)) {
                suspendCancellableCoroutine { cont ->
                    rc.fetchAndActivate().addOnCompleteListener {
                        if (cont.isActive) cont.resume(Unit)
                    }
                }
            }

            val snapshot = RemoteConfigSnapshot(
                onboardingShowLanguage = rc.getBoolean(KEY_ONBOARDING_SHOW_LANGUAGE),
                onboardingShowPaywall = rc.getBoolean(KEY_ONBOARDING_SHOW_PAYWALL),
                adsInterstitialCooldownSec = rc.getLong(KEY_ADS_INTERSTITIAL_COOLDOWN_SEC),
                adsAppOpenCooldownSec = rc.getLong(KEY_ADS_APP_OPEN_COOLDOWN_SEC),
                adsNativeListInterval = rc.getLong(KEY_ADS_NATIVE_LIST_INTERVAL).toInt(),
                adsDisableEditorResume = rc.getBoolean(KEY_ADS_DISABLE_EDITOR_RESUME),
                paywallDefaultSku = rc.getString(KEY_PAYWALL_DEFAULT_SKU)
                    .ifBlank { SubscriptionPlan.YEARLY.skuId },
                toolRewardUnlockHours = rc.getLong(KEY_TOOL_REWARD_UNLOCK_HOURS)
                    .toInt()
                    .coerceIn(1, 168),
                lokAutosaveIntervalSec = rc.getLong(KEY_LOK_AUTOSAVE_INTERVAL_SEC)
                    .toInt()
                    .coerceIn(5, 300),
            )
            _config.value = snapshot
            snapshot
        } catch (c: CancellationException) {
            throw c
        } catch (_: Exception) {
            _config.value
        }
    }

    companion object {
        private const val PROD_FETCH_INTERVAL_SEC = 3600L

        const val KEY_ONBOARDING_SHOW_LANGUAGE = "onboarding_show_language"
        const val KEY_ONBOARDING_SHOW_PAYWALL = "onboarding_show_paywall"
        const val KEY_ADS_INTERSTITIAL_COOLDOWN_SEC = "ads_interstitial_cooldown_sec"
        const val KEY_ADS_APP_OPEN_COOLDOWN_SEC = "ads_app_open_cooldown_sec"
        const val KEY_ADS_NATIVE_LIST_INTERVAL = "ads_native_list_interval"
        const val KEY_ADS_DISABLE_EDITOR_RESUME = "ads_disable_editor_resume"
        const val KEY_PAYWALL_DEFAULT_SKU = "paywall_default_sku"
        const val KEY_TOOL_REWARD_UNLOCK_HOURS = "tool_reward_unlock_hours"
        const val KEY_LOK_AUTOSAVE_INTERVAL_SEC = "lok_autosave_interval_sec"

        private val DEFAULTS_MAP: Map<String, Any> = mapOf(
            KEY_ONBOARDING_SHOW_LANGUAGE to true,
            KEY_ONBOARDING_SHOW_PAYWALL to true,
            KEY_ADS_INTERSTITIAL_COOLDOWN_SEC to 60L,
            KEY_ADS_APP_OPEN_COOLDOWN_SEC to 90L,
            KEY_ADS_NATIVE_LIST_INTERVAL to 6L,
            KEY_ADS_DISABLE_EDITOR_RESUME to true,
            KEY_PAYWALL_DEFAULT_SKU to SubscriptionPlan.YEARLY.skuId,
            KEY_TOOL_REWARD_UNLOCK_HOURS to 24L,
            KEY_LOK_AUTOSAVE_INTERVAL_SEC to 15L,
        )
    }
}
