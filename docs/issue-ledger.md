# Vellum Issue Ledger
Updated: 2026-09-10 (Scout landed, Sentinel/Muse pending)

## SCOUT-01 CRITICAL — keystore + weak password committed
- Source: Scout | File: vellum-release.keystore, gradle.properties (VELLUM_*=<redacted weak password> tracked)
- Status: VERIFIED FIXED 2026-09-10 (opencode spark, my grep/ls-files check clean)
- Action: new keystore offline, git rm keystore, secrets to env/CI, gitignore, rotate if uploaded to Play

## SCOUT-02 MAJOR — F-Droid metadata stale
- Source: Scout | metadata/com.vellum.notes.yml still 1.2.0/2, app is 1.3.0/3; docs/f-droid-submission.md YOUR-USER placeholders
- Status: VERIFIED FIXED 2026-09-10 (1.3.0/3 bumped, kusal630/vellum identity, no placeholders, merged to main)

## SCOUT-03 MAJOR — no screenshots / changelogs (store blocker)
- Source: Scout | fastlane/.../phoneScreenshots/ missing, only changelogs/1.0.0.txt
- Status: QUEUED (task 3, needs device/emulator captures)

## SCOUT-04 MAJOR — Play needs AAB, Data Safety, privacy policy, graphics
- Source: Scout | APK-only, no bundleRelease, no privacy URL, no feature graphic
- Status: QUEUED (task 4)

## SCOUT-05 IMPORTANT — FGS mic + audio permission disclosure
- Source: Scout | FOREGROUND_SERVICE_MICROPHONE, RECORD_AUDIO need rationale + Play disclosure
- Status: FIXED 2026-09-10 (rationale dialogs + strings + disclosure doc merged, gate green)

## SCOUT-06 OPTIONAL — 89MB APK (Vosk 40MB), ProGuard header comment stale
- Source: Scout | size friction, proguard-rules.pro comment says isMinifyEnabled=false
- Status: QUEUED (task 6, low priority)

## SENTINEL — landed 22:53, verdict NOT READY (1 critical DUPLICATE, 4 major NEW)
## SENT-01 CRITICAL DUPLICATE of SCOUT-01 — keystore in git
- Status: DUPLICATE, already IN PROGRESS (opencode proc_99e3f77f)

## SENT-02 MAJOR NEW — InkCanvasView leak, no onDetachedFromWindow
- Evidence: InkCanvasView.kt:115 pdfBackground, :191 imageBitmaps, :84 listener
- Fix: override onDetachedFromWindow, listener=null, recycle bitmaps sync, engine.reset()
- Status: VERIFIED FIXED 2026-09-10 (diff at :598-610 with guards, :app:testDebugUnitTest green)

## SENT-03 MAJOR NEW — engine state bleeds across pages
- Evidence: InkCanvasView.kt:87-110 setters never call engine.reset(); PalmRejectionEngine.kt:44 reset() exists, no caller on content change
- Fix: engine.reset() + clear strokeBuilder/gesture on page switch / setPage()
- Status: VERIFIED FIXED 2026-09-10 (resetInputStateForContentSwitch in both setters, tests green, in main)

## SENT-04 MAJOR NEW — AudioCaptureService START_STICKY null-intent idle
- Evidence: AudioCaptureService.kt onStartCommand returns START_STICKY
- Fix: return START_NOT_STICKY
- Status: VERIFIED FIXED 2026-09-10 (single-line change, both returns NOT_STICKY, merged to main)

## SENT-05 MAJOR NEW — ProGuard broad keeps defeat R8
- Evidence: app/proguard-rules.pro keeps androidx.compose.**, ui.**, datastore.**
- Fix: drop broad keeps, keep entry points/@Keep only, verify usage.txt
- Status: VERIFIED FIXED 2026-09-10 (blanket keeps gone, Room/input/Vosk/serialization keeps intact, merged to main)

## SENT-06 MINOR DUPLICATE of SCOUT stale comment — proguard header
- Status: DUPLICATE, fold into SCOUT-06

