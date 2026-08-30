# Claude Design prompt — DIPI Staff desk screens (Dhamma Sudha)

**Date:** 2026-08-16
**For:** Claude Design (paste everything below the line)
**App (already at 1.4.3):** `/Users/wizops/DIPI/dipi-app`
**Visual tokens already in Compose:** `docs/DIPI Staff.dc.html` and `core/ui/.../DipiTheme.kt`
**Older Vertical 1 prompt (superseded for these screens):** `docs/plans/2026-08-13-claude-design-prompt.md`
**Approved sketches (open in a browser):** `docs/sketches/index.html` (12 PNGs in the same folder)

The Compose app already has working screens for this set. Use this brief to produce
high-fidelity tablet (and phone) mockups that match the locked product rules, so
implementation can be tightened to the drawings. Do not invent Letters, LC, AT,
assign teacher, referral, or Group seating.

---

Design high-fidelity Android mockups for **DIPI Staff**, a native registrar-desk
app (Jetpack Compose) used at a Vipassana centre. The example centre in drawings
is **Dhamma Sudha**. The hall name is **Main Dhamma Hall**. Never label the
centre Dhamma Ganga, Dhamma Giri, or the hall Sama Dhamma.

One registrar at a busy desk. Sessions are 30 seconds to 2 minutes. Large
targets, high scan-ability, calm utilitarian tone. Clinical but warm. No
gamification, no decoration, no Material You color explosion.

## Platforms

Design every screen twice:

- **Tablet landscape** first (~1280 x 800, Pixel C / width >= 600dp). This is
  the real desk device.
- **Phone portrait** (~412dp): stacked, one screen at a time.

Worklist + card on tablet is list-detail (list left, card right). Hub, Zero Day,
Rooms, Centre settings, Audit, and Calling are full-width on both.

## Visual tokens (do not invent a new palette)

- Background `#F2F2F3`
- Text `#1D1F20`
- Muted `#5D5D60`
- Accent `#5980A6`
- Hairline `#D4D4D7` / strong `#B7B7BA`
- Hard flag `#A15C5C`, soft flag `#8A7645`
- Type: Barlow, Barlow Condensed for titles and tiles, IBM Plex Mono for codes
  (conf no, room codes, phone)
- 20dp page padding, 8dp tile gap, 4dp corners
- Tile min height 56dp, primary action buttons 56-64dp

## Product rules (locked)

- Centre name comes from the signed-in account. Drawings may say Dhamma Sudha.
- In scope: find applicant, card, status, photo review, course hub, Zero Day,
  rooms, seating/lists (as tiles), centre ops. Out of scope: Letters, LC
  Approved, AT schedule, assign teacher, referral. Do not draw those tiles.
- Never show Aadhaar, passport, voter id, PAN.
- Status sheet never offers Approved.
- Photo review stays on the hub. Do not hide it.
- Laundry, valuables, and groups are optional centre settings. Default: groups
  **off**. When groups are off, everyone is in Main Dhamma Hall and Zero Day
  has no group UI. When groups are on, Zero Day shows compact group chips
  (smaller than seating buttons).
- Adding a room is simple: Gender + Section + comma-separated codes (type `F32`).
- Room pick is its own page, gender-filtered (female applicant sees female
  rooms only). Rooms may show Geyser, Indian toilet, Western toilet.

## Screens to draw

### 1. Centre dashboard

After login. Header: centre name from account + signed-in user.
Upcoming courses as a short list (name, dates, "starts in N days").
Then a tile grid of centre ops: Centre Settings, Advanced Search, Course
Report, Bulk Mail.
Do not add Letters, AT, Referral.

### 2. Course hub (first window after picking a course)

Header: centre name, course name, dates. Back to Centre. Settings (app) in the
corner.

Tile grid, this exact set and order:

1. View Applications
2. Add Application
3. Photo review
4. Audit applications
5. Calling students
6. Zero Day
7. Day 0 List
8. Day 0 summary
9. Seating Plan
10. Student Chit
11. Checking Slip
12. Male PDF
13. Female PDF
14. Teachers List
15. Laundry List
16. Valuable List
17. Course Summary Report
18. Centre Settings

**Do not draw Group seating / Group-wise Seating.**

### 3. Worklist (View Applications) + applicant card

Keep the existing Today list: search, status chips, rows with name / status /
conf no. Card: public fields only, photo, Change status. Tablet split view.
No Approved chip. No NPI.

### 4. Change status sheet

Bottom sheet. Header: `{current} → choose new status`.

**Common (large 2x2 equal buttons, min 56-64dp):**
Confirmed, Cancelled, Duplicate, Custom

**Less used (small chips under a "Less used" label):**
Pending, Received, Expected, Reconfirmation, Rejected, Clarification

Custom shows a text field. Comment is optional. Confirm change button.
Footnote: the server may send the applicant a letter for this change.
No letter picker. No Approved.

### 5. Zero Day

Header: course name, "Zero Day".
Hall line: "Main Dhamma Hall" when groups are off. If groups are on, compact
A/B/C chips only (do not dominate the screen).

Two lists: Unattended, then Attended.

Each unattended row:

- Name (opens the card), status, gender
- **Large equal seating buttons:** Chowky, Chair, Backrest, None
- Room button (opens the Rooms page, filtered by that applicant's gender)
- Laundry field only if centre setting Laundry is on
- Valuables field only if Valuables is on
- Mark attended

Draw two variants: groups off (default), and groups on (small chips only).

### 6. Rooms page

Own full screen, not a dropdown.

- When opened from Zero Day: gender-filtered list, tap a code to pick
- When opened from Centre settings: same list, chips toggle Geyser / Indian /
  Western
- Row: room code (mono), section, feature chips
- Back to the opener

### 7. Centre settings (ops, not app Settings)

This is **not** theme / logout. Title "Centre settings".

Toggles:

- Laundry (default on)
- Valuables (default on)
- Groups (default off). Help: when off, everyone sits in Main Dhamma Hall and
  Zero Day hides group chips

Accommodation table matching the desk: **Gender | Section | Rooms**
Add row: Gender (M/F), Section, Rooms field (`F32, F33`). Delete a section.
Link to the Rooms page.

App Settings (theme, logout, erase local data) stays a separate screen. Do not
merge them.

### 8. Audit applications

Worklist of applicants that have audit flags. Hard first, then soft.
Row: name, status, hard/soft counts, first flag label.
No NPI. Tap opens the existing card.
Empty: "No audit flags on this course."

### 9. Calling students

Queue of Confirmed and Expected with a mobile number.
Chips: To call, Reached, No answer, All.
Each row: name, status, mobile, **large Call button** (device dialer).
Empty state when nobody to call.

## What not to draw

- Group seating
- Letters / letter picker
- LC Approved / AT review
- Assign teacher / AT schedule
- Referral
- Aadhaar, PAN, passport, voter id
- A URL field on login
- A new color system

## Copy notes

English. No em dash. Room codes and phones in mono. Hall is always written
"Main Dhamma Hall". Centre example "Dhamma Sudha".

## Deliverable

One click-through (or a frame per screen) covering tablet + phone for the nine
screens above, plus the two Zero Day variants (groups off / on). Name frames
clearly so they can be dropped next to the 1.4.3 Compose screens.
