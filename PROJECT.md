# Project
Vellum — private, fully offline handwriting notes for Android with software palm-rejection pipeline. Write naturally with stylus or finger; raw touch input is analyzed in real time to ignore palm rests.

# Features (v1 — must build)
1. Low-latency ink with predicted-tip ghost, pen + highlighter, 6 pen styles, one-tap Smooth cleanup
2. Software palm rejection: passive-stylus + finger engine, second-touch handoff, writing-hand posture, sensitivity presets
3. Infinite canvas with pinch-to-zoom + pan, viewport-culled display list, no auto-scroll
4. Multi-page notebooks with templates, page rail, duplicate/delete, history snapshots
5. Text boxes, photo insertion, shapes, handwriting-to-text conversion
6. One-tap local ZIP export (database + images + PDF), passphrase-encrypted backups
7. Device sync via Syncthing, versioned snapshots
8. PDF import & annotate, export preserves layers
9. Read mode with freehand highlighter, search review list
10. Classroom notes: on-device recording + Vosk transcription + summary + audio-linked ink replay

# Done criteria (loop stops when ALL true)
- All v1 features implemented and tested
- Build passes, no critical bugs, all 443 unit tests green
- Palm rejection works reliably with stylus and finger input
- Canvas background is clean (no visual noise around paper)
- Pen color/thickness controls accessible above the canvas

# Loop instruction
Each run: read RESEARCH/*.md, pick highest-priority work, build it.
