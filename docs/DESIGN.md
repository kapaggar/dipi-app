# Design — source of truth

The binding design reference is [`design/DIPI-Staff.dc.html`](design/DIPI-Staff.dc.html)
(open in a browser; measurements are 1px = 1dp). It wins every visual argument
(AGENTS.md hard rule 9). This file carries its measurement spec and, below, the
shipped-delta ledger every design pass must honour.

## Origin: v4 desk pass (delivered on 1.22.0)

## Overview

A design pass on the DIPI Staff centre-registrar desk: a native Android tablet app used by Vipassana centre staff to verify applicants, call them, check them in and pull rooms. One account → one centre → upcoming courses → a six-section course desk (Board, Applications, Audit, Calling, Check-in, Rooms & seats).

This pass fixes a clipped fold, an unreachable keyboard-up login, a persisted-scan bug, a confusing dark mode, a left-weighted settings page and a loose Board grid. It adds **no screens and no features**.

## About the design files

`design/DIPI-Staff.dc.html` is a **design reference created in HTML** — a prototype of intended look and measurements, not production code. The target is the existing **Jetpack Compose / Kotlin** app. Recreate the frames there using the existing `Skin.kt` tokens, existing composables and existing navigation. Do not port HTML, CSS or the file's canvas chrome.

Open the file in a browser. Each option is a `{turn}{letter}` id (`1a`…`1i`) with the 1.22.0 screenshot on the left and the proposed frame on the right, plus notes underneath.

**Scale:** every frame is 1280×900 CSS px = the Pixel C's 2560×1800 px at density 2 → **1 px in the file = 1 dp in Compose, and font px = sp**. Read sizes directly off the file.

## Fidelity

**High-fidelity.** Colours, type, spacing and dp measurements are final and were checked at 1:1. Recreate pixel-accurately using the app's existing token ramps. The one exception: the Android status bar, nav bar and on-screen keyboard drawn in the frames are *stand-ins for the platform chrome* — the system draws those.

---

## Screens / views

### 1a — Centre dashboard (Steel) · before `21-centre-steel.png`

**Purpose:** the registrar's home. Pick an upcoming course, reach an older one, or reach the three centre-level destinations.

**Layout** — root column inside the 1280×900 window; status bar 24dp, nav bar 48dp, content 828dp, horizontal padding 24dp.

| Band | Height | Notes |
|---|---|---|
| Header | 52dp | `{centre} · {displayName}`, Barlow Condensed 600 23sp, letter-spacing 0.2 |
| Upcoming pane | `max-height 460dp` (60% ceiling, `fill = false`) | own scroll |
| Lower pane | remainder, 12dp top padding | **two columns**, 18dp gap |

**Upcoming pane:** "Upcoming courses" Roboto 400 16sp `neutral600`, 10dp bottom margin. 2-column grid, 12dp gap, `align-content: start`.

**Matrix card:** `surface #FAFAFB`, 1dp border `#DEDEE1`, radius 8dp, padding 11/14/9dp, shadow `0 1 2 rgba(0,0,0,.05)`.
- Title: Barlow Condensed 600 17sp, line-height 1.15.
- **Next-course marker:** on the soonest course only — a 3dp accent bar, `left 0`, `top/bottom 8dp`, radius `0 3 3 0`, colour `accent`. This is the page's only chromatic mark.
- Column geometry: label column flexes; six numeric columns of **54dp**, right-aligned. Male trio = columns 1–3, female trio = 4–6.
- Group caps row: "MALE" and "FEMALE", each centred over 162dp, IBM Plex Mono 500 9sp, letter-spacing 1.7, `neutral500`.
- Label row: `NM OM M NF OF F`, IBM Plex Mono 600 12sp. NM/OM/NF/OF `neutral600 #7A7A7D`; **M and F `#2B2B2D`**. 4dp top margin, 5dp bottom padding, 1dp bottom border `#E0E0E3`.
- **Subtotal bands:** two rects behind the rows, `top 15dp → bottom`, width 54dp, at `right: 0` and `right: 162dp`, fill `#EDEDF1`, radius 3dp.
- **Gutter:** 1dp `#D4D4D7` at `right: 161dp`, same vertical span.
- Data rows: 26dp tall. Key Roboto 400 13.5sp `neutral700`; cells IBM Plex Mono 400 14.5sp `#424244`. Empty = `·`.
- Total row: 30dp, 1dp top border `#E0E0E3`. "Total" Roboto 500 14sp; sevak suffix IBM Plex Mono 400 11.5sp `neutral500`; cells IBM Plex Mono 500 15sp `text`.

**Lower pane, left column (flex):** "Older courses" kicker; rows 42dp, `#FAFAFB` on 1dp `#DEDEE1`, radius 6dp, padding 0 14dp, Barlow Condensed 600 16sp + a `›` chevron `neutral400`. 6dp gap. Own scroll.

**Lower pane, right column (416dp fixed, no scroll):** "Centre desk" kicker; three tiles 48dp, **transparent fill**, 1dp `#D4D4D7`, radius 6dp, Barlow Condensed 600 16sp + `›`. 6dp gap. Then 14dp margin, 11dp padding above a 1dp `#E0E0E3` rule, kicker `MORE ON THE DESK SITE` (mono 500 9sp / ls 1.7 / `neutral500`), then the two desk-site links as 30dp pill chips — 1dp `#E0E0E3`, radius 15dp, Roboto 12.5sp `neutral700`, trailing `↗` `neutral400`, 6dp gap, wrapping.

**Empty older-courses (see 1g):** heading omitted (as today) and the desk column takes the **full width, three tiles across at 52dp**.

**2026-08-30:** Manage Courses, Daily Activity and SMS Report are gone from the desk-site chip row — owner instruction; still reachable on the desk site. The chip row is now two links (Course Report, Bulk Mail), not five. **2026-09-02 (v5 T3):** Course report became a native tile, so the chip row is one link (Bulk Mail) and the native grid is 2 × 2. See the do-not-re-propose list in the Shipped delta ledger below.

### 1b — Login, keyboard up · before `20-login-clean.png`

Card **380dp wide**, `#FAFAFB` on 1dp `#DEDEE1`, radius 10dp, shadow `0 2 8 rgba(0,0,0,.07)`, padding 18/20/20dp, **~324dp tall**, centred in the 472dp band between the status bar and the IME.

Top-to-bottom: brand row (34dp lotus + "DIPI Staff" Barlow Condensed 700 21sp + "Centre admin desk" Roboto 11.5sp `neutral600`) → error strip → USERNAME → PASSWORD → action row.

- **Error strip:** background `#FBEFEE`, 1dp `#E8CDC9`, 3dp left bar `#A33A34`, radius 6dp, padding 8/10dp. Title Roboto 500 12.5sp `#A33A34`; body Roboto 400 12sp `#7A5450`, **verbatim server text**.
- Field labels: IBM Plex Mono 500 9.5sp, ls 1.5, `neutral600`.
- Fields 40dp, radius 6dp. Idle `#F5F5F8` on 1dp `#D4D4D7`. Focused `#FFF` on **2dp `accent`** with a 2×18dp accent caret.
- Action row 44dp: checkbox 19dp radius 3dp filled `accent` with a white tick + "Remember me" Roboto 13.5sp on the left; **SIGN IN** 148×44dp, `accent`, radius 6dp, Barlow Condensed 600 14sp, ls 2.2, white.
- **Collapses only while the IME is up:** brand block to one row, "Your centre is read from your account after sign-in." hidden, Remember-me moved onto the button row. Restore the tall card when the IME hides.

Background: lotus relief only (vector mark, opacity 0.09, `saturate(.45) hue-rotate(165deg)` in Steel). **No photo hero, never full-bleed.**

### 1c — Check-in · before `16-desk-checkin.png`

**The bug:** the conf field still holds `NF24` from an earlier session, so the roster opens silently filtered.

- **Scan buffer is session-scoped.** Check-in always opens empty, on the full roster.
- Field 52dp, `#FAFAFB` on 1dp `#D4D4D7`, radius 6dp, padding 0 14dp, 15dp scan glyph, placeholder "Scan a chit or type a conf number" Roboto 15sp `neutral500`.
- **Clear control:** 48×48dp touch target holding a 32dp circle `#E7E7EA` with `✕` Roboto 15sp `neutral700`. Appears only when the field has content; clearing restores the full roster.
- Focused field: 2dp `accent`, value in IBM Plex Mono 15sp.
- Optional **state 3** (only if a restored query is genuinely wanted): show it — a 30dp `accent100` strip under the field, "Kept from the last session" `accent800` + `CLEAR` Barlow Condensed 600 11sp / ls 1.4 / `accent700`.
- Segmented controls keep shipped sizing; the primary row is 52dp to match the field.
- Rooms-free panel: label and count are **separate columns** so "Male · Mbk + Guest block" stops wrapping mid-label. Count IBM Plex Mono 500 14sp `accent700`.

