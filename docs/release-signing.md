# Release Signing

Release signing uses **environment variables only**. No keystore files,
passwords, aliases, or paths are committed to git or stored in
`gradle.properties`. `app/build.gradle.kts` reads signing config via
`System.getenv` and fails a `release` build with a clear error if any
required variable is missing.

Required variables:

- `VELLUM_STORE_FILE` — absolute path to the production `.keystore` / `.jks` file
- `VELLUM_STORE_PASSWORD` — keystore password
- `VELLUM_KEY_ALIAS` — key alias
- `VELLUM_KEY_PASSWORD` — key password

`*.keystore` and `*.jks` are ignored in `.gitignore`. Do not add
exceptions.

## Generate a new production keystore (offline)

Do this once on an offline / secure machine. Do not reuse the old
committed `vellum-release.keystore` (weak password, exposed in git
history) for production.

```bash
# 1. Generate (RSA 2048+, validity 25+ years for Play)
keytool -genkeypair \
  -keystore /secure/path/vellum-production.keystore \
  -alias vellum-production \
  -keyalg RSA -keysize 2048 -validity 9125

# 2. Verify
keytool -list -v -keystore /secure/path/vellum-production.keystore

# 3. Back up the keystore + passwords offline (encrypted USB / password
#    manager). Losing the upload key means a new app listing on Play.
```

Use strong random passwords, distinct for store and key:

```bash
openssl rand -base64 24
```

If the old keystore was ever used to sign an upload to Play, rotate /
follow Play key-reset procedures after switching.

## Local release build

```bash
export VELLUM_STORE_FILE=/secure/path/vellum-production.keystore
export VELLUM_STORE_PASSWORD='<store-password>'
export VELLUM_KEY_ALIAS='vellum-production'
export VELLUM_KEY_PASSWORD='<key-password>'

./gradlew :app:assembleRelease
# or
./gradlew :app:bundleRelease
```

Debug builds and Gradle sync do not require these variables. Only the
`release` build type requires them; missing variables throw:

> Release signing requires environment variables missing: ...

Do not put these values in `gradle.properties`, `local.properties`,
shell history, or docs. Unset when done (`unset VELLUM_STORE_PASSWORD
VELLUM_KEY_PASSWORD`).

## CI release build

Store the four values as secret env vars / protected CI variables, never
as plain text in the repo or job logs:

- `VELLUM_STORE_FILE` — either (a) path where a prior CI step writes the
  decoded keystore, or (b) absolute path on the runner.
- `VELLUM_STORE_PASSWORD`, `VELLUM_KEY_ALIAS`, `VELLUM_KEY_PASSWORD` —
  masked secret variables.

Example pattern (GitHub Actions style):

```yaml
- name: Write keystore
  run: echo "${{ secrets.VELLUM_KEYSTORE_BASE64 }}" | base64 -d > /tmp/vellum-production.keystore
- name: Build AAB
  env:
    VELLUM_STORE_FILE: /tmp/vellum-production.keystore
    VELLUM_STORE_PASSWORD: ${{ secrets.VELLUM_STORE_PASSWORD }}
    VELLUM_KEY_ALIAS: ${{ secrets.VELLUM_KEY_ALIAS }}
    VELLUM_KEY_PASSWORD: ${{ secrets.VELLUM_KEY_PASSWORD }}
  run: ./gradlew :app:bundleRelease
```

- Base64-encode the production keystore once offline
  (`base64 -w0 vellum-production.keystore`) and store only as a secret.
- Restrict secrets to protected branches / release jobs, enable masked
  logs, and delete the temporary keystore after the build.

## What not to do

- Never `git add *.keystore *.jks`.
- Never commit passwords in `gradle.properties` or elsewhere.
- Never print env secrets in logs (`set -x`, `printenv`).
- Never push without checking `git status --short` and
  `git ls-files | grep -E 'keystore|\.jks'`.
