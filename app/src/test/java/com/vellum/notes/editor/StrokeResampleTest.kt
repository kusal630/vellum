package com.vellum.notes.editor

import com.vellum.notes.model.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeResampleTest {

    private fun line(n: Int, step: Float = 1f) =
        List(n) { i -> Point(i * step, 0.05f * i) }

    @Test
    fun resample_keepsEndpoints_andThinsDenseInput() {
        val pts = line(101)
        val out = StrokeResample.resampleUniform(pts, 10f)
        assertEquals(pts.first(), out.first())
        assertEquals(pts.last(), out.last())
        assertTrue("expected thinning, got ${out.size}", out.size < pts.size)
    }

    @Test
    fun resample_shortInput_returnedAsIs() {
        assertEquals(listOf(Point(1f, 2f)), StrokeResample.resampleUniform(listOf(Point(1f, 2f)), 5f))
        assertTrue(StrokeResample.resampleUniform(emptyList(), 5f).isEmpty())
    }

    @Test
    fun rdp_collapsesStraightRuns_keepsCorners() {
        val straight = line(21)
        val thin = StrokeResample.simplifyRdp(straight, 0.05f)
        assertTrue("straight run should collapse, got ${thin.size}", thin.size <= 4)
        assertEquals(straight.first(), thin.first())
        assertEquals(straight.last(), thin.last())

        val corner = listOf(Point(0f, 0f), Point(10f, 0f), Point(10f, 10f))
        assertEquals(3, StrokeResample.simplifyRdp(corner, 0.05f).size)
    }

    @Test
    fun rdp_neverDropsBelowTwoPoints() {
        val pts = listOf(Point(0f, 0f), Point(0.01f, 0.01f))
        assertEquals(2, StrokeResample.simplifyRdp(pts, 5f).size)
    }

    @Test
    fun commitPipeline_budget_512points() {
        val pts = List(512) { i -> Point(i * 0.2f, (i % 7) * 0.1f) }
        val start = System.nanoTime()
        val out = StrokeResample.simplifyRdp(StrokeResample.resampleUniform(pts, 0.3f), 0.05f)
        val ms = (System.nanoTime() - start) / 1_000_000L
        assertTrue("pipeline output empty", out.size >= 2)
        assertTrue("commit thinning took ${ms}ms", ms < 50L)
    }
}
