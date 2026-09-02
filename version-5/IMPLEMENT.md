# IMPLEMENT.md — handover for the Claude Code agent

Read this **after** Claude Design has produced `DIPI Staff v5.dc.html` and its per-item spec. Read `PROMPT.md` (the brief Design worked from), `HAR-ROUTES.md` (transport), then `AGENTS.md` and `docs/DESIGN.md` in the repo.

Baseline: **1.30.5** / `versionCode` 51, on `main`, clean tree. This pass lands as a **MINOR** → `1.31.0` / `versionCode` 52. Bump both **before** assembling. Do not bump while only writing docs.

## The shape of the work

Ten items, but they collapse into five pieces of engineering. Land them in this order — each is independently shippable.

### T1 · Shared sheet chrome + injected sheet stylesheet (unlocks items 1–7)

Owner: `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/SheetViewerPane.kt`.

The viewer already owns the HTML string before `loadDataWithBaseURL`, so all of this is client-side rendering with **zero change to what we send**:

- One injected `<style>` block prepended to `SheetPayload.Html.html` at render time. It hides `.no-print` (the dead toolbars, the `window.print()` links, `remove-seat` / `remove-cell` / `store-seat-changes` / `helptext` / the row-and-column buttons) and restyles the tables onto Industry tokens. **Keep the transport verbatim** — inject in the UI layer, not in `SheetTransport.htmlPayload`, so `SheetPayload.Html` still carries exactly what the server said.
- Column-visibility chips drive the same injected stylesheet (`.d0-occ`, `.d0-contact`, `.d0-comments`, `.tl-cell`, `.tl-langs`, `.tl-comments`, `.ml-cell`). Toggling is a CSS rule change, not a refetch.
- Sort/order segmented controls **do** refetch, with `?conf=1` (Day 0 list) or `?seating=1` (teacher list, student chit). That needs a nullable query on the sheet-page call in `core/network/.../StaffApi.kt` (`SheetRoute.Page` / `sheetPage`). Keep it an explicit allowlist of parameter names.
- **Guard the `r` parameter in code, not just in review.** Add a test that fails if any sheet GET can be built with an `r` query param. `?r=1` triggers server-side bulk seat auto-allocation.
- Print stays the Android print framework (`createPrintDocumentAdapter`). Design's A4 rules become `@media print` inside the injected stylesheet.

Do **not** enable JavaScript in the WebView. The hardening (`javaScriptEnabled = false`, no cookies, no DOM storage, no file/content access) is deliberate — these pages carry health disclosures and contact data.

### T2 · Day 0 summary as a native surface (item 2)

The `#day-summary` fragment is nine numbers and six short strings. If Design's answer is native (it should be), this needs a small parser beside `AttendedTableParser` in `core/network` — three fixed tables (`#table-conf`, `#table-totals`, `#table-special`), a model in `core/model`, and a pane in `feature/desk`. **This is the only item that may need a new parser.** It is an HTML parse of a page we already fetch: no new endpoint, no new permission.

Tests go in `core/network` (parser, including the malformed unclosed-`<b>` cells) and `:app` (the pane, Robolectric).

### T3 · Course report native surface (item 8)

Transport already exists and needs no change: `SheetExport.CourseReport` → `SheetRoute.ReportForm` → `CourseReportFormParser` scrape + POST → CSV in `cacheDir/sheets`. `DeskViewModel.openCourseReport()` already fetches without an open course.

What changes:

- `feature/course/.../DeskTiles.kt` — `Course Report` moves from the desk-site chip row (where it currently sits with `sheet = "Course report"`) to a **native tile** beside `Centre Settings`: add a `DeskTileAction` case and drop the `sheet` field. `centreDeskTiles` currently returns three action tiles + two chips; it becomes four action tiles + one chip (`Bulk Mail`). `CentreScreenTest` and `CentreScreenWideTest` both assert the current split — update them, don't delete them.
- `feature/course/.../CentreScreen.kt` — the tile row is `tilesPerRow = 3` on wide and `1` on narrow. Four tiles need a reflow decision; take Design's.
- A new screen (`feature/course` or `feature/summary`, your call — follow whichever already hosts `CentreOpsScreen`) that renders the CSV natively: parse the 14 columns, render the table, grand-total row, date range, and the loading / empty / **verbatim refusal** states. `Share CSV` keeps the existing `openDoc` path.
- The CSV parse must tolerate teacher names wrapping onto continuation lines — records are not one-per-physical-line. Test that with a fixture.
- The date range is the only input the server accepts (`report_from_date[date]`, `report_to_date[date]`). Do not invent filters.

### T4 · Board: drop the Course report chip (item 9)

Owner: `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/BoardPane.kt`.

`EXPORT_SHELVES` shelf 3 is `"FOR THE TEAM" to listOf("Teacher list", "Manager list", "Valuable list", "Course report")`. Remove `"Course report"` and apply Design's shelf-3 resolution. **Keep the Day-11 fourth-line row exactly as it is** — a solid full-width chip below the 3×4 grid, `testTag("export-day11")`, emitting the label `"Course summary report"`. It is not a 13th grid cell and the dashed "GAP" marker is dead history.

`SheetExport.CourseReport` stays in the enum — the Course report surface still uses it, and the phone hub's `hubSheetLabel` mapping is unaffected. Board tests count export chips; update the counts.

### T5 · Rooms & seats (item 10)

Two separate fixes; do not conflate them.

- **`( View )` is a parser bug, not a UI string.** `RoomsPane.kt` renders `occupant ?: "free"` with no decoration — the text arrives inside `displayName`. Fix it where the sibling artefact is already stripped: `core/network/.../SearchPageParser.kt` has `PDF_SUFFIX = Regex("""\s*\(\s*PDF\s*\)""", IGNORE_CASE)` applied in `mapRow`. Add `View` to that treatment (a shared `LINK_REMNANT` regex covering `PDF` and `View`, same spacing/case tolerance). Fixing it at parse time cleans every screen at once, which is why the PDF case was done there. Extend `displayNameDropsThePdfLinkRemnantInAnySpacingOrCase` in `SearchPageParserTest` rather than writing a parallel test.
- **The visual rebalance** is `RoomsPane.kt` only: cell hierarchy, free-cell treatment, amenity legend, block header weight. Keep `PULL FROM SERVER` and the `SYNC N TO SERVER` accent button (hidden at N=0) exactly as wired. **Add no write affordance** — no per-cell assign, no drag-to-assign. Allocation sync is a separate, already-authorised, user-initiated POST and is out of scope.

## File ownership map

| Item | Files you own |
|---|---|
| Shared sheet chrome, all HTML sheets | `feature/desk/.../SheetViewerPane.kt`; nullable sort param in `core/network/.../StaffApi.kt` (`SheetRoute.Page`, `sheetPage`) |
| Day 0 summary native | new parser in `core/network`, model in `core/model`, pane in `feature/desk` |
| Course report | `feature/course/.../DeskTiles.kt`, `feature/course/.../CentreScreen.kt`, new report screen, `app/.../DeskViewModel.kt` (state only) |
| Board | `feature/desk/.../BoardPane.kt` |
| Rooms | `feature/desk/.../RoomsPane.kt`, `core/network/.../SearchPageParser.kt` |
| Docs | `docs/DESIGN.md` (append the v5 delta), `AGENTS.md` + `CLAUDE.md` (version line only) |

Tests live in `:app`, `:core:model`, `:core:network`, `:core:datastore`, `:core:audit` **only**. Feature modules have no test source set, so every Compose screen is covered by Robolectric tests in `:app`.

## Never touch