Rail (all desk screens): 190dp, `#EFEFF0`, 1dp right border `#E0E0E3`. Lotus 54dp at 18dp left. Kicker `DESK`. Items 46dp, Roboto 15.5sp, counts IBM Plex Mono 13sp `neutral500`. **Selected item:** `accent100` fill, 3dp left `accent`, label Roboto 500 `accent800`, count `accent700`. Footer `sudha.user` / `synced just now`, IBM Plex Mono 12sp. Faint lotus watermark bottom-left.

### 1d — Settings, light · before `07-settings-steel-light.png`

Two columns: left flexes, right is **428dp**, 18dp gap. Cards `#FAFAFB` on 1dp `#DEDEE1`, radius 8dp, padding 15/18/17dp.

Left, **APPEARANCE**: Theme row 48dp with a 38dp segmented `Light | Dark` (selected = `accent` fill, white); 1dp `#E7E7EA` rule; `SKIN` kicker; five 40dp chips (15dp gradient swatch + Barlow Condensed 600 13.5sp / ls 1.3; selected = `accent` fill + `accent` border + white); the existing sentence; two ramp strips (`ACCENT 100–900`, `NEUTRAL 100–900`) of nine 26×18dp swatches, 3dp gap; rule; Lotus watermark row 48dp with a 46×26dp Material switch; "Status colours stay put; they carry meaning, not mood."

Left, **TESTING**: "Simulate offline" + switch, and one caption line. (Was a text link reading like a label.)

Right, **ACCOUNT & SESSION**: `sudha.user` Barlow Condensed 600 20sp, centre name Roboto 14sp `neutral600`; three 34dp rows (Last synced / Queue / App version) with IBM Plex Mono 12.5sp values; **Log out** 44dp pill, `accent`, padding 0 30dp; rule; **Erase all local data** Roboto 500 15sp `#A33A34` + the existing description.

### 1e — Settings, dark · before `10-settings-dark-steel.png`

**Decision: option B — dark is the Steel night ramp, and the UI says so.** Rationale: five night ladders each need their own contrast pass against fixed status hexes, for a screen used at a lit reception desk.

- Callout under the Theme row: `accent900 #1D2D3D` ground, radius 6dp, padding 11/13dp. Line 1 Roboto 500 13.5sp `#B5D9FD`: "Dark runs the Steel night ramp." Line 2 Roboto 13sp `#9BA1A8`: Blossom is remembered and returns in Light.
- Kicker becomes `SKIN` + `APPLIES IN LIGHT` (mono 9.5sp `#5E666D`).
- Chips render on night neutrals; the **selected** chip keeps its true-colour swatch, an `accent` border, `#1D2D3D` fill, `#B5D9FD` label and a `SAVED` tag (mono 9sp `#749DC4`).
- Ramp strips relabel to `NIGHT ACCENT` / `NIGHT NEUTRALS`.

**Three bugs fixed from shot 10:** the "Settings" heading was near-invisible (now `#E4E6E9`), the offline strip was still Blossom pink (now `#22272C` / `#C3C9D0`), and Erase-all kept a light-mode red (now the dark half of the fixed severity pair).

### 1f — Board · before `12-desk-board.png`

- Roll sentence unchanged, Roboto 15sp `neutral700`.
- Stat cards **250 → 100dp**: number Barlow Condensed 700 38sp `accent800`; kicker IBM Plex Mono 500 10sp ls 1.6 `neutral700`; sub Roboto 12.5sp `neutral500`. 12dp gaps, four across.
- **NEXT rows 158 → 58dp**: title Barlow Condensed 600 18sp with a Roboto 12.5sp sub beneath, `→` `accent400` right. 7dp gaps. This alone buys ~300dp.
- **SHEETS & EXPORTS**: all twelve names, unchanged, grouped on three shelves — `ROLL SHEETS` (Day 0 list, Day 0 summary, Male PDF, Female PDF) · `DESK SLIPS` (Student chit, Checking slip, Seating plan, Laundry list) · `FOR THE TEAM` (Teacher list, Manager list, Valuable list, Course report). Chips 40dp, four per row, 8dp gap, 1dp `#E0E0E3`, radius 6dp, leading `↓` `accent400`, label Roboto 13.5sp.
- **Day 11 · Course summary report** is drawn as a 40dp **dashed** row (`1dp dashed #C6C6CA`) with a `GAP — NOT IN 1.22.0` badge. **Do not implement it.**
- Everything now lands on one fold with no scroll.

Applications and Rooms stay **out of NEXT**: their counts are inventory, not a queue.

### 1g — Sync strips and empty older-courses

- **Offline strip** (unchanged copy): 38dp, `#E7E7EA` on 1dp `#DEDEE1`, `◍` + "Offline — showing cached list" Roboto 14sp `neutral700`.
- **Queued strip**, stacked *below* it: **56dp**, `accent100 #EEF6FF`, 1dp bottom `accent200 #D6EBFF`. Text "N change(s) waiting to sync" Roboto 14sp `accent800`; "last try HH:MM" IBM Plex Mono 12.5sp `accent700`; **RETRY** button 48dp tall, padding 0 22dp, 1dp `accent400`, radius 5dp, Barlow Condensed 600 13.5sp ls 1.8 `accent800`. Deeper than the offline strip because it is the only one you can tap.
- Both strips **push content down**; they never float over it.

### 1h — Centre dashboard, Blossom · before `11-centre-blossom.png`

Identical geometry. Only the OKLCH ladder moves (hue 352, chroma 0.095) and the lotus wash goes to opacity 0.17 with no filter. Proof that no frame knows which skin it runs.

---

## Interactions & behaviour

- **Centre:** upcoming and older lists scroll independently; the desk column does not scroll. Tapping a course opens the desk at Board. Tiles 1–3 are in-app; the two chips open the desk site externally.
- **Login:** IME visibility drives the compact/tall card. The error strip appears on a failed POST and holds the server's verbatim message; it does not clear until the next submit.
- **Check-in:** the scan buffer lives for the desk session only. `✕` clears the field and the roster filter in one action.
- **Settings:** Theme is a two-way segmented control; skin chips apply instantly in Light and are stored (not applied) in Dark; the lotus switch gates the watermark everywhere.
- **Sync:** offline strip on connectivity loss; queued strip whenever `queue > 0`, independently of connectivity; RETRY re-drains the outbox and updates "last try".
- Touch targets ≥ 48dp throughout. Pointer/hover is a bonus, not a theme.

## State

No new state beyond what 1.22.0 has, with two exceptions:
1. `scanQuery` moves from persisted to **session-scoped** (or gains an explicit clear-on-enter).
2. The queued strip needs `outboxCount` and `lastSyncAttemptAt` surfaced to the centre and desk scaffolds.

Device-local DataStore (skin, lotus, room-layout columns) is unchanged and still wiped by Erase-all.

## Design tokens

**Steel (default, as shipped)**

| Token | Hex |
|---|---|
| bg | `#F2F2F3` |
| surface | `#E9E9EA` (cards in the frames sit at `#FAFAFB`, hairline `#DEDEE1`) |
| text | `#1D1F20` |
| neutral 100–900 | `#F5F5F8` `#E7E7EA` `#D4D4D7` `#B7B7BA` `#98989B` `#7A7A7D` `#5D5D60` `#424244` `#2B2B2D` |
| accent | `#5980A6` |
| accent 100–900 | `#EEF6FF` `#D6EBFF` `#B5D9FD` `#94BCE3` `#749DC4` `#597EA3` `#416180` `#2C455D` `#1D2D3D` |

**Steel night (dark, all skins)** — `#14171A` `#1A1E22` `#22272C` `#2E3339` `#3A4046` `#4A5157` `#6B7278` `#9BA1A8` `#E4E6E9`; accent unchanged at `#5980A6`, tint `#1D2D3D`, accent text `#B5D9FD`.

