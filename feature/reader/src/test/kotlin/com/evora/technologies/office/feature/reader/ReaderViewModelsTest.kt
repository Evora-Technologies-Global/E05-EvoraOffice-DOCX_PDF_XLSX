package com.evora.technologies.office.feature.reader

import app.cash.turbine.test
import com.evora.technologies.office.core.domain.port.ArchiveEnginePort
import com.evora.technologies.office.core.domain.port.ArchiveInspectionResult
import com.evora.technologies.office.core.domain.port.DocumentInvalidPasswordException
import com.evora.technologies.office.core.domain.port.DocumentPasswordRequiredException
import com.evora.technologies.office.core.domain.port.DocumentRepository
import com.evora.technologies.office.core.domain.port.HwpEnginePort
import com.evora.technologies.office.core.domain.port.HwpRenderResult
import com.evora.technologies.office.core.domain.port.OfficeEnginePort
import com.evora.technologies.office.core.domain.port.ParsedTextDocument
import com.evora.technologies.office.core.domain.port.PdfDocumentInfo
import com.evora.technologies.office.core.domain.port.PdfEnginePort
import com.evora.technologies.office.core.domain.port.PdfPageRaster
import com.evora.technologies.office.core.domain.port.TextChapter
import com.evora.technologies.office.core.domain.port.TextEpubEnginePort
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.ExternalIntentPayload
import com.evora.technologies.office.core.model.OutlineNode
import com.evora.technologies.office.core.model.PdfSearchMatch
import com.evora.technologies.office.core.model.ReaderBookmarkState
import com.evora.technologies.office.core.model.ReaderTheme
import com.evora.technologies.office.core.model.SortOrder
import com.evora.technologies.office.core.model.TrashItem
import com.evora.technologies.office.feature.reader.archive.ArchiveInspectorIntent
import com.evora.technologies.office.feature.reader.archive.ArchiveInspectorViewModel
import com.evora.technologies.office.feature.reader.viewer.DocumentReaderEffect
import com.evora.technologies.office.feature.reader.viewer.DocumentReaderIntent
import com.evora.technologies.office.feature.reader.viewer.DocumentReaderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeReaderDocRepo : DocumentRepository {
    var savedBookmark: ReaderBookmarkState? = ReaderBookmarkState(
        uriString = "file:///docs/Contract.pdf",
        lastPageIndex = 3,
        totalPages = 10,
        scrollOffsetY = 42f,
        zoom = 1.5f,
        readerTheme = ReaderTheme.SEPIA,
        isContinuousScroll = false,
    )

    override fun observeLibraryDocuments(filter: DocFormat?, sortOrder: SortOrder): Flow<List<DocumentItem>> = emptyFlow()
    override fun observeRecentDocuments(): Flow<List<DocumentItem>> = emptyFlow()
    override fun observeFavoriteDocuments(): Flow<List<DocumentItem>> = emptyFlow()
    override fun observeTrashItems(): Flow<List<TrashItem>> = emptyFlow()
    override fun searchDocuments(query: String, filter: DocFormat?): Flow<List<DocumentItem>> = emptyFlow()
    override suspend fun refreshMediaStoreIndex(): Result<Int> = Result.success(0)
    override suspend fun getDocumentMeta(uriString: String): DocumentItem? = null
    override suspend fun getReaderBookmark(uriString: String): ReaderBookmarkState? =
        savedBookmark?.takeIf { it.uriString == uriString }
    override suspend fun toggleFavorite(uriString: String) = Unit
    override suspend fun recordDocumentOpened(item: DocumentItem, totalPages: Int) = Unit
    override suspend fun updateReadingPosition(
        uriString: String,
        pageIndex: Int,
        totalPages: Int,
        scrollOffsetY: Float,
        zoom: Float,
        readerTheme: ReaderTheme,
        isContinuousScroll: Boolean,
    ) {
        savedBookmark = ReaderBookmarkState(uriString, pageIndex, totalPages, scrollOffsetY, zoom, readerTheme, isContinuousScroll)
    }
    override suspend fun renameDocument(uriString: String, newBaseName: String): Result<DocumentItem> =
        Result.failure(UnsupportedOperationException())
    override suspend fun moveToTrash(uriString: String): Result<TrashItem> = Result.failure(UnsupportedOperationException())
    override suspend fun restoreFromTrash(trashId: String): Result<Unit> = Result.success(Unit)
    override suspend fun deleteForeverFromTrash(trashId: String): Result<Unit> = Result.success(Unit)
    override suspend fun emptyTrash(): Result<Int> = Result.success(0)
    override suspend fun copyDocumentToFolder(uriString: String, targetFolderName: String): Result<DocumentItem> =
        Result.failure(UnsupportedOperationException())
    override suspend fun moveDocumentToFolder(uriString: String, targetFolderName: String): Result<DocumentItem> =
        Result.failure(UnsupportedOperationException())
    override suspend fun importExternalUri(uriString: String, displayNameHint: String?, mimeTypeHint: String?): Result<ExternalIntentPayload> =
        Result.failure(UnsupportedOperationException())
    override suspend fun registerSafUri(uriString: String): Result<DocumentItem> = Result.failure(UnsupportedOperationException())
}

