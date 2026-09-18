package com.vellum.notes.editor

import com.vellum.notes.model.Point
import kotlin.math.hypot

object CircleSelect {

    data class Loop(val minX: Float, val minY: Float, val maxX: Float, val maxY: Float)

    fun analyze(points: List<Point>, durationMs: Long, maxDurationMs: Long = 1000L): Loop? {
        if (points.size < 8 || durationMs < 0L || durationMs > maxDurationMs) return null
        var length = 0f
        var l = Float.MAX_VALUE
        var t = Float.MAX_VALUE
        var r = -Float.MAX_VALUE
        var b = -Float.MAX_VALUE
        for (i in points.indices) {
            val p = points[i]
            if (p.x < l) l = p.x
            if (p.x > r) r = p.x
            if (p.y < t) t = p.y
            if (p.y > b) b = p.y
            if (i > 0) {
                length += hypot(
                    (p.x - points[i - 1].x).toDouble(),
                    (p.y - points[i - 1].y).toDouble(),
                ).toFloat()
            }
        }
        if (length < 20f) return null
        val w = r - l
        val h = b - t
        if (w < 8f || h < 8f) return null
        val closing = hypot(
            (points.last().x - points.first().x).toDouble(),
            (points.last().y - points.first().y).toDouble(),
        ).toFloat()
        if (closing > length * 0.15f) return null
        var area2 = 0.0
        for (i in points.indices) {
            val a = points[i]
            val c = points[(i + 1) % points.size]
            area2 += a.x.toDouble() * c.y - c.x.toDouble() * a.y
        }
        val area = kotlin.math.abs(area2) / 2.0
        if (area < 0.15 * w * h) return null
        return Loop(l, t, r, b)
    }
}
