# Product Requirements Document (PRD) — E05 Native Android Office & PDF Suite

**Project Code:** E05  
**Product Brand & Store Title:** `Evora Office` (`Evora Office: DOCX, PDF, XLSX` — `29/30` chars, locked per `CD-05`)  
**Application ID / Package Name:** `com.evora.technologies.office` *(Locked per `CD-05`)*  
**App Visual Style:** `Style B — Familiar Vibrant Utility` *(Locked per `CD-06`; App Logo/Icon deferred)*  
**Target Release:** `versionName = "1.0.0"`, `versionCode = 100` (Single Full-Parity Release)  
**Owners:** Product Owner & Business Analysis (`ba-writer`, `product-manager`)  
**Date:** 2026-11-25  
**Status:** Complete + Pre-Phase 4 Brand/Scope Lock Applied -> Unlocks Phase 4 UI/UX Design  
**Upstream Inputs:** [`01_MARKET_RESEARCH.md`](01_MARKET_RESEARCH.md) · [`01_COMPETITOR_REVERSE_ENGINEERING.md`](01_COMPETITOR_REVERSE_ENGINEERING.md) · [`02_PROJECT_CHARTER.md`](02_PROJECT_CHARTER.md)

### Pre-Phase 4 CEO Decision Overrides (`2026-11-25`, Charter `CD-05`–`CD-09`)

| Date | Decision ID | Locked Specification | Impact on PRD / Phase 4 |
|---|---|---|---|
| 2026-11-25 | `CD-05` | **Name & Package:** Launcher `Evora Office`; Play Title `Evora Office: DOCX, PDF, XLSX`; `applicationId = com.evora.technologies.office` | Replaces working placeholder `com.evora.office.document.reader` across manifest, deep links, and headers. |
| 2026-11-25 | `CD-06` | **Style B & Deferred Logo:** Familiar Vibrant Utility (`#2489FF` primary CTA, pastel tiles, dense utility layout + Compose M3 Light/Dark & WCAG AA `>= 48dp`); App Logo/Icon deferred to later milestone | Phase 4 `05_DESIGN_SYSTEM.md` & HTML wireframes adopt Style B with temporary monogram/wordmark header placeholder. |
| 2026-11-25 | `CD-07` | **Feature-First Ads Deferral & Paywall/Pro Boundaries:** Defer implementing active AdMob/Mediation ads until core features (`F-01`–`F-09`) are `Done`; keep Phase 4 Paywall (`Weekly $4.99/3d`, `Yearly $39.99/7d`, `Lifetime $59.99`, skippable onboarding trial) & `Crown` boundary (`Merge > 3`, `Compress High`, `Watermark`, `Batch OCR`, `Dual ID Card`, `PKCS#7`, `PDF->DOCX` w/ optional `1` free/day) | Phase 6 builds all core features cleanly without wiring ad SDK placements first; monetization integration follows feature completion. |
| 2026-11-25 | `CD-08` & `CD-09` | **HTML Gate 2 Wireframes + GitHub Actions LOK Build + Personal Play Account Ignored:** Interactive HTML/CSS/SVG wireframes for Gate 2; `LibreOfficeKit 16 KB` C++ spike on GitHub Actions Linux (`duylt-dev`); Personal Play account 14-day/12-tester gate ignored during engineering | Unblocks Phase 4 visual review and Week 1 R-01 CI build despite `12 GiB` local free disk. |

---

## 1. Overview

### 1.1 Product Vision & Charter Alignment
E05 is a clean-room, **100% offline (`$0` server COGS)** Native Android Office & PDF utility built strictly with **Kotlin + Jetpack Compose + MVI + Clean Architecture** (`minSdk 26`, `targetSdk 36`, `16 KB` ELF aligned). E05 replicates every real working capability of benchmark `com.officedocument.word.docx.document.viewer` in **`v1.0.0` (`versionCode 100`)**, turns the competitor's broken *"Coming Soon"* marketing stubs into genuinely engineered features, and removes the dark patterns driving competitor 1-star churn (`11.9%` EN / `17.1%` VN).

### 1.2 Core Engineering Stack (Locked Track A — `$0` Royalties, `$0` Server)
- **Office View + Editing (`DOC/DOCX/XLS/XLSX/PPT/PPTX/ODT/ODS/ODP`):** `LibreOfficeKit / Collabora Android Core` (`MPLv2/LGPLv3`, cross-compiled NDK r27+ `16 KB`).
- **PDF Viewing & Rendering:** `PdfiumAndroid` (`Apache 2.0`, NDK r27+ `16 KB`).
- **PDF Power Manipulation & Security:** `TomRoush/PdfBox-Android` (`Apache 2.0`, pure Java) + `BouncyCastle` + Compose Ink/e-Sign Canvas + `PrintManager`.
- **Korean Hangul (`HWP/HWPX`):** `kr.dogfoot:hwplib` (`1.1.4`, `Apache 2.0`) + `kr.dogfoot:hwpxlib` (`1.0.1`, `Apache 2.0`) -> offline HTML/CSS/SVG WebView bridge & LibreOfficeKit ODT/PDF export.
- **PDF -> DOCX Conversion:** Offline `LibreOfficeKit` Writer import (`sdext/pdfimport`) + `PDFBox-Android` layout extraction + on-device `ML Kit` OCR fallback for image PDFs.
- **Archive Extraction (`ZIP/7z`):** `org.apache.commons:commons-compress` (`1.27.1`, `Apache 2.0`) + `org.tukaani:xz`.
- **Scan, ID Card, OCR, QR:** `CameraX` + `Google ML Kit Document Scanner` + `ML Kit Text Recognition v2` (`5` script groups) + `ML Kit Barcode Scanner`.
- **Ecosystem & Transfer:** `Room`, `WorkManager`, embedded `Ktor CIO` Wi-Fi HTTP/FTP server, `Google Drive API v3`, `Microsoft OneDrive (MSAL + Graph SDK)`, `Jetpack Glance` Home Widgets.
- **Monetization & Compliance:** `Google UMP SDK` (CMP) + `AdMob Mediation` (Open Bidding + AppLovin MAX / Unity / Mintegral / CAS) + `Google Play Billing 8.0.0`.

