# Centre Dashboard Rework — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the centre dashboard around the desk's own status matrix — upcoming courses at 60% of the screen showing Received/Confirmed/Cancelled split by gender, three older courses, and desk tiles that recede into the page instead of competing with it.

**Architecture:** Additive data layer — a new `CourseMatrix` carries the full status × gender × new/old × student/sevak grid alongside the existing `CourseSummary`, which stays untouched because `CourseHubScreen` shares it. Parser, repository and screen each change in one owned file. The 60/40 split uses Compose weights rather than nested scrolls.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Hilt, Robolectric + `createComposeRule`, JUnit4, Gradle 8.9 / JDK 20.

**Spec:** `docs/specs/2026-08-27-centre-dashboard-spec.md`

## Global Constraints

- **The spec's backend contract is authoritative.** Cell indices, the 10-cell row shape, and the status set being data-driven all come from PHP read on 2026-08-27. Do not hardcode the status list.
- **`dipi-web` is read-only reference.** No backend change, ever.
- **`CourseSummary` and `courseCountsLine()` must not change** — `CourseHubScreen.kt:107` depends on them, and `countsLineDropsZeroesAndAbsentSummaries` pins them.
- **Colours from `Industry`/`LocalDipi` tokens only**, never hard-coded hex — five user-selectable skins.
- **No client-side access control.** The desk omits links the user cannot reach; render what is present, never gate locally.
- **Never persist or log NPI.** This work touches aggregate counts only; no applicant row may enter any model here.
- **Only two existing assertions may be modified**, both named in the spec's "Tests this invalidates" section and both owned by Task 5. Every other existing test is untouchable.
- **Test command** (never bare `./gradlew test` — it drags in `:app:testReleaseUnitTest`, broken here for an unrelated pre-existing reason):
  ```bash
  ./gradlew :core:model:test :core:audit:test \
            :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest \
            :app:testDebugUnitTest
  ```
- Kotlin JVM target 17; compileSdk/targetSdk 35; minSdk 26.
- Commits carry **no** `Co-Authored-By` / `Claude-Session` trailers — repo convention, verified against history. gpg signing is on.

---

## Multi-agent workflow layout

Strict file ownership: no two concurrently-running workers share a file. A file
re-touched in a later phase is fine because the phases are ordered.

```
Phase 1  ── W1 model ──┐          (parallel, disjoint)
         └─ W2 cap ────┤
                       ▼
Phase 2  ── W3 parser ─┤          (needs W1's type)
                       ▼
Phase 3  ── W4 wiring ─┐          (needs W1+W3)   (parallel, disjoint)
         └─ W5 UI ─────┤          (needs W1)
                       ▼
Phase 4  ── Integrator ┘          suite, SemVer, release, Pixel C install
```

| Worker | Owns exclusively | Needs |
|---|---|---|
| W1 model | `core/model/.../CourseMatrix.kt` (new), `core/model/.../Models.kt`, `core/model/src/test/.../CourseMatrixTest.kt` (new) | — |
| W2 cap | `app/.../data/StaffRepository.kt`, `app/src/test/.../OlderCourseLimitTest.kt` (new) | — |
| W3 parser | `core/network/.../CentrePageParser.kt`, `core/network/src/test/.../CentrePageParserTest.kt`, `core/network/.../MockFixtures.kt` | W1 |
| W4 wiring | `app/.../data/StaffRepository.kt` (second visit) | W1, W3 |
| W5 UI | `feature/course/.../DeskTiles.kt`, `feature/course/.../CentreScreen.kt`, `app/src/test/.../CentreScreenTest.kt` | W1 |
| Integrator | `app/build.gradle.kts`, `CLAUDE.md`, `AGENTS.md` | all |

`StaffRepository.kt` is touched by W2 then W4 — never concurrently.

---

### Task 1 (W1): Course matrix model

