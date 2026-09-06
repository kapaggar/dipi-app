# Seating Plan Landscape Print Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the native seating-plan print fill A4 landscape and show Dhamma seat, name, room, age, and seniority in readable paper blocks.

**Architecture:** Keep `seatingPlanPrintHtml` as a pure formatter over the existing in-memory `TeacherRoll` and `hallLayout`. Add a landscape A4 print attribute beside the existing portrait default, select it only for native hall printing, and use a fixed A4 landscape page box whose hall-grid wrapper flexes into the remaining height.

**Tech Stack:** Kotlin, Android Print Framework, WebView print HTML/CSS, JUnit 4, Robolectric, Gradle 8.9, headless Chrome for PDF inspection.

## Global Constraints

- Keep one gender per A4 landscape page for the configured desk hall.
- Native seating print must remain in-memory and must never request `/seating` or send `?r=`.
- Existing portrait print jobs must retain `ISO_A4` portrait attributes.
- Preserve teacher-at-bottom orientation, descending depth, sevak exclusion, HTML escaping, and conditional backrest legend.
- This user-visible print polish releases as PATCH `1.42.1`, `versionCode` 70.
- Do not commit generated PDFs, student data, `local.properties`, or signing material.

---

### Task 1: Landscape Print Attributes

**Files:**
- Modify: `app/src/test/kotlin/org/dhamma/dipi/staff/SheetViewerTest.kt`
- Modify: `core/ui/src/main/kotlin/org/dhamma/dipi/staff/ui/NativePrint.kt`
- Modify: `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/SheetViewerPane.kt`

**Interfaces:**
- Consumes: `NativePrint.a4Attributes(): PrintAttributes` and `NativePrint.printHtml(Context, String, String, PrintAttributes)`.
- Produces: `NativePrint.a4LandscapeAttributes(): PrintAttributes`; native hall print passes this value while all other callers use the portrait default.

- [ ] **Step 1: Write the failing orientation test**

Extend the existing print-attribute assertion in `SheetViewerTest`:

```kotlin
assertEquals(
    android.print.PrintAttributes.MediaSize.ISO_A4.asLandscape(),
    NativePrint.a4LandscapeAttributes().mediaSize,
)
assertEquals(
    android.print.PrintAttributes.MediaSize.ISO_A4,
    NativePrint.a4Attributes().mediaSize,
)
```

- [ ] **Step 2: Run RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests org.dhamma.dipi.staff.SheetViewerTest.injectedPrintCssLaysStudentChits12UpAndCheckingSlip2Up
```

Expected: compilation fails because `a4LandscapeAttributes` does not exist.

- [ ] **Step 3: Implement the landscape attribute and injectable print attributes**

In `NativePrint`, add:

```kotlin
fun a4LandscapeAttributes(): PrintAttributes =
    PrintAttributes.Builder()
        .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
        .build()
```

Add `attributes: PrintAttributes = a4Attributes()` to `printHtml` and pass it to `manager.print`. In `SheetViewerPane`, pass `NativePrint.a4LandscapeAttributes()` only in the `nativeHallPrintHtml` branch.

- [ ] **Step 4: Run GREEN**

Run the focused command from Step 2. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/ui/src/main/kotlin/org/dhamma/dipi/staff/ui/NativePrint.kt \
  feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/SheetViewerPane.kt \
  app/src/test/kotlin/org/dhamma/dipi/staff/SheetViewerTest.kt
git commit -m "fix: print seating plan on landscape A4"
```

---

### Task 2: Full-Page Operational Seat Blocks

**Files:**
- Modify: `app/src/test/kotlin/org/dhamma/dipi/staff/SeatingPrintTest.kt`
- Modify: `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/SeatingPrint.kt`

**Interfaces:**
- Consumes: `TeacherRoll`, `HallGrid`, `hallLayout`, `backrestSeatLabel`.
- Produces: unchanged `seatingPlanPrintHtml(TeacherRoll, (Gender) -> HallGrid): String` with landscape page geometry and operational detail markup.

- [ ] **Step 1: Write failing content and geometry tests**

Add focused assertions:

```kotlin
@Test fun printUsesTheFullLandscapePageBox() {
    assertTrue(html.contains("size:A4 landscape"))
    assertTrue(html.contains("height:198mm"))
    assertTrue(html.contains("class=\"grid-wrap\""))
    assertTrue(html.contains("grid-template-columns:repeat(4,minmax(0,1fr))"))
}

@Test fun floorSeatShowsOperationalDetails() {
    assertTrue(html.contains("SEAT A1"))
    assertTrue(html.contains("ROOM Mbk-1"))
    assertTrue(html.contains("AGE 40"))
}

@Test fun railSeatShowsOperationalDetails() {
    assertTrue(html.contains("SEAT CW-A1"))
    assertTrue(html.contains("ROOM Mbk-3"))
    assertTrue(html.contains("AGE 40"))
}
```

Add a row with blank room/age and assert its block uses `ROOM —` and `AGE —`. Add room/name text containing `<`, `>`, and `&` and assert escaped output.

