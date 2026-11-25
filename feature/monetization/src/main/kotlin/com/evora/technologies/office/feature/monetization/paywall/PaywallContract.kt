package com.evora.technologies.office.feature.monetization.paywall

import com.evora.technologies.office.core.model.ProStatus
import com.evora.technologies.office.core.model.SubscriptionPlan
import com.evora.technologies.office.core.mvi.UiEffect
import com.evora.technologies.office.core.mvi.UiIntent
import com.evora.technologies.office.core.mvi.UiState

data class PaywallState(
    val selectedPlan: SubscriptionPlan = SubscriptionPlan.YEARLY,
    val proStatus: ProStatus = ProStatus(),
    val isProcessing: Boolean = false,
    val statusMessage: String? = null,
) : UiState

sealed interface PaywallIntent : UiIntent {
    data class SelectPlan(val plan: SubscriptionPlan) : PaywallIntent
    data object StartTrialClicked : PaywallIntent
    data object RestorePurchasesClicked : PaywallIntent
    data object GrantPreviewRewardPassClicked : PaywallIntent
    data object SkipClicked : PaywallIntent
}

sealed interface PaywallEffect : UiEffect {
    data object ClosePaywall : PaywallEffect
    data class ShowMessage(val message: String) : PaywallEffect
}
