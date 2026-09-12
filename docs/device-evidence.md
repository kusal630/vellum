# On-device evidence — tablet_api35, debug APK 2026-09-10 (post-A7)
Screenshots: /tmp/vellum-v2-editor.png, /tmp/vellum-v3-stroke.png (copies: keep in docs/device-shots/ when needed)

## Verified working
- Install Success, launch OK, zero FATAL (logcat), home renders, finger swipe leaves ink stroke.

## BUG 1 — toolbar icons invisible (uiautomator dump /tmp/vellum-ui2.xml)
- Nodes laid out on-screen: Back [436,88][484,136], Undo [532,88][580,136],
  Pen [832,64][928,160], Highlighter [932,64][1028,160], plus Eraser/Select/
  Shapes/Text/Image/Template/Auto-erase/pages/Export/Classroom/transcript.
- Pixels at those rects: empty beige band. Diagnosis: icon tint/contentColor
  matches container in light theme. Fix tints for light AND dark themes.

## BUG 2 — palm coachmark never displays
- PalmZoneCoachmark + showPalmZoneCoachmark (DataStore default true) exist in
  EditorScreen.kt / SettingsRepository.kt / PalmRejectionSettings.kt.
- Fresh-install editor dump: no coachmark / Rest-your-palm / Got-it node.
- Debug the display condition, fix once-per-install show.

## BUG 3 — PalmZoneHandle absent from composition
- No palm-handle node in dump at all. If gated behind default-off setting,
  default it visible; keep drag-resize + palmZone persist working.
