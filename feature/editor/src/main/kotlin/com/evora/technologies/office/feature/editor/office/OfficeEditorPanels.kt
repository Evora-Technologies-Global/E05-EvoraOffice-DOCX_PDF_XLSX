package com.evora.technologies.office.feature.editor.office

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FormatAlignCenter
import androidx.compose.material.icons.outlined.FormatAlignLeft
import androidx.compose.material.icons.outlined.FormatAlignRight
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.evora.technologies.office.core.designsystem.component.EvoraIconButton
import com.evora.technologies.office.core.designsystem.component.EvoraOutlinedButton
import com.evora.technologies.office.core.designsystem.component.EvoraPrimaryButton
import com.evora.technologies.office.core.designsystem.theme.EvoraRadii
import com.evora.technologies.office.core.designsystem.theme.EvoraSpacing
import com.evora.technologies.office.core.designsystem.theme.EvoraTabularMonoStyle
import com.evora.technologies.office.core.designsystem.theme.EvoraThemeTokens
import com.evora.technologies.office.core.domain.util.ExcelFormulaEvaluator
import com.evora.technologies.office.core.model.CellHorizontalAlign
import com.evora.technologies.office.core.model.CellNumberFormat
import com.evora.technologies.office.core.model.SlideStrokeStyle
import com.evora.technologies.office.core.model.TextAlignOption
import com.evora.technologies.office.core.model.TrackChangeKind
import kotlin.math.roundToInt

private val QUICK_SWATCHES = listOf(0xFF0F172AL, 0xFF1563DBL, 0xFFC82828L, 0xFF1D820EL, 0xFFE6F1FFL, 0xFFFFFFFFL)
private val EXCEL_FORMULAS = listOf("SUM", "AVERAGE", "COUNT", "MIN", "MAX", "IF")

@Composable
internal fun WordTabStrip(selected: WordEditorTab, onSelect: (WordEditorTab) -> Unit) {
    ScrollableTabRow(selectedTabIndex = selected.ordinal, edgePadding = 4.dp) {
        WordEditorTab.entries.forEach { tab ->
            Tab(selected = selected == tab, onClick = { onSelect(tab) }, text = { Text(tab.label, maxLines = 1) })
        }
    }
}

@Composable
internal fun ExcelTabStrip(selected: ExcelEditorTab, onSelect: (ExcelEditorTab) -> Unit) {
    ScrollableTabRow(selectedTabIndex = selected.ordinal, edgePadding = 4.dp) {
        ExcelEditorTab.entries.forEach { tab ->
            Tab(selected = selected == tab, onClick = { onSelect(tab) }, text = { Text(tab.label, maxLines = 1) })
        }
    }
}

@Composable
internal fun PptTabStrip(selected: PptEditorTab, onSelect: (PptEditorTab) -> Unit) {
    ScrollableTabRow(selectedTabIndex = selected.ordinal, edgePadding = 4.dp) {
        PptEditorTab.entries.forEach { tab ->
            Tab(selected = selected == tab, onClick = { onSelect(tab) }, text = { Text(tab.label, maxLines = 1) })
        }
    }
}

