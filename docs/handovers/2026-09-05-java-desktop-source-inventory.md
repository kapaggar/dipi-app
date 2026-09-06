# Source inventory — DIPI Staff Android as of 1.42.0

Verified against the tree at `/Users/wizops/DIPI/dipi-app` on 2026-09-05.
This file is the **source of truth for what the tablet app already does**.
It is not a Java design and not an implementation plan. The planning session
that ports this product to a platform-independent Java desktop client must
re-verify any claim it intends to copy, then write a spec, a phase plan, and
an implement-phase handover. It must not write production code.

**Baseline:** `1.42.0` / `versionCode` 69 on `main` (`app/build.gradle.kts`).
Package `org.dhamma.dipi.staff`. Device of record: Pixel C, 2560×1800 at
density 2 → design window **1280×900 dp** (1 px in the design file = 1 dp).

**Stale documents in this repo (do not treat as current version):**
`README.md` still headers 1.4.1; `docs/DECISIONS.md` footer still says
1.30.5. Later rulings live in the same DECISIONS file and in the
`docs/DESIGN.md` shipped-delta ledger. **Tree + `AGENTS.md` + DESIGN
shipped-delta win.**

---

## What this product is

A centre-staff client for the live Drupal registrar desk
(`https://dipi.vridhamma.org`, module `dh_manageapp`). It is **not** a
WebView wrapper, not student-apply, not the APP `/api`, and not a
`/staff/*` JSON client on the live host. The live protocol is Drupal HTML
plus one embedded `var dataset` array. Backend PHP is immutable. Server
clone (read-only reference): `/Users/wizops/DIPI/dipi-web`.

One signed-in account → one centre (`dh_user_center`) → upcoming / older
courses → either the six-section registrar desk or Course ops (assistant-
teacher mode). Photo upload is mock-only.

---

## Module map (Android)

| Module | Owns |
|---|---|
| `:app` | Hilt app, `StaffRepository`, `DeskViewModel`, `DipiAppUi`, `DeskScreen` |
| `:core:model` | ids, status vocabulary, `ConfPrefix`, `CourseMatrix`, `SeatGrid`, `SheetExport`/`SheetSort`/`SheetPayload`, `RoomAllocSync`, `Backrest` |
| `:core:network` | Retrofit `StaffApi` + `DrupalAuthApi`, HTML/CSV parsers, `SheetTransport`, cookie jar, mock dispatcher |
| `:core:database` | Room + SQLCipher (one course cache + outbox) |
| `:core:datastore` | Encrypted prefs (cookie / CSRF / remember-me / course-ops PIN+roll) + DataStore (skin, lotus, `tabletMode`, room/hall layout) |
| `:core:ui` | `Industry` / `LocalDipi` tokens, fonts, shared chrome |
| `:core:audit` | Client rules that never block a request |
| `:feature:auth` | `LoginScreen` |
| `:feature:course` | Centre, hub, Centre ops, Rooms, Course report, Advanced Search, `DeskTiles` |
| `:feature:desk` | `DeskShell` / `DeskSection`, Board, sheet viewer, seating print |
| `:feature:teacher` | Teacher list, Seating plan, Student card (Course ops) |
| `:feature:applicants` | Today, Card, Audit, Calling, Zero Day |
| `:feature:photos` | Photo review (mock) |
| `:feature:summary` | Day 0 summary native surface |
| `:feature:settings` | Theme, offline, logout, erase-all |

Tests live only in `:app`, `:core:model`, `:core:network`, `:core:datastore`,
`:core:audit`. Feature modules have no test source set; Compose is covered
by Robolectric tests in `:app`. **Never `./gradlew test` at the root** —
that pulls `:app:testReleaseUnitTest` and every Compose test dies.

---

## Destinations the tablet actually paints

