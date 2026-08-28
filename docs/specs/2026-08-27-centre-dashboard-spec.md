# Centre dashboard rework — status matrix, 60/40 layout, blended desk tiles

**Status:** proposed, 2026-08-27
**Origin:** owner feedback 2026-08-27, with a reference screenshot of the live
desk's own "Upcoming Courses" block.
**Backend reference:** `/Users/wizops/DIPI/dipi-web` resynced with
`upstream/master` on 2026-08-27 (50 commits merged, clean, local work
preserved). All PHP below was read from that tree and is **immutable** — no
backend change is proposed or permitted.

## What the backend actually serves

`GET /centre/{cid}` → `dh_manage_centre()` (`inc/centre.inc`) emits, in order:

1. an `important_notice` div,
2. `<ul class="multi-column">` of desk-module links, each wrapped
   `<h2><li><a href="…">Label</a></li></h2>`, **server-gated** by
   `drupal_valid_path()` — a user without the permission simply gets no link,
3. the `dh_zero_select_course` form (the course dropdown the app already
   scrapes for older courses),
4. `dashboard($cid)` = `received_applications()` + `upcoming_courses()`.

`upcoming_courses()` (`inc/centre.inc:583`) selects **at most 4** courses
(`limit 4`, `c_start >= today`, `c_deleted=0`, ordered by start) and hands them
to `course_summary()` (`inc/course.inc:638`).

### The matrix contract — `course_summary()`

Per course it emits:

```html
<div class="summary-block">
  <div class="table-heading"><a href="/course/{cid}/{courseId}">{c_name}</a></div>
  <table>
    <tr><th></th><th>NM</th><th>OM</th><th>Total</th><th>SM</th><th>&nbsp;&nbsp;</th>
        <th>NF</th><th>OF</th><th>Total</th><th>SF</th></tr>
    <tr><td><a>{status}</a></td><td>NM</td><td>OM</td><td><b>Tot</b></td><td>SM</td>
        <td>&nbsp;</td><td>NF</td><td>OF</td><td><b>Tot</b></td><td>SF</td></tr>
    …one row per status…
    <tr><td><b>Total</b></td>…same 9 numeric cells…</tr>
  </table>
</div>
```

Exactly **10 cells per row**: status label, then for male `NM, OM, Total, SM`,
then a spacer cell, then for female `NF, OF, Total, SF`. `N`=new, `O`=old,
`S`=sevak. The per-status `Total` cells count **students only** — sevaks live
in the `SM`/`SF` columns and are added in separately on the grand-`Total` row.

Status rows come from `dh_type_detail`: `COURSE-SYSTEM-STATUS` (minus
`STATUS-LEFT`, `STATUS-CUSTOM`, `STATUS-ATTENDED`) followed by `COURSE-STATUS`,
each ordered by `td_val2`. Observed set: Received, Confirmed, Cancelled,
Clarification-Response, ReConfirmation, Expected, Errors, Review, Clarification,
WaitList, PreConfirmation, Rejected, Regret, Duplicate. **The app must not
hardcode this list** — it is data-driven and a centre may add `COURSE-STATUS`
rows. Parse whatever rows are present, in page order.

Empty cells are rendered as `""`, not `0`. Counts appear bare, as `<a>N</a>`,
or as `<b><a>N</a></b>`.

### The desk-module list

`$modules` in `dh_manage_centre()` is exactly:
Centre Settings (`centre/{cid}/edit`), Manage Courses (`manage-course/{cid}`),
Manage Letters (`letters/{cid}`), Search (`search-app/{cid}`), Daily Activity
(`daily-activity/{cid}`), AT Schedule (`at-schedule/{cid}`), Referral List
(`referral/{cid}`), Center Referral List (`center-referral/{cid}`), SMS Report
(`centre/{cid}/sms-report`), Course Report (`centre/{cid}/course-report`),
Bulk Mail Schedule (`centre/{cid}/bulk-mail-schedule`).

## What ships today

`CentrePageParser.courseSummaries()` already finds these `summary-block`
tables, but collapses each to a flat
`CourseSummary(received, confirmed, expected, cancelled, total)` with male and
female **summed away** and eleven statuses discarded. `CentreScreen` renders
that as one line: `"Confirmed 77 | Cancelled 7 | Received 2 | Total 111"`.

`CentreScreen` is one long `verticalScroll` column: header → upcoming grid →
**all** older courses → a standalone Centre-settings card → "Centre desk" tiles
→ a bottom `TextButton("Settings")`.

---

## S1 — Course matrix model (additive)

