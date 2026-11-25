package com.evora.technologies.office.core.domain.util

import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.SortOrder

sealed interface FileNameValidationResult {
    data class Valid(val finalFileName: String) : FileNameValidationResult
    data class Invalid(val reason: String) : FileNameValidationResult
}

/**
 * Validates and guards file renames so invalid filesystem characters are rejected
 * and the original file extension is preserved unless intentionally preserved (US-013).
 */
object FileNameValidator {
    private val ILLEGAL_CHARS = setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|', '\u0000')

    fun validateAndPreserveExtension(
        rawInput: String,
        originalExtension: String,
        existingNamesInFolder: Set<String> = emptySet(),
    ): FileNameValidationResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return FileNameValidationResult.Invalid("File name cannot be empty")
        }
        if (trimmed == "." || trimmed == "..") {
            return FileNameValidationResult.Invalid("File name is invalid")
        }
        if (trimmed.any { it in ILLEGAL_CHARS || it.code < 0x20 }) {
            return FileNameValidationResult.Invalid("Contains unsupported characters (/ \\ : * ? \" < > |)")
        }

        val cleanExt = originalExtension.trimStart('.').lowercase()
        val strippedBase = if (cleanExt.isNotEmpty() && trimmed.lowercase().endsWith(".$cleanExt")) {
            trimmed.dropLast(cleanExt.length + 1).trimEnd()
        } else {
            trimmed
        }

        if (strippedBase.isEmpty()) {
            return FileNameValidationResult.Invalid("File name cannot be empty")
        }

        val finalName = if (cleanExt.isNotEmpty()) "$strippedBase.$cleanExt" else strippedBase
        if (existingNamesInFolder.any { it.equals(finalName, ignoreCase = true) }) {
            return FileNameValidationResult.Invalid("A file named \"$finalName\" already exists")
        }
        return FileNameValidationResult.Valid(finalName)
    }
}

object DocumentListProcessor {
    fun filterAndSort(
        documents: List<DocumentItem>,
        filter: DocFormat?,
        sortOrder: SortOrder,
        query: String = "",
    ): List<DocumentItem> {
        val normalizedQuery = query.trim().lowercase()
        val filtered = documents.filter { item ->
            val matchesFormat = filter == null || item.format == filter
            val matchesQuery = normalizedQuery.isEmpty() ||
                item.displayName.lowercase().contains(normalizedQuery) ||
                item.parentFolderName.lowercase().contains(normalizedQuery)
            matchesFormat && matchesQuery
        }
        return when (sortOrder) {
            SortOrder.MODIFIED_DESC -> filtered.sortedByDescending { it.modifiedAtEpochMs }
            SortOrder.MODIFIED_ASC -> filtered.sortedBy { it.modifiedAtEpochMs }
            SortOrder.NAME_ASC -> filtered.sortedBy { it.displayName.lowercase() }
            SortOrder.NAME_DESC -> filtered.sortedByDescending { it.displayName.lowercase() }
            SortOrder.SIZE_DESC -> filtered.sortedByDescending { it.sizeBytes }
            SortOrder.FORMAT -> filtered.sortedWith(
                compareBy<DocumentItem> { it.format.name }.thenBy { it.displayName.lowercase() },
            )
        }
    }
}
