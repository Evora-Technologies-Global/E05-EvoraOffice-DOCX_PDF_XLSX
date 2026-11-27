# Phase 1 Market & UA Research Report: Android Non-Game App Opportunities

**Project:** E05 — Native Android Application (Kotlin + Jetpack Compose, MVI + Clean Architecture)  
**Company:** Evora Technologies Global  
**Prepared by:** UA & Trend Research (`trend-researcher`, `growth-hacker`), Pricing (`pricing-analyst`), Reality Check (`reality-checker`)  
**Date:** 2026-11-25  
**Revision Note:** Expanded to 12 non-game verticals across 4 strategic clusters; explicitly **excludes** Note-Taking, Receipt/Billing Scanning, Launchers, and Kill-List apps per CEO directive.

---

## 1. Executive Summary

The global mobile app economy has crossed a structural inflection point: **non-game app IAP spending (~$85B, +21% YoY) surpassed mobile games for the first time**, while total cross-store downloads grew just ~0.8% YoY to ~150B (*Sensor Tower State of Mobile*). For a native Android studio targeting a **dual-market footprint (Tier-1 for IAP/high eCPM + Tier-2/3 for AdMob impression volume)** with **zero-to-low early server/AI API COGS**, winning comes from picking a category with **high daily frequency or urgent problem intent**, **clean AdMob/Hybrid placement ergonomics**, and **low Google Play policy friction**.

Following CEO feedback excluding Note-Taking, Receipt/Billing Scanning, and Launchers, this report evaluates **12 viable Android Non-Game verticals** across four clusters and recommends **4 standout directions** spanning pure utility, high-urgency education, daily habit/parenting, and connectivity diagnostics:

1. **Cluster A Pick — Media Processing Toolbox (Video Compressor, Audio Cutter/Ringtone Maker, ID Photo Maker):** Zero server COGS via **Jetpack Media3 `Transformer`** + **ML Kit Selfie Segmentation**. Huge global search volume, natural Rewarded/Interstitial export hooks, and Pro IAP upsells.
2. **Cluster C Pick — Driving & Certification Test Prep Engine:** Extreme user urgency ("must pass exam soon") yields standout conversion to short-term passes / one-time unlocks ($4.99–$14.99) plus high AdMob mock-exam impressions, easily localized across markets.
3. **Cluster B Pick — Daily Habit / Parenting & Couple Companion (Baby Tracker OR Spiritual/Prayer/Astrology OR Couple Glance Widgets):** Unlocks 3–10 daily sessions for steady AdMob yield plus recurring Yearly/Monthly subscriptions for sync, charts, and ad-free UX.
4. **Cluster A Secondary — Device & Local Diagnostics (Wi-Fi Analyzer, GPS Speedometer HUD, Decibel Meter) OR Aesthetic Glance Widgets/4K Wallpapers:** Pure offline/organic ASO workhorses with low paid UA dependency and strong micro-IAP margins.

### Supporting Phase 1 Reports
- [Competitor Reverse Engineering Blueprint (`com.officedocument.word.docx.document.viewer`)](01_COMPETITOR_REVERSE_ENGINEERING.md)
- [Target Focus UA & Business Evaluation (`com.officedocument.word.docx.document.viewer`)](../research/phase1/office_viewer_ua_business_evaluation.md)
- [Target Focus REA Pre-Audit & Feasibility Checklist (Completed via Device APK Pull)](../research/phase1/office_viewer_rea_pre_audit.md)
- [Expanded 12-Category Android Landscape](../research/phase1/expanded_category_landscape.md)
- [UA Benchmarks & Acquisition Economics](../research/phase1/ua_benchmarks_and_economics.md)
- [Monetization, Pricing & Unit Economics Analysis](../research/phase1/monetization_and_pricing_analysis.md)
- [Reality Check, Policy Traps & Technical Feasibility Audit](../research/phase1/reality_check_and_policy_audit.md)
- [Android Launcher Feasibility Assessment (Evaluated & Deferred)](../research/phase1/launcher_feasibility_assessment.md)

---

## 2. Macro Market & Android UA Benchmarks (2025–2026)

### 2.1 Platform Monetization & Retention Realities
- **Android Subscription Funnel (*RevenueCat State of Subscription Apps*, 115k+ apps / $16B revenue):**
  - Median Day 35 Download-to-Paid is **0.9% on Android** vs. **2.6% on iOS** (~3x top-of-funnel gap).
  - Once users start a free trial, **Trial-to-Paid converts identically (~32.5% Android vs. ~32.6% iOS)**.
  - Hard paywalls convert ~5x better than freemium (**10.7% vs. 2.1%** at D35), but trigger 1-star ratings and kill Tier-2/3 ad volume on Android utilities. **Hybrid (Rewarded feature unlock + Post-action Interstitial + Ad-Free/Pro IAP)** maximizes dual-market yield.
  - **~35%** of subscription apps mix recurring plans with lifetime unlocks or consumables.
