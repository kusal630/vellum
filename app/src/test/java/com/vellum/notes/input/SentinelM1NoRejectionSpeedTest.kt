package com.vellum.notes.input

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SENT-M1: processWithoutPalmRejection used a hardcoded speed 0f instead of the
 * actual tracked pointer speed.
 */
class SentinelM1NoRejectionSpeedTest {

    @Test
    fun disabledPipelineReportsTrackedSpeed() {
        val e = PalmRejectionEngine(testCapabilities()) {
            testSettings().apply { palmRejectionEnabled = false }
        }
        e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.pen(0, timeMs = 0L)), added = 0))
        e.process(TestTouchFactory.frame(InputAction.MOVE, 5L, listOf(TestTouchFactory.pen(0, x = 180f, y = 100f, timeMs = 5L))))
        val out = e.process(
            TestTouchFactory.frame(InputAction.MOVE, 10L, listOf(TestTouchFactory.pen(0, x = 260f, y = 100f, timeMs = 10L))),
        )
        val speed = out.contactFor(0)?.speedMmPerSec ?: 0f
        assertTrue("expected tracked speed > 0, was $speed", speed > 0f)
    }
}
