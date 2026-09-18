package com.vellum.notes.editor

import com.vellum.notes.model.Point
import kotlin.math.hypot

object ZoomWindow {

    const val DEFAULT_ZOOM = 2.5f

    data class WindowRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        val centerX: Float get() = (left + right) / 2f
        val centerY: Float get() = (top + bottom) / 2f
        fun contains(x: Float, y: Float): Boolean = x >= left && x <= right && y >= top && y <= bottom
    }

    fun windowRect(viewW: Float, viewH: Float, density: Float): WindowRect {
        val h = (260f * density).coerceAtMost(viewH * 0.45f)
        val m = 12f * density
        return WindowRect(m, viewH - h - m, viewW - m, viewH - m)
    }

    data class WorldClip(val left: Float, val top: Float, val right: Float, val bottom: Float)

    fun worldClipFor(
        focus: Point,
        window: WindowRect,
        baseScale: Float,
        zoom: Float = DEFAULT_ZOOM,
    ): WorldClip {
        val halfW = (window.right - window.left) / 2f / (baseScale * zoom)
        val halfH = (window.bottom - window.top) / 2f / (baseScale * zoom)
        return WorldClip(
            focus.x - halfW, focus.y - halfH, focus.x + halfW, focus.y + halfH,
        )
    }

    fun toWorld(
        screenX: Float,
        screenY: Float,
        window: WindowRect,
        focus: Point,
        baseScale: Float,
        zoom: Float = DEFAULT_ZOOM,
    ): Point {
        val eff = baseScale * zoom
        return Point(
            focus.x + (screenX - window.centerX) / eff,
            focus.y + (screenY - window.centerY) / eff,
        )
    }

    fun focusFollowsTip(current: Point?, tip: Point, maxJumpMm: Float = 40f): Point {
        if (current == null) return tip
        val d = hypot((tip.x - current.x).toDouble(), (tip.y - current.y).toDouble()).toFloat()
        if (d <= maxJumpMm) return tip
        val t = maxJumpMm / d
        return Point(current.x + (tip.x - current.x) * t, current.y + (tip.y - current.y) * t)
    }
}
