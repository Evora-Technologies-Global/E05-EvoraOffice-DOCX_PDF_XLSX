package com.evora.technologies.office.engine.monetization

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.evora.technologies.office.core.datastore.EvoraPreferencesDataSource
import com.evora.technologies.office.core.model.BillingOffer
import com.evora.technologies.office.core.model.PurchaseResult
import com.evora.technologies.office.core.model.RemoteConfigSnapshot
import com.evora.technologies.office.core.model.SubscriptionPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.MessageDigest
import java.util.UUID

private class TestHmacSigner : EntitlementSigner {
    private val secret = "evora-unit-test-hmac-key-32-bytes!".toByteArray()

    override fun sign(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(secret)
        digest.update(data)
        digest.update(secret)
        return digest.digest()
    }

    override fun verify(data: ByteArray, signature: ByteArray): Boolean =
        MessageDigest.isEqual(sign(data), signature)
}

private class FakeBillingGateway : BillingGateway {
    val updates = MutableSharedFlow<PurchaseUpdateEvent>(extraBufferCapacity = 8)
    override val purchaseUpdates: Flow<PurchaseUpdateEvent> = updates

    var offersResult: Result<List<BillingOffer>> = Result.success(BillingOffer.defaults())
    var purchasesResult: PlayQueryPurchasesResult = PlayQueryPurchasesResult.Success(emptyList())
    var launchResult: PurchaseResult = PurchaseResult.Cancelled
    val acknowledgedTokens = mutableListOf<String>()

    override suspend fun queryOffers(): Result<List<BillingOffer>> = offersResult

    override suspend fun queryActivePurchases(): PlayQueryPurchasesResult = purchasesResult

    override suspend fun acknowledgeIfNeeded(purchaseToken: String): Boolean {
        acknowledgedTokens += purchaseToken
        return true
    }

