package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.dhamma.dipi.staff.course.CentreScreen
import org.dhamma.dipi.staff.course.DeskTileAction
import org.dhamma.dipi.staff.course.centreDeskTiles
import org.dhamma.dipi.staff.course.courseCountsLine
import org.dhamma.dipi.staff.model.Centre
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.CourseMatrix
import org.dhamma.dipi.staff.model.CourseSummary
import org.dhamma.dipi.staff.model.MatrixRow
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CentreScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val session = Session(
        uid = 1,
        name = "sudha.user",
        displayName = "sudha.user",
        centres = listOf(Centre(CentreId(1), "Dhamma Sudha")),
        modeTest = false,
    )
    // Confirmed row deliberately all-nonzero so numeric assertions can't be
    // confused with the "·" that stands in for a zero elsewhere on screen.
    private val matrix = CourseMatrix(
        rows = listOf(
            MatrixRow("Received", newMale = 1, newFemale = 1),
            MatrixRow("Confirmed", newMale = 41, oldMale = 17, sevakMale = 3, newFemale = 33, oldFemale = 9, sevakFemale = 2),
            MatrixRow("Cancelled", newMale = 2, newFemale = 1),
        ),
        total = MatrixRow("Total", newMale = 50, oldMale = 20, sevakMale = 4, newFemale = 40, oldFemale = 15, sevakFemale = 3),
    )

    private val course = Course(
        CourseId(10),
        CentreId(1),
        "10-Day",
        "2026-08-20",
        "2026-08-31",
        summary = CourseSummary(received = 2, confirmed = 77, expected = 0, cancelled = 7, total = 111),
        matrix = matrix,
    )

    @Test
    fun catalogueOmitsLettersAtAndReferral() {
        val titles = centreDeskTiles(1).map { it.title }
        assertTrue(titles.contains("Centre Settings"))
        assertTrue(titles.contains("Bulk Mail"))
        assertFalse(titles.any { it.contains("Letter", ignoreCase = true) })
        assertFalse(titles.any { it.contains("AT", ignoreCase = true) })
        assertFalse(titles.any { it.contains("Referral", ignoreCase = true) })
        assertEquals(
            DeskTileAction.CentreOps,
            centreDeskTiles(1).first { it.title == "Centre Settings" }.action,
        )
        assertTrue(centreDeskTiles(1).any { it.title == "App Settings" && it.action == DeskTileAction.AppSettings })
        assertEquals("search-app/1", centreDeskTiles(1).first { it.title == "Advanced Search" }.route)
    }

    @Test
    fun countsLineDropsZeroesAndAbsentSummaries() {
        assertNull(courseCountsLine(null))
        assertNull(courseCountsLine(CourseSummary()))
        assertEquals(
            "Confirmed 77 | Cancelled 7 | Received 2 | Total 111",
            courseCountsLine(course.summary),
        )
        assertEquals(
            "Confirmed 12 · Expected 59 | Total 175",
            courseCountsLine(CourseSummary(confirmed = 12, expected = 59, total = 175)),
        )
        assertEquals("Expected 3", courseCountsLine(CourseSummary(expected = 3)))
    }

    @Test
    fun dashboardShowsCoursesCountsAndCentreRows() {
        var picked: Course? = null
        var later: Pair<String, String>? = null
        var ops = false
        var advanced = false
        rule.setContent {
            DipiTheme {
                CentreScreen(
                    session = session,
                    courses = listOf(course),
                    onPick = { picked = it },
                    onLater = { title, route -> later = title to route },
                    onCentreOps = { ops = true },
                    onAdvancedSearch = { advanced = true },
                )
            }
        }
        rule.onNodeWithText("Upcoming courses").assertIsDisplayed()
        rule.onNodeWithText("10-Day").assertIsDisplayed()
        // Owner feedback 2026-08-27: the "from your account" clause was
        // redundant and is gone from the header.
        rule.onNodeWithText("Dhamma Sudha · sudha.user").assertIsDisplayed()
        rule.onNodeWithText("Dhamma Sudha · from your account · sudha.user").assertDoesNotExist()
        // Finding 3: the kicker header is now per-cell (weight()-based, like
        // the data rows) rather than one manually-spaced literal string, so
        // each label sits above its column.
        rule.onNodeWithText("NM").assertIsDisplayed()
        rule.onNodeWithText("OM").assertIsDisplayed()
        rule.onNodeWithText("NF").assertIsDisplayed()
        rule.onNodeWithText("OF").assertIsDisplayed()
        rule.onNodeWithText("Confirmed").assertIsDisplayed()
        rule.onNodeWithText("41").assertIsDisplayed()
        rule.onNodeWithText("17").assertIsDisplayed()
        rule.onNodeWithText("58").assertIsDisplayed()
        rule.onNodeWithText("33").assertIsDisplayed()
        rule.onNodeWithText("9").assertIsDisplayed()
        rule.onNodeWithText("42").assertIsDisplayed()
        // The desk links render as a tile grid below the courses; Advanced
        // Search rides along as one of the tiles (owner feedback 2026-08-16).
        rule.onNodeWithText("Centre desk").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Advanced Search").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Centre Settings").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Manage Courses").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Daily Activity").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("SMS Report").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Course Report").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Bulk Mail").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("App Settings").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Manage Letters").assertDoesNotExist()
        rule.onNodeWithText("AT Schedule").assertDoesNotExist()
        rule.onNodeWithText("Referral List").assertDoesNotExist()

        rule.onNodeWithText("10-Day").performScrollTo().performClick()
        assertEquals(course, picked)
        // The Advanced Search tile opens the in-app screen, not the desk site.
        rule.onNodeWithText("Advanced Search").performScrollTo().performClick()
        assertTrue(advanced)
        assertNull(later)
        // The tile is native now: it invokes onCentreOps directly and never
        // reaches the desk-site onLater path.
        rule.onNodeWithText("Centre Settings").performScrollTo().performClick()
        assertTrue(ops)
        assertNull(later)
    }

    @Test
    fun centreSettingsRowIsReachableWithoutCourses() {
        // Centre settings must stay reachable when the centre has no
        // upcoming courses — the invariant survived the redesign; only the
        // widget (now the "Centre Settings" tile, not a standalone card)
        // changed. App Settings lives in the same grid under the same risk.
        var ops = false
        var settingsOpened = false
        rule.setContent {
            DipiTheme {
                CentreScreen(
                    session = session,
                    courses = emptyList(),
                    onPick = {},
                    onCentreOps = { ops = true },
                    onSettings = { settingsOpened = true },
                )
            }
        }
        rule.onNodeWithText("No upcoming courses.").assertIsDisplayed()
        rule.onNodeWithText("App Settings").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Centre Settings").performScrollTo().performClick()
        assertTrue(ops)
        rule.onNodeWithText("App Settings").performScrollTo().performClick()
        assertTrue(settingsOpened)
    }

    @Test
    fun olderCoursesListOpensTheBoard() {
        val older = Course(
            CourseId(8),
            CentreId(1),
            "Dhamma Sudha / 10 Day / 2026 / 6th-Aug to 17th-Aug",
            "2026-08-06",
            "2026-08-17",
        )
        var picked: Course? = null
        rule.setContent {
            DipiTheme {
                CentreScreen(
                    session = session,
                    courses = listOf(course),
                    onPick = { picked = it },
                    olderCourses = listOf(older),
                )
            }
        }
        rule.onNodeWithText("Older courses").performScrollTo().assertIsDisplayed()
        // Owner feedback 2026-08-27: the sub-line was redundant and is gone;
        // the deletion is proven, not merely untested.
        rule.onNodeWithText("Teacher list · valuables · seating — check-in is closed")
            .assertDoesNotExist()
        rule.onNodeWithText(older.name).performScrollTo().performClick()
        assertEquals(older, picked)
    }

    @Test
    fun matrixCardShowsOnlyTheThreeHighlightRows() {
        // A non-priority status row alongside the three highlights — the
        // card must render Received/Confirmed/Cancelled and omit this one.
        val withExtraRow = course.copy(
            matrix = matrix.copy(rows = matrix.rows + MatrixRow("Expected", newMale = 6)),
        )
        rule.setContent {
            DipiTheme {
                CentreScreen(session = session, courses = listOf(withExtraRow), onPick = {})
            }
        }
        rule.onNodeWithText("Received").assertIsDisplayed()
        rule.onNodeWithText("Confirmed").assertIsDisplayed()
        rule.onNodeWithText("Cancelled").assertIsDisplayed()
        rule.onNodeWithText("Expected").assertDoesNotExist()
    }

    @Test
    fun zeroMatrixCellRendersAsMiddot() {
        val zeroMatrix = CourseMatrix(
            rows = listOf(MatrixRow("Confirmed", newMale = 5, oldMale = 0, newFemale = 3, oldFemale = 2)),
        )
        val withZero = course.copy(matrix = zeroMatrix)
        rule.setContent {
            DipiTheme {
                CentreScreen(session = session, courses = listOf(withZero), onPick = {})
            }
        }
        // Exactly one blank cell (oldMale = 0) renders as a middot; the header
        // kicker row is a separate whole-string node and does not collide.
        rule.onAllNodesWithText("·").assertCountEquals(1)
    }

    @Test
    fun nullMatrixFallsBackToCountsLine() {
        val noMatrix = course.copy(matrix = null)
        rule.setContent {
            DipiTheme {
                CentreScreen(session = session, courses = listOf(noMatrix), onPick = {})
            }
        }
        rule.onNodeWithText("Confirmed 77 | Cancelled 7 | Received 2 | Total 111").assertIsDisplayed()
        rule.onNodeWithText("NM  OM  M  ·  NF  OF  F").assertDoesNotExist()
    }

    @Test
    fun matrixHeaderShowsGroupCapsAboveAllSixColumnLabels() {
        // v4 frame 1a: the header gains a MALE/FEMALE group-caps row over the
        // two trios; the six column labels are unchanged in name, only in
        // weight (M and F darker than NM/OM/NF/OF).
        rule.setContent {
            DipiTheme {
                CentreScreen(session = session, courses = listOf(course), onPick = {})
            }
        }
        rule.onNodeWithText("MALE").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("FEMALE").performScrollTo().assertIsDisplayed()
        listOf("NM", "OM", "M", "NF", "OF", "F").forEach {
            rule.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
        // The sevak count is its own mono suffix beside "Total" now, not part
        // of the label string — and it ellipsises rather than clipping
        // mid-glyph when the phone's label column runs out of room.
        rule.onNodeWithText("Total").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("+7 sevak").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun deskSiteChipsStillFireOnLaterWithTheSameTitleAndRoute() {
        // The five `action == null` entries render as pill chips under
        // MORE ON THE DESK SITE; each still hands `onLater` exactly the
        // (title, route) pair `centreDeskTiles` publishes.
        val deskSite = centreDeskTiles(1).filter { it.action == null }
        val fired = mutableListOf<Pair<String, String>>()
        rule.setContent {
            DipiTheme {
                CentreScreen(
                    session = session,
                    courses = listOf(course),
                    onPick = {},
                    onLater = { title, route -> fired += title to route },
                )
            }
        }
        rule.onNodeWithText("MORE ON THE DESK SITE").performScrollTo().assertIsDisplayed()
        deskSite.forEach { rule.onNodeWithText(it.title).performScrollTo().performClick() }
        assertEquals(deskSite.map { it.title to it.route }, fired)
    }

    @Test
    fun appSettingsTileInvokesOnSettings() {
        var settingsOpened = false
        rule.setContent {
            DipiTheme {
                CentreScreen(
                    session = session,
                    courses = listOf(course),
                    onPick = {},
                    onSettings = { settingsOpened = true },
                )
            }
        }
        rule.onNodeWithText("App Settings").performScrollTo().performClick()
        assertTrue(settingsOpened)
    }
}
