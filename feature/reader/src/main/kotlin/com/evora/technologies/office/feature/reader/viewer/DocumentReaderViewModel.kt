package com.evora.technologies.office.feature.reader.viewer

import androidx.lifecycle.viewModelScope
import com.evora.technologies.office.core.domain.port.DocumentInvalidPasswordException
import com.evora.technologies.office.core.domain.port.DocumentPasswordRequiredException
import com.evora.technologies.office.core.domain.port.DocumentRepository
import com.evora.technologies.office.core.domain.port.HwpEnginePort
import com.evora.technologies.office.core.domain.port.OfficeEnginePort
import com.evora.technologies.office.core.domain.port.PdfEnginePort
import com.evora.technologies.office.core.domain.port.PdfPageRaster
import com.evora.technologies.office.core.domain.port.TextEpubEnginePort
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.ReaderTheme
import com.evora.technologies.office.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class DocumentReaderViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val pdfEngine: PdfEnginePort,
    private val hwpEngine: HwpEnginePort,
    private val textEpubEngine: TextEpubEnginePort,
    private val officeEngine: OfficeEnginePort,
) : MviViewModel<DocumentReaderState, DocumentReaderIntent, DocumentReaderEffect>(
    DocumentReaderState(),
) {
    private var activePassword: String? = null
    private var loadedDocumentKey: String? = null
    private var loadJob: Job? = null
    private var persistJob: Job? = null
    private var autoScrollJob: Job? = null
    private val renderingPages = HashSet<Int>()

    override fun onIntent(intent: DocumentReaderIntent) {
        when (intent) {
            is DocumentReaderIntent.OpenDocument -> loadDocument(
                uriString = intent.uriString,
                displayName = intent.displayName,
                format = intent.format,
                password = null,
            )
            is DocumentReaderIntent.SubmitPassword -> {
                activePassword = intent.password
                loadDocument(
                    uriString = currentState.uriString,
                    displayName = currentState.displayName,
                    format = currentState.format,
                    password = intent.password,
                )
            }
            DocumentReaderIntent.DismissPasswordDialog -> {
                setState { copy(passwordRequired = false, passwordError = null) }
                sendEffect(DocumentReaderEffect.NavigateBack)
            }
            is DocumentReaderIntent.PageChanged -> handlePageChanged(intent.pageIndex, intent.scrollOffsetY)
            is DocumentReaderIntent.JumpToPage -> jumpToPage(intent.pageIndex)
            is DocumentReaderIntent.ZoomChanged -> {
                val bounded = intent.zoom.coerceIn(0.75f, 4.0f)
                setState { copy(zoom = bounded) }
                schedulePersistReadingPosition()
            }
            is DocumentReaderIntent.RequestPageRender -> renderPdfPageIfNeeded(intent.pageIndex, intent.targetWidthPx)
            DocumentReaderIntent.ToggleContinuousScroll -> {
                setState { copy(isContinuousScroll = !isContinuousScroll) }
                schedulePersistReadingPosition()
            }
            DocumentReaderIntent.ToggleReflowMode -> handleToggleReflow()
            is DocumentReaderIntent.SelectReaderTheme -> {
                setState { copy(readerTheme = intent.theme) }
                schedulePersistReadingPosition()
            }
            DocumentReaderIntent.ToggleOrientation -> {
                val next = !currentState.isLandscapePreferred
                setState { copy(isLandscapePreferred = next) }
                sendEffect(DocumentReaderEffect.ApplyOrientation(next))
            }
            DocumentReaderIntent.ToggleFullscreen -> setState { copy(isFullscreen = !isFullscreen) }
            DocumentReaderIntent.ToggleAutoScroll -> toggleAutoScroll()
            is DocumentReaderIntent.SetAutoScrollSpeed -> setState {
                copy(autoScrollSpeed = intent.speed.coerceIn(1, 3))
            }
            DocumentReaderIntent.ToggleTts -> toggleTts()
            DocumentReaderIntent.TtsPlaybackFinished -> setState { copy(isTtsPlaying = false) }
            DocumentReaderIntent.OpenOutlineDrawer -> setState { copy(showOutlineDrawer = true) }
            DocumentReaderIntent.DismissOutlineDrawer -> setState { copy(showOutlineDrawer = false) }
            is DocumentReaderIntent.SelectOutlineNode -> {
                setState { copy(showOutlineDrawer = false) }
                jumpToPage(intent.pageIndex)
            }
            DocumentReaderIntent.OpenReadingSettings -> setState { copy(showReadingSettingsSheet = true) }
            DocumentReaderIntent.DismissReadingSettings -> setState { copy(showReadingSettingsSheet = false) }
            DocumentReaderIntent.ToggleSearchBar -> setState {
                copy(
                    showSearchBar = !showSearchBar,
                    searchMatches = if (showSearchBar) emptyList() else searchMatches,
                    activeMatchIndex = 0,
                )
            }
            is DocumentReaderIntent.SearchQueryChanged -> setState { copy(searchQuery = intent.query) }
            DocumentReaderIntent.SubmitInDocSearch -> runInDocSearch()
            DocumentReaderIntent.NextSearchMatch -> stepSearchMatch(+1)
            DocumentReaderIntent.PrevSearchMatch -> stepSearchMatch(-1)
            DocumentReaderIntent.PrintClicked -> triggerPrint()
            DocumentReaderIntent.EditClicked -> {
                stopAllPlayback()
                sendEffect(
                    DocumentReaderEffect.NavigateToEditor(
                        uriString = currentState.uriString,
                        displayName = currentState.displayName,
                        format = currentState.format,
                    ),
                )
            }
            DocumentReaderIntent.BackClicked -> {
                stopAllPlayback()
                sendEffect(DocumentReaderEffect.NavigateBack)
            }
        }
    }

    private fun loadDocument(
        uriString: String,
        displayName: String,
        format: DocFormat,
        password: String?,
    ) {
        val docKey = "$uriString|$format"
        if (password == null &&
            loadedDocumentKey == docKey &&
            !currentState.isLoading &&
            currentState.errorMessage == null &&
            !currentState.passwordRequired
        ) {
            return
        }
        loadedDocumentKey = docKey
        loadJob?.cancel()
        persistJob?.cancel()
        autoScrollJob?.cancel()
        renderingPages.clear()
        val mode = when (format) {
            DocFormat.PDF -> ReaderEngineMode.PDF
            DocFormat.HWP -> ReaderEngineMode.HWP_HTML
            DocFormat.EPUB, DocFormat.TEXT -> ReaderEngineMode.TEXT_EPUB
            DocFormat.WORD, DocFormat.EXCEL, DocFormat.PPT, DocFormat.ODF -> ReaderEngineMode.OFFICE_PREVIEW
            DocFormat.ARCHIVE -> ReaderEngineMode.TEXT_EPUB
        }

        loadJob = launchSafely(
            onError = { err ->
                when (err) {
                    is DocumentPasswordRequiredException -> setState {
                        copy(
                            isLoading = false,
                            isUnlocking = false,
                            passwordRequired = true,
                            passwordError = null,
                        )
                    }
                    is DocumentInvalidPasswordException -> setState {
                        copy(
                            isLoading = false,
                            isUnlocking = false,
                            passwordRequired = true,
                            passwordError = err.message ?: "Wrong password — please try again",
                        )
                    }
                    else -> setState {
                        copy(
                            isLoading = false,
                            isUnlocking = false,
                            errorMessage = err.message ?: "Unable to open $displayName",
                        )
                    }
                }
            },
        ) {
            setState {
                copy(
                    uriString = uriString,
                    displayName = displayName,
                    format = format,
                    engineMode = mode,
                    isLoading = password == null,
                    isUnlocking = password != null,
                    errorMessage = null,
                    passwordError = null,
                    renderedPages = emptyMap(),
                )
            }

            val bookmark = documentRepository.getReaderBookmark(uriString)
            val savedTheme = bookmark?.readerTheme ?: ReaderTheme.LIGHT
            val savedContinuous = bookmark?.isContinuousScroll ?: true
            val savedZoom = bookmark?.zoom ?: 1f

            when (mode) {
                ReaderEngineMode.PDF -> {
                    val info = pdfEngine.openDocumentInfo(uriString, password)
                    val restoredPage = (bookmark?.lastPageIndex ?: 0)
                        .coerceIn(0, (info.pageCount - 1).coerceAtLeast(0))
                    val restoredScroll = bookmark?.scrollOffsetY ?: 0f

                    setState {
                        copy(
                            isLoading = false,
                            isUnlocking = false,
                            passwordRequired = false,
                            passwordError = null,
                            totalPages = info.pageCount,
                            currentPage = restoredPage,
                            scrollOffsetY = restoredScroll,
                            zoom = savedZoom,
                            readerTheme = savedTheme,
                            isContinuousScroll = savedContinuous,
                            outline = info.outline,
                        )
                    }
                    recordOpened(uriString, displayName, format, info.pageCount)
                    if (restoredPage > 0) {
                        sendEffect(DocumentReaderEffect.ScrollToPage(restoredPage))
                    }
                }

                ReaderEngineMode.HWP_HTML -> {
                    val result = hwpEngine.renderToSanitizedHtml(uriString, password)
                    setState {
                        copy(
                            isLoading = false,
                            isUnlocking = false,
                            passwordRequired = false,
                            passwordError = null,
                            hwpSanitizedHtml = result.sanitizedHtml,
                            fullPlainText = result.plainText,
                            extractedCurrentPageText = result.plainText,
                            isLargeHwpWarning = result.isLargeFileWarning,
                            totalPages = 1,
                            currentPage = 0,
                            readerTheme = savedTheme,
                        )
                    }
                    recordOpened(uriString, displayName, format, 1)
                }

                ReaderEngineMode.TEXT_EPUB -> {
                    val parsed = textEpubEngine.loadDocument(uriString, format)
                    val restoredPage = (bookmark?.lastPageIndex ?: 0)
                        .coerceIn(0, (parsed.chapters.size - 1).coerceAtLeast(0))
                    setState {
                        copy(
                            isLoading = false,
                            isUnlocking = false,
                            textChapters = parsed.chapters,
                            fullPlainText = parsed.fullPlainText,
                            extractedCurrentPageText = parsed.chapters.getOrNull(restoredPage)?.body.orEmpty(),
                            totalPages = parsed.chapters.size.coerceAtLeast(1),
                            currentPage = restoredPage,
                            readerTheme = savedTheme,
                            isContinuousScroll = savedContinuous,
                        )
                    }
                    recordOpened(uriString, displayName, format, parsed.chapters.size.coerceAtLeast(1))
                    if (restoredPage > 0) {
                        sendEffect(DocumentReaderEffect.ScrollToPage(restoredPage))
                    }
                }

                ReaderEngineMode.OFFICE_PREVIEW -> {
                    val parsed = officeEngine.extractReadablePreview(uriString, format)
                    setState {
                        copy(
                            isLoading = false,
                            isUnlocking = false,
                            textChapters = parsed.chapters,
                            fullPlainText = parsed.fullPlainText,
                            extractedCurrentPageText = parsed.fullPlainText,
                            totalPages = parsed.chapters.size.coerceAtLeast(1),
                            currentPage = 0,
                            readerTheme = savedTheme,
                        )
                    }
                    recordOpened(uriString, displayName, format, parsed.chapters.size.coerceAtLeast(1))
                }
            }
        }
    }

    private suspend fun recordOpened(
        uriString: String,
        displayName: String,
        format: DocFormat,
        totalPages: Int,
    ) {
        val existing = documentRepository.getDocumentMeta(uriString)
            ?: DocumentItem(
                id = uriString,
                uriString = uriString,
                displayName = displayName,
                extension = displayName.substringAfterLast('.', "").lowercase(),
                format = format,
                sizeBytes = 0L,
                modifiedAtEpochMs = System.currentTimeMillis(),
                parentFolderName = "Device",
            )
        documentRepository.recordDocumentOpened(existing, totalPages)
    }

    private fun handlePageChanged(pageIndex: Int, scrollOffsetY: Float) {
        val bounded = pageIndex.coerceIn(0, (currentState.totalPages - 1).coerceAtLeast(0))
        if (bounded == currentState.currentPage && scrollOffsetY == currentState.scrollOffsetY) return
        setState {
            val chapterText = if (textChapters.isNotEmpty()) {
                textChapters.getOrNull(bounded)?.body.orEmpty()
            } else {
                extractedCurrentPageText
            }
            copy(
                currentPage = bounded,
                scrollOffsetY = scrollOffsetY,
                extractedCurrentPageText = chapterText,
            )
        }
        schedulePersistReadingPosition()
    }

    private fun jumpToPage(pageIndex: Int) {
        val bounded = pageIndex.coerceIn(0, (currentState.totalPages - 1).coerceAtLeast(0))
        handlePageChanged(bounded, 0f)
        sendEffect(DocumentReaderEffect.ScrollToPage(bounded))
    }

    private fun renderPdfPageIfNeeded(pageIndex: Int, targetWidthPx: Int) {
        if (currentState.engineMode != ReaderEngineMode.PDF) return
        if (pageIndex < 0 || pageIndex >= currentState.totalPages) return
        if (pageIndex in currentState.renderedPages || pageIndex in renderingPages) return

        renderingPages.add(pageIndex)
        launchSafely(
            onError = { renderingPages.remove(pageIndex) },
        ) {
            try {
                val raster = pdfEngine.renderPage(
                    uriString = currentState.uriString,
                    pageIndex = pageIndex,
                    targetWidthPx = targetWidthPx,
                    password = activePassword,
                )
                setState {
                    val updated = LinkedHashMap<Int, PdfPageRaster>(renderedPages)
                    updated[pageIndex] = raster
                    // Keep LRU window bounded around current page to protect low-RAM devices
                    if (updated.size > 7) {
                        val farthest = updated.keys.maxByOrNull { kotlin.math.abs(it - currentPage) }
                        if (farthest != null && farthest != pageIndex) updated.remove(farthest)
                    }
                    copy(renderedPages = updated)
                }
            } finally {
                renderingPages.remove(pageIndex)
            }
        }
    }

    private fun handleToggleReflow() {
        val enabling = !currentState.isReflowMode
        setState { copy(isReflowMode = enabling) }
        if (enabling && currentState.engineMode == ReaderEngineMode.PDF) {
            launchSafely {
                val text = pdfEngine.extractPageText(
                    uriString = currentState.uriString,
                    pageIndex = currentState.currentPage,
                    password = activePassword,
                )
                setState { copy(extractedCurrentPageText = text.ifBlank { "No selectable text found on this page." }) }
            }
        }
    }

    private fun toggleAutoScroll() {
        if (currentState.isAutoScrollPlaying) {
            autoScrollJob?.cancel()
            autoScrollJob = null
            setState { copy(isAutoScrollPlaying = false) }
            return
        }
        setState { copy(isAutoScrollPlaying = true) }
        autoScrollJob = launchSafely(
            onError = { setState { copy(isAutoScrollPlaying = false) } },
        ) {
            while (currentState.isAutoScrollPlaying) {
                val intervalMs = when (currentState.autoScrollSpeed) {
                    3 -> 2500L
                    2 -> 4500L
                    else -> 7000L
                }
                delay(intervalMs)
                if (currentState.currentPage + 1 < currentState.totalPages) {
                    jumpToPage(currentState.currentPage + 1)
                } else {
                    setState { copy(isAutoScrollPlaying = false) }
                }
            }
        }
    }

    private fun toggleTts() {
        if (currentState.isTtsPlaying) {
            setState { copy(isTtsPlaying = false) }
            sendEffect(DocumentReaderEffect.StopTts)
            return
        }
        launchSafely(
            onError = { err ->
                setState { copy(isTtsPlaying = false) }
                sendEffect(DocumentReaderEffect.ShowMessage(err.message ?: "Unable to read text aloud"))
            },
        ) {
            val textToSpeak = when (currentState.engineMode) {
                ReaderEngineMode.PDF -> pdfEngine.extractPageText(
                    uriString = currentState.uriString,
                    pageIndex = currentState.currentPage,
                    password = activePassword,
                )
                ReaderEngineMode.HWP_HTML -> currentState.fullPlainText
                ReaderEngineMode.TEXT_EPUB, ReaderEngineMode.OFFICE_PREVIEW -> {
                    currentState.extractedCurrentPageText.ifBlank { currentState.fullPlainText }
                }
            }.trim()

            if (textToSpeak.isEmpty()) {
                sendEffect(DocumentReaderEffect.ShowMessage("No readable text available for Read-Aloud"))
                return@launchSafely
            }
            setState { copy(isTtsPlaying = true, extractedCurrentPageText = textToSpeak) }
            sendEffect(DocumentReaderEffect.SpeakTts(textToSpeak.take(3500)))
        }
    }

    private fun runInDocSearch() {
        val needle = currentState.searchQuery.trim()
        if (needle.isEmpty()) {
            setState { copy(searchMatches = emptyList(), activeMatchIndex = 0) }
            return
        }
        launchSafely(
            onError = { setState { copy(isSearchingInDoc = false) } },
        ) {
            setState { copy(isSearchingInDoc = true) }
            val matches = when (currentState.engineMode) {
                ReaderEngineMode.PDF -> pdfEngine.searchText(currentState.uriString, needle, activePassword)
                ReaderEngineMode.HWP_HTML -> {
                    if (currentState.fullPlainText.contains(needle, ignoreCase = true)) {
                        listOf(com.evora.technologies.office.core.model.PdfSearchMatch(0, needle, needle))
                    } else {
                        emptyList()
                    }
                }
                ReaderEngineMode.TEXT_EPUB, ReaderEngineMode.OFFICE_PREVIEW -> {
                    currentState.textChapters.mapIndexedNotNull { idx, chapter ->
                        if (chapter.body.contains(needle, ignoreCase = true)) {
                            com.evora.technologies.office.core.model.PdfSearchMatch(idx, needle, chapter.title)
                        } else {
                            null
                        }
                    }
                }
            }
            setState { copy(isSearchingInDoc = false, searchMatches = matches, activeMatchIndex = 0) }
            if (matches.isNotEmpty()) {
                jumpToPage(matches.first().pageIndex)
            } else {
                sendEffect(DocumentReaderEffect.ShowMessage("No matches for \"$needle\""))
            }
        }
    }

    private fun stepSearchMatch(delta: Int) {
        val matches = currentState.searchMatches
        if (matches.isEmpty()) return
        val next = (currentState.activeMatchIndex + delta + matches.size) % matches.size
        setState { copy(activeMatchIndex = next) }
        jumpToPage(matches[next].pageIndex)
    }

    private fun triggerPrint() {
        val isHtml = currentState.engineMode == ReaderEngineMode.HWP_HTML &&
            !currentState.hwpSanitizedHtml.isNullOrBlank()
        val payload = if (isHtml) {
            currentState.hwpSanitizedHtml!!
        } else {
            currentState.fullPlainText.ifBlank { currentState.extractedCurrentPageText }
        }
        sendEffect(
            DocumentReaderEffect.LaunchSystemPrint(
                title = currentState.displayName,
                content = payload,
                isHtml = isHtml,
            ),
        )
    }

    private fun schedulePersistReadingPosition() {
        persistJob?.cancel()
        persistJob = launchSafely {
            delay(250L)
            documentRepository.updateReadingPosition(
                uriString = currentState.uriString,
                pageIndex = currentState.currentPage,
                totalPages = currentState.totalPages,
                scrollOffsetY = currentState.scrollOffsetY,
                zoom = currentState.zoom,
                readerTheme = currentState.readerTheme,
                isContinuousScroll = currentState.isContinuousScroll,
            )
        }
    }

    private fun stopAllPlayback() {
        autoScrollJob?.cancel()
        if (currentState.isTtsPlaying) {
            sendEffect(DocumentReaderEffect.StopTts)
        }
    }
}
