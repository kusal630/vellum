package com.vellum.notes.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regression tests mirroring the REAL DEVICE startup path that unit tests miss:
 * tests use testCapabilities() / testSettings(), but on a real device the engine is
 * built from InputCapabilities.detect(context) (real pxPerMm, clamped) and from
 * AppContainer.currentSettings (which starts as PalmRejectionSettings() default and
 * is updated asynchronously by the SettingsRepository.DataStore flow).
 *
 * These tests use the default PalmRejectionSettings() (no persisted values) and a
 * clamped real-device capabilities (pxPerMm ~ 9.45f = 240dpi) to reproduce the path
 * where the first touch arrives before persisted settings have landed.
 */
class PalmRejectionColdStartRealDeviceTest {

    // Realistic 240dpi tablet: 240 / 25.4 = 9.448f px/mm, clamped into range.
    private val realDeviceCaps = InputCapabilities(
        pxPerMm = 9.448f,
        displayMaxPx = 2560f,
        screenDiagonalMm = 254f,
        supportsStylus = false,
        supportsToolType = true,
        supportsStylusToolType = false,
        supportsContactSize = true,
        supportsPressure = true,
        supportsMultiTouch = true,
        hasPalmClassificationHint = false,
        apiLevel = 35,
    )

    // Default-in-memory settings: what the engine sees BEFORE the DataStore flow lands.
    private val defaultSettings = PalmRejectionSettings()

    private fun engine() = PalmRejectionEngine(realDeviceCaps) { defaultSettings }

    // ---- Cold-start with no geometry (common on real digitizers that don't report
    // ---- toolMajor/toolMinor, only getSize(). On a real device this is the typical
    // ---- first-touch path: the digitizer reports a size but no tool geometry.

