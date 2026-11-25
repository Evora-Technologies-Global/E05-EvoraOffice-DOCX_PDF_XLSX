# Project Charter — E05 (Native Android All-in-One Office & PDF Suite)

**Project Code:** E05  
**Company:** Evora Technologies Global  
**Document Owner:** Chief of Staff (`chief-of-staff`)  
**Date:** 2026-11-25  
**Status:** APPROVED & LOCKED BY CEO (Phase 2 Complete -> Unlocking Phase 3 PO/BA Specs)  
**Benchmark App:** `com.officedocument.word.docx.document.viewer` (*Word Document: DOCX, PDF, XLSX* / *Office App* by `iKame Applications - Begamob Global`)  
**Product Brand & Store Title:** `Evora Office` (`Evora Office: DOCX, PDF, XLSX` — `29/30` chars)  
**Package Name (`applicationId`):** `com.evora.technologies.office`  
**Release Target:** `versionName = 1.0.0`, `versionCode = 100` (Single Full-Parity Release — Amended Option P2)  
**Target Platform & Architecture:** Native Android (`Kotlin` + `Jetpack Compose`, `MVI + Clean Architecture`, `minSdk 26`, `targetSdk 36`, strict `16 KB` ELF alignment)  

---

## 1. Decision Log (Phase 1 -> Phase 4 Pre-Kickoff Locked)

