package com.vellum.notes.packs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.vellum.notes.packs.PackCatalog
import com.vellum.notes.packs.PackEntitlements
import com.vellum.notes.packs.PackId

/** Locked pack icons render at 38% alpha per spec; tap opens the unlock dialog. */
const val LOCKED_PACK_ALPHA = 0.38f

fun packIcon(pack: PackId): ImageVector = when (pack) {
    PackId.CLASSROOM -> Icons.Filled.School
    PackId.PDF -> Icons.Filled.PictureAsPdf
    PackId.GESTURE -> Icons.Filled.Bookmark
}

/**
 * Toolbar icon that renders locked packs at 38% alpha. A locked tap must show
 * the [PackUnlockDialog]; an unlocked tap runs [onUnlockedClick].
 */
@Composable
fun PackGateIconButton(
    pack: PackId,
    unlocked: Boolean,
    contentDescription: String,
    onUnlockedClick: () -> Unit,
    onLockedClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = { if (unlocked) onUnlockedClick() else onLockedClick() },
        modifier = modifier
            .size(48.dp)
            .semantics {
                this.contentDescription = contentDescription
                stateDescription = if (unlocked) "Unlocked" else "Locked"
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                packIcon(pack),
                contentDescription = null,
                modifier = Modifier.then(
                    if (unlocked) Modifier else Modifier.alpha(LOCKED_PACK_ALPHA),
                ),
                tint = if (unlocked) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = LOCKED_PACK_ALPHA),
            )
            if (!unlocked) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = LOCKED_PACK_ALPHA),
                )
            }
        }
    }
}

/**
 * Unlock dialog: feature list + "Restore Purchases" / "Import License" buttons.
 * Purchase flow is attempted via [onRestorePurchases]; license import via
 * [onImportLicense] (caller opens a picker and feeds bytes to [PackUnlocker]).
 */
@Composable
fun PackUnlockDialog(
    pack: PackId,
    purchaseAvailable: Boolean,
    restoreMessage: String?,
    licenseMessage: String?,
    onRestorePurchases: () -> Unit,
    onImportLicense: () -> Unit,
    onBuyPack: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${pack.title} — locked") },
        text = {
            Column {
                Text(
                    PackCatalog.description(pack),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                PackCatalog.features.getValue(pack).forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp),
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(feature, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                restoreMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                licenseMessage?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (purchaseAvailable) {
                    Button(onClick = onBuyPack, modifier = Modifier.fillMaxWidth()) {
                        Text("Buy ${pack.title}")
                    }
                    Spacer(Modifier.height(4.dp))
                }
                OutlinedButton(onClick = onRestorePurchases, modifier = Modifier.fillMaxWidth()) {
                    Text("Restore Purchases")
                }
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = onImportLicense, modifier = Modifier.fillMaxWidth()) {
                    Text("Import License")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        },
    )
}

/** Settings -> Packs card grid showing lock/unlock state. */
@Composable
fun PacksCardGrid(
    entitlements: PackEntitlements,
    onPackClick: (PackId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Revenue Packs", style = MaterialTheme.typography.titleLarge)
        Text(
            "One-time unlocks. Fully offline — buy via Play or import a signed license file.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PackId.entries.forEach { pack ->
            val unlocked = entitlements.isUnlocked(pack)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "${pack.title} ${if (unlocked) "unlocked" else "locked"}"
                        stateDescription = if (unlocked) "Unlocked" else "Locked"
                    }
                    .clickable { onPackClick(pack) },
                colors = CardDefaults.cardColors(
                    containerColor = if (unlocked) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(
                                    alpha = if (unlocked) 1f else LOCKED_PACK_ALPHA,
                                ),
                                CircleShape,
                            )
                            .then(if (unlocked) Modifier else Modifier.alpha(LOCKED_PACK_ALPHA)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(packIcon(pack), contentDescription = null)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(pack.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (unlocked) "Unlocked"
                            else "${PackCatalog.features.getValue(pack).size} features — tap to unlock",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        if (unlocked) Icons.Filled.Check else Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.then(
                            if (unlocked) Modifier else Modifier.alpha(LOCKED_PACK_ALPHA),
                        ),
                    )
                }
            }
        }
    }
}
