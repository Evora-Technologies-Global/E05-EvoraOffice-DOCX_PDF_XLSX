package com.evora.technologies.office.feature.settings

import com.evora.technologies.office.core.model.AppLanguage
import com.evora.technologies.office.core.model.AppThemeMode
import com.evora.technologies.office.core.model.ProStatus
import com.evora.technologies.office.core.mvi.UiEffect
import com.evora.technologies.office.core.mvi.UiIntent
import com.evora.technologies.office.core.mvi.UiState

data class SettingsState(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val selectedLanguage: AppLanguage = AppLanguage.EN,
    val autosaveIntervalSec: Int = 15,
    val proStatus: ProStatus = ProStatus(),
    val connectedCloudCount: Int = 0,
    val isRestoringPurchases: Boolean = false,
    val showThemeDialog: Boolean = false,
    val showLanguageDialog: Boolean = false,
    val showAutosaveDialog: Boolean = false,
    val showDefaultHandlerDialog: Boolean = false,
    val showUmpPrivacyDialog: Boolean = false,
    val showLicensesSheet: Boolean = false,
) : UiState

sealed interface SettingsIntent : UiIntent {
    data object OpenThemeDialog : SettingsIntent
    data class SelectThemeMode(val mode: AppThemeMode) : SettingsIntent
    data object OpenLanguageDialog : SettingsIntent
    data class SelectLanguage(val language: AppLanguage) : SettingsIntent
    data object OpenAutosaveDialog : SettingsIntent
    data class SelectAutosaveInterval(val seconds: Int) : SettingsIntent
    data object OpenDefaultHandlerGuide : SettingsIntent
    data object LaunchSystemAppSettings : SettingsIntent
    data object ManageProClicked : SettingsIntent
    data object RestorePurchasesClicked : SettingsIntent
    data object ConnectedCloudsClicked : SettingsIntent
    data object OpenUmpPrivacy : SettingsIntent
    data object OpenLicenses : SettingsIntent
    data object DismissDialogs : SettingsIntent
    data object BackClicked : SettingsIntent
}

sealed interface SettingsEffect : UiEffect {
    data object NavigateBack : SettingsEffect
    data class ApplyLocale(val languageTag: String) : SettingsEffect
    data object NavigateToPaywall : SettingsEffect
    data object NavigateToCloudDrives : SettingsEffect
    data object OpenSystemAppDetails : SettingsEffect
    data class ShowMessage(val message: String) : SettingsEffect
}
