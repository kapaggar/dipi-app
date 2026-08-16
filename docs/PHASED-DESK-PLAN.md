# DIPI Staff — full desk, phased

**Chrome (1.4.3):** 1.4.3 implements hub tile set (audit + calling, no group seating), status-sheet hierarchy, Zero Day UI, rooms page, centre ops toggles + accommodation add. Hall name Main Dhamma Hall. A–F+K have a home on the Centre dashboard and Course hub. Live tiles: course picker, View Applications, Photo review, Audit applications, Calling students, Zero Day, Day 0 summary, Centre settings (ops), app Settings. Other in-scope tiles are visible and open a desk-path placeholder. G–J stay off the primary chrome.

**Date:** 2026-08-15
**Decision:** Do not hide in-scope desk functionality. Cover A–F and K in validate-ready phases. **Out of scope for now (later):** G letters, H LC review, I AT schedule, J referral.

**Zero Day options (centre settings):** Laundry, valuables, and groups are optional. Default: groups off (Main Dhamma Hall, no group UI). If groups are enabled, Zero Day shows group chips. Laundry off hides the laundry field. Special seating is always Chowky / Chair / Backrest / None as large buttons.
**Current step:** organize display screens and controls around the in-scope desk APIs (hub first, then wire).
**App:** `/Users/wizops/DIPI/dipi-app`
**PHP (read-only):** `/Users/wizops/DIPI/dipi-web/sites/all/modules/dh_manageapp`
**Layout inventory:** `docs/DESK-LAYOUT-FOR-ANDROID.md`

Nothing in this plan invents `/staff/*` or new PHP. Each phase talks to routes that already exist. NPI (`aadhar`, `passport`, `voterid`, `pancard`) never persists. `Approved` / `R-ATReview` are never sent from the status sheet until a later LC-specific phase that implements the desk’s LC rules exactly.

---

## Scope now vs later

| Now (organize UI + implement) | Later (do not build yet) |
|---|---|
| A core (find, card, status, photo read) | G letters + status letter picker |
| B centre dashboard + course hub | H LC Approved / AT-review on worklist |
| C worklist parity | I AT schedule + assign teacher |
| D add/edit application + photo write | J referral + center-referral |
| E Zero Day | |
| F seating + print lists | |
| K centre ops (manage courses, settings, reports, bulk mail, daily activity, advanced search) | |

Hub tiles for G–J are omitted from the primary chrome until those phases start. Do not hide A–F or K.

---

## How a phase is “validate-ready”

A phase ships only when all of these are true:

1. **One registrar job** is complete end-to-end on the Pixel C (or emulator ≥600dp), against the live desk HTML/JSON the PHP already emits.
2. **Parser / request tests** use checked-in synthetic fixtures shaped like `search.inc` / `_change_status` / the relevant `.inc`. No live login in CI. No real student rows.
3. **Desk tablet walkthrough** written in the phase’s “Validate” section: which PHP page it mirrors, which buttons must work, which errors must show verbatim.
4. **SemVer + versionCode** bumped. Install on the desk tablet.
5. **No silent hiding.** If a desk control is not in this phase, it is visible as “later” (disabled with a label, or listed on the course hub as coming), not removed. Exception: NPI fields stay out of storage and off the card forever unless a later phase is explicitly about ID documents.

---

## Spine (what the desk actually is)

```
Login → Centre dashboard → Course hub → Worklist + card + status
                              ├─ Add / edit application
                              ├─ Zero Day (attend, room, laundry, seat type)
                              ├─ Seating / lists / PDFs
                              ├─ Assign teacher
Centre dashboard also: Letters, AT schedule, Referral, Reports, Bulk mail, Daily activity, Centre settings, Manage courses
```

Android v1.4.1 already does Login → Courses → Today → Card → Status, plus stub Day 0 and mock Photo review. That is the start of Phase A, not the product.

---

## Phase A — Desk-faithful registrar core

**Job:** Find someone, read the public card, change a status, see the photo the desk already has.

**Keeps (do not remove):** Photo review screen, Day 0 summary entry, Settings. Photo upload stays on screen but is marked not-yet-wired if the only desk path is the full edit form.

**Build**
- Parser completeness for public `$rs` keys (see inventory §3.1). Drop NPI in the parser.
- Today = `GET /search-course/{cid}/{courseId}?s=&t=&g=&d=a` + `var dataset`. Local q on name, conf no, phones, email.
- Chips from statuses in the dataset. Never offer Approved.
- Card from cache + public keys + `show-photo/{aid}`. Referral as a flag only in this phase.
- Status: `GET /change-status/{id}?s=&l=0&c=`, parse `{status,msg,confno,newstatus}`, snackbar `msg` verbatim. Re-fetch worklist after OK.
- Course dates from centre dashboard HTML. `LocalDate.now()` for “starts in N days”.
- Mock `/staff/*` may remain behind a flag until Phase A is green on the live desk; then delete it. Do not let mock define the UI.

