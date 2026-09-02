# Claude Design brief — DIPI Staff v5: desk sheets, course report, Board, rooms

Paste this whole file into Claude Design. It is self-contained. `README.md` beside it has the shot index and the HAR route map; `HAR-ROUTES.md` has the redacted transport table; `IMPLEMENT.md` is for the Claude Code agent that comes *after* you.

---

## 1. What DIPI Staff is

A native Android tablet app (Jetpack Compose / Kotlin) used by Vipassana meditation-centre registrars at the check-in desk. One account → one centre → upcoming courses → a six-section course desk: **Board, Applications, Audit, Calling, Check-in, Rooms & seats**.

The primary device is a **Pixel C, 2560×1800, landscape, density 2** — so **1280×900 dp** of usable canvas. A phone path exists for some sheets. Touch targets are ≥ 48 dp because staff tap this standing up, at speed, at 07:00 on day 0 with a queue of students in front of them.

The centre's real system of record is an old Drupal site (`https://dipi.vridhamma.org`). **The backend PHP is immutable.** The app logs in as a browser would, scrapes HTML, and re-renders it natively. Twelve "sheets" — day-0 rosters, seating plans, chits, teacher/manager lists — are still served by that Drupal desk as print-styled HTML or streamed PDF/Excel, and the app shows them in a display-only viewer.

The shipped build is **1.30.5** (`versionCode` 51). This pass is a **MINOR** — no new product features, no new write protocol.

## 2. What you are designing (and what you are not)

**You are designing:** ten surfaces, listed in §7. Nine are readability/print/layout jobs on existing screens and sheets. One (item 8, Course report) is a genuinely new native surface on the centre dashboard.

**Design must produce HTML frames** — one `.dc.html` file in the shape of the previous pass (`DIPI Staff v4.dc.html`): before/after pairs, each frame drawn at **1280×900 CSS px = 1280×900 dp**, so the implementer reads dp straight off the file. Font px = sp. Include the print frames at **A4 portrait proportions** where the item is a printed artefact (items 3, 4, 5, 6, 7 have print frames; see §8).

**Do not implement in Design — plan only:**
- No Kotlin, no Compose, no XML. You never touch the repo.
- No new endpoints, no new query parameters, no changes to what the app *sends*.
- Do not design a room-allocation write flow. Allocation sync already exists as a separate user-initiated POST and is out of scope for this pass. Item 10 is **visual only**.
- Do not design an editor for the seating plan. The desk's own drag-and-drop seat editor is JavaScript and is dead in our viewer; our surface is read + print.

## 3. Design language — extend v4, do not invent a brand

The house style is called **Industry**. The default skin is **Steel**. Five skins exist (Steel, Paper, Blossom, Pond, Still) and no frame may know which one it runs — only the token ladder moves.

| Token | Steel hex |
|---|---|
| bg | `#F2F2F3` |
| surface | `#E9E9EA` (cards render at `#FAFAFB`, hairline `#DEDEE1`) |
| text | `#1D1F20` |
| neutral 100–900 | `#F5F5F8` `#E7E7EA` `#D4D4D7` `#B7B7BA` `#98989B` `#7A7A7D` `#5D5D60` `#424244` `#2B2B2D` |
| accent | `#5980A6` |
| accent 100–900 | `#EEF6FF` `#D6EBFF` `#B5D9FD` `#94BCE3` `#749DC4` `#597EA3` `#416180` `#2C455D` `#1D2D3D` |
| danger (fixed, never follows the skin) | `#A33A34` light / `#E0796F` dark |

**Type:** Barlow Condensed (titles, crumbs, kickers, buttons) · IBM Plex Mono (digits, counts, conf numbers, timestamps, kickers) · Roboto (body, controls). Nothing below 9 sp; no body text below 12.5 sp.

**Geometry:** spacing ramp 6 / 8 / 12 / 14 / 18 / 24 dp. Radius **8** (cards) / **6** (fields, tiles, chips) / **5** (segments). Desk rail **190 dp** wide, `#EFEFF0`, 1 dp right hairline `#E0E0E3`, selected item = `accent100` fill + 3 dp left `accent` bar. Cards `0 1 2 rgba(0,0,0,.05)`; desk tiles stay at elevation 0.

**Accent discipline:** accent means exactly one thing — *live, occupied, or selected*. Everything else is a hairline on the neutral ramp. On a page of 80 students, two accents is a lot.

**Print is monochrome.** Assume a shared mono laser printer with no colour and imperfect toner. Anything that carries meaning by colour on screen must carry it by weight, rule, or glyph on paper.

