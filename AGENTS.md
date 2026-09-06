# AGENTS.md

Guidance for Claude Code, Cursor, Codex, Fable, Grok.

## What this is

Centre-staff Android client for the DIPI registrar desk. Package: `org.dhamma.dipi.staff`.

**WhatsApp automation 1.43.1** (`versionCode` 80): Android-only, opt-in per server/centre. One separately distributed `DIPI-WA1` provisioning code replaces the pilot's two fields; no secret is bundled in the APK. Calling supports filtered recipient selection, active managed-letter preview, explicit duplicate-number review, a package-restricted accessibility sender and encrypted paused recovery. Pixel C self-tests verify exact composer text and a new outgoing message; a WhatsApp version change requires another test. Saved-name-only chats stop for manual handling. Automation ships off. No Drupal edits or student test messages. See `docs/plans/2026-09-06-whatsapp-automation.md`. Includes the approved 1.42.1 landscape seating-print fixes already on the tablet.

**Prior shipped baseline:** Vertical 2 desk + **Course ops (assistant-teacher mode)** + **Sheets v5** + **Reach v5 UI**, **1.42.1** (`versionCode` 70) on `main` — native seating print now requests A4 landscape end-to-end, fills the printable page, trims trailing empty rows/columns, anchors the Dhamma seat below row 1, and places occupied chowky/chair seats in a vertical side rail; blocks show seat, name, room, age, and old/new. It still prints from the in-memory roll and never calls `/seating` or sends `?r=`. Prior 1.42.0 — the centre dashboard keeps the four newest older courses, filling its existing two-column wide layout as a 2×2 grid while narrow layouts remain a vertical list; centre-page discovery and server order are unchanged. Prior 1.41.0 — cached roll fallback preserves an unknown timestamp; desk error snackbars read `LocalDipi.snackError`; and only Advanced Search and Add Application hand off exact URLs to the system browser. Those handoffs use separate browser session state and never transfer app cookies. Prior ui-gaps pass (owner Q1-Q3 rulings 2026-09-05): **phone sheet viewer** (1.39.0: `SheetViewerPane` mounts at the `DipiAppUi` root, all eight Board exports in `hubSheetLabel`, full v5 chrome with <600dp band scroll + WebView pinch-zoom, hardening unchanged; phone Seating plan = native hall + 5i print), **per-student backrest** (1.40.0: `BACKREST_GLYPH` `⌐` prefix on teacher list / student card / hall / rail / 5i print with used-only legend), **`SeatKind` single source** (1.40.1: `SeatGrid` reads the enum, ordering pins unchanged) and **Bulk Mail retired** (1.40.2: last desk-site chip gone, `MORE ON THE DESK SITE` shelf renders only when a chip exists). Prior 1.38.0 — native seating **PRINT** (5i: `seatingPlanPrintHtml` from the in-memory roll, one gender per A4 page, never `GET /seating`) and the Course report **empty-range** state (`CourseReportCsvParser` drops blank-name zero rows so a no-course range shows the `EmptyRange` guidance, not a ghost "1 course · 0 students" row). Prior 1.37.2 — Course ops v6 chrome plus owner PATCH: rail **0 Day Board**, Male/Female course PDFs removed, Board **3×3** (Course summary in-grid; Valuable list off the Board), chowky/chair **vertical column** (CW-A1 nearest the Dhamma seat), student chit print **12-up** (5e), checking slip **2-up** (5g), Course ops destination **Teacher list**. Board seating is the Course ops hall (native 5h), not `GET /seating` HTML. **Course report left the Board** (v5 T4) for its native centre-dashboard surface (v5 T3). Course ops: teacher-at-the-bottom seating (r2), chowky/chair rail, worklist + application buffering on entry with a pull progress strip.

