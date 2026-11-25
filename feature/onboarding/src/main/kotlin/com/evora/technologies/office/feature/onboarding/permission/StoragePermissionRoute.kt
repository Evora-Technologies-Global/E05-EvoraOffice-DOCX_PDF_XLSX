package com.evora.technologies.office.feature.onboarding.permission

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.office.core.designsystem.component.EvoraOutlinedButton
import com.evora.technologies.office.core.designsystem.component.EvoraPrimaryButton
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraTheme
import com.evora.technologies.office.core.mvi.CollectEffects

@Composable
fun StoragePermissionRoute(
    onNavigateHome: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StoragePermissionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val allFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
        viewModel.onIntent(StoragePermissionIntent.PermissionSystemResult(granted))
    }

    val legacyStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onIntent(StoragePermissionIntent.PermissionSystemResult(granted))
    }

    val safDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
        viewModel.onIntent(StoragePermissionIntent.SafDocumentPicked(uri?.toString()))
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            StoragePermissionEffect.RequestSystemAllFilesPermission -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        viewModel.onIntent(StoragePermissionIntent.PermissionSystemResult(true))
                    } else {
                        val pkgIntent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        runCatching { allFilesLauncher.launch(pkgIntent) }.onFailure {
                            val fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            runCatching { allFilesLauncher.launch(fallbackIntent) }
                                .onFailure { viewModel.onIntent(StoragePermissionIntent.PermissionSystemResult(false)) }
                        }
                    }
                } else {
                    legacyStorageLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            StoragePermissionEffect.LaunchSafDocumentPicker -> {
                safDocumentLauncher.launch(arrayOf("*/*"))
            }
            StoragePermissionEffect.NavigateToHome -> onNavigateHome()
            is StoragePermissionEffect.ShowMessage -> onShowMessage(effect.message)
        }
    }

    StoragePermissionScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
internal fun StoragePermissionScreen(
    state: StoragePermissionState,
    onIntent: (StoragePermissionIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(EvoraSpacing.xl),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.padding(top = EvoraSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(EvoraRadii.lg)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(modifier = Modifier.height(EvoraSpacing.lg))
            Text(
                text = "Manage Your Local Documents",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(EvoraSpacing.sm))
            Text(
                text = "Grant All-Files Access to automatically index PDF, Word, Excel, PPT, HWP, and ZIP/7z files across device folders.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(EvoraSpacing.lg))
            Surface(
                shape = EvoraRadii.md,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "100% Offline Promise: Files never leave your phone. Zero server uploads.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(EvoraSpacing.lg),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(EvoraSpacing.md)) {
            EvoraPrimaryButton(
                text = "Allow All-Files Access",
                onClick = { onIntent(StoragePermissionIntent.AllowAllFilesClicked) },
                isLoading = state.isFinalizing,
            )
            EvoraOutlinedButton(
                text = "Use Folder Picker (SAF) Instead",
                onClick = { onIntent(StoragePermissionIntent.UseSafPickerClicked) },
                enabled = !state.isFinalizing,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StoragePermissionPreview() {
    EvoraTheme {
        StoragePermissionScreen(
            state = StoragePermissionState(),
            onIntent = {},
        )
    }
}
