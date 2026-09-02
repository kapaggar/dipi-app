package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.dhamma.dipi.staff.applicants.CardScreen
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.ConfNo
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.SensitiveInfo
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class CardScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun card() = ApplicantCard(
        id = ApplicantId(31),
        centreId = CentreId(1),
        courseId = CourseId(10),
        givenName = "Priya",
        familyName = "Nair",
        gender = Gender.F,
        status = ApplicantStatus("Confirmed"),
        type = ApplicantType.Student,
        oldStudent = false,
        attended = false,
        confNo = ConfNo("NF31"),
        age = 34,
    )

    @Test
    fun idLabelAndNumberRender() {
        rule.setContent {
            DipiTheme {
                CardScreen(
                    card = card(),
                    photoNote = "Photo",
                    dark = false,
                    onChangeStatus = {},
                    onPhoto = {},
                    sensitive = SensitiveInfo(idLabel = "Aadhaar", idNumber = "1234 5678"),
                )
            }
        }
        rule.onNodeWithText("ID VERIFICATION").assertIsDisplayed()
        rule.onNodeWithText("Aadhaar").assertIsDisplayed()
        rule.onNodeWithText("1234 5678").assertIsDisplayed()
    }

    @Test
    fun noIdOnFileWhenAbsent() {
        rule.setContent {
            DipiTheme {
                CardScreen(
                    card = card(),
                    photoNote = "Photo",
                    dark = false,
                    onChangeStatus = {},
                    onPhoto = {},
                    sensitive = SensitiveInfo(),
                )
            }
        }
        rule.onNodeWithText("No ID on file").assertIsDisplayed()
    }

    @Test
    fun healthKeysRenderOnLocalDipiBlock() {
        rule.setContent {
            DipiTheme {
                CardScreen(
                    card = card(),
                    photoNote = "Photo",
                    dark = true,
                    onChangeStatus = {},
                    onPhoto = {},
                    sensitive = SensitiveInfo(
                        health = mapOf("Medication" to "thyroid", "Pregnancy" to "no"),
                    ),
                )
            }
        }
        rule.onNodeWithText("HEALTH · VERIFY WITH APPLICANT").assertIsDisplayed()
        rule.onNodeWithText("Medication").assertIsDisplayed()
        rule.onNodeWithText("thyroid").assertIsDisplayed()
        rule.onNodeWithText("Pregnancy").assertIsDisplayed()
    }
}
