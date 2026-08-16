package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.dhamma.dipi.staff.model.Centre
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.settings.SettingsScreen
import org.dhamma.dipi.staff.ui.theme.DeskSkin
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Phone-sized display: the skin switcher pushes "Erase all local data" below
// Robolectric's default 320x470 viewport.
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class SettingsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val session =
        Session(0, "sudha.user", "sudha.user", listOf(Centre(CentreId(1), "Dhamma Sudha")), false)

    @Test
    fun factoryResetAsksThenFires() {
        var wiped = false
        rule.setContent {
            DipiTheme {
                SettingsScreen(
                    session = session,
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

    @Test
    fun skinSwitcherListsAllFiveAndPicks() {
        var picked: DeskSkin? = null
        rule.setContent {
            DipiTheme {
                SettingsScreen(
                    session = session,
                    dark = false,
                    lastSync = null,
                    queued = 0,
                    offline = false,
                    onToggleTheme = {},
                    onLogout = {},
                    skin = DeskSkin.Steel,
                    onSkin = { picked = it },
                )
            }
        }
        rule.onNodeWithText("SKIN").assertIsDisplayed()
        DeskSkin.entries.forEach { s ->
            rule.onNodeWithText(s.label.uppercase()).assertIsDisplayed()
        }
        rule.onNodeWithText("Status colours stay put; they carry meaning, not mood.").assertIsDisplayed()
        rule.onNodeWithText("BLOSSOM").performClick()
        assertEquals(DeskSkin.Blossom, picked)
    }

    @Test
    fun lotusToggleFires() {
        var toggled = false
        rule.setContent {
            DipiTheme {
                SettingsScreen(
                    session = session,
                    dark = false,
                    lastSync = null,
                    queued = 0,
                    offline = false,
                    onToggleTheme = {},
                    onLogout = {},
                    lotus = true,
                    onToggleLotus = { toggled = true },
                )
            }
        }
        rule.onNodeWithText("Lotus watermark").assertIsDisplayed().performClick()
        assertTrue(toggled)
    }
}
