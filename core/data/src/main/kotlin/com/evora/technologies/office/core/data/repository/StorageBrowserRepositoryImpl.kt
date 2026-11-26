package com.evora.technologies.office.core.data.repository

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import androidx.documentfile.provider.DocumentFile
import com.evora.technologies.office.core.data.di.IoDispatcher
import com.evora.technologies.office.core.database.dao.DocumentMetaDao
import com.evora.technologies.office.core.database.entity.DocumentMetaEntity
import com.evora.technologies.office.core.datastore.EvoraPreferencesDataSource
import com.evora.technologies.office.core.domain.port.DocumentRepository
import com.evora.technologies.office.core.domain.port.StorageBrowserRepository
import com.evora.technologies.office.core.domain.util.FileNameValidationResult
import com.evora.technologies.office.core.domain.util.FileNameValidator
import com.evora.technologies.office.core.model.BatchOperationReport
import com.evora.technologies.office.core.model.BreadcrumbSegment
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.StorageDirectoryListing
import com.evora.technologies.office.core.model.StorageEntry
import com.evora.technologies.office.core.model.StorageRootKind
import com.evora.technologies.office.core.model.StorageVolumeRoot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageBrowserRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesDataSource: EvoraPreferencesDataSource,
    private val documentMetaDao: DocumentMetaDao,
    private val documentRepository: DocumentRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : StorageBrowserRepository {

    override suspend fun getAvailableRoots(): List<StorageVolumeRoot> = withContext(ioDispatcher) {
        val roots = mutableListOf<StorageVolumeRoot>()

        val internalDir = resolveInternalRoot()
        val internalStat = safeStatFs(internalDir)
        roots += StorageVolumeRoot(
            id = "root_internal",
            kind = StorageRootKind.INTERNAL,
            title = "Internal",
            pathOrUri = internalDir.absolutePath,
            freeBytes = internalStat.first,
            totalBytes = internalStat.second,
            isWritable = internalDir.canWrite(),
        )

        val downloadsDir = resolveDownloadsRoot()
        val dlStat = safeStatFs(downloadsDir)
        roots += StorageVolumeRoot(
            id = "root_downloads",
            kind = StorageRootKind.DOWNLOADS,
            title = "Downloads",
            pathOrUri = downloadsDir.absolutePath,
            freeBytes = dlStat.first,
            totalBytes = dlStat.second,
            isWritable = downloadsDir.canWrite(),
        )

        detectSdCardRoot(internalDir)?.let { sdDir ->
            val sdStat = safeStatFs(sdDir)
            roots += StorageVolumeRoot(
                id = "root_sd_card",
                kind = StorageRootKind.SD_CARD,
                title = "SD Card",
                pathOrUri = sdDir.absolutePath,
                freeBytes = sdStat.first,
                totalBytes = sdStat.second,
                isWritable = sdDir.canWrite(),
            )
        }

        val safUris = preferencesDataSource.safTreeUris.first()
        safUris.sorted().forEach { uriStr ->
            val docTree = runCatching { DocumentFile.fromTreeUri(context, Uri.parse(uriStr)) }.getOrNull()
            if (docTree != null && docTree.canRead()) {
                roots += StorageVolumeRoot(
                    id = "saf_${uriStr.hashCode()}",
                    kind = StorageRootKind.SAF_TREE,
                    title = docTree.name?.takeIf { it.isNotBlank() } ?: "SAF Folder",
                    pathOrUri = uriStr,
                    isWritable = docTree.canWrite(),
                )
            }
        }
        roots
    }

    override suspend fun listDirectory(
        root: StorageVolumeRoot,
        pathOrUri: String?,
    ): Result<StorageDirectoryListing> = withContext(ioDispatcher) {
        try {
            if (root.kind == StorageRootKind.SAF_TREE) {
                listSafDirectory(root, pathOrUri ?: root.pathOrUri)
            } else {
                listLocalDirectory(root, pathOrUri ?: root.pathOrUri)
            }
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun createFolder(
        root: StorageVolumeRoot,
        parentPathOrUri: String,
        folderName: String,
    ): Result<StorageEntry> = withContext(ioDispatcher) {
        try {
            val existingListing = listDirectory(root, parentPathOrUri).getOrThrow()
            val existingNames = existingListing.entries.mapTo(HashSet()) { it.name }
            val validation = FileNameValidator.validateAndPreserveExtension(
                rawInput = folderName,
                originalExtension = "",
                existingNamesInFolder = existingNames,
            )
            val validName = when (validation) {
                is FileNameValidationResult.Invalid -> {
                    return@withContext Result.failure(IllegalArgumentException(validation.reason))
                }
                is FileNameValidationResult.Valid -> validation.finalFileName
            }

            if (root.kind == StorageRootKind.SAF_TREE) {
                val parentDoc = DocumentFile.fromTreeUri(context, Uri.parse(parentPathOrUri))
                    ?: DocumentFile.fromSingleUri(context, Uri.parse(parentPathOrUri))
                    ?: return@withContext Result.failure(IllegalStateException("Unable to access SAF directory"))
                val created = parentDoc.createDirectory(validName)
                    ?: return@withContext Result.failure(IllegalStateException("SAF folder creation denied"))
                val createdUri = created.uri.toString()
                Result.success(
                    StorageEntry(
                        id = createdUri,
                        uriString = createdUri,
                        pathOrUri = createdUri,
                        name = created.name ?: validName,
                        isDirectory = true,
                        sizeBytes = 0L,
                        modifiedAtEpochMs = created.lastModified(),
                        isSafEntry = true,
                    ),
                )
            } else {
                val parentDir = validateDirectoryInsideRoot(root.pathOrUri, parentPathOrUri)
                val targetDir = File(parentDir, validName).canonicalFile
                require(targetDir.path.startsWith(parentDir.canonicalPath + File.separator)) {
                    "Blocked unsafe folder path"
                }
                if (!targetDir.mkdirs() && !targetDir.isDirectory) {
                    return@withContext Result.failure(IllegalStateException("Unable to create folder (grant storage permission or use SAF)"))
                }
                val uri = targetDir.toURI().toString()
                Result.success(
                    StorageEntry(
                        id = uri,
                        uriString = uri,
                        pathOrUri = targetDir.absolutePath,
                        name = targetDir.name,
                        isDirectory = true,
                        sizeBytes = 0L,
                        modifiedAtEpochMs = targetDir.lastModified(),
                    ),
                )
            }
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun batchCopy(
        entries: List<StorageEntry>,
        targetFolderPathOrUri: String,
    ): Result<BatchOperationReport> = withContext(ioDispatcher) {
        executeBatchCopyOrMove(entries, targetFolderPathOrUri, deleteSource = false)
    }

    override suspend fun batchMove(
        entries: List<StorageEntry>,
        targetFolderPathOrUri: String,
    ): Result<BatchOperationReport> = withContext(ioDispatcher) {
        executeBatchCopyOrMove(entries, targetFolderPathOrUri, deleteSource = true)
    }

    override suspend fun batchMoveToTrash(
        entries: List<StorageEntry>,
    ): Result<BatchOperationReport> = withContext(ioDispatcher) {
        var succeeded = 0
        var failed = 0
        val deletedPaths = mutableListOf<String>()

        entries.forEach { entry ->
            try {
                if (entry.isDirectory) {
                    if (entry.isSafEntry) {
                        val doc = DocumentFile.fromTreeUri(context, Uri.parse(entry.uriString))
                            ?: DocumentFile.fromSingleUri(context, Uri.parse(entry.uriString))
                        if (doc != null && doc.isDirectory && trashSafDirectoryRecursively(doc) && doc.delete()) {
                            succeeded++
                        } else {
                            failed++
                        }
                    } else {
                        val dir = File(entry.pathOrUri)
                        if (dir.exists()) {
                            trashFilesRecursively(dir, deletedPaths)
                            if (dir.deleteRecursively()) succeeded++ else failed++
                        } else {
                            failed++
                        }
                    }
                } else {
                    val res = documentRepository.moveToTrash(entry.uriString)
                    if (res.isSuccess) {
                        if (!entry.isSafEntry) deletedPaths += entry.pathOrUri
                        succeeded++
                    } else if (!entry.isSafEntry && File(entry.pathOrUri).delete()) {
                        deletedPaths += entry.pathOrUri
                        succeeded++
                    } else {
                        failed++
                    }
                }
            } catch (c: CancellationException) {
                throw c
            } catch (_: Throwable) {
                failed++
            }
        }

        if (deletedPaths.isNotEmpty()) {
            scanMediaPaths(deletedPaths)
        }
        documentRepository.refreshMediaStoreIndex()
        Result.success(
            BatchOperationReport(
                actionName = "Trash",
                succeededCount = succeeded,
                failedCount = failed,
                message = if (failed == 0) {
                    "Moved $succeeded item(s) to Trash"
                } else {
                    "Trashed $succeeded item(s); $failed failed"
                },
            ),
        )
    }

    override suspend fun registerSafTreeUri(
        treeUriString: String,
        displayLabel: String?,
    ): Result<StorageVolumeRoot> = withContext(ioDispatcher) {
        try {
            val tree = DocumentFile.fromTreeUri(context, Uri.parse(treeUriString))
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid SAF folder URI"))
            preferencesDataSource.addSafTreeUri(treeUriString)
            Result.success(
                StorageVolumeRoot(
                    id = "saf_${treeUriString.hashCode()}",
                    kind = StorageRootKind.SAF_TREE,
                    title = displayLabel?.takeIf { it.isNotBlank() } ?: tree.name ?: "SAF Folder",
                    pathOrUri = treeUriString,
                    isWritable = tree.canWrite(),
                ),
            )
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun removeSafTreeUri(treeUriString: String) = withContext(ioDispatcher) {
        preferencesDataSource.removeSafTreeUri(treeUriString)
    }

    override suspend fun materializeEntryForOpen(entry: StorageEntry): Result<DocumentItem> =
        withContext(ioDispatcher) {
            try {
                if (!entry.isDirectory && entry.format == null) {
                    return@withContext Result.failure(
                        UnsupportedOperationException("Unsupported file format: ${entry.name}"),
                    )
                }
                if (entry.isSafEntry) {
                    documentRepository.registerSafUri(entry.uriString)
                } else {
                    val file = File(entry.pathOrUri)
                    val uri = file.toURI().toString()
                    val existing = documentRepository.getDocumentMeta(uri)
                    if (existing != null) {
                        Result.success(existing)
                    } else {
                        val ext = file.extension.lowercase()
                        val fmt = entry.format ?: DocFormat.fromExtension(ext)
                            ?: return@withContext Result.failure(
                                UnsupportedOperationException("Unsupported file format: ${file.name}"),
                            )
                        Result.success(
                            DocumentItem(
                                id = uri,
                                uriString = uri,
                                displayName = file.name,
                                extension = ext,
                                format = fmt,
                                sizeBytes = file.length(),
                                modifiedAtEpochMs = file.lastModified(),
                                parentFolderName = file.parentFile?.name ?: "Storage",
                            ),
                        )
                    }
                }
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }

    private fun listLocalDirectory(
        root: StorageVolumeRoot,
        targetPath: String,
    ): Result<StorageDirectoryListing> {
        val rootDir = File(root.pathOrUri).canonicalFile
        val currentDir = validateDirectoryInsideRoot(root.pathOrUri, targetPath)
        if (!currentDir.exists() || !currentDir.isDirectory) {
            return Result.failure(IllegalStateException("Directory not accessible: ${currentDir.name}"))
        }
        val rawFiles = currentDir.listFiles()
            ?: return Result.failure(IllegalStateException("Permission required to list ${currentDir.name}. Use SAF Folder Picker."))

        val entries = rawFiles
            .filter { !it.name.startsWith(".") }
            .map { file ->
                val ext = if (file.isDirectory) "" else file.extension.lowercase()
                val fmt = if (file.isDirectory) null else DocFormat.fromExtension(ext)
                val uri = file.toURI().toString()
                val children = if (file.isDirectory) file.listFiles()?.count { !it.name.startsWith(".") } ?: 0 else 0
                StorageEntry(
                    id = uri,
                    uriString = uri,
                    pathOrUri = file.absolutePath,
                    name = file.name,
                    isDirectory = file.isDirectory,
                    sizeBytes = if (file.isDirectory) 0L else file.length(),
                    modifiedAtEpochMs = file.lastModified(),
                    childCount = children,
                    format = fmt,
                    extension = ext,
                    isSafEntry = false,
                )
            }
            .sortedWith(
                compareByDescending<StorageEntry> { it.isDirectory }
                    .thenBy { it.name.lowercase() },
            )

        val breadcrumbs = buildLocalBreadcrumbs(root, rootDir, currentDir)
        return Result.success(
            StorageDirectoryListing(
                root = root,
                currentPathOrUri = currentDir.absolutePath,
                breadcrumbs = breadcrumbs,
                entries = entries,
                isSafMode = false,
            ),
        )
    }

    private fun listSafDirectory(
        root: StorageVolumeRoot,
        targetUriStr: String,
    ): Result<StorageDirectoryListing> {
        val rootDoc = DocumentFile.fromTreeUri(context, Uri.parse(root.pathOrUri))
            ?: return Result.failure(IllegalStateException("SAF folder unavailable"))
        val currentDoc = if (targetUriStr == root.pathOrUri) {
            rootDoc
        } else {
            DocumentFile.fromTreeUri(context, Uri.parse(targetUriStr))
                ?: DocumentFile.fromSingleUri(context, Uri.parse(targetUriStr))
                ?: rootDoc
        }
        val children = currentDoc.listFiles()
        val entries = children
            .filter { !(it.name ?: "").startsWith(".") }
            .map { doc ->
                val name = doc.name ?: "item"
                val isDir = doc.isDirectory
                val ext = if (isDir) "" else name.substringAfterLast('.', "").lowercase()
                val fmt = if (isDir) null else DocFormat.fromExtension(ext)
                val uriStr = doc.uri.toString()
                StorageEntry(
                    id = uriStr,
                    uriString = uriStr,
                    pathOrUri = uriStr,
                    name = name,
                    isDirectory = isDir,
                    sizeBytes = if (isDir) 0L else doc.length(),
                    modifiedAtEpochMs = doc.lastModified(),
                    format = fmt,
                    extension = ext,
                    isSafEntry = true,
                )
            }
            .sortedWith(
                compareByDescending<StorageEntry> { it.isDirectory }
                    .thenBy { it.name.lowercase() },
            )

        val breadcrumbs = if (targetUriStr == root.pathOrUri) {
            listOf(BreadcrumbSegment(root.title, root.pathOrUri))
        } else {
            listOf(
                BreadcrumbSegment(root.title, root.pathOrUri),
                BreadcrumbSegment(currentDoc.name ?: "Folder", targetUriStr),
            )
        }
        return Result.success(
            StorageDirectoryListing(
                root = root,
                currentPathOrUri = targetUriStr,
                breadcrumbs = breadcrumbs,
                entries = entries,
                isSafMode = true,
            ),
        )
    }

    private suspend fun executeBatchCopyOrMove(
        entries: List<StorageEntry>,
        targetFolderPathOrUri: String,
        deleteSource: Boolean,
    ): Result<BatchOperationReport> {
        var succeeded = 0
        var failed = 0
        val touchedPaths = mutableListOf<String>()
        val isTargetSaf = targetFolderPathOrUri.startsWith("content://")

        for (entry in entries) {
            try {
                if (isTargetSaf) {
                    val destTree = DocumentFile.fromTreeUri(context, Uri.parse(targetFolderPathOrUri))
                        ?: error("Target SAF folder inaccessible")
                    if (entry.isDirectory) {
                        failed++
                        continue
                    }
                    val mime = context.contentResolver.getType(Uri.parse(entry.uriString)) ?: "application/octet-stream"
                    val created = destTree.createFile(mime, entry.name) ?: error("Cannot create target SAF file")
                    copyEntryToSaf(entry, created.uri)
                    if (deleteSource) {
                        deleteEntrySource(entry)
                    }
                    succeeded++
                } else {
                    val destDir = File(targetFolderPathOrUri).canonicalFile
                    if (!destDir.exists()) destDir.mkdirs()
                    require(destDir.isDirectory) { "Target must be a folder" }

                    if (entry.isDirectory) {
                        if (!entry.isSafEntry) {
                            val srcDir = File(entry.pathOrUri).canonicalFile
                            if (destDir.path == srcDir.path || destDir.path.startsWith(srcDir.path + File.separator)) {
                                failed++
                                continue
                            }
                            val targetSub = uniqueFile(destDir, srcDir.name)
                            val oldChildUris = if (deleteSource) {
                                srcDir.walkTopDown().filter { it.isFile }.map { it.toURI().toString() }.toList()
                            } else {
                                emptyList()
                            }
                            if (deleteSource && srcDir.renameTo(targetSub)) {
                                oldChildUris.forEach { documentMetaDao.deleteByUri(it) }
                                touchedPaths += targetSub.absolutePath
                                succeeded++
                            } else if (srcDir.copyRecursively(targetSub, overwrite = false)) {
                                touchedPaths += targetSub.absolutePath
                                if (deleteSource) {
                                    srcDir.deleteRecursively()
                                    oldChildUris.forEach { documentMetaDao.deleteByUri(it) }
                                }
                                succeeded++
                            } else {
                                failed++
                            }
                        } else {
                            failed++
                        }
                    } else {
                        val destFile = uniqueFile(destDir, entry.name)
                        if (deleteSource && !entry.isSafEntry && File(entry.pathOrUri).renameTo(destFile)) {
                            updateMovedFileMeta(entry, destFile)
                        } else {
                            copyEntryToLocalFile(entry, destFile)
                            updateCopiedFileMeta(entry, destFile)
                            if (deleteSource) {
                                deleteEntrySource(entry)
                                documentMetaDao.deleteByUri(entry.uriString)
                            }
                        }
                        touchedPaths += destFile.absolutePath
                        succeeded++
                    }
                }
            } catch (c: CancellationException) {
                throw c
            } catch (_: Throwable) {
                failed++
            }
        }

        if (touchedPaths.isNotEmpty()) {
            scanMediaPaths(touchedPaths)
        }
        documentRepository.refreshMediaStoreIndex()

        val verb = if (deleteSource) "Move" else "Copy"
        val past = if (deleteSource) "Moved" else "Copied"
        return Result.success(
            BatchOperationReport(
                actionName = verb,
                succeededCount = succeeded,
                failedCount = failed,
                message = if (failed == 0) {
                    "$past $succeeded item(s)"
                } else {
                    "$past $succeeded item(s); $failed failed"
                },
            ),
        )
    }

    private suspend fun trashFilesRecursively(dir: File, deletedPaths: MutableList<String>) {
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                trashFilesRecursively(child, deletedPaths)
            } else {
                val uri = child.toURI().toString()
                documentRepository.moveToTrash(uri)
                deletedPaths += child.absolutePath
            }
        }
    }

    private suspend fun trashSafDirectoryRecursively(dir: DocumentFile): Boolean {
        var allOk = true
        dir.listFiles().forEach { child ->
            if (child.isDirectory) {
                if (!trashSafDirectoryRecursively(child) || !child.delete()) {
                    allOk = false
                }
            } else {
                val res = documentRepository.moveToTrash(child.uri.toString())
                if (res.isFailure) {
                    allOk = false
                }
            }
        }
        return allOk
    }

    private suspend fun updateMovedFileMeta(oldEntry: StorageEntry, newFile: File) {
        val oldMeta = documentMetaDao.getByUri(oldEntry.uriString)
        val newUri = newFile.toURI().toString()
        if (oldMeta != null) {
            documentMetaDao.deleteByUri(oldEntry.uriString)
            documentMetaDao.upsert(
                oldMeta.copy(
                    uriString = newUri,
                    displayName = newFile.name,
                    modifiedAtEpochMs = System.currentTimeMillis(),
                ),
            )
        }
    }

    private suspend fun updateCopiedFileMeta(entry: StorageEntry, destFile: File) {
        val ext = destFile.extension.lowercase()
        val fmt = DocFormat.fromExtension(ext) ?: return
        val newUri = destFile.toURI().toString()
        documentMetaDao.upsert(
            DocumentMetaEntity(
                uriString = newUri,
                displayName = destFile.name,
                format = fmt.name,
                sizeBytes = destFile.length(),
                modifiedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    private fun copyEntryToLocalFile(entry: StorageEntry, dest: File) {
        val input = if (!entry.isSafEntry) {
            FileInputStream(File(entry.pathOrUri))
        } else {
            context.contentResolver.openInputStream(Uri.parse(entry.uriString))
                ?: error("Cannot read SAF source ${entry.name}")
        }
        input.use { src ->
            FileOutputStream(dest).use { dst ->
                src.copyTo(dst)
            }
        }
    }

    private fun copyEntryToSaf(entry: StorageEntry, destUri: Uri) {
        val input = if (!entry.isSafEntry) {
            FileInputStream(File(entry.pathOrUri))
        } else {
            context.contentResolver.openInputStream(Uri.parse(entry.uriString))
                ?: error("Cannot read source ${entry.name}")
        }
        val output = context.contentResolver.openOutputStream(destUri)
            ?: error("Cannot write destination ${entry.name}")
        input.use { src -> output.use { dst -> src.copyTo(dst) } }
    }

    private fun deleteEntrySource(entry: StorageEntry) {
        if (!entry.isSafEntry) {
            val file = File(entry.pathOrUri)
            if (file.exists() && !file.delete()) error("Unable to delete ${entry.name}")
        } else {
            val doc = DocumentFile.fromSingleUri(context, Uri.parse(entry.uriString))
            if (doc != null && !doc.delete()) error("Unable to delete SAF ${entry.name}")
        }
    }

    private fun validateDirectoryInsideRoot(rootPath: String, requestedPath: String): File {
        val rootDir = File(rootPath).canonicalFile
        val targetDir = File(requestedPath).canonicalFile
        val allowed = targetDir.path == rootDir.path ||
            targetDir.path.startsWith(rootDir.path + File.separator)
        return if (allowed) targetDir else rootDir
    }

    private fun buildLocalBreadcrumbs(
        root: StorageVolumeRoot,
        rootDir: File,
        currentDir: File,
    ): List<BreadcrumbSegment> {
        val segments = mutableListOf(BreadcrumbSegment(root.title, rootDir.absolutePath))
        if (currentDir.path == rootDir.path) return segments
        val rel = currentDir.path.removePrefix(rootDir.path).trimStart(File.separatorChar)
        if (rel.isBlank()) return segments
        var acc = rootDir
        rel.split(File.separatorChar).forEach { part ->
            if (part.isNotBlank()) {
                acc = File(acc, part)
                segments += BreadcrumbSegment(part, acc.absolutePath)
            }
        }
        return segments
    }

    private fun resolveInternalRoot(): File {
        val extRoot = runCatching { Environment.getExternalStorageDirectory() }.getOrNull()
        if (extRoot != null && extRoot.exists() && extRoot.canRead() && extRoot.listFiles() != null) {
            return extRoot
        }
        return (context.getExternalFilesDir(null) ?: File(context.filesDir, "documents")).apply { mkdirs() }
    }

    private fun resolveDownloadsRoot(): File {
        val dl = runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) }.getOrNull()
        if (dl != null && dl.exists() && dl.canRead() && dl.listFiles() != null) {
            return dl
        }
        return (context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "downloads")).apply { mkdirs() }
    }

    private fun detectSdCardRoot(internalRoot: File): File? {
        val candidates = runCatching { context.getExternalFilesDirs(null).drop(1) }.getOrDefault(emptyList())
        return candidates.firstOrNull { dir ->
            dir != null &&
                dir.exists() &&
                dir.canRead() &&
                dir.absolutePath != internalRoot.absolutePath
        }
    }

    private fun safeStatFs(dir: File): Pair<Long, Long> = runCatching {
        val stat = StatFs(dir.absolutePath)
        stat.availableBytes to stat.totalBytes
    }.getOrDefault(0L to 0L)

    private fun scanMediaPaths(paths: List<String>) {
        runCatching {
            MediaScannerConnection.scanFile(context, paths.toTypedArray(), null, null)
        }
    }

    private fun uniqueFile(dir: File, fileName: String): File {
        var candidate = File(dir, fileName)
        if (!candidate.exists()) return candidate
        val dot = fileName.lastIndexOf('.')
        val base = if (dot > 0) fileName.substring(0, dot) else fileName
        val ext = if (dot > 0) fileName.substring(dot) else ""
        var idx = 1
        while (candidate.exists()) {
            candidate = File(dir, "${base}_$idx$ext")
            idx++
        }
        return candidate
    }
}
