# Desk Polish — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tighten the centre screen, strip three redundant strings, make the room chart's grid shape editable per gender and section, and turn the centre-settings toggles into real switches with the room chart promoted to the top.

**Architecture:** One new pure model (`RoomLayout`) persisted for free inside the already-serialized `CentreOpsPrefs`. Everything else is presentation, split so no two concurrent workers share a file. The dead band on the centre screen is a one-word fix (`fill = false`), not a restructure.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Robolectric + `createComposeRule`, JUnit4, Gradle 8.9 / JDK 20.

**Spec:** `docs/specs/2026-08-27-desk-polish-spec.md`

## Global Constraints

- **No client-side access control**; render what the server sends.
- **Never persist or log NPI.** Nothing here touches applicant data.
- **Backend immutable** — `/Users/wizops/DIPI/dipi-web` is read-only reference. Room layout is device-local; there is no server field for it.
- **Colours from `Industry`/`LocalDipi` tokens only** — five user-selectable skins. No hard-coded hex, including for the new `Switch`.
- **Tap targets ≥ 48dp** on any new control. This session already fixed one sub-48dp control; do not add another.
- **Only the assertions named in the spec's "Tests this invalidates" may be modified.** Anything else — stop and report.
- **Test command** (never bare `./gradlew test`; it drags in `:app:testReleaseUnitTest`, broken here for a pre-existing unrelated reason):
  ```bash
  ./gradlew :core:model:test :core:audit:test \
            :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest \
            :app:testDebugUnitTest
  ```
- Commits carry **no** `Co-Authored-By` / `Claude-Session` trailers. gpg signing is on.

---

## Multi-agent workflow layout

```
Phase 1  ── W1 model ─────────────┐   (alone; everything else may depend on it)
                                  ▼
Phase 2  ── W2 centre screen ─────┐   (3 parallel, disjoint files)
         ── W3 rooms chart ───────┤
         ── W4 centre settings ───┤
                                  ▼
Phase 3  ── W5 chrome + wiring ───┤   (owns both app-level files)
                                  ▼
Phase 4  ── Integrator ───────────┘   suite, SemVer, release, Pixel C
```

| Worker | Owns exclusively | Needs |
|---|---|---|
| W1 model | `core/model/.../RoomLayout.kt` (new), `core/model/.../Models.kt`, `core/model/src/test/.../RoomLayoutTest.kt` (new) | — |
| W2 centre screen | `feature/course/.../CentreScreen.kt`, `app/src/test/.../CentreScreenTest.kt`, `app/src/test/.../CentreScreenWideTest.kt` | — |
| W3 rooms chart | `feature/course/.../RoomsScreen.kt`, `app/src/test/.../RoomsScreenTest.kt` | W1 |
| W4 centre settings | `feature/course/.../CentreOpsScreen.kt`, `app/src/test/.../CentreOpsScreenTest.kt` | — |
| W5 chrome + wiring | `feature/desk/.../DeskShell.kt`, `feature/desk/.../BoardPane.kt`, `app/.../ui/DipiAppUi.kt`, `app/.../ui/DeskViewModel.kt`, `app/src/test/.../DeskShellTest.kt` | W1, W3 |
| Integrator | `app/build.gradle.kts`, `CLAUDE.md`, `AGENTS.md` | all |

W5 owns both app-level files precisely because the Board-heading removal and the room-layout wiring both land there; splitting them would collide.

---

### Task 1 (W1): RoomLayout model

**Files:**
- Create: `core/model/src/main/kotlin/org/dhamma/dipi/staff/model/RoomLayout.kt`
- Modify: `core/model/src/main/kotlin/org/dhamma/dipi/staff/model/Models.kt` (one field on `CentreOpsPrefs`)
- Test: `core/model/src/test/kotlin/org/dhamma/dipi/staff/model/RoomLayoutTest.kt`

**Interfaces:**
- Produces: `RoomLayout` with `columnsFor`, `withColumns`, and companion `DEFAULT_COLUMNS`/`MIN_COLUMNS`/`MAX_COLUMNS`/`key`/`rowsFor`; plus `CentreOpsPrefs.roomLayout: RoomLayout = RoomLayout()`. W3 and W5 bind to these exact names.

- [ ] **Step 1: Write the failing test**

