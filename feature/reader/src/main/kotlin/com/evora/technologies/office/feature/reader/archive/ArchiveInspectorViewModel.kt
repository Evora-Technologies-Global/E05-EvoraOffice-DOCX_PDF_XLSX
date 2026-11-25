package com.evora.technologies.office.feature.reader.archive

import com.evora.technologies.office.core.domain.port.ArchiveEnginePort
import com.evora.technologies.office.core.domain.port.DocumentInvalidPasswordException
import com.evora.technologies.office.core.domain.port.DocumentPasswordRequiredException
import com.evora.technologies.office.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ArchiveInspectorViewModel @Inject constructor(
    private val archiveEngine: ArchiveEnginePort,
) : MviViewModel<ArchiveInspectorState, ArchiveInspectorIntent, ArchiveInspectorEffect>(
    ArchiveInspectorState(),
) {
    private var activePassword: String? = null

    override fun onIntent(intent: ArchiveInspectorIntent) {
        when (intent) {
            is ArchiveInspectorIntent.LoadArchive -> loadArchive(intent.uriString, intent.displayName, null)
            is ArchiveInspectorIntent.SubmitPassword -> {
                activePassword = intent.password
                loadArchive(currentState.uriString, currentState.archiveName, intent.password)
            }
            ArchiveInspectorIntent.DismissPasswordDialog -> {
                setState { copy(passwordRequired = false, passwordError = null) }
                sendEffect(ArchiveInspectorEffect.NavigateBack)
            }
            is ArchiveInspectorIntent.ToggleEntrySelection -> {
                setState {
                    val updated = if (intent.path in selectedEntryPaths) {
                        selectedEntryPaths - intent.path
                    } else {
                        selectedEntryPaths + intent.path
                    }
                    copy(selectedEntryPaths = updated)
                }
            }
            is ArchiveInspectorIntent.ExtractEntries -> extract(intent.destinationDirPath, intent.extractAll)
            is ArchiveInspectorIntent.OpenExtractedFile -> {
                val name = intent.path.substringAfterLast('/')
                sendEffect(ArchiveInspectorEffect.OpenExtractedDocument(intent.path, name))
            }
            ArchiveInspectorIntent.BackClicked -> sendEffect(ArchiveInspectorEffect.NavigateBack)
        }
    }

    private fun loadArchive(uriString: String, displayName: String, password: String?) {
        launchSafely(
            onError = { err ->
                when (err) {
                    is DocumentPasswordRequiredException -> setState {
                        copy(isLoading = false, passwordRequired = true, passwordError = null)
                    }
                    is DocumentInvalidPasswordException -> setState {
                        copy(isLoading = false, passwordRequired = true, passwordError = err.message)
                    }
                    else -> setState {
                        copy(isLoading = false, errorMessage = err.message ?: "Unable to read archive")
                    }
                }
            },
        ) {
            setState {
                copy(
                    uriString = uriString,
                    archiveName = displayName,
                    isLoading = true,
                    errorMessage = null,
                    passwordError = null,
                )
            }
            val result = archiveEngine.listEntries(uriString, password)
            setState {
                copy(
                    isLoading = false,
                    archiveName = result.archiveName.ifBlank { displayName },
                    extension = result.extension,
                    isSupported = result.isSupported,
                    unsupportedReason = result.unsupportedReason,
                    entries = result.entries,
                    passwordRequired = false,
                    passwordError = null,
                )
            }
        }
    }

    private fun extract(destinationDirPath: String, extractAll: Boolean) {
        if (currentState.isExtracting || !currentState.isSupported) return
        launchSafely(
            onError = { err ->
                setState { copy(isExtracting = false, errorMessage = err.message) }
            },
        ) {
            setState { copy(isExtracting = true, errorMessage = null) }
            val paths = if (extractAll || currentState.selectedEntryPaths.isEmpty()) {
                null
            } else {
                currentState.selectedEntryPaths
            }
            val outcome = archiveEngine.extractEntries(
                uriString = currentState.uriString,
                destinationDirPath = destinationDirPath,
                entryPaths = paths,
                password = activePassword,
            )
            outcome.fold(
                onSuccess = { extracted ->
                    setState { copy(isExtracting = false, extractedPaths = extracted) }
                    sendEffect(ArchiveInspectorEffect.ShowMessage("Extracted ${extracted.size} file(s)"))
                },
                onFailure = { err ->
                    when (err) {
                        is DocumentPasswordRequiredException -> setState {
                            copy(isExtracting = false, passwordRequired = true, passwordError = null)
                        }
                        is DocumentInvalidPasswordException -> setState {
                            copy(isExtracting = false, passwordRequired = true, passwordError = err.message)
                        }
                        else -> {
                            setState { copy(isExtracting = false, errorMessage = err.message) }
                            sendEffect(ArchiveInspectorEffect.ShowMessage(err.message ?: "Extraction failed"))
                        }
                    }
                },
            )
        }
    }
}
