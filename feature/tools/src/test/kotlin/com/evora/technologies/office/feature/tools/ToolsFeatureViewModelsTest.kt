package com.evora.technologies.office.feature.tools

import app.cash.turbine.test
import com.evora.technologies.office.core.domain.port.AnalyticsPort
import com.evora.technologies.office.core.domain.port.DocumentRepository
import com.evora.technologies.office.core.domain.port.PdfDocumentInfo
import com.evora.technologies.office.core.domain.port.PdfEnginePort
import com.evora.technologies.office.core.domain.port.PdfPageRaster
import com.evora.technologies.office.core.domain.port.PdfPowerPort
import com.evora.technologies.office.core.domain.port.TemplateRepository
import com.evora.technologies.office.core.model.CompressQuality
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocTemplatePreset
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.ExternalIntentPayload
import com.evora.technologies.office.core.model.OfficeKind
import com.evora.technologies.office.core.model.PageSizePreset
import com.evora.technologies.office.core.model.PdfAnnotationDraft
import com.evora.technologies.office.core.model.PdfSearchMatch
import com.evora.technologies.office.core.model.PdfSignaturePlacement
import com.evora.technologies.office.core.model.PdfToolKind
import com.evora.technologies.office.core.model.PdfToolResult
import com.evora.technologies.office.core.model.Pkcs7CertificateInfo
import com.evora.technologies.office.core.model.ReaderBookmarkState
import com.evora.technologies.office.core.model.ReaderTheme
import com.evora.technologies.office.core.model.SortOrder
import com.evora.technologies.office.core.model.SplitMode
import com.evora.technologies.office.core.model.TrashItem
import com.evora.technologies.office.core.model.WatermarkConfig
import com.evora.technologies.office.feature.tools.hub.ToolsHubEffect
import com.evora.technologies.office.feature.tools.hub.ToolsHubIntent
import com.evora.technologies.office.feature.tools.hub.ToolsHubViewModel
import com.evora.technologies.office.feature.tools.hub.UtilityToolId
import com.evora.technologies.office.feature.tools.pdf.PdfToolConfigEffect
import com.evora.technologies.office.feature.tools.pdf.PdfToolConfigIntent
import com.evora.technologies.office.feature.tools.pdf.PdfToolConfigViewModel
import com.evora.technologies.office.feature.tools.pdf.ToolInputFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeToolsTemplateRepo : TemplateRepository {
    override fun getAvailableTemplates() = listOf(
        DocTemplatePreset("t1", "Resume CV", "Layout", OfficeKind.WORD, "resume_cv.docx", "Resume.docx"),
    )
    override suspend fun createDocumentFromTemplate(preset: DocTemplatePreset, customBaseName: String?) =
        Result.success(DocumentItem("1", "file:///Resume.docx", "Resume.docx", "docx", DocFormat.WORD, 100L, 1L, "Docs"))
}

private class FakeToolsDocRepo : DocumentRepository {
    val docs = MutableStateFlow(emptyList<DocumentItem>())
    override fun observeLibraryDocuments(filter: DocFormat?, sortOrder: SortOrder): Flow<List<DocumentItem>> = docs
    override fun observeRecentDocuments(): Flow<List<DocumentItem>> = docs
    override fun observeFavoriteDocuments(): Flow<List<DocumentItem>> = docs
    override fun observeTrashItems(): Flow<List<TrashItem>> = MutableStateFlow(emptyList())
    override fun searchDocuments(query: String, filter: DocFormat?): Flow<List<DocumentItem>> = docs
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
    override suspend fun registerSafUri(uriString: String): Result<DocumentItem> = Result.failure(UnsupportedOperationException())
}

