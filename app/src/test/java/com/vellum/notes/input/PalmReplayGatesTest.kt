package com.vellum.notes.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rate-gated replay battery over [PalmRejectionEngine]: recorded-style
 * palm/pen fixtures replayed in isolation (fresh engine each), asserting
 * false-accept and false-reject rates against hard gates.
 *
 * Complements the scenario acceptance tests: those prove the four canonical
 * cases; this proves the error rate across a wider fixture surface.
 */
class PalmReplayGatesTest {

    companion object {
        const val FAR_GATE = 0.003
        const val FRR_GATE = 0.01
    }

    private var settings = testSettings()

    private fun engine() = PalmRejectionEngine(testCapabilities()) { settings }

    private fun pen(id: Int, x: Float, y: Float, t: Long) =
        TestTouchFactory.pen(pointerId = id, x = x, y = y, timeMs = t)

    private fun palm(id: Int, x: Float, y: Float, t: Long, majorPx: Float = 300f) =
        TestTouchFactory.palm(pointerId = id, x = x, y = y, timeMs = t)
            .copy(toolMajorPx = majorPx, toolMinorPx = majorPx * 0.8f)

    /** Replays a lone palm DOWN; returns true if the engine wrongly writes it. */
    private fun replayLonePalm(x: Float, y: Float, majorPx: Float): Boolean {
        val e = engine()
        val down = e.process(
            TestTouchFactory.frame(
                InputAction.DOWN, 0L, listOf(palm(2, x, y, 0L, majorPx)), added = 2,
            ),
        )
        return down.contactFor(2)?.classification == ContactClassification.WRITING
    }

    /** Replays a lonely pen stroke (DOWN + stroke-like MOVEs); true if it writes. */
    private fun replayLonelyPen(x0: Float, y0: Float): Boolean {
        val e = engine()
        e.process(
            TestTouchFactory.frame(
                InputAction.DOWN, 0L, listOf(pen(0, x0, y0, 0L)), added = 0,
            ),
        )
        var last: ClassifiedFrame? = null
        var x = x0
        var t = 10L
        while (t <= 50L) {
            x += 40f
            last = e.process(
                TestTouchFactory.frame(
                    InputAction.MOVE, t, listOf(pen(0, x, y0, 0L)),
                ),
            )
            t += 10L
        }
        return last?.contactFor(0)?.classification == ContactClassification.WRITING
    }

