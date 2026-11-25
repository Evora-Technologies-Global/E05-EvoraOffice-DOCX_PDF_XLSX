# Sprint Backlog & Execution Plan — E05 (`Evora Office`)

**Project Code:** E05  
**Product Brand & Package:** `Evora Office` (`Evora Office: DOCX, PDF, XLSX` — `com.evora.technologies.office`, `CD-05`)  
**Release Target:** `versionName = "1.0.0"`, `versionCode = 100` (Single Full-Parity Release — Amended Option P2, `CD-02`)  
**Owners:** Senior Project Manager (`project-manager-senior`) · Sprint Prioritizer (`sprint-prioritizer`)  
**Date:** 2026-11-25  
**Status:** **COMPLETE (Phase 5 Approved Pipeline -> Ready to Unlock Phase 6 Architecture & Engineering)**  
**Upstream Inputs:**  
- [`01_COMPETITOR_REVERSE_ENGINEERING.md`](01_COMPETITOR_REVERSE_ENGINEERING.md) *(Real-world modules, native engines, competitor flaw remediation)*  
- [`02_PROJECT_CHARTER.md`](02_PROJECT_CHARTER.md) *(`PD-01`–`PD-02`, `CD-01`–`CD-09`, 4-week target, Track A `$0` Server)*  
- [`03_PRD.md`](03_PRD.md) *(`F-01`–`F-10`, 5 Room entities, Firebase Remote Config, Ktor LAN API, 21 routes)*  
- [`04_USER_STORIES.md`](04_USER_STORIES.md) *(`37` prioritized stories `US-001`–`US-112`)*  
- [`05_DESIGN_SYSTEM.md`](05_DESIGN_SYSTEM.md) *(`Gate 2` Approved & Locked `CD-10`; `S-01`–`S-20` + `W-01`/`W-02`; WCAG AA `75/75`)*  
- [`.gemini/rules/android-native-standards.md`](../.gemini/rules/android-native-standards.md) · [`LLM.md`](../LLM.md)

---

## 1. Sprint Strategy & Prioritization Framework

### 1.1 Charter Constraints Governing Scheduling
1. **Single Full-Parity `1.0.0` (`100`) Release (`CD-02`):** All `P0` and `P1` features across `F-01`–`F-10` ship together inside a 4-week `[Internal Target]` AI-accelerated schedule (`Week 1` completed Phase 3 + Phase 4 + `R-01` NDK check; `Weeks 2–4` execute Sprints 1–4 + Phase 7 QA + Phase 8 Store prep).
2. **Feature-First Ads Deferral (`CD-07`):** **Strict sequencing rule:** Core product features (`F-01`–`F-09`) MUST reach `Done` before wiring active `AdMob Mediation` placements. Sprints 1–3 code against clean `ProEntitlementPort` & `AdPlacementHook` domain abstractions (`no-op` / local QA toggle) with zero ad SDK calls. `Sprint 4` activates `BillingClient 8.0.0`, `Google UMP CMP`, and `AdMob Mediation` strictly after `F-01`–`F-09` sign-off.
3. **Zero Toy-Scope Estimates (Competitor Reality Grounding):** Every hidden complexity uncovered in [`01_COMPETITOR_REVERSE_ENGINEERING.md`](01_COMPETITOR_REVERSE_ENGINEERING.md) is explicitly scheduled:
   - `LibreOfficeKit / Collabora Android Core` (`MPLv2/LGPLv3`) cross-compiled on **GitHub Actions Linux Runner (`CD-08`)** under `duylt-dev` with NDK r27+/r28 `16 KB` flags (`-Wl,-z,max-page-size=16384`) and `llvm-readelf -l` `0x4000` validation (`R-01` critical path).
   - `PdfiumAndroid` (`Apache 2.0`) `16 KB` binary verification.
   - `48+` OS `<activity-alias>` intent filters replaced cleanly with granular supported format aliases (`PDF`, `Word`, `Excel`, `PPT`, `HWP`, `Text`, `Scan`, `Sign`, `OCR`, `External`) + magic-byte URI header sniffing (`US-004`).
   - Enriched 5-table Room database fixing competitor's path-only `RecentTable`/`TrashTable` flaw, plus `WorkManager` 30-day Trash auto-purge and 15-second atomic `DraftRecoveryEntity` checkpoints (`US-011`, `US-032`).
   - Real local engines replacing every competitor `"Coming soon"` stub: `Split PDF`, `Compress PDF`, `Watermark PDF`, `Tools Print`, `Dual-Side ID Card -> A4`, `Word Review / Track Changes`, `Continuous Scroll`, `Pure-Java ZIP/7z` (`Commons Compress 1.27.1`), and **100% offline `HWP/HWPX` (`hwplib`/`hwpxlib`) + `PDF->DOCX`** (`$0` server COGS).
   - Strict exclusion of competitor release/policy landmines (`zero Chucker` in release, `zero SYSTEM_ALERT_WINDOW` / lockscreen `IkmASideLock`, `zero SAVE_HEADER` ransom locks, `zero onResume` editor/scanner ads).
4. **Pre-Code Architecture Gate (`LLM.md` §10):** Zero product `.kt` files may be written until `T-001` (`docs/07_TECH_SPEC.md` + `LLM.md` §2–§10) completes.

### 1.2 Sprint Allocation Overview (RICE / MoSCoW / Critical-Path Driven)

