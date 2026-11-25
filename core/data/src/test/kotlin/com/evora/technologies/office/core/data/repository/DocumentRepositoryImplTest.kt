package com.evora.technologies.office.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.evora.technologies.office.core.database.EvoraOfficeDatabase
import com.evora.technologies.office.core.datastore.EvoraPreferencesDataSource
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DocumentRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var db: EvoraOfficeDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, EvoraOfficeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `importExternalUri resolves misnamed docx and trash restore round trips file`() = runTest {
        val prefsDs = PreferenceDataStoreFactory.create(
            scope = TestScope(UnconfinedTestDispatcher(testScheduler)),
            produceFile = { tempFolder.newFile("repo_prefs.preferences_pb") },
        )
        val prefsRepo = PreferencesRepositoryImpl(EvoraPreferencesDataSource(prefsDs))

        val docFile = tempFolder.newFile("fake_renamed.pdf")
        ZipOutputStream(FileOutputStream(docFile)).use { zos ->
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write("<Types/>".toByteArray())
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("word/document.xml"))
            zos.write("<w:document/>".toByteArray())
            zos.closeEntry()
        }

        val scanner = object : LocalDocumentScanner {
            override suspend fun scanDeviceDocuments(): List<DocumentItem> = emptyList()
        }
        val repo = DocumentRepositoryImpl(
            context = context,
            scanner = scanner,
            documentMetaDao = db.documentMetaDao(),
            trashDao = db.trashDao(),
            draftRecoveryDao = db.draftRecoveryDao(),
            preferencesRepository = prefsRepo,
            ioDispatcher = Dispatchers.Unconfined,
        )

        val imported = repo.importExternalUri(
            uriString = docFile.toURI().toString(),
            displayNameHint = "fake_renamed.pdf",
            mimeTypeHint = "application/pdf",
        ).getOrThrow()

        assertEquals(DocFormat.WORD, imported.format)
        assertEquals("docx", imported.resolvedExtension)

        // Move real file to trash and verify restore restores it back
        val localFile = tempFolder.newFile("TrashTarget.txt").apply { writeText("restore me") }
        val localUri = localFile.toURI().toString()
        repo.recordDocumentOpened(
            DocumentItem(localUri, localUri, "TrashTarget.txt", "txt", DocFormat.TEXT, 10L, 100L, "tmp"),
            totalPages = 1,
        )
        val trashed = repo.moveToTrash(localUri).getOrThrow()
        assertFalse(localFile.exists())
        assertTrue(File(trashed.cachedFilePath).exists())
        assertEquals(1, repo.observeTrashItems().first().size)

        repo.restoreFromTrash(trashed.trashId).getOrThrow()
        assertTrue(localFile.exists())
        assertEquals("restore me", localFile.readText())
        assertEquals(0, repo.observeTrashItems().first().size)
    }
}