## 4. Transport truth — how these sheets actually arrive

Read this before you design anything; it decides what is buildable.

- Print-styled HTML pages: `GET /{slug}/{cid}/{courseId}` for `day0-list`, `teacher-list`, `manager-list`, `student-chit`, `checking-slip`, `seating`. Each page `@import`s its own small Drupal stylesheet (`day0-list.css`, `teacher-list-v2.css`, `manager-list-v2.css`, `student-chit.css`, `seating.css`).
- **Day 0 summary is not a page.** The app fetches `GET /zero-day/{cid}/{courseId}` and cuts out the `#day-summary` fragment: three bare tables (`#table-conf`, `#table-totals`, `#table-special`). The fragment arrives **with no stylesheet at all** — which is exactly why shot `02` looks like raw browser default. Any styling here is ours to invent.
- Streamed binaries (PDF/Excel) go to the system viewer, not to us: `course-pdf-m`, `course-pdf-f`, `laundry-list`, `valuable-list`, `report-day11`.
- The HTML sheets render in an in-app WebView that is **hardened: JavaScript OFF, no cookies, no DOM storage, no cache**. Three consequences you must design around:
  1. Every `col-toggle` button on the live sheets (`Columns: Occupation | Contact | Comments`) is a JS handler. **In our app those pills are dead furniture.** They render and do nothing. Same for `Print` (`onclick="window.print()"`), `Click to remove Seats`, `Click to remove Cells`, and the whole seating drag-and-drop.
  2. Every in-sheet hyperlink (`← Course page`, and every name link) is a dead end: the WebView has no session cookie, so a tap lands on a 403 or a login form.
  3. **Sort and order are URL parameters, not JavaScript** — `?conf=1` sorts Day 0 list by confirmation number; `?seating=1` orders the teacher list and the student chits in seating-plan order. Those are safe, ordinary GETs the app can re-issue. This is the one interactive affordance that survives, and you should use it.
- The app owns the HTML string before it reaches the WebView, so **injecting a stylesheet is free** and does not alter server content. A "desk-sheet skin" — one injected `<style>` block that hides dead chrome and restyles the tables onto Industry tokens — is a legitimate and expected part of your answer.
- **NEVER send an `r` query parameter on a sheet GET.** The live desk uses `?r=1` as "regenerate" and its mere presence triggers **server-side bulk seat auto-allocation** — it silently reshuffles every student's seat. The desk site links to `/seating/{cid}/{courseId}?r=1` as "Re-Gen"; **do not draw that control, do not label anything "regenerate", do not imply the app can re-generate seats.**
- Sheets are **display and print only**. HTML stays in memory; PDFs/Excel live in `cacheDir/sheets`; everything is wiped on logout, session expiry and erase-all. No sheet body is ever persisted or logged, because these pages carry phone numbers, emergency contacts and health disclosures.

## 5. The three cross-cutting problems

Solve these once and eight of the ten items get better:

**A. Dead chrome outranks content.** Every sheet opens with a toolbar of controls that cannot work (JS off) and links that cannot resolve (no cookie), drawn at the same or greater visual weight than the data. Shot `05` is the extreme case: 400 dp of blue instruction panel above the actual seating grid. **Design job: one native sheet toolbar, one injected stylesheet that hides `.no-print`.**

**B. The viewer header is doing a document's job.** Today it is `SHEET · VIEW ONLY` / title / `PRINT` / `CLOSE`, and the sheet's own `<div class="title">` repeats the title and the course line underneath it. Two headers, no controls. **Design job: one header that carries the course identity once, plus the sheet's real controls (sort/order, column visibility, print, close).**

**C. Tables were built for a desktop browser, not a 1280 dp tablet at arm's length.** Ten- and thirteen-column tables with no zebra, no column rhythm, and comment cells that grow rows to 120 dp. **Design job: a single table style for all sheets — column rhythm, hairline rows, mono for numerics, and a stated rule for how long free-text cells behave.**

## 6. Shot index

Screenshots are in `shots/` in the handover zip, captured on the Pixel C (2560×1800) and exported at 1024×720.

| # | Item | Shot |
|---|---|---|
| 1 | Day 0 list | `01-day0-list.png` |
| 2 | Day 0 summary | `02-day0-summary.png` |
| 3 | Student chits | `03-student-chit.png` |
| 4 | Checking slip | `04-checking-slip.png` |
| 5 | Seating plan — male page + instruction panel | `05-seating-plan-male.png` |
| 6 | Seating plan — female page | `06-seating-plan-female.png` |
| 7 | Teacher list | `07-teacher-list.png` |
| 8 | Manager list | `08-manager-list.png` |
| 9 | Course report — what the app returns today (raw CSV) | `09-course-report-csv.png` |
| 9b | Course report — the shape the desk produces (target content) | `10-course-summary-day11-pdf.png` |
| 10 | Main Board | `11-main-board.png` |
| 11 | Rooms & seats | `12-rooms-and-seats.png` |

