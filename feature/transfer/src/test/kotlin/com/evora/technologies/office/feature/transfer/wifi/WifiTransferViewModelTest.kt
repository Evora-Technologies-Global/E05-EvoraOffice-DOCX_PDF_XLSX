package com.evora.technologies.office.feature.transfer.wifi

import app.cash.turbine.test
import com.evora.technologies.office.core.domain.port.WifiTransferPort
import com.evora.technologies.office.core.model.WifiTransferConfig
import com.evora.technologies.office.core.model.WifiTransferServerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeWifiTransferPort : WifiTransferPort {
    override val serverState = MutableStateFlow(WifiTransferServerState())

    override suspend fun startServer(config: WifiTransferConfig): Result<Unit> {
        serverState.value = serverState.value.copy(
            isRunning = true,
            ipAddress = "192.168.1.45",
            httpUrl = "http://192.168.1.45:${config.httpPort}",
            ftpUrl = if (config.ftpEnabled) "ftp://192.168.1.45:${config.ftpPort}" else "",
            config = config,
        )
        return Result.success(Unit)
    }

    override suspend fun stopServer(): Result<Unit> {
        serverState.value = serverState.value.copy(isRunning = false, httpUrl = "", ftpUrl = "")
        return Result.success(Unit)
    }

    override suspend fun updateConfig(config: WifiTransferConfig) {
        serverState.value = serverState.value.copy(config = config)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class WifiTransferViewModelTest {
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
    fun `start stop, read-only, pin lock, and copy URL emit expected state and effects`() = runTest {
        val port = FakeWifiTransferPort()
        val vm = WifiTransferViewModel(port)
        advanceUntilIdle()

        vm.onIntent(WifiTransferIntent.ToggleReadOnly(true))
        vm.onIntent(WifiTransferIntent.TogglePinLock(true))
        vm.onIntent(WifiTransferIntent.UpdatePinCode("5566"))
        advanceUntilIdle()
        assertTrue(vm.state.value.serverState.config.readOnly)
        assertEquals("5566", vm.state.value.serverState.config.pinCode)

        vm.effects.test {
            vm.onIntent(WifiTransferIntent.ToggleServerClicked)
            advanceUntilIdle()
            assertTrue(vm.state.value.serverState.isRunning)
            assertTrue(awaitItem() is WifiTransferEffect.ShowMessage)

            vm.onIntent(WifiTransferIntent.CopyUrlClicked("http://192.168.1.45:8998"))
            assertEquals(
                WifiTransferEffect.CopyToClipboard("http://192.168.1.45:8998", "Server URL"),
                awaitItem(),
            )

            vm.onIntent(WifiTransferIntent.ToggleServerClicked)
            advanceUntilIdle()
            assertFalse(vm.state.value.serverState.isRunning)
            assertTrue(awaitItem() is WifiTransferEffect.ShowMessage)

            // Clearing PIN while PIN lock is enabled keeps active >=4-digit PIN and refuses unauthenticated start
            vm.onIntent(WifiTransferIntent.UpdatePinCode(""))
            advanceUntilIdle()
            assertEquals("5566", vm.state.value.serverState.config.pinCode)
            vm.onIntent(WifiTransferIntent.ToggleServerClicked)
            advanceUntilIdle()
            assertFalse(vm.state.value.serverState.isRunning)
            assertTrue(awaitItem() is WifiTransferEffect.ShowMessage)
        }
    }
}
