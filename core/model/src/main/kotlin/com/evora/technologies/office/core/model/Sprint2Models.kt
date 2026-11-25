package com.evora.technologies.office.core.model

enum class PdfAnnotationType {
    HIGHLIGHT,
    UNDERLINE,
    STRIKETHROUGH,
    INK,
    STICKY_NOTE,
}

data class NormalizedPoint(
    val x: Float,
    val y: Float,
)

data class PdfAnnotationDraft(
    val id: String,
    val pageIndex: Int,
    val type: PdfAnnotationType,
    val colorArgb: Long,
    val strokeWidthDp: Float = 3f,
    val points: List<NormalizedPoint> = emptyList(),
    val left: Float = 0.1f,
    val top: Float = 0.1f,
    val right: Float = 0.5f,
    val bottom: Float = 0.15f,
    val noteText: String = "",
    val author: String = "Evora User",
)

data class PdfSignaturePlacement(
    val id: String,
    val signatureAssetPath: String,
    val pageIndex: Int,
    val centerXFraction: Float = 0.5f,
    val centerYFraction: Float = 0.72f,
    val widthFraction: Float = 0.28f,
)

data class Pkcs7CertificateInfo(
    val signatureCount: Int,
    val signerName: String,
    val issuerName: String,
    val serialHex: String,
    val validFromEpochMs: Long,
    val validToEpochMs: Long,
    val signedAtEpochMs: Long?,
    val isIntegrityValid: Boolean,
    val isCurrentlyWithinValidity: Boolean,
    val reason: String = "",
)

enum class PdfToolKind {
    MERGE,
    SPLIT,
    COMPRESS,
    WATERMARK,
    SET_PASSWORD,
    REMOVE_PASSWORD,
    PKCS7_SIGN,
    IMAGE_TO_PDF,
    TEXT_TO_PDF,
    DOC_TO_PDF,
    PDF_TO_IMAGES,
    PDF_TO_DOCX,
}

enum class SplitMode {
    RANGE,
    EVERY_N,
    SELECTED_PAGES,
}

enum class CompressQuality(
    val label: String,
    val description: String,
    val targetDpi: Int,
    val jpegQuality: Float,
) {
    LOW("Low Compression", "Best crispness (~150 DPI)", 150, 0.85f),
    MEDIUM("Balanced", "Recommended (~110 DPI)", 110, 0.65f),
    HIGH("High Compression", "Smallest file (~72 DPI)", 72, 0.45f),
}

enum class PageSizePreset(val label: String) {
    A4("A4"),
    LETTER("Letter"),
    FIT("Fit Image"),
}

data class WatermarkConfig(
    val text: String = "CONFIDENTIAL",
    val fontSizePt: Float = 42f,
    val colorArgb: Long = 0xFF64748BL,
    val opacity: Float = 0.30f,
    val angleDegrees: Float = -45f,
    val tiled: Boolean = false,
)

data class PdfToolResult(
    val toolKind: PdfToolKind,
    val outputUriString: String,
    val outputDisplayName: String,
    val beforeSizeBytes: Long,
    val afterSizeBytes: Long,
    val summaryMessage: String,
    val extraOutputUris: List<String> = emptyList(),
) {
    val savedPercent: Int
        get() = if (beforeSizeBytes > 0 && afterSizeBytes < beforeSizeBytes) {
            (((beforeSizeBytes - afterSizeBytes).toDouble() / beforeSizeBytes.toDouble()) * 100.0).toInt()
        } else {
            0
        }
}

enum class OcrScript(val label: String, val sampleHint: String) {
    LATIN("Latin (EN/VI/EU)", "English, Vietnamese, Spanish, French, German"),
    CHINESE("Chinese (ZH)", "Simplified & Traditional Chinese"),
    DEVANAGARI("Devanagari (HI)", "Hindi, Marathi, Sanskrit"),
    JAPANESE("Japanese (JA)", "Kanji, Hiragana, Katakana"),
    KOREAN("Korean (KO)", "Hangul"),
}

enum class ScanMode(val label: String) {
    DOCUMENT("Document"),
    ID_CARD("ID Card"),
    OCR("OCR"),
    QR_BARCODE("QR / Barcode"),
}

enum class ScanFilter(val label: String) {
    ORIGINAL("Original"),
    MAGIC_COLOR("Magic Color"),
    GRAYSCALE("Grayscale"),
    BLACK_WHITE("B&W"),
}

sealed interface QrDecodedAction {
    val rawValue: String

    data class Url(override val rawValue: String, val url: String) : QrDecodedAction
    data class Wifi(
        override val rawValue: String,
        val ssid: String,
        val password: String,
        val securityType: String,
    ) : QrDecodedAction

