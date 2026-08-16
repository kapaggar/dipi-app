# Handoff: DIPI Staff — registrar desk (tablet) + worklist (phone)

## Overview

DIPI Staff is the administrative front end for a Vipassana meditation centre
(Dhamma Sudha). Centre staff — a registrar, a manager, a couple of servers —
run each course's application, arrival and audit work from this app. It replaces
the current practice of working from printed lists and a desktop admin site.

Two form factors are designed:

- **Tablet desk (1240×844 landscape)** — the primary surface. Used at the
  registration table on Day 0 while a queue of students is standing in front of
  the desk. Seven sections: Board, Applications, Audit, Calling, Check-in,
  Rooms & seats, Centre settings.
- **Phone worklist (411×914 Android)** — the secondary surface. Used away from
  the desk: reading an application, changing a status, reviewing photos.

The design goal throughout: **a session is 30 seconds long**. Every screen is
one job — find, read, change, put it down. No screen asks the user to hold
context across a navigation.

---

## About the design files

The files in `design/` are **design references created in HTML**. They are
prototypes that show intended look and behaviour. They are **not production
code to copy**.

The task is to **recreate these designs in the target codebase's own
environment** — React, Vue, Flutter, SwiftUI, Android native, whatever the app
is being built in — using its established component library, routing, state
management and styling conventions. If no codebase exists yet, choose the
framework that fits the deployment target (a tablet + phone app used offline at
a rural centre argues for React Native, Flutter, or a PWA with a service
worker) and implement there.

The HTML uses a small in-house template runtime (`support.js`, `<x-dc>`,
`{{ hole }}` bindings, `<sc-for>` / `<sc-if>`). **Ignore that runtime.** It is
scaffolding for the design tool, not an architectural suggestion. Read it as:
`<sc-for list="{{ rows }}" as="r">` is a list map, `<sc-if value="{{ x }}">` is
a conditional render, `{{ x }}` is an interpolated value, `onClick="{{ fn }}"`
is a click handler. The logic class at the bottom of the file
(`class Component extends DCLogic`) is an ordinary React-style class component:
`state`, `setState`, and one `renderVals()` method that computes every derived
value the markup reads.

### How to open the design

Open `design/DIPI Staff.dc.html` in any browser — no build step, no server.
It is an infinite canvas; scroll and pan. Two turns of work are stacked, newest
first:

- **`2a` — the tablet desk.** Fully interactive. Click the rail to move between
  sections. Notes down the right-hand side explain each design decision.
- **`1a` / `1b` / `1c` — the phone.** `1a` is interactive; `1b` and `1c` are
  static frames of the remaining screens.

---

## Fidelity

**High fidelity.** Final colours, typography, spacing, copy and interaction
behaviour. Recreate the tablet desk pixel-accurately using the codebase's
existing primitives. Every value in this document is the value in the file.

Two deliberate exceptions, both marked in the design:

1. **Student photographs** are placeholder boxes (initials on a hairline frame).
   Real photos come from the application record.
2. **Sample data is invented.** 26 roster names, 12 call numbers, 8 audit
   findings. Wire to the real API; keep the shapes described under *Data model*.

---

## Screens / views — tablet desk (`2a`)

