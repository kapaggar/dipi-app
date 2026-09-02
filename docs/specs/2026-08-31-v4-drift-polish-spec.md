# v4 drift polish — spec (T7)

**Status:** proposed, 2026-08-31
**Baseline:** `main` @ `0ce3342`, 1.27.0 / versionCode 42.
**Origin:** design-vs-code audit 2026-08-31 against `version-4/DIPI Staff v4.dc.html`
(binding per AGENTS.md hard rule 9) and `version-4/README.md` (measurements,
1px = 1dp). This spec covers only **unrecorded drift** — deviations no owner
decision sanctions. Post-v4 owner decisions (removed 416dp two-column pane, single
centre scroll, `Confirmed + Expected` card rows, retired tiles) are NOT drift; the
T8 spec records them in `SHIPPED-DELTA.md`.

Every item cites the design source. If code and this spec disagree with the
design file, the design file wins — re-measure there before deviating.

## P1 — Desk top bar (`DeskShell.kt:200-227`) · frame 1c/1f, `dc.html:243-246`

The worst single item: title `letterSpacing = 0.1.em` ≈ 1.7sp where the design
draws **0.2px** — 8× the intended tracking.

| Property | Design | Change to |
|---|---|---|
| title letterSpacing | 0.2px | `letterSpacing = 0.2.sp` |
| title color | `#1D1F20` (text) | `Industry.text` (currently `neutral700`) |
| title size | Barlow Condensed 600 17sp | keep if already 17sp, else 17sp |
| clock | mono 13sp `#7A7A7D` | `fontSize = 13.sp`, `Industry.neutral600` (currently 11sp neutral500) |
| horizontal padding | 20dp | `20.dp` (currently 26dp) |
| bar height / bottom hairline | 52dp / 1dp `#E0E0E3` | keep (already conformant) |

## P2 — Desk rail (`DeskShell.kt:122-197`) · `README.md:84`, `dc.html:228-241`

