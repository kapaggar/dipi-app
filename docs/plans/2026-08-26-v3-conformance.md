# Version-3 Conformance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the three remaining version-3 design conformance gaps on `main` — the phone sync banners (which currently claim "offline" while online), the Today loading skeleton, and the unstyled centre-settings screen — and drop one unused dependency.

**Architecture:** Each gap follows the codebase's existing split: a pure Kotlin function carrying the logic (testable without Robolectric, mirroring the `deskSyncLine` precedent) plus a thin Compose layer reading Industry tokens so every skin keeps working. One enabling refactor moves `DeskKicker` from `:feature:desk` down into `:core:ui` so `:feature:course` can use it without a feature-to-feature dependency.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Hilt, Robolectric + `createComposeRule` for UI tests, JUnit4 for JVM tests, Gradle 8.9 / JDK 20.

**Spec:** `docs/specs/2026-08-26-v3-conformance-spec.md`

## Global Constraints

- **Design authority:** `version-3/project/DIPI Staff.dc.html` wins every visual argument. Do not delete or edit `version-2/` or `version-3/` — they are the design bundles, not scratch space.
- **No client-side access control.** Send the request; render the server response verbatim. This governs the new Retry control (spec S1.5).
- **Never persist or log NPI** (`aadhar`, `passport`, `voterid`, `pancard`, `ae_*`). Nothing in this plan touches applicant data.
- **Colours come from `Industry` tokens**, never hard-coded hex. The design's hexes are steel-only; the mapping is in spec S2.4.
- **Test command** (the full green suite — never `./gradlew test`, which drags in `:app:testReleaseUnitTest` and fails on missing `ui-test-manifest`):
  ```bash
  ./gradlew :core:model:test :core:audit:test \
            :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest \
            :app:testDebugUnitTest
  ```
- **Do not modify existing tests.** Every test in this plan is new. If an existing test fails, stop and report rather than editing it.
- **SemVer:** the version bump happens once, in Task 7. Do not bump per task.
- Kotlin JVM target 17; `compileSdk`/`targetSdk` 35; `minSdk` 26.

---

## File Structure

| File | Responsibility | Task |
|---|---|---|
| `core/ui/src/main/kotlin/org/dhamma/dipi/staff/ui/theme/DeskKicker.kt` | **Create.** The all-caps mono kicker, moved down from `:feature:desk` so any feature module can use it. | 1 |
| `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/DeskShell.kt` | **Modify.** Delete the local `DeskKicker`; import from `:core:ui`. | 1 |
| `feature/desk/.../{ApplicationsPane,SheetViewerPane,BoardPane,CheckInPane}.kt` | **Modify.** Import update only. | 1 |
| `core/model/src/main/kotlin/org/dhamma/dipi/staff/model/CentreOpsEffect.kt` | **Create.** `centreOpsEffect(prefs)` — the RESULT sentence. Pure, JVM-testable. | 2 |
| `app/src/main/kotlin/org/dhamma/dipi/staff/ui/SyncBanners.kt` | **Create.** `syncBanners()` matrix + the two strip composables. | 3 |
| `app/src/main/res/values/strings.xml` | **Modify.** Replace `offline_banner` with two strings and one plural. | 3 |
| `app/src/main/kotlin/org/dhamma/dipi/staff/ui/DeskViewModel.kt` | **Modify.** Add public `retrySync()`. | 3 |
| `app/src/main/kotlin/org/dhamma/dipi/staff/ui/DipiAppUi.kt` | **Modify.** Swap the single banner for `SyncBannerStrips`. | 3 |
| `feature/applicants/src/main/kotlin/org/dhamma/dipi/staff/applicants/TodaySkeleton.kt` | **Create.** The eight-row loading skeleton. | 4 |
| `feature/applicants/.../TodayScreen.kt` | **Modify.** Call `TodaySkeleton()` instead of the dots. | 4 |
| `feature/course/.../CentreOpsScreen.kt` | **Modify.** Restyle to the design system; render the RESULT block. | 5 |
| `app/build.gradle.kts` | **Modify.** Drop `material3.window` (Task 6); bump version (Task 7). | 6, 7 |
| `app/src/test/kotlin/org/dhamma/dipi/staff/{SyncBannersTest,TodaySkeletonTest,CentreOpsScreenTest}.kt` | **Create.** UI + logic tests. | 3, 4, 5 |
| `core/model/src/test/kotlin/org/dhamma/dipi/staff/model/CentreOpsEffectTest.kt` | **Create.** The four spec vectors. | 2 |

---

### Task 1: Move `DeskKicker` into `:core:ui`

Pure refactor with no behaviour change. It exists so Task 5 can style
`CentreOpsScreen` (in `:feature:course`, which depends only on `:core:ui`)
without adding a `:feature:desk` dependency. Spec S3.5.

**Files:**
- Create: `core/ui/src/main/kotlin/org/dhamma/dipi/staff/ui/theme/DeskKicker.kt`
- Modify: `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/DeskShell.kt:243-255` (delete the function)
- Modify: `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/ApplicationsPane.kt` (add import)
- Modify: `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/SheetViewerPane.kt` (add import)
- Modify: `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/BoardPane.kt` (add import)
- Modify: `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/CheckInPane.kt` (add import)

**Interfaces:**
- Consumes: `DipiMono` from `org.dhamma.dipi.staff.ui.theme` (already in `:core:ui`).
- Produces: `@Composable fun DeskKicker(text: String, color: Color, modifier: Modifier = Modifier)` in package `org.dhamma.dipi.staff.ui.theme`. Task 5 depends on this exact signature and package.

- [ ] **Step 1: Create the moved file**

Create `core/ui/src/main/kotlin/org/dhamma/dipi/staff/ui/theme/DeskKicker.kt`:

```kotlin
package org.dhamma.dipi.staff.ui.theme

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** IBM Plex Mono 600 / 9.5sp / .16em kicker — the system's all-caps label. */
@Composable
fun DeskKicker(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        fontFamily = DipiMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 9.5.sp,
        letterSpacing = 0.16.em,
        color = color,
        modifier = modifier,
    )
}
```

- [ ] **Step 2: Delete the old definition**

In `feature/desk/.../DeskShell.kt`, delete these lines exactly (the doc comment
and the whole function):

```kotlin
/** IBM Plex Mono 600 / 9.5sp / .16em kicker — the system's all-caps label. */
@Composable
fun DeskKicker(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        fontFamily = DipiMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 9.5.sp,
        letterSpacing = 0.16.em,
        color = color,
        modifier = modifier,
    )
}
```

- [ ] **Step 3: Add the import to all five consumers**

In each of `DeskShell.kt`, `ApplicationsPane.kt`, `SheetViewerPane.kt`,
`BoardPane.kt`, `CheckInPane.kt`, add this import line (keep imports
alphabetically sorted — it sits just before `import org.dhamma.dipi.staff.ui.theme.DeskStyle`
where that already exists):

```kotlin
import org.dhamma.dipi.staff.ui.theme.DeskKicker
```

- [ ] **Step 4: Compile and run the full suite**

Run:
```bash
./gradlew :core:model:test :core:audit:test \
          :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest \
          :app:testDebugUnitTest
```
Expected: PASS, exit 0. This is a pure move — if anything fails, an import is
missing or `DeskShell.kt` now has an unused `DipiMono`/`FontWeight`/`em`
import. Remove only imports that the compiler reports as unused.

- [ ] **Step 5: Commit**

```bash
git add core/ui/src/main/kotlin/org/dhamma/dipi/staff/ui/theme/DeskKicker.kt \
        feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/
git commit -m "refactor: move DeskKicker into core:ui so feature modules can share it"
```

---

### Task 2: `centreOpsEffect` — the RESULT sentence

The pure function behind the centre-settings RESULT block. Spec S3.4.

**Files:**
- Create: `core/model/src/main/kotlin/org/dhamma/dipi/staff/model/CentreOpsEffect.kt`
- Test: `core/model/src/test/kotlin/org/dhamma/dipi/staff/model/CentreOpsEffectTest.kt`

**Interfaces:**
- Consumes: `CentreOpsPrefs(laundry: Boolean, valuables: Boolean, groups: Boolean, rooms: List<AccoRoom>)` and `MAIN_DHAMMA_HALL`, both already in `org.dhamma.dipi.staff.model` (`Models.kt:217` and `:225`).
- Produces: `fun centreOpsEffect(prefs: CentreOpsPrefs): String`. Task 5 renders this string.

- [ ] **Step 1: Write the failing test**

Create `core/model/src/test/kotlin/org/dhamma/dipi/staff/model/CentreOpsEffectTest.kt`:

```kotlin
package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CentreOpsEffectTest {

    @Test
    fun allThreeOnListsEveryQuestionAndDropsTheHallSentence() {
        val prefs = CentreOpsPrefs(laundry = true, valuables = true, groups = true)
        assertEquals(
            "Check-in asks for room, seating, laundry, valuables and group.",
            centreOpsEffect(prefs),
        )
    }

    @Test
    fun groupsOffAddsTheHallSentence() {
        val prefs = CentreOpsPrefs(laundry = true, valuables = true, groups = false)
        assertEquals(
            "Check-in asks for room, seating, laundry and valuables. " +
                "Everyone sits in Main Dhamma Hall and Zero Day hides group chips.",
            centreOpsEffect(prefs),
        )
    }

    @Test
    fun allOffStillAsksRoomAndSeating() {
        val prefs = CentreOpsPrefs(laundry = false, valuables = false, groups = false)
        assertEquals(
            "Check-in asks for room and seating. " +
                "Everyone sits in Main Dhamma Hall and Zero Day hides group chips.",
            centreOpsEffect(prefs),
        )
    }

    @Test
    fun laundryOffKeepsTheRestInOrder() {
        val prefs = CentreOpsPrefs(laundry = false, valuables = true, groups = true)
        assertEquals(
            "Check-in asks for room, seating, valuables and group.",
            centreOpsEffect(prefs),
        )
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:model:test --tests '*CentreOpsEffectTest*'`
Expected: FAIL — compilation error, `Unresolved reference: centreOpsEffect`.

- [ ] **Step 3: Write the implementation**

Create `core/model/src/main/kotlin/org/dhamma/dipi/staff/model/CentreOpsEffect.kt`:

```kotlin
package org.dhamma.dipi.staff.model

/**
 * The centre-settings RESULT line: what check-in will ask for, given the three
 * switches. Room and seating are unconditional (CheckInPane renders ROOM and
 * SEATING always); the switches only append. See
 * docs/specs/2026-08-26-v3-conformance-spec.md S3.4.
 */
fun centreOpsEffect(prefs: CentreOpsPrefs): String {
    val asks = buildList {
        add("room")
        add("seating")
        if (prefs.laundry) add("laundry")
        if (prefs.valuables) add("valuables")
        if (prefs.groups) add("group")
    }
    val list = asks.dropLast(1).joinToString(", ") + " and " + asks.last()
    val head = "Check-in asks for $list."
    return if (prefs.groups) {
        head
    } else {
        "$head Everyone sits in $MAIN_DHAMMA_HALL and Zero Day hides group chips."
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:model:test --tests '*CentreOpsEffectTest*'`
Expected: PASS, 4 tests.

- [ ] **Step 5: Commit**

```bash
git add core/model/src/main/kotlin/org/dhamma/dipi/staff/model/CentreOpsEffect.kt \
        core/model/src/test/kotlin/org/dhamma/dipi/staff/model/CentreOpsEffectTest.kt
git commit -m "feat: derive the centre-settings RESULT sentence from the three switches"
```

---

### Task 3: Phone sync banners — split the strips and add Retry

Fixes the defect where an online device with queued changes reports itself
offline. Spec S1.

**Files:**
- Create: `app/src/main/kotlin/org/dhamma/dipi/staff/ui/SyncBanners.kt`
- Modify: `app/src/main/res/values/strings.xml:4` (replace `offline_banner`)
- Modify: `app/src/main/kotlin/org/dhamma/dipi/staff/ui/DeskViewModel.kt` (add `retrySync()`)
- Modify: `app/src/main/kotlin/org/dhamma/dipi/staff/ui/DipiAppUi.kt:131-140`
- Test: `app/src/test/kotlin/org/dhamma/dipi/staff/SyncBannersTest.kt`

**Interfaces:**
- Consumes: `Industry.neutral200/neutral300/neutral700/accent100/accent300/accent700/accent800` and `DipiMono` from `:core:ui`; `DeskViewModel.flush()` (private, `DeskViewModel.kt:1217`).
- Produces:
  - `sealed interface SyncBanner` with `data object Offline` and `data class Queued(val count: Int)`
  - `fun syncBanners(offline: Boolean, queued: Int): List<SyncBanner>`
  - `@Composable fun SyncBannerStrips(offline: Boolean, queued: Int, onRetry: () -> Unit)`
  - `fun DeskViewModel.retrySync()` (public member)

- [ ] **Step 1: Write the failing test**

Create `app/src/test/kotlin/org/dhamma/dipi/staff/SyncBannersTest.kt`:

```kotlin
package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.dhamma.dipi.staff.ui.SyncBanner
import org.dhamma.dipi.staff.ui.SyncBannerStrips
import org.dhamma.dipi.staff.ui.syncBanners
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncBannersTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun quietWhenOnlineAndNothingQueued() {
        assertEquals(emptyList<SyncBanner>(), syncBanners(offline = false, queued = 0))
    }

    @Test
    fun offlineAloneShowsOnlyTheOfflineStrip() {
        assertEquals(listOf(SyncBanner.Offline), syncBanners(offline = true, queued = 0))
    }

    @Test
    fun queuedWhileOnlineNeverClaimsOffline() {
        assertEquals(listOf(SyncBanner.Queued(2)), syncBanners(offline = false, queued = 2))
    }

    @Test
    fun offlineAndQueuedShowsOfflineFirst() {
        assertEquals(
            listOf(SyncBanner.Offline, SyncBanner.Queued(3)),
            syncBanners(offline = true, queued = 3),
        )
    }

    @Test
    fun onlineWithQueuedRendersCountAndRetryButNotOfflineCopy() {
        rule.setContent {
            DipiTheme { SyncBannerStrips(offline = false, queued = 2, onRetry = {}) }
        }
        rule.onNodeWithTag("queued-strip").assertIsDisplayed()
        rule.onNodeWithText("changes waiting to sync").assertIsDisplayed()
        rule.onNodeWithText("RETRY").assertIsDisplayed()
        rule.onAllNodesWithTag("offline-strip").assertCountEquals(0)
    }

    @Test
    fun singularCopyForOneQueuedChange() {
        rule.setContent {
            DipiTheme { SyncBannerStrips(offline = false, queued = 1, onRetry = {}) }
        }
        rule.onNodeWithText("change waiting to sync").assertIsDisplayed()
    }

    @Test
    fun retryFiresTheCallback() {
        var retried = false
        rule.setContent {
            DipiTheme { SyncBannerStrips(offline = false, queued = 2, onRetry = { retried = true }) }
        }
        rule.onNodeWithText("RETRY").performClick()
        assertTrue(retried)
    }
}
```

Add these two imports to the test file as well (they are used by
`onAllNodesWithTag`/`assertCountEquals`):

```kotlin
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests '*SyncBannersTest*'`
Expected: FAIL — `Unresolved reference: syncBanners`.

- [ ] **Step 3: Replace the strings**

In `app/src/main/res/values/strings.xml`, delete this line:

```xml
<string name="offline_banner">◍ Offline — showing cached list · %1$d changes waiting to sync</string>
```

and add, inside `<resources>`:

```xml
<string name="offline_strip">◍ Offline — showing cached list</string>
<string name="retry_sync">RETRY</string>
<plurals name="changes_waiting">
    <item quantity="one">change waiting to sync</item>
    <item quantity="other">changes waiting to sync</item>
</plurals>
```

The count is drawn separately in mono (spec S1.4), so the plural carries no
`%d` — `pluralStringResource` still selects on the count.

- [ ] **Step 4: Write the implementation**

Create `app/src/main/kotlin/org/dhamma/dipi/staff/ui/SyncBanners.kt`:

```kotlin
package org.dhamma.dipi.staff.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.R
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.Industry

/** One sync strip on the phone shell. */
sealed interface SyncBanner {
    data object Offline : SyncBanner
    data class Queued(val count: Int) : SyncBanner
}

/**
 * Which strips the phone shell shows. Offline and queued are independent —
 * an online device with queued rows must never claim to be offline. See
 * docs/specs/2026-08-26-v3-conformance-spec.md S1.2.
 */
fun syncBanners(offline: Boolean, queued: Int): List<SyncBanner> = buildList {
    if (offline) add(SyncBanner.Offline)
    if (queued > 0) add(SyncBanner.Queued(queued))
}

@Composable
fun SyncBannerStrips(offline: Boolean, queued: Int, onRetry: () -> Unit) {
    for (banner in syncBanners(offline, queued)) {
        when (banner) {
            SyncBanner.Offline -> OfflineStrip()
            is SyncBanner.Queued -> QueuedStrip(banner.count, onRetry)
        }
    }
}

@Composable
private fun OfflineStrip() {
    Column(Modifier.fillMaxWidth().testTag("offline-strip")) {
        Text(
            text = stringResource(R.string.offline_strip),
            color = Industry.neutral700,
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .background(Industry.neutral200)
                .padding(horizontal = 16.dp, vertical = 7.dp),
        )
        HorizontalDivider(thickness = 1.dp, color = Industry.neutral300)
    }
}

@Composable
private fun QueuedStrip(count: Int, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("queued-strip")) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Industry.accent100)
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                count.toString(),
                fontFamily = DipiMono,
                fontSize = 11.sp,
                color = Industry.accent800,
            )
            Text(
                pluralStringResource(R.plurals.changes_waiting, count),
                fontSize = 12.5.sp,
                color = Industry.accent800,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp),
            )
            Text(
                stringResource(R.string.retry_sync),
                fontSize = 11.5.sp,
                letterSpacing = 0.06.em,
                color = Industry.accent700,
                modifier = Modifier
                    .clickable(onClick = onRetry)
                    .padding(start = 8.dp)
                    .testTag("retry-sync"),
            )
        }
        HorizontalDivider(thickness = 1.dp, color = Industry.accent300)
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests '*SyncBannersTest*'`
Expected: PASS, 7 tests.

- [ ] **Step 6: Add the public retry to the ViewModel**

In `app/src/main/kotlin/org/dhamma/dipi/staff/ui/DeskViewModel.kt`, directly
above the existing `private suspend fun flush()` (line 1217), add:

```kotlin
/**
 * User-initiated outbox retry from the queued strip. Always attempts the
 * send — no client-side reachability gate (hard rule 1); failures surface
 * through the existing FlushSnack path.
 */
fun retrySync() {
    viewModelScope.launch { flush() }
}
```

- [ ] **Step 7: Wire the strips into the shell**

In `app/src/main/kotlin/org/dhamma/dipi/staff/ui/DipiAppUi.kt`, replace this
block (lines 131-140):

```kotlin
                if (state.offline || state.queuedCount > 0) {
                    Text(
                        text = stringResource(R.string.offline_banner, state.queuedCount),
                        color = c.foreground,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(c.tint)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
```

with:

```kotlin
                SyncBannerStrips(
                    offline = state.offline,
                    queued = state.queuedCount,
                    onRetry = vm::retrySync,
                )
```

- [ ] **Step 8: Run the full suite**

Run:
```bash
./gradlew :core:model:test :core:audit:test \
          :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest \
          :app:testDebugUnitTest
```
Expected: PASS, exit 0. If the compiler reports `c` or `stringResource` as now
unused in `DipiAppUi.kt`, leave them — both are used elsewhere in that file.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/kotlin/org/dhamma/dipi/staff/ui/SyncBanners.kt \
        app/src/main/kotlin/org/dhamma/dipi/staff/ui/DipiAppUi.kt \
        app/src/main/kotlin/org/dhamma/dipi/staff/ui/DeskViewModel.kt \
        app/src/main/res/values/strings.xml \
        app/src/test/kotlin/org/dhamma/dipi/staff/SyncBannersTest.kt
git commit -m "fix: split the offline and queued sync strips and add a Retry"
```

---

### Task 4: Today loading skeleton

Replaces the six-dots placeholder with the design's eight-row skeleton.
Spec S2.

**Files:**
- Create: `feature/applicants/src/main/kotlin/org/dhamma/dipi/staff/applicants/TodaySkeleton.kt`
- Modify: `feature/applicants/src/main/kotlin/org/dhamma/dipi/staff/applicants/TodayScreen.kt:90-94`
- Test: `app/src/test/kotlin/org/dhamma/dipi/staff/TodaySkeletonTest.kt`

**Interfaces:**
- Consumes: `Industry.neutral200`, `Industry.neutral300` from `:core:ui`.
- Produces: `@Composable fun TodaySkeleton(modifier: Modifier = Modifier)`, tagged `today-skeleton`, with each row tagged `skeleton-row`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/kotlin/org/dhamma/dipi/staff/TodaySkeletonTest.kt`:

```kotlin
package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import org.dhamma.dipi.staff.applicants.TodaySkeleton
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TodaySkeletonTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun drawsEightRows() {
        rule.setContent { DipiTheme { TodaySkeleton() } }
        rule.onNodeWithTag("today-skeleton").assertIsDisplayed()
        rule.onAllNodesWithTag("skeleton-row").assertCountEquals(8)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests '*TodaySkeletonTest*'`
Expected: FAIL — `Unresolved reference: TodaySkeleton`.

- [ ] **Step 3: Write the implementation**

Create `feature/applicants/src/main/kotlin/org/dhamma/dipi/staff/applicants/TodaySkeleton.kt`:

```kotlin
package org.dhamma.dipi.staff.applicants

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.dhamma.dipi.staff.ui.theme.Industry

/**
 * Name-bar widths as a fraction of the row. The design binds these to absent
 * data; fixed here so screenshot tests stay stable. See
 * docs/specs/2026-08-26-v3-conformance-spec.md S2.3.
 */
private val SKELETON_WIDTHS = listOf(0.52f, 0.66f, 0.44f, 0.60f, 0.72f, 0.48f, 0.58f, 0.64f)

/** The eight-row Today loading skeleton (design: `3 · Today — loading skeleton`). */
@Composable
fun TodaySkeleton(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().testTag("today-skeleton")) {
        SKELETON_WIDTHS.forEach { width -> SkeletonRow(width) }
    }
}

@Composable
private fun SkeletonRow(nameWidth: Float) {
    Column(Modifier.fillMaxWidth().testTag("skeleton-row")) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Bar(Modifier.weight(nameWidth).height(14.dp), Industry.neutral300)
                Spacer(Modifier.weight(1f - nameWidth))
                Bar(Modifier.width(46.dp).height(14.dp), Industry.neutral200)
            }
            Bar(Modifier.fillMaxWidth(0.62f).height(11.dp), Industry.neutral200)
        }
        HorizontalDivider(thickness = 1.dp, color = Industry.neutral300)
    }
}

@Composable
private fun Bar(modifier: Modifier, color: Color) {
    Box(modifier.clip(RoundedCornerShape(2.dp)).background(color))
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests '*TodaySkeletonTest*'`
Expected: PASS, 1 test.

- [ ] **Step 5: Use it in TodayScreen**

In `feature/applicants/.../TodayScreen.kt`, replace lines 90-94:

```kotlin
            if (loading && rows.isEmpty()) {
                Column {
                    repeat(6) { Text("········", modifier = Modifier.padding(16.dp), color = c.muted) }
                }
            } else if (rows.isEmpty()) {
```

with:

```kotlin
            if (loading && rows.isEmpty()) {
                TodaySkeleton()
            } else if (rows.isEmpty()) {
```

- [ ] **Step 6: Run the full suite**

Run:
```bash
./gradlew :core:model:test :core:audit:test \
          :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest \
          :app:testDebugUnitTest
```
Expected: PASS, exit 0. `TodayScreenTest` has three existing cases; none
asserted on the dots, so all three must still pass untouched.

- [ ] **Step 7: Commit**

```bash
git add feature/applicants/src/main/kotlin/org/dhamma/dipi/staff/applicants/ \
        app/src/test/kotlin/org/dhamma/dipi/staff/TodaySkeletonTest.kt
git commit -m "feat: draw the designed eight-row Today loading skeleton"
```

---

### Task 5: Centre settings conformance

Brings the only unstyled desk surface onto the design system and renders the
live RESULT block. Spec S3.

**Files:**
- Modify: `feature/course/src/main/kotlin/org/dhamma/dipi/staff/course/CentreOpsScreen.kt:42-53`
- Test: `app/src/test/kotlin/org/dhamma/dipi/staff/CentreOpsScreenTest.kt`

**Interfaces:**
- Consumes: `DeskKicker(text, color, modifier)` from Task 1; `centreOpsEffect(prefs)` from Task 2; `deskCard()` and `DeskStyle` already in `:core:ui`.
- Produces: no new public API — the screen signature is unchanged, so
  `DipiAppUi.kt:277` needs no edit.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/kotlin/org/dhamma/dipi/staff/CentreOpsScreenTest.kt`:

```kotlin
package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.dhamma.dipi.staff.course.CentreOpsScreen
import org.dhamma.dipi.staff.model.CentreOpsPrefs
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CentreOpsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun showsTheSubLineNotesAndDerivedResult() {
        rule.setContent {
            DipiTheme {
                CentreOpsScreen(
                    prefs = CentreOpsPrefs(laundry = true, valuables = true, groups = false),
                    onToggleLaundry = {},
                    onToggleValuables = {},
                    onToggleGroups = {},
                    onOpenRooms = {},
                    onBack = {},
                )
            }
        }
        rule.onNodeWithText(
            "Three switches change what check-in asks for. " +
                "The line at the bottom shows the result.",
        ).assertIsDisplayed()
        rule.onNodeWithText("Check-in asks whether laundry was issued.").assertIsDisplayed()
        rule.onNodeWithText("RESULT").assertIsDisplayed()
        rule.onNodeWithText(
            "Check-in asks for room, seating, laundry and valuables. " +
                "Everyone sits in Main Dhamma Hall and Zero Day hides group chips.",
        ).assertIsDisplayed()
    }

    @Test
    fun resultFollowsTheSwitches() {
        rule.setContent {
            DipiTheme {
                CentreOpsScreen(
                    prefs = CentreOpsPrefs(laundry = false, valuables = false, groups = true),
                    onToggleLaundry = {},
                    onToggleValuables = {},
                    onToggleGroups = {},
                    onOpenRooms = {},
                    onBack = {},
                )
            }
        }
        rule.onNodeWithText("Check-in asks for room, seating and group.").assertIsDisplayed()
    }

    @Test
    fun tappingARowToggles() {
        var toggled = false
        rule.setContent {
            DipiTheme {
                CentreOpsScreen(
                    prefs = CentreOpsPrefs(),
                    onToggleLaundry = { toggled = true },
                    onToggleValuables = {},
                    onToggleGroups = {},
                    onOpenRooms = {},
                    onBack = {},
                )
            }
        }
        rule.onNodeWithText("Laundry").performClick()
        assertTrue(toggled)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests '*CentreOpsScreenTest*'`
Expected: FAIL — the sub-line, the note and `RESULT` are not rendered today.

- [ ] **Step 3: Add the imports**

In `feature/course/.../CentreOpsScreen.kt`, add to the import block:

```kotlin
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import org.dhamma.dipi.staff.model.centreOpsEffect
import org.dhamma.dipi.staff.ui.theme.DeskKicker
import org.dhamma.dipi.staff.ui.theme.DeskStyle
import org.dhamma.dipi.staff.ui.theme.Industry
import org.dhamma.dipi.staff.ui.theme.deskCard
```

- [ ] **Step 4: Replace the header, toggles and effect line**

Replace lines 42-53 — the whole run from `Text("Centre settings", ...)` through
`TextButton(onClick = onOpenRooms) { Text("Room chart") }`, which is exactly:

```kotlin
        Text("Centre settings", fontFamily = DipiCondensed, fontSize = 22.sp, color = c.foreground)
        TextButton(onClick = onBack) { Text("Back") }
        ToggleRow("Laundry", prefs.laundry, onToggleLaundry)
        ToggleRow("Valuables", prefs.valuables, onToggleValuables)
        ToggleRow("Groups", prefs.groups, onToggleGroups)
        Text(
            "when off, everyone sits in Main Dhamma Hall and Zero Day hides group chips",
            color = c.muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        TextButton(onClick = onOpenRooms) { Text("Room chart") }
```

with the following. Note that **`onBack` and `onOpenRooms` must both survive** —
dropping either strips the screen's only navigation:

```kotlin
        Text("Centre settings", fontFamily = DipiCondensed, fontSize = 22.sp, color = c.foreground)
        TextButton(onClick = onBack) { Text("Back") }
        Text(
            "Three switches change what check-in asks for. " +
                "The line at the bottom shows the result.",
            color = c.muted,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp),
        )
        ToggleRow(
            title = "Laundry",
            note = "Check-in asks whether laundry was issued.",
            on = prefs.laundry,
            onClick = onToggleLaundry,
        )
        ToggleRow(
            title = "Valuables",
            note = "Check-in asks whether valuables were deposited.",
            on = prefs.valuables,
            onClick = onToggleValuables,
        )
        ToggleRow(
            title = "Groups",
            note = "Check-in assigns a sitting group; Zero Day shows group chips.",
            on = prefs.groups,
            onClick = onToggleGroups,
        )
        Spacer(Modifier.height(14.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .deskCard()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DeskKicker("RESULT", Industry.neutral500)
            Text(centreOpsEffect(prefs), color = c.foreground, fontSize = 13.sp)
        }
        Spacer(Modifier.height(14.dp))
        TextButton(onClick = onOpenRooms) { Text("Room chart") }
```

- [ ] **Step 5: Replace the ToggleRow composable**

Replace the whole private `ToggleRow` (from `private fun ToggleRow(label: String, on: Boolean, onClick: () -> Unit) {` to its closing brace) with:

```kotlin
@Composable
private fun ToggleRow(title: String, note: String, on: Boolean, onClick: () -> Unit) {
    val c = LocalDipi.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = c.foreground, fontSize = 15.sp)
            Text(note, color = c.muted, fontSize = 12.sp)
        }
        DeskKicker(
            if (on) "ON" else "OFF",
            if (on) Industry.accent700 else Industry.neutral500,
            Modifier.padding(start = 12.dp),
        )
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests '*CentreOpsScreenTest*'`
Expected: PASS, 3 tests. The old static "when off, everyone sits in…" line is
already gone — Step 4 replaced it along with the rest of that range.

- [ ] **Step 7: Run the full suite**

Run:
```bash
./gradlew :core:model:test :core:audit:test \
          :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest \
          :app:testDebugUnitTest
```
Expected: PASS, exit 0.

- [ ] **Step 8: Commit**

```bash
git add feature/course/src/main/kotlin/org/dhamma/dipi/staff/course/CentreOpsScreen.kt \
        app/src/test/kotlin/org/dhamma/dipi/staff/CentreOpsScreenTest.kt
git commit -m "feat: bring centre settings onto the design system with a live RESULT line"
```

---

### Task 6: Drop the unused window-size-class dependency

Spec S4. The adaptive split already runs off `LocalConfiguration.screenWidthDp`.

**Files:**
- Modify: `app/build.gradle.kts:93`

**Interfaces:**
- Consumes: nothing.
- Produces: nothing.

- [ ] **Step 1: Confirm it is genuinely unused**

Run:
```bash
grep -rn "windowsizeclass\|WindowSizeClass" --include=*.kt . | grep -v "/build/"
```
Expected: no output. If this prints anything, **stop** — the dependency is in
use and this task must be skipped.

- [ ] **Step 2: Remove the line**

In `app/build.gradle.kts`, delete:

```kotlin
    implementation(libs.androidx.material3.window)
```

Leave the `androidx-material3-window` entry in `gradle/libs.versions.toml`
alone — the catalog may list versions no module currently uses.

- [ ] **Step 3: Verify the app still assembles and tests pass**

Run:
```bash
./gradlew :app:assembleDebug
./gradlew :core:model:test :core:audit:test \
          :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest \
          :app:testDebugUnitTest
```
Expected: both exit 0.

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts
git commit -m "chore: drop the unused material3 window-size-class dependency"
```

---

### Task 7: Ship 1.20.0

Hard rule 11 requires the bump; hard rule 12 requires the tablet install
because the registrar taps every surface changed here.

**Files:**
- Modify: `app/build.gradle.kts:27-28`
- Modify: `CLAUDE.md:5`
- Modify: `AGENTS.md:9`

**Interfaces:**
- Consumes: everything from Tasks 1-6.
- Produces: a signed-with-debug-key release APK installed on the Pixel C.

- [ ] **Step 1: Confirm the version is still free**

Run:
```bash
git show feat/desk-gap:app/build.gradle.kts | grep -E "versionCode|versionName"
```
Expected: `versionCode = 30`, `versionName = "1.19.0"`. Those belong to the
unmerged desk-gap branch, so this work takes the next slot. If desk-gap has
moved to 1.20.0, use 1.21.0 / 32 instead and adjust the doc edits below.

- [ ] **Step 2: Bump the version**

In `app/build.gradle.kts`, change:

```kotlin
        versionCode = 29
        versionName = "1.18.0"
```

to:

```kotlin
        versionCode = 31
        versionName = "1.20.0"
```

- [ ] **Step 3: Update the helper files**

In `CLAUDE.md`, replace `Vertical 2 desk **1.18.0** (`versionCode` 29) on `main``
with `Vertical 2 desk **1.20.0** (`versionCode` 31) on `main``.

In `AGENTS.md`, replace `**Shipped:** Vertical 2 desk on `main`, **1.18.0** (`versionCode` 29)`
with `**Shipped:** Vertical 2 desk on `main`, **1.20.0** (`versionCode` 31)`.

- [ ] **Step 4: Run the full suite one last time**

Run:
```bash
./gradlew :core:model:test :core:audit:test \
          :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest \
          :app:testDebugUnitTest
```
Expected: PASS, exit 0.

- [ ] **Step 5: Build the slim release**

Run: `./gradlew :app:assembleRelease`
Expected: exit 0, APK at `app/build/outputs/apk/release/app-release.apk`.

- [ ] **Step 6: Install on the Pixel C**

```bash
export ANDROID_HOME=/Users/wizops/Android/Sdk
export PATH="$ANDROID_HOME/platform-tools:$PATH"
adb connect 10.0.0.144:5555
adb -s 10.0.0.144:5555 install -r -d app/build/outputs/apk/release/app-release.apk
adb -s 10.0.0.144:5555 shell am start -n org.dhamma.dipi.staff/.MainActivity
```

If `adb connect` fails, re-pair over USB once with
`adb -s 5C01001294 tcpip 5555`, then retry. If DHCP moved the tablet, find it
with `adb shell ip -f inet addr show wlan0`.

- [ ] **Step 7: Verify on the tablet by hand**

Check each changed surface, since none of this is covered by an instrumented
test:
1. Settings → toggle offline. The grey `◍ Offline — showing cached list` strip
   appears **alone** when nothing is queued.
2. Queue a status change while offline, then go back online. The accent strip
   reads `N changes waiting to sync` **without** the offline copy, and RETRY is
   tappable.
3. Pull-to-refresh Today on a cold cache — eight skeleton rows, not dots.
4. Centre → Centre settings — the three switches show notes and ON/OFF, and the
   RESULT line changes as you toggle.

- [ ] **Step 8: Commit**

```bash
git add app/build.gradle.kts CLAUDE.md AGENTS.md
git commit -m "feat: version-3 conformance for sync banners, skeleton and centre settings at 1.20.0"
```

---

## Self-review notes

**Spec coverage.** S1.2 → Task 3 Step 1 (the four-row matrix is four assertions).
S1.3/S1.4 → Task 3 Step 4. S1.5 → Task 3 Steps 6-7. S2.2/S2.3/S2.4 → Task 4
Step 3. S3.2/S3.3 → Task 5 Steps 4-5. S3.4 → Task 2. S3.5 → Task 1. S4 → Task 6.
Versioning → Task 7. Out-of-scope items are deliberately unplanned.

**Type consistency.** `syncBanners`/`SyncBanner.Offline`/`SyncBanner.Queued(count)`
are used identically in Task 3's test and implementation. `centreOpsEffect(prefs)`
is defined in Task 2 and consumed in Task 5. `DeskKicker(text, color, modifier)`
keeps its exact signature across the Task 1 move and the Task 5 call sites.
`CentreOpsScreen`'s parameter list is unchanged, so `DipiAppUi.kt:277` is
untouched.

**Known risk.** Task 5 Step 4 replaces a line range; if Tasks 1-4 have shifted
`CentreOpsScreen.kt`, match on the quoted code rather than the line numbers.
That file is not touched by any earlier task, so the numbers should hold.
