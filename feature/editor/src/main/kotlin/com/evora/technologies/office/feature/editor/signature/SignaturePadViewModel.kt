package com.evora.technologies.office.feature.editor.signature

import androidx.lifecycle.viewModelScope
import com.evora.technologies.office.core.domain.port.SignatureRepository
import com.evora.technologies.office.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class SignaturePadViewModel @Inject constructor(
    private val signatureRepository: SignatureRepository,
) : MviViewModel<SignaturePadState, SignaturePadIntent, SignaturePadEffect>(
    SignaturePadState(),
) {
    init {
        signatureRepository.observeSignatures()
            .onEach { list -> setState { copy(savedSignatures = list) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: SignaturePadIntent) {
        when (intent) {
            is SignaturePadIntent.CanvasSized -> setState {
                copy(
                    canvasWidthPx = intent.widthPx.coerceAtLeast(240),
                    canvasHeightPx = intent.heightPx.coerceAtLeast(160),
                )
            }
            is SignaturePadIntent.AddStroke -> if (intent.points.isNotEmpty()) {
                setState { copy(strokes = strokes + listOf(intent.points), errorMessage = null) }
            }
            is SignaturePadIntent.SelectColor -> setState { copy(selectedColorArgb = intent.colorArgb) }
            is SignaturePadIntent.SetStrokeWidth -> setState { copy(strokeWidthPx = intent.widthPx.coerceIn(2f, 20f)) }
            SignaturePadIntent.UndoStroke -> if (currentState.strokes.isNotEmpty()) {
                setState { copy(strokes = strokes.dropLast(1)) }
            }
            SignaturePadIntent.ClearCanvas -> setState { copy(strokes = emptyList(), errorMessage = null) }
            SignaturePadIntent.SaveSignatureClicked -> saveSignature()
            is SignaturePadIntent.DeleteSavedSignature -> {
                launchSafely { signatureRepository.deleteSignature(intent.id) }
            }
            is SignaturePadIntent.SelectSavedSignature -> {
                sendEffect(SignaturePadEffect.SignatureReady(intent.item))
            }
            SignaturePadIntent.BackClicked -> sendEffect(SignaturePadEffect.NavigateBack)
        }
    }

    private fun saveSignature() {
        if (currentState.isSaving) return
        if (currentState.strokes.isEmpty()) {
            setState { copy(errorMessage = "Draw your signature above before saving") }
            return
        }
        launchSafely(
            onError = { err ->
                setState { copy(isSaving = false, errorMessage = err.message ?: "Unable to save signature") }
            },
        ) {
            setState { copy(isSaving = true, errorMessage = null) }
            val saved = signatureRepository.saveSignaturePng(
                width = currentState.canvasWidthPx,
                height = currentState.canvasHeightPx,
                strokes = currentState.strokes,
                strokeColorArgb = currentState.selectedColorArgb,
                strokeWidthPx = currentState.strokeWidthPx,
            ).getOrThrow()
            setState { copy(isSaving = false, strokes = emptyList()) }
            sendEffect(SignaturePadEffect.SignatureReady(saved))
        }
    }
}
