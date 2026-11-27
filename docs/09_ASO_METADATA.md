# Phase 8 — App Store Optimization (ASO) Metadata & Conversion Kit (`09_ASO_METADATA.md`)

**Project:** Evora Office (`E05`) — Native Android All-Document Reader, Editor & PDF Tools  
**Company:** Evora Technologies Global  
**Task ID:** `T-027` (Phase 8 — Store Release Preparation & ASO)  
**Package Name (`applicationId`):** `com.evora.technologies.office`  
**Release Target:** `versionName = "1.0.0"`, `versionCode = 100`  
**Owner Personas:** `app-store-optimizer` + `mobile-release-engineer`  
**Date:** `2026-11-27`  
**Status:** **READY FOR CEO REVIEW & PLAY CONSOLE SUBMISSION** *(Post-Gate 3 `Zero Blockers`)*  
**Upstream Inputs:** [`01_MARKET_RESEARCH.md`](01_MARKET_RESEARCH.md) · [`01_COMPETITOR_REVERSE_ENGINEERING.md`](01_COMPETITOR_REVERSE_ENGINEERING.md) · [`02_PROJECT_CHARTER.md`](02_PROJECT_CHARTER.md) · [`03_PRD.md`](03_PRD.md) · [`05_DESIGN_SYSTEM.md`](05_DESIGN_SYSTEM.md) · [`08_QA_TEST_REPORT.md`](08_QA_TEST_REPORT.md) · [`office_viewer_ua_business_evaluation.md`](../research/phase1/office_viewer_ua_business_evaluation.md)

---

## 1. Dated Decision & Platform Scope Table

| Date | Decision / Gate ID | Owner | Specification & ASO Scope Impact |
|---|---|---|---|
| `2026-11-25` | `PD-01` | CEO | **Native Android Only (Google Play Store):** App is Kotlin + Jetpack Compose + MVI (`minSdk 26`, `targetSdk 36`). No iOS binary exists. |
| `2026-11-25` | `CD-02`, `CD-04` | CEO | **Single Full-Parity `v1.0.0` (`100`), 100% Offline, Honest Scope:** Bundle all working modules (`DOC/DOCX`, `XLS/XLSX`, `PPT/PPTX`, `ODT/ODS/ODP`, `HWP/HWPX`, `PDF`, `EPUB`, `TXT/CSV`, `ZIP/7z`). Explicitly exclude `.rar` and Cloud AI claims from store copy to eliminate expectation-mismatch 1★ reviews. |
| `2026-11-25` | `CD-03` | CEO | **Tier B UA (`$1,000–$5,000/mo`):** Allocate **40%** of budget to ASO & Store Listing CVR Experiments and **60%** to strictly geo-segmented Google App Campaigns (`Tier-1` vs `Tier-2/3`). |
| `2026-11-25` | `CD-05`, `CD-06` | CEO | **Brand & Visual Style:** In-app name `Evora Office`; Play Store title `Evora Office: DOCX, PDF, XLSX` (`29/30` chars); Style B Vibrant Utility (`#2489FF` / `#186FD4`, `#0F172A` dark header). Adaptive vector launcher icon (`ic_launcher_foreground.xml`) ships for `v1.0.0` while custom brand mark A/B testing queues post-launch. |
| `2026-11-25` | `CD-07` | CEO | **Transparent Monetization & Ad-Free Update (`2026-11-27`):** All ad SDKs/placements/Rewarded Ad gates cleared until ads are scheduled; all viewing, editing, PDF tools, Dual ID Card, PKCS#7, and PDF-to-DOCX run locally with zero ads, while optional Evora Pro Play Billing remains available. |
| `2026-11-27` | `Gate 3` | CEO | **QA Zero Blockers Approved:** Cold start `910.70 ms` (`SM-A165F`), `84/84` unit tests, `25/25` `16 KB` `.so`, `EVORA_LOG_VDI_CALLS=0` unlocked `T-027`. |
| `2026-11-27` | `T-027` | `app-store-optimizer` | **10-Locale Store Metadata Generated & Verified:** Programmatically validated character limits across all 10 supported app locales under [`../fastlane/metadata/android/`](../fastlane/metadata/android/en-US/title.txt). |

### 1.1 Platform Scope Mapping (Android Google Play vs. Cross-Platform Lifecycle Template)

Per `PD-01`, E05 targets **Google Play Store only**. The mapping below aligns E05 deliverables with the corporate lifecycle checklist:

