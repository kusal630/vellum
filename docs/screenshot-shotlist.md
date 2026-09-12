# Screenshot Shotlist — One-Pass Emulator Runbook

Goal: capture all 9 Play / F-Droid phone screenshots in a single emulator
session, in numeric order, at 1080x1920 portrait.

Output files (repo copies):

```text
fastlane/metadata/android/en-US/images/phoneScreenshots/01_home.png
fastlane/metadata/android/en-US/images/phoneScreenshots/02_ink_canvas.png
fastlane/metadata/android/en-US/images/phoneScreenshots/03_highlighter.png
fastlane/metadata/android/en-US/images/phoneScreenshots/04_palm_zone.png
fastlane/metadata/android/en-US/images/phoneScreenshots/05_pdf_annotate.png
fastlane/metadata/android/en-US/images/phoneScreenshots/06_read_mode.png
fastlane/metadata/android/en-US/images/phoneScreenshots/07_classroom.png
fastlane/metadata/android/en-US/images/phoneScreenshots/08_settings.png
fastlane/metadata/android/en-US/images/phoneScreenshots/09_labs.png
```

Specs for every shot: 1080x1920 px, PNG, sRGB, English (US), under 8 MB.
Full asset specs: `docs/play-graphics.md`.

## Emulator setup (do once, before shot 01)

1. Device: Pixel phone profile set to 1080x1920 (e.g. Pixel 5 / Pixel 8 with
   custom resolution), portrait. API 35, matching `targetSdk = 35`.
2. Language English (US), light theme, font scale 1.0x, gesture nav on.
3. Status bar: full battery, Wi-Fi on or airplane mode with Wi-Fi icon hidden
   consistently, clock set to a fixed time (e.g. 9:00), no notifications.
   Enable Demo Mode (`adb shell settings put global sysui_demo_allowed 1`
   then `adb shell am broadcast -a com.android.systemui.demo -e command enter`)
   if available.
4. Fresh install: `adb uninstall com.vellum.notes` then install the release
   build under test. Deny no permissions; grant none unless a shot calls for it.
5. Seed data before capturing:
   - Create notebooks: `Physics 101`, `Sketchbook`, `Meeting notes`.
   - In `Physics 101`, create a note `Lecture 3 — Forces` with 2 pages of
     sample handwriting so Home shows non-empty previews.
   - Import one single-page PDF (public-domain or self-made, e.g. a one-page
     `sample-lecture.pdf`) for shot 05.
   - For shot 07, prepare a short classroom transcript (2–4 lines) saved in a
     note titled `Bio 201 — Transcript`; if the Vosk model is absent the UI
     disables recording gracefully — capture the transcript + summary state,
     not the download prompt.
6. Disable auto-rotate. Turn off developer toasts and USB-debugging overlays.
7. Capture with the emulator camera / `adb exec-out screencap -p` at native
   1080x1920; do not scale, crop, or frame with device bezels.

Shoot in order 01–09 without wiping data between shots. If a shot fails,
retake only that shot; do not reseed mid-pass.

## Pass order

### 01 — Home (`01_home.png`)

- Screen: `ui.home.HomeScreen` notebook list.
- Setup: from cold launch after seeding step 5. Stay on the Home tab.
- Must be visible: `Vellum` title, the three seeded notebooks with page
  previews, FAB / new-note action, bottom nav unselected except Home.
- Pass criteria: no empty-state illustration dominates; no placeholder repo
  text; list fills at least half the screen.

### 02 — Ink canvas, pen (`02_ink_canvas.png`)

- Screen: `ui.editor.EditorScreen` + `InkCanvasView`, pen tool active.
- Setup: open `Physics 101 / Lecture 3 — Forces`, select pen, black, medium
  width. Write 2–3 words plus a small diagram (arrow + box) with a finger or
  stylus so real ink is on the canvas.
- Must be visible: handwritten ink strokes, pen toolbar selected state,
  infinite-canvas dotted/grid paper background, page indicator.
- Pass criteria: ink is crisp and continuous; toolbar is not covering the ink;
  no palm overlay visible in this shot (that is shot 04).

### 03 — Highlighter (`03_highlighter.png`)

- Screen: same editor note, highlighter tool active.
- Setup: without leaving the note, switch to highlighter (yellow), swipe 2
  translucent strokes over the words written in shot 02.