Shots contain real student data. They stay in the zip and are never committed.

---

## 7. The ten items

### Item 1 — Day 0 list

**Current** (`01-day0-list.png`): `GET /day0-list/{cid}/{courseId}`, ~55 KB of HTML. Header `Day 0 List` + `Dhamma Sudha / 10 Day / 2026 / 2nd-Sep to 13th-Sep`. A `.day0-toolbar` with `← Course page · Print`, `Sort: Name · Confirmation no.`, `Columns: Occupation | Contact | Comments`. Then four group blocks — **Male|Old · Male|New · Female|Old · Female|New**, each with its own `n total` and its own 10-column table: `Sr · Conf No · Student · Crs · Education · Age · City · Occupation · Contact Details · Recom./Comments`.

**Problem:** ten columns of 12 px unstyled browser type with no zebra and no column rhythm; the registrar scans this while a student stands at the desk. `Contact Details` stacks a mobile and an email into one cell and eats a third of the width. `Recom./Comments` is free text of unpredictable length (`Can serve` / `Hindi`) and sets the row height. The three `Columns:` pills — the one control that would fix the width problem — are dead. Group headings (`Male | Old students | 19 total`) are the same weight as body text, so the four blocks read as one 83-row table.

**Design job:**
- Make the four group blocks unmistakable — this is the page's real structure. A sticky group band with gender, cohort and count, in the Board's kicker idiom.
- Fix the column rhythm. Propose explicit widths for all ten columns at 1280 dp, and decide what happens to `Contact Details` (two lines? mono for the number? a phone glyph?) and to `Recom./Comments` (clamp to two lines? own trailing column? hide by default?).
- Turn `Columns:` into **native toggle chips in the sheet toolbar** that drive a CSS rule (the column classes already exist: `d0-occ`, `d0-contact`, `d0-comments`). State the default set — I suggest Occupation and Comments on, Contact off, since Contact is the widest and the least used at the desk.
- Turn `Sort:` into a **native segmented control** — `Name | Conf no.` — that re-fetches with `?conf=1`.
- `Conf No` is the field staff read aloud and match against a chit: mono, bold, and the only accent-eligible cell on the row.

**Print vs screen:** both. Screen is the desk lookup; print is the day-0 clipboard. On paper: repeat the group band and the column header at every page break, drop `Sr` to grey, keep the whole row on one page (no orphan rows).

**Acceptance:** at 1280 dp, a 19-row group fits without horizontal scroll; the four groups are countable in one glance; the toolbar has no dead control; sorting by conf number works with a single tap; the printed sheet is legible mono at A4.

---

### Item 2 — Day 0 summary

**Current** (`02-day0-summary.png`): the `#day-summary` fragment from `GET /zero-day/{cid}/{courseId}` — three tables, **no CSS whatsoever**. `#table-conf` (Confirmed Male / Confirmed Female / Total × Old · New · Total · Server), `#table-totals` (same shape, Attended), `#table-special` (Male / Female / Total × Chowky · Chair · Backrest, each cell a string like `0 (O) + 0 (N)`).

**Problem:** it is the single most-read number set on day 0 and it renders as unstyled browser default in the top-left corner of a 1280 dp tablet — 15 % of the screen used, no hierarchy, three headers that look identical, `Total` bold via a stray unclosed `<b>` tag. There is no visual difference between "how many we expect" and "how many actually walked in", which is the only comparison that matters.

**Design job:**
- Recommend and draw a **native surface**, not a styled fragment. The payload is nine numbers plus six strings; it deserves the same treatment as the centre dashboard's course matrix (which already uses mono digits, group caps, a hairline gutter and a neutral band behind subtotals). Reuse that matrix idiom so the two screens rhyme.
- Make the Confirmed → Attended relationship the point of the screen. A gap of 60 vs 15 should be readable across the desk.
- Give `Chowky / Chair / Backrest` its own treatment: it is a *facilities* count (who needs a low seat, a chair, a back rest), not a roll count. Decide what `0 (O) + 0 (N)` becomes — old/new split as two mono figures under one header is my suggestion.
- State what happens when a number is zero. A grid of zeros on day −1 should look calm, not broken.

