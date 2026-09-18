package com.vellum.notes.editor

import com.vellum.notes.model.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokePredictorTest {

    @Test
    fun predictsOneFrameAhead_alongVelocity() {
        val tip = StrokePredictor.predictTip(
            last = Point(1f, 0f), prev = Point(0f, 0f),
            dtNanos = 16_666_666L, horizonNanos = 16_666_666L,
        )!!
        assertEquals(2f, tip.x, 0.01f)
        assertEquals(0f, tip.y, 0.01f)
    }

    @Test
    fun clampsWildExtrapolation_toMaxStep() {
        val tip = StrokePredictor.predictTip(
            last = Point(0f, 0f), prev = Point(-100f, 0f),
            dtNanos = 1_000_000L, horizonNanos = 16_666_666L, maxStepMm = 2f,
        )!!
        assertTrue("overshoot not clamped: $tip", tip.x <= 2f)
    }

    @Test
    fun stationaryPen_predictsExactTip() {
        val p = Point(5f, 5f)
        assertEquals(p, StrokePredictor.predictTip(p, p, 16_666_666L))
    }

    @Test
    fun invalidTiming_returnsNull() {
        assertNull(StrokePredictor.predictTip(Point(1f, 1f), Point(0f, 0f), 0L))
        assertNull(StrokePredictor.predictTip(Point(1f, 1f), Point(0f, 0f), -5L))
    }

    @Test
    fun listHelper_needsTwoPoints() {
        assertNull(StrokePredictor.predictTipFor(listOf(Point(1f, 1f)), 16_666_666L))
        val tip = StrokePredictor.predictTipFor(
            listOf(Point(0f, 0f), Point(1f, 0f)), 16_666_666L,
        )!!
        assertEquals(2f, tip.x, 0.01f)
    }
}
