package com.vellum.notes.editor

import com.vellum.notes.input.SmoothingMode
import com.vellum.notes.input.StrokeSmoother
import com.vellum.notes.model.PenStyle
import com.vellum.notes.model.Point
import com.vellum.notes.model.Stroke
import kotlin.math.hypot

/**
 * Pure, per-stroke accumulator. Consumes writing-pointer samples (world coordinates,
 * millimeters), applies a dead-zone so a stationary pen does not make dots, runs the
 * active smoothing mode, and produces a committed [Stroke] on pen-up.
 *
 * The stroke is never built directly from raw events — this is the "noise filtering →
 * point smoothing → stroke construction" stage of the pipeline.
 */
class StrokeBuilder(
    val style: PenStyle,
    private val id: Long,
    private val deadZoneMm: Float = 0.5f,
) {
    private val smoother: StrokeSmoother = StrokeSmoother.create(style.smoothing)
    private val points = ArrayList<Point>()
    private var anchorX = 0f
    private var anchorY = 0f
    private var hasAnchor = false
    private var lastSampleX = 0f
    private var lastSampleY = 0f
    private var lastSampleTimeNanos = 0L
    private var lastSegmentDtNanos = 0L

    fun onDown(x: Float, y: Float) {
        smoother.reset()
        points.clear()
        hasAnchor = true
        lastSampleX = x
        lastSampleY = y
        lastSampleTimeNanos = 0L
        lastSegmentDtNanos = 0L
        anchorX = x
        anchorY = y
        points += Point(x, y)
    }

    /**
     * Adds a raw sample. Returns true if the stroke gained a point (so the caller can
     * decide to invalidate the canvas).
     */
    fun onMove(x: Float, y: Float, t: Long): Boolean {
        if (!hasAnchor) return false
        // Dead zone: ignore movement smaller than the anchor tolerance (in mm).
        if (hypot(x - lastSampleX, y - lastSampleY) < deadZoneMm) return false
        if (lastSampleTimeNanos > 0L && t > lastSampleTimeNanos) {
            lastSegmentDtNanos = t - lastSampleTimeNanos
        }
        lastSampleTimeNanos = t
        lastSampleX = x
        lastSampleY = y
        val smoothed = smoother.process(x, y, t)
        if (smoothed.isEmpty()) return false
        points += smoothed.map { Point(it[0], it[1]) }
        return true
    }

    /** Returns the committed stroke, or null if the stroke was too short to keep. */
    fun onUp(x: Float, y: Float, t: Long): Stroke? {
        if (!hasAnchor) return null
        // Snap the pen-up point exactly.
        if (points.isEmpty()) points += Point(x, y)
        else if (points.last() != Point(x, y)) {
            val flushed = smoother.flush()
            points += flushed.map { Point(it[0], it[1]) }
            if (points.last() != Point(x, y)) points += Point(x, y)
        }
        hasAnchor = false
        if (points.size < 2) return null
        // Commit-time thinning: RDP drops collinear samples the smoother added.
        // Endpoints are preserved; these points were already shown live.
        val thinned = StrokeResample.simplifyRdp(points, COMMIT_SIMPLIFY_EPSILON_MM)
        val kept = if (thinned.size >= 2) thinned else points
        return Stroke(
            id = id,
            style = style,
            pointsPacked = Stroke.pack(kept),
        )
    }

    fun onCancel() {
        hasAnchor = false
        smoother.reset()
        points.clear()
        lastSampleTimeNanos = 0L
        lastSegmentDtNanos = 0L
    }

    /**
     * Predicted live tip one frame ahead, for the translucent ghost segment
     * that hides input→photon latency. Null until two live points with valid
     * timing exist. Never affects the committed stroke.
     */
    fun predictedTip(horizonNanos: Long = 16_666_666L): Point? =
        StrokePredictor.predictTipFor(points, lastSegmentDtNanos, horizonNanos)

    companion object {
        /** RDP epsilon at commit (world mm): kills collinear runs, keeps corners. */
        const val COMMIT_SIMPLIFY_EPSILON_MM = 0.05f
    }

    /** Live point list for incremental rendering of the in-progress stroke. */
    val livePoints: List<Point> get() = points
}