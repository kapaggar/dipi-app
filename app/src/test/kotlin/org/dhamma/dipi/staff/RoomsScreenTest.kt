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

import org.dhamma.dipi.staff.course.RoomsScreen

@RunWith(RobolectricTestRunner::class)
class RoomsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun genderFilterShowsFemaleOnly() {
        rule.setContent {
            DipiTheme {
                RoomsScreen(
                    rooms = listOf(
                        AccoRoom("F32", Gender.F, "East"),
                        AccoRoom("M12", Gender.M, "West"),
                    ),
                    genderFilter = Gender.F,
                )
            }
        }
        rule.onNodeWithText("F32").assertIsDisplayed()
        rule.onNodeWithText("M12").assertDoesNotExist()
    }
}
