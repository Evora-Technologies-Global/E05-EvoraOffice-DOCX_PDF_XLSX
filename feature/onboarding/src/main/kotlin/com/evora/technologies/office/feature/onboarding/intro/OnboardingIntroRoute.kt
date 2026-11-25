package com.evora.technologies.office.feature.onboarding.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.office.core.designsystem.component.EvoraPrimaryButton
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraTabularMonoStyle
import com.evora.technologies.office.core.designsystem.theme.EvoraTheme
import com.evora.technologies.office.core.designsystem.theme.EvoraThemeTokens
import com.evora.technologies.office.core.mvi.CollectEffects
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun OnboardingIntroRoute(
    onNavigateNext: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingIntroViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = state.currentPage) { state.slides.size }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                viewModel.onIntent(OnboardingIntroIntent.PageChanged(page))
            }
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is OnboardingIntroEffect.ScrollToPage -> pagerState.animateScrollToPage(effect.pageIndex)
            OnboardingIntroEffect.NavigateNext -> onNavigateNext()
        }
    }

    OnboardingIntroScreen(
        state = state,
        pagerState = pagerState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
internal fun OnboardingIntroScreen(
    state: OnboardingIntroState,
    pagerState: androidx.compose.foundation.pager.PagerState = rememberPagerState { state.slides.size },
    onIntent: (OnboardingIntroIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = EvoraThemeTokens.colors
    val formats = EvoraThemeTokens.formats

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = EvoraSpacing.lg, vertical = EvoraSpacing.md),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = EvoraRadii.full,
                color = extra.surfaceSubtle,
            ) {
                Text(
                    text = "Slide ${state.currentPage + 1} / ${state.slides.size}",
                    style = EvoraTabularMonoStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            TextButton(
                onClick = { onIntent(OnboardingIntroIntent.SkipClicked) },
                modifier = Modifier.heightIn(min = EvoraSpacing.minTouchTarget),
            ) {
                Text("Skip")
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            val slide = state.slides[page]
            val (bgTint, inkTint, icon) = when (page) {
                0 -> Triple(formats.word.tintBg, formats.word.inkText, Icons.Outlined.Description)
                1 -> Triple(formats.pdf.tintBg, formats.pdf.inkText, Icons.Outlined.PictureAsPdf)
                else -> Triple(formats.excel.tintBg, formats.excel.inkText, Icons.Outlined.DocumentScanner)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = EvoraSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(144.dp)
                        .clip(EvoraRadii.xl)
                        .background(bgTint),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = inkTint,
                        modifier = Modifier.size(64.dp),
                    )
                }
                Spacer(modifier = Modifier.height(EvoraSpacing.xl))
                Surface(
                    shape = EvoraRadii.full,
                    color = extra.successContainer,
                ) {
                    Text(
                        text = slide.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = extra.onSuccessContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }
                Spacer(modifier = Modifier.height(EvoraSpacing.md))
                Text(
                    text = slide.title,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(EvoraSpacing.sm))
                Text(
                    text = slide.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(EvoraSpacing.lg),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.sm)) {
                repeat(state.slides.size) { index ->
                    val active = index == state.currentPage
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (active) 24.dp else 8.dp)
                            .clip(EvoraRadii.full)
                            .background(
                                if (active) extra.brandAccent else MaterialTheme.colorScheme.outlineVariant,
                            ),
                    )
                }
            }
            EvoraPrimaryButton(
                text = if (state.isLastSlide) "Continue" else "Next",
                onClick = { onIntent(OnboardingIntroIntent.NextClicked) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingIntroPreview() {
    EvoraTheme {
        OnboardingIntroScreen(
            state = OnboardingIntroState(),
            onIntent = {},
        )
    }
}
