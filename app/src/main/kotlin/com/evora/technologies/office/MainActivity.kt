package com.evora.technologies.office

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import java.io.File
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.evora.technologies.office.core.designsystem.component.InlineNativeAdSlotRenderer
import com.evora.technologies.office.core.designsystem.component.LocalInlineNativeAdRenderer
import com.evora.technologies.office.core.designsystem.component.ProvideReducedMotion
import com.evora.technologies.office.core.designsystem.theme.EvoraTheme
import com.evora.technologies.office.core.domain.port.AdPlacementHook
import com.evora.technologies.office.core.domain.port.ConsentPort
import com.evora.technologies.office.core.domain.port.PreferencesRepository
import com.evora.technologies.office.core.domain.port.RemoteConfigPort
import com.evora.technologies.office.core.model.AppThemeMode
import com.evora.technologies.office.navigation.AppNavHost
import com.evora.technologies.office.navigation.IncomingLaunchAction
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var adPlacementHook: AdPlacementHook

    @Inject
    lateinit var inlineNativeAdRenderer: InlineNativeAdSlotRenderer

    @Inject
    lateinit var remoteConfigPort: RemoteConfigPort

    @Inject
    lateinit var consentPort: ConsentPort

    private var incomingActionState by mutableStateOf<IncomingLaunchAction?>(null)
    private var startupConsentTriggered = false

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            incomingActionState = extractIncomingAction(intent)
            if (incomingActionState is IncomingLaunchAction.ViewDocument) {
                adPlacementHook.markExternalIntentLaunch(true)
            }
        }
        if (incomingActionState !is IncomingLaunchAction.ViewDocument && !startupConsentTriggered) {
            startupConsentTriggered = true
            // Run Remote Config + UMP consent asynchronously so cold start stays < 1.2s (T-025)
            // while still gating MobileAds initialization strictly behind ConsentPort.consentState (US-002).
            lifecycleScope.launch {
                remoteConfigPort.fetchAndActivate(timeoutMs = 1_500L)
                consentPort.gatherConsentIfNeeded()
            }
        }

        setContent {
            val themeMode by preferencesRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = AppThemeMode.SYSTEM)
            val isDark = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }
            val windowSizeClass = calculateWindowSizeClass(this)

            EvoraTheme(darkTheme = isDark) {
                CompositionLocalProvider(
                    LocalInlineNativeAdRenderer provides inlineNativeAdRenderer,
                ) {
                    ProvideReducedMotion {
                        AppNavHost(
                            windowWidthSizeClass = windowSizeClass.widthSizeClass,
                            incomingAction = incomingActionState,
                            onIncomingActionConsumed = { incomingActionState = null },
                            onRouteChanged = { route, isProtected ->
                                adPlacementHook.setProtectedSurfaceActive(isProtected)
                                if (route == "main") {
                                    adPlacementHook.onHomeHubEntered()
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractIncomingAction(intent)?.let {
            if (it is IncomingLaunchAction.ViewDocument) {
                adPlacementHook.markExternalIntentLaunch(true)
            }
            incomingActionState = it
        }
    }

    private fun extractIncomingAction(intent: Intent?): IncomingLaunchAction? {
        if (intent == null) return null
        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data!!
            if (isForbiddenPrivateUri(uri, allowExportDirs = false)) return null
            return IncomingLaunchAction.ViewDocument(
                uriString = uri.toString(),
                displayNameHint = uri.lastPathSegment,
                mimeTypeHint = intent.type ?: contentResolver.getType(uri),
            )
        }
        return when (intent.action) {
            "com.evora.technologies.office.ACTION_SCAN" -> IncomingLaunchAction.OpenScanner("DOCUMENT")
            "com.evora.technologies.office.ACTION_SIGN" -> IncomingLaunchAction.OpenPdfSignTool
            "com.evora.technologies.office.ACTION_OCR" -> IncomingLaunchAction.OpenScanner("OCR")
            "com.evora.technologies.office.ACTION_WIDGET_SEARCH" -> IncomingLaunchAction.OpenSearch
            "com.evora.technologies.office.ACTION_WIDGET_NEW_DOC" -> IncomingLaunchAction.OpenNewTemplate
            "com.evora.technologies.office.ACTION_WIDGET_OPEN_DOC" -> {
                val uri = intent.getStringExtra("extra_doc_uri")?.takeIf { it.isNotBlank() } ?: return null
                val parsed = runCatching { Uri.parse(uri) }.getOrNull() ?: return null
                if (parsed.scheme !in setOf("file", "content")) return null
                if (isForbiddenPrivateUri(parsed, allowExportDirs = true)) return null
                val name = intent.getStringExtra("extra_doc_name") ?: "document"
                val fmt = intent.getStringExtra("extra_doc_format") ?: "PDF"
                IncomingLaunchAction.OpenWidgetDocument(uri, name, fmt)
            }
            else -> null
        }
    }

    private fun isForbiddenPrivateUri(uri: Uri, allowExportDirs: Boolean): Boolean {
        if (uri.scheme != "file") return false
        val path = uri.path ?: return true
        val canonical = runCatching { File(path).canonicalPath }.getOrNull() ?: return true
        val sensitiveRoots = if (allowExportDirs) {
            listOfNotNull(
                runCatching { File(filesDir, "cloud_vault").canonicalPath }.getOrNull(),
                runCatching { File(filesDir, "datastore").canonicalPath }.getOrNull(),
                runCatching { File(dataDir, "databases").canonicalPath }.getOrNull(),
                runCatching { File(dataDir, "shared_prefs").canonicalPath }.getOrNull(),
                runCatching { cacheDir.canonicalPath }.getOrNull(),
                runCatching { codeCacheDir.canonicalPath }.getOrNull(),
                runCatching { noBackupFilesDir.canonicalPath }.getOrNull(),
            )
        } else {
            listOfNotNull(
                runCatching { dataDir.canonicalPath }.getOrNull(),
                runCatching { filesDir.canonicalPath }.getOrNull(),
                runCatching { cacheDir.canonicalPath }.getOrNull(),
                runCatching { codeCacheDir.canonicalPath }.getOrNull(),
                runCatching { noBackupFilesDir.canonicalPath }.getOrNull(),
            )
        }
        return sensitiveRoots.any { root -> canonical == root || canonical.startsWith(root + File.separator) }
    }
}
