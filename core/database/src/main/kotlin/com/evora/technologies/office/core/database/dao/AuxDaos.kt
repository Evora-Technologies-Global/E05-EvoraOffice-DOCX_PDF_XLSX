package com.evora.technologies.office.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.evora.technologies.office.core.database.entity.CloudAccountEntity
import com.evora.technologies.office.core.database.entity.DraftRecoveryEntity
import com.evora.technologies.office.core.database.entity.SignatureEntity
import com.evora.technologies.office.core.database.entity.TrashEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {
    @Query("SELECT * FROM trash_items ORDER BY trashed_at_epoch_ms DESC")
    fun observeAll(): Flow<List<TrashEntity>>

    @Query("SELECT * FROM trash_items WHERE trash_id = :trashId LIMIT 1")
    suspend fun getById(trashId: String): TrashEntity?

    @Query("SELECT * FROM trash_items WHERE expires_at_epoch_ms <= :nowEpochMs")
    suspend fun findExpired(nowEpochMs: Long): List<TrashEntity>

    @Query("SELECT * FROM trash_items")
    suspend fun getAllSnapshot(): List<TrashEntity>

    @Upsert
    suspend fun upsert(entity: TrashEntity)

    @Query("DELETE FROM trash_items WHERE trash_id = :trashId")
    suspend fun deleteById(trashId: String)

    @Query("DELETE FROM trash_items WHERE expires_at_epoch_ms <= :nowEpochMs")
    suspend fun deleteExpired(nowEpochMs: Long): Int

    @Query("DELETE FROM trash_items")
    suspend fun deleteAll(): Int
}

@Dao
interface DraftRecoveryDao {
    @Query("SELECT * FROM draft_recovery WHERE has_pending_recovery = 1 ORDER BY updated_at_epoch_ms DESC")
    fun observePending(): Flow<List<DraftRecoveryEntity>>

    @Query("SELECT * FROM draft_recovery WHERE target_uri_string = :targetUriString LIMIT 1")
    suspend fun getByTargetUri(targetUriString: String): DraftRecoveryEntity?

    @Upsert
    suspend fun upsert(entity: DraftRecoveryEntity)

    @Query("UPDATE draft_recovery SET has_pending_recovery = 0 WHERE target_uri_string = :targetUriString")
    suspend fun clearPending(targetUriString: String)

    @Query("DELETE FROM draft_recovery WHERE target_uri_string = :targetUriString")
    suspend fun deleteByTargetUri(targetUriString: String)
}

@Dao
interface SignatureDao {
    @Query("SELECT * FROM signatures ORDER BY created_at_epoch_ms DESC")
    fun observeAll(): Flow<List<SignatureEntity>>

    @Upsert
    suspend fun upsert(entity: SignatureEntity)

    @Query("DELETE FROM signatures WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface CloudAccountDao {
    @Query("SELECT * FROM cloud_accounts ORDER BY provider ASC, display_name ASC")
    fun observeAll(): Flow<List<CloudAccountEntity>>

    @Query("SELECT * FROM cloud_accounts WHERE provider = :provider ORDER BY display_name ASC")
    fun observeByProvider(provider: String): Flow<List<CloudAccountEntity>>

    @Upsert
    suspend fun upsert(entity: CloudAccountEntity)

    @Query("DELETE FROM cloud_accounts WHERE account_id = :accountId")
    suspend fun deleteById(accountId: String)
}