### 1.3 Key Product Assumptions & Exclusions
1. **Clean-Room Rule:** Zero binaries, XML layouts, strings, icons, packages, or endpoints (`office-convert.begamob.com`) from `.local/` may enter E05.
2. **No `.rar` Support (`CD-04`):** Pure-Java `Commons Compress` supports `.zip` and `.7z` cleanly; `.rar` is excluded to respect proprietary UnRAR licensing.
3. **No Cloud AI Stubs (`CD-04`):** E05 ships zero Cloud LLM endpoints and zero fake *"Coming Soon"* AI buttons.
4. **Ethical Anti-Churn Guarantee:** Zero `SAVE_HEADER` edit-ransom locks, zero `onResume` interstitials inside active editors/scanners, and zero `SYSTEM_ALERT_WINDOW` / lockscreen overlay ads.

---

## 2. Release Scope (`1.0.0` / `versionCode 100`)

All functional domains ship together in `1.0.0` (`versionCode 100`) per amended Charter Decision `CD-02`:

| Module ID | Domain | Parity Status vs. Benchmark | Priority | Target Engine |
|---|---|---|---|---|
| `F-01` | Onboarding, Permissions (`MANAGE_EXTERNAL_STORAGE` / SAF), OS Intent Routing | Enhanced Parity (Zero darkscreen ads, clean SAF fallback) | `P0` | Compose + Manifest `activity-alias` |
| `F-02` | Home Library, Fast MediaStore/File Scanner, Search, Recent/Favorites/Trash, Glance Widgets | Enhanced Parity (Full reading position & metadata Room DB) | `P0` | Kotlin Coroutines + Room + Glance |
| `F-03` | Multi-Format Document Reader (`PDF`, Office, `ODF`, `HWP/HWPX`, `EPUB`, `TXT/CSV`, `ZIP/7z`) | **Exceeds Benchmark** (Real Continuous Scroll, Dark/Sepia themes, TTS, ZIP/7z preview) | `P0` | `PdfiumAndroid` + `LibreOfficeKit` + `hwplib` + WebView/Compose |
| `F-04` | Rich Office Editor (`Word`, `Excel`, `PowerPoint`) + Atomic Draft Recovery | **Exceeds Benchmark** (Real Track Changes toggle + Atomic Draft Recovery + Zero `SAVE_HEADER` lock) | `P0` | `LibreOfficeKit` (`MPLv2`) + Room Draft Ledger |
| `F-05` | PDF Annotate, e-Sign, PKCS#7 Cert Viewer/Signer & AES Password Encryption | Full Parity (`AES-128/256` + BouncyCastle + Compose Ink/e-Sign) | `P0` | `PdfiumAndroid` + `PDFBox-Android` + Compose Canvas |
| `F-06` | PDF Power Tools & Converters (`Merge`, `Split`, `Compress`, `Watermark`, `Print`, `Image/Text/Docs->PDF`, `PDF->DOCX`, `Extract Image`) | **Exceeds Benchmark** (Real Split, Compress, Watermark, Tools Print vs. competitor stubs) | `P0` | `PDFBox-Android` + `LibreOfficeKit` + `PrintManager` |
| `F-07` | Document Scanner, Dual-Side ID Card Mode, 5-Script OCR & QR/Barcode | **Exceeds Benchmark** (Real ID Card layout vs. competitor stub) | `P0` | `CameraX` + `ML Kit` Doc Scanner / Text Rec v2 / Barcode |
| `F-08` | Storage Browser, Pure-Java `ZIP/7z` Extractor, Blank/Preset Templates & Wi-Fi Web/FTP Transfer | **Exceeds Benchmark** (Real `.zip`/`.7z` extraction + browser drag-drop Web UI + FTPS) | `P0` | `Apache Commons Compress` + `Ktor CIO` |
| `F-09` | Cloud Integration (`Google Drive` & `OneDrive MSAL`) | Full Parity (Clean OAuth/MSAL browse, download, upload) | `P1` | `Drive v3` + `MSAL` + `Graph SDK` |
| `F-10` | Hybrid Monetization (`UMP CMP`, `AdMob Mediation`, `Play Billing 8.0.0`) & Settings | Enhanced Parity (Coherent frequency caps, localized 10+ languages) | `P0` | `UMP` + `AdMob Mediation` + `BillingClient 8` |

---

## 3. Feature Specifications

### `F-01`: Onboarding, Storage Permissions & OS File Intent Integration
| Property | Specification |
|---|---|
| **Entry & Cold Start** | Single `MainActivity` Compose host + dedicated OS `<activity-alias>` entry points (`PDF`, `Word`, `Excel`, `PPT`, `HWP`, `Text`, `Scan`, `Sign`, `OCR`, `External`) allowing granular "Open With" & launcher shortcut routing (`targetSdk 36`). Cold launch target `< 1.2s`. |
| **First-Launch Funnel** | Conditional flow controlled by Firebase Remote Config flags: Splash -> Language Selection (`EN`, `VI`, `ES`, `PT-BR`, `KO`, `JA`, `ID`, `HI`, `DE`, `FR`) -> 3-slide Onboarding Carousel (skippable) -> Optional Soft Trial Paywall (skippable) -> Storage Permission Guide -> Home. Returning users bypass straight to Home or targeted `ACTION_VIEW` file. |
| **Permission Strategy** | 1) Prompt `MANAGE_EXTERNAL_STORAGE` with clear core-functionality disclosure and Settings deep-link. 2) If denied or on restricted profiles, seamlessly degrade to **Storage Access Framework (`ACTION_OPEN_DOCUMENT` / `ACTION_OPEN_DOCUMENT_TREE`) + MediaStore Docs query** so user can immediately open/convert files without deadlocking the app. |
| **External Intent Handling** | Inspect incoming `content://` or `file://` URI, resolve MIME + magic bytes (detect `.docx` vs legacy `.doc` misnamed extensions), copy stream safely to app-private cache if read-only URI, and open target Reader/Editor immediately without blocking ads first. |

