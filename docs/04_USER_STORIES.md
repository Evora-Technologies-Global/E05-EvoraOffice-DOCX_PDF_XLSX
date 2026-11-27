# User Stories & Acceptance Criteria — E05 Native Android Office & PDF Suite

**Project Code:** E05  
**Target Release:** `versionName = "1.0.0"`, `versionCode = 100` (Full Parity Scope)  
**Owner:** Business Analysis (`ba-writer`)  
**Date:** 2026-11-25  
**Status:** Complete (Phase 3 Baseline -> Maps 1:1 to `docs/03_PRD.md`)  
**Format Standard:** Domain-grouped tables (`US-001`..`US-119`) with Role-Action-Benefit + **Gherkin (`Given / When / Then`)** Acceptance Criteria per [`.claude/rules/product-lifecycle-workflow.md`](../.claude/rules/product-lifecycle-workflow.md).

---

## Priority Legend

| Priority | Meaning | Target Release |
|---|---|---|
| `P0` | Critical parity & core UX blocker; mandatory for release | `1.0.0` (`100`) |
| `P1` | Important enhancement / cloud / convenience utility | `1.0.0` (`100`) |
| `P2` | Secondary polish / nice-to-have convenience | `1.0.0` (`100`) if timeline green |

---

### 2.1 First Launch, Onboarding, Permissions & External Intents

| ID | User Story | Priority | Gherkin Acceptance Criteria |
|---|---|---|---|
| `US-001` | As a first-time user, I want to choose my preferred interface language on first launch so that every menu and tool label matches my language | `P0` | - **Given** fresh app install with no persisted locale preference<br>- **When** `SplashRoute` finishes cold-start routing<br>- **Then** `LanguageSelectRoute` displays supported locales (`EN`, `VI`, `ES`, `PT-BR`, `KO`, `JA`, `ID`, `HI`, `DE`, `FR`), applies selection immediately, persists flag, and skips on future launches |
| `US-002` | As a first-time user in the EEA/UK/CH, I want a compliant privacy consent prompt before any ad loads so that my data preferences are respected | `P0` | - **Given** first launch inside an EEA/UK/Switzerland region<br>- **When** consent form is required by Google UMP SDK<br>- **Then** UMP dialog presents cleanly prior to initializing AdMob SDK, and revocation remains accessible in `SettingsRoute` |
| `US-003` | As a user, I want a concise skippable onboarding walkthrough and permission prompt with a seamless fallback if I deny All-Files access so that I am never locked out | `P0` | - **Given** `StoragePermissionRoute` explains document indexing clearly<br>- **When** user grants `MANAGE_EXTERNAL_STORAGE` OR denies it and taps *"Use Folder Picker (SAF)"*<br>- **Then** granted users enter auto-indexed Home, while denied users enter Home cleanly with persistent SAF `"Open / Add File"` action and zero crash or infinite modal loop |
| `US-004` | As an Android user, I want to open any `.pdf`, `.docx`, `.xlsx`, `.pptx`, `.hwp`, `.odt`, or `.txt` attachment from WhatsApp, Gmail, or Files directly in E05 without waiting for startup ads | `P0` | - **Given** user taps a document in an external app and selects E05 via OS "Open With"<br>- **When** `ACTION_VIEW` URI (`content://` or `file://`) arrives at app entry<br>- **Then** URI MIME/header is inspected safely, stream cached if read-only, and target Reader opens directly in `< 1.5s` with **zero startup or interstitial ad** blocking view |
| `US-005` | As an Android user, I want a guided prompt to set E05 as my default document opener per file type so that future files launch automatically | `P1` | - **Given** user opens >= 2 external files cleanly<br>- **When** contextual `"Set as Default App"` card is tapped on Home<br>- **Then** system opens app-default settings or instructions accurately without spamming repeat modals |

---

### 2.2 Home Document Library, File Indexing, Recent / Favorites / Trash & Search

