package com.evora.technologies.office.core.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.evora.technologies.office.core.data.di.IoDispatcher
import com.evora.technologies.office.core.database.dao.DocumentMetaDao
import com.evora.technologies.office.core.database.dao.DraftRecoveryDao
import com.evora.technologies.office.core.database.dao.TrashDao
import com.evora.technologies.office.core.database.entity.DocumentMetaEntity
import com.evora.technologies.office.core.database.entity.TrashEntity
import com.evora.technologies.office.core.domain.port.DocumentRepository
import com.evora.technologies.office.core.domain.port.PreferencesRepository
import com.evora.technologies.office.core.domain.util.DocumentListProcessor
import com.evora.technologies.office.core.domain.util.FileNameValidationResult
import com.evora.technologies.office.core.domain.util.FileNameValidator
import com.evora.technologies.office.core.domain.util.MagicByteSniffer
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.ExternalIntentPayload
import com.evora.technologies.office.core.model.ReaderBookmarkState
import com.evora.technologies.office.core.model.ReaderTheme
import com.evora.technologies.office.core.model.SortOrder
import com.evora.technologies.office.core.model.TrashItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val scanner: LocalDocumentScanner,
    private val documentMetaDao: DocumentMetaDao,
    private val trashDao: TrashDao,
    private val draftRecoveryDao: DraftRecoveryDao,
    private val preferencesRepository: PreferencesRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DocumentRepository {

    private val scannedDocsFlow = MutableStateFlow<List<DocumentItem>>(emptyList())

    private val mergedLibraryFlow: Flow<List<DocumentItem>> = combine(
        scannedDocsFlow,
        documentMetaDao.observeAll(),
        trashDao.observeAll(),
        draftRecoveryDao.observePending(),
    ) { scanned, metas, trash, drafts ->
        val trashedUris = trash.mapTo(HashSet()) { it.originalUriString }
        val draftUris = drafts.mapTo(HashSet()) { it.targetUriString }
        val metaMap = metas.associateBy { it.uriString }
        val combined = LinkedHashMap<String, DocumentItem>()

        scanned.forEach { doc ->
            if (doc.uriString !in trashedUris) {
                val meta = metaMap[doc.uriString]
                combined[doc.uriString] = doc.mergeWith(meta, hasPendingDraft = doc.uriString in draftUris)
            }
        }

        metas.forEach { meta ->
            if (meta.uriString !in combined && meta.uriString !in trashedUris && !meta.uriString.contains("external_imports")) {
                val ext = meta.displayName.substringAfterLast('.', "").lowercase()
                val format = runCatching { DocFormat.valueOf(meta.format) }.getOrElse {
                    DocFormat.fromExtension(ext) ?: DocFormat.TEXT
                }
                combined[meta.uriString] = DocumentItem(
                    id = meta.uriString,
                    uriString = meta.uriString,
                    displayName = meta.displayName,
                    extension = ext,
                    format = format,
                    sizeBytes = meta.sizeBytes,
                    modifiedAtEpochMs = meta.modifiedAtEpochMs,
                    parentFolderName = deriveFolderName(meta.uriString),
                    isFavorite = meta.isFavorite,
                    lastOpenedAtEpochMs = meta.lastOpenedAtEpochMs,
                    lastPageIndex = meta.lastPageIndex,
                    lastTotalPages = meta.lastTotalPages,
                    lastScrollOffsetY = meta.lastScrollOffsetY,
                    lastZoom = meta.lastZoom,
                    hasPendingDraft = meta.uriString in draftUris,
                )
            }
        }

        combined.values.toList()
    }

    override fun observeLibraryDocuments(
        filter: DocFormat?,
        sortOrder: SortOrder,
    ): Flow<List<DocumentItem>> = mergedLibraryFlow
        .map { list -> DocumentListProcessor.filterAndSort(list, filter, sortOrder) }
        .flowOn(ioDispatcher)

    override fun observeRecentDocuments(): Flow<List<DocumentItem>> = combine(
        documentMetaDao.observeRecent(),
        trashDao.observeAll(),
        draftRecoveryDao.observePending(),
    ) { recents, trash, drafts ->
        val trashed = trash.mapTo(HashSet()) { it.originalUriString }
        val draftSet = drafts.mapTo(HashSet()) { it.targetUriString }
        recents.filter { it.uriString !in trashed }.map { meta ->
            meta.toDocumentItem(hasPendingDraft = meta.uriString in draftSet)
        }
    }.flowOn(ioDispatcher)

    override fun observeFavoriteDocuments(): Flow<List<DocumentItem>> = combine(
        documentMetaDao.observeFavorites(),
        trashDao.observeAll(),
        draftRecoveryDao.observePending(),
    ) { favs, trash, drafts ->
        val trashed = trash.mapTo(HashSet()) { it.originalUriString }
        val draftSet = drafts.mapTo(HashSet()) { it.targetUriString }
        favs.filter { it.uriString !in trashed }.map { meta ->
            meta.toDocumentItem(hasPendingDraft = meta.uriString in draftSet)
        }
    }.flowOn(ioDispatcher)

    override fun observeTrashItems(): Flow<List<TrashItem>> = trashDao.observeAll()
        .map { entities ->
            entities.map { entity ->
                val ext = entity.displayName.substringAfterLast('.', "").lowercase()
                val fmt = runCatching { DocFormat.valueOf(entity.format) }.getOrElse {
                    DocFormat.fromExtension(ext) ?: DocFormat.TEXT
                }
                TrashItem(
                    trashId = entity.trashId,
                    originalUriString = entity.originalUriString,
                    cachedFilePath = entity.cachedFilePath,
                    displayName = entity.displayName,
                    extension = ext,
                    format = fmt,
                    sizeBytes = entity.sizeBytes,
                    trashedAtEpochMs = entity.trashedAtEpochMs,
                    expiresAtEpochMs = entity.expiresAtEpochMs,
                )
            }
        }
        .flowOn(ioDispatcher)

    override fun searchDocuments(query: String, filter: DocFormat?): Flow<List<DocumentItem>> =
        mergedLibraryFlow
            .map { list -> DocumentListProcessor.filterAndSort(list, filter, SortOrder.MODIFIED_DESC, query) }
            .flowOn(ioDispatcher)

    override suspend fun refreshMediaStoreIndex(): Result<Int> = withContext(ioDispatcher) {
        try {
            val docs = scanner.scanDeviceDocuments()
            scannedDocsFlow.value = docs
            Result.success(docs.size)
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun getDocumentMeta(uriString: String): DocumentItem? = withContext(ioDispatcher) {
        val fromScan = scannedDocsFlow.value.firstOrNull { it.uriString == uriString }
        val meta = documentMetaDao.getByUri(uriString)
        when {
            fromScan != null -> fromScan.mergeWith(meta, hasPendingDraft = false)
            meta != null -> meta.toDocumentItem(hasPendingDraft = false)
            else -> resolveDirectFileItem(uriString)
        }
    }

    override suspend fun getReaderBookmark(uriString: String): ReaderBookmarkState? = withContext(ioDispatcher) {
        val meta = documentMetaDao.getByUri(uriString) ?: return@withContext null
        val theme = runCatching { ReaderTheme.valueOf(meta.readerTheme) }.getOrDefault(ReaderTheme.LIGHT)
        ReaderBookmarkState(
            uriString = meta.uriString,
            lastPageIndex = meta.lastPageIndex,
            totalPages = meta.lastTotalPages,
            scrollOffsetY = meta.lastScrollOffsetY,
            zoom = meta.lastZoom,
            readerTheme = theme,
            isContinuousScroll = meta.isContinuousScroll,
        )
    }

    override suspend fun toggleFavorite(uriString: String) = withContext(ioDispatcher) {
        val existing = documentMetaDao.getByUri(uriString)
        if (existing != null) {
            documentMetaDao.setFavorite(uriString, !existing.isFavorite)
        } else {
            val item = getDocumentMeta(uriString) ?: return@withContext
            documentMetaDao.upsert(item.toEntity().copy(isFavorite = true))
        }
    }

    override suspend fun recordDocumentOpened(item: DocumentItem, totalPages: Int) = withContext(ioDispatcher) {
        val existing = documentMetaDao.getByUri(item.uriString)
        val now = System.currentTimeMillis()
        val updated = (existing ?: item.toEntity()).copy(
            displayName = item.displayName,
            format = item.format.name,
            sizeBytes = item.sizeBytes,
            modifiedAtEpochMs = item.modifiedAtEpochMs,
            lastOpenedAtEpochMs = now,
            lastTotalPages = if (totalPages > 0) totalPages else existing?.lastTotalPages ?: 0,
        )
        documentMetaDao.upsert(updated)
    }

    override suspend fun updateReadingPosition(
        uriString: String,
        pageIndex: Int,
        totalPages: Int,
        scrollOffsetY: Float,
        zoom: Float,
        readerTheme: ReaderTheme,
        isContinuousScroll: Boolean,
    ) = withContext(ioDispatcher) {
        val existing = documentMetaDao.getByUri(uriString)
        if (existing == null) {
            val item = getDocumentMeta(uriString)
            if (item != null) {
                documentMetaDao.upsert(
                    item.toEntity().copy(
                        lastOpenedAtEpochMs = System.currentTimeMillis(),
                        lastPageIndex = pageIndex,
                        lastTotalPages = totalPages,
                        lastScrollOffsetY = scrollOffsetY,
                        lastZoom = zoom,
                        readerTheme = readerTheme.name,
                        isContinuousScroll = isContinuousScroll,
                    ),
                )
            }
        } else {
            documentMetaDao.updateReadingMemory(
                uriString = uriString,
                openedAtMs = System.currentTimeMillis(),
                pageIndex = pageIndex,
                totalPages = if (totalPages > 0) totalPages else existing.lastTotalPages,
                scrollOffsetY = scrollOffsetY,
                zoom = zoom,
                readerTheme = readerTheme.name,
                isContinuousScroll = isContinuousScroll,
            )
        }
    }

    override suspend fun renameDocument(uriString: String, newBaseName: String): Result<DocumentItem> =
        withContext(ioDispatcher) {
            try {
                val current = getDocumentMeta(uriString)
                    ?: return@withContext Result.failure(IllegalArgumentException("Document not found"))
                val parsedUri = Uri.parse(uriString)
                val siblingNames: Set<String> = if (parsedUri.scheme == "file" && parsedUri.path != null) {
                    File(parsedUri.path!!).parentFile?.listFiles()?.mapTo(HashSet()) { it.name }.orEmpty() - current.displayName
                } else {
                    scannedDocsFlow.value
                        .filter { it.parentFolderName == current.parentFolderName && it.uriString != uriString }
                        .mapTo(HashSet()) { it.displayName }
                }

                val validation = FileNameValidator.validateAndPreserveExtension(
                    rawInput = newBaseName,
                    originalExtension = current.extension,
                    existingNamesInFolder = siblingNames,
                )
                val validName = when (validation) {
                    is FileNameValidationResult.Invalid -> {
                        return@withContext Result.failure(IllegalArgumentException(validation.reason))
                    }
                    is FileNameValidationResult.Valid -> validation.finalFileName
                }

                val newUriString = performStorageRename(uriString, validName)
                val meta = documentMetaDao.getByUri(uriString)
                if (meta != null) {
                    if (newUriString != uriString) {
                        documentMetaDao.deleteByUri(uriString)
                    }
                    documentMetaDao.upsert(
                        meta.copy(
                            uriString = newUriString,
                            displayName = validName,
                            modifiedAtEpochMs = System.currentTimeMillis(),
                        ),
                    )
                }
                val updated = current.copy(
                    id = newUriString,
                    uriString = newUriString,
                    displayName = validName,
                    modifiedAtEpochMs = System.currentTimeMillis(),
                )
                scannedDocsFlow.value = scannedDocsFlow.value.map {
                    if (it.uriString == uriString) updated else it
                }
                Result.success(updated)
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    override suspend fun moveToTrash(uriString: String): Result<TrashItem> = withContext(ioDispatcher) {
        var cachedFile: File? = null
        try {
            val item = getDocumentMeta(uriString)
                ?: return@withContext Result.failure(IllegalArgumentException("Document not found"))
            val trashDir = File(context.filesDir, "trash_cache").apply { mkdirs() }
            val trashId = UUID.randomUUID().toString()
            val safeName = sanitizeSingleFileName(item.displayName)
            val destFile = File(trashDir, "${trashId}_$safeName")
            cachedFile = destFile

            copyUriToFile(uriString, destFile)
            deleteSourceOrThrow(uriString)

            val now = System.currentTimeMillis()
            val expiresAt = now + THIRTY_DAYS_MS
            val entity = TrashEntity(
                trashId = trashId,
                originalUriString = uriString,
                cachedFilePath = destFile.absolutePath,
                displayName = safeName,
                format = item.format.name,
                sizeBytes = destFile.length().coerceAtLeast(item.sizeBytes),
                trashedAtEpochMs = now,
                expiresAtEpochMs = expiresAt,
            )
            trashDao.upsert(entity)
            Result.success(
                TrashItem(
                    trashId = entity.trashId,
                    originalUriString = entity.originalUriString,
                    cachedFilePath = entity.cachedFilePath,
                    displayName = entity.displayName,
                    extension = item.extension,
                    format = item.format,
                    sizeBytes = entity.sizeBytes,
                    trashedAtEpochMs = now,
                    expiresAtEpochMs = expiresAt,
                ),
            )
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            cachedFile?.delete()
            Result.failure(t)
        }
    }

    override suspend fun restoreFromTrash(trashId: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val entity = trashDao.getById(trashId)
                ?: return@withContext Result.failure(IllegalArgumentException("Trash entry not found"))
            val cached = File(entity.cachedFilePath)
            if (cached.exists()) {
                val restoredUri = restoreFileToOriginalOrFallback(cached, entity.originalUriString, entity.displayName)
                val existingMeta = documentMetaDao.getByUri(entity.originalUriString)
                if (existingMeta != null && restoredUri != entity.originalUriString) {
                    documentMetaDao.deleteByUri(entity.originalUriString)
                    documentMetaDao.upsert(existingMeta.copy(uriString = restoredUri))
                }
                cached.delete()
            }
            trashDao.deleteById(trashId)
            refreshMediaStoreIndex()
            Result.success(Unit)
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun deleteForeverFromTrash(trashId: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val entity = trashDao.getById(trashId)
            if (entity != null) {
                val file = File(entity.cachedFilePath)
                if (file.exists() && !file.delete()) {
                    return@withContext Result.failure(IllegalStateException("Unable to delete cached trash file"))
                }
                documentMetaDao.deleteByUri(entity.originalUriString)
                trashDao.deleteById(trashId)
            }
            Result.success(Unit)
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun emptyTrash(): Result<Int> = withContext(ioDispatcher) {
        try {
            val all = trashDao.getAllSnapshot()
            var removed = 0
            all.forEach { entity ->
                val file = File(entity.cachedFilePath)
                if (!file.exists() || file.delete()) {
                    documentMetaDao.deleteByUri(entity.originalUriString)
                    trashDao.deleteById(entity.trashId)
                    removed++
                }
            }
            Result.success(removed)
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun copyDocumentToFolder(uriString: String, targetFolderName: String): Result<DocumentItem> =
        withContext(ioDispatcher) {
            try {
                val item = getDocumentMeta(uriString)
                    ?: return@withContext Result.failure(IllegalArgumentException("Document not found"))
                val docsRoot = File(context.filesDir, "documents").apply { mkdirs() }
                val safeFolder = sanitizeSingleFileName(targetFolderName).ifBlank { "Documents" }
                val targetDir = File(docsRoot, safeFolder).canonicalFile
                require(targetDir.path.startsWith(docsRoot.canonicalPath + File.separator)) {
                    "Invalid target folder path"
                }
                targetDir.mkdirs()

                val safeName = sanitizeSingleFileName(item.displayName)
                val dest = uniqueFile(targetDir, safeName)
                copyUriToFile(uriString, dest)
                val destUri = dest.toURI().toString()
                val copied = item.copy(
                    id = destUri,
                    uriString = destUri,
                    displayName = dest.name,
                    parentFolderName = safeFolder,
                    modifiedAtEpochMs = System.currentTimeMillis(),
                )
                documentMetaDao.upsert(copied.toEntity())
                refreshMediaStoreIndex()
                Result.success(copied)
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    override suspend fun moveDocumentToFolder(uriString: String, targetFolderName: String): Result<DocumentItem> =
        withContext(ioDispatcher) {
            val copied = copyDocumentToFolder(uriString, targetFolderName)
            val copiedItem = copied.getOrNull() ?: return@withContext copied
            try {
                deleteSourceOrThrow(uriString)
                documentMetaDao.deleteByUri(uriString)
                refreshMediaStoreIndex()
                Result.success(copiedItem)
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                runCatching { Uri.parse(copiedItem.uriString).path?.let { File(it).delete() } }
                Result.failure(t)
            }
        }

    override suspend fun importExternalUri(
        uriString: String,
        displayNameHint: String?,
        mimeTypeHint: String?,
    ): Result<ExternalIntentPayload> = withContext(ioDispatcher) {
        try {
            val uri = Uri.parse(uriString)
            val resolvedName = displayNameHint?.takeIf { it.isNotBlank() }
                ?: queryDisplayName(uri)
                ?: uri.lastPathSegment?.substringAfterLast('/')
                ?: "document"

            val importDir = File(context.cacheDir, "external_imports").apply { mkdirs() }
            pruneOldExternalImports(importDir)
            val tempFile = File(importDir, "import_${System.nanoTime()}.tmp")

            val inputStream = if (uri.scheme == "file" && uri.path != null) {
                FileInputStream(File(uri.path!!))
            } else {
                context.contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure(IllegalStateException("Cannot open URI stream"))
            }

            val headerCapture = ByteArray(64 * 1024)
            var headerLen = 0
            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        if (headerLen < headerCapture.size) {
                            val toCopy = minOf(read, headerCapture.size - headerLen)
                            System.arraycopy(buffer, 0, headerCapture, headerLen, toCopy)
                            headerLen += toCopy
                        }
                    }
                }
            }

            val headerBytes = headerCapture.copyOf(headerLen)
            var sniffed = MagicByteSniffer.sniff(headerBytes, resolvedName, mimeTypeHint)
            if (headerBytes.size >= 4 && headerBytes[0] == 0x50.toByte() && headerBytes[1] == 0x4B.toByte()) {
                val deepZip = FileInputStream(tempFile).use { MagicByteSniffer.inspectZipFileStream(it) }
                if (deepZip != null) {
                    sniffed = sniffed.copy(format = deepZip.first, extension = deepZip.second)
                }
            }

            val safeBase = sanitizeSingleFileName(sniffed.baseName).ifBlank { "document" }
            val finalName = "$safeBase.${sniffed.extension}"
            val finalFile = uniqueFile(importDir, finalName)
            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            val cachedUri = finalFile.toURI().toString()
            documentMetaDao.upsert(
                DocumentMetaEntity(
                    uriString = cachedUri,
                    displayName = finalName,
                    format = sniffed.format.name,
                    sizeBytes = finalFile.length(),
                    modifiedAtEpochMs = System.currentTimeMillis(),
                    lastOpenedAtEpochMs = System.currentTimeMillis(),
                ),
            )
            preferencesRepository.incrementExternalOpenCount()

            Result.success(
                ExternalIntentPayload(
                    uriString = cachedUri,
                    displayName = finalName,
                    format = sniffed.format,
                    resolvedExtension = sniffed.extension,
                    isExternalView = true,
                ),
            )
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun registerSafUri(uriString: String): Result<DocumentItem> = withContext(ioDispatcher) {
        try {
            val uri = Uri.parse(uriString)
            val name = queryDisplayName(uri) ?: uri.lastPathSegment?.substringAfterLast('/') ?: "document.pdf"
            val ext = name.substringAfterLast('.', "").lowercase()
            val format = DocFormat.fromExtension(ext) ?: DocFormat.PDF
            val size = querySizeBytes(uri)
            val now = System.currentTimeMillis()
            val item = DocumentItem(
                id = uriString,
                uriString = uriString,
                displayName = name,
                extension = ext,
                format = format,
                sizeBytes = size,
                modifiedAtEpochMs = now,
                parentFolderName = "Folder Picker (SAF)",
            )
            documentMetaDao.upsert(item.toEntity())
            Result.success(item)
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun performStorageRename(uriString: String, newFileName: String): String {
        val parsed = Uri.parse(uriString)
        if (parsed.scheme == "file" && parsed.path != null) {
            val oldFile = File(parsed.path!!)
            val targetFile = File(oldFile.parentFile, newFileName)
            if (oldFile.exists() && oldFile.renameTo(targetFile)) {
                return targetFile.toURI().toString()
            }
            error("Unable to rename file on disk")
        }
        if (parsed.scheme == "content") {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, newFileName)
            }
            val updated = context.contentResolver.update(parsed, values, null, null)
            if (updated > 0) return uriString
            error("System denied renaming this shared file; grant All-Files access or use Copy.")
        }
        error("Unsupported URI scheme for rename")
    }

    private fun copyUriToFile(uriString: String, dest: File) {
        val uri = Uri.parse(uriString)
        val input = if (uri.scheme == "file" && uri.path != null) {
            FileInputStream(File(uri.path!!))
        } else {
            context.contentResolver.openInputStream(uri)
                ?: error("Cannot read URI $uriString")
        }
        input.use { src ->
            FileOutputStream(dest).use { dst ->
                src.copyTo(dst)
            }
        }
    }

    private fun deleteSourceOrThrow(uriString: String) {
        val uri = Uri.parse(uriString)
        if (uri.scheme == "file" && uri.path != null) {
            val f = File(uri.path!!)
            if (f.exists() && !f.delete()) {
                error("Unable to delete original file: ${f.name}")
            }
            return
        }
        val deleted = context.contentResolver.delete(uri, null, null)
        if (deleted <= 0) {
            error("System denied deleting this shared file; grant All-Files access to delete.")
        }
    }

    private fun restoreFileToOriginalOrFallback(
        cached: File,
        originalUriString: String,
        displayName: String,
    ): String {
        val uri = Uri.parse(originalUriString)
        if (uri.scheme == "file" && uri.path != null) {
            val orig = File(uri.path!!)
            orig.parentFile?.mkdirs()
            val target = if (orig.exists()) uniqueFile(orig.parentFile!!, displayName) else orig
            cached.copyTo(target, overwrite = true)
            return target.toURI().toString()
        }
        val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: File(context.filesDir, "documents")
        fallbackDir.mkdirs()
        val restored = uniqueFile(fallbackDir, sanitizeSingleFileName(displayName))
        cached.copyTo(restored, overwrite = true)
        return restored.toURI().toString()
    }

    private fun pruneOldExternalImports(importDir: File) {
        val cutoff = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        importDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        if (uri.scheme == "file") return uri.path?.let { File(it).name }
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
        }.getOrNull()
    }

    private fun querySizeBytes(uri: Uri): Long {
        if (uri.scheme == "file") return uri.path?.let { File(it).length() } ?: 0L
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
                ?.use { c ->
                    if (c.moveToFirst()) c.getLong(0) else 0L
                }
        }.getOrNull() ?: 0L
    }

    private fun resolveDirectFileItem(uriString: String): DocumentItem? {
        val parsed = Uri.parse(uriString)
        if (parsed.scheme != "file" || parsed.path == null) return null
        val file = File(parsed.path!!)
        if (!file.exists()) return null
        val ext = file.extension.lowercase()
        val format = DocFormat.fromExtension(ext) ?: DocFormat.TEXT
        return DocumentItem(
            id = uriString,
            uriString = uriString,
            displayName = file.name,
            extension = ext,
            format = format,
            sizeBytes = file.length(),
            modifiedAtEpochMs = file.lastModified(),
            parentFolderName = file.parentFile?.name ?: "Device",
        )
    }

    private fun sanitizeSingleFileName(raw: String): String =
        raw.substringAfterLast('/').substringAfterLast('\\')
            .replace("..", "_")
            .replace(Regex("[/\\\\:*?\"<>|\\x00-\\x1F]"), "_")
            .trim()
            .ifBlank { "document" }

    private fun uniqueFile(dir: File, fileName: String): File {
        var candidate = File(dir, fileName)
        if (!candidate.exists()) return candidate
        val dot = fileName.lastIndexOf('.')
        val base = if (dot > 0) fileName.substring(0, dot) else fileName
        val ext = if (dot > 0) fileName.substring(dot) else ""
        var index = 1
        while (candidate.exists()) {
            candidate = File(dir, "${base}_$index$ext")
            index++
        }
        return candidate
    }

    private fun DocumentItem.mergeWith(
        meta: DocumentMetaEntity?,
        hasPendingDraft: Boolean,
    ): DocumentItem = if (meta == null) {
        copy(hasPendingDraft = hasPendingDraft)
    } else {
        copy(
            displayName = meta.displayName,
            isFavorite = meta.isFavorite,
            lastOpenedAtEpochMs = meta.lastOpenedAtEpochMs,
            lastPageIndex = meta.lastPageIndex,
            lastTotalPages = meta.lastTotalPages,
            lastScrollOffsetY = meta.lastScrollOffsetY,
            lastZoom = meta.lastZoom,
            hasPendingDraft = hasPendingDraft,
        )
    }

    private fun DocumentMetaEntity.toDocumentItem(hasPendingDraft: Boolean): DocumentItem {
        val ext = displayName.substringAfterLast('.', "").lowercase()
        val fmt = runCatching { DocFormat.valueOf(format) }.getOrElse {
            DocFormat.fromExtension(ext) ?: DocFormat.TEXT
        }
        return DocumentItem(
            id = uriString,
            uriString = uriString,
            displayName = displayName,
            extension = ext,
            format = fmt,
            sizeBytes = sizeBytes,
            modifiedAtEpochMs = modifiedAtEpochMs,
            parentFolderName = deriveFolderName(uriString),
            isFavorite = isFavorite,
            lastOpenedAtEpochMs = lastOpenedAtEpochMs,
            lastPageIndex = lastPageIndex,
            lastTotalPages = lastTotalPages,
            lastScrollOffsetY = lastScrollOffsetY,
            lastZoom = lastZoom,
            hasPendingDraft = hasPendingDraft,
        )
    }

    private fun DocumentItem.toEntity(): DocumentMetaEntity = DocumentMetaEntity(
        uriString = uriString,
        displayName = displayName,
        format = format.name,
        sizeBytes = sizeBytes,
        modifiedAtEpochMs = modifiedAtEpochMs,
        lastOpenedAtEpochMs = lastOpenedAtEpochMs,
        lastPageIndex = lastPageIndex,
        lastTotalPages = lastTotalPages,
        lastScrollOffsetY = lastScrollOffsetY,
        lastZoom = lastZoom,
        isFavorite = isFavorite,
    )

    private fun deriveFolderName(uriString: String): String {
        val parsed = Uri.parse(uriString)
        if (parsed.scheme == "file" && parsed.path != null) {
            return File(parsed.path!!).parentFile?.name ?: "Device"
        }
        return "Device"
    }

    companion object {
        private const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000
    }
}
