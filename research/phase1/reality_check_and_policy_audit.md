# Phase 1 Supporting Report: Reality Check, Policy Traps & Technical Feasibility Audit

**Prepared by:** Reality Checker & Technical Risk Team (`reality-checker`)  
**Date:** 2026-11-25  
**Scope:** Google Play Policy Compliance (2025–2026), Account Readiness & Android Native Feasibility  
**Default Assessment Stance:** Skeptical / Evidence-Required (`NEEDS WORK` until mitigated)

---

## 1. Critical Reality Check: Why Indie/Studio Android Apps Die Early

Most Phase 1 market proposals fail not because the UI looks bad, but because they pick categories that hit **Google Play policy walls**, **hidden API restrictions**, or **negative unit economics**. Below is the verified 2025–2026 risk audit for Android Non-Game apps.

---

## 2. Google Play Policy & Technical Landmines (2025–2026)

| Policy / Mandate | What Changed / Enforcement Rule | Categories Most Threatened | Mandatory Mitigation for E05 |
|---|---|---|---|
| **Target API 35 / 36** | New apps & updates must target **Android 16 (API 36)**; existing availability floor is **Android 15 (API 35)** | All apps | Configure `targetSdk = 36` from Day 1 in Phase 6 Gradle setup |
| **16 KB Memory Page Size** | Apps using native C/C++ `.so` libs targeting Android 15+ MUST align to **16 KB pages** or Play Console blocks updates | PDF engines (MuPDF/PDFium), local Whisper.cpp, OpenCV, NDK tools | Pure Kotlin + Jetpack Compose + official Google ML Kit artifacts when possible; verify any NDK `.so` with AGP 8.5+ / NDK r26+ & 16 KB emulator tests |
| **Photo Picker & `READ_MEDIA_IMAGES`** | Enforced **May 28, 2025**: broad image/video read permissions banned unless core gallery/editor app with Play Console declaration | Photo editors, Receipt/PDF scanners, Document tools | Use Android `PickVisualMedia()` & **ML Kit Document Scanner API** (requires **zero camera & zero storage permission**) |
| **`AccessibilityService` Ban for Blockers** | Cannot use `AccessibilityService` or `isAccessibilityTool="true"` for app blocking, screen time, or automation unless core disability aid | Screen Time / App Blockers, Auto-clickers | Build blockers strictly with **`UsageStatsManager` + Foreground Service + `SYSTEM_ALERT_WINDOW`**, prominent disclosure, and opt-in video demo |
| **`QUERY_ALL_PACKAGES` Restriction** | Broad installed-app inventory access rejected unless launcher, antivirus, or core device manager | App Blockers, Cleaners, Launchers | Replace with manifest `<queries>` / filtered launcher intent queries; expect Play review scrutiny |
| **Cleaner / Booster / VPN Crackdown** | Generic "RAM booster / cleaner" rejected under **Minimum Functionality** & **Deceptive Behavior**. `VpnService` restricted to core encrypted tunnels (MASA validation pushed) | Phone Cleaners, Battery Savers, Free Ad-VPNs | **KILL LIST:** Do NOT build Cleaner/RAM Booster/Ad-Arbitrage VPN apps |
| **Generative AI Content Policy** | Mandatory **in-app flagging/reporting UI** (no email/external redirect) + strict harmful output guardrails; dev liable for 3rd-party AI APIs | AI Chatbots, AI Photo/Avatar, AI Note apps | Include native in-app report dialog on any AI output; prefer deterministic on-device ML (OCR/Scanner) over open-ended LLM chat |
| **AdMob UMP & Certified CMP (GDPR)** | Mandatory Google-certified **TCF v2.2 CMP (Google UMP SDK)** before `MobileAds.initialize()` for EEA/UK/CH; failure kills EU monetization | All AdMob / Hybrid apps | Wire UMP SDK initialization gate into app startup repository before any ad request |
| **Personal Account 12-Tester Gate** | Personal accounts (created >= Nov 13, 2023) MUST run **12 opted-in testers for 14 consecutive days** before production access | All new indie/personal launches | Use a registered **Organization Play Console account** (requires **D-U-N-S number**, 1–4 week setup) OR plan a 14-day closed-beta buffer in Phase 8 |

