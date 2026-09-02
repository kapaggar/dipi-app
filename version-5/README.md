# version-5 — desk sheets, Course report, Board and Rooms

A design handover for **DIPI Staff** (`org.dhamma.dipi.staff`), following the `version-4` pattern: a prompt for Claude Design, a shot index, a redacted route map, and a separate handover for the Claude Code agent that implements the result.

Shipped baseline: **1.30.5** (`versionCode` 51) on `main`. This pass will land as a **MINOR**.

## Files

| File | What it is | In git? |
|---|---|---|
| `PROMPT.md` | The Claude Design brief. Self-contained: ten items, frames to draw, hard constraints. | yes |
| `README.md` | This file — how to use the handover, shot index, route map pointer. | yes |
| `IMPLEMENT.md` | Handover for the Claude Code agent *after* Design: file ownership, SemVer, test command, never-touch list. | yes |
| `HAR-ROUTES.md` | Redacted route map from the 2026-09-02 zero-day sweep: paths, content types, which sheet each feeds. | yes |
| `CLAUDE-DESIGN.md` | Paste-ready first message for Claude Design. | zip only |
| `shots/01…12.png` | Pixel C captures of the ten surfaces. | **zip only** |

## Why the screenshots are not in git

Every sheet capture carries real student data — names on all of them, and readable **phone numbers and email addresses** on `01-day0-list.png`, plus emergency contacts and numbers on `08-manager-list.png`. `AGENTS.md` hard rule 10 forbids committing real student data, so **no shot is committed**, not even the two that happen to be clean (`02`, `11`). One rule, no judgement calls.

The shots live in the handover zip only:

```
/Users/wizops/Downloads/dipi-sheets-v5-handover.zip
```

Unzip it somewhere outside the repo, hand the folder to Claude Design, and paste `CLAUDE-DESIGN.md` as the first message. Do not copy `shots/` back into the tree.

The source HAR (`~/Downloads/dipi.vridhamma.org.har`, 5.6 MB) is **not** in the zip and **not** in git. It contains session cookies, CSRF tokens and 178 full response bodies including student rows. `HAR-ROUTES.md` is the only thing derived from it that is safe to keep.

## Shot index

Captured on the Pixel C (2560×1800), exported at **1024×720** — the Day-11 PDF page (`10`) at 1024×777. Session `sudha.user` · Dhamma Sudha, courses `10 Day / 2026 / 19th-Aug to 30th-Aug` and `2nd-Sep to 13th-Sep`.

| # | Zip name | Surface | Source | Notes |
|---|---|---|---|---|
| 1 | `01-day0-list.png` | Day 0 list | `GET /day0-list/{cid}/{courseId}` | **Readable phones + emails.** Male\|Old block, 19 total, 10 columns |
| 2 | `02-day0-summary.png` | Day 0 summary | `#day-summary` of `GET /zero-day/…` | Unstyled fragment. Counts only, no names |
| 3 | `03-student-chit.png` | Student chits | `GET /student-chit/…` | Shows the `Cell: Room No.:` redundancy |
| 4 | `04-checking-slip.png` | Checking slip | `GET /checking-slip/…` | Header still reads "Student Chit"; bilingual note |
| 5 | `05-seating-plan-male.png` | Seating plan, male | `GET /seating/…` | The 400 dp instruction panel + drag handles |
| 6 | `06-seating-plan-female.png` | Seating plan, female | same page, second gender page | Grid + `+ Blank Row / + Col / − Col` row |
| 7 | `07-teacher-list.png` | Teacher list | `GET /teacher-list/…` | 13 columns; one ~140 dp comment row |
| 8 | `08-manager-list.png` | Manager list | `GET /manager-list/…` | **Emergency contacts + numbers.** Empty `Cell` column |
| 9 | `09-course-report-csv.png` | Course report **today** | `POST /centre/{cid}/course-report` → CSV | What the app actually returns: raw CSV in a system viewer |
| 10 | `10-course-summary-day11-pdf.png` | Day-11 PDF | `GET /report-day11/…` | **Content reference only** — the shape registrars expect from a report. Not to be redesigned |
| 11 | `11-main-board.png` | Main Board | in-app | 12 chips on 3 shelves + Day-11 fourth-line row |
| 12 | `12-rooms-and-seats.png` | Rooms & seats | in-app | Every occupied cell reads `( View )` |

Items 1–10 in `PROMPT.md` map to shots as: 1→`01`, 2→`02`, 3→`03`, 4→`04`, 5→`05`+`06`, 6→`07`, 7→`08`, 8→`09`+`10`, 9→`11`, 10→`12`.

## Route map, in one glance

Full table with content types and sizes in `HAR-ROUTES.md`. The short version:

- **HTML sheets** (rendered in-app, JS off, no cookies): `day0-list`, `teacher-list`, `manager-list`, `student-chit`, `checking-slip`, `seating`.
- **Fragment**: Day 0 summary is the `#day-summary` block cut out of `zero-day` — three tables, no stylesheet.
- **Streamed documents** (handed to the system viewer): `course-pdf-m`, `course-pdf-f`, `laundry-list`, `valuable-list`, `report-day11`.
- **Form POST**: Course report is `dh_center_course_report_form` at `/centre/{cid}/course-report` → CSV.
- **Safe query params**: `?conf=1` (Day 0 list sort), `?seating=1` (teacher list, student chit, course PDFs).
- **Forbidden**: `?r=1`. The desk site links to it as "Re-Gen" on `seating`, `group-seating` and `cell-list`; its presence triggers **server-side bulk seat auto-allocation**. Never send it.
- **Seen in the HAR but not used by the app**: `GET /group-seating/{cid}/{courseId}` (group-wise seating, 27 KB HTML) and `GET /cell-list/{cid}/{courseId}`. Neither is in scope for this pass — noted so nobody "discovers" them mid-implementation and adds a chip.

## What Design must not re-propose

`docs/DESIGN.md` §"Shipped delta ledger" is the authority; `PROMPT.md` §10 carries the short list. The three that get re-proposed most often:

1. **Manage Courses / Daily Activity / SMS Report / Letters** — retired by owner decision 2026-08-30.
2. **A 13th cell in the Board's 3×4 export grid**, or the dashed "GAP" Day-11 marker from the v4 canvas. Day-11 shipped as a solid full-width fourth-line row (T1, 2026-08-31).
3. **Server-side Advanced Search** (`POST /search-app`) — parked, needs HAR verification and owner sign-off.

## Reading order

1. `docs/DESIGN.md` — design authority and the shipped-delta ledger.
2. `docs/LIVE-DESK.md` — transport, page inventory, server facts.
3. `AGENTS.md` — hard rules (no client ACL, no `Approved`, no attendance write, verbatim server messages, bridge rule).
4. `PROMPT.md` — this pass.
