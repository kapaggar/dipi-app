# Handover — Sheets v5 implementation (2026-09-02)

Paste this whole file to the next agent. It is written to be read cold.

---

## 1 · Where the work is

| | |
|---|---|
| **Branch** | `feat/sheets-v5` |
| **Tip** | `docs: v5 close-out — ship at 1.34.0, progress ledger, design delta, handover` |
| **Version** | `versionName` **1.34.1**, `versionCode` **56** |
| **Base** | `main` at `5428052` — the course-ops merge. Sheets v5 sits **on top of** course-ops, not beside it. |
| **Plan** | `docs/plans/2026-09-02-sheets-v5.md` — § 0.5 is the progress ledger |
| **Design spec** | `version-5/README.md`; frames `5a`–`5t` in `DIPI Sheets v5.dc.html` |
| **Design ledger** | `docs/DESIGN.md` § "Sheets v5" — the do-not-re-propose list |

**Two wrinkles worth knowing before you read `git log`.**

1. During this run the main checkout at `/Users/wizops/DIPI/dipi-app` was on
   `feat/seating-r2` (which is `feat/sheets-v5` plus one unrelated docs commit for
   the follow-on seating spec), while `feat/sheets-v5` itself was checked out in a
   review worktree at `/private/tmp/review-v5`. T3 and T4 were committed in the
   main checkout and **cherry-picked** onto `feat/sheets-v5`, so both branches
   carry the work. Same content, different SHAs — that is expected, not a mistake.
2. A second agent was committing into the same working tree at the same time. On
   `feat/seating-r2` it swept the v5 documentation and the version bump into its
   own commit, `e4fcd1d` *"feat: course ops buffers the worklist…"*, whose message
   therefore does not describe half of what it contains. **On `feat/sheets-v5`
   those doc changes are a separate, correctly-titled commit.** Prefer
   `feat/sheets-v5` when reading this pass's history.

---

## 2 · What the pass was for

Twelve desk sheets arrive from the immutable Drupal desk as print-styled HTML and
render in a WebView with JavaScript off. Every `Columns:` pill, `Print` link,
in-sheet hyperlink and the entire seating drag-and-drop panel is **dead furniture
drawn at the same weight as the data** — the seating plan spent roughly 400dp of a
900dp screen on an instruction panel about dragging students, none of which works
here. This pass removed what cannot work, replaced it with native controls that
can, gave all six HTML sheets one table style and one header, and moved Course
report off the Board onto the centre dashboard as a native surface.

**No new product feature, no new write protocol, no new endpoint.**

---

## 3 · What changed, per task

### T5 · `( View )` at parse + Rooms rebalance — `1053937`

- `core/network/.../SearchPageParser.kt` — `PDF_SUFFIX` generalises to
  `LINK_REMNANT`, which strips `( PDF )` **and** `( View )` in any spacing or case.
  Stripping at parse means every screen is clean at once rather than each screen
  learning the same trick.
- `feature/desk/.../RoomsPane.kt` — block headers read *"N occupied · N free of N"*
  over an occupancy bar; free cells lose the word "free" and the accent and get a
  quiet fill; occupied cells keep the ellipsized name and the accent; amenity marks
  move to the cell's top-right under a legend in the pane header.
- **No write affordance was added.** `noWriteAffordanceExists` pins that.

### T1 · Sheet chrome + injected stylesheet — `87fa431`

- New `feature/desk/.../SheetStylesheet.kt`. Real CSS — the one place in this app
  where the design file's markup ships verbatim. It hides the dead furniture and
  gives the six HTML sheets one table style, and it defines the column chips.
- `feature/desk/.../SheetViewerPane.kt` — `SheetHeader` (title, course identity
  shown **once**, `VIEW ONLY` / `READ & PRINT` chip) and `SheetControlBand`
  (segmented sort that refetches; column chips that toggle a CSS class with **no**
  refetch). The stylesheet is injected at `loadDataWithBaseURL` time and never
  travels in the transport payload.
