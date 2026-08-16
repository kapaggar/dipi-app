package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.dhamma.dipi.staff.course.DESK_ACTION_PLACEHOLDER
import org.dhamma.dipi.staff.course.DeskActionScreen
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeskActionScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun showsTitleRouteAndPlaceholderCopy() {
        rule.setContent {
            DipiTheme {
                DeskActionScreen(title = "Centre Settings", route = "centre/1/edit")
            }
        }
        rule.onNodeWithText("Centre Settings").assertIsDisplayed()
        rule.onNodeWithText("centre/1/edit").assertIsDisplayed()
        rule.onNodeWithText(DESK_ACTION_PLACEHOLDER).assertIsDisplayed()
        rule.onNodeWithText("Back").assertIsDisplayed()
    }
}
