package com.vellum.notes.editor

import com.vellum.notes.model.PenStyle
import com.vellum.notes.model.Stroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeReplayTest {

    private fun stroke(id: Long, at: Long) = Stroke(
        id = id, style = PenStyle(),
        pointsPacked = floatArrayOf(0f, 0f, 10f, 10f), createdAtMs = at,
    )

    @Test
    fun nullCutoff_showsEverything() {
        assertTrue(StrokeReplay.visibleInReplay(5000L, null))
        assertTrue(StrokeReplay.visibleInReplay(0L, null))
    }

    @Test
    fun legacyZero_alwaysShown() {
        assertTrue(StrokeReplay.visibleInReplay(0L, 1000L))
    }

    @Test
    fun cutoffHidesFuture_revealsPast() {
        assertTrue(StrokeReplay.visibleInReplay(1000L, 2000L))
        assertTrue(StrokeReplay.visibleInReplay(2000L, 2000L))
        assertFalse(StrokeReplay.visibleInReplay(2001L, 2000L))
    }

    @Test
    fun rangeSkipsLegacy() {
        assertNull(StrokeReplay.replayRange(listOf(stroke(1, 0L))))
        assertEquals(1000L to 3000L, StrokeReplay.replayRange(listOf(stroke(1, 0L), stroke(2, 3000L), stroke(3, 1000L))))
    }

    @Test
    fun cutoffForPlayback_addsElapsedToAnchor() {
        assertEquals(65000L, StrokeReplay.cutoffForPlayback(60000L, 5000L))
        assertEquals(60000L, StrokeReplay.cutoffForPlayback(60000L, 0L))
    }

    @Test
    fun cutoffForPlayback_missingAnchorOrNegative_returnsNull() {
        assertNull(StrokeReplay.cutoffForPlayback(0L, 5000L))
        assertNull(StrokeReplay.cutoffForPlayback(-1L, 5000L))
        assertNull(StrokeReplay.cutoffForPlayback(60000L, -1L))
    }
}
