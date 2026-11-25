package com.evora.technologies.office.feature.onboarding.intro

import com.evora.technologies.office.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingIntroViewModel @Inject constructor() :
    MviViewModel<OnboardingIntroState, OnboardingIntroIntent, OnboardingIntroEffect>(
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
                    sendEffect(OnboardingIntroEffect.NavigateNext)
                } else {
                    val next = currentState.currentPage + 1
                    setState { copy(currentPage = next) }
                    sendEffect(OnboardingIntroEffect.ScrollToPage(next))
                }
            }
            OnboardingIntroIntent.SkipClicked -> sendEffect(OnboardingIntroEffect.NavigateNext)
        }
    }
}
