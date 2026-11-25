package com.evora.technologies.office.feature.reader.archive

import com.evora.technologies.office.core.model.ArchiveEntryItem
import com.evora.technologies.office.core.mvi.UiEffect
import com.evora.technologies.office.core.mvi.UiIntent
import com.evora.technologies.office.core.mvi.UiState

data class ArchiveInspectorState(
    val uriString: String = "",
    val archiveName: String = "",
    val extension: String = "zip",
    val isLoading: Boolean = true,
    val isSupported: Boolean = true,
    val unsupportedReason: String? = null,
    val entries: List<ArchiveEntryItem> = emptyList(),
    val selectedEntryPaths: Set<String> = emptySet(),
    val passwordRequired: Boolean = false,
    val passwordError: String? = null,
    val isExtracting: Boolean = false,
    val extractedPaths: List<String> = emptyList(),
    val errorMessage: String? = null,
) : UiState

sealed interface ArchiveInspectorIntent : UiIntent {
    data class LoadArchive(val uriString: String, val displayName: String) : ArchiveInspectorIntent
    data class SubmitPassword(val password: String) : ArchiveInspectorIntent
    data object DismissPasswordDialog : ArchiveInspectorIntent
    data class ToggleEntrySelection(val path: String) : ArchiveInspectorIntent
    data class ExtractEntries(val destinationDirPath: String, val extractAll: Boolean) : ArchiveInspectorIntent
    data class OpenExtractedFile(val path: String) : ArchiveInspectorIntent
    data object BackClicked : ArchiveInspectorIntent
}

sealed interface ArchiveInspectorEffect : UiEffect {
    data object NavigateBack : ArchiveInspectorEffect
    data class OpenExtractedDocument(val fileUri: String, val displayName: String) : ArchiveInspectorEffect
    data class ShowMessage(val message: String) : ArchiveInspectorEffect
}
