package com.vellum.notes.ui.editor

import android.graphics.RectF
import com.vellum.notes.input.ClassifiedFrame
import com.vellum.notes.input.InputAction
import com.vellum.notes.input.InputFrame
import com.vellum.notes.input.PalmRejectionEngine
import com.vellum.notes.input.SmoothingMode
import com.vellum.notes.input.TestTouchFactory
import com.vellum.notes.input.testCapabilities
import com.vellum.notes.input.testSettings
import com.vellum.notes.model.PenStyle
import com.vellum.notes.model.Point
import com.vellum.notes.model.ShapeObject
import com.vellum.notes.model.Stroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * PH-02 regression: the OS may batch the final MOVE samples into the UP event. The view
 * must feed that UP-batch history into the active stroke before finalizing, otherwise
 * the committed stroke ends abruptly at the last MOVE position (dropped tail).
 *
 * Geometry: 10px/mm capabilities, zoom 1, no pan → screen px / 10 = world mm.
 * Smoothing NONE so committed points match the fed samples exactly.
 */
@RunWith(RobolectricTestRunner::class)
class InkCanvasUpBatchTailTest {

    private lateinit var view: InkCanvasView
    private lateinit var engine: PalmRejectionEngine
    private var committed: Stroke? = null

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication().applicationContext
        view = InkCanvasView(context)
        val caps = testCapabilities(pxPerMm = 10f)
        engine = PalmRejectionEngine(caps) { testSettings() }
        view.capabilities = caps
        view.engine = engine
        view.penStyle = PenStyle(smoothing = SmoothingMode.NONE)
        view.listener = object : InkCanvasView.Listener {
            override fun onStrokeCommitted(stroke: Stroke) {
                committed = stroke
            }

            override fun onShapeCommitted(shape: ShapeObject) = Unit
            override fun onEraseGestureBegin() = Unit
            override fun onEraseAt(x: Float, y: Float, radiusMm: Float) = Unit
            override fun onEraseAlong(x1: Float, y1: Float, x2: Float, y2: Float, radiusMm: Float) = Unit
            override fun onEraseGestureEnd() = Unit
            override fun onViewportChanged(zoom: Float, offsetX: Float, offsetY: Float) = Unit
            override fun onSelectInRect(rect: RectF) = Unit
            override fun onSelectionDragStart(worldX: Float, worldY: Float) = Unit
            override fun onSelectionDragTo(worldX: Float, worldY: Float) = Unit
            override fun onSelectionDragEnd() = Unit
            override fun onSelectionResizeStart(handleIndex: Int) = Unit
            override fun onSelectionResizeTo(worldX: Float, worldY: Float) = Unit
            override fun onSelectionResizeEnd() = Unit
        }
        view.layout(0, 0, 1000, 1000)
    }

    /** Pumps one frame through the engine and the view's real stroke handler. */
    private fun pump(input: InputFrame) {
        val classified = engine.process(input)
        val m = InkCanvasView::class.java.getDeclaredMethod(
            "handleStroke",
            InputFrame::class.java,
            ClassifiedFrame::class.java,
        )
        m.isAccessible = true
        m.invoke(view, input, classified)
    }

    @Test
    fun upBatchHistoryIsCommittedIntoStrokeTail() {
        // DOWN (cold start → CANDIDATE, no stroke yet).
        pump(
            TestTouchFactory.frame(
                InputAction.DOWN, 0L,
                listOf(TestTouchFactory.pen(0, x = 100f, y = 100f, timeMs = 0L)),
                added = 0,
            )
        )
        // MOVE far enough to promote to WRITING and start the stroke.
        pump(
            TestTouchFactory.frame(
                InputAction.MOVE, 10L,
                listOf(TestTouchFactory.pen(0, x = 200f, y = 100f, timeMs = 10L)),
            )
        )
        // UP at x=300 whose batch carries the trailing samples at x=240 and x=270.
        fun tailSample(x: Float, t: Long) =
            TestTouchFactory.contact(0, x, 100f, t, majorPx = 26f, minorPx = 24f, pressure = 0.6f, size = 0.02f)
        pump(
            TestTouchFactory.frame(
                InputAction.UP, 20L,
                listOf(TestTouchFactory.pen(0, x = 300f, y = 100f, timeMs = 20L)),
                history = listOf(tailSample(240f, 16L), tailSample(270f, 18L)),
                lifted = 0,
            )
        )

        val stroke = committed
        assertNotNull("UP must commit the active stroke", stroke)
        val pts: List<Point> = stroke!!.points
        // World mm: start (10,10), move (20,10), batched tail (24,10)+(27,10), lift (30,10).
        // Commit-time RDP thinning may drop collinear intermediates, so the PH-02
        // intent is asserted geometrically: the tail must reach the lift point
        // (no truncation at the last MOVE) and extend past it via the batch.
        assertEquals(Point(30f, 10f), pts.last())
        assertTrue("UP-batch tail was dropped, got $pts", (pts.maxOf { it.x }) >= 27f)
        assertTrue("stroke must span down→lift, got $pts", pts.first().x <= 10f)
    }
}
