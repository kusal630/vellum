package com.vellum.notes.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewportAnimatorTest {

    @Test
    fun spring_converges() {
        var x = 0f
        var v = 0f
        repeat(240) {
            val (nx, nv) = ViewportAnimator.springStep(x, 100f, v)
            x = nx
            v = nv
        }
        assertEquals(100f, x, 0.5f)
        assertTrue(ViewportAnimator.settled(ViewportAnimator.Viewport(1f, x, 50f), ViewportAnimator.Viewport(1f, 100f, 50f)))
    }

    @Test
    fun spring_noOvershoot_withHighDamping() {
        var x = 0f
        var v = 0f
        var max = 0f
        repeat(240) {
            val (nx, nv) = ViewportAnimator.springStep(x, 100f, v, stiffness = 120f, damping = 30f)
            x = nx
            v = nv
            if (x > max) max = x
        }
        assertTrue("overshoot to $max", max <= 101f)
    }

    @Test
    fun fit_centersContent() {
        val vp = ViewportAnimator.fitViewport(0f, 0f, 210f, 297f, 1000f, 800f, 10f)
        assertTrue(vp.zoom in 0.25f..8f)
        assertEquals(500f, 105f * 10f * vp.zoom + vp.offsetX, 1f)
        assertEquals(400f, 148.5f * 10f * vp.zoom + vp.offsetY, 1f)
    }

    @Test
    fun fit_clampsTinyContentToMaxZoom() {
        val vp = ViewportAnimator.fitViewport(0f, 0f, 1f, 1f, 1000f, 800f, 10f)
        assertEquals(8f, vp.zoom, 0.001f)
    }
}