- **The backend.** `/Users/wizops/DIPI/dipi-web` is read-only reference. No PHP change, no new route, no `/staff/*`.
- **What the client sends.** No new endpoint, no new POST, no CSRF handling beyond the existing form scrape. The only new query parameters allowed are `conf` and `seating`.
- **`r` on any sheet GET.** Ever.
- **Status writes.** `/change-status/{id}?s=&l=0&c=` unchanged; never send `Approved`; no status engine in Kotlin.
- **Attendance writes.** None in this pass.
- **NPI persistence.** No `ae_*`, Aadhaar, PAN, passport, voter ID, phone or email in Room, DataStore, a DTO, or a log line. On-screen display for desk verification is allowed via the in-memory `SensitiveInfo` map only. **Sheet bodies are never persisted**: HTML in memory, documents in `cacheDir/sheets`, wiped on logout / session expiry / erase-all.
- **WebView hardening.** JavaScript stays off. Cookies stay out of `CookieManager`.
- **Server messages.** Rendered verbatim, unmodified — including ugly ones like `Please Edit application and choose Area teacher before approving!`.
- **Retired surfaces.** Manage Courses, Daily Activity, SMS Report, Letters. Do not re-add.
- **The Day-11 chip.** Keep it; do not turn it into a 13th grid cell; do not draw a dashed GAP marker.
- **`local.properties`, keystores, real student data.** Not committed, ever — including the screenshots in the handover zip and the source HAR.

## Verify

Full green suite (JVM modules use `:test`, Android modules `:testDebugUnitTest`):

```bash
./gradlew :core:model:test :core:audit:test \
  :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest \
  :app:testDebugUnitTest
```

**Never run `./gradlew test` or `:app:test` at the root.** It drags in `:app:testReleaseUnitTest`, where all ~76 Robolectric Compose tests fail with "Unable to resolve activity … ComponentActivity" — `androidx.ui.test.manifest` is `debugImplementation`, so the release variant has no test activity. That failure is the build config, not your code.

Build and install:

```bash
./gradlew :app:assembleDebug
export ANDROID_HOME=/Users/wizops/Android/Sdk
export PATH="$ANDROID_HOME/platform-tools:$PATH"
adb connect 10.0.0.144:5555
adb -s 10.0.0.144:5555 install -r -d app/build/outputs/apk/debug/app-debug.apk
adb -s 10.0.0.144:5555 shell am start -n org.dhamma.dipi.staff/.MainActivity
```

This pass is tablet-facing, so **install on the Pixel C** (`5C01001294`, Wi-Fi `10.0.0.144:5555`) after the version bump. Toolchain: Kotlin JVM target 17, Gradle 8.9, compileSdk/targetSdk 35, minSdk 26; the build Mac has only JDK 20.

### What to check by hand on the tablet

The unit suite cannot see any of this:

1. Open all six HTML sheets. **No dead control anywhere** — no `Columns:` pills that do nothing, no `Print` link that does nothing, no seating instruction panel, no drag handles.
2. Tap the native sort/order controls on Day 0 list, teacher list and student chit. Confirm the refetch and confirm from the desk that **no seat has been reshuffled** (the `r` regression test).
3. Print each print-first sheet to PDF and check the A4 geometry against Design's mm annotations — chits especially, since they get guillotined.
4. Course report from the centre dashboard: loaded, empty range, and a forced failure to see the verbatim server text.
5. Rooms & seats: **no `( View )` anywhere**, on a course with both occupied and free blocks.
6. Board: eleven chips, no `Course report`, Day-11 still on its own fourth line, whole Board on one fold with no scroll.

## When you finish

Append the v5 delta to `docs/DESIGN.md` (§"Shipped delta ledger") so the next design pass does not re-propose what you just landed — in particular the removed `Course report` Board chip, the injected sheet stylesheet, and the native Day 0 summary. Update the version line in `AGENTS.md` and `CLAUDE.md`. Attach the APK to the GitHub release twice, versioned (`dipi-staff-1.31.0.apk`) and as the stable `dipi-staff.apk`, then mark the release latest.