`DeskScreen` (`DeskViewModel.kt`): `Login`, `Centre`, `CourseHub`, `Today`,
`Card`, `Photos`, `Summary`, `Settings`, `DeskAction`, `ZeroDay`, `Audit`,
`Calling`, `Rooms`, `CentreOps`, `Search`, `TeacherRoll`, `SeatingPlan`,
`TeacherCard`, `CourseReport`.

Adaptive breakpoints (S4): `screenWidthDp ≥ 600` list-detail;
`≥ 1100` full desk shell. Phone sheet viewer mounts at the `DipiAppUi`
root (1.39.0), not inside the desk host.

### Vertical 1 — login to a card

1. Wipe cookies (`commit()`, not `apply()`).
2. `GET /user/login` (prefer 200). Fallback `GET /` or `GET /centre`
   (often 403 with the form in Retrofit `errorBody()` — `Response.html()`).
3. POST to the parsed form action (`user_login` or `user_login_block`).
4. Follow redirects `/centre` → `/centre/{cid}`. Centre is never hardcoded.
5. Parse upcoming + older course links from the centre page.
6. Worklist: `GET /search-course/{cid}/{courseId}?s=&t=&g=&d=a`, parse
   `var dataset`. Do **not** `POST /search-app`.
7. Public card. Status write: existing
   `GET /change-status/{id}?s=&l=0&c=`. Never send `Approved`.
8. Settings: remember me (EncryptedSharedPreferences; logout keeps them),
   simulate offline, erase all local data (cookies, remember-me, Room,
   outbox, photo edits, course-ops store).

Keep-alive every 20 minutes: `GET /services/session/token` + `GET /centre`.
403 → Sign in. Session is the full Drupal cookie jar (`SESS…`), not a
single header.

### Centre dashboard (1.42.0)

- Header `{centre} · {displayName}`.
- Upcoming pane: desk serves at most 4 upcoming (`upcoming_courses()`
  `limit 4`). Matrix cards: columns `NM OM M NF OF F`, fixed
  `cardRows` = Received / Confirmed+Expected / Cancelled / Total (+sevak).
  Zero renders as `·`, never `0`. Status set is parsed from the page, never
  hardcoded.
- Older courses: `OLDER_COURSE_LIMIT = 4` in `StaffRepository` (parser
  stays a faithful page reader). Wide layout: 2×2 grid; narrow: vertical
  list. Server order, no client sort.
- Four native tiles (`centreDeskTiles`): **Centre Settings**, **Course
  report**, **Advanced Search**, **App Settings**. No desk-site chip shelf
  unless a chip exists (Bulk Mail retired 1.40.2).
- Advanced Search and Add Application are the **only** system-browser
  handoffs (1.41.0). Exact URLs; **no app cookies transfer**. Separate
  browser session.
- Sync: two independent strips (offline / queued + RETRY). Retry always
  attempts the send; no client reachability gate.

### Vertical 2 — one course, six rail sections

`DeskSection`: **0 Day Board**, Applications, Audit, Calling, Check-in,
Rooms & seats. Rail 190 dp. Centre settings are **not** a desk section.

**0 Day Board 3×3** (1.37.2):

| | | |
|---|---|---|
| Day 0 list | Day 0 summary | Seating plan |
| Student chit | Checking slip | Laundry list |
| Teacher list | Manager list | Course summary |

- Course summary = streamed PDF `GET /report-day11/{cid}/{courseId}`.
- Course report left the Board (v5 T3) for the centre dashboard.
- Valuable list is off the Board; enum / phone hub / `GET /valuable-list`
  stay.
- Male/Female course PDFs (`course-pdf-m|f`) left in 1.37.1 — do not fetch.
- Board seating is the native Course ops hall (5h) + 5i print from the
  in-memory roll, **never** `GET /seating` as the primary surface.

**Applications:** worklist + card + applicant desk history
(`/app-courses`, `/app-activity`, `/app-clarifications` + clarification
PDF). Status vocabulary from the worklist `#edit-app-status` select, roster
fallback. Display and send strings only.