| Sprint | Target Window (`CD-02`) | Primary Goal & Exit Milestone | Modules Covered | Story Points |
|---|---|---|---|---:|
| **Sprint 1 — Core Foundation, Library, Reader & `R-01` CI** *(MVP Core)* | **Week 2 (Days 1–4)** | Lock `07_TECH_SPEC.md` + `LLM.md`; compile `LibreOfficeKit 16 KB` on Linux CI (`R-01`); build MVI Gradle shell, Design System, Room DB, Onboarding/Intent Router, Fast Library, Universal Reader (`PDF`, `HWP`, `EPUB`, `TXT`), and `ZIP/7z` Inspector. | Tech Spec, `R-01`, `F-01`, `F-02` (core), `F-03`, `F-08` (ZIP/7z) | `68` |
| **Sprint 2 — PDF Power Suite, Scanner/OCR & Office Editors** | **Week 2–3 (Days 5–9)** | Ship `PDFBox` Annotations, Handwritten e-Sign, PKCS#7 & AES Security; Real Split/Compress/Watermark/Print/Converters + Offline `PDF->DOCX`; `CameraX`/`ML Kit` Scan, Dual-Side ID Card, 5-Script OCR & QR; Integrate LOK `.so` into Word/Excel/PPT Editors + `SlideShowRoute` + 15s Draft Recovery + Offline Templates. | `F-05`, `F-06`, `F-07`, `F-04`, `F-08` (Templates) | `92` |
| **Sprint 3 — Storage, Wi-Fi Web/FTP, Cloud, Widgets & Core Lock** | **Week 3 (Days 10–12)** | Ship Storage Batch Manager, `Ktor CIO` Wi-Fi HTTP/FTP + `TileService`, Google Drive + OneDrive MSAL, Jetpack Glance Widgets (`4×1` & `4×2`), Settings core (`S-20`), and pass **Core Feature `Done` Gate** (`F-01`–`F-09` = 100% functional offline/cloud parity). | `F-08` (Storage/Wi-Fi), `F-09`, `F-02` (Glance), `S-20` core | `48` |
| **Sprint 4 — Hybrid Monetization (`CD-07`), QA Gate 3 & Store Release** | **Week 4 (Days 13–18)** | Integrate `Play Billing 8.0.0`, `Google UMP CMP`, `Firebase Remote Config/Analytics/Adjust`, `AdMob Mediation` (`16 KB` audited), `Baseline Profiles` (`< 1.2s`), execute **Phase 7 QA/Security (`08_QA_TEST_REPORT.md` -> Zero Blocker Gate 3)** on physical `SM_A165F`, and deliver **Phase 8 ASO & Release Kit (`09_ASO_METADATA.md`, `10_RELEASE_CHECKLIST.md`)**. | `F-10`, Perf/R8, Phase 7 QA, Phase 8 Release | `54` |
| **Total Release Scope** | **Weeks 2–4 (`v1.0.0` / `100`)** | **100% Parity + Competitor Stub Remediation + Zero Blocker Gate 3** | **All `F-01`–`F-10` · `37/37` US · `21+2` Screens** | **`262`** |

---

## 2. Sprint 1 Backlog — Core Foundation, Library, Universal Reader & `R-01` CI (`68` pts)

*Note: Sprint 1 establishes the multi-module MVI architecture, Design System tokens (`05_DESIGN_SYSTEM.md`), `R-01` native C++ artifact pipeline, and end-to-end Read + Library + Intent MVP loop.*

