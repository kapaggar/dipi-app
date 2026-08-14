package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.dhamma.dipi.staff.auth.LoginScreen
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LoginScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun wordmarkAndNoUrlField() {
        rule.setContent {
            DipiTheme {
                LoginScreen(
                    username = "",
                    password = "",
                    error = null,
                    loading = false,
                    onUser = {},
                    onPass = {},
                    onSubmit = {},
                )
            }
        }
        rule.onNodeWithText("DIPI Staff").assertIsDisplayed()
        rule.onNodeWithText("Centre admin desk").assertIsDisplayed()
        rule.onNodeWithText("Sign in").assertIsDisplayed()
        rule.onNodeWithText("Your centre is read from your account after sign-in.").assertIsDisplayed()
        rule.onNodeWithText("Remember me").assertIsDisplayed()
        rule.onNodeWithText("https://", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Server URL", substring = true).assertDoesNotExist()
    }

    @Test
    fun showsServerErrorVerbatim() {
        val msg = "Please Edit application and choose Area teacher before approving!"
        rule.setContent {
            DipiTheme {
                LoginScreen("a", "b", msg, false, {}, {}, {})
            }
        }
        rule.onNodeWithText(msg).assertIsDisplayed()
    }
}
