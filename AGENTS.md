# AGENTS.md

Guidance for Claude Code, Cursor, Codex, Fable, Grok.

## What this is

Centre-staff Android client for the DIPI registrar desk. Package: `org.dhamma.dipi.staff`.

**Current release 2.3.4** (`versionCode` 107). Release-by-release history from 2.0.0 is `CHANGELOG.md`; the per-release design ledger is `docs/DESIGN.md`; owner rulings, retirements and parked items are `docs/DECISIONS.md`. Nothing shippable lives outside `main`. Build-flag surfaces: photo review is off by default (`docs/PHOTO-BUILDS.md`); centre-specific WhatsApp automation is off by default and opt-in per server/centre (`docs/WHATSAPP.md`).

**Layout:** `:app` (repository, `DeskViewModel`, `DipiAppUi`), `:core:{model,network,database,datastore,ui,audit}`, `:feature:{auth,course,desk,applicants,photos,summary,settings}`. Tests live in `:app`, `:core:model`, `:core:network`, `:core:datastore`, `:core:audit` only — feature modules have no test source set, so their Compose screens are covered by Robolectric tests in `:app`.

**Read first:** this file, then `docs/LIVE-DESK.md` (transport, page inventory, server facts) and `docs/DESIGN.md` (design authority).  
This file is the single source of truth for product rules — the 2026-08-13 implementation prompt that used to carry them is absorbed into the Hard rules below and deleted (its transport section was wrong anyway: there is no `/staff` JSON layer on the live host).

Server reference (read-only): `/Users/wizops/DIPI/dipi-web` module `dh_manageapp`.

## Current assumptions (2026-08-15, re-verified against the tree 2026-08-31)

1. **Live protocol is the browser desk**, not Services `POST /api/user/login` and not `/staff/*`. Mock `/staff/*` exists only behind `-Pdipi.useMock=true`.
2. **Login:** wipe cookies first. Prefer `GET /user/login` (200). Fallback `GET /` or `GET /centre` (often **403** with the form in Retrofit `errorBody()` — use `Response.html()`). POST to the parsed form action (`user_login` or `user_login_block`).
3. **Centre:** Drupal `dh_user_center`. `GET /centre` → `/centre/{cid}`. Do not hardcode Dhamma Giri. Mock-only: `UserCentreMap` (`sudha.user` → Dhamma Sudha).
4. **Courses:** parse upcoming links from `GET /centre/{cid}` HTML.
5. **Worklist:** `GET /search-course/{cid}/{courseId}?s=&t=&g=&d=a` and parse `var dataset`. Do **not** POST `/search-app`.
6. **Status write:** existing `GET /change-status/{id}?s=&l=0&c=`. Never send `Approved`.
7. **HTML parse is required** for login, dashboard, and `dataset`. Never persist or log NPI (`aadhar`, `passport`, `voterid`, `pancard`, `ae_*`). Display-only amendment (owner decision 2026-08-16): ID docs and health disclosures MAY be shown on-screen for desk verification — in-memory `SensitiveInfo` only, no Room/DataStore/DTO fields.
8. **Session keep-alive:** every 20 minutes, `GET /services/session/token` (CSRF) + `GET /centre` (SESS cookie). 403 → Sign in.
9. **Remember me** stores username/password in EncryptedSharedPreferences. Logout keeps them. **Erase all local data** (Settings) wipes cookies, remember-me, Room, outbox, photo edits.
10. **Photo write on the live desk is the full applicant edit form.** Owner amendment 2026-09-07: `GET /app/{id}/edit`, echo every current field, attach the corrected JPEG on the form's file input, `POST` the form action. Fail closed if tokens or the file field are missing. Never invent names, health, seats, status or attendance. Never send `Approved`. No `/app/{aid}/photo` and no live `/staff/*`. Mock `/staff/*` upload stays unused. Course ops cannot upload.
11. **Launcher:** lotus adaptive icon (sage badge + safe-zone flower). Pixel C caches icons — re-add the shortcut after an icon change.
12. **Allocation sync amendment (owner decision 2026-08-16):** the app MAY replicate the desk's own per-applicant allocation update — `POST /app-update-attended/{id}` with the dialog's fields (`s,r,g,l,v,c,cf,chow,chai,back,comment,a`, no CSRF form token) — as a bulk, user-initiated room sync; this narrows hard rule 5, and the client still never sends a status, never `Approved`, never NPI.
    **Checked-in / room-key amendment (owner 2026-09-16):** a parsed room allocation plus not Left means checked in. Format does not matter (`Mbk 51`, `Mbk-51`, `Mbk- 51`, `MBK51` are one room via `RoomAllocSync.roomKey` / `parseDeskRoom`). Do not invent attendance POSTs; server Attended after finalize stays the `finalized` / `section` / `acc` path. Dash-only `#table-attending` rows stay checked in. No allocation and not on the attending table is not checked in. Left never occupies a room.
