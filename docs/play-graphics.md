# Play Store Graphics — Specs and Gap List

App: Vellum (`com.vellum.notes`, label `Vellum`).
Source repo: `https://github.com/kusal630/vellum`.
Scope: store-listing graphics only. No code changes in this pack.

Related runbook: `docs/screenshot-shotlist.md` (emulator capture order).
F-Droid fastlane input: `fastlane/metadata/android/en-US/`.
F-Droid submission reference: `docs/f-droid-submission.md`.

## 1) Launcher icon audit (res/)

Audit date: 2026-09-10. Command used:

```bash
ls -R app/src/main/res
```

### Every `mipmap-*` dir found

Only one `mipmap-*` directory exists in `app/src/main/res`:

| Directory | File | Status |
|---|---|---|
| `app/src/main/res/mipmap-anydpi-v26/` | `ic_launcher.xml` | Present — adaptive-icon XML (background + foreground + monochrome) |

No other `mipmap-*` directories exist. Explicitly absent:

- `mipmap-mdpi/` — absent
- `mipmap-hdpi/` — absent
- `mipmap-xhdpi/` — absent
- `mipmap-xxhdpi/` — absent
- `mipmap-xxxhdpi/` — absent

### Supporting drawable / value files found

| Path | Status | Notes |
|---|---|---|
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | Present | Vector 108dp, gold V-nib (`#E8B84B`) on transparent, nib slit + dot |
| `app/src/main/res/drawable/ic_launcher_monochrome.xml` | Present | White single-glyph vector for themed icons |
| `app/src/main/res/values/colors.xml` | Present | `ic_launcher_background` = `#171B24`, `ic_launcher_foreground` = `#FFFFFF` |
| `app/src/main/res/drawable/ic_mic.xml` | Present | Unrelated mic glyph, not a launcher icon |

### Manifest references

`app/src/main/AndroidManifest.xml` sets:

```xml
android:icon="@mipmap/ic_launcher"
android:roundIcon="@mipmap/ic_launcher"
```

Both resolve today only to `mipmap-anydpi-v26/ic_launcher.xml`.

### Gap analysis

1. **Pre-API-26 fallback missing.** The adaptive XML in `mipmap-anydpi-v26/` only
   applies on API 26+. `minSdk = 26` in `app/build.gradle.kts`, so at runtime
   this is acceptable, but there are no legacy PNGs (`ic_launcher.png`,
   `ic_launcher_round.png`) at 48 / 72 / 96 / 144 / 192 px for
   mdpi / hdpi / xhdpi / xxhdpi / xxxhdpi. Any tool that expects density PNGs
   (older launchers, some lint / asset-studio checks, F-Droid icon scanners)
   reports them as missing.
2. **No 512x512 store icon file in the repo.** Google Play does not reuse the
   APK `mipmap/` icon for the listing; it requires a separately uploaded
   512x512 high-res icon (see section 2). No such file exists under
   `fastlane/` or `metadata/` today.
3. **No `featureGraphic` file in the repo.** Play listing requires a 1024x500
   feature graphic. `fastlane/metadata/android/en-US/` currently contains only
   `title.txt`, `short_description.txt`, `full_description.txt`,
   `changelogs/1.0.0.txt` — no `images/` directory at all.
4. **No phone screenshots in the repo.** `fastlane/metadata/android/en-US/`
   has no `images/phoneScreenshots/` directory. `docs/f-droid-submission.md`
   already flags screenshots as "not yet provided".
5. **F-Droid icon path is covered at runtime, weak for tooling.** F-Droid
   extracts the launcher icon from the APK, so the adaptive icon is enough to
   install and display. The gap is the same as (1): no density PNG fallbacks
   and no `fastlane/.../images/icon.png` override, so `fdroid lint` /
   `fdroiddata` reviewers may ask for a high-res icon source. Provide the
   512x512 PNG from section 2 as the canonical source.

Decision for this pack: docs only. Do not generate PNGs yet; produce them
from the `#171B24` background + `#E8B84B` V-nib foreground vectors when the
asset pass runs, then verify with the checks in section 3.

## 2) Exact specs for missing assets

All dimensions are in pixels. Produce sRGB exports. Keep text inside the
center safe area and test at small sizes before uploading.

### 2.1 App icon (Play high-res icon) — missing

- Size: **512 x 512 px**, 32-bit PNG, sRGB.
- Shape: full-bleed square, no rounded corners, no transparency padding added
  by hand (Play applies masking). Keep the key art inside the 66dp-diameter
  adaptive safe circle so it survives circular / squircle masks.