### `F-02`: Home Library, File Indexer, Recent / Favorites / Trash & Widgets
| Property | Specification |
|---|---|
| **Document Filtering & Sorting** | Top horizontal filter chips (`All`, `PDF`, `Word`, `Excel`, `PPT`, `ODF`, `HWP`, `TXT/CSV`, `ZIP/7z`). Multi-criteria sort (`Modified Date desc/asc`, `Name A-Z/Z-A`, `Size desc/asc`, `Format`). Real-time debounced keyword search (`300ms`) matching filename + parent folder. |
| **File Row Capabilities** | Displays format-coded icon badge, filename, file size, modified date, Favorite star toggle, "Recent" indicator, and contextual bottom sheet (`Open`, `Share`, `Rename`, `Move/Copy`, `Delete to Trash`, `Add Home Shortcut`, `Properties`, `Convert to PDF`). *(Matches `MyDocument` contract without abusive inline native ad frequency: max 1 native card after index >= 5 if non-Pro.)* |
| **Recent, Favorites & Soft Trash** | Backed by enriched Room entities: `Recent` tracks `lastOpenedAt`, `lastPage`, `scrollOffsetY`, `zoom`; `Trash` preserves `originalPath` and `trashedAt` (auto-purge after `30` days `[Internal Target]` or instant Restore/Permanent Delete). |
| **Home Screen Widgets (`Jetpack Glance`)** | Two Glance widgets matching competitor utility: **1) Quick Bar Widget** (Search + Document Scan + New Note/PDF shortcut) and **2) Recent Documents List Widget** (top 3–5 recent files launching directly into Reader/Editor). Pin app shortcut support via `ShortcutManagerCompat`. |

### `F-03`: Multi-Format Document Reader & Reading Comfort Modes
| Property | Specification |
|---|---|
| **Supported Formats (Read-Only / Universal Open)** | `PDF` (`pdf`, `wps`, `wpt`, `wpss`), `Word` (`doc`, `docx`, `dotx`, `docm`), `Excel` (`xls`, `xlsx`, `xltx`, `xlsm`), `PPT` (`ppt`, `pptx`, `pps`, `ppsx`, `pot`), `ODF` (`odt`, `ods`, `odp`, `odf`), `Hangul` (`hwp`, `hwpx`, `hwt`, `hwdt`), `Ebook` (`epub`), `Text/Data` (`txt`, `csv`, `log`, `md`), `Archive` (`zip`, `7z` directory browser + extract/open entry). |
| **Unified Reader Chrome** | Format-colored Top App Bar (`Back`, editable/readable Filename, `Outline/TOC`, `Search in Doc`, `Fullscreen`, `Pro` crown if free user, `Edit` mode toggle on editable docs). Draggable right-edge thumb pill showing `"page / total"` (`PageIndicatorPill`). Persistent Reading Memory restores exact page/offset/zoom on reopen. |
| **Reading Mode Controls (Replaces Competitor Stub)** | 1) **Continuous Scroll Toggle** (`Vertical Continuous` vs. `Single Page Snap` / horizontal). 2) **Orientation Toggle** (`Portrait` / `Landscape` adaptive two-pane on tablets). 3) **Reader Color Themes** (`Original Light`, `Night Inverted Dark`, `Sepia Comfort`). 4) **Reflow View** (word-wrap zoom for PDF & Word on mobile screens). 5) **Auto-Scroll Bar** (`isOn`, `speed 1..3`, `Play/Pause`). 6) **TTS Read-Aloud** (Android `TextToSpeech` reading current page/selection). |
| **Hangul (`HWP/HWPX`) Offline Pipeline** | Uses `kr.dogfoot:hwplib` (`1.1.4`) / `hwpxlib` (`1.0.1`) inside a background `Dispatchers.IO` parser to generate sanitized local HTML/CSS/SVG rendered in an internet-disabled sandboxed `WebView` (`allowFileAccessFromFileURLs = false`, JS off unless needed for pagination), with password-protected HWP prompt and local HTML/ODT->PDF print/export. Max offline file warning banner if `> 50 MB`. |

### `F-04`: Rich Office Editor (`Word`, `Excel`, `PowerPoint`) & Draft Recovery
| Property | Specification |
|---|---|
| **LibreOfficeKit Editing Engine (`MPLv2`)** | Single unified Kotlin JNI bridge managing `LibreOfficeKit` tile buffer rendering, cursor/selection handles, IME keyboard text input, undo/redo stack, and native OOXML/ODF save serialization. |
| **Word Editor (`DOC/DOCX/ODT`)** | Collapsible 2-row quick bar + tabbed bottom shelf (`Edit`, `File`, `Insert`, `Review`, `Page`, `Draw`, `Find`): Font family/size, Bold/Italic/Underline/Strikethrough, Text color & Highlight color, Cut/Copy/Paste, Undo/Redo, 4-way Alignment, Bullets/Numbering, Indent Left/Right, Insert Image (Gallery/Camera) & Shapes, Freehand Ink overlay, **Track Changes Toggle & Comments (`Review` tab — beating competitor stub)**, Save / Save As / Export PDF / Print / Share. |
| **Excel Editor (`XLS/XLSX/ODS`)** | Formula bar + 5 bottom tabs (`File`, `Edit`, `Insert`, `Formulas`, `Find`): Sheet switcher tabs (`+ Add Sheet`, Rename, Delete), Cell editing with Formula palette (`SUM`, `AVERAGE`, `COUNT`, `MIN`, `MAX`, `IF`, grouped functions), Merge/Unmerge cells, 9-way alignment, Number formatting (`General`, `Number`, `Currency`, `Accounting`, `Date`, `Percentage`, `Fraction`), Insert/Delete rows & columns, Freeze rows/columns/panes, Row height / Column width (cm/dp), Charts rendering. |
| **PowerPoint Editor (`PPT/PPTX/ODP`)** | Slide thumbnail strip + 7 bottom tabs (`Edit`, `File`, `Insert`, `Format`, `Annotate`, `Page`, `Find`): Slide add/duplicate/delete/reorder, Textbox & Image/Shape insert, Shape Fill/Stroke color, Stroke width/dash style, 4-way Z-order Arrange (`Bring Front`, `Send Back`, `Forward`, `Backward`), Presenter Notes drawer, **Fullscreen SlideShow Mode** (`SlideShow` route with swipe transitions & laser-pointer ink). |
| **Atomic Draft Recovery & Honest Save** | Every `15s` on dirty buffer (or `onStop`), dirty buffer snapshots cleanly into `filesDir/drafts/{hash}` and registers in Room `DraftRecoveryEntity`. Relaunch after crash offers `"Restore Unsaved Changes"` or `"Discard"`. **Critical Rule:** Standard `Save` and `Save As` on user-edited files are **never** hijacked by a Rewarded Ad dialog (`zero SAVE_HEADER locks`). |