| Property | Design | Change to |
|---|---|---|
| rail width | 190dp | `190.dp` (currently 212dp) |
| ground | `#EFEFF0` fill + 1dp right border | `Industry.neutral150`-equivalent rail fill token — use the existing token nearest `#EFEFF0` in `Skin.kt`/`Industry`; never inline hex. Keep the right hairline. |
| lotus | 54dp at 18dp left inset | `54.dp`, `start = 18.dp` (currently 72dp at 14dp) |
| item height | 46dp fixed | `height(46.dp)` |
| item label | Roboto 15.5sp | `15.5.sp` (currently DipiSans 13.5sp — size changes, family stays the app's DipiSans) |
| count | mono 13sp `neutral500` | `13.sp` (currently 11sp) |
| selected treatment | `accent100` fill + **3dp left accent bar**, label `accent800`, count `accent700` | replace the rounded inset pill (`:169-175`) with a full-bleed `accent100` row + 3dp `Industry.accent` leading bar |
| footer | user + sync line both mono 12sp | `DipiMono`, `12.sp` both lines |

Note: the pill was a pre-v4 "sleek pass" choice (comment at `:166-167`) that v4
did not adopt and no do-not-re-propose entry protects. Delete the comment with it.

## P3 — Check-in sidebar + roster (`CheckInPane.kt`) · `dc.html:275-290`

| Property | Design | Change to |
|---|---|---|
| sidebar width | 296dp | `296.dp` (currently 266dp at `:406-410`) |
| sidebar ground | `#EFEFF0` + 1dp left hairline | same rail-fill token as P2 |
| progress number | Barlow Condensed 600 22sp | `22.sp` (currently 26sp at `:182`) |
| "N to arrive" | mono 13sp | `13.sp` (currently 11sp at `:197`) |
| progress bar | 5dp tall, radius 3 | `height(5.dp)`, `RoundedCornerShape(3.dp)` (currently 8dp pill at `:206`) |
| roster name | Roboto 15.5sp | `15.5.sp` (currently 14.5sp) |
| roster meta column | 190dp @ 14sp | `190.dp`, `14.sp` (currently 170dp @ 12.5sp) |
| Mark attended button | 132×36dp, 1dp `accent300`, radius 5dp, label ≥13sp | `width(132.dp).height(36.dp)`, border `Industry.accent300`, `RoundedCornerShape(5.dp)` (currently 150dp wide, 8dp shape, 12sp). The 36dp control sits in a 54dp row — pad the tap target to ≥48dp with `Modifier.minimumInteractiveComponentSize()`. |
| rooms-free value | `46 / 46` format | `"$free / ${block.size}"` (currently `"$free of ${block.size} free"` at `:447`) |

## P4 — Board (`BoardPane.kt`) · `dc.html:568`, `README.md:114`

- Split the single kicker `"SHEETS & EXPORTS · RARELY URGENT"` (`:128-132`) into
  two spans on one row: `SHEETS & EXPORTS` mono 500 9.5sp / ls 1.7 /
  `Industry.neutral600`, then `RARELY URGENT` mono 400 9.5sp / ls 1.2 /
  `Industry.neutral400` — the de-emphasis is the design's point.
- Stat cards: `deskCard(shape = CardShape)` at `:175` and `:217` currently
  default to 2dp elevation; design draws `0 1 2 rgba(0,0,0,.05)`. Pass
  `elevation = 1.dp` explicitly on both.
- Run AFTER T1 has landed (same file).

## P5 — Queued sync strip (`SyncBanners.kt:181-205`) · `dc.html:612-616`

One row, not a stacked column: copy takes `weight(1f)`, then "last try 10:29"
mono 12.5sp inline beside it, then RETRY. Horizontal padding `start = 24.dp,
end = 16.dp` (currently uniform 16dp). Depths (56/38dp), colors, RETRY chrome
are already conformant — do not touch them.

## P6 — Global radius ramp (`DeskStyle.kt:24-34`) · `README.md:170`

Design: "Radius 5dp (segments) · 6dp (fields, tiles, chips) · 8dp (cards) ·
10dp (login card)". Code runs one step larger: `cardRadius = 12`, `tileRadius
= 10`, `controlRadius = 8`.

Change the three constants to `cardRadius = 8.dp`, `tileRadius = 6.dp`,
`controlRadius = 6.dp` — every `deskCard`/`controlShape` consumer follows
automatically, and the three call sites that already opt out to spec values
(`LoginScreen.kt:69,72`, `BoardPane.kt:51-52`, `SyncBanners.kt:209`) become
consistent with their surroundings instead of exceptions.

**Risk note:** this repaints every card/field/segment in the app. Search
`app/src/test` for any assertion pinning a radius before changing (none are
known); after the change, eyeball Settings, Centre, Login and all six desk
sections on the Pixel C — this item alone justifies the install step.

## Explicitly NOT in this spec

- Centre-screen page metrics (24dp padding / 23sp header / 12dp grid gap) and
  the two-line older-course rows — they sit inside layout that owner feedback
  reshaped on 2026-08-30; changing them needs an owner look, not a drift fix.
- Login brand-block chrome, lotus wash alphas, ambient `deskWash`/`phoneWash`
  gradients — flagged in the audit as needing an on-device 1:1 against the
  frames; park until the owner compares.
- Settings `FlowRow` skin chips, 800dp breakpoint, TEST-MODE line — reasoned
  in-code; leave.

## Tests

- `DeskShellTest`: retarget any assertion pinned to the 212dp rail or pill
  highlight (this spec's direct subject). Add: selected rail row shows the 3dp
  accent bar (`testTag` it) and rail width is 190dp
  (`getUnclippedBoundsInRoot().width`).
- `DeskPanesTest` (check-in): add `markAttendedButtonMeetsTouchTarget` —
  assert the button's tappable bounds are ≥48dp tall.
- `SyncBannersTest`: the truth-table stays untouched; add a layout test that
  "changes waiting" copy and the "last try" text share a row (same top bound).
- P6: run the FULL suite; fix only assertions that pinned the old radii, each
  listed in the task report.

## Constraints

Tokens only (`Industry`/`LocalDipi`/`DeskStyle`) — zero inline hex anywhere in
this spec's diffs; every interactive control keeps ≥48dp tap target; dark mode:
every touched surface must re-verify against `DarkTokensTest` expectations
(Industry is not dark-aware — if a P2/P3 fill looks wrong in dark Steel on
device, stop and report rather than inventing a token); no agent trailers;
never bare `./gradlew test`.

## Versioning note

Registrar-visible polish across the desk → PATCH or MINOR per the master plan's
wave; Pixel C install mandatory (P6 repaints everything).
