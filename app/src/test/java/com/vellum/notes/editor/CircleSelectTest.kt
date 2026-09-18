package com.vellum.notes.editor

import com.vellum.notes.model.Point
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class CircleSelectTest {

    private fun circle(cx: Float, cy: Float, rad: Float, n: Int = 32, closeGap: Float = 0f): List<Point> {
        val pts = ArrayList<Point>(n)
        for (i in 0 until n) {
            val a = (i.toFloat() / n) * 6.2832f
            pts += Point(cx + cos(a) * rad, cy + sin(a) * rad)
        }
        if (closeGap == 0f) pts += Point(cx + rad, cy)
        return pts
    }

    @Test
    fun closedCircle_detected() {
        val loop = CircleSelect.analyze(circle(50f, 50f, 20f), 600L)
        assertNotNull(loop)
        assertTrue(loop!!.minX < 50f && loop.maxX > 50f)
    }

    @Test
    fun openStroke_rejected() {
        val line = List(20) { i -> Point(i * 2f, 50f) }
        assertNull(CircleSelect.analyze(line, 400L))
    }

    @Test
    fun slowLoop_rejected() {
        assertNull(CircleSelect.analyze(circle(50f, 50f, 20f), 5000L))
    }

    @Test
    fun tinyLoop_rejected() {
        assertNull(CircleSelect.analyze(circle(50f, 50f, 2f), 400L))
    }

    @Test
    fun scribbleZigzag_rejected() {
        val zig = (0 until 24).map { i -> Point(i * 2f, if (i % 2 == 0) 0f else 30f) }
        assertNull(CircleSelect.analyze(zig, 700L))
    }
}
