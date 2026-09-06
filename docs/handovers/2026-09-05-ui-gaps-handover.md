# Handover — close the four UI gaps over shipped code

For the implementing agent. **Plan first, then build.** Four milestones, ordered,
each independently shippable. Two of them need an owner answer before any code
is written; those questions are in § Open questions and they are blocking.

**Baseline:** `1.38.0` / `versionCode` 63 on `main`, clean tree, suite green.

**Read first, in this precedence:** `AGENTS.md` (Hard rules and Current
assumptions — these win over everything below) → `docs/DESIGN.md` (visual
authority and the shipped-delta ledger, § "Sheets v5" and § "Reach v5 UI") →
`docs/LIVE-DESK.md` Part 4 (sheet routes and markup skeletons) →
`docs/DECISIONS.md` (owner rulings) → this file.

**What this pass is not.** No new endpoint, no new query parameter, no new write
protocol, no backend change. Every milestone here is UI over transport and
parsers that already ship and already have tests. If you find yourself adding a
`@GET` to `StaffApi.kt`, stop — you have left the scope.

---

## How this list was produced

Every `StaffRepository` function was traced through `DeskViewModel` to a UI
caller. The ViewModel exposes 111 functions and **all 111 are wired** — there
are no orphaned actions. The gaps below are all downstream of that: data that
is fetched, parsed, and in two cases persisted, but that no composable draws.

---

## M1 · The phone cannot open five sheets it already fetches

**MINOR → `1.39.0` / `versionCode` 64.**

### The gap

`DESK_ACTION_PLACEHOLDER` — *"This control is wired to the desk path;
implementation is the next slice."* — catches five hub tiles on the phone:
**Day 0 List, Seating Plan, Student Chit, Checking Slip, Teachers List**.

For all five the transport, the parse, the injected stylesheet and the print
path already ship. The only missing piece is a mount point:

- `sheetView` is a field on `DeskUiState` (`DeskViewModel.kt:312`), **not** a
  `DeskScreen` value. `openSheet` (`DeskViewModel.kt:875`) populates it
  regardless of screen size.
- `SheetViewerPane` is mounted once, at `DipiAppUi.kt:658`, inside `DeskHost`.
- `DeskHost` is only reached when `deskWide` — `screenWidthDp >= 1100`
  (`DipiAppUi.kt:98`, branch at `:369`).

So on a phone the state is set and nothing draws it. `hubSheetLabel`
(`DeskTiles.kt`) documents this as deliberate and deliberately narrow: it maps
only the three `SheetRoute.Document` exports (Laundry list, Valuable list,
Course summary), because `Page` exports resolve to HTML that only
`SheetViewerPane` can draw.

### The trap — a test actively forbids this

`CourseHubScreenTest.hubSheetLabelsAreAllDocumentRoutes` (line 165) asserts that
every HTML `Page` export stays `null` in `hubSheetLabel`, with the comment
*"fetch HTML nothing draws"*. **That invariant is the thing M1 changes.**

Per the project rule, do not quietly rewrite a passing test to make new code go
green. Rewrite it only once the owner has answered Q1, and rewrite it to pin the
*new* invariant (every `Page` export resolves to a viewer the current window size
can actually draw) rather than deleting it. Say so explicitly in the commit.

### What to build

Hoist `SheetViewerPane` out of `DeskHost` so it overlays at the `DipiAppUi`
level, the way `openDoc` already does. The pane is already a full-screen
`Column` with `fillMaxSize()`, so it does not assume the desk frame — but it was
drawn for 1280dp and the sheets are 10- and 13-column tables. That is Q1.

Keep `nativeHall` / `nativeHallPrintHtml` wired for the tablet path; the phone
Seating Plan tile should reach the **native** hall (`HallBody` + 5i print), not
the `/seating` HTML, consistent with 1.36.0.

### Files you own

`app/.../ui/DipiAppUi.kt` (mount point and routing only),
`feature/course/.../DeskTiles.kt` (`hubSheetLabel`),
`feature/desk/.../SheetViewerPane.kt` (responsive chrome only — not the
stylesheet, not the transport).

### Tests

`CourseHubScreenTest` (rewrite the invariant, see above), `SheetViewerTest`
(add a narrow-window case), `DeskActionScreenTest` (the placeholder's remaining
users shrink — update the count, do not delete the test).

---

## M2 · `backrest` is parsed, persisted, and never shown

**MINOR → `1.40.0` / `versionCode` 65.**

### The gap

`TeacherListParser.parseSeat` (`TeacherListParser.kt:144`) returns a per-student
backrest flag. It lands on `RollRow.backrest` (`TeacherRoll.kt:79`) and is
persisted in `CourseOpsStore` (`RowDto`, line 268). It has parser test coverage
(`TeacherListParserTest.kt:90-100`). **No composable renders it.**

`DaySummaryPane` shows the *aggregate* (`DaySummaryPane.kt:319`, `BACKREST` with
old/new counts). So a registrar can see that three people need a backrest and
has no way to find out who they are — on a day where the answer decides how the
hall gets laid out before 07:00.

