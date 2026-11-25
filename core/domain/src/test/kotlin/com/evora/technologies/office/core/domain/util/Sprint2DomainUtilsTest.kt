package com.evora.technologies.office.core.domain.util

import com.evora.technologies.office.core.model.CellNumberFormat
import com.evora.technologies.office.core.model.ExcelCell
import com.evora.technologies.office.core.model.QrDecodedAction
import com.evora.technologies.office.core.model.TrackChangeItem
import com.evora.technologies.office.core.model.TrackChangeKind
import com.evora.technologies.office.core.model.WordDocumentSnapshot
import com.evora.technologies.office.core.model.WordParagraph
import com.evora.technologies.office.core.model.WorksheetData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.zip.ZipInputStream

class Sprint2DomainUtilsTest {
    @Test
    fun `page range parser parses ranges, singles, and rejects out of bounds`() {
        val ok = PageRangeParser.parseOneBasedRanges("1-3, 5, 2", totalPages = 6).getOrThrow()
        assertEquals(listOf(0, 1, 2, 4), ok)

        val err = PageRangeParser.parseOneBasedRanges("1-8", totalPages = 5)
        assertTrue(err.isFailure)

        assertEquals(listOf(listOf(0, 1), listOf(2, 3), listOf(4)), PageRangeParser.splitEveryN(5, 2))
    }

    @Test
    fun `excel formula evaluator computes SUM, AVERAGE, COUNT, MIN, MAX, IF and arithmetic`() {
        val sheet = WorksheetData(
            name = "Budget",
            cells = mapOf(
                "0,0" to ExcelCell(0, 0, rawInput = "10"),
                "1,0" to ExcelCell(1, 0, rawInput = "20"),
                "2,0" to ExcelCell(2, 0, rawInput = "30"),
                "3,0" to ExcelCell(3, 0, rawInput = "=SUM(A1:A3)", numberFormat = CellNumberFormat.CURRENCY),
                "4,0" to ExcelCell(4, 0, rawInput = "=AVERAGE(A1:A3)"),
                "5,0" to ExcelCell(5, 0, rawInput = "=COUNT(A1:A3)"),
                "6,0" to ExcelCell(6, 0, rawInput = "=MIN(A1:A3)"),
                "7,0" to ExcelCell(7, 0, rawInput = "=MAX(A1:A3)"),
                "8,0" to ExcelCell(8, 0, rawInput = "=IF(A4>50,\"Over\",\"OK\")"),
                "9,0" to ExcelCell(9, 0, rawInput = "=A1*2+A2"),
            ),
        )
        val evaluated = ExcelFormulaEvaluator.evaluateSheet(sheet)
        assertEquals("$60.00", evaluated.cells["3,0"]?.evaluatedDisplay)
        assertEquals("20", evaluated.cells["4,0"]?.evaluatedDisplay)
        assertEquals("3", evaluated.cells["5,0"]?.evaluatedDisplay)
        assertEquals("10", evaluated.cells["6,0"]?.evaluatedDisplay)
        assertEquals("30", evaluated.cells["7,0"]?.evaluatedDisplay)
        assertEquals("Over", evaluated.cells["8,0"]?.evaluatedDisplay)
        assertEquals("40", evaluated.cells["9,0"]?.evaluatedDisplay)
    }

    @Test
    fun `qr payload parser detects url, wifi, vcard, email, phone, and plain text`() {
        assertTrue(QrPayloadParser.parse("https://evora.dev") is QrDecodedAction.Url)
        val wifi = QrPayloadParser.parse("WIFI:T:WPA;S:EvoraLab;P:secret123;;") as QrDecodedAction.Wifi
        assertEquals("EvoraLab", wifi.ssid)
        assertEquals("secret123", wifi.password)
        val card = QrPayloadParser.parse("BEGIN:VCARD\nFN:Jane Doe\nTEL:+12345\nEMAIL:a@b.com\nEND:VCARD") as QrDecodedAction.VCard
        assertEquals("Jane Doe", card.fullName)
        assertTrue(QrPayloadParser.parse("mailto:hi@evora.dev?subject=Hello") is QrDecodedAction.Email)
        assertTrue(QrPayloadParser.parse("tel:+84901234567") is QrDecodedAction.Phone)
        assertTrue(QrPayloadParser.parse("plain note") is QrDecodedAction.PlainText)
    }

    @Test
    fun `ooxml docx writer embeds track changes and comments xml`() {
        val snap = WordDocumentSnapshot(
            title = "Contract.docx",
            paragraphs = listOf(WordParagraph(id = "p0", text = "Clause 1", bold = true)),
            trackChangesEnabled = true,
            trackChanges = listOf(
                TrackChangeItem("1", TrackChangeKind.INSERTION, 0, "updated term", timestampEpochMs = 1_700_000_000_000L),
                TrackChangeItem("2", TrackChangeKind.COMMENT, 0, "Please review", timestampEpochMs = 1_700_000_000_000L),
            ),
        )
        val zipBytes = OoxmlDocxWriter.buildFromWordSnapshot(snap)
        val entries = mutableMapOf<String, String>()
        ZipInputStream(zipBytes.inputStream()).use { zis ->
            var e = zis.nextEntry
            while (e != null) {
                entries[e.name] = String(zis.readBytes(), Charsets.UTF_8)
                e = zis.nextEntry
            }
        }
        val docXml = entries["word/document.xml"].orEmpty()
        assertTrue(docXml.contains("<w:trackRevisions/>"))
        assertTrue(docXml.contains("<w:ins "))
        assertTrue(entries["word/comments.xml"].orEmpty().contains("Please review"))
    }
}
