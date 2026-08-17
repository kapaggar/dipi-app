package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.dhamma.dipi.staff.ui.ExitAppDialog
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExitAppDialogTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun asksWhetherToExitTotallyAndStayCancels() {
        var stayed = false
        var exited = false
        rule.setContent {
            DipiTheme {
                ExitAppDialog(onStay = { stayed = true }, onExit = { exited = true })
            }
        }
        rule.onNodeWithText("Exit DIPI Staff?").assertIsDisplayed()
        rule.onNodeWithText("Do you want to exit the app totally?").assertIsDisplayed()
        rule.onNodeWithText("Stay").performClick()
        assertTrue(stayed)
        assertEquals(false, exited)
    }

    @Test
    fun exitConfirms() {
        var exited = false
        rule.setContent {
            DipiTheme {
                ExitAppDialog(onStay = {}, onExit = { exited = true })
            }
        }
        rule.onNodeWithText("Exit").performClick()
        assertTrue(exited)
    }
}