**Other skins** — OKLCH ladder unchanged; see the Shipped delta ledger below. Blossom = hue 352 / chroma 0.095 / mark opacity 0.17 / no filter.

**Fixed severity (never follows the skin)** — danger `#A33A34` light / `#E0796F` dark. Status colours likewise stay put.

**Accent discipline** — accent means one thing: live, occupied, or selected. `accent100` selected fills and hovers · `accent` active bars, ticks, solid primaries · `accent600` pressed · `accent700–800` text on tints and numerals. Everything else is a hairline on the neutral ramp.

**Type** — Barlow Condensed (titles, crumbs, kickers, buttons) · IBM Plex Mono (matrix digits, counts, kickers, timestamps) · Roboto (body and controls). Sizes are listed per component above; nothing below 9sp, and no body text below 12.5sp.

**Spacing** — 6 / 8 / 12 / 14 / 18 / 24dp. Radius 5dp (segments) · 6dp (fields, tiles, chips) · 8dp (cards) · 10dp (login card) · pill for Log out. Elevation: cards `0 1 2 rgba(0,0,0,.05)`; login card `0 2 8 rgba(0,0,0,.07)`; **desk tiles stay at 0**.

## Assets

- `assets/lotus.png` — the lotus mark, cropped from the app's own sign-in capture (`20-login-clean.png`). Use the app's existing vector drawable; this PNG is a stand-in for the HTML only. **It is a vector mark, never a photograph.**
- The seven 1.22.0 "before" captures live in git history (pre-consolidation `version-4/uploads/`).
- Fonts load from Google Fonts in the HTML; the app already ships both families.

## Files

| File | What it is |
|---|---|
| `PROMPT.md` | Paste-ready brief for Claude Code |
| `design/DIPI-Staff.dc.html` | The design reference — open in a browser |
| `support.js` | Runtime the HTML file needs; keep it beside the HTML |
| `assets/lotus.png` | Lotus mark used by the frames |
| Shipped delta ledger (below) | What shipped after v3 — the do-not-re-propose list and the full token tables |

## Do not

Reintroduce lotus photographs · one-line course counts · a single centre scroll instead of 60/40 · raised desk tiles · "from your account" · the older-courses chrome line · a Board heading repeating the centre name · ON/OFF text instead of switches · Room chart as a small link · App Settings hidden in overflow. All were removed on purpose; `SHIPPED-DELTA.md` has the full list.

---

## Shipped delta ledger — live app vs the retired prototypes

Compared: the Pixel C build **1.30.3** (`versionCode` 49) against `version-3/project/DIPI Staff.dc.html` and `version-3/DELTA.md`. The 1.22.0 shot set next to this file remains the visual archive; use the tree, not that header date, as current truth.

The 1.22.0 capture set lives in git history (pre-consolidation `version-4/uploads/dipi-ui-export/`). Use the tree, not any canvas, as ground truth.

The prototype still describes an earlier desk. Three owner rounds after v3 landed in the app and **must not be re-proposed**.

---

### Do not re-propose (already shipped, already accepted)

| Drift | What the v3 canvas still shows | What 1.22.0 actually does | Shot |
|---|---|---|---|
| Status matrix cards | Course cards with a one-line counts string (`Confirmed 58 · Expected 15 \| …`) | Full desk matrix: columns `NM OM M NF OF F`, rows Confirmed / Cancelled / Total +sevak, IBM Plex Mono | `02`, `11` |
| 60/40 centre split | Single scrolling phone/tablet page | Wide: upcoming `weight(0.6, fill=false)` + older+tiles `weight(0.4)` as two independent scrolls; header capped | `02` |
| Blended desk tiles | Raised cards competing with courses | Three transparent-fill, 0-elevation tiles: Centre Settings, Advanced Search, App Settings. Other desk-site tiles still exist but recede | `02`, `03` |
| Removed headings | `"… from your account …"`; older-courses sub-line `Teacher list · valuables · seating — check-in is closed`; Board 40sp centre-name heading; crumb `label · dates` duplicating the centre | Header is `{centre} · {displayName}`; no older-courses chrome line; Board starts at the roll sentence; crumb is `dates · dayChip` at 17sp | `02`, `12` |
| Real switches | ON/OFF kickers | Material 3 `Switch` on Laundry / Valuables / Groups; RESULT card still derived | `04` |
| Room chart first + editable | Small TextButton after RESULT; fixed 4-across grid | Full-width card at top of Centre settings; chart has `− N +` columns per gender+section (1–12, default 4) | `04`, `05` |
| Skin photos gone | `--sk-photo` lotus photographs on sign-in (430px hero) and as skin ground | Photos deleted at 1.15.0. Login is a centred 380dp card over `LoginLotusRelief` (vector mark + fade), not a photo. Lotus watermark is the circular mark, gated by the Settings switch | `01` |
| Compact login | Bottom-justified full-bleed form + photo hero | Owner 2026-08-16: compact card, remember-me, verbatim server error | `01` |
| Dark pinned to steel | `THEME.dark` untouched | Dark still uses steel tokens even if a non-steel chip is selected (see `10`) | `10` |
| Sync banners split | One strip that said “offline” while online if anything was queued | Two strips: offline (`◍ Offline — showing cached list`) then queued (`N change(s) waiting to sync` + RETRY). Desk rail already distinguished | `09`, `11` |
| App Settings is a tile | Settings only from overflow | Native tile on the centre desk row | `02` |

### 2026-08-30 — three desk destinations retired

| Destination | Reason |
|---|---|
| Manage Courses | removed from the app on owner instruction; still reachable on the desk site |
| Daily Activity | removed from the app on owner instruction; still reachable on the desk site |
| SMS Report | removed from the app on owner instruction; still reachable on the desk site |
| Letters | removed from the app on owner instruction; still reachable on the desk site |
| CentreEditScreen | do not port the Drupal `GET /centre/{cid}/edit` scrape; centre settings are `CentreOpsScreen` |

### Post-v4 owner decisions (2026-08-30 / 2026-08-31)

Do not re-propose:

| Decision | What the v4 canvas still shows | What shipped |
|---|---|---|
| Centre lower pane | 416dp two-column (older \| desk) | Older courses on the upcoming grid; desk column stacked beneath (`centre-trim-spec.md` §S4) |
| Upcoming ceiling | 460dp / 60% + independent pane scrolls | Fixed header + one below-header scroll (card-bloat fix, `4ba1b3b`) |
| Matrix cards | separate Confirmed and Expected rows | Fixed `cardRows` with `Confirmed + Expected` summed (§S3) |
| Day-11 Board chip | dashed GAP row, "do not implement" | Solid full-width fourth-line chip under the 3×4 shelves (T1, 2026-08-31) |
| Status vocabulary | (undescribed) | Desk `edit-app-status` select, roster fallback (T3) |
| v4 drift polish | (the six P-items in T7) | P1–P6 shipped: 0.2sp top-bar tracking, 190dp rail + 3dp accent bar, 296dp check-in sidebar, Board kicker split, queued last-try on the same row, radius ramp 8/6/5. Extra chrome (design-file values): pad 20, clock 13, lotus 54. |
| Applicant desk history | dataset first/recent/counts only | Prior courses / Activity / Clarifications + clarification PDF (T6). Not server Advanced Search. |

---

### Current Industry tokens (`Skin.kt`)

### Steel (hand-picked hexes — the wireframe)

| Token | Hex |
|---|---|
| bg | `#F2F2F3` |
| surface | `#E9E9EA` |
| text | `#1D1F20` |
| neutral 100–900 | `#F5F5F8` `#E7E7EA` `#D4D4D7` `#B7B7BA` `#98989B` `#7A7A7D` `#5D5D60` `#424244` `#2B2B2D` |
| accent | `#5980A6` |
| accent 100–900 | `#EEF6FF` `#D6EBFF` `#B5D9FD` `#94BCE3` `#749DC4` `#597EA3` `#416180` `#2C455D` `#1D2D3D` |
| chip | `#B5D9FD → #5980A6 → #2F4A66` |
| mark opacity | 0.11 |
| mark filter | saturate(0.45) hue-rotate(165°) |

### Paper / Blossom / Pond / Still (OKLCH ladder, hue+chroma only)

| Skin | hue | chroma | mark opacity | mark filter |
|---|---|---|---|---|
| Paper | 262 | 0.03 | 0.18 | grayscale(0.92) |
| Blossom | 352 | 0.095 | 0.17 | none |
| Pond | 152 | 0.07 | 0.15 | hue-rotate(76°) saturate(0.8) |
| Still | 272 | 0.095 | 0.16 | hue-rotate(202°) saturate(0.85) |

