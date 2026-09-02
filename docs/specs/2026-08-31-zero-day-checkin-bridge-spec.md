# Phone Zero Day writes real check-in records — spec (T5)

**Status:** proposed, 2026-08-31
**Baseline:** `main` @ `0ce3342`, 1.27.0 / versionCode 42.
**Origin:** repo scan 2026-08-31 — the phone Zero Day screen collects seating /
laundry / valuables / room per applicant into `ZeroDayDraft`
(`feature/applicants/.../ZeroDayScreen.kt:34-39`) and it dead-ends:
`DeskUiState.zeroDayDrafts` is never persisted (no `SessionStore` key) and never
transported (`RoomAllocSync.pending()` reads only `state.checkIns`, which only the
tablet `CheckInDialog` writes). The same screen shows "Sync rooms (n)" / "Pull
rooms" buttons whose queue the screen itself can never feed — on a phone-only
deployment the counter is permanently 0. Phone `markAttended` (`DeskViewModel:877`)
flips the row but creates no record either.

## Decision — delete the draft type; the phone edits the same `checkIns` map

One source of truth. `ZeroDayDraft` is removed and the phone rows read/write
`CheckInRecord` (`core/model/.../Models.kt:170-180`) through per-card ViewModel
functions, exactly like the tablet dialog's `patchDeskRecord` family
(`DeskViewModel:491-557`) but keyed by the passed card instead of `deskMarkId`.
Records then persist via the existing `persistCheckIns` → `SessionStore.CHECK_INS`
path, enter `RoomAllocSync.pending()` automatically, and the screen's own Sync
rooms button becomes truthful. This stays inside the allocation-sync amendment
(2026-08-16): bulk, user-initiated, dialog's own fields, never a status, never
`Approved`, never NPI.

Field mapping (draft → record):

| Draft field | Record field | UI change |
|---|---|---|
| `seating: String` ("Chowky"/"Chair"/"Backrest"/"None") | `seat: String` — same `SEAT_TYPES` values | none (segments identical) |
| `laundry: String` (free text) | `laundry: Boolean` | text field → the tablet's toggle semantics: a 48dp toggle row "Laundry", checked = true |
| `valuables: String` (free text) | `valuables: Boolean` | same, "Valuables" (record default is `true`, keep it) |
| `roomCode: String?` | `room: String` (`""` = none) | none (picker already exists via `pickRoom`) |
| — | `checkedIn: Boolean` | set by Mark attended |
| — | `group: String` | untouched — stays `"1"`; the phone offers no group control (YAGNI) |