- **Google Play 2026 Commission Structure:**
  - Effective June 30, 2026 (US/UK/EEA; staggered globally through Sept 2027), Google Play splits fees into **10% base service fee** (subscriptions & first $1M small-dev non-sub IAP) + **5% Play Billing fee** = **15% effective fee** (net revenue multiplier **0.85x**). AdMob retains zero Play Store commission.

### 2.2 Android Category CPI & Retention Comparison

| Vertical | Tier-1 CPI (Android) | Tier-2/3 CPI (LATAM/SEA/IN) | Day 1 (D1) | Day 7 (D7) | Day 30 (D30) | Source |
|---|---|---|---|---|---|---|
| **Utilities & Productivity** | $0.60 – $2.20 *(GAC up to $3.50–$4.50)* | $0.05 – $0.80 | 25% – 32% | 11% – 15% | 3.0% – 5.0% *(up to 12% workflow)* | AppsFlyer / Adjust / GAC |
| **Photo & Video Tools** | $1.00 – $3.00 | $0.05 – $0.60 *(est.)* | 18% – 24% | 6% – 9% | 2.0% – 3.5% *(up to 5%)* | Adjust / Industry Benchmarks |
| **Music & Audio** | $1.00 – $3.00 | ~$0.10 – $0.60 *(est.)* | 22% – 28% | 10% – 14% | 4.0% – 6.0% | Industry Benchmarks |
| **Health & Fitness** | $1.50 – $4.00 *(Q1 $2–$5+)* | ~$0.10 – $1.00 *(est.)* | 19% – 28% | 8% – 14% | 3.0% – 8.0% *(Top sub 10–14%)* | AppsFlyer / Adjust |
| **Lifestyle / Spiritual** | $1.50 – $4.50 | ~$0.10 – $0.80 *(est.)* | 20% – 25% | 8% – 11% | 3.0% – 4.5% | Industry Benchmarks |
| **Education / Test Prep** | $1.50 – $4.00 | ~$0.10 – $0.80 *(est.)* | 16% – 22% | 6% – 9% | 1.8% – 3.0% | Industry Benchmarks |
| **Entertainment / Media** | $0.80 – $2.50 | ~$0.08 – $0.50 *(est.)* | 18% – 22% | 7% – 9% | 2.5% – 3.5% | Industry Benchmarks |

### 2.3 AdMob Android eCPM Benchmarks by Format & Geo (2025–2026)

| Ad Format | US / Tier-1 | Brazil (Tier-2) | SEA Avg (ID/VN/TH/PH) | India (Tier-3) | Placement Note |
|---|---|---|---|---|---|
| **Banner / MREC** | $0.40–$1.50 *(MREC $1.60–$3.00)* | $0.15 – $0.40 | $0.10 – $0.30 | $0.05 – $0.15 | Bottom dock / list footer |
| **App Open** | $4.00 – $8.00 | $1.50 – $3.50 | $1.00 – $2.50 | $0.50 – $1.20 | Cold/warm resume with cooldown |
| **Interstitial** | $8.00 – $14.00 *(Med. $4–$9)* | $2.50 – $5.00 | $1.50 – $3.50 | $0.80 – $2.00 | Post-task completion only |
| **Rewarded Video** | $14.00 – $25.00+ *(Med. $12–$22+)* | $5.00 – $9.00 | $3.00 – $7.00 | $1.50 – $3.50 | Highest yield & UX satisfaction |

*Mediation (Google Open Bidding + AppLovin MAX / Unity / Mintegral / CAS / Appodeal) typically lifts effective eCPMs by **30%–50%**. Q1 seasonal eCPM dips run **-20% to -40%**.*

---

## 3. Expanded 12-Category Landscape (Shortlist & Trade-Offs)

Full scoring and policy details are documented in [Expanded 12-Category Android Landscape](../research/phase1/expanded_category_landscape.md). Summary ranking across our 4 strategic clusters:

