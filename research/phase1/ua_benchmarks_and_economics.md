# Phase 1 Supporting Report: Mobile Market & UA Benchmarks (Android Non-Game)

**Prepared by:** UA & Trend Research Team (`trend-researcher`, `growth-hacker`)  
**Date:** 2026-11-25  
**Scope:** Native Android Non-Game Applications (Global Tier-1 + Tier-2/3 Dual-Market Strategy)  
**Status:** Active Phase 1 Evidence Base

---

## 1. Macro Market Shift (2025–2026)

1. **Non-Game Apps Overtook Mobile Games in Global IAP Spending**
   - Global consumer IAP spending across iOS and Google Play reached **$167 billion** (+10.6% YoY), while cross-store downloads grew only **~0.8%** to **~150 billion** (*Sensor Tower State of Mobile*).
   - Non-game app IAP revenue jumped **~21% YoY** to roughly **$85 billion** (nearly 3x five years prior), surpassing mobile gaming IAP revenue for the first time (*Sensor Tower*).
   - **Strategic Implication for E05:** Download volume alone no longer builds sustainable mobile businesses. Winners optimize **monetization depth, Day 1–30 retention, and LTV:CPI ratio** rather than chasing raw install vanity metrics.

2. **Generative AI Boom vs. Retention Reality**
   - Generative AI app IAP revenue surged **232% YoY** (*Sensor Tower*), pulling high initial revenue per user (+41% initial revenue per customer, Year-1 LTV median **$30.16**, D60 RPI > **$0.63** per *RevenueCat*).
   - **Warning Signal:** AI apps suffer **~30% faster churn** than non-AI apps (*RevenueCat*) because curiosity-driven users cancel once novelty wears off or replicate simple prompts in free tools (ChatGPT, Gemini).
   - **Strategic Implication:** AI should be used as **invisible friction-removal** (OCR parsing, receipt line-item extraction, local speech-to-task structuring) inside an established daily utility workflow—not as a thin cloud-API wrapper.

3. **Android Native Advantage in Utility & System Integration**
   - Android commands dominant share in emerging/Tier-2/3 markets (e.g., ~85%+ in Brazil; high volume across SEA and India) while holding a massive monetizable Tier-1 base (US, UK, CA, AU, EU).
   - Native Kotlin + Jetpack Compose apps gain defensible UX edges over cross-platform wrappers via **sub-second cold start**, **Android Glance home screen widgets**, **ML Kit Document Scanner / Text Recognition v2**, and **lightweight APK footprint**.

---

## 2. Android Non-Game UA Cost (CPI) & Retention Benchmarks

### 2.1 Google Ads App Campaigns (GAC) Android CPI by Category & Geo

| Category / Vertical | US / Tier-1 CPI | Brazil (LATAM) CPI | SEA (ID, VN, TH, PH) CPI | India CPI | Source / Notes |
|---|---|---|---|---|---|
| **Utility (Mass / Tools)** | $1.50 – $3.50 *(arbitrage floor $0.80–$2.50)* | $0.30 – $0.80 *(arbitrage floor $0.08–$0.30)* | $0.15 – $0.45 | $0.05 – $0.20 | Google Ads App Campaigns benchmarks (2025–2026) |
| **Productivity & Business** | $2.00 – $4.50 | $0.30 – $0.80 | $0.15 – $0.45 | $0.05 – $0.20 | Google Ads App Campaigns benchmarks (2025–2026) |
| **Photo & Video Tools** | $1.20 – $2.50 | ~$0.25 – $0.70 *(estimate)* | ~$0.15 – $0.40 *(estimate)* | ~$0.05 – $0.18 *(estimate)* | Adjust / Industry UA benchmarks |
| **Health & Fitness** | $1.50 – $4.00 *(Q1 spikes $2.00–$5.00+)* | ~$0.40 – $1.00 *(estimate)* | ~$0.20 – $0.60 *(estimate)* | ~$0.08 – $0.25 *(estimate)* | AppsFlyer / Adjust benchmarks |
| **Finance / Fintech** | $3.50 – $12.00+ | $0.50 – $2.00 *(EM range)* | $0.50 – $2.00 *(EM range)* | ~$0.30 – $1.00 *(estimate)* | AppsFlyer benchmarks |

*Note: Global blended Android non-game CPI across mixed geographies averages ~$1.00–$1.50 (Adjust).*

### 2.2 Android Non-Game Retention Benchmarks (D1 / D7 / D30)

| Vertical | Day 1 (D1) | Day 7 (D7) | Day 30 (D30) | Key Retention Driver / Risk | Source |
|---|---|---|---|---|---|
| **Productivity & Utility** | 24% – 35% | 10% – 18% | 5% – 12% | Habit/workflow anchoring; strong D30 when integrated into daily work | AppsFlyer / Adjust |
| **Finance / Personal Budget** | 22% – 28% | 10% – 12% | 5% – 7% | High intent, recurring financial check-ins | AppsFlyer |
| **Health & Fitness** | 19% – 28% | 8% – 14% | 3% – 8% *(Top quartile sub: 10–14%)* | Steep D1–D7 "intent fatigue"; streak loops & doctor-export value boost D30 | AppsFlyer / Adjust |
| **Photo & Video** | 20% – 25% | 7% – 10% | 3% – 5% | Single-use "utility of convenience" churn once edit completes | Adjust |
| **E-Commerce / Shopping** | 12% – 18% | 5% – 6% | 2% – 3% | Heavy remarketing reliance; poor fit for small native utility studio | AppsFlyer |

