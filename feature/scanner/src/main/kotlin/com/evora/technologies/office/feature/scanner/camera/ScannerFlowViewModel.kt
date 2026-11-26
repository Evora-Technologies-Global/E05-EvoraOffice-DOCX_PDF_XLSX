package com.evora.technologies.office.feature.scanner.camera

import androidx.lifecycle.viewModelScope
import com.evora.technologies.office.core.domain.port.AdPlacementHook
import com.evora.technologies.office.core.domain.port.AnalyticsPort
import com.evora.technologies.office.core.domain.port.ProEntitlementPort
import com.evora.technologies.office.core.domain.port.ScannerPort
import com.evora.technologies.office.core.model.OcrScript
import com.evora.technologies.office.core.model.RewardedAdOutcome
import com.evora.technologies.office.core.model.ScanFilter
import com.evora.technologies.office.core.model.ScanMode
import com.evora.technologies.office.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ScannerFlowViewModel @Inject constructor(
    private val scannerPort: ScannerPort,
    private val proEntitlementPort: ProEntitlementPort,
    private val adPlacementHook: AdPlacementHook,
    private val analyticsPort: AnalyticsPort,
) : MviViewModel<ScannerFlowState, ScannerFlowIntent, ScannerFlowEffect>(
    ScannerFlowState(),
) {
    private var workJob: Job? = null
    private var pendingGateKind: String? = null

    init {
        proEntitlementPort.proStatus
            .onEach { status -> setState { copy(proStatus = status) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: ScannerFlowIntent) {
        when (intent) {
            is ScannerFlowIntent.InitializeMode -> setState { copy(mode = intent.mode) }
            is ScannerFlowIntent.CameraPermissionUpdated -> setState { copy(cameraPermissionGranted = intent.granted) }
            is ScannerFlowIntent.SelectMode -> setState { copy(mode = intent.mode, errorMessage = null) }
            ScannerFlowIntent.ToggleFlash -> setState { copy(flashEnabled = !flashEnabled) }
            ScannerFlowIntent.ToggleGrid -> setState { copy(showGrid = !showGrid) }
            is ScannerFlowIntent.ImageCaptured -> handleCapturedImage(intent.filePath)
            is ScannerFlowIntent.DocScannerPagesImported -> handleDocScannerPages(intent.filePaths)
            is ScannerFlowIntent.SelectPageIndex -> setState {
                copy(activePageIndex = intent.index.coerceIn(0, (pages.size - 1).coerceAtLeast(0)))
            }
            is ScannerFlowIntent.ApplyFilterToActivePage -> applyFilterAndRotate(filter = intent.filter, deltaRot = 0)
            ScannerFlowIntent.RotateActivePage90 -> {
                val active = currentState.pages.getOrNull(currentState.activePageIndex) ?: return
                applyFilterAndRotate(filter = active.filter, deltaRot = 90)
            }
            is ScannerFlowIntent.RemovePageAt -> removePage(intent.index)
            is ScannerFlowIntent.ExportDocumentsPdf -> exportPagesPdf(intent.outputBaseName)
            ScannerFlowIntent.RetakeIdFront -> setState { copy(idFrontPath = null) }
            ScannerFlowIntent.RetakeIdBack -> setState { copy(idBackPath = null) }
            is ScannerFlowIntent.ExportDualIdCard -> exportIdCard(intent.asPdf)
            is ScannerFlowIntent.SelectOcrScript -> setState { copy(ocrScript = intent.script) }
            is ScannerFlowIntent.UpdateOcrText -> setState { copy(ocrExtractedText = intent.text) }
            ScannerFlowIntent.CopyOcrTextClicked -> {
                val text = currentState.ocrExtractedText.trim()
                if (text.isNotEmpty()) {
                    sendEffect(ScannerFlowEffect.CopyToClipboard(text))
                    sendEffect(ScannerFlowEffect.ShowMessage("Copied OCR text"))
                }
            }
            is ScannerFlowIntent.ExportOcrFile -> exportOcr(intent.asDocx)
            is ScannerFlowIntent.LiveQrDetected -> decodeLiveQr(intent.rawValue)
            is ScannerFlowIntent.RequestOpenUrl -> setState { copy(pendingOpenUrlConfirm = intent.url) }
            ScannerFlowIntent.ConfirmOpenUrl -> {
                val url = currentState.pendingOpenUrlConfirm
                setState { copy(pendingOpenUrlConfirm = null) }
                if (url != null) sendEffect(ScannerFlowEffect.LaunchBrowserUrl(url))
            }
            ScannerFlowIntent.DismissOpenUrl -> setState { copy(pendingOpenUrlConfirm = null) }
            ScannerFlowIntent.DismissRewardGate -> {
                pendingGateKind = null
                setState { copy(showRewardGateTitle = null) }
            }
            ScannerFlowIntent.GrantRewardedPassClicked -> grantPassAndResume()
            ScannerFlowIntent.UpgradeProClicked -> {
                pendingGateKind = null
                setState { copy(showRewardGateTitle = null) }
                sendEffect(ScannerFlowEffect.NavigateToPaywall)
            }
            ScannerFlowIntent.OpenExportedResult -> {
                val res = currentState.exportResult ?: return
                setState { copy(exportResult = null) }
                sendEffect(ScannerFlowEffect.OpenDocument(res.outputUriString, res.outputDisplayName))
            }
            ScannerFlowIntent.DismissExportResult -> setState { copy(exportResult = null) }
            ScannerFlowIntent.DismissError -> setState { copy(errorMessage = null) }
            ScannerFlowIntent.BackClicked -> {
                workJob?.cancel()
                sendEffect(ScannerFlowEffect.NavigateBack)
            }
        }
    }

    private fun handleCapturedImage(filePath: String) {
        when (currentState.mode) {
            ScanMode.DOCUMENT -> {
                val item = ScannedPageItem(
                    id = UUID.randomUUID().toString(),
                    rawPath = filePath,
                    processedPath = filePath,
                )
                val updated = currentState.pages + item
                setState { copy(pages = updated, activePageIndex = updated.lastIndex, errorMessage = null) }
            }
            ScanMode.ID_CARD -> {
                if (currentState.idFrontPath == null) {
                    setState { copy(idFrontPath = filePath, errorMessage = null) }
                } else {
                    setState { copy(idBackPath = filePath, errorMessage = null) }
                }
            }
            ScanMode.OCR -> runOcrOnImage(filePath, currentState.ocrScript)
            ScanMode.QR_BARCODE -> runBarcodeOnImage(filePath)
        }
    }

    private fun handleDocScannerPages(paths: List<String>) {
        if (paths.isEmpty()) return
        val newItems = paths.map { p ->
            ScannedPageItem(id = UUID.randomUUID().toString(), rawPath = p, processedPath = p)
        }
        val combined = currentState.pages + newItems
        setState { copy(pages = combined, activePageIndex = combined.lastIndex, errorMessage = null) }
    }

    private fun applyFilterAndRotate(filter: ScanFilter, deltaRot: Int) {
        val idx = currentState.activePageIndex
        val page = currentState.pages.getOrNull(idx) ?: return
        val nextRot = (page.rotationDegrees + deltaRot) % 360
        workJob?.cancel()
        workJob = launchSafely(
            onError = { setState { copy(isBusy = false, errorMessage = it.message) } },
        ) {
            setState { copy(isBusy = true) }
            val processed = scannerPort.applyFilterToImage(page.rawPath, filter, nextRot).getOrThrow()
            val updated = currentState.pages.toMutableList()
            updated[idx] = page.copy(processedPath = processed, filter = filter, rotationDegrees = nextRot)
            setState { copy(isBusy = false, pages = updated) }
        }
    }

    private fun removePage(index: Int) {
        val list = currentState.pages.toMutableList()
        if (index !in list.indices) return
        list.removeAt(index)
        setState {
            copy(
                pages = list,
                activePageIndex = activePageIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0)),
            )
        }
    }

    private fun exportPagesPdf(baseName: String) {
        if (currentState.pages.isEmpty() || currentState.isBusy) return
        workJob?.cancel()
        workJob = launchSafely(
            onError = { setState { copy(isBusy = false, errorMessage = it.message) } },
        ) {
            setState { copy(isBusy = true, errorMessage = null) }
            val res = scannerPort.exportPagesToPdf(
                imagePaths = currentState.pages.map { it.processedPath },
                outputBaseName = baseName.ifBlank { "Scan_${System.currentTimeMillis()}" },
            ).getOrThrow()
            analyticsPort.logScanOcrComplete("doc", currentState.pages.size, "none")
            setState { copy(isBusy = false, exportResult = res) }
            adPlacementHook.onMilestoneCompleted("scanner_doc_export")
        }
    }

    private fun exportIdCard(asPdf: Boolean) {
        val front = currentState.idFrontPath ?: return
        val back = currentState.idBackPath ?: return
        if (!currentState.proStatus.isUnlocked) {
            pendingGateKind = if (asPdf) "ID_PDF" else "ID_PNG"
            setState { copy(showRewardGateTitle = "Dual-Side ID Card A4 Layout") }
            return
        }
        runIdCardExport(front, back, asPdf)
    }

    private fun runIdCardExport(front: String, back: String, asPdf: Boolean) {
        workJob?.cancel()
        workJob = launchSafely(
            onError = { setState { copy(isBusy = false, errorMessage = it.message) } },
        ) {
            setState { copy(isBusy = true, errorMessage = null) }
            val res = scannerPort.composeDualIdCard(
                frontImagePath = front,
                backImagePath = back,
                exportAsPdf = asPdf,
                outputBaseName = "ID_Card_${System.currentTimeMillis()}",
            ).getOrThrow()
            analyticsPort.logScanOcrComplete("id_card", 2, "none")
            setState { copy(isBusy = false, exportResult = res) }
            adPlacementHook.onMilestoneCompleted("scanner_id_card_export")
        }
    }

    private fun runOcrOnImage(path: String, script: OcrScript) {
        workJob?.cancel()
        workJob = launchSafely(
            onError = { setState { copy(isBusy = false, errorMessage = it.message) } },
        ) {
            setState { copy(isBusy = true, errorMessage = null) }
            val res = scannerPort.recognizeTextFromFile(path, script).getOrThrow()
            val existing = currentState.ocrExtractedText
            val merged = if (existing.isBlank()) res.text else "$existing\n\n${res.text}"
            setState { copy(isBusy = false, ocrExtractedText = merged) }
            if (res.text.isBlank()) {
                sendEffect(ScannerFlowEffect.ShowMessage("No text recognized for ${script.label}"))
            }
        }
    }

    private fun exportOcr(asDocx: Boolean) {
        val text = currentState.ocrExtractedText.trim()
        if (text.isEmpty()) {
            sendEffect(ScannerFlowEffect.ShowMessage("Capture or extract text before exporting"))
            return
        }
        workJob?.cancel()
        workJob = launchSafely(
            onError = { setState { copy(isBusy = false, errorMessage = it.message) } },
        ) {
            setState { copy(isBusy = true) }
            val res = scannerPort.exportOcrText(
                text = text,
                outputBaseName = "OCR_Extract_${System.currentTimeMillis()}",
                asDocx = asDocx,
            ).getOrThrow()
            setState { copy(isBusy = false, exportResult = res) }
        }
    }

    private fun runBarcodeOnImage(path: String) {
        workJob?.cancel()
        workJob = launchSafely(
            onError = { setState { copy(isBusy = false, errorMessage = it.message) } },
        ) {
            setState { copy(isBusy = true) }
            val items = scannerPort.decodeBarcodesFromFile(path).getOrThrow()
            setState { copy(isBusy = false, decodedQrs = items) }
            if (items.isEmpty()) {
                sendEffect(ScannerFlowEffect.ShowMessage("No QR or Barcode detected in image"))
            }
        }
    }

    private fun decodeLiveQr(rawValue: String) {
        if (rawValue.isBlank()) return
        launchSafely {
            val action = scannerPort.decodeBarcodeFromRaw(rawValue)
            if (currentState.decodedQrs.none { it.rawValue == action.rawValue }) {
                setState { copy(decodedQrs = listOf(action) + decodedQrs.take(4)) }
            }
        }
    }

    private fun grantPassAndResume() {
        val gate = pendingGateKind
        launchSafely(
            onError = { setState { copy(showRewardGateTitle = null, errorMessage = it.message) } },
        ) {
            when (val outcome = adPlacementHook.showRewardedPassAd()) {
                is RewardedAdOutcome.Earned -> {
                    proEntitlementPort.grantRewardedPass(hours = outcome.hours)
                    pendingGateKind = null
                    setState { copy(showRewardGateTitle = null) }
                    val front = currentState.idFrontPath
                    val back = currentState.idBackPath
                    if (front != null && back != null && (gate == "ID_PDF" || gate == "ID_PNG")) {
                        runIdCardExport(front, back, asPdf = gate == "ID_PDF")
                    }
                }
                RewardedAdOutcome.Dismissed -> {
                    pendingGateKind = null
                    setState { copy(showRewardGateTitle = null) }
                    sendEffect(ScannerFlowEffect.ShowMessage("Complete the rewarded video to unlock Dual ID Card export."))
                }
                is RewardedAdOutcome.Unavailable -> {
                    pendingGateKind = null
                    setState { copy(showRewardGateTitle = null) }
                    sendEffect(ScannerFlowEffect.ShowMessage(outcome.reason))
                }
            }
        }
    }
}