**Files:**
- Create: `core/model/src/main/kotlin/org/dhamma/dipi/staff/model/CourseMatrix.kt`
- Modify: `core/model/src/main/kotlin/org/dhamma/dipi/staff/model/Models.kt` (one field on `Course`)
- Test: `core/model/src/test/kotlin/org/dhamma/dipi/staff/model/CourseMatrixTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `MatrixRow`, `CourseMatrix` (exact shape in spec S1), and `Course.matrix: CourseMatrix? = null`. Tasks 3, 4 and 5 all bind to these names.

- [ ] **Step 1: Write the failing test**

Create `CourseMatrixTest.kt`:

```kotlin
package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseMatrixTest {

    private val confirmed = MatrixRow("Confirmed", newMale = 7, oldMale = 3, sevakMale = 1, newFemale = 3, oldFemale = 0, sevakFemale = 0)

    @Test
    fun rowDerivesItsOwnTotals() {
        assertEquals(10, confirmed.maleTotal)
        assertEquals(3, confirmed.femaleTotal)
        assertEquals(13, confirmed.studentTotal)
        assertEquals(1, confirmed.sevakTotal)
        assertFalse(confirmed.isEmpty)
    }

    @Test
    fun aRowWithNoStudentsAndNoSevaksIsEmpty() {
        assertTrue(MatrixRow("Errors").isEmpty)
        assertFalse(MatrixRow("Errors", sevakFemale = 1).isEmpty)
    }

    @Test
    fun lookupIsCaseInsensitiveAndMissingRowsAreNull() {
        val m = CourseMatrix(rows = listOf(confirmed))
        assertEquals(confirmed, m.row("confirmed"))
        assertNull(m.row("WaitList"))
    }

    @Test
    fun highlightsKeepSpecOrderAndDropEmptyRows() {
        val m = CourseMatrix(
            rows = listOf(
                MatrixRow("Cancelled", newMale = 2),
                MatrixRow("Received"),
                confirmed,
                MatrixRow("Expected", newMale = 44),
            ),
        )
        // Received is present but all-zero, so it drops; Expected is not a highlight.
        assertEquals(listOf("Confirmed", "Cancelled"), m.highlights.map { it.label })
    }

    @Test
    fun anEmptyMatrixHasNoHighlights() {
        assertEquals(emptyList<MatrixRow>(), CourseMatrix().highlights)
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

Run: `./gradlew :core:model:test --tests '*CourseMatrixTest*'`
Expected: FAIL — `Unresolved reference: MatrixRow`.

- [ ] **Step 3: Write `CourseMatrix.kt`**

Use the exact code in spec S1. Do not add fields beyond it.

- [ ] **Step 4: Add the field to `Course`**

In `Models.kt`, `Course` currently ends with `val summary: CourseSummary? = null,`. Add directly beneath it, inside the same class:

```kotlin
    /** The full centre-dashboard status matrix; null when the desk did not render the block. */
    val matrix: CourseMatrix? = null,
```

It must be defaulted so every existing construction site still compiles.

- [ ] **Step 5: Run the targeted test, then the full suite**

Run `./gradlew :core:model:test --tests '*CourseMatrixTest*'` (expect PASS, 5 tests), then the full-suite command from Global Constraints (expect exit 0).

- [ ] **Step 6: Commit**

```bash
git add core/model/src/main/kotlin/org/dhamma/dipi/staff/model/ core/model/src/test/kotlin/org/dhamma/dipi/staff/model/CourseMatrixTest.kt
git commit -m "feat: model the centre dashboard status matrix"
```

---

### Task 2 (W2): Cap older courses at three

**Files:**
- Modify: `app/src/main/kotlin/org/dhamma/dipi/staff/data/StaffRepository.kt` (`loadCourses`, around lines 191-213)
- Test: `app/src/test/kotlin/org/dhamma/dipi/staff/OlderCourseLimitTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `StaffRepository.OLDER_COURSE_LIMIT = 3` (a `const val` in the file's `companion object`, or a private top-level `const` if the class has none — expose it so the test can assert against the constant rather than a literal).

- [ ] **Step 1: Read `loadCourses` first**

It has two branches: a mock branch returning `api.courses(centreId.value, upcoming = 0).items` as `older`, and a live branch building `older` from `CentrePageParser.olderCourseOptions(...)`. **Both** must be capped.

- [ ] **Step 2: Write the failing test**

The repository is heavy to construct. Test the cap as a pure list operation against the same constant the production code uses, so the test breaks if the constant moves:

```kotlin
package org.dhamma.dipi.staff

import org.dhamma.dipi.staff.data.OLDER_COURSE_LIMIT
import org.junit.Assert.assertEquals
import org.junit.Test

class OlderCourseLimitTest {

    @Test
    fun theDeskShowsAtMostThreeOlderCourses() {
        assertEquals(3, OLDER_COURSE_LIMIT)
    }

    @Test
    fun takingTheLimitKeepsTheNewestAndDropsTheRest() {
        val newestFirst = listOf("sep", "aug", "jul", "jun", "may")
        assertEquals(listOf("sep", "aug", "jul"), newestFirst.take(OLDER_COURSE_LIMIT))
    }

    @Test
    fun shorterListsAreUnaffected() {
        assertEquals(listOf("sep"), listOf("sep").take(OLDER_COURSE_LIMIT))
    }
}
```

Declare `OLDER_COURSE_LIMIT` as an internal top-level `const val` in
`StaffRepository.kt`'s package (`org.dhamma.dipi.staff.data`) so this import
resolves. If `internal` blocks the test module from seeing it, make it public.

- [ ] **Step 3: Run it and watch it fail**

Run: `./gradlew :app:testDebugUnitTest --tests '*OlderCourseLimitTest*'`
Expected: FAIL — unresolved reference.

- [ ] **Step 4: Implement**

Add above the class in `StaffRepository.kt`:

```kotlin
/**
 * The desk lists every course from the last six months in its dropdown; the
 * registrar only ever reaches for the last few (owner feedback 2026-08-27).
 */
const val OLDER_COURSE_LIMIT = 3
```

Then apply `.take(OLDER_COURSE_LIMIT)` to the `older` list on **both** branches of `loadCourses`. `olderCourseOptions` already returns newest-first, so `take` keeps the newest three.

- [ ] **Step 5: Run targeted test, then the full suite**

Expect PASS then exit 0.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/org/dhamma/dipi/staff/data/StaffRepository.kt app/src/test/kotlin/org/dhamma/dipi/staff/OlderCourseLimitTest.kt
git commit -m "feat: show only the three most recent older courses"
```

---

### Task 3 (W3): Parse the full matrix

**Files:**
- Modify: `core/network/src/main/kotlin/org/dhamma/dipi/staff/network/CentrePageParser.kt` (add, do not alter `courseSummaries`)
- Modify: `core/network/src/test/kotlin/org/dhamma/dipi/staff/network/CentrePageParserTest.kt` (add cases only)
- Modify: `core/network/src/main/kotlin/org/dhamma/dipi/staff/network/MockFixtures.kt` (so mock mode renders a matrix)

**Interfaces:**
- Consumes: `CourseMatrix`, `MatrixRow` from Task 1.
- Produces: `CentrePageParser.courseMatrices(html: String): Map<Int, CourseMatrix>`. Task 4 calls it.

- [ ] **Step 1: Write the failing test**

Add to `CentrePageParserTest.kt` a fixture matching the real markup (spec "The matrix contract") and these assertions. Build the fixture from the shape below — two courses, so segmentation is exercised:

```html
<div class="summary-block"><div class="table-heading"><a href="/course/7/41">Dhamma Sudha / 10 Day / 2nd-Sep</a></div>
<table><tr><th></th><th>NM</th><th>OM</th><th>Total</th><th>SM</th><th>&nbsp;</th><th>NF</th><th>OF</th><th>Total</th><th>SF</th></tr>
<tr><td><a>Received</a></td><td></td><td></td><td></td><td></td><td>&nbsp;</td><td></td><td><a>1</a></td><td><b><a>1</a></b></td><td></td></tr>
<tr><td><a>Confirmed</a></td><td><a>7</a></td><td><a>3</a></td><td><b><a>10</a></b></td><td><a>1</a></td><td>&nbsp;</td><td><a>3</a></td><td></td><td><b><a>3</a></b></td><td></td></tr>
<tr><td><a>Errors</a></td><td></td><td></td><td></td><td></td><td>&nbsp;</td><td></td><td></td><td></td><td></td></tr>
<tr><td><b>Total</b></td><td>72</td><td>31</td><td><b>103</b></td><td>4</td><td>&nbsp;</td><td>32</td><td>13</td><td><b>45</b></td><td>3</td></tr>
</table></div>
```

Assert: the map is keyed by course id 41; `Confirmed` has `newMale=7, oldMale=3, sevakMale=1, newFemale=3, oldFemale=0`; `maleTotal` is 10 (**recomputed**, not read from the Total cell); `Received.oldFemale == 1`; the `Errors` row is present in `rows` but `isEmpty`; `total` is the bold row with `newMale=72, sevakFemale=3`; `highlights` returns Received then Confirmed (Cancelled absent); and a second course in the same HTML parses independently. Also assert `courseSummaries` still returns its old shape for the same HTML — proof it was not disturbed.

- [ ] **Step 2: Run it and watch it fail**

Run: `./gradlew :core:network:testDebugUnitTest --tests '*CentrePageParserTest*'`
Expected: FAIL — `Unresolved reference: courseMatrices`.

- [ ] **Step 3: Implement `courseMatrices`**

Mirror `courseSummaries`' segmentation exactly (same `headingRe`, same slice between headings, same `tableRe`). Per row: take cells via `cellRe`, require `cells.size >= 10`, label = `SearchPageParser.stripTags(cells[0]).trim()`. Build:

```kotlin
MatrixRow(
    label = label,
    newMale = num(cells[1]), oldMale = num(cells[2]), sevakMale = num(cells[4]),
    newFemale = num(cells[6]), oldFemale = num(cells[7]), sevakFemale = num(cells[9]),
)
```

Cells 3 and 8 are the desk's own totals and are deliberately ignored — `MatrixRow` recomputes them. A row whose label equals "Total" (case-insensitive) becomes `CourseMatrix.total`; everything else appends to `rows` in page order.

- [ ] **Step 4: Update the mock fixture**

In `MockFixtures.kt`, the mock centre page must include at least one `summary-block` in the real shape so `-Pdipi.useMock=true` exercises the same path. Keep the existing fixture content otherwise intact.

- [ ] **Step 5: Targeted test, then full suite**

Expect PASS then exit 0. The pre-existing `CentrePageParserTest` cases must all still pass untouched.

- [ ] **Step 6: Commit**

```bash
git add core/network/src/main/kotlin/org/dhamma/dipi/staff/network/ core/network/src/test/kotlin/org/dhamma/dipi/staff/network/CentrePageParserTest.kt
git commit -m "feat: parse the full centre dashboard status matrix"
```

---

### Task 4 (W4): Carry the matrix into the model

**Files:**
- Modify: `app/src/main/kotlin/org/dhamma/dipi/staff/data/StaffRepository.kt` (`loadCourses`, live branch)

**Interfaces:**
- Consumes: `CentrePageParser.courseMatrices` (Task 3), `Course.matrix` (Task 1), `OLDER_COURSE_LIMIT` (Task 2, already in this file).
- Produces: upcoming `Course` objects with `matrix` populated.

- [ ] **Step 1: Read the live branch of `loadCourses`**

It already calls `CentrePageParser.courseSummaries(html)` and attaches `summary` to each upcoming course. Follow that pattern exactly.

- [ ] **Step 2: Populate `matrix` alongside `summary`**

Call `CentrePageParser.courseMatrices(html)` once, then attach `matrix = matrices[courseId]` on each upcoming course, next to where `summary` is attached. Older courses do **not** get a matrix — the desk only renders the block for the upcoming four.

- [ ] **Step 3: Verify with the full suite**

Run the full-suite command. Expect exit 0. No new test is required here — Task 3 covers the parse and Task 5 covers the render; this step is pure wiring, and a repository test would need the whole Hilt graph for no added confidence. If you disagree after reading the code, add one and say why in your report.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/org/dhamma/dipi/staff/data/StaffRepository.kt
git commit -m "feat: attach the status matrix to upcoming courses"
```

---

### Task 5 (W5): The screen — 60/40, matrix cards, blended tiles

The largest task. Read spec sections S3, S4, S6 and S7 in full before starting.

**Files:**
- Modify: `feature/course/src/main/kotlin/org/dhamma/dipi/staff/course/DeskTiles.kt`
- Modify: `feature/course/src/main/kotlin/org/dhamma/dipi/staff/course/CentreScreen.kt`
- Modify: `app/src/test/kotlin/org/dhamma/dipi/staff/CentreScreenTest.kt` (**only** the two assertions the spec names)

**Interfaces:**
- Consumes: `CourseMatrix`, `MatrixRow` (Task 1).
- Produces: `DeskTileAction` enum; `DeskTileSpec(title, route, action)`; `CentreScreen` with an unchanged parameter list.

- [ ] **Step 1: `DeskTiles.kt` — add the action discriminator**

Add the enum and field from spec S7. Rebuild `centreDeskTiles` so native tiles lead:

```kotlin
fun centreDeskTiles(centreId: Int): List<DeskTileSpec> = listOf(
    DeskTileSpec("Centre Settings", "centre/$centreId/edit", DeskTileAction.CentreOps),
    DeskTileSpec("Advanced Search", "search-app/$centreId", DeskTileAction.AdvancedSearch),
    DeskTileSpec("App Settings", "", DeskTileAction.AppSettings),
    DeskTileSpec("Manage Courses", "manage-course/$centreId"),
    DeskTileSpec("Daily Activity", "daily-activity/$centreId"),
    DeskTileSpec("SMS Report", "centre/$centreId/sms-report"),
    DeskTileSpec("Course Report", "centre/$centreId/course-report"),
    DeskTileSpec("Bulk Mail", "centre/$centreId/bulk-mail-schedule"),
)
```

The Centre Settings `route` is kept for reference but unused when `action` is set. Letters / AT Schedule / Referral stay excluded — earlier owner feedback, still binding.

- [ ] **Step 2: `CentreScreen.kt` — the matrix card**

Add a private `CourseMatrixTable(matrix: CourseMatrix)` composable per spec S4: a kicker header row (`NM OM M · NF OF F`), one row per `matrix.highlights`, then the emphasised `Total` row with `+{n} sevak` appended when `sevakTotal > 0`. Zeros render as `·`. Numbers use `DipiMono`; labels use the body face. In `CourseCard`, replace the `courseCountsLine` line with: if `course.matrix != null` render `CourseMatrixTable`, else fall back to the existing counts line.

- [ ] **Step 3: `CentreScreen.kt` — the 60/40 split**

Per spec S3. On `screenWidthDp >= 600`: outer `Column(Modifier.fillMaxSize())`, fixed header, then upcoming in `Modifier.weight(0.6f).verticalScroll(rememberScrollState())` and the remainder in `Modifier.weight(0.4f).verticalScroll(rememberScrollState())` — **two separate scroll states**. Below 600dp keep today's single scrolling column exactly as it is. Do not nest one `verticalScroll` inside another on the same axis; that is a measurement crash, not a style choice.

- [ ] **Step 4: `CentreScreen.kt` — blended tiles, and fold in the two controls**

Change the tile `Box` to `deskCard(shape = DeskStyle.tileShape, fill = Color.Transparent, border = c.hairline, elevation = 0.dp)` and the label colour from `c.accent` to `c.foreground` (spec S6). Replace the fragile `if (tile.title == "Advanced Search")` with a `when (tile.action)` dispatch: `CentreOps -> onCentreOps()`, `AdvancedSearch -> onAdvancedSearch()`, `AppSettings -> onSettings()`, `null -> onLater(tile.title, tile.route)`. Then **delete** the standalone Centre-settings card and the bottom `TextButton("Settings")` — both are now tiles.

- [ ] **Step 5: Update exactly two existing assertions**

In `CentreScreenTest.kt`, and nowhere else:
- In `catalogueOmitsLettersAtAndReferral`, replace the `assertEquals("centre/1/edit", …Centre Settings….route)` line with assertions that the Centre Settings tile's `action` is `DeskTileAction.CentreOps` and that a tile titled `App Settings` exists with action `DeskTileAction.AppSettings`. Leave the Letters/AT/Referral exclusions and the Advanced Search route assertion alone.
- In `dashboardShowsCoursesCountsAndCentreRows`, replace the
  `"Confirmed 77 | Cancelled 7 | Received 2 | Total 111"` assertion with matrix
  assertions: give the fixture course a `matrix`, then assert the `Confirmed`
  label and its numbers render.

**`countsLineDropsZeroesAndAbsentSummaries` must not be touched.** If you find yourself editing a third assertion, stop and report it.

- [ ] **Step 6: Add new coverage**

Add tests for: `highlights` rendering only the three important rows; the `·` standing in for a zero; a null matrix falling back to the counts line; and the App Settings tile invoking `onSettings`.

- [ ] **Step 7: Targeted, then full suite**

`./gradlew :app:testDebugUnitTest --tests '*CentreScreenTest*'` then the full-suite command. Both green.

- [ ] **Step 8: Commit**

```bash
git add feature/course/src/main/kotlin/org/dhamma/dipi/staff/course/ app/src/test/kotlin/org/dhamma/dipi/staff/CentreScreenTest.kt
git commit -m "feat: centre dashboard at 60/40 with gender-split matrix cards and blended tiles"
```

---

### Task 6 (Integrator): Ship 1.21.0

**Files:** `app/build.gradle.kts`, `CLAUDE.md`, `AGENTS.md`

- [ ] **Step 1: Confirm the version is free**

`git show feat/desk-gap:app/build.gradle.kts | grep -E "versionCode|versionName"` — expect 1.19.0 / 30. If 1.21.0 / 33 is taken, stop and report.

- [ ] **Step 2: Bump** `versionCode = 32` → `33`, `versionName = "1.20.1"` → `"1.21.0"`.

- [ ] **Step 3: Update helper docs** — `CLAUDE.md` and `AGENTS.md` reference 1.20.1 / 32; change to 1.21.0 / 33. Change **only** the numbers; leave the phrase "on `main`" alone.

- [ ] **Step 4: Full suite.** Expect exit 0.

- [ ] **Step 5: `./gradlew :app:assembleRelease`.** Report path and byte size.

- [ ] **Step 6: Install and verify on the Pixel C**

```bash
export ANDROID_HOME=/Users/wizops/Android/Sdk
export PATH="$ANDROID_HOME/platform-tools:$PATH"
adb connect 10.0.0.144:5555
adb -s 10.0.0.144:5555 install -r -d app/build/outputs/apk/release/app-release.apk
adb -s 10.0.0.144:5555 shell am start -n org.dhamma.dipi.staff/.MainActivity
adb -s 10.0.0.144:5555 shell dumpsys package org.dhamma.dipi.staff | grep -E "versionCode|versionName"
```

Must report 33 / 1.21.0. Device unreachable is not a failure — report it.

- [ ] **Step 7: Commit**

```bash
git add app/build.gradle.kts CLAUDE.md AGENTS.md
git commit -m "feat: centre dashboard rework at 1.21.0"
```

---

## Self-review notes

**Spec coverage.** S1 → Task 1. S2 → Task 3. S3 → Task 5 Step 3. S4 → Task 5 Step 2. S5 → Task 2. S6 → Task 5 Step 4. S7 → Task 5 Steps 1 and 4. "Tests this invalidates" → Task 5 Step 5. Versioning → Task 6.

**Type consistency.** `MatrixRow` / `CourseMatrix` / `Course.matrix` are defined in Task 1 and consumed under those exact names in 3, 4 and 5. `courseMatrices` is produced in Task 3 and called in Task 4. `DeskTileAction` is produced and consumed inside Task 5. `OLDER_COURSE_LIMIT` is produced in Task 2 and lives in the file Task 4 revisits.

**Known risk.** `StaffRepository.kt` is edited by Task 2 and again by Task 4. They are in different phases and never run concurrently, but Task 4's implementer must re-read the file rather than working from the plan's line numbers.
