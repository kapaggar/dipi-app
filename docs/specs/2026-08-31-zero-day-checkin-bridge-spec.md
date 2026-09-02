# T5 — Phone Zero Day writes the same checkIns map as the tablet

**Status:** specified, 2026-08-31
**Baseline:** `main` 1.27.0 / versionCode 42
**Inherits:** Global Constraints in `docs/plans/2026-08-31-ui-gap-closure.md`

## Current

`ZeroDayDraft` is in-memory only. `setZeroDay*` / `pickRoom` / `markAttended`
never call `persistCheckIns`. Phone Sync rooms walks `state.checkIns` —
tablet/pulled records only. Tablet `saveDeskMark` uses `deskSaveSnack`
(blank room blocked). `RoomAllocSync.params` always posts `l=""` `v=""`.
Tablet `DialogToggleRow` is double-clickable (row + `DeskToggle`).

## Do

- Delete `ZeroDayDraft` and `zeroDayDrafts` / `patchDraft` / `setZeroDay*`.
- `ZeroDayScreen` reads/writes `Map<ApplicantId, CheckInRecord>`.
- Phone seating / room / mark-attended go through `setDeskSeat` / `setDeskRoom`
  / `saveDeskMark`. Same no-room guard, same snack.
- Laundry / valuables become boolean toggles, **double-fire-safe** (one
  clickable). Still not posted (`l`/`v` stay empty).
- After a successful phone mark, `RoomAllocSync.pending` contains the id.

## Tests this invalidates

Do not retarget `RoomAllocSyncTest.paramsNeverCarryAStatus`.
Add: mark without room → snack + `checkIns` unchanged; mark with room →
`checkedIn && !synced && room.isNotBlank()`. `ZeroDayDraft` references = 0.

## Never-touched

`RoomAllocSync.params` keys/empties, `AttendedTableParser`, sheet GET `r` rule.
