# Kế hoạch sau phát hành: tách Route/Screen và chia nhỏ file quá 200 dòng

- **Trạng thái:** Chờ thực hiện. Chỉ bắt đầu sau khi bản `1.0.0` (`versionCode 100`) đã lên Google Play.
- **Phạm vi:** Refactor thuần, không thay đổi hành vi, không đổi public API (chữ ký `XRoute`), không đổi navigation graph.
- **Người lập:** phiên điều phối chính, 27/11/2026, theo yêu cầu (c) của CEO.

## Bảng quyết định

| Ngày | Quyết định | Nguồn |
|---|---|---|
| 27/11/2026 | Lập kế hoạch tách Route/Screen và chia nhỏ file quá dài; làm sau khi phát hành | CEO, yêu cầu (c) |
| 27/11/2026 | Đề xuất: file test và file 201–230 dòng chỉ xử lý khi có người sửa tới (xem §8) | Đề xuất, chờ CEO |

## 1. Hiện trạng (đo ngày 27/11/2026, commit `204af3e`)

- `LLM.md` §5 mô tả mỗi màn hình có `<Screen>Route.kt` **và** `<Screen>Screen.kt`. Thực tế: 21 file `*Route.kt`, chỉ 1 file `*Screen.kt` (`PaywallScreen.kt`).
- Ở mức hàm, chuẩn `.claude/rules/android-native-standards.md` §4 đã đạt: 20/21 file Route đã có composable stateless `XScreen(state, onIntent)` riêng, nằm chung file với `XRoute`. Riêng `PaywallRoute.kt` đã tách file.
- Vấn đề thật là kích thước. `.claude/rules/development-rules.md` giới hạn file dưới 200 dòng, nhưng có **56 file** `src/main/**/*.kt` vượt mức (19 trong đó là file Route), và thêm 7 file test.
- Bản thân các hàm `XScreen` rất dài, ví dụ `DocumentReaderScreen` khoảng 510 dòng, `PdfEditorScreen` khoảng 565 dòng, `ScannerFlowScreen` khoảng 490 dòng. Vì vậy chỉ chuyển file là không đủ: phải tách hàm thành các section composable.

Lệnh đo lại (chạy ở gốc repo):

```bash
find . -path ./.local -prune -o -path '*/build' -prune -o -name '*.kt' -print | grep '/src/main/' \
  | while read f; do n=$(wc -l < "$f"); [ "$n" -gt 200 ] && printf "%5d %s\n" "$n" "$f"; done | sort -rn
```

## 2. Nguyên tắc bất biến cho mọi bước

1. **Refactor thuần, không đổi hành vi.** Không sửa logic, string, design token hay thứ tự side effect. Nếu phát hiện bug thì ghi lại và sửa ở một commit riêng.
2. **Giữ nguyên package.** File mới nằm cùng package với file gốc, nên caller không phải đổi import. `XScreen` giữ `internal`; helper `private` dùng chung giữa Route và Screen được nâng lên `internal`, không lên `public`.
3. **Ranh giới Route/Screen theo `android-native-standards.md` §4:**
   - `XRoute.kt` giữ `hiltViewModel()`, `collectAsStateWithLifecycle()`, `CollectEffects`, các `rememberLauncherForActivityResult`, permission launcher và callback navigation.
   - `XScreen.kt` nhận `state` và `onIntent` (cùng lambda UI thuần nếu cần), chứa `@Preview`.
4. **Tách theo trách nhiệm, không cắt theo số dòng:** section của màn hình, sheet/dialog, platform view (`AndroidView`), helper Intent/share/print.
5. **Quy tắc MVI giữ nguyên** (CLAUDE.md, mục "Before writing code"): `onIntent` vẫn là public method duy nhất, `State`/`Intent`/`Effect` vẫn ở `XContract.kt`, mọi coroutine vẫn qua `launchSafely`.
6. **Mỗi màn hình hoặc mỗi file lớn là một commit** qua `/ck:git cm` trên nhánh refactor tách từ `duylt_dev`. Mỗi commit phải qua đủ gate: `./gradlew testDebugUnitTest lintDebug assembleDebug` (JDK 17) và `python3 -I scripts/verify_16kb_elf.py app/build/outputs/apk/debug/app-debug.apk`.
7. **`code-reviewer` review từng commit,** tập trung vào thay đổi hành vi lọt vào, visibility bị nới quá mức và việc bỏ quên `remember`/`key` khi tách composable.