@Composable
internal fun WordEditorCanvas(
    state: OfficeEditorState,
    onIntent: (OfficeEditorIntent) -> Unit,
) {
    val doc = state.wordSnapshot ?: return
    val extra = EvoraThemeTokens.colors

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE2E8F0)),
        contentPadding = PaddingValues(EvoraSpacing.md),
        verticalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
    ) {
        item(key = "page_sheet") {
            Surface(
                shape = EvoraRadii.md,
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(doc.pageMarginDp.dp),
                    verticalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
                ) {
                    if (doc.trackChangesEnabled) {
                        Surface(shape = EvoraRadii.xs, color = extra.warningContainer, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Track Changes Recording ON (${doc.trackChanges.size} revision/comment items)",
                                style = MaterialTheme.typography.labelMedium,
                                color = extra.onWarningContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }

                    doc.paragraphs.forEachIndexed { idx, para ->
                        val isSelected = idx == state.selectedParagraphIndex
                        val textAlign = when (para.alignment) {
                            TextAlignOption.LEFT -> TextAlign.Start
                            TextAlignOption.CENTER -> TextAlign.Center
                            TextAlignOption.RIGHT -> TextAlign.End
                            TextAlignOption.JUSTIFY -> TextAlign.Justify
                        }
                        val decoration = when {
                            para.underline && para.strikethrough -> TextDecoration.combine(
                                listOf(TextDecoration.Underline, TextDecoration.LineThrough),
                            )
                            para.underline -> TextDecoration.Underline
                            para.strikethrough -> TextDecoration.LineThrough
                            else -> TextDecoration.None
                        }
                        Surface(
                            shape = EvoraRadii.xs,
                            color = para.highlightColorArgb?.let { Color(it).copy(alpha = 0.35f) } ?: Color.Transparent,
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = (para.indentLevel * 12).dp)
                                .clickable { onIntent(OfficeEditorIntent.SelectParagraph(idx)) },
                        ) {
                            if (isSelected) {
                                OutlinedTextField(
                                    value = para.text,
                                    onValueChange = { onIntent(OfficeEditorIntent.UpdateParagraphText(idx, it)) },
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = para.fontSizeSp.sp,
                                        fontWeight = if (para.bold) FontWeight.Bold else FontWeight.Normal,
                                        fontStyle = if (para.italic) FontStyle.Italic else FontStyle.Normal,
                                        textDecoration = decoration,
                                        color = Color(para.textColorArgb),
                                        textAlign = textAlign,
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                val prefix = when {
                                    para.isBullet -> "• "
                                    para.isNumbered -> "${idx + 1}. "
                                    else -> ""
                                }
                                Text(
                                    text = prefix + para.text.ifBlank { "(Tap to edit paragraph)" },
                                    fontSize = para.fontSizeSp.sp,
                                    fontWeight = if (para.bold) FontWeight.Bold else FontWeight.Normal,
                                    fontStyle = if (para.italic) FontStyle.Italic else FontStyle.Normal,
                                    textDecoration = decoration,
                                    color = Color(para.textColorArgb),
                                    textAlign = textAlign,
                                    modifier = Modifier.padding(8.dp),
                                )
                            }
                        }
                    }

                    if (doc.inkNoteSummary.isNotBlank()) {
                        Surface(shape = EvoraRadii.xs, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Text(
                                text = "Ink Draw Overlay: ${doc.inkNoteSummary}",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WordToolbarShelf(
    state: OfficeEditorState,
    onIntent: (OfficeEditorIntent) -> Unit,
) {
    val doc = state.wordSnapshot ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = EvoraSpacing.md, vertical = EvoraSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (state.wordTab) {
            WordEditorTab.EDIT -> {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EvoraIconButton(onClick = { onIntent(OfficeEditorIntent.ToggleBold) }) {
                        Icon(Icons.Outlined.FormatBold, "Bold")
                    }
                    EvoraIconButton(onClick = { onIntent(OfficeEditorIntent.ToggleItalic) }) {
                        Icon(Icons.Outlined.FormatItalic, "Italic")
                    }
                    EvoraIconButton(onClick = { onIntent(OfficeEditorIntent.ToggleUnderline) }) {
                        Icon(Icons.Outlined.FormatUnderlined, "Underline")
                    }
                    EvoraIconButton(onClick = { onIntent(OfficeEditorIntent.ToggleStrikethrough) }) {
                        Icon(Icons.Outlined.FormatStrikethrough, "Strikethrough")
                    }
                    TextButton(onClick = { onIntent(OfficeEditorIntent.StepFontSize(-2)) }) { Text("A-") }
                    TextButton(onClick = { onIntent(OfficeEditorIntent.StepFontSize(+2)) }) { Text("A+") }
                    EvoraIconButton(onClick = { onIntent(OfficeEditorIntent.SetParagraphAlign(TextAlignOption.LEFT)) }) {
                        Icon(Icons.Outlined.FormatAlignLeft, "Align Left")
                    }
                    EvoraIconButton(onClick = { onIntent(OfficeEditorIntent.SetParagraphAlign(TextAlignOption.CENTER)) }) {
                        Icon(Icons.Outlined.FormatAlignCenter, "Align Center")
                    }
                    EvoraIconButton(onClick = { onIntent(OfficeEditorIntent.SetParagraphAlign(TextAlignOption.RIGHT)) }) {
                        Icon(Icons.Outlined.FormatAlignRight, "Align Right")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Color:", style = MaterialTheme.typography.labelMedium)
                    QUICK_SWATCHES.take(4).forEach { argb ->
                        Surface(
                            shape = CircleShape,
                            color = Color(argb),
                            modifier = Modifier.size(28.dp).clip(CircleShape).clickable {
                                onIntent(OfficeEditorIntent.SetTextColor(argb))
                            },
                        ) {}
                    }
                    TextButton(onClick = { onIntent(OfficeEditorIntent.SetHighlightColor(0xFFFFD54FL)) }) { Text("Highlight") }
                    TextButton(onClick = { onIntent(OfficeEditorIntent.SetHighlightColor(null)) }) { Text("Clear Hi") }
                }
            }

            WordEditorTab.FILE -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EvoraPrimaryButton(text = "Save", onClick = { onIntent(OfficeEditorIntent.SaveClicked) }, modifier = Modifier.weight(1f))
                EvoraOutlinedButton(text = "Export PDF", onClick = { onIntent(OfficeEditorIntent.ExportPdfClicked) }, modifier = Modifier.weight(1f))
                EvoraOutlinedButton(text = "Print", onClick = { onIntent(OfficeEditorIntent.PrintClicked) }, modifier = Modifier.weight(1f))
                EvoraOutlinedButton(text = "Share", onClick = { onIntent(OfficeEditorIntent.ShareClicked) }, modifier = Modifier.weight(1f))
            }

            WordEditorTab.INSERT -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.AddWordParagraph) }, label = { Text("+ Paragraph") })
                FilterChip(
                    selected = false,
                    onClick = { onIntent(OfficeEditorIntent.ToggleBulletList) },
                    label = { Text("Bullets") },
                    leadingIcon = { Icon(Icons.Outlined.FormatListBulleted, null) },
                )
                FilterChip(
                    selected = false,
                    onClick = { onIntent(OfficeEditorIntent.ToggleNumberedList) },
                    label = { Text("Numbered") },
                    leadingIcon = { Icon(Icons.Outlined.FormatListNumbered, null) },
                )
                FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.StepIndent(+1)) }, label = { Text("Indent +") })
                FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.StepIndent(-1)) }, label = { Text("Indent -") })
                FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.AppendWordBlock("[Image Attachment]")) }, label = { Text("+ Image Note") })
            }

            WordEditorTab.REVIEW -> {
                var commentDraft by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Track Changes (<w:ins>/<w:del>)", style = MaterialTheme.typography.titleSmall)
                    Switch(
                        checked = doc.trackChangesEnabled,
                        onCheckedChange = { onIntent(OfficeEditorIntent.ToggleTrackChanges(it)) },
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = commentDraft,
                        onValueChange = { commentDraft = it },
                        placeholder = { Text("Add review comment...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            onIntent(OfficeEditorIntent.AddReviewComment(commentDraft))
                            commentDraft = ""
                        },
                    ) { Text("Comment") }
                }
                if (doc.trackChanges.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(doc.trackChanges, key = { it.id }) { tc ->
                            Surface(
                                shape = EvoraRadii.sm,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    val prefix = when (tc.kind) {
                                        TrackChangeKind.INSERTION -> "+Ins:"
                                        TrackChangeKind.DELETION -> "-Del:"
                                        TrackChangeKind.COMMENT -> "Note:"
                                    }
                                    Text("$prefix ${tc.text.take(24)}", style = MaterialTheme.typography.labelMedium)
                                    EvoraIconButton(onClick = { onIntent(OfficeEditorIntent.AcceptTrackChange(tc.id)) }) {
                                        Icon(Icons.Outlined.Check, "Accept", modifier = Modifier.size(18.dp))
                                    }
                                    EvoraIconButton(onClick = { onIntent(OfficeEditorIntent.RejectTrackChange(tc.id)) }) {
                                        Icon(Icons.Outlined.Close, "Reject", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            WordEditorTab.PAGE -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = doc.pageMarginDp == 12, onClick = { onIntent(OfficeEditorIntent.SetWordMargins(12)) }, label = { Text("Narrow Margin") })
                FilterChip(selected = doc.pageMarginDp == 24, onClick = { onIntent(OfficeEditorIntent.SetWordMargins(24)) }, label = { Text("Normal Margin") })
                FilterChip(selected = doc.pageMarginDp == 36, onClick = { onIntent(OfficeEditorIntent.SetWordMargins(36)) }, label = { Text("Wide Margin") })
                FilterChip(selected = !doc.isLandscape, onClick = { onIntent(OfficeEditorIntent.ToggleWordOrientation(false)) }, label = { Text("Portrait") })
                FilterChip(selected = doc.isLandscape, onClick = { onIntent(OfficeEditorIntent.ToggleWordOrientation(true)) }, label = { Text("Landscape") })
            }

            WordEditorTab.DRAW -> {
                var inkText by remember(doc.inkNoteSummary) { mutableStateOf(doc.inkNoteSummary) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = inkText,
                        onValueChange = {
                            inkText = it
                            onIntent(OfficeEditorIntent.UpdateInkNoteSummary(it))
                        },
                        label = { Text("Freehand / Ink Annotation Note") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            WordEditorTab.FIND -> FindReplaceBar(state = state, onIntent = onIntent)
        }
    }
}

@Composable
internal fun ExcelEditorCanvas(
    state: OfficeEditorState,
    onIntent: (OfficeEditorIntent) -> Unit,
) {
    val wb = state.workbookSnapshot ?: return
    val sheet = wb.sheets.getOrNull(wb.activeSheetIndex) ?: return
    val cellAddr = "${ExcelFormulaEvaluator.colIndexToName(state.selectedCol)}${state.selectedRow + 1}"

    Column(modifier = Modifier.fillMaxSize()) {
        // fx Formula Bar
        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = EvoraSpacing.sm, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(shape = EvoraRadii.xs, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        text = "$cellAddr · fx",
                        style = EvoraTabularMonoStyle,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
                OutlinedTextField(
                    value = state.formulaBarText,
                    onValueChange = { onIntent(OfficeEditorIntent.UpdateFormulaBar(it)) },
                    placeholder = { Text("Value or =SUM(A1:A5)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                EvoraIconButton(onClick = { onIntent(OfficeEditorIntent.CommitCellFormula) }) {
                    Icon(Icons.Outlined.Check, "Apply formula")
                }
            }
        }

        // Sheet tabs
        LazyRow(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow),
            contentPadding = PaddingValues(horizontal = EvoraSpacing.sm, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(wb.sheets, key = { idx, s -> "${idx}_${s.name}" }) { idx, s ->
                FilterChip(
                    selected = idx == wb.activeSheetIndex,
                    onClick = { onIntent(OfficeEditorIntent.SelectExcelSheet(idx)) },
                    label = { Text(s.name) },
                )
            }
            item(key = "add_sheet") {
                FilterChip(
                    selected = false,
                    onClick = { onIntent(OfficeEditorIntent.AddExcelSheet("Sheet${wb.sheets.size + 1}")) },
                    label = { Text("+ Sheet") },
                )
            }
        }

        // Scrollable Grid
        val hScroll = rememberScrollState()
        val vScroll = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(vScroll)
                .horizontalScroll(hScroll),
        ) {
            // Header row
            Row {
                Box(
                    modifier = Modifier.width(42.dp).height(32.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                )
                for (c in 0 until sheet.colCount) {
                    val colW = (sheet.colWidthsDp[c] ?: 92).dp
                    Box(
                        modifier = Modifier.width(colW).height(32.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(ExcelFormulaEvaluator.colIndexToName(c), style = EvoraTabularMonoStyle)
                    }
                }
            }

            for (r in 0 until sheet.rowCount) {
                val rowH = (sheet.rowHeightsDp[r] ?: 40).dp
                val isFrozenRow = r < sheet.frozenRows
                Row {
                    Box(
                        modifier = Modifier.width(42.dp).height(rowH)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${r + 1}", style = EvoraTabularMonoStyle)
                    }
                    for (c in 0 until sheet.colCount) {
                        val colW = (sheet.colWidthsDp[c] ?: 92).dp
                        val cell = sheet.cells["$r,$c"]
                        val isSelected = r == state.selectedRow && c == state.selectedCol
                        val bg = when {
                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                            isFrozenRow || c < sheet.frozenCols -> MaterialTheme.colorScheme.surfaceContainerLow
                            else -> Color.White
                        }
                        val align = when (cell?.hAlign) {
                            CellHorizontalAlign.CENTER -> Alignment.Center
                            CellHorizontalAlign.RIGHT -> Alignment.CenterEnd
                            else -> Alignment.CenterStart
                        }
                        Box(
                            modifier = Modifier
                                .width(if (cell != null && cell.mergedSpanCols > 1) colW * cell.mergedSpanCols else colW)
                                .height(rowH)
                                .background(bg)
                                .border(
                                    width = if (isSelected) 2.dp else 0.5.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                )
                                .clickable { onIntent(OfficeEditorIntent.SelectCell(r, c)) }
                                .padding(horizontal = 6.dp),
                            contentAlignment = align,
                        ) {
                            Text(
                                text = cell?.evaluatedDisplay.orEmpty(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (cell?.bold == true) FontWeight.Bold else FontWeight.Normal,
                                ),
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ExcelToolbarShelf(
    state: OfficeEditorState,
    onIntent: (OfficeEditorIntent) -> Unit,
) {
    val wb = state.workbookSnapshot ?: return
    val sheet = wb.sheets.getOrNull(wb.activeSheetIndex) ?: return
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = EvoraSpacing.md, vertical = EvoraSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (state.excelTab) {
            ExcelEditorTab.FILE -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EvoraPrimaryButton(text = "Save", onClick = { onIntent(OfficeEditorIntent.SaveClicked) }, modifier = Modifier.weight(1f))
                EvoraOutlinedButton(text = "Export PDF", onClick = { onIntent(OfficeEditorIntent.ExportPdfClicked) }, modifier = Modifier.weight(1f))
                EvoraOutlinedButton(text = "Print", onClick = { onIntent(OfficeEditorIntent.PrintClicked) }, modifier = Modifier.weight(1f))
            }

            ExcelEditorTab.EDIT -> {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.ToggleCellBold) }, label = { Text("Bold") })
                    FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.SetCellHorizontalAlign(CellHorizontalAlign.LEFT)) }, label = { Text("Left") })
                    FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.SetCellHorizontalAlign(CellHorizontalAlign.CENTER)) }, label = { Text("Center") })
                    FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.SetCellHorizontalAlign(CellHorizontalAlign.RIGHT)) }, label = { Text("Right") })
                    FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.ToggleMergeRight) }, label = { Text("Merge") })
                    FilterChip(selected = sheet.frozenRows > 0, onClick = { onIntent(OfficeEditorIntent.ToggleFreezeFirstRow) }, label = { Text("Freeze Row 1") })
                    FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.AdjustSelectedColWidth(+20)) }, label = { Text("Col W+") })
                    FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.AdjustSelectedRowHeight(+10)) }, label = { Text("Row H+") })
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(CellNumberFormat.entries, key = { it.name }) { fmt ->
                        FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.SetCellNumberFormat(fmt)) }, label = { Text(fmt.label) })
                    }
                }
            }

            ExcelEditorTab.INSERT -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.InsertRowAfterSelected) }, label = { Text("+ Row") })
                FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.DeleteSelectedRow) }, label = { Text("- Row") })
                FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.InsertColAfterSelected) }, label = { Text("+ Col") })
                FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.DeleteSelectedCol) }, label = { Text("- Col") })
                FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.DeleteActiveSheet) }, label = { Text("Delete Sheet") })
            }

            ExcelEditorTab.FORMULAS -> LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(EXCEL_FORMULAS, key = { it }) { fn ->
                    FilterChip(
                        selected = false,
                        onClick = { onIntent(OfficeEditorIntent.InsertFormulaFunction(fn)) },
                        label = { Text("=$fn(...)") },
                    )
                }
            }

            ExcelEditorTab.FIND -> FindReplaceBar(state = state, onIntent = onIntent)
        }
    }
}

