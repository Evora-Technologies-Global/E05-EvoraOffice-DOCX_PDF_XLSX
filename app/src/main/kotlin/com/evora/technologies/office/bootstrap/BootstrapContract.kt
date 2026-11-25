package com.evora.technologies.office.bootstrap

import com.evora.technologies.office.core.mvi.UiEffect
import com.evora.technologies.office.core.mvi.UiIntent
import com.evora.technologies.office.core.mvi.UiState

data class BootstrapState(
    val isProUnlocked: Boolean = false,
    val isOfficeNativeAvailable: Boolean = false,
    val isChecking: Boolean = false,
    val statusMessage: String? = null,
) : UiState

sealed interface BootstrapIntent : UiIntent {
    data object VerifyEnvironmentClicked : BootstrapIntent
}

sealed interface BootstrapEffect : UiEffect {
    data class ShowStatus(val message: String) : BootstrapEffect
}
