# UI Gap Closure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development
> (recommended) or superpowers:executing-plans to implement this plan task-by-task.
> Steps use checkbox (`- [ ]`) syntax for tracking. Each task's full brief is its
> spec file — read the spec BEFORE touching code; the spec wins over this summary.

**Goal:** Close every implemented-but-unreachable gap found in the 2026-08-31 scan
of `main` @ `0ce3342` (1.27.0/42): surface the Day-11 export on the tablet, show
ID/health info on the phone card, make the Centre Course-Report chip real, feed
the phone Zero Day into the real check-in/sync pipeline, use the desk's own
status vocabulary, port applicant desk history from `feat/desk-gap`, fix
unrecorded v4 design drift, then sweep dead code and refresh the living docs.

**Architecture:** Eight independent specs executed in five sequential waves;
inside a wave, workers run in parallel ONLY where their file sets are disjoint.
`DeskViewModel.kt` and `DipiAppUi.kt` are the contention hot-spots — never give
them to two live workers at once. An integrator closes every wave: full suite,
SemVer bump, slim release, Pixel C install where required.

**Tech Stack:** Kotlin / Jetpack Compose, Robolectric JVM tests, Retrofit +
OkHttp against the live Drupal desk (`https://dipi.vridhamma.org`), DataStore,
Gradle 8.9.

**Specs (one per task, same directory):**
- T1 `docs/specs/2026-08-31-day11-board-chip-spec.md`
- T2 `docs/specs/2026-08-31-phone-sensitive-info-spec.md`
- T3 `docs/specs/2026-08-31-status-vocabulary-spec.md`
- T4 `docs/specs/2026-08-31-course-report-chip-spec.md`
- T5 `docs/specs/2026-08-31-zero-day-checkin-bridge-spec.md`
- T6 `docs/specs/2026-08-31-applicant-history-port-spec.md`
- T7 `docs/specs/2026-08-31-v4-drift-polish-spec.md`
- T8 `docs/specs/2026-08-31-dead-code-and-docs-spec.md`

## Global Constraints

Every task inherits these verbatim; a worker that needs to break one returns
BLOCKED instead of improvising.

- Backend PHP is immutable: no `/staff/*`, no change to `dipi-web`, no new endpoints.
- No client-side access control; render server responses verbatim.
- Never send status `Approved`; no status engine in Kotlin (strings only).
- Never persist or log NPI (`aadhar`, `passport`, `voterid`, `pancard`, `ae_*`,
  health disclosures) — display-only in-memory `SensitiveInfo`.
- **NEVER send an `r` query param on any sheet/desk GET** — its mere presence
  triggers server-side bulk seat auto-allocation.
- Colors via tokens only (`LocalDipi` is dark-aware; `Industry` is skin-aware but
  NOT dark-aware — never use `Industry` on a phone/dark surface). No inline hex.
- ≥48dp touch targets on every new/changed interactive control.
- Design authority: `version-4/DIPI Staff v4.dc.html` wins every visual argument;
  measurements per `version-4/README.md` (1px = 1dp).
- Tests: green suite is `./gradlew :core:model:test :core:audit:test
  :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest
  :app:testDebugUnitTest`. NEVER bare `./gradlew test` (`:app:testReleaseUnitTest`
  cannot resolve `ComponentActivity`).
- Never weaken an existing test to pass; each spec names the assertions it is
  authorized to retarget — anything else breaking means STOP and report.
- Commits: plain messages with no agent attribution of any kind — no attribution
  trailers, no tool watermarks, no session URLs.
- Workers persist work incrementally (edit owned files as they go) and keep a
  progress note in `.superpowers/sdd/2026-08-31-ui-gap-closure/progress.md`.
- SemVer per wave (hard rule 11): bump `versionName` + `versionCode` in
  `app/build.gradle.kts` — integrator only, never a worker.

## File-ownership matrix (contention control)

| File | T1 | T2 | T3 | T4 | T5 | T6 | T7 | T8 |
|---|---|---|---|---|---|---|---|---|
| `BoardPane.kt` (+test) | **W1** | | | | | | W3 | |
| `CardScreen.kt` | | **W1** | | | | W4 | | |
| `ApplicationsPane.kt` | | | | | | **W4** | | |
| `DeskViewModel.kt` | | **W1** | | W2 | W2* | W4 | | |
| `DipiAppUi.kt` | | **W1** | | W2 | W2* | W4 | | |
| `StaffRepository.kt` | | | **W1** | | | W4 | | |
| `DeskTiles.kt`, `CentreScreen.kt` | | | | **W2** | | | | W5 |
| `ZeroDayScreen.kt` (+test) | | | | | **W2*** | | | |
| `DeskShell.kt`, `CheckInPane.kt`, `SyncBanners.kt`, `DeskStyle.kt` | | | | | | | **W3** | |
| `StaffApi.kt`, mocks, `core/model`, `core/ui` new files | | | | | | **W4** | | W5 |

*W2: T4 and T5 both touch `DeskViewModel`/`DipiAppUi` → ONE worker takes both
specs in wave 2, sequentially (T4 first — smaller).

## Waves

Each wave: dispatch workers with their spec path + this plan's Global
Constraints; every worker runs scoped tests before committing; a reviewer
(separate agent) gate-reviews each task diff against its spec; integrator then
runs the FULL suite, bumps the version, builds `:app:assembleRelease`, installs
on the Pixel C (`10.0.0.144:5555`) when the wave column says so, and commits the
bump.