```kotlin
package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RoomLayoutTest {

    @Test
    fun anUnsetBlockFallsBackToTheDefault() {
        assertEquals(RoomLayout.DEFAULT_COLUMNS, RoomLayout().columnsFor(Gender.M, "Mbk"))
    }

    @Test
    fun genderAndSectionAreIndependentScopes() {
        val l = RoomLayout()
            .withColumns(Gender.M, "Mbk", 7)
            .withColumns(Gender.F, "Fbk", 5)
        assertEquals(7, l.columnsFor(Gender.M, "Mbk"))
        assertEquals(5, l.columnsFor(Gender.F, "Fbk"))
        // A section the registrar never touched keeps the default.
        assertEquals(RoomLayout.DEFAULT_COLUMNS, l.columnsFor(Gender.M, "Guest"))
        // Same section name under the other gender is a different block.
        assertEquals(RoomLayout.DEFAULT_COLUMNS, l.columnsFor(Gender.F, "Mbk"))
    }

    @Test
    fun columnsAreClampedOnWriteAndOnRead() {
        assertEquals(RoomLayout.MAX_COLUMNS, RoomLayout().withColumns(Gender.M, "Mbk", 99).columnsFor(Gender.M, "Mbk"))
        assertEquals(RoomLayout.MIN_COLUMNS, RoomLayout().withColumns(Gender.M, "Mbk", 0).columnsFor(Gender.M, "Mbk"))
        // Corrupt stored JSON must not produce a zero-column grid.
        assertEquals(RoomLayout.MIN_COLUMNS, RoomLayout(mapOf("M|Mbk" to -3)).columnsFor(Gender.M, "Mbk"))
    }

    @Test
    fun rowsAreDerivedByCeilingDivision() {
        assertEquals(10, RoomLayout.rowsFor(rooms = 70, columns = 7))
        assertEquals(18, RoomLayout.rowsFor(rooms = 70, columns = 4))
        assertEquals(1, RoomLayout.rowsFor(rooms = 3, columns = 7))
        assertEquals(0, RoomLayout.rowsFor(rooms = 0, columns = 7))
        assertEquals(0, RoomLayout.rowsFor(rooms = 5, columns = 0))
    }

    @Test
    fun rewritingABlockReplacesRatherThanAccumulates() {
        val l = RoomLayout().withColumns(Gender.M, "Mbk", 7).withColumns(Gender.M, "Mbk", 3)
        assertEquals(3, l.columnsFor(Gender.M, "Mbk"))
        assertEquals(1, l.columns.size)
    }
}
```

- [ ] **Step 2: Run it and watch it fail**

`./gradlew :core:model:test --tests '*RoomLayoutTest*'` → FAIL, `Unresolved reference: RoomLayout`.

- [ ] **Step 3: Write `RoomLayout.kt`** using the exact code in spec S4.

- [ ] **Step 4: Add the field to `CentreOpsPrefs`**

In `Models.kt`, `CentreOpsPrefs` (~line 217) currently ends with
`val rooms: List<AccoRoom> = emptyList(),`. Add beneath it, inside the class:

```kotlin
    /** Chart grid shape per gender+section block. Device-local; wiped by Erase-all. */
    val roomLayout: RoomLayout = RoomLayout(),
```

Defaulted so previously-stored `centre_ops` JSON still decodes.

- [ ] **Step 5: Targeted test, then the full suite.** Both green.

- [ ] **Step 6: Commit**

```bash
git add core/model/src/main/kotlin/org/dhamma/dipi/staff/model/ core/model/src/test/kotlin/org/dhamma/dipi/staff/model/RoomLayoutTest.kt
git commit -m "feat: model the room chart grid shape per gender and section"
```

---

### Task 2 (W2): Centre screen — two strings and the dead band

**Files:**
- Modify: `feature/course/.../CentreScreen.kt`
- Modify: `app/src/test/.../CentreScreenTest.kt`, `app/src/test/.../CentreScreenWideTest.kt` (only assertions the spec names)

- [ ] **Step 1: Drop "from your account" (spec S1.1)**

`CentreScreen.kt:170` renders
`"${centre?.name ?: "Centre"} · from your account · ${session.displayName}"`.
Change to `"${centre?.name ?: "Centre"} · ${session.displayName}"`. Nothing else about the `Text` changes.

- [ ] **Step 2: Delete the older-courses sub-line (spec S1.2)**

Remove the entire `Text` at `CentreScreen.kt:244` whose content is
`"Teacher list · valuables · seating — check-in is closed"`, including its
`modifier`. Keep the `"Older courses"` heading above it. Adjust the heading's
bottom padding so the spacing still reads correctly with the sub-line gone.

- [ ] **Step 3: Close the dead band (spec S2)**

In the wide branch, change the upcoming region's modifier from
`Modifier.weight(0.6f)` to `Modifier.weight(0.6f, fill = false)`. Change
nothing else — not the 0.4f sibling, not the two scroll states, not the header.

- [ ] **Step 4: Update only the assertions the spec names**

