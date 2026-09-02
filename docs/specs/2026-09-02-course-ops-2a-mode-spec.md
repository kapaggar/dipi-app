# 2a — Tablet mode switch, device PIN, course lock — spec

**Status:** approved for build, 2026-09-02. **Baseline:** `main` @ 1.30.5/51.
**Design:** `docs/design/DIPI-Staff.dc.html` frame 2a; measurements in
`docs/DESIGN.md` § Course ops (2a). Owner decisions: device PIN gate;
read-only mode; course locked to the course whose dates contain today.

## S1 — Mode state

`core/model`: `enum class TabletMode { DESK, COURSE_OPS }`.
`SessionStore`: `stringPreferencesKey("tablet_mode")` with the `skin` pattern —
`setTabletMode(mode)`, `tabletMode: Flow<TabletMode>` defaulting `DESK` via
`fromKey`-style fallback. Wiped by logout and Erase-all (acceptable: logout is
PIN-gated in course ops, see S3). `DeskViewModel`: `mode` on `DeskUiState`,
collected in init like `skin`; `fun setTabletMode(mode: TabletMode)`.

## S2 — Course lock: dates from the course name

`core/model/CourseDates.kt` (pure, tested):
```kotlin
data class CourseWindow(val start: LocalDate, val end: LocalDate)
/** "Dhamma Sudha / 10 Day / 2026 / 2nd-Sep to 13th-Sep" → window; null when unparseable. */
fun parseCourseWindow(name: String, today: LocalDate): CourseWindow?
fun runningCourse(courses: List<Course>, today: LocalDate): Course?
```
Rules (from fixtures in the recon): split on `/`; optional 4-digit year segment
(absent → infer: the year that puts the start within [today-11mo, today+11mo]);
date range `<ord>-<Mon> to <ord>-<Mon>` with `st|nd|rd|th` ordinals and 3-letter
English months; single-date names (`… / 10 Day / 2nd-Sep`) get `end = start`;
a Dec→Jan range rolls the year on the end date; unparseable → null.
`runningCourse` picks the course whose window contains today (first match in
page order); null → course ops shows the empty state "No course is running
today" (no picker, ever). Side benefit: wire `parseCourseWindow` into the
existing `deskDayChip` path so the DAY-0 chip finally fires on the live desk —
optional, only if zero-risk; otherwise note it.

## S3 — Device PIN

New `CourseOpsStore` (`core/datastore`), its OWN EncryptedSharedPreferences
file `dipi_course_ops`: `setPin(pin: String)` stores `sha256(salt + pin)` +
random salt; `checkPin(pin): Boolean`; `clearPin()`. Wiped by Erase-all
(`factoryReset` calls a new `courseOpsStore.wipeAll()`); NOT wiped by logout.
Flows:
- Enabling course ops: if no PIN is set, a dialog collects a 4-digit PIN twice
  (set + confirm) before the mode flips. Numeric keypad text field, 48dp keys
  not required (system IME), CONFIRM disabled until 4 digits match.
- In course ops, **opening Settings prompts for the PIN** (one gate covers the
  mode switch, Logout and Erase-all — otherwise logout would clear the mode key
  and bypass the gate). Wrong PIN → the dialog shakes-free error text
  "Wrong PIN" (fixed severity pair for the text), stays.
- Switching back to Desk ops inside Settings needs no second prompt (already
  gated at the door).
The dialog: `PinDialog(title, onSubmit)` in `:feature:settings`, `LocalDipi`
tokens, never logs or persists the raw digits.

## S4 — Settings card (frame 2a)

`SettingsScreen` gains `TabletModeCard` as the FIRST card (both branches,
`SettingsScreen.kt:134`/`150`), built from `SettingsCard`+`DeskKicker`:
- Kicker `TABLET MODE`; two radio cards per the frame (unselected `#FAFAFB` on
  `#DEDEE1`; selected white on 1.5dp accent + 3dp accent bar + `ON` chip; radio
  22dp ring, 11dp dot; titles DipiCondensed 19sp; copy VERBATIM from
  docs/DESIGN.md 2a). Selection uses `.selectable(role = Role.RadioButton)`,
  single-fire.
- Rule + kicker `WHILE COURSE OPS IS ON` + the four consequence rows verbatim:
  `✓ Teacher list — seniority + seating plan`, `✓ Student card — application,
  read-only`, `— Board, applications, calling, check-in — hidden`,
  `— Exports, rooms & seats, bulk mail — hidden` (48dp rows, values right).
- Right column: dashed "Course being taught" card (course name + parsed window,
  or "No course is running today") and a 48dp static row "Switching back asks
  for the device PIN" (NO switch — the frame's toggle is replaced by the
  always-on gate, owner decision; note the deviation in the report).
- The narrow (<800dp) branch stacks the same content.

## S5 — Navigation swap

`DipiAppUi`: when `state.mode == COURSE_OPS && session != null`:
- Start destination after login/restore = the teacher list route (new
  `DeskScreen.TeacherRoll` and `DeskScreen.SeatingPlan`, `DeskScreen.TeacherCard`
  — add to `deskBack` exhaustively: card → whichever teacher screen opened it,
  screens → TeacherRoll, TeacherRoll → itself).
- NEVER compose: `DeskHost`/`DeskShell` rail, `SyncBannerStrips` queued strip
  (call with `queued = 0` so only the offline strip can show), Centre,
  CourseHub, any desk destination. Test-mode banner may stay.
- A ⚙ affordance (48dp) in the teacher header opens Settings THROUGH the PIN
  prompt (S3). `canBack` guard: TeacherRoll joins Login/Centre as an exit-dialog
  root.
- `afterLogin`/restore in course ops: resolve `runningCourse`, set it as
  `state.course`, land on TeacherRoll; no course → empty-state screen with the
  ⚙ affordance still reachable.

## Tests

- `CourseDatesTest` (core/model, plain JUnit): every fixture format from the
  recon (4-segment, 3-segment no-year, STP, single date, Dec→Jan roll, garbage
  → null); `runningCourse` picks by containment and returns null cleanly.
- `TabletModeTest` (app, Robolectric): default DESK; enabling prompts for PIN
  set; radio cards single-fire; consequence rows verbatim-present.
- `PinGateTest` (app): wrong PIN keeps Settings closed; right PIN opens; PIN
  survives logout (store untouched); Erase-all clears it (then enabling asks to
  set again).
- `CourseOpsNavTest` (app, w1240dp-land): in COURSE_OPS with a seeded running
  course — desk rail absent (`assertDoesNotExist` on rail tags), queued strip
  absent even with queued>0, TeacherRoll is start, ⚙ present ≥48dp.
Never touched: every desk-mode test — the desk build must be byte-identical
when the mode is off (pin this: one test asserts Centre still starts in DESK).