| Lifecycle Template Field | Google Play (Active E05 Target) | Apple App Store (`PD-01` Disposition) |
|---|---|---|
| **App Title** | **30 chars max** (`Evora Office: DOCX, PDF, XLSX`, `29/30` EN) | `N/A — Android-Only Native Build (PD-01)` |
| **Subtitle / Short Description** | **Google Play Short Description (80 chars max)** across 10 locales | `N/A — Android-Only Native Build (PD-01)` |
| **100-Character Keywords** | Mapped to **100-char Core ASO Seed Bank (`99/100` chars)** + Play Store 5 Tags + Google App Campaigns text assets *(Play Console has no hidden keyword field; indexing comes from Title, Short Desc, Full Desc & Package ID)* | `N/A — Android-Only Native Build (PD-01)` |
| **Promotional Text (170 chars)** | Covered by **Short Description (`<= 80` chars)** + **What's New (`<= 500` chars)** | `N/A — Android-Only Native Build (PD-01)` |
| **Full Description** | **4,000 chars max** (`en-US`, `vi`, plus 8 localized markets in `fastlane/metadata/android/`) | `N/A — Android-Only Native Build (PD-01)` |
| **Screenshot Storyline** | **8 Phone (`1080×1920`) + Tablet (`7-inch` / `10-inch`)** + `1024×500` Feature Graphic + App Preview Video | `N/A — Android-Only Native Build (PD-01)` |

---

## 2. ASO Objectives, Competitive Wedge & Conversion Targets

### 2.1 Quantitative ASO & Growth Targets

| Metric | Benchmark Baseline (`01_MARKET_RESEARCH.md` / `02_PROJECT_CHARTER.md`) | E05 Target (`[Internal Target]` / `[Estimate]`) | Tracking Source |
|---|---|---|---|
| **Store Listing Conversion Rate (CVR)** | Productivity utility baseline | **`25% – 35%`** `[Internal Target]` | Play Console Store Analysis & Listing Experiments |
| **Google Play Rating** | Competitor `4.4★` EN / `4.2★` VN; Simple Design `~4.7★` | **`>= 4.7★`** `[Internal Target]` | Play Console Ratings & Reviews |
| **1-Star Review Share** | Competitor `11.9%` EN (`45,083/378,971`) / `17.1%` VN (`64,770/378,971`) | **`<= 4.0%`** `[Internal Target]` | Post-launch review tag clustering |
| **Organic Share of Total Installs** | Tier-1 break-even needs `30%–62%+`; Tier-2/3 needs `>= 35%–50%` `[Estimate]` | **`>= 40%` blended organic** (`10–20+` top-10 vertical long-tail ranks by M3) `[Internal Target]` | Play Console Acquisition |
| **Android Vitals Technical ASO Floor** | Charter SLA (`02_PROJECT_CHARTER.md` §2.2) | **Crash-free `>= 99.8%`**, **ANR `<= 0.15%`**, Cold Start **`910.70 ms` (`< 1.2s`)** | Android Vitals + Macrobenchmark (`SM-A165F`) |

### 2.2 Competitive Positioning Wedge (Verified Against Codebase)

Competitor reverse engineering ([`01_COMPETITOR_REVERSE_ENGINEERING.md`](01_COMPETITOR_REVERSE_ENGINEERING.md)) proved why *Word Document: DOCX, PDF, XLSX* (`94,749,451` installs) suffers `11.9%–17.1%` 1-star reviews while *Document Reader - PDF Editor* (Simple Design) sustains `~4.7★`. Every positioning claim in Evora Office's Play Store copy corresponds directly to verified code in [`08_QA_TEST_REPORT.md`](08_QA_TEST_REPORT.md):

1. **Zero Save-Lock Ransom (`CD-07`):** Editing and saving Word/Excel/PPT documents in `OfficeEditorRoute` never triggers a `SAVE_HEADER` paywall lock, and `DraftRecoveryEntity` checkpoints dirty edits every `15s` (surviving process death on `SM-A165F`).
2. **Zero Ads in App:** All AdMob/Mediation/UMP/Adjust SDKs and Rewarded Ad gates are cleared; users work across readers, editors, scanner, and PDF power tools with zero ad interruptions.
3. **100% Offline Privacy (`$0` Server Uploads):** `LibreOfficeKit` (`14` `16 KB` ARM64 `.so`), `PdfiumAndroid`, `PDFBox-Android`, `hwplib/hwpxlib`, `Apache Commons Compress`, and `ML Kit` run locally on-device — notably **Hangul `.hwp/.hwpx`** never uploads files to any third-party server.
4. **Real Tools Replacing Competitor Stubs:** Split PDF, Compress PDF, Watermark PDF, Tools Print (`PrintManager`), Dual-Side ID Card to A4, Word Track Changes (`Review` tab), Continuous Scroll, and pure-Java `.zip`/`.7z` extraction all execute offline.
5. **Policy-Clean Packaging & Performance:** `targetSdk 36`, `22/22` `.so` aligned to `16 KB`, `allowBackup="false"`, and zero `AD_ID` / `SYSTEM_ALERT_WINDOW` / lockscreen overlay permissions.

---

## 3. Keyword Strategy, Play Tags & Competitive Gaps

### 3.1 Keyword Tier Architecture (Google Play Indexing & Intent Matrix)

