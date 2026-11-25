package com.evora.technologies.office.feature.onboarding.splash

import com.evora.technologies.office.core.model.ExternalIntentPayload
import com.evora.technologies.office.core.mvi.UiEffect
import com.evora.technologies.office.core.mvi.UiIntent
import com.evora.technologies.office.core.mvi.UiState

data class SplashState(
    val isRouting: Boolean = true,
    val errorMessage: String? = null,
) : UiState

sealed interface SplashIntent : UiIntent {
    data class StartRouting(
        val externalUri: String? = null,
        val displayNameHint: String? = null,
        val mimeTypeHint: String? = null,
    ) : SplashIntent
}

sealed interface SplashEffect : UiEffect {
    data object NavigateToLanguageSelect : SplashEffect
    data object NavigateToHome : SplashEffect
    data class NavigateToExternalDocument(val payload: ExternalIntentPayload) : SplashEffect
    data class ShowError(val message: String) : SplashEffect
}