**Print vs screen:** screen-first (this is a glance surface, not a handout), but keep a clean print because it gets pinned up.

**Acceptance:** confirmed-vs-attended readable from 1 m; the three tables read as three distinct questions; no unstyled browser default anywhere; fits the fold at 1280×900 dp with room for the desk rail.

---

### Item 3 — Student chits

**Current** (`03-student-chit.png`): `GET /student-chit/{cid}/{courseId}`, ~4 KB. Repeated `.table-student-chit` blocks, each holding four divs: `.seat` → `"Seat: E4"` or `"Chowky: CW-A1"`, `.name` → given name `<br>` family name, `.cell` → `"Cell: "`, `.room` → `"Room No.:Mbk-27"`. Three across on screen. Above them: `Click to remove Seats`, `Click to remove Cells` (both dead JS) and a link `Click to Generate Student Chit in order of sitting plan` (`?seating=1`).

**Problem:** this is a **printed, cut-apart artefact** — 9 or 12 per A4, guillotined, handed to each student on arrival with their seat and room on it. It is currently designed as a web grid that happens to print. Specifically: `Cell:` is an empty label (the mobile-number column, unused at this centre) sitting directly above `Room No.:Mbk-27`, so every chit reads `Cell: Room No.:Mbk-27` — the redundancy the owner called out. `Room No.:` has no space after the colon. `Seat:` is empty for unassigned students, leaving a dangling label. The name breaks across two lines by accident of markup. No cut guides.

**Design job:**
- Design the **chit as a card for paper**: A4 portrait, both a **3×4 (12-up)** and a **3×3 (9-up)** grid. Give exact cell dimensions in mm for A4 at both densities, plus margins and gutters that survive a real guillotine.
- Hierarchy on the chit: the student reads their **seat** and their **room**. The name is for the volunteer handing it over. Set the type scale accordingly — seat largest, room second, name third.
- **Kill the redundancy.** `Cell:` renders only when a number exists; the room line becomes `Room` + value, not `Room No.:`. Empty `Seat:` collapses to nothing (or an explicit dashed placeholder if an unseated chit must still be printable — say which).
- Distinguish **Chowky** (`CW-A1`) from **Seat** (`E4`) at a glance; a chowky student needs a low wooden seat and the volunteer sorts by it.
- Add cut guides: hairline crop marks or a 0.5 pt rule grid. State whether the rule prints on the cut line or inside it.
- Fold the `?seating=1` "in order of sitting plan" link into the native toolbar as an **order segmented control** (`Name | Seating`). It is genuinely useful: chits in seating order get handed out row by row.

**Print vs screen:** **print-first.** The screen view is a proof sheet; say so in the frame. Draw the A4 sheet, not just one chit.

**Acceptance:** 12-up and 9-up A4 frames with mm dimensions; no chit shows an empty label; seat and room legible at arm's length on paper; a volunteer can sort a stack of 80 chits by seat without reading names.

---

### Item 4 — Checking slip

**Current** (`04-checking-slip.png`): `GET /checking-slip/{cid}/{courseId}`. Two-up cards, each `Name (Seat: E4)` / `Room No.:Mbk-27` / `Cell:` then a bilingual note — English then Hindi — telling the student to meet the teacher at `_____ AM/PM` at Teacher's Room / Interview Room / Dhamma Hall / Mini Hall / `_____`. There are form inputs above (`English-Text:` / `Hindi-Text:` + `Replace empty location with provided text`) which are dead JS in our viewer. The header still says `Student Chit`.

**Problem:** the owner rates this least important, so keep the pass cheap. But: the blanks a volunteer fills in by hand are `_______` underscores with no writing room; the English and Hindi blocks are the same weight so the eye can't pick its language; the two-up layout wastes A4; the header is wrong; and the fill-in-text form is dead furniture.

**Design job (small):**
- Fix the header to `Checking slip`.
- Give the two hand-filled blanks real writing space — a ruled box, not underscores. A volunteer fills these with a pen at speed.
- Separate the two languages visibly (a hairline, or Hindi at a slightly smaller size with a language tag). Do not translate or re-word: **the bilingual text is fixed content and ships verbatim.**
- Choose the A4 up-count (2-up or 4-up) and inherit the chit's cut-guide treatment from item 3.
- Hide the dead `English-Text` / `Hindi-Text` form.

**Print vs screen:** print-only for practical purposes. One A4 frame is enough.

**Acceptance:** correct title; blanks are writable; a Hindi reader finds their block immediately; shares item 3's cut treatment.

---

### Item 5 — Seating plan

