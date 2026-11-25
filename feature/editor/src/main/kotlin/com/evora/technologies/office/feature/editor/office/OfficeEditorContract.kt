package com.evora.technologies.office.feature.editor.office

import com.evora.technologies.office.core.model.CellHorizontalAlign
import com.evora.technologies.office.core.model.CellNumberFormat
import com.evora.technologies.office.core.model.DraftCheckpoint
import com.evora.technologies.office.core.model.OfficeKind
import com.evora.technologies.office.core.model.SlideDeckSnapshot
import com.evora.technologies.office.core.model.SlideStrokeStyle
import com.evora.technologies.office.core.model.TextAlignOption
import com.evora.technologies.office.core.model.WordDocumentSnapshot
import com.evora.technologies.office.core.model.WorkbookSnapshot
import com.evora.technologies.office.core.mvi.UiEffect
import com.evora.technologies.office.core.mvi.UiIntent
import com.evora.technologies.office.core.mvi.UiState

enum class WordEditorTab(val label: String) {
    EDIT("Edit"),
    FILE("File"),
    INSERT("Insert"),
    REVIEW("Review"),
    PAGE("Page"),
    DRAW("Draw"),
    FIND("Find"),
}

enum class ExcelEditorTab(val label: String) {
    FILE("File"),
    EDIT("Edit"),
    INSERT("Insert"),
    FORMULAS("Formulas"),
    FIND("Find"),
}

enum class PptEditorTab(val label: String) {
    EDIT("Edit"),
    FILE("File"),
    INSERT("Insert"),
    FORMAT("Format"),
    ANNOTATE("Annotate"),
    PAGE("Page"),
    FIND("Find"),
}

enum class ZOrderDirection {
    BRING_FRONT,
    FORWARD,
    BACKWARD,
    SEND_BACK,
}

data class OfficeEditorState(
    val uriString: String = "",
    val displayName: String = "Document",
    val kind: OfficeKind = OfficeKind.WORD,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isDirty: Boolean = false,
    val lastDraftSavedEpochMs: Long? = null,
    val pendingRecoveryDraft: DraftCheckpoint? = null,
    val isNativeLokLoaded: Boolean = false,
    val isBottomShelfExpanded: Boolean = true,
    // Undo / Redo availability
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    // Word state
    val wordSnapshot: WordDocumentSnapshot? = null,
    val selectedParagraphIndex: Int = 0,
    val wordTab: WordEditorTab = WordEditorTab.EDIT,
    // Excel state
    val workbookSnapshot: WorkbookSnapshot? = null,
    val selectedRow: Int = 0,
    val selectedCol: Int = 0,
    val formulaBarText: String = "",
    val excelTab: ExcelEditorTab = ExcelEditorTab.EDIT,
    // PPT state
    val slideDeckSnapshot: SlideDeckSnapshot? = null,
    val activeSlideIndex: Int = 0,
    val selectedSlideElementId: String? = null,
    val pptTab: PptEditorTab = PptEditorTab.EDIT,
    // Find
    val findQuery: String = "",
    val findMatchCount: Int = 0,
    val errorMessage: String? = null,
) : UiState