Because Google Play indexes `packageName` (`com.evora.technologies.office` already contains `office`), **Title** (`30` chars — highest weight), **Short Description** (`80` chars), and **Full Description** (natural semantic repetition `2–4x` without stuffing), keywords are distributed by tier:

| Tier | Target Keywords | Relevance | Competition | Placement Strategy |
|---|---|---|---|---|
| **Primary Head (Title + Short)** | `office`, `docx`, `pdf`, `xlsx`, `word`, `excel`, `ppt`, `editor`, `reader`, `scanner`, `file manager` | `10/10` | Very High (`50M–1B+` incumbents) | Title (`Evora Office: DOCX, PDF, XLSX`) + Short Description (`Offline Word, Excel, PPT & PDF editor, scanner, HWP viewer & file manager.`) |
| **Mid-Tail Utility (Short + Hook)** | `pdf editor`, `pdf reader`, `split pdf`, `compress pdf`, `merge pdf`, `pdf to docx`, `ocr scanner`, `zip extractor` | `9/10` | High | Bullet headers + Paragraph 1–3 of Full Description (`2–3x` natural cadence) |
| **Long-Tail High-Intent (Gap Capture)** | `offline docx editor android`, `hwp hwpx viewer offline`, `dual side id card scan pdf`, `sign pdf pkcs7`, `wifi transfer phone to pc`, `odt ods odp viewer`, `add watermark pdf offline`, `7z file extractor` | `9–10/10` | Low–Medium | Dedicated section bullets + localized descriptions (`ko-KR` leads with `HWP/HWPX`, `vi`/`pt-BR`/`id` emphasize PDF Compress/ID Scan) |

### 3.2 Competitor Keyword & Feature Gaps Exploited

1. **`offline hwp viewer` / `hwp hwpx` (`ko-KR` & global Korean diaspora):** Top global viewers either skip `.hwp` or upload user documents to conversion servers (`50 MB` cap). Evora runs `hwplib`/`hwpxlib` 100% locally.
2. **`split pdf` / `watermark pdf` / `id card scanner a4`:** High-intent queries where users churn from competitors hitting `"Coming soon"` toasts; Evora demonstrates real results in Screenshots 4 & 5.
3. **`offline docx editor no ads on save` / `privacy office app` (`de-DE`, `fr-FR`, `en-US`):** Captures disillusioned users leaving 1★ reviews on ad-heavy document viewers.
4. **`wifi file transfer browser ftp`:** High retention secondary workflow query rarely highlighted by standard PDF viewers.

### 3.3 100-Character Core Keyword Seed Bank & Play Console Category Tags

- **Core ASO Keyword Seed (`99/100` chars, programmatically verified):**
  ```text
  docx,pdf,xlsx,word,excel,ppt,hwp,reader,editor,scanner,ocr,sign,merge,split,compress,zip,7z,offline
  ```
- **Google Play Store Category & Tags (`[Play Console Tag Selection]`):**
  - **Application Type:** `App`
  - **Category:** `Productivity`
  - **Recommended Play Console Tags (max 5):** `Productivity`, `Document viewer`, `Document editor`, `PDF tools`, `Scanner`

---

## 4. Localized Store Metadata (`10` Target Markets)

