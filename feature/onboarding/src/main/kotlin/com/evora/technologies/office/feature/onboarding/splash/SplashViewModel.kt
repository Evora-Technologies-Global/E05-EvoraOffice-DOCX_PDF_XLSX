package com.evora.technologies.office.feature.onboarding.splash

import com.evora.technologies.office.core.domain.port.AdPlacementHook
import com.evora.technologies.office.core.domain.port.AnalyticsPort
import com.evora.technologies.office.core.domain.port.ConsentPort
import com.evora.technologies.office.core.domain.port.DocumentRepository
import com.evora.technologies.office.core.domain.port.PreferencesRepository
import com.evora.technologies.office.core.domain.port.RemoteConfigPort
import com.evora.technologies.office.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val documentRepository: DocumentRepository,
    private val remoteConfigPort: RemoteConfigPort,
    private val consentPort: ConsentPort,
    private val adPlacementHook: AdPlacementHook,
    private val analyticsPort: AnalyticsPort,
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
                // US-004: External ACTION_VIEW bypasses startup UMP/ads and opens reader immediately
                adPlacementHook.markExternalIntentLaunch(true)
                val imported = documentRepository.importExternalUri(
                    uriString = intent.externalUri,
                    displayNameHint = intent.displayNameHint,
                    mimeTypeHint = intent.mimeTypeHint,
                )
                setState { copy(isRouting = false) }
                imported.fold(
                    onSuccess = { payload ->
                        analyticsPort.logDocumentOpen(payload.format.name, "external_intent")
                        sendEffect(SplashEffect.NavigateToExternalDocument(payload))
                    },
                    onFailure = { err ->
                        sendEffect(SplashEffect.ShowError(err.message ?: "Unable to open external file"))
                        sendEffect(SplashEffect.NavigateToHome)
                    },
                )
                return@launchSafely
            }

            adPlacementHook.markExternalIntentLaunch(false)
            // Check persisted onboarding state immediately so returning users launch straight to Home (< 1.2s)
            val onboardingDone = preferencesRepository.onboardingCompleted.first()
            if (onboardingDone) {
                setState { copy(isRouting = false) }
                analyticsPort.logOnboardingStep("splash", "complete")
                sendEffect(SplashEffect.NavigateToHome)
                return@launchSafely
            }

            // First launch: bounded fast Remote Config read; UMP consent runs concurrently via MainActivity
            val remote = remoteConfigPort.fetchAndActivate(timeoutMs = 350L)
            analyticsPort.logOnboardingStep("splash", "complete")
            setState { copy(isRouting = false) }
            if (remote.onboardingShowLanguage) {
                sendEffect(SplashEffect.NavigateToLanguageSelect)
            } else {
                sendEffect(SplashEffect.NavigateToIntro)
            }
        }
    }
}
