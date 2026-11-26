package com.evora.technologies.office.feature.monetization.paywall

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.office.core.mvi.CollectEffects

@Composable
fun PaywallRoute(
    onClose: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaywallViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onIntent(PaywallIntent.RefreshOffers)
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            PaywallEffect.ClosePaywall -> onClose()
            is PaywallEffect.ShowMessage -> onShowMessage(effect.message)
        }
    }

    PaywallScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}
