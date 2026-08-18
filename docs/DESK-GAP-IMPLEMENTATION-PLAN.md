# Desk Gap Implementation Plan — everything the backend serves that the app doesn't show yet

**Scope date:** 2026-08-17 · **App at scoping time:** 1.18.0 (versionCode 29) on `main`
**Audience:** worker agents implementing one slice each. Read `AGENTS.md` and `docs/LIVE-DESK-HAR.md` first. Every slice below is self-contained: backend facts, app touch points, tests, hazards.

---

## 0. Non-negotiable ground rules (apply to every slice)

1. **Transport is the browser desk** (Drupal HTML on `https://dipi.vridhamma.org`), session cookie auth. Never `/services/rest/dipi/*`, never `/staff/*`, never `get-app-detail`.
2. **Backend PHP is immutable.** `/Users/wizops/DIPI/dipi-web` is read-only reference.
3. **No client ACL.** Send the request; render the server's refusal verbatim.
4. **No status engine.** Never send `Approved`. Status write stays `GET /change-status/{id}?s=&l=0&c=`.
5. **NPI** (`aadhar`, `passport`, `voterid`, `pancard`, `ae_*`, health) is display-only, in-memory `SensitiveInfo` — never Room/DataStore/DTO/logs.
6. **NEVER send an `r` query param on any sheet/seating/cell GET** — its mere presence triggers server-side bulk auto-allocation (`inc/zero-day.inc:17-46`).
7. **Print-sheet GETs have a write side effect**: `teacher-list`, `manager-list`, `cell-list`, `seating`, `group-seating`, `seating2` call `zeroize_new_course_data()` (`inc/zero-day.inc:3185-3189`) which deletes `dh_applicant_course` rows for **new** students on the course. The 12 shipped exports already accept this (the desk browser does it too); do not add *new* callers of these routes casually.
8. **Drupal forms need scraping**: `form_build_id` + `form_token` + `form_id`, POST to the parsed action. Same pattern as login and course-report (`CourseReportFormParser` in `core/network/.../StaffApi.kt:397`).
9. **Most AJAX mutators are GET with no CSRF** — permission-gated only. Fire them exactly as the desk JS does; never invent params.
10. **SemVer**: one MINOR bump per shipped slice (1.19.0/30 onward), `versionCode` +1 every time. Install debug APK on the Pixel C (`10.0.0.144:5555`) after each tablet-facing slice.
11. Tests: JVM unit tests only (`:app:testDebugUnitTest`, `:core:model:test`, `:core:network:testDebugUnitTest`). Add mock routes to `DipiMockDispatcher` + `MockFixtures` for every new endpoint.
12. Do not commit unless the owner asks.

**Where placeholders live today:** every unimplemented control routes through `DeskViewModel.openLater(title, route)` → `DeskActionScreen` (`feature/course/.../DeskActionScreen.kt:17`) showing *"This control is wired to the desk path; implementation is the next slice."* Entry points: `centreDeskTiles()` and `courseHubTiles()` in `feature/course/.../DeskTiles.kt`, plus `AdvancedSearchScreen.onOpenDesk`.

**Already shipped (do not rebuild):** login scrape · centre dashboard + older courses · worklist `var dataset` · status write · room allocation push (`POST /app-update-attended/{id}`) + zero-day room pull · acco **read** · 12 board sheet exports + hardened viewer (tablet) · display-only `/app/{id}/edit` · check-in/audit/calling/rooms panes · advanced search over the local Room cache · photo loading from `dataset.photo`.

---

## Wave 1 — cheap wins on existing plumbing

### Slice 1: Phone hub sheet-export parity
**Gap:** the 12 sheet exports are live on the tablet Board pane (`SheetTransport`, `SheetViewerPane`) but the phone course-hub ⋯ overflow (`courseHubDeskTiles`) still lands on the placeholder for: Day 0 List, Seating Plan, Student Chit, Checking Slip, Male/Female PDF, Teachers List, Laundry List, Valuable List.
**Build:** route those overflow items through the existing `DeskViewModel.openSheet(...)`/sheet-view path instead of `openLater`. The viewer already renders below-1100dp; verify and adapt padding if needed.
**Files:** `DipiAppUi.kt` (CourseHub wiring), `CourseHubScreen.kt`, possibly `SheetViewerPane.kt`.
**Tests:** hub click opens viewer, not placeholder; refusal HTML renders verbatim.
**Hazards:** none new — same routes as tablet. Keep the no-`r` rule.

