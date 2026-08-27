package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.dhamma.dipi.staff.course.CentreOpsScreen
import org.dhamma.dipi.staff.model.CentreOpsPrefs
import org.dhamma.dipi.staff.ui.theme.DipiTheme
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
}
