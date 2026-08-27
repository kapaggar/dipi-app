package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.centerRight
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import org.dhamma.dipi.staff.course.CentreOpsScreen
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.CentreOpsPrefs
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class CentreOpsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val rooms = listOf(
        AccoRoom("Fbk 1", Gender.F, "Fbk", number = "1"),
        AccoRoom("Fbk 2", Gender.F, "Fbk", number = "2"),
        AccoRoom("Mbk 1", Gender.M, "Mbk", number = "1"),
    )

    @Test
    fun showsTheSubLineNotesAndDerivedResult() {
        rule.setContent {
            DipiTheme {
                CentreOpsScreen(
                    prefs = CentreOpsPrefs(laundry = true, valuables = true, groups = false),
                    onToggleLaundry = {},
                    onToggleValuables = {},
                    onToggleGroups = {},
                    onOpenRooms = {},
                    onBack = {},
                )
            }
        }
        rule.onNodeWithText("Centre settings").assertIsDisplayed()
        rule.onNodeWithText(
            "Three switches change what check-in asks for. " +
                "The line at the bottom shows the result.",
        ).assertIsDisplayed()
        rule.onNodeWithText("Check-in asks whether laundry was issued.").assertIsDisplayed()
        rule.onNodeWithText("RESULT").assertIsDisplayed()
        rule.onNodeWithText(
            "Check-in asks for room, seating, laundry and valuables. " +
                "Everyone sits in Main Dhamma Hall and Zero Day hides group chips.",
        ).assertIsDisplayed()
        rule.onNodeWithTag("toggle-laundry").assertIsOn()
        rule.onNodeWithTag("toggle-valuables").assertIsOn()
        rule.onNodeWithTag("toggle-groups").assertIsOff()
    }

    @Test
    fun resultFollowsTheSwitches() {
        rule.setContent {
            DipiTheme {
                CentreOpsScreen(
                    prefs = CentreOpsPrefs(laundry = false, valuables = false, groups = true),
                    onToggleLaundry = {},
                    onToggleValuables = {},
                    onToggleGroups = {},
                    onOpenRooms = {},
                    onBack = {},
                )
            }
        }
        rule.onNodeWithText("Check-in asks for room, seating and group.").assertIsDisplayed()
    }

    @Test
    fun tappingARowToggles() {
        var toggled = false
        rule.setContent {
            DipiTheme {
                CentreOpsScreen(
                    prefs = CentreOpsPrefs(),
                    onToggleLaundry = { toggled = true },
                    onToggleValuables = {},
                    onToggleGroups = {},
                    onOpenRooms = {},
                    onBack = {},
                )
            }
        }
        rule.onNodeWithText("Laundry").performClick()
        assertTrue(toggled)
    }

    @Test
    fun tappingTheSwitchThumbTogglesExactlyOnce() {
        var count = 0
        rule.setContent {
            DipiTheme {
                CentreOpsScreen(
                    prefs = CentreOpsPrefs(),
                    onToggleLaundry = { count++ },
                    onToggleValuables = {},
                    onToggleGroups = {},
                    onOpenRooms = {},
                    onBack = {},
                )
            }
        }
        // Tap at the row's trailing edge — where the Switch thumb renders —
        // rather than the row's text. The row carries a single `toggleable`
        // (the decorative Switch has onCheckedChange = null), so this must
        // fire the callback exactly once; a regression to two competing
        // toggle handlers would fire twice and this count would catch it.
        rule.onNodeWithTag("toggle-laundry").performTouchInput { click(centerRight) }
        assertEquals(1, count)
    }

    @Test
    fun accommodationSummaryIsReadOnly() {
        var openedRooms = false
        rule.setContent {
            DipiTheme {
                CentreOpsScreen(
                    prefs = CentreOpsPrefs(rooms = rooms),
                    onToggleLaundry = {},
                    onToggleValuables = {},
                    onToggleGroups = {},
                    onOpenRooms = { openedRooms = true },
                    onBack = {},
                )
            }
        }
        rule.onNodeWithText("Room list comes from the desk site (Centre → Edit) and refreshes on sign-in.")
            .assertIsDisplayed()
        rule.onNodeWithText("2 rooms").assertIsDisplayed()
        rule.onAllNodesWithText("Add rooms").assertCountEquals(0)
        rule.onAllNodesWithText("Delete").assertCountEquals(0)

        rule.onNodeWithText("Room chart").performClick()
        assertTrue(openedRooms)
    }
}