**Decision: additive, not a replacement.** `CourseSummary` and
`courseCountsLine()` stay exactly as they are, because `CourseHubScreen.kt:107`
also consumes them. A new type carries the richer data alongside.

New file `core/model/src/main/kotlin/org/dhamma/dipi/staff/model/CourseMatrix.kt`:

```kotlin
package org.dhamma.dipi.staff.model

/** One status row of the centre dashboard matrix, split the way the desk splits it. */
data class MatrixRow(
    val label: String,
    val newMale: Int = 0,
    val oldMale: Int = 0,
    val sevakMale: Int = 0,
    val newFemale: Int = 0,
    val oldFemale: Int = 0,
    val sevakFemale: Int = 0,
) {
    val maleTotal: Int get() = newMale + oldMale
    val femaleTotal: Int get() = newFemale + oldFemale
    val studentTotal: Int get() = maleTotal + femaleTotal
    val sevakTotal: Int get() = sevakMale + sevakFemale
    val isEmpty: Boolean get() = studentTotal == 0 && sevakTotal == 0
}

/**
 * Everything `course_summary()` rendered for one course. [rows] keeps the
 * desk's own order and label spelling — the status set is data-driven
 * (dh_type_detail) and must never be hardcoded here.
 */
data class CourseMatrix(
    val rows: List<MatrixRow> = emptyList(),
    val total: MatrixRow? = null,
) {
    fun row(label: String): MatrixRow? = rows.firstOrNull { it.label.equals(label, ignoreCase = true) }

    /** The three the registrar acts on, in this order, omitting any that are all-zero. */
    val highlights: List<MatrixRow> get() = HIGHLIGHT_LABELS.mapNotNull { row(it) }.filterNot { it.isEmpty }

    companion object {
        val HIGHLIGHT_LABELS = listOf("Received", "Confirmed", "Cancelled")
    }
}
```

`Course` gains one nullable field: `val matrix: CourseMatrix? = null`, placed
after the existing `summary`. Absent block → `null`, rendered as nothing.

## S2 — Parser

`CentrePageParser` gains `fun courseMatrices(html: String): Map<Int, CourseMatrix>`
using the same `summary-block` / `table-heading` segmentation the existing
`courseSummaries` uses. Rules:

- A row is a status row when it has ≥10 `<td>` cells and its label is not
  `Total`; the `Total` row (label equals "Total", case-insensitive) goes to
  `CourseMatrix.total`.
- Cell → int: strip tags, keep digits, empty ⇒ 0 (reuse the existing `num()`).
- Cell index map: `0` label, `1` NM, `2` OM, `3` male student total (derived —
  **ignore it, recompute** from NM+OM so the model is self-consistent), `4` SM,
  `5` spacer, `6` NF, `7` OF, `8` female student total (ignore), `9` SF.
- Preserve page order in `rows`. Keep rows even when all-zero — the caller
  decides what to hide (`isEmpty`).
- The label is the stripped text of cell 0, with surrounding whitespace
  trimmed; keep the desk's own spelling including hyphens
  (`Clarification-Response`).

`courseSummaries` is **left untouched** so its tests and `CourseHubScreen`
keep working.

## S3 — Upcoming courses at 60% of the screen

On a wide screen (`screenWidthDp >= 600`) the centre screen stops being one
long scroll and becomes a fixed header over two independently scrolling
regions:

- header (centre name, account line, centre switcher) — fixed, unscrolled
- **upcoming courses — `Modifier.weight(0.6f)`, its own `verticalScroll`**
- everything below (older courses, desk tiles) — `Modifier.weight(0.4f)`, its
  own `verticalScroll`

Weights are used rather than `fillMaxHeight(0.6f)` deliberately: nesting a
`verticalScroll` inside another `verticalScroll` on the same axis is a Compose
measurement error, and weights give the 60/40 split without one.

On a narrow screen (`< 600dp`) the current single-scroll column is kept —
60% of a phone viewport cannot hold a matrix card legibly.

## S4 — What a course card shows

Each upcoming course card keeps its name, dates and "STARTS IN n DAYS" chip,
and **replaces the flat counts line** with a compact matrix:

- A header row of column kickers: `NM  OM  M  ·  NF  OF  F`
- One row per `CourseMatrix.highlights` entry (Received, Confirmed, Cancelled;
  all-zero rows omitted), label left-aligned, six numbers right-aligned
- A final `Total` row from `CourseMatrix.total`, rendered in the same shape but
  emphasised, with the sevak counts appended as `+{n} sevak` when non-zero
- A zero renders as `·` (a middot), never `0` — matching the desk, which leaves
  empty cells blank rather than printing zeros