This is the only genuine feature hole in the list. The other three are routing
and hygiene.

### What to build

Surface the per-student flag on the surfaces that already draw a seat:

- `TeacherListScreen` — the `SEAT` column (`:406` header, `:524` cell). Note
  `SeatW = 76.dp` (`:65`): a marker must fit inside that width without
  reflowing the table. Do not widen the column without checking the other
  twelve.
- `StudentCardScreen` — it already draws `seat` three times; the flag belongs
  beside it.
- `SeatingPlanScreen` hall cell and `seatingPlanPrintHtml` (5i) — the print is
  monochrome, so a colour-only treatment will not survive the printer.

`docs/DESIGN.md` is the visual authority; take the token and glyph from there
rather than inventing one. If the design file has no backrest treatment, that is
Q2.

### Files you own

`feature/teacher/.../TeacherListScreen.kt`,
`feature/teacher/.../StudentCardScreen.kt`,
`feature/teacher/.../SeatingPlanScreen.kt`,
`feature/desk/.../SeatingPrint.kt`.

### Tests

`TeacherListScreenTest`, `StudentCardScreenTest`, `SeatingPlanScreenTest`,
`SeatingPrintTest` — all four exist and all four already reference `backrest`
fixtures. Extend them; the parser tests need nothing.

---

## M3 · `SeatKind` is a second implementation of one rule

**PATCH → `1.40.1` / `versionCode` 66.** Internal; no user-visible change.

### The gap

`TeacherListParser` classifies each seat into `SeatKind.CELL / CHAIR / FLOOR`
by prefix (`TeacherListParser.kt:148-150`). `SeatGrid` then **re-derives the
same classification from the raw seat string**, ignoring the enum:

```
SeatGrid.kt:108  chowkySeats = chowkyChair.filter { it.row.seat.startsWith("CW-", ignoreCase = true) }
SeatGrid.kt:111  chairSeats  = chowkyChair.filter { it.row.seat.startsWith("CH-", ignoreCase = true) }
```

`SeatKind` reaches no UI at all. Two spellings of one rule, and the seat-string
one is the one that ships.

### Pick a direction, then apply it once

Either collapse toward the enum (make `SeatGrid` read `it.row.seatKind`;
`PlacedSeat` already carries the row) or delete `SeatKind` and keep prefix
derivation. **Recommended: keep the enum as the single source and have
`SeatGrid` read it** — the parser already normalises case and handles the
unparseable case, and the vertical rail's ordering rule (`CW-A1` nearest the
Dhamma seat) still needs the trailing number off the string either way.

### Persistence is not a blocker either way

If you delete the field: `CourseOpsStore` builds its `Json` with
`ignoreUnknownKeys = true` (line 172) and decodes inside
`runCatching { … }.getOrNull()` (line 116). Existing encrypted snapshots that
carry `seatKind` will decode cleanly without it. No migration, no wipe. Confirm
this with a `CourseOpsStoreTest` case that decodes a fixture containing the
retired key.

### Files you own

`core/model/.../SeatGrid.kt`, `core/model/.../TeacherRoll.kt`,
`core/network/.../TeacherListParser.kt`,
`core/datastore/.../CourseOpsStore.kt`.

### Tests

`SeatGridTest`, `TeacherListParserTest`, `CourseOpsStoreTest`. The rail ordering
(`CW-A1` at the Dhamma seat) is pinned in `SeatGridTest` — that behaviour must
not move.

---

## M4 · Bulk Mail — decision-gated, DO NOT IMPLEMENT

`DeskTiles.kt:55` ships `DeskTileSpec("Bulk Mail", "centre/$centreId/bulk-mail-schedule")`
with no `action` and no `sheet`, so it opens the placeholder. There is **no
transport behind it** — no route in `StaffApi.kt`, nothing in the repository.
It is a label and a promise.

It is also adjacent to a family the owner already retired: Manage Courses, Daily
Activity, SMS Report and **Letters** went 2026-08-30. And AGENTS.md hard rule 14
(bridge rule) puts letters, waitlist and SMS/WhatsApp dispatch behind the desk's
own `_change_status` as black boxes — never reimplemented, never previewed.

Building a bulk-mail flow would cross that rule. Do not design one, do not
propose one. The owner's choice is Q3 and it is between retiring the tile and
relabelling it honestly. Implement whichever answer comes back, nothing more.

---

## File ownership map

| Milestone | Files |
|---|---|
| M1 | `app/.../ui/DipiAppUi.kt`, `feature/course/.../DeskTiles.kt`, `feature/desk/.../SheetViewerPane.kt` |
| M2 | `feature/teacher/.../{TeacherListScreen,StudentCardScreen,SeatingPlanScreen}.kt`, `feature/desk/.../SeatingPrint.kt` |
| M3 | `core/model/.../{SeatGrid,TeacherRoll}.kt`, `core/network/.../TeacherListParser.kt`, `core/datastore/.../CourseOpsStore.kt` |
| M4 | `feature/course/.../DeskTiles.kt`, possibly `feature/course/.../DeskActionScreen.kt` — after Q3 only |
| Docs | `docs/DESIGN.md` (append the delta), `AGENTS.md` + `CLAUDE.md` (version line only) |

