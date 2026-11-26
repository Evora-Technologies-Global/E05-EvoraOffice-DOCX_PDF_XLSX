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
 * Office engine adapter wiring LibreOfficeKit 16 KB native libraries (R-01 CI artifact)
 * with pure-Kotlin OOXML/ODF text fallback when LOK runtime assets are unavailable.
 */
@Singleton
class LibreOfficeEngineAdapter @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : OfficeEnginePort {

    override fun isNativeEngineAvailable(): Boolean = LibreOfficeNativeBridge.ensureLoaded()

    override suspend fun extractReadablePreview(
        uriString: String,
        format: DocFormat,
    ): ParsedTextDocument = withContext(Dispatchers.IO) {
        val uri = Uri.parse(uriString)
        val title = uri.lastPathSegment?.substringAfterLast('/') ?: "Office Document"

        val lokText = extractViaLokIfActive(uriString, format)
        if (!lokText.isNullOrBlank()) {
            return@withContext ParsedTextDocument(
                title = title,
                chapters = listOf(TextChapter(title = title, body = lokText)),
                fullPlainText = lokText,
            )
        }

        val stream = openStream(uri, uriString)
            ?: return@withContext ParsedTextDocument(title, listOf(TextChapter("Preview", "Unable to open file.")), "")

        stream.use { input ->
            val extracted = runCatching { extractZipXmlText(input, format) }.getOrNull().orEmpty()
            val body = extracted.ifBlank {
                when {
                    !isNativeEngineAvailable() ->
                        "Binary legacy Office format preview requires LibreOfficeKit 16 KB native library."
                    !LibreOfficeNativeBridge.isRuntimeInitialized ->
                        "LibreOfficeKit native libraries loaded; runtime UNO assets (.rdb/.xcd) required for legacy binary format."
                    else ->
                        "Unable to extract text from document."
                }
            }
            ParsedTextDocument(
                title = title,
                chapters = listOf(TextChapter(title = title, body = body)),
                fullPlainText = body,
            )
        }
    }

    override suspend fun convertToPdfIfAvailable(
        sourceUriString: String,
        outPdfPath: String,
    ): Boolean = withContext(Dispatchers.IO) {
        if (!LibreOfficeNativeBridge.ensureInitialized(context)) {
            return@withContext false
        }
        val tmpSrc = LibreOfficeAssetHelper.copyUriToTempFile(context, sourceUriString, "docx")
            ?: return@withContext false
        try {
            LibreOfficeNativeBridge.convertDocument(
                context = context,
                sourcePath = tmpSrc.absolutePath,
                targetPath = outPdfPath,
                targetFormat = "pdf",
            )
        } finally {
            tmpSrc.delete()
        }
    }

    private suspend fun extractViaLokIfActive(uriString: String, format: DocFormat): String? {
        if (!LibreOfficeNativeBridge.ensureInitialized(context)) return null
        val fallbackExt = when (format) {
            DocFormat.WORD -> "docx"
            DocFormat.EXCEL -> "xlsx"
            DocFormat.PPT -> "pptx"
            DocFormat.ODF -> "odt"
            else -> "docx"
        }
        val tmpSrc = LibreOfficeAssetHelper.copyUriToTempFile(context, uriString, fallbackExt) ?: return null
        return try {
            LibreOfficeNativeBridge.extractPlainText(context, tmpSrc.absolutePath)
        } finally {
            tmpSrc.delete()
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
