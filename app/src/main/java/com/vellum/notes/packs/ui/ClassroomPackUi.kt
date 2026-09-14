package com.vellum.notes.packs.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vellum.notes.model.TranscriptSegment
import com.vellum.notes.packs.Chapter
import com.vellum.notes.packs.ClassroomPro

/**
 * Classroom Pack: Chapters tab. Auto-generates chapters from the transcript,
 * tap seeks the audio-sync playback position.
 */
@Composable
fun ChaptersTab(
    segments: List<TranscriptSegment>,
    chapters: List<Chapter>,
    onGenerate: () -> Unit,
    onSeek: (chapter: Chapter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        if (segments.isEmpty()) {
            Text(
                "No transcript yet — record a lecture first, then generate chapters.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth()
                .semantics { contentDescription = "Auto-generate chapters" },
        ) {
            Text(if (chapters.isEmpty()) "Generate chapters" else "Regenerate chapters")
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(chapters, key = { it.id }) { chapter ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .semantics { contentDescription = "Chapter ${chapter.title}" }
                        .clickable { onSeek(chapter) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        ClassroomPro.formatMs(chapter.startMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(44.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        chapter.title,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * Classroom Pack: audio-sync playback bar. Dragging seeks; the live segment
 * text shows what is playing at [positionMs].
 */
@Composable
fun AudioSyncBar(
    segments: List<TranscriptSegment>,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val max = (segments.maxOfOrNull { it.endMs.takeIf { it > 0 } ?: it.startMs } ?: 0L)
        .coerceAtLeast(1L)
    var slider by remember(positionMs, max) {
        mutableFloatStateOf(positionMs.toFloat() / max.toFloat())
    }
    val liveIndex = ClassroomPro.segmentAt(segments, positionMs)
    Column(modifier.semantics { contentDescription = "Audio-sync playback" }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                ClassroomPro.formatMs(positionMs),
                style = MaterialTheme.typography.labelMedium,
            )
            Slider(
                value = slider,
                onValueChange = {
                    slider = it
                    onSeek((it * max).toLong())
                },
                modifier = Modifier.weight(1f),
            )
            Text(
                ClassroomPro.formatMs(max),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        if (liveIndex >= 0 && liveIndex < segments.size) {
            Text(
                segments[liveIndex].text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Classroom Pack: Export tab — chapter + transcript export text with share.
 */
@Composable
fun ClassroomExportPanel(
    exportText: String,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(
            if (expanded) exportText else exportText.take(600),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (exportText.length > 600) {
            Text(
                if (expanded) "Show less" else "Show more",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { expanded = !expanded }.padding(vertical = 8.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onShare,
            modifier = Modifier.fillMaxWidth()
                .semantics { contentDescription = "Share classroom export" },
        ) {
            Text("Share export")
        }
    }
}
