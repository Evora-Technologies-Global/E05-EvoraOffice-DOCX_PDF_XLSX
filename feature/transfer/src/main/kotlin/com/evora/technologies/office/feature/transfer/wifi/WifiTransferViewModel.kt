package com.evora.technologies.office.feature.transfer.wifi

import androidx.lifecycle.viewModelScope
import com.evora.technologies.office.core.domain.port.WifiTransferPort
import com.evora.technologies.office.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class WifiTransferViewModel @Inject constructor(
    private val wifiTransferPort: WifiTransferPort,
) : MviViewModel<WifiTransferUiState, WifiTransferIntent, WifiTransferEffect>(
    WifiTransferUiState(serverState = wifiTransferPort.serverState.value),
) {

    private val configMutex = Mutex()

    init {
        wifiTransferPort.serverState
            .onEach { srv ->
                setState {
                    copy(
                        serverState = srv,
                        pinLockEnabled = srv.config.pinCode.isNotBlank() || pinLockEnabled,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: WifiTransferIntent) {
        when (intent) {
            WifiTransferIntent.ToggleServerClicked -> toggleServer()
            is WifiTransferIntent.ToggleFtp -> updateConfig { it.copy(ftpEnabled = intent.enabled) }
            is WifiTransferIntent.ToggleReadOnly -> updateConfig { it.copy(readOnly = intent.readOnly) }
            is WifiTransferIntent.TogglePinLock -> {
                val sanitizedPin = currentState.pinInput.filter { it.isDigit() }.ifBlank { "4829" }
                setState { copy(pinLockEnabled = intent.enabled, pinInput = sanitizedPin) }
                updateConfig { cfg -> cfg.copy(pinCode = if (intent.enabled) sanitizedPin else "") }
            }
            is WifiTransferIntent.UpdatePinCode -> {
                val digits = intent.pin.filter { it.isDigit() }.take(8)
                setState { copy(pinInput = digits) }
                if (currentState.pinLockEnabled && digits.length >= 4) {
                    updateConfig { it.copy(pinCode = digits) }
                }
            }
            is WifiTransferIntent.CopyUrlClicked -> {
                if (intent.url.isNotBlank()) {
                    sendEffect(WifiTransferEffect.CopyToClipboard(intent.url, "Server URL"))
                }
            }
            WifiTransferIntent.BackClicked -> sendEffect(WifiTransferEffect.NavigateBack)
        }
    }

    private fun toggleServer() {
        if (currentState.isToggling) return
        val currentlyRunning = currentState.serverState.isRunning
        if (!currentlyRunning && currentState.pinLockEnabled && currentState.pinInput.length < 4) {
            sendEffect(WifiTransferEffect.ShowMessage("PIN Lock requires at least 4 digits"))
            return
        }
        launchSafely(
            onError = { err ->
                setState { copy(isToggling = false) }
                sendEffect(WifiTransferEffect.ShowMessage(err.message ?: "Transfer toggle failed"))
            },
        ) {
            setState { copy(isToggling = true) }
            if (currentlyRunning) {
                wifiTransferPort.stopServer().getOrThrow()
                sendEffect(WifiTransferEffect.ShowMessage("Wi-Fi Transfer stopped"))
            } else {
                val baseConfig = currentState.serverState.config.copy(
                    pinCode = if (currentState.pinLockEnabled) currentState.pinInput else "",
                )
                wifiTransferPort.startServer(baseConfig).getOrThrow()
                sendEffect(WifiTransferEffect.ShowMessage("Wi-Fi Transfer running"))
            }
            setState { copy(isToggling = false) }
        }
    }

    private fun updateConfig(
        transform: (com.evora.technologies.office.core.model.WifiTransferConfig) -> com.evora.technologies.office.core.model.WifiTransferConfig,
    ) {
        launchSafely {
            configMutex.withLock {
                val latest = wifiTransferPort.serverState.value.config
                val next = transform(latest)
                wifiTransferPort.updateConfig(next)
                setState { copy(serverState = serverState.copy(config = next)) }
            }
        }
    }
}
