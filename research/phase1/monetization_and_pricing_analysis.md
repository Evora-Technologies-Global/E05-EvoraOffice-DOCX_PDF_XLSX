# Phase 1 Supporting Report: Monetization, Pricing & Unit Economics Analysis

**Prepared by:** Pricing & Monetization Analyst (`pricing-analyst`)  
**Date:** 2026-11-25  
**Scope:** AdMob vs. IAP/Subscription vs. Hybrid Monetization for Native Android Non-Game Apps  
**Status:** Active Phase 1 Evidence Base

---

## 1. AdMob vs. IAP vs. Hybrid: Structural Reality on Android

### 1.1 The Android Monetization Paradox
- **Subscription Funnel Reality (*RevenueCat State of Subscription Apps* — 115,000+ apps, $16B revenue):**
  - Median **Day 35 Download-to-Paid** on Android is **0.9%** vs. **2.6%** on iOS (~3x gap).
  - However, **Trial-to-Paid conversion** is virtually identical: **32.5%–32.6%** on both Android and iOS.
  - **Key Insight:** Android users who enter a trial convert just as well as iOS users; the gap lies in **top-of-funnel willingness to initiate trials** and **lower Tier-2/3 purchasing power**.
- **Why Hybrid Wins for Dual-Market Android Strategy:**
  - Roughly **90%–95%+** of Android utility users (especially in SEA, LATAM, India) will never subscribe. A **Hybrid model** captures AdMob revenue from non-payers (**25%–40%** of total app revenue in tuned hybrid utilities per *Appodeal/CAS* benchmarks) while capturing high-margin IAP/subscriptions from Tier-1 power users.
  - Around **35% of subscription apps** now blend recurring plans with lifetime unlocks or consumables (*RevenueCat*).

---

## 2. AdMob Android eCPM Benchmarks by Format & Geo (2025–2026)

| Ad Format | US / Tier-1 | Brazil (Tier-2) | SEA Avg (ID/VN/TH/PH) | India (Tier-3) | Placement Strategy for Utilities |
|---|---|---|---|---|---|
| **Banner (320x50 / Adaptive)** | $0.40 – $1.50 *(MREC 300x250: $1.60–$3.00)* | $0.15 – $0.40 | $0.10 – $0.30 | $0.05 – $0.15 | Docked bottom of viewer/history tabs; avoid accidental tap zones |
| **App Open Ads** | $4.00 – $8.00 | $1.50 – $3.50 | $1.00 – $2.50 | $0.50 – $1.20 | Cold/warm return only with cooldown; never block urgent utility launch |
| **Interstitial (Static / Video)** | $8.00 – $14.00 *(Mediation utility avg: $4.00–$9.00)* | $2.50 – $5.00 | $1.50 – $3.50 | $0.80 – $2.00 | Trigger **post-milestone** (e.g., after PDF export/scan complete), never on startup |
| **Rewarded Video** | $14.00 – $25.00+ *(Mediation avg: $12.00–$22.00+)* | $5.00 – $9.00 | $3.00 – $7.00 | $1.50 – $3.50 | Highest yield & UX safety: unlock batch export, OCR pass, or 24h Pro access |

*Mediation Multiplier:* Relying solely on default AdMob leaves revenue on the table. Adding **Google Open Bidding + AppLovin MAX / Unity / Mintegral / CAS / Appodeal** lifts effective eCPMs by **~30%–50%** (*Playwire / Appodeal*).

---

## 3. Subscription & IAP Benchmarks by Category (RevenueCat)

