package com.evora.technologies.office.core.designsystem.component

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Launches Android PrintManager directly for any local PDF file:// or content:// URI.
 */
fun launchSystemPdfPrint(
    context: Context,
    uriString: String,
    jobTitle: String,
    onError: (String) -> Unit,
) {
    runCatching {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: error("Print service unavailable")
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?,
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(jobTitle)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback,
            ) {
                runCatching {
                    val uri = Uri.parse(uriString)
                    val input = if (uri.scheme == "file" && uri.path != null) {
                        FileInputStream(File(uri.path!!))
                    } else {
                        context.contentResolver.openInputStream(uri)
                            ?: error("Unable to open document stream for printing")
                    }
                    input.use { src ->
                        FileOutputStream(destination.fileDescriptor).use { dst ->
                            src.copyTo(dst)
                        }
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                }.onFailure { err ->
                    callback.onWriteFailed(err.message ?: "Print failed")
                }
            }
        }
        printManager.print(jobTitle, adapter, PrintAttributes.Builder().build())
    }.onFailure { err ->
        onError(err.message ?: "Unable to open Android Print Manager")
    }
}
