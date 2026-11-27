# LLM.md — E05 code structure

> **Read this file BEFORE writing any code. Update it AFTER any structural change.**
> This is the map of the codebase: where things live, what each layer may depend on, and
> where a new file belongs. For *how* to write an MVI screen, read
> [`.claude/rules/android-native-standards.md`](.claude/rules/android-native-standards.md).

**Status (27/11/2026): Sprints 1–4 & Phases 1–8 Complete (`F-01`–`F-09 = Done`; `T-001`–`T-027` done; `R-01` `14/14` `16 KB` ARM64 `.so` binaries from CI run `45768969780` + `org.libreoffice.kit` JNI bindings & asset unpacker integrated in `:engine:office`); Ad-Free Architecture Cleared (all AdMob, Mediation, UMP, Adjust, `AD_ID`, and Rewarded Ad gates removed per CEO directive while keeping Play Billing `8.0.0`, Firebase Analytics/Remote Config, and all Pro utilities unlocked freely for now), Baseline Profiles + R8 Hardening (`T-025`), Phase 7 QA (`T-026`, `08_QA_TEST_REPORT.md` Zero Blockers, CEO Gate 3 Approved `27/11/2026`), and Phase 8 Release Prep & ASO (`T-027`, `09_ASO_METADATA.md`, `10_RELEASE_CHECKLIST.md`, `fastlane/`, `scripts/package_mplv2_disclosure.sh`) Complete — Pending Production Store Credentials (`10_RELEASE_CHECKLIST.md` §2.2) for Play Console Upload.** Architecture and module boundaries are
defined in [`docs/07_TECH_SPEC.md`](docs/07_TECH_SPEC.md). Modules marked `[Active]` are compiled and verified; modules marked `[Planned — Sprint N]` activate in their respective sprint.
Code snippets in §4–§6 are copied strictly from real E05 code as modules land.

---

## 1. Rules of use

| When | Do |
|---|---|
| Before writing code | Read §2–§10 of this file, [`docs/07_TECH_SPEC.md`](docs/07_TECH_SPEC.md), and [`.claude/rules/android-native-standards.md`](.claude/rules/android-native-standards.md). |
| Before adding a file | Check §10 "Where does a new file go?". Do not invent an unlisted package or module. |
| After adding, moving, renaming, or deleting a package, module, layer, or architectural rule | Update this file in the same commit. |
| After fixing something in §11 | Move the row from "Open" to "Fixed" with the commit reference. |
| Never | Copy a pattern from a file listed in §11 as a known deviation, or import `android.*` / `androidx.compose.*` in ViewModels, Contracts, `:core:model`, or `:core:domain`. |

---

## 2. Module graph

Native Android application (**Kotlin + Jetpack Compose**, **MVI + Clean Architecture**, **Hilt + KSP**), built with **AGP `9.4.1` (Built-in Kotlin)** + **Gradle `9.6.0`** (`compileSdk = 37`, `targetSdk = 36`, `minSdk = 26`). No `org.jetbrains.kotlin.android` or `kapt` plugins are applied anywhere.

- **Build Logic:** `build-logic/convention` (`evora.kotlin.jvm`, `evora.android.library`, `evora.android.compose`, `evora.android.hilt`, `evora.android.feature`, `evora.android.application`) `[Active — T-003]`
- **App Shell:** `:app` (`com.evora.technologies.office`) `[Active — T-003]` -> depends on `:core:*`, active `:engine:*`, active `:feature:*`
- **Core Layer:**
  - `:core:model` (pure JVM, zero dependencies) `[Active — T-003]`
  - `:core:domain` (pure JVM, depends only on `:core:model` + `kotlinx-coroutines-core` + `javax.inject`; includes `ProEntitlementPort`, `RemoteConfigPort`, `AnalyticsPort` without ad ports/policies) `[Active — T-003]`
  - `:core:mvi` (Android Library + Compose runtime, depends on AndroidX Lifecycle) `[Active — T-003]`
  - `:core:designsystem` (Android Library + Compose Material 3 + shared components) `[Active — T-003/T-004]`
  - `:core:database` (Room `2.8.5` + KSP + Hilt) `[Active — T-003/T-005]`
  - `:core:datastore` (DataStore Preferences + Hilt) `[Active — T-003/T-005]`
  - `:core:data` (Hilt `@Binds` implementations of `:core:domain` ports + `WorkManager` `@HiltWorker`; depends on `:core:model`, `:core:domain`, `:core:database`, `:core:datastore`) `[Active — T-003/T-005]`