### `F-05` & `F-06`: PDF Annotations, Signing, Encryption & Power Tools
| Tool / Feature | Detailed Functional Requirement |
|---|---|
| **PDF Annotations (`4 tabs`)** | `Annotate`, `Signature`, `File`, `Page`. Supports Highlight (`4` quick swatches + custom picker), Underline, Strikethrough, Freehand Ink Draw (`stroke width`, `color`, eraser), Sticky Text Note with Author metadata, saved non-destructive incrementally into PDF via `PDFBox-Android`. |
| **e-Sign & Digital Cert (`PKCS#7`)** | Compose Canvas freehand Signature creator (`DrawingPad`) with color/pen thickness & transparent PNG vector persistence (`SignatureEntity` reusable history). Embed signature stamps at movable/scalable coordinates on PDF pages. **PKCS#7 Digital Signature Inspector** verifies embedded certificates (`X.509` issuer/validity) and supports local `.p12`/`.pfx` signing (`BouncyCastle` + `PDFBox`). |
| **Merge & Real Split PDF** | Multi-file drag-to-reorder Merge PDF with custom output name. **Real Offline Split PDF** (`PDFBox` `Splitter`): modes for *By Page Range* (e.g., `1-3, 5, 8-10`), *Every N Pages*, or *Extract Selected Visual Pages*. |
| **Compress & Watermark PDF** | **Real Compress PDF:** Image downsampling DPI + JPEG recompression presets (`Low / Medium / High Compression`) preserving text crispness. **Real Watermark PDF:** Configurable watermark text, font size, color, opacity slider (`10%–100%`), rotation angle (`-45°` default), and single-center vs. tiled layout. |
| **Password Security (`AES`)** | **Set Password** (`AES-128` / `AES-256` user/owner protection via `PDFBox` + `BouncyCastle`) and **Remove Password** (unlock known encrypted PDF cleanly into decrypted copy). Prompt password input modal gracefully on protected PDF open (`max unlimited retries`). |
| **Converters & Print** | **Image -> PDF** (reorder images, page size `A4/Letter/Fit`, margins, orientation); **Text -> PDF**; **Office/HWP/ODF -> PDF** (`LibreOfficeKit` / HTML bridge); **PDF -> Long Image / Per-Page PNG/JPG**; **Offline PDF -> DOCX** (`LOK sdext/pdfimport` + `PDFBox` paragraph/table extraction + ML Kit OCR fallback for scanned pages); **System Print** exposed both inside Editor (`File -> Print`) and in `Tools` grid (`Android PrintManager` — fixing competitor stub). |

### `F-07`: Scanner, Dual-Side ID Card Mode, OCR & QR/Barcode
- **Document Scanner (`ML Kit Document Scanner` + `CameraX`):** Single-page & continuous Multi-page capture, edge crop adjustment, rotation, filters (`Original`, `Magic Color`, `Grayscale`, `B&W`), save directly to multi-page PDF or PNG gallery.
- **Dual-Side ID Card Mode (Replaces Competitor Stub):** Dedicated `ID Card` camera preset prompts **Step 1: Scan Front -> Step 2: Scan Back**, automatically composes both cropped faces centered vertically on an `A4` white canvas, and saves ready-to-print `PDF` / `PNG`.
- **Offline OCR (`ML Kit Text Recognition v2`):** User chooses script model (`Latin`, `Chinese`, `Devanagari`, `Japanese`, `Korean` covering `53+` languages) on Gallery image, Camera capture, or PDF page bitmap -> extracts clean editable text preview -> **Copy All**, **Save as `.txt`**, or **Export `.docx`**.
- **QR & Barcode Scanner (`CameraX` + `ML Kit Barcode`):** Real-time camera scan + gallery QR decode supporting `URL`, `Wi-Fi SSID/Password`, `vCard Contact`, `Email`, `Phone`, `Plain Text` with safe open-in-browser confirmation.

### `F-08` & `F-09`: Storage, Pure-Java `ZIP/7z`, Templates, Wi-Fi Transfer & Cloud
- **Storage & `.zip` / `.7z` Archives:** Folder hierarchy tree (Internal Storage, SD Card if mounted, Downloads), create folder, batch multi-select (`Move`, `Copy`, `Delete`, `Share`, `Merge`), **Native `.zip` & `.7z` Viewer/Extractor** (`Commons Compress 1.27.1` + `XZ`) showing archive entries, size, and extract-to-folder progress (gracefully handles password-protected `.zip`/`.7z` with prompt; informs cleanly if user taps an unsupported `.rar` file).
- **Document Templates:** Built-in clean-room starter blanks & templates bundled offline (`Blank Word`, `Resume CV`, `Meeting Notes`, `Blank Excel`, `Monthly Budget`, `Invoice Sheet`, `Blank Slide`, `Pitch Deck`) stored cleanly in `assets/templates/` (`$0` server download required).
- **Wi-Fi Transfer (`Ktor CIO` Foreground Service):** One-tap start (`DATA_SYNC` foreground notification + Android Quick Settings `TileService`) exposes **1) Modern Drag-and-Drop Web Browser UI (`http://<ip>:8998`)** so PC/Mac users upload/download files directly from Chrome/Safari with **zero client install**, PLUS **2) Local FTP Server** (`port 8998`) with configurable Anonymous / Username-Password & Read-Only toggle and idle auto-stop (`10 min` `[Internal Target]`).
- **Cloud Drives (`Drive v3` & `OneDrive MSAL`):** Multi-account Room persistence (storing account metadata + encrypted token reference — zero plaintext refresh token exposure), folder traversal, search, download to local sandbox/Downloads, upload modified/exported files back to Cloud. *(Dropbox excluded unless implemented cleanly; no dead Dropbox button.)*