## 3. Giai đoạn 1: tách Route/Screen (21 màn hình, thuộc `:feature:*`)

Thứ tự từ dễ đến khó, để chốt khuôn mẫu ở file nhỏ trước.

| # | File (số dòng) | Việc cần làm |
|---|---|---|
| 1 | `SplashRoute.kt` (117), `LanguageSelectRoute.kt` (197) | Tách file `XScreen.kt` làm khuôn mẫu; sau tách cả hai file đều dưới 200 dòng |
| 2 | `StoragePermissionRoute.kt` (209), `OnboardingIntroRoute.kt` (222) | Tách file |
| 3 | `SearchDocumentsRoute.kt` (271), `RecentBookmarksRoute.kt` (287), `SlideShowRoute.kt` (294), `SignaturePadRoute.kt` (299), `OfficeEditorRoute.kt` (309), `ArchiveInspectorRoute.kt` (321) | Tách file; nếu `XScreen.kt` còn trên 200 dòng thì tách tiếp row/item composable sang `XComponents.kt` |
| 4 | `ToolsHubRoute.kt` (384), `WifiTransferRoute.kt` (465), `SettingsRoute.kt` (508), `CloudDriveBrowserRoute.kt` (570) | Tách file và tách section composable |
| 5 | `HomeDocumentsRoute.kt` (682) | Tách file. `HomeDocumentsScreen` hiện là một hàm lớn; khi bắt đầu, xác định các vùng UI rồi tách mỗi vùng thành một section composable |
| 6 | `PdfEditorRoute.kt` (734) | `PdfEditorScreen` dài khoảng 565 dòng và chưa có hàm con: tách theo vùng UI như bước 5 |
| 7 | `PdfToolConfigRoute.kt` (747) | `ToolConfigSection` (khoảng 260 dòng) chuyển sang file riêng, tách theo nhóm `PdfToolKind`; `shareFileUri` và `resolveDisplayName` sang `PdfToolConfigIntents.kt` |
| 8 | `StorageBrowserRoute.kt` (834) | Các row `CloudQuickRow`, `CloudSideRow`, `VolumeChipsRow`, `StorageEntryRow` và `BatchBarAction` sang `StorageBrowserRows.kt`; `shareMultipleEntries` sang file helper |
| 9 | `ScannerFlowRoute.kt` (808) | Phần CameraX `AndroidView` cùng `DisposableEffect` và `analyzeQrFrame` sang `ScannerCameraViewfinder.kt`, giữ nguyên bản sửa lifecycle/clip ngày 27/11; `QrActionCard` sang file riêng; `copyUriToTempJpeg` sang helper. **Test lại trên thiết bị:** xoay màn hình, ra nền rồi quay lại, chuyển mode QR/Document |
| 10 | `DocumentReaderRoute.kt` (1134) | `PdfPageSheet`, `HwpSandboxedWebView` và `ReadingSettingsSheet` mỗi cái một file; `flattenOutline` và `printHtmlOrText` sang `DocumentReaderHelpers.kt`; `DocumentReaderScreen` tách theo vùng (top bar, viewer theo định dạng, thanh điều hướng trang). **Giữ nguyên cấu hình sandbox của `HwpSandboxedWebView`** (yêu cầu bảo mật); `appsec-engineer` xem lại diff |
| 11 | `PaywallScreen.kt` (291) | Đã tách file. `PaywallScreen` là một hàm lớn; tách theo vùng UI cho dưới 200 dòng |

## 4. Giai đoạn 2: file UI khác (không phải Route)

