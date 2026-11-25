package com.evora.technologies.office.feature.reader.viewer

import com.evora.technologies.office.core.domain.port.PdfPageRaster
import com.evora.technologies.office.core.domain.port.TextChapter
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.OutlineNode
import com.evora.technologies.office.core.model.PdfSearchMatch
import com.evora.technologies.office.core.model.ReaderTheme
import com.evora.technologies.office.core.mvi.UiEffect
import com.evora.technologies.office.core.mvi.UiIntent
import com.evora.technologies.office.core.mvi.UiState

enum class ReaderEngineMode {
    PDF,
    HWP_HTML,
    TEXT_EPUB,
    OFFICE_PREVIEW,
}

data class DocumentReaderState(
    val uriString: String = "",
    val displayName: String = "",
    val format: DocFormat = DocFormat.PDF,
    val engineMode: ReaderEngineMode = ReaderEngineMode.PDF,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val passwordRequired: Boolean = false,
    val passwordError: String? = null,
    val isUnlocking: Boolean = false,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val scrollOffsetY: Float = 0f,
    val zoom: Float = 1f,
    val isContinuousScroll: Boolean = true,
    val isLandscapePreferred: Boolean = false,
    val readerTheme: ReaderTheme = ReaderTheme.LIGHT,
    val isReflowMode: Boolean = false,
    val isAutoScrollPlaying: Boolean = false,
    val autoScrollSpeed: Int = 1,
    val isTtsPlaying: Boolean = false,
    val isFullscreen: Boolean = false,
    val isLargeHwpWarning: Boolean = false,
    val hwpSanitizedHtml: String? = null,
    val textChapters: List<TextChapter> = emptyList(),
    val fullPlainText: String = "",
    val extractedCurrentPageText: String = "",
    val renderedPages: Map<Int, PdfPageRaster> = emptyMap(),
    val outline: List<OutlineNode> = emptyList(),
    val showOutlineDrawer: Boolean = false,
    val showReadingSettingsSheet: Boolean = false,
    val showSearchBar: Boolean = false,
    val searchQuery: String = "",
    val isSearchingInDoc: Boolean = false,
    val searchMatches: List<PdfSearchMatch> = emptyList(),
    val activeMatchIndex: Int = 0,
) : UiState

sealed interface DocumentReaderIntent : UiIntent {
    data class OpenDocument(
        val uriString: String,
        val displayName: String,
        val format: DocFormat,
    ) : DocumentReaderIntent

    data class SubmitPassword(val password: String) : DocumentReaderIntent
    data object DismissPasswordDialog : DocumentReaderIntent

    data class PageChanged(val pageIndex: Int, val scrollOffsetY: Float = 0f) : DocumentReaderIntent
    data class JumpToPage(val pageIndex: Int) : DocumentReaderIntent
    data class ZoomChanged(val zoom: Float) : DocumentReaderIntent
    data class RequestPageRender(val pageIndex: Int, val targetWidthPx: Int) : DocumentReaderIntent

    data object ToggleContinuousScroll : DocumentReaderIntent
    data object ToggleReflowMode : DocumentReaderIntent
    data class SelectReaderTheme(val theme: ReaderTheme) : DocumentReaderIntent
    data object ToggleOrientation : DocumentReaderIntent
    data object ToggleFullscreen : DocumentReaderIntent

    data object ToggleAutoScroll : DocumentReaderIntent
    data class SetAutoScrollSpeed(val speed: Int) : DocumentReaderIntent
    data object ToggleTts : DocumentReaderIntent
    data object TtsPlaybackFinished : DocumentReaderIntent

    data object OpenOutlineDrawer : DocumentReaderIntent
    data object DismissOutlineDrawer : DocumentReaderIntent
    data class SelectOutlineNode(val pageIndex: Int) : DocumentReaderIntent

    data object OpenReadingSettings : DocumentReaderIntent
    data object DismissReadingSettings : DocumentReaderIntent

    data object ToggleSearchBar : DocumentReaderIntent
    data class SearchQueryChanged(val query: String) : DocumentReaderIntent
    data object SubmitInDocSearch : DocumentReaderIntent
    data object NextSearchMatch : DocumentReaderIntent
    data object PrevSearchMatch : DocumentReaderIntent

    data object PrintClicked : DocumentReaderIntent
    data object BackClicked : DocumentReaderIntent
}

sealed interface DocumentReaderEffect : UiEffect {
    data class ScrollToPage(val pageIndex: Int) : DocumentReaderEffect
    data class SpeakTts(val text: String) : DocumentReaderEffect
    data object StopTts : DocumentReaderEffect
    data class ApplyOrientation(val landscape: Boolean) : DocumentReaderEffect
    data class LaunchSystemPrint(val title: String, val content: String, val isHtml: Boolean) : DocumentReaderEffect
    data object NavigateBack : DocumentReaderEffect
    data class ShowMessage(val message: String) : DocumentReaderEffect
}
