# 2c — Seating plan + hall-grid config — spec

**Status:** approved for build, 2026-09-02 (Wave 3). **Baseline:** Waves 1-2.
**Design:** frame 2c + `docs/DESIGN.md` § Course ops (2c + corrections).
Owner decision: hall grid is REGISTRAR-CONFIGURED device-locally (mirrors the
server's per-centre INI, which no readable endpoint exposes).

## S1 — Hall-grid config

`CentreOpsPrefs` gains `hallGrid: Map<String, HallGrid>` keyed by gender name
(`"M"`,`"F"`), `@Serializable HallGrid(rows: Int = 5, seatsPerRow: Int = 7)`,
clamped 1..26 rows / 1..20 seats on read and write (RoomLayout pattern).
Edited in `CentreOpsScreen` as a new "Hall chart" section beside the Room
chart: per-gender steppers with the stage-then-SAVE flow the room chart
already uses (reuse its composables where sane). Wiped by Erase-all with the
rest of `centre_ops`. This is DESK-side config — the teacher never edits it.

## S2 — Seat placement (pure, `core/model/SeatGrid.kt`)

```kotlin
data class SeatCell(val id: String, val row: Int, val col: Int)
fun seatPlacement(label: String): SeatCell?   // "E4"→(4,3); "12"→numeric flow; null when blank
fun hallLayout(rows: List<RollRow>, grid: HallGrid): HallPlan
```
- Alphanumeric labels: letter(s)→row index (A=0… AA=26), number→column-1.
  Labels outside the configured grid EXTEND the plan (extra rows/cols appear;
  data wins over config — never drop a seated student).
- Numeric-only labels: flow by `seatsPerRow` (`(n-1)/spr`, `(n-1)%spr`), row
  letters synthesized A….
- `CW-`/`CH-` prefixed seats are EXCLUDED from the hall grid → the cell/pagoda
  column, in label order. (`CH-` chairs join the cell/pagoda column with their
  label shown — the design never knew chairs; note as ruling.)
- Blank seat → `UNSEATED` with the row's `roleTag` (e.g. `SEVAK`, `AT`,
  `SAT-2011`) or `—` when none — the only "reason" the page carries.
- Old/new per seat comes from the row's group seniority (band-derived).

## S3 — Screen (`:feature:teacher/SeatingPlanScreen.kt`)

Per frame 2c: header 62dp with sub-line `"{hall} hall · facing the front ·
{o} old, {n} new"`; hall tabs (Male/Female, 32dp pills, client-side over the
one response); legend (Old `accent100`+`accent300` per the correction — the
cells, not the legend-swatch hexes; New `#FAFAFB`+neutral300; Empty white +
dashed neutral400); FRONT · DHAMMA SEAT marker 30dp drawn once at top; grid of
58dp cells (seat id mono 10sp accent600 top, name 12.5sp clipped bottom, never
3 lines) with 26dp row letters; 280dp cell/pagoda column (2-col grid of the
same cells on accent tint — occupied only; EMPTY cell/pagoda slots are not
drawn at all, resolving the frame's tint-vs-legend contradiction); UNSEATED
rows 34dp with reason tag right. Seat tap → the same student card as 2b (two
doors, one record). Read-only; no drag.

## Tests

- `SeatGridTest` (pure): A1/B7/E4 placement; A8 lands col 8 with A7 empty
  (non-contiguous ids); numeric flow; AA row; CW-/CH- exclusion; blank →
  unseated with reason; grid extension beyond config; old/new tally matches
  the sub-line.
- `CentreOpsScreenTest` additions: hall-chart steppers stage locally, SAVE
  commits (mirror the room-chart tests; retarget nothing existing).
- `SeatingPlanScreenTest` (app): legend + cell fills; empty cells dashed with
  no name; hall tab switch keeps the single response (no refetch — assert
  fetch-count via seam); seat tap surfaces the applicant; FRONT marker once.
Never touched: room-chart tests, `RoomLayoutTest`, desk seating exports.
