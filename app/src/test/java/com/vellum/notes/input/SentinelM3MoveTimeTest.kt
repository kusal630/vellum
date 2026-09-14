package com.vellum.notes.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * SENT-M3: lastMoveTimeNanos uninitialized (0L) made the first stationary delta
 * enormous; isNew could leak past its frame without try-finally.
 */
class SentinelM3MoveTimeTest {

    @Test
    fun lastMoveTimeDefaultsToUnsetSentinel() {
        val st = PointerMotionState(
            downTimeNanos = 0L,
            startX = 0f,
            startY = 0f,
            lastX = 0f,
            lastY = 0f,
            lastTimeNanos = 0L,
        )
        assertEquals(-1L, st.lastMoveTimeNanos)
    }

    @Test
    fun freshContactAtLargeUptimeIsNotInstantlyResting() {
        // Realistic device uptime (~1h): with a 0L default the first stationary
        // delta would be enormous and the contact would demote to RESTING at once.
        val e = PalmRejectionEngine(testCapabilities()) {
            testSettings().apply { allowImmediateDrawWhenIsolated = false }
        }
        val t0 = 3_600_000L
        fun writer(x: Float, timeMs: Long) =
            TestTouchFactory.contact(0, x, 100f, timeMs, majorPx = 90f, minorPx = 82f)

        val down = e.process(
            TestTouchFactory.frame(InputAction.DOWN, t0, listOf(writer(100f, t0)), added = 0),
        )
        assertEquals(ContactClassification.CANDIDATE, down.contactFor(0)?.classification)

        // Still stationary 10ms later: buffered CANDIDATE, not instantly RESTING.
        val held = e.process(
            TestTouchFactory.frame(InputAction.MOVE, t0 + 10L, listOf(writer(100f, t0))),
        )
        assertEquals(ContactClassification.CANDIDATE, held.contactFor(0)?.classification)
    }

    @Test
    fun isNewClearedAfterFirstFrame() {
        val tracker = RestingHandTracker(testCapabilities())
        val settings = testSettings()
        val caps = testCapabilities()
        val normalizer = InputNormalizer(caps)
        val raw = TestTouchFactory.pen(pointerId = 0, x = 100f, y = 100f, timeMs = 0L)
        val normalized = normalizer.normalize(raw)
        val base = listOf(
            ClassifiedContact(
                contact = normalized,
                classification = ContactClassification.FINGER,
                confidence = 0.6f,
                reason = ClassificationReason.MEDIUM_CONTACT,
                effectiveThresholdMm = 0f,
                speedMmPerSec = 0f,
                durationMs = 0L,
            ),
        )
        val frame = TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(raw), added = 0)
        tracker.process(frame, base, null, settings)

        val f = RestingHandTracker::class.java.getDeclaredField("pointerStates")
        f.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val states = f.get(tracker) as HashMap<Int, PointerMotionState>
        assertFalse(states[0]?.isNew ?: true)
    }
}
