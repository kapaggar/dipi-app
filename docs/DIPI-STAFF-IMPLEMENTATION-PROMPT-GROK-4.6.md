# DIPI Staff — implementation prompt for Grok Heavy Build 4.6

**Date:** 2026-08-13  
**Status (2026-08-15):** Vertical 1 is **shipped at 1.4.1**. Product rules below still hold. **Transport does not:** there is no `/staff` JSON layer and no Services login on `https://dipi.vridhamma.org`. The live client uses the browser desk (`docs/LIVE-DESK-HAR.md`, `AGENTS.md`). PHP is immutable. Do not hardcode Dhamma Giri.

**Date:** 2026-08-13
**Repo:** `/Users/wizops/DIPI/dipi-app`
**Server source (read-only reference):** `/Users/wizops/DIPI/dipi-web` (Drupal 7, module `dh_manageapp`)
**Design (the visual + behavioral spec):** `docs/DIPI Staff.dc.html` — open it in a browser; frame 1a is a live click-through. **The design wins every visual argument.**
**API contract:** `docs/openapi-staff.yaml` is **mock-only / historical**. Live paths are in `docs/LIVE-DESK-HAR.md`.
**Background:** `docs/00-architecture.md`, `docs/DIPI-STAFF-ANDROID-GROK-PROMPT.md` (domain spec; where it conflicts with this file **on product rules**, this file wins).

---

## 0. Mission

Build **Vertical 1** of DIPI Staff: a native Android app for the **registrar at the main centre** (Dhamma Giri). One user, short sessions (30 s – 2 min at a busy desk): find an applicant, read the card, change a status, put the phone down. Backend is the existing Drupal 7 `dh_manageapp` plus a small new `/staff` JSON layer that may not exist yet — **code against the contract with a mock server first**; tag every mocked route `TODO(server): <php function>`.

Package `org.dhamma.dipi.staff`. Phone portrait first; tablet ≥ 600 dp gets list–detail (worklist left, card right).

## 1. Ground rules (non-negotiable)

1. **No access control in the app. None.** No tenancy filtering, no gender hiding, no permission gating, no "is this allowed" pre-checks, no disabled-because-finalized logic. The server filters everything and returns a suitable error for erroneous access; the app sends the request and renders the server's response **verbatim**. (Decision by the owner, 2026-08-13 — overrides anything contrary in the older docs.)
2. **No status engine in Kotlin.** The app displays and sends status strings; the server decides transitions, waitlist, conf-no minting, letters. Never `UPDATE` state locally except as an optimistic echo that the next fetch overwrites.
3. **Status choices never include "Approved".** The sheet offers exactly the design's list (see §4.5) merged with `GET /staff/meta/statuses` when available.
4. **Server messages verbatim.** Error snackbars show the server's `msg` text unmodified, e.g. `Please Edit application and choose Area teacher before approving!`.
5. **Bridge rule:** letters, waitlist, LC review, SMS/WhatsApp are black boxes behind `_change_status`. Never reimplement, never preview letter bodies.
6. **Never parse HTML.** `/search-app` is a web page, not an API. Reads that don't exist as JSON get a new `/staff` route (mocked until PHP lands).
7. **NPI stays out of the client by construction:** the server only sends whitelisted fields, so Room schemas and logs simply have no columns/keys for `ae_*`, Aadhaar, PAN, passport, voter id. Audit findings that reference ID fields arrive **pre-masked from the server** (§7).
8. **Server URL is fixed at build time** (`BuildConfig.BASE_URL`, debug-overridable via a Gradle property). The design's login has no URL field; centre comes from the account after sign-in.
9. English UI, all strings in resources. No analytics/crash SDKs in v1.
10. Small commits in the §10 order; each step ends green (`./gradlew :app:testDebugUnitTest` and `:app:assembleDebug`).

## 2. Stack & skeleton

Kotlin (JDK 17+), Jetpack Compose + Material 3, Navigation 3 (`ListDetailSceneStrategy` for tablet), Hilt, Coroutines/Flow, Retrofit + OkHttp + kotlinx.serialization, Encrypted DataStore (session cookie + CSRF token + theme), Room + SQLCipher (active course only), MockWebServer for the mock backend. Min SDK 26, target 35+. Edge-to-edge, light + dark theme (user-switchable in Settings, persisted).

