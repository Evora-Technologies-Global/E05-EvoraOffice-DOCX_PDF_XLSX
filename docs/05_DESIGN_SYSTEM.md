# Design System & UI/UX Blueprint — E05 (`Evora Office`)

**Project Code:** E05  
**Product Brand & Store Title:** `Evora Office` (`Evora Office: DOCX, PDF, XLSX` — `29/30` chars, `CD-05`)  
**Application ID:** `com.evora.technologies.office` (`CD-05`)  
**Visual Style:** `Style B — Familiar Vibrant Utility` (`CD-06`)  
**Owners:** UX Architect (`ux-architect`), UI Designer (`ui-designer`), UX Researcher (`ux-researcher`), Brand Guardian (`brand-guardian`), Rapid Prototyper (`rapid-prototyper`)  
**Date:** 2026-11-25  
**Status:** **APPROVED & LOCKED BY CEO — Gate 2 Passed (`2026-11-25`) -> Unlocks Phase 5 Sprint Planning** (`docs/05_DESIGN_SYSTEM.md` + [`wireframes/index.html`](../wireframes/index.html))  
**Upstream Inputs:** [`01_COMPETITOR_REVERSE_ENGINEERING.md`](01_COMPETITOR_REVERSE_ENGINEERING.md) §3 · [`02_PROJECT_CHARTER.md`](02_PROJECT_CHARTER.md) · [`03_PRD.md`](03_PRD.md) · [`04_USER_STORIES.md`](04_USER_STORIES.md) · [`ux_ergonomic_and_accessibility_audit.md`](../research/phase4/ux_ergonomic_and_accessibility_audit.md)

---

## 1. Phase 4 Decision Log & Constraints (`CD-05`–`CD-10`)

| Date | Decision ID | Design Rule Locked | Impact on UI & Phase 5/6 |
|---|---|---|---|
| 2026-11-25 | `CD-05` | **Brand & Namespace:** App name `Evora Office`; Store title `Evora Office: DOCX, PDF, XLSX`; `com.evora.technologies.office` | All headers, splash screens, and Wi-Fi web UI display `Evora Office`. |
| 2026-11-25 | `CD-06` | **Style B + Deferred Logo:** Familiar Vibrant Utility (`#2489FF` cobalt accent, format-coded badges/bars, pastel tiles) upgraded to Material 3 Light/Dark + WCAG AA + `>= 48dp` targets; final App Icon deferred | Phase 4 uses temporary `EO` geometric monogram tile placeholder (`#2489FF`, `12dp` radius). |
| 2026-11-25 | `CD-07` | **Feature-First Ads Deferral + Paywall Boundaries:** No AdMob/Mediation UI wired until `F-01`–`F-09` are `Done`; Phase 4 wireframes include skippable (`X`) Onboarding Paywall (`$4.99/wk`, `$39.99/yr`, `$59.99 life`), `Crown` badges, and honest pre-action `RewardGateSheet` | Reserve layout hooks for post-feature ads (`index >= 5` in lists only); zero save-lock dialogs (`SAVE_HEADER` banned). |
| 2026-11-25 | `CD-08` | **Gate 2 Interactive HTML Wireframes:** Pencil MCP unavailable in current runtime; interactive HTML/CSS/SVG prototype delivered at [`../wireframes/index.html`](../wireframes/index.html) | CEO tests all 21 routes, Light/Dark themes, Tablet/Phone breakpoints, and states interactively in browser. |
| 2026-11-25 | `CD-09` | **Personal Play Account Logistics Out-of-Scope:** Account-level 14-day / 12-tester closed test administrative gate excluded from engineering design scope per Charter | UI/Engineering focuses strictly on product quality, Play technical compliance (`targetSdk 36`, `16 KB`), and UMP/Billing specs. |
| 2026-11-25 | `CD-10` (`Gate 2`) | **CEO Gate 2 Design Sign-Off ("Chốt Phase 4 / Duyệt Gate 2"):** Approved `docs/05_DESIGN_SYSTEM.md`, `75/75` WCAG AA tokens, and interactive wireframes (`21` routes + `2` Glance widgets) | **Phase 4 Locked.** Immediately unlocks Phase 5 Sprint Backlog (`docs/06_SPRINT_BACKLOG.md`). |

---

## 2. Brand Foundations & Ethical UX Guardrails

