package com.evora.technologies.office.feature.editor

import app.cash.turbine.test
import com.evora.technologies.office.core.domain.port.DraftRecoveryRepository
import com.evora.technologies.office.core.domain.port.OfficeEditorPort
import com.evora.technologies.office.core.domain.port.PdfDocumentInfo
import com.evora.technologies.office.core.domain.port.PdfEnginePort
import com.evora.technologies.office.core.domain.port.PdfPageRaster
import com.evora.technologies.office.core.domain.port.PdfPowerPort
import com.evora.technologies.office.core.domain.port.PreferencesRepository
import com.evora.technologies.office.core.domain.port.ProEntitlementPort
import com.evora.technologies.office.core.domain.port.SignatureRepository
import com.evora.technologies.office.core.model.AppThemeMode
import com.evora.technologies.office.core.model.CompressQuality
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.DraftCheckpoint
import com.evora.technologies.office.core.model.OfficeKind
import com.evora.technologies.office.core.model.PageSizePreset
import com.evora.technologies.office.core.model.PdfAnnotationDraft
import com.evora.technologies.office.core.model.PdfSearchMatch
import com.evora.technologies.office.core.model.PdfSignaturePlacement
import com.evora.technologies.office.core.model.PdfToolKind
import com.evora.technologies.office.core.model.PdfToolResult
import com.evora.technologies.office.core.model.Pkcs7CertificateInfo
import com.evora.technologies.office.core.model.ProFeature
import com.evora.technologies.office.core.model.ProStatus
import com.evora.technologies.office.core.model.SignatureItem
import com.evora.technologies.office.core.model.SlideDeckSnapshot
import com.evora.technologies.office.core.model.SlideElement
import com.evora.technologies.office.core.model.SlidePageData
import com.evora.technologies.office.core.model.SortOrder
import com.evora.technologies.office.core.model.SplitMode
import com.evora.technologies.office.core.model.WatermarkConfig
import com.evora.technologies.office.core.model.WordDocumentSnapshot
import com.evora.technologies.office.core.model.WordParagraph
import com.evora.technologies.office.core.model.WorkbookSnapshot
import com.evora.technologies.office.core.model.WorksheetData
import com.evora.technologies.office.feature.editor.office.OfficeEditorEffect
import com.evora.technologies.office.feature.editor.office.OfficeEditorIntent
import com.evora.technologies.office.feature.editor.office.OfficeEditorViewModel
import com.evora.technologies.office.feature.editor.pdf.PdfEditorIntent
import com.evora.technologies.office.feature.editor.pdf.PdfEditorViewModel
import com.evora.technologies.office.feature.editor.slideshow.SlideShowIntent
import com.evora.technologies.office.feature.editor.slideshow.SlideShowViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeEditorPrefsRepo : PreferencesRepository {
    override val onboardingCompleted = MutableStateFlow(true)
    override val selectedLocaleTag = MutableStateFlow<String?>("en")
    override val themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    override val defaultSortOrder = MutableStateFlow(SortOrder.MODIFIED_DESC)
    override val autosaveIntervalSec = MutableStateFlow(15)
    override val defaultAppPromptDismissed = MutableStateFlow(false)
    override val externalOpenCount = MutableStateFlow(0)
    override suspend fun setOnboardingCompleted(completed: Boolean) = Unit
    override suspend fun setSelectedLocaleTag(tag: String) = Unit
    override suspend fun setThemeMode(mode: AppThemeMode) = Unit
    override suspend fun setDefaultSortOrder(sortOrder: SortOrder) = Unit
    override suspend fun setDefaultAppPromptDismissed(dismissed: Boolean) = Unit
    override suspend fun incrementExternalOpenCount() = Unit
}

private class FakeDraftRepo : DraftRecoveryRepository {
    var pending: DraftCheckpoint? = null
    var savedBytes: ByteArray? = null
    override fun observePendingDrafts() = MutableStateFlow(listOfNotNull(pending))
    override suspend fun getPendingDraft(targetUriString: String) = pending?.takeIf { it.targetUriString == targetUriString }
    override suspend fun writeAtomicCheckpoint(targetUriString: String, displayName: String, serializedPayload: ByteArray): Result<DraftCheckpoint> {
        savedBytes = serializedPayload
        val cp = DraftCheckpoint(targetUriString, "/tmp/draft", displayName, 100L, true)
        pending = cp
        return Result.success(cp)
    }
    override suspend fun readDraftBytes(draftCachePath: String) = savedBytes
    override suspend fun discardDraft(targetUriString: String): Result<Unit> {
        if (pending?.targetUriString == targetUriString) pending = null
        return Result.success(Unit)
    }
}

