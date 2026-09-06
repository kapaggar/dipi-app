# Seating Plan Landscape Print Design

**Date:** 2026-09-05
**Status:** Approved

## Problem

The native seating-plan PDF uses A4 landscape paper, but its content retains
portrait-width geometry. The hall occupies only about two thirds of the page
width, leaving a large blank area. Seat blocks show the seat, name, and
old/new status, but omit the room/lodging and age that Dhamma sevaks need while
checking the hall.

## Outcome

Each gender remains on one A4 landscape page. The hall grid fills the printable
width and expands vertically into the space left after the header, teacher
marker, chowky/chair rail, and unseated list. Printed blocks prioritize the
details used during the physical check: Dhamma seat, student name, room, age,
and old/new status.

## Print Contract

- The Android print job requests `ISO_A4.asLandscape()` for the native seating
  plan only. Existing portrait print jobs keep their current attributes.
- The HTML declares `@page { size: A4 landscape; margin: 6mm; }` and uses the
  resulting printable area directly, without transforms or browser zoom.
- Each populated floor-seat block contains:
  - a prominent `SEAT <id>` line, including the existing backrest glyph;
  - the student name;
  - `ROOM <room> · AGE <age>` with a clear fallback dash for blank values;
  - `OLD` or `NEW`.
- Empty floor seats keep a visible seat identifier and no invented student
  data.
- Chowky/chair students render as compact multi-column blocks with the same
  seat, name, room, age, old/new, and backrest information.
- Visible unseated students remain on the gender page in compact multi-column
  blocks, including room and age. Sevaks remain excluded as today.
- The teacher marker stays immediately below the floor grid, matching the
  screen orientation. Depth remains descending so row 1 is nearest the Dhamma
  seat.
- The backrest legend appears only on pages that use the glyph.

## Layout

The page section uses the printable A4 landscape height as a column. A compact
header and any lower lists take only their content height. The floor-grid
wrapper consumes the remaining height, and the table stretches to both the
wrapper width and height. This lets a shallow hall use larger cells while a
deep hall still fits on one page.

The lower chowky/chair and unseated areas use a four-column print grid.
They keep all entries visible while consuming horizontal space that the current
single-column lists waste. Typography remains monochrome and optimized for a
standard office printer.

## Data and Safety

The print continues to use the already-loaded in-memory `TeacherRoll` and the
same pure `hallLayout` used by the native hall. It adds no request, persistence,
logging, or server write. It never calls `/seating` and never sends `?r=`.

## Verification

- Unit tests pin landscape print attributes and the explicit landscape page
  rule.
- HTML tests pin room, age, seat labels, blank fallbacks, chowky/chair details,
  escaping, gender page separation, and conditional backrest legends.
- A generated PDF is checked for A4 landscape dimensions, two gender pages,
  full-width grid use, readable content, and absence of overflow.
- The prescribed full JVM/Robolectric suite and release assembly must pass
  before installation or release.