- Art: deep ink background `#171B24` edge-to-edge; gold V-nib `#E8B84B` with
  nib slit `#171B24` and dot `#B98A1E`, matching
  `drawable/ic_launcher_foreground.xml`. Match the monochrome glyph silhouette
  so the themed icon stays recognizable.
- File size: under 1024 KB.
- Do not include text, badges, or version numbers in the icon.
- Delivery: upload in Play Console under Main store listing > App icon. Keep
  a repo copy at `fastlane/metadata/android/en-US/images/icon.png` (512x512)
  so F-Droid / review tooling has a canonical source.
- Acceptance: legible at 48px preview; background exactly `#171B24`; no
  JPEG artifacts; PNG only.

### 2.2 Feature graphic — missing

- Size: **1024 x 500 px**, PNG or JPEG (JPEG preferred, quality 90+, no alpha).
- Safe area: keep title art and any tagline inside the center ~840 x 300 px;
  edges get cropped on some surfaces.
- Content: `Vellum` wordmark + tagline `Offline handwriting notes` on the
  `#171B24` ink background with the gold V-nib mark left or center. No device
  screenshots collaged at an angle, no small body copy, no URLs.
- Text must be rasterized vectors at export (no missing-font substitution).
- Delivery: upload in Play Console under Main store listing > Feature graphic.
  Keep a repo copy at
  `fastlane/metadata/android/en-US/images/featureGraphic.png` (1024x500) for
  F-Droid / fastlane supply.
- Acceptance: exactly 1024x500; under 1024 KB; no transparency; readable on a
  phone-width preview.

### 2.3 Phone screenshots — missing

- Minimum for Play: **2 phone screenshots**. This pack targets **9**
  (see shot list) so the listing and the F-Droid submission are covered in
  one emulator pass.
- Size: **1080 x 1920 px portrait** (9:16). Play accepts 320–3840 px per side
  with max 8 MB each; 1080x1920 PNG satisfies both Play and F-Droid guidance
  in `docs/f-droid-submission.md`.
- Format: PNG (preferred) or JPEG, sRGB, no status-bar carrier text or
  personal data. Use English (US) locale, light theme except where a shot
  explicitly calls for dark theme.
- Delivery paths (both, same files):
  - Play Console upload + repo copy at
    `fastlane/metadata/android/en-US/images/phoneScreenshots/01_home.png`
    through `09_labs.png` (names per `docs/screenshot-shotlist.md`).
- Acceptance: exactly 1080x1920; English strings; no placeholder owner text,
  no debug toasts, no notification shade pulled down, no low-battery icon.

### Which screens to shoot (maps to shotlist numbers)

| Shot | Screen / route | Why it is on the listing |
|---|---|---|
| 01 | Home notebook list (`ui.home.HomeScreen`) | First impression, library |
| 02 | Editor ink canvas, pen (`ui.editor.EditorScreen` + `InkCanvasView`) | Core pen + infinite canvas |
| 03 | Editor highlighter | Second core tool, translucent strokes |
| 04 | Palm-zone / resting-hand (`SettingsContent` palm-zone + canvas overlay) | Differentiator: palm rejection |
| 05 | PDF annotate (`ui.reader.PdfReaderScreen`) | PDF import + ink over page |
| 06 | Read mode (`ui.reader.ReadModeScreen`) | Reading / highlights path |
| 07 | Classroom Notes transcription (`speech/` + note transcript) | Offline lecture capture |
| 08 | Settings palm-rejection (`MainActivity.SettingsScreen`) | Tunability, offline trust |
| 09 | Labs diagnostics (`ui.diagnostics.DiagnosticsScreen`, titled Labs) | Input transparency, hardware proof |

Full setup steps and must-be-visible checklists live in
`docs/screenshot-shotlist.md`. Shoot in numeric order in one emulator pass.

## 3) Verification checklist (docs-only pack)

- [ ] This file exists at `docs/play-graphics.md`.
- [ ] `docs/screenshot-shotlist.md` exists with shots 01–09 in order.
- [ ] Neither doc contains placeholder tokens.
- [ ] Section 1 above lists every `mipmap-*` dir found
      (today: only `mipmap-anydpi-v26/`).
- [ ] No code under `app/` was modified; no push performed.
- [ ] When assets are produced later: confirm 512x512 icon, 1024x500 feature
      graphic, and 1080x1920 screenshots 01–09 exist at the
      `fastlane/metadata/android/en-US/images/` paths above.
