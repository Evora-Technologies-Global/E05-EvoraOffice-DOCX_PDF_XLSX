package com.evora.technologies.office.feature.onboarding.permission

import com.evora.technologies.office.core.domain.port.DocumentRepository
import com.evora.technologies.office.core.domain.port.PreferencesRepository
import com.evora.technologies.office.core.model.StorageAccessMode
import com.evora.technologies.office.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StoragePermissionViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val documentRepository: DocumentRepository,
) : MviViewModel<StoragePermissionState, StoragePermissionIntent, StoragePermissionEffect>(
    StoragePermissionState(),
) {

    override fun onIntent(intent: StoragePermissionIntent) {
        when (intent) {
            StoragePermissionIntent.AllowAllFilesClicked -> {
                sendEffect(StoragePermissionEffect.RequestSystemAllFilesPermission)
            }
            is StoragePermissionIntent.PermissionSystemResult -> handleAllFilesResult(intent.granted)
            StoragePermissionIntent.UseSafPickerClicked -> {
                sendEffect(StoragePermissionEffect.LaunchSafDocumentPicker)
            }
            is StoragePermissionIntent.SafDocumentPicked -> handleSafPicked(intent.uriString)
        }
    }

    private fun handleAllFilesResult(granted: Boolean) {
        if (currentState.isFinalizing) return
        launchSafely(
            onError = { setState { copy(isFinalizing = false) } },
        ) {
            setState {
                copy(
                    isFinalizing = true,
                    accessMode = if (granted) StorageAccessMode.ALL_FILES else StorageAccessMode.SAF_ONLY,
                )
            }
            preferencesRepository.setOnboardingCompleted(true)
            if (granted) {
                documentRepository.refreshMediaStoreIndex()
            } else {
                sendEffect(
                    StoragePermissionEffect.ShowMessage(
                        "You can open files anytime via the Folder Picker (SAF).",
                    ),
                )
            }
            setState { copy(isFinalizing = false) }
            sendEffect(StoragePermissionEffect.NavigateToHome)
        }
    }

    private fun handleSafPicked(uriString: String?) {
        if (currentState.isFinalizing) return
        launchSafely(
            onError = { setState { copy(isFinalizing = false) } },
        ) {
            setState { copy(isFinalizing = true, accessMode = StorageAccessMode.SAF_ONLY) }
            if (!uriString.isNullOrBlank()) {
                documentRepository.registerSafUri(uriString)
            }
            preferencesRepository.setOnboardingCompleted(true)
            setState { copy(isFinalizing = false) }
            sendEffect(StoragePermissionEffect.NavigateToHome)
        }
    }
}