**Current** (`05-seating-plan-male.png`, `06-seating-plan-female.png`): `GET /seating/{cid}/{courseId}`. Above the grid: a **large `#B9E3EE` instruction panel** — "Editing Seating Plan:" plus seven numbered steps about dragging students, `+ Blank Row`, `+ Col`, `− Col`, `Store Seat Changes`, print settings — then `Print Seating Plan` / `Store Seat Changes` buttons, a legend key cell, then `Seating Plan - MALE` / `- FEMALE` with the course line, then the actual grid of seat cells (`.s-seat-no`, `.s-name`, `.s-age`, `.s-acco`, `.s-cell`, `.s-old-student`, `.s-backrest`, teacher seats shaded) with `✥` drag handles on every cell, and `+ Blank Row / + Col / − Col / + Blank Col` buttons under each column group. Male and Female are separate pages (`.dh-page-sep`). Empty cells are real grid positions, not gaps.

**Problem:** the instruction panel and the edit affordances take the top ~400 dp of a 900 dp screen and **every one of them is dead in our app** — the drag-and-drop, the row/column buttons and `Store Seat Changes` are jQuery UI. The registrar came here to read the room and to print it. The drag handle `✥` on every cell is pure noise, ~80 glyphs competing with 80 names. Room codes (`Mbk-43`) and seat codes (`A7`) are the same size as the age. The legend key cell (`(O) old student · s: short courses · L: long courses · ▬ backrest · shaded = teacher`) is a tiny inline block that reads like a data cell.

**Design job:**
- **Delete the chrome.** No instruction panel, no drag handles, no row/column buttons, no `Store Seat Changes`, no "Regenerate" **ever** (see the `r` rule in §4). Replace with one line of native sub-text stating this is a read-and-print view.
- Design the **seat cell**: seat code, name, room, age, and the four flags (old student, short/long course counts, backrest, teacher). Establish which two of those are primary. Seat code is how the hall is walked; room code is how the student is found.
- Make **empty grid positions read as empty seats**, not as layout bugs. They are meaningful — the hall genuinely has gaps.
- Promote the legend to a real legend, once per page, in the kicker idiom.
- Draw **Male and Female as separate pages** with an explicit page break, and a page identity (`MALE` / `FEMALE` + centre + dates) that survives being printed and pinned to the hall door.
- On screen at 1280 dp: decide whether Male and Female sit side by side or stack with a section switch. Say which, and why.

**Print vs screen:** both, equally. The print goes on the hall wall and into the teacher's hand.

**Acceptance:** the grid starts within the first 120 dp of the screen; zero controls that cannot work; a teacher can find seat `C4` in under two seconds; A4 print keeps one gender per page with a legend and header on each.

---

### Item 6 — Teacher list

**Current** (`07-teacher-list.png`): `GET /teacher-list/{cid}/{courseId}`. `Order: Seniority · Seating plan` (`?seating=1`), `Columns: Cell | Languages | Comments` (dead JS). Group band `AT: {teacher} [{code}] | Male | Old | Group 1 | 16 total`, then 13 columns: `S/N · Student · Room · Age · City · Courses · Cell · Seat · Occupation · Education · Languages · Comments`.

**Problem:** thirteen columns. `Courses` is a dense code string (`10D:6 STP:2 20D:1 30D:2 45D:1`) that carries the student's whole practice history and is set in the same 12 px as `City`. `Comments` holds paragraph-length disclosures — one row in the shot is ~140 dp tall and pushes the next four teachers off the fold. `Cell` is often empty but still takes width. The group band is a `<th colspan="13">` that reads as a table row.

**Design job:**
- Set an explicit column priority for 1280 dp and state what the toggles do to it. `Courses`, `Room` and `Seat` are the teacher's working columns; `Occupation` and `Education` are context; `City` is nearly never read at the desk.
- **Design the `Courses` code string.** It is the most information-dense cell in the app. Mono, with a rule for the separator and for weight on the primary count. Do not expand the codes into words — the teachers read the codes.
- **State a comment rule.** My suggestion: clamp to two lines with a `⌄` expander on screen, full text on print. Draw both states. A single long comment must not cost four rows of vertical space.
- Turn the group band into a real band (teacher name + code, gender, cohort, group, count) in the kicker idiom, sticky on scroll.
- `Order:` becomes a native segmented control (`Seniority | Seating`, re-fetching `?seating=1`); `Columns:` becomes native chips (`tl-cell`, `tl-langs`, `tl-comments`).

**Print vs screen:** both. The teacher gets paper; the registrar uses the screen.

**Acceptance:** at least 8 rows visible on the fold regardless of comment length; `Courses` parses in one look; column header and group band repeat on printed page breaks.