```
:app
:core:model        value types + DTOs
:core:network      Retrofit StaffApi + DrupalAuthApi + mock server
:core:database     Room: applicants, outbox (one course)
:core:datastore    session, csrf, theme
:core:ui           theme tokens, status badge, chips, list row, snackbar host
:core:audit        pure-Kotlin audit rules (§7)
:feature:auth      login
:feature:course    centre + course picker
:feature:applicants  today list, card, change-status sheet
:feature:photos    photo review
:feature:summary   day summary
:feature:settings
```

## 3. Design system (extracted from `DIPI Staff.dc.html` — use these exact values)

**Type:** Barlow (body/UI), **Barlow Condensed** (headings, section labels, chip text), **IBM Plex Mono** (conf numbers, audit rule ids, tabular numbers). Bundle via `res/font` (Google Fonts, OFL).

**Core palette — light:** background `#F2F2F3`, foreground `#1D1F20`, muted `#5D5D60`, hairline `#D4D4D7`, hairline-strong `#B7B7BA`, hover `#E7E7EA`, field `#FFFFFF`, tint `#EEF6FF`.
**Dark:** background `#14181C`, foreground `#ECEFF2`, muted `#98A1A8`, hairline `#232A31`, hairline-strong `#3A424A`, hover/field `#1C2229`, tint `#22384C`.
**Primary/accent (both themes):** steel blue `#5980A6`; pressed/link tone `#416180`. Selected chips: `#5980A6` fill, white text; unselected: transparent fill, hairline-strong border.

**Status badge tones** (light `bg/fg`, dark `bg/fg`):

| Status | light | dark |
|---|---|---|
| Confirmed | `#DFEAE1` / `#2F5A41` | `#22392C` / `#A9CDB6` |
| Pending | `#E7E7EA` / `#5D5D60` | `#2A3138` / `#C0C7CD` |
| Received, Reconfirmation | `#EEF6FF` / `#2C455D` | `#22384C` / `#B5D9FD` |
| Expected | `#F0ECE2` / `#6A5A38` | `#3A3223` / `#DBCBA6` |
| Cancelled, Rejected | `#F0E3E3` / `#7A4141` | `#3B2626` / `#DEAEAE` |
| any unknown status | Pending tone | Pending tone |

**Audit severity:** hard `#7A4141`, safety `#6A5A38`, soft `#7A7A7D`. Row flag tag: `△ 3 to fix` in `#A15C5C` when any hard flag, else `△ 1 flag` in `#8A7645`.
**Snackbar:** normal `#2B2B2D` (light) / `#0D1114` (dark); error `#5A2F2F`. Auto-dismiss ~4 s.
**Feel:** hairline-framed cards, no elevation shadows, no gradients, calm and utilitarian. Match the mock's spacing/hierarchy; when in doubt, screenshot-compare against frame 1a.

## 4. Screens & behaviors (all in the design; build all states shown in frames 1a/1b)

**Global chrome:** `TEST MODE — DHAMMA GIRI SANDBOX` strip pinned on **every** screen when session `modeTest=true`. Offline banner `◍ Offline — showing cached list · N changes waiting to sync` when offline and/or outbox non-empty; queued rows get tint background + `queued: → Confirmed` suffix. Skeleton rows while Today loads.

### 4.1 Login
Wordmark "DIPI Staff", caption "Centre admin desk". Username + password, one **Sign in** button, helper text "Your centre is read from your account after sign-in." Error state renders the server's message verbatim under the form. No URL field, no sign-up, no forgot-password.

### 4.2 Centre & courses
Centre name shown as a header ("Dhamma Giri · from your account · registrar.giri") — no picker when the account has one centre; plain list picker when it has several. "Upcoming courses" list: name, date range, `STARTS IN N DAYS` (accent tone on the soonest). Finalized/past courses never appear (server-side query). Tap → Today.