## SENT-07 MINOR NEW — debug overlay double-draw + String.format per frame
- Evidence: InkCanvasView.kt:1850-1874
- Status: VERIFIED FIXED 2026-09-10 (single-circle + StringBuilders, my clean-env merge-gate green)

## SENT-08 MINOR NEW — EditorViewModel nested launch redundant
- Evidence: EditorViewModel.kt:46-68
- Status: VERIFIED FIXED 2026-09-10 (single launch + collectLatest, tests green, merged to main)

## SENT-09 MINOR — POINTER_UP one-frame retention, acceptable, document it
- Status: WON'T FIX / document

## MUSE — landed 22:53, design pass NOT complete (3 P0)
## MUSE-P0-1 CRITICAL NEW — no palm/writing-state feedback
- Spec: status chip top-center (Pen ready / Palm rejected / Pan), 120ms fade, rejected ring 300ms, liveRegion polite
- Status: QUEUED (implement first, small diff on engine signals)

## MUSE-P0-2 CRITICAL NEW — toolbar scroll hides tools
- Spec: fixed 2-row bar, 8 tools, overflow More, selected primaryContainer + label, 150ms animate
- Status: QUEUED (second)

## MUSE-P0-3 HIGH NEW — palm rest zone invisible
- Spec: dashed 96x48 handle bottom-right, first-run coachmark, persist palmZone
- Status: QUEUED (third)

## MUSE-P1-4 HIGH — pickers unfriendly (hex names, O/S/F letters, duplicate ColorRail)
- Fix: remove ColorRail, named swatches + check icon, 5 width previews with mm, OFF/STEADY/FLOW chips
- Status: QUEUED

## MUSE-P1-5 HIGH — settings engineer dump (~25 raw params)
- Fix: 4 sections progressive disclosure, presets Low/Med/High, Try-it strip
- Status: FIXED 2026-09-10 (merged, merge-gate green; needs my on-device UI pass later)

## MUSE-P1-6 MED-HIGH — no latency/sync reassurance
- Status: QUEUED
## MUSE-P2-7 MEDIUM — home density/dialogs, veil 900→400ms
- Status: VERIFIED FIXED 2026-09-10 (delay(400), non-blocking preserved, tests green, merged)
## MUSE-P2-8 LOW-MED — bundle Fraunces/Inter fonts
- Status: VERIFIED FIXED 2026-09-10 (5 real TTFs, Type.kt wired, tests green, merged)

