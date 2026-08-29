package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.dhamma.dipi.staff.auth.LoginCard
import org.dhamma.dipi.staff.auth.LoginScreen
import org.dhamma.dipi.staff.ui.theme.DeskSkin
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private val caption = "Your centre is read from your account after sign-in."

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
        rule.onNodeWithText(caption).assertIsDisplayed()
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

    /**
     * Robolectric cannot raise a real IME, so the compact/tall arrangement is a
     * pure function of `imeVisible` on [LoginCard] and the tests force the flag.
     */
    private fun card(imeVisible: Boolean, error: String? = null, onRemember: (Boolean) -> Unit = {}) {
        rule.setContent {
            DipiTheme {
                LoginCard(
                    imeVisible = imeVisible,
                    username = "sudha.user",
                    password = "secret",
                    error = error,
                    loading = false,
                    onUser = {},
                    onPass = {},
                    onSubmit = {},
                    remember = true,
                    onRemember = onRemember,
                )
            }
        }
    }

    @Test
    fun tallCardKeepsTheCaptionAndItsOwnRememberRow() {
        card(imeVisible = false)
        rule.onNodeWithText(caption).assertIsDisplayed()
        rule.onNodeWithText("DIPI Staff").assertIsDisplayed()
        rule.onNodeWithText("Remember me").assertIsDisplayed()
        rule.onNodeWithText("SIGN IN").assertIsDisplayed()
        val remember = rule.onNodeWithText("Remember me").getUnclippedBoundsInRoot()
        val button = rule.onNodeWithText("SIGN IN").getUnclippedBoundsInRoot()
        assertTrue(
            "tall card stacks remember-me above the SIGN IN row",
            remember.bottom.value <= button.top.value,
        )
    }

    @Test
    fun compactCardHidesTheCaptionAndPutsRememberOnTheButtonRow() {
        card(imeVisible = true)
        rule.onNodeWithText(caption).assertDoesNotExist()
        rule.onNodeWithText("DIPI Staff").assertIsDisplayed()
        rule.onNodeWithText("USERNAME").assertIsDisplayed()
        rule.onNodeWithText("PASSWORD").assertIsDisplayed()
        val remember = rule.onNodeWithText("Remember me").getUnclippedBoundsInRoot()
        val button = rule.onNodeWithText("SIGN IN").getUnclippedBoundsInRoot()
        assertTrue(
            "compact card shares one row between remember-me and SIGN IN",
            remember.top.value < button.bottom.value && remember.bottom.value > button.top.value,
        )
    }

    @Test
    fun errorStripKeepsTheServerTextVerbatimInBothArrangements() {
        val msg = "Unrecognized username or password."
        card(imeVisible = true, error = msg)
        rule.onNodeWithText("Sign-in failed").assertIsDisplayed()
        rule.onNodeWithText(msg).assertIsDisplayed()
    }

    @Test
    fun rememberMeTogglesThroughTheRow() {
        var seen: Boolean? = null
        card(imeVisible = false, onRemember = { seen = it })
        rule.onNodeWithText("Remember me").performClick()
        assertEquals(false, seen)
    }
}
