# Centre "Course Report" chip fetches the real export — spec (T4)

**Status:** proposed, 2026-08-31
**Baseline:** `main` @ `0ce3342`, 1.27.0 / versionCode 42.
**Origin:** repo scan 2026-08-31 — `SheetExport.CourseReport` has complete,
tested transport (scrape `GET /centre/{cid}/course-report` →
`CourseReportFormParser` → `POST` → streamed CSV; `StaffApi.kt:183-193, 307-321`,
pinned by `ExportMockTest`) and is live from the tablet Board chip. But the
Centre screen's identically named desk-site chip routes to
`onLater("Course Report", …)` → the `DeskActionScreen` "implementation is the
next slice" placeholder (`DipiAppUi.kt:187` area). Same name, one real, one dead.

## Why this is NOT a one-line swap

`DeskViewModel.openSheet(label)` (`:708-715`) requires `_state.value.course != null`
— it fetches with `course.centreId`/`course.id`. On the Centre screen no course is
open, so `openSheet` would silently no-op. However, the Course Report transport is
**centre-scoped**: `courseReport(centreId)` (`StaffApi.kt:307`) never uses a
courseId (the desk form itself carries the course choice). So the fix is a
centre-scoped ViewModel entry, not a reroute to `openSheet`.

## Changes

### 1. `app/src/main/kotlin/org/dhamma/dipi/staff/ui/DeskViewModel.kt`

Add beside `openSheet` (after line 715):

```kotlin
/**
 * The Centre screen's Course report chip. The export is centre-scoped —
 * `SheetRoute.ReportForm` never reads the courseId — so it works with no
 * course open; 0 is a deliberate dummy. Document payloads land in
 * `openDoc`, collected above the width gate, so this works on both sizes.
 */
fun openCourseReport() {
    val cid = _state.value.session?.centres?.firstOrNull()?.id?.value ?: return
    val label = SheetExport.CourseReport.label
    _state.update { it.copy(sheetView = SheetViewUi(title = label)) }
    viewModelScope.launch {
        resolveSheet(label) { sheetFetch(SheetExport.CourseReport, cid, 0) }
    }
}
```

`resolveSheet` already handles all three outcomes (`:730-752`): `Document` →
`sheetView = null` + `openDoc` (system viewer via `DipiAppUi.kt:91-95`, above the
width gate); `NotAvailable` → error snackbar; auth failure → sign-in. The
transient non-null `sheetView` while loading is the same pattern the phone hub
already uses for its Document exports.

### 2. `feature/course/src/main/kotlin/org/dhamma/dipi/staff/course/DeskTiles.kt`

Data-driven dispatch (the KDoc explicitly forbids fragile title matching):

```kotlin
data class DeskTileSpec(
    val title: String,
    val route: String,
    val action: DeskTileAction? = null,
    /** SheetExport label when the chip fetches a real export instead of the placeholder. */
    val sheet: String? = null,
)
```

and in `centreDeskTiles`:

```kotlin
DeskTileSpec("Course Report", "centre/$centreId/course-report", sheet = "Course report"),
```

(`"Course report"` is `SheetExport.CourseReport.label` — casing matters.)
Bulk Mail is unchanged (`sheet = null` → placeholder, honest: no transport exists).

### 3. `feature/course/src/main/kotlin/org/dhamma/dipi/staff/course/CentreScreen.kt`

Add a defaulted callback and use it in the chip row (currently `:471-473`):

```kotlin
onExport: (String) -> Unit = {},
```

```kotlin
tiles.filter { it.action == null }.forEach { tile ->
    DeskSiteChip(tile.title) {
        tile.sheet?.let(onExport) ?: onLater(tile.title, tile.route)
    }
}
```

### 4. `app/src/main/kotlin/org/dhamma/dipi/staff/ui/DipiAppUi.kt`

At the `CentreScreen` call site, pass `onExport = { vm.openCourseReport() }`.
(Only one sheet-bearing chip exists; if a second centre-scoped export ever
appears this becomes a label dispatch — YAGNI now.)

## Tests

`app/src/test/kotlin/org/dhamma/dipi/staff/CentreScreenTest.kt` (this spec's
direct subject — authorized additions, no existing assertion weakened):

- `courseReportChipFiresExport` — compose `CentreScreen` with an `onExport`
  capture and an `onLater` capture; tap "Course Report"; assert `onExport`
  received `"Course report"` and `onLater` was never called.
- `bulkMailChipStillRoutesToPlaceholder` — tap "Bulk Mail"; assert `onLater`
  received `("Bulk Mail", "centre/{cid}/bulk-mail-schedule")` and `onExport`
  was never called.

Never touched: `ExportMockTest` (transport already pinned), `BoardPaneTest`
("Course report" Board chip unchanged), `DeskActionScreenTest`,
`CentreScreenTest`'s existing tile/chip assertions.

## Constraints

Never send an `r` query param on any sheet GET (transport untouched — reused
as-is); sheets stay display-only (`cacheDir/sheets`, wiped on logout); tokens via
`LocalDipi`/`Industry`; no agent trailers; never bare `./gradlew test`.
