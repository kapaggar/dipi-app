# Theme Honesty Implementation Plan

> **For agentic workers:** Use subagent-driven-development or executing-plans task by task. Astra owns this plan; Terra owns theme infrastructure and tests; Luna owns component token migrations. No commits or live student interactions.

**Goal:** Make Dark render the documented Steel night palette across all six desk panes and Settings, retain the saved Light skin, and fix the four Board caption contrasts.

**Architecture:** Keep `Industry` as the saved Light preference holder. `DipiTheme` provides a separate effective `IndustryPalette` and explicit dark-aware app surface roles through composition locals. Rendered consumers capture those locals; no process-global dark palette mutation.

**Tech Stack:** Kotlin, Compose, existing Robolectric app test source set.

**Spec:** `AGENTS.md`, `docs/DESIGN.md` (Steel night, Settings skin retention), `docs/LIVE-DESK.md`, and ignored `app/build/reports/theme-honesty-2026-09-11/REPORT.md`.

**Status (2026-09-11):** Implementation and verification are complete: all 858 prescribed tests pass (app 463, model 150, audit 31, network 188, datastore 26), debug and release builds succeed, and release 2.0.1 / 99 is installed on Pixel C. The device matrix passed; evidence is in `/Users/wizops/DIPI/dipi-app/app/build/reports/theme-honesty-fixed-2026-09-11/REPORT.md`.

## Constraints

- Fix rendering only. No endpoint, persistence schema, student data, server, status vocabulary or workflow changes.
- Existing Light skin ramps and fills remain byte-for-byte except Board captions; preserve semantic status/severity pairs and white text on filled primary controls.
- Exported HTML/PDF/print paper is intentionally independent; do not recolor document bodies.
- No global `Industry.apply(Steel)` on entering Dark. Existing saved skin must survive Dark, changes while Dark, and return to Light.
- Patch release `2.0.1` / `99` before assembly. Root alone runs Gradle, builds and device verification; implementers do not install or navigate live data.

## Agreed API contract

Terra adds `IndustryPalette.SteelNight` and `fun effectiveIndustry(saved: IndustryPalette, dark: Boolean): IndustryPalette` (returns SteelNight when dark, saved otherwise). `DipiTheme` provides that result as `LocalIndustry`; it continues to derive LightDipi from the saved palette and DarkDipi from its existing constant. Add `LocalDarkTheme` (Boolean, default false) and `LocalDeskSkin` (DeskSkin, default Steel): the latter is Steel when dark and the saved `Industry.skin` otherwise. `Industry.skin`, `Industry.palette`, `Industry.apply` remain saved-state APIs.

SteelNight roles, explicit to prevent interpretation drift:

| Token | Hex |
| --- | --- |
| bg | 14171A |
| surface, neutral100 | 1A1E22 |
| neutral200 | 22272C |
| neutral300 | 2E3339 |
| neutral400 | 3A4046 |
| neutral500 | 6B7278 |
| neutral600 | 9BA1A8 |
| neutral700 | C0C7CD |
| neutral800 | D4D8DC |
| neutral900, text | E4E6E9 |
| accent | 5980A6 |
| accent100 | 1D2D3D |
| accent200 | 22384C |
| accent300 | 2C455D |
| accent400 | 416180 |
| accent500 | 749DC4 |
| accent600 | 94BCE3 |
| accent700, accent800 | B5D9FD |
| accent900 | D6EBFF |

This is a role adapter, not a replacement of DarkDipi's documented ladder; DarkDipi retains its 4A5157 snackbar and fixed severity values. Text-oriented accent700/800 must be pale against dark selected fills. Do not use these for Dipi accentPressed, which stays its existing explicit value.

Add `DeskColors` + `LocalDeskColors`, populated by DipiTheme. Exact fields:

| Field | Light | Dark |
| --- | --- | --- |
| cardFill | lerp(saved.bg, White, .55f) | DarkDipi.field |
| cardBorder | saved.neutral300 alpha .75 | DarkDipi.hairline alpha .75 |
| whiteSurface | FFFFFF | DarkDipi.field |
| subtleSurface | FAFAFB | DarkDipi.field |
| exportTile | FCFCFD | DarkDipi.field |
| modeBorder | DEDEE1 | DarkDipi.hairline |
| rule | E0E0E3 | DarkDipi.hairline |
| strongRule | D4D4D7 | DarkDipi.hairlineStrong |
| keyText | 424244 | DarkDipi.foreground |
| caption | saved.neutral700 | DarkDipi.muted |

