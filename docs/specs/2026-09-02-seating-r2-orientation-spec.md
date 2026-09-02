# Seating plan r2 — hall orientation, chowky/chair, names, portrait — spec

**Status:** approved for build, 2026-09-02 (owner feedback with three screenshots,
same day as 2c shipped at 1.33.0). **Baseline:** `main` @ 1.33.0/54.
**Authority:** the live web seating page (owner screenshot) OVERRIDES frame 2c's
guessed geometry. Record the ruling in docs/DESIGN.md's corrections list at
integration.

## S1 — Orientation: teacher at the bottom, letters are columns

The web renderer (and this centre's `cs_seat_naming_conv = 1`) lays the hall out
as: **letters = columns** (A, B, C… left→right), **numbers = depth rows**, seat 1
nearest the teacher. Students face the dhamma seat, which sits at the BOTTOM.

- `hallLayout` reshapes: `seatPlacement("E4")` → column = letter index (A=0, AA=26
  supported), depth = 4. Render rows by depth DESCENDING — the highest number at
  the top, **row 1 at the bottom**, directly above:
- a 20dp **column-letter axis row** (DipiCondensed SemiBold 15sp `neutral500`,
  centred per column) and then the moved marker: **`TEACHER · DHAMMA SEAT`**,
  same 30dp chrome as today's FRONT marker, at the BOTTOM of the grid. Nothing
  is drawn above the grid.
- Numeric-only centres (`cs_seat_naming_conv = 0`): flow by configured columns —
  seat n → column (n-1) % columns, depth (n-1)/columns + 1; same bottom-up render.
- `HallGrid` fields become `columns: Int = 7` (clamp 1..26) and `depth: Int = 5`
  (clamp 1..40). Old persisted JSON (`rows`/`seatsPerRow`, one day old) decodes
  to defaults via `ignoreUnknownKeys` — acceptable, the registrar re-saves; note
  it in the report. Extension + the 2× safety cap keep their semantics on the
  new axes (a label beyond the grid EXTENDS columns/depth; garbage → UNSEATED).
- Centre Settings "Hall chart" steppers re-label accordingly:
  `Columns (A, B, C…)` and `Rows deep (1 sits nearest the teacher)`.
- The empty-cell dashed style, old/new fills and legend are unchanged.

## S2 — "CELL / PAGODA" becomes "CHOWKY / CHAIR"

`CW-` is a chowky (low seat), `CH-` a chair — they are hall positions, not
pagoda cells (the pagoda is a separate building; its cells are a future
feature, do not model them). Changes:
- Kicker text → `CHOWKY / CHAIR`; any identifier/kdoc named cellPagoda/cell
  column renames to chowkyChair (model `HallPlan.cellColumn` → `chowkyChair`).
- Order the side cells by trailing number DESCENDING top→bottom so `…-1` ends
  nearest the teacher, matching the grid's bottom-up read (web shows CW-A6 top,
  CW-A1 bottom).
- Landscape: keep the 280dp right column. Portrait: see S5.

## S3 — Names must never clip mid-glyph

Owner screenshot: two-line names hard-clip ("Ravikiran Dhulipala" cut through
the second line). Fix:
- Seat cell height **58dp → 66dp** (recorded deviation from frame 2c — owner
  feedback wins; 66 = id line ~14 + two 12.5sp/14sp lines + padding).
- Name: `fontSize 12.5sp, lineHeight 14sp, maxLines = 2,
  overflow = TextOverflow.Ellipsis` — a too-long name ellipsizes cleanly,
  never a shorn glyph row.
- Chowky/chair and grid cells share the same cell composable, so both get it.

## S4 — Hide unseated sevaks

Unseated **sevaks** sit on cushions that this plan does not draw — filter rows
whose `roleTag` is `Sevak` (case-insensitive) OUT of the UNSEATED section. Other
unseated rows (garbage labels, duplicate-label losers, unseated teachers) still
show with their reason. When nothing remains, the whole UNSEATED section
disappears. The header tally is UNCHANGED — sevaks are in the hall (on
cushions) and stay counted; only the list rows hide.

## S5 — Portrait uses the width

Below **1000dp** width (Pixel C portrait = 900dp):
- The chowky/chair column leaves the side and renders as a full-width section
  BELOW the grid (same 66dp cells, `columns`-per-row flow, 8dp gaps), above
  UNSEATED.
- The grid then takes the full width; cells flex (they already `weight(1f)`).
- ≥1000dp keeps today's side-by-side layout. No other portrait change.

## Tests (retargets are this spec's direct subject — never weaken elsewhere)

- `SeatGridTest`: retarget to the new axes — "E4" → column E depth 4; bottom row
  is depth 1; A8-with-A7-empty now means column A depth 8 extends DEPTH;
  numeric flow across columns; chowkyChair descending order; sevak-vs-other
  unseated split helper if placed in the model; clamps 26/40; extension + cap
  on both axes; tally unchanged by the sevak filter.
- `SeatingPlanScreenTest`: teacher marker + letter axis at the BOTTOM
  (bounds-below-grid assertion); depth-1 row bounds below depth-2; kicker
  `CHOWKY / CHAIR`; side cells descending; unseated sevaks absent while a
  non-sevak unseated row still shows, and the section vanishes when only
  sevaks; 66dp cell bounds; name node carries maxLines-2/ellipsis behaviour
  (bounds never exceed the cell). New `@Config(qualifiers = "w900dp-h1240dp")`
  portrait test: chowky/chair section renders BELOW the grid (top bound
  greater), full width.
- `CentreOpsScreenTest`: hall-chart additions retargeted to the new labels and
  field names; the four existing hall tests are the authorized retarget set;
  every other assertion untouched.

## Constraints

Unchanged from the plan: read-only, no fetch changes (still ONE roll GET),
tokens per DESIGN, ≥48dp targets, no re-sort of roll order for UNSEATED,
never bare `./gradlew test`, no agent attribution. Versioning: user-visible
MINOR → **1.34.0 / versionCode 55** at integration.