private class FakePdfEngine : PdfEnginePort {
    var expectedPassword: String? = null
    override suspend fun getPageCount(uriString: String, password: String?): Int = 10
    override suspend fun openDocumentInfo(uriString: String, password: String?): PdfDocumentInfo {
        if (expectedPassword != null) {
            if (password == null) throw DocumentPasswordRequiredException()
            if (password != expectedPassword) throw DocumentInvalidPasswordException("Wrong password — please try again")
        }
        return PdfDocumentInfo(
            pageCount = 10,
            outline = listOf(OutlineNode("Chapter 1", pageIndex = 1)),
        )
    }
    override suspend fun renderPage(uriString: String, pageIndex: Int, targetWidthPx: Int, password: String?): PdfPageRaster =
        PdfPageRaster(10, 10, IntArray(100))
    override suspend fun extractPageText(uriString: String, pageIndex: Int, password: String?): String =
        "Contract clause page ${pageIndex + 1}"
    override suspend fun searchText(uriString: String, query: String, password: String?): List<PdfSearchMatch> =
        listOf(PdfSearchMatch(pageIndex = 5, matchText = query, snippet = "...$query..."))
}

private class FakeHwpEngine : HwpEnginePort {
    override suspend fun renderToSanitizedHtml(uriString: String, password: String?): HwpRenderResult =
        HwpRenderResult("Hangul.hwp", "<p>Hello</p>", "Hello", 60L * 1024 * 1024, true)
}

private class FakeTextEpubEngine : TextEpubEnginePort {
    override suspend fun loadDocument(uriString: String, format: DocFormat): ParsedTextDocument =
        ParsedTextDocument("Notes.txt", listOf(TextChapter("Notes", "Hello text")), "Hello text")
}

private class FakeReaderOfficeEngine : OfficeEnginePort {
    override fun isNativeEngineAvailable(): Boolean = false
    override suspend fun extractReadablePreview(uriString: String, format: DocFormat): ParsedTextDocument =
        ParsedTextDocument("Doc.docx", listOf(TextChapter("Doc.docx", "Preview")), "Preview")
}

private class FakeArchiveEngine : ArchiveEnginePort {
    override suspend fun listEntries(uriString: String, password: String?): ArchiveInspectionResult =
        ArchiveInspectionResult("test.rar", "rar", false, emptyList(), "RAR unsupported")
    override suspend fun extractEntries(uriString: String, destinationDirPath: String, entryPaths: Set<String>?, password: String?): Result<List<String>> =
        Result.failure(UnsupportedOperationException())
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderViewModelsTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `reopening PDF restores saved page, zoom, theme, and scroll mode`() = runTest {
        val repo = FakeReaderDocRepo()
        val vm = DocumentReaderViewModel(repo, FakePdfEngine(), FakeHwpEngine(), FakeTextEpubEngine(), FakeReaderOfficeEngine())

        vm.effects.test {
            vm.onIntent(DocumentReaderIntent.OpenDocument("file:///docs/Contract.pdf", "Contract.pdf", DocFormat.PDF))
            advanceUntilIdle()

            assertEquals(3, vm.state.value.currentPage)
            assertEquals(1.5f, vm.state.value.zoom, 0.01f)
            assertEquals(ReaderTheme.SEPIA, vm.state.value.readerTheme)
            assertFalse(vm.state.value.isContinuousScroll)
            assertEquals(DocumentReaderEffect.ScrollToPage(3), awaitItem())
        }

        vm.onIntent(DocumentReaderIntent.SelectReaderTheme(ReaderTheme.DARK))
        advanceTimeBy(300L)
        advanceUntilIdle()
        assertEquals(ReaderTheme.DARK, repo.savedBookmark?.readerTheme)
    }

    @Test
    fun `encrypted PDF prompts password, displays wrong-password inline error, then unlocks cleanly`() = runTest {
        val pdf = FakePdfEngine().apply { expectedPassword = "secret" }
        val vm = DocumentReaderViewModel(FakeReaderDocRepo(), pdf, FakeHwpEngine(), FakeTextEpubEngine(), FakeReaderOfficeEngine())

        vm.onIntent(DocumentReaderIntent.OpenDocument("file:///docs/Locked.pdf", "Locked.pdf", DocFormat.PDF))
        advanceUntilIdle()
        assertTrue(vm.state.value.passwordRequired)
        assertNull(vm.state.value.passwordError)

        vm.onIntent(DocumentReaderIntent.SubmitPassword("wrong"))
        advanceUntilIdle()
        assertTrue(vm.state.value.passwordRequired)
        assertEquals("Wrong password — please try again", vm.state.value.passwordError)

        vm.onIntent(DocumentReaderIntent.SubmitPassword("secret"))
        advanceUntilIdle()
        assertFalse(vm.state.value.passwordRequired)
        assertNull(vm.state.value.passwordError)
        assertEquals(10, vm.state.value.totalPages)
    }

    @Test
    fun `TTS toggle emits SpeakTts and large HWP triggers warning banner`() = runTest {
        val vm = DocumentReaderViewModel(FakeReaderDocRepo(), FakePdfEngine(), FakeHwpEngine(), FakeTextEpubEngine(), FakeReaderOfficeEngine())
        vm.onIntent(DocumentReaderIntent.OpenDocument("file:///docs/Large.hwp", "Large.hwp", DocFormat.HWP))
        advanceUntilIdle()

        assertTrue(vm.state.value.isLargeHwpWarning)
        vm.effects.test {
            vm.onIntent(DocumentReaderIntent.ToggleTts)
            advanceUntilIdle()
            assertTrue(vm.state.value.isTtsPlaying)
            assertEquals(DocumentReaderEffect.SpeakTts("Hello"), awaitItem())
        }
    }

    @Test
    fun `archive inspector exposes honest unsupported state for rar`() = runTest {
        val vm = ArchiveInspectorViewModel(FakeArchiveEngine())
        vm.onIntent(ArchiveInspectorIntent.LoadArchive("file:///docs/test.rar", "test.rar"))
        advanceUntilIdle()
        assertFalse(vm.state.value.isSupported)
        assertEquals("RAR unsupported", vm.state.value.unsupportedReason)
    }
}
