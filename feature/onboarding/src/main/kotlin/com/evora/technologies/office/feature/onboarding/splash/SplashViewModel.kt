package com.evora.technologies.office.feature.onboarding.splash

import com.evora.technologies.office.core.domain.port.DocumentRepository
import com.evora.technologies.office.core.domain.port.PreferencesRepository
import com.evora.technologies.office.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val documentRepository: DocumentRepository,
) : MviViewModel<SplashState, SplashIntent, SplashEffect>(SplashState()) {

    private var hasRouted = false

    override fun onIntent(intent: SplashIntent) {
        when (intent) {
            is SplashIntent.StartRouting -> routeColdStart(intent)
        }
    }

    private fun routeColdStart(intent: SplashIntent.StartRouting) {
        if (hasRouted && intent.externalUri == null) return
        hasRouted = true

        launchSafely(
            onError = { err ->
                setState { copy(isRouting = false, errorMessage = err.message) }
                sendEffect(SplashEffect.ShowError(err.message ?: "Unable to open document"))
                sendEffect(SplashEffect.NavigateToHome)
            },
        ) {
            if (!intent.externalUri.isNullOrBlank()) {
                val imported = documentRepository.importExternalUri(
                    uriString = intent.externalUri,
                    displayNameHint = intent.displayNameHint,
                    mimeTypeHint = intent.mimeTypeHint,
                )
                setState { copy(isRouting = false) }
                imported.fold(
                    onSuccess = { payload ->
                        sendEffect(SplashEffect.NavigateToExternalDocument(payload))
                    },
                    onFailure = { err ->
                        sendEffect(SplashEffect.ShowError(err.message ?: "Unable to open external file"))
                        sendEffect(SplashEffect.NavigateToHome)
                    },
                )
                return@launchSafely
            }

            val onboardingDone = preferencesRepository.onboardingCompleted.first()
            setState { copy(isRouting = false) }
            if (onboardingDone) {
                sendEffect(SplashEffect.NavigateToHome)
            } else {
                sendEffect(SplashEffect.NavigateToLanguageSelect)
            }
        }
    }
}
