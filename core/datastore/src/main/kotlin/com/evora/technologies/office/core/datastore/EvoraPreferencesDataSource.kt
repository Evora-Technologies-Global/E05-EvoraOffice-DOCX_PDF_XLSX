package com.evora.technologies.office.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.evora.technologies.office.core.model.AppThemeMode
import com.evora.technologies.office.core.model.SortOrder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvoraPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val selectedLocaleTag = stringPreferencesKey("selected_locale_tag")
        val themeMode = stringPreferencesKey("theme_mode")
        val defaultSortOrder = stringPreferencesKey("default_sort_order")
        val autosaveIntervalSec = intPreferencesKey("autosave_interval_sec")
        val rewardPassExpiresAtMs = longPreferencesKey("reward_pass_expires_at_ms")
        val qaProOverride = booleanPreferencesKey("qa_pro_override")
        val entitlementHmacPayload = stringPreferencesKey("entitlement_hmac_payload")
        val defaultAppPromptDismissed = booleanPreferencesKey("default_prompt_dismissed")
        val externalOpenCount = intPreferencesKey("external_open_count")
    }

    val onboardingCompleted: Flow<Boolean> =
        dataStore.data.map { it[Keys.onboardingCompleted] ?: false }

    val selectedLocaleTag: Flow<String?> =
        dataStore.data.map { it[Keys.selectedLocaleTag] }

    val themeMode: Flow<AppThemeMode> =
        dataStore.data.map { prefs ->
            val raw = prefs[Keys.themeMode]
            AppThemeMode.entries.firstOrNull { it.name == raw } ?: AppThemeMode.SYSTEM
        }

    val defaultSortOrder: Flow<SortOrder> =
        dataStore.data.map { prefs ->
            val raw = prefs[Keys.defaultSortOrder]
            SortOrder.entries.firstOrNull { it.name == raw } ?: SortOrder.MODIFIED_DESC
        }

    val autosaveIntervalSec: Flow<Int> =
        dataStore.data.map { it[Keys.autosaveIntervalSec] ?: 15 }

    val rewardPassExpiresAtEpochMs: Flow<Long> =
        dataStore.data.map { it[Keys.rewardPassExpiresAtMs] ?: 0L }

    val qaProOverride: Flow<Boolean> =
        dataStore.data.map { it[Keys.qaProOverride] ?: false }

    val entitlementHmacPayload: Flow<String?> =
        dataStore.data.map { it[Keys.entitlementHmacPayload] }

    val defaultAppPromptDismissed: Flow<Boolean> =
        dataStore.data.map { it[Keys.defaultAppPromptDismissed] ?: false }

    val externalOpenCount: Flow<Int> =
        dataStore.data.map { it[Keys.externalOpenCount] ?: 0 }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.onboardingCompleted] = completed }
    }

    suspend fun setSelectedLocaleTag(tag: String) {
        dataStore.edit { it[Keys.selectedLocaleTag] = tag }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { it[Keys.themeMode] = mode.name }
    }

    suspend fun setDefaultSortOrder(sortOrder: SortOrder) {
        dataStore.edit { it[Keys.defaultSortOrder] = sortOrder.name }
    }

    suspend fun setAutosaveIntervalSec(seconds: Int) {
        dataStore.edit { it[Keys.autosaveIntervalSec] = seconds.coerceIn(5, 300) }
    }

    suspend fun setRewardPassExpiresAt(epochMs: Long) {
        dataStore.edit { it[Keys.rewardPassExpiresAtMs] = epochMs }
    }

    suspend fun setQaProOverride(enabled: Boolean) {
        dataStore.edit { it[Keys.qaProOverride] = enabled }
    }

    suspend fun setEntitlementHmacPayload(payload: String?) {
        dataStore.edit { prefs ->
            if (payload == null) {
                prefs.remove(Keys.entitlementHmacPayload)
            } else {
                prefs[Keys.entitlementHmacPayload] = payload
            }
        }
    }

    suspend fun setDefaultAppPromptDismissed(dismissed: Boolean) {
        dataStore.edit { it[Keys.defaultAppPromptDismissed] = dismissed }
    }

    suspend fun incrementExternalOpenCount() {
        dataStore.edit { prefs ->
            val current = prefs[Keys.externalOpenCount] ?: 0
            prefs[Keys.externalOpenCount] = current + 1
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        produceFile = { context.preferencesDataStoreFile("evora_prefs") },
    )
}
