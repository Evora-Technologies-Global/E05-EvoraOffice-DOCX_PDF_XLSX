package com.evora.technologies.office.feature.onboarding.permission

import com.evora.technologies.office.core.model.StorageAccessMode
import com.evora.technologies.office.core.mvi.UiEffect
import com.evora.technologies.office.core.mvi.UiIntent
import com.evora.technologies.office.core.mvi.UiState

data class StoragePermissionState(
    val accessMode: StorageAccessMode = StorageAccessMode.NOT_GRANTED,
    val isFinalizing: Boolean = false,
) : UiState

sealed interface StoragePermissionIntent : UiIntent {
    data object AllowAllFilesClicked : StoragePermissionIntent
    data class PermissionSystemResult(val granted: Boolean) : StoragePermissionIntent
    data object UseSafPickerClicked : StoragePermissionIntent
    data class SafDocumentPicked(val uriString: String?) : StoragePermissionIntent
}

sealed interface StoragePermissionEffect : UiEffect {
    data object RequestSystemAllFilesPermission : StoragePermissionEffect
    data object LaunchSafDocumentPicker : StoragePermissionEffect
    data object NavigateToHome : StoragePermissionEffect
    data class ShowMessage(val message: String) : StoragePermissionEffect
}
