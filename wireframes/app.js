(function () {
  "use strict";

  // === SVG ICONS (Consistent 24x24, 1.8 stroke, zero emojis) ===
  const svg = (paths, size = 20) =>
    `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths}</svg>`;

  const ICONS = {
    home: svg('<path d="M3 10.5L12 3l9 7.5"/><path d="M5 9.5V20h5v-5h4v5h5V9.5"/>'),
    recent: svg('<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>'),
    scan: svg('<path d="M4 8V5a1 1 0 0 1 1-1h3"/><path d="M16 4h3a1 1 0 0 1 1 1v3"/><path d="M20 16v3a1 1 0 0 1-1 1h-3"/><path d="M8 20H5a1 1 0 0 1-1-1v-3"/><rect x="7" y="8" width="10" height="8" rx="1"/>'),
    folder: svg('<path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>'),
    tools: svg('<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>'),
    search: svg('<circle cx="11" cy="11" r="7"/><path d="M20 20l-3.5-3.5"/>'),
    back: svg('<path d="M15 18l-6-6 6-6"/>'),
    more: svg('<circle cx="12" cy="5" r="1.5" fill="currentColor" stroke="none"/><circle cx="12" cy="12" r="1.5" fill="currentColor" stroke="none"/><circle cx="12" cy="19" r="1.5" fill="currentColor" stroke="none"/>'),
    star: (filled) =>
      `<svg width="19" height="19" viewBox="0 0 24 24" fill="${filled ? "#F59E0B" : "none"}" stroke="${filled ? "#D97706" : "currentColor"}" stroke-width="1.9" aria-hidden="true"><polygon points="12 2 15.1 8.3 22 9.3 17 14.1 18.2 21 12 17.7 5.8 21 7 14.1 2 9.3 8.9 8.3 12 2"/></svg>`,
    crown: svg('<path d="M3 18h18l-2-10-4.5 4L12 5l-2.5 7L5 8l-2 10z"/>'),
    settings: svg('<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09a1.65 1.65 0 0 0-1-1.51 1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09a1.65 1.65 0 0 0 1.51-1 1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>'),
    plus: svg('<path d="M12 5v14M5 12h14"/>'),
    close: svg('<path d="M18 6L6 18M6 6l12 12"/>'),
    sort: svg('<path d="M3 6h18M6 12h12M10 18h4"/>'),
    undo: svg('<path d="M9 14L4 9l5-5"/><path d="M4 9h10.5a5.5 5.5 0 0 1 0 11H11"/>'),
    redo: svg('<path d="M15 14l5-5-5-5"/><path d="M20 9H9.5a5.5 5.5 0 0 0 0 11H13"/>'),
    check: svg('<path d="M20 6L9 17l-5-5"/>'),
    lock: svg('<rect x="4" y="11" width="16" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/>'),
    shield: svg('<path d="M12 2l8 4v6c0 5-3.4 9-8 10-4.6-1-8-5-8-10V6l8-4z"/><path d="M9 12l2 2 4-4"/>'),
    wifi: svg('<path d="M5 12.5a11 11 0 0 1 14 0"/><path d="M8.5 16a6 6 0 0 1 7 0"/><circle cx="12" cy="19" r="1" fill="currentColor"/>'),
    cloud: svg('<path d="M6 18h11a4 4 0 0 0 .5-7.97A6 6 0 0 0 6.1 8.5 4.5 4.5 0 0 0 6 18z"/>'),
    print: svg('<path d="M6 9V3h12v6"/><rect x="4" y="9" width="16" height="8" rx="2"/><path d="M6 14h12v7H6z"/>'),
    edit: svg('<path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"/>'),
    play: svg('<polygon points="6 4 20 12 6 20 6 4"/>'),
    sliders: svg('<path d="M4 21v-7M4 10V3M12 21v-9M12 8V3M20 21v-5M20 12V3M1 14h6M9 8h6M17 16h6"/>'),
    list: svg('<path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01"/>'),
    share: svg('<circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><path d="M8.6 13.5l6.8 4M15.4 6.5l-6.8 4"/>'),
    trash: svg('<path d="M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14"/>'),
    archive: svg('<rect x="3" y="4" width="18" height="4" rx="1"/><path d="M5 8v11a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8"/><path d="M10 13h4"/>'),
    volume: svg('<polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M15.5 8.5a5 5 0 0 1 0 7"/>'),
    idCard: svg('<rect x="3" y="5" width="18" height="14" rx="2"/><circle cx="9" cy="11" r="2"/><path d="M6 15c1-1.3 5-1.3 6 0M15 10h4M15 14h3"/>'),
    qr: svg('<rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><path d="M14 14h3v3h-3zM19 14h2v5h-5v-2"/>'),
    scissors: svg('<circle cx="6" cy="6" r="3"/><circle cx="6" cy="18" r="3"/><path d="M20 4L8.1 15.9M14.5 14.5L20 20M8.1 8.1L12 12"/>'),
    merge: svg('<path d="M6 4v6a4 4 0 0 0 4 4h8"/><path d="M15 11l3 3-3 3"/><path d="M6 20v-4"/>'),
    compress: svg('<path d="M4 14h6v6M20 10h-6V4M14 10l7-7M3 21l7-7"/>'),
    stamp: svg('<path d="M5 20h14M7 16h10l-1-4H8l-1 4zM12 12V7a2.5 2.5 0 1 1 0-5 2.5 2.5 0 0 1 0 5z"/>'),
    penTool: svg('<path d="M12 19l7-7 3 3-7 7-3-3z"/><path d="M18 13l-1.5-7.5L2 2l3.5 14.5L13 18l5-5z"/>'),
    ocr: svg('<path d="M4 7V5h2M18 5h2v2M4 17v2h2M18 19h2v-2M7 10h10M7 14h6"/>'),
  };

  // === ROUTE METADATA ===
  const FLOWS = [
    {
      title: "Flow 1 · Onboarding & Consent",
      items: [
        { id: "S-01", label: "Splash & Intent Router", route: "SplashRoute", us: ["US-001", "US-002", "US-004"], transition: "Root -> Cross-fade 200ms", notes: ["Cold launch < 1.2s target", "UMP Consent check before ads", "External ACTION_VIEW skips intro & ads"] },
        { id: "S-02", label: "Language Select (10)", route: "LanguageSelectRoute", us: ["US-001"], transition: "Cross-fade -> Push", notes: ["10 target locales incl. VI, EN, KO, JA, ES, PT-BR", "Minimum 48dp touch rows", "Persisted immediately via DataStore"] },
        { id: "S-03", label: "Intro Carousel (3 Slides)", route: "OnboardingIntroRoute", us: ["US-003"], transition: "Horizontal Slide Push", notes: ["Always skippable via top-right Skip (48dp)", "Highlights 100% offline engine & zero save ransom"] },
        { id: "S-04", label: "Paywall & Reward Gate", route: "PaywallRoute + RewardGateSheet", us: ["US-110", "US-111"], transition: "Modal Slide Up 250ms", notes: ["CD-07: Skippable (X) Soft Onboarding Paywall", "Tier-1 anchor SKUs ($4.99/wk, $39.99/yr, $59.99 life)", "Pre-action RewardGateSheet (Watch 1 Video vs Pro)"] },
        { id: "S-05", label: "Storage Permission + SAF", route: "StoragePermissionRoute", us: ["US-003", "US-005"], transition: "Horizontal Slide Push", notes: ["Clear MANAGE_EXTERNAL_STORAGE rationale", "Non-blocking 'Use Folder Picker (SAF)' fallback"] },
      ],
    },
    {
      title: "Flow 2 · Main Hub & Library",
      items: [
        { id: "S-06", label: "Home Documents (MainShell)", route: "HomeDocumentsRoute", us: ["US-010", "US-013", "US-005"], transition: "Tab Cross-fade 150ms", notes: ["4 tabs + 56dp center Scan FAB (NavigationRail on Tablet)", "9 format filter chips + Sort + 3-dot Context Sheet", "Temporary 'EO' geometric monogram (CD-06)"] },
        { id: "S-07", label: "Recent · Favorites · Trash", route: "RecentBookmarksRoute", us: ["US-011"], transition: "Tab Cross-fade 150ms", notes: ["Fixes competitor path-only bug: tracks Page 3/12 & timestamp", "Soft Trash preserves originalPath + 30-day auto-purge"] },
        { id: "S-08", label: "Search Documents", route: "SearchDocumentsRoute", us: ["US-012"], transition: "Vertical Fade", notes: ["300ms debounced search across filename & parent folder", "Matched substring highlight + recent chips"] },
        { id: "S-09", label: "Templates Gallery", route: "TemplatesSheet", us: ["US-081"], transition: "Modal Bottom Sheet", notes: ["100% offline bundled Word/Excel/PPT templates", "Zero server download or fake AI templates"] },
        { id: "S-10", label: "Storage & ZIP/7z Viewer", route: "StorageBrowserRoute", us: ["US-024", "US-080"], transition: "Tab Cross-fade / Folder Push", notes: ["Local folders + Cloud quick entry + Multi-select bar", "Real Commons Compress ZIP/7z browser; honest .rar notice"] },
        { id: "S-11", label: "Cloud Drive & OneDrive", route: "CloudDriveBrowserRoute", us: ["US-100"], transition: "Horizontal Slide Push", notes: ["Google Drive REST v3 + Microsoft MSAL OneDrive", "Encrypted token alias only; zero dead Dropbox button"] },
      ],
    },
    {
      title: "Flow 3 · Reader & Rich Editors",
      items: [
        { id: "S-13", label: "Universal Reader (PDF/HWP)", route: "DocumentReaderRoute", us: ["US-020", "US-021", "US-022", "US-023"], transition: "Horizontal Push", notes: ["Interactive Light / Sepia / Night comfort themes", "Real Continuous Scroll, Reflow, Auto-Scroll, TTS Read-Aloud", "Offline hwplib/hwpxlib HWP pipeline (<=50 MB)"] },
        { id: "S-14a", label: "Word Editor + Review", route: "OfficeEditorRoute (Word)", us: ["US-030", "US-031", "US-032"], transition: "Horizontal Push", notes: ["LibreOfficeKit NDK 16KB + 7 tabs incl. REAL Review/Track Changes", "Zero SAVE_HEADER lock on Done/Save", "DraftRecoveryEntity 15s atomic checkpoint"] },
        { id: "S-14b", label: "Excel Spreadsheet Editor", route: "OfficeEditorRoute (Excel)", us: ["US-040"], transition: "Horizontal Push", notes: ["Formula bar (fx) + multi-sheet switcher + freeze panes", "5 bottom tabs: File, Edit, Insert, Formulas, Find"] },
        { id: "S-14c", label: "PowerPoint Slide Editor", route: "OfficeEditorRoute (Slide)", us: ["US-041"], transition: "Horizontal Push", notes: ["Slide thumbnail strip + Shape Fill/Stroke/Z-order + Notes", "7 tabs + direct launch into Fullscreen SlideShow"] },
        { id: "S-15", label: "Fullscreen SlideShow", route: "SlideShowRoute", us: ["US-041"], transition: "Fullscreen Cross-fade", notes: ["Immersive slide player with timer & laser pointer ink"] },
        { id: "S-16", label: "PDF Annotate · e-Sign · PKCS#7", route: "PdfEditorRoute + SignaturePadRoute", us: ["US-050", "US-051"], transition: "Horizontal Push / Modal Canvas", notes: ["Highlight, Ink, Sticky Notes, draggable Handwritten Signature", "PKCS#7 .p12/.pfx Certificate Inspector & Signer"] },
      ],
    },
    {
      title: "Flow 4 · PDF Tools & Scanner",
      items: [
        { id: "S-12", label: "Tools Hub (Zero Stubs)", route: "ToolsHubRoute", us: ["US-060", "US-061", "US-062", "US-063", "US-072"], transition: "Tab Cross-fade 150ms", notes: ["3 sections: Convert, PDF Power, Utilities", "Real Split PDF, Compress, Watermark, Print, Dual ID Card"] },
        { id: "S-17", label: "PDF Tool Config & Progress", route: "PdfToolConfigRoute + ConvertProgressRoute", us: ["US-052", "US-060", "US-061", "US-062", "US-063"], transition: "Horizontal Push -> Cross-fade", notes: ["Supports Split range, Compress presets, Watermark, Merge, AES Lock, PDF->DOCX", "Shows size delta (14.2 MB -> 3.8 MB) + Open/Share"] },
        { id: "S-18", label: "Scanner · Dual ID Card · OCR · QR", route: "ScannerFlowRoute", us: ["US-070", "US-071", "US-072"], transition: "Vertical Slide Up 250ms", notes: ["Real 2-step Dual-Side ID Card -> A4 composite", "5-script offline ML Kit OCR + QR/Barcode action card"] },
      ],
    },
    {
      title: "Flow 5 · Transfer, Settings & Widgets",
      items: [
        { id: "S-19", label: "Wi-Fi Web & FTP Transfer", route: "WifiTransferRoute", us: ["US-090"], transition: "Horizontal Slide Push", notes: ["Ktor CIO HTTP browser drag-drop + FTP on port 8998", "PIN & Read-Only toggles + 10:00 idle auto-stop"] },
        { id: "S-20", label: "Settings · UMP · Licenses", route: "SettingsRoute", us: ["US-001", "US-002", "US-111", "US-112"], transition: "Horizontal Slide Push", notes: ["Theme (System/Light/Dark), Language, Default Handler", "UMP Privacy Choices, Restore Purchases, MPLv2/Apache Licenses"] },
        { id: "W-01", label: "Home Glance Widgets (2)", route: "GlanceWidgets (4x1 & 4x2)", us: ["US-091"], transition: "Launcher Deep-Link", notes: ["Quick Bar (Search + Scan + New) & Recent List (Top 3 docs)"] },
      ],
    },
  ];

  const ALL_ROUTES = FLOWS.flatMap((f) => f.items);

  // === MOCK DATASET ===
  const DOCS = [
    { id: "d1", name: "Contract_Signed_v2.pdf", fmt: "pdf", ext: "PDF", size: "1.8 MB", date: "Today, 14:22", page: "Page 3/12", folder: "Downloads", fav: true, draft: false, locked: false },
    { id: "d2", name: "Q4_Product_Roadmap.docx", fmt: "word", ext: "DOCX", size: "420 KB", date: "Today, 11:05", page: "Page 2/8", folder: "Documents", fav: true, draft: true, locked: false },
    { id: "d3", name: "Daily_Sales_Report_2026.xlsx", fmt: "excel", ext: "XLSX", size: "690 KB", date: "Yesterday", page: "3 sheets", folder: "Finance", fav: false, draft: false, locked: false },
    { id: "d4", name: "Evora_Seed_Deck.pptx", fmt: "ppt", ext: "PPTX", size: "4.6 MB", date: "Nov 24", page: "Slide 4/14", folder: "Pitch", fav: false, draft: false, locked: false },
    { id: "d5", name: "Seoul_Vendor_Agreement.hwpx", fmt: "hwp", ext: "HWPX", size: "1.1 MB", date: "Nov 23", page: "Page 1/6", folder: "Downloads", fav: false, draft: false, locked: false },
    { id: "d6", name: "Project_Charter_Spec.odt", fmt: "odf", ext: "ODT", size: "310 KB", date: "Nov 22", page: "Page 5/9", folder: "Documents", fav: false, draft: false, locked: false },
    { id: "d7", name: "Android_Design_Guide.epub", fmt: "epub", ext: "EPUB", size: "2.4 MB", date: "Nov 20", page: "Ch. 4", folder: "Books", fav: true, draft: false, locked: false },
    { id: "d8", name: "Client_Invoices_Q4.zip", fmt: "txt", ext: "ZIP", size: "8.2 MB", date: "Nov 19", page: "6 entries", folder: "Downloads", fav: false, draft: false, locked: false },
  ];

  const TRASH_DOCS = [
    { id: "t1", name: "Old_Draft_v1.docx", fmt: "word", ext: "DOCX", size: "215 KB", expires: "Deletes in 28 days", orig: "/Documents/Work" },
    { id: "t2", name: "Scan_Receipt_09.pdf", fmt: "pdf", ext: "PDF", size: "940 KB", expires: "Deletes in 19 days", orig: "/Download/Scans" },
  ];

  // === INTERACTIVE APP STATE ===
  const appState = {
    activeScreen: "S-06",
    theme: "light",
    device: "phone",
    viewMode: "content", // content | loading | empty | error
    selectedLang: "en",
    introSlide: 0,
    selectedSku: "yearly",
    homeFilter: "all",
    homeSort: "Modified",
    selectedDocId: "d1",
    recentTab: "recent", // recent | fav | trash
    searchQuery: "Contract",
    tplFilter: "all",
    storageFolder: "Downloads",
    multiSelectMode: false,
    selectedStorageIds: new Set(["d1", "d5"]),
    cloudProvider: "gdrive",
    readerTheme: "light", // light | sepia | night
    readerContinuous: true,
    readerReflow: false,
    readerAutoScroll: false,
    readerTts: false,
    readerPage: 3,
    wordTab: "Review",
    trackChangesOn: true,
    excelTab: "Formulas",
    pptTab: "Format",
    pdfTab: "Signature",
    toolSelected: "split", // split | compress | watermark | merge | password | pdf2docx
    toolStage: "config", // config | progress | success
    compressPreset: "medium",
    scanMode: "idcard", // doc | idcard | ocr | qr
    idCardStep: 1, // 1=front, 2=back, 3=a4ready
    ocrScript: "Latin",
    wifiRunning: true,
    wifiPin: true,
    wifiReadOnly: false,
    appThemeSetting: "Light",
    toastMsg: null,
    activeModal: null, // context | reward | password | draft | reading | sigpad | pkcs7 | zip | rar | templates
  };

  function showToast(msg) {
    appState.toastMsg = msg;
    render();
    clearTimeout(showToast._t);
    showToast._t = setTimeout(() => {
      appState.toastMsg = null;
      render();
    }, 2400);
  }

  function navigate(screenId, modal = null) {
    appState.activeScreen = screenId;
    appState.activeModal = modal;
    render();
  }

  // === RENDER HELPERS ===
  function renderStatusBar(bgHex = null) {
    const bar = document.getElementById("statusBar");
    if (bgHex) {
      bar.className = "android-status-bar colored";
      bar.style.background = bgHex;
    } else {
      bar.className = "android-status-bar";
      bar.style.background = "";
    }
  }

  function renderDocCard(doc, opts = {}) {
    const selected = (opts.selectable && appState.selectedStorageIds.has(doc.id)) || (appState.device === "tablet" && appState.selectedDocId === doc.id);
    const favBtn = opts.hideFav
      ? ""
      : `<button class="icon-btn-48" data-toggle-fav="${doc.id}" aria-label="Toggle favorite for ${doc.name}">${ICONS.star(doc.fav)}</button>`;
    return `
      <div class="doc-card ${selected ? "selected" : ""}" data-open-doc="${doc.id}">
        <div class="fmt-badge fmt-${doc.fmt}">${doc.ext}</div>
        <div class="doc-info">
          <div class="doc-name">${doc.name}</div>
          <div class="doc-meta">
            <span>${doc.size}</span>
            <span>·</span>
            <span>${doc.date}</span>
            ${doc.page ? `<span class="pill-mini pill-page">${doc.page}</span>` : ""}
            ${doc.draft ? `<span class="pill-mini pill-draft">Draft auto-saved</span>` : ""}
          </div>
        </div>
        ${favBtn}
        <button class="icon-btn-48" data-open-context="${doc.id}" aria-label="More options for ${doc.name}">${ICONS.more}</button>
      </div>
    `;
  }

  function renderMainShell(activeTab, topbarHtml, bodyHtml, extraTopHtml = "") {
    const navItems = [
      { id: "home", screen: "S-06", label: "Home", icon: ICONS.home },
      { id: "recent", screen: "S-07", label: "Recent", icon: ICONS.recent },
      { id: "storage", screen: "S-10", label: "Storage", icon: ICONS.folder },
      { id: "tools", screen: "S-12", label: "Tools", icon: ICONS.tools },
    ];
    return `
      <div class="app-shell-wrap with-rail">
        <nav class="nav-rail" aria-label="Tablet Navigation Rail">
          <button class="rail-fab" data-nav="S-18" aria-label="Scan Document">${ICONS.scan}</button>
          ${navItems
            .map(
              (n) => `
            <button class="rail-item ${activeTab === n.id ? "active" : ""}" data-nav="${n.screen}">
              ${n.icon}<span>${n.label}</span>
            </button>`
            )
            .join("")}
        </nav>
        <div style="flex:1;min-width:0;display:flex;flex-direction:column;min-height:0;">
          ${topbarHtml}
          ${extraTopHtml}
          ${bodyHtml}
          <nav class="bottom-nav" aria-label="Bottom Navigation">
            ${navItems
              .slice(0, 2)
              .map(
                (n) => `
              <button class="bnav-btn ${activeTab === n.id ? "active" : ""}" data-nav="${n.screen}">
                ${n.icon}<span>${n.label}</span>
              </button>`
              )
              .join("")}
            <div class="bnav-scan-slot">
              <button class="scan-fab" data-nav="S-18" aria-label="Open Document Scanner">${ICONS.scan}</button>
              <span class="scan-fab-label">Scan</span>
            </div>
            ${navItems
              .slice(2)
              .map(
                (n) => `
              <button class="bnav-btn ${activeTab === n.id ? "active" : ""}" data-nav="${n.screen}">
                ${n.icon}<span>${n.label}</span>
              </button>`
              )
              .join("")}
          </nav>
        </div>
      </div>
    `;
  }

  function renderGlobalStateOverride(contextName) {
    if (appState.viewMode === "loading") {
      return `
        <div class="scroll-content">
          <div class="skel-box" style="height:36px;width:55%;margin-bottom:14px;"></div>
          ${[1, 2, 3, 4, 5]
            .map(
              () => `
            <div class="card-box" style="display:flex;gap:12px;align-items:center;">
              <div class="skel-box" style="width:44px;height:44px;border-radius:11px;flex-shrink:0;"></div>
              <div style="flex:1;">
                <div class="skel-box" style="height:14px;width:72%;margin-bottom:8px;"></div>
                <div class="skel-box" style="height:11px;width:44%;"></div>
              </div>
            </div>`
            )
            .join("")}
        </div>`;
    }
    if (appState.viewMode === "empty") {
      return `
        <div class="scroll-content" style="display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;padding:32px 24px;">
          <div style="width:76px;height:76px;border-radius:22px;background:var(--primary-container);color:var(--on-primary-container);display:grid;place-items:center;margin-bottom:16px;">
            ${svg('<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6M9 14h6"/>', 34)}
          </div>
          <h3 style="font-size:17px;margin-bottom:6px;">No documents in ${contextName}</h3>
          <p style="font-size:13px;color:var(--text-secondary);margin-bottom:18px;max-width:270px;">Scan a paper file, pick documents via System Folder Picker, or create a clean blank doc.</p>
          <button class="btn-primary" style="max-width:240px;margin-bottom:10px;" data-nav="S-18">${ICONS.scan} Scan Document</button>
          <button class="btn-outlined" style="max-width:240px;" data-set-state="content">Pick via SAF / Reset Demo</button>
        </div>`;
    }
    if (appState.viewMode === "error") {
      return `
        <div class="scroll-content" style="display:flex;flex-direction:column;justify-content:center;padding:24px;">
          <div class="card-box" style="border-color:var(--error);">
            <div class="banner-error" style="margin-bottom:12px;">
              <span>All-Files Storage access was declined. SAF Fallback Mode is active.</span>
            </div>
            <h3 style="font-size:15px;margin-bottom:6px;">No lockout — pick any folder safely</h3>
            <p style="font-size:13px;color:var(--text-secondary);margin-bottom:14px;">Evora Office continues working via Android Storage Access Framework without requiring MANAGE_EXTERNAL_STORAGE.</p>
            <button class="btn-primary" style="margin-bottom:8px;" data-set-state="content">Open System Folder Picker (SAF)</button>
            <button class="btn-outlined" data-nav="S-05">Review Storage Permission</button>
          </div>
        </div>`;
    }
    return null;
  }

  // === INDIVIDUAL SCREENS ===
  function renderScreen() {
    renderStatusBar(null);
    const s = appState.activeScreen;

    if (s === "S-01") {
      renderStatusBar("#186FD4");
      return `
        <div style="flex:1;background:linear-gradient(165deg,#186FD4 0%,#0D4A9E 100%);color:#fff;display:flex;flex-direction:column;justify-content:space-between;padding:42px 24px 32px;text-align:center;">
          <div style="font-size:11.5px;opacity:0.85;font-weight:600;letter-spacing:0.05em;">TEMPORARY MONOGRAM PLACEHOLDER (CD-06)</div>
          <div style="display:flex;flex-direction:column;align-items:center;">
            <div style="width:82px;height:82px;border-radius:22px;background:rgba(255,255,255,0.18);border:2px solid rgba(255,255,255,0.45);display:grid;place-items:center;font-size:32px;font-weight:800;margin-bottom:16px;">EO</div>
            <h2 style="font-size:26px;font-weight:700;margin-bottom:6px;">Evora Office</h2>
            <p style="font-size:13.5px;opacity:0.9;">DOCX · PDF · XLSX · PPTX · HWP · 100% Offline</p>
          </div>
          <div>
            <div style="height:4px;background:rgba(255,255,255,0.25);border-radius:99px;overflow:hidden;max-width:200px;margin:0 auto 14px;">
              <div style="width:68%;height:100%;background:#fff;"></div>
            </div>
            <div style="display:flex;gap:8px;justify-content:center;">
              <button class="done-cta-on-color" data-nav="S-02">First Launch Flow</button>
              <button class="done-cta-on-color" data-nav="S-13">External Intent (0ms Ad)</button>
            </div>
          </div>
        </div>`;
    }

    if (s === "S-02") {
      const langs = [
        ["en", "English", "United States / Global"],
        ["vi", "Tiếng Việt", "Việt Nam"],
        ["ko", "한국어", "대한민국 (HWP Ready)"],
        ["ja", "日本語", "日本"],
        ["es", "Español", "LATAM / España"],
        ["pt", "Português", "Brasil"],
        ["id", "Bahasa Indonesia", "Indonesia"],
        ["hi", "हिन्दी", "India"],
        ["de", "Deutsch", "Deutschland"],
        ["fr", "Français", "France"],
      ];
      return `
        <div class="app-topbar">
          <div class="topbar-left"><span class="topbar-title">Choose Language</span></div>
          <button class="done-cta-pill" data-nav="S-03">Continue</button>
        </div>
        <div class="scroll-content">
          <p style="font-size:13px;color:var(--text-secondary);margin-bottom:12px;">Select your preferred interface language. Change anytime in Settings.</p>
          ${langs
            .map(
              ([code, label, sub]) => `
            <div class="doc-card ${appState.selectedLang === code ? "selected" : ""}" style="min-height:60px;margin-bottom:8px;padding:10px 14px;" data-select-lang="${code}">
              <div class="fmt-badge fmt-word" style="width:36px;height:36px;font-size:11px;">${code.toUpperCase()}</div>
              <div class="doc-info">
                <div class="doc-name">${label}</div>
                <div class="doc-meta">${sub}</div>
              </div>
              <span style="color:var(--primary-action);">${appState.selectedLang === code ? ICONS.check : ""}</span>
            </div>`
            )
            .join("")}
        </div>`;
    }

    if (s === "S-03") {
      const slides = [
        { title: "Edit Word, Excel & PPT Offline", desc: "Powered by LibreOfficeKit 16KB engine. Save your documents anytime with zero paywall ransom traps.", badge: "100% Offline Engine", color: "var(--fmt-word-tint)", ink: "var(--fmt-word-ink)" },
        { title: "Real PDF Power Tools", desc: "Split pages, compress attachments, apply custom watermarks, sign with PKCS#7, and convert PDF to DOCX locally.", badge: "Zero Fake Stubs", color: "var(--fmt-pdf-tint)", ink: "var(--fmt-pdf-ink)" },
        { title: "Dual-Side ID Scan & Wi-Fi Drop", desc: "Scan Front & Back IDs onto one printable A4 page, extract 5-script OCR text, and drag-drop files over LAN Wi-Fi.", badge: "Zero Server Uploads", color: "var(--fmt-excel-tint)", ink: "var(--fmt-excel-ink)" },
      ];
      const sl = slides[appState.introSlide];
      return `
        <div style="flex:1;display:flex;flex-direction:column;justify-content:space-between;padding:16px 20px 24px;background:var(--surface-card);">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <span class="pill-mini pill-page">Slide ${appState.introSlide + 1} / 3</span>
            <button class="btn-text" data-nav="S-04">Skip</button>
          </div>
          <div style="text-align:center;padding:10px 6px;">
            <div style="width:156px;height:156px;margin:0 auto 22px;border-radius:32px;background:${sl.color};color:${sl.ink};display:grid;place-items:center;">
              ${appState.introSlide === 0 ? svg('<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6M8 13h8M8 17h5"/>', 64) : appState.introSlide === 1 ? svg('<path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"/>', 64) : svg('<rect x="3" y="5" width="18" height="14" rx="2"/><circle cx="9" cy="11" r="2"/><path d="M15 10h4M15 14h3"/>', 64)}
            </div>
            <span class="pill-mini pill-success" style="margin-bottom:10px;">${sl.badge}</span>
            <h2 style="font-size:22px;line-height:1.25;margin-bottom:10px;">${sl.title}</h2>
            <p style="font-size:14px;color:var(--text-secondary);line-height:1.5;">${sl.desc}</p>
          </div>
          <div>
            <div style="display:flex;justify-content:center;gap:8px;margin-bottom:16px;">
              ${[0, 1, 2].map((i) => `<button style="width:${i === appState.introSlide ? 24 : 8}px;height:8px;border-radius:99px;border:none;cursor:pointer;background:${i === appState.introSlide ? "var(--brand-accent)" : "var(--border-subtle)"};" data-intro-dot="${i}" aria-label="Slide ${i + 1}"></button>`).join("")}
            </div>
            <button class="btn-primary" data-next-intro="1">${appState.introSlide < 2 ? "Next" : "Continue"}</button>
          </div>
        </div>`;
    }

    if (s === "S-04") {
      const skus = [
        { id: "weekly", title: "Weekly Pro", price: "$4.99 / week", sub: "Includes 3-day free trial", badge: "" },
        { id: "yearly", title: "Annual Pro", price: "$39.99 / year", sub: "7-day free trial · $3.33/mo", badge: "BEST VALUE" },
        { id: "lifetime", title: "Lifetime Pro", price: "$59.99 once", sub: "One-time unlock · Forever ad-free", badge: "" },
      ];
      return `
        <div class="scroll-content" style="display:flex;flex-direction:column;justify-content:space-between;background:var(--bg-canvas);">
          <div>
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;">
              <button class="icon-btn-48" data-nav="S-05" aria-label="Skip paywall">${ICONS.close}</button>
              <span class="pill-mini pill-pro">${ICONS.crown} EVORA PRO</span>
              <button class="btn-text" data-action="toast:Restored previous purchases">Restore</button>
            </div>
            <div style="text-align:center;margin-bottom:14px;">
              <div class="eo-mark-mini" style="width:54px;height:54px;margin:0 auto 10px;border-radius:16px;font-size:20px;">EO</div>
              <h2 style="font-size:21px;margin-bottom:4px;">Unlock Full PDF & Pro Suite</h2>
              <p style="font-size:13px;color:var(--text-secondary);">Viewing, editing & saving your docs is always 100% free.</p>
            </div>
            <div class="card-box" style="padding:12px;">
              ${["Remove all future ads globally", "Batch Merge > 3 PDFs & High Compress", "Dual-Side ID Card, Batch OCR & Watermark", "Unlimited Offline PDF -> DOCX & PKCS#7 Sign"]
                .map((b) => `<div style="display:flex;align-items:center;gap:8px;font-size:12.5px;margin-bottom:6px;"><span style="color:var(--success);">${ICONS.check}</span><span>${b}</span></div>`)
                .join("")}
            </div>
            ${skus
              .map(
                (k) => `
              <div class="sku-card ${appState.selectedSku === k.id ? "selected" : ""}" data-select-sku="${k.id}">
                <div>
                  <div style="display:flex;align-items:center;gap:6px;">
                    <strong style="font-size:14.5px;">${k.title}</strong>
                    ${k.badge ? `<span class="pill-mini pill-pro">${k.badge}</span>` : ""}
                  </div>
                  <div style="font-size:12px;color:var(--text-secondary);">${k.sub}</div>
                </div>
                <strong style="font-size:15px;color:var(--primary-action);">${k.price}</strong>
              </div>`
              )
              .join("")}
          </div>
          <div>
            <button class="btn-primary" style="margin-bottom:8px;" data-nav="S-05">Start Free Trial</button>
            <div style="display:flex;justify-content:space-between;align-items:center;">
              <button class="btn-text" style="font-size:12px;" data-open-modal="reward">Preview RewardGateSheet</button>
              <button class="btn-text" style="font-size:12px;" data-nav="S-05">Continue Free</button>
            </div>
          </div>
        </div>`;
    }

    if (s === "S-05") {
      return `
        <div style="flex:1;display:flex;flex-direction:column;justify-content:space-between;padding:24px 20px;background:var(--surface-card);">
          <div style="text-align:center;margin-top:24px;">
            <div style="width:82px;height:82px;margin:0 auto 18px;border-radius:24px;background:var(--primary-container);color:var(--on-primary-container);display:grid;place-items:center;">
              ${svg('<path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><path d="M9 13l2 2 4-4"/>', 40)}
            </div>
            <h2 style="font-size:21px;margin-bottom:8px;">Manage Your Local Documents</h2>
            <p style="font-size:13.5px;color:var(--text-secondary);line-height:1.5;margin-bottom:16px;">Grant All-Files Access to automatically index PDF, Word, Excel, PPT, HWP, and ZIP/7z files across device folders.</p>
            <div class="banner-info banner-primary" style="text-align:left;">
              <span><strong>100% Offline Promise:</strong> Files never leave your phone. Zero server uploads.</span>
            </div>
          </div>
          <div>
            <button class="btn-primary" style="margin-bottom:10px;" data-nav="S-06">Allow All-Files Access</button>
            <button class="btn-outlined" data-nav="S-06">Use Folder Picker (SAF) Instead</button>
          </div>
        </div>`;
    }

    if (s === "S-06") {
      const topbar = `
        <div class="app-topbar">
          <div class="topbar-left">
            <div class="eo-mark-mini" aria-hidden="true">EO</div>
            <span class="topbar-title">Evora Office</span>
          </div>
          <div class="topbar-actions">
            <button class="icon-btn-48" data-nav="S-08" aria-label="Search documents">${ICONS.search}</button>
            <button class="icon-btn-48" data-nav="S-09" aria-label="New from templates">${ICONS.plus}</button>
            <button class="icon-btn-48" style="color:var(--on-pro-gold);" data-nav="S-04" aria-label="Evora Pro">${ICONS.crown}</button>
          </div>
        </div>`;
      const chips = [
        ["all", "All"], ["pdf", "PDF"], ["word", "Word"], ["excel", "Excel"], ["ppt", "PPT"], ["hwp", "HWP"], ["odf", "ODF"], ["epub", "EPUB"], ["txt", "ZIP/TXT"],
      ];
      const chipStrip = `
        <div class="chip-row">
          ${chips.map(([k, lbl]) => `<button class="filter-chip ${appState.homeFilter === k ? "active" : ""}" data-home-filter="${k}">${lbl}</button>`).join("")}
        </div>`;

      const stateOverride = renderGlobalStateOverride("All Documents");
      if (stateOverride) return renderMainShell("home", topbar, stateOverride, chipStrip);

      const list = appState.homeFilter === "all" ? DOCS : DOCS.filter((d) => d.fmt === appState.homeFilter);
      const selectedDoc = DOCS.find((d) => d.id === appState.selectedDocId) || DOCS[0];
      const body = `
        <div class="tablet-two-pane">
          <div class="scroll-content">
            <div class="banner-info banner-primary">
              <span>Set Evora Office as default PDF & DOCX viewer</span>
              <button class="btn-text" style="min-height:34px;padding:4px 8px;" data-action="toast:Opened Android Default App Settings">Set Default</button>
            </div>
            <div class="list-meta-bar">
              <button class="sort-pill" data-cycle-sort="1">${ICONS.sort} Sort: ${appState.homeSort}</button>
              <span class="count-label">${list.length} files</span>
            </div>
            <div class="doc-list">
              ${list.map((d) => renderDocCard(d)).join("")}
            </div>
            <div style="margin-top:12px;padding:10px;border:1px dashed var(--border-subtle);border-radius:10px;text-align:center;font-size:11.5px;color:var(--text-muted);">
              CD-07 Policy: AdMob placements deferred until features Done (zero inline ads shown here)
            </div>
          </div>
          <div class="tablet-preview-pane">
            <div style="display:flex;align-items:center;gap:10px;">
              <div class="fmt-badge fmt-${selectedDoc.fmt}">${selectedDoc.ext}</div>
              <div>
                <div style="font-weight:700;font-size:15px;">${selectedDoc.name}</div>
                <div style="font-size:12px;color:var(--text-muted);">${selectedDoc.size} · ${selectedDoc.folder} · ${selectedDoc.page}</div>
              </div>
            </div>
            <div class="doc-page-sheet" style="margin:0;">
              <strong>Tablet Master-Detail Preview</strong>
              <p style="margin-top:6px;font-size:12.5px;color:var(--text-secondary);">Adaptive <code>Expanded</code> WindowSizeClass renders live metadata & instant Open/Edit triggers side-by-side.</p>
            </div>
            <button class="btn-primary" data-open-doc-force="${selectedDoc.id}">Open in Reader / Editor</button>
            <button class="btn-outlined" data-open-context="${selectedDoc.id}">File Actions & Convert</button>
          </div>
        </div>`;
      return renderMainShell("home", topbar, body, chipStrip);
    }

    if (s === "S-07") {
      const topbar = `
        <div class="app-topbar">
          <div class="topbar-left"><span class="topbar-title">Recent & Trash</span></div>
          ${appState.recentTab === "trash" ? `<button class="btn-text" style="color:var(--error);" data-action="toast:Emptied Trash cleanly">Empty Trash</button>` : ""}
        </div>`;
      const stateOverride = renderGlobalStateOverride("Recent / Trash");
      if (stateOverride) return renderMainShell("recent", topbar, stateOverride);

      let listHtml = "";
      if (appState.recentTab === "recent") {
        listHtml = `<div class="doc-list grid-on-tablet">${DOCS.slice(0, 5).map((d) => renderDocCard(d)).join("")}</div>`;
      } else if (appState.recentTab === "fav") {
        listHtml = `<div class="doc-list grid-on-tablet">${DOCS.filter((d) => d.fav).map((d) => renderDocCard(d)).join("")}</div>`;
      } else {
        listHtml = `
          <div class="banner-info banner-warn"><span>Deleted files stay recoverable for 30 days before auto-purge.</span></div>
          <div class="doc-list">
            ${TRASH_DOCS.map(
              (t) => `
              <div class="doc-card" style="justify-content:space-between;">
                <div class="fmt-badge fmt-${t.fmt}">${t.ext}</div>
                <div class="doc-info">
                  <div class="doc-name">${t.name}</div>
                  <div class="doc-meta">${t.size} · ${t.expires}</div>
                </div>
                <button class="sort-pill" data-action="toast:Restored ${t.name} to ${t.orig}">Restore</button>
              </div>`
            ).join("")}
          </div>`;
      }

      const body = `
        <div class="scroll-content">
          <div class="segmented">
            <button class="seg-btn ${appState.recentTab === "recent" ? "active" : ""}" data-recent-tab="recent">Recent</button>
            <button class="seg-btn ${appState.recentTab === "fav" ? "active" : ""}" data-recent-tab="fav">Favorites</button>
            <button class="seg-btn ${appState.recentTab === "trash" ? "active" : ""}" data-recent-tab="trash">Trash (${TRASH_DOCS.length})</button>
          </div>
          ${listHtml}
        </div>`;
      return renderMainShell("recent", topbar, body);
    }

    if (s === "S-08") {
      const q = appState.searchQuery.trim().toLowerCase();
      const matches = q ? DOCS.filter((d) => d.name.toLowerCase().includes(q) || d.folder.toLowerCase().includes(q)) : DOCS;
      return `
        <div class="app-topbar" style="gap:8px;">
          <button class="icon-btn-48" data-nav="S-06" aria-label="Back">${ICONS.back}</button>
          <input class="input-field" style="min-height:40px;flex:1;" id="searchInputField" value="${appState.searchQuery}" placeholder="Search filename or folder..." aria-label="Search documents" />
          <button class="icon-btn-48" data-clear-search="1" aria-label="Clear search">${ICONS.close}</button>
        </div>
        <div class="scroll-content">
          <div style="display:flex;gap:6px;flex-wrap:wrap;margin-bottom:12px;">
            ${["Contract", "Report", "HWPX", "Roadmap"].map((term) => `<button class="filter-chip" style="min-height:32px;font-size:12px;" data-quick-search="${term}">${term}</button>`).join("")}
          </div>
          <div class="list-meta-bar">
            <span class="count-label">${matches.length} matching results (300ms debounce)</span>
          </div>
          <div class="doc-list grid-on-tablet">
            ${matches.length ? matches.map((d) => renderDocCard(d)).join("") : `<div class="card-box" style="text-align:center;">No files matching "${appState.searchQuery}"</div>`}
          </div>
        </div>`;
    }

    if (s === "S-09") {
      const tpls = [
        { name: "Blank Word", type: "word", desc: "Clean A4 .docx", fmt: "DOCX" },
        { name: "Resume / CV", type: "word", desc: "Modern 1-page layout", fmt: "DOCX" },
        { name: "Meeting Notes", type: "word", desc: "Agenda + action table", fmt: "DOCX" },
        { name: "Blank Sheet", type: "excel", desc: "Empty .xlsx workbook", fmt: "XLSX" },
        { name: "Monthly Budget", type: "excel", desc: "Prebuilt SUM formulas", fmt: "XLSX" },
        { name: "Invoice Sheet", type: "excel", desc: "Tax + total formula", fmt: "XLSX" },
        { name: "Blank Slide", type: "ppt", desc: "16:9 widescreen deck", fmt: "PPTX" },
        { name: "Pitch Deck", type: "ppt", desc: "8 clean business slides", fmt: "PPTX" },
      ];
      const list = appState.tplFilter === "all" ? tpls : tpls.filter((t) => t.type === appState.tplFilter);
      return `
        <div class="app-topbar">
          <div class="topbar-left">
            <button class="icon-btn-48" data-nav="S-06" aria-label="Back">${ICONS.back}</button>
            <span class="topbar-title">New from Offline Templates</span>
          </div>
        </div>
        <div class="chip-row">
          ${[["all", "All"], ["word", "Word"], ["excel", "Excel"], ["ppt", "Slides"]].map(([k, l]) => `<button class="filter-chip ${appState.tplFilter === k ? "active" : ""}" data-tpl-filter="${k}">${l}</button>`).join("")}
        </div>
        <div class="scroll-content">
          <div style="display:grid;grid-template-columns:repeat(2,1fr);gap:10px;">
            ${list
              .map(
                (t) => `
              <div class="card-box" style="margin:0;cursor:pointer;" data-nav="${t.type === "excel" ? "S-14b" : t.type === "ppt" ? "S-14c" : "S-14a"}">
                <div style="height:68px;border-radius:10px;background:var(--fmt-${t.type}-tint);color:var(--fmt-${t.type}-ink);display:grid;place-items:center;font-weight:700;font-size:15px;margin-bottom:8px;">${t.fmt}</div>
                <div style="font-size:13.5px;font-weight:600;">${t.name}</div>
                <div style="font-size:11.5px;color:var(--text-muted);">${t.desc}</div>
              </div>`
              )
              .join("")}
          </div>
        </div>`;
    }

    if (s === "S-10") {
      const topbar = `
        <div class="app-topbar">
          <div class="topbar-left"><span class="topbar-title">Storage & Archives</span></div>
          <div class="topbar-actions">
            <button class="icon-btn-48" data-toggle-multiselect="1" aria-label="Batch select">${ICONS.check}</button>
            <button class="icon-btn-48" data-action="toast:Created new folder in /Download" aria-label="New folder">${ICONS.plus}</button>
          </div>
        </div>`;
      const stateOverride = renderGlobalStateOverride("Storage Explorer");
      if (stateOverride) return renderMainShell("storage", topbar, stateOverride);

      const body = `
        <div class="scroll-content">
          <div class="section-hdr" style="margin-top:0;"><span>Cloud Storage</span></div>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-bottom:14px;">
            <div class="card-box" style="margin:0;cursor:pointer;display:flex;align-items:center;gap:10px;" data-open-cloud="gdrive">
              <div class="fmt-badge fmt-word" style="width:38px;height:38px;">${ICONS.cloud}</div>
              <div><div style="font-size:13px;font-weight:600;">Google Drive</div><div style="font-size:11px;color:var(--success);">Connected</div></div>
            </div>
            <div class="card-box" style="margin:0;cursor:pointer;display:flex;align-items:center;gap:10px;" data-open-cloud="onedrive">
              <div class="fmt-badge fmt-hwp" style="width:38px;height:38px;">${ICONS.cloud}</div>
              <div><div style="font-size:13px;font-weight:600;">OneDrive</div><div style="font-size:11px;color:var(--text-muted);">MSAL Ready</div></div>
            </div>
          </div>

          <div class="section-hdr"><span>Device Volumes · /${appState.storageFolder}</span></div>
          <div style="display:flex;gap:8px;margin-bottom:10px;">
            ${["Internal", "Downloads", "SD Card"].map((vol) => `<button class="filter-chip ${appState.storageFolder === vol ? "active" : ""}" style="min-height:34px;" data-storage-vol="${vol}">${vol}</button>`).join("")}
          </div>

          <div class="doc-list">
            <div class="doc-card" style="cursor:pointer;" data-open-modal="zip">
              <div class="fmt-badge fmt-txt">7z</div>
              <div class="doc-info">
                <div class="doc-name">Client_Invoices_Q4.zip / .7z</div>
                <div class="doc-meta">8.2 MB · Tap to inspect & extract (Commons Compress)</div>
              </div>
            </div>
            <div class="doc-card" style="cursor:pointer;" data-open-modal="rar">
              <div class="fmt-badge" style="background:var(--surface-subtle);color:var(--text-secondary);">RAR</div>
              <div class="doc-info">
                <div class="doc-name">Legacy_Archive.rar</div>
                <div class="doc-meta">Unsupported format test · Explains ZIP/7z clearly</div>
              </div>
            </div>
            ${DOCS.slice(0, 3).map((d) => renderDocCard(d, { selectable: appState.multiSelectMode })).join("")}
          </div>

          ${appState.multiSelectMode ? `
            <div class="card-box" style="margin-top:12px;background:var(--primary-container);display:flex;justify-content:space-around;">
              <button class="btn-text" data-action="toast:Moved 2 files">Move</button>
              <button class="btn-text" data-action="toast:Copied 2 files">Copy</button>
              <button class="btn-text" data-nav="S-17">Merge PDF</button>
              <button class="btn-text" style="color:var(--error);" data-action="toast:Moved 2 items to Trash">Delete</button>
            </div>` : ""}
        </div>`;
      return renderMainShell("storage", topbar, body);
    }

    if (s === "S-11") {
      const isDrive = appState.cloudProvider === "gdrive";
      return `
        <div class="app-topbar">
          <div class="topbar-left">
            <button class="icon-btn-48" data-nav="S-10" aria-label="Back">${ICONS.back}</button>
            <span class="topbar-title">${isDrive ? "Google Drive" : "Microsoft OneDrive"}</span>
          </div>
          <button class="sort-pill" data-toggle-cloud="1">Switch Account</button>
        </div>
        <div class="scroll-content">
          <div class="banner-info banner-success">
            <span>Signed in as <strong>ops@evora-demo.com</strong> · Encrypted Keystore Token</span>
            <button class="btn-text" style="min-height:32px;padding:2px 6px;" data-action="toast:Disconnected account cleanly">Sign Out</button>
          </div>
          <div class="doc-list">
            ${DOCS.slice(0, 4)
              .map(
                (d) => `
              <div class="doc-card">
                <div class="fmt-badge fmt-${d.fmt}">${d.ext}</div>
                <div class="doc-info">
                  <div class="doc-name">${d.name}</div>
                  <div class="doc-meta">${d.size} · Synced from ${isDrive ? "Drive v3" : "Graph API"}</div>
                </div>
                <button class="sort-pill" data-action="toast:Downloaded ${d.name} for offline viewing">Open</button>
              </div>`
              )
              .join("")}
          </div>
        </div>`;
    }

    if (s === "S-12") {
      const topbar = `
        <div class="app-topbar">
          <div class="topbar-left"><span class="topbar-title">Tools Hub</span></div>
          <button class="icon-btn-48" data-nav="S-20" aria-label="Settings">${ICONS.settings}</button>
        </div>`;
      const tile = (label, icon, tint, ink, target, toolKey = null, pro = false) => `
        <button class="tool-tile" ${toolKey ? `data-open-tool="${toolKey}"` : `data-nav="${target}"`}>
          ${pro ? `<span class="crown-badge">PRO</span>` : ""}
          <div class="tool-tile-icon" style="background:${tint};color:${ink};">${icon}</div>
          <span class="tool-tile-label">${label}</span>
        </button>`;
      const body = `
        <div class="scroll-content">
          <div class="section-hdr" style="margin-top:0;"><span>Convert (100% Offline)</span></div>
          <div class="tools-grid">
            ${tile("Image to PDF", ICONS.plus, "var(--fmt-pdf-tint)", "var(--fmt-pdf-ink)", "S-17", "img2pdf")}
            ${tile("Docs to PDF", ICONS.edit, "var(--fmt-word-tint)", "var(--fmt-word-ink)", "S-17", "doc2pdf")}
            ${tile("PDF to DOCX", ICONS.merge, "var(--fmt-hwp-tint)", "var(--fmt-hwp-ink)", "S-17", "pdf2docx", true)}
            ${tile("Text to PDF", ICONS.list, "var(--fmt-txt-tint)", "var(--fmt-txt-ink)", "S-17", "txt2pdf")}
            ${tile("PDF to Image", ICONS.scan, "var(--fmt-ppt-tint)", "var(--fmt-ppt-ink)", "S-17", "pdf2img")}
          </div>

          <div class="section-hdr"><span>PDF Power Tools</span></div>
          <div class="tools-grid">
            ${tile("Merge PDF", ICONS.merge, "var(--fmt-pdf-tint)", "var(--fmt-pdf-ink)", "S-17", "merge")}
            ${tile("Split PDF", ICONS.scissors, "var(--fmt-ppt-tint)", "var(--fmt-ppt-ink)", "S-17", "split")}
            ${tile("Compress PDF", ICONS.compress, "var(--fmt-excel-tint)", "var(--fmt-excel-ink)", "S-17", "compress", true)}
            ${tile("Watermark", ICONS.stamp, "var(--fmt-odf-tint)", "var(--fmt-odf-ink)", "S-17", "watermark", true)}
            ${tile("AES Password", ICONS.lock, "var(--fmt-word-tint)", "var(--fmt-word-ink)", "S-17", "password")}
            ${tile("Sign PDF", ICONS.penTool, "var(--fmt-hwp-tint)", "var(--fmt-hwp-ink)", "S-16")}
            ${tile("Print PDF", ICONS.print, "var(--fmt-epub-tint)", "var(--fmt-epub-ink)", "S-12", "print")}
          </div>

          <div class="section-hdr"><span>Utilities</span></div>
          <div class="tools-grid">
            ${tile("OCR Extract", ICONS.ocr, "var(--fmt-hwp-tint)", "var(--fmt-hwp-ink)", "S-18", "scan_ocr")}
            ${tile("QR / Barcode", ICONS.qr, "var(--fmt-excel-tint)", "var(--fmt-excel-ink)", "S-18", "scan_qr")}
            ${tile("Wi-Fi Drop", ICONS.wifi, "var(--fmt-word-tint)", "var(--fmt-word-ink)", "S-19")}
            ${tile("Templates", ICONS.plus, "var(--fmt-ppt-tint)", "var(--fmt-ppt-ink)", "S-09")}
          </div>
        </div>`;
      return renderMainShell("tools", topbar, body);
    }

    if (s === "S-13") {
      renderStatusBar("var(--fmt-pdf-hdr)");
      return `
        <div class="app-topbar format-hdr" style="background:var(--fmt-pdf-hdr);">
          <div class="topbar-left">
            <button class="icon-btn-48" data-nav="S-06" aria-label="Back">${ICONS.back}</button>
            <span class="topbar-title">Contract_Signed_v2.pdf</span>
          </div>
          <div class="topbar-actions">
            <button class="icon-btn-48" data-open-modal="password" aria-label="Unlock modal demo">${ICONS.lock}</button>
            <button class="icon-btn-48" data-open-modal="reading" aria-label="Reading comfort settings">${ICONS.sliders}</button>
            <button class="done-cta-on-color" data-nav="S-16">Annotate</button>
          </div>
        </div>
        <div class="reader-canvas theme-${appState.readerTheme}">
          <div class="page-pill-float" role="status">${appState.readerPage} / 12</div>
          ${appState.readerTts ? `
            <div class="banner-info banner-primary" style="margin-bottom:10px;">
              <span>${ICONS.volume} TTS Reading Page ${appState.readerPage} (1.0x)</span>
              <button class="btn-text" style="min-height:30px;padding:2px 8px;" data-toggle-tts="1">Stop</button>
            </div>` : ""}
          <div class="doc-page-sheet">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;">
              <strong style="font-size:14.5px;">MASTER SERVICES AGREEMENT</strong>
              <span class="pill-mini pill-page">${appState.readerContinuous ? "Continuous" : "Single Page"} · ${appState.readerReflow ? "Reflow" : "Page Layout"}</span>
            </div>
            <p style="margin-bottom:10px;">1. <strong>Offline Processing Guarantee:</strong> All rendering runs locally on-device using PdfiumAndroid (Apache 2.0) and hwplib/hwpxlib for Korean Hangul documents.</p>
            <p style="margin-bottom:10px;">2. <strong>Comfort Mode:</strong> Theme is currently set to <strong>${appState.readerTheme.toUpperCase()}</strong>. Contrast verified >= 11.08:1 (Sepia) and >= 15.23:1 (Night).</p>
            <p>3. <strong>Auto-Scroll & TTS:</strong> Active status — Auto-Scroll: <strong>${appState.readerAutoScroll ? "Playing (Speed 2)" : "Off"}</strong>. Reading position auto-restores via DocumentMetaEntity.</p>
          </div>
          <div class="doc-page-sheet">
            <strong>Page ${appState.readerPage + 1} · Schedule A (Deliverables)</strong>
            <p style="margin-top:6px;">Drag the right-edge pill or tap Reading Settings to switch Continuous Vertical Scroll vs Single Page Snap.</p>
          </div>
        </div>
        <div class="editor-bottom-shelf" style="padding:8px 12px;display:flex;align-items:center;justify-content:space-between;gap:6px;">
          <button class="ed-action-chip ${appState.readerTheme === "light" ? "on" : ""}" data-rtheme="light">Light</button>
          <button class="ed-action-chip ${appState.readerTheme === "sepia" ? "on" : ""}" data-rtheme="sepia">Sepia</button>
          <button class="ed-action-chip ${appState.readerTheme === "night" ? "on" : ""}" data-rtheme="night">Night</button>
          <button class="ed-action-chip ${appState.readerTts ? "on" : ""}" data-toggle-tts="1">${ICONS.volume} TTS</button>
          <button class="ed-action-chip" data-open-modal="reading">More</button>
        </div>`;
    }

    if (s === "S-14a") {
      renderStatusBar("var(--fmt-word-hdr)");
      const tabs = ["Edit", "File", "Insert", "Review", "Page", "Draw", "Find"];
      let panelContent = "";
      if (appState.wordTab === "Review") {
        panelContent = `
          <button class="ed-action-chip ${appState.trackChangesOn ? "on" : ""}" data-toggle-track="1">${ICONS.check} Track Changes: ${appState.trackChangesOn ? "ON" : "OFF"}</button>
          <button class="ed-action-chip" data-action="toast:Accepted revision #1">Accept</button>
          <button class="ed-action-chip" data-action="toast:Rejected revision #1">Reject</button>
          <button class="ed-action-chip" data-action="toast:Added inline DOCX comment">+ Comment</button>`;
      } else {
        panelContent = `
          <button class="ed-action-chip on"><strong>B</strong></button>
          <button class="ed-action-chip"><em>I</em></button>
          <button class="ed-action-chip"><u>U</u></button>
          <button class="ed-action-chip">Poppins 14</button>
          <button class="ed-action-chip" data-action="toast:Inserted image from Gallery">Image</button>
          <button class="ed-action-chip" data-open-modal="draft">Test Draft Crash</button>`;
      }
      return `
        <div class="app-topbar format-hdr" style="background:var(--fmt-word-hdr);">
          <div class="topbar-left">
            <button class="icon-btn-48" data-nav="S-06" aria-label="Back">${ICONS.back}</button>
            <span class="topbar-title">Q4_Product_Roadmap.docx</span>
          </div>
          <div class="topbar-actions">
            <button class="icon-btn-48" data-action="toast:Undo step" aria-label="Undo">${ICONS.undo}</button>
            <button class="icon-btn-48" data-action="toast:Redo step" aria-label="Redo">${ICONS.redo}</button>
            <button class="done-cta-on-color" data-action="toast:Saved directly to DOCX (Zero SAVE_HEADER paywall!)">Done</button>
          </div>
        </div>
        <div style="background:var(--success-container);color:var(--on-success-container);padding:5px 14px;font-size:11.5px;font-weight:600;display:flex;justify-content:space-between;">
          <span>LibreOfficeKit 16KB · Draft checkpointed 4s ago</span>
          <span>Zero onResume Ads</span>
        </div>
        <div class="reader-canvas">
          <div class="doc-page-sheet">
            <h3 style="font-size:17px;margin-bottom:8px;">Q4 Product Architecture & Delivery</h3>
            <p style="margin-bottom:8px;">E05 ships native Kotlin + Jetpack Compose MVI parity across Word, Excel, PowerPoint, and PDF without deceptive save locks.</p>
            ${appState.trackChangesOn ? `<p style="background:var(--fmt-word-tint);color:var(--fmt-word-ink);padding:6px 8px;border-radius:6px;border-left:3px solid var(--brand-accent);font-size:12px;"><strong>Tracked Revision (Review Tab):</strong> <ins style="text-decoration:underline;font-weight:600;">Added offline HWPX bridge & Track Changes UI</ins> — beating competitor hidden stub.</p>` : ""}
          </div>
        </div>
        <div class="editor-bottom-shelf">
          <div class="editor-tab-strip">
            ${tabs.map((t) => `<button class="ed-tab ${appState.wordTab === t ? "active" : ""}" data-word-tab="${t}">${t}</button>`).join("")}
          </div>
          <div class="editor-tool-grid">${panelContent}</div>
        </div>`;
    }

    if (s === "S-14b") {
      renderStatusBar("var(--fmt-excel-hdr)");
      const tabs = ["File", "Edit", "Insert", "Formulas", "Find"];
      return `
        <div class="app-topbar format-hdr" style="background:var(--fmt-excel-hdr);">
          <div class="topbar-left">
            <button class="icon-btn-48" data-nav="S-06" aria-label="Back">${ICONS.back}</button>
            <span class="topbar-title">Daily_Sales_Report_2026.xlsx</span>
          </div>
          <button class="done-cta-on-color" data-action="toast:Workbook saved cleanly">Done</button>
        </div>
        <div class="excel-formula-bar">
          <span class="pill-mini pill-success">C6</span>
          <span style="color:var(--text-muted);font-weight:700;">fx</span>
          <span style="flex:1;color:var(--text-primary);">=SUM(C2:C5) * 1.15</span>
        </div>
        <div class="scroll-content no-pad" style="background:var(--surface-card);">
          <table class="excel-grid-table">
            <thead><tr><th></th><th>A · Region</th><th>B · Units</th><th>C · Net USD</th></tr></thead>
            <tbody>
              <tr><th>1</th><td>North America</td><td>1,240</td><td>$14,880</td></tr>
              <tr><th>2</th><td>Europe (EEA)</td><td>980</td><td>$11,270</td></tr>
              <tr><th>3</th><td>Brazil</td><td>3,410</td><td>$5,115</td></tr>
              <tr><th>4</th><td>SEA Cluster</td><td>4,890</td><td>$4,620</td></tr>
              <tr><th>5</th><td><strong>Total</strong></td><td><strong>10,520</strong></td><td class="excel-cell-active">$41,268</td></tr>
            </tbody>
          </table>
          <div style="padding:10px 12px;display:flex;gap:6px;border-top:1px solid var(--border-subtle);">
            <button class="filter-chip active" style="min-height:32px;">Q4 Summary</button>
            <button class="filter-chip" style="min-height:32px;">Raw Feed</button>
            <button class="filter-chip" style="min-height:32px;" data-action="toast:Added Sheet 3">+ Sheet</button>
          </div>
        </div>
        <div class="editor-bottom-shelf">
          <div class="editor-tab-strip">
            ${tabs.map((t) => `<button class="ed-tab ${appState.excelTab === t ? "active" : ""}" data-excel-tab="${t}">${t}</button>`).join("")}
          </div>
          <div class="editor-tool-grid">
            ${["SUM", "AVERAGE", "COUNT", "Freeze Top Row", "Currency $", "Merge"].map((a) => `<button class="ed-action-chip" data-action="toast:Applied ${a}">${a}</button>`).join("")}
          </div>
        </div>`;
    }

    if (s === "S-14c") {
      renderStatusBar("var(--fmt-ppt-hdr)");
      const tabs = ["Edit", "File", "Insert", "Format", "Annotate", "Page", "Find"];
      return `
        <div class="app-topbar format-hdr" style="background:var(--fmt-ppt-hdr);">
          <div class="topbar-left">
            <button class="icon-btn-48" data-nav="S-06" aria-label="Back">${ICONS.back}</button>
            <span class="topbar-title">Evora_Seed_Deck.pptx</span>
          </div>
          <div class="topbar-actions">
            <button class="icon-btn-48" data-nav="S-15" aria-label="Play Slideshow">${ICONS.play}</button>
            <button class="done-cta-on-color" data-action="toast:Deck saved cleanly">Done</button>
          </div>
        </div>
        <div class="reader-canvas" style="display:flex;flex-direction:column;justify-content:space-between;">
          <div class="doc-page-sheet" style="aspect-ratio:16/10;display:flex;flex-direction:column;justify-content:center;border-left:6px solid var(--fmt-ppt-hdr);">
            <span class="pill-mini pill-pro" style="align-self:flex-start;margin-bottom:8px;">Slide 4 / 14</span>
            <h3 style="font-size:18px;margin-bottom:6px;">Hybrid Monetization & Clean-Room Moat</h3>
            <p style="font-size:12.5px;color:var(--text-secondary);">Zero server COGS · 16 KB ELF64 verified · Ethical pre-action gating.</p>
          </div>
          <div style="display:flex;gap:8px;overflow-x:auto;padding-bottom:4px;">
            ${[1, 2, 3, 4, 5].map((n) => `<div style="min-width:74px;height:46px;border-radius:8px;background:var(--surface-card);border:${n === 4 ? "2px solid var(--brand-accent)" : "1px solid var(--border-subtle)"};display:grid;place-items:center;font-size:11.5px;font-weight:700;">#${n}</div>`).join("")}
          </div>
        </div>
        <div class="editor-bottom-shelf">
          <div class="editor-tab-strip">
            ${tabs.map((t) => `<button class="ed-tab ${appState.pptTab === t ? "active" : ""}" data-ppt-tab="${t}">${t}</button>`).join("")}
          </div>
          <div class="editor-tool-grid">
            ${["Fill Color", "Stroke", "Bring Front", "Send Back", "Speaker Notes", "Play Deck"].map((a) => `<button class="ed-action-chip" ${a === "Play Deck" ? 'data-nav="S-15"' : `data-action="toast:${a} selected"`}>${a}</button>`).join("")}
          </div>
        </div>`;
    }

    if (s === "S-15") {
      renderStatusBar("#090D14");
      return `
        <div style="flex:1;background:#090D14;color:#F8FAFC;display:flex;flex-direction:column;justify-content:space-between;padding:20px;">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <span class="pill-mini" style="background:rgba(255,255,255,0.12);color:#fff;">Slide 4 / 14 · 02:18</span>
            <button class="done-cta-on-color" data-nav="S-14c">Exit</button>
          </div>
          <div style="background:#FFFFFF;color:#0F172A;border-radius:14px;padding:28px 20px;box-shadow:0 20px 50px rgba(0,0,0,0.6);position:relative;">
            <div style="width:12px;height:12px;border-radius:50%;background:#EF4444;box-shadow:0 0 12px #EF4444;position:absolute;right:48px;top:44px;"></div>
            <h2 style="font-size:20px;margin-bottom:8px;">Evora Office — Presenter Mode</h2>
            <p style="font-size:13px;color:#475569;">Live laser pointer overlay active. Swipe left/right to advance slides seamlessly.</p>
          </div>
          <div style="display:flex;justify-content:space-between;gap:10px;">
            <button class="done-cta-on-color" style="flex:1;">Prev</button>
            <button class="done-cta-on-color" style="flex:1;background:rgba(239,68,68,0.28);">Laser Ink: ON</button>
            <button class="done-cta-on-color" style="flex:1;">Next</button>
          </div>
        </div>`;
    }

    if (s === "S-16") {
      renderStatusBar("var(--fmt-pdf-hdr)");
      const tabs = ["Annotate", "Signature", "File", "Page"];
      return `
        <div class="app-topbar format-hdr" style="background:var(--fmt-pdf-hdr);">
          <div class="topbar-left">
            <button class="icon-btn-48" data-nav="S-13" aria-label="Back">${ICONS.back}</button>
            <span class="topbar-title">PDF Annotate & Sign</span>
          </div>
          <button class="done-cta-on-color" data-action="toast:Annotations & Signature flattened to PDF">Save</button>
        </div>
        <div class="reader-canvas">
          <div class="doc-page-sheet">
            <p style="margin-bottom:10px;">This <mark style="background:#FEF08A;padding:1px 3px;">highlighted agreement clause</mark> binds clean-room PDFBox-Android incremental writes compatible with Acrobat and Chrome.</p>
            <div style="border:2px dashed var(--brand-accent);border-radius:10px;padding:12px;margin:14px 0;background:rgba(36,137,255,0.05);display:flex;align-items:center;justify-content:space-between;">
              <svg width="150" height="44" viewBox="0 0 150 44" aria-label="Handwritten Signature Preview">
                <path d="M10 32 C 25 6, 35 38, 48 20 C 60 6, 65 34, 82 22 C 95 14, 118 28, 138 18" fill="none" stroke="#0F172A" stroke-width="2.5" stroke-linecap="round"/>
              </svg>
              <span class="pill-mini pill-success">${ICONS.shield} PKCS#7 Valid</span>
            </div>
            <div class="banner-info banner-warn" style="margin:0;">
              <span>Sticky Note (Author: CEO): "Approved for v1.0.0"</span>
            </div>
          </div>
        </div>
        <div class="editor-bottom-shelf">
          <div class="editor-tab-strip">
            ${tabs.map((t) => `<button class="ed-tab ${appState.pdfTab === t ? "active" : ""}" data-pdf-tab="${t}">${t}</button>`).join("")}
          </div>
          <div class="editor-tool-grid">
            ${appState.pdfTab === "Signature"
              ? `<button class="ed-action-chip on" data-open-modal="sigpad">${ICONS.penTool} Draw Signature</button>
                 <button class="ed-action-chip" data-open-modal="pkcs7">${ICONS.shield} Inspect PKCS#7 Cert</button>`
              : `<button class="ed-action-chip on">Highlight</button>
                 <button class="ed-action-chip">Underline</button>
                 <button class="ed-action-chip">Ink Pen</button>
                 <button class="ed-action-chip">+ Sticky Note</button>`}
          </div>
        </div>`;
    }

    if (s === "S-17") {
      const titles = {
        split: "Split PDF (Real Offline)",
        compress: "Compress PDF (DPI Presets)",
        watermark: "Watermark PDF",
        merge: "Merge PDF (Reorder)",
        password: "Set / Remove AES Password",
        pdf2docx: "Offline PDF -> DOCX",
        img2pdf: "Image -> PDF",
        doc2pdf: "Office / HWP -> PDF",
        txt2pdf: "Text -> PDF",
        pdf2img: "PDF -> Long / PNG Images",
      };
      const tTitle = titles[appState.toolSelected] || "PDF Power Tool";
      const isPro = ["compress", "watermark", "pdf2docx"].includes(appState.toolSelected);
      let stageHtml = "";
      if (appState.toolStage === "progress") {
        stageHtml = `
          <div class="card-box" style="text-align:center;padding:34px 18px;">
            <div style="width:74px;height:74px;margin:0 auto 14px;border-radius:50%;border:5px solid var(--primary-container);border-top-color:var(--brand-accent);display:grid;place-items:center;font-family:var(--font-mono);font-weight:700;">74%</div>
            <h3 style="margin-bottom:6px;">Processing Offline via PDFBox</h3>
            <p style="font-size:13px;color:var(--text-secondary);margin-bottom:16px;">Zero cloud upload · Preserving typography & layers</p>
            <button class="btn-primary" data-tool-stage="success">Finish Simulation (100%)</button>
          </div>`;
      } else if (appState.toolStage === "success") {
        stageHtml = `
          <div class="card-box" style="text-align:center;padding:24px 18px;">
            <div style="width:62px;height:62px;margin:0 auto 12px;border-radius:50%;background:var(--success-container);color:var(--on-success-container);display:grid;place-items:center;">${ICONS.check}</div>
            <h3 style="margin-bottom:4px;">Completed Successfully</h3>
            <p style="font-size:13px;color:var(--text-secondary);margin-bottom:12px;">Contract_Signed_v2_output.pdf</p>
            <div class="banner-info banner-success" style="justify-content:center;font-family:var(--font-mono);font-weight:700;">14.2 MB -> 3.8 MB (-73% saved)</div>
            <button class="btn-primary" style="margin-bottom:8px;" data-nav="S-13">Open Output</button>
            <button class="btn-outlined" data-tool-stage="config">Configure Another Run</button>
          </div>`;
      } else {
        stageHtml = `
          ${isPro ? `<div class="banner-info banner-warn"><span>Pro Utility: Upfront Rewarded Pass or Pro required before starting.</span><button class="btn-text" style="min-height:30px;" data-open-modal="reward">Gate</button></div>` : ""}
          <div class="card-box">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;">
              <strong>Selected File</strong>
              <span class="pill-mini pill-page">12 Pages · 14.2 MB</span>
            </div>
            <div class="doc-card" style="min-height:60px;">
              <div class="fmt-badge fmt-pdf">PDF</div>
              <div class="doc-info"><div class="doc-name">Contract_Signed_v2.pdf</div></div>
            </div>
          </div>
          <div class="card-box">
            <strong style="display:block;margin-bottom:10px;">Tool Parameters</strong>
            <label style="display:block;font-size:12.5px;color:var(--text-secondary);margin-bottom:6px;">
              ${appState.toolSelected === "split" ? "Split Page Ranges (or Every N Pages):" : appState.toolSelected === "watermark" ? "Watermark Text (-45° / 35% Opacity):" : "Output Filename:"}
            </label>
            <input class="input-field" value="${appState.toolSelected === "split" ? "1-3, 5, 8-12" : appState.toolSelected === "watermark" ? "CONFIDENTIAL — EVORA" : "Contract_Processed_v2"}" aria-label="Tool parameter input" />
          </div>
          <button class="btn-primary" data-tool-stage="progress">Run Offline Now</button>`;
      }
      return `
        <div class="app-topbar">
          <div class="topbar-left">
            <button class="icon-btn-48" data-nav="S-12" aria-label="Back to Tools">${ICONS.back}</button>
            <span class="topbar-title">${tTitle}</span>
          </div>
        </div>
        <div class="scroll-content">
          <div style="display:flex;gap:6px;overflow-x:auto;margin-bottom:12px;">
            ${[["split", "Split"], ["compress", "Compress"], ["watermark", "Watermark"], ["merge", "Merge"], ["password", "AES Lock"], ["pdf2docx", "PDF->DOCX"]]
              .map(([k, lbl]) => `<button class="filter-chip ${appState.toolSelected === k ? "active" : ""}" style="min-height:34px;" data-switch-tool="${k}">${lbl}</button>`)
              .join("")}
          </div>
          ${stageHtml}
        </div>`;
    }

    if (s === "S-18") {
      renderStatusBar("#070B12");
      let centerStage = "";
      if (appState.scanMode === "idcard") {
        const msg =
          appState.idCardStep === 1
            ? "Step 1 of 2: Align FRONT Side of ID Card"
            : appState.idCardStep === 2
            ? "Step 2 of 2: Flip & Align BACK Side"
            : "A4 Dual-Side Composite Ready (Front + Back)";
        centerStage = `
          <div class="camera-frame-box">
            <span class="pill-mini pill-pro">${ICONS.idCard} DUAL-SIDE ID MODE</span>
            <h3 style="font-size:16px;">${msg}</h3>
            ${appState.idCardStep === 3
              ? `<div style="width:170px;background:#fff;color:#0F172A;border-radius:10px;padding:10px;display:flex;flex-direction:column;gap:8px;">
                   <div style="height:44px;border:1px solid #94A3B8;border-radius:6px;display:grid;place-items:center;font-size:11px;font-weight:700;">ID FRONT</div>
                   <div style="height:44px;border:1px solid #94A3B8;border-radius:6px;display:grid;place-items:center;font-size:11px;font-weight:700;">ID BACK</div>
                 </div>
                 <button class="done-cta-pill" data-action="toast:Exported A4 Dual-Side ID Card PDF!">Export A4 PDF</button>`
              : `<p style="font-size:12px;color:#94A3B8;">Fixes competitor stub: real CameraX + A4 centering</p>`}
          </div>`;
      } else if (appState.scanMode === "ocr") {
        centerStage = `
          <div class="camera-frame-box" style="border-style:solid;background:rgba(15,23,42,0.9);text-align:left;align-items:stretch;">
            <div style="display:flex;gap:5px;flex-wrap:wrap;">
              ${["Latin", "Chinese", "Devanagari", "Japanese", "Korean"].map((sc) => `<button class="filter-chip ${appState.ocrScript === sc ? "active" : ""}" style="min-height:30px;font-size:11px;" data-ocr-script="${sc}">${sc}</button>`).join("")}
            </div>
            <div style="background:#1E293B;padding:10px;border-radius:10px;font-size:12px;line-height:1.5;">
              <strong>ML Kit Text Recognition v2 (${appState.ocrScript}):</strong><br/>
              "Evora Technologies Global — Invoice #2026-118 · Total: $1,420.00"
            </div>
            <div style="display:flex;gap:8px;">
              <button class="done-cta-pill" style="flex:1;" data-action="toast:Copied OCR text">Copy Text</button>
              <button class="done-cta-on-color" style="flex:1;" data-action="toast:Exported .docx">Export .docx</button>
            </div>
          </div>`;
      } else if (appState.scanMode === "qr") {
        centerStage = `
          <div class="camera-frame-box">
            ${svg('<rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><path d="M14 14h4v4h-4z"/>', 44)}
            <div style="background:#1E293B;padding:12px;border-radius:12px;width:100%;">
              <div style="font-size:11px;color:#93C5FD;font-weight:700;">WIFI QR DETECTED</div>
              <div style="font-size:13px;margin:4px 0 8px;">SSID: Evora_Studio_5G</div>
              <button class="done-cta-pill" data-action="toast:Joined Wi-Fi / Copied credentials">Connect Safely</button>
            </div>
          </div>`;
      } else {
        centerStage = `
          <div class="camera-frame-box">
            <span class="pill-mini pill-success">Auto-Edge Quad Locked</span>
            <p style="font-size:13px;">Tap shutter for Multi-page Capture · Magic Color / B&W Filters</p>
          </div>`;
      }
      return `
        <div class="camera-stage">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <button class="icon-btn-48" style="color:#fff;" data-nav="S-06" aria-label="Close scanner">${ICONS.close}</button>
            <span style="font-weight:600;font-size:14px;">Smart Scanner</span>
            <button class="done-cta-on-color" data-action="toast:Flash Auto">Flash</button>
          </div>
          ${centerStage}
          <div>
            <div style="display:flex;justify-content:space-around;margin-bottom:14px;font-size:12px;">
              ${[["doc", "Document"], ["idcard", "ID Card"], ["ocr", "OCR Text"], ["qr", "QR Code"]].map(([m, l]) => `<button class="btn-text" style="color:${appState.scanMode === m ? "#4DA1FF" : "#94A3B8"};min-height:36px;" data-scan-mode="${m}">${l}</button>`).join("")}
            </div>
            <div style="display:flex;justify-content:center;">
              <button class="shutter-btn" data-shutter-tap="1" aria-label="Capture"></button>
            </div>
          </div>
        </div>`;
    }

    if (s === "S-19") {
      return `
        <div class="app-topbar">
          <div class="topbar-left">
            <button class="icon-btn-48" data-nav="S-12" aria-label="Back">${ICONS.back}</button>
            <span class="topbar-title">Wi-Fi Web & FTP Transfer</span>
          </div>
          <span class="pill-mini ${appState.wifiRunning ? "pill-success" : "pill-page"}">${appState.wifiRunning ? "ACTIVE · 09:42" : "STOPPED"}</span>
        </div>
        <div class="scroll-content">
          <div class="card-box" style="text-align:center;">
            <div style="width:128px;height:128px;margin:0 auto 12px;border:2px solid var(--border-subtle);border-radius:14px;display:grid;place-items:center;background:#fff;color:#0F172A;">
              <svg width="96" height="96" viewBox="0 0 24 24" fill="currentColor" aria-label="LAN Web UI QR Code">
                <path d="M3 3h7v7H3V3zm2 2v3h3V5H5zm9-2h7v7h-7V3zm2 2v3h3V5h-3zM3 14h7v7H3v-7zm2 2v3h3v-3H5zm9 0h2v2h-2v-2zm3 3h2v2h-2v-2zm2-5h2v4h-2v-4zm-5 5h2v2h-2v-2z"/>
              </svg>
            </div>
            <div style="font-family:var(--font-mono);font-size:14px;font-weight:700;color:var(--primary-action);">http://192.168.1.45:8998</div>
            <div style="font-family:var(--font-mono);font-size:12px;color:var(--text-muted);margin-bottom:12px;">ftp://192.168.1.45:8998</div>
            <div style="display:flex;gap:8px;">
              <button class="btn-primary" style="flex:1;" data-toggle-wifi="1">${appState.wifiRunning ? "Stop Server" : "Start Server"}</button>
              <button class="btn-outlined" style="flex:1;" data-action="toast:Copied URL to clipboard">Copy Link</button>
            </div>
          </div>
          <div class="card-box">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;">
              <div><strong>Require PIN Lock</strong><div style="font-size:12px;color:var(--text-muted);">PIN: 4829 · Protects LAN uploads</div></div>
              <button class="sort-pill" data-toggle-wifipin="1">${appState.wifiPin ? "ON" : "OFF"}</button>
            </div>
            <div style="display:flex;justify-content:space-between;align-items:center;">
              <div><strong>Read-Only Mode</strong><div style="font-size:12px;color:var(--text-muted);">Disable browser drag-drop uploads</div></div>
              <button class="sort-pill" data-toggle-wifiro="1">${appState.wifiReadOnly ? "ON" : "OFF"}</button>
            </div>
          </div>
        </div>`;
    }

    if (s === "S-20") {
      return `
        <div class="app-topbar">
          <div class="topbar-left">
            <button class="icon-btn-48" data-nav="S-12" aria-label="Back">${ICONS.back}</button>
            <span class="topbar-title">Settings & Compliance</span>
          </div>
        </div>
        <div class="scroll-content">
          <div class="section-hdr" style="margin-top:0;"><span>Appearance</span></div>
          <div class="card-box" style="padding:4px 14px;">
            <button class="sheet-action-row" style="justify-content:space-between;" data-cycle-theme="1">
              <span>App Theme (System / Light / Dark)</span><strong style="color:var(--primary-action);">${appState.theme.toUpperCase()}</strong>
            </button>
            <button class="sheet-action-row" style="justify-content:space-between;" data-nav="S-02">
              <span>Language</span><span style="color:var(--text-muted);">English (10 locales)</span>
            </button>
          </div>
          <div class="section-hdr"><span>Subscription & Privacy</span></div>
          <div class="card-box" style="padding:4px 14px;">
            <button class="sheet-action-row" style="justify-content:space-between;" data-nav="S-04"><span>Evora Pro Plans</span><span class="pill-mini pill-pro">Upgrade</span></button>
            <button class="sheet-action-row" data-action="toast:Queried Play Billing 8.0 purchases">Restore Purchases</button>
            <button class="sheet-action-row" data-action="toast:Opened Google UMP Privacy Choices">Privacy & Consent Choices (UMP)</button>
          </div>
          <div class="section-hdr"><span>Open-Source Attribution</span></div>
          <div class="card-box" style="font-size:12.5px;color:var(--text-secondary);">
            <strong>Evora Office v1.0.0 (100)</strong><br/>
            Clean-Room Track A · LibreOfficeKit (MPLv2/LGPLv3), PdfiumAndroid (Apache 2.0), PDFBox-Android (Apache 2.0), hwplib/hwpxlib (Apache 2.0), Commons Compress (Apache 2.0).
          </div>
        </div>`;
    }

    if (s === "W-01") {
      return `
        <div class="app-topbar">
          <div class="topbar-left">
            <button class="icon-btn-48" data-nav="S-06" aria-label="Back">${ICONS.back}</button>
            <span class="topbar-title">Android Home Glance Widgets</span>
          </div>
        </div>
        <div class="scroll-content" style="background:linear-gradient(150deg,#1E3A5F,#0F172A);">
          <div style="font-size:11.5px;color:#CBD5E1;margin-bottom:8px;font-weight:600;">W-01 · QUICK ACTION BAR (4×1)</div>
          <div class="card-box" style="border-radius:20px;display:flex;align-items:center;gap:8px;padding:10px 12px;">
            <div class="eo-mark-mini">EO</div>
            <div style="flex:1;background:var(--surface-subtle);border-radius:99px;padding:9px 12px;font-size:12.5px;color:var(--text-muted);cursor:pointer;" data-nav="S-08">Search documents...</div>
            <button class="icon-btn-48" style="background:var(--primary-container);color:var(--on-primary-container);" data-nav="S-18" aria-label="Scan">${ICONS.scan}</button>
            <button class="icon-btn-48" style="background:var(--surface-subtle);" data-nav="S-09" aria-label="New doc">${ICONS.plus}</button>
          </div>

          <div style="font-size:11.5px;color:#CBD5E1;margin:16px 0 8px;font-weight:600;">W-02 · RECENT DOCUMENTS WIDGET (4×2)</div>
          <div class="card-box" style="border-radius:22px;padding:12px;">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;">
              <strong style="font-size:13.5px;">Recent Files</strong>
              <button class="sort-pill" data-nav="S-18">${ICONS.scan} Scan</button>
            </div>
            ${DOCS.slice(0, 3)
              .map(
                (d) => `
              <div style="display:flex;align-items:center;gap:10px;padding:7px 4px;border-top:1px solid var(--border-subtle);cursor:pointer;" data-open-doc="${d.id}">
                <div class="fmt-badge fmt-${d.fmt}" style="width:32px;height:32px;font-size:10px;">${d.ext}</div>
                <div style="flex:1;min-width:0;">
                  <div class="doc-name" style="font-size:12.5px;">${d.name}</div>
                  <div style="font-size:11px;color:var(--text-muted);">${d.page} · ${d.date}</div>
                </div>
              </div>`
              )
              .join("")}
          </div>
        </div>`;
    }

    return `<div class="scroll-content">Screen ${s}</div>`;
  }

  // === MODALS ===
  function renderModal() {
    const m = appState.activeModal;
    if (!m) return "";

    if (m === "context") {
      const doc = DOCS.find((d) => d.id === appState.selectedDocId) || DOCS[0];
      return `
        <div class="modal-scrim" data-close-scrim="1">
          <div class="bottom-sheet">
            <div class="sheet-handle"></div>
            <div style="display:flex;align-items:center;gap:10px;padding-bottom:10px;border-bottom:1px solid var(--border-subtle);margin-bottom:6px;">
              <div class="fmt-badge fmt-${doc.fmt}">${doc.ext}</div>
              <div><div class="doc-name">${doc.name}</div><div class="doc-meta">${doc.size} · ${doc.folder}</div></div>
            </div>
            <button class="sheet-action-row" data-open-doc-force="${doc.id}">${ICONS.play} Open Reader / Editor</button>
            <button class="sheet-action-row" data-action="toast:Shared via Android FileProvider">${ICONS.share} Share Document</button>
            <button class="sheet-action-row" data-nav="S-17">${ICONS.merge} Convert / Merge / Split</button>
            <button class="sheet-action-row" data-action="toast:Renamed file cleanly">${ICONS.edit} Rename</button>
            <button class="sheet-action-row" data-action="toast:Pinned launcher shortcut">${ICONS.plus} Add Home Shortcut</button>
            <button class="sheet-action-row" style="color:var(--error);" data-action="toast:Moved ${doc.name} to Trash">${ICONS.trash} Delete to Trash</button>
          </div>
        </div>`;
    }

    if (m === "reward") {
      return `
        <div class="modal-scrim" data-close-scrim="1">
          <div class="bottom-sheet">
            <div class="sheet-handle"></div>
            <span class="pill-mini pill-pro" style="margin-bottom:8px;">${ICONS.crown} PRE-ACTION GATE (CD-07)</span>
            <h3 style="font-size:17px;margin-bottom:6px;">Choose How to Unlock This Pro Tool</h3>
            <p style="font-size:13px;color:var(--text-secondary);margin-bottom:14px;">We always inform you before work runs — never after. Unlock High Compression, Watermark, or Batch Merge now:</p>
            <button class="btn-primary" style="margin-bottom:10px;" data-action="toast:Granted 24h Pro Pass after Rewarded Video!">${ICONS.play} Watch 1 Short Video (24h Free Pass)</button>
            <button class="btn-outlined" style="margin-bottom:8px;" data-nav="S-04">${ICONS.crown} Upgrade to Evora Pro</button>
            <button class="btn-text" style="width:100%;" data-close-modal="1">Cancel</button>
          </div>
        </div>`;
    }

    if (m === "reading") {
      return `
        <div class="modal-scrim" data-close-scrim="1">
          <div class="bottom-sheet">
            <div class="sheet-handle"></div>
            <h3 style="font-size:16px;margin-bottom:12px;">Reader Comfort Controls</h3>
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;">
              <span>Continuous Vertical Scroll</span>
              <button class="sort-pill" data-toggle-cont="1">${appState.readerContinuous ? "ON" : "Single Snap"}</button>
            </div>
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;">
              <span>Mobile Reflow View</span>
              <button class="sort-pill" data-toggle-reflow="1">${appState.readerReflow ? "ON" : "OFF"}</button>
            </div>
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:14px;">
              <span>Auto-Scroll (Speed 1..3)</span>
              <button class="sort-pill" data-toggle-autoscroll="1">${appState.readerAutoScroll ? "Playing (2x)" : "OFF"}</button>
            </div>
            <button class="btn-primary" data-close-modal="1">Apply</button>
          </div>
        </div>`;
    }

    if (m === "password") {
      return `
        <div class="modal-scrim center" data-close-scrim="1">
          <div class="center-dialog">
            <h3 style="font-size:16px;margin-bottom:6px;">Password Protected Document</h3>
            <p style="font-size:12.5px;color:var(--text-secondary);margin-bottom:10px;">Enter document password to decrypt locally offline.</p>
            <input class="input-field" type="password" value="secret123" style="margin-bottom:12px;" aria-label="Document password" />
            <div style="display:flex;gap:8px;">
              <button class="btn-outlined" data-close-modal="1">Cancel</button>
              <button class="btn-primary" data-action="toast:Document unlocked cleanly">Unlock</button>
            </div>
          </div>
        </div>`;
    }

    if (m === "draft") {
      return `
        <div class="modal-scrim center" data-close-scrim="1">
          <div class="center-dialog">
            <span class="pill-mini pill-draft" style="margin-bottom:8px;">ATOMIC DRAFT RECOVERY (US-032)</span>
            <h3 style="font-size:16px;margin-bottom:6px;">Restore Unsaved Changes?</h3>
            <p style="font-size:12.5px;color:var(--text-secondary);margin-bottom:14px;">Recovered checkpoint for <strong>Q4_Product_Roadmap.docx</strong> saved at 14:22 before process exit.</p>
            <div style="display:flex;gap:8px;">
              <button class="btn-outlined" data-action="toast:Discarded draft checkpoint">Discard</button>
              <button class="btn-primary" data-action="toast:Restored unsaved draft!">Restore Draft</button>
            </div>
          </div>
        </div>`;
    }

    if (m === "sigpad") {
      return `
        <div class="modal-scrim" data-close-scrim="1">
          <div class="bottom-sheet">
            <div class="sheet-handle"></div>
            <h3 style="font-size:16px;margin-bottom:10px;">SignaturePadRoute — Draw e-Signature</h3>
            <div style="height:130px;border:1.5px solid var(--border-input);border-radius:14px;background:#fff;display:grid;place-items:center;margin-bottom:12px;">
              <svg width="210" height="72" viewBox="0 0 210 72"><path d="M18 52 C 35 12, 55 58, 76 28 C 94 6, 108 54, 134 30 C 154 14, 174 44, 194 24" fill="none" stroke="#186FD4" stroke-width="3.2" stroke-linecap="round"/></svg>
            </div>
            <div style="display:flex;gap:8px;">
              <button class="btn-outlined" data-close-modal="1">Clear</button>
              <button class="btn-primary" data-action="toast:Saved transparent PNG signature to SignatureEntity">Save & Place</button>
            </div>
          </div>
        </div>`;
    }

    if (m === "pkcs7") {
      return `
        <div class="modal-scrim center" data-close-scrim="1">
          <div class="center-dialog">
            <span class="pill-mini pill-success" style="margin-bottom:8px;">${ICONS.shield} PKCS#7 VERIFIED</span>
            <h3 style="font-size:16px;margin-bottom:8px;">Digital Certificate Details</h3>
            <div style="font-size:12.5px;color:var(--text-secondary);line-height:1.6;margin-bottom:14px;">
              <strong>Issuer:</strong> Evora Trust CA G2<br/>
              <strong>Valid:</strong> 2026-01-01 to 2028-01-01<br/>
              <strong>Algorithm:</strong> SHA-256 RSA (BouncyCastle)
            </div>
            <button class="btn-primary" data-close-modal="1">Close Inspector</button>
          </div>
        </div>`;
    }

    if (m === "zip") {
      return `
        <div class="modal-scrim" data-close-scrim="1">
          <div class="bottom-sheet">
            <div class="sheet-handle"></div>
            <h3 style="font-size:16px;margin-bottom:4px;">ZIP / 7z Archive Inspector</h3>
            <p style="font-size:12px;color:var(--text-muted);margin-bottom:10px;">Powered by Apache Commons Compress 1.27.1 + XZ</p>
            ${["Invoice_Oct.pdf (1.2 MB)", "Budget_2026.xlsx (450 KB)", "Contract_Terms.docx (310 KB)"]
              .map((entry) => `<div style="padding:9px 0;border-bottom:1px solid var(--border-subtle);font-size:13px;display:flex;justify-content:space-between;"><span>${entry}</span><span style="color:var(--primary-action);font-weight:600;">Extract</span></div>`)
              .join("")}
            <button class="btn-primary" style="margin-top:12px;" data-action="toast:Extracted 3 entries to /Download/Unzipped/">Extract All Offline</button>
          </div>
        </div>`;
    }

    if (m === "rar") {
      return `
        <div class="modal-scrim center" data-close-scrim="1">
          <div class="center-dialog">
            <h3 style="font-size:16px;margin-bottom:6px;">Unsupported Archive (.rar)</h3>
            <p style="font-size:13px;color:var(--text-secondary);margin-bottom:14px;">Per clean-room policy (CD-04), Evora Office extracts <strong>.zip</strong> and <strong>.7z</strong> offline and excludes proprietary .rar libraries.</p>
            <button class="btn-primary" data-close-modal="1">Got It</button>
          </div>
        </div>`;
    }
    return "";
  }

  // === STUDIO SHELL RENDERING ===
  function renderSidebar() {
    const sb = document.getElementById("routeSidebar");
    sb.innerHTML = FLOWS.map(
      (g) => `
      <div class="flow-group">
        <div class="flow-group-title">${g.title}</div>
        ${g.items
          .map(
            (item) => `
          <button class="route-nav-btn ${appState.activeScreen === item.id ? "active" : ""}" data-nav="${item.id}">
            <span class="route-id-pill">${item.id}</span>
            <span>${item.label}</span>
          </button>`
          )
          .join("")}
      </div>`
    ).join("");
  }

  function renderInspector() {
    const meta = ALL_ROUTES.find((r) => r.id === appState.activeScreen) || ALL_ROUTES[0];
    document.getElementById("specInspector").innerHTML = `
      <div class="spec-card">
        <div class="spec-label">Active Screen & Route</div>
        <div class="spec-value">${meta.id} · ${meta.route}</div>
        <div class="spec-tags">${meta.us.map((u) => `<span class="spec-tag">${u}</span>`).join("")}</div>
      </div>
      <div class="spec-card">
        <div class="spec-label">Transition & Ergonomics</div>
        <div style="font-size:12.5px;color:#E2E8F0;margin-bottom:8px;">${meta.transition}</div>
        <ul class="spec-list">${meta.notes.map((n) => `<li>${n}</li>`).join("")}</ul>
      </div>
      <div class="spec-card">
        <div class="spec-label">Inspect Modals & Edge States</div>
        <div class="quick-modal-triggers">
          <button class="quick-trigger-btn" data-open-modal="context">FileContextMenu Bottom Sheet</button>
          <button class="quick-trigger-btn" data-open-modal="reward">RewardGateSheet (CD-07 Upfront)</button>
          <button class="quick-trigger-btn" data-open-modal="draft">DraftRecoveryDialog (US-032)</button>
          <button class="quick-trigger-btn" data-open-modal="password">Password Unlock Dialog (US-022)</button>
          <button class="quick-trigger-btn" data-open-modal="sigpad">SignaturePad Canvas (US-051)</button>
          <button class="quick-trigger-btn" data-open-modal="pkcs7">PKCS#7 Cert Inspector (US-051)</button>
          <button class="quick-trigger-btn" data-open-modal="zip">ZIP/7z Archive Inspector (US-024)</button>
        </div>
      </div>
    `;
  }

  function render() {
    document.documentElement.setAttribute("data-app-theme", appState.theme);
    const frame = document.getElementById("deviceFrame");
    frame.classList.toggle("tablet", appState.device === "tablet");

    const vp = document.getElementById("screenViewport");
    const toast = appState.toastMsg
      ? `<div style="position:absolute;bottom:86px;left:16px;right:16px;background:#0F172A;color:#FFFFFF;padding:10px 14px;border-radius:12px;font-size:12.5px;font-weight:600;z-index:30;box-shadow:0 8px 20px rgba(0,0,0,0.35);text-align:center;">${appState.toastMsg}</div>`
      : "";

    vp.innerHTML = renderScreen() + renderModal() + toast;
    renderSidebar();
    renderInspector();

    // Sync top controls
    document.querySelectorAll("[data-theme-btn]").forEach((b) => b.classList.toggle("active", b.dataset.themeBtn === appState.theme));
    document.querySelectorAll("[data-device-btn]").forEach((b) => b.classList.toggle("active", b.dataset.deviceBtn === appState.device));
    document.querySelectorAll("[data-state-btn]").forEach((b) => b.classList.toggle("active", b.dataset.stateBtn === appState.viewMode));

    const searchEl = document.getElementById("searchInputField");
    if (searchEl) {
      searchEl.addEventListener("input", (e) => {
        appState.searchQuery = e.target.value;
        render();
        const nextEl = document.getElementById("searchInputField");
        if (nextEl) {
          nextEl.focus();
          nextEl.setSelectionRange(nextEl.value.length, nextEl.value.length);
        }
      });
    }
  }

  // === GLOBAL EVENT DELEGATION ===
  document.addEventListener("click", (e) => {
    if (e.target && e.target.dataset && e.target.dataset.closeScrim) {
      appState.activeModal = null;
      render();
      return;
    }
    const t = e.target.closest("button, [data-nav], [data-open-modal], [data-open-doc], [data-open-doc-force], [data-open-cloud], [data-select-lang], [data-select-sku], [data-close-modal]");
    if (!t) return;

    if (t.dataset.themeBtn) { appState.theme = t.dataset.themeBtn; render(); return; }
    if (t.dataset.deviceBtn) { appState.device = t.dataset.deviceBtn; render(); return; }
    if (t.dataset.stateBtn) { appState.viewMode = t.dataset.stateBtn; render(); return; }
    if (t.dataset.setState) { appState.viewMode = t.dataset.setState; render(); return; }

    if (t.dataset.nav) { navigate(t.dataset.nav); return; }
    if (t.dataset.openModal) { appState.activeModal = t.dataset.openModal; render(); return; }
    if (t.dataset.closeModal) { appState.activeModal = null; render(); return; }
    if (t.dataset.action) {
      const msg = t.dataset.action.replace("toast:", "");
      appState.activeModal = null;
      showToast(msg);
      return;
    }

    if (t.dataset.selectLang) { appState.selectedLang = t.dataset.selectLang; render(); return; }
    if (t.dataset.introDot !== undefined) { appState.introSlide = Number(t.dataset.introDot); render(); return; }
    if (t.dataset.nextIntro) {
      if (appState.introSlide < 2) { appState.introSlide += 1; render(); } else { navigate("S-04"); }
      return;
    }
    if (t.dataset.selectSku) { appState.selectedSku = t.dataset.selectSku; render(); return; }

    if (t.dataset.homeFilter) { appState.homeFilter = t.dataset.homeFilter; render(); return; }
    if (t.dataset.cycleSort) {
      const order = ["Modified", "Name A-Z", "Size", "Format"];
      appState.homeSort = order[(order.indexOf(appState.homeSort) + 1) % order.length];
      render(); return;
    }
    if (t.dataset.toggleFav) {
      e.stopPropagation();
      const doc = DOCS.find((d) => d.id === t.dataset.toggleFav);
      if (doc) doc.fav = !doc.fav;
      render(); return;
    }
    if (t.dataset.openContext) {
      e.stopPropagation();
      appState.selectedDocId = t.dataset.openContext;
      appState.activeModal = "context";
      render(); return;
    }
    if (t.dataset.openDoc || t.dataset.openDocForce) {
      const id = t.dataset.openDoc || t.dataset.openDocForce;
      const doc = DOCS.find((d) => d.id === id) || DOCS[0];
      if (appState.activeScreen === "S-10" && appState.multiSelectMode) {
        if (appState.selectedStorageIds.has(id)) appState.selectedStorageIds.delete(id);
        else appState.selectedStorageIds.add(id);
        render(); return;
      }
      if (appState.device === "tablet" && appState.activeScreen === "S-06" && !t.dataset.openDocForce) {
        appState.selectedDocId = id; render(); return;
      }
      appState.selectedDocId = id;
      if (doc.fmt === "word") navigate("S-14a");
      else if (doc.fmt === "excel") navigate("S-14b");
      else if (doc.fmt === "ppt") navigate("S-14c");
      else if (doc.ext === "ZIP") { appState.activeModal = "zip"; render(); }
      else navigate("S-13");
      return;
    }

    if (t.dataset.recentTab) { appState.recentTab = t.dataset.recentTab; render(); return; }
    if (t.dataset.quickSearch) { appState.searchQuery = t.dataset.quickSearch; render(); return; }
    if (t.dataset.clearSearch) { appState.searchQuery = ""; render(); return; }
    if (t.dataset.tplFilter) { appState.tplFilter = t.dataset.tplFilter; render(); return; }
    if (t.dataset.toggleMultiselect) { appState.multiSelectMode = !appState.multiSelectMode; render(); return; }
    if (t.dataset.storageVol) { appState.storageFolder = t.dataset.storageVol; render(); return; }
    if (t.dataset.openCloud) { appState.cloudProvider = t.dataset.openCloud; navigate("S-11"); return; }
    if (t.dataset.toggleCloud) { appState.cloudProvider = appState.cloudProvider === "gdrive" ? "onedrive" : "gdrive"; render(); return; }

    if (t.dataset.rtheme) { appState.readerTheme = t.dataset.rtheme; render(); return; }
    if (t.dataset.toggleTts) { appState.readerTts = !appState.readerTts; render(); return; }
    if (t.dataset.toggleCont) { appState.readerContinuous = !appState.readerContinuous; render(); return; }
    if (t.dataset.toggleReflow) { appState.readerReflow = !appState.readerReflow; render(); return; }
    if (t.dataset.toggleAutoscroll) { appState.readerAutoScroll = !appState.readerAutoScroll; render(); return; }

    if (t.dataset.wordTab) { appState.wordTab = t.dataset.wordTab; render(); return; }
    if (t.dataset.toggleTrack) { appState.trackChangesOn = !appState.trackChangesOn; render(); return; }
    if (t.dataset.excelTab) { appState.excelTab = t.dataset.excelTab; render(); return; }
    if (t.dataset.pptTab) { appState.pptTab = t.dataset.pptTab; render(); return; }
    if (t.dataset.pdfTab) { appState.pdfTab = t.dataset.pdfTab; render(); return; }

    if (t.dataset.openTool) {
      const k = t.dataset.openTool;
      if (k === "print") { showToast("Opened Android System PrintManager Preview (Fixed Stub!)"); return; }
      if (k === "scan_ocr") { appState.scanMode = "ocr"; navigate("S-18"); return; }
      if (k === "scan_qr") { appState.scanMode = "qr"; navigate("S-18"); return; }
      appState.toolSelected = k;
      appState.toolStage = "config";
      navigate("S-17"); return;
    }
    if (t.dataset.switchTool) { appState.toolSelected = t.dataset.switchTool; appState.toolStage = "config"; render(); return; }
    if (t.dataset.toolStage) { appState.toolStage = t.dataset.toolStage; render(); return; }

    if (t.dataset.scanMode) { appState.scanMode = t.dataset.scanMode; appState.idCardStep = 1; render(); return; }
    if (t.dataset.ocrScript) { appState.ocrScript = t.dataset.ocrScript; render(); return; }
    if (t.dataset.shutterTap) {
      if (appState.scanMode === "idcard") {
        appState.idCardStep = appState.idCardStep >= 3 ? 1 : appState.idCardStep + 1;
        render();
      } else {
        showToast("Captured frame -> Perspective Crop & Magic Color");
      }
      return;
    }

    if (t.dataset.toggleWifi) { appState.wifiRunning = !appState.wifiRunning; render(); return; }
    if (t.dataset.toggleWifipin) { appState.wifiPin = !appState.wifiPin; render(); return; }
    if (t.dataset.toggleWifiro) { appState.wifiReadOnly = !appState.wifiReadOnly; render(); return; }
    if (t.dataset.cycleTheme) { appState.theme = appState.theme === "light" ? "dark" : "light"; render(); return; }
  });

  // Apply optional URL query overrides (?screen=S-12&theme=dark&device=tablet)
  const params = new URLSearchParams(window.location.search);
  if (params.get("screen") && ALL_ROUTES.some((r) => r.id === params.get("screen"))) {
    appState.activeScreen = params.get("screen");
  }
  if (params.get("theme") === "dark" || params.get("theme") === "light") {
    appState.theme = params.get("theme");
  }
  if (params.get("device") === "tablet" || params.get("device") === "phone") {
    appState.device = params.get("device");
  }
  if (params.get("modal")) {
    appState.activeModal = params.get("modal");
  }

  render();
})();