- `core/model/.../SheetExport.kt` — new `SheetSort` enum. It is the only route from
  a control to a query parameter and it knows exactly two names: `conf` and
  `seating`.
- `core/network/.../StaffApi.kt` — `sheetPage` takes nullable `@Query("conf")` and
  `@Query("seating")` and nothing else. No `@QueryMap`, no `@QueryName`.
- **JavaScript stays off.** `hardenForSheets()` is public purely so a test can call
  it on a bare `WebView`.

### T2 · Native Day 0 summary — `e9d1ac0`

- New `core/model/.../DaySummary.kt`, `core/network/.../DaySummaryParser.kt`,
  `feature/desk/.../DaySummaryPane.kt`; `SheetPayload.Summary` replaces the HTML
  payload for this one export.
- The parser reads **text**, not tags, because the desk emits unclosed `<b>` tags
  in this block. Three fixed tables; `(O)`/`(N)` pairs in special seating.

### T3 · Course report on the centre dashboard — `ca44361`

- `feature/course/.../DeskTiles.kt` — `DeskTileAction.CourseReport`; the desk-site
  "Course Report" chip becomes the fourth **native** tile, with a one-release `NEW`
  tag and a sub-line. Four native tiles, one chip (Bulk Mail).
- `feature/course/.../CentreScreen.kt` — action tiles are a **2 × 2 grid at 66dp**;
  the extra 14dp is what buys each tile its sub-line.
- New `feature/course/.../CourseReportScreen.kt` — `CourseReportUi` covers first
  open, running, loaded, empty range and refusal. Fourteen CSV columns render as
  five groups (NEW · OLD · ROLL TOTAL · SEVAK · TEACHERS) with the roll total
  banded; the grand total is a pinned **footer**, not a row.
- New `core/model/.../CourseReport.kt` and
  `core/network/.../CourseReportCsvParser.kt`. The parser is a **quote-aware
  scanner**, not a line split: the teacher column wraps across physical lines inside
  a quoted field, so splitting on `\n` silently truncates the report and invents
  blank courses. Unknown columns degrade to zero rather than reading the wrong
  figure into the wrong group; a course name that does not match
  `centre / type / year / dates` prints raw rather than as an error.
- `StaffApi.courseReport(cid, from, to)` scrapes the desk's own form, overrides
  only the two date fields, POSTs, and parses the CSV. The CSV still lands in
  `cacheDir/sheets` so `Share CSV` keeps working.
- **Nothing is fetched on open** — the desk rebuilds this report from scratch every
  time, so RUN is a deliberate act. **The range is the only control**, because the
  desk's form offers no course picker, status filter or sort.
- `DeskViewModel` gains `DeskScreen.CourseReport`, `courseReport` state,
  `openCourseReport` / `setReportFrom` / `setReportTo` / `runCourseReport` /
  `shareCourseReportCsv`.

### T4 · Board drops the chip — `6a608c9`

- `feature/desk/.../BoardPane.kt` — shelf 3 loses Course report and **keeps
  four-column width with the last cell empty**. Stretching three chips across four
  columns would make them read as more important than the eight above; the hole is
  honest.
- Chips de-emphasise: 38dp (was 40), fill `#FCFCFD`, border `#E7E7EA`, 13sp
  `neutral600` label, `accent300` glyph. Each shelf kicker gains a grey qualifier.
- Stat cards read as navigation through **one** 14sp `accent300` arrow — no button,
  no border change, roll sentence untouched.
- **Day-11 keeps its full-width fourth line**, tag `export-day11`, label
  `Course summary report`, and gains only an `END OF COURSE` qualifier.

### Docs

