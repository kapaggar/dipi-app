package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.AuditFlag
import org.dhamma.dipi.staff.model.AuditSeverity
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.CentreOpsPrefs
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.MAIN_DHAMMA_HALL
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

import org.dhamma.dipi.staff.applicants.ZeroDayScreen

@RunWith(RobolectricTestRunner::class)
class ZeroDayScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val course = Course(CourseId(10), CentreId(1), "10-Day", "2026-08-20", "2026-08-31")

    private fun card(
        id: Int = 1,
        given: String = "Meera",
        family: String = "Deshpande",
        gender: Gender = Gender.F,
        status: String = "Confirmed",
        attended: Boolean = false,
        mobile: String? = "9876543210",
        flags: List<AuditFlag> = emptyList(),
    ) = ApplicantCard(
        id = ApplicantId(id),
        centreId = CentreId(1),
        courseId = CourseId(10),
        givenName = given,
        familyName = family,
        gender = gender,
        status = ApplicantStatus(status),
        type = ApplicantType.Student,
        oldStudent = true,
        attended = attended,
        mobile = mobile,
        flags = flags,
    )

    @Test
    fun seatingButtonsAndMainHallWhenGroupsOff() {
        var seat: String? = null
        rule.setContent {
            DipiTheme {
                ZeroDayScreen(
                    course = course,
                    rows = listOf(card()),
                    prefs = CentreOpsPrefs(laundry = true, valuables = true, groups = false),
                    onSeating = { _, s -> seat = s },
                )
            }
        }
        rule.onNodeWithText(MAIN_DHAMMA_HALL).assertIsDisplayed()
        rule.onNodeWithText("Chowky").assertIsDisplayed()
        rule.onNodeWithText("Chair").assertIsDisplayed()
        rule.onNodeWithText("Backrest").assertIsDisplayed()
        rule.onNodeWithText("None").assertIsDisplayed()
        rule.onNodeWithText("A").assertDoesNotExist()
        rule.onNodeWithText("Chowky").performClick()
        assertEquals("Chowky", seat)
    }

    @Test
    fun pullRoomsButtonIsVisibleAndFires() {
        var pulled = false
        rule.setContent {
            DipiTheme {
                ZeroDayScreen(
                    course = course,
                    rows = listOf(card()),
                    prefs = CentreOpsPrefs(),
                    onPullRooms = { pulled = true },
                )
            }
        }
        rule.onNodeWithText("Pull rooms").assertIsDisplayed().performClick()
        assertTrue(pulled)
    }
}
