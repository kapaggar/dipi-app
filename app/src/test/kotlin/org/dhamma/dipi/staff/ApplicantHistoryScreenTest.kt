package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.dhamma.dipi.staff.applicants.CardScreen
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantClarificationRow
import org.dhamma.dipi.staff.model.ApplicantCourseRow
import org.dhamma.dipi.staff.model.ApplicantDeskHistory
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ApplicantHistoryScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val card = ApplicantCard(
        id = ApplicantId(1),
        centreId = CentreId(1),
        courseId = CourseId(10),
        givenName = "Meera",
        familyName = "Deshpande",
        gender = Gender.F,
        status = ApplicantStatus("Confirmed"),
        type = ApplicantType.Student,
        oldStudent = false,
        attended = false,
    )

    @Test
    fun expandFetchesAndClarificationOpens() {
        var expanded: String? = null
        var clar: Int? = null
        rule.setContent {
            DipiTheme {
                CardScreen(
                    card = card,
                    photoNote = "◎ Photo looks fine",
                    dark = false,
                    onChangeStatus = {},
                    onPhoto = {},
                    history = ApplicantDeskHistory(
                        courses = listOf(ApplicantCourseRow("10-Day · Aug 2026", "Student", "Confirmed", "False", "Pune")),
                        clarifications = listOf(
                            ApplicantClarificationRow("2026-08-12", "Please confirm travel date", "View", 3),
                        ),
                    ),
                    onExpandHistory = { expanded = it },
                    onOpenClarification = { clar = it },
                )
            }
        }
        rule.onNodeWithContentDescription("Expand Prior courses").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("10-Day · Aug 2026 · Student · Confirmed · False · Pune")
            .performScrollTo()
            .assertIsDisplayed()
        rule.onNodeWithContentDescription("Expand Activity").performScrollTo().performClick()
        assertEquals("activity", expanded)
        rule.onNodeWithText("Please confirm travel date").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Open PDF").performScrollTo().performClick()
        assertEquals(3, clar)
    }
}
