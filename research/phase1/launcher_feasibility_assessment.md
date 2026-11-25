# Phase 1 Technical & Policy Note: Android Custom Launcher Feasibility

**Prepared by:** Reality Checker, UA & Android Technical Architecture Review  
**Date:** 2026-11-25  
**Status:** Exploratory Evaluation (Addendum to `docs/01_MARKET_RESEARCH.md`)

---

## 1. Google Play Store Review & Policy Risk: **HIGH SCRUTINY**

Launchers are legally allowed on Google Play, **but** they sit in one of the highest-rejection, highest-enforcement categories for both Play Console review and AdMob monetization:

1. **`QUERY_ALL_PACKAGES` Mandatory Declaration & Audit:**
   - Standalone Home Launchers (`CATEGORY_HOME`) are one of the few categories explicitly permitted to request `QUERY_ALL_PACKAGES`, **provided** app-drawer discovery is the app's singular core functionality.
   - **Common Rejection Traps:** Missing or vague Play Console Permissions Declaration forms, missing screen-recording video proving home-screen/app-drawer functionality, or stale AAB artifacts in inactive/closed testing tracks containing undeclared permissions.
   - If the app looks like a wallpaper/utility/theme pack masquerading as a launcher, Google rejects `QUERY_ALL_PACKAGES`.
2. **Repetitive Content & Metadata Spam Crackdown:**
   - Google Play's **Repetitive Content** policy bans publishing multiple theme/iOS-clone launcher APKs with swapped assets. All themes/styles must live inside a single consolidated app.
   - Title/description keyword stuffing (e.g., `"OS Launcher 2026 Fast Cool Theme"`) triggers Metadata Spam rejections.
3. **AdMob & Disruptive Ads / IVT Traps (Critical for AdMob Strategy):**
   - **Out-of-Context / Lockscreen Ban:** Ads must only render inside explicit foreground app UI screens (never injected onto the system lockscreen, over other apps, or popped unexpectedly when pressing the Home button or waking the screen).
   - **Accidental Clicks / Invalid Traffic (IVT):** Home screens pack dense touch targets (icons, dock, widgets). Placing AdMob banners near app grids or docks causes accidental taps, triggering **AdMob IVT ad-serving limits or account suspension**.
   - **Why Market Leaders Avoid AdMob:** Minimalist/pro launchers like *Niagara Launcher* explicitly avoid ads and monetize via **Pro Subscriptions / Lifetime IAP** (~$30 benchmark), because home-screen ad UX destroys retention.

---

## 2. Technical Complexity (Kotlin + Jetpack Compose): **HIGH**

Compared to standard utility/workflow apps, building a production-grade Android Launcher requires deep system-level Android framework mastery:

1. **Gesture Navigation & QuickStep Limitation (Unfixable by 3rd-Party Apps):**
   - Modern Android full-screen gesture navigation and the Recents screen are tightly coupled to **QuickStep** inside the privileged SystemUI/OEM launcher package.
   - Google does not expose a full public animation pipeline to third-party launchers (`GestureNavContract` only mitigates partial icon transitions).
   - On major OEM skins (**Xiaomi HyperOS, Samsung One UI, OnePlus OxygenOS**), setting a third-party launcher often forces fallback to **3-button navigation** or produces visible animation stutter/lag when swiping Home on Android 15/16. Root workarounds (`QuickSwitch`) are largely broken on Android 15–16.
2. **`LauncherApps` & Android 15–16 Private Space / Work Profiles:**
   - Cannot rely solely on legacy `PackageManager` queries. Must integrate **`LauncherApps` + `UserManager`**, handle `UserHandle` managed Work Profiles with mandatory badged icons (`getBadgedIcon()`), dynamic shortcuts (`ShortcutInfo`), and Android 15/16 **Private Space** lock/unlock lifecycle (`ACTION_MANAGED_PROFILE_UNLOCKED` / `LOCKED`) without leaking private app metadata in search.
3. **`AppWidgetHost` & Cross-Process IPC Fragility:**
   - Hosting third-party widgets requires running an `AppWidgetHost` communicating via Binder IPC with `AppWidgetService`. RemoteView inflation errors, restricted background widget providers on Android 15/16, and context leaks can freeze or crash the launcher UI thread unless carefully sandboxed.
4. **OEM Fragmentation & Default Home Role:**
   - Handling `RoleManager` (`ROLE_HOME`), OEM battery killers, and dozens of vendor-specific drawer/gesture quirks dramatically increases QA matrix cost.

---

## 3. Device Performance & Battery Impact

A third-party launcher stays **persistently resident** as the device Home surface:

- **RAM / Low Memory Killer Pressure:** Poorly managed icon bitmaps, unrecycled grid views, live shaders/blur effects, or heavy `AppWidgetHost` instances inflate resident RAM, causing Android's Low Memory Killer to evict other background apps sooner (noticeable on 4–6 GB RAM Tier-2/3 Android devices).
- **UI Jank / Frame Drops:** Decoding app icons synchronously on the main thread causes 60/120 Hz scrolling stutter. Production launchers require a **2-tier L1 (`LruCache`) + L2 (WebP/PNG disk) async icon cache** invalidated incrementally via `LauncherApps.Callback`.
- **Battery Drain Traps:** Polling background services, wake locks, weather widget background refreshes, or continuous blur/particle animations that fail to pause in `onStop()` quickly put the launcher at the top of Android battery usage warnings—leading to immediate uninstalls.

---

## 4. Strategic Recommendation for E05

- **Verdict vs. Top-3 Picks in `docs/01_MARKET_RESEARCH.md`:** **Do NOT pick a Launcher as E05's first flagship app** if the goal is fast MVP velocity, AdMob/Hybrid profitability, and low policy/OEM risk.
- **If CEO Still Wants a Launcher:** Only pursue a **differentiated productivity/minimalist niche** (e.g., *Niagara*-style focus/ADHD launcher) monetized via **IAP/Subscription (zero home-screen banner ads)**, budget significant QA across Xiaomi/Samsung/Pixel Android 14–16 devices, and accept that OEM QuickStep gesture limitations remain outside third-party developer control.