## JOB SCHEDULER (speed mode, 2026-09-10) — isolated worktrees, merge gate at end
- Job A MAIN proc_271eca2c: SENT-02 canvas leak + SCOUT-01 signing regression (debug build broken by env-var throw) | files: InkCanvasView.kt, app/build.gradle.kts
- Job B /tmp/vellum-job2 proc_00dfcabd: SENT-04 START_NOT_STICKY | file: AudioCaptureService.kt
- Job C /tmp/vellum-job3 proc_c22c7b55: SENT-05 narrow ProGuard + header | file: proguard-rules.pro
- Job D /tmp/vellum-job4 proc_024bbdac: SCOUT-02 F-Droid bump + placeholders | files: metadata yml, f-droid doc
- Conflict matrix: no shared files across jobs. Merge: copy fixed files into main, run assembleDebug + unit tests, commit, push.
- README BMC link: done by Conductor (docs), in main workdir uncommitted.
- Wave 1 MERGED+VERIFIED 2026-09-10: SENT-04, SENT-05, SCOUT-02 (files copied to main, diffs self-checked).
- Wave 2 DISPATCHED: B2 /tmp/vellum-job2 SENT-08 EditorViewModel | C2 DONE/MERGED SCOUT-04docs | D2 /tmp/vellum-job4 MUSE-P2-7 veil 900->400ms.
- Wave 2b: C3 /tmp/vellum-job3 MUSE-P2-8 bundle Fraunces+Inter fonts (res/font + Type.kt only).
- Wave 2c: D3 /tmp/vellum-job4 MUSE-P1-5 settings IA rework (SettingsContent only) — B2/D2 merged.
- Wave 2d: C4 /tmp/vellum-job3 store-graphics prep pack (icon audit + graphic specs + screenshot shot-list, docs only).
- Main: A3 DONE (signing release-only gate + SENT-07 overlay, worker-verified clean-env) -> MY merge-gate build next -> canvas UI mega-pack (P0-1/P0-2/P0-3 + P1-4/P1-6).
- Wave 3 MERGED 2026-09-10: D3 settings IA (MainActivity), B3 mic-pack (manifest+rationale+strings+mic-disclosure doc; sticky line re-applied post-merge), C4 graphics prep (2 docs).
- Wave 4 DISPATCHED: A4 canvas UI mega-pack (main) + E1 emulator smoke (job2).
- A4 BROKE BUILD (my gate caught it) -> A5 repair VERIFIED (my gate green, components present).
- A5 DEVIATION: ColorRail kept, spec said remove -> A6 VERIFIED (removed, audit honest: ring 600→300 tuning + coachmark gap open, icon-labels deferred as redesign).
- A8b PARTIAL (my pixels): coachmark + handle FIXED and visible; toolbar icons STILL absent (zoomed crop = zero pixels, not tint) -> A9 root-cause pack (drawables? alpha? composition gate?).
- A9 NO-OP (died on /tmp screenshot reads; zero edits) -> shots copied to docs/device-shots/, A9b next with repo-local refs only. LESSON: never mention /tmp in prompts at all.
- MERGE GATE 2026-09-10: my clean-env :app:assembleDebug + :app:testDebugUnitTest GREEN (18s, 44 tasks).
- E1 EMULATOR SMOKE PASS (Conductor, tablet_api35 headless): install Success, launch OK, zero FATAL, v1.3.0 home renders (drawer + 2 notebooks). Emulator left running daemonized for A4 verify pass. Note: notebook covers show no titles (possible truncation, watch in UI pass).
- Still queued: SCOUT-03 full screenshots (after UI settles), on-device UI pass, version bump + release APK + push.
## PH-01 MAJOR NEW — cold-start palm-first-down promotes to WRITING (investigator 2026-09-12)
- Evidence: PalmClassifier.kt:287-314 classifySingle cold start -> classifyWithSettings pure thresholds; small-ellipse palm-first-down under writingMax => WRITING 0.9, immediate ink.
- Fix: hold cold-start single contact as CANDIDATE until velocity/size/pressure confirms; regression tests palm-first / mid-stroke palm / no-palm / continued writing.
- Status: IN PROGRESS (opencode pack V1 proc_599521c88edf)

## PH-02 MAJOR NEW — UP-batch stroke tail dropped (investigator 2026-09-12)
- Evidence: InkCanvasView finalizeActiveStroke ~:1137, history fed :1066-1077 on MOVE only, onUp timestamp 0.
- Fix: feed UP batch history into active stroke before finalize.
- Status: IN PROGRESS (opencode pack V1 proc_599521c88edf)

## PH-03 MAJOR NEW — viewport/edge context uninitialized on common paths (investigator 2026-09-12)
- Evidence: RestingHandTracker.kt:162-163 falls back to displayMaxPx when viewport 0; setViewportSize only from syncPalmZoneRect() InkCanvasView.kt:705-710.
- Fix: setViewportSize from onSizeChanged/layout paths too.
- Status: IN PROGRESS (repair proc_0f4168068e46; Sentinel dirty-diff verdict 2026-09-12: KEEP all hunks, 13 failures transient code-before-tests, rerun GREEN, zero pre-existing)