Device screen: **1240 × 844**, `background: var(--color-bg)` (#f2f2f3),
`color: var(--color-text)` (#1d1f20). Root is a horizontal flex: fixed rail,
then a flexible main column. The desk never scrolls as a whole — the rail is
fixed, and each pane scrolls independently (`overflow: auto`, scrollbars
hidden via `::-webkit-scrollbar { width: 0 }`).

### Left rail (persistent, all sections)

| Property | Value |
|---|---|
| Width | 212px, `flex: none` |
| Border | `border-right: 1px solid var(--color-neutral-300)` |
| Padding | `20px 0 16px` |

**Brand row** — padding `0 18px 18px`, flex, gap 10px. DIPI logo image
(`https://dipi.vridhamma.org/sites/default/files/dipi-logo.gif`, height 20px)
inside a blueprint frame (1px accent border, padding 5px 6px, corner marks).
Wordmark "DIPI Staff": Barlow Condensed 700 / 18px / line-height 1.

**Course card** — margin `0 14px 22px`, `1px solid var(--color-neutral-400)`,
padding `11px 12px 12px`, blueprint corner marks, column gap 3px:

- Kicker "COURSE" — IBM Plex Mono 600 / 9.5px / letter-spacing .16em / `--color-accent-700`
- Name "Dhamma Sudha" — Barlow Condensed 600 / 19px / line-height 1.1
- Dates "10 Day · 2–13 Sep 2026" — Barlow 400 / 11.5px / `--color-neutral-600`
- Status chip "DAY 0 · TODAY" — IBM Plex Mono 600 / 10px / letter-spacing .1em,
  `--color-accent-800` on `--color-accent-100`, padding 3px 6px, square, `align-self: flex-start`

**Section label** "DESK" — IBM Plex Mono 600 / 9.5px / letter-spacing .16em /
`--color-neutral-500`, padding `0 0 6px 18px`.

**Nav rows** — one per section, `padding: 9px 16px 9px 0`, gap 10px,
`border-bottom: 1px solid var(--color-neutral-200)`, `cursor: pointer`,
hover `background: var(--color-neutral-100)`. Each row is three parts:

- Active bar: 2px × 16px, `--color-accent` when active, transparent otherwise
- Label: 13.5px, weight 600 when active / 400 otherwise, `--color-accent-800` active / `--color-neutral-700` inactive
- Count: IBM Plex Mono 500 / 11px / `--color-neutral-500`, right-aligned

Rows and their live counts:

| Label | Count |
|---|---|
| Board | — |
| Applications | 214 (total applications) |
| Audit | 20 (open findings, all checks) |
| Calling | 12 (numbers not yet logged) |
| Check-in | 15 (roster not yet arrived) |
| Rooms & seats | 16 (free rooms, both blocks) |
| Centre settings | — |

**Footer** — padding `0 18px`, pushed down with a `flex: 1` spacer.
"registrar.sudha" 11.5px `--color-neutral-600`; "synced 2 min ago" IBM Plex
Mono 500 / 10.5px / `--color-neutral-500`.

### Top bar (persistent)

Height 52px `flex: none`, `border-bottom: 1px solid var(--color-neutral-300)`,
padding `0 26px`, space-between. Left: current section name, Barlow Condensed
600 / 12px / letter-spacing .14em / uppercase / `--color-neutral-600`
("Board", "Applications", "Audit", "Calling round", "Zero Day · check-in",
"Rooms & seats", "Centre settings"). Right: "Wed 2 Sep · 09:41", IBM Plex Mono
500 / 11px / `--color-neutral-500`.

---

### 1. Board

**Purpose** — the first thing on screen at 09:00. Answer "what is the state of
today, and what do I do next?" in one glance, and let the user leave for the
work without hunting.

Padding 26px, scrolls.

**Headline** — h2 "Day 0 at Dhamma Sudha", Barlow Condensed 700 / 40px /
line-height 1 / letter-spacing -.01em. Sub-paragraph 14px /
`--color-neutral-700` / `max-width: 640px` / `text-wrap: pretty`:
"Gate opens at 14:00. Twenty-six on the roll, eleven already in their rooms.
Everything below is a number you can act on — tap it." Block margin-bottom 24px.

**Four number tiles** — `grid-template-columns: repeat(4, 1fr)`, gap 14px,
margin-bottom 26px. Each tile: `1px solid var(--color-neutral-400)`, padding
`16px 16px 14px`, blueprint corner marks, `cursor: pointer`, hover
`border-color: var(--color-accent); background: var(--color-accent-100)`.

- Number: Barlow Condensed 700 / 46px / line-height 1 / letter-spacing -.02em / `--color-accent-800`
- Label: Barlow Condensed 600 / 13px / letter-spacing .1em / uppercase, margin-top 4px
- Note: 11.5px / `--color-neutral-600`

| Number | Label | Note | Navigates to |
|---|---|---|---|
| 26 | ARRIVING TODAY | 26 confirmed · gate opens 14:00 | Check-in |
| 11 | CHECKED IN | 42% of the roll | Check-in |
| 12 | STILL TO CALL | 0 logged this round | Calling |
| 20 | NEEDS ATTENTION | across 8 checks | Audit |

All four numbers are derived live, not stored — see *State*.

**"NEXT" — three action rows.** Label IBM Plex Mono 600 / 10px /
letter-spacing .16em / `--color-neutral-500`, margin-bottom 10px. Rows: column,
gap 10px, margin-bottom 30px. Each row `1px solid var(--color-neutral-400)`,
padding `14px 18px`, gap 16px, blueprint marks, same hover as tiles.

- Verb-first label: Barlow Condensed 600 / 20px / line-height 1.1
- Sub: 12px / `--color-neutral-600`
- Right: Lucide `arrow-right`, 18px, stroke-width 1.5, `--color-accent`

| Label | Sub |
|---|---|
| Check in arrivals | 15 still to arrive |
| Clear audit findings | 20 findings · 15 must fix |
| Finish the call round | 12 numbers left |

**"SHEETS & EXPORTS · RARELY URGENT"** — the twelve PDF exports, deliberately
demoted to a hairline grid. `grid-template-columns: repeat(4, 1fr)`,
`gap: 1px`, `background: var(--color-neutral-300)`,
`border: 1px solid var(--color-neutral-300)` — the 1px gap over a grey ground
draws the rules, no per-cell borders. Cells `background: var(--color-bg)`,
padding `10px 12px`, gap 8px, hover `background: var(--color-accent-100)`.
Lucide `download` 13px stroke 1.5 accent + label 12.5px `--color-neutral-800`.

Items, in order: Day 0 list · Day 0 summary · Student chit · Checking slip ·
Male PDF · Female PDF · Teacher list · Manager list · Laundry list ·
Valuable list · Seating plan · Course report.

---

### 2. Check-in ("Zero Day")

**Purpose** — the Day 0 arrival desk. A student walks up, gives a name or a
conf number; the registrar finds the row, assigns a room and a seat, and marks
them in. Target: under 20 seconds per arrival, no screen changes.

Two panes: work pane (`flex: 1`, `border-right: 1px solid var(--color-neutral-300)`)
and a statistics sidebar (266px `flex: none`).

**Search + filter header** — padding `20px 24px 16px`, gap 14px.

- Label "CONF NUMBER OR NAME" — IBM Plex Mono 600 / 9.5px / letter-spacing .16em / `--color-neutral-500`
- Input — full width, IBM Plex Mono 500 / **17px** (large: it is typed at speed,
  often one-handed), padding `10px 12px`, `1px solid var(--color-neutral-400)`,
  `border-radius: 2px`, `background: #fff`, `outline: none`,
  placeholder "NF24". Filters on conf number **or** name, case-insensitive
  substring.
- Segmented control — `1px solid var(--color-neutral-400)`, radius 2px,
  `overflow: hidden`; options padding `11px 15px`, 12.5px, `white-space: nowrap`,
  `border-right: 1px solid var(--color-neutral-300)`. Selected: `background:
  var(--color-accent)`, `color: #fff`. Options: **To arrive** (default) ·
  Arrived · All.

**Progress card** — `1px solid var(--color-neutral-400)`, padding `12px 14px`,
gap 9px, blueprint marks.

- Count Barlow Condensed 700 / 26px / `--color-accent-800`, then "of 26 checked
  in" 13px `--color-neutral-600`, then right-aligned "15 to arrive" IBM Plex
  Mono 500 / 11px
- Bar: 8px tall, `background: var(--color-neutral-200)`; fill positioned
  absolute, width = percentage, `background: var(--color-accent)`,
  `transition: width .25s`

**Roster rows** — one merged list (see *Design decisions*, #2). Row: padding
`11px 24px`, gap 14px, `border-bottom: 1px solid var(--color-neutral-200)`,
`cursor: pointer`, hover `background: var(--color-neutral-100)`. Columns, left
to right:

| Column | Width | Style |
|---|---|---|
| Tick | 20 × 20px | `1px solid`. Unchecked: border `--color-neutral-400`, transparent fill. Checked: border + fill `--color-accent`, Lucide `check` 12px stroke 2.2 in #fff |
| Conf no | 56px | IBM Plex Mono 500 / 13px / `--color-neutral-700` |
| Name | flex 1 | 14.5px / weight 500, ellipsis; followed by a 5px round dot, `--color-accent` when the person has an audit finding, transparent otherwise |
| Age/gender/city | 170px | 12.5px / `--color-neutral-600` — e.g. "34 F · Pune" |
| Slot | 150px | Centred, `1px solid`, padding `6px 8px`, 12px. Not arrived: border `--color-accent`, text `--color-accent-800`, reads "Mark attended". Arrived: border `--color-neutral-300`, text `--color-neutral-700`, reads "F21 · Chowky" |

Empty state (search matches nothing): padding `40px 24px`, centred, 13px
`--color-neutral-600` — "Nobody matches that. Clear the field to see the whole roll."

**Statistics sidebar** — 266px, padding `20px 20px 24px`, column gap 20px.
Three blocks, each with an IBM Plex Mono 600 / 9.5px / letter-spacing .16em /
`--color-neutral-500` label:

1. **THE ROLL** — a table, `1px solid var(--color-neutral-300)`. Header row
   `background: var(--color-neutral-100)`, columns blank / M / F (40px each,
   centred, Barlow Condensed 600 / 11px / `--color-neutral-600`). Rows: Old
   students, New students, Total. Cells IBM Plex Mono 500 / 13px, centred.
   Counts derive from the conf number prefix.
2. **ROOMS FREE** — one line per block, space-between, padding `6px 0`,
   `border-bottom: 1px solid var(--color-neutral-200)`. Left 12.5px, right
   IBM Plex Mono 500 / 11.5px / `--color-neutral-600` — "Female · Fbk block /
   11 of 15 free".
3. **SEATING ISSUED** — Chowky / Chair / Backrest / None with live counts,
   IBM Plex Mono 500 / 13px.

---

### 3. Check-in dialog — "mark attended"

**Purpose** — everything one arrival needs, in one modal, so the registrar
never leaves the roster. Opened by clicking any roster row.

Backdrop: absolutely positioned over the device screen, `rgba(29,31,32,.42)`,
`display: grid; place-items: center; padding: 24px`. Clicking the backdrop
closes; clicks inside must **not** bubble (the backdrop is a separate absolute
sibling behind the panel, `z-index: 1` on the panel — a stopPropagation
handler on the panel also works).

Panel: **560px** wide, `background: var(--color-bg)`,
`1px solid var(--color-accent)`, `box-shadow: var(--shadow-lg)`,
`max-height: 100%`, blueprint marks, column: fixed header, scrolling body,
fixed footer.

**Header** — padding `18px 20px 14px`,
`border-bottom: 1px solid var(--color-neutral-300)`, gap 14px.
Kicker "CHECK IN · NF41" IBM Plex Mono 600 / 9.5px / letter-spacing .16em /
`--color-accent-700`; name Barlow Condensed 700 / 27px; meta "28 F · Pune"
12.5px `--color-neutral-600`. Close button 28 × 28,
`1px solid var(--color-neutral-400)`, Lucide `x` 13px stroke 1.5, hover
`border-color: var(--color-accent)`.

**Body** — padding `16px 20px 18px`, gap 16px, scrolls.

*ROOM* — a collapsed row that expands a picker in place:

- Row: `1px solid var(--color-neutral-400)`, padding `11px 13px`, gap 12px,
  hover `border-color: var(--color-accent)`. Value Barlow Condensed 700 / 20px —
  `--color-accent-800` when chosen, `--color-neutral-500` and reading
  "Not chosen" when not. Hint text "only free rooms for this gender" 11.5px
  `--color-neutral-600`. Lucide `chevron-down` 14px stroke 1.5 accent.
- Expanded picker: `grid-template-columns: repeat(3, 1fr)`, gap 8px. Each cell
  `1px solid`, padding `9px 10px`, gap 4px; selected gets border
  `--color-accent` + `background: var(--color-accent-100)`. Room code Barlow
  Condensed 700 / 17px. Amenity chips in a `white-space: nowrap` row, gap 4px:
  IBM Plex Mono 500 / 8.5px / letter-spacing .04em / `--color-neutral-600`,
  `1px solid var(--color-neutral-300)`, padding `1px 3px` — "Geyser",
  "Indian", "Western".
- **The list is pre-filtered**: only rooms in the block matching the student's
  gender, and only rooms not already occupied tonight. Choosing a room collapses
  the picker.

*SEATING* — `grid-template-columns: repeat(4, 1fr)`, gap 8px. Cells `1px solid`,
padding `12px 6px`, centred, Barlow Condensed 600 / 15px / letter-spacing .04em.
Selected: border `--color-accent`, `background: var(--color-accent-100)`,
`color: var(--color-accent-800)`. Options: **Chowky · Chair · Backrest · None**
(default None).

*Toggles* — shown only when the matching centre setting is on:

- "Valuables deposited" — visible when Centre settings › Valuables is on (default on)
- "Laundry issued" — visible when Centre settings › Laundry is on (default off)
- Track 40 × 20px, knob 16 × 16px `#fff` inset 2px, `transition: .18s`
  on both `background` and `left`. On: `--color-accent`; off: `--color-neutral-300`.
  Label 14px, row gap 14px, first toggle carries
  `border-top: 1px solid var(--color-neutral-200); padding-top: 14px`.

*GROUP* — visible only when Centre settings › Groups is on (default off). Nine
cells 36 × 36px, gap 7px, `1px solid`, IBM Plex Mono 500 / 14px; selected fills
`--color-accent` with `#fff` text.

**Footer** — padding `14px 20px`,
`border-top: 1px solid var(--color-neutral-300)`, gap 10px. "Undo check-in"
(only when already checked in) 12.5px `--color-neutral-600` underlined, left.
Then a `flex: 1` spacer, "Cancel" (outlined, `--color-neutral-400`) and the
primary — `background: var(--color-accent)`, `#fff`, hover
`--color-accent-600`, blueprint marks. Both buttons: padding `10px 18px`
(primary `10px 22px`), Barlow Condensed 600 / 14px / letter-spacing .06em /
uppercase, `white-space: nowrap`. Primary label is
**"Check in <first name>"** when new, **"Save changes"** when editing an
existing check-in.

**Validation** — pressing the primary with no room chosen does *not* close the
dialog; it raises an error snackbar: "Choose a room before checking Priya in".

**Success** — dialog closes, roster row gains its tick and shows
"F21 · Chowky", counter and progress bar advance, sidebar counts update, and a
snackbar reads "✓ Priya Shah checked in · F21 · Chowky".

---

### 4. Audit

**Purpose** — clear data problems in the application set before the course
starts. Grouped by **the check that fired**, not by person, so the user fixes
one *kind* of mistake at a time (see *Design decisions*, #3).

Two panes: findings list (410px, `border-right`, padding 20px) and detail
(`flex: 1`, padding `24px 26px`).

**Findings list** — h2 "20 findings" Barlow Condensed 700 / 30px; sub 12.5px
`--color-neutral-600`: "Grouped by the check that fired, not by person — fix
one kind of mistake at a time." Cards: column gap 8px, `1px solid`, padding
`11px 13px`, gap 12px, blueprint marks. Selected: border `--color-accent`,
`background: var(--color-accent-100)`.

- Title 13.5px weight 500, `text-wrap: pretty`
- Machine code IBM Plex Mono 500 / 10.5px / `--color-neutral-500`
- Count Barlow Condensed 700 / 20px / `--color-accent-800`, right
- Severity Barlow Condensed 600 / 9.5px / letter-spacing .1em / uppercase —
  "Must fix" in `--color-accent-800`, "Check" in `--color-neutral-600`

The eight checks, in priority order (these mirror the existing `audit.js`
rule names — keep them):

| Code | Title | Severity | n |
|---|---|---|---|
| `phone_prefix_invalid` | Mobile number has no country code | Must fix | 4 |
| `conf_gender_mismatch` | Conf number disagrees with recorded gender | Must fix | 1 |
| `id_missing` | No ID document on file | Must fix | 3 |
| `emergency_eq_self` | Emergency contact is their own mobile | Must fix | 3 |
| `cross_course_duplicate` | Also active in another course | Must fix | 2 |
| `age_dob_mismatch` | Listed age disagrees with date of birth | Check | 3 |
| `shared_mobile` | Mobile shared with another applicant | Check | 2 |
| `name_title_prefix` | Honorific left in the name field | Check | 3 |

**Detail pane** — machine code IBM Plex Mono 500 / 11px / `--color-accent-700`;
h3 Barlow Condensed 700 / 28px / line-height 1.05 / `max-width: 520px`;
meta "Must fix · 4 applications" 12.5px `--color-neutral-600`.

**Batch action** — present only when the fix is mechanical. Solid accent
button, padding `11px 16px`, gap 10px, Barlow Condensed 600 / 15px /
letter-spacing .04em / uppercase, `white-space: nowrap`, Lucide `arrow-right`
16px, blueprint marks, margin-bottom 20px. Two exist:
"Prefix +91 to all 7" (`phone_prefix_invalid`) and "Strip 3 honorifics"
(`name_title_prefix`). On success the snackbar confirms **and states what was
preserved**: "✓ Prefix +91 to all 7 · all other fields preserved".

**People rows** — `border-top: 1px solid var(--color-neutral-300)` on the list,
rows padding `13px 0`, gap 16px,
`border-bottom: 1px solid var(--color-neutral-200)`:
conf no 56px (mono 12.5px `--color-neutral-600`), name 180px (14px / 500),
the offending value `flex: 1` in IBM Plex Mono 500 / 12.5px /
`--color-accent-800`, then an "Open" button — `1px solid
var(--color-neutral-400)`, padding `6px 12px`, 12px, hover border + text accent.

---

### 5. Calling

**Purpose** — the pre-course call round. Confirm each applicant is coming, and
**record what happened** so the round can be picked up by someone else, or
finished after lunch.

Header padding `20px 26px 14px`. h2 "Call round" Barlow Condensed 700 / 30px;
sub 12.5px `--color-neutral-600` — "0 of 12 logged · log the outcome as you go,
the list empties itself". Segmented control (same spec as check-in, padding
`9px 14px`): **To call** (default) · Reached · No answer · Call back.

Rows padding `11px 26px`, gap 14px,
`border-bottom: 1px solid var(--color-neutral-200)`:

| Column | Width | Style |
|---|---|---|
| Conf no | 56px | IBM Plex Mono 500 / 12.5px / `--color-neutral-600` |
| Name | 170px | 14.5px / 500, ellipsis |
| City | 92px | 12.5px / `--color-neutral-600` |
| Number | flex 1 | Lucide `phone` 15px stroke 1.5 accent + IBM Plex Mono 500 / 13.5px / `--color-accent-800`; hover underline; click hands off to the device dialler |
| Outcome | auto | Three chips, gap 6px, `1px solid`, padding `7px 11px`, 12px, `white-space: nowrap`. Selected: border `--color-accent`, `background: var(--color-accent-100)`, `color: var(--color-accent-800)` |

Logging an outcome removes the row from "To call" and files it under that
outcome; the header count advances. Empty state: padding `46px 26px`, centred,
13px — "Nothing in this pile."

---

### 6. Rooms & seats

**Purpose** — the occupancy picture. Who is where tonight, and what is free.

Padding `24px 26px`. h2 Barlow Condensed 700 / 30px; sub 12.5px — "Filled cells
are occupied tonight. Amenity marks: G geyser · I Indian · W Western."

Two blocks side by side, `grid-template-columns: 1fr 1fr`, gap 26px. Block
header: `border-bottom: 1px solid var(--color-neutral-400)`,
`padding-bottom: 7px` — title Barlow Condensed 700 / 22px ("Female · Fbk
block", "Male · B block") + count IBM Plex Mono 500 / 11.5px
`--color-neutral-600` ("15 rooms · 11 free").

Room cells: `grid-template-columns: repeat(3, 1fr)`, gap 10px, `1px solid`,
padding `10px 11px`, gap 3px, blueprint marks. Occupied: border
`--color-accent`, `background: var(--color-accent-100)`, code
`--color-accent-800`, occupant's name below. Free: border
`--color-neutral-300`, transparent, code `--color-neutral-500`, the word
"free" below. Code Barlow Condensed 700 / 19px; amenity string IBM Plex Mono
500 / 9.5px / letter-spacing .1em / `--color-neutral-500` (e.g. "GIW");
occupant 11.5px `--color-neutral-600`, ellipsis.

---

### 7. Centre settings

**Purpose** — a centre's practices differ. These three switches change what
check-in asks for, and the panel shows the resulting question set so the
consequence is visible before anyone tests it on a queue.

`max-width: 760px`, padding `24px 26px`. h2 Barlow Condensed 700 / 30px;
sub 12.5px — "Three switches change what check-in asks for. The line at the
bottom shows the result."

Rows on a `border-top: 1px solid var(--color-neutral-300)` list, each padding
`18px 4px`, gap 20px, `border-bottom: 1px solid var(--color-neutral-200)`,
whole row clickable:

- Title Barlow Condensed 600 / 20px
- Note 12.5px `--color-neutral-600` `max-width: 440px` `text-wrap: pretty`
- State word "On"/"Off" IBM Plex Mono 600 / 10px / letter-spacing .12em /
  `--color-neutral-500`, fixed 24px width so the toggles align
- Toggle track 44 × 22px, knob 18 × 18px, `transition: .18s`

| Setting | Default | Note |
|---|---|---|
| Laundry | Off | "Some centres do not run laundry. Off hides the field on check-in." |
| Valuables | On | "Shows the valuables field. Usually follows the room." |
| Groups | Off | "Off seats everyone in Sama Dhamma hall and hides group numbers." |

**Result panel** — margin-top 22px, `1px solid var(--color-accent)`, padding
`13px 15px`, blueprint marks. Label "RESULT" IBM Plex Mono 600 / 9.5px /
letter-spacing .16em / `--color-accent-700`. Body 13.5px
`--color-neutral-800`, composed live: `"Check-in will ask for: room · " +
(valuables ? "valuables · " : "") + (laundry ? "laundry · " : "") +
"Chowky / Chair / Backrest / None" + (groups ? " · group" : "")`.

---

### 8. Applications

**Purpose** — the year-round work: read an application, check it, change its
status. List–detail, both panes scrolling independently.

**List** — 396px `flex: none`, `border-right`. Rows padding `11px 18px`,
gap 12px, `border-bottom: 1px solid var(--color-neutral-200)`,
`border-left: 2px solid` (accent when selected, transparent otherwise),
`background: var(--color-accent-100)` when selected, hover
`--color-neutral-100`. Name 14px / 500 with ellipsis + a 5px attention dot
(`--color-accent` for a must-fix finding, `--color-neutral-400` for a soft one,
transparent when clean); meta "34 F · Pune" 11.5px `--color-neutral-600`; conf
no IBM Plex Mono 500 / 11.5px `--color-neutral-500`; status pill 66px wide,
centred, padding `3px 8px`, 10.5px / 500.

**Detail** — padding `24px 26px`, gap 20px.

- Photo placeholder 104 × 130px, `1px solid var(--color-neutral-400)`,
  `background: var(--color-neutral-100)`, blueprint marks, initials Barlow
  Condensed 700 / 30px `--color-neutral-500`. **Replace with the real
  photograph**; keep the frame and marks.
- Name h3 Barlow Condensed 700 / 32px; identity line 14px
  `--color-neutral-700` — "34 F · Pune, Maharashtra, India" (city, state and
  country merged into one line; **no** Student/Old label — the conf number
  already encodes it); status pill padding `4px 10px` / 11.5px + conf no mono
  12px; "Last course · 1 Apr 2026 · Dhamma Pattana · Bhumidhar" 12px.
- Attention panel — `1px solid var(--color-accent)`, padding `13px 15px`,
  gap 10px, blueprint marks. Header line IBM Plex Mono 600 / 9.5px /
  letter-spacing .16em / `--color-accent-700` — "Needs attention · 2" or
  "Audit clean · nothing to fix". Each finding: label 13.5px / 500 + detail
  IBM Plex Mono 500 / 11px `--color-neutral-600`. **Only checks that need a
  human decision appear here** — the eight in the Audit table.
- Facts list — `border-top: 1px solid var(--color-neutral-300)`, rows padding
  `10px 0`, space-between: Mobile, Email, Date of birth, Applied. Key 12.5px
  `--color-neutral-600`, value 13px.
- Actions — "Change status" (solid accent, blueprint marks), "Call", "Edit"
  (outlined `--color-neutral-400`). Padding `10px 18px`, Barlow Condensed 600 /
  14px / letter-spacing .06em / uppercase, `white-space: nowrap`, gap 10px.

---

### Snackbar (all sections)

`position: absolute; left: 24px; bottom: 20px`, `max-width: 520px`, padding
`12px 16px`, 13px / line-height 1.4, `#fff`, `box-shadow: var(--shadow-md)`,
square. Success `background: var(--color-accent-800)`; error uses the error
tone. Anchored to the device screen, not the page.

---

## Screens — phone worklist (`1a`, `1b`, `1c`)

411 × 914 Android frame. Material 3 *structure* — top app bar, filter chips,
list items, bottom sheet, snackbars, FAB — drawn entirely on the Industry
palette. Screens:

1. **Login** — DIPI logo, "Centre admin desk", username/password. The centre
   name is read from the account after sign-in, never typed.
2. **Courses** — the list of courses this account administers.
3. **Today** (zero-day summary) — one headline number (expected today), an
   arrivals progress bar, gender breakdowns as bars, and five action buttons.
   *Not* a grid of counters.
4. **Worklist** — searchable, chip-filtered application list. Rows carry the
   same small attention marker as the tablet.
5. **Applicant detail** — photo first, then the "Needs attention" panel, then
   identity and facts; old students additionally get a **Courses completed**
   block: first course, most recent (date · centre · teacher), and counts by
   type.
6. **Status change** — bottom sheet: radio list + comment field, snackbar
   confirmation, undo.
7. **Photo review** — filter pills, per-card suggestion badges matching the
   `review.js` classes, rotate / crop / ✓ per card, and a batch upload action.
8. **Settings** — includes a Light / Dark theme toggle; the whole design has a
   dark variant driven from it.

The chip rows in the static frames are `flex: none` — without it the row
collapses to its padding. Worth knowing if you reproduce that pattern.

---

## Interactions & behaviour

**Navigation** — the rail replaces the screen in the main column. No page
transition, no loading state between sections: all data for the course is
already local (see *Offline*).

**Check-in, the critical path**

1. Type a conf number or name → roster filters as you type (no submit).
2. Click the row → dialog opens over the roster, roster stays behind it.
3. Click the ROOM row → picker expands **in place**, showing only free rooms of
   the right gender.
4. Click a room → picker collapses, value fills in.
5. Click a seat type; toggle valuables / laundry / group as the centre's
   settings require.
6. Primary button. Blocked with an error snackbar if no room. Otherwise: dialog
   closes, row updates in place, counter + bar + sidebar counts advance,
   confirmation snackbar.
7. Re-opening a checked-in row shows the same dialog with "Save changes" and an
   "Undo check-in" link.

**Transitions** — deliberately few. Progress bar width `.25s`; toggle
background and knob `.18s`. Hovers are instantaneous. Nothing else animates:
the surface is used under fluorescent light by someone standing up, and motion
costs attention.

**Hover / press** — every interactive object takes `border-color:
var(--color-accent)`, or `background: var(--color-accent-100)` where it already
has an accent border, or `--color-accent-600` for the solid primary. Keyboard
focus must be `outline: 2px solid var(--color-accent); outline-offset: 2px` —
the design system's rule, and the tablet is sometimes used with a keyboard.

**Error states** — validation is a snackbar plus a blocked action, never an
inline red field: the user is looking at the person in front of them, not the
form. Exactly one error case exists in this design (room not chosen).

**Offline** — a centre often has no reliable network. The rail footer carries
"synced 2 min ago" as an always-visible truth claim. Every action in this
design must succeed offline against local state and reconcile later; the phone
design has an explicit offline indicator and a test-mode flag. Do not build a
spinner-per-action model.

**Responsive** — the tablet layout is designed at one size, 1240 × 844
landscape, and does not reflow. Below roughly 1100px logical width, use the
phone layout instead. The statistics sidebar is the first thing to drop if an
intermediate width is needed.

---

## State management

The prototype holds all of this in one component. In a real app, split by
section, but the shape is right.

**Tablet**

| State | Type | Purpose |
|---|---|---|
| `v2` | enum | Active section: `board` \| `apps` \| `audit` \| `calls` \| `check` \| `rooms` \| `opt` |
| `scan` | string | Check-in search box |
| `zf` | enum | Roster filter: `To arrive` \| `Arrived` \| `All` |
| `arrived` | `{ [confNo]: { in, room, seat, val, laundry, group } }` | Check-in records, keyed by conf number. Seeded from the server for anyone already in |
| `mark` | conf no \| null | Which row's dialog is open |
| `roomOpen` | boolean | Room picker expanded |
| `finding` | index | Selected audit check |
| `calls` | `{ [confNo]: 'Reached' \| 'No answer' \| 'Call back' }` | Call outcomes |
| `cf` | enum | Calling filter |
| `appIdx` | index | Selected application |
| `opt` | `{ laundry, valuables, groups }` | Centre settings |
| `snack` / `snackErr` | string / boolean | Snackbar text and tone |

**Derived, never stored** — arrivals count, percentage, "to arrive", every rail
count, roll table cells, free-room lists, seating-issued counts, and the free
list inside the dialog. All computed from `arrived` + the roster. This matters:
it is why four independent numbers on the board stay consistent with the roster
without any sync code.

**Phone** — `screen`, `query`, `filters[]`, `selIdx`, `sheet`, `pick`,
`comment`, `snack`, `offline`, `test`, `theme`, `photoFilter`, `photos{}`.

**Data fetching** — one fetch per course, on open, of the whole application set
for that course, then local. Writes queue.

---

## Data model

**Conf number** — `N|O` + `M|F` + serial. `NM5` = new male #5, `OF11` = old
female #11. The prefix is load-bearing throughout the UI: it drives the roll
table, the room block filter, and the old/new counts. Because the conf number
already carries it, the UI **never** prints a "New student" / "Old student"
label — that was removed deliberately.

`conf_gender_mismatch` exists precisely because this encoding can disagree with
the recorded gender field (sample: Priya Shah, `NM41`, recorded F). Treat that
disagreement as data to be resolved by a human, never auto-corrected.

**Roster row** — `{ conf, name, gender, age, city, room, seat }`.
**Check-in record** — `{ in, room, seat, val, laundry, group }`.
**Room** — `{ code, block, gender, amenities }` where amenities ⊆ {G geyser,
I Indian toilet, W Western toilet}.
**Seat types** — Chowky, Chair, Backrest, None.
**Audit finding** — `{ code, title, severity: 'Must fix' | 'Check', batchLabel?,
people: [{ name, conf, offendingValue }] }`.
**Call** — `{ name, conf, city, mobile, outcome? }`.
**Application** — `{ name, conf, status, gender, age, city, state, country,
mobile, email, dob, applied, flags[], history? }` where `history` is
`{ first, recent, counts: [[courseType, n]] }`.

**Statuses** — Confirmed, Pending, Waitlist, Cancelled. Each has a light and a
dark pill tone; they are the only non-steel colours in the design and they are
muted on purpose (`#dfeae1` / `#2f5a41` for Confirmed, etc. — see `TONE` in the
design file).

---

## Design tokens

All from the **Industry** design system —
`design/_ds/industry-0b639033-ca39-4864-b492-9b1ca1b256e6/styles.css`. Use the
CSS variables, not the hex values, wherever the target platform allows.

**Ground and ink**

| Token | Hex |
|---|---|
| `--color-bg` | `#f2f2f3` |
| `--color-surface` | `#e9e9ea` |
| `--color-text` | `#1d1f20` |
| Canvas behind the device | `#dcdcde` |

**Neutral ramp** — `100 #f5f5f8` · `200 #e7e7ea` · `300 #d4d4d7` ·
`400 #b7b7ba` · `500 #98989b` · `600 #7a7a7d` · `700 #5d5d60` · `800 #424244` ·
`900 #2b2b2d`

**Accent (steel) ramp** — base `--color-accent #5980a6` ·
`100 #eef6ff` · `200 #d6ebff` · `300 #b5d9fd` · `400 #94bce3` · `500 #749dc4` ·
`600 #597ea3` · `700 #416180` · `800 #2c455d` · `900 #1d2d3d`

Usage discipline, and the point of the redesign: **steel is the only colour,
and it means one thing — live, occupied, or selected.** 100 for selected fills
and hovers, base for active bars / ticks / solid primaries, 600 for pressed,
700–800 for text on tinted fills and for numerals. Everything else is a
hairline drawing on the neutral ramp. Do not introduce a second hue.

**Type**

| Role | Font | Notes |
|---|---|---|
| Headings, numerals, buttons | **Barlow Condensed** 600/700 | Every large number, section title, button label |
| Body | **Barlow** 400/500 | Rows, notes, paragraphs |
| Codes and data | **IBM Plex Mono** 500 | Conf numbers, phone numbers, counts, all-caps kickers, timestamps |

Sizes in play: 46 / 40 / 32 / 30 / 28 / 27 / 26 / 22 / 20 / 19 / 17 (input) /
15 / 14.5 / 14 / 13.5 / 13 / 12.5 / 12 / 11.5 / 11 / 10.5 / 9.5 / 8.5px.
Kickers are 9.5–10px mono at letter-spacing .16em, uppercase.

**Spacing** — the system's scale is `--space-1..8` (3.4 / 6.8 / 10.2 / 13.6 /
20.4 / 27.2px). In practice the desk uses a 26px page gutter, 20px pane
padding, 14px card padding, gaps of 6 / 8 / 10 / 14 / 16 / 20 / 26px.

**Radius** — `2px` on inputs and segmented controls; **0 everywhere else**.
Cards, tiles, chips, buttons, dialogs and toggles are square. This is not
negotiable in this system: they are line drawings, not filled rounded blocks.

**Shadows** — only two are used: `--shadow-md` (`0 3px 10px rgba(43,43,45,.16)`)
on the snackbar, `--shadow-lg` (`0 12px 32px rgba(43,43,45,.22)`) on the
dialog. Nothing else is elevated.

**Blueprint frame** — the system's signature. Cards, figures and the primary
button carry four corner registration marks: 9px square, `opacity: .5`, a 1px
accent cross drawn with `::before` (horizontal) and `::after` (vertical),
positioned `-5px` outside each corner, `pointer-events: none`. In the design
file this is `.bp` + four `<i class="tl|tr|bl|br">`; the design system ships it
as `.blueprint` + `<i class="corner tl">`. **Keep the marks.** A framed element
without them is off-system.

**Icons** — Lucide, stroke-width **1.5** (the tick inside a filled check box is
2.2). Sizes 13–18px. Used: `check`, `x`, `chevron-down`, `arrow-right`,
`download`, `phone`.

---

## Assets

| Asset | Source |
|---|---|
| DIPI logo | `https://dipi.vridhamma.org/sites/default/files/dipi-logo.gif` — replace with a vector mark if one exists |
| Barlow, Barlow Condensed | Google Fonts |
| IBM Plex Mono | Google Fonts |
| Icons | Lucide (https://lucide.dev), stroke 1.5 |
| Student photographs | **None supplied.** Placeholder frames with initials |
| Industry design system | `design/_ds/industry-…/styles.css` — tokens and component classes |

No image assets are bundled; everything is drawn.

---

## Design decisions worth preserving

These are the changes the redesign made against the original sketches. Each was
a specific fix; please do not regress them while adapting to your component
library.

1. **The hub became a board.** Eighteen equal tiles gave every job the same
   weight. Four live numbers now carry the navigation, three verb-first rows say
   what to do next, and the twelve PDF exports drop to small type — they are
   exports, not decisions.
2. **One roll, not two tables.** "Unattended" and "Attended" were separate
   tables, so the user scanned twice and lost their place. It is one list: check
   someone in and the row keeps its position, gains a tick, and shows
   room · seat.
3. **Audit grouped by check, not by person.** Red / orange / yellow severity
   squares told the user nothing about the work. The list is the check that
   fired with its count, and where the fix is mechanical, one button clears all
   of them — while stating what it preserved.
4. **Room picking stays in the dialog.** Choosing a room used to be a separate
   screen. It is now inline, pre-filtered to free rooms of the right gender, so
   one arrival is one dialog and never a round trip.
5. **Calls log an outcome.** A column of identical call buttons has no memory of
   who answered. Reached / No answer / Call back sit on the row, the pile empties
   as the user works, and the header counts the round.
6. **Colour carries meaning again.** Amenity chips were blue, green and purple
   for no reason. Steel is the only colour and it means live / occupied /
   selected.
7. **The conf number is trusted as the label.** Student/Old chips were removed
   because the prefix already says it; city, state and country merged into one
   line. Less to read at the desk.

---

## Files

| Path | What it is |
|---|---|
| `design/DIPI Staff.dc.html` | **The design.** Open in any browser. `2a` = tablet desk (interactive), `1a`–`1c` = phone |
| `design/support.js` | The design tool's template runtime. Required to open the file; **not** part of the deliverable |
| `design/_ds/industry-…/styles.css` | Industry design system — tokens and component classes |
| `design/_ds/industry-…/_ds_bundle.js` | Industry component bundle |
| `IMPLEMENTATION_PROMPT.md` | Paste-into-Claude-Code prompt for building this |
| `README.md` | This document |

Sample data and all derived-value logic live in the `<script data-dc-script>`
block at the bottom of the design file: `V2_ROSTER`, `V2_ROOMS`, `V2_FINDINGS`,
`V2_CALLS`, `V2_DOCS`, `SEATS`, `AMEN`, `PEOPLE`, `TONE`, `SEV`, and the
`v2vals()` method that computes every number on screen.