| ID | User Story | Priority | Gherkin Acceptance Criteria |
|---|---|---|---|
| `US-010` | As a busy user, I want all local documents automatically grouped by format chips (`All`, `PDF`, `Word`, `Excel`, `PPT`, `ODF`, `HWP`, `TXT/CSV`, `ZIP/7z`) so that I can find files in one tap | `P0` | - **Given** granted storage permission on Home tab<br>- **When** user selects any format filter chip and toggles sort order (`Modified Date`, `Name`, `Size`, `Format`)<br>- **Then** document list updates instantaneously (`< 100ms`), shows accurate format color icon, file size, date, and pulls-to-refresh smoothly |
| `US-011` | As a returning user, I want Recent files, Favorites, and Deleted Trash backed by full metadata so I can resume reading or recover deleted files | `P0` | - **Given** user marks a file Favorite or deletes a local copy to Trash<br>- **When** user opens `RecentBookmarksRoute` tabs (`Recent`, `Favorites`, `Trash`)<br>- **Then** `Favorites` preserves starred entries; `Recent` displays last opened timestamp + saved page badge (`"Page 4/18"`); `Trash` restores file to `originalPath` or permanently deletes it (auto-purging after `30` days) |
| `US-012` | As a user with hundreds of documents, I want instant keyword search by filename and folder so that I locate contracts within seconds | `P0` | - **Given** user opens `SearchDocumentsRoute`<br>- **When** user types a query (`>= 1` character) with optional format chip filter<br>- **Then** matching results render within `300ms` debounce with highlighted matched substring and empty-state prompt if zero results match |
| `US-013` | As a power user, I want contextual actions on every file card (`Rename`, `Share`, `Delete`, `Add Shortcut`, `Convert to PDF`, `Properties`) so I can manage documents from one list | `P0` | - **Given** user taps the 3-dot overflow menu on any document card<br>- **When** user picks `Rename` (validates illegal characters/duplicate name), `Share` (`FileProvider`), `Properties` (path/size/MIME/pages), or `Add Shortcut`<br>- **Then** action executes cleanly, updates MediaStore & Room DB synchronously, and never corrupts file extension |

---

### 2.3 Universal Document Reader (`PDF`, Office, `ODF`, `HWP/HWPX`, `EPUB`, `TXT/CSV`, `ZIP/7z`)

| ID | User Story | Priority | Gherkin Acceptance Criteria |
|---|---|---|---|
| `US-020` | As a reader, I want smooth pinch-to-zoom, draggable page-number scrub thumb, outline/TOC drawer, in-document text search, and automatic reading-position memory | `P0` | - **Given** user opens a multi-page `PDF`, `DOCX`, `PPTX`, or `EPUB` in `DocumentReaderRoute`<br>- **When** user jumps via right-edge drag thumb `"12/45"`, searches a keyword, or reopens the file later<br>- **Then** search jumps sequentially across matches with highlight overlay, and reopening the file restores exact saved `lastPageIndex`, `scrollOffsetY`, and `zoom` |
| `US-021` | As a mobile reader, I want working Continuous Vertical Scroll vs. Single-Page Snap, Light/Dark/Sepia reader themes, Reflow View, Auto-Scroll, and TTS Read-Aloud | `P0` | - **Given** user opens Reader View Settings sheet<br>- **When** user toggles Continuous Scroll, switches Theme (`Light` / `Night Dark` / `Sepia`), enables Reflow mode, turns on Auto-Scroll (`speed 1..3`), or starts TTS Read-Aloud<br>- **Then** viewport updates immediately with **zero "Coming Soon" stubs**, TTS reads visible page content via Android `TextToSpeech`, and reader theme persists in `DocumentMetaEntity` |
| `US-022` | As a user receiving encrypted PDFs or password-protected Office/HWP docs, I want a secure inline password dialog so I can unlock and read them offline | `P0` | - **Given** user opens an encrypted document<br>- **When** engine reports password required<br>- **Then** password dialog prompts user (`show/hide` toggle), unlocks document on valid password, shows inline validation error on wrong password without crashing, and cancels cleanly back to caller |
| `US-023` | As a Korean business user, I want `.hwp` and `.hwpx` Hangul documents rendered completely offline without uploading sensitive files to external servers | `P0` | - **Given** user selects a local `.hwp` or `.hwpx` file (`<= 50 MB`)<br>- **When** offline Hangul pipeline parses AST via `hwplib`/`hwpxlib` and renders sanitized HTML/CSS/SVG or LOK bridge<br>- **Then** document renders with readable headings, paragraphs, tables, and embedded images with **zero network traffic**, and supports `Search`, `Print`, and `Export PDF` |
| `US-024` | As a user receiving `.zip` or `.7z` attachments, I want to inspect contents inside the archive and extract files locally | `P0` | - **Given** user opens a `.zip` or `.7z` file via `StorageBrowserRoute` or Home<br>- **When** `Apache Commons Compress` lists entries (prompting if archive is encrypted)<br>- **Then** user previews entry names/sizes, extracts one or all files cleanly to a chosen folder, and can immediately open extracted documents; attempting `.rar` explains cleanly that `.zip`/`.7z` are supported |