Search both test files for assertions on the two deleted strings and retarget
them: assert the header now reads `"{centre} · {displayName}"`, and that the
old sub-line no longer exists (`assertDoesNotExist()`), so the deletion is
proven rather than merely untested. Touch nothing else. If a third assertion
appears to need changing, STOP and report BLOCKED.

- [ ] **Step 5: Add coverage for the tight layout**

In `CentreScreenWideTest.kt` (which already runs at `@Config(qualifiers = "w1240dp-h844dp-land")`), add a test with a short upcoming list asserting that "Older courses" is reachable without scrolling past a gap — practically: assert both the upcoming content and the "Older courses" heading are displayed in the same frame.

- [ ] **Step 6: Targeted then full suite; commit**

```bash
git add feature/course/src/main/kotlin/org/dhamma/dipi/staff/course/CentreScreen.kt app/src/test/kotlin/org/dhamma/dipi/staff/CentreScreenTest.kt app/src/test/kotlin/org/dhamma/dipi/staff/CentreScreenWideTest.kt
git commit -m "feat: tighten the centre screen and drop two redundant lines"
```

---

### Task 3 (W3): Editable room chart

**Files:**
- Modify: `feature/course/.../RoomsScreen.kt`
- Test: `app/src/test/kotlin/org/dhamma/dipi/staff/RoomsScreenTest.kt`

**Interfaces:**
- Consumes: `RoomLayout` (Task 1).
- Produces: `RoomsScreen(rooms, genderFilter, layout: RoomLayout = RoomLayout(), onColumns: (Gender, String, Int) -> Unit = { _, _, _ -> }, onPick, onBack)`. **Both new parameters must be defaulted** so the existing call site keeps compiling — W5 wires them in a later phase.

- [ ] **Step 1: Write the failing test**

Cover: a block with no stored columns renders the default 4 per row; a stored 7 renders 7; the header shows both the column and derived row counts; `+` and `−` call `onColumns` with the incremented/decremented value and the right gender+section; `−` is disabled at `MIN_COLUMNS` and `+` at `MAX_COLUMNS`. Assert row counts via the header text rather than by counting nodes, which is brittle.

- [ ] **Step 2: Run it and watch it fail.**

- [ ] **Step 3: Implement (spec S4)**

Replace the hardcoded `chunked(4)` (`RoomsScreen.kt:75`) and the trailing
`repeat(4 - rowRooms.size)` (`:108`) with `layout.columnsFor(g, section)`.
Extend the block header to
`"{label} · {section} · {n} rooms · {c} per row · {r} rows"` using
`RoomLayout.rowsFor`. Add the stepper on the header's trailing edge: `−`, the
current count, `+`. Steppers are ≥48dp tap targets, disabled at the bounds,
and use `Industry`/`LocalDipi` tokens only.

- [ ] **Step 4: Targeted then full suite; commit**

```bash
git add feature/course/src/main/kotlin/org/dhamma/dipi/staff/course/RoomsScreen.kt app/src/test/kotlin/org/dhamma/dipi/staff/RoomsScreenTest.kt
git commit -m "feat: let the registrar set the room chart grid shape per block"
```

---

### Task 4 (W4): Centre settings — switches and room chart first

**Files:**
- Modify: `feature/course/.../CentreOpsScreen.kt`
- Modify: `app/src/test/.../CentreOpsScreenTest.kt` (only the ON/OFF assertions)

- [ ] **Step 1: Promote the room chart (spec S5.2)**

Move the `Room chart` control from after the RESULT card to directly beneath
the "Centre settings" heading and its Back control, above the three switches.
Render it as a full-width `deskCard` row with the label at **18.sp** in
`DipiCondensed` plus the sub-line `"Rooms, sections and chart layout"`. It
keeps calling `onOpenRooms`. Everything below keeps its current relative order.

- [ ] **Step 2: Real switches (spec S5.1)**

In `ToggleRow`, replace the trailing `DeskKicker("ON"/"OFF")` with a Material 3
`Switch(checked = on, onCheckedChange = { onClick() })`. The row stays
clickable as a whole. Title and note unchanged. Do not hard-code switch
colours — let the theme supply them.

- [ ] **Step 3: Retarget only the ON/OFF assertions**

`CentreOpsScreenTest` asserts `"ON"`/`"OFF"` node counts for a given prefs
combination. With a `Switch` the state is semantics, not text. Retarget to
`assertIsOn()` / `assertIsOff()` on the switch nodes, keeping the same prefs
combination so the test proves the same thing. Give each switch a stable
`testTag` so the assertions can address them.

**`centreSettingsRowIsReachableWithoutCourses` and every other assertion stay
untouched.** A third changed assertion means STOP and report BLOCKED.

- [ ] **Step 4: Targeted then full suite; commit**

