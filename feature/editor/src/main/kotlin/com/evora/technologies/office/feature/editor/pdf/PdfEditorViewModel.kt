package com.evora.technologies.office.feature.editor.pdf

import androidx.lifecycle.viewModelScope
import com.evora.technologies.office.core.domain.port.AdPlacementHook
import com.evora.technologies.office.core.domain.port.DocumentInvalidPasswordException
import com.evora.technologies.office.core.domain.port.DocumentPasswordRequiredException
import com.evora.technologies.office.core.domain.port.PdfEnginePort
import com.evora.technologies.office.core.domain.port.PdfPowerPort
import com.evora.technologies.office.core.domain.port.ProEntitlementPort
import com.evora.technologies.office.core.domain.port.SignatureRepository
import com.evora.technologies.office.core.model.NormalizedPoint
import com.evora.technologies.office.core.model.PdfAnnotationDraft
import com.evora.technologies.office.core.model.PdfAnnotationType
import com.evora.technologies.office.core.model.PdfSignaturePlacement
import com.evora.technologies.office.core.model.RewardedAdOutcome
import com.evora.technologies.office.core.model.SignatureItem
import com.evora.technologies.office.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PdfEditorViewModel @Inject constructor(
    private val pdfEngine: PdfEnginePort,
    private val pdfPowerPort: PdfPowerPort,
    private val signatureRepository: SignatureRepository,
    private val proEntitlementPort: ProEntitlementPort,
    private val adPlacementHook: AdPlacementHook,
) : MviViewModel<PdfEditorState, PdfEditorIntent, PdfEditorEffect>(
    PdfEditorState(),
) {
    private var openJob: Job? = null
    private var loadedPdfUri: String? = null
    private var renderJob: Job? = null
    private var activePassword: String? = null
    private var lastRenderWidthPx: Int = 1080
    private var pendingPkcs7Gate = false

    init {
        signatureRepository.observeSignatures()
            .onEach { list -> setState { copy(savedSignatures = list) } }
            .launchIn(viewModelScope)

        proEntitlementPort.proStatus
            .onEach { status -> setState { copy(proStatus = status) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: PdfEditorIntent) {
        when (intent) {
            is PdfEditorIntent.OpenPdf -> loadPdf(intent.uriString, intent.displayName, null)
            is PdfEditorIntent.SubmitOpenPassword -> {
                activePassword = intent.password
                loadPdf(currentState.uriString, currentState.displayName, intent.password)
            }
            PdfEditorIntent.DismissOpenPassword -> {
                setState { copy(passwordRequired = false, passwordError = null) }
                sendEffect(PdfEditorEffect.NavigateBack)
            }
            is PdfEditorIntent.SelectTab -> setState { copy(activeTab = intent.tab) }
            is PdfEditorIntent.SelectTool -> setState { copy(activeTool = intent.type) }
            is PdfEditorIntent.SelectColor -> setState { copy(activeColorArgb = intent.colorArgb) }
            is PdfEditorIntent.SetStrokeWidth -> setState { copy(inkStrokeWidthDp = intent.widthDp.coerceIn(1f, 16f)) }
            is PdfEditorIntent.AddInkStroke -> addInkStroke(intent.points)
            is PdfEditorIntent.AddMarkupBox -> addMarkupBox(intent.left, intent.top, intent.right, intent.bottom)
            is PdfEditorIntent.OpenStickyNoteAt -> setState {
                copy(showStickyNoteDialog = true, pendingNoteX = intent.xFraction, pendingNoteY = intent.yFraction)
            }
            is PdfEditorIntent.SaveStickyNote -> addStickyNote(intent.text, intent.author)
            PdfEditorIntent.DismissStickyNoteDialog -> setState { copy(showStickyNoteDialog = false) }
            PdfEditorIntent.UndoLastAnnotation -> if (currentState.annotations.isNotEmpty()) {
                setState { copy(annotations = annotations.dropLast(1)) }
            }
            PdfEditorIntent.ClearCurrentPageAnnotations -> {
                val page = currentState.currentPage
                setState { copy(annotations = annotations.filterNot { it.pageIndex == page }) }
            }
            is PdfEditorIntent.PlaceSavedSignature -> placeSignature(intent.item)
            is PdfEditorIntent.SelectPlacedSignature -> setState { copy(selectedSignaturePlacementId = intent.placementId) }
            is PdfEditorIntent.MoveSelectedSignature -> moveSelectedSignature(intent.deltaX, intent.deltaY)
            is PdfEditorIntent.ResizeSelectedSignature -> resizeSelectedSignature(intent.widthFraction)
            is PdfEditorIntent.RemovePlacedSignature -> setState {
                copy(
                    signatures = signatures.filterNot { it.id == intent.placementId },
                    selectedSignaturePlacementId = null,
                )
            }
            is PdfEditorIntent.GoToPage -> switchPage(intent.pageIndex)
            is PdfEditorIntent.RequestRenderPage -> renderCurrentPage(intent.targetWidthPx)
            PdfEditorIntent.SavePdfClicked -> saveIncremental()
            PdfEditorIntent.InspectPkcs7Clicked -> inspectPkcs7()
            PdfEditorIntent.DismissCertModal -> setState { copy(showCertModal = false) }
            is PdfEditorIntent.SignWithP12 -> signWithPkcs12(intent.p12Uri, intent.p12Password, intent.reason)
            PdfEditorIntent.OpenSetPasswordDialog -> setState { copy(showSetPasswordDialog = true) }
            is PdfEditorIntent.ConfirmSetPassword -> applySetPassword(intent.userPass, intent.aes256)
            PdfEditorIntent.OpenRemovePasswordDialog -> setState { copy(showRemovePasswordDialog = true) }
            is PdfEditorIntent.ConfirmRemovePassword -> applyRemovePassword(intent.currentPass)
            PdfEditorIntent.DismissSecurityDialogs -> setState {
                copy(showSetPasswordDialog = false, showRemovePasswordDialog = false)
            }
            PdfEditorIntent.PrintClicked -> {
                if (currentState.uriString.isNotBlank()) {
                    sendEffect(PdfEditorEffect.LaunchPrint(currentState.uriString, currentState.displayName))
                }
            }
            PdfEditorIntent.OpenSignaturePadClicked -> sendEffect(PdfEditorEffect.NavigateToSignaturePad)
            PdfEditorIntent.DismissRewardGate -> {
                pendingPkcs7Gate = false
                setState { copy(showRewardGateTitle = null) }
            }
            PdfEditorIntent.GrantRewardPassAndProceed -> grantRewardPassAndOpenCerts()
            PdfEditorIntent.UpgradeProClicked -> {
                pendingPkcs7Gate = false
                setState { copy(showRewardGateTitle = null) }
                sendEffect(PdfEditorEffect.NavigateToPaywall)
            }
            PdfEditorIntent.BackClicked -> sendEffect(PdfEditorEffect.NavigateBack)
        }
    }

    private fun loadPdf(uriString: String, displayName: String, password: String?) {
        if (password == null &&
            loadedPdfUri == uriString &&
            currentState.errorMessage == null &&
            !currentState.passwordRequired &&
            currentState.totalPages > 0
        ) {
            return
        }
        openJob?.cancel()
        openJob = launchSafely(
            onError = { err ->
                when (err) {
                    is DocumentPasswordRequiredException -> setState {
                        copy(isLoading = false, passwordRequired = true, passwordError = null)
                    }
                    is DocumentInvalidPasswordException -> setState {
                        copy(isLoading = false, passwordRequired = true, passwordError = err.message)
                    }
                    else -> setState {
                        copy(isLoading = false, errorMessage = err.message ?: "Unable to open PDF")
                    }
                }
            },
        ) {
            setState {
                copy(
                    uriString = uriString,
                    displayName = displayName,
                    isLoading = true,
                    errorMessage = null,
                    passwordError = null,
                )
            }
            val info = pdfEngine.openDocumentInfo(uriString, password)
            val raster = if (info.pageCount > 0) {
                pdfEngine.renderPage(uriString, 0, lastRenderWidthPx, password)
            } else {
                null
            }
            loadedPdfUri = uriString
            setState {
                copy(
                    isLoading = false,
                    passwordRequired = false,
                    totalPages = info.pageCount,
                    currentPage = 0,
                    pageRaster = raster,
                )
            }
        }
    }

    private fun switchPage(pageIndex: Int) {
        val total = currentState.totalPages
        if (total <= 0) return
        val bounded = pageIndex.coerceIn(0, total - 1)
        if (bounded == currentState.currentPage && currentState.pageRaster != null) return
        setState { copy(currentPage = bounded, selectedSignaturePlacementId = null) }
        renderCurrentPage(lastRenderWidthPx)
    }

    private fun renderCurrentPage(targetWidthPx: Int) {
        if (currentState.uriString.isBlank() || currentState.totalPages <= 0) return
        lastRenderWidthPx = targetWidthPx.coerceIn(480, 1800)
        renderJob?.cancel()
        renderJob = launchSafely(
            onError = { setState { copy(errorMessage = it.message) } },
        ) {
            val raster = pdfEngine.renderPage(
                uriString = currentState.uriString,
                pageIndex = currentState.currentPage,
                targetWidthPx = lastRenderWidthPx,
                password = activePassword,
            )
            setState { copy(pageRaster = raster) }
        }
    }

    private fun addInkStroke(points: List<Pair<Float, Float>>) {
        if (points.size < 2) return
        val draft = PdfAnnotationDraft(
            id = UUID.randomUUID().toString(),
            pageIndex = currentState.currentPage,
            type = PdfAnnotationType.INK,
            colorArgb = currentState.activeColorArgb,
            strokeWidthDp = currentState.inkStrokeWidthDp,
            points = points.map { NormalizedPoint(it.first.coerceIn(0f, 1f), it.second.coerceIn(0f, 1f)) },
        )
        setState { copy(annotations = annotations + draft) }
    }

    private fun addMarkupBox(left: Float, top: Float, right: Float, bottom: Float) {
        val draft = PdfAnnotationDraft(
            id = UUID.randomUUID().toString(),
            pageIndex = currentState.currentPage,
            type = currentState.activeTool,
            colorArgb = currentState.activeColorArgb,
            left = minOf(left, right).coerceIn(0f, 1f),
            top = minOf(top, bottom).coerceIn(0f, 1f),
            right = maxOf(left, right).coerceIn(0f, 1f),
            bottom = maxOf(top, bottom).coerceIn(0f, 1f),
        )
        setState { copy(annotations = annotations + draft) }
    }

    private fun addStickyNote(text: String, author: String) {
        val clean = text.trim()
        if (clean.isEmpty()) {
            setState { copy(showStickyNoteDialog = false) }
            return
        }
        val draft = PdfAnnotationDraft(
            id = UUID.randomUUID().toString(),
            pageIndex = currentState.currentPage,
            type = PdfAnnotationType.STICKY_NOTE,
            colorArgb = currentState.activeColorArgb,
            left = currentState.pendingNoteX,
            top = currentState.pendingNoteY,
            noteText = clean,
            author = author.ifBlank { "Evora User" },
        )
        setState { copy(showStickyNoteDialog = false, annotations = annotations + draft) }
    }

    private fun placeSignature(item: SignatureItem) {
        val placement = PdfSignaturePlacement(
            id = UUID.randomUUID().toString(),
            signatureAssetPath = item.assetPath,
            pageIndex = currentState.currentPage,
        )
        setState {
            copy(
                signatures = signatures + placement,
                selectedSignaturePlacementId = placement.id,
            )
        }
    }

    private fun moveSelectedSignature(dx: Float, dy: Float) {
        val id = currentState.selectedSignaturePlacementId ?: return
        setState {
            copy(
                signatures = signatures.map { sig ->
                    if (sig.id == id) {
                        sig.copy(
                            centerXFraction = (sig.centerXFraction + dx).coerceIn(0.1f, 0.9f),
                            centerYFraction = (sig.centerYFraction + dy).coerceIn(0.1f, 0.9f),
                        )
                    } else {
                        sig
                    }
                },
            )
        }
    }

    private fun resizeSelectedSignature(widthFraction: Float) {
        val id = currentState.selectedSignaturePlacementId ?: return
        setState {
            copy(
                signatures = signatures.map { sig ->
                    if (sig.id == id) sig.copy(widthFraction = widthFraction.coerceIn(0.12f, 0.65f)) else sig
                },
            )
        }
    }

    private fun saveIncremental() {
        if (currentState.isSaving) return
        launchSafely(
            onError = { err ->
                setState { copy(isSaving = false) }
                sendEffect(PdfEditorEffect.ShowMessage(err.message ?: "Failed to save PDF"))
            },
        ) {
            setState { copy(isSaving = true) }
            val base = currentState.displayName.substringBeforeLast('.')
            val result = pdfPowerPort.applyAnnotationsAndSignatures(
                sourceUriString = currentState.uriString,
                outputFileName = "${base}_annotated.pdf",
                annotations = currentState.annotations,
                signatures = currentState.signatures,
                password = activePassword,
            ).getOrThrow()
            setState {
                copy(
                    isSaving = false,
                    uriString = result.outputUriString,
                    displayName = result.outputDisplayName,
                    annotations = emptyList(),
                    signatures = emptyList(),
                    selectedSignaturePlacementId = null,
                )
            }
            sendEffect(PdfEditorEffect.ShowMessage(result.summaryMessage))
            renderCurrentPage(lastRenderWidthPx)
        }
    }

    private fun inspectPkcs7() {
        if (!currentState.proStatus.isUnlocked) {
            pendingPkcs7Gate = true
            setState { copy(showRewardGateTitle = "PKCS#7 Digital Certificate Signer") }
            return
        }
        launchSafely(
            onError = { sendEffect(PdfEditorEffect.ShowMessage(it.message ?: "Unable to inspect certificates")) },
        ) {
            val certs = pdfPowerPort.inspectPkcs7Signatures(currentState.uriString, activePassword).getOrThrow()
            setState { copy(pkcs7Certs = certs, showCertModal = true) }
        }
    }

    private fun signWithPkcs12(p12Uri: String, p12Pass: String, reason: String) {
        if (currentState.isSaving) return
        launchSafely(
            onError = { err ->
                setState { copy(isSaving = false) }
                sendEffect(PdfEditorEffect.ShowMessage(err.message ?: "Certificate signing failed"))
            },
        ) {
            setState { copy(isSaving = true) }
            val result = pdfPowerPort.signWithPkcs12(
                sourceUriString = currentState.uriString,
                p12UriString = p12Uri,
                p12Password = p12Pass,
                reason = reason,
                location = "Evora Mobile",
                pdfPassword = activePassword,
            ).getOrThrow()
            val certs = pdfPowerPort.inspectPkcs7Signatures(result.outputUriString, activePassword).getOrDefault(emptyList())
            setState {
                copy(
                    isSaving = false,
                    uriString = result.outputUriString,
                    displayName = result.outputDisplayName,
                    pkcs7Certs = certs,
                )
            }
            sendEffect(PdfEditorEffect.ShowMessage(result.summaryMessage))
        }
    }

    private fun applySetPassword(userPass: String, aes256: Boolean) {
        launchSafely(
            onError = {
                setState { copy(isSaving = false) }
                sendEffect(PdfEditorEffect.ShowMessage(it.message ?: "Unable to set password"))
            },
        ) {
            setState { copy(showSetPasswordDialog = false, isSaving = true) }
            val res = pdfPowerPort.setPassword(
                sourceUriString = currentState.uriString,
                userPassword = userPass,
                ownerPassword = userPass,
                useAes256 = aes256,
                currentPassword = activePassword,
            ).getOrThrow()
            activePassword = userPass
            setState { copy(isSaving = false, uriString = res.outputUriString, displayName = res.outputDisplayName) }
            sendEffect(PdfEditorEffect.ShowMessage(res.summaryMessage))
        }
    }

    private fun applyRemovePassword(currentPass: String) {
        launchSafely(
            onError = {
                setState { copy(isSaving = false) }
                sendEffect(PdfEditorEffect.ShowMessage(it.message ?: "Wrong password"))
            },
        ) {
            setState { copy(showRemovePasswordDialog = false, isSaving = true) }
            val res = pdfPowerPort.removePassword(currentState.uriString, currentPass).getOrThrow()
            activePassword = null
            setState { copy(isSaving = false, uriString = res.outputUriString, displayName = res.outputDisplayName) }
            sendEffect(PdfEditorEffect.ShowMessage(res.summaryMessage))
        }
    }

    private fun grantRewardPassAndOpenCerts() {
        launchSafely(
            onError = { setState { copy(showRewardGateTitle = null) }; sendEffect(PdfEditorEffect.ShowMessage(it.message ?: "Unable to unlock pass")) },
        ) {
            when (val outcome = adPlacementHook.showRewardedPassAd()) {
                is RewardedAdOutcome.Earned -> {
                    proEntitlementPort.grantRewardedPass(hours = outcome.hours)
                    setState { copy(showRewardGateTitle = null) }
                    if (pendingPkcs7Gate) {
                        pendingPkcs7Gate = false
                        inspectPkcs7()
                    }
                }
                RewardedAdOutcome.Dismissed -> {
                    pendingPkcs7Gate = false
                    setState { copy(showRewardGateTitle = null) }
                    sendEffect(PdfEditorEffect.ShowMessage("Complete the rewarded video to unlock PKCS#7 inspection."))
                }
                is RewardedAdOutcome.Unavailable -> {
                    pendingPkcs7Gate = false
                    setState { copy(showRewardGateTitle = null) }
                    sendEffect(PdfEditorEffect.ShowMessage(outcome.reason))
                }
            }
        }
    }
}