1. **Visual Identity (`Style B — Familiar Vibrant Utility`):**
   - Users switching from legacy document viewers expect instant format color coding (Blue = Word, Red = PDF, Green = Excel, Orange = PPT, Cyan = HWP, Purple = ODF) and fast thumb-zone access to files and camera scan.
   - E05 keeps `dynamicColor = false` in Jetpack Compose `MaterialTheme` so OEM wallpaper theming never shifts format-coded icons or reduces brand clarity.
   - **Temporary Logo Placeholder (`CD-06`):** `32dp × 32dp` (`Home` header) and `72dp × 72dp` (`Splash` / `Paywall`) rounded square (`10dp` / `20dp` radius) in `#2489FF` -> `#186FD4` gradient displaying clean white `"EO"` monogram. Replaceable cleanly via single `EvoraBrandMark` composable when final icon milestone executes.
2. **Microcopy & Honest UX Standard:**
   - **Zero Dead Stubs:** Every visible tile, tab, and checkbox maps to an implemented offline capability (`Split PDF`, `Watermark`, `Tools Print`, `Dual ID Card`, `Continuous Scroll`, `Track Changes`, `ZIP/7z`). Unsupported `.rar` triggers a clear informative dialog (`"Evora Office supports ZIP and 7z offline archives."`) instead of silent failure.
   - **Upfront Gating Only (`CD-07`):** Pro tools (`Merge > 3`, `High Compress`, `Watermark`, `Batch OCR`, `Dual ID Card`, `PKCS#7`, `PDF->DOCX` beyond 1 daily free run) show `RewardGateSheet` **before** processing starts, offering `Watch 1 Short Video (24h Pass)` or `Upgrade to Pro`. Standard `Save` / `Save As` never triggers a paywall.

---

## 3. Visual Foundations & Design Tokens

All foreground/background pairs were verified programmatically via [`verify_wcag_contrast.py`](../research/phase4/verify_wcag_contrast.py) (`75/75` checks passing `>= 4.5:1` for text and `>= 3.0:1` for UI controls/graphics).

### 3.1 Adaptive Material 3 Color System (Light & Dark)

> **WCAG Note:** Competitor `#2489FF` yields `3.44:1` against `#FFFFFF` (passing Large Text/Icons `>= 3.0:1`, failing normal text `4.5:1`). E05 splits tokens cleanly: `BrandAccent` (`#2489FF` Light / `#4DA1FF` Dark) drives FABs, icons, selection rings, and progress strokes (`>= 3.13:1`), while `Primary` (`#186FD4` Light / `#4DA1FF` Dark) drives filled CTA button backgrounds and clickable text links (`4.93:1` Light, `6.28:1` Dark). Competitor's `#BBBBBB` (`1.92:1`) is replaced by `#526075` (`6.39:1`).

| Semantic Token (`Compose ColorScheme` / `EvoraColors`) | Light Hex | Dark Hex | Usage & Verified Contrast |
|---|---|---|---|
| `background` (`bgCanvas`) | `#F2F4F8` | `#101318` | App canvas behind cards (`16.21:1` L / `16.99:1` D w/ `onBackground`) |
| `surface` (`surfaceCard`) | `#FFFFFF` | `#1A1E26` | Document rows, tool groups, sheets (`17.85:1` L / `15.24:1` D w/ `onSurface`) |
| `surfaceContainerLow` (`surfaceSubtle`) | `#E9EEF5` | `#232934` | Search wells, stepper pills, unselected chip fills, shimmer base |
| `surfaceContainerHigh` (`surfaceElevated`) | `#FFFFFF` | `#252B36` | Bottom navigation bar, editor bottom drawer, modal dialogs |
| `onSurface` (`textPrimary`) | `#0F172A` | `#F1F5F9` | Headlines, file titles, primary body text |
| `onSurfaceVariant` (`textSecondary`) | `#475569` | `#B0BAC9` | Subtitles, metadata lines, inactive labels (`7.58:1` L / `8.52:1` D) |
| `textMuted` | `#526075` | `#94A1B5` | Helper captions, timestamps, file counts (`6.39:1` L / `6.38:1` D on Card) |
| `outlineVariant` (`dividerSubtle`) | `#CBD5E1` | `#333C4D` | `1dp` card dividers, table gridlines |
| `outline` (`borderInput`) | `#7C8BA1` | `#68768C` | Input borders, unfocused checkbox/radio outlines (`>= 3.46:1` UI AA) |
| `primary` (`primaryAction`) | `#186FD4` | `#4DA1FF` | Filled CTA buttons, text action links (`4.93:1` L / `6.25:1` D) |
| `onPrimary` | `#FFFFFF` | `#061D3D` | Text & icons inside filled Primary buttons (`4.93:1` L / `6.28:1` D) |
| `brandAccent` (`BrandBlue`) | `#2489FF` | `#4DA1FF` | Center `SCAN` FAB, active nav indicator icon, progress bar (`3.44:1` UI) |
| `primaryContainer` / `onPrimaryContainer` | `#E6F1FF` / `#0D4A9E` | `#183359` / `#BFDBFE` | Selected format chip, active tab pill (`7.38:1` L / `8.92:1` D) |
| `success` / `onSuccess` | `#1E7D5E` / `#FFFFFF` | `#46D8A9` / `#04261B` | Save toast, positive compression badge (`5.06:1` L / `8.98:1` D) |
| `successContainer` / `onSuccessContainer` | `#E3F8F0` / `#125741` | `#12382B` / `#86EFCD` | Compression size-saved pill, verified PKCS#7 badge (`7.69:1` L / `9.37:1` D) |
| `warning` / `onWarning` | `#B45309` / `#FFFFFF` | `#FBBF24` / `#2D1902` | Unsaved draft badge, HWP >50 MB offline notice (`5.02:1` L / `10.05:1` D) |
| `warningContainer` / `onWarningContainer` | `#FEF3C7` / `#78350F` | `#3B2809` / `#FDE68A` | Draft recovery banner, password warning (`8.15:1` L / `11.29:1` D) |
| `error` / `onError` | `#C82828` / `#FFFFFF` | `#FF7878` / `#3B0A0A` | Validation errors, permanent delete CTA (`5.54:1` L / `6.65:1` D) |
| `errorContainer` / `onErrorContainer` | `#FEECEC` / `#8C1D1D` | `#3F171A` / `#FECACA` | Wrong password banner, storage full alert (`7.98:1` L / `10.76:1` D) |
| `proGoldContainer` / `onProGoldContainer` | `#FFF4DB` / `#7A4B00` | `#3A2B0D` / `#FCD34D` | `PRO` crown pill, Best Value paywall badge (`6.78:1` L / `9.51:1` D) |

