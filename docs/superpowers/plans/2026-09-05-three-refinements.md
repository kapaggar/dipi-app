# Three approved desk refinements implementation plan

> **Execution:** Use the subagent-driven development workflow. Every production change begins with a focused failing test and a recorded RED result. Implementation tasks are sequential because they share the same branch.

**Goal:** Ship truthful cached-roll status, two closed external desk-site handoffs, and token-driven desk error snackbars as DIPI Staff 1.41.0.

**Architecture:** Keep the timestamp correction in `DeskViewModel`; keep external navigation at the app UI boundary with a closed destination type and Android `ACTION_VIEW`; keep snackbar color selection inside `DeskSnackbar` through the existing composition local. Do not add network endpoints, parsers, persistence fields, generic URL routing, or cookie transfer.

**Stack:** Kotlin, Jetpack Compose, Android intents, StateFlow, Robolectric Compose tests, Gradle 8.9.

## Task 1: Stop inventing cached-roll timestamps

**Files:**
- Modify: `app/src/test/kotlin/org/dhamma/dipi/staff/TabletModeTest.kt`
- Modify: `app/src/main/kotlin/org/dhamma/dipi/staff/ui/DeskViewModel.kt`

1. Add `coldProcessCachedRollFallbackDoesNotInventFetchTime`. Seed the encrypted course-ops roll cache, make the teacher-list request fail, pin `sheetClock` to `09:41`, enter Course ops, and assert the restored roll is shown with `teacherRollCachedAt == null`.
2. Run `./gradlew :app:testDebugUnitTest --tests org.dhamma.dipi.staff.TabletModeTest.coldProcessCachedRollFallbackDoesNotInventFetchTime`. Record the expected failure showing the current fallback assigned `09:41`.
3. In the failed-fetch cached-roll branch, set `teacherRollCachedAt = null`. Preserve `sheetClock()` on a successful fetch.
4. Re-run the focused test, then all of `TabletModeTest`; both must pass.
5. Commit the slice as `fix: keep restored roll cache age unknown`.

## Task 2: Read the active snackbar error token

**Files:**
- Modify: `app/src/test/kotlin/org/dhamma/dipi/staff/DeskPanesTest.kt`
- Modify: `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/DeskWidgets.kt`

1. Add a Compose test that supplies a conspicuous `LightDipi.copy(snackError = ...)` through `LocalDipi`, renders `DeskSnackbar(error = true)`, and verifies the rendered background follows the supplied token. Also verify the success branch keeps `Industry.accent800` and the message remains verbatim. Prefer image-pixel behavior over source or constant assertions.
2. Run the focused test and record the expected failure against the fixed `Color(0xFF5A2F2F)` implementation.
3. Change the error branch to `LocalDipi.current.snackError`; leave the success branch unchanged.
4. Re-run the focused test, then `DeskPanesTest`; both must pass.
5. Commit the slice as `fix: use theme token for desk error snackbars`.

## Task 3: Open the two approved desk-site destinations

**Files:**
- Create: `app/src/main/kotlin/org/dhamma/dipi/staff/ui/DeskSiteHandoff.kt`
- Create: `app/src/test/kotlin/org/dhamma/dipi/staff/DeskSiteHandoffTest.kt`
- Modify: `app/src/main/kotlin/org/dhamma/dipi/staff/ui/DipiAppUi.kt`
- Modify: `feature/course/src/main/kotlin/org/dhamma/dipi/staff/course/AdvancedSearchScreen.kt`
- Modify: `feature/course/src/main/kotlin/org/dhamma/dipi/staff/course/CourseHubScreen.kt`
- Modify: `app/src/test/kotlin/org/dhamma/dipi/staff/AdvancedSearchScreenTest.kt`
- Modify: `app/src/test/kotlin/org/dhamma/dipi/staff/CourseHubScreenTest.kt`

1. Add root-level Compose integration tests with a controlled intent resolver/launcher boundary. From `DeskScreen.Search`, click `Full Advanced Search on the desk site ↗` and assert exactly `${BuildConfig.BASE_URL}/search-app/12` opens while the VM stays on Search. From the phone Course Hub overflow, click `Add Application ↗` and assert exactly `${BuildConfig.BASE_URL}/app/add/12/77` opens while the VM stays on CourseHub. Add a failure-path test asserting `No browser can open the desk site` is surfaced locally.
2. Run the new test class and record the expected failures: the current callbacks navigate to `DeskAction` and launch no browser intent.
3. Add a closed `DeskSiteDestination` representation for `AdvancedSearch(centreId)` and `AddApplication(centreId, courseId)`, with literal URL construction from `BuildConfig.BASE_URL.trimEnd('/')`. Add a small launcher that uses un-packaged `Intent.ACTION_VIEW`, catches `ActivityNotFoundException`, and reports failure without cookie or credential handling.
4. Wire Advanced Search and the Course Hub’s sole non-sheet Add Application entry to the closed destinations at `DipiAppUi`. Keep sheet callbacks on `vm.openSheet` and leave unrelated routing alone. Add the trailing `↗` labels in both screens.
5. Re-run the new integration test, `AdvancedSearchScreenTest`, and `CourseHubScreenTest`. Confirm exact URL, no `DeskAction` transition, failure feedback, and unchanged native cached-search behavior.
6. Commit the slice as `feat: open approved desk-site links in browser`.

## Task 4: Release ledger, complete verification, and tablet install

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `AGENTS.md`
- Modify: `CLAUDE.md`
- Modify: `docs/DESIGN.md`
- Modify: `docs/DECISIONS.md` only if the implementation establishes a new owner ruling beyond this approved design

1. Set `versionName = "1.41.0"` and `versionCode = 68`.
2. Add a concise shipped-delta entry to `docs/DESIGN.md`, and update the current-version lines in `AGENTS.md` and `CLAUDE.md`. Record that external handoffs use system browser state and do not transfer app cookies.
3. Run `./gradlew :core:model:test :core:audit:test :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest :app:testDebugUnitTest`.
4. Run `./gradlew :app:assembleDebug :app:assembleRelease` and verify both APKs exist.
5. Connect to `10.0.0.144:5555`, install `app/build/outputs/apk/debug/app-debug.apk` with `-r -d`, and launch `org.dhamma.dipi.staff/.MainActivity`. Capture command results; if the tablet is unreachable, report the exact blocker without claiming install.
6. Commit the release metadata as `release: bump DIPI Staff to 1.41.0`.
7. Generate a final review package from the plan base to HEAD. Run a broad reviewer for requirements, security invariants, tests, and diff quality; address any High or Medium findings and re-run affected checks.
