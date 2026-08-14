package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.dhamma.dipi.staff.model.Centre
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.settings.SettingsScreen
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun factoryResetAsksThenFires() {
        var wiped = false
        rule.setContent {
            DipiTheme {
                SettingsScreen(
                    session = Session(0, "sudha.user", "sudha.user", listOf(Centre(CentreId(1), "Dhamma Sudha")), false),
                    dark = false,
                    lastSync = null,
                    queued = 0,
                    offline = false,
                    onToggleTheme = {},
                    onLogout = {},
                    onFactoryReset = { wiped = true },
                )
            }
        }
        rule.onNodeWithText("Settings").assertIsDisplayed()
        rule.onNodeWithText("Erase all local data").assertIsDisplayed().performClick()
        rule.onNodeWithText("Erase everything on this tablet?").assertIsDisplayed()
        rule.onNodeWithText("Erase").performClick()
        assertTrue(wiped)
    }
}
