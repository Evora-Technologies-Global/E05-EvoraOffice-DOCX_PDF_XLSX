package com.evora.technologies.office.engine.monetization

import android.content.Context
import android.os.Bundle
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.LogLevel
import com.evora.technologies.office.core.domain.port.AnalyticsPort
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

data class AdImpressionRecord(
    val adPlatform: String,
    val adFormat: String,
    val adUnitName: String,
    val currency: String,
    val value: Double,
    val valueMicros: Long,
)

interface TelemetrySink {
    fun initAdjustIfConfigured()
    fun dispatchFirebaseEvent(eventName: String, params: Map<String, Any>)
    fun dispatchAdjustAdRevenue(record: AdImpressionRecord)
}

@Singleton
class FirebaseAndAdjustSink @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val runtimeConfig: MonetizationRuntimeConfig,
) : TelemetrySink {

    private val adjustInitialized = AtomicBoolean(false)

    override fun initAdjustIfConfigured() {
        val token = runtimeConfig.adjustAppToken.trim()
        if (token.isEmpty()) return
        if (!adjustInitialized.compareAndSet(false, true)) return

        runCatching {
            val env = if (runtimeConfig.isDebugBuild) {
                AdjustConfig.ENVIRONMENT_SANDBOX
            } else {
                AdjustConfig.ENVIRONMENT_PRODUCTION
            }
            val config = AdjustConfig(context, token, env).apply {
                setLogLevel(if (runtimeConfig.isDebugBuild) LogLevel.WARN else LogLevel.SUPPRESS)
            }
            Adjust.initSdk(config)
        }.onFailure {
            adjustInitialized.set(false)
        }
    }

    override fun dispatchFirebaseEvent(eventName: String, params: Map<String, Any>) {
        if (runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false).not()) {
            return
        }
        runCatching {
            val bundle = Bundle().apply {
                params.forEach { (k, v) ->
                    when (v) {
                        is String -> putString(k, v)
                        is Int -> putInt(k, v)
                        is Long -> putLong(k, v)
                        is Double -> putDouble(k, v)
                        is Boolean -> putInt(k, if (v) 1 else 0)
                        else -> putString(k, v.toString())
                    }
                }
            }
            FirebaseAnalytics.getInstance(context).logEvent(eventName, bundle)
        }
    }

    override fun dispatchAdjustAdRevenue(record: AdImpressionRecord) {
        initAdjustIfConfigured()
        if (!adjustInitialized.get()) return
        runCatching {
            val adRevenue = AdjustAdRevenue(ADJUST_ADMOB_SOURCE).apply {
                setRevenue(record.value, record.currency)
                setAdRevenueNetwork(record.adPlatform)
                setAdRevenueUnit(record.adUnitName)
            }
            Adjust.trackAdRevenue(adRevenue)
        }
    }

    companion object {
        private const val ADJUST_ADMOB_SOURCE = "admob_sdk"
    }
}

/**
 * Implements 03_PRD.md §10 event taxonomy across Firebase Analytics and Adjust ILAR (`ad_impression`).
 */
@Singleton
class FirebaseAndAdjustTelemetry @Inject constructor(
    private val sink: TelemetrySink,
) : AnalyticsPort {

    init {
        sink.initAdjustIfConfigured()
    }

    override fun logAdImpression(
        adPlatform: String,
        adFormat: String,
        adUnitName: String,
        currency: String,
        value: Double,
        valueMicros: Long,
    ) {
        sink.dispatchFirebaseEvent(
            eventName = "ad_impression",
            params = mapOf(
                "ad_platform" to adPlatform,
                "ad_format" to adFormat,
                "ad_unit_name" to adUnitName,
                "currency" to currency,
                "value" to value,
            ),
        )
        sink.dispatchAdjustAdRevenue(
            AdImpressionRecord(
                adPlatform = adPlatform,
                adFormat = adFormat,
                adUnitName = adUnitName,
                currency = currency,
                value = value,
                valueMicros = valueMicros,
            ),
        )
    }

    override fun logOnboardingStep(stepName: String, action: String) {
        sink.dispatchFirebaseEvent(
            "onboarding_step",
            mapOf("step_name" to stepName, "action" to action),
        )
    }

    override fun logStoragePermissionResult(grantType: String) {
        sink.dispatchFirebaseEvent(
            "storage_permission_result",
            mapOf("grant_type" to grantType),
        )
    }

    override fun logDocumentOpen(format: String, source: String) {
        sink.dispatchFirebaseEvent(
            "document_open",
            mapOf("format" to format, "source" to source),
        )
    }

    override fun logEditorSessionEnd(format: String, editDurationSec: Long, saveStatus: String) {
        sink.dispatchFirebaseEvent(
            "editor_session_end",
            mapOf(
                "format" to format,
                "edit_duration_sec" to editDurationSec,
                "save_status" to saveStatus,
            ),
        )
    }

    override fun logPdfToolComplete(toolType: String, durationMs: Long, success: Boolean) {
        sink.dispatchFirebaseEvent(
            "pdf_tool_complete",
            mapOf(
                "tool_type" to toolType,
                "duration_ms" to durationMs,
                "success" to success,
            ),
        )
    }

    override fun logScanOcrComplete(mode: String, pageCount: Int, script: String) {
        sink.dispatchFirebaseEvent(
            "scan_ocr_complete",
            mapOf(
                "mode" to mode,
                "page_count" to pageCount,
                "script" to script,
            ),
        )
    }

    override fun logPaywallImpression(triggerSource: String) {
        sink.dispatchFirebaseEvent(
            "paywall_impression",
            mapOf("trigger_source" to triggerSource),
        )
    }

    override fun logPurchaseSuccess(skuId: String, priceUsdMicros: Long) {
        sink.dispatchFirebaseEvent(
            "purchase_success",
            mapOf(
                "sku_id" to skuId,
                "price_usd_micros" to priceUsdMicros,
            ),
        )
    }
}