### Wave 1 — three parallel workers (disjoint files)

- [ ] **W1a = T1** Day-11 Board chip. Owns `BoardPane.kt`, `BoardPaneTest.kt`,
      the R2 note in `docs/specs/2026-08-28-v4-design-pass-spec.md`.
      Scoped test: `./gradlew :app:testDebugUnitTest --tests '*BoardPaneTest*'`
- [ ] **W1b = T2** Phone sensitive info. Owns `CardScreen.kt`, `DipiAppUi.kt`,
      `DeskViewModel.kt` (openCard only), new `CardSensitiveTest.kt`.
      Scoped test: `--tests '*CardSensitiveTest*' --tests '*TodayScreenTest*'`
- [ ] **W1c = T3** Status vocabulary. Owns `StaffRepository.kt` (line 294 region
      only), new `StatusVocabularyTest.kt`.
      Scoped test: `--tests '*StatusVocabularyTest*'`
- [ ] Gate review each of the three diffs against its spec (reviewer agent).
- [ ] Integrator: full suite → bump **1.28.0 / versionCode 43** → release build →
      Pixel C install (tablet-facing: T1) → commit → merge/push per owner's
      standing flow.

### Wave 2 — one worker, two specs in sequence

- [ ] **W2 = T4 then T5** (shared `DeskViewModel`/`DipiAppUi` ownership).
      T4: Course Report chip (`DeskTiles.kt`, `CentreScreen.kt`, VM, `DipiAppUi`,
      `CentreScreenTest`). Commit T4 before starting T5.
      T5: Zero Day bridge (`ZeroDayScreen.kt`, VM, `DipiAppUi`,
      `ZeroDayScreenTest`).
      Scoped tests: `--tests '*CentreScreenTest*'` then `--tests '*ZeroDayScreenTest*' --tests '*RoomSyncTest*'`
- [ ] Gate review both diffs.
- [ ] Integrator: full suite → **1.29.0 / 44** → release → Pixel C install
      (Centre screen is registrar-facing) → commit.

### Wave 3 — one worker

- [ ] **W3 = T7** v4 drift polish (`DeskShell.kt`, `CheckInPane.kt`,
      `SyncBanners.kt`, `DeskStyle.kt`, `BoardPane.kt` P4 — after T1 is merged).
      P6 (radius ramp) LAST within the task, full suite immediately after it.
- [ ] Gate review with the design file open — every value re-checked against
      `dc.html` line numbers cited in the spec.
- [ ] Integrator: full suite → **1.29.1 / 45** (visual polish = PATCH) → release
      → Pixel C install (mandatory — P6 repaints everything) → owner eyeball
      before merge.

### Wave 4 — one worker (widest surface, solo)

- [ ] **W4 = T6** applicant history port from `feat/desk-gap` (via `git show`
      only — never checkout the branch). Steps 1–7 in spec order; compile + green
      scoped tests after each step; one commit per step.
      Scoped tests: `:core:network:testDebugUnitTest --tests '*ApplicantHistory*'`
      then `--tests '*ApplicantHistoryScreenTest*'` plus the untouched-surface
      canaries `--tests '*CourseHubScreenTest*' --tests '*AdvancedSearchScreenTest*'`
- [ ] Gate review; explicit check: nothing from the spec's OUT-of-scope list
      (server-side search, dead screens, wider `hubSheetLabel`) crept in.
- [ ] Integrator: full suite → **1.30.0 / 46** → release → Pixel C install →
      commit.

### Wave 5 — one worker, runs last

- [ ] **W5 = T8** dead-code sweep + `SHIPPED-DELTA.md` / `AGENTS.md` /
      `CLAUDE.md` refresh. Re-grep every symbol before deleting (waves 1–4 may
      have consumed one). One commit per deletion batch, suite per batch.
- [ ] Gate review: confirm the KEEP list survived and no test was weakened.
- [ ] Integrator: full suite → **1.30.1 / 47** → release build (no install
      needed unless the owner asks) → commit, merge to `main`, push, cut the
      GitHub release with the final APK.

## Decision-gated items — NOT in any wave, owner input required

1. **Server-side Advanced Search** (`feat/desk-gap`): re-adding `POST /search-app`
   reverses the deliberate deletion in `0dd0c71` and contradicts AGENTS.md's
   do-not-assume list. Needs HAR verification + owner authorization first.
2. **Real photo upload / review**: the live desk exposes no upload route; the only
   write surface is the full `/app/{id}/edit` form (highest-risk slice, NPI-bearing,
   owner sign-off required) and no pixel re-encode pipeline exists. Product
   decision, not a wiring task.
3. **Centre-screen metric drift + login/wash items** (T7's NOT-list): owner
   on-device comparison before anyone "fixes" them.

## Self-review record

Spec coverage: every scan finding maps to a task (T1–T8) or the decision-gated
list. Placeholder scan: specs carry concrete code, exact anchors, and named
tests; the two look-ups left to workers (soft-accent token in T2, rail-fill token
in T7) are bounded instructions with explicit fallbacks, not TBDs. Type
consistency: `deriveStatuses` (T3), `openCourseReport` (T4), `patchRecord` /
`setZeroDaySeat` / `toggleZeroDayLaundry` / `toggleZeroDayValuables` (T5),
`records: Map<ApplicantId, CheckInRecord>` (T5), `ApplicantHistory.kt` types
(T6) are each defined once and referenced with the same names throughout.
