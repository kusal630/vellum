package com.vellum.notes.data

import com.vellum.notes.editor.InkIndexer
import com.vellum.notes.model.PageContent

/**
 * Full-text index support (tags + FTS workstream).
 *
 * [bodyFor] flattens everything searchable inside a page into one string: typed
 * text boxes, classroom transcripts, summaries, and recognized handwriting.
 * Ink is transcribed on-device by [InkIndexer] (bundled $1 recognizer, no
 * downloads) at index time; the sync protocol is unchanged.
 *
 * [sanitizeQuery] turns raw user input into a safe FTS4 MATCH expression:
 * tokens are quoted and space-joined (implicit AND) so punctuation can never
 * break the syntax. Implicit AND is used instead of the explicit AND keyword:
 * identical semantics, and it works on every SQLite/FTS build.
 */
object SearchIndex {

    fun bodyFor(content: PageContent): String {
        val parts = ArrayList<String>()
        for (t in content.textObjects) {
            if (t.text.isNotBlank()) parts += t.text
        }
        for (seg in content.transcript) {
            if (seg.text.isNotBlank()) parts += seg.text
        }
        content.summary?.let { if (it.isNotBlank()) parts += it }
        val ink = InkIndexer.indexStrokes(content.strokes)
        if (ink.isNotBlank()) parts += ink
        return parts.joinToString("\n")
    }

    fun sanitizeQuery(raw: String): String {
        val tokens = raw.split(Regex("[^\\p{L}\\p{N}]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(10)
        if (tokens.isEmpty()) return ""
        return tokens.joinToString(" ") { "\"$it\"" }
    }
}