    /** Pen writes while a palm rests mid-stroke; returns (penWrites, palmRejected). */
    private fun replayPenWithPalm(): Pair<Boolean, Boolean> {
        val e = engine()
        e.process(
            TestTouchFactory.frame(
                InputAction.DOWN, 0L, listOf(pen(0, 200f, 200f, 0L)), added = 0,
            ),
        )
        e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 5L, listOf(pen(0, 240f, 220f, 5L)),
            ),
        )
        e.process(
            TestTouchFactory.frame(
                InputAction.POINTER_DOWN, 10L,
                listOf(pen(0, 240f, 220f, 5L), palm(2, 500f, 700f, 10L)), added = 2,
            ),
        )
        val move = e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 20L,
                listOf(pen(0, 280f, 230f, 5L), palm(2, 500f, 700f, 10L)),
            ),
        )
        val penWrites = move.contactFor(0)?.classification == ContactClassification.WRITING
        val palmRejected = move.contactFor(2)?.classification != ContactClassification.WRITING
        return penWrites to palmRejected
    }

    @Test
    fun replayBattery_falseAcceptRate_withinGate() {
        val spots = listOf(
            500f to 700f, 150f to 650f, 700f to 650f,
            400f to 750f, 600f to 600f, 250f to 720f,
        )
        val sizes = listOf(220f, 300f, 400f)
        var falseAccepts = 0
        var total = 0
        for ((x, y) in spots) {
            for (major in sizes) {
                total++
                if (replayLonePalm(x, y, major)) falseAccepts++
            }
        }
        // Mid-stroke palms must never write either.
        repeat(2) {
            total++
            if (!replayPenWithPalm().second) falseAccepts++
        }
        val far = falseAccepts.toDouble() / total
        assertTrue("FAR $far ($falseAccepts/$total) exceeds gate $FAR_GATE", far <= FAR_GATE)
    }

    @Test
    fun replayBattery_falseRejectRate_withinGate() {
        val starts = listOf(
            200f to 200f, 350f to 300f, 150f to 450f,
            500f to 250f, 600f to 400f, 280f to 520f,
        )
        var falseRejects = 0
        var total = 0
        for ((x, y) in starts) {
            total++
            if (!replayLonelyPen(x, y)) falseRejects++
        }
        repeat(2) {
            total++
            if (!replayPenWithPalm().first) falseRejects++
        }
        val frr = falseRejects.toDouble() / total
        assertTrue("FRR $frr ($falseRejects/$total) exceeds gate $FRR_GATE", frr <= FRR_GATE)
    }

    @Test
    fun externalHoverLatch_suppressesFingerWhilePenPoised() {
        val e = engine()
        e.setStylusHovering(true)
        val finger = TestTouchFactory.fingertip(pointerId = 1, x = 300f, y = 300f, timeMs = 0L)
        val down = e.process(
            TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(finger), added = 1),
        )
        assertTrue(
            "finger must not write while stylus hovers, got ${down.contactFor(1)?.classification}",
            down.contactFor(1)?.classification != ContactClassification.WRITING,
        )
    }

    @Test
    fun externalHoverLatch_released_returnsToNormalPath() {
        val e = engine()
        e.setStylusHovering(true)
        e.setStylusHovering(false)
        val finger = TestTouchFactory.fingertip(pointerId = 1, x = 300f, y = 300f, timeMs = 0L)
        val down = e.process(
            TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(finger), added = 1),
        )
        assertTrue(
            "released latch must not suppress, got ${down.contactFor(1)?.classification}",
            down.contactFor(1)?.classification != ContactClassification.PALM,
        )
    }

    @Test
    fun leftHandMirror_penWrites_palmRejected() {
        val e = engine()
        e.process(
            TestTouchFactory.frame(
                InputAction.DOWN, 0L, listOf(palm(2, 100f, 700f, 0L)), added = 2,
            ),
        )
        val penDown = e.process(
            TestTouchFactory.frame(
                InputAction.POINTER_DOWN, 10L,
                listOf(palm(2, 100f, 700f, 0L), pen(0, 450f, 200f, 10L)), added = 0,
            ),
        )
        assertEquals(ContactClassification.WRITING, penDown.contactFor(0)?.classification)
        assertEquals(ContactClassification.PALM, penDown.contactFor(2)?.classification)
    }

    @Test
    fun edgeGripPalm_rejected() {
        val e = engine()
        val down = e.process(
            TestTouchFactory.frame(
                InputAction.DOWN, 0L, listOf(palm(2, 8f, 400f, 0L)), added = 2,
            ),
        )
        assertTrue(
            "edge palm wrote: ${down.contactFor(2)?.classification}",
            down.contactFor(2)?.classification != ContactClassification.WRITING,
        )
    }

    @Test
    fun stylusHover_fingerSuppressed() {
        val e = engine()
        val hoverStylus = TestTouchFactory.contact(
            pointerId = 9, x = 310f, y = 290f, timeMs = 0L,
            majorPx = 8f, minorPx = 8f, pressure = 0f,
            toolType = TestTouchFactory.TOOL_STYLUS,
        ).copy(hoverDistance = 5f)
        val finger = TestTouchFactory.fingertip(pointerId = 1, x = 300f, y = 300f, timeMs = 0L)
        val down = e.process(
            TestTouchFactory.frame(
                InputAction.DOWN, 0L, listOf(hoverStylus, finger), added = 1,
            ),
        )
        assertTrue(
            "hover-suppressed finger wrote: ${down.contactFor(1)?.classification}",
            down.contactFor(1)?.classification != ContactClassification.WRITING,
        )
    }

    @Test
    fun secondFingerJoiningMidStroke_doesNotStealStroke() {
        val e = engine()
        e.process(
            TestTouchFactory.frame(
                InputAction.DOWN, 0L, listOf(pen(0, 200f, 200f, 0L)), added = 0,
            ),
        )
        e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 10L, listOf(pen(0, 240f, 220f, 10L)),
            ),
        )
        val join = e.process(
            TestTouchFactory.frame(
                InputAction.POINTER_DOWN, 20L,
                listOf(
                    pen(0, 240f, 220f, 10L),
                    TestTouchFactory.fingertip(pointerId = 1, x = 500f, y = 500f, timeMs = 20L),
                ),
                added = 1,
            ),
        )
        assertEquals(ContactClassification.WRITING, join.contactFor(0)?.classification)
        assertEquals(0, join.activeWritingPointerId)
        assertTrue(
            "joining finger must not write, got ${join.contactFor(1)?.classification}",
            join.contactFor(1)?.classification != ContactClassification.WRITING,
        )
    }
}
