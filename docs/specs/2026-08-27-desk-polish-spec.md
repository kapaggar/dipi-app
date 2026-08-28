# Desk polish — tight layout, editable room chart, switch toggles

**Status:** proposed, 2026-08-27 (second feedback round of the day)
**Origin:** owner feedback with four annotated tablet screenshots taken from
1.21.0/1.21.1 running on the Pixel C.
**Baseline:** `feat/v3-conformance` at 1.21.1 / versionCode 34.

## S1 — Centre screen: drop two strings

**S1.1** `CentreScreen.kt:170` renders
`"{centre} · from your account · {displayName}"`. Drop the middle clause. New:
`"{centre} · {displayName}"` — same face, same size, same colour.

**S1.2** `CentreScreen.kt:244` renders the sub-line
`"Teacher list · valuables · seating — check-in is closed"` under the "Older
courses" heading. Delete the whole `Text`, not just its content. The heading
itself stays.

## S2 — Centre screen: the dead band

The screenshot shows an empty strip between the upcoming-course grid and
"Older courses", annotated *"useless space. keep the UI tight"*.

**Cause.** The wide layout gives the upcoming region `Modifier.weight(0.6f)`.
A plain `weight` is an *exact* allocation: the child is measured at 60% of the
remaining height whether or not its content needs it. Four short course cards
therefore leave a visible band of nothing before the next region starts.

**Fix.** `Modifier.weight(0.6f, fill = false)`. With `fill = false` the child
takes *at most* its weighted share and shrinks to content when it needs less,
and the leftover flows to the sibling region instead of becoming a gap. The
60% remains a ceiling — which is what "upcoming courses at 60%" always meant —
and a tall list still scrolls inside it exactly as today.

Do not remove the split or the two independent scroll states; both are correct
and were fixed once already this session.

## S3 — Desk chrome

**S3.1 The top bar crumb duplicates the centre name.** `DeskCourse.crumbLine`
(`DeskShell.kt:67`) joins `label · dates · dayChip`, where `label` is the
centre name (`DipiAppUi.kt:522`) and `dates` falls back to `course.name` when
the date range is blank. Because `course.name` already begins with the centre
name, the bar reads *"Dhamma Sudha · Dhamma Sudha / 10 Day / 2026 / …"*.

Fix: `crumbLine` drops `label` and joins `dates · dayChip` only. `label` stays
on the data class — the rail still uses it — but no longer enters the crumb.

**S3.2 The crumb is too small.** `DeskTopBar` renders it at `13.sp`
(`DeskShell.kt:216`). Raise to **17.sp**. Keep the face
(`DipiCondensed`/SemiBold), the `0.1.em` tracking, the colour, and
`maxLines = 1`. The bar's 52dp height is unchanged and comfortably fits 17sp.

**S3.3 The Board's centre-name heading goes.** `BoardPane` renders a 40sp
heading of `"{dayLabel} at {centreName}"`, or bare `centreName` when there is
no day label — the screenshot shows it as a redundant *"Dhamma Sudha"* directly
under the crumb that already names the course. Delete that `Text`. The
subtitle beneath it ("109 on the roll, …") stays and becomes the block's first
line.

If `centreName` becomes an unused parameter, remove it from `BoardPane`'s
signature and update its single call site. Do not leave a dead parameter.

## S4 — Editable room chart

**The ask:** *"make the room layout editable … depending on how the rooms are
actually built in the centre the layout might differ, give the ability to add
row and columns, male and female separately."*

**Decision — columns are the editable dimension; rows follow.** A block of N
rooms laid out C per row occupies `ceil(N/C)` rows. Rows are therefore not an
independent control: choosing the column count *is* choosing the grid shape.
The UI exposes a column stepper per block and shows the resulting row count, so
the registrar sees both numbers while adjusting one. A separate "add row"
control would be either redundant or contradictory.

**Scope key — per gender *and* section.** `RoomsScreen` already groups rooms by
gender then by `section` (`RoomsScreen.kt:63-66`), which is how the desk models
the building (`Mbk`, `Fbk`, `Guest`). Each gender+section block carries its own
column count, so male and female are independent exactly as asked, and a
guest annexe with a different shape is independent too.

**Model** — new `core/model/.../RoomLayout.kt`:

