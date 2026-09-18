package com.vellum.notes.editor

import com.vellum.notes.model.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot
import kotlin.random.Random

class StrokeResamplePropertyTest {

    private fun randomWalk(seed: Long, n: Int, step: Float): List<Point> {
        val rnd = Random(seed)
        val pts = ArrayList<Point>(n)
        var x = 0f
        var y = 0f
        repeat(n) {
            pts += Point(x, y)
            val a = rnd.nextFloat() * 6.2832f
            val s = step * (0.5f + rnd.nextFloat())
            x += kotlin.math.cos(a) * s
            y += kotlin.math.sin(a) * s
        }
        return pts
    }

    @Test
    fun rdp_preservesEndpoints_andOnlyDropsPoints() {
        repeat(50) { seed ->
            val pts = randomWalk(seed.toLong(), 64, 1f)
            val out = StrokeResample.simplifyRdp(pts, 0.4f)
            assertEquals(pts.first(), out.first())
            assertEquals(pts.last(), out.last())
            assertTrue(out.all { it in pts })
            assertTrue(out.size in 2..pts.size)
        }
    }

    @Test
    fun rdp_isIdempotent() {
        repeat(20) { seed ->
            val pts = randomWalk(1000L + seed, 96, 1f)
            val once = StrokeResample.simplifyRdp(pts, 0.3f)
            assertEquals(once, StrokeResample.simplifyRdp(once, 0.3f))
        }
    }

    @Test
    fun resample_spacingBound_holds() {
        val step = 1f
        val spacing = 1f
        repeat(20) { seed ->
            val pts = randomWalk(2000L + seed, 80, step)
            val out = StrokeResample.resampleUniform(pts, spacing)
            assertEquals(pts.first(), out.first())
            assertEquals(pts.last(), out.last())
            for (i in 1 until out.size) {
                val d = hypot(
                    (out[i].x - out[i - 1].x).toDouble(),
                    (out[i].y - out[i - 1].y).toDouble(),
                )
                assertTrue("gap $d exceeds spacing+step", d <= spacing + 1.5 * step + 1e-3)
            }
        }
    }

    @Test
    fun predictor_stepBounded_andFinite_onRandomInput() {
        val rnd = Random(42L)
        repeat(500) {
            val ax = rnd.nextFloat() * 2000f - 1000f
            val ay = rnd.nextFloat() * 2000f - 1000f
            val bx = rnd.nextFloat() * 2000f - 1000f
            val by = rnd.nextFloat() * 2000f - 1000f
            val dt = rnd.nextLong(1L, 100_000_000L)
            val tip = StrokePredictor.predictTip(Point(bx, by), Point(ax, ay), dt)!!
            assertTrue(tip.x.isFinite() && tip.y.isFinite())
            val step = hypot((tip.x - bx).toDouble(), (tip.y - by).toDouble())
            assertTrue(step <= 2f * 1.4143f + 1e-3f)
        }
    }
}
