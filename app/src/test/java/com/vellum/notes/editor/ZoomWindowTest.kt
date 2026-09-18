package com.vellum.notes.editor

import com.vellum.notes.model.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomWindowTest {

    @Test
    fun windowRect_dockedBottom_withMargins() {
        val w = ZoomWindow.windowRect(1000f, 800f, 2f)
        assertTrue(w.left >= 20f && w.right <= 980f)
        assertTrue(w.bottom <= 800f - 20f)
        assertTrue(w.centerX == 500f)
        assertTrue(w.contains(500f, 700f))
        assertFalse(w.contains(500f, 100f))
    }

    @Test
    fun toWorld_centerMapsToFocus() {
        val win = ZoomWindow.WindowRect(0f, 500f, 1000f, 800f)
        val focus = Point(100f, 100f)
        val c = ZoomWindow.toWorld(win.centerX, win.centerY, win, focus, 1f)
        assertEquals(100f, c.x, 0.001f)
        assertEquals(100f, c.y, 0.001f)
    }

    @Test
    fun toWorld_roundTripsMagnification() {
        val win = ZoomWindow.WindowRect(0f, 500f, 1000f, 800f)
        val focus = Point(100f, 100f)
        val p = ZoomWindow.toWorld(win.centerX + 250f, win.centerY, win, focus, 2f, 2.5f)
        assertEquals(100f + 250f / 5f, p.x, 0.001f)
    }

    @Test
    fun worldClip_matchesWindowAspect() {
        val win = ZoomWindow.WindowRect(0f, 500f, 1000f, 800f)
        val clip = ZoomWindow.worldClipFor(Point(100f, 100f), win, 1f, 2.5f)
        assertEquals(1000f / 2.5f, clip.right - clip.left, 0.01f)
        assertEquals(300f / 2.5f, clip.bottom - clip.top, 0.01f)
        assertEquals(100f, (clip.left + clip.right) / 2f, 0.01f)
    }

    @Test
    fun focusFollowsTip_clamped() {
        val cur = Point(0f, 0f)
        assertEquals(Point(5f, 0f), ZoomWindow.focusFollowsTip(cur, Point(5f, 0f)))
        val jumped = ZoomWindow.focusFollowsTip(cur, Point(100f, 0f))
        assertEquals(40f, jumped.x, 0.01f)
        assertEquals(Point(7f, 7f), ZoomWindow.focusFollowsTip(null, Point(7f, 7f)))
    }
}
