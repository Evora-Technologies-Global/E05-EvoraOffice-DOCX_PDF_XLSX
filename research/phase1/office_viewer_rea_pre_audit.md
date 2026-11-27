# Phase 1 REA Pre-Audit & Feature Feasibility Checklist: `com.officedocument.word.docx.document.viewer`

**Project:** E05 — Native Android Application (Kotlin + Jetpack Compose, MVI + Clean Architecture)  
**Company:** Evora Technologies Global  
**Prepared by:** App Reverse Engineer (`app-reverse-engineer`) & Reality Checker (`reality-checker`)  
**Date:** 2026-11-25  
**Target Package:** `com.officedocument.word.docx.document.viewer`  
**Status:** **`COMPLETED — UNBLOCKED VIA ADB DEVICE PULL`** (See full decompiled report in [`../../docs/01_COMPETITOR_REVERSE_ENGINEERING.md`](../../docs/01_COMPETITOR_REVERSE_ENGINEERING.md))

---

> [!NOTE]
> This pre-audit checklist has been **resolved and superseded** by the completed REA APK decompilation report at [`../../docs/01_COMPETITOR_REVERSE_ENGINEERING.md`](../../docs/01_COMPETITOR_REVERSE_ENGINEERING.md) after pulling `base.apk`, `split_config.arm64_v8a.apk`, and `split_config.xxhdpi.apk` directly from physical device `SM_A165F` (`RF8Y60B9NCZ`). The pre-audit sections below are retained for historical audit traceability.

---

## 1. Honest Execution & Tooling Readiness Report

Per CEO directive (*"report trung thực"* / *"đục code để xác nhận tính khả thi"*) and E05 anti-hallucination rules (`CLAUDE.md` "Untrusted and local-only data", `agency-app-reverse-engineer`, `reality-checker`), **no code decompilation has been performed yet** because no authentic APK/XAPK/AAB binary for `com.officedocument.word.docx.document.viewer` is present locally or on an attached device.

### 1.1 Blocker Verification Summary
| Check | Observed State | Impact |
|---|---|---|
| **Workspace `.local/` & Nearby Folders** | `.local/` had no APK; `~/Downloads/`, `~/Desktop/`, `/Users/macbook/Desktop/Kaito Punch/` contain no APK/XAPK for `com.officedocument.word.docx.document.viewer` | Cannot inspect `AndroidManifest.xml`, `classes*.dex`, `lib/*.so`, assets, or Billing/AdMob SDKs |
| **Connected Android Device (`adb devices -l`)** | Empty (`List of devices attached` = 0 devices) | Cannot pull split APKs directly from a Google Play installation |
| **3rd-Party Mirror Scrape Guardrail** | Not attempted via anti-bot bypass | Per project safety rules, we do not bypass Cloudflare/anti-bot gates or analyze unverified repackaged binaries |
| **Local Decompilation CLI Toolchain** | `/opt/homebrew/bin/rea` (`6.8.0`), `/opt/homebrew/bin/jadx` (`1.5.3`), `/opt/homebrew/bin/apktool` (`3.0.3`), `Java 24.0.2`, Android `build-tools` (`28.0.3`–`36.1.0`) are installed | Ready to inspect manifests, DEX/Java/Kotlin code, resources, APK signatures, and native `.so` libs immediately upon receiving the APK |
| **REA JADX MCP Bridge (`REA_JADX_MCP_JAR`)** | `rea doctor --provider jadx --json` reports `missing_analysis_engine` (`jadx-headless-mcp 0.7.1` JAR not configured) | `rea` archive/signature/native tools work natively; Java/Kotlin decompilation can run immediately via `jadx` 1.5.3 + `apktool` 3.0.3 CLI, or via REA MCP if `REA_JADX_MCP_JAR` is supplied |

---

## 2. Observed Competitor Feature Surface (Play Store Listing & Visual Screenshot Audit)

Before code inspection, the following feature inventory was **directly observed** from Google Play Store metadata (`hl=en` & `hl=vi`, version `311200`, updated Nov 23, 2026) and the 5 official English Play Store screenshots saved in `.local/playstore_assets/ss_en_0.png`–`ss_en_4.png`.

