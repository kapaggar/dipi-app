# 2b — Teacher list (roll parser + screen) — spec

**Status:** approved for build, 2026-09-02. **Baseline:** post-2a (Wave 1 peer).
**Design:** frame 2b + `docs/DESIGN.md` § Course ops (2b + ground-truth
corrections). Server contract from `dh_manageapp/inc/zero-day.inc:877-1072`.

## S1 — Fetch (once, never poll)

Reuse `StaffApi.sheetPage("teacher-list", cid, courseId)` (path-only params —
the no-`r` rule is structural). New repository fn `loadTeacherRoll(cid,
courseId): TeacherRoll` fetching on: entry to course ops, process start in
course ops. NO other refetch — the endpoint mutates server data on GET
(`zeroize_new_course_data`). Auth handling: login-HTML/403 → `unauthorized`
ApiException (existing pattern); 404 → verbatim error surface. Success body is
an UNTHEMED fragment starting `<style>` — the parser must not expect
`<html>`/`<body>`.

## S2 — Parser (`core/network/TeacherListParser.kt`)

Follow the `AttendedTableParser` conventions (precompiled regex, arity guard,
per-cell normalisers, NPI discipline in the KDoc). Structure per the PHP:
- Blocks: each `<table class="table-teacher-list">` inside `tl-block` divs.
- Band `<th class="tl-groupinfo">`: strip tags, split on `|`, trim → tokens
  `[AT: {name} [{code}]', 'Male|Female', 'Old|New', 'Group {g}', '{N} total']`.
  AT text may be `(unassigned)`. Old/New comes ONLY from here.
- Rows: 12 `<td>`s in server order (S/N, Student, Room, Age, City, Courses,
  Cell, Seat, Occupation, Education, Languages, Comments). `tlc` span wrappers
  on langs/comments are stripped like any tag. **Comments (index 11) is never
  read** — skip the cell entirely, never store it (unlabelled health text).
- Student cell: name + optional `<b>(SUFFIX)</b>` tokens — parse suffixes
  `(Sevak)`, `(BT[-yr])`, `(T[-yr])`, `(SAT[-yr])`, `(AT[-yr])` into
  `roleTag: String?`.
- Courses cell: pairs `<b>KEY:</b>N` with KEY ∈ 10D STP SPL TSC 20D 30D 45D
  60D → ordered `List<Pair<String,Int>>` (empty list = new student, renders
  nothing).
- Seat cell: raw label; split prefix `CW-`/`CH-` into `seatKind` (CELL/CHAIR/
  FLOOR), keep the display string; `BR` span → `backrest: Boolean`. Blank seat
  = unseated.
Models in `core/model/TeacherRoll.kt` — NOT `@Serializable` here; a separate
serializable snapshot DTO lives in the course store (2d owns persistence).
`TeacherRoll(groups: List<RollGroup>)`; `RollGroup(at, code, gender, seniority
Old/New, group, total, rows)`; `RollRow(sn, applicantId?, name, roleTag, room,
age, city, courses, cell, seat, seatKind, backrest, occupation, education,
languages)`. `applicantId`: the page carries no id attribute on this table —
**verify against a live capture in the mock fixture step; if truly absent, the
card must be keyed by (name, room) → BLOCKED-check with the integrator before
2d.** (The PHP SELECT has `a_id`; check whether any attribute/link carries it —
if not, 2d's prefetch maps by row order via a one-time `/zero-day` merge or the
search worklist; decide with evidence, record the ruling.)

## S3 — Screen (`:feature:teacher` module, new)

New Gradle module `:feature:teacher` mirroring `:feature:desk`'s build file (no
test source set; tests in `:app`). `TeacherListScreen` per frame 2b:
- Header 62dp: title + course line; destination pair `Seniority`/`Seating plan`
  (48dp, selected white on 1.5dp accent) — a two-way segmented control over the
  ONE fetched response; switching never refetches. ⚙ affordance from 2a.
- Group filter band 44dp: one 30dp pill per group (label + mono count), tap
  filters to that group, tap again clears.
- Sticky group band 34dp (`accent100` on `accent300`): band text from the parse
  (` · ` joined per the frame), count right.
- Column header 28dp + 52dp rows exactly per the frame widths (S/N 34, STUDENT
  flex, ROOM 86, AGE 46R, CITY 124, COURSES 236, SEAT 64R, FLAGS 96R). Student
  = name 15.5sp + folded `occupation · education · languages` 11.5sp line
  (em-dash for blanks). Courses chips 20dp (`10D 6` style, nothing when empty).
  Seat mono SemiBold. FLAGS pills come from 2d's store — render whatever set
  the state carries (empty until prefetch lands; MONK included).
- Next-group footer peek 40dp (band text + count + `›`, not a control).
- Row tap → student card (2d) — until 2d lands, the callback is a defaulted
  no-op.
- Offline: `SyncBannerStrips(offline, queued = 0)` above content, pushes down.
- NEVER re-sort: render groups and rows in parse order.

## Tests

- `TeacherListParserTest` (core/network): fixture built VERBATIM from the PHP
  shapes — pipe band incl. `(unassigned)`, 12 columns, `tlc` wrappers,
  suffixes, `CW-`/`CH-`/`BR`, empty seats, S/N restart across blocks, courses
  pairs incl. SPL, comments cell present-but-never-parsed (assert the model
  has no comments field and the fixture's health text string appears NOWHERE
  in the parsed output — the load-bearing NPI assertion).
- Mock: `MockFixtures.teacherListHtml(...)` replacing the generic sheet body
  for this slug + dispatcher branch; `ExportMockTest`-style request-line
  assertion `GET /teacher-list/{c}/{co}` with no query.
- `TeacherListScreenTest` (app, w1240dp-land): groups render in given order
  (positional assertion), never re-sorted even when a test feeds shuffled-
  looking input; group pill filters; sticky band text verbatim; row 52dp
  bounds; empty-courses renders no chip; folded line em-dashes; footer peek
  text.
Never touched: every desk test; `SheetExport`/`ExportMockTest` existing
assertions (teacher-list stays a Board export in desk mode too).