### `F-10`: Hybrid Monetization, Consent (`UMP`) & Anti-Churn Placement Policy *(Ad Integration Deferred Per `CD-07`)*
- **Execution Phasing (`CD-07`):** Core features (`F-01`–`F-09`) ship to **`Done` BEFORE implementing/activating AdMob Mediation placements** in code. UI/architecture retains clean `ProStatus` entitlement boundaries and optional post-feature ad container hooks without rendering live ads during feature implementation.
- **Consent Gate:** Initialize **Google UMP SDK (CMP)** prior to requesting any ads (when ads are enabled) for EEA, UK, and Switzerland users (`Privacy Settings` revocation option visible inside Settings tab).
- **Pro Entitlements (`BillingClient 8.0.0`):** Single unified `ProStatus` unlocking ad removal + Pro power packs (`Batch Merge > 3 files`, `High-DPI Compress`, `Custom Watermark Presets`, `Batch/Multi-page OCR`, `Dual-Side ID Card`, `PKCS#7 Signing`, `Offline PDF->DOCX` after optional `1` free daily run, or **24h Rewarded Unlock Option**). Standard View/Edit/Save across all formats, Merge `<= 3` files, Set/Remove Password, Basic Scan, QR, Wi-Fi Transfer, and Print remain **100% Free**. Planned SKU tiers (`[Internal Target]` / Tier-1 Paywall Wireframe Anchor): `e05_weekly_trial` (`$4.99` w/ 3-day trial), `e05_yearly_pro_3999` (`$39.99/yr` w/ 7-day trial, Best Value), `e05_lifetime_pro` (`$59.99` Tier-1, geo-priced down to `$2.99–$9.99` in Tier-2/3). Skippable (`X`) Soft Onboarding Paywall controlled via `onboarding_show_paywall`.
- **Strict Ad Safety Caps (Applied When Ads Are Activated Post-Feature Completion):**
  1. **Zero ads** before a user successfully opens an external `ACTION_VIEW` document or inside `WordEditor` / `ExcelEditor` / `SlideEditor` / `PdfEditor` / `Scanner` `onResume` lifecycle.
  2. **Zero `SAVE_HEADER` traps** when saving user documents.
  3. **Interstitial ads** strictly post-milestone (e.g., export complete) with remote-configurable cooldown (`>= 60s` `[Internal Target]`) and zero back-to-back ads.
  4. **App-Open ads** strictly on qualifying cold launch / returning to `Home` tab (`>= 90s` cooldown `[Internal Target]`, skipped on notification/shortcut/file-intent deep links).

---

## 4. Screen Flows & Navigation

### 4.1 Screen & Route Map (`presentation.*` Single-Activity Compose + Manifest Aliases)

| # | Screen / Destination | Package / Module Scope | Entry Trigger & Core Role |
|---|---|---|---|
| 1 | `SplashRoute` | `feature.onboarding.splash` | Cold launch router, UMP CMP check, OS intent dispatcher (`ACTION_VIEW` -> jump directly to target Reader) |
| 2 | `LanguageSelectRoute` | `feature.onboarding.language` | First-launch locale selector (`10+` languages) |
| 3 | `OnboardingIntroRoute` | `feature.onboarding.intro` | 3-page feature carousel + skip |
| 4 | `StoragePermissionRoute` | `feature.onboarding.permission` | Core All-Files permission rationale + `MANAGE_EXTERNAL_STORAGE` / SAF selector |
| 5 | `MainShell` (`BottomBar`) | `app.navigation` | Persistent 4 tabs (`Home`, `Recent`, `Storage`, `Tools`) + Center Elevated Scan FAB (`FAB_SCAN`) |
| 6 | `HomeDocumentsRoute` | `feature.library.home` | Filter chips, quick search trigger, sort sheet, templates trigger, doc list |
| 7 | `RecentBookmarksRoute` | `feature.library.recent` | Segmented toggle: `Recent` (w/ reading progress) · `Favorites` · `Trash` |
| 8 | `SearchDocumentsRoute` | `feature.library.search` | Full-screen instant filename/folder search + format filters + recent queries |
| 9 | `StorageBrowserRoute` | `feature.storage.browser` | Local directory drill-down, `ZIP/7z` inspector, Cloud entry (`Google Drive`, `OneDrive`) |
| 10 | `CloudDriveBrowserRoute` | `feature.cloud.browser` | Account switcher, cloud folder browse, upload/download queue |
| 11 | `ToolsHubRoute` | `feature.tools.hub` | 3-section grid: `Convert` (`Image/Text/Doc->PDF`, `PDF->DOCX`), `PDF Power` (`Merge`, `Split`, `Compress`, `Watermark`, `Password`, `Sign`, `Print`), `Utilities` (`OCR`, `QR`, `Wi-Fi Transfer`, `Templates`, `Settings`) |
| 12 | `DocumentReaderRoute` | `feature.viewer` | Universal reader for `PDF`, `HWP/HWPX`, `ODF`, `EPUB`, `TXT/CSV` (`Continuous/Single`, `Dark/Sepia`, `Reflow`, `Auto-Scroll`, `TTS`, `Outline`, `Search`) |
| 13 | `OfficeEditorRoute` | `feature.editor.office` | Dynamic bottom workspace adapter for **Word** (`6` tabs + Review), **Excel** (`5` tabs + Formula bar), **Slide** (`7` tabs + Notes/SlideShow) powered by `LibreOfficeKit` |
| 14 | `SlideShowRoute` | `feature.editor.slideshow` | Fullscreen landscape presentation player + ink laser pointer |
| 15 | `PdfEditorRoute` | `feature.editor.pdf` | `PdfiumAndroid` viewport + Annotate/Highlight/Ink/Note + Signature placement + PKCS#7 Cert viewer |
| 16 | `SignaturePadRoute` | `feature.pdf.signature` | Handwritten Canvas signature drawer & saved signature history |
| 17 | `PdfToolConfigRoute` & `ConvertProgressRoute` | `feature.tools.pdf` | Unified MVI config + progress + result share/open flow (`Merge`, `Split`, `Compress`, `Watermark`, `Encrypt`, `Image->PDF`, `PDF->DOCX`) |
| 18 | `ScannerFlowRoute` | `feature.scanner.camera` | `ML Kit Doc Scanner` / `CameraX` (`Multi-Page Doc`, `Dual-Side ID Card`, `Crop & Filter`, `QR/Barcode`, `OCR Preview`) |
| 19 | `WifiTransferRoute` | `feature.transfer.wifi` | `Ktor CIO` HTTP Web Upload/Download + FTP server toggle, IP/QR link display, credentials & idle timer |
| 20 | `PaywallRoute` & `RewardGateSheet` | `feature.monetization` | Clean subscription/lifetime paywall + transparent pre-action Rewarded ad sheet |
| 21 | `SettingsRoute` | `feature.settings` | App Theme (`System/Light/Dark`), Language, Default File Handler setup, UMP Privacy Choices, Restore Purchases, OSS Licenses (`MPLv2`/Apache notices) |

