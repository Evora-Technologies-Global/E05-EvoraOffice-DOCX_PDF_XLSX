package com.evora.technologies.office.engine.hwp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.evora.technologies.office.core.model.DocFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HwpAndTextEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `sanitizer strips script tags, event handlers, svg, and external URLs`() {
        val html = HwpHtmlSanitizer.wrapDocumentHtml(
            title = "<script>alert(1)</script>Title",
            bodyHtml = """<p onclick="evil()" onload=bad()>Text</p><script>steal()</script><svg onload=x()><path/></svg><img src="https://evil.test/x.png"/><a href="data:text/html,1">x</a>""",
        )

        assertFalse(html.contains("<script>steal()</script>"))
        assertFalse(html.contains("onclick="))
        assertFalse(html.contains("onload="))
        assertFalse(html.contains("<svg"))
        assertFalse(html.contains("https://evil.test"))
        assertFalse(html.contains("data:text/html"))
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;Title"))
        assertTrue(html.contains("default-src 'none'"))
    }

    @Test
    fun `text engine parses CSV and EPUB spine chapters offline`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = TextEpubEngineImpl(context, Dispatchers.Unconfined)

        val csv = tempFolder.newFile("budget.csv").apply {
            writeText("Category,Amount,\"Notes, Extra\"\nRent,1200,\"Paid, Oct\"")
        }
        val parsedCsv = engine.loadDocument(csv.toURI().toString(), DocFormat.TEXT)
        assertTrue(parsedCsv.fullPlainText.contains("Category  |  Amount  |  Notes, Extra"))
        assertTrue(parsedCsv.fullPlainText.contains("Rent  |  1200  |  Paid, Oct"))

        val epubFile = tempFolder.newFile("sample.epub")
        ZipOutputStream(FileOutputStream(epubFile)).use { zos ->
            zos.putNextEntry(ZipEntry("META-INF/container.xml"))
            zos.write(
                """<?xml version="1.0"?><container version="1.0"><rootfiles><rootfile full-path="OEBPS/content.opf"/></rootfiles></container>"""
                    .toByteArray(),
            )
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("OEBPS/content.opf"))
            zos.write(
                """<?xml version="1.0"?><package><manifest><item id="c1" href="ch1.xhtml"/></manifest><spine><itemref idref="c1"/></spine></package>"""
                    .toByteArray(),
            )
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("OEBPS/ch1.xhtml"))
            zos.write("<html><body><h1>Intro</h1><p>Offline chapter one.</p></body></html>".toByteArray())
            zos.closeEntry()
        }

        val parsedEpub = engine.loadDocument(epubFile.toURI().toString(), DocFormat.EPUB)
        assertEquals(1, parsedEpub.chapters.size)
        assertTrue(parsedEpub.chapters.first().body.contains("Offline chapter one."))
    }
}
