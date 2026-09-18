package com.vellum.notes.packs

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RecordingAnchorTest {

    private fun repo(): PackRepository {
        val ctx: Context = RuntimeEnvironment.getApplication()
        return PackRepository(ctx.packsDataStore())
    }

    @Test
    fun anchor_defaultsToZero_roundTrips() = runBlocking {
        val repo = repo()
        assertEquals(0L, repo.getRecordingAnchor(7L))
        repo.setRecordingAnchor(7L, 123456789L)
        assertEquals(123456789L, repo.getRecordingAnchor(7L))
        assertEquals(0L, repo.getRecordingAnchor(8L))
    }
}
