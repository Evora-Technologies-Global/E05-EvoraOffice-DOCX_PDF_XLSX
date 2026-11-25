package com.evora.technologies.office.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.evora.technologies.office.core.database.entity.CloudAccountEntity
import com.evora.technologies.office.core.database.entity.DocumentMetaEntity
import com.evora.technologies.office.core.database.entity.DraftRecoveryEntity
import com.evora.technologies.office.core.database.entity.SignatureEntity
import com.evora.technologies.office.core.database.entity.TrashEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EvoraOfficeDatabaseTest {

    private lateinit var db: EvoraOfficeDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, EvoraOfficeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `document meta reading memory and favorite toggle round trip`() = runTest {
        val dao = db.documentMetaDao()
        val uri = "file:///storage/emulated/0/Documents/Contract.pdf"
        dao.upsert(
            DocumentMetaEntity(
                uriString = uri,
                displayName = "Contract.pdf",
                format = "PDF",
                sizeBytes = 2048L,
                modifiedAtEpochMs = 1000L,
            ),
        )

        dao.setFavorite(uri, true)
        dao.updateReadingMemory(
            uriString = uri,
            openedAtMs = 5000L,
            pageIndex = 4,
            totalPages = 18,
            scrollOffsetY = 124.5f,
            zoom = 1.75f,
            readerTheme = "SEPIA",
            isContinuousScroll = false,
        )

        val stored = dao.getByUri(uri)
        assertNotNull(stored)
        assertTrue(stored!!.isFavorite)
        assertEquals(4, stored.lastPageIndex)
        assertEquals(18, stored.lastTotalPages)
        assertEquals(124.5f, stored.lastScrollOffsetY, 0.01f)
        assertEquals(1.75f, stored.lastZoom, 0.01f)
        assertEquals("SEPIA", stored.readerTheme)
        assertFalse(stored.isContinuousScroll)
        assertEquals(1, dao.observeFavorites().first().size)
        assertEquals(1, dao.observeRecent().first().size)
    }

    @Test
    fun `trash entity expiration query and purge work cleanly`() = runTest {
        val trashDao = db.trashDao()
        trashDao.upsert(
            TrashEntity(
                trashId = "expired-1",
                originalUriString = "file:///docs/old.pdf",
                cachedFilePath = "/tmp/old.pdf",
                displayName = "old.pdf",
                format = "PDF",
                sizeBytes = 100L,
                trashedAtEpochMs = 100L,
                expiresAtEpochMs = 500L,
            ),
        )
        trashDao.upsert(
            TrashEntity(
                trashId = "active-1",
                originalUriString = "file:///docs/keep.docx",
                cachedFilePath = "/tmp/keep.docx",
                displayName = "keep.docx",
                format = "WORD",
                sizeBytes = 200L,
                trashedAtEpochMs = 100L,
                expiresAtEpochMs = 2000L,
            ),
        )

        val expired = trashDao.findExpired(nowEpochMs = 1000L)
        assertEquals(listOf("expired-1"), expired.map { it.trashId })

        val deletedCount = trashDao.deleteExpired(nowEpochMs = 1000L)
        assertEquals(1, deletedCount)
        assertNull(trashDao.getById("expired-1"))
        assertNotNull(trashDao.getById("active-1"))
    }

    @Test
    fun `draft recovery signature and cloud account entities round trip`() = runTest {
        val draftDao = db.draftRecoveryDao()
        val sigDao = db.signatureDao()
        val cloudDao = db.cloudAccountDao()

        draftDao.upsert(
            DraftRecoveryEntity(
                targetUriString = "file:///docs/draft.docx",
                draftCachePath = "/files/drafts/abc.docx",
                displayName = "draft.docx",
                updatedAtEpochMs = 9999L,
                hasPendingRecovery = true,
            ),
        )
        assertEquals(1, draftDao.observePending().first().size)
        draftDao.clearPending("file:///docs/draft.docx")
        assertEquals(0, draftDao.observePending().first().size)

        sigDao.upsert(SignatureEntity("sig-1", "/files/sigs/1.png", 1234L))
        assertEquals(1, sigDao.observeAll().first().size)

        cloudDao.upsert(
            CloudAccountEntity(
                accountId = "acc-1",
                provider = "GOOGLE_DRIVE",
                email = "user@evora.test",
                displayName = "Evora User",
                encryptedTokenAlias = "alias_1",
            ),
        )
        assertEquals(1, cloudDao.observeByProvider("GOOGLE_DRIVE").first().size)
    }
}
