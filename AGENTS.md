# AGENTS.md

Guidance for Claude Code, Cursor, Codex, Fable, Grok.

## What this is

Centre-staff Android client for DIPI registrar desk. Package: `org.dhamma.dipi.staff`.

**Governing spec:** `docs/DIPI-STAFF-IMPLEMENTATION-PROMPT-GROK-4.6.md`  
It **supersedes** `docs/00-architecture.md` and `docs/DIPI-STAFF-ANDROID-GROK-PROMPT.md` on conflicts.

Server reference (read-only): `/Users/wizops/DIPI/dipi-web` module `dh_manageapp`.

## Hard rules

1. No access control in the app. Send the request; render the server response verbatim.
2. No status engine in Kotlin. Display and send strings only.
3. Never send status `Approved`.
4. Status write = existing `/change-status/{id}?s=&l=&c=` with `l=0`.
5. No attendance writes in v1.
6. Never parse HTML. Never use APP API / `get-app-detail`.
7. No NPI columns in Room or logs (`ae_*`, Aadhaar, PAN, passport, voter id).
8. Server URL is `BuildConfig.BASE_URL` (`https://dipi.vridhamma.org`). Live protocol is the **browser desk** (see `docs/LIVE-DESK-HAR.md`), not Services `/api/user/login` and not `/staff/*`. Login = drop cookies, then `GET /user/login` (200) or `GET /` (403) for `form_build_id`, then `POST` to the form action (`user_login` or `user_login_block`). Dashboard = `GET /centre` → `/centre/{cid}`. Worklist = `GET /search-course/{cid}/{courseId}?s=&t=&g=&d=a` (`var dataset`). Status write = `GET /change-status/{id}?s=&l=0&c=`. Mock only with `-Pdipi.useMock=true`.
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
./gradlew :app:assembleDebug
```

JDK 17+, `sdk.dir` in `local.properties`.