| Task ID | Title & Actionable Engineering Scope | Domain | User Stories | Screens | Size / Pts | Owner | Dependencies | Verification / Acceptance Hook |
|---|---|---|---|---|---|---|---|---|
| `T-001` | **[DONE]** **Architecture Tech Spec & `LLM.md` Map Population:** Author `docs/07_TECH_SPEC.md` (multi-module Gradle graph, **Hilt with KSP (`CD-11`)**, MVI base contracts, Room v1 DDL, LOK/Pdfium/PDFBox JNI boundaries, Ktor endpoints, R8 rules) and populate `LLM.md` §2–§10 before writing any `.kt` file. | Infra | All (`P0`) | All | `L` / `5` | `backend-architect` | Gate 2 (`CD-10`), `CD-11` | `07_TECH_SPEC.md` approved; `LLM.md` §10 lists exact module paths; zero `.kt` code written prior. |
| `T-002` | **[CI WORKFLOW READY — Blocked on GitHub OAuth Org Approval & `main` Merge]** **`R-01` Critical Path — GitHub Actions Linux LOK `16 KB` Build (`CD-08`, `CD-12`):** Configure Linux CI workflow inside existing repo `Evora-Technologies-Global/E05-EvoraOffice-DOCX_PDF_XLSX` (`easimon/maximize-build-space`, NDK r28b) to cross-compile `LibreOfficeKit` (`arm64-v8a`) with `-Wl,-z,max-page-size=16384`, verify `llvm-readelf -l` `p_align == 0x4000`, and emit prebuilt `.so` artifact + `MPLv2` source/patch manifest. | `F-04` / Infra | `US-030`, `US-040`, `US-041` | `S-14` | `XL` / `13` | `backend-architect` | `T-001`, `CD-12` | CI workflow green; downloaded `libsofficeapp.so` / `liblo-native-code.so` & bundled deps pass `readelf -l` `0x4000` check locally. |
| `T-003` | **[DONE — MVI/Hilt/Pdfium Foundation Verified]** **Gradle Multi-Module Skeleton, MVI Core & `PdfiumAndroid` `16 KB` Check:** Scaffold Gradle Kotlin DSL (`minSdk 26`, `targetSdk 36`, `compileSdk 37`, `versionName "1.0.0"`, `versionCode 100`, `com.evora.technologies.office`), `core:mvi` (`MviViewModel`, `UiState`, `UiIntent`, `UiEffect`, `launchSafely`, `CollectEffects`), `compose-stability.conf`, Hilt (KSP), and verify `PdfiumAndroid 2.0.3` `.so` for `0x4000` alignment. *(Add Detekt/ktlint plugin rule pass as Sprint 1 continues.)* | Infra | `US-112` (`P0`) | All | `L` / `5` | `mobile-app-builder` | `T-001` | `./gradlew assembleDebug testDebugUnitTest lintDebug` passes; `verify_16kb_elf.py` confirms `0x4000` on `libpdfium*.so`; zero `Chucker` in release deps. |
| `T-004` | **[DONE]** **Material 3 Design System (`Style B`) & Shared UI Kit (`core:designsystem`):** Bundle `Poppins` (`400/500/600/700`) + `JetBrains Mono` (`tnum`), implement exact Light/Dark `ColorScheme` + `EvoraFormatColors` (`8` families) + Reader themes (`Light/Night/Sepia`), `EvoraBrandMark` (`EO` placeholder `CD-06`), `DocCard`, `FormatFilterChip`, `MainBottomBar` + `56dp` Scan FAB / Tablet `NavigationRail`, `PageIndicatorPill`, `ShimmerSkeleton`, and `48dp` minimum hit-target wrappers. | UI System | `US-112` (`P0`) | `S-06`, All | `L` / `8` | `ui-designer` + `mobile-app-builder` | `T-003` | `75/75` contrast tokens match `05_DESIGN_SYSTEM.md`; Compose Preview tests pass across Compact (`390dp`) & Expanded (`880dp`) in Light/Dark & `2.0x` font scale. |
| `T-005` | **[DONE]** **Enriched Room Database (`EvoraOfficeDatabase`) & `DataStore` Preferences:** Implement `5` Room entities (`DocumentMetaEntity`, `TrashEntity`, `DraftRecoveryEntity`, `SignatureEntity`, `CloudAccountEntity`) + DAOs + `WorkManager` 30-day Trash auto-purge worker + Proto/Encrypted `DataStore` for locale, theme, and local `ProEntitlementPort` stub (`CD-07`). | `F-02` / `F-04` | `US-011`, `US-032`, `US-100` | `S-07`, `S-14`, `S-20` | `M` / `5` | `mobile-app-builder` | `T-003` | Room schema export + DAO instrumentation/unit tests pass; `DraftRecoveryEntity` & `TrashEntity` round-trip verified. |
| `T-006` | **[DONE]** **Cold-Start Intent Dispatcher, `<activity-alias>` Manifest & Onboarding Funnel:** Implement `SplashRoute`, `LanguageSelectRoute` (`10` locales), `OnboardingIntroRoute` (`3` slides + Skip), `PaywallRoute` wireframe shell (skippable `X`, `ProEntitlementPort` offline toggle per `CD-07`), `StoragePermissionRoute` (`MANAGE_EXTERNAL_STORAGE` + SAF fallback), and OS `<activity-alias>` entry points (`PDF/Word/Excel/PPT/HWP/Text/Scan/Sign/OCR/External`) with magic-byte MIME sniffer (`content://`/`file://` safe stream cache). | `F-01` | `US-001`, `US-003`, `US-004`, `US-005` | `S-01`–`S-05` | `L` / `8` | `mobile-app-builder` | `T-004`, `T-005` | Opening external `.docx` renamed `.pdf` resolves magic bytes cleanly in `< 1.5s`; denying `MANAGE_EXTERNAL_STORAGE` lands cleanly on Home w/ SAF CTA. |
| `T-007` | **[DONE]** **Fast MediaStore File Indexer, Home Library, Search & Recent/Favorites/Trash:** Implement `MediaStore` + incremental cache indexer (`< 800ms` / 1,000 docs), `HomeDocumentsRoute` (9 format chips, 4 sort criteria, `"Set as Default"` prompt, 3-dot `FileContextMenu`: Rename/Share/Move/Copy/Delete-to-Trash/Shortcut/Convert/Properties), `SearchDocumentsRoute` (300ms debounce + highlight), and `RecentBookmarksRoute` (`Recent` w/ `Page x/y`, `Favorites`, `Trash` w/ Restore & Empty Trash). | `F-02` | `US-010`, `US-011`, `US-012`, `US-013` | `S-06`, `S-07`, `S-08` | `L` / `8` | `mobile-app-builder` | `T-005`, `T-006` | Turbine VM tests verify chip filtering, rename extension guard, soft Trash restore to `originalPath`, and search debounce. |
| `T-008` | **[DONE]** **Universal Reader Core (`PdfiumAndroid`, Comfort Modes, TTS & Password Modal):** Implement `DocumentReaderRoute` with `PdfiumAndroid` tile/page rendering (`< 350ms` first page), outline/TOC drawer, in-doc text search + highlights, draggable right-edge `PageIndicatorPill`, persistent reading-position memory (`lastPageIndex`, `scrollOffsetY`, `zoom`), **real Continuous Vertical Scroll vs Single-Page Snap**, **Light / Night / Sepia** themes, **Reflow View**, **Auto-Scroll (`1..3`)**, **Android `TextToSpeech` Read-Aloud**, and inline Password Unlock Dialog. | `F-03` | `US-020`, `US-021`, `US-022` | `S-13` | `XL` / `8` | `mobile-app-builder` | `T-004`, `T-005` | All reading controls functional (zero stubs); reopening closed PDF restores exact page/offset/zoom; wrong password displays inline error cleanly. |
| `T-009` | **[DONE]** **Offline Hangul (`HWP/HWPX`), EPUB/TXT/CSV Readers & Pure-Java `ZIP/7z` Inspector:** Integrate `kr.dogfoot:hwplib` (`1.1.4`) + `hwpxlib` (`1.0.1`) -> background HTML/CSS/SVG sanitizer rendered in network-disabled `WebView` (`allowFileAccessFromFileURLs = false`, `> 50 MB` banner, password prompt, `Print`/`PDF` export); implement `EPUB`/`TXT`/`CSV`/`MD` readers; build `Apache Commons Compress 1.27.1` + `XZ` `.zip`/`.7z` entry browser/extractor (password prompt + honest `.rar` info dialog). | `F-03`, `F-08` | `US-022`, `US-023`, `US-024` | `S-10`, `S-13` | `L` / `8` | `mobile-app-builder` | `T-005`, `T-008` | Sample `.hwp`/`.hwpx` renders offline (Airplane mode verified); `.zip`/`.7z` extracts encrypted & normal entries cleanly; `.rar` displays clean unsupported notice. |

---

## 3. Sprint 2 Backlog — PDF Power Suite, Scanner/OCR & Rich Office Editors (`92` pts)

*Note: Sprint 2 delivers full PDF editing/signing/power tools, Camera/ID Card/OCR/QR scanning, and integrates the `R-01` LibreOfficeKit `16 KB` `.so` artifact into Word/Excel/PowerPoint editors.*