All 10 markets matching [`locales_config.xml`](../app/src/main/res/xml/locales_config.xml) (`AppLanguage` enum) have been generated under `fastlane/metadata/android/` and verified against Google Play's strict character ceilings (`Title <= 30`, `Short Description <= 80`, `Full Description <= 4,000`, `What's New <= 500`):

| Play Locale | Title (`<= 30`) | Len | Short Description (`<= 80`) | Len | Full (`<= 4,000`) | Log (`<= 500`) | Fastlane Source |
|---|---|---:|---|---:|---:|---:|---|
| `en-US` | `Evora Office: DOCX, PDF, XLSX` | `29` | `Offline Word, Excel, PPT & PDF editor, scanner, HWP viewer & file manager.` | `74` | `3,947` | `370` | [`en-US`](../fastlane/metadata/android/en-US/full_description.txt) |
| `vi` | `Evora Office: DOCX, PDF, XLSX` | `29` | `Đọc & sửa Word, Excel, PPT, PDF, HWP ngoại tuyến. Tách, nén PDF & quét OCR.` | `75` | `3,394` | `357` | [`vi`](../fastlane/metadata/android/vi/full_description.txt) |
| `ko-KR` | `Evora Office: HWP, DOCX, PDF` | `28` | `오프라인 HWP/HWPX, 워드, 엑셀, PPT, PDF 편집·변환 및 스캔 문서 관리` | `48` | `976` | `190` | [`ko-KR`](../fastlane/metadata/android/ko-KR/full_description.txt) |
| `ja-JP` | `Evora Office: PDF, Word, Excel` | `30` | `オフラインでDOCX・XLSX・PPTX・PDF閲覧編集、分割・圧縮・OCRスキャン。` | `43` | `762` | `179` | [`ja-JP`](../fastlane/metadata/android/ja-JP/full_description.txt) |
| `es-419` | `Evora Office: DOCX, PDF, XLSX` | `29` | `Edita Word, Excel, PPT y PDF sin internet. Escáner, OCR, ZIP y archivos.` | `72` | `1,659` | `283` | [`es-419`](../fastlane/metadata/android/es-419/full_description.txt) |
| `pt-BR` | `Evora Office: DOCX, PDF, XLSX` | `29` | `Edite Word, Excel, PPT e PDF offline. Divida, comprima, assine e escaneie.` | `74` | `1,693` | `299` | [`pt-BR`](../fastlane/metadata/android/pt-BR/full_description.txt) |
| `id` | `Evora Office: DOCX, PDF, XLSX` | `29` | `Baca & edit Word, Excel, PPT, PDF offline. Gabung, kompres & pindai dokumen.` | `76` | `1,674` | `306` | [`id`](../fastlane/metadata/android/id/full_description.txt) |
| `hi-IN` | `Evora Office: DOCX, PDF, XLSX` | `29` | `ऑफ़लाइन Word, Excel, PPT व PDF एडिटर, स्कैनर, OCR और फ़ाइल मैनेजर।` | `66` | `1,559` | `290` | [`hi-IN`](../fastlane/metadata/android/hi-IN/full_description.txt) |
| `de-DE` | `Evora Office: DOCX, PDF, XLSX` | `29` | `Word, Excel, PPT & PDF offline bearbeiten, komprimieren, signieren & scannen.` | `77` | `1,890` | `311` | [`de-DE`](../fastlane/metadata/android/de-DE/full_description.txt) |
| `fr-FR` | `Evora Office: DOCX, PDF, XLSX` | `29` | `Éditeur hors ligne Word, Excel, PPT, PDF, scanner, OCR et gestion de fichiers.` | `78` | `1,927` | `309` | [`fr-FR`](../fastlane/metadata/android/fr-FR/full_description.txt) |

### 4.1 English (`en-US`) Store Listing Copy

- **Title (`29/30` chars):** `Evora Office: DOCX, PDF, XLSX`
- **Short Description (`74/80` chars):** `Offline Word, Excel, PPT & PDF editor, scanner, HWP viewer & file manager.`
- **Release Notes (`100.txt`, `370/500` chars):**
  ```text
  Evora Office v1.0.0 Launch:
  • View & edit DOCX, XLSX, PPTX, ODT, ODS, ODP offline
  • Full PDF reader, annotations, e-Sign, PKCS#7, Split, Compress, Watermark & PDF to DOCX
  • Offline Hangul (HWP/HWPX), EPUB, TXT, CSV, ZIP & 7z viewer
  • Smart Document Scanner, Dual-Side ID Card to A4 & 5-script offline OCR
  • Wi-Fi Web & FTP transfer, Google Drive, OneDrive & Home widgets
  ```
- **Full Description (`3,947/4,000` chars):**
  ```text
  Evora Office is a 100% offline Android document reader, rich Office editor, PDF power toolkit, document scanner, and local file manager built for speed and privacy. Open, edit, save, sign, scan, and organize your documents directly on your device without server uploads or save-lock paywalls.

  KEY CAPABILITIES

  ALL-IN-ONE DOCUMENT READER & FILE MANAGER
  • Automatically index and organize local PDF, Word, Excel, PowerPoint, OpenDocument, Hangul, Ebook, Text, and ZIP/7z files across device storage, Downloads, and SD cards (or browse safely with Android Folder Picker SAF).
  • Filter by format, sort by date, name, size, or type, search filenames in milliseconds, pin home screen widgets, and recover deleted files within 30 days from Trash.
  • Comfort reading modes: Light, Night Dark, and Sepia themes, Vertical Continuous Scroll or Single-Page Snap, Reflow View, Auto-Scroll, Text-to-Speech read-aloud, outline navigation, and persistent page/zoom memory.

  RICH OFFLINE OFFICE EDITOR (WORD, EXCEL, POWERPOINT)
  • Word (DOC, DOCX, DOTX, DOCM, ODT): Font styling, colors, highlights, alignment, lists, image/shape insertion, freehand ink, Review Track Changes and Comments, plus 15-second atomic draft recovery.
  • Excel (XLS, XLSX, XLTX, XLSM, ODS): Multi-sheet tabs, formula bar (SUM, AVERAGE, COUNT, MIN, MAX, IF), cell merging, number formats, freeze panes, and row/column sizing.
  • PowerPoint (PPT, PPTX, PPS, PPSX, POT, ODP): Slide manager, text/shape formatting, Z-order arrangement, presenter notes, and fullscreen SlideShow mode with laser ink.
  • Built-in offline starter templates (Resume CV, Meeting Notes, Monthly Budget, Invoice Sheet, Pitch Deck).
  • Viewing, editing, and saving your documents is always free with zero save-lock interruptions.

  COMPLETE PDF EDITOR & POWER TOOLS
  • Annotate PDFs with highlights, underlines, strikethroughs, freehand ink, sticky notes, and reusable handwritten e-signatures.
  • Merge PDFs, split by page range or every N pages, compress PDFs (Low/Medium/High DPI), apply custom text watermarks, lock/unlock AES-128/256 passwords, and print via Android PrintManager.
  • Convert Image to PDF, Text to PDF, Office/ODF/HWP to PDF, PDF to PNG images, and offline PDF to DOCX.
  • Inspect and sign PDFs with PKCS#7 (.p12/.pfx) digital certificates.

  HANGUL (HWP/HWPX), EPUB & ZIP/7Z ARCHIVES
  • Read Hangul (.hwp, .hwpx, .hwt, .hwdt), EPUB, TXT, CSV, Log, and Markdown files completely offline.
  • Browse and extract password-protected or standard .zip and .7z archives locally (RAR archives are not supported).

  SMART SCANNER, DUAL ID CARD, OFFLINE OCR & QR
  • Scan multi-page documents with edge crop, rotation, and image filters.
  • Dual-Side ID Card mode captures front and back onto a single printable A4 PDF or PNG.
  • On-device OCR extracts editable text across 5 script families (Latin, Chinese, Devanagari, Japanese, Korean) to TXT or DOCX.
  • Scan QR codes and barcodes for URLs, Wi-Fi, contacts, and text.

  WI-FI TRANSFER & CLOUD DRIVES
  • Share files with PC or Mac browsers over local Wi-Fi via drag-and-drop HTTP Web UI or local FTP with optional PIN lock, read-only toggle, and Quick Settings tile.
  • Optional user-initiated sync with Google Drive and Microsoft OneDrive using encrypted local token storage.

  ALL FEATURES INCLUDED & EVORA PRO
  Evora Office includes zero ads. Core document reading, Office editing and saving, PDF Merge, Split, Compress, Watermark, AES password protection, PKCS#7 digital signing, PDF-to-DOCX, scanning, Dual ID Card layout, OCR, Print, and Wi-Fi Transfer work completely locally on your device. Optional Evora Pro subscriptions and Lifetime unlock are available via Google Play Billing to support ongoing offline development.

  PERMISSION & PRIVACY NOTE
  Evora Office requests All-Files Access (MANAGE_EXTERNAL_STORAGE) strictly to index, edit, convert, and organize local document folders offline on your device, with Folder Picker (SAF) always available as an alternative.
  ```

### 4.2 Vietnamese (`vi`) Store Listing Copy

- **Title (`29/30` chars):** `Evora Office: DOCX, PDF, XLSX`
- **Short Description (`75/80` chars):** `Đọc & sửa Word, Excel, PPT, PDF, HWP ngoại tuyến. Tách, nén PDF & quét OCR.`
- **Release Notes (`100.txt`, `357/500` chars):**
  ```text
  Ra mắt Evora Office v1.0.0:
  • Đọc & sửa DOCX, XLSX, PPTX, ODT, ODS, ODP ngoại tuyến 100%
  • Công cụ PDF: Ghi chú, Ký tên, Tách, Nén, Đóng dấu, Bảo mật AES & PDF sang DOCX
  • Mở HWP/HWPX, EPUB, TXT, CSV, ZIP và 7z không cần mạng
  • Quét tài liệu, Ghép CCCD 2 mặt ra A4, OCR ngoại tuyến 5 nhóm chữ
  • Chuyển file qua Wi-Fi Web/FTP, Google Drive, OneDrive & Widget
  ```
- **Full Description (`3,394/4,000` chars):**
  ```text
  Evora Office là bộ ứng dụng văn phòng, đọc và chỉnh sửa tài liệu, công cụ PDF, máy quét OCR và quản lý tệp ngoại tuyến 100% trên Android. Mở, sửa, lưu, ký tên, nén và sắp xếp tài liệu ngay trên điện thoại mà không tải tệp lên máy chủ hay khóa nút Lưu.

  TÍNH NĂNG NỔI BẠT

  ĐỌC MỌI ĐỊNH DẠNG & QUẢN LÝ TÀI LIỆU
  • Tự động quét và sắp xếp tệp PDF, Word, Excel, PowerPoint, OpenDocument, Hangul (HWP), EPUB, TXT, CSV và kho lưu trữ ZIP/7z trong bộ nhớ máy, Tải xuống và thẻ SD (hoặc chọn thư mục an toàn qua SAF).
  • Lọc theo định dạng, tìm kiếm tức thì, ghim Widget màn hình chính và khôi phục tệp đã xóa trong Thùng rác 30 ngày.
  • Chế độ đọc thoải mái: Nền Sáng, Ban đêm, Sepia; cuộn dọc liên tục hoặc từng trang; Reflow co giãn chữ; tự động cuộn; đọc thành tiếng (TTS) và ghi nhớ vị trí trang đang đọc.

  SOẠN THẢO OFFICE NGOẠI TUYẾN (WORD, EXCEL, POWERPOINT)
  • Word (DOC, DOCX, DOTX, DOCM, ODT): Định dạng chữ, màu sắc, căn lề, danh sách, chèn ảnh/hình khối, vẽ tay, theo dõi thay đổi (Track Changes) & bình luận, tự động lưu bản nháp mỗi 15 giây.
  • Excel (XLS, XLSX, XLTX, XLSM, ODS): Quản lý nhiều trang tính, thanh công thức (SUM, AVERAGE, COUNT, MIN, MAX, IF), gộp ô, định dạng số/tiền tệ, cố định dòng/cột.
  • PowerPoint (PPT, PPTX, PPS, PPSX, POT, ODP): Chỉnh sửa trang chiếu, định dạng hình khối, sắp xếp lớp, ghi chú thuyết trình và trình chiếu toàn màn hình kèm bút laser.
  • Kèm sẵn 8 mẫu văn bản, bảng tính ngân sách, hóa đơn và bài thuyết trình ngoại tuyến.
  • Xem, chỉnh sửa và lưu tài liệu luôn miễn phí — không khóa tính năng hay gián đoạn khi nhấn Lưu.

  BỘ CÔNG CỤ PDF CHUYÊN SÂU
  • Ghi chú PDF: tô sáng, gạch chân, gạch ngang, vẽ tay, ghi chú dán và chèn chữ ký viết tay lưu sẵn.
  • Gộp PDF, Tách PDF theo trang, Nén giảm dung lượng PDF, Đóng dấu chìm (Watermark), Đặt/Gỡ mật khẩu AES-128/256 và In qua hệ thống Android.
  • Chuyển đổi Ảnh sang PDF, Văn bản sang PDF, Office/HWP sang PDF, PDF sang ảnh PNG và chuyển PDF sang DOCX ngay trên thiết bị.
  • Kiểm tra và ký chứng thư số PKCS#7 (.p12/.pfx).

  HỖ TRỢ HWP/HWPX, EPUB & GIẢI NÉN ZIP/7Z
  • Đọc tệp Hangul (.hwp, .hwpx), EPUB, TXT, CSV, Markdown hoàn toàn ngoại tuyến.
  • Duyệt và giải nén tệp .zip, .7z kể cả có mật khẩu (không hỗ trợ định dạng .rar).

  QUÉT TÀI LIỆU, GHÉP CCCD 2 MẶT, OCR & QR
  • Quét tài liệu nhiều trang, tự cắt mép, xoay và bộ lọc màu.
  • Chế độ Thẻ ID / CCCD 2 mặt tự động ghép mặt trước và mặt sau lên 1 trang A4 sẵn sàng in.
  • OCR ngoại tuyến nhận diện chữ từ ảnh/PDF (Latin, Trung, Nhật, Hàn, Devanagari) xuất ra TXT hoặc DOCX.
  • Quét mã QR & mã vạch (URL, Wi-Fi, danh bạ).

  CHUYỂN FILE WI-FI & ĐÁM MÂY
  • Chia sẻ tệp với máy tính qua trình duyệt Web kéo-thả hoặc FTP nội bộ có khóa PIN và chế độ chỉ đọc.
  • Tùy chọn kết nối Google Drive và Microsoft OneDrive để tải xuống hoặc tải lên tệp của bạn.

  MINH BẠCH MIỄN PHÍ & KHÔNG QUẢNG CÁO
  Evora Office hiện không chứa quảng cáo. Toàn bộ tính năng đọc, sửa/lưu tài liệu, Gộp/Tách/Nén PDF, Watermark, Đặt/Gỡ mật khẩu AES, Ký số PKCS#7, PDF sang DOCX, Quét tài liệu, Ghép CCCD 2 mặt, OCR, QR, In và Wi-Fi đều hoạt động cục bộ trên thiết bị. Tùy chọn Evora Pro qua Google Play Billing giúp ủng hộ phát triển lâu dài.

  QUYỀN TRUY CẬP TỆP & QUYỀN RIÊNG TƯ
  Evora Office yêu cầu quyền Truy cập tất cả tệp (MANAGE_EXTERNAL_STORAGE) chỉ để lập chỉ mục, đọc, sửa và quản lý tài liệu trên bộ nhớ máy hoàn toàn ngoại tuyến; bạn luôn có thể dùng Trình chọn thư mục (SAF) thay thế.
  ```

### 4.3 Regional Positioning Matrix (`8` Secondary Localized Markets)

| Market | Positioning Angle in Localized Full Copy |
|---|---|
| `ko-KR` | Leads title with `HWP, DOCX, PDF` (`28/30`); highlights zero-server-upload offline `HWP/HWPX`, Korean OCR, and Dual ID Card A4 export. |
| `ja-JP` | Leads with precision office formatting, Japanese offline OCR, PDF Split/Compress/Sign, and clean privacy (`$0` server uploads). |
| `es-419` & `pt-BR` | Highlights free offline Word/Excel/PPT saving without lock paywalls, small compressed footprint, Dual ID Card A4 (`RG/CNH`), and ad-free experience. |
| `id` & `hi-IN` | Emphasizes fast low-RAM document opening (`910.70 ms`), offline ZIP/7z extraction, Devanagari OCR (`hi-IN`), and Wi-Fi transfer without data usage. |
| `de-DE` & `fr-FR` | Leads with local GDPR/offline privacy (`100%` local document processing, zero ads/trackers), PKCS#7 certificates, Track Changes, and AES-256 PDF security. |

---

## 5. Visual Assets & 8-Slide Screenshot Storyline

### 5.1 App Icon & Feature Graphic Specifications

1. **Google Play High-Res Icon (`512 × 512` px, 32-bit PNG, max `1024 KB`):**
   - **Current `v1.0.0` Implementation (`CD-06` + Phase 7 Fix):** Vector adaptive icon defined in [`ic_launcher_background.xml`](../app/src/main/res/drawable/ic_launcher_background.xml) (`#0F172A` navy base with `#1E3A8A` curved accent wave) and [`ic_launcher_foreground.xml`](../app/src/main/res/drawable/ic_launcher_foreground.xml) (folded-corner `#2563EB` / `#93C5FD` document silhouette with white horizontal rule lines). Export at `512 × 512` px with Google Play standard masked corners applied automatically by Play Store.
   - **Post-Launch Experiment (`CD-06`):** Queue custom brand icon vs. multi-format badge (`PDF + DOCX + XLSX` corner tag) in Store Listing Experiment #1.
