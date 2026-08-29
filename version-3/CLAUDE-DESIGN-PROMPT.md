# Prompt for Claude Design — DIPI Staff 1.22.0 improvement pass

Copy everything below the line into the existing Claude Design project **“Mobile app design system”**. Attach `SHIPPED-DELTA.md` and the PNGs in this folder (`01`–`21`). Do not attach the old version-3 HTML as the source of truth.

---

You are iterating the **DIPI Staff** centre-registrar desk. This is a **native Android tablet app** (Pixel C, **2560×1800 landscape**, Android 8.1), not a phone and not a website. The screenshots in this chat are the **shipped 1.22.0** UI. Your version-3 prototype is stale. Improve from the screenshots + `SHIPPED-DELTA.md`, using your existing Industry design system.

## What this product is

A Vipassana centre desk: one account → one centre → upcoming courses → a six-section course desk (Board, Applications, Audit, Calling, Check-in, Rooms & seats). Staff verify applicants, call them, check them in, and pull rooms. The live host is Drupal HTML (`https://dipi.vridhamma.org`). You are **not** redesigning the backend, adding screens that do not exist, or inventing a consumer app.

## How to use the files

- **Ground truth:** the PNGs. Read them first.
- **Drift note:** `SHIPPED-DELTA.md` lists what already shipped after v3 and must not be re-proposed.
- **Your job:** redraw / tighten the **current** tablet UI in this project’s HTML medium (a version-4 style pass). Keep Barlow Condensed + IBM Plex Mono. Keep the Industry token discipline below.

## Industry colour discipline (do not break)

Accent is the **only** chromatic colour and it means one thing: live, occupied, or selected.

- `accent100` — selected fills, hovers
- `accent` — active bars, ticks, solid primaries (`#5980A6` in Steel)
- `accent600` — pressed
- `accent700–800` — text on tinted fills and numerals
- Everything else is a hairline drawing on the **neutral** ramp
- **Status and severity colours stay fixed hexes.** They do not follow the skin. Copy: *“Status colours stay put; they carry meaning, not mood.”*

**Steel (default, as shipped):**

| Token | Hex |
|---|---|
| bg | `#F2F2F3` |
| surface | `#E9E9EA` |
| text | `#1D1F20` |
| neutrals 100–900 | `#F5F5F8` `#E7E7EA` `#D4D4D7` `#B7B7BA` `#98989B` `#7A7A7D` `#5D5D60` `#424244` `#2B2B2D` |
| accent | `#5980A6` |
| accent 100–900 | `#EEF6FF` `#D6EBFF` `#B5D9FD` `#94BCE3` `#749DC4` `#597EA3` `#416180` `#2C455D` `#1D2D3D` |

Paper / Blossom / Pond / Still still use the OKLCH ladder in `SHIPPED-DELTA.md`. Lotus watermark is a **vector mark**, not a photograph.

## Do not re-propose (owner already accepted these)

1. **No lotus photographs** on login or as page ground. Deleted on purpose. Login stays a centred ~380dp card over a faint lotus relief.
2. **No one-line course counts.** Upcoming courses are **status matrix cards**: columns `NM OM M NF OF F`, rows Confirmed / Cancelled / Total +sevak, IBM Plex Mono. See `02`, `21`.
3. **Keep the 60/40 centre split** (upcoming ceiling 60%, older+tiles 40%, two independent scrolls, `fill=false` so no dead band).
4. **Keep blended desk tiles** — transparent fill, 0 elevation, not raised cards competing with courses.
5. **Do not bring back** “from your account”, the older-courses “Teacher list · valuables · seating” chrome line, or a giant Board heading that repeats the centre name. Crumb is `dates · dayChip` at 17sp. Board starts with the roll sentence.
6. **Keep real Material switches** on Laundry / Valuables / Groups, RESULT card, Room chart **first** and **editable columns** per gender+section (`05`).
7. **App Settings is a centre tile.** Do not hide it in overflow.
8. **Dark mode today is pinned to Steel tokens** even if another skin chip is selected (`10`). You may *design* a real dark-per-skin **or** a clearer “dark is Steel” treatment — pick one and state it. Do not silently recolour status.
9. **Do not add** a public App Store consumer flow, onboarding carousel, or new desk sections.

## What to improve (priority order)

Work in this order. For each item, show **before (screenshot name) → after (your frame)** and keep the same information density — this is a registrar desk, not a marketing page.

### P1 — Centre dashboard (`02`, `03`, `11`, `21`)

The 40% pane is cramped: older-course rows are fine, but **Centre desk tiles sit below the fold** (only the top of the three tiles shows). Tighten so **Centre Settings / Advanced Search / App Settings are fully visible** without a second scroll, without growing raised chrome, and without stealing the matrix cards’ 60% ceiling.

Also: matrix header labels (`NM`…) are small; empty cells are `·`. Improve readability **without** turning the card into a spreadsheet or adding icons for decoration.

### P2 — Login + keyboard (`01`, `20`)

The compact card is correct. Design the **keyboard-up** state on a 2560×1800 landscape tablet so SIGN IN and the server error stay visible. Do not go full-bleed. Do not add a photo hero.

### P3 — Check-in search leftover (`16`)

The field still shows `NF24` from an earlier session. Design the empty field, a clear affordance, and a “clear” control. This is a UX fix for persisted scan state — do not add a new filter language.

### P4 — Dark (`10`)

Either (A) a complete dark palette per skin that still obeys “status colours stay put”, or (B) Settings copy + chrome that make “Dark = Steel night” obvious when Blossom/Pond/etc. is selected. The current shot is confusing (Blossom chip + steel-dark surfaces).

### P5 — Settings (`07`, `09`)

The page is a left-weighted stack with a huge empty field on the tablet. Use the width: skin switcher + account/session column, without turning it into a settings-app clone. Keep the SKIN kicker, five chips, lotus switch, Log out, Erase all.

### P6 — Desk rail + Board (`12`, `16`, `17`)

Keep the six sections and the lotus rail mark. Improve: export grid tightness (`SHEETS & EXPORTS`); whether Applications / Rooms belong in NEXT (they have counts but no next-row); do **not** invent a 13th “Course summary report” tile unless you label it as a gap, not as shipped.

### P7 — States you do not have screenshots for

Design, don’t guess data:

- **Queued strip** stacked under offline: `{n} change(s) waiting to sync` + `RETRY` (accent100 / accent800 / uppercase retry). Offline copy is already `◍ Offline — showing cached list`.
- **Empty older-courses:** heading omitted (that is what the app does) — show how the tiles sit when the list is gone.
- **Phone Today loading skeleton** is out of scope for this tablet pass.

## Constraints

- Target **landscape tablet** first (2560×1800). Portrait only if it costs nothing.
- Touch targets ≥ 48dp. Pointer/hover is a bonus, not a theme.
- No NPI theatre: do not add extra ID-document chrome. Aadhaar on the card is display-only and already decided.
- No new product features (bulk mail, add application, photo upload, seating editor).
- Do not change API contracts or invent `/staff` JSON.
- Output: updated frames in this project, plus a short **delta list** of what you changed vs 1.22.0 (not vs version-3).

Start with P1 (centre fold) and P2 (login + keyboard) in Steel, then one Blossom frame so we can see the skin still holds.
