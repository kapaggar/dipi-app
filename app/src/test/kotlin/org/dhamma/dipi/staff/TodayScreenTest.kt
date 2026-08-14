package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.dhamma.dipi.staff.applicants.TodayScreen
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.ConfNo
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TodayScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val course = Course(CourseId(10), CentreId(1), "10-Day", "2026-08-20", "2026-08-31")
    private val meera = ApplicantCard(
        id = ApplicantId(1),
        centreId = CentreId(1),
        courseId = CourseId(10),
        givenName = "Meera",
        familyName = "Deshpande",
        gender = Gender.F,
        status = ApplicantStatus("Confirmed"),
        type = ApplicantType.Student,
        oldStudent = true,
        attended = false,
        confNo = ConfNo("NF128"),
        city = "Pune",
        state = "Maharashtra",
        country = "India",
        age = 34,
    )

    @Test
    fun chipsAndRowAndEmpty() {
        val toggled = mutableListOf<String>()
        rule.setContent {
            DipiTheme {
                TodayScreen(
                    course = course,
                    centreName = "Dhamma Sudha",
                    query = "",
                    onQuery = {},
                    counts = mapOf("All" to 214, "Pending" to 61, "Confirmed" to 72),
                    selected = emptySet(),
                    onToggleStatus = { toggled += it },
                    rows = listOf(meera),
                    queued = emptyMap(),
                    loading = false,
                    dark = false,
                    onOpen = {},
                    onSummary = {},
                    onPhotos = {},
                    onSettings = {},
                )
            }
        }
        rule.onNodeWithText("Meera Deshpande").assertIsDisplayed()
        rule.onNodeWithText("NF128").assertIsDisplayed()
        rule.onNodeWithText("All 214").assertIsDisplayed()
        rule.onNodeWithText("Pending 61").performClick()
        assertEquals(listOf("Pending"), toggled)
    }

    @Test
    fun queuedSuffixAndEmptyState() {
        rule.setContent {
            DipiTheme {
                TodayScreen(
                    course = course,
                    centreName = "Dhamma Sudha",
                    query = "zzz",
                    onQuery = {},
                    counts = emptyMap(),
                    selected = setOf("Cancelled"),
                    onToggleStatus = {},
                    rows = emptyList(),
                    queued = mapOf(ApplicantId(1) to "Confirmed"),
                    loading = false,
                    dark = false,
                    onOpen = {},
                    onSummary = {},
                    onPhotos = {},
                    onSettings = {},
                )
            }
        }
        rule.onNodeWithText("No applicants match those filters.").assertIsDisplayed()
    }

    @Test
    fun queuedRowShowsSuffix() {
        var opened = false
        rule.setContent {
            DipiTheme {
                TodayScreen(
                    course = course,
                    centreName = "Dhamma Sudha",
                    query = "",
                    onQuery = {},
                    counts = emptyMap(),
                    selected = emptySet(),
                    onToggleStatus = {},
                    rows = listOf(meera),
                    queued = mapOf(meera.id to "Confirmed"),
                    loading = false,
                    dark = false,
                    onOpen = { opened = true },
                    onSummary = {},
                    onPhotos = {},
                    onSettings = {},
                )
            }
        }
        rule.onNodeWithText("queued: → Confirmed", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Meera Deshpande").performClick()
        assertTrue(opened)
    }
}
