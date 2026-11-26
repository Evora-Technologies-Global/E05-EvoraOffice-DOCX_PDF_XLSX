package com.evora.technologies.office.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.evora.technologies.office.core.designsystem.component.MainBottomBar
import com.evora.technologies.office.core.designsystem.component.MainNavigationRail
import com.evora.technologies.office.core.designsystem.component.MainTab
import com.evora.technologies.office.core.model.CloudProvider
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.ExternalIntentPayload
import com.evora.technologies.office.core.model.OfficeKind
import com.evora.technologies.office.core.model.PdfToolKind
import com.evora.technologies.office.core.model.ScanMode
import com.evora.technologies.office.core.model.SignatureItem
import com.evora.technologies.office.feature.cloud.browser.CloudDriveBrowserRoute
import com.evora.technologies.office.feature.editor.office.OfficeEditorRoute
import com.evora.technologies.office.feature.editor.pdf.PdfEditorRoute
import com.evora.technologies.office.feature.editor.signature.SignaturePadRoute
import com.evora.technologies.office.feature.editor.slideshow.SlideShowRoute
import com.evora.technologies.office.feature.library.home.HomeDocumentsRoute
import com.evora.technologies.office.feature.library.recent.RecentBookmarksRoute
import com.evora.technologies.office.feature.library.search.SearchDocumentsRoute
import com.evora.technologies.office.feature.library.storage.StorageBrowserRoute
import com.evora.technologies.office.feature.monetization.paywall.PaywallRoute
import com.evora.technologies.office.feature.onboarding.intro.OnboardingIntroRoute
import com.evora.technologies.office.feature.onboarding.language.LanguageSelectRoute
import com.evora.technologies.office.feature.onboarding.permission.StoragePermissionRoute
import com.evora.technologies.office.feature.onboarding.splash.SplashRoute
import com.evora.technologies.office.feature.reader.archive.ArchiveInspectorRoute
import com.evora.technologies.office.feature.reader.viewer.DocumentReaderRoute
import com.evora.technologies.office.feature.scanner.camera.ScannerFlowRoute
import com.evora.technologies.office.feature.settings.SettingsRoute
import com.evora.technologies.office.feature.tools.hub.ToolsHubRoute
import com.evora.technologies.office.feature.tools.pdf.PdfToolConfigRoute
import com.evora.technologies.office.feature.transfer.wifi.WifiTransferRoute
import kotlinx.coroutines.launch

sealed interface IncomingLaunchAction {
    data class ViewDocument(
        val uriString: String,
        val displayNameHint: String?,
        val mimeTypeHint: String?,
    ) : IncomingLaunchAction

    data class OpenScanner(val modeName: String) : IncomingLaunchAction
    data object OpenPdfSignTool : IncomingLaunchAction
    data object OpenSearch : IncomingLaunchAction
    data object OpenNewTemplate : IncomingLaunchAction
    data class OpenWidgetDocument(
        val uriString: String,
        val displayName: String,
        val formatName: String,
    ) : IncomingLaunchAction
}

private object AppRoutes {
    const val SPLASH = "splash"
    const val LANGUAGE_SELECT = "language_select"
    const val ONBOARDING_INTRO = "onboarding_intro"
    const val PAYWALL = "paywall?onboarding={onboarding}"
    const val STORAGE_PERMISSION = "storage_permission"
    const val MAIN = "main"
    const val SEARCH = "search"
    const val READER = "reader?uri={uri}&name={name}&format={format}"
    const val ARCHIVE = "archive?uri={uri}&name={name}"
    const val PDF_TOOL = "pdf_tool?kind={kind}&uri={uri}"
    const val SCANNER = "scanner?mode={mode}"
    const val PDF_EDITOR = "pdf_editor?uri={uri}&name={name}"
    const val SIGNATURE_PAD = "signature_pad"
    const val OFFICE_EDITOR = "office_editor?uri={uri}&name={name}&kind={kind}"
    const val SLIDE_SHOW = "slide_show?uri={uri}&start={start}"
    const val CLOUD = "cloud?provider={provider}"
    const val WIFI_TRANSFER = "wifi_transfer"
    const val SETTINGS = "settings"

