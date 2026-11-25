package com.evora.technologies.office.bootstrap

import app.cash.turbine.test
import com.evora.technologies.office.core.domain.port.OfficeEnginePort
import com.evora.technologies.office.core.domain.port.ProEntitlementPort
import com.evora.technologies.office.core.model.ProFeature
import com.evora.technologies.office.core.model.ProStatus
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
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

private class FakeProEntitlementPort : ProEntitlementPort {
    private val _status = MutableStateFlow(ProStatus())
    override val proStatus: StateFlow<ProStatus> = _status
    override suspend fun hasAccess(feature: ProFeature): Boolean = _status.value.isUnlocked
    override suspend fun grantRewardedPass(hours: Int) = Unit
    override suspend fun restorePurchases(): Boolean = false
}

private class FakeOfficeEnginePort(private val available: Boolean = false) : OfficeEnginePort {
    override fun isNativeEngineAvailable(): Boolean = available
}

@OptIn(ExperimentalCoroutinesApi::class)
class BootstrapViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `verify environment updates state and emits status effect`() = runTest {
        val viewModel = BootstrapViewModel(
            proEntitlementPort = FakeProEntitlementPort(),
            officeEnginePort = FakeOfficeEnginePort(available = false),
        )

        viewModel.effects.test {
            viewModel.onIntent(BootstrapIntent.VerifyEnvironmentClicked)
            advanceUntilIdle()

            assertFalse(viewModel.state.value.isChecking)
            assertEquals("Hilt KSP DI verified", viewModel.state.value.statusMessage)
            assertEquals(
                BootstrapEffect.ShowStatus("Hilt KSP DI verified"),
                awaitItem(),
            )
            expectNoEvents()
        }
    }
}
