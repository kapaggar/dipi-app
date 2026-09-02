# Day-11 Board chip — spec (T1)

**Status:** proposed, 2026-08-31
**Baseline:** `main` @ `0ce3342`, 1.27.0 / versionCode 42.
**Origin:** repo scan 2026-08-31 — `SheetExport.Day11Report` has complete, tested
transport (`GET /report-day11/{cid}/{courseId}` → streamed PDF) but its only UI
control is the phone hub ⋯ overflow, which composes only under 1100dp. The Pixel C
is 1280dp, so on the primary device the export is unreachable.

## Decision — place the chip as the design's own fourth-line row

The v4 design file already draws the answer: `version-4/DIPI Staff v4.dc.html:579-582`
puts "Day 11 · Course summary report" on its **own row below the three shelves**
(`margin-top:12px; height:40px`, content-width, NOT `flex:1`), left-aligned under the
`FOR THE TEAM` shelf. It was drawn dashed with a `GAP — NOT IN 1.22.0` badge because
the transport did not exist then. It does now (cherry-picked at 1.27.0), so the row
ships as a **normal chip**: solid `1dp` hairline border (same as every other chip),
no badge, same 40dp height, 13dp horizontal padding, `↓` glyph, Roboto 13.5sp label.
The 3×4 shelf grid is untouched — this is the only option the design file draws.

Fold budget (verified against `version-4/README.md` frame 1f): content area
776dp; existing Board stack ≈ 597dp; +52dp for the fourth line ≈ 649dp. Fits.

## Changes

`feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/BoardPane.kt`:

1. After the `EXPORT_SHELVES.forEach { ... }` loop (which ends around line 160),
   add a fourth-line chip. Reuse the exact chip visual from the shelf loop but
   content-width instead of `weight(1f)`:

```kotlin
// Day 11 lands after the course; the design's own fourth line keeps it out
// of the urgent shelves (v4 frame 1f, fourth-line row).
Row(
    Modifier
        .padding(top = 12.dp)
        .height(40.dp)
        .deskCard(shape = ChipShape, elevation = 0.dp)
        .clickable { onExport("Course summary report") }
        .padding(horizontal = 13.dp)
        .testTag("export-chip"),
    horizontalArrangement = Arrangement.spacedBy(9.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    DeskIcon(DeskIconKind.Download, 13.dp, Industry.accent400)
    Text("Day 11 · Course summary report", fontSize = 13.5.sp, maxLines = 1, color = Industry.neutral800)
}
```

   The label shown is the design's row text ("Day 11 · Course summary report");
   the `onExport` argument must be exactly `"Course summary report"` — that is
   `SheetExport.Day11Report.label` and what `SheetExport.fromLabel` resolves
   (`core/model/.../SheetExport.kt:28`). Do not add it to `EXPORT_SHELVES`.

2. Rewrite the file-header KDoc paragraph at `BoardPane.kt:37-43` ("Day 11 ·
   Course summary report is still deliberately absent HERE…") to record that the
   chip now ships on the design's fourth-line row, placed 2026-08-31.

`docs/specs/2026-08-28-v4-design-pass-spec.md` (ruling R2, lines ~40-45): add a
dated correction note — the stated reason ("lives on unmerged `feat/desk-gap`")
went stale when `0ce3342` cherry-picked the export; the chip ships per this spec.

## Tests

`app/src/test/kotlin/org/dhamma/dipi/staff/BoardPaneTest.kt`:

- **Line ~185 currently asserts the label does NOT exist. Flip it**: assert the
  node with text `"Day 11 · Course summary report"` exists and is displayed.
  (Authorized test edit — the assertion's subject is this spec's direct change.)
- Add: clicking the new chip invokes `onExport` with exactly
  `"Course summary report"` (capture with a `var got: String?`).
- Add: the twelve shelf chips are unchanged — assert `onAllNodesWithTag("export-chip")`
  count is 13 and each of the three `export-shelf-*` tags still holds 4 chips.

Never touched: `SheetExportTest`, `ExportMockTest`, `CourseHubScreenTest`
(`hubSheetLabel` already routes the phone; nothing changes there), every other
Board assertion.

## Constraints

Tokens via `Industry` only, no inline hex; the chip keeps ≥40dp height with the
full-row touch target; no transport change (never send an `r` param); commit
messages carry no agent trailers; never bare `./gradlew test`.