| Date | Decision ID | Decision Item | Status / Owner | Locked CEO Decision & Rationale |
|---|---|---|---|---|
| 2026-11-25 | `PD-01` | Target Platform & Architecture | **Locked (CEO)** | Strictly Native Android (`Kotlin + Jetpack Compose`, `MVI + Clean Architecture`) per [`GEMINI.md`](../GEMINI.md) and [`LLM.md`](../LLM.md) §13. |
| 2026-11-25 | `PD-02` | Benchmark Selection & Full Parity Mandate | **Locked (CEO)** | Full (`100%`) clean-room functional parity with `com.officedocument.word.docx.document.viewer` across View, Edit, PDF Tools, Scan/OCR, Cloud, and Wi-Fi Transfer. |
| 2026-11-25 | `CD-01` | Core Document Engine Track | **Locked (CEO)** | **Track A: Clean-Room Open-Source (`$0` Engine Royalties)** — `LibreOfficeKit / Collabora Android Core` (`MPLv2/LGPLv3`, Linux NDK r27+ 16 KB build) + `PdfiumAndroid` (`Apache 2.0`, 16 KB) + `TomRoush/PdfBox-Android` (`Apache 2.0`) + `ML Kit` + `Apache Commons Compress`. |
| 2026-11-25 | `CD-02` | Delivery Phasing, Versioning & Schedule | **Locked (CEO)** | **Amended Option P2 (Single Full-Parity `v1.0.0` Launch):** `versionName = 1.0.0`, `versionCode = 100`. Target **`4-week` (1-month) `[Internal Target]`** AI-accelerated engineering execution covering full features, ASO metadata, and store screenshots. *(External Google Play review / 14-day closed-test policy gates tracked separately from build completion.)* |
| 2026-11-25 | `CD-03` | Launch UA Budget Tier | **Locked (CEO)** | **Tier B (`$1,000–$5,000/mo` Validation & Hybrid Test)** — 60% Google App Campaigns (strictly segmented Tier-1 vs. Tier-2/3) + 40% ASO & Store Listing CVR iteration. |
| 2026-11-25 | `CD-04` | HWP / PDF->DOCX / Archive & AI Scope | **Locked (CEO)** | **Strictly 100% Offline `$0` Server COGS:** `HWP/HWPX` rendered offline via `hwplib`/`hwpxlib` -> local HTML/CSS/SVG or ODT/LOK bridge; `PDF->DOCX` offline via `PDFBox-Android` + `LibreOfficeKit` / OCR fallback; **`.zip` and `.7z` supported natively** (exclude `.rar` to respect UnRAR license); **Zero Cloud AI** and zero fake "Coming Soon" stubs. |
| 2026-11-25 | `CD-05` | Product Naming & Package Namespace | **Locked (CEO)** | **Launcher / In-App Name:** `Evora Office`; **Google Play Title:** `Evora Office: DOCX, PDF, XLSX` (`29/30` chars); **Package Name (`applicationId`):** `com.evora.technologies.office` (replaces placeholder `com.evora.office.document.reader`). |
| 2026-11-25 | `CD-06` | App Visual Style & Brand Logo Timing | **Locked (CEO)** | **Style B — Familiar Vibrant Utility:** Primary blue CTA (`#2489FF`), format-coded color badges, pastel tool tiles, and high-density mainstream utility layout upgraded to Compose Material 3 adaptive Light/Dark themes, `>= 48dp` touch targets, and `WCAG AA` contrast. **App Logo / Icon design is deferred** to a later milestone (Phase 4 uses a temporary wordmark/monogram placeholder). |
| 2026-11-25 | `CD-07` | Ads Implementation Phasing & Pro/Paywall Spec | **Locked (CEO)** | **Feature-Complete First:** Do **not** implement AdMob/Mediation ads yet — complete all core product features (`Done`) before integrating ad placements. **Paywall & Pro UX Boundary (for Phase 4 Wireframes):** Skippable (`X`) Soft Onboarding Paywall (`onboarding_show_paywall`), sample Tier-1 anchor SKUs (`Weekly $4.99 / 3d trial`, `Yearly $39.99 / 7d trial`, `Lifetime $59.99`); Free 100% View/Edit/Save + Merge `<= 3` files + Basic Scan + QR + Wi-Fi Transfer + Print; `Pro` (`Crown`) / 24h Rewarded Pass gating reserved for Ad-free, Batch Merge `> 3` files, High Compression, Custom Watermark, Batch/Multi-page OCR, Dual-Side ID Card, PKCS#7 Signing, and Offline PDF->DOCX (`1` free daily run or Pro/Reward). |
| 2026-11-25 | `CD-08` | Phase 4 Wireframe Format & R-01 Build Channel | **Locked (CEO)** | **Gate 2 Wireframes:** Interactive HTML/CSS/SVG prototype + `docs/05_DESIGN_SYSTEM.md` (`Pencil MCP` unavailable in current runtime). **R-01 C++ Build Channel:** **Option 1 — GitHub Actions Linux Runner** (via `gh` `duylt-dev`, cross-compiling `LibreOfficeKit CPAndroid` ARM64 `16 KB` `.so` and verifying `readelf -l` `0x4000` in CI to bypass the `12 GiB` local SSD limit). |
| 2026-11-25 | `CD-09` | Google Play Developer Account Status | **Locked (CEO)** | Google Play account is a **Personal Developer account** (requires external 14-day / 12-tester closed test). Per CEO directive, account-type administration is **ignored / out-of-scope** for engineering decisions. |

---

## 2. Executive Mandate & Product Vision

### 2.1 Market Thesis (Grounded in Phase 1 Evidence)
- The benchmark app (`com.officedocument.word.docx.document.viewer`) proves massive demand (**`94,749,451` cumulative installs**) paired with high dissatisfaction (**`4.4★` EN / `4.2★` VN** rating; **`11.9%` EN / `17.1%` VN 1-star share**), documented in [`01_MARKET_RESEARCH.md`](01_MARKET_RESEARCH.md) and [`../research/phase1/office_viewer_ua_business_evaluation.md`](../research/phase1/office_viewer_ua_business_evaluation.md).
- Reverse engineering ([`01_COMPETITOR_REVERSE_ENGINEERING.md`](01_COMPETITOR_REVERSE_ENGINEERING.md)) isolated the competitor's churn triggers:
  1. Aggressive monetization traps (`SAVE_HEADER` post-edit lock prompts, `onResume` editor interstitial spam, `33` `UnlockFeature` paywall gates).
  2. Dead UI stubs advertised in menus ("Coming Soon" AI tools, Tools Split PDF, Tools Print shortcut, ID Card Scan, Track Changes UI, Continuous Scroll toggle, PDF Watermark, zero archive extraction code).
  3. Commercial `Artifex SmartOffice` (`libso.so`), `MuPDF` (`libmupdf_java.so`), and a private backend endpoint (`office-convert.begamob.com`).