`DeskStyle.cardFill` and `.cardBorder` become `@Composable` getters delegating to LocalDeskColors. `Modifier.deskCard(...)` becomes `@Composable`, retaining its parameter names/defaults and visual chain. Read locals and resolve defaults before draw/click lambdas; pure helpers accept Color/IndustryPalette parameters. Do not read locals inside draw scopes or callbacks.

All feature rendering reads `val industry = LocalIndustry.current` in the owning composable, replacing `Industry.<color>` with `industry.<color>`. For composable default arguments use `LocalIndustry.current.<color>` where legal; pure helpers require explicit parameters. Keep only actual saved preference reads (`Industry.skin`, `.palette`, `.apply`) and fixed scrim reads outside this migration. No mutable global effective palette.

## Task 1 - Terra: theme infrastructure, shared rendering and system bars

**Own files:**
- `core/ui/src/main/kotlin/org/dhamma/dipi/staff/ui/theme/Skin.kt`
- `core/ui/src/main/kotlin/org/dhamma/dipi/staff/ui/theme/DipiTheme.kt`
- `core/ui/src/main/kotlin/org/dhamma/dipi/staff/ui/theme/DeskStyle.kt`
- `core/ui/src/main/kotlin/org/dhamma/dipi/staff/ui/theme/Lotus.kt`
- `core/ui/src/main/kotlin/org/dhamma/dipi/staff/ui/theme/Blueprint.kt` only if removing its unused global-color default is necessary
- `app/src/main/kotlin/org/dhamma/dipi/staff/ui/DipiAppUi.kt`
- new `app/src/main/kotlin/org/dhamma/dipi/staff/ui/ThemeSystemBars.kt` if useful
- `app/src/main/kotlin/org/dhamma/dipi/staff/ui/{SyncBanners,CourseOpsHost}.kt` (mechanical rendered-global-read migration only)
- tests listed below; no feature files

- [ ] Implement the exact API above and share completion with Luna. Keep Material dark surface consistent with `colors.field`; foreground and background keep current Dipi roles.
- [ ] Make LotusWatermark's default skin `LocalDeskSkin.current`; migration callers use the effective skin for opacity too.
- [ ] Audit `statusColors`: preserve its pure API or add an optional palette parameter, defaulting to the prior saved palette for compatibility; composable Received/Reconfirmation callsites pass the effective local palette. Fixed pairs remain identical.
- [ ] Inside DipiAppUi's active theme, synchronize `WindowInsetsControllerCompat.isAppearanceLightStatusBars` and `.isAppearanceLightNavigationBars` to `!state.dark` in a SideEffect. Resolve activity safely via LocalView context, skip edit-mode previews; retain existing edge-to-edge setup/insets. App preference, not Android system preference, determines icon contrast. Verify API 27 support using existing AndroidX dependencies.
- [ ] Coordinate the 2.0.1 (99) version bump with the other-features Luna worker.

## Task 2 - Luna: component migration

**Own files:** all affected `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/*.kt`, `feature/settings/src/main/kotlin/org/dhamma/dipi/staff/settings/{SettingsScreen,PinDialog}.kt`, and the teacher Course ops surfaces `feature/teacher/src/main/kotlin/org/dhamma/dipi/staff/teacher/{TeacherListScreen,StudentCardScreen,SeatingPlanScreen}.kt`. Also mechanical direct rendered-global-read replacements only in `feature/course/.../{CentreScreen,CentreOpsScreen,CourseReportScreen}.kt`, `feature/applicants/.../TodaySkeleton.kt`. No core/theme, app UI or test files. Teacher screens require the same effective-local migration; preserve their print HTML and fixed semantic distinctions.

**Three-worker partition:** Luna desk owns only `feature/desk`; Luna other-features owns Settings/PIN, course and applicant files plus `app/build.gradle.kts` (bump 2.0.1 / 99) and the brief shipped-delta documentation in AGENTS.md/docs/DESIGN.md. Terra owns core, app UI and all tests. Each worker follows the steps relevant to its files. Root alone runs Gradle after integration.

