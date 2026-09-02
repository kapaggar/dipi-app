# T2 — Phone ID + health on CardScreen

**Status:** specified, 2026-08-31
**Baseline:** `main` 1.27.0 / versionCode 42
**Inherits:** Global Constraints in `docs/plans/2026-08-31-ui-gap-closure.md`

## Current

In-memory `sensitiveById` is populated from `SearchPageParser`, wiped on
course/logout/erase, never Room/DataStore/DTO. Tablet `ApplicationsPane`
shows ID + health + `!` + a snack from `selectDeskApp` (once per new
selection). Phone `CardScreen` has no `SensitiveInfo` param. `openCard`
does not fire the snack. Tablet `IdVerificationBlock` / `HealthPanel` paint
`Industry.accent100` (pink-strip class; not dark-aware).

## Do

Thread `state.sensitiveById[card.id]` into `CardScreen`. New phone-native
`LocalDipi` ID + health blocks (not a copy of the tablet Industry blocks).
`openCard` fires the same snack as `selectDeskApp` when `health.isNotEmpty()`
on a **new** open (`previous card id != opening id`). Still no persist/log
of the number.

## Tests this invalidates

None. Add `CardScreenTest`: ID label+number render; “No ID on file” when
absent; health keys render; blocks use `LocalDipi` (no `Industry.accent100`).
Add `deskHealthSnack` coverage: first open with health → snack; second open
of the same id → no re-fire.

## Never-touched

`ApplicationsPane` Industry blocks, `SensitiveInfo` shape, parser ID-priority,
`SearchPageParser`, NPI columns.
