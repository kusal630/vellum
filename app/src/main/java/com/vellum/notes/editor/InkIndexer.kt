package com.vellum.notes.editor

import com.vellum.notes.model.Stroke

/**
 * Page-level handwriting index: turns raw ink into searchable transcript text
 * using only the bundled $1 recognizer — no downloads, no network, no new
 * permissions. Feeds [com.vellum.notes.data.SearchIndex].
 *
 * Pipeline: strokes → text lines (vertical gaps) → words (wide horizontal
 * gaps) → [InkRecognizer] per word → "line one\nline two". Words that
 * recognize to nothing but "?" (doodles, arrows, underlines) are dropped so
 * drawings never pollute search results.
 */
object InkIndexer {

    fun indexStrokes(strokes: List<Stroke>): String {
        if (strokes.isEmpty()) return ""
        return groupLines(strokes).mapNotNull { line ->
            indexLine(line).takeIf { it.isNotBlank() }
        }.joinToString("\n")
    }

    private data class Span(val minX: Float, val maxX: Float, val minY: Float, val maxY: Float)

    private fun spanOf(s: Stroke): Span? {
        val pts = s.pointsPacked
        if (pts.size < 4) return null
        var l = pts[0]
        var r = pts[0]
        var t = pts[1]
        var b = pts[1]
        var i = 2
        while (i + 1 < pts.size) {
            val x = pts[i]
            val y = pts[i + 1]
            if (x < l) l = x
            if (x > r) r = x
            if (y < t) t = y
            if (y > b) b = y
            i += 2
        }
        return Span(l, r, t, b)
    }

    private fun groupLines(strokes: List<Stroke>): List<List<Stroke>> {
        val items = strokes.mapNotNull { s -> spanOf(s)?.let { s to it } }
            .sortedBy { (_, span) -> (span.minY + span.maxY) / 2f }
        if (items.isEmpty()) return emptyList()
        val heights = items.map { (_, span) -> (span.maxY - span.minY).coerceAtLeast(0.5f) }.sorted()
        val medianH = heights[heights.size / 2]
        val lineGap = (medianH * 0.6f).coerceIn(1f, 12f)
        val lines = ArrayList<ArrayList<Stroke>>()
        var current = arrayListOf(items[0].first)
        var lineBottom = items[0].second.maxY
        for (k in 1 until items.size) {
            val (s, span) = items[k]
            if (span.minY - lineBottom > lineGap) {
                lines += current
                current = arrayListOf(s)
                lineBottom = span.maxY
            } else {
                current += s
                if (span.maxY > lineBottom) lineBottom = span.maxY
            }
        }
        lines += current
        return lines
    }

    private fun indexLine(line: List<Stroke>): String {
        val items = line.mapNotNull { s -> spanOf(s)?.let { s to it } }
            .sortedBy { (_, span) -> span.minX }
        if (items.isEmpty()) return ""
        val heights = items.map { (_, span) -> (span.maxY - span.minY).coerceAtLeast(0.5f) }.sorted()
        val medianH = heights[heights.size / 2]
        val wordGap = (medianH * 0.9f).coerceIn(2f, 16f)
        val words = ArrayList<ArrayList<Stroke>>()
        var current = arrayListOf(items[0].first)
        var wordRight = items[0].second.maxX
        for (k in 1 until items.size) {
            val (s, span) = items[k]
            if (span.minX - wordRight > wordGap) {
                words += current
                current = arrayListOf(s)
                wordRight = span.maxX
            } else {
                current += s
                if (span.maxX > wordRight) wordRight = span.maxX
            }
        }
        words += current
        return words.mapNotNull { word ->
            InkRecognizer.recognize(word)
                .filter { it.isLetterOrDigit() }
                .takeIf { it.isNotEmpty() }
        }.joinToString(" ")
    }
}
