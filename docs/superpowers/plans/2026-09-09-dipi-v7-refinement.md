# DIPI Staff v7 refinement implementation plan

> **For agentic workers:** Use `superpowers:executing-plans` if available to implement this plan task by task. Use `superpowers:subagent-driven-development` only when the execution session authorizes delegation. If neither skill is installed, follow the checklists directly; installing a skill is not a prerequisite.

**Goal:** Implement the compatible v7 design refinements in the native DIPI Staff Android app, prove the corrected behavior with automated tests and device evidence, and preserve the shipped transport, data, print, and navigation contracts.

**Architecture:** Keep the existing Compose screens, `DeskViewModel`, repositories, and parsers. Put population arithmetic and source-answer classification in pure functions; put temporary navigation context in UI state; keep HTTP, persistence, and printing behind their existing boundaries. Introduce small presentation helpers where several screens need identical facts. This is an incremental refinement, not a web-to-Android rewrite.

**Tech Stack:** Existing Kotlin 2.0.21, Compose BOM 2024.12.01, AGP 8.7.2, Gradle 8.9, Hilt 2.52, Room 2.6.1, DataStore 1.1.1, Retrofit 2.11.0, OkHttp 4.12.0, JUnit 4.13.2, Robolectric 4.14.1. Keep JVM target 17, minSdk 26, compileSdk/targetSdk 35. No dependency upgrade is required by this plan.

**Spec:** The handover package's `design/DIPI Staff v7.dc.html` and `design/DIPI Staff v7 Handoff.dc.html`, reconciled by `DECISIONS_AND_CONFLICTS.md`, `REQUIREMENTS.csv`, and this plan. The package's `source/` is the actual current tracked working tree, including uncommitted 1.46.3 fixes. Paths below are repository-relative for portability; on the original machine the repository is `/Users/wizops/DIPI/dipi-app`.

**Planning baseline:** 2026-09-09. Git HEAD `64db74e549be55b4061004ab1503323a1ecd11f6` is 1.46.1. The working tree is ** 1.46.3 / 97**, with 12 modified tracked files. Do not reset, stash, replace, commit, or attribute those existing edits as part of this work without a reason and the owner's direction. The supplied source snapshot already includes them; do not apply the baseline patch to it again.

## Global constraints

- The request authorizes an implementation handover. This document is a proposed execution specification, not evidence that implementation, tests, installation, or publication have occurred.
- Explicit user instructions and newer owner rulings prevail. Read live `AGENTS.md`, `docs/LIVE-DESK.md`, and the shipped-delta ledger in `docs/DESIGN.md` before implementing. Old design sections and archived prompts can contradict later amendments. The conflict register records the decisions made for this plan; optional policy changes remain excluded by default.
- Build native Compose UI. Never ship the prototype's frame navigation, change-class key, source-case picker, comparison switches, 348dp specification sidebar, synthetic names/counts, or JavaScript runtime in the Android app.
- Live backend PHP is immutable. Preserve existing Drupal HTML transport; no live `/staff/*`, APP API, `/get-app-detail`, or new endpoint. No attendance/status engine. Never send `Approved`. Server messages and status strings stay verbatim.
- Preserve the user-initiated allocation-sync and photo-correction amendments. Do not broaden their request fields, destinations, or authorization. Course ops stays read-only.
- Room occupancy uses `deskRecord` and effective checked-in/historical Attended allocation. **A room string on a non-arrived record is not automatically occupied. Left never occupies.** Finalization comes from worklist fields, not course dates. Finalized course allocation edits/sync stay disabled and Zero Day is skipped.
- No sheet request may include `r`; only existing `SheetSort` query allowlist (`conf`, `seating`) applies. Native seating print never fetches `/seating`. Teacher-list GET can mutate server data: fetch once per entry, never poll, and never refetch on local filter/width/theme changes.
- No new raw identity fields in Room, DataStore, DTOs, logs, test artifacts, or backups. Course-ops health answers remain in the existing encrypted, scoped cache; wipe rules remain course change/logout/erase. UI helpers carrying disclosures must redact `toString` or avoid carrying raw values entirely.
- Keep all six health positions, verbatim answers, original status vocabulary, current group/seniority order, teacher-at-bottom hall, 66dp cells, CW-A1 at the bottom of the vertical furniture rail, backrest top bar, and existing unseated-sevak convention.
- Keep five light skins and **Steel night for every dark skin**. Preserve fixed status/danger colors. Retain Room Chart names at 17sp Medium and muted 12sp age once at reserved top-right. Preserve configured room columns (default four, allowed 1..12); the prototype's seven columns are an example.
- Keep Board 3×3 and current cell order. Course report remains a centre destination with NEW. Valuable stays off Board but available through its existing enum/phone hub. Retired destinations and removed PDFs stay retired.
- Preserve full phone sheet controls and pinch-zoom below 600dp, plus existing native phone seating/print routes. A partial phone prototype is not authority to remove existing phone features.
- Display dimensions in the HTML are design targets, not measured Android output. Account for system bars, font scaling, keyboard, actual available window size, and current Compose APIs.
- Use synthetic fixtures and controlled devices for verification. No real WhatsApp messages, delivery/status writes, photo uploads, or production allocation writes as incidental UI tests. Keep live operational behavior covered with mocks and existing regression tests.
- Never run `./gradlew test` or `:app:test`. Explicit debug test tasks are listed below; feature UI tests belong in `:app`, not new feature-module test source sets.
- Tentative final version is ** 1.47.0 / 98** if 1.46.3 / 97 remains the baseline. This is MINOR because it adds visible controls. Re-read the current version before cutting the build; increment from the actual latest version and code. Do not edit version values during planning.

## Delivery sequence and gates

| Phase | Tasks | Exit evidence |
| ---|---|---|
| Baseline | 01 | Baseline identified; existing edits preserved; focused baseline results recorded |
| Shared correctness | 02–05 | Health classification, arrival/call populations, room scope, and audit evidence tests pass |
| Desk workflow | 06–09 | Board, Check-in, Calling, Audit/Applications, and Room Chart use the same defined facts |
| Course ops | 10 | Card/list/hall layouts and state transitions verified without extra transport |
| Centre, settings, sheets | 11–14 | Accurate dates/freshness, accessible controls, and screen/print separation |
| Integration and acceptance | 15–17 | Full suite, real print PDFs, device matrix, release artifacts, evidence manifest |
| Owner options | 18 | Explicitly excluded unless approved; do not hold the compatible release for them |

Shared dependency order: 01 → 02/03/04/05. Task 06 depends on 02+04; 07 on 04; 08 on 05; 09 on 02+04; 10 on 02+03; 11–12 on 02; 13 on 02; 14 on 02+13. Task 15 integrates all changes; 16–17 close acceptance. These are dependency boundaries, not an instruction to dispatch agents.

## Task 01 - Establish the exact baseline and execution workspace

**Files to read:** `AGENTS.md`, `CLAUDE.md`, `docs/LIVE-DESK.md`, `docs/DESIGN.md`, `docs/DECISIONS.md`, `app/build.gradle.kts`, `gradle/libs.versions.toml`; package `baseline/*`, `DECISIONS_AND_CONFLICTS.md`, and `REQUIREMENTS.csv`.

**Produces:** An execution log, a source selection decision, and baseline test results. Suggested local log: `docs/handovers/dipi-v7-execution-log.md`. Keep private device/test data outside committed documents.

- [ ] Verify package integrity using its `verify_package.py`. Select one source path: existing current checkout (preferred), fresh clone at the recorded commit plus checked baseline patch, or included `source/` snapshot when remote access is unavailable. Never overlay the snapshot on a working checkout blindly.
- [ ] Capture status and build configuration before edits:

```bash
pwd
git status --short
git rev-parse HEAD
rg -n 'versionName|versionCode|compileSdk|minSdk|targetSdk' app/build.gradle.kts
java -version
./gradlew --version
```

- [ ] In the original checkout, compare all 12 pre-existing modified files with `baseline/source-manifest.json`. A mismatch means work progressed since packaging; review the new diff and retain it. Do not force the repository back to this snapshot.
- [ ] If starting a new branch in an appropriate clean execution checkout, use `codex/dipi-v7-refinement`. Do not create a worktree from HEAD alone and lose the uncommitted 1.46.3 work. The snapshot is an alternative source, not a second patch layer.
- [ ] Run the baseline tests most relevant to known behavior before changing it:

```bash
./gradlew :core:model:test :core:audit:test
./gradlew :app:testDebugUnitTest \
  --tests 'org.dhamma.dipi.staff.DeskDeriveTest' \
  --tests 'org.dhamma.dipi.staff.DeskPanesTest' \
  --tests 'org.dhamma.dipi.staff.StudentCardScreenTest' \
  --tests 'org.dhamma.dipi.staff.FinalizedRoomsTest'
```

Record command, exit code, and XML report paths. Do not claim the baseline is green before running it.

