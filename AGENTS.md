# AGENTS.md

Guidance for Claude Code, Cursor, Codex, Fable, Grok.

## What this is

Centre-staff Android client for the DIPI registrar desk. Package: `org.dhamma.dipi.staff`.

**Shipped:** Vertical 2 desk on `feat/vertical-1`, **1.15.0** (`versionCode` 25). Live default is `https://dipi.vridhamma.org`. Backend PHP is **immutable** — do not add `/staff/*` or change `dipi-web`.

**Read first:** this file, then `docs/LIVE-DESK-HAR.md`.  
`docs/DIPI-STAFF-IMPLEMENTATION-PROMPT-GROK-4.6.md` still wins on product rules (no client ACL, no `Approved`, no attendance write) but is **wrong** on transport: there is no `/staff` JSON layer on the live host.

Server reference (read-only): `/Users/wizops/DIPI/dipi-web` module `dh_manageapp`.

## Current assumptions (2026-08-15)

1. **Live protocol is the browser desk**, not Services `POST /api/user/login` and not `/staff/*`. Mock `/staff/*` exists only behind `-Pdipi.useMock=true`.
2. **Login:** wipe cookies first. Prefer `GET /user/login` (200). Fallback `GET /` or `GET /centre` (often **403** with the form in Retrofit `errorBody()` — use `Response.html()`). POST to the parsed form action (`user_login` or `user_login_block`).
3. **Centre:** Drupal `dh_user_center`. `GET /centre` → `/centre/{cid}`. Do not hardcode Dhamma Giri. Mock-only: `UserCentreMap` (`sudha.user` → Dhamma Sudha).
4. **Courses:** parse upcoming links from `GET /centre/{cid}` HTML.
5. **Worklist:** `GET /search-course/{cid}/{courseId}?s=&t=&g=&d=a` and parse `var dataset`. Do **not** POST `/search-app`.
6. **Status write:** existing `GET /change-status/{id}?s=&l=0&c=`. Never send `Approved`.
7. **HTML parse is required** for login, dashboard, and `dataset`. Never persist or log NPI (`aadhar`, `passport`, `voterid`, `pancard`, `ae_*`). Display-only amendment (owner decision 2026-08-16): ID docs and health disclosures MAY be shown on-screen for desk verification — in-memory `SensitiveInfo` only, no Room/DataStore/DTO fields.
8. **Session keep-alive:** every 20 minutes, `GET /services/session/token` (CSRF) + `GET /centre` (SESS cookie). 403 → Sign in.
9. **Remember me** stores username/password in EncryptedSharedPreferences. Logout keeps them. **Erase all local data** (Settings) wipes cookies, remember-me, Room, outbox, photo edits.
10. **Photo upload is not on the live desk.** Mock only.
11. **Launcher:** lotus adaptive icon (sage badge + safe-zone flower). Pixel C caches icons — re-add the shortcut after an icon change.
12. **Allocation sync amendment (owner decision 2026-08-16):** the app MAY replicate the desk's own per-applicant allocation update — `POST /app-update-attended/{id}` with the dialog's fields (`s,r,g,l,v,c,cf,chow,chai,back,comment,a`, no CSRF form token) — as a bulk, user-initiated room sync; this narrows hard rule 5, and the client still never sends a status, never `Approved`, never NPI.
13. **Board sheet exports (12) are served by the live desk:** streamed PDF `GET /course-pdf-m|f/{cid}/{courseId}`; streamed Excel `GET /laundry-list|valuable-list/{cid}/{courseId}`; print HTML `GET /day0-list|teacher-list|manager-list|student-chit|checking-slip|seating/{cid}/{courseId}`; Day 0 summary = the `#day-summary` block of `GET /zero-day/{cid}/{courseId}`; course report = its own Drupal form POST (CSV). Sheets are display-only — in-memory / `cacheDir/sheets` only, wiped on logout/session-expiry/erase-all. **NEVER send an `r` query param on sheet GETs** — its mere presence triggers server-side bulk seat auto-allocation. App edit page `GET /app/{id}/edit` is rendered display-only.

## Hard rules

1. No access control in the app. Send the request; render the server response verbatim.
2. No status engine in Kotlin. Display and send strings only.
3. Never send status `Approved`.
4. Status write = existing `/change-status/{id}?s=&l=&c=` with `l=0`.
5. No attendance writes in v1.
6. Never use APP API / `get-app-detail`. Parse desk HTML only as above; never store NPI.
7. No NPI columns in Room or logs (`ae_*`, Aadhaar, PAN, passport, voter id).
8. Server URL is `BuildConfig.BASE_URL` (`https://dipi.vridhamma.org`). See Current assumptions for the live paths.
9. Design file `docs/DIPI Staff.dc.html` wins every visual argument.
10. Do not commit `local.properties`, keystores, or real student data.
11. **SemVer on every shippable change.** Bump `versionName` + `versionCode` in `app/build.gradle.kts` before assembling:
    - **MAJOR** (`x.0.0`) — new vertical, breaking API/UX, or a drop-in incompatible rewrite.
    - **MINOR** (`1.x.0`) — user-visible feature within the current vertical.
    - **PATCH** (`1.0.x`) — bugfix, visual polish, test-only behaviour that still goes to the tablet.
    Always increment `versionCode` by 1. Do not leave two installs with the same `versionName`.
12. **Install on the desk tablet after every MAJOR (and after MINOR if the registrar will tap it).** See below.

## Desk tablet (Wi-Fi ADB)

- Device: **Pixel C** (`ryu` / `dragon`), serial `5C01001294`, Android 8.1.
- LAN: `10.0.0.144:5555` (SSID `searching`). Re-discover with `adb shell ip -f inet addr show wlan0` if DHCP moves it.
- Reconnect (USB once, then Wi-Fi):

```bash
export ANDROID_HOME=/Users/wizops/Android/Sdk
export PATH="$ANDROID_HOME/platform-tools:$PATH"
adb -s 5C01001294 tcpip 5555
adb connect 10.0.0.144:5555
adb -s 10.0.0.144:5555 install -r -d app/build/outputs/apk/debug/app-debug.apk
adb -s 10.0.0.144:5555 shell am start -n org.dhamma.dipi.staff/.MainActivity
```

Prefer the Wi-Fi serial (`10.0.0.144:5555`) for install/launch so the cable can come off.

## Commands

```bash
./gradlew :app:testDebugUnitTest
./gradlew :core:model:test :core:network:testDebugUnitTest
./gradlew :app:assembleDebug
# fixtures only:
./gradlew :app:assembleDebug -Pdipi.useMock=true
```

Kotlin JVM target 17. The Mac that last built this tree used JDK 20 (no JDK 17 toolchain installed). `sdk.dir` in `local.properties`.