### 2.2 E05 Product Vision & Strategic Targets
> Ship a `100%` offline, `$0`-server-COGS **Native Android Office & PDF Suite** (`v1.0.0`, `versionCode 100`) that matches every functional module of the benchmark app, delivers real implementations for the competitor's broken UI stubs, eliminates `SAVE_HEADER` lock traps, and achieves top-quartile performance and store trust.

| Metric | Benchmark Baseline | E05 Charter Target `[Internal Target]` | Verification Source |
|---|---|---|---|
| **Google Play Rating** | `4.4★` EN / `4.2★` VN | **`>= 4.7★`** *(simple-design tier parity)* | Play Console Vitals & Store Reviews |
| **1-Star Review Share** | `11.9%` EN / `17.1%` VN | **`<= 4.0%`** | Post-launch sentiment audit |
| **Cold Start Time** | Legacy XML / 681 layouts | **`< 1.2s`** *(Compose + Baseline Profiles)* | Macrobenchmark / Android Vitals |
| **Crash-Free / ANR** | Standard legacy baseline | **Crash-free `>= 99.8%` / ANR `<= 0.15%`** | Android Vitals Gate |
| **Day 30 Retention** | `5%–12%` category range | **`10%–14%`** *(top-quartile productivity)* | Firebase / MMP Cohort Tracking |
| **16 KB Page Compliance** | Competitor `20/20` `.so` aligned (`0x4000`) | **`100%` ELF64 `PT_LOAD` `p_align == 0x4000`** | CI `readelf -l` verification gate |

---

## 3. Locked Technical Architecture (`Track A: 100% Clean-Room Offline`)

No `AGPLv3` binaries (upstream `MuPDF`) and zero competitor binaries, strings, packages, icons, or server endpoints from `.local/` may ever enter E05.

### 3.1 Approved Engine & Subsystem Stack (`$0` Royalties, `$0` Server COGS)

| Subsystem | Locked Clean-Room Technology | License / Compliance Notes | 16 KB & Build Reality |
|---|---|---|---|
| **Office View + Rich Edit (`DOC/DOCX/XLS/XLSX/PPT/PPTX/ODT/ODS/ODP`)** | **LibreOfficeKit / Collabora Android Core** (`--with-distro=CPAndroid --enable-android-editing`) | `MPLv2` / `LGPLv3` — Kotlin/Compose app stays proprietary closed-source provided LOK library boundaries/patches stay public under `MPLv2`. | **Critical Path:** No prebuilt Maven AAR exists. Must cross-compile on Linux with **NDK r27+ (`-Wl,-z,max-page-size=16384`)** and bundle `~30+ MB` native `.so` inside ABI-split `.aab`. |
| **PDF View + Render** | **`PdfiumAndroid`** (`16 KB` NDK r27+ recompiled build) | `Apache 2.0` — safe commercial usage, zero AGPL obligations. | Legacy upstream builds are 4 KB aligned; must build/verify NDK r27+ `16 KB` alignment (`0x4000`). |
| **PDF Power Tools (`Merge, Split, Compress, Sign, Encrypt, Watermark, Image->PDF`)** | **`TomRoush/PdfBox-Android`** + Android `PrintManager` + Compose Canvas | `Apache 2.0` — pure Java/Kotlin PDF manipulation. | Zero native `.so` risk (`0` 16 KB issues). Enables real offline Split/Compress/Watermark that competitor stubbed out. |
| **Korean `HWP` / `HWPX`** | **`kr.dogfoot:hwplib` (`1.1.4`)** + **`kr.dogfoot:hwpxlib` (`1.0.1`)** -> Offline Renderer Bridge | `Apache 2.0` — AST parsers only (no built-in rasterizer). | Parse AST -> clean local HTML/CSS/SVG rendered in locked offline WebView, or emit ODT/DOCX into LibreOfficeKit for viewing & PDF export (`$0` server). |
| **PDF -> DOCX Export** | **`LibreOfficeKit` Writer import (`sdext/pdfimport`)** + **`PDFBox-Android` + ML Kit OCR fallback** | `MPLv2` + `Apache 2.0` — 100% offline conversion. | Digital PDFs convert via LOK/PDFBox structure; scanned image PDFs extract text via on-device ML Kit OCR before `.docx` assembly. |
| **Archive Browser (`ZIP` / `7z`)** | **`org.apache.commons:commons-compress` (`1.27.1`)** + **`org.tukaani:xz`** | `Apache 2.0` — pure Java extraction. | `.rar` explicitly excluded per `CD-04` to avoid UnRAR proprietary licensing obligations. |
| **Scan, ID Card, OCR, QR** | **CameraX + ML Kit Document Scanner + ML Kit Text Recognition v2 + ML Kit Barcode** | Google Play Services / ML Kit SDK (`$0` API cost). | Genuine dual-side ID Card scan layout, perspective crop, filters, and offline OCR. |
| **Cloud, Transfer & Widgets** | **Google Drive REST API + MSAL OneDrive + Ktor CIO Web/FTP Server + Jetpack Glance** | Standard Android / Apache-licensed libraries (`$0` server). | Local LAN Wi-Fi HTTP/FTP sharing & modern Material 3 / Glance desktop shortcuts. |
| **AI Menu Items** | **Omitted / Excluded per `CD-04`** | Zero Cloud AI endpoints (`$0` variable API COGS). | Eliminates all fake "Coming Soon" AI buttons from UI to protect store rating. |

