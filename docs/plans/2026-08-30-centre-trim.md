# Centre Trim — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Drop three desk destinations from the app and the design contract, take the scroll off the upcoming pane, give every course card the same four rows and therefore the same height, and widen older-course buttons to match an upcoming card.

**Architecture:** One model addition (`plus` on `MatrixRow`, a `cardRows` accessor) then one screen task — the tile list, the card and the lower pane are too coupled to split across concurrent workers, since all three land in the same two test files. A docs task runs in parallel with the model since it shares nothing.

**Tech Stack:** Kotlin, Jetpack Compose (M3), Robolectric + `createComposeRule`, JUnit4, Gradle 8.9 / JDK 20.

**Spec:** `docs/specs/2026-08-30-centre-trim-spec.md`

## Global Constraints

- Tokens via `LocalDipi`/`Industry`, never inline hex. ≥48dp targets on anything new.
- No client ACL; no NPI; backend immutable — this round touches no network code at all.
- Only the assertions the spec's "Tests this invalidates" section names may be retargeted; a third category = STOP and report BLOCKED. Adding tests is always allowed.
- `highlights` / `HIGHLIGHT_LABELS` stay — `cardRows` is additive.
- **Test command** (never bare `./gradlew test`; `:app:testReleaseUnitTest` is known-broken for a pre-existing reason):
  ```bash
  ./gradlew :core:model:test :core:audit:test \
            :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest \
            :app:testDebugUnitTest
  ```
- Wide tests use `@Config(qualifiers = "w1240dp-h844dp-land")`.
- Commits: no `Co-Authored-By`/`Claude-Session` trailers; gpg on.

## Workflow layout

```
Phase 1  ── T1 model ──┐   (parallel, share nothing)
         └─ T2 docs ───┤
                       ▼
Phase 2  ── T3 screen ─┤   (needs T1; owns both test files)
                       ▼
Phase 3  ── T4 ship ───┘
```

| Task | Owns exclusively | Needs |
|---|---|---|
| T1 model | `core/model/.../CourseMatrix.kt`, `core/model/src/test/.../CourseMatrixTest.kt` | — |
| T2 docs | `version-4/uploads/dipi-ui-export/SHIPPED-DELTA.md`, `version-4/README.md`, `docs/CLAUDE-DESIGN-DESK-SCREENS.md` | — |
| T3 screen | `feature/course/.../DeskTiles.kt`, `feature/course/.../CentreScreen.kt`, `app/src/test/.../CentreScreenTest.kt`, `app/src/test/.../CentreScreenWideTest.kt` | T1 |
| T4 ship | `app/build.gradle.kts`, `CLAUDE.md`, `AGENTS.md` | all |

---

### Task 1 (T1): `plus` and `cardRows`

**Files:** Modify `core/model/src/main/kotlin/org/dhamma/dipi/staff/model/CourseMatrix.kt`; modify `core/model/src/test/kotlin/org/dhamma/dipi/staff/model/CourseMatrixTest.kt` (additions only).

**Interfaces produced:** `operator fun MatrixRow.plus(other: MatrixRow?): MatrixRow` and `val CourseMatrix.cardRows: List<MatrixRow>` — T3 renders `cardRows`.

- [ ] **Step 1: failing tests** (append to `CourseMatrixTest`, change nothing existing)

