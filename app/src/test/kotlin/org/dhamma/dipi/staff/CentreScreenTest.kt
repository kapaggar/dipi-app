package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.dhamma.dipi.staff.course.CentreScreen
import org.dhamma.dipi.staff.course.centreDeskTiles
import org.dhamma.dipi.staff.course.courseCountsLine
import org.dhamma.dipi.staff.model.Centre
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.CourseSummary
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
    private val course = Course(
        CourseId(10),
        CentreId(1),
        "10-Day",
        "2026-08-20",
        "2026-08-31",
        summary = CourseSummary(received = 2, confirmed = 77, expected = 0, cancelled = 7, total = 111),
    )

    @Test
    fun catalogueOmitsAtAndReferralAndAddsLetters() {
        val titles = centreDeskTiles(1).map { it.title }
        assertTrue(titles.contains("Centre Settings"))
        assertTrue(titles.contains("Bulk Mail"))
        assertTrue(titles.contains("Letters"))
        assertEquals("letters/1", centreDeskTiles(1).first { it.title == "Letters" }.route)
        assertFalse(titles.any { it.contains("AT", ignoreCase = true) })
        assertFalse(titles.any { it.contains("Referral", ignoreCase = true) })
        assertEquals("centre/1/edit", centreDeskTiles(1).first { it.title == "Centre Settings" }.route)
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
        var daily = false
        var sms = false
        var manage = false
        var edit = false
        var letters = false
        var report = false
        rule.setContent {
            DipiTheme {
                CentreScreen(
                    session = session,
                    courses = listOf(course),
                    onPick = { picked = it },
                    onLater = { title, route -> later = title to route },
                    onCentreOps = { ops = true },
                    onAdvancedSearch = { advanced = true },
                    onDailyActivity = { daily = true },
                    onSmsReport = { sms = true },
                    onManageCourses = { manage = true },
                    onCentreEdit = { edit = true },
                    onLetters = { letters = true },
                    onCourseReport = { report = true },
                )
            }
        }
        rule.onNodeWithText("Upcoming courses").assertIsDisplayed()
        rule.onNodeWithText("10-Day").assertIsDisplayed()
        rule.onNodeWithText("Confirmed 77 | Cancelled 7 | Received 2 | Total 111").assertIsDisplayed()
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
        rule.onNodeWithText("Settings").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Letters").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Manage Letters").assertDoesNotExist()
        rule.onNodeWithText("AT Schedule").assertDoesNotExist()
        rule.onNodeWithText("Referral List").assertDoesNotExist()

        rule.onNodeWithText("10-Day").performScrollTo().performClick()
        assertEquals(course, picked)
        rule.onNodeWithText("Advanced Search").performScrollTo().performClick()
        assertTrue(advanced)
        assertNull(later)
        rule.onNodeWithText("Centre Settings").performScrollTo().performClick()
        assertTrue(edit)
        assertNull(later)
        rule.onNodeWithText("Daily Activity").performScrollTo().performClick()
        assertTrue(daily)
        rule.onNodeWithText("SMS Report").performScrollTo().performClick()
        assertTrue(sms)
        rule.onNodeWithText("Manage Courses").performScrollTo().performClick()
        assertTrue(manage)
        rule.onNodeWithText("Letters").performScrollTo().performClick()
        assertTrue(letters)
        rule.onNodeWithText("Course Report").performScrollTo().performClick()
        assertTrue(report)
        assertNull(later)
        rule.onNodeWithText("Centre settings").performScrollTo().performClick()
        assertTrue(ops)
    }

    @Test
    fun centreSettingsRowIsReachableWithoutCourses() {
        var ops = false
        rule.setContent {
            DipiTheme {
                CentreScreen(
                    session = session,
                    courses = emptyList(),
                    onPick = {},
                    onCentreOps = { ops = true },
                )
            }
        }
        rule.onNodeWithText("No upcoming courses.").assertIsDisplayed()
        rule.onNodeWithText("Centre settings").performScrollTo().performClick()
        assertTrue(ops)
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
        rule.onNodeWithText("Teacher list · valuables · seating — check-in is closed")
            .performScrollTo()
            .assertIsDisplayed()
        rule.onNodeWithText(older.name).performScrollTo().performClick()
        assertEquals(older, picked)
    }
}
