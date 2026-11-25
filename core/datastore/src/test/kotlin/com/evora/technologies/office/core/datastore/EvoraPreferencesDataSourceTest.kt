package com.evora.technologies.office.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.evora.technologies.office.core.model.AppThemeMode
import com.evora.technologies.office.core.model.SortOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EvoraPreferencesDataSourceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `preferences round trip all sprint 1 keys and defaults`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = TestScope(UnconfinedTestDispatcher(testScheduler)),
            produceFile = { tempFolder.newFile("evora_prefs_test.preferences_pb") },
        )
        val source = EvoraPreferencesDataSource(dataStore)

        assertFalse(source.onboardingCompleted.first())
        assertNull(source.selectedLocaleTag.first())
        assertEquals(AppThemeMode.SYSTEM, source.themeMode.first())
        assertEquals(SortOrder.MODIFIED_DESC, source.defaultSortOrder.first())
        assertEquals(15, source.autosaveIntervalSec.first())

        source.setOnboardingCompleted(true)
        source.setSelectedLocaleTag("vi")
        source.setThemeMode(AppThemeMode.DARK)
        source.setDefaultSortOrder(SortOrder.NAME_ASC)
        source.incrementExternalOpenCount()
        source.incrementExternalOpenCount()

        assertTrue(source.onboardingCompleted.first())
        assertEquals("vi", source.selectedLocaleTag.first())
        assertEquals(AppThemeMode.DARK, source.themeMode.first())
        assertEquals(SortOrder.NAME_ASC, source.defaultSortOrder.first())
        assertEquals(2, source.externalOpenCount.first())
    }
}