The free-text laundry/valuables fields never mapped to anything the desk form
accepts (`RoomAllocSync.params` posts the dialog's boolean-backed fields), so
converting them to toggles is a correctness fix, not a regression.

## Changes

### 1. `app/src/main/kotlin/org/dhamma/dipi/staff/ui/DeskViewModel.kt`

Remove `zeroDayDrafts` from `DeskUiState`, remove `patchDraft`, and replace the
four draft functions (`setZeroDaySeating/setZeroDayLaundry/setZeroDayValuables`
at `:865-875`, and `pickRoom`'s draft write at `:837-846`) with record patches:

```kotlin
/** Phone Zero Day edits the same records the tablet dialog writes. */
private fun patchRecord(id: ApplicantId, patch: (CheckInRecord) -> CheckInRecord) {
    val cur = _state.value.checkIns[id] ?: CheckInRecord()
    persistCheckIns(_state.value.checkIns + (id to patch(cur).clearSyncedIfChanged(cur)))
}

fun setZeroDaySeat(card: ApplicantCard, seat: String) = patchRecord(card.id) { it.copy(seat = seat) }
fun toggleZeroDayLaundry(card: ApplicantCard) = patchRecord(card.id) { it.copy(laundry = !it.laundry) }
fun toggleZeroDayValuables(card: ApplicantCard) = patchRecord(card.id) { it.copy(valuables = !it.valuables) }
```

`pickRoom(room)` (`:837`) keeps its `roomsApplicantId` flow but writes the record:

```kotlin
fun pickRoom(room: AccoRoom) {
    _state.value.roomsApplicantId?.let { id -> patchRecord(id) { it.copy(room = room.code) } }
    back()
}
```

`markAttended(card)` (`:877-883`) additionally checks in the record, with the
same no-room guard the tablet enforces (`deskSaveSnack` errors without a room):

```kotlin
fun markAttended(card: ApplicantCard) {
    val record = _state.value.checkIns[card.id] ?: CheckInRecord()
    val (text, err) = deskSaveSnack(record, card)
    if (err) {
        _state.update { it.copy(snack = FlushSnack(text, error = true)) }
        return
    }
    persistCheckIns(_state.value.checkIns + (card.id to record.copy(checkedIn = true).clearSyncedIfChanged(record)))
    _state.update { cur ->
        val rows = cur.rows.map { if (it.id == card.id) it.copy(attended = true) else it }
        cur.copy(rows = rows, visible = WorklistFilter.visible(rows, cur.selected, cur.query), auditRows = flagAudit(rows), snack = FlushSnack(text, error = false))
    }
    viewModelScope.launch { repo.markAttendedLocal(card.id) }
}
```

(`deskSaveSnack` lives in `DeskDerive` — reuse, do not duplicate its rule.)

### 2. `feature/applicants/src/main/kotlin/org/dhamma/dipi/staff/applicants/ZeroDayScreen.kt`

- Delete `data class ZeroDayDraft`.
- Signature: `drafts: Map<ApplicantId, ZeroDayDraft>` becomes
  `records: Map<ApplicantId, CheckInRecord> = emptyMap()`; callbacks become
  `onSeat: (ApplicantCard, String) -> Unit`, `onLaundry: (ApplicantCard) -> Unit`,
  `onValuables: (ApplicantCard) -> Unit` (others unchanged).
- Per row: `val record = records[card.id] ?: CheckInRecord()`; the seat segments
  select on `record.seat == seat`; the two `OutlinedTextField`s are replaced by
  48dp toggle rows using `Modifier.toggleable(value = record.laundry, role = Role.Switch, onValueChange = { onLaundry(card) })`
  with `Switch(checked = record.laundry, onCheckedChange = null)` — the
  double-fire-safe pattern shipped in `CentreOpsScreen` on 2026-08-30. Same for
  valuables. Keep them behind `prefs.laundry` / `prefs.valuables` as today.
- Room button reads `record.room`: `if (record.room.isBlank()) "Room" else "Room  ${record.room}"`.

### 3. `app/src/main/kotlin/org/dhamma/dipi/staff/ui/DipiAppUi.kt`

At the `ZeroDayScreen` call site: pass `records = state.checkIns` and the renamed
callbacks (`vm::setZeroDaySeat`, `vm::toggleZeroDayLaundry`,
`vm::toggleZeroDayValuables`); remove the drafts argument.

## Tests

`app/src/test/kotlin/org/dhamma/dipi/staff/ZeroDayScreenTest.kt` — the draft
assertions are this spec's direct subject; retarget, do not weaken:

- `seatTapReportsCardAndSeat` — tap "Chair" on a row; assert callback got
  `(card, "Chair")`.
- `laundryToggleFiresExactlyOnce` — count-based: tapping the Laundry row fires
  `onLaundry` exactly once (double-fire guard).
- `markAttendedWithRoomChecksIn` (ViewModel-level, using the existing
  `seedForTest` seam): seed a record with `room = "Mbk 1"`, call
  `markAttended(card)`, assert `state.checkIns[card.id]?.checkedIn == true` and
  the row is attended.
- `markAttendedWithoutRoomIsBlocked` — no record: assert error snackbar, row NOT
  attended, no record checked in.
- `recordEntersRoomSyncQueue` — after the checked-in seed,
  `RoomAllocSync.pending(state.checkIns)` (match its real signature at
  `RoomAllocSync.kt:56`) contains the id — this is the assertion that pins the
  whole point of the bridge.

Never touched: `DeskPanesTest`'s tablet check-in dialog assertions,
`RoomSyncTest`, `RoomAllocSync` itself, `SessionStore` (the `CHECK_INS` key
already exists), tablet `patchDeskRecord`/`saveDeskMark`/`undoDeskMark`.

## Constraints

Room sync stays bulk + user-initiated (`POST /app-update-attended/{id}`, dialog
fields only — never a status, never `Approved`, never NPI); no attendance write
to the server beyond that existing form; records persist in DataStore exactly as
the tablet's already do (no new keys); ≥48dp touch targets on the new toggle
rows; tokens via `LocalDipi`/`DeskStyle`; no agent trailers; never bare
`./gradlew test`.

## Versioning note

User-visible behaviour change on the phone (fields become toggles; Mark attended
gains the room guard). Part of a MINOR wave per the master plan.