@Composable
internal fun PptEditorCanvas(
    state: OfficeEditorState,
    onIntent: (OfficeEditorIntent) -> Unit,
) {
    val deck = state.slideDeckSnapshot ?: return
    val slide = deck.slides.getOrNull(state.activeSlideIndex) ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE2E8F0))
            .padding(EvoraSpacing.md),
        verticalArrangement = Arrangement.spacedBy(EvoraSpacing.sm),
    ) {
        // 16:9 Slide Canvas
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(EvoraRadii.md)
                .background(Color(slide.backgroundColorArgb)),
        ) {
            val wPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val hPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
            slide.elements.sortedBy { it.zOrder }.forEach { el ->
                val isSelected = state.selectedSlideElementId == el.id
                val shape = if (el.kind == "SHAPE_OVAL") CircleShape else RoundedCornerShape(8.dp)
                val borderWidth = if (isSelected) 2.5.dp else if (el.strokeStyle == SlideStrokeStyle.NONE) 0.dp else el.strokeWidthDp.dp
                val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(el.strokeColorArgb)
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset((el.xFraction * wPx).roundToInt(), (el.yFraction * hPx).roundToInt())
                        }
                        .size(
                            width = (maxWidth * el.widthFraction),
                            height = (maxHeight * el.heightFraction),
                        )
                        .clip(shape)
                        .background(Color(el.fillColorArgb))
                        .border(borderWidth, borderColor, shape)
                        .clickable { onIntent(OfficeEditorIntent.SelectSlideElement(el.id)) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = el.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // Slide Thumbnail Strip
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(deck.slides, key = { idx, s -> "${idx}_${s.id}" }) { idx, s ->
                val selected = idx == state.activeSlideIndex
                Surface(
                    shape = EvoraRadii.sm,
                    color = Color(s.backgroundColorArgb),
                    border = BorderStroke(if (selected) 2.5.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .width(92.dp)
                        .height(54.dp)
                        .clickable { onIntent(OfficeEditorIntent.SelectSlide(idx)) },
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.Center) {
                        Text("Slide ${idx + 1}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF0F172A))
                    }
                }
            }
            item(key = "add_slide") {
                FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.AddSlide) }, label = { Text("+ Slide") })
            }
        }

        // Selected Element Quick Text Editor or Notes Preview
        val selectedEl = slide.elements.firstOrNull { it.id == state.selectedSlideElementId }
        if (selectedEl != null) {
            OutlinedTextField(
                value = selectedEl.text,
                onValueChange = { onIntent(OfficeEditorIntent.UpdateSelectedElementText(it)) },
                label = { Text("Selected Element Text") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        } else if (slide.presenterNotes.isNotBlank()) {
            Surface(shape = EvoraRadii.sm, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Notes: ${slide.presenterNotes}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(EvoraSpacing.sm),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PptToolbarShelf(
    state: OfficeEditorState,
    onIntent: (OfficeEditorIntent) -> Unit,
) {
    val deck = state.slideDeckSnapshot ?: return
    val slide = deck.slides.getOrNull(state.activeSlideIndex) ?: return
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = EvoraSpacing.md, vertical = EvoraSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (state.pptTab) {
            PptEditorTab.EDIT -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EvoraOutlinedButton(
                    text = "Delete Selected",
                    onClick = { onIntent(OfficeEditorIntent.DeleteSelectedElement) },
                    modifier = Modifier.weight(1f),
                )
                EvoraPrimaryButton(
                    text = "Play SlideShow",
                    onClick = { onIntent(OfficeEditorIntent.LaunchSlideShowClicked) },
                    modifier = Modifier.weight(1f),
                )
            }

            PptEditorTab.FILE -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EvoraPrimaryButton(text = "Save", onClick = { onIntent(OfficeEditorIntent.SaveClicked) }, modifier = Modifier.weight(1f))
                EvoraOutlinedButton(text = "Export PDF", onClick = { onIntent(OfficeEditorIntent.ExportPdfClicked) }, modifier = Modifier.weight(1f))
                EvoraOutlinedButton(text = "Print", onClick = { onIntent(OfficeEditorIntent.PrintClicked) }, modifier = Modifier.weight(1f))
            }

            PptEditorTab.INSERT -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.AddSlideElement("TEXT")) }, label = { Text("+ Textbox") })
                FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.AddSlideElement("SHAPE_RECT")) }, label = { Text("+ Rectangle") })
                FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.AddSlideElement("SHAPE_OVAL")) }, label = { Text("+ Oval") })
                FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.AddSlide) }, label = { Text("+ New Slide") })
            }

            PptEditorTab.FORMAT -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Fill:", style = MaterialTheme.typography.labelMedium)
                    QUICK_SWATCHES.forEach { argb ->
                        Surface(
                            shape = CircleShape,
                            color = Color(argb),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.size(26.dp).clip(CircleShape).clickable {
                                onIntent(OfficeEditorIntent.SetElementFillColor(argb))
                            },
                        ) {}
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.ReorderSelectedElement(ZOrderDirection.BRING_FRONT)) }, label = { Text("Front") })
                    FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.ReorderSelectedElement(ZOrderDirection.FORWARD)) }, label = { Text("Forward") })
                    FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.ReorderSelectedElement(ZOrderDirection.BACKWARD)) }, label = { Text("Backward") })
                    FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.ReorderSelectedElement(ZOrderDirection.SEND_BACK)) }, label = { Text("Back") })
                }
            }

            PptEditorTab.ANNOTATE -> OutlinedTextField(
                value = slide.presenterNotes,
                onValueChange = { onIntent(OfficeEditorIntent.UpdatePresenterNotes(it)) },
                label = { Text("Presenter Notes") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            PptEditorTab.PAGE -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.DuplicateActiveSlide) }, label = { Text("Duplicate Slide") })
                FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.DeleteActiveSlide) }, label = { Text("Delete Slide") })
                FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.SetSlideBackgroundColor(0xFFFFFFFFL)) }, label = { Text("White BG") })
                FilterChip(selected = false, onClick = { onIntent(OfficeEditorIntent.SetSlideBackgroundColor(0xFFF2F4F8L)) }, label = { Text("Slate BG") })
            }

            PptEditorTab.FIND -> FindReplaceBar(state = state, onIntent = onIntent)
        }
    }
}

@Composable
private fun FindReplaceBar(
    state: OfficeEditorState,
    onIntent: (OfficeEditorIntent) -> Unit,
) {
    var replaceText by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.findQuery,
                onValueChange = { onIntent(OfficeEditorIntent.UpdateFindQuery(it)) },
                placeholder = { Text("Find text...") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Text("${state.findMatchCount} hits", style = EvoraTabularMonoStyle)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = replaceText,
                onValueChange = { replaceText = it },
                placeholder = { Text("Replace with...") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onIntent(OfficeEditorIntent.ReplaceInActiveDoc(replaceText)) }) {
                Text("Replace All")
            }
        }
    }
}
