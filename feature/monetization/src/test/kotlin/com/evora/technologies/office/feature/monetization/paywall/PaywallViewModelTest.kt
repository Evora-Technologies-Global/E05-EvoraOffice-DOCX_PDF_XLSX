package com.evora.technologies.office.feature.monetization.paywall

import app.cash.turbine.test
import com.evora.technologies.office.core.domain.port.AdPlacementHook
import com.evora.technologies.office.core.domain.port.AnalyticsPort
import com.evora.technologies.office.core.domain.port.ProEntitlementPort
import com.evora.technologies.office.core.domain.port.RemoteConfigPort
import com.evora.technologies.office.core.model.BillingOffer
import com.evora.technologies.office.core.model.ProFeature
import com.evora.technologies.office.core.model.ProStatus
import com.evora.technologies.office.core.model.PurchaseResult
import com.evora.technologies.office.core.model.RemoteConfigSnapshot
import com.evora.technologies.office.core.model.RewardedAdOutcome
import com.evora.technologies.office.core.model.SubscriptionPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakePaywallEntitlementPort : ProEntitlementPort {
    private val _status = MutableStateFlow(ProStatus())
    override val proStatus: StateFlow<ProStatus> = _status

    val offersFlow = MutableStateFlow(BillingOffer.defaults())
    var nextPurchaseResult: PurchaseResult = PurchaseResult.Success(SubscriptionPlan.YEARLY.skuId)
    var restoreReturnValue: Boolean = false
    var restoreError: Throwable? = null
    var refreshCalled = false

    override suspend fun hasAccess(feature: ProFeature): Boolean = _status.value.isUnlocked

    override suspend fun grantRewardedPass(hours: Int) {
        _status.value = _status.value.copy(hasRewardedPass = true, rewardPassExpiresAtEpochMs = 9999999999999L)
    }

    override suspend fun restorePurchases(): Boolean {
        restoreError?.let { throw it }
        if (restoreReturnValue) {
            _status.value = _status.value.copy(isPro = true, activeSku = SubscriptionPlan.LIFETIME.skuId)
        }
        return restoreReturnValue
    }

    override fun observeBillingOffers(): Flow<List<BillingOffer>> = offersFlow

    override suspend fun refreshCatalog() {
        refreshCalled = true
    }

    override suspend fun purchasePlan(plan: SubscriptionPlan): PurchaseResult {
        val result = nextPurchaseResult
        if (result is PurchaseResult.Success) {
            _status.value = _status.value.copy(isPro = true, activeSku = result.skuId)
        }
        return result
    }
}

private class FakePaywallRemoteConfig(
    defaultSku: String = SubscriptionPlan.YEARLY.skuId,
) : RemoteConfigPort {
    override val config: StateFlow<RemoteConfigSnapshot> = MutableStateFlow(
        RemoteConfigSnapshot(paywallDefaultSku = defaultSku),
    )

    override suspend fun fetchAndActivate(timeoutMs: Long): RemoteConfigSnapshot = config.value
}

private class FakePaywallAdHook : AdPlacementHook {
    var nextRewardedOutcome: RewardedAdOutcome = RewardedAdOutcome.Earned(24)

    override fun onMilestoneCompleted(screenId: String) = Unit
    override fun onHomeHubEntered() = Unit
    override suspend fun showRewardedPassAd(): RewardedAdOutcome = nextRewardedOutcome
}

private class FakePaywallAnalytics : AnalyticsPort {
    val paywallImpressions = mutableListOf<String>()
    val purchaseSuccesses = mutableListOf<String>()

    override fun logAdImpression(
        adPlatform: String,
        adFormat: String,
        adUnitName: String,
        currency: String,
        value: Double,
        valueMicros: Long,
    ) = Unit

    override fun logOnboardingStep(stepName: String, action: String) = Unit
    override fun logStoragePermissionResult(grantType: String) = Unit
    override fun logDocumentOpen(format: String, source: String) = Unit
    override fun logEditorSessionEnd(format: String, editDurationSec: Long, saveStatus: String) = Unit
    override fun logPdfToolComplete(toolType: String, durationMs: Long, success: Boolean) = Unit
    override fun logScanOcrComplete(mode: String, pageCount: Int, script: String) = Unit
    override fun logPaywallImpression(triggerSource: String) {
        paywallImpressions.add(triggerSource)
    }

    override fun logPurchaseSuccess(skuId: String, priceUsdMicros: Long) {
        purchaseSuccesses.add(skuId)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PaywallViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        port: FakePaywallEntitlementPort = FakePaywallEntitlementPort(),
        remoteConfig: FakePaywallRemoteConfig = FakePaywallRemoteConfig(),
        adHook: FakePaywallAdHook = FakePaywallAdHook(),
        analytics: FakePaywallAnalytics = FakePaywallAnalytics(),
    ): PaywallViewModel = PaywallViewModel(
        proEntitlementPort = port,
        remoteConfigPort = remoteConfig,
        adPlacementHook = adHook,
        analyticsPort = analytics,
    )

