# v4 Design Pass — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Recreate the eight v4 frames in the shipped Compose app — dense Board, two-column centre lower pane and Settings, keyboard-aware Login, session-scoped Check-in scan with a clear control, legible matrix cards, an honest Steel-night dark mode, a fixed severity pair, and the 56dp queued strip.

**Architecture:** `version-4/README.md` carries every dp/sp/hex; each task recreates its frame in the existing composables using existing `Industry`/`LocalDipi` tokens — layout and token changes, not new abstractions. One foundation task retunes the dark/severity tokens first because three later frames repaint on them. App-level files (`DipiAppUi`, `DeskViewModel`, `SyncBanners`) are owned by exactly one task per phase.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Robolectric + `createComposeRule`, JUnit4, Gradle 8.9 / JDK 20.

**Spec:** `docs/specs/2026-08-28-v4-design-pass-spec.md` — which adopts `version-4/README.md` as the binding visual spec. **Every implementer reads its own frame's README section in full before coding; the values there are final and are not restated here.**

## Global Constraints

- `version-4/README.md` wins every look-and-measurement argument; existing code wins on architecture. 1 px in the design file = 1 dp; font px = sp.
- **No new product features.** No Day 11 export (spec R2 — it lives on unmerged `feat/desk-gap`), no dashed placeholder rows in the app, no Applications/Rooms NEXT entries, no photo hero, nothing from README's "Do not" list or `version-4/uploads/dipi-ui-export/SHIPPED-DELTA.md`.
- **No client-side ACL; render server responses verbatim. Never persist or log NPI. Backend immutable.**
- **Colours via `Industry`/`LocalDipi` tokens.** The README's hexes for the Steel ramp are the *values of those tokens*, not licences to inline hex in screens. Screens hard-code a hex only where the README declares it fixed-for-all-skins (severity pair, status colours) — and those live in the theme file, not call sites.
- **Fixed severity pair** `#A33A34` light / `#E0796F` dark on the `hard` token (spec R6). `statusColors()` untouched.
- **Touch targets ≥ 48dp.**
- **Test boundary:** only assertions the spec's "Tests this invalidates" section names may be retargeted; the retarget must prove the same behaviour against the new structure. A third-party assertion needing change = STOP and report BLOCKED. Adding tests is always allowed.
- **Test command** (never bare `./gradlew test` — `:app:testReleaseUnitTest` is known-broken for a pre-existing reason):
  ```bash
  ./gradlew :core:model:test :core:audit:test \
            :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest \
            :app:testDebugUnitTest
  ```
- Wide-layout tests use `@Config(qualifiers = "w1240dp-h844dp-land")` (module convention).
- Commits: no `Co-Authored-By`/`Claude-Session` trailers; gpg on.

---

## Multi-agent workflow layout

```
Phase 1  ── T1 tokens ────────────┐   (foundation; three frames repaint on it)
                                  ▼
Phase 2  ── T2 centre ────────────┐
         ── T3 login ─────────────┤   (5 parallel, disjoint files)
         ── T4 check-in ──────────┤
         ── T5 settings ──────────┤
         ── T6 board ─────────────┤
                                  ▼
Phase 3  ── T7 sync + wiring ─────┤   (owns the app-level files)
                                  ▼
Phase 4  ── T8 integrator ────────┘   suite, 1.23.0/36, release, Pixel C
```

| Task | README frame | Owns exclusively |
|---|---|---|
| T1 tokens | 1e ramp, tokens table | `core/ui/.../DipiTheme.kt`; new `app/src/test/.../DarkTokensTest.kt` |
| T2 centre | 1a, 1g(empty), 1h(proof only) | `feature/course/.../CentreScreen.kt`, `app/src/test/.../CentreScreenTest.kt`, `CentreScreenWideTest.kt` |
| T3 login | 1b | `feature/auth/.../LoginScreen.kt`, `app/src/test/.../LoginScreenTest.kt` |
| T4 check-in | 1c | `feature/desk/.../CheckInPane.kt`, `app/.../ui/DeskViewModel.kt` (scan reset only), `app/src/test/.../DeskPanesTest.kt` |
| T5 settings | 1d, 1e | `feature/settings/.../SettingsScreen.kt`, `app/src/test/.../SettingsScreenTest.kt` |
| T6 board | 1f | `feature/desk/.../BoardPane.kt`, new `app/src/test/.../BoardPaneTest.kt` |
| T7 sync + wiring | 1g strips | `app/.../ui/SyncBanners.kt`, `DeskViewModel.kt` (second visit), `DipiAppUi.kt`, `app/src/test/.../SyncBannersTest.kt` |
| T8 integrator | — | `app/build.gradle.kts`, `CLAUDE.md`, `AGENTS.md` |

