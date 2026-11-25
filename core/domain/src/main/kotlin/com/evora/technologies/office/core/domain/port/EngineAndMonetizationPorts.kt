package com.evora.technologies.office.core.domain.port

import com.evora.technologies.office.core.model.ProFeature
import com.evora.technologies.office.core.model.ProStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstracts subscription, lifetime unlock, and 24h rewarded pass entitlements.
 * Backed by a local offline stub in Sprints 1–3 and Play Billing 8.0.0 in Sprint 4 (CD-07).
 */
interface ProEntitlementPort {
    val proStatus: StateFlow<ProStatus>
    suspend fun hasAccess(feature: ProFeature): Boolean
    suspend fun grantRewardedPass(hours: Int)
    suspend fun restorePurchases(): Boolean
}

/**
 * Hook for post-milestone and home-tab ad placements.
 * Bound to a strict no-op until core features F-01..F-09 reach Done (CD-07).
 */
interface AdPlacementHook {
    fun onMilestoneCompleted(screenId: String)
    fun onHomeHubEntered()
}

interface PdfEnginePort {
    suspend fun getPageCount(uriString: String, password: String? = null): Int
}

interface OfficeEnginePort {
    fun isNativeEngineAvailable(): Boolean
}
