package com.vellum.notes.data

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * BUG 2 regression: the first-run palm-zone coachmark must show once per install.
 * The [com.vellum.notes.input.PalmRejectionSettings.showPalmZoneCoachmark] flag defaults
 * to true, persists dismissal, and survives unrelated settings writes.
 */
@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryCoachmarkTest {

    private fun repo(): SettingsRepository {
        val ctx: Context = RuntimeEnvironment.getApplication()
        return SettingsRepository(ctx.settingsDataStore())
    }

    @Before
    fun clearPrefs() = runBlocking {
        repo().resetToDefaults()
    }

    @Test
    fun coachmark_showsByDefaultOnFreshInstall() = runBlocking {
        assertTrue(repo().settingsFlow.first().showPalmZoneCoachmark)
    }

    @Test
    fun coachmark_dismissalPersists() = runBlocking {
        val repo = repo()
        repo.updateSettings { showPalmZoneCoachmark = false }
        assertFalse(repo.settingsFlow.first().showPalmZoneCoachmark)
    }

    @Test
    fun coachmark_unrelatedWritesPreserveDismissal() = runBlocking {
        val repo = repo()
        repo.updateSettings { showPalmZoneCoachmark = false }
        repo.updateSettings { smoothing = com.vellum.notes.input.SmoothingMode.HIGH }
        repo.updateSettings { autoEraseEnabled = true }
        assertFalse(repo.settingsFlow.first().showPalmZoneCoachmark)
    }

    @Test
    fun coachmark_unrelatedWritesDoNotDismiss() = runBlocking {
        val repo = repo()
        assertTrue(repo.settingsFlow.first().showPalmZoneCoachmark)
        repo.updateSettings { smoothing = com.vellum.notes.input.SmoothingMode.NONE }
        assertTrue(repo.settingsFlow.first().showPalmZoneCoachmark)
    }
}
