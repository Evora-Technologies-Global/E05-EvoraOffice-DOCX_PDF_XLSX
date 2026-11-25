# Android Native Architecture & MVI Standards

> **Standard Guide for Android Development with Jetpack Compose & MVI**
> This document defines the engineering standards, architecture patterns, and conventions for Android native projects using Kotlin, Jetpack Compose, Coroutines/Flow, and Clean Architecture.

---

## 0. Core Architecture Philosophy (The One-Paragraph Rule)

**State flows down, intents flow up, effects fire once.**
A screen composable has exactly one immutable `StateFlow<S>` and exactly one user action entry point: `onIntent(I)`. Anything that must happen *to* the UI rather than *be rendered by* it (navigation, snackbar, toast, scrolling to position, requesting runtime permissions) is a one-shot `Effect` delivered through a buffered `Channel`.

The ViewModel never touches Compose UI elements, Android Context, or platform SDKs; the screen composable never directly queries repositories or executes business logic.

```
┌──────────────┐         state: StateFlow<S>         ┌────────────────┐    Execute     ┌──────────────┐
│    Screen    │ <────────────────────────────────── │   ViewModel    │ ─────────────> │   UseCase    │
│ (Stateless)  │         onIntent(I)                 │ (MviViewModel) │ <───────────── │   (Domain)   │
└──────────────┘ ──────────────────────────────────> └────────────────┘   Result/Data  └──────────────┘
       ▲                     Flow<E>                        │
       └──────────────── (Buffered Channel) ────────────────┘
```

---

## 1. MVI Foundation Base Classes

Every MVI feature module must extend standard base contracts. Place this in `core/mvi/MviViewModel.kt`:

```kotlin
package core.mvi

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
 * Standard MVI ViewModel base class.
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
            throw cancellation // NEVER swallow CancellationException
        } catch (throwable: Throwable) {
            onError(throwable)
        }
    }
}
```

### Why `Channel`, not `SharedFlow`, for Effects

| Mechanism | Behavior When Screen Is In Background / Recreated |
|---|---|
| `MutableSharedFlow(replay = 0)` | **Drops** events. If navigation fires while the screen is stopped, it is lost forever. |
| `MutableSharedFlow(replay = 1)` | **Re-fires** on every configuration change or re-subscription. Requires `consumed` flags. |
| `Channel(BUFFERED).receiveAsFlow()` | **Buffers safely** and delivers exactly once when a collector resumes. ✅ |

### The `launchSafely` Mandate
`viewModelScope` uses a `SupervisorJob` but has **no default CoroutineExceptionHandler**. An unhandled exception inside a raw `viewModelScope.launch` crashes the Android app process.
- **Always rethrow `CancellationException`** to allow coroutine cancellation to propagate.
- **Ensure `onError` resets all raised flags** (e.g., `isLoading = false`). A stranded busy flag becomes a permanent deadlock against user retry.

---

## 2. Feature Contract (`XContract.kt`)

Every screen feature MUST have an `XContract.kt` containing **only** State, Intent, and Effect definitions.

```kotlin
package feature.profile

import core.mvi.UiEffect
import core.mvi.UiIntent
import core.mvi.UiState

data class ProfileState(
    val userId: String = "",
    val username: String = "",
    val avatarUrl: String? = null,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) : UiState {
    /** Derived property: true when user interaction should be disabled. */
    val isBusy: Boolean get() = isLoading || isSaving
}

sealed interface ProfileIntent : UiIntent {
    data class UpdateUsername(val newName: String) : ProfileIntent
    data object SaveProfileClicked : ProfileIntent
    data object RefreshRequested : ProfileIntent
    data object ScreenStarted : ProfileIntent
}

sealed interface ProfileEffect : UiEffect {
    data class NavigateToSettings(val userId: String) : ProfileEffect
    data class ShowSnackbar(val message: String) : ProfileEffect
}
```

### State Rules
1. **Immutable Data Class**: All properties must be `val`, with sensible default values representing the initial idle state.
2. **Derive, Do Not Duplicate**: Never store two fields that can contradict each other (e.g., do not store `isBusy: Boolean` alongside `isLoading` and `isSaving`; use a computed `val`).
3. **Store IDs, Not Full Objects for Selection**:
   ```kotlin
   // ✅ Resilient to background updates
   val selectedItemId: String? = null
   val selectedItem: Item? get() = items.find { it.id == selectedItemId }

   // ❌ Stale data when list updates in background
   val selectedItem: Item? = null
   ```
4. **Compose Stability**: All types exposed on `UiState` must be `@Immutable` or `@Stable`. List types should be wrapped or configured in `compose-stability.conf`.

### Intent Rules
1. **Intent Names Describe Action, Not State Mutation**:
   - `SaveButtonClicked`, NOT `SetSaving(true)`
   - `ShutterPressed`, NOT `StartCameraCapture`
2. **Exhaustive Handling**: Intents are sealed interfaces; reducers must use exhaustive `when` without an `else` branch so new actions trigger compile-time checks.

---

## 3. ViewModel Implementation Rules

