package org.dhamma.dipi.staff

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.width
import org.dhamma.dipi.staff.course.CentreScreen
import org.dhamma.dipi.staff.course.centreDeskTiles
import org.dhamma.dipi.staff.model.Centre
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.CourseMatrix
import org.dhamma.dipi.staff.model.MatrixRow
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The wide (Pixel-C-shaped) branch of [CentreScreen]: a fixed header above
 * an upcoming-courses region capped at 60% of the space below it, and a lower
 * pane taking all the rest. Before this file, no test ran at a
 * `screenWidthDp >= 600` config, so this branch was compiled but never
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
        // Upper region: upcoming courses.
        rule.onNodeWithText("Upcoming courses").assertIsDisplayed()
        rule.onNodeWithText("10-Day").assertIsDisplayed()
        // Lower region: with no older courses the desk column takes the whole
        // pane and never scrolls, so its content must already be wholly on
        // screen — no performScrollTo() to lean on, and assertIsDisplayed()
        // alone would pass on a single visible pixel.
        rule.onNodeWithText("Centre desk").assertWhollyOnScreen()
        rule.onNodeWithText("Centre Settings").assertWhollyOnScreen()
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
        // Lower region is still reachable, not squeezed to 0dp. The desk
        // column has no scroll of its own, so being wholly on screen (not
        // merely one visible pixel via assertIsDisplayed()) is the whole test.
        rule.onNodeWithText("Centre desk").assertWhollyOnScreen()
        rule.onNodeWithText("Centre Settings").assertWhollyOnScreen()
    }

    @Test
    fun shortUpcomingListLeavesNoDeadBandBeforeOlderCourses() {
        // Owner feedback 2026-08-27: "useless space. keep the UI tight" — a
        // short upcoming list must not be padded out to its full share,
        // pushing "Older courses" out of the initial frame. The upcoming
        // region wraps its content under a heightIn(max = 60%) cap rather
        // than claiming a fixed slot, so both regions land in the same frame
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
        // is already wholly on screen, not stranded below a dead band (and
        // not merely one visible pixel via assertIsDisplayed()).
        rule.onNodeWithText("Older courses").assertWhollyOnScreen()
    }

    @Test
    fun lowerPaneStacksOlderCoursesAboveAFullWidthDeskColumn() {
        // S4 (owner decision 2026-08-30): the side-by-side split is gone.
        // Older courses take the pane's full width on the upcoming grid, and
        // the desk column stacks beneath them, also full width — three tiles
        // across, then the rule + kicker and the two surviving desk-site
        // chips. The pane keeps the scroll the older column used to carry, so
        // the tail of the stack is reachable rather than lost.
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
        rule.onNodeWithText("Older courses").assertIsDisplayed()
        rule.onNodeWithText(older.name).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Centre desk").assertIsDisplayed()
        rule.onNodeWithText("Centre Settings").assertIsDisplayed()
        rule.onNodeWithText("Advanced Search").assertIsDisplayed()
        rule.onNodeWithText("App Settings").assertIsDisplayed()
        // No desk-site chips remain after the Bulk Mail retirement
        // (2026-09-05); the kicker leaves with them.
        rule.onNodeWithText("MORE ON THE DESK SITE").assertDoesNotExist()
        rule.onNodeWithText("Course report").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun deskSiteChipsStayAboveTheFoldWithOlderCoursesPresent() {
        // The dead-band regression this pins: Compose reserves a weighted
        // child's slot from the weight ratio alone and never redistributes
        // what a `fill = false` child declines — so against the old
        // weight(0.6f)/weight(0.4f) pair the lower pane stayed clamped to 40%
        // while ~292px sat empty at the bottom of the screen, pushing the
        // tail of the desk column past the fold. The lower pane's weight(1f)
        // now takes the whole remainder. (The tail is the last tile row since
        // the Bulk Mail retirement removed the chip shelf, 2026-09-05.)
        //
        // No performScrollTo() anywhere in this test: that is the whole
        // assertion. It fails against weight(0.4f).
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
        rule.onNodeWithText("Older courses").assertIsDisplayed()
        rule.onNodeWithText("Centre desk").assertIsDisplayed()
        rule.onNodeWithText("Course report").assertWhollyOnScreen()
        rule.onNodeWithText("App Settings").assertWhollyOnScreen()
        rule.onNodeWithText("Bulk Mail").assertDoesNotExist()
    }

    @Test
    fun allFourUpcomingCoursesStayWhollyOnScreen() {
        // S2 removed the upcoming pane's scroll on the premise that its
        // content is bounded and fits under the 60% ceiling: at most four
        // courses (`limit 4` in the backend's `upcoming_courses()`), two per
        // row, every card the same fixed height. With no scroll, anything
        // that does not fit is not merely awkward — it is invisible and
        // unreachable, so the premise has to be pinned, not asserted in a
        // comment.
        //
        // This is also what stops the dead-band fix from being a re-weighting:
        // any weight ratio that hands the leftover downward also lowers this
        // ceiling (weight(0.6f) beside a weight(1f) sibling is a 37.5%
        // ceiling, which clips the second card row away entirely). Hence the
        // measured heightIn(max = 60%) cap, which holds both properties.
        val matrix = CourseMatrix(
            rows = listOf(
                MatrixRow("Received", newMale = 1, newFemale = 1),
                MatrixRow("Confirmed", newMale = 41, oldMale = 17, newFemale = 33, oldFemale = 9),
                MatrixRow("Cancelled", newMale = 2, newFemale = 1),
            ),
            total = MatrixRow("Total", newMale = 44, oldMale = 17, newFemale = 35, oldFemale = 9),
        )
        val upcoming = (1..4).map {
            course.copy(id = CourseId(it), name = "Course $it", matrix = matrix)
        }
        rule.setContent {
            DipiTheme {
                CentreScreen(
                    session = singleCentreSession,
                    courses = upcoming,
                    onPick = {},
                    olderCourses = emptyList(),
                )
            }
        }
        upcoming.forEach {
            rule.onNodeWithText(it.name, useUnmergedTree = true).onParent().assertWhollyOnScreen()
        }
    }

    @Test
    fun fourthCardsTotalRowIsReachableWithoutClipping() {
        // Bug B (SOLVED 2026-08-30, .superpowers/sdd/centre-card-bloat.md):
        // a post-1.24.1 screenshot showed the third and fourth cards (the
        // second card row) rendering Received and Confirmed + Expected, then
        // ending abruptly — Cancelled and Total were clipped off by the
        // upcoming pane's heightIn(max = 60%) ceiling, which had no scroll.
        // allFourUpcomingCoursesStayWhollyOnScreen above did not catch this:
        // it checks the bounds of the node that is the course *name*'s
        // parent, which sits at the very top of the card and stayed wholly
        // on screen even while the card's own bottom was clipped away — an
        // assertion that passed against the broken layout. This test targets
        // the fourth card's Total row directly, with a full four-row matrix
        // (Received, Confirmed, Expected, Cancelled, plus the Total row) so
        // all four rendered rows are present, matching the screenshot.
        val matrix = CourseMatrix(
            rows = listOf(
                MatrixRow("Received", newMale = 1, newFemale = 1),
                MatrixRow("Confirmed", newMale = 16, oldMale = 13, newFemale = 16, oldFemale = 13),
                MatrixRow("Expected", newMale = 2, newFemale = 2),
                MatrixRow("Cancelled", newMale = 2, newFemale = 1),
            ),
            total = MatrixRow("Total", newMale = 44, oldMale = 17, newFemale = 35, oldFemale = 9),
        )
        val upcoming = (1..4).map {
            course.copy(id = CourseId(it), name = "Course $it", matrix = matrix)
        }
        rule.setContent {
            DipiTheme {
                CentreScreen(
                    session = singleCentreSession,
                    courses = upcoming,
                    onPick = {},
                )
            }
        }
        // Four cards -> four "Total" rows, in card order (row 0: 1, 2; row
        // 1: 3, 4). Index 3 is the fourth card's.
        rule.onAllNodesWithText("Total")[3].performScrollTo().assertWhollyOnScreen()
    }

    @Test
    fun theLowerPaneStillNeedsItsScrollAtTheBoundedWorstCase() {
        // Why the below-header region still needs its verticalScroll: the
        // worst case the desk can serve still overflows the viewport. Header
        // capped at 220dp by 8 centres, the backend's full four upcoming
        // courses each carrying a matrix (the tallest card there is), and
        // OLDER_COURSE_LIMIT older courses. "Older courses" and the chips are
        // then only reachable by scrolling — so a missing scroll here would
        // strand a control, and performScrollTo() would fail outright with
        // no scrollable ancestor (as it now does pre-fix — see
        // fourthCardsTotalRowIsReachableWithoutClipping).
        //
        // Post-Bug-B-fix (2026-08-30): the scroll moved from the lower
        // pane's own box to the one outer scroll covering upcoming + the
        // lower pane, so "Older courses" is no longer guaranteed inside the
        // initial viewport the way it was when the lower pane had a bounded,
        // separately-scrolling box of its own — it is still reachable, just
        // via the same scroll as everything else now.
        val matrix = CourseMatrix(
            rows = listOf(
                MatrixRow("Received", newMale = 1, newFemale = 1),
                MatrixRow("Confirmed", newMale = 41, oldMale = 17, newFemale = 33, oldFemale = 9),
                MatrixRow("Cancelled", newMale = 2, newFemale = 1),
            ),
            total = MatrixRow("Total", newMale = 44, oldMale = 17, newFemale = 35, oldFemale = 9),
        )
        val upcoming = (1..4).map {
            course.copy(id = CourseId(it), name = "Course $it", matrix = matrix)
        }
        val older = (5..7).map {
            Course(CourseId(it), CentreId(1), "Older course $it", "2026-07-06", "2026-07-17")
        }
        rule.setContent {
            DipiTheme {
                CentreScreen(
                    session = singleCentreSession.copy(
                        centres = (1..8).map { Centre(CentreId(it), "Dhamma Centre $it") },
                    ),
                    courses = upcoming,
                    onPick = {},
                    olderCourses = older,
                )
            }
        }
        rule.onNodeWithText("Older courses").performScrollTo().assertIsDisplayed()
        deskSiteTiles.forEach {
            rule.onNodeWithText(it.title).performScrollTo().assertIsDisplayed()
        }
    }

    /**
     * Stronger than [assertIsDisplayed], which is satisfied by a single
     * visible pixel — against `weight(0.4f)` the chips hung 26dp below the
     * pane and still "displayed". A node is only above the fold when the
     * pane's clip takes nothing off it, i.e. its clipped bounds are its
     * unclipped bounds.
     */
    private fun SemanticsNodeInteraction.assertWhollyOnScreen() {
        val clipped = getBoundsInRoot()
        val unclipped = getUnclippedBoundsInRoot()
        assertEquals(unclipped.top.value, clipped.top.value, 1f)
        assertEquals(unclipped.bottom.value, clipped.bottom.value, 1f)
    }

    @Test
    fun olderCourseButtonsAreAsWideAsAnUpcomingCard() {
        // S4: older courses render on the same two-column grid as upcoming
        // courses, inside a pane with the same horizontal insets — so an
        // older button is exactly as wide as an upcoming card, the "mid way"
        // the owner asked for. Compare the clickable rows themselves (the
        // Text nodes' semantic parent), not the label nodes.
        val older = listOf(
            Course(CourseId(8), CentreId(1), "Older course A", "2026-08-06", "2026-08-17"),
            Course(CourseId(9), CentreId(1), "Older course B", "2026-07-06", "2026-07-17"),
        )
        rule.setContent {
            DipiTheme {
                CentreScreen(
                    session = singleCentreSession,
                    courses = listOf(course),
                    onPick = {},
                    olderCourses = older,
                )
            }
        }
        val cardWidth = rule.onNodeWithText("10-Day", useUnmergedTree = true)
            .onParent().getUnclippedBoundsInRoot().width
        val buttonWidth = rule.onNodeWithText("Older course A", useUnmergedTree = true)
            .onParent().getUnclippedBoundsInRoot().width
        assertEquals(cardWidth.value, buttonWidth.value, 1f)
    }

    @Test
    fun noDeskSiteChipsRenderOnTheWideLayout() {
        // Bulk Mail was the last `action == null` chip (retired 2026-09-05);
        // the wide layout drops the chip shelf and its kicker with it.
        assertTrue(deskSiteTiles.isEmpty())
        rule.setContent {
            DipiTheme {
                CentreScreen(
                    session = singleCentreSession,
                    courses = listOf(course),
                    onPick = {},
                )
            }
        }
        rule.onNodeWithText("MORE ON THE DESK SITE").assertDoesNotExist()
    }

    @Test
    fun matrixHeaderShowsGroupCapsAboveAllSixColumnLabels() {
        // v4 frame 1a: MALE/FEMALE caps over each trio, and the six column
        // labels keep their names — NM/OM/NF/OF muted, M and F darker.
        val withMatrix = course.copy(
            matrix = CourseMatrix(
                rows = listOf(
                    MatrixRow("Confirmed", newMale = 41, oldMale = 17, newFemale = 33, oldFemale = 9),
                ),
                total = MatrixRow("Total", newMale = 50, oldMale = 20, newFemale = 40, oldFemale = 15),
            ),
        )
        rule.setContent {
            DipiTheme {
                CentreScreen(session = singleCentreSession, courses = listOf(withMatrix), onPick = {})
            }
        }
        rule.onNodeWithText("MALE").assertIsDisplayed()
        rule.onNodeWithText("FEMALE").assertIsDisplayed()
        listOf("NM", "OM", "M", "NF", "OF", "F").forEach {
            rule.onNodeWithText(it).assertIsDisplayed()
        }
    }

    @Test
    fun emptyOlderCoursesGivesTheDeskColumnTheFullWidth() {
        // Frame 1g: with no older courses the heading stays omitted and the
        // desk column reflows to the full width — three tiles across, chips
        // underneath, still no scroll.
        rule.setContent {
            DipiTheme {
                CentreScreen(
                    session = singleCentreSession,
                    courses = listOf(course),
                    onPick = {},
                    olderCourses = emptyList(),
                )
            }
        }
        rule.onNodeWithText("Older courses").assertDoesNotExist()
        rule.onNodeWithText("Centre desk").assertIsDisplayed()
        rule.onNodeWithText("Centre Settings").assertIsDisplayed()
        rule.onNodeWithText("Advanced Search").assertIsDisplayed()
        rule.onNodeWithText("App Settings").assertIsDisplayed()
        rule.onNodeWithText("Course report").assertIsDisplayed()
        rule.onNodeWithText("MORE ON THE DESK SITE").assertDoesNotExist()
    }

    private val deskSiteTiles = centreDeskTiles(1).filter { it.action == null }
}
