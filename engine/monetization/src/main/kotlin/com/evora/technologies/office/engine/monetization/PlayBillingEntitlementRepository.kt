package com.evora.technologies.office.engine.monetization

import com.evora.technologies.office.core.data.di.IoDispatcher
import com.evora.technologies.office.core.datastore.EvoraPreferencesDataSource
import com.evora.technologies.office.core.domain.port.ProEntitlementPort
import com.evora.technologies.office.core.model.BillingOffer
import com.evora.technologies.office.core.model.ProFeature
import com.evora.technologies.office.core.model.ProStatus
import com.evora.technologies.office.core.model.PurchaseResult
import com.evora.technologies.office.core.model.SubscriptionPlan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IsDebugBuild

/**
 * Sprint 4 production [ProEntitlementPort] backed by Google Play Billing 8.0.0 ([BillingGateway])
 * and hardware-backed HMAC-SHA256 offline entitlement caching ([SignedEntitlementCache]).
 */
@Singleton
class PlayBillingEntitlementRepository @Inject constructor(
    private val preferences: EvoraPreferencesDataSource,
    private val signedCache: SignedEntitlementCache,
    private val billingGateway: BillingGateway,
    @param:IsDebugBuild private val isDebugBuild: Boolean,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ProEntitlementPort {

    private val repoScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val _billingOffers = MutableStateFlow(BillingOffer.defaults())

    override val proStatus: StateFlow<ProStatus> = combine(
        preferences.entitlementHmacPayload,
        preferences.qaProOverride,
    ) { signedPayload, qaOverride ->
        val now = System.currentTimeMillis()
        val verifiedCached = signedCache.verify(signedPayload, now)
        val debugOverride = isDebugBuild && qaOverride
        ProStatus(
            isPro = verifiedCached != null || debugOverride,
            activeSku = verifiedCached?.skuId ?: if (debugOverride) "qa_debug_pro" else null,
        )
    }.stateIn(
        scope = repoScope,
        started = SharingStarted.Eagerly,
        initialValue = ProStatus(),
    )

    init {
        billingGateway.purchaseUpdates
            .onEach { event ->
                when (event) {
                    is PurchaseUpdateEvent.Purchased -> persistVerifiedPurchases(event.purchases)
                    PurchaseUpdateEvent.RefreshNeeded -> syncPurchasesQuietly()
                    PurchaseUpdateEvent.Pending -> Unit
                }
            }
            .launchIn(repoScope)

        repoScope.launch {
            syncPurchasesQuietly()
            refreshCatalog()
        }
    }

    override suspend fun hasAccess(feature: ProFeature): Boolean = proStatus.value.isUnlocked

    override fun observeBillingOffers(): Flow<List<BillingOffer>> = _billingOffers.asStateFlow()

    override suspend fun refreshCatalog() {
        runCatching { billingGateway.queryOffers() }
            .getOrNull()
            ?.getOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?.let { offers -> _billingOffers.value = offers }
    }

    override suspend fun purchasePlan(plan: SubscriptionPlan): PurchaseResult {
        val result = billingGateway.launchPurchase(plan)
        if (result is PurchaseResult.Success) {
            syncPurchasesQuietly()
        }
        return result
    }

    override suspend fun restorePurchases(): Boolean {
        return when (val result = billingGateway.queryActivePurchases()) {
            is PlayQueryPurchasesResult.Success -> {
                if (result.purchases.isEmpty()) {
                    signedCache.clear()
                    false
                } else {
                    persistVerifiedPurchases(result.purchases)
                    true
                }
            }
            is PlayQueryPurchasesResult.Unavailable -> {
                // Keep valid signed offline cache untouched, but inform caller Play Store is unreachable.
                throw IllegalStateException(result.message)
            }
        }
    }

    override suspend fun setQaProOverride(enabled: Boolean) {
        if (isDebugBuild) {
            preferences.setQaProOverride(enabled)
        }
    }

    private suspend fun syncPurchasesQuietly() {
        try {
            when (val result = billingGateway.queryActivePurchases()) {
                is PlayQueryPurchasesResult.Success -> {
                    if (result.purchases.isEmpty()) {
                        signedCache.clear()
                    } else {
                        persistVerifiedPurchases(result.purchases)
                    }
                }
                is PlayQueryPurchasesResult.Unavailable -> Unit // Preserve signed offline cache
            }
        } catch (c: CancellationException) {
            throw c
        } catch (_: Exception) {
            // Preserve offline cache if Play Services throws during cold start
        }
    }

    private suspend fun persistVerifiedPurchases(purchases: List<VerifiedPlayPurchase>) {
        purchases.forEach { purchase ->
            if (!purchase.isAcknowledged) {
                billingGateway.acknowledgeIfNeeded(purchase.purchaseToken)
            }
        }
        val primary = pickHighestPriorityPurchase(purchases) ?: return
        signedCache.saveVerifiedEntitlement(
            skuId = primary.skuId,
            purchaseToken = primary.purchaseToken,
        )
    }

    private fun pickHighestPriorityPurchase(purchases: List<VerifiedPlayPurchase>): VerifiedPlayPurchase? {
        val priorityOrder = listOf(
            SubscriptionPlan.LIFETIME.skuId,
            SubscriptionPlan.YEARLY.skuId,
            SubscriptionPlan.WEEKLY.skuId,
        )
        for (sku in priorityOrder) {
            purchases.firstOrNull { it.skuId == sku }?.let { return it }
        }
        return purchases.firstOrNull()
    }
}
