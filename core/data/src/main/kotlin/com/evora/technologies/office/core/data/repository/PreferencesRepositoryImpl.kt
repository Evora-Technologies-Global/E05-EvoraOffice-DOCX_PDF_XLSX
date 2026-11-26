package com.evora.technologies.office.core.data.repository

import com.evora.technologies.office.core.datastore.EvoraPreferencesDataSource
import com.evora.technologies.office.core.domain.port.PreferencesRepository
import com.evora.technologies.office.core.model.AppThemeMode
import com.evora.technologies.office.core.model.SortOrder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val dataSource: EvoraPreferencesDataSource,
) : PreferencesRepository {
    override val onboardingCompleted: Flow<Boolean> = dataSource.onboardingCompleted
    override val selectedLocaleTag: Flow<String?> = dataSource.selectedLocaleTag
    override val themeMode: Flow<AppThemeMode> = dataSource.themeMode
    override val defaultSortOrder: Flow<SortOrder> = dataSource.defaultSortOrder
    override val autosaveIntervalSec: Flow<Int> = dataSource.autosaveIntervalSec
    override val defaultAppPromptDismissed: Flow<Boolean> = dataSource.defaultAppPromptDismissed
    override val externalOpenCount: Flow<Int> = dataSource.externalOpenCount

    override suspend fun setOnboardingCompleted(completed: Boolean) =
        dataSource.setOnboardingCompleted(completed)

    override suspend fun setSelectedLocaleTag(tag: String) =
        dataSource.setSelectedLocaleTag(tag)

    override suspend fun setThemeMode(mode: AppThemeMode) =
        dataSource.setThemeMode(mode)

    override suspend fun setDefaultSortOrder(sortOrder: SortOrder) =
        dataSource.setDefaultSortOrder(sortOrder)

    override suspend fun setAutosaveIntervalSec(seconds: Int) =
        dataSource.setAutosaveIntervalSec(seconds)

    override suspend fun setDefaultAppPromptDismissed(dismissed: Boolean) =
        dataSource.setDefaultAppPromptDismissed(dismissed)

    override suspend fun incrementExternalOpenCount() =
        dataSource.incrementExternalOpenCount()
}
