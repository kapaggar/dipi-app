# Shipped delta — live 1.22.0 Pixel C vs version-3 prototype

Compared: the Pixel C build **1.22.0** (`versionCode` 35, `sudha.user` / Dhamma Sudha, 2026-08-28) against `version-3/project/DIPI Staff.dc.html` and `version-3/DELTA.md`.

Screenshots live next to this file (`~/Downloads/dipi-ui-export/*.png`). Use these, not the v3 canvas, as ground truth.

The prototype still describes an earlier desk. Three owner rounds after v3 landed in the app and **must not be re-proposed**.

---

## Do not re-propose (already shipped, already accepted)

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

---

## Current Industry tokens (`Skin.kt`)

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

## Live Pixel C inventory (this export)

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

## What is still fair game for a design pass

- Dark mode is steel-only; a real dark-per-skin (or a stated “dark is steel” treatment) has room.
- Check-in search persisting `NF24` across sessions (still visible in `16`).
- Centre desk tiles below the fold on the 40% pane (only the top of the three tiles shows until scroll).
- Board exports wrap tightly; Day-11 “Course summary report” is not on this 1.22.0 fold (that tile lived on unmerged desk-gap / later work — confirm before drawing it back).
- Skin photographs: **do not bring them back** unless the owner asks. The app deleted them on purpose.

---

## Hard rules unchanged

Live Drupal desk at `https://dipi.vridhamma.org`. No `/staff/*`. No client ACL. Never send `Approved`. Never persist/log NPI. Never send `?r=` on sheet GETs. Skin + lotus + room-layout columns are device-local DataStore, wiped by Erase-all.