### 3.2 Format-Coded Palette (`EvoraFormatColors`)

Each document format exposes two calibrated color pairs: **Editor Top Bar** (`headerBg` with `#FFFFFF` L / `#F1F5F9` D text, `>= 4.80:1`) and **Badge / Pastel Tile** (`tintBg` + `inkText`, `>= 6.39:1`).

| Format Family | Extensions | Light TopBar (`#FFF` Text) | Light Badge (`Tint` / `Ink`) | Dark TopBar (`#F1F5F9` Text + `2dp` Accent) | Dark Badge (`Tint` / `Ink`) |
|---|---|---|---|---|---|
| **Word** | `doc`, `docx`, `dotx`, `docm` | `#1563DB` (`5.47:1`) | `#EAF2FF` / `#114FB3` | `#162642` + `#4D8DF0` (`13.79:1`) | `#1B2E4F` / `#89B7FF` |
| **PDF** | `pdf`, `wps`, `wpt`, `wpss` | `#D33434` (`4.87:1`) | `#FFEBEB` / `#A81E1E` | `#38181A` + `#FF7878` (`14.59:1`) | `#401C1F` / `#FF9494` |
| **Excel** | `xls`, `xlsx`, `xltx`, `xlsm` | `#1D820E` (`4.94:1`) | `#E6F7E4` / `#146308` | `#142B16` + `#48C736` (`13.82:1`) | `#17331A` / `#75E067` |
| **PowerPoint** | `ppt`, `pptx`, `pps`, `ppsx`, `pot` | `#B85600` (`4.81:1`) | `#FFF2E4` / `#8F4100` | `#36200D` + `#FF9838` (`14.00:1`) | `#3D2510` / `#FFB067` |
| **Hangul (HWP)** | `hwp`, `hwpx`, `hwt`, `hwdt` | `#0074BF` (`4.94:1`) | `#E5F5FF` / `#005894` | `#0F273B` + `#33A5EE` (`13.95:1`) | `#133047` / `#66C3FF` |
| **OpenDocument** | `odt`, `ods`, `odp`, `odf` | `#8B1ED9` (`6.39:1`) | `#F5EAFF` / `#6C14AD` | `#2B183E` + `#BD58FA` (`14.71:1`) | `#341D4A` / `#D599FF` |
| **Ebook (EPUB)** | `epub` | `#5A7D0E` (`4.80:1`) | `#F0F8DF` / `#446107` | `#222E10` + `#9FD134` (`13.07:1`) | `#293814` / `#B5E34F` |
| **Text & Archive** | `txt`, `csv`, `log`, `md`, `zip`, `7z` | `#1474B0` (`5.05:1`) | `#E6F5FF` / `#0D5887` | `#13293B` + `#52BCFB` (`13.61:1`) | `#173247` / `#72C8FF` |

### 3.3 Reader Comfort Themes (`DocumentReaderRoute`)

