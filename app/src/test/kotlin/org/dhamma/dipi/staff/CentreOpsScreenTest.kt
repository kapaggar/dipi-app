package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.centerRight
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import org.dhamma.dipi.staff.course.CentreOpsScreen
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.CentreHallSettings
import org.dhamma.dipi.staff.model.CentreOpsPrefs
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.HallGrid
import org.dhamma.dipi.staff.model.HallSeatPlan
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
            "These settings belong to the centre and apply to every course the desk runs.",
        ).assertIsDisplayed()
        rule.onNodeWithText("Room chart").assertIsDisplayed()
        rule.onNodeWithText("CHECK-IN OPTIONS").assertIsDisplayed()
        rule.onNodeWithText(
            "Ask for laundry at check-in and include a laundry column on the Day 0 list.",
        ).assertIsDisplayed()
        rule.onNodeWithText("RESULT").assertIsDisplayed()
        rule.onNodeWithText(
            "Check-in: room, seating, laundry and valuables. " +
                "Hall: Main Dhamma Hall.",
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
        rule.onNodeWithText("Check-in: room, seating and group.").assertIsDisplayed()
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
        rule.onNodeWithText("Edit rooms on the desk site. Refreshes when you open this page.")
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
        rule.onNodeWithText("Reset message").performScrollTo().performClick()
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
        rule.onNodeWithText("Male hall").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Female hall").performScrollTo().assertIsDisplayed()
        rule.onAllNodesWithText("7 columns · 5 deep · A1 nearest the Dhamma seat")
            .assertCountEquals(2)
        rule.onNodeWithContentDescription("Increase columns · Male hall").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Decrease rows deep · Female hall").performScrollTo().performClick()
        // The header lines reflow instantly — but nothing reached persistence.
        rule.onNodeWithText("8 columns · 5 deep · A1 nearest the Dhamma seat").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("7 columns · 4 deep · A1 nearest the Dhamma seat").performScrollTo().assertIsDisplayed()
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

    @Test
    fun deskHallSettingsShowSudhaMainPlanAndLockDeskColumns() {
        val desk = CentreHallSettings(
            combinedHall = false,
            seatNaming = 1,
            maleSeatsPerRow = 5,
            femaleSeatsPerRow = 2,
            malePlan = HallSeatPlan(columns = 5, chowkyColumns = 1, direction = "right", chowkyPosition = ""),
            femalePlan = HallSeatPlan(columns = 2, chowkyColumns = 2, direction = "left", chowkyPosition = ""),
        )
        rule.setContent {
            DipiTheme {
                CentreOpsScreen(
                    prefs = CentreOpsPrefs(hallSettings = desk),
                    onToggleLaundry = {},
                    onToggleValuables = {},
                    onToggleGroups = {},
                    onOpenRooms = {},
                    onBack = {},
                )
            }
        }
        rule.onNodeWithText("Hall Settings").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Male/Female students in same hall").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("No").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Alphanumeric - columns A, B, C / rows 1, 2, 3")
            .performScrollTo()
            .assertIsDisplayed()
        rule.onNodeWithText("Male (Main Plan)").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("5 columns · 1 chowky · Left to right · Chowky Default (right) · Empty seats 0")
            .performScrollTo()
            .assertIsDisplayed()
        rule.onNodeWithText("Female (Main Plan)").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("2 columns · 2 chowky · Right to left · Chowky Default (left) · Empty seats 0")
            .performScrollTo()
            .assertIsDisplayed()
        rule.onNodeWithText("5 columns · 5 deep · A1 nearest the Dhamma seat").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("2 columns · 5 deep · A1 nearest the Dhamma seat").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("Increase columns · Male hall").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithContentDescription("Increase columns · Female hall").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithContentDescription("Increase rows deep · Male hall").performScrollTo().assertIsEnabled()
    }

    @Test
    fun twoHallCardsShowShippedStepperLabels() {
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
        rule.onNodeWithTag("hall-card-male").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("hall-card-female").performScrollTo().assertIsDisplayed()
        rule.onAllNodesWithText("Columns (A, B, C …)").assertCountEquals(2)
        rule.onAllNodesWithText("Rows (1 is nearest the teacher)").assertCountEquals(2)
        rule.onNodeWithText("HALL CHART").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText(
            "Seat labels outside these dimensions extend the grid rather than being dropped.",
        ).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("RESULT").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Room chart").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun toggleAccessibleNameIncludesWhatItTurnsOn() {
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
        rule.onNodeWithContentDescription(
            "Laundry. Ask for laundry at check-in and include a laundry column on the Day 0 list.",
        ).assertIsDisplayed()
        rule.onNodeWithContentDescription(
            "Valuables. Record valuables handed in at check-in. Items are listed on the checking slip, not on the printed roll.",
        ).assertIsDisplayed()
        rule.onNodeWithContentDescription(
            "Groups. Assign a sitting group at check-in. Groups still come from the desk site; this only shows the field at the desk.",
        ).assertIsDisplayed()
    }

    @Test
    fun togglesAndSteppersMeet48dp() {
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
        val laundry = rule.onNodeWithTag("toggle-laundry").getBoundsInRoot()
        assertTrue(laundry.height.value >= 48.dp.value)
        val valuables = rule.onNodeWithTag("toggle-valuables").getBoundsInRoot()
        assertTrue(valuables.height.value >= 48.dp.value)
        val groups = rule.onNodeWithTag("toggle-groups").getBoundsInRoot()
        assertTrue(groups.height.value >= 48.dp.value)
        val minus = rule.onNodeWithContentDescription("Decrease columns · Male hall")
            .performScrollTo()
            .getBoundsInRoot()
        assertTrue(minus.width.value >= 48.dp.value)
        assertTrue(minus.height.value >= 48.dp.value)
        val plus = rule.onNodeWithContentDescription("Increase rows deep · Female hall")
            .performScrollTo()
            .getBoundsInRoot()
        assertTrue(plus.width.value >= 48.dp.value)
        assertTrue(plus.height.value >= 48.dp.value)
    }

}
