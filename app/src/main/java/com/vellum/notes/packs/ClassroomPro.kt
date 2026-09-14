package com.vellum.notes.packs

import com.vellum.notes.model.TranscriptSegment

/**
 * Classroom Pack logic (pure, testable): audio-sync playback mapping,
 * auto-chapters, and chapter/transcript export text.
 */
object ClassroomPro {

    /**
     * Audio-sync playback: finds the live segment for [positionMs].
     * Returns the segment index, or -1 when the transcript is empty.
     */
    fun segmentAt(segments: List<TranscriptSegment>, positionMs: Long): Int {
        if (segments.isEmpty()) return -1
        var best = 0
        for ((i, seg) in segments.withIndex()) {
            if (seg.startMs <= positionMs) best = i else break
        }
        return best
    }

    /**
     * Auto-chapters: groups segments into chapters of ~[segmentsPerChapter]
     * segments. Titles are "Chapter N · <first words…>".
     */
    fun autoChapters(
        segments: List<TranscriptSegment>,
        segmentsPerChapter: Int = 8,
    ): List<Chapter> {
        if (segments.isEmpty()) return emptyList()
        val per = segmentsPerChapter.coerceAtLeast(1)
        return segments.chunked(per).mapIndexed { i, chunk ->
            val first = chunk.first()
            val words = first.text.trim().split(Regex("\\s+")).take(5).joinToString(" ")
            Chapter(
                id = i.toLong(),
                title = "Chapter ${i + 1} · ${words.ifBlank { "Untitled" }}".take(80),
                startMs = first.startMs,
                segmentIds = chunk.map { it.id },
            )
        }
    }

    /** Export text: chapters header + transcript + optional summary. */
    fun exportText(
        segments: List<TranscriptSegment>,
        chapters: List<Chapter>,
        summary: String?,
        pageTitle: String,
    ): String = buildString {
        appendLine("# $pageTitle — Classroom export")
        appendLine()
        if (chapters.isNotEmpty()) {
            appendLine("## Chapters")
            chapters.forEach { c ->
                appendLine("- [${formatMs(c.startMs)}] ${c.title}")
            }
            appendLine()
        }
        if (!summary.isNullOrBlank()) {
            appendLine("## Summary")
            appendLine(summary)
            appendLine()
        }
        appendLine("## Transcript")
        segments.forEach { s ->
            appendLine("[${formatMs(s.startMs)}] ${s.text}")
        }
    }

    fun formatMs(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val m = totalSec / 60
        val s = totalSec % 60
        return "%d:%02d".format(m, s)
    }
}
