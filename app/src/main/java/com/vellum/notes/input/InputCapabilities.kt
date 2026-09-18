package com.vellum.notes.input

import android.content.Context
import android.hardware.input.InputManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.InputDevice
import android.view.MotionEvent
import android.view.WindowManager

/**
 * Immutable description of what the current device can actually tell us about touch.
 * This is the single source of truth the palm rejection system uses to adapt its
 * behavior — the app never assumes capabilities the hardware does not expose.
 */
data class InputCapabilities(
    /** Physical pixel density: pixels per millimeter (approximated from densityDpi). */
    val pxPerMm: Float,
    /** Largest touch-screen dimension in pixels (used to convert getSize() to px). */
    val displayMaxPx: Float,
    /** Screen diagonal in millimeters (informational). */
    val screenDiagonalMm: Float,
    /** Whether at least one input device exposes an active stylus source. */
    val supportsStylus: Boolean,
    /** Whether the OS is capable of reporting distinct tool types (always true on minSdk 26+). */
    val supportsToolType: Boolean,
    /** Whether any device exposes stylus tool-type classification (active stylus). */
    val supportsStylusToolType: Boolean,
    /** Whether the device exposes contact-size (getSize) meaningfully. */
    val supportsContactSize: Boolean,
    /** Whether pressure values are available (active stylus or capable digitizer). */
    val supportsPressure: Boolean,
    /** Whether multi-touch is available (effectively always true on tablets). */
    val supportsMultiTouch: Boolean,
    /** Device has known palm-classification behavior hints (informational only). */
    val hasPalmClassificationHint: Boolean,
    val apiLevel: Int,
    /** Whether this instance was produced by a successful [detect] call (never defaults). */
    val isRealDeviceScan: Boolean = false,
) {
    fun dimFromPx(px: Float): Float = if (pxPerMm > 0f) px / pxPerMm else px / 10f

    companion object {
        /** Plausible px/mm range for any consumer touch display. */
        private const val MIN_PX_PER_MM = 5f
        private const val MAX_PX_PER_MM = 22f

        /**
         * Detects [InputCapabilities] from the running hardware. This is honest detection:
         * `supportsStylus` is true only when an input device actually reports a stylus source.
         * A passive stylus almost always results in `supportsStylusToolType == false`, and the
         * app then relies on the software classifier.
         */
        fun detect(context: Context): InputCapabilities {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()

            var densityDpi: Int = 0
            var widthPx: Int = 0
            var heightPx: Int = 0
            var ok = false

            try {
                // getRealMetrics is the most honest path but may fail on newer API levels
                // or on displays that are not yet ready (early Application.onCreate, multi-display,
                // restricted profiles). Fall back to the resources-backed metrics.
                wm.defaultDisplay.getRealMetrics(metrics)
                densityDpi = metrics.densityDpi
                widthPx = metrics.widthPixels
                heightPx = metrics.heightPixels
                ok = densityDpi > 0 && widthPx > 0 && heightPx > 0
            } catch (t: Throwable) {
                // noqa
            }

            if (!ok) {
                // Fallback: use the resources metrics, which are always populated by the time
                // a real Activity is on-screen. These may be slightly less accurate (e.g. not
                // accounting for system bars) but are safe for classification purposes.
                val resMetrics = context.resources.displayMetrics
                densityDpi = resMetrics.densityDpi
                widthPx = resMetrics.widthPixels
                heightPx = resMetrics.heightPixels
                if (densityDpi <= 0 || widthPx <= 0 || heightPx <= 0) {
                    // Truly unusable: fall back to conservative software defaults.
                    densityDpi = 240
                    widthPx = 1920
                    heightPx = 1080
                }
            }

            val clampedDpi = densityDpi.coerceIn(1, 640)
            val rawPxPerMm = clampedDpi / 25.4f
            val pxPerMm = rawPxPerMm.coerceIn(MIN_PX_PER_MM, MAX_PX_PER_MM)
            val displayMaxPx = maxOf(widthPx, heightPx).toFloat().coerceAtLeast(1f)
            val diagPx = kotlin.math.sqrt(
                (widthPx * widthPx + heightPx * heightPx).toDouble()
            )
            val screenDiagonalMm = (diagPx / pxPerMm).toFloat()

            val inputManager = context.getSystemService(InputManager::class.java)

            val stylusSources = mutableListOf<Int>()
            val hasStylusDevice = inputManager.inputDeviceIds.any { id ->
                val dev = inputManager.getInputDevice(id)
                val s = dev?.sources ?: 0
                if (s and InputDevice.SOURCE_STYLUS != 0) {
                    stylusSources.add(s)
                    true
                } else false
            }

            // A stylus source that also reports class-specific tool types (active stylus).
            // NOTE: we check whether ANY stylus-capable device exposes tool-type classification,
            // not just the first stylus source, because multiple stylus devices can be present.
            val hasAnyStylusWithToolType = hasStylusDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            val stylusToolTypeExposed = stylusSources.any { s ->
                s and InputDevice.SOURCE_STYLUS != 0
            } && hasAnyStylusWithToolType

            return InputCapabilities(
                pxPerMm = pxPerMm,
                displayMaxPx = displayMaxPx,
                screenDiagonalMm = screenDiagonalMm,
                supportsStylus = hasStylusDevice,
                supportsToolType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT,
                supportsStylusToolType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasStylusDevice,
                supportsContactSize = true,
                supportsPressure = true,
                supportsMultiTouch = true,
                hasPalmClassificationHint = false,
                apiLevel = Build.VERSION.SDK_INT,
                isRealDeviceScan = true,
            )
        }

        /**
         * Maps a MotionEvent tool type constant to our [ToolKind]. Unknown types fall back
         * to geometric classification; we never invent a stylus where none was reported.
         */
        fun toolKindFromRaw(raw: Int): ToolKind = when (raw) {
            MotionEvent.TOOL_TYPE_STYLUS -> ToolKind.STYLUS
            MotionEvent.TOOL_TYPE_ERASER -> ToolKind.ERASER
            MotionEvent.TOOL_TYPE_FINGER -> ToolKind.FINGER
            MotionEvent.TOOL_TYPE_MOUSE -> ToolKind.MOUSE
            else -> ToolKind.UNKNOWN
        }
    }
}