### 4.2 App Navigation Hierarchy

```text
App Launch / External ACTION_VIEW Intent
   |
   +--> [External Document Intent] --> MIME Resolver --> [DocumentReader / OfficeEditor / PdfEditor]
   |
   +--> [Standard Launch -> SplashRoute (UMP Consent + Pref Check)]
           |
           +--> First Launch: [Language] --> [Intro] --> [Optional Paywall] --> [Permission] --> [MainShell]
           |
           +--> Returning User: ---------------------------------------------------------------> [MainShell]
                                                                                                     |
               +----------------------+-----------------------+------------------+-------------------+
               |                      |                       |                  |                   |
         [Home Tab]             [Recent Tab]           [Scan FAB (Center)] [Storage Tab]       [Tools Tab]
               |                      |                       |                  |                   |
     +---------+---------+            |                 [ScannerFlow]      [Local Folders]    [PdfToolConfig]
     |         |         |            v                 (Doc/ID/OCR/QR)    [ZIP/7z Viewer]    [ConvertProgress]
 [Search] [Templates] [Reader/Editor] <-----------------------+            [Cloud Browser]    [WifiTransfer]
                                                                                              [Settings]
```

### 4.3 Critical User Flows

#### Flow A: Word / Excel / PPT Editing with Draft Protection (Anti-`SAVE_HEADER` Flow)
1. User taps `report.docx` on Home -> checks read permission -> `OfficeEditorRoute` opens via `LibreOfficeKit` in `View Mode` -> user taps `"Edit"` FAB.
2. User modifies text/table/formula -> every `15s` & on `onStop` MVI writes delta/snapshot to private `filesDir/drafts/` + Room `DraftRecoveryEntity`.
3. User switches temporarily to Chrome to copy text and returns -> **zero `onResume` ad interrupts editor**.
4. User taps `"Done" / "Save"` -> writes directly back to original URI (`MediaStore`/`DocumentFile`) cleanly with **zero paywall interception**. Post-milestone toast confirms `"Saved"` *(optional `"Export PDF"` offered cleanly)*.

#### Flow B: Gated Power Tool Flow (`Split PDF` / `Batch Compress` / `Dual ID Card`)
1. Free user opens `ToolsHubRoute` -> taps `"Compress PDF"` -> selects PDF & quality preset.
2. If preset requires Pro entitlement, clear upfront bottom sheet shows **2 honest options before work runs**: `(a) Watch 1 short Rewarded Video for 24h Pass / Free Run` OR `(b) Upgrade to Pro` (never after processing).
3. `ConvertProgressRoute` converts via `PDFBox-Android` (`0% -> 100%`) -> `Success` displays size reduction (`e.g., 14.2 MB -> 3.8 MB`), `Open`, `Share`, `Locate`. One capped post-milestone Interstitial may trigger on return to Tools if cooldown elapsed.

---

## 5. UI Specifications & Competitor Design Inheritance

*(Full visual specs & interactive HTML wireframes expand in Phase 4 `05_DESIGN_SYSTEM.md` per `CD-08`; baseline tokens extracted from `01_COMPETITOR_REVERSE_ENGINEERING.md` §3.)*

- **Design System (`CD-06` Style B — Familiar Vibrant Utility):** Kotlin Jetpack Compose **Material 3** with full adaptive **Light Theme** + **Dark Theme** (`dynamicColor = false` to preserve clear brand/format color recognition), vibrant primary `#2489FF` CTAs, pastel icon tiles, and high-density utility layout. App Icon / Logo asset is deferred to a later milestone (temporary wordmark/monogram placeholder in headers).
- **Format-Coded Palette Tokens:**

| Token Name | Light Hex | Dark Adapt Token `[Phase 4 Target]` | Semantic Role |
|---|---|---|---|
| `BrandPrimary` | `#2489FF` | `#4DA1FF` | Primary CTA, Active Nav Tab, Center Scan FAB |
| `SurfaceBackground` | `#F2F4F8` | `#101318` | App canvas background |
| `SurfaceCard` | `#FFFFFF` | `#1A1E26` | Document list cards, bottom sheets, tool tiles |
| `SuccessExport` | `#3CC79A` | `#46D8A9` | Save confirmation, positive compression delta |
| `FormatWord` | `#1563DB` | `#4D8DF0` | Word (`DOC/DOCX`) icon badge & editor top bar |
| `FormatPdf` | `#FF5959` | `#FF7878` | PDF badge & PDF editor top bar |
| `FormatExcel` | `#27AC14` | `#48C736` | Excel (`XLS/XLSX`) badge & sheet toolbar |
| `FormatPpt` | `#FF7D05` | `#FF9838` | PowerPoint (`PPT/PPTX`) badge & slide bar |
| `FormatHwp` | `#0189DF` | `#33A5EE` | Hangul (`HWP/HWPX`) badge & viewer bar |
| `FormatOdf` | `#A724F8` | `#BD58FA` | OpenDocument (`ODT/ODS/ODP`) badge & bar |
| `FormatEpub` | `#86B918` | `#9FD134` | EPUB reader badge |
| `FormatTextArchive` | `#22A8F9` | `#52BCFB` | `TXT/CSV` & `ZIP/7z` badge |

