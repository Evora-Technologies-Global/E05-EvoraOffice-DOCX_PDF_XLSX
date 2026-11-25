package com.evora.technologies.office.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything a screen renders, captured in a single immutable snapshot. */
interface UiState

/** An action initiated by the user or an unambiguous lifecycle event. */
interface UiIntent

/** A one-shot instruction to the UI: navigate, show snackbar, trigger haptics. */
interface UiEffect

/**
 * Standard MVI ViewModel base class for Evora Office.
 *
 * @param S Immutable UI State extending [UiState]
 * @param I User or UI Intent extending [UiIntent]
 * @param E One-shot Effect extending [UiEffect]
 */
abstract class MviViewModel<S : UiState, I : UiIntent, E : UiEffect>(
    initialState: S,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effects = Channel<E>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    protected val currentState: S get() = _state.value

    /** Single entry point for all UI events. */
    abstract fun onIntent(intent: I)

    /** Atomically update state using an immutable transform. */
    protected fun setState(reducer: S.() -> S) = _state.update { it.reducer() }

    /** Emit a one-shot UI effect buffered safely for collectors. */
    protected fun sendEffect(effect: E) {
        viewModelScope.launch { _effects.send(effect) }
    }

    /**
     * Executes asynchronous work safely within viewModelScope.
     * Prevents unhandled exceptions from terminating the process while strictly
     * preserving structured concurrency by rethrowing [CancellationException].
     */
    protected fun launchSafely(
        onError: (Throwable) -> Unit = {},
        block: suspend CoroutineScope.() -> Unit,
    ): Job = viewModelScope.launch {
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            onError(throwable)
        }
    }
}
