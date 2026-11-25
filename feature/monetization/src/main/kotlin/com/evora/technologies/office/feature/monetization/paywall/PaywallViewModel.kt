package com.evora.technologies.office.feature.monetization.paywall

import androidx.lifecycle.viewModelScope
import com.evora.technologies.office.core.domain.port.ProEntitlementPort
import com.evora.technologies.office.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val proEntitlementPort: ProEntitlementPort,
) : MviViewModel<PaywallState, PaywallIntent, PaywallEffect>(PaywallState()) {

    init {
        proEntitlementPort.proStatus
            .onEach { status -> setState { copy(proStatus = status) } }
            .catch { /* retain default ProStatus */ }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: PaywallIntent) {
        when (intent) {
            is PaywallIntent.SelectPlan -> setState { copy(selectedPlan = intent.plan) }
            PaywallIntent.StartTrialClicked -> activateLocalOrBillingPreview()
            PaywallIntent.RestorePurchasesClicked -> restorePurchases()
            PaywallIntent.GrantPreviewRewardPassClicked -> grantRewardPass()
            PaywallIntent.SkipClicked -> sendEffect(PaywallEffect.ClosePaywall)
        }
    }

    private fun activateLocalOrBillingPreview() {
        if (currentState.isProcessing) return
        launchSafely(
            onError = { err ->
                setState { copy(isProcessing = false, statusMessage = err.message) }
            },
        ) {
            setState { copy(isProcessing = true, statusMessage = null) }
            // CD-07: Live Play Billing 8.0.0 activates in Sprint 4 after F-01..F-09 Done.
            proEntitlementPort.setQaProOverride(true)
            setState { copy(isProcessing = false) }
            sendEffect(PaywallEffect.ShowMessage("Local Pro preview enabled (${currentState.selectedPlan.title})"))
            sendEffect(PaywallEffect.ClosePaywall)
        }
    }

    private fun restorePurchases() {
        if (currentState.isProcessing) return
        launchSafely(
            onError = { err ->
                setState { copy(isProcessing = false, statusMessage = err.message) }
            },
        ) {
            setState { copy(isProcessing = true) }
            val restored = proEntitlementPort.restorePurchases()
            setState { copy(isProcessing = false) }
            sendEffect(
                PaywallEffect.ShowMessage(
                    if (restored) "Pro purchases restored" else "No active Play purchases found yet",
                ),
            )
        }
    }

    private fun grantRewardPass() {
        launchSafely(
            onError = { err ->
                setState { copy(isProcessing = false, statusMessage = err.message) }
            },
        ) {
            proEntitlementPort.grantRewardedPass(hours = 24)
            sendEffect(PaywallEffect.ShowMessage("24-hour Pro tool pass activated"))
            sendEffect(PaywallEffect.ClosePaywall)
        }
    }
}
