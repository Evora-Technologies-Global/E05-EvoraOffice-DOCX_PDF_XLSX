package com.evora.technologies.office.core.model

/** Supported document and archive formats in Evora Office (excludes .rar per CD-04). */
enum class DocFormat(val extensions: Set<String>) {
    PDF(setOf("pdf", "wps", "wpt", "wpss")),
    WORD(setOf("doc", "docx", "dotx", "docm")),
    EXCEL(setOf("xls", "xlsx", "xltx", "xlsm")),
    PPT(setOf("ppt", "pptx", "pps", "ppsx", "pot")),
    ODF(setOf("odt", "ods", "odp", "odf")),
    HWP(setOf("hwp", "hwpx", "hwt", "hwdt")),
    EPUB(setOf("epub")),
    TEXT(setOf("txt", "csv", "log", "md")),
    ARCHIVE(setOf("zip", "7z")),
    ;

    companion object {
        fun fromExtension(ext: String): DocFormat? {
            val normalized = ext.trimStart('.').lowercase()
            return entries.firstOrNull { normalized in it.extensions }
        }
    }
}

enum class ReaderTheme {
    LIGHT,
    DARK,
    SEPIA,
}

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class SortOrder {
    MODIFIED_DESC,
    MODIFIED_ASC,
    NAME_ASC,
    NAME_DESC,
    SIZE_DESC,
    FORMAT,
}

data class DocumentItem(
    val id: String,
    val uriString: String,
    val displayName: String,
    val extension: String,
    val format: DocFormat,
    val sizeBytes: Long,
    val modifiedAtEpochMs: Long,
    val parentFolderName: String,
    val isFavorite: Boolean = false,
    val lastOpenedAtEpochMs: Long? = null,
    val lastPageIndex: Int = 0,
    val lastTotalPages: Int = 0,
    val isPasswordProtected: Boolean = false,
)

data class ProStatus(
    val isPro: Boolean = false,
    val hasRewardedPass: Boolean = false,
    val rewardPassExpiresAtEpochMs: Long = 0L,
    val activeSku: String? = null,
) {
    val isUnlocked: Boolean get() = isPro || hasRewardedPass
}

enum class ProFeature {
    BATCH_MERGE_OVER_3,
    HIGH_COMPRESS,
    WATERMARK,
    BATCH_OCR,
    DUAL_ID_CARD,
    PKCS7_SIGN,
    PDF_TO_DOCX,
}
