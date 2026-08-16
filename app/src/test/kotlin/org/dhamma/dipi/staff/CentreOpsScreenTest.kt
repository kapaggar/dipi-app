package org.dhamma.dipi.staff

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.dhamma.dipi.staff.course.CentreOpsScreen
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.CentreOpsPrefs
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CentreOpsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val rooms = listOf(
        AccoRoom("Fbk 1", Gender.F, "Fbk", number = "1"),
        AccoRoom("Fbk 2", Gender.F, "Fbk", number = "2"),
        AccoRoom("Mbk 1", Gender.M, "Mbk", number = "1"),
    )

    @Test
    fun togglesWorkAndRoomsAreReadOnly() {
        var openedRooms = false
        rule.setContent {
            DipiTheme {
                var prefs by remember { mutableStateOf(CentreOpsPrefs(rooms = rooms)) }
                CentreOpsScreen(
                    prefs = prefs,
                    onToggleLaundry = { prefs = prefs.copy(laundry = !prefs.laundry) },
                    onToggleValuables = {},
                    onToggleGroups = {},
                    onOpenRooms = { openedRooms = true },
                    onBack = {},
                )
            }
        }
        rule.onNodeWithText("Centre settings").assertIsDisplayed()
        rule.onNodeWithText("Laundry: on").performClick()
        rule.onNodeWithText("Laundry: off").assertIsDisplayed()

        // Server-derived accommodation summary, no add/delete controls.
        rule.onNodeWithText("Room list comes from the desk site (Centre → Edit) and refreshes on sign-in.")
            .assertIsDisplayed()
        rule.onNodeWithText("2 rooms").assertIsDisplayed()
        rule.onAllNodesWithText("Add rooms").assertCountEquals(0)
        rule.onAllNodesWithText("Delete").assertCountEquals(0)

        rule.onNodeWithText("Room chart").performClick()
        assertTrue(openedRooms)
    }
}
