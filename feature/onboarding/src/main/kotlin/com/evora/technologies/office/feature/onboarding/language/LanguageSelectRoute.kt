package com.evora.technologies.office.feature.onboarding.language

import androidx.activity.compose.ReportDrawn
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.office.core.designsystem.component.EvoraPrimaryButton
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraTheme
import com.evora.technologies.office.core.designsystem.theme.EvoraThemeTokens
import com.evora.technologies.office.core.model.AppLanguage
import com.evora.technologies.office.core.mvi.CollectEffects

@Composable
fun LanguageSelectRoute(
    onNavigateIntro: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LanguageSelectViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is LanguageSelectEffect.ApplyLocale -> {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(effect.languageTag),
                )
            }
            LanguageSelectEffect.NavigateToIntro -> onNavigateIntro()
        }
    }

    ReportDrawn()

    LanguageSelectScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
internal fun LanguageSelectScreen(
    state: LanguageSelectState,
    onIntent: (LanguageSelectIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = EvoraThemeTokens.colors
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Box(modifier = Modifier.padding(EvoraSpacing.lg)) {
                    EvoraPrimaryButton(
                        text = "Continue",
                        onClick = { onIntent(LanguageSelectIntent.ContinueClicked) },
                        isLoading = state.isSaving,
                        modifier = Modifier.testTag("language_continue_btn"),
                    )
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("language_list")
                .padding(innerPadding)
                .padding(horizontal = EvoraSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
        ) {
            item {
                Column(modifier = Modifier.padding(vertical = EvoraSpacing.md)) {
                    Text(
                        text = "Choose Language",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "Select your preferred interface language. Change anytime in Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(state.languages, key = { it.tag }) { language ->
                val isSelected = state.selectedLanguage == language
                Surface(
                    shape = EvoraRadii.md,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) extra.brandAccent else MaterialTheme.colorScheme.outlineVariant,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .clip(EvoraRadii.md)
                        .clickable { onIntent(LanguageSelectIntent.SelectLanguage(language)) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = EvoraSpacing.md, vertical = EvoraSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(EvoraRadii.sm)
                                .background(EvoraThemeTokens.formats.word.tintBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = language.tag.uppercase().take(5),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = EvoraThemeTokens.formats.word.inkText,
                                maxLines = 1,
                            )
                        }
                        Spacer(modifier = Modifier.width(EvoraSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = language.nativeName,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = language.regionSubtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LanguageSelectPreview() {
    EvoraTheme {
        LanguageSelectScreen(
            state = LanguageSelectState(selectedLanguage = AppLanguage.VI),
            onIntent = {},
        )
    }
}
