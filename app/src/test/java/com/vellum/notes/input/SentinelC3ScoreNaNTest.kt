package com.vellum.notes.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SENT-C3: div-by-zero → NaN in writeScoreFor/restScoreFor.
 *
 * Zero velocity/path/size/growth thresholds divided straight into NaN, poisoning
 * the resting-hand score so every frame re-evaluated (CPU spin). All four divisors
 * are guarded with EPSILON.
 */
class SentinelC3ScoreNaNTest {

    private fun engine(configure: PalmRejectionSettings.() -> Unit = {}) =
        PalmRejectionEngine(testCapabilities()) {
            testSettings().apply(configure)
        }

    @Test
    fun zeroVelocityAndPath_writeScoreIsZeroNotNaN() {
        // Zero velocity + zero path gates with a stationary palm-sized contact:
        // vel 0/eps = 0, path 0/eps = 0, continuity 0 (single sample),
        // size (1 - 30/24) clamped to 0 → writeScore exactly 0.0 (was NaN).
        val e = engine {
            minPromoteVelocityMmPerSec = 0f
            movementPromoteThresholdMm = 0f
        }
        val out = e.process(
            TestTouchFactory.frame(
                InputAction.DOWN, 0L,
                listOf(TestTouchFactory.palm(pointerId = 2, timeMs = 0L)),
                added = 2,
            ),
        )
        val c = out.contactFor(2)!!
        assertFalse("writeScore must not be NaN", c.writeScore.isNaN())
        assertFalse("restScore must not be NaN", c.restScore.isNaN())
        assertEquals(0.0f, c.writeScore, 0.001f)
    }

    @Test
    fun zeroSizeThresholds_scoresAreFiniteNotNaN() {
        // Zero-size contact (no geometry) with zeroed size/growth thresholds:
        // every 0/0 division is guarded → finite scores (were NaN).
        val e = engine {
            minPromoteVelocityMmPerSec = 0f
            movementPromoteThresholdMm = 0f
            palmSizeThresholdMm = 0f
            palmGrowthFactor = 0f
        }
        val out = e.process(
            TestTouchFactory.frame(
                InputAction.DOWN, 0L,
                listOf(TestTouchFactory.contact(0, 100f, 100f, 0L, majorPx = 0f, minorPx = 0f, size = 0f)),
                added = 0,
            ),
        )
        val c = out.contactFor(0)!!
        assertFalse("writeScore must not be NaN", c.writeScore.isNaN())
        assertFalse("restScore must not be NaN", c.restScore.isNaN())
        assertTrue("writeScore must be finite", c.writeScore.isFinite())
        assertTrue("restScore must be finite", c.restScore.isFinite())
        // Zero-size rest evidence contributes 0: size 0/eps = 0, growth 0 → bounded restScore.
        assertTrue(c.restScore in 0f..1f)
        assertTrue(c.writeScore in 0f..1f)
    }
}