Tests live in `:app`, `:core:model`, `:core:network`, `:core:datastore`,
`:core:audit` **only**. Feature modules have no test source set, so every
Compose screen is covered by Robolectric tests in `:app`.

---

## Never touch

- **The backend.** `/Users/wizops/DIPI/dipi-web` is read-only reference. No PHP
  change, no new route, no `/staff/*`.
- **What the client sends.** No new endpoint, no new POST, no new query
  parameter. The sheet-GET allowlist stays exactly `conf` and `seating`.
- **`r` on any sheet GET.** Ever. `SheetRouteSafetyTest` fails the build if it
  becomes constructible — do not weaken it.
- **Status writes** (`/change-status/{id}?s=&l=0&c=`), and never `Approved`.
- **NPI persistence.** Nothing in Room, DataStore, a DTO, or a log line. M1
  widens where sheet bodies are *drawn*; it must not widen where they are
  *stored* — HTML stays in memory, documents in `cacheDir/sheets`, all wiped on
  logout / session expiry / erase-all.
- **WebView hardening.** JavaScript stays off. Cookies stay out of
  `CookieManager`. M1 puts sheets on a phone; the hardening does not relax
  because the screen got smaller.
- **Server messages verbatim.**
- **Retired surfaces.** Manage Courses, Daily Activity, SMS Report, Letters.
- **Existing tests.** Do not edit a test to make new code pass without saying so
  and getting authorization. M1 requires exactly one such change
  (`hubSheetLabelsAreAllDocumentRoutes`) and it is called out above.

---

## Open questions — blocking, answer before writing code

1. **(M1) Does the phone get the full v5 sheet chrome?** The sheets are 10- and
   13-column tables designed for 1280dp. Options: (a) the same chrome, tables
   scroll horizontally; (b) a reduced chrome — print and close only, no sort
   segmented control, no column chips; (c) a phone-specific default column set
   per sheet. This decides whether M1 is a mount-point move or a responsive
   redesign, and therefore whether it is one slice or three.
2. **(M2) Is there a backrest treatment in the design file?** If
   `docs/DESIGN.md` and `DIPI-Staff.dc.html` have none, the owner picks the
   glyph and where it sits, given `SeatW` is 76dp and the 5i print is
   monochrome.
3. **(M4) Retire Bulk Mail, or relabel it?** Retiring drops the tile the way
   Letters was dropped. Relabelling keeps it as an honest desk-site link with a
   sub-line saying it opens the desk site, replacing the "next slice" promise.
   No third option — building it crosses the bridge rule.

Do not silently pick a default on any of these. Flag them, get the answer.

---

## Verify

Full green suite (JVM modules use `:test`, Android modules `:testDebugUnitTest`):

```bash
./gradlew :core:model:test :core:audit:test \
  :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest \
  :app:testDebugUnitTest
```

**Never run `./gradlew test` or `:app:test` at the root.** It drags in
`:app:testReleaseUnitTest`, where every Robolectric Compose test dies with
"Unable to resolve activity … ComponentActivity" — `androidx.ui.test.manifest`
is `debugImplementation`, so the release variant has no test activity. That
failure is the build config, not your code.

Build and install:

```bash
./gradlew :app:assembleDebug
export ANDROID_HOME=/Users/wizops/Android/Sdk
export PATH="$ANDROID_HOME/platform-tools:$PATH"
adb connect 10.0.0.144:5555
adb -s 10.0.0.144:5555 install -r -d app/build/outputs/apk/debug/app-debug.apk
adb -s 10.0.0.144:5555 shell am start -n org.dhamma.dipi.staff/.MainActivity
```

Bump `versionName` + `versionCode` in `app/build.gradle.kts` **before**
assembling, once per milestone. Do not bump while only writing docs.

### What to check by hand

The unit suite cannot see any of this.

- **M1** is a phone feature, so check it on a phone window — but install on the
  Pixel C too and confirm the tablet desk path is unchanged: the viewer still
  overlays the rail, `vm.back()` still closes the sheet before the `DeskScreen`
  back stack, the native hall and 5i print still work from the Board.
- **M1** — open all five newly-reachable sheets on the phone. No dead control
  anywhere, and no sheet that renders as an unreadable smear of columns.
- **M2** — a course with at least one backrest request in each gender. Confirm
  the marker appears in the teacher list, the student card, the hall, and the
  printed 5i page, and that the teacher-list table has not reflowed.
- **M3** — the chowky/chair rail still draws bottom-to-top with `CW-A1` nearest
  the Dhamma seat, and an existing device with a saved course-ops snapshot still
  restores its roll after the field change.

---

## When you finish

Append the delta to `docs/DESIGN.md` so the next design pass does not re-propose
what you landed. Update the version line in `AGENTS.md` and `CLAUDE.md`. Record
the answers to Q1–Q3 in `docs/DECISIONS.md` with their date — they are owner
rulings, and the next agent needs to know they were asked and settled.
