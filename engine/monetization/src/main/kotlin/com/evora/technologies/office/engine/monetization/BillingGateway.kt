package com.evora.technologies.office.engine.monetization

import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.evora.technologies.office.core.data.di.IoDispatcher
import com.evora.technologies.office.core.data.di.MainDispatcher
import com.evora.technologies.office.core.model.BillingOffer
import com.evora.technologies.office.core.model.PurchaseResult
import com.evora.technologies.office.core.model.SubscriptionPlan
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class VerifiedPlayPurchase(
    val skuId: String,
    val purchaseToken: String,
    val isAcknowledged: Boolean,
    val isAutoRenewing: Boolean,
    val purchaseTimeEpochMs: Long,
)

sealed interface PlayQueryPurchasesResult {
    data class Success(val purchases: List<VerifiedPlayPurchase>) : PlayQueryPurchasesResult
    data class Unavailable(val message: String) : PlayQueryPurchasesResult
}

sealed interface PurchaseUpdateEvent {
    data class Purchased(val purchases: List<VerifiedPlayPurchase>) : PurchaseUpdateEvent
    data object Pending : PurchaseUpdateEvent
    data object RefreshNeeded : PurchaseUpdateEvent
}

interface BillingGateway {
    val purchaseUpdates: Flow<PurchaseUpdateEvent>
    suspend fun queryOffers(): Result<List<BillingOffer>>
    suspend fun queryActivePurchases(): PlayQueryPurchasesResult
    suspend fun acknowledgeIfNeeded(purchaseToken: String): Boolean
    suspend fun launchPurchase(plan: SubscriptionPlan): PurchaseResult
}

/**
 * Production Google Play Billing 8.0.0 gateway using [PendingPurchasesParams],
 * [BillingClient.Builder.enableAutoServiceReconnection], and KTX coroutine extensions.
 */
@Singleton
class PlayBillingClientGateway @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val activityTracker: ActiveActivityTracker,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @param:MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) : BillingGateway, PurchasesUpdatedListener {

    private val connectMutex = Mutex()
    private val purchaseMutex = Mutex()
    private val productDetailsCache = ConcurrentHashMap<String, ProductDetails>()
    private val selectedOfferTokens = ConcurrentHashMap<String, String>()

    @Volatile
    private var activeAttemptSku: String? = null

    @Volatile
    private var pendingPurchaseDeferred: CompletableDeferred<PurchaseResult>? = null

    private val _purchaseUpdates = MutableSharedFlow<PurchaseUpdateEvent>(extraBufferCapacity = 16)
    override val purchaseUpdates: Flow<PurchaseUpdateEvent> = _purchaseUpdates.asSharedFlow()

