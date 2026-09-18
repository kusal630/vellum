package com.vellum.notes.editor

import com.vellum.notes.model.Point
import com.vellum.notes.model.Stroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InkIndexerTest {

    @Test
    fun emptyStrokes_indexToEmpty() {
        assertEquals("", InkIndexer.indexStrokes(emptyList()))
    }

    @Test
    fun printedWord_HI_indexes() {
        val h = InkRecognizer.templateInput('H', dx = 0f, firstId = 1L)
        val i = InkRecognizer.templateInput('I', dx = 9f, firstId = 10L)
        val transcript = InkIndexer.indexStrokes(h + i)
        assertTrue("expected HI in '$transcript'", "HI" in transcript)
    }

    @Test
    fun twoLines_separatedByNewline() {
        val top = InkRecognizer.templateInput('H', dx = 0f, dy = 0f, firstId = 1L)
        val bottom = InkRecognizer.templateInput('I', dx = 0f, dy = 30f, firstId = 10L)
        val transcript = InkIndexer.indexStrokes(top + bottom)
        assertTrue("expected two lines in '$transcript'", "\n" in transcript)
    }

    @Test
    fun dotAlone_indexesToEmpty() {
        val dot = Stroke(id = 1L, style = com.vellum.notes.model.PenStyle(), pointsPacked = floatArrayOf(5f, 5f))
        assertEquals("", InkIndexer.indexStrokes(listOf(dot)))
    }

    @Test
    fun doodleScribble_doesNotPolluteIndex() {
        val zigzag = (0 until 12).flatMap { i ->
            listOf(Point(i * 2f, if (i % 2 == 0) 0f else 8f))
        }
        val flat = FloatArray(zigzag.size * 2)
        zigzag.forEachIndexed { i, p ->
            flat[i * 2] = p.x
            flat[i * 2 + 1] = p.y
        }
        val scribble = Stroke(id = 1L, style = com.vellum.notes.model.PenStyle(), pointsPacked = flat)
        val transcript = InkIndexer.indexStrokes(listOf(scribble))
        assertTrue("scribble must not index, got '$transcript'", transcript.all { !it.isLetterOrDigit() })
    }

    @Test
    fun sixtyWordPage_indexesWithinBudget() {
        val strokes = ArrayList<Stroke>()
        var id = 1L
        for (w in 0 until 60) {
            val dx = (w % 6) * 14f
            val dy = (w / 6) * 12f
            for (s in InkRecognizer.templateInput('A', dx = dx, dy = dy, firstId = id)) {
                strokes += s
                id += 10L
            }
        }
        val start = System.nanoTime()
        val transcript = InkIndexer.indexStrokes(strokes)
        val ms = (System.nanoTime() - start) / 1_000_000L
        assertTrue("expected A words, got '$transcript'", "A" in transcript)
        assertTrue("60-word index took ${ms}ms", ms < 300L)
    }

    @Test
    fun pointsHelper_unused_sanity() {
        assertEquals(Point(1f, 2f), Point(1f, 2f))
    }
}