    fun paywall(fromOnboarding: Boolean): String = "paywall?onboarding=$fromOnboarding"

    fun reader(uriString: String, displayName: String, format: DocFormat): String =
        "reader?uri=${Uri.encode(uriString)}&name=${Uri.encode(displayName)}&format=${Uri.encode(format.name)}"

    fun archive(uriString: String, displayName: String): String =
        "archive?uri=${Uri.encode(uriString)}&name=${Uri.encode(displayName)}"

    fun pdfTool(kind: PdfToolKind, uriString: String = ""): String =
        "pdf_tool?kind=${Uri.encode(kind.name)}&uri=${Uri.encode(uriString)}"

    fun scanner(mode: ScanMode): String = "scanner?mode=${Uri.encode(mode.name)}"

    fun pdfEditor(uriString: String, displayName: String): String =
        "pdf_editor?uri=${Uri.encode(uriString)}&name=${Uri.encode(displayName)}"

    fun officeEditor(uriString: String, displayName: String, kind: OfficeKind): String =
        "office_editor?uri=${Uri.encode(uriString)}&name=${Uri.encode(displayName)}&kind=${Uri.encode(kind.name)}"

    fun slideShow(uriString: String, startIndex: Int): String =
        "slide_show?uri=${Uri.encode(uriString)}&start=$startIndex"

    fun cloud(provider: CloudProvider): String = "cloud?provider=${Uri.encode(provider.name)}"
}

