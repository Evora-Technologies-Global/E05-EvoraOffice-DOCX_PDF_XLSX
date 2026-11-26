package com.evora.technologies.office.feature.onboarding.intro

import com.evora.technologies.office.core.mvi.UiEffect
import com.evora.technologies.office.core.mvi.UiIntent
import com.evora.technologies.office.core.mvi.UiState

data class OnboardingSlide(
    val title: String,
    val description: String,
    val badge: String,
)

data class OnboardingIntroState(
    val slides: List<OnboardingSlide> = DEFAULT_SLIDES,
    val currentPage: Int = 0,
) : UiState {
    val isLastSlide: Boolean get() = currentPage >= slides.lastIndex

    companion object {
        val DEFAULT_SLIDES = listOf(
            OnboardingSlide(
                title = "Edit Word, Excel & PPT Offline",
                description = "Powered by LibreOfficeKit 16KB engine. Save your documents anytime with zero paywall ransom traps.",
                badge = "100% Offline Engine",
            ),
            OnboardingSlide(
                title = "Real PDF Power Tools",
                description = "Split pages, compress attachments, apply custom watermarks, sign with PKCS#7, and convert PDF to DOCX locally.",
                badge = "Zero Fake Stubs",
            ),
            OnboardingSlide(
                title = "Dual-Side ID Scan & Wi-Fi Drop",
                description = "Scan Front & Back IDs onto one printable A4 page, extract 5-script OCR text, and drag-drop files over LAN Wi-Fi.",
                badge = "Zero Server Uploads",
            ),
        )
    }
}

sealed interface OnboardingIntroIntent : UiIntent {
    data class PageChanged(val pageIndex: Int) : OnboardingIntroIntent
    data object NextClicked : OnboardingIntroIntent
    data object SkipClicked : OnboardingIntroIntent
}

sealed interface OnboardingIntroEffect : UiEffect {
    data class ScrollToPage(val pageIndex: Int) : OnboardingIntroEffect
    data object NavigateNext : OnboardingIntroEffect
    data object NavigateSkipPaywall : OnboardingIntroEffect
}