- **Engine Layer (isolated SDK/JNI wrappers implementing `:core:domain` ports):**
  - `:engine:pdf` (`io.legere:pdfiumandroid:2.0.3` + `PDFBox-Android 2.0.27.0` + `BouncyCastle 1.80`) `[Active — T-003/T-008/T-010..T-013]`
  - `:engine:office` (`LibreOfficeKit` `14` `16 KB` `.so` + `org.libreoffice.kit` JNI + `LibreOfficeNativeBridge` + `LibreOfficeAssetHelper` + `OoxmlOfficeEngine` + `DraftSnapshotSerializer`) `[Active — T-002/T-003/T-015..T-017]`
  - `:engine:hwp` (`kr.dogfoot:hwplib:1.1.11` + `hwpxlib:1.0.9` offline HTML sanitizer + `EPUB`/`TXT`/`CSV`/`MD` reader) `[Active — T-009]`
  - `:engine:archive` (`Apache Commons Compress 1.28.0` + `XZ 1.12` `.zip`/`.7z`) `[Active — T-009]`
  - `:engine:scanner` (`CameraX 1.7.0` + `ML Kit` Doc Scanner `16.0.0` / 5-Script OCR `16.0.1` / Barcode `17.3.0`) `[Active — Sprint 2 T-014]`
  - `:engine:transfer` (`Ktor CIO` Wi-Fi HTTP + `EmbeddedFtpServer` + `TransferPathSandbox` + `WifiTransferService` + `WifiTransferTileService`) `[Active — Sprint 3 T-019]`
  - `:engine:cloud` (`Google Drive REST v3` + `OneDrive MSAL / Graph v6` + `AndroidKeystoreTokenVault`) `[Active — Sprint 3 T-020]`
  - `:engine:monetization` (`Play Billing 8.0.0`, `SignedEntitlementCache`, `ActiveActivityTracker`, `FirebaseRemoteConfigManager` `firebase-bom 35.2.0`, `FirebaseTelemetry` product/billing events — zero AdMob/Mediation/UMP/Adjust SDKs) `[Active — T-023]`
- **Feature Layer (isolated; `:feature:*` NEVER depends on another `:feature:*`):**
  - `:feature:onboarding` (`SplashRoute`, `LanguageSelectRoute`, `OnboardingIntroRoute`, `StoragePermissionRoute`) `[Active — T-006]`
  - `:feature:library` (`HomeDocumentsRoute`, `RecentBookmarksRoute`, `SearchDocumentsRoute`, `StorageBrowserRoute`, `QuickBarGlanceWidget`, `RecentDocsGlanceWidget`, `EvoraTemplatesSheet`) `[Active — Sprints 1–3 T-007/T-015/T-018/T-021]`
  - `:feature:reader` (`DocumentReaderRoute`, `ArchiveInspectorRoute`) `[Active — T-008/T-009]`
  - `:feature:editor` (`PdfEditorRoute`, `SignaturePadRoute`, `OfficeEditorRoute`, `SlideShowRoute`) `[Active — Sprint 2 T-010/T-011/T-015/T-016/T-017]`
  - `:feature:tools` (`ToolsHubRoute`, `PdfToolConfigRoute`) `[Active — Sprint 2 T-012/T-013]`
  - `:feature:scanner` (`ScannerFlowRoute`) `[Active — Sprint 2 T-014]`
  - `:feature:cloud` (`CloudDriveBrowserRoute`) `[Active — Sprint 3 T-020]`
  - `:feature:transfer` (`WifiTransferRoute`) `[Active — Sprint 3 T-019]`
  - `:feature:monetization` (`PaywallRoute` wired to `PlayBillingEntitlementRepository` via `ProEntitlementPort`) `[Active — T-023]`
  - `:feature:settings` (`SettingsRoute`) `[Active — Sprint 3 T-022]`
- **Performance:** `:baselineprofile` (`BaselineProfileGenerator`, `StartupBenchmarks`, `HomeScrollBenchmarks`, `BenchmarkNavigationHelpers`) `[Active — Sprint 4 T-025]`

---

## 3. Package layout

Root namespace is locked at `com.evora.technologies.office` (`CD-05`, Launcher name `Evora Office`, Play Store title `Evora Office: DOCX, PDF, XLSX`).

