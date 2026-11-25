package com.evora.technologies.office.core.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Collects one-shot [UiEffect] flows safely during the Lifecycle STARTED state.
 * Uses rememberUpdatedState to ensure latest lambdas are invoked without restarting the collector.
 */
@Composable
fun <E : UiEffect> CollectEffects(
    effects: Flow<E>,
    onEffect: suspend (E) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val handler by rememberUpdatedState(onEffect)
    LaunchedEffect(effects, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            effects.collect { handler(it) }
        }
    }
}
