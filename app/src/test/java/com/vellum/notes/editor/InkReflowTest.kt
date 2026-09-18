package com.vellum.notes.editor

import com.vellum.notes.model.PageContent
import com.vellum.notes.model.PenStyle
import com.vellum.notes.model.Point
import com.vellum.notes.model.ShapeKind
import com.vellum.notes.model.ShapeObject
import com.vellum.notes.model.Stroke
import com.vellum.notes.model.TextObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InkReflowTest {

    private fun stroke(id: Long, y: Float): Stroke = Stroke(
        id = id, style = PenStyle(),
        pointsPacked = floatArrayOf(10f, y, 20f, y + 2f),
    )

    private fun content() = PageContent(
        strokes = listOf(stroke(1, 10f), stroke(2, 50f), stroke(3, 100f)),
        textObjects = listOf(TextObject(id = 1, x = 5f, y = 60f, width = 40f, height = 8f)),
        shapeObjects = listOf(
            ShapeObject(id = 1, kind = ShapeKind.RECT, points = listOf(Point(0f, 90f)), x = 0f, y = 90f),
        ),
    )

    @Test
    fun shiftsOnlyContentAtOrBelowAnchor() {
        val out = InkReflow.shiftBelow(content(), anchorY = 40f, dy = 20f)
        assertEquals(10f, out.strokes[0].pointsPacked[1], 0.001f)
        assertEquals(70f, out.strokes[1].pointsPacked[1], 0.001f)
        assertEquals(120f, out.strokes[2].pointsPacked[1], 0.001f)
        assertEquals(80f, out.textObjects[0].y, 0.001f)
        assertEquals(110f, out.shapeObjects[0].y, 0.001f)
        assertEquals(110f, out.shapeObjects[0].points[0].y, 0.001f)
    }

    @Test
    fun zeroDy_returnsSameContent() {
        val c = content()
        assertTrue(InkReflow.shiftBelow(c, 40f, 0f) === c)
    }

    @Test
    fun command_invertsExactly() {
        val c = content()
        val cmd = ReflowContentCommand(40f, 20f)
        val moved = cmd.apply(c)
        val back = cmd.invert().apply(moved)
        assertEquals(c.strokes.map { it.pointsPacked.toList() }, back.strokes.map { it.pointsPacked.toList() })
        assertEquals(c.textObjects.map { it.y }, back.textObjects.map { it.y })
    }
}