2. **Feature Graphic (`1024 × 500` px, PNG or 24-bit JPEG, max `15 MB`):**
   - **Layout:** Left `45%` displays `Evora Office` monogram + bold headline `"Offline Office & PDF Suite"` (`Poppins Bold`, `#F1F5F9` on `#0F172A` navy-to-`#186FD4` gradient); Right `55%` renders angled phone mockup showing `HomeDocumentsRoute` format color chips (`Word #1563DB`, `PDF #D33434`, `Excel #1D820E`, `PPT #B85600`, `HWP #0074BF`) + `ZERO SAVE LOCKS · 100% OFFLINE` trust pill (`#3CC79A`). Keep all text inside `88 px` horizontal / `64 px` vertical safe margins for Play video-thumbnail overlay cropping.

### 5.2 8-Screen Phone (`1080 × 1920`, `9:16`) & Tablet (`7-inch` / `10-inch`) Screenshot Storyline

All mockups embed real UI captures verified on `SM-A165F` during Phase 7 QA (`.local/qa_evidence/`):

| Slide | Role in Funnel | Top Headline (`<= 5` Words, `Poppins Bold 72px`) | Subcaption (`<= 9` Words) | Source App Screen & QA Evidence |
|---|---|---|---|---|
| **1 (Hero)** | Immediate value & format breadth | **All Docs. Zero Save Locks.** | *View & edit Word, Excel, PPT, PDF & HWP offline* | `HomeDocumentsRoute` (`06c_home_library_fixed.png`) showing format chips (`All`, `PDF`, `Word`, `Excel`, `PPT`, `ODF`), color-coded `DocCard` badges, and center Scan FAB |
| **2 (Core Edit)** | Beat competitor's edit/save pain | **Edit Office 100% Offline** | *Rich DOCX, XLSX formulas & PPT slides with auto-draft* | `OfficeEditorRoute` (`31_office_editor_open.png`) displaying Word navy topbar, `LOK 16KB Active`, `Done` button with zero paywall, and formatting bar |
| **3 (PDF Reader)** | Core PDF comfort & speed | **Fast PDF Reader & e-Sign** | *Night & Sepia modes, reflow, TTS & handwritten sign* | `DocumentReaderRoute` (`16b_pdf_fixed.png`) showing crisp `"1 / 2"` page pill, `Light/Sepia/Night/TTS` bottom switcher, and PDF editing trigger |
| **4 (PDF Power)** | Beat competitor `"Coming soon"` stubs | **Split, Compress & Watermark** | *Real offline PDF tools, AES lock & PDF to DOCX* | `ToolsHubRoute` (`30_tools_tab.png`) showing `Convert` + `PDF Power` pastel tiles |
| **5 (Scan & ID)** | High-intent daily utility | **Scan Docs & Dual ID Cards** | *Auto-crop, A4 front+back ID & 5-script offline OCR* | `ScannerFlowRoute` Dual-Side ID Card A4 preview + OCR 5-script export chip |
| **6 (Wi-Fi + Cloud)** | Cross-device productivity | **Wi-Fi Transfer & Cloud Sync** | *Drag-drop to PC browser, FTP PIN, Drive & OneDrive* | `WifiTransferRoute` (`35b_wifi_transfer_running.png`) showing QR + `http://192.168.x.x:8998`, FTP toggle & PIN lock + `StorageBrowserRoute` (`29_storage_tab.png`) |
| **7 (Niche Formats)** | Korea / Power user differentiator | **HWP, ODF, EPUB, ZIP & 7z** | *Zero server uploads — private files stay on device* | `DocumentReaderRoute` HWP (`18_hwp_viewer.png`) paired with `ArchiveInspectorRoute` `.zip`/`.7z` entry list |
| **8 (Trust & Pro)** | Transparent monetization | **Save Free. Zero Ads.** | *All local utilities included; optional Evora Pro support* | `PaywallRoute` (`03_paywall.png`) highlighting `"Viewing, editing & saving your docs is always 100% free."` and `7-day free trial` |

