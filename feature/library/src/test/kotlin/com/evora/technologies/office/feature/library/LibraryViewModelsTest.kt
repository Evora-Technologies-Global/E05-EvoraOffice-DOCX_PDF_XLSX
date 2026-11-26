package com.evora.technologies.office.feature.library

import app.cash.turbine.test
import com.evora.technologies.office.core.domain.port.ConsentPort
import com.evora.technologies.office.core.domain.port.DocumentRepository
import com.evora.technologies.office.core.domain.port.PreferencesRepository
import com.evora.technologies.office.core.domain.port.ProEntitlementPort
import com.evora.technologies.office.core.domain.port.RemoteConfigPort
import com.evora.technologies.office.core.domain.port.TemplateRepository
import com.evora.technologies.office.core.domain.util.DocumentListProcessor
import com.evora.technologies.office.core.domain.util.FileNameValidationResult
import com.evora.technologies.office.core.domain.util.FileNameValidator
import com.evora.technologies.office.core.model.AppThemeMode
import com.evora.technologies.office.core.model.ConsentState
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocTemplatePreset
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.ExternalIntentPayload
import com.evora.technologies.office.core.model.OfficeKind
import com.evora.technologies.office.core.model.ProFeature
import com.evora.technologies.office.core.model.ProStatus
import com.evora.technologies.office.core.model.ReaderBookmarkState
import com.evora.technologies.office.core.model.ReaderTheme
import com.evora.technologies.office.core.model.RemoteConfigSnapshot
import com.evora.technologies.office.core.model.SortOrder
import com.evora.technologies.office.core.model.TrashItem
import com.evora.technologies.office.feature.library.home.HomeDocumentsEffect
import com.evora.technologies.office.feature.library.home.HomeDocumentsIntent
import com.evora.technologies.office.feature.library.home.HomeDocumentsViewModel
import com.evora.technologies.office.feature.library.recent.RecentBookmarksEffect
import com.evora.technologies.office.feature.library.recent.RecentBookmarksIntent
import com.evora.technologies.office.feature.library.recent.RecentBookmarksViewModel
import com.evora.technologies.office.feature.library.recent.RecentSubTab
import com.evora.technologies.office.feature.library.search.SearchDocumentsIntent
import com.evora.technologies.office.feature.library.search.SearchDocumentsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeLibraryPrefsRepo : PreferencesRepository {
    override val onboardingCompleted = MutableStateFlow(true)
    override val selectedLocaleTag = MutableStateFlow<String?>("en")
    override val themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    override val defaultSortOrder = MutableStateFlow(SortOrder.MODIFIED_DESC)
    override val autosaveIntervalSec = MutableStateFlow(15)
    override val defaultAppPromptDismissed = MutableStateFlow(false)
    override val externalOpenCount = MutableStateFlow(0)
    override suspend fun setOnboardingCompleted(completed: Boolean) = Unit
    override suspend fun setSelectedLocaleTag(tag: String) = Unit
    override suspend fun setThemeMode(mode: AppThemeMode) = Unit
    override suspend fun setDefaultSortOrder(sortOrder: SortOrder) {
        defaultSortOrder.value = sortOrder
    }
    override suspend fun setAutosaveIntervalSec(seconds: Int) = Unit
    override suspend fun setDefaultAppPromptDismissed(dismissed: Boolean) = Unit
    override suspend fun incrementExternalOpenCount() = Unit
}

private class FakeLibraryDocRepo : DocumentRepository {
    val docs = MutableStateFlow(
        listOf(
            DocumentItem("1", "file:///docs/Contract.pdf", "Contract.pdf", "pdf", DocFormat.PDF, 1024L, 200L, "Docs"),
            DocumentItem("2", "file:///docs/Budget.xlsx", "Budget.xlsx", "xlsx", DocFormat.EXCEL, 2048L, 100L, "Finance"),
        ),
    )
    val trash = MutableStateFlow<List<TrashItem>>(emptyList())

    override fun observeLibraryDocuments(filter: DocFormat?, sortOrder: SortOrder): Flow<List<DocumentItem>> =
        docs.map { DocumentListProcessor.filterAndSort(it, filter, sortOrder) }

