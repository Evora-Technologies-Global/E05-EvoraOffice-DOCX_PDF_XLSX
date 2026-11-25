package com.evora.technologies.office.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "document_meta",
    indices = [
        Index(value = ["last_opened_at_epoch_ms"]),
        Index(value = ["is_favorite", "modified_at_epoch_ms"]),
    ],
)
data class DocumentMetaEntity(
    @PrimaryKey
    @ColumnInfo(name = "uri_string")
    val uriString: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "format")
    val format: String,
    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long,
    @ColumnInfo(name = "modified_at_epoch_ms")
    val modifiedAtEpochMs: Long,
    @ColumnInfo(name = "last_opened_at_epoch_ms")
    val lastOpenedAtEpochMs: Long? = null,
    @ColumnInfo(name = "last_page_index")
    val lastPageIndex: Int = 0,
    @ColumnInfo(name = "last_total_pages")
    val lastTotalPages: Int = 0,
    @ColumnInfo(name = "last_scroll_offset_y")
    val lastScrollOffsetY: Float = 0f,
    @ColumnInfo(name = "last_zoom")
    val lastZoom: Float = 1f,
    @ColumnInfo(name = "reader_theme")
    val readerTheme: String = "LIGHT",
    @ColumnInfo(name = "is_continuous_scroll")
    val isContinuousScroll: Boolean = true,
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
)
