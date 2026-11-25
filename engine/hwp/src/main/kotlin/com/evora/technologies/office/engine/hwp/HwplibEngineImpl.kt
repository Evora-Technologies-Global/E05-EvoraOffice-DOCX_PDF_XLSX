package com.evora.technologies.office.engine.hwp

import android.content.Context
import android.net.Uri
import com.evora.technologies.office.core.data.di.IoDispatcher
import com.evora.technologies.office.core.domain.port.DocumentInvalidPasswordException
import com.evora.technologies.office.core.domain.port.DocumentPasswordRequiredException
import com.evora.technologies.office.core.domain.port.HwpEnginePort
import com.evora.technologies.office.core.domain.port.HwpRenderResult
import com.evora.technologies.office.core.domain.port.TextEpubEnginePort
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kr.dogfoot.hwplib.`object`.HWPFile
import kr.dogfoot.hwplib.`object`.bodytext.control.ControlTable
import kr.dogfoot.hwplib.`object`.bodytext.paragraph.Paragraph
import kr.dogfoot.hwplib.reader.HWPReader
import kr.dogfoot.hwplib.tool.textextractor.TextExtractMethod
import kr.dogfoot.hwplib.tool.textextractor.TextExtractor
import kr.dogfoot.hwpxlib.reader.HWPXReader
import kr.dogfoot.hwpxlib.tool.textextractor.TextExtractMethod as HwpxTextExtractMethod
import kr.dogfoot.hwpxlib.tool.textextractor.TextMarks as HwpxTextMarks
import kr.dogfoot.hwpxlib.tool.textextractor.TextExtractor as HwpxTextExtractor
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 100% offline Korean Hangul (HWP / HWPX) parser -> XSS-sanitized HTML generator (T-009 / US-023).
 */
@Singleton
class HwplibEngineImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : HwpEnginePort {

    override suspend fun renderToSanitizedHtml(
        uriString: String,
        password: String?,
    ): HwpRenderResult = withContext(ioDispatcher) {
        val (localFile, ownedTemp, title) = materializeFile(uriString)
        try {
            val fileSize = localFile.length()
            val isLarge = fileSize > FIFTY_MEGABYTES_BYTES
            val ext = title.substringAfterLast('.', "").lowercase()

            val (bodyHtml, plainText) = if (ext == "hwpx" || isZipSignature(localFile)) {
                parseHwpx(localFile, password)
            } else {
                parseHwpBinary(localFile, password)
            }

            HwpRenderResult(
                title = title,
                sanitizedHtml = HwpHtmlSanitizer.wrapDocumentHtml(title = title, bodyHtml = bodyHtml),
                plainText = plainText,
                fileSizeBytes = fileSize,
                isLargeFileWarning = isLarge,
            )
        } catch (c: CancellationException) {
            throw c
        } catch (p: DocumentPasswordRequiredException) {
            throw p
        } catch (p: DocumentInvalidPasswordException) {
            throw p
        } catch (t: Throwable) {
            val msg = t.message.orEmpty()
            if (msg.contains("password", ignoreCase = true) || msg.contains("encrypt", ignoreCase = true)) {
                if (password == null) throw DocumentPasswordRequiredException()
                throw DocumentInvalidPasswordException()
            }
            throw t
        } finally {
            if (ownedTemp) localFile.delete()
        }
    }

    private fun parseHwpBinary(file: File, password: String?): Pair<String, String> {
        val hwpFile: HWPFile = HWPReader.fromFile(file)
        if (hwpFile.fileHeader?.hasPassword() == true) {
            if (password.isNullOrEmpty()) {
                throw DocumentPasswordRequiredException("Password-protected HWP file")
            }
            // Binary HWP v5 decryption is not supported by hwplib 1.1.11 directly
            throw DocumentInvalidPasswordException("Encrypted HWP v5 binary cannot be decrypted offline")
        }

        val htmlSb = StringBuilder()
        val sections = hwpFile.bodyText?.sectionList.orEmpty()
        sections.forEach { section ->
            section.paragraphs?.forEach { paragraph ->
                appendHwpParagraphHtml(paragraph, htmlSb)
            }
        }
        val plain = runCatching {
            TextExtractor.extract(hwpFile, TextExtractMethod.InsertControlTextBetweenParagraphText).orEmpty()
        }.getOrDefault("")
        val resolvedBody = htmlSb.toString().ifBlank {
            plain.lines().filter { it.isNotBlank() }
                .joinToString("\n") { "<p>${HwpHtmlSanitizer.escapeHtml(it)}</p>" }
        }
        return resolvedBody to plain
    }