- [ ] Migrate DeskShell, DeskWidgets, BoardPane, ApplicationsPane, AuditPane, CallingPane, CheckInPane, RoomsPane, DaySummaryPane and SheetViewerPane rendered color reads to local captures. Sheet viewer chrome follows the theme; its document HTML and print stay unchanged.
- [ ] Capture LocalDeskSkin in DeskShell for watermark opacity; leave Settings saved chip selection based on its `skin` argument.
- [ ] Board's nine `FCFCFD` export fills use `LocalDeskColors.current.exportTile`; four summary notes use `.caption`. Preserve all Light geometry and other colors.
- [ ] Room Chart's `FAFAFB` available fill uses `.subtleSurface`; migrate paired borders/text through the effective palette. Preserve occupancy/old-new treatment.
- [ ] Settings fixed `ModeCardFill/Border/Rule/Dash/KeyText` become corresponding local roles: subtleSurface/modeBorder/rule/strongRule/keyText. Selected mode and course cards use whiteSurface. Replace fixed neutral explanatory text with effective corresponding neutral. PIN dialog panel/slots use subtleSurface/whiteSurface. White primary-control text and deliberate skin swatch colors stay literal.
- [ ] Check other fixed white/pale app fills within the six panes and Settings, including dialogs; use the matching existing-light role. Do not blanket replace every Color literal, semantic badge or export CSS.
- [x] Surface anchors used by tests are the Board stat/export-chip collections, unique `theme-board-caption-0` through `theme-board-caption-3`, `room-cell-free`, Settings inner `mode-desk` and `mode-course-ops` backgrounds, and `theme-settings-course`. Tags identify surfaces; assertions observe rendered modifiers and text layout.

## Task 3 - Terra: regression evidence

**Own tests:** `app/src/test/kotlin/org/dhamma/dipi/staff/SkinTest.kt`, new `ThemeHonestyTest.kt`; extend `DeskPanesTest.kt`/`SettingsScreenTest.kt` only where their fixture helpers simplify coverage.

- [x] Pure palette tests cover every saved skin, dark Steel equality, unchanged Light values, caption contrast >=4.5 against actual cardFill in each mode, fixed status pairs, and dark role consistency.
- [x] Regression suite passes 858 tests: app 463, model 150, audit 31, network 188, datastore 26. Debug and release builds succeed.
- [x] Rendered assertions use modifier inspection plus `TextLayoutResult` style observations, including the Board, Room and Settings anchors. Bitmap/native capture was not used.
- [x] Verified anchor tags are the Board stat/export-chip collections, unique `theme-board-caption-0` through `theme-board-caption-3`, `room-cell-free`, Settings inner `mode-desk` and `mode-course-ops` backgrounds, and `theme-settings-course`.
- [x] Light -> Dark -> saved-skin-change-in-Dark -> Light preserves the newly selected Light palette while Dark remains Steel.
- [ ] Deferred: nonempty student fixture coverage, nested light/dark composition coverage, and Robolectric system-bar appearance flags.

## Integration and acceptance - root

- [ ] Review diff: no saved preference rewrite, no transport/data changes, no handover snapshot edits, no document-body color changes.
- [ ] Run focused theme/UI tests, then repository-prescribed suite:

```bash
./gradlew :core:model:test :core:audit:test :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Never run `./gradlew test` or `:app:test`.

- [x] Pixel C device matrix completed: all 60 desk-pane backgrounds pass (5 skins × Light/Dark × 6 sections), all 10 Settings states were captured and Dark cards visually inspected, and the original Still + Dark state was restored with the original Dhamma Sudha 2–13 Sep Board reopened. Board summary-caption ratios were Steel Light 6.24, Paper Light 7.21, Blossom Light 7.22, Pond Light 7.16, Still Light 7.21, and all Dark 6.43. Evidence: `/Users/wizops/DIPI/dipi-app/app/build/reports/theme-honesty-fixed-2026-09-11/REPORT.md`.
- [x] Final APK: 6,946,211 bytes; SHA-256 `f9e3a6138b9a95fcd35e90e0fc5642ea5154457e5b05c5418f6baee927b24461`.