| Module | Base Package | Notes |
|---|---|---|
| `:app` | `com.evora.technologies.office` | `EvoraApplication`, `MainActivity`, `navigation.AppNavHost` |
| `:core:model` | `com.evora.technologies.office.core.model` | Pure Kotlin immutable models & enums |
| `:core:domain` | `com.evora.technologies.office.core.domain` | `.port.*` interfaces, `.usecase.*`, `.common.AppResult` |
| `:core:mvi` | `com.evora.technologies.office.core.mvi` | `MviViewModel`, `UiState`, `UiIntent`, `UiEffect`, `CollectEffects` |
| `:core:designsystem` | `com.evora.technologies.office.core.designsystem` | `.theme.*`, `.component.*`, `.icon.*` |
| `:core:database` | `com.evora.technologies.office.core.database` | `.entity.*`, `.dao.*`, `.di.DatabaseModule`, `EvoraOfficeDatabase` |
| `:core:datastore` | `com.evora.technologies.office.core.datastore` | `EvoraPreferencesDataSource`, `.di.DataStoreModule` |
| `:core:data` | `com.evora.technologies.office.core.data` | `.repository.*`, `.worker.*`, `.di.DataModule`, `.di.DispatchersModule` |
| `:engine:<name>` | `com.evora.technologies.office.engine.<name>` | Engine adapters, `.di.<Name>EngineModule` |
| `:feature:<name>` | `com.evora.technologies.office.feature.<name>` | Subpackages per screen (`<screen>/XContract.kt`, `XViewModel.kt`, `XRoute.kt`, `XScreen.kt`) |

---

## 4. The MVI core

Package `com.evora.technologies.office.core.mvi` inside `:core:mvi` implements
[`.claude/rules/android-native-standards.md`](.claude/rules/android-native-standards.md) §1 & §4
(`core/mvi/src/main/kotlin/com/evora/technologies/office/core/mvi/MviViewModel.kt` and `CollectEffects.kt`):

```kotlin
abstract class MviViewModel<S : UiState, I : UiIntent, E : UiEffect>(
    initialState: S,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effects = Channel<E>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    protected val currentState: S get() = _state.value

    abstract fun onIntent(intent: I)

    protected fun setState(reducer: S.() -> S) = _state.update { it.reducer() }

    protected fun sendEffect(effect: E) {
        viewModelScope.launch { _effects.send(effect) }
    }

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
```

---

## 5. Anatomy of a feature package

Every screen feature under `:feature:<name>/src/main/kotlin/.../feature/<name>/<screen>/` strictly contains:
1. `<Screen>Contract.kt` — Immutable `data class <Screen>State : UiState`, `sealed interface <Screen>Intent : UiIntent`, `sealed interface <Screen>Effect : UiEffect`.
2. `<Screen>ViewModel.kt` — `@HiltViewModel class <Screen>ViewModel @Inject constructor(...) : MviViewModel<...>` with `onIntent` as its **sole public method**, zero `android.*` / `androidx.compose.*` imports.
3. `<Screen>Route.kt` — Public composable resolving `hiltViewModel()`, collecting `state.collectAsStateWithLifecycle()`, wiring `CollectEffects`, passing `state` + `viewModel::onIntent` to `<Screen>Screen`.
4. `<Screen>Screen.kt` — `internal` stateless composable rendering `Loading`, `Empty`, `Error`, `Content` states + Compose `@Preview` functions.

---

## 6. Dependency injection

Locked by CEO (`CD-11`, `2026-11-25`): **Hilt with KSP (`com.google.dagger:hilt-android:2.60.1` + `com.google.devtools.ksp:2.3.14`)**.
- `:app` hosts `@HiltAndroidApp class EvoraApplication : Application(), Configuration.Provider` (injecting `HiltWorkerFactory`) and `@AndroidEntryPoint class MainActivity : ComponentActivity()`.
- Android modules using Hilt apply convention plugin `evora.android.hilt` (never `kapt`).
- `:core:domain` uses pure `javax.inject:javax.inject:1` (`@Inject` constructors) or constructor parameters wired in `:core:data` `@Module` bindings, keeping `:core:domain` 100% Android-free.
- Glance widgets (`W-01`/`W-02`) resolve domain ports via `@EntryPoint @InstallIn(SingletonComponent::class)` and `EntryPointAccessors.fromApplication(...)`.
- Billing & Telemetry Wiring: `:engine:monetization` (`MonetizationBindingsModule` + `MonetizationConfigModule`) binds `ProEntitlementPort` (`PlayBillingEntitlementRepository`), `RemoteConfigPort` (`FirebaseRemoteConfigManager`), and `AnalyticsPort` (`FirebaseTelemetry`) with conditional `google-services 4.6.0` in `:app`. All ad SDKs/hooks are removed; utilities run freely without Rewarded Ad gates.