**Check-in / Rooms:** merge web-assigned rooms from `#table-attending` on
`GET /zero-day/{cid}/{courseId}` (`AttendedTableParser` — `a_id` plus
room/seat/laundry/valuable/group cells only; never names, never the hidden
comment). Centre room config is read-only `GET /centre/{cid}/acco-handler`.
Allocation write (owner 2026-08-16): `POST /app-update-attended/{id}` with
the dialog's own fields
(`s,r,g,l,v,c,cf,chow,chai,back,comment,a`) — no CSRF form token, no
status, no `Approved`, no NPI. `l`/`v` posted empty.

**Confirmation prefixes** `nf`/`of`/`nm`/`om` (sevak `sm`/`sf`) drive
gender + new/old filters via `ConfPrefix`. Unparseable stays visible under
"all" and hides under a specific filter.

### Course ops — device mode, not a login role

`tabletMode` in DataStore. Two read-only destinations (Teacher list,
Seating plan) plus the student card. Allowed GETs: worklist (owner
amendment — id mapping starves without it),
`GET /teacher-list/{cid}/{courseId}` (**mutates server data on GET** —
once per entry, never poll; Comments column never parsed),
`GET /application-view/{id}` (allowlist: header / Personal / Course
History / Health only). Device PIN (own encrypted store, survives logout,
dies on Erase-all) gates Settings. Running course parsed from the course
**name** (`CourseDates.kt`). Roll + answers persist encrypted in
`dipi_course_ops`, wiped on course change / logout / Erase-all.

Hall: teacher at the bottom (r2), letters as columns, numbers ascending
away from the Dhamma seat. Chowky/chair vertical rail, `CW-A1` nearest
the Dhamma seat. `SeatKind` is the single source (`SeatGrid` reads the
enum). Per-student backrest glyph `⌐` (`BACKREST_GLYPH`, prefix) on every
seat surface + 5i print (used-only legend).

### Sheets v5

`SheetExport` labels: Day 0 list, Day 0 summary, Student chit, Checking
slip, Teacher list, Manager list, Laundry list, Valuable list, Seating
plan, Course report, Course summary.

Delivery (`SheetRoutes` / `SheetTransport`):

| Kind | How |
|---|---|
| `Page` | `GET /{slug}/{cid}/{courseId}` HTML, in-memory, hardened viewer (JS off, no `CookieManager`) |
| `DaySummary` | `#day-summary` of `GET /zero-day/...`, parsed natively (`DaySummaryParser`) |
| `Document` | streamed XLS/PDF to `cacheDir/sheets`, wiped on logout / expiry / erase-all |
| `ReportForm` | scrape `GET /centre/{cid}/course-report`, POST the desk's form, parse CSV (`CourseReportCsvParser` drops blank-name zero rows) |

**The only query names a sheet GET may carry are `conf` and `seating`**
(`SheetSort` allowlist). **Never send `r`** — mere presence triggers
server-side bulk seat auto-allocation. `SheetRouteSafetyTest` fails the
build otherwise. Sort is honoured only when `SheetSort.optionsFor(export)`
lists it.

Print: student chits 12-up; checking slip 2-up; seating 5i one gender per
A4 from the in-memory roll.

---

## Live HTTP surface the Java client must speak

From `StaffApi` live methods + `SheetTransport`. Mock-only `/staff/*` and
`POST /api/user/login` exist behind `-Pdipi.useMock=true` and **must not**
be the live default.

