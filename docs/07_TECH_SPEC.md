# Technical Architecture Specification — E05 (`Evora Office`)

**Project Code:** E05  
**Product Brand & Store Title:** `Evora Office` (`Evora Office: DOCX, PDF, XLSX` — `CD-05`)  
**Package Name (`applicationId`):** `com.evora.technologies.office`  
**Release Target:** `versionName = "1.0.0"`, `versionCode = 100` (`minSdk = 26`, `targetSdk = 36`, `compileSdk = 37`)  
**Repository:** `Evora-Technologies-Global/E05-EvoraOffice-DOCX_PDF_XLSX` (`CD-12`)  
**Owner:** Backend / Mobile Systems Architect (`backend-architect`)  
**Date:** 2026-11-25  
**Status:** **LOCKED (`T-001` Complete -> Unlocks `T-002` CI & `T-003` Gradle Multi-Module Scaffold)**  
**Upstream Inputs:**  
- [`01_COMPETITOR_REVERSE_ENGINEERING.md`](01_COMPETITOR_REVERSE_ENGINEERING.md)  
- [`02_PROJECT_CHARTER.md`](02_PROJECT_CHARTER.md) (`PD-01`–`PD-02`, `CD-01`–`CD-09`)  
- [`03_PRD.md`](03_PRD.md) (`F-01`–`F-10`, 21 routes, 5 Room tables, Remote Config, Ktor LAN API)  
- [`04_USER_STORIES.md`](04_USER_STORIES.md) (`US-001`–`US-112`)  
- [`05_DESIGN_SYSTEM.md`](05_DESIGN_SYSTEM.md) (`CD-10` Gate 2, `S-01`–`S-20`, `W-01`–`W-02`)  
- [`06_SPRINT_BACKLOG.md`](06_SPRINT_BACKLOG.md) (`T-001`–`T-027`)  
- [`.gemini/rules/android-native-standards.md`](../.gemini/rules/android-native-standards.md) · [`LLM.md`](../LLM.md)

---

## 1. Phase 6 Architectural Decision Log (`CD-11`–`CD-12`)

This specification inherits `PD-01`–`PD-02` and `CD-01`–`CD-10`, and locks the remaining Phase 6 infrastructure choices directed by the CEO:

| Date | Decision ID | Architectural Choice | Owner | Rationale & Scope Impact |
|---|---|---|---|---|
| 2026-11-25 | `CD-11` | **Dependency Injection: Hilt with KSP (`2.60.1` + `KSP 2.3.14`)** | CEO | Replaces legacy `kapt` completely (required for AGP 9 built-in Kotlin and faster incremental builds). All `@HiltViewModel`, `@AndroidEntryPoint`, `@HiltWorker`, `@Module`, and `@EntryPoint` bindings compile strictly via `ksp(libs.hilt.compiler)`. |
| 2026-11-25 | `CD-12` | **CI/CD & `R-01` Build Repo: Existing Project GitHub Repository** | CEO | Supersedes creating an extra private CI repo in `CD-08`. CI/CD workflows (`.github/workflows/android-ci.yml`) and the Linux `LibreOfficeKit 16 KB` cross-compile workflow (`.github/workflows/lok-android-16kb.yml`) live inside `Evora-Technologies-Global/E05-EvoraOffice-DOCX_PDF_XLSX`. Public `MPLv2` LOK build-recipe/patch disclosure tarball is emitted as a release artifact (`10_RELEASE_CHECKLIST.md`) so proprietary Kotlin/Compose code remains closed-source. |

---

## 2. Build Toolchain & Verified Dependency Matrix

Because the project runs on Android Studio `2026.2` (`Gradle 9.6.0`, `AGP 9.4.1`) with **AGP Built-in Kotlin**, `org.jetbrains.kotlin.android` and `org.jetbrains.kotlin.kapt` are **never applied** on Android modules (`com.android.application` / `com.android.library`). Pure JVM modules apply `org.jetbrains.kotlin.jvm`.

