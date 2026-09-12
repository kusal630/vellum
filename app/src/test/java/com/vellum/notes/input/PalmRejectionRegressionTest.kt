package com.vellum.notes.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the palm-rejection bug-fix session. Each test maps to a
 * required scenario (a)-(h) or to a confirmed logic bug:
 *
 *  (a) palm down first, then pen writes
 *  (b) pen writing, palm joins mid-stroke
 *  (c) pen lifts while palm remains
 *  (d) large contact area slow touch = palm
 *  (e) small fast touch = pen
 *  (f) palm rest zone touch never draws
 *  (g) two-finger pinch/pan never rejected
 *  (h) state resets on ACTION_UP/CANCEL (incl. pointer-id reuse)
 */
class PalmRejectionRegressionTest {

    private fun engine(mode: PalmRejectionMode = PalmRejectionMode.WRITING) =
        PalmRejectionEngine(testCapabilities()) { testSettings(mode) }

    // --- (a) palm down first, then pen writes ---------------------------------

    @Test
    fun palmDownFirstThenPenWrites() {
        val e = engine()
        val palmFirst = e.process(
            TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.palm(2, timeMs = 0L)), added = 2)
        )
        assertNull(palmFirst.activeWritingPointerId)
        assertEquals(ContactClassification.PALM, palmFirst.contactFor(2)?.classification)

        val out = e.process(
            TestTouchFactory.frame(
                InputAction.POINTER_DOWN, 20L,
                listOf(
                    TestTouchFactory.palm(2, x = 500f, y = 700f, timeMs = 20L),
                    TestTouchFactory.pen(0, x = 120f, y = 110f, timeMs = 20L),
                ),
                added = 0,
            )
        )
        assertEquals(0, out.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, out.contactFor(0)?.classification)
        assertEquals(ContactClassification.PALM, out.contactFor(2)?.classification)
        assertTrue(out.gesturePointerIds.isEmpty())
    }

    // --- (b) pen writing, palm joins mid-stroke --------------------------------

    @Test
    fun penWritingPalmJoinsMidStrokeKeepsLock() {
        val e = engine()
        // Pen starts: cold start -> CANDIDATE, promote via MOVE.
        e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.pen(0, timeMs = 0L)), added = 0))
        e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 5L,
                listOf(TestTouchFactory.pen(0, x = 160f, y = 150f, timeMs = 5L)),
            )
        )

        val out = e.process(
            TestTouchFactory.frame(
                InputAction.POINTER_DOWN, 20L,
                listOf(
                    TestTouchFactory.pen(0, x = 160f, y = 150f, timeMs = 5L),
                    TestTouchFactory.palm(2, timeMs = 20L),
                ),
                added = 2,
            )
        )
        assertEquals(0, out.activeWritingPointerId)
        assertEquals(ContactClassification.PALM, out.contactFor(2)?.classification)
        assertTrue(out.gesturePointerIds.isEmpty())

        // Palm drifts mid-stroke; the pen still owns the lock.
        val move = e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 40L,
                listOf(
                    TestTouchFactory.pen(0, x = 200f, y = 190f, timeMs = 5L),
                    TestTouchFactory.palm(2, x = 520f, y = 740f, timeMs = 40L),
                ),
            )
        )
        assertEquals(0, move.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, move.contactFor(0)?.classification)
    }

    // --- (c) pen lifts while palm remains --------------------------------------

    @Test
    fun penLiftsWhilePalmRemainsPalmStaysRejected() {
        val e = engine()
        e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.pen(0, timeMs = 0L)), added = 0))
        e.process(
            TestTouchFactory.frame(
                InputAction.POINTER_DOWN, 20L,
                listOf(
                    TestTouchFactory.pen(0, x = 120f, y = 110f, timeMs = 20L),
                    TestTouchFactory.palm(2, timeMs = 20L),
                ),
                added = 2,
            )
        )

        // Pen lifts FIRST while the palm is still down: the lock releases, the palm
        // stays rejected, and no gesture is granted to a lone palm.
        val penGone = e.process(
            TestTouchFactory.frame(
                InputAction.POINTER_UP, 30L,
                listOf(TestTouchFactory.palm(2, x = 500f, y = 700f, timeMs = 30L)),
                lifted = 0,
            )
        )
        assertNull(penGone.activeWritingPointerId)
        assertEquals(ContactClassification.PALM, penGone.contactFor(2)?.classification)
        assertTrue(penGone.gesturePointerIds.isEmpty())

        // Palm alone keeps moving: still rejected, still no lock/gesture.
        val palmAlone = e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 50L,
                listOf(TestTouchFactory.palm(2, x = 520f, y = 740f, timeMs = 50L)),
            )
        )
        assertNull(palmAlone.activeWritingPointerId)
        assertEquals(ContactClassification.PALM, palmAlone.contactFor(2)?.classification)
        assertTrue(palmAlone.gesturePointerIds.isEmpty())

        // Palm lifts; a fresh pen stroke works immediately (no stuck lock).
        e.process(
            TestTouchFactory.frame(
                InputAction.UP, 70L,
                listOf(TestTouchFactory.palm(2, x = 520f, y = 740f, timeMs = 70L)),
                lifted = 2,
            )
        )
        val fresh = e.process(
            TestTouchFactory.frame(InputAction.DOWN, 90L, listOf(TestTouchFactory.pen(0, x = 130f, y = 120f, timeMs = 90L)), added = 0)
        )
        assertEquals(0, fresh.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, fresh.contactFor(0)?.classification)
    }

    // --- (d) large contact area slow touch = palm -------------------------------

    @Test
    fun largeSlowTouchIsPalm() {
        val e = engine()
        val down = e.process(
            TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.palm(2, timeMs = 0L)), added = 2)
        )
        assertEquals(ContactClassification.PALM, down.contactFor(2)?.classification)
        assertNull(down.activeWritingPointerId)

        // Slow drift (10px = 1mm, below the stroke gate): still a palm, never a writer.
        val drift = e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 60L,
                listOf(TestTouchFactory.palm(2, x = 510f, y = 705f, timeMs = 60L)),
            )
        )
        assertEquals(ContactClassification.PALM, drift.contactFor(2)?.classification)
        assertNull(drift.activeWritingPointerId)
        assertTrue(drift.gesturePointerIds.isEmpty())
    }

    // --- (e) small fast touch = pen ----------------------------------------------

    @Test
    fun smallFastTouchWrites() {
        val e = engine()
        // Cold start: lone contact is CANDIDATE, no lock claimed.
        val down = e.process(
            TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.pen(0, timeMs = 0L)), added = 0)
        )
        assertNull(down.activeWritingPointerId)
        assertEquals(ContactClassification.CANDIDATE, down.contactFor(0)?.classification)

        // Fast stroke motion (80px = 8mm in 10ms): promotes to WRITING and claims the lock.
        val fast = e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 10L,
                listOf(TestTouchFactory.pen(0, x = 180f, y = 100f, timeMs = 10L)),
            )
        )
        assertEquals(0, fast.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, fast.contactFor(0)?.classification)
    }

    // --- (f) palm rest zone touch never draws -------------------------------------

    @Test
    fun palmZoneFingerNeverDrawsOrGestures() {
        val e = engine().also {
            it.setPalmZoneRect(PalmZoneRect(leftPx = 200f, topPx = 200f, rightPx = 800f, bottomPx = 900f))
        }
        val fingerInZone = TestTouchFactory.fingertip(pointerId = 0, x = 400f, y = 400f, timeMs = 0L)
        val out = e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(fingerInZone), added = 0))
        assertEquals(ContactClassification.PALM, out.contactFor(0)?.classification)
        assertEquals(ClassificationReason.IN_PALM_ZONE, out.contactFor(0)?.reason)
        assertNull(out.activeWritingPointerId)
        assertTrue(out.gesturePointerIds.isEmpty())
    }

    // --- (g) two-finger pinch/pan never rejected -----------------------------------

    @Test
    fun twoFingerPinchSpreadNeverRejected() {
        val e = engine(PalmRejectionMode.BALANCED)
        e.process(
            TestTouchFactory.frame(
                InputAction.DOWN, 0L,
                listOf(TestTouchFactory.fingertip(0, x = 400f, y = 400f, timeMs = 0L)),
                added = 0,
            )
        )
        val down = e.process(
            TestTouchFactory.frame(
                InputAction.POINTER_DOWN, 10L,
                listOf(
                    TestTouchFactory.fingertip(0, x = 400f, y = 400f, timeMs = 0L),
                    TestTouchFactory.fingertip(1, x = 500f, y = 400f, timeMs = 10L),
                ),
                added = 1,
            )
        )
        assertNull(down.activeWritingPointerId)

        // Fingers spread apart (pinch-zoom): both stay FINGER and drive the gesture.
        val spread = e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 30L,
                listOf(
                    TestTouchFactory.fingertip(0, x = 350f, y = 400f, timeMs = 0L),
                    TestTouchFactory.fingertip(1, x = 550f, y = 400f, timeMs = 10L),
                ),
            )
        )
        assertNull(spread.activeWritingPointerId)
        assertEquals(ContactClassification.FINGER, spread.contactFor(0)?.classification)
        assertEquals(ContactClassification.FINGER, spread.contactFor(1)?.classification)
        assertEquals(listOf(0, 1), spread.gesturePointerIds)
    }

    // --- (h) state resets on UP/CANCEL + pointer-id reuse --------------------------

    @Test
    fun pointerIdReuseAfterUpIsFreshTouch() {
        val e = engine()
        // Pen DOWN (cold start -> CANDIDATE), then MOVE enough to promote to WRITING.
        e.process(
            TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.pen(0, x = 100f, y = 100f, timeMs = 0L)), added = 0)
        )
        e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 5L,
                listOf(TestTouchFactory.pen(0, x = 200f, y = 100f, timeMs = 5L)),
            )
        )
        e.process(
            TestTouchFactory.frame(
                InputAction.UP, 10L,
                listOf(TestTouchFactory.pen(0, x = 200f, y = 100f, timeMs = 10L)),
                lifted = 0,
            )
        )
        val reused = e.process(
            TestTouchFactory.frame(InputAction.DOWN, 500L, listOf(TestTouchFactory.pen(0, x = 100f, y = 100f, timeMs = 500L)), added = 0)
        )
        assertEquals(ContactClassification.WRITING, reused.contactFor(0)?.classification)
        assertEquals(0, reused.activeWritingPointerId)
    }

    @Test
    fun cancelWithContactsResetsRestingTracker() {
        val e = engine()
        fun fingertip(id: Int, x: Float, y: Float, t: Long) = TestTouchFactory.fingertip(id, x, y, t)
        e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(fingertip(1, 150f, 150f, 0L)), added = 1))
        e.process(
            TestTouchFactory.frame(
                InputAction.POINTER_DOWN, 10L,
                listOf(fingertip(1, 150f, 150f, 0L), fingertip(3, 500f, 200f, 10L)), added = 3,
            )
        )
        e.process(
            TestTouchFactory.frame(
                InputAction.POINTER_DOWN, 20L,
                listOf(fingertip(1, 150f, 150f, 0L), fingertip(3, 500f, 200f, 10L), fingertip(4, 800f, 250f, 20L)),
                added = 4,
            )
        )
        val resting = e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 400L,
                listOf(fingertip(1, 150f, 150f, 0L), fingertip(3, 500f, 200f, 10L), fingertip(4, 800f, 250f, 20L)),
            )
        )
        assertEquals(ContactClassification.RESTING, resting.contactFor(1)?.classification)

        // Real ACTION_CANCEL still lists the contacts; the tracker must not leak
        // RESTING state into the next gesture even when an id is reused.
        e.process(
            TestTouchFactory.frame(
                InputAction.CANCEL, 410L,
                listOf(fingertip(1, 150f, 150f, 0L), fingertip(3, 500f, 200f, 10L), fingertip(4, 800f, 250f, 20L)),
            )
        )
        // After CANCEL the history is reset; the fresh DOWN is CANDIDATE (cold start).
        // A MOVE with enough travel promotes it to WRITING and claims the lock.
        e.process(
            TestTouchFactory.frame(
                InputAction.DOWN, 500L,
                listOf(TestTouchFactory.pen(1, x = 300f, y = 500f, timeMs = 500L)),
                added = 1,
            )
        )
        val freshMove = e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 510L,
                listOf(TestTouchFactory.pen(1, x = 400f, y = 500f, timeMs = 510L)),
            )
        )
        assertEquals(1, freshMove.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, freshMove.contactFor(1)?.classification)
    }

    // --- Bug: adaptive valid-range must never promote a lone palm to WRITING -------

    @Test
    fun loneLargeContactAfterFingerUseNeverWrites() {
        val e = PalmRejectionEngine(testCapabilities()) {
            testSettings(mode = PalmRejectionMode.WRITING).apply { enableFingerWriting = true }
        }
        // A finger stroke seeds a large "confirmed small" average (~11mm).
        e.process(
            TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.fingertip(0, x = 300f, y = 500f, timeMs = 0L)), added = 0)
        )
        e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 5L,
                listOf(TestTouchFactory.fingertip(0, x = 400f, y = 500f, timeMs = 5L)),
            )
        )
        e.process(
            TestTouchFactory.frame(
                InputAction.UP, 10L,
                listOf(TestTouchFactory.fingertip(0, x = 400f, y = 500f, timeMs = 10L)),
                lifted = 0,
            )
        )
        // A lone 20mm contact is within 2.5x of the finger average but far above the
        // writing cutoff: it must never claim the writing lock.
        val big = TestTouchFactory.contact(2, 500f, 700f, 30L, majorPx = 200f, minorPx = 180f, pressure = 1f, size = 0.2f)
        val out = e.process(TestTouchFactory.frame(InputAction.DOWN, 30L, listOf(big), added = 2))
        assertTrue(out.contactFor(2)?.classification != ContactClassification.WRITING)
        assertNull(out.activeWritingPointerId)
    }

    // --- Bug: size-derived ellipse is not tool geometry ------------------------------

    @Test
    fun sizeDerivedEllipseIsNotToolGeometry() {
        val normalizer = InputNormalizer(testCapabilities(pxPerMm = 10f))
        val fromSize = normalizer.normalize(
            TestTouchFactory.contact(0, 0f, 0f, 0L, majorPx = 0f, minorPx = 0f, size = 0.2f)
        )
        assertEquals(false, fromSize.hasGeometry)
        assertEquals(true, fromSize.hasSize)

        val degenerate = normalizer.normalize(
            TestTouchFactory.contact(0, 0f, 0f, 0L, majorPx = 0f, minorPx = 0f, size = 1.0f)
        )
        assertEquals(false, degenerate.hasGeometry)
        assertEquals(false, degenerate.hasSize)
        assertEquals(0f, degenerate.maxDimMm, 0.01f)

        // Implausible tool axis (500mm >> 120mm max) with a usable size fallback: the
        // geometry must be discarded, not trusted.
        val implausible = normalizer.normalize(
            TestTouchFactory.contact(0, 0f, 0f, 0L, majorPx = 5000f, minorPx = 4000f, size = 0.02f)
        )
        assertEquals(false, implausible.hasGeometry)
        assertEquals(true, implausible.hasSize)
    }

    // --- Bug: two resting fingers sweeping together are never two writers --------------

    @Test
    fun twoRestingFingersMovingFastTogetherNeverBothWrite() {
        val e = engine()
        fun fingertip(id: Int, x: Float, y: Float, t: Long) = TestTouchFactory.fingertip(id, x, y, t)
        e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.palm(2, timeMs = 0L)), added = 2))
        e.process(
            TestTouchFactory.frame(
                InputAction.POINTER_DOWN, 10L,
                listOf(TestTouchFactory.palm(2, timeMs = 0L), fingertip(1, 100f, 100f, 10L), fingertip(3, 300f, 100f, 10L)),
                added = 3,
            )
        )
        e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 400L,
                listOf(TestTouchFactory.palm(2, timeMs = 0L), fingertip(1, 100f, 100f, 10L), fingertip(3, 300f, 100f, 10L)),
            )
        )
        // Both resting fingers sweep fast together (8mm in 10ms): a hand shift, not
        // two strokes. Neither may become WRITING and no lock may be claimed.
        val swept = e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 410L,
                listOf(TestTouchFactory.palm(2, timeMs = 0L), fingertip(1, 180f, 100f, 10L), fingertip(3, 380f, 100f, 10L)),
            )
        )
        assertTrue(swept.contactFor(1)?.classification != ContactClassification.WRITING)
        assertTrue(swept.contactFor(3)?.classification != ContactClassification.WRITING)
        assertNull(swept.activeWritingPointerId)
    }

    // --- Bug race: hardware eraser joining mid-stroke stays an eraser -------------------

    @Test
    fun hardwareEraserWhilePenLockedStaysEraser() {
        val e = engine()
        // Pen starts: cold start -> CANDIDATE, promote via MOVE to claim the lock.
        e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.pen(0, timeMs = 0L)), added = 0))
        e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 5L,
                listOf(TestTouchFactory.pen(0, x = 180f, y = 100f, timeMs = 5L)),
            )
        )
        val eraser = TestTouchFactory.pen(5, x = 200f, y = 200f, timeMs = 20L, toolType = TestTouchFactory.TOOL_ERASER)
        val out = e.process(
            TestTouchFactory.frame(
                InputAction.POINTER_DOWN, 20L,
                listOf(TestTouchFactory.pen(0, x = 120f, y = 110f, timeMs = 5L), eraser),
                added = 5,
            )
        )
        assertEquals(ContactClassification.ERASER, out.contactFor(5)?.classification)
        // The pen keeps the writing lock; the eraser never becomes a gesture finger.
        assertEquals(0, out.activeWritingPointerId)
        assertTrue(out.gesturePointerIds.isEmpty())
    }

    // --- device-verified scenarios (emulator injection found these) ------------

    /** A brief large palm TAP (DOWN + 3 MOVEs + UP in ~250ms) must never write. */
    @Test
    fun largePalmTapNeverWrites() {
        val e = engine()
        val t0 = 0L
        val down = e.process(
            TestTouchFactory.frame(
                InputAction.DOWN, t0,
                listOf(TestTouchFactory.palm(2, x = 800f, y = 600f, timeMs = t0)), added = 2,
            )
        )
        assertNull("large tap DOWN must not claim the writing lock", down.activeWritingPointerId)
        assertEquals(ContactClassification.PALM, down.contactFor(2)?.classification)

        // brief wiggle while the palm tap settles
        for (i in 1..3) {
            val t = t0 + i * 80L
            val out = e.process(
                TestTouchFactory.frame(
                    InputAction.MOVE, t,
                    listOf(TestTouchFactory.palm(2, x = 800f + i * 3f, y = 600f + i * 2f, timeMs = t)),
                )
            )
            assertEquals("large tap MOVE must never classify WRITING (frame $i)", ContactClassification.PALM, out.contactFor(2)?.classification)
        }
        val up = e.process(
            TestTouchFactory.frame(InputAction.UP, 250L, listOf(TestTouchFactory.palm(2, timeMs = 250L)), lifted = 2)
        )
        assertNull("large tap UP must not leave a writing lock", up.activeWritingPointerId)
    }

    /** A large contact moving quickly (a dragged palm) must never write ink. */
    @Test
    fun largeMovingContactNeverWrites() {
        val e = engine()
        val down = e.process(
            TestTouchFactory.frame(
                InputAction.DOWN, 0L,
                listOf(TestTouchFactory.palm(2, x = 600f, y = 900f, timeMs = 0L)), added = 2,
            )
        )
        assertNull(down.activeWritingPointerId)
        var last = down
        for (i in 1..10) {
            val t = i * 60L
            last = e.process(
                TestTouchFactory.frame(
                    InputAction.MOVE, t,
                    listOf(TestTouchFactory.palm(2, x = 600f + i * 30f, y = 900f + i * 10f, timeMs = t)),
                )
            )
            assertEquals("dragged palm must never classify WRITING (frame $i)", ContactClassification.PALM, last.contactFor(2)?.classification)
            assertNull("dragged palm must never take the lock", last.activeWritingPointerId)
        }
    }

    // --- Cold-start palm-first-down: CANDIDATE, not WRITING (no ink on palm-first) ---

    @Test
    fun coldStartPalmFirstDownIsCandidateNotWriting() {
        val e = engine(PalmRejectionMode.WRITING)
        // A single small-ellipse contact with empty history must never be WRITING
        // on the DOWN frame — it is held as CANDIDATE until motion confirms it.
        val down = e.process(
            TestTouchFactory.frame(
                InputAction.DOWN, 0L,
                listOf(TestTouchFactory.pen(0, x = 200f, y = 200f, timeMs = 0L)), added = 0,
            )
        )
        assertNull("cold-start palm-first must not claim the lock", down.activeWritingPointerId)
        assertEquals(ContactClassification.CANDIDATE, down.contactFor(0)?.classification)
    }

    @Test
    fun coldStartCandidatePromotesToWritingAfterSufficientMotion() {
        val e = engine(PalmRejectionMode.WRITING)
        e.process(
            TestTouchFactory.frame(
                InputAction.DOWN, 0L,
                listOf(TestTouchFactory.pen(0, x = 200f, y = 200f, timeMs = 0L)), added = 0,
            )
        )
        // Move enough (>=40px=4mm) to pass the stroke gate and promote to WRITING.
        val move = e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 10L,
                listOf(TestTouchFactory.pen(0, x = 280f, y = 220f, timeMs = 10L)),
            )
        )
        assertEquals(0, move.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, move.contactFor(0)?.classification)
        assertTrue(move.gesturePointerIds.isEmpty())
    }

    // --- Mid-stroke palm join: ink must stay correct --------------------------------

    @Test
    fun midStrokePalmJoinKeepsWritingCorrect() {
        val e = engine(PalmRejectionMode.WRITING)
        // Pen starts and is promoted to writing.
        e.process(
            TestTouchFactory.frame(
                InputAction.DOWN, 0L,
                listOf(TestTouchFactory.pen(0, x = 200f, y = 200f, timeMs = 0L)), added = 0,
            )
        )
        e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 5L,
                listOf(TestTouchFactory.pen(0, x = 280f, y = 220f, timeMs = 5L)),
            )
        )
        // Palm lands mid-stroke: pen stays the writer, palm is rejected.
        val palmJoins = e.process(
            TestTouchFactory.frame(
                InputAction.POINTER_DOWN, 20L,
                listOf(
                    TestTouchFactory.pen(0, x = 280f, y = 220f, timeMs = 5L),
                    TestTouchFactory.palm(2, x = 500f, y = 700f, timeMs = 20L),
                ),
                added = 2,
            )
        )
        assertEquals("pen must keep the lock", 0, palmJoins.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, palmJoins.contactFor(0)?.classification)
        assertEquals(ContactClassification.PALM, palmJoins.contactFor(2)?.classification)
        assertTrue(palmJoins.gesturePointerIds.isEmpty())

        // Continuing stroke with palm still resting.
        val cont = e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 30L,
                listOf(
                    TestTouchFactory.pen(0, x = 320f, y = 260f, timeMs = 30L),
                    TestTouchFactory.palm(2, x = 500f, y = 700f, timeMs = 20L),
                ),
            )
        )
        assertEquals(0, cont.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, cont.contactFor(0)?.classification)
    }

    // --- No-palm single stroke: writing without palm still works ---------------------

    @Test
    fun noPalmSingleStrokeWritesCorrectly() {
        val e = engine(PalmRejectionMode.WRITING)
        // Cold start -> CANDIDATE, promote via MOVE.
        e.process(
            TestTouchFactory.frame(
                InputAction.DOWN, 0L,
                listOf(TestTouchFactory.pen(0, x = 200f, y = 200f, timeMs = 0L)), added = 0,
            )
        )
        e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 5L,
                listOf(TestTouchFactory.pen(0, x = 280f, y = 220f, timeMs = 5L)),
            )
        )
        // Pen is writing, no palm present.
        val move = e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 15L,
                listOf(TestTouchFactory.pen(0, x = 360f, y = 240f, timeMs = 15L)),
            )
        )
        assertEquals(0, move.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, move.contactFor(0)?.classification)
        // Clean up.
        e.process(
            TestTouchFactory.frame(
                InputAction.UP, 25L,
                listOf(TestTouchFactory.pen(0, x = 400f, y = 260f, timeMs = 25L)),
                lifted = 0,
            )
        )
    }

    // --- Continued writing after palm lift: ink stays correct ------------------------

    @Test
    fun continuedWritingAfterPalmLift() {
        val e = engine(PalmRejectionMode.WRITING)
        // Pen starts writing.
        e.process(
            TestTouchFactory.frame(
                InputAction.DOWN, 0L,
                listOf(TestTouchFactory.pen(0, x = 200f, y = 200f, timeMs = 0L)), added = 0,
            )
        )
        e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 5L,
                listOf(TestTouchFactory.pen(0, x = 280f, y = 220f, timeMs = 5L)),
            )
        )
        // Palm joins.
        e.process(
            TestTouchFactory.frame(
                InputAction.POINTER_DOWN, 20L,
                listOf(
                    TestTouchFactory.pen(0, x = 280f, y = 220f, timeMs = 5L),
                    TestTouchFactory.palm(2, x = 500f, y = 700f, timeMs = 20L),
                ),
                added = 2,
            )
        )
        // Palm lifts while pen still writing.
        val palmLift = e.process(
            TestTouchFactory.frame(
                InputAction.POINTER_UP, 30L,
                listOf(TestTouchFactory.pen(0, x = 320f, y = 250f, timeMs = 30L)),
                lifted = 2,
            )
        )
        assertEquals("pen must keep the lock after palm lifts", 0, palmLift.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, palmLift.contactFor(0)?.classification)

        // Writing continues after palm is gone.
        val cont = e.process(
            TestTouchFactory.frame(
                InputAction.MOVE, 40L,
                listOf(TestTouchFactory.pen(0, x = 400f, y = 280f, timeMs = 40L)),
            )
        )
        assertEquals(0, cont.activeWritingPointerId)
        assertEquals(ContactClassification.WRITING, cont.contactFor(0)?.classification)
        assertTrue(cont.gesturePointerIds.isEmpty())
    }

    // --- Palm rejection disabled: no behavior change (regression guard) -------------

    @Test
    fun palmRejectionDisabledBehaviorUnchanged() {
        val e = PalmRejectionEngine(testCapabilities()) {
            testSettings().apply { palmRejectionEnabled = false }
        }
        // With palm rejection off, every contact writes — cold start has no effect.
        val down = e.process(
            TestTouchFactory.frame(
                InputAction.DOWN, 0L,
                listOf(TestTouchFactory.pen(0, x = 200f, y = 200f, timeMs = 0L)), added = 0,
            )
        )
        assertEquals(ContactClassification.WRITING, down.contactFor(0)?.classification)
        assertEquals(0, down.activeWritingPointerId)
    }

    // --- (i) palm resting, then finger writes: no dead window -------------------

    @Test
    fun palmRestingThenFingerWritesImmediately() {
        val e = engine()
        val palmFirst = e.process(
            TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.palm(2, timeMs = 0L)), added = 2)
        )
        assertNull(palmFirst.activeWritingPointerId)

        // Fingertip joins while the palm rests: it must claim the lock on the POINTER_DOWN
        // frame — the relative classifier marks it WRITING (the smallest contact beside a
        // palm-sized contact).
        val out = e.process(
            TestTouchFactory.frame(
                InputAction.POINTER_DOWN, 20L,
                listOf(
                    TestTouchFactory.palm(2, x = 500f, y = 700f, timeMs = 20L),
                    TestTouchFactory.fingertip(1, x = 200f, y = 200f, timeMs = 20L),
                ),
                added = 1,
            )
        )
        assertEquals(1, out.activeWritingPointerId)
        assertTrue(out.gesturePointerIds.isEmpty())
        // Palm stays rejected while the finger writes.
        assertEquals(ContactClassification.PALM, out.contactFor(2)?.classification)
    }

    @Test
    fun secondFingerWithoutRestingPalmDoesNotStealLock() {
        val e = engine()
        e.process(
            TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(TestTouchFactory.fingertip(1, timeMs = 0L)), added = 1)
        )
        // No palm down — the second finger must NOT claim the lock, so the pair can
        // still drive pan/zoom gestures.
        val out = e.process(
            TestTouchFactory.frame(
                InputAction.POINTER_DOWN, 20L,
                listOf(
                    TestTouchFactory.fingertip(1, x = 200f, y = 200f, timeMs = 20L),
                    TestTouchFactory.fingertip(3, x = 400f, y = 400f, timeMs = 20L),
                ),
                added = 3,
            )
        )
        assertNull(out.activeWritingPointerId)
        // After cold start, the first finger is CANDIDATE (not yet promoted); only
        // the second finger is FINGER and may gesture. The lock remains unclaimed.
        assertTrue(out.gesturePointerIds.contains(3))
    }
}
