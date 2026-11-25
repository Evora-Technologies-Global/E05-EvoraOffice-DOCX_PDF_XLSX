package com.evora.technologies.office.feature.monetization.paywall

import app.cash.turbine.test
import com.evora.technologies.office.core.domain.port.ProEntitlementPort
import com.evora.technologies.office.core.model.ProFeature
import com.evora.technologies.office.core.model.ProStatus
import com.evora.technologies.office.core.model.SubscriptionPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

private class FakePaywallEntitlementPort : ProEntitlementPort {
    private val _status = MutableStateFlow(ProStatus())
    override val proStatus: StateFlow<ProStatus> = _status

    override suspend fun hasAccess(feature: ProFeature): Boolean = _status.value.isUnlocked

    override suspend fun grantRewardedPass(hours: Int) {
        _status.value = _status.value.copy(hasRewardedPass = true, rewardPassExpiresAtEpochMs = 9999999999999L)
    }

    override suspend fun restorePurchases(): Boolean = false

    override suspend fun setQaProOverride(enabled: Boolean) {
        _status.value = _status.value.copy(isPro = enabled)
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

    @Test
    fun `skip emits ClosePaywall effect immediately`() = runTest {
        val vm = PaywallViewModel(FakePaywallEntitlementPort())
        vm.effects.test {
            vm.onIntent(PaywallIntent.SkipClicked)
            assertEquals(PaywallEffect.ClosePaywall, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `selecting plan and starting trial unlocks local entitlement preview`() = runTest {
        val port = FakePaywallEntitlementPort()
        val vm = PaywallViewModel(port)

        vm.onIntent(PaywallIntent.SelectPlan(SubscriptionPlan.LIFETIME))
        assertEquals(SubscriptionPlan.LIFETIME, vm.state.value.selectedPlan)

        vm.effects.test {
            vm.onIntent(PaywallIntent.StartTrialClicked)
            advanceUntilIdle()
            assertTrue(vm.state.value.proStatus.isPro)
            assertTrue(awaitItem() is PaywallEffect.ShowMessage)
            assertEquals(PaywallEffect.ClosePaywall, awaitItem())
        }
    }
}