    data class VCard(
        override val rawValue: String,
        val fullName: String,
        val phone: String?,
        val email: String?,
        val org: String?,
    ) : QrDecodedAction

    data class Email(
        override val rawValue: String,
        val address: String,
        val subject: String?,
    ) : QrDecodedAction

    data class Phone(override val rawValue: String, val number: String) : QrDecodedAction
    data class PlainText(override val rawValue: String) : QrDecodedAction
}

enum class OfficeKind(val label: String) {
    WORD("Word"),
    EXCEL("Excel"),
    PPT("PowerPoint"),
}

enum class TextAlignOption {
    LEFT,
    CENTER,
    RIGHT,
    JUSTIFY,
}

data class DocTemplatePreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val kind: OfficeKind,
    val assetFileName: String,
    val defaultFileName: String,
)

data class WordParagraph(
    val id: String,
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val fontSizeSp: Int = 14,
    val textColorArgb: Long = 0xFF0F172AL,
    val highlightColorArgb: Long? = null,
    val alignment: TextAlignOption = TextAlignOption.LEFT,
    val isBullet: Boolean = false,
    val isNumbered: Boolean = false,
    val indentLevel: Int = 0,
)

enum class TrackChangeKind {
    INSERTION,
    DELETION,
    COMMENT,
}

data class TrackChangeItem(
    val id: String,
    val kind: TrackChangeKind,
    val paragraphIndex: Int,
    val text: String,
    val author: String = "Evora User",
    val timestampEpochMs: Long,
)

data class WordDocumentSnapshot(
    val title: String,
    val paragraphs: List<WordParagraph>,
    val trackChangesEnabled: Boolean = false,
    val trackChanges: List<TrackChangeItem> = emptyList(),
    val pageMarginDp: Int = 24,
    val isLandscape: Boolean = false,
    val inkNoteSummary: String = "",
    val poweredByNativeLok: Boolean = false,
)

enum class CellNumberFormat(val label: String) {
    GENERAL("General"),
    NUMBER("Number"),
    CURRENCY("Currency ($)"),
    ACCOUNTING("Accounting"),
    DATE("Date"),
    PERCENTAGE("Percentage (%)"),
    FRACTION("Fraction"),
}

enum class CellHorizontalAlign {
    LEFT,
    CENTER,
    RIGHT,
}

enum class CellVerticalAlign {
    TOP,
    MIDDLE,
    BOTTOM,
}

data class ExcelCell(
    val row: Int,
    val col: Int,
    val rawInput: String = "",
    val evaluatedDisplay: String = "",
    val bold: Boolean = false,
    val fillColorArgb: Long? = null,
    val textColorArgb: Long = 0xFF0F172AL,
    val hAlign: CellHorizontalAlign = CellHorizontalAlign.LEFT,
    val vAlign: CellVerticalAlign = CellVerticalAlign.MIDDLE,
    val numberFormat: CellNumberFormat = CellNumberFormat.GENERAL,
    val mergedSpanRows: Int = 1,
    val mergedSpanCols: Int = 1,
)

data class WorksheetData(
    val name: String,
    val rowCount: Int = 20,
    val colCount: Int = 8,
    val frozenRows: Int = 1,
    val frozenCols: Int = 0,
    val colWidthsDp: Map<Int, Int> = emptyMap(),
    val rowHeightsDp: Map<Int, Int> = emptyMap(),
    val cells: Map<String, ExcelCell> = emptyMap(), // key = "R,C"
)

data class WorkbookSnapshot(
    val title: String,
    val sheets: List<WorksheetData>,
    val activeSheetIndex: Int = 0,
    val poweredByNativeLok: Boolean = false,
)

enum class SlideStrokeStyle {
    SOLID,
    DASHED,
    NONE,
}

data class SlideElement(
    val id: String,
    val kind: String, // "TEXT" | "SHAPE_RECT" | "SHAPE_OVAL"
    val text: String = "",
    val xFraction: Float = 0.1f,
    val yFraction: Float = 0.2f,
    val widthFraction: Float = 0.8f,
    val heightFraction: Float = 0.22f,
    val fillColorArgb: Long = 0xFFE6F1FFL,
    val strokeColorArgb: Long = 0xFF186FD4L,
    val strokeWidthDp: Float = 2f,
    val strokeStyle: SlideStrokeStyle = SlideStrokeStyle.SOLID,
    val zOrder: Int = 0,
)

data class SlidePageData(
    val id: String,
    val elements: List<SlideElement>,
    val presenterNotes: String = "",
    val backgroundColorArgb: Long = 0xFFFFFFFFL,
)

data class SlideDeckSnapshot(
    val title: String,
    val slides: List<SlidePageData>,
    val poweredByNativeLok: Boolean = false,
)
