package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import org.dhamma.dipi.staff.desk.BoardPane
import org.dhamma.dipi.staff.desk.DeskCourse
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

    private val course = DeskCourse(
        dates = "10 Day · 2–13 Sep 2026",
        dayChip = "DAY 0 · TODAY",
    )

    @Test
    fun railShowsAllSixSectionsAndFooterWithoutWordmarkOrCourseCard() {
        rule.setContent {
            DipiTheme {
                DeskShell(
                    section = DeskSection.Board,
                    rail = rail,
                    course = course,
                    clock = "Wed 2 Sep · 09:41",
                    onSection = {},
                )
            }
        }
        DeskSection.entries.forEach { s ->
            rule.onNodeWithText(s.label).assertIsDisplayed()
        }
        // The lotus launcher icon sits in the rail header — with no wordmark.
        rule.onNodeWithContentDescription("DIPI").assertIsDisplayed()
        rule.onNodeWithText("DIPI Staff").assertDoesNotExist()
        // The COURSE card is gone from the rail; its identity moved to the top bar.
        rule.onNodeWithText("COURSE").assertDoesNotExist()
        rule.onNodeWithText("10 Day · 2–13 Sep 2026 · DAY 0 · TODAY").assertIsDisplayed()
        // The crumb no longer repeats the centre name (spec S3.1).
        rule.onNodeWithText("Dhamma Sudha · 10 Day · 2–13 Sep 2026 · DAY 0 · TODAY").assertDoesNotExist()
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
                DeskShell(section = DeskSection.Board, rail = rail, course = course, clock = "", onSection = { picked = it })
            }
        }
        rule.onNodeWithText("Check-in").performClick()
        assertEquals(DeskSection.CheckIn, picked)
    }

    @Test
    fun topBarShowsTheCourseLineNotTheSectionCrumb() {
        rule.setContent {
            DipiTheme {
                DeskShell(section = DeskSection.CheckIn, rail = rail, course = course, clock = "", onSection = {})
            }
        }
        rule.onNodeWithText("10 Day · 2–13 Sep 2026 · DAY 0 · TODAY").assertIsDisplayed()
        // The crumb no longer repeats the centre name (spec S3.1).
        rule.onNodeWithText("Dhamma Sudha · 10 Day · 2–13 Sep 2026 · DAY 0 · TODAY").assertDoesNotExist()
        rule.onNodeWithText("ZERO DAY · CHECK-IN").assertDoesNotExist()
        // Not loading — no progress hairline under the top bar.
        rule.onNodeWithTag("desk-loading").assertDoesNotExist()
    }

    @Test
    fun loadingDrawsTheProgressHairlineUnderTheTopBar() {
        rule.setContent {
            DipiTheme {
                DeskShell(section = DeskSection.Board, rail = rail, course = course, clock = "", onSection = {}, loading = true)
            }
        }
        rule.onNodeWithTag("desk-loading").assertExists()
    }

    @Test
    fun lotusPrefGatesTheWatermark() {
        rule.setContent {
            DipiTheme {
                DeskShell(section = DeskSection.Board, rail = rail, course = course, clock = "", onSection = {}, lotus = true)
            }
        }
        rule.onNodeWithTag("desk-watermark").assertExists()
    }

    @Test
    fun lotusOffRemovesTheWatermark() {
        rule.setContent {
            DipiTheme {
                DeskShell(section = DeskSection.Board, rail = rail, course = course, clock = "", onSection = {}, lotus = false)
            }
        }
        rule.onNodeWithTag("desk-watermark").assertDoesNotExist()
    }

    @Test
    fun railIsOneHundredNinetyDpWithAThreeDpAccentBarOnTheSelectedRow() {
        rule.setContent {
            DipiTheme {
                DeskShell(section = DeskSection.Board, rail = rail, course = course, clock = "", onSection = {})
            }
        }
        // Tag the rail column `desk-rail` in DeskRailPane.
        val w = rule.onNodeWithTag("desk-rail").getUnclippedBoundsInRoot().width
        assertEquals(190.dp.value, w.value, 0.5f)
        rule.onNodeWithTag("rail-accent-bar", useUnmergedTree = true).assertExists()
        val bar = rule.onNodeWithTag("rail-accent-bar", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(3.dp.value, bar.width.value, 0.5f)
    }

    @Test
    fun boardPaneNoLongerShowsTheBareCentreNameHeading() {
        // Spec S3.3: the Board's "{dayLabel} at {centreName}" / bare
        // centreName heading is gone — BoardPane no longer even takes those
        // parameters, so nothing under the crumb can repeat the centre name.
        rule.setContent {
            DipiTheme {
                DeskShell(
                    section = DeskSection.Board,
                    rail = rail,
                    course = course,
                    clock = "",
                    onSection = {},
                ) {
                    BoardPane(
                        roll = emptyList(),
                        checkIns = emptyMap(),
                        flagged = emptyList(),
                        callOutcomes = emptyMap(),
                        onGoto = {},
                        onExport = {},
                    )
                }
            }
        }
        rule.onNodeWithText("Dhamma Sudha").assertDoesNotExist()
        // The subtitle survives and is now the block's first line.
        rule.onNodeWithText(
            "0 on the roll · 0 checked in",
        ).assertIsDisplayed()
    }
}
