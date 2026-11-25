package com.evora.technologies.office.core.domain.util

import com.evora.technologies.office.core.model.DocFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MagicByteSnifferTest {

    @Test
    fun `sniff identifies renamed docx disguised as pdf via zip contents`() {
        val docxBytes = buildZipBytes(
            "[Content_Types].xml" to "<Types/>",
            "word/document.xml" to "<w:document/>",
        )

        val result = MagicByteSniffer.sniff(
            bytes = docxBytes,
            displayNameHint = "misnamed_contract.pdf",
            mimeTypeHint = "application/pdf",
        )

        assertEquals(DocFormat.WORD, result.format)
        assertEquals("docx", result.extension)
        assertEquals("misnamed_contract", result.baseName)
    }

    @Test
    fun `sniff identifies pdf magic header even without extension`() {
        val pdfBytes = "%PDF-1.7\n1 0 obj".toByteArray()

        val result = MagicByteSniffer.sniff(pdfBytes, displayNameHint = "invoice")

        assertEquals(DocFormat.PDF, result.format)
        assertEquals("pdf", result.extension)
    }

    @Test
    fun `sniff identifies hwpx archive via Contents header xml`() {
        val hwpxBytes = buildZipBytes(
            "Contents/header.xml" to "<hh:head/>",
            "Contents/section0.xml" to "<hs:sec/>",
        )

        val result = MagicByteSniffer.sniff(hwpxBytes, displayNameHint = "report.zip")

        assertEquals(DocFormat.HWP, result.format)
        assertEquals("hwpx", result.extension)
    }

    @Test
    fun `sniff identifies 7z header`() {
        val sevenZBytes = byteArrayOf(0x37, 0x7A, 0xBC.toByte(), 0xAF.toByte(), 0x27, 0x1C, 0x00, 0x04)

        val result = MagicByteSniffer.sniff(sevenZBytes, displayNameHint = "archive.bin")

        assertEquals(DocFormat.ARCHIVE, result.format)
        assertEquals("7z", result.extension)
    }

    @Test
    fun `validator rejects illegal characters and preserves extension`() {
        val valid = FileNameValidator.validateAndPreserveExtension(
            rawInput = "Q4_Final_Contract",
            originalExtension = "pdf",
            existingNamesInFolder = setOf("Other.pdf"),
        )
        assertEquals(FileNameValidationResult.Valid("Q4_Final_Contract.pdf"), valid)

        val invalidChar = FileNameValidator.validateAndPreserveExtension("bad/name", "pdf")
        assertTrue(invalidChar is FileNameValidationResult.Invalid)

        val duplicate = FileNameValidator.validateAndPreserveExtension(
            rawInput = "Contract.pdf",
            originalExtension = "pdf",
            existingNamesInFolder = setOf("contract.pdf"),
        )
        assertTrue(duplicate is FileNameValidationResult.Invalid)
    }

    private fun buildZipBytes(vararg entries: Pair<String, String>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            entries.forEach { (name, content) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray())
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }
}
