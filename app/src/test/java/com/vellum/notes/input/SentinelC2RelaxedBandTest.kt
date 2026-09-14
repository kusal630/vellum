package com.vellum.notes.input

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * SENT-C2: RELAXED bandMax wrong.
 *
 * The pressure/fallback gates in classifyValid hardcoded fingerMax instead of
 * resolving the palm band by mode (RELAXED → relaxedPalmMm). An 18mm pressurized
 * contact slipped through and was misclassified instead of PALM.
 */
class SentinelC2RelaxedBandTest {

    private val caps = testCapabilities()
    private val normalizer = InputNormalizer(caps)

    private fun settings() = PalmRejectionSettings(
        mode = PalmRejectionMode.RELAXED,
        sensitivity = 0.5f,
        writingHoldoffMs = 0L,
        fingerMaxMm = 10f,
        relaxedPalmMm = 15f,
    )

    /** Seeds the adaptive valid-size history so the single-pointer path reaches classifyValid. */
    private fun seedValidHistory(classifier: PalmClassifier, sizeMm: Float) {
        val raw = TestTouchFactory.contact(
            pointerId = 0, x = 100f, y = 100f, timeMs = 0L,
            majorPx = sizeMm * caps.pxPerMm, minorPx = sizeMm * caps.pxPerMm * 0.9f,
        )
        val normalized = normalizer.normalize(raw)
        classifier.updateHistory(
            ClassifiedContact(
                contact = normalized,
                classification = ContactClassification.WRITING,
                confidence = 0.9f,
                reason = ClassificationReason.SMALL_CONTACT,
                effectiveThresholdMm = sizeMm,
                speedMmPerSec = 0f,
                durationMs = 0L,
            ),
        )
    }

    @Test
    fun relaxedAdaptivePath_18mmPressurizedContact_isPalm() {
        val s = settings()
        val classifier = PalmClassifier(s)
        // Seed a large-ish valid average (8mm) so 18mm stays within the generous
        // valid multiple (2.5 * 8 = 20) and reaches classifyValid.
        seedValidHistory(classifier, 8f)

        // 18mm contact with saturated pressure.
        val raw = TestTouchFactory.contact(
            pointerId = 1, x = 500f, y = 500f, timeMs = 0L,
            majorPx = 180f, minorPx = 170f, pressure = 1.0f,
        )
        val normalized = normalizer.normalize(raw)
        val r = classifier.classify(
            normalized,
            PalmClassifier.ClassifyContext(mode = PalmRejectionMode.RELAXED),
        )
        assertEquals(ContactClassification.PALM, r.classification)
    }

    @Test
    fun relaxedColdStart_18mmPressurizedContact_isPalm() {
        // Cold-start settings backstop (no history): 18mm > relaxedPalmMm(15) → PALM.
        val s = settings()
        val raw = TestTouchFactory.contact(
            pointerId = 1, x = 500f, y = 500f, timeMs = 0L,
            majorPx = 180f, minorPx = 170f, pressure = 1.0f,
        )
        val r = PalmClassifier(s).classify(
            normalizer.normalize(raw),
            PalmClassifier.ClassifyContext(mode = PalmRejectionMode.RELAXED),
        )
        assertEquals(ContactClassification.PALM, r.classification)
    }

    @Test
    fun relaxedBandStillAllowsFingerBelowRelaxedPalm() {
        // A 13mm contact (fingerMax=10 < 13 <= relaxedPalm=15) stays FINGER in RELAXED.
        val s = settings()
        val classifier = PalmClassifier(s)
        seedValidHistory(classifier, 8f)
        val raw = TestTouchFactory.contact(
            pointerId = 1, x = 200f, y = 200f, timeMs = 0L,
            majorPx = 130f, minorPx = 120f, pressure = 0.5f,
        )
        val r = classifier.classify(
            normalizer.normalize(raw),
            PalmClassifier.ClassifyContext(mode = PalmRejectionMode.RELAXED),
        )
        assertEquals(ContactClassification.FINGER, r.classification)
    }
}
