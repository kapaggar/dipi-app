# T4 — Centre Course Report chip fetches the CSV

**Status:** specified, 2026-08-31
**Baseline:** `main` 1.27.0 / versionCode 42
**Inherits:** Global Constraints in `docs/plans/2026-08-31-ui-gap-closure.md`

## Current

`DeskTileSpec` is `(title, route, action?)`. Course Report is `action == null`
→ `CentreScreen.onLater` → `openLater` → `DeskActionScreen` placeholder.
`openSheet(label)` returns if `course == null`. Course-report transport is
centre-scoped (`StaffApi.courseReport` uses `centreId` only). The Board chip
already fetches via `openSheet("Course report")`.

## Do

Add `val sheet: String? = null` on `DeskTileSpec`. Course Report:
`sheet = "Course report"`. Bulk Mail stays `null`.
`CentreScreen` grows a defaulted `onExport`/`onSheet: (String) -> Unit = {}`
and fires it when `spec.sheet != null`.
Add `DeskViewModel.openCourseReport()` that resolves `SheetExport.CourseReport`
with `centre.id` and `course?.id ?: 0`. DipiAppUi wires it.

## Tests this invalidates

Retarget `theFiveSurvivingTilesRenderAndFireTheirCallbacks` so Course Report
fires `onExport` / `onSheet` and does **not** fire `onLater`. Keep Bulk Mail
on `onLater`. Do not retarget `centreSettingsRowIsReachableWithoutCourses`.
Add `openCourseReportFetchesWithoutAnOpenCourse` if a VM harness is present.

## Never-touched

`EXPORT_SHELVES`, `hubSheetLabel` keys, Bulk Mail, `SheetExport` enum.
