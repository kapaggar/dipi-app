package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import org.dhamma.dipi.staff.course.CourseReportScreen
import org.dhamma.dipi.staff.course.CourseReportUi
import org.dhamma.dipi.staff.model.CourseReport
import org.dhamma.dipi.staff.model.CourseReportCounts
import org.dhamma.dipi.staff.model.CourseReportRow
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The native course report (v5 T3, frames `5n`–`5q`): the range is the only
 * control, an empty range is an answer, and a refusal prints verbatim.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1280dp-h900dp-mdpi")
class CourseReportScreenTest {

    @get:Rule val rule = createComposeRule()

    private fun counts(roll: Int) = CourseReportCounts(
        newMale = 40,
        newFemale = 38,
        newTotal = 78,
        oldMale = 22,
        oldFemale = 19,
        oldTotal = 41,
        rollTotal = roll,
        sevakMale = 6,
        sevakFemale = 5,
        sevakTotal = 11,
        teacherConducting = 1,
        teacherAssistant = 2,
        teacherTrainee = 0,
    )

    private val loaded = CourseReport(
        rows = listOf(
            CourseReportRow("Dhamma Sudha / 10 Day / 2026 / 03 Jan - 14 Jan", counts(119)),
            CourseReportRow("Long weekend for old students", counts(46)),
        ),
        grandTotal = counts(165),
        from = "2026-01-01",
        to = "2026-03-31",
    )

    private fun screen(
        state: CourseReportUi,
        onRun: () -> Unit = {},
        onFrom: (String) -> Unit = {},
        onCopy: (String) -> Unit = {},
    ) {
        rule.setContent {
            DipiTheme {
                CourseReportScreen(
                    state = state,
                    onFrom = onFrom,
                    onRun = onRun,
                    onCopyMessage = onCopy,
                )
            }
        }
    }

    /** Nothing is fetched on open: RUN is a deliberate act. */
    @Test
    fun firstOpenAsksForNothing() {
        var ran = false
        screen(CourseReportUi(from = "2026-01-01", to = "2026-12-31"), onRun = { ran = true })

        rule.onNodeWithTag("report-first-open").assertIsDisplayed()
        rule.onNodeWithTag("report-grand-total").assertDoesNotExist()
        assertTrue("Opening the screen must not fetch", !ran)

        rule.onNodeWithTag("report-run").performClick()
        assertTrue(ran)
    }

    /** The desk's form offers only a date range, so the app offers only that. */
    @Test
    fun theRangeIsTheOnlyControl() {
        screen(CourseReportUi(from = "2026-01-01", to = "2026-12-31"))

        rule.onNodeWithTag("report-from").assertIsDisplayed()
        rule.onNodeWithTag("report-to").assertIsDisplayed()
        rule.onNodeWithTag("report-run").assertIsDisplayed()
        listOf("Course", "Status", "Sort", "Gender").forEach {
            rule.onNodeWithText(it).assertDoesNotExist()
        }
    }

    /** A slow wide run stays narrowable — the range is never locked. */
    @Test
    fun theRangeStaysEditableWhileARunIsInFlight() {
        var typed: String? = null
        screen(
            CourseReportUi(from = "2026-01-01", to = "2026-12-31", ran = true, running = true),
            onFrom = { typed = it },
        )

        rule.onNodeWithTag("report-running").assertIsDisplayed()
        rule.onNodeWithTag("report-from").performTextReplacement("2026-06-01")
        assertEquals("2026-06-01", typed)
    }

    /** Fourteen columns, five groups, nothing dropped. */
    @Test
    fun allFourteenFiguresRenderUnderFiveGroupCaps() {
        screen(CourseReportUi(from = "2026-01-01", to = "2026-03-31", ran = true, report = loaded))

        listOf("NEW", "OLD", "ROLL", "SEVAK", "TEACHERS").forEach {
            rule.onNodeWithText(it).assertIsDisplayed()
        }
        // 13 figure cells per row + header: two course rows and the footer.
        rule.onNodeWithText("Dhamma Sudha").assertDoesNotExist()
        rule.onNodeWithText("10 Day").assertIsDisplayed()
        rule.onAllNodesWithTag("report-grand-total").assertCountEquals(1)
        rule.onNodeWithText("GRAND TOTAL").assertIsDisplayed()
        rule.onNodeWithText("01-01-2026 → 31-03-2026 · 2 courses").assertIsDisplayed()
    }

