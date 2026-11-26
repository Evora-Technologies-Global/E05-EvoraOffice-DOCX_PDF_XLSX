package com.evora.technologies.office.engine.transfer

import java.io.File

/**
 * Enforces strict canonical sandbox path resolution for both Ktor HTTP and FTP transfer servers.
 * Any path traversal attempt (`..`, backslashes, NUL bytes, or escaping [rootDir]) throws [PathTraversalException].
 */
class PathTraversalException(message: String) : SecurityException(message)

object TransferPathSandbox {

    fun resolveInsideRoot(rootDir: File, requestedRelativePath: String?): File {
        val canonicalRoot = rootDir.canonicalFile
        val raw = requestedRelativePath.orEmpty().trim()
        if (raw.isEmpty() || raw == "/" || raw == ".") {
            return canonicalRoot
        }
        if (raw.contains('\u0000') || raw.contains('\\')) {
            throw PathTraversalException("Blocked invalid path characters")
        }
        val cleanSegments = raw.trim('/').split('/')
        if (cleanSegments.any { it == ".." || it.isEmpty() && raw.contains("//") }) {
            throw PathTraversalException("Blocked path traversal attempt: $raw")
        }

        val candidate = File(canonicalRoot, raw.trimStart('/')).canonicalFile
        val rootPrefix = canonicalRoot.path + File.separator
        if (candidate.path != canonicalRoot.path && !candidate.path.startsWith(rootPrefix)) {
            throw PathTraversalException("Blocked directory escape: $raw")
        }
        return candidate
    }

    fun sanitizeSingleUploadFilename(rawFilename: String): String {
        val trimmed = rawFilename.trim()
        if (trimmed.contains("..") || trimmed.contains('/') || trimmed.contains('\\') || trimmed.contains('\u0000')) {
            throw PathTraversalException("Blocked unsafe upload filename: $rawFilename")
        }
        val cleaned = trimmed.replace(Regex("[:*?\"<>|\\x00-\\x1F]"), "_").trim()
        if (cleaned.isEmpty() || cleaned == "." || cleaned == "..") {
            throw PathTraversalException("Invalid upload filename")
        }
        return cleaned
    }

    fun uniqueTargetFile(directory: File, safeFileName: String): File {
        var candidate = File(directory, safeFileName)
        if (!candidate.exists()) return candidate
        val dot = safeFileName.lastIndexOf('.')
        val base = if (dot > 0) safeFileName.substring(0, dot) else safeFileName
        val ext = if (dot > 0) safeFileName.substring(dot) else ""
        var index = 1
        while (candidate.exists()) {
            candidate = File(directory, "${base}_$index$ext")
            index++
        }
        return candidate
    }
}