- **Typography (`Poppins` 400/500/600/700):** `headlineMedium` (`22sp` Bold), `titleMedium` (`16sp` SemiBold), `bodyMedium` (`14sp` Regular), `labelMedium` (`12sp` Medium), `labelSmall` (`11sp` Medium — upgraded from competitor's sub-accessible `10sp` `#BBBBBB`!).
- **Spacing & Ergonomics:** `4dp` base grid (`xs 4dp`, `sm 8dp`, `md 12dp`, `lg 16dp`, `xl 24dp`). **Strict accessibility minimum:** All clickable nav items, toolbar icons, and quick chips have minimum **`48dp x 48dp`** hit box (`WCAG AA >= 4.5:1` contrast).

### 5.1 Key Screen ASCII Layout Reference

```text
+--------------------------------------------------+
| [Logo]  Evora Office         [Search] [Pro] [:]  | <- 64.dp Top Bar
+--------------------------------------------------+
| [All] [PDF] [Word] [Excel] [PPT] [HWP] [ZIP/7z]  | <- Horizontal FilterRow (48.dp)
+--------------------------------------------------+
| [Sort: Modified v]                 248 files     |
| +----------------------------------------------+ |
| | [PDF]  Contract_Signed_v2.pdf     1.8 MB [*] | | <- 72.dp DocCard (48.dp touch targets)
| |        Today, 14:22 · Page 3/12              | |
| +----------------------------------------------+ |
| | [DOCX] Q4_Product_Roadmap.docx    420 KB [ ] | |
| |        Yesterday · Draft Auto-saved          | |
| +----------------------------------------------+ |
| |        (Ad slots omitted until features Done per CD-07) | |
+--------------------------------------------------+
| [Home]   [Recent]   ((SCAN))  [Storage]  [Tools] | <- 80.dp NavBar + 56.dp Scan FAB
+--------------------------------------------------+
```

---

## 6. Remote Configuration Schema (`Firebase Remote Config`)

Clean, plaintext typed parameters (zero obfuscated Base64 waterfall payloads; fetched asynchronously without blocking splash beyond `1.5s`):

| Key | Type | Default | Description |
|---|---|---|---|
| `onboarding_show_language` | Boolean | `true` | Toggle first-launch language picker step |
| `onboarding_show_paywall` | Boolean | `true` | Show skippable Welcome Pro paywall post-intro |
| `ads_interstitial_cooldown_sec` | Long | `60` | Minimum seconds between post-milestone interstitials |
| `ads_app_open_cooldown_sec` | Long | `90` | Minimum seconds between cold/home App-Open ads |
| `ads_native_list_interval` | Int | `6` | List item index before first native ad slot (`0` disables) |
| `ads_disable_editor_resume` | Boolean | `true` | Safety lock ensuring zero resume ads inside editors/camera |
| `paywall_default_sku` | String | `"e05_yearly_pro_3999"` | Pre-highlighted plan on Pro paywall |
| `tool_reward_unlock_hours` | Int | `24` | Hours granted for one Rewarded Video Pro Tool pass |
| `lok_autosave_interval_sec` | Int | `15` | Background draft checkpoint cadence inside editors |

---

## 7. Non-Functional Requirements (`targetSdk 36` Compliance)

1. **Performance:** Cold launch to interactive Home `< 1.2s`; local file indexing `< 800ms` for `1,000` documents (`MediaStore` + incremental cache); `PdfiumAndroid` first-page render `< 350ms`; `60/120fps` scroll smoothness via Compose `remember`/immutable `@Immutable` models & Baseline Profiles.
2. **Binary & Page Alignment:** Every bundled `.so` (`libsofficeapp.so`, `libpdfium*.so`, AndroidX JNI) passes automated CI `readelf -l` verification for **ELF64 `PT_LOAD` `p_align == 0x4000` (`16 KB`)**. Zero release `Chucker` interceptors (`releaseCompileOnly`/`debugImplementation` only).
3. **Reliability:** Crash-free sessions `>= 99.8%`, ANR `<= 0.15%`. Surviving process death preserves open Reader page & dirty Editor draft via `DraftRecoveryEntity` + `SavedStateHandle`.
4. **Security & Privacy:** Strictly **zero upload of user files** (`100%` offline `$0` server). Encrypted `DataStore` / Android Keystore wrapping for stored Cloud OAuth tokens; `PendingIntent.FLAG_IMMUTABLE`; scoped file writes; no `SYSTEM_ALERT_WINDOW` or `USE_FULL_SCREEN_INTENT`.
5. **Responsive & Localization:** Adaptive UI supporting Phone (`portrait/landscape`), Tablets, Foldables (`WindowSizeClass`), RTL layouts (`AR`), and localized `strings.xml` for Top-10 target Tier-1 + Tier-2/3 markets (`en`, `vi`, `es`, `pt-rBR`, `ko`, `ja`, `in`, `hi`, `de`, `fr`).

---

## 8. External Interfaces & Local Server Contracts (Zero Backend APIs)

Per Charter `CD-04`, E05 operates **zero proprietary server endpoints** (no `office-convert` upload endpoint). External integrations follow standard vendor SDKs & LAN protocols:

1. **Google Drive (`REST API v3`):** `GoogleSignIn` / Credential Manager OAuth2 (`DriveScopes.DRIVE_FILE` / `DRIVE_READONLY` scoped cleanly), `Files.List` paginated (`pageSize=50`), `MediaHttpDownloader` resumable stream, `Files.Create` upload (`FileContent`).
2. **Microsoft OneDrive (`MSAL + Graph SDK`):** Multi-account MSAL configuration (`Files.ReadWrite.All`, `User.Read`), `DriveItemCollectionPage`, `LargeFileUploadTask` chunked upload for files `> 4 MB`.
3. **Embedded Wi-Fi Web/FTP Server (`Ktor CIO` LAN Port `8998`):**
   - `GET /` -> Served responsive offline HTML5 drag-drop file browser (`assets/wifi_web/index.html`)
   - `GET /api/files?path={dir}` -> JSON file list (path traversal validated inside shared sandbox/storage scope)
   - `GET /api/download?path={file}` -> Streaming file download (`Content-Disposition: attachment`)
   - `POST /api/upload` -> Multipart streaming write into `Download/EvoraOffice/`
   - `FTP` / optional `FTPS` (`port 8998`) configurable with read-only & PIN auth toggles.

---

## 9. Domain Data Models & Enriched Room Schema

Fixes the competitor's path-only SQLite flaw (`RecentTable(path TEXT PK)` lacking timestamps/positions):

```kotlin
// Pure Kotlin Domain Contracts (Zero android.* imports)
enum class DocFormat { PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX, ODT, ODS, ODP, HWP, HWPX, EPUB, TXT, CSV, ZIP, SEVEN_Z }

data class DocumentItem(
    val id: String,               // Stable MediaStore ID or canonical URI hash
    val uriString: String,
    val displayName: String,
    val extension: String,
    val format: DocFormat,
    val sizeBytes: Long,
    val modifiedAtEpochMs: Long,
    val parentFolderName: String,
    val isFavorite: Boolean,
    val lastOpenedAtEpochMs: Long?,
    val lastPageIndex: Int,
    val lastTotalPages: Int,
    val isPasswordProtected: Boolean
)
```

### Room Database Entities (`EvoraOfficeDatabase`)
1. `DocumentMetaEntity`: `@PrimaryKey val uriString: String`, `displayName`, `format`, `sizeBytes`, `modifiedAt`, `lastOpenedAt`, `lastPageIndex`, `lastScrollOffsetY`, `lastZoom`, `readerTheme` (`LIGHT`/`DARK`/`SEPIA`), `isContinuousScroll`, `isFavorite`.
2. `TrashEntity`: `@PrimaryKey val trashId: String`, `originalUriString`, `cachedFilePath`, `displayName`, `format`, `sizeBytes`, `trashedAtEpochMs`, `expiresAtEpochMs`.
3. `DraftRecoveryEntity`: `@PrimaryKey val targetUriString: String`, `draftCachePath`, `displayName`, `updatedAtEpochMs`, `hasPendingRecovery: Boolean`.
4. `SignatureEntity`: `@PrimaryKey val id: String`, `svgOrPngPath`, `createdAtEpochMs`.
5. `CloudAccountEntity`: `@PrimaryKey val accountId: String`, `provider` (`GOOGLE_DRIVE`/`ONE_DRIVE`), `email`, `displayName`, `avatarUri`, `encryptedTokenAlias`.

---

## 10. Analytics Event Taxonomy (`Firebase Analytics` + `Adjust`)

Clean action & unit-economics telemetry without intrusive ad-escalation scripts:

| Event Name | Parameters | Purpose |
|---|---|---|
| `ad_impression` | `ad_platform`, `ad_format`, `ad_unit_name`, `currency`, `value` | Mandatory impression-level ad revenue feedback into Google Ads `tROAS` (Tier B UA) |
| `onboarding_step` | `step_name`, `action` (`view`/`complete`/`skip`) | Measure first-run funnel & permission grant rate |
| `storage_permission_result` | `grant_type` (`all_files`/`saf_fallback`/`denied`) | Track `MANAGE_EXTERNAL_STORAGE` acceptance vs fallback |
| `document_open` | `format`, `source` (`home`/`external_intent`/`cloud`/`widget`/`search`) | Core engagement & ASO/format mix insight |
| `editor_session_end` | `format`, `edit_duration_sec`, `save_status` (`saved`/`draft`/`discarded`) | Verify zero crash/save loss inside `LibreOfficeKit` |
| `pdf_tool_complete` | `tool_type` (`merge`/`split`/`compress`/`watermark`/`sign`/`encrypt`/`pdf_to_docx`/`print`), `duration_ms`, `success` | Power suite adoption & performance SLA |
| `scan_ocr_complete` | `mode` (`doc`/`id_card`/`ocr`/`qr`), `page_count`, `script` | Scanner & OCR script distribution |
| `paywall_impression` & `purchase_success` | `trigger_source`, `sku_id`, `price_usd_micros` | Trial-to-Paid (`32.5%+` benchmark) & LTV tracking |

---

## 11. Release Plan (`1.0.0` / `versionCode 100`) & Appendix

### 11.1 Mandatory Release Exit Gates (Before Google Play Production)
1. **100% Format Smoke Matrix on Physical Device (`arm64-v8a`):** Open, scroll, search, bookmark, and export/print sample files across `PDF`, `DOC/DOCX`, `XLS/XLSX`, `PPT/PPTX`, `ODT/ODS/ODP`, `HWP/HWPX`, `EPUB`, `TXT/CSV`, `ZIP/7z`.
2. **ELF64 `16 KB` Check:** `0` non-aligned `.so` segments across final Release `.aab`.
3. **Zero P0/Blocker QA Sign-Off (`Gate 3`):** Process death preserves `DraftRecoveryEntity`, zero release `Chucker`/log leaks, UMP consent tested under EEA VPN profile.
4. **Play Store Submission Kit:** `MANAGE_EXTERNAL_STORAGE` compliance video recorded in English, Google Play Data Safety form synchronized with Drive/OneDrive/AdMob/Firebase SDK disclosures, localized screenshots & feature graphic ready in Week 4.

### 11.2 Document Handoff
- **Immediate Next Step (Phase 4 UI/UX):** `ux-architect` & `ui-designer` translate Section 4 (Screens) and Section 5 (Tokens) into `docs/05_DESIGN_SYSTEM.md` + interactive/Pencil wireframes for **CEO Gate 2**.