    override fun observeRecentDocuments(): Flow<List<DocumentItem>> = docs
    override fun observeFavoriteDocuments(): Flow<List<DocumentItem>> = docs.map { list -> list.filter { it.isFavorite } }
    override fun observeTrashItems(): Flow<List<TrashItem>> = trash
    override fun searchDocuments(query: String, filter: DocFormat?): Flow<List<DocumentItem>> =
        docs.map { DocumentListProcessor.filterAndSort(it, filter, SortOrder.MODIFIED_DESC, query) }

    override suspend fun refreshMediaStoreIndex(): Result<Int> = Result.success(docs.value.size)
    override suspend fun getDocumentMeta(uriString: String): DocumentItem? = docs.value.firstOrNull { it.uriString == uriString }
    override suspend fun getReaderBookmark(uriString: String): ReaderBookmarkState? = null
    override suspend fun toggleFavorite(uriString: String) {
        docs.value = docs.value.map { if (it.uriString == uriString) it.copy(isFavorite = !it.isFavorite) else it }
    }
    override suspend fun recordDocumentOpened(item: DocumentItem, totalPages: Int) = Unit
    override suspend fun updateReadingPosition(
        uriString: String,
        pageIndex: Int,
        totalPages: Int,
        scrollOffsetY: Float,
        zoom: Float,
        readerTheme: ReaderTheme,
        isContinuousScroll: Boolean,
    ) = Unit

    override suspend fun renameDocument(uriString: String, newBaseName: String): Result<DocumentItem> {
        val current = docs.value.first { it.uriString == uriString }
        return when (val v = FileNameValidator.validateAndPreserveExtension(newBaseName, current.extension)) {
            is FileNameValidationResult.Invalid -> Result.failure(IllegalArgumentException(v.reason))
            is FileNameValidationResult.Valid -> {
                val updated = current.copy(displayName = v.finalFileName)
                docs.value = docs.value.map { if (it.uriString == uriString) updated else it }
                Result.success(updated)
            }
        }
    }

    override suspend fun moveToTrash(uriString: String): Result<TrashItem> {
        val doc = docs.value.first { it.uriString == uriString }
        val item = TrashItem("t1", uriString, "/cache/${doc.displayName}", doc.displayName, doc.extension, doc.format, doc.sizeBytes, 1L, 999L)
        docs.value = docs.value.filterNot { it.uriString == uriString }
        trash.value = listOf(item)
        return Result.success(item)
    }

    override suspend fun restoreFromTrash(trashId: String): Result<Unit> {
        val entry = trash.value.first { it.trashId == trashId }
        trash.value = trash.value.filterNot { it.trashId == trashId }
        docs.value = docs.value + DocumentItem(entry.originalUriString, entry.originalUriString, entry.displayName, entry.extension, entry.format, entry.sizeBytes, 300L, "Docs")
        return Result.success(Unit)
    }

    override suspend fun deleteForeverFromTrash(trashId: String): Result<Unit> = Result.success(Unit)
    override suspend fun emptyTrash(): Result<Int> {
        val count = trash.value.size
        trash.value = emptyList()
        return Result.success(count)
    }

    override suspend fun copyDocumentToFolder(uriString: String, targetFolderName: String): Result<DocumentItem> =
        Result.failure(UnsupportedOperationException())
    override suspend fun moveDocumentToFolder(uriString: String, targetFolderName: String): Result<DocumentItem> =
        Result.failure(UnsupportedOperationException())
    override suspend fun importExternalUri(uriString: String, displayNameHint: String?, mimeTypeHint: String?): Result<ExternalIntentPayload> =
        Result.failure(UnsupportedOperationException())
    override suspend fun registerSafUri(uriString: String): Result<DocumentItem> =
        Result.failure(UnsupportedOperationException())
}

private class FakeLibraryTemplateRepo : TemplateRepository {
    override fun getAvailableTemplates(): List<DocTemplatePreset> = listOf(
        DocTemplatePreset("t1", "Blank Word", "Clean doc", OfficeKind.WORD, "blank_word.docx", "Untitled.docx"),
    )
    override suspend fun createDocumentFromTemplate(preset: DocTemplatePreset, customBaseName: String?): Result<DocumentItem> =
        Result.success(DocumentItem("tpl", "file:///docs/Untitled.docx", "Untitled.docx", "docx", DocFormat.WORD, 128L, 1L, "Docs"))
}