### 2.1 Visually Verified UI Capabilities (Screenshots `ss_en_1` – `ss_en_4`)
| Module | Visually Observed UI Elements (Screenshots) | Play Store Listing Claims (Unverified in Code!) | Engineering Parity Difficulty (Pre-APK Estimate) |
|---|---|---|---|
| **Word (`DOC`, `DOCX`, `DOCS`)** | Top bar: Undo, Redo, `Done`; teardrop text selection handles; bottom sheet `Edit` dropdown, Font selector (`Arial`), Font size (`14` stepper), `B`/`I`/`U`, Text Color + 12-swatch palette, Highlight color, Cut/Copy/Paste, 4 text alignments (`ss_en_1.png`) | Offline create/view/edit, **Track Changes**, **Comments**, **Spell Check**, search, share | **HIGH:** Pure read-only viewing is moderate; full WYSIWYG layout-faithful editing + selection handles + Track Changes/Comments is very complex without an engine |
| **Excel (`XLS`, `XLSX`)** | Top bar: filename, **Crown (Pro/VIP) icon**, outline/sheet icon, fullscreen; grid rows/cols with active cell border + resize handles; **embedded Donut/Pie charts** rendered below data rows (`ss_en_2.png`); bottom sheet `Format` dropdown, `Width (cm)`, `Height (cm)`, `Merge`, `Number format` | Update data, review calculations/formulas, format reports | **HIGH:** Formula evaluation engine + cell span merging + embedded chart rendering + virtualized 2D grid scrolling |
| **PDF** | Top bar: filename, outline/pages icon, fullscreen; crisp rendered text; interactive **handwritten e-Signature overlay** with dashed bounding box + delete (`×`) + rotate/scale handles (`ss_en_3.png`); bottom sheet `Signature` dropdown, `Note`, `Author`, `Highlight` | PDF reader & editor, annotations, highlight, bookmarks, reading modes, e-Sign, watermark | **MEDIUM–HIGH:** Viewing via Pdfium (`Apache 2.0`) or Android `PdfRenderer` is straightforward; persistent PDF annotation/signature saving (`PDFBox` / `Pdfium` write / `MuPDF`) & text selection/reflow require careful library choice |
| **PowerPoint (`PPT`, `PPTX`, `PPS`, `PPSX`)** | Top bar: filename, slide list icon, fullscreen; slide deck rendering with embedded images & multi-column text/shapes; bottom sheet `Format` tab, `Shape & Line` styles (fill/outline/dash), `Arrange` (Z-order forward/backward), 15-swatch color picker (`ss_en_4.png`) | View, **edit**, and present slides | **VERY HIGH:** Slide object canvas editing (vector shapes, z-order, text boxes, image placement, legacy binary `.ppt` vs `.pptx`) is heavy engine territory |
| **Document Scanner + OCR** | Not shown in the 5 screenshots | Scan papers/notes/receipts -> **editable Word** or **searchable PDF**, OCR text extraction, e-signature | **LOW–MEDIUM (on Android):** **Google ML Kit Document Scanner + Text Recognition v2** provides offline scanning/OCR at `$0` cost; reconstructing layout-accurate **editable DOCX** from arbitrary photos is much harder than plain text extraction |
| **Other Formats & File Ops** | Listed in description only | `TXT`, `HWP` (Hangul!), `ODT`, `ZIP` extraction/viewing; device-wide auto-scan, search, share | **MEDIUM–HIGH:** `TXT`/`ZIP` trivial; `ODT` moderate; **`HWP` (Hangul Word Processor proprietary/HWPX format)** requires a specialized parser if truly supported! |

### 2.2 Observed Play Store Permissions (Require Code Justification Audit)
Extracted directly from Play Store permissions metadata (`ds:5[1][2][74]`):
- **Storage / Media:** Read & modify/delete USB storage (likely paired with **`MANAGE_EXTERNAL_STORAGE`** in manifest on Android 11+ to auto-scan all local documents).
- **System Alert / Overlay:** `draw over other apps` (`SYSTEM_ALERT_WINDOW`) — **CRITICAL RED FLAG** to investigate in APK (likely floating quick-viewer, caller-ID/after-call screen, or out-of-context ad overlay).
- **Identity / Contacts:** `find accounts on the device` (`GET_ACCOUNTS`) — verify if used for Google Drive/account sign-in or legacy tracking.
- **Camera:** `take pictures and videos` — Document Scanner flow (note: Google ML Kit Document Scanner does not even require `CAMERA` permission if delegated via Play Services).
- **Connectivity / Hardware:** `control Near Field Communication` (`NFC`), `view Wi-Fi connections`, `prevent device from sleeping`, `install shortcuts`, `Google Play license check`.

---

## 3. Critical Legal & Technical Feasibility Reality Check ("Tái sử dụng công nghệ")

Per CEO instruction (*"có thể lấy công nghệ của họ để tái sử dụng cho công nghệ của mình"* and *"app đối thủ làm được tính năng gì thì app của mình cũng phải làm được tính năng đó"*), E05 engineering must strictly respect **IP/licensing boundaries** and **2026 Google Play technical mandates**:

1. **What CAN vs. CANNOT Be Reused from Competitor Decompilation:**
   - **CANNOT Reuse:** Copying competitor proprietary Java/Kotlin code, obfuscated SDKs, `.so` compiled binaries extracted from their APK, icons/assets, or pirated commercial SDK keys. Doing so creates copyright/DMCA takedown liability, violates Play Developer Program Policies, and exposes E05 to unmaintainable native `.so` crashes.
   - **CAN Adopt / Reuse:**
     - Identification of **permissive open-source libraries** (`Apache 2.0`, `MIT`, `BSD`, `MPLv2` weak copyleft) bundled inside the competitor app (e.g., `Apache POI`, `Pdfium`, `LibreOfficeKit` under MPLv2, `PDFBox-Android`, `ML Kit`, `Room`, `OkHttp`).
     - Architectural patterns, UI component blueprints, file-indexing strategies, gesture/zoom mechanics, and state-machine/edge-case handling observed in their flow — rebuilt cleanly in **Kotlin + Jetpack Compose + MVI**.
     - Knowing **which commercial SDKs** (if any — e.g., Foxit, Apryse, Nutrient, Aspose, OliveOffice, Thinkfree) the competitor pays for, so CEO can make an informed **Build vs. License** decision in Phase 2 Charter.