| Component | Artifact / Tool | Locked Version | Purpose & Compatibility Notes |
|---|---|---|---|
| **Gradle / JDK** | Gradle Wrapper / JVM Toolchain | `9.6.0` / Java `17` | Toolchain `17` bytecode compatibility across local macOS (`JDK 24`/`17`) and Ubuntu CI runners |
| **AGP** | `com.android.tools.build:gradle` | `9.4.1` | Built-in Kotlin enabled; `minSdk 26`, `targetSdk 36`, `compileSdk 37` (required by AndroidX `2026-09+` & `PdfiumAndroid 2.0.3`) |
| **Kotlin / Compose** | `kotlin` / `compose-compiler` / `Compose BOM` | `2.3.21` / `2026.09.00` | Material 3 (`dynamicColor = false`), `WindowSizeClass`, `compose-stability.conf` |
| **DI (Hilt + KSP)** | `com.google.dagger:hilt-android` + `hilt-compiler` | `2.60.1` + `KSP 2.3.14` | AGP 9 compatible (`>= 2.59`); paired with `androidx.hilt:*:1.4.0` (`hilt-work`, `hilt-navigation-compose`) |
| **Persistence** | `androidx.room:*` + `androidx.datastore:datastore-preferences` | `2.8.5` + `1.3.0` | Room schema export via `ksp(libs.room.compiler)`; WorkManager `2.12.0` |
| **PDF View & Tools** | `io.legere:pdfiumandroid` + `com.tom-roush:pdfbox-android` + `bcprov-jdk18on` | `2.0.3` + `2.0.27.0` + `1.80` | `PdfiumAndroid 2.0.3` ships `16 KB`-aligned `.so`; `PDFBox` + BouncyCastle handle annotations, AES, PKCS#7, Split/Merge/Compress/Watermark |
| **Office Engine (`R-01`)** | `LibreOfficeKit / Collabora Android Core` (`MPLv2/LGPLv3`) | CI Artifact (`arm64-v8a` `16 KB`) | Cross-compiled on Ubuntu Linux CI with NDK `28.2.13676358` + `-Wl,-z,max-page-size=16384` |
| **HWP & Archives** | `kr.dogfoot:hwplib` + `hwpxlib` + `commons-compress` + `xz` | `1.1.11` + `1.0.9` + `1.28.0` + `1.10` | 100% offline HWP/HWPX AST -> sanitized local HTML; pure-Java `.zip` / `.7z` (no `.rar`) |
| **Scan, OCR, QR** | `CameraX` + `play-services-mlkit-document-scanner` + `mlkit:text-recognition*` + `barcode-scanning` | `16.0.0` / `16.0.1` / `17.3.0` | On-device 5-script OCR (`Latin`, `Chinese`, `Devanagari`, `Japanese`, `Korean`), Dual ID Card `A4` compositor |
| **Transfer & Cloud** | `io.ktor:ktor-server-cio` + `Google Drive REST v3` + `MSAL` + `Graph SDK` | `3.6.1` / Official SDKs | Embedded HTTP/FTP LAN server on port `8998`; Keystore-wrapped OAuth aliases |
| **Widgets & Perf** | `androidx.glance:glance-appwidget` + `androidx.profileinstaller` | `1.2.0` + `1.4.1` | `4x1` Quick Bar & `4x2` Recent Docs widgets; Baseline Profile cold start `< 1.2s` |
| **Testing & Lint** | `junit4`, `kotlinx-coroutines-test`, `app.cash.turbine:turbine`, Android Lint | `4.13.2` / `1.11.0` / `1.2.1` | Strict JVM ViewModel/Domain tests with readable fakes; `verify_16kb_elf.py` binary gate |
| **Monetization (`CD-07`)** | `billing-ktx:8.0.0`, `UMP CMP`, `AdMob Mediation` (`Sprint 4` only) | Deferred until `F-01`–`F-09` Done | Sprints 1–3 bind only `ProEntitlementPort` & `AdPlacementHook` stubs in `:core:data` |

---

## 3. Gradle Multi-Module Graph & Clean Architecture Boundaries

E05 uses a disciplined multi-module modular monolith backed by `build-logic/` Gradle convention plugins.