`docs/plans/2026-09-02-sheets-v5.md` § 0.5 (progress ledger + deviations),
`docs/DESIGN.md` § "Sheets v5" (shipped delta / do-not-re-propose), version
sentences in `AGENTS.md` and `CLAUDE.md`, and the shelf-count and `r`-guard
sentences in `AGENTS.md` assumption 13.

---

## 4 · What was **not** done

1. **Hall geometry — still unanswered, still blocking.** The design draws rows
   **A–E × 7 seats** and records that as *inferred, not observed*. Frames `5h`/`5i`
   are unbuilt. Only dead-furniture removal and the chrome shipped on seating.
   **Do not invent a hall grid.** The follow-on spec
   `docs/specs/2026-09-02-seating-r2-orientation-spec.md` picks this up.
2. **Chit density (9-up vs 12-up) and Contact Details on print** — owner questions
   3 and 4, unanswered. The shipped defaults are the design's own.
3. **`?seating=1` on the student chit** — `HAR-ROUTES.md` lists it as seen and safe,
   so it is in the allowlist. If the owner says the chit exposes order differently,
   remove `SheetExport.StudentChit` from `SheetSort.optionsFor` and nothing else
   changes.
4. **Pixel C install** — not done; the tablet was not reachable during this run.
   `AGENTS.md` requires it for a registrar-facing MINOR, so **this is outstanding**.
5. **No push, no merge to `main`** — the user did not ask for either.
6. **Release APK not cut**, so the twice-attached-APK release ritual in `AGENTS.md`
   hard rule 12 has not run.

---

## 5 · How to review

### Suite

```bash
./gradlew :core:model:test :core:audit:test \
  :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest \
  :app:testDebugUnitTest
```

**Green as of the tip: 337 tests, 0 failures.**

**Never run bare `./gradlew test` or `:app:test`.** It drags in
`:app:testReleaseUnitTest`, where every Robolectric Compose test dies because
`ui-test-manifest` is `debugImplementation` and `ComponentActivity` will not
resolve. That failure is the build config, not the code.

### The tests that carry the load

| Guard | Test |
|---|---|
| **No `r` on any sheet GET** | `SheetRouteSafetyTest.noSheetGetCanCarryAnRParam`, `.theAllowlistIsExactlyConfAndSeating`, `ExportMockTest.noSortValueCanEverProduceAnRParam` |
| JS stays off | `SheetViewerTest.javaScriptStaysDisabled` |
| Stylesheet is chrome, not payload | `SheetViewerTest.injectedStylesheetIsNotInTheTransportPayload` |
| Sort refetches, columns do not | `SheetViewerTest.sortSegmentRefetches`, `.columnChipTogglesWithoutARefetch` |
| Day 0 summary parses a real fragment | `DaySummaryParserTest` (whole class) |
| Wrapped teacher names do not split a course in two | `CourseReportCsvParserTest.aWrappedTeacherListDoesNotSplitOneCourseIntoTwoRows` |
| Server refusal verbatim | `CourseReportScreenTest.aRefusalPrintsTheServersOwnWordsUnchanged` |
| RUN is deliberate | `CourseReportScreenTest.firstOpenAsksForNothing` |
| Board is eleven chips | `BoardPaneTest.boardHasElevenExportChipsAndNoCourseReport` |
| Day-11 keeps its fourth line | `BoardPaneTest.day11KeepsItsFourthLineAndGainsAnEndOfCourseTag` |
| No write affordance in Rooms | `RoomsPaneTest.noWriteAffordanceExists` |

### By hand on the Pixel C

Install first — this has not been done:

```bash
export ANDROID_HOME=/Users/wizops/Android/Sdk
export PATH="$ANDROID_HOME/platform-tools:$PATH"
adb connect 10.0.0.144:5555
./gradlew :app:assembleDebug
adb -s 10.0.0.144:5555 install -r -d app/build/outputs/apk/debug/app-debug.apk
adb -s 10.0.0.144:5555 shell am start -n org.dhamma.dipi.staff/.MainActivity
```

Then check, in this order:

1. **Every HTML sheet** — no `Columns:` pill, no `Print` link, no live in-sheet
   hyperlink, one table style, one header, course identity shown exactly once.
2. **Seating plan** — the drag-and-drop instruction panel is gone; the chip reads
   `READ & PRINT`; nothing anywhere says or implies "regenerate".
3. **Print each sheet through the Android print dialog** — comments and health
   annotations must not be truncated. Chits especially: they get guillotined.
4. **Course report from the centre dashboard** — loaded, empty range, and a forced
   failure so you can see the verbatim server text with the request line beneath.
5. **Rooms & seats** — no `( View )` anywhere, on a course with both occupied and
   free blocks.
6. **Board** — eleven chips, no Course report, Day-11 still on its own fourth line,
   whole Board on one fold at 1280×900 with no scroll.

---

## 6 · How to upgrade or improve next

**Open, in rough priority order:**

1. **Install on the Pixel C and confirm the print path.** This is the one required
   step that did not run.
2. **Get the hall geometry answered**, then build frames `5h`/`5i` against the real
   grid. The follow-on spec is already written.
3. **Owner questions 3 and 4** — chit density and Contact Details on print. Both
   change a default, not a build.
4. **Take the `NEW` tag off the Course report tile** in the next MINOR. It is a
   one-release affordance; `centreDeskTiles` has a test that only one tile wears it.
5. **A date picker for the report range.** Today the range is two mono text fields.
   The desk's own form is a Drupal date widget; a native picker would be kinder, and
   nothing about the transport changes.

**Parked — do not re-scope without owner sign-off:**

- Server-side Advanced Search (`POST /search-app`, `DeskSearchFields`). Needs HAR
  verification.
- A seating editor / drag-to-reseat. Read + print only.
- `/group-seating/{cid}/{courseId}` and `/cell-list/{cid}/{courseId}`.
- Allocation-sync writes beyond the already-authorised
  `POST /app-update-attended/{id}`.
- Manage Courses, Daily Activity, SMS Report, Letters — retired by owner decision
  2026-08-30.

**Follow-up (verify run):** tip `281f51e` did not compile. `prefetchApplicationViews`
takes `onProgress` last, so a trailing lambda was typed as `(done, total)` and
`teacherCards + (id to card)` failed. Named `onCard =` in `DeskViewModel` and
`TeacherCardPrefetchTest`. Full suite re-run green in an isolated worktree.

**SemVer for the next pass:** `1.35.0` / `57` for a feature, `1.34.2` / `57` for a
fix. Bump both fields; never leave two installs on the same `versionName`.

---

## 7 · Hard rules — read before touching anything

- **Never send `r` on a sheet GET.** Its mere presence triggers server-side bulk
  seat auto-allocation. The allowlist is `conf` and `seating`, enforced by
  `SheetSort` and pinned by `SheetRouteSafetyTest`. That allowlist is the guard's
  whole defence — do not widen it on a guess.
- **JavaScript stays off in the WebView.**
- **Backend PHP is immutable.** No `/staff/*`. No new endpoint.
- **Render server messages verbatim.** No rewording, no client-side reading of a
  status code, no client ACL.
- **Never send status `Approved`.** No status engine in Kotlin.
- **Never persist or log NPI** (`ae_*`, Aadhaar, PAN, passport, voter id). Display
  is allowed in memory only (`SensitiveInfo`); Room, DataStore and DTOs are not.
- **Do not truncate comments or health disclosures on print.**
- **Day-11 keeps its full-width fourth-line row.** Status hexes stay. Touch targets
  ≥ 48dp.
- **Sheet bodies never outlive the session** — memory or `cacheDir/sheets` only,
  wiped on logout, session expiry and Erase-all.
- Commits: GPG on; **no** `Co-Authored-By`, no session trailers, no watermarks.
- Do not commit `local.properties`, keystores, `shots/`, HARs, or real student data.
