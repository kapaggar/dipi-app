# Handoff: DIPI Staff — v4 desk pass on 1.22.0

## Overview

A design pass on the DIPI Staff centre-registrar desk: a native Android tablet app used by Vipassana centre staff to verify applicants, call them, check them in and pull rooms. One account → one centre → upcoming courses → a six-section course desk (Board, Applications, Audit, Calling, Check-in, Rooms & seats).

This pass fixes a clipped fold, an unreachable keyboard-up login, a persisted-scan bug, a confusing dark mode, a left-weighted settings page and a loose Board grid. It adds **no screens and no features**.

## About the design files

`DIPI Staff v4.dc.html` is a **design reference created in HTML** — a prototype of intended look and measurements, not production code. The target is the existing **Jetpack Compose / Kotlin** app. Recreate the frames there using the existing `Skin.kt` tokens, existing composables and existing navigation. Do not port HTML, CSS or the file's canvas chrome.

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

**2026-08-30:** Manage Courses, Daily Activity and SMS Report are gone from the desk-site chip row — owner instruction; still reachable on the desk site. The chip row is now two links (Course Report, Bulk Mail), not five. See the do-not-re-propose list in `uploads/dipi-ui-export/SHIPPED-DELTA.md`.

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

**Other skins** — OKLCH ladder unchanged; see `uploads/dipi-ui-export/SHIPPED-DELTA.md`. Blossom = hue 352 / chroma 0.095 / mark opacity 0.17 / no filter.

**Fixed severity (never follows the skin)** — danger `#A33A34` light / `#E0796F` dark. Status colours likewise stay put.

**Accent discipline** — accent means one thing: live, occupied, or selected. `accent100` selected fills and hovers · `accent` active bars, ticks, solid primaries · `accent600` pressed · `accent700–800` text on tints and numerals. Everything else is a hairline on the neutral ramp.

**Type** — Barlow Condensed (titles, crumbs, kickers, buttons) · IBM Plex Mono (matrix digits, counts, kickers, timestamps) · Roboto (body and controls). Sizes are listed per component above; nothing below 9sp, and no body text below 12.5sp.

**Spacing** — 6 / 8 / 12 / 14 / 18 / 24dp. Radius 5dp (segments) · 6dp (fields, tiles, chips) · 8dp (cards) · 10dp (login card) · pill for Log out. Elevation: cards `0 1 2 rgba(0,0,0,.05)`; login card `0 2 8 rgba(0,0,0,.07)`; **desk tiles stay at 0**.

## Assets

- `assets/lotus.png` — the lotus mark, cropped from the app's own sign-in capture (`20-login-clean.png`). Use the app's existing vector drawable; this PNG is a stand-in for the HTML only. **It is a vector mark, never a photograph.**
- `uploads/dipi-ui-export/*.png` — the seven 1.22.0 captures referenced as "before" thumbnails.
- Fonts load from Google Fonts in the HTML; the app already ships both families.

## Files

| File | What it is |
|---|---|
| `PROMPT.md` | Paste-ready brief for Claude Code |
| `DIPI Staff v4.dc.html` | The design reference — open in a browser |
| `support.js` | Runtime the HTML file needs; keep it beside the HTML |
| `assets/lotus.png` | Lotus mark used by the frames |
| `uploads/dipi-ui-export/SHIPPED-DELTA.md` | What shipped after v3 — the do-not-re-propose list and the full token tables |
| `uploads/dipi-ui-export/*.png` | 1.22.0 captures |

## Do not

Reintroduce lotus photographs · one-line course counts · a single centre scroll instead of 60/40 · raised desk tiles · "from your account" · the older-courses chrome line · a Board heading repeating the centre name · ON/OFF text instead of switches · Room chart as a small link · App Settings hidden in overflow. All were removed on purpose; `SHIPPED-DELTA.md` has the full list.
