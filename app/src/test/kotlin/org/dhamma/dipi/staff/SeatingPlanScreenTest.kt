package org.dhamma.dipi.staff

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import okhttp3.mockwebserver.MockWebServer
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.HallGrid
import org.dhamma.dipi.staff.model.RollGroup
import org.dhamma.dipi.staff.model.RollRow
import org.dhamma.dipi.staff.model.RollSeniority
import org.dhamma.dipi.staff.model.SeatKind
import org.dhamma.dipi.staff.model.TabletMode
import org.dhamma.dipi.staff.model.TeacherRoll
import org.dhamma.dipi.staff.network.DipiMockDispatcher
import org.dhamma.dipi.staff.teacher.SeatingPlanScreen
import org.dhamma.dipi.staff.teacher.TeacherView
import org.dhamma.dipi.staff.ui.DeskScreen
import org.dhamma.dipi.staff.ui.DeskUiState
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Spec 2c S3 — `SeatingPlanScreen` (frame 2c) on the tablet window: legend
 * and cell fills (old/new/dashed-empty, the empty cell nameless), the FRONT
 * marker drawn exactly once, 58dp cells, seat tap → the same student card,
 * and hall switching as a pure state flip over the ONE fetched response —
 * never a refetch.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1240dp-h844dp-land")
class SeatingPlanScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun row(
        sn: Int,
        name: String,
        seat: String,
        roleTag: String? = null,
    ) = RollRow(
        sn = sn, name = name, roleTag = roleTag, room = "Mbk-$sn", age = "40", city = "Pune",
        courses = emptyList(), cell = "",
        seat = seat,
        seatKind = when {
            seat.startsWith("CW-", ignoreCase = true) -> SeatKind.CELL
            seat.startsWith("CH-", ignoreCase = true) -> SeatKind.CHAIR
            else -> SeatKind.FLOOR
        },
        backrest = false, occupation = "", education = "", languages = "",
    )

    private val maleOld = RollGroup(
        at = "Trainee-A-M Teacher", code = "TAM", gender = Gender.M,
        seniority = RollSeniority.OLD, group = "1", total = 3,
        rows = listOf(
            row(1, "Suresh Nair", "A1"),
            row(2, "Vikram Joshi", "CW-A3"),
            row(3, "Karan Velu", "", roleTag = "Sevak"),
        ),
    )
    private val maleNew = RollGroup(
        at = "(unassigned)", code = null, gender = Gender.M,
        seniority = RollSeniority.NEW, group = "2", total = 2,
        rows = listOf(
            row(1, "Rakesh Iyer", "B2"),
            row(2, "Arjun Patel", "CH-12"),
        ),
    )
    private val femaleOld = RollGroup(
        at = "Uma Rangan", code = "URN", gender = Gender.F,
        seniority = RollSeniority.OLD, group = "1", total = 1,
        rows = listOf(row(1, "Zara Bhosale", "A1")),
    )
    private val roll = TeacherRoll(listOf(maleOld, maleNew, femaleOld))

    @Test
    fun legendAndCellFillsFollowTheGroundTruthCorrection() {
        rule.setContent { DipiTheme { SeatingPlanScreen(roll = roll) } }
        // Legend labels present.
        rule.onNodeWithText("Old").assertIsDisplayed()
        rule.onNodeWithText("New").assertIsDisplayed()
        rule.onNodeWithText("Empty").assertIsDisplayed()
        // Old = the corrected accent100/accent300 cell; new; dashed empty.
        rule.onNodeWithTag("seat-cell-A1-old", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("seat-cell-B2-new", useUnmergedTree = true).assertExists()
        // An empty cell carries ONLY its synthesized id — no name line at all.
        rule.onNodeWithTag("seat-cell-A2-empty", useUnmergedTree = true)
            .onChildren()
            .assertCountEquals(1)
        rule.onNodeWithText("A2").assertIsDisplayed()
        // Names render on their seats.
        rule.onNodeWithText("Suresh Nair").assertIsDisplayed()
        rule.onNodeWithText("Rakesh Iyer").assertIsDisplayed()
    }

    @Test
    fun headerSubLineCarriesHallOrientationAndTally() {
        rule.setContent { DipiTheme { SeatingPlanScreen(roll = roll) } }
        // Male hall: 3 old (incl. cell + sevak) and 2 new — every row counts.
        rule.onNodeWithText("Male hall · facing the front · 3 old, 2 new").assertIsDisplayed()
    }

    @Test
    fun frontMarkerIsDrawnExactlyOnceAtTheTop() {
        rule.setContent { DipiTheme { SeatingPlanScreen(roll = roll) } }
        rule.onAllNodesWithTag("front-marker").assertCountEquals(1)
        rule.onAllNodesWithText("FRONT · DHAMMA SEAT").assertCountEquals(1)
        // Above the grid: the marker sits above row A's first cell.
        val marker = rule.onNodeWithTag("front-marker").getUnclippedBoundsInRoot()
        val a1 = rule.onNodeWithTag("seat-cell-A1-old").getUnclippedBoundsInRoot()
        assertTrue("front marker must sit above the seat grid", marker.bottom <= a1.top)
    }

    @Test
    fun seatCellsAre58dpTall() {
        rule.setContent { DipiTheme { SeatingPlanScreen(roll = roll) } }
        val cell = rule.onNodeWithTag("seat-cell-A1-old").getUnclippedBoundsInRoot()
        assertEquals(58f, (cell.bottom - cell.top).value, 0.5f)
        // The cell/pagoda column reuses the same 58dp cell.
        val cw = rule.onNodeWithTag("cell-seat-CW-A3").getUnclippedBoundsInRoot()
        assertEquals(58f, (cw.bottom - cw.top).value, 0.5f)
    }

    @Test
    fun cellPagodaColumnHoldsOnlyOccupiedCwAndChSeats() {
        rule.setContent { DipiTheme { SeatingPlanScreen(roll = roll) } }
        // CH- chairs join the column with their label shown (recorded ruling).
        rule.onNodeWithTag("cell-seat-CW-A3", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("cell-seat-CH-12", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("CELL / PAGODA").assertIsDisplayed()
        // Occupied only: exactly the two occupied cells, no empty slots drawn.
        rule.onNodeWithText("Vikram Joshi").assertIsDisplayed()
        rule.onNodeWithText("Arjun Patel").assertIsDisplayed()
    }

    @Test
    fun unseatedRowsKeepTheirReasonTag() {
        rule.setContent { DipiTheme { SeatingPlanScreen(roll = roll) } }
        rule.onNodeWithText("UNSEATED").assertIsDisplayed()
        rule.onNodeWithText("Karan Velu").assertIsDisplayed()
        rule.onNodeWithText("Sevak").assertIsDisplayed()
    }

    @Test
    fun seatTapSurfacesTheRowLikeAListRowTap() {
        var opened: RollRow? = null
        rule.setContent {
            DipiTheme { SeatingPlanScreen(roll = roll, onOpen = { opened = it }) }
        }
        rule.onNodeWithTag("seat-cell-A1-old").performClick()
        assertEquals("Suresh Nair", opened?.name)
        // The cell/pagoda column is a door into the same record too.
        rule.onNodeWithTag("cell-seat-CW-A3").performClick()
        assertEquals("Vikram Joshi", opened?.name)
    }

    @Test
    fun hallTabSwitchIsClientSideOverTheOneRollProp() {
        var hall by mutableStateOf(Gender.M)
        rule.setContent {
            DipiTheme {
                SeatingPlanScreen(roll = roll, hall = hall, onHall = { hall = it })
            }
        }
        rule.onNodeWithText("Suresh Nair").assertIsDisplayed()
        rule.onNodeWithTag("hall-tab-F").performClick()
        rule.waitForIdle()
        // Same response, other hall: the female group renders, the male goes.
        rule.onNodeWithText("Zara Bhosale").assertIsDisplayed()
        rule.onNodeWithText("Suresh Nair").assertDoesNotExist()
        rule.onNodeWithText("Female hall · facing the front · 1 old, 0 new").assertIsDisplayed()
    }

    @Test
    fun hallAndViewSwitchesNeverRefetchTheRoll() {
        // The endpoint mutates server data on GET (zeroize_new_course_data),
        // so the fetch-count seam is the mock server itself: after seeding a
        // roll, flipping hall and view must leave the request log EMPTY and
        // the state's roll the SAME instance.
        val server = MockWebServer().apply {
            dispatcher = DipiMockDispatcher()
            start()
        }
        try {
            val t = buildTestVm(server, pinPrefsName = "seating_no_refetch")
            t.vm.seedForTest(
                DeskUiState(
                    screen = DeskScreen.TeacherRoll,
                    mode = TabletMode.COURSE_OPS,
                    teacherRoll = roll,
                ),
            )
            t.vm.setTeacherView(TeacherView.SEATING)
            t.vm.setTeacherHall(Gender.F)
            t.vm.setTeacherHall(Gender.M)
            t.vm.setTeacherView(TeacherView.SENIORITY)
            rule.waitForIdle()
            assertEquals(0, server.requestCount)
            assertSame(roll, t.vm.state.value.teacherRoll)
            assertEquals(Gender.M, t.vm.state.value.teacherHall)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun gridExtensionRendersTheOutOfConfigSeat() {
        // A8 with a 7-wide config: the grid extends, A7 stays an empty dashed
        // cell, and the student is never dropped (data wins over config).
        val extended = TeacherRoll(
            listOf(maleOld.copy(rows = listOf(row(1, "Suresh Nair", "A8")), total = 1)),
        )
        rule.setContent {
            DipiTheme {
                SeatingPlanScreen(roll = extended, gridFor = { HallGrid(rows = 2, seatsPerRow = 7) })
            }
        }
        rule.onNodeWithTag("seat-cell-A8-old", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("seat-cell-A7-empty", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Suresh Nair").assertIsDisplayed()
    }
}