---

## 4. Complete Feature Scope (`v1.0.0`, `versionCode 100`)

All real benchmark modules plus competitor stubs converted into real features ship together in **`1.0.0` (`100`)**:

| Domain | Scope Included in `1.0.0` (`100`) | Explicitly Excluded (`CD-04`) |
|---|---|---|
| **Document Viewing & Library** | `PDF`, `DOC/DOCX`, `XLS/XLSX`, `PPT/PPTX`, `ODT/ODS/ODP`, `HWP/HWPX`, `TXT`, `RTF`, `CSV`, `ZIP/7z` preview; thumbnails, page jump, text search, outline/TOC, bookmarks, password modal, **Continuous Scroll**, **Reflow/Reading Mode**, **Dark/Night Mode**, **TTS Read-Aloud**, Room history/favorites | `.rar` archives; server-side rendering |
| **Office Editing** | Rich DOCX text/paragraph/list/table styling, XLSX sheets/formulas/cell editing, PPTX slide editing & notes via `LibreOfficeKit`, Track Changes / Comments where supported by LOK, **zero `SAVE_HEADER` lock traps** | Real-time multiplayer co-editing |
| **PDF Power Tools** | View/Annotate (brush, highlight, underline, strike, text, e-Sign), Merge PDF, **Real Offline Split PDF**, **PDF Compress**, **PDF Watermark**, **Print Tool (`PrintManager`)**, Image->PDF, Text->PDF, Extract/Long Image, Add/Remove Password (`AES`), **Offline PDF->DOCX** | Cloud conversion APIs |
| **Scan, OCR & QR** | Multi-page camera scan, **Dual-Side ID Card Scan**, perspective crop, image filters, offline OCR text copy/export, QR/Barcode scanner | Server OCR |
| **Ecosystem & Storage** | `MANAGE_EXTERNAL_STORAGE` device file index + SAF fallback, Wi-Fi File Transfer (`Web HTTP` + `FTP`), Google Drive + OneDrive (`MSAL`), Jetpack Glance home widgets | Any `SYSTEM_ALERT_WINDOW` / lockscreen overlay or Cloud AI assistant |

---

## 5. Compressed 4-Week Execution Roadmap & External Gate Reality (`CD-02`)

