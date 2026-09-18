package com.vellum.notes.editor

import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs

object ViewportAnimator {

    data class Viewport(val zoom: Float, val offsetX: Float, val offsetY: Float)

    fun springStep(
        current: Float,
        target: Float,
        velocity: Float,
        stiffness: Float = 180f,
        damping: Float = 22f,
        dt: Float = 1f / 60f,
    ): Pair<Float, Float> {
        val force = -stiffness * (current - target) - damping * velocity
        val v = velocity + force * dt
        return (current + v * dt) to v
    }

    fun settled(current: Viewport, target: Viewport, eps: Float = 0.5f): Boolean =
        abs(current.zoom - target.zoom) < 0.001f &&
            abs(current.offsetX - target.offsetX) < eps &&
            abs(current.offsetY - target.offsetY) < eps

    fun fitViewport(
        minX: Float,
        minY: Float,
        maxX: Float,
        maxY: Float,
        viewW: Float,
        viewH: Float,
        pxPerMm: Float,
        minZoom: Float = 0.25f,
        maxZoom: Float = 8f,
        paddingPx: Float = 32f,
    ): Viewport {
        val w = (maxX - minX).coerceAtLeast(1f) * pxPerMm
        val h = (maxY - minY).coerceAtLeast(1f) * pxPerMm
        val zoom = ((viewW - paddingPx * 2f) / w)
            .coerceAtMost((viewH - paddingPx * 2f) / h)
            .coerceIn(minZoom, maxZoom)
        val cxMm = (minX + maxX) / 2f
        val cyMm = (minY + maxY) / 2f
        val scale = pxPerMm * zoom
        return Viewport(
            zoom = zoom,
            offsetX = viewW / 2f - cxMm * scale,
            offsetY = viewH / 2f - cyMm * scale,
        )
    }

    fun animate(
        from: Viewport,
        to: Viewport,
        durationMs: Long = 280L,
        onUpdate: (Viewport) -> Unit,
    ): ValueAnimator {
        val anim = ValueAnimator.ofFloat(0f, 1f)
        anim.duration = durationMs
        anim.interpolator = DecelerateInterpolator(1.5f)
        anim.addUpdateListener { v ->
            val t = v.animatedValue as Float
            onUpdate(
                Viewport(
                    zoom = from.zoom + (to.zoom - from.zoom) * t,
                    offsetX = from.offsetX + (to.offsetX - from.offsetX) * t,
                    offsetY = from.offsetY + (to.offsetY - from.offsetY) * t,
                ),
            )
        }
        return anim
    }
}
