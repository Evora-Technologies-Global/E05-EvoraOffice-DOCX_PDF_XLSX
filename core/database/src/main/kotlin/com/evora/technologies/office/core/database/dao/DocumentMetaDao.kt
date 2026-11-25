package com.evora.technologies.office.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.evora.technologies.office.core.database.entity.DocumentMetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentMetaDao {
    @Query("SELECT * FROM document_meta ORDER BY modified_at_epoch_ms DESC")
    fun observeAll(): Flow<List<DocumentMetaEntity>>

    @Query("SELECT * FROM document_meta WHERE last_opened_at_epoch_ms IS NOT NULL ORDER BY last_opened_at_epoch_ms DESC")
    fun observeRecent(): Flow<List<DocumentMetaEntity>>

    @Query("SELECT * FROM document_meta WHERE is_favorite = 1 ORDER BY modified_at_epoch_ms DESC")
    fun observeFavorites(): Flow<List<DocumentMetaEntity>>

    @Query("SELECT * FROM document_meta WHERE display_name LIKE '%' || :query || '%' ORDER BY modified_at_epoch_ms DESC")
    fun searchByName(query: String): Flow<List<DocumentMetaEntity>>

    @Query("SELECT * FROM document_meta WHERE uri_string = :uriString LIMIT 1")
    suspend fun getByUri(uriString: String): DocumentMetaEntity?

    @Upsert
    suspend fun upsert(entity: DocumentMetaEntity)

    @Upsert
    suspend fun upsertAll(entities: List<DocumentMetaEntity>)

    @Query("UPDATE document_meta SET is_favorite = :isFavorite WHERE uri_string = :uriString")
    suspend fun setFavorite(uriString: String, isFavorite: Boolean)

    @Query(
        """
        UPDATE document_meta
        SET last_opened_at_epoch_ms = :openedAtMs,
            last_page_index = :pageIndex,
            last_total_pages = :totalPages,
            last_scroll_offset_y = :scrollOffsetY,
            last_zoom = :zoom,
            reader_theme = :readerTheme,
            is_continuous_scroll = :isContinuousScroll
        WHERE uri_string = :uriString
        """,
    )
    suspend fun updateReadingMemory(
        uriString: String,
        openedAtMs: Long,
        pageIndex: Int,
        totalPages: Int,
        scrollOffsetY: Float,
        zoom: Float,
        readerTheme: String,
        isContinuousScroll: Boolean,
    )

    @Query("DELETE FROM document_meta WHERE uri_string = :uriString")
    suspend fun deleteByUri(uriString: String)
}
