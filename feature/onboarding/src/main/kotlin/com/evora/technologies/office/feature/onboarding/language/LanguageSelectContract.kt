package com.evora.technologies.office.feature.onboarding.language

import com.evora.technologies.office.core.model.AppLanguage
import com.evora.technologies.office.core.mvi.UiEffect
import com.evora.technologies.office.core.mvi.UiIntent
import com.evora.technologies.office.core.mvi.UiState

data class LanguageSelectState(
    val languages: List<AppLanguage> = AppLanguage.entries,
    val selectedLanguage: AppLanguage = AppLanguage.EN,
    val isSaving: Boolean = false,
) : UiState

sealed interface LanguageSelectIntent : UiIntent {
    data class SelectLanguage(val language: AppLanguage) : LanguageSelectIntent
    data object ContinueClicked : LanguageSelectIntent
}

sealed interface LanguageSelectEffect : UiEffect {
    data class ApplyLocale(val languageTag: String) : LanguageSelectEffect
    data object NavigateToIntro : LanguageSelectEffect
}
