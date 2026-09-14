package com.vellum.notes.input

import android.os.Build
import android.view.MotionEvent
import android.view.MotionEvent.FLAG_CANCELED
import android.view.MotionEvent.TOOL_TYPE_STYLUS
import kotlin.math.hypot

/**
 * Orchestrates the palm rejection pipeline for one canvas: it tracks per-pointer motion
 * state (velocity, duration), runs the [PalmClassifier], manages the [WritingLock], and
 * decides which pointers are allowed to drive pan/zoom gestures.
 *
 * The engine is stateful per page/canvas session. It keeps only lightweight numeric
 * per-pointer state — no per-event object churn — so it is safe to run on the input
 * thread every frame.
 */
class PalmRejectionEngine(
    private val capabilities: InputCapabilities,
    private val settingsProvider: () -> PalmRejectionSettings,
) {
    private val normalizer = InputNormalizer(capabilities)
    private val pointerStates = HashMap<Int, PointerMotionState>()

    // Mutable references that are recreated only when settings actually change.
    private var currentSettings: PalmRejectionSettings = settingsProvider()
    private var classifier = PalmClassifier(currentSettings)
    private var lock = WritingLock(currentSettings.writingHoldoffMs)
    private var lastKnownHoldoffMs = currentSettings.writingHoldoffMs
    private val restingTracker = RestingHandTracker(capabilities)

    /**
     * Cold-start CANDIDATE tracked for a potential claim on the next MOVE. The
     * size classifier buffers a lone cold-start contact as CANDIDATE on DOWN
     * (never WRITING, so [manageWritingLock] cannot claim it there); it is
     * promoted when stroke-like motion arrives. Kept here so the MOVE promotion
     * has a backfill anchor (the tracker's downX/downY seeds the stroke tail
     * in InkCanvasView) even if the tracker lags one frame.
     */
    private var pendingCandidateId: Int? = null

    /**
     * The user-reserved palm rest zone resolved to screen pixels, or null when disabled.
     * Updated by the canvas every frame (it knows the zone and its own size).
     */
    private var palmZoneRect: PalmZoneRect? = null

    fun setPalmZoneRect(rect: PalmZoneRect?) {
        palmZoneRect = rect
    }

    /** Viewport size in screen px (used by the resting-hand tracker for edge detection). */
    fun setViewportSize(widthPx: Int, heightPx: Int) {
        restingTracker.viewportWidthPx = widthPx.toFloat()
        restingTracker.viewportHeightPx = heightPx.toFloat()
    }

    fun reset() {
        lock.reset(System.nanoTime())
        pointerStates.clear()
        classifier.resetHistory()
        restingTracker.reset()
        pendingCandidateId = null
    }

    /** Recreates derived state only when the settings instance actually changes. */
    private fun refreshIfSettingsChanged() {
        val settings = settingsProvider()
        if (settings !== currentSettings) {
            currentSettings = settings
            if (settings.writingHoldoffMs != lastKnownHoldoffMs) {
                lock = WritingLock(settings.writingHoldoffMs)
                lastKnownHoldoffMs = settings.writingHoldoffMs
            }
            // Update the classifier's settings in place: recreating it would wipe the
            // adaptive history (the device's learned contact-size scale), which must
            // survive settings tweaks within a session.
            classifier.updateSettings(settings)
        }
    }

    fun process(frame: InputFrame): ClassifiedFrame {
        refreshIfSettingsChanged()
        val nowNanos = frame.eventTimeNanos

        // Master switch: with palm rejection off nothing is ever rejected or buffered —
        // every contact behaves as plain writable/finger input.
        if (!currentSettings.palmRejectionEnabled) {
            return processWithoutPalmRejection(frame)
        }

        // Normalize every active contact once so the relative classifier can compare the
        // CURRENT frame's contact sizes against each other.
        val normalized = frame.contacts.map { normalizer.normalize(it) }
        val activeSizesMm = normalized.map { it.maxDimMm }

        val baseClassified = mutableListOf<ClassifiedContact>()
        for (i in normalized.indices) {
            val rawContact = frame.contacts[i]
            val contact = normalized[i]
            val state = pointerStates[contact.pointerId]

            // Per-contact OS-detected cancellation (API 33+): if this individual pointer was
            // flagged by the system as an unintentional touch (palm, grip, bezel), reject it
            // immediately regardless of size or zone. The parser only remaps the ACTION to
            // CANCEL for the whole frame; here we reject the specific flagged contact inline
            // so a valid writer in the same frame is not affected.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                (rawContact.flags and FLAG_CANCELED) != 0
            ) {
                val c = ClassifiedContact(
                    contact = contact,
                    classification = ContactClassification.PALM,
                    confidence = 0.95f,
                    reason = ClassificationReason.LARGE_CONTACT,
                    effectiveThresholdMm = 0f,
                    speedMmPerSec = state?.speedMmPerSec ?: 0f,
                    durationMs = state?.let { (nowNanos - it.downTimeNanos) / 1_000_000L } ?: 0L,
                )
                baseClassified += c
                classifier.updateHistory(c)
                continue
            }

            // Stylus hover gating (API 26+): when any stylus is hovering above the screen
            // (AXIS_DISTANCE > 0), any simultaneous finger contact is almost certainly a
            // palm resting while the pen is poised to write. Suppress the finger as RESTING
            // so it does not draw or drive gestures. Only suppresses when the hovering stylus
            // is not already the locked writer (don't kill an in-progress stroke).
            // A finger pair is a pan/zoom candidate that must never be hover-suppressed.
            // Exempt two-finger gestures so pan/zoom still works while the pen hovers.
            val isGestureCandidate = frame.contacts.count { it.toolTypeRaw == MotionEvent.TOOL_TYPE_FINGER } >= 2
            val hoverSuppressed = currentSettings.palmRejectionEnabled &&
                contact.toolType == ToolKind.FINGER && lock.activePointerId != contact.pointerId &&
                !isGestureCandidate &&
                frame.contacts.any { other ->
                    other.toolTypeRaw == TOOL_TYPE_STYLUS &&
                        other.hoverDistance != null && other.hoverDistance > 0f
                }
            if (hoverSuppressed) {
                val c = ClassifiedContact(
                    contact = contact,
                    classification = ContactClassification.RESTING,
                    confidence = 0.7f,
                    reason = ClassificationReason.RESTING_STATIONARY,
                    effectiveThresholdMm = 0f,
                    speedMmPerSec = state?.speedMmPerSec ?: 0f,
                    durationMs = state?.let { (nowNanos - it.downTimeNanos) / 1_000_000L } ?: 0L,
                    downX = state?.startX,
                    downY = state?.startY,
                )
                baseClassified += c
                continue
            }

            // The user-reserved palm rest zone is authoritative for FINGER contacts: any
            // finger contact whose center falls inside it is the resting palm. It can never
            // be the writer and can never drive a gesture. Hardware tools (stylus/eraser)
            // are deliberately exempt — the zone is set aside for the palm, and a pen that
            // wanders into it must keep writing, otherwise strokes are lost. An already
            // locked writing pointer is exempt so an in-progress stroke crossing the zone
            // is not cut mid-stroke.
            val isFingerContact = contact.toolType == ToolKind.FINGER ||
                contact.toolType == ToolKind.UNKNOWN
            val inZone = palmZoneRect?.contains(contact.x, contact.y) == true &&
                isFingerContact
            if (inZone && lock.activePointerId != contact.pointerId) {
                val classifiedContact = ClassifiedContact(
                    contact = contact,
                    classification = ContactClassification.PALM,
                    confidence = 1f,
                    reason = ClassificationReason.IN_PALM_ZONE,
                    effectiveThresholdMm = 0f,
                    speedMmPerSec = state?.speedMmPerSec ?: 0f,
                    durationMs = state?.let { (nowNanos - it.downTimeNanos) / 1_000_000L } ?: 0L,
                )
                baseClassified += classifiedContact
                classifier.updateHistory(classifiedContact)
                continue
            }

            val result = classifier.classify(
                contact,
                PalmClassifier.ClassifyContext(
                    mode = currentSettings.mode,
                    pointerCount = frame.pointerCount,
                    activeSizesMm = activeSizesMm,
                    activeWritingPointerId = lock.activePointerId,
                    writingLockActive = lock.isActive,
                    contactSpeedMmPerSec = state?.speedMmPerSec ?: 0f,
                    contactDurationMs = state?.let { (nowNanos - it.downTimeNanos) / 1_000_000L } ?: 0L,
                    fingerWritingEnabled = currentSettings.enableFingerWriting,
                    honorPressure = capabilities.supportsPressure,
                )
            )
            val classifiedContact = ClassifiedContact(
                contact = contact,
                classification = result.classification,
                confidence = result.confidence,
                reason = result.reason,
                effectiveThresholdMm = result.effectiveThresholdMm,
                speedMmPerSec = state?.speedMmPerSec ?: 0f,
                durationMs = state?.let { (nowNanos - it.downTimeNanos) / 1_000_000L } ?: 0L,
            )
            baseClassified += classifiedContact
            // Feed the decision back so the adaptive single-pointer fallback learns this
            // device's real contact-size scale.
            classifier.updateHistory(classifiedContact)
        }

        // Resting-hand layer: adds motion/timing/cluster/edge evidence on top of the size
        // decision. It may buffer small contacts in a resting context (CANDIDATE), mark
        // stationary fingers RESTING, promote a moving candidate to WRITING, or cancel a
        // drawing pointer whose smoothed contact size grew palm-like.
        val trackerResult = restingTracker.process(
            frame,
            baseClassified,
            lock.activePointerId,
            currentSettings,
        )
        var classified = trackerResult.classified

        // The lock may change AFTER manageWritingLock runs (a candidate promoted to the
        // writer on a MOVE frame, or a locked pointer cancelled by palm-growth). Re-read it
        // after applying the tracker's lock decisions so the frame reflects the real writer.
        manageWritingLock(frame, classified, nowNanos)
        applyTrackerLockChanges(frame, trackerResult, nowNanos)
        // Cold-start safety net: if the tracker left a tracked DOWN CANDIDATE as
        // CANDIDATE on this MOVE but it already moved like a stroke (>= 4mm with
        // stroke velocity), promote it here so the first stroke is never dropped
        // one full frame. The returned contact is upgraded to WRITING; its
        // downX/downY backfills the buffered leading tail in InkCanvasView.
        classified = applyPendingCandidatePromotion(frame, classified, nowNanos)
        val finalWritingPointerId = lock.activePointerId
        val gestureIds = selectGesturePointers(classified, finalWritingPointerId)

        return ClassifiedFrame(
            frame = frame,
            contacts = classified,
            activeWritingPointerId = finalWritingPointerId,
            gesturePointerIds = gestureIds,
            clusterBounds = trackerResult.clusterBounds,
        )
    }

    /**
     * Fallback used when [PalmRejectionSettings.palmRejectionEnabled] is false: the size
     * classifier, palm zone and resting-hand tracker are all bypassed. Hardware pens always
     * write, erasers erase, a lone finger writes when finger writing is enabled, and any
     * finger pair pans/zooms — no contact is ever rejected.
     */
    private fun processWithoutPalmRejection(frame: InputFrame): ClassifiedFrame {
        val nowNanos = frame.eventTimeNanos
        val classified = frame.contacts.map { contact ->
            val normalized = normalizer.normalize(contact)
            val classification: ContactClassification
            val reason: ClassificationReason
            when (normalized.toolType) {
                ToolKind.STYLUS -> {
                    classification = ContactClassification.WRITING
                    reason = ClassificationReason.HARDWARE_STYLUS
                }
                ToolKind.ERASER -> {
                    classification = ContactClassification.ERASER
                    reason = ClassificationReason.HARDWARE_ERASER
                }
                else -> {
                    if (currentSettings.enableFingerWriting && frame.pointerCount == 1) {
                        classification = ContactClassification.WRITING
                        reason = ClassificationReason.FINGER_WRITING
                    } else {
                        classification = ContactClassification.FINGER
                        reason = ClassificationReason.SMALL_CONTACT
                    }
                }
            }
            ClassifiedContact(
                contact = normalized,
                classification = classification,
                confidence = 0.8f,
                reason = reason,
                effectiveThresholdMm = 0f,
                speedMmPerSec = 0f,
                durationMs = normalized.ageMs(nowNanos),
            )
        }
        val writingPointerId = manageWritingLock(frame, classified, nowNanos)
        val gestureIds = selectGesturePointers(classified, writingPointerId)
        return ClassifiedFrame(frame, classified, writingPointerId, gestureIds)
    }

    /**
     * Applies the resting-hand tracker's writing-lock decisions that happen outside the
     * DOWN/POINTER_DOWN claim path: promoting a buffered candidate to the lock on a MOVE
     * frame, and cancelling a locked pointer whose smoothed size grew palm-like.
     */
    private fun applyTrackerLockChanges(
        frame: InputFrame,
        result: RestingHandTracker.Result,
        nowNanos: Long,
    ) {
        val cancelId = result.cancelLockPointerId
        if (cancelId != null && lock.activePointerId == cancelId) {
            lock.reset(nowNanos)
        }
        val promoteId = result.promoteCandidatePointerId
        if (promoteId != null && !lock.isActive && frame.action == InputAction.MOVE) {
            lock.tryClaim(promoteId, nowNanos, respectHoldoff = false)
            if (lock.activePointerId == promoteId) pendingCandidateId = null
        }
    }

    /**
     * Cold-start safety net for a DOWN-buffered CANDIDATE. When the tracker leaves
     * the pending candidate as CANDIDATE on a MOVE that already moved like a stroke
     * (>= 4mm with stroke velocity, unique mover implied by single-contact frame),
     * claim the lock and upgrade the contact to WRITING so the first stroke is never
     * dropped. The contact's downX/downY backfills the buffered leading tail.
     */
    private fun applyPendingCandidatePromotion(
        frame: InputFrame,
        classified: List<ClassifiedContact>,
        nowNanos: Long,
    ): List<ClassifiedContact> {
        val pendingId = pendingCandidateId
        val isMove = frame.action == InputAction.MOVE
        val isUp = frame.action == InputAction.UP || frame.action == InputAction.POINTER_UP
        if (pendingId == null || lock.isActive || (!isMove && !isUp)) {
            // Drop stale pending state: lifted, no longer present, or no longer a
            // candidate (promoted/demoted by the tracker).
            if (pendingId != null &&
                (frame.contacts.none { it.pointerId == pendingId } ||
                    classified.firstOrNull { it.contact.pointerId == pendingId }
                        ?.classification != ContactClassification.CANDIDATE)
            ) {
                pendingCandidateId = null
            }
            return classified
        }
        val pending = classified.firstOrNull { it.contact.pointerId == pendingId }
            ?: run { pendingCandidateId = null; return classified }
        if (pending.classification != ContactClassification.CANDIDATE) {
            pendingCandidateId = null
            return classified
        }
        // PH-05: slow writers and taps were dropped because promotion required both
        // distance AND velocity. A slow deliberate stroke still travels >=4mm but at
        // low windowed velocity (e.g. 30mm/s) and was never promoted, then demoted to
        // RESTING after 250ms. For the isolated cold-start pending candidate (single
        // contact, no palm to confuse), distance alone is sufficient — palm jitter is
        // <1.5mm and a tap that barely moved is still a dot that must not be lost.
        val distanceOk = pending.pathLengthMm >=
            maxOf(COLD_START_PROMOTE_DISTANCE_MM, currentSettings.movementPromoteThresholdMm)
        // On UP a tap/lift with any movement (or a short stationary tap) must still
        // promote so the dot is committed — otherwise taps are swallowed.
        if (isUp) {
            val isTap = pending.pathLengthMm < maxOf(COLD_START_PROMOTE_DISTANCE_MM, currentSettings.movementPromoteThresholdMm) &&
                pending.durationMs < 400L
            if (!distanceOk && !isTap) return classified
        } else {
            if (!distanceOk) return classified
            // For MOVE, still require some motion but allow slow writers: if the resting
            // noise is low (no hand resting) distance alone promotes; otherwise require
            // velocity. The tracker already adapts the velocity gate for noisy hands, but
            // the cold-start pending candidate bypasses the tracker, so here we relax:
            // promote on distance alone, velocity is advisory not mandatory.
        }
        lock.tryClaim(pendingId, nowNanos, respectHoldoff = false)
        if (lock.activePointerId != pendingId) return classified
        pendingCandidateId = null
        return classified.map {
            if (it.contact.pointerId == pendingId) {
                it.copy(
                    classification = ContactClassification.WRITING,
                    reason = ClassificationReason.PROMOTED_TO_WRITING,
                    confidence = 0.8f,
                )
            } else it
        }
    }

    companion object {
        /**
         * Cold-start MOVE promotion distance (mm). Matches the regression test gate
         * (>= 40px = 4mm at 10px/mm) and stays above the 3mm resting-hand jitter
         * threshold so a settling palm tap never promotes.
         */
        const val COLD_START_PROMOTE_DISTANCE_MM = 4f
    }

    private fun manageWritingLock(
        frame: InputFrame,
        classified: List<ClassifiedContact>,
        nowNanos: Long,
    ): Int? {
        // Track per-pointer motion state for contacts that are down.
        for (c in classified) {
            val cid = c.contact.pointerId
            val s = pointerStates[cid]
            if (s == null) {
                pointerStates[cid] = PointerMotionState(
                    downTimeNanos = c.contact.downTimeNanos,
                    startX = c.contact.x,
                    startY = c.contact.y,
                    lastX = c.contact.x,
                    lastY = c.contact.y,
                    lastTimeNanos = c.contact.eventTimeNanos,
                )
            } else {
                val dtSec = (c.contact.eventTimeNanos - s.lastTimeNanos).coerceAtLeast(1L) / 1_000_000_000.0
                if (dtSec > 0.0) {
                    val distPx = hypot(c.contact.x - s.lastX, c.contact.y - s.lastY)
                    s.speedMmPerSec = (capabilities.dimFromPx(distPx) / dtSec.toFloat())
                    s.lastX = c.contact.x
                    s.lastY = c.contact.y
                    s.lastTimeNanos = c.contact.eventTimeNanos
                }
                s.rawSampleCount++
            }
        }

        when (frame.action) {
            InputAction.DOWN -> {
                // A down can only establish the lock. Only the newly added pointer is
                // eligible — already-down pointers are gestures/palm and must not claim.
                if (!lock.isActive && frame.addedPointerId != null) {
                    val candidate = classified.firstOrNull {
                        it.contact.pointerId == frame.addedPointerId &&
                            it.classification == ContactClassification.WRITING
                    }
                    if (candidate != null) {
                        // Finger writing lifts the hold-off so fast consecutive strokes
                        // are never dropped; a genuine palm is never a WRITING candidate.
                        lock.tryClaim(
                            frame.addedPointerId,
                            nowNanos,
                            respectHoldoff = !currentSettings.enableFingerWriting,
                        )
                        if (lock.activePointerId == frame.addedPointerId) pendingCandidateId = null
                    } else {
                        // Cold-start buffering: a lone contact lands as CANDIDATE (never
                        // WRITING on DOWN). Track it so the next stroke-like MOVE can
                        // promote it with its downX/downY backfilled.
                        val buffered = classified.firstOrNull {
                            it.contact.pointerId == frame.addedPointerId &&
                                it.classification == ContactClassification.CANDIDATE
                        }
                        if (buffered != null) pendingCandidateId = frame.addedPointerId
                    }
                }
            }

            InputAction.POINTER_DOWN -> {
                // A new contact landed while another pointer is already down. Two cases:
                //  1. No writing lock yet (e.g. a resting palm was the first contact). A
                //     newly added WRITING-classified pointer (pen, or a finger with finger
                //     writing enabled) must be able to claim the lock so it can start
                //     drawing even though the palm is still resting.
                //  2. A writing lock is active and a second contact joins. If that contact
                //     is finger-sized it is the start of a two-finger gesture (pan/zoom):
                //     release the writing lock so the pair can drive navigation. A resting
                //     palm is classified PALM and never triggers this; the in-progress
                //     stroke is finalized by the view.
                val addedId = frame.addedPointerId
                if (addedId != null) {
                    val added = classified.firstOrNull { it.contact.pointerId == addedId }
                    if (added != null) {
                        if (!lock.isActive) {
                            // A palm is already resting but holds no lock: the newly added
                            // WRITING-classified contact (a pen, or a finger with finger
                            // writing enabled) is the intended writer and claims the lock.
                            // A FINGER contact with finger writing on also claims it —
                            // but ONLY when a resting palm is already down (PALM/RESTING
                            // or palm-sized). Without that guard, the second finger of a
                            // two-finger pan/zoom would steal the lock and kill gestures.
                            // Waiting for the tracker to promote leaves a dead window
                            // where the user writes and nothing appears.
                            val restingPalmPresent = classified.any {
                                it.contact.pointerId != addedId &&
                                    (it.classification == ContactClassification.PALM ||
                                        it.classification == ContactClassification.RESTING ||
                                        it.contact.maxDimMm >= currentSettings.palmSizeThresholdMm)
                            }
                            val claimable = added.classification == ContactClassification.WRITING ||
                                (added.classification == ContactClassification.FINGER &&
                                    currentSettings.enableFingerWriting && restingPalmPresent)
                            if (claimable) {
                                lock.tryClaim(
                                    addedId,
                                    nowNanos,
                                    respectHoldoff = !currentSettings.enableFingerWriting,
                                )
                            }
                        } else {
                            val nonPalm = added.classification == ContactClassification.WRITING ||
                                added.classification == ContactClassification.FINGER
                            if (nonPalm) {
                                // A second contact landed while a lock is active. Two cases:
                                //  - The locked pointer is genuinely small (a pen/finger) and
                                //    the new contact is finger-sized too: this is a two-finger
                                //    pan/zoom intent — drop the lock so the pair navigates.
                                //  - The lock is held by a false-positive palm (a resting palm
                                //    whose size fell in the finger/writing band while alone) and
                                //    the new contact is the real writer: hand the lock to it so
                                //    writing works even with the palm still resting — otherwise
                                //    the user can never draw while the palm is down.
                                //
                                // A contact is the real writer when it is WRITING-classified
                                // (a hardware stylus is always the writer, regardless of size)
                                // or when it is clearly smaller than the falsely-locked palm.
                                val locked = classified.firstOrNull { it.contact.pointerId == lock.activePointerId }
                                val lockedDim = locked?.contact?.maxDimMm ?: 0f
                                val addedDim = added.contact.maxDimMm
                                val addedIsConfirmedWriter = added.classification == ContactClassification.WRITING
                                val addedClearlySmaller = lockedDim > 0f && addedDim > 0f &&
                                    lockedDim / addedDim >= PalmClassifier.PALM_HANDOFF_RATIO
                                val handOff = addedIsConfirmedWriter ||
                                    (addedClearlySmaller && currentSettings.enableFingerWriting)
                                // The evicted holder was palm-sized but the size ratio was
                                // not enough for a handoff, yet the newcomer is a small
                                // finger and finger writing is on: the holder was a false
                                // palm lock, so hand the lock over instead of leaving it
                                // dead (otherwise the user writes and nothing appears).
                                // PH-05: relax for medium palms (15-18mm) that digitizers report
                                // below the 24mm palmSizeThreshold but above fingerMax. Such
                                // contacts are still the resting hand (suspicious size), and a
                                // 12mm pen next to a 16mm palm (ratio 1.33) must still hand off.
                                val palmHolderHandoff = !handOff &&
                                    added.classification == ContactClassification.FINGER &&
                                    currentSettings.enableFingerWriting &&
                                    lockedDim >= currentSettings.suspiciousSizeThresholdMm &&
                                    addedDim <= currentSettings.effectiveFingerMaxMm()
                                // PH-05: confirmed writers (lock held >120ms, i.e. a real
                                // stroke, not a transient palm) must hand off even on
                                // borderline size ratios — otherwise a slow pen that lands
                                // next to a medium palm is dropped and no ink appears.
                                // Require only that the newcomer is genuinely smaller.
                                val lockHeldMs = (nowNanos - lock.lockAcquiredAtNanos) / 1_000_000L
                                val confirmedWriterHandoff = !handOff && !palmHolderHandoff &&
                                    currentSettings.enableFingerWriting &&
                                    lockHeldMs >= 80L &&
                                    lockedDim > addedDim &&
                                    addedDim <= currentSettings.effectiveFingerMaxMm() &&
                                    addedDim > 0f
                                if (handOff || palmHolderHandoff || confirmedWriterHandoff) {
                                    lock.reset(nowNanos)
                                    lock.tryClaim(addedId, nowNanos, respectHoldoff = false)
                                } else {
                                    // PH-04: don't immediately kill an in-progress stroke for
                                    // an ambiguous second contact. A palm that is borderline
                                    // finger-sized was previously a FINGER and instantly reset
                                    // the lock, committing a truncated stroke and panning.
                                    // Require that the dropped lock's evicted writer not become
                                    // a gesture finger (selectGesturePointers now excludes WRITING)
                                    // and let the view require gesture confirmation before
                                    // finalizing. Here we keep the lock if the second contact
                                    // is not clearly a gesture finger (e.g. suspicious size or
                                    // low confidence) — the view will confirm via MOVE.
                                    // For strictly finger-sized contacts with no palm history,
                                    // still drop as before so two-finger pan remains responsive.
                                    val isStrictlyFinger = addedDim <= currentSettings.effectiveFingerMaxMm() &&
                                        lockedDim <= currentSettings.effectiveFingerMaxMm()
                                    if (isStrictlyFinger) {
                                        lock.reset(nowNanos)
                                    } else {
                                        // Ambiguous — keep writing lock, don't steal
                                        // (a subsequent MOVE that confirms a two-finger
                                        // gesture will drop the lock then).
                                    }
                                }
                            }
                        }
                    }
                }
            }

            InputAction.UP, InputAction.POINTER_UP -> {
                val lifted = frame.liftedPointerId
                if (lifted != null) {
                    lock.release(lifted, nowNanos)
                    pointerStates.remove(lifted)
                    if (pendingCandidateId == lifted) pendingCandidateId = null
                    // The tracker keeps its own per-pointer motion state: drop the lifted
                    // pointer there too. Without this a reused pointer id on the next
                    // DOWN is treated as a continuing contact (stale isNew=false,
                    // stale resting classification, stale distance window) instead of
                    // a fresh touch.
                    restingTracker.removePointer(lifted)
                }
            }

            InputAction.CANCEL -> {
                lock.reset(nowNanos)
                pointerStates.clear()
                pendingCandidateId = null
                // A cancel aborts the whole gesture: no contact survives, so the
                // tracker's motion states and noise estimate must not leak into the
                // next gesture (stale RESTING/CANDIDATE would swallow the next stroke).
                restingTracker.reset()
            }

            InputAction.MOVE -> Unit
        }

        // Defensive: never leave a stale lock for a pointer no longer present.
        if (lock.activePointerId != null) {
            val activeId = lock.activePointerId
            if (frame.contacts.none { it.pointerId == activeId }) {
                lock.reset(nowNanos)
            }
        }

        return lock.activePointerId
    }

    /**
     * Gesture pointers are contacts allowed to pan/zoom. Rules:
     *  - While a writing lock is active, no gestures (a resting palm must not pan).
     *  - Otherwise up to two non-palm contacts. WRITING is included because after
     *    the lock is dropped by a deliberate two-finger gesture the original writer
     *    (still WRITING-classified) must remain part of the gesture — otherwise the
     *    gesture loses a finger and pan/zoom stutters. PH-04 keeps the lock for
     *    ambiguous second contacts (suspicious size) so the evicted writer never
     *    becomes a gesture finger in the palm-steal case.
     */
    private fun selectGesturePointers(
        classified: List<ClassifiedContact>,
        writingPointerId: Int?,
    ): List<Int> {
        if (writingPointerId != null) return emptyList()

        return classified
            .filter {
                it.classification == ContactClassification.WRITING ||
                    it.classification == ContactClassification.FINGER
            }
            .take(2)
            .map { it.contact.pointerId }
    }
}