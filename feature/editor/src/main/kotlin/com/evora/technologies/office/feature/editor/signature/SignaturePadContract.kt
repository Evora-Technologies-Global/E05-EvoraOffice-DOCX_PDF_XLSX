package com.evora.technologies.office.feature.editor.signature

import com.evora.technologies.office.core.model.SignatureItem
import com.evora.technologies.office.core.mvi.UiEffect
import com.evora.technologies.office.core.mvi.UiIntent
import com.evora.technologies.office.core.mvi.UiState

data class SignaturePadState(
    val strokes: List<List<Pair<Float, Float>>> = emptyList(),
    val selectedColorArgb: Long = 0xFF0F172AL,
    val strokeWidthPx: Float = 8f,
    val canvasWidthPx: Int = 800,
    val canvasHeightPx: Int = 360,
    val savedSignatures: List<SignatureItem> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) : UiState

sealed interface SignaturePadIntent : UiIntent {
    data class CanvasSized(val widthPx: Int, val heightPx: Int) : SignaturePadIntent
    data class AddStroke(val points: List<Pair<Float, Float>>) : SignaturePadIntent
    data class SelectColor(val colorArgb: Long) : SignaturePadIntent
    data class SetStrokeWidth(val widthPx: Float) : SignaturePadIntent
    data object UndoStroke : SignaturePadIntent
    data object ClearCanvas : SignaturePadIntent
    data object SaveSignatureClicked : SignaturePadIntent
    data class DeleteSavedSignature(val id: String) : SignaturePadIntent
    data class SelectSavedSignature(val item: SignatureItem) : SignaturePadIntent
    data object BackClicked : SignaturePadIntent
}

sealed interface SignaturePadEffect : UiEffect {
    data class SignatureReady(val item: SignatureItem) : SignaturePadEffect
    data object NavigateBack : SignaturePadEffect
    data class ShowMessage(val message: String) : SignaturePadEffect
}