**Validate**
- Synthetic: login form, dashboard course links, dataset with public keys only, change-status OK/Failed JSON.
- Tablet: sign in → pick upcoming course → search a name → open card → photo loads if `photo` set → change Received→Confirmed → conf no from server → logout / remember-me / erase.
- Fail if NPI appears in Room, logs, or UI.

**Out until later:** letter picker (`l≠0`), LC Approved, transfer/delete/add, edit form, Day 0 writes.

---

## Phase B — Centre dashboard + course hub

**Job:** The app lands where the desk lands, not on a stripped “Today” as the only home.

**Build**
- Centre dashboard: module link list (same `$modules` as `dh_manage_centre`), course picker, received/upcoming count tables linking into filtered worklists.
- After picking a course: **course hub** (`dh_course_dashboard`) — View Applications, Add Application, Assign Teacher, Day 0 List, Zero Day, lists, seating, PDFs, course summary crosstab.
- Links that are later phases stay visible, labeled later / open the stub screen. Do not hide them.
- No client ACL. Send the request; show the server’s 403/HTML verbatim.

**Validate**
- Hub shows the same action names as `dh_course_dashboard` `$modules`.
- Tapping View Applications is today’s worklist. Tapping a later tile does not crash and does not pretend the feature is done.
- Count cells pass `s,t,g,at` into search-course the way `course_summary()` does.

---

## Phase C — Worklist parity

**Job:** Today is the desk DataTable, not a subset that throws away filters and row actions.

**Build**
- Filters: `s` (multi), `t` old/new, `g` M/F, `at` Student/Sevak, `d=a`.
- Row: name, eocd chips that are public (monk, referral, sevak) — not medical/NPI chips.
- Expand / card depth: clarifications (`/app-clarifications/{aid}`), other courses (`/app-courses/{aid}`), activity (`/app-activity/{aid}`), LC reco/area names (not approve), emergency contact, discourse lang, friend-family. Still no medical, street, NPI.
- Row actions the desk has: Transfer (`/move-to-course` + `/get-courses`), copy-to-course, Add Referral, Delete, Send Review Email. Each is a real call with verbatim errors.
- Application PDF (`show-application/{aid}`) and clarification file as read.
- Excel export only if we can do it without dumping NPI columns.

**Validate**
- Same course, same `s/t/g/at` query as the desk URL, same row count (public fields).
- Each action: success path + a forced server error shown verbatim.
- NPI still absent from storage.

---

## Phase D — Add / edit application + photo write

**Job:** The big form. This is how the desk adds people and changes photos. Do not invent a side-door upload API.

**Build**
- `app/add/{cid}/{courseId}` and `app/{aid}/edit` — `dh_ma_applicant_form` field map from `inc/application.inc` / zero-day router.
- Photo change goes through that form, the same way the desk does. Photo review screen becomes: browse `show-photo`, rotate/crop locally, submit via the edit form, then re-read.
- Autocomplete: country/state/city/pincode/teacher as the desk uses them.

**Validate**
- Add one synthetic fixture form POST shape from PHP. On tablet: edit a non-sensitive field, save, card matches. Photo replace, card photo updates. Failed validation shows Drupal messages verbatim.
- This phase is large. Split internally (identity/contact first, then course/history, then photo) but do not ship “done” until a full add and a full edit both work.

---

## Phase E — Zero Day

**Job:** Arrival desk. Unattended / attended / mark attended.

**Build**
- `zero-day/{cid}/{courseId}` — `dh_zero_main`.
- Unattended list, attended list, `#day-summary` tables (confirmed / attended / special seat / groups).
- Mark-attended dialog: attending, room section/number, laundry, valuable, cell, group, special seating (Chowky/Chair/BackRest), comments → `POST /app-update-attended/{aid}`.
- Add applicant from Zero Day (reuses Phase D).
- Finalized course: show the desk error, no writes.

**Validate**
- Tablet walkthrough on a non-finalized upcoming course: mark one attended with room + chowky, lists and summary update from the server response. Undo is “update again”, not a client invention.

---

## Phase F — Seating and print lists

**Job:** Hall and paper.