| Task ID | Title & Actionable Engineering Scope | Domain | User Stories | Screens | Size / Pts | Owner | Dependencies | Verification / Acceptance Hook |
|---|---|---|---|---|---|---|---|---|
| `T-010` | **[DONE]** **PDF Annotations (`PDFBox-Android`) & Handwritten e-Sign Canvas:** Implement `PdfEditorRoute` (`4` tabs: `Annotate`, `Signature`, `File`, `Page`) over `PdfiumAndroid` viewport: Highlight (`4` swatches + custom), Underline, Strikethrough, Freehand Ink Draw (width/color/eraser), Sticky Notes w/ Author metadata, incremental `PDFBox-Android` save; implement `SignaturePadRoute` Compose vector canvas -> `SignatureEntity` transparent PNGs + draggable/resizable PDF placement. | `F-05` | `US-050`, `US-051` | `S-16` | `XL` / `13` | `mobile-app-builder` | `T-008` | Saved annotations & e-Signatures render accurately in external desktop PDF viewers; `SignatureEntity` persists across launches. |
| `T-011` | **[DONE]** **PKCS#7 Digital Certificate Signer/Inspector & AES PDF Password Security:** Implement `BouncyCastle` + `PDFBox-Android` PKCS#7 inspection modal (`Issuer`, `Valid From/To`, `Trust Status`) and `.p12`/`.pfx` local certificate signing flow; build `Set Password` (`AES-128/256`) and `Remove Password` flows inside `PdfToolConfigRoute` & Editor `File` tab. | `F-05` | `US-051`, `US-052` | `S-16`, `S-17` | `L` / `8` | `mobile-app-builder` | `T-010` | `.p12` signed PDF passes cryptographic signature inspection; locked PDF enforces password in `DocumentReaderRoute` and unlocks cleanly when removed. |
| `T-012` | **[DONE]** **`ToolsHubRoute` & Real PDF Power Tools (`Merge`, `Split`, `Compress`, `Watermark`, `Print`):** Build `ToolsHubRoute` (`Convert`, `PDF Power`, `Utilities`, `Pro` crown badges wired to upfront `RewardGateSheet` contract per `CD-07`) and `PdfToolConfigRoute` + `ConvertProgressRoute` implementing: Merge PDF (drag-reorder, free `<= 3` files), **Real Split PDF** (`PDFBox Splitter`: range `"1-3, 5"`, every N, visual select), **Real Compress PDF** (Low/Med/High DPI + size-saved badge), **Real Watermark PDF** (text/size/color/opacity/`-45°`/tiled), and `Android PrintManager` in both Tools & Editors. | `F-06` | `US-060`, `US-061`, `US-062`, `US-063`, `US-110` | `S-04`, `S-12`, `S-17` | `XL` / `13` | `mobile-app-builder` | `T-004`, `T-008` | Zero `"Coming soon"` stubs; Merge/Split/Compress/Watermark/Print produce valid files + accurate Before/After size metrics; `RewardGateSheet` triggers **before** Pro jobs only. |
| `T-013` | **[DONE]** **Offline Converters (`Image/Text/Docs/HWP -> PDF`, `PDF -> Images`, `PDF -> DOCX`):** Implement `Image->PDF` (reorder, A4/Letter/Fit, margins), `Text->PDF`, `Office/ODF/HWP -> PDF` (via LOK headless / `WebView` print adapter), `PDF -> Long Image / PNGs`, and **100% Offline `PDF -> DOCX`** (`LOK sdext/pdfimport` + `PDFBox` paragraph/table extraction + `ML Kit` OCR fallback for scanned PDFs). | `F-06` | `US-063` | `S-12`, `S-17` | `L` / `8` | `mobile-app-builder` | `T-002`, `T-009`, `T-012` | Digital & scanned sample PDFs convert offline (Airplane mode) into valid editable `.docx`; multi-image PDF export matches page margins. |
| `T-014` | **[DONE]** **Document Scanner, Real Dual-Side ID Card Mode, 5-Script OCR & QR Scanner:** Build `ScannerFlowRoute` using `ML Kit Document Scanner` + `CameraX` (`Document`, **`ID Card`**, `OCR`, `QR/Barcode`): perspective corner crop, rotation, filters (`Original/Magic/Gray/B&W`), multi-page PDF/PNG export; **2-Step Dual-Side ID Card (`Front -> Back -> centered A4 PDF/PNG`)**; **5-Script Offline OCR** (`Latin`, `Chinese`, `Devanagari`, `Japanese`, `Korean`) w/ Copy / `.txt` / `.docx`; QR/Barcode typed action cards (`URL`, `Wi-Fi`, `vCard`, `Email`, `Phone`, `Text`). | `F-07` | `US-070`, `US-071`, `US-072` | `S-18` | `XL` / `13` | `mobile-app-builder` | `T-004`, `T-013` | ID Card mode composites Front+Back onto single A4 PDF cleanly; all 5 OCR scripts extract text offline; QR decodes Wi-Fi/vCard safely. |
| `T-015` | **[JNI BRIDGE / OOXML FALLBACK / DRAFTS / TEMPLATES DONE — Awaiting `R-01` `.so`]** **LibreOfficeKit (`16 KB`) JNI Bridge, Atomic Draft Recovery & Offline Templates:** Package `R-01` `libsofficeapp.so` into `:core:engine-office`, build clean JNI/Kotlin tile buffer + IME keyboard + undo/redo + native OOXML/ODF serializer; implement **`15s` / `onStop` atomic checkpointing** into `filesDir/drafts/` + `DraftRecoveryEntity` recovery dialog; bundle offline `assets/templates/` (`8` Word/Excel/PPT starter presets) in `TemplatesSheet`. **Enforce zero `SAVE_HEADER` lock.** | `F-04`, `F-08` | `US-030`, `US-032`, `US-081`, `US-110` | `S-09`, `S-14` | `XL` / `13` | `backend-architect` + `mobile-app-builder` | `T-002`, `T-005` | JNI bridge opens/edits/saves `.docx`/`.xlsx`/`.pptx`/`.odt` cleanly; killing app process mid-edit prompts *"Restore Unsaved Changes"* on relaunch; Save completes with zero paywall. |
| `T-016` | **[DONE]** **Word Editor UI (`OfficeEditorRoute` — `7` Tabs incl. Real `Review` / Track Changes):** Implement Word Editor (`S-14a`) over LOK canvas: top bar (`Undo/Redo`, `Done`), `Draft Auto-saved` badge, 2-row quick format bar + `7` collapsible tabs (`Edit`, `File`, `Insert`, **`Review` [Track Changes toggle, Add/View Comments, Accept/Reject]**, `Page`, `Draw`, `Find`) with `>= 48dp` targets & `220ms` drawer motion. | `F-04` | `US-030`, `US-031`, `US-032` | `S-14a` | `L` / `8` | `mobile-app-builder` | `T-015` | `.docx` `<w:ins>`/`<w:del>`/`<w:comment>` toggles & persists cleanly on save; `Review` tab verified functional (beating competitor stub). |
| `T-017` | **[DONE]** **Excel Editor (`5` Tabs + `fx` Formula Bar) & PPT Editor + `SlideShowRoute`:** Implement Excel mode (`S-14b`: `fx` formula bar, multi-sheet switcher `+`/rename/delete, `File/Edit/Insert/Formulas/Find` tabs, `SUM/AVERAGE/COUNT/MIN/MAX/IF`, merge cells, 9-way align, number formats, freeze panes, row/col sizing, chart render) and PPT mode (`S-14c`: slide strip, `7` tabs incl. `Format` Fill/Stroke/Z-order, presenter notes, plus fullscreen **`SlideShowRoute`** `S-15` w/ timer & laser pointer ink). | `F-04` | `US-040`, `US-041` | `S-14b`, `S-14c`, `S-15` | `XL` / `16` *(split `8` Excel + `8` PPT/SlideShow in Phase 6)* | `mobile-app-builder` | `T-015` | Excel formulas evaluate live & persist on save; PPT Z-order & SlideShow laser ink run at `60fps` across Portrait/Landscape. |

