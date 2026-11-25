package com.evora.technologies.office.core.domain.port

import com.evora.technologies.office.core.model.CompressQuality
import com.evora.technologies.office.core.model.DocTemplatePreset
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.DraftCheckpoint
import com.evora.technologies.office.core.model.OcrScript
import com.evora.technologies.office.core.model.PageSizePreset
import com.evora.technologies.office.core.model.PdfAnnotationDraft
import com.evora.technologies.office.core.model.PdfSignaturePlacement
import com.evora.technologies.office.core.model.PdfToolResult
import com.evora.technologies.office.core.model.Pkcs7CertificateInfo
import com.evora.technologies.office.core.model.QrDecodedAction
import com.evora.technologies.office.core.model.ScanFilter
import com.evora.technologies.office.core.model.SignatureItem
import com.evora.technologies.office.core.model.SlideDeckSnapshot
import com.evora.technologies.office.core.model.SplitMode
import com.evora.technologies.office.core.model.WatermarkConfig
import com.evora.technologies.office.core.model.WordDocumentSnapshot
import com.evora.technologies.office.core.model.WorkbookSnapshot
import kotlinx.coroutines.flow.Flow

interface PdfPowerPort {
    suspend fun applyAnnotationsAndSignatures(
        sourceUriString: String,
        outputFileName: String?,
        annotations: List<PdfAnnotationDraft>,
        signatures: List<PdfSignaturePlacement>,
        password: String? = null,
    ): Result<PdfToolResult>

    suspend fun inspectPkcs7Signatures(
        uriString: String,
        password: String? = null,
    ): Result<List<Pkcs7CertificateInfo>>

    suspend fun signWithPkcs12(
        sourceUriString: String,
        p12UriString: String,
        p12Password: String,
        reason: String,
        location: String,
        pdfPassword: String? = null,
    ): Result<PdfToolResult>

    suspend fun setPassword(
        sourceUriString: String,
        userPassword: String,
        ownerPassword: String,
        useAes256: Boolean,
        currentPassword: String? = null,
    ): Result<PdfToolResult>

    suspend fun removePassword(
        sourceUriString: String,
        currentPassword: String,
    ): Result<PdfToolResult>

    suspend fun mergePdfs(
        sourceUris: List<String>,
        outputBaseName: String,
    ): Result<PdfToolResult>

    suspend fun splitPdf(
        sourceUriString: String,
        mode: SplitMode,
        rangeExpression: String,
        everyNPages: Int,
        selectedZeroBasedPages: Set<Int>,
        password: String? = null,
        onProgress: (Float) -> Unit = {},
    ): Result<PdfToolResult>

    suspend fun compressPdf(
        sourceUriString: String,
        quality: CompressQuality,
        password: String? = null,
        onProgress: (Float) -> Unit = {},
    ): Result<PdfToolResult>

    suspend fun watermarkPdf(
        sourceUriString: String,
        config: WatermarkConfig,
        password: String? = null,
        onProgress: (Float) -> Unit = {},
    ): Result<PdfToolResult>

    suspend fun imagesToPdf(
        imageUris: List<String>,
        outputBaseName: String,
        pageSize: PageSizePreset,
        marginDp: Int,
        landscape: Boolean,
        onProgress: (Float) -> Unit = {},
    ): Result<PdfToolResult>

    suspend fun textToPdf(
        textContent: String,
        outputBaseName: String,
        pageSize: PageSizePreset,
        onProgress: (Float) -> Unit = {},
    ): Result<PdfToolResult>

    suspend fun documentToPdf(
        sourceUriString: String,
        onProgress: (Float) -> Unit = {},
    ): Result<PdfToolResult>

    suspend fun pdfToImages(
        sourceUriString: String,
        stitchedLongImage: Boolean,
        password: String? = null,
        onProgress: (Float) -> Unit = {},
    ): Result<PdfToolResult>

    suspend fun pdfToDocxOffline(
        sourceUriString: String,
        password: String? = null,
        onProgress: (Float) -> Unit = {},
    ): Result<PdfToolResult>
}

interface SignatureRepository {
    fun observeSignatures(): Flow<List<SignatureItem>>
    suspend fun saveSignaturePng(
        width: Int,
        height: Int,
        strokes: List<List<Pair<Float, Float>>>,
        strokeColorArgb: Long,
        strokeWidthPx: Float,
    ): Result<SignatureItem>
    suspend fun deleteSignature(id: String): Result<Unit>
}

data class OcrExtractionResult(
    val script: OcrScript,
    val text: String,
    val blockCount: Int,
)

interface ScannerPort {
    suspend fun recognizeTextFromFile(imagePath: String, script: OcrScript): Result<OcrExtractionResult>
    suspend fun decodeBarcodesFromFile(imagePath: String): Result<List<QrDecodedAction>>
    suspend fun decodeBarcodeFromRaw(rawValue: String): QrDecodedAction
    suspend fun applyFilterToImage(sourcePath: String, filter: ScanFilter, rotationDegrees: Int = 0): Result<String>
    suspend fun exportPagesToPdf(imagePaths: List<String>, outputBaseName: String): Result<PdfToolResult>
    suspend fun composeDualIdCard(
        frontImagePath: String,
        backImagePath: String,
        exportAsPdf: Boolean,
        outputBaseName: String,
    ): Result<PdfToolResult>
    suspend fun exportOcrText(text: String, outputBaseName: String, asDocx: Boolean): Result<PdfToolResult>
}

interface OfficeEditorPort {
    fun isNativeLokLoaded(): Boolean
    suspend fun loadWordDocument(uriString: String): Result<WordDocumentSnapshot>
    suspend fun saveWordDocument(uriString: String, snapshot: WordDocumentSnapshot): Result<DocumentItem>
    suspend fun loadWorkbook(uriString: String): Result<WorkbookSnapshot>
    suspend fun saveWorkbook(uriString: String, snapshot: WorkbookSnapshot): Result<DocumentItem>
    suspend fun loadSlideDeck(uriString: String): Result<SlideDeckSnapshot>
    suspend fun saveSlideDeck(uriString: String, snapshot: SlideDeckSnapshot): Result<DocumentItem>
    fun encodeWordDraft(snapshot: WordDocumentSnapshot): ByteArray
    fun decodeWordDraft(bytes: ByteArray): WordDocumentSnapshot
    fun encodeWorkbookDraft(snapshot: WorkbookSnapshot): ByteArray
    fun decodeWorkbookDraft(bytes: ByteArray): WorkbookSnapshot
    fun encodeSlideDeckDraft(snapshot: SlideDeckSnapshot): ByteArray
    fun decodeSlideDeckDraft(bytes: ByteArray): SlideDeckSnapshot
}

interface DraftRecoveryRepository {
    fun observePendingDrafts(): Flow<List<DraftCheckpoint>>
    suspend fun getPendingDraft(targetUriString: String): DraftCheckpoint?
    suspend fun writeAtomicCheckpoint(
        targetUriString: String,
        displayName: String,
        serializedPayload: ByteArray,
    ): Result<DraftCheckpoint>
    suspend fun readDraftBytes(draftCachePath: String): ByteArray?
    suspend fun discardDraft(targetUriString: String): Result<Unit>
}

interface TemplateRepository {
    fun getAvailableTemplates(): List<DocTemplatePreset>
    suspend fun createDocumentFromTemplate(
        preset: DocTemplatePreset,
        customBaseName: String? = null,
    ): Result<DocumentItem>
}
