package com.evora.technologies.office.feature.editor.slideshow

import com.evora.technologies.office.core.domain.port.OfficeEditorPort
import com.evora.technologies.office.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import javax.inject.Inject

@HiltViewModel
class SlideShowViewModel @Inject constructor(
    private val officeEditorPort: OfficeEditorPort,
) : MviViewModel<SlideShowState, SlideShowIntent, SlideShowEffect>(
    SlideShowState(),
) {
    private var timerJob: Job? = null

    override fun onIntent(intent: SlideShowIntent) {
        when (intent) {
            is SlideShowIntent.OpenPresentation -> loadSlides(intent.uriString, intent.startSlideIndex)
            is SlideShowIntent.SlideChanged -> setState {
                copy(
                    currentSlide = intent.index.coerceIn(0, (slides.size - 1).coerceAtLeast(0)),
                    laserStrokes = emptyList(),
                )
            }
            SlideShowIntent.NextSlide -> setState {
                copy(currentSlide = (currentSlide + 1).coerceAtMost((slides.size - 1).coerceAtLeast(0)), laserStrokes = emptyList())
            }
            SlideShowIntent.PrevSlide -> setState {
                copy(currentSlide = (currentSlide - 1).coerceAtLeast(0), laserStrokes = emptyList())
            }
            SlideShowIntent.ToggleLaserPointer -> setState { copy(isLaserEnabled = !isLaserEnabled) }
            is SlideShowIntent.AddLaserStroke -> if (intent.points.size >= 2) {
                setState { copy(laserStrokes = (laserStrokes + listOf(intent.points)).takeLast(6)) }
            }
            SlideShowIntent.ClearLaserStrokes -> setState { copy(laserStrokes = emptyList()) }
            SlideShowIntent.ToggleNotes -> setState { copy(showNotes = !showNotes) }
            SlideShowIntent.ExitClicked -> {
                timerJob?.cancel()
                sendEffect(SlideShowEffect.NavigateBack)
            }
        }
    }

    private fun loadSlides(uriString: String, startIndex: Int) {
        timerJob?.cancel()
        launchSafely(
            onError = { setState { copy(isLoading = false, errorMessage = it.message ?: "Unable to load slides") } },
        ) {
            setState { copy(isLoading = true, errorMessage = null) }
            val deck = officeEditorPort.loadSlideDeck(uriString).getOrThrow()
            setState {
                copy(
                    title = deck.title,
                    slides = deck.slides,
                    currentSlide = startIndex.coerceIn(0, (deck.slides.size - 1).coerceAtLeast(0)),
                    isLoading = false,
                )
            }
            timerJob = launchSafely {
                while (isActive) {
                    delay(1000L)
                    setState { copy(elapsedSeconds = elapsedSeconds + 1) }
                }
            }
        }
    }
}
