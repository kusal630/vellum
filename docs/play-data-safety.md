# Play Data Safety — draft answers for Vellum 1.3.0 (`com.vellum.notes`)

> Draft to paste into Play Console → App content → Data safety and
> Foreground-service / sensitive-permission declarations. Vellum declares no
> internet permission and transmits nothing off-device.

## 1. Data collection and sharing: none

- **Does your app collect or share any of the required user data types? No.**
  - No personal info, no financial info, no health/fitness, no messages, no
    photos/videos (user-inserted page images stay on-device), no audio files
    uploaded, no files/docs transmitted, no calendar/contacts, no app
    activity, no web browsing, no app info/performance, no device IDs.
- **Data sharing: none.** No third parties, no SDKs, no servers.
- **Security practices questionnaire:** no data is collected or transmitted,
  so transport encryption / deletion-request options are not applicable. Data
  is deleted by the user on-device (per-note delete, Clear data, uninstall).
  `android:allowBackup="false"` excludes app data from cloud backups.

## 2. RECORD_AUDIO — optional, on-device only

- **Permission:** `android.permission.RECORD_AUDIO` (sensitive permission).
- **Optional:** requested in-app only when the user starts a Classroom
  recording; the app is fully functional without it.
- **Use:** classroom lecture recording with live transcription via the bundled
  on-device Vosk speech model. Audio is captured, transcribed, and stored
  **entirely on-device**; no streaming, upload, or cloud processing.
- **Play declaration guidance:** Audio → "recorded locally on the device only,
  never transmitted" → ephemeral / user-initiated; not collected or shared.

## 3. Foreground-service (FGS) microphone declaration

- **Permissions:** `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MICROPHONE`;
  service `.speech.AudioCaptureService` with
  `android:foregroundServiceType="microphone"` and a persistent recording
  notification (`POST_NOTIFICATIONS` on Android 13+).
- **Justification (paste into Play Console):**
  > "Vellum uses a microphone foreground service solely to capture the user's
  > classroom lecture audio while they take handwritten notes, and to produce
  > a live on-device transcript (bundled Vosk model) saved with the note. The
  > service starts only when the user taps Record, shows a persistent
  > notification, and stops when the user stops recording. Audio never leaves
  > the device; the app has no internet permission."
- **Demo-video notes for review:** start a Classroom notebook → tap Record →
  grant mic → show persistent notification → stop → transcript saved with the
  note; then show full app use after denying mic.

## 4. Other permissions (non-sensitive, for completeness)

- `POST_NOTIFICATIONS`: only for the recording notification on Android 13+.
- `FOREGROUND_SERVICE`: base permission for the recording service above.
- FileProvider / photo picker / scoped storage: user-chosen images and local
  ZIP backups stay on-device or in the user's own Syncthing folder.

Contact for policy questions: kusal630 via
https://github.com/kusal630/vellum/issues. See also `docs/privacy.md`.
