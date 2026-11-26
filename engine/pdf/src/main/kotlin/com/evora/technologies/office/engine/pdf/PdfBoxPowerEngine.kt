package com.evora.technologies.office.engine.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import com.evora.technologies.office.core.data.di.IoDispatcher
import com.evora.technologies.office.core.domain.port.DocumentInvalidPasswordException
import com.evora.technologies.office.core.domain.port.DocumentPasswordRequiredException
import com.evora.technologies.office.core.domain.port.DocumentRepository
import com.evora.technologies.office.core.domain.port.HwpEnginePort
import com.evora.technologies.office.core.domain.port.OfficeEnginePort
import com.evora.technologies.office.core.domain.port.PdfEnginePort
import com.evora.technologies.office.core.domain.port.PdfPowerPort
import com.evora.technologies.office.core.domain.port.ScannerPort
import com.evora.technologies.office.core.domain.port.TextEpubEnginePort
import com.evora.technologies.office.core.domain.util.OoxmlDocxWriter
import com.evora.technologies.office.core.domain.util.PageRangeParser
import com.evora.technologies.office.core.model.CompressQuality
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.OcrScript
import com.evora.technologies.office.core.model.PageSizePreset
import com.evora.technologies.office.core.model.PdfAnnotationDraft
import com.evora.technologies.office.core.model.PdfAnnotationType
import com.evora.technologies.office.core.model.PdfSignaturePlacement
import com.evora.technologies.office.core.model.PdfToolKind
import com.evora.technologies.office.core.model.PdfToolResult
import com.evora.technologies.office.core.model.Pkcs7CertificateInfo
import com.evora.technologies.office.core.model.SplitMode
import com.evora.technologies.office.core.model.WatermarkConfig
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationText
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup
import com.tom_roush.pdfbox.pdmodel.interactive.digitalsignature.PDSignature
import com.tom_roush.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.util.Matrix
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfBoxPowerEngine @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val documentRepository: DocumentRepository,
    private val pdfEngine: PdfEnginePort,
    private val scannerPort: ScannerPort,
    private val hwpEngine: HwpEnginePort,
    private val officeEngine: OfficeEnginePort,
    private val textEpubEngine: TextEpubEnginePort,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PdfPowerPort {

    @Volatile
    private var pdfBoxInitialized = false
    private val initLock = Any()
    private val bcProvider by lazy { BouncyCastleProvider() }

    private fun ensureInitialized() {
        if (!pdfBoxInitialized) {
            synchronized(initLock) {
                if (!pdfBoxInitialized) {
                    PDFBoxResourceLoader.init(context.applicationContext)
                    pdfBoxInitialized = true
                }
            }
        }
    }

    override suspend fun applyAnnotationsAndSignatures(
        sourceUriString: String,
        outputFileName: String?,
        annotations: List<PdfAnnotationDraft>,
        signatures: List<PdfSignaturePlacement>,
        password: String?,
    ): Result<PdfToolResult> = withContext(ioDispatcher) {
        try {
            ensureInitialized()
            val srcSize = measureUriBytes(sourceUriString)
            loadPdDocument(sourceUriString, password).use { doc ->
                for (annot in annotations) {
                    if (annot.pageIndex !in 0 until doc.numberOfPages) continue
                    val page = doc.getPage(annot.pageIndex)
                    val box = page.mediaBox
                    val (r, g, b) = argbToUnitRgb(annot.colorArgb)

                    when (annot.type) {
                        PdfAnnotationType.HIGHLIGHT,
                        PdfAnnotationType.UNDERLINE,
                        PdfAnnotationType.STRIKETHROUGH -> {
                            val subType = when (annot.type) {
                                PdfAnnotationType.HIGHLIGHT -> PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT
                                PdfAnnotationType.UNDERLINE -> PDAnnotationTextMarkup.SUB_TYPE_UNDERLINE
                                PdfAnnotationType.STRIKETHROUGH -> PDAnnotationTextMarkup.SUB_TYPE_STRIKEOUT
                                else -> PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT
                            }
                            val x1 = annot.left.coerceIn(0f, 1f) * box.width
                            val x2 = annot.right.coerceIn(0f, 1f) * box.width
                            val yTop = (1f - annot.top.coerceIn(0f, 1f)) * box.height
                            val yBottom = (1f - annot.bottom.coerceIn(0f, 1f)) * box.height
                            val minX = minOf(x1, x2)
                            val maxX = maxOf(x1, x2).coerceAtLeast(minX + 4f)
                            val minY = minOf(yTop, yBottom)
                            val maxY = maxOf(yTop, yBottom).coerceAtLeast(minY + 4f)

                            val markup = PDAnnotationTextMarkup(subType).apply {
                                color = PDColor(floatArrayOf(r, g, b), PDDeviceRGB.INSTANCE)
                                rectangle = PDRectangle(minX, minY, maxX - minX, maxY - minY)
                                quadPoints = floatArrayOf(minX, maxY, maxX, maxY, minX, minY, maxX, minY)
                            }
                            page.annotations.add(markup)

                            // Render crisp vector fallback in content stream so all mobile renderers display immediately
                            PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                                val gs = PDExtendedGraphicsState().apply {
                                    nonStrokingAlphaConstant = if (annot.type == PdfAnnotationType.HIGHLIGHT) 0.35f else 0.9f
                                    strokingAlphaConstant = 0.9f
                                }
                                cs.setGraphicsStateParameters(gs)
                                if (annot.type == PdfAnnotationType.HIGHLIGHT) {
                                    cs.setNonStrokingColor(r, g, b)
                                    cs.addRect(minX, minY, maxX - minX, maxY - minY)
                                    cs.fill()
                                } else {
                                    val lineY = if (annot.type == PdfAnnotationType.UNDERLINE) minY else (minY + maxY) / 2f
                                    cs.setStrokingColor(r, g, b)
                                    cs.setLineWidth(1.8f)
                                    cs.moveTo(minX, lineY)
                                    cs.lineTo(maxX, lineY)
                                    cs.stroke()
                                }
                            }
                        }

                        PdfAnnotationType.INK -> {
                            if (annot.points.size >= 2) {
                                PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                                    cs.setStrokingColor(r, g, b)
                                    cs.setLineWidth(annot.strokeWidthDp.coerceIn(1f, 16f))
                                    cs.setLineCapStyle(1)
                                    cs.setLineJoinStyle(1)
                                    val first = annot.points.first()
                                    cs.moveTo(first.x * box.width, (1f - first.y) * box.height)
                                    for (i in 1 until annot.points.size) {
                                        val pt = annot.points[i]
                                        cs.lineTo(pt.x * box.width, (1f - pt.y) * box.height)
                                    }
                                    cs.stroke()
                                }
                            }
                        }

                        PdfAnnotationType.STICKY_NOTE -> {
                            val px = annot.left.coerceIn(0.05f, 0.92f) * box.width
                            val py = (1f - annot.top.coerceIn(0.05f, 0.92f)) * box.height
                            val note = PDAnnotationText().apply {
                                rectangle = PDRectangle(px, py - 24f, 24f, 24f)
                                titlePopup = annot.author
                                contents = annot.noteText
                                name = PDAnnotationText.NAME_COMMENT
                                color = PDColor(floatArrayOf(r, g, b), PDDeviceRGB.INSTANCE)
                            }
                            page.annotations.add(note)
                        }
                    }
                }

                for (sig in signatures) {
                    if (sig.pageIndex !in 0 until doc.numberOfPages) continue
                    val sigFile = File(sig.signatureAssetPath)
                    if (!sigFile.exists()) continue
                    val bmp = BitmapFactory.decodeFile(sigFile.absolutePath) ?: continue
                    try {
                        val page = doc.getPage(sig.pageIndex)
                        val box = page.mediaBox
                        val pdImg = LosslessFactory.createFromImage(doc, bmp)
                        val drawW = (box.width * sig.widthFraction.coerceIn(0.10f, 0.70f))
                        val aspect = bmp.height.toFloat() / bmp.width.coerceAtLeast(1).toFloat()
                        val drawH = drawW * aspect
                        val x = (box.width * sig.centerXFraction - drawW / 2f).coerceIn(8f, (box.width - drawW).coerceAtLeast(8f))
                        val y = (box.height * (1f - sig.centerYFraction) - drawH / 2f).coerceIn(8f, (box.height - drawH).coerceAtLeast(8f))
                        PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                            cs.drawImage(pdImg, x, y, drawW, drawH)
                        }
                    } finally {
                        bmp.recycle()
                    }
                }

                val targetName = outputFileName?.takeIf { it.isNotBlank() } ?: "Annotated_${System.currentTimeMillis()}.pdf"
                val outFile = uniqueExportFile(if (targetName.endsWith(".pdf", true)) targetName else "$targetName.pdf")
                FileOutputStream(outFile).use { doc.save(it) }
                val outUri = outFile.toURI().toString()
                documentRepository.registerSafUri(outUri)
                Result.success(
                    PdfToolResult(
                        toolKind = PdfToolKind.PKCS7_SIGN,
                        outputUriString = outUri,
                        outputDisplayName = outFile.name,
                        beforeSizeBytes = srcSize,
                        afterSizeBytes = outFile.length(),
                        summaryMessage = "Saved PDF with ${annotations.size} annotation(s) & ${signatures.size} signature(s)",
                    ),
                )
            }
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun inspectPkcs7Signatures(
        uriString: String,
        password: String?,
    ): Result<List<Pkcs7CertificateInfo>> = withContext(ioDispatcher) {
        try {
            ensureInitialized()
            val rawBytes = readUriBytes(uriString)
            loadPdDocument(rawBytes, password).use { doc ->
                val dicts = doc.signatureDictionaries
                if (dicts.isEmpty()) return@withContext Result.success(emptyList())

                val out = mutableListOf<Pkcs7CertificateInfo>()
                for (sig in dicts) {
                    val contents = runCatching { sig.getContents(rawBytes) }.getOrNull()
                    val signedContent = runCatching { sig.getSignedContent(rawBytes) }.getOrNull()
                    if (contents == null || contents.isEmpty()) {
                        out += Pkcs7CertificateInfo(
                            signatureCount = dicts.size,
                            signerName = sig.name ?: "Digital Signer",
                            issuerName = "Embedded Certificate",
                            serialHex = "-",
                            validFromEpochMs = 0L,
                            validToEpochMs = 0L,
                            signedAtEpochMs = sig.signDate?.timeInMillis,
                            isIntegrityValid = false,
                            isCurrentlyWithinValidity = false,
                            reason = sig.reason.orEmpty(),
                        )
                        continue
                    }
                    val cms = CMSSignedData(
                        CMSProcessableByteArray(signedContent ?: ByteArray(0)),
                        contents,
                    )
                    val signer = cms.signerInfos.signers.firstOrNull()
                    val allCerts = cms.certificates.getMatches(null).mapNotNull { it as? X509CertificateHolder }
                    val holder = allCerts.firstOrNull { signer?.sid?.match(it) == true } ?: allCerts.firstOrNull()
                    val verifierValid = if (signer != null && holder != null) {
                        runCatching {
                            val verifier = JcaSimpleSignerInfoVerifierBuilder()
                                .setProvider(bcProvider)
                                .build(holder)
                            signer.verify(verifier)
                        }.getOrDefault(false)
                    } else {
                        false
                    }
                    val now = Date()
                    val validNow = holder != null && !now.before(holder.notBefore) && !now.after(holder.notAfter)
                    out += Pkcs7CertificateInfo(
                        signatureCount = dicts.size,
                        signerName = holder?.subject?.toString() ?: (sig.name ?: "Unknown"),
                        issuerName = holder?.issuer?.toString() ?: "Self-Signed / Unknown CA",
                        serialHex = holder?.serialNumber?.toString(16)?.uppercase() ?: "-",
                        validFromEpochMs = holder?.notBefore?.time ?: 0L,
                        validToEpochMs = holder?.notAfter?.time ?: 0L,
                        signedAtEpochMs = sig.signDate?.timeInMillis,
                        isIntegrityValid = verifierValid,
                        isCurrentlyWithinValidity = validNow,
                        reason = sig.reason.orEmpty(),
                    )
                }
                Result.success(out)
            }
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun signWithPkcs12(
        sourceUriString: String,
        p12UriString: String,
        p12Password: String,
        reason: String,
        location: String,
        pdfPassword: String?,
    ): Result<PdfToolResult> = withContext(ioDispatcher) {
        try {
            ensureInitialized()
            val p12Bytes = readUriBytes(p12UriString)
            val keyStore = KeyStore.getInstance("PKCS12")
            p12Bytes.inputStream().use { keyStore.load(it, p12Password.toCharArray()) }
            val alias = keyStore.aliases().toList().firstOrNull { keyStore.isKeyEntry(it) }
                ?: return@withContext Result.failure(IllegalArgumentException("No signing key found in .p12/.pfx"))
            val privateKey = keyStore.getKey(alias, p12Password.toCharArray()) as PrivateKey
            val chain = keyStore.getCertificateChain(alias)
                ?.mapNotNull { it as? X509Certificate }
                ?: return@withContext Result.failure(IllegalArgumentException("Certificate chain missing in keystore"))

            val pdfBytes = readUriBytes(sourceUriString)
            val baseName = deriveBaseName(sourceUriString, "Document")
            val outFile = uniqueExportFile("${baseName}_signed.pdf")

            loadPdDocument(pdfBytes, pdfPassword).use { doc ->
                val signature = PDSignature().apply {
                    setFilter(PDSignature.FILTER_ADOBE_PPKLITE)
                    setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED)
                    setName(chain.first().subjectX500Principal.name)
                    setReason(reason.ifBlank { "Approved with Evora Office" })
                    setLocation(location.ifBlank { "Mobile Device" })
                    setSignDate(Calendar.getInstance())
                }
                val signerInterface = SignatureInterface { content: InputStream ->
                    val data = content.readBytes()
                    val sigAlg = if (privateKey.algorithm.equals("EC", true)) "SHA256withECDSA" else "SHA256withRSA"
                    val gen = CMSSignedDataGenerator()
                    val contentSigner = JcaContentSignerBuilder(sigAlg)
                        .setProvider(bcProvider)
                        .build(privateKey)
                    val digestProvider = JcaDigestCalculatorProviderBuilder()
                        .setProvider(bcProvider)
                        .build()
                    gen.addSignerInfoGenerator(JcaSignerInfoGeneratorBuilder(digestProvider).build(contentSigner, chain.first()))
                    gen.addCertificates(JcaCertStore(chain))
                    val signedData = gen.generate(CMSProcessableByteArray(data), false)
                    signedData.encoded
                }
                doc.addSignature(signature, signerInterface)
                FileOutputStream(outFile).use { fos ->
                    doc.saveIncremental(fos)
                }
            }

            val outUri = outFile.toURI().toString()
            documentRepository.registerSafUri(outUri)
            Result.success(
                PdfToolResult(
                    toolKind = PdfToolKind.PKCS7_SIGN,
                    outputUriString = outUri,
                    outputDisplayName = outFile.name,
                    beforeSizeBytes = pdfBytes.size.toLong(),
                    afterSizeBytes = outFile.length(),
                    summaryMessage = "Signed with PKCS#7 certificate (${chain.first().subjectX500Principal.name})",
                ),
            )
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun setPassword(
        sourceUriString: String,
        userPassword: String,
        ownerPassword: String,
        useAes256: Boolean,
        currentPassword: String?,
    ): Result<PdfToolResult> = withContext(ioDispatcher) {
        try {
            ensureInitialized()
            if (userPassword.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Enter a password"))
            }
            val srcBytes = measureUriBytes(sourceUriString)
            val outFile = uniqueExportFile("${deriveBaseName(sourceUriString, "Protected")}_locked.pdf")
            loadPdDocument(sourceUriString, currentPassword).use { doc ->
                val effectiveOwner = ownerPassword.ifBlank { userPassword }
                val policy = StandardProtectionPolicy(effectiveOwner, userPassword, AccessPermission()).apply {
                    encryptionKeyLength = if (useAes256) 256 else 128
                    isPreferAES = true
                }
                doc.protect(policy)
                FileOutputStream(outFile).use { doc.save(it) }
            }
            val outUri = outFile.toURI().toString()
            documentRepository.registerSafUri(outUri)
            Result.success(
                PdfToolResult(
                    toolKind = PdfToolKind.SET_PASSWORD,
                    outputUriString = outUri,
                    outputDisplayName = outFile.name,
                    beforeSizeBytes = srcBytes,
                    afterSizeBytes = outFile.length(),
                    summaryMessage = "Protected with AES-${if (useAes256) 256 else 128} encryption",
                ),
            )
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun removePassword(
        sourceUriString: String,
        currentPassword: String,
    ): Result<PdfToolResult> = withContext(ioDispatcher) {
        try {
            ensureInitialized()
            val srcBytes = measureUriBytes(sourceUriString)
            val outFile = uniqueExportFile("${deriveBaseName(sourceUriString, "Unlocked")}_unlocked.pdf")
            loadPdDocument(sourceUriString, currentPassword).use { doc ->
                doc.isAllSecurityToBeRemoved = true
                FileOutputStream(outFile).use { doc.save(it) }
            }
            val outUri = outFile.toURI().toString()
            documentRepository.registerSafUri(outUri)
            Result.success(
                PdfToolResult(
                    toolKind = PdfToolKind.REMOVE_PASSWORD,
                    outputUriString = outUri,
                    outputDisplayName = outFile.name,
                    beforeSizeBytes = srcBytes,
                    afterSizeBytes = outFile.length(),
                    summaryMessage = "PDF password protection removed cleanly",
                ),
            )
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun mergePdfs(
        sourceUris: List<String>,
        outputBaseName: String,
    ): Result<PdfToolResult> = withContext(ioDispatcher) {
        try {
            ensureInitialized()
            if (sourceUris.size < 2) {
                return@withContext Result.failure(IllegalArgumentException("Select at least 2 PDF files to merge"))
            }
            val tempInputs = mutableListOf<File>()
            var totalBefore = 0L
            try {
                val merger = PDFMergerUtility()
                val outFile = uniqueExportFile("${sanitizeBase(outputBaseName, "Merged_PDF")}.pdf")
                merger.destinationFileName = outFile.absolutePath
                for (uri in sourceUris) {
                    val temp = copyUriToTempFile(uri, ".pdf")
                    tempInputs += temp
                    totalBefore += temp.length()
                    merger.addSource(temp)
                }
                merger.mergeDocuments(MemoryUsageSetting.setupTempFileOnly())
                val outUri = outFile.toURI().toString()
                documentRepository.registerSafUri(outUri)
                Result.success(
                    PdfToolResult(
                        toolKind = PdfToolKind.MERGE,
                        outputUriString = outUri,
                        outputDisplayName = outFile.name,
                        beforeSizeBytes = totalBefore,
                        afterSizeBytes = outFile.length(),
                        summaryMessage = "Merged ${sourceUris.size} PDF files",
                    ),
                )
            } finally {
                tempInputs.forEach { it.delete() }
            }
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun splitPdf(
        sourceUriString: String,
        mode: SplitMode,
        rangeExpression: String,
        everyNPages: Int,
        selectedZeroBasedPages: Set<Int>,
        password: String?,
        onProgress: (Float) -> Unit,
    ): Result<PdfToolResult> = withContext(ioDispatcher) {
        try {
            ensureInitialized()
            val srcSize = measureUriBytes(sourceUriString)
            val baseName = deriveBaseName(sourceUriString, "Split")
            loadPdDocument(sourceUriString, password).use { srcDoc ->
                val totalPages = srcDoc.numberOfPages
                val groups: List<List<Int>> = when (mode) {
                    SplitMode.RANGE -> {
                        val indices = PageRangeParser.parseOneBasedRanges(rangeExpression, totalPages).getOrThrow()
                        listOf(indices)
                    }
                    SplitMode.EVERY_N -> PageRangeParser.splitEveryN(totalPages, everyNPages)
                    SplitMode.SELECTED_PAGES -> {
                        val valid = selectedZeroBasedPages.filter { it in 0 until totalPages }.sorted()
                        if (valid.isEmpty()) {
                            return@withContext Result.failure(IllegalArgumentException("Select at least one page"))
                        }
                        listOf(valid)
                    }
                }

                val outputs = mutableListOf<File>()
                groups.forEachIndexed { idx, pages ->
                    onProgress((idx + 1).toFloat() / groups.size.toFloat())
                    PDDocument().use { partDoc ->
                        for (pageIdx in pages) {
                            partDoc.importPage(srcDoc.getPage(pageIdx))
                        }
                        val suffix = if (groups.size == 1) "extracted" else "part_${idx + 1}"
                        val partFile = uniqueExportFile("${baseName}_$suffix.pdf")
                        FileOutputStream(partFile).use { partDoc.save(it) }
                        documentRepository.registerSafUri(partFile.toURI().toString())
                        outputs += partFile
                    }
                }

                val primary = outputs.first()
                val totalAfter = outputs.sumOf { it.length() }
                Result.success(
                    PdfToolResult(
                        toolKind = PdfToolKind.SPLIT,
                        outputUriString = primary.toURI().toString(),
                        outputDisplayName = primary.name,
                        beforeSizeBytes = srcSize,
                        afterSizeBytes = totalAfter,
                        summaryMessage = "Created ${outputs.size} PDF file(s) from $totalPages pages",
                        extraOutputUris = outputs.map { it.toURI().toString() },
                    ),
                )
            }
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun compressPdf(
        sourceUriString: String,
        quality: CompressQuality,
        password: String?,
        onProgress: (Float) -> Unit,
    ): Result<PdfToolResult> = withContext(ioDispatcher) {
        try {
            ensureInitialized()
            val srcSize = measureUriBytes(sourceUriString)
            val baseName = deriveBaseName(sourceUriString, "Document")
            val outFile = uniqueExportFile("${baseName}_compressed.pdf")

            loadPdDocument(sourceUriString, password).use { doc ->
                val pageTotal = doc.numberOfPages.coerceAtLeast(1)
                val maxPx = when (quality) {
                    CompressQuality.LOW -> 1400
                    CompressQuality.MEDIUM -> 1050
                    CompressQuality.HIGH -> 720
                }
                for (i in 0 until doc.numberOfPages) {
                    onProgress((i + 1).toFloat() / pageTotal.toFloat())
                    val page = doc.getPage(i)
                    val resources = page.resources ?: continue
                    val names = resources.xObjectNames?.toList().orEmpty()
                    for (name in names) {
                        val xObj = runCatching { resources.getXObject(name) }.getOrNull()
                        if (xObj is PDImageXObject) {
                            runCatching {
                                val srcBmp = xObj.image ?: return@runCatching
                                val scaled = scaleDownBitmap(srcBmp, maxPx)
                                try {
                                    val recompressed = JPEGFactory.createFromImage(doc, scaled, quality.jpegQuality)
                                    resources.put(name, recompressed)
                                } finally {
                                    if (scaled !== srcBmp) scaled.recycle()
                                    srcBmp.recycle()
                                }
                            }
                        }
                    }
                }
                FileOutputStream(outFile).use { doc.save(it) }
            }

            val outUri = outFile.toURI().toString()
            documentRepository.registerSafUri(outUri)
            Result.success(
                PdfToolResult(
                    toolKind = PdfToolKind.COMPRESS,
                    outputUriString = outUri,
                    outputDisplayName = outFile.name,
                    beforeSizeBytes = srcSize,
                    afterSizeBytes = outFile.length(),
                    summaryMessage = "Compressed using ${quality.label} (${quality.targetDpi} DPI)",
                ),
            )
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun watermarkPdf(
        sourceUriString: String,
        config: WatermarkConfig,
        password: String?,
        onProgress: (Float) -> Unit,
    ): Result<PdfToolResult> = withContext(ioDispatcher) {
        try {
            ensureInitialized()
            val safeText = sanitizeWinAnsi(config.text).ifBlank { "CONFIDENTIAL" }
            val srcSize = measureUriBytes(sourceUriString)
            val outFile = uniqueExportFile("${deriveBaseName(sourceUriString, "Document")}_watermarked.pdf")

            loadPdDocument(sourceUriString, password).use { doc ->
                val font = PDType1Font.HELVETICA_BOLD
                val (r, g, b) = argbToUnitRgb(config.colorArgb)
                val radians = Math.toRadians(config.angleDegrees.toDouble())
                val pageTotal = doc.numberOfPages.coerceAtLeast(1)

                for (i in 0 until doc.numberOfPages) {
                    onProgress((i + 1).toFloat() / pageTotal.toFloat())
                    val page = doc.getPage(i)
                    val box = page.mediaBox
                    PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                        val gs = PDExtendedGraphicsState().apply {
                            nonStrokingAlphaConstant = config.opacity.coerceIn(0.08f, 1.0f)
                        }
                        cs.setGraphicsStateParameters(gs)
                        cs.setNonStrokingColor(r, g, b)
                        cs.setFont(font, config.fontSizePt.coerceIn(14f, 96f))

                        val textWidth = font.getStringWidth(safeText) / 1000f * config.fontSizePt.coerceIn(14f, 96f)
                        if (config.tiled) {
                            val stepX = (textWidth + 90f).coerceAtLeast(180f)
                            val stepY = 160f
                            var y = 80f
                            while (y < box.height) {
                                var x = 80f
                                while (x < box.width) {
                                    drawRotatedWatermark(cs, safeText, x, y, radians, textWidth)
                                    x += stepX
                                }
                                y += stepY
                            }
                        } else {
                            drawRotatedWatermark(cs, safeText, box.width / 2f, box.height / 2f, radians, textWidth)
                        }
                    }
                }
                FileOutputStream(outFile).use { doc.save(it) }
            }

            val outUri = outFile.toURI().toString()
            documentRepository.registerSafUri(outUri)
            Result.success(
                PdfToolResult(
                    toolKind = PdfToolKind.WATERMARK,
                    outputUriString = outUri,
                    outputDisplayName = outFile.name,
                    beforeSizeBytes = srcSize,
                    afterSizeBytes = outFile.length(),
                    summaryMessage = "Applied watermark \"$safeText\"",
                ),
            )
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun imagesToPdf(
        imageUris: List<String>,
        outputBaseName: String,
        pageSize: PageSizePreset,
        marginDp: Int,
        landscape: Boolean,
        onProgress: (Float) -> Unit,
    ): Result<PdfToolResult> = withContext(ioDispatcher) {
        try {
            ensureInitialized()
            if (imageUris.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Select at least one image"))
            }
            val outFile = uniqueExportFile("${sanitizeBase(outputBaseName, "Images_to_PDF")}.pdf")
            var beforeBytes = 0L
            PDDocument().use { doc ->
                imageUris.forEachIndexed { idx, uriStr ->
                    onProgress((idx + 1).toFloat() / imageUris.size.toFloat())
                    beforeBytes += measureUriBytes(uriStr)
                    val bmp = decodeUriBitmap(uriStr, maxDim = 2200) ?: return@forEachIndexed
                    try {
                        val rect = resolvePageRect(pageSize, landscape, bmp.width.toFloat(), bmp.height.toFloat())
                        val page = PDPage(rect)
                        doc.addPage(page)
                        val pdImage = JPEGFactory.createFromImage(doc, bmp, 0.88f)
                        val marginPt = if (pageSize == PageSizePreset.FIT) 0f else marginDp.coerceIn(0, 72).toFloat()
                        val availW = (rect.width - marginPt * 2f).coerceAtLeast(32f)
                        val availH = (rect.height - marginPt * 2f).coerceAtLeast(32f)
                        val scale = minOf(availW / bmp.width.toFloat(), availH / bmp.height.toFloat())
                        val drawW = bmp.width * scale
                        val drawH = bmp.height * scale
                        val drawX = (rect.width - drawW) / 2f
                        val drawY = (rect.height - drawH) / 2f
                        PDPageContentStream(doc, page).use { cs ->
                            cs.drawImage(pdImage, drawX, drawY, drawW, drawH)
                        }
                    } finally {
                        bmp.recycle()
                    }
                }
                if (doc.numberOfPages == 0) {
                    return@withContext Result.failure(IllegalArgumentException("No readable images were found"))
                }
                FileOutputStream(outFile).use { doc.save(it) }
            }
            val outUri = outFile.toURI().toString()
            documentRepository.registerSafUri(outUri)
            Result.success(
                PdfToolResult(
                    toolKind = PdfToolKind.IMAGE_TO_PDF,
                    outputUriString = outUri,
                    outputDisplayName = outFile.name,
                    beforeSizeBytes = beforeBytes,
                    afterSizeBytes = outFile.length(),
                    summaryMessage = "Converted ${imageUris.size} image(s) to PDF",
                ),
            )
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun textToPdf(
        textContent: String,
        outputBaseName: String,
        pageSize: PageSizePreset,
        onProgress: (Float) -> Unit,
    ): Result<PdfToolResult> = withContext(ioDispatcher) {
        try {
            ensureInitialized()
            val outFile = uniqueExportFile("${sanitizeBase(outputBaseName, "Text_Document")}.pdf")
            val rect = resolvePageRect(pageSize, landscape = false, fitW = PDRectangle.A4.width, fitH = PDRectangle.A4.height)
            writeLinesToPdf(
                lines = textContent.lines(),
                rect = rect,
                outFile = outFile,
                onProgress = onProgress,
            )
            val outUri = outFile.toURI().toString()
            documentRepository.registerSafUri(outUri)
            Result.success(
                PdfToolResult(
                    toolKind = PdfToolKind.TEXT_TO_PDF,
                    outputUriString = outUri,
                    outputDisplayName = outFile.name,
                    beforeSizeBytes = textContent.toByteArray().size.toLong(),
                    afterSizeBytes = outFile.length(),
                    summaryMessage = "Converted text to paginated PDF",
                ),
            )
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun documentToPdf(
        sourceUriString: String,
        onProgress: (Float) -> Unit,
    ): Result<PdfToolResult> = withContext(ioDispatcher) {
        try {
            ensureInitialized()
            val srcSize = measureUriBytes(sourceUriString)
            val base = deriveBaseName(sourceUriString, "Converted")
            val ext = sourceUriString.substringAfterLast('.', "").lowercase()
            val format = DocFormat.fromExtension(ext) ?: DocFormat.WORD
            onProgress(0.25f)
            val outFile = uniqueExportFile("${base}_converted.pdf")
            val convertedNatively = if (format in setOf(DocFormat.WORD, DocFormat.EXCEL, DocFormat.PPT, DocFormat.ODF)) {
                officeEngine.convertToPdfIfAvailable(sourceUriString, outFile.absolutePath)
            } else {
                false
            }
            if (!convertedNatively) {
                val plainText = when (format) {
                    DocFormat.HWP -> hwpEngine.renderToSanitizedHtml(sourceUriString).plainText
                    DocFormat.WORD, DocFormat.EXCEL, DocFormat.PPT, DocFormat.ODF ->
                        officeEngine.extractReadablePreview(sourceUriString, format).fullPlainText
                    DocFormat.EPUB, DocFormat.TEXT ->
                        textEpubEngine.loadDocument(sourceUriString, format).fullPlainText
                    else -> "Unsupported source format for PDF conversion"
                }
                onProgress(0.60f)
                writeLinesToPdf(
                    lines = plainText.lines().ifEmpty { listOf(base) },
                    rect = PDRectangle.A4,
                    outFile = outFile,
                    onProgress = { p -> onProgress(0.60f + p * 0.40f) },
                )
            } else {
                onProgress(1.0f)
            }
            val outUri = outFile.toURI().toString()
            documentRepository.registerSafUri(outUri)
            Result.success(
                PdfToolResult(
                    toolKind = PdfToolKind.DOC_TO_PDF,
                    outputUriString = outUri,
                    outputDisplayName = outFile.name,
                    beforeSizeBytes = srcSize,
                    afterSizeBytes = outFile.length(),
                    summaryMessage = "Converted $ext offline to PDF",
                ),
            )
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun pdfToImages(
        sourceUriString: String,
        stitchedLongImage: Boolean,
        password: String?,
        onProgress: (Float) -> Unit,
    ): Result<PdfToolResult> = withContext(ioDispatcher) {
        try {
            val srcSize = measureUriBytes(sourceUriString)
            val base = deriveBaseName(sourceUriString, "Page")
            val pageCount = pdfEngine.getPageCount(sourceUriString, password).coerceAtLeast(1)
            val bitmaps = mutableListOf<Bitmap>()
            val outUris = mutableListOf<String>()

            try {
                for (i in 0 until pageCount) {
                    onProgress((i + 1).toFloat() / pageCount.toFloat())
                    val raster = pdfEngine.renderPage(sourceUriString, i, targetWidthPx = 1080, password = password)
                    val bmp = Bitmap.createBitmap(raster.width, raster.height, Bitmap.Config.ARGB_8888)
                    bmp.setPixels(raster.argbPixels, 0, raster.width, 0, 0, raster.width, raster.height)
                    if (stitchedLongImage) {
                        bitmaps += bmp
                    } else {
                        val file = uniqueExportFile("${base}_p${i + 1}.png")
                        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
                        bmp.recycle()
                        outUris += file.toURI().toString()
                    }
                }

                if (stitchedLongImage) {
                    val totalW = bitmaps.maxOf { it.width }
                    val totalH = bitmaps.sumOf { it.height }.coerceAtMost(14_000)
                    val stitched = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
                    try {
                        val canvas = Canvas(stitched)
                        canvas.drawColor(Color.WHITE)
                        var yOffset = 0f
                        for (bmp in bitmaps) {
                            if (yOffset >= totalH) break
                            canvas.drawBitmap(bmp, 0f, yOffset, null)
                            yOffset += bmp.height
                        }
                        val longFile = uniqueExportFile("${base}_long.png")
                        FileOutputStream(longFile).use { stitched.compress(Bitmap.CompressFormat.PNG, 95, it) }
                        return@withContext Result.success(
                            PdfToolResult(
                                toolKind = PdfToolKind.PDF_TO_IMAGES,
                                outputUriString = longFile.toURI().toString(),
                                outputDisplayName = longFile.name,
                                beforeSizeBytes = srcSize,
                                afterSizeBytes = longFile.length(),
                                summaryMessage = "Exported ${bitmaps.size} page(s) as Long PNG",
                                extraOutputUris = listOf(longFile.toURI().toString()),
                            ),
                        )
                    } finally {
                        stitched.recycle()
                    }
                } else {
                    val firstFile = File(Uri.parse(outUris.first()).path!!)
                    val totalSize = outUris.sumOf { File(Uri.parse(it).path!!).length() }
                    return@withContext Result.success(
                        PdfToolResult(
                            toolKind = PdfToolKind.PDF_TO_IMAGES,
                            outputUriString = outUris.first(),
                            outputDisplayName = firstFile.name,
                            beforeSizeBytes = srcSize,
                            afterSizeBytes = totalSize,
                            summaryMessage = "Exported $pageCount PNG image(s)",
                            extraOutputUris = outUris,
                        ),
                    )
                }
            } finally {
                bitmaps.forEach { it.recycle() }
            }
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun pdfToDocxOffline(
        sourceUriString: String,
        password: String?,
        onProgress: (Float) -> Unit,
    ): Result<PdfToolResult> = withContext(ioDispatcher) {
        try {
            ensureInitialized()
            val srcSize = measureUriBytes(sourceUriString)
            val base = deriveBaseName(sourceUriString, "Converted")
            val pagesLines = mutableListOf<List<String>>()
            var usedOcrPages = 0

            loadPdDocument(sourceUriString, password).use { doc ->
                val pageCount = doc.numberOfPages.coerceAtLeast(1)
                val stripper = PDFTextStripper()
                for (pageNum in 1..doc.numberOfPages) {
                    onProgress(pageNum.toFloat() / pageCount.toFloat())
                    stripper.startPage = pageNum
                    stripper.endPage = pageNum
                    val rawText = stripper.getText(doc).trim()
                    if (rawText.isNotBlank()) {
                        pagesLines += rawText.lines().map { it.trimEnd() }.filter { it.isNotEmpty() }
                    } else {
                        // Offline ML Kit OCR fallback for scanned image-only PDF pages (03_PRD.md §1.2)
                        val raster = runCatching {
                            pdfEngine.renderPage(sourceUriString, pageNum - 1, targetWidthPx = 1240, password = password)
                        }.getOrNull()
                        if (raster != null) {
                            val tmpPng = File.createTempFile("pdf_ocr_", ".png", context.cacheDir)
                            val bmp = Bitmap.createBitmap(raster.width, raster.height, Bitmap.Config.ARGB_8888)
                            try {
                                bmp.setPixels(raster.argbPixels, 0, raster.width, 0, 0, raster.width, raster.height)
                                FileOutputStream(tmpPng).use { bmp.compress(Bitmap.CompressFormat.PNG, 92, it) }
                                val ocr = scannerPort.recognizeTextFromFile(tmpPng.absolutePath, OcrScript.LATIN).getOrNull()
                                val extracted = ocr?.text.orEmpty()
                                if (extracted.isNotBlank()) {
                                    usedOcrPages++
                                    pagesLines += extracted.lines().filter { it.isNotBlank() }
                                } else {
                                    pagesLines += listOf("[Page $pageNum: Blank or unrecognized scan]")
                                }
                            } finally {
                                bmp.recycle()
                                tmpPng.delete()
                            }
                        }
                    }
                }
            }

            if (pagesLines.isEmpty()) {
                pagesLines += listOf("Converted from $base")
            }

            val docxBytes = OoxmlDocxWriter.buildFromPlainParagraphs(base, pagesLines)
            val outFile = uniqueExportFile("${base}.docx")
            FileOutputStream(outFile).use { it.write(docxBytes) }
            val outUri = outFile.toURI().toString()
            documentRepository.registerSafUri(outUri)
            val note = if (usedOcrPages > 0) " (with offline OCR on $usedOcrPages scanned page(s))" else ""
            Result.success(
                PdfToolResult(
                    toolKind = PdfToolKind.PDF_TO_DOCX,
                    outputUriString = outUri,
                    outputDisplayName = outFile.name,
                    beforeSizeBytes = srcSize,
                    afterSizeBytes = outFile.length(),
                    summaryMessage = "Converted offline to ${outFile.name}$note",
                ),
            )
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun writeLinesToPdf(
        lines: List<String>,
        rect: PDRectangle,
        outFile: File,
        onProgress: (Float) -> Unit,
    ) {
        val margin = 44f
        val fontSize = 11f
        val lineHeight = 15f
        val font = PDType1Font.HELVETICA
        val maxTextWidth = rect.width - margin * 2f

        val wrappedLines = mutableListOf<String>()
        for (rawLine in lines) {
            val clean = sanitizeWinAnsi(rawLine)
            if (clean.isEmpty()) {
                wrappedLines += ""
                continue
            }
            var current = StringBuilder()
            for (word in clean.split(' ')) {
                val candidate = if (current.isEmpty()) word else "$current $word"
                val w = font.getStringWidth(candidate) / 1000f * fontSize
                if (w > maxTextWidth && current.isNotEmpty()) {
                    wrappedLines += current.toString()
                    current = StringBuilder(word)
                } else {
                    current = StringBuilder(candidate)
                }
            }
            if (current.isNotEmpty()) wrappedLines += current.toString()
        }
        if (wrappedLines.isEmpty()) wrappedLines += ""

        val linesPerPage = ((rect.height - margin * 2f) / lineHeight).toInt().coerceAtLeast(1)
        val chunks = wrappedLines.chunked(linesPerPage)

        PDDocument().use { doc ->
            chunks.forEachIndexed { idx, chunk ->
                onProgress((idx + 1).toFloat() / chunks.size.coerceAtLeast(1).toFloat())
                val page = PDPage(rect)
                doc.addPage(page)
                PDPageContentStream(doc, page).use { cs ->
                    cs.beginText()
                    cs.setFont(font, fontSize)
                    cs.newLineAtOffset(margin, rect.height - margin - fontSize)
                    for (line in chunk) {
                        cs.showText(line)
                        cs.newLineAtOffset(0f, -lineHeight)
                    }
                    cs.endText()
                }
            }
            FileOutputStream(outFile).use { doc.save(it) }
        }
    }

    private fun drawRotatedWatermark(
        cs: PDPageContentStream,
        text: String,
        centerX: Float,
        centerY: Float,
        radians: Double,
        textWidth: Float,
    ) {
        cs.beginText()
        val matrix = Matrix.getRotateInstance(radians, centerX, centerY)
        matrix.translate(-textWidth / 2f, 0f)
        cs.setTextMatrix(matrix)
        cs.showText(text)
        cs.endText()
    }

    private fun loadPdDocument(uriString: String, password: String?): PDDocument {
        val bytes = readUriBytes(uriString)
        return loadPdDocument(bytes, password)
    }

    private fun loadPdDocument(bytes: ByteArray, password: String?): PDDocument = try {
        if (password != null) {
            PDDocument.load(bytes.inputStream(), password)
        } else {
            PDDocument.load(bytes.inputStream())
        }
    } catch (_: InvalidPasswordException) {
        if (password == null) {
            throw DocumentPasswordRequiredException("Password required to unlock PDF")
        } else {
            throw DocumentInvalidPasswordException("Wrong password — please try again")
        }
    }

    private fun readUriBytes(uriString: String): ByteArray {
        val uri = Uri.parse(uriString)
        val stream = if (uri.scheme == "file" && uri.path != null) {
            FileInputStream(File(uri.path!!))
        } else {
            context.contentResolver.openInputStream(uri)
                ?: runCatching { FileInputStream(File(uriString)) }.getOrNull()
                ?: error("Cannot read URI: $uriString")
        }
        return stream.use { it.readBytes() }
    }

    private fun measureUriBytes(uriString: String): Long {
        val uri = Uri.parse(uriString)
        if (uri.scheme == "file" && uri.path != null) {
            return File(uri.path!!).length()
        }
        return runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
        }.getOrDefault(0L)
    }

    private fun copyUriToTempFile(uriString: String, suffix: String): File {
        val tmp = File.createTempFile("evora_pdf_", suffix, context.cacheDir)
        val bytes = readUriBytes(uriString)
        FileOutputStream(tmp).use { it.write(bytes) }
        return tmp
    }

    private fun decodeUriBitmap(uriString: String, maxDim: Int): Bitmap? {
        val bytes = readUriBytes(uriString)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) {
            sample *= 2
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun scaleDownBitmap(source: Bitmap, maxDim: Int): Bitmap {
        val maxSide = maxOf(source.width, source.height)
        if (maxSide <= maxDim) return source
        val ratio = maxDim.toFloat() / maxSide.toFloat()
        return Bitmap.createScaledBitmap(
            source,
            (source.width * ratio).toInt().coerceAtLeast(1),
            (source.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
    }

    private fun resolvePageRect(preset: PageSizePreset, landscape: Boolean, fitW: Float, fitH: Float): PDRectangle {
        val base = when (preset) {
            PageSizePreset.A4 -> PDRectangle.A4
            PageSizePreset.LETTER -> PDRectangle.LETTER
            PageSizePreset.FIT -> return PDRectangle(fitW.coerceAtLeast(100f), fitH.coerceAtLeast(100f))
        }
        return if (landscape) PDRectangle(base.height, base.width) else base
    }

    private fun argbToUnitRgb(argb: Long): Triple<Float, Float, Float> {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        return Triple(r, g, b)
    }

    private fun sanitizeWinAnsi(raw: String): String = buildString(raw.length) {
        for (ch in raw) {
            if (ch.code in 32..126 || ch.code in 160..255) append(ch) else if (ch == '\t') append(' ')
        }
    }

    private fun deriveBaseName(uriString: String, fallback: String): String {
        val seg = Uri.parse(uriString).lastPathSegment?.substringAfterLast('/') ?: fallback
        val base = seg.substringBeforeLast('.')
        return sanitizeBase(base, fallback)
    }

    private fun sanitizeBase(raw: String, fallback: String): String = raw.trim()
        .replace(Regex("[/\\\\:*?\"<>|\\x00-\\x1F]"), "_")
        .ifBlank { fallback }

    private fun uniqueExportFile(fileName: String): File {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: File(context.filesDir, "documents")
        val outDir = File(baseDir, "EvoraExports").apply { mkdirs() }
        val dot = fileName.lastIndexOf('.')
        val base = if (dot > 0) fileName.substring(0, dot) else fileName
        val ext = if (dot > 0) fileName.substring(dot) else ""
        var candidate = File(outDir, fileName)
        var idx = 1
        while (candidate.exists()) {
            candidate = File(outDir, "${base}_$idx$ext")
            idx++
        }
        return candidate
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PdfPowerModule {
    @Binds
    @Singleton
    abstract fun bindPdfPowerPort(impl: PdfBoxPowerEngine): PdfPowerPort
}
