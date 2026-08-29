# Paste this into Claude Code

You are implementing a design pass on **DIPI Staff**, the native Android tablet app for Vipassana centre registrars (Jetpack Compose, Kotlin, Pixel C 2560×1800 landscape, Android 8.1). The current shipped build is **1.22.0 / versionCode 35**.

Read `README.md` in this folder first — it is the full spec. Then open `DIPI Staff v4.dc.html` in a browser (it is a design reference, not code to copy) and compare each "after" frame with the 1.22.0 screenshot beside it.

## What this is

`DIPI Staff v4.dc.html` is an **HTML design reference**. It is drawn at the tablet's dp grid — every frame is 1280×900 CSS px and **1 px in that file = 1 dp in Compose**, so you can read sizes straight off it. Do not port the HTML. Recreate the frames in the existing Compose code using the existing `Skin.kt` tokens, existing composables and existing navigation.

## Scope — nine changes, in this order

1. **Centre dashboard, 40% pane** — split into two columns so Centre Settings / Advanced Search / App Settings are fully visible without a second scroll. Do not touch the 60% upcoming ceiling.
2. **Course matrix legibility** — MALE / FEMALE group caps, 12sp mono column labels, hairline gutter between the trios, neutral band behind the M and F subtotal columns. Keep `·` for empty cells.
3. **Login, keyboard-up** — the 380dp card collapses to ~324dp so SIGN IN and the server error stay above the IME.
4. **Check-in scan field** — session-scope the scan buffer (this fixes a real bug: `NF24` persists across sessions), add a placeholder and a 48dp clear control.
5. **Settings** — two columns on tablet width; Theme becomes a segmented control, Simulate offline becomes a switch.
6. **Dark** — keep dark pinned to the Steel night ramp, but **say so in the UI**. Repaint the dark heading, offline strip and Erase-all on night tokens.
7. **Severity colours** — pin to a fixed light/dark pair so Erase-all stops following the skin.
8. **Board** — NEXT rows 158 → 58dp, twelve exports on three shelves, stat cards 250 → 100dp.
9. **New states** — queued sync strip under the offline strip; empty older-courses reflow.

## Hard rules — do not break these

- Live Drupal desk at `https://dipi.vridhamma.org`. **No `/staff/*` endpoints, no new JSON contracts, no client-side ACL.** Never send `Approved`. Never persist or log NPI. Never send `?r=` on sheet GETs.
- Skin, lotus toggle and room-layout columns are device-local DataStore, wiped by Erase-all.
- **No new product features.** No bulk mail, no add-application, no photo upload, no seating editor, no thirteenth export.
- **Status and severity colours are fixed hexes.** They never follow the skin.
- Touch targets ≥ 48dp.
- Do not reintroduce anything on the "already shipped, do not re-propose" list in `uploads/dipi-ui-export/SHIPPED-DELTA.md`.

## One open item

The Board frame draws **"Day 11 · Course summary report" as a dashed GAP marker**, not a tile. It is not in 1.22.0. Do not implement it — ask the owner whether it is dropped, deferred, or living on an unmerged branch.

## How to work

Land it in the order above; 1–3 first. Each item is independently shippable. Prefer changing layout and tokens over adding composables. When the design and the existing code disagree on something the README does not cover, follow the existing code and flag it.