### 3.1 Convention Plugins (`build-logic/convention`)
- `evora.kotlin.jvm`: Pure Kotlin JVM (`Java 17`, coroutines test support, **zero** Android SDK dependencies).
- `evora.android.library`: Android Library (`compileSdk 36`, `minSdk 26`, JVM `17`, consumer ProGuard).
- `evora.android.compose`: Applies `org.jetbrains.kotlin.plugin.compose`, Compose BOM, stability config (`compose-stability.conf`), and Material 3 dependencies.
- `evora.android.hilt`: Applies `com.google.devtools.ksp` + `com.google.dagger.hilt.android`, adds `hilt-android` + `ksp(hilt-compiler)`.
- `evora.android.feature`: Composes `evora.android.library` + `evora.android.compose` + `evora.android.hilt` and attaches `:core:model`, `:core:domain`, `:core:mvi`, `:core:designsystem`, plus `hilt-navigation-compose`, `lifecycle-runtime-compose`, and `turbine` test dependencies.
- `evora.android.application`: Configures `:app` (`com.evora.technologies.office`, `targetSdk 36`, `versionCode 100`, `versionName "1.0.0"`, ABI splits / packaging, signing hooks, and R8 release rules).

### 3.2 Target Module Graph & Sprint Activation Schedule

```text
                                     ┌───────┐
                                     │ :app  │  (MainActivity, NavHost, Manifest Aliases, R8)
                                     └───┬───┘
            ┌────────────────────────────┼────────────────────────────┐
            ▼                            ▼                            ▼
     ┌──────────────┐             ┌──────────────┐             ┌──────────────┐
     │ :feature:*   │             │ :core:data   │             │ :engine:*    │
     │ (10 modules) │             │ (Hilt @Binds)│             │ (8 modules)  │
     └──────┬───────┘             └──────┬───────┘             └──────┬───────┘
            │                            │                            │
   ┌────────┼────────┐          ┌────────┼────────┐                   │
   ▼        ▼        ▼          ▼        ▼        ▼                   │
:core:   :core:   :core:     :core:   :core:   (calls :engine:*) ◄────┘
 mvi     design   domain    database datastore
            │        │          │        │
            └────────┴────┬─────┴────────┘
                          ▼
                    :core:model  (Pure Kotlin JVM — zero android.* imports)
```