    @Test
    fun `skip emits ClosePaywall effect immediately`() = runTest {
        val vm = createViewModel()
        vm.effects.test {
            vm.onIntent(PaywallIntent.SkipClicked)
            assertEquals(PaywallEffect.ClosePaywall, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `remote config default sku selects initial paywall plan`() = runTest {
        val vm = createViewModel(
            remoteConfig = FakePaywallRemoteConfig(defaultSku = SubscriptionPlan.WEEKLY.skuId),
        )
        assertEquals(SubscriptionPlan.WEEKLY, vm.state.value.selectedPlan)
    }

    @Test
    fun `selecting plan and completing Play Billing purchase activates Pro and closes paywall`() = runTest {
        val port = FakePaywallEntitlementPort().apply {
            nextPurchaseResult = PurchaseResult.Success(SubscriptionPlan.LIFETIME.skuId)
        }
        val analytics = FakePaywallAnalytics()
        val vm = createViewModel(port = port, analytics = analytics)

        vm.onIntent(PaywallIntent.RefreshOffers)
        vm.onIntent(PaywallIntent.SelectPlan(SubscriptionPlan.LIFETIME))
        assertEquals(SubscriptionPlan.LIFETIME, vm.state.value.selectedPlan)

        vm.effects.test {
            vm.onIntent(PaywallIntent.StartTrialClicked)
            advanceUntilIdle()
            assertTrue(port.refreshCalled)
            assertEquals(listOf("paywall"), analytics.paywallImpressions)
            assertEquals(listOf(SubscriptionPlan.LIFETIME.skuId), analytics.purchaseSuccesses)
            assertTrue(vm.state.value.proStatus.isPro)
            assertTrue(vm.state.value.proStatus.shouldHideAds)
            assertFalse(vm.state.value.isProcessing)
            assertTrue(awaitItem() is PaywallEffect.ShowMessage)
            assertEquals(PaywallEffect.ClosePaywall, awaitItem())
        }
    }

    @Test
    fun `cancelled and unavailable purchase outcomes clear processing flag cleanly`() = runTest {
        val port = FakePaywallEntitlementPort().apply {
            nextPurchaseResult = PurchaseResult.Cancelled
        }
        val vm = createViewModel(port = port)

        vm.onIntent(PaywallIntent.StartTrialClicked)
        advanceUntilIdle()
        assertFalse(vm.state.value.isProcessing)
        assertNull(vm.state.value.statusMessage)

        port.nextPurchaseResult = PurchaseResult.BillingUnavailable("Play Store unavailable")
        vm.effects.test {
            vm.onIntent(PaywallIntent.StartTrialClicked)
            advanceUntilIdle()
            assertFalse(vm.state.value.isProcessing)
            assertEquals("Play Store unavailable", vm.state.value.statusMessage)
            assertEquals(PaywallEffect.ShowMessage("Play Store unavailable"), awaitItem())
        }
    }

    @Test
    fun `restore purchases restores Pro entitlement and closes paywall when found`() = runTest {
        val port = FakePaywallEntitlementPort().apply {
            restoreReturnValue = true
        }
        val vm = createViewModel(port = port)

        vm.effects.test {
            vm.onIntent(PaywallIntent.RestorePurchasesClicked)
            advanceUntilIdle()
            assertTrue(vm.state.value.proStatus.isPro)
            assertFalse(vm.state.value.isProcessing)
            assertTrue(awaitItem() is PaywallEffect.ShowMessage)
            assertEquals(PaywallEffect.ClosePaywall, awaitItem())
        }
    }

    @Test
    fun `reward pass grants 24h access when ad earned and stays locked when dismissed`() = runTest {
        val port = FakePaywallEntitlementPort()
        val adHook = FakePaywallAdHook().apply {
            nextRewardedOutcome = RewardedAdOutcome.Dismissed
        }
        val vm = createViewModel(port = port, adHook = adHook)

        vm.effects.test {
            vm.onIntent(PaywallIntent.GrantRewardPassClicked)
            advanceUntilIdle()
            assertFalse(vm.state.value.proStatus.hasRewardedPass)
            assertFalse(vm.state.value.isProcessing)
            assertEquals(
                PaywallEffect.ShowMessage("Complete the rewarded video to unlock a 24h Pro pass"),
                awaitItem(),
            )
            expectNoEvents()
        }

        adHook.nextRewardedOutcome = RewardedAdOutcome.Earned(24)
        vm.effects.test {
            vm.onIntent(PaywallIntent.GrantRewardPassClicked)
            advanceUntilIdle()
            assertTrue(vm.state.value.proStatus.hasRewardedPass)
            assertTrue(awaitItem() is PaywallEffect.ShowMessage)
            assertEquals(PaywallEffect.ClosePaywall, awaitItem())
        }
    }
}
