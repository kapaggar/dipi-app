# DIPI Staff 2.2 — desk pass handover

Self-contained. A designer or engineer with no prior context can work from this file alone.
Source of truth for the audit it answers: `../uploads/dipi-ui-audit-2.1.0-handover/` (PROMPT.md §1–§8, shots/).
Design artifact: `DIPI Staff 2.2.dc.html` (open in a browser; toggle LANDSCAPE / PORTRAIT top-left).

---

## 1. Product one-pager

DIPI Staff is the Android tablet app used at a meditation centre to run a course from
application intake through to close. Users are the **centre registrar** (owns the roll and
statuses), the **desk volunteer** (checks people in on arrival day), and the **centre manager**
(rooms, seating, printed sheets).

Operating conditions that drive every decision below:

- Arrival day is a queue under time pressure. One shared tablet, often held in portrait.
- Connectivity is unreliable; the desk must stay usable between syncs.
- The printed Day 0 / Course summary sheets are the institutional record. Screen figures that
  disagree with them cause arguments at the desk.
- Applicant health and ID data is sensitive and must not be stored on the device.

Three core flows: **course setup → section → room/time → publish**; **applications →
call → status decision → record**; **desk: scan/search → verify → check in → seat**.

---

## 2. What changed in 2.2 and why

Answers to the decisions taken on the 2.1.0 audit. Each is a behaviour change, not a restyle.

| Ref | Audit issue | Decision | Implementation |
|---|---|---|---|
| **A4** | Portrait at 900dp fell back to a phone tile hub; Room Chart unreachable | `deskWide` lowered **1024 → 840dp** | Portrait gets the identical rail-and-pane desk. Only list-detail differs: the list fills the pane, a tap pushes the card, a 48dp **← Back to list** row returns. No hub, no missing panes. |
| **A3** | On the last calendar day the Board still demanded an 82-person call round | Board switches to **reconcile** | `NEXT` becomes *Attendance check · Print Course summary · Print Day 0 summary*. Calling stays in the rail but is scoped to the 10 who never arrived. |
| **A1 / Q2 / Q6** | Left, and others, were counted as arrivals | Roll = **Confirmed + Expected** | Left, Rejected, Regret, Duplicate are off the arriving roll, off Calling, off Room Chart. Marked `◦` on their Applications chips with the rule stated in words. **WaitList stays on** — held, counted separately, so a freed room can be filled. |
| **A2 / Q3** | Persisted `Female · New` scope silently narrowed every count | Sticky scope is a **feature**, made legible | Named chip in the top bar with a 48dp clear; the bar always reads `showing 14 of 72`. No scoped figure ever appears bare. |
| **B1 / Q1** | `arriving today` disagreed with the Day 0 sheet | Show the app figure **with its derivation** | Every Board tile carries its formula: `72 = 82 − 10 Left`, `62 of 72`, `10 = 72 − 62`, `7 = 1 hard · 1 safety · 5 soft`. No glossary needed. |
| **B1 (rail)** | Rail counts mixed scoped and unscoped totals | One kind of number | All rail counts are **work left, unscoped**: 173 applications, 7 findings, 10 to reconcile, 10 still to arrive, 24 rooms free. |
| **A5** | Zero Day "Mark attended" is local-only | **Kept as-is** by owner decision | No change. Still needs a server write or an explicit local-only label — carried forward as open. |
| **B4 / C1** | 46dp targets, 12sp captions, low contrast on night Steel | 48dp / 14sp floor | Rail rows, chips, toggles, the scope clear, icon buttons all 48dp. No text below 14sp. Aadhaar masks by default behind an explicit REVEAL, in memory only. |

---

## 3. Screens in the build

Six panes, identical in both orientations.

1. **0 Day Board** — four derived tiles, last-day reconcile block, nine sheets/exports.
2. **Applications** — status chips with off-roll markers, list-detail, ID verification block
   (masked), health block, audit state, CHANGE STATUS sheet.