---

### Item 7 — Manager list

**Current** (`08-manager-list.png`): `GET /manager-list/{cid}/{courseId}`. `Columns: Cell` (dead). Group band `Male | 13 total` plus a second band `Teachers  {teacher} [{code}]`. Ten columns: `S/N · Student · Age · Room · Emergency Contact · Emergency No. · Cell · Seat · Conf No · Set`.

**Problem:** this is the **emergency sheet** — the one a course manager grabs when someone needs help at 02:00 — and it is styled exactly like every other table, with the emergency columns given no more weight than `S/N`. `Emergency Contact` mixes a name and a relationship in one cell (`{name} (Mother)`). `Cell` is empty on every row in the shot and still holds a column. The `Print` control is duplicated: the sheet's own `Print` link plus the viewer's `PRINT` button, and only one of them works.

**Design job:**
- Design for the 02:00 use: **name → room → emergency number**, in that order, findable by touch on paper in bad light. That may mean re-ordering columns away from the server's order — you may, this is a render.
- The emergency number is dialled under stress: mono, largest numeric on the row, grouped digits.
- Split `Emergency Contact` into person and relationship visually (relationship smaller/greyer), without changing the string.
- Drop `Cell` from the default column set and let the toggle bring it back (`ml-cell`).
- **Resolve the duplicate Print.** The injected stylesheet hides the sheet's own `.no-print` toolbar; the viewer's `PRINT` is the only one. Show that in the frame.
- Keep the teacher band — the manager needs to know which AT to wake.

**Print vs screen:** **print-first**, and design for a photocopy. This sheet lives on a clipboard in the office.

**Acceptance:** an emergency number is findable in under three seconds on paper; exactly one Print control; no empty column by default.

---

### Item 8 — Course report, onto the centre dashboard

**Current:** the app has a `Course Report` chip on the centre screen under `MORE ON THE DESK SITE`, wired to `SheetExport.CourseReport`. It scrapes the Drupal form `dh_center_course_report_form` at `GET /centre/{cid}/course-report` (fields `report_from_date[date]`, `report_to_date[date]`, defaults last-year → today), POSTs it, and gets back **raw CSV** — which the app hands to the system viewer. Shot `09-course-report-csv.png` is what the registrar actually sees: a wall of unaligned comma-separated text.

The CSV's real columns are: `Course, NewMale, NewFemale, NewTotal, OldMale, OldFemale, OldTotal, StudentTotal, SevakMale, SevakFemale, SevakTotal, ConductingTeachers, AssistingTeachers, TrainingTeachers` — one row per course in the date range, with a grand-total row at the bottom. The `Course` value is a single string like `Dhamma Sudha / 10 Day / 2026 / 3rd-Sep to 14th-Sep`, and teacher names spill onto following lines.

Shot `10-course-summary-day11-pdf.png` shows the *content shape* the centre expects from a report: centre name, course dates, course type, address, assistant teachers by gender and role, then an Indian/Foreigner × New/Old × Dhamma Sevak matrix with a `LEFT` row. That one is the separate **Day-11 PDF** (`GET /report-day11/{cid}/{courseId}`), which stays a Board chip and is not yours to redesign — it is here as a **content reference** for what registrars consider a finished report.

**Problem:** a CSV dump is not a report. And it sits in the wrong place: it is centre-level and multi-course, filed under a course-desk export shelf and a desk-site link row.

**Design job:**
- Design a **native `Course report` surface, reached from a tile on the centre dashboard next to `Centre Settings`** — same transparent-fill, zero-elevation, hairline tile treatment as the existing three (`Centre Settings`, `Advanced Search`, `App Settings`). Draw the dashboard with four tiles and say how the row reflows.
- Design the screen itself: a **date-range control** (the form already takes from/to — this is the only input the server accepts, so it is the only control you may draw), then a **native table** of the CSV's columns with the course string parsed into readable parts (centre · type · year · dates), a grand-total row that reads as a total, and the New/Old/Sevak groupings expressed the way the centre matrix already does it (group caps, mono digits, hairline gutter, neutral band behind subtotals).
- Decide the column set for 1280 dp. Fourteen columns is too many; propose what collapses (New/Old/Sevak trios under group caps, teacher counts as a compact trailing trio).
- Design the **empty, loading and refusal states.** The server can answer with a re-rendered form or a 403 HTML page; the app renders server messages **verbatim, unmodified** — draw that state with a long, ugly server sentence in it.
- Keep a way out to the raw file (`Share CSV` or `Open CSV`) for the registrar who wants it in a spreadsheet. Secondary, not primary.
- **Print:** an A4 frame. This report is emailed and filed.

