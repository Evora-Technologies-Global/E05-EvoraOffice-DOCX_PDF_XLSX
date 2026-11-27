package com.evora.technologies.office.core.domain.util

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Reads ZIP entries with strict entry-count and uncompressed byte caps to prevent
 * Zip-Bomb / OOM denial-of-service on untrusted OOXML, ODF, HWPX, or EPUB containers.
 */
object BoundedZipReader {

    const val DEFAULT_MAX_ENTRIES = 2_000
    const val DEFAULT_MAX_UNCOMPRESSED_BYTES = 128L * 1024L * 1024L

    fun readEntries(
        inputStream: InputStream,
        maxEntries: Int = DEFAULT_MAX_ENTRIES,
        maxTotalBytes: Long = DEFAULT_MAX_UNCOMPRESSED_BYTES,
        entryFilter: (String) -> Boolean = { true },
    ): Map<String, ByteArray> {
        val entries = LinkedHashMap<String, ByteArray>()
        var entryCount = 0
        var totalBytes = 0L
        val buffer = ByteArray(16 * 1024)

        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entryCount++
                require(entryCount <= maxEntries) {
                    "Archive exceeds safety entry limit ($maxEntries)"
                }
                val name = entry.name
                if (!entry.isDirectory && entryFilter(name)) {
                    val out = ByteArrayOutputStream()
                    while (true) {
                        val read = zis.read(buffer)
                        if (read <= 0) break
                        totalBytes += read
                        require(totalBytes <= maxTotalBytes) {
                            "Archive exceeds safety uncompressed size limit (${maxTotalBytes / (1024 * 1024)} MB)"
                        }
                        out.write(buffer, 0, read)
                    }
                    entries[name] = out.toByteArray()
                }
                entry = zis.nextEntry
            }
        }
        return entries
    }
}