The `feat/desk-gap` branch is **closed out and deleted** (close-out merge, 2026-09-01) — its history stays reachable from `main`. Applicant desk history (`/app-courses`, `/app-activity`, `/app-clarifications` + clarification PDF) and the `HtmlForms`/`HtmlTables` scrapers shipped in T6. Manage Courses, Daily Activity, SMS Report and Letters were retired by owner decision 2026-08-30 (`DeskTiles.kt`) — do not re-propose them. The one live feature never ported is **server-side Advanced Search** (`DeskSearchFields`); `POST /search-app` is on the do-not-assume list, so it needs HAR verification and owner sign-off first. Live default is `https://dipi.vridhamma.org`. Backend PHP is **immutable** — do not add `/staff/*` or change `dipi-web`.

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
10. **Photo upload is not on the live desk.** Mock only.
11. **Launcher:** lotus adaptive icon (sage badge + safe-zone flower). Pixel C caches icons — re-add the shortcut after an icon change.
12. **Allocation sync amendment (owner decision 2026-08-16):** the app MAY replicate the desk's own per-applicant allocation update — `POST /app-update-attended/{id}` with the dialog's fields (`s,r,g,l,v,c,cf,chow,chai,back,comment,a`, no CSRF form token) — as a bulk, user-initiated room sync; this narrows hard rule 5, and the client still never sends a status, never `Approved`, never NPI.
13. **Board sheet exports (3×3 cells) are served by the live desk:** streamed Excel `GET /laundry-list|valuable-list/{cid}/{courseId}` (Valuable stays in the enum / phone hub; it is not a Board cell since 1.37.2); print HTML `GET /day0-list|teacher-list|manager-list|student-chit|checking-slip|seating/{cid}/{courseId}`; Day 0 summary = the `#day-summary` block of `GET /zero-day/{cid}/{courseId}`, parsed natively by `DaySummaryParser` since v5 T2; course report = its own Drupal form POST (CSV), reached from the **centre dashboard** since v5 T3, never from the Board; Day-11 course summary = streamed PDF `GET /report-day11/{cid}/{courseId}` as the **Course summary** cell in the 3×3, not a fourth-line chip. **Male/Female course PDFs (`course-pdf-m|f`) left the Board in 1.37.1** — the app does not fetch them. Sheets are display-only — in-memory / `cacheDir/sheets` only, wiped on logout/session-expiry/erase-all. **NEVER send an `r` query param on sheet GETs** — its mere presence triggers server-side bulk seat auto-allocation. Since v5 T1 the only query names a sheet GET can carry are `conf` and `seating`, through the `SheetSort` allowlist; `SheetRouteSafetyTest` fails the build otherwise. App edit page `GET /app/{id}/edit` is rendered display-only.
14. **Rooms come back from the web desk (1.18.0):** opening a course also GETs `/zero-day/{cid}/{courseId}` and merges `#table-attending` via `AttendedTableParser` — `a_id` plus room/seat/laundry/valuable/group cells only, never names or the hidden comment column. Same no-`r`-param rule as every other sheet GET.
15. **Centre room config is read-only:** `GET /centre/{cid}/acco-handler` (the DataTables source behind `/centre/{cid}/edit`), parsed by `AccoHandlerParser`. Never POST it.
16. **Course ops (2026-09-02):** a device mode, not a login role — `tablet_mode` in DataStore flips the app to two read-only teacher destinations (Teacher list, Seating plan) plus the student card, fed by exactly two GETs: `/teacher-list/{cid}/{courseId}` (UNTHEMED fragment; mutates server data on GET — fetch once per entry, never poll; Comments column never parsed) and `/application-view/{id}` (themed page; allowlist parsing only — Personal/Course History/Health; all other sections carry NPI and never reach a regex). A device PIN (own encrypted store, survives logout, dies on Erase-all) gates the Settings door in course ops. The running course is parsed out of the course NAME (`CourseDates.kt`). Course-scoped roll+answers persist encrypted in `dipi_course_ops` per the 2026-09-02 owner amendment — wiped on course change, logout and Erase-all. Rulings in `docs/DECISIONS.md` § Course ops; design in `docs/DESIGN.md` § Course ops.
17. **Older courses** are the extra options scraped out of the centre page (`CentrePageParser.olderCourseOptions`), not a separate endpoint. **Confirmation prefixes** (`nf`/`of`/`nm`/`om`, sevak `sm`/`sf`) drive the gender + new/old filters via `ConfPrefix`; anything unparseable stays visible under "all" and hides under a specific filter.
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
    **Owner amendment (2026-09-06, WhatsApp automation):** an explicitly opted-in centre/device may list its active managed letters, retrieve and preview a selected server-rendered applicant letter in memory, and hand it to the installed WhatsApp app. Provision shared encryption material separately into protected local storage; never bundle it in the APK. Do not reproduce merge fields or invoke Drupal delivery/status endpoints. Unattended device sending and release are gated on a successful Pixel C pilot with controlled recipients. Existing letter rendering may initialise missing applicant login/auth-code fields on the server. Implementation status and pilot evidence: `docs/plans/2026-09-06-whatsapp-automation.md`.

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
