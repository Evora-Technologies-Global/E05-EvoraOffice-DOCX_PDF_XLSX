package com.evora.technologies.office.feature.scanner.camera

import com.evora.technologies.office.core.model.OcrScript
import com.evora.technologies.office.core.model.PdfToolResult
import com.evora.technologies.office.core.model.QrDecodedAction
import com.evora.technologies.office.core.model.ScanFilter
import com.evora.technologies.office.core.model.ScanMode
import com.evora.technologies.office.core.mvi.UiEffect
import com.evora.technologies.office.core.mvi.UiIntent
import com.evora.technologies.office.core.mvi.UiState

data class ScannedPageItem(
    val id: String,
    val rawPath: String,
    val processedPath: String,
    val filter: ScanFilter = ScanFilter.ORIGINAL,
    val rotationDegrees: Int = 0,
)

data class ScannerFlowState(
    val mode: ScanMode = ScanMode.DOCUMENT,
    val flashEnabled: Boolean = false,
    val showGrid: Boolean = true,
    val cameraPermissionGranted: Boolean = false,
    // Document mode
    val pages: List<ScannedPageItem> = emptyList(),
    val activePageIndex: Int = 0,
    // ID Card 2-step mode
    val idFrontPath: String? = null,
    val idBackPath: String? = null,
    // OCR mode
    val ocrScript: OcrScript = OcrScript.LATIN,
    val ocrExtractedText: String = "",
    // QR / Barcode mode
    val decodedQrs: List<QrDecodedAction> = emptyList(),
    val pendingOpenUrlConfirm: String? = null,
    // Shared status
    val isBusy: Boolean = false,
    val exportResult: PdfToolResult? = null,
    val errorMessage: String? = null,
) : UiState {
    val idCardStep: Int
        get() = when {
            idFrontPath == null -> 1
            idBackPath == null -> 2
            else -> 3
        }
}

sealed interface ScannerFlowIntent : UiIntent {
    data class InitializeMode(val mode: ScanMode) : ScannerFlowIntent
    data class CameraPermissionUpdated(val granted: Boolean) : ScannerFlowIntent
    data class SelectMode(val mode: ScanMode) : ScannerFlowIntent
    data object ToggleFlash : ScannerFlowIntent
    data object ToggleGrid : ScannerFlowIntent
    data class ImageCaptured(val filePath: String) : ScannerFlowIntent
    data class DocScannerPagesImported(val filePaths: List<String>) : ScannerFlowIntent
    // Document mode
    data class SelectPageIndex(val index: Int) : ScannerFlowIntent
    data class ApplyFilterToActivePage(val filter: ScanFilter) : ScannerFlowIntent
    data object RotateActivePage90 : ScannerFlowIntent
    data class RemovePageAt(val index: Int) : ScannerFlowIntent
    data class ExportDocumentsPdf(val outputBaseName: String) : ScannerFlowIntent
    // ID Card mode
    data object RetakeIdFront : ScannerFlowIntent
    data object RetakeIdBack : ScannerFlowIntent
    data class ExportDualIdCard(val asPdf: Boolean) : ScannerFlowIntent
    // OCR mode
    data class SelectOcrScript(val script: OcrScript) : ScannerFlowIntent
    data class UpdateOcrText(val text: String) : ScannerFlowIntent
    data object CopyOcrTextClicked : ScannerFlowIntent
    data class ExportOcrFile(val asDocx: Boolean) : ScannerFlowIntent
    // QR mode
    data class LiveQrDetected(val rawValue: String) : ScannerFlowIntent
    data class RequestOpenUrl(val url: String) : ScannerFlowIntent
    data object ConfirmOpenUrl : ScannerFlowIntent
    data object DismissOpenUrl : ScannerFlowIntent
    // Output
    data object OpenExportedResult : ScannerFlowIntent
    data object DismissExportResult : ScannerFlowIntent
    data object DismissError : ScannerFlowIntent
    data object BackClicked : ScannerFlowIntent
}

sealed interface ScannerFlowEffect : UiEffect {
    data object NavigateBack : ScannerFlowEffect
    data class OpenDocument(val uriString: String, val displayName: String) : ScannerFlowEffect
    data class CopyToClipboard(val text: String) : ScannerFlowEffect
    data class LaunchBrowserUrl(val url: String) : ScannerFlowEffect
    data class ShowMessage(val message: String) : ScannerFlowEffect
}
