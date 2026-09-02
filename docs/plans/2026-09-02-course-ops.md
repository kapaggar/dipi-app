# Course Ops (Assistant-Teacher Mode) Implementation Plan

> **For agentic workers:** execute task-by-task via the dynamic multi-agent
> workflow (parallel scoped workers, strict file ownership, scoped tests,
> integrator per wave). Each task's full brief is its spec — read it AND
> `docs/DESIGN.md` § "Course ops" (measurements + ground-truth corrections)
> before touching code. The spec wins over this summary.

**Goal:** A second device mode. After day 0 the tablet flips to *Course ops ·
teacher*: two destinations (Teacher list, Seating plan) and a read-only student
card that shows what each applicant wrote on their application — from two
existing desk GETs, with zero server change.

**Architecture:** One `tabletMode` DataStore key swaps the start destination and
hides every desk surface. One `GET /teacher-list/{cid}/{courseId}` per entry
feeds both teacher screens (seat order derives client-side — the endpoint
mutates server data on GET, so never poll). `GET /application-view/{id}` is
prefetched for the whole roll (≤4 concurrent) into an encrypted device-local
course store (owner amendment 2026-09-02) keyed to the running course, which is
resolved by parsing dates out of the course *name*. A device PIN (set when the
mode is enabled) gates Settings and the switch back.

**Tech Stack:** existing app stack; new `:feature:teacher` module for the three
screens; parsers in `:core:network`; pure models in `:core:model`.

**Specs:**
- 2a `docs/specs/2026-09-02-course-ops-2a-mode-spec.md`
- 2b `docs/specs/2026-09-02-course-ops-2b-teacher-list-spec.md`
- 2d `docs/specs/2026-09-02-course-ops-2d-student-card-spec.md`
- 2c `docs/specs/2026-09-02-course-ops-2c-seating-spec.md`

## Global Constraints

- **Two existing GETs only.** No `/staff/*`, no new JSON contracts, no writes,
  no client ACL. Course ops is read-only end to end (owner: no attendance).
- **NEVER send an `r` query param** on any desk GET; the new calls declare
  `@Path` params only (no `@Query` anywhere).
- `GET /teacher-list` runs a server-side DELETE on every request — fetch once
  on entry to course ops / process start; no polling, no pull-to-refresh.
- **The teacher-list Comments column is never parsed or stored** (unlabelled
  health text). Flags derive from `/application-view` fields only.
- **Course-ops persistence amendment (owner, 2026-09-02):** roll + application
  answers (health included) MAY persist on-device for the running course —
  encrypted at rest (its own EncryptedSharedPreferences file), wiped when the
  resolved course changes, on Erase-all, and on logout. Never logged; all
  models keep redacting `toString()`. Nothing course-ops touches Room.
- FLAGS (`HLTH MED INTOX TECH PREG MONK`) mean only "something is written
  here" — never severity, never a colour code. Never truncate/summarise/rank
  an answer; unanswered questions render as `NO` (or `N/A`) with no body.
- Never re-sort or re-group the roll — page order is meaning.
- Design authority: `docs/design/DIPI-Staff.dc.html` turn 2 + `docs/DESIGN.md`
  § Course ops incl. its ground-truth corrections (frames+server beat prose).
  Tokens via `LocalDipi`/`Industry`, fixed hexes only where DESIGN.md says so;
  ≥48dp targets; rows 52dp; seat cells 58dp.
- No desk rail, no queued strip, no desk destination while course ops is on.
  Offline strip reuses `SyncBannerStrips(offline, queued = 0)`.
- Tests: the full green suite command in AGENTS.md; NEVER bare `./gradlew test`.
  Never weaken an existing test; each spec names its authorized retargets.
- Commits: plain messages, no agent attribution of any kind.
- SemVer per wave, integrator-only bumps; Pixel C install after each wave.

## File-ownership matrix

| Surface | 2a | 2b | 2d | 2c |
|---|---|---|---|---|
| `SessionStore.kt`, new `CourseOpsStore` | **W1** | | | |
| `SettingsScreen.kt`, PIN dialog | **W1** | | | |
| `DeskViewModel.kt` / `DipiAppUi.kt` | **W1** | W2* | W2* | W3 |
| `core/model` (CourseDates, TeacherRoll, mode) | **W1** (dates/mode) | **W1** (roll) | W2 (card) | W3 (grid) |
| `core/network` parsers + `StaffApi` + mocks | | **W1** | **W2** | |
| `:feature:teacher` module (new) | | **W1** | **W2** | **W3** |
| `CentreOpsScreen` (hall-grid config) | | | | **W3** |

*W2: 2d touches VM/DipiAppUi — single worker after W1 lands.

## Waves

- **Wave 1 — 2a ∥ 2b** (disjoint: 2a owns settings/store/VM-mode; 2b owns
  parser/module/roll models; the thin VM/DipiAppUi wiring for 2b's screens is
  done by the integrator from both branches or sequenced last inside 2b with
  W1a already merged). Version **1.31.0 / 52**, install.
- **Wave 2 — 2d** (application-view parser, course store persistence, prefetch
  pipeline, flags onto 2b's rows, student card + ‹› group walk). **1.32.0 / 53**,
  install.
- **Wave 3 — 2c** (hall-grid config in Centre Settings, seating screen,
  cell/pagoda + unseated) + docs/DECISIONS updates + polish. **1.33.0 / 54**,
  install, merge to main, release with stable `dipi-staff.apk` asset.

Each wave: workers run scoped tests → gate review against the spec by a
separate reviewer → integrator runs the full suite, bumps SemVer, builds
`:app:assembleRelease`, installs on `10.0.0.144:5555`.

## Owner decisions recorded (2026-09-02)

1. Exit gate = **device PIN**: 4 digits, set at enable time, salted-hashed in
   EncryptedSharedPreferences, wiped by Erase-all; gates entering Settings from
   course ops (which also covers Logout/Erase — otherwise logout would reset
   the mode key and bypass the gate).
2. Hall grid = **registrar-configured device-locally** (per gender: rows ×
   seats-per-row) in Centre Settings; seat labels place students into the grid.
3. Prefetch = **all applications on entry, ≤4 concurrent**, persisted per the
   amendment above so the hall reads offline across restarts.
4. **Read-only** — no attendance marking.