```kotlin
class ProfileViewModel(
    private val getUserProfile: GetUserProfileUseCase,
    private val saveUserProfile: SaveUserProfileUseCase,
    observeUserProfile: ObserveUserProfileUseCase, // Inject and observe in init
) : MviViewModel<ProfileState, ProfileIntent, ProfileEffect>(ProfileState()) {

    private var saveJob: Job? = null

    init {
        observeUserProfile()
            .onEach { user ->
                setState { copy(username = user.name, avatarUrl = user.avatar) }
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.UpdateUsername     -> setState { copy(username = intent.newName) }
            is ProfileIntent.SaveProfileClicked -> handleSave()
            is ProfileIntent.RefreshRequested   -> refreshProfile()
            is ProfileIntent.ScreenStarted      -> Unit
        }
    }

    private fun handleSave() {
        if (currentState.isBusy) return // Guard re-entrancy

        saveJob?.cancel() // Cancel and replace in-flight work
        saveJob = launchSafely(
            onError = { err -> setState { copy(isSaving = false, errorMessage = err.message) } }
        ) {
            setState { copy(isSaving = true, errorMessage = null) }
            saveUserProfile(currentState.username)
            setState { copy(isSaving = false) }
            sendEffect(ProfileEffect.ShowSnackbar("Profile saved successfully"))
        }
    }

    private fun refreshProfile() {
        launchSafely(onError = { setState { copy(isLoading = false, errorMessage = it.message) } }) {
            setState { copy(isLoading = true, errorMessage = null) }
            getUserProfile()
            setState { copy(isLoading = false) }
        }
    }
}
```

### Hard Rules for ViewModels
1. **`onIntent` is the ONLY public action method.** Never expose `fun save()`, `fun update()`, or mutable properties.
2. **Never Import `androidx.compose.*` in ViewModel.** The ViewModel is pure presentation logic and must remain UI-agnostic and unit-testable on JVM without Robolectric.
3. **Database / Single Source of Truth**: After database writes, do not manually patch state if an observed Flow/Repository will emit the updated dataset automatically.
4. **Cancel & Replace, Don't Queue**: Replace in-flight requests when the user requests a new action rather than stacking stale tasks.

---

## 4. UI Layer: Route & Screen Split

Every screen feature is partitioned into two composables:
1. `XRoute`: Public, stateful, resolves dependencies from DI, observes ViewModel state, and collects effects.
2. `XScreen`: Internal/private, pure, stateless. Accepts `state: XState` and `onIntent: (XIntent) -> Unit`. Ideal for Compose previews.

```kotlin
// feature/profile/ProfileRoute.kt
@Composable
fun ProfileRoute(
    onNavigateSettings: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(), // Or hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is ProfileEffect.NavigateToSettings -> onNavigateSettings(effect.userId)
            is ProfileEffect.ShowSnackbar -> scope.launch {
                snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    ProfileScreen(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
private fun ProfileScreen(
    state: ProfileState,
    onIntent: (ProfileIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        // Render UI purely from state
    }
}
```

### The Reusable Effect Collector (`CollectEffects.kt`)

Place this in `core/mvi/CollectEffects.kt`:

```kotlin
package core.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Collects one-shot [UiEffect] flows safely during the Lifecycle STARTED state.
 * Uses rememberUpdatedState to ensure latest lambdas are invoked without restarting the collector.
 */
@Composable
fun <E : UiEffect> CollectEffects(effects: Flow<E>, onEffect: suspend (E) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val handler by rememberUpdatedState(onEffect)
    LaunchedEffect(effects, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            effects.collect { handler(it) }
        }
    }
}
```

---

## 5. Dependency Injection Support

### Koin Pattern
```kotlin
val profileModule = module {
    viewModel { ProfileViewModel(get(), get(), get()) }
}
```

### Hilt / Dagger Pattern
```kotlin
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserProfile: GetUserProfileUseCase,
    private val saveUserProfile: SaveUserProfileUseCase,
    observeUserProfile: ObserveUserProfileUseCase,
) : MviViewModel<ProfileState, ProfileIntent, ProfileEffect>(ProfileState()) { … }
```

---

## 6. Testing an MVI ViewModel

Use **Turbine** and Kotlin Coroutines Test framework.

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @Test
    fun `save action emits success effect and resets busy state`() = runTest {
        val saveUseCase = FakeSaveProfileUseCase()
        val viewModel = ProfileViewModel(
            getUserProfile = FakeGetUserProfileUseCase(),
            saveUserProfile = saveUseCase,
            observeUserProfile = FakeObserveUserProfileUseCase(),
        )

        viewModel.effects.test {
            viewModel.onIntent(ProfileIntent.SaveProfileClicked)

            val effect = awaitItem()
            assertTrue(effect is ProfileEffect.ShowSnackbar)
            expectNoEvents()
        }

        assertFalse(viewModel.state.value.isSaving)
    }
}
```

---

## 7. Anti-Patterns & Verification Checklist

### ❌ Anti-Patterns
1. **Multiple StateFlows in ViewModel**: Storing `val isLoading = MutableStateFlow(false)` alongside `state`. Always unify into `UiState`.
2. **ViewModel importing Android Context / Compose**: Breaks JVM testing and leaks references.
3. **Direct Database / Repo in Composable**: Bypasses ViewModel; composable must only interact via `onIntent`.
4. **Ignoring CancellationException**: Swallowing cancellation in `catch (e: Exception)` breaks coroutine lifecycle.
5. **SharedFlow(replay = 0) for navigation**: Events are lost if emitted while app is paused or in background.

### ✅ Pre-PR Checklist
- [ ] Screen ViewModel extends `MviViewModel<S, I, E>`.
- [ ] `XContract.kt` contains only immutable State, sealed Intent, and sealed Effect.
- [ ] `onIntent` is the sole public function of the ViewModel.
- [ ] No `androidx.compose.*` or `android.content.Context` imports inside ViewModel.
- [ ] Route/Screen separation strictly enforced (`XRoute` takes VM, `XScreen` takes `state` + `onIntent`).
- [ ] Effects collected via `CollectEffects` with `repeatOnLifecycle(STARTED)`.
- [ ] Coroutine cancellation exceptions rethrown in all try/catch blocks.

