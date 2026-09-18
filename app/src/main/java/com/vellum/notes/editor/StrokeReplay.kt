package com.vellum.notes.editor

import com.vellum.notes.model.Stroke

object StrokeReplay {

    fun visibleInReplay(createdAtMs: Long, cutoffMs: Long?): Boolean {
        if (cutoffMs == null) return true
        if (createdAtMs <= 0L) return true
        return createdAtMs <= cutoffMs
    }

    fun replayRange(strokes: List<Stroke>): Pair<Long, Long>? {
        var min = Long.MAX_VALUE
        var max = Long.MIN_VALUE
        for (s in strokes) {
            if (s.createdAtMs <= 0L) continue
            if (s.createdAtMs < min) min = s.createdAtMs
            if (s.createdAtMs > max) max = s.createdAtMs
        }
        if (min == Long.MAX_VALUE) return null
        return min to max
    }
}
