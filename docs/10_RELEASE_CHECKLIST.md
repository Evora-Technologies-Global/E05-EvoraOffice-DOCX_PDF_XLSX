# Phase 8 — Release Engineering, Data Safety & Play Submission Checklist (`10_RELEASE_CHECKLIST.md`)

**Project:** Evora Office (`E05`) — Native Android All-Document Reader, Editor & PDF Tools  
**Company:** Evora Technologies Global  
**Task ID:** `T-027` (Phase 8 — Store Release Preparation)  
**Package Name (`applicationId`):** `com.evora.technologies.office`  
**Release Candidate:** `versionName = "1.0.0"`, `versionCode = 100` (`minSdk = 26`, `targetSdk = 36`, `compileSdk = 37`)  
**Owner Personas:** `mobile-release-engineer` + `appsec-engineer` + `app-store-optimizer`  
**Date:** `2026-11-27`  
**Status:** **RELEASE KIT READY — PENDING PRODUCTION CREDENTIALS & CEO SUBMISSION**  
**Upstream Inputs:** [`02_PROJECT_CHARTER.md`](02_PROJECT_CHARTER.md) · [`03_PRD.md`](03_PRD.md) · [`06_SPRINT_BACKLOG.md`](06_SPRINT_BACKLOG.md) · [`07_TECH_SPEC.md`](07_TECH_SPEC.md) · [`08_QA_TEST_REPORT.md`](08_QA_TEST_REPORT.md) · [`09_ASO_METADATA.md`](09_ASO_METADATA.md)

---

## 1. Dated Release Decision & Readiness Audit Table

| Date | Owner / Gate | Audit / Release Action | Status |
|---|---|---|---|
| `2026-11-25` | `PD-01`, `CD-02` | Locked Native Android (`Kotlin + Compose`), `versionName = "1.0.0"`, `versionCode = 100`, `minSdk = 26`, `targetSdk = 36`. | **Locked** |
| `2026-11-25` | `CD-09` | Google Play account is a **Personal Developer account**; external 14-day/12-tester administrative logistics are tracked outside engineering execution per CEO directive. | **Noted (`CD-09`)** |
| `2026-11-27` | **CEO Gate 3** | Approved [`08_QA_TEST_REPORT.md`](08_QA_TEST_REPORT.md) with **ZERO Blockers (`P0 = 0`, `P1 Open = 0`)**: `84/84` tests, `0` lint errors, `86/86` `16 KB` `.so`, `910.70 ms` cold start on `SM-A165F`. | **Approved** |
| `2026-11-27` | `mobile-release-engineer` | Audited ad-free `app-release.aab` (`119.18 MiB` compressed / `104.95 MiB` Play payload, `22/22` `16 KB` `.so`, `assets/dexopt/baseline.prof` embedded, signed by local `.local/evora-upload-test.jks`). All AdMob/Mediation/UMP/Adjust/`AD_ID`/Rewarded Ad code removed per CEO directive; `79/79` tests (`27` suites) & `0` lint errors. Built Fastlane kit & MPLv2 disclosure tarball + `.sha256` manifest (`8.2K`). | **Verified (Test Keystore)** |
| `Pending` | **CEO / Release Owner** | Swap production credentials (§2.2: Play upload keystore, `app/google-services.json`, Play Billing SKUs, Cloud OAuth & Privacy/Terms URLs) and run `bundle exec fastlane android internal`. | **Pre-Submission Gate** |

---

## 2. Artifact Verification & Production Credential Swap Gate

### 2.1 Current Local Release Artifact Snapshot (Verified `2026-11-27`)