`DeskViewModel.kt` is touched by T4 (one line) then T7 — different phases, never concurrent. T4 and T6 share `feature/desk/` but not a file; T6 gets a NEW test file so `DeskPanesTest.kt` has one owner (T4).

---

### Task 1 (T1): Steel-night ramp and the fixed severity pair

**Read first:** `version-4/README.md` — "Design tokens" section and frame 1e. Spec R6.

**Files:** Modify `core/ui/src/main/kotlin/org/dhamma/dipi/staff/ui/theme/DipiTheme.kt`. Create `app/src/test/kotlin/org/dhamma/dipi/staff/DarkTokensTest.kt`.

**Interfaces:** `DipiColors`' member list is UNCHANGED — only the values in `DarkDipi` and two values in `lightDipi`/`LightDipi` move. Every consumer keeps compiling untouched.

- [ ] Step 1: failing test — `DarkTokensTest` asserting the night ramp values on `DarkDipi` (`background == Color(0xFF14171A)`, `foreground == Color(0xFFE4E6E9)`, `tint == Color(0xFF1D2D3D)`, `muted == Color(0xFF9BA1A8)`, `hard == Color(0xFFE0796F)`) and the light pair (`lightDipi(IndustryPalette.Steel).hard == Color(0xFFA33A34)`).
- [ ] Step 2: run `./gradlew :app:testDebugUnitTest --tests '*DarkTokensTest*'` — FAIL on current values.
- [ ] Step 3: retune `DarkDipi` to the README's Steel-night ramp — map `hairline`/`hairlineStrong`/`hover`/`field` onto `#1A1E22 #22272C #2E3339 #3A4046` by role (hairline lighter-than-ground rule as today), keep `accent #5980A6`, set `hard = #E0796F`; in the light builder set `hard = #A33A34`. Do NOT touch `statusColors()`, the skin ladder, or any other member.
- [ ] Step 4: targeted test green, then the full suite (some existing test may pin an old dark hex — if one does, it is exactly the assertion the spec authorises; anything unrelated = STOP).
- [ ] Step 5: commit `feat: steel-night dark ramp and the fixed severity pair`.

---

### Task 2 (T2): Centre dashboard — two-column lower pane, matrix legibility, empty reflow

**Read first:** `version-4/README.md` frames **1a, 1g (empty older-courses), 1h**. Spec R4.

**Files:** Modify `feature/course/.../CentreScreen.kt`; retarget only the named assertions in `CentreScreenTest.kt` / `CentreScreenWideTest.kt`, adding coverage freely.

**Interfaces:** `CentreScreen`'s parameter list unchanged. `DeskTiles.kt` unchanged — the split is `action != null` → three 48dp tiles, `action == null` → five 30dp desk-site pill chips with trailing `↗` under a `MORE ON THE DESK SITE` kicker (spec R4). All callbacks (`onCentreOps`/`onAdvancedSearch`/`onSettings`/`onLater`) keep firing exactly as today.

- [ ] Step 1: failing tests — wide config: tiles and chips both reachable, chips fire `onLater` with the same (title, route) pairs as before; matrix header shows MALE/FEMALE group caps AND the six NM/OM/M/NF/OF/F labels; empty older-courses renders the full-width three-across 52dp tile row and no "Older courses" heading.
- [ ] Step 2: red run.
- [ ] Step 3: implement 1a's lower pane (left flexing older-courses column with its own scroll, right 416dp fixed no-scroll desk column), the matrix card upgrades (group caps row, 12sp mono labels with M/F darker, 1dp gutter, subtotal bands behind M and F columns, `·` empty cells, next-course 3dp accent bar on the soonest course only), and 1g's empty reflow. Narrow (<600dp) path keeps its single-scroll structure with the same tile/chip rendering stacked.
- [ ] Step 4: targeted then full suite; `centreSettingsRowIsReachableWithoutCourses` must pass UNMODIFIED.
- [ ] Step 5: commit `feat: two-column centre lower pane and legible matrix cards`.

