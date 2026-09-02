# T7 — six measured v4 drifts

**Status:** specified, 2026-08-31
**Baseline:** after T1 (Board fourth row exists)
**Inherits:** Global Constraints in `docs/plans/2026-08-31-ui-gap-closure.md`

Cite + change only these. Values vs `version-4/DIPI Staff v4.dc.html` /
`version-4/README.md`:

1. **Top-bar tracking** — `DeskShell.kt` `letterSpacing = 0.1.em` → `0.2.sp`.
   Leave pad 26 and clock 11.
2. **Rail geometry** — width `212.dp` → `190.dp`. Rows 46 dp. Selected =
   `accent100` + 3 dp left accent bar.
3. **Check-in sidebar** — `266.dp` → `296.dp`.
4. **Board kicker** — split `"SHEETS & EXPORTS · RARELY URGENT"` into
   `SHEETS & EXPORTS` + muted `RARELY URGENT`. Do not add/remove chips.
5. **Queued strip** — offline pad 16 → 24. Last-try on the **same row** as
   the count. Heights 38/56 stay.
6. **Global radius ramp** — `DeskStyle` 12/10/8 → **8 / 6 / 5**.

## NOT-list (do not change)

Top-bar pad 26 vs 20; clock 11 vs 13; lotus 72 vs 54; `DeskStyle.cardFill`
Industry lift; centre `cardRows` vs v4 frames; NEXT copy; Day-11 dashed
marker; Board 100/58/40 densify; strip heights; skin photos.

## Shipped beyond the six P-items

I3 also shipped the design-file values for pad/clock/lotus (20 / 13sp / 54)
plus the listed incidental type/geometry, beyond the original six P-items:
roster name 15.5sp, roster meta 14sp / column 190dp, check-in headline 22sp,
"to arrive" 13sp, progress bar 5dp + `RoundedCornerShape(3.dp)`, rooms-free
"N / M", rail label 15.5sp / count 13sp, `Industry.surface` rail fills,
`DipiMono` user/sync lines, `BoardTile`/`BoardAction` elevation 1dp. Those
already shipped on 1.29.1 and match hard rule 9 (design file wins). Do not
revert pad 20 / clock 13 / lotus 54.

## Tests this invalidates

Only a `SyncBannersTest` assertion that the stack→row change breaks, if any.
Add rail width / sidebar width / tracking / radii assertions if a harness
already measures them.

## Never-touched

`DeskViewModel`, `DipiAppUi`, `statusColors()`, skins, `cardRows`.