```kotlin
    @Test
    fun plusSumsEveryCountAndKeepsTheReceiverLabel() {
        val a = MatrixRow("Confirmed", newMale = 7, oldMale = 3, sevakMale = 1, newFemale = 3)
        val b = MatrixRow("Expected", newMale = 44, oldMale = 19, newFemale = 19, oldFemale = 10, sevakFemale = 1)
        val sum = a + b
        assertEquals("Confirmed", sum.label)
        assertEquals(51, sum.newMale)
        assertEquals(22, sum.oldMale)
        assertEquals(1, sum.sevakMale)
        assertEquals(22, sum.newFemale)
        assertEquals(10, sum.oldFemale)
        assertEquals(1, sum.sevakFemale)
        assertEquals(73, sum.maleTotal)
    }

    @Test
    fun plusNullIsTheReceiver() {
        val a = MatrixRow("Confirmed", newMale = 7)
        assertEquals(a, a + null)
    }

    @Test
    fun cardRowsAlwaysHasFourRowsInOrderEvenWhenStatusesAreAbsent() {
        val empty = CourseMatrix().cardRows
        assertEquals(listOf("Received", "Confirmed + Expected", "Cancelled"), empty.map { it.label })
        assertEquals(3, empty.size)
        assertTrue(empty.all { it.isEmpty })
    }

    @Test
    fun cardRowsMergesConfirmedAndExpected() {
        val m = CourseMatrix(
            rows = listOf(
                MatrixRow("Received", oldFemale = 1),
                MatrixRow("Confirmed", newMale = 7, oldMale = 3),
                MatrixRow("Expected", newMale = 44, oldMale = 19),
                MatrixRow("Cancelled", newMale = 10),
            ),
        )
        val rows = m.cardRows
        assertEquals(listOf("Received", "Confirmed + Expected", "Cancelled"), rows.map { it.label })
        assertEquals(51, rows[1].newMale)
        assertEquals(22, rows[1].oldMale)
        assertEquals(73, rows[1].maleTotal)
        assertEquals(1, rows[0].oldFemale)
        assertEquals(10, rows[2].newMale)
    }

    @Test
    fun cardRowsKeepsAnAbsentStatusAsAnEmptyRowRatherThanDroppingIt() {
        val m = CourseMatrix(rows = listOf(MatrixRow("Confirmed", newMale = 5)))
        val rows = m.cardRows
        assertEquals(3, rows.size)
        assertTrue(rows[0].isEmpty)   // Received absent
        assertEquals(5, rows[1].newMale)
        assertTrue(rows[2].isEmpty)   // Cancelled absent
    }
```

Note `cardRows` returns the **three status rows**; the Total row is
`CourseMatrix.total` and the card renders it separately, as it already does.

- [ ] **Step 2:** `./gradlew :core:model:test --tests '*CourseMatrixTest*'` → FAIL (unresolved `plus` / `cardRows`).
- [ ] **Step 3:** implement per spec S3. `plus` returns `this` when `other == null`. `cardRows` builds `Received`, `row("Confirmed") + row("Expected")` relabelled `"Confirmed + Expected"`, `Cancelled` — each falling back to `MatrixRow(label)` when absent. Keep `highlights`/`HIGHLIGHT_LABELS` exactly as they are.
- [ ] **Step 4:** targeted green, then the full suite.
- [ ] **Step 5:** commit `feat: fixed card rows with a merged confirmed-plus-expected line`.

---

### Task 2 (T2): record the removal in the design contract

**Files:** Modify `version-4/uploads/dipi-ui-export/SHIPPED-DELTA.md`, `version-4/README.md`, `docs/CLAUDE-DESIGN-DESK-SCREENS.md`. No code, no tests.

- [ ] **Step 1:** In `SHIPPED-DELTA.md`, find the do-not-re-propose list and add Manage Courses, Daily Activity and SMS Report, each with the date (2026-08-30) and one clause of reason ("removed from the app on owner instruction; still reachable on the desk site"). Match the file's existing entry style — read it first.
- [ ] **Step 2:** In `version-4/README.md` frame 1a, the sentence describing "the five desk-site links as 30dp pill chips" is now wrong — change five to two and append a dated note naming the three that went. Do NOT repaint frames, edit the `.dc.html`, or touch the PNGs; they are a delivered artifact.
- [ ] **Step 3:** In `docs/CLAUDE-DESIGN-DESK-SCREENS.md:73-74`, remove the three from the tile-grid sentence.
- [ ] **Step 4:** Leave `docs/LIVE-DESK-HAR.md`, `docs/DESK-LAYOUT-FOR-ANDROID.md` and `docs/DIPI_MEMORY_MAP.md` alone — they document the live Drupal server, which still serves these routes, so they remain accurate.
- [ ] **Step 5:** `./gradlew :core:model:test :core:audit:test :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest :app:testDebugUnitTest` (docs-only, but prove the tree is green) and commit `docs: retire three desk destinations from the design contract`.

