package com.evora.technologies.office.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.office.core.designsystem.R
import com.evora.technologies.office.core.designsystem.component.EvoraIconButton
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraTabularMonoStyle
import com.evora.technologies.office.core.designsystem.theme.EvoraTheme
import com.evora.technologies.office.core.designsystem.theme.EvoraThemeTokens
import com.evora.technologies.office.core.model.AppLanguage
import com.evora.technologies.office.core.model.AppThemeMode
import com.evora.technologies.office.core.mvi.CollectEffects

private data class OssLicenseEntry(
    val library: String,
    val license: String,
    val notice: String,
)

private val OSS_LICENSES = listOf(
    OssLicenseEntry(
        library = "LibreOfficeKit / Collabora Android Core",
        license = "MPLv2 / LGPLv3",
        notice = "Cross-compiled 16 KB ARM64 build recipe & patch manifest disclosed at Evora-Technologies-Global/E05-EvoraOffice-DOCX_PDF_XLSX (.github/workflows/lok-android-16kb.yml).",
    ),
    OssLicenseEntry("PdfiumAndroid", "Apache 2.0", "Native 16 KB PDF rasterization and outline/text indexing."),
    OssLicenseEntry("PDFBox-Android", "Apache 2.0", "Offline PDF annotations, AES encryption, split, compress, and watermark."),
    OssLicenseEntry("BouncyCastle (bcprov / bcpkix)", "MIT License", "PKCS#7 certificate inspection and signing."),
    OssLicenseEntry("hwplib & hwpxlib", "Apache 2.0", "100% offline Hangul (.hwp / .hwpx) AST parsing."),
    OssLicenseEntry("Apache Commons Compress & XZ", "Apache 2.0", "Pure-Java offline .zip and .7z inspection and extraction."),
    OssLicenseEntry("Ktor CIO", "Apache 2.0", "Embedded LAN Wi-Fi HTTP web transfer engine."),
    OssLicenseEntry("ZXing Core", "Apache 2.0", "Local QR bitmap generation for LAN Wi-Fi URLs."),
    OssLicenseEntry("AndroidX / Jetpack Compose / Room / Glance / Hilt", "Apache 2.0", "Jetpack UI, persistence, DI, and launcher widgets."),
)

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenCloudDrives: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            SettingsEffect.NavigateBack -> onBack()
            is SettingsEffect.ApplyLocale -> {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(effect.languageTag),
                )
            }
            SettingsEffect.NavigateToPaywall -> onOpenPaywall()
            SettingsEffect.NavigateToCloudDrives -> onOpenCloudDrives()
            SettingsEffect.OpenSystemAppDetails -> {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                runCatching { context.startActivity(intent) }
                    .onFailure { onShowMessage("Open Android Settings -> Default apps to configure.") }
            }
            is SettingsEffect.ShowMessage -> onShowMessage(effect.message)
        }
    }

    SettingsScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = EvoraThemeTokens.colors

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = EvoraSpacing.xs, vertical = EvoraSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EvoraIconButton(onClick = { onIntent(SettingsIntent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = stringResource(R.string.evora_settings_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(EvoraSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(EvoraSpacing.md),
            ) {
                item(key = "sec_appearance") {
                    SettingsSectionCard(title = stringResource(R.string.evora_settings_appearance)) {
                        SettingsRow(
                            icon = Icons.Outlined.DarkMode,
                            title = stringResource(R.string.evora_settings_theme),
                            subtitle = when (state.themeMode) {
                                AppThemeMode.SYSTEM -> stringResource(R.string.evora_settings_theme_system)
                                AppThemeMode.LIGHT -> stringResource(R.string.evora_settings_theme_light)
                                AppThemeMode.DARK -> stringResource(R.string.evora_settings_theme_dark)
                            },
                            onClick = { onIntent(SettingsIntent.OpenThemeDialog) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsRow(
                            icon = Icons.Outlined.Language,
                            title = stringResource(R.string.evora_settings_language),
                            subtitle = "${state.selectedLanguage.nativeName} (${state.selectedLanguage.tag})",
                            onClick = { onIntent(SettingsIntent.OpenLanguageDialog) },
                        )
                    }
                }

                item(key = "sec_files") {
                    SettingsSectionCard(title = stringResource(R.string.evora_settings_files_defaults)) {
                        SettingsRow(
                            icon = Icons.Outlined.Description,
                            title = stringResource(R.string.evora_settings_default_handler),
                            subtitle = "PDF, DOCX, XLSX, PPTX, HWP, ODF",
                            onClick = { onIntent(SettingsIntent.OpenDefaultHandlerGuide) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsRow(
                            icon = Icons.Outlined.Save,
                            title = stringResource(R.string.evora_settings_autosave),
                            subtitle = "${state.autosaveIntervalSec}s atomic draft checkpoint",
                            onClick = { onIntent(SettingsIntent.OpenAutosaveDialog) },
                        )
                    }
                }

                item(key = "sec_account") {
                    SettingsSectionCard(title = stringResource(R.string.evora_settings_account_pro)) {
                        SettingsRow(
                            icon = Icons.Outlined.WorkspacePremium,
                            title = stringResource(R.string.evora_settings_manage_pro),
                            subtitle = if (state.proStatus.isPro) "Pro Active" else if (state.proStatus.hasRewardedPass) "24h Pass Active" else "Free Plan",
                            onClick = { onIntent(SettingsIntent.ManageProClicked) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsRow(
                            icon = Icons.Outlined.Restore,
                            title = stringResource(R.string.evora_settings_restore_purchases),
                            subtitle = "Sync Play Store subscriptions & lifetime unlock",
                            onClick = { onIntent(SettingsIntent.RestorePurchasesClicked) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsRow(
                            icon = Icons.Outlined.CloudQueue,
                            title = stringResource(R.string.evora_settings_connected_clouds),
                            subtitle = "${state.connectedCloudCount} connected account(s)",
                            onClick = { onIntent(SettingsIntent.ConnectedCloudsClicked) },
                        )
                    }
                }

                item(key = "sec_privacy") {
                    SettingsSectionCard(title = stringResource(R.string.evora_settings_privacy_legal)) {
                        SettingsRow(
                            icon = Icons.Outlined.PrivacyTip,
                            title = stringResource(R.string.evora_settings_ump_privacy),
                            subtitle = "100% offline processing · Consent & GDPR/EEA controls",
                            onClick = { onIntent(SettingsIntent.OpenUmpPrivacy) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingsRow(
                            icon = Icons.Outlined.Code,
                            title = stringResource(R.string.evora_settings_oss_licenses),
                            subtitle = "MPLv2 / LGPLv3 (LibreOfficeKit) · Apache 2.0 · MIT",
                            onClick = { onIntent(SettingsIntent.OpenLicenses) },
                        )
                    }
                }

                item(key = "footer") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = EvoraSpacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Evora Office · v1.0.0 (100)",
                            style = EvoraTabularMonoStyle,
                            color = extra.textMuted,
                        )
                        Text(
                            text = "Evora Technologies Global · Track A Offline Core",
                            style = MaterialTheme.typography.labelMedium,
                            color = extra.textMuted,
                        )
                    }
                }
            }
        }
    }

    if (state.showThemeDialog) {
        AlertDialog(
            onDismissRequest = { onIntent(SettingsIntent.DismissDialogs) },
            title = { Text(stringResource(R.string.evora_settings_theme)) },
            text = {
                Column {
                    AppThemeMode.entries.forEach { mode ->
                        val label = when (mode) {
                            AppThemeMode.SYSTEM -> stringResource(R.string.evora_settings_theme_system)
                            AppThemeMode.LIGHT -> stringResource(R.string.evora_settings_theme_light)
                            AppThemeMode.DARK -> stringResource(R.string.evora_settings_theme_dark)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = EvoraSpacing.minTouchTarget)
                                .clickable { onIntent(SettingsIntent.SelectThemeMode(mode)) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = state.themeMode == mode,
                                onClick = { onIntent(SettingsIntent.SelectThemeMode(mode)) },
                            )
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onIntent(SettingsIntent.DismissDialogs) }) { Text("Close") }
            },
        )
    }

    if (state.showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { onIntent(SettingsIntent.DismissDialogs) },
            title = { Text(stringResource(R.string.evora_settings_language)) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(AppLanguage.entries, key = { it.tag }) { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = EvoraSpacing.minTouchTarget)
                                .clickable { onIntent(SettingsIntent.SelectLanguage(lang)) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = state.selectedLanguage == lang,
                                onClick = { onIntent(SettingsIntent.SelectLanguage(lang)) },
                            )
                            Column {
                                Text(lang.nativeName, style = MaterialTheme.typography.titleSmall)
                                Text(lang.regionSubtitle, style = MaterialTheme.typography.labelMedium, color = extra.textMuted)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onIntent(SettingsIntent.DismissDialogs) }) { Text("Close") }
            },
        )
    }

    if (state.showAutosaveDialog) {
        val options = listOf(10, 15, 30, 60)
        AlertDialog(
            onDismissRequest = { onIntent(SettingsIntent.DismissDialogs) },
            title = { Text(stringResource(R.string.evora_settings_autosave)) },
            text = {
                Column {
                    options.forEach { sec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = EvoraSpacing.minTouchTarget)
                                .clickable { onIntent(SettingsIntent.SelectAutosaveInterval(sec)) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = state.autosaveIntervalSec == sec,
                                onClick = { onIntent(SettingsIntent.SelectAutosaveInterval(sec)) },
                            )
                            Text("Every $sec seconds", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onIntent(SettingsIntent.DismissDialogs) }) { Text("Close") }
            },
        )
    }

    if (state.showDefaultHandlerDialog) {
        AlertDialog(
            onDismissRequest = { onIntent(SettingsIntent.DismissDialogs) },
            title = { Text(stringResource(R.string.evora_settings_default_handler)) },
            text = {
                Text(
                    "In Android App Info, open 'Set as default' -> 'Supported web/file links' or tap 'Always' when opening PDF, Word, Excel, PowerPoint, or HWP files from Files, Gmail, or WhatsApp.",
                )
            },
            confirmButton = {
                TextButton(onClick = { onIntent(SettingsIntent.LaunchSystemAppSettings) }) {
                    Text("Open App Info")
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(SettingsIntent.DismissDialogs) }) {
                    Text("Close")
                }
            },
        )
    }

    if (state.showUmpPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { onIntent(SettingsIntent.DismissDialogs) },
            title = { Text(stringResource(R.string.evora_settings_ump_privacy)) },
            text = {
                Text(
                    "Evora Office processes all documents, OCR, PDF tools, and Wi-Fi transfers 100% locally on your device. No document contents are uploaded. EEA/UK/CH privacy consent controls apply when billing and ad-consent services are enabled.",
                )
            },
            confirmButton = {
                TextButton(onClick = { onIntent(SettingsIntent.DismissDialogs) }) { Text("OK") }
            },
        )
    }

    if (state.showLicensesSheet) {
        ModalBottomSheet(onDismissRequest = { onIntent(SettingsIntent.DismissDialogs) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EvoraSpacing.lg, vertical = EvoraSpacing.sm),
            ) {
                Text(
                    text = stringResource(R.string.evora_settings_oss_licenses),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(EvoraSpacing.sm))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                ) {
                    items(OSS_LICENSES, key = { it.library }) { item ->
                        Surface(
                            shape = EvoraRadii.md,
                            color = extra.surfaceSubtle,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(EvoraSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(item.library, style = MaterialTheme.typography.titleSmall)
                                Text(item.license, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Text(item.notice, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(EvoraSpacing.xl))
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(EvoraSpacing.xs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = EvoraSpacing.xs),
        )
        Surface(
            shape = EvoraRadii.lg,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.titleSmall) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.labelMedium) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clickable(onClick = onClick),
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    EvoraTheme {
        SettingsScreen(state = SettingsState(), onIntent = {})
    }
}
