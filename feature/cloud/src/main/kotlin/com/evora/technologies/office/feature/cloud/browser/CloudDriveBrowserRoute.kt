package com.evora.technologies.office.feature.cloud.browser

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.office.core.designsystem.R
import com.evora.technologies.office.core.designsystem.component.EmptyStateView
import com.evora.technologies.office.core.designsystem.component.ErrorStateCard
import com.evora.technologies.office.core.designsystem.component.EvoraIconButton
import com.evora.technologies.office.core.designsystem.component.EvoraOutlinedButton
import com.evora.technologies.office.core.designsystem.component.FormatBadge
import com.evora.technologies.office.core.designsystem.component.ShimmerDocListSkeleton
import com.evora.technologies.office.core.designsystem.component.formatByteSize
import com.evora.technologies.office.core.designsystem.component.formatEpochShort
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraTheme
import com.evora.technologies.office.core.designsystem.theme.EvoraThemeTokens
import com.evora.technologies.office.core.model.CloudItem
import com.evora.technologies.office.core.model.CloudProvider
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.mvi.CollectEffects
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException

@Composable
fun CloudDriveBrowserRoute(
    initialProvider: CloudProvider = CloudProvider.GOOGLE_DRIVE,
    onBack: () -> Unit,
    onOpenDownloadedDocument: (DocumentItem) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CloudDriveBrowserViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(initialProvider) {
        viewModel.onIntent(CloudDriveBrowserIntent.InitializeProvider(initialProvider))
    }

    val googleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK && res.data != null) {
            runCatching {
                val authResult = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(res.data)
                val token = authResult.accessToken
                val email = authResult.toGoogleSignInAccount()?.email ?: "google_user@drive"
                val name = authResult.toGoogleSignInAccount()?.displayName ?: email
                if (!token.isNullOrBlank()) {
                    viewModel.onIntent(
                        CloudDriveBrowserIntent.GoogleSignInCompleted(
                            accessToken = token,
                            email = email,
                            displayName = name,
                        ),
                    )
                } else {
                    viewModel.onIntent(CloudDriveBrowserIntent.AuthFailed("Google Drive access token unavailable"))
                }
            }.onFailure { err ->
                viewModel.onIntent(CloudDriveBrowserIntent.AuthFailed(err.message ?: "Google Drive sign-in failed"))
            }
        }
    }

    val uploadPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        viewModel.onIntent(CloudDriveBrowserIntent.LocalFilePickedForUpload(uri?.toString()))
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            CloudDriveBrowserEffect.NavigateBack -> onBack()
            CloudDriveBrowserEffect.LaunchGoogleDriveAuth -> {
                launchGoogleDriveAuthorization(
                    context = context,
                    onLaunchIntentSender = { pendingIntent ->
                        googleAuthLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                    },
                    onTokenReady = { token, email, name ->
                        viewModel.onIntent(CloudDriveBrowserIntent.GoogleSignInCompleted(token, email, name))
                    },
                    onFailure = { msg ->
                        viewModel.onIntent(CloudDriveBrowserIntent.AuthFailed(msg))
                    },
                )
            }
            CloudDriveBrowserEffect.LaunchOneDriveAuth -> {
                val activity = context as? Activity
                if (activity == null) {
                    viewModel.onIntent(CloudDriveBrowserIntent.AuthFailed("Activity context unavailable"))
                } else {
                    launchOneDriveMsal(
                        activity = activity,
                        onTokenReady = { token, accountId, email, name ->
                            viewModel.onIntent(
                                CloudDriveBrowserIntent.OneDriveSignInCompleted(
                                    accessToken = token,
                                    accountId = accountId,
                                    email = email,
                                    displayName = name,
                                ),
                            )
                        },
                        onFailure = { msg ->
                            viewModel.onIntent(CloudDriveBrowserIntent.AuthFailed(msg))
                        },
                    )
                }
            }
            CloudDriveBrowserEffect.LaunchUploadFilePicker -> uploadPickerLauncher.launch(arrayOf("*/*"))
            is CloudDriveBrowserEffect.OpenDownloadedDocument -> onOpenDownloadedDocument(effect.document)
            is CloudDriveBrowserEffect.ShowMessage -> onShowMessage(effect.message)
        }
    }

    CloudDriveBrowserScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
