package com.evora.technologies.office.feature.tools.pdf

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.office.core.designsystem.component.ErrorStateCard
import com.evora.technologies.office.core.designsystem.component.EvoraIconButton
import com.evora.technologies.office.core.designsystem.component.EvoraOutlinedButton
import com.evora.technologies.office.core.designsystem.component.EvoraPrimaryButton
import com.evora.technologies.office.core.designsystem.component.FormatBadge
import com.evora.technologies.office.core.designsystem.component.PasswordUnlockDialog
import com.evora.technologies.office.core.designsystem.component.formatByteSize
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraTabularMonoStyle
import com.evora.technologies.office.core.designsystem.theme.EvoraTheme
import com.evora.technologies.office.core.designsystem.theme.EvoraThemeTokens
import com.evora.technologies.office.core.model.CompressQuality
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.PageSizePreset
import com.evora.technologies.office.core.model.PdfToolKind
import com.evora.technologies.office.core.model.SplitMode
import com.evora.technologies.office.core.mvi.CollectEffects
import java.io.File

@Composable
fun PdfToolConfigRoute(
    toolKind: PdfToolKind,
    preselectedUri: String? = null,
    onBack: () -> Unit,
    onOpenOutput: (uriString: String, displayName: String, format: DocFormat) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PdfToolConfigViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(toolKind, preselectedUri) {
        viewModel.onIntent(PdfToolConfigIntent.Initialize(toolKind, preselectedUri))
    }

    val multiFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            val files = uris.map { uri ->
                val name = resolveDisplayName(context, uri)
                ToolInputFile(uri.toString(), name)
            }
            viewModel.onIntent(PdfToolConfigIntent.AddExternalFiles(files))
        }
    }

    val p12Picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.onIntent(PdfToolConfigIntent.SetP12Certificate(uri.toString(), resolveDisplayName(context, uri)))
        }
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            PdfToolConfigEffect.NavigateBack -> onBack()
            is PdfToolConfigEffect.OpenOutputFile -> {
                val ext = effect.displayName.substringAfterLast('.', "pdf").lowercase()
                val fmt = DocFormat.fromExtension(ext) ?: DocFormat.PDF
                onOpenOutput(effect.uriString, effect.displayName, fmt)
            }
            is PdfToolConfigEffect.ShareOutputFile -> shareFileUri(context, effect.uriString, effect.displayName, onShowMessage)
            is PdfToolConfigEffect.ShowMessage -> onShowMessage(effect.message)
        }
    }

    PdfToolConfigScreen(
        state = state,
        onPickExternalFiles = {
            val mime = if (state.toolKind == PdfToolKind.IMAGE_TO_PDF) arrayOf("image/*") else arrayOf("*/*")
            multiFilePicker.launch(mime)
        },
        onPickP12Certificate = { p12Picker.launch(arrayOf("*/*")) },
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun PdfToolConfigScreen(
    state: PdfToolConfigState,
    onPickExternalFiles: () -> Unit,
    onPickP12Certificate: () -> Unit,
    onIntent: (PdfToolConfigIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = EvoraThemeTokens.colors
    val title = state.toolKind.displayTitle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .safeDrawingPadding()
                        .heightIn(min = 56.dp)
                        .padding(horizontal = EvoraSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EvoraIconButton(onClick = { onIntent(PdfToolConfigIntent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                }
            }
        },
    ) { innerPadding ->
        when {
            state.isProcessing -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(EvoraSpacing.xl),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(EvoraSpacing.lg),
                    ) {
                        CircularProgressIndicator(
                            progress = { state.progress.coerceIn(0.05f, 1f) },
                            modifier = Modifier.size(72.dp),
                        )
                        Text(
                            text = "${(state.progress * 100).toInt()}%",
                            style = EvoraTabularMonoStyle.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = "Processing locally offline...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        EvoraOutlinedButton(
                            text = "Cancel",
                            onClick = { onIntent(PdfToolConfigIntent.CancelExecution) },
                        )
                    }
                }
            }

            state.result != null -> {
                val res = state.result
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(EvoraSpacing.xl),
                    verticalArrangement = Arrangement.spacedBy(EvoraSpacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = extra.success,
                        modifier = Modifier.size(64.dp),
                    )
                    Text(res.outputDisplayName, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = res.summaryMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        shape = EvoraRadii.md,
                        color = extra.successContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(EvoraSpacing.lg),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("Before -> After", style = MaterialTheme.typography.labelMedium, color = extra.onSuccessContainer)
                                Text(
                                    "${formatByteSize(res.beforeSizeBytes)} -> ${formatByteSize(res.afterSizeBytes)}",
                                    style = EvoraTabularMonoStyle,
                                    color = extra.onSuccessContainer,
                                )
                            }
                            if (res.savedPercent > 0) {
                                Surface(shape = EvoraRadii.full, color = extra.success) {
                                    Text(
                                        text = "-${res.savedPercent}%",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = extra.onSuccess,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }

                    EvoraPrimaryButton(
                        text = "Open File",
                        onClick = { onIntent(PdfToolConfigIntent.OpenResultClicked) },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                    ) {
                        EvoraOutlinedButton(
                            text = "Share",
                            onClick = { onIntent(PdfToolConfigIntent.ShareResultClicked) },
                            modifier = Modifier.weight(1f),
                        )
                        EvoraOutlinedButton(
                            text = "Run Another",
                            onClick = { onIntent(PdfToolConfigIntent.ResetForAnotherRun) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(EvoraSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(EvoraSpacing.md),
                ) {
                    state.errorMessage?.let { err ->
                        item(key = "err") {
                            ErrorStateCard(
                                title = "Operation Notice",
                                description = err,
                                actionLabel = "Dismiss",
                                onActionClick = { onIntent(PdfToolConfigIntent.ResetForAnotherRun) },
                            )
                        }
                    }

                    if (state.toolKind != PdfToolKind.TEXT_TO_PDF) {
                        item(key = "input_files") {
                            Surface(
                                shape = EvoraRadii.md,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(EvoraSpacing.md),
                                    verticalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("Selected Files (${state.selectedInputs.size})", style = MaterialTheme.typography.titleSmall)
                                        Row {
                                            if (state.toolKind != PdfToolKind.IMAGE_TO_PDF) {
                                                TextButton(onClick = { onIntent(PdfToolConfigIntent.OpenLibraryPicker) }) {
                                                    Icon(Icons.Outlined.FolderOpen, null)
                                                    Text(" Library")
                                                }
                                            }
                                            TextButton(onClick = onPickExternalFiles) {
                                                Icon(Icons.Outlined.Add, null)
                                                Text(" Pick")
                                            }
                                        }
                                    }
                                    state.selectedInputs.forEachIndexed { idx, file ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                "${idx + 1}. ${file.displayName}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f),
                                            )
                                            if (state.toolKind == PdfToolKind.MERGE || state.toolKind == PdfToolKind.IMAGE_TO_PDF) {
                                                EvoraIconButton(
                                                    onClick = { onIntent(PdfToolConfigIntent.MoveInput(idx, idx - 1)) },
                                                    enabled = idx > 0,
                                                ) {
                                                    Icon(Icons.Outlined.ArrowUpward, "Move up")
                                                }
                                                EvoraIconButton(
                                                    onClick = { onIntent(PdfToolConfigIntent.MoveInput(idx, idx + 1)) },
                                                    enabled = idx < state.selectedInputs.lastIndex,
                                                ) {
                                                    Icon(Icons.Outlined.ArrowDownward, "Move down")
                                                }
                                            }
                                            EvoraIconButton(onClick = { onIntent(PdfToolConfigIntent.RemoveInputAt(idx)) }) {
                                                Icon(Icons.Outlined.Close, "Remove")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Dynamic tool config section
                    item(key = "tool_config") {
                        ToolConfigSection(
                            state = state,
                            onPickP12Certificate = onPickP12Certificate,
                            onIntent = onIntent,
                        )
                    }

                    item(key = "run_cta") {
                        EvoraPrimaryButton(
                            text = "Run ${title}",
                            onClick = { onIntent(PdfToolConfigIntent.ExecuteClicked) },
                        )
                    }
                }
            }
        }
    }

    if (state.showLibraryPicker) {
        val docs = if (state.toolKind == PdfToolKind.DOC_TO_PDF) {
            state.availableLibraryDocs.filter { it.format != DocFormat.PDF && it.format != DocFormat.ARCHIVE }
        } else {
            state.availableLibraryDocs.filter { it.format == DocFormat.PDF }
        }
        ModalBottomSheet(onDismissRequest = { onIntent(PdfToolConfigIntent.DismissLibraryPicker) }) {
            Column(modifier = Modifier.padding(EvoraSpacing.lg)) {
                Text("Select from Library", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(EvoraSpacing.sm))
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(docs, key = { it.id }) { doc ->
                        ListItem(
                            headlineContent = { Text(doc.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text(formatByteSize(doc.sizeBytes)) },
                            leadingContent = { FormatBadge(format = doc.format, extension = doc.extension) },
                            modifier = Modifier.clickable { onIntent(PdfToolConfigIntent.AddLibraryDocument(doc)) },
                        )
                    }
                }
            }
        }
    }

    if (state.passwordPromptRequired) {
        PasswordUnlockDialog(
            fileName = state.selectedInputs.firstOrNull()?.displayName ?: "PDF",
            errorMessage = state.passwordPromptError,
            onUnlock = { onIntent(PdfToolConfigIntent.SubmitUnlockPassword(it)) },
            onDismiss = { onIntent(PdfToolConfigIntent.DismissUnlockDialog) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ToolConfigSection(
    state: PdfToolConfigState,
    onPickP12Certificate: () -> Unit,
    onIntent: (PdfToolConfigIntent) -> Unit,
) {
    Surface(
        shape = EvoraRadii.md,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(EvoraSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(EvoraSpacing.md),
        ) {
            when (state.toolKind) {
                PdfToolKind.MERGE, PdfToolKind.IMAGE_TO_PDF, PdfToolKind.TEXT_TO_PDF -> {
                    OutlinedTextField(
                        value = state.outputBaseName,
                        onValueChange = { onIntent(PdfToolConfigIntent.UpdateOutputName(it)) },
                        label = { Text("Output filename (.pdf)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (state.toolKind == PdfToolKind.IMAGE_TO_PDF || state.toolKind == PdfToolKind.TEXT_TO_PDF) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PageSizePreset.entries.forEach { preset ->
                                FilterChip(
                                    selected = state.pageSizePreset == preset,
                                    onClick = { onIntent(PdfToolConfigIntent.SelectPageSize(preset)) },
                                    label = { Text(preset.label) },
                                )
                            }
                        }
                    }
                    if (state.toolKind == PdfToolKind.IMAGE_TO_PDF) {
                        Text("Margins: ${state.imageMarginDp} pt")
                        Slider(
                            value = state.imageMarginDp.toFloat(),
                            onValueChange = { onIntent(PdfToolConfigIntent.UpdateMarginDp(it.toInt())) },
                            valueRange = 0f..48f,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Landscape Orientation")
                            Switch(checked = state.landscape, onCheckedChange = { onIntent(PdfToolConfigIntent.ToggleLandscape(it)) })
                        }
                    }
                    if (state.toolKind == PdfToolKind.TEXT_TO_PDF) {
                        OutlinedTextField(
                            value = state.plainTextInput,
                            onValueChange = { onIntent(PdfToolConfigIntent.UpdatePlainTextInput(it)) },
                            label = { Text("Text content to convert to PDF") },
                            minLines = 5,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                PdfToolKind.SPLIT -> {
                    Text("Split Mode (${state.sourceTotalPages} pages detected)", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SplitMode.entries.forEach { mode ->
                            FilterChip(
                                selected = state.splitMode == mode,
                                onClick = { onIntent(PdfToolConfigIntent.SelectSplitMode(mode)) },
                                label = {
                                    Text(
                                        when (mode) {
                                            SplitMode.RANGE -> "Page Range"
                                            SplitMode.EVERY_N -> "Every N Pages"
                                            SplitMode.SELECTED_PAGES -> "Select Pages"
                                        },
                                    )
                                },
                            )
                        }
                    }
                    when (state.splitMode) {
                        SplitMode.RANGE -> OutlinedTextField(
                            value = state.splitRangeExpression,
                            onValueChange = { onIntent(PdfToolConfigIntent.UpdateSplitRange(it)) },
                            label = { Text("Pages (e.g. 1-3, 5)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        SplitMode.EVERY_N -> {
                            Text("Split every ${state.splitEveryN} page(s)")
                            Slider(
                                value = state.splitEveryN.toFloat(),
                                onValueChange = { onIntent(PdfToolConfigIntent.UpdateSplitEveryN(it.toInt())) },
                                valueRange = 1f..20f,
                            )
                        }
                        SplitMode.SELECTED_PAGES -> {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (i in 0 until state.sourceTotalPages.coerceIn(0, 60)) {
                                    FilterChip(
                                        selected = i in state.splitSelectedPages,
                                        onClick = { onIntent(PdfToolConfigIntent.ToggleSplitPage(i)) },
                                        label = { Text("${i + 1}") },
                                    )
                                }
                            }
                        }
                    }
                }

                PdfToolKind.COMPRESS -> {
                    Text("Compression Preset", style = MaterialTheme.typography.titleSmall)
                    CompressQuality.entries.forEach { q ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onIntent(PdfToolConfigIntent.SelectCompressQuality(q)) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = state.compressQuality == q,
                                onClick = { onIntent(PdfToolConfigIntent.SelectCompressQuality(q)) },
                            )
                            Column {
                                Text(q.label, style = MaterialTheme.typography.titleSmall)
                                Text(q.description, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                PdfToolKind.WATERMARK -> {
                    OutlinedTextField(
                        value = state.watermarkText,
                        onValueChange = { onIntent(PdfToolConfigIntent.UpdateWatermarkText(it)) },
                        label = { Text("Watermark text") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Size: ${state.watermarkFontSize.toInt()} pt · Opacity: ${(state.watermarkOpacity * 100).toInt()}%")
                    Slider(
                        value = state.watermarkFontSize,
                        onValueChange = { onIntent(PdfToolConfigIntent.UpdateWatermarkSize(it)) },
                        valueRange = 18f..72f,
                    )
                    Slider(
                        value = state.watermarkOpacity,
                        onValueChange = { onIntent(PdfToolConfigIntent.UpdateWatermarkOpacity(it)) },
                        valueRange = 0.10f..1.0f,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(-45f, 0f, 45f).forEach { ang ->
                            FilterChip(
                                selected = state.watermarkAngle == ang,
                                onClick = { onIntent(PdfToolConfigIntent.UpdateWatermarkAngle(ang)) },
                                label = { Text("${ang.toInt()}°") },
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Tiled Grid Watermark")
                        Switch(checked = state.watermarkTiled, onCheckedChange = { onIntent(PdfToolConfigIntent.ToggleWatermarkTiled(it)) })
                    }
                }

                PdfToolKind.SET_PASSWORD -> {
                    OutlinedTextField(
                        value = state.userPassword,
                        onValueChange = { onIntent(PdfToolConfigIntent.UpdateUserPassword(it)) },
                        label = { Text("User open password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.ownerPassword,
                        onValueChange = { onIntent(PdfToolConfigIntent.UpdateOwnerPassword(it)) },
                        label = { Text("Owner permission password (optional)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Use AES-256 Encryption")
                        Switch(checked = state.useAes256, onCheckedChange = { onIntent(PdfToolConfigIntent.ToggleAes256(it)) })
                    }
                }

                PdfToolKind.REMOVE_PASSWORD -> {
                    OutlinedTextField(
                        value = state.currentPdfPassword,
                        onValueChange = { onIntent(PdfToolConfigIntent.UpdateCurrentPdfPassword(it)) },
                        label = { Text("Current PDF password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                PdfToolKind.PKCS7_SIGN -> {
                    EvoraOutlinedButton(
                        text = if (state.p12CertName.isBlank()) "Pick .p12 / .pfx Keystore" else "Cert: ${state.p12CertName}",
                        onClick = onPickP12Certificate,
                    )
                    OutlinedTextField(
                        value = state.p12Password,
                        onValueChange = { onIntent(PdfToolConfigIntent.UpdateP12Password(it)) },
                        label = { Text("Keystore Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.signReason,
                        onValueChange = { onIntent(PdfToolConfigIntent.UpdateSignReason(it)) },
                        label = { Text("Reason") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    EvoraOutlinedButton(
                        text = "Inspect Existing PKCS#7 Signatures",
                        onClick = { onIntent(PdfToolConfigIntent.InspectExistingCertificates) },
                    )
                    state.inspectedCerts.forEach { cert ->
                        Surface(shape = EvoraRadii.sm, color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(EvoraSpacing.sm), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Outlined.VerifiedUser, null)
                                    Text(cert.signerName, style = MaterialTheme.typography.titleSmall)
                                }
                                Text("Issuer: ${cert.issuerName}", style = MaterialTheme.typography.labelMedium)
                                Text("Integrity: ${if (cert.isIntegrityValid) "Verified" else "Unverified"} · Validity Window: ${if (cert.isCurrentlyWithinValidity) "Valid" else "Expired"}", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                PdfToolKind.PDF_TO_IMAGES -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Stitch into single Long PNG")
                        Switch(checked = state.stitchedLongImage, onCheckedChange = { onIntent(PdfToolConfigIntent.ToggleStitchedLongImage(it)) })
                    }
                }

                PdfToolKind.PDF_TO_DOCX -> {
                    Text(
                        text = "Offline PDF -> DOCX ready (uses PDFBox + ML Kit OCR fallback).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                PdfToolKind.DOC_TO_PDF -> {
                    Text(
                        "Converts Word, Excel, PowerPoint, ODF, HWP, or Text files locally into paginated A4 PDF.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun PdfToolKind.displayTitle(): String = when (this) {
    PdfToolKind.MERGE -> "Merge PDF"
    PdfToolKind.SPLIT -> "Split PDF"
    PdfToolKind.COMPRESS -> "Compress PDF"
    PdfToolKind.WATERMARK -> "Watermark PDF"
    PdfToolKind.SET_PASSWORD -> "Set PDF Password"
    PdfToolKind.REMOVE_PASSWORD -> "Remove Password"
    PdfToolKind.PKCS7_SIGN -> "PKCS#7 Digital Sign"
    PdfToolKind.IMAGE_TO_PDF -> "Image to PDF"
    PdfToolKind.TEXT_TO_PDF -> "Text to PDF"
    PdfToolKind.DOC_TO_PDF -> "Docs to PDF"
    PdfToolKind.PDF_TO_IMAGES -> "PDF to Images"
    PdfToolKind.PDF_TO_DOCX -> "PDF to DOCX"
}

private fun resolveDisplayName(context: android.content.Context, uri: Uri): String =
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: "file"

private fun shareFileUri(
    context: android.content.Context,
    uriString: String,
    displayName: String,
    onShowMessage: (String) -> Unit,
) {
    runCatching {
        val parsed = Uri.parse(uriString)
        val shareUri = if (parsed.scheme == "file" && parsed.path != null) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(parsed.path!!))
        } else {
            parsed
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share $displayName"))
    }.onFailure {
        onShowMessage("Unable to share $displayName")
    }
}

@Preview(showBackground = true)
@Composable
private fun PdfToolConfigPreview() {
    EvoraTheme {
        PdfToolConfigScreen(
            state = PdfToolConfigState(toolKind = PdfToolKind.SPLIT, sourceTotalPages = 5),
            onPickExternalFiles = {},
            onPickP12Certificate = {},
            onIntent = {},
        )
    }
}
