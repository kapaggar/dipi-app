package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.dhamma.dipi.staff.course.CourseHubLive
import org.dhamma.dipi.staff.course.CourseHubScreen
import org.dhamma.dipi.staff.course.courseHubDeskTiles
import org.dhamma.dipi.staff.course.courseHubTiles
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CourseHubScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val course = Course(CourseId(10), CentreId(1), "10-Day", "2026-08-20", "2026-08-31")

    @Test
    fun catalogueKeepsPhotoReviewOmitsAssignTeacher() {
        val tiles = courseHubTiles(1, 10)
        val titles = tiles.map { it.title }
        assertTrue(titles.contains("View Applications"))
        assertTrue(titles.contains("Photo review"))
        assertTrue(titles.contains("Day 0 summary"))
        assertTrue(titles.contains("Zero Day"))
        assertTrue(titles.contains("Audit applications"))
        assertTrue(titles.contains("Calling students"))
        assertTrue(titles.contains("Centre Settings"))
        assertFalse(titles.any { it.contains("Assign Teacher") })
        assertFalse(titles.any { it.contains("Referral") })
        assertFalse(titles.any { it.contains("Group-wise Seating") })
        assertFalse(titles.any { it.contains("Letter") })
        assertEquals(
            "search-course/1/10?s=&t=&g=&d=a",
            tiles.first { it.title == "View Applications" }.route,
        )
    }

    @Test
    fun catalogueSplitsNativeFlowsFromDeskSiteLinks() {
        val desk = courseHubDeskTiles(1, 10)
        assertTrue(desk.none { it.live != null })
        assertEquals("Add Application", desk.first().title)
        assertTrue(desk.any { it.title == "Course Summary Report" && it.route == "report-day11/1/10" })
        assertTrue(desk.any { it.title == "Seating Plan" && it.route == "seating/1/10" })
    }

    @Test
    fun hubOpensIntoNativeCardsWithCountsNoDeskRow() {
        var apps = 0
        var photos = 0
        var summary = 0
        var audit = 0
        var calling = 0
        var zero = 0
        var ops = 0
        var later: Pair<String, String>? = null
        rule.setContent {
            DipiTheme {
                CourseHubScreen(
                    course = course,
                    centreName = "Dhamma Sudha",
                    counts = mapOf(
                        CourseHubLive.Applications to 12,
                        CourseHubLive.ZeroDay to 3,
                        CourseHubLive.Audit to 2,
                        CourseHubLive.Calling to 5,
                    ),
                    onBack = {},
                    onApplications = { apps += 1 },
                    onSummary = { summary += 1 },
                    onPhotos = { photos += 1 },
                    onAudit = { audit += 1 },
                    onCalling = { calling += 1 },
                    onZeroDay = { zero += 1 },
                    onCentreOps = { ops += 1 },
                    onLater = { title, route -> later = title to route },
                )
            }
        }
        // The phone hub opens into native flows — the desk-site links are
        // not on the screen, only inside the ⋯ overflow menu.
        rule.onNodeWithText("During the course").assertIsDisplayed()
        rule.onNodeWithText("View Applications").assertIsDisplayed()
        rule.onNodeWithText("Zero Day").assertIsDisplayed()
        rule.onNodeWithText("Day 0 summary").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Photo review").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Audit applications").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Calling students").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Centre Settings").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Add Application").assertDoesNotExist()
        rule.onNodeWithText("Seating Plan").assertDoesNotExist()
        rule.onNodeWithText("Student Chit").assertDoesNotExist()
        rule.onNodeWithText("Male PDF").assertDoesNotExist()
        rule.onNodeWithText("Course Summary Report").assertDoesNotExist()
        rule.onNodeWithText("Assign Teacher").assertDoesNotExist()
        // Count chips: mono numerals on the cards that have derived counts.
        rule.onNodeWithText("12").assertIsDisplayed()
        rule.onNodeWithText("3").assertIsDisplayed()
        rule.onNodeWithText("2").assertIsDisplayed()
        rule.onNodeWithText("5").assertIsDisplayed()
        // Every native destination still fires.
        rule.onNodeWithText("View Applications").performClick()
        rule.onNodeWithText("Zero Day").performClick()
        rule.onNodeWithText("Day 0 summary").performScrollTo().performClick()
        rule.onNodeWithText("Photo review").performScrollTo().performClick()
        rule.onNodeWithText("Audit applications").performScrollTo().performClick()
        rule.onNodeWithText("Calling students").performScrollTo().performClick()
        rule.onNodeWithText("Centre Settings").performScrollTo().performClick()
        assertEquals(1, apps)
        assertEquals(1, zero)
        assertEquals(1, summary)
        assertEquals(1, photos)
        assertEquals(1, audit)
        assertEquals(1, calling)
        assertEquals(1, ops)
        assertEquals(null, later)
    }

    @Test
    fun overflowMenuReachesTheDeskSiteDestinations() {
        var later: Pair<String, String>? = null
        rule.setContent {
            DipiTheme {
                CourseHubScreen(
                    course = course,
                    centreName = "Dhamma Sudha",
                    onBack = {},
                    onApplications = {},
                    onSummary = {},
                    onPhotos = {},
                    onLater = { title, route -> later = title to route },
                )
            }
        }
        rule.onNodeWithText("Add Application").assertDoesNotExist()
        rule.onNodeWithContentDescription("Desk site links").performClick()
        rule.onNodeWithText("Add Application").assertIsDisplayed()
        rule.onNodeWithText("Course Summary Report").assertExists()
        rule.onNodeWithText("Add Application").performClick()
        assertEquals("Add Application" to "app/add/1/10", later)
    }
}
