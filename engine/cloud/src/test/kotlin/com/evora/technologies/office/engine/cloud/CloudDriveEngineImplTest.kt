package com.evora.technologies.office.engine.cloud

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.evora.technologies.office.core.database.EvoraOfficeDatabase
import com.evora.technologies.office.core.model.CloudBrowsePage
import com.evora.technologies.office.core.model.CloudItem
import com.evora.technologies.office.core.model.CloudProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

private class InMemoryTokenVault : TokenVault {
    val map = mutableMapOf<String, String>()
    override suspend fun storeToken(alias: String, token: String) {
        map[alias] = token
    }
    override suspend fun readToken(alias: String): String? = map[alias]
    override suspend fun deleteToken(alias: String) {
        map.remove(alias)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CloudDriveEngineImplTest {

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
    fun `connect stores encrypted alias only, browse and download succeed, disconnect wipes token`() = runTest {
        val vault = InMemoryTokenVault()
        val fakeGoogle = object : CloudRemoteProvider {
            override suspend fun listFolder(
                accessToken: String,
                folderId: String,
                pageToken: String?,
                query: String,
            ): CloudBrowsePage {
                assertEquals("secret-token-xyz", accessToken)
                return CloudBrowsePage(
                    items = listOf(
                        CloudItem("g1", "Quarterly.pdf", CloudProvider.GOOGLE_DRIVE, false, "application/pdf", 120L, 100L, "root"),
                    ),
                    nextPageToken = "page2",
                )
            }

            override suspend fun downloadFile(
                accessToken: String,
                item: CloudItem,
                destinationFile: File,
                onProgress: (Long, Long) -> Unit,
            ): File {
                destinationFile.writeText("PDF_CONTENT")
                onProgress(11L, 11L)
                return destinationFile
            }

            override suspend fun uploadFile(
                accessToken: String,
                localFile: File,
                mimeType: String,
                parentFolderId: String,
                onProgress: (Long, Long) -> Unit,
            ): CloudItem {
                onProgress(localFile.length(), localFile.length())
                return CloudItem("up1", localFile.name, CloudProvider.GOOGLE_DRIVE, false, mimeType, localFile.length(), 100L, parentFolderId)
            }
        }

        val engine = CloudDriveEngineImpl(
            context = context,
            cloudAccountDao = db.cloudAccountDao(),
            documentMetaDao = db.documentMetaDao(),
            tokenVault = vault,
            googleDriveClient = fakeGoogle,
            oneDriveClient = fakeGoogle,
            ioDispatcher = Dispatchers.Unconfined,
        )

        val account = engine.connectGoogleDrive("secret-token-xyz", "ceo@evora.test", "CEO").getOrThrow()
        assertFalse(account.encryptedTokenAlias.contains("secret-token-xyz"))
        assertEquals("secret-token-xyz", vault.readToken(account.encryptedTokenAlias))

        val accounts = engine.observeConnectedAccounts().first()
        assertEquals(1, accounts.size)

        val page = engine.listFolder(account, "root").getOrThrow()
        assertEquals(1, page.items.size)
        assertEquals("page2", page.nextPageToken)

        val doc = engine.downloadFile(account, page.items.first()).getOrThrow()
        assertTrue(doc.displayName.endsWith(".pdf"))
        assertTrue(db.documentMetaDao().getByUri(doc.uriString) != null)

        engine.disconnectAccount(account.accountId).getOrThrow()
        assertNull(vault.readToken(account.encryptedTokenAlias))
        assertEquals(0, engine.observeConnectedAccounts().first().size)
    }
}
