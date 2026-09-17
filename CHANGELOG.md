# Changelog

Single ledger of releases from 2.0.0. Each entry names `versionName` / `versionCode`
as set in `app/build.gradle.kts`. Design measurements for each release are in the
shipped-delta ledger of [docs/DESIGN.md](docs/DESIGN.md); owner rulings are in
[docs/DECISIONS.md](docs/DECISIONS.md). Backend PHP is unchanged in every release.

## [2.3.4] / 107 - 2026-09-17

Consolidation release. The app behaves exactly as 2.3.3.

### Removed

- Unreached code: `Modifier.blueprintMarks` (core:ui `Blueprint.kt`, the retired registration-mark renderer), `jsonPretty` (core:network `MockFixtures.kt`), `ChipFill` (feature:desk `BoardPane.kt`).
- Side branches `codex/desk-2.2`, `codex/theme-honesty`, `feat/dipi-v7`, `feat/room-name-type` (all already on `main` or behind it) and `dipi-v7` (never merged; preserved with its implementation package as tag `archive/dipi-v7`).
- Shipped dated plans, specs, handovers and performance reports under `docs/` (see the docs consolidation commit); two stale stashes.

### Changed

- `CHANGELOG.md` is the release ledger from 2.0.0; `docs/README.md` indexes every remaining doc; `CLAUDE.md` and `AGENTS.md` headers carry only the current version and binding rules.
- WhatsApp automation record moves to `docs/WHATSAPP.md`; the Java desktop twin set moves to `docs/java-desktop/`; open findings are collected in `docs/BACKLOG.md`.

## [2.3.3] / 106 - 2026-09-17

### Changed

- Desk New/Old treats sevak confirmation prefixes `sm`/`sf` as Old; gender comes from the second letter.
- THE ROLL cell and the New/Old filter share `ConfPrefix`, so the New filter no longer hides sevaks as unknown. Unparseable prefixes stay visible under All and hide under a specific filter.
- Teacher-list grouping, seating-plan positions, room occupancy and `roomKey` are unchanged.

Shipped in the same commit as 2.3.1 and 2.3.2.

## [2.3.2] / 105 - 2026-09-16

### Changed

- Course report FROM/TO fields are 168x48 with a 12dp gap and DD-MM-YYYY kickers. RUN is 48dp tall and disabled while TO is before FROM.
- Presets This month / This year / Last 12 months sit on the filter row, wrap under 900dp, fill both fields from the device calendar and run.
- Reversed range shows the inline message `TO is before FROM. Swap the dates or pick a preset.` (Light `#FBF0EF` / `#DFAFAB` / `#7A2B26`; night blush in Dark). Valid field border `#C99FB1`; ROLL TOTAL `#F3E2E8`.
- Table columns are fixed width; the grand-total bar is pinned with 12x26 padding. `TrainingTeachers` fills TR. Teacher names stay as the CSV returned them.
- PRINT and SHARE CSV keep their handlers. No new endpoint, no `?r=`.

## [2.3.1] / 104 - 2026-09-16

### Changed

- Centre Settings matches the two-card hall chart: Male and Female hall cards sit 1fr/1fr with an 18dp gap from 800dp and stack below.
- Stepper buttons are 48x48dp with a 56dp centred value and 12dp between rows. Check-in toggles are 48dp with an accessible name that includes what they turn on.
- Live Hall Settings from `GET /centre/{cid}/edit` still drive columns and the read-only lines under the cards. SAVE HALL LAYOUT remains the local depth override. No POST.

## [2.3.0] / 103 - 2026-09-16

### Changed

- A parsed room allocation plus not Left is checked in, even when the worklist `attended` flag is false. Left never occupies a room.
- Room numbers match inventory regardless of dash, space or case (`Mbk- 51` == `Mbk 51`) through `RoomAllocSync.roomKey` / `parseDeskRoom`. Room Chart cells, `deskOccupied` and Check-in free counts use that key.
- Room Chart blocks come from the acco-handler inventory (`GET /centre/{cid}/acco-handler`).
- Centre Settings shows live Hall Settings from `GET /centre/{cid}/edit` (same-hall, naming convention, Male/Female Main Plan columns, chowky columns, direction, chowky side, empty seats). Unset Room Chart wrap and seating columns use that plan. Combined hall is displayed; the app still draws two gender blocks.
- No new attendance POST. No `?r=`.

## [2.2.0] / 102 - 2026-09-14

### Changed

- The desk rail starts at 840dp (was 1100dp); list-detail splits stay at 1100dp. Compact detail has Back to list; under 840dp the phone flow and root sheet overlay remain.
- The Board names each count's derivation and shows last-day reconciliation guidance when the course is server-finalized or on its exact last calendar day. Dates never establish finalization or write attendance.
- ID values are masked until Reveal; reveal state is in memory only and resets on applicant, detail or session changes.
- Desk controls use 14sp text and 48dp target floors, overriding smaller examples in the supplied 2.2 HTML.
- Roll eligibility (owner 2026-09-13): confirmation-number holders with any active status stay on the roll; only Cancelled, Left, Rejected, Regret and Duplicate are excluded, case-insensitively. WaitList stays on the roll and is labelled as held.
- Scope stays per device: named scope, separate 48dp clear, `showing scoped of total`; rail counts remain unscoped.