3. **Audit** — 7 findings grouped hard / safety / soft, detail pane on landscape.
4. **Calling** — reconcile round, outcome chips, WhatsApp batch, per-person state.
5. **Check-in** — scan field, to-arrive / arrived / all, progress against the scoped roll,
   an explicit *not on this list* block, and roll / rooms / seating sidebar.
6. **Room Chart** — occupancy legend, old/new/available, pull-from-server.

Status vocabulary comes from the desk worklist select. **There is no Approve and no defer** —
do not introduce them.

---

## 4. Tokens observed and used

Dark / Steel night.

```
ink          #E4E6E9   primary text
ink-dim      #C3C8CD   rail idle label
muted        #9BA1A8   captions, meta, formulas   (14sp floor)
faint        #6C737A   room numbers, empty
accent       #5980A6   Steel — selection, CTA, links
accent-lift  #9EC1E0   numerals on accent ground
accent-bg    #1D2D3D   selected row / scope chip ground
warn         #DBCBA6   safety, WaitList, held
alert        #E0796F   must-fix, never-arrived
surface-0    #0B0D0F   desk
surface-1    #14171A   pane
surface-2    #1A1E22   card, rail
surface-3    #22272C / #171A1D   avatar, empty room
line         #2E3339   hairline
line-strong  #3A4046   control border
```

Type: **Barlow Condensed** 600/700 headings and numerals · **Barlow** 400/500 body 15sp,
caption 14sp · **IBM Plex Mono** 400/500 for codes, counts, formulas, kickers.
Radii 5 control / 6 tile / 8 card / 12 sheet. Targets 48dp. Frames 1280×900 and 900×1280.

**Raw hex above still needs tokenising** before build — the values are canonical, the names
are proposals.

---

## 5. Must not change

- Status vocabulary and its source (desk worklist select).
- Printed sheets are the institutional record; screen figures reconcile to them, not vice versa.
- ID and health data stay in memory. Never persisted, never logged, never in a screenshot path.
- WaitList is held, not rejected — it must remain fillable.
- Android system back, edge-to-edge, Material 3. No iOS patterns.

---

## 6. Open — needs product owner

1. **A5** "Mark attended" remains local-only by decision. Server write, or label it local?
2. **Cancelled (71)** is off the arriving roll as a consequence of `Confirmed + Expected`,
   but was not explicitly confirmed in the status exclusion list. Confirm.
3. Day 0 sheet shows `67 → 57`, the app shows `72 → 62`. Both derivations are now visible;
   which is the figure staff read aloud?
4. Is the sticky scope per-user or per-device? A shared tablet makes this a real difference.
5. Room Chart `PULL FROM SERVER` — what happens to local seating edits on pull?

---

## 7. Prompt for the next agent

> You are continuing DIPI Staff, an Android tablet app for centre-registrar desk operations.
> Read `HANDOVER.md` in full before touching anything: it carries the product one-pager, the
> 2.2 behaviour decisions with their audit refs, the six-pane screen list, the Steel night
> tokens, the locked constraints, and five open questions.
>
> The artifact is `DIPI Staff 2.2.dc.html`. It is a single Design Component, inline styles only,
> with a LANDSCAPE / PORTRAIT toggle and two tweak props (`lastDayMode`, `showOffRollMarkers`).
>
> Rules: 48dp targets and a 14sp text floor are non-negotiable. Never show a scoped count bare —
> always "14 of 72". Every derived number carries its formula. Left, Rejected, Regret and
> Duplicate are off the arriving roll, Calling and Room Chart; WaitList is on and held. Do not
> add Approve or defer to the status set. ID and health data stay in memory.
>
> Do not restyle what you were not asked to change. Any change to *intent* rather than
> *presentation* needs human sign-off — flag it, do not apply it silently. Answer the §6 open
> questions with the product owner before designing anything that depends on them.