**CEO Mandate:** Complete full `1.0.0` (`versionCode 100`) engineering build, ASO metadata, and store screenshots within **1 month (~4 weeks)** using AI-accelerated multi-agent workflows.

| Week | Lifecycle Phases | Core Deliverables & Parallel Critical Path | Exit Gate |
|---|---|---|---|
| **Week 1** | **Phase 3 (PO/BA) + Phase 4 (UI/UX) + Toolchain Spike** | `03_PRD.md`, `04_USER_STORIES.md`, `05_DESIGN_SYSTEM.md`, Pencil/HTML wireframes -> **CEO Gate 2 Visual Approval**. Start parallel isolated **Linux CI build spike for `LibreOfficeKit` NDK r27+ 16 KB** inside `.local/spike/` (no product Kotlin files until Phase 6). | **Gate 2 Sign-Off** + LOK ARM64 `.so` compilation validated |
| **Week 2** | **Phase 5 (Sprint Plan) + Phase 6 Core Architecture & Viewer** | `06_SPRINT_BACKLOG.md`, `07_TECH_SPEC.md`, `LLM.md` §2–§10, Gradle multi-module MVI setup, Room DB, Storage Scanner, `PdfiumAndroid 16KB` viewer, `PDFBox-Android` suite, ML Kit Scan/ID Card/OCR/QR, `ZIP/7z`, `hwplib/hwpxlib` offline bridge. | Core Viewer + PDF/Scan modules pass unit & Compose UI tests |
| **Week 3** | **Phase 6 Editing, Cloud, Transfer, Billing & Integration** | Integrate compiled `LibreOfficeKit 16KB` DOCX/XLSX/PPTX/ODF editor + `PDF->DOCX`, Ktor Wi-Fi Transfer, Drive/OneDrive, Glance Widgets, UMP CMP + Play Billing (`1.0.0` / `100`), Baseline Profiles *(AdMob Mediation deferred until all core features are Done per `CD-07`)*. | Feature-complete debug/release `.aab` passing `readelf -l` `16 KB` check |
| **Week 4** | **Phase 7 (QA/Security) + Phase 8 (ASO/Screenshots/Release Prep)** | `08_QA_TEST_REPORT.md` (strict Zero Blocker **CEO Gate 3** on real Android device via ADB), `09_ASO_METADATA.md`, store feature graphic & screenshots, Play policy compliance video (`MANAGE_EXTERNAL_STORAGE`), `10_RELEASE_CHECKLIST.md`. | **Gate 3 Sign-Off** & Signed Release `.aab` ready for Play Console submission |

> [!WARNING]
> **Chief-of-Staff External Calendar Advisory (Zero-Blindside Notice):**  
> While AI-accelerated workflows compress implementation, QA, and ASO asset production into **4 weeks**, two external Google Play timelines sit outside Evora engineering control: (1) **`MANAGE_EXTERNAL_STORAGE` review + Play review** (`~3–7` days typical), and (2) **Google Play 14-day / 12-tester closed test** if published under a newly created Personal Developer account rather than an approved Organization account.

---

## 6. Monetization, Pricing & Ethical UX Charter

Grounded directly in [`../research/phase1/monetization_and_pricing_analysis.md`](../research/phase1/monetization_and_pricing_analysis.md):

1. **Hybrid Revenue Engine (AdMob Mediation + Play Billing):**
   - Android median Day 35 Download-to-Paid is **`0.9%`**, while Trial-to-Paid is **`32.5%–32.6%`**. Roughly **`90%–95%+`** of utility installs never subscribe, making Hybrid monetization mandatory (`25%–40%` AdMob share).
   - **Mediation Stack:** AdMob integrated with **Google Open Bidding + AppLovin MAX / Unity / Mintegral / CAS** (`~30%–50%` eCPM lift over standalone AdMob).
   - **Google Play 2026 Commission Math:** **10% Base Service Fee + 5% Play Billing Fee = 15% Effective Take Rate** (`0.85x` net multiplier on subscriptions and up to `$1M` small-developer non-sub IAP; `0%` Play commission on AdMob revenue).
