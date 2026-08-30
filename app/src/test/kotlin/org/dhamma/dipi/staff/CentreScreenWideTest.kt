package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
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
        // Lower (0.4) region: with no older courses the desk column takes the
        // whole pane and never scrolls, so its content must already be on
        // screen — no performScrollTo() to lean on.
        rule.onNodeWithText("Centre desk").assertIsDisplayed()
        rule.onNodeWithText("Centre Settings").assertIsDisplayed()
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
        // column has no scroll of its own, so "displayed" is the whole test.
        rule.onNodeWithText("Centre desk").assertIsDisplayed()
        rule.onNodeWithText("Centre Settings").assertIsDisplayed()
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
        rule.onNodeWithText("MORE ON THE DESK SITE").performScrollTo().assertIsDisplayed()
        deskSiteTiles.forEach {
            rule.onNodeWithText(it.title).performScrollTo().assertIsDisplayed()
        }
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
    fun deskSiteChipsStillFireOnLaterWithTheSameTitleAndRoute() {
        // The 3/2 split is a rendering change only: the `action == null`
        // entries become pill chips, and each still hands `onLater` exactly
        // the (title, route) pair `centreDeskTiles` publishes.
        val fired = mutableListOf<Pair<String, String>>()
        rule.setContent {
            DipiTheme {
                CentreScreen(
                    session = singleCentreSession,
                    courses = listOf(course),
                    onPick = {},
                    onLater = { title, route -> fired += title to route },
                )
            }
        }
        deskSiteTiles.forEach { rule.onNodeWithText(it.title).performClick() }
        assertEquals(deskSiteTiles.map { it.title to it.route }, fired)
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
        rule.onNodeWithText("MORE ON THE DESK SITE").assertIsDisplayed()
        deskSiteTiles.forEach { rule.onNodeWithText(it.title).assertIsDisplayed() }
    }

    private val deskSiteTiles = centreDeskTiles(1).filter { it.action == null }
}
