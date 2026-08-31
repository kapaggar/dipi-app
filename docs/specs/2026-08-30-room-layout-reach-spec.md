# Room layout, reach and stacking — spec + task plan

**Status:** proposed, 2026-08-30 (third feedback round of the day)
**Baseline:** `fix/centre-card-bloat` at 1.24.2 / versionCode 39, verified on the Pixel C.
**Origin:** owner feedback with three screenshots, to land **before** the merge to `main`.

## S1 — Save button on the room chart (Centre Settings → Room chart)

Today the `− n +` column stepper calls `onColumns` on every tap, persisting straight
to DataStore. The owner wants an explicit **Save room layout** action.

**Decision — stage, preview, then commit.** Stepper taps update *local* state so the
grid reflows immediately (the preview is the whole point of a column stepper), but
nothing is persisted until **SAVE ROOM LAYOUT** is pressed. The button is disabled
when nothing is dirty. Leaving the screen without saving discards — the layout is a
device-local display preference, so a lost edit costs one re-tap and no data.

Placement: a full-width button at the top of the chart, under the "Room chart"
heading, so it is visible without scrolling past 70 room tiles. ≥48dp tall.
A dirty-state hint ("unsaved changes") sits beside it when staged edits exist.

## S2 — The layout must reach the desk's Rooms & seats pane

`RoomsPane` (the desk section) hardcodes `chunked(4)` and does not take a
`RoomLayout` at all, so an edit in Centre Settings changes the chart and nothing
else. That is the owner's "it should reflect in Rooms & seats".

**Decision — group the desk pane by gender AND section, matching the layout's keys.**
`RoomLayout` is keyed `gender|section`. `RoomsPane` currently merges sections into
one block per gender ("Male · Mbk/Guest block", 73 rooms), which has no single
column count to honour. Splitting it into one block per gender+section (Male · Mbk
70, Male · Guest 3, Female · Fbk 41) makes the mapping exact and is what S3's
vertical stacking accommodates. Each block's header keeps its `n rooms · n free`
counts.

`RoomsPane` gains `layout: RoomLayout = RoomLayout()` — **defaulted**, so the
existing call site keeps compiling — and uses `layout.columnsFor(gender, section)`
in place of `chunked(4)`, including the trailing `Spacer` fill. `DipiAppUi` passes
`state.centreOps.roomLayout`, the same source the chart reads.

## S3 — Stack the gender blocks instead of placing them side by side

`RoomsPane` renders Female and Male as two `weight(1f)` columns in a `Row`, which
halves the width available to each block's room grid. Stack them vertically instead,
in the pane's existing `verticalScroll`, each block full width. Combined with S2
that yields one full-width block per gender+section, in a single scroll.

## S4 — The check-in room picker cannot reach past room 27 (functional bug)

**This is the important one.** `CheckInPane`'s mark-attended dialog body is

```kotlin
Column(Modifier.weight(1f, fill = false).padding(...).fillMaxWidth(), ...)
```

with **no scroll**. The room picker renders every free room at three per row, so with
73 free rooms it needs 25 rows and only about nine fit. Rows past that are clipped
and unreachable: **a registrar cannot allocate any room above ~27.** The owner's
screenshot shows the list ending at Mbk 27 of 73.

Fix: give the dialog body `verticalScroll(rememberScrollState())`. The header and the
CANCEL / CHECK IN action row stay fixed outside the scroll so the primary action is
never scrolled away.

This is the third bounded-container-without-a-scroll defect in this screen family
(the centre cards, the settings band, now this). Add a test that reaches the **last**
free room in a 73-room block and asserts it is wholly on screen after scrolling, so
the class of bug is pinned rather than just this instance.

The picker keeps three per row — it is a narrow dialog listing only free rooms, a
different context from the chart, and the owner did not ask for it to follow the
layout.

## Tests this invalidates

- `RoomsScreenTest`: the stepper assertions currently expect `onColumns` to fire on
  each tap. Per S1 it now fires only on Save — retarget them to assert (a) the grid
  reflows on tap without persisting, and (b) `onColumns` fires for each changed block
  when Save is pressed. Same behaviours proved, one indirection later.
- Any `DeskPanesTest` assertion pinned to the side-by-side gender layout or to
  `RoomsPane`'s merged "Mbk/Guest block" header text (S2/S3).

Never touched: `centreSettingsRowIsReachableWithoutCourses`, the accommodation
read-only assertions, `RoomLayoutTest`, `OlderCourseLimitTest`, the `SyncBanners`
truth table, parser tests.

---

## Task plan — three parallel workers, disjoint files

| Task | Owns exclusively | Item |
|---|---|---|
| T1 dialog reach | `feature/desk/.../CheckInPane.kt`, `app/src/test/.../DeskPanesTest.kt` | S4 |
| T2 save button | `feature/course/.../RoomsScreen.kt`, `app/src/test/.../RoomsScreenTest.kt` | S1 |
| T3 desk pane | `feature/desk/.../RoomsPane.kt`, `app/.../ui/DipiAppUi.kt`, new `app/src/test/.../RoomsPaneTest.kt` | S2, S3 |

T3 gets a **new** test file so `DeskPanesTest` has exactly one owner (T1) — the
shared-test-file trap that cost a round earlier today.

**Global constraints:** tokens via `LocalDipi`/`Industry`, no inline hex; ≥48dp on new
controls; `RoomsPane`'s and `RoomsScreen`'s new parameters defaulted; no backend
change; no NPI; commits carry no agent trailers; never bare `./gradlew test`.

**Versioning:** functional bugfix plus user-visible feature → **MINOR 1.25.0 /
versionCode 40**, then merge to `main`, push, and cut the release.