---

### Task 3 (T3): the screen

**Files:** Modify `feature/course/.../DeskTiles.kt`, `feature/course/.../CentreScreen.kt`, `app/src/test/.../CentreScreenTest.kt`, `app/src/test/.../CentreScreenWideTest.kt`.

**Consumes:** `cardRows` (T1). `CentreScreen`'s and `centreDeskTiles`' signatures are unchanged.

- [ ] **Step 1: failing tests.** Assert: the three removed titles `assertDoesNotExist()`; the five survivors render and Centre Settings / Advanced Search / App Settings still fire `onCentreOps` / `onAdvancedSearch` / `onSettings` and the two chips still fire `onLater` with their unchanged (title, route) pairs; a course whose matrix has no Received row still renders a `Received` line (proving equal heights); the `Confirmed + Expected` label renders with the summed numbers; older-course buttons and upcoming cards share a width (assert both on the same grid — compare `getUnclippedBoundsInRoot().width` of an older row against an upcoming card, allowing a dp of rounding).
- [ ] **Step 2:** red run.
- [ ] **Step 3: S1** — delete the three entries from `centreDeskTiles`.
- [ ] **Step 4: S2** — in the wide branch, drop `.verticalScroll(rememberScrollState())` from the upcoming pane, keeping `weight(0.6f, fill = false)`. Remove the now-unused scroll state. Do not touch the narrow branch's page scroll or the lower pane's.
- [ ] **Step 5: S3** — `CourseMatrixTable` renders `matrix.cardRows` instead of `matrix.highlights`, then the Total row as today. Empty rows render `·` in every cell via the existing cell logic.
- [ ] **Step 6: S4** — restructure `WideLowerPane`: older courses full-width on the same two-column grid as upcoming (use the same `columns` value), desk column stacked beneath at full width. Keep the empty-older-courses case exactly as it is (desk column, three tiles across, 52dp). Older row height stays 42dp.
- [ ] **Step 7:** targeted `./gradlew :app:testDebugUnitTest --tests '*CentreScreen*'` then the full suite. `centreSettingsRowIsReachableWithoutCourses` must pass unmodified.
- [ ] **Step 8:** commit `feat: trim the desk catalogue and standardise the course cards`.

---

### Task 4 (T4): ship 1.24.0

**Files:** `app/build.gradle.kts`, `CLAUDE.md`, `AGENTS.md`.

- [ ] **Step 1:** guard — `git show feat/desk-gap:app/build.gradle.kts | grep -E "versionCode|versionName"` must show 1.19.0/30, else STOP.
- [ ] **Step 2:** bump `36 → 37`, `"1.23.0" → "1.24.0"`.
- [ ] **Step 3:** update the version references in `CLAUDE.md` (~line 5) and `AGENTS.md` (~line 9). Change only the numbers; leave "on `main`" as-is; leave hard rule 9's `version-4/` pointer as-is.
- [ ] **Step 4:** full suite green.
- [ ] **Step 5:** `./gradlew :app:assembleRelease`; report path, bytes, md5.
- [ ] **Step 6:** install on the Pixel C at `10.0.0.144:5555`, launch, confirm `dumpsys` reports 37/1.24.0, process alive, crash buffer empty. Unreachable device = report, not failure.
- [ ] **Step 7:** commit `feat: centre trim at 1.24.0`.

---

## Self-review notes

**Spec coverage.** S1 → T3 Step 3 (code) + T2 (design contract). S2 → T3 Step 4. S3 → T1 + T3 Step 5. S4 → T3 Step 6. Versioning → T4.

**Type consistency.** `plus` and `cardRows` are defined in T1 and consumed by name in T3. No signature changes anywhere: `centreDeskTiles` and `CentreScreen` keep their parameter lists, so `DipiAppUi` is untouched by this plan.

**Known risks.** (1) T3 is the whole visual change in one task — deliberate, because the tile list, the card and the pane all land in the same two test files and splitting them would hand two workers one file. (2) Removing the upcoming scroll is safe only because the desk caps upcoming courses at four and S3 fixes the card height; if either changes, the ceiling clips instead of scrolling. Noted at the call site.
