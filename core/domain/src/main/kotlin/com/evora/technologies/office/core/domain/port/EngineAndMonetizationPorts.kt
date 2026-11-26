package com.evora.technologies.office.core.domain.port

import com.evora.technologies.office.core.model.AppThemeMode
import com.evora.technologies.office.core.model.ArchiveEntryItem
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.ExternalIntentPayload
import com.evora.technologies.office.core.model.OutlineNode
import com.evora.technologies.office.core.model.PdfSearchMatch
import com.evora.technologies.office.core.model.ProFeature
import com.evora.technologies.office.core.model.ProStatus
import com.evora.technologies.office.core.model.ReaderBookmarkState
import com.evora.technologies.office.core.model.ReaderTheme
import com.evora.technologies.office.core.model.SortOrder
import com.evora.technologies.office.core.model.TrashItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstracts subscription, lifetime unlock, and 24h rewarded pass entitlements.
 * Backed by a local offline stub in Sprints 1–3 and Play Billing 8.0.0 in Sprint 4 (CD-07).
 */
interface ProEntitlementPort {
    val proStatus: StateFlow<ProStatus>
    suspend fun hasAccess(feature: ProFeature): Boolean
    suspend fun grantRewardedPass(hours: Int)
    suspend fun restorePurchases(): Boolean
    suspend fun setQaProOverride(enabled: Boolean) = Unit
    suspend fun hasFreeDailyPdfToDocxAvailable(): Boolean = false
    suspend fun consumeFreeDailyPdfToDocx(): Unit = Unit
}

/**
 * Hook for post-milestone and home-tab ad placements.
 * Bound to a strict no-op until core features F-01..F-09 reach Done (CD-07).
 */
interface AdPlacementHook {
    fun onMilestoneCompleted(screenId: String)
    fun onHomeHubEntered()
}

interface PreferencesRepository {
    val onboardingCompleted: Flow<Boolean>
    val selectedLocaleTag: Flow<String?>
    val themeMode: Flow<AppThemeMode>
    val defaultSortOrder: Flow<SortOrder>
    val autosaveIntervalSec: Flow<Int>
    val defaultAppPromptDismissed: Flow<Boolean>
    val externalOpenCount: Flow<Int>

    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setSelectedLocaleTag(tag: String)
    suspend fun setThemeMode(mode: AppThemeMode)
    suspend fun setDefaultSortOrder(sortOrder: SortOrder)
    suspend fun setAutosaveIntervalSec(seconds: Int)
    suspend fun setDefaultAppPromptDismissed(dismissed: Boolean)
    suspend fun incrementExternalOpenCount()
}

interface DocumentRepository {
    fun observeLibraryDocuments(
        filter: DocFormat? = null,
        sortOrder: SortOrder = SortOrder.MODIFIED_DESC,
    ): Flow<List<DocumentItem>>

    fun observeRecentDocuments(): Flow<List<DocumentItem>>
    fun observeFavoriteDocuments(): Flow<List<DocumentItem>>
    fun observeTrashItems(): Flow<List<TrashItem>>
    fun searchDocuments(query: String, filter: DocFormat? = null): Flow<List<DocumentItem>>

    suspend fun refreshMediaStoreIndex(): Result<Int>
    suspend fun getDocumentMeta(uriString: String): DocumentItem?
    suspend fun getReaderBookmark(uriString: String): ReaderBookmarkState?
    suspend fun toggleFavorite(uriString: String)
    suspend fun recordDocumentOpened(item: DocumentItem, totalPages: Int = item.lastTotalPages)
    suspend fun updateReadingPosition(
        uriString: String,
        pageIndex: Int,
        totalPages: Int,
        scrollOffsetY: Float,
        zoom: Float,
        readerTheme: ReaderTheme,
        isContinuousScroll: Boolean,
    )

    suspend fun renameDocument(uriString: String, newBaseName: String): Result<DocumentItem>
    suspend fun moveToTrash(uriString: String): Result<TrashItem>
    suspend fun restoreFromTrash(trashId: String): Result<Unit>
    suspend fun deleteForeverFromTrash(trashId: String): Result<Unit>
    suspend fun emptyTrash(): Result<Int>
    suspend fun copyDocumentToFolder(uriString: String, targetFolderName: String): Result<DocumentItem>
    suspend fun moveDocumentToFolder(uriString: String, targetFolderName: String): Result<DocumentItem>
    suspend fun importExternalUri(
        uriString: String,
        displayNameHint: String?,
        mimeTypeHint: String?,
    ): Result<ExternalIntentPayload>

    suspend fun registerSafUri(uriString: String): Result<DocumentItem>
}

class DocumentPasswordRequiredException(message: String = "Password required") : Exception(message)
class DocumentInvalidPasswordException(message: String = "Incorrect password") : Exception(message)

data class PdfDocumentInfo(
    val pageCount: Int,
    val outline: List<OutlineNode>,
)

data class PdfPageRaster(
    val width: Int,
    val height: Int,
    val argbPixels: IntArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PdfPageRaster) return false
        return width == other.width && height == other.height && argbPixels.contentEquals(other.argbPixels)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + argbPixels.contentHashCode()
        return result
    }
}

interface PdfEnginePort {
    suspend fun getPageCount(uriString: String, password: String? = null): Int
    suspend fun openDocumentInfo(uriString: String, password: String? = null): PdfDocumentInfo
    suspend fun renderPage(
        uriString: String,
        pageIndex: Int,
        targetWidthPx: Int,
        password: String? = null,
    ): PdfPageRaster

    suspend fun extractPageText(uriString: String, pageIndex: Int, password: String? = null): String
    suspend fun searchText(uriString: String, query: String, password: String? = null): List<PdfSearchMatch>
}

data class HwpRenderResult(
    val title: String,
    val sanitizedHtml: String,
    val plainText: String,
    val fileSizeBytes: Long,
    val isLargeFileWarning: Boolean,
)

interface HwpEnginePort {
    suspend fun renderToSanitizedHtml(uriString: String, password: String? = null): HwpRenderResult
}

data class TextChapter(
    val title: String,
    val body: String,
)

data class ParsedTextDocument(
    val title: String,
    val chapters: List<TextChapter>,
    val fullPlainText: String,
)

interface TextEpubEnginePort {
    suspend fun loadDocument(uriString: String, format: DocFormat): ParsedTextDocument
}

data class ArchiveInspectionResult(
    val archiveName: String,
    val extension: String,
    val isSupported: Boolean,
    val entries: List<ArchiveEntryItem>,
    val unsupportedReason: String? = null,
)

interface ArchiveEnginePort {
    suspend fun listEntries(uriString: String, password: String? = null): ArchiveInspectionResult
    suspend fun extractEntries(
        uriString: String,
        destinationDirPath: String,
        entryPaths: Set<String>? = null,
        password: String? = null,
    ): Result<List<String>>
}

interface OfficeEnginePort {
    fun isNativeEngineAvailable(): Boolean
    suspend fun extractReadablePreview(uriString: String, format: DocFormat): ParsedTextDocument
    suspend fun convertToPdfIfAvailable(sourceUriString: String, outPdfPath: String): Boolean = false
}
