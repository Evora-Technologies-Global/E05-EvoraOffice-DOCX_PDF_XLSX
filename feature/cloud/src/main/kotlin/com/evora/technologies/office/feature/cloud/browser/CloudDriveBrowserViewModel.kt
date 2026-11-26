package com.evora.technologies.office.feature.cloud.browser

import androidx.lifecycle.viewModelScope
import com.evora.technologies.office.core.domain.port.CloudDrivePort
import com.evora.technologies.office.core.model.BreadcrumbSegment
import com.evora.technologies.office.core.model.CloudAccount
import com.evora.technologies.office.core.model.CloudItem
import com.evora.technologies.office.core.model.CloudProvider
import com.evora.technologies.office.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class CloudDriveBrowserViewModel @Inject constructor(
    private val cloudDrivePort: CloudDrivePort,
) : MviViewModel<CloudDriveBrowserState, CloudDriveBrowserIntent, CloudDriveBrowserEffect>(
    CloudDriveBrowserState(),
) {
    private var listJob: Job? = null
    private var searchJob: Job? = null
    private var transferJob: Job? = null

    init {
        cloudDrivePort.observeConnectedAccounts()
            .onEach { all ->
                val prevActive = currentState.activeAccount?.accountId
                setState { copy(accounts = all) }
                val newActive = currentState.activeAccount
                if (newActive != null &&
                    newActive.accountId != prevActive &&
                    !currentState.isLoading &&
                    currentState.items.isEmpty()
                ) {
                    loadFolder(newActive, "root", resetCrumbs = true)
                } else if (newActive == null) {
                    setState { copy(items = emptyList(), nextPageToken = null) }
                }
            }
            .launchIn(viewModelScope)

        cloudDrivePort.observeTransferProgress()
            .onEach { progress ->
                setState { copy(transferProgress = progress) }
            }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: CloudDriveBrowserIntent) {
        when (intent) {
            is CloudDriveBrowserIntent.InitializeProvider -> switchProvider(intent.provider)
            is CloudDriveBrowserIntent.SelectProvider -> switchProvider(intent.provider)
            is CloudDriveBrowserIntent.SelectAccount -> {
                setState { copy(activeAccountId = intent.accountId, errorMessage = null) }
                currentState.activeAccount?.let { loadFolder(it, "root", resetCrumbs = true) }
            }
            CloudDriveBrowserIntent.ConnectProviderClicked -> triggerAuth()
            is CloudDriveBrowserIntent.GoogleSignInCompleted -> handleGoogleConnected(intent)
            is CloudDriveBrowserIntent.OneDriveSignInCompleted -> handleOneDriveConnected(intent)
            is CloudDriveBrowserIntent.AuthFailed -> {
                setState { copy(errorMessage = intent.message) }
                sendEffect(CloudDriveBrowserEffect.ShowMessage(intent.message))
            }
            is CloudDriveBrowserIntent.DisconnectAccount -> disconnect(intent.accountId)
            is CloudDriveBrowserIntent.OpenCloudItem -> handleItemClick(intent.item)
            is CloudDriveBrowserIntent.NavigateBreadcrumb -> jumpToBreadcrumb(intent.folderId)
            is CloudDriveBrowserIntent.SearchQueryChanged -> handleSearch(intent.query)
            CloudDriveBrowserIntent.LoadNextPage -> loadNextPage()
            CloudDriveBrowserIntent.Refresh -> {
                currentState.activeAccount?.let { loadFolder(it, currentState.currentFolderId, resetCrumbs = false) }
            }
            CloudDriveBrowserIntent.UploadFileClicked -> {
                if (currentState.activeAccount == null) {
                    sendEffect(CloudDriveBrowserEffect.ShowMessage("Connect an account first"))
                } else {
                    sendEffect(CloudDriveBrowserEffect.LaunchUploadFilePicker)
                }
            }
            is CloudDriveBrowserIntent.LocalFilePickedForUpload -> uploadLocalFile(intent.uriString)
            CloudDriveBrowserIntent.BackClicked -> sendEffect(CloudDriveBrowserEffect.NavigateBack)
        }
    }

    private fun switchProvider(provider: CloudProvider) {
        searchJob?.cancel()
        setState {
            copy(
                selectedProvider = provider,
                activeAccountId = null,
                currentFolderId = "root",
                breadcrumbs = listOf(BreadcrumbSegment("Root", "root")),
                items = emptyList(),
                nextPageToken = null,
                searchQuery = "",
                errorMessage = null,
            )
        }
        currentState.activeAccount?.let { loadFolder(it, "root", resetCrumbs = true) }
    }

    private fun triggerAuth() {
        launchSafely(
            onError = { err -> sendEffect(CloudDriveBrowserEffect.ShowMessage(err.message ?: "Auth error")) },
        ) {
            when (currentState.selectedProvider) {
                CloudProvider.GOOGLE_DRIVE -> sendEffect(CloudDriveBrowserEffect.LaunchGoogleDriveAuth)
                CloudProvider.ONE_DRIVE -> {
                    if (!cloudDrivePort.isProviderConfigured(CloudProvider.ONE_DRIVE)) {
                        sendEffect(
                            CloudDriveBrowserEffect.ShowMessage(
                                "Configure evora_onedrive_client_id in strings.xml/msal_config.json to sign in to OneDrive.",
                            ),
                        )
                    } else {
                        sendEffect(CloudDriveBrowserEffect.LaunchOneDriveAuth)
                    }
                }
            }
        }
    }

    private fun handleGoogleConnected(intent: CloudDriveBrowserIntent.GoogleSignInCompleted) {
        launchSafely(
            onError = { err ->
                setState { copy(errorMessage = err.message) }
                sendEffect(CloudDriveBrowserEffect.ShowMessage(err.message ?: "Google Drive sign-in failed"))
            },
        ) {
            val acc = cloudDrivePort.connectGoogleDrive(
                accessToken = intent.accessToken,
                email = intent.email,
                displayName = intent.displayName,
                avatarUri = intent.avatarUri,
            ).getOrThrow()
            setState { copy(activeAccountId = acc.accountId, errorMessage = null) }
            sendEffect(CloudDriveBrowserEffect.ShowMessage("Connected ${acc.email}"))
            loadFolder(acc, "root", resetCrumbs = true)
        }
    }

    private fun handleOneDriveConnected(intent: CloudDriveBrowserIntent.OneDriveSignInCompleted) {
        launchSafely(
            onError = { err ->
                setState { copy(errorMessage = err.message) }
                sendEffect(CloudDriveBrowserEffect.ShowMessage(err.message ?: "OneDrive sign-in failed"))
            },
        ) {
            val acc = cloudDrivePort.connectOneDrive(
                accessToken = intent.accessToken,
                accountId = intent.accountId,
                email = intent.email,
                displayName = intent.displayName,
            ).getOrThrow()
            setState { copy(activeAccountId = acc.accountId, errorMessage = null) }
            sendEffect(CloudDriveBrowserEffect.ShowMessage("Connected ${acc.email}"))
            loadFolder(acc, "root", resetCrumbs = true)
        }
    }

    private fun disconnect(accountId: String) {
        launchSafely(
            onError = { err -> sendEffect(CloudDriveBrowserEffect.ShowMessage(err.message ?: "Disconnect failed")) },
        ) {
            cloudDrivePort.disconnectAccount(accountId).getOrThrow()
            val wasActive = currentState.activeAccount?.accountId == accountId ||
                currentState.activeAccountId == accountId
            if (wasActive) {
                setState { copy(activeAccountId = null, items = emptyList(), nextPageToken = null) }
            }
            sendEffect(CloudDriveBrowserEffect.ShowMessage("Account disconnected and tokens wiped"))
        }
    }

    private fun handleItemClick(item: CloudItem) {
        val account = currentState.activeAccount ?: return
        if (item.isFolder) {
            searchJob?.cancel()
            val nextCrumbs = currentState.breadcrumbs + BreadcrumbSegment(item.name, item.id)
            setState { copy(breadcrumbs = nextCrumbs, searchQuery = "") }
            loadFolder(account, item.id, resetCrumbs = false)
            return
        }
        if (transferJob?.isActive == true) {
            sendEffect(CloudDriveBrowserEffect.ShowMessage("Wait for active transfer to finish"))
            return
        }
        transferJob = launchSafely(
            onError = { err ->
                sendEffect(CloudDriveBrowserEffect.ShowMessage(err.message ?: "Download failed"))
            },
        ) {
            val localDoc = cloudDrivePort.downloadFile(account, item).getOrThrow()
            sendEffect(CloudDriveBrowserEffect.OpenDownloadedDocument(localDoc))
        }
    }

    private fun jumpToBreadcrumb(folderId: String) {
        val account = currentState.activeAccount ?: return
        searchJob?.cancel()
        val idx = currentState.breadcrumbs.indexOfFirst { it.pathOrUri == folderId }
        if (idx >= 0) {
            setState { copy(breadcrumbs = breadcrumbs.take(idx + 1), searchQuery = "") }
        }
        loadFolder(account, folderId, resetCrumbs = false)
    }

    private fun handleSearch(query: String) {
        setState { copy(searchQuery = query) }
        val account = currentState.activeAccount ?: return
        searchJob?.cancel()
        searchJob = launchSafely {
            delay(350L)
            loadFolder(account, currentState.currentFolderId, resetCrumbs = false)
        }
    }

    private fun loadFolder(account: CloudAccount, folderId: String, resetCrumbs: Boolean) {
        listJob?.cancel()
        listJob = launchSafely(
            onError = { err ->
                setState { copy(isLoading = false, errorMessage = err.message ?: "Unable to load folder") }
            },
        ) {
            setState {
                copy(
                    isLoading = true,
                    currentFolderId = folderId,
                    breadcrumbs = if (resetCrumbs) listOf(BreadcrumbSegment("Root", "root")) else breadcrumbs,
                    errorMessage = null,
                )
            }
            val page = cloudDrivePort.listFolder(
                account = account,
                folderId = folderId,
                pageToken = null,
                query = currentState.searchQuery,
            ).getOrThrow()
            setState {
                copy(
                    isLoading = false,
                    items = page.items,
                    nextPageToken = page.nextPageToken,
                    errorMessage = null,
                )
            }
        }
    }

    private fun loadNextPage() {
        val account = currentState.activeAccount ?: return
        val token = currentState.nextPageToken ?: return
        if (currentState.isLoadingMore) return
        launchSafely(
            onError = { setState { copy(isLoadingMore = false) } },
        ) {
            setState { copy(isLoadingMore = true) }
            val page = cloudDrivePort.listFolder(
                account = account,
                folderId = currentState.currentFolderId,
                pageToken = token,
                query = currentState.searchQuery,
            ).getOrThrow()
            setState {
                copy(
                    isLoadingMore = false,
                    items = (items + page.items).distinctBy { it.id },
                    nextPageToken = page.nextPageToken,
                )
            }
        }
    }

    private fun uploadLocalFile(uriString: String?) {
        if (uriString.isNullOrBlank()) return
        val account = currentState.activeAccount ?: return
        if (transferJob?.isActive == true) {
            sendEffect(CloudDriveBrowserEffect.ShowMessage("Wait for active transfer to finish"))
            return
        }
        transferJob = launchSafely(
            onError = { err ->
                sendEffect(CloudDriveBrowserEffect.ShowMessage(err.message ?: "Upload failed"))
            },
        ) {
            val uploaded = cloudDrivePort.uploadLocalFile(
                account = account,
                localUriString = uriString,
                targetFolderId = currentState.currentFolderId,
            ).getOrThrow()
            sendEffect(CloudDriveBrowserEffect.ShowMessage("Uploaded ${uploaded.name}"))
            loadFolder(account, currentState.currentFolderId, resetCrumbs = false)
        }
    }
}
