package org.dhamma.dipi.staff

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.dhamma.dipi.staff.applicants.StatusSheetContent
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StatusSheetTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun offersDesignListNeverApproved() {
        rule.setContent {
            DipiTheme {
                StatusSheetContent(
                    current = "Pending",
                    choices = ApplicantStatus.SHEET_CHOICES,
                    pick = "Confirmed",
                    comment = "",
                    custom = "",
                    onPick = {},
                    onComment = {},
                    onCustom = {},
                    onConfirm = {},
                )
            }
        }
        rule.onNodeWithText("Pending → choose new status").assertExists()
        rule.onNodeWithText("Confirmed").assertExists()
        rule.onNodeWithText("Cancelled").assertExists()
        rule.onNodeWithText("Duplicate").assertExists()
        rule.onNodeWithText("Custom…").assertExists()
        rule.onNodeWithText("Less used").assertExists()
        rule.onNodeWithText("Confirm change").assertExists()
        rule.onNodeWithText("The server may send the applicant a letter for this change.", substring = true)
            .assertExists()
        assertTrue(ApplicantStatus.SHEET_CHOICES.contains("Duplicate"))
        assertFalse(ApplicantStatus.SHEET_CHOICES.any { it.equals("Approved", true) })
        rule.onNodeWithText("Approved").assertDoesNotExist()
    }
}
