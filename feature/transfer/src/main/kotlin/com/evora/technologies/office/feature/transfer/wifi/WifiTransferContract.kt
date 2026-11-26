package com.evora.technologies.office.feature.transfer.wifi

import com.evora.technologies.office.core.model.WifiTransferServerState
import com.evora.technologies.office.core.mvi.UiEffect
import com.evora.technologies.office.core.mvi.UiIntent
import com.evora.technologies.office.core.mvi.UiState

data class WifiTransferUiState(
    val serverState: WifiTransferServerState = WifiTransferServerState(),
    val pinLockEnabled: Boolean = false,
    val pinInput: String = "4829",
    val isToggling: Boolean = false,
) : UiState

sealed interface WifiTransferIntent : UiIntent {
    data object ToggleServerClicked : WifiTransferIntent
    data class ToggleFtp(val enabled: Boolean) : WifiTransferIntent
    data class ToggleReadOnly(val readOnly: Boolean) : WifiTransferIntent
    data class TogglePinLock(val enabled: Boolean) : WifiTransferIntent
    data class UpdatePinCode(val pin: String) : WifiTransferIntent
    data class CopyUrlClicked(val url: String) : WifiTransferIntent
    data object BackClicked : WifiTransferIntent
}

sealed interface WifiTransferEffect : UiEffect {
    data object NavigateBack : WifiTransferEffect
    data class CopyToClipboard(val value: String, val label: String) : WifiTransferEffect
    data class ShowMessage(val message: String) : WifiTransferEffect
}