---

## 7. Navigation

- Single-activity Compose Navigation (`androidx.navigation:navigation-compose`) hosted in `:app` (`AppNavHost`).
- Feature modules emit navigation strictly as one-shot `UiEffect` events (`sendEffect(XEffect.NavigateTo...)`) and receive lambda callbacks in `XRoute` — never boolean navigation flags in `UiState` and never cross-feature Gradle dependencies.
- External `ACTION_VIEW` URIs resolve via magic-byte sniffing and jump straight to Reader/Editor without pre-launch ads.

---

## 8. Compose stability

- Shared stability rules live at root `compose-stability.conf`, wired via `composeCompiler { stabilityConfigurationFiles.add(...) }` inside `evora.android.compose`.
- Domain models in `:core:model` are immutable `data class` / `enum class` types listed in `compose-stability.conf` (so `:core:model` stays free of Compose runtime imports while remaining strong-skippable).
- UI-only wrapper models in `:core:designsystem` or `:feature:*` use `@Immutable` where collections are exposed.

---

## 9. Testing layout

- **MVI / ViewModel / Domain unit tests (JVM):** `src/test/kotlin/...` in each module using `junit4`, `kotlinx-coroutines-test`, and **`app.cash.turbine:turbine`** with readable fakes (zero Robolectric in ViewModels).
- **Room / DataStore instrumentation or JVM tests:** `:core:database` and `:core:datastore` under `src/test/` or `src/androidTest/`.
- **Binary 16 KB Alignment Gate:** `python3 -I scripts/verify_16kb_elf.py <apk|aab|dir>` verifies `ELF64 PT_LOAD p_align == 0x4000` on every bundled `.so`.

---

## 10. Where does a new file go?

