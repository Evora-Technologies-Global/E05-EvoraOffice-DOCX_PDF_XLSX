package com.evora.technologies.office.feature.tools.hub

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.office.core.designsystem.component.EvoraIconButton
import com.evora.technologies.office.core.designsystem.component.EvoraTemplatesSheet
import com.evora.technologies.office.core.designsystem.component.FormatBadge
import com.evora.technologies.office.core.designsystem.component.launchSystemPdfPrint
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraTheme
import com.evora.technologies.office.core.designsystem.theme.EvoraThemeTokens
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.PdfToolKind
import com.evora.technologies.office.core.mvi.CollectEffects

private data class ToolTileSpec(
    val id: UtilityToolId,
    val title: String,
    val icon: ImageVector,
    val tintBg: Color,
    val ink: Color,
    val badgeText: String? = null,
)

@Composable
fun ToolsHubRoute(
    columns: Int,
    onNavigateToPdfTool: (PdfToolKind) -> Unit,
    onNavigateToScannerMode: (String) -> Unit,
    onNavigateToPaywall: () -> Unit,
    onOpenCreatedOfficeDoc: (DocumentItem) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToWifiTransfer: () -> Unit = {},
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ToolsHubViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is ToolsHubEffect.NavigateToPdfTool -> onNavigateToPdfTool(effect.kind)
            is ToolsHubEffect.NavigateToScannerMode -> onNavigateToScannerMode(effect.modeName)
            ToolsHubEffect.NavigateToPaywall -> onNavigateToPaywall()
            ToolsHubEffect.NavigateToSettings -> onNavigateToSettings()
            ToolsHubEffect.NavigateToWifiTransfer -> onNavigateToWifiTransfer()
            is ToolsHubEffect.OpenCreatedOfficeDoc -> onOpenCreatedOfficeDoc(effect.document)
            is ToolsHubEffect.LaunchSystemPrint -> {
                if (effect.document.format == DocFormat.PDF) {
                    launchSystemPdfPrint(context, effect.document.uriString, effect.document.displayName, onShowMessage)
                } else {
                    onShowMessage("Open ${effect.document.displayName} in Reader/Editor to print formatted pages.")
                }
            }
            is ToolsHubEffect.ShowMessage -> onShowMessage(effect.message)
        }
    }

    ToolsHubScreen(
        state = state,
        columns = columns,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ToolsHubScreen(
    state: ToolsHubState,
    columns: Int = 3,
    onIntent: (ToolsHubIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = EvoraThemeTokens.colors
    val formats = EvoraThemeTokens.formats

    val convertTiles = listOf(
        ToolTileSpec(UtilityToolId.IMAGE_TO_PDF, "Image to PDF", Icons.Outlined.Image, formats.pdf.tintBg, formats.pdf.inkText),
        ToolTileSpec(UtilityToolId.TEXT_TO_PDF, "Text to PDF", Icons.Outlined.TextFields, formats.textArchive.tintBg, formats.textArchive.inkText),
        ToolTileSpec(UtilityToolId.DOC_TO_PDF, "Docs to PDF", Icons.Outlined.PictureAsPdf, formats.odf.tintBg, formats.odf.inkText),
        ToolTileSpec(
            UtilityToolId.PDF_TO_DOCX,
            "PDF to DOCX",
            Icons.Outlined.Description,
            formats.word.tintBg,
            formats.word.inkText,
        ),
        ToolTileSpec(UtilityToolId.PDF_TO_IMAGE, "PDF to Image", Icons.Outlined.Image, formats.epub.tintBg, formats.epub.inkText),
    )

    val pdfPowerTiles = listOf(
        ToolTileSpec(UtilityToolId.MERGE_PDF, "Merge PDF", Icons.Outlined.MergeType, formats.pdf.tintBg, formats.pdf.inkText),
        ToolTileSpec(UtilityToolId.SPLIT_PDF, "Split PDF", Icons.Outlined.CallSplit, formats.ppt.tintBg, formats.ppt.inkText),
        ToolTileSpec(UtilityToolId.COMPRESS_PDF, "Compress PDF", Icons.Outlined.Compress, formats.excel.tintBg, formats.excel.inkText),
        ToolTileSpec(
            UtilityToolId.WATERMARK_PDF,
            "Watermark",
            Icons.Outlined.AutoFixHigh,
            formats.hwp.tintBg,
            formats.hwp.inkText,
        ),
        ToolTileSpec(UtilityToolId.SET_PASSWORD, "Lock PDF", Icons.Outlined.Lock, formats.odf.tintBg, formats.odf.inkText),
        ToolTileSpec(UtilityToolId.REMOVE_PASSWORD, "Unlock PDF", Icons.Outlined.LockOpen, formats.textArchive.tintBg, formats.textArchive.inkText),
        ToolTileSpec(
            UtilityToolId.SIGN_PDF,
            "PKCS#7 Sign",
            Icons.Outlined.Draw,
            formats.word.tintBg,
            formats.word.inkText,
        ),
        ToolTileSpec(UtilityToolId.PRINT_DOCUMENT, "Print PDF", Icons.Outlined.Print, formats.excel.tintBg, formats.excel.inkText),
    )

    val utilityTiles = listOf(
        ToolTileSpec(UtilityToolId.OCR_EXTRACT, "OCR Extract", Icons.Outlined.DocumentScanner, formats.hwp.tintBg, formats.hwp.inkText),
        ToolTileSpec(UtilityToolId.QR_SCANNER, "QR / Barcode", Icons.Outlined.QrCodeScanner, formats.word.tintBg, formats.word.inkText),
        ToolTileSpec(UtilityToolId.TEMPLATES, "Templates", Icons.Outlined.LibraryBooks, formats.ppt.tintBg, formats.ppt.inkText),
        ToolTileSpec(UtilityToolId.WIFI_TRANSFER, "Wi-Fi Transfer", Icons.Outlined.Wifi, formats.epub.tintBg, formats.epub.inkText),
    )

    Column(modifier = modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(horizontal = EvoraSpacing.lg, vertical = EvoraSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("PDF & Office Tools", style = MaterialTheme.typography.titleLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EvoraIconButton(onClick = { onIntent(ToolsHubIntent.PaywallClicked) }) {
                        Icon(
                            imageVector = Icons.Outlined.WorkspacePremium,
                            contentDescription = "Evora Pro",
                            tint = extra.onProGoldContainer,
                        )
                    }
                    EvoraIconButton(onClick = { onIntent(ToolsHubIntent.SettingsClicked) }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("tools_hub_list"),
            contentPadding = PaddingValues(EvoraSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(EvoraSpacing.lg),
        ) {
            item(key = "sec_convert") {
                ToolSectionGrid(
                    title = "Convert",
                    tiles = convertTiles,
                    columns = columns.coerceAtLeast(3),
                    onTileClick = { onIntent(ToolsHubIntent.ToolClicked(it)) },
                )
            }
            item(key = "sec_pdf_power") {
                ToolSectionGrid(
                    title = "PDF Power",
                    tiles = pdfPowerTiles,
                    columns = columns.coerceAtLeast(3),
                    onTileClick = { onIntent(ToolsHubIntent.ToolClicked(it)) },
                )
            }
            item(key = "sec_utilities") {
                ToolSectionGrid(
                    title = "Utilities",
                    tiles = utilityTiles,
                    columns = columns.coerceAtLeast(3),
                    onTileClick = { onIntent(ToolsHubIntent.ToolClicked(it)) },
                )
            }
        }
    }

    if (state.showTemplatesSheet) {
        EvoraTemplatesSheet(
            templates = state.templates,
            selectedKind = state.selectedTemplateFilter,
            isCreating = state.isCreatingTemplate,
            onSelectKind = { onIntent(ToolsHubIntent.FilterTemplates(it)) },
            onTemplateClicked = { onIntent(ToolsHubIntent.CreateFromTemplate(it)) },
            onDismiss = { onIntent(ToolsHubIntent.DismissTemplatesSheet) },
        )
    }

    if (state.showPrintPickerSheet) {
        val pdfs = state.printableDocuments.filter { it.format == DocFormat.PDF }
        ModalBottomSheet(onDismissRequest = { onIntent(ToolsHubIntent.DismissPrintPicker) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EvoraSpacing.lg, vertical = EvoraSpacing.sm),
            ) {
                Text("Select PDF to Print", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(EvoraSpacing.sm))
                if (pdfs.isEmpty()) {
                    Text(
                        "No local PDF files indexed yet. Open a PDF from Home or Storage to print.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = EvoraSpacing.lg),
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(pdfs, key = { it.id }) { doc ->
                            ListItem(
                                headlineContent = { Text(doc.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingContent = { FormatBadge(format = doc.format, extension = doc.extension) },
                                modifier = Modifier.clickable { onIntent(ToolsHubIntent.PrintSelectedDocument(doc)) },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(EvoraSpacing.lg))
            }
        }
    }
}

@Composable
private fun ToolSectionGrid(
    title: String,
    tiles: List<ToolTileSpec>,
    columns: Int,
    onTileClick: (UtilityToolId) -> Unit,
) {
    val extra = EvoraThemeTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(EvoraSpacing.sm)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val rows = tiles.chunked(columns)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { rowTiles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowTiles.forEach { tile ->
                        Surface(
                            shape = EvoraRadii.md,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 96.dp)
                                .clip(EvoraRadii.md)
                                .clickable { onTileClick(tile.id) },
                        ) {
                            Box(modifier = Modifier.padding(EvoraSpacing.sm)) {
                                if (tile.badgeText != null) {
                                    Surface(
                                        shape = EvoraRadii.xs,
                                        color = extra.proGoldContainer,
                                        modifier = Modifier.align(Alignment.TopEnd),
                                    ) {
                                        Text(
                                            text = tile.badgeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = extra.onProGoldContainer,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.Center)
                                        .padding(top = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Surface(
                                        shape = EvoraRadii.md,
                                        color = tile.tintBg,
                                        modifier = Modifier.size(44.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = tile.icon,
                                                contentDescription = null,
                                                tint = tile.ink,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                    }
                                    Text(
                                        text = tile.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                    repeat(columns - rowTiles.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ToolsHubPreview() {
    EvoraTheme {
        ToolsHubScreen(
            state = ToolsHubState(),
            onIntent = {},
        )
    }
}