| Theme Mode | Page Canvas | Body Ink | Muted Chrome Ink | Verified Contrast |
|---|---|---|---|---|
| **Original Light** | `#FFFFFF` | `#0F172A` | `#526075` | `17.85:1` / `6.39:1` |
| **Night Inverted Dark** | `#0F1216` | `#E2E8F0` | `#94A1B5` | `15.23:1` / `7.32:1` |
| **Sepia Comfort** | `#F5ECD8` | `#3B2F20` | `#6E5A42` | `11.08:1` / `5.58:1` |

### 3.4 Typography Scale (`Poppins` + `JetBrains Mono` Tabular)

Bundle `Poppins` (`Regular 400`, `Medium 500`, `SemiBold 600`, `Bold 700`) in `res/font/`. Use `fontFeatureSettings = "tnum"` (or `JetBrains Mono` `400/500`) for file sizes, page counters (`"12 / 45"`), Excel coordinates, and Wi-Fi LAN URLs (`http://192.168.1.45:8998`). Minimum UI font size is strictly **`11sp`** (no `10sp` sub-accessible labels).

| Material 3 Role | Family / Weight | Size / Line Height | Letter Spacing | Primary Usage |
|---|---|---|---|---|
| `displaySmall` | `Poppins Bold (700)` | `32sp / 38sp` | `-0.5sp` | Paywall hero price, Splash title, Result metric (`"Saved 73%"`) |
| `headlineMedium` | `Poppins Bold (700)` | `22sp / 28sp` | `-0.2sp` | Onboarding titles, Permission rationale header |
| `titleLarge` | `Poppins SemiBold (600)` | `18sp / 24sp` | `0sp` | Top App Bar brand title (`Evora Office`), Sheet titles |
| `titleMedium` | `Poppins SemiBold (600)` | `16sp / 22sp` | `0.1sp` | Section headers (`Convert`, `PDF Power`), Dialog titles |
| `titleSmall` | `Poppins SemiBold (600)` | `14sp / 20sp` | `0.1sp` | Document card filename, Editor tab label, Button CTA |
| `bodyLarge` | `Poppins Regular (400)` | `16sp / 24sp` | `0.15sp` | Reflow reader text, Onboarding body, OCR extracted text |
| `bodyMedium` | `Poppins Regular (400)` | `14sp / 20sp` | `0.15sp` | Search query text, Settings rows, Archive entries |
| `labelLarge` | `Poppins Medium (500)` | `14sp / 20sp` | `0.1sp` | Filter chips, CTA buttons, Segmented tabs |
| `labelMedium` | `Poppins Medium (500)` | `12sp / 16sp` | `0.2sp` | File metadata (`1.8 MB · Today 14:22`), Tool grid tile labels |
| `labelSmall` | `Poppins SemiBold (600)` | `11sp / 16sp` | `0.3sp` | Bottom bar labels, Format badge pills (`PDF`), `PRO` chip |

### 3.5 Spacing, Shapes, Elevation & Responsive Grid (`4dp/8dp`)

- **Spacing Tokens:** `xxs = 2dp`, `xs = 4dp`, `sm = 8dp`, `md = 12dp`, `lg = 16dp`, `xl = 24dp`, `xxl = 32dp`, `section = 48dp`.
- **Standard Gutters:** Compact phone `16dp`; Medium tablet/foldable `24dp`; Expanded tablet `32dp` (long-form reader/settings constrained to `maxWidth = 680dp`).
- **Corner Radii (`Shapes`):** `xs = 6dp` (format badge icon), `sm = 10dp` (quick chips, inputs), `md = 14dp` (document cards, tool tiles), `lg = 20dp` (paywall tier cards, dialogs), `xl = 28dp` (bottom sheets), `full = 999dp` (filter pills, Scan FAB, page thumb pill).
- **Elevation:** Flat-first utility aesthetic using surface contrast (`#FFFFFF` on `#F2F4F8` in Light; `#1A1E26` on `#101318` in Dark) + `1dp` `outlineVariant` hairline borders on Dark cards. Only Bottom Navigation (`4dp`), Center Scan FAB (`6dp`), Floating Page Pill (`4dp`), and Modal Sheets (`8dp`) carry tonal/shadow elevation.
- **WindowSizeClass Layout Adaptation:**
  - `Compact (< 600dp)`: `80dp` Bottom Nav Bar + elevated `56dp` center Scan FAB; `1`-col document list; `3`-col Tools grid.
  - `Medium (600–839dp)`: `80dp` `NavigationRail` (or adaptive centered Bottom Bar max `560dp`); `2`-col document grid; `4`-col Tools grid.
  - `Expanded (>= 840dp)`: Persistent `80dp` `NavigationRail`; Two-Pane Master-Detail (Document list `360dp` left + Preview/Reader right; Storage folder tree left + Files right); `6`-col Tools grid.

