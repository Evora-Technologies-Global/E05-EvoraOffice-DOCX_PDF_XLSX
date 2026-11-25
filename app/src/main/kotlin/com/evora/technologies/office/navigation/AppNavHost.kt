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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.evora.technologies.office.core.designsystem.component.EmptyStateView
import com.evora.technologies.office.core.designsystem.component.MainBottomBar
import com.evora.technologies.office.core.designsystem.component.MainNavigationRail
import com.evora.technologies.office.core.designsystem.component.MainTab
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.ExternalIntentPayload
import com.evora.technologies.office.core.model.OfficeKind
import com.evora.technologies.office.core.model.PdfToolKind
import com.evora.technologies.office.core.model.ScanMode
import com.evora.technologies.office.core.model.SignatureItem
import com.evora.technologies.office.feature.editor.office.OfficeEditorRoute
import com.evora.technologies.office.feature.editor.pdf.PdfEditorRoute
import com.evora.technologies.office.feature.editor.signature.SignaturePadRoute
import com.evora.technologies.office.feature.editor.slideshow.SlideShowRoute
import com.evora.technologies.office.feature.library.home.HomeDocumentsRoute
import com.evora.technologies.office.feature.library.recent.RecentBookmarksRoute
import com.evora.technologies.office.feature.library.search.SearchDocumentsRoute
import com.evora.technologies.office.feature.monetization.paywall.PaywallRoute
import com.evora.technologies.office.feature.onboarding.intro.OnboardingIntroRoute
import com.evora.technologies.office.feature.onboarding.language.LanguageSelectRoute
import com.evora.technologies.office.feature.onboarding.permission.StoragePermissionRoute
import com.evora.technologies.office.feature.onboarding.splash.SplashRoute
import com.evora.technologies.office.feature.reader.archive.ArchiveInspectorRoute
import com.evora.technologies.office.feature.reader.viewer.DocumentReaderRoute
import com.evora.technologies.office.feature.scanner.camera.ScannerFlowRoute
import com.evora.technologies.office.feature.tools.hub.ToolsHubRoute
import com.evora.technologies.office.feature.tools.pdf.PdfToolConfigRoute
import kotlinx.coroutines.launch

sealed interface IncomingLaunchAction {
    data class ViewDocument(
        val uriString: String,
        val displayNameHint: String?,
        val mimeTypeHint: String?,
    ) : IncomingLaunchAction

    data class OpenScanner(val modeName: String) : IncomingLaunchAction
    data object OpenPdfSignTool : IncomingLaunchAction
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
}

@Composable
fun AppNavHost(
    windowWidthSizeClass: WindowWidthSizeClass,
    incomingAction: IncomingLaunchAction?,
    onIncomingActionConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingSignatureId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingSignaturePath by rememberSaveable { mutableStateOf<String?>(null) }

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

    LaunchedEffect(incomingAction) {
        when (incomingAction) {
            is IncomingLaunchAction.ViewDocument -> {
                if (navController.currentDestination?.route != AppRoutes.SPLASH) {
                    navController.navigate(AppRoutes.SPLASH) { launchSingleTop = true }
                }
            }
            is IncomingLaunchAction.OpenScanner -> {
                onIncomingActionConsumed()
                val mode = runCatching { ScanMode.valueOf(incomingAction.modeName) }.getOrDefault(ScanMode.DOCUMENT)
                navController.navigate(AppRoutes.scanner(mode)) { launchSingleTop = true }
            }
            IncomingLaunchAction.OpenPdfSignTool -> {
                onIncomingActionConsumed()
                navController.navigate(AppRoutes.pdfTool(PdfToolKind.PKCS7_SIGN)) { launchSingleTop = true }
            }
            null -> Unit
        }
    }

    val viewIntent = incomingAction as? IncomingLaunchAction.ViewDocument

    Scaffold(
        modifier = modifier.fillMaxSize(),
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
                    onNavigateHome = {
                        onIncomingActionConsumed()
                        navController.navigate(AppRoutes.MAIN) {
                            popUpTo(AppRoutes.SPLASH) { inclusive = true }
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
                    },
                    onShowMessage = showMessage,
                )
            }

            composable(AppRoutes.MAIN) {
                MainShell(
                    windowWidthSizeClass = windowWidthSizeClass,
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
                    onOpenPdfTool = { kind -> navController.navigate(AppRoutes.pdfTool(kind)) },
                    onOpenScanner = { mode -> navController.navigate(AppRoutes.scanner(mode)) },
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
        }
    }
}

@Composable
private fun MainShell(
    windowWidthSizeClass: WindowWidthSizeClass,
    onOpenSearch: () -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenDocument: (DocumentItem) -> Unit,
    onOpenEditor: (DocumentItem) -> Unit,
    onConvertToPdf: (DocumentItem) -> Unit,
    onOpenArchiveInspector: (DocumentItem) -> Unit,
    onOpenPdfTool: (PdfToolKind) -> Unit,
    onOpenScanner: (ScanMode) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
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
                        onShowMessage = onShowMessage,
                    )
                    MainTab.RECENT -> RecentBookmarksRoute(
                        onOpenDocument = onOpenDocument,
                        onOpenArchiveInspector = onOpenArchiveInspector,
                        onShowMessage = showMessage@{ onShowMessage(it) },
                    )
                    MainTab.STORAGE -> EmptyStateView(
                        title = "Storage Explorer (Sprint 3)",
                        description = "Folder hierarchy and batch manager land in T-018. Use Home -> Search or + Picker now.",
                        icon = Icons.Outlined.FolderOpen,
                        primaryActionLabel = "Go to Home Library",
                        onPrimaryAction = { selectedTab = MainTab.HOME },
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
                        onShowMessage = onShowMessage,
                    )
                }
            }
        }
    }
}
