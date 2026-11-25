package com.evora.technologies.office.core.domain.util

import com.evora.technologies.office.core.model.TextAlignOption
import com.evora.technologies.office.core.model.TrackChangeKind
import com.evora.technologies.office.core.model.WordDocumentSnapshot
import com.evora.technologies.office.core.model.WordParagraph
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Pure Kotlin OOXML (.docx) serializer producing standards-compliant Word packages
 * including rich formatting, Track Changes (<w:ins>/<w:del>), and Comments (word/comments.xml).
 */
object OoxmlDocxWriter {
    fun buildFromPlainParagraphs(title: String, pages: List<List<String>>): ByteArray {
        val paragraphs = mutableListOf<WordParagraph>()
        var idx = 0
        pages.forEachIndexed { pageIndex, lines ->
            if (pageIndex > 0) {
                paragraphs += WordParagraph(id = "p_${idx++}", text = "--- Page ${pageIndex + 1} ---", bold = true)
            }
            lines.forEach { line ->
                paragraphs += WordParagraph(id = "p_${idx++}", text = line)
            }
        }
        return buildFromWordSnapshot(WordDocumentSnapshot(title = title, paragraphs = paragraphs))
    }

    fun buildFromWordSnapshot(snapshot: WordDocumentSnapshot): ByteArray {
        val baos = ByteArrayOutputStream()
        val comments = snapshot.trackChanges.filter { it.kind == TrackChangeKind.COMMENT }
        val hasComments = comments.isNotEmpty()

        ZipOutputStream(baos).use { zos ->
            writeZipEntry(zos, "[Content_Types].xml", contentTypesXml(hasComments))
            writeZipEntry(zos, "_rels/.rels", rootRelsXml())
            writeZipEntry(zos, "word/_rels/document.xml.rels", documentRelsXml(hasComments))
            writeZipEntry(zos, "word/document.xml", buildDocumentXml(snapshot))
            if (hasComments) {
                writeZipEntry(zos, "word/comments.xml", buildCommentsXml(comments))
            }
        }
        return baos.toByteArray()
    }

