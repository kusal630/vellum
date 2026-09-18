# ✒️ Vellum

**Private, fully offline handwriting notes for Android — with a software palm-rejection pipeline at its core.**

![GitHub Sponsors](https://img.shields.io/github/sponsors/kusal630?label=Sponsor&color=181717)
![Liberapay](https://img.shields.io/liberapay/recipients/kusal630?label=Liberapay&color=24292e)

Write naturally with a stylus or your finger. Raw touch input is analyzed in real time to reliably ignore palm rests, so only your pen or writing finger leaves ink — no proprietary or cloud service required. Everything stays on your device: notes, ink, images, transcripts.

> ⬇️ **Download:** grab the latest APK from [**Releases**](https://github.com/kusal630/vellum/releases) (`app-release.apk`, release-signed, ~90MB — includes the on-device speech model).

> ☕ Enjoying Vellum? [**Support development**](https://buymeacoffee.com/kusal630) — every coffee keeps it offline, private, and free.

## ✨ Features

| Area | What you get |
|---|---|
| ✍️ Handwriting | Low-latency ink (predicted-tip ghost), pen + highlighter, 6 pen styles, one-tap **Smooth** cleanup, RDP-thinned stroke storage, ink replay with fade-in, zoom writing aid, insert-space reflow, circle-to-select |
| 🌴 Palm rejection | Passive-stylus + finger engine: write with your palm resting — second-touch handoff, palm-sized lock rescue, writing-hand posture (right/left/two-handed edge bias), Low/Med/High sensitivity presets, saturated-pressure palm confirmation, stylus-hover gating, two-finger pan that works with a palm down, per-device calibration + Labs screen. Acceptance-tested: palm-first, mid-stroke palm, no-palm, and continued writing all keep inking |
| 🧰 Toolbar | Floating pills up top (navigate/undo, tools, page actions), tap the active pen for colors & thickness, left color rail with quick dots + full palette, customizable tool row (More → Customize toolbar) — never under the palm |
| 🔤 Text & images | Text boxes (insert + edit), photo insertion, move / resize / duplicate |
| 🔷 Shapes | Line, arrow, rect, circle, ellipse, triangle, star, hexagon — select, move, resize |
| 📄 Pages | Multi-page notebooks, page rail, long-press **duplicate / delete**, templates per notebook + per page |
| 📚 Paper | 11 templates (ruled, grid, dotted, graph, Cornell, music, math…), 10 premium covers incl. Aurum Gold |
| 🕘 History | Page snapshots on close + on demand (deduped, newest 20 kept), restore with pre-restore safety snapshot, per-page history from the page rail |
| 🔍 Organize | Noteshelf home (sidebar + categories, Starred/Unfiled/Trash/Archived), tags with filter chips, full-text search over titles/typed text/transcripts/summaries/recognized handwriting, Quick Note, Recent / A–Z sort |
| 💾 Backup | One-tap local ZIP export (database + images + PDF pages) to your own folder — no cloud. Passphrase-encrypted backups (AES-256-GCM) + in-app restore with safety copy; the passphrase is never stored |
| 🔄 Sync | Device sync via your Syncthing folder: versioned snapshots (plain/encrypted) with manifests, explicit newest-wins import with safety copy — no accounts, no servers |
| 📥 PDF | Import & annotate any PDF offline; export preserves italic/underline/alignment; reader themes + auto-trim coming |
| 📖 Read mode | Read imported books full-screen (Original/Sepia/Night), freehand highlighter in 4 colors, tap-erase, every highlight saved per page + searchable review list that jumps back to the exact page |
| 🎙️ Classroom | Optional on-device recording with live Vosk transcription + summary, audio-linked ink replay, saved with the note |
| 🎨 Canvas | Infinite canvas, pinch-to-zoom + pan, spring-animated Fit to page, toolbar auto-hide while writing, undo/redo (incl. two-finger double-tap), viewport-culled display list (off-screen ink skipped, zero-allocation draw path, surgical partial invalidation while writing), no auto-scroll (viewport moves only by pan/zoom/scroll bar), light & dark themes |
| ✍️ Convert | Select ink → **Convert** to an editable text box (single undo step; on-device $1-style print recognition, A–Z/0–9, "?" marks low confidence) |
| 🧪 Diagnostics | Input Labs screen for calibrating palm rejection to your hardware |

## 🔒 Privacy

- **100% offline** — Room database on-device; transcription runs on-device (bundled Vosk model). Nothing ever leaves the phone.
- **No internet permission**, no accounts, no tracking, no ads, no analytics.
- Only **one optional permission**: `RECORD_AUDIO`, asked only when you start a classroom recording. The app works fully without it.
- `allowBackup` is **disabled** — notes are never uploaded to cloud backups.
- Hardened storage: import size caps (25MB images / 50-page PDFs), path-traversal-safe media resolution, sampled bitmap decoding.

## 🚀 Quick start

1. Download the APK from [Releases](https://github.com/kusal630/vellum/releases) and install it.
2. Tap **+** to create a notebook (Normal or Classroom), pick a cover + paper template.
3. Write. Rest your palm — rejection is on by default. Open **Labs** to calibrate.
4. Long-press pages in the rail to duplicate/delete; use Select → **Smooth** to clean up ink.

## 🛠️ Building

Requirements: JDK 17+, Android SDK (compileSdk 35, minSdk 26, targetSdk 35). The wrapper fetches Gradle 8.13 automatically.

```sh
# Debug APK (signed, installable)
./gradlew :app:assembleDebug

# Release APK (unsigned; F-Droid signs its own builds)
./gradlew :app:assembleRelease

# Unit tests (327 passing)
./gradlew :app:testDebugUnitTest
```

Release builds are reproducible: two clean builds from the same commit produce byte-for-byte identical APKs.

### Speech model for Classroom Notes

Classroom Notes uses the small English Vosk model, downloaded once at build time (~40MB) and bundled into the APK — fully offline, no runtime downloads:

```sh
./gradlew :app:downloadVoskModel
```

The model is not committed to the repository. Builds without it still succeed; the app then disables Classroom Notes with an explanatory message.

## 🗺️ Roadmap

## 🆕 What's New in v1.3.1 (Sentinel Bugfix Wave)

- **SENT-C1 Pointer Leak Fix**: cleared stale `pointerStates` entries on `UP/CANCEL` via `retainAll(activeIds)` to prevent velocity spikes and false palm classification on reused pointer IDs.
- **SENT-C2 RELAXED Band-Max Fix**: resolved palm classification band-max by mode (`RELAXED` → `relaxedPalmMm`, `STRICT` → `fingerMax`) so pressurized contacts don't slip through.
- **SENT-C3 NaN Division Guard**: guarded velocity, path, size, and growth divisors with `EPSILON (0.0001f)` to eliminate NaN scores and CPU spin.
- **SENT-M1 Speed Routing**: sourced pipeline speed from `pointerStates` instead of hardcoded `0f` in fallback rejection.
- **SENT-M2 Tool-Type Holdoff**: qualified writing holdoff lift strictly by tool type (only lifting for the specific writing tool, preventing resting palms).
- **SENT-M3 Time Initialization**: initialized `lastMoveTimeNanos` to `-1L` and used explicit try-finally for touch session cleanup.

- Auto shape recognition + handwriting alignment
- Tags + full-text search (Room FTS)
- Versioned ZIP backup + scheduled local auto-backup
- Home-screen widgets + app shortcuts
- Voice canvas commands, spotlight focus mode
- Audio-note sync replay

## ☕ Support

Vellum is free, offline, and GPL-3.0 — no ads, no tracking, no paywalls. If it earns its keep in your pocket, [**buy me a coffee**](https://buymeacoffee.com/kusal630) to fund devices for palm-rejection calibration, F-Droid/Play fees, and late-night ink-smoothing sessions.

## 📄 License

Copyright (C) 2026 codeRed — GPL-3.0-or-later. See [LICENSE](LICENSE).