| Layer | Module | Package Root | Activation | Responsibility |
|---|---|---|---|---|
| **App** | `:app` | `com.evora.technologies.office` | **Sprint 1 (`T-003`)** | `@HiltAndroidApp`, `MainActivity`, `AppNavHost`, `<activity-alias>` intent dispatchers, `FileProvider` |
| **Core** | `:core:model` | `com.evora.technologies.office.core.model` | **Sprint 1 (`T-003`)** | Immutable domain models (`DocFormat`, `DocumentItem`, `ProStatus`, `ReaderTheme`, `DraftCheckpoint`, `ArchiveEntry`) |
| **Core** | `:core:domain` | `com.evora.technologies.office.core.domain` | **Sprint 1 (`T-003`)** | Repository/Engine interfaces (`DocumentRepository`, `ProEntitlementPort`, `AdPlacementHook`, `OfficeEnginePort`, `PdfEnginePort`) & pure use cases |
| **Core** | `:core:mvi` | `com.evora.technologies.office.core.mvi` | **Sprint 1 (`T-003`)** | `UiState`, `UiIntent`, `UiEffect`, `MviViewModel<S, I, E>`, `launchSafely`, `CollectEffects` |
| **Core** | `:core:designsystem` | `com.evora.technologies.office.core.designsystem` | **Sprint 1 (`T-003`/`T-004`)** | `EvoraTheme`, `EvoraFormatColors`, `Poppins`/`JetBrains Mono`, `EvoraBrandMark`, `DocCard`, `MinTouchTargetBox` |
| **Core** | `:core:database` | `com.evora.technologies.office.core.database` | **Sprint 1 (`T-003`/`T-005`)** | Room `EvoraOfficeDatabase` (`5` entities, DAOs, migrations, `DatabaseModule`) |
| **Core** | `:core:datastore` | `com.evora.technologies.office.core.datastore` | **Sprint 1 (`T-003`/`T-005`)** | DataStore Preferences (locale, theme, onboarding flags, autosave interval, cached signed entitlement) |
| **Core** | `:core:data` | `com.evora.technologies.office.core.data` | **Sprint 1 (`T-003`/`T-005`)** | Repository implementations, `MediaStore` indexer, magic-byte MIME resolver, `TrashPurgeWorker`, `CD-07` `ProEntitlementPort`/`AdPlacementHook` stubs |
| **Engine** | `:engine:pdf` | `com.evora.technologies.office.engine.pdf` | **Sprint 1 (`T-003`)** | `PdfiumAndroid 2.0.3` page/tile renderer + `PDFBox-Android` + `BouncyCastle` + `PrintManager` |
| **Engine** | `:engine:office` | `com.evora.technologies.office.engine.office` | **Sprint 1 (`T-003`) / Sprint 2 (`T-015`)** | `LibreOfficeKit` `16 KB` `.so` wrapper, JNI bridge, tile buffer, IME & OOXML/ODF serializer, `PDF -> DOCX` |
| **Engine** | `:engine:hwp` | `com.evora.technologies.office.engine.hwp` | **Sprint 1 (`T-009`)** | `hwplib`/`hwpxlib` offline parser + sanitized local HTML/CSS/SVG + sandboxed WebView host |
| **Engine** | `:engine:archive` | `com.evora.technologies.office.engine.archive` | **Sprint 1 (`T-009`)** | `Commons Compress` + `XZ` pure-Java `.zip`/`.7z` inspector & extractor |
| **Engine** | `:engine:scanner` | `com.evora.technologies.office.engine.scanner` | **Sprint 2 (`T-014`)** | `CameraX` + `ML Kit` Doc Scanner, Dual ID Card A4 compositor, 5-script OCR, QR/Barcode |
| **Engine** | `:engine:transfer` | `com.evora.technologies.office.engine.transfer` | **Sprint 3 (`T-019`)** | `Ktor CIO` HTTP/FTP foreground service, path validator, idle timer, `TileService` |
| **Engine** | `:engine:cloud` | `com.evora.technologies.office.engine.cloud` | **Sprint 3 (`T-020`)** | `Google Drive v3` + `OneDrive MSAL` + Keystore token alias manager |
| **Engine** | `:engine:monetization` | `com.evora.technologies.office.engine.monetization` | **Sprint 4 (`T-023`/`T-024`)** | `Play Billing 8.0.0`, `Google UMP CMP`, `Firebase Remote Config/Analytics`, `Adjust`, `AdMob Mediation` *(strictly deferred per `CD-07`)* |
| **Feature** | `:feature:onboarding` | `com.evora.technologies.office.feature.onboarding` | **Sprint 1 (`T-006`)** | `SplashRoute` (`S-01`), `LanguageSelectRoute` (`S-02`), `OnboardingIntroRoute` (`S-03`), `StoragePermissionRoute` (`S-05`) |
| **Feature** | `:feature:library` | `com.evora.technologies.office.feature.library` | **Sprint 1 (`T-007`) / Sprint 3 (`T-018`/`T-021`)** | `HomeDocumentsRoute` (`S-06`), `RecentBookmarksRoute` (`S-07`), `SearchDocumentsRoute` (`S-08`), `TemplatesSheet` (`S-09`), `StorageBrowserRoute` (`S-10`), `GlanceWidgets` (`W-01`/`W-02`) |
| **Feature** | `:feature:reader` | `com.evora.technologies.office.feature.reader` | **Sprint 1 (`T-008`/`T-009`)** | `DocumentReaderRoute` (`S-13`) + Comfort Modes + TTS + Password modal |
| **Feature** | `:feature:editor` | `com.evora.technologies.office.feature.editor` | **Sprint 2 (`T-010`/`T-011`/`T-016`/`T-017`)** | `OfficeEditorRoute` (`S-14a/b/c`), `SlideShowRoute` (`S-15`), `PdfEditorRoute` & `SignaturePadRoute` (`S-16`) |
| **Feature** | `:feature:tools` | `com.evora.technologies.office.feature.tools` | **Sprint 2 (`T-012`/`T-013`)** | `ToolsHubRoute` (`S-12`), `PdfToolConfigRoute` & `ConvertProgressRoute` (`S-17`) |
| **Feature** | `:feature:scanner` | `com.evora.technologies.office.feature.scanner` | **Sprint 2 (`T-014`)** | `ScannerFlowRoute` (`S-18`) |
| **Feature** | `:feature:cloud` | `com.evora.technologies.office.feature.cloud` | **Sprint 3 (`T-020`)** | `CloudDriveBrowserRoute` (`S-11`) |
| **Feature** | `:feature:transfer` | `com.evora.technologies.office.feature.transfer` | **Sprint 3 (`T-019`)** | `WifiTransferRoute` (`S-19`) |
| **Feature** | `:feature:monetization` | `com.evora.technologies.office.feature.monetization` | **Sprint 1 (`T-006` shell) / Sprint 4 (`T-023`)** | `PaywallRoute` & `RewardGateSheet` (`S-04`) |
| **Feature** | `:feature:settings` | `com.evora.technologies.office.feature.settings` | **Sprint 3 (`T-022`)** | `SettingsRoute` (`S-20`) |
| **Perf** | `:baselineprofile` | `com.evora.technologies.office.baselineprofile` | **Sprint 4 (`T-025`)** | Macrobenchmark & startup/scroll Baseline Profile generator |

