package com.evora.technologies.office.feature.tools.hub

import com.evora.technologies.office.core.model.DocTemplatePreset
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.OfficeKind
import com.evora.technologies.office.core.model.PdfToolKind
import com.evora.technologies.office.core.mvi.UiEffect
import com.evora.technologies.office.core.mvi.UiIntent
import com.evora.technologies.office.core.mvi.UiState

enum class UtilityToolId {
    IMAGE_TO_PDF,
    TEXT_TO_PDF,
    DOC_TO_PDF,
    PDF_TO_DOCX,
    PDF_TO_IMAGE,
    MERGE_PDF,
    SPLIT_PDF,
    COMPRESS_PDF,
    WATERMARK_PDF,
    SET_PASSWORD,
    REMOVE_PASSWORD,
    SIGN_PDF,
    PRINT_DOCUMENT,
    OCR_EXTRACT,
    QR_SCANNER,
    WIFI_TRANSFER,
    TEMPLATES,
}

data class ToolsHubState(
    val templates: List<DocTemplatePreset> = emptyList(),
    val selectedTemplateFilter: OfficeKind? = null,
    val showTemplatesSheet: Boolean = false,
    val showPrintPickerSheet: Boolean = false,
    val printableDocuments: List<DocumentItem> = emptyList(),
    val isCreatingTemplate: Boolean = false,
) : UiState

sealed interface ToolsHubIntent : UiIntent {
    data class ToolClicked(val toolId: UtilityToolId) : ToolsHubIntent
    data class FilterTemplates(val kind: OfficeKind?) : ToolsHubIntent
    data class CreateFromTemplate(val preset: DocTemplatePreset) : ToolsHubIntent
    data object DismissTemplatesSheet : ToolsHubIntent
    data class PrintSelectedDocument(val doc: DocumentItem) : ToolsHubIntent
    data object DismissPrintPicker : ToolsHubIntent
    data object SettingsClicked : ToolsHubIntent
    data object PaywallClicked : ToolsHubIntent
}

sealed interface ToolsHubEffect : UiEffect {
    data class NavigateToPdfTool(val kind: PdfToolKind) : ToolsHubEffect
    data class NavigateToScannerMode(val modeName: String) : ToolsHubEffect
    data object NavigateToPaywall : ToolsHubEffect
    data object NavigateToSettings : ToolsHubEffect
    data object NavigateToWifiTransfer : ToolsHubEffect
    data class OpenCreatedOfficeDoc(val document: DocumentItem) : ToolsHubEffect
    data class LaunchSystemPrint(val document: DocumentItem) : ToolsHubEffect
    data class ShowMessage(val message: String) : ToolsHubEffect
}