13. **Board sheet exports (3×3 cells) are served by the live desk:** streamed Excel `GET /laundry-list|valuable-list/{cid}/{courseId}` (Valuable stays in the enum / phone hub; it is not a Board cell since 1.37.2); print HTML `GET /day0-list|teacher-list|manager-list|student-chit|checking-slip|seating/{cid}/{courseId}`; Day 0 summary = the `#day-summary` block of `GET /zero-day/{cid}/{courseId}`, parsed natively by `DaySummaryParser` since v5 T2; course report = its own Drupal form POST (CSV), reached from the **centre dashboard** since v5 T3, never from the Board; Day-11 course summary = streamed PDF `GET /report-day11/{cid}/{courseId}` as the **Course summary** cell in the 3×3, not a fourth-line chip. **Male/Female course PDFs (`course-pdf-m|f`) left the Board in 1.37.1** — the app does not fetch them. Sheets are display-only — in-memory / `cacheDir/sheets` only, wiped on logout/session-expiry/erase-all. **NEVER send an `r` query param on sheet GETs** — its mere presence triggers server-side bulk seat auto-allocation. Since v5 T1 the only query names a sheet GET can carry are `conf` and `seating`, through the `SheetSort` allowlist; `SheetRouteSafetyTest` fails the build otherwise. Application editing uses the browser handoff described above; never mount its editable form in the sheet viewer.
14. **Finalized-course amendment (owner 2026-09-06):** use existing worklist `finalized`, `section`, `acc` for historical Attended allocations, with read-only Check-in and Room Chart. Left students never occupy rooms. Never infer finalization from dates. Skip Zero Day when finalized.
    **Rooms come back from the web desk (1.18.0):** opening a course also GETs `/zero-day/{cid}/{courseId}` and merges `#table-attending` via `AttendedTableParser` — `a_id` plus room/seat/laundry/valuable/group cells only, never names or the hidden comment column. Same no-`r`-param rule as every other sheet GET.
15. **Centre room config is read-only:** `GET /centre/{cid}/acco-handler` (the DataTables source behind `/centre/{cid}/edit`), parsed by `AccoHandlerParser`. Never POST it. Room Chart inventory is those `AccoRoom` rows (section + number). Hall Settings on `GET /centre/{cid}/edit` (`cs_hall_combined`, `cs_seat_naming_conv`, `seatcfg_{male|female}_{spr,sprc,dir,pos,empty,empty_cho}`) are also GET-only. The visual editor (live `seat-visual.js`) fills those fields from `cs_seat_config`; the INI itself is assembled on POST and is not in the live GET HTML. Centre Settings shows the Main Plan; seats-per-row / chowky width seed Room Chart wrap and seating when a block has no local SAVE ROOM LAYOUT. Hall depth is still not on the edit page, so SAVE HALL LAYOUT stays the local override for omitted fields. The HAR has no hall-name / RESULT-hall / room-chart column field. Never POST the edit form. No `?r=`.
16. **Course ops (2026-09-02):** a device mode, not a login role — `tablet_mode` in DataStore flips the app to two read-only teacher destinations (Teacher list, Seating plan) plus the student card, fed by `/teacher-list/{cid}/{courseId}` (UNTHEMED fragment; mutates server data on GET — fetch once per entry, never poll; Comments column never parsed) and `/application-view/{id}` (themed page; allowlist parsing only — Personal/Course History/Health; all other sections carry NPI and never reach a regex). When Course History has no `Teacher(s)`, opening a card may `GET /app/{id}/edit` and keep only `ac_first_teacher_str` / `ac_last_teacher_str` — GET only, never POST. A device PIN (own encrypted store, survives logout, dies on Erase-all) gates the Settings door in course ops. The running course is parsed out of the course NAME (`CourseDates.kt`). Course-scoped roll+answers persist encrypted in `dipi_course_ops` per the 2026-09-02 owner amendment — wiped on course change, logout and Erase-all. Rulings in `docs/DECISIONS.md` § Course ops; design in `docs/DESIGN.md` § Course ops.
17. **Older courses** are the extra options scraped out of the centre page (`CentrePageParser.olderCourseOptions`), not a separate endpoint. **Confirmation prefixes** (`nf`/`of`/`nm`/`om`, sevak `sm`/`sf`) drive the gender + new/old filters via `ConfPrefix`; `sm`/`sf` are Old (owner 2026-09-16). Anything unparseable stays visible under "all" and hides under a specific filter. Teacher-list groups and hall seats do not use this rule.
18. **Status vocabulary (T3):** the worklist page's own `edit-app-status` select, with roster fallback. Display and send strings only; never invent statuses in Kotlin.

