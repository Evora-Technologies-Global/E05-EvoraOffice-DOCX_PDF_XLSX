package com.evora.technologies.office.feature.monetization.paywall

import androidx.lifecycle.viewModelScope
import com.evora.technologies.office.core.domain.port.AnalyticsPort
import com.evora.technologies.office.core.domain.port.ProEntitlementPort
import com.evora.technologies.office.core.domain.port.RemoteConfigPort
import com.evora.technologies.office.core.model.PurchaseResult
import com.evora.technologies.office.core.model.SubscriptionPlan
import com.evora.technologies.office.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val proEntitlementPort: ProEntitlementPort,
    private val remoteConfigPort: RemoteConfigPort,
    private val analyticsPort: AnalyticsPort,
) : MviViewModel<PaywallState, PaywallIntent, PaywallEffect>(
    PaywallState(
        selectedPlan = SubscriptionPlan.entries.firstOrNull {
            it.skuId == remoteConfigPort.config.value.paywallDefaultSku
        } ?: SubscriptionPlan.YEARLY,
    ),
) {

    init {
        proEntitlementPort.proStatus
            .onEach { status -> setState { copy(proStatus = status) } }
            .catch { /* retain default ProStatus */ }
            .launchIn(viewModelScope)

        proEntitlementPort.observeBillingOffers()
            .onEach { offers ->
                if (offers.isNotEmpty()) {
                    setState { copy(offers = offers) }
                }
            }
            .catch { /* keep default fallback offers */ }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: PaywallIntent) {
        when (intent) {
            PaywallIntent.RefreshOffers -> refreshOffers()
            is PaywallIntent.SelectPlan -> setState { copy(selectedPlan = intent.plan, statusMessage = null) }
            PaywallIntent.StartTrialClicked -> startPlayBillingPurchase()
            PaywallIntent.RestorePurchasesClicked -> restorePurchases()
            PaywallIntent.SkipClicked -> sendEffect(PaywallEffect.ClosePaywall)
        }
    }

    private fun refreshOffers() {
        analyticsPort.logPaywallImpression("paywall")
        launchSafely {
            proEntitlementPort.refreshCatalog()
        }
    }

    private fun startPlayBillingPurchase() {
        if (currentState.isProcessing) return
        if (currentState.proStatus.isPro) {
            sendEffect(PaywallEffect.ShowMessage("Evora Pro is already active"))
            sendEffect(PaywallEffect.ClosePaywall)
            return
        }

        val chosenPlan = currentState.selectedPlan
        launchSafely(
            onError = { err ->
                val msg = err.message ?: "Unable to start Google Play purchase"
                setState { copy(isProcessing = false, statusMessage = msg) }
                sendEffect(PaywallEffect.ShowMessage(msg))
            },
        ) {
            setState { copy(isProcessing = true, statusMessage = null) }
            when (val result = proEntitlementPort.purchasePlan(chosenPlan)) {
                is PurchaseResult.Success -> {
                    analyticsPort.logPurchaseSuccess(result.skuId, 0L)
                    setState { copy(isProcessing = false, statusMessage = null) }
                    sendEffect(PaywallEffect.ShowMessage("Evora Pro activated (${chosenPlan.title})"))
                    sendEffect(PaywallEffect.ClosePaywall)
                }
                PurchaseResult.Pending -> {
                    val msg = "Purchase pending Google Play confirmation"
                    setState { copy(isProcessing = false, statusMessage = msg) }
                    sendEffect(PaywallEffect.ShowMessage(msg))
                }
                PurchaseResult.Cancelled -> {
                    setState { copy(isProcessing = false) }
                }
                is PurchaseResult.BillingUnavailable -> {
                    setState { copy(isProcessing = false, statusMessage = result.message) }
                    sendEffect(PaywallEffect.ShowMessage(result.message))
                }
                is PurchaseResult.Error -> {
                    setState { copy(isProcessing = false, statusMessage = result.message) }
                    sendEffect(PaywallEffect.ShowMessage(result.message))
                }
            }
        }
    }

    private fun restorePurchases() {
        if (currentState.isProcessing) return
        launchSafely(
            onError = { err ->
                val msg = err.message ?: "Unable to restore purchases"
                setState { copy(isProcessing = false, statusMessage = msg) }
                sendEffect(PaywallEffect.ShowMessage(msg))
            },
        ) {
            setState { copy(isProcessing = true, statusMessage = null) }
            val restored = proEntitlementPort.restorePurchases()
            val msg = if (restored) {
                "Evora Pro purchases restored"
            } else {
                "No active Google Play purchases found for this account"
            }
            setState { copy(isProcessing = false, statusMessage = if (restored) null else msg) }
            sendEffect(PaywallEffect.ShowMessage(msg))
            if (restored) {
                sendEffect(PaywallEffect.ClosePaywall)
            }
        }
    }
}
