# T1 — Day-11 Board chip on the design's fourth-line row

**Status:** specified, 2026-08-31
**Baseline:** `main` 1.27.0 / versionCode 42
**Inherits:** Global Constraints in `docs/plans/2026-08-31-ui-gap-closure.md`

## Current

`EXPORT_SHELVES` in `feature/desk/.../BoardPane.kt` is a 3×4 of twelve labels.
`SheetExport.Day11Report` (`"Course summary report"`) already fetches
`GET /report-day11/{cid}/{courseId}`. Phone hub overflow reaches it via
`hubSheetLabel`. The Board chip is withheld because a 13th flex chip would
break the shelf grid.

`version-4/DIPI Staff v4.dc.html:579` draws a **full-width dashed 40px row**
under the three shelves (`Day 11 · Course summary report` + `GAP — NOT IN 1.22.0`).
`BoardPaneTest.noDayElevenGapMarkerIsDrawn` (~line 183) asserts both strings
do not exist. That R2 ruling is stale: transport shipped in 1.27.0.

## Do

Keep `EXPORT_SHELVES` at 12. Under the shelves add one full-width 40 dp row.
Visible title: `Day 11 · Course summary report`.
`onExport` **must** fire `"Course summary report"` so `SheetExport.fromLabel`
+ existing `openSheet` work. No `?r=`. No dashed GAP badge.

## Tests this invalidates

Retarget `noDayElevenGapMarkerIsDrawn` only. Replace with:
fourth-row chip exists; `"GAP — NOT IN 1.22.0"` still absent; tap fires
`onExport("Course summary report")`; shelf `export-chip` count stays 12.
`twelveExportsSitOnThreeShelvesInTheDesignsGrouping` stays unmodified.

## Never-touched

`SheetExport.kt`, `SheetRoutes`, `StaffApi`, `hubSheetLabel`, `DeskViewModel`,
`DipiAppUi`, `DeskPanesTest.kt`.
