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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vellum.notes.packs.GestureMapping
import com.vellum.notes.packs.GesturePro
import com.vellum.notes.packs.PageBookmark

/**
 * Gesture/Bookmark Pack: bookmarks rail section. Locked state renders at 38%
 * alpha and opens the unlock dialog on tap.
 */
@Composable
fun BookmarksRail(
    bookmarks: List<PageBookmark>,
    currentPageId: Long,
    unlocked: Boolean,
    onJump: (Long) -> Unit,
    onAddCurrent: () -> Unit,
    onRemove: (Long) -> Unit,
    onLockedClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.semantics { contentDescription = "Bookmarks rail" },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Bookmarks",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f)
                    .then(if (unlocked) Modifier else Modifier.alpha(LOCKED_PACK_ALPHA)),
            )
            if (!unlocked) {
                Icon(
                    Icons.Filled.BookmarkBorder,
                    contentDescription = "Bookmarks locked",
                    modifier = Modifier.alpha(LOCKED_PACK_ALPHA),
                )
            }
        }
        if (!unlocked) {
            Text(
                "Unlock to bookmark pages",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = LOCKED_PACK_ALPHA),
                modifier = Modifier.clickable { onLockedClick() }.padding(vertical = 8.dp),
            )
            return
        }
        if (bookmarks.isEmpty()) {
            Text(
                "No bookmarks yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        bookmarks.forEach { bookmark ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .semantics { contentDescription = "Bookmark ${bookmark.title}" }
                    .clickable { onJump(bookmark.pageId) }
                    .padding(vertical = 6.dp),
            ) {
                Icon(
                    if (bookmark.pageId == currentPageId) Icons.Filled.Bookmark
                    else Icons.Filled.BookmarkBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    bookmark.title.ifBlank { "Page ${bookmark.pageId}" },
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
                IconButton(
                    onClick = { onRemove(bookmark.pageId) },
                    modifier = Modifier.semantics {
                        contentDescription = "Remove bookmark ${bookmark.title}"
                    },
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                }
            }
        }
        OutlinedButton(
            onClick = onAddCurrent,
            modifier = Modifier.fillMaxWidth()
                .semantics { contentDescription = "Bookmark current page" },
        ) {
            Text("Bookmark this page")
        }
    }
}

/**
 * Gesture/Bookmark Pack: custom gesture mapping dialog. Each known gesture
 * maps to one known action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureMappingDialog(
    mapping: List<GestureMapping>,
    onSave: (List<GestureMapping>) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(mapping) {
        mutableStateOf(
            GesturePro.knownGestures.map { gesture ->
                gesture to (mapping.firstOrNull { it.gesture == gesture }?.action
                    ?: GesturePro.defaultMapping().firstOrNull { it.gesture == gesture }?.action
                    ?: GesturePro.ACTION_UNDO)
            },
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gesture mapping") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Assign a custom action to each gesture. Changes apply immediately after save.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                draft.forEachIndexed { index, (gesture, action) ->
                    var expanded by remember(gesture) { mutableStateOf(false) }
                    Text(
                        gestureLabel(gesture),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                    ) {
                        TextField(
                            value = actionLabel(action),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                                .semantics { contentDescription = "Action for ${gestureLabel(gesture)}" },
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            GesturePro.knownActions.forEach { candidate ->
                                DropdownMenuItem(
                                    text = { Text(actionLabel(candidate)) },
                                    onClick = {
                                        draft = draft.toMutableList().also {
                                            it[index] = gesture to candidate
                                        }
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(draft.map { (g, a) -> GestureMapping(g, a) })
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onReset) { Text("Reset") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

private fun gestureLabel(gesture: String): String = when (gesture) {
    GesturePro.GESTURE_TWO_FINGER_DOUBLE_TAP -> "Two-finger double-tap"
    GesturePro.GESTURE_THREE_FINGER_TAP -> "Three-finger tap"
    GesturePro.GESTURE_TWO_FINGER_SWIPE_LEFT -> "Two-finger swipe left"
    GesturePro.GESTURE_TWO_FINGER_SWIPE_RIGHT -> "Two-finger swipe right"
    else -> gesture
}

private fun actionLabel(action: String): String = when (action) {
    GesturePro.ACTION_UNDO -> "Undo"
    GesturePro.ACTION_REDO -> "Redo"
    GesturePro.ACTION_TOGGLE_RAIL -> "Show/hide page rail"
    GesturePro.ACTION_NEW_PAGE -> "New page"
    GesturePro.ACTION_EXPORT_PDF -> "Export PDF"
    GesturePro.ACTION_TOGGLE_TRANSCRIPT -> "Show/hide transcript"
    else -> action
}