### 3.3 Hard Dependency Direction Rules
1. `:feature:*` modules **NEVER** depend on another `:feature:*` module (navigation passes typed routes/URIs back to `:app` `AppNavHost`) and **NEVER** depend directly on `:core:database`, `:core:datastore`, or `:engine:*` implementations — they consume `:core:domain` ports only.
2. `:core:model` and `:core:domain` **NEVER** depend on Android SDK, Jetpack Compose, Room, or Hilt Gradle plugin.
3. `:core:mvi` **NEVER** imports feature models or navigation frameworks; it provides only the generic `MviViewModel`, `UiState`, `UiIntent`, `UiEffect`, and `CollectEffects`.

---

## 4. Hilt (KSP) Dependency Injection Architecture (`CD-11`)

### 4.1 Core Hilt Patterns
- **Application Root (`:app`):** `EvoraApplication : Application(), Configuration.Provider` annotated with `@HiltAndroidApp`, injecting `HiltWorkerFactory` for WorkManager.
- **Activity & Entry Points:** Single-activity `MainActivity` annotated with `@AndroidEntryPoint`. Foreground services (`WifiTransferService`) use `@AndroidEntryPoint`. Android Home Glance Widgets (`QuickBarGlanceWidget`, `RecentDocsGlanceWidget`) resolve dependencies via a `@EntryPoint @InstallIn(SingletonComponent::class)` interface accessed from `applicationContext` using `EntryPointAccessors.fromApplication(...)`.
- **ViewModels:** Every screen ViewModel extends `MviViewModel<S, I, E>`, carries `@HiltViewModel` and `@Inject constructor(...)`, and is resolved strictly inside `XRoute` via `hiltViewModel()`.
- **Domain Purity vs. Hilt:** Use cases or stateless domain services in `:core:domain` may use `javax.inject:javax.inject:1` (`@Inject` constructor) OR be exposed via `@Provides` in `:core:data` without applying the Android/Hilt Gradle plugin to `:core:domain`.
- **Qualifiers (`:core:domain` / `:core:data`):** `@IoDispatcher`, `@DefaultDispatcher`, `@MainDispatcher`, `@ApplicationScope`.

### 4.2 `CD-07` Feature-First Monetization Binding Switch
To enforce `CD-07` (*zero live AdMob calls until `F-01`–`F-09` reach `Done`*):
- `:core:domain` defines `ProEntitlementPort` (`val proStatus: StateFlow<ProStatus>`, `suspend fun hasAccess(feature: ProFeature): Boolean`, `suspend fun grantRewardedPass(hours: Int)`) and `AdPlacementHook` (`fun onMilestoneComplete(screenId: String)`, `fun onHomeEntered()`).
- In Sprints 1–3, `:core:data`'s `MonetizationStubModule` binds `LocalStubProEntitlementPort` (backed by encrypted `DataStore` for QA toggle testing) and `NoOpAdPlacementHook`.
- In Sprint 4 (`T-023`/`T-024`), `:engine:monetization` replaces the binding with `PlayBillingProEntitlementPort` and `MediatedAdPlacementHook` in a single `@TestInstallIn` / module swap without editing any `:feature:*` ViewModel.

---

## 5. MVI Foundation & Concurrency Contract

