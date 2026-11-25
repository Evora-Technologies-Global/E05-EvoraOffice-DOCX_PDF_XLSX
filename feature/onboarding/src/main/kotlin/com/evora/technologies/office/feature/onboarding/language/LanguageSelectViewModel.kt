package com.evora.technologies.office.feature.onboarding.language

import androidx.lifecycle.viewModelScope
import com.evora.technologies.office.core.domain.port.PreferencesRepository
import com.evora.technologies.office.core.model.AppLanguage
import com.evora.technologies.office.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class LanguageSelectViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : MviViewModel<LanguageSelectState, LanguageSelectIntent, LanguageSelectEffect>(
    LanguageSelectState(),
) {
    init {
        preferencesRepository.selectedLocaleTag
            .onEach { savedTag ->
                if (savedTag != null) {
                    setState { copy(selectedLanguage = AppLanguage.fromTag(savedTag)) }
                }
            }
            .catch { /* keep default English selection */ }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: LanguageSelectIntent) {
        when (intent) {
            is LanguageSelectIntent.SelectLanguage -> setState { copy(selectedLanguage = intent.language) }
            LanguageSelectIntent.ContinueClicked -> persistAndContinue()
        }
    }

    private fun persistAndContinue() {
        if (currentState.isSaving) return
        launchSafely(
            onError = { setState { copy(isSaving = false) } },
        ) {
            setState { copy(isSaving = true) }
            val tag = currentState.selectedLanguage.tag
            preferencesRepository.setSelectedLocaleTag(tag)
            setState { copy(isSaving = false) }
            sendEffect(LanguageSelectEffect.ApplyLocale(tag))
            sendEffect(LanguageSelectEffect.NavigateToIntro)
        }
    }
}