### 4.3 Today (home; most engineering effort here)
Top bar: course name + dates, centre small above; icons **▤** (day summary) **◎** (photo review) **⚙** (settings). Search field `⌕ Name, conf no, phone…` — debounced server-side `q` (name / conf no / phone / email); local filter over the cached list when offline. Horizontally scrolling **multi-select** status chips with counts from the server (`All 214 · Pending 61 · Received 48 · Confirmed 72 · Expected 18 · Cancelled 9 · Rejected 6` in fixtures); "All" clears. Dense list rows: name (primary), conf no in mono right-aligned (`—` when none), status badge, meta line `34 F · Pune, Maharashtra, India`, flag tag when audited (§7). Empty state: "No applicants match those filters." List keyed by applicant id; pull-to-refresh.

### 4.4 Applicant card
Status-toned header: name large, status badge, conf no (or "no conf no"), `age gender`, **Monk/Nun** tag when robed. Photo thumbnail with note line: `◎ Photo looks fine` / `◎ Photo needs review` / `◎ Photo fixed` (links into Photo review). **Audit panel** — title `Needs attention · N` (hard-red when any hard flag) or `Audit clean`; each finding: severity square, human label, mono detail with rule id (e.g. `phone_prefix_invalid · +91 50031 55402`); clean records show one quiet line "No audit flags. Identity, contact, emergency and cross-course checks all pass." Facts: Location, Mobile (tap-to-dial), Email (tap-to-compose), Home phone, Date of birth, Application date. **Old students** additionally get "Courses completed · course audit": First, Most recent (`date · centre · teacher`), and counts by type (`10-day 4 · Satipatthana 1 · Dhamma service 2`). Bottom actions: **Call**, **Email**, primary **Change status**. Deliberately absent: ID numbers, medical info, street address, edit button, attendance toggle.

### 4.5 Change-status bottom sheet
Header `«current» → choose new status`. Radio list exactly: `Pending, Received, Confirmed, Expected, Reconfirmation, Cancelled, Rejected, Custom…` (replace/merge with `GET /staff/meta/statuses` payload when live; **never add Approved**). Optional comment field. Persistent notice with ✉: "The server may send the applicant a letter for this change." **Confirm change** → POST; success snackbar `Status updated · conf no NF129` (include conf no only when the server minted one); failure snackbar shows server `msg` verbatim in error color. Optimistic row update, reconciled by re-fetch; offline → enqueue in outbox.

### 4.6 Photo review (`◎`)
Grid/list of applicant photos for the course. Filter pills with counts: `All / Suggested / Auto-fixed / Fixed / Unreviewed`. Per card: photo (rendered with current rotation), name, conf no, badge (`suggest ↻90°`, `✨ auto ↻180° — ✓ keep`, `suggest ✂ zoom`, `no face found`, `✓ fixed`; badge colors: fixed `#3D6B52`, auto `#5A63A8`, suggest `#8A6A35`, no-face `#6C7075`), controls `↺ ↻ ✂ ✓`. Corrections are **geometry only, kept on device** until upload. Top-right **`⬆ dipi (n)`** uploads all fixed photos: the server resubmits the whole application form with the photo swapped, then re-reads it and flags any field that drifted. Client sends `{applicantId, rotate, crop}` per photo — nothing else. Empty-upload toast: "No fixed, un-uploaded photos yet"; success: `✓ Uploaded n photo(s), all other fields preserved`. Suggestion classes come from the server (`TODO(server)`); until then the mock seeds them (see fixtures).

### 4.7 Day summary (`▤`)
Read-only, computed from the worklist — no new required endpoint. Header `Day 0 · 20 Aug 2026`. Big number **Expected today** (confirmed applicants + servers, e.g. `88 = 80 + 8`). **Arrived** progress `0 of 88 · 0%` with caption "Registration desk opens 14:00 · no one marked attended yet" (arrived = server `attended` flags; the app never writes attendance in v1). "Confirmed · old / new" — one bar per gender: total, `+n server`, old/new split with percentages. "Seating requests · not assigned" table (Chowky/Chair/Backrest × M/F) renders `—` dashes until the server sends data. **Do not build** the seating/teacher action buttons shown at the mock's foot — Vertical 2.

