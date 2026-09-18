package com.vellum.notes.editor

import com.vellum.notes.model.Point

/**
 * One-frame tip extrapolation for the live-stroke ghost segment.
 *
 * Rendering the predicted tip as a translucent extension hides ~1 frame of
 * input→photon latency: the eye sees continuous motion at 60Hz instead of a
 * tip that lags the pen. The committed stroke is untouched — the ghost is
 * replaced by real samples on the next frame.
 *
 * Velocity is clamped ([maxStepMm]) so a fast flick followed by a stop cannot
 * overshoot into a visible spike. Pure Kotlin, JVM-testable.
 */
object StrokePredictor {

    /**
     * Extrapolates from [prev] → [last] over [dtNanos] to [horizonNanos] ahead.
     * Returns null when the segment carries no usable timing (dt <= 0).
     * A stationary pen predicts the tip exactly (zero velocity → zero step).
     */
    fun predictTip(
        last: Point,
        prev: Point,
        dtNanos: Long,
        horizonNanos: Long = 16_666_666L,
        maxStepMm: Float = 2f,
    ): Point? {
        if (dtNanos <= 0L || horizonNanos < 0L || maxStepMm < 0f) return null
        if (horizonNanos == 0L) return last
        val vx = (last.x - prev.x).toDouble() / dtNanos
        val vy = (last.y - prev.y).toDouble() / dtNanos
        val dx = (vx * horizonNanos).toFloat().coerceIn(-maxStepMm, maxStepMm)
        val dy = (vy * horizonNanos).toFloat().coerceIn(-maxStepMm, maxStepMm)
        return Point(last.x + dx, last.y + dy)
    }

    /** Convenience over the tail of a live point list; null when < 2 points. */
    fun predictTipFor(
        points: List<Point>,
        dtNanos: Long,
        horizonNanos: Long = 16_666_666L,
        maxStepMm: Float = 2f,
    ): Point? {
        if (points.size < 2) return null
        return predictTip(points.last(), points[points.size - 2], dtNanos, horizonNanos, maxStepMm)
    }
}
