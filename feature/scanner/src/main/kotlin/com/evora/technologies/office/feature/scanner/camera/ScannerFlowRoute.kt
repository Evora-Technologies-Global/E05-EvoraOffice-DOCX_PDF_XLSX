package com.evora.technologies.office.feature.scanner.camera

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview as CameraXPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.GridOff
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.office.core.designsystem.component.ErrorStateCard
import com.evora.technologies.office.core.designsystem.component.EvoraIconButton
import com.evora.technologies.office.core.designsystem.component.EvoraOutlinedButton
import com.evora.technologies.office.core.designsystem.component.EvoraPrimaryButton
import com.evora.technologies.office.core.designsystem.component.EvoraRewardGateSheet
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraTheme
import com.evora.technologies.office.core.designsystem.theme.EvoraThemeTokens
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.OcrScript
import com.evora.technologies.office.core.model.QrDecodedAction
import com.evora.technologies.office.core.model.ScanFilter
import com.evora.technologies.office.core.model.ScanMode
import com.evora.technologies.office.core.mvi.CollectEffects
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

@Composable
fun ScannerFlowRoute(
    initialMode: ScanMode = ScanMode.DOCUMENT,
    onBack: () -> Unit,
    onOpenPaywall: () -> Unit,
    onOpenDocument: (uriString: String, displayName: String, format: DocFormat) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScannerFlowViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(initialMode) {
        viewModel.onIntent(ScannerFlowIntent.InitializeMode(initialMode))
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        viewModel.onIntent(ScannerFlowIntent.CameraPermissionUpdated(granted))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onIntent(ScannerFlowIntent.CameraPermissionUpdated(granted))
    }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            val cached = copyUriToTempJpeg(context, uri)
            if (cached != null) {
                viewModel.onIntent(ScannerFlowIntent.ImageCaptured(cached))
            } else {
                onShowMessage("Unable to import selected image")
            }
        }
    }

    val mlkitDocScannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val res = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
            val pagePaths = res?.pages?.mapNotNull { page -> copyUriToTempJpeg(context, page.imageUri) }.orEmpty()
            if (pagePaths.isNotEmpty()) {
                viewModel.onIntent(ScannerFlowIntent.DocScannerPagesImported(pagePaths))
            }
        }
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            ScannerFlowEffect.NavigateBack -> onBack()
            ScannerFlowEffect.NavigateToPaywall -> onOpenPaywall()
            is ScannerFlowEffect.OpenDocument -> {
                val ext = effect.displayName.substringAfterLast('.', "pdf").lowercase()
                onOpenDocument(effect.uriString, effect.displayName, DocFormat.fromExtension(ext) ?: DocFormat.PDF)
            }
            is ScannerFlowEffect.CopyToClipboard -> {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                cm?.setPrimaryClip(ClipData.newPlainText("OCR Text", effect.text))
            }
            is ScannerFlowEffect.LaunchBrowserUrl -> {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(effect.url)))
                }.onFailure { onShowMessage("No browser available for URL") }
            }
            is ScannerFlowEffect.ShowMessage -> onShowMessage(effect.message)
        }
    }

    ScannerFlowScreen(
        state = state,
        onRequestCameraPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        onPickFromGallery = { galleryPicker.launch("image/*") },
        onLaunchMlKitDocScanner = {
            val activity = context as? Activity
            if (activity == null) {
                onShowMessage("Use live camera shutter below.")
            } else {
                val opts = GmsDocumentScannerOptions.Builder()
                    .setGalleryImportAllowed(true)
                    .setPageLimit(20)
                    .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                    .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                    .build()
                GmsDocumentScanning.getClient(opts)
                    .getStartScanIntent(activity)
                    .addOnSuccessListener { sender ->
                        mlkitDocScannerLauncher.launch(IntentSenderRequest.Builder(sender).build())
                    }
                    .addOnFailureListener {
                        onShowMessage("Google Play Document Scanner unavailable; using built-in CameraX capture.")
                    }
            }
        },
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ScannerFlowScreen(
    state: ScannerFlowState,
    onRequestCameraPermission: () -> Unit,
    onPickFromGallery: () -> Unit,
    onLaunchMlKitDocScanner: () -> Unit,
    onIntent: (ScannerFlowIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val extra = EvoraThemeTokens.colors
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraRef by remember { mutableStateOf<Camera?>(null) }

    LaunchedEffect(state.flashEnabled, cameraRef) {
        cameraRef?.cameraControl?.enableTorch(state.flashEnabled)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(color = Color(0xFF0F172A)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .safeDrawingPadding()
                        .heightIn(min = 56.dp)
                        .padding(horizontal = EvoraSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EvoraIconButton(onClick = { onIntent(ScannerFlowIntent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = Color.White)
                    }
                    Text(
                        text = state.mode.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    EvoraIconButton(onClick = { onIntent(ScannerFlowIntent.ToggleGrid) }) {
                        Icon(
                            imageVector = if (state.showGrid) Icons.Outlined.GridOn else Icons.Outlined.GridOff,
                            contentDescription = "Toggle grid",
                            tint = Color.White,
                        )
                    }
                    EvoraIconButton(onClick = { onIntent(ScannerFlowIntent.ToggleFlash) }) {
                        Icon(
                            imageVector = if (state.flashEnabled) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                            contentDescription = "Toggle flash",
                            tint = Color.White,
                        )
                    }
                    EvoraIconButton(onClick = onLaunchMlKitDocScanner) {
                        Icon(
                            imageVector = Icons.Outlined.DocumentScanner,
                            contentDescription = "Smart Edge Scanner",
                            tint = Color.White,
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(color = Color(0xFF0F172A)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(vertical = EvoraSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = EvoraSpacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                    ) {
                        itemsIndexed(ScanMode.entries, key = { _, m -> m.name }) { _, mode ->
                            val label = if (mode == ScanMode.ID_CARD && !state.proStatus.isUnlocked) {
                                "${mode.label} · PRO"
                            } else {
                                mode.label
                            }
                            FilterChip(
                                selected = state.mode == mode,
                                onClick = { onIntent(ScannerFlowIntent.SelectMode(mode)) },
                                label = { Text(label) },
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = EvoraSpacing.xl),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        EvoraIconButton(onClick = onPickFromGallery) {
                            Icon(Icons.Outlined.PhotoLibrary, "Pick from gallery", tint = Color.White)
                        }
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(4.dp, extra.brandAccent, CircleShape)
                                .clickable(enabled = state.cameraPermissionGranted && !state.isBusy) {
                                    val capture = imageCapture ?: return@clickable
                                    val dir = File(context.cacheDir, "scanner_raw").apply { mkdirs() }
                                    val target = File(dir, "cap_${System.currentTimeMillis()}.jpg")
                                    val opts = ImageCapture.OutputFileOptions.Builder(target).build()
                                    capture.takePicture(
                                        opts,
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                onIntent(ScannerFlowIntent.ImageCaptured(target.absolutePath))
                                            }
                                            override fun onError(exception: ImageCaptureException) {
                                                onIntent(ScannerFlowIntent.CameraPermissionUpdated(false))
                                            }
                                        },
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.CameraAlt, contentDescription = "Capture", tint = Color(0xFF0F172A))
                        }
                        if (state.pages.isNotEmpty() && state.mode == ScanMode.DOCUMENT) {
                            TextButton(
                                onClick = {
                                    onIntent(ScannerFlowIntent.ExportDocumentsPdf("Scan_${System.currentTimeMillis()}"))
                                },
                            ) {
                                Text("Save PDF (${state.pages.size})", color = extra.brandAccent)
                            }
                        } else {
                            Spacer(modifier = Modifier.size(48.dp))
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
                    .background(Color.Black),
            ) {
                if (state.cameraPermissionGranted) {
                    val executor = remember { Executors.newSingleThreadExecutor() }
                    val barcodeClient = remember { BarcodeScanning.getClient() }
                    DisposableEffect(Unit) {
                        onDispose {
                            barcodeClient.close()
                            executor.shutdown()
                        }
                    }
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener(
                                {
                                    runCatching {
                                        val provider = cameraProviderFuture.get()
                                        val preview = CameraXPreview.Builder().build().also {
                                            it.surfaceProvider = previewView.surfaceProvider
                                        }
                                        val cap = ImageCapture.Builder()
                                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                            .build()
                                        val analysis = ImageAnalysis.Builder()
                                            .setTargetResolution(Size(960, 720))
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()
                                        analysis.setAnalyzer(executor) { proxy ->
                                            analyzeQrFrame(proxy, state.mode == ScanMode.QR_BARCODE, barcodeClient) { raw ->
                                                onIntent(ScannerFlowIntent.LiveQrDetected(raw))
                                            }
                                        }
                                        provider.unbindAll()
                                        val cam = provider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview,
                                            cap,
                                            analysis,
                                        )
                                        imageCapture = cap
                                        cameraRef = cam
                                    }
                                },
                                ContextCompat.getMainExecutor(ctx),
                            )
                            previewView
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(EvoraSpacing.xl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            "Camera permission needed for live capture, or pick an image from Gallery.",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(EvoraSpacing.md))
                        EvoraPrimaryButton(text = "Grant Camera Permission", onClick = onRequestCameraPermission)
                    }
                }

                if (state.showGrid) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val lineColor = Color.White.copy(alpha = 0.25f)
                        drawLine(lineColor, Offset(size.width / 3f, 0f), Offset(size.width / 3f, size.height), 1.5f)
                        drawLine(lineColor, Offset(size.width * 2f / 3f, 0f), Offset(size.width * 2f / 3f, size.height), 1.5f)
                        drawLine(lineColor, Offset(0f, size.height / 3f), Offset(size.width, size.height / 3f), 1.5f)
                        drawLine(lineColor, Offset(0f, size.height * 2f / 3f), Offset(size.width, size.height * 2f / 3f), 1.5f)
                    }
                }

                if (state.mode == ScanMode.ID_CARD) {
                    Surface(
                        shape = EvoraRadii.full,
                        color = extra.brandAccent,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = EvoraSpacing.sm),
                    ) {
                        val stepText = when (state.idCardStep) {
                            1 -> "Step 1/2: Capture Front Side"
                            2 -> "Step 2/2: Capture Back Side"
                            else -> "Both Sides Ready -> Export A4"
                        }
                        Text(
                            text = stepText,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }

                if (state.isBusy) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            state.errorMessage?.let { err ->
                Box(modifier = Modifier.padding(EvoraSpacing.md)) {
                    ErrorStateCard(
                        title = "Scanner Notice",
                        description = err,
                        actionLabel = "Dismiss",
                        onActionClick = { onIntent(ScannerFlowIntent.DismissError) },
                    )
                }
            }

            // Mode-specific workspace below viewfinder
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(EvoraSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(EvoraSpacing.md),
            ) {
                when (state.mode) {
                    ScanMode.DOCUMENT -> {
                        if (state.pages.isEmpty()) {
                            Text(
                                "Tap the shutter button or pick from Gallery to scan documents. Apply filters, rotate, and export multi-page PDF.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text("Captured Pages (${state.pages.size})", style = MaterialTheme.typography.titleSmall)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsIndexed(state.pages, key = { _, p -> p.id }) { idx, _ ->
                                    FilterChip(
                                        selected = idx == state.activePageIndex,
                                        onClick = { onIntent(ScannerFlowIntent.SelectPageIndex(idx)) },
                                        label = { Text("Page ${idx + 1}") },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Outlined.Close,
                                                "Remove",
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable { onIntent(ScannerFlowIntent.RemovePageAt(idx)) },
                                            )
                                        },
                                    )
                                }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val currentFilter = state.pages.getOrNull(state.activePageIndex)?.filter
                                ScanFilter.entries.forEach { f ->
                                    FilterChip(
                                        selected = currentFilter == f,
                                        onClick = { onIntent(ScannerFlowIntent.ApplyFilterToActivePage(f)) },
                                        label = { Text(f.label) },
                                    )
                                }
                                FilterChip(
                                    selected = false,
                                    onClick = { onIntent(ScannerFlowIntent.RotateActivePage90) },
                                    label = { Text("Rotate 90°") },
                                    leadingIcon = { Icon(Icons.Outlined.RotateRight, null) },
                                )
                            }
                        }
                    }

                    ScanMode.ID_CARD -> {
                        Text(
                            "Dual-Side ID Card Mode places Front & Back centered on an A4 white sheet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                        ) {
                            EvoraOutlinedButton(
                                text = if (state.idFrontPath != null) "Retake Front" else "Front Pending",
                                onClick = { onIntent(ScannerFlowIntent.RetakeIdFront) },
                                modifier = Modifier.weight(1f),
                            )
                            EvoraOutlinedButton(
                                text = if (state.idBackPath != null) "Retake Back" else "Back Pending",
                                onClick = { onIntent(ScannerFlowIntent.RetakeIdBack) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (state.idFrontPath != null && state.idBackPath != null) {
                            EvoraPrimaryButton(
                                text = "Compose A4 PDF",
                                onClick = { onIntent(ScannerFlowIntent.ExportDualIdCard(asPdf = true)) },
                            )
                            EvoraOutlinedButton(
                                text = "Export A4 PNG",
                                onClick = { onIntent(ScannerFlowIntent.ExportDualIdCard(asPdf = false)) },
                            )
                        }
                    }

                    ScanMode.OCR -> {
                        Text("Select Recognition Script (100% Offline)", style = MaterialTheme.typography.titleSmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OcrScript.entries.forEach { script ->
                                FilterChip(
                                    selected = state.ocrScript == script,
                                    onClick = { onIntent(ScannerFlowIntent.SelectOcrScript(script)) },
                                    label = { Text(script.label) },
                                )
                            }
                        }
                        OutlinedTextField(
                            value = state.ocrExtractedText,
                            onValueChange = { onIntent(ScannerFlowIntent.UpdateOcrText(it)) },
                            label = { Text("Extracted Text (${state.ocrScript.label})") },
                            minLines = 4,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            EvoraOutlinedButton(
                                text = "Copy",
                                onClick = { onIntent(ScannerFlowIntent.CopyOcrTextClicked) },
                                modifier = Modifier.weight(1f),
                            )
                            EvoraOutlinedButton(
                                text = "Save .txt",
                                onClick = { onIntent(ScannerFlowIntent.ExportOcrFile(asDocx = false)) },
                                modifier = Modifier.weight(1f),
                            )
                            EvoraPrimaryButton(
                                text = "Export .docx",
                                onClick = { onIntent(ScannerFlowIntent.ExportOcrFile(asDocx = true)) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    ScanMode.QR_BARCODE -> {
                        if (state.decodedQrs.isEmpty()) {
                            Text(
                                "Point camera at any QR code / barcode or pick from Gallery.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            state.decodedQrs.forEach { action ->
                                QrActionCard(
                                    action = action,
                                    onCopy = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        cm?.setPrimaryClip(ClipData.newPlainText("QR Value", it))
                                    },
                                    onOpenUrl = { onIntent(ScannerFlowIntent.RequestOpenUrl(it)) },
                                    onSaveContact = { vcard ->
                                        val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                                            type = ContactsContract.RawContacts.CONTENT_TYPE
                                            putExtra(ContactsContract.Intents.Insert.NAME, vcard.fullName)
                                            vcard.phone?.let { putExtra(ContactsContract.Intents.Insert.PHONE, it) }
                                            vcard.email?.let { putExtra(ContactsContract.Intents.Insert.EMAIL, it) }
                                            vcard.org?.let { putExtra(ContactsContract.Intents.Insert.COMPANY, it) }
                                        }
                                        runCatching { context.startActivity(intent) }
                                    },
                                    onDial = { num ->
                                        runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num"))) }
                                    },
                                    onEmail = { email ->
                                        runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${email.address}"))) }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    state.pendingOpenUrlConfirm?.let { url ->
        AlertDialog(
            onDismissRequest = { onIntent(ScannerFlowIntent.DismissOpenUrl) },
            title = { Text("Open External URL?") },
            text = { Text("Verify destination before opening:\n$url") },
            confirmButton = {
                TextButton(onClick = { onIntent(ScannerFlowIntent.ConfirmOpenUrl) }) { Text("Open Browser") }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(ScannerFlowIntent.DismissOpenUrl) }) { Text("Cancel") }
            },
        )
    }

    state.exportResult?.let { res ->
        AlertDialog(
            onDismissRequest = { onIntent(ScannerFlowIntent.DismissExportResult) },
            title = { Text("Saved ${res.outputDisplayName}") },
            text = { Text(res.summaryMessage) },
            confirmButton = {
                TextButton(onClick = { onIntent(ScannerFlowIntent.OpenExportedResult) }) { Text("Open") }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(ScannerFlowIntent.DismissExportResult) }) { Text("Close") }
            },
        )
    }

    state.showRewardGateTitle?.let { gateTitle ->
        EvoraRewardGateSheet(
            featureTitle = gateTitle,
            onWatchRewardedPass = { onIntent(ScannerFlowIntent.GrantRewardedPassClicked) },
            onUpgradePro = { onIntent(ScannerFlowIntent.UpgradeProClicked) },
            onDismiss = { onIntent(ScannerFlowIntent.DismissRewardGate) },
        )
    }
}

@Composable
private fun QrActionCard(
    action: QrDecodedAction,
    onCopy: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onSaveContact: (QrDecodedAction.VCard) -> Unit,
    onDial: (String) -> Unit,
    onEmail: (QrDecodedAction.Email) -> Unit,
) {
    Surface(
        shape = EvoraRadii.md,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(EvoraSpacing.md),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when (action) {
                is QrDecodedAction.Url -> {
                    Text("Website URL", style = MaterialTheme.typography.titleSmall)
                    Text(action.url, style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { onOpenUrl(action.url) }) { Text("Open URL") }
                        TextButton(onClick = { onCopy(action.url) }) { Text("Copy") }
                    }
                }
                is QrDecodedAction.Wifi -> {
                    Text("Wi-Fi: ${action.ssid}", style = MaterialTheme.typography.titleSmall)
                    Text("Security: ${action.securityType} · Password: ${action.password}", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { onCopy(action.password) }) { Text("Copy Wi-Fi Password") }
                }
                is QrDecodedAction.VCard -> {
                    Text("Contact: ${action.fullName}", style = MaterialTheme.typography.titleSmall)
                    action.phone?.let { Text("Phone: $it", style = MaterialTheme.typography.bodyMedium) }
                    action.email?.let { Text("Email: $it", style = MaterialTheme.typography.bodyMedium) }
                    TextButton(onClick = { onSaveContact(action) }) { Text("Save Contact") }
                }
                is QrDecodedAction.Email -> {
                    Text("Email: ${action.address}", style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = { onEmail(action) }) { Text("Compose Email") }
                }
                is QrDecodedAction.Phone -> {
                    Text("Phone: ${action.number}", style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = { onDial(action.number) }) { Text("Dial Number") }
                }
                is QrDecodedAction.PlainText -> {
                    Text("Decoded Text", style = MaterialTheme.typography.titleSmall)
                    Text(action.rawValue, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { onCopy(action.rawValue) }) { Text("Copy Text") }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun analyzeQrFrame(
    proxy: ImageProxy,
    enabled: Boolean,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onDetected: (String) -> Unit,
) {
    if (!enabled) {
        proxy.close()
        return
    }
    val mediaImage = proxy.image
    if (mediaImage == null) {
        proxy.close()
        return
    }
    val input = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
    scanner.process(input)
        .addOnSuccessListener { list ->
            list.firstOrNull()?.rawValue?.let(onDetected)
        }
        .addOnCompleteListener {
            proxy.close()
        }
}

private fun copyUriToTempJpeg(context: Context, uri: Uri): String? = runCatching {
    val dir = File(context.cacheDir, "scanner_raw").apply { mkdirs() }
    val out = File(dir, "pick_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { src ->
        FileOutputStream(out).use { dst -> src.copyTo(dst) }
    } ?: return null
    out.absolutePath
}.getOrNull()

@Preview(showBackground = true)
@Composable
private fun ScannerFlowPreview() {
    EvoraTheme {
        ScannerFlowScreen(
            state = ScannerFlowState(),
            onRequestCameraPermission = {},
            onPickFromGallery = {},
            onLaunchMlKitDocScanner = {},
            onIntent = {},
        )
    }
}