private class FakeOfficePort : OfficeEditorPort {
    var savedWord: WordDocumentSnapshot? = null
    override fun isNativeLokLoaded() = false
    override suspend fun loadWordDocument(uriString: String) = Result.success(
        WordDocumentSnapshot("Contract.docx", listOf(WordParagraph("p0", "Original clause"))),
    )
    override suspend fun saveWordDocument(uriString: String, snapshot: WordDocumentSnapshot): Result<DocumentItem> {
        savedWord = snapshot
        return Result.success(DocumentItem(uriString, uriString, snapshot.title, "docx", DocFormat.WORD, 100L, 1L, "Docs"))
    }
    override suspend fun loadWorkbook(uriString: String) = Result.success(
        WorkbookSnapshot("Budget.xlsx", listOf(WorksheetData("Sheet1"))),
    )
    override suspend fun saveWorkbook(uriString: String, snapshot: WorkbookSnapshot) =
        Result.success(DocumentItem(uriString, uriString, snapshot.title, "xlsx", DocFormat.EXCEL, 100L, 1L, "Docs"))
    override suspend fun loadSlideDeck(uriString: String) = Result.success(
        SlideDeckSnapshot("Deck.pptx", listOf(SlidePageData("s0", listOf(SlideElement("e0", "TEXT", "Intro"))))),
    )
    override suspend fun saveSlideDeck(uriString: String, snapshot: SlideDeckSnapshot) =
        Result.success(DocumentItem(uriString, uriString, snapshot.title, "pptx", DocFormat.PPT, 100L, 1L, "Docs"))
    override fun encodeWordDraft(snapshot: WordDocumentSnapshot) = snapshot.paragraphs.first().text.toByteArray()
    override fun decodeWordDraft(bytes: ByteArray) = WordDocumentSnapshot("Contract.docx", listOf(WordParagraph("p0", String(bytes))))
    override fun encodeWorkbookDraft(snapshot: WorkbookSnapshot) = ByteArray(0)
    override fun decodeWorkbookDraft(bytes: ByteArray) = WorkbookSnapshot("Budget.xlsx", emptyList())
    override fun encodeSlideDeckDraft(snapshot: SlideDeckSnapshot) = ByteArray(0)
    override fun decodeSlideDeckDraft(bytes: ByteArray) = SlideDeckSnapshot("Deck.pptx", emptyList())
}

