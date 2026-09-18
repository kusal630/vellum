package com.vellum.notes.editor

import com.vellum.notes.model.Point
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Commit-time stroke thinning: uniform resampling + Ramer–Douglas–Peucker
 * simplification, in world millimeters.
 *
 * Smoothing modes (especially Catmull-Rom) can triple the stored point count;
 * a straight 100mm run keeps hundreds of collinear samples that cost database
 * bytes and per-frame path work forever. Thinning runs once at pen-up —
 * never on the live path — so it must preserve endpoints exactly.
 *
 * Pure Kotlin, no Android dependencies: covered by JVM unit tests.
 */
object StrokeResample {

    /**
     * Resamples [points] to roughly uniform [spacingMm]. Endpoints are always
     * kept. Returns a copy; input shorter than 2 points is returned as-is.
     */
    fun resampleUniform(points: List<Point>, spacingMm: Float): List<Point> {
        if (points.size < 2 || spacingMm <= 0f) return points.toList()
        val out = ArrayList<Point>(points.size)
        out += points.first()
        var acc = 0f
        for (i in 1 until points.size) {
            acc += dist(points[i], points[i - 1])
            if (acc >= spacingMm) {
                out += points[i]
                acc = 0f
            }
        }
        if (out.last() != points.last()) out += points.last()
        return out
    }

    /**
     * Ramer–Douglas–Peucker simplification: drops points whose perpendicular
     * distance to the current segment is within [epsilonMm]. Iterative
     * (no recursion) so hostile inputs cannot overflow the stack.
     */
    fun simplifyRdp(points: List<Point>, epsilonMm: Float): List<Point> {
        if (points.size < 3 || epsilonMm <= 0f) return points.toList()
        val keep = BooleanArray(points.size)
        keep[0] = true
        keep[points.size - 1] = true
        val stack = ArrayDeque<IntArray>()
        stack.addLast(intArrayOf(0, points.size - 1))
        while (stack.isNotEmpty()) {
            val seg = stack.removeLast()
            val s = seg[0]
            val e = seg[1]
            var dMax = 0f
            var idx = s
            for (i in s + 1 until e) {
                val d = perpDist(points[i], points[s], points[e])
                if (d > dMax) {
                    dMax = d
                    idx = i
                }
            }
            if (dMax > epsilonMm) {
                keep[idx] = true
                stack.addLast(intArrayOf(s, idx))
                stack.addLast(intArrayOf(idx, e))
            }
        }
        return points.filterIndexed { i, _ -> keep[i] }
    }

    private fun dist(a: Point, b: Point): Float {
        val dx = (a.x - b.x).toDouble()
        val dy = (a.y - b.y).toDouble()
        return sqrt(dx * dx + dy * dy).toFloat()
    }

    private fun perpDist(p: Point, a: Point, b: Point): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len2 = dx * dx + dy * dy
        if (len2 == 0f) return hypot((p.x - a.x).toDouble(), (p.y - a.y).toDouble()).toFloat()
        val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / len2).coerceIn(0f, 1f)
        val cx = a.x + t * dx
        val cy = a.y + t * dy
        return hypot((p.x - cx).toDouble(), (p.y - cy).toDouble()).toFloat()
    }
}
