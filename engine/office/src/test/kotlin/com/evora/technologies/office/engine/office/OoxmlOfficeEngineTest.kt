package com.evora.technologies.office.engine.office

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.evora.technologies.office.core.domain.port.DocumentRepository
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.ExcelCell
import com.evora.technologies.office.core.model.ExternalIntentPayload
import com.evora.technologies.office.core.model.ReaderBookmarkState
import com.evora.technologies.office.core.model.ReaderTheme
import com.evora.technologies.office.core.model.SlideDeckSnapshot
import com.evora.technologies.office.core.model.SlideElement
import com.evora.technologies.office.core.model.SlidePageData
import com.evora.technologies.office.core.model.SortOrder
import com.evora.technologies.office.core.model.TrackChangeItem
import com.evora.technologies.office.core.model.TrackChangeKind
import com.evora.technologies.office.core.model.TrashItem
import com.evora.technologies.office.core.model.WordDocumentSnapshot
import com.evora.technologies.office.core.model.WordParagraph
import com.evora.technologies.office.core.model.WorkbookSnapshot
import com.evora.technologies.office.core.model.WorksheetData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

private class StubOfficeDocRepo : DocumentRepository {
    override fun observeLibraryDocuments(filter: DocFormat?, sortOrder: SortOrder): Flow<List<DocumentItem>> = emptyFlow()
    override fun observeRecentDocuments(): Flow<List<DocumentItem>> = emptyFlow()
    override fun observeFavoriteDocuments(): Flow<List<DocumentItem>> = emptyFlow()
    override fun observeTrashItems(): Flow<List<TrashItem>> = emptyFlow()
    override fun searchDocuments(query: String, filter: DocFormat?): Flow<List<DocumentItem>> = emptyFlow()
    override suspend fun refreshMediaStoreIndex(): Result<Int> = Result.success(0)
    override suspend fun getDocumentMeta(uriString: String): DocumentItem? = null
    override suspend fun getReaderBookmark(uriString: String): ReaderBookmarkState? = null
    override suspend fun toggleFavorite(uriString: String) = Unit
    override suspend fun recordDocumentOpened(item: DocumentItem, totalPages: Int) = Unit
    override suspend fun updateReadingPosition(uriString: String, pageIndex: Int, totalPages: Int, scrollOffsetY: Float, zoom: Float, readerTheme: ReaderTheme, isContinuousScroll: Boolean) = Unit
    override suspend fun renameDocument(uriString: String, newBaseName: String): Result<DocumentItem> = Result.failure(UnsupportedOperationException())
    override suspend fun moveToTrash(uriString: String): Result<TrashItem> = Result.failure(UnsupportedOperationException())
    override suspend fun restoreFromTrash(trashId: String): Result<Unit> = Result.failure(UnsupportedOperationException())
    override suspend fun deleteForeverFromTrash(trashId: String): Result<Unit> = Result.failure(UnsupportedOperationException())
    override suspend fun emptyTrash(): Result<Int> = Result.success(0)
    override suspend fun copyDocumentToFolder(uriString: String, targetFolderName: String): Result<DocumentItem> = Result.failure(UnsupportedOperationException())
    override suspend fun moveDocumentToFolder(uriString: String, targetFolderName: String): Result<DocumentItem> = Result.failure(UnsupportedOperationException())
    override suspend fun importExternalUri(uriString: String, displayNameHint: String?, mimeTypeHint: String?): Result<ExternalIntentPayload> = Result.failure(UnsupportedOperationException())
    override suspend fun registerSafUri(uriString: String): Result<DocumentItem> =
        Result.success(DocumentItem(uriString, uriString, "saved", "docx", DocFormat.WORD, 100L, 1L, "Docs"))
}

@RunWith(RobolectricTestRunner::class)
class OoxmlOfficeEngineTest {
    @Test
    fun `word track changes, excel formulas, and ppt notes roundtrip cleanly`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = OoxmlOfficeEngine(context, StubOfficeDocRepo(), Dispatchers.IO)

        val docxFile = File(context.filesDir, "roundtrip.docx")
        val wordSnap = WordDocumentSnapshot(
            title = "roundtrip.docx",
            paragraphs = listOf(WordParagraph("p0", "Hello Track Changes", bold = true)),
            trackChangesEnabled = true,
            trackChanges = listOf(
                TrackChangeItem("1", TrackChangeKind.INSERTION, 0, "added", timestampEpochMs = 1_700_000_000_000L),
                TrackChangeItem("2", TrackChangeKind.COMMENT, 0, "Needs signoff", timestampEpochMs = 1_700_000_000_000L),
            ),
        )
        engine.saveWordDocument(docxFile.toURI().toString(), wordSnap).getOrThrow()
        val loadedWord = engine.loadWordDocument(docxFile.toURI().toString()).getOrThrow()
        assertTrue(loadedWord.trackChangesEnabled)
        assertTrue(loadedWord.paragraphs.first().text.contains("Hello Track Changes"))
        assertTrue(loadedWord.trackChanges.any { it.kind == TrackChangeKind.INSERTION })

        val xlsxFile = File(context.filesDir, "roundtrip.xlsx")
        val wb = WorkbookSnapshot(
            title = "roundtrip.xlsx",
            sheets = listOf(
                WorksheetData(
                    name = "Sales",
                    cells = mapOf(
                        "0,0" to ExcelCell(0, 0, rawInput = "12"),
                        "1,0" to ExcelCell(1, 0, rawInput = "8"),
                        "2,0" to ExcelCell(2, 0, rawInput = "=SUM(A1:A2)"),
                    ),
                ),
            ),
        )
        engine.saveWorkbook(xlsxFile.toURI().toString(), wb).getOrThrow()
        val loadedWb = engine.loadWorkbook(xlsxFile.toURI().toString()).getOrThrow()
        assertEquals("Sales", loadedWb.sheets.first().name)
        assertEquals("20", loadedWb.sheets.first().cells["2,0"]?.evaluatedDisplay)

        val pptxFile = File(context.filesDir, "roundtrip.pptx")
        val deck = SlideDeckSnapshot(
            title = "roundtrip.pptx",
            slides = listOf(
                SlidePageData(
                    id = "s0",
                    elements = listOf(SlideElement("e0", "TEXT", "Quarterly Review")),
                    presenterNotes = "Speak slowly on Q4 growth",
                ),
            ),
        )
        engine.saveSlideDeck(pptxFile.toURI().toString(), deck).getOrThrow()
        val loadedDeck = engine.loadSlideDeck(pptxFile.toURI().toString()).getOrThrow()
        assertEquals("Quarterly Review", loadedDeck.slides.first().elements.first().text)
        assertEquals("Speak slowly on Q4 growth", loadedDeck.slides.first().presenterNotes)

        val draftDecoded = engine.decodeWordDraft(engine.encodeWordDraft(wordSnap))
        assertEquals(wordSnap.title, draftDecoded.title)
        assertEquals(wordSnap.trackChanges.size, draftDecoded.trackChanges.size)
    }
}
