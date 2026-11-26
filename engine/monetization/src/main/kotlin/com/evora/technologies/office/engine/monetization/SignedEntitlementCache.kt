package com.evora.technologies.office.engine.monetization

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.evora.technologies.office.core.datastore.EvoraPreferencesDataSource
import com.evora.technologies.office.core.model.SubscriptionPlan
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

interface EntitlementSigner {
    fun sign(data: ByteArray): ByteArray?
    fun verify(data: ByteArray, signature: ByteArray): Boolean
}

/**
 * Hardware-backed Android Keystore HMAC-SHA256 signer (API 26+) ensuring cached
 * Pro entitlements stored in DataStore cannot be forged offline.
 */
@Singleton
class AndroidKeystoreHmacSigner @Inject constructor() : EntitlementSigner {

    override fun sign(data: ByteArray): ByteArray? = runCatching {
        val key = getOrCreateHmacKey() ?: return null
        val mac = Mac.getInstance(HMAC_ALGORITHM).apply { init(key) }
        mac.doFinal(data)
    }.getOrNull()

    override fun verify(data: ByteArray, signature: ByteArray): Boolean = runCatching {
        val key = loadExistingKey() ?: return false
        val mac = Mac.getInstance(HMAC_ALGORITHM).apply { init(key) }
        val expected = mac.doFinal(data)
        MessageDigest.isEqual(expected, signature)
    }.getOrDefault(false)

    private fun getOrCreateHmacKey(): SecretKey? {
        loadExistingKey()?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
        ).build()
        generator.init(spec)
        return generator.generateKey()
    }

    private fun loadExistingKey(): SecretKey? {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return (ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val HMAC_ALGORITHM = "HmacSHA256"
        private const val KEY_ALIAS = "evora_pro_entitlement_hmac_v1"
    }
}

data class CachedEntitlement(
    val skuId: String,
    val isPro: Boolean,
    val verifiedAtEpochMs: Long,
    val expiresAtEpochMs: Long,
)

/**
 * Signed local offline entitlement cache persisted in [EvoraPreferencesDataSource].
 * Payload format (Base64URL): `<payloadB64>.<hmacSha256B64>`.
 */
@Singleton
class SignedEntitlementCache @Inject constructor(
    private val preferences: EvoraPreferencesDataSource,
    private val signer: EntitlementSigner,
) {
    suspend fun saveVerifiedEntitlement(
        skuId: String,
        purchaseToken: String,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        val expiresAt = offlineExpiryForSku(skuId, nowMs)
        val tokenDigest = sha256HexPrefix(purchaseToken)
        val body = "v=1|sku=$skuId|pro=1|tok=$tokenDigest|ver=$nowMs|exp=$expiresAt"
        val bodyBytes = body.toByteArray(Charsets.UTF_8)
        val sig = signer.sign(bodyBytes) ?: return
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val payload = "${encoder.encodeToString(bodyBytes)}.${encoder.encodeToString(sig)}"
        preferences.setEntitlementHmacPayload(payload)
    }

    fun verify(rawPayload: String?, nowMs: Long = System.currentTimeMillis()): CachedEntitlement? {
        if (rawPayload.isNullOrBlank()) return null
        val dot = rawPayload.indexOf('.')
        if (dot <= 0 || dot >= rawPayload.lastIndex) return null
        val decoder = Base64.getUrlDecoder()
        val bodyBytes = runCatching { decoder.decode(rawPayload.substring(0, dot)) }.getOrNull() ?: return null
        val sigBytes = runCatching { decoder.decode(rawPayload.substring(dot + 1)) }.getOrNull() ?: return null
        if (!signer.verify(bodyBytes, sigBytes)) return null

        val fields = String(bodyBytes, Charsets.UTF_8)
            .split('|')
            .mapNotNull { entry ->
                val eq = entry.indexOf('=')
                if (eq <= 0) null else entry.substring(0, eq) to entry.substring(eq + 1)
            }
            .toMap()

        if (fields["v"] != "1" || fields["pro"] != "1") return null
        val skuId = fields["sku"]?.takeIf { it.isNotBlank() } ?: return null
        val verifiedAt = fields["ver"]?.toLongOrNull() ?: return null
        val expiresAt = fields["exp"]?.toLongOrNull() ?: return null
        if (expiresAt <= nowMs || verifiedAt > nowMs + CLOCK_SKEW_MS) return null
        if (SubscriptionPlan.entries.none { it.skuId == skuId }) return null

        return CachedEntitlement(
            skuId = skuId,
            isPro = true,
            verifiedAtEpochMs = verifiedAt,
            expiresAtEpochMs = expiresAt,
        )
    }

    suspend fun clear() {
        preferences.setEntitlementHmacPayload(null)
    }

    private fun offlineExpiryForSku(skuId: String, nowMs: Long): Long = when (skuId) {
        SubscriptionPlan.LIFETIME.skuId -> Long.MAX_VALUE
        SubscriptionPlan.YEARLY.skuId -> nowMs + YEARLY_OFFLINE_GRACE_MS
        SubscriptionPlan.WEEKLY.skuId -> nowMs + WEEKLY_OFFLINE_GRACE_MS
        else -> nowMs + WEEKLY_OFFLINE_GRACE_MS
    }

    private fun sha256HexPrefix(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val WEEKLY_OFFLINE_GRACE_MS = 7L * 86_400_000L
        private const val YEARLY_OFFLINE_GRACE_MS = 14L * 86_400_000L
        private const val CLOCK_SKEW_MS = 86_400_000L
    }
}
