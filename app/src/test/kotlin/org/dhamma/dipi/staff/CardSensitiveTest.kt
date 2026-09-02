package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.height
import org.dhamma.dipi.staff.applicants.CardScreen
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.SensitiveInfo
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class CardSensitiveTest {
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

    private fun show(sensitive: SensitiveInfo? = null) {
        rule.setContent {
            DipiTheme {
                CardScreen(
                    card = card,
                    photoNote = "Photo",
                    dark = false,
                    onChangeStatus = {},
                    onPhoto = {},
                    sensitive = sensitive,
                )
            }
        }
    }

    @Test
    fun healthPanelRendersDisclosures() {
        show(
            SensitiveInfo(
                idLabel = "Aadhaar",
                idNumber = "1234 5678 9012",
                health = mapOf("Medication" to "Insulin, morning"),
            ),
        )
        listOf("HEALTH · VERIFY WITH APPLICANT", "Medication", "Insulin, morning").forEach { label ->
            rule.onNodeWithText(label).assertIsDisplayed()
            assertTrue(rule.onNodeWithText(label).getUnclippedBoundsInRoot().height.value >= 10f)
        }
    }

    @Test
    fun idBlockRendersLabelAndNumber() {
        show(SensitiveInfo(idLabel = "Aadhaar", idNumber = "1234 5678 9012"))
        rule.onNodeWithText("ID VERIFICATION").assertIsDisplayed()
        rule.onNodeWithText("Aadhaar").assertIsDisplayed()
        rule.onNodeWithText("1234 5678 9012").assertIsDisplayed()
    }

    @Test
    fun nullSensitiveShowsNoIdOnFile() {
        show(null)
        rule.onNodeWithText("No ID on file").assertIsDisplayed()
        rule.onNodeWithText("HEALTH · VERIFY WITH APPLICANT").assertDoesNotExist()
    }

    @Test
    fun emptyHealthHidesPanel() {
        show(SensitiveInfo(idLabel = "PAN", idNumber = "X", health = emptyMap()))
        rule.onNodeWithText("HEALTH · VERIFY WITH APPLICANT").assertDoesNotExist()
        rule.onNodeWithText("PAN").assertIsDisplayed()
    }
}