### Slice 2: Day-11 course summary report (`report-day11`)
**Gap:** `GET /report-day11/{cid}/{courseId}` (`dh_manageapp.module:441` → `course.inc:835`) streams the Day-11 PDF from S3. The hub tile "Course Summary Report" is a placeholder; the tablet Board doesn't list it either.
**Build:** add `SheetExport.Day11Report` (`SheetRoute.Document("report-day11", "application/pdf", "pdf")`) to `core/model/.../SheetExport.kt` + `SheetRoutes.of`; add to Board `EXPORTS` and the phone hub. Mock: PDF bytes route in `DipiMockDispatcher`.
**Tests:** `SheetExportTest` route mapping; `ExportMockTest` stream-to-cache; wipe on logout.
**Hazards:** display-only, `cacheDir/sheets`, wiped like all sheets.

### Slice 3: Applicant photo via `show-photo/{id}`
**Gap:** `GET /show-photo/{app_id}` (`dh_manageapp.module:653` → `:2353`) streams the S3 photo on the desk session — a stable fallback when `dataset.photo` is a relative/expired URL.
**Build:** extend `PhotoLoader` (`core/network/.../PhotoLoader.kt`) to fall back to `/show-photo/{id}`; in-memory only, initials placeholder on any failure (existing contract).
**Tests:** loader fallback on 404 primary; no disk writes.
**Hazards:** photos are personal data — never cache to disk, never log.

---

## Wave 2 — the placeholder centre tiles (read-mostly)

### Slice 4: Daily Activity
**Backend:** `daily-activity/{cid}` (`dh_manageapp.module:556`) = `drupal_get_form(dh_daily_activity_form)` (`bulk-mail.inc`/reports area, form at `:1581`). Filters `dh_log` by date range / course / user / event; renders `#table-daily-activity`.
**Build:** scrape form (tokens + select options), POST filters, parse the result table (id, date, user, event, detail columns — verify against live HTML). Native list screen with date-range + event filter chips. Read-only.
**Files:** new `DailyActivityParser` (core/network) + repository fetch + screen in `feature/course` (phone) and a desk pane or dialog (tablet). Replace the `centreDeskTiles` placeholder route.
**Tests:** parser fixture (rows, empty, 403 verbatim), form-token POST shape against mock.
**Hazards:** log lines may embed applicant names — display-only, never persist rows.

### Slice 5: SMS Report
**Backend:** `centre/{cid}/sms-report` (`dh_manageapp.module:258` → `reports.inc:5`) renders `#table-applicants` of SMS credits per course; row expand loads `GET /sms-count/{courseId}` (`:298` → `:2310`) — an HTML fragment of letter id / name / SMS count.
**Build:** parse the summary table; tap a course row → fetch + parse the fragment inline. Read-only screen replacing the placeholder tile.
**Tests:** two parsers with fixtures; expand-on-tap renders fragment rows.
**Hazards:** none — aggregate counts only.

### Slice 6: Bulk Mail schedule (list + safe toggles)
**Backend:** list `centre/{cid}/bulk-mail-schedule` (`:263` → `bulk-mail.inc:71`); GET writes `bulk-mail/{cid}/{id}/mute` / `unmute` (`:288-294`), `delete` (`:268`); log at `show-log` / `get-log` (`:278/:283`). Schedules are *created* from the search-app form (out of scope here).
**Build:** native list of scheduled jobs with status; mute/unmute buttons; delete behind a confirm dialog; log viewer (parse `get-log` rows). Do **not** build schedule creation in this slice.
**Tests:** list parser; mute → GET fired verbatim, server reply snackbar; delete confirm gate.
**Hazards:** GET writes with no CSRF — user-initiated taps only, never automatic. **Owner sign-off: new write surface (mute/delete).**