---

## 4. Sprint 3 Backlog — Storage, Wi-Fi Transfer, Cloud, Widgets & Core Lock (`48` pts)

*Note: Completing Sprint 3 marks **100% Core Feature Completion (`F-01`–`F-09 = Done`)**, satisfying Charter `CD-07` to unlock Monetization (`F-10`).*

| Task ID | Title & Actionable Engineering Scope | Domain | User Stories | Screens | Size / Pts | Owner | Dependencies | Verification / Acceptance Hook |
|---|---|---|---|---|---|---|---|---|
| `T-018` | **Storage Explorer (`StorageBrowserRoute`) & Batch Operations:** Implement `Internal / Downloads / SD Card` directory traversal + breadcrumb trail + SAF tree bridge for non-All-Files users, `+ New Folder`, and long-press multi-select sticky bar (`Move`, `Copy`, `Delete to Trash`, `Share`, `Merge PDF`) with synchronous Room + `MediaStore` updates. | `F-08` | `US-080` | `S-10` | `L` / `8` | `mobile-app-builder` | `T-007`, `T-009` | Batch Move/Copy/Delete reflects accurately on disk & Library without orphaned DB entries; Tablet two-pane tree + file list renders cleanly. |
| `T-019` | **Wi-Fi Web & FTP Transfer (`Ktor CIO` `dataSync` Service + `TileService`):** Implement `WifiTransferRoute`, `Ktor CIO` HTTP server serving offline drag-and-drop web UI (`assets/wifi_web/index.html`, path traversal validation, streaming upload to `Download/EvoraOffice/`), embedded FTP/FTPS server (`port 8998`, Read-Only + PIN lock), persistent stop notification, `10:00` idle auto-stop timer, QR URL display, and Quick Settings `TileService`. | `F-08` | `US-090` | `S-19` | `XL` / `13` | `backend-architect` + `mobile-app-builder` | `T-004`, `T-018` | PC browser uploads/downloads `.pdf`/`.docx` over LAN Wi-Fi cleanly; path-traversal `../` probes rejected with `403`; idle timer stops service at `10m`. |
| `T-020` | **Google Drive (`REST v3`) & Microsoft OneDrive (`MSAL + Graph SDK`):** Implement `CloudDriveBrowserRoute` supporting Google Credential Manager / OAuth (`DRIVE_FILE`) and Microsoft `MSAL` multi-account auth, encrypted token alias storage via `Android Keystore`, paginated remote folders + search, resumable download (`MediaHttpDownloader`), chunked upload (`LargeFileUploadTask` `> 4 MB`), and clean account disconnect (zero Dropbox stub). | `F-09` | `US-100` | `S-11` | `XL` / `13` | `mobile-app-builder` | `T-005`, `T-018` | Both providers authenticate, browse folders, open file into Reader/Editor, upload modified file, and wipe tokens cleanly on disconnect. |
| `T-021` | **Jetpack Glance Home Widgets (`W-01`, `W-02`) & App Shortcuts:** Implement `QuickBarGlanceWidget` (`4×1`: Search pill, Scan, New doc) and `RecentDocsGlanceWidget` (`4×2`: Scan + top 3 recent docs w/ format badge & progress), Material adaptive theming, Room `Recent` update observer refresh, and `ShortcutManagerCompat` pin-to-home support. | `F-02` | `US-091`, `US-013` | `W-01`, `W-02` | `M` / `5` | `mobile-app-builder` | `T-006`, `T-007` | Launcher widgets deep-link straight to `SearchDocumentsRoute`, `ScannerFlowRoute`, `TemplatesSheet`, or target `DocumentReaderRoute` without blocking ads. |
| `T-022` | **Core `SettingsRoute`, Localization (`10` Languages) & `F-01`–`F-09` Core Exit Gate:** Implement `SettingsRoute` (`System/Light/Dark` theme switch, `10`-language `strings.xml` + RTL audit, Default Handler guide, Auto-Save interval, Open-Source Licenses [`MPLv2`/`LGPLv3`/`Apache 2.0`] + LOK build disclosure link), run full `code-reviewer` + unit/UI sweep across `F-01`–`F-09`. | `F-01`–`F-09` | `US-001`, `US-112` | `S-20`, All | `L` / `9` | `mobile-app-builder` + `code-reviewer` | `T-016`–`T-021` | **MILESTONE GATE (`CD-07`):** `F-01`–`F-09` verified `Done` across all `10` locales, Light/Dark, Phone/Tablet; zero `AdMob` calls wired yet. |

