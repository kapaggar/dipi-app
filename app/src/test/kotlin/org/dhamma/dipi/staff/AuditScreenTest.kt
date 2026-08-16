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

import org.dhamma.dipi.staff.applicants.AuditScreen

@RunWith(RobolectricTestRunner::class)
class AuditScreenTest {
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
    fun showsFlaggedRow() {
        var opened = false
        val flagged = card(
            flags = listOf(
                AuditFlag(AuditSeverity.HARD, "Honorific left in the name field", "name_title_prefix", "name_title_prefix"),
            ),
        )
        rule.setContent {
            DipiTheme {
                AuditScreen(rows = listOf(flagged), onOpen = { opened = true })
            }
        }
        rule.onNodeWithText("Meera Deshpande").assertIsDisplayed()
        rule.onNodeWithText("Honorific left in the name field").assertIsDisplayed()
        rule.onNodeWithText("Meera Deshpande").performClick()
        assertTrue(opened)
    }
}
