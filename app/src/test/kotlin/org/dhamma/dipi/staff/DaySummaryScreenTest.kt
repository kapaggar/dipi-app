package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.summary.DaySummaryScreen
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DaySummaryScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun card(id: Int, status: String, type: ApplicantType = ApplicantType.Student) = ApplicantCard(
        id = ApplicantId(id),
        centreId = CentreId(1),
        courseId = CourseId(10),
        givenName = "A$id",
        familyName = "B",
        gender = Gender.F,
        status = ApplicantStatus(status),
        type = type,
        oldStudent = false,
        attended = false,
    )

    @Test
    fun expectedRowsAreNotZeros() {
        val course = Course(
            CourseId(10),
            CentreId(1),
            "Dhamma Sudha / 10 Day / 2026 / 2nd-Sep to 13th-Sep",
            "",
            "",
        )
        rule.setContent {
            DipiTheme {
                DaySummaryScreen(
                    course = course,
                    rows = listOf(card(1, "Expected"), card(2, "Expected", ApplicantType.Sevak)),
                )
            }
        }
        rule.onNodeWithText("2 = 1 + 1").assertIsDisplayed()
        rule.onNodeWithText("Day 0 · 2 Sep 2026").assertIsDisplayed()
    }
}
