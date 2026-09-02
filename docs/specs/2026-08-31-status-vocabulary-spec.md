# T3 — deriveStatuses() prefers the parsed select

**Status:** specified, 2026-08-31
**Baseline:** `main` 1.27.0 / versionCode 42
**Inherits:** Global Constraints in `docs/plans/2026-08-31-ui-gap-closure.md`

## Current

No `deriveStatuses()`. Live path: `StaffRepository.refreshApplicants` writes
`lastStatuses` from roster keys; `loadStatuses()` returns that.
`DeskViewModel.ensureWorklist` does `ApplicantStatus.mergeChoices(list)`.
`SearchPage.statuses` from `#edit-app-status` is parsed and discarded.
`mergeChoices` and the sheet hide Approved. Custom / `changeStatusFor` can
still **send** `Approved`. `StatusWrite.query` does not refuse it.

## Do

Add `ApplicantStatus.deriveStatuses(select, roster): List<String>` — prefer
non-empty parsed select, else roster, then `mergeChoices` (Approved stripped).
`refreshApplicants` stores the result; `loadStatuses` / `ensureWorklist` use it.
Keep `SHEET_CHOICES` as the empty fallback.

Hard rule 3 extra: `confirmStatus` / `changeStatusFor` / `StaffRepository.changeStatus`
no-op (error snack) when the string equals `Approved` ignore-case.
`StatusWrite.query` refuses `Approved` so it cannot reach the GET params.
Do **not** invent a status engine.

## Tests this invalidates

None of the existing merge tests. Add:
`deriveStatusesPrefersSelectOverRoster`,
`deriveStatusesFallsBackToRosterWhenSelectEmpty`,
`deriveStatusesNeverIncludesApproved`,
`queryRefusesApproved`.

## Never-touched

`BoardPane`, `CardScreen`, `DipiAppUi`, sheet GET paths, `statusColors()`.
