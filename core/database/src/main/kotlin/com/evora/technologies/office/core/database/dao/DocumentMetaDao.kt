package com.evora.technologies.office.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.evora.technologies.office.core.database.entity.DocumentMetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentMetaDao {
    @Query("SELECT * FROM document_meta WHERE last_opened_at_epoch_ms IS NOT NULL ORDER BY last_opened_at_epoch_ms DESC")
    fun observeRecent(): Flow<List<DocumentMetaEntity>>

    @Query("SELECT * FROM document_meta WHERE is_favorite = 1 ORDER BY modified_at_epoch_ms DESC")
    fun observeFavorites(): Flow<List<DocumentMetaEntity>>

    @Upsert
    suspend fun upsert(entity: DocumentMetaEntity)
}