private class FakePdfPowerPort : PdfPowerPort {
    var mergeCalled = false
    override suspend fun applyAnnotationsAndSignatures(sourceUriString: String, outputFileName: String?, annotations: List<PdfAnnotationDraft>, signatures: List<PdfSignaturePlacement>, password: String?) =
        Result.success(PdfToolResult(PdfToolKind.PKCS7_SIGN, "file:///out.pdf", "out.pdf", 10, 10, "ok"))
    override suspend fun inspectPkcs7Signatures(uriString: String, password: String?) = Result.success(emptyList<Pkcs7CertificateInfo>())
    override suspend fun signWithPkcs12(sourceUriString: String, p12UriString: String, p12Password: String, reason: String, location: String, pdfPassword: String?) =
        Result.success(PdfToolResult(PdfToolKind.PKCS7_SIGN, "file:///signed.pdf", "signed.pdf", 10, 10, "signed"))
    override suspend fun setPassword(sourceUriString: String, userPassword: String, ownerPassword: String, useAes256: Boolean, currentPassword: String?) =
        Result.success(PdfToolResult(PdfToolKind.SET_PASSWORD, "file:///locked.pdf", "locked.pdf", 10, 10, "locked"))
    override suspend fun removePassword(sourceUriString: String, currentPassword: String) =
        Result.success(PdfToolResult(PdfToolKind.REMOVE_PASSWORD, "file:///unlocked.pdf", "unlocked.pdf", 10, 10, "unlocked"))
    override suspend fun mergePdfs(sourceUris: List<String>, outputBaseName: String): Result<PdfToolResult> {
        mergeCalled = true
        return Result.success(PdfToolResult(PdfToolKind.MERGE, "file:///merged.pdf", "merged.pdf", 400, 350, "merged"))
    }
    override suspend fun splitPdf(sourceUriString: String, mode: SplitMode, rangeExpression: String, everyNPages: Int, selectedZeroBasedPages: Set<Int>, password: String?, onProgress: (Float) -> Unit) =
        Result.success(PdfToolResult(PdfToolKind.SPLIT, "file:///split.pdf", "split.pdf", 100, 50, "split"))
    override suspend fun compressPdf(sourceUriString: String, quality: CompressQuality, password: String?, onProgress: (Float) -> Unit) =
        Result.success(PdfToolResult(PdfToolKind.COMPRESS, "file:///comp.pdf", "comp.pdf", 1000, 400, "compressed"))
    override suspend fun watermarkPdf(sourceUriString: String, config: WatermarkConfig, password: String?, onProgress: (Float) -> Unit) =
        Result.success(PdfToolResult(PdfToolKind.WATERMARK, "file:///wm.pdf", "wm.pdf", 100, 110, "watermarked"))
    override suspend fun imagesToPdf(imageUris: List<String>, outputBaseName: String, pageSize: PageSizePreset, marginDp: Int, landscape: Boolean, onProgress: (Float) -> Unit) =
        Result.success(PdfToolResult(PdfToolKind.IMAGE_TO_PDF, "file:///img.pdf", "img.pdf", 100, 80, "ok"))
    override suspend fun textToPdf(textContent: String, outputBaseName: String, pageSize: PageSizePreset, onProgress: (Float) -> Unit) =
        Result.success(PdfToolResult(PdfToolKind.TEXT_TO_PDF, "file:///txt.pdf", "txt.pdf", 100, 80, "ok"))
    override suspend fun documentToPdf(sourceUriString: String, onProgress: (Float) -> Unit) =
        Result.success(PdfToolResult(PdfToolKind.DOC_TO_PDF, "file:///doc.pdf", "doc.pdf", 100, 80, "ok"))
    override suspend fun pdfToImages(sourceUriString: String, stitchedLongImage: Boolean, password: String?, onProgress: (Float) -> Unit) =
        Result.success(PdfToolResult(PdfToolKind.PDF_TO_IMAGES, "file:///p1.png", "p1.png", 100, 80, "ok"))
    override suspend fun pdfToDocxOffline(sourceUriString: String, password: String?, onProgress: (Float) -> Unit) =
        Result.success(PdfToolResult(PdfToolKind.PDF_TO_DOCX, "file:///out.docx", "out.docx", 100, 80, "ok"))
}