| Rank / Cluster | Category | Best Fit For | Key Policy / Technical Gate | Competitor Targets for REA |
|---|---|---|---|---|
| **#1 (Cluster A)** | **Media Processing Toolbox** *(Video Compressor, Audio Cutter/Ringtone, ID Photo Maker)* | Dual-market AdMob Rewarded + Pro IAP; $0 COGS; viral short-video UA | Use **Jetpack Media3 `Transformer`** & Android **Photo Picker**; ZERO video-downloader features | `Panda Video Compressor`, `InShot` (lite tools), `MP3 Cutter & Ringtone Maker`, `Passport Photo Maker` |
| **#2 (Cluster C)** | **Driving & Certification Test Prep** *(DMV / Theory / Mock Exam)* | High-intent IAP/Pass conversion + clean AdMob mock-exam loop; zero server cost | Original/public-domain exam content; localize via Custom Store Listings | `DMV Genie`, `Driving Theory Test 4 in 1`, regional driving prep leaders |
| **#3 (Cluster A)** | **Device & Local Diagnostics** *(Wi-Fi Analyzer, Speedometer HUD, Decibel Meter)* | Organic ASO evergreen traffic + zero server COGS + clean IAP Pro | Keep GPS **foreground-only** (no `ACCESS_BACKGROUND_LOCATION`); scrub `QUERY_ALL_PACKAGES` | `WiFiman`, `NetSpot`, `GPS Speedometer - Odometer`, `Sound Meter` |
| **#4 (Cluster B)** | **Baby Tracker** *(Feeding / Sleep / Growth Charts)* | Exceptional Year-1 DAU (10+ opens/day) + Parent Subscription LTV | Optional caregiver cloud sync; clean pediatric PDF export | `Huckleberry`, `Baby Tracker - Newborn Log` |
| **#5 (Cluster C)** | **Language Micro-Learning** *(Vocab / Kana / Phonics)* | Highest Subscription share (51–59% Yearly) + Rewarded Lesson unlocks | Pre-packaged local audio/decks keep server COGS ~$0 | `Duolingo` (energy loop), `Drops`, `Busuu` |
| **#6 (Cluster B)** | **Spiritual / Faith Daily OR Astrology / Tarot** | Prayer apps: up to 5 opens/day; Astrology: viral compatibility + Rewarded Tarot | Tasteful ad cadence; precise location disclosure for Qibla | `Muslim Pro`, `Pillars`, `Co-Star`, `Labyrinthos` |
| **#7 (Cluster A)** | **Aesthetic Glance Widgets & 4K Wallpapers** *(Non-Launcher)* | Huge Tier-2/3 AdMob volume + micro-IAP without Launcher gesture bugs | Zero copyrighted anime/celebrity art; UGC moderation if user-uploaded | `Widgetsmith`-style Android peers, `Color Widgets`, `Zedge`, `Backdrops` |
| **#8 (Cluster B)** | **Couple Shared Widgets & Relationship Countdown** | Organic 2-for-1 viral loop via Jetpack Glance widgets | Lightweight sync backend needed for shared live widgets | `Widgetable`, `Paired`, `SumOne` |
| **#9 (Cluster B)** | **Specialized Health** *(BP/Meds Logbook OR Desk Stretch/Posture)* | Top-tier Health Sub conversion (~39.9% trial-to-paid) & Tier-1 eCPM | Strict non-diagnostic disclaimers + health-safe ad settings | `Blood Pressure App`, `MyTherapy`, `Bend` |
| **#10 (Cluster A)** | **TV Remote (IR + Wi-Fi) & Cast to TV** | Massive global Play Store search volume (millions of installs/mo) | **High Trademark Risk** (never imply official brand affiliation/logos) | `Cast to TV - XCast`, `Universal TV Remote Control` |
| **#11 (Cluster C)** | **Mindful Screen-Time Friction & Focus Soundscape** | High Tier-1 LTV ($39.99–$59.99/yr) + Reddit/TikTok organic | Strict `UsageStatsManager` + `<queries>` hygiene (NO `AccessibilityService`) | `ScreenZen`, `AppBlock`, `Tide` |
| **#12 (Cluster D)** | **Visual AI Identifier (Plant/Coin) OR Family GPS Locator** | High viral CTR (Plant) / High sub retention (GPS) | Vision API COGS eats free AdMob margins; `ACCESS_BACKGROUND_LOCATION` heavy Play gate | `PictureThis`, `Life360` |

---

## 4. UA & Monetization Strategy by Budget Tier

