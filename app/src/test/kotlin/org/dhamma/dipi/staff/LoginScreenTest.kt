package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.dhamma.dipi.staff.auth.LoginScreen
import org.dhamma.dipi.staff.ui.theme.DeskSkin
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Phone-sized display: the centred 380dp sign-in card needs more room than
// Robolectric's default 320x470 viewport, which cuts it off.
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class LoginScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun wordmarkFieldsAndNoUrlField() {
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
        rule.onNodeWithText("USERNAME").assertIsDisplayed()
        rule.onNodeWithText("PASSWORD").assertIsDisplayed()
        rule.onNodeWithText("SIGN IN").assertIsDisplayed()
        rule.onNodeWithText("Your centre is read from your account after sign-in.").assertIsDisplayed()
        rule.onNodeWithText("Remember me").assertIsDisplayed()
        rule.onNodeWithTag("login-lotus").assertExists()
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

    @Test
    fun loadingStateRelabelsTheButton() {
        rule.setContent {
            DipiTheme {
                LoginScreen("a", "b", null, true, {}, {}, {})
            }
        }
        rule.onNodeWithText("SIGNING IN…").assertIsDisplayed()
        rule.onNodeWithText("SIGN IN").assertDoesNotExist()
    }

    @Test
    fun lotusOffStillShowsTheFullForm() {
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
                    skin = DeskSkin.Pond,
                    lotus = false,
                )
            }
        }
        rule.onNodeWithText("DIPI Staff").assertIsDisplayed()
        rule.onNodeWithText("SIGN IN").assertIsDisplayed()
        rule.onNodeWithTag("login-lotus").assertDoesNotExist()
    }
}
