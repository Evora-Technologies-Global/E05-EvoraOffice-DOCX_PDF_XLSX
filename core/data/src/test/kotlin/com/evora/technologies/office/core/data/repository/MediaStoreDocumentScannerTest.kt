package com.evora.technologies.office.core.data.repository

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import com.evora.technologies.office.core.model.DocFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MediaStoreDocumentScannerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Robolectric.buildContentProvider(FakeMediaFilesProvider::class.java).create("media")
    }

    @Test
    fun `scanDeviceDocuments includes Android 11 plus MEDIA_TYPE_DOCUMENT alongside MEDIA_TYPE_NONE`() = runTest {
        val scanner = MediaStoreDocumentScanner(context, Dispatchers.Unconfined)
        val docs = scanner.scanDeviceDocuments()
        val names = docs.map { it.displayName }.toSet()

        assertTrue("Expected PDF classified as MEDIA_TYPE_DOCUMENT (6)", "annual_report.pdf" in names)
        assertTrue("Expected DOCX classified as MEDIA_TYPE_DOCUMENT (6)", "proposal.docx" in names)
        assertTrue("Expected ODT classified as MEDIA_TYPE_DOCUMENT (6)", "notes.odt" in names)
        assertTrue("Expected HWP classified as MEDIA_TYPE_NONE (0)", "hangul.hwp" in names)
        assertTrue("Expected ZIP classified as MEDIA_TYPE_NONE (0)", "bundle.zip" in names)
        assertFalse("Images (MEDIA_TYPE_IMAGE = 1) must be excluded", "photo.png" in names)
        assertEquals(DocFormat.PDF, docs.first { it.displayName == "annual_report.pdf" }.format)
        assertEquals(DocFormat.ARCHIVE, docs.first { it.displayName == "bundle.zip" }.format)
    }

    class FakeMediaFilesProvider : ContentProvider() {
        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor {
            val cols = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.RELATIVE_PATH,
            )
            val cursor = MatrixCursor(cols)
            val allowedMediaTypes = selectionArgs?.mapNotNull { it.toIntOrNull() }?.toSet().orEmpty()
            val sampleRows = listOf(
                arrayOf<Any>(101L, "annual_report.pdf", 2048L, 1700000100L, "Documents/", 6),
                arrayOf<Any>(102L, "proposal.docx", 4096L, 1700000090L, "Documents/", 6),
                arrayOf<Any>(103L, "notes.odt", 1024L, 1700000080L, "Documents/", 6),
                arrayOf<Any>(104L, "hangul.hwp", 5120L, 1700000070L, "Documents/", 0),
                arrayOf<Any>(105L, "bundle.zip", 512L, 1700000060L, "Documents/", 0),
                arrayOf<Any>(106L, "photo.png", 8192L, 1700000050L, "Pictures/", 1),
            )
            sampleRows.forEach { row ->
                val mediaType = row[5] as Int
                if (mediaType in allowedMediaTypes) {
                    cursor.addRow(arrayOf(row[0], row[1], row[2], row[3], row[4]))
                }
            }
            return cursor
        }

        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
    }
}
