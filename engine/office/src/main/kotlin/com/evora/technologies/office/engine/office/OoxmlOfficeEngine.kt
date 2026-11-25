package com.evora.technologies.office.engine.office

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Xml
import com.evora.technologies.office.core.data.di.IoDispatcher
import com.evora.technologies.office.core.domain.port.DocumentRepository
import com.evora.technologies.office.core.domain.port.OfficeEditorPort
import com.evora.technologies.office.core.domain.util.ExcelFormulaEvaluator
import com.evora.technologies.office.core.domain.util.OoxmlDocxWriter
import com.evora.technologies.office.core.model.CellHorizontalAlign
import com.evora.technologies.office.core.model.CellNumberFormat
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.ExcelCell
import com.evora.technologies.office.core.model.SlideDeckSnapshot
import com.evora.technologies.office.core.model.SlideElement
import com.evora.technologies.office.core.model.SlidePageData
import com.evora.technologies.office.core.model.TextAlignOption
import com.evora.technologies.office.core.model.TrackChangeItem
import com.evora.technologies.office.core.model.TrackChangeKind
import com.evora.technologies.office.core.model.WordDocumentSnapshot
import com.evora.technologies.office.core.model.WordParagraph
import com.evora.technologies.office.core.model.WorkbookSnapshot
import com.evora.technologies.office.core.model.WorksheetData
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified Office editor engine wiring LibreOfficeKit 16 KB JNI when present,
 * backed by a standards-compliant OOXML/ODF parser & serializer so Word/Excel/PPT
 * editing, formulas, Track Changes, presenter notes, and save work 100% offline.
 */
@Singleton
class OoxmlOfficeEngine @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val documentRepository: DocumentRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : OfficeEditorPort {

    override fun isNativeLokLoaded(): Boolean = LibreOfficeNativeBridge.ensureLoaded()

    override fun encodeWordDraft(snapshot: WordDocumentSnapshot): ByteArray =
        DraftSnapshotSerializer.encodeWord(snapshot)

    override fun decodeWordDraft(bytes: ByteArray): WordDocumentSnapshot =
        DraftSnapshotSerializer.decodeWord(bytes)

    override fun encodeWorkbookDraft(snapshot: WorkbookSnapshot): ByteArray =
        DraftSnapshotSerializer.encodeWorkbook(snapshot)

    override fun decodeWorkbookDraft(bytes: ByteArray): WorkbookSnapshot =
        DraftSnapshotSerializer.decodeWorkbook(bytes)

    override fun encodeSlideDeckDraft(snapshot: SlideDeckSnapshot): ByteArray =
        DraftSnapshotSerializer.encodeSlideDeck(snapshot)

    override fun decodeSlideDeckDraft(bytes: ByteArray): SlideDeckSnapshot =
        DraftSnapshotSerializer.decodeSlideDeck(bytes)

