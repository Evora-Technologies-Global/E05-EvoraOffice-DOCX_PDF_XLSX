package com.evora.technologies.office.engine.archive

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.evora.technologies.office.core.data.di.IoDispatcher
import com.evora.technologies.office.core.domain.port.ArchiveEnginePort
import com.evora.technologies.office.core.domain.port.ArchiveInspectionResult
import com.evora.technologies.office.core.domain.port.DocumentInvalidPasswordException
import com.evora.technologies.office.core.domain.port.DocumentPasswordRequiredException
import com.evora.technologies.office.core.model.ArchiveEntryItem
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.apache.commons.compress.PasswordRequiredException
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure-Java ZIP/7z inspector and extractor powered by Apache Commons Compress + XZ (T-009 / US-024).
 * Enforces canonical Zip-Slip path traversal checks, extraction caps, and honest unsupported messaging for .rar.
 */
@Singleton
class CommonsCompressArchiveEngine @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ArchiveEnginePort {

    override suspend fun listEntries(
        uriString: String,
        password: String?,
    ): ArchiveInspectionResult = withContext(ioDispatcher) {
        val (localFile, ownedTemp, archiveName) = materializeFile(uriString)
        try {
            val ext = archiveName.substringAfterLast('.', "").lowercase()
            if (ext == "rar") {
                return@withContext ArchiveInspectionResult(
                    archiveName = archiveName,
                    extension = "rar",
                    isSupported = false,
                    entries = emptyList(),
                    unsupportedReason = "Evora Office supports .zip and .7z offline archives. Proprietary .rar archives are not supported.",
                )
            }

            val entries = when (ext) {
                "7z" -> listSevenZEntries(localFile, password)
                else -> listZipEntries(localFile)
            }
            ArchiveInspectionResult(
                archiveName = archiveName,
                extension = ext.ifBlank { "zip" },
                isSupported = true,
                entries = entries,
            )
        } finally {
            if (ownedTemp) localFile.delete()
        }
    }

    override suspend fun extractEntries(
        uriString: String,
        destinationDirPath: String,
        entryPaths: Set<String>?,
        password: String?,
    ): Result<List<String>> = withContext(ioDispatcher) {
        val (localFile, ownedTemp, archiveName) = materializeFile(uriString)
        try {
            val ext = archiveName.substringAfterLast('.', "").lowercase()
            if (ext == "rar") {
                return@withContext Result.failure(
                    UnsupportedOperationException("RAR extraction is not supported; Evora Office supports .zip and .7z."),
                )
            }
            val destDir = File(destinationDirPath).apply { mkdirs() }
            val extracted = when (ext) {
                "7z" -> extractSevenZ(localFile, destDir, entryPaths, password)
                else -> extractZip(localFile, destDir, entryPaths)
            }
            Result.success(extracted)
        } catch (c: CancellationException) {
            throw c
        } catch (p: DocumentPasswordRequiredException) {
            Result.failure(p)
        } catch (p: DocumentInvalidPasswordException) {
            Result.failure(p)
        } catch (t: Throwable) {
            Result.failure(t)
        } finally {
            if (ownedTemp) localFile.delete()
        }
    }

    private fun listZipEntries(file: File): List<ArchiveEntryItem> {
        ZipFile.builder().setFile(file).get().use { zipFile ->
            val list = mutableListOf<ArchiveEntryItem>()
            val entries = zipFile.entries
            while (entries.hasMoreElements() && list.size < MAX_ENTRIES) {
                val entry = entries.nextElement()
                val name = entry.name.trimEnd('/').substringAfterLast('/')
                list += ArchiveEntryItem(
                    path = entry.name,
                    name = name.ifBlank { entry.name },
                    sizeBytes = entry.size.coerceAtLeast(0L),
                    compressedSizeBytes = entry.compressedSize.coerceAtLeast(0L),
                    isDirectory = entry.isDirectory,
                    isEncrypted = entry.generalPurposeBit.usesEncryption(),
                    modifiedEpochMs = entry.time,
                )
            }
            return list
        }
    }

    private fun listSevenZEntries(file: File, password: String?): List<ArchiveEntryItem> {
        return try {
            val builder = SevenZFile.builder().setFile(file)
            if (password != null) builder.setPassword(password.toCharArray())
            builder.get().use { sevenZ ->
                sevenZ.entries.take(MAX_ENTRIES).map { entry ->
                    val name = entry.name.trimEnd('/').substringAfterLast('/')
                    ArchiveEntryItem(
                        path = entry.name,
                        name = name.ifBlank { entry.name },
                        sizeBytes = entry.size.coerceAtLeast(0L),
                        compressedSizeBytes = entry.size.coerceAtLeast(0L),
                        isDirectory = entry.isDirectory,
                        isEncrypted = password != null,
                        modifiedEpochMs = runCatching { entry.lastModifiedDate.time }.getOrDefault(0L),
                    )
                }
            }
        } catch (_: PasswordRequiredException) {
            throw DocumentPasswordRequiredException("Password required to inspect 7z archive")
        } catch (ex: Throwable) {
            val msg = ex.message.orEmpty()
            if (msg.contains("password", ignoreCase = true) || msg.contains("Corrupted", ignoreCase = true)) {
                if (password == null) throw DocumentPasswordRequiredException()
                throw DocumentInvalidPasswordException("Wrong password for 7z archive")
            }
            throw ex
        }
    }