- **Tablet (`7-inch` & `10-inch` `16:10` / `2560 × 1600`) Requirement:** Capture `Expanded (>= 840dp)` two-pane layouts (`NavigationRail` + `StorageBrowserRoute` tree/files and `OfficeEditorRoute` landscape/split view) for Google Play's **"Designed for tablets"** quality badge.
- **Screenshot Localization Priority:** Localize overlay headlines and captions across all 10 store markets; swap Slide 1 vs. Slide 7 order in **`ko-KR`** so **Offline HWP/HWPX + Word/PDF** occupies Slide 1 Hero.

### 5.3 App Preview Video (`24s`, `1080 × 1920` Vertical / Unlisted YouTube Link)

- **0–3s (Hook):** Document opens instantaneously (`910.70 ms` launch) -> caption *"Open any document offline — zero save locks."*
- **3–18s (Core Proof):** Quick-cut walkthrough across `HomeDocumentsRoute` chips -> `OfficeEditorRoute` Word edit + tap `Done` saving cleanly -> `ToolsHubRoute` Split & Compress PDF (`size saved` badge) -> `ScannerFlowRoute` Dual ID Card A4 scan -> `WifiTransferRoute` PC browser drag-drop.
- **18–24s (Trust & CTA):** `"100% Offline · HWP, DOCX, PDF, XLSX, ZIP/7z"` + `"Download Evora Office Free"`.

