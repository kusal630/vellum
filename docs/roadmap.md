# Roadmap

## Phase 0 — Foundation ✅
- [x] Environment: Android SDK, Gradle wrapper, project skeleton
- [x] Version catalog, AGP/Kotlin/Compose configuration
- [x] Buildable minimal app + theme + icon

## Phase 1 — Input System ✅ (M1 complete)
- [x] Documentation (architecture, palm rejection, input, drawing, data model)
- [x] `InputCapabilities` device detection (honest, hardware-derived)
- [x] `MotionEventParser` + `InputNormalizer`
- [x] Pure `PalmClassifier` + `PalmRejectionEngine` + `WritingLock`
- [x] Rejection modes (Strict/Balanced/Relaxed/Writing)
- [x] Diagnostics + calibration screen (live per-contact readout, mode selector, save-pen/finger/palm)
- [x] Smoothing: None/Low/Medium/High (streaming, endpoint-exact)
- [x] Unit tests for the full input pipeline (33 passing)

## Phase 2 — Drawing Engine ✅ (M2 complete)
- [x] `InkPathBuilder`, pen rendering (ballpoint, gel/monoline, fountain, pencil, marker, calligraphy)
- [x] Low-latency active-stroke rendering + committed-layer display list cache
- [x] Smoothing: None/Low/Medium/High (streaming, endpoint-exact)
- [x] Dirty-rect invalidation, zoom/pan viewport with scroll bar
- [x] Variable-width pen rendering (fountain/calligraphy filled polygons, pencil 3-pass grain)
- [x] Highlighter below ink, eraser integration
- [x] Shape rendering (rect, triangle, circle, ellipse, line, arrow, star, hexagon)
- [x] Text object rendering (word-wrapped, bold, rotated)
- [x] Image rendering (z-ordered between paper and ink)
- [x] Palm rest zone visualization (manual mode)

## Phase 3 — Editor ✅ (M3 complete)
- [x] Notebook list + creation (Normal/Classroom types, cover picker, template picker)
- [x] Page list, thumbnails, page management (add/reorder/delete)
- [x] Editor scaffold: toolbar, top bar, paper background
- [x] Editor state: tools, undo/redo command stack
- [x] Notebook cover gallery (8 premium gradient/pattern covers)
- [x] Settings screens (writing, gestures, appearance, storage, advanced)
- [x] Dark mode
- [x] Infinite canvas with pinch-to-zoom and pan
- [x] Multi-page notebooks with page navigator
- [x] Toolbar with pen/highlighter/eraser/select/shapes/text/image/template/auto-erase tools
- [x] Color palette (11-color) + pen width selection
- [x] Smoothing mode selector
- [x] Commit-time stroke thinning (RDP ε=0.05mm in StrokeBuilder.onUp)
- [x] Predicted-tip ghost segment on the live stroke (StrokePredictor, 1 frame)
- [x] Stylus hover latch: view feeds onHoverEvent into the engine hover gate
- [x] Two-finger pan with palm resting: hardware-writer handoff rule (finger-sized writers yield to pinch)
- [x] Ink replay: commit timestamps + Replay tab with play/scrub over stroke history
- [x] Zoom writing aid: 2.5x magnified strip with remapped ink input
- [x] White paper default + paper fills viewport (no desk void) + White/Dark toggle in template dialog
- [x] Insert-space reflow (drag a gap; undoable ReflowContentCommand)
- [x] Circle-to-select pen gesture (fast closed loops select instead of inking)
- [x] Spring viewport animator + Fit to page + toolbar auto-hide while writing
- [x] Replay fade-in (fresh strokes ease from 25% alpha over 600ms)
- [x] Audio-linked replay (recording anchor + transcript clock drives ink cutoff)
- [x] Offline spellcheck for typed text (bundled wordlist, tap-to-apply suggestions)
- [x] System Notes role (CREATE_NOTE: tail-button + lock-screen quick note)
- [x] Zoom focus clamped to content; haptic snaps on circle-select + insert-space
- [x] FAR/FRR-gated replay battery + left-hand/edge/hover/pinch fixtures
- [x] Auto-erase (write/erase detection) toggle
- [x] Page template picker per page
- [x] Selection: lasso, move, resize (8 handles), duplicate, delete
- [x] Eraser with adjustable size
- [x] Shape hold-to-straighten (via ShapeRenderer)

## Phase 4 — Persistence ✅ (M3 complete)
- [x] Room schema, DAOs, repository
- [x] Document serialization (strokes, text, image, shape)
- [x] Autosave (debounce + lifecycle hooks)
- [x] Crash recovery journal

## Phase 5 — Tools ✅ (M4 complete)
- [x] Full pen set (ballpoint, gel/monoline, fountain, pencil, marker, calligraphy)
- [x] Color system (11-color palette)
- [x] Erasers (stroke / segment / area)
- [x] Selection (lasso, rect, move/resize/duplicate/delete)
- [x] Shapes (rect, triangle, circle, ellipse, line, arrow, star, hexagon) + hold-to-straighten
- [x] Text tool (typed text boxes, word-wrapped, bold, rotated)
- [x] Image insertion (photo picker via SAF), move/scale

## Phase 6 — Export ✅ (M4 complete)
- [x] PDF export (vector paths, backgrounds, images)
- [x] PDF import + annotation (every page becomes a writable page with ink on top)
- [x] PNG/JPEG export (page + notebook)

## Phase 7 — Polish ✅
- [x] Settings screens (writing, gestures, appearance, storage, advanced)
- [x] Toolbar customization (hide/show tools, persisted, min-one-visible guard)
- [x] Handwriting OCR search (InkIndexer: $1 word/line grouping into FTS body; 60-word budget <300ms)
- [x] Dark mode (Material3 dynamic + DarkColors), toolbar/canvas TalkBack labels audited
- [x] Performance passes (10k-item cull budget test, commit-pipeline budget tests)

## Phase 8 — Ship readiness
- [ ] Instrumented hardware test matrix (requires physical devices; unit + Robolectric gates green)
- [x] Release build, proguard (`app/proguard-rules.pro`), signing docs (`docs/release-signing.md`; CI release needs repo secrets)
- [x] Play/F-Droid packaging notes (`docs/play-*.md`, `docs/f-droid-submission.md`)

## Honest Limitations (never faked)
- **Handwriting recognition**: bundled $1 print recognizer powers Convert and
  search indexing (A–Z/0–9 print; "?" marks low confidence). Cursive and
  300-language ML models (e.g. ML Kit digital-ink, ~20MB download + Google
  dependency) are deliberately out: they break the offline/no-vendor doctrine.
- **Passive stylus detection**: strictly software-based; the diagnostics screen reports
  exactly what the device exposes. No claims of hardware stylus identification when the
  OS provides none.
- **Cloud sync**: out of scope (offline-first).

## Milestones
1. **M1**: Input pipeline + diagnostics + palm rejection with passing unit tests. ✅ (2026-09-07)
2. **M2**: Write a real stroke on a canvas with low latency + undo/redo. ✅ (2026-09-07)
3. **M3**: Notebooks/pages persist across restart. ✅ (2026-09-07)
4. **M4**: Full toolset + export. ✅ (2026-09-07)
5. **M5**: Polish + hardware validation. 🔄 In progress (settings done; accessibility, toolbar customization, performance tuning, hardware test matrix remaining)