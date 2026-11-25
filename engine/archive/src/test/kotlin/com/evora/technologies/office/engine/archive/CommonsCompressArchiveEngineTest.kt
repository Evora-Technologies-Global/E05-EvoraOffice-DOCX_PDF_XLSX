package com.evora.technologies.office.engine.archive

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.evora.technologies.office.core.domain.port.DocumentPasswordRequiredException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CommonsCompressArchiveEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createEngine(): CommonsCompressArchiveEngine {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return CommonsCompressArchiveEngine(context, Dispatchers.Unconfined)
    }

    @Test
    fun `zip archive inspects entries and extracts cleanly`() = runTest {
        val zipFile = tempFolder.newFile("sample.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            zos.putNextEntry(ZipEntry("docs/readme.txt"))
            zos.write("hello evora".toByteArray())
            zos.closeEntry()
        }

        val engine = createEngine()
        val inspection = engine.listEntries(zipFile.toURI().toString())
        assertTrue(inspection.isSupported)
        assertEquals(1, inspection.entries.size)
        assertEquals("readme.txt", inspection.entries.first().name)

        val outDir = tempFolder.newFolder("extracted_zip")
        val extracted = engine.extractEntries(zipFile.toURI().toString(), outDir.absolutePath)
        assertTrue(extracted.isSuccess)
        assertEquals("hello evora", File(outDir, "docs/readme.txt").readText())
    }

    @Test
    fun `zip slip traversal entry is blocked during extraction`() = runTest {
        val zipFile = tempFolder.newFile("evil.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            zos.putNextEntry(ZipEntry("../../outside.txt"))
            zos.write("blocked".toByteArray())
            zos.closeEntry()
        }

        val engine = createEngine()
        val outDir = tempFolder.newFolder("safe_dest")
        val result = engine.extractEntries(zipFile.toURI().toString(), outDir.absolutePath)
        assertTrue(result.isFailure)
        assertFalse(File(outDir.parentFile, "outside.txt").exists())
    }

    @Test
    fun `7z archive inspects and extracts cleanly`() = runTest {
        val source = tempFolder.newFile("note.txt").apply { writeText("7z offline payload") }
        val archive7z = tempFolder.newFile("bundle.7z")
        SevenZOutputFile(archive7z).use { out ->
            val entry = out.createArchiveEntry(source, "folder/note.txt")
            out.putArchiveEntry(entry)
            out.write(source.readBytes())
            out.closeArchiveEntry()
        }

        val engine = createEngine()
        val inspection = engine.listEntries(archive7z.toURI().toString())
        assertTrue(inspection.isSupported)
        assertEquals("note.txt", inspection.entries.first().name)

        val outDir = tempFolder.newFolder("extracted_7z")
        val extracted = engine.extractEntries(archive7z.toURI().toString(), outDir.absolutePath)
        assertTrue(extracted.isSuccess)
        assertEquals("7z offline payload", File(outDir, "folder/note.txt").readText())
    }

    @Test
    fun `rar archive returns honest unsupported message`() = runTest {
        val rarFile = tempFolder.newFile("legacy.rar").apply { writeText("Rar!") }
        val engine = createEngine()

        val inspection = engine.listEntries(rarFile.toURI().toString())
        assertFalse(inspection.isSupported)
        assertTrue(inspection.unsupportedReason?.contains(".zip and .7z") == true)
    }

    @Test
    fun `empty password failure check compiles against typed exception`() {
        val ex = DocumentPasswordRequiredException()
        assertEquals("Password required", ex.message)
    }
}
