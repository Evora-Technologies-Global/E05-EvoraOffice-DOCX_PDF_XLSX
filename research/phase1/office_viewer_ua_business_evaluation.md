# Phase 1 UA & Business Evaluation: `com.officedocument.word.docx.document.viewer`

**Project:** E05 — Native Android Application (Kotlin + Jetpack Compose, MVI + Clean Architecture)  
**Company:** Evora Technologies Global  
**Prepared by:** UA, Trend Research & Pricing Review (`trend-researcher`, `pricing-analyst`, `reality-checker`)  
**Date:** 2026-11-25  
**Target Benchmark:** `com.officedocument.word.docx.document.viewer` (*Word Document: DOCX, PDF, XLSX* / *Office App - DOCX, PDF, XLSX*)  
**Status:** Active UA Business Assessment (REA Verification Complete — see [`../../docs/01_COMPETITOR_REVERSE_ENGINEERING.md`](../../docs/01_COMPETITOR_REVERSE_ENGINEERING.md))

---

## 1. Executive Summary & Business Verdict

**UA & Business Verdict: `CONDITIONAL GO (High-Volume Evergreen Market / High Execution & Red-Ocean Risk)`**

1. **Proven Massive Demand:** The CEO-selected benchmark app has accumulated **94,749,451 installs** (`50,000,000+` Google Play badge) since launching on **Jan 27, 2020** (~6.8 years, averaging **~1.16M installs/month** across its lifecycle) and remains actively maintained (latest Play Store update **Nov 23, 2026**).
2. **Clear UX & Trust Vulnerability in the Leader:** Despite scale, the target app holds **11.9% 1-star ratings globally** (`45,083 / 378,971`; `4.4★`) and **17.1% 1-star ratings in Vietnam** (`64,770 / 378,971`; `4.2★`). User reviews reveal severe dissatisfaction around **app-switch interstitial spam**, **lost document edits / crashes**, **paywalled Save/Export after editing**, and **invasive permissions**.
3. **Why This Is NOT a Simple Utility Clone:** Reaching **100% feature parity** (*DOC/DOCX editing + Track Changes/Comments/Spellcheck, XLS/XLSX editing + charts/formulas/merge, PPT/PPTX editing + shapes/arrange, PDF annotation + e-Signature, OCR scanner, plus HWP/ODT/ZIP*) requires either a **substantial native/document-engine engineering budget** or **commercial SDK licensing**, alongside strict Google Play `MANAGE_EXTERNAL_STORAGE` review compliance.
4. **Unit Economics Gate:** Generic Android document keywords are heavily saturated by 50M–1B+ incumbents (*WPS Office*, *Microsoft Word*, *Google Docs*, *Simple Design Document Reader*, *Polaris Office*). Scaling profitably requires a **dual-market Hybrid engine** (AdMob Mediation + clean Tier-1 subscriptions/IAP without deceptive save-locks) **and** a realistic R&D/SDK budget verified via **REA APK decompilation** before committing Phase 2 Charter scope.

---

## 2. Target App Profile (Verified Google Play Store Telemetry)

*Source: Direct inspection of Google Play Store listings (`hl=en&gl=US` and `hl=vi&gl=VN`) on 2026-11-25.*