internal fun CloudDriveBrowserScreen(
    state: CloudDriveBrowserState,
    onIntent: (CloudDriveBrowserIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = EvoraThemeTokens.colors

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(bottom = EvoraSpacing.xs)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = EvoraSpacing.xs, vertical = EvoraSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        EvoraIconButton(onClick = { onIntent(CloudDriveBrowserIntent.BackClicked) }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            text = stringResource(R.string.evora_cloud_title),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                        )
                        if (state.activeAccount != null) {
                            EvoraIconButton(onClick = { onIntent(CloudDriveBrowserIntent.UploadFileClicked) }) {
                                Icon(
                                    Icons.Outlined.CloudUpload,
                                    contentDescription = stringResource(R.string.evora_cloud_upload),
                                )
                            }
                            EvoraIconButton(onClick = { onIntent(CloudDriveBrowserIntent.Refresh) }) {
                                Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                            }
                        }
                    }

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = EvoraSpacing.lg),
                    ) {
                        CloudProvider.entries.forEachIndexed { idx, provider ->
                            SegmentedButton(
                                selected = state.selectedProvider == provider,
                                onClick = { onIntent(CloudDriveBrowserIntent.SelectProvider(provider)) },
                                shape = SegmentedButtonDefaults.itemShape(idx, CloudProvider.entries.size),
                                label = {
                                    Text(
                                        when (provider) {
                                            CloudProvider.GOOGLE_DRIVE -> stringResource(R.string.evora_cloud_google_drive)
                                            CloudProvider.ONE_DRIVE -> stringResource(R.string.evora_cloud_onedrive)
                                        },
                                    )
                                },
                            )
                        }
                    }

                    if (state.providerAccounts.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = EvoraSpacing.lg, vertical = EvoraSpacing.xs),
                            horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                        ) {
                            items(state.providerAccounts, key = { it.accountId }) { acc ->
                                FilterChip(
                                    selected = state.activeAccount?.accountId == acc.accountId,
                                    onClick = { onIntent(CloudDriveBrowserIntent.SelectAccount(acc.accountId)) },
                                    label = { Text(acc.email) },
                                    modifier = Modifier.heightIn(min = EvoraSpacing.minTouchTarget),
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            state.transferProgress?.let { prog ->
                Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(EvoraSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val verb = if (prog.isUpload) "Uploading" else "Downloading"
                        Text(
                            text = "$verb ${prog.itemName} (${(prog.fraction * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        LinearProgressIndicator(
                            progress = { prog.fraction },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            if (state.activeAccount == null) {
                EmptyStateView(
                    title = when (state.selectedProvider) {
                        CloudProvider.GOOGLE_DRIVE -> stringResource(R.string.evora_cloud_google_drive)
                        CloudProvider.ONE_DRIVE -> stringResource(R.string.evora_cloud_onedrive)
                    },
                    description = "Connect your account to browse remote folders, edit offline, and upload changes.",
                    icon = Icons.Outlined.CloudQueue,
                    primaryActionLabel = stringResource(R.string.evora_cloud_connect),
                    onPrimaryAction = { onIntent(CloudDriveBrowserIntent.ConnectProviderClicked) },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = EvoraSpacing.lg, vertical = EvoraSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { onIntent(CloudDriveBrowserIntent.SearchQueryChanged(it)) },
                        placeholder = { Text(stringResource(R.string.evora_cloud_search_hint)) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = { onIntent(CloudDriveBrowserIntent.DisconnectAccount(state.activeAccount!!.accountId)) },
                        modifier = Modifier.heightIn(min = EvoraSpacing.minTouchTarget),
                    ) {
                        Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.evora_cloud_disconnect), color = MaterialTheme.colorScheme.error)
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = EvoraSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items(state.breadcrumbs.size) { idx ->
                        val crumb = state.breadcrumbs[idx]
                        val isLast = idx == state.breadcrumbs.lastIndex
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onIntent(CloudDriveBrowserIntent.NavigateBreadcrumb(crumb.pathOrUri)) }) {
                                Text(
                                    text = crumb.title,
                                    fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                            if (!isLast) {
                                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                when {
                    state.isLoading -> {
                        Box(modifier = Modifier.padding(EvoraSpacing.lg)) {
                            ShimmerDocListSkeleton(itemCount = 5)
                        }
                    }
                    state.errorMessage != null -> {
                        Column(modifier = Modifier.padding(EvoraSpacing.lg)) {
                            ErrorStateCard(
                                title = "Cloud Sync Error",
                                description = state.errorMessage,
                                actionLabel = "Retry",
                                onActionClick = { onIntent(CloudDriveBrowserIntent.Refresh) },
                            )
                        }
                    }
                    state.items.isEmpty() -> {
                        EmptyStateView(
                            title = stringResource(R.string.evora_cloud_empty),
                            description = "Upload a document from your device or navigate back.",
                            icon = Icons.Outlined.Folder,
                            primaryActionLabel = stringResource(R.string.evora_cloud_upload),
                            onPrimaryAction = { onIntent(CloudDriveBrowserIntent.UploadFileClicked) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(EvoraSpacing.lg),
                            verticalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                        ) {
                            items(state.items, key = { it.id }) { item ->
                                CloudItemRow(
                                    item = item,
                                    onClick = { onIntent(CloudDriveBrowserIntent.OpenCloudItem(item)) },
                                )
                            }
                            if (state.nextPageToken != null) {
                                item(key = "load_more") {
                                    EvoraOutlinedButton(
                                        text = if (state.isLoadingMore) "Loading…" else "Load More",
                                        onClick = { onIntent(CloudDriveBrowserIntent.LoadNextPage) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudItemRow(
    item: CloudItem,
    onClick: () -> Unit,
) {
    val extra = EvoraThemeTokens.colors
    val ext = item.name.substringAfterLast('.', "").lowercase()
    val fmt = DocFormat.fromExtension(ext) ?: DocFormat.TEXT

    Surface(
        shape = EvoraRadii.md,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clip(EvoraRadii.md)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = EvoraSpacing.md, vertical = EvoraSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.isFolder) {
                Surface(
                    shape = EvoraRadii.sm,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            } else {
                FormatBadge(format = fmt, extension = ext)
            }
            Spacer(modifier = Modifier.width(EvoraSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = if (item.isFolder) {
                    formatEpochShort(item.modifiedAtEpochMs).ifBlank { "Folder" }
                } else {
                    "${formatByteSize(item.sizeBytes)} · ${formatEpochShort(item.modifiedAtEpochMs)}"
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    color = extra.textMuted,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun launchGoogleDriveAuthorization(
    context: Context,
    onLaunchIntentSender: (android.app.PendingIntent) -> Unit,
    onTokenReady: (token: String, email: String, name: String) -> Unit,
    onFailure: (String) -> Unit,
) {
    val scopes = listOf(
        Scope("https://www.googleapis.com/auth/drive.file"),
        Scope("https://www.googleapis.com/auth/drive.readonly"),
    )
    val request = AuthorizationRequest.builder()
        .setRequestedScopes(scopes)
        .build()

    Identity.getAuthorizationClient(context)
        .authorize(request)
        .addOnSuccessListener { result ->
            val pending = result.pendingIntent
            val token = result.accessToken
            if (result.hasResolution() && pending != null) {
                onLaunchIntentSender(pending)
            } else if (!token.isNullOrBlank()) {
                val email = result.toGoogleSignInAccount()?.email ?: "google_user@drive"
                val name = result.toGoogleSignInAccount()?.displayName ?: email
                onTokenReady(token, email, name)
            } else {
                onFailure("Configure Google OAuth client for package com.evora.technologies.office to authorize Drive.")
            }
        }
        .addOnFailureListener { ex ->
            onFailure(ex.message ?: "Google Drive authorization failed")
        }
}

private fun launchOneDriveMsal(
    activity: Activity,
    onTokenReady: (token: String, accountId: String, email: String, name: String) -> Unit,
    onFailure: (String) -> Unit,
) {
    val clientIdRes = activity.resources.getIdentifier("evora_onedrive_client_id", "string", activity.packageName)
    val configuredClientId = if (clientIdRes != 0) activity.getString(clientIdRes).trim() else ""
    if (configuredClientId.isBlank()) {
        onFailure("Configure evora_onedrive_client_id and msal_config.json to sign in to OneDrive.")
        return
    }
    val configId = activity.resources.getIdentifier("msal_config", "raw", activity.packageName)
    if (configId == 0) {
        onFailure("MSAL configuration (res/raw/msal_config.json) missing")
        return
    }
    PublicClientApplication.createMultipleAccountPublicClientApplication(
        activity.applicationContext,
        configId,
        object : com.microsoft.identity.client.IPublicClientApplication.IMultipleAccountApplicationCreatedListener {
            override fun onCreated(application: IMultipleAccountPublicClientApplication) {
                val params = AcquireTokenParameters.Builder()
                    .startAuthorizationFromActivity(activity)
                    .withScopes(listOf("Files.ReadWrite.All", "User.Read"))
                    .withCallback(
                        object : AuthenticationCallback {
                            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                                val acc = authenticationResult.account
                                onTokenReady(
                                    authenticationResult.accessToken,
                                    acc.id,
                                    acc.username,
                                    acc.username,
                                )
                            }

                            override fun onError(exception: MsalException) {
                                onFailure(exception.message ?: "MSAL sign-in error")
                            }

                            override fun onCancel() {
                                onFailure("OneDrive sign-in cancelled")
                            }
                        },
                    )
                    .build()
                application.acquireToken(params)
            }

            override fun onError(exception: MsalException) {
                onFailure(exception.message ?: "Unable to initialize MSAL")
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun CloudDriveBrowserPreview() {
    EvoraTheme {
        CloudDriveBrowserScreen(
            state = CloudDriveBrowserState(selectedProvider = CloudProvider.GOOGLE_DRIVE),
            onIntent = {},
        )
    }
}