| File (số dòng) | Hướng tách |
|---|---|
| `app/.../navigation/AppNavHost.kt` (691) | `AppNavHost` dài khoảng 460 dòng: tách thành các extension `NavGraphBuilder.onboardingGraph()`, `libraryGraph()`, `readerGraph()`, `editorGraph()`, `toolsGraph()`… mỗi graph một file trong `:app`; `AppRoutes` và `IncomingLaunchAction` mỗi thứ một file; `MainShell` một file riêng |
| `feature/editor/.../office/OfficeEditorPanels.kt` (822) | Mỗi định dạng một file: `WordEditorPanels.kt`, `ExcelEditorPanels.kt`, `PptEditorPanels.kt`, `FindReplaceBar.kt` |
| `feature/library/.../widget/EvoraGlanceWidgets.kt` (409) | `QuickBarGlanceWidget` và `RecentDocsGlanceWidget` mỗi widget một file, cùng `EntryPoint` dùng chung |
| `core/designsystem/.../component/StateComponents.kt` (328) | Mỗi component một file: `ShimmerDocListSkeleton`, `EmptyStateView`, `ErrorStateCard`, `PasswordUnlockDialog`; `LocalReducedMotion` và `ProvideReducedMotion` sang file riêng |
| `core/designsystem/.../theme/EvoraTheme.kt` (257), `component/DocCard.kt` (212) | Bảng màu (Light/Dark color scheme, `EvoraExtraColors`, `EvoraFormatColors`, `ReaderTheme.toPalette`) sang `EvoraColors.kt`; `EvoraTheme` và `EvoraThemeTokens` ở lại. `DocCard` xử lý khi có người sửa tới (§8) |

## 5. Giai đoạn 3: ViewModel quá 200 dòng (rủi ro cao hơn)

`OfficeEditorViewModel` (796), `DocumentReaderViewModel` (528), `PdfEditorViewModel` (378), `PdfToolConfigViewModel` (360), `CloudDriveBrowserViewModel` (297), `StorageBrowserViewModel` (266), `ScannerFlowViewModel` (235), `HomeDocumentsViewModel` (230).

Cách làm:

- Logic thuần (tính state mới, validate, map) chuyển sang hàm `internal` hoặc class `internal` không phụ thuộc Android, đặt cùng package, ví dụ `OfficeEditorFormattingReducer.kt`. Logic dùng chung được giữa nhiều màn hình thì đưa xuống use case trong `:core:domain`.
- ViewModel chỉ còn `onIntent` → dispatch → `launchSafely`. Không thêm public method nào.
- Bắt buộc: test ViewModel hiện có vẫn xanh **mà không sửa assertion**. Viết thêm unit test cho phần logic thuần vừa tách (`tester`).

## 6. Giai đoạn 4: data, engine và model

| Nhóm | File (số dòng) | Hướng tách |
|---|---|---|
| Engine PDF | `PdfBoxPowerEngine.kt` (1232) | Giữ class này làm facade cài port; tách thành các collaborator `internal`: annotation/chữ ký tay; PKCS#7 (inspect, sign); mật khẩu (set, remove); merge/split/nén/watermark; chuyển đổi (images/text/document → PDF, PDF → images/DOCX). Helper dùng chung (`loadPdDocument`, `readUriBytes`) sang một file riêng |
| Engine Office | `OoxmlOfficeEngine.kt` (993), `DraftSnapshotSerializer.kt` (289), `LibreOfficeNativeBridge.kt` (270), `LibreOfficeEngineAdapter.kt` (207) | Tách theo định dạng DOCX/XLSX/PPTX; **không đụng chữ ký JNI** trong `org.libreoffice.kit.*` |
| Repository | `DocumentRepositoryImpl.kt` (867), `StorageBrowserRepositoryImpl.kt` (722) | `DocumentRepositoryImpl` tách theo nhóm: observe/search cùng index MediaStore; trạng thái đọc (meta, bookmark, favorite, recent, vị trí đọc); thao tác file (rename, copy, move); trash (move, restore, delete, empty). `StorageBrowserRepositoryImpl`: xác định nhóm khi bắt đầu |
| Đặt tên lại theo domain | `Sprint2Models.kt` (319), `Sprint2Repositories.kt` (318) | Tên "Sprint2" không nói lên nội dung; tách theo domain thành nhiều file. Cần đổi `LLM.md` §10 cùng commit |
| Port | `EngineAndMonetizationPorts.kt` (210) | Mỗi port một file |
| Engine khác | `MlKitScannerEngine.kt` (478), `BillingGateway.kt` (455), `WifiTransferEngine.kt` (454), `EmbeddedFtpServer.kt` (388), `CloudDriveEngineImpl.kt` (376), `CommonsCompressArchiveEngine.kt` (305), `KtorWifiHttpServer.kt` (243), `HwplibEngineImpl.kt` (242), `OneDriveCloudClient.kt` (226), `GoogleDriveCloudClient.kt` (204) | Tách theo trách nhiệm. Với `EmbeddedFtpServer`, `KtorWifiHttpServer` và `TransferPathSandbox`, giữ nguyên kiểm tra path sandbox, và `appsec-engineer` review |
| Domain util | `ExcelFormulaEvaluator.kt` (297), `OoxmlDocxWriter.kt` (207), `DocModels.kt` (236) | `ExcelFormulaEvaluator`: tách parser khỏi evaluator. Hai file còn lại xử lý khi có người sửa tới (§8) |