private class FakeLibraryProPort(initialPro: Boolean = false) : ProEntitlementPort {
    override val proStatus = MutableStateFlow(ProStatus(isPro = initialPro))
    override suspend fun hasAccess(feature: ProFeature) = proStatus.value.isUnlocked
    override suspend fun grantRewardedPass(hours: Int) = Unit
    override suspend fun restorePurchases() = false
}

private class FakeLibraryConsentPort : ConsentPort {
    override val consentState = MutableStateFlow(ConsentState(canRequestAds = true, isInitialized = true))
    override suspend fun gatherConsentIfNeeded() = consentState.value
    override suspend fun showPrivacyOptionsForm() = Result.success(consentState.value)
}

private class FakeLibraryRemoteConfigPort : RemoteConfigPort {
    override val config = MutableStateFlow(RemoteConfigSnapshot())
    override suspend fun fetchAndActivate(timeoutMs: Long) = config.value
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelsTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `home chip filtering, rename extension guard, and soft trash work`() = runTest {
        val repo = FakeLibraryDocRepo()
        val vm = HomeDocumentsViewModel(
            repo,
            FakeLibraryPrefsRepo(),
            FakeLibraryTemplateRepo(),
            FakeLibraryProPort(),
            FakeLibraryConsentPort(),
            FakeLibraryRemoteConfigPort(),
        )
        advanceUntilIdle()
        assertEquals(2, vm.state.value.documents.size)

        vm.onIntent(HomeDocumentsIntent.SelectFilter(DocFormat.PDF))
        advanceUntilIdle()
        assertEquals(1, vm.state.value.documents.size)
        assertEquals("Contract.pdf", vm.state.value.documents.first().displayName)

        // Illegal rename character blocked
        vm.onIntent(HomeDocumentsIntent.ConfirmRename("file:///docs/Contract.pdf", "bad/name"))
        advanceUntilIdle()
        assertTrue(vm.state.value.renameError != null)

        // Valid rename preserves .pdf extension
        vm.onIntent(HomeDocumentsIntent.ConfirmRename("file:///docs/Contract.pdf", "Signed_Contract"))
        advanceUntilIdle()
        assertNull(vm.state.value.renameError)
        assertEquals("Signed_Contract.pdf", vm.state.value.documents.first().displayName)

        // Move to trash removes from home library
        vm.effects.test {
            assertEquals(HomeDocumentsEffect.ShowMessage("Renamed to Signed_Contract.pdf"), awaitItem())
            vm.onIntent(HomeDocumentsIntent.MoveToTrash("file:///docs/Contract.pdf"))
            advanceUntilIdle()
            assertTrue(vm.state.value.documents.isEmpty())
            assertEquals(HomeDocumentsEffect.ShowMessage("Moved Signed_Contract.pdf to Trash"), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `recent view model restores soft trashed file cleanly`() = runTest {
        val repo = FakeLibraryDocRepo()
        repo.moveToTrash("file:///docs/Contract.pdf")
        val vm = RecentBookmarksViewModel(repo)
        advanceUntilIdle()

        vm.onIntent(RecentBookmarksIntent.SelectTab(RecentSubTab.TRASH))
        assertEquals(1, vm.state.value.trashItems.size)

        vm.effects.test {
            vm.onIntent(RecentBookmarksIntent.RestoreTrash("t1"))
            advanceUntilIdle()
            assertEquals(0, vm.state.value.trashItems.size)
            assertEquals(2, repo.docs.value.size)
            assertTrue(awaitItem() is RecentBookmarksEffect.ShowMessage)
        }
    }

    @Test
    fun `search view model debounces 300ms and filters matching query`() = runTest {
        val repo = FakeLibraryDocRepo()
        val vm = SearchDocumentsViewModel(repo)
        advanceUntilIdle()
        assertEquals(2, vm.state.value.results.size)

        vm.onIntent(SearchDocumentsIntent.QueryChanged("budget"))
        advanceTimeBy(350L)
        advanceUntilIdle()
        assertEquals(1, vm.state.value.results.size)
        assertEquals("Budget.xlsx", vm.state.value.results.first().displayName)
    }
}