---

## 4. Component Library Specifications

### 4.1 Atomic Controls & Touch Ergonomics

| Component | Height / Dimensions | Visual States | Compose Contract Notes |
|---|---|---|---|
| **Primary CTA Button** | Min height `48dp`, `14dp` radius, `20dp` H-pad | `Default` (`#186FD4` L / `#4DA1FF` D), `Pressed` (ripple + 92% tint), `Disabled` (`surfaceSubtle` + `textMuted` 50%), `Loading` (`18dp` inline spinner) | Never place text smaller than `14sp`; single line w/ ellipsis |
| **Secondary / Outlined Button** | Min height `48dp`, `1dp` `borderInput` | `Default` (transparent on `surface`), `Pressed` (`primaryContainer` 40%) | Used for secondary actions (`Use Folder Picker SAF`, `Restore Purchases`) |
| **Format Filter Chip** | Visual pill `36dp` inside **`48dp` min touch wrapper**, `full` radius | `Unselected` (`surfaceCard` + `outlineVariant`), `Selected` (`primaryContainer` + `onPrimaryContainer` + `1.5dp` `brandAccent` border) | Horizontal `LazyRow` (`8dp` gap) |
| **Document List Card (`DocCard`)** | Min height `76dp`, `14dp` radius, `12dp` inner padding | `Default`, `Pressed` (ripple), `Multi-Select Active` (`2dp` `brandAccent` border + checkbox), `Draft Dirty` (amber `"Draft auto-saved"` dot) | Leading `44dp` format icon badge (`tintBg` + crisp SVG), 2 text lines + optional `"Page 3/12"` pill, trailing `48dp × 48dp` Favorite star + `48dp × 48dp` `MoreVert` button |
| **Tool Grid Pastel Tile** | `>= 88dp` height, `14dp` radius, centered column | `44dp × 44dp` soft pastel circle/squircle icon (`tintBg`), `12sp` Medium 2-line label, optional top-right `PRO` crown chip | `3`-col (`10dp` gap) on phones; `4`/`6`-col on tablets |
| **Main Bottom Bar + Scan FAB** | Bar `80dp` + system nav bar inset; Center FAB `56dp × 56dp` (`18dp` radius or circle) | `Home`, `Recent`, center `SCAN` (`brandAccent` fill, `#FFFFFF` L / `#061D3D` D camera glyph), `Storage`, `Tools` | Each tab occupies `>= 48dp × 56dp` hit area; labels `11sp` SemiBold (`textPrimary` when active -> `#186FD4` L / `#4DA1FF` D) |
| **Collapsible Editor Bottom Shelf** | Quick bar (`48dp`) + Tabbed panel (`200dp–240dp` expanded, collapses to `48dp` header strip) | Horizontal scrollable pill tabs (`Edit`, `File`, `Insert`, `Review`, `Page`, `Draw`, `Find`) + icon actions (`48dp × 48dp` targets) + popover color swatch grid | Header dropdown/tabs collapse downward with `220ms` tween so editing viewport maximizes cleanly |
| **Draggable Page Pill** | `36dp` height pill (`"12 / 45"`) anchored to `48dp`-wide right edge touch track | `Idle` (semi-transparent dark scrim `75%` + `#FFF`), `Dragging` (expanded thumb + live page jump) | Eliminates accidental swipe-back conflicts (`32dp` vertical rail inset) |

### 4.2 Mandatory Screen State Matrix

Every primary screen implements exhaustive MVI rendering states (`Loading`, `Empty`, `Content`, `Error`) plus specialized utility modals:

1. **Loading / Shimmer Skeleton:** `DocCard` placeholder skeleton rows (`surfaceSubtle` pulsing at `1200ms` linear, static muted surface when Reduced Motion enabled) — never blank white screens.
2. **Empty State:** Centered crisp vector illustration (no emojis), `16sp` title, `14sp` explanation, and direct recovery CTA (e.g., `"No PDFs found"` -> `"Scan Document"` or `"Browse Device Folders"`).
3. **Error / Permission Fallback State:** Non-blocking inline card (`errorContainer` / `warningContainer`) with actionable `"Retry"` or `"Open Folder Picker (SAF)"` CTA (`US-003`).
4. **Password Unlock Dialog:** Modal with masked input (`show/hide` eye icon `48dp` target), `"Wrong password — please try again"` inline error (`error` token), and clean `"Cancel"` exit (`US-022`).
5. **Draft Recovery Banner / Dialog:** Shown automatically when `DraftRecoveryEntity.hasPendingRecovery == true` (`US-032`): `"Unsaved changes recovered from 14:22"` with `[Discard]` and `[Restore Draft]`.
6. **Deferred Ad Container Rule (`CD-07`):** No ad banners or native placeholders are rendered during Phase 6 feature implementation (`F-01`–`F-09`). Wireframes annotate where future post-feature collapsible banner / `index >= 5` native card hooks attach safely without shifting layout or colliding with editor/scanner surfaces.

