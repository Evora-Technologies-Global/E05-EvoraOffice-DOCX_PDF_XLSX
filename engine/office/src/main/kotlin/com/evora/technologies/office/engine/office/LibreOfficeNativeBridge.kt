package com.evora.technologies.office.engine.office

import android.graphics.Bitmap
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/**
 * Single-threaded native looper dispatcher + JNI boundary for LibreOfficeKit 16 KB (.so)
 * produced by R-01 CI (07_TECH_SPEC.md §7.1).
 *
 * All native LOK calls MUST run on [lokDispatcher] so C++ document handles are never raced
 * across Compose or IO threads.
 */
object LibreOfficeNativeBridge {
    val lokDispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "EvoraLOKLooper").apply { isDaemon = true }
    }.asCoroutineDispatcher()

    @Volatile
    private var libraryAttempted = false

    @Volatile
    var isLoaded: Boolean = false
        private set

    fun ensureLoaded(): Boolean {
        if (libraryAttempted) return isLoaded
        synchronized(this) {
            if (!libraryAttempted) {
                isLoaded = runCatching {
                    System.loadLibrary("lo-native-code")
                    true
                }.recoverCatching {
                    System.loadLibrary("sofficeapp")
                    true
                }.getOrDefault(false)
                libraryAttempted = true
            }
        }
        return isLoaded
    }

    external fun nativeInit(appDataDir: String, cacheDir: String): Boolean
    external fun nativeLoadDocument(filePath: String): Long
    external fun nativeGetPageCount(docHandle: Long): Int
    external fun nativeRenderTile(
        docHandle: Long,
        partIndex: Int,
        bitmap: Bitmap,
        tilePosX: Int,
        tilePosY: Int,
        tileWidthTwips: Int,
        tileHeightTwips: Int,
    ): Boolean

    external fun nativePostKeyEvent(docHandle: Long, type: Int, charCode: Int, keyCode: Int)
    external fun nativePostMouseEvent(docHandle: Long, type: Int, xTwips: Int, yTwips: Int, count: Int)
    external fun nativeUndo(docHandle: Long): Boolean
    external fun nativeRedo(docHandle: Long): Boolean
    external fun nativeSaveAs(docHandle: Long, targetPath: String, format: String): Boolean
    external fun nativeCloseDocument(docHandle: Long)
}
