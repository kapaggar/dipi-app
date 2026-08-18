package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.dhamma.dipi.staff.course.CentreEditScreen
import org.dhamma.dipi.staff.model.CentreFormSettings
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CentreEditScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun showsFormValuesAndNoSave() {
        rule.setContent {
            DipiTheme {
                CentreEditScreen(
                    settings = CentreFormSettings(
                        name = "Dhamma Sudha",
                        address = "Igatpuri Road",
                        email = "info@sudha.dhamma.org",
                        reconf = true,
                        reconfDays = "10",
                    ),
                    loading = false,
                    error = null,
                    onBack = {},
                )
            }
        }
        rule.onNodeWithText("Centre Settings").assertIsDisplayed()
        rule.onNodeWithText("Dhamma Sudha").assertIsDisplayed()
        rule.onNodeWithText("Igatpuri Road").assertIsDisplayed()
        rule.onNodeWithText("info@sudha.dhamma.org").assertIsDisplayed()
        rule.onNodeWithText("Yes · 10 days before start").assertIsDisplayed()
        rule.onNodeWithText("Save").assertDoesNotExist()
    }
}
