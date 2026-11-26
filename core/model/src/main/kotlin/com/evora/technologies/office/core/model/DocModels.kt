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
        val allSupportedExtensions: Set<String> = entries.flatMapTo(mutableSetOf()) { it.extensions }

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

enum class AppLanguage(
    val tag: String,
    val nativeName: String,
    val regionSubtitle: String,
) {
    EN("en", "English", "United States / Global"),
    VI("vi", "Tiếng Việt", "Việt Nam"),
    KO("ko", "한국어", "대한민국 (HWP Ready)"),
    JA("ja", "日本語", "日本"),
    ES("es", "Español", "LATAM / España"),
    PT_BR("pt-BR", "Português", "Brasil"),
    ID("id", "Bahasa Indonesia", "Indonesia"),
    HI("hi", "हिन्दी", "India"),
    DE("de", "Deutsch", "Deutschland"),
    FR("fr", "Français", "France"),
    ;

    companion object {
        fun fromTag(tag: String): AppLanguage {
            val normalized = when {
                tag.equals("in", ignoreCase = true) || tag.startsWith("in-", ignoreCase = true) -> "id"
                else -> tag
            }
            return entries.firstOrNull { it.tag.equals(normalized, ignoreCase = true) } ?: EN
        }
    }
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
    val lastScrollOffsetY: Float = 0f,
    val lastZoom: Float = 1f,
    val isPasswordProtected: Boolean = false,
    val hasPendingDraft: Boolean = false,
)

data class TrashItem(
    val trashId: String,
    val originalUriString: String,
    val cachedFilePath: String,
    val displayName: String,
    val extension: String,
    val format: DocFormat,
    val sizeBytes: Long,
    val trashedAtEpochMs: Long,
    val expiresAtEpochMs: Long,
)

data class DraftCheckpoint(
    val targetUriString: String,
    val draftCachePath: String,
    val displayName: String,
    val updatedAtEpochMs: Long,
    val hasPendingRecovery: Boolean,
)

data class SignatureItem(
    val id: String,
    val assetPath: String,
    val createdAtEpochMs: Long,
)

enum class CloudProvider {
    GOOGLE_DRIVE,
    ONE_DRIVE,
}

data class CloudAccount(
    val accountId: String,
    val provider: CloudProvider,
    val email: String,
    val displayName: String,
    val avatarUri: String? = null,
    val encryptedTokenAlias: String,
)

data class ReaderBookmarkState(
    val uriString: String,
    val lastPageIndex: Int = 0,
    val totalPages: Int = 0,
    val scrollOffsetY: Float = 0f,
    val zoom: Float = 1f,
    val readerTheme: ReaderTheme = ReaderTheme.LIGHT,
    val isContinuousScroll: Boolean = true,
)

data class OutlineNode(
    val title: String,
    val pageIndex: Int,
    val level: Int = 0,
    val children: List<OutlineNode> = emptyList(),
)

data class PdfSearchMatch(
    val pageIndex: Int,
    val matchText: String,
    val snippet: String,
)

data class ArchiveEntryItem(
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val compressedSizeBytes: Long,
    val isDirectory: Boolean,
    val isEncrypted: Boolean = false,
    val modifiedEpochMs: Long = 0L,
)

data class ExternalIntentPayload(
    val uriString: String,
    val displayName: String,
    val format: DocFormat,
    val resolvedExtension: String,
    val isExternalView: Boolean = true,
)

enum class StorageAccessMode {
    ALL_FILES,
    SAF_ONLY,
    NOT_GRANTED,
}

data class ProStatus(
    val isPro: Boolean = false,
    val hasRewardedPass: Boolean = false,
    val rewardPassExpiresAtEpochMs: Long = 0L,
    val activeSku: String? = null,
) {
    val isUnlocked: Boolean get() = isPro || hasRewardedPass

    /** True only when paid Pro subscription/lifetime is active, hiding all ad slots globally. */
    val shouldHideAds: Boolean get() = isPro
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

enum class SubscriptionPlan(
    val skuId: String,
    val title: String,
    val priceDisplay: String,
    val subtitle: String,
    val badge: String?,
) {
    WEEKLY("e05_weekly_trial", "Weekly Pro", "$4.99 / week", "Includes 3-day free trial", null),
    YEARLY("e05_yearly_pro_3999", "Annual Pro", "$39.99 / year", "7-day free trial · $3.33/mo", "BEST VALUE"),
    LIFETIME("e05_lifetime_pro", "Lifetime Pro", "$59.99 once", "One-time unlock · Forever ad-free", null),
}

data class BillingOffer(
    val plan: SubscriptionPlan,
    val formattedPrice: String = plan.priceDisplay,
    val billingSubtitle: String = plan.subtitle,
    val hasFreeTrial: Boolean = plan != SubscriptionPlan.LIFETIME,
    val isFromPlayStore: Boolean = false,
) {
    companion object {
        fun defaults(): List<BillingOffer> = SubscriptionPlan.entries.map { plan ->
            BillingOffer(
                plan = plan,
                formattedPrice = plan.priceDisplay,
                billingSubtitle = plan.subtitle,
                hasFreeTrial = plan != SubscriptionPlan.LIFETIME,
                isFromPlayStore = false,
            )
        }
    }
}

sealed interface PurchaseResult {
    data class Success(val skuId: String) : PurchaseResult
    data object Pending : PurchaseResult
    data object Cancelled : PurchaseResult
    data class BillingUnavailable(val message: String) : PurchaseResult
    data class Error(val message: String) : PurchaseResult
}