All UI and presentation layers follow [`.gemini/rules/android-native-standards.md`](../.gemini/rules/android-native-standards.md):
- `XContract.kt`: Holds immutable `data class XState : UiState`, `sealed interface XIntent : UiIntent`, and `sealed interface XEffect : UiEffect` exclusively.
- `XViewModel.kt`: Extends `MviViewModel<XState, XIntent, XEffect>`, exposes **`onIntent(intent: XIntent)` as the sole public method**, runs every coroutine through `launchSafely(onError = { ... })` (rethrowing `CancellationException`, resetting busy flags in `onError`), and emits navigation/snackbars exclusively via `sendEffect(...)`.
- `XRoute.kt` / `XScreen.kt`: `XRoute` injects `hiltViewModel()`, collects `state.collectAsStateWithLifecycle()`, registers `CollectEffects(viewModel.effects) { ... }` (`repeatOnLifecycle(STARTED)` + `effects.collect`), and passes `state` + `viewModel::onIntent` down to stateless `internal fun XScreen(...)`.
- ViewModels and Contracts contain **zero** `androidx.compose.*` or `android.*` imports (enforced in code review and unit-tested directly on JVM).

---

## 6. Local Persistence: Room v1 DDL & DataStore (`T-005`)

### 6.1 `EvoraOfficeDatabase` (`version = 1`, `exportSchema = true`)
Fixes the competitor's path-only schema flaw (`01_COMPETITOR_REVERSE_ENGINEERING.md` §4.1) with full metadata, reading memory, soft trash expiration, atomic draft checkpoints, and Keystore token aliases.

```sql
CREATE TABLE IF NOT EXISTS `document_meta` (
    `uri_string` TEXT NOT NULL PRIMARY KEY,
    `display_name` TEXT NOT NULL,
    `format` TEXT NOT NULL,
    `size_bytes` INTEGER NOT NULL,
    `modified_at_epoch_ms` INTEGER NOT NULL,
    `last_opened_at_epoch_ms` INTEGER,
    `last_page_index` INTEGER NOT NULL DEFAULT 0,
    `last_total_pages` INTEGER NOT NULL DEFAULT 0,
    `last_scroll_offset_y` REAL NOT NULL DEFAULT 0.0,
    `last_zoom` REAL NOT NULL DEFAULT 1.0,
    `reader_theme` TEXT NOT NULL DEFAULT 'LIGHT',
    `is_continuous_scroll` INTEGER NOT NULL DEFAULT 1,
    `is_favorite` INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS `idx_doc_meta_last_opened` ON `document_meta` (`last_opened_at_epoch_ms` DESC);
CREATE INDEX IF NOT EXISTS `idx_doc_meta_favorite` ON `document_meta` (`is_favorite`, `modified_at_epoch_ms` DESC);

CREATE TABLE IF NOT EXISTS `trash_items` (
    `trash_id` TEXT NOT NULL PRIMARY KEY,
    `original_uri_string` TEXT NOT NULL,
    `cached_file_path` TEXT NOT NULL,
    `display_name` TEXT NOT NULL,
    `format` TEXT NOT NULL,
    `size_bytes` INTEGER NOT NULL,
    `trashed_at_epoch_ms` INTEGER NOT NULL,
    `expires_at_epoch_ms` INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS `idx_trash_expires_at` ON `trash_items` (`expires_at_epoch_ms` ASC);

CREATE TABLE IF NOT EXISTS `draft_recovery` (
    `target_uri_string` TEXT NOT NULL PRIMARY KEY,
    `draft_cache_path` TEXT NOT NULL,
    `display_name` TEXT NOT NULL,
    `updated_at_epoch_ms` INTEGER NOT NULL,
    `has_pending_recovery` INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS `signatures` (
    `id` TEXT NOT NULL PRIMARY KEY,
    `asset_path` TEXT NOT NULL,
    `created_at_epoch_ms` INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS `cloud_accounts` (
    `account_id` TEXT NOT NULL PRIMARY KEY,
    `provider` TEXT NOT NULL,
    `email` TEXT NOT NULL,
    `display_name` TEXT NOT NULL,
    `avatar_uri` TEXT,
    `encrypted_token_alias` TEXT NOT NULL
);
```