### Slice 7: Centre Settings (edit) + acco write
**Backend:** `centre/{cid}/edit` (`:138` → `centre.inc:69`) — Drupal form updating `dh_center` + `dh_center_setting` (name, address, email, reconf/preconf/WhatsApp flags, seat config; form token). The embedded room editor POSTs DataTables Editor payloads to `centre/{cid}/acco-handler` (`centre.inc:449`, create/edit, soft-delete only, **no token**).
**Build (two sub-slices):**
 a. **Read-only view** of centre settings parsed from the form's current values — replaces the placeholder immediately.
 b. **Acco write**: room band add/edit (`csa_gender`, `csa_section`, `csa_room` token syntax `1:6W, 9IC`) posting the same Editor shape the desk JS sends. App already parses this format (`AccoHandlerParser`).
**Files:** extend `AccoHandlerParser` with request-body builders; `CentreOpsScreen`/pane gains an edit affordance.
**Tests:** Editor POST body matches desk JS byte-for-byte (fixture from live HAR if available); soft-delete only; parse round-trip.
**Hazards:** acco edits change what every device sees as valid rooms. **Owner sign-off: full-form centre edit POST** (sub-slice b limited to acco is lower risk).

### Slice 8: Manage Courses (read first, CRUD behind sign-off)
**Backend:** page `manage-course/{cid}` (`:103` → `course.inc:70`); AJAX CRUD `POST /course/handler/{cid}` (`course.inc:377`, DataTables Editor, **no token**); helpers `GET /app-student-count-finalize/{courseId}` (`:626` → `:2284`) and `GET /app-student-count-cancel/{ids}` (`:634` → `:2254`). Finalize sets attended → STATUS-ATTENDED and copies rows to the student tables — **one-way**.
**Build (staged):**
 a. **Read-only course list** (type, dates, per-cohort statuses `c_status_nm/om/nf/of/svr_m/f`, finalized flag) parsed from the handler GET — replaces the tile.
 b. **Edit dates/statuses/comments** via handler POST — owner sign-off.
 c. **Finalize** — separate owner decision; confirm dialog must show the `app-student-count-finalize` count and say "one-way".
**Tests:** handler-GET parser; POST shape; finalize gate never fires without explicit double-confirm.
**Hazards:** finalize is irreversible; cancel checks `ApplicantFound`. Ship (a) alone first.

---

## Wave 3 — worklist power actions (the desk JS the app doesn't mirror yet)

### Slice 9: Applicant history fragments on the card
**Backend:** `GET /app-courses/{app_id}` (`:198` → `search.inc:1417`) prior-course table; `GET /app-activity/{app_id}` (`:208` → `:1802`) status/letter/LC log; `GET /app-clarifications/{app_id}` (`:203` → `:1774`) clarification rows (file via `show-clarification/{app_id}/{clar_id}`, PDF stream `:660`).
**Build:** three collapsible sections on the applicant card / desk Applications detail, fetched on expand, parsed from the HTML fragments. Clarification PDF opens through the existing sheet-document path (cache wiped with sheets).
**Tests:** three fragment parsers with fixtures; lazy fetch on expand.
**Hazards:** fragments carry history text — in-memory only.

### Slice 10: Transfer to another course
**Backend:** `GET /get-courses/{centreId}` (`:562` → `:1306`) upcoming `{name,id}` for the dialog; then `GET /move-to-course/{app_id}/{course_id}/{centre_id}?c={comment}` (`:361` → `:1824`). Server resets status to Received, clears conf no, blocks LC destinations and cross-centre without permission — render its JSON `msg` verbatim.
**Build:** "Transfer…" action on the worklist row / card: dialog lists courses from `get-courses`, optional comment, confirm → GET, snackbar with server msg, then worklist refresh.
**Tests:** dialog fires exact URL; refusal msg surfaces; refresh after success.
**Hazards:** real write, permission-gated server-side. **Owner sign-off: new write surface.**

