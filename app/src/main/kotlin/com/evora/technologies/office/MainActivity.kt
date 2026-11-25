package com.evora.technologies.office

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.office.core.designsystem.component.ProvideReducedMotion
import com.evora.technologies.office.core.designsystem.theme.EvoraTheme
import com.evora.technologies.office.core.domain.port.PreferencesRepository
import com.evora.technologies.office.core.model.AppThemeMode
import com.evora.technologies.office.navigation.AppNavHost
import com.evora.technologies.office.navigation.IncomingLaunchAction
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private var incomingActionState by mutableStateOf<IncomingLaunchAction?>(null)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        incomingActionState = extractIncomingAction(intent)

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
                ProvideReducedMotion {
                    AppNavHost(
                        windowWidthSizeClass = windowSizeClass.widthSizeClass,
                        incomingAction = incomingActionState,
                        onIncomingActionConsumed = { incomingActionState = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractIncomingAction(intent)?.let {
            incomingActionState = it
        }
    }

    private fun extractIncomingAction(intent: Intent?): IncomingLaunchAction? {
        if (intent == null) return null
        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            return IncomingLaunchAction.ViewDocument(
                uriString = intent.data.toString(),
                displayNameHint = intent.data?.lastPathSegment,
                mimeTypeHint = intent.type ?: contentResolver.getType(intent.data!!),
            )
        }
        return when (intent.action) {
            "com.evora.technologies.office.ACTION_SCAN" -> IncomingLaunchAction.ToolShortcut("Document Scanner activates in Sprint 2 (T-014)")
            "com.evora.technologies.office.ACTION_SIGN" -> IncomingLaunchAction.ToolShortcut("PDF Signer activates in Sprint 2 (T-010)")
            "com.evora.technologies.office.ACTION_OCR" -> IncomingLaunchAction.ToolShortcut("5-Script OCR activates in Sprint 2 (T-014)")
            else -> null
        }
    }
}
