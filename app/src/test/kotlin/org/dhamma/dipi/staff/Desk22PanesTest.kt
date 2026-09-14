package org.dhamma.dipi.staff

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import org.dhamma.dipi.staff.desk.CallingPane
import org.dhamma.dipi.staff.desk.CheckInPane
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.model.ConfNo
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.RoomFeature
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w840dp-h1099dp-port", fontScale = 1.3f)
class Desk22PanesTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun compactCallingPushesDetailAndBothBackPathsRestoreTheList() {
        var dispatcher: androidx.activity.OnBackPressedDispatcher? = null
        rule.setContent {
            dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            DipiTheme {
                Box(Modifier.width(650.dp).height(900.dp)) {
                    CallingPane(
                        roll = listOf(card(1, "Priya", "Nair")), outcomes = emptyMap(), filter = "To call",
                        onFilter = {}, onOutcome = { _, _ -> }, onDial = {}, onWhatsApp = {}, onNote = { _, _ -> },
                    )
                }
            }
        }
        rule.onNodeWithText("Priya Nair").performClick()
        rule.onNodeWithText("Back to list").assertIsDisplayed()
        rule.onNodeWithText("9876543210").assertIsDisplayed()
        rule.onNodeWithText("Back to list").performClick()
        rule.onNodeWithText("Back to list").assertDoesNotExist()
        rule.onNodeWithText("Priya Nair").performClick()
        rule.runOnIdle { dispatcher?.onBackPressed() }
        rule.onNodeWithText("Back to list").assertDoesNotExist()
        rule.onNodeWithText("Priya Nair").assertIsDisplayed()
    }

    @Test
    fun compactCheckInKeepsNamesReadableAndSidebarReachableAtLargeFont() {
        val roll = listOf(card(1, "A very long applicant", "name that must remain readable"))
        rule.setContent {
            DipiTheme {
                Box(Modifier.width(650.dp).height(1050.dp)) {
                    CheckInPane(
                        roll = roll,
                        checkIns = emptyMap(),
                        rooms = listOf(AccoRoom("F21", Gender.F, "Fbk", RoomFeature())),
                        scan = "",
                        filter = "All",
                        flaggedIds = emptySet(),
                        onScan = {}, onFilter = {}, onOpen = {},
                        excludedStatusCounts = mapOf("Left" to 2, "Duplicate" to 1),
                    )
                }
            }
        }
        rule.onNodeWithText("A very long applicant name that must remain readable").assertIsDisplayed()
        val action = rule.onNodeWithTag("checkin-mark", useUnmergedTree = true)
        rule.onNodeWithTag("checkin-scroll").performScrollToNode(hasText("Mark attended"))
        action.assertIsDisplayed()
        assertTrue(action.getUnclippedBoundsInRoot().height.value >= 48f)
        rule.onNodeWithTag("checkin-scroll").performScrollToNode(hasText("THE ROLL"))
        rule.onNodeWithText("THE ROLL").assertIsDisplayed()
        rule.onNodeWithTag("checkin-scroll").performScrollToNode(hasText("SEATING ISSUED"))
        rule.onNodeWithText("SEATING ISSUED").assertIsDisplayed()
        rule.onNodeWithTag("checkin-scroll").performScrollToNode(hasText("NOT ON THIS LIST"))
        rule.onNodeWithText("NOT ON THIS LIST").assertIsDisplayed()
        rule.onNodeWithText("2 Left · excluded from arrivals").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun scopedCheckInRoomsAccountForOccupantsOutsideScope() {
        val newcomer = card(1, "New", "Applicant")
        val old = card(2, "Old", "Applicant").copy(confNo = ConfNo("OF2"))
        rule.setContent {
            DipiTheme {
                Box(Modifier.width(650.dp).height(1050.dp)) {
                    CheckInPane(roll = listOf(newcomer, old),
                        checkIns = mapOf(old.id to CheckInRecord(checkedIn = true, room = "F21")),
                        rooms = listOf(AccoRoom("F21", Gender.F, "Fbk", RoomFeature())),
                        scan = "", filter = "All", flaggedIds = emptySet(), gender = "Female", seniority = "New",
                        onScan = {}, onFilter = {}, onOpen = {})
                }
            }
        }
        rule.onNodeWithTag("checkin-scroll").performScrollToNode(hasText("ROOMS FREE"))
        rule.onNodeWithText("0 / 1").performScrollTo().assertIsDisplayed()
    }

    private fun card(id: Int, given: String, family: String) = ApplicantCard(
        id = ApplicantId(id), centreId = CentreId(1), courseId = CourseId(10),
        givenName = given, familyName = family, gender = Gender.F,
        status = ApplicantStatus("Confirmed"), type = ApplicantType.Student,
        oldStudent = false, attended = false, confNo = ConfNo("NF$id"),
        mobile = "9876543210", city = "Pune", age = 34,
    )
}
