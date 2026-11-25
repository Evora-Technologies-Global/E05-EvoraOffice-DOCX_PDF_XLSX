# Phase 4 UX, Brand & Accessibility Audit — E05 (`Evora Office`)

**Project:** E05 — Native Android Office & PDF Suite (`com.evora.technologies.office`)  
**Prepared by:** UX Researcher (`ux-researcher`), Brand Guardian (`brand-guardian`), UX Architect (`ux-architect`)  
**Date:** 2026-11-25  
**Upstream Inputs:** [`01_COMPETITOR_REVERSE_ENGINEERING.md`](../../docs/01_COMPETITOR_REVERSE_ENGINEERING.md) §3 & §6 · [`01_MARKET_RESEARCH.md`](../../docs/01_MARKET_RESEARCH.md) · [`02_PROJECT_CHARTER.md`](../../docs/02_PROJECT_CHARTER.md) (`CD-05`–`CD-09`) · [`03_PRD.md`](../../docs/03_PRD.md) · [`04_USER_STORIES.md`](../../docs/04_USER_STORIES.md)

---

## 1. Brand Foundation (`CD-05`, `CD-06`, `CD-07`)

- **Launcher / In-App Brand:** `Evora Office`
- **Google Play Store Title:** `Evora Office: DOCX, PDF, XLSX` (`29/30` chars)
- **Visual Direction (`CD-06`):** **Style B — Familiar Vibrant Utility**. Retains instant mainstream file-type recognition (`#2489FF` cobalt accent, format-coded document badges and editor bars, clean white card elevation on cool slate `#F2F4F8`, pastel tool tiles) while upgrading architecture to **Jetpack Compose Material 3** adaptive Light & Dark themes (`dynamicColor = false` so document format colors remain invariant across OEM wallpapers).
- **App Icon / Logo Timing (`CD-06`):** Final app launcher icon & brand logo artwork are deferred to a later milestone. Phase 4 headers and splash views render a clean **geometric monogram placeholder** (`EO` wordmark tile in `#2489FF` rounded square `12dp` radius) clearly labeled as temporary placeholder.
- **Brand Voice & Ethical Trust Promise:**
  1. **Calm, direct utility copy:** Action-first verbs (`"Save"`, `"Split PDF"`, `"Export DOCX"`, `"Restore draft"`); zero fear-mongering countdown timers or misleading fake buttons.
  2. **Zero Deceptive Traps (`CD-07`):** No `"Coming Soon"` stubs anywhere; no `SAVE_HEADER` lock traps after editing; pre-action honest disclosure (`RewardGateSheet`) before gated Pro operations run.

---

## 2. Core UX Personas & Anti-Churn Pain Mapping

Grounded in Phase 1 review synthesis (`11.9%` EN / `17.1%` VN 1-star share in `01_MARKET_RESEARCH.md` & `01_COMPETITOR_REVERSE_ENGINEERING.md`):

| Persona | Primary Usage Context | Competitor Pain Trigger | E05 Phase 4 Design Remediation |
|---|---|---|---|
| **Mobile Field Professional** | Opens WhatsApp/Gmail `.pdf` & `.docx` contracts on commute; signs or edits quick figures | Cold-start & `onResume` interstitial traps; `"can edit but can't save"` `SAVE_HEADER` lock; losing edits on app switch | Direct `ACTION_VIEW` reader entry (`< 1.5s`), zero `onResume` ads in editors, explicit `"Done"` save without paywall, visible `"Auto-saved"` draft badge |
| **Student / Admin Power User** | Merges/splits PDFs, compresses large attachments, scans dual-side ID cards, transfers files to PC | `Split PDF`, `Print`, `ID Card`, `Continuous Scroll`, and `ZIP/7z` show `"Coming soon"` or fail | Dedicated multi-mode `PdfToolConfigRoute`, `Dual-Side ID Card` 2-step viewfinder -> A4 sheet preview, real `Continuous` toggle, LAN `Ktor` web drop UI |
| **Night Reader / Tablet / Accessibility User** | Reads long `.pdf`, `.hwp`, `.epub` reports at night or with TalkBack / large system font | Portrait-locked XML, no Dark Theme, `10sp` `#BBBBBB` text (`1.92:1` contrast), `< 48dp` toolbar buttons, missing `contentDescription` | Adaptive Dark Theme (`#101318`/`#1A1E26`), Reader `Light/Night/Sepia` themes, Reflow + TTS bar, `>= 11sp` labels, `>= 48dp` hit boxes, `100%` WCAG AA pairs |

---

## 3. Competitor UI Audit vs. E05 Upgrades