---

## 5. Screen Blueprints, User Story Mapping & Navigation Transitions

### 5.1 Complete Route-to-Wireframe Matrix (`21` PRD Routes + `2` Glance Widgets)

| Screen ID | PRD Route (`03_PRD.md`) | Linked User Stories | Transition Pattern | Core Layout & Interactive Spec |
|---|---|---|---|---|
| `S-01` | `SplashRoute` | `US-001`, `US-002`, `US-004` | Root Entry -> Cross-fade (`200ms`) | Temporary `EO` monogram (`72dp`), `"Evora Office"` wordmark, subtle progress indicator; bypasses directly to `S-12/S-13/S-15` on `ACTION_VIEW`. |
| `S-02` | `LanguageSelectRoute` | `US-001` | Cross-fade -> Horizontal Push | `10` locale radio cards (`EN`, `VI`, `ES`, `PT-BR`, `KO`, `JA`, `ID`, `HI`, `DE`, `FR`) with native names + sticky `"Continue"` `48dp` CTA. |
| `S-03` | `OnboardingIntroRoute` | `US-003` | Horizontal Slide Push | `3`-slide `HorizontalPager` (Offline Office Editing, Real PDF Power Tools, Smart Scan & Wi-Fi Transfer), top-right `Skip` (`48dp`), dot indicator, `"Next"` / `"Get Started"`. |
| `S-04` | `PaywallRoute` & `RewardGateSheet` | `US-110`, `US-111` | Modal Slide Up (`250ms`) | **Paywall:** Top-left `X` close (`48dp`, zero hidden delay), 4 benefit bullets, 3 SKU cards (`Weekly $4.99 / 3d`, `Yearly $39.99 / 7d` Best Value highlighted, `Lifetime $59.99`), `"Start 7-Day Free Trial"` CTA, `"Restore Purchases"`. **RewardGateSheet:** Upfront 2-option modal before Pro tool runs (`Watch 1 Video for 24h Pass` vs `Go Pro`). |
| `S-05` | `StoragePermissionRoute` | `US-003`, `US-005` | Horizontal Slide Push | Shield/folder vector graphic, plain-language `MANAGE_EXTERNAL_STORAGE` explanation, `"Allow All-Files Access"` primary CTA, `"Use Folder Picker (SAF)"` secondary outlined CTA. |
| `S-06` | `MainShell` + `HomeDocumentsRoute` | `US-010`, `US-013`, `US-005`, `US-081` | Tab Cross-fade (`150ms`) | Header (`EO` mark + `Evora Office`, Search `48dp`, Pro Crown `48dp`, `+ New` template trigger), format filter chip strip (`All`, `PDF`, `Word`, `Excel`, `PPT`, `ODF`, `HWP`, `TXT`, `ZIP/7z`), sort/count bar, optional dismissible `"Set as Default"` card, `DocCard` list w/ Favorite + 3-dot `FileContextMenu` Bottom Sheet. |
| `S-07` | `RecentBookmarksRoute` | `US-011` | Tab Cross-fade (`150ms`) | 3-way segmented control (`Recent` · `Favorites` · `Trash`). `Recent` shows `"Page 3/12"` progress bar; `Trash` shows `"Auto-deletes after 30 days"` banner + `Restore` / `Delete Forever` actions + `"Empty Trash"` CTA. |
| `S-08` | `SearchDocumentsRoute` | `US-012` | Shared-Element / Vertical Fade | Auto-focused search input (`48dp` height) w/ clear button, format filter pills, recent query chips, highlighted substring matches in results, empty-search state. |
| `S-09` | `TemplatesSheet` (Modal) | `US-081` | Modal Bottom Sheet (`250ms`) | Category pills (`All`, `Word`, `Excel`, `PPT`) + 2-column thumbnail cards (`Blank Doc`, `Resume CV`, `Meeting Notes`, `Blank Sheet`, `Monthly Budget`, `Invoice`, `Blank Slide`, `Pitch Deck`). |
| `S-10` | `StorageBrowserRoute` (+ `ZIP/7z` Viewer) | `US-024`, `US-080` | Tab Cross-fade / Folder Push | Cloud quick cards (`Google Drive`, `OneDrive`), device volume tabs (`Internal`, `Downloads`, `SD Card`), breadcrumb path, `+ Folder` action, multi-select sticky action bar (`Move`, `Copy`, `Delete`, `Share`, `Merge`). Tapping `.zip`/`.7z` opens Archive Inspector sheet listing entries + Extract CTA (unsupported `.rar` shows clean info dialog). |
| `S-11` | `CloudDriveBrowserRoute` | `US-100` | Horizontal Slide Push | Provider header (`Google Drive` / `OneDrive`), account switcher pill, remote folder tree + search, download/upload transfer progress bar, disconnect option. |
| `S-12` | `ToolsHubRoute` | `US-060`–`US-063`, `US-072`, `US-090` | Tab Cross-fade (`150ms`) | Header w/ `Settings` gear (`48dp`). 3 pastel-tile sections (`3`-col phone): **Convert** (`Image->PDF`, `Text->PDF`, `Docs->PDF`, `PDF->DOCX [1/day|PRO]`, `PDF->Image`), **PDF Power** (`Merge PDF`, `Split PDF`, `Compress PDF`, `Watermark PDF`, `Set/Unlock Password`, `Sign PDF`, `Print`), **Utilities** (`OCR Extract`, `QR/Barcode`, `Wi-Fi Transfer`, `Templates`). Zero fake stubs. |
| `S-13` | `DocumentReaderRoute` | `US-020`–`US-023` | Horizontal Push / Immersive Toggle | Format-colored Top Bar (`Back`, Title, `TOC/Outline`, `Search`, `Fullscreen`, `Edit`/`Pro`), clean page viewport (`Pdfium` / `HWP HTML` / `EPUB` / `TXT`), right-edge draggable `PageIndicatorPill`, optional `TTS Read-Aloud` / `Auto-Scroll` floating pill, and `ReadingSettingsSheet` (`Continuous vs Single`, `Portrait/Landscape`, `Light / Night / Sepia`, `Reflow`, `Auto-Scroll 1..3`, `TTS`). |
| `S-14` | `OfficeEditorRoute` (`Word` / `Excel` / `PPT`) | `US-030`–`US-032`, `US-040`, `US-041` | Horizontal Push | **Word:** Top bar (`Back`, Title, `Undo`, `Redo`, **`Done`** primary text CTA — zero paywall), `Draft Auto-saved` indicator, 2-row quick format bar + `7` collapsible bottom tabs (`Edit`, `File`, `Insert`, **`Review` [Track Changes + Comments]**, `Page`, `Draw`, `Find`). **Excel:** Formula bar (`fx`), sheet tabs (`+`), frozen grid + `5` tabs (`File`, `Edit`, `Insert`, `Formulas`, `Find`). **PPT:** Slide strip + canvas + Notes toggle + Play `SlideShow` button + `7` tabs (`Edit`, `File`, `Insert`, `Format` [Shape/Fill/Z-Order], `Annotate`, `Page`, `Find`). |
| `S-15` | `SlideShowRoute` | `US-041` | Fullscreen Cross-fade | Fullscreen landscape/immersive slide canvas, translucent bottom-hover HUD (`Prev`, `3 / 14`, `02:18`, `Laser Ink Toggle`, `Exit`). |
| `S-16` | `PdfEditorRoute` & `SignaturePadRoute` | `US-050`, `US-051` | Horizontal Push / Modal Canvas | **PDF Editor:** `Pdfium` canvas w/ floating draggable signature box (`X` delete / resize handle) & sticky note pins, `4` bottom tabs (`Annotate` [Highlight 4 swatches, Underline, Strike, Ink, Note], `Signature` [Create/Saved/PKCS#7 Cert], `File`, `Page`). **SignaturePad:** Landscape/portrait drawing canvas, `3` ink colors, stroke slider, `Clear` / `Save` + **PKCS#7 Cert Inspector** modal (`Issuer`, `Valid Dates`, `Trusted Status`). |
| `S-17` | `PdfToolConfigRoute` & `ConvertProgressRoute` | `US-052`, `US-060`–`US-063` | Horizontal Push -> State Cross-fade | **Config:** Dynamic body by tool (`Merge` drag-reorder list; `Split` range `"1-3, 5"` / every N / visual thumbnail select; `Compress` Low/Med/High DPI radio cards; `Watermark` text + opacity slider + `-45°` angle + tiled preview; `AES Password` lock/unlock; `PDF->DOCX`). **Progress:** Circular `%` ring + Cancel. **Success:** Green delta pill (`14.2 MB -> 3.8 MB`, `-73%`), `Open`, `Share`, `Locate`. |
| `S-18` | `ScannerFlowRoute` | `US-070`–`US-072` | Vertical Slide Up (`250ms`) | Live camera viewfinder w/ top flash/grid/auto-capture controls + mode strip (`Document`, **`ID Card`**, `OCR`, `QR/Barcode`). **ID Card Mode:** Explicit 2-step banner (`"Step 1/2: Front Side"` -> `"Step 2/2: Back Side"`) -> centered A4 preview canvas. **Crop/Filter:** Corner handles + `Original / Magic Color / Grayscale / B&W`. **OCR:** `5`-script picker (`Latin`, `Chinese`, `Devanagari`, `Japanese`, `Korean`) -> editable text -> `Copy` / `.txt` / `.docx`. **QR:** Structured action card (`Open URL`, `Join Wi-Fi`, `Save Contact`). |
| `S-19` | `WifiTransferRoute` | `US-090` | Horizontal Slide Push | Status badge (`Running` / `Stopped`), large toggle button, LAN HTTP URL (`http://192.168.1.45:8998`) + QR code + Copy CTA, FTP toggle (`port 8998`), `Read-Only` & `PIN Lock` switches, `10:00` idle countdown indicator, browser drag-drop tip. |
| `S-20` | `SettingsRoute` | `US-001`, `US-002`, `US-111`, `US-112` | Horizontal Slide Push | Grouped clean cards: **Appearance** (`Theme: System / Light / Dark`, `Language`), **Files & Defaults** (`Default App Handler`, `Auto-Save Interval`), **Account & Pro** (`Manage Pro`, `Restore Purchases`, `Connected Clouds`), **Privacy & Legal** (`UMP Privacy Choices`, `Open-Source Licenses [MPLv2/LGPLv3/Apache]`, `v1.0.0 (100)`). |
| `W-01` / `W-02` | **Android Home Glance Widgets** | `US-091` | Direct Launcher Deep-Link | **W-01 Quick Bar (`4×1`):** `EO` icon + `Search docs...` pill + `Scan` button + `New` button (`48dp` targets). **W-02 Recent List (`4×2`):** Header w/ `Scan` + top `3` recent files w/ format badge, filename, page progress. Adapts to Android wallpaper / Light & Dark system theme. |

---

## 6. Accessibility, Motion & Haptics Checklist (Compose Implementation Gate)

1. **Interactive Target Floor:** Wrap every icon-only button (`24dp` glyph inside `48dp × 48dp` box) with `Modifier.minimumInteractiveComponentSize()` and maintain `>= 8dp` gap between adjacent editor/toolbar targets.
2. **TalkBack & Semantics:**
   - Every decorative vector uses `contentDescription = null`.
   - Every icon button, format badge, and `DocCard` supplies localized `contentDescription` (e.g., `"PDF file, Contract_Signed_v2.pdf, 1.8 megabytes, page 3 of 12"`).
   - Group multi-line `DocCard` title/subtitle in `Modifier.semantics(mergeDescendants = true)` while keeping trailing Favorite & Overflow buttons as separate focus stops.
3. **Dynamic Type (`fontScale` up to `2.0x`):** All button heights use `heightIn(min = 48.dp)` (never fixed `height(48.dp)`) and `DocCard` uses `heightIn(min = 76.dp)` so scaled text wraps cleanly without clipping.
4. **Reduced Motion & Haptics:** Check `LocalReducedMotion` / system `ANIMATOR_DURATION_SCALE` to replace transitions/shimmers with instant cross-fades. Attach `HapticFeedbackType.LongPress` to multi-select, merge drag-reorder, and scan lock; subtle click on Save/Server start.

---

## 7. Interactive Prototype Verification (`CEO Gate 2`)

Open [`wireframes/index.html`](../wireframes/index.html) directly in any desktop/mobile browser (`file://`, zero network dependencies) to inspect:
- **All `21` Application Screens + `2` Android Glance Widgets (`W-01`, `W-02`)** organized into logical flows (`Onboarding`, `Main Hub`, `Readers & Editors`, `PDF Tools & Scanner`, `Transfer & Settings`).
- **Live Controls Bar:** Toggle **Light / Dark Theme**, **Compact Phone (`390×844`) / Expanded Tablet (`880×620`)**, **State Simulator (`Content` / `Empty` / `Loading` / `Error`)**, and interactive **Reader Themes (`Light` / `Sepia` / `Night`)**, **Word/Excel/PPT/PDF Editor Tabs**, **ID Card 2-Step Flow**, **Paywall SKU Selector**, and **Pre-Action Reward Gate Sheet**.