| Category | Trial-to-Paid / Funnel Highlight | Dominant Billing Cadence | Typical Pricing Range | Retention / Churn Dynamics |
|---|---|---|---|---|
| **Health & Fitness** | Trial-to-Paid ~**39.9%**; RPI **$0.48–$0.63** (D14/D60) | **Yearly (65%–68%)** | $29.99 – $79.99 / yr *(estimate)* | Highest annual commitment when tied to health records/habits |
| **Business & Productivity** | Download-to-Trial ~**9.1%** (Business) | **Monthly (59%–68%)** | $4.99 – $14.99 / mo | Task-oriented flexibility; low churn if replacing desktop workflow |
| **Photo & Video** | Strong viral trial starts | **Yearly (51%–59%)** + **Weekly (~31%)** | $4.99–$7.99/wk or $39.99–$59.99/yr | Weekly plans capture quick payback for one-off editors |
| **Education** | Wide spread top-quartile vs median | **Yearly (51%–59%)** + **Monthly (~30%)** | $29.99 – $89.99 / yr | Content depth & streak motivation decide renewal |
| **AI Apps (GenAI)** | D60 RPI **>$0.63**; Y1 LTV **$30.16** | Weekly / Monthly + Consumable tokens | $7.99–$19.99/mo | **30% faster churn**; API COGS erodes gross margin on free/AdMob tier |

### Paywall & Trial Engineering Rules
1. **Hard vs. Soft vs. Rewarded-Gated:** Hard paywalls convert **~5x higher** than pure freemium (**10.7%** vs **2.1%** at Day 35, *RevenueCat*), **but** hard paywalls kill Android Tier-2/3 AdMob volume and trigger 1-star Play Store reviews on utility apps. **Best Hybrid Solution on Android:** *Soft Value-First Freemium + Rewarded-Video Feature Unlock + High-Intent Hard Gate on Pro Automation/Batch Export*.
2. **Trial Length Trade-off:** Short trials (<= 3 days) are popular for fast ROAS feedback (~46% of apps) but convert at only **~25.5%**, whereas **17–32 day trials** convert at **~42.5%** (+70% higher, *RevenueCat*). Use 7-day trials for annual plans and reserve 3-day trials only for high-urgency weekly plans.

---

## 4. Cost Structure & Google Play 2026 Commission Math

### 4.1 Google Play Store Fee Structure (2026 Rollout)
- **Subscriptions & First $1M Small-Developer Non-Sub IAP:**
  - **10% Base Service Fee + 5% Google Play Billing Fee = 15% Effective Take Rate** (for US/UK/EEA effective June 30, 2026; AU/JP Sept 30, 2026; KR Dec 31, 2026; RoW legacy 15% applies through Sept 30, 2027).
  - **Net Revenue Multiplier on Native Play Billing:** **0.85x** of gross IAP/subscription revenue (up to $1M annual revenue / all subscriptions).
- **AdMob Payout:** Zero Play Store 15% commission on AdMob ad revenue (100% of publisher share deposited directly, subject only to local tax withholding/FX).

### 4.2 COGS Guardrail (Zero/Low Server Cost Advantage)
- **Offline-First Client App (Kotlin + Room + ML Kit On-Device):** Variable COGS per free user = **~$0.00**. Every $0.05 earned via AdMob in India/SEA is pure gross contribution margin.
- **Cloud LLM Wrapper App:** If a free user triggers $0.03–$0.10/month in cloud API inference while generating only $0.02 in Tier-2/3 AdMob eCPM, **unit economics turn negative at scale**. Therefore, cloud AI calls must NEVER be subsidized by AdMob banners; reserve cloud APIs strictly for paid IAP/Subscription tiers or gate them behind high-value Rewarded Ads in Tier-1 only.

---

## 5. Illustrative Unit Economics & Sensitivity Analysis (`[Estimate / Illustrative Model]`)

*The following scenarios combine cited CPI, eCPM, and conversion benchmarks to model break-even feasibility across monetization modes for a recommended **Hybrid Offline Utility / Health-Workflow App**. All projections below are `estimates` for planning and must be validated in Phase 2/UA tests.*

### Scenario 1: Tier-1 Hybrid Utility (US/UK/CA/AU) — Paid GAC + ASO
- **Assumed Blended CPI (Paid + 30% Organic ASO Lift):** Paid CPI = $2.00 -> Blended CAC = **$1.40**
- **AdMob Yield (Free Users, 98.5% of cohort):**
  - Avg D90 impressions per install (conservative utility usage: 6 interstitials @ $10 eCPM + 1.5 rewarded @ $18 eCPM + 20 banners @ $0.80 eCPM)
  - AdMob D90 ARPU = `(6 * 0.010) + (1.5 * 0.018) + (20 * 0.0008)` = $0.060 + $0.027 + $0.016 = **$0.103 / install**