---

## 5. Sprint 4 Backlog — Hybrid Monetization (`CD-07`), Perf, QA Gate 3 & Release (`54` pts)

*Note: Sprint 4 begins strictly after `T-022` confirms `F-01`–`F-09 = Done`. Covers `F-10`, Performance/Baseline Profiles, Phase 7 QA & Security (`08_QA_TEST_REPORT.md`), and Phase 8 ASO/Release (`09_ASO_METADATA.md`, `10_RELEASE_CHECKLIST.md`).*

| Task ID | Title & Actionable Engineering Scope | Domain | User Stories | Screens | Size / Pts | Owner | Dependencies | Verification / Acceptance Hook |
|---|---|---|---|---|---|---|---|---|
| `T-023` | **Google Play Billing `8.0.0`, `PaywallRoute`, `RewardGateSheet` & Restore:** Wire real `BillingClient 8.0.0` behind `ProEntitlementPort` (`e05_weekly_trial`, `e05_yearly_pro_3999`, `e05_lifetime_pro`), signed local offline entitlement cache, skippable Onboarding Paywall, pre-action `RewardGateSheet` (24h pass vs Pro), and `SettingsRoute -> Restore Purchases`. | `F-10` | `US-110`, `US-111` | `S-04`, `S-20` | `L` / `8` | `mobile-app-builder` | `T-022` (`CD-07`) | Billing test tracks (`Play License Testing`) activate `ProStatus`, hide all ad slots globally, and restore cleanly across reinstall. |
| `T-024` | **Google UMP CMP, Remote Config, Analytics (`ad_impression`) & AdMob Mediation:** Initialize `Google UMP SDK` prior to Mobile Ads SDK (EEA/UK/CH revocation in `SettingsRoute`), wire plaintext `Firebase Remote Config` keys (`03_PRD.md` §6), Firebase Analytics + Adjust `ad_impression`, and **AdMob Mediation** (`Open Bidding` + `AppLovin/Unity/Mintegral/CAS` — **zero `Pangle` or lockscreen SDKs**). Enforce cooldown caps (`>= 60s` post-milestone Interstitial, `>= 90s` cold/home App-Open, `index >= 6` inline native, **0 editor/scanner `onResume` ads, 0 `SAVE_HEADER` locks**). Audit all mediation `.so` files for `16 KB` (`0x4000`). | `F-10` | `US-002`, `US-110` | `S-01`, `S-04`, `S-06`, `S-17`, `S-20` | `XL` / `13` | `mobile-app-builder` + `appsec-engineer` | `T-023` | EEA VPN UMP consent test passes; `0` ads fire during `ACTION_VIEW` or editor `onResume`; all bundled `.so` pass `llvm-readelf -l` `0x4000`. |
| `T-025` | **Baseline Profiles, Macrobenchmark (`< 1.2s` Cold Start) & R8 Release Hardening:** Generate `androidx.baselineprofile` startup/scroll profile, verify cold start `< 1.2s` & Compose 60/120fps scrolling on `SM_A165F`, strip debug loggers (`0 Chucker`), verify `R8` minification + signed Release `.aab` ABI splits & size budget (`R-02`). | Perf / `R-02` | `US-004`, `US-112` | All | `M` / `5` | `mobile-app-builder` + `backend-architect` | `T-024` | Release `.aab` builds cleanly; Macrobenchmark median cold start `< 1.2s`; `jadx`/APK check confirms zero debug HTTP interceptors or test keys. |
| `T-026` | **Phase 7 QA Test Matrix, Interruption Stress & OWASP Security Audit (`08_QA_TEST_REPORT.md`):** Execute unit/UI suites + physical device (`SM_A165F` / `RF8Y60B9NCZ`) end-to-end verification across all `17` formats (`PDF`, `DOC/DOCX`, `XLS/XLSX`, `PPT/PPTX`, `ODT/ODS/ODP`, `HWP/HWPX`, `EPUB`, `TXT/CSV`, `ZIP/7z`), process death (`DraftRecovery`), airplane-mode offline proofs, permission denials, TalkBack/Dynamic Type `2.0x`, OWASP Mobile Top 10 audit, and `16 KB` `.aab` gate. Produce `docs/08_QA_TEST_REPORT.md` for **CEO Gate 3 (Zero Blocker)**. | Phase 7 QA | All `US-001`–`US-112` | All `S-01`–`S-20`, `W-01`–`W-02` | `XL` / `16` | `tester` + `evidence-collector` + `appsec-engineer` | `T-025` | `docs/08_QA_TEST_REPORT.md` reports **ZERO Blockers (`P0`)** with ADB test evidence; CEO approves **Gate 3**. |
| `T-027` | **Phase 8 ASO Metadata, Compliance Video & Play Release Kit (`09_ASO_METADATA.md`, `10_RELEASE_CHECKLIST.md`):** Author localized Store Titles, Short/Full descriptions (`EN`, `VI`, + target markets), keyword optimization, screenshot storyline + visual specs, English `MANAGE_EXTERNAL_STORAGE` compliance screencast script/capture, Google Play Data Safety audit (Drive/OneDrive/AdMob/Firebase/Adjust), `MPLv2` attribution package, and Fastlane/Play upload checklist (`docs/09_ASO_METADATA.md`, `docs/10_RELEASE_CHECKLIST.md`). | Phase 8 Release | `US-001`–`US-112` | Store Kit | `L` / `12` | `app-store-optimizer` + `mobile-release-engineer` | `T-026` | `docs/09_ASO_METADATA.md` & `docs/10_RELEASE_CHECKLIST.md` complete; signed `.aab` ready for Play Console submission. |

---