@Composable
fun AppNavHost(
    windowWidthSizeClass: WindowWidthSizeClass,
    incomingAction: IncomingLaunchAction?,
    onIncomingActionConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    onRouteChanged: (route: String?, isProtectedSurface: Boolean) -> Unit = { _, _ -> },
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingSignatureId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingSignaturePath by rememberSaveable { mutableStateOf<String?>(null) }
    var openTemplatesTrigger by rememberSaveable { mutableIntStateOf(0) }
    var deferredDeepLink by remember { mutableStateOf<IncomingLaunchAction?>(null) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    LaunchedEffect(currentRoute) {
        val protected = currentRoute in setOf(
            AppRoutes.OFFICE_EDITOR,
            AppRoutes.PDF_EDITOR,
            AppRoutes.SIGNATURE_PAD,
            AppRoutes.SLIDE_SHOW,
            AppRoutes.SCANNER,
        )
        onRouteChanged(currentRoute, protected)
    }

    val showMessage: (String) -> Unit = remember(snackbarHostState) {
        { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
        }
    }

    val openTargetDocument: (DocumentItem) -> Unit = { doc ->
        if (doc.format == DocFormat.ARCHIVE) {
            navController.navigate(AppRoutes.archive(doc.uriString, doc.displayName))
        } else {
            navController.navigate(AppRoutes.reader(doc.uriString, doc.displayName, doc.format))
        }
    }

    val openEditorForDoc: (String, String, DocFormat) -> Unit = { uriString, displayName, format ->
        when (format) {
            DocFormat.PDF -> navController.navigate(AppRoutes.pdfEditor(uriString, displayName))
            DocFormat.WORD -> navController.navigate(AppRoutes.officeEditor(uriString, displayName, OfficeKind.WORD))
            DocFormat.EXCEL -> navController.navigate(AppRoutes.officeEditor(uriString, displayName, OfficeKind.EXCEL))
            DocFormat.PPT -> navController.navigate(AppRoutes.officeEditor(uriString, displayName, OfficeKind.PPT))
            DocFormat.ODF -> {
                val ext = displayName.substringAfterLast('.', "").lowercase()
                val kind = when (ext) {
                    "ods" -> OfficeKind.EXCEL
                    "odp" -> OfficeKind.PPT
                    else -> OfficeKind.WORD
                }
                navController.navigate(AppRoutes.officeEditor(uriString, displayName, kind))
            }
            else -> navController.navigate(AppRoutes.reader(uriString, displayName, format))
        }
    }

    val openExternalPayload: (ExternalIntentPayload) -> Unit = { payload ->
        onIncomingActionConsumed()
        val target = if (payload.format == DocFormat.ARCHIVE) {
            AppRoutes.archive(payload.uriString, payload.displayName)
        } else {
            AppRoutes.reader(payload.uriString, payload.displayName, payload.format)
        }
        val wasOnSplash = navController.currentDestination?.route == AppRoutes.SPLASH
        navController.navigate(target) {
            if (wasOnSplash) {
                popUpTo(AppRoutes.SPLASH) { inclusive = true }
            }
            launchSingleTop = true
        }
    }

    val dispatchSecondaryAction: (IncomingLaunchAction) -> Unit = { action ->
        when (action) {
            is IncomingLaunchAction.OpenScanner -> {
                val mode = runCatching { ScanMode.valueOf(action.modeName) }.getOrDefault(ScanMode.DOCUMENT)
                navController.navigate(AppRoutes.scanner(mode)) { launchSingleTop = true }
            }
            IncomingLaunchAction.OpenPdfSignTool -> {
                navController.navigate(AppRoutes.pdfTool(PdfToolKind.PKCS7_SIGN)) { launchSingleTop = true }
            }
            IncomingLaunchAction.OpenSearch -> {
                navController.navigate(AppRoutes.SEARCH) { launchSingleTop = true }
            }
            IncomingLaunchAction.OpenNewTemplate -> {
                openTemplatesTrigger++
                if (navController.currentDestination?.route != AppRoutes.MAIN) {
                    navController.navigate(AppRoutes.MAIN) { launchSingleTop = true }
                }
            }
            is IncomingLaunchAction.OpenWidgetDocument -> {
                val fmt = runCatching { DocFormat.valueOf(action.formatName) }.getOrDefault(DocFormat.PDF)
                val target = if (fmt == DocFormat.ARCHIVE) {
                    AppRoutes.archive(action.uriString, action.displayName)
                } else {
                    AppRoutes.reader(action.uriString, action.displayName, fmt)
                }
                navController.navigate(target) { launchSingleTop = true }
            }
            is IncomingLaunchAction.ViewDocument -> Unit
        }
    }

    LaunchedEffect(incomingAction) {
        when (incomingAction) {
            is IncomingLaunchAction.ViewDocument -> {
                if (navController.currentDestination?.route != AppRoutes.SPLASH) {
                    navController.navigate(AppRoutes.SPLASH) { launchSingleTop = true }
                }
            }
            is IncomingLaunchAction.OpenScanner,
            IncomingLaunchAction.OpenPdfSignTool,
            IncomingLaunchAction.OpenSearch,
            IncomingLaunchAction.OpenNewTemplate,
            is IncomingLaunchAction.OpenWidgetDocument,
            -> {
                onIncomingActionConsumed()
                if (navController.currentDestination?.route == AppRoutes.SPLASH) {
                    deferredDeepLink = incomingAction
                } else {
                    dispatchSecondaryAction(incomingAction)
                }
            }
            null -> Unit
        }
    }

    val viewIntent = incomingAction as? IncomingLaunchAction.ViewDocument

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { rootPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoutes.SPLASH,
            modifier = Modifier
                .fillMaxSize()
                .padding(rootPadding),
        ) {
            composable(AppRoutes.SPLASH) {
                SplashRoute(
                    externalUri = viewIntent?.uriString,
                    displayNameHint = viewIntent?.displayNameHint,
                    mimeTypeHint = viewIntent?.mimeTypeHint,
                    onNavigateLanguageSelect = {
                        navController.navigate(AppRoutes.LANGUAGE_SELECT) {
                            popUpTo(AppRoutes.SPLASH) { inclusive = true }
                        }
                    },
                    onNavigateIntro = {
                        navController.navigate(AppRoutes.ONBOARDING_INTRO) {
                            popUpTo(AppRoutes.SPLASH) { inclusive = true }
                        }
                    },
                    onNavigateHome = {
                        onIncomingActionConsumed()
                        navController.navigate(AppRoutes.MAIN) {
                            popUpTo(AppRoutes.SPLASH) { inclusive = true }
                        }
                        deferredDeepLink?.let { pending ->
                            deferredDeepLink = null
                            dispatchSecondaryAction(pending)
                        }
                    },
                    onNavigateExternalDocument = openExternalPayload,
                    onShowError = showMessage,
                )
            }

            composable(AppRoutes.LANGUAGE_SELECT) {
                LanguageSelectRoute(
                    onNavigateIntro = { navController.navigate(AppRoutes.ONBOARDING_INTRO) },
                )
            }

            composable(AppRoutes.ONBOARDING_INTRO) {
                OnboardingIntroRoute(
                    onNavigateNext = { navController.navigate(AppRoutes.paywall(fromOnboarding = true)) },
                    onNavigateSkipPaywall = { navController.navigate(AppRoutes.STORAGE_PERMISSION) },
                )
            }

            composable(
                route = AppRoutes.PAYWALL,
                arguments = listOf(navArgument("onboarding") { type = NavType.BoolType; defaultValue = false }),
            ) { backStackEntry ->
                val fromOnboarding = backStackEntry.arguments?.getBoolean("onboarding") ?: false
                PaywallRoute(
                    onClose = {
                        if (fromOnboarding) {
                            navController.navigate(AppRoutes.STORAGE_PERMISSION) {
                                popUpTo(AppRoutes.PAYWALL) { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                    onShowMessage = showMessage,
                )
            }

            composable(AppRoutes.STORAGE_PERMISSION) {
                StoragePermissionRoute(
                    onNavigateHome = {
                        navController.navigate(AppRoutes.MAIN) {
                            popUpTo(AppRoutes.LANGUAGE_SELECT) { inclusive = true }
                        }
                        deferredDeepLink?.let { pending ->
                            deferredDeepLink = null
                            dispatchSecondaryAction(pending)
                        }
                    },
                    onShowMessage = showMessage,
                )
            }

            composable(AppRoutes.MAIN) {
                MainShell(
                    windowWidthSizeClass = windowWidthSizeClass,
                    openTemplatesTrigger = openTemplatesTrigger,
                    onOpenSearch = { navController.navigate(AppRoutes.SEARCH) },
                    onOpenPaywall = { navController.navigate(AppRoutes.paywall(fromOnboarding = false)) },
                    onOpenDocument = openTargetDocument,
                    onOpenEditor = { doc -> openEditorForDoc(doc.uriString, doc.displayName, doc.format) },
                    onConvertToPdf = { doc ->
                        navController.navigate(AppRoutes.pdfTool(PdfToolKind.DOC_TO_PDF, doc.uriString))
                    },
                    onOpenArchiveInspector = { doc ->
                        navController.navigate(AppRoutes.archive(doc.uriString, doc.displayName))
                    },
                    onOpenArchiveRaw = { uri, name ->
                        navController.navigate(AppRoutes.archive(uri, name))
                    },
                    onOpenCloudDrive = { provider ->
                        navController.navigate(AppRoutes.cloud(provider))
                    },
                    onOpenPdfTool = { kind -> navController.navigate(AppRoutes.pdfTool(kind)) },
                    onMergePdfUris = { uris ->
                        navController.navigate(AppRoutes.pdfTool(PdfToolKind.MERGE, uris.joinToString("\n")))
                    },
                    onOpenScanner = { mode -> navController.navigate(AppRoutes.scanner(mode)) },
                    onOpenSettings = { navController.navigate(AppRoutes.SETTINGS) },
                    onOpenWifiTransfer = { navController.navigate(AppRoutes.WIFI_TRANSFER) },
                    onShowMessage = showMessage,
                )
            }

            composable(AppRoutes.SEARCH) {
                SearchDocumentsRoute(
                    onBack = { navController.popBackStack() },
                    onOpenDocument = openTargetDocument,
                    onOpenArchiveInspector = { doc ->
                        navController.navigate(AppRoutes.archive(doc.uriString, doc.displayName))
                    },
                )
            }

            composable(
                route = AppRoutes.READER,
                arguments = listOf(
                    navArgument("uri") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType },
                    navArgument("format") { type = NavType.StringType },
                ),
            ) { entry ->
                val uri = entry.arguments?.getString("uri").orEmpty()
                val name = entry.arguments?.getString("name").orEmpty()
                val fmtName = entry.arguments?.getString("format").orEmpty()
                val format = runCatching { DocFormat.valueOf(fmtName) }.getOrDefault(DocFormat.PDF)
                DocumentReaderRoute(
                    uriString = uri,
                    displayName = name,
                    format = format,
                    onBack = { navController.popBackStack() },
                    onOpenEditor = openEditorForDoc,
                    onShowMessage = showMessage,
                )
            }

            composable(
                route = AppRoutes.ARCHIVE,
                arguments = listOf(
                    navArgument("uri") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType },
                ),
            ) { entry ->
                val uri = entry.arguments?.getString("uri").orEmpty()
                val name = entry.arguments?.getString("name").orEmpty()
                ArchiveInspectorRoute(
                    uriString = uri,
                    displayName = name,
                    onBack = { navController.popBackStack() },
                    onOpenExtractedDocument = { fileUri, fileName ->
                        val ext = fileName.substringAfterLast('.', "").lowercase()
                        val fmt = DocFormat.fromExtension(ext) ?: DocFormat.TEXT
                        if (fmt == DocFormat.ARCHIVE) {
                            navController.navigate(AppRoutes.archive(fileUri, fileName))
                        } else {
                            navController.navigate(AppRoutes.reader(fileUri, fileName, fmt))
                        }
                    },
                    onShowMessage = showMessage,
                )
            }

            composable(
                route = AppRoutes.PDF_TOOL,
                arguments = listOf(
                    navArgument("kind") { type = NavType.StringType },
                    navArgument("uri") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                val kindName = entry.arguments?.getString("kind").orEmpty()
                val preUri = entry.arguments?.getString("uri")?.takeIf { it.isNotBlank() }
                val kind = runCatching { PdfToolKind.valueOf(kindName) }.getOrDefault(PdfToolKind.MERGE)
                PdfToolConfigRoute(
                    toolKind = kind,
                    preselectedUri = preUri,
                    onBack = { navController.popBackStack() },
                    onOpenPaywall = { navController.navigate(AppRoutes.paywall(fromOnboarding = false)) },
                    onOpenOutput = { outUri, outName, fmt ->
                        if (fmt in setOf(DocFormat.WORD, DocFormat.EXCEL, DocFormat.PPT)) {
                            openEditorForDoc(outUri, outName, fmt)
                        } else {
                            navController.navigate(AppRoutes.reader(outUri, outName, fmt))
                        }
                    },
                    onShowMessage = showMessage,
                )
            }

            composable(
                route = AppRoutes.SCANNER,
                arguments = listOf(navArgument("mode") { type = NavType.StringType; defaultValue = ScanMode.DOCUMENT.name }),
            ) { entry ->
                val modeName = entry.arguments?.getString("mode").orEmpty()
                val mode = runCatching { ScanMode.valueOf(modeName) }.getOrDefault(ScanMode.DOCUMENT)
                ScannerFlowRoute(
                    initialMode = mode,
                    onBack = { navController.popBackStack() },
                    onOpenPaywall = { navController.navigate(AppRoutes.paywall(fromOnboarding = false)) },
                    onOpenDocument = { outUri, outName, fmt ->
                        if (fmt == DocFormat.WORD) {
                            openEditorForDoc(outUri, outName, fmt)
                        } else {
                            navController.navigate(AppRoutes.reader(outUri, outName, fmt))
                        }
                    },
                    onShowMessage = showMessage,
                )
            }

            composable(
                route = AppRoutes.PDF_EDITOR,
                arguments = listOf(
                    navArgument("uri") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType },
                ),
            ) { entry ->
                val uri = entry.arguments?.getString("uri").orEmpty()
                val name = entry.arguments?.getString("name").orEmpty()
                val createdSig = if (pendingSignatureId != null && pendingSignaturePath != null) {
                    SignatureItem(pendingSignatureId!!, pendingSignaturePath!!, 0L)
                } else {
                    null
                }
                PdfEditorRoute(
                    uriString = uri,
                    displayName = name,
                    newlyCreatedSignature = createdSig,
                    onNewlyCreatedSignatureConsumed = {
                        pendingSignatureId = null
                        pendingSignaturePath = null
                    },
                    onOpenSignaturePad = { navController.navigate(AppRoutes.SIGNATURE_PAD) },
                    onOpenPaywall = { navController.navigate(AppRoutes.paywall(fromOnboarding = false)) },
                    onBack = { navController.popBackStack() },
                    onShowMessage = showMessage,
                )
            }

            composable(AppRoutes.SIGNATURE_PAD) {
                SignaturePadRoute(
                    onSignatureSelected = { item ->
                        pendingSignatureId = item.id
                        pendingSignaturePath = item.assetPath
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() },
                    onShowMessage = showMessage,
                )
            }

            composable(
                route = AppRoutes.OFFICE_EDITOR,
                arguments = listOf(
                    navArgument("uri") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType },
                    navArgument("kind") { type = NavType.StringType },
                ),
            ) { entry ->
                val uri = entry.arguments?.getString("uri").orEmpty()
                val name = entry.arguments?.getString("name").orEmpty()
                val kindName = entry.arguments?.getString("kind").orEmpty()
                val kind = runCatching { OfficeKind.valueOf(kindName) }.getOrDefault(OfficeKind.WORD)
                OfficeEditorRoute(
                    uriString = uri,
                    displayName = name,
                    kind = kind,
                    onBack = { navController.popBackStack() },
                    onLaunchSlideShow = { deckUri, _, startIdx ->
                        navController.navigate(AppRoutes.slideShow(deckUri, startIdx))
                    },
                    onShowMessage = showMessage,
                )
            }

            composable(
                route = AppRoutes.SLIDE_SHOW,
                arguments = listOf(
                    navArgument("uri") { type = NavType.StringType },
                    navArgument("start") { type = NavType.IntType; defaultValue = 0 },
                ),
            ) { entry ->
                val uri = entry.arguments?.getString("uri").orEmpty()
                val start = entry.arguments?.getInt("start") ?: 0
                SlideShowRoute(
                    uriString = uri,
                    startSlideIndex = start,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = AppRoutes.CLOUD,
                arguments = listOf(
                    navArgument("provider") { type = NavType.StringType; defaultValue = CloudProvider.GOOGLE_DRIVE.name },
                ),
            ) { entry ->
                val rawProv = entry.arguments?.getString("provider").orEmpty()
                val provider = runCatching { CloudProvider.valueOf(rawProv) }.getOrDefault(CloudProvider.GOOGLE_DRIVE)
                CloudDriveBrowserRoute(
                    initialProvider = provider,
                    onBack = { navController.popBackStack() },
                    onOpenDownloadedDocument = openTargetDocument,
                    onShowMessage = showMessage,
                )
            }

            composable(AppRoutes.WIFI_TRANSFER) {
                WifiTransferRoute(
                    onBack = { navController.popBackStack() },
                    onShowMessage = showMessage,
                )
            }

            composable(AppRoutes.SETTINGS) {
                SettingsRoute(
                    onBack = { navController.popBackStack() },
                    onOpenPaywall = { navController.navigate(AppRoutes.paywall(fromOnboarding = false)) },
                    onOpenCloudDrives = { navController.navigate(AppRoutes.cloud(CloudProvider.GOOGLE_DRIVE)) },
                    onShowMessage = showMessage,
                )
            }
        }
    }
}

@Composable
private fun MainShell(
    windowWidthSizeClass: WindowWidthSizeClass,
    openTemplatesTrigger: Int,
    onOpenSearch: () -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenDocument: (DocumentItem) -> Unit,
    onOpenEditor: (DocumentItem) -> Unit,
    onConvertToPdf: (DocumentItem) -> Unit,
    onOpenArchiveInspector: (DocumentItem) -> Unit,
    onOpenArchiveRaw: (uriString: String, displayName: String) -> Unit,
    onOpenCloudDrive: (CloudProvider) -> Unit,
    onOpenPdfTool: (PdfToolKind) -> Unit,
    onMergePdfUris: (List<String>) -> Unit,
    onOpenScanner: (ScanMode) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWifiTransfer: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    LaunchedEffect(openTemplatesTrigger) {
        if (openTemplatesTrigger > 0) {
            selectedTab = MainTab.HOME
        }
    }
    val isMediumOrExpanded = windowWidthSizeClass != WindowWidthSizeClass.Compact
    val isExpandedTwoPane = windowWidthSizeClass == WindowWidthSizeClass.Expanded
    val toolColumns = when (windowWidthSizeClass) {
        WindowWidthSizeClass.Expanded -> 6
        WindowWidthSizeClass.Medium -> 4
        else -> 3
    }

    val onScanClicked = { onOpenScanner(ScanMode.DOCUMENT) }

    Scaffold(
        bottomBar = {
            if (!isMediumOrExpanded) {
                MainBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onScanClick = onScanClicked,
                )
            }
        },
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (isMediumOrExpanded) {
                MainNavigationRail(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onScanClick = onScanClicked,
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                when (selectedTab) {
                    MainTab.HOME -> HomeDocumentsRoute(
                        isMediumOrExpanded = isMediumOrExpanded,
                        isExpandedTwoPane = isExpandedTwoPane,
                        onOpenSearch = onOpenSearch,
                        onOpenPaywall = onOpenPaywall,
                        onOpenDocument = onOpenDocument,
                        onOpenEditor = onOpenEditor,
                        onConvertDocumentToPdf = onConvertToPdf,
                        onOpenArchiveInspector = onOpenArchiveInspector,
                        openTemplatesTrigger = openTemplatesTrigger,
                        onShowMessage = onShowMessage,
                    )
                    MainTab.RECENT -> RecentBookmarksRoute(
                        onOpenDocument = onOpenDocument,
                        onOpenArchiveInspector = onOpenArchiveInspector,
                        onShowMessage = { onShowMessage(it) },
                    )
                    MainTab.STORAGE -> StorageBrowserRoute(
                        isExpandedTwoPane = isExpandedTwoPane,
                        onOpenDocument = onOpenDocument,
                        onOpenArchiveInspector = onOpenArchiveRaw,
                        onOpenCloudDrive = onOpenCloudDrive,
                        onMergePdfs = onMergePdfUris,
                        onShowMessage = onShowMessage,
                    )
                    MainTab.TOOLS -> ToolsHubRoute(
                        columns = toolColumns,
                        onNavigateToPdfTool = onOpenPdfTool,
                        onNavigateToScannerMode = { modeName ->
                            val mode = runCatching { ScanMode.valueOf(modeName) }.getOrDefault(ScanMode.OCR)
                            onOpenScanner(mode)
                        },
                        onNavigateToPaywall = onOpenPaywall,
                        onOpenCreatedOfficeDoc = onOpenEditor,
                        onNavigateToSettings = onOpenSettings,
                        onNavigateToWifiTransfer = onOpenWifiTransfer,
                        onShowMessage = onShowMessage,
                    )
                }
            }
        }
    }
}
