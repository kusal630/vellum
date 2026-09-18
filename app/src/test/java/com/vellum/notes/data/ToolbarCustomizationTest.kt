package com.vellum.notes.data

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ToolbarCustomizationTest {

    private fun repo(): SettingsRepository {
        val ctx: Context = RuntimeEnvironment.getApplication()
        return SettingsRepository(ctx.settingsDataStore())
    }

    @Before
    fun clearPrefs() = runBlocking {
        repo().resetToDefaults()
    }

    @Test
    fun hiddenDefaultsToEmpty() = runBlocking {
        assertTrue(repo().toolbarHiddenFlow.first().isEmpty())
    }

    @Test
    fun hiddenRoundTrips() = runBlocking {
        val repo = repo()
        repo.setToolbarHidden(setOf("Template", "Auto-erase"))
        assertEquals(setOf("Template", "Auto-erase"), repo.toolbarHiddenFlow.first())
        repo.setToolbarHidden(emptySet())
        assertTrue(repo.toolbarHiddenFlow.first().isEmpty())
    }

    @Test
    fun hiddenSurvivesUnrelatedWrites() = runBlocking {
        val repo = repo()
        repo.setToolbarHidden(setOf("Image"))
        repo.updateSettings { autoEraseEnabled = true }
        assertEquals(setOf("Image"), repo.toolbarHiddenFlow.first())
    }
}
