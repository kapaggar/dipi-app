# Design delta — version-3 vs version-2 ("DIPI Staff" DC prototype)

Compared: `version-3/project/DIPI Staff.dc.html` (180,412 B, 2,004 lines) against
`version-2/design/DIPI Staff.dc.html` (160,016 B, 1,841 lines).
`styles.css`, `support.js` and the fonts are **byte-identical** — the whole revision
lives in the one HTML file. The zip's `README.md` is the generic handoff cover and the
`_ds` readme is the unchanged Industry guide; neither adds spec.

## What changed

### 1. New section 3a — "Five skins, one wireframe" (the headline feature)
Five selectable skins re-colour the entire design from one token block each:

| Skin | Key | Hue/Chroma (OKLCH) | Photo (`--sk-photo`) | Mark filter (`--sk-markf`) | Mark opacity |
|---|---|---|---|---|---|
| Steel (default) | `steel` | hand-picked hexes (accent `#5980a6`) | `Archive/images.jpg` | `saturate(.45) hue-rotate(165deg)` | .11 |
| Paper | `paper` | 262 / .03 | `Archive/AnneBelmont_Pink-Lotus.jpg` | `grayscale(.92)` | .18 |
| Blossom | `blossom` | 352 / .095 | `Archive/lotus-pink-nature-flowers-preview.jpg` | none | .17 |
| Pond | `pond` | 152 / .07 | `Archive/photo-1757291684288….avif` | `hue-rotate(76deg) saturate(.8)` | .15 |
| Still | `still` | 272 / .095 | `Archive/lotus-flower.jpg` | `hue-rotate(202deg) saturate(.85)` | .16 |

Non-steel skins derive **every token** in OKLCH from (hue, chroma) on one lightness ladder:
accent = `oklch(56% c h)`, accent-100…900 = 97/93/87/78/56/50/43/35/26 % with chroma factors
.3/.55/.75/.92/1/1/1/.92/.8; bg `97.4% c*.16`, surface `95.2% c*.14`, text `23% c*.25`,
divider `87.5% c*.12`; neutrals 100–900 = 97.6/93.6/87.5/78.5/67/55/45/35/26 % with chroma
.12 (…600–900 use .14). Switcher UI: blueprint box, `SKIN` mono kicker, five 40px buttons
with an 18px gradient chip (steel chip `#b5d9fd→#5980a6 52%→#2f4a66`; others
`oklch(88% c*.75 h)→oklch(56% c h) 52%→oklch(30% c*.8 h)`), Barlow Condensed uppercase
labels, selected = accent fill. Copy: *"Status colours stay put; they carry meaning, not mood."*

→ **App:** dynamic Industry palette (state-backed tokens + OKLCH→sRGB converter, new
`core/ui …/theme/Skin.kt`, rework `Industry.kt`, `DipiTheme.kt`), skin switcher + lotus
toggle in `feature/settings SettingsScreen.kt`, persistence in
`core/datastore SessionStore.kt`, plumbing in `DeskViewModel.kt` / `DipiAppUi.kt`.

### 2. New DC props: `skin` (enum, default `steel`) and `lotus` (boolean, default `true`)
`lotus` gates the page-ground lotus marks and the sign-in photo hero.
→ **App:** two persisted preferences (DataStore; cleared by Erase-all). The app's lotus
toggle gates the sign-in hero **and** the desk watermark — one switch governs the lotus
decoration (the design leaves the desk watermark ungated, but it is the same ornament).

### 3. Runtime script var-ified — light theme follows the skin
`THEME.light` moves from hexes to tokens with three real remaps: `mut` `#5d5d60` →
`neutral-600`, `field` `#ffffff` → `neutral-100`, `fade` → `transparent` (`tint` stays
accent-100, now dynamic). `TONE.Received`/`Reconfirmation` light bg `#eef6ff` →
`accent-100` (follows skin; fg stays `#2c455d`). `SEV.soft` `#7a7a7d` → `neutral-600`
(identical in steel). Hard/safety severities and all other status colours stay fixed
hexes. **`THEME.dark` is untouched — dark mode stays steel.**
→ **App:** `LightDipi` becomes a function of the active palette; `statusColors()`
Received light bg reads the palette; `DarkDipi` unchanged.

### 4. Desk frame (2a) — ambient accent wash + lotus watermark
Inside the 1240×844 frame, under all content: two radial accent washes
(660×400 px at top-right, accent 13%; 420×300 px at bottom-left, accent 9%) and a lotus
mark bottom-left (300 px at left −52 / bottom −64, opacity `sk-op × .7`, per-skin filter,
radial alpha mask solid 42% → transparent 78%). Not animated inside the frame.
→ **App:** underlay in `feature/desk DeskShell.kt` (static — motion stays progress
.25s / toggles .18s only; the 34s `lotusfloat` exists only on the design page ground).

### 5. Phone frame (1a) — ambient wash
One radial accent wash top-right (320×240 at 108%/−4%, accent 14%) behind every phone
screen. → **App:** underlay in `DipiAppUi.kt` root (the desk paints over it).

### 6. Sign-in redesigned
Content bottom-justified (28px sides / 30px bottom, gap 22); when `lotus` is on, a photo
hero fills the top 430px — the skin's lotus photograph fading into the background
(gradient bg-8% → bg-46% at 44% → bg-86% at 74% → solid at 97%, photo `cover` at
`center 36%`). The logo is now the **circular lotus icon**
(`uploads/lotus-android-circular-icon-1024.png`) at 46×46 in an accent blueprint box
(7px padding, 78%-opaque paper fill) — replacing the remote `dipi-logo.gif`. Title
"DIPI Staff" (Barlow Condensed 700 32px) and "Centre admin desk" copy unchanged; fields
52px with 11.5px uppercase labels on `--s-field`; Sign-in button 56px accent blueprint.
→ **App:** rebuild `feature/auth LoginScreen.kt` (keeping remember-me, error, loading —
app features the design omits); bundle downscaled per-skin photos + icon in `core/ui`.

### 7. Asset: lotus circular icon
The rail logo keeps its place (the design rail still shows the old gif; the app's lotus
rail mark is a kept 1.6+ feature) but the asset upgrades to the new 1024px circular icon,
downscaled to 256px. → **App:** shared drawable in `core/ui`, rail (`DeskShell.kt`) +
sign-in use it; `feature/desk/...drawable-nodpi/desk_logo_lotus.png` retired.

### 8. Design-canvas only (no app change)
Page body `#dcdcde` → `#e7e7e9`; floating page-ground lotus marks with `lotusfloat`
34–52s animation; section 3a showcase cards; intro copy now points at 3a.

## Not changed (verified by diff — keep everything as shipped in 1.8.0)
All six desk pane frames, the phone flow frames apart from sign-in, all copy, spacing and
counts logic, `styles.css` tokens, `support.js`, status/severity semantics, audit rules,
calling tracker, photo loading, ID-verification/health display (`SensitiveInfo`).

## Hard-rule / backend notes
- Skin + lotus are device-local UI preferences — no server write exists or is needed;
  stored in plain DataStore (no NPI), wiped by *Erase all local data*.
- No new endpoints anywhere in the revision; `/change-status` remains the only write.
- The AVIF pond photo is converted to JPEG at import (AVIF decodes only on Android 12+;
  the Pixel C is 8.1).