---

### 2.4 Rich Word Editor & Track Changes / Comments

| ID | User Story | Priority | Gherkin Acceptance Criteria |
|---|---|---|---|
| `US-030` | As a user editing Word (`DOC/DOCX/ODT`) files, I want full formatting controls (`Edit`, `Insert`, `Page`, `Draw`, `Find`) via `LibreOfficeKit` so I achieve desktop-grade mobile editing | `P0` | - **Given** user taps `Edit` on a `.docx`/`.doc`/`.odt` document<br>- **When** `OfficeEditorRoute` switches to Word Editor (`6` bottom tabs + quick toolbar)<br>- **Then** user edits text with Undo/Redo, Font family/size, B/I/U/Strikethrough, Text & Highlight colors, 4-way Alignment, Bullets/Numbering, L/R Indent, Insert Image (Camera/Gallery) & Shapes, Freehand Ink overlay, and Page orientation/margins cleanly |
| `US-031` | As a team reviewer, I want a working Review tab (`Track Changes` toggle, accept/reject revision, add/view comments) inside the Word Editor | `P1` | - **Given** user opens a `.docx` file containing comments or revisions<br>- **When** user selects the `Review` bottom tab in Word Editor<br>- **Then** existing comments and tracked changes render accurately via `LibreOfficeKit`, and user can toggle `Track Changes`, add a comment, or accept/reject revisions |
| `US-032` | As an author, I want my edited document saved cleanly on tap AND protected against process death crashes without any Save ransom paywall | `P0` | - **Given** user edits any existing or newly created document<br>- **When** app backgrounds or crashes mid-edit (`DraftRecoveryEntity` saved every `15s` / `onStop`) OR user taps `"Save"` / `"Save As"`<br>- **Then** saving writes atomically back to target file / chosen location with **zero Rewarded Ad or Pro lock dialog intercepting `Save`**, and reopening after crash prompts *"Restore unsaved draft?"* |

---

### 2.5 Excel Spreadsheet Editor & PowerPoint Slide Presenter/Editor

| ID | User Story | Priority | Gherkin Acceptance Criteria |
|---|---|---|---|
| `US-040` | As an analyst editing spreadsheets (`XLS/XLSX/ODS`), I want multi-sheet tabs, formula evaluation, cell formatting, row/column insertion, and freeze panes | `P0` | - **Given** user edits a workbook in `OfficeEditorRoute` (Excel mode: `5` bottom tabs + Formula bar)<br>- **When** user adds/switches/renames sheets, inserts formulas (`SUM`, `AVERAGE`, `COUNT`, `MIN`, `MAX`, `IF`), applies Number formats (`Currency`, `Date`, `%`), merges cells, resizes row/col width, or toggles `Freeze Top Row / Column`<br>- **Then** `LibreOfficeKit` recalculates dependent cells live, preserves formulas on Save, and renders embedded charts cleanly |
| `US-041` | As a presenter editing decks (`PPT/PPTX/ODP`), I want full slide editing (`7` tabs) plus a fullscreen SlideShow presenter mode | `P0` | - **Given** user opens a presentation in `OfficeEditorRoute` (Slide mode)<br>- **When** user adds/duplicates/deletes slides, modifies textboxes/shapes (Fill color, Stroke color/width/dash, 4-way Z-order Arrange), edits presenter notes, or taps `Play SlideShow`<br>- **Then** `SlideShowRoute` opens fullscreen with smooth swipe navigation, elapsed timer, and temporary laser/ink pointer overlay |