## 7. Đồng bộ tài liệu

- **Cuối giai đoạn 1:**
  - `LLM.md` §5: xác nhận `XScreen.kt` là file riêng và nêu quy ước tên file tách thêm (`XComponents.kt`, `XSheets.kt`…).
  - `LLM.md` §11: chuyển mục "Route/Screen file split" từ **Open** sang **Fixed**.
- **Cuối giai đoạn 2:** `LLM.md` §7 ghi lại cấu trúc nav graph theo feature.
- **Cuối giai đoạn 4:**
  - `LLM.md` §10 cập nhật tên file mới, nhất là các file `Sprint2*`.
  - `LLM.md` §11: chuyển mục "200-line rule" sang **Fixed**, hoặc ghi rõ ngoại lệ CEO đã chấp nhận.
- Mỗi lần đổi package, module hoặc pattern thì cập nhật `LLM.md` **trong cùng commit**, theo CLAUDE.md.

## 8. Câu hỏi chờ CEO chốt (kèm đề xuất)

1. **File test quá 200 dòng (7 file):** đề xuất **không** tách chủ động, chỉ tách khi có người sửa file test đó. Lý do: test dài thường là nhiều case độc lập; tách ra không giảm rủi ro mà còn làm diff khó review.
2. **File 201–230 dòng** (`DocCard`, `EngineAndMonetizationPorts`, `LibreOfficeEngineAdapter`, `OoxmlDocxWriter`, `GoogleDriveCloudClient`, `HomeDocumentsViewModel`…): đề xuất tách khi có người sửa tới, thay vì mở commit riêng chỉ để cắt vài dòng.
3. **Thứ tự giai đoạn:** đề xuất chạy 1 → 2 → 3 → 4. Giai đoạn 1 và 2 rủi ro thấp (chủ yếu là chuyển code UI); giai đoạn 3 và 4 đụng logic nên cần review kỹ hơn.

## 9. Phân công và ước tính

| Giai đoạn | Thực hiện | Review / kiểm thử | Ước tính (ước tính) |
|---|---|---|---|
| 1 Route/Screen | `mobile-app-builder` | `code-reviewer`; kiểm thử tay trên thiết bị cho Scanner và Reader | 2–3 ngày làm việc |
| 2 UI khác | `mobile-app-builder` | `code-reviewer` | 1 ngày |
| 3 ViewModel | `mobile-app-builder` | `code-reviewer`, `tester` (unit test cho logic tách) | 2–3 ngày |
| 4 Data/engine | `mobile-app-builder`, `backend-architect` cho cách chia engine | `code-reviewer`, `appsec-engineer` (transfer, HWP sandbox) | 3–4 ngày |

Phiên điều phối chính viết brief, xác minh diff, chạy gate và giữ git.

## 10. Rủi ro và cách lùi

- **Composable tách ra làm đổi recomposition hoặc mất `remember`:** khi tách, giữ `remember` ở đúng scope cũ; sau mỗi màn hình, so sánh trên thiết bị thật.
- **Camera hoặc WebView lifecycle bị đổi khi chuyển `DisposableEffect`:** chuyển nguyên khối, không sửa logic; kiểm thử tay theo danh sách ở bước 9 và 10 của §3.
- **Cách lùi:** mỗi commit độc lập và revert được bằng `git revert`, không có migration dữ liệu nào.
