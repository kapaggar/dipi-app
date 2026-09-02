# HAR route map — redacted

Derived from a browser trace of the live desk (`dipi.vridhamma.org`) during a **zero-day activity sweep** for a course starting that day, captured 2026-09-02. 178 entries.

**What was removed, deliberately:** the HAR itself, every request and response header (so no `Cookie`, no `Set-Cookie`, no CSRF or `form_token`), every request body, and every response body. No student row, name, phone, email, Aadhaar/PAN/passport/voter ID, emergency contact or health disclosure appears here or anywhere in this folder. What survives is method, path, query parameter *names*, response status, content type, body size and hit count — plus the CSS class and table-header skeletons of the sheet markup, which are server template structure, not data.

The source HAR stays at `~/Downloads/dipi.vridhamma.org.har`. **Do not commit it and do not put it in the handover zip.**

Centre `63`, course `66884` in the capture. Read them as `{cid}` and `{courseId}`.

## Desk pages the app already consumes

| Method | Path | Status | Content-Type | Size | Feeds |
|---|---|---|---|---|---|
| GET | `/zero-day/{cid}/{courseId}` | 200 | `text/html` | 67.6 KB | **Day 0 summary** (`#day-summary` fragment) and **room merge** (`#table-attending`) |
| GET | `/day0-list/{cid}/{courseId}` | 200 | `text/html` | 55.0 KB | Day 0 list |
| GET | `/teacher-list/{cid}/{courseId}` | 200 | `text/html` | 13.9 KB | Teacher list |
| GET | `/manager-list/{cid}/{courseId}` | 200 | `text/html` | 8.8 KB | Manager list |
| GET | `/student-chit/{cid}/{courseId}` | 200 | `text/html` | 4.0 KB | Student chit |
| GET | `/seating/{cid}/{courseId}` | 200 | `text/html` | 33.8 KB | Seating plan |
| GET | `/laundry-list/{cid}/{courseId}` | 200 | `application/vnd.ms-excel` | streamed | Laundry list → `cacheDir/sheets` |
| GET | `/valuable-list/{cid}/{courseId}` | 200 | `application/vnd.ms-excel` | streamed | Valuable list → `cacheDir/sheets` |
| GET | `/course/{cid}/{courseId}` | 200 | `text/html` | 20.9 KB | Course page — the desk's own hub; source of the per-course `Course Summary` matrix |

Not exercised in this capture but wired in the app and unchanged: `GET /checking-slip/{cid}/{courseId}` (HTML), `GET /course-pdf-m|course-pdf-f/{cid}/{courseId}` (PDF), `GET /report-day11/{cid}/{courseId}` (PDF), `GET /centre/{cid}/acco-handler` (room config, read-only), `GET /search-course/{cid}/{courseId}?s=&t=&g=&d=a` (worklist `var dataset`), `GET /change-status/{id}?s=&l=0&c=` (the one status write).

## Routes present on the desk but **not** used by the app

| Method | Path | Status | Content-Type | Size | Note |
|---|---|---|---|---|---|
| GET | `/group-seating/{cid}/{courseId}` | 200 | `text/html` | 26.9 KB | Group-wise seating. Same markup family as `/seating`. Out of scope for v5 — do not add a chip for it |
| GET | `/cell-list/{cid}/{courseId}` | — | — | — | Linked from the course page only. Out of scope |

## Query parameters

| Param | Seen on | Verdict |
|---|---|---|
| `?conf=1` | `day0-list` | **Safe.** Sorts by confirmation number instead of name. Use it for the native sort control |
| `?seating=1` | `teacher-list`, `student-chit`, `course-pdf-m`, `course-pdf-f` | **Safe.** Orders rows/chits by seating plan. Use it for the native order control |
| `?r=1` | `seating`, `group-seating`, `cell-list` — the desk's own "Re-Gen" links | **FORBIDDEN.** Presence triggers server-side bulk seat auto-allocation. Never send it, never draw a control that implies it |
| `?tkp923`, `?v=…` | static CSS/JS | Drupal cache-busters, irrelevant |

## Sheet markup skeletons

Server template structure only — no cell values.

### `day0-list` → `day0-list.css`

`.header-day0` (`.title`, `.title-head`) · `.day0-toolbar.no-print` → `.grp` × 3: back link + `Print` (`onclick="window.print()"`), `Sort: Name | Confirmation no.` (the second is `?conf=1`), `Columns:` three `<button class="col-toggle" data-toggle-col="occ|contact|comments">` · then four `.day0-block` groups, each `.day0-scroll > table.table-day0-list` with `<colgroup>` classes `c-sr d0-sr`, `c-conf d0-conf`, `c-student d0-student`, `c-courses d0-courses`, `c-edu d0-edu`, `c-age d0-age`, `c-city d0-city`, `c-occ d0-occ`, `c-contact d0-contact`, `c-comments d0-comments`.

Group headers, in order: `Male|Old students|n total`, `Male|New students|n total`, `Female|Old students|n total`, `Female|New students|n total` (the `|` are `.sep` spans, the count a `.cnt` span). Column headers: `Sr · Conf No · Student · Crs · Education · Age · City · Occupation · Contact Details · Recom. / Comments`. `.day0-break` marks the print page break.

### `teacher-list` → `teacher-list-v2.css`

`.header-teacher` · `.tl-toolbar.no-print` → back + `Print`, `Order: Seniority | Seating plan` (`?seating=1`), `Columns:` `col-toggle` for `cell|langs|comments` · `.tl-block > .tl-scroll > table.table-teacher-list`, colgroup `tl-sn tl-student tl-room tl-age tl-city tl-courses tl-cell tl-seat tl-occ tl-edu tl-langs tl-comments`. Group band is a `<th class="tl-groupinfo" colspan>` reading `AT: {teacher} [{code}] | {gender} | {cohort} | Group n | n total`. Headers: `S/N · Student · Room · Age · City · Courses · Cell · Seat · Occupation · Education · Languages · Comments`.

