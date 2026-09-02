# UI Gap Closure — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Close the eight remaining UI/transport gaps on the shipped Vertical 2 desk without merging `feat/desk-gap`, without adding a 13th Board-shelf chip, and without reversing the 2026-08-30 catalogue retirements.

**Baseline:** `main` at **1.27.0 / versionCode 42**. `feat/desk-gap` is a read-only donor via `git show` (still 1.19.0 / 30). Do not merge it.

**Specs:**
- T1 `docs/specs/2026-08-31-day11-board-chip-spec.md`
- T2 `docs/specs/2026-08-31-phone-sensitive-info-spec.md`
- T3 `docs/specs/2026-08-31-status-vocabulary-spec.md`
- T4 `docs/specs/2026-08-31-course-report-chip-spec.md`
- T5 `docs/specs/2026-08-31-zero-day-checkin-bridge-spec.md`
- T6 `docs/specs/2026-08-31-applicant-history-port-spec.md`
- T7 `docs/specs/2026-08-31-v4-drift-polish-spec.md`
- T8 `docs/specs/2026-08-31-dead-code-and-docs-spec.md`

## Global Constraints

- Backend PHP is **immutable**. No `/staff/*` on the live host. No edits under `/Users/wizops/DIPI/dipi-web`.
- No client ACL. Render the server response verbatim.
- Never send status `Approved`. Status write stays `GET /change-status/{id}?s=&l=0&c=`.
- Never persist or log NPI (`aadhar`, `passport`, `voterid`, `pancard`, `ae_*`). Display-only `SensitiveInfo` is in-memory / session-scoped.
- **NEVER send `r` on sheet GETs.** Presence triggers server-side bulk seat auto-allocation.
- Tokens via `LocalDipi` / `Industry`. New phone surfaces use `LocalDipi` (dark-aware). Do not reuse tablet `IdVerificationBlock` / `HealthPanel`.
- Touch targets ≥ 48 dp on anything new.
- Only the assertions the spec's "Tests this invalidates" section names may be retargeted. Adding tests is always allowed.
- **Never** bare `./gradlew test`:

```bash
./gradlew :core:model:test :core:audit:test \
          :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest \
          :app:testDebugUnitTest
```

- Wide tests: `@Config(qualifiers = "w1240dp-h844dp-land")`.
- Commits: no `Co-Authored-By`, no `Claude-Session`, no `Generated with`. GPG on.
- SemVer on every shippable wave in `app/build.gradle.kts`.
- `feat/desk-gap` is **read-only**. T6 ports via `git show` only.

## File-ownership

`DeskViewModel.kt` and `DipiAppUi.kt` never have two writers in the same wave.

| Wave | Version | Tasks | Exclusive files |
|---|---|---|---|
| 0 | — | specs | `docs/plans/`, `docs/specs/` |
| 1 | 1.28.0 / 43 | T1 ∥ T3 | T1: `BoardPane.kt`, `BoardPaneTest.kt`. T3: `ApplicantStatus.kt`, `StaffRepository.kt`, narrow `ensureWorklist` |
| 2 | 1.29.0 / 44 | T2 | `CardScreen.kt`, `openCard`, DipiAppUi Card branch |
| 3 | 1.29.1 / 45 | T4 ∥ T7 | T4: `DeskTiles.kt`, `CentreScreen.kt`, `openCourseReport`, DipiAppUi Centre. T7: `DeskShell.kt`, `CheckInPane` sidebar, `DeskStyle.kt`, `SyncBanners.kt`, BoardPane kicker |
| 4 | 1.30.0 / 46 | T5 | `ZeroDayScreen.kt`, VM checkIns, DipiAppUi ZeroDay |
| 5 | 1.30.1 / 47 | T6 then T8 | T6: history port. T8: docs + leftover sweep |

## Integrator (I1–I5)

1. Guard: `git show feat/desk-gap:app/build.gradle.kts` still 1.19.0/30.
2. Full green suite (command above).
3. Bump `versionCode`/`versionName`; update `AGENTS.md` and `CLAUDE.md` version sentence only.
4. `./gradlew :app:assembleRelease`; report path, bytes, md5.
5. Pixel C `10.0.0.144:5555`: install, launch, dumpsys reports the pair. Unreachable = report, not failure.
6. Gate-review before the next wave.
7. Commit `feat: … at 1.x.y`.

## Parked (no wave)

1. Server-side Advanced Search (`POST /search-app`) — needs HAR re-verify + owner authorization.
2. Real photo upload — no live desk route.
3. Centre-screen metric drift (`cardRows` vs v4 frames) — needs on-device look.

See each spec for the Do / Tests / Never-touched lists.