## Hard rules

1. No access control in the app. Send the request; render the server response verbatim.
2. No status engine in Kotlin. Display and send strings only.
3. Never send status `Approved`.
4. Status write = existing `/change-status/{id}?s=&l=&c=` with `l=0`.
5. No attendance writes in v1.
6. Never use APP API / `get-app-detail`. Parse desk HTML only as above; never store NPI.
7. No NPI columns in Room or logs (`ae_*`, Aadhaar, PAN, passport, voter id).
8. Server URL is `BuildConfig.BASE_URL` (`https://dipi.vridhamma.org`). See Current assumptions for the live paths.
9. Design file `docs/design/DIPI-Staff.dc.html` wins every visual argument; use `docs/DESIGN.md` for measurements and the shipped-delta ledger.
10. Do not commit `local.properties`, keystores, or real student data.
11. **SemVer on every shippable change.** Bump `versionName` + `versionCode` in `app/build.gradle.kts` before assembling:
    - **MAJOR** (`x.0.0`) — new vertical, breaking API/UX, or a drop-in incompatible rewrite.
    - **MINOR** (`1.x.0`) — user-visible feature within the current vertical.
    - **PATCH** (`1.0.x`) — bugfix, visual polish, test-only behaviour that still goes to the tablet.
    Always increment `versionCode` by 1. Do not leave two installs with the same `versionName`.
12. **Install on the desk tablet after every MAJOR (and after MINOR if the registrar will tap it).** See below.
    When cutting a GitHub release, attach the APK twice: versioned (`dipi-staff-<version>.apk`) **and** as the
    stable name `dipi-staff.apk`, then mark the release latest — that keeps the permanent link
    `https://github.com/kapaggar/dipi-app/releases/latest/download/dipi-staff.apk` pointing at the newest build.
13. **Server messages verbatim.** Error snackbars show the server's text unmodified (e.g. `Please Edit application and choose Area teacher before approving!`).
14. **Bridge rule:** letters, waitlist, LC review, SMS/WhatsApp dispatch are black boxes behind the desk's `_change_status`. Never reimplement them, never preview letter bodies.
    **Owner amendment (2026-09-06, WhatsApp automation):** an explicitly opted-in centre/device may list its active managed letters, retrieve and preview a selected server-rendered applicant letter in memory, and hand it to the installed WhatsApp app. Provision shared encryption material separately into protected local storage; never bundle it in the APK. Do not reproduce merge fields or invoke Drupal delivery/status endpoints. Unattended device sending and release are gated on a successful Pixel C pilot with controlled recipients. Existing letter rendering may initialise missing applicant login/auth-code fields on the server. Implementation status and pilot evidence: `docs/WHATSAPP.md`.

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
# full green suite (JVM modules use :test, Android modules :testDebugUnitTest)
./gradlew :core:model:test :core:audit:test \
          :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest \
          :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease          # slim arm64 desk build, debug-signed
# fixtures only:
./gradlew :app:assembleDebug -Pdipi.useMock=true
```

**Never run `./gradlew test` (or `:app:test`).** It adds `:app:testReleaseUnitTest`, where all ~76 Robolectric Compose tests fail with "Unable to resolve activity … ComponentActivity" — `androidx.ui.test.manifest` is `debugImplementation`, so the release variant has no test activity. The failure is the build config, not the code.

Kotlin JVM target 17, Gradle 8.9, compileSdk/targetSdk 35, minSdk 26. The Mac that builds this tree has only JDK 20 (`/Library/Java/JavaVirtualMachines/jdk-20.jdk`) — no JDK 17 toolchain. `sdk.dir` in `local.properties`; `dipi.baseUrl` / `dipi.useMock` may be set there or passed as `-P` flags.
