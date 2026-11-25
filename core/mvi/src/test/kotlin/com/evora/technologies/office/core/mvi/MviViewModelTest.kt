package com.evora.technologies.office.core.mvi

import app.cash.turbine.test
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private data class SampleState(
    val count: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
) : UiState

private sealed interface SampleIntent : UiIntent {
    data object Increment : SampleIntent
    data object TriggerError : SampleIntent
    data object StartLongWork : SampleIntent
    data object CancelLongWork : SampleIntent
}

private sealed interface SampleEffect : UiEffect {
    data class ShowMessage(val message: String) : SampleEffect
}

private class SampleViewModel : MviViewModel<SampleState, SampleIntent, SampleEffect>(SampleState()) {
    var longWorkStarted = false
    var cancelledObserved = false
    var errorCalledOnCancel = false
    private var longJob: kotlinx.coroutines.Job? = null

    override fun onIntent(intent: SampleIntent) {
        when (intent) {
            SampleIntent.Increment -> {
                setState { copy(count = count + 1) }
                sendEffect(SampleEffect.ShowMessage("count=${currentState.count}"))
            }

            SampleIntent.TriggerError -> {
                launchSafely(
                    onError = { err -> setState { copy(isLoading = false, error = err.message) } },
                ) {
                    setState { copy(isLoading = true, error = null) }
                    error("boom")
                }
            }

            SampleIntent.StartLongWork -> {
                longJob = launchSafely(
                    onError = { errorCalledOnCancel = true },
                ) {
                    longWorkStarted = true
                    try {
                        delay(10_000)
                    } catch (c: CancellationException) {
                        cancelledObserved = true
                        throw c
                    }
                }
            }

            SampleIntent.CancelLongWork -> {
                longJob?.cancel()
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MviViewModelTest {
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
    fun `onIntent updates state and buffers one-shot effect`() = runTest {
        val vm = SampleViewModel()

        vm.effects.test {
            vm.onIntent(SampleIntent.Increment)
            advanceUntilIdle()

            assertEquals(1, vm.state.value.count)
            assertEquals(SampleEffect.ShowMessage("count=1"), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `launchSafely catches non-cancellation errors and clears loading flag`() = runTest {
        val vm = SampleViewModel()

        vm.onIntent(SampleIntent.TriggerError)
        advanceUntilIdle()

        assertFalse(vm.state.value.isLoading)
        assertEquals("boom", vm.state.value.error)
    }

    @Test
    fun `launchSafely rethrows CancellationException without invoking onError`() = runTest {
        val vm = SampleViewModel()

        vm.onIntent(SampleIntent.StartLongWork)
        runCurrent()
        assertTrue(vm.longWorkStarted)

        vm.onIntent(SampleIntent.CancelLongWork)
        advanceUntilIdle()

        assertFalse(vm.errorCalledOnCancel)
        assertNull(vm.state.value.error)
        assertTrue(vm.cancelledObserved)
    }
}
