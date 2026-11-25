package com.evora.technologies.office.core.domain.util

import com.evora.technologies.office.core.model.DocFormat
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

data class SniffedDocumentType(
    val format: DocFormat,
    val extension: String,
    val baseName: String,
)

/**
 * Pure JVM magic-byte & container MIME sniffer (US-004).
 * Inspects PDF, 7z, OLE2 (DOC/XLS/PPT/HWP), and ZIP-based containers (DOCX/XLSX/PPTX/HWPX/ODF/EPUB/ZIP)
 * so renamed or extensionless external URIs resolve cleanly.
 */
object MagicByteSniffer {

    private val PDF_MAGIC = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D) // %PDF-
    private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04) // PK\x03\x04
    private val ZIP_EMPTY_MAGIC = byteArrayOf(0x50, 0x4B, 0x05, 0x06)
    private val SEVEN_Z_MAGIC = byteArrayOf(0x37, 0x7A, 0xBC.toByte(), 0xAF.toByte(), 0x27, 0x1C)
    private val OLE2_MAGIC = byteArrayOf(
        0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(),
        0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte(),
    )

    fun sniff(
        bytes: ByteArray,
        displayNameHint: String? = null,
        mimeTypeHint: String? = null,
    ): SniffedDocumentType {
        val rawName = displayNameHint?.trim().orEmpty().ifBlank { "document" }
        val extFromName = rawName.substringAfterLast('.', "").lowercase()
        val baseName = if ('.' in rawName && extFromName.length in 1..6) {
            rawName.substringBeforeLast('.')
        } else {
            rawName
        }

        if (bytes.startsWith(PDF_MAGIC)) {
            return SniffedDocumentType(DocFormat.PDF, "pdf", baseName)
        }
        if (bytes.startsWith(SEVEN_Z_MAGIC)) {
            return SniffedDocumentType(DocFormat.ARCHIVE, "7z", baseName)
        }
        if (bytes.startsWith(ZIP_MAGIC) || bytes.startsWith(ZIP_EMPTY_MAGIC)) {
            val zipDetected = ByteArrayInputStream(bytes).use { inspectZipStream(it) }
            if (zipDetected != null) {
                return SniffedDocumentType(zipDetected.first, zipDetected.second, baseName)
            }
            val fallbackZipExt = DocFormat.fromExtension(extFromName)?.let {
                if (it in setOf(DocFormat.WORD, DocFormat.EXCEL, DocFormat.PPT, DocFormat.ODF, DocFormat.HWP, DocFormat.EPUB, DocFormat.ARCHIVE)) {
                    it to extFromName
                } else {
                    null
                }
            }
            if (fallbackZipExt != null) {
                return SniffedDocumentType(fallbackZipExt.first, fallbackZipExt.second, baseName)
            }
            return SniffedDocumentType(DocFormat.ARCHIVE, "zip", baseName)
        }
        if (bytes.startsWith(OLE2_MAGIC)) {
            val oleResolved = resolveOle2Hint(extFromName, mimeTypeHint)
            return SniffedDocumentType(oleResolved.first, oleResolved.second, baseName)
        }

        val mimeResolved = resolveMimeHint(mimeTypeHint)
        if (mimeResolved != null) {
            return SniffedDocumentType(mimeResolved.first, mimeResolved.second, baseName)
        }

        val nameFormat = DocFormat.fromExtension(extFromName)
        if (nameFormat != null) {
            return SniffedDocumentType(nameFormat, extFromName, baseName)
        }

        val textExt = if (extFromName in setOf("csv", "md", "log")) extFromName else "txt"
        return SniffedDocumentType(DocFormat.TEXT, textExt, baseName)
    }

    fun inspectZipFileStream(inputStream: InputStream): Pair<DocFormat, String>? =
        runCatching { inspectZipStream(inputStream) }.getOrNull()

    private fun inspectZipStream(inputStream: InputStream): Pair<DocFormat, String>? {
        var entryCount = 0
        val zis = ZipInputStream(inputStream)
        var entry = zis.nextEntry
        while (entry != null && entryCount < 128) {
            entryCount++
            val name = entry.name
            when {
                name.equals("mimetype", ignoreCase = true) -> {
                    val mime = zis.readBytesLimited(256).toString(Charsets.UTF_8).trim().lowercase()
                    when {
                        mime.contains("epub") -> return DocFormat.EPUB to "epub"
                        mime.contains("oasis.opendocument.text") -> return DocFormat.ODF to "odt"
                        mime.contains("oasis.opendocument.spreadsheet") -> return DocFormat.ODF to "ods"
                        mime.contains("oasis.opendocument.presentation") -> return DocFormat.ODF to "odp"
                        mime.contains("oasis.opendocument") -> return DocFormat.ODF to "odf"
                    }
                }
                name.startsWith("word/") -> return DocFormat.WORD to "docx"
                name.startsWith("xl/") -> return DocFormat.EXCEL to "xlsx"
                name.startsWith("ppt/") -> return DocFormat.PPT to "pptx"
                name.equals("Contents/header.xml", ignoreCase = true) ||
                    name.equals("Contents/section0.xml", ignoreCase = true) -> {
                    return DocFormat.HWP to "hwpx"
                }
            }
            entry = zis.nextEntry
        }
        return null
    }

    private fun resolveOle2Hint(extFromName: String, mimeTypeHint: String?): Pair<DocFormat, String> {
        val normalizedExt = extFromName.lowercase()
        when (normalizedExt) {
            "doc" -> return DocFormat.WORD to "doc"
            "xls" -> return DocFormat.EXCEL to "xls"
            "ppt", "pps", "pot" -> return DocFormat.PPT to normalizedExt
            "hwp", "hwt" -> return DocFormat.HWP to normalizedExt
        }
        val mime = mimeTypeHint?.lowercase().orEmpty()
        return when {
            "msword" in mime -> DocFormat.WORD to "doc"
            "ms-excel" in mime -> DocFormat.EXCEL to "xls"
            "ms-powerpoint" in mime -> DocFormat.PPT to "ppt"
            "hwp" in mime || "haansoft" in mime -> DocFormat.HWP to "hwp"
            else -> DocFormat.WORD to "doc"
        }
    }

    private fun resolveMimeHint(mimeTypeHint: String?): Pair<DocFormat, String>? {
        val mime = mimeTypeHint?.lowercase()?.trim().orEmpty()
        if (mime.isEmpty()) return null
        return when {
            mime == "application/pdf" -> DocFormat.PDF to "pdf"
            "wordprocessingml" in mime -> DocFormat.WORD to "docx"
            "msword" in mime -> DocFormat.WORD to "doc"
            "spreadsheetml" in mime -> DocFormat.EXCEL to "xlsx"
            "ms-excel" in mime -> DocFormat.EXCEL to "xls"
            "presentationml" in mime -> DocFormat.PPT to "pptx"
            "ms-powerpoint" in mime -> DocFormat.PPT to "ppt"
            "opendocument.text" in mime -> DocFormat.ODF to "odt"
            "opendocument.spreadsheet" in mime -> DocFormat.ODF to "ods"
            "opendocument.presentation" in mime -> DocFormat.ODF to "odp"
            "hwpx" in mime -> DocFormat.HWP to "hwpx"
            "hwp" in mime -> DocFormat.HWP to "hwp"
            "epub" in mime -> DocFormat.EPUB to "epub"
            "x-7z-compressed" in mime -> DocFormat.ARCHIVE to "7z"
            "zip" in mime -> DocFormat.ARCHIVE to "zip"
            mime == "text/csv" || mime == "text/comma-separated-values" -> DocFormat.TEXT to "csv"
            mime == "text/markdown" -> DocFormat.TEXT to "md"
            mime.startsWith("text/") -> DocFormat.TEXT to "txt"
            else -> null
        }
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        for (i in prefix.indices) {
            if (this[i] != prefix[i]) return false
        }
        return true
    }

    private fun InputStream.readBytesLimited(maxBytes: Int): ByteArray {
        val buf = ByteArray(maxBytes)
        var offset = 0
        while (offset < maxBytes) {
            val read = read(buf, offset, maxBytes - offset)
            if (read <= 0) break
            offset += read
        }
        return buf.copyOf(offset)
    }
}
