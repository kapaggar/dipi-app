package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.centerRight
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import org.dhamma.dipi.staff.course.CentreOpsScreen
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.CentreOpsPrefs
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.HallGrid
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
        // The accommodation summary now sits under the WhatsApp-message card,
        // so it has to be scrolled to before it is on screen.
        rule.onNodeWithText("Room list comes from the desk site (Centre → Edit) and refreshes on sign-in.")
            .performScrollTo()
            .assertIsDisplayed()
        rule.onNodeWithText("2 rooms").performScrollTo().assertIsDisplayed()
        rule.onAllNodesWithText("Add rooms").assertCountEquals(0)
        rule.onAllNodesWithText("Delete").assertCountEquals(0)

        rule.onNodeWithText("Room chart").performScrollTo().performClick()
        assertTrue(openedRooms)
    }

    @Test
    fun whatsAppMessageIsCentreWritableAndPreviewsAgainstASampleApplicant() {
        var written: String? = null
        rule.setContent {
            DipiTheme {
                CentreOpsScreen(
                    prefs = CentreOpsPrefs(whatsAppTemplate = "Hi {name}, {course} starts {dates}."),
                    onToggleLaundry = {},
                    onToggleValuables = {},
                    onToggleGroups = {},
                    onOpenRooms = {},
                    onBack = {},
                    onWhatsAppTemplate = { written = it },
                )
            }
        }
        // The preview is the message as it will actually be sent.
        rule.onNodeWithText("Hi Rajat, 10 Day starts 2 Sep - 13 Sep.").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("whatsapp-template").performScrollTo().performTextInput("X")
        assertTrue(written!!.contains("Hi {name}"))
        // Reset hands back a blank template, which means the built-in default.
        rule.onNodeWithText("Reset to the default message").performScrollTo().performClick()
        assertEquals("", written)
    }

    // --- Hall chart (spec 2c S1) — same stage-then-SAVE flow as the room chart ---

    @Test
    fun hallChartSteppersStageLocallyWithoutPersisting() {
        val captured = mutableListOf<Pair<Gender, HallGrid>>()
        rule.setContent {
            DipiTheme {
                CentreOpsScreen(
                    prefs = CentreOpsPrefs(),
                    onToggleLaundry = {},
                    onToggleValuables = {},
                    onToggleGroups = {},
                    onOpenRooms = {},
                    onBack = {},
                    onHallGrid = { g, grid -> captured.add(g to grid) },
                )
            }
        }
        rule.onNodeWithText("Male hall · 7 columns · 5 deep").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Female hall · 7 columns · 5 deep").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("Increase columns · Male hall").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Decrease rows deep · Female hall").performScrollTo().performClick()
        // The header lines reflow instantly — but nothing reached persistence.
        rule.onNodeWithText("Male hall · 8 columns · 5 deep").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Female hall · 7 columns · 4 deep").performScrollTo().assertIsDisplayed()
        assertEquals(0, captured.size)
    }

    @Test
    fun hallChartSaveCommitsOnlyChangedGenders() {
        val captured = mutableListOf<Pair<Gender, HallGrid>>()
        rule.setContent {
            DipiTheme {
                CentreOpsScreen(
                    prefs = CentreOpsPrefs(),
                    onToggleLaundry = {},
                    onToggleValuables = {},
                    onToggleGroups = {},
                    onOpenRooms = {},
                    onBack = {},
                    onHallGrid = { g, grid -> captured.add(g to grid) },
                )
            }
        }
        rule.onNodeWithContentDescription("Increase columns · Male hall").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Increase columns · Male hall").performScrollTo().performClick()
        assertEquals(0, captured.size)
        rule.onNodeWithText("SAVE HALL LAYOUT").performScrollTo().performClick()
        // Only the touched hall persists, once, with the staged value.
        assertEquals(listOf(Gender.M to HallGrid(columns = 9, depth = 5)), captured)
    }

    @Test
    fun hallChartSaveDisabledWhenCleanEnabledWhenDirty() {
        rule.setContent {
            DipiTheme {
                CentreOpsScreen(
                    prefs = CentreOpsPrefs(),
                    onToggleLaundry = {},
                    onToggleValuables = {},
                    onToggleGroups = {},
                    onOpenRooms = {},
                    onBack = {},
                )
            }
        }
        rule.onNodeWithText("SAVE HALL LAYOUT").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithContentDescription("Increase rows deep · Male hall").performScrollTo().performClick()
        rule.onNodeWithText("SAVE HALL LAYOUT").performScrollTo().assertIsEnabled()
    }

    @Test
    fun hallChartSteppersDisableAtTheClampBounds() {
        val stored = CentreOpsPrefs()
            .withHallGrid(Gender.M, HallGrid(columns = HallGrid.MIN_COLUMNS, depth = HallGrid.MAX_DEPTH))
        rule.setContent {
            DipiTheme {
                CentreOpsScreen(
                    prefs = stored,
                    onToggleLaundry = {},
                    onToggleValuables = {},
                    onToggleGroups = {},
                    onOpenRooms = {},
                    onBack = {},
                )
            }
        }
        rule.onNodeWithContentDescription("Decrease columns · Male hall").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithContentDescription("Increase rows deep · Male hall").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithContentDescription("Increase columns · Male hall").performScrollTo().assertIsEnabled()
        rule.onNodeWithContentDescription("Decrease rows deep · Male hall").performScrollTo().assertIsEnabled()
    }
}