- **Auto-Purge Worker:** `TrashPurgeWorker` (`@HiltWorker`) runs periodically via `WorkManager` (`ExistingPeriodicWorkPolicy.KEEP`, 24h cadence) deleting `trash_items` and cached files where `expires_at_epoch_ms <= now`.
- **Preferences DataStore (`evora_prefs.preferences_pb`):** Stores `onboarding_completed: Boolean`, `selected_locale_tag: String`, `theme_mode: String` (`SYSTEM`/`LIGHT`/`DARK`), `default_sort_order: String`, `autosave_interval_sec: Int` (`15`), `reward_pass_expires_at_ms: Long`, `qa_pro_override: Boolean` (debug builds only), and HMAC-verified local entitlement cache payload.

---

## 7. Native Engines & External Boundary Specifications

### 7.1 `LibreOfficeKit` (`:engine:office`, `R-01` Critical Path)
- **Packaging:** Native `.so` files compiled in GitHub Actions Linux CI (`.github/workflows/lok-android-16kb.yml`) targeting `arm64-v8a` (`libsofficeapp.so` / `liblo-native-code.so` + minimal ICU/UNOIDL assets) drop into `engine/office/src/main/jniLibs/arm64-v8a/` (or prebuilt Maven/CI artifact).
- **16 KB Compliance:** Linked with `-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384` using Android NDK r28 (`28.2.13676358`), verified via `llvm-readelf -l` and `scripts/verify_16kb_elf.py` (`p_align == 0x4000` on every `PT_LOAD`).
- **JNI Threading & Crash Isolation:** Dedicated single-thread LOK looper dispatcher (`LOKDispatcher`) owns native `LibreOfficeKit` document handles so concurrent Compose renders never race native C++ memory. Tile bitmaps render into pooled `HardwareBuffer`/`Bitmap` buffers.
- **Atomic Draft Recovery:** Every `15s` on dirty buffer and on `onStop` (via `ProcessLifecycleOwner` / screen lifecycle intent), LOK exports a delta/snapshot atomically (`write temp -> fsync -> rename`) into `filesDir/drafts/{sha256}` and marks `DraftRecoveryEntity.hasPendingRecovery = true`. Saving explicitly (`Done` / `Save`) commits back to the source URI with **zero paywall lock** (`SAVE_HEADER` banned) and clears the recovery flag.

### 7.2 `PdfiumAndroid` & `PDFBox-Android` (`:engine:pdf`)
- `io.legere:pdfiumandroid:2.0.3` renders pages/tiles on `Dispatchers.IO` bounded semaphore, extracting outline/TOC and text search coordinates.
- `PDFBoxResourceLoader.init(applicationContext)` initializes lazily on first `:engine:pdf` use (never blocking `Application.onCreate()` cold launch `< 1.2s` budget!).
- `PDFBox-Android 2.0.27.0` + `BouncyCastle` execute non-destructive annotation saving, signature PNG stamping, PKCS#7 `.p12`/`.pfx` signing/inspection, AES-128/256 encryption/decryption, `Splitter` page extraction, image-downsampling `Compress`, and `Watermark` rendering.

### 7.3 Offline `HWP/HWPX` (`:engine:hwp`) & `ZIP/7z` (`:engine:archive`)
- **HWP/HWPX:** `hwplib` / `hwpxlib` parse document AST on `Dispatchers.IO`, escape all text nodes, strip active scripts, and generate deterministic local HTML/CSS/SVG rendered inside an offline sandboxed `WebView` (`allowFileAccess = false`, `allowFileAccessFromFileURLs = false`, `javaScriptEnabled = false`, custom `WebViewClient.shouldInterceptRequest` blocking all `http://`/`https://` network calls). Files `> 50 MB` display an upfront memory notice.
- **ZIP/7z:** `Apache Commons Compress 1.28.0` (`ZipFile` / `SevenZFile`) enumerates archive metadata lazily and validates every extracted entry path (`canonicalTarget.startsWith(canonicalDestDir)`) to prevent **Zip Slip** path traversal. Tapping `.rar` opens a clear informational sheet stating `.zip` and `.7z` are supported offline (`CD-04`).

### 7.4 Embedded Wi-Fi Web/FTP Transfer (`:engine:transfer`)
- Foreground `Service` (`android:foregroundServiceType="dataSync"`) runs `Ktor CIO` HTTP server on `0.0.0.0:8998` + embedded FTP listener (`port 8998`, optional PIN/Read-Only toggle).
- Serves bundled `assets/wifi_web/index.html` (zero external CDNs). Every `?path=` and multipart upload filename passes canonical sandbox validation (`canonical.startsWith(allowedRoot)` -> `403 Forbidden` on `..` traversal attempts).
- `10m` idle timer (`delay(10.minutes)` reset on active requests) stops service cleanly and syncs Quick Settings `TileService`.