---

### 2.6 PDF Annotations, Handwritten e-Sign, PKCS#7 Cert & Encryption

| ID | User Story | Priority | Gherkin Acceptance Criteria |
|---|---|---|---|
| `US-050` | As a reviewer marking up PDFs, I want persistent highlight, underline, strikethrough, freehand ink draw, and sticky note comments | `P0` | - **Given** user opens a PDF in `PdfEditorRoute`<br>- **When** user draws ink strokes, highlights selected text (`4` quick swatches + custom picker), underlines, strikes through, or adds a sticky Note with author name and saves<br>- **Then** annotations write cleanly into the PDF document via `PDFBox-Android` so standard desktop readers (Acrobat, Preview, Chrome) render them accurately |
| `US-051` | As a professional signing contracts, I want to draw reusable handwritten signatures (`SignaturePadRoute`), place them anywhere on a PDF, and inspect/apply PKCS#7 `.p12`/`.pfx` certificates | `P0` | - **Given** user opens `Signature` tab in `PdfEditorRoute`<br>- **When** user draws a signature (selecting pen ink & thickness), saves it to `SignatureEntity`, drags/resizes it onto target page, OR inspects/signs with a local `.p12`/`.pfx` certificate<br>- **Then** signature stamp flattens/embeds cleanly on Save, and PKCS#7 certificate details display `Issuer`, `Valid From/To`, and `Signature Status` cleanly |
| `US-052` | As a security-conscious user, I want to lock sensitive PDFs with an AES password or unlock a known password-protected PDF offline | `P0` | - **Given** user launches `Set Password` or `Remove Password` in `ToolsHubRoute` or Editor `File` menu<br>- **When** user enters/confirms password (`>= 4` chars) to lock, OR enters current password to decrypt<br>- **Then** `PDFBox-Android` + `BouncyCastle` outputs an `AES-128/256` encrypted (or unlocked) PDF without altering visual layout |

---

### 2.7 PDF Power Tools & Converters

| ID | User Story | Priority | Gherkin Acceptance Criteria |
|---|---|---|---|
| `US-060` | As a user combining documents, I want to select 2+ PDFs, reorder them visually, and merge into one clean PDF offline | `P0` | - **Given** user selects multiple PDFs in `Merge PDF`<br>- **When** user drags items to reorder, enters output filename, and confirms<br>- **Then** `PDFBox` merges all pages in exact sequence displaying `0%->100%` progress and opening `Success` result with Open/Share actions (gated upfront if batch size exceeds free limit -> clear Rewarded/Pro choice) |
| `US-061` | As a user extracting pages, I want a **real offline Split PDF** tool supporting custom page ranges, per-page split, or visual multi-select | `P0` | - **Given** user opens `Split PDF` in `ToolsHubRoute`<br>- **When** user selects visual page thumbnails OR types custom range (e.g., `1-3, 5`) and confirms<br>- **Then** `PDFBox` writes valid separated PDF file(s) (`_part1.pdf`, etc.) to output folder (zero *"Coming soon"* stub!) |
| `US-062` | As a user sending large attachments, I want real offline **PDF Compress** (Low/Medium/High presets) and **PDF Watermark** (custom text, angle, opacity) | `P0` | - **Given** user configures compression preset OR watermark text/opacity/angle/tiling<br>- **When** job executes offline via `PDFBox-Android`<br>- **Then** Compress shows exact Before -> After file size reduction; Watermark overlays crisp translucent stamps across selected/all pages cleanly |
| `US-063` | As a user converting documents, I want offline **Image -> PDF**, **Text -> PDF**, **Office/HWP/ODF -> PDF**, **PDF -> Images (or Long Image)**, **Offline PDF -> DOCX**, and **Tools Print** | `P0` | - **Given** user triggers any converter or `Print` inside `ToolsHubRoute` or Editor<br>- **When** operation executes completely offline (`LibreOfficeKit` / `PDFBox` / `ML Kit` OCR fallback for scanned PDF->DOCX / `PrintManager` for Print)<br>- **Then** output `.pdf`, `.png`, `.docx`, or Android Print Preview opens reliably with **zero server uploads** |