---

### Task 3 (T3): Login, keyboard-up

**Read first:** `version-4/README.md` frame **1b**. Spec R8.

**Files:** Modify `feature/auth/.../LoginScreen.kt`; `LoginScreenTest.kt` per boundary.

- [ ] Step 1: failing tests — tall card: brand row, both labelled fields, remember-me, SIGN IN, and the "Your centre is read from your account after sign-in." caption all present; error strip shows verbatim server text when error != null. (IME collapse itself is not driveable in Robolectric — structure the composable so the compact arrangement is a pure function of an `imeVisible: Boolean` derived from `WindowInsets.isImeVisible`, and test the compact arrangement by invoking that internal layout with the flag forced true: caption absent, remember-me on the button row.)
- [ ] Step 2: red run.
- [ ] Step 3: implement the 380dp card per 1b — error strip (fixed danger-tinted hexes are IN the README and severity-fixed, use `c.hard`-derived container per README hexes), 40dp fields with 2dp accent focus, 148×44dp SIGN IN, checkbox row; compact variant on IME. Keep `LoginLotusRelief`; no photo.
- [ ] Step 4: targeted then full suite.
- [ ] Step 5: commit `feat: keyboard-aware login card`.

---

### Task 4 (T4): Check-in — session-scoped scan, placeholder, clear control

**Read first:** `version-4/README.md` frame **1c**. Spec R1 — the real cause is ViewModel lifetime; `pickCourse` resets `deskScan = ""` and nothing else. The optional "state 3" strip is NOT built.

**Files:** Modify `feature/desk/.../CheckInPane.kt`; `app/.../ui/DeskViewModel.kt` (the one-line reset in `pickCourse`, ~line 382); `DeskPanesTest.kt` per boundary.

- [ ] Step 1: failing tests — placeholder text renders when scan empty; clear control absent when empty, present with content; tapping it fires `onScan("")`; a VM-level test that `pickCourse` clears a previously-set `deskScan` (pure state assertion via the existing test harness pattern for the VM, or a `deskBack`-style pure function if extraction is cleaner).
- [ ] Step 2: red run.
- [ ] Step 3: implement — 52dp field, scan glyph, placeholder, 48×48dp clear target (32dp circle glyph), focused 2dp accent + mono value; rooms-free panel label/count as separate columns; primary segmented row to 52dp; the `pickCourse` reset.
- [ ] Step 4: targeted (`*DeskPanesTest*`) then full suite.
- [ ] Step 5: commit `feat: session-scoped check-in scan with a clear control`.

---

### Task 5 (T5): Settings — two columns, real controls, honest dark

**Read first:** `version-4/README.md` frames **1d and 1e**. Spec R5.

**Files:** Modify `feature/settings/.../SettingsScreen.kt`; `SettingsScreenTest.kt` per boundary (the `"Theme: …"`/`"Simulate offline: …"` strings are the named-invalidated assertions).

**Interfaces:** existing callbacks unchanged; segmented control calls `onToggleTheme` only when the tapped segment differs from current; offline `Switch` calls `onToggleOffline`. New DEFAULTED param `appVersion: String = ""` — rendered in the ACCOUNT & SESSION card when non-blank; T7 wires the real value. `queued` and `lastSync` params already exist.