| Budget Tier | Monthly UA Spend | Recommended Categories | Acquisition & Monetization Playbook |
|---|---|---|---|
| **$0 – $500 / mo** *(Organic ASO)* | $0 – $500 | **#3 Diagnostics (Wi-Fi/Decibel/Speedometer)**, **#2 Localized Test Prep**, **#7 Aesthetic Glance Widgets**, **#8 Couple Widgets** | Target low-difficulty long-tail ASO keywords + Custom Store Listings; leverage Glance widget word-of-mouth or couple 2-user viral loops; aim for Store CVR >= 25–35% *(internal target)* |
| **$1,000 – $5,000 / mo** *(Validation)* | $1,000 – $5,000 | **#1 Media Toolbox (Video Compressor / ID Photo)**, **#2 Driving Test Prep**, **#4 Baby Tracker**, **#6 Spiritual/Astrology** | Split **2 separate GAC campaigns**: Tier-2/3 (`BR + SEA`) testing AdMob ARPU & D7 ROAS >= 25–35% at $0.15–$0.40 CPI; Tier-1 (`US/UK/AU`) testing `trial_started` / `pro_iap` conversion |
| **$10,000+ / mo** *(Scale & Arbitrage)* | $10,000+ | **#1 Media Toolbox**, **#10 TV Remote/Cast** *(with strict legal review)*, **#5 Language Learning**, **#7 Wallpapers/Widgets** | Switch GAC to **tROAS** (>= 20–50 value events/day), deploy UGC TikTok/Meta video creatives (cuts Android CPI 30–50%), enable Mediation (Open Bidding + MAX/CAS/Appodeal), maintain D30 ROAS >= 80–100% or Payback <= 90–120 days (20% Q1 buffer) |

---

## 5. Financial Feasibility & Play Store Readiness Guardrails

1. **Unit Economics Reminder (`[Estimate / Illustrative Model]`):** Detailed scenario math in [Monetization & Pricing Analysis](../research/phase1/monetization_and_pricing_analysis.md) shows pure Tier-3 AdMob arbitrage yields ~$0.07–$0.11 D30–D90 ARPU/install (vulnerable to Q1 -20% to -40% eCPM drops). Adding a **Tier-1 Pro IAP/Subscription layer** ($3.99–$9.99/mo or $14.99–$39.99/yr/lifetime, netting 85% after Google Play's 15% fee) plus **30%–50% organic ASO share** is what turns blended LTV:CAC cleanly positive (`>= 3:1`).
2. **2026 Compliance Baseline:** Target **Android 16 (API 36)**, verify **16 KB page alignment** for any native `.so` code, use **Photo Picker** (`PickVisualMedia`) instead of broad `READ_MEDIA_IMAGES`, integrate **Google UMP SDK (TCF v2.2 CMP)** before AdMob init in EEA/UK/CH, and plan either an **Organization Play Console account (with D-U-N-S)** or a **14-day / 12-tester closed test** for Personal accounts.

---

## 6. CEO Decision Log & Next Step

| Date | Decision Item | Proposed Options / Constraints | CEO Decision | Notes |
|---|---|---|---|---|
| 2026-11-25 | Target Platform & Architecture | Native Android (Kotlin + Jetpack Compose, MVI + Clean Architecture) | **Confirmed** | Fixed in `CLAUDE.md` / `LLM.md` §13 |
| 2026-11-25 | Market, Budget & Monetization Scope | Non-Game Dual-Market (Tier-1 + Tier-2/3), Flexible UA Budget Tiers, Hybrid Preferred ($0/low COGS) | **Confirmed** | Baseline scope |
| 2026-11-25 | Explicit Category Exclusions | Exclude **Note-Taking**, **Receipt/Billing Scanning**, and **Launchers** (deferred due to QuickStep/Play/AdMob IVT risk) | **Confirmed** | Applied across 12-category matrix above |
| 2026-11-25 | **Selected Target Benchmark App** | Focus E05 on **`com.officedocument.word.docx.document.viewer`** (*Word Document: DOCX, PDF, XLSX* / *Office App* by iKame Applications - Begamob Global) with full feature parity target | **Confirmed by CEO** | UA evaluation (`CONDITIONAL GO`) in `research/phase1/office_viewer_ua_business_evaluation.md`. REA decompilation **COMPLETE** in [`01_COMPETITOR_REVERSE_ENGINEERING.md`](01_COMPETITOR_REVERSE_ENGINEERING.md) (**Artifex SmartOffice + MuPDF AGPL/OEM finding + 100% clean-room parity paths ready for CEO Gate 1**) |
| 2026-11-25 | **CEO Gate 1 & Phase 2 Charter** | Proceed only if verified engineering paths achieve `100%` feature parity with benchmark; lock Engine Track, Phasing/Versioning, UA Budget, and Offline Scope | **Approved by CEO** | Charter locked in [`02_PROJECT_CHARTER.md`](02_PROJECT_CHARTER.md): **Track A (`$0` royalty LOK 16KB + Pdfium 16KB + PDFBox)**, **Amended Option P2 (`v1.0.0` / `versionCode 100`, 4-week target)**, **Tier B (`$1k–$5k/mo`)**, **100% Offline `$0` Server (`HWP` via HTML/LOK, `ZIP/7z` only, Zero Cloud AI)** -> **Phase 3 Unlocked** |