**Acceptance:** reachable in one tap from the centre dashboard; no CSV text visible in the default path; totals distinguishable from course rows; a date range change re-runs the report; refusal state shows the server's own words.

---

### Item 9 — Main Board

**Current** (`11-main-board.png`): roll sentence, four 100 dp stat cards (`ARRIVING TODAY`, `CHECKED IN`, `STILL TO CALL`, `NEEDS ATTENTION`), a `NEXT` block of three 58 dp action rows, then `SHEETS & EXPORTS  RARELY URGENT` — twelve 40 dp chips on three labelled shelves (`ROLL SHEETS`, `DESK SLIPS`, `FOR THE TEAM`), four per row, plus a **full-width fourth-line chip** `Day 11 · Course summary report`.

**Problem:** modest but real. `Course report` currently sits on the `FOR THE TEAM` shelf and **must move to the centre dashboard** (item 8), which leaves an eleven-chip grid. The three shelves are visually identical, so the `RARELY URGENT` kicker is doing all the de-emphasis on its own. The four stat cards and the three `NEXT` rows both compete to be the first thing read.

**Design job:**
- **Remove `Course report` from `SHEETS & EXPORTS`** and decide what shelf 3 becomes: three chips at 4-column width with a hole, three chips stretched across, or a re-cut of the twelve into new groupings. State the choice.
- **Keep the Day-11 chip** exactly where it is in principle — a solid, full-width, fourth-line row under the 3×4 grid. It is **not** a 13th cell in the grid and the dashed "GAP" marker from the v4 canvas is dead history. Do not re-propose either.
- Sharpen the reading order: numbers → next actions → exports. The export band should be visibly the quietest thing on the page.
- The stat cards are navigation, not decoration (each opens a rail section). Make the tap affordance legible without adding chrome.
- The roll sentence (`68 on the roll, 46 already in their rooms. Everything below is a number you can act on — tap it.`) is deliberate and stays.

**Print vs screen:** screen only.

**Acceptance:** no `Course report` chip on the Board; Day-11 still on its own fourth line; the whole Board lands on one fold at 1280×900 dp with no scroll; the export band recedes.

---

### Item 10 — Rooms & seats

**Current** (`12-rooms-and-seats.png`): the desk's occupancy grid. One block per gender + accommodation section (`Female · Fbk`, `Male · Mbk`), each with `n rooms · n free`, then a grid of room cells at the centre's configured column count (default 4, 1–12, set in Centre Settings). Occupied cells are `accent100` fill + accent border + `accent800` room number; free cells are card fill with a grey number and the word `free`. Alternate rows sit on a soft neutral band.

**Problem:** every occupied cell reads `{occupant name} ( View )` — the `( View )` is a **link remnant scraped from the desk's HTML**, not a control. It is repeated 68 times in accent-adjacent grey and it is the loudest repeated string on the screen. Beyond that: the room number and the occupant name compete (the number is 19 sp bold condensed, the name 11.5 sp), amenity marks (`G` geyser · `IC` Indian toilet · `W` Western) are 9.5 sp mono in the same row as the number, and at 40+ free rooms the word `free` repeats as much visual mass as the occupied cells.

**Design job:**
- **Remove `( View )`.** It is a parse artefact and must never render. (Implementer note: strip it where `( PDF )` is already stripped.)
- Re-balance the cell: room code, occupant, amenity marks. Decide what a **free** cell looks like when there are 40 of them — it should read as available capacity, not as 40 empty cards demanding attention.
- Give the block header its `n rooms · n free` counts real weight; that ratio is what the registrar is actually looking for when they open this pane.
- Amenity marks need a legend (currently only a sub-line at the top of the pane) and a treatment that does not compete with the room code.
- Keep `PULL FROM SERVER`, and keep the `SYNC N TO SERVER` accent button — the pane's one deliberate accent fill, hidden at N=0. **Do not design a new allocation write flow, a per-cell assign control, or a drag-to-assign.** Visual only.
- The occupied/free distinction must survive greyscale (it is not printed, but skins change).

**Print vs screen:** screen only.

**Acceptance:** `( View )` gone; occupied vs free readable at a glance across a 70-room block; 12-column layout still legible at 1280 dp; no new write affordance.

---

## 8. Frames to draw

Each numbered frame = one before/after pair with notes, in the v4 `.dc.html` idiom. Screen frames at 1280×900 dp; print frames at A4 proportion with mm annotations.

