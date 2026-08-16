package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.AuditFlag
import org.dhamma.dipi.staff.model.AuditSeverity
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.CentreOpsPrefs
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.MAIN_DHAMMA_HALL
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.dhamma.dipi.staff.course.CentreOpsScreen

@RunWith(RobolectricTestRunner::class)
class CentreOpsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun togglesAndAddF32() {
        rule.setContent {
            DipiTheme {
                var prefs by remember { mutableStateOf(CentreOpsPrefs(rooms = emptyList())) }
                CentreOpsScreen(
                    prefs = prefs,
                    onToggleLaundry = { prefs = prefs.copy(laundry = !prefs.laundry) },
                    onToggleValuables = {},
                    onToggleGroups = {},
                    onAddRooms = { g, s, codes ->
                        val add = CentreOpsPrefs.parseRoomCodes(codes).map { AccoRoom(it, g, s) }
                        prefs = prefs.copy(rooms = prefs.rooms + add)
                    },
                    onDeleteSection = { _, _ -> },
                    onOpenRooms = {},
                    onBack = {},
                )
            }
        }
        rule.onNodeWithText("Centre settings").assertIsDisplayed()
        rule.onNodeWithText("Laundry: on").assertIsDisplayed()
        rule.onNodeWithText("Laundry: on").performClick()
        rule.onNodeWithText("Laundry: off").assertIsDisplayed()
        rule.onNodeWithText("Section name").performTextInput("East")
        rule.onNodeWithText("Room codes").performTextInput("F32")
        rule.onNodeWithText("Add rooms").performClick()
        rule.onNodeWithText("F32").assertIsDisplayed()
    }
}