---

## 6. Store Listing A/B Testing, Tier B UA & Post-Launch Review Loop

### 6.1 Play Console Experiments Roadmap (`40%` of Tier B Budget — `CD-03`)

Run sequential `90%` statistical-confidence Google Play Store Listing Experiments (never test multiple elements simultaneously unless multivariate traffic exceeds `>= 5,000` daily store visitors):

| Experiment Wave | Variable Tested | Control | Variant(s) | Primary Metric |
|---|---|---|---|---|
| **Wave 1 (Launch + Weeks 1–2)** | **Hero Slide 1 + Icon Contrast** | Library list hero + `#0F172A` navy icon | (A) Word/PDF Split Visual Hero; (B) Bright `#2489FF` icon background | First-time installer CVR (`%`) |
| **Wave 2 (Weeks 3–4)** | **Short Description Value Framing** | Utility format list (`Offline Word, Excel, PPT...`) | (A) Anti-friction trust (`No save locks. Offline Word, PDF...`); (B) Action-led (`Edit DOCX, Compress PDF & Scan...`) | Search-to-install CVR (`%`) |
| **Wave 3 (Month 2)** | **Custom Store Listings (CSL)** | Global EN listing | (A) Korea CSL leading with `HWP/HWPX Offline`; (B) Brazil/SEA CSL leading with `Compress PDF + ID Scan + Small Download` | Regional CVR + D7 retention |

### 6.2 Google App Campaigns (GAC) & Review Rating Protection

- **Strict Geo Segmentation (`02_PROJECT_CHARTER.md` §7):** Operate separate GAC campaigns for **Tier-1 (`US/UK/DE/FR/JP/KR`)** vs. **Tier-2/3 (`BR/VN/ID/IN`)**; feed Firebase `purchase_success` + `paywall_impression` (`FirebaseTelemetry`) to graduate from `tCPI`/`tCPA` (`pdf_tool_complete`, `editor_session_end`) to `tROAS` once reaching `>= 10–20` value events/day.
- **Rating Defense (`>= 4.7★` Target):** Prompt Play In-App Review strictly after **positive value completion** (e.g., 2nd successful PDF export or saved edit with `0` crashes) — never during onboarding. Monitor Play Console reviews weekly per locale for `HWP` layout questions, `.rar` confusion, or billing restore support.