private class FakeEditorPdfPower : PdfPowerPort {
    var savedCount = 0
    override suspend fun applyAnnotationsAndSignatures(sourceUriString: String, outputFileName: String?, annotations: List<PdfAnnotationDraft>, signatures: List<PdfSignaturePlacement>, password: String?): Result<PdfToolResult> {
        savedCount++
        return Result.success(PdfToolResult(PdfToolKind.PKCS7_SIGN, sourceUriString, "annotated.pdf", 100, 110, "Saved"))
    }
    override suspend fun inspectPkcs7Signatures(uriString: String, password: String?) = Result.success(emptyList<Pkcs7CertificateInfo>())
    override suspend fun signWithPkcs12(sourceUriString: String, p12UriString: String, p12Password: String, reason: String, location: String, pdfPassword: String?) =
        Result.success(PdfToolResult(PdfToolKind.PKCS7_SIGN, sourceUriString, "signed.pdf", 100, 120, "Signed"))
    override suspend fun setPassword(sourceUriString: String, userPassword: String, ownerPassword: String, useAes256: Boolean, currentPassword: String?) =
        Result.success(PdfToolResult(PdfToolKind.SET_PASSWORD, sourceUriString, "locked.pdf", 100, 100, "Locked"))
    override suspend fun removePassword(sourceUriString: String, currentPassword: String) =
        Result.success(PdfToolResult(PdfToolKind.REMOVE_PASSWORD, sourceUriString, "unlocked.pdf", 100, 100, "Unlocked"))
    override suspend fun mergePdfs(sourceUris: List<String>, outputBaseName: String) = Result.failure<PdfToolResult>(UnsupportedOperationException())
    override suspend fun splitPdf(sourceUriString: String, mode: SplitMode, rangeExpression: String, everyNPages: Int, selectedZeroBasedPages: Set<Int>, password: String?, onProgress: (Float) -> Unit) = Result.failure<PdfToolResult>(UnsupportedOperationException())
    override suspend fun compressPdf(sourceUriString: String, quality: CompressQuality, password: String?, onProgress: (Float) -> Unit) = Result.failure<PdfToolResult>(UnsupportedOperationException())
    override suspend fun watermarkPdf(sourceUriString: String, config: WatermarkConfig, password: String?, onProgress: (Float) -> Unit) = Result.failure<PdfToolResult>(UnsupportedOperationException())
    override suspend fun imagesToPdf(imageUris: List<String>, outputBaseName: String, pageSize: PageSizePreset, marginDp: Int, landscape: Boolean, onProgress: (Float) -> Unit) = Result.failure<PdfToolResult>(UnsupportedOperationException())
    override suspend fun textToPdf(textContent: String, outputBaseName: String, pageSize: PageSizePreset, onProgress: (Float) -> Unit) = Result.failure<PdfToolResult>(UnsupportedOperationException())
    override suspend fun documentToPdf(sourceUriString: String, onProgress: (Float) -> Unit) =
        Result.success(PdfToolResult(PdfToolKind.DOC_TO_PDF, "file:///exp.pdf", "exp.pdf", 100, 90, "Exported"))
    override suspend fun pdfToImages(sourceUriString: String, stitchedLongImage: Boolean, password: String?, onProgress: (Float) -> Unit) = Result.failure<PdfToolResult>(UnsupportedOperationException())
    override suspend fun pdfToDocxOffline(sourceUriString: String, password: String?, onProgress: (Float) -> Unit) = Result.failure<PdfToolResult>(UnsupportedOperationException())
}

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelsTest {
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
    fun `word track changes, 15s checkpoint, and save without paywall work`() = runTest {
        val officePort = FakeOfficePort()
        val draftRepo = FakeDraftRepo()
        val vm = OfficeEditorViewModel(officePort, draftRepo, FakeEditorPdfPower(), FakeEditorPrefsRepo())

        vm.onIntent(OfficeEditorIntent.OpenDocument("file:///Contract.docx", "Contract.docx", OfficeKind.WORD))
        runCurrent()
        assertFalse(vm.state.value.isLoading)

        vm.onIntent(OfficeEditorIntent.ToggleTrackChanges(true))
        vm.onIntent(OfficeEditorIntent.UpdateParagraphText(0, "Original clause revised"))
        vm.onIntent(OfficeEditorIntent.AddReviewComment("Checked legal"))
        assertTrue(vm.state.value.isDirty)
        assertTrue((vm.state.value.wordSnapshot?.trackChanges?.size ?: 0) >= 2)

        // Verify 15-second periodic atomic autosave ticker
        advanceTimeBy(15_100L)
        runCurrent()
        assertNotNull(draftRepo.pending)

        // Verify Save completes with zero paywall and clears pending recovery
        vm.effects.test {
            vm.onIntent(OfficeEditorIntent.SaveClicked)
            runCurrent()
            assertFalse(vm.state.value.isDirty)
            assertNull(draftRepo.pending)
            assertNotNull(officePort.savedWord)
            assertTrue(awaitItem() is OfficeEditorEffect.ShowMessage)
            vm.onIntent(OfficeEditorIntent.BackClicked)
            runCurrent()
            assertEquals(OfficeEditorEffect.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `excel formula bar evaluates live and saves cleanly`() = runTest {
        val vm = OfficeEditorViewModel(FakeOfficePort(), FakeDraftRepo(), FakeEditorPdfPower(), FakeEditorPrefsRepo())
        vm.onIntent(OfficeEditorIntent.OpenDocument("file:///Budget.xlsx", "Budget.xlsx", OfficeKind.EXCEL))
        runCurrent()

        vm.onIntent(OfficeEditorIntent.SelectCell(0, 0))
        vm.onIntent(OfficeEditorIntent.UpdateFormulaBar("25"))
        vm.onIntent(OfficeEditorIntent.CommitCellFormula)
        vm.onIntent(OfficeEditorIntent.SelectCell(1, 0))
        vm.onIntent(OfficeEditorIntent.UpdateFormulaBar("=A1*2"))
        vm.onIntent(OfficeEditorIntent.CommitCellFormula)

        val sheet = vm.state.value.workbookSnapshot?.sheets?.first()
        assertEquals("50", sheet?.cells?.get("1,0")?.evaluatedDisplay)
        vm.onIntent(OfficeEditorIntent.BackClicked)
        runCurrent()
    }

    @Test
    fun `pdf editor records ink, sticky note, signature and saves incrementally`() = runTest {
        val power = FakeEditorPdfPower()
        val sigRepo = object : SignatureRepository {
            override fun observeSignatures() = MutableStateFlow(listOf(SignatureItem("s1", "/tmp/s1.png", 1L)))
            override suspend fun saveSignaturePng(width: Int, height: Int, strokes: List<List<Pair<Float, Float>>>, strokeColorArgb: Long, strokeWidthPx: Float) =
                Result.success(SignatureItem("s1", "/tmp/s1.png", 1L))
            override suspend fun deleteSignature(id: String) = Result.success(Unit)
        }
        val pdfEngine = object : PdfEnginePort {
            override suspend fun getPageCount(uriString: String, password: String?) = 2
            override suspend fun openDocumentInfo(uriString: String, password: String?) = PdfDocumentInfo(2, emptyList())
            override suspend fun renderPage(uriString: String, pageIndex: Int, targetWidthPx: Int, password: String?) =
                PdfPageRaster(4, 4, IntArray(16))
            override suspend fun extractPageText(uriString: String, pageIndex: Int, password: String?) = ""
            override suspend fun searchText(uriString: String, query: String, password: String?) = emptyList<PdfSearchMatch>()
        }
        val proPort = object : ProEntitlementPort {
            override val proStatus = MutableStateFlow(ProStatus())
            override suspend fun hasAccess(feature: ProFeature) = false
            override suspend fun grantRewardedPass(hours: Int) = Unit
            override suspend fun restorePurchases() = false
        }
        val vm = PdfEditorViewModel(pdfEngine, power, sigRepo, proPort)
        vm.onIntent(PdfEditorIntent.OpenPdf("file:///doc.pdf", "doc.pdf"))
        advanceUntilIdle()

        vm.onIntent(PdfEditorIntent.AddInkStroke(listOf(0.1f to 0.1f, 0.3f to 0.3f)))
        vm.onIntent(PdfEditorIntent.OpenStickyNoteAt(0.4f, 0.4f))
        vm.onIntent(PdfEditorIntent.SaveStickyNote("Sign here", "Legal"))
        vm.onIntent(PdfEditorIntent.PlaceSavedSignature(SignatureItem("s1", "/tmp/s1.png", 1L)))
        assertEquals(2, vm.state.value.annotations.size)
        assertEquals(1, vm.state.value.signatures.size)

        vm.onIntent(PdfEditorIntent.SavePdfClicked)
        advanceUntilIdle()
        assertEquals(1, power.savedCount)
        assertTrue(vm.state.value.annotations.isEmpty())
    }

    @Test
    fun `slideshow advances slides, ticks timer, and toggles laser ink`() = runTest {
        val vm = SlideShowViewModel(FakeOfficePort())
        vm.onIntent(SlideShowIntent.OpenPresentation("file:///Deck.pptx", 0))
        runCurrent()
        assertEquals(1, vm.state.value.slides.size)

        advanceTimeBy(2_100L)
        runCurrent()
        assertEquals(2, vm.state.value.elapsedSeconds)

        vm.onIntent(SlideShowIntent.ToggleLaserPointer)
        assertTrue(vm.state.value.isLaserEnabled)
        vm.onIntent(SlideShowIntent.AddLaserStroke(listOf(0.1f to 0.1f, 0.8f to 0.8f)))
        assertEquals(1, vm.state.value.laserStrokes.size)
        vm.onIntent(SlideShowIntent.ExitClicked)
        runCurrent()
    }
}