**Build**
- Seating plan read (`seating/{cid}/{courseId}`), then regen `?r=1`, then drag-save `seating-update`. Group-wise seating the same.
- Lists: day0-list, teacher/manager/cell/laundry/valuable, student chit, checking slip, day11, male/female PDF.
- Native canvas if the INI grid is tractable; otherwise a contained WebView of the existing HTML. Prefer native for the grid once the INI parser is trusted. Do not hide the feature behind “v2 someday”.

**Validate**
- Read-only seating matches desk for one course (seat names, old/new, special seat marks).
- Regen and one drag-save round-trip, then confirm on the desk page.
- Each print/PDF opens and is readable on the tablet.

---

## Phase G — Letters and status-letter picker

**Job:** Templates, then the status sheet can choose `l` like the desk.

**Build**
- `letters/{cid}` CRUD + letter-fields editor (`inc/letters.inc`).
- Status sheet: if multiple letters exist for the event, force letter + comment before confirm (desk JS in `search.inc`). `l=0` only when the desk would send 0.
- Still never send Approved until Phase H.

**Validate**
- Create/copy/delete/restore a template on a sandbox centre.
- Status change that has two letters: sheet blocks until one is picked; applicant gets that letter (confirm on desk activity / letter log, not by guessing).

---

## Phase H — Long-course AT review on the worklist

**Job:** The desk’s LC status dropdown, including Approved, with the same server rules.

**Build**
- Implement the exact `_change_status` LC branches in the **UI choices only** (server still decides). Offer Approved / Rejected-R-AT / Rejected-A-AT / R-AT Review only when the desk JS would.
- Failed Area-teacher / recommending-teacher messages verbatim.
- Send Review Email (`send_at_email`).

**Validate**
- Fixtures for each LC branch (OK and Failed).
- Tablet: a row that the desk shows as `NA` is `NA` in the app. A row the desk allows Approved can send it. Never send Approved on a normal 10-day row.

---

## Phase I — AT schedule

**Job:** `at-schedule/{cid}` + `assign-teacher/{cid}/{courseId}`.

**Build**
- Recent received AT apps, upcoming/completed course teacher tables.
- Change AT status/type/group, add conducting/assisting, trainee teacher, delete trainee.
- AT info popup, AT activity.

**Validate**
- Assign one AT, change Received→Confirmed, see them on the upcoming table. Finalized course blocked as on the desk.

---

## Phase J — Referral

**Job:** AT referral list and centre referral list.

**Build**
- `referral/{cid}` and `center-referral/{cid}` list + expand + add/edit/delete + readonly flag.
- Card referral block from dataset (`referral`, reason, dates) — still no NPI in storage. Desk expand shows ID fields; the app must not persist them even if the HTML contains them. Display strategy: omit ID numbers (product rule), keep reason/dates/listed-for.

**Validate**
- Add a referral without ID numbers, see it on the list, open from an applicant row. Delete/readonly match the desk.

---

## Phase K — Centre operations

**Job:** Everything else on the centre dashboard.

**Build**
- Manage courses editor (`manage-course/{cid}`).
- Centre settings + acco/hall INI (`centre/{cid}/edit`, acco-handler).
- Daily activity, SMS report, course report, bulk mail schedule (list/edit/mute/logs).
- Advanced search `search-app/{cid}` (the big filter form), then the same worklist.

**Validate**
- Each tile: one read path and one write path (where the desk has a write), verbatim errors, no invented APIs.

---

## Cross-cutting (every phase)

- Session: cookie jar, `GET /user/login` POST, keep-alive `/services/session/token` + `/centre`, logout.
- No client ACL. No status engine. Server messages verbatim.
- No NPI in Room, logs, or analytics.
- Design file `docs/DIPI Staff.dc.html` still wins visual arguments for screens that exist there; new screens follow the same tokens (Barlow / Condensed / Plex Mono, steel `#5980A6`).
- `dipi-web` stays immutable.
- Do not crawl the live host to “discover” routes. PHP is the contract.

---

## Suggested order (dependencies, not speed)

A (core) → B (hub) → C (worklist) → D (forms/photo write) → E (zero day) → F (seating/print) → G (letters) → H (LC) → I (AT) → J (referral) → K (centre ops)

B can start as soon as A’s session + course parse are solid. D is the longest. F may take a full vertical by itself. Do not skip A’s NPI/parser bar to “get to Day 0 faster”.

---

## What we are explicitly not doing in any phase

- New PHP, `/staff/*`, Services login, APP API, `get-app-detail`.
- Hiding Photo review, Day 0, seating, letters, AT, or referral “to ship sooner”.
- Storing Aadhaar / PAN / passport / voter id.
- Implementing AT portal / VRI teacher dashboards (`center-dashboard`, `vri-management`, `search-lc`) unless a later product decision adds those roles.
