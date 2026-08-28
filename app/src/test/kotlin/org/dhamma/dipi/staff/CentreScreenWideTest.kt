package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import org.dhamma.dipi.staff.course.CentreScreen
import org.dhamma.dipi.staff.model.Centre
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The wide (Pixel-C-shaped) branch of [CentreScreen]: a fixed header above
 * two independently-scrolling 60/40 regions. Before this file, no test ran
 * at a `screenWidthDp >= 600` config, so this branch was compiled but never
 * executed (whole-branch review finding 1).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1240dp-h844dp-land")
class CentreScreenWideTest {
    @get:Rule
    val rule = createComposeRule()

    private val singleCentreSession = Session(
        uid = 1,
        name = "sudha.user",
        displayName = "sudha.user",
        centres = listOf(Centre(CentreId(1), "Dhamma Sudha")),
        modeTest = false,
    )

    private val course = Course(
        CourseId(10),
        CentreId(1),
        "10-Day",
        "2026-08-20",
        "2026-08-31",
    )

    @Test
    fun wideLayoutRendersUpcomingCoursesAndReachesLowerRegion() {
        rule.setContent {
            DipiTheme {
                CentreScreen(
                    session = singleCentreSession,
                    courses = listOf(course),
                    onPick = {},
                )
            }
        }
        // Upper (0.6) region: upcoming courses.
        rule.onNodeWithText("Upcoming courses").assertIsDisplayed()
        rule.onNodeWithText("10-Day").assertIsDisplayed()
        // Lower (0.4) region is its own independent scroll — its content is
        // still reachable via performScrollTo() within that region.
        rule.onNodeWithText("Centre desk").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Centre Settings").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun manyCentresDoNotStarveTheWeightedRegions() {
        // Finding 2: an unbounded header (one row per centre) can squeeze
        // the weighted regions toward 0dp, making their verticalScroll
        // content permanently unreachable. 8 centres is well past what a
        // single-centre account shows and would have broken the old,
        // unbounded header.
        val manyCentresSession = singleCentreSession.copy(
            centres = (1..8).map { Centre(CentreId(it), "Dhamma Centre $it") },
        )
        rule.setContent {
            DipiTheme {
                CentreScreen(
                    session = manyCentresSession,
                    courses = listOf(course),
                    onPick = {},
                )
            }
        }
        // Upper region still renders the courses.
        rule.onNodeWithText("Upcoming courses").assertIsDisplayed()
        rule.onNodeWithText("10-Day").assertIsDisplayed()
        // Lower region is still reachable, not squeezed to 0dp.
        rule.onNodeWithText("Centre desk").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Centre Settings").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun shortUpcomingListLeavesNoDeadBandBeforeOlderCourses() {
        // Owner feedback 2026-08-27: "useless space. keep the UI tight" — a
        // short upcoming list must not leave the 0.6-weighted region padded
        // out to its full share, pushing "Older courses" out of the initial
        // frame. Modifier.weight(0.6f, fill = false) makes the 60% a ceiling,
        // not an exact allocation, so both regions land in the same frame
        // with no intervening scroll required to reach the heading.
        val older = Course(CourseId(8), CentreId(1), "Dhamma Sudha / 10 Day / 2026", "2026-08-06", "2026-08-17")
        rule.setContent {
            DipiTheme {
                CentreScreen(
                    session = singleCentreSession,
                    courses = listOf(course),
                    onPick = {},
                    olderCourses = listOf(older),
                )
            }
        }
        rule.onNodeWithText("Upcoming courses").assertIsDisplayed()
        rule.onNodeWithText("10-Day").assertIsDisplayed()
        // No performScrollTo() before this assertion — proving the heading
        // is already on screen, not stranded below a dead band.
        rule.onNodeWithText("Older courses").assertIsDisplayed()
    }
}