---

## 3. Category Risk & "Kill-List" Audit

### 3.1 Red Zone (REJECTED — High Policy or Economic Failure Risk)
1. **Phone Cleaner / RAM Booster / Battery Optimizer:** Historically high AdMob arbitrage category, now systematically suppressed or suspended under Google Play Deceptive Behavior & Minimum Functionality policies.
2. **Generic "Free VPN" for Ad Arbitrage:** Extreme compliance burden (`VpnService` policy + MASA security audit) plus server bandwidth COGS that crush Tier-2/3 AdMob margins.
3. **Generic AI Chatbot / Cloud LLM Wrapper:** High API COGS per user makes AdMob negative-margin; 30% faster churn (*RevenueCat*) as users default to free official ChatGPT/Gemini apps; mandatory AI moderation liability.
4. **Basic Calculator / Flashlight / QR-Only Scanner:** Built-in OEM/OS features dominate; Google Play Minimum Functionality rejects thin single-screen wrappers unless deeply differentiated.

### 3.2 Amber Zone (VIABLE WITH STRICT GUARDRAILS)
1. **Mindful Screen-Time / ADHD Focus Blocker:** High Tier-1 willingness-to-pay ($39.99–$59.99/yr), strong organic Reddit/social demand, **but** requires clean engineering without `AccessibilityService` or `QUERY_ALL_PACKAGES`, plus clear prominent-disclosure UX to pass Play Console App Content review.
2. **Health Metric / Blood Pressure / Symptom Tracker:** High Tier-1 AdMob eCPM & strong 39.9% trial-to-paid subscription health (*RevenueCat*), **but** must stay strictly framed as a *personal logbook/export tool* (zero diagnostic/medical claims) and handle sensitive health ad-personalization restrictions carefully.
3. **On-Device AI Voice-to-Task Note App:** Avoids cloud LLM COGS if on-device, **but** Google's Gemini Nano (`AICore`) is restricted to flagship hardware, requiring fallback to local speech/transcription (e.g., Android SpeechRecognizer / bundled NDK Whisper aligned to 16 KB pages) for mid-range Android devices.

### 3.3 Green Zone (HIGHEST POLICY SAFETY + NATIVE ANDROID FIT)
1. **Vertical / Offline-First Document, PDF & Receipt Workflow Utility:**
   - Uses **ML Kit Document Scanner API** (no camera permission needed!) + **ML Kit Text Recognition v2** (100% offline OCR) + **Android Photo Picker** (no `READ_MEDIA_IMAGES` needed!).
   - Zero server/AI API COGS, dual-market appeal (Tier-1 Pro workflows + Tier-2/3 document tools), clean Hybrid monetization (Rewarded Video for batch/OCR unlocks + Pro IAP/Sub).
2. **Privacy-First Offline Micro-Budget & Freelancer Cash-Envelope Tracker:**
   - Zero bank-sync APIs (no Plaid fees/compliance), zero dangerous permissions, clean Room DB + Compose Charts + Glance home screen quick-add widget.

---

## 4. Reality Check Verdict on Initial Phase 1 Readiness

- **Market Research Data Quality:** Grounded in Sensor Tower, Adjust, AppsFlyer, RevenueCat, Google Play Developer Policy docs, and Google Ads GAC benchmarks (`PASS`).
- **Missing Step Before Phase 2 Charter (`NEEDS WORK`):** `docs/01_COMPETITOR_REVERSE_ENGINEERING.md` cannot be finalized until the CEO selects 1–2 candidate categories from `docs/01_MARKET_RESEARCH.md` so `app-reverse-engineer` can pull and inspect real top competitor APKs in `.local/` via REA MCP.
