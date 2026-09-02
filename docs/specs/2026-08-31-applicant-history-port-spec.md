# T6 — Applicant desk history port from feat/desk-gap

**Status:** specified, 2026-08-31
**Baseline:** after T5, 1.30.0 / 46
**Inherits:** Global Constraints in `docs/plans/2026-08-31-ui-gap-closure.md`

## Current

Main's `ApplicantHistory` is dataset `first` / `recent` / `counts` only.
`feat/desk-gap` has `/app-courses`, `/app-activity`, `/app-clarifications`
+ clarification PDF, `HtmlForms`/`HtmlTables`, `ApplicantHistoryParser`.
Do **not** merge the branch. Do **not** reuse the name `ApplicantHistory`.

## Do — seven `git show` steps

1. Models: `ApplicantDeskHistory` + row types only from desk-gap `DeskReads.kt`.
2. Scrapers: `HtmlForms` + `HtmlTables`. Do not replace `CourseReportFormParser`.
3. Parser + `ApplicantHistoryParserTest`.
4. API: three GETs + `SheetTransport.clarificationPdf` →
   `GET /show-clarification/{appId}/{clarId}` into `cacheDir/sheets`. No `r`.
5. Repo: `loadAppCourses` / `loadAppActivity` / `loadAppClarifications` /
   `fetchClarification`.
6. VM: `expandHistory` + `openClarification`. Keep `openCard`'s dataset merge.
7. UI: `ApplicantHistorySections` on phone `CardScreen` and tablet
   `ApplicationsPane`.

## Do-not-port

`DeskSearchFields` / `POST /search-app`; Manage Courses / Daily Activity /
SMS / Letters / `CentreEditScreen`; desk-gap `centreDeskTiles`; widening
`hubSheetLabel` to Page exports; `docs/qa-1.19.0/`.

## Tests this invalidates

None. Add parser fixtures, `clarificationPdfStreamsToSheetsCache`, expand +
PDF coverage on card/applications.

## Never-touched

`POST /search-app`, retired screens, `hubSheetLabel` keys, dataset `ApplicantHistory`.
