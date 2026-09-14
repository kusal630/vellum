package com.vellum.notes.ui.editor

import com.vellum.notes.input.PalmRejectionEngine
import com.vellum.notes.input.RestingHandTracker
import com.vellum.notes.input.testCapabilities
import com.vellum.notes.input.testSettings
import com.vellum.notes.ui.diagnostics.DiagnosticsTouchView
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * PH-03 regression: the resting-hand tracker's viewport/edge context must be initialized
 * from the layout paths (onSizeChanged/onLayout), not only when the first touch lands.
 * Without it the viewport reads 0 and edge/cluster rules silently fall back to
 * `displayMaxPx`.
 */
@RunWith(RobolectricTestRunner::class)
class ViewportSyncTest {

    /** White-box read of the engine's resting-hand viewport (no public getter). */
    private fun trackerViewport(engine: PalmRejectionEngine): Pair<Float, Float> {
        val field = PalmRejectionEngine::class.java.getDeclaredField("restingTracker")
        field.isAccessible = true
        val tracker = field.get(engine) as RestingHandTracker
        return tracker.viewportWidthPx to tracker.viewportHeightPx
    }

    @Test
    fun inkCanvas_layoutPublishesViewportToTracker() {
        val context = RuntimeEnvironment.getApplication().applicationContext
        val view = InkCanvasView(context)
        val caps = testCapabilities(pxPerMm = 10f)
        val engine = PalmRejectionEngine(caps) { testSettings() }
        view.capabilities = caps
        view.engine = engine

        view.layout(0, 0, 1080, 1920)

        val (w, h) = trackerViewport(engine)
        assertEquals(1080f, w, 0.01f)
        assertEquals(1920f, h, 0.01f)
    }

    @Test
    fun diagnosticsTouchView_layoutPublishesViewportToTracker() {
        val context = RuntimeEnvironment.getApplication().applicationContext
        val caps = testCapabilities(pxPerMm = 10f)
        val engine = PalmRejectionEngine(caps) { testSettings() }
        val view = DiagnosticsTouchView(context, engine, pxPerMm = 10f)

        view.layout(0, 0, 1080, 1920)

        val (w, h) = trackerViewport(engine)
        assertEquals(1080f, w, 0.01f)
        assertEquals(1920f, h, 0.01f)
    }
}