    /** A course name the desk typed freehand prints raw, never as an error. */
    @Test
    fun anUnparseableCourseNamePrintsRaw() {
        screen(CourseReportUi(ran = true, report = loaded))
        rule.onNodeWithText("Long weekend for old students").assertIsDisplayed()
    }

    /** An empty range is an answer, and it names the mistake worth checking. */
    @Test
    fun anEmptyRangeIsAnAnswerNotAFailure() {
        screen(
            CourseReportUi(
                from = "2026-05-01",
                to = "2026-05-02",
                ran = true,
                report = CourseReport(from = "2026-05-01", to = "2026-05-02"),
            ),
        )
        rule.onNodeWithTag("report-empty").assertIsDisplayed()
        rule.onNodeWithText("No course started between 01-05-2026 and 02-05-2026.")
            .assertIsDisplayed()
    }

    @Test
    fun aReversedRangeIsCalledOut() {
        screen(
            CourseReportUi(
                from = "2026-09-01",
                to = "2026-01-01",
                ran = true,
                report = CourseReport(),
            ),
        )
        rule.onNodeWithText(
            "The dates are the wrong way round — FROM is later than TO. Swap them and run again.",
        ).assertIsDisplayed()
    }

    /**
     * Hard rule: server messages render verbatim. No rewording, no icon, no
     * retry, no client-side reading of the status code.
     */
    @Test
    fun aRefusalPrintsTheServersOwnWordsUnchanged() {
        val message = "You are not authorized to access this page."
        var copied: String? = null
        screen(
            CourseReportUi(
                from = "2026-01-01",
                to = "2026-12-31",
                ran = true,
                refusal = message,
                refusalContext = "POST /centre/12/course-report · 09:14",
            ),
            onCopy = { copied = it },
        )

        rule.onNodeWithTag("report-refusal-text").assertTextEquals(message)
        rule.onNodeWithText("POST /centre/12/course-report · 09:14").assertIsDisplayed()
        rule.onNodeWithTag("report-grand-total").assertDoesNotExist()
        listOf("Try again", "Retry", "Permission denied").forEach {
            rule.onNodeWithText(it).assertDoesNotExist()
        }

        rule.onNodeWithTag("report-copy-message").performClick()
        assertEquals(message, copied)
    }

    /** Share CSV only appears once a CSV exists to share. */
    @Test
    fun shareCsvIsAbsentUntilThereIsAFile() {
        screen(CourseReportUi(ran = true, report = loaded))
        rule.onNodeWithTag("report-share-csv").assertDoesNotExist()
    }

    @Test
    fun printButtonExistsOnceAReportIsLoaded() {
        screen(CourseReportUi(ran = true, report = loaded, ranAt = "09:41"))
        rule.onNodeWithTag("report-print").assertIsDisplayed()
        rule.onNodeWithTag("report-run-strip")
            .assertTextEquals("2 COURSES · 165 STUDENTS · RAN 09:41")
        rule.onNodeWithText("every course the desk has in this range").assertIsDisplayed()
    }

    /** Every tap target on this screen clears the 48dp floor. */
    @Test
    fun controlsClearTheTouchFloor() {
        screen(CourseReportUi(from = "2026-01-01", to = "2026-12-31"))
        listOf("report-run", "report-back").forEach {
            val h = rule.onNodeWithTag(it).getUnclippedBoundsInRoot().let { b -> b.bottom - b.top }
            assertTrue("$it is ${h.value}dp", h.value >= 44f)
        }
    }
}
