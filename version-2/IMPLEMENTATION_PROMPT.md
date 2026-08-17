# Implementation prompt

Paste this into Claude Code (or your agent of choice) from the root of the
target repository, with this handoff folder available to it.

---

You are implementing a designed feature into this codebase. The design is
supplied as an HTML prototype plus a written specification.

**Read first, in this order:**

1. `design_handoff_dipi_staff_desk/README.md` — the full specification. Every
   measurement, colour, font, interaction and state variable is in there. It is
   self-sufficient; treat it as the source of truth.
2. `design_handoff_dipi_staff_desk/design/DIPI Staff.dc.html` — the prototype.
   Open it in a browser to see and click the real thing. `2a` is the tablet
   desk (interactive: use the left rail); `1a`–`1c` are the phone screens.
3. This repository — before writing anything, survey the existing conventions:
   component library, styling approach (CSS-in-JS / modules / Tailwind /
   platform styles), routing, state management, data layer, test setup, and any
   design-token file already present.

**What this is.** DIPI Staff is the admin app for a Vipassana meditation centre.
Centre staff run application review, the pre-course calling round, a data audit,
and Day 0 arrival check-in from it. The primary surface is a **1240 × 844
landscape tablet** used at the registration desk with a queue of people
standing in front of it. A secondary **411 × 914 phone** layout covers the same
work away from the desk.

**The prototype is a reference, not code to copy.** It uses a bespoke template
runtime (`<x-dc>`, `{{ holes }}`, `<sc-for>`, `<sc-if>`, `support.js`) that
exists only for the design tool. Ignore that runtime entirely and read the
markup as: `<sc-for list="{{ rows }}" as="r">` is a list map,
`<sc-if value="{{ x }}">` is a conditional render, `{{ x }}` is an interpolated
value, `onClick="{{ fn }}"` is a click handler. The logic class at the bottom of
the file is a plain React-style class component whose single `renderVals()`
method computes every derived value the markup reads — that method is the best
single summary of the app's behaviour, and worth reading in full.

**Rebuild it in this codebase's own idiom.** Use the existing component
primitives, layout system and state patterns. Do not introduce a new UI library,
a new styling approach, or a parallel token system. If this repository has no
front end yet, choose the framework that fits a tablet-and-phone app used
offline at a rural site — React Native, Flutter, or a PWA with a service worker
— say which you chose and why, then build there.

**Fidelity: high.** Match the design pixel-accurately. Colours, type sizes,
weights, letter-spacing, paddings and border widths in the README are the real
values, not approximations. Two exceptions, both marked in the design: student
photographs are placeholder frames, and all sample data is invented — wire it to
the real API, keeping the shapes under *Data model*.

**Non-negotiable visual rules** (from the Industry design system, which ships in
`design/_ds/industry-…/styles.css` — read its `:root` block and use the CSS
variables, not literal hex values, wherever the platform allows):

- Square corners. `2px` radius on text inputs and segmented controls; **0**
  everywhere else — cards, tiles, chips, buttons, dialogs, toggles.
- Cards and figures are transparent hairline line drawings. No surface fills.
  The one deliberate exception is the solid accent primary button.
- Framed objects keep their four `+` corner registration marks (9px, 50%
  opacity, 1px accent cross, offset -5px outside each corner). An element with
  the frame but without the marks is off-system.
- **Steel (`--color-accent` #5980a6) is the only colour, and it means one thing:
  live, occupied, or selected.** Do not add a second hue. Status pills carry the
  only other tones and they are muted on purpose.
- Barlow Condensed for headings, numerals and button labels; Barlow for body;
  IBM Plex Mono for conf numbers, phone numbers, counts and all-caps kickers.
- Lucide icons at stroke-width 1.5.
- Keyboard focus is `outline: 2px solid var(--color-accent); outline-offset: 2px`
  — never the browser default. The tablet is sometimes used with a keyboard.

**Behavioural rules that matter more than they look:**

- **Derive, never duplicate.** Every count on screen — the four board numbers,
  all seven rail counts, the roll table, free-room lists, seating-issued counts,
  the progress bar — is computed from the check-in records plus the roster. Do
  not store them. This is why the numbers cannot drift.
- **Offline first.** A centre often has no reliable network. Every action must
  succeed against local state and reconcile later. One fetch of the course's
  application set on open, then local; writes queue. Do not build a
  spinner-per-action model. The rail footer's "synced 2 min ago" is a real
  claim about sync state.
- **Check-in is the critical path** — target under 20 seconds per arrival with
  no screen change. Search filters as you type; the row opens a dialog *over*
  the roster; the room picker expands *inside* the dialog, pre-filtered to free
  rooms matching the student's gender; the row updates in place on save. Never
  navigate away mid-arrival.
- **Validation is a snackbar plus a blocked action**, not an inline red field —
  the user is looking at the person in front of them, not the form. Exactly one
  error case exists: no room chosen.
- **The conf number is load-bearing.** `N|O` + `M|F` + serial (`NM5` = new male
  #5). It drives the roll table, the room-block filter and the old/new counts,
  and it is why the UI never prints a "New student" / "Old student" label.
  `conf_gender_mismatch` exists because this encoding can disagree with the
  recorded gender field — surface it for a human, never auto-correct it.
- **Audit codes are contractual.** `phone_prefix_invalid`,
  `conf_gender_mismatch`, `id_missing`, `emergency_eq_self`,
  `cross_course_duplicate`, `age_dob_mismatch`, `shared_mobile`,
  `name_title_prefix` — these mirror the existing `audit.js` rule names. Keep
  them exactly. Batch fixes must state what they preserved.
- **Motion is deliberately scarce.** Progress bar width `.25s`; toggle
  background and knob `.18s`. Nothing else animates. Do not add page
  transitions, skeleton shimmers, or entrance animations.

**Section 7 of the README, "Design decisions worth preserving",** lists seven
specific fixes this design makes against an earlier version — merged arrival
tables, audit grouped by check rather than person, room picking inline, calls
logging an outcome, and so on. Read it before you adapt anything to your
component library, and do not regress those while doing so.

**Build order** — ship it in slices that are each usable at the desk:

1. Shell: rail, top bar, section routing, tokens wired to the design system.
2. Check-in — roster, search, filters, progress, the dialog, validation,
   snackbar, derived sidebar counts. This is the highest-value screen; get it
   fully right before moving on.
3. Board — the four derived tiles, three action rows, the export grid.
4. Audit — findings list, detail pane, batch actions.
5. Calling — rows, dialler hand-off, outcome logging, filters.
6. Rooms & seats, Centre settings.
7. Applications list–detail.
8. Phone layout for the same data (`1a`–`1c` in the prototype).

**Before you start**, tell me: which framework and component primitives you will
use, how you will model the offline write queue, anything in the spec that
conflicts with an existing pattern in this repository, and any question whose
answer would change the structure. Then build slice 1 and stop for review.
