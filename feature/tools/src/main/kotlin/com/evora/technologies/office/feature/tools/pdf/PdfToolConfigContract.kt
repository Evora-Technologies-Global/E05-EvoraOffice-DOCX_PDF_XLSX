package com.evora.technologies.office.feature.tools.pdf

import com.evora.technologies.office.core.model.CompressQuality
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.PageSizePreset
import com.evora.technologies.office.core.model.PdfToolKind
import com.evora.technologies.office.core.model.PdfToolResult
import com.evora.technologies.office.core.model.Pkcs7CertificateInfo
import com.evora.technologies.office.core.model.ProStatus
import com.evora.technologies.office.core.model.SplitMode
import com.evora.technologies.office.core.mvi.UiEffect
import com.evora.technologies.office.core.mvi.UiIntent
import com.evora.technologies.office.core.mvi.UiState

data class ToolInputFile(
    val uriString: String,
    val displayName: String,
)

data class PdfToolConfigState(
    val toolKind: PdfToolKind = PdfToolKind.MERGE,
    val proStatus: ProStatus = ProStatus(),
    val freeDailyPdfToDocxAvailable: Boolean = true,
    val availableLibraryDocs: List<DocumentItem> = emptyList(),
    val selectedInputs: List<ToolInputFile> = emptyList(),
    val outputBaseName: String = "Evora_Output",
    val sourceTotalPages: Int = 0,
    // Split
    val splitMode: SplitMode = SplitMode.RANGE,
    val splitRangeExpression: String = "1-2",
    val splitEveryN: Int = 2,
    val splitSelectedPages: Set<Int> = emptySet(),
    // Compress
    val compressQuality: CompressQuality = CompressQuality.MEDIUM,
    // Watermark
    val watermarkText: String = "CONFIDENTIAL",
    val watermarkFontSize: Float = 42f,
    val watermarkColorArgb: Long = 0xFF64748BL,
    val watermarkOpacity: Float = 0.30f,
    val watermarkAngle: Float = -45f,
    val watermarkTiled: Boolean = false,
    // Password
    val userPassword: String = "",
    val ownerPassword: String = "",
    val useAes256: Boolean = true,
    val currentPdfPassword: String = "",
    // PKCS#7
    val p12CertUriString: String? = null,
    val p12CertName: String = "",
    val p12Password: String = "",
    val signReason: String = "Approved",
    val signLocation: String = "Mobile",
    val inspectedCerts: List<Pkcs7CertificateInfo> = emptyList(),
    // Converters
    val pageSizePreset: PageSizePreset = PageSizePreset.A4,
    val imageMarginDp: Int = 16,
    val landscape: Boolean = false,
    val plainTextInput: String = "",
    val stitchedLongImage: Boolean = false,
    // Execution
    val showLibraryPicker: Boolean = false,
    val showRewardGateTitle: String? = null,
    val passwordPromptRequired: Boolean = false,
    val passwordPromptError: String? = null,
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val result: PdfToolResult? = null,
    val errorMessage: String? = null,
) : UiState

sealed interface PdfToolConfigIntent : UiIntent {
    data class Initialize(val toolKind: PdfToolKind, val preselectedUri: String?) : PdfToolConfigIntent
    data object OpenLibraryPicker : PdfToolConfigIntent
    data object DismissLibraryPicker : PdfToolConfigIntent
    data class AddLibraryDocument(val doc: DocumentItem) : PdfToolConfigIntent
    data class AddExternalFiles(val files: List<ToolInputFile>) : PdfToolConfigIntent
    data class RemoveInputAt(val index: Int) : PdfToolConfigIntent
    data class MoveInput(val fromIndex: Int, val toIndex: Int) : PdfToolConfigIntent
    data class UpdateOutputName(val name: String) : PdfToolConfigIntent
    // Split
    data class SelectSplitMode(val mode: SplitMode) : PdfToolConfigIntent
    data class UpdateSplitRange(val expr: String) : PdfToolConfigIntent
    data class UpdateSplitEveryN(val n: Int) : PdfToolConfigIntent
    data class ToggleSplitPage(val zeroBasedPage: Int) : PdfToolConfigIntent
    // Compress
    data class SelectCompressQuality(val quality: CompressQuality) : PdfToolConfigIntent
    // Watermark
    data class UpdateWatermarkText(val text: String) : PdfToolConfigIntent
    data class UpdateWatermarkSize(val size: Float) : PdfToolConfigIntent
    data class UpdateWatermarkColor(val colorArgb: Long) : PdfToolConfigIntent
    data class UpdateWatermarkOpacity(val opacity: Float) : PdfToolConfigIntent
    data class UpdateWatermarkAngle(val angle: Float) : PdfToolConfigIntent
    data class ToggleWatermarkTiled(val tiled: Boolean) : PdfToolConfigIntent
    // Password
    data class UpdateUserPassword(val value: String) : PdfToolConfigIntent
    data class UpdateOwnerPassword(val value: String) : PdfToolConfigIntent
    data class ToggleAes256(val use256: Boolean) : PdfToolConfigIntent
    data class UpdateCurrentPdfPassword(val value: String) : PdfToolConfigIntent
    // PKCS#7
    data class SetP12Certificate(val uriString: String, val displayName: String) : PdfToolConfigIntent
    data class UpdateP12Password(val value: String) : PdfToolConfigIntent
    data class UpdateSignReason(val value: String) : PdfToolConfigIntent
    data class UpdateSignLocation(val value: String) : PdfToolConfigIntent
    data object InspectExistingCertificates : PdfToolConfigIntent
    // Converters
    data class SelectPageSize(val preset: PageSizePreset) : PdfToolConfigIntent
    data class UpdateMarginDp(val dp: Int) : PdfToolConfigIntent
    data class ToggleLandscape(val landscape: Boolean) : PdfToolConfigIntent
    data class UpdatePlainTextInput(val text: String) : PdfToolConfigIntent
    data class ToggleStitchedLongImage(val stitched: Boolean) : PdfToolConfigIntent
    // Run / Gate / Cancel
    data object ExecuteClicked : PdfToolConfigIntent
    data object CancelExecution : PdfToolConfigIntent
    data class SubmitUnlockPassword(val password: String) : PdfToolConfigIntent
    data object DismissUnlockDialog : PdfToolConfigIntent
    data object DismissRewardGate : PdfToolConfigIntent
    data object GrantRewardedPassAndRun : PdfToolConfigIntent
    data object UpgradeProClicked : PdfToolConfigIntent
    data object OpenResultClicked : PdfToolConfigIntent
    data object ShareResultClicked : PdfToolConfigIntent
    data object ResetForAnotherRun : PdfToolConfigIntent
    data object BackClicked : PdfToolConfigIntent
}

sealed interface PdfToolConfigEffect : UiEffect {
    data object NavigateBack : PdfToolConfigEffect
    data object NavigateToPaywall : PdfToolConfigEffect
    data class OpenOutputFile(val uriString: String, val displayName: String) : PdfToolConfigEffect
    data class ShareOutputFile(val uriString: String, val displayName: String) : PdfToolConfigEffect
    data class ShowMessage(val message: String) : PdfToolConfigEffect
}