- Must be visible: yellow translucent highlight layered over dark ink, with
  the highlighter tool highlighted in the toolbar and the underlying text
  still readable through it.
- Pass criteria: translucency is obvious; before/after contrast (ink +
  highlight) in one frame.

### 04 — Palm zone / resting hand (`04_palm_zone.png`)

- Screen: editor with palm-zone overlay enabled
  (`SettingsContent` palm-zone AUTO + canvas resting-hand indicator).
- Setup: enable Palm rejection + resting-hand mode in Settings, set palm-zone
  side to match the drawing hand. Return to the same note, rest the palm edge
  (or enable the debug zone overlay) so the shaded palm-rest zone renders at
  the canvas edge while the pen keeps drawing.
- Must be visible: shaded palm-rest zone band, a palm contact rendered as
  rejected (no ink under it), plus one clean pen stroke beside it proving the
  palm did not leave ink.
- Pass criteria: zone + rejected palm + clean ink all in frame; toolbar
  visible so reviewers connect it to the editor.

### 05 — PDF annotate (`05_pdf_annotate.png`)

- Screen: `ui.reader.PdfReaderScreen` with ink over the imported PDF.
- Setup: open `sample-lecture.pdf` from the note attachments or reader entry
  point. With pen or highlighter, underline one heading and circle one figure.
- Must be visible: rendered PDF page content, at least two annotation strokes
  (underline + circle), PDF toolbar / page scrubber.
- Pass criteria: annotations clearly sit on top of the PDF (not on blank
  paper); page text remains legible.

### 06 — Read mode (`06_read_mode.png`)

- Screen: `ui.reader.ReadModeScreen` (and `HighlightsScreen` entry if visible).
- Setup: from the same PDF or a text note, enter Read mode. Scroll so a full
  column of clean rendered text fills the screen.
- Must be visible: distraction-free reading layout, readable serif/sans body
  text, minimal chrome (no editing toolbar), highlights affordance if present.
- Pass criteria: visually distinct from shots 02–05 — no drawing toolbar,
  reading-optimized margins and typography.

### 07 — Classroom Notes (`07_classroom.png`)

- Screen: note with Classroom Notes transcript + summary
  (`speech/SpeechController`, `SummaryGenerator` output).
- Setup: open `Bio 201 — Transcript`. Show the transcript block (2–4 lines,
  e.g. lecture sentences) with the generated summary line and the
  recording-stopped state. If the device has no microphone grant, show the
  permission rationale inline rather than the system dialog.
- Must be visible: transcript text saved with the note, summary line,
  offline badge / stopped-recording state, note title proving it is stored
  alongside handwriting.
- Pass criteria: no error toasts, no model-download prompt as the hero; the
  offline transcript is the focus.

### 08 — Settings (`08_settings.png`)

- Screen: `MainActivity.SettingsScreen` / `SettingsContent`.
- Setup: navigate Home > Settings. Scroll to the Palm Rejection section so
  the mode segmented control, sensitivity slider, and palm-rejection toggle
  are in frame.
- Must be visible: `Settings` title, Writing + Palm Rejection sections,
  selected mode, sensitivity value, toggles. Light theme.
- Pass criteria: no scrolled-to-blank area; at least three settings rows plus
  section headers visible; no debug-only sliders dominating.

### 09 — Labs diagnostics (`09_labs.png`)

- Screen: `ui.diagnostics.DiagnosticsScreen` (header titled `Labs`).
- Setup: navigate Home > Labs (Science icon). Place a finger + palm on the
  canvas (or use the touch simulator) so Live contact data populates, then
  hold for the capture.
- Must be visible: `Labs` title, Input capabilities card, Live contact data
  card with at least one contact row, Palm rest zone card with values.
- Pass criteria: live data present (not all zeros/empty); proves the input
  pipeline is real hardware-measured.

## After the pass

1. Verify each file is exactly 1080x1920 with
   `file fastlane/metadata/android/en-US/images/phoneScreenshots/*.png`.
2. Open each PNG at 100% and check: English strings, no personal data, no
   notification icons, no battery warnings, no half-rendered ink.
3. Copy the same 9 files to the Play Console listing (phone screenshots) —
   do not rename between F-Droid and Play so reviews match.
4. Do not commit device frames, videos, or raw `screencap` dumps outside
   `phoneScreenshots/`; keep the listing directory to the 9 files above.