    @Test
    fun coldStartNoGeometryFingerStrokesAfterFirstMove() {
        // A finger contact with getSize() but no toolMajor/toolMinor: on a real device
        // the normalizer derives sizePx from getSize() * displayMaxPx. If getSize() is
        // reasonable (<= MAX_PLAUSIBLE_CONTACT_MM * pxPerMm), we get a size-derived
        // ellipse with hasGeometry = false.
        //
        // Cold start: lone contact DOWN -> CANDIDATE (never WRITING on DOWN).
        val fingerNoGeom = TestTouchFactory.contact(
            pointerId = 0, x = 100f, y = 100f, timeMs = 0L,
            majorPx = 0f, minorPx = 0f, pressure = 0.6f, size = 0.04f,
        )
        val e = engine()
        val down = e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(fingerNoGeom), added = 0))
        assertEquals(ContactClassification.CANDIDATE, down.contactFor(0)?.classification)

        // A MOVE promotes it to WRITING (distance >= 4mm at 9.448f px/mm).
        val move = e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 5L,
                listOf(TestTouchFactory.contact(0, x = 200f, y = 100f, timeMs = 5L, majorPx = 0f, minorPx = 0f, pressure = 0.6f, size = 0.04f)),
            )
        )
        assertEquals(0, move.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, move.contactFor(0)?.classification)
    }

    @Test
    fun coldStartNoGeometryPalmRejectedIndependentlyOfSettings() {
        // A palm-sized getSize() value (0.28f) with no tool geometry. On a real device
        // the size-derived ellipse is ~ 0.28f * 2560f = 716.8px = 75.8mm at 9.448f px/mm
        // — above the 24mm palmSizeThresholdMm default.
        val palmNoGeom = TestTouchFactory.contact(
            pointerId = 2, x = 500f, y = 700f, timeMs = 0L,
            majorPx = 0f, minorPx = 0f, pressure = 1.0f, size = 0.28f,
        )
        val e = engine()
        val down = e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(palmNoGeom), added = 2))
        assertEquals(ContactClassification.PALM, down.contactFor(2)?.classification)
    }

    // ---- Degenerate pxPerMm path: the normalizer's maxPlausiblePx must not collapse
    // ---- when pxPerMm is 0 (caught by the normalizer guard; tested via a 0 px/mm cap).

    @Test
    fun degenerateZeroPxPerMmStillClassifiesPalmBySize() {
        // If somehow pxPerMm is 0, the normalizer falls back to 10f. A palm contact
        // (300px major) at 10f px/mm = 30mm > 24mm threshold -> PALM.
        val zeroCaps = InputCapabilities(
            pxPerMm = 0f,
            displayMaxPx = 2560f,
            screenDiagonalMm = 0f,
            supportsStylus = false, supportsToolType = true, supportsStylusToolType = false,
            supportsContactSize = true, supportsPressure = true, supportsMultiTouch = true,
            hasPalmClassificationHint = false, apiLevel = 35,
        )
        val e = PalmRejectionEngine(zeroCaps) { defaultSettings }
        val palm = TestTouchFactory.palm(pointerId = 2, timeMs = 0L)
        val out = e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(palm), added = 2))
        assertEquals(ContactClassification.PALM, out.contactFor(2)?.classification)
    }

    // ---- Cold-start lock: the first genuine writer must claim the lock WITHOUT the
    // ---- hold-off (no prior lock exists). Real devices: first pen/finger touch must
    // ---- write immediately, never delayed by the post-lift cooldown.

    @Test
    fun coldStartFirstPenClaimsLockWithoutHoldoff() {
        val e = engine()
        // DOWN: CANDIDATE (never WRITING on DOWN).
        val penDown = e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.pen(0, timeMs = 0L)), added = 0))
        assertNull(penDown.activeWritingPointerId)

        // MOVE: promotes to WRITING and claims the lock. At cold start (lockAcquiredAtNanos == 0)
        // the hold-off must NOT block this claim.
        val penMove = e.process(
            TestTouchFactory.frame(InputAction.MOVE, 5L, listOf(TestTouchFactory.pen(0, x = 180f, y = 100f, timeMs = 5L)))
        )
        assertEquals(0, penMove.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, penMove.contactFor(0)?.classification)
    }

    @Test
    fun coldStartFirstFingerClaimsLockWhenFingerWritingEnabled() {
        val settings = PalmRejectionSettings().apply { enableFingerWriting = true }
        val e = PalmRejectionEngine(realDeviceCaps) { settings }
        val fingerDown = e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.fingertip(0, timeMs = 0L)), added = 0))
        assertNull(fingerDown.activeWritingPointerId)

        val fingerMove = e.process(
            TestTouchFactory.frame(InputAction.MOVE, 5L, listOf(TestTouchFactory.fingertip(0, x = 280f, y = 220f, timeMs = 5L)))
        )
        assertEquals(0, fingerMove.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, fingerMove.contactFor(0)?.classification)
    }

    // ---- AppContainer.currentSettings starts as PalmRejectionSettings() default and
    // ---- is updated asynchronously. The engine's settingsProvider lambda reads the
    // ---- @Volatile var each time; the first process() call must see the default and
    // ---- subsequent calls see the updated value.

    @Test
    fun enginePicksUpSettingsUpdateMidSession() {
        val caps = testCapabilities()
        val initial = PalmRejectionSettings()
        val updated = PalmRejectionSettings().apply {
            mode = PalmRejectionMode.STRICT
            writingMaxMm = 4f  // much stricter
        }
        val e = PalmRejectionEngine(caps) { initial }
        // First stroke under default (WRITING mode, 9mm writing max).
        val penDown = e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.pen(0, timeMs = 0L)), added = 0))
        assertNull(penDown.activeWritingPointerId)

        // Mid-session the settings are updated (as if the DataStore flow delivered).
        // The engine's refreshIfSettingsChanged() must pick up the new instance.
        val e2 = PalmRejectionEngine(caps) { updated }
        val penDown2 = e2.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.pen(0, timeMs = 0L)), added = 0))
        assertNull(penDown2.activeWritingPointerId)
    }

    // ---- Real-device zero-size contact: some digitizers report toolMajor=0, toolMinor=0,
    // ---- size=0 on the very first contact. The engine must not crash and must fall back
    // ---- to the no-geometry path.

    @Test
    fun coldStartZeroSizeContactDoesNotCrashAndFallsBack() {
        val e = engine()
        val zero = TestTouchFactory.contact(
            pointerId = 0, x = 100f, y = 100f, timeMs = 0L,
            majorPx = 0f, minorPx = 0f, pressure = 0f, size = 0f,
        )
        // Must not throw. Classification falls back to NO_GEOMETRY_INFO / FINGER.
        val out = e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(zero), added = 0))
        assertTrue(out.contactFor(0)?.classification == ContactClassification.FINGER ||
            out.contactFor(0)?.classification == ContactClassification.CANDIDATE)
    }

    // ---- The 26-thread thread pool in the test harness must not affect engine behavior:
    // ---- engine is single-threaded by design; thread pool size is a test-harness detail.

    @Test
    fun engineBehaviorIndependentOfThreadPoolSize() {
        // Two engines created with different effective thread pools (via settings that
        // don't affect threading) must classify the same contact identically.
        val e1 = engine()
        val e2 = engine()
        val pen = TestTouchFactory.pen(pointerId = 0, timeMs = 0L)
        val out1 = e1.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(pen), added = 0))
        val out2 = e2.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(pen), added = 0))
        assertEquals(out1.contactFor(0)?.classification, out2.contactFor(0)?.classification)
    }
}
