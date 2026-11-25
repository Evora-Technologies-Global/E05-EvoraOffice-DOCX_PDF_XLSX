package com.evora.technologies.office.feature.editor.office

import androidx.lifecycle.viewModelScope
import com.evora.technologies.office.core.domain.port.DraftRecoveryRepository
import com.evora.technologies.office.core.domain.port.OfficeEditorPort
import com.evora.technologies.office.core.domain.port.PdfPowerPort
import com.evora.technologies.office.core.domain.port.PreferencesRepository
import com.evora.technologies.office.core.domain.util.ExcelFormulaEvaluator
import com.evora.technologies.office.core.model.ExcelCell
import com.evora.technologies.office.core.model.OfficeKind
import com.evora.technologies.office.core.model.SlideDeckSnapshot
import com.evora.technologies.office.core.model.SlideElement
import com.evora.technologies.office.core.model.SlidePageData
import com.evora.technologies.office.core.model.TrackChangeItem
import com.evora.technologies.office.core.model.TrackChangeKind
import com.evora.technologies.office.core.model.WordDocumentSnapshot
import com.evora.technologies.office.core.model.WordParagraph
import com.evora.technologies.office.core.model.WorkbookSnapshot
import com.evora.technologies.office.core.model.WorksheetData
import com.evora.technologies.office.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import java.util.UUID
import javax.inject.Inject

private data class EditorMemento(
    val word: WordDocumentSnapshot? = null,
    val workbook: WorkbookSnapshot? = null,
    val slideDeck: SlideDeckSnapshot? = null,
)