| File Purpose | Exact Module & Path |
|---|---|
| Gradle Version Catalog | `gradle/libs.versions.toml` |
| Gradle Convention Plugins | `build-logic/convention/src/main/kotlin/...` |
| Application, Manifest `<activity-alias>`, `AppNavHost`, ProGuard | `app/src/main/kotlin/com/evora/technologies/office/...` |
| Pure Kotlin Domain Models (`DocFormat`, `DocumentItem`, `ProStatus`, etc.) | `core/model/src/main/kotlin/com/evora/technologies/office/core/model/...` |
| Pure Kotlin Ports & UseCases (`ProEntitlementPort`, `DocumentRepository`, etc.) | `core/domain/src/main/kotlin/com/evora/technologies/office/core/domain/...` |
| MVI Base (`MviViewModel`, `UiState`, `UiIntent`, `UiEffect`, `CollectEffects`) | `core/mvi/src/main/kotlin/com/evora/technologies/office/core/mvi/...` |
| Material 3 Theme (`Style B`), Format Colors, Fonts, Shared Components (`EvoraTemplatesSheet`, `AndroidPrintHelper`) | `core/designsystem/src/main/kotlin/com/evora/technologies/office/core/designsystem/...` |
| Room Entities, DAOs, `EvoraOfficeDatabase`, `DatabaseModule` | `core/database/src/main/kotlin/com/evora/technologies/office/core/database/...` |
| DataStore Preferences, Local Entitlement Cache, `DataStoreModule` | `core/datastore/src/main/kotlin/com/evora/technologies/office/core/datastore/...` |
| Repository Implementations, `MediaStore` Indexer, `TrashPurgeWorker`, `StorageBrowserRepositoryImpl`, `SignatureRepositoryImpl`, `DraftRecoveryRepositoryImpl`, `TemplateRepositoryImpl`, `assets/templates/` | `core/data/src/main/kotlin/.../core/data/...` & `core/data/src/main/assets/templates/` |
| PDF Engine (`PdfiumAndroid`, `PDFBox-Android`, `BouncyCastle`, `PdfBoxPowerEngine`) | `engine/pdf/src/main/kotlin/com/evora/technologies/office/engine/pdf/...` |
| Office Engine (`LibreOfficeNativeBridge`, `LibreOfficeAssetHelper`, `org.libreoffice.kit.*`, `OoxmlOfficeEngine`, `DraftSnapshotSerializer`) | `engine/office/src/main/kotlin/.../engine/office/`, `engine/office/src/main/java/org/libreoffice/kit/`, `engine/office/src/main/jniLibs/arm64-v8a/` *(gitignored >100MB; fetched via `scripts/fetch_lok_prebuilt.sh`)*, `engine/office/mplv2-disclosure/` |
| Hangul Engine (`hwplib`, `hwpxlib`, Sanitized HTML WebView Bridge) | `engine/hwp/src/main/kotlin/com/evora/technologies/office/engine/hwp/...` |
| Archive Engine (`Commons Compress` `.zip`/`.7z`) | `engine/archive/src/main/kotlin/com/evora/technologies/office/engine/archive/...` |
| Scanner Engine (`CameraX`, `ML Kit` Doc/OCR/QR, `MlKitScannerEngine`, Dual ID Card) | `engine/scanner/src/main/kotlin/com/evora/technologies/office/engine/scanner/...` |
| Wi-Fi Transfer Engine (`Ktor CIO` HTTP, `EmbeddedFtpServer`, `TransferPathSandbox`, `WifiTransferService`, `TileService`) | `engine/transfer/src/main/kotlin/com/evora/technologies/office/engine/transfer/...` & `engine/transfer/src/main/assets/wifi_web/` |
| Cloud Engine (`GoogleDriveCloudClient`, `OneDriveCloudClient`, `AndroidKeystoreTokenVault`) | `engine/cloud/src/main/kotlin/com/evora/technologies/office/engine/cloud/...` & `app/src/main/res/raw/msal_config.json` |
| Monetization Engine (`PlayBillingClientGateway`, `PlayBillingEntitlementRepository`, `SignedEntitlementCache`, `ActiveActivityTracker`, `FirebaseRemoteConfigManager`, `FirebaseTelemetry`, `MonetizationBindingsModule`) | `engine/monetization/src/main/kotlin/com/evora/technologies/office/engine/monetization/...` & `engine/monetization/src/main/AndroidManifest.xml` *(ad-free billing & telemetry)* |
| Glance Widgets & App Shortcuts (`QuickBarGlanceWidget`, `RecentDocsGlanceWidget`) | `feature/library/src/main/kotlin/.../widget/` & `app/src/main/res/xml/shortcuts.xml` |
| Feature UI Screens (`XContract`, `XViewModel`, `XRoute`, `XScreen`) | `feature/<domain>/src/main/kotlin/com/evora/technologies/office/feature/<domain>/<screen>/...` |
| Baseline Profiles & Macrobenchmarks | `baselineprofile/src/main/kotlin/com/evora/technologies/office/baselineprofile/...` & `app/src/release/generated/baselineProfiles/` (`baseline-prof.txt`, `startup-prof.txt`) |
| CI Workflows, Release & ELF/MPLv2 Scripts | `.github/workflows/*.yml`, `scripts/verify_16kb_elf.py`, `scripts/fetch_lok_prebuilt.sh`, `scripts/package_mplv2_disclosure.sh` |
| Play Store Fastlane & Localized Metadata | `fastlane/Appfile`, `fastlane/Fastfile`, `fastlane/Gemfile`, `fastlane/metadata/android/<locale>/` |

---

## 11. Known deviations from the standard

### Open

- **BouncyCastle Version Resolution:** `pdfbox-android:2.0.27.0` transitively pulls `bc*-jdk15to18:1.72`; `:engine:pdf` and `:app` exclude `-jdk15to18` in favor of `bcprov-jdk18on:1.80` + `bcpkix-jdk18on:1.80`.

### Fixed

