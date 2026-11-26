package com.evora.technologies.office.feature.cloud.browser

import com.evora.technologies.office.core.model.BreadcrumbSegment
import com.evora.technologies.office.core.model.CloudAccount
import com.evora.technologies.office.core.model.CloudItem
import com.evora.technologies.office.core.model.CloudProvider
import com.evora.technologies.office.core.model.CloudTransferProgress
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.mvi.UiEffect
import com.evora.technologies.office.core.mvi.UiIntent
import com.evora.technologies.office.core.mvi.UiState

data class CloudDriveBrowserState(
    val selectedProvider: CloudProvider = CloudProvider.GOOGLE_DRIVE,
    val accounts: List<CloudAccount> = emptyList(),
    val activeAccountId: String? = null,
    val currentFolderId: String = "root",
    val breadcrumbs: List<BreadcrumbSegment> = listOf(BreadcrumbSegment("Root", "root")),
    val items: List<CloudItem> = emptyList(),
    val nextPageToken: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val transferProgress: CloudTransferProgress? = null,
    val errorMessage: String? = null,
) : UiState {
    val providerAccounts: List<CloudAccount>
        get() = accounts.filter { it.provider == selectedProvider }
    val activeAccount: CloudAccount?
        get() = providerAccounts.firstOrNull { it.accountId == activeAccountId } ?: providerAccounts.firstOrNull()
}

sealed interface CloudDriveBrowserIntent : UiIntent {
    data class InitializeProvider(val provider: CloudProvider) : CloudDriveBrowserIntent
    data class SelectProvider(val provider: CloudProvider) : CloudDriveBrowserIntent
    data class SelectAccount(val accountId: String) : CloudDriveBrowserIntent
    data object ConnectProviderClicked : CloudDriveBrowserIntent
    data class GoogleSignInCompleted(
        val accessToken: String,
        val email: String,
        val displayName: String,
        val avatarUri: String? = null,
    ) : CloudDriveBrowserIntent
    data class OneDriveSignInCompleted(
        val accessToken: String,
        val accountId: String,
        val email: String,
        val displayName: String,
    ) : CloudDriveBrowserIntent
    data class AuthFailed(val message: String) : CloudDriveBrowserIntent
    data class DisconnectAccount(val accountId: String) : CloudDriveBrowserIntent
    data class OpenCloudItem(val item: CloudItem) : CloudDriveBrowserIntent
    data class NavigateBreadcrumb(val folderId: String) : CloudDriveBrowserIntent
    data class SearchQueryChanged(val query: String) : CloudDriveBrowserIntent
    data object LoadNextPage : CloudDriveBrowserIntent
    data object Refresh : CloudDriveBrowserIntent
    data object UploadFileClicked : CloudDriveBrowserIntent
    data class LocalFilePickedForUpload(val uriString: String?) : CloudDriveBrowserIntent
    data object BackClicked : CloudDriveBrowserIntent
}

sealed interface CloudDriveBrowserEffect : UiEffect {
    data object NavigateBack : CloudDriveBrowserEffect
    data object LaunchGoogleDriveAuth : CloudDriveBrowserEffect
    data object LaunchOneDriveAuth : CloudDriveBrowserEffect
    data object LaunchUploadFilePicker : CloudDriveBrowserEffect
    data class OpenDownloadedDocument(val document: DocumentItem) : CloudDriveBrowserEffect
    data class ShowMessage(val message: String) : CloudDriveBrowserEffect
}
