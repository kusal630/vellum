package com.vellum.notes.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * SENT-M2: the writing-lock hold-off was lifted globally whenever finger writing
 * was enabled — a resting palm (or stylus) could claim writing right after a lift.
 * The lift now applies only to the finger tool itself.
 */
class SentinelM2HoldoffToolTest {

    private fun engine() = PalmRejectionEngine(testCapabilities()) {
        testSettings(mode = PalmRejectionMode.WRITING).apply {
            enableFingerWriting = true
            writingHoldoffMs = 5000L
        }
    }

    @Test
    fun stylusRespectsHoldoffWhileFingerBypassesIt() {
        val e = engine()

        // Finger stroke claims and lifts, starting the hold-off window.
        e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.fingertip(0, timeMs = 0L)), added = 0))
        e.process(TestTouchFactory.frame(InputAction.MOVE, 5L, listOf(TestTouchFactory.fingertip(0, x = 280f, y = 220f, timeMs = 5L))))
        val fingerUp = TestTouchFactory.fingertip(pointerId = 0, x = 280f, y = 220f, timeMs = 10L)
        e.process(TestTouchFactory.frame(InputAction.UP, 10L, listOf(fingerUp), lifted = 0))

        // A stylus DOWN inside the hold-off window must NOT claim (hold-off respected).
        val stylus = TestTouchFactory.pen(pointerId = 1, x = 120f, y = 110f, timeMs = 20L, toolType = TestTouchFactory.TOOL_STYLUS)
        val stylusDown = e.process(TestTouchFactory.frame(InputAction.DOWN, 20L, listOf(stylus), added = 1))
        assertNull("stylus must respect the hold-off, was ${stylusDown.activeWritingPointerId}", stylusDown.activeWritingPointerId)

        // A finger stroke inside the same window still claims via promotion
        // (hold-off lifted for the finger tool only).
        val e2 = engine()
        e2.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.fingertip(0, timeMs = 0L)), added = 0))
        e2.process(TestTouchFactory.frame(InputAction.MOVE, 5L, listOf(TestTouchFactory.fingertip(0, x = 280f, y = 220f, timeMs = 5L))))
        val fingerUp2 = TestTouchFactory.fingertip(pointerId = 0, x = 280f, y = 220f, timeMs = 10L)
        e2.process(TestTouchFactory.frame(InputAction.UP, 10L, listOf(fingerUp2), lifted = 0))
        e2.process(TestTouchFactory.frame(InputAction.DOWN, 20L, listOf(TestTouchFactory.fingertip(1, timeMs = 20L)), added = 1))
        val fingerMove = e2.process(
            TestTouchFactory.frame(InputAction.MOVE, 25L, listOf(TestTouchFactory.fingertip(1, x = 280f, y = 220f, timeMs = 25L))),
        )
        assertEquals(1, fingerMove.activeWritingPointerId)
    }
}