### `manager-list` → `manager-list-v2.css`

`.header-manager` · `.ml-toolbar.no-print` → back + `Print`, `Columns:` one `col-toggle` for `cell` · `table.table-manager-list`, colgroup `ml-sn ml-student ml-age ml-room ml-ename ml-enum ml-cell ml-seat ml-conf ml-set`. Two stacked `<th class="ml-groupinfo">` bands: `{gender} | n total`, then `.ml-teachers` (`.lbl` "Teachers" + names with `.code`). Headers: `S/N · Student · Age · Room · Emergency Contact · Emergency No. · Cell · Seat · Conf No · Set`.

### `student-chit` → `student-chit.css`

`.header-day0` (`.title` "Student Chit", `.title-head` course line) · two dead buttons `.no-print.remove-seat` / `.no-print.remove-cell` · a `.no-print` link to `?seating=1` · `.main-div` → repeated `.table-student-chit`, each containing exactly four divs: `.seat` (`"Seat: E4"` / `"Chowky: CW-A1"` / `"Seat: "` when unassigned), `.name` (given `<br>` family), `.cell` (`"Cell: "` — the mobile column, empty at this centre), `.room` (`"Room No.:Mbk-27"`, no space after the colon). The `.cell` + `.room` adjacency is the `Cell: Room No.:` redundancy the owner flagged.

### `checking-slip`

Same `.header-day0` family (title still reads "Student Chit" on the live page). Per-student card: `{name} ({seat})`, room, empty `Cell:`, then the fixed bilingual meet-the-teacher note (English then Hindi) with two hand-filled blanks — a time and a location. A `.no-print` form offers `English-Text:` / `Hindi-Text:` + `Replace empty location with provided text`; it is JavaScript and dead in our viewer.

### `seating` and `group-seating` → `seating.css`

`.helptext` (the large instruction panel) · `.no-print` buttons `Print Seating Plan`, `.store-seat-changes` · `.seat-legend` (`.legend-bar`, `.legend-cell`, `.legend-swatch`, `.legend-note`) · `.plan-header` / `.plan-header-title` (`Seating Plan - MALE` / `- FEMALE` + centre/type/year/dates) · `.sortable-m` / `.sortable-f` grids on `.bg-table-m` / `.bg-table-f`, cells carrying `.s-seat-no`, `.s-name`, `.s-age`, `.s-acco`, `.s-cell`, `.s-app-id`, `.s-old-student`, `.s-backrest` / `.s-backrest-yes`, `.teacher-seat`, `.chowky-div`, `.cwch` · per-column `.dh-add-col`, `.dh-blank-col`, `.dh-del-col`, `.add-row` · `.dh-page-sep` between the male and female pages. Every cell carries a jQuery-UI `.ui-state-default` drag handle. **All of the editing affordances require JavaScript, which the app's WebView has disabled.**

### `zero-day` → `#day-summary`

Three tables inside the fragment:

| Table | Columns | Rows |
|---|---|---|
| `#table-conf` | (label) · `Old` · `New` · `Total` · `Server` | `Confirmed Male`, `Confirmed Female`, `Total` |
| `#table-totals` | (label) · `Old` · `New` · `Total` · `Server` | `Attended Male`, `Attended Female`, `Total` |
| `#table-special` | (label) · `Chowky` · `Chair` · `Backrest` | `Male`, `Female`, `Total` — cells are strings like `1 (O) + 1 (N)` |

The fragment arrives with **no stylesheet**, which is why the in-app view is browser default. `Total` cells rely on an unclosed `<b>` tag.

Also on the same page and already consumed by the app: `#table-attending`, whose columns are `ConfNo · Name · Gender · Type · Age · Teen/10D/STP · LC · RoomNo · Laundry · Valuable · Chowky · Chair · BackRest · Group · H`. The app takes `a_id` plus room/seat/laundry/valuable/group only — never names, never the hidden comment column. The row's update action posts to `/app-update-attended/{id}` (the existing, separately-authorised allocation sync).

### `course/{cid}/{courseId}` → `.summary-block`

`<h2>Course Summary</h2>` then `.summary-block` → `.table-heading` (course identity, linked) → a table with headers `(label) · NM · OM · Total · SM · (spacer) · NF · OF · Total · SF`. Row labels are statuses (`Received`, `Confirmed`, `Cancelled`, `Clarification-Response`, `ReConfirmation`, `Expected`, …), each cell an anchor drilling into `/search-course/{cid}/{courseId}?s={status}&t={0|1}&g={M|F}&at={a|s}`. This is the same matrix the centre dashboard already paints natively — useful as the idiom for the new Course report table, not as a new fetch.

## Course report transport (already implemented)

`GET /centre/{cid}/course-report` returns a Drupal form, `dh_center_course_report_form`. The app scrapes `form_build_id`, `form_token` and the two date fields, then POSTs to the form's own action with `op = "Download Course Report"`; the reply streams `text/csv`.

Form fields: `report_from_date[date]`, `report_to_date[date]` (defaults last year → today), `form_build_id`, `form_token`, `form_id`, `op`.

CSV columns: `Course, NewMale, NewFemale, NewTotal, OldMale, OldFemale, OldTotal, StudentTotal, SevakMale, SevakFemale, SevakTotal, ConductingTeachers, AssistingTeachers, TrainingTeachers` — one row per course in range plus a grand-total row. `Course` is a single string (`{centre} / {type} / {year} / {dates}`); teacher names wrap onto continuation lines, so a parser must not assume one physical line per record.