- [ ] **Step 2: Run RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests org.dhamma.dipi.staff.SeatingPrintTest
```

Expected: the new landscape, full-page, room, age, fallback, and card-grid assertions fail against the current compact portrait-width markup.

- [ ] **Step 3: Implement minimal formatter and CSS changes**

Update the document head and page layout:

```html
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>
@page{size:A4 landscape;margin:6mm}
html,body{width:100%;height:100%;margin:0}
.hall{height:198mm;box-sizing:border-box;display:flex;flex-direction:column;overflow:hidden}
.grid-wrap{flex:1;min-height:0;display:flex}
table.grid{width:100%;height:100%;border-collapse:collapse;table-layout:fixed}
.cards{display:grid;grid-template-columns:repeat(4,minmax(0,1fr))}
</style>
```

Render each populated floor cell and lower card with separate seat, name, room/age, and old/new elements. Use a helper equivalent to:

```kotlin
private fun printable(value: String): String = esc(value.trim().ifEmpty { "—" })
```

Apply `esc` after adding no labels to user data. Keep empty cells seat-only, keep the existing `hallLayout` order, and place teacher and lower lists after the flexible grid.

- [ ] **Step 4: Run GREEN**

Run the focused command from Step 2. Expected: PASS.

- [ ] **Step 5: Review the generated HTML**

Confirm the diff contains no network call, storage, JavaScript, external resource, or `?r=` path. Run `git diff --check`.

- [ ] **Step 6: Commit**

```bash
git add feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/SeatingPrint.kt \
  app/src/test/kotlin/org/dhamma/dipi/staff/SeatingPrintTest.kt
git commit -m "feat: enlarge seating print blocks"
```

---

### Task 3: Preview, Version, Documentation, and Verification

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `AGENTS.md`
- Modify: `CLAUDE.md`
- Modify: `docs/DESIGN.md`
- Temporary only: `app/src/test/kotlin/org/dhamma/dipi/staff/SeatingPrintPreviewTest.kt`
- Generate outside Git: `/private/tmp/dipi-seating-print-preview.html`, `/private/tmp/dipi-seating-print-preview.pdf`, and PNG page renders.

**Interfaces:**
- Consumes: final `seatingPlanPrintHtml` and synthetic `TeacherRoll` fixture.
- Produces: release metadata `1.42.1` / 70 and a locally inspected preview artifact.

- [ ] **Step 1: Generate an exact formatter preview**

Create a disposable JUnit test that builds synthetic male and female rolls, calls `seatingPlanPrintHtml`, and writes `/private/tmp/dipi-seating-print-preview.html`. Run that single test, then delete the disposable source file before committing.

- [ ] **Step 2: Render and inspect the PDF**

Run headless Chrome:

```bash
'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome' \
  --headless --disable-gpu --no-pdf-header-footer \
  --print-to-pdf=/private/tmp/dipi-seating-print-preview.pdf \
  file:///private/tmp/dipi-seating-print-preview.html
pdfinfo /private/tmp/dipi-seating-print-preview.pdf
pdftoppm -png -r 120 /private/tmp/dipi-seating-print-preview.pdf \
  /private/tmp/dipi-seating-print-preview-page
```

Verify A4 landscape dimensions, two pages, full-width grid use, readable room/age/seat lines, teacher-at-bottom orientation, and no clipped lower blocks.

- [ ] **Step 3: Bump and document the patch release**

Set `versionName = "1.42.1"` and `versionCode = 70`. Update shipped summaries in `AGENTS.md` and `CLAUDE.md`, and append a PATCH 1.42.1 entry to `docs/DESIGN.md` describing landscape print attributes, full-page adaptive grid, operational fields, and unchanged in-memory/no-`?r=` safety.

- [ ] **Step 4: Run full verification**

```bash
./gradlew :core:model:test :core:audit:test \
  :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest \
  :app:testDebugUnitTest
./gradlew :app:assembleRelease
```

Expected: both commands end with `BUILD SUCCESSFUL`.

- [ ] **Step 5: Verify artifact identity**

```bash
/Users/wizops/Android/Sdk/build-tools/34.0.0/aapt dump badging \
  app/build/outputs/apk/release/app-release.apk | head -1
shasum -a 256 app/build/outputs/apk/release/app-release.apk
git diff --check
```

Expected: package `org.dhamma.dipi.staff`, versionCode 70, versionName 1.42.1.

- [ ] **Step 6: Commit release metadata**

```bash
git add app/build.gradle.kts AGENTS.md CLAUDE.md docs/DESIGN.md
git commit -m "release: bump DIPI Staff to 1.42.1"
```

---

### Task 4: Apply Print-Preview Layout Feedback

**Files:**
- Modify: `app/src/test/kotlin/org/dhamma/dipi/staff/SeatingPrintTest.kt`
- Modify: `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/SeatingPrint.kt`
- Modify: `docs/superpowers/specs/2026-09-05-seating-print-landscape-design.md`
- Modify: `docs/DESIGN.md`

**Contract:** Render the floor grid only through the furthest occupied column
and depth, preserve internal gaps, place chowky/chair cards in a vertical side
rail with A1 nearest the teacher, keep the teacher marker below row 1 without
overlap, and omit unseated rows from this occupied-seat printout.

- [ ] Add failing formatter tests for occupied-footprint trimming, vertical
  rail structure/order, teacher-after-grid placement, and unseated omission.
- [ ] Implement the page-body/main/rail layout and occupied footprint helper.
- [ ] Run the focused formatter tests and inspect generated markup.
- [ ] Build and install the debug APK, then inspect the Pixel C print preview.
- [ ] Run the prescribed full suite and assemble/verify the release APK.