*Android Churn Reality:* Industry telemetry shows **~45%+ of Android installs are uninstalled within 30 days**, making first-48-hour onboarding activation critical.

---

## 3. UA Arbitrage & ROAS Payback Benchmarks

### 3.1 AdMob Arbitrage vs. Hybrid/Subscription ROAS Curve

| Monetization Path | D7 ROAS Benchmark | D30 ROAS Benchmark | Target Breakeven (100% ROAS) | Viable LTV:CAC Target | Source |
|---|---|---|---|---|---|
| **AdMob Arbitrage (Mass Utility)** | Avg: 15% – 30%<br>Top-tier: 35% – 50%+ | Avg: 50% – 80%<br>Profitable scale: 100% – 120%+ | **Day 30 – Day 60** | >= 3:1 on D30/D90 cohorts *(narrower arbitrage margin acceptable at fast D30 turn)* | UA Arbitrage / GAC Benchmarks |
| **Paid Subscription / Hybrid Utility** | 15% – 40% | 50% – 80% | **Day 60 – Day 120** *(<= 3–4 months)* | **>= 3:1** on mature D90+ cohorts | AppsFlyer / Hybrid Utility Benchmarks |

### 3.2 Google App Campaigns (GAC) Mechanics for Android
1. **Strict Geo-Tier Campaign Separation:** Never combine Tier-1 (US/UK/AU) and Tier-2/3 (Brazil/SEA/India) inside one tROAS or tCPI campaign. AdMob eCPM differs by **10x–20x** between US and India; blended bidding causes Google ML to dump budget into cheap Tier-3 installs with low monetization yield.
2. **Bidding Progression:**
   - **Launch Phase (< 10–20 conversion events/day):** Start with `tCPI` or `tCPA` on a proxy activation event (e.g., `completed_core_action`).
   - **Scale Phase (>= 10–20 value events/day; ideally 50–100/day for fast learning):** Pass Firebase/MMP **AdMob impression-level ad revenue** (`ad_impression`) + IAP/trial events into Google Ads and switch to **tROAS**, starting conservatively at **10%–30% D7/window target** so delivery does not choke.
3. **Attribution Lag & Seasonality Guardrail:** GAC has a **24–48 hour** conversion attribution lag, and AdMob eCPMs drop **20%–40% in Q1** vs. Q4. Always build a **20% seasonal margin buffer** into ROAS scaling gates.

---

## 4. UA Strategy Playbooks by Budget Tier

| Budget Tier | Monthly Spend | Recommended Acquisition Mix | Best-Fit App Categories | Expected Bottlenecks |
|---|---|---|---|---|
| **Tier A: Bootstrapped / Organic-First** | $0 – $500 / mo | 100% **ASO** (long-tail high-intent keywords, localized listings, Custom Store Listings), Reddit/community seeding (e.g., r/ADHD, r/freelance), Glance widget word-of-mouth | Hyper-niche offline utilities (Offline Receipt/Tax Tracker, Vertical PDF Redactor, ADHD Focus Friction, Niche Calculators) | Slow initial velocity; must pick low-keyword-difficulty vertical terms rather than broad head terms ("PDF scanner", "habit tracker") |
| **Tier B: Validation & Hybrid Test** | $1,000 – $5,000 / mo | **60% GAC** (split Tier-2/3 volume test + targeted Tier-1 high-intent test) + **40% ASO / CVR iteration** (Store Listing Experiments) | Hybrid PDF/Doc Tools, Specialized Health Trackers (Blood Pressure/Medication/Rehab), Screen-Time Friction Apps | Insufficient daily conversion volume in Tier-1 for GAC tROAS (<10 conversions/day at $2.50 CPI -> need careful event proxy selection) |
| **Tier C: Scale & Arbitrage Engine** | $10,000+ / mo | Multi-geo segmented **GAC tROAS** + **Meta/TikTok UGC video creatives** + Mediation optimization (AppLovin MAX / AdMob Open Bidding / CAS) | High-frequency AdMob/Hybrid Utilities, AI Photo/Doc Workflows, Broad Habit/Wellness Subscriptions with proven D30 ROAS >= 80–100% | Creative fatigue (needs weekly UGC refresh), Q1 eCPM compression (-20–40%), cash-flow lockup if payback > 90 days |

---

## 5. ASO & Store Conversion Synergy (Android)

- Google Play ranking weighs **post-install retention, crash/ANR rates (Android Vitals), and engagement velocity** alongside metadata. Keyword stuffing fails; semantic intent and store **Conversion Rate (CVR)** drive organic lift.
- Paid UA boosts keyword install velocity, lowering **blended CAC** when Store Listing CVR is tuned via first-5-second feature screenshots and Play Store A/B tests.
- **Technical ASO Edge:** Keeping APK/AAB lightweight, cold start **< 1.2s**, and ANR/crash rate minimal directly protects Google Play organic discoverability.