## 6. 100% Traceability Matrix (Stories & Screens -> Backlog Tasks)

### 6.1 User Story Coverage (`37 / 37` Stories Mapped = `100%`)

| Domain Bucket | User Story IDs (`docs/04_USER_STORIES.md`) | Mapped Sprint Tasks | Sprint |
|---|---|---|---|
| **2.1 Onboarding, Consent & Intents** | `US-001`, `US-002`, `US-003`, `US-004`, `US-005` | `T-006`, `T-022`, `T-024`, `T-026` | S1, S3, S4 |
| **2.2 Library, Search, Trash & Widgets** | `US-010`, `US-011`, `US-012`, `US-013` | `T-005`, `T-007`, `T-021`, `T-026` | S1, S3, S4 |
| **2.3 Universal Reader & Archives** | `US-020`, `US-021`, `US-022`, `US-023`, `US-024` | `T-008`, `T-009`, `T-026` | S1, S4 |
| **2.4 Word Editor & Draft Recovery** | `US-030`, `US-031`, `US-032` | `T-002`, `T-005`, `T-015`, `T-016`, `T-026` | S1, S2, S4 |
| **2.5 Excel & PowerPoint / SlideShow** | `US-040`, `US-041` | `T-002`, `T-015`, `T-017`, `T-026` | S1, S2, S4 |
| **2.6 PDF Annotate, Sign, PKCS#7 & AES** | `US-050`, `US-051`, `US-052` | `T-010`, `T-011`, `T-026` | S2, S4 |
| **2.7 PDF Power Tools & Converters** | `US-060`, `US-061`, `US-062`, `US-063` | `T-012`, `T-013`, `T-026` | S2, S4 |
| **2.8 Scanner, ID Card, OCR & QR** | `US-070`, `US-071`, `US-072` | `T-014`, `T-026` | S2, S4 |
| **2.9 Storage Explorer & Templates** | `US-080`, `US-081` | `T-015`, `T-018`, `T-026` | S2, S3, S4 |
| **2.10 Wi-Fi Transfer & Glance Widgets** | `US-090`, `US-091` | `T-019`, `T-021`, `T-026` | S3, S4 |
| **2.11 Cloud Drives (Drive & OneDrive)** | `US-100` | `T-005`, `T-020`, `T-026` | S1, S3, S4 |
| **2.12 Monetization, Accessibility & Safety** | `US-110`, `US-111`, `US-112` | `T-003`, `T-004`, `T-012`, `T-015`, `T-022`, `T-023`, `T-024`, `T-025`, `T-026` | S1–S4 |

### 6.2 Design Screen & Widget Coverage (`21` Routes + `2` Glance Widgets = `100%`)

| Screen / Widget ID | Route / Component (`05_DESIGN_SYSTEM.md`) | Implementing Tasks | Verification Sprint |
|---|---|---|---|
| `S-01`–`S-03`, `S-05` | `SplashRoute`, `LanguageSelectRoute`, `OnboardingIntroRoute`, `StoragePermissionRoute` | `T-006`, `T-024` (UMP) | Sprint 1 / 4 |
| `S-04` | `PaywallRoute` & `RewardGateSheet` | `T-006` (shell), `T-012` (pre-action trigger), `T-023` (Billing) | Sprint 1 / 2 / 4 |
| `S-06`–`S-08` | `MainShell` + `HomeDocumentsRoute`, `RecentBookmarksRoute`, `SearchDocumentsRoute` | `T-004`, `T-007` | Sprint 1 |
| `S-09` | `TemplatesSheet` | `T-015` | Sprint 2 |
| `S-10`, `S-11` | `StorageBrowserRoute` (+ `ZIP/7z`), `CloudDriveBrowserRoute` | `T-009` (`ZIP/7z`), `T-018` (Storage), `T-020` (Cloud) | Sprint 1 / 3 |
| `S-12`, `S-17` | `ToolsHubRoute`, `PdfToolConfigRoute`, `ConvertProgressRoute` | `T-012`, `T-013` | Sprint 2 |
| `S-13` | `DocumentReaderRoute` (`PDF`, `HWP`, `EPUB`, `TXT/CSV`) | `T-008`, `T-009` | Sprint 1 |
| `S-14a`, `S-14b`, `S-14c`, `S-15` | `OfficeEditorRoute` (Word/Excel/PPT) & `SlideShowRoute` | `T-002`, `T-015`, `T-016`, `T-017` | Sprint 1 / 2 |
| `S-16` | `PdfEditorRoute`, `SignaturePadRoute` & PKCS#7 Inspector | `T-010`, `T-011` | Sprint 2 |
| `S-18` | `ScannerFlowRoute` (`Doc`, `Dual ID Card`, `OCR`, `QR`) | `T-014` | Sprint 2 |
| `S-19` | `WifiTransferRoute` (`Ktor CIO` Web + FTP + `TileService`) | `T-019` | Sprint 3 |
| `S-20` | `SettingsRoute` (Theme, Language, UMP, Restore, OSS Licenses) | `T-022`, `T-023`, `T-024` | Sprint 3 / 4 |
| `W-01`, `W-02` | Android Glance Widgets (`Quick Bar 4×1` & `Recent List 4×2`) | `T-021` | Sprint 3 |

---

## 7. Strict Definition of Done (DoD)

A task in `Sprint 1`–`Sprint 4` moves to **`Done`** **ONLY** when **100% of applicable criteria below pass**:

1. **MVI & Clean Architecture Compliance (`.gemini/rules/android-native-standards.md`):**
   - Every screen ViewModel extends `MviViewModel<S, I, E>` with `onIntent` as its **sole** public method.
   - `State`, `Intent`, and `Effect` live strictly in `XContract.kt`; navigation is emitted strictly as a `UiEffect` via `Channel(BUFFERED)` + `CollectEffects`.
   - Zero `androidx.compose.*`, `android.*`, or platform SDK imports inside ViewModels, Contracts, or `:domain` classes.
   - All coroutines use `launchSafely` (always rethrowing `CancellationException` and clearing loading/busy flags in `onError`).
   - `Route` / `Screen` separation enforced (`XRoute` connects DI/VM; `XScreen` is stateless & `@Preview`-able).
   - [`LLM.md`](../LLM.md) updated in the same commit whenever modules, packages, DI bindings, navigation routes, or deviations change.
