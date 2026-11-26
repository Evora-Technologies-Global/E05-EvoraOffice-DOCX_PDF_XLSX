package com.evora.technologies.office.engine.cloud

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import com.evora.technologies.office.core.data.di.IoDispatcher
import com.evora.technologies.office.core.database.dao.CloudAccountDao
import com.evora.technologies.office.core.database.dao.DocumentMetaDao
import com.evora.technologies.office.core.database.entity.CloudAccountEntity
import com.evora.technologies.office.core.database.entity.DocumentMetaEntity
import com.evora.technologies.office.core.domain.port.CloudDrivePort
import com.evora.technologies.office.core.model.CloudAccount
import com.evora.technologies.office.core.model.CloudBrowsePage
import com.evora.technologies.office.core.model.CloudItem
import com.evora.technologies.office.core.model.CloudProvider
import com.evora.technologies.office.core.model.CloudTransferProgress
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocumentItem
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GoogleCloudRemote

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OneDriveCloudRemote

@Singleton
class CloudDriveEngineImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val cloudAccountDao: CloudAccountDao,
    private val documentMetaDao: DocumentMetaDao,
    private val tokenVault: TokenVault,
    @param:GoogleCloudRemote private val googleDriveClient: CloudRemoteProvider,
    @param:OneDriveCloudRemote private val oneDriveClient: CloudRemoteProvider,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CloudDrivePort {

    private val _transferProgress = MutableStateFlow<CloudTransferProgress?>(null)
    @Volatile
    private var activeTransferToken: String? = null

    override fun observeConnectedAccounts(): Flow<List<CloudAccount>> = cloudAccountDao.observeAll()
        .map { entities ->
            entities.map { entity ->
                CloudAccount(
                    accountId = entity.accountId,
                    provider = runCatching { CloudProvider.valueOf(entity.provider) }
                        .getOrDefault(CloudProvider.GOOGLE_DRIVE),
                    email = entity.email,
                    displayName = entity.displayName,
                    avatarUri = entity.avatarUri,
                    encryptedTokenAlias = entity.encryptedTokenAlias,
                )
            }
        }
        .flowOn(ioDispatcher)

    override fun observeTransferProgress(): Flow<CloudTransferProgress?> = _transferProgress.asStateFlow()

    override suspend fun isProviderConfigured(provider: CloudProvider): Boolean = withContext(ioDispatcher) {
        when (provider) {
            CloudProvider.GOOGLE_DRIVE -> true
            CloudProvider.ONE_DRIVE -> {
                val resId = context.resources.getIdentifier("evora_onedrive_client_id", "string", context.packageName)
                if (resId == 0) false else context.getString(resId).isNotBlank()
            }
        }
    }

    override suspend fun connectGoogleDrive(
        accessToken: String,
        email: String,
        displayName: String,
        avatarUri: String?,
    ): Result<CloudAccount> = withContext(ioDispatcher) {
        saveAccountAndToken(
            provider = CloudProvider.GOOGLE_DRIVE,
            accountId = "gdrive_${email.lowercase().hashCode()}",
            accessToken = accessToken,
            email = email,
            displayName = displayName,
            avatarUri = avatarUri,
        )
    }

    override suspend fun connectOneDrive(
        accessToken: String,
        accountId: String,
        email: String,
        displayName: String,
    ): Result<CloudAccount> = withContext(ioDispatcher) {
        saveAccountAndToken(
            provider = CloudProvider.ONE_DRIVE,
            accountId = accountId.ifBlank { "onedrive_${email.lowercase().hashCode()}" },
            accessToken = accessToken,
            email = email,
            displayName = displayName,
            avatarUri = null,
        )
    }

    override suspend fun disconnectAccount(accountId: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            // Delete token alias deterministically
            tokenVault.deleteToken(aliasForAccount(accountId))
            cloudAccountDao.deleteById(accountId)
            Result.success(Unit)
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun listFolder(
        account: CloudAccount,
        folderId: String,
        pageToken: String?,
        query: String,
    ): Result<CloudBrowsePage> = withContext(ioDispatcher) {
        try {
            val token = tokenVault.readToken(account.encryptedTokenAlias)
                ?: return@withContext Result.failure(IllegalStateException("Cloud session expired. Reconnect ${account.email}."))
            val client = remoteFor(account.provider)
            val page = client.listFolder(token, folderId, pageToken, query)
            Result.success(page)
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            invalidateTokenIfUnauthorized(account, t)
            Result.failure(t)
        }
    }

    override suspend fun downloadFile(
        account: CloudAccount,
        item: CloudItem,
    ): Result<DocumentItem> = withContext(ioDispatcher) {
        var destFile: File? = null
        val transferToken = UUID.randomUUID().toString()
        try {
            val token = tokenVault.readToken(account.encryptedTokenAlias)
                ?: return@withContext Result.failure(IllegalStateException("Cloud session expired. Reconnect ${account.email}."))

            val cloudRoot = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir,
                "EvoraCloud/${account.provider.name.lowercase()}",
            ).apply { mkdirs() }
            val safeName = item.name.replace(Regex("[/\\\\:*?\"<>|\\x00-\\x1F]"), "_").ifBlank { "cloud_file" }
            val resolvedDest = uniqueFile(cloudRoot, safeName)
            destFile = resolvedDest
            activeTransferToken = transferToken

            val client = remoteFor(account.provider)
            client.downloadFile(token, item, resolvedDest) { done, total ->
                if (activeTransferToken == transferToken) {
                    val frac = if (total > 0) (done.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                    _transferProgress.value = CloudTransferProgress(item.name, false, done, total, frac)
                }
            }
            if (activeTransferToken == transferToken) {
                _transferProgress.value = null
                activeTransferToken = null
            }

            val ext = resolvedDest.extension.lowercase()
            val fmt = DocFormat.fromExtension(ext) ?: DocFormat.TEXT
            val uri = resolvedDest.toURI().toString()
            val now = System.currentTimeMillis()
            val doc = DocumentItem(
                id = uri,
                uriString = uri,
                displayName = resolvedDest.name,
                extension = ext,
                format = fmt,
                sizeBytes = resolvedDest.length(),
                modifiedAtEpochMs = now,
                parentFolderName = account.provider.name,
                lastOpenedAtEpochMs = now,
            )
            documentMetaDao.upsert(
                DocumentMetaEntity(
                    uriString = uri,
                    displayName = resolvedDest.name,
                    format = fmt.name,
                    sizeBytes = resolvedDest.length(),
                    modifiedAtEpochMs = now,
                    lastOpenedAtEpochMs = now,
                ),
            )
            Result.success(doc)
        } catch (c: CancellationException) {
            runCatching { destFile?.delete() }
            clearTransferProgress(transferToken)
            throw c
        } catch (t: Throwable) {
            runCatching { destFile?.delete() }
            clearTransferProgress(transferToken)
            invalidateTokenIfUnauthorized(account, t)
            Result.failure(t)
        }
    }

    override suspend fun uploadLocalFile(
        account: CloudAccount,
        localUriString: String,
        targetFolderId: String,
    ): Result<CloudItem> = withContext(ioDispatcher) {
        var tempCopy: File? = null
        val transferToken = UUID.randomUUID().toString()
        try {
            val token = tokenVault.readToken(account.encryptedTokenAlias)
                ?: return@withContext Result.failure(IllegalStateException("Cloud session expired. Reconnect ${account.email}."))

            val parsed = Uri.parse(localUriString)
            val localFile = if (parsed.scheme == "file" && parsed.path != null) {
                File(parsed.path!!)
            } else {
                val name = queryDisplayName(parsed) ?: "upload_${UUID.randomUUID().toString().take(6)}.pdf"
                val tmp = File(context.cacheDir, "cloud_up_${System.nanoTime()}_$name")
                tempCopy = tmp
                val input = context.contentResolver.openInputStream(parsed)
                    ?: FileInputStream(File(localUriString))
                input.use { src -> FileOutputStream(tmp).use { dst -> src.copyTo(dst) } }
                tmp
            }

            val mime = context.contentResolver.getType(parsed) ?: "application/octet-stream"
            activeTransferToken = transferToken
            val client = remoteFor(account.provider)
            val uploaded = client.uploadFile(token, localFile, mime, targetFolderId) { done, total ->
                if (activeTransferToken == transferToken) {
                    val frac = if (total > 0) (done.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                    _transferProgress.value = CloudTransferProgress(localFile.name, true, done, total, frac)
                }
            }
            clearTransferProgress(transferToken)
            Result.success(uploaded)
        } catch (c: CancellationException) {
            clearTransferProgress(transferToken)
            throw c
        } catch (t: Throwable) {
            clearTransferProgress(transferToken)
            invalidateTokenIfUnauthorized(account, t)
            Result.failure(t)
        } finally {
            tempCopy?.delete()
        }
    }

    private fun clearTransferProgress(token: String) {
        if (activeTransferToken == token) {
            _transferProgress.value = null
            activeTransferToken = null
        }
    }

    private suspend fun invalidateTokenIfUnauthorized(account: CloudAccount, error: Throwable) {
        val msg = error.message.orEmpty()
        if (msg.contains("401") ||
            msg.contains("InvalidAuthenticationToken", ignoreCase = true) ||
            msg.contains("UNAUTHENTICATED", ignoreCase = true)
        ) {
            runCatching { tokenVault.deleteToken(account.encryptedTokenAlias) }
        }
    }

    private suspend fun saveAccountAndToken(
        provider: CloudProvider,
        accountId: String,
        accessToken: String,
        email: String,
        displayName: String,
        avatarUri: String?,
    ): Result<CloudAccount> {
        return try {
            val alias = aliasForAccount(accountId)
            tokenVault.storeToken(alias, accessToken)
            val entity = CloudAccountEntity(
                accountId = accountId,
                provider = provider.name,
                email = email,
                displayName = displayName.ifBlank { email },
                avatarUri = avatarUri,
                encryptedTokenAlias = alias,
            )
            cloudAccountDao.upsert(entity)
            Result.success(
                CloudAccount(
                    accountId = entity.accountId,
                    provider = provider,
                    email = entity.email,
                    displayName = entity.displayName,
                    avatarUri = entity.avatarUri,
                    encryptedTokenAlias = alias,
                ),
            )
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun remoteFor(provider: CloudProvider): CloudRemoteProvider = when (provider) {
        CloudProvider.GOOGLE_DRIVE -> googleDriveClient
        CloudProvider.ONE_DRIVE -> oneDriveClient
    }

    private fun aliasForAccount(accountId: String): String =
        "tok_${accountId.replace(Regex("[^A-Za-z0-9._-]"), "_")}"

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()

    private fun uniqueFile(dir: File, fileName: String): File {
        var candidate = File(dir, fileName)
        if (!candidate.exists()) return candidate
        val dot = fileName.lastIndexOf('.')
        val base = if (dot > 0) fileName.substring(0, dot) else fileName
        val ext = if (dot > 0) fileName.substring(dot) else ""
        var i = 1
        while (candidate.exists()) {
            candidate = File(dir, "${base}_$i$ext")
            i++
        }
        return candidate
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class CloudEngineModule {
    @Binds
    @Singleton
    abstract fun bindTokenVault(impl: AndroidKeystoreTokenVault): TokenVault

    @Binds
    @Singleton
    @GoogleCloudRemote
    abstract fun bindGoogleRemote(impl: GoogleDriveCloudClient): CloudRemoteProvider

    @Binds
    @Singleton
    @OneDriveCloudRemote
    abstract fun bindOneDriveRemote(impl: OneDriveCloudClient): CloudRemoteProvider

    @Binds
    @Singleton
    abstract fun bindCloudDrivePort(impl: CloudDriveEngineImpl): CloudDrivePort
}
