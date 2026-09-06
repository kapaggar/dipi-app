package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
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

// Pixel-C-shaped display: v4 frame 1d puts settings in two columns (left flexes,
// right 428dp) at tablet width, and the whole page lands on one fold there — so
// every node is displayed without scrolling. The stacked branch is exercised at
// both ends of its range by [stacksOnAPhone] and [stacksInTheSevenHundredBand]
// (the 600–799dp band a 428dp right column cannot share), which override the
// qualifiers.
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1240dp-h844dp-land")
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
        rule.onNodeWithText("Status colours stay fixed.").assertIsDisplayed()
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

    /**
     * The lotus row is the codebase's single-fire switch row: the row carries
     * the `Role.Switch` semantics and the [androidx.compose.material3.Switch] is
     * display-only, so one tap is one call.
     */
    @Test
    fun lotusRowIsASwitchAndFiresOnce() {
        var toggles = 0
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
                    onToggleLotus = { toggles++ },
                )
            }
        }
        rule.onNodeWithTag("toggle-lotus").assertIsOn().performClick()
        assertEquals(1, toggles)
    }

    // ---- Theme: a two-way segmented control, not a label that reads like a link.

    @Test
    fun themeSegmentIsSelectedForTheLiveTheme() {
        rule.setContent {
            DipiTheme(dark = true) {
                SettingsScreen(
                    session = session,
                    dark = true,
                    lastSync = null,
                    queued = 0,
                    offline = false,
                    onToggleTheme = {},
                    onLogout = {},
                )
            }
        }
        rule.onNodeWithText("Dark").assertIsSelected()
        rule.onNodeWithText("Light").assertIsNotSelected()
    }

    @Test
    fun themeSegmentFiresOnceOnChangeAndNeverOnRetap() {
        var themeTaps = 0
        rule.setContent {
            DipiTheme {
                SettingsScreen(
                    session = session,
                    dark = false,
                    lastSync = null,
                    queued = 0,
                    offline = false,
                    onToggleTheme = { themeTaps++ },
                    onLogout = {},
                )
            }
        }
        rule.onNodeWithText("Light").assertIsSelected()
        // The other segment: exactly one call.
        rule.onNodeWithText("Dark").performClick()
        assertEquals(1, themeTaps)
        // The already-selected segment: a no-op, not a second toggle back.
        rule.onNodeWithText("Light").performClick()
        assertEquals(1, themeTaps)
    }

    // ---- Simulate offline: a real switch in a TESTING card.

    @Test
    fun offlineSwitchShowsStateAndFiresOnce() {
        var taps = 0
        rule.setContent {
            DipiTheme {
                SettingsScreen(
                    session = session,
                    dark = false,
                    lastSync = null,
                    queued = 0,
                    offline = true,
                    onToggleTheme = {},
                    onToggleOffline = { taps++ },
                    onLogout = {},
                )
            }
        }
        rule.onNodeWithText("Simulate offline").assertIsDisplayed()
        rule.onNodeWithTag("toggle-offline").assertIsOn().performClick()
        assertEquals(1, taps)
    }

    @Test
    fun offlineSwitchReadsOffWhenOnline() {
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
                )
            }
        }
        rule.onNodeWithTag("toggle-offline").assertIsOff()
    }

    // ---- Frame 1e: dark says out loud that it is the Steel night ramp.

    @Test
    fun darkModeExplainsTheNightRampAndKeepsTheSavedSkin() {
        rule.setContent {
            DipiTheme(dark = true) {
                SettingsScreen(
                    session = session,
                    dark = true,
                    lastSync = null,
                    queued = 0,
                    offline = false,
                    onToggleTheme = {},
                    onLogout = {},
                    skin = DeskSkin.Blossom,
                )
            }
        }
        rule.onNodeWithText("Dark theme uses Steel.").assertIsDisplayed()
        rule.onNodeWithText("APPLIES IN LIGHT").assertIsDisplayed()
        rule.onNodeWithText("SAVED").assertIsDisplayed()
        rule.onNodeWithText("NIGHT ACCENT").assertIsDisplayed()
        rule.onNodeWithText("NIGHT NEUTRALS").assertIsDisplayed()
    }

    @Test
    fun lightModeHasNoNightRampCallout() {
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
                    skin = DeskSkin.Blossom,
                )
            }
        }
        rule.onNodeWithText("Dark theme uses Steel.").assertDoesNotExist()
        rule.onNodeWithText("APPLIES IN LIGHT").assertDoesNotExist()
        rule.onNodeWithText("SAVED").assertDoesNotExist()
        rule.onNodeWithText("ACCENT 100–900").assertIsDisplayed()
        rule.onNodeWithText("NEUTRAL 100–900").assertIsDisplayed()
    }

    // ---- ACCOUNT & SESSION card.

    @Test
    fun sessionCardListsSyncQueueAndVersion() {
        rule.setContent {
            DipiTheme {
                SettingsScreen(
                    session = session,
                    dark = false,
                    lastSync = "11:57",
                    queued = 3,
                    offline = false,
                    onToggleTheme = {},
                    onLogout = {},
                    appVersion = "1.22.0",
                )
            }
        }
        rule.onNodeWithText("ACCOUNT & SESSION").assertIsDisplayed()
        rule.onNodeWithText("sudha.user").assertIsDisplayed()
        rule.onNodeWithText("Dhamma Sudha").assertIsDisplayed()
        rule.onNodeWithText("Last synced").assertIsDisplayed()
        rule.onNodeWithText("11:57").assertIsDisplayed()
        rule.onNodeWithText("Queue").assertIsDisplayed()
        rule.onNodeWithText("3 waiting").assertIsDisplayed()
        rule.onNodeWithText("App version").assertIsDisplayed()
        rule.onNodeWithText("1.22.0").assertIsDisplayed()
        rule.onNodeWithText("Log out").assertIsDisplayed()
    }

    /** `appVersion` is defaulted; the row stays out until someone supplies it. */
    @Test
    fun appVersionRowIsOmittedWhenBlank() {
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
                )
            }
        }
        rule.onNodeWithText("Queue").assertIsDisplayed()
        rule.onNodeWithText("App version").assertDoesNotExist()
    }

    /** A phone: the two columns stack into the one scrolling page. */
    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun stacksOnAPhone() {
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
                    appVersion = "1.22.0",
                )
            }
        }
        rule.onNodeWithText("APPEARANCE").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("TESTING").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("ACCOUNT & SESSION").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Erase all local data").performScrollTo().assertIsDisplayed()
    }

    /**
     * The 600–799dp band — 7"–9" tablets and split-screen. The right column is a
     * hard 428dp, so two columns leave the left one too narrow for the 258dp
     * ramp strips and the 169dp segmented control; the page stacks instead, and
     * every control stays on screen and operable.
     */
    @Test
    @Config(qualifiers = "w720dp-h1280dp")
    fun stacksInTheSevenHundredBand() {
        var themeTaps = 0
        var offlineTaps = 0
        var lotusTaps = 0
        rule.setContent {
            DipiTheme {
                SettingsScreen(
                    session = session,
                    dark = false,
                    lastSync = null,
                    queued = 0,
                    offline = false,
                    onToggleTheme = { themeTaps++ },
                    onToggleOffline = { offlineTaps++ },
                    onLogout = {},
                    lotus = true,
                    onToggleLotus = { lotusTaps++ },
                    appVersion = "1.22.0",
                )
            }
        }
        rule.onNodeWithText("Light").performScrollTo().assertIsSelected().assertIsDisplayed()
        rule.onNodeWithText("Dark").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(1, themeTaps)

        rule.onNodeWithTag("toggle-lotus").performScrollTo().assertIsDisplayed().assertIsOn().performClick()
        assertEquals(1, lotusTaps)

        rule.onNodeWithTag("toggle-offline").performScrollTo().assertIsDisplayed().assertIsOff().performClick()
        assertEquals(1, offlineTaps)

        rule.onNodeWithText("ACCOUNT & SESSION").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Erase all local data").performScrollTo().assertIsDisplayed()
        assertRampsAreNotSqueezed()
    }

    /** The same invariant in the two-column branch, where the left column is narrowest. */
    @Test
    fun rampStripIsNotSqueezedInTheTwoColumnLayout() {
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
                )
            }
        }
        assertRampsAreNotSqueezed()
    }

    /**
     * A ramp strip is nine 26dp swatches with 3dp gaps — a 258dp `Row` that does
     * not wrap, and the widest thing in the APPEARANCE card. Hand that card less
     * interior than 258dp and `Row` clamps itself to the constraint while its
     * children run past the clip, so the strip renders truncated. That is what a
     * two-column split does below ~788dp, since the right column is a hard
     * 428dp.
     *
     * `assertIsDisplayed` cannot see this — the truncated strip still intersects
     * the window — so the assertion is on the laid-out width instead.
     */
    private fun assertRampsAreNotSqueezed() {
        listOf("ramp-accent", "ramp-neutral").forEach { tag ->
            val bounds = rule.onNodeWithTag(tag).getUnclippedBoundsInRoot()
            val width = bounds.right - bounds.left
            assertTrue(
                "$tag was squeezed to $width; nine swatches and eight gaps need 258dp",
                width >= 258.dp,
            )
        }
    }
}