2. **Design System, Accessibility & Responsiveness (`docs/05_DESIGN_SYSTEM.md`):**
   - Exact Light/Dark Material 3 tokens (`dynamicColor = false`), `Poppins` (`tnum` on counters/coordinates), and `>= 48dp` interactive touch targets (`minimumInteractiveComponentSize()`).
   - Verified `>= 4.5:1` text contrast & `>= 3.0:1` UI graphics contrast; zero `10sp` labels (`min 11sp`).
   - All `4` mandatory MVI screen states rendered cleanly (`Loading` shimmer, `Empty` vector + CTA, `Error/SAF`, `Content`) plus `Compact`/`Medium`/`Expanded` (`WindowSizeClass`) layout checks.
   - TalkBack `contentDescription` on interactive icons (`null` on decorative vectors); `2.0x` font scale non-clipping; Reduced Motion fallback respected.
3. **Code Quality, Static Checks & Automated Tests:**
   - Zero `./gradlew lint`, `ktlint`, or `detekt` errors.
   - JVM Unit Tests pass for every new ViewModel (using **Turbine + `kotlinx-coroutines-test`** + readable fakes) and every Domain policy/converter.
   - Code review (`code-reviewer`) resolves all `Blocker 🔴` findings before merge.
4. **Binary, Security & Anti-Churn Compliance:**
   - Every native `.so` bundled in APK/AAB passes `llvm-readelf -l` (`ELF64 PT_LOAD p_align == 0x4000`, `16 KB` page alignment).
   - Zero `AGPLv3` code/binaries, zero files/assets/endpoints copied from `.local/`, zero hardcoded keys/tokens, zero `Chucker` in release builds, zero `SYSTEM_ALERT_WINDOW` / lockscreen activities.
   - Zero `"Coming soon"` placeholders, zero `SAVE_HEADER` save locks, and zero `onResume` ads in editors/scanner.

---

## 8. Workforce RACI Matrix

**Legend:** **R** = Responsible (executes work) · **A** = Accountable (final sign-off / gate owner) · **C** = Consulted (reviews/advises) · **I** = Informed

| Deliverable / Lifecycle Activity | CEO | `chief-of-staff` | `project-manager-senior` / `sprint-prioritizer` | `backend-architect` | `mobile-app-builder` | `ux-architect` / `ui-designer` | `code-reviewer` | `tester` / `evidence-collector` | `appsec-engineer` | `app-store-optimizer` / `mobile-release-engineer` |
|---|---|---|---|---|---|---|---|---|---|---|
| **Gate 1 & Project Charter (`02_PROJECT_CHARTER.md`)** | **A** | **R** | I | C | I | I | I | I | C | I |
| **Gate 2 Design System & Wireframes (`05_DESIGN_SYSTEM.md`)** | **A** | C | I | I | C | **R** | I | C | I | I |
| **Phase 5 Sprint Backlog (`06_SPRINT_BACKLOG.md`)** | I | **A** | **R** | C | C | C | I | C | C | C |
| **`07_TECH_SPEC.md`, `LLM.md` §2–§10 & `R-01` LOK 16KB CI** | I | I | I | **R / A** | C | I | C | I | C | I |
| **Sprints 1–3 Core Engineering (`F-01`–`F-09`)** | I | I | **A** *(tracking)* | C | **R** | C | C *(PR gate)* | C | C | I |
| **Sprint 4 Monetization (`F-10` per `CD-07`) & Perf/R8** | I | C | **A** | C | **R** | C | C | C | C | I |
| **Code Review Gate (`Blocker 🔴` Zero Tolerance)** | I | I | I | C | C | I | **R / A** | I | C | I |
| **Phase 7 QA Matrix, Device ADB Tests & OWASP (`08_QA_TEST_REPORT.md`)** | I | C | I | C | C *(fixes)* | C | I | **R** | **R** *(Security)* | I |
| **Gate 3 Quality Sign-Off (Zero Blocker)** | **A** | **C** | C | I | I | I | I | **R** | **R** | I |
| **Phase 8 ASO, Play Data Safety & Release `.aab` (`09`, `10`)** | **A** *(submit)* | C | I | I | C | C | I | C | C | **R** |

---

## 9. Risk Execution Triggers & Immediate Phase 6 Activation

### 9.1 Active Sprint Risk Watchlist
1. **`R-01` (`T-002` — Critical Path):** Run the GitHub Actions Linux `LibreOfficeKit CPAndroid` ARM64 `16 KB` cross-compile immediately alongside `T-001` so prebuilt `libsofficeapp.so` arrives **before** `T-015` begins in Sprint 2. Any C++ compiler/NDK snag surfaces on Day 1–2 rather than blocking Week 3.
2. **`R-02` (`T-025` — AAB Size Budget):** Monitor ABI-split `.aab` download size after `LibreOfficeKit` + `Pdfium` + `ML Kit`; keep optional ML Kit OCR language packs on-demand where supported by Play Services.
3. **`R-04` (`T-009`, `T-013` — Offline HWP & Scanned PDF->DOCX):** Build a local test fixture suite inside `.local/fixtures/` (gitignored) covering multi-table `.hwp`/`.hwpx`, encrypted `.pdf`, and scanned multi-column PDFs during Sprint 1–2.

### 9.2 Immediate Next Step: Phase 6 Kickoff Checklist
1. Execute **`T-001`** (`backend-architect` authors `docs/07_TECH_SPEC.md` with **Hilt + KSP** and populates `LLM.md` §2–§10).
2. Execute **`T-002`** (`R-01` GitHub Actions CI workflow & Linux ARM64 `16 KB` LOK build inside `Evora-Technologies-Global/E05-EvoraOffice-DOCX_PDF_XLSX` per `CD-12`).
3. Scaffold Gradle project (`T-003`) strictly after `LLM.md` §10 unlocks product file paths.