- **IAP / Subscription Yield (Target 2.5% Trial Start x 33% Trial-to-Paid = 0.825%–1.5% Paid Conversion):**
  - At **1.2% D90 Paid Conversion** on **$39.99/yr** (or $6.99/mo avg net after 15% fee = $34.00 net):
  - IAP D90 ARPU = `0.012 * $34.00` = **$0.408 / install** (excl. renewals) -> **Blended D90 LTV = $0.51 / install** (still below $1.40 blended CAC unless conversion hits **>= 3.8%** or CPI <= **$0.80** via ASO/viral!).
- **Sensitivity Takeaway (±20% Price / Conversion):**

| Tier-1 Scenario | Paid Conv % | Net Sub Price | AdMob D90 | Blended D90 LTV | Max Payable Blended CAC (1.0x D90) | Required Organic Share @ $2.00 Paid CPI |
|---|---|---|---|---|---|---|
| **Conservative** | 0.9% *(Android median)* | $25.49 ($29.99 gross) | $0.10 | **$0.33** | $0.33 | ~83% organic (Pure ASO-led) |
| **Base Target** | 1.8% *(Optimized soft+rewarded funnel)* | $33.99 ($39.99 gross) | $0.14 | **$0.75** | $0.75 | ~62% organic or $0.75 niche CPI |
| **Strong Niche (-/+20% test)** | 3.0% *(High-pain B2C/Pro)* | $40.79 ($47.99 gross) | $0.18 | **$1.40** | $1.40 | ~30% organic (Paid UA scalable!) |

**Pricing Verdict for Tier-1:** Pure Paid UA on Android Tier-1 ONLY scales profitably if **either** (a) blended D90 LTV clears **$1.40+** via high-pain niche subscription conversion (>= 2.5–3.0% at $39.99–$49.99/yr), **or** (b) organic ASO supplies >= 60% of total installs.

### Scenario 2: Tier-2/3 Mass Volume (Brazil / SEA) — AdMob-First + Micro-IAP
- **Assumed Paid CPI:** Brazil $0.35 / SEA $0.20 (Blended with 35% ASO = **$0.18**)
- **AdMob Yield (High-engagement daily utility or PDF tool):**
  - Avg D30 impressions per install: 14 interstitials @ $2.80 eCPM + 4 rewarded @ $5.50 eCPM + 40 banners @ $0.20 eCPM
  - AdMob D30 ARPU = `(14 * 0.0028) + (4 * 0.0055) + (40 * 0.0002)` = $0.0392 + $0.0220 + $0.0080 = **$0.069 / install**
  - With D90 tail (+40% residual usage) + 0.3% micro-IAP ($2.99 lifetime remove ads = $2.54 net -> +$0.008) = **$0.105 D90 LTV**.
- **Arbitrage Break-Even Condition:** To hit D30–D60 payback at **$0.10–$0.15 D60 LTV**, Paid CPI in SEA/LATAM MUST stay **<= $0.10–$0.14** (or blended CPI <= $0.10 with ASO), OR session frequency must support **>= 25–30 high-value interstitial/rewarded impressions per install** without destroying D7 retention.

**Strategic Synthesis:** The highest-margin, lowest-risk formula for E05 is **not pure Tier-3 ad arbitrage** (razor-thin margin, vulnerable to Q1 -30% eCPM drops) and **not generic Tier-1 subscriptions** (high $2–$4 CPI). The sweet spot is a **Dual-Engine Niche Utility / Specialized Workflow App**:
1. Monetizes Tier-1 via **$39.99–$49.99/yr Pro Subscription + Lifetime Option** (organic ASO + targeted high-intent UA).
2. Simultaneously monetizes global Tier-2/3 organic + cheap UA traffic via **AdMob Rewarded Unlocks & Post-Milestone Interstitials** with **$0 server COGS**.