2. **Strict Anti-Churn Ad & Paywall Rules:**
   - **Banned:** `SAVE_HEADER` edit-ransom locks; `onResume` interstitial spam while returning to a document; lockscreen / `SYSTEM_ALERT_WINDOW` ads; blocking cold launch before opening a file intent.
   - **Placement Rules:**
     - *Collapsible / Adaptive Bottom Banner* in file browser/tabs only (US `$0.40–$1.50` / SEA `$0.10–$0.30`).
     - *Post-Milestone Interstitials* strictly after completed export/merge/scan with enforced cooldown caps (US mediation utility `$4.00–$9.00`; BR `$2.50–$5.00`; SEA `$1.50–$3.50`).
     - *Rewarded Video Value Unlocks* for non-subscribers accessing heavy batch exports, OCR batches, or temporary 24h Pro Pass (US `$14.00–$25.00+`; BR `$5.00–$9.00`; SEA `$3.00–$7.00`).
     - *Pro Subscription & Micro-IAP SKUs (`[Internal Target]`):* Geo-tiered **Weekly** (3-day trial), **Annual** (`$39.99–$47.99/yr` Tier-1 anchor, 7-day trial), and **Lifetime Ad-Free / Pro Unlock** (`$2.99–$9.99` Tier-2/3 micro-IAP up to `$59.99` Tier-1).

---

## 7. Locked UA Budget & Financial Guardrails (`Tier B: $1,000–$5,000/mo`)

From [`../research/phase1/ua_benchmarks_and_economics.md`](../research/phase1/ua_benchmarks_and_economics.md) and [`../research/phase1/monetization_and_pricing_analysis.md`](../research/phase1/monetization_and_pricing_analysis.md):

- **Approved Launch Tier (`CD-03`): Tier B (`$1,000–$5,000/mo`) — Validation & Hybrid Test**
  - **Allocation:** **60% Google App Campaigns (GAC)** strictly segmented by geo tier (**never** mix US/Tier-1 with Brazil/SEA/India inside one `tROAS`/`tCPI` campaign) + **40% ASO & Store Listing CVR Experiments**.
  - **Bidding Progression:** Launch on `tCPI`/`tCPA` proxy events (`completed_core_action`), then pass Firebase `ad_impression` + IAP/trial events into Google Ads to graduate to `tROAS` (`10%–30%` initial target) once hitting `>= 10–20` value events/day.
  - **Seasonality Buffer:** Maintain a **`>= 20%` margin buffer** for Q1 AdMob eCPM compression (`-20%` to `-40%` vs Q4).
- **Sensitivity Targets (`[Estimate]`):**
  - *Tier-1 Productivity Target:* `$2.00` Paid CPI (`$1.40` Blended CAC w/ 30% ASO); Base (`1.8%` Paid Conv @ `$39.99/yr` gross / `$33.99` net + `$0.14` AdMob = **`$0.75` D90 LTV**) requires ~62% organic share; Strong Niche (`3.0%` Paid Conv @ `$47.99/yr` gross / `$40.79` net + `$0.18` AdMob = **`$1.40` D90 LTV**) clears break-even at ~30% organic lift.
  - *Tier-2/3 Arbitrage Target:* Brazil Paid CPI `$0.35` / SEA `$0.20` (Blended `$0.18`); **`$0.105` D90 LTV** (`$0.069` D30 AdMob + 40% tail + `0.3%` `$2.99` micro-IAP) requires Tier-2/3 paid CPI `<= $0.10–$0.14` unless engagement lifts impressions safely without hurting D7 retention (`10%–18%` benchmark).

---

## 8. Compliance, Security & Clean-Room Engineering Constraints