sealed interface OfficeEditorIntent : UiIntent {
    data class OpenDocument(val uriString: String, val displayName: String, val kind: OfficeKind) : OfficeEditorIntent
    data object RestoreDraftClicked : OfficeEditorIntent
    data object DiscardDraftClicked : OfficeEditorIntent
    data object LifecycleStopped : OfficeEditorIntent
    data object ToggleBottomShelf : OfficeEditorIntent
    data object UndoClicked : OfficeEditorIntent
    data object RedoClicked : OfficeEditorIntent
    data object SaveClicked : OfficeEditorIntent
    data object ExportPdfClicked : OfficeEditorIntent
    data object PrintClicked : OfficeEditorIntent
    data object ShareClicked : OfficeEditorIntent
    data object BackClicked : OfficeEditorIntent
    // Word intents
    data class SelectWordTab(val tab: WordEditorTab) : OfficeEditorIntent
    data class SelectParagraph(val index: Int) : OfficeEditorIntent
    data class UpdateParagraphText(val index: Int, val text: String) : OfficeEditorIntent
    data object ToggleBold : OfficeEditorIntent
    data object ToggleItalic : OfficeEditorIntent
    data object ToggleUnderline : OfficeEditorIntent
    data object ToggleStrikethrough : OfficeEditorIntent
    data class StepFontSize(val deltaSp: Int) : OfficeEditorIntent
    data class SetTextColor(val argb: Long) : OfficeEditorIntent
    data class SetHighlightColor(val argb: Long?) : OfficeEditorIntent
    data class SetParagraphAlign(val align: TextAlignOption) : OfficeEditorIntent
    data object ToggleBulletList : OfficeEditorIntent
    data object ToggleNumberedList : OfficeEditorIntent
    data class StepIndent(val delta: Int) : OfficeEditorIntent
    data object AddWordParagraph : OfficeEditorIntent
    data class AppendWordBlock(val prefix: String) : OfficeEditorIntent
    data class ToggleTrackChanges(val enabled: Boolean) : OfficeEditorIntent
    data class AddReviewComment(val commentText: String) : OfficeEditorIntent
    data class AcceptTrackChange(val changeId: String) : OfficeEditorIntent
    data class RejectTrackChange(val changeId: String) : OfficeEditorIntent
    data class SetWordMargins(val marginDp: Int) : OfficeEditorIntent
    data class ToggleWordOrientation(val landscape: Boolean) : OfficeEditorIntent
    data class UpdateInkNoteSummary(val summary: String) : OfficeEditorIntent
    // Excel intents
    data class SelectExcelTab(val tab: ExcelEditorTab) : OfficeEditorIntent
    data class SelectExcelSheet(val sheetIndex: Int) : OfficeEditorIntent
    data class AddExcelSheet(val name: String) : OfficeEditorIntent
    data class RenameActiveSheet(val newName: String) : OfficeEditorIntent
    data object DeleteActiveSheet : OfficeEditorIntent
    data class SelectCell(val row: Int, val col: Int) : OfficeEditorIntent
    data class UpdateFormulaBar(val text: String) : OfficeEditorIntent
    data object CommitCellFormula : OfficeEditorIntent
    data class InsertFormulaFunction(val functionName: String) : OfficeEditorIntent
    data object ToggleCellBold : OfficeEditorIntent
    data class SetCellHorizontalAlign(val align: CellHorizontalAlign) : OfficeEditorIntent
    data class SetCellNumberFormat(val format: CellNumberFormat) : OfficeEditorIntent
    data object ToggleMergeRight : OfficeEditorIntent
    data object ToggleFreezeFirstRow : OfficeEditorIntent
    data object ToggleFreezeFirstCol : OfficeEditorIntent
    data class AdjustSelectedColWidth(val deltaDp: Int) : OfficeEditorIntent
    data class AdjustSelectedRowHeight(val deltaDp: Int) : OfficeEditorIntent
    data object InsertRowAfterSelected : OfficeEditorIntent
    data object DeleteSelectedRow : OfficeEditorIntent
    data object InsertColAfterSelected : OfficeEditorIntent
    data object DeleteSelectedCol : OfficeEditorIntent
    // PPT intents
    data class SelectPptTab(val tab: PptEditorTab) : OfficeEditorIntent
    data class SelectSlide(val index: Int) : OfficeEditorIntent
    data class SelectSlideElement(val elementId: String?) : OfficeEditorIntent
    data class UpdateSelectedElementText(val text: String) : OfficeEditorIntent
    data class AddSlideElement(val kind: String) : OfficeEditorIntent
    data object DeleteSelectedElement : OfficeEditorIntent
    data class SetElementFillColor(val argb: Long) : OfficeEditorIntent
    data class SetElementStrokeColor(val argb: Long) : OfficeEditorIntent
    data class SetElementStrokeStyle(val style: SlideStrokeStyle) : OfficeEditorIntent
    data class StepElementStrokeWidth(val deltaDp: Float) : OfficeEditorIntent
    data class ReorderSelectedElement(val direction: ZOrderDirection) : OfficeEditorIntent
    data class UpdatePresenterNotes(val notes: String) : OfficeEditorIntent
    data class SetSlideBackgroundColor(val argb: Long) : OfficeEditorIntent
    data object AddSlide : OfficeEditorIntent
    data object DuplicateActiveSlide : OfficeEditorIntent
    data object DeleteActiveSlide : OfficeEditorIntent
    data object LaunchSlideShowClicked : OfficeEditorIntent
    // Shared Find
    data class UpdateFindQuery(val query: String) : OfficeEditorIntent
    data class ReplaceInActiveDoc(val replacement: String) : OfficeEditorIntent
}

sealed interface OfficeEditorEffect : UiEffect {
    data object NavigateBack : OfficeEditorEffect
    data class LaunchSlideShow(val uriString: String, val displayName: String, val startSlideIndex: Int) : OfficeEditorEffect
    data class LaunchPdfPrint(val uriString: String, val title: String) : OfficeEditorEffect
    data class ShareFile(val uriString: String, val displayName: String) : OfficeEditorEffect
    data class ShowMessage(val message: String) : OfficeEditorEffect
}
