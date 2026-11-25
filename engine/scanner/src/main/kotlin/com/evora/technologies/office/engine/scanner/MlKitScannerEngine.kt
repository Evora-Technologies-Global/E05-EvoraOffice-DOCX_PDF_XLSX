package com.evora.technologies.office.engine.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import com.evora.technologies.office.core.data.di.IoDispatcher
import com.evora.technologies.office.core.domain.port.DocumentRepository
import com.evora.technologies.office.core.domain.port.OcrExtractionResult
import com.evora.technologies.office.core.domain.port.ScannerPort
import com.evora.technologies.office.core.domain.util.OoxmlDocxWriter
import com.evora.technologies.office.core.domain.util.QrPayloadParser
import com.evora.technologies.office.core.model.OcrScript
import com.evora.technologies.office.core.model.PdfToolKind
import com.evora.technologies.office.core.model.PdfToolResult
import com.evora.technologies.office.core.model.QrDecodedAction
import com.evora.technologies.office.core.model.ScanFilter
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class MlKitScannerEngine @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val documentRepository: DocumentRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ScannerPort {

    override suspend fun recognizeTextFromFile(
        imagePath: String,
        script: OcrScript,
    ): Result<OcrExtractionResult> = withContext(ioDispatcher) {
        try {
            val bitmap = decodeBoundedBitmap(imagePath, maxDimPx = 2200)
                ?: return@withContext Result.failure(IllegalArgumentException("Unable to decode image for OCR"))
            try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val recognizer = when (script) {
                    OcrScript.LATIN -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    OcrScript.CHINESE -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                    OcrScript.DEVANAGARI -> TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
                    OcrScript.JAPANESE -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
                    OcrScript.KOREAN -> TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
                }
                val visionText = recognizer.use { client ->
                    client.process(inputImage).awaitResult()
                }
                Result.success(
                    OcrExtractionResult(
                        script = script,
                        text = visionText.text.trim(),
                        blockCount = visionText.textBlocks.size,
                    ),
                )
            } finally {
                bitmap.recycle()
            }
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun decodeBarcodesFromFile(imagePath: String): Result<List<QrDecodedAction>> =
        withContext(ioDispatcher) {
            try {
                val bitmap = decodeBoundedBitmap(imagePath, maxDimPx = 1800)
                    ?: return@withContext Result.failure(IllegalArgumentException("Unable to decode image"))
                try {
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val barcodes = BarcodeScanning.getClient().use { scanner ->
                        scanner.process(image).awaitResult()
                    }
                    val decoded = barcodes.mapNotNull { it.rawValue?.takeIf(String::isNotBlank) }
                        .map { QrPayloadParser.parse(it) }
                    Result.success(decoded)
                } finally {
                    bitmap.recycle()
                }
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    override suspend fun decodeBarcodeFromRaw(rawValue: String): QrDecodedAction =
        QrPayloadParser.parse(rawValue)

    override suspend fun applyFilterToImage(
        sourcePath: String,
        filter: ScanFilter,
        rotationDegrees: Int,
    ): Result<String> = withContext(ioDispatcher) {
        try {
            val source = decodeBoundedBitmap(sourcePath, maxDimPx = 2200)
                ?: return@withContext Result.failure(IllegalArgumentException("Unable to read captured image"))
            var rotated: Bitmap? = null
            var filtered: Bitmap? = null
            try {
                rotated = if (rotationDegrees % 360 != 0) {
                    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                    Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
                } else {
                    source
                }

                filtered = when (filter) {
                    ScanFilter.ORIGINAL -> rotated.copy(Bitmap.Config.ARGB_8888, false)
                    ScanFilter.MAGIC_COLOR -> applyColorMatrix(
                        rotated,
                        ColorMatrix().apply {
                            val contrast = 1.35f
                            val translate = (-0.5f * contrast + 0.5f) * 255f
                            set(
                                floatArrayOf(
                                    contrast, 0f, 0f, 0f, translate,
                                    0f, contrast, 0f, 0f, translate,
                                    0f, 0f, contrast, 0f, translate,
                                    0f, 0f, 0f, 1f, 0f,
                                ),
                            )
                        },
                    )
                    ScanFilter.GRAYSCALE -> applyColorMatrix(
                        rotated,
                        ColorMatrix().apply { setSaturation(0f) },
                    )
                    ScanFilter.BLACK_WHITE -> applyThresholdBw(rotated)
                }

                val dir = File(context.cacheDir, "scanner_processed").apply { mkdirs() }
                val outFile = File(dir, "scan_${System.currentTimeMillis()}.jpg")
                FileOutputStream(outFile).use { fos ->
                    filtered.compress(Bitmap.CompressFormat.JPEG, 92, fos)
                }
                Result.success(outFile.absolutePath)
            } finally {
                if (rotated !== source) rotated?.recycle()
                source.recycle()
                filtered?.recycle()
            }
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun exportPagesToPdf(
        imagePaths: List<String>,
        outputBaseName: String,
    ): Result<PdfToolResult> = withContext(ioDispatcher) {
        try {
            if (imagePaths.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No scanned pages to export"))
            }
            val pdfDoc = PdfDocument()
            var beforeBytes = 0L
            try {
                imagePaths.forEachIndexed { idx, path ->
                    val file = resolveFile(path)
                    if (file != null) beforeBytes += file.length()
                    val bmp = decodeBoundedBitmap(path, maxDimPx = 2000) ?: return@forEachIndexed
                    try {
                        val pageW = 595
                        val pageH = 842
                        val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, idx + 1).create()
                        val page = pdfDoc.startPage(pageInfo)
                        val destRect = fitCenterRect(bmp.width, bmp.height, pageW - 48f, pageH - 48f, 24f, 24f)
                        page.canvas.drawColor(Color.WHITE)
                        page.canvas.drawBitmap(bmp, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                        pdfDoc.finishPage(page)
                    } finally {
                        bmp.recycle()
                    }
                }
                val outFile = uniqueExportFile("${sanitizeBase(outputBaseName)}.pdf")
                FileOutputStream(outFile).use { fos -> pdfDoc.writeTo(fos) }
                val uri = outFile.toURI().toString()
                documentRepository.registerSafUri(uri)
                Result.success(
                    PdfToolResult(
                        toolKind = PdfToolKind.IMAGE_TO_PDF,
                        outputUriString = uri,
                        outputDisplayName = outFile.name,
                        beforeSizeBytes = beforeBytes,
                        afterSizeBytes = outFile.length(),
                        summaryMessage = "Exported ${imagePaths.size} scanned page(s) to PDF",
                    ),
                )
            } finally {
                pdfDoc.close()
            }
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun composeDualIdCard(
        frontImagePath: String,
        backImagePath: String,
        exportAsPdf: Boolean,
        outputBaseName: String,
    ): Result<PdfToolResult> = withContext(ioDispatcher) {
        try {
            val front = decodeBoundedBitmap(frontImagePath, maxDimPx = 1600)
                ?: return@withContext Result.failure(IllegalArgumentException("Missing front ID card capture"))
            val back = decodeBoundedBitmap(backImagePath, maxDimPx = 1600)
                ?: run {
                    front.recycle()
                    return@withContext Result.failure(IllegalArgumentException("Missing back ID card capture"))
                }

            val a4W = 1240
            val a4H = 1754
            val canvasBitmap = Bitmap.createBitmap(a4W, a4H, Bitmap.Config.ARGB_8888)
            try {
                val canvas = Canvas(canvasBitmap)
                canvas.drawColor(Color.WHITE)
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#CBD5E1")
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)

                val slotW = a4W * 0.78f
                val slotH = a4H * 0.34f
                val frontRect = fitCenterRect(front.width, front.height, slotW, slotH, (a4W - slotW) / 2f, a4H * 0.10f)
                val backRect = fitCenterRect(back.width, back.height, slotW, slotH, (a4W - slotW) / 2f, a4H * 0.54f)

                canvas.drawBitmap(front, null, frontRect, bitmapPaint)
                canvas.drawRect(frontRect, borderPaint)
                canvas.drawBitmap(back, null, backRect, bitmapPaint)
                canvas.drawRect(backRect, borderPaint)

                val safeBase = sanitizeBase(outputBaseName)
                val outFile = if (exportAsPdf) {
                    val file = uniqueExportFile("$safeBase.pdf")
                    val pdfDoc = PdfDocument()
                    try {
                        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                        val page = pdfDoc.startPage(pageInfo)
                        page.canvas.drawBitmap(
                            canvasBitmap,
                            null,
                            RectF(0f, 0f, 595f, 842f),
                            bitmapPaint,
                        )
                        pdfDoc.finishPage(page)
                        FileOutputStream(file).use { pdfDoc.writeTo(it) }
                    } finally {
                        pdfDoc.close()
                    }
                    file
                } else {
                    val file = uniqueExportFile("$safeBase.png")
                    FileOutputStream(file).use { canvasBitmap.compress(Bitmap.CompressFormat.PNG, 95, it) }
                    file
                }

                val uri = outFile.toURI().toString()
                if (exportAsPdf) documentRepository.registerSafUri(uri)
                val beforeSize = (resolveFile(frontImagePath)?.length() ?: 0L) +
                    (resolveFile(backImagePath)?.length() ?: 0L)
                Result.success(
                    PdfToolResult(
                        toolKind = PdfToolKind.IMAGE_TO_PDF,
                        outputUriString = uri,
                        outputDisplayName = outFile.name,
                        beforeSizeBytes = beforeSize,
                        afterSizeBytes = outFile.length(),
                        summaryMessage = "Front & Back ID Card composed onto centered A4",
                    ),
                )
            } finally {
                front.recycle()
                back.recycle()
                canvasBitmap.recycle()
            }
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun exportOcrText(
        text: String,
        outputBaseName: String,
        asDocx: Boolean,
    ): Result<PdfToolResult> = withContext(ioDispatcher) {
        try {
            val safeBase = sanitizeBase(outputBaseName)
            val outFile = if (asDocx) {
                val file = uniqueExportFile("$safeBase.docx")
                val lines = text.lines().ifEmpty { listOf(text) }
                val bytes = OoxmlDocxWriter.buildFromPlainParagraphs(safeBase, listOf(lines))
                FileOutputStream(file).use { it.write(bytes) }
                file
            } else {
                val file = uniqueExportFile("$safeBase.txt")
                file.writeText(text, Charsets.UTF_8)
                file
            }
            val uri = outFile.toURI().toString()
            documentRepository.registerSafUri(uri)
            Result.success(
                PdfToolResult(
                    toolKind = PdfToolKind.PDF_TO_DOCX,
                    outputUriString = uri,
                    outputDisplayName = outFile.name,
                    beforeSizeBytes = text.toByteArray().size.toLong(),
                    afterSizeBytes = outFile.length(),
                    summaryMessage = "Saved OCR text as ${outFile.name}",
                ),
            )
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun applyColorMatrix(source: Bitmap, matrix: ColorMatrix): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return out
    }

    private fun applyThresholdBw(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        var lumSum = 0L
        val lum = IntArray(pixels.size)
        for (i in pixels.indices) {
            val p = pixels[i]
            val l = (Color.red(p) * 30 + Color.green(p) * 59 + Color.blue(p) * 11) / 100
            lum[i] = l
            lumSum += l
        }
        val threshold = ((lumSum / pixels.size.coerceAtLeast(1)).toInt() - 12).coerceIn(75, 195)
        for (i in pixels.indices) {
            pixels[i] = if (lum[i] >= threshold) Color.WHITE else Color.BLACK
        }
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, w, 0, 0, w, h)
        }
    }

    private fun fitCenterRect(
        srcW: Int,
        srcH: Int,
        boxW: Float,
        boxH: Float,
        boxLeft: Float,
        boxTop: Float,
    ): RectF {
        val scale = minOf(boxW / srcW.coerceAtLeast(1), boxH / srcH.coerceAtLeast(1))
        val drawW = srcW * scale
        val drawH = srcH * scale
        val left = boxLeft + (boxW - drawW) / 2f
        val top = boxTop + (boxH - drawH) / 2f
        return RectF(left, top, left + drawW, top + drawH)
    }

    private fun decodeBoundedBitmap(pathOrUri: String, maxDimPx: Int): Bitmap? {
        val file = resolveFile(pathOrUri)
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        if (file != null) {
            BitmapFactory.decodeFile(file.absolutePath, opts)
        } else {
            context.contentResolver.openInputStream(Uri.parse(pathOrUri))?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
        }
        var sample = 1
        while ((opts.outWidth / sample) > maxDimPx || (opts.outHeight / sample) > maxDimPx) {
            sample *= 2
        }
        val loadOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        return if (file != null) {
            BitmapFactory.decodeFile(file.absolutePath, loadOpts)
        } else {
            context.contentResolver.openInputStream(Uri.parse(pathOrUri))?.use {
                BitmapFactory.decodeStream(it, null, loadOpts)
            }
        }
    }

    private fun resolveFile(pathOrUri: String): File? {
        if (pathOrUri.startsWith("file://")) {
            return Uri.parse(pathOrUri).path?.let(::File)?.takeIf { it.exists() }
        }
        val direct = File(pathOrUri)
        return direct.takeIf { it.exists() }
    }

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

    private fun sanitizeBase(raw: String): String = raw.trim()
        .replace(Regex("[/\\\\:*?\"<>|\\x00-\\x1F]"), "_")
        .ifBlank { "Scan_${System.currentTimeMillis()}" }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { exc ->
        if (cont.isActive) cont.resumeWithException(exc)
    }
    addOnCanceledListener {
        cont.cancel()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScannerEngineModule {
    @Binds
    @Singleton
    abstract fun bindScannerPort(impl: MlKitScannerEngine): ScannerPort
}
