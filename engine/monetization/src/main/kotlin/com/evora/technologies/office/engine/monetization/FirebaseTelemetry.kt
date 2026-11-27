package com.evora.technologies.office.engine.monetization

import android.content.Context
import android.os.Bundle
import com.evora.technologies.office.core.domain.port.AnalyticsPort
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface TelemetrySink {
    fun dispatchFirebaseEvent(eventName: String, params: Map<String, Any>)
}

@Singleton
class FirebaseAnalyticsSink @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : TelemetrySink {

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
}

/**
 * Implements 03_PRD.md §10 product and billing event taxonomy via Firebase Analytics.
 */
@Singleton
class FirebaseTelemetry @Inject constructor(
    private val sink: TelemetrySink,
) : AnalyticsPort {

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