    override suspend fun loadWordDocument(uriString: String): Result<WordDocumentSnapshot> =
        withContext(ioDispatcher) {
            try {
                val lokReady = isNativeLokLoaded()
                val title = resolveFileName(uriString, "Document.docx")
                val entries = readZipEntries(uriString)
                val docXml = entries["word/document.xml"] ?: entries["content.xml"]
                if (docXml == null) {
                    return@withContext Result.success(
                        WordDocumentSnapshot(
                            title = title,
                            paragraphs = listOf(WordParagraph(id = "p_0", text = "")),
                            poweredByNativeLok = lokReady,
                        ),
                    )
                }

                val commentsMap = entries["word/comments.xml"]?.let(::parseWordComments).orEmpty()
                val (paragraphs, trackChanges, trackEnabled) = parseWordDocumentXml(docXml, commentsMap)
                Result.success(
                    WordDocumentSnapshot(
                        title = title,
                        paragraphs = paragraphs.ifEmpty { listOf(WordParagraph(id = "p_0", text = "")) },
                        trackChangesEnabled = trackEnabled,
                        trackChanges = trackChanges,
                        poweredByNativeLok = lokReady,
                    ),
                )
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    override suspend fun saveWordDocument(
        uriString: String,
        snapshot: WordDocumentSnapshot,
    ): Result<DocumentItem> = withContext(ioDispatcher) {
        try {
            val bytes = OoxmlDocxWriter.buildFromWordSnapshot(snapshot)
            writeBytesAndSyncMeta(uriString, snapshot.title, "docx", DocFormat.WORD, bytes)
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun loadWorkbook(uriString: String): Result<WorkbookSnapshot> =
        withContext(ioDispatcher) {
            try {
                val lokReady = isNativeLokLoaded()
                val title = resolveFileName(uriString, "Workbook.xlsx")
                val entries = readZipEntries(uriString)
                val sharedStrings = entries["xl/sharedStrings.xml"]?.let(::parseSharedStrings).orEmpty()
                val sheetRefs = entries["xl/workbook.xml"]?.let(::parseWorkbookSheets).orEmpty()

                val worksheets = if (sheetRefs.isNotEmpty()) {
                    sheetRefs.mapIndexed { idx, name ->
                        val sheetXml = entries["xl/worksheets/sheet${idx + 1}.xml"]
                        val raw = if (sheetXml != null) {
                            parseWorksheetXml(name, sheetXml, sharedStrings)
                        } else {
                            WorksheetData(name = name)
                        }
                        ExcelFormulaEvaluator.evaluateSheet(raw)
                    }
                } else {
                    val fallbackSheets = entries.keys
                        .filter { it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml") }
                        .sorted()
                    if (fallbackSheets.isEmpty()) {
                        listOf(WorksheetData(name = "Sheet1"))
                    } else {
                        fallbackSheets.mapIndexed { idx, key ->
                            ExcelFormulaEvaluator.evaluateSheet(
                                parseWorksheetXml("Sheet${idx + 1}", entries.getValue(key), sharedStrings),
                            )
                        }
                    }
                }

                Result.success(
                    WorkbookSnapshot(
                        title = title,
                        sheets = worksheets,
                        activeSheetIndex = 0,
                        poweredByNativeLok = lokReady,
                    ),
                )
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    override suspend fun saveWorkbook(
        uriString: String,
        snapshot: WorkbookSnapshot,
    ): Result<DocumentItem> = withContext(ioDispatcher) {
        try {
            val bytes = buildXlsxZip(snapshot)
            writeBytesAndSyncMeta(uriString, snapshot.title, "xlsx", DocFormat.EXCEL, bytes)
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun loadSlideDeck(uriString: String): Result<SlideDeckSnapshot> =
        withContext(ioDispatcher) {
            try {
                val lokReady = isNativeLokLoaded()
                val title = resolveFileName(uriString, "Presentation.pptx")
                val entries = readZipEntries(uriString)
                val slideKeys = entries.keys
                    .filter { it.startsWith("ppt/slides/slide") && it.endsWith(".xml") }
                    .sortedBy { extractTrailingNumber(it) }

                val slides = if (slideKeys.isEmpty()) {
                    listOf(
                        SlidePageData(
                            id = "slide_0",
                            elements = listOf(
                                SlideElement(id = "el_0", kind = "TEXT", text = title.substringBeforeLast('.')),
                            ),
                        ),
                    )
                } else {
                    slideKeys.mapIndexed { idx, key ->
                        val num = extractTrailingNumber(key)
                        val notesBytes = entries["ppt/notesSlides/notesSlide$num.xml"]
                        val notes = notesBytes?.let(::parsePlainXmlText).orEmpty()
                        val elements = parseSlideXml(entries.getValue(key), idx)
                        SlidePageData(
                            id = "slide_$idx",
                            elements = elements.ifEmpty {
                                listOf(SlideElement(id = "el_${idx}_0", kind = "TEXT", text = "Slide ${idx + 1}"))
                            },
                            presenterNotes = notes,
                        )
                    }
                }

                Result.success(
                    SlideDeckSnapshot(
                        title = title,
                        slides = slides,
                        poweredByNativeLok = lokReady,
                    ),
                )
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    override suspend fun saveSlideDeck(
        uriString: String,
        snapshot: SlideDeckSnapshot,
    ): Result<DocumentItem> = withContext(ioDispatcher) {
        try {
            val bytes = buildPptxZip(snapshot)
            writeBytesAndSyncMeta(uriString, snapshot.title, "pptx", DocFormat.PPT, bytes)
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun parseWordComments(bytes: ByteArray): Map<String, TrackChangeItem> {
        val out = LinkedHashMap<String, TrackChangeItem>()
        val parser = Xml.newPullParser().apply { setInput(bytes.inputStream(), "UTF-8") }
        var event = parser.eventType
        var currentId = ""
        var currentAuthor = "Reviewer"
        val currentText = StringBuilder()
        var inComment = false
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val local = localTag(parser.name)
                    if (local == "comment") {
                        inComment = true
                        currentId = attrValue(parser, "id").ifBlank { "${out.size + 1}" }
                        currentAuthor = attrValue(parser, "author").ifBlank { "Reviewer" }
                        currentText.clear()
                    }
                }
                XmlPullParser.TEXT -> if (inComment) currentText.append(parser.text.orEmpty())
                XmlPullParser.END_TAG -> {
                    if (localTag(parser.name) == "comment" && inComment) {
                        out[currentId] = TrackChangeItem(
                            id = currentId,
                            kind = TrackChangeKind.COMMENT,
                            paragraphIndex = 0,
                            text = currentText.toString().trim(),
                            author = currentAuthor,
                            timestampEpochMs = System.currentTimeMillis(),
                        )
                        inComment = false
                    }
                }
            }
            event = parser.next()
        }
        return out
    }

    private fun parseWordDocumentXml(
        bytes: ByteArray,
        commentsById: Map<String, TrackChangeItem>,
    ): Triple<List<WordParagraph>, List<TrackChangeItem>, Boolean> {
        val paragraphs = mutableListOf<WordParagraph>()
        val revisions = mutableListOf<TrackChangeItem>()
        var trackEnabled = false

        val parser = Xml.newPullParser().apply { setInput(bytes.inputStream(), "UTF-8") }
        var event = parser.eventType
        var inP = false
        var inIns = false
        var inDel = false
        var insAuthor = "Evora User"
        var delAuthor = "Evora User"
        val pText = StringBuilder()
        val insText = StringBuilder()
        val delText = StringBuilder()
        var bold = false
        var italic = false
        var underline = false
        var strike = false
        var align = TextAlignOption.LEFT
        var fontSizeSp = 14

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val tag = localTag(parser.name)
                    when (tag) {
                        "trackRevisions" -> trackEnabled = true
                        "p" -> {
                            inP = true
                            pText.clear()
                            bold = false
                            italic = false
                            underline = false
                            strike = false
                            align = TextAlignOption.LEFT
                            fontSizeSp = 14
                        }
                        "b" -> if (inP) bold = true
                        "i" -> if (inP) italic = true
                        "u" -> if (inP) underline = true
                        "strike" -> if (inP) strike = true
                        "jc" -> if (inP) {
                            align = when (attrValue(parser, "val").lowercase()) {
                                "center" -> TextAlignOption.CENTER
                                "right" -> TextAlignOption.RIGHT
                                "both", "distribute" -> TextAlignOption.JUSTIFY
                                else -> TextAlignOption.LEFT
                            }
                        }
                        "sz" -> if (inP) {
                            attrValue(parser, "val").toIntOrNull()?.let { hp ->
                                fontSizeSp = (hp / 2).coerceIn(10, 36)
                            }
                        }
                        "ins" -> if (inP) {
                            inIns = true
                            insAuthor = attrValue(parser, "author").ifBlank { "Reviewer" }
                            insText.clear()
                        }
                        "del" -> if (inP) {
                            inDel = true
                            delAuthor = attrValue(parser, "author").ifBlank { "Reviewer" }
                            delText.clear()
                        }
                        "commentReference" -> {
                            val cid = attrValue(parser, "id")
                            commentsById[cid]?.let { comment ->
                                revisions += comment.copy(paragraphIndex = paragraphs.size)
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    val txt = parser.text.orEmpty()
                    when {
                        inIns -> insText.append(txt)
                        inDel -> delText.append(txt)
                        inP -> pText.append(txt)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (localTag(parser.name)) {
                        "ins" -> if (inIns) {
                            val t = insText.toString().trim()
                            if (t.isNotEmpty()) {
                                revisions += TrackChangeItem(
                                    id = "ins_${revisions.size}",
                                    kind = TrackChangeKind.INSERTION,
                                    paragraphIndex = paragraphs.size,
                                    text = t,
                                    author = insAuthor,
                                    timestampEpochMs = System.currentTimeMillis(),
                                )
                            }
                            inIns = false
                        }
                        "del" -> if (inDel) {
                            val t = delText.toString().trim()
                            if (t.isNotEmpty()) {
                                revisions += TrackChangeItem(
                                    id = "del_${revisions.size}",
                                    kind = TrackChangeKind.DELETION,
                                    paragraphIndex = paragraphs.size,
                                    text = t,
                                    author = delAuthor,
                                    timestampEpochMs = System.currentTimeMillis(),
                                )
                            }
                            inDel = false
                        }
                        "p" -> if (inP) {
                            val text = pText.toString().trimEnd()
                            if (text.isNotEmpty() || paragraphs.isEmpty()) {
                                paragraphs += WordParagraph(
                                    id = "p_${paragraphs.size}",
                                    text = text,
                                    bold = bold,
                                    italic = italic,
                                    underline = underline,
                                    strikethrough = strike,
                                    fontSizeSp = fontSizeSp,
                                    alignment = align,
                                )
                            }
                            inP = false
                        }
                    }
                }
            }
            event = parser.next()
        }
        return Triple(paragraphs, revisions, trackEnabled)
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val list = mutableListOf<String>()
        val parser = Xml.newPullParser().apply { setInput(bytes.inputStream(), "UTF-8") }
        var event = parser.eventType
        var inSi = false
        val sb = StringBuilder()
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> if (localTag(parser.name) == "si") {
                    inSi = true
                    sb.clear()
                }
                XmlPullParser.TEXT -> if (inSi) sb.append(parser.text.orEmpty())
                XmlPullParser.END_TAG -> if (localTag(parser.name) == "si" && inSi) {
                    list += sb.toString()
                    inSi = false
                }
            }
            event = parser.next()
        }
        return list
    }

    private fun parseWorkbookSheets(bytes: ByteArray): List<String> {
        val names = mutableListOf<String>()
        val parser = Xml.newPullParser().apply { setInput(bytes.inputStream(), "UTF-8") }
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && localTag(parser.name) == "sheet") {
                val n = attrValue(parser, "name")
                if (n.isNotBlank()) names += n
            }
            event = parser.next()
        }
        return names
    }

    private fun parseWorksheetXml(
        sheetName: String,
        bytes: ByteArray,
        sharedStrings: List<String>,
    ): WorksheetData {
        val cells = LinkedHashMap<String, ExcelCell>()
        var frozenRows = 1
        var frozenCols = 0
        var maxRow = 19
        var maxCol = 7

        val parser = Xml.newPullParser().apply { setInput(bytes.inputStream(), "UTF-8") }
        var event = parser.eventType
        var cellAddr = ""
        var cellType = ""
        var inFormula = false
        var inValue = false
        var inInlineStr = false
        val formulaBuf = StringBuilder()
        val valBuf = StringBuilder()

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (localTag(parser.name)) {
                        "pane" -> {
                            frozenRows = attrValue(parser, "ySplit").toDoubleOrNull()?.toInt() ?: frozenRows
                            frozenCols = attrValue(parser, "xSplit").toDoubleOrNull()?.toInt() ?: frozenCols
                        }
                        "c" -> {
                            cellAddr = attrValue(parser, "r")
                            cellType = attrValue(parser, "t")
                            formulaBuf.clear()
                            valBuf.clear()
                        }
                        "f" -> inFormula = true
                        "v" -> inValue = true
                        "t" -> if (cellType == "inlineStr") inInlineStr = true
                    }
                }
                XmlPullParser.TEXT -> {
                    val txt = parser.text.orEmpty()
                    when {
                        inFormula -> formulaBuf.append(txt)
                        inValue -> valBuf.append(txt)
                        inInlineStr -> valBuf.append(txt)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (localTag(parser.name)) {
                        "f" -> inFormula = false
                        "v" -> inValue = false
                        "t" -> inInlineStr = false
                        "c" -> {
                            val coord = ExcelFormulaEvaluator.parseCellAddress(cellAddr)
                            if (coord != null) {
                                val (r, c) = coord
                                maxRow = maxOf(maxRow, r + 4)
                                maxCol = maxOf(maxCol, c + 2)
                                val formula = formulaBuf.toString().trim()
                                val rawVal = valBuf.toString().trim()
                                val displayInput = when {
                                    formula.isNotEmpty() -> "=$formula"
                                    cellType == "s" -> {
                                        val idx = rawVal.toIntOrNull() ?: -1
                                        sharedStrings.getOrElse(idx) { rawVal }
                                    }
                                    else -> rawVal
                                }
                                if (displayInput.isNotEmpty()) {
                                    cells["$r,$c"] = ExcelCell(
                                        row = r,
                                        col = c,
                                        rawInput = displayInput,
                                        evaluatedDisplay = displayInput,
                                        bold = r == 0,
                                        hAlign = if (displayInput.toDoubleOrNull() != null) CellHorizontalAlign.RIGHT else CellHorizontalAlign.LEFT,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            event = parser.next()
        }

        return WorksheetData(
            name = sheetName,
            rowCount = maxRow.coerceIn(15, 60),
            colCount = maxCol.coerceIn(6, 16),
            frozenRows = frozenRows,
            frozenCols = frozenCols,
            cells = cells,
        )
    }

    private fun buildXlsxZip(snapshot: WorkbookSnapshot): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            val sheets = snapshot.sheets.ifEmpty { listOf(WorksheetData("Sheet1")) }
            val contentTypes = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
                append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
                append("""<Default Extension="xml" ContentType="application/xml"/>""")
                append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
                sheets.forEachIndexed { idx, _ ->
                    append("""<Override PartName="/xl/worksheets/sheet${idx + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
                }
                append("""</Types>""")
            }
            writeZip(zos, "[Content_Types].xml", contentTypes)
            writeZip(
                zos,
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""",
            )
            val wbRels = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
                sheets.forEachIndexed { idx, _ ->
                    append("""<Relationship Id="rId${idx + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${idx + 1}.xml"/>""")
                }
                append("""</Relationships>""")
            }
            writeZip(zos, "xl/_rels/workbook.xml.rels", wbRels)

            val wbXml = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>""")
                sheets.forEachIndexed { idx, s ->
                    append("""<sheet name="${OoxmlDocxWriter.escapeXml(s.name)}" sheetId="${idx + 1}" r:id="rId${idx + 1}"/>""")
                }
                append("""</sheets></workbook>""")
            }
            writeZip(zos, "xl/workbook.xml", wbXml)

            sheets.forEachIndexed { idx, sheet ->
                val evaluated = ExcelFormulaEvaluator.evaluateSheet(sheet)
                val rowsGrouped = evaluated.cells.values
                    .filter { it.rawInput.isNotEmpty() }
                    .groupBy { it.row }
                    .toSortedMap()

                val sheetXml = buildString {
                    append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                    append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
                    append("""<sheetViews><sheetView workbookViewId="0">""")
                    if (evaluated.frozenRows > 0 || evaluated.frozenCols > 0) {
                        append("""<pane xSplit="${evaluated.frozenCols}" ySplit="${evaluated.frozenRows}" topLeftCell="${ExcelFormulaEvaluator.colIndexToName(evaluated.frozenCols)}${evaluated.frozenRows + 1}" activePane="bottomLeft" state="frozen"/>""")
                    }
                    append("""</sheetView></sheetViews>""")
                    append("<sheetData>")
                    for ((r, rowCells) in rowsGrouped) {
                        append("""<row r="${r + 1}">""")
                        for (cell in rowCells.sortedBy { it.col }) {
                            val addr = "${ExcelFormulaEvaluator.colIndexToName(cell.col)}${r + 1}"
                            val raw = cell.rawInput
                            if (raw.startsWith("=")) {
                                val f = OoxmlDocxWriter.escapeXml(raw.drop(1))
                                val cachedNum = cell.evaluatedDisplay.replace(",", "").replace("$", "").replace("%", "").toDoubleOrNull()
                                append("""<c r="$addr"><f>$f</f>""")
                                if (cachedNum != null) append("<v>$cachedNum</v>")
                                append("</c>")
                            } else {
                                val num = raw.toDoubleOrNull()
                                if (num != null) {
                                    append("""<c r="$addr"><v>$num</v></c>""")
                                } else {
                                    append("""<c r="$addr" t="inlineStr"><is><t xml:space="preserve">${OoxmlDocxWriter.escapeXml(raw)}</t></is></c>""")
                                }
                            }
                        }
                        append("</row>")
                    }
                    append("</sheetData>")
                    val merged = evaluated.cells.values.filter { it.mergedSpanRows > 1 || it.mergedSpanCols > 1 }
                    if (merged.isNotEmpty()) {
                        append("""<mergeCells count="${merged.size}">""")
                        merged.forEach { m ->
                            val start = "${ExcelFormulaEvaluator.colIndexToName(m.col)}${m.row + 1}"
                            val end = "${ExcelFormulaEvaluator.colIndexToName(m.col + m.mergedSpanCols - 1)}${m.row + m.mergedSpanRows}"
                            append("""<mergeCell ref="$start:$end"/>""")
                        }
                        append("</mergeCells>")
                    }
                    append("</worksheet>")
                }
                writeZip(zos, "xl/worksheets/sheet${idx + 1}.xml", sheetXml)
            }
        }
        return baos.toByteArray()
    }

    private fun parseSlideXml(bytes: ByteArray, slideIndex: Int): List<SlideElement> {
        val out = mutableListOf<SlideElement>()
        val parser = Xml.newPullParser().apply { setInput(bytes.inputStream(), "UTF-8") }
        var event = parser.eventType
        var inSp = false
        val sb = StringBuilder()
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> if (localTag(parser.name) == "sp") {
                    inSp = true
                    sb.clear()
                }
                XmlPullParser.TEXT -> if (inSp) {
                    val t = parser.text?.trim().orEmpty()
                    if (t.isNotEmpty()) {
                        if (sb.isNotEmpty()) sb.append('\n')
                        sb.append(t)
                    }
                }
                XmlPullParser.END_TAG -> if (localTag(parser.name) == "sp" && inSp) {
                    val txt = sb.toString().trim()
                    if (txt.isNotEmpty()) {
                        val idx = out.size
                        out += SlideElement(
                            id = "el_${slideIndex}_$idx",
                            kind = "TEXT",
                            text = txt,
                            xFraction = 0.08f,
                            yFraction = (0.12f + idx * 0.24f).coerceAtMost(0.75f),
                            widthFraction = 0.84f,
                            heightFraction = 0.22f,
                            zOrder = idx,
                        )
                    }
                    inSp = false
                }
            }
            event = parser.next()
        }
        return out
    }

    private fun buildPptxZip(snapshot: SlideDeckSnapshot): ByteArray {
        val baos = ByteArrayOutputStream()
        val slides = snapshot.slides.ifEmpty { listOf(SlidePageData("slide_0", emptyList())) }
        ZipOutputStream(baos).use { zos ->
            val ct = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
                append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
                append("""<Default Extension="xml" ContentType="application/xml"/>""")
                append("""<Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>""")
                slides.forEachIndexed { idx, s ->
                    append("""<Override PartName="/ppt/slides/slide${idx + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>""")
                    if (s.presenterNotes.isNotBlank()) {
                        append("""<Override PartName="/ppt/notesSlides/notesSlide${idx + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.notesSlide+xml"/>""")
                    }
                }
                append("""</Types>""")
            }
            writeZip(zos, "[Content_Types].xml", ct)
            writeZip(
                zos,
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/></Relationships>""",
            )
            val presRels = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
                slides.forEachIndexed { idx, _ ->
                    append("""<Relationship Id="rId${idx + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide${idx + 1}.xml"/>""")
                }
                append("""</Relationships>""")
            }
            writeZip(zos, "ppt/_rels/presentation.xml.rels", presRels)
            val presXml = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("""<p:presentation xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><p:sldIdLst>""")
                slides.forEachIndexed { idx, _ ->
                    append("""<p:sldId id="${256 + idx}" r:id="rId${idx + 1}"/>""")
                }
                append("""</p:sldIdLst><p:sldSz cx="9144000" cy="5143500"/></p:presentation>""")
            }
            writeZip(zos, "ppt/presentation.xml", presXml)

            slides.forEachIndexed { idx, slide ->
                val ordered = slide.elements.sortedBy { it.zOrder }
                val slideXml = buildString {
                    append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                    append("""<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"><p:cSld><p:spTree>""")
                    append("""<p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr/>""")
                    ordered.forEachIndexed { eIdx, el ->
                        val x = (el.xFraction * 9144000L).toLong()
                        val y = (el.yFraction * 5143500L).toLong()
                        val cx = (el.widthFraction * 9144000L).toLong().coerceAtLeast(457200L)
                        val cy = (el.heightFraction * 5143500L).toLong().coerceAtLeast(365760L)
                        val geom = if (el.kind == "SHAPE_OVAL") "ellipse" else "rect"
                        val fillHex = String.format("%06X", el.fillColorArgb and 0xFFFFFFL)
                        append("""<p:sp><p:nvSpPr><p:cNvPr id="${eIdx + 2}" name="Shape${eIdx + 1}"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>""")
                        append("""<p:spPr><a:xfrm><a:off x="$x" y="$y"/><a:ext cx="$cx" cy="$cy"/></a:xfrm><a:prstGeom prst="$geom"><a:avLst/></a:prstGeom><a:solidFill><a:srgbClr val="$fillHex"/></a:solidFill></p:spPr>""")
                        append("""<p:txBody><a:bodyPr/><a:lstStyle/><a:p><a:r><a:t>${OoxmlDocxWriter.escapeXml(el.text)}</a:t></a:r></a:p></p:txBody></p:sp>""")
                    }
                    append("""</p:spTree></p:cSld></p:sld>""")
                }
                writeZip(zos, "ppt/slides/slide${idx + 1}.xml", slideXml)

                if (slide.presenterNotes.isNotBlank()) {
                    writeZip(
                        zos,
                        "ppt/slides/_rels/slide${idx + 1}.xml.rels",
                        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rIdNotes" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/notesSlide" Target="../notesSlides/notesSlide${idx + 1}.xml"/></Relationships>""",
                    )
                    val notesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><p:notes xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"><p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr/><p:sp><p:nvSpPr><p:cNvPr id="2" name="Notes"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr><p:spPr/><p:txBody><a:bodyPr/><a:lstStyle/><a:p><a:r><a:t>${OoxmlDocxWriter.escapeXml(slide.presenterNotes)}</a:t></a:r></a:p></p:txBody></p:sp></p:spTree></p:cSld></p:notes>"""
                    writeZip(zos, "ppt/notesSlides/notesSlide${idx + 1}.xml", notesXml)
                }
            }
        }
        return baos.toByteArray()
    }

    private fun parsePlainXmlText(bytes: ByteArray): String {
        val parser = Xml.newPullParser().apply { setInput(bytes.inputStream(), "UTF-8") }
        var event = parser.eventType
        val sb = StringBuilder()
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.TEXT) {
                val t = parser.text?.trim().orEmpty()
                if (t.isNotEmpty()) {
                    if (sb.isNotEmpty()) sb.append(' ')
                    sb.append(t)
                }
            }
            event = parser.next()
        }
        return sb.toString().trim()
    }

    private suspend fun writeBytesAndSyncMeta(
        targetUriString: String,
        title: String,
        defaultExt: String,
        format: DocFormat,
        bytes: ByteArray,
    ): Result<DocumentItem> {
        val parsed = Uri.parse(targetUriString)
        val finalUriString: String = when {
            parsed.scheme == "file" && parsed.path != null -> {
                val targetFile = File(parsed.path!!)
                targetFile.parentFile?.mkdirs()
                FileOutputStream(targetFile).use { it.write(bytes) }
                targetFile.toURI().toString()
            }
            parsed.scheme == "content" -> {
                val writtenDirect = runCatching {
                    context.contentResolver.openOutputStream(parsed, "wt")?.use { it.write(bytes) } != null
                }.getOrDefault(false)
                if (writtenDirect) {
                    targetUriString
                } else {
                    saveFallbackFile(title, defaultExt, bytes)
                }
            }
            else -> saveFallbackFile(title, defaultExt, bytes)
        }
        val item = documentRepository.registerSafUri(finalUriString).getOrElse {
            DocumentItem(
                id = finalUriString,
                uriString = finalUriString,
                displayName = title,
                extension = defaultExt,
                format = format,
                sizeBytes = bytes.size.toLong(),
                modifiedAtEpochMs = System.currentTimeMillis(),
                parentFolderName = "EvoraDocuments",
            )
        }
        documentRepository.recordDocumentOpened(item, totalPages = 1)
        return Result.success(item)
    }

    private fun saveFallbackFile(title: String, ext: String, bytes: ByteArray): String {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: File(context.filesDir, "documents")
        val dir = File(baseDir, "EvoraDocuments").apply { mkdirs() }
        val base = title.substringBeforeLast('.').ifBlank { "Document" }
        val file = File(dir, "$base.$ext")
        FileOutputStream(file).use { it.write(bytes) }
        return file.toURI().toString()
    }

    private fun readZipEntries(uriString: String): Map<String, ByteArray> {
        val uri = Uri.parse(uriString)
        val input = if (uri.scheme == "file" && uri.path != null) {
            FileInputStream(File(uri.path!!))
        } else {
            context.contentResolver.openInputStream(uri)
                ?: runCatching { FileInputStream(File(uriString)) }.getOrNull()
                ?: error("Unable to open Office file: $uriString")
        }
        val map = LinkedHashMap<String, ByteArray>()
        input.use { stream ->
            ZipInputStream(stream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        map[entry.name] = zis.readBytes()
                    }
                    entry = zis.nextEntry
                }
            }
        }
        return map
    }

    private fun writeZip(zos: ZipOutputStream, name: String, xml: String) {
        zos.putNextEntry(ZipEntry(name))
        zos.write(xml.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }

    private fun resolveFileName(uriString: String, fallback: String): String =
        Uri.parse(uriString).lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: fallback

    private fun localTag(rawName: String?): String = rawName?.substringAfter(':') ?: ""

    private fun attrValue(parser: XmlPullParser, localName: String): String {
        for (i in 0 until parser.attributeCount) {
            if (parser.getAttributeName(i).substringAfter(':') == localName) {
                return parser.getAttributeValue(i).orEmpty()
            }
        }
        return ""
    }

    private fun extractTrailingNumber(name: String): Int =
        Regex("(\\d+)\\.xml$").find(name)?.groupValues?.get(1)?.toIntOrNull() ?: 0
}

@Module
@InstallIn(SingletonComponent::class)
abstract class OfficeEditorModule {
    @Binds
    @Singleton
    abstract fun bindOfficeEditorPort(impl: OoxmlOfficeEngine): OfficeEditorPort
}