private class FakeToolsPdfEngine : PdfEnginePort {
    override suspend fun getPageCount(uriString: String, password: String?) = 5
    override suspend fun openDocumentInfo(uriString: String, password: String?) = PdfDocumentInfo(5, emptyList())
    override suspend fun renderPage(uriString: String, pageIndex: Int, targetWidthPx: Int, password: String?) =
        PdfPageRaster(1, 1, intArrayOf(0))
    override suspend fun extractPageText(uriString: String, pageIndex: Int, password: String?) = ""
    override suspend fun searchText(uriString: String, query: String, password: String?) = emptyList<PdfSearchMatch>()
}

private class FakeToolsAnalytics : AnalyticsPort {
    val pdfToolsCompleted = mutableListOf<String>()

    override fun logOnboardingStep(stepName: String, action: String) = Unit
    override fun logStoragePermissionResult(grantType: String) = Unit
    override fun logDocumentOpen(format: String, source: String) = Unit
    override fun logEditorSessionEnd(format: String, editDurationSec: Long, saveStatus: String) = Unit
    override fun logPdfToolComplete(toolType: String, durationMs: Long, success: Boolean) {
        if (success) pdfToolsCompleted += toolType
    }

    override fun logScanOcrComplete(mode: String, pageCount: Int, script: String) = Unit
    override fun logPaywallImpression(triggerSource: String) = Unit
    override fun logPurchaseSuccess(skuId: String, priceUsdMicros: Long) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class ToolsFeatureViewModelsTest {
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
    fun `tools hub dispatches watermark, sign, and pdf to docx directly without ad gates`() = runTest {
        val vm = ToolsHubViewModel(FakeToolsTemplateRepo(), FakeToolsDocRepo())
        advanceUntilIdle()

        vm.effects.test {
            vm.onIntent(ToolsHubIntent.ToolClicked(UtilityToolId.WATERMARK_PDF))
            assertEquals(ToolsHubEffect.NavigateToPdfTool(PdfToolKind.WATERMARK), awaitItem())
            vm.onIntent(ToolsHubIntent.ToolClicked(UtilityToolId.SIGN_PDF))
            assertEquals(ToolsHubEffect.NavigateToPdfTool(PdfToolKind.PKCS7_SIGN), awaitItem())
            vm.onIntent(ToolsHubIntent.ToolClicked(UtilityToolId.PDF_TO_DOCX))
            assertEquals(ToolsHubEffect.NavigateToPdfTool(PdfToolKind.PDF_TO_DOCX), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `merge over 3 files executes directly without ad gate`() = runTest {
        val power = FakePdfPowerPort()
        val analytics = FakeToolsAnalytics()
        val vm = PdfToolConfigViewModel(
            pdfPowerPort = power,
            pdfEnginePort = FakeToolsPdfEngine(),
            documentRepository = FakeToolsDocRepo(),
            analyticsPort = analytics,
        )
        vm.onIntent(PdfToolConfigIntent.Initialize(PdfToolKind.MERGE, null))
        vm.onIntent(
            PdfToolConfigIntent.AddExternalFiles(
                listOf(
                    ToolInputFile("file:///1.pdf", "1.pdf"),
                    ToolInputFile("file:///2.pdf", "2.pdf"),
                    ToolInputFile("file:///3.pdf", "3.pdf"),
                    ToolInputFile("file:///4.pdf", "4.pdf"),
                ),
            ),
        )
        advanceUntilIdle()

        vm.onIntent(PdfToolConfigIntent.ExecuteClicked)
        advanceUntilIdle()
        assertTrue(power.mergeCalled)
        assertEquals(listOf("merge"), analytics.pdfToolsCompleted)
        assertEquals(12, vm.state.value.result?.savedPercent)
    }
}