---

### 2.8 Document Scanner, Dual-Side ID Card Mode, OCR & QR

| ID | User Story | Priority | Gherkin Acceptance Criteria |
|---|---|---|---|
| `US-070` | As a mobile user digitizing paper, I want automatic multi-page perspective crop, filter enhancement, and instant PDF/Image export via the center Scan FAB | `P0` | - **Given** user taps center `SCAN` FAB on `MainShell`<br>- **When** `ML Kit Document Scanner` / `CameraX` captures 1 or more document pages<br>- **Then** user adjusts corners/rotation, applies filters (`Original`, `Magic Color`, `Grayscale`, `B&W`), reorders pages, and saves as PDF or PNGs cleanly |
| `US-071` | As a user scanning official IDs/passports, I want a working **Dual-Side ID Card Scan** workflow placing Front and Back cleanly onto one printable A4 PDF page | `P0` | - **Given** user switches scanner mode to `ID Card`<br>- **When** user scans Front side, then Back side<br>- **Then** app composites both cropped faces cleanly onto a centered single A4 sheet ready to export as `PDF` or `PNG` (zero *"Coming soon"* toast!) |
| `US-072` | As a user extracting text from images/scans, I want offline multi-script **OCR (`5` script packs covering `53+` languages)** and **QR/Barcode Scanner** | `P0` | - **Given** user runs `OCR Extract Text` (choosing `Latin`, `Chinese`, `Devanagari`, `Japanese`, or `Korean` script) OR opens `QR Scanner`<br>- **When** `ML Kit Text Recognition v2` or `ML Kit Barcode` processes image/frame offline<br>- **Then** OCR produces copyable/editable text exportable as `.txt` or `.docx`; QR safely classifies `URL`, `Wi-Fi`, `Contact`, `Email`, `Text` with explicit action buttons |

---

### 2.9 Storage Explorer, Pure-Java `ZIP/7z` Archive Manager & Templates

| ID | User Story | Priority | Gherkin Acceptance Criteria |
|---|---|---|---|
| `US-080` | As an advanced user, I want to browse device folders (`StorageBrowserRoute`), create folders, and run batch `Copy/Move/Delete/Share` operations | `P0` | - **Given** user opens `Storage` tab<br>- **When** user navigates folders (or picks folder via SAF if All-Files not granted), creates a folder, or long-presses files for batch operations<br>- **Then** actions reflect accurately on filesystem + MediaStore without orphaned index entries |
| `US-081` | As a productivity user, I want offline built-in starter **Templates** (`Word`, `Excel`, `PPT` blanks & business presets) so I can create documents cleanly from scratch | `P1` | - **Given** user taps `+ New Document` / `Templates` on Home or Tools<br>- **When** user chooses blank or bundled starter template (e.g., *Blank Word*, *Resume*, *Monthly Budget*, *Pitch Deck*)<br>- **Then** template clones safely into user Documents folder and opens immediately inside `OfficeEditorRoute` |

---

### 2.10 Wi-Fi Web/FTP File Transfer & Home Glance Widgets