    private val billingClient: BillingClient by lazy {
        val pendingParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .enablePrepaidPlans()
            .build()

        BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(pendingParams)
            .enableAutoServiceReconnection()
            .build()
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        val code = billingResult.responseCode
        when (code) {
            BillingClient.BillingResponseCode.OK -> {
                val list = purchases.orEmpty()
                val verified = list
                    .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    .mapNotNull(::toVerifiedPurchase)
                if (verified.isNotEmpty()) {
                    _purchaseUpdates.tryEmit(PurchaseUpdateEvent.Purchased(verified))
                    val targetSku = activeAttemptSku
                    val matchedSku = verified.firstOrNull { it.skuId == targetSku }?.skuId
                        ?: verified.first().skuId
                    pendingPurchaseDeferred?.complete(PurchaseResult.Success(matchedSku))
                    return
                }
                val hasPending = list.any { it.purchaseState == Purchase.PurchaseState.PENDING }
                if (hasPending) {
                    _purchaseUpdates.tryEmit(PurchaseUpdateEvent.Pending)
                    pendingPurchaseDeferred?.complete(PurchaseResult.Pending)
                } else {
                    pendingPurchaseDeferred?.complete(PurchaseResult.Cancelled)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                pendingPurchaseDeferred?.complete(PurchaseResult.Cancelled)
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                _purchaseUpdates.tryEmit(PurchaseUpdateEvent.RefreshNeeded)
                val sku = activeAttemptSku ?: SubscriptionPlan.YEARLY.skuId
                pendingPurchaseDeferred?.complete(PurchaseResult.Success(sku))
            }
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            -> {
                pendingPurchaseDeferred?.complete(
                    PurchaseResult.BillingUnavailable(
                        billingResult.debugMessage.ifBlank { "Google Play Billing is currently unavailable" },
                    ),
                )
            }
            else -> {
                pendingPurchaseDeferred?.complete(
                    PurchaseResult.Error(
                        billingResult.debugMessage.ifBlank { "Google Play Billing error ($code)" },
                    ),
                )
            }
        }
    }

    override suspend fun queryOffers(): Result<List<BillingOffer>> = withContext(ioDispatcher) {
        val connection = ensureConnected()
        if (connection.responseCode != BillingClient.BillingResponseCode.OK) {
            return@withContext Result.failure(
                IllegalStateException(connection.debugMessage.ifBlank { "Google Play Billing unavailable" }),
            )
        }

        val subsProducts = listOf(
            SubscriptionPlan.WEEKLY.skuId,
            SubscriptionPlan.YEARLY.skuId,
        ).map { sku ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(sku)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val inAppProducts = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SubscriptionPlan.LIFETIME.skuId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val subsResult = runCatching {
            billingClient.queryProductDetails(
                QueryProductDetailsParams.newBuilder().setProductList(subsProducts).build(),
            )
        }.getOrNull()

        val inAppResult = runCatching {
            billingClient.queryProductDetails(
                QueryProductDetailsParams.newBuilder().setProductList(inAppProducts).build(),
            )
        }.getOrNull()

        val allDetails = buildList {
            subsResult?.productDetailsList?.let(::addAll)
            inAppResult?.productDetailsList?.let(::addAll)
        }
        allDetails.forEach { details ->
            productDetailsCache[details.productId] = details
        }

        val offers = SubscriptionPlan.entries.map { plan ->
            val details = productDetailsCache[plan.skuId]
            if (details == null) {
                BillingOffer(
                    plan = plan,
                    formattedPrice = plan.priceDisplay,
                    billingSubtitle = plan.subtitle,
                    hasFreeTrial = plan != SubscriptionPlan.LIFETIME,
                    isFromPlayStore = false,
                )
            } else {
                mapDetailsToOffer(plan, details)
            }
        }
        Result.success(offers)
    }

    override suspend fun queryActivePurchases(): PlayQueryPurchasesResult = withContext(ioDispatcher) {
        val connection = ensureConnected()
        if (connection.responseCode != BillingClient.BillingResponseCode.OK) {
            return@withContext PlayQueryPurchasesResult.Unavailable(
                connection.debugMessage.ifBlank { "Google Play Billing unavailable" },
            )
        }

        val subsResult = runCatching {
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
            )
        }.getOrNull() ?: return@withContext PlayQueryPurchasesResult.Unavailable("Failed querying subscriptions")

        val inAppResult = runCatching {
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build(),
            )
        }.getOrNull() ?: return@withContext PlayQueryPurchasesResult.Unavailable("Failed querying one-time products")

        if (subsResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK ||
            inAppResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK
        ) {
            val msg = subsResult.billingResult.debugMessage
                .ifBlank { inAppResult.billingResult.debugMessage }
                .ifBlank { "Unable to query Google Play purchases" }
            return@withContext PlayQueryPurchasesResult.Unavailable(msg)
        }

        val verified = (subsResult.purchasesList + inAppResult.purchasesList)
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .mapNotNull(::toVerifiedPurchase)

        PlayQueryPurchasesResult.Success(verified)
    }

    override suspend fun acknowledgeIfNeeded(purchaseToken: String): Boolean = withContext(ioDispatcher) {
        val connection = ensureConnected()
        if (connection.responseCode != BillingClient.BillingResponseCode.OK) {
            return@withContext false
        }
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        val result = runCatching { billingClient.acknowledgePurchase(params) }.getOrNull()
        result?.responseCode == BillingClient.BillingResponseCode.OK
    }

    override suspend fun launchPurchase(plan: SubscriptionPlan): PurchaseResult = purchaseMutex.withLock {
        if (!productDetailsCache.containsKey(plan.skuId)) {
            queryOffers()
        }
        val details = productDetailsCache[plan.skuId]
            ?: return@withLock PurchaseResult.BillingUnavailable(
                "Plan '${plan.skuId}' is not yet available from Google Play.",
            )
        val activity = activityTracker.currentActivity()
            ?: return@withLock PurchaseResult.BillingUnavailable(
                "No active foreground window available to open Google Play checkout.",
            )

        val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
        if (plan != SubscriptionPlan.LIFETIME) {
            val offerToken = selectedOfferTokens[plan.skuId]
                ?: details.subscriptionOfferDetails?.firstOrNull()?.offerToken
                ?: return@withLock PurchaseResult.BillingUnavailable(
                    "No valid subscription offer found for ${plan.title}.",
                )
            productParamsBuilder.setOfferToken(offerToken)
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParamsBuilder.build()))
            .build()

