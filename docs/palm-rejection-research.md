# Palm Rejection Research Notes (external sources)

_Collected during the Vellum rework (2026-09-06). Sources: Android official
documentation (developer.android.com) — extracted verbatim points, condensed._

## 1. Signals Android provides that we should consume

### FLAG_CANCELED (API 33+)
- Android itself marks unintentional touches: "typically set when the user was
  accidentally touching the screen, such as by gripping the device, or placing
  the palm of the hand on the screen."
- Access: `(event.flags and MotionEvent.FLAG_CANCELED) != 0`.
- Required behavior: undo the last motion set **from the last ACTION_DOWN of that
  pointer** (found via `getPointerId(actionIndex)`) and re-render.
- Status in Vellum: NOT consumed -> improvement item.

### ACTION_CANCEL semantics (per-pointer)
- Fired for navigation gestures and OS palm rejection.
- Correct handling: identify the active pointer with
  `getPointerId(getActionIndex())`, remove ONLY the stroke created by that
  pointer from the input history, re-render the scene.
- Implication: we must keep a per-pointer input history to enable rollback, and
  clear per-pointer classifier/tracker state (fixed 2026-09-06 in
  PalmRejectionEngine/RestingHandTracker; engine-level rollback of provisional
  ink is a further improvement item).

### Hover (AXIS_DISTANCE) and hover actions
- For stylus: `getAxisValue(AXIS_DISTANCE)` 0.0 = contact, larger = hovering.
- Hover events (ACTION_HOVER_ENTER/MOVE/EXIT) arrive without contact.
- Rejection use: when a stylus is HOVERING (distance > 0), any simultaneous
  finger contact is almost certainly a palm -> suppress finger inking while
  stylus hover is active.
- Status in Vellum: NOT consumed -> improvement item.

### Edge flags (getEdgeFlags, ACTION_DOWN only)
- Reports display-edge contact; useful for edge-suppression heuristics (bezel
  grips produce edge-flagged large slow contacts).
- Status in Vellum: NOT consumed -> improvement item (low priority).

### Stylus identification
- `TOOL_TYPE_STYLUS` / `TOOL_TYPE_ERASER` (eraser also = inverted stylus posture).
- Verify pressure is normalized to 0..1 (values >1 possible); normalize before use.

## 2. Official recommended pipeline (Google)
1. Start stroke on ACTION_DOWN (per pointer), extend on ACTION_MOVE, finish on
   ACTION_UP.
2. Keep a history of user inputs so unwanted touches can be removed and the
   scene re-rendered (provisional ink + rollback model).
3. On ACTION_CANCEL / FLAG_CANCELED: rollback that pointer's motion set and
   re-render.
4. For full-screen apps: use WindowInsetsController
   `setSystemBeansBehavior(BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)` (sic, method
   name is setSystemBarsBehavior) so navigation swipes don't create stray marks.

## 3. Latency (adjacent but relevant to writing feel)
- Low-latency rendering: front-buffer rendering while a pointer is down
  (GL front-buffer / Jetpack low-latency graphics).
- Perceived latency: Jetpack motion prediction library predicts future motion
  events. (Would be a new dependency; evaluate later against the
  no-new-dependencies constraint.)

## 4. Concrete improvement backlog derived from this research
| # | Item | Priority |
|---|------|----------|
| 1 | Consume FLAG_CANCELED (API 33+): mark pointer as palm, rollback its provisional ink | High |
| 2 | Per-pointer ACTION_CANCEL rollback (only the canceled pointer's stroke, not global reset) | High |
| 3 | Stylus-hover gating: while stylus hover active, suppress finger/palm inking | High |
| 4 | Keep per-pointer provisional ink history to enable clean rollback + re-render | High |
| 5 | Edge-flag-assisted edge suppression for bezel grips | Medium |
| 6 | Normalize pressure >1.0 cases before feature use | Medium |
| 7 | Evaluate Jetpack low-latency graphics / motion prediction | Later (new dep) |

## 5. Validation methodology (from panel review + research)
- Log and heat-map accepted vs rejected contacts (x/y, toolMajor/minor, size,
  zone hits, classification) behind a debug flag.
- Calibration doodles to measure false-rejection rate for small/slow strokes.
- Rolling per-device percentile normalization of geometry features to survive
  screen-protector / glove / moisture drift.

## 6. 2026-09-18 wave: online research + synthesis (GoodNotes 6, Notability 15, Samsung Notes, ML Kit)

- Competitors converge on: adjustable sensitivity + writing-posture modes
  (GoodNotes), hover preview (Apple Pencil Pro), shape-hold straightening,
  handwriting search, customizable toolbar (Notability 15). Vellum already
  ships all but toolbar customization (added this wave) and ML-backed search.
- ML Kit Digital Ink Recognition (Gboard tech, 300+ languages, ~100ms/line,
  ~20MB model download) was evaluated and REJECTED for vellum: the download
  and Google dependency violate the offline/no-vendor doctrine. The bundled
  $1 recognizer was extended into page-level indexing (InkIndexer) instead —
  zero deps, zero downloads, FTS-searchable today.
- Hover suppression (S Pen hover + finger down) was engine-dead-code: touch
  frames never carry the hovering pen. Fixed with an onHoverEvent-driven
  latch in this wave; covered by replay fixtures.
- Fuzz finding: unphysical size teleportation in synthetic streams can strand
  WRITING on huge contacts; physical streams (stable size +/-10%, continuous
  motion) hold the never-write invariant across 200 seeds x 30 frames.
  Palm-growth cancel verified: EMA + hysteresis ride out spikes, then
  PALM_GROWTH_CANCELLED releases the lock once settled.

## 7. 2026-09-18 wave 2: fluid editing (GoodNotes Smart Ink parity, offline)

- Smart Ink reflow/insert-space, circle-to-lasso, scribble-to-erase and
  handwriting search are the 2026 battleground (GoodNotes 6, Notability 15).
  Vellum now ships insert-space (drag-a-gap with undoable command),
  circle-to-select (fast closed loops; area-gated so zigzags never match)
  and handwriting search; scribble-erase pre-existed.
- Cloud AI (spellcheck-in-own-handwriting, Ask-docs, math solver) stays out
  per the offline doctrine; the same jobs are covered on-device where
  feasible ($1 recognition, extractive summary).
- Two-finger pan with a palm down was broken (fingers latched as writers);
  fixed by restricting lock handoff to hardware writers.

## 8. 2026-09-18 wave 3: no pending moats

- Audio-linked ink replay: recording-start wall anchor persisted per page;
  transcript seeks and Replay play drive the same ink cutoff clock.
- Offline spellcheck ships for typed text (73k-word bundled list, SCOWL
  attribution in third-party-notices.md); ink-word correction stays out
  (rewriting strokes needs ML; doctrine).
- System Notes role (CREATE_NOTE + showWhenLocked): tail-button and
  lock-screen quick note, per Android stylus guidance.
- Platform Motion Prediction Jetpack + Material expressive MotionScheme
  evaluated: alpha-only APIs on the pinned BOM; hand-rolled predictor and
  spring animator stay until they go stable.
- 2026 UX guidance applied: distinct haptics per gesture (reject vs snap),
  compound-gesture discovery via overflow labels, four-level dark surfaces
  already in theme.
