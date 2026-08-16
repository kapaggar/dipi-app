package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.dhamma.dipi.staff.course.CourseHubScreen
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
    fun hubShowsLiveAndPlaceholderTiles() {
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
        rule.onNodeWithText("View Applications").assertIsDisplayed()
        rule.onNodeWithText("Add Application").assertIsDisplayed()
        rule.onNodeWithText("Photo review").assertIsDisplayed()
        rule.onNodeWithText("Audit applications").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Calling students").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Day 0 summary").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Zero Day").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Seating Plan").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Assign Teacher").assertDoesNotExist()
        rule.onNodeWithText("Group-wise Seating").assertDoesNotExist()
        rule.onNodeWithText("Referral").assertDoesNotExist()
        rule.onNodeWithText("Letters").assertDoesNotExist()
        rule.onNodeWithText("View Applications").performScrollTo().performClick()
        rule.onNodeWithText("Photo review").performScrollTo().performClick()
        rule.onNodeWithText("Day 0 summary").performScrollTo().performClick()
        rule.onNodeWithText("Zero Day").performScrollTo().performClick()
        rule.onNodeWithText("Audit applications").performScrollTo().performClick()
        rule.onNodeWithText("Calling students").performScrollTo().performClick()
        rule.onNodeWithText("Add Application").performScrollTo().performClick()
        assertEquals(1, apps)
        assertEquals(1, photos)
        assertEquals(1, summary)
        assertEquals(1, zero)
        assertEquals(1, audit)
        assertEquals(1, calling)
        assertEquals("Add Application" to "app/add/1/10", later)
    }
}
