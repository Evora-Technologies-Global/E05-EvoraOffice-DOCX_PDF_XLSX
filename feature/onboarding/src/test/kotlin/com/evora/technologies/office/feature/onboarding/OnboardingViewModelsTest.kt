package com.evora.technologies.office.feature.onboarding

import app.cash.turbine.test
import com.evora.technologies.office.core.domain.port.DocumentRepository
import com.evora.technologies.office.core.domain.port.PreferencesRepository
import com.evora.technologies.office.core.model.AppLanguage
import com.evora.technologies.office.core.model.AppThemeMode
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.ExternalIntentPayload
import com.evora.technologies.office.core.model.ReaderBookmarkState
import com.evora.technologies.office.core.model.ReaderTheme
import com.evora.technologies.office.core.model.SortOrder
import com.evora.technologies.office.core.model.TrashItem
import com.evora.technologies.office.feature.onboarding.intro.OnboardingIntroEffect
import com.evora.technologies.office.feature.onboarding.intro.OnboardingIntroIntent
import com.evora.technologies.office.feature.onboarding.intro.OnboardingIntroViewModel
import com.evora.technologies.office.feature.onboarding.language.LanguageSelectEffect
import com.evora.technologies.office.feature.onboarding.language.LanguageSelectIntent
import com.evora.technologies.office.feature.onboarding.language.LanguageSelectViewModel
import com.evora.technologies.office.feature.onboarding.permission.StoragePermissionEffect
import com.evora.technologies.office.feature.onboarding.permission.StoragePermissionIntent
import com.evora.technologies.office.feature.onboarding.permission.StoragePermissionViewModel
import com.evora.technologies.office.feature.onboarding.splash.SplashEffect
import com.evora.technologies.office.feature.onboarding.splash.SplashIntent
import com.evora.technologies.office.feature.onboarding.splash.SplashViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakePrefsRepo(initialOnboarding: Boolean = false) : PreferencesRepository {
    val onboarding = MutableStateFlow(initialOnboarding)
    val locale = MutableStateFlow<String?>(null)
    override val onboardingCompleted: Flow<Boolean> = onboarding
    override val selectedLocaleTag: Flow<String?> = locale
    override val themeMode: Flow<AppThemeMode> = MutableStateFlow(AppThemeMode.SYSTEM)
    override val defaultSortOrder: Flow<SortOrder> = MutableStateFlow(SortOrder.MODIFIED_DESC)
    override val autosaveIntervalSec: Flow<Int> = MutableStateFlow(15)
    override val defaultAppPromptDismissed: Flow<Boolean> = MutableStateFlow(false)
    override val externalOpenCount: Flow<Int> = MutableStateFlow(0)

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        onboarding.value = completed
    }

    override suspend fun setSelectedLocaleTag(tag: String) {
        locale.value = tag
    }

    override suspend fun setThemeMode(mode: AppThemeMode) = Unit
    override suspend fun setDefaultSortOrder(sortOrder: SortOrder) = Unit
    override suspend fun setAutosaveIntervalSec(seconds: Int) = Unit
    override suspend fun setDefaultAppPromptDismissed(dismissed: Boolean) = Unit
    override suspend fun incrementExternalOpenCount() = Unit
}

