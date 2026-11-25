package com.evora.technologies.office.core.domain.util

/**
 * Parses 1-based PDF page range expressions (e.g. "1-3, 5, 8-10") into sorted 0-based page indices.
 */
object PageRangeParser {
    fun parseOneBasedRanges(expression: String, totalPages: Int): Result<List<Int>> {
        if (totalPages <= 0) {
            return Result.failure(IllegalArgumentException("Document has no pages"))
        }
        val cleaned = expression.trim()
        if (cleaned.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter a page range such as 1-3, 5"))
        }
        val zeroBased = linkedSetOf<Int>()
        val tokens = cleaned.split(',')
        for (rawToken in tokens) {
            val token = rawToken.trim()
            if (token.isEmpty()) continue
            if ('-' in token) {
                val parts = token.split('-')
                if (parts.size != 2) {
                    return Result.failure(IllegalArgumentException("Invalid range '$token'"))
                }
                val start = parts[0].trim().toIntOrNull()
                    ?: return Result.failure(IllegalArgumentException("Invalid page number in '$token'"))
                val end = parts[1].trim().toIntOrNull()
                    ?: return Result.failure(IllegalArgumentException("Invalid page number in '$token'"))
                if (start < 1 || end < start || end > totalPages) {
                    return Result.failure(IllegalArgumentException("Range '$token' is out of bounds (1..$totalPages)"))
                }
                for (page in start..end) {
                    zeroBased.add(page - 1)
                }
            } else {
                val single = token.toIntOrNull()
                    ?: return Result.failure(IllegalArgumentException("Invalid page '$token'"))
                if (single < 1 || single > totalPages) {
                    return Result.failure(IllegalArgumentException("Page $single is out of bounds (1..$totalPages)"))
                }
                zeroBased.add(single - 1)
            }
        }
        if (zeroBased.isEmpty()) {
            return Result.failure(IllegalArgumentException("No valid pages selected"))
        }
        return Result.success(zeroBased.toList().sorted())
    }

    fun splitEveryN(totalPages: Int, chunkSize: Int): List<List<Int>> {
        if (totalPages <= 0) return emptyList()
        val safeChunk = chunkSize.coerceIn(1, totalPages)
        return (0 until totalPages).chunked(safeChunk)
    }
}
