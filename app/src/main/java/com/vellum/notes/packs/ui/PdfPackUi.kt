package com.vellum.notes.packs.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vellum.notes.model.PageSummary
import com.vellum.notes.packs.LayeredExportOptions

/**
 * PDF Power Tools: page manager dialog — reorder (up/down), insert, duplicate,
 * delete. Entry: toolbar overflow "Page Manager" + page-rail "Manage Pages".
 */
@Composable
fun PageManagerDialog(
    pages: List<PageSummary>,
    onMove: (pageId: Long, newOrder: Int) -> Unit,
    onInsert: () -> Unit,
    onDuplicate: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage pages") },
        text = {
            Column {
                LazyColumn(
                    modifier = Modifier.height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(pages, key = { _, p -> p.id }) { index, page ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                                .semantics { contentDescription = "Manage page ${page.title}" },
                        ) {
                            Text(
                                "${index + 1}. ${page.title}",
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            IconButton(
                                onClick = { onMove(page.id, index - 1) },
                                enabled = index > 0,
                                modifier = Modifier.semantics { contentDescription = "Move ${page.title} up" },
                            ) {
                                Icon(Icons.Filled.ArrowUpward, contentDescription = null)
                            }
                            IconButton(
                                onClick = { onMove(page.id, index + 1) },
                                enabled = index < pages.lastIndex,
                                modifier = Modifier.semantics { contentDescription = "Move ${page.title} down" },
                            ) {
                                Icon(Icons.Filled.ArrowDownward, contentDescription = null)
                            }
                            IconButton(
                                onClick = { onDuplicate(page.id) },
                                modifier = Modifier.semantics { contentDescription = "Duplicate ${page.title}" },
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null)
                            }
                            IconButton(
                                onClick = { onDelete(page.id) },
                                modifier = Modifier.semantics { contentDescription = "Delete ${page.title}" },
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onInsert,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Insert page")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

/**
 * PDF Power Tools: layered export options dialog — paper / images / ink /
 * shapes / selectable text layer.
 */
@Composable
fun LayeredExportDialog(
    initial: LayeredExportOptions,
    onExport: (LayeredExportOptions) -> Unit,
    onDismiss: () -> Unit,
) {
    var options by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Layered PDF export") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                LayerCheck(
                    "Paper background",
                    options.includeBackground,
                ) { options = options.copy(includeBackground = it) }
                LayerCheck("Images", options.includeImages) {
                    options = options.copy(includeImages = it)
                }
                LayerCheck("Ink strokes", options.includeInk) {
                    options = options.copy(includeInk = it)
                }
                LayerCheck("Shapes", options.includeShapes) {
                    options = options.copy(includeShapes = it)
                }
                LayerCheck(
                    "Selectable text layer (typed text)",
                    options.includeTextLayer,
                ) { options = options.copy(includeTextLayer = it) }
                LayerCheck(
                    "Include transcript in text layer",
                    options.includeTranscript,
                ) { options = options.copy(includeTranscript = it) }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Text layer makes typed text selectable in the exported PDF.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onExport(options) }) { Text("Export") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun LayerCheck(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .semantics { contentDescription = label },
    ) {
        Checkbox(checked = checked, onCheckedChange = onChecked)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