        val deferred = CompletableDeferred<PurchaseResult>()
        activeAttemptSku = plan.skuId
        pendingPurchaseDeferred = deferred
        try {
            val launchResult = withContext(mainDispatcher) {
                billingClient.launchBillingFlow(activity, flowParams)
            }
            when (launchResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    withTimeoutOrNull(BILLING_FLOW_TIMEOUT_MS) { deferred.await() }
                        ?: PurchaseResult.Error("Google Play checkout timed out")
                }
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    _purchaseUpdates.tryEmit(PurchaseUpdateEvent.RefreshNeeded)
                    PurchaseResult.Success(plan.skuId)
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> PurchaseResult.Cancelled
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
                -> PurchaseResult.BillingUnavailable(
                    launchResult.debugMessage.ifBlank { "Google Play Billing unavailable" },
                )
                else -> PurchaseResult.Error(
                    launchResult.debugMessage.ifBlank {
                        "Unable to launch checkout (${launchResult.responseCode})"
                    },
                )
            }
        } finally {
            if (pendingPurchaseDeferred === deferred) {
                pendingPurchaseDeferred = null
                activeAttemptSku = null
            }
        }
    }

    private suspend fun ensureConnected(): BillingResult = connectMutex.withLock {
        if (billingClient.isReady) {
            return@withLock BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.OK)
                .build()
        }
        suspendCancellableCoroutine { cont ->
            try {
                billingClient.startConnection(
                    object : BillingClientStateListener {
                        override fun onBillingSetupFinished(billingResult: BillingResult) {
                            if (cont.isActive) cont.resume(billingResult)
                        }

                        override fun onBillingServiceDisconnected() {
                            if (cont.isActive) {
                                cont.resume(
                                    BillingResult.newBuilder()
                                        .setResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
                                        .setDebugMessage("Billing service disconnected")
                                        .build(),
                                )
                            }
                        }
                    },
                )
            } catch (t: Throwable) {
                if (cont.isActive) {
                    cont.resume(
                        BillingResult.newBuilder()
                            .setResponseCode(BillingClient.BillingResponseCode.BILLING_UNAVAILABLE)
                            .setDebugMessage(t.message ?: "Google Play Store unavailable")
                            .build(),
                    )
                }
            }
        }
    }

    private fun mapDetailsToOffer(plan: SubscriptionPlan, details: ProductDetails): BillingOffer {
        if (plan == SubscriptionPlan.LIFETIME) {
            val oneTime = details.oneTimePurchaseOfferDetails
            val price = oneTime?.formattedPrice?.let { "$it once" } ?: plan.priceDisplay
            return BillingOffer(
                plan = plan,
                formattedPrice = price,
                billingSubtitle = plan.subtitle,
                hasFreeTrial = false,
                isFromPlayStore = oneTime != null,
            )
        }

        val offers = details.subscriptionOfferDetails.orEmpty()
        val chosenOffer = offers.firstOrNull { offer ->
            offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
        } ?: offers.firstOrNull()

        if (chosenOffer == null) {
            return BillingOffer(
                plan = plan,
                formattedPrice = plan.priceDisplay,
                billingSubtitle = plan.subtitle,
                hasFreeTrial = true,
                isFromPlayStore = false,
            )
        }

        selectedOfferTokens[plan.skuId] = chosenOffer.offerToken
        val phases = chosenOffer.pricingPhases.pricingPhaseList
        val trialPhase = phases.firstOrNull { it.priceAmountMicros == 0L }
        val paidPhase = phases.lastOrNull { it.priceAmountMicros > 0L } ?: phases.lastOrNull()

        val periodLabel = periodSuffix(paidPhase?.billingPeriod.orEmpty())
        val price = paidPhase?.formattedPrice?.let { "$it$periodLabel" } ?: plan.priceDisplay
        val subtitle = when {
            trialPhase != null -> "Includes ${trialPeriodLabel(trialPhase.billingPeriod)}"
            else -> plan.subtitle
        }

        return BillingOffer(
            plan = plan,
            formattedPrice = price,
            billingSubtitle = subtitle,
            hasFreeTrial = trialPhase != null,
            isFromPlayStore = true,
        )
    }

    private fun toVerifiedPurchase(purchase: Purchase): VerifiedPlayPurchase? {
        val knownSkus = SubscriptionPlan.entries.map { it.skuId }.toSet()
        val matchedSku = purchase.products.firstOrNull { it in knownSkus } ?: return null
        return VerifiedPlayPurchase(
            skuId = matchedSku,
            purchaseToken = purchase.purchaseToken,
            isAcknowledged = purchase.isAcknowledged,
            isAutoRenewing = purchase.isAutoRenewing,
            purchaseTimeEpochMs = purchase.purchaseTime,
        )
    }

    private fun periodSuffix(isoPeriod: String): String = when (isoPeriod) {
        "P1W" -> " / week"
        "P1M" -> " / month"
        "P1Y" -> " / year"
        else -> ""
    }

    private fun trialPeriodLabel(isoPeriod: String): String = when (isoPeriod) {
        "P3D" -> "3-day free trial"
        "P7D", "P1W" -> "7-day free trial"
        "P14D", "P2W" -> "14-day free trial"
        "P1M" -> "1-month free trial"
        else -> "free trial"
    }

    companion object {
        private const val BILLING_FLOW_TIMEOUT_MS = 5 * 60_000L
    }
}
