package com.vellum.notes.editor

import com.vellum.notes.model.PageContent

object InkReflow {

    fun shiftBelow(content: PageContent, anchorY: Float, dy: Float): PageContent {
        if (dy == 0f) return content
        return content.copy(
            strokes = content.strokes.map { s ->
                if (strokeMinY(s.pointsPacked) >= anchorY) {
                    val pts = s.pointsPacked.copyOf()
                    var i = 1
                    while (i < pts.size) {
                        pts[i] += dy
                        i += 2
                    }
                    s.copy(pointsPacked = pts)
                } else s
            },
            shapeObjects = content.shapeObjects.map { sh ->
                if (sh.y >= anchorY) {
                    sh.copy(
                        y = sh.y + dy,
                        points = sh.points.map { it.copy(y = it.y + dy) },
                    )
                } else sh
            },
            textObjects = content.textObjects.map { t ->
                if (t.y >= anchorY) t.copy(y = t.y + dy) else t
            },
            imageObjects = content.imageObjects.map { im ->
                if (im.y >= anchorY) im.copy(y = im.y + dy) else im
            },
        )
    }

    fun strokeMinY(packed: FloatArray): Float {
        var m = Float.MAX_VALUE
        var i = 1
        while (i < packed.size) {
            if (packed[i] < m) m = packed[i]
            i += 2
        }
        return m
    }
}