    private fun extractZip(
        file: File,
        destDir: File,
        entryPaths: Set<String>?,
    ): List<String> {
        val extractedPaths = mutableListOf<String>()
        val canonicalRoot = destDir.canonicalFile
        var totalBytes = 0L
        var entryCount = 0
        ZipFile.builder().setFile(file).get().use { zipFile ->
            val entries = zipFile.entries
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entryPaths != null && entry.name !in entryPaths) continue
                entryCount++
                require(entryCount <= MAX_ENTRIES) { "Archive exceeds max entry limit ($MAX_ENTRIES)" }

                val target = resolveSafeEntryFile(canonicalRoot, entry.name)
                if (entry.isDirectory) {
                    target.mkdirs()
                    continue
                }
                if (entry.generalPurposeBit.usesEncryption()) {
                    throw UnsupportedOperationException(
                        "Encrypted .zip extraction is not supported offline; unencrypted .zip and password-protected .7z archives are supported.",
                    )
                }
                target.parentFile?.mkdirs()
                zipFile.getInputStream(entry).use { input ->
                    FileOutputStream(target).use { output ->
                        val buf = ByteArray(16 * 1024)
                        while (true) {
                            val read = input.read(buf)
                            if (read <= 0) break
                            totalBytes += read
                            require(totalBytes <= MAX_EXTRACTED_BYTES) {
                                "Archive exceeds 512 MB safety extraction limit"
                            }
                            output.write(buf, 0, read)
                        }
                    }
                }
                extractedPaths += target.absolutePath
            }
        }
        return extractedPaths
    }

    private fun extractSevenZ(
        file: File,
        destDir: File,
        entryPaths: Set<String>?,
        password: String?,
    ): List<String> {
        val extractedPaths = mutableListOf<String>()
        val canonicalRoot = destDir.canonicalFile
        var totalBytes = 0L
        var entryCount = 0
        try {
            val builder = SevenZFile.builder().setFile(file)
            if (password != null) builder.setPassword(password.toCharArray())
            builder.get().use { sevenZ ->
                var entry = sevenZ.nextEntry
                while (entry != null) {
                    if (entryPaths == null || entry.name in entryPaths) {
                        entryCount++
                        require(entryCount <= MAX_ENTRIES) { "Archive exceeds max entry limit ($MAX_ENTRIES)" }
                        val target = resolveSafeEntryFile(canonicalRoot, entry.name)
                        if (entry.isDirectory) {
                            target.mkdirs()
                        } else {
                            target.parentFile?.mkdirs()
                            FileOutputStream(target).use { out ->
                                val buf = ByteArray(16 * 1024)
                                while (true) {
                                    val read = sevenZ.read(buf)
                                    if (read <= 0) break
                                    totalBytes += read
                                    require(totalBytes <= MAX_EXTRACTED_BYTES) {
                                        "Archive exceeds 512 MB safety extraction limit"
                                    }
                                    out.write(buf, 0, read)
                                }
                            }
                            extractedPaths += target.absolutePath
                        }
                    }
                    entry = sevenZ.nextEntry
                }
            }
            return extractedPaths
        } catch (_: PasswordRequiredException) {
            throw DocumentPasswordRequiredException()
        } catch (ex: Throwable) {
            val msg = ex.message.orEmpty()
            if (msg.contains("password", ignoreCase = true) || msg.contains("Corrupted", ignoreCase = true)) {
                if (password == null) throw DocumentPasswordRequiredException()
                throw DocumentInvalidPasswordException("Wrong password for 7z archive")
            }
            throw ex
        }
    }

    private fun resolveSafeEntryFile(canonicalRoot: File, entryName: String): File {
        val candidate = File(canonicalRoot, entryName).canonicalFile
        val rootPathWithSep = canonicalRoot.path + File.separator
        require(candidate.path == canonicalRoot.path || candidate.path.startsWith(rootPathWithSep)) {
            "Blocked unsafe archive path traversal entry: $entryName"
        }
        return candidate
    }

    private fun materializeFile(uriString: String): Triple<File, Boolean, String> {
        val uri = Uri.parse(uriString)
        if (uri.scheme == "file" && uri.path != null) {
            val file = File(uri.path!!)
            return Triple(file, false, file.name)
        }
        val rawName = queryDisplayName(uri) ?: uri.lastPathSegment?.substringAfterLast('/') ?: "archive.zip"
        val safeName = rawName.replace(Regex("[/\\\\:*?\"<>|\\x00-\\x1F]"), "_").ifBlank { "archive.zip" }
        val temp = File(context.cacheDir, "archive_inspect_${System.nanoTime()}_$safeName")
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: FileInputStream(File(uriString))
            input.use { src -> FileOutputStream(temp).use { dst -> src.copyTo(dst) } }
            return Triple(temp, true, safeName)
        } catch (t: Throwable) {
            temp.delete()
            throw t
        }
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()

    companion object {
        private const val MAX_ENTRIES = 2_000
        private const val MAX_EXTRACTED_BYTES = 512L * 1024L * 1024L
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ArchiveEngineModule {
    @Binds
    @Singleton
    abstract fun bindArchiveEnginePort(impl: CommonsCompressArchiveEngine): ArchiveEnginePort
}