- If `matrix` is null, fall back to the existing `courseCountsLine(summary)`
  so a permission-gated or malformed page still shows something

"More detail from the backend" is satisfied by the model retaining every row;
the card deliberately shows only the three the registrar acts on. A later
change can expose the rest without another parser change.

## S5 — Older courses capped at 3

`StaffRepository.loadCourses` caps the older list at **3**, via a named
constant `OLDER_COURSE_LIMIT = 3`, applied on **both** branches (the mock
branch and the live-HTML branch). The cap is a product decision and belongs in
the repository, not in `CentrePageParser` — the parser stays a faithful reader
of whatever the page contains.

## S6 — Desk tiles blended into the background

Today each tile is `deskCard(fill = c.field, border = c.hairline)` with
`c.accent` text — a raised, lighter-than-background card with coloured text.
It reads as a stack of buttons competing with the content.

New treatment, still entirely token-driven so all five skins follow:

- fill: `Color.Transparent` (the page ground shows through)
- border: `c.hairline`
- elevation: `0.dp` (no shadow — this is the part that makes it read as raised)
- label: `c.foreground`, not `c.accent`

Nothing else about the tile changes — same `tileShape`, same 62dp min height,
same 3-across grid, same padding.

## S7 — Centre settings and App Settings become desk tiles

Both move into the "Centre desk" tile grid, and the two standalone controls
above and below it are removed:

- the standalone `Centre settings` card (currently between older courses and
  the tile grid) is deleted; its `onCentreOps` action moves onto a tile
- the bottom `TextButton("Settings")` is deleted; it becomes a tile labelled
  **"App Settings"** wired to `onSettings`

`centreDeskTiles()` therefore needs tiles that open **native screens** rather
than desk-site routes. `DeskTileSpec` gains a nullable action discriminator:

```kotlin
enum class DeskTileAction { CentreOps, AppSettings, AdvancedSearch }

data class DeskTileSpec(
    val title: String,
    val route: String,
    val action: DeskTileAction? = null,
)
```

`CentreScreen` dispatches on `action` when present and falls back to
`onLater(title, route)` otherwise — replacing today's string comparison
`if (tile.title == "Advanced Search")`, which is fragile.

**Ordering decision.** Native tiles lead, because they are what the registrar
touches: `Centre Settings` (native), `Advanced Search` (native),
`App Settings` (native), then the desk-site links in their existing order.

**Collision decision.** `centreDeskTiles` today has `Centre Settings` →
`centre/{cid}/edit`, the desk-site edit form. That entry is **replaced** by the
native one rather than duplicated — two tiles reading "Centre Settings" that
go to different places is worse than losing the web form, which stays
reachable through the desk site itself.

## Tests this invalidates — deliberate, not incidental

Two assertions in `app/src/test/.../CentreScreenTest.kt` pin behaviour this
spec changes. They must be updated **knowingly**, and nothing else in that file
may be touched:

1. `catalogueOmitsLettersAtAndReferral` asserts
   `assertEquals("centre/1/edit", centreDeskTiles(1).first { it.title == "Centre Settings" }.route)`.
   Per S7 that tile is now native. Replacement: assert the tile's `action` is
   `DeskTileAction.CentreOps`, and that an `App Settings` tile exists with
   action `DeskTileAction.AppSettings`. The existing Letters/AT/Referral
   exclusion assertions in the same test stay exactly as they are.
2. `dashboardShowsCoursesCountsAndCentreRows` asserts the flat card line
   `"Confirmed 77 | Cancelled 7 | Received 2 | Total 111"`. Per S4 the card now
   renders a matrix. Replacement: assert the matrix header kickers and the
   Confirmed row's numbers are displayed.

`countsLineDropsZeroesAndAbsentSummaries` must **not** change —
`courseCountsLine` is unchanged and still used by `CourseHubScreen`.

## Out of scope

- Any backend change. `dipi-web` is read-only reference.
- The `received_applications()` block (the second dashboard table). Not asked
  for; the upcoming matrix is the ask.
- Adding Manage Letters / AT Schedule / Referral tiles — they were removed from
  the catalogue by earlier owner feedback and that still stands.
- Making the full status list visible on the card. The model carries it; the UI
  deliberately does not yet.

## Versioning

Current `main`-line is 1.20.1 / 32 on `feat/v3-conformance`. This is a
user-visible feature change inside the current vertical → **MINOR**:
**1.21.0 / versionCode 33**. `feat/desk-gap` holds 1.19.0/30 and does not
collide. Registrar-facing, so hard rule 12 requires the Pixel C install.