| Competitor Defect (`01_COMPETITOR_REVERSE_ENGINEERING.md` §3) | Measured Failure | E05 Fix Verified in Phase 4 |
|---|---|---|
| Muted label `#BBBBBB` on `#FFFFFF` | `1.92:1` contrast (fails WCAG `4.5:1`) | Replaced with `#526075` (`6.39:1`) for muted text & `#475569` (`7.58:1`) for secondary text in Light mode |
| Vibrant `#2489FF` used behind `14sp` white CTA text | `3.44:1` contrast (fails normal-text `4.5:1`) | Split token roles: `#2489FF` kept for graphical Brand Accent / FAB (`3.44:1 >= 3.0:1` UI), `#186FD4` (`4.93:1 >= 4.5:1`) used for filled button surfaces & text links |
| Format headers `#FF7D05` (PPT), `#86B918` (EPUB), `#22A8F9` (TXT), `#3CC79A` (Success) with `#FFFFFF` text | `2.14:1 – 2.61:1` (fails AA) | Calibrated solid Light headers to `>= 4.80:1` (`#B85600`, `#5A7D0E`, `#1474B0`, `#1E7D5E`) + pastel tint badges (`>= 6.39:1`) |
| No Dark Theme (`Theme.AppCompat.Light`) | Eye strain at night; inverted PDF hacks | Full Material 3 Dark palette (`#101318` canvas, `#1A1E26` cards, muted dark format headers `>= 13:1` w/ `#F1F5F9` text) |
| Dense editor/bottom nav controls `< 48dp` (`28dp` / `40dp`) | High mis-tap rate on compact phones | Mandatory `48dp x 48dp` minimum Compose `Modifier.minimumInteractiveComponentSize()` / `sizeIn(minWidth = 48.dp, minHeight = 48.dp)` |
| `10sp` sub-minimum caption typography | Poor legibility under Dynamic Type | Raised minimum label token (`labelSmall`) to `11sp` (`lineHeight 16sp`) and body baseline to `14sp–16sp` |

---

## 4. Mathematical WCAG 2.1 Verification Summary

Verified programmatically via [`verify_wcag_contrast.py`](verify_wcag_contrast.py) (`75/75` checks passed, `0` failures):

- **Light Canvas & Card Text:** Primary `#0F172A` (`17.85:1` on Card, `16.21:1` on Canvas), Secondary `#475569` (`7.58:1`), Muted `#526075` (`6.39:1` on Card, `5.80:1` on Canvas).
- **Dark Canvas & Card Text:** Primary `#F1F5F9` (`16.99:1` on Canvas, `15.24:1` on Card), Secondary `#B0BAC9` (`8.52:1`), Muted `#94A1B5` (`6.38:1`).
- **Interactive Borders (WCAG 1.4.11 >= 3.0:1):** Light input border `#7C8BA1` (`3.46:1` on Card, `3.14:1` on Canvas); Dark input border `#68768C` (`3.62:1` on Card, `4.04:1` on Canvas).
- **Format Badges & Headers:** All `8` format families (`Word`, `PDF`, `Excel`, `PPT`, `HWP`, `ODF`, `EPUB`, `TXT/ZIP`) exceed `4.80:1` on Light headers, `6.39:1` on Light tint badges, `13.07:1` on Dark headers, and `6.63:1` on Dark tint badges.
- **Reader Comfort Modes:** Sepia (`#3B2F20` on `#F5ECD8` = `11.08:1`), Night (`#E2E8F0` on `#0F1216` = `15.23:1`).

---

## 5. Ergonomics, Motion & Safe-Area Guardrails

1. **Thumb-Zone Architecture:** Primary Bottom Nav (`Home`, `Recent`, center `56dp` `SCAN` FAB, `Storage`, `Tools`) and Editor Bottom Tool Shelves sit in the bottom `35%` ergonomic reach zone, leaving top bars strictly for navigation back, document title, quick status, and `Save/Done`.
2. **Adaptive Responsive Rules (`WindowSizeClass`):**
   - **Compact (`< 600dp`):** Bottom Navigation Bar + single-column doc list (`16dp` horizontal margin) + 3-column Tools grid.
   - **Medium (`600dp – 839dp`, Foldables/Small Tablets):** Bottom Bar OR Navigation Rail (`80dp`), 2-column doc grid (`maxWidth 720dp`), 4-column Tools grid.
   - **Expanded (`>= 840dp`, Tablets/Landscape):** Persistent `NavigationRail`, split master-detail pane (Document List + Reader/Preview or Storage Tree + Inspector), `6`-column Tools grid, centered modal sheets (`maxWidth 560dp`).
3. **Motion Budget (`150ms – 300ms` + Reduced Motion):** Standard Compose `tween(220, FastOutSlowInEasing)` for sheets/tabs; `spring(DampingRatioNoBouncy, StiffnessMediumLow)` for FAB & card expansion; respect `Settings.Global.ANIMATOR_DURATION_SCALE == 0f` by falling back to `snap()` transitions.
4. **Haptic Touchpoints:** `HapticFeedbackType.LongPress` on document multi-select activation, drag-to-reorder PDF merge items, and camera scan edge lock; subtle confirmation tick on signature save & server toggle.
5. **Ad Container Guardrail (`CD-07`):** Per CEO decision `CD-07`, AdMob Mediation UI implementation is **deferred** until all core features (`F-01`–`F-09`) reach `Done`. Phase 4 reserves clean non-intrusive list spacing rules (`>= 5` rows before any future inline native slot, `0` banners inside active editors/scanners/dialogs) without rendering live ad banners in core workflows.
