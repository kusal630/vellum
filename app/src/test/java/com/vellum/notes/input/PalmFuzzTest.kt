package com.vellum.notes.input

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PalmFuzzTest {

    private var settings = testSettings()

    private fun engine() = PalmRejectionEngine(testCapabilities()) { settings }

    private data class Profile(
        val tool: Int,
        val baseMajorPx: Float,
        var x: Float,
        var y: Float,
    )

    private fun newProfile(rnd: Random): Profile {
        val kind = rnd.nextInt(10)
        return when {
            kind < 2 -> Profile(TestTouchFactory.TOOL_STYLUS, rnd.nextFloat() * 20f + 8f, rnd.nextFloat() * 1000f, rnd.nextFloat() * 1000f)
            kind < 6 -> Profile(TestTouchFactory.TOOL_FINGER, rnd.nextFloat() * 100f + 40f, rnd.nextFloat() * 1000f, rnd.nextFloat() * 1000f)
            kind < 8 -> Profile(TestTouchFactory.TOOL_FINGER, rnd.nextFloat() * 200f + 250f, rnd.nextFloat() * 1000f, rnd.nextFloat() * 1000f)
            kind < 9 -> Profile(TestTouchFactory.TOOL_UNKNOWN, rnd.nextFloat() * 100f + 40f, rnd.nextFloat() * 1000f, rnd.nextFloat() * 1000f)
            else -> Profile(TestTouchFactory.TOOL_UNKNOWN, rnd.nextFloat() * 200f + 250f, rnd.nextFloat() * 1000f, rnd.nextFloat() * 1000f)
        }
    }

    private fun sample(rnd: Random, id: Int, p: Profile, t: Long): RawTouchContact {
        p.x = (p.x + (rnd.nextFloat() - 0.5f) * 40f).coerceIn(0f, 1000f)
        p.y = (p.y + (rnd.nextFloat() - 0.5f) * 40f).coerceIn(0f, 1000f)
        val major = p.baseMajorPx * (0.9f + rnd.nextFloat() * 0.2f)
        return TestTouchFactory.contact(
            pointerId = id, x = p.x, y = p.y, timeMs = t, downTimeMs = t,
            majorPx = major, minorPx = major * 0.8f,
            pressure = 0.3f + rnd.nextFloat() * 0.6f, toolType = p.tool,
        )
    }

    @Test
    fun physicalStreams_neverCrash_andStablePalmsNeverWrite() {
        repeat(200) { seed ->
            val rnd = Random(seed.toLong())
            val e = engine()
            var t = 0L
            val profiles = mutableMapOf<Int, Profile>()
            var nextId = 0
            var hugeWrote = 0
            var hugeTotal = 0
            // Safety property: a stably huge FINGER/UNKNOWN contact must never
            // write. (Hardware STYLUS always writes by design, whatever its size.)
            fun check(out: ClassifiedFrame, contacts: List<RawTouchContact>) {
                for (c in contacts) {
                    val base = profiles[c.pointerId]?.baseMajorPx ?: 0f
                    if (base > 250f &&
                        (c.toolTypeRaw == TestTouchFactory.TOOL_FINGER ||
                            c.toolTypeRaw == TestTouchFactory.TOOL_UNKNOWN)
                    ) {
                        hugeTotal++
                        if (out.contactFor(c.pointerId)?.classification == ContactClassification.WRITING) {
                            hugeWrote++
                        }
                    }
                }
            }
            repeat(30) {
                t += rnd.nextLong(5L, 30L)
                when {
                    profiles.isEmpty() || (profiles.size < 3 && rnd.nextBoolean()) -> {
                        val id = nextId++
                        profiles[id] = newProfile(rnd)
                        val all = profiles.map { (pid, p) -> sample(rnd, pid, p, t) }
                        val action = if (profiles.size == 1) InputAction.DOWN else InputAction.POINTER_DOWN
                        check(e.process(TestTouchFactory.frame(action, t, all, added = id)), all)
                    }
                    rnd.nextInt(10) < 2 -> {
                        val id = profiles.keys.elementAt(rnd.nextInt(profiles.size))
                        profiles.remove(id)
                        val rest = profiles.map { (pid, p) -> sample(rnd, pid, p, t) }
                        val action = if (rest.isEmpty()) InputAction.UP else InputAction.POINTER_UP
                        e.process(TestTouchFactory.frame(action, t, rest, lifted = id))
                    }
                    else -> {
                        val contacts = profiles.map { (pid, p) -> sample(rnd, pid, p, t) }
                        check(e.process(TestTouchFactory.frame(InputAction.MOVE, t, contacts)), contacts)
                    }
                }
            }
            assertTrue("seed $seed: stable palm wrote ($hugeWrote/$hugeTotal)", hugeWrote == 0)
            e.process(TestTouchFactory.frame(InputAction.CANCEL, t + 1, emptyList()))
        }
    }

    @Test
    fun loneStylusDown_alwaysWrites_acrossFuzz() {
        repeat(50) { seed ->
            val rnd = Random(10_000L + seed)
            val e = engine()
            val stylus = TestTouchFactory.contact(
                pointerId = 0, x = rnd.nextFloat() * 1000f, y = rnd.nextFloat() * 1000f, timeMs = 0L,
                majorPx = 20f, minorPx = 18f, pressure = 0.6f,
                toolType = TestTouchFactory.TOOL_STYLUS,
            )
            val down = e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(stylus), added = 0))
            assertTrue(
                "seed $seed: lone stylus did not write (${down.contactFor(0)?.classification})",
                down.contactFor(0)?.classification == ContactClassification.WRITING,
            )
        }
    }

    @Test
    fun writerThatGrowsPalmSized_isCancelled() {
        val e = engine()
        val pen = TestTouchFactory.pen(pointerId = 0, x = 200f, y = 200f, timeMs = 0L)
        e.process(TestTouchFactory.frame(InputAction.DOWN, 0L, listOf(pen), added = 0))
        e.process(TestTouchFactory.frame(InputAction.MOVE, 10L, listOf(pen.copy(x = 240f, eventTimeNanos = 10L * 1_000_000L))))
        var last: ClassifiedFrame? = null
        var major = 26f
        var t = 20L
        // Hand settles onto the screen: contact grows 26px -> 300px while drifting.
        while (t <= 200L) {
            major += 30f
            val grown = TestTouchFactory.contact(
                pointerId = 0, x = 240f + t, y = 220f, timeMs = t,
                majorPx = major, minorPx = major * 0.8f, pressure = 0.9f,
            )
            last = e.process(TestTouchFactory.frame(InputAction.MOVE, t, listOf(grown)))
            t += 20L
        }
        // Settled palm held still: the smoothed size converges past the cancel
        // threshold and the lock must release (PALM_GROWTH_CANCELLED).
        while (t <= 400L) {
            val held = TestTouchFactory.contact(
                pointerId = 0, x = 440f, y = 220f, timeMs = t,
                majorPx = 330f, minorPx = 264f, pressure = 0.9f,
            )
            last = e.process(TestTouchFactory.frame(InputAction.MOVE, t, listOf(held)))
            t += 20L
        }
        assertTrue(
            "grown palm lock must cancel, got ${last?.contactFor(0)?.classification}",
            last?.contactFor(0)?.classification != ContactClassification.WRITING,
        )
    }
}