| Metric | Observed Value (EN / US) | Observed Value (VI / VN) | Strategic Implication |
|---|---|---|---|
| **Package ID** | `com.officedocument.word.docx.document.viewer` | `com.officedocument.word.docx.document.viewer` | Long exact-match keyword package URL for ASO |
| **Store Title** | *Word Document: DOCX, PDF, XLSX* | *Office App - DOCX, PDF, XLSX* | Localized head-keyword title optimization |
| **Developer** | `iKame Applications - Begamob Global` *(Begamob Global Limited)* | Same | Top-tier Vietnamese app publisher (strong UA, A/B testing & mediation ops) |
| **Release / Last Update** | Released **Jan 27, 2020** / Updated **Nov 23, 2026** | Same | Actively maintained 6.8-year-old live asset |
| **Cumulative Installs** | **94,749,451** (`50,000,000+` badge) | Same (`50 Tr+`) | ~1.16M historical installs/month average |
| **Version & SDK Floor** | Version code `311200` (reviews up to `311702`); **Min Android 8.0 (API 26)**; **Target API 35** | Same | Targets API 35 (E05 must target API 36 + 16 KB ELF alignment) |
| **Rating & Distribution** | **4.4★** (`4.396`) across **378,971** ratings<br>5★: `302,957` (79.9%) · 4★: `18,481` · 3★: `7,331` · 2★: `5,065` · **1★: `45,083` (11.9%)** | **4.2★** (`4.221`) across **378,971** ratings<br>5★: `287,403` (75.8%) · **1★: `64,770` (17.1%)** | Sharp bimodal distribution (love free multi-format viewing, hate ads/lost saves/paywalls) |
| **Monetization** | **Contains ads** + **IAP `$1.99 – $39.99` per item** | **Chứa quảng cáo** + **IAP `44.000 ₫ – 1.050.000 ₫`** | Hybrid model; reviews cite `$4.94` ad-removal & `$43.99` trial/annual charge |
| **Data Safety (Declared)** | No data shared with 3rd parties *(declared)*; collects App activity, App info/perf, Device/other IDs; encrypted in transit | Same | Standard AdMob/Firebase telemetry profile (needs APK verification) |

### Developer Ecosystem Context (`iKame Applications - Begamob Global`)
Google Play "More by developer" listing shows Begamob operates a multi-vertical high-volume utility portfolio rather than a single app:
- `com.casttv.castforchromecast.screencast` (**50,000,000+**)
- `com.casttotv.screenmirroring.smarttv.castvideo` (**10,000,000+**)
- `com.tv.remote.super` (**1,000,000+**)
- `com.authenticator.otp.password`, `com.ikame.bloodpressure.heartrate.monitor`, `com.facescan.health.monitor` (**100,000+** each)

