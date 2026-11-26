package com.evora.technologies.office.feature.settings

import app.cash.turbine.test
import com.evora.technologies.office.core.domain.port.CloudDrivePort
import com.evora.technologies.office.core.domain.port.ConsentPort
import com.evora.technologies.office.core.domain.port.PreferencesRepository
import com.evora.technologies.office.core.domain.port.ProEntitlementPort
import com.evora.technologies.office.core.model.AppLanguage
import com.evora.technologies.office.core.model.AppThemeMode
import com.evora.technologies.office.core.model.CloudAccount
import com.evora.technologies.office.core.model.CloudBrowsePage
import com.evora.technologies.office.core.model.CloudItem
import com.evora.technologies.office.core.model.CloudProvider
import com.evora.technologies.office.core.model.CloudTransferProgress
import com.evora.technologies.office.core.model.ConsentState
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.ProFeature
import com.evora.technologies.office.core.model.ProStatus
import com.evora.technologies.office.core.model.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

private class FakeSettingsPrefs : PreferencesRepository {
    override val onboardingCompleted = MutableStateFlow(true)
    override val selectedLocaleTag = MutableStateFlow<String?>("en")
    override val themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    override val defaultSortOrder = MutableStateFlow(SortOrder.MODIFIED_DESC)
    override val autosaveIntervalSec = MutableStateFlow(15)
    override val defaultAppPromptDismissed = MutableStateFlow(false)
    override val externalOpenCount = MutableStateFlow(0)
    override suspend fun setOnboardingCompleted(completed: Boolean) = Unit
    override suspend fun setSelectedLocaleTag(tag: String) {
        selectedLocaleTag.value = tag
    }
    override suspend fun setThemeMode(mode: AppThemeMode) {
        themeMode.value = mode
    }
    override suspend fun setDefaultSortOrder(sortOrder: SortOrder) = Unit
    override suspend fun setAutosaveIntervalSec(seconds: Int) {
        autosaveIntervalSec.value = seconds
    }
    override suspend fun setDefaultAppPromptDismissed(dismissed: Boolean) = Unit
    override suspend fun incrementExternalOpenCount() = Unit
}

private class FakeSettingsPro : ProEntitlementPort {
    override val proStatus = MutableStateFlow(ProStatus())
    var restoreResult: Boolean = false
    var restoreException: Throwable? = null
    override suspend fun hasAccess(feature: ProFeature) = proStatus.value.isUnlocked
    override suspend fun grantRewardedPass(hours: Int) = Unit
    override suspend fun restorePurchases(): Boolean {
        restoreException?.let { throw it }
        if (restoreResult) {
            proStatus.value = ProStatus(isPro = true, activeSku = "e05_yearly_pro_3999")
        }
        return restoreResult
    }
}

private class FakeSettingsCloud : CloudDrivePort {
    val list = MutableStateFlow(listOf(CloudAccount("1", CloudProvider.GOOGLE_DRIVE, "a@b.c", "A", null, "alias")))
    override fun observeConnectedAccounts(): Flow<List<CloudAccount>> = list
    override fun observeTransferProgress(): Flow<CloudTransferProgress?> = MutableStateFlow(null)
    override suspend fun isProviderConfigured(provider: CloudProvider) = true
    override suspend fun connectGoogleDrive(accessToken: String, email: String, displayName: String, avatarUri: String?) = Result.failure<CloudAccount>(UnsupportedOperationException())
    override suspend fun connectOneDrive(accessToken: String, accountId: String, email: String, displayName: String) = Result.failure<CloudAccount>(UnsupportedOperationException())
    override suspend fun disconnectAccount(accountId: String) = Result.success(Unit)
    override suspend fun listFolder(account: CloudAccount, folderId: String, pageToken: String?, query: String) = Result.failure<CloudBrowsePage>(UnsupportedOperationException())
    override suspend fun downloadFile(account: CloudAccount, item: CloudItem) = Result.failure<DocumentItem>(UnsupportedOperationException())
    override suspend fun uploadLocalFile(account: CloudAccount, localUriString: String, targetFolderId: String) = Result.failure<CloudItem>(UnsupportedOperationException())
}

private class FakeSettingsConsent(privacyRequired: Boolean = false) : ConsentPort {
    var privacyFormShown = false
    override val consentState = MutableStateFlow(
        ConsentState(canRequestAds = true, privacyOptionsRequired = privacyRequired, isInitialized = true),
    )
    override suspend fun gatherConsentIfNeeded() = consentState.value
    override suspend fun showPrivacyOptionsForm(): Result<ConsentState> {
        privacyFormShown = true
        return Result.success(consentState.value)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
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
    fun `theme, language, autosave, and navigation intents emit expected state and effects`() = runTest {
        val prefs = FakeSettingsPrefs()
        val vm = SettingsViewModel(prefs, FakeSettingsPro(), FakeSettingsCloud(), FakeSettingsConsent())
        advanceUntilIdle()
        assertEquals(1, vm.state.value.connectedCloudCount)

        vm.onIntent(SettingsIntent.SelectThemeMode(AppThemeMode.DARK))
        advanceUntilIdle()
        assertEquals(AppThemeMode.DARK, vm.state.value.themeMode)

        vm.effects.test {
            vm.onIntent(SettingsIntent.SelectLanguage(AppLanguage.VI))
            advanceUntilIdle()
            assertEquals(AppLanguage.VI, vm.state.value.selectedLanguage)
            assertEquals(SettingsEffect.ApplyLocale("vi"), awaitItem())

            vm.onIntent(SettingsIntent.SelectAutosaveInterval(30))
            advanceUntilIdle()
            assertEquals(30, vm.state.value.autosaveIntervalSec)
            assertTrue(awaitItem() is SettingsEffect.ShowMessage)

            vm.onIntent(SettingsIntent.ManageProClicked)
            assertEquals(SettingsEffect.NavigateToPaywall, awaitItem())

            vm.onIntent(SettingsIntent.ConnectedCloudsClicked)
            assertEquals(SettingsEffect.NavigateToCloudDrives, awaitItem())
        }
        assertEquals(AppLanguage.ID, AppLanguage.fromTag("in"))
        assertEquals(AppLanguage.ID, AppLanguage.fromTag("id"))
    }

    @Test
    fun `restore purchases updates pro status or resets busy flag on failure`() = runTest {
        val pro = FakeSettingsPro().apply { restoreResult = true }
        val consent = FakeSettingsConsent(privacyRequired = true)
        val vm = SettingsViewModel(FakeSettingsPrefs(), pro, FakeSettingsCloud(), consent)
        advanceUntilIdle()
        assertTrue(vm.state.value.umpPrivacyOptionsRequired)

        vm.onIntent(SettingsIntent.OpenUmpPrivacy)
        advanceUntilIdle()
        assertTrue(consent.privacyFormShown)

        vm.effects.test {
            vm.onIntent(SettingsIntent.RestorePurchasesClicked)
            advanceUntilIdle()
            assertTrue(vm.state.value.proStatus.isPro)
            assertTrue(!vm.state.value.isRestoringPurchases)
            assertEquals(SettingsEffect.ShowMessage("Evora Pro purchases restored"), awaitItem())

            pro.restoreException = IllegalStateException("Google Play unavailable")
            vm.onIntent(SettingsIntent.RestorePurchasesClicked)
            advanceUntilIdle()
            assertTrue(!vm.state.value.isRestoringPurchases)
            assertEquals(SettingsEffect.ShowMessage("Google Play unavailable"), awaitItem())
        }
    }
}