### Slice 11: Delete application + send-AT-email
**Backend:** `GET /app/{app_id}/delete` (`:335` → `:1895`) soft-delete, JSON `{status,msg}`; `GET /app/{app_id}/send-at-email` (`:340` → `application.inc:1285`) resends the AT letter for R-ATReview/A-ATReview apps.
**Build:** overflow actions on the card: Delete behind a typed-confirm dialog ("type the conf no"); Send AT email as a single tap with snackbar.
**Tests:** confirm gate; URLs verbatim; server msg rendered.
**Hazards:** delete is a write (soft, but user-visible). **Owner sign-off.**

### Slice 12: Desk-backed Advanced Search
**Gap:** in-app search only queries the local Room cache; the desk form `search-app/{cid}` (`:181` → `search.inc:1396`) searches everything (name, conf no, phones, status[], dates, pin/city/occupation, course-count bounds) and returns the same `var dataset` the worklist uses.
**Build:** scrape the form (tokens + option lists), POST the user's criteria, parse `var dataset` with the existing `SearchPageParser`, show results in the existing `AdvancedSearchScreen` (merge/replace the local-cache results, labelled "from desk"). Replace the `onOpenDesk` placeholder.
**Tests:** form POST shape; dataset parse reuse; NPI from results stays in `SensitiveInfo` (existing path).
**Hazards:** none new — read + existing parser. Bulk-mail scheduling fields on that form are **out of scope** here.

---

## Wave 4 — new write surfaces (each needs explicit owner sign-off before build)

### Slice 13: Applicant add / edit (full form POST)
**Backend:** `app/add/{cid}/{courseId}` (`:346`) and `app/{app_id}/edit` (`:325`) both render `dh_ma_applicant_form` (`application.inc:4`; form token). Submit writes `a_*`, `ac_*`, `ae_*`, `aa_*`, `al_*` — including NPI and health fields, `?ref=` prefill on add.
**Build:** scrape form → native editor → POST back **only fields the user changed plus every hidden/untouched field exactly as scraped** (Drupal validates the full form). NPI/health fields render from the scrape but are never persisted locally (extend the `SensitiveInfo` pattern); autocomplete helpers available: `get-location-from-pincode?code=`, `autocomplete/get-country|state|city` (`:568-583`).
**Tests:** round-trip fidelity (scrape → POST unchanged = server accepts); changed-field POST; NPI absent from Room/DataStore after the flow; mock route in dispatcher.
**Hazards:** the highest-risk slice — it can set `a_attended` and `a_status=Left`. Constrain the native UI to the fields the owner approves; everything else posts back verbatim-as-scraped.

### Slice 14: Assign Teacher + AT schedule
**Backend:** `assign-teacher/{cid}/{courseId}` (`:451` → `at-schedule.inc:574`) lists AT applications + a form (token) adding trainee/teacher rows to `dh_course_teacher`; `at-schedule/{cid}` (`:446` → `:4`) overview. GET writes (no CSRF): `at-schedule/change-status/{ct_id}?s=Confirmed|Cancelled&comment=` (`:456` → `:1293`, emails the AT), `change-type?s=Conducting|Assisting` (`:461`), `change-group?s=1..9` (`:466`), `del-trainee-teacher/...` (`:471`). Info: `GET /at-schedule/get-at-info/{teacherId}` (`:476`), autocomplete `get-trainee-teacher` / `get-teacher` (`:486`, `:588`).
**Build:** native AT list per course (name, type, group, status), status/type/group pickers firing the GETs, add-trainee via scraped form. Blocked on finalized courses — render the server's block verbatim.
**Tests:** each GET verbatim; status change surfaces server msg; form-token add.
**Hazards:** status change emails ATs — make every action an explicit tap with confirm on Cancel.

### Slice 15: Referrals
**Backend:** list `referral/{cid}` (`:502` → `referral.inc:1029`) and centre-owned `center-referral/{cid}` (`:535+`); add/edit forms (`:492/:497` → `:1002`, token, `?aid=` prefills from an applicant); GET writes `referral/{cid}/{ref_id}/delete` (`:507`), `referral/read-only/{ref_id}?readonly=0|1` (`:513`); activity fragment `referral/get-activity/{id}` (`:529`).
**Build:** referral list screen + "Add referral" action on the applicant card (prefill via `?aid=`); edit/delete behind confirms; activity on expand.
**Tests:** list/fragment parsers; form POST; delete confirm.
**Hazards:** referral queries include NPI columns — display-only. Delete blocked server-side when `r_readonly`; render the refusal.

