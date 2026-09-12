# Canvas mega-pack verification plan (Conductor on-device pass)
Build: debug APK from merge gate + A4 mega-pack. Device: tablet_api35 emulator, headless.

## P0-1 status chip
- [ ] Chip visible top-center below toolbar on editor open, reads Pen ready
- [ ] Palm touch shows Palm rejected + fading ring on canvas, no ink left
- [ ] Two-finger touch shows pan state; TalkBack announces state changes

## P0-2 toolbar
- [ ] No horizontal scroll on narrow width; 8 tools visible, rest under More
- [ ] Selected tool highlighted + label; switching animates; context panel opens for pen/eraser

## P0-3 palm handle
- [ ] Dashed handle bottom-right; drag resizes zone; first-run coachmark appears once

## P1-4 pickers
- [ ] No ColorRail overlay; 12 named swatches with check on selected; no hex announcements
- [ ] 5 widths with stroke preview + mm; smoothing OFF/STEADY/FLOW chips

## P1-6 trust
- [ ] Sync is a status dot, not a pill; undo/redo dim when disabled; page loading shimmers

## Regression
- [ ] No FATAL in logcat; palm-first / mid-stroke-palm / no-palm acceptance holds
- [ ] Screenshot each state to fastlane/.../phoneScreenshots/ (feeds SCOUT-03)