    private fun appendHwpParagraphHtml(paragraph: Paragraph, out: StringBuilder) {
        val text = runCatching { paragraph.normalString.orEmpty() }.getOrDefault("").trim()
        if (text.isNotEmpty()) {
            out.append("<p>").append(HwpHtmlSanitizer.escapeHtml(text)).append("</p>\n")
        }
        paragraph.controlList?.forEach { control ->
            if (control is ControlTable) {
                out.append("<table>\n")
                control.rowList?.forEach { row ->
                    out.append("<tr>")
                    row.cellList?.forEach { cell ->
                        val colSpan = cell.listHeader?.colSpan?.coerceAtLeast(1) ?: 1
                        val rowSpan = cell.listHeader?.rowSpan?.coerceAtLeast(1) ?: 1
                        val cellText = buildString {
                            cell.paragraphList?.paragraphs?.forEach { p ->
                                val line = runCatching { p.normalString.orEmpty() }.getOrDefault("").trim()
                                if (line.isNotEmpty()) {
                                    if (isNotEmpty()) append("<br/>")
                                    append(HwpHtmlSanitizer.escapeHtml(line))
                                }
                            }
                        }
                        out.append("<td")
                        if (colSpan > 1) out.append(" colspan=\"").append(colSpan).append('"')
                        if (rowSpan > 1) out.append(" rowspan=\"").append(rowSpan).append('"')
                        out.append('>').append(cellText).append("</td>")
                    }
                    out.append("</tr>\n")
                }
                out.append("</table>\n")
            }
        }
    }

    private fun parseHwpx(file: File, password: String?): Pair<String, String> {
        val hwpxFile = HWPXReader.fromFile(file)
        val marks = HwpxTextMarks()
            .paraSeparatorAnd("\n")
            .lineBreakAnd("\n")
            .tableStartAnd("\n[[EVORA_TBL_START]]\n")
            .tableEndAnd("\n[[EVORA_TBL_END]]\n")
            .tableRowSeparatorAnd("\n[[EVORA_ROW]]")
            .tableCellSeparatorAnd("[[EVORA_CELL]]")
        val rawMarked = HwpxTextExtractor.extract(
            hwpxFile,
            HwpxTextExtractMethod.InsertControlTextBetweenParagraphText,
            false,
            marks,
        ).orEmpty()

        val plain = rawMarked
            .replace("[[EVORA_TBL_START]]", "")
            .replace("[[EVORA_TBL_END]]", "")
            .replace("[[EVORA_ROW]]", "\n")
            .replace("[[EVORA_CELL]]", " | ")
            .trim()

        if (plain.isEmpty() && password == null && hwpxFile.sectionXMLFileList().count() == 0) {
            throw DocumentPasswordRequiredException("Encrypted or empty HWPX file")
        }

        val htmlSb = StringBuilder()
        var inTable = false
        rawMarked.lines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed == "[[EVORA_TBL_START]]" -> {
                    inTable = true
                    htmlSb.append("<table>\n")
                }
                trimmed == "[[EVORA_TBL_END]]" -> {
                    if (inTable) {
                        inTable = false
                        htmlSb.append("</table>\n")
                    }
                }
                inTable -> {
                    val rowContent = trimmed.removePrefix("[[EVORA_ROW]]")
                    val cells = rowContent.split("[[EVORA_CELL]]")
                    htmlSb.append("<tr>")
                    cells.forEach { cell ->
                        htmlSb.append("<td>").append(HwpHtmlSanitizer.escapeHtml(cell.trim())).append("</td>")
                    }
                    htmlSb.append("</tr>\n")
                }
                trimmed.isNotEmpty() -> {
                    htmlSb.append("<p>").append(HwpHtmlSanitizer.escapeHtml(trimmed)).append("</p>\n")
                }
            }
        }
        if (inTable) htmlSb.append("</table>\n")
        return htmlSb.toString() to plain
    }

    private fun isZipSignature(file: File): Boolean = runCatching {
        FileInputStream(file).use { fis ->
            val magic = ByteArray(2)
            fis.read(magic) == 2 && magic[0] == 0x50.toByte() && magic[1] == 0x4B.toByte()
        }
    }.getOrDefault(false)

    private fun materializeFile(uriString: String): Triple<File, Boolean, String> {
        val uri = Uri.parse(uriString)
        if (uri.scheme == "file" && uri.path != null) {
            val file = File(uri.path!!)
            return Triple(file, false, file.name)
        }
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "document.hwp"
        val temp = File(context.cacheDir, "hwp_tmp_${System.nanoTime()}_$name")
        val input = context.contentResolver.openInputStream(uri) ?: FileInputStream(File(uriString))
        input.use { src -> FileOutputStream(temp).use { dst -> src.copyTo(dst) } }
        return Triple(temp, true, name)
    }

    companion object {
        const val FIFTY_MEGABYTES_BYTES = 50L * 1024L * 1024L
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class HwpEngineModule {
    @Binds
    @Singleton
    abstract fun bindHwpEnginePort(impl: HwplibEngineImpl): HwpEnginePort

    @Binds
    @Singleton
    abstract fun bindTextEpubEnginePort(impl: TextEpubEngineImpl): TextEpubEnginePort
}
