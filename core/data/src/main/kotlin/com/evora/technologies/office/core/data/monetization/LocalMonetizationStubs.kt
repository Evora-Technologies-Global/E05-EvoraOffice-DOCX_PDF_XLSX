package com.evora.technologies.office.core.data.monetization

import com.evora.technologies.office.core.datastore.EvoraPreferencesDataSource
import com.evora.technologies.office.core.domain.port.AdPlacementHook
import com.evora.technologies.office.core.domain.port.ProEntitlementPort
import com.evora.technologies.office.core.model.ProFeature
import com.evora.technologies.office.core.model.ProStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local offline entitlement implementation active during Sprints 1–3 before
 * BillingClient 8.0.0 activation in Sprint 4 (CD-07).
 */
@Singleton
class LocalProEntitlementRepository @Inject constructor(
    private val preferences: EvoraPreferencesDataSource,
) : ProEntitlementPort {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val proStatus: StateFlow<ProStatus> = preferences.rewardPassExpiresAtEpochMs
        .map { expiresAt ->
            val activeReward = expiresAt > System.currentTimeMillis()
            ProStatus(
                isPro = false,
                hasRewardedPass = activeReward,
                rewardPassExpiresAtEpochMs = expiresAt,
            )
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProStatus(),
        )

    override suspend fun hasAccess(feature: ProFeature): Boolean = proStatus.value.isUnlocked

    override suspend fun grantRewardedPass(hours: Int) {
        val expiresAt = System.currentTimeMillis() + hours * 3_600_000L
        preferences.setRewardPassExpiresAt(expiresAt)
    }

    override suspend fun restorePurchases(): Boolean = false
}

/**
 * Strict no-op ad placement hook active until F-01..F-09 reach Done (CD-07).
 */
@Singleton
class NoOpAdPlacementHook @Inject constructor() : AdPlacementHook {
    override fun onMilestoneCompleted(screenId: String) = Unit
    override fun onHomeHubEntered() = Unit
}