### 7.5 Cold Start & External `ACTION_VIEW` Intent Router (`:app`)
- `MainActivity` (`launchMode="singleTop"`) plus manifest `<activity-alias>` targets (`PDF`, `Word`, `Excel`, `PPT`, `HWP`, `Text`, `Scan`, `Sign`, `OCR`, `External`).
- Incoming `content://` or `file://` URIs undergo **magic-byte sniffing** (first `16` bytes: `%PDF-` -> PDF, `PK\x03\x04` -> OOXML/ODF/HWPX/EPUB/ZIP inspection, `\xD0\xCF\x11\xE0` OLE2 -> DOC/XLS/PPT/HWP5, `7z\xBC\xAF\x27\x1C` -> 7z) before copying read-only streams to `cacheDir/external_imports/` so misnamed extensions open reliably in `< 1.5s`.

---

## 8. CI/CD Workflows (`CD-12`) & `16 KB` / R8 Hardening

### 8.1 Existing GitHub Repository Workflows (`.github/workflows/`)
1. **`android-ci.yml`** (triggers on `push` / `pull_request` to `main`, `duylt_dev`):
   - Sets up JDK 17 + Gradle cache + Android SDK 36.
   - Runs `./gradlew testDebugUnitTest lintDebug assembleDebug`.
   - Runs `python3 -I scripts/verify_16kb_elf.py app/build/outputs/apk/debug/app-debug.apk` ensuring every bundled `.so` satisfies `PT_LOAD p_align == 0x4000`.
2. **`lok-android-16kb.yml`** (`workflow_dispatch` on `ubuntu-24.04` with `easimon/maximize-build-space` and NDK `28.2.13676358`):
   - Checks out Collabora/LibreOffice core Android target, configures `--with-distro=LibreOfficeAndroidAarch64` (or `CPAndroid` branch tag input) with `LDFLAGS="-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"`, compiles `arm64-v8a` `.so`, validates `llvm-readelf -l` + `verify_16kb_elf.py` (`0x4000`), and uploads both the prebuilt `.so` artifact and `MPLv2` source-patch disclosure archive.

### 8.2 R8, Packaging & Security Guardrails
- `isMinifyEnabled = true`, `isShrinkResources = true` on `release`; strict `proguard-rules.pro` rules for `PDFBox`, `BouncyCastle`, `hwplib`, `Ktor`, `Room`, `Hilt`, and `LibreOfficeKit` JNI callbacks.
- **ABI Filtering:** `debug` packages `arm64-v8a` + `x86_64` (device + emulator); `release` packages `arm64-v8a` only (matching `R-01` LOK ARM64 `.so`, avoiding `UnsatisfiedLinkError` from mismatched 32-bit AAR `.so` files).
- `packaging.jniLibs.useLegacyPackaging = false` (ensures uncompressed, `16 KB`-aligned `.so` storage inside `.aab`).
- **Banned Dependencies / Permissions:** Zero `Chucker` in release (`0` debug network inspectors), zero `AGPLv3` binaries, zero `SYSTEM_ALERT_WINDOW`, zero `USE_FULL_SCREEN_INTENT`, zero hardcoded keys/tokens in VCS.

---

## 9. Verification & Definition-of-Done Matrix (`T-003` -> `T-027`)

- **Automated Unit Tests (`JVM`):** Every ViewModel tested with `Turbine` + `kotlinx-coroutines-test` + readable fakes; every `:core:domain` parser/validator/policy tested on JVM.
- **Static & Binary Checks:** `./gradlew lintDebug`, `scripts/verify_16kb_elf.py` (`0` unaligned `PT_LOAD` segments), `validate-docs.cjs`.
- **Physical Device Verification (`SM_A165F`):** Macrobenchmark Baseline Profile cold start `< 1.2s`, Airplane-mode offline tests (`HWP`, `PDF->DOCX`, `OCR`, `LOK`), process-death `DraftRecoveryEntity` check, EEA UMP VPN test (`Sprint 4`), and OWASP Mobile Top 10 audit (`docs/08_QA_TEST_REPORT.md`).
