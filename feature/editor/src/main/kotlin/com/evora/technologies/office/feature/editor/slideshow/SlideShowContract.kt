package com.evora.technologies.office.feature.editor.slideshow

import com.evora.technologies.office.core.model.SlidePageData
import com.evora.technologies.office.core.mvi.UiEffect
import com.evora.technologies.office.core.mvi.UiIntent
import com.evora.technologies.office.core.mvi.UiState

data class SlideShowState(
    val title: String = "Presentation",
    val slides: List<SlidePageData> = emptyList(),
    val currentSlide: Int = 0,
    val elapsedSeconds: Int = 0,
    val isLaserEnabled: Boolean = false,
    val laserStrokes: List<List<Pair<Float, Float>>> = emptyList(),
    val showNotes: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) : UiState

sealed interface SlideShowIntent : UiIntent {
    data class OpenPresentation(val uriString: String, val startSlideIndex: Int) : SlideShowIntent
    data class SlideChanged(val index: Int) : SlideShowIntent
    data object NextSlide : SlideShowIntent
    data object PrevSlide : SlideShowIntent
    data object ToggleLaserPointer : SlideShowIntent
    data class AddLaserStroke(val points: List<Pair<Float, Float>>) : SlideShowIntent
    data object ClearLaserStrokes : SlideShowIntent
    data object ToggleNotes : SlideShowIntent
    data object ExitClicked : SlideShowIntent
}

sealed interface SlideShowEffect : UiEffect {
    data object NavigateBack : SlideShowEffect
}
