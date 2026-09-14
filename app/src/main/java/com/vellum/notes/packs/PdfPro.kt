package com.vellum.notes.packs

import com.vellum.notes.model.PageContent
import com.vellum.notes.model.PageSummary
import kotlinx.serialization.Serializable

/** Layered-export toggles for PDF Power Tools. */
@Serializable
data class LayeredExportOptions(
    val includeBackground: Boolean = true,
    val includeImages: Boolean = true,
    val includeInk: Boolean = true,
    val includeShapes: Boolean = true,
    /** Selectable text layer built from text boxes (+ optional transcript). */
    val includeTextLayer: Boolean = true,
    val includeTranscript: Boolean = false,
)

/**
 * PDF Power Tools logic (pure, testable): layered export text-layer building
 * and page-manager list operations.
 */
object PdfPro {

    /** Builds the selectable text layer for an exported PDF page. */
    fun buildTextLayer(
        content: PageContent,
        options: LayeredExportOptions = LayeredExportOptions(),
    ): String {
        if (!options.includeTextLayer) return ""
        val lines = ArrayList<String>()
        content.textObjects.sortedBy { it.y * 10000 + it.x }.forEach { t ->
            if (t.text.isNotBlank()) lines += t.text
        }
        if (options.includeTranscript) {
            content.transcript.forEach { s ->
                if (s.text.isNotBlank()) lines += s.text
            }
        }
        if (!content.summary.isNullOrBlank()) lines += content.summary!!
        return lines.joinToString("\n")
    }

    /** Pure page-manager reorder: moves [pageId] to [newOrder] in the list. */
    fun movePage(pages: List<PageSummary>, pageId: Long, newOrder: Int): List<PageSummary> {
        val index = pages.indexOfFirst { it.id == pageId }
        if (index < 0) return pages
        val mutable = pages.toMutableList()
        val item = mutable.removeAt(index)
        mutable.add(newOrder.coerceIn(0, mutable.size), item)
        return mutable
    }

    /** Pure delete helper (mirrors repository semantics for previews/tests). */
    fun deletePage(pages: List<PageSummary>, pageId: Long): List<PageSummary> =
        pages.filterNot { it.id == pageId }

    /** Effective export layers given content + options (for tests/UI state). */
    fun activeLayers(content: PageContent, options: LayeredExportOptions): List<String> {
        val out = ArrayList<String>()
        if (options.includeBackground) out += "background"
        if (options.includeImages && content.imageObjects.isNotEmpty()) out += "images"
        if (options.includeInk && content.strokes.isNotEmpty()) out += "ink"
        if (options.includeShapes && content.shapeObjects.isNotEmpty()) out += "shapes"
        if (options.includeTextLayer && buildTextLayer(content, options).isNotBlank()) out += "text"
        return out
    }
}