1. **Google Play 2026 Technical Compliance:**
   - `minSdk = 26`, **`targetSdk = 36` (Android 16)**, `versionName = "1.0.0"`, `versionCode = 100`.
   - **Strict `16 KB` Native Page Alignment (`0x4000`)** on every `.so` in `.aab`, verified automatically via `readelf -l`.
   - **`MANAGE_EXTERNAL_STORAGE` Core Declaration + Recorded Policy Video** proving all-files document management utility.
   - **Google UMP SDK (CMP)** initialized strictly prior to Mobile Ads SDK across EEA/UK/Switzerland.
2. **OSS Licensing & Clean-Room Isolation:**
   - Zero `AGPLv3` libraries anywhere in dependency graph.
   - Maintain public disclosure build recipe/source repository for `LibreOfficeKit` (`MPLv2` / `LGPLv3`) + in-app open-source attribution screen so proprietary Kotlin/Compose code stays closed-source.
   - Strict Chinese Wall between `.local/competitor_apk/` research files and clean-room app source.
3. **Architecture & Pre-Code Gate (`LLM.md` §10):**
   - Product Kotlin source files remain **blocked** until Phase 5 (`06_SPRINT_BACKLOG.md`) and Phase 6 (`07_TECH_SPEC.md` + `LLM.md` §2–§10) are generated.
   - All future ViewModels must enforce `MviViewModel<S, I, E>`, single `onIntent`, separate `XContract.kt`, `launchSafely`, `CollectEffects`, navigation via `Effect`, and zero `android.*`/`androidx.compose.*` imports inside ViewModels/Contracts.

---

## 9. Risk Register & Mitigations

| ID | Risk | Severity | Likelihood | Mitigation |
|---|---|---|---|---|
| `R-01` | **LibreOfficeKit NDK r27+ 16 KB cross-compile jeopardizes 4-week timeline** | Critical | Medium | Local NDK r28b (`28.2.13676358`) `16 KB` alignment verified in `.local/spike/r01_ndk16kb_check/`; execute full `CPAndroid` ARM64 build on **GitHub Actions Linux Runner (`CD-08`)** due to `12 GiB` free local SSD limit, verify `0x4000` on `SM_A165F`, and surface any C++ blocker at Gate 2. |
| `R-02` | **AAB/APK size bloat (`LibreOfficeKit ~30+ MB` + `Pdfium` + `ML Kit`) hurts SEA/BR CVR** | High | Medium | Ship `.aab` ABI splits, vector drawables, Play Feature Delivery for Editor module if needed, and on-demand ML Kit download models. |
| `R-03` | **Google Play rejects `MANAGE_EXTERNAL_STORAGE` or stalls in 14-day closed test** | Critical | Medium | Foreground All-Files Manager UX clarity + English Compliance Video + SAF fallback. Per `CD-09`, Play Console is a **Personal account** (14-day / 12-tester closed test) and account-level logistics are ignored/excluded from engineering execution. |
| `R-04` | **Offline `HWP/HWPX` & scanned `PDF->DOCX` layout edge cases (`CD-04` `$0` Server)** | Medium | High | Test top Korean `.hwp`/`.hwpx` & multi-column PDF templates in Week 2; pair `hwplib`/`hwpxlib` HTML/LOK bridge with ML Kit OCR -> `.docx` structured writer. |
| `R-05` | **User expectation mismatch on missing `.rar` & AI features** | Low | Low | Explicitly describe supported archives (`ZIP, 7z`) in Play Store listing & UI; keep zero fake AI placeholders. |
| `R-06` | **Q1 AdMob eCPM compression (`-20%–40%`) erodes Tier-2/3 ROAS** | High | High | Strict geo-separated GAC campaigns + `>= 20%` seasonal ROAS buffer + Tier-1 Annual subscription anchor. |

---

## 10. Immediate Phase 3 Activation (`PO / BA`)

Phase 2 Charter is **approved and locked**. Phase 3 (`product-manager` -> `docs/03_PRD.md` and `ba-writer` -> `docs/04_USER_STORIES.md`) is now **UNLOCKED** and proceeds immediately against `1.0.0` (`versionCode 100`), **Track A (`$0` Server Offline)**, and full competitor parity minus fake AI stubs and `.rar`.
