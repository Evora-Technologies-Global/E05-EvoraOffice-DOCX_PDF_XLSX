package com.evora.technologies.office.engine.pdf

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.evora.technologies.office.core.data.di.IoDispatcher
import com.evora.technologies.office.core.domain.port.DocumentInvalidPasswordException
import com.evora.technologies.office.core.domain.port.DocumentPasswordRequiredException
import com.evora.technologies.office.core.domain.port.PdfDocumentInfo
import com.evora.technologies.office.core.domain.port.PdfEnginePort
import com.evora.technologies.office.core.domain.port.PdfPageRaster
import com.evora.technologies.office.core.model.OutlineNode
import com.evora.technologies.office.core.model.PdfSearchMatch
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.legere.pdfiumandroid.PdfDocument
import io.legere.pdfiumandroid.PdfiumCore
import io.legere.pdfiumandroid.api.Bookmark
import io.legere.pdfiumandroid.api.PdfPasswordException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfiumPdfEngine @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PdfEnginePort {

    private val pdfiumCore by lazy { PdfiumCore(context) }
    private val renderSemaphore = Semaphore(permits = 2)

    override suspend fun getPageCount(uriString: String, password: String?): Int =
        withPdfDocument(uriString, password) { document ->
            document.getPageCount()
        }

    override suspend fun openDocumentInfo(uriString: String, password: String?): PdfDocumentInfo =
        withPdfDocument(uriString, password) { document ->
            val pageCount = document.getPageCount()
            val outline = document.getTableOfContents().map { it.toOutlineNode(level = 0) }
            PdfDocumentInfo(pageCount = pageCount, outline = outline)
        }

    override suspend fun renderPage(
        uriString: String,
        pageIndex: Int,
        targetWidthPx: Int,
        password: String?,
    ): PdfPageRaster = withPdfDocument(uriString, password) { document ->
        val safeWidth = targetWidthPx.coerceIn(240, 2048)
        val page = document.openPage(pageIndex) ?: error("Unable to open PDF page $pageIndex")
        page.use { openedPage ->
            val ptWidth = openedPage.getPageWidthPoint().coerceAtLeast(1)
            val ptHeight = openedPage.getPageHeightPoint().coerceAtLeast(1)
            val aspect = ptHeight.toFloat() / ptWidth.toFloat()
            val safeHeight = (safeWidth * aspect).toInt().coerceIn(240, 4096)
            val bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
            try {
                openedPage.renderPageBitmap(
                    bitmap = bitmap,
                    startX = 0,
                    startY = 0,
                    drawSizeX = safeWidth,
                    drawSizeY = safeHeight,
                    renderAnnot = true,
                    textMask = false,
                )
                val pixels = IntArray(safeWidth * safeHeight)
                bitmap.getPixels(pixels, 0, safeWidth, 0, 0, safeWidth, safeHeight)
                PdfPageRaster(width = safeWidth, height = safeHeight, argbPixels = pixels)
            } finally {
                bitmap.recycle()
            }
        }
    }

    override suspend fun extractPageText(
        uriString: String,
        pageIndex: Int,
        password: String?,
    ): String = withPdfDocument(uriString, password) { document ->
        val page = document.openPage(pageIndex) ?: return@withPdfDocument ""
        page.use { openedPage ->
            openedPage.openTextPage()?.use { textPage ->
                val count = textPage.textPageCountChars()
                if (count <= 0) "" else textPage.textPageGetText(0, count).orEmpty()
            }.orEmpty()
        }
    }

    override suspend fun searchText(
        uriString: String,
        query: String,
        password: String?,
    ): List<PdfSearchMatch> {
        val needle = query.trim()
        if (needle.isEmpty()) return emptyList()

        return withPdfDocument(uriString, password) { document ->
            val total = document.getPageCount()
            val matches = mutableListOf<PdfSearchMatch>()
            for (pageIndex in 0 until total) {
                if (matches.size >= 100) break
                val text = document.openPage(pageIndex)?.use { openedPage ->
                    openedPage.openTextPage()?.use { textPage ->
                        val count = textPage.textPageCountChars()
                        if (count <= 0) "" else textPage.textPageGetText(0, count).orEmpty()
                    }
                }.orEmpty()
                val hitIdx = text.indexOf(needle, ignoreCase = true)
                if (hitIdx >= 0) {
                    val start = (hitIdx - 28).coerceAtLeast(0)
                    val end = (hitIdx + needle.length + 40).coerceAtMost(text.length)
                    val snippet = text.substring(start, end).replace('\n', ' ').trim()
                    matches += PdfSearchMatch(
                        pageIndex = pageIndex,
                        matchText = needle,
                        snippet = snippet,
                    )
                }
            }
            matches
        }
    }

    private suspend fun <T> withPdfDocument(
        uriString: String,
        password: String?,
        block: (PdfDocument) -> T,
    ): T = withContext(ioDispatcher) {
        renderSemaphore.withPermit {
            val descriptor = openDescriptor(uriString)
            descriptor.use { pfd ->
                val doc = try {
                    if (password != null) {
                        pdfiumCore.newDocument(pfd, password)
                    } else {
                        pdfiumCore.newDocument(pfd)
                    }
                } catch (_: PdfPasswordException) {
                    if (password == null) {
                        throw DocumentPasswordRequiredException("Password required to open PDF")
                    } else {
                        throw DocumentInvalidPasswordException("Wrong password — please try again")
                    }
                }
                doc.use(block)
            }
        }
    }

    private fun openDescriptor(uriString: String): ParcelFileDescriptor {
        val uri = Uri.parse(uriString)
        if (uri.scheme == "file" && uri.path != null) {
            return ParcelFileDescriptor.open(File(uri.path!!), ParcelFileDescriptor.MODE_READ_ONLY)
        }
        return context.contentResolver.openFileDescriptor(uri, "r")
            ?: error("Unable to open PDF descriptor: $uriString")
    }

    private fun Bookmark.toOutlineNode(level: Int): OutlineNode = OutlineNode(
        title = title.orEmpty().ifBlank { "Page ${pageIdx + 1}" },
        pageIndex = pageIdx.toInt().coerceAtLeast(0),
        level = level,
        children = children.map { it.toOutlineNode(level + 1) },
    )
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PdfEngineModule {
    @Binds
    @Singleton
    abstract fun bindPdfEnginePort(impl: PdfiumPdfEngine): PdfEnginePort
}