2. **The `MuPDF` AGPL v3 & 16 KB Page Alignment Trap:**
   - **AGPL v3 Trap:** If the competitor uses open-source **MuPDF** without a paid Artifex commercial license, bundling AGPL v3 code in a closed-source Play Store app creates severe copyleft risk. E05 should favor **Pdfium (`Apache 2.0`)**, **Android 15/16 Jetpack `PdfViewer` / `PdfRenderer`**, or **Apache POI (`Apache 2.0`)** / **LibreOfficeKit (`MPLv2`)** unless a commercial PDF SDK budget is approved.
   - **16 KB Memory Page Size Mandate (API 35/36):** Competitor currently targets **API 35**. E05 must target **Android 16 (API 36)** from Day 1, meaning **every** native C/C++ `.so` library (Pdfium, LibreOfficeKit, OpenCV, etc.) must be compiled with **NDK r27+ / 16 KB ELF alignment** (`readelf -l` / `objdump` verification).

---

## 4. Mandatory REA Code Audit Checklist (To Execute Immediately Once APK Is Supplied)

As soon as `com.officedocument.word.docx.document.viewer` APK/XAPK is placed in `.local/`, REA (`app-reverse-engineer` + `rea` + `jadx` + `apktool`) will execute and produce **`docs/01_COMPETITOR_REVERSE_ENGINEERING.md`** answering these exact questions:

1. **Rendering & Editing Engine Identification (Mấu chốt kỹ thuật #1):**
   - Inspect `lib/arm64-v8a/*.so` and `classes*.dex` package namespaces to determine the **exact** Word/Excel/PPT/PDF/HWP/ODT engine (e.g., custom `Apache POI` + `Canvas` port like `OfficeReader`, `LibreOfficeKit`, `WebView` HTML bridge, `Pdfium`/`MuPDF`, or a commercial SDK).
   - Verify whether **Track Changes**, **Comments**, **Spell Check**, **PPT/XLS editing**, and **`HWP`/`ODT`** are genuinely implemented in bytecode/native libs or are read-only/partial/stubbed.
   - Run ELF alignment check on every bundled `.so` to test 16 KB Android 15/16 readiness.
2. **Scanner, OCR & e-Signature Stack:**
   - Confirm whether Document Scanner / OCR uses **Google ML Kit**, **Tesseract**, **OpenCV**, or a **remote backend API** (and check API endpoints/costs).
3. **Permissions & Policy Deep-Dive:**
   - Trace exact call sites of **`MANAGE_EXTERNAL_STORAGE`**, **`SYSTEM_ALERT_WINDOW`**, **`GET_ACCOUNTS`**, and **`NFC`** in decompiled code to verify whether they support essential UX or aggressive ad/background retention hooks.
4. **Monetization, Ad Mediation & Offline Ad Mechanics:**
   - Extract AdMob Mediation adapters (`AppLovin`, `Mintegral`, `Meta`, `Pangle`, `Unity`, `iKame/Begamob SDK`), Firebase Remote Config keys, exact triggers for **app-switch (`onResume`) interstitials** and **offline/cached ads**, and exact Google Play Billing SKUs (`$1.99–$39.99`) + save-gate logic.
5. **Architecture, Screen Inventory & Stability Bugs:**
   - Map all Activities/Fragments/Composables, local database (`Room`/`SQLite`) schema, and root-cause why competitor crashes/loses unsaved edits on backgrounding (so E05 guarantees zero data loss via `Room` + atomic draft recovery).

---

## 5. How to Unblock REA Immediately

Please provide the genuine Play Store build of `com.officedocument.word.docx.document.viewer` using **either** method:
1. **Option A (Recommended — USB ADB Pull from Test Device):** Install the app from Google Play on an Android test phone, connect via USB with USB Debugging enabled, and let us run `adb shell pm path com.officedocument.word.docx.document.viewer` + `adb pull` directly into `.local/competitor_apk/`.
2. **Option B (Manual Drop):** Place the `.apk` / split `.apks` / `.xapk` into:
   `E05/.local/competitor_apk/`
3. *(Optional)* To also enable REA's JADX MCP bridge (`inspect_android_package`, `inspect_android_method`), set `REA_JADX_MCP_JAR` to a `jadx-headless-mcp-0.7.1.jar` path; otherwise standalone `jadx` 1.5.3 + `apktool` 3.0.3 + `rea` 6.8.0 are already installed and ready to run.
