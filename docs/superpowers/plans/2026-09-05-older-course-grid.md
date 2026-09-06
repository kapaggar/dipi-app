# Four-course Dashboard History Grid Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show the four newest older courses so the wide centre dashboard renders a complete 2×2 older-course grid.

**Architecture:** Change the single repository cap shared by live and mock course loading. Keep the existing parser order and `CentreScreen` two-column chunking; add tests at the repository boundary and wide rendered layout. Release from the current Android app without changing transport or persistence.

**Tech Stack:** Kotlin, Jetpack Compose, Coroutines, Robolectric Compose tests, MockWebServer, Gradle 8.9, Android SDK/ADB, GitHub CLI.

## Global Constraints

- `OLDER_COURSE_LIMIT` is exactly 4; server/parser order is preserved.
- Wide older-course controls render as two rows of two; narrow layout remains a vertical list.
- No new endpoint, parser, sorting rule, persistence field, NPI handling, or status behavior.
- Release is `versionName = "1.42.0"` and `versionCode = 69`.
- GitHub release tag is `v1.42.0`, marked latest, with identical `dipi-staff-1.42.0.apk` and `dipi-staff.apk` assets.

---

### Task 1: Raise the older-course boundary to four

**Files:**
- Modify: `app/src/test/kotlin/org/dhamma/dipi/staff/OlderCourseLimitTest.kt`
- Modify: `app/src/test/kotlin/org/dhamma/dipi/staff/CentreScreenWideTest.kt`
- Modify: `app/src/main/kotlin/org/dhamma/dipi/staff/data/StaffRepository.kt`

**Interfaces:**
- Consumes: `StaffRepository.loadCourses(CentreId): CentreCourses` and `CentreScreen(olderCourses = ...)`.
- Produces: `const val OLDER_COURSE_LIMIT = 4`; `CentreCourses.older` contains at most four items in source order.

- [ ] **Step 1: Write the failing repository test**

Rename `loadCoursesCapsTooManyOlderCoursesAtThree` to `loadCoursesCapsTooManyOlderCoursesAtFour`. For the existing five-item response, assert:

```kotlin
assertEquals(4, courses.older.size)
assertEquals(listOf("5th older", "4th older", "3rd older", "2nd older"), courses.older.map { it.name })
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests org.dhamma.dipi.staff.OlderCourseLimitTest.loadCoursesCapsTooManyOlderCoursesAtFour
```

Expected: FAIL because the current repository returns 3 items.

- [ ] **Step 3: Implement the boundary change**

In `StaffRepository.kt`, change only:

```kotlin
const val OLDER_COURSE_LIMIT = 4
```

Both live and mock branches must continue to call `.take(OLDER_COURSE_LIMIT)`.

- [ ] **Step 4: Add the rendered 2×2 regression test**

In `CentreScreenWideTest`, render four uniquely named older courses at the existing wide qualifier. Read each clickable row's bounds through the text node's parent and assert:

```kotlin
assertEquals(a.top.value, b.top.value, 1f)
assertEquals(c.top.value, d.top.value, 1f)
assertTrue(c.top > a.top)
assertEquals(a.left.value, c.left.value, 1f)
assertEquals(b.left.value, d.left.value, 1f)
```

This pins two columns and two rows without changing Compose production code.

- [ ] **Step 5: Verify GREEN**

Run:

```bash
./gradlew :app:testDebugUnitTest \
  --tests org.dhamma.dipi.staff.OlderCourseLimitTest \
  --tests org.dhamma.dipi.staff.CentreScreenWideTest
```

Expected: BUILD SUCCESSFUL; the short-list test still returns both items.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/org/dhamma/dipi/staff/data/StaffRepository.kt \
  app/src/test/kotlin/org/dhamma/dipi/staff/OlderCourseLimitTest.kt \
  app/src/test/kotlin/org/dhamma/dipi/staff/CentreScreenWideTest.kt
git commit -m "feat: show four older courses on dashboard"
```

### Task 2: Cut Android release 1.42.0

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `AGENTS.md`
- Modify: `CLAUDE.md`
- Modify: `docs/DESIGN.md`

**Interfaces:**
- Consumes: Task 1's four-course behavior.
- Produces: verified Android release 1.42.0/69 and updated shipped ledger.

- [ ] **Step 1: Update release metadata and ledger**

Set:

```kotlin
versionCode = 69
versionName = "1.42.0"
```

Update the current-version lines in `AGENTS.md` and `CLAUDE.md`. Add a `MINOR 1.42.0 / 69` shipped-delta entry to `docs/DESIGN.md` stating that the four newest older courses fill the existing two-column grid as 2×2 and that transport/order are unchanged.

- [ ] **Step 2: Run the full repository suite**

```bash
./gradlew :core:model:test :core:audit:test \
  :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest \
  :app:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Assemble and verify release APK**

```bash
./gradlew :app:assembleRelease
/Users/wizops/Android/Sdk/build-tools/34.0.0/aapt dump badging \
  app/build/outputs/apk/release/app-release.apk
```

Expected package: `org.dhamma.dipi.staff`, `versionCode='69'`, `versionName='1.42.0'`.

- [ ] **Step 4: Install and launch the release APK**

```bash
/Users/wizops/Android/Sdk/platform-tools/adb connect 10.0.0.144:5555
/Users/wizops/Android/Sdk/platform-tools/adb -s 10.0.0.144:5555 install -r -d \
  app/build/outputs/apk/release/app-release.apk
/Users/wizops/Android/Sdk/platform-tools/adb -s 10.0.0.144:5555 shell am start \
  -n org.dhamma.dipi.staff/.MainActivity
```

Expected: install `Success`; `MainActivity` starts.

- [ ] **Step 5: Commit release metadata**

```bash
git add app/build.gradle.kts AGENTS.md CLAUDE.md docs/DESIGN.md
git commit -m "release: bump DIPI Staff to 1.42.0"
```

- [ ] **Step 6: Merge, push, and publish**

After review, fast-forward `main`, rerun the full suite and release assembly on merged `main`, reinstall that exact release APK, and push `main`. Publish `v1.42.0` targeting the merged commit with release notes derived from this spec, mark it latest, and upload byte-identical assets named `dipi-staff-1.42.0.apk` and `dipi-staff.apk`. Verify the GitHub latest API returns `v1.42.0` and both uploaded asset digests match the local APK SHA-256.
