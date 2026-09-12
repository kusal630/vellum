# Vellum Privacy Policy

Effective date: 2026-09-10 · App version: 1.3.0 · Package: `com.vellum.notes`

Vellum is a private, fully offline handwriting notes app. This policy is
intentionally short because there is very little to disclose: **your data
never leaves your device.**

## 1. Fully offline — no data collection

- Vellum **collects no personal data, usage data, or analytics of any kind**.
- There are **no accounts, no servers, no tracking SDKs, no ads, and no
  analytics**.
- The app declares **no internet permission** (`INTERNET` is absent from the
  manifest), so it is technically incapable of transmitting anything off-device.
- Notes, ink strokes, images, transcripts, and summaries are stored only in the
  on-device Room database and app-private files.

## 2. Microphone — on-device classroom transcription only

- The **only** optional permission is `RECORD_AUDIO` (microphone). It is
  **requested only when you start a Classroom recording**; the app works fully
  without granting it.
- When granted, microphone audio is used **solely for classroom recording with
  live transcription** via the bundled on-device Vosk speech model.
- Audio is **processed on-device only** — recording, transcription, and the
  saved transcript never leave your phone. There is no streaming, no upload,
  and no cloud speech service.
- The persistent recording notification (Android 13+ `POST_NOTIFICATIONS`) and
  the `microphone` foreground-service type (`FOREGROUND_SERVICE` /
  `FOREGROUND_SERVICE_MICROPHONE`, `AudioCaptureService`) exist only to keep
  the classroom recording running visibly while you take notes.
- Denying the microphone permission only disables Classroom recording. All
  handwriting, reading, backup, and sync features keep working.

## 3. Backups and sync stay in your hands

- `android:allowBackup` is **`false`** — notes are never uploaded to cloud
  (Google) backups.
- Local ZIP export/backup (optionally passphrase-encrypted with AES-256-GCM;
  the passphrase is never stored) is written **only to a folder you choose**.
- Device sync works through **your own Syncthing folder** with versioned
  snapshots and explicit newest-wins import plus a safety copy — no Vellum
  servers or third parties are involved.

## 4. Data retention and deletion

- Your data is retained on your device until you delete it (per-note Trash /
  delete, or Clear data / uninstall, which removes everything).
- Because nothing is ever sent anywhere, there is nothing to request back or
  delete server-side.

## 5. Children

- Vellum collects no data from anyone, including children, and classroom
  recordings stay on the device that made them.

## 6. Changes and contact

- If this policy ever changes, the updated version will ship in the app
  repository (`docs/privacy.md`).
- Questions: contact **kusal630** via https://github.com/kusal630/vellum/issues.
