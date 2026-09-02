# v4 design pass — spec adoption and rulings

**Status:** proposed, 2026-08-28
**Baseline:** `main` at 1.22.0 / versionCode 35 (merge `bf29408`), installed on the Pixel C.
**Design handover:** `version-4/` — `DIPI Staff v4.dc.html` (851 lines, eight paired
before/after frames at 1280×900, 1 px = 1 dp), `README.md` (the full written spec),
`PROMPT.md` (scope + hard rules), `uploads/dipi-ui-export/SHIPPED-DELTA.md`
(do-not-re-propose list + token tables).

## Authority

**`version-4/README.md` is the binding visual spec for this pass**, adopted verbatim:
its per-frame sections (1a–1h), its "Interactions & behaviour", "State" and
"Design tokens" sections carry the exact dp/sp/hex values. This document does not
restate them; it records only where the code's reality demands a decision the
README could not know, plus what the pass deliberately does not do. On conflict:
README wins on look and measurement; existing code wins on architecture (per the
handover's own instruction "prefer changing layout and tokens over adding
composables"; when they disagree on something the README does not cover, follow
the existing code and flag it).

Scope = PROMPT.md's nine items, in its order: (1) centre 40% pane two columns,
(2) matrix legibility, (3) login keyboard-up, (4) check-in scan field,
(5) settings two columns + real controls, (6) dark = Steel night, said in the UI,
(7) fixed severity pair, (8) board densification, (9) queued strip + empty
older-courses reflow.

## Rulings — code reality the handover could not see

**R1 — The scan bug's real cause is ViewModel lifetime, not DataStore.**
The README says "scanQuery moves from persisted to session-scoped". `deskScan` is
NOT persisted — it is plain `DeskUiState` in the activity-scoped `DeskViewModel`
(`DeskViewModel.kt:143`), set only by `setDeskScan`, cleared never. It therefore
survives closing one course and opening another, which is exactly the reported
`NF24` symptom. Fix: `pickCourse` (`DeskViewModel.kt:382`) resets `deskScan = ""`
(and nothing else — gender/filter persistence is deliberate and untouched). The
README's optional "state 3" (restored-query strip) is NOT built: once the buffer
is session-scoped there is nothing to restore.

**R2 — The Day 11 open item has a known answer.** Transport shipped on `main`
at 1.27.0 (`SheetExport.Day11Report` → `GET /report-day11/{cid}/{courseId}`).
The Board chip shipped in UI-gap-closure T1 as the design file's **fourth-line
row** (`dc.html:579`) — not a 13th 3×4 shelf cell, and the dashed GAP badge
is never drawn. This v4 pass still does not place that row; T1 does.

**R2 correction (2026-08-31):** the stated reason ("lives on unmerged
`feat/desk-gap`") went stale when `0ce3342` cherry-picked the export. The Board
chip now ships on the design file's own fourth-line row
(`docs/specs/2026-08-31-day11-board-chip-spec.md`). The 3×4 shelf grid is
untouched.

**R3 — Applications/Rooms stay out of NEXT.** Deferred by the handover itself
("counts are inventory, not a queue"); nothing to build.

**R4 — The 3-tiles + 5-chips split maps 1:1 onto the existing catalogue.**
`DeskTileSpec.action != null` (Centre Settings, Advanced Search, App Settings) are
the three 48dp tiles; `action == null` (Manage Courses, Daily Activity, SMS
Report, Course Report, Bulk Mail) are the five 30dp desk-site pill chips under
`MORE ON THE DESK SITE`. `DeskTiles.kt` is unchanged; only `CentreScreen`'s
rendering changes.

**R5 — Settings keeps its callback signatures.** The segmented `Light | Dark`
control and the offline `Switch` drive the existing `onToggleTheme` /
`onToggleOffline` `() -> Unit` callbacks, invoked only when the selection actually
changes — no signature change, no `DipiAppUi` edit in that task. Two new data
needs (`appVersion`, and `queued` already exists as a param) enter as **defaulted
parameters**; the phase-3 wiring task supplies `appVersion` from the app module's
`BuildConfig.VERSION_NAME` at the call site.

**R6 — The fixed severity pair lands on the existing `hard` token.** Light
`hard` `#7A4141` → `#A33A34`; dark `hard` `#DEAEAE` → `#E0796F`. That token
already backs Erase-all and hard audit severity — exactly the "severity" the
README pins. `statusColors()`' fixed status hexes are untouched. `DarkDipi`'s
neutral/ground members move to the README's Steel-night ramp (`bg #14171A`,
`foreground #E4E6E9`, `tint #1D2D3D`, hairlines/fields from `#1A1E22 #22272C
#2E3339 #3A4046`, muted `#9BA1A8`, accent text `#B5D9FD` where the README says
so); `LightDipi` stays a function of the skin palette.