### 4.8 Settings (`⚙`)
TEST MODE explainer card (sandbox; status changes not sent; strip stays on every screen). Theme selector Light/Dark (persisted). Rows: Signed in (`registrar.giri · Dhamma Giri`), Sync status (`Last synced 2 min ago` / `Offline · 2 changes queued`), App version (`1.0.0 (v1 · vertical 1)`). **Log out** (wipes Room, outbox, session).

## 5. Domain model (`:core:model`)

Value classes: `CentreId`, `CourseId`, `ApplicantId`, `ConfNo`. `ApplicantStatus(value: String)` — open string with known-literal enum fallback for badge tones. `Gender` M/F. `ApplicantCard`: id, centreId, courseId, given/middle/family name, gender, status, type (Student/Sevak), oldStudent, attended, confNo?, email?, mobile?, phoneHome?, city/state/country, dob?, age?, monk/robed, createdAt, photoUrl?, history? (`first`, `recent`, `counts: List<Pair<String,Int>>`), flags: `List<AuditFlag>`. `AuditFlag(severity: HARD|SAFETY|SOFT, label, detail, ruleId)`. `Course`: id, name, start, end, typeKey. `Session`: user, displayName, centres, modeTest. `OutboxOp.ChangeStatus(applicantId, status, letterId=0, comment)` with `pending/synced/failed`. Conf-no semantics for display only: `{N|O|S}{M|F}` + number.

## 6. Network contract

**Existing Drupal endpoints (reuse as-is):**
- `POST /api/user/login` → `{sessid, session_name, token, user}`; cookie + `X-CSRF-Token` from `/services/session/token` on every subsequent POST. Persist in Encrypted DataStore.
- **`GET/POST /change-status/{appId}?s={status}&l={letterId}&c={comment}`** → `{status: "OK"|"Failed", msg, confno, newstatus}` — **this is the status write path** (owner decision; no façade). Send `l=0` always in v1.

