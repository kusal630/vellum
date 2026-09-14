package com.vellum.notes.packs

/** Built-in gesture actions (custom mapping in the Gesture/Bookmark pack). */
object GesturePro {
    const val GESTURE_TWO_FINGER_DOUBLE_TAP = "two_finger_double_tap"
    const val GESTURE_THREE_FINGER_TAP = "three_finger_tap"
    const val GESTURE_TWO_FINGER_SWIPE_LEFT = "two_finger_swipe_left"
    const val GESTURE_TWO_FINGER_SWIPE_RIGHT = "two_finger_swipe_right"

    const val ACTION_UNDO = "undo"
    const val ACTION_REDO = "redo"
    const val ACTION_TOGGLE_RAIL = "toggle_rail"
    const val ACTION_NEW_PAGE = "new_page"
    const val ACTION_EXPORT_PDF = "export_pdf"
    const val ACTION_TOGGLE_TRANSCRIPT = "toggle_transcript"

    val knownGestures: List<String> = listOf(
        GESTURE_TWO_FINGER_DOUBLE_TAP,
        GESTURE_THREE_FINGER_TAP,
        GESTURE_TWO_FINGER_SWIPE_LEFT,
        GESTURE_TWO_FINGER_SWIPE_RIGHT,
    )

    val knownActions: List<String> = listOf(
        ACTION_UNDO,
        ACTION_REDO,
        ACTION_TOGGLE_RAIL,
        ACTION_NEW_PAGE,
        ACTION_EXPORT_PDF,
        ACTION_TOGGLE_TRANSCRIPT,
    )

    fun defaultMapping(): List<GestureMapping> = listOf(
        GestureMapping(GESTURE_TWO_FINGER_DOUBLE_TAP, ACTION_UNDO),
        GestureMapping(GESTURE_THREE_FINGER_TAP, ACTION_REDO),
        GestureMapping(GESTURE_TWO_FINGER_SWIPE_LEFT, ACTION_TOGGLE_RAIL),
        GestureMapping(GESTURE_TWO_FINGER_SWIPE_RIGHT, ACTION_NEW_PAGE),
    )

    /** Resolves a gesture to its action using a custom mapping (falls back to default). */
    fun resolveAction(gesture: String, mapping: List<GestureMapping>): String? =
        mapping.firstOrNull { it.gesture == gesture }?.action
            ?: defaultMapping().firstOrNull { it.gesture == gesture }?.action

    fun isKnownGesture(gesture: String): Boolean = gesture in knownGestures
    fun isKnownAction(action: String): Boolean = action in knownActions
}