**R7 — The queued strip needs one new piece of state.** `lastSyncAttemptAt`
(an ISO instant or epoch ms in `DeskUiState`), stamped on every outbox flush
attempt — automatic reconnect flush and manual RETRY alike — and rendered as
"last try HH:MM" in mono. Not persisted; a fresh process shows the strip without
a last-try line until the first attempt. `SyncBannerStrips` already renders above
every screen (centre and desk), so placement needs no new scaffold work.

**R8 — IME behaviour uses insets, not a new screen.** The manifest already sets
`adjustResize`; the login card observes `WindowInsets.ime`/`isImeVisible` to swap
between the tall (~380×~430) and compact (~324dp-tall content) arrangements the
README draws. The lotus relief stays (`LoginLotusRelief`); no photo hero — that
was removed at 1.15.0 and sits on the do-not-re-propose list.

**R9 — Design-authority pointer moves to v4.** `AGENTS.md` hard rule 9 still
names `docs/DIPI Staff.dc.html`, two design generations stale. It changes to
`version-4/DIPI Staff v4.dc.html` + `version-4/README.md` (measurements). The
`version-2`/`version-3` bundles stay tracked as history.

## Tests this invalidates (retarget knowingly; anything else = stop and report)

- `SettingsScreenTest`: the `"Theme: Dark"` / `"Simulate offline: …"` TextButton
  strings become a segmented control and a `Switch` — retarget to the new
  semantics (`assertIsOn`/`assertIsOff`, segment selection), same states proved.
- `CentreScreenTest` / `CentreScreenWideTest`: assertions pinned to the single
  3-across tile grid and the matrix header row's exact node shape — retarget to
  the two-column lower pane (tiles + chips both still asserted reachable and
  firing the same callbacks) and the new header (group caps + same NM/OM/M/NF/OF/F
  labels).
- `LoginScreenTest`: layout-order assertions if any; behaviour assertions
  (error verbatim, remember-me, sign-in enabled) must survive unchanged.
- `DeskPanesTest`: check-in field assertions (placeholder, clear control appear);
  Board assertions pinned to old stat-card/NEXT geometry, if any.
- `SyncBannersTest`: strip geometry/text-structure assertions — the four-row
  offline/queued truth table and the RETRY-fires-callback assertions must
  survive with identical meaning.

Never touched: `centreSettingsRowIsReachableWithoutCourses`,
`countsLineDropsZeroesAndAbsentSummaries`, the accommodation read-only invariant,
`OlderCourseLimitTest`, `RoomLayoutTest`, `CourseMatrixTest`, all parser tests.

## Out of scope

Everything on PROMPT.md's hard-rules list and README's "Do not" list; the Day 11
export (R2); Applications/Rooms queue semantics (R3); any backend change; skin
ladder changes (1h is a proof frame, zero work); photo upload, bulk mail,
add-application, seating editor.

## Versioning

User-visible design pass inside the current vertical → **MINOR: 1.23.0 /
versionCode 36**. `feat/desk-gap` holds 1.19.0/30 — no collision. Registrar-facing
→ Pixel C install required (hard rule 12).
