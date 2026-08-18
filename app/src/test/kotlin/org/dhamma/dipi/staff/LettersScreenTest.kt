package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.dhamma.dipi.staff.course.LettersScreen
import org.dhamma.dipi.staff.model.LetterRow
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LettersScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun listsLettersAndPreviewsBodyOnTap() {
        rule.setContent {
            DipiTheme {
                LettersScreen(
                    rows = listOf(
                        LetterRow("Confirmed", "Confirmed", "10-Day", "Your place is confirmed", "Dear meditator, your place is confirmed."),
                    ),
                    loading = false,
                    error = null,
                    onBack = {},
                )
            }
        }
        rule.onNodeWithText("Letters").assertIsDisplayed()
        rule.onNodeWithText("Confirmed").assertIsDisplayed()
        rule.onNodeWithText("Your place is confirmed").assertIsDisplayed()
        rule.onNodeWithText("Dear meditator, your place is confirmed.").assertDoesNotExist()
        rule.onNodeWithText("Confirmed").performClick()
        rule.onNodeWithText("Dear meditator, your place is confirmed.").assertIsDisplayed()
        rule.onNodeWithText("Edit").assertDoesNotExist()
        rule.onNodeWithText("Delete").assertDoesNotExist()
    }
}