```kotlin
package org.dhamma.dipi.staff.model

import kotlinx.serialization.Serializable

/**
 * How many room tiles sit in one chart row, per gender+section block. Rooms
 * wrap, so the row count is derived: ceil(rooms / columns). Device-local, and
 * wiped by Erase-all along with the rest of CentreOpsPrefs.
 */
@Serializable
data class RoomLayout(
    val columns: Map<String, Int> = emptyMap(),
) {
    fun columnsFor(gender: Gender, section: String): Int =
        columns[key(gender, section)]?.coerceIn(MIN_COLUMNS, MAX_COLUMNS) ?: DEFAULT_COLUMNS

    fun withColumns(gender: Gender, section: String, n: Int): RoomLayout =
        copy(columns = columns + (key(gender, section) to n.coerceIn(MIN_COLUMNS, MAX_COLUMNS)))

    companion object {
        const val DEFAULT_COLUMNS = 4
        const val MIN_COLUMNS = 1
        const val MAX_COLUMNS = 12
        fun key(gender: Gender, section: String): String = "${gender.name}|$section"
        fun rowsFor(rooms: Int, columns: Int): Int =
            if (rooms <= 0 || columns <= 0) 0 else (rooms + columns - 1) / columns
    }
}
```

`DEFAULT_COLUMNS = 4` preserves today's appearance for any block the registrar
never touches.

**Persistence.** `CentreOpsPrefs` gains `val roomLayout: RoomLayout = RoomLayout()`.
It is already `@Serializable` and stored as JSON under the `centre_ops`
DataStore key (`SessionStore.kt:126-137`), so the new field persists with no
store change and is already cleared by Erase-all. The defaulted field keeps old
stored JSON decodable.

**UI.** Each block header in `RoomsScreen` gains a stepper on its trailing
edge: `−  {C} per row  +`, with the derived row count in the header text, e.g.
`Male · Mbk · 70 rooms · 7 per row · 10 rows`. `−` is disabled at
`MIN_COLUMNS`, `+` at `MAX_COLUMNS`. The grid uses `columnsFor(gender, section)`
in place of the hardcoded `chunked(4)`, and the trailing `Spacer` fill uses the
same value.

Tap targets on the steppers are at least 48dp — this session already fixed one
sub-48dp control and should not add another.

**Not in scope.** No server write. Room layout is a device-local display
preference; the desk has no field for it and the backend is immutable.

## S5 — Centre settings

**S5.1 Real switches.** The three rows render state as a `DeskKicker` reading
`"ON"`/`"OFF"` (`CentreOpsScreen.kt:117-136`). Replace with a Material 3
`Switch` on the row's trailing edge, `checked` bound to the pref and
`onCheckedChange` calling the existing callback. The whole row stays clickable
so the tap target is not just the switch. Title and note are unchanged.

Switch colours come from the theme; do not hard-code any.

**S5.2 Room chart first, and bigger.** Today `Room chart` is a small
`TextButton` sitting after the RESULT card (`CentreOpsScreen.kt:86`). Move it
to the **top of the page**, directly under the "Centre settings" heading and
its Back control, before the three switches. Render it as a full-width
`deskCard` row — not a `TextButton` — with the label at **18.sp** in
`DipiCondensed` and a short sub-line (`"Rooms, sections and chart layout"`), so
it reads as the primary destination on the page.

The RESULT card, the three switches and the Accommodation summary keep their
current relative order beneath it.

## Tests this invalidates

Existing assertions that pin the strings and structures above must be updated
**knowingly**, and nothing else may be touched:

- Any assertion on `"… from your account …"` (S1.1) or on the older-courses
  sub-line (S1.2).
- `CentreOpsScreenTest` asserts `"ON"`/`"OFF"` text for the toggle state
  (S5.1). With a `Switch` the state is a semantics property, not text —
  retarget to `assertIsOn()`/`assertIsOff()` on the switch node, keeping the
  same prefs combination so the test proves the same thing.
- Any assertion on the Board's centre-name heading (S3.3).

Every other existing assertion — in particular
`centreSettingsRowIsReachableWithoutCourses`, which proves centre settings is
reachable with no courses — stays exactly as it is. If a worker finds itself
needing to change an assertion not listed here, it must stop and report rather
than rewrite.

## Versioning

Baseline 1.21.1 / 34. This is user-visible feature work inside the current
vertical → **MINOR: 1.22.0 / versionCode 35**. `feat/desk-gap` holds 1.19.0/30
and does not collide. Registrar-facing, so hard rule 12 requires the Pixel C
install.