**New `/staff` routes (mock-first; keep `docs/openapi-staff.yaml` in sync):**
- `GET /staff/session` → user, displayName, centres[], modeTest — `TODO(server): users + dh_user_center`
- `GET /staff/centres/{cid}/courses?upcoming=1` → unfinalized only — `TODO(server): dh_course where c_finalized=0`
- `GET /staff/courses/{id}/applicants?status=&q=&cursor=` → whitelisted rows + status counts — `TODO(server): search.inc WHERE + whitelist SELECT`
- `GET /staff/applicants/{id}` → full public card incl. `history` (`TODO(server): dh_applicant_course ac_*`) and server-side `flags` (§7)
- `GET /staff/meta/statuses` → `TODO(server): dh_type_detail`
- `GET /staff/applicants/{id}/photo` (auth'd image) — `TODO(server): a_photo S3 stream`
- `GET /staff/courses/{id}/photo-review` → suggestion classes per applicant — `TODO(server): new`
- `POST /staff/applicants/{id}/photo` `{rotate, crop}` → resubmit-with-swapped-photo + drift report — `TODO(server): new; reuses application save path`

**Removed relative to older docs:** no `POST /staff/applicants/{id}/attended` (attendance deferred to Vertical 2), no status façade. Update `openapi-staff.yaml` accordingly.

**Mock server:** ships in `:core:network`, on by default in `debug`; `BuildConfig.BASE_URL` switches to real. Seed with the design's fixtures — 12 applicants from the HTML (Meera Deshpande NF128 Confirmed clean; Rakesh Iyer Pending, 3 hard flags, photo rot90; Ananya Bhosale NF131 Received, aadhar_masked, crop-suggest; Suresh Nair OM42 Sevak safety flag; Priya Chandrasekhar NF140 Expected age_dob_mismatch; Vikram Joshi OM17 Cancelled; Fatima Sheikh NF133 cross_course_duplicate, no-face photo; Devendra Kulkarni Pending soft shared_mobile; Lakshmi Menon NF136 Received rot180 auto; Arjun Patel Rejected; Sister Uma Rangan NF102 robed, name_title_prefix; Nikhil Rane NF144 Expected), chip counts `214/61/48/72/18/9/6`, courses `10-Day 20–31 Aug`, `Satipatthana 3–12 Sep`, `10-Day 16–27 Sep`. Mock `/change-status`: refuse with the Area-teacher message for one fixture (Rakesh), succeed with minted `NF129` otherwise — mirrors the design's demo script.

## 7. Audit engine (`:core:audit`)

Findings surface data-entry problems to the registrar — they never block anything. Two sources, merged and de-duplicated by `ruleId`:
1. **Client rules** (pure Kotlin, unit-tested, over whitelisted card fields only): `phone_prefix_invalid`, `missing_field` (emergency contact — requires the server to send an `emergencyPresent` boolean or the field itself; otherwise server-side), `age_dob_mismatch`, `name_title_prefix` (Sister/Brother/Mr/Mrs/Ms in name), `shared_mobile` (within the loaded course).
2. **Server rules** for anything needing data the client never receives: `id_missing`, `aadhar_masked` (detail arrives pre-masked, e.g. `XXXX XXXX 4417`), `cross_course_duplicate` — `TODO(server)`.

Severity mapping as in §3. Rule ids render in mono so they map 1:1 to the checks.

## 8. Offline & outbox

Room caches **one** course's worklist (SQLCipher). Stale-while-revalidate: Room is the source of truth for UI; refresh on course entry and pull-to-refresh. Status changes enqueue as outbox rows, flush FIFO when online; **server wins** — after each flush re-fetch the applicant and show a snackbar if the result differs from the optimistic value. Failed flush keeps the row queued with the server message. Wipe DB + outbox on logout and centre switch. Photo geometry edits persist on device until uploaded. Session expiry (401/403 from Drupal) → back to Login with a verbatim message.

## 9. Testing & acceptance

Unit: status-request mapping (`s/l/c` params), conf-no display parse, audit rules table-driven, outbox flush + server-wins reconciliation, `ApplicantStatus` unknown-value fallback. Compose UI: login → courses → today; search filters list; chip multi-select; change-status success shows minted conf no; change-status refusal shows exact server text; queued-offline row styling. Screenshot-compare Today and Card (light + dark) against the design.

**Done =** on a phone and a ≥600 dp tablet, with the mock server: sign in → pick course → see counts/chips → search → open Rakesh Iyer (3 audit flags) → attempt status change → refusal verbatim → open Meera Deshpande → Confirmed flow with conf no → review/rotate/upload photos → day summary numbers correct → settings theme + TEST MODE strip → offline toggle shows cached list + queued badge → logout wipes.

## 10. Build order (commit after each; keep tests green)

1. Gradle multi-module skeleton, version catalog, `:core:model` + theme tokens in `:core:ui` (fonts, palettes, status tones).
2. `:core:network`: DTOs, Retrofit, cookie/CSRF plumbing, mock server with all fixtures.
3. `:core:datastore` + `:core:database` (entities, DAOs, outbox, wipe-on-logout).
4. `:feature:auth` + `:feature:course` (login, courses, session).
5. `:feature:applicants`: Today (chips/search/list/skeleton/offline) + card (audit panel, history, facts) + change-status sheet end-to-end against mock.
6. `:core:audit` client rules + `:feature:photos` + `:feature:summary` + `:feature:settings`; outbox flush; UI tests; README (base URL config, mock vs real, `TODO(server)` inventory).

## 11. Out of scope (do not build)

Attendance writes and `/app-update-attended`; Day-0 arrival marking; seating/room/cell/teacher assignment (the summary's action buttons in the mock); conf-no scanning/CameraX; letter admin or body preview; add/edit application form; AT portal/LC review screens; `/api` APP-API endpoints (`get-app-detail` is banned); WebView or HTML parsing; any client-side permission/tenancy logic; SMS/WhatsApp/IVR anything.

## 12. Deliverables

Building `org.dhamma.dipi.staff` app (assembleDebug green), mock server + fixtures, updated `docs/openapi-staff.yaml`, `README.md`, test suite per §9, and a short `TODO-SERVER.md` listing every mocked route with the PHP function that must back it.

---
End of prompt.