This confirms Begamob runs an **industrialized UA + AdMob Mediation + cross-promotion funnel** (consistent with Vietnam's leading hyper-casual/utility publishing playbooks).

---

## 3. Competitive Landscape & Market Benchmarking

Google Play "Similar apps" and Sensor Tower / AppBrain category benchmarks show a bifurcated market between **Ecosystem Giants** and **Lightweight All-in-One Utility Viewers**:

| Competitor | Package / Publisher | Scale / Rating | Monetization Model | Positioning vs. E05 |
|---|---|---|---|---|
| **WPS Office** | Kingsoft / WPS | **1B+ lifetime**; **1M–10M/mo** downloads; **$1M–$10M/mo** est. rev (*Sensor Tower*) | Freemium + *WPS Premium* subs + WPS AI + Ads | Heavyweight suite; strong cloud/AI, bloated footprint |
| **Microsoft Word / 365** | `com.microsoft.office.word` | **1B+** (`10M+/mo` suite downloads; Top Grossing, *Sensor Tower*) | Microsoft 365 Enterprise/Personal subs + free mobile view/basic edit | Gold standard formatting fidelity, heavy APK, requires Microsoft account for many flows |
| **Google Docs** | `com.google.android.apps.docs.editors.docs` | **1B+** | Free (Google Workspace ecosystem) | Cloud-first; weak for local multi-format (`HWP`, `ZIP`, offline `PDF` tools) |
| **Document Reader - PDF Editor** | `alldocumentreader.office.viewer.filereader` (*Simple Design Ltd.*) | **50M+** (~74M+ est. installs); **~4.7★** (~900K+ reviews) | Hybrid (Ads + IAP/Sub) | **Critical benchmark:** achieves **4.7★** (vs. Begamob's 4.4★/4.2★), proving ad-supported document readers CAN maintain high UX trust |
| **Polaris Office** | `com.infraware.office.link` | **50M+** | Freemium subscription + Ads | Legacy full-editing office suite |
| **Word Document: DOCX, PDF, XLSX** | `com.officedocument.word.docx.document.viewer` (*Begamob*) | **94.7M installs** (`50M+`); **4.4★** EN / **4.2★** VN | Aggressive Ads + `$1.99–$39.99` IAP | Direct target: strong volume & multi-format UX, weak stability & trust |

---

## 4. User Pain Points & Churn Drivers (Voice of Customer)

Inspection of recent EN and VI Google Play reviews (versions `310602` through `311702`) exposes **five repeatable failure modes** driving competitor 1-star reviews:

1. **App-Switch Interstitial Rage (`onResume` Trigger):** Users switching out briefly to copy text from another app report full-screen ads firing every single time they return to the document (`"every time I swap between programs... an ad pops up every single time I swap back"`).
2. **Offline / Pre-Cached Ad Frustration:** Multiple EN and VI reviewers report 30-second unskippable or persistent ads appearing even with Wi-Fi and mobile data turned off (`"quảng cáo quá nhiều dù đã tắt wifi"`).
3. **Lost Work, Crashes & Unexpected Exit:** Severe reliability complaints during multi-page editing (`"đang viết thì app lỗi hay đăng xuất"`, `"Sửa file đc 1 mặt tự dưng lỗi nội bộ thế là mất cả file lẫn thời gian"`, `"LOST MY PROGRESS I WORK IT FOR 3 HOURS"`, `"logs you out of the document after a set time"`). Lack of robust background autosave/draft recovery is a primary churn driver.
4. **Deceptive Save / Export Paywall:** Users report being allowed to spend time editing a document only to hit a subscription paywall when attempting to save or export (`"you can edit a document but not save it thereafter"`, `"have to buy subscription in order to export"`), plus `$43.99` trial cancellation / billing disputes.
5. **Invasive Permission Anxiety:** Users flag excessive permissions on launch (`"requested invasive permissions to view my phone"`), aligned with Play Store listing declarations including `draw over other apps` (`SYSTEM_ALERT_WINDOW`) and `find accounts` (`GET_ACCOUNTS`).

**Strategic Product Wedge for E05:** Matching **100% of competitor file/editing capabilities** while eliminating **crash-induced data loss (atomic autosave)**, **app-switch ad loops**, **surprise save-locks (clear free vs. Pro boundaries upfront + Rewarded unlock option)**, and **unnecessary invasive permissions** directly addresses why Simple Design hits **4.7★** while Begamob sits at **4.2★–4.4★**.

---

## 5. UA Economics, Monetization & Feasibility Modeling (`[Estimate / Illustrative Model]`)

Building on [`ua_benchmarks_and_economics.md`](ua_benchmarks_and_economics.md) and [`monetization_and_pricing_analysis.md`](monetization_and_pricing_analysis.md), Productivity/Utilities is a **high-CPI, dual-market category** where unit economics fail if paid UA is launched prematurely without strong retention and ASO lift:

### 5.1 Android Productivity / Utility Benchmarks
| Geo Tier | Google Ads GAC CPI (Productivity / Utility) | Expected Retention (D1 / D7 / D30) | Primary Monetization Driver |
|---|---|---|---|
| **Tier-1 (US / UK / CA / AU)** | **$2.00 – $4.50** *(Utility floor $1.50–$3.50)* | 24–35% / 10–18% / 5–12% | **Annual/Weekly/Monthly Pro Sub** ($29.99–$49.99/yr or $4.99–$7.99/mo; net **0.85x** after 15% Play fee) + high-eCPM Rewarded ($14–$25+) & Interstitial ($8–$14) |
| **Tier-2 (Brazil / LATAM / Mexico)** | **$0.30 – $0.80** | ~22–28% / ~9–14% / ~4–7% *(est.)* | **AdMob Mediation** (Interstitial $2.50–$5.00; Rewarded $5–$9) + low-ticket Remove Ads IAP ($1.99–$4.99) |
| **Tier-2/3 (SEA: VN / ID / TH / PH)** | **$0.15 – $0.45** | ~22–28% / ~8–13% / ~3–6% *(est.)* | **AdMob Mediation** (Interstitial $1.50–$3.50; Rewarded $3–$7) + micro-IAP (`44,000 ₫+`) |
| **Tier-3 (India)** | **$0.05 – $0.20** | High volume, high uninstall rate | **Volume AdMob Mediation** (Interstitial $0.80–$2.00; Rewarded $1.50–$3.50) |

### 5.2 Break-Even Reality Check (`[Estimate]`)
- **Tier-2/3 Paid Arbitrage Risk:** Using baseline AdMob D90 yield (`~$0.07–$0.11 / install` from our Phase 1 monetization model; or `$0.12–$0.18` with high DAU + 30–50% Mediation lift + micro-IAP), paying `$0.25–$0.45` CPI in SEA/LATAM **loses money** unless **either** (a) organic ASO contributes **40%–60%+** of total installs (pulling blended CAC <= `$0.10–$0.15`), **or** (b) repeat daily document sessions push lifetime high-value impressions (Rewarded + post-task Interstitial) to **20–30+ per install** without cratering D7 retention.
- **Tier-1 Paid UA Risk:** At **$2.50–$3.50** Tier-1 CPI, pure AdMob yields only `~$0.10–$0.20`. Break-even within 90 days requires **either** >= **60%+ organic ASO** (`~$1.00–$1.40` blended CAC) paired with `>= 2.5%–3.5%` paid conversion at `$39.99/yr` (`$33.99` net after 15% Play fee), **or** high-urgency weekly/monthly Pro plans targeting PDF/Scanner/Office-Editor intent.
- **Hidden Cost Multiplier — R&D & SDK Licensing:** Unlike a simple media tool (`$0` licensing COGS), if matching the competitor's Office editing engine requires a commercial SDK (e.g., Foxit/Apryse/Nutrient/Aspose at `$5k–$50k+/yr`) or months of C++/NDK LibreOfficeKit stabilization, **fixed upfront capex / annual royalties** directly raise the required break-even scale (potentially requiring 500K–2M+ active users or strong Tier-1 subscription revenue before payback).

---

## 6. Play Store Policy & Technical Go-to-Market Risks

1. **`MANAGE_EXTERNAL_STORAGE` (All Files Access) Gate:**
   - Google Play permits `MANAGE_EXTERNAL_STORAGE` for **Document Management Apps**, **but** requires: (a) file scanning/management across arbitrary folders to be core functionality (not replaceable by SAF/Photo Picker), (b) explicit promotion of this capability in the Play Store description, (c) passing the Play Console Permissions Declaration Form + video review, and (d) prominent in-app disclosure before requesting the system toggle.
2. **Avoid Competitor's Policy Landmines (`SYSTEM_ALERT_WINDOW` / IVT):**
   - Competitor declares `draw over other apps`. Unless a concrete, user-opt-in productivity feature strictly requires an overlay, E05 should **exclude `SYSTEM_ALERT_WINDOW`** to avoid Play Store policy scrutiny and AdMob Invalid Traffic (IVT) risk.
3. **2026 Compliance Baseline:**
   - Mandatory **`targetSdk = 36` (Android 16)**, **16 KB ELF page alignment** on all bundled native `.so` engines, **Google UMP SDK (CMP)** before AdMob init in EEA/UK/CH, and **Organization Play Console account (D-U-N-S)** or 14-day/12-tester closed test.

---

## 7. REA Resolution & Impact on Phase 2 Charter

**Status: COMPLETE** — REA decompilation of the genuine Play Store split APKs is documented in [`../../docs/01_COMPETITOR_REVERSE_ENGINEERING.md`](../../docs/01_COMPETITOR_REVERSE_ENGINEERING.md) and confirms three decisive inputs for CEO Gate 1 & Phase 2 Charter budgeting:
1. **Core Rendering & Licensing Gate:** Competitor embeds **Artifex SmartOffice (`libso.so`) + MuPDF (`libmupdf_java.so`)** (**dual-licensed AGPL v3 / Commercial OEM**). Binary reuse is legally prohibited; E05 must either build cleanly using **$0-royalty open stacks** (`PdfiumAndroid` Apache 2.0 + `PDFBox-Android` Apache 2.0 + **LibreOfficeKit `MPLv2`** or native Compose/`Apache POI` editors + **ML Kit `$0`**) **or** allocate a Commercial Document SDK budget in Phase 2.
2. **Real Parity vs. Marketing Stubs:** Split PDF, Tools Print shortcut, AI Translate/SpellCheck/GenText, ID Card Scan, Track Changes UI, PDF Watermark, and ZIP/RAR/7z extraction are **unwired/`coming_soon` stubs** in competitor code, and **HWP** depends on a server upload (`office-convert.begamob.com`). E05 can ship clean local parity + beat competitor reliability without fake buttons.
3. **Mediation & SKU Blueprint Verified:** Runs `IKSdkController` with **10 ad networks**, **15 Sub + 5 One-Time Google Play Billing `8.0.0` SKUs**, **DEAR event tracking**, **31 Firebase Remote Config experiments**, and exact resume-ad / `SAVE_HEADER` reward-gate triggers confirmed as root causes of 1★ churn.
