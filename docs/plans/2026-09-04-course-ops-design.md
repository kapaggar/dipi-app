# Course ops design advise — inventory + implement slice

> Source zip: `/Users/wizops/Downloads/claude design output.zip` (space in
> filename), unpacked to `/tmp/claude-design-output/`. Visual authority for
> this mode is **`DIPI Course ops v6.dc.html`** (frames C1–C10 + CQ).
> Precedent handover (what Design was asked): `uploads/dipi-course-ops-handover/`
> in the same zip, plus `~/Downloads/course-ops-stroll/`. Live shots are
> 1.36.1 Pixel C Course ops and are **not** committed.
>
> Branch: `feat/course-ops-design` from `main` @ `370e0d8`.
> SemVer: **MINOR 1.37.0 / versionCode 60** (tablet-facing Course ops chrome).

**Locks that win over any frame:** PHP immutable; no `/staff/*`; no
`POST /search-app`; never `?r=`; never `Approved`; Course ops is a device
mode; PIN store unchanged; two GETs only (`/teacher-list` once per entry,
`/application-view` allowlist); no NPI persist/log; seating-r2 (teacher at
the bottom, letters as columns, CHOWKY / CHAIR, **66 dp cells**, no drag,
sevaks hidden); no desk rail; no writes. Tests only in `:app` /
`:core:{model,network,datastore,audit}`.

---

## 1 · Zip inventory

102 files. Three HTML design canvases at the root, plus three handoff
folders and the original handover/shot packs.

| Path | What it is |
|---|---|
| `DIPI Course ops v6.dc.html` | **This pass.** Critique + frames C1–C10 + CQ. Drawn in Steel. |
| `DIPI Sheets v5.dc.html` | Sheets v5 (already shipped). Out of scope. |
| `DIPI Staff v4.dc.html` | Desk v4 + original Course ops 2a–2d. Out of scope as a rewrite. |
| `support.js` / `assets/lotus.png` / `.thumbnail` | Canvas runtime + mark. |
| `design_handoff_dipi_course_ops/` | **Older** Course ops handoff (v4 frames 2a–2d, already shipped). README + PROMPT + Staff v4 HTML. |
| `design_handoff_dipi_v4/` | Desk v4 handoff + a few UI-export shots. |
| `design_handoff_dipi_v5/` | Sheets v5 IMPLEMENT/HAR/BRIEF. |
| `uploads/dipi-course-ops-handover/` | What Design was asked: `CLAUDE-DESIGN.md`, `CONSTRAINTS.md`, `README.md`, shots `01`–`17` + `INDEX.md`. |
| `uploads/dipi-sheets-v5-handover/` | Sheets v5 handover (desk). |
| `uploads/dipi-ui-export/` | Desk/login Pixel shots + SHIPPED-DELTA. |
| `uploads/DIPI Staff centre-registrar desk/` | Duplicate of the v4 desk pack. |
| `uploads/pasted-1787970930129-0.png` | One pasted reference image. |

Names and photos in the after-frames are **substitutes**. The `shots/` PNGs
carry a live roster — stay out of git.

### v6 frames (visual authority for Course ops)

| Frame | Subject |
|---|---|
| C1 | Teacher list default — SEAT/FLAGS gutter, clamped sub-line, roll count, FLAGS order |
| C2 | One group selected — COURSES collapse + **filter-empty** + Clear filter |
| C3 | Male hall — rail tint = old/new, CW/CH sub-labels, seated tally grammar |
| C4 | Female hall — grown/centred cells + UNSEATED band |
| C5 | Student card — NO 56 dp / YES full card, summary line, named back |
| C6 | Same card from a seat — `‹ Seating plan` + CAME FROM footer |
| C7 | Prefetch strip (never captured) + miss/retry + tap-too-early |
| C8 | PIN gate — four cells, “not the account password” |
| C9 | Offline strip + cache age; roll-error / empty-host bodies |
| C10 | Settings — PIN-row copy, two extra consequence rows, Erase-all copy |
| CQ | Nine owner questions. Design did **not** add search or a dark hall. |

---

## 2 · Map — implement / defer / reject