Every other token is the same lightness ladder as `version-3/DELTA.md` §1:

- bg `oklch(97.4% c·0.16 h)`, surface `95.2% c·0.14`, text `23% c·0.25`
- neutrals 100–900: L 97.6 / 93.6 / 87.5 / 78.5 / 67 / 55 / 45 / 35 / 26; C `c·0.12` (600–900 `c·0.14`)
- accent `oklch(56% c h)`; accent 100–900 L 97/93/87/78/56/50/43/35/26 with C factors 0.3/0.55/0.75/0.92/1/1/1/0.92/0.8
- chip `oklch(88% c·0.75 h) → oklch(56% c h) → oklch(30% c·0.8 h)`

Status / severity colours stay **fixed hexes**. They do not follow the skin.

**Faces:** Barlow Condensed (titles, crumbs, kickers) + IBM Plex Mono (matrix digits, sync counts). Design file still wins arguments; this table is what the tablet actually paints.

---

### Live Pixel C inventory (this export)

App **1.22.0**, tablet 2560×1800, session `sudha.user` · Dhamma Sudha.

| File | Screen / state |
|---|---|
| `01-login.png` / `20-login-clean.png` | Compact Steel login, remember-me filled, **no keyboard** |
| `02-centre-dashboard.png` | Matrix cards, 60/40, three older courses, blended tiles (earlier pass) |
| `03-centre-older-and-tiles.png` | Same page after a swipe |
| `04-centre-settings.png` | Room-chart card first; real switches; RESULT; accommodation Mbk 70 / Fbk 46 / Guest 3 |
| `05-room-chart.png` | Female · Fbk · 46 rooms · 4 per row · 12 rows, stepper live |
| `06-advanced-search.png` | In-app name/conf search |
| `07-settings-steel-light.png` | Steel selected, Light, online, lotus on, 1.22.0 |
| `08-settings-offline-on.png` | Simulate offline on |
| `09-settings-blossom.png` | Blossom selected; **offline strip** at top; `Offline · 0 changes queued` |
| `10-settings-dark-steel.png` | Theme Dark + offline; skin chip still Blossom (dark tokens stay steel) |
| `11-centre-blossom.png` | Centre in Blossom + offline strip |
| `12-desk-board.png` / `18` | Six-rail Board; no centre-name heading; 12 exports |
| `13-desk-applications.png` | List–detail |
| `14-desk-audit.png` | Findings |
| `15-desk-calling.png` | Call round |
| `16-desk-checkin.png` | Roster + roll + rooms-free. Search still held `NF24` (deskScan leftover — real bug) |
| `17-desk-rooms.png` | Rooms & seats grid + PULL FROM SERVER |
| `19-centre-after-desk.png` | Back on centre (earlier pass) |
| `21-centre-steel.png` | Centre after re-login, Steel, online |

### Not captured (cannot fake on this tablet)

- **Today loading skeleton** — phone-width `TodayScreen` only. Pixel C is always ≥600dp; that skeleton never paints here.
- **Queued strip** — outbox was empty. A real queued shot would mean a live `/change-status` write. Offline strip is in `09`/`11`.
- **Empty older-courses** — Sudha has three older courses. The empty path only hides the heading.

---

### What is still fair game for a design pass

- Dark mode is steel-only; a real dark-per-skin (or a stated “dark is steel” treatment) has room.
- Check-in search persisting `NF24` across sessions (still visible in `16`).
- Centre desk tiles below the fold on the 40% pane (only the top of the three tiles shows until scroll).
- Board fourth-row chip for Day-11 is **shipped** (solid row under the 3×4 shelves; dashed GAP badge is never drawn). Do not re-propose the gap marker or a 13th shelf cell.
- Skin photographs: **do not bring them back** unless the owner asks. The app deleted them on purpose.

### Parked (decision-gated — do not implement from a design pass)

- **Server-side Advanced Search** (`POST /search-app`) — HAR + AGENTS assumption 5. Needs a re-verify and owner authorization.
- **Real photo upload** — no live desk route; mock only.
- **Centre-screen metric drift** — shipped `cardRows` is Received / Confirmed+Expected / Cancelled / Total. Changing the math needs an on-device look.

---

### Hard rules unchanged

Live Drupal desk at `https://dipi.vridhamma.org`. No `/staff/*`. No client ACL. Never send `Approved`. Never persist/log NPI. Never send `?r=` on sheet GETs. Skin + lotus + room-layout columns are device-local DataStore, wiped by Erase-all.


---

## Course ops — assistant-teacher mode (turn 2, options 2a-2d, adopted 2026-09-02)

After registration day the tablet is handed to the assistant teacher, who reads the
roll. The design file's **turn 2** (top of `design/DIPI-Staff.dc.html`) specs a
device mode with two destinations (Teacher list, Seating plan) and a read-only
student card fed by two existing GETs: `/teacher-list/{cid}/{courseId}` and
`/application-view/{applicantId}`. Owner decisions of 2026-09-02: device PIN gates
the switch back; hall grid is registrar-configured device-locally; every
application is prefetched on entry (≤4 concurrent) and **answers may persist
on-device for the running course** (owner amendment — wiped on course change and
Erase-all); course ops stays read-only (no attendance).

### Ground-truth corrections (frames + server override the prose below)

Verified against the drawn frames and the live PHP (`dh_manageapp`); where the
turn-2 prose disagrees, these win:

- **Band text is pipe-separated**: `AT: {name} [{code}] | Male | Old | Group {n} | {N} total`; AT name can be the literal `(unassigned)`.
- **Twelve columns in server order**: S/N, Student, Room, Age, City, Courses, Cell, Seat, Occupation, Education, Languages, Comments — occupation/education/langs sit AFTER seat, not under the name; the client folds them per the frame regardless. S/N restarts per block. Old/New is derivable ONLY from the band.
- **Courses cell** carries 8 keys at most, in order `10D STP SPL TSC 20D 30D 45D 60D` (bold key + count); `Teen`/`Service` never print here. The frame's `SRV` chip does not exist; `SPL` does.
- **Seat strings**: `CW-` = chowky/cell, **`CH-` = chair (the design didn't know this prefix)**, optional `BR` backrest span; plain numbers at numeric-convention centres. Seat ids are data — row A can be `A1…A6, A8`.
- **Unseated rows** are ordinary rows with an empty Seat cell; the only "reason" the page carries is the name suffix — `(Sevak)`, `(AT)`, `(SAT-2011)`, `(T…)`, `(BT…)`.
- **The Comments column is an unlabelled concatenation of health text — the client never parses or stores it.** Flags derive from `/application-view` fields only: `HLTH` (Physical/Mental), `MED` (Medication), `INTOX` (Intoxicants), `TECH` (Other Techniques), `PREG` (Pregnancy = Yes), plus the frame's `MONK` (Personal · Monk/Nun = Yes). Pregnancy renders an `N/A` tag for male applicants (frame).
- **`GET /teacher-list` mutates server data on every request** (`zeroize_new_course_data`) — fetch once per entry, never poll. `?seating=1` exists (same markup, seat ORDER BY) but is unnecessary: seat order derives from labels client-side; one fetch serves both views.
- **`/application-view` is a full themed Drupal page**; the card parses ONLY the header, `Personal`, `Course History` (ten counts, server order `10-Day Teen STP Special TSC 20-Day 30-Day 45-Day 60-Day Service` — the frame's tile order was wrong) and `Health` (labels verbatim: `Physical, Mental, Medication, Intoxicants, Other Techniques, Pregnancy`; the frame's invented question texts are replaced by these per the spec's own verbatim rule). The parser NEVER touches `Identification`, `Emergency Contact`, `Contact`, `Background`, `Languages`, `Other`, `Children/Teen`, `Long Course Details`, or the four lazy-loaded sections.
- Photo: `<img src="{base}/show-photo/{id}">` when present — reuse `PhotoLoader`.
- Auth failures: wrong centre/gender/bad id ⇒ **404** (wildcard loaders); missing permission/expired session ⇒ themed 403/login HTML; teacher-list success is an **unthemed fragment** starting `<style>`.
- Token fixes vs frames: old seat cells fill `accent100` on `accent300` (the prose described the legend swatches); flagged answer cards use `accent100` (the frame's `#F7FBFF` is off-ramp and not adopted); the turn-2 prose's `neutral600` often means `#5D5D60` = neutral700 — hexes win.
- The drawn "Switching back asks for the centre PIN" switch is replaced by an always-on **device PIN** gate (owner decision — no PIN existed to reuse).
- **Seating r2 (owner feedback 2026-09-02, live web page overrides frame 2c):** the
  hall renders with **letters as columns** and seat numbers receding from the
  **teacher at the BOTTOM** (`TEACHER · DHAMMA SEAT` marker + column-letter axis
  both below the grid; A1/B1/C1 face the dhamma seat). The side rail is
  **CHOWKY / CHAIR** (`CW-` chowky, `CH-` chair) — real pagoda cells are a
  separate building and a future feature. Seat cells are 66dp with two-line
  ellipsized names (frame's 58dp clipped). Unseated sevaks are hidden (they sit
  on cushions the plan does not draw); the tally still counts them. Below
  1000dp the rail stacks full-width under the grid. HallGrid config =
  columns (1..26) × rows-deep (1..40).

### Screens / views

Shared frame geometry for all three teacher screens: 1280×900dp window · status bar 24dp · nav bar 48dp · content band 828dp · horizontal padding 24dp · background `bg #F2F2F3`, text `#1D1F20`, body Roboto 400 14sp/1.35.

**There is no desk rail in course ops.** The 190dp `DESK` rail from the desk build is not drawn — course ops has two destinations, and they are a segmented pair in the header instead.

---

#### 2a — App Settings gains the mode switch

**Purpose:** the registrar flips the tablet from desk ops to course ops at the end of day 0, and flips it back at the end of the course.

**Layout** — existing App Settings screen. Header band 56dp: 40dp back `‹` (`neutral600`) + "App Settings" Barlow Condensed 600 23sp / ls 0.2. Body is two columns, 20dp gap: left **660dp fixed**, right flexes with a 26dp top offset.

**Left column**

- Kicker `TABLET MODE` — IBM Plex Mono 500 9sp, ls 1.7, `neutral500`, margins 6dp top / 10dp bottom.
- Two radio cards, 10dp gap, padding 16/18dp, radius 8dp, 14dp gap between radio and text.
  - **Unselected:** fill `#FAFAFB`, 1dp `#DEDEE1`; radio 22dp circle, 2dp `neutral400` ring, empty.
  - **Selected:** fill `#FFF`, **1.5dp `accent #5980A6`**, shadow `0 1 3 rgba(65,97,128,.14)`, plus a 3dp accent bar at `left 0`, `top/bottom 14dp`, radius `0 3 3 0`; radio ring 2dp `accent700 #416180` with an 11dp `accent700` dot; title followed by an `ON` chip — mono 500 9sp / ls 1.4 / `accent700` on `accent100 #EEF6FF`, radius 3dp, padding 4/6dp.
  - Titles Barlow Condensed 600 19sp / ls 0.2. Descriptions Roboto 400 13.5sp/1.5 `neutral600`, 5dp above.
  - Copy, verbatim: **"Desk ops · registration"** / "Board, applications, calling, check-in, rooms & seats, exports. What the registrar uses on day 0." — **"Course ops · teacher"** / "Teacher list and seating plan only, for the running course. Desk destinations are hidden until the mode is switched back."
- Rule: 18dp margin, 13dp padding above a 1dp `#E0E0E3` border. Kicker `WHILE COURSE OPS IS ON` (same kicker style), 9dp below.
- Consequence rows, 6dp gap: 48dp tall, `#FAFAFB` on 1dp `#E0E0E3`, radius 6dp, padding 0 14dp; 18dp centred index Roboto 14sp `accent400 #749DC4`; key Roboto 14sp `#424244`; value Roboto 13sp `neutral500` right. These state plainly what disappears and what remains.

**Right column**

- Dashed card: 1dp dashed `#D4D4D7`, radius 8dp, `#FFF`, padding 16/18dp. Title "Course being taught" Barlow Condensed 600 17sp; body Roboto 14sp/1.5 `#424244` = course line + dates; caption Roboto 12.5sp/1.5 `#7A7A7D`: "Locked to the course that is running. The teacher never picks a course; the roll follows the dates."
- Below, 12dp gap: a 48dp row, 1dp `#D4D4D7`, radius 6dp, padding 0 14dp — "Switching back asks for the centre PIN" Roboto 14sp `#424244` + a 44×24dp Material switch, on-state `accent`, 18dp white thumb inset 3dp.

**Rules**

- The switch lives **inside the existing App Settings tile** — no new top-level destination, and the desk build is byte-identical when the mode is off.
- Course ops is a **mode, not a login role**. One tablet, one account, one running course. No new ACL, no new user type, no server-side permission.
- Entering course ops is a plain toggle; **leaving it prompts for the centre PIN** (existing PIN, existing prompt).
- The course is **locked to the course whose dates contain today**. The teacher never gets a course picker.

---

#### 2b — Teacher list (course ops home)

**Purpose:** the seniority roll. The teacher scans it standing at the front of the hall, and opens a card when a flag or a course history warrants it.

**Header band 62dp** — left: "Teacher list" Barlow Condensed 600 23sp / ls 0.2, and beneath it (4dp) the course line "Dhamma Sudha / 10 Day / 2026 / 19th-Aug to 30th-Aug" Roboto 13sp `neutral600`. Right: the destination pair, 8dp gap, both **48dp** tall, padding 0 20dp, radius 6dp —
- selected: `#FFF`, **1.5dp `accent`**, Roboto 500 14sp `accent800 #2C455D`;
- unselected: transparent, 1dp `neutral300 #D4D4D7`, Roboto 400 14sp `neutral600`.

**Group filter band 44dp**, 1dp bottom `#E0E0E3`, 8dp gap. Kicker `GROUP` (mono 500 9sp / ls 1.7 / `neutral500`, 4dp right margin), then one 30dp pill per group returned: radius 15dp, 1dp `#E0E0E3` on `#FAFAFB`, label Roboto 12.5sp `neutral600` + count IBM Plex Mono 500 11sp `neutral500`, 7dp gap, padding 0 12dp.

**Group band (sticky), 34dp** — `accent100 #EEF6FF`, 1dp `accent300 #B5D9FD`, radius 6dp, padding 0 12dp, 10dp gap. Group title Barlow Condensed 600 15sp / ls 0.3 `accent800`; qualifier Roboto 12.5sp `accent500 #597EA3`; count right, mono 500 12sp `accent700`. Title text is the page's own band text, e.g. `AT: Trainee-A-M Teacher [TAM]` + `Male · Old · Group 1` + `16 TOTAL`.

**Column header 28dp**, bottom-aligned, 1dp bottom `#E0E0E3`, padding 0 12dp, mono 500 9sp / ls 1.4 / `neutral500`:

| Column | Width | Align |
|---|---|---|
| S/N | 34dp | left |
| STUDENT | flex | left |
| ROOM | 86dp | left |
| AGE | 46dp | right |
| CITY | 124dp (+16dp left pad) | left |
| COURSES | 236dp (+16dp left pad) | left |
| SEAT | 64dp | right |
| FLAGS | 96dp | right |

**Row 52dp**, padding 0 12dp, 1dp bottom `#EDEDF1`, whole row tappable → student card (`2d`).

- S/N — IBM Plex Mono 400 13sp `neutral500`.
- Student — name Roboto 500 15.5sp/1.15 `text`; under it (3dp) the folded line Roboto 400 11.5sp `neutral500` = occupation · qualification · languages, as the page sends them, joined with ` · `.
- Room — IBM Plex Mono 400 13.5sp `neutral600`.
- Age — IBM Plex Mono 400 14sp `#424244`.
- City — Roboto 400 13.5sp `neutral600`.
- **Courses** — chips, wrapping, 4dp gap: 20dp tall, radius 3dp, `#EDEDF1`, padding 0 6dp, key mono 500 11sp `#424244` + value mono 500 11sp `neutral600`, 4dp gap. Only non-zero types appear (`10D 6` · `STP 2` · `30D 2` · `SRV 2`). **Empty history renders nothing** — a blank cell is how a new student reads at a glance.
- Seat — IBM Plex Mono **600** 14sp `text`.
- **Flags** — right-aligned, 5dp gap: 22dp pills, radius 11dp, `#FFF` on 1dp `neutral300`, mono 500 10sp / ls 0.8 / `neutral600`, padding 0 7dp.

**Next-group footer 40dp** at the bottom of the viewport: `#F5F5F8`, 1dp top `#E0E0E3`, padding 0 12dp — the next group's band text Barlow Condensed 600 15sp `neutral600`, its count mono 500 12sp `neutral600`, then `›` Roboto 15sp `neutral400`. It is a peek, not a control; scrolling continues normally.

**Data rules**

- **Grouping and order are the page's.** AT → gender → old/new → group, in the order the response lists them. The tablet **never re-sorts** and never merges groups; seniority order is meaning, not presentation.
- The web table's twelve columns don't fit a hand-held read. The row keeps what the teacher scans for; **cell, languages, comments and the rest move to the card**.
- **FLAGS are derived, never typed.** Emit a flag when the corresponding `/application-view` answer is non-empty:

| Flag | Source field |
|---|---|
| `HLTH` | Health · Physical, or Health · Mental |
| `MED` | Medication |
| `INTOX` | Intoxicants |
| `TECH` | Other Techniques |
| `PREG` | Pregnancy = yes |

  A flag says only "there is something written here" — it is never a summary or a severity. Fetching the card is what shows the words.
- 52dp rows and 48dp buttons throughout: this screen is used standing up.

---

#### 2c — Seating plan

**Purpose:** find a named student in the hall, or find out who a seat belongs to.

**Header band 62dp** — same as `2b`; title "Seating plan", sub line = hall + orientation + old/new tally ("Male hall · facing the front · 16 old, 14 new"). Destination pair with *Seating plan* selected.

**Hall + legend band 40dp** — hall tabs, 8dp gap: 32dp pills, padding 0 16dp, radius 16dp; selected `accent800 #2C455D` with white Roboto 500 13sp; unselected 1dp `neutral300`, Roboto 400 13sp `neutral600`. Right-aligned legend, 14dp gap between items, 6dp between swatch and label: 12dp swatches, radius 2dp — **Old** `accent300 #B5D9FD` on 1dp `accent400 #94BCE3` · **New** `#FAFAFB` on 1dp `neutral300` · **Empty** `#FFF` on **1dp dashed** `neutral400`. Labels Roboto 12sp `neutral600`.

**Front marker 30dp** — `#E7E7EA`, radius 4dp, centred, margin 2dp top / 10dp bottom, text `FRONT · DHAMMA SEAT` mono 500 9sp / **ls 2.4** / `neutral600`. Drawn once, at the top, so the plan is never read upside-down.

**Body** — two columns, 18dp gap: grid flexes, side column **280dp fixed**.

**Seat grid** — one row per hall row, 8dp vertical gap. Row = 26dp centred row letter (Barlow Condensed 600 15sp `neutral500`) + a `repeat(7, 1fr)` grid, 8dp gap.

**Seat cell 58dp** — radius 5dp, padding 6/8dp, column layout, `space-between`:
- seat id top — mono 500 10sp / ls 0.8 / `accent500 #597EA3`;
- name bottom — Roboto 500 12.5sp/1.15 `text`, clipped (never wrapped to a third line).
- **Old** = `accent300` fill + 1dp `accent400`. **New** = `#FAFAFB` + 1dp `neutral300`. **Empty** = `#FFF` + 1dp dashed `neutral400`, no name.

**Cell / pagoda column** — kicker `CELL / PAGODA`, then a 2-column grid, 8dp gap, of the same 58dp cells on `accent100` + 1dp `accent300`. `CW-` seats are not in the row grid on the web page either, so they get their own column rather than being force-fitted into row A–E.

**Unseated** — 14dp margin, 11dp padding above a 1dp `#E0E0E3` rule; kicker `UNSEATED`, 9dp below; rows 34dp, 6dp gap, `#FAFAFB` on 1dp `#E0E0E3`, radius 5dp, padding 0 10dp — name Roboto 13sp `#424244` + reason tag mono 500 10sp / ls 0.8 / `neutral500` right. Sevaks come back with an empty seat cell; they land here **with their reason**, never dropped.

**Rules**

- A seat tap and a list-row tap open the **same** student card. Seat and name are two doors into one record.
- The plan is **read-only**. No drag, no reseat, no editor — seating belongs to the registrar's desk build.
- Hall switching is client-side over the one response.

---

#### 2d — Student card

**Purpose:** the whole reason the teacher has the tablet. What in this student's past or present could affect his meditation, in the student's own words.

**Header band 60dp** — 44dp back `‹`; then name Barlow Condensed 600 24sp / ls 0.2 with a status chip 10dp right (mono 500 10sp / ls 1.2 / `accent700` on `accent100`, radius 3dp, padding 5/7dp, e.g. `OLD · OM7`); under it (4dp) the placement line Roboto 12.5sp `neutral600` — `Mbk-37 · seat E1 · Group 1 · TAM`. Right: prev/next pair, 8dp gap, **48dp** tall, min-width 48dp, padding 0 16dp, 1dp `neutral300`, radius 6dp, glyph Roboto 20sp `neutral600`. They walk the **current group in seniority order**, so a teacher can read one group through without returning to the list.

**Body** — two columns, 18dp gap: left **404dp fixed**, right flexes.

**Left column — the facts, compressed**

- Photo + personal, 14dp gap.
  - **Photo 132×158dp**, radius 6dp, `#E7E7EA` on 1dp `neutral300`. Placeholder text mono 500 9sp/1.5 / ls 1 / `neutral500`, centred. Real source: the photo on the application. Never cropped to a circle; never enlarged past 132dp.
  - **Personal table**, flexes: rows 22.5dp, 1dp bottom `#EDEDF1`; key Roboto 12sp `neutral500` left, value **IBM Plex Mono 12.5sp** `text` right. Keys, in order, exactly as `/application-view` labels them: Gender · Date of Birth · Age · Nationality · Old / New · Monk / Nun · A-List · Applied On. Missing values render the page's own `-`.
- **COURSE HISTORY** kicker, 8dp below, then a `repeat(5, 1fr)` grid, 6dp gap, of 50dp count tiles: radius 5dp, 1dp border, centred column, 3dp gap — value IBM Plex Mono 600 18sp, key mono 500 8.5sp / ls 0.9 / `neutral500`. Ten tiles, in the page's order: `10-DAY TEEN STP SPECIAL TSC 20-DAY 30-DAY 45-DAY 60-DAY SERVICE`.
  - **Non-zero** tile: `accent100 #EEF6FF` on 1dp `accent300`, value `accent700`.
  - **Zero** tile: `#FAFAFB` on 1dp `#E7E7EA`, value `neutral400`. Zeros stay on screen — the shape of the history is the information.
- **History meta** rows, 8dp below: min-height 26dp, 1dp bottom `#EDEDF1`, 10dp gap — key 104dp Roboto 12sp/1.3 `neutral500`, value Roboto 13sp/1.3 `text`, wrapping. Keys: First Course · Last Course · Practice Details. Values verbatim (`2025-1-15, Dhamma sota sohna`).

**Right column — what the applicant wrote**

- Kicker `WHAT THE APPLICANT WROTE` + a caption Roboto 11.5sp `neutral400`: "page 2 of the application · in his own words". 9dp below.
- One card per question, 8dp gap, radius 7dp, padding 11/14/12dp, 1dp border.
  - Head row, 12dp gap: 20dp question index mono 500 12sp/1.3 `neutral400`; the **question text** Roboto 13sp/1.4 `neutral600` (verbatim from the application — the teacher must see what was asked); then a tag chip 24dp, radius 12dp, padding 0 9dp, mono 600 10.5sp / ls 1.1.
  - **Answered** card: `#FFF` on 1dp `#DEDEE1`, tag `YES` `accent700` on `accent100`.
  - **Answered, flagged** (health / medication / intoxicants / other techniques / pregnancy): tinted `accent100` on 1dp `accent300`, same `YES` tag — the same flags that surfaced in `2b`.
  - **Empty** card: `#FAFAFB` on 1dp `#E7E7EA`, tag `NO` `neutral500` on `#E7E7EA`, and no body. **The row still shows.** Absence is information; hiding it would make an unanswered question look like a clean one.
  - **Answer body** — 9dp above, 32dp left indent, 12dp left padding, **2dp left rule** (`accent400` on flagged cards, `#DEDEE1` otherwise); text **Roboto 400 14.5sp/1.5 `text`, `text-wrap: pretty`**, never truncated, never behind a "more" affordance. This is the largest body type on any screen in the app, on purpose.
- The column scrolls; the left column does not.

**Rules**

- **Read-only by design.** No edit, no note field, no export, no share. If the teacher needs something recorded it goes to the registrar on the desk tablet.
- **Weighting is reversed against the desk build:** answers get the type, admin facts compress. That is the ordering the teacher asked for — history and answers first, personal info and photo alongside, admin last.
- Never summarise, score, rank or colour-code a health answer. Quote it.

---

### Interactions & behaviour

- **Mode switch:** toggling to course ops replaces the navigation graph's start destination with Teacher list and removes every desk destination; toggling back requires the centre PIN. The setting is device-local (see State).
- **Teacher list ⇄ Seating plan:** the header pair is a two-way segmented control over one fetched response — switching does not refetch.
- **Group pills** filter the list to one group; the sticky band and the footer peek follow the filtered set.
- **Row tap / seat tap** → student card. `‹ ›` in the card header walk the group in seniority order and stop at its ends (no wrap into the next group).
- **Offline:** both teacher screens use the existing offline strip (38dp, `#E7E7EA` on 1dp `#DEDEE1`, `◍` + "Offline — showing cached list", Roboto 14sp `neutral700`) and **push content down, never float**. The roll and all cards for the running course should be cached on entry to course ops so a hall with no signal still reads.
- **No queued strip in course ops** — nothing here writes, so there is never an outbox.
- Touch targets ≥ 48dp. Rows are 52dp; seat cells 58dp.

### State

New, device-local (DataStore, alongside skin and lotus, wiped by Erase-all):

- `tabletMode: DESK | COURSE_OPS` — default `DESK`.
- `courseOpsCourseId` — resolved from the running course's dates on entry, re-resolved on app start; never user-picked.

Per-screen:

- `rollResponse` (from `/teacher-list/…`) — the single source for both `2b` and `2c`, cached with a fetched-at stamp.
- `groupFilter: String?`, `hall: MALE | FEMALE`, `view: SENIORITY | SEATING`.
- `applicationView(applicantId)` cache — the card's data; prefetch the group so `‹ ›` is instant.
- Derived, not stored: the FLAGS set per student, and the non-zero course-history chips.

No new writes, no new endpoints, no new ACL. Course ops is `GET`-only.

### Design tokens

Course ops uses the shipped **Steel** ramp with no additions:

| Token | Hex |
|---|---|
| bg | `#F2F2F3` |
| text | `#1D1F20` |
| neutral 100–900 | `#F5F5F8` `#E7E7EA` `#D4D4D7` `#B7B7BA` `#98989B` `#7A7A7D` `#5D5D60` `#424244` `#2B2B2D` |
| accent | `#5980A6` |
| accent 100–900 | `#EEF6FF` `#D6EBFF` `#B5D9FD` `#94BCE3` `#749DC4` `#597EA3` `#416180` `#2C455D` `#1D2D3D` |
| row hairline | `#EDEDF1` |
| card hairline | `#DEDEE1` · rules `#E0E0E3` |

**Steel night (dark, all skins)** — `#14171A` `#1A1E22` `#22272C` `#2E3339` `#3A4046` `#4A5157` `#6B7278` `#9BA1A8` `#E4E6E9`; accent unchanged, tint `#1D2D3D`, accent text `#B5D9FD`. Course ops screens are not drawn in dark in this pass; they inherit the ramp mechanically. Ask for a dark frame if a hall is dark at 4am.

**Accent discipline (unchanged):** accent means *old student, selected, or live*. On these screens that is exactly three things — the old-student seat tint, the selected destination/mode, and the answered-question tint. Everything else is a hairline on the neutral ramp.

**Type** — Barlow Condensed (titles, group bands, row letters, buttons) · IBM Plex Mono (ids, ages, counts, kickers, seat ids, personal values) · Roboto (names, body, answers). Nothing below 8.5sp for kickers; no body text below 12sp; the answer body is the ceiling at 14.5sp.

**Spacing** — 6 / 8 / 12 / 14 / 18 / 24dp. Radius 3dp (chips) · 5dp (seat cells, small rows) · 6dp (buttons, tiles) · 7dp (answer cards) · 8dp (setting cards) · 11–16dp pills.

**Fixed severity (never follows the skin)** — danger `#A33A34` light / `#E0796F` dark. Note that **no health answer uses it**: severity is for destructive actions, not for students.

### Assets

- `assets/lotus.png` — the lotus **vector** mark, cropped from the app's own sign-in capture. A stand-in for the HTML only; use the app's existing drawable. Never a photograph.
- Fonts load from Google Fonts in the HTML; the app already ships Barlow Condensed, IBM Plex Mono and Roboto.
- The student photo in `2d` is a placeholder box; the real image comes from the application record.
- No new icons are introduced. `‹ › ↓ ◍` are the existing glyph set.

### Files

| File | What it is |
|---|---|
| `PROMPT.md` | Paste-ready brief for Claude Code |
| `README.md` | This spec — course ops (turn 2, options `2a`–`2d`) |
| `README-v4-desk-pass.md` | Spec for the earlier desk pass (turn 1, options `1a`–`1i`) in the same HTML file |
| `design/DIPI-Staff.dc.html` | The design reference — open in a browser |
| `support.js` | Runtime the HTML needs; keep it beside the HTML |
| `assets/lotus.png` | Lotus mark used by the frames |

### Do not

- Do not make course ops a **login role**, a second account, or a server-side permission. It is a device mode.
- Do not give the teacher a **course picker**; the roll follows the running course's dates.
- Do not **re-sort or re-group** the teacher list. Seniority order is the page's.
- Do not add **write** affordances: no attendance marking (not asked for yet — flagged as an open question), no notes, no edit, no photo upload, no export, no share sheet.
- Do not add a **seating editor** or drag-to-reseat.
- Do not **truncate, summarise, score or colour-code** an applicant's answer, and do not hide unanswered questions.
- Do not invent JSON contracts, `/staff/*` endpoints, or client-side ACL. Two existing `GET`s only.
- Do not draw the desk rail, the queued-sync strip, or any desk destination while course ops is on.

---

## Sheets v5 — desk sheets, Course report, Board, Rooms (turn 5, frames `5a`–`5t`, shipped 1.34.0 on 2026-09-02)

Design spec: `version-5/README.md`; frames `5a`–`5t` in `DIPI Sheets v5.dc.html`;
plan and progress ledger: `docs/plans/2026-09-02-sheets-v5.md`.

**The problem this pass fixed.** The twelve desk sheets arrive as print-styled
Drupal HTML with JavaScript off, so every `Columns:` pill, `Print` link, in-sheet
hyperlink and the whole seating drag-and-drop panel is dead furniture drawn at the
same weight as the data. The seating plan spent roughly 400dp of a 900dp screen on
an instruction panel about dragging students that cannot work here.

### What shipped — do not re-propose

- **Injected sheet stylesheet** (`feature/desk/.../SheetStylesheet.kt`). Real CSS,
  the one place in the app where the design file's markup ships verbatim. It hides
  the dead furniture and gives all six HTML sheets one table style. **JavaScript
  stays off**; the stylesheet is injected at `loadDataWithBaseURL` time and never
  travels in the transport payload.
- **Sheet chrome** — `SheetHeader` (title, course identity **once**, and a
  `VIEW ONLY` / `READ & PRINT` chip) plus `SheetControlBand`: a segmented sort that
  refetches, and column chips that toggle a CSS class with no refetch.
- **Sort parameter allowlist.** `SheetSort` is the only route from a control to a
  query parameter, and it knows exactly two names: `conf` and `seating`. Day 0 list
  offers confirmation-number order; teacher list and student chit offer seating
  order; everything else offers nothing. `SheetRouteSafetyTest` fails the build if
  any sheet GET can be built with `r`.
- **Native Day 0 summary.** `DaySummaryParser` → `SheetPayload.Summary` →
  `DaySummaryPane`. The `#day-summary` block is parsed for text rather than matched
  by tag, because the desk emits unclosed `<b>` tags.
- **Course report is a centre-dashboard destination, not a Board export.** The
  centre action tiles are a **2 × 2 grid at 66dp** with sub-lines; the report is
  native, the date range is its only control, and nothing is fetched until RUN.
- **Board:** eleven export chips on three shelves, shelf 3 keeping four-column
  width with an honest empty cell; chips de-emphasised to 38dp / `neutral600` /
  `accent300`; grey qualifier on each shelf kicker; one `accent300` arrow on each
  stat card. **Day-11 keeps its full-width fourth line** with an `END OF COURSE`
  tag — it is not a 13th grid cell, and the v4 dashed GAP marker is dead history.
- **Rooms & seats:** block headers read *"N occupied · N free of N"* over an
  occupancy bar; free cells lose the word "free" and the accent; amenity marks sit
  top-right under a pane-header legend. `( View )` is stripped at parse, beside
  `( PDF )`, so every screen is clean at once.

### Not built, deliberately

- **The hall grid.** The design draws rows A–E × 7 seats and records the geometry
  as *inferred, not observed*. Frames `5h`/`5i` are unbuilt. Only dead-furniture
  removal and the chrome shipped on seating. Picked up in
  `docs/specs/2026-09-02-seating-r2-orientation-spec.md`.
- **A seating editor.** The desk's drag-to-reseat is JavaScript and dead here. This
  surface is read + print. **Nothing labels, implies or reaches "regenerate"** — the
  `?r=1` route is unreachable from this app by construction, not by discipline.
- **Any new write protocol.** The pass added none.

### Hard rules unchanged

Live Drupal desk. No `/staff/*`. No client ACL. Server refusals verbatim. Never
send `?r=` on sheet GETs. Sheet bodies stay in memory or `cacheDir/sheets` and are
wiped on logout, session expiry and Erase-all.

---

## Reach v5 UI — Board native seating, print, visual pack (1.36.0 / 58, 2026-09-03)

Plan: `docs/plans/2026-09-03-reach-v5-ui.md`. Walkthrough on tablet 1.35.0 was
the visual source of truth; this pass ships the remaining pack.

### What shipped — do not re-propose

- **Native Board 5h.** The Board "Seating plan" chip opens the Course ops hall
  (`HallBody`: teacher-at-bottom, letters-as-columns, chowky/chair rail, 66dp,
  no drag). One `GET /teacher-list` if the roll is not already loaded; never
  `GET /seating`, never `?r=`, never a card prefetch. The injected stylesheet
  still unhides `.ui-state-default` for any leftover HTML path.
- **Binary Board exports** snack the server's words, `"$title came back empty"`,
  or *"{title} saved — no app on this device can open it"*. No in-app PDF/Excel
  renderer. OkHttp read timeout 60s; binary paths send `Accept-Encoding: identity`.
- **Day 0 summary stays an overlay.** PRINT + `SPECIAL SEATING` + `FROM THE DESK · HH:MM`.
- **Print CSS:** student chits 9-up; Contact `display:none` in `@media print`.
- **Teacher-list City / Education chips** (off by default). Cell / Languages stay off.
- **Stat arrows** stay `accent300`; cards are 112dp so the overlay arrow is not clipped.
- **Course report** displays `dd-MM-yyyy`, POSTs ISO, PRINT next to SHARE CSV,
  run strip `N COURSES · N STUDENTS · RAN HH:MM`. The `NEW` tag stays.
- **Entity decode** is shared (`HtmlEntities`) for Kotlin-parsed surfaces only.

### Not built, deliberately

- Group seating + Cell list under MORE ON THE DESK SITE.
- In-app PDF/Excel renderer.
- Moving Day 0 summary into the desk rail.
- "Fixing" stale Board KPIs (stale-until-revalidate is intended).
- A seating editor / `?r=`.

### PATCH 1.36.1 / 59 (2026-09-04)

- Binary sheet `save()` / `body.bytes()` / file write run on `Dispatchers.IO`. Smoke of 1.36.0: Laundry `NetworkOnMainThreadException`, Male PDF sat on the 60s Main-thread read timeout.
- Student chit print CSS wins over the imported desk stylesheet: 9-up **63.3×92.3mm** on A4; Contact stays `display:none` in `@media print`. The print job media is `ISO_A4` (Pixel C default is Letter).

### MINOR 1.37.0 / 60 (2026-09-04) — Course ops v6 advise

Plan: `docs/plans/2026-09-04-course-ops-design.md`. Visual authority:
`DIPI Course ops v6.dc.html`. Seating-r2 locks held (teacher at the bottom,
letters as columns, CHOWKY / CHAIR, **66 dp** cells, sevaks hidden). No new
GET, no search, no writes.

- **Teacher list.** SEAT 76 + FLAGS 150 + 16 dp gutter; COURSES collapses when
  the rendered set has no chips (stated on a foot line); selected GROUP pill
  is accent-filled; `Clear filter ×`; filter-empty body; `N on the roll`;
  FLAGS pending bar while a card has not landed.
- **Hall.** Rail cells follow the hall old/new tint; `CW · CHOWKY` /
  `CH · CHAIR` sub-labels in a hairline card; empty chair/chowky run says
  `None in this hall`; UNSEATED kicker + count; hall pills carry seated
  counts. Header tally stays `N old, N new`.
- **Student card.** NO rows 56 dp, YES full cards, 34 dp summary, named back
  door, walk position, CAME FROM footer, `in his/her own words`.
- **PIN / Settings / empty.** Four PIN cells + “not the account password”
  (store unchanged); Settings PIN-row and Erase-all copy; consequence rows
  for writes and health storage; offline strip cache age; roll-error /
  empty-host bodies (server verbatim; no silent `/teacher-list` retry).

### PATCH 1.37.1 / 61 (2026-09-05) — owner Board / hall / print

- **Rail.** Display string **0 Day Board** (`DeskSection.Board` id unchanged).
- **Hall.** Chowky/chair default is one horizontal row, CW then CH, suffix
  ascending (`CW-A1` first). `ChowkyRailLayout.WRAP` keeps the older 2-across
  stack (Centre settings · Hall chart). Teacher stays at the bottom; 66 dp;
  Male/Female pills stay; a 12 sp foot line repeats 5h's read-only sentence.
- **Board chips.** Male PDF and Female PDF are gone — no `course-pdf-m|f`
  fetch from the Board or the phone hub. ROLL SHEETS is Day 0 list + summary
  plus two honest holes. Day-11 PDF stays.
- **Print.** Student chits **12-up** (5e, 63.3×69.3mm); Contact stays off.
  Checking slip **2-up** stacked (5g, 190×138.5mm) with a TIME/PLACE band.
  Chit ORDER default is **Name** (5f); Course ops destination is **Teacher list**.

### PATCH 1.37.2 / 62 (2026-09-05) — Board 3×3 + vertical rail

- **Board exports.** SHEETS & EXPORTS is a neat **3×3** of equal 64 dp cells.
  No shelf headers (`ROLL SHEETS` / `DESK SLIPS` / `FOR THE TEAM`), no
  `RARELY URGENT` / `END OF COURSE`, no full-width Day-11 row. Grid (day-0
  first): Day 0 list · Day 0 summary · **Course summary** / Student chit ·
  Checking slip · Seating plan / Teacher list · Manager list · Laundry list.
  **Valuable list left the Board** (enum + phone hub + `GET /valuable-list`
  stay). Male/Female PDFs stay gone. Course summary is a normal cell
  (`export-chip` + `export-day11`); `onExport("Course summary")` →
  `SheetExport.Day11Report` → `GET /report-day11`. The older Board string
  `"Course summary report"` still resolves.
- **Hall.** Chowky/chair default is one **vertical column**, CW then CH,
  suffix ascending, painted bottom-to-top so `CW-A1` sits nearest the
  Dhamma seat. `ChowkyRailLayout.WRAP` keeps the older 2-across stack
  (Centre settings · Hall chart). Teacher stays at the bottom; 66 dp.

### MINOR 1.38.0 / 63 (2026-09-05) — seating print + report empty-state

- **Native seating print (5i).** The Board seating surface's `READ & PRINT`
  tag now has a real `PRINT` button. It prints from the **in-memory roll**
  through the same pure `hallLayout` the screen draws — no `GET /seating`,
  no invented geometry — one gender per A4 page, depth descending (row 1 at
  the Dhamma seat), with the chowky/chair rail and the visible unseated list.
  `seatingPlanPrintHtml` (feature/desk). The button is hidden when the roll
  is empty (nothing to print).
- **Course report empty range.** The live desk answers a range with no
  courses with a header plus one blank-name, all-zero row; that row is not a
  course, so `CourseReportCsvParser` now drops blank-name rows. An empty
  range therefore renders the `EmptyRange` guidance instead of a ghost
  "1 course · 0 students" line (verified on the Pixel C, any future/reversed
  range).
