package com.evora.technologies.office.core.data.repository

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.evora.technologies.office.core.data.di.IoDispatcher
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface LocalDocumentScanner {
    suspend fun scanDeviceDocuments(): List<DocumentItem>
}

@Singleton
class MediaStoreDocumentScanner @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LocalDocumentScanner {

    override suspend fun scanDeviceDocuments(): List<DocumentItem> = withContext(ioDispatcher) {
        val results = LinkedHashMap<String, DocumentItem>()
        val supportedExts = DocFormat.allSupportedExtensions

        queryMediaStore(supportedExts, results)
        scanAppAndPublicFallbackDirs(supportedExts, results)

        results.values.sortedByDescending { it.modifiedAtEpochMs }
    }

    private fun queryMediaStore(
        supportedExts: Set<String>,
        sink: MutableMap<String, DocumentItem>,
    ) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = buildList {
            add(MediaStore.Files.FileColumns._ID)
            add(MediaStore.Files.FileColumns.DISPLAY_NAME)
            add(MediaStore.Files.FileColumns.SIZE)
            add(MediaStore.Files.FileColumns.DATE_MODIFIED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Files.FileColumns.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                add(MediaStore.Files.FileColumns.DATA)
            }
        }.toTypedArray()

        // Filter out images/videos/audio at the SQLite layer to stay well under the <800ms / 1,000 docs target.
        // Android 11+ (API 30+) classifies PDF, Office, ODF, EPUB, and TXT files under MEDIA_TYPE_DOCUMENT (6),
        // whereas HWP/HWPX and archive formats remain MEDIA_TYPE_NONE (0).
        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?) AND ${MediaStore.Files.FileColumns.SIZE} > 0"
        } else {
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? AND ${MediaStore.Files.FileColumns.SIZE} > 0"
        }
        val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_NONE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_DOCUMENT.toString(),
            )
        } else {
            arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_NONE.toString())
        }
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        runCatching {
            context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
                val nameIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                val modIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val relIdx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Files.FileColumns.RELATIVE_PATH)
                } else {
                    @Suppress("DEPRECATION")
                    cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                }

                while (cursor.moveToNext()) {
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext !in supportedExts) continue
                    val format = DocFormat.fromExtension(ext) ?: continue
                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else continue
                    val size = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 0L
                    val modifiedSec = if (modIdx >= 0) cursor.getLong(modIdx) else 0L
                    val rawPath = if (relIdx >= 0) cursor.getString(relIdx).orEmpty() else ""
                    val folderName = extractFolderName(rawPath, isRelativePath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    val contentUri = ContentUris.withAppendedId(collection, id).toString()

                    sink[contentUri] = DocumentItem(
                        id = contentUri,
                        uriString = contentUri,
                        displayName = name,
                        extension = ext,
                        format = format,
                        sizeBytes = size.coerceAtLeast(0L),
                        modifiedAtEpochMs = (modifiedSec * 1000L).coerceAtLeast(0L),
                        parentFolderName = folderName,
                    )
                }
            }
        }
    }

    private fun scanAppAndPublicFallbackDirs(
        supportedExts: Set<String>,
        sink: MutableMap<String, DocumentItem>,
    ) {
        val candidateRoots = buildList {
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.let(::add)
            add(File(context.filesDir, "documents"))
        }
        candidateRoots.filter { it.exists() && it.isDirectory }.forEach { root ->
            root.walkTopDown().maxDepth(3).filter { it.isFile }.forEach { file ->
                val ext = file.extension.lowercase()
                if (ext !in supportedExts) return@forEach
                val format = DocFormat.fromExtension(ext) ?: return@forEach
                val uri = file.toURI().toString()
                if (uri !in sink) {
                    sink[uri] = DocumentItem(
                        id = uri,
                        uriString = uri,
                        displayName = file.name,
                        extension = ext,
                        format = format,
                        sizeBytes = file.length(),
                        modifiedAtEpochMs = file.lastModified(),
                        parentFolderName = file.parentFile?.name ?: "Documents",
                    )
                }
            }
        }
    }

    private fun extractFolderName(rawPath: String, isRelativePath: Boolean): String {
        val trimmed = rawPath.trimEnd('/')
        if (trimmed.isEmpty()) return "Device"
        if (!isRelativePath) {
            return File(trimmed).parentFile?.name ?: "Device"
        }
        return trimmed.substringAfterLast('/').ifBlank { "Device" }
    }
}