| Item | Source | Verdict | Why |
|---|---|---|---|
| SEAT 76 + FLAGS 150 + 16 dp gutter | C1 defect 3 | **Implement now** | Layout only; no transport. |
| Sub-line one ellipsized line | C1 | **Implement now** | Already `maxLines = 1`; keep / confirm. |
| SEAT as locator (no accent) | C1 | **Implement now** | Token only. |
| FLAGS order `HLTH MED INTOX TECH PREG MONK`; HLTH heavier hairline | C1 | **Implement now** | Order already locked in `FLAG_ORDER`; border weight is visual. |
| Header `· N on the roll` | C1 | **Implement now** | Count of the fetched roll. |
| COURSES collapses when the **rendered** set has no chips | C2 refine 4 | **Implement now** | Data consequence, not a toggle; column returns when any chip exists. |
| Collapse foot-line | C2 | **Implement now** | So the column set never changes silently. |
| Selected GROUP pill = accent fill; `Clear filter ×` | C2 | **Implement now** | Existing local filter. |
| Filter-empty body | C2 | **Implement now** | Empty state for a zero-row group; no fetch. |
| Rail cells follow hall old/new tint | C3 defect 1 | **Implement now** | Legend promise; r2 intact. |
| `CW · CHOWKY` / `CH · CHAIR` sub-labels | C3 defect 2 | **Implement now** | Same rail, occupied-only. |
| Rail in a hairline card | C3 | **Implement now** | “In the rail” is the container, not the fill. |
| Empty CHAIR run → `None in this hall` | C4 | **Implement now** | No phantom `CH-` slots. |
| Empty-cell seat code `neutral400` | C3 | **Implement now** | Token. |
| Hall pills carry **seated** counts | C3/C4 | **Implement now** | Additive. Header keeps `old, new` (CQ5). |
| UNSEATED band polish (kicker + count + 38 dp rows) | C4 | **Implement now** | Band already exists; reasons stay the page’s (`roleTag` / `—`). Sevaks stay hidden. |
| NO row 56 dp; YES full card; 34 dp summary | C5 refine 6 | **Implement now** | Every Health row still shows; no severity colour. |
| Back pill names the door | C5/C6 | **Implement now** | `‹ Teacher list` / `‹ Seating plan`. |
| Placement `· N of M in this group` | C5 | **Implement now** | Walk already group-scoped. |
| `in his/her own words` | C6 | **Implement now** | From the group’s gender, already on the roll. |
| CAME FROM footer | C6 / CQ9 | **Implement now** | Navigation fact, not a new surface. Owner may cut later. |
| Prefetch strip → 38 dp, `◍ Pulling applications… N of M` | C7 | **Implement now** | Existing pull; same slot as offline; never both. |
| Pending FLAGS bar while a card has not landed | C7 | **Implement now** | Distinguishes “not known yet” from “nothing written”. |
| PIN four cells + “not the account password” | C8 | **Implement now** | Visual of the existing gate. PIN store / hash unchanged. |
| Offline strip cache age + “this mode never writes” | C9 | **Implement now** | Session `HH:mm` from the existing sheet clock. |
| Roll-error / empty-host bodies (visual) | C9 | **Implement now** | Existing `CourseOpsRollError` / pending; server text verbatim. **No** auto-retry of `/teacher-list`. |
| Settings PIN-row copy | C10 | **Implement now** | “Settings and logout need the device PIN”. |
| Consequence rows `Nothing writes` + `Health answers` | C10 | **Implement now** | Copy only. |
| Erase-all enumerates PIN + mode key | C10 | **Implement now** | Copy only; wipe behaviour unchanged. |
| Small-hall cells 78 dp / 660 dp centred block | C4 / CQ6 | **Defer** | Fights the r2 **66 dp** lock and “do not invent a second hall geometry”. |
| Header tally `N seated · N not in the hall` *instead of* old/new | C3 / CQ5 | **Defer** | Owner question. Keep `N old, N new`. |
| UNSEATED invented reasons (`SEVAK · NO SEAT LABEL`, leftover-label quotes) | C4 / CQ2 | **Defer** | Do not invent strings the desk did not emit. |
| Name search (even client-side) | CQ1 | **Defer** | Design did not draw it; user: no search unless already present. |
| Prefetch RETRY / `FETCH THIS ONE NOW` | C7 / CQ3 | **Defer** | New control; misses already retry on next entry. |
| Dark / 4am hall | CQ4 | **Defer** | Never drawn; Steel night inherit is mechanical later. |
| `MED` discrimination / drop the flag | CQ7 | **Defer** | Derivation is a lock. |
| Exit-confirm copy | CQ8 | **Defer** | No frame; product string. |
| Growing chrome: third destination, course picker, desk rail | v6 “did not touch” | **Reject** | Hard rules + assumption 16. |
| `POST /search-app`, new endpoints, `/staff/*` | locks | **Reject** | Hard rules. |
| Writes, attendance, seating editor, `?r=`, `Approved` | locks | **Reject** | Hard rules. |
| Poll `/teacher-list`; parse Comments / extra card sections | assumption 16 | **Reject** | Mutates server; NPI. |
| Flip teacher to the top; rename rail to CELL / PAGODA | r2 | **Reject** | Seating locks. |
| Change the PIN store | user | **Reject** | Encrypted `dipi_course_ops` PIN keys stay. |
| Sheets v5 / Desk v4 rewrite | other canvases | **Reject** | Different product surface. |

---

## 3 · Task slice (this branch)

1. Teacher list C1/C2 — columns, pills, collapse, filter-empty, roll count, prefetch/offline strips.
2. Hall C3 (+ C4 polish that keeps 66 dp) — rail tint + sub-labels + UNSEATED chrome + seated pill counts.
3. Student card C5/C6 — compressed NO rows, summary, named back, walk position, CAME FROM.
4. PIN C8 + Settings C10 copy.
5. Empty-host / roll-error C9 visual; no silent `/teacher-list` retry.
6. Tests in `:app` / `:core:model`. Suite. SemVer 1.37.0 / 60. DESIGN.md shipped-delta.
7. Assemble debug; Pixel C smoke if ADB is up (shots out of git).

Do not commit unless asked.

---

## 4 · PATCH 1.37.1 / 61 (2026-09-05)

Folded onto this branch after the v6 visual pass. Owner requests (not a
new Course ops canvas):

1. Rail label **0 Day Board** (enum id unchanged).
2. Chowky/chair **single row** default (`ChowkyRailLayout`); CW-A1 first,
   then chairs. Wrap stays configurable on the hall chart.
3. Male/Female Board PDFs removed — no `course-pdf-m|f` from the app.
4. Student chit print **5e 12-up**; checking slip **5g 2-up**.
5. Native hall keeps 5h dead-control removal; teacher-at-bottom lock holds.
6. Course ops destination **Teacher list** (not Seniority). `/teacher-list` GET unchanged.
