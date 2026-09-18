package com.vellum.notes.packs

import kotlinx.serialization.Serializable

/** Revenue pack identifiers. SKUs are one-time Play Billing v7 products. */
enum class PackId(val id: String, val title: String, val sku: String) {
    CLASSROOM("classroom", "Classroom Pack", "vellum_pack_classroom"),
    PDF("pdf", "PDF Power Tools", "vellum_pack_pdf"),
    GESTURE("gesture", "Gesture & Bookmark Pack", "vellum_pack_gesture"),
    ;

    companion object {
        fun fromId(id: String): PackId? = entries.firstOrNull { it.id == id }
    }
}

/** Offline-first entitlements persisted as DataStore JSON booleans. */
@Serializable
data class PackEntitlements(
    val classroomPack: Boolean = false,
    val pdfPack: Boolean = false,
    val gesturePack: Boolean = false,
) {
    fun isUnlocked(pack: PackId): Boolean =
        // Paywalls removed for now: every pack reads unlocked. Flip back to
        // per-pack flags when billing returns.
        if (PAYWALLS_DISABLED) true else when (pack) {
            PackId.CLASSROOM -> classroomPack
            PackId.PDF -> pdfPack
            PackId.GESTURE -> gesturePack
        }

    companion object {
        const val PAYWALLS_DISABLED = true
    }

    fun withUnlocked(pack: PackId, unlocked: Boolean = true): PackEntitlements = when (pack) {
        PackId.CLASSROOM -> copy(classroomPack = unlocked)
        PackId.PDF -> copy(pdfPack = unlocked)
        PackId.GESTURE -> copy(gesturePack = unlocked)
    }
}

/** Marketing + unlock-dialog copy per pack (Compose only, no new deps). */
object PackCatalog {
    val features: Map<PackId, List<String>> = mapOf(
        PackId.CLASSROOM to listOf(
            "Audio-sync playback (tap transcript to seek)",
            "Chapters tab with auto-chapters",
            "Chapter + transcript export",
            "Scheduled local auto-backup",
        ),
        PackId.PDF to listOf(
            "Layered PDF export (paper / images / ink / text)",
            "Selectable text layer in exported PDFs",
            "Page manager: reorder, insert, delete, duplicate",
        ),
        PackId.GESTURE to listOf(
            "Bookmarks rail (jump between marked pages)",
            "Custom gesture mapping (e.g. two-finger double-tap)",
            "Per-notebook bookmark organization",
        ),
    )

    fun description(pack: PackId): String = when (pack) {
        PackId.CLASSROOM -> "Classroom superpowers for lecture notes."
        PackId.PDF -> "Professional PDF export and page control."
        PackId.GESTURE -> "Navigate faster with bookmarks and gestures."
    }
}
