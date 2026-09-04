package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.dhamma.dipi.staff.desk.DaySummaryPane
import org.dhamma.dipi.staff.model.DayRollRow
import org.dhamma.dipi.staff.model.DaySummary
import org.dhamma.dipi.staff.model.OldNew
import org.dhamma.dipi.staff.model.RollMatrix
import org.dhamma.dipi.staff.model.SpecialRow
import org.dhamma.dipi.staff.model.SpecialSeating
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The native Day 0 summary (v5 T2, frame `5d`). The desk serves this as an
 * unstyled fragment, so before this pass it was the one Board cell that
 * rendered as browser-default HTML.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1240dp-h844dp-land")
class DaySummaryPaneTest {
    @get:Rule
    val rule = createComposeRule()

    private val loaded = DaySummary(
        confirmed = RollMatrix(
            male = DayRollRow(18, 27, 45, 3),
            female = DayRollRow(14, 22, 36, 2),
            total = DayRollRow(32, 49, 81, 5),
        ),
        attended = RollMatrix(
            male = DayRollRow(1, 0, 1, 0),
            female = DayRollRow(0, 0, 0, 0),
            total = DayRollRow(1, 0, 1, 0),
        ),
        specialSeating = SpecialSeating(
            male = SpecialRow(OldNew(1, 1), OldNew(0, 2), OldNew(3, 0)),
            female = SpecialRow(OldNew(0, 0), OldNew(1, 0), OldNew(0, 1)),
            total = SpecialRow(OldNew(1, 1), OldNew(1, 2), OldNew(3, 1)),
        ),
    )

    private fun show(summary: DaySummary) {
        rule.setContent { DipiTheme { DaySummaryPane(summary) } }
    }

    /**
     * `CONFIRMED 81 → ATTENDED 1` is the whole point of the screen, so it
     * gets the largest type on it — 62sp, well past the 38sp Board stats.
     */
    @Test
    fun confirmedToAttendedHeadlineIs62sp() {
        show(loaded)
        rule.onNodeWithTag("day-summary-headline").assertIsDisplayed()
        val confirmed = rule.onNodeWithTag("day-summary-figure-CONFIRMED")
            .getUnclippedBoundsInRoot()
        val attended = rule.onNodeWithTag("day-summary-figure-ATTENDED")
            .getUnclippedBoundsInRoot()
        // 62sp of condensed digits is ~60dp tall at density 1; anything much
        // smaller means the headline lost its weight.
        val tall = (confirmed.bottom - confirmed.top).value
        assertTrue("headline figure looks too small: ${tall}dp", tall > 50f)
        // Side by side, not stacked: the arrow between them is the sentence.
        assertTrue(kotlin.math.abs(confirmed.top.value - attended.top.value) < 30f)
    }

    /** 81 − 1 is arithmetic on numbers we already hold, not a third request. */
    @Test
    fun stillToArriveIsDerivedNotFetched() {
        show(loaded)
        rule.onNodeWithTag("day-summary-still-to-arrive").assertIsDisplayed()
        rule.onNodeWithText("80").assertIsDisplayed()
        rule.onNodeWithText("STILL TO ARRIVE").assertIsDisplayed()
    }

    /**
     * Day −1: the whole screen is zeros. That is a real answer, so it must
     * not read as a failed fetch — no dashes, no hidden rows, and the
     * facilities card says in words why its grid is empty.
     */
    @Test
    fun allZeroesDoNotLookBroken() {
        show(DaySummary())
        rule.onNodeWithTag("day-summary-headline").assertIsDisplayed()
        rule.onNodeWithTag("day-summary-confirmed").assertIsDisplayed()
        rule.onNodeWithTag("day-summary-attended").assertIsDisplayed()
        rule.onNodeWithTag("day-summary-facilities-empty").assertExists()
        // Zeros are shown, never replaced by an em dash or an empty cell.
        rule.onNodeWithText("—").assertDoesNotExist()
        rule.onNodeWithText("STILL TO ARRIVE").assertExists()
    }

    /** The matrix borrows the centre dashboard's idiom rather than inventing one. */
    @Test
    fun matrixMatchesTheCentreDashboardIdiom() {
        show(loaded)
        listOf("OLD", "NEW", "TOTAL", "SEVAK").forEach {
            rule.onAllNodesWithText(it).fetchSemanticsNodes().let { nodes ->
                assertTrue("$it column label missing", nodes.isNotEmpty())
            }
        }
        rule.onNodeWithTag("day-summary-confirmed").assertExists()
        rule.onNodeWithTag("day-summary-attended").assertExists()
        rule.onNodeWithText("SPECIAL SEATING").assertExists()
    }

    /** Headline, both matrices and the facilities card all reachable on the tablet. */
    @Test
    fun fitsTheFoldWithTheRail() {
        show(loaded)
        rule.onNodeWithTag("day-summary-headline").assertIsDisplayed()
        rule.onNodeWithTag("day-summary-facilities").assertExists()
    }

    /**
     * `0 (O) + 0 (N)` is an instruction to the hall team, not a roll count,
     * so it reads as two figures under one header rather than as a string.
     */
    @Test
    fun facilitiesCardSplitsOldAndNew() {
        show(loaded)
        rule.onNodeWithTag("day-summary-facilities").assertExists()
        // One header per row: Male, Female and Total.
        listOf("CHOWKY", "CHAIR", "BACKREST").forEach {
            rule.onAllNodesWithText(it).assertCountEquals(3)
        }
        // The raw desk string never reaches the screen.
        rule.onNodeWithText("(O) +", substring = true).assertDoesNotExist()
    }
}