## PH-04 MAJOR QUEUED — lock-drop → gesture-steal kills in-progress strokes (investigator 2026-09-12, V1b next)
- Evidence: PalmRejectionEngine.kt:329-370 any second non-palm contact resets lock; InkCanvasView.kt:636-639 routes to handleNavigation -> finalizeActiveStroke :1558 commits truncated stroke + pans mid-writing; selectGesturePointers :420-433 includes WRITING contacts.
- Fix: evicted writer must not become gesture finger; require gesture confirmation before killing active stroke.
- Status: QUEUED (pack V1b after V1 lands — same files, sequential)

## PH-05 MAJOR QUEUED — palm-first/pen handoff + CANDIDATE buffering drop real writers (investigator 2026-09-12, V1b)
- Evidence: handoff ratio/finger-band gates PalmRejectionEngine.kt:346-368; CANDIDATE promotion needs MOVE + velocity gate :231-233, slow writers/taps demoted RESTING RestingHandTracker.kt:434-437; raw-event undo/scroll/zone pre-emption InkCanvasView.kt:568-610 swallows gestures with no engine reset.
- Fix (V1b with PH-04): relax handoff for confirmed writers, promote slow/tap writers, reset engine state after raw-consumed gestures.
- Status: QUEUED (pack V1b after V1 lands)

## FEATURE GAPS (offline parity backlog, investigator 2026-09-12 — queued after stability+ship)
- Critical: active-stylus stack (pressure/tilt/hover/eraser-end), auto shape recognition, zoom-writing window, full lasso cut/copy/paste + rotation, page reorder/insert-anywhere/move-between-notebooks, offline handwriting-indexed search, audio-ink sync replay, scheduled auto-backup, stylus-only mode.
- Important: layers, real PDF-text layer ops, pen presets, ruler/fill/dash styles, LaTeX/math, image crop/background, SVG+print export, widgets/share-sheet, tag management, nested notebooks, spread/split-view.
- Status: QUEUED (scope after V1/V1b + release; no assumptions — each needs its own pack + tests)

## MUSE-V2 UI PACK QUEUED (design spec 2026-09-12, all in Compose, low-risk)
- D1: remove TOOLBAR PROBE debug text (EditorScreen.kt:1967) — trivial, first.
- D2: toolbar active underline contrast on dark (onPrimaryContainer).
- D3: gate Compose PalmZoneHandle on MANUAL mode (no phantom handle in AUTO).
- D4: top scrim on shelf card covers (star visibility); home grid minSize 140dp = card minWidth.
- D5: close X on phone nav-drawer sidebar.
- D6 PHASE 2: page-rail thumbnail raster preview; handedness-biased default palm-zone position.
- Keep untouched: InkCanvasView pipeline, VellumTheme, PenPickersPanel, WritingStatusChip, ClassroomSidebar, veil, engine integration, sync colors, rail scrollbar.
- Status: QUEUED pack V2 (dispatch after V1 lands; combine with P0-1/P0-2/P0-3 + toolbar-icon root cause in one worker brief).

## MUSE-R1 REVENUE PACK QUEUED (design spec 2026-09-12: Classroom/PDF/Gesture packs + offline entitlements)
- Packs: Classroom (audio-sync playback, auto-backup, chapters tab + export), PDF (layered export + text layer, page manager), Gesture (bookmarks rail, gesture mapping).
- Entitlements: DataStore JSON {classroomPack, pdfPack, gesturePack}; unlock via Play Billing v7 one-time IAP OR Ed25519-signed license file (offline ethos, no backend); locked icons at 38% alpha -> Unlock dialog.
- Entry points: toolbar overflow (Export/Page Manager/Bookmarks), page-rail Manage Pages, classroom sidebar Chapters+Export, Settings -> Packs cards. All Compose, no new deps.
- Status: QUEUED pack R1 (after stability + V2 ship; needs pricing decision first).