private class FakeOnboardingDocRepo : DocumentRepository {
    var refreshedCount = 0
    var registeredSafUri: String? = null
    override fun observeLibraryDocuments(filter: DocFormat?, sortOrder: SortOrder): Flow<List<DocumentItem>> = emptyFlow()
    override fun observeRecentDocuments(): Flow<List<DocumentItem>> = emptyFlow()
    override fun observeFavoriteDocuments(): Flow<List<DocumentItem>> = emptyFlow()
    override fun observeTrashItems(): Flow<List<TrashItem>> = emptyFlow()
    override fun searchDocuments(query: String, filter: DocFormat?): Flow<List<DocumentItem>> = emptyFlow()
    override suspend fun refreshMediaStoreIndex(): Result<Int> {
        refreshedCount++
        return Result.success(10)
    }
    override suspend fun getDocumentMeta(uriString: String): DocumentItem? = null
    override suspend fun getReaderBookmark(uriString: String): ReaderBookmarkState? = null
    override suspend fun toggleFavorite(uriString: String) = Unit
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
    override suspend fun renameDocument(uriString: String, newBaseName: String): Result<DocumentItem> =
        Result.failure(UnsupportedOperationException())
    override suspend fun moveToTrash(uriString: String): Result<TrashItem> =
        Result.failure(UnsupportedOperationException())
    override suspend fun restoreFromTrash(trashId: String): Result<Unit> = Result.success(Unit)
    override suspend fun deleteForeverFromTrash(trashId: String): Result<Unit> = Result.success(Unit)
    override suspend fun emptyTrash(): Result<Int> = Result.success(0)
    override suspend fun copyDocumentToFolder(uriString: String, targetFolderName: String): Result<DocumentItem> =
        Result.failure(UnsupportedOperationException())
    override suspend fun moveDocumentToFolder(uriString: String, targetFolderName: String): Result<DocumentItem> =
        Result.failure(UnsupportedOperationException())
    override suspend fun importExternalUri(
        uriString: String,
        displayNameHint: String?,
        mimeTypeHint: String?,
    ): Result<ExternalIntentPayload> = Result.success(
        ExternalIntentPayload(
            uriString = "file:///cache/contract.docx",
            displayName = "contract.docx",
            format = DocFormat.WORD,
            resolvedExtension = "docx",
        ),
    )
    override suspend fun registerSafUri(uriString: String): Result<DocumentItem> {
        registeredSafUri = uriString
        return Result.failure(UnsupportedOperationException())
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelsTest {
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
    fun `splash routes external ACTION_VIEW directly to reader skipping onboarding`() = runTest {
        val vm = SplashViewModel(FakePrefsRepo(initialOnboarding = false), FakeOnboardingDocRepo())
        vm.effects.test {
            vm.onIntent(
                SplashIntent.StartRouting(
                    externalUri = "content://downloads/renamed.pdf",
                    displayNameHint = "renamed.pdf",
                ),
            )
            advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is SplashEffect.NavigateToExternalDocument)
            assertEquals(DocFormat.WORD, (effect as SplashEffect.NavigateToExternalDocument).payload.format)
        }
    }

    @Test
    fun `language select saves tag and emits ApplyLocale and NavigateToIntro`() = runTest {
        val prefs = FakePrefsRepo()
        val vm = LanguageSelectViewModel(prefs)
        vm.effects.test {
            vm.onIntent(LanguageSelectIntent.SelectLanguage(AppLanguage.VI))
            vm.onIntent(LanguageSelectIntent.ContinueClicked)
            advanceUntilIdle()
            assertEquals("vi", prefs.locale.value)
            assertEquals(LanguageSelectEffect.ApplyLocale("vi"), awaitItem())
            assertEquals(LanguageSelectEffect.NavigateToIntro, awaitItem())
        }
    }

    @Test
    fun `intro skips cleanly to next onboarding step`() = runTest {
        val vm = OnboardingIntroViewModel()
        vm.effects.test {
            vm.onIntent(OnboardingIntroIntent.SkipClicked)
            assertEquals(OnboardingIntroEffect.NavigateNext, awaitItem())
        }
    }

    @Test
    fun `denying all-files permission still completes onboarding via SAF fallback`() = runTest {
        val prefs = FakePrefsRepo()
        val docs = FakeOnboardingDocRepo()
        val vm = StoragePermissionViewModel(prefs, docs)
        vm.effects.test {
            vm.onIntent(StoragePermissionIntent.PermissionSystemResult(granted = false))
            advanceUntilIdle()
            assertTrue(prefs.onboarding.value)
            assertTrue(awaitItem() is StoragePermissionEffect.ShowMessage)
            assertEquals(StoragePermissionEffect.NavigateToHome, awaitItem())
        }
    }
}