```bash
git add feature/course/src/main/kotlin/org/dhamma/dipi/staff/course/CentreOpsScreen.kt app/src/test/kotlin/org/dhamma/dipi/staff/CentreOpsScreenTest.kt
git commit -m "feat: switch toggles and a promoted room chart on centre settings"
```

---

### Task 5 (W5): Desk chrome and room-layout wiring

**Files:**
- Modify: `feature/desk/.../DeskShell.kt`, `feature/desk/.../BoardPane.kt`
- Modify: `app/.../ui/DipiAppUi.kt`, `app/.../ui/DeskViewModel.kt`
- Modify/Create: `app/src/test/.../DeskShellTest.kt`

- [ ] **Step 1: Fix the crumb (spec S3.1, S3.2)**

In `DeskShell.kt`, `DeskCourse.crumbLine` (~line 67) currently joins
`label · dates · dayChip`. Drop `label`; join `dates · dayChip` only. Keep
`label` as a property — the rail uses it. In `DeskTopBar` (~line 216) raise the
crumb `fontSize` from `13.sp` to `17.sp`, leaving face, tracking, colour and
`maxLines = 1` alone.

- [ ] **Step 2: Remove the Board heading (spec S3.3)**

In `BoardPane`, delete the 40sp `Text` rendering
`"{dayLabel} at {centreName}"` / `centreName`. The subtitle beneath stays. If
`centreName` is then unused, remove it from the signature and update the single
call site in `DipiAppUi.kt` — which you own. Leave no dead parameter.

- [ ] **Step 3: Wire the room layout**

`RoomsScreen` now takes `layout: RoomLayout` and
`onColumns: (Gender, String, Int) -> Unit`. In `DeskViewModel`, add a handler
that reads the current `CentreOpsPrefs`, applies `withColumns`, and persists via
the existing `sessionStore.setCentreOps` path used by the laundry/valuables/groups
toggles — follow that pattern exactly. In `DipiAppUi.kt`, pass
`state.centreOps.roomLayout` and the new handler to `RoomsScreen`.

- [ ] **Step 4: Add coverage**

Assert the crumb no longer starts with the centre name and renders at the new
size, and that the Board no longer shows the bare centre name. Add tests rather
than rewriting existing ones; only assertions the spec names may change.

- [ ] **Step 5: Targeted then full suite; commit**

```bash
git add feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/ app/src/main/kotlin/org/dhamma/dipi/staff/ui/ app/src/test/kotlin/org/dhamma/dipi/staff/DeskShellTest.kt
git commit -m "feat: tighten desk chrome and persist the room chart layout"
```

---

### Task 6 (Integrator): Ship 1.22.0

**Files:** `app/build.gradle.kts`, `CLAUDE.md`, `AGENTS.md`

- [ ] **Step 1:** Confirm `feat/desk-gap` still holds 1.19.0/30 so 1.22.0/35 is free; if not, STOP and report.
- [ ] **Step 2:** Bump `versionCode = 34` → `35`, `versionName = "1.21.1"` → `"1.22.0"`.
- [ ] **Step 3:** Update `CLAUDE.md` (~line 5) and `AGENTS.md` (~line 9) to 1.22.0 / 35. Change only the numbers; **leave "on `main`" exactly as it is.**
- [ ] **Step 4:** Full suite, green.
- [ ] **Step 5:** `./gradlew :app:assembleRelease`; report path, byte size and md5.
- [ ] **Step 6:** Install on the Pixel C at `10.0.0.144:5555`, launch, and confirm via `dumpsys` that versionCode 35 / versionName 1.22.0 landed; confirm the process is alive and the crash buffer is empty. Device unreachable is not a failure — report it.
- [ ] **Step 7:** Commit.

---

## Self-review notes

**Spec coverage.** S1.1 → Task 2 Step 1. S1.2 → Task 2 Step 2. S2 → Task 2 Step 3. S3.1/S3.2 → Task 5 Step 1. S3.3 → Task 5 Step 2. S4 → Tasks 1 and 3, wired in Task 5 Step 3. S5.1 → Task 4 Step 2. S5.2 → Task 4 Step 1. "Tests this invalidates" → Tasks 2, 4 and 5. Versioning → Task 6.

**Type consistency.** `RoomLayout` and its companion members are defined in Task 1 and consumed under those names in Tasks 3 and 5. `RoomsScreen`'s two new parameters are defaulted in Task 3 and supplied in Task 5, so the intermediate state compiles.

**Known risk.** Task 5 is the only task touching `DipiAppUi.kt` and `DeskViewModel.kt`, and it depends on Task 3's signature. It must run after Task 3 completes, never beside it.
