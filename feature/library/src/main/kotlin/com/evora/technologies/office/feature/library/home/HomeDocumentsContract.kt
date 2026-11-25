package com.evora.technologies.office.feature.library.home

import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocTemplatePreset
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.OfficeKind
import com.evora.technologies.office.core.model.SortOrder
import com.evora.technologies.office.core.mvi.UiEffect
import com.evora.technologies.office.core.mvi.UiIntent
import com.evora.technologies.office.core.mvi.UiState

data class HomeDocumentsState(
    val documents: List<DocumentItem> = emptyList(),
    val selectedFilter: DocFormat? = null,
    val sortOrder: SortOrder = SortOrder.MODIFIED_DESC,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val showDefaultAppBanner: Boolean = false,
    val selectedPreviewDocId: String? = null,
    val contextMenuDocId: String? = null,
    val renameTargetDocId: String? = null,
    val renameError: String? = null,
    val propertiesDocId: String? = null,
    val showTemplatesSheet: Boolean = false,
    val templates: List<DocTemplatePreset> = emptyList(),
    val selectedTemplateFilter: OfficeKind? = null,
    val isCreatingTemplate: Boolean = false,
    val errorMessage: String? = null,
) : UiState {
    val selectedPreviewDoc: DocumentItem?
        get() = documents.firstOrNull { it.id == selectedPreviewDocId } ?: documents.firstOrNull()

    val contextMenuDoc: DocumentItem?
        get() = documents.firstOrNull { it.uriString == contextMenuDocId }

    val renameTargetDoc: DocumentItem?
        get() = documents.firstOrNull { it.uriString == renameTargetDocId }

    val propertiesDoc: DocumentItem?
        get() = documents.firstOrNull { it.uriString == propertiesDocId }
}

sealed interface HomeDocumentsIntent : UiIntent {
    data class SelectFilter(val format: DocFormat?) : HomeDocumentsIntent
    data class SelectSortOrder(val sortOrder: SortOrder) : HomeDocumentsIntent
    data object RefreshRequested : HomeDocumentsIntent
    data class DocumentClicked(val document: DocumentItem, val isTwoPaneTablet: Boolean = false) : HomeDocumentsIntent
    data class OpenDocumentForced(val document: DocumentItem) : HomeDocumentsIntent
    data class ToggleFavorite(val uriString: String) : HomeDocumentsIntent
    data class OpenContextMenu(val uriString: String) : HomeDocumentsIntent
    data object DismissContextMenu : HomeDocumentsIntent
    data class StartRename(val uriString: String) : HomeDocumentsIntent
    data class ConfirmRename(val uriString: String, val newBaseName: String) : HomeDocumentsIntent
    data object DismissRename : HomeDocumentsIntent
    data class ShareDocument(val document: DocumentItem) : HomeDocumentsIntent
    data class MoveToTrash(val uriString: String) : HomeDocumentsIntent
    data class AddHomeShortcut(val document: DocumentItem) : HomeDocumentsIntent
    data class OpenProperties(val uriString: String) : HomeDocumentsIntent
    data object DismissProperties : HomeDocumentsIntent
    data object DismissDefaultAppBanner : HomeDocumentsIntent
    data object OpenDefaultAppSettings : HomeDocumentsIntent
    data object ImportViaSafClicked : HomeDocumentsIntent
    data class SafDocumentPicked(val uriString: String?) : HomeDocumentsIntent
    data object OpenTemplatesSheet : HomeDocumentsIntent
    data object DismissTemplatesSheet : HomeDocumentsIntent
    data class FilterTemplates(val kind: OfficeKind?) : HomeDocumentsIntent
    data class CreateFromTemplate(val preset: DocTemplatePreset) : HomeDocumentsIntent
    data class EditDocument(val document: DocumentItem) : HomeDocumentsIntent
    data class ConvertDocumentToPdf(val document: DocumentItem) : HomeDocumentsIntent
}

sealed interface HomeDocumentsEffect : UiEffect {
    data class OpenDocument(val document: DocumentItem) : HomeDocumentsEffect
    data class OpenEditor(val document: DocumentItem) : HomeDocumentsEffect
    data class OpenConvertToPdf(val document: DocumentItem) : HomeDocumentsEffect
    data class OpenArchiveInspector(val document: DocumentItem) : HomeDocumentsEffect
    data class ShareDocumentUri(val document: DocumentItem) : HomeDocumentsEffect
    data class PinHomeShortcut(val document: DocumentItem) : HomeDocumentsEffect
    data object LaunchDefaultAppSettings : HomeDocumentsEffect
    data object LaunchSafPicker : HomeDocumentsEffect
    data class ShowMessage(val message: String) : HomeDocumentsEffect
}
