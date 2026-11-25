package com.evora.technologies.office.engine.hwp

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.evora.technologies.office.core.data.di.IoDispatcher
import com.evora.technologies.office.core.domain.port.ParsedTextDocument
import com.evora.technologies.office.core.domain.port.TextChapter
import com.evora.technologies.office.core.domain.port.TextEpubEnginePort
import com.evora.technologies.office.core.model.DocFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure-Kotlin reader for EPUB, TXT, CSV, LOG, and MD documents (T-009 / US-020).
 */
@Singleton
class TextEpubEngineImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TextEpubEnginePort {

    override suspend fun loadDocument(
        uriString: String,
        format: DocFormat,
    ): ParsedTextDocument = withContext(ioDispatcher) {
        val uri = Uri.parse(uriString)
        val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Document"
        val ext = fileName.substringAfterLast('.', "").lowercase()

        openStream(uri, uriString).use { input ->
            if (format == DocFormat.EPUB || ext == "epub") {
                parseEpub(fileName, input)
            } else {
                val rawText = input.readBytes().toString(Charsets.UTF_8).removePrefix("\uFEFF")
                val formatted = if (ext == "csv") formatCsvPreview(rawText) else rawText
                ParsedTextDocument(
                    title = fileName,
                    chapters = listOf(TextChapter(title = fileName, body = formatted)),
                    fullPlainText = formatted,
                )
            }
        }
    }

    private fun parseEpub(title: String, input: InputStream): ParsedTextDocument {
        val entries = LinkedHashMap<String, ByteArray>()
        ZipInputStream(input).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name] = zis.readBytes()
                }
                entry = zis.nextEntry
            }
        }

        val opfPath = entries["META-INF/container.xml"]?.let(::extractRootFilePath)
        val opfDir = opfPath?.substringBeforeLast('/', "")?.takeIf { it.isNotEmpty() }?.plus("/") ?: ""
        val spinePaths = opfPath?.let { entries[it] }?.let { extractOpfSpineHrefs(it, opfDir) }.orEmpty()

        val orderedHtmlPaths = if (spinePaths.isNotEmpty()) {
            spinePaths.filter { it in entries }
        } else {
            entries.keys.filter {
                it.endsWith(".xhtml", ignoreCase = true) ||
                    it.endsWith(".html", ignoreCase = true) ||
                    it.endsWith(".htm", ignoreCase = true)
            }.sorted()
        }

        val chapters = orderedHtmlPaths.mapIndexedNotNull { idx, path ->
            val bytes = entries[path] ?: return@mapIndexedNotNull null
            val text = stripHtmlToPlainText(bytes.toString(Charsets.UTF_8))
            if (text.isBlank()) null else TextChapter("Chapter ${idx + 1}", text)
        }

        val resolvedChapters = if (chapters.isEmpty()) {
            listOf(TextChapter(title, "No readable text chapters found in EPUB."))
        } else {
            chapters
        }
        return ParsedTextDocument(
            title = title,
            chapters = resolvedChapters,
            fullPlainText = resolvedChapters.joinToString("\n\n") { "${it.title}\n${it.body}" },
        )
    }

    private fun extractRootFilePath(containerXml: ByteArray): String? {
        return runCatching {
            val parser = Xml.newPullParser()
            parser.setInput(containerXml.inputStream(), "UTF-8")
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "rootfile") {
                    return@runCatching parser.getAttributeValue(null, "full-path")
                }
                event = parser.next()
            }
            null
        }.getOrNull()
    }

    private fun extractOpfSpineHrefs(opfXml: ByteArray, opfDir: String): List<String> {
        return runCatching {
            val manifest = HashMap<String, String>()
            val spineIds = mutableListOf<String>()
            val parser = Xml.newPullParser()
            parser.setInput(opfXml.inputStream(), "UTF-8")
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    when (parser.name.substringAfterLast(':')) {
                        "item" -> {
                            val id = parser.getAttributeValue(null, "id")
                            val href = parser.getAttributeValue(null, "href")
                            if (id != null && href != null) {
                                manifest[id] = opfDir + href.trimStart('/')
                            }
                        }
                        "itemref" -> {
                            val idref = parser.getAttributeValue(null, "idref")
                            if (idref != null) spineIds += idref
                        }
                    }
                }
                event = parser.next()
            }
            spineIds.mapNotNull { manifest[it] }
        }.getOrDefault(emptyList())
    }

    private fun stripHtmlToPlainText(html: String): String {
        val noHead = html
            .replace(Regex("<head[^>]*>[\\s\\S]*?</head>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("</(p|div|h1|h2|h3|h4|h5|h6|li|tr|br)>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), " ")
        return decodeBasicEntities(noHead)
            .lines()
            .map { it.trim().replace(Regex("\\s{2,}"), " ") }
            .filter { it.isNotEmpty() }
            .joinToString("\n\n")
    }

    private fun decodeBasicEntities(text: String): String = text
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")

    private fun formatCsvPreview(raw: String): String = raw
        .lineSequence()
        .take(500)
        .mapIndexed { rowIdx, line ->
            val cols = parseCsvLine(line)
            if (rowIdx == 0) cols.joinToString("  |  ") else cols.joinToString("  |  ")
        }
        .joinToString("\n")

    private fun parseCsvLine(line: String): List<String> {
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    cells += current.toString().trim()
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        cells += current.toString().trim()
        return cells
    }

    private fun openStream(uri: Uri, raw: String): InputStream =
        if (uri.scheme == "file" && uri.path != null) {
            FileInputStream(File(uri.path!!))
        } else {
            context.contentResolver.openInputStream(uri) ?: FileInputStream(File(raw))
        }
}
