package com.evora.technologies.office.engine.pdf

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.evora.technologies.office.core.domain.port.DocumentPasswordRequiredException
import com.evora.technologies.office.core.domain.port.DocumentRepository
import com.evora.technologies.office.core.domain.port.HwpEnginePort
import com.evora.technologies.office.core.domain.port.HwpRenderResult
import com.evora.technologies.office.core.domain.port.OcrExtractionResult
import com.evora.technologies.office.core.domain.port.OfficeEnginePort
import com.evora.technologies.office.core.domain.port.ParsedTextDocument
import com.evora.technologies.office.core.domain.port.PdfDocumentInfo
import com.evora.technologies.office.core.domain.port.PdfEnginePort
import com.evora.technologies.office.core.domain.port.PdfPageRaster
import com.evora.technologies.office.core.domain.port.ScannerPort
import com.evora.technologies.office.core.domain.port.TextEpubEnginePort
import com.evora.technologies.office.core.model.CompressQuality
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.ExternalIntentPayload
import com.evora.technologies.office.core.model.OcrScript
import com.evora.technologies.office.core.model.PageSizePreset
import com.evora.technologies.office.core.model.PdfSearchMatch
import com.evora.technologies.office.core.model.PdfToolKind
import com.evora.technologies.office.core.model.PdfToolResult
import com.evora.technologies.office.core.model.QrDecodedAction
import com.evora.technologies.office.core.model.ReaderBookmarkState
import com.evora.technologies.office.core.model.ReaderTheme
import com.evora.technologies.office.core.model.ScanFilter
import com.evora.technologies.office.core.model.SortOrder
import com.evora.technologies.office.core.model.SplitMode
import com.evora.technologies.office.core.model.TrashItem
import com.evora.technologies.office.core.model.WatermarkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.net.URI
import java.util.zip.ZipInputStream

private class StubPdfDocRepo : DocumentRepository {
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
        Result.success(DocumentItem(uriString, uriString, "out.pdf", "pdf", DocFormat.PDF, 100L, 1L, "Exports"))
}

private class StubPdfEngine : PdfEnginePort {
    override suspend fun getPageCount(uriString: String, password: String?): Int = 1
    override suspend fun openDocumentInfo(uriString: String, password: String?): PdfDocumentInfo = PdfDocumentInfo(1, emptyList())
    override suspend fun renderPage(uriString: String, pageIndex: Int, targetWidthPx: Int, password: String?): PdfPageRaster =
        PdfPageRaster(10, 10, IntArray(100))
    override suspend fun extractPageText(uriString: String, pageIndex: Int, password: String?): String = ""
    override suspend fun searchText(uriString: String, query: String, password: String?): List<PdfSearchMatch> = emptyList()
}

private class StubScannerPort : ScannerPort {
    override suspend fun recognizeTextFromFile(imagePath: String, script: OcrScript) =
        Result.success(OcrExtractionResult(script, "OCR fallback text", 1))
    override suspend fun decodeBarcodesFromFile(imagePath: String) = Result.success(emptyList<QrDecodedAction>())
    override suspend fun decodeBarcodeFromRaw(rawValue: String) = QrDecodedAction.PlainText(rawValue)
    override suspend fun applyFilterToImage(sourcePath: String, filter: ScanFilter, rotationDegrees: Int) = Result.success(sourcePath)
    override suspend fun exportPagesToPdf(imagePaths: List<String>, outputBaseName: String) =
        Result.failure<PdfToolResult>(UnsupportedOperationException())
    override suspend fun composeDualIdCard(frontImagePath: String, backImagePath: String, exportAsPdf: Boolean, outputBaseName: String) =
        Result.failure<PdfToolResult>(UnsupportedOperationException())
    override suspend fun exportOcrText(text: String, outputBaseName: String, asDocx: Boolean) =
        Result.failure<PdfToolResult>(UnsupportedOperationException())
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PdfBoxPowerEngineTest {
    @Test
    fun `text to pdf, watermark, aes lock and unlock, and pdf to docx work offline`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Robolectric on JDK 24 places Conscrypt at slot 1, breaking JDK JarVerifier.testSignatures();
        // restore JDK default provider order in test scope so real AES-256 PDFBox encryption executes.
        java.security.Security.removeProvider("Conscrypt")
        val engine = PdfBoxPowerEngine(
            context = context,
            documentRepository = StubPdfDocRepo(),
            pdfEngine = StubPdfEngine(),
            scannerPort = StubScannerPort(),
            hwpEngine = object : HwpEnginePort {
                override suspend fun renderToSanitizedHtml(uriString: String, password: String?) =
                    HwpRenderResult("h", "<p>h</p>", "h", 1L, false)
            },
            officeEngine = object : OfficeEnginePort {
                override fun isNativeEngineAvailable() = false
                override suspend fun extractReadablePreview(uriString: String, format: DocFormat) =
                    ParsedTextDocument("t", emptyList(), "")
            },
            textEpubEngine = object : TextEpubEnginePort {
                override suspend fun loadDocument(uriString: String, format: DocFormat) =
                    ParsedTextDocument("t", emptyList(), "")
            },
            ioDispatcher = Dispatchers.IO,
        )

        val textPdf = engine.textToPdf(
            textContent = List(90) { "Line $it Evora Office offline PDF conversion check." }.joinToString("\n"),
            outputBaseName = "UnitTestText",
            pageSize = PageSizePreset.A4,
        ).getOrThrow()
        assertEquals(PdfToolKind.TEXT_TO_PDF, textPdf.toolKind)
        assertTrue(File(URI(textPdf.outputUriString)).exists())

        val splitRes = engine.splitPdf(
            sourceUriString = textPdf.outputUriString,
            mode = SplitMode.RANGE,
            rangeExpression = "1",
            everyNPages = 1,
            selectedZeroBasedPages = emptySet(),
        ).getOrThrow()
        assertTrue(File(URI(splitRes.outputUriString)).exists())

        val watermarked = engine.watermarkPdf(
            sourceUriString = splitRes.outputUriString,
            config = WatermarkConfig(text = "EVORA", tiled = true),
        ).getOrThrow()
        assertTrue(File(URI(watermarked.outputUriString)).exists())

        val locked = engine.setPassword(
            sourceUriString = watermarked.outputUriString,
            userPassword = "pass123",
            ownerPassword = "owner123",
            useAes256 = true,
        ).getOrThrow()

        // Opening locked PDF without password must raise DocumentPasswordRequiredException
        val lockedAttempt = engine.compressPdf(locked.outputUriString, CompressQuality.LOW)
        assertTrue(lockedAttempt.exceptionOrNull() is DocumentPasswordRequiredException)

        val unlocked = engine.removePassword(locked.outputUriString, "pass123").getOrThrow()
        val docxRes = engine.pdfToDocxOffline(unlocked.outputUriString).getOrThrow()
        val docxFile = File(URI(docxRes.outputUriString))
        assertTrue(docxFile.exists())
        ZipInputStream(docxFile.inputStream()).use { zis ->
            val names = generateSequence { zis.nextEntry?.name }.toList()
            assertTrue("word/document.xml" in names)
        }
    }
}