| Frame | Subject | Kind |
|---|---|---|
| 5a | Sheet viewer chrome — the shared native toolbar (title, course identity, order/sort segmented control, column chips, PRINT, CLOSE) and the injected desk-sheet stylesheet, shown on Day 0 list | screen |
| 5b | Day 0 list — screen, all four group blocks, default column set | screen |
| 5c | Day 0 list — A4 print, with a page break | print |
| 5d | Day 0 summary — native surface | screen |
| 5e | Student chit — A4 12-up | print |
| 5f | Student chit — A4 9-up + one chit at 2× with anatomy callouts | print |
| 5g | Checking slip — A4 | print |
| 5h | Seating plan — screen, male page, chrome removed | screen |
| 5i | Seating plan — A4, one gender per page, legend + header | print |
| 5j | Teacher list — screen, both comment states (clamped + expanded) | screen |
| 5k | Teacher list — A4 print | print |
| 5l | Manager list — screen | screen |
| 5m | Manager list — A4 print, emergency-first | print |
| 5n | Centre dashboard with the fourth tile (`Course report` beside `Centre Settings`) | screen |
| 5o | Course report — native surface, loaded state with date range | screen |
| 5p | Course report — empty / loading / verbatim-refusal states | screen |
| 5q | Course report — A4 print | print |
| 5r | Main Board — `Course report` removed, shelf 3 resolved, Day-11 row kept | screen |
| 5s | Rooms & seats — `( View )` gone, rebalanced cells, free-cell treatment | screen |
| 5t | Table style specimen — the one table style all sheets inherit (columns, rules, mono, group band, clamped free text), screen and print side by side | both |

Draw 5a and 5t first: they are the shared vocabulary the other frames spend.

## 9. Hard constraints — you and the implementer both inherit these

1. Live desk is Drupal HTML at `https://dipi.vridhamma.org`. **Backend PHP is immutable.** No `/staff/*` routes, no new JSON contracts, no new endpoints.
2. **No access control in the app.** Send the request, render the server's response **verbatim** — including its error text, unmodified.
3. **Never send status `Approved`.** No status engine in Kotlin; display and send strings only.
4. **Never persist or log NPI** — Aadhaar, PAN, passport, voter ID, emergency contacts, health disclosures, phone numbers, email. ID documents and health disclosures MAY be shown on screen for desk verification (owner decision 2026-08-16) but live in an in-memory session map only. Never in Room, DataStore, a DTO, or a log line.
5. Sheets are **display and print only**: HTML in memory, documents in `cacheDir/sheets`, all wiped on logout / session expiry / erase-all.
6. **NEVER send an `r` query parameter on a sheet GET.** Its presence triggers server-side bulk seat auto-allocation. `?conf=1` and `?seating=1` are safe and expected.
7. No attendance writes. This pass adds **no write protocol at all** — allocation sync already exists and is out of scope.
8. Status and severity colours are **fixed hexes** and never follow the skin.
9. Touch targets ≥ 48 dp. No frame may assume a mouse.
10. The design file wins visual arguments, but it must **extend** the v4 Industry/Steel tokens listed in §3 — no new brand, no new type family, no new radius, no colour outside the ladders.

## 10. Do not re-propose

Already decided, already shipped, or explicitly retired by the owner:

- **Manage Courses · Daily Activity · SMS Report · Letters** — retired from the app 2026-08-30. Still on the desk site. Do not bring them back in any form.
- **A 13th cell in the Board's 3×4 export grid**, or the dashed "GAP — NOT IN 1.22.0" Day-11 marker. Day-11 is a shipped solid fourth-line row.
- **Server-side Advanced Search** (`POST /search-app`) — parked pending verification and owner sign-off.
- **Lotus photographs**, one-line course counts, raised desk tiles, `ON/OFF` text instead of switches, the room chart as a small link, App Settings hidden in overflow — all removed on purpose.
- **A seating editor, a "Regenerate seats" control, or anything that sends `r`.**
- **Letter bodies or SMS/WhatsApp previews.** Letters, waitlist and dispatch are black boxes behind the desk's own status change. Never reimplement, never preview.
- **A photo upload flow.** No live desk route exists; it is mock-only.

## 11. What to hand back

1. `DIPI Staff v5.dc.html` — the frames in §8, before/after, dp-accurate, with per-frame notes.
2. A short written spec per item in the v4 `README.md` idiom: purpose, layout bands with dp, type and token per element, interaction, and the print rule.
3. An explicit list of **open questions for the owner** — anything you had to guess (default column sets, 9-up vs 12-up, side-by-side vs stacked seating). Flag them; do not silently decide.
4. Nothing else. No code.
