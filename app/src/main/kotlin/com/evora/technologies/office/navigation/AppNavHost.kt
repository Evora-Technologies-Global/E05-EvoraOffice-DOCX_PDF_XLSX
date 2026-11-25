package com.evora.technologies.office.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
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
import kotlinx.coroutines.launch

sealed interface IncomingLaunchAction {
    data class ViewDocument(
        val uriString: String,
        val displayNameHint: String?,
        val mimeTypeHint: String?,
    ) : IncomingLaunchAction

    data class ToolShortcut(val message: String) : IncomingLaunchAction
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

    fun paywall(fromOnboarding: Boolean): String = "paywall?onboarding=$fromOnboarding"

    fun reader(uriString: String, displayName: String, format: DocFormat): String =
        "reader?uri=${Uri.encode(uriString)}&name=${Uri.encode(displayName)}&format=${Uri.encode(format.name)}"

    fun archive(uriString: String, displayName: String): String =
        "archive?uri=${Uri.encode(uriString)}&name=${Uri.encode(displayName)}"
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
                // Remove SPLASH from back stack so Back exits cleanly instead of returning to frozen Splash.
                popUpTo(AppRoutes.SPLASH) { inclusive = true }
            }
            launchSingleTop = true
        }
    }

    // Handle external ACTION_VIEW or launcher alias intents arriving while singleTop MainActivity is already running
    LaunchedEffect(incomingAction) {
        when (incomingAction) {
            is IncomingLaunchAction.ViewDocument -> {
                if (navController.currentDestination?.route != AppRoutes.SPLASH) {
                    navController.navigate(AppRoutes.SPLASH) {
                        launchSingleTop = true
                    }
                }
            }
            is IncomingLaunchAction.ToolShortcut -> {
                onIncomingActionConsumed()
                if (navController.currentDestination?.route == AppRoutes.SPLASH) {
                    navController.navigate(AppRoutes.MAIN) {
                        popUpTo(AppRoutes.SPLASH) { inclusive = true }
                    }
                }
                showMessage(incomingAction.message)
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
                    onOpenArchiveInspector = { doc ->
                        navController.navigate(AppRoutes.archive(doc.uriString, doc.displayName))
                    },
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
        }
    }
}

@Composable
private fun MainShell(
    windowWidthSizeClass: WindowWidthSizeClass,
    onOpenSearch: () -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenDocument: (DocumentItem) -> Unit,
    onOpenArchiveInspector: (DocumentItem) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    val isMediumOrExpanded = windowWidthSizeClass != WindowWidthSizeClass.Compact
    val isExpandedTwoPane = windowWidthSizeClass == WindowWidthSizeClass.Expanded

    val onScanClicked = {
        onShowMessage("Document Scanner activates in Sprint 2 (T-014)")
    }

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
                        onOpenArchiveInspector = onOpenArchiveInspector,
                        onShowMessage = onShowMessage,
                    )
                    MainTab.RECENT -> RecentBookmarksRoute(
                        onOpenDocument = onOpenDocument,
                        onOpenArchiveInspector = onOpenArchiveInspector,
                        onShowMessage = onShowMessage,
                    )
                    MainTab.STORAGE -> EmptyStateView(
                        title = "Storage Explorer (Sprint 3)",
                        description = "Folder hierarchy and batch manager land in T-018. Use Home -> Search or + Picker now.",
                        icon = Icons.Outlined.FolderOpen,
                        primaryActionLabel = "Go to Home Library",
                        onPrimaryAction = { selectedTab = MainTab.HOME },
                    )
                    MainTab.TOOLS -> EmptyStateView(
                        title = "PDF Power Tools (Sprint 2)",
                        description = "Merge, Split, Compress, Watermark, e-Sign & OCR land in T-010..T-014.",
                        icon = Icons.Outlined.Build,
                        primaryActionLabel = "Go to Home Library",
                        onPrimaryAction = { selectedTab = MainTab.HOME },
                    )
                }
            }
        }
    }
}