- **`T-002` / `T-015` `R-01` `LibreOfficeKit` `16 KB` `.so` Placement & JNI Bindings:** Built on GitHub Actions (`45768969780`), verified `14/14` `PT_LOAD p_align == 0x4000`, wired to `org.libreoffice.kit` JNI classes + `LibreOfficeNativeBridge` (`lokDispatcher`) + `LibreOfficeEngineAdapter` + `OoxmlOfficeEngine` + `PdfBoxPowerEngine`.
- **`T-015` / `T-026` LOK Runtime UNO Assets, Bootstrap Fix & CI Sync:** Integrated `lok-android-runtime-assets` from CI run `45792780069`, updated `scripts/fetch_lok_prebuilt.sh` (`sync_local_runtime_assets`) and `.github/workflows/lok-android-16kb.yml` (`fundamentalrc` 7 extension/cache keys), patched `LibreOfficeKit.java` (`init`) to set default extension/cache env vars under `file://$dataDir/user/...` and gate `redirectStdio` on debuggable builds (fixing native `SIGSEGV` in `initializeNative`), and updated `LibreOfficeBridgeTest` to pass whether or not staged runtime assets are present on disk.

---

## 12. What is already right — keep it

- **Gradle Build Logic & Catalog (`T-003`):** `gradle/libs.versions.toml`, `compose-stability.conf`, and `build-logic/convention` cleanly compile AGP `9.4.1` built-in Kotlin + Compose + Hilt `2.60.1` (KSP `2.3.14`) + Room `2.8.5` without `kapt` or `kotlin-android`.
- **Clean Architecture & MVI Foundation (`T-003`–`T-007`):** `:core:model` and `:core:domain` (`MagicByteSniffer`, `FileNameValidator`, `PageRangeParser`, `ExcelFormulaEvaluator`, `QrPayloadParser`, `OoxmlDocxWriter`, `BoundedZipReader`) are pure JVM modules; `:core:mvi` enforces `MviViewModel` + `Channel(BUFFERED)` + `CollectEffects` + `CancellationException` rethrow verified in `MviViewModelTest`; `:core:designsystem` bundles `Poppins` + `JetBrains Mono` (`tnum`) and Style B tokens/components (`EvoraRewardGateSheet`, `EvoraTemplatesSheet`, `AndroidPrintHelper`); `:core:database` exports all 5 Room entities (`EvoraOfficeDatabaseTest`); `:core:data` runs `TrashPurgeWorker`, `MediaStore` scanner, `SignatureRepositoryImpl`, `DraftRecoveryRepositoryImpl`, `TemplateRepositoryImpl` (`8` real OOXML templates); `:app` hosts `AppNavHost`, 10 localized `strings.xml`, `FileProvider`, and granular `<activity-alias>` targets.
- **Universal Readers & Archives (`T-008`–`T-009`):** `:engine:pdf` (`PdfiumAndroid`), `:engine:hwp` (`hwplib`/`hwpxlib` + XSS sanitizer + `EPUB`/`TXT`/`CSV`), and `:engine:archive` (`Commons Compress` + `XZ` with Zip-Slip checks & `.rar` guard) drive `DocumentReaderRoute` and `ArchiveInspectorRoute`.
- **Sprint 2 PDF Power, Scanner & Office Editors (`T-010`–`T-017`):** `:engine:pdf` (`PdfBoxPowerEngine` annotations, signatures, PKCS#7, AES-128/256, merge, split, compress, watermark, offline `PDF->DOCX` w/ ML Kit OCR fallback), `:engine:scanner` (`CameraX`, ML Kit Doc Scanner, 5-script OCR, Dual-Side ID Card A4, QR), `:engine:office` (`OoxmlOfficeEngine` + `LibreOfficeNativeBridge`), and `:feature:tools`, `:feature:scanner`, `:feature:editor` (`PdfEditorRoute`, `SignaturePadRoute`, `OfficeEditorRoute`, `SlideShowRoute`) compile, pass unit tests + Lint, and enforce zero `SAVE_HEADER` locks.
- **Sprint 3 Storage, Wi-Fi Transfer, Cloud, Widgets & Core Gate (`T-018`–`T-022`):** `StorageBrowserRepositoryImpl` + `StorageBrowserRoute` (`Internal`/`Downloads`/`SD Card`/`SAF` trees, batch Move/Copy/Trash/Share/Merge PDF, tablet split), `:engine:transfer` + `WifiTransferRoute` (`Ktor CIO` offline drag-and-drop HTTP UI + `EmbeddedFtpServer` + `TransferPathSandbox` + PIN/Read-Only + `10m` idle stop + `TileService`), `:engine:cloud` + `CloudDriveBrowserRoute` (Google Drive REST v3 + Microsoft OneDrive MSAL/Graph v6 + `AndroidKeystoreTokenVault`), `QuickBarGlanceWidget` / `RecentDocsGlanceWidget` + `shortcuts.xml` + `deferredDeepLink` cold-start routing, `SettingsRoute` + `10`-locale `AppLanguage`/`RTL` polish, and **Core Feature Code Review Gate (`F-01`–`F-09 = Done`, zero Blocker, zero AdMob/UMP/Billing calls per `CD-07`)**.
- **Sprint 4 Play Billing `8.0.0` (`T-023`):** `:engine:monetization` wires `BillingClient 8.0.0` (`PendingPurchasesParams`, `enableAutoServiceReconnection()`, `queryProductDetails`, `queryPurchasesAsync`, `acknowledgePurchase`) via `PlayBillingClientGateway` + `ActiveActivityTracker`, persists entitlements offline in `SignedEntitlementCache` (Android Keystore `HmacSHA256`), enforces debug-only `qaProOverride`, and drives `PaywallRoute` and `SettingsRoute -> Restore Purchases`.
- **Ad-Free Cleanup (27/11/2026 CEO Directive):** Removed all ad SDKs, mediation adapters, UMP consent, Adjust SDK, `AD_ID`/`ACCESS_ADSERVICES_*` permissions (plus `google_analytics_adid_collection_enabled=false`), `AdCooldownPolicy`, `AdPlacementHook`, `ConsentPort`, `AdSlots`, `EvoraRewardGateSheet`, and `ad_impression` / `ads_*` telemetry/config keys. All previously gated utilities (`Merge > 3`, `High Compress`, `Watermark`, `PKCS#7`, `Dual ID Card`, `PDF -> DOCX`) are unlocked freely; Firebase Analytics + Remote Config remain cleanly wired for non-ad product events/flags (`79/79` unit tests across `27` suites, `0` lint errors, `22/22` `16 KB` `.so` in `app-release.aab` `119.18 MiB` / `104.95 MiB` Play payload).
- **16 KB Native Gate (`T-002`/`T-003`/`T-014`/`T-023`/`T-025`):** `scripts/verify_16kb_elf.py` verifies `22/22` `arm64-v8a` `.so` binaries in `app-release.aab` pass `0x4000` alignment across `LibreOfficeKit` (`14` `.so`), `PdfiumAndroid`, `CameraX`, `ML Kit` OCR/Barcode, `DataStore`, and `androidx.graphics.path`.
- **Sprint 4 Baseline Profiles, Macrobenchmark & R8 Hardening (`T-025`):** Generated `app/src/release/generated/baselineProfiles/baseline-prof.txt` and `startup-prof.txt` via `emulator-5554` (`api 36`) and stripped obsolete ad/Adjust class rules; moved `TrashPurgeWorker.schedulePeriodic` to `EvoraApplication` `@IoDispatcher` and ran `RemoteConfig` async in `MainActivity` + immediate `onboardingCompleted` check in `SplashViewModel` with `ReportDrawn`/`ReportDrawnWhen` milestones. Physical **`SM-A165F`** (`Android 16` / `SDK 36`) Macrobenchmarks (`:baselineprofile:connectedBenchmarkReleaseAndroidTest`): `StartupBenchmarks.startupBaselineProfile` achieved **`910.70 ms`** median `timeToFullDisplayMs` (`860.91–1,005.75 ms`, **`-21.8%`** vs `1,164.38 ms` `startupNoCompilation`, strictly `< 1.2s`) and **`587.13 ms`** median `timeToInitialDisplayMs`; `HomeScrollBenchmarks.scrollHomeAndToolsBaselineProfile` achieved `frameDurationCpuMs` **`P50 = 7.04 ms`**, `P90 = 16.73 ms`, and `frameOverrunMs` `P50 = -0.42 ms`. R8 minification (`isMinifyEnabled = true`, `isShrinkResources = true`, `-assumenosideeffects` on `Log.v/d/i`) and `allowBackup="false"` verified on `app-release.aab` (`119.18 MiB` total / `104.95 MiB` Play payload excl. `BUNDLE-METADATA`, `4` DEX files, `assets/dexopt/baseline.prof` embedded).
- **Phase 7 QA, Physical Device & OWASP Verification (`T-026`):** Produced [`docs/08_QA_TEST_REPORT.md`](docs/08_QA_TEST_REPORT.md) (**ZERO Blockers (`P0 = 0`)**; **CEO Gate 3 Approved `27/11/2026`**) covering MediaStore `MEDIA_TYPE_DOCUMENT (6)`, LOK `fundamentalrc` + `12s` timeout, ZIP/XML fast-path, `.csv` MIME + adaptive launcher icon, OWASP network/PIN/HMAC/WebView hardening, and `2.0x` font scaling.
- **Phase 8 Store Release & ASO Kit (`T-027`):** Produced [`docs/09_ASO_METADATA.md`](docs/09_ASO_METADATA.md) and [`docs/10_RELEASE_CHECKLIST.md`](docs/10_RELEASE_CHECKLIST.md), generated character-validated 10-locale Play Store listings (`fastlane/metadata/android/*`) updated for ad-free launch, configured Android Fastlane lanes (`fastlane/Fastfile`, `Appfile`, `Gemfile`), verified `22/22` `16 KB` `.so` in `app-release.aab` (`119.18 MiB` / `104.95 MiB` Play payload, `assets/dexopt/baseline.prof`, test upload keystore `.local/evora-upload-test.jks` signed), audited merged release manifest permissions & SDK Data Safety matrix + `MANAGE_EXTERNAL_STORAGE` compliance screencast spec, documented the `5`-item production credential swap gate (`docs/10_RELEASE_CHECKLIST.md` §2.2), and built `scripts/package_mplv2_disclosure.sh` emitting `.local/release_kit/evora-office-v1.0.0-mplv2-lok-disclosure.tar.gz`.

---

## 13. Constraints already fixed (inputs to Phase 6+)

| Constraint | Value | Source |
|---|---|---|
| Target platform | Native Android (Kotlin + Jetpack Compose) | [`CLAUDE.md`](CLAUDE.md) (CEO, 25/11/2026) |
| Architecture | MVI + Clean Architecture | [`CLAUDE.md`](CLAUDE.md); [`.claude/rules/android-native-standards.md`](.claude/rules/android-native-standards.md) |
| Documentation & lifecycle | 8-phase Company Bot lifecycle (`docs/01_*.md` to `docs/10_*.md`); CEO communication, new docs and rewritten sections in Vietnamese (technical terms in English); docs written before 27/11/2026 stay in English; code, commits, `CLAUDE.md` and `LLM.md` in English | [`CLAUDE.md`](CLAUDE.md) (CEO, 27/11/2026); [`.claude/rules/product-lifecycle-workflow.md`](.claude/rules/product-lifecycle-workflow.md) |
| Untrusted / temp competitor data | `.local/` only, never committed; release builds must never ship competitor assets | [`CLAUDE.md`](CLAUDE.md) "Untrusted and local-only data" |
| SDK levels, versioning & monetization | `minSdk 26`, `targetSdk 36` (Android 16), `16 KB` ELF alignment; `versionName "1.0.0"`, `versionCode 100`; Track A Open-Source (`$0` royalty, `100%` offline `$0` server); Play Billing `8.0.0` Pro paywall (all ad SDKs & Rewarded Ad gates cleared until ads are scheduled for rollout); Tier B UA (`$1k–$5k/mo`) | [`02_PROJECT_CHARTER.md`](docs/02_PROJECT_CHARTER.md) (`CD-01`–`CD-07`, 25/11/2026) |
| Brand, package, style & account scope | App name `Evora Office` (`Evora Office: DOCX, PDF, XLSX`), `applicationId = com.evora.technologies.office`, **Style B (Familiar Vibrant Utility)**, App logo deferred (`CD-06`), Interactive HTML Gate 2 wireframes (`CD-08`), Personal Play account 14d/12-tester gate ignored (`CD-09`) | [`02_PROJECT_CHARTER.md`](docs/02_PROJECT_CHARTER.md) (`CD-05`–`CD-09`, 25/11/2026); [`05_DESIGN_SYSTEM.md`](docs/05_DESIGN_SYSTEM.md) (`CD-10`) |
| DI Framework & GitHub CI (`CD-11`, `CD-12`) | **Hilt with KSP (`2.60.1` + `KSP 2.3.14`)**, zero `kapt`; use existing GitHub repo `Evora-Technologies-Global/E05-EvoraOffice-DOCX_PDF_XLSX` for Android CI and `R-01` Linux LOK `16 KB` builds | CEO Decision (`CD-11`, `CD-12`, 25/11/2026); [`07_TECH_SPEC.md`](docs/07_TECH_SPEC.md) |
