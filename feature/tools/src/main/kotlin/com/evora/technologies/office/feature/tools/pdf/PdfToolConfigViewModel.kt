package com.evora.technologies.office.feature.tools.pdf

import androidx.lifecycle.viewModelScope
import com.evora.technologies.office.core.domain.port.DocumentInvalidPasswordException
import com.evora.technologies.office.core.domain.port.DocumentPasswordRequiredException
import com.evora.technologies.office.core.domain.port.DocumentRepository
import com.evora.technologies.office.core.domain.port.PdfEnginePort
import com.evora.technologies.office.core.domain.port.PdfPowerPort
import com.evora.technologies.office.core.domain.port.ProEntitlementPort
import com.evora.technologies.office.core.model.CompressQuality
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.PdfToolKind
import com.evora.technologies.office.core.model.SplitMode
import com.evora.technologies.office.core.model.WatermarkConfig
import com.evora.technologies.office.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class PdfToolConfigViewModel @Inject constructor(
    private val pdfPowerPort: PdfPowerPort,
    private val pdfEnginePort: PdfEnginePort,
    private val documentRepository: DocumentRepository,
    private val proEntitlementPort: ProEntitlementPort,
) : MviViewModel<PdfToolConfigState, PdfToolConfigIntent, PdfToolConfigEffect>(
    PdfToolConfigState(),
) {
    private var runJob: Job? = null
    private var initializedKey: String? = null

    init {
        proEntitlementPort.proStatus
            .onEach { status ->
                val freeDocx = proEntitlementPort.hasFreeDailyPdfToDocxAvailable()
                setState { copy(proStatus = status, freeDailyPdfToDocxAvailable = freeDocx) }
            }
            .launchIn(viewModelScope)

        documentRepository.observeLibraryDocuments()
            .onEach { docs -> setState { copy(availableLibraryDocs = docs) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: PdfToolConfigIntent) {
        when (intent) {
            is PdfToolConfigIntent.Initialize -> initializeTool(intent.toolKind, intent.preselectedUri)
            PdfToolConfigIntent.OpenLibraryPicker -> setState { copy(showLibraryPicker = true) }
            PdfToolConfigIntent.DismissLibraryPicker -> setState { copy(showLibraryPicker = false) }
            is PdfToolConfigIntent.AddLibraryDocument -> addDocumentInput(intent.doc)
            is PdfToolConfigIntent.AddExternalFiles -> addExternalFiles(intent.files)
            is PdfToolConfigIntent.RemoveInputAt -> removeInput(intent.index)
            is PdfToolConfigIntent.MoveInput -> moveInput(intent.fromIndex, intent.toIndex)
            is PdfToolConfigIntent.UpdateOutputName -> setState { copy(outputBaseName = intent.name) }
            is PdfToolConfigIntent.SelectSplitMode -> setState { copy(splitMode = intent.mode) }
            is PdfToolConfigIntent.UpdateSplitRange -> setState { copy(splitRangeExpression = intent.expr) }
            is PdfToolConfigIntent.UpdateSplitEveryN -> setState { copy(splitEveryN = intent.n.coerceIn(1, 100)) }
            is PdfToolConfigIntent.ToggleSplitPage -> toggleSplitPage(intent.zeroBasedPage)
            is PdfToolConfigIntent.SelectCompressQuality -> setState { copy(compressQuality = intent.quality) }
            is PdfToolConfigIntent.UpdateWatermarkText -> setState { copy(watermarkText = intent.text) }
            is PdfToolConfigIntent.UpdateWatermarkSize -> setState { copy(watermarkFontSize = intent.size.coerceIn(18f, 72f)) }
            is PdfToolConfigIntent.UpdateWatermarkColor -> setState { copy(watermarkColorArgb = intent.colorArgb) }
            is PdfToolConfigIntent.UpdateWatermarkOpacity -> setState { copy(watermarkOpacity = intent.opacity.coerceIn(0.1f, 1f)) }
            is PdfToolConfigIntent.UpdateWatermarkAngle -> setState { copy(watermarkAngle = intent.angle) }
            is PdfToolConfigIntent.ToggleWatermarkTiled -> setState { copy(watermarkTiled = intent.tiled) }
            is PdfToolConfigIntent.UpdateUserPassword -> setState { copy(userPassword = intent.value) }
            is PdfToolConfigIntent.UpdateOwnerPassword -> setState { copy(ownerPassword = intent.value) }
            is PdfToolConfigIntent.ToggleAes256 -> setState { copy(useAes256 = intent.use256) }
            is PdfToolConfigIntent.UpdateCurrentPdfPassword -> setState { copy(currentPdfPassword = intent.value) }
            is PdfToolConfigIntent.SetP12Certificate -> setState {
                copy(p12CertUriString = intent.uriString, p12CertName = intent.displayName)
            }
            is PdfToolConfigIntent.UpdateP12Password -> setState { copy(p12Password = intent.value) }
            is PdfToolConfigIntent.UpdateSignReason -> setState { copy(signReason = intent.value) }
            is PdfToolConfigIntent.UpdateSignLocation -> setState { copy(signLocation = intent.value) }
            PdfToolConfigIntent.InspectExistingCertificates -> inspectCertificates()
            is PdfToolConfigIntent.SelectPageSize -> setState { copy(pageSizePreset = intent.preset) }
            is PdfToolConfigIntent.UpdateMarginDp -> setState { copy(imageMarginDp = intent.dp.coerceIn(0, 64)) }
            is PdfToolConfigIntent.ToggleLandscape -> setState { copy(landscape = intent.landscape) }
            is PdfToolConfigIntent.UpdatePlainTextInput -> setState { copy(plainTextInput = intent.text) }
            is PdfToolConfigIntent.ToggleStitchedLongImage -> setState { copy(stitchedLongImage = intent.stitched) }
            PdfToolConfigIntent.ExecuteClicked -> checkProGateAndExecute()
            PdfToolConfigIntent.CancelExecution -> {
                runJob?.cancel()
                setState { copy(isProcessing = false, progress = 0f) }
            }
            is PdfToolConfigIntent.SubmitUnlockPassword -> {
                setState { copy(currentPdfPassword = intent.password, passwordPromptRequired = false, passwordPromptError = null) }
                executeTool(passwordOverride = intent.password)
            }
            PdfToolConfigIntent.DismissUnlockDialog -> setState { copy(passwordPromptRequired = false, passwordPromptError = null) }
            PdfToolConfigIntent.DismissRewardGate -> setState { copy(showRewardGateTitle = null) }
            PdfToolConfigIntent.GrantRewardedPassAndRun -> grantRewardPassAndRun()
            PdfToolConfigIntent.UpgradeProClicked -> {
                setState { copy(showRewardGateTitle = null) }
                sendEffect(PdfToolConfigEffect.NavigateToPaywall)
            }
            PdfToolConfigIntent.OpenResultClicked -> {
                currentState.result?.let { res ->
                    sendEffect(PdfToolConfigEffect.OpenOutputFile(res.outputUriString, res.outputDisplayName))
                }
            }
            PdfToolConfigIntent.ShareResultClicked -> {
                currentState.result?.let { res ->
                    sendEffect(PdfToolConfigEffect.ShareOutputFile(res.outputUriString, res.outputDisplayName))
                }
            }
            PdfToolConfigIntent.ResetForAnotherRun -> setState { copy(result = null, errorMessage = null, progress = 0f) }
            PdfToolConfigIntent.BackClicked -> {
                runJob?.cancel()
                sendEffect(PdfToolConfigEffect.NavigateBack)
            }
        }
    }

    private fun initializeTool(kind: PdfToolKind, preselectedUri: String?) {
        val key = "$kind|${preselectedUri.orEmpty()}"
        if (initializedKey == key) return
        initializedKey = key
        val defaultOut = when (kind) {
            PdfToolKind.MERGE -> "Merged_PDF"
            PdfToolKind.IMAGE_TO_PDF -> "Scanned_Images"
            PdfToolKind.TEXT_TO_PDF -> "Text_Document"
            else -> "Evora_Output"
        }
        setState {
            PdfToolConfigState(
                toolKind = kind,
                proStatus = proStatus,
                freeDailyPdfToDocxAvailable = freeDailyPdfToDocxAvailable,
                availableLibraryDocs = availableLibraryDocs,
                outputBaseName = defaultOut,
            )
        }
        if (!preselectedUri.isNullOrBlank()) {
            launchSafely {
                val uris = preselectedUri.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                if (uris.size > 1) {
                    val items = uris.map { uri ->
                        val meta = documentRepository.getDocumentMeta(uri)
                        ToolInputFile(uri, meta?.displayName ?: uri.substringAfterLast('/'))
                    }
                    addExternalFiles(items)
                } else {
                    val single = uris.first()
                    val meta = documentRepository.getDocumentMeta(single)
                    if (meta != null) {
                        addDocumentInput(meta)
                    } else {
                        addExternalFiles(listOf(ToolInputFile(single, single.substringAfterLast('/'))))
                    }
                }
            }
        }
    }

    private fun addDocumentInput(doc: DocumentItem) {
        val entry = ToolInputFile(doc.uriString, doc.displayName)
        addExternalFiles(listOf(entry))
        setState { copy(showLibraryPicker = false) }
    }

    private fun addExternalFiles(files: List<ToolInputFile>) {
        val singleFileTools = setOf(
            PdfToolKind.SPLIT,
            PdfToolKind.COMPRESS,
            PdfToolKind.WATERMARK,
            PdfToolKind.SET_PASSWORD,
            PdfToolKind.REMOVE_PASSWORD,
            PdfToolKind.PKCS7_SIGN,
            PdfToolKind.DOC_TO_PDF,
            PdfToolKind.PDF_TO_IMAGES,
            PdfToolKind.PDF_TO_DOCX,
        )
        val merged = if (currentState.toolKind in singleFileTools) {
            files.take(1)
        } else {
            val existing = currentState.selectedInputs.map { it.uriString }.toSet()
            currentState.selectedInputs + files.filter { it.uriString !in existing }
        }
        setState { copy(selectedInputs = merged, errorMessage = null) }
        val first = merged.firstOrNull()
        if (first != null && currentState.toolKind == PdfToolKind.SPLIT) {
            probePdfPageCount(first.uriString)
        }
    }

    private fun probePdfPageCount(uriString: String) {
        launchSafely(
            onError = { /* handled on execute */ },
        ) {
            val count = pdfEnginePort.getPageCount(uriString, currentState.currentPdfPassword.ifBlank { null })
            setState { copy(sourceTotalPages = count) }
        }
    }

    private fun removeInput(index: Int) {
        val list = currentState.selectedInputs.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            setState { copy(selectedInputs = list) }
        }
    }

    private fun moveInput(from: Int, to: Int) {
        val list = currentState.selectedInputs.toMutableList()
        if (from !in list.indices || to !in list.indices) return
        val item = list.removeAt(from)
        list.add(to, item)
        setState { copy(selectedInputs = list) }
    }

    private fun toggleSplitPage(pageZeroBased: Int) {
        val next = currentState.splitSelectedPages.toMutableSet()
        if (!next.add(pageZeroBased)) next.remove(pageZeroBased)
        setState { copy(splitSelectedPages = next) }
    }

    private fun checkProGateAndExecute() {
        launchSafely(
            onError = { setState { copy(errorMessage = it.message) } },
        ) {
            val unlocked = proEntitlementPort.proStatus.value.isUnlocked
            if (!unlocked) {
                val gateReason: String? = when (currentState.toolKind) {
                    PdfToolKind.MERGE if currentState.selectedInputs.size > 3 ->
                        "Merge > 3 PDF Files"
                    PdfToolKind.COMPRESS if currentState.compressQuality == CompressQuality.HIGH ->
                        "High Compression Mode"
                    PdfToolKind.WATERMARK -> "Watermark PDF"
                    PdfToolKind.PKCS7_SIGN -> "PKCS#7 Digital Signature"
                    PdfToolKind.PDF_TO_DOCX if !proEntitlementPort.hasFreeDailyPdfToDocxAvailable() ->
                        "Offline PDF to DOCX"
                    else -> null
                }
                if (gateReason != null) {
                    setState { copy(showRewardGateTitle = gateReason) }
                    return@launchSafely
                }
            }
            executeTool(passwordOverride = currentState.currentPdfPassword.ifBlank { null })
        }
    }

    private fun grantRewardPassAndRun() {
        launchSafely(
            onError = { setState { copy(showRewardGateTitle = null, errorMessage = it.message) } },
        ) {
            proEntitlementPort.grantRewardedPass(hours = 24)
            setState { copy(showRewardGateTitle = null) }
            executeTool(passwordOverride = currentState.currentPdfPassword.ifBlank { null })
        }
    }

    private fun executeTool(passwordOverride: String?) {
        if (currentState.isProcessing) return
        runJob?.cancel()
        runJob = launchSafely(
            onError = { err ->
                when (err) {
                    is DocumentPasswordRequiredException -> setState {
                        copy(isProcessing = false, passwordPromptRequired = true, passwordPromptError = null)
                    }
                    is DocumentInvalidPasswordException -> setState {
                        copy(isProcessing = false, passwordPromptRequired = true, passwordPromptError = err.message)
                    }
                    else -> setState {
                        copy(isProcessing = false, progress = 0f, errorMessage = err.message ?: "Operation failed")
                    }
                }
            },
        ) {
            setState { copy(isProcessing = true, progress = 0.05f, errorMessage = null, result = null) }
            val stateSnap = currentState
            val primaryUri = stateSnap.selectedInputs.firstOrNull()?.uriString

            val res = when (stateSnap.toolKind) {
                PdfToolKind.MERGE -> pdfPowerPort.mergePdfs(
                    sourceUris = stateSnap.selectedInputs.map { it.uriString },
                    outputBaseName = stateSnap.outputBaseName,
                )
                PdfToolKind.SPLIT -> {
                    val uri = primaryUri ?: error("Select a PDF file first")
                    pdfPowerPort.splitPdf(
                        sourceUriString = uri,
                        mode = stateSnap.splitMode,
                        rangeExpression = stateSnap.splitRangeExpression,
                        everyNPages = stateSnap.splitEveryN,
                        selectedZeroBasedPages = stateSnap.splitSelectedPages,
                        password = passwordOverride,
                        onProgress = { p -> setState { copy(progress = p) } },
                    )
                }
                PdfToolKind.COMPRESS -> {
                    val uri = primaryUri ?: error("Select a PDF file first")
                    pdfPowerPort.compressPdf(
                        sourceUriString = uri,
                        quality = stateSnap.compressQuality,
                        password = passwordOverride,
                        onProgress = { p -> setState { copy(progress = p) } },
                    )
                }
                PdfToolKind.WATERMARK -> {
                    val uri = primaryUri ?: error("Select a PDF file first")
                    pdfPowerPort.watermarkPdf(
                        sourceUriString = uri,
                        config = WatermarkConfig(
                            text = stateSnap.watermarkText,
                            fontSizePt = stateSnap.watermarkFontSize,
                            colorArgb = stateSnap.watermarkColorArgb,
                            opacity = stateSnap.watermarkOpacity,
                            angleDegrees = stateSnap.watermarkAngle,
                            tiled = stateSnap.watermarkTiled,
                        ),
                        password = passwordOverride,
                        onProgress = { p -> setState { copy(progress = p) } },
                    )
                }
                PdfToolKind.SET_PASSWORD -> {
                    val uri = primaryUri ?: error("Select a PDF file first")
                    pdfPowerPort.setPassword(
                        sourceUriString = uri,
                        userPassword = stateSnap.userPassword,
                        ownerPassword = stateSnap.ownerPassword,
                        useAes256 = stateSnap.useAes256,
                        currentPassword = passwordOverride,
                    )
                }
                PdfToolKind.REMOVE_PASSWORD -> {
                    val uri = primaryUri ?: error("Select a PDF file first")
                    pdfPowerPort.removePassword(
                        sourceUriString = uri,
                        currentPassword = passwordOverride ?: stateSnap.currentPdfPassword,
                    )
                }
                PdfToolKind.PKCS7_SIGN -> {
                    val uri = primaryUri ?: error("Select a PDF file first")
                    val p12 = stateSnap.p12CertUriString ?: error("Select a .p12 or .pfx certificate file")
                    pdfPowerPort.signWithPkcs12(
                        sourceUriString = uri,
                        p12UriString = p12,
                        p12Password = stateSnap.p12Password,
                        reason = stateSnap.signReason,
                        location = stateSnap.signLocation,
                        pdfPassword = passwordOverride,
                    )
                }
                PdfToolKind.IMAGE_TO_PDF -> pdfPowerPort.imagesToPdf(
                    imageUris = stateSnap.selectedInputs.map { it.uriString },
                    outputBaseName = stateSnap.outputBaseName,
                    pageSize = stateSnap.pageSizePreset,
                    marginDp = stateSnap.imageMarginDp,
                    landscape = stateSnap.landscape,
                    onProgress = { p -> setState { copy(progress = p) } },
                )
                PdfToolKind.TEXT_TO_PDF -> pdfPowerPort.textToPdf(
                    textContent = stateSnap.plainTextInput,
                    outputBaseName = stateSnap.outputBaseName,
                    pageSize = stateSnap.pageSizePreset,
                    onProgress = { p -> setState { copy(progress = p) } },
                )
                PdfToolKind.DOC_TO_PDF -> {
                    val uri = primaryUri ?: error("Select a Word, Excel, PPT, ODF, or HWP file")
                    pdfPowerPort.documentToPdf(uri) { p -> setState { copy(progress = p) } }
                }
                PdfToolKind.PDF_TO_IMAGES -> {
                    val uri = primaryUri ?: error("Select a PDF file first")
                    pdfPowerPort.pdfToImages(
                        sourceUriString = uri,
                        stitchedLongImage = stateSnap.stitchedLongImage,
                        password = passwordOverride,
                        onProgress = { p -> setState { copy(progress = p) } },
                    )
                }
                PdfToolKind.PDF_TO_DOCX -> {
                    val uri = primaryUri ?: error("Select a PDF file first")
                    val out = pdfPowerPort.pdfToDocxOffline(
                        sourceUriString = uri,
                        password = passwordOverride,
                        onProgress = { p -> setState { copy(progress = p) } },
                    )
                    if (out.isSuccess && !proEntitlementPort.proStatus.value.isUnlocked) {
                        proEntitlementPort.consumeFreeDailyPdfToDocx()
                    }
                    out
                }
            }.getOrThrow()

            setState { copy(isProcessing = false, progress = 1f, result = res) }
        }
    }

    private fun inspectCertificates() {
        val uri = currentState.selectedInputs.firstOrNull()?.uriString ?: return
        launchSafely(
            onError = { setState { copy(errorMessage = it.message) } },
        ) {
            val certs = pdfPowerPort.inspectPkcs7Signatures(
                uriString = uri,
                password = currentState.currentPdfPassword.ifBlank { null },
            ).getOrThrow()
            setState { copy(inspectedCerts = certs) }
            if (certs.isEmpty()) {
                sendEffect(PdfToolConfigEffect.ShowMessage("No digital PKCS#7 signatures found in this PDF."))
            }
        }
    }
}
