package com.evora.technologies.office.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trash_items",
    indices = [
        Index(value = ["expires_at_epoch_ms"], name = "idx_trash_expires_at"),
    ],
)
data class TrashEntity(
    @PrimaryKey
    @ColumnInfo(name = "trash_id")
    val trashId: String,
    @ColumnInfo(name = "original_uri_string")
    val originalUriString: String,
    @ColumnInfo(name = "cached_file_path")
    val cachedFilePath: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "format")
    val format: String,
    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long,
    @ColumnInfo(name = "trashed_at_epoch_ms")
    val trashedAtEpochMs: Long,
    @ColumnInfo(name = "expires_at_epoch_ms")
    val expiresAtEpochMs: Long,
)

@Entity(tableName = "draft_recovery")
data class DraftRecoveryEntity(
    @PrimaryKey
    @ColumnInfo(name = "target_uri_string")
    val targetUriString: String,
    @ColumnInfo(name = "draft_cache_path")
    val draftCachePath: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "updated_at_epoch_ms")
    val updatedAtEpochMs: Long,
    @ColumnInfo(name = "has_pending_recovery")
    val hasPendingRecovery: Boolean = false,
)

@Entity(tableName = "signatures")
data class SignatureEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "asset_path")
    val assetPath: String,
    @ColumnInfo(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long,
)

@Entity(tableName = "cloud_accounts")
data class CloudAccountEntity(
    @PrimaryKey
    @ColumnInfo(name = "account_id")
    val accountId: String,
    @ColumnInfo(name = "provider")
    val provider: String,
    @ColumnInfo(name = "email")
    val email: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "avatar_uri")
    val avatarUri: String? = null,
    @ColumnInfo(name = "encrypted_token_alias")
    val encryptedTokenAlias: String,
)
