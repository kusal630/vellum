package com.vellum.notes.editor

import com.vellum.notes.input.SmoothingMode
import com.vellum.notes.model.PenStyle
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeBuilderCommitTest {

    private fun builder() = StrokeBuilder(
        style = PenStyle(smoothing = SmoothingMode.NONE),
        id = 1L,
    )

    @Test
    fun straightStroke_commitsToEndpointsOnly() {
        val b = builder()
        b.onDown(0f, 0f)
        var t = 1_000_000L
        for (i in 1..100) {
            b.onMove(i.toFloat(), 0f, t)
            t += 8_000_000L
        }
        val stroke = b.onUp(100f, 0f, t)
        assertNotNull(stroke)
        assertTrue(
            "straight 100-sample run should thin to endpoints, got ${stroke!!.pointsPacked.size} floats",
            stroke.pointsPacked.size <= 4,
        )
    }

    @Test
    fun curvedStroke_keepsShape() {
        val b = builder()
        b.onDown(10f, 0f)
        var t = 1_000_000L
        for (i in 1..60) {
            val a = Math.PI * i / 60.0
            b.onMove((10 + 10 * Math.cos(a)).toFloat(), (10 * Math.sin(a)).toFloat(), t)
            t += 8_000_000L
        }
        val stroke = b.onUp(0f, 0f, t)
        assertNotNull(stroke)
        assertTrue("arc must keep shape points, got ${stroke!!.points.size}", stroke.points.size >= 6)
    }
}