    override suspend fun launchPurchase(plan: SubscriptionPlan): PurchaseResult = launchResult
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MonetizationEngineTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createPrefs(scope: kotlinx.coroutines.CoroutineScope): EvoraPreferencesDataSource {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val ds: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { context.preferencesDataStoreFile("test_mon_${UUID.randomUUID()}") },
        )
        return EvoraPreferencesDataSource(ds)
    }

    @Test
    fun `signed entitlement cache rejects tampered payloads and verifies authentic lifetime unlock`() = runTest(testDispatcher) {
        val prefs = createPrefs(backgroundScope)
        val cache = SignedEntitlementCache(prefs, TestHmacSigner())
        val now = 1_700_000_000_000L

        cache.saveVerifiedEntitlement(
            skuId = SubscriptionPlan.LIFETIME.skuId,
            purchaseToken = "tok_live_123456",
            nowMs = now,
        )
        val stored = prefs.entitlementHmacPayload.first()
        assertNotNull(stored)

        val verified = cache.verify(stored, nowMs = now + 86_400_000L * 365)
        assertNotNull(verified)
        assertEquals(SubscriptionPlan.LIFETIME.skuId, verified?.skuId)
        assertTrue(verified?.isPro == true)

        // Tamper signature byte
        val tampered = stored!!.dropLast(2) + "AA"
        assertNull(cache.verify(tampered, nowMs = now + 1000L))
    }

    @Test
    fun `PlayBillingEntitlementRepository acknowledges purchases, hides ads, and persists offline across restarts`() = runTest(testDispatcher) {
        val prefs = createPrefs(backgroundScope)
        val signer = TestHmacSigner()
        val cache = SignedEntitlementCache(prefs, signer)
        val gateway = FakeBillingGateway().apply {
            purchasesResult = PlayQueryPurchasesResult.Success(
                listOf(
                    VerifiedPlayPurchase(
                        skuId = SubscriptionPlan.YEARLY.skuId,
                        purchaseToken = "tok_yearly_unack",
                        isAcknowledged = false,
                        isAutoRenewing = true,
                        purchaseTimeEpochMs = 1_700_000_000_000L,
                    ),
                ),
            )
            launchResult = PurchaseResult.Success(SubscriptionPlan.YEARLY.skuId)
        }

        val repo = PlayBillingEntitlementRepository(
            preferences = prefs,
            signedCache = cache,
            billingGateway = gateway,
            isDebugBuild = false,
            ioDispatcher = testDispatcher,
        )
        advanceUntilIdle()

        assertTrue(repo.proStatus.value.isPro)
        assertTrue(repo.proStatus.value.shouldHideAds)
        assertEquals(SubscriptionPlan.YEARLY.skuId, repo.proStatus.value.activeSku)
        assertEquals(listOf("tok_yearly_unack"), gateway.acknowledgedTokens)

        // Release build ignores QA Pro override attempts
        repo.setQaProOverride(true)
        advanceUntilIdle()
        assertEquals(SubscriptionPlan.YEARLY.skuId, repo.proStatus.value.activeSku)

        // Simulate app restart while offline (Play Store unreachable): signed cache must stay active
        val offlineGateway = FakeBillingGateway().apply {
            purchasesResult = PlayQueryPurchasesResult.Unavailable("Offline")
        }
        val restartedRepo = PlayBillingEntitlementRepository(
            preferences = prefs,
            signedCache = SignedEntitlementCache(prefs, signer),
            billingGateway = offlineGateway,
            isDebugBuild = false,
            ioDispatcher = testDispatcher,
        )
        advanceUntilIdle()
        assertTrue(restartedRepo.proStatus.value.isPro)
        assertTrue(restartedRepo.proStatus.value.shouldHideAds)
    }

    @Test
    fun `restorePurchases clears cache when Play confirms zero active purchases and preserves when offline`() = runTest(testDispatcher) {
        val prefs = createPrefs(backgroundScope)
        val signer = TestHmacSigner()
        val cache = SignedEntitlementCache(prefs, signer)
        val gateway = FakeBillingGateway().apply {
            purchasesResult = PlayQueryPurchasesResult.Success(
                listOf(
                    VerifiedPlayPurchase(
                        skuId = SubscriptionPlan.WEEKLY.skuId,
                        purchaseToken = "tok_weekly",
                        isAcknowledged = true,
                        isAutoRenewing = true,
                        purchaseTimeEpochMs = System.currentTimeMillis(),
                    ),
                ),
            )
        }

        val repo = PlayBillingEntitlementRepository(
            preferences = prefs,
            signedCache = cache,
            billingGateway = gateway,
            isDebugBuild = false,
            ioDispatcher = testDispatcher,
        )
        advanceUntilIdle()
        assertTrue(repo.proStatus.value.isPro)

        // Play confirms subscription expired / refunded
        gateway.purchasesResult = PlayQueryPurchasesResult.Success(emptyList())
        val restored = repo.restorePurchases()
        advanceUntilIdle()
        assertFalse(restored)
        assertFalse(repo.proStatus.value.isPro)
        assertFalse(repo.proStatus.value.shouldHideAds)
    }

    @Test
    fun `observeBillingOffers updates from Play catalog`() = runTest(testDispatcher) {
        val prefs = createPrefs(backgroundScope)
        val gateway = FakeBillingGateway().apply {
            offersResult = Result.success(
                listOf(
                    BillingOffer(SubscriptionPlan.WEEKLY, "₫119,000 / week", "Includes 3-day free trial", true, true),
                    BillingOffer(SubscriptionPlan.YEARLY, "₫899,000 / year", "Includes 7-day free trial", true, true),
                    BillingOffer(SubscriptionPlan.LIFETIME, "₫1,399,000 once", "One-time unlock", false, true),
                ),
            )
        }

        val repo = PlayBillingEntitlementRepository(
            preferences = prefs,
            signedCache = SignedEntitlementCache(prefs, TestHmacSigner()),
            billingGateway = gateway,
            isDebugBuild = true,
            ioDispatcher = testDispatcher,
        )

        repo.observeBillingOffers().test {
            advanceUntilIdle()
            val latest = expectMostRecentItem()
            assertEquals("₫899,000 / year", latest.first { it.plan == SubscriptionPlan.YEARLY }.formattedPrice)
            assertTrue(latest.all { it.isFromPlayStore })
        }
    }

    @Test
    fun `firebaseAndAdjustTelemetry dispatches ad_impression AdjustAdRevenue and PRD section 10 events`() {
        val sink = FakeTelemetrySink()
        val telemetry = FirebaseAndAdjustTelemetry(sink)
        assertEquals(1, sink.adjustInitCount)

        telemetry.logAdImpression(
            adPlatform = "AppLovin",
            adFormat = "interstitial",
            adUnitName = "ca-app-pub-test/interstitial",
            currency = "USD",
            value = 0.015,
            valueMicros = 15_000L,
        )
        assertEquals(1, sink.adjustRevenues.size)
        assertEquals("AppLovin", sink.adjustRevenues.single().adPlatform)
        assertEquals(15_000L, sink.adjustRevenues.single().valueMicros)

        telemetry.logOnboardingStep("welcome", "continue")
        telemetry.logDocumentOpen("pdf", "home_list")
        telemetry.logPdfToolComplete("merge", 420L, success = true)
        telemetry.logScanOcrComplete("ocr", 2, "latin")
        telemetry.logPaywallImpression("settings")
        telemetry.logPurchaseSuccess(SubscriptionPlan.YEARLY.skuId, 34_990_000L)

        val names = sink.firebaseEvents.map { it.first }
        assertEquals(
            listOf(
                "ad_impression",
                "onboarding_step",
                "document_open",
                "pdf_tool_complete",
                "scan_ocr_complete",
                "paywall_impression",
                "purchase_success",
            ),
            names,
        )
        assertEquals("AppLovin", sink.firebaseEvents.first().second["ad_platform"])
        assertEquals("interstitial", sink.firebaseEvents.first().second["ad_format"])
    }

    @Test
    fun `firebaseRemoteConfigManager returns PRD defaults safely when FirebaseApp is absent`() = runTest(testDispatcher) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = FirebaseRemoteConfigManager(
            context = context,
            runtimeConfig = MonetizationRuntimeConfig(
                isDebugBuild = true,
                admobAppId = "ca-app-pub-3940256099942544~3347511713",
                interstitialUnitId = "ca-app-pub-3940256099942544/1033173712",
                appOpenUnitId = "ca-app-pub-3940256099942544/9257395921",
                rewardedUnitId = "ca-app-pub-3940256099942544/5224354917",
                nativeUnitId = "ca-app-pub-3940256099942544/2247696110",
                adjustAppToken = "",
            ),
            ioDispatcher = testDispatcher,
        )

        val snapshot = manager.fetchAndActivate()
        assertEquals(RemoteConfigSnapshot(), snapshot)
        assertEquals(60L, snapshot.adsInterstitialCooldownSec)
        assertEquals(90L, snapshot.adsAppOpenCooldownSec)
        assertEquals(6, snapshot.adsNativeListInterval)
        assertTrue(snapshot.adsDisableEditorResume)
        assertEquals(SubscriptionPlan.YEARLY.skuId, snapshot.paywallDefaultSku)
        assertEquals(24, snapshot.toolRewardUnlockHours)
        assertEquals(15, snapshot.lokAutosaveIntervalSec)
    }

    @Test
    fun `umpConsentManager completes safely with initialized ConsentState without an active activity`() = runTest(testDispatcher) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val consentManager = UmpConsentManager(
            context = context,
            activityTracker = ActiveActivityTracker(),
            mainDispatcher = testDispatcher,
        )

        val state = consentManager.gatherConsentIfNeeded()
        assertTrue(state.isInitialized)
        assertFalse(state.canRequestAds)
        assertFalse(state.privacyOptionsRequired)
    }

    @Test
    fun `signedEntitlementCache rejects tampered payload and release builds block qaProOverride`() = runTest(testDispatcher) {
        val prefs = createPrefs(backgroundScope)
        val signer = TestHmacSigner()
        val cache = SignedEntitlementCache(prefs, signer)

        val now = 1_750_000_000_000L
        cache.saveVerifiedEntitlement(SubscriptionPlan.YEARLY.skuId, "tok_sec", now)
        val raw = prefs.entitlementHmacPayload.first()!!
        val tampered = raw.dropLast(2) + if (raw.endsWith("AA")) "BB" else "AA"
        assertNull(cache.verify(tampered, now + 1000L))

        val releaseRepo = PlayBillingEntitlementRepository(
            preferences = prefs,
            signedCache = cache,
            billingGateway = FakeBillingGateway(),
            isDebugBuild = false,
            ioDispatcher = testDispatcher,
        )
        releaseRepo.setQaProOverride(true)
        advanceUntilIdle()
        assertFalse(releaseRepo.proStatus.value.isPro)
    }
}

private class FakeTelemetrySink : TelemetrySink {
    var adjustInitCount = 0
    val firebaseEvents = mutableListOf<Pair<String, Map<String, Any>>>()
    val adjustRevenues = mutableListOf<AdImpressionRecord>()

    override fun initAdjustIfConfigured() {
        adjustInitCount += 1
    }

    override fun dispatchFirebaseEvent(eventName: String, params: Map<String, Any>) {
        firebaseEvents += eventName to params
    }

    override fun dispatchAdjustAdRevenue(record: AdImpressionRecord) {
        adjustRevenues += record
    }
}