### Slice 16: Letters CRUD
**Backend:** list `letters/{cid}` (`:218` → `letters.inc:12`, `#table-letters`); edit/copy forms (`:223/:228`, token; fields `l_name,l_event,l_subject,l_body,l_sms,l_attachment`); GET writes delete/restore/delattach (`:233-243`); merge fields `letter-fields/{cid}` + DataTables handler (`:248/:253` → `:322/:409`).
**Build:** letter list + read-only body preview first; edit/copy behind owner sign-off (rich text `l_body` — post back verbatim-as-scraped except edited fields).
**Tests:** list parser; edit POST fidelity.
**Hazards:** letters drive every mail/SMS the centre sends — read-only view ships first. Note: the desk UI's own "add" link is unregistered upstream (`letters/{cid}/add` 404s) — copy-from-existing is the real path.

---

## Explicitly OUT of scope (do not build, do not call)

| Thing | Why |
|---|---|
| `?r=` on `seating`, `group-seating`, `cell-list`; `seating-update`/`group-seating-update` drag writes | Server-side bulk auto-allocation / seat writes via GET. Interactive seating is its own future vertical needing an owner decision and a design. |
| `finalize_course` beyond Slice 8c's gated confirm | One-way copy into the student DB. |
| `/services/rest/dipi/*` (all of `dipi_api`), `get-app-detail`, `mark-attended-by-conf-num` | Wrong product surface; AGENTS.md rule 6; BOLA-shaped endpoints. |
| `dh_atportal` (LC review, address book, AT-Seva, AT-meet, sit/serve) | Separate AT product with its own users; not the registrar desk. |
| `dh_patrika`, `find_people` | Newsletter admin and Drupal admin search — not desk features. |
| Webhooks (`webhook/mailgun`, `wa-hook*`), admin form, `user-mapping`, `center-dashboard`, `vri-management` | Server-to-server / VRI-admin surfaces. |
| Bulk-mail schedule **creation**, `search-lc` | Creation rides the search form (needs `mass mail` perm + owner decision); `search-lc` is mis-registered upstream (its own sub-links 404). |
| Photo **upload** | Only exists in `dipi_api post-application`; desk has no upload. Stays mock-only. |

---

## Ordering, versions, owner decisions

| Order | Slice | Version | Owner sign-off needed? |
|---|---|---|---|
| 1 | 1 Phone sheet parity | 1.19.0 (30) | No |
| 2 | 2 Day-11 report | 1.20.0 (31) | No |
| 3 | 3 show-photo fallback | patch or with #2 | No |
| 4 | 9 History fragments | 1.21.0 | No |
| 5 | 12 Desk-backed search | 1.22.0 | No |
| 6 | 5 SMS report | 1.23.0 | No |
| 7 | 4 Daily activity | 1.24.0 | No |
| 8 | 8a Courses read-only | 1.25.0 | No |
| 9 | 7a Centre settings read | 1.26.0 | No |
| 10 | 10 Transfer | — | **Yes** (write) |
| 11 | 11 Delete + AT email | — | **Yes** (write) |
| 12 | 6 Bulk-mail toggles | — | **Yes** (write) |
| 13 | 7b Acco write | — | **Yes** (write) |
| 14 | 8b/8c Course edit/finalize | — | **Yes** (write, one-way) |
| 15 | 13 Applicant add/edit | — | **Yes** (biggest write) |
| 16 | 14 Assign teacher | — | **Yes** (write + emails) |
| 17 | 15 Referrals | — | **Yes** (write) |
| 18 | 16 Letters | — | **Yes** (write; read-only list can ship early unsigned) |

Every slice: mock route in `DipiMockDispatcher` + fixtures, parser unit tests, screen tests, SemVer bump, tablet install if registrar-facing. Worker agents take exactly one slice, own only that slice's files, and run only the scoped test targets before handing to the integrator.