- [ ] Read the two v7 documents in a browser. Use the included local runtime and localhost preview command in START_HERE. Review all 17 frames and their notes. Treat numerical examples as fixture illustrations; do not transplant their arithmetic.
- [ ] Start a coverage log keyed by requirement IDs. Mark existing shipped features as “preserved and verified, ” not “implemented” unless actually changed.

## Task 02 - Semantic text colors and reusable touch/layout rules

**Modify:** `core/ui/src/main/kotlin/org/dhamma/dipi/staff/ui/theme/DipiTheme.kt`; affected text consumers in the feature files listed in subsequent tasks.
**Create:** `core/ui/src/main/kotlin/org/dhamma/dipi/staff/ui/theme/ReadableTokens.kt`.
**Tests:** Extend `app/src/test/kotlin/org/dhamma/dipi/staff/DarkTokensTest.kt` and `SkinTest.kt`; create `app/src/test/kotlin/org/dhamma/dipi/staff/ReadableTokensTest.kt`.

**Interface:** `fun readableTokens(palette: IndustryPalette, dark: Boolean): ReadableTokens`. The value contains `primary`, `secondary`, `caption`, `bodyOnCard`, `recordedText`, `recordedFill`, `blankText`, and `blankFill`, all `Color`. Provide it via a new `LocalReadableTokens` composition local in `DipiTheme`; retain existing locals and palette values.

- [ ] Add contrast tests using composited foreground/background colors for each supported light palette and Steel night. Use small prose target 4.5:1, large text 3:1, and meaningful control/focus boundaries 3:1 where they carry the state. Do not infer compliance from a screenshot color name or rounded ratio.
- [ ] Select existing ramp steps for semantic text roles; usually light neutral700/800 and dark muted/foreground are safer than neutral400/500. Verify every selected pair against the actual background used. Do not globally darken a neutral ramp step used for hairlines, and do not copy the handoff's sampled Blossom hexes into every skin.
- [ ] Concrete color calculation for the test helper (use `Double` and compare the unrounded ratio):

```kotlin
fun contrastRatio(foreground: Color, background: Color): Double {
    val fg = foreground.compositeOver(background)
    val a = fg.luminance().toDouble()
    val b = background.luminance().toDouble()
    return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
}
```

Use `androidx.compose.ui.graphics.compositeOver` and `luminance`; test helper inputs must have an opaque final background. Document text roles that use a different surface. Do not make assertions against token names alone.

- [ ] Use `FontFamily.Default` for Android Roboto body text when required by v7. `DipiSans` is Barlow in this repository, not Roboto. Keep bundled `DipiCondensed`/`DipiMono` for their roles. No Google Fonts request at runtime.
- [ ] Standard control pattern: `Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)` around each independent clickable target; adjacent previous/next/Open controls get 12dp separation where space permits. Use `heightIn(min = ...)` for text-bearing rows. Target dimensions do not justify clipping large text.
- [ ] Add explicit action labels to icon buttons and selected-state semantics to filters. Avoid duplicate announcements by having one owner for each click. Decorative lotus has no semantic action. Do not add full health answers to hidden content descriptions or logs.
- [ ] Replace low-contrast prose in touched screens by semantic roles. Keep fixed status colors and danger pairing. S4 acceptance is Steel night with Blossom still remembered for Light.

**Verify:** `./gradlew :app:testDebugUnitTest --tests 'org.dhamma.dipi.staff.ReadableTokensTest' --tests 'org.dhamma.dipi.staff.DarkTokensTest' --tests 'org.dhamma.dipi.staff.SkinTest'`. Save actual foreground/background/ratio pairs in the verification report. Tests must assert rendered-role mappings and palette switching, not simply hardcoded proposed values.

## Task 03 - Health-answer correctness and honest course-history presentation

**Create:** `core/model/src/main/kotlin/org/dhamma/dipi/staff/model/HealthAnswerPresentation.kt` and its test under `core/model/src/test/kotlin/org/dhamma/dipi/staff/model/HealthAnswerPresentationTest.kt`.
**Modify:** `ApplicationCard.kt`, `core/network/src/main/kotlin/org/dhamma/dipi/staff/network/ApplicationViewParser.kt`, `core/datastore/src/main/kotlin/org/dhamma/dipi/staff/datastore/CourseOpsStore.kt`, `feature/teacher/src/main/kotlin/org/dhamma/dipi/staff/teacher/StudentCardScreen.kt`.
**Tests:** `ApplicationCardTest`, `ApplicationViewParserTest`, `CourseOpsStoreTest`, `StudentCardScreenTest`.

**Interfaces:** `enum class HealthAnswerKind { NOT_PROVIDED, EXPLICIT_YES, EXPLICIT_NO, RECORDED, NOT_APPLICABLE }`; `fun healthAnswerKind(row: HealthRow, gender: Gender): HealthAnswerKind`. The classifier returns an enum, not a new raw-answer data class.

- [ ] Write classification tests first. Exact cases and expected results are in verification H01–H08. Reference implementation:

```kotlin
fun healthAnswerKind(row: HealthRow, gender: Gender): HealthAnswerKind {
    if (gender == Gender.M && row.label.equals("Pregnancy", ignoreCase = true)) {
        return HealthAnswerKind.NOT_APPLICABLE
    }
    val value = row.answer.trim()
    return when {
        value.isEmpty() || value == "-" || value == "–" -> HealthAnswerKind.NOT_PROVIDED
        value.equals("Yes", ignoreCase = true) -> HealthAnswerKind.EXPLICIT_YES
        value.equals("No", ignoreCase = true) -> HealthAnswerKind.EXPLICIT_NO
        else -> HealthAnswerKind.RECORDED
    }
}
```

Trim only for classification. Render the original `row.answer` without rewriting punctuation, case, or clinical content. A response starting “Yes - …” is `RECORDED`, not a parsed clinical answer. This deliberately resolves the contradictory prefix-based example in T3.

- [ ] Meaningful unit-test seed:

```kotlin
@Test fun presenceDoesNotMeanYes() {
    assertEquals(HealthAnswerKind.EXPLICIT_NO,
        healthAnswerKind(HealthRow("Medication", "No"), Gender.F))
    assertEquals(HealthAnswerKind.NOT_PROVIDED,
        healthAnswerKind(HealthRow("Physical", ""), Gender.F))
    assertEquals(HealthAnswerKind.RECORDED,
        healthAnswerKind(HealthRow("Physical", "Yes - source narrative"), Gender.F))
    assertEquals(HealthAnswerKind.RECORDED,
        healthAnswerKind(HealthRow("Physical", "No longer taking medication"), Gender.F))
}
```

- [ ] Render positions from `ApplicationCard.HEALTH_ORDER`, matching existing rows by label and providing an empty display row only when absent. Keep exactly six positions. Parser already emits all six; the UI normalization also protects partial cached/test cards.
- [ ] Badge mapping: RECORDED → “Response recorded”; EXPLICIT_YES/NO → “Yes · as supplied” / “No · as supplied”; NOT_PROVIDED → “Not provided”; NOT_APPLICABLE → “Not applicable”. For absent/blank source body use “No answer in the source”; for a dash show `Source value: -` or the exact en-dash. Male Pregnancy gets an explicitly app-authored applicability note, excluded from the answered denominator. If unexpected source text exists there, distinguish and retain it as source text instead of interpreting it.
- [ ] Summary arithmetic: `recorded = RECORDED + EXPLICIT_YES + EXPLICIT_NO`; `notProvided = NOT_PROVIDED`; `notApplicable = NOT_APPLICABLE`; sum always six. Example male empty card: 0 recorded, 5 not provided, 1 not applicable. Handle singular “1 response recorded”. No score, ranking, danger color, or positive/negative clinical conclusion.
- [ ] Keep `flagsFor` and existing neutral presence flags unchanged. Do not reuse this new classifier to change HLTH/MED/INTOX/TECH/PREG/MONK eligibility. The requested badge correction is separate from existing teacher-list flag rules; test this explicitly.
- [ ] Remove fixed 56dp unanswered cards. Header labels/badges wrap; body uses 15sp/1.55, no `maxLines`, ellipsis, nested answer-body scroll, or token that implies severity. Start with 13×15dp padding and radius 7; preserve body indentation/rule only if it fits at 1.3×.
- [ ] Course-history completeness must not be invented. The parser currently converts missing/non-numeric count cells to zero. Add `historyCountsPresent: Set<String>? = null` to `ApplicationCard`: null means legacy/unknown provenance, empty set means no valid source count cells, a set names valid numeric source keys. Parser populates it from valid, nonnegative integer source values in `HISTORY_ORDER`; no new section is parsed.
- [ ] Add the same optional, default-null field to the **private encrypted** `CardDto`; round-trip it. Old encrypted snapshots decode with null. No Room/database schema migration is needed. Explicitly test old-JSON decoding and wipe behavior.
- [ ] Collapse history only when all ten source keys are present and all ten values are zero. Display “No prior courses recorded” plus an expander. On expand show the ten tiles in server order. Otherwise show the existing detail; newly parsed missing keys read “Not provided”, legacy unknown-provenance snapshots retain stored values without claiming completeness. Nonzero history never auto-collapses. Preserve literal Unknown first/last-course values and teacher precedence; remove only app-generated empty punctuation, not source text.

**Verify:** `./gradlew :core:model:test`; `./gradlew :core:network:testDebugUnitTest --tests '*ApplicationViewParserTest'`; `./gradlew :core:datastore:testDebugUnitTest --tests '*CourseOpsStoreTest'`; `./gradlew :app:testDebugUnitTest --tests '*StudentCardScreenTest'`. Assert cache serialization has no newly allowed identity/contact sections. Any new answer-bearing helper must retain redacted logging behavior.

## Task 04 - Define course, arrival, call, and room populations once

**Modify:** `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/DeskDerive.kt`.
**Create:** `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/DeskScope.kt`.
**Tests:** Extend `app/src/test/kotlin/org/dhamma/dipi/staff/DeskDeriveTest.kt`; create `DeskScopeTest.kt` in the same directory. Preserve `FinalizedRoomsTest` and `FinalizedRoomActionsTest`.

**Interfaces to add:**

```kotlin
data class RoomBlockKey(val gender: Gender, val section: String)
data class ArrivalCounts(val roll: Int, val arrived: Int, val pending: Int, val left: Int) {
    val eligible: Int get() = arrived + pending
}
data class RoomAvailability(val block: RoomBlockKey, val total: Int, val occupied: Int) {
    val free: Int get() = total - occupied
}
fun deskIsLeft(card: ApplicantCard): Boolean = card.status.normalize() == "left"
fun deskArrivalCounts(scope: List<ApplicantCard>, records: Map<ApplicantId, CheckInRecord>): ArrivalCounts {
    val left = scope.count(::deskIsLeft)
    val arrived = scope.count { !deskIsLeft(it) && deskCheckedIn(it, records) }
    return ArrivalCounts(scope.size, arrived, scope.size - left - arrived, left)
}
fun deskRoomAvailability(
    fullRoll: List<ApplicantCard>, records: Map<ApplicantId, CheckInRecord>,
    inventory: List<AccoRoom>, block: RoomBlockKey,
): RoomAvailability {
    val codes = inventory.filter { it.gender == block.gender && it.section == block.section }
        .map { it.code }.toSet()
    val occupied = deskOccupied(fullRoll, records).intersect(codes).size
    return RoomAvailability(block, codes.size, occupied)
}
```

Use existing model imports. `RoomBlockKey` is display/session state, not a new server identifier. Inventory codes are opaque exact strings; do not rebuild them with prototype hyphens.

- [ ] Write the R01 fixture tests: four Mbk rooms, two occupied by different seniorities, one pending allocated room, one Left room; free remains two when Old/New/query/status changes. Also test Guest block and a historical finalized course. Assert no negative counts, occupied counted by unique inventory code rather than occupant count, and unknown room strings do not subtract a room from a different block.
- [ ] Separate scopes: full course `deskRoll(state.rows)` feeds room occupancy; list scope uses confirmation-prefix gender/seniority plus search. Arrival status tabs select rows **after** population counts are computed. “To arrive” must not make its own progress denominator one merely because one pending row is shown. Say which scope the progress counts cover.
- [ ] Preserve `deskRoll` membership, including Left and every supported server status; do not change it to only Confirmed/Expected because prototype prose uses that shorthand. Do not infer role or eligibility from display wording.
- [ ] Change `deskRosterRows` filters: Arrived → checked-in and not Left; Left → `deskIsLeft`; All → true; To arrive → not checked-in and not Left. Keep current case-insensitive scan matching and A–Z ordering. Do not silently change to server order.
- [ ] Keep `deskCallList` as all roll applicants with a phone for historical/All presentation. Add `deskCallRound(roll)` that filters that list by `!deskIsLeft`. `deskCallRows(..., "All", search)` reads the full callable list, while outcome segments and To call read `deskCallRound`. Existing outcome vocabulary and stored attempts remain untouched.
- [ ] Make `deskCallCounts` include All separately; never sum All together with the mutually exclusive outcome piles. Board/rail “still to call” use the To-call count from the round. “Left held out” counts callable Left rows, not all Left applicants with no phone. The All tab retains their actual outcome and actual server status.
- [ ] Do not feed a broadened All list into WhatsApp automation. Existing explicit recipient eligibility/selection is a separate path and must not expand as a side effect. Test this in Task 07.
- [ ] Distinct board quantities: onRoll = roll.size; students/sevaks = `ApplicantType`; checkedIn = effective checked-in not Left; eligibleArrivals = arrived+pending; percentage uses an explicitly named denominator. Prefer “13 of 14 eligible arrivals · 1 Left excluded” while the separate roll tile still says 15. At zero denominator show a factual empty state, not NaN/100%.

**Verify:** `./gradlew :app:testDebugUnitTest --tests '*DeskScopeTest' --tests '*DeskDeriveTest' --tests '*FinalizedRoomsTest' --tests '*FinalizedRoomActionsTest'`.

## Task 05 - Add structured audit relationships without inventing evidence

**Modify:** `core/model/src/main/kotlin/org/dhamma/dipi/staff/model/Models.kt` (`AuditFlag`), `core/audit/src/main/kotlin/org/dhamma/dipi/staff/audit/ClientAudit.kt`, `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/DeskDerive.kt`.
**Tests:** Existing `core/audit/src/test` tests for the affected rules; add `AuditRelatedApplicantsTest.kt` in that module's existing test package; extend `DeskDeriveTest.kt`.

**Interface:** Add `val relatedApplicantIds: List<ApplicantId> = emptyList()` at the end of `AuditFlag`. Existing call sites continue to compile. Add `val relatedApplicantIds: List<ApplicantId> = emptyList()` to `DeskFindingPerson`, copied from its flag. No raw comparison values are stored in the relation. No new HTTP or Room fields.

- [ ] Refactor the existing matching functions so the exact match they already use also supplies the related ID. Do not independently reimplement a name/phone/email matcher in Compose. Preserve current active-status rules, severity, rule ordering, detail strings, and `withinFileDuplicate` phone-first/name+DOB fallback behavior.
- [ ] Implement related IDs for `conf_no_duplicate`, `within_file_duplicate`, `shared_mobile`, and `shared_email_unrelated`. Preserve the matches actually supported by the rule; do not attach every applicant mentioned in a narrative. Where the rule already considers a group, retain stable order and unique IDs.
- [ ] `ClientAudit.merge` currently keeps server findings first. When merging the same rule, preserve server label/detail/severity but supplement an empty relationship list from the corresponding client finding. Never replace the server message to make it agree with the client display.
- [ ] Preserve server-first merge while enriching only missing relations. Concrete replacement for the current merge body:

```kotlin
fun merge(client: List<AuditFlag>, server: List<AuditFlag>): List<AuditFlag> =
    (server + client).groupBy { it.ruleId }.values.map { matches ->
        val preferred = matches.first()
        if (preferred.relatedApplicantIds.isNotEmpty()) preferred
        else preferred.copy(relatedApplicantIds =
            matches.firstOrNull { it.relatedApplicantIds.isNotEmpty() }
                ?.relatedApplicantIds.orEmpty())
    }
```

This preserves insertion order and every preferred server field. In each matching rule, use the already-established partner ID when constructing/copying the flag; do not re-parse the detail string.

- [ ] Test real behavior: two active synthetic applicants with the same normalized phone yield each other's IDs; an inactive Duplicate does not become an active comparison partner just because v7 draws one. A server finding with no resolvable relation remains a one-person finding with full evidence text and “Related applicant details unavailable” when comparison context is requested.
- [ ] Cross-course findings cannot fabricate the other record from prose. If no structured ID/source is available, retain the current finding and explain the missing comparison locally. No new cross-course fetch is part of this plan.
- [ ] `deskFindings` retains flagged-person counts. Adding a comparison partner does not double the audit total or inflate “needs attention”. Dedupe displayed partner rows by `ApplicantId`, never name or confirmation number.

**Test seed:** After building two cards with the existing test fixture helper, call `ClientAudit.evaluate(a, listOf(a, b))`, select `ruleId == "within_file_duplicate"`, and assert `relatedApplicantIds == listOf(b.id)`. Change b to server status Duplicate and assert that rule disappears under the existing ACTIVE policy. Then merge a server flag and the client flag and assert both the unchanged server detail and the related ID survive.

**Verify:** `./gradlew :core:audit:test :core:model:test`; `./gradlew :app:testDebugUnitTest --tests '*DeskDeriveTest'`. Add no persistence layer for the relation; it is re-derived from current allowed data.

## Task 06 - Connect defined populations to Board and Check-in

**Modify:** `BoardPane.kt`, `CheckInPane.kt`, `DeskShell.kt` under `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/`; `app/src/main/kotlin/org/dhamma/dipi/staff/ui/DipiAppUi.kt` and `DeskViewModel.kt`.
**Tests:** `BoardPaneTest.kt`, `DeskPanesTest.kt`, `DeskNavTest.kt`, `ZeroDayScreenTest.kt`.

**Consumes:** Task 04 functions. **State addition:** `selectedRoomBlock: RoomBlockKey? = null` in `DeskUiState`; `fun selectRoomBlock(block: RoomBlockKey)` in `DeskViewModel`. Clear or validate it on course/centre changes, inventory changes, logout, and erase. Share this temporary selection between Check-in and Room Chart; it must not change Applications' gender/seniority filters.

- [ ] Change Check-in wiring to pass **both** full course roll and filtered list scope. Use explicit parameter names (`fullRoll`, `listScope`) to make accidental reuse visible. Sidebar receives full roll for occupancy/seating aids and scoped roll only for its labelled applicant table.
- [ ] Compute progress from gender/seniority/search scope before the arrival-status segment. Show visible-row count separately. Keep scan, tabs, scope line, and progress above the scrolling list; give the fact sidebar its own bounded scroll. At insufficient width stack the facts below the list using existing phone navigation, without hidden controls.
- [ ] Add the Left segment and its count. Left rows show server status plus “Excluded from arrivals; holds no room.” No active check-in/allocation action on those rows. All remains inspectable.
- [ ] Render per-block room inventory: “Mbk · Male”, “2 free of 4 rooms”, “2 occupied”. Selected block changes inventory, not applicant filters. Use `RoomLayout.key`/gender+section as the stable key. If no inventory is available, show “Room inventory unavailable”; zero rooms is not unlimited capacity. When no explicit block is selected, either select the first valid matching block or show explicitly named per-block rows; do not imply a combined total belongs to one block.
- [ ] Seating-issued helper counts must use the full course effective records, labelled “Whole course”. Do not substitute the teacher hall's physical grid seated count; those are different sources.
- [ ] Board: replace “arriving today” with “On the roll”; add students+sevaks scope; checked-in tile uses the explicitly named eligible-arrivals denominator; Next uses `pending`; call tile uses round backlog; audit tile distinguishes findings from affected applicants if both counts are shown. Preserve existing Board refresh behavior and all nine tile actions/order.
- [ ] Add actual sheet format labels to Board export tiles using a typed metadata mapping in Task 14. Do not add a refresh button solely because a prototype control is drawn.
- [ ] Representative Compose assertions after rendering the R02 fixture:

```kotlin
rule.onNodeWithText("13 of 14 arrived").assertIsDisplayed()
rule.onNodeWithText("1 Left excluded").assertIsDisplayed()
rule.onNodeWithTag("checkin-tab-left").performClick()
rule.onNodeWithTag("checkin-left-row-15").assertIsDisplayed()
rule.onNodeWithTag("room-availability-M-Mbk").assertTextEquals("2 free of 4 rooms")
```

These are proposed stable tags/copy to add, not claims that the tags already exist. Split text or use text containment only where the actual semantic node contains multiple labels; do not weaken identity assertions.

**Verify:** `./gradlew :app:testDebugUnitTest --tests '*BoardPaneTest' --tests '*DeskPanesTest' --tests '*DeskNavTest' --tests '*ZeroDayScreenTest'`. Prove toggling Old/New, entering a name, and switching status cannot change physical availability. Keep the zero-day parser and allocation-update payload tests green.

## Task 07 - Calling round membership and historical visibility

**Modify:** `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/CallingPane.kt`, `DeskViewModel.kt`, and relevant `DipiAppUi.kt` wiring.
**Tests:** `CallingScreenTest.kt`, `DeskDeriveTest.kt`, `WhatsAppHandoffTest.kt`, existing WhatsApp selection/recovery tests.

**Consumes:** `deskCallList`, `deskCallRound`, `deskCallRows`, `deskCallCounts` from Task 04. No changes to `CallRecord` persistence or status-write endpoints.

- [ ] Add All as a **view** in the existing segment set. It includes phone-bearing Left rows and retained outcomes. To call and every normal outcome segment use the current non-Left round. Counts remain unsearched pile sizes; a separate result count may follow the search query.
- [ ] Add the held-out notice and “Show in All” action when at least one callable Left applicant exists. Preserve server status exactly; “held out” is a separate app explanation. Disabled/absent call-round actions must not erase or reset previous attempts.
- [ ] Let 48dp tabs wrap at smaller widths; do not squeeze text into tiny fixed-height rows. Row name/status/outcome spacing starts at min-height 88 and grows at 1.3×. Keep existing A–Z/priority sort semantics and outcome translations for legacy Reached/Call back.
- [ ] Audit every caller of `deskCallList` with `rg -n 'deskCallList|deskCallRows|deskCallCounts' app feature`. Update callers intentionally. WhatsApp batch preparation must keep its existing recipient selection, managed-letter preview, duplicate-number review, provisioning, and pilot gates. All-view visibility must never auto-select recipients.
- [ ] The core round function is deliberately small and independently testable:

```kotlin
fun deskCallRound(roll: List<ApplicantCard>): List<ApplicantCard> =
    deskCallList(roll).filterNot(::deskIsLeft)
```

In `deskCallRows`, select `deskCallList(roll)` only for the All view, otherwise `deskCallRound(roll)`. Its pile predicate is `filter == "All" || (if (filter == "To call") o.isBlank() else o == filter)`, with existing query matching applied afterwards.

- [ ] UI test fixture: 15 phone-bearing rows, 1 Left, no outcomes. Assert To call=14, All=15, held-out=1; logging one eligible outcome changes backlog to13 but leaves All 15. Removing the Left row's phone changes All 14 and held-out 0; it remains visible in Applications and Check-in All.

**Verify:** `./gradlew :app:testDebugUnitTest --tests '*CallingScreenTest' --tests '*WhatsAppHandoffTest' --tests '*DeskDeriveTest'`. Include the existing full WhatsApp tests in final regression; do not send an actual message as a test.

## Task 08 - Audit evidence layouts and reversible applicant context

**Modify:** `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/AuditPane.kt`, `ApplicationsPane.kt`; `DeskViewModel.kt`, `DipiAppUi.kt`.
**Tests:** `DeskPanesTest.kt`, `DeskNavTest.kt`, `DeskSiteHandoffTest.kt`; create `AuditNavigationContextTest.kt` under `app/src/test/kotlin/org/dhamma/dipi/staff/`.

**New interface/state:** `data class AuditOpenContext(val ruleId: String, val applicantId: ApplicantId)` in `DeskViewModel.kt`; `auditOpenContext: AuditOpenContext?` in `DeskUiState`; `openApplicantFromAudit(card: ApplicantCard, ruleId: String)` and `returnToAudit()` methods. Keep `deskAppPinned` and the existing `deskSelectedApplicant`/`deskApplicationList` helpers. Do not replace the pin with changed global filters.

- [ ] Pass full current `state.rows` as the lookup corpus for known related IDs, not `deskRoll` (which drops Duplicate) and not the filtered Applications list. This allows an already-established relationship to resolve a record without changing audit ACTIVE logic.
- [ ] Layout: findings approximately 470dp at 1280dp, evidence fluid; use independently scrolling lists with finding heading above evidence. Names and evidence wrap; no fixed one-line `offendingValue`, no `take(10)` cap, and no name ellipsis. Full list of ten or more affected people must be reachable. Small screens use the existing navigation pattern or a stacked detail transition; do not force a 470dp pane into a phone.
- [ ] Each evidence subject has name, confirmation number (or a truthful absent state), server status, and an explicit min 96×48 Open. For a known pair/group, compare allowed `ApplicantCard` fields: status, DOB, mobile, email, city, createdAt (Applied on). Empty fields read “Not provided”; no ID numbers/health text are added to comparison DTOs. Preserve all source formatting.
- [ ] A comparison is context, not an adjudication. Do not label a pair “same person”, mark Duplicate, merge records, or suggest status changes automatically. Keep current rule title/evidence.
- [ ] Open records context and calls existing selection with `pinned=true`. Applications shows “Opened from Audit” plus the rule identifier and “Back to Audit”. It explains that this explicit target is visible despite filters. Store no raw evidence in navigation state.
- [ ] Keep the navigation transition explicit rather than mutating the global filters:

```kotlin
fun openApplicantFromAudit(card: ApplicantCard, ruleId: String) {
    _state.update { it.copy(
        deskSection = DeskSection.Applications,
        deskFinding = ruleId,
        auditOpenContext = AuditOpenContext(ruleId, card.id),
    ) }
    selectDeskApp(card, pinned = true)
}
```

This is a `DeskViewModel` member using its existing `_state` and selection method. UI passes the selected finding code and actual row card.

- [ ] Back restores the selected finding and scroll position. Use `rememberSaveable` list state keyed by course+finding, or retained existing list states. On return, if the finding vanished after refresh, show the updated Audit list with “This finding is no longer present”; never reopen a stale applicant implicitly.
- [ ] Clear context and pin coherently when the user selects a normal applicant, deliberately changes relevant filters, changes course/centre, logs out, or erases data. The shipped 1.46.3 filter-change unpin behavior remains. Do not clear the audit pin just because recomposition or browser return occurred.
- [ ] Browser “Edit on desk site” retains separate session routing and the existing once-on-return refresh. After refresh, resolve the same applicant ID; update its fields, retain context when still available, and recompute findings. Do not transfer cookies or assume a successful edit.

**Verify:** For two affected rows A and B, click Open A then Open B and assert each detail's applicant ID, not merely a shared name. Repeat when each is outside current gender/seniority/status scope. Assert no status request during Open/Back. Test stale finding, missing partner, same-name/different-ID, filter-change unpin, browser return, and course change. Run `:app:testDebugUnitTest` filtered to the four named classes.

## Task 09 - Room Chart navigation and readable cells

**Modify:** `RoomsPane.kt`, `DeskViewModel.kt`, `DipiAppUi.kt`; reuse `RoomLayout.kt` unchanged unless a verified bug requires a narrow fix.
**Create:** `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/RoomNavigation.kt`.
**Tests:** `RoomsPaneTest.kt`, `FinalizedRoomsTest.kt`, `RoomSyncTest.kt`; add `RoomNavigationTest.kt` under the app tests.

**Interfaces:** `fun nextOccupiedRoomCode(orderedCodes: List<String>, occupied: Set<String>, current: String?): String?` and `fun resolveRoomJump(rooms: List<AccoRoom>, query: String): AccoRoom?`. Resolve exact current-block `code` or exact unambiguous `displayNo`, case-insensitive trimmed comparison; preserve the original selected code in state. No fuzzy first-match behavior across blocks.

- [ ] Use existing physical block/room ordering and configured columns. Verify `RoomLayout.columnsFor` and the current renderer's order before adding navigation. `Next occupied` advances in **that list**, not numeric sort of the display text. Wrap from last occupied to first; empty set returns null and disables the action. Current not found starts at first occupied.
- [ ] Concrete traversal implementation:

```kotlin
fun nextOccupiedRoomCode(orderedCodes: List<String>, occupied: Set<String>, current: String?): String? {
    if (orderedCodes.isEmpty()) return null
    val start = orderedCodes.indexOf(current)
    for (offset in 1..orderedCodes.size) {
        val code = orderedCodes[(start + offset).mod(orderedCodes.size)]
        if (code in occupied) return code
    }
    return null
}
```

- [ ] Put title/actions, selected block chips, counts, jump field/button, and explanatory legend above the scrolling grid. Reuse Task 06 `selectedRoomBlock`. Changing block clears room focus; returning to a block preserves physical order and does not filter away empties.
- [ ] Jump runs on explicit button/IME action. Invalid/ambiguous input produces an inline “Room not found in this block” state without jumping elsewhere. Bring the selected tile into view with its existing scroll container (`BringIntoViewRequester` or lazy-grid item scroll available in the pinned Compose version), then provide a visible focus ring and semantic selection. Focus alone must not allocate or edit a room.
- [ ] Occupied cells start at 104dp and grow enough for names at 17sp Medium; reserve top-right for one 12sp muted age; room number and amenity marks stay in their own tracks. Empty-only rows stay compact at 44dp, but if they are interactive give them a48dp hit area without overlapping neighbors; otherwise keep them noninteractive. Mixed rows take the occupied row height.
- [ ] Prefer three name lines, word-aware wrapping, and no mid-word break. If three lines cannot show a long name at 1.3×, grow the cell or provide a reachable full-name detail; do not shrink below 17sp. A visual clamp must not hide the only way to identify the occupant.
- [ ] Keep solid Old / dashed New / Available legend, plus explicit words. Do not unify it with hall fills without Task 18 approval. Show block inventory totals, not prototype hardcoded 70/12.
- [ ] Keep Pull/Sync controls, busy-state disabling, per-row server refusals, read-only finalized behavior, and room assignment safety. Never make Next occupied open a sync path. Existing room refresh may remain available if already supported for finalized display, but allocation sync/edit remains disabled.

**Verify:** Physical order `["Mbk 2", "Mbk 10", "Mbk A"]` traverses that exact order with wrap; duplicate display numbers are ambiguous; unknown inventory codes do not create tiles. Screenshot tall names, all-empty rows, mixed rows, multiple blocks and 1.3×. Run the four named app test classes and existing RoomLayout model tests.

## Task 10 - Teacher list, seating plan, and student card as one read-only workflow

**Modify:** `feature/teacher/src/main/kotlin/org/dhamma/dipi/staff/teacher/TeacherListScreen.kt`, `SeatingPlanScreen.kt`, `StudentCardScreen.kt`; minimal `DipiAppUi.kt`/`DeskViewModel.kt` wiring.
**Tests:** `TeacherListScreenTest.kt`, `SeatingPlanScreenTest.kt`, `StudentCardScreenTest.kt`, `CourseOpsNavTest.kt`, `TeacherCardPrefetchTest.kt`, `TeacherIdMappingTest.kt`, `TabletModeTest.kt`, `PinGateTest.kt`.

**Consumes:** Task 02 tokens, Task 03 source classification, existing `TeacherRoll`, `HallPlan`, `TeacherCardRef`, and `SeatKind`. No new teacher transport or enrolment classification.

- [ ] Teacher header labels whole roll and composition when the underlying roll supports it; selected group reads “N in this view · M overall”. Count role tags using existing rules, never confirmation-prefix assumptions that differ from the roll parser. Group order and seniority remain server-derived. Do not add the prototype's invented Monk/Nun group or a roll search box.
- [ ] Keep the existing group-filter empty state. Distinguish no matching rows after successful load from not loaded, failed load, and cached-offline states. “Clear filter” only appears when a real local filter can be cleared; clearing it makes no request. Row names wrap, seat/flags have separate tracks, and metadata may ellipsize only when full data remains available on the card.
- [ ] Hall facts: total rows in the selected hall, old/new, grid occupants, CW/CH occupants, `HallPlan.seatedCount`, `unseated`, and `unseatedVisible`. Count each from its existing source. Display total unseated and an aggregate sevak explanation, plus visible non-sevak rows and their existing source reason (or “No placeable seat label”). Do not invent a reason such as “seat has not been issued” when the label is merely malformed.
- [ ] Derive the explanatory unseated band directly from the same plan used to draw seats:

```kotlin
val unseatedTotal = plan.unseated.size
val unseatedListed = plan.unseatedVisible.size
val unseatedSevaks = unseatedTotal - unseatedListed
val seated = plan.seatedCount
```

Here `plan` is the existing `HallPlan` for the selected hall. Use actual role tags for any broader students/sevaks roll split; never derive a medical or attendance reason from these counts.

- [ ] Preserve axes, 66dp cell height, teacher marker at bottom, CW-A1 nearest teacher, occupied-only vertical rail, existing OLD/NEW fills, dark backrest bar and used-only legend. Use a two-line name where feasible. For longer names at 1.3×, retain fixed hall geometry and full accessible card/name rather than compressing to unreadable text; document this justified difference from expandable Room Chart cells.
- [ ] Keep grid/rail scrolling and fixed header/legend usable together. Pinned bottom axis/teacher/unseated regions cannot consume the entire viewport; if an unseated list grows, bound and scroll its rows while retaining its heading. On phones preserve existing native hall access and panning.
- [ ] Student card: use the same record and classifier whether entered from list or seat. Keep Room/Seat beside the photo, current hidden personal fields, larger OLD/NEW chip, first/most recent teacher precedence, and existing optional GET-only teacher fallback. Do not add contact/language sections drawn by the prototype but disallowed by `ApplicationViewParser`.
- [ ] On wide cards allow left facts/history and right answers to scroll independently with header pinned. At 412dp stack the body and ensure answers are promptly reachable; retain identity and origin above them. Card answer bodies never get their own internal scroll. Scale preview controls are not app UI.
- [ ] Previous/next walk the current group in current order, do not wrap, disable at boundaries, and retain origin. Android Back and visible Back return to the correct list or seating plan with selection/scroll. No extra entry fetch occurs on return from a card.
- [ ] Existing PIN gate persists across logout as designed, clears only on Erase-all, and covers protected Settings routes. Verify empty/incorrect PIN, cancel, successful entry, and no logged digits. Do not implement the prototype's misleading “no raw digits persisted” by destroying the encrypted device credential.

**Verify:** Run the eight named app test classes. Count network requests in prefetch/group/navigation tests: local filtering, hall switching, card Back, layout change, or theme change must not add `/teacher-list` fetches. Preserve source allowlist and `SeatGridTest` behavior. Capture T1/T2/T3 and S2/S3 evidence with synthetic names.

## Task 11 - Centre dashboard and Centre settings layout

**Modify:** `feature/course/src/main/kotlin/org/dhamma/dipi/staff/course/CentreScreen.kt` and `CentreOpsScreen.kt`.
**Tests:** `CentreScreenTest.kt`, `CentreScreenWideTest.kt`, `OlderCourseLimitTest.kt`, `CentreOpsScreenTest.kt` under app tests.

**Interfaces:** Keep existing `onCourseReport`, `onSettings`, `onCentreOps`, `onAdvancedSearch`, and setting callbacks. Reuse current `CentreOpsPrefs`, `HallGrid.clamped`, and room-layout setters; do not add a server settings API.

- [ ] Move Course report (with NEW), App Settings, and Centre Settings into an action row reachable on initial load. Reuse the same callbacks; remove redundant copies of the moved actions from the lower area. Advanced Search keeps its explicit external-browser explanation and existing exact URL handoff.
- [ ] Preserve the current fixed centre header and single below-header scroll on wide screens. The older 60/40 description in early DESIGN sections is superseded by the shipped ledger and current source. Put the action row at the top of that scroll; no independently scrolling Upcoming list. Preserve the bounded many-centre switcher so it cannot consume the whole display.
- [ ] Wide: two columns, sensible 14dp gutters; four newest older entries remain a2×2 grid in current server-derived order. Narrow: one column. Action buttons wrap with min-height 56; at 1280×900 they are all available without scrolling. At 1.3× allow increased height with the course grid still reachable.
- [ ] Add concise matrix legend mapping NM/OM/M/NF/OF/F to New male/Old male/Male total/New female/Old female/Female total. Use current `CourseMatrix` and total semantics; no new arithmetic to force agreement with prototype sample numbers. Do not label Received as arrivals or assume nonzero sevak breakdown when not provided.
- [ ] Centre settings: group each hall's column/depth controls inside its own card. Keep documented clamps (HallGrid 1..26 columns, 1..40 depth; room layout 1..12 columns), update/result sentence, and existing save/apply semantics. Disable minus/plus at bounds and expose “Male hall columns”/“Female hall rows” action labels. Each target 48dp.
- [ ] Check-in option descriptions wrap and state only behavior supported by the current setting. CentreOpsPrefs is presently a device-local preference blob with cached centre room inventory, not a server settings editor and not proven to be independently scoped per centre. Do not introduce per-centre persistence migration or claim server-wide effects as a layout refinement. Room inventory continues to refresh through its read-only route.
- [ ] Concrete layout pattern for a consequence row:

```kotlin
Column(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(14.dp)) {
    Text(title, fontWeight = FontWeight.Medium)
    Text(description, modifier = Modifier.fillMaxWidth())
}
```

Here `title` and `description` refer to the existing row arguments; no fixed-height wrapper or ellipsis. A trailing switch/stepper sits in a separate 48dp target, not on top of wrapped copy.

**Verify:** Existing test callbacks still fire once. Screenshot first viewport with four Upcoming courses, four older courses, long centre names, and many switcher options. Assert no clipped matrix rows and server order unchanged. Run the four named app classes.

## Task 12 - Course report dates, presets, and stable table alignment

**Create:** `core/model/src/main/kotlin/org/dhamma/dipi/staff/model/ReportDateRange.kt` and `core/model/src/test/kotlin/org/dhamma/dipi/staff/model/ReportDateRangeTest.kt`.
**Modify:** `CourseReportScreen.kt`, `DeskViewModel.kt`, `DipiAppUi.kt`.
**Tests:** `CourseReportScreenTest.kt`, existing `CourseReportCsvParserTest.kt`; add `CourseReportRunTest.kt` under app tests using existing repository/mock conventions.

**Interfaces:** `enum class ReportPreset { THIS_MONTH, THIS_YEAR, LAST_12_MONTHS }`; `fun reportRangeError(fromIso: String, toIso: String): String?`; `fun reportPresetRange(preset: ReportPreset, today: LocalDate): Pair<String, String>`. Preserve `CourseReportUi.from/to` ISO state and existing display conversion.

- [ ] Add strict validation without a network call:

```kotlin
fun reportRangeError(fromIso: String, toIso: String): String? {
    val from = runCatching { LocalDate.parse(fromIso) }.getOrNull()
        ?: return "Enter a valid From date (DD-MM-YYYY)."
    val to = runCatching { LocalDate.parse(toIso) }.getOrNull()
        ?: return "Enter a valid To date (DD-MM-YYYY)."
    return if (from > to) "From date must be on or before To date." else null
}
fun reportPresetRange(preset: ReportPreset, today: LocalDate): Pair<String, String> {
    val dates = when (preset) {
        ReportPreset.THIS_MONTH -> today.withDayOfMonth(1) to today.withDayOfMonth(today.lengthOfMonth())
        ReportPreset.THIS_YEAR -> LocalDate.of(today.year, 1, 1) to LocalDate.of(today.year, 12, 31)
        ReportPreset.LAST_12_MONTHS -> today.minusMonths(12).plusDays(1) to today
    }
    return dates.first.toString() to dates.second.toString()
}
```

The presets use device-local calendar dates, not hardcoded IST. The last 12 months definition is inclusive and ends today; show the filled range so it is reviewable. Future dates are not banned by an invented client rule.

- [ ] Guard `runCourseReport()` with this validator as well as disabling the UI Run button while invalid/running. Label fields DD-MM-YYYY; preserve incomplete input as editable, do not normalize it to a valid date or reset it during typing. Keep strict round-trip tests for `parseDeskDate`/`displayDeskDate` with leap day, bad month, and partial text.
- [ ] Presets **fill the two fields but do not run automatically**. This resolves v7's auto-run suggestion against the shipped “RUN is a deliberate act” behavior. Opening the screen still fetches nothing. User taps Run for exactly one existing form scrape+POST.
- [ ] Start with From/To 168×48 and Run 48 high, gaps 12; let labels and presets wrap on phones/1.3×. Place Run immediately after To in reading order. Display specific inline validation under the range, rather than a synthetic empty report.
- [ ] Keep drafts separate from loaded-result identity: `CourseReport.report.from/to` describe the successful request; current editable fields may differ. If edited while a request runs, keep the result labelled with its submitted range and mark draft fields as not yet run when they differ. Reject/discard results belonging to a centre/session that has changed; cancellation or a request token can enforce that without modifying transport.
- [ ] Table numeric headings/body/footer share a single column-width/weight definition. Right-align numbers. Course/teacher text wraps separately. Preserve C/A/TR, student totals and separate sevaks exactly as parsed; keep actual ConductingTeachers/AssistingTeachers names and unknown/empty-range behavior.
- [ ] Keep filter block above a bounded scrolling table and grand-total footer reachable/pinned. On a phone permit a coordinated horizontal table scroll with headers and total aligned. The print/CSV actions use the last successful report, not the edited unrun date strings.

**Test seed:** `assertEquals(null, reportRangeError("2024-02-29", "2024-02-29"))`; assert non-null for `2025-02-29`, blank, `2026-09-30`→`2026-09-01`; assert THIS_MONTH at 2026-09-09 equals 2026-09-01→2026-09-30 and LAST_12_MONTHS equals 2025-09-10→2026-09-09. Spy on repository calls for invalid input, preset tap, first open, repeated Run, centre change, and edit-during-run.

**Verify:** `./gradlew :core:model:test`; `./gradlew :app:testDebugUnitTest --tests '*CourseReportScreenTest' --tests '*CourseReportRunTest'`; `./gradlew :core:network:testDebugUnitTest --tests '*CourseReportCsvParserTest'`. Refusal text remains verbatim and CSV share/print retains teacher names.

## Task 13 - App Settings, freshness, and simulation state

**Create:** `core/model/src/main/kotlin/org/dhamma/dipi/staff/model/FreshnessText.kt` and matching `FreshnessTextTest.kt` under model tests.
**Modify:** `feature/settings/src/main/kotlin/org/dhamma/dipi/staff/settings/SettingsScreen.kt`; `DeskViewModel.kt`, `DipiAppUi.kt`; inspect existing `ConnectivityMonitor.kt` without changing its physical-network definition.
**Tests:** `SettingsScreenTest.kt`, `SimulateOfflineTest.kt`, `SyncBannersTest.kt`, `SessionExpiryTest.kt`.

**Interface:** `fun freshnessText(iso: String?, now: Instant, zone: ZoneId, locale: Locale): String`. Invalid/missing/future timestamps return an honest unknown/clock state. `DeskUiState` gains `simulatedOffline: Boolean = false` and `networkOffline: Boolean = false` while retaining effective `offline` for request gating.

- [ ] Replace Settings' `lastSync ?: "just now"`. Valid ISO timestamps display localized absolute date/time with actual timezone plus a relative age; null/invalid → “Unknown”. Future timestamp → absolute time plus “Device clock differs”, never a negative relative age or “just now”. Keep unit tests supplied with a fixed `now`, zone, locale.
- [ ] The shared formatter must branch before calculating an age:

```kotlin
val stamp = iso?.let { runCatching { Instant.parse(it) }.getOrNull() }
    ?: return "Unknown"
val absolute = DateTimeFormatter.ofPattern("dd MMM uuuu, HH:mm z", locale)
    .withZone(zone).format(stamp)
if (stamp.isAfter(now)) return "$absolute · Device clock differs"
val age = Duration.between(stamp, now)
```

These statements belong inside `freshnessText`; `iso`, `now`, `zone`, and `locale` are its specified arguments. Continue with the thresholds below and return the absolute and relative parts together.

- [ ] Absolute format example: `DateTimeFormatter.ofPattern("dd MMM uuuu, HH:mm z", locale).withZone(zone).format(instant)`. Relative thresholds: <60s “less than a minute ago”, <60min integer minutes, <24h integer hours, otherwise integer days; singular/plural handled. If localized duration resources already exist, reuse them. This is app copy, not a server message.
- [ ] Use the timestamp for the specific data source. `state.lastSync` is existing session/repository successful worklist freshness, not proof that every sheet/report or centre matrix refreshed then. Keep per-sheet/report `fetchedAt`/`ranAt` attached to their own data. Existing `teacherRollCachedAt` is HH: mm only; do not manufacture a date/relative age from it. Either retain that time-only label or add a proper nullable epoch in the existing encrypted roll cache with legacy unknown fallback, only if needed for that display.
- [ ] Settings descriptions use min-height 56 and wrap. Move palette ramps and Simulate offline under collapsed Diagnostics; keep Theme/Skin and a simple preview visible. Diagnostics is a presentation group, not new authorization or a hidden production role. Keep app version and useful session facts visible.
- [ ] Maintain `effectiveOffline = networkOffline || simulatedOffline`. Surface simulation as “Offline simulation is on” with its own clear label; real network loss uses the existing 38dp strip. If both occur, physical offline remains true after simulation is switched off. Keep the existing immediate-on-tap behavior and persistence of the simulation preference.
- [ ] Do not create a queued-change strip in Course ops. Desk queued strip stays 56dp below the offline strip and retains Retry, last-attempt timestamp, actual queued count, and verbatim errors. Diagnostics copy must not imply a simulated connection is physically unavailable.
- [ ] Update official storage copy accurately: encrypted course-scoped health cache and wipe events; device PIN retained across logout and erased by Erase-all; browser sessions separate. Preserve remember-me and erase semantics. No newly exposed PIN, auth tokens, cookie dumps, raw applicant data, or hidden transport details in Diagnostics.

**Verify:** Fixed-clock tests for known/unknown/invalid/future ISO, IST and America/Los_Angeles including DST. State truth table: online+off=false; online+simulation=true; disconnected+off=true; disconnected+simulation=true. Turn simulation off in both physical states. Run model tests and the four named app classes. Verify theme changes do not reset mode, local settings, queue, or PIN.

## Task 14 - Sheet reading widths and honest print-format metadata

**Modify:** `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/SheetViewerPane.kt`, `SheetStylesheet.kt`, `BoardPane.kt`; `core/datastore/src/main/kotlin/org/dhamma/dipi/staff/datastore/SessionStore.kt`; `DeskViewModel.kt` and `DipiAppUi.kt`.
**Create:** `core/model/src/main/kotlin/org/dhamma/dipi/staff/model/SheetPresentation.kt`.
**Tests:** `SheetViewerTest.kt`, `SeatingPrintTest.kt`, `SessionStoreTest.kt`, `SheetRouteSafetyTest.kt`, `SheetTransportSaveTest.kt`; create `SheetPresentationTest.kt` under model tests.

**Interfaces:** `enum class SheetScreenWidth { FIT, READABLE }`; `data class SheetPresentation(val formatLabel: String, val supportsReadingWidth: Boolean)`; `fun sheetPresentation(export: SheetExport): SheetPresentation`. Use the existing enum's actual case names; inspect `SheetExport.kt` before writing exhaustive `when` branches. Metadata applies to existing exports only, including phone-only ones.

**Preference:** Add an allowlisted map of export enum name → width choice in existing device preferences, or a set of names using READABLE. API `setSheetScreenWidth(export: SheetExport, width: SheetScreenWidth)`; invalid/old keys fall back safely. Preference contains no course/applicant IDs, HTML, URLs, filenames, or disclosures. Erase-all clears it. Preserve original logout policy for harmless UI settings; sheet content still clears on logout/expiry.

- [ ] For wide HTML sheet reading, expose Fit width and Readable column. Readable caps the table container at 900dp and centres it; Fit uses available width. Below 900dp both must remain usable, with existing pinch-zoom/horizontal movement. Keep full v5 sort/column/control band on phones; use its existing horizontal scroll, not an overflow-only replacement.
- [ ] Implement width as **screen-only CSS**, not a print adapter attribute and not a server parameter. A safe pattern after identifying the existing wrapper element:

```css
@media screen {
  html.dipi-readable .dipi-sheet-body { max-width: 900px; margin-inline: auto; }
  html.dipi-fit .dipi-sheet-body { max-width: none; width: 100%; }
}
@media print {
  .dipi-sheet-body { max-width: none !important; width: auto !important; margin: 0 !important; }
}
```

Add `dipi-sheet-body` only around the body content in the existing sanitized/wrapped HTML. Verify Android WebView CSS-pixel scaling on Pixel C; 900CSSpx must correspond to intended reading width under the existing viewport, not a shrunken desktop page. Do not run JavaScript to resize sheets; it is disabled by hardening.

- [ ] State survives close/reopen per export. A width toggle does not refetch HTML, reset sort/column selections, invoke print, or mutate a room. Preserve scroll/zoom where the existing WebView update method allows; avoid recreating WebView for a simple width preference change if a screen-only wrapper approach suffices.
- [ ] Add format text near Print and on Board: HTML/native screen with A4 portrait where applicable, Student chit12-up, Checking slip2-up, native seating A4 landscape, Course summary PDF external viewer, Laundry Excel external viewer. No fixed page count. At most say “Page count shown in print preview” until Android's actual layout determines it.
- [ ] Preserve print-only rules, explicit page breaks, landscape attributes, teacher marker and vertical furniture rail. Keep Day 0 `.d0-contact` suppressed in print even if selected on screen. **Do not suppress all contacts in every sheet**; Manager sheet's existing fields remain governed by current rules. Existing other column toggles retain their present screen/print effects.
- [ ] The prototype's spec sidebar stays in documentation. Teacher-list Comments remain excluded from native Course-ops parsing; do not confuse that parser rule with deleting the display-only sheet's allowed HTML content. Keep sheet cache hardening/lifetime and existing transport sorting.
- [ ] Actual printing verification is required separately from Compose assertions; use Task 16. Android HTML printing does not support every browser pagination feature, so do not promise repeated group headings, a single summary page, or exact page counts from CSS alone. Preserve established print outputs and record platform limits.

**Verify:** Unit-map every `SheetExport`; width preference round-trip/default/erase; screen-only class changes do not alter print CSS/output; route tests prove no `r` query. Run `:core:model:test`, filtered `:core:datastore:testDebugUnitTest --tests '*SessionStoreTest'`, filtered network route/save tests, and app SheetViewer/SeatingPrint tests. Phone native hall remains native and no server `/seating` fetch appears.

## Task 15 - Integrate responsive, large-text, and accessibility checks

**Modify:** Only touched screens/shared components where checks reveal an actual defect. No speculative rewrite or extra dependency upgrade.
**Test additions:** Add `V7AccessibilityTest.kt` and `V7ResponsiveTest.kt` under app tests using existing Robolectric/Compose infrastructure. Use native screenshots for visual proof; add semantics/bounds tests for interaction or clipping regressions, not snapshot tests that simply mirror constants.

- [ ] Test widths 412,599,600,900,1280dp, font scales 1.0 and 1.3, primary Blossom light and Steel night. At 599/600 verify the switch in layouts and that no route or action disappears. Capture full primary frame set at 1280dp; use targeted narrow/dark subsets per the verification matrix rather than every possible Cartesian combination.
- [ ] In a Compose test, override density for the content under test using `CompositionLocalProvider(LocalDensity provides Density(base.density, 1.3f))`, where `base` is the existing `LocalDensity.current`. Do not only multiply font sizes in one component; test the same constraint width with genuine font scale.
- [ ] Controls retain readable labels, 48dp non-overlapping targets, selected/disabled semantics, logical focus order, TalkBack labels, and keyboard/IME actions. Test Back behavior, repeated taps, and scrolling to final content. Verify icon glyph scaling does not change their meaning.
- [ ] Use unmerged semantics/bounds where necessary to measure actual targets. For example, after locating a proposed `student-next` tag, assert its unclipped width/height are each at least 48dp and its rectangle does not overlap `student-previous`. Also perform the click and assert correct next ID. Bounds alone do not prove the right action fired.
- [ ] Test long names, a400-character explanation, 2000-character source answer, long email, no photo, all-six-missing, group empty, missing room inventory, 100 evidence rows, large room block, and long settings text. Use only generated records. `V7AccessibilityTest` must not write health content into logs on failure.
- [ ] Check loading, failed fetch, cached-offline, unknown freshness, session expiry, empty filters, zero eligible arrivals, finalized historical view, missing partner, and cancelled print. Prototype unreviewed states inherit existing behavior and get regression coverage, not invented product flows.
- [ ] Keep state through recomposition, rotation/configuration changes as currently supported, ordinary Back, and browser return. Reset scoped transient data on course/centre/session changes. Scan added `rememberSaveable` calls: never save raw identity or health answers there.
- [ ] Use Android accessibility guidance and actual TalkBack behavior. The newer official Compose accessibility-checking API may require a later dependency than this repo; do not add it blindly. Existing semantics assertions plus device TalkBack/manual checks are adequate for this refinement.

**Verify:** Run the two new app test classes plus affected screen classes. Record layout/copy adjustments and remaining limits in the evidence ledger. A rendered HTML design at 1.3× is not proof the native app passed.

## Task 16 - Full regression, APKs, device inspection, and physical print evidence

**Produces:** Green test XML, debug/release APKs with hashes, synthetic screenshots, exported synthetic PDFs and metadata, a requirement evidence ledger with Pass/Fail/Blocked/Not applicable and reason.

- [ ] Run the supported full suite once after integration; repeat only affected checks after later fixes, then final build gates:

```bash
./gradlew :core:model:test :core:audit:test \
  :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest \
  :app:testDebugUnitTest
./gradlew :app:lintDebug
```

`lintDebug` is an additional Android check; record pre-existing failures separately and resolve new findings caused by this change. Do not silently disable lint or change unrelated code. Review HTML/XML reports under each module's `build/reports` and `build/test-results`. A process exit code alone is not a count of tests run; report executed/skipped/failed totals.

- [ ] Re-read versionName/versionCode; before assembling the changed app, set the next unique version, provisionally 1.47.0/98. Update release notes/design ledger for the implemented subset and recorded reconciliations. Do not mark optional policies as shipped.
- [ ] Build with unchanged transport configuration. A mock build must be explicitly segregated from production artifacts, and must not replace the registrar's live package/state as an incidental screenshot method:

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
shasum -a 256 app/build/outputs/apk/debug/app-debug.apk
shasum -a 256 app/build/outputs/apk/release/app-release.apk
```

Confirm the actual APK paths in Gradle output. Release is currently debug-signed; do not represent it as a new signing setup. Verify BASE_URL and mock flag for each artifact; exclude local.properties, keystores and credentials from handover outputs.

- [ ] Use a dedicated emulator/test device or existing synthetic in-process fixtures for mock screenshots and destructive lifecycle tests. `-Pdipi.useMock=true` is available, but it builds the same app package unless deliberately isolated; do not install it over the live registrar tablet and clear its data.
- [ ] The project requires the MINOR registrar build on Pixel C. Verify device serial and APK version before installation; preserve app data with `install -r`. The remembered Wi-Fi endpoint is 10.0.0.144:5555 and may move; use USB serial 5C01001294 to rediscover if needed. Do not hardcode a different detected device.

```bash
/Users/wizops/Android/Sdk/platform-tools/adb devices -l
/Users/wizops/Android/Sdk/platform-tools/adb connect 10.0.0.144:5555
/Users/wizops/Android/Sdk/platform-tools/adb -s 10.0.0.144:5555 shell wm size
/Users/wizops/Android/Sdk/platform-tools/adb -s 10.0.0.144:5555 shell wm density
/Users/wizops/Android/Sdk/platform-tools/adb -s 10.0.0.144:5555 install -r app/build/outputs/apk/debug/app-debug.apk
/Users/wizops/Android/Sdk/platform-tools/adb -s 10.0.0.144:5555 shell am start -n org.dhamma.dipi.staff/.MainActivity
```

Do not use `-d` to downgrade automatically. If a higher version is already installed, reconcile versions first. If the device is unavailable, finish builds/tests and mark device acceptance blocked with the exact unavailable step. Do not call that verified or stop unrelated work.

- [ ] Inspect on-device native layouts against v7 and reconciliations: all 14 main frames plus the three adaptation checks and pertinent dark states. Capture synthetic evidence only. Do not export live applicant screens to a shareable handover; use controlled fixtures for evidence and local-only observation if the live device is needed for compatibility.
- [ ] Print from the **Android print framework** to PDF using synthetic data. Inspect output, not just WebView preview. Required fixtures: 25 chits →3 pages with 12/12/1; 5 checking slips →3 pages with 2/2/1; native two-hall seating → one landscape page per gender for the supported fixture, teacher below row 1, rail vertical, no trailing empty rows/columns; Day 0 contacts suppressed despite screen selection; Manager allowed fields retained; long comments/health text not clipped; course report teacher names and totals present.
- [ ] Toggle Fit/Readable and print the same fixture each way. Compare page count, paper size/orientation, content/order, hidden columns and text completeness. Bytes may differ because PDF timestamps change; compare rendered pages/text/geometry, not raw SHA equality.
- [ ] Test Print while HTML is loading, missing external viewer, cancellation, offline cached sheet, sheet close/logout/session expiry, and selected columns. One unsuccessful export does not justify changing WebView hardening or generating an ad hoc endpoint.
- [ ] Device checks include theme restoration, immediate simulation toggle, wrong/empty PIN, Back from seating card, finalization read-only, and no duplicate teacher-list fetch on local controls. Use mocked request capture for write-prevention assertions, not full production traffic logs.

**Acceptance gate:** No new failures in the supported test suite; all mandatory matrix cases have evidence or a clearly named external blocker. Device/print blockers mean implementation may be code-complete but overall verification is incomplete. Do not fake screenshots or infer actual pages from the design's sample label.

## Task 17 - Review the final diff and prepare the implementation handover

**Modify:** `docs/DESIGN.md` shipped-delta ledger and current release text in `AGENTS.md`/`CLAUDE.md` only as required by implemented release; create a concise implementation report and evidence index. Keep old decisions as history with clear supersession; do not silently rewrite the original design package.

- [ ] Reconcile every row in `REQUIREMENTS.csv`: Implemented with evidence, Preserved with evidence, Excluded optional, Reconciled with linked decision, or Blocked by a specific external dependency. No unexplained blank rows.
- [ ] Compare against the pre-work source snapshot. Preserve all existing 1.46.3 Audit pin and Room Chart name changes and unrelated photo handovers. Do not count them as new v7 work. Review new network/persistence/manifest changes especially carefully.
- [ ] Check for new forbidden routes/params, unsupported status writes, raw NPI logging/persistence, answer `toString` leakage, forced dark Blossom tokens, removed phone controls, blanket print contact suppression, and prototype fake data. Use `rg` as a targeted diff review aid, not as proof of absence by itself.
- [ ] Run `git diff --check`; inspect exact staged/unstaged diff. If making commits is authorized in the execution session, stage only specific requested implementation files and use concise problem-oriented messages. Never `git add .` over user changes; never add Co-Authored-By, generated attribution, session URLs, or other watermarks.
- [ ] Implementation report includes: baseline/working-tree preservation, changes by requirement, effective resolutions, tests with counts/results, APK names/version/hash/mock state, device identity/OS/configuration, actual print evidence, remaining limits, and explicit status of optional policies. Link only sanitized artifacts.
- [ ] GitHub publication is a separate externally visible action; do not infer it from building a handover. If release publication is explicitly authorized later, follow project attachment requirements: versioned APK plus stable `dipi-staff.apk`, mark latest, and verify release download links.
- [ ] Final response to the owner leads with actual outcome and remaining verification gaps. Distinguish “implemented”, “tests passed”, “installed”, and “verified on device”. Do not use one as a substitute for another.

## Task 18 - Optional owner decisions, excluded from the default implementation

These are bounded alternatives, not blocking prerequisites. Complete Tasks 01–17 without waiting for them.

**O1: Mask identity numbers.** Default: preserve existing in-memory display policy. If explicitly approved, change only Applications' display of existing `SensitiveInfo`; last four characters visible, explicit Reveal, automatic re-mask after 20 seconds and on applicant change, route leave, app background, session expiry and erase. Use transient `remember`, never saved state for raw numbers; cancel/reset the timer on lifecycle changes. Tests with synthetic markers cover short/missing values, 20second virtual time, rapid applicant changes, rotation/background, and no persistence/logging. No new identity collection or fetching. Tradeoff: less incidental screen exposure versus one extra verification tap.

**O2: Unify hall and Room Chart OLD/NEW legends.** Default: keep current hall fill convention and Room Chart solid/dashed borders, both explained in words. If owner/teacher explicitly selects unification, document exact old/new/available/backrest styling across hall, rail, rooms and print; validate standing-distance readability and Steel night before changing all consumers. Do not add a registrar-facing experimental legend toggle just because the prototype has a prop.

**O3: Count pre-arrival allocations as occupied.** Default: retain checked-in/historical Attended occupancy. This is a domain policy change, not a fix to filter scope. If later approved, first establish authoritative allocation data for non-arrived applicants, treatment of reserved/duplicate/conflicting rooms, finalized courses, Left, and sync semantics with owner-reviewed server evidence. Do not implement from a room string or screenshot alone.

**O4: Change dark policy, remove phone controls, add roll search or automatic report runs.** Excluded. These are not prerequisites for v7. Any later explicit request needs a separate scoped amendment; do not bundle them into this release as hidden implementation choices.

## Completion definition

The work is complete only when the compatible requirement set is implemented, existing behavior is preserved, mandatory checks have evidence, and the final report states all actual limits. A screenshot match with incorrect populations fails. Correct arithmetic with unreadable/clipped controls fails. Passing JVM tests without the required device/print checks is partial verification, not full acceptance.
