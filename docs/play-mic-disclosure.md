# Play Console — Foreground-Service Microphone Disclosure (SCOUT-05)

Feature: **Classroom Notes** — classroom recording with live on-device transcription.
Service: `com.vellum.notes.speech.AudioCaptureService`
(`android:foregroundServiceType="microphone"`)

## 1. Manifest (audited, minimal)

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-feature android:name="android.hardware.microphone" android:required="false" />

<service
    android:name=".speech.AudioCaptureService"
    android:exported="false"
    android:foregroundServiceType="microphone" />
```

Why each entry exists:

| Declaration | Why it is required | Minimal? |
|---|---|---|
| `RECORD_AUDIO` | Captures class audio via `AudioRecord(MIC)` only while the user records; fed to the on-device Vosk recognizer. Requested lazily when record is tapped, never at launch. | Yes — no recording without it; service defensively stops if revoked. |
| `FOREGROUND_SERVICE` | Base permission for any FGS on API 28+. | Yes. |
| `FOREGROUND_SERVICE_MICROPHONE` | API 30+ sub-type permission for `microphone` FGS. Runtime `startForeground(id, notification, FOREGROUND_SERVICE_TYPE_MICROPHONE)` on API 34+ (UPSIDE_DOWN_CAKE). | Yes. |
| `POST_NOTIFICATIONS` | Shows the persistent “Classroom Notes recording / Transcribing on-device…” notification with Stop action on API 33+. Optional — recording starts even if denied. | Yes. |
| `hardware.microphone required=false` | Documents mic as optional so Play does not filter out mic-less devices; app works fully for handwriting without it. | Yes — prevents over-filtering. |

No location, camera, background-mic, or `RECORD_AUDIO` maxSdk games. No other FGS types.

## 2. Runtime behavior (what the reviewer sees)

1. User opens a **classroom notebook** → transcript sidebar visible, mic button in top bar.
2. Tap **Start recording** → in-app rationale dialog first (before any system prompt):
   - Title: “Allow microphone for classroom recording?”
   - Text: “Vellum uses the microphone only when you tap record in a classroom notebook, to transcribe the lesson live with on-device speech recognition. Audio is processed on this device and never uploaded. You can deny and keep writing notes normally.”
   - Buttons: Continue / Not now. (strings: `mic_rationale_*` in `strings.xml`)
3. On Continue → system `RECORD_AUDIO` prompt. On deny → inline notice from `mic_permission_denied`, recording stays off, handwriting unaffected.
4. If mic granted and device is API 33+ without notification permission → second in-app rationale:
   - Title: “Show recording notification?” / text explains the persistent recording indicator + Stop action (`notification_rationale_*`). Continue fires the system `POST_NOTIFICATIONS` prompt; dismiss still starts recording (notification optional).
5. Recording starts via `startForegroundService` → `startAsForeground()` posts channel `Classroom recording` (IMPORTANCE_LOW), ongoing notification with content intent + Stop action, then validates `RECORD_AUDIO` again and the Vosk model before opening `AudioRecord`.
6. While recording: red “Microphone active — transcription is on-device” row in sidebar, live partial + segments, persistent notification. Stop from sidebar or notification → `ACTION_STOP`, thread join ≤500 ms, `SpeechController.endRecording`.

Audio never leaves the device: `VoskSpeechToText` asset model, no network call in the capture path.

## 3. Exact Play Console disclosure text (copy-paste)

Use **App content → Foreground service permissions → Microphone**:

> **Foreground service type:** Microphone
>
> **Is the microphone foreground service the core purpose of the app?** No.
>
> **Use-case description (paste):**
> Vellum is a handwriting notes app. Its optional “Classroom Notes” feature lets a student tap Record in a classroom notebook to transcribe a lecture live. When started, the foreground service `AudioCaptureService` (type `microphone`) captures audio from the device microphone and runs it through an on-device (Vosk) speech recognizer; recognized text appears in the transcript sidebar and is saved into the note. The service runs only while the user is actively recording, shows a persistent “Classroom Notes recording — Transcribing on-device…” notification with a Stop action, and stops immediately when the user taps Stop, the model is missing, or the microphone permission is revoked. Audio is processed on-device and never uploaded. The app works fully without the microphone.
>
> **Justification for microphone access (paste):**
> Classroom recording with live on-device transcription is user-initiated (mic button), time-boxed to the recording session, and disclosed in-app before the system prompt plus via the ongoing notification. `RECORD_AUDIO` + `FOREGROUND_SERVICE_MICROPHONE` with `foregroundServiceType="microphone"` is the only API that keeps capture alive with user awareness while the screen is off or the user switches pages. No background recording: the service cannot start without an explicit tap, and `hardware.microphone required=false` marks it optional.

Data safety declaration must also tick **Microphone → recorded only during the user-initiated classroom session, processed on-device, not collected/shared**.

## 4. What screenshots / video to attach to the declaration

Record on a real API 34 device with the bundled Vosk model (`./gradlew downloadVoskModel` first):

1. `01-rationale-mic.png` — the “Allow microphone for classroom recording?” in-app dialog over the editor (prove pre-prompt disclosure).
2. `02-system-mic-prompt.png` — Android system RECORD_AUDIO prompt immediately after Continue.
3. `03-rationale-notification.png` — (API 33+) “Show recording notification?” dialog.
4. `04-recording-active.png` — editor during recording: red live dot + “Microphone active — transcription is on-device”, live transcript, persistent status-bar/notification-shade notification with Stop.
5. `05-notification-shade.png` — expanded notification showing “Classroom Notes recording / Transcribing on-device… / Stop”.
6. `video-classroom-recording.mp4` (30–60 s) — tap mic → rationale → system prompt → Allow → recording indicator + live words → Stop from notification → transcript persisted after stop. Narrate “audio stays on-device”.

Name files exactly as above when uploading to the Play review thread so the reviewer can map each to §2.

## 5. Reviewer test script

1. Install release, open a classroom notebook, tap mic → expect rationale dialog (no system prompt yet).
2. Continue → Allow mic → (API 33+) notification rationale → Allow/Deny → recording starts, notification appears.
3. Deny mic on a fresh install → inline “Microphone permission denied…” notice, no crash, handwriting works.
4. Revoke mic mid-recording (Settings) → service stops itself, notification removed.
5. Airplane mode → recording + transcription still work (proves on-device).

## 6. Files touched by SCOUT-05

- `app/src/main/AndroidManifest.xml` — added `hardware.microphone required=false`; verified FGS type + 4 permissions.
- `app/src/main/res/values/strings.xml` — `mic_rationale_*`, `notification_rationale_*`, `mic_permission_denied`, `recording_notification_*`, `recording_channel_name` (no hardcoded user-visible text in new UI).
- `app/src/main/java/com/vellum/notes/ui/editor/EditorScreen.kt` — `showMicRationale` / `showNotificationRationale` dialogs gate both `RequestPermission` launchers; no direct launch.
- `app/src/main/java/com/vellum/notes/speech/AudioCaptureService.kt` — notification title/text/Stop/channel from `strings.xml`.
