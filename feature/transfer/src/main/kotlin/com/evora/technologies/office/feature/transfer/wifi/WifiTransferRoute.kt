package com.evora.technologies.office.feature.transfer.wifi

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.office.core.designsystem.R
import com.evora.technologies.office.core.designsystem.component.ErrorStateCard
import com.evora.technologies.office.core.designsystem.component.EvoraIconButton
import com.evora.technologies.office.core.designsystem.component.EvoraOutlinedButton
import com.evora.technologies.office.core.designsystem.component.EvoraPrimaryButton
import com.evora.technologies.office.core.designsystem.component.formatByteSize
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraTabularMonoStyle
import com.evora.technologies.office.core.designsystem.theme.EvoraTheme
import com.evora.technologies.office.core.designsystem.theme.EvoraThemeTokens
import com.evora.technologies.office.core.model.WifiTransferServerState
import com.evora.technologies.office.core.mvi.CollectEffects
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun WifiTransferRoute(
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WifiTransferViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        viewModel.onIntent(WifiTransferIntent.ToggleServerClicked)
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            WifiTransferEffect.NavigateBack -> onBack()
            is WifiTransferEffect.CopyToClipboard -> {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                cm?.setPrimaryClip(ClipData.newPlainText(effect.label, effect.value))
                onShowMessage("Copied ${effect.value}")
            }
            is WifiTransferEffect.ShowMessage -> onShowMessage(effect.message)
        }
    }

    WifiTransferScreen(
        state = state,
        onToggleWithPermissionCheck = {
            val needsPermission = !state.serverState.isRunning &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            if (needsPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.onIntent(WifiTransferIntent.ToggleServerClicked)
            }
        },
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
internal fun WifiTransferScreen(
    state: WifiTransferUiState,
    onToggleWithPermissionCheck: () -> Unit,
    onIntent: (WifiTransferIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = EvoraThemeTokens.colors
    val srv = state.serverState

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = EvoraSpacing.xs, vertical = EvoraSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EvoraIconButton(onClick = { onIntent(WifiTransferIntent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = stringResource(R.string.evora_wifi_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Surface(
                        shape = EvoraRadii.full,
                        color = if (srv.isRunning) extra.successContainer else extra.surfaceSubtle,
                        modifier = Modifier.padding(end = EvoraSpacing.md),
                    ) {
                        Text(
                            text = if (srv.isRunning) {
                                stringResource(R.string.evora_wifi_running)
                            } else {
                                stringResource(R.string.evora_wifi_stopped)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (srv.isRunning) extra.onSuccessContainer else extra.textMuted,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(EvoraSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(EvoraSpacing.md),
        ) {
            srv.errorMessage?.let { err ->
                item(key = "err") {
                    ErrorStateCard(
                        title = "Transfer Server Error",
                        description = err,
                        actionLabel = stringResource(R.string.evora_wifi_start),
                        onActionClick = onToggleWithPermissionCheck,
                    )
                }
            }

            item(key = "control_card") {
                Surface(
                    shape = EvoraRadii.lg,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(EvoraSpacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(EvoraSpacing.md),
                    ) {
                        Surface(
                            shape = EvoraRadii.lg,
                            color = if (srv.isRunning) extra.successContainer else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(64.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (srv.isRunning) Icons.Outlined.Wifi else Icons.Outlined.WifiOff,
                                    contentDescription = null,
                                    tint = if (srv.isRunning) extra.onSuccessContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }

                        if (srv.isRunning) {
                            val qrBitmap = remember(srv.httpUrl) { generateQrBitmap(srv.httpUrl, 440) }
                            if (qrBitmap != null) {
                                Surface(
                                    shape = EvoraRadii.md,
                                    color = androidx.compose.ui.graphics.Color.White,
                                ) {
                                    Image(
                                        bitmap = qrBitmap.asImageBitmap(),
                                        contentDescription = "QR code for ${srv.httpUrl}",
                                        modifier = Modifier
                                            .size(168.dp)
                                            .padding(10.dp),
                                    )
                                }
                            }

                            Text(
                                text = srv.httpUrl,
                                style = EvoraTabularMonoStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize),
                                color = MaterialTheme.colorScheme.primary,
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                            ) {
                                EvoraOutlinedButton(
                                    text = stringResource(R.string.evora_wifi_copy_url),
                                    onClick = { onIntent(WifiTransferIntent.CopyUrlClicked(srv.httpUrl)) },
                                    modifier = Modifier.weight(1f),
                                )
                                EvoraPrimaryButton(
                                    text = stringResource(R.string.evora_wifi_stop),
                                    isLoading = state.isToggling,
                                    onClick = onToggleWithPermissionCheck,
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            if (srv.config.ftpEnabled && srv.ftpUrl.isNotBlank()) {
                                Surface(
                                    shape = EvoraRadii.sm,
                                    color = extra.surfaceSubtle,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = EvoraSpacing.md, vertical = EvoraSpacing.sm),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = srv.ftpUrl,
                                            style = EvoraTabularMonoStyle,
                                            modifier = Modifier.weight(1f),
                                        )
                                        EvoraIconButton(onClick = { onIntent(WifiTransferIntent.CopyUrlClicked(srv.ftpUrl)) }) {
                                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy FTP URL")
                                        }
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Outlined.Timer, contentDescription = null, tint = extra.textMuted, modifier = Modifier.size(16.dp))
                                val mins = srv.idleSecondsRemaining / 60
                                val secs = srv.idleSecondsRemaining % 60
                                Text(
                                    text = "${stringResource(R.string.evora_wifi_idle_prefix)}: %02d:%02d".format(mins, secs),
                                    style = EvoraTabularMonoStyle,
                                    color = extra.textMuted,
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.evora_wifi_tip),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            EvoraPrimaryButton(
                                text = stringResource(R.string.evora_wifi_start),
                                isLoading = state.isToggling,
                                onClick = onToggleWithPermissionCheck,
                            )
                        }
                    }
                }
            }

            item(key = "settings_card") {
                Surface(
                    shape = EvoraRadii.lg,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(EvoraSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(EvoraSpacing.md),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.evora_wifi_ftp_toggle), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Expose local FTP endpoint (port ${srv.config.ftpPort})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = extra.textMuted,
                                )
                            }
                            Switch(
                                checked = srv.config.ftpEnabled,
                                onCheckedChange = { onIntent(WifiTransferIntent.ToggleFtp(it)) },
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.evora_wifi_readonly), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Allow browser/FTP downloads only; block uploads",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = extra.textMuted,
                                )
                            }
                            Switch(
                                checked = srv.config.readOnly,
                                onCheckedChange = { onIntent(WifiTransferIntent.ToggleReadOnly(it)) },
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.evora_wifi_pin_lock), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Require PIN header/FTP password on LAN requests",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = extra.textMuted,
                                )
                            }
                            Switch(
                                checked = state.pinLockEnabled,
                                onCheckedChange = { onIntent(WifiTransferIntent.TogglePinLock(it)) },
                            )
                        }
                        if (state.pinLockEnabled) {
                            OutlinedTextField(
                                value = state.pinInput,
                                onValueChange = { onIntent(WifiTransferIntent.UpdatePinCode(it)) },
                                label = { Text("Access PIN") },
                                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            item(key = "tip_banner") {
                Surface(
                    shape = EvoraRadii.md,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(EvoraSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(EvoraSpacing.sm))
                        Text(
                            text = stringResource(R.string.evora_wifi_tip),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            if (srv.transferLogs.isNotEmpty()) {
                item(key = "logs_header") {
                    Text("Recent Transfers", style = MaterialTheme.typography.titleMedium)
                }
                items(srv.transferLogs, key = { it.id }) { log ->
                    Surface(
                        shape = EvoraRadii.md,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(EvoraSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(log.targetName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${log.protocol} · ${log.action} · ${formatByteSize(log.bytes)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = extra.textMuted,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun generateQrBitmap(content: String, sizePx: Int): Bitmap? {
    if (content.isBlank()) return null
    return runCatching {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        bmp
    }.getOrNull()
}

@Preview(showBackground = true)
@Composable
private fun WifiTransferPreview() {
    EvoraTheme {
        WifiTransferScreen(
            state = WifiTransferUiState(
                serverState = WifiTransferServerState(
                    isRunning = true,
                    ipAddress = "192.168.1.45",
                    httpUrl = "http://192.168.1.45:8998",
                    ftpUrl = "ftp://192.168.1.45:8999",
                    idleSecondsRemaining = 595,
                ),
            ),
            onToggleWithPermissionCheck = {},
            onIntent = {},
        )
    }
}
