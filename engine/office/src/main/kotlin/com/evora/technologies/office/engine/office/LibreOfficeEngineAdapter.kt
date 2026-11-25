package com.evora.technologies.office.engine.office

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.evora.technologies.office.core.domain.port.OfficeEnginePort
import com.evora.technologies.office.core.domain.port.ParsedTextDocument
import com.evora.technologies.office.core.domain.port.TextChapter
import com.evora.technologies.office.core.model.DocFormat
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Office engine boundary awaiting LibreOfficeKit 16 KB native artifact from R-01 CI (T-002 / T-015).
 * Provides an offline pure-Kotlin OOXML/ODF text preview extractor until LOK .so lands in Sprint 2.
 */
@Singleton
class LibreOfficeEngineAdapter @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : OfficeEnginePort {
    override fun isNativeEngineAvailable(): Boolean = runCatching {
        System.loadLibrary("lo-native-code")
        true
    }.getOrDefault(false)

    override suspend fun extractReadablePreview(
        uriString: String,
        format: DocFormat,
    ): ParsedTextDocument = withContext(Dispatchers.IO) {
        val uri = Uri.parse(uriString)
        val title = uri.lastPathSegment?.substringAfterLast('/') ?: "Office Document"
        val stream = openStream(uri, uriString)
            ?: return@withContext ParsedTextDocument(title, listOf(TextChapter("Preview", "Unable to open file.")), "")

        stream.use { input ->
            val extracted = runCatching { extractZipXmlText(input, format) }.getOrNull().orEmpty()
            val body = extracted.ifBlank {
                if (!isNativeEngineAvailable()) {
                    "Binary legacy Office format preview requires LibreOfficeKit 16 KB native library (R-01 CI artifact)."
                } else {
                    "Ready in LibreOfficeKit."
                }
            }
            ParsedTextDocument(
                title = title,
                chapters = listOf(TextChapter(title = title, body = body)),
                fullPlainText = body,
            )
        }
    }

    private fun extractZipXmlText(input: InputStream, format: DocFormat): String {
        val sections = mutableListOf<String>()
        val zis = ZipInputStream(input)
        var entry = zis.nextEntry
        while (entry != null) {
            val name = entry.name
            val matches = when (format) {
                DocFormat.WORD -> name == "word/document.xml"
                DocFormat.EXCEL -> name.startsWith("xl/sharedStrings") || name.startsWith("xl/worksheets/sheet")
                DocFormat.PPT -> name.startsWith("ppt/slides/slide") && name.endsWith(".xml")
                DocFormat.ODF -> name == "content.xml"
                else -> false
            }
            if (matches) {
                val text = extractTextFromXmlBytes(zis.readBytes())
                if (text.isNotBlank()) sections += text
            }
            entry = zis.nextEntry
        }
        return sections.joinToString("\n\n")
    }

    private fun extractTextFromXmlBytes(bytes: ByteArray): String {
        val parser = Xml.newPullParser()
        parser.setInput(bytes.inputStream(), "UTF-8")
        val sb = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.TEXT -> {
                    val t = parser.text?.trim()
                    if (!t.isNullOrEmpty()) {
                        if (sb.isNotEmpty() && sb.last() != '\n') sb.append(' ')
                        sb.append(t)
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tag = parser.name
                    if (tag == "p" || tag == "row" || tag == "tr") {
                        if (sb.isNotEmpty() && sb.last() != '\n') sb.append('\n')
                    }
                }
            }
            event = parser.next()
        }
        return sb.toString().trim()
    }

    private fun openStream(uri: Uri, raw: String): InputStream? =
        if (uri.scheme == "file" && uri.path != null) {
            FileInputStream(File(uri.path!!))
        } else {
            context.contentResolver.openInputStream(uri) ?: runCatching { FileInputStream(File(raw)) }.getOrNull()
        }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class OfficeEngineModule {
    @Binds
    @Singleton
    abstract fun bindOfficeEnginePort(impl: LibreOfficeEngineAdapter): OfficeEnginePort
}