- [ ] Step 1: failing tests — segmented control selects per `dark`; tapping the other segment fires the callback once, tapping the current segment does not; offline switch `assertIsOn/Off` + single-fire (row-level `toggleable` + display-only `Switch`, the codebase's established pattern from `CentreOpsScreen`); dark callout text present only when `dark`; Erase-all renders on `c.hard`; skin chips still apply.
- [ ] Step 2: red run.
- [ ] Step 3: implement 1d/1e — two columns at tablet width (left flex, right 428dp; stack below 600dp), APPEARANCE card (segmented theme, skin chips, ramp strips, lotus switch row), TESTING card, ACCOUNT & SESSION card (Log out pill, Erase-all in `c.hard`), dark-mode callout + `APPLIES IN LIGHT` kicker + `SAVED` tag per 1e.
- [ ] Step 4: targeted then full suite.
- [ ] Step 5: commit `feat: two-column settings with real controls and an honest dark mode`.

---

### Task 6 (T6): Board densification

**Read first:** `version-4/README.md` frame **1f**. Spec R2 — NO Day 11 row, dashed or otherwise.

**Files:** Modify `feature/desk/.../BoardPane.kt`; CREATE `app/src/test/.../BoardPaneTest.kt` (do NOT touch `DeskPanesTest.kt` — another worker owns it this phase).

- [ ] Step 1: failing tests in the new file — all twelve export names render, grouped under the three shelf kickers `ROLL SHEETS` / `DESK SLIPS` / `FOR THE TEAM` with the README's grouping; tapping an export chip fires `onExport(label)` unchanged; the four stat cards and three NEXT rows render and fire `onGoto`.
- [ ] Step 2: red run.
- [ ] Step 3: implement — stat cards to 100dp with the 38sp numeral, NEXT rows to 58dp, exports as three labelled shelves of four 40dp chips with leading `↓`. Same data, same callbacks, new geometry.
- [ ] Step 4: targeted then full suite (if an existing `DeskPanesTest` board assertion breaks on geometry, STOP and report — that file is not yours this phase).
- [ ] Step 5: commit `feat: one-fold board with shelved exports`.

---

### Task 7 (T7): Queued strip v2 and wiring

**Read first:** `version-4/README.md` frame **1g** (strips). Spec R7, R5.

**Files:** Modify `app/.../ui/SyncBanners.kt`, `app/.../ui/DeskViewModel.kt` (add `lastSyncAttemptAt` to `DeskUiState`, stamp it in every flush attempt — the reconnect collector's `flush()` and `retrySync()` share one path), `app/.../ui/DipiAppUi.kt` (pass `lastTry` into the strips; pass `BuildConfig.VERSION_NAME` as `appVersion` into `SettingsScreen`); `SyncBannersTest.kt` per boundary.

- [ ] Step 1: failing tests — queued strip renders the mono "last try HH:MM" line when a timestamp is present and omits it when null; RETRY still fires; the four-row offline/queued truth table still passes byte-identical.
- [ ] Step 2: red run.
- [ ] Step 3: implement — offline strip 38dp per 1g; queued strip 56dp on `accent100` with the bordered 48dp RETRY button (1dp `accent400`, radius 5, Barlow Condensed 13.5sp ls 1.8); `lastSyncAttemptAt` stamped at flush entry; formatter "last try HH:MM" from the instant.
- [ ] Step 4: targeted then full suite.
- [ ] Step 5: commit `feat: tappable queued strip with last-try, wired app version`.

---

### Task 8 (T8): Ship 1.23.0

**Files:** `app/build.gradle.kts`, `CLAUDE.md`, `AGENTS.md`.

- [ ] Step 1: guard — `git show feat/desk-gap:app/build.gradle.kts | grep -E "versionCode|versionName"` must still show 1.19.0/30; else STOP.
- [ ] Step 2: bump `35 → 36`, `"1.22.0" → "1.23.0"`.
- [ ] Step 3: docs — CLAUDE.md (~line 5) and AGENTS.md (~line 9) version references to 1.23.0/36 (leave "on `main`" as-is); **AGENTS.md hard rule 9** repointed to `version-4/DIPI Staff v4.dc.html` (+ `version-4/README.md` for measurements) per spec R9.
- [ ] Step 4: full suite green.
- [ ] Step 5: `./gradlew :app:assembleRelease`; report path, bytes, md5.
- [ ] Step 6: install on the Pixel C (`10.0.0.144:5555`), launch, `dumpsys` must report 36/1.23.0, process alive, crash buffer empty. Unreachable device = report, not failure.
- [ ] Step 7: commit `feat: v4 design pass at 1.23.0`.

---

## Self-review notes

**Spec coverage.** PROMPT items 1–2 → T2. 3 → T3. 4 → T4. 5 → T5. 6–7 → T1 (+T5's callout). 8 → T6. 9 → T7 (+T2's empty reflow). R9 → T8. R2/R3 deliberately unimplemented.

**Type consistency.** `DipiColors` members unchanged (T1 values only). `SettingsScreen.appVersion` defaulted in T5, supplied in T7. `deskScan` reset is state-only. No new public types anywhere.

**Known risks.** (1) `DeskViewModel` is touched in phases 2 and 3 — ordered, and T7's brief says re-read the file. (2) T2 is the largest single frame; its test boundary names are in the spec and the stop-rule applies. (3) T3's IME collapse is untestable end-to-end in Robolectric — the plan mandates the testable-flag structure instead of skipping coverage.
