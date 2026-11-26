package com.evora.technologies.office.feature.onboarding.intro

import com.evora.technologies.office.core.domain.port.AnalyticsPort
import com.evora.technologies.office.core.domain.port.RemoteConfigPort
import com.evora.technologies.office.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingIntroViewModel @Inject constructor(
    private val remoteConfigPort: RemoteConfigPort,
    private val analyticsPort: AnalyticsPort,
) : MviViewModel<OnboardingIntroState, OnboardingIntroIntent, OnboardingIntroEffect>(
    OnboardingIntroState(),
) {

    override fun onIntent(intent: OnboardingIntroIntent) {
        when (intent) {
            is OnboardingIntroIntent.PageChanged -> {
                val bounded = intent.pageIndex.coerceIn(0, currentState.slides.lastIndex)
                setState { copy(currentPage = bounded) }
            }
            OnboardingIntroIntent.NextClicked -> {
                if (currentState.isLastSlide) {
                    analyticsPort.logOnboardingStep("intro", "complete")
                    routeAfterIntro()
                } else {
                    val next = currentState.currentPage + 1
                    setState { copy(currentPage = next) }
                    sendEffect(OnboardingIntroEffect.ScrollToPage(next))
                }
            }
            OnboardingIntroIntent.SkipClicked -> {
                analyticsPort.logOnboardingStep("intro", "skip")
                routeAfterIntro()
            }
        }
    }

    private fun routeAfterIntro() {
        val showPaywall = remoteConfigPort.config.value.onboardingShowPaywall
        if (showPaywall) {
            sendEffect(OnboardingIntroEffect.NavigateNext)
        } else {
            sendEffect(OnboardingIntroEffect.NavigateSkipPaywall)
        }
    }
}