| Check | Observed Value | Evidence / Tool Output |
|---|---|---|
| **AAB Bundle Path** | `app/build/outputs/bundle/release/app-release.aab` | `119.18 MiB` total compressed / `104.95 MiB` Play payload (excl. `BUNDLE-METADATA/`) |
| **ABI / Splits / R8** | `arm64-v8a` release filter; `abi`/`density`/`language` splits `enableSplit = true`; `isMinifyEnabled = true`; `isShrinkResources = true` (`4` DEX files) | [`app/build.gradle.kts`](../app/build.gradle.kts) · [`proguard-rules.pro`](../app/proguard-rules.pro) |
| **16 KB Native Page Gate** | **`22/22` `.so` ELF64 `PT_LOAD p_align == 0x4000`** (`14` LibreOfficeKit, `2` PdfiumAndroid, `2` ML Kit, `2` CameraX, `2` AndroidX — zero AdMob/AppLovin/Unity/Adjust `.so`) | `python3 -I scripts/verify_16kb_elf.py app/build/outputs/bundle/release/app-release.aab` |
| **Baseline Profiles** | `assets/dexopt/baseline.prof` embedded (`saveInSrc = true`, obsolete ad/Adjust rules stripped) | Verified in AAB ZIP entries & `output-metadata.json` |
| **Debug Symbol Mapping** | `app/build/outputs/mapping/release/mapping.txt` | Generated on release build; uploaded via Fastlane `supply` `mapping:` |
| **Current Signing State** | Signed with **local test upload keystore** (`.local/evora-upload-test.jks`, alias `evora_upload`) | `jarsigner -verify -certs` (`jar verified.`) |
| **Ad-Free & Banned SDK Check** | **Zero** AdMob, Mediation (AppLovin/Unity/Mintegral/Pangle), UMP, Adjust, `AD_ID` / `ACCESS_ADSERVICES_*`, `Chucker`, `LeakCanary`; `allowBackup="false"` | [`app/build.gradle.kts`](../app/build.gradle.kts) · [`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml) |

### 2.2 Mandatory Production Credential Swap Before Play Store Upload (Release-Blocking)

The local QA release AAB intentionally omits production secrets from VCS. **Do NOT promote a build to Production until all 5 items below replace their local/test placeholders:**

| # | Component | Current Local State (Verified in Repo) | Mandatory Production Action Before `fastlane android internal` |
|---|---|---|---|
| **1** | **Upload Keystore (`Play App Signing`)** | `keystore.properties` points to `.local/evora-upload-test.jks` (`evora_upload` test key) | Enroll in **Google Play App Signing**; generate/use dedicated production upload keystore stored in encrypted vault + CI secrets (`EVORA_RELEASE_STORE_FILE`, `EVORA_RELEASE_STORE_PASSWORD`, `EVORA_RELEASE_KEY_ALIAS`, `EVORA_RELEASE_KEY_PASSWORD`) — never commit `.jks` or `keystore.properties`. |
| **2** | **Firebase (`google-services.json`)** | `app/google-services.json` absent; `app/build.gradle.kts` conditionally skips `com.google.gms.google-services` | Download production `google-services.json` for `com.evora.technologies.office` from Firebase Console, inject in CI secret/local ignored path, and publish the 4 non-ad Remote Config defaults (`onboarding_show_language`, `onboarding_show_paywall`, `paywall_default_sku`, `lok_autosave_interval_sec`). |
| **3** | **Play Billing SKUs** | `SubscriptionPlan` enum hardcodes `e05_weekly_trial`, `e05_yearly_pro_3999`, `e05_lifetime_pro` | Create exact matching product IDs in Play Console (`Subscriptions` with 3-day / 7-day trials + `In-app products` lifetime w/ Tier-2/3 geo pricing); add QA License Tester emails before internal track smoke test. |
| **4** | **Cloud OAuth (Drive & OneDrive)** | `evora_onedrive_client_id` in [`strings.xml`](../app/src/main/res/values/strings.xml) and `client_id` in [`msal_config.json`](../app/src/main/res/raw/msal_config.json) are blank `""` | Register **Google Play App Signing SHA-1** AND **Upload Key SHA-1** in Google Cloud Console (`Drive API v3` OAuth Android client); set Entra ID Application ID in `evora_onedrive_client_id` + `msal_config.json` with `msauth://com.evora.technologies.office/msal_auth` signature hash. |
| **5** | **Legal & Store URLs** | Play Console metadata fields | Publish live HTTPS **Privacy Policy URL**, **Terms of Service URL**, and **Support Email** in Play Console (`Store settings` + `App content`). |

---

## 3. Fastlane Release Pipeline & Keystore Hygiene

Fastlane configuration files are committed and syntax-checked (`ruby -c` passed) at [`../fastlane/Appfile`](../fastlane/Appfile), [`../fastlane/Gemfile`](../fastlane/Gemfile), [`../fastlane/Fastfile`](../fastlane/Fastfile), and localized store listings under [`../fastlane/metadata/android/`](../fastlane/metadata/android/en-US/title.txt).

### 3.1 Keystore & Play App Signing Security Rules

1. **Enforce Google Play App Signing:** Google holds the final App Signing key (irreversible safety net if an upload key is ever rotated); Evora holds only the **Upload Key** (`RSA 2048-bit` or `4096-bit`, valid `>= 25` years).
2. **Zero Secrets in Git:** `.gitignore` already excludes `*.jks`, `*.keystore`, `keystore.properties`, `signing.properties`, `.env*`, and `.local/`. CI runners decode base64-encrypted upload keystores and `play-service-account.json` into ephemeral runner temp files deleted post-job.
3. **Monotonic Versioning (`versionCode`):** Every store upload increments `versionCode` monotonically (`100 -> 101 -> 102`, never reused or decremented).

### 3.2 Fastlane Commands (`fastlane/Fastfile`)

```bash
# 1. Install Fastlane locally or on CI runner
bundle install --gemfile=fastlane/Gemfile

# 2. Local/CI verification gate (unit tests, lint, bundleRelease, 16KB ELF check, MPLv2 archive, signature verify)
bundle exec fastlane android verify_release

# 3. Dry-run Play Console API validation before touching live tracks
PLAY_JSON_KEY_FILE=.local/play-service-account.json bundle exec fastlane android validate_play

# 4. Build & ship to Internal Testing track (guard fails if google-services.json is missing)
PLAY_JSON_KEY_FILE=.local/play-service-account.json bundle exec fastlane android internal

# 5. Promote verified build from Internal to Closed Testing (or Production staged rollout at 1% = 0.01)
PROMOTE_FROM_INTERNAL=true VERSION_CODE=100 bundle exec fastlane android closed
ROLLOUT=0.01 FROM_TRACK=internal VERSION_CODE=100 bundle exec fastlane android promote_staged

# 6. Emergency rollout halt if Crash-Free or ANR SLO breaches
VERSION_CODE=100 bundle exec fastlane android halt_production
```

---

## 4. Google Play Permissions Audit & `MANAGE_EXTERNAL_STORAGE` Compliance

### 4.1 Merged Release Manifest Permission Inventory

Audited directly from `app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml`:

| Permission | Origin | Justification & Play Console Action |
|---|---|---|
| `MANAGE_EXTERNAL_STORAGE` | `:app` [`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml) | **Core All-Files Document Management:** Required to auto-index & manage user `PDF/DOCX/XLSX/PPTX/HWP/ODF/ZIP/7z` across arbitrary device folders & SD card, edit in-place, and batch move/copy/trash. **Requires Play Permissions Declaration + English Video (§4.2).** |
| `READ_EXTERNAL_STORAGE` (`<=32`), `WRITE_EXTERNAL_STORAGE` (`<=29`) | `:app` Manifest | Legacy scoped fallback on Android 8.0–12 (`minSdk 26`). |
| `CAMERA` (`uses-feature required="false"`) | `:app` Manifest | CameraX Document Scanner, Dual ID Card A4 scan, and QR/Barcode capture; optional feature so non-camera Chromebooks/tablets are not blocked. |
| `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` | `:app` Manifest | Local LAN Wi-Fi HTTP/FTP server IP detection, user-initiated Drive/OneDrive sync, Play Billing, Firebase Analytics/Remote Config. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `POST_NOTIFICATIONS` | `:app` Manifest | User-started `WifiTransferService` (`dataSync` foreground notification + `10m` idle auto-stop). |
| `com.android.vending.BILLING`, `BIND_GET_INSTALL_REFERRER_SERVICE` | Merged (`billing-ktx`, `firebase-analytics`) | Google Play Billing `8.0.0` purchases & Play Install Referrer (`AD_ID` and `ACCESS_ADSERVICES_*` explicitly stripped via `tools:node="remove"` + `google_analytics_adid_collection_enabled=false`). |
| `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | Merged (Jetpack WorkManager / Core) | Standard Jetpack background worker wake lock & broadcast receivers. |
| `android.permission.NFC` (`uses-feature required="false"`) | Merged transitively via `com.microsoft.identity.client:msal:7.1.0` | MSAL supports YubiKey/FIDO2 NFC smartcards optionally (`required="false"`). Verify during Play review or add `<uses-permission android:name="android.permission.NFC" tools:node="remove" />` if enterprise smartcard auth is not needed. |

**Explicitly Absent High-Risk / Ad Permissions (Verified `0` occurrences in merged release manifest):** `AD_ID`, `ACCESS_ADSERVICES_AD_ID`, `ACCESS_ADSERVICES_ATTRIBUTION`, `SYSTEM_ALERT_WINDOW`, `USE_FULL_SCREEN_INTENT`, `QUERY_ALL_PACKAGES`, `ACCESS_BACKGROUND_LOCATION`, `READ_CONTACTS`, `RECORD_AUDIO`, `REQUEST_INSTALL_PACKAGES`. Declare **`No` — Advertising ID not used** and **`Contains ads: No`** in Google Play Console App Content.

### 4.2 `MANAGE_EXTERNAL_STORAGE` Play Console Declaration & English Compliance Screencast

- **Play Console App Content -> Sensitive Permissions -> All Files Access Declaration:**
  - **Core App Functionality Category:** *File management / Document creation, editing & management utility* (`Document Management`).
  - **Why MediaStore / SAF alone is insufficient:** Users receive and store mixed-format office documents (`PDF`, `DOC/DOCX`, `XLS/XLSX`, `PPT/PPTX`, `ODT/ODS/ODP`, `HWP/HWPX`, `ZIP/7z`) across arbitrary directories (`/Documents`, `/Download`, WhatsApp/Telegram/email folders, SD cards) and expect automatic indexing, batch folder organization, in-place Office editing back to source paths, and archive extraction without re-picking every directory manually. SAF (`ACTION_OPEN_DOCUMENT` / `ACTION_OPEN_DOCUMENT_TREE`) remains wired as a seamless fallback if the user declines All-Files Access.
  - **Prominent In-App Disclosure Verified:** [`StoragePermissionRoute.kt`](../feature/onboarding/src/main/kotlin/com/evora/technologies/office/feature/onboarding/permission/StoragePermissionRoute.kt) presents `"Manage Your Local Documents"` + `"Grant All-Files Access to automatically index PDF, Word, Excel, PPT, HWP, and ZIP/7z files across device folders. 100% Offline Promise: Files never leave your phone. Zero server uploads."` prior to launching `ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`.

- **English Compliance Screencast Capture Script (`45–60s`, Unlisted YouTube / MP4 upload):**
  1. **`00:00–00:10` (Launch & Prominent Disclosure):** Cold launch `Evora Office` (`com.evora.technologies.office`) in English locale -> skip intro -> stop clearly on `StoragePermissionRoute` showing the prominent disclosure card and both `"Allow All-Files Access"` and `"Use Folder Picker (SAF) Instead"` buttons.
  2. **`00:10–00:22` (System Grant):** Tap `"Allow All-Files Access"` -> show Android system `"All files access"` settings screen -> toggle **`Evora Office` ON** -> navigate back to the app.
  3. **`00:22–00:42` (Core Functionality Proof):** Show `HomeDocumentsRoute` indexing local `PDF`, `DOCX`, `XLSX`, `HWP`, and `ZIP` documents -> switch to `StorageBrowserRoute` (`Internal` / `Downloads` folders) -> multi-select two files to demonstrate **Move / Copy / Merge PDF** across folders -> open a `.docx` file in `OfficeEditorRoute` and tap `Done` to save locally.
  4. **`00:42–00:55` (SAF Alternative & Offline Proof):** Briefly show `Add Folder (SAF)` button working cleanly as fallback and zero background uploads.

- **ADB Screenrecord Command (Physical `SM-A165F` / `RF8Y60B9NCZ`):**
  ```bash
  adb -s RF8Y60B9NCZ shell screenrecord --bit-rate 8000000 --time-limit 60 /sdcard/Download/evora_all_files_compliance.mp4
  adb -s RF8Y60B9NCZ pull /sdcard/Download/evora_all_files_compliance.mp4 .local/release_kit/
  ```

---

## 5. Google Play Data Safety Form Declaration (SDK-Verified)

Audited against actual codebase dependencies (`gradle/libs.versions.toml`, `engine/cloud/`, `engine/monetization/`, `engine/transfer/`, `app/src/main/res/xml/network_security_config.xml`):

> [!IMPORTANT]
> **Local Document Content Rule:** Evora Office processes PDFs, Office files, HWP/HWPX, scans, OCR, and ZIP/7z archives **100% on-device**. Document contents are **never** uploaded to Evora servers (`$0` server COGS). Network transmission occurs only when the user explicitly connects **Google Drive / OneDrive** (direct to Google/Microsoft OAuth endpoints), uses **local LAN Wi-Fi Transfer**, or sends non-document product telemetry / config fetches via **Firebase Analytics & Remote Config** (ad-free build: **zero** ad SDKs, **zero** MMPs, **zero** `AD_ID` collection).

### 5.1 Data Collection & Sharing Summary

| Data Safety Question | Answer | Grounded Codebase Evidence |
|---|---|---|
| **Does your app collect or share any of the required user data types?** | **Yes** *(Collected: Analytics, Billing & optional Cloud OAuth; Shared with Ad Networks: No)* | `Firebase Analytics` / `Firebase Remote Config` (`google_analytics_adid_collection_enabled=false`), `Google Play Billing 8.0.0`, and optional user-initiated `Google Drive` / `Microsoft OneDrive` sign-in. Zero AdMob, Mediation, UMP, or Adjust SDKs in this ad-free build. |
| **Is all collected data encrypted in transit?** | **Yes** | [`network_security_config.xml`](../app/src/main/res/xml/network_security_config.xml) sets global `cleartextTrafficPermitted="false"` with system CA trust anchors (only local LAN `WifiTransferService` serves user-initiated offline HTTP/FTP on local Wi-Fi). |
| **Can users request data deletion / revoke consent?** | **Yes** | (1) `CloudDriveBrowserRoute` -> `disconnectAccount` wipes Android Keystore AES-256-GCM token (`AndroidKeystoreTokenVault`) + `CloudAccountDao` row; (2) `SettingsRoute` -> `Privacy Notice` (`OpenPrivacyNotice`) explains 100% on-device document processing with zero server uploads and no ad tracking; (3) `AD_ID`, `ACCESS_ADSERVICES_ATTRIBUTION`, and `ACCESS_ADSERVICES_AD_ID` are stripped from the merged release manifest via `tools:node="remove"`. |

### 5.2 Data Types Matrix (Google Play Data Safety Form Mapping)

| Play Data Category & Type | Collected? | Shared? | Ephemeral? | Optional / Required | Purpose(s) | Exact SDK / Module Responsible |
|---|---|---|---|---|---|---|
| **Device or other IDs** *(Firebase Installation ID / App Instance ID only — **no Advertising ID (`AD_ID`)**)* | **Yes** | **No** *(Zero ad networks / MMPs)* | No | Required for Firebase Analytics & Remote Config when `google-services.json` is active | Analytics, App functionality (`Remote Config`) | `firebase-analytics:24.0.0`, `firebase-config:23.0.1` (`AD_ID` & `ACCESS_ADSERVICES_*` removed and `google_analytics_adid_collection_enabled=false` in [`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml)) |
| **App activity** *(App interactions, viewed screens, onboarding/tool events)* | **Yes** | **No** *(Google Analytics / Firebase Telemetry only)* | No | Collected when Google Services is enabled | Analytics | [`FirebaseTelemetry.kt`](../engine/monetization/src/main/kotlin/com/evora/technologies/office/engine/monetization/FirebaseTelemetry.kt) (`onboarding_step`, `document_open`, `pdf_tool_complete`, `scan_ocr_complete`, `paywall_impression`, `purchase_success`) |
| **App info and performance** *(Diagnostics, crash/ANR telemetry, Remote Config fetch)* | **Yes** | **No** *(Play Vitals / Firebase)* | No | Required for app functionality & diagnostics | App functionality, Analytics | `Firebase Remote Config`, `Play Core / Vitals` |
| **Financial info — Purchase history** *(Play Billing subscription/IAP SKU & token)* | **Yes** | No *(Google Play Billing -> verified locally via HMAC)* | No | Optional (user-initiated Pro purchase) | App functionality (Pro entitlement unlock & `Restore Purchases`) | [`PlayBillingEntitlementRepository.kt`](../engine/monetization/src/main/kotlin/com/evora/technologies/office/engine/monetization/PlayBillingEntitlementRepository.kt) · [`SignedEntitlementCache.kt`](../engine/monetization/src/main/kotlin/com/evora/technologies/office/engine/monetization/SignedEntitlementCache.kt) |
| **Personal info — Name, Email address** *(Only if Cloud Drive connected)* | **Yes** *(On-device + OAuth)* | No *(Not sent to Evora servers)* | No | Optional (only when user taps Connect Drive / OneDrive) | Account management / Cloud Drive sync UI display | `GoogleDriveCloudClient` / `OneDriveCloudClient` (`CloudAccountEntity` stores email/displayName locally; OAuth access token encrypted in `AndroidKeystoreTokenVault`) |
| **Files and docs** *(Local & Cloud files)* | **No** *(Processed 100% locally; Drive/OneDrive is direct user-to-provider transfer)* | **No** | N/A | N/A | N/A *(Do NOT declare File collection unless Evora operates a server; note that user-initiated Drive/OneDrive sync uses official Google/Microsoft APIs directly)* | Track A Offline Engines (`LibreOfficeKit`, `Pdfium`, `PDFBox`, `hwplib`, `ML Kit` on-device) |
| **Approximate Location** *(IP-derived country/region by Firebase SDKs)* | **Yes** *(IP-derived)* | **No** | Yes / SDK-managed | Required for `Firebase Analytics` / `Remote Config` | Analytics, App functionality | `Firebase Analytics`, `Firebase Remote Config` (AdMob, UMP, Mediation & Adjust removed) |

---

## 6. Open-Source Attribution & `MPLv2` / `LGPLv3` Disclosure Package

### 6.1 LibreOfficeKit (`MPLv2` / `LGPLv3`) Compliance Package

To keep Evora Office's proprietary Kotlin/Jetpack Compose codebase closed-source under **MPL-2.0 §3.3 (Larger Work)** while strictly honoring `LibreOfficeKit` disclosure obligations:

1. **Public Disclosure Repository (`CD-12`):** `https://github.com/Evora-Technologies-Global/E05-EvoraOffice-DOCX_PDF_XLSX`
2. **Release Disclosure Bundle Generator:** [`scripts/package_mplv2_disclosure.sh`](../scripts/package_mplv2_disclosure.sh) builds `.local/release_kit/evora-office-v1.0.0-mplv2-lok-disclosure.tar.gz` (`8.2K`) alongside its `.sha256` checksum file, verified containing:
   - `mplv2-disclosure/lo-core-commit.txt` (`87eb99e801cf8198d766a9d82107089d1c9304fe`, branch `libreoffice-25-8`)
   - `mplv2-disclosure/autogen.input` (`--with-distro=LibreOfficeAndroidAarch64`, NDK `28.2.13676358`, API `26`)
   - `mplv2-disclosure/lok_patches.diff` (`-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384` in `android/Bootstrap/Makefile.shared`)
   - `workflows/lok-android-16kb.yml` (reproducible Linux CI build recipe)
   - `lok-prebuilt.sha256` (exact SHA-256 lockfile for all `14` ARM64 `.so` binaries)
   - `NOTICE_MPLV2_LGPLV3.txt`
3. **In-App Attribution Screen Verified:** [`SettingsRoute.kt`](../feature/settings/src/main/kotlin/com/evora/technologies/office/feature/settings/SettingsRoute.kt) exposes **Open-Source Licenses** displaying `LibreOfficeKit / Collabora Android Core (MPLv2 / LGPLv3)`, `PdfiumAndroid`, `PDFBox-Android`, `BouncyCastle`, `hwplib & hwpxlib`, `Apache Commons Compress & XZ`, `Ktor CIO`, `ZXing Core`, and `AndroidX / Jetpack Compose / Room / Glance / Hilt` (`0` `AGPLv3` libraries anywhere in dependency tree).

### 6.2 Cross-Platform Lifecycle Template Disposition (`PD-01`)

| Template Item (`product-lifecycle-workflow.md`) | E05 Status | Rationale |
|---|---|---|
| **iOS Certificates & Provisioning Profiles** | `N/A — Android-Only Native Build (PD-01)` | Project Charter `PD-01` scopes E05 strictly to Native Android; replaced by Play App Signing & Upload Keystore (§2.2, §3). |
| **Apple `PrivacyInfo.xcprivacy` Manifest** | `N/A — Android-Only Native Build (PD-01)` | No iOS target exists; replaced by Google Play Data Safety Form & Permissions Declaration (§4, §5). |

---

## 7. Pre-Submission Go/No-Go Checklist & Staged Rollout Runbook

### 7.1 Final Release-Blocking Pre-Submission Checklist (`v1.0.0` / `versionCode 100`)

- [x] `versionName = "1.0.0"` and `versionCode = 100` verified monotonic and exact in `app/build.gradle.kts`
- [x] `minSdk = 26`, `targetSdk = 36`, `compileSdk = 37` verified in `AndroidConventionPlugins.kt` and merged release manifest
- [x] `79/79` unit/Robolectric tests (`27` suites) and `lintDebug` (`0` errors) passed; zero ad/mediation/UMP/Adjust/`AD_ID` dependencies or permissions remain
- [x] `22/22` `.so` libraries inside `app-release.aab` passed `16 KB (0x4000)` ELF64 `PT_LOAD` check (`verify_16kb_elf.py`)
- [x] R8 minification, resource shrinking, `allowBackup="false"`, and `cleartextTrafficPermitted="false"` verified (`4` DEX files, `119.18 MiB` AAB / `104.95 MiB` Play payload)
- [x] `baseline-prof.txt` embedded in AAB (`obsolete ad/Adjust rules removed`)
- [x] Localized Play Store metadata (`en-US`, `vi` + 8 markets) verified within character limits in `fastlane/metadata/android/` (updated for ad-free launch)
- [x] `MPLv2 / LGPLv3` disclosure tarball built and SHA-256 verified via `scripts/package_mplv2_disclosure.sh`
- [ ] **Production Credential Swap Complete (§2.2):** Production upload keystore, `app/google-services.json`, Play Billing SKUs, Drive/OneDrive OAuth credentials, and live Privacy/Terms/Support URLs populated
- [ ] **Store Graphics & Compliance Video Uploaded:** `512×512` icon, `1024×500` feature graphic, 8 phone + tablet screenshots (`09_ASO_METADATA.md` §5), and English `MANAGE_EXTERNAL_STORAGE` screencast uploaded to Play Console
- [ ] **Release Candidate Smoke Test on Internal Track:** Signed production AAB downloaded via Google Play Internal Testing link onto `SM-A165F` (`RF8Y60B9NCZ`), verifying Play Billing sandbox purchase + restore and Drive/OneDrive login
- [ ] **R8 Mapping Uploaded:** Confirm `app/build/outputs/mapping/release/mapping.txt` is attached to `versionCode 100` in Play Console App Bundle Explorer

### 7.2 Phased Rollout Schedule & Halt / Forward-Fix SLO Runbook

Once an `.aab` ships to Play users, **binaries cannot be rolled back** — only halted and rolled forward via monotonic `versionCode` (`101+`). Never launch straight to `100%`.

```text
Internal Track (QA/CEO) ──> Closed Testing (CD-09 if Personal) ──> Staged Production Rollout:
   [100% Internal]            [12+ testers / 14d if Personal]        1% (24h) ──> 5% (24h) ──> 20% (48h) ──> 50% (48h) ──> 100%
```

| Rollout Gate | Target Metric (`Play Android Vitals` + `Firebase` + `Reviews`) | Warning Watch (Hold Expansion) | Mandatory Halt Signal (`fastlane android halt_production`) |
|---|---|---|---|
| **Crash-Free Sessions** | `>= 99.8%` *(Charter Target)* | `99.5% – 99.79%` | **`< 99.5%`** OR any crash spike inside `LibreOfficeKit` / `MediaStoreDocumentScanner` |
| **ANR Rate** | `<= 0.15%` *(Charter Target)* | `0.16% – 0.46%` | **`> 0.47%`** (Play Bad Behavior threshold) |
| **1-Star Reviews / Billing** | `<= 4.0%` 1★ share | `> 5.0%` 1★ citing specific crash/format | Any P0 regression (data loss, billing double-charge/non-unlock, broken `ACTION_VIEW`) |

**Forward-Fix Procedure on Halt:**
1. Execute `VERSION_CODE=100 bundle exec fastlane android halt_production` immediately.
2. Pull symbolicated stack trace from **Play Console Android Vitals** (decoded automatically via uploaded `mapping.txt` + native LOK symbols).
3. Branch hotfix from release tag `v1.0.0`, patch & verify unit tests + `SM-A165F` repro + `verify_16kb_elf.py`.
4. Bump `versionCode` to **`101`** (`versionName = "1.0.1"`), upload to `internal` for smoke verification, then restart staged production rollout at `1%` (`ROLLOUT=0.01`).
