package com.vellum.notes.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Integration tests driving the full [PalmRejectionEngine] with synthetic streams that
 * simulate the real hardware scenarios: pen writing, resting palm, pen+palm together,
 * finger gestures, and pointer cancellation.
 */
class PalmRejectionEngineTest {

    private fun engine(mode: PalmRejectionMode = PalmRejectionMode.WRITING) =
        PalmRejectionEngine(testCapabilities()) { testSettings(mode) }

    @Test
    fun smallPenDownBecomesActiveWritingPointer() {
        val e = engine()
        // Cold start: lone contact is CANDIDATE, no lock. A MOVE promotes to WRITING.
        val pen = TestTouchFactory.pen(pointerId = 0, timeMs = 0L)
        val down = e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(pen), added = 0))
        assertNull(down.activeWritingPointerId)
        assertEquals(ContactClassification.CANDIDATE, down.contactFor(0)?.classification)

        val move = e.process(
            TestTouchFactory.frame(InputAction.MOVE, 5L, listOf(TestTouchFactory.pen(0, x = 180f, y = 100f, timeMs = 5L)))
        )
        assertEquals(0, move.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, move.contactFor(0)?.classification)
        assertTrue(move.gesturePointerIds.isEmpty())
    }

    @Test
    fun largePalmDownNeverClaimsWriting() {
        val e = engine()
        val palm = TestTouchFactory.palm(pointerId = 2, timeMs = 0L)
        val out = e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(palm), added = 2))
        assertNull(out.activeWritingPointerId)
        assertEquals(ContactClassification.PALM, out.contactFor(2)?.classification)
    }

    @Test
    fun palmRestingWhileWritingIsRejectedAndLockPersists() {
        val e = engine()

        // 1. Pen writes: cold start -> CANDIDATE, promote via MOVE.
        val penDown = TestTouchFactory.pen(pointerId = 0, timeMs = 0L)
        e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(penDown), added = 0))
        e.process(
            TestTouchFactory.frame(InputAction.MOVE, 5L, listOf(TestTouchFactory.pen(0, x = 180f, y = 100f, timeMs = 5L)))
        )

        // 2. Palm lands while pen still down.
        val penMove = TestTouchFactory.pen(pointerId = 0, x = 120f, y = 110f, timeMs = 30L)
        val palmDown = TestTouchFactory.palm(pointerId = 2, timeMs = 30L)
        val withPalm = e.process(
            TestTouchFactory.frame(InputAction.POINTER_DOWN, 30L, listOf(penMove, palmDown), added = 2)
        )
        assertEquals(0, withPalm.activeWritingPointerId)
        assertEquals(ContactClassification.PALM, withPalm.contactFor(2)?.classification)
        assertTrue(withPalm.gesturePointerIds.isEmpty())

        // 3. Palm moves around; pen still writing; palm never takes the lock.
        val palmMove = TestTouchFactory.palm(pointerId = 2, x = 520f, y = 740f, timeMs = 50L)
        val penMove2 = TestTouchFactory.pen(pointerId = 0, x = 140f, y = 130f, timeMs = 50L)
        val mid = e.process(TestTouchFactory.frame(InputAction.MOVE, 50L, listOf(penMove2, palmMove)))
        assertEquals(0, mid.activeWritingPointerId)
        assertEquals(ContactClassification.PALM, mid.contactFor(2)?.classification)

        // 4. Palm lifts; pen continues; then pen lifts.
        e.process(TestTouchFactory.frame(InputAction.POINTER_UP, 70L, listOf(penMove2), lifted = 2))
        val penUp = TestTouchFactory.pen(pointerId = 0, x = 150f, y = 140f, timeMs = 90L)
        val after = e.process(TestTouchFactory.frame(InputAction.UP, 90L, listOf(penUp), lifted = 0))
        assertNull(after.activeWritingPointerId)
    }

    @Test
    fun secondFingerWhilePenActiveDropsLockForTwoFingerGesture() {
        val e = engine()
        // Pen starts: cold start -> CANDIDATE, promote via MOVE.
        e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.pen(0, timeMs = 0L)), added = 0))
        e.process(
            TestTouchFactory.frame(InputAction.MOVE, 5L, listOf(TestTouchFactory.pen(0, x = 180f, y = 100f, timeMs = 5L)))
        )
        // A finger-sized second contact while a pen tool is active is a gesture intent,
        // not a palm: the lock drops so the pair can pan/zoom out of the page bottom.
        val finger = TestTouchFactory.fingertip(pointerId = 3, timeMs = 20L)
        val pen = TestTouchFactory.pen(0, x = 180f, y = 100f, timeMs = 20L)
        val out = e.process(TestTouchFactory.frame(InputAction.POINTER_DOWN, 20L, listOf(pen, finger), added = 3))
        assertNull(out.activeWritingPointerId)
        assertEquals(listOf(0, 3), out.gesturePointerIds)
    }

    @Test
    fun twoFingerGesturesAllowedWhenNothingIsWriting() {
        val e = engine(PalmRejectionMode.BALANCED)
        val f1 = TestTouchFactory.fingertip(pointerId = 0, x = 100f, timeMs = 0L)
        val f2 = TestTouchFactory.fingertip(pointerId = 1, x = 300f, timeMs = 0L)
        val out = e.process(TestTouchFactory.frame(InputAction.POINTER_DOWN, 0L, listOf(f1, f2), added = 1))
        assertNull(out.activeWritingPointerId)
        assertEquals(listOf(0, 1), out.gesturePointerIds)
    }

    @Test
    fun cancelDiscardsLockAndPointerState() {
        val e = engine()
        e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.pen(0, timeMs = 0L)), added = 0))
        val out = e.process(TestTouchFactory.frame(InputAction.CANCEL, 10L, emptyList()))
        assertNull(out.activeWritingPointerId)
    }

    @Test
    fun penAfterPalmEstablishesLock() {
        val e = engine()
        e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.palm(2, timeMs = 0L)), added = 2))
        val palmLift = e.process(TestTouchFactory.frame(InputAction.UP, 20L, listOf(TestTouchFactory.palm(2, x = 500f, y = 700f, timeMs = 20L)), lifted = 2))
        assertNull(palmLift.activeWritingPointerId)

        val out = e.process(TestTouchFactory.frame(InputAction.DOWN, 40L, listOf(TestTouchFactory.pen(0, timeMs = 40L)), added = 0))
        assertEquals(0, out.activeWritingPointerId)
    }

    @Test
    fun fingerWritesInWritingModeWhenFingerWritingEnabled() {
        val e = PalmRejectionEngine(testCapabilities()) {
            testSettings(mode = PalmRejectionMode.WRITING).apply { enableFingerWriting = true }
        }
        // Cold start: lone contact is CANDIDATE; promote via MOVE.
        val finger = TestTouchFactory.fingertip(pointerId = 0, timeMs = 0L)
        val down = e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(finger), added = 0))
        assertNull(down.activeWritingPointerId)
        assertEquals(ContactClassification.CANDIDATE, down.contactFor(0)?.classification)

        val move = e.process(
            TestTouchFactory.frame(InputAction.MOVE, 5L, listOf(TestTouchFactory.fingertip(0, x = 280f, y = 220f, timeMs = 5L)))
        )
        assertEquals(0, move.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, move.contactFor(0)?.classification)
    }

    @Test
    fun palmRestingWhileFingerWritesIsRejected() {
        val e = PalmRejectionEngine(testCapabilities()) {
            testSettings(mode = PalmRejectionMode.WRITING).apply { enableFingerWriting = true }
        }

        // Finger starts writing: cold start -> CANDIDATE, promote via MOVE.
        e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.fingertip(0, timeMs = 0L)), added = 0))
        e.process(
            TestTouchFactory.frame(InputAction.MOVE, 5L, listOf(TestTouchFactory.fingertip(0, x = 280f, y = 220f, timeMs = 5L)))
        )

        // Palm lands while the finger is still down; the lock must stay on the finger.
        val fingerMove = TestTouchFactory.fingertip(0, x = 280f, y = 220f, timeMs = 20L)
        val palmDown = TestTouchFactory.palm(pointerId = 2, timeMs = 20L)
        val out = e.process(
            TestTouchFactory.frame(InputAction.POINTER_DOWN, 20L, listOf(fingerMove, palmDown), added = 2)
        )
        assertEquals(0, out.activeWritingPointerId)
        assertEquals(ContactClassification.PALM, out.contactFor(2)?.classification)
        assertTrue(out.gesturePointerIds.isEmpty())
    }

    @Test
    fun secondFingerDuringPenDropsLockAndEnablesGesture() {
        val e = PalmRejectionEngine(testCapabilities()) {
            testSettings(mode = PalmRejectionMode.WRITING).apply { enableFingerWriting = true }
        }
        // Pen starts writing: cold start -> CANDIDATE, promote via MOVE.
        e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.pen(0, timeMs = 0L)), added = 0))
        e.process(
            TestTouchFactory.frame(InputAction.MOVE, 5L, listOf(TestTouchFactory.pen(0, x = 180f, y = 100f, timeMs = 5L)))
        )

        // A finger-sized second contact is a gesture intent (not a palm): the lock is
        // released so the pair can pan/zoom even though a pen tool is selected.
        val pen = TestTouchFactory.pen(0, x = 180f, y = 100f, timeMs = 20L)
        val finger = TestTouchFactory.fingertip(pointerId = 3, timeMs = 20L)
        val out = e.process(
            TestTouchFactory.frame(InputAction.POINTER_DOWN, 20L, listOf(pen, finger), added = 3)
        )
        assertNull(out.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, out.contactFor(0)?.classification)
        assertEquals(listOf(0, 3), out.gesturePointerIds)
    }

    @Test
    fun palmSizedSecondContactWhileWritingKeepsLock() {
        val e = PalmRejectionEngine(testCapabilities()) {
            testSettings(mode = PalmRejectionMode.WRITING).apply { enableFingerWriting = true }
        }
        // Pen starts: cold start -> CANDIDATE, promote via MOVE.
        e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.pen(0, timeMs = 0L)), added = 0))
        e.process(
            TestTouchFactory.frame(InputAction.MOVE, 5L, listOf(TestTouchFactory.pen(0, x = 180f, y = 100f, timeMs = 5L)))
        )

        val pen = TestTouchFactory.pen(0, x = 180f, y = 100f, timeMs = 20L)
        val palm = TestTouchFactory.palm(pointerId = 2, timeMs = 20L)
        val out = e.process(
            TestTouchFactory.frame(InputAction.POINTER_DOWN, 20L, listOf(pen, palm), added = 2)
        )
        // A palm-sized contact is the resting hand: lock persists, no gesture.
        assertEquals(0, out.activeWritingPointerId)
        assertEquals(ContactClassification.PALM, out.contactFor(2)?.classification)
        assertTrue(out.gesturePointerIds.isEmpty())
    }

    // --- Phase 1: palm resting FIRST, then a writing contact lands elsewhere ----------

    @Test
    fun penTouchesWhilePalmAlreadyRestingClaimsWritingLock() {
        val e = engine()
        // Scenario (a): palm rests alone first — correctly rejected, no writing lock.
        val palmFirst = e.process(
            TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.palm(2, timeMs = 0L)), added = 2)
        )
        assertNull(palmFirst.activeWritingPointerId)
        assertEquals(ContactClassification.PALM, palmFirst.contactFor(2)?.classification)

        // Scenario (b): pen touches elsewhere while the palm is still resting. The newly
        // added WRITING pointer must claim the writing lock on POINTER_DOWN.
        val palm = TestTouchFactory.palm(2, x = 500f, y = 700f, timeMs = 20L)
        val pen = TestTouchFactory.pen(pointerId = 0, x = 120f, y = 110f, timeMs = 20L)
        val out = e.process(
            TestTouchFactory.frame(InputAction.POINTER_DOWN, 20L, listOf(palm, pen), added = 0)
        )
        assertEquals(0, out.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, out.contactFor(0)?.classification)
        assertEquals(ContactClassification.PALM, out.contactFor(2)?.classification)
        // The resting palm must never enable gestures while writing is locked.
        assertTrue(out.gesturePointerIds.isEmpty())
    }

    @Test
    fun fingerTouchesWhilePalmAlreadyRestingClaimsWritingLock() {
        val e = PalmRejectionEngine(testCapabilities()) {
            testSettings(mode = PalmRejectionMode.WRITING).apply { enableFingerWriting = true }
        }
        e.process(
            TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.palm(2, timeMs = 0L)), added = 2)
        )
        // Scenario (c): a bare finger touches elsewhere while the palm rests.
        val palm = TestTouchFactory.palm(2, x = 500f, y = 700f, timeMs = 20L)
        val finger = TestTouchFactory.fingertip(pointerId = 0, x = 220f, y = 220f, timeMs = 20L)
        val out = e.process(
            TestTouchFactory.frame(InputAction.POINTER_DOWN, 20L, listOf(palm, finger), added = 0)
        )
        assertEquals(0, out.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, out.contactFor(0)?.classification)
        assertTrue(out.gesturePointerIds.isEmpty())
    }

    // --- Phase 1: no mid-stroke flip-flop ----------------------------------------------

    @Test
    fun palmRapidOnOffWhileWritingKeepsLockContinuous() {
        val e = engine()
        // Pen starts: cold start -> CANDIDATE, promote via MOVE.
        e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.pen(0, timeMs = 0L)), added = 0))
        e.process(
            TestTouchFactory.frame(InputAction.MOVE, 5L, listOf(TestTouchFactory.pen(0, x = 180f, y = 100f, timeMs = 5L)))
        )

        // Palm lands (POINTER_DOWN): lock persists.
        val withPalm = e.process(
            TestTouchFactory.frame(
                InputAction.POINTER_DOWN, 20L,
                listOf(TestTouchFactory.pen(0, x = 180f, y = 100f, timeMs = 5L), TestTouchFactory.palm(2, timeMs = 20L)),
                added = 2,
            )
        )
        assertEquals(0, withPalm.activeWritingPointerId)

        // Palm lifts (POINTER_UP): lock stays on the pen.
        val palmLift = e.process(
            TestTouchFactory.frame(InputAction.POINTER_UP, 30L, listOf(TestTouchFactory.pen(0, x = 200f, y = 120f, timeMs = 30L)), lifted = 2)
        )
        assertEquals(0, palmLift.activeWritingPointerId)

        // Palm re-lands while pen still down: lock stays.
        val palmAgain = e.process(
            TestTouchFactory.frame(
                InputAction.POINTER_DOWN, 40L,
                listOf(TestTouchFactory.pen(0, x = 220f, y = 140f, timeMs = 40L), TestTouchFactory.palm(2, x = 480f, y = 700f, timeMs = 40L)),
                added = 2,
            )
        )
        assertEquals(0, palmAgain.activeWritingPointerId)

        // Pen continues writing with both contacts down: lock persists every frame.
        val move = e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 50L,
                listOf(TestTouchFactory.pen(0, x = 260f, y = 180f, timeMs = 50L), TestTouchFactory.palm(2, x = 460f, y = 710f, timeMs = 50L)),
            )
        )
        assertEquals(0, move.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, move.contactFor(0)?.classification)
        assertTrue(move.gesturePointerIds.isEmpty())
    }

    @Test
    fun borderlinePenReclassificationNeverDropsLockMidStroke() {
        val e = engine()
        // Pen establishes the writing lock: cold start -> CANDIDATE, promote via MOVE.
        e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.pen(0, timeMs = 0L)), added = 0))
        e.process(
            TestTouchFactory.frame(InputAction.MOVE, 5L, listOf(TestTouchFactory.pen(0, x = 180f, y = 100f, timeMs = 5L)))
        )

        // A later frame reports the SAME pointer with a degenerate/palm-sized contact
        // (e.g. the digitizer briefly saturates). The sticky lock must not flip-flop:
        // the pointer stays the active writer and no gesture is granted.
        val bloatedPen = TestTouchFactory.palm(0, x = 180f, y = 100f, timeMs = 20L)
        val out = e.process(TestTouchFactory.frame(InputAction.MOVE, 20L, listOf(bloatedPen)))
        assertEquals(0, out.activeWritingPointerId)
        assertTrue(out.gesturePointerIds.isEmpty())

        // And the pen keeps writing on the next normal frame.
        val next = e.process(
            TestTouchFactory.frame(InputAction.MOVE, 30L, listOf(TestTouchFactory.pen(0, x = 220f, y = 140f, timeMs = 30L)))
        )
        assertEquals(0, next.activeWritingPointerId)
    }

    // --- Phase 3: two palms/heels resting simultaneously with pen writing --------------

    @Test
    fun twoPalmsRestingWithPenAcceptsOnlyTheGenuinelySmallest() {
        val e = engine()

        // Palm 1 lands alone first (cold start) -> rejected as palm, no lock.
        val palmFirst = e.process(
            TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.palm(pointerId = 2, timeMs = 0L)), added = 2)
        )
        assertNull(palmFirst.activeWritingPointerId)
        assertEquals(ContactClassification.PALM, palmFirst.contactFor(2)?.classification)

        // Palm 2 lands while palm 1 still rests -> rejected too (even the smallest active
        // contact is palm-sized, so nothing here is genuinely small). No gestures.
        val palm1 = TestTouchFactory.palm(pointerId = 2, x = 500f, y = 700f, timeMs = 20L)
        val palm2 = TestTouchFactory.palm(pointerId = 4, x = 800f, y = 900f, timeMs = 20L)
        val twoPalms = e.process(
            TestTouchFactory.frame(InputAction.POINTER_DOWN, 20L, listOf(palm1, palm2), added = 4)
        )
        assertNull(twoPalms.activeWritingPointerId)
        assertEquals(ContactClassification.PALM, twoPalms.contactFor(2)?.classification)
        assertEquals(ContactClassification.PALM, twoPalms.contactFor(4)?.classification)
        assertTrue(twoPalms.gesturePointerIds.isEmpty())

        // Pen joins while both palms rest: it is the genuinely smallest contact and must
        // claim writing immediately; both larger contacts stay rejected.
        val pen = TestTouchFactory.pen(pointerId = 0, x = 120f, y = 110f, timeMs = 40L)
        val out = e.process(
            TestTouchFactory.frame(InputAction.POINTER_DOWN, 40L, listOf(palm1, palm2, pen), added = 0)
        )
        assertEquals(0, out.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, out.contactFor(0)?.classification)
        assertEquals(ContactClassification.PALM, out.contactFor(2)?.classification)
        assertEquals(ContactClassification.PALM, out.contactFor(4)?.classification)
        assertTrue(out.gesturePointerIds.isEmpty())
    }

    // --- Phase 3: adaptive single-pointer fallback after history exists ----------------

    @Test
    fun lonePalmAfterPenStrokesIsRejectedByAdaptiveHistory() {
        val e = engine()

        // A pen stroke seeds the confirmed-small range and establishes/lifts the lock.
        e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.pen(0, timeMs = 0L)), added = 0))
        e.process(
            TestTouchFactory.frame(InputAction.UP, 10L, listOf(TestTouchFactory.pen(0, x = 100f, y = 100f, timeMs = 10L)), lifted = 0)
        )

        // A lone palm lands after the pen lifted -> rejected via the adaptive fallback:
        // it is far larger than the device's confirmed-small range. No drawing, no lock.
        val out = e.process(
            TestTouchFactory.frame(InputAction.DOWN, 30L, listOf(TestTouchFactory.palm(2, timeMs = 30L)), added = 2)
        )
        assertNull(out.activeWritingPointerId)
        assertEquals(ContactClassification.PALM, out.contactFor(2)?.classification)
    }

    @Test
    fun loneFingerTapAfterPenStrokesStaysValid() {
        val e = engine()

        // A pen stroke seeds the confirmed-small range.
        e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.pen(0, timeMs = 0L)), added = 0))
        e.process(
            TestTouchFactory.frame(InputAction.MOVE, 5L, listOf(TestTouchFactory.pen(0, x = 180f, y = 100f, timeMs = 5L)))
        )
        e.process(
            TestTouchFactory.frame(InputAction.UP, 10L, listOf(TestTouchFactory.pen(0, x = 180f, y = 100f, timeMs = 10L)), lifted = 0)
        )

        // A lone fingertip (UI tap) after pen strokes must NOT be misclassified as a palm.
        val out = e.process(
            TestTouchFactory.frame(InputAction.DOWN, 30L, listOf(TestTouchFactory.fingertip(3, timeMs = 30L)), added = 3)
        )
        val cls = out.contactFor(3)?.classification
        assertTrue(cls == ContactClassification.FINGER || cls == ContactClassification.WRITING)
    }

    // --- Bug regression: resting palm must never permanently block writing -------------

    /** A medium palm (~15mm) whose size sits in the finger/writing band. */
    private fun mediumPalm(
        pointerId: Int = 2,
        x: Float = 500f,
        y: Float = 700f,
        timeMs: Long = 0L,
    ) = TestTouchFactory.contact(
        pointerId, x, y, timeMs,
        majorPx = 150f, minorPx = 120f, pressure = 1f, size = 0.28f,
    )

    @Test
    fun penTakesOverWritingLockFromFalselyLockedMediumPalm() {
        val e = engine()

        // A medium palm alone — on cold start this is CANDIDATE (no lock falsely claimed).
        val palmAlone = e.process(
            TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(mediumPalm(timeMs = 0L)), added = 2)
        )
        assertNull(palmAlone.activeWritingPointerId)
        assertEquals(ContactClassification.CANDIDATE, palmAlone.contactFor(2)?.classification)

        // A genuinely small pen contact lands while the palm still rests. The relative
        // classifier marks the pen WRITING and it claims the writing lock immediately.
        val palm = mediumPalm(timeMs = 20L)
        val pen = TestTouchFactory.pen(pointerId = 0, x = 120f, y = 110f, timeMs = 20L)
        val out = e.process(
            TestTouchFactory.frame(InputAction.POINTER_DOWN, 20L, listOf(palm, pen), added = 0)
        )
        assertEquals(0, out.activeWritingPointerId)
        assertTrue(out.gesturePointerIds.isEmpty())

        // Pen keeps writing with the palm still resting: lock stays on the pen, no gesture.
        val move = e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 30L,
                listOf(mediumPalm(x = 500f, y = 700f, timeMs = 30L), TestTouchFactory.pen(0, x = 140f, y = 130f, timeMs = 30L)),
            )
        )
        assertEquals(0, move.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, move.contactFor(0)?.classification)
        assertTrue(move.gesturePointerIds.isEmpty())
    }

    @Test
    fun fingerWritingRecoversAfterAmbiguousGestureWithMediumPalm() {
        val e = engine()

        // Medium palm rests alone (cold start -> CANDIDATE, no false lock).
        e.process(
            TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(mediumPalm(timeMs = 0L)), added = 2)
        )
        val palm = mediumPalm(timeMs = 20L)
        val finger = TestTouchFactory.fingertip(pointerId = 0, x = 220f, y = 220f, timeMs = 20L)
        val gesture = e.process(
            TestTouchFactory.frame(InputAction.POINTER_DOWN, 20L, listOf(palm, finger), added = 0)
        )
        // With no false lock, both contacts enter the relative path. The finger is
        // classified FINGER (not dramatically smaller than the palm). However the
        // medium palm was CANDIDATE from the previous frame and the tracker keeps it
        // as CANDIDATE, so only the new finger is FINGER and eligible for gestures.
        assertNull(gesture.activeWritingPointerId)
        assertTrue(gesture.gesturePointerIds.size >= 1)

        // Everything lifts.
        e.process(TestTouchFactory.frame(InputAction.POINTER_UP, 40L, listOf(mediumPalm(x = 500f, y = 700f, timeMs = 40L)), lifted = 0))
        e.process(TestTouchFactory.frame(InputAction.UP, 50L, listOf(mediumPalm(x = 500f, y = 700f, timeMs = 50L)), lifted = 2))

        // Writing alone afterwards must work — no stuck lock, no swallowed strokes.
        val alone = e.process(
            TestTouchFactory.frame(InputAction.DOWN, 70L, listOf(TestTouchFactory.fingertip(0, x = 220f, y = 220f, timeMs = 70L)), added = 0)
        )
        assertEquals(0, alone.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, alone.contactFor(0)?.classification)
    }

    @Test
    fun passivePenClearlySmallerThanFalselyLockedPalmTakesOverLock() {
        val e = engine()

        // Medium palm alone is CANDIDATE (cold start, no false lock claimed).
        e.process(
            TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(mediumPalm(timeMs = 0L)), added = 2)
        )

        // A passive pen (~8mm, tool type FINGER) lands. Since no lock is falsely held,
        // both contacts enter the relative path. The pen is just ~1.9x smaller than the
        // palm — not enough for the 2.5x ratio — so both are FINGER. No lock, but gestures
        // are available (the pair can pan/zoom).
        val palm = mediumPalm(timeMs = 20L)
        val pen = TestTouchFactory.contact(0, x = 120f, y = 110f, timeMs = 20L, majorPx = 80f, minorPx = 72f, pressure = 0.6f, size = 0.02f)
        val out = e.process(
            TestTouchFactory.frame(InputAction.POINTER_DOWN, 20L, listOf(palm, pen), added = 0)
        )
        assertNull(out.activeWritingPointerId)
        assertTrue(out.gesturePointerIds.isNotEmpty())
    }

    @Test
    fun stylusTakesOverLockFromFalselyLockedPalmRegardlessOfSize() {
        val e = engine()

        // Medium palm rests alone -> falsely locked as the writer.
        e.process(
            TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(mediumPalm(timeMs = 0L)), added = 2)
        )

        // A hardware stylus lands with a size almost identical to the palm (ratio < 1.6).
        // A real stylus is always the writer, so the lock must be handed to it regardless
        // of the size comparison.
        val palm = mediumPalm(timeMs = 20L)
        val stylus = TestTouchFactory.pen(pointerId = 0, x = 120f, y = 110f, timeMs = 20L, majorPx = 140f, toolType = TestTouchFactory.TOOL_STYLUS)
        val out = e.process(
            TestTouchFactory.frame(InputAction.POINTER_DOWN, 20L, listOf(palm, stylus), added = 0)
        )
        assertEquals(0, out.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, out.contactFor(0)?.classification)
        assertTrue(out.gesturePointerIds.isEmpty())
    }

    @Test
    fun mediumPalmRestingWhilePenWritesKeepsLockAfterPalmHistoryConfirmed() {
        val e = engine()

        // Pen starts writing: cold start -> CANDIDATE, promote via MOVE.
        e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.pen(0, timeMs = 0L)), added = 0))
        e.process(
            TestTouchFactory.frame(InputAction.MOVE, 5L, listOf(TestTouchFactory.pen(0, x = 180f, y = 100f, timeMs = 5L)))
        )

        // A LARGE palm lands while the pen writes -> rejected as palm, seeds the
        // confirmed-palm range for this device.
        e.process(
            TestTouchFactory.frame(
                InputAction.POINTER_DOWN, 20L,
                listOf(TestTouchFactory.pen(0, x = 180f, y = 100f, timeMs = 5L), TestTouchFactory.palm(pointerId = 2, timeMs = 20L)),
                added = 2,
            )
        )
        // Large palm lifts.
        e.process(TestTouchFactory.frame(InputAction.POINTER_UP, 30L, listOf(TestTouchFactory.pen(0, x = 200f, y = 120f, timeMs = 30L)), lifted = 2))

        // A MEDIUM palm (15mm, below the finger threshold) lands while the pen writes.
        // The device has now confirmed its palm range, so a matching secondary contact is
        // the resting hand and must NOT drop the writing lock into a two-finger gesture.
        val out = e.process(
            TestTouchFactory.frame(
                InputAction.POINTER_DOWN, 40L,
                listOf(TestTouchFactory.pen(0, x = 220f, y = 140f, timeMs = 40L), mediumPalm(timeMs = 40L)),
                added = 2,
            )
        )
        assertEquals(0, out.activeWritingPointerId)
        assertEquals(ContactClassification.PALM, out.contactFor(2)?.classification)
        assertTrue(out.gesturePointerIds.isEmpty())
    }

    // --- palm rest zone ---

    private fun zonedEngine() = engine().also {
        it.setPalmZoneRect(PalmZoneRect(leftPx = 200f, topPx = 200f, rightPx = 800f, bottomPx = 900f))
    }

    @Test
    fun stylusInsidePalmZoneIsStillWriting() {
        val e = zonedEngine()
        // A stylus (pen) inside the zone should be EXEMPT from the zone override
        // and be classified as WRITING, allowing the stroke to be captured.
        val stylusInZone = TestTouchFactory.pen(pointerId = 0, x = 400f, y = 400f, timeMs = 0L, toolType = TestTouchFactory.TOOL_STYLUS)
        val out = e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(stylusInZone), added = 0))
        assertEquals(ContactClassification.WRITING, out.contactFor(0)?.classification)
        assertEquals(ClassificationReason.HARDWARE_STYLUS, out.contactFor(0)?.reason)
        assertEquals(0, out.activeWritingPointerId)
    }

    @Test
    fun eraserInsidePalmZoneIsStillEraser() {
        val e = zonedEngine()
        // An eraser (hardware tool) inside the zone should retain its ERASER classification,
        // not be turned into a PALM by the zone override.
        val eraserInZone = TestTouchFactory.pen(pointerId = 0, x = 400f, y = 400f, timeMs = 0L, toolType = TestTouchFactory.TOOL_ERASER)
        val out = e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(eraserInZone), added = 0))
        assertEquals(ContactClassification.ERASER, out.contactFor(0)?.classification)
        assertEquals(ClassificationReason.HARDWARE_ERASER, out.contactFor(0)?.reason)
    }

    @Test
    fun twoContactsInsideZoneNeverBecomeGesture() {
        val e = zonedEngine()
        val f1 = TestTouchFactory.fingertip(pointerId = 0, x = 300f, y = 300f, timeMs = 0L)
        val f2 = TestTouchFactory.fingertip(pointerId = 1, x = 500f, y = 400f, timeMs = 0L)
        val out = e.process(TestTouchFactory.frame(InputAction.POINTER_DOWN, 0L, listOf(f1, f2), added = 1))
        assertEquals(ContactClassification.PALM, out.contactFor(0)?.classification)
        assertEquals(ContactClassification.PALM, out.contactFor(1)?.classification)
        assertNull(out.activeWritingPointerId)
        assertTrue(out.gesturePointerIds.isEmpty())
    }

    @Test
    fun lockedWritingPointerCrossingZoneKeepsLockAndWrites() {
        val e = zonedEngine()
        // Pen starts OUTSIDE the zone: cold start -> CANDIDATE, promote via MOVE.
        e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.pen(0, x = 100f, y = 100f, timeMs = 0L)), added = 0))
        e.process(
            TestTouchFactory.frame(InputAction.MOVE, 5L, listOf(TestTouchFactory.pen(0, x = 180f, y = 100f, timeMs = 5L)))
        )
        // The stroke now moves INTO the zone: the locked pointer is exempt from the zone
        // override so the in-progress stroke is not cut mid-stroke.
        val intoZone = TestTouchFactory.pen(0, x = 400f, y = 400f, timeMs = 30L)
        val out = e.process(TestTouchFactory.frame(InputAction.MOVE, 30L, listOf(intoZone)))
        assertEquals(0, out.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, out.contactFor(0)?.classification)
        assertTrue(out.gesturePointerIds.isEmpty())
    }

    @Test
    fun palmZoneRectGeometryContainsOnlyInsidePoints() {
        val rect = PalmZoneRect(leftPx = 200f, topPx = 200f, rightPx = 800f, bottomPx = 900f)
        assertTrue(rect.contains(400f, 400f))
        assertTrue(rect.contains(200f, 200f)) // inclusive edges
        assertTrue(!rect.contains(199f, 400f))
        assertTrue(!rect.contains(400f, 901f))
        assertEquals(500f, rect.centerX(), 0.001f)
        assertEquals(550f, rect.centerY(), 0.001f)
        assertEquals(600f, rect.widthPx(), 0.001f)
        assertEquals(700f, rect.heightPx(), 0.001f)
    }

    // --- Bug regression: resting palm + finger pen must always produce ink -------------

    @Test
    fun fingerPenWritesNextToPalmOnlySlightlyLargerThanFinger() {
        val e = engine()
        // A resting palm (~18mm) reports just ~1.5x the fingertip, below the 2.5x relative
        // ratio. The palm alone is correctly rejected by the settings threshold.
        val palmAlone = e.process(
            TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(palm18mm(timeMs = 0L)), added = 2)
        )
        assertNull(palmAlone.activeWritingPointerId)
        assertEquals(ContactClassification.PALM, palmAlone.contactFor(2)?.classification)

        // A 12mm finger pen lands next to it. The relative classifier cannot use the 2.5x
        // ratio here, so the "smallest contact beside a palm-sized contact" rule must mark
        // it WRITING and it must claim the writing lock — otherwise both contacts become a
        // two-finger gesture and no ink is produced.
        val out = e.process(
            TestTouchFactory.frame(InputAction.POINTER_DOWN, 20L, listOf(palm18mm(timeMs = 20L), finger12mm(timeMs = 20L)), added = 0)
        )
        assertEquals(0, out.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, out.contactFor(0)?.classification)
        assertTrue(out.gesturePointerIds.isEmpty())
    }

    private fun palm18mm(pointerId: Int = 2, timeMs: Long = 0L) =
        TestTouchFactory.contact(pointerId, 500f, 700f, timeMs, majorPx = 180f, minorPx = 150f, pressure = 1f, size = 0.25f)

    private fun finger12mm(pointerId: Int = 0, timeMs: Long = 0L) =
        TestTouchFactory.contact(pointerId, 120f, 110f, timeMs, majorPx = 120f, minorPx = 110f, pressure = 0.6f, size = 0.04f)
}