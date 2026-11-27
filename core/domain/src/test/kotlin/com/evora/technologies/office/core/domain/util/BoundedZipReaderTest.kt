package com.evora.technologies.office.core.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BoundedZipReaderTest {

    @Test
    fun `reads filtered entries cleanly within safety bounds`() {
        val bytes = buildZip(
            "word/document.xml" to "<w:p/>".toByteArray(),
            "word/styles.xml" to "<styles/>".toByteArray(),
        )
        val result = BoundedZipReader.readEntries(
            inputStream = ByteArrayInputStream(bytes),
            entryFilter = { it == "word/document.xml" },
        )
        assertEquals(setOf("word/document.xml"), result.keys)
        assertEquals("<w:p/>", result.getValue("word/document.xml").toString(Charsets.UTF_8))
    }

    @Test
    fun `blocks zip bomb exceeding max entry limit`() {
        val entries = (1..12).map { "item_$it.txt" to "x".toByteArray() }
        val bytes = buildZip(*entries.toTypedArray())

        val error = assertThrows(IllegalArgumentException::class.java) {
            BoundedZipReader.readEntries(
                inputStream = ByteArrayInputStream(bytes),
                maxEntries = 10,
            )
        }
        assertTrue(error.message.orEmpty().contains("entry limit"))
    }

    @Test
    fun `blocks zip bomb exceeding max uncompressed byte limit`() {
        val payload = ByteArray(2048) { 'A'.code.toByte() }
        val bytes = buildZip("bomb.xml" to payload)

        val error = assertThrows(IllegalArgumentException::class.java) {
            BoundedZipReader.readEntries(
                inputStream = ByteArrayInputStream(bytes),
                maxTotalBytes = 1024L,
            )
        }
        assertTrue(error.message.orEmpty().contains("uncompressed size limit"))
    }

    private fun buildZip(vararg items: Pair<String, ByteArray>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            for ((name, data) in items) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(data)
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }
}
