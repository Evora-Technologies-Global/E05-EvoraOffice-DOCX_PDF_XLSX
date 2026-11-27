package com.evora.technologies.office.feature.tools.hub

import androidx.lifecycle.viewModelScope
import com.evora.technologies.office.core.domain.port.DocumentRepository
import com.evora.technologies.office.core.domain.port.TemplateRepository
import com.evora.technologies.office.core.model.DocTemplatePreset
import com.evora.technologies.office.core.model.PdfToolKind
import com.evora.technologies.office.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class ToolsHubViewModel @Inject constructor(
    private val templateRepository: TemplateRepository,
    documentRepository: DocumentRepository,
) : MviViewModel<ToolsHubState, ToolsHubIntent, ToolsHubEffect>(
    ToolsHubState(templates = templateRepository.getAvailableTemplates()),
) {

    init {
        documentRepository.observeLibraryDocuments()
            .onEach { docs ->
                setState { copy(printableDocuments = docs.take(25)) }
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: ToolsHubIntent) {
        when (intent) {
            is ToolsHubIntent.ToolClicked -> dispatchTool(intent.toolId)
            is ToolsHubIntent.FilterTemplates -> setState { copy(selectedTemplateFilter = intent.kind) }
            is ToolsHubIntent.CreateFromTemplate -> createTemplate(intent.preset)
            ToolsHubIntent.DismissTemplatesSheet -> setState { copy(showTemplatesSheet = false) }
            is ToolsHubIntent.PrintSelectedDocument -> {
                setState { copy(showPrintPickerSheet = false) }
                sendEffect(ToolsHubEffect.LaunchSystemPrint(intent.doc))
            }
            ToolsHubIntent.DismissPrintPicker -> setState { copy(showPrintPickerSheet = false) }
            ToolsHubIntent.SettingsClicked -> sendEffect(ToolsHubEffect.NavigateToSettings)
            ToolsHubIntent.PaywallClicked -> sendEffect(ToolsHubEffect.NavigateToPaywall)
        }
    }

    private fun dispatchTool(toolId: UtilityToolId) {
        when (toolId) {
            UtilityToolId.IMAGE_TO_PDF -> sendEffect(ToolsHubEffect.NavigateToPdfTool(PdfToolKind.IMAGE_TO_PDF))
            UtilityToolId.TEXT_TO_PDF -> sendEffect(ToolsHubEffect.NavigateToPdfTool(PdfToolKind.TEXT_TO_PDF))
            UtilityToolId.DOC_TO_PDF -> sendEffect(ToolsHubEffect.NavigateToPdfTool(PdfToolKind.DOC_TO_PDF))
            UtilityToolId.PDF_TO_DOCX -> sendEffect(ToolsHubEffect.NavigateToPdfTool(PdfToolKind.PDF_TO_DOCX))
            UtilityToolId.PDF_TO_IMAGE -> sendEffect(ToolsHubEffect.NavigateToPdfTool(PdfToolKind.PDF_TO_IMAGES))
            UtilityToolId.MERGE_PDF -> sendEffect(ToolsHubEffect.NavigateToPdfTool(PdfToolKind.MERGE))
            UtilityToolId.SPLIT_PDF -> sendEffect(ToolsHubEffect.NavigateToPdfTool(PdfToolKind.SPLIT))
            UtilityToolId.COMPRESS_PDF -> sendEffect(ToolsHubEffect.NavigateToPdfTool(PdfToolKind.COMPRESS))
            UtilityToolId.WATERMARK_PDF -> sendEffect(ToolsHubEffect.NavigateToPdfTool(PdfToolKind.WATERMARK))
            UtilityToolId.SET_PASSWORD -> sendEffect(ToolsHubEffect.NavigateToPdfTool(PdfToolKind.SET_PASSWORD))
            UtilityToolId.REMOVE_PASSWORD -> sendEffect(ToolsHubEffect.NavigateToPdfTool(PdfToolKind.REMOVE_PASSWORD))
            UtilityToolId.SIGN_PDF -> sendEffect(ToolsHubEffect.NavigateToPdfTool(PdfToolKind.PKCS7_SIGN))
            UtilityToolId.PRINT_DOCUMENT -> setState { copy(showPrintPickerSheet = true) }
            UtilityToolId.OCR_EXTRACT -> sendEffect(ToolsHubEffect.NavigateToScannerMode("OCR"))
            UtilityToolId.QR_SCANNER -> sendEffect(ToolsHubEffect.NavigateToScannerMode("QR_BARCODE"))
            UtilityToolId.WIFI_TRANSFER -> sendEffect(ToolsHubEffect.NavigateToWifiTransfer)
            UtilityToolId.TEMPLATES -> setState { copy(showTemplatesSheet = true) }
        }
    }

    private fun createTemplate(preset: DocTemplatePreset) {
        if (currentState.isCreatingTemplate) return
        launchSafely(
            onError = { err ->
                setState { copy(isCreatingTemplate = false) }
                sendEffect(ToolsHubEffect.ShowMessage(err.message ?: "Unable to create document"))
            },
        ) {
            setState { copy(isCreatingTemplate = true) }
            val created = templateRepository.createDocumentFromTemplate(preset).getOrThrow()
            setState { copy(isCreatingTemplate = false, showTemplatesSheet = false) }
            sendEffect(ToolsHubEffect.OpenCreatedOfficeDoc(created))
        }
    }
}