### Fixed

- Scoped Check-in availability now includes occupancy by students outside the scope.

Verification: 888 tests green, debug and release assembly, 24 Pixel C pane checks (six destinations x two orientations x Steel Dark / Still Light). Design delta: [docs/design/desk-2.2/HANDOVER.md](docs/design/desk-2.2/HANDOVER.md).

## [2.1.1] / 101 - 2026-09-13

### Changed

- The desk roll drops Left the same way it drops Cancelled, Duplicate and Rejected (Left never occupies a room).
- The desk top bar shows the persisted gender / seniority scope as a chip (for example `Female · New`); tapping it clears both axes. Rail counts stay unfiltered totals.

Shipped in the same commit as 2.2.0.

## [2.1.0] / 100 - 2026-09-13

After a Pixel C stroll of 2.0.1 in Desk ops and Course ops, both orientations.

### Fixed

- Phone Day 0 summary tile opens the Board's Day 0 summary sheet; finalized courses keep the worklist screen, which now counts Expected with Confirmed (no more `0 = 0 + 0`).
- Zero Day no longer lists Cancelled, Duplicate, Rejected or Left as unattended.
- Applications empty copy says `No applications match these filters.` when a filter is on, instead of implying an unloaded worklist.
- Centre Settings refreshes `GET /centre/{cid}/acco-handler` on open so Accommodation matches Room Chart.

### Changed

- Course ops under 1100dp uses two-line teacher rows and a stacked student card.
- The centre dashboard lifts a running older course into a Teaching now / `Day n · last day` band using the course-name window.
- Switching to Course ops lands on Teacher list instead of Settings.
- The desk day chip reads the course-name window when `start` is blank; Settings Last synced shows a relative age instead of a raw ISO instant.
- `deskWide` stays 1100. Photo review and WhatsApp stay off.

## [2.0.1] / 99 - 2026-09-11

### Fixed

- Dark renders the documented Steel night palette across Settings, course, applicant and desk surfaces while keeping the saved Light skin for the next Light render.
- Shared surface roles replace fixed light fills where surfaces render in Dark; skin swatches stay true colour and are labelled as applying in Light. Four Board caption contrasts fixed.
- No transport or persistence change. Exported documents are unchanged.

## [2.0.0] / 98 - 2026-09-10

### Fixed

- Move desk HTML parsing, row preparation and observed-roll audits off the main thread; audit results use immutable disclosure snapshots, and cancelled or previous-course work cannot publish into the current desk.
- Reuse worklist parser regexes without changing parsed results.
- Stop the desk minute clock while backgrounded or screen-off; show current time on return.
- Skip the Day 0 summary request for finalized courses (shares the existing Zero Day exclusion).
- Guard delayed processing against session/course changes while retaining Sign-in on session expiry.

### Measured

- Baseline taken on Pixel C at 1.46.2 / 96 (process CPU, UID bytes, `gfxinfo`, idle) on 2026-09-10; the release run on 2026-09-11 repeated course, sheet, refresh, Course ops, centre idle and 90-second return checks with mixed results. Exact endpoint timing and method attribution need a separately authorized profiling build. No tablet speedup or battery saving is claimed. Open follow-ups are in [docs/BACKLOG.md](docs/BACKLOG.md).

Version 2.0.0 / 98 is the owner-selected release number; the unpublished 1.46.3 / 97
preparation is superseded. Photo review remains off, ordinary photos remain available,
the 20-minute keep-alive pair and request order (worklist, finalized decision, optional
Zero Day) are unchanged, and no v7 UI is included.

## Before 2.0.0

Releases 1.4.1 through 1.46.2 are recorded per version in the shipped-delta ledger of
[docs/DESIGN.md](docs/DESIGN.md) and as git tags (`v1.35.0` … `v1.46.2`). Their dated
plans, specs and handovers were retired from the tree on 2026-09-17 and remain in git
history at `c829e17`.

[2.3.4]: https://github.com/kapaggar/dipi-app/compare/c829e17...main
[2.3.3]: https://github.com/kapaggar/dipi-app/compare/v2.3.0...c829e17
[2.3.2]: https://github.com/kapaggar/dipi-app/compare/v2.3.0...c829e17
[2.3.1]: https://github.com/kapaggar/dipi-app/compare/v2.3.0...c829e17
[2.3.0]: https://github.com/kapaggar/dipi-app/compare/v2.2.0...v2.3.0
[2.2.0]: https://github.com/kapaggar/dipi-app/compare/206bdf2...v2.2.0
[2.1.1]: https://github.com/kapaggar/dipi-app/compare/206bdf2...v2.2.0
[2.1.0]: https://github.com/kapaggar/dipi-app/compare/v2.0.1...206bdf2
[2.0.1]: https://github.com/kapaggar/dipi-app/compare/v2.0.0...v2.0.1
[2.0.0]: https://github.com/kapaggar/dipi-app/compare/v1.46.2...v2.0.0
