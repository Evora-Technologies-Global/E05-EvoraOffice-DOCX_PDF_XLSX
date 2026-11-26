package com.evora.technologies.office.engine.office

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.libreoffice.kit.DirectBufferAllocator
import org.libreoffice.kit.Document
import org.libreoffice.kit.LibreOfficeKit
import org.libreoffice.kit.Office
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * Single-threaded native looper dispatcher + JNI boundary for LibreOfficeKit 16 KB (.so)
 * produced by R-01 CI (07_TECH_SPEC.md §7.1).
 *
 * All native [Office] and [Document] calls MUST run on [lokDispatcher] so C++ document handles
 * are never raced across Compose or IO threads.
 */
object LibreOfficeNativeBridge {
    private const val TAG = "LibreOfficeNativeBridge"

    val lokDispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "EvoraLOKLooper").apply { isDaemon = true }
    }.asCoroutineDispatcher()

    private val nextHandle = AtomicLong(1L)
    private val openDocuments = HashMap<Long, Document>()

    @Volatile
    private var officeHandle: Office? = null

    @Volatile
    private var initializationAttempted = false

    @Volatile
    var isLoaded: Boolean = false
        private set

    @Volatile
    var isRuntimeInitialized: Boolean = false
        private set

    fun ensureLoaded(): Boolean {
        val loaded = runCatching { LibreOfficeKit.loadLibraries() }.getOrDefault(false)
        isLoaded = loaded
        return loaded
    }

    suspend fun ensureInitialized(context: Context): Boolean = withContext(lokDispatcher) {
        ensureInitializedOnLooper(context)
    }

    private fun ensureInitializedOnLooper(context: Context): Boolean {
        if (isRuntimeInitialized && officeHandle != null) {
            return true
        }
        if (initializationAttempted && !isRuntimeInitialized) {
            return false
        }
        initializationAttempted = true

        if (!ensureLoaded()) {
            return false
        }
        if (!LibreOfficeAssetHelper.prepareRuntime(context)) {
            Log.i(TAG, "LOK runtime assets (.rdb/.xcd) not bundled yet; using Kotlin OOXML fallback.")
            return false
        }
        if (!LibreOfficeKit.init(context)) {
            return false
        }

        val rawHandle = runCatching { LibreOfficeKit.getLibreOfficeKitHandle() }.getOrNull()
        if (rawHandle == null) {
            Log.e(TAG, "getLibreOfficeKitHandle() returned null")
            return false
        }

        val office = Office(rawHandle).apply {
            setOptionalFeatures(Document.LOK_FEATURE_DOCUMENT_PASSWORD)
        }
        officeHandle = office
        isRuntimeInitialized = true
        return true
    }

    suspend fun openDocument(
        context: Context,
        filePath: String,
        password: String? = null,
    ): Long = withContext(lokDispatcher) {
        openDocumentOnLooper(context, filePath, password)
    }

    private fun openDocumentOnLooper(
        context: Context,
        filePath: String,
        password: String? = null,
    ): Long {
        if (!ensureInitializedOnLooper(context)) return 0L
        val office = officeHandle ?: return 0L
        val fileUrl = File(filePath).toURI().toASCIIString()
        if (!password.isNullOrEmpty()) {
            office.setDocumentPassword(fileUrl, password)
        }
        val doc = office.documentLoad(fileUrl) ?: run {
            Log.w(TAG, "documentLoad failed for $filePath: ${office.error}")
            return 0L
        }
        doc.initializeForRendering()
        val id = nextHandle.getAndIncrement()
        openDocuments[id] = doc
        return id
    }

    suspend fun getPageCount(docHandle: Long): Int = withContext(lokDispatcher) {
        openDocuments[docHandle]?.parts ?: 0
    }

    suspend fun getDocumentType(docHandle: Long): Int = withContext(lokDispatcher) {
        openDocuments[docHandle]?.documentType ?: Document.DOCTYPE_OTHER
    }

    suspend fun getDocumentSizeTwips(docHandle: Long): Pair<Long, Long> = withContext(lokDispatcher) {
        val doc = openDocuments[docHandle] ?: return@withContext (0L to 0L)
        doc.documentWidth to doc.documentHeight
    }

    suspend fun renderTile(
        docHandle: Long,
        partIndex: Int,
        bitmap: Bitmap,
        tilePosX: Int,
        tilePosY: Int,
        tileWidthTwips: Int,
        tileHeightTwips: Int,
    ): Boolean = withContext(lokDispatcher) {
        val doc = openDocuments[docHandle] ?: return@withContext false
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return@withContext false

        val buffer = DirectBufferAllocator.guardedAllocate(w * h * 4) ?: return@withContext false
        return@withContext try {
            if (doc.part != partIndex) {
                doc.setPart(partIndex)
            }
            doc.paintTile(buffer, w, h, tilePosX, tilePosY, tileWidthTwips, tileHeightTwips)
            buffer.rewind()
            bitmap.copyPixelsFromBuffer(buffer)
            true
        } catch (t: Throwable) {
            Log.e(TAG, "renderTile failed: ${t.message}", t)
            false
        } finally {
            DirectBufferAllocator.free(buffer)
        }
    }

    suspend fun postKeyEvent(docHandle: Long, type: Int, charCode: Int, keyCode: Int) =
        withContext(lokDispatcher) {
            openDocuments[docHandle]?.postKeyEvent(type, charCode, keyCode)
        }

    suspend fun postMouseEvent(
        docHandle: Long,
        type: Int,
        xTwips: Int,
        yTwips: Int,
        count: Int,
        button: Int = Document.MOUSE_BUTTON_LEFT,
        modifier: Int = Document.KEYBOARD_MODIFIER_NONE,
    ) = withContext(lokDispatcher) {
        openDocuments[docHandle]?.postMouseEvent(type, xTwips, yTwips, count, button, modifier)
    }

    suspend fun postUnoCommand(
        docHandle: Long,
        command: String,
        argumentsJson: String? = null,
        notifyWhenFinished: Boolean = false,
    ) = withContext(lokDispatcher) {
        openDocuments[docHandle]?.postUnoCommand(command, argumentsJson, notifyWhenFinished)
    }

    suspend fun undo(docHandle: Long): Boolean = withContext(lokDispatcher) {
        val doc = openDocuments[docHandle] ?: return@withContext false
        doc.postUnoCommand(".uno:Undo", null, false)
        true
    }

    suspend fun redo(docHandle: Long): Boolean = withContext(lokDispatcher) {
        val doc = openDocuments[docHandle] ?: return@withContext false
        doc.postUnoCommand(".uno:Redo", null, false)
        true
    }

    suspend fun saveAs(
        docHandle: Long,
        targetPath: String,
        format: String,
        options: String = "",
    ): Boolean = withContext(lokDispatcher) {
        saveAsOnLooper(docHandle, targetPath, format, options)
    }

    private fun saveAsOnLooper(
        docHandle: Long,
        targetPath: String,
        format: String,
        options: String = "",
    ): Boolean {
        val doc = openDocuments[docHandle] ?: return false
        val office = officeHandle ?: return false
        val targetUrl = File(targetPath).toURI().toASCIIString()
        val rc = doc.saveAs(targetUrl, format, options)
        val err = office.error.orEmpty()
        if (err.isNotBlank()) {
            Log.w(TAG, "saveAs($format) error: $err (rc=$rc)")
        }
        val outFile = File(targetPath)
        return rc != 0 || (err.isBlank() && outFile.exists() && outFile.length() > 0L)
    }

    suspend fun closeDocument(docHandle: Long) = withContext(lokDispatcher) {
        closeDocumentOnLooper(docHandle)
    }

    private fun closeDocumentOnLooper(docHandle: Long) {
        openDocuments.remove(docHandle)?.let { doc ->
            runCatching { doc.destroy() }
        }
    }

    suspend fun convertDocument(
        context: Context,
        sourcePath: String,
        targetPath: String,
        targetFormat: String,
        password: String? = null,
    ): Boolean = withContext(lokDispatcher) {
        val handle = openDocumentOnLooper(context, sourcePath, password)
        if (handle == 0L) return@withContext false
        try {
            val saved = saveAsOnLooper(handle, targetPath, targetFormat, "")
            val outFile = File(targetPath)
            saved && outFile.exists() && outFile.length() > 0L
        } finally {
            closeDocumentOnLooper(handle)
        }
    }

    suspend fun extractPlainText(
        context: Context,
        sourcePath: String,
        password: String? = null,
    ): String? {
        val tmpTxt = File.createTempFile("lok_txt_", ".txt", context.cacheDir)
        return try {
            val ok = convertDocument(context, sourcePath, tmpTxt.absolutePath, "txt", password)
            if (ok && tmpTxt.exists()) tmpTxt.readText(Charsets.UTF_8).trim() else null
        } finally {
            tmpTxt.delete()
        }
    }
}
