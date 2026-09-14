package com.vellum.notes.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SENT-C1: pointerStates leak → velocity spike on reused IDs.
 *
 * When a pointer is released and its ID is reused, stale velocity/pressure data
 * must not survive. Verifies retainAll(activeIds) cleanup after UP/CANCEL.
 */
class SentinelC1PointerStatesTest {

    private fun engine() =
        PalmRejectionEngine(testCapabilities()) { testSettings(PalmRejectionMode.WRITING) }

    @Suppress("UNCHECKED_CAST")
    private fun pointerStateCount(e: PalmRejectionEngine): Int {
        val f = PalmRejectionEngine::class.java.getDeclaredField("pointerStates")
        f.isAccessible = true
        return (f.get(e) as HashMap<*, *>).size
    }

    @Suppress("UNCHECKED_CAST")
    private fun pointerStateIds(e: PalmRejectionEngine): Set<Int> {
        val f = PalmRejectionEngine::class.java.getDeclaredField("pointerStates")
        f.isAccessible = true
        return ((f.get(e) as HashMap<Int, *>).keys).toSet()
    }

    @Test
    fun upClearsStalePointerStateSoReusedIdStartsFresh() {
        val e = engine()
        // Create pointerStates entries with motion (stale high velocity).
        e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.pen(0, timeMs = 0L)), added = 0))
        e.process(TestTouchFactory.frame(InputAction.MOVE, 5L, listOf(TestTouchFactory.pen(0, x = 500f, y = 500f, timeMs = 5L))))
        assertTrue(pointerStateCount(e) > 0)

        // Simulate UP with cleanup (single-pointer UP carries the lifted contact).
        val penUp = TestTouchFactory.pen(pointerId = 0, x = 500f, y = 500f, timeMs = 10L)
        e.process(TestTouchFactory.frame(InputAction.UP, 10L, listOf(penUp), lifted = 0))

        // No stale entries survive the next frame.
        assertEquals(emptySet<Int>(), pointerStateIds(e))

        // Reused ID starts fresh — no velocity spike, not misclassified as palm.
        val reused = e.process(
            TestTouchFactory.frame(
                InputAction.DOWN, 20L,
                listOf(TestTouchFactory.pen(pointerId = 0, x = 100f, y = 100f, timeMs = 20L)),
                added = 0,
            ),
        )
        val c = reused.contactFor(0)!!
        assertEquals(0f, c.speedMmPerSec, 0.001f)
        assertTrue(
            "reused ID must not inherit stale velocity as palm, was ${c.classification}",
            c.classification != ContactClassification.PALM,
        )
    }

    @Test
    fun pointerUpRetainsOnlyActiveIds() {
        val e = engine()
        val pen0 = TestTouchFactory.pen(pointerId = 0, x = 100f, y = 100f, timeMs = 0L)
        val pen1 = TestTouchFactory.pen(pointerId = 1, x = 300f, y = 300f, timeMs = 0L)
        e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(pen0), added = 0))
        e.process(TestTouchFactory.frame(InputAction.POINTER_DOWN, 5L, listOf(pen0, pen1), added = 1))
        assertEquals(setOf(0, 1), pointerStateIds(e))

        // Pointer 1 lifts; only pointer 0 survives.
        val pen0b = TestTouchFactory.pen(pointerId = 0, x = 110f, y = 100f, timeMs = 10L)
        e.process(TestTouchFactory.frame(InputAction.POINTER_UP, 10L, listOf(pen0b), lifted = 1))
        assertEquals(setOf(0), pointerStateIds(e))
    }

    @Test
    fun cancelClearsAllPointerStates() {
        val e = engine()
        e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.pen(0, timeMs = 0L)), added = 0))
        e.process(TestTouchFactory.frame(InputAction.CANCEL, 10L, emptyList()))
        assertEquals(emptySet<Int>(), pointerStateIds(e))
        // Fresh DOWN after CANCEL starts clean (no stale velocity spike).
        val out = e.process(
            TestTouchFactory.frame(InputAction.DOWN, 20L, listOf(TestTouchFactory.pen(0, timeMs = 20L)), added = 0),
        )
        assertEquals(0f, out.contactFor(0)?.speedMmPerSec ?: -1f, 0.001f)
    }
}
