package com.evora.technologies.office.feature.editor.pdf

import com.evora.technologies.office.core.domain.port.PdfPageRaster
import com.evora.technologies.office.core.model.PdfAnnotationDraft
import com.evora.technologies.office.core.model.PdfAnnotationType
import com.evora.technologies.office.core.model.PdfSignaturePlacement
import com.evora.technologies.office.core.model.Pkcs7CertificateInfo
import com.evora.technologies.office.core.model.ProStatus
import com.evora.technologies.office.core.model.SignatureItem
import com.evora.technologies.office.core.mvi.UiEffect
import com.evora.technologies.office.core.mvi.UiIntent
import com.evora.technologies.office.core.mvi.UiState

enum class PdfEditorTab(val label: String) {
    ANNOTATE("Annotate"),
    SIGNATURE("Signature"),
    FILE("File"),
    PAGE("Page"),
}

data class PdfEditorState(
    val uriString: String = "",
    val displayName: String = "Document.pdf",
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val pageRaster: PdfPageRaster? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val activeTab: PdfEditorTab = PdfEditorTab.ANNOTATE,
    val activeTool: PdfAnnotationType = PdfAnnotationType.INK,
    val activeColorArgb: Long = 0xFFFFD54FL,
    val inkStrokeWidthDp: Float = 3.5f,
    val annotations: List<PdfAnnotationDraft> = emptyList(),
    val signatures: List<PdfSignaturePlacement> = emptyList(),
    val selectedSignaturePlacementId: String? = null,
    val savedSignatures: List<SignatureItem> = emptyList(),
    val pkcs7Certs: List<Pkcs7CertificateInfo> = emptyList(),
    val showCertModal: Boolean = false,
    val showStickyNoteDialog: Boolean = false,
    val pendingNoteX: Float = 0.5f,
    val pendingNoteY: Float = 0.5f,
    val showSetPasswordDialog: Boolean = false,
    val showRemovePasswordDialog: Boolean = false,
    val passwordRequired: Boolean = false,
    val passwordError: String? = null,
    val proStatus: ProStatus = ProStatus(),
    val showRewardGateTitle: String? = null,
    val errorMessage: String? = null,
) : UiState

sealed interface PdfEditorIntent : UiIntent {
    data class OpenPdf(val uriString: String, val displayName: String) : PdfEditorIntent
    data class SubmitOpenPassword(val password: String) : PdfEditorIntent
    data object DismissOpenPassword : PdfEditorIntent
    data class SelectTab(val tab: PdfEditorTab) : PdfEditorIntent
    data class SelectTool(val type: PdfAnnotationType) : PdfEditorIntent
    data class SelectColor(val colorArgb: Long) : PdfEditorIntent
    data class SetStrokeWidth(val widthDp: Float) : PdfEditorIntent
    data class AddInkStroke(val points: List<Pair<Float, Float>>) : PdfEditorIntent
    data class AddMarkupBox(val left: Float, val top: Float, val right: Float, val bottom: Float) : PdfEditorIntent
    data class OpenStickyNoteAt(val xFraction: Float, val yFraction: Float) : PdfEditorIntent
    data class SaveStickyNote(val text: String, val author: String) : PdfEditorIntent
    data object DismissStickyNoteDialog : PdfEditorIntent
    data object UndoLastAnnotation : PdfEditorIntent
    data object ClearCurrentPageAnnotations : PdfEditorIntent
    data class PlaceSavedSignature(val item: SignatureItem) : PdfEditorIntent
    data class SelectPlacedSignature(val placementId: String?) : PdfEditorIntent
    data class MoveSelectedSignature(val deltaX: Float, val deltaY: Float) : PdfEditorIntent
    data class ResizeSelectedSignature(val widthFraction: Float) : PdfEditorIntent
    data class RemovePlacedSignature(val placementId: String) : PdfEditorIntent
    data class GoToPage(val pageIndex: Int) : PdfEditorIntent
    data class RequestRenderPage(val targetWidthPx: Int) : PdfEditorIntent
    data object SavePdfClicked : PdfEditorIntent
    data object InspectPkcs7Clicked : PdfEditorIntent
    data object DismissCertModal : PdfEditorIntent
    data class SignWithP12(val p12Uri: String, val p12Password: String, val reason: String) : PdfEditorIntent
    data object OpenSetPasswordDialog : PdfEditorIntent
    data class ConfirmSetPassword(val userPass: String, val aes256: Boolean) : PdfEditorIntent
    data object OpenRemovePasswordDialog : PdfEditorIntent
    data class ConfirmRemovePassword(val currentPass: String) : PdfEditorIntent
    data object DismissSecurityDialogs : PdfEditorIntent
    data object PrintClicked : PdfEditorIntent
    data object OpenSignaturePadClicked : PdfEditorIntent
    data object DismissRewardGate : PdfEditorIntent
    data object GrantRewardPassAndProceed : PdfEditorIntent
    data object UpgradeProClicked : PdfEditorIntent
    data object BackClicked : PdfEditorIntent
}

sealed interface PdfEditorEffect : UiEffect {
    data object NavigateBack : PdfEditorEffect
    data object NavigateToSignaturePad : PdfEditorEffect
    data object NavigateToPaywall : PdfEditorEffect
    data class LaunchPrint(val uriString: String, val displayName: String) : PdfEditorEffect
    data class ShowMessage(val message: String) : PdfEditorEffect
}