@HiltViewModel
class OfficeEditorViewModel @Inject constructor(
    private val officeEditorPort: OfficeEditorPort,
    private val draftRecoveryRepository: DraftRecoveryRepository,
    private val pdfPowerPort: PdfPowerPort,
    private val preferencesRepository: PreferencesRepository,
) : MviViewModel<OfficeEditorState, OfficeEditorIntent, OfficeEditorEffect>(
    OfficeEditorState(),
) {
    private var loadJob: Job? = null
    private var autosaveTickerJob: Job? = null
    private val undoStack = ArrayDeque<EditorMemento>()
    private val redoStack = ArrayDeque<EditorMemento>()

    override fun onIntent(intent: OfficeEditorIntent) {
        when (intent) {
            is OfficeEditorIntent.OpenDocument -> openDocument(intent.uriString, intent.displayName, intent.kind)
            OfficeEditorIntent.RestoreDraftClicked -> restorePendingDraft()
            OfficeEditorIntent.DiscardDraftClicked -> discardPendingDraft()
            OfficeEditorIntent.LifecycleStopped -> writeCheckpointIfDirty()
            OfficeEditorIntent.ToggleBottomShelf -> setState { copy(isBottomShelfExpanded = !isBottomShelfExpanded) }
            OfficeEditorIntent.UndoClicked -> performUndo()
            OfficeEditorIntent.RedoClicked -> performRedo()
            OfficeEditorIntent.SaveClicked -> saveDocument()
            OfficeEditorIntent.ExportPdfClicked -> exportToPdf()
            OfficeEditorIntent.PrintClicked -> printAsPdf()
            OfficeEditorIntent.ShareClicked -> {
                if (currentState.uriString.isNotBlank()) {
                    sendEffect(OfficeEditorEffect.ShareFile(currentState.uriString, currentState.displayName))
                }
            }
            OfficeEditorIntent.BackClicked -> {
                writeCheckpointIfDirty()
                autosaveTickerJob?.cancel()
                sendEffect(OfficeEditorEffect.NavigateBack)
            }
            // Word
            is OfficeEditorIntent.SelectWordTab -> setState { copy(wordTab = intent.tab, isBottomShelfExpanded = true) }
            is OfficeEditorIntent.SelectParagraph -> setState { copy(selectedParagraphIndex = intent.index) }
            is OfficeEditorIntent.UpdateParagraphText -> updateParagraphText(intent.index, intent.text)
            OfficeEditorIntent.ToggleBold -> mutateSelectedParagraph { it.copy(bold = !it.bold) }
            OfficeEditorIntent.ToggleItalic -> mutateSelectedParagraph { it.copy(italic = !it.italic) }
            OfficeEditorIntent.ToggleUnderline -> mutateSelectedParagraph { it.copy(underline = !it.underline) }
            OfficeEditorIntent.ToggleStrikethrough -> mutateSelectedParagraph { it.copy(strikethrough = !it.strikethrough) }
            is OfficeEditorIntent.StepFontSize -> mutateSelectedParagraph {
                it.copy(fontSizeSp = (it.fontSizeSp + intent.deltaSp).coerceIn(10, 36))
            }
            is OfficeEditorIntent.SetTextColor -> mutateSelectedParagraph { it.copy(textColorArgb = intent.argb) }
            is OfficeEditorIntent.SetHighlightColor -> mutateSelectedParagraph { it.copy(highlightColorArgb = intent.argb) }
            is OfficeEditorIntent.SetParagraphAlign -> mutateSelectedParagraph { it.copy(alignment = intent.align) }
            OfficeEditorIntent.ToggleBulletList -> mutateSelectedParagraph { it.copy(isBullet = !it.isBullet, isNumbered = false) }
            OfficeEditorIntent.ToggleNumberedList -> mutateSelectedParagraph { it.copy(isNumbered = !it.isNumbered, isBullet = false) }
            is OfficeEditorIntent.StepIndent -> mutateSelectedParagraph {
                it.copy(indentLevel = (it.indentLevel + intent.delta).coerceIn(0, 4))
            }
            OfficeEditorIntent.AddWordParagraph -> addWordParagraph("")
            is OfficeEditorIntent.AppendWordBlock -> addWordParagraph(intent.prefix)
            is OfficeEditorIntent.ToggleTrackChanges -> mutateWord { it.copy(trackChangesEnabled = intent.enabled) }
            is OfficeEditorIntent.AddReviewComment -> addReviewComment(intent.commentText)
            is OfficeEditorIntent.AcceptTrackChange -> resolveTrackChange(intent.changeId, accept = true)
            is OfficeEditorIntent.RejectTrackChange -> resolveTrackChange(intent.changeId, accept = false)
            is OfficeEditorIntent.SetWordMargins -> mutateWord { it.copy(pageMarginDp = intent.marginDp) }
            is OfficeEditorIntent.ToggleWordOrientation -> mutateWord { it.copy(isLandscape = intent.landscape) }
            is OfficeEditorIntent.UpdateInkNoteSummary -> mutateWord { it.copy(inkNoteSummary = intent.summary) }
            // Excel
            is OfficeEditorIntent.SelectExcelTab -> setState { copy(excelTab = intent.tab, isBottomShelfExpanded = true) }
            is OfficeEditorIntent.SelectExcelSheet -> selectSheet(intent.sheetIndex)
            is OfficeEditorIntent.AddExcelSheet -> addSheet(intent.name)
            is OfficeEditorIntent.RenameActiveSheet -> renameActiveSheet(intent.newName)
            OfficeEditorIntent.DeleteActiveSheet -> deleteActiveSheet()
            is OfficeEditorIntent.SelectCell -> selectCell(intent.row, intent.col)
            is OfficeEditorIntent.UpdateFormulaBar -> setState { copy(formulaBarText = intent.text) }
            OfficeEditorIntent.CommitCellFormula -> commitFormulaBar()
            is OfficeEditorIntent.InsertFormulaFunction -> {
                val r = currentState.selectedRow + 1
                val col = ExcelFormulaEvaluator.colIndexToName(currentState.selectedCol)
                val template = when (intent.functionName) {
                    "IF" -> "=IF(${col}1>0,\"Yes\",\"No\")"
                    else -> "=${intent.functionName}(${col}1:${col}${(r - 1).coerceAtLeast(1)})"
                }
                setState { copy(formulaBarText = template) }
            }
            OfficeEditorIntent.ToggleCellBold -> mutateSelectedCell { it.copy(bold = !it.bold) }
            is OfficeEditorIntent.SetCellHorizontalAlign -> mutateSelectedCell { it.copy(hAlign = intent.align) }
            is OfficeEditorIntent.SetCellNumberFormat -> mutateSelectedCell { it.copy(numberFormat = intent.format) }
            OfficeEditorIntent.ToggleMergeRight -> mutateSelectedCell {
                it.copy(mergedSpanCols = if (it.mergedSpanCols > 1) 1 else 2)
            }
            OfficeEditorIntent.ToggleFreezeFirstRow -> mutateActiveSheet {
                it.copy(frozenRows = if (it.frozenRows > 0) 0 else 1)
            }
            OfficeEditorIntent.ToggleFreezeFirstCol -> mutateActiveSheet {
                it.copy(frozenCols = if (it.frozenCols > 0) 0 else 1)
            }
            is OfficeEditorIntent.AdjustSelectedColWidth -> mutateActiveSheet { s ->
                val cur = s.colWidthsDp[currentState.selectedCol] ?: 92
                s.copy(colWidthsDp = s.colWidthsDp + (currentState.selectedCol to (cur + intent.deltaDp).coerceIn(56, 220)))
            }
            is OfficeEditorIntent.AdjustSelectedRowHeight -> mutateActiveSheet { s ->
                val cur = s.rowHeightsDp[currentState.selectedRow] ?: 40
                s.copy(rowHeightsDp = s.rowHeightsDp + (currentState.selectedRow to (cur + intent.deltaDp).coerceIn(32, 96)))
            }
            OfficeEditorIntent.InsertRowAfterSelected -> mutateActiveSheet {
                it.copy(rowCount = (it.rowCount + 1).coerceAtMost(80))
            }
            OfficeEditorIntent.DeleteSelectedRow -> mutateActiveSheet {
                if (it.rowCount <= 5) it else it.copy(rowCount = it.rowCount - 1)
            }
            OfficeEditorIntent.InsertColAfterSelected -> mutateActiveSheet {
                it.copy(colCount = (it.colCount + 1).coerceAtMost(20))
            }
            OfficeEditorIntent.DeleteSelectedCol -> mutateActiveSheet {
                if (it.colCount <= 3) it else it.copy(colCount = it.colCount - 1)
            }
            // PPT
            is OfficeEditorIntent.SelectPptTab -> setState { copy(pptTab = intent.tab, isBottomShelfExpanded = true) }
            is OfficeEditorIntent.SelectSlide -> setState {
                val maxIdx = ((slideDeckSnapshot?.slides?.size ?: 1) - 1).coerceAtLeast(0)
                copy(activeSlideIndex = intent.index.coerceIn(0, maxIdx), selectedSlideElementId = null)
            }
            is OfficeEditorIntent.SelectSlideElement -> setState { copy(selectedSlideElementId = intent.elementId) }
            is OfficeEditorIntent.UpdateSelectedElementText -> mutateSelectedSlideElement { it.copy(text = intent.text) }
            is OfficeEditorIntent.AddSlideElement -> addSlideElement(intent.kind)
            OfficeEditorIntent.DeleteSelectedElement -> deleteSelectedSlideElement()
            is OfficeEditorIntent.SetElementFillColor -> mutateSelectedSlideElement { it.copy(fillColorArgb = intent.argb) }
            is OfficeEditorIntent.SetElementStrokeColor -> mutateSelectedSlideElement { it.copy(strokeColorArgb = intent.argb) }
            is OfficeEditorIntent.SetElementStrokeStyle -> mutateSelectedSlideElement { it.copy(strokeStyle = intent.style) }
            is OfficeEditorIntent.StepElementStrokeWidth -> mutateSelectedSlideElement {
                it.copy(strokeWidthDp = (it.strokeWidthDp + intent.deltaDp).coerceIn(0f, 8f))
            }
            is OfficeEditorIntent.ReorderSelectedElement -> reorderSelectedElement(intent.direction)
            is OfficeEditorIntent.UpdatePresenterNotes -> mutateActiveSlide { it.copy(presenterNotes = intent.notes) }
            is OfficeEditorIntent.SetSlideBackgroundColor -> mutateActiveSlide { it.copy(backgroundColorArgb = intent.argb) }
            OfficeEditorIntent.AddSlide -> addSlide()
            OfficeEditorIntent.DuplicateActiveSlide -> duplicateActiveSlide()
            OfficeEditorIntent.DeleteActiveSlide -> deleteActiveSlide()
            OfficeEditorIntent.LaunchSlideShowClicked -> launchSlideShow()
            // Find
            is OfficeEditorIntent.UpdateFindQuery -> updateFindQuery(intent.query)
            is OfficeEditorIntent.ReplaceInActiveDoc -> replaceAllMatches(intent.replacement)
        }
    }

    private fun openDocument(uriString: String, displayName: String, kind: OfficeKind) {
        loadJob?.cancel()
        autosaveTickerJob?.cancel()
        undoStack.clear()
        redoStack.clear()

        loadJob = launchSafely(
            onError = { err ->
                setState { copy(isLoading = false, errorMessage = err.message ?: "Unable to open $displayName") }
            },
        ) {
            setState {
                OfficeEditorState(
                    uriString = uriString,
                    displayName = displayName,
                    kind = kind,
                    isLoading = true,
                    isNativeLokLoaded = officeEditorPort.isNativeLokLoaded(),
                )
            }
            val pendingDraft = draftRecoveryRepository.getPendingDraft(uriString)
            when (kind) {
                OfficeKind.WORD -> {
                    val doc = officeEditorPort.loadWordDocument(uriString).getOrThrow()
                    setState {
                        copy(
                            isLoading = false,
                            wordSnapshot = doc,
                            pendingRecoveryDraft = pendingDraft,
                            isNativeLokLoaded = doc.poweredByNativeLok,
                        )
                    }
                }
                OfficeKind.EXCEL -> {
                    val wb = officeEditorPort.loadWorkbook(uriString).getOrThrow()
                    val firstCell = wb.sheets.firstOrNull()?.cells?.get("0,0")?.rawInput.orEmpty()
                    setState {
                        copy(
                            isLoading = false,
                            workbookSnapshot = wb,
                            formulaBarText = firstCell,
                            pendingRecoveryDraft = pendingDraft,
                            isNativeLokLoaded = wb.poweredByNativeLok,
                        )
                    }
                }
                OfficeKind.PPT -> {
                    val deck = officeEditorPort.loadSlideDeck(uriString).getOrThrow()
                    setState {
                        copy(
                            isLoading = false,
                            slideDeckSnapshot = deck,
                            pendingRecoveryDraft = pendingDraft,
                            isNativeLokLoaded = deck.poweredByNativeLok,
                        )
                    }
                }
            }
            startAutosaveTicker()
        }
    }

    private fun startAutosaveTicker() {
        autosaveTickerJob?.cancel()
        autosaveTickerJob = launchSafely {
            val intervalSec = preferencesRepository.autosaveIntervalSec.first().coerceIn(5, 120)
            while (isActive) {
                delay(intervalSec * 1000L)
                if (currentState.isDirty) {
                    performAtomicCheckpoint()
                }
            }
        }
    }

    private fun writeCheckpointIfDirty() {
        if (!currentState.isDirty) return
        launchSafely { performAtomicCheckpoint() }
    }

    private suspend fun performAtomicCheckpoint() {
        val bytes = encodeCurrentSnapshot() ?: return
        val checkpoint = draftRecoveryRepository.writeAtomicCheckpoint(
            targetUriString = currentState.uriString,
            displayName = currentState.displayName,
            serializedPayload = bytes,
        ).getOrNull() ?: return
        setState { copy(lastDraftSavedEpochMs = checkpoint.updatedAtEpochMs) }
    }

    private fun encodeCurrentSnapshot(): ByteArray? = when (currentState.kind) {
        OfficeKind.WORD -> currentState.wordSnapshot?.let(officeEditorPort::encodeWordDraft)
        OfficeKind.EXCEL -> currentState.workbookSnapshot?.let(officeEditorPort::encodeWorkbookDraft)
        OfficeKind.PPT -> currentState.slideDeckSnapshot?.let(officeEditorPort::encodeSlideDeckDraft)
    }

    private fun restorePendingDraft() {
        val draft = currentState.pendingRecoveryDraft ?: return
        launchSafely(
            onError = { setState { copy(pendingRecoveryDraft = null) } },
        ) {
            val bytes = draftRecoveryRepository.readDraftBytes(draft.draftCachePath)
            if (bytes != null) {
                when (currentState.kind) {
                    OfficeKind.WORD -> setState {
                        copy(wordSnapshot = officeEditorPort.decodeWordDraft(bytes), isDirty = true, pendingRecoveryDraft = null)
                    }
                    OfficeKind.EXCEL -> {
                        val wb = officeEditorPort.decodeWorkbookDraft(bytes)
                        val active = wb.sheets.getOrNull(wb.activeSheetIndex)
                        val cellInput = active?.cells?.get("${currentState.selectedRow},${currentState.selectedCol}")?.rawInput.orEmpty()
                        setState {
                            copy(workbookSnapshot = wb, formulaBarText = cellInput, isDirty = true, pendingRecoveryDraft = null)
                        }
                    }
                    OfficeKind.PPT -> setState {
                        copy(slideDeckSnapshot = officeEditorPort.decodeSlideDeckDraft(bytes), isDirty = true, pendingRecoveryDraft = null)
                    }
                }
                sendEffect(OfficeEditorEffect.ShowMessage("Unsaved draft restored"))
            } else {
                setState { copy(pendingRecoveryDraft = null) }
            }
        }
    }

    private fun discardPendingDraft() {
        val target = currentState.uriString
        setState { copy(pendingRecoveryDraft = null) }
        launchSafely { draftRecoveryRepository.discardDraft(target) }
    }

    private fun saveDocument() {
        if (currentState.isSaving) return
        // Zero SAVE_HEADER lock (CD-04 / 03_PRD.md §4.3)
        launchSafely(
            onError = { err ->
                setState { copy(isSaving = false) }
                sendEffect(OfficeEditorEffect.ShowMessage(err.message ?: "Unable to save document"))
            },
        ) {
            setState { copy(isSaving = true) }
            val savedItem = when (currentState.kind) {
                OfficeKind.WORD -> {
                    val doc = currentState.wordSnapshot ?: return@launchSafely
                    officeEditorPort.saveWordDocument(currentState.uriString, doc).getOrThrow()
                }
                OfficeKind.EXCEL -> {
                    val wb = currentState.workbookSnapshot ?: return@launchSafely
                    officeEditorPort.saveWorkbook(currentState.uriString, wb).getOrThrow()
                }
                OfficeKind.PPT -> {
                    val deck = currentState.slideDeckSnapshot ?: return@launchSafely
                    officeEditorPort.saveSlideDeck(currentState.uriString, deck).getOrThrow()
                }
            }
            draftRecoveryRepository.discardDraft(currentState.uriString)
            setState {
                copy(
                    isSaving = false,
                    isDirty = false,
                    uriString = savedItem.uriString,
                    displayName = savedItem.displayName,
                    lastDraftSavedEpochMs = System.currentTimeMillis(),
                )
            }
            sendEffect(OfficeEditorEffect.ShowMessage("Saved ${savedItem.displayName}"))
        }
    }

    private fun exportToPdf() {
        launchSafely(
            onError = { sendEffect(OfficeEditorEffect.ShowMessage(it.message ?: "Export PDF failed")) },
        ) {
            if (currentState.isDirty) saveDocument()
            val res = pdfPowerPort.documentToPdf(currentState.uriString).getOrThrow()
            sendEffect(OfficeEditorEffect.ShowMessage("Exported ${res.outputDisplayName}"))
        }
    }

    private fun printAsPdf() {
        launchSafely(
            onError = { sendEffect(OfficeEditorEffect.ShowMessage(it.message ?: "Print preparation failed")) },
        ) {
            val res = pdfPowerPort.documentToPdf(currentState.uriString).getOrThrow()
            sendEffect(OfficeEditorEffect.LaunchPdfPrint(res.outputUriString, currentState.displayName))
        }
    }

    private fun pushUndoMemento() {
        val m = EditorMemento(
            word = currentState.wordSnapshot,
            workbook = currentState.workbookSnapshot,
            slideDeck = currentState.slideDeckSnapshot,
        )
        if (undoStack.size >= 30) undoStack.removeFirst()
        undoStack.addLast(m)
        redoStack.clear()
        setState { copy(canUndo = undoStack.isNotEmpty(), canRedo = false) }
    }

    private fun performUndo() {
        if (undoStack.isEmpty()) return
        val current = EditorMemento(currentState.wordSnapshot, currentState.workbookSnapshot, currentState.slideDeckSnapshot)
        redoStack.addLast(current)
        val prev = undoStack.removeLast()
        setState {
            copy(
                wordSnapshot = prev.word ?: wordSnapshot,
                workbookSnapshot = prev.workbook ?: workbookSnapshot,
                slideDeckSnapshot = prev.slideDeck ?: slideDeckSnapshot,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                isDirty = true,
            )
        }
    }

    private fun performRedo() {
        if (redoStack.isEmpty()) return
        val current = EditorMemento(currentState.wordSnapshot, currentState.workbookSnapshot, currentState.slideDeckSnapshot)
        undoStack.addLast(current)
        val next = redoStack.removeLast()
        setState {
            copy(
                wordSnapshot = next.word ?: wordSnapshot,
                workbookSnapshot = next.workbook ?: workbookSnapshot,
                slideDeckSnapshot = next.slideDeck ?: slideDeckSnapshot,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                isDirty = true,
            )
        }
    }

    private fun updateParagraphText(index: Int, newText: String) {
        val doc = currentState.wordSnapshot ?: return
        if (index !in doc.paragraphs.indices) return
        val oldText = doc.paragraphs[index].text
        if (oldText == newText) return
        pushUndoMemento()
        val updatedParagraphs = doc.paragraphs.toMutableList()
        updatedParagraphs[index] = updatedParagraphs[index].copy(text = newText)
        val updatedChanges = if (doc.trackChangesEnabled) {
            val isAdding = newText.length >= oldText.length
            val delta = if (isAdding) {
                newText.removePrefix(oldText).trim()
            } else {
                oldText.removePrefix(newText).trim()
            }
            if (delta.isNotEmpty()) {
                doc.trackChanges + TrackChangeItem(
                    id = "tc_${UUID.randomUUID().toString().take(8)}",
                    kind = if (isAdding) TrackChangeKind.INSERTION else TrackChangeKind.DELETION,
                    paragraphIndex = index,
                    text = delta,
                    timestampEpochMs = System.currentTimeMillis(),
                )
            } else {
                doc.trackChanges
            }
        } else {
            doc.trackChanges
        }
        setState {
            copy(
                wordSnapshot = doc.copy(paragraphs = updatedParagraphs, trackChanges = updatedChanges),
                isDirty = true,
            )
        }
    }

    private fun mutateSelectedParagraph(transform: (WordParagraph) -> WordParagraph) {
        val doc = currentState.wordSnapshot ?: return
        val idx = currentState.selectedParagraphIndex.coerceIn(0, (doc.paragraphs.size - 1).coerceAtLeast(0))
        if (idx !in doc.paragraphs.indices) return
        pushUndoMemento()
        val updated = doc.paragraphs.toMutableList()
        updated[idx] = transform(updated[idx])
        setState { copy(wordSnapshot = doc.copy(paragraphs = updated), isDirty = true) }
    }

    private fun mutateWord(transform: (WordDocumentSnapshot) -> WordDocumentSnapshot) {
        val doc = currentState.wordSnapshot ?: return
        pushUndoMemento()
        setState { copy(wordSnapshot = transform(doc), isDirty = true) }
    }

    private fun addWordParagraph(initialText: String) {
        val doc = currentState.wordSnapshot ?: return
        pushUndoMemento()
        val nextPara = WordParagraph(id = "p_${UUID.randomUUID().toString().take(6)}", text = initialText)
        val list = doc.paragraphs + nextPara
        setState {
            copy(
                wordSnapshot = doc.copy(paragraphs = list),
                selectedParagraphIndex = list.lastIndex,
                isDirty = true,
            )
        }
    }

    private fun addReviewComment(commentText: String) {
        val clean = commentText.trim()
        if (clean.isEmpty()) return
        val doc = currentState.wordSnapshot ?: return
        pushUndoMemento()
        val item = TrackChangeItem(
            id = "${doc.trackChanges.size + 1}",
            kind = TrackChangeKind.COMMENT,
            paragraphIndex = currentState.selectedParagraphIndex.coerceIn(0, (doc.paragraphs.size - 1).coerceAtLeast(0)),
            text = clean,
            timestampEpochMs = System.currentTimeMillis(),
        )
        setState { copy(wordSnapshot = doc.copy(trackChanges = doc.trackChanges + item), isDirty = true) }
    }

    private fun resolveTrackChange(changeId: String, accept: Boolean) {
        val doc = currentState.wordSnapshot ?: return
        val target = doc.trackChanges.firstOrNull { it.id == changeId } ?: return
        pushUndoMemento()
        val remaining = doc.trackChanges.filterNot { it.id == changeId }
        val updatedParagraphs = doc.paragraphs.toMutableList()
        if (!accept && target.kind == TrackChangeKind.DELETION && target.paragraphIndex in updatedParagraphs.indices) {
            val p = updatedParagraphs[target.paragraphIndex]
            updatedParagraphs[target.paragraphIndex] = p.copy(text = (p.text + " " + target.text).trim())
        }
        setState {
            copy(
                wordSnapshot = doc.copy(paragraphs = updatedParagraphs, trackChanges = remaining),
                isDirty = true,
            )
        }
    }

    private fun selectSheet(sheetIndex: Int) {
        val wb = currentState.workbookSnapshot ?: return
        val idx = sheetIndex.coerceIn(0, (wb.sheets.size - 1).coerceAtLeast(0))
        val cellVal = wb.sheets[idx].cells["0,0"]?.rawInput.orEmpty()
        setState {
            copy(
                workbookSnapshot = wb.copy(activeSheetIndex = idx),
                selectedRow = 0,
                selectedCol = 0,
                formulaBarText = cellVal,
            )
        }
    }

    private fun addSheet(name: String) {
        val wb = currentState.workbookSnapshot ?: return
        pushUndoMemento()
        val sheetName = name.trim().ifBlank { "Sheet${wb.sheets.size + 1}" }
        val next = wb.sheets + WorksheetData(name = sheetName)
        setState {
            copy(
                workbookSnapshot = wb.copy(sheets = next, activeSheetIndex = next.lastIndex),
                selectedRow = 0,
                selectedCol = 0,
                formulaBarText = "",
                isDirty = true,
            )
        }
    }

    private fun renameActiveSheet(newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty()) return
        val wb = currentState.workbookSnapshot ?: return
        pushUndoMemento()
        val list = wb.sheets.toMutableList()
        list[wb.activeSheetIndex] = list[wb.activeSheetIndex].copy(name = clean)
        setState { copy(workbookSnapshot = wb.copy(sheets = list), isDirty = true) }
    }

    private fun deleteActiveSheet() {
        val wb = currentState.workbookSnapshot ?: return
        if (wb.sheets.size <= 1) return
        pushUndoMemento()
        val list = wb.sheets.toMutableList()
        list.removeAt(wb.activeSheetIndex)
        val nextIdx = wb.activeSheetIndex.coerceAtMost(list.lastIndex)
        setState {
            copy(
                workbookSnapshot = wb.copy(sheets = list, activeSheetIndex = nextIdx),
                selectedRow = 0,
                selectedCol = 0,
                isDirty = true,
            )
        }
    }

    private fun selectCell(row: Int, col: Int) {
        val wb = currentState.workbookSnapshot ?: return
        val sheet = wb.sheets.getOrNull(wb.activeSheetIndex) ?: return
        val raw = sheet.cells["$row,$col"]?.rawInput.orEmpty()
        setState { copy(selectedRow = row, selectedCol = col, formulaBarText = raw) }
    }

    private fun commitFormulaBar() {
        val input = currentState.formulaBarText
        mutateSelectedCell { existing ->
            existing.copy(rawInput = input)
        }
    }

    private fun mutateSelectedCell(transform: (ExcelCell) -> ExcelCell) {
        val r = currentState.selectedRow
        val c = currentState.selectedCol
        mutateActiveSheet { sheet ->
            val key = "$r,$c"
            val current = sheet.cells[key] ?: ExcelCell(row = r, col = c)
            val updated = transform(current)
            val nextCells = sheet.cells.toMutableMap()
            nextCells[key] = updated
            ExcelFormulaEvaluator.evaluateSheet(sheet.copy(cells = nextCells))
        }
    }

    private fun mutateActiveSheet(transform: (WorksheetData) -> WorksheetData) {
        val wb = currentState.workbookSnapshot ?: return
        val idx = wb.activeSheetIndex.coerceIn(0, (wb.sheets.size - 1).coerceAtLeast(0))
        pushUndoMemento()
        val list = wb.sheets.toMutableList()
        list[idx] = ExcelFormulaEvaluator.evaluateSheet(transform(list[idx]))
        setState { copy(workbookSnapshot = wb.copy(sheets = list), isDirty = true) }
    }

    private fun mutateActiveSlide(transform: (SlidePageData) -> SlidePageData) {
        val deck = currentState.slideDeckSnapshot ?: return
        val idx = currentState.activeSlideIndex.coerceIn(0, (deck.slides.size - 1).coerceAtLeast(0))
        pushUndoMemento()
        val list = deck.slides.toMutableList()
        list[idx] = transform(list[idx])
        setState { copy(slideDeckSnapshot = deck.copy(slides = list), isDirty = true) }
    }

    private fun mutateSelectedSlideElement(transform: (SlideElement) -> SlideElement) {
        val targetId = currentState.selectedSlideElementId ?: return
        mutateActiveSlide { slide ->
            slide.copy(
                elements = slide.elements.map { el -> if (el.id == targetId) transform(el) else el },
            )
        }
    }

    private fun addSlideElement(kind: String) {
        val deck = currentState.slideDeckSnapshot ?: return
        val slide = deck.slides.getOrNull(currentState.activeSlideIndex) ?: return
        val nextZ = (slide.elements.maxOfOrNull { it.zOrder } ?: 0) + 1
        val el = SlideElement(
            id = "el_${UUID.randomUUID().toString().take(8)}",
            kind = kind,
            text = if (kind == "TEXT") "New Textbox" else "Shape",
            xFraction = 0.14f,
            yFraction = 0.25f,
            widthFraction = 0.50f,
            heightFraction = 0.24f,
            zOrder = nextZ,
        )
        mutateActiveSlide { it.copy(elements = it.elements + el) }
        setState { copy(selectedSlideElementId = el.id) }
    }

    private fun deleteSelectedSlideElement() {
        val id = currentState.selectedSlideElementId ?: return
        mutateActiveSlide { it.copy(elements = it.elements.filterNot { el -> el.id == id }) }
        setState { copy(selectedSlideElementId = null) }
    }

    private fun reorderSelectedElement(direction: ZOrderDirection) {
        val id = currentState.selectedSlideElementId ?: return
        mutateActiveSlide { slide ->
            val sorted = slide.elements.sortedBy { it.zOrder }.toMutableList()
            val idx = sorted.indexOfFirst { it.id == id }
            if (idx !in sorted.indices) return@mutateActiveSlide slide
            val item = sorted.removeAt(idx)
            val newIdx = when (direction) {
                ZOrderDirection.BRING_FRONT -> sorted.size
                ZOrderDirection.FORWARD -> (idx + 1).coerceAtMost(sorted.size)
                ZOrderDirection.BACKWARD -> (idx - 1).coerceAtLeast(0)
                ZOrderDirection.SEND_BACK -> 0
            }
            sorted.add(newIdx, item)
            slide.copy(elements = sorted.mapIndexed { z, el -> el.copy(zOrder = z) })
        }
    }

    private fun addSlide() {
        val deck = currentState.slideDeckSnapshot ?: return
        pushUndoMemento()
        val newSlide = SlidePageData(
            id = "slide_${UUID.randomUUID().toString().take(6)}",
            elements = listOf(SlideElement(id = "el_new_${deck.slides.size}", kind = "TEXT", text = "Slide ${deck.slides.size + 1}")),
        )
        val updated = deck.slides + newSlide
        setState {
            copy(
                slideDeckSnapshot = deck.copy(slides = updated),
                activeSlideIndex = updated.lastIndex,
                selectedSlideElementId = null,
                isDirty = true,
            )
        }
    }

    private fun duplicateActiveSlide() {
        val deck = currentState.slideDeckSnapshot ?: return
        val active = deck.slides.getOrNull(currentState.activeSlideIndex) ?: return
        pushUndoMemento()
        val dup = active.copy(id = "slide_${UUID.randomUUID().toString().take(6)}")
        val list = deck.slides.toMutableList()
        list.add(currentState.activeSlideIndex + 1, dup)
        setState {
            copy(
                slideDeckSnapshot = deck.copy(slides = list),
                activeSlideIndex = currentState.activeSlideIndex + 1,
                isDirty = true,
            )
        }
    }

    private fun deleteActiveSlide() {
        val deck = currentState.slideDeckSnapshot ?: return
        if (deck.slides.size <= 1) return
        pushUndoMemento()
        val list = deck.slides.toMutableList()
        list.removeAt(currentState.activeSlideIndex)
        val nextIdx = currentState.activeSlideIndex.coerceAtMost(list.lastIndex)
        setState {
            copy(
                slideDeckSnapshot = deck.copy(slides = list),
                activeSlideIndex = nextIdx,
                selectedSlideElementId = null,
                isDirty = true,
            )
        }
    }

    private fun launchSlideShow() {
        launchSafely {
            if (currentState.isDirty) {
                performAtomicCheckpoint()
                val deck = currentState.slideDeckSnapshot
                if (deck != null) {
                    officeEditorPort.saveSlideDeck(currentState.uriString, deck)
                    setState { copy(isDirty = false) }
                }
            }
            sendEffect(
                OfficeEditorEffect.LaunchSlideShow(
                    uriString = currentState.uriString,
                    displayName = currentState.displayName,
                    startSlideIndex = currentState.activeSlideIndex,
                ),
            )
        }
    }

    private fun updateFindQuery(query: String) {
        val count = if (query.isBlank()) 0 else when (currentState.kind) {
            OfficeKind.WORD -> currentState.wordSnapshot?.paragraphs?.sumOf {
                Regex.escape(query).toRegex(RegexOption.IGNORE_CASE).findAll(it.text).count()
            } ?: 0
            OfficeKind.EXCEL -> {
                val sheet = currentState.workbookSnapshot?.let { it.sheets.getOrNull(it.activeSheetIndex) }
                sheet?.cells?.values?.count {
                    it.rawInput.contains(query, ignoreCase = true) || it.evaluatedDisplay.contains(query, ignoreCase = true)
                } ?: 0
            }
            OfficeKind.PPT -> currentState.slideDeckSnapshot?.slides?.sumOf { s ->
                s.elements.count { it.text.contains(query, ignoreCase = true) }
            } ?: 0
        }
        setState { copy(findQuery = query, findMatchCount = count) }
    }

    private fun replaceAllMatches(replacement: String) {
        val q = currentState.findQuery
        if (q.isBlank()) return
        when (currentState.kind) {
            OfficeKind.WORD -> mutateWord { doc ->
                doc.copy(
                    paragraphs = doc.paragraphs.map { p ->
                        p.copy(text = p.text.replace(q, replacement, ignoreCase = true))
                    },
                )
            }
            OfficeKind.EXCEL -> mutateActiveSheet { sheet ->
                val next = sheet.cells.mapValues { (_, cell) ->
                    cell.copy(rawInput = cell.rawInput.replace(q, replacement, ignoreCase = true))
                }
                sheet.copy(cells = next)
            }
            OfficeKind.PPT -> {
                val deck = currentState.slideDeckSnapshot ?: return
                pushUndoMemento()
                val slides = deck.slides.map { s ->
                    s.copy(elements = s.elements.map { el -> el.copy(text = el.text.replace(q, replacement, ignoreCase = true)) })
                }
                setState { copy(slideDeckSnapshot = deck.copy(slides = slides), isDirty = true) }
            }
        }
        updateFindQuery(q)
    }
}
