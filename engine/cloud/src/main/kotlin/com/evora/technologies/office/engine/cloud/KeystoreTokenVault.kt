package com.evora.technologies.office.engine.cloud

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.evora.technologies.office.core.data.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

interface TokenVault {
    suspend fun storeToken(alias: String, token: String)
    suspend fun readToken(alias: String): String?
    suspend fun deleteToken(alias: String)
}

/**
 * Hardware-backed Android Keystore AES-256-GCM vault storing encrypted cloud OAuth tokens
 * in app-private `filesDir/cloud_vault/` (zero plaintext tokens in Room or DataStore).
 */
@Singleton
class AndroidKeystoreTokenVault @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TokenVault {

    private val vaultMutex = Mutex()

    override suspend fun storeToken(alias: String, token: String) = withContext(ioDispatcher) {
        vaultMutex.withLock {
            val secretKey = getOrCreateAesKey(alias)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, secretKey)
            }
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(token.toByteArray(Charsets.UTF_8))

            val file = tokenFile(alias)
            val payload = ByteArray(1 + iv.size + ciphertext.size)
            payload[0] = iv.size.toByte()
            System.arraycopy(iv, 0, payload, 1, iv.size)
            System.arraycopy(ciphertext, 0, payload, 1 + iv.size, ciphertext.size)
            file.writeBytes(payload)
        }
    }

    override suspend fun readToken(alias: String): String? = withContext(ioDispatcher) {
        vaultMutex.withLock {
            try {
                val file = tokenFile(alias)
                if (!file.exists()) return@withLock null
                val payload = file.readBytes()
                if (payload.size < 14) {
                    deleteTokenLocked(alias)
                    return@withLock null
                }
                val ivLen = payload[0].toInt() and 0xFF
                if (payload.size <= 1 + ivLen) {
                    deleteTokenLocked(alias)
                    return@withLock null
                }
                val iv = payload.copyOfRange(1, 1 + ivLen)
                val ciphertext = payload.copyOfRange(1 + ivLen, payload.size)

                val secretKey = loadExistingAesKey(alias) ?: return@withLock null
                val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                    init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
                }
                String(cipher.doFinal(ciphertext), Charsets.UTF_8)
            } catch (_: GeneralSecurityException) {
                deleteTokenLocked(alias)
                null
            } catch (_: IllegalArgumentException) {
                deleteTokenLocked(alias)
                null
            }
        }
    }

    override suspend fun deleteToken(alias: String) = withContext(ioDispatcher) {
        vaultMutex.withLock {
            deleteTokenLocked(alias)
        }
    }

    private fun deleteTokenLocked(alias: String) {
        val file = tokenFile(alias)
        if (file.exists()) {
            file.writeBytes(ByteArray(file.length().toInt().coerceAtMost(4096)))
            file.delete()
        }
        runCatching {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val keyAlias = keystoreAlias(alias)
            if (ks.containsAlias(keyAlias)) {
                ks.deleteEntry(keyAlias)
            }
        }
    }

    private fun tokenFile(alias: String): File {
        val dir = File(context.filesDir, "cloud_vault").apply { mkdirs() }
        val safeName = alias.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(dir, "$safeName.enc")
    }

    private fun keystoreAlias(alias: String): String =
        "evora_cloud_${alias.replace(Regex("[^A-Za-z0-9._-]"), "_")}"

    private fun getOrCreateAesKey(alias: String): SecretKey {
        loadExistingAesKey(alias)?.let { return it }
        val keyAlias = keystoreAlias(alias)
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private fun loadExistingAesKey(alias: String): SecretKey? {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return (ks.getEntry(keystoreAlias(alias), null) as? KeyStore.SecretKeyEntry)?.secretKey
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
