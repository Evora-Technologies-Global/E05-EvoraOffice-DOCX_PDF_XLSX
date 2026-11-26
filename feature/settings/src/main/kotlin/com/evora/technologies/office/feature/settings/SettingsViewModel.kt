package com.evora.technologies.office.feature.settings

import androidx.lifecycle.viewModelScope
import com.evora.technologies.office.core.domain.port.CloudDrivePort
import com.evora.technologies.office.core.domain.port.PreferencesRepository
import com.evora.technologies.office.core.domain.port.ProEntitlementPort
import com.evora.technologies.office.core.model.AppLanguage
import com.evora.technologies.office.core.model.AppThemeMode
import com.evora.technologies.office.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val proEntitlementPort: ProEntitlementPort,
    private val cloudDrivePort: CloudDrivePort,
) : MviViewModel<SettingsState, SettingsIntent, SettingsEffect>(SettingsState()) {

    init {
        preferencesRepository.themeMode
            .onEach { mode -> setState { copy(themeMode = mode) } }
            .launchIn(viewModelScope)

        preferencesRepository.selectedLocaleTag
            .onEach { tag ->
                if (tag != null) {
                    setState { copy(selectedLanguage = AppLanguage.fromTag(tag)) }
                }
            }
            .launchIn(viewModelScope)

        preferencesRepository.autosaveIntervalSec
            .onEach { sec -> setState { copy(autosaveIntervalSec = sec) } }
            .launchIn(viewModelScope)

        proEntitlementPort.proStatus
            .onEach { status -> setState { copy(proStatus = status) } }
            .launchIn(viewModelScope)

        cloudDrivePort.observeConnectedAccounts()
            .onEach { accounts -> setState { copy(connectedCloudCount = accounts.size) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.OpenThemeDialog -> setState { copy(showThemeDialog = true) }
            is SettingsIntent.SelectThemeMode -> saveTheme(intent.mode)
            SettingsIntent.OpenLanguageDialog -> setState { copy(showLanguageDialog = true) }
            is SettingsIntent.SelectLanguage -> saveLanguage(intent.language)
            SettingsIntent.OpenAutosaveDialog -> setState { copy(showAutosaveDialog = true) }
            is SettingsIntent.SelectAutosaveInterval -> saveAutosave(intent.seconds)
            SettingsIntent.OpenDefaultHandlerGuide -> setState { copy(showDefaultHandlerDialog = true) }
            SettingsIntent.LaunchSystemAppSettings -> {
                setState { copy(showDefaultHandlerDialog = false) }
                sendEffect(SettingsEffect.OpenSystemAppDetails)
            }
            SettingsIntent.ManageProClicked -> sendEffect(SettingsEffect.NavigateToPaywall)
            SettingsIntent.RestorePurchasesClicked -> restorePurchases()
            SettingsIntent.ConnectedCloudsClicked -> sendEffect(SettingsEffect.NavigateToCloudDrives)
            SettingsIntent.OpenUmpPrivacy -> setState { copy(showUmpPrivacyDialog = true) }
            SettingsIntent.OpenLicenses -> setState { copy(showLicensesSheet = true) }
            SettingsIntent.DismissDialogs -> setState {
                copy(
                    showThemeDialog = false,
                    showLanguageDialog = false,
                    showAutosaveDialog = false,
                    showDefaultHandlerDialog = false,
                    showUmpPrivacyDialog = false,
                    showLicensesSheet = false,
                )
            }
            SettingsIntent.BackClicked -> sendEffect(SettingsEffect.NavigateBack)
        }
    }

    private fun saveTheme(mode: AppThemeMode) {
        setState { copy(showThemeDialog = false) }
        launchSafely {
            preferencesRepository.setThemeMode(mode)
        }
    }

    private fun saveLanguage(language: AppLanguage) {
        setState { copy(showLanguageDialog = false, selectedLanguage = language) }
        launchSafely {
            preferencesRepository.setSelectedLocaleTag(language.tag)
            sendEffect(SettingsEffect.ApplyLocale(language.tag))
        }
    }

    private fun saveAutosave(seconds: Int) {
        setState { copy(showAutosaveDialog = false) }
        launchSafely {
            preferencesRepository.setAutosaveIntervalSec(seconds)
            sendEffect(SettingsEffect.ShowMessage("Auto-save interval set to ${seconds}s"))
        }
    }

    private fun restorePurchases() {
        launchSafely(
            onError = { err -> sendEffect(SettingsEffect.ShowMessage(err.message ?: "Restore failed")) },
        ) {
            val restored = proEntitlementPort.restorePurchases()
            if (restored) {
                sendEffect(SettingsEffect.ShowMessage("Evora Pro purchases restored"))
            } else {
                sendEffect(
                    SettingsEffect.ShowMessage(
                        "No prior Google Play purchases found for this account.",
                    ),
                )
            }
        }
    }
}