| ID | User Story | Priority | Gherkin Acceptance Criteria |
|---|---|---|---|
| `US-090` | As a user moving files between phone and PC, I want both a **Wi-Fi Drag-and-Drop Web Browser UI** and an **FTP server** controlled from `WifiTransferRoute` or Quick Settings Tile | `P0` | - **Given** phone and PC share a LAN Wi-Fi network<br>- **When** user taps `Start Server` in `WifiTransferRoute` (or Quick Settings `TileService`)<br>- **Then** `Ktor CIO` foreground `dataSync` service starts on LAN IP (`port 8998`) with persistent stop notification + QR/URL copy, allows browser upload/download & FTP client transfers (supporting read-only & PIN lock), validates paths against traversal, and auto-stops after `10m` idle timeout |
| `US-091` | As an everyday user, I want Android Home Screen **Glance Widgets** (`Recent Files` & `Quick Scan/Search`) for 1-tap document access | `P1` | - **Given** user adds an E05 Glance widget to the launcher<br>- **When** recent files update or user taps Search / Scan / Doc item on widget<br>- **Then** widget updates cleanly within system budget and deep-links straight into `SearchDocumentsRoute`, `ScannerFlowRoute`, or target `DocumentReaderRoute` |

---

### 2.11 Cloud Storage Integration (`Google Drive` & `OneDrive`)

| ID | User Story | Priority | Gherkin Acceptance Criteria |
|---|---|---|---|
| `US-100` | As a cloud user, I want to connect my **Google Drive** and **Microsoft OneDrive** accounts to browse, open, and upload documents cleanly | `P1` | - **Given** user taps `Google Drive` or `OneDrive` in `StorageBrowserRoute`<br>- **When** user completes Google OAuth or Microsoft `MSAL` login<br>- **Then** account displays cleanly in `CloudAccountEntity`, remote folders paginate smoothly, downloading opens document locally, and edited/exported files upload back with resumable progress (`LargeFileUploadTask` / `MediaHttpDownloader`); disconnecting wipes stored token alias cleanly |

---

### 2.12 Ethical Hybrid Monetization, Accessibility & Reliability

| ID | User Story | Priority | Gherkin Acceptance Criteria |
|---|---|---|---|
| `US-110` | As a free user, I want clear upfront Rewarded Video unlocks for Pro utilities without annoying ads interrupting my reading/editing or blocking `Save` | `P0` | - **Given** non-Pro user operates inside `OfficeEditorRoute`, `PdfEditorRoute`, or `ScannerFlowRoute`<br>- **When** user switches apps (`onPause -> onResume`), drags pages, or taps `Save`<br>- **Then** **zero** App-Open or Interstitial ads appear; capped Interstitials fire strictly post-milestone (`>= 60s` cooldown), and Pro tools ask upfront *(Watch 1 Rewarded Ad or Upgrade)* rather than after finishing work |
| `US-111` | As a Pro subscriber, I want flexible Play Billing plans (`Weekly`, `Annual w/ 7-day Trial`, `Lifetime Ad-Free`) synced cleanly across reinstalls via Restore Purchases | `P0` | - **Given** user opens `PaywallRoute` or taps `Restore Purchases` in `SettingsRoute`<br>- **When** `BillingClient 8.0.0` completes purchase or queries `queryPurchasesAsync`<br>- **Then** `ProStatus` activates instantaneously, hides all ad slots globally (`Banner`, `Native`, `Interstitial`, `App-Open`), unlocks all Pro features offline via cached signed entitlement, and persists across cold restarts |
| `US-112` | As a user relying on dark mode, tablets, or accessibility TalkBack, I want high-contrast Adaptive Light/Dark themes and full `>= 48dp` touch targets everywhere | `P0` | - **Given** system Dark Mode, TalkBack enabled, or Tablet landscape layout<br>- **When** user navigates every screen and bottom editor shelf<br>- **Then** interactive controls meet `>= 48dp x 48dp` bounds, text meets WCAG AA (`>= 4.5:1`), all icon buttons expose localized `contentDescription`, and layouts adapt cleanly without clipped controls |
