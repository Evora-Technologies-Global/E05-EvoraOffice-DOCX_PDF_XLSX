package com.evora.technologies.office.feature.onboarding.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.office.core.designsystem.component.EvoraBrandMark
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraTheme
import com.evora.technologies.office.core.model.ExternalIntentPayload
import com.evora.technologies.office.core.mvi.CollectEffects

@Composable
fun SplashRoute(
    externalUri: String?,
    displayNameHint: String?,
    mimeTypeHint: String?,
    onNavigateLanguageSelect: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateExternalDocument: (ExternalIntentPayload) -> Unit,
    onShowError: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(externalUri) {
        viewModel.onIntent(
            SplashIntent.StartRouting(
                externalUri = externalUri,
                displayNameHint = displayNameHint,
                mimeTypeHint = mimeTypeHint,
            ),
        )
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            SplashEffect.NavigateToLanguageSelect -> onNavigateLanguageSelect()
            SplashEffect.NavigateToHome -> onNavigateHome()
            is SplashEffect.NavigateToExternalDocument -> onNavigateExternalDocument(effect.payload)
            is SplashEffect.ShowError -> onShowError(effect.message)
        }
    }

    SplashScreen(state = state, modifier = modifier)
}

@Composable
internal fun SplashScreen(
    state: SplashState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF186FD4), Color(0xFF2489FF)),
                ),
            )
            .padding(EvoraSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EvoraBrandMark(size = 72.dp)
        Spacer(modifier = Modifier.height(EvoraSpacing.lg))
        Text(
            text = "Evora Office",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(EvoraSpacing.xs))
        Text(
            text = "DOCX · PDF · XLSX · PPTX · HWP · 100% Offline",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.9f),
        )
        if (state.isRouting) {
            Spacer(modifier = Modifier.height(EvoraSpacing.xl))
            LinearProgressIndicator(
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.width(180.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    EvoraTheme {
        SplashScreen(state = SplashState())
    }
}
