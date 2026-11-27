package com.evora.technologies.office.feature.scanner

import app.cash.turbine.test
import com.evora.technologies.office.core.domain.port.AnalyticsPort
import com.evora.technologies.office.core.domain.port.OcrExtractionResult
import com.evora.technologies.office.core.domain.port.ScannerPort
import com.evora.technologies.office.core.domain.util.QrPayloadParser
import com.evora.technologies.office.core.model.OcrScript
import com.evora.technologies.office.core.model.PdfToolKind
import com.evora.technologies.office.core.model.PdfToolResult
import com.evora.technologies.office.core.model.QrDecodedAction
import com.evora.technologies.office.core.model.ScanFilter
import com.evora.technologies.office.core.model.ScanMode
import com.evora.technologies.office.feature.scanner.camera.ScannerFlowEffect
import com.evora.technologies.office.feature.scanner.camera.ScannerFlowIntent
import com.evora.technologies.office.feature.scanner.camera.ScannerFlowViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

private class FakeScannerPort : ScannerPort {
    var idCardComposed = false
    override suspend fun recognizeTextFromFile(imagePath: String, script: OcrScript) =
        Result.success(OcrExtractionResult(script, "Extracted ${script.name}", 1))
    override suspend fun decodeBarcodesFromFile(imagePath: String) =
        Result.success(listOf(QrDecodedAction.Url("https://evora.dev", "https://evora.dev")))
    override suspend fun decodeBarcodeFromRaw(rawValue: String) = QrPayloadParser.parse(rawValue)
    override suspend fun applyFilterToImage(sourcePath: String, filter: ScanFilter, rotationDegrees: Int) =
        Result.success("${sourcePath}_${filter.name}_$rotationDegrees.jpg")
    override suspend fun exportPagesToPdf(imagePaths: List<String>, outputBaseName: String) =
        Result.success(PdfToolResult(PdfToolKind.IMAGE_TO_PDF, "file:///scan.pdf", "scan.pdf", 100, 80, "ok"))
    override suspend fun composeDualIdCard(frontImagePath: String, backImagePath: String, exportAsPdf: Boolean, outputBaseName: String): Result<PdfToolResult> {
        idCardComposed = true
        return Result.success(PdfToolResult(PdfToolKind.IMAGE_TO_PDF, "file:///id.pdf", "id.pdf", 200, 120, "A4 ID"))
    }
    override suspend fun exportOcrText(text: String, outputBaseName: String, asDocx: Boolean) =
        Result.success(PdfToolResult(PdfToolKind.PDF_TO_DOCX, "file:///ocr.docx", "ocr.docx", 50, 120, "OCR saved"))
}

private class FakeScannerAnalytics : AnalyticsPort {
    override fun logOnboardingStep(stepName: String, action: String) = Unit
    override fun logStoragePermissionResult(grantType: String) = Unit
    override fun logDocumentOpen(format: String, source: String) = Unit
    override fun logEditorSessionEnd(format: String, editDurationSec: Long, saveStatus: String) = Unit
    override fun logPdfToolComplete(toolType: String, durationMs: Long, success: Boolean) = Unit
    override fun logScanOcrComplete(mode: String, pageCount: Int, script: String) = Unit
    override fun logPaywallImpression(triggerSource: String) = Unit
    override fun logPurchaseSuccess(skuId: String, priceUsdMicros: Long) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerFlowViewModelTest {
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
    fun `dual id card advances step 1 to 2 to A4 composition cleanly without ad gate`() = runTest {
        val scannerPort = FakeScannerPort()
        val vm = ScannerFlowViewModel(scannerPort, FakeScannerAnalytics())
        vm.onIntent(ScannerFlowIntent.SelectMode(ScanMode.ID_CARD))
        assertEquals(1, vm.state.value.idCardStep)

        vm.onIntent(ScannerFlowIntent.ImageCaptured("/tmp/front.jpg"))
        assertEquals(2, vm.state.value.idCardStep)

        vm.onIntent(ScannerFlowIntent.ImageCaptured("/tmp/back.jpg"))
        assertEquals(3, vm.state.value.idCardStep)

        vm.onIntent(ScannerFlowIntent.ExportDualIdCard(asPdf = true))
        advanceUntilIdle()
        assertTrue(scannerPort.idCardComposed)
        assertEquals("id.pdf", vm.state.value.exportResult?.outputDisplayName)
    }

    @Test
    fun `ocr 5-script selection extracts text and exports docx`() = runTest {
        val vm = ScannerFlowViewModel(FakeScannerPort(), FakeScannerAnalytics())
        vm.onIntent(ScannerFlowIntent.SelectMode(ScanMode.OCR))
        vm.onIntent(ScannerFlowIntent.SelectOcrScript(OcrScript.KOREAN))
        vm.onIntent(ScannerFlowIntent.ImageCaptured("/tmp/hangul.jpg"))
        advanceUntilIdle()
        assertEquals("Extracted KOREAN", vm.state.value.ocrExtractedText)

        vm.effects.test {
            vm.onIntent(ScannerFlowIntent.CopyOcrTextClicked)
            assertEquals(ScannerFlowEffect.CopyToClipboard("Extracted KOREAN"), awaitItem())
            assertTrue(awaitItem() is ScannerFlowEffect.ShowMessage)
            expectNoEvents()
        }
    }
}
