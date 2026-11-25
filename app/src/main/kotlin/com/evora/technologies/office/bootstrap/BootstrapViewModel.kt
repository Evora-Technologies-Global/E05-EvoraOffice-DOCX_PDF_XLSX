package com.evora.technologies.office.bootstrap

import androidx.lifecycle.viewModelScope
import com.evora.technologies.office.core.domain.port.OfficeEnginePort
import com.evora.technologies.office.core.domain.port.ProEntitlementPort
import com.evora.technologies.office.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class BootstrapViewModel @Inject constructor(
    proEntitlementPort: ProEntitlementPort,
    private val officeEnginePort: OfficeEnginePort,
) : MviViewModel<BootstrapState, BootstrapIntent, BootstrapEffect>(BootstrapState()) {

    init {
        proEntitlementPort.proStatus
            .onEach { status ->
                setState { copy(isProUnlocked = status.isUnlocked) }
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: BootstrapIntent) {
        when (intent) {
            BootstrapIntent.VerifyEnvironmentClicked -> verifyEnvironment()
        }
    }

    private fun verifyEnvironment() {
        if (currentState.isChecking) return
        launchSafely(
            onError = { err ->
                setState { copy(isChecking = false, statusMessage = err.message) }
            },
        ) {
            setState { copy(isChecking = true, statusMessage = null) }
            val hasOfficeNative = officeEnginePort.isNativeEngineAvailable()
            setState {
                copy(
                    isChecking = false,
                    isOfficeNativeAvailable = hasOfficeNative,
                    statusMessage = "Hilt KSP DI verified",
                )
            }
            sendEffect(BootstrapEffect.ShowStatus("Hilt KSP DI verified"))
        }
    }
}