| Method | Path | Notes |
|---|---|---|
| GET | `/` | often 403 with login block |
| GET | `/user/login` | prefer 200 form |
| POST | parsed login action | `name, pass, form_build_id, form_id, op` |
| GET | `/user/logout` | |
| GET | `/services/session/token` | CSRF keep-alive |
| GET | `/centre` → `/centre/{cid}` | `dh_user_center` |
| GET | `/centre/{cid}/acco-handler` | read-only rooms |
| GET | `/search-course/{cid}/{courseId}` | `s,t,g,d` |
| GET | `/change-status/{id}` | `s, l=0, c` — never `Approved` |
| POST | `/app-update-attended/{id}` | allocation fields only |
| GET | `/{sheet}/{cid}/{courseId}` | page or document; `conf`/`seating` only |
| GET | `/app/{id}/edit` | display-only; NPI in body |
| GET | `/application-view/{id}` | allowlist parse |
| GET | `/app-courses/{id}` `/app-activity/{id}` `/app-clarifications/{id}` | history |
| GET | `/show-clarification/{appId}/{clarId}` | PDF, no `r` |
| GET + POST | `/centre/{cid}/course-report` | form scrape then CSV |

Do not assume: `POST /api/user/login`, `GET /staff/session`,
`POST /search-app`, a hardcoded Dhamma Giri centre.

---

## Parsers that already pin the contract

| Parser | Input | Must survive a port |
|---|---|---|
| login form scrape | 200/403 HTML | form action + `form_build_id` + `form_id` |
| `CentrePageParser` | `GET /centre/{cid}` | upcoming links, older options, matrix, desk tiles present |
| `SearchPageParser` | worklist HTML | `var dataset` public fields; NPI never stored |
| `DaySummaryParser` | `#day-summary` | counts only |
| `AttendedTableParser` | `#table-attending` | `a_id` + allocation cells |
| `AccoHandlerParser` | acco-handler JSON/HTML | room inventory, GET only |
| `TeacherListParser` | UNTHEMED teacher-list | seat, `SeatKind`, backrest; no Comments |
| `ApplicationViewParser` | themed application-view | header / Personal / Course History / Health allowlist |
| `ApplicantHistoryParser` + `HtmlForms`/`HtmlTables` | history pages | |
| `CourseReportFormParser` / `CourseReportCsvParser` | report form + CSV | drop blank-name zero rows |
| Sensitive-info in-memory map | card / edit / health | session only, or course-ops encrypted store |

Existing JVM tests of these parsers are the acceptance oracle for a Java
rewrite. Port the **invariants**, not the Kotlin source line-for-line.

---

## Persistence and privacy

| Store | What | Wipe |
|---|---|---|
| EncryptedSharedPreferences `dipi_staff_secure` | cookie jar, CSRF, remember-me user/pass | erase-all; logout keeps remember-me, drops cookies |
| DataStore `dipi_staff_prefs` | skin, lotus, `tabletMode`, room/hall columns | erase-all |
| Encrypted `dipi_course_ops` | PIN (salted SHA-256), roll + application-view answers | course change, logout, erase-all |
| Room + SQLCipher | one course cache + outbox | erase-all |
| In-memory `SensitiveInfo` | ID docs + health (desk mode) | course change, logout, erase-all |
| `cacheDir/sheets` | PDF/XLS/CSV only | logout, session expiry, erase-all |

**Never persist or log:** `aadhar`, `passport`, `voterid`, `pancard`,
`ae_*`. Display-only amendment (2026-08-16): those MAY appear on screen
for desk verification. Course-ops amendment (2026-09-02): roll + answers
(health included) MAY persist encrypted for the running course only.

---

## Design authority

1. `docs/design/DIPI-Staff.dc.html` wins every visual argument (1 px = 1 dp,
   font px = sp).
2. `docs/DESIGN.md` measurements + shipped-delta ledger (do-not-re-propose).
3. Tokens via `Industry` / `LocalDipi` — five skins (Steel default; Paper,
   Blossom, Pond, Still). Dark is the Steel night ramp. Status / severity
   hexes never follow the skin (`#A33A34` / `#E0796F`).
4. Type: Barlow Condensed, IBM Plex Mono, Roboto. OFL faces already in
   `core/ui/src/main/res/font/` — see `docs/FONTS.md`.
