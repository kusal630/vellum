package com.vellum.notes.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Path
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import com.vellum.notes.model.PageBackground
import com.vellum.notes.model.PageBackgroundType

/**
 * Renders paper templates (ruled, grid, dotted, etc.) in world coordinates
 * (millimeters). The caller applies the viewport transform before drawing, and passes
 * the visible world region so only on-screen pattern lines are generated.
 */
object PageBackgroundRenderer {

    // Reused across frames to avoid per-draw allocations on the UI thread.
    private val linePaint = Paint().apply { isAntiAlias = true }

    // --- Premium page-sheet rendering (Noteshelf-style desk + paper) -----------
    const val PAGE_W_MM = 210f
    const val PAGE_H_MM = 297f
    private const val PAGE_CORNER_MM = 5f

    private val shadowPaint = Paint().apply { isAntiAlias = true }
    private val sheetPaint = Paint().apply { isAntiAlias = true }
    private val sheetEdgePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 0.15f
        color = Color.argb(38, 0, 0, 0)
    }
    private val sheenPaint = Paint().apply { isAntiAlias = true }
    private val sheetRect = RectF()
    private val clipPath = Path()
    private var sheenShader: LinearGradient? = null
    private var sheenPaperColor = 0

    fun drawBackground(canvas: Canvas, bg: PageBackground, pxPerMm: Float, worldClip: RectF) {
        val bgColor = (bg.colorArgb and 0xFFFFFFFF.toLong()).toInt()

        // 1) Paper fills the whole viewport: pure color, no shadows, no sheen, no
        // visual noise outside the page bounds. World-class note apps (GoodNotes,
        // Notability, Concepts) keep the canvas clean — the paper IS the background.
        sheetPaint.style = Paint.Style.FILL
        sheetPaint.color = bgColor
        canvas.drawRect(worldClip, sheetPaint)

        // 2) Subtle page boundary: a single thin hairline so the user can see the
        // page edge when zoomed out, but no shadow or sheen that creates visual
        // clutter. The line is clipped to the viewport so it never draws outside.
        sheetPaint.style = Paint.Style.FILL
        sheetPaint.color = bgColor
        sheetRect.set(0f, 0f, PAGE_W_MM, PAGE_H_MM)
        canvas.drawRoundRect(sheetRect, PAGE_CORNER_MM, PAGE_CORNER_MM, sheetPaint)
        // Thin edge line — just enough to delineate the page, no shadow.
        canvas.drawRoundRect(sheetRect, PAGE_CORNER_MM, PAGE_CORNER_MM, sheetEdgePaint)

        linePaint.color = bg.lineColorArgb.toInt()

        // 4) Template pattern tiles across the whole visible paper, seamless at
        // any zoom: loops already align to the spacing grid from the clip.
        val fromX = worldClip.left
        val fromY = worldClip.top
        val toX = worldClip.right
        val toY = worldClip.bottom
        if (fromX >= toX || fromY >= toY) return

        when (bg.type) {
            PageBackgroundType.BLANK -> Unit

            PageBackgroundType.RULED,
            PageBackgroundType.NARROW_RULED,
            PageBackgroundType.WIDE_RULED,
            -> {
                val spacing = spacingFor(bg)
                linePaint.strokeWidth = (0.4f * pxPerMm).coerceAtLeast(1f)
                var y = (fromY / spacing).toInt() * spacing
                while (y < toY) {
                    canvas.drawLine(fromX, y, toX, y, linePaint)
                    y += spacing
                }
                linePaint.strokeWidth = (0.6f * pxPerMm).coerceAtLeast(1f)
                val margin = 24f * pxPerMm
                if (margin >= fromX && margin <= toX) {
                    canvas.drawLine(margin, fromY, margin, toY, linePaint)
                }
            }

            PageBackgroundType.GRID,
            PageBackgroundType.SMALL_GRID,
            PageBackgroundType.GRAPH,
            PageBackgroundType.MATH,
            -> {
                val size = gridSizeFor(bg)
                linePaint.strokeWidth = (0.3f * pxPerMm).coerceAtLeast(1f)
                var x = (fromX / size).toInt() * size
                while (x <= toX) {
                    canvas.drawLine(x, fromY, x, toY, linePaint)
                    x += size
                }
                var y = (fromY / size).toInt() * size
                while (y <= toY) {
                    canvas.drawLine(fromX, y, toX, y, linePaint)
                    y += size
                }
            }

            PageBackgroundType.DOTTED -> {
                val spacing = bg.dotSpacingMm
                var x = (fromX / spacing).toInt() * spacing
                while (x <= toX) {
                    var y = (fromY / spacing).toInt() * spacing
                    while (y <= toY) {
                        canvas.drawCircle(x, y, (0.25f * pxPerMm).coerceAtLeast(0.8f), linePaint)
                        y += spacing
                    }
                    x += spacing
                }
            }

            PageBackgroundType.CORNELL -> {
                val spacing = spacingFor(bg)
                linePaint.strokeWidth = (0.4f * pxPerMm).coerceAtLeast(1f)
                var y = (fromY / spacing).toInt() * spacing
                while (y < toY) {
                    canvas.drawLine(fromX, y, toX, y, linePaint)
                    y += spacing
                }
                linePaint.strokeWidth = (0.8f * pxPerMm).coerceAtLeast(2f)
                val keyCol = 56f * pxPerMm
                canvas.drawLine(keyCol, fromY, keyCol, toY, linePaint)
                val headerRow = 56f * pxPerMm
                if (headerRow >= fromY && headerRow <= toY) {
                    canvas.drawLine(fromX, headerRow, toX, headerRow, linePaint)
                }
            }

            PageBackgroundType.MUSIC -> {
                linePaint.strokeWidth = (0.3f * pxPerMm).coerceAtLeast(1f)
                val staffGap = 1.6f * pxPerMm
                val staffHeight = 4 * staffGap
                var y = (fromY / staffHeight).toInt() * staffHeight
                while (y < toY) {
                    for (i in 0 until 5) {
                        val ly = y + i * staffGap
                        canvas.drawLine(fromX, ly, toX, ly, linePaint)
                    }
                    y += staffHeight + 4f * pxPerMm
                }
            }
        }
    }

    private fun spacingFor(bg: PageBackground): Float = when (bg.type) {
        PageBackgroundType.NARROW_RULED -> bg.lineSpacingMm * 0.6f
        PageBackgroundType.WIDE_RULED -> bg.lineSpacingMm * 1.5f
        else -> bg.lineSpacingMm
    }

    private fun gridSizeFor(bg: PageBackground): Float = when (bg.type) {
        PageBackgroundType.SMALL_GRID -> bg.gridSizeMm * 0.5f
        else -> bg.gridSizeMm
    }
}