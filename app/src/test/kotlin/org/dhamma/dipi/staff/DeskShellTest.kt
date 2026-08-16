package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.dhamma.dipi.staff.desk.DeskRail
import org.dhamma.dipi.staff.desk.DeskSection
import org.dhamma.dipi.staff.desk.DeskShell
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1240dp-h844dp-land")
class DeskShellTest {
    @get:Rule
    val rule = createComposeRule()

    private val rail = DeskRail(
        courseName = "Dhamma Sudha",
        courseDates = "10 Day · 2–13 Sep 2026",
        dayChip = "DAY 0 · TODAY",
        userName = "registrar.sudha",
        syncLine = "synced 2 min ago",
        counts = mapOf(
            DeskSection.Applications to 214,
            DeskSection.Audit to 20,
            DeskSection.Calling to 12,
            DeskSection.CheckIn to 15,
            DeskSection.Rooms to 16,
        ),
    )

    @Test
    fun railShowsAllSixSectionsCourseCardAndFooter() {
        rule.setContent {
            DipiTheme {
                DeskShell(section = DeskSection.Board, rail = rail, clock = "Wed 2 Sep · 09:41", onSection = {})
            }
        }
        DeskSection.entries.forEach { s ->
            rule.onNodeWithText(s.label).assertIsDisplayed()
        }
        // The lotus launcher icon sits inside the blueprint frame next to the wordmark.
        rule.onNodeWithContentDescription("DIPI").assertIsDisplayed()
        rule.onNodeWithText("DIPI Staff").assertIsDisplayed()
        rule.onNodeWithText("Dhamma Sudha").assertIsDisplayed()
        rule.onNodeWithText("DAY 0 · TODAY").assertIsDisplayed()
        rule.onNodeWithText("registrar.sudha").assertIsDisplayed()
        rule.onNodeWithText("synced 2 min ago").assertIsDisplayed()
        rule.onNodeWithText("214").assertIsDisplayed()
        rule.onNodeWithText("Wed 2 Sep · 09:41").assertIsDisplayed()
    }

    @Test
    fun clickingARailRowRoutesToThatSection() {
        var picked: DeskSection? = null
        rule.setContent {
            DipiTheme {
                DeskShell(section = DeskSection.Board, rail = rail, clock = "", onSection = { picked = it })
            }
        }
        rule.onNodeWithText("Check-in").performClick()
        assertEquals(DeskSection.CheckIn, picked)
    }

    @Test
    fun topBarCrumbFollowsTheActiveSection() {
        rule.setContent {
            DipiTheme {
                DeskShell(section = DeskSection.CheckIn, rail = rail, clock = "", onSection = {})
            }
        }
        rule.onNodeWithText("ZERO DAY · CHECK-IN").assertIsDisplayed()
        // Not loading — no progress hairline under the top bar.
        rule.onNodeWithTag("desk-loading").assertDoesNotExist()
    }

    @Test
    fun loadingDrawsTheProgressHairlineUnderTheTopBar() {
        rule.setContent {
            DipiTheme {
                DeskShell(section = DeskSection.Board, rail = rail, clock = "", onSection = {}, loading = true)
            }
        }
        rule.onNodeWithTag("desk-loading").assertExists()
    }

    @Test
    fun lotusPrefGatesTheWatermark() {
        rule.setContent {
            DipiTheme {
                DeskShell(section = DeskSection.Board, rail = rail, clock = "", onSection = {}, lotus = true)
            }
        }
        rule.onNodeWithTag("desk-watermark").assertExists()
    }

    @Test
    fun lotusOffRemovesTheWatermark() {
        rule.setContent {
            DipiTheme {
                DeskShell(section = DeskSection.Board, rail = rail, clock = "", onSection = {}, lotus = false)
            }
        }
        rule.onNodeWithTag("desk-watermark").assertDoesNotExist()
    }
}