5. Accent means live / occupied / selected. Desk tiles: transparent fill,
   hairline, 0 elevation.
6. Touch targets ≥ 48 dp on the tablet; desktop pointer/hover is a bonus,
   not a theme. Lotus is a **vector mark**, never a photograph (photos
   deleted 1.15.0).

---

## Hard rules (copy verbatim into the Java spec)

From `AGENTS.md`. These win over everything below them.

1. No access control in the app. Send the request; render the server
   response verbatim.
2. No status engine. Display and send strings only.
3. Never send status `Approved`.
4. Status write = existing `/change-status/{id}?s=&l=&c=` with `l=0`.
5. No attendance writes in v1 (narrowed only by the allocation-sync
   amendment: `POST /app-update-attended/{id}` with the dialog's fields).
6. Never use APP API / `get-app-detail`. Parse desk HTML only.
7. No NPI columns in the local DB or logs.
8. Server URL is the live host (`https://dipi.vridhamma.org`). No URL
   field on login; centre comes from `dh_user_center`.
9. Design file wins visual arguments.
10. Do not commit `local.properties`, keystores, or real student data.
11. SemVer on every shippable change (desktop versioning must be defined
    in the spec; do not silently reuse the Android `versionCode`).
12. Server messages verbatim.
13. **Bridge rule:** letters, waitlist, LC review, SMS/WhatsApp dispatch
    are black boxes behind `_change_status`. Never reimplement, never
    preview letter bodies.

---

## Retired and parked — do not re-propose

**Retired from the app** (still on the desk site): Manage Courses, Daily
Activity, SMS Report, Letters (2026-08-30), Bulk Mail (2026-09-05),
`CentreEditScreen` (native Centre Settings replaced it), Male/Female
course PDFs (1.37.1), skin photographs (1.15.0), sign-in photo hero.

**Parked / owner-gated:** server-side Advanced Search (`POST /search-app`
— HAR re-verify + sign-off first); real photo upload (no live route);
T6 Group seating + Cell list (Reach v5: skipped); attendance writes
inside Course ops (owner: read-only).

---

## Android-only machinery a Java port must replace, not copy

These APIs do not exist on a desktop JVM. The planning spec must name a
cross-platform substitute for each:

| Android piece | Job |
|---|---|
| Jetpack Compose / Material 3 | UI |
| Hilt | DI |
| Retrofit + OkHttp + Android `CookieManager` | HTTP + cookie jar |
| EncryptedSharedPreferences / Android Keystore | secrets |
| DataStore | non-secret prefs |
| Room + SQLCipher | course cache + outbox |
| Android WebView (JS off) | HTML sheet viewer |
| FileProvider / system viewer | PDF/XLS open |
| `adb` + Pixel C | install/verify |
| Robolectric Compose tests | UI tests |
| `BuildConfig.BASE_URL` / `-Pdipi.useMock` | host + fixtures |

The **product rules, parsers' invariants, and design tokens** transfer.
The **framework** does not.

---

## Read-first list for the planning session

1. `AGENTS.md` — Hard rules and Current assumptions.
2. `docs/DESIGN.md` — visual authority + shipped-delta (through 1.42.0).
3. `docs/LIVE-DESK.md` — HAR, PHP inventory, sheet skeletons.
4. `docs/DECISIONS.md` — owner rulings (ignore the stale footer version).
5. This inventory.
6. `docs/design/DIPI-Staff.dc.html` — open in a browser.
7. Course-ops specs under `docs/specs/2026-09-02-course-ops-2*.md`.
8. `core/network/.../StaffApi.kt` and `SheetExport.kt` — live surface.
9. `feature/course/.../DeskTiles.kt` — what the centre actually offers.
10. `CLAUDE.md` — current ship line and test-command rule.

Server reference: `/Users/wizops/DIPI/dipi-web` module `dh_manageapp`,
read-only.