## DONATE PACK QUEUED (Scout concrete edits 2026-09-12): funding.json + F-Droid metadata donation fields + README badges + in-app Support ACTION_VIEW button (rememberLauncherForActivityResult pattern, already used in HomeScreen) reusing README line-96 sentence in both surfaces. Opencode pack after V2. Play listing untouched (owner discrepancy open).
- OWNER DEPLOY DECISION 2026-09-12: Vellum ships F-Droid FIRST, Play later. Consequences: F-Droid metadata + donation fields + reproducible-build check move up; R1 entitlements must lead with license-file unlock (Play Billing v7 is unusable on F-Droid — gate IAP code to Play flavor only).
## POLICY (user): grouped fixes per opencode worker (multi-bug packs, not singles); zero idle.

## SENTINEL V1c QUEUED (relay 2026-09-12, all NEW — dispatch after V1 lands, same files sequential)
- SENT-C1 CRITICAL: pointerStates leak -> velocity spike on reused IDs (PalmRejectionEngine manageWritingLock tail + UP/CANCEL ~315-340/390-410). Fix: retainAll(activeIds); no stale IDs survive.
- SENT-C2 CRITICAL: RELAXED bandMax wrong — pressure/fallback hardcode fingerMax not relaxedPalmMm (PalmClassifier classifyValid ~275-295). Fix: bandMax by mode; 18mm pressurized in RELAXED must be PALM.
- SENT-C3 CRITICAL: div-by-zero -> NaN in writeScoreFor/restScoreFor (RestingHandTracker ~336-357). Fix: guard all four divisors (vel/path/size/growth), coerce safe.
- SENT-M1: processWithoutPalmRejection speed hardcoded 0f — source from pointerStates.
- SENT-M2: holdoff lifted globally when fingerWriting on incl. stylus — qualify by tool type.
- SENT-M3: lastMoveTimeNanos uninit + isNew try-finally (RestingHandTracker :85/:121).
- SENT-M4 canvas: pureAppend O(n) structural compare (InkCanvasView :94-153) -> size+lastId; bitmap post-recycle race; StrokeSmoother.kt:105 lastEmitted!! crash guard + :72 NaN float equality.
- SENT-M5: pointerStates HashMap thread-safety (engine mutation vs debug overlay -> CME risk) — ConcurrentHashMap or thread-confinement assert.
- VULN-SWEEP 2026-09-12 VERIFIED SAFE (Conductor direct check, no bugs): zip-slip guarded both ways (BackupManager :163/:273-274); ImageStore traversal neutralized (substringAfterLast :70/:79); SyncCrypto solid (SecureRandom salt+IV, PBKDF2-200k, GCM-128, passphrase zeroed); manifest minimal (only launcher exported, FileProvider/service false, PendingIntents IMMUTABLE, no INTERNET). Open: 25MB image-size cap enforcement point not yet located — confirm in V1c pack, not a finding.
- Status: QUEUED pack V1c (after V1; V1b PH-04/05 may merge into same worker if files overlap cleanly).
- V-SEC QUEUED (vuln research 2026-09-12; zip-slip/crypto/exported already VERIFIED SAFE above — not repeated): PdfImporter resolveFile traversal + oversized/malformed PDF tests (PdfImporter.kt, MAX_PAGES=50 present); gradle dep scan (Vosk 0.3.75, Room 2.8.4, AGP/Kotlin) via dependencyCheck/osv; 25MB image-cap enforcement point; file_paths.xml + grantUriPermissions check. Pack after V2.
- SWITCHBOARD: verified working 2026-09-10 (smoke SWITCHBOARD_SMOKE_OK, ~2min/query). Role in pipeline: relay/aggregator — progress posts, output triage, recon missions, merge-gate verification. First mission: Sellable deep recon (below).
- NEXT PROJECT (after Vellum DONE): ~/Documents/projects_opencode/razorpay_ai_buildaton_real_solution = "Sellable", governed AI revenue agent for Razorpay merchants (cart recovery, failed-payment retry, upsell, AI-buyer protocol, ledger, holdout; TEST-mode prod system). Goal: fix/add/compete-check/out-build, UI + website, revenue-max deployment, deploy public + Google-visible, payments to https://buymeacoffee.com/kusal630, all bots.