    private fun writeZipEntry(zos: ZipOutputStream, name: String, content: String) {
        zos.putNextEntry(ZipEntry(name))
        zos.write(content.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }

    private fun contentTypesXml(hasComments: Boolean): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>""")
        if (hasComments) {
            append("""<Override PartName="/word/comments.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.comments+xml"/>""")
        }
        append("""</Types>""")
    }

    private fun rootRelsXml(): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
            """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>""" +
            """</Relationships>"""

    private fun documentRelsXml(hasComments: Boolean): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        if (hasComments) {
            append("""<Relationship Id="rIdComments" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/comments" Target="comments.xml"/>""")
        }
        append("""</Relationships>""")
    }

    private fun buildDocumentXml(snapshot: WordDocumentSnapshot): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""")
        append("""<w:body>""")
        if (snapshot.trackChangesEnabled) {
            append("""<w:trackRevisions/>""")
        }

        val commentsByParagraph = snapshot.trackChanges
            .filter { it.kind == TrackChangeKind.COMMENT }
            .groupBy { it.paragraphIndex }
        val revisionsByParagraph = snapshot.trackChanges
            .filter { it.kind != TrackChangeKind.COMMENT }
            .groupBy { it.paragraphIndex }

        snapshot.paragraphs.forEachIndexed { idx, para ->
            append("<w:p>")
            append("<w:pPr>")
            val jcVal = when (para.alignment) {
                TextAlignOption.LEFT -> "left"
                TextAlignOption.CENTER -> "center"
                TextAlignOption.RIGHT -> "right"
                TextAlignOption.JUSTIFY -> "both"
            }
            append("""<w:jc w:val="$jcVal"/>""")
            if (para.indentLevel > 0 || para.isBullet || para.isNumbered) {
                val leftTwips = (para.indentLevel.coerceAtLeast(if (para.isBullet || para.isNumbered) 1 else 0)) * 360
                append("""<w:ind w:left="$leftTwips"/>""")
            }
            append("</w:pPr>")

            val paraComments = commentsByParagraph[idx].orEmpty()
            paraComments.forEach { c ->
                append("""<w:commentRangeStart w:id="${escapeXml(c.id)}"/>""")
            }

            val prefix = when {
                para.isBullet -> "• "
                para.isNumbered -> "${idx + 1}. "
                else -> ""
            }
            append(buildRunXml(prefix + para.text, para))

            revisionsByParagraph[idx].orEmpty().forEachIndexed { revIdx, rev ->
                val dateIso = formatIso8601(rev.timestampEpochMs)
                val revId = "${idx}_${revIdx}"
                when (rev.kind) {
                    TrackChangeKind.INSERTION -> {
                        append("""<w:ins w:id="$revId" w:author="${escapeXml(rev.author)}" w:date="$dateIso">""")
                        append(buildRunXml(" " + rev.text, para))
                        append("</w:ins>")
                    }
                    TrackChangeKind.DELETION -> {
                        append("""<w:del w:id="$revId" w:author="${escapeXml(rev.author)}" w:date="$dateIso">""")
                        append("""<w:r><w:delText xml:space="preserve">${escapeXml(rev.text)}</w:delText></w:r>""")
                        append("</w:del>")
                    }
                    TrackChangeKind.COMMENT -> Unit
                }
            }

            paraComments.forEach { c ->
                append("""<w:commentRangeEnd w:id="${escapeXml(c.id)}"/>""")
                append("""<w:r><w:commentReference w:id="${escapeXml(c.id)}"/></w:r>""")
            }

            append("</w:p>")
        }

        val orientAttr = if (snapshot.isLandscape) """ w:orient="landscape" w:w="15840" w:h="12240"""" else """ w:w="12240" w:h="15840""""
        val marginTwips = (snapshot.pageMarginDp * 20).coerceIn(360, 2160)
        append("""<w:sectPr><w:pgSz$orientAttr/><w:pgMar w:top="$marginTwips" w:right="$marginTwips" w:bottom="$marginTwips" w:left="$marginTwips"/></w:sectPr>""")
        append("</w:body></w:document>")
    }

    private fun buildRunXml(text: String, para: WordParagraph): String = buildString {
        append("<w:r><w:rPr>")
        if (para.bold) append("<w:b/>")
        if (para.italic) append("<w:i/>")
        if (para.underline) append("""<w:u w:val="single"/>""")
        if (para.strikethrough) append("<w:strike/>")
        val halfPoints = (para.fontSizeSp.coerceIn(8, 72) * 2)
        append("""<w:sz w:val="$halfPoints"/>""")
        val rgbHex = String.format(Locale.US, "%06X", para.textColorArgb and 0xFFFFFFL)
        append("""<w:color w:val="$rgbHex"/>""")
        if (para.highlightColorArgb != null) {
            append("""<w:highlight w:val="yellow"/>""")
        }
        append("</w:rPr>")
        append("""<w:t xml:space="preserve">${escapeXml(text)}</w:t>""")
        append("</w:r>")
    }

    private fun buildCommentsXml(comments: List<com.evora.technologies.office.core.model.TrackChangeItem>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<w:comments xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""")
        comments.forEach { c ->
            append("""<w:comment w:id="${escapeXml(c.id)}" w:author="${escapeXml(c.author)}" w:date="${formatIso8601(c.timestampEpochMs)}">""")
            append("""<w:p><w:r><w:t xml:space="preserve">${escapeXml(c.text)}</w:t></w:r></w:p>""")
            append("</w:comment>")
        }
        append("</w:comments>")
    }

    private fun formatIso8601(epochMs: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(epochMs))
    }

    fun escapeXml(raw: String): String = buildString(raw.length) {
        for (ch in raw) {
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                '\u0000' -> Unit
                else -> append(ch)
            }
        }
    }
}
