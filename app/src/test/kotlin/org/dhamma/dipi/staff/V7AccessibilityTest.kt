package org.dhamma.dipi.staff

import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import org.dhamma.dipi.staff.desk.AuditPane
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.AuditFlag
import org.dhamma.dipi.staff.model.AuditSeverity
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.ConfNo
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w1280dp-h800dp")
class V7AccessibilityTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun auditOpenTargetIsAtLeastFortyEightDp() {
        val card = ApplicantCard(
            id = ApplicantId(7),
            centreId = CentreId(1),
            courseId = CourseId(10),
            givenName = "Synthetic",
            familyName = "Applicant",
            gender = Gender.F,
            status = ApplicantStatus("Confirmed"),
            type = ApplicantType.Student,
            oldStudent = false,
            attended = false,
            confNo = ConfNo("NF7"),
            flags = listOf(
                AuditFlag(AuditSeverity.HARD, "Duplicate confirmation", "NF7", "conf_no_duplicate"),
            ),
        )
        rule.setContent {
            DipiTheme {
                AuditPane(
                    flagged = listOf(card),
                    selectedCode = "conf_no_duplicate",
                    onSelect = {},
                    onBatch = { _, _ -> },
                    onOpen = {},
                )
            }
        }
        rule.onNodeWithText("Open").assertExists()
        val box = rule.onNodeWithTag("audit-open-7", useUnmergedTree = true).getBoundsInRoot()
        assertTrue("Open height ${box.height}", box.height.value >= 48.dp.value)
        assertTrue("Open width ${box.width}", box.width.value >= 48.dp.value)
    }
}
