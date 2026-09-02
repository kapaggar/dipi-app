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
 * `SeatingPlanScreen` on the r2 orientation (the live web page overrides
 * frame 2c's geometry): legend and cell fills, letters-as-columns with depth
 * row 1 at the bottom, the TEACHER · DHAMMA SEAT marker and column-letter
 * axis BELOW the grid, 66dp cells with ellipsized two-line names, the
 * CHOWKY / CHAIR rail in trailing-number-descending order (full-width below
 * the grid under 1000dp), sevaks hidden from UNSEATED without touching the
 * tally, seat tap → the same student card, and hall switching as a pure
 * state flip over the ONE fetched response — never a refetch.
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
        seniority = RollSeniority.NEW, group = "2", total = 3,
        rows = listOf(
            row(1, "Rakesh Iyer", "B2"),
            row(2, "Arjun Patel", "CH-12"),
            row(3, "Tara Singh", "", roleTag = "SAT-2011"),
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
        // Male hall: 3 old (incl. chowky + sevak) and 3 new — every row
        // counts, the hidden sevak included.
        rule.onNodeWithText("Male hall · facing the front · 3 old, 3 new").assertIsDisplayed()
    }

    @Test
    fun teacherMarkerAndColumnAxisSitBelowTheGrid() {
        rule.setContent { DipiTheme { SeatingPlanScreen(roll = roll) } }
        rule.onAllNodesWithTag("teacher-marker").assertCountEquals(1)
        rule.onAllNodesWithText("TEACHER · DHAMMA SEAT").assertCountEquals(1)
        // Nothing above the grid: the axis and the marker both sit BELOW the
        // bottom (depth 1) row.
        val marker = rule.onNodeWithTag("teacher-marker").getUnclippedBoundsInRoot()
        val axis = rule.onNodeWithTag("column-axis").getUnclippedBoundsInRoot()
        val a1 = rule.onNodeWithTag("seat-cell-A1-old").getUnclippedBoundsInRoot()
        assertTrue("column axis must sit below the seat grid", axis.top >= a1.bottom)
        assertTrue("teacher marker must sit below the column axis", marker.top >= axis.bottom)
    }

    @Test
    fun depthOneRendersBelowDepthTwo() {
        rule.setContent { DipiTheme { SeatingPlanScreen(roll = roll) } }
        // Depth descending: A1 (depth 1, nearest the teacher) draws BELOW
        // A2/B2 (depth 2).
        val a1 = rule.onNodeWithTag("seat-cell-A1-old").getUnclippedBoundsInRoot()
        val a2 = rule.onNodeWithTag("seat-cell-A2-empty").getUnclippedBoundsInRoot()
        val b2 = rule.onNodeWithTag("seat-cell-B2-new").getUnclippedBoundsInRoot()
        assertTrue("depth 1 must render below depth 2", a1.top >= a2.bottom)
        assertTrue("depth 1 must render below every depth-2 cell", a1.top >= b2.bottom)
    }

    @Test
    fun seatCellsAre66dpTall() {
        rule.setContent { DipiTheme { SeatingPlanScreen(roll = roll) } }
        val cell = rule.onNodeWithTag("seat-cell-A1-old").getUnclippedBoundsInRoot()
        assertEquals(66f, (cell.bottom - cell.top).value, 0.5f)
        // The chowky/chair rail reuses the same 66dp cell.
        val cw = rule.onNodeWithTag("chowky-seat-CW-A3").getUnclippedBoundsInRoot()
        assertEquals(66f, (cw.bottom - cw.top).value, 0.5f)
    }

    @Test
    fun longNamesEllipsizeInsideTheCellNeverAHardClip() {
        val long = TeacherRoll(
            listOf(maleOld.copy(rows = listOf(row(1, "Ravikiran Dhulipala Venkatasubramanian", "A1")), total = 1)),
        )
        rule.setContent { DipiTheme { SeatingPlanScreen(roll = long) } }
        val cell = rule.onNodeWithTag("seat-cell-A1-old").getUnclippedBoundsInRoot()
        val name = rule.onNodeWithText("Ravikiran Dhulipala Venkatasubramanian", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        // maxLines 2 + Ellipsis: the measured text never spills past the cell.
        assertTrue("name must stay inside its 66dp cell", name.bottom <= cell.bottom)
        assertTrue(name.top >= cell.top)
    }

    @Test
    fun chowkyChairRailHoldsOnlyOccupiedCwAndChSeats() {
        rule.setContent { DipiTheme { SeatingPlanScreen(roll = roll) } }
        // CH- chairs join the rail with their label shown (recorded ruling).
        rule.onNodeWithTag("chowky-seat-CW-A3", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("chowky-seat-CH-12", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("CHOWKY / CHAIR").assertIsDisplayed()
        // Occupied only: exactly the two occupied cells, no empty slots drawn.
        rule.onNodeWithText("Vikram Joshi").assertIsDisplayed()
        rule.onNodeWithText("Arjun Patel").assertIsDisplayed()
    }

    @Test
    fun chowkyChairRailOrdersTrailingNumbersDescending() {
        // Web ground truth: CW-A6 at the top, CW-A1 at the bottom, nearest
        // the teacher. 2-per-row rail: [CW-A6, CW-A3] then [CW-A1].
        val chowkies = TeacherRoll(
            listOf(
                maleOld.copy(
                    rows = listOf(
                        row(1, "Suresh Nair", "CW-A1"),
                        row(2, "Vikram Joshi", "CW-A6"),
                        row(3, "Rakesh Iyer", "CW-A3"),
                    ),
                    total = 3,
                ),
            ),
        )
        rule.setContent { DipiTheme { SeatingPlanScreen(roll = chowkies) } }
        val a6 = rule.onNodeWithTag("chowky-seat-CW-A6").getUnclippedBoundsInRoot()
        val a3 = rule.onNodeWithTag("chowky-seat-CW-A3").getUnclippedBoundsInRoot()
        val a1 = rule.onNodeWithTag("chowky-seat-CW-A1").getUnclippedBoundsInRoot()
        assertTrue("CW-A6 leads the first rail row", a6.left < a3.left)
        assertEquals(a6.top.value, a3.top.value, 0.5f)
        assertTrue("CW-A1 ends nearest the teacher", a1.top >= a6.bottom)
    }

    @Test
    fun sevaksHideFromUnseatedWhileOtherReasonsStillShow() {
        rule.setContent { DipiTheme { SeatingPlanScreen(roll = roll) } }
        rule.onNodeWithText("UNSEATED").assertIsDisplayed()
        // The non-sevak unseated row keeps its reason.
        rule.onNodeWithText("Tara Singh").assertIsDisplayed()
        rule.onNodeWithText("SAT-2011").assertIsDisplayed()
        // The sevak sits on a cushion the plan does not draw — no list row.
        rule.onNodeWithText("Karan Velu").assertDoesNotExist()
        // The tally is untouched — the sevak stays counted.
        rule.onNodeWithText("Male hall · facing the front · 3 old, 3 new").assertIsDisplayed()
    }

    @Test
    fun unseatedSectionVanishesWhenOnlySevaksAreUnseated() {
        val onlySevak = TeacherRoll(
            listOf(
                maleOld.copy(
                    rows = listOf(row(1, "Suresh Nair", "A1"), row(2, "Karan Velu", "", roleTag = "Sevak")),
                    total = 2,
                ),
            ),
        )
        rule.setContent { DipiTheme { SeatingPlanScreen(roll = onlySevak) } }
        rule.onNodeWithText("UNSEATED").assertDoesNotExist()
        rule.onNodeWithText("Karan Velu").assertDoesNotExist()
        // Still counted in the header tally.
        rule.onNodeWithText("Male hall · facing the front · 2 old, 0 new").assertIsDisplayed()
    }

    @Test
    fun seatTapSurfacesTheRowLikeAListRowTap() {
        var opened: RollRow? = null
        rule.setContent {
            DipiTheme { SeatingPlanScreen(roll = roll, onOpen = { opened = it }) }
        }
        rule.onNodeWithTag("seat-cell-A1-old").performClick()
        assertEquals("Suresh Nair", opened?.name)
        // The chowky/chair rail is a door into the same record too.
        rule.onNodeWithTag("chowky-seat-CW-A3").performClick()
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
        // A8 = column A depth 8 on a 2-deep config: the grid extends its
        // DEPTH, A7 stays an empty dashed cell, and the student is never
        // dropped (data wins over config).
        val extended = TeacherRoll(
            listOf(maleOld.copy(rows = listOf(row(1, "Suresh Nair", "A8")), total = 1)),
        )
        rule.setContent {
            DipiTheme {
                SeatingPlanScreen(roll = extended, gridFor = { HallGrid(columns = 7, depth = 2) })
            }
        }
        rule.onNodeWithTag("seat-cell-A8-old", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("seat-cell-A7-empty", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Suresh Nair").assertIsDisplayed()
        // Deeper draws HIGHER: A8 above A7, both above the teacher marker.
        val a8 = rule.onNodeWithTag("seat-cell-A8-old").getUnclippedBoundsInRoot()
        val a7 = rule.onNodeWithTag("seat-cell-A7-empty").getUnclippedBoundsInRoot()
        assertTrue(a7.top >= a8.bottom)
    }

    @Test
    @Config(qualifiers = "w900dp-h1240dp")
    fun portraitMovesTheChowkyChairRailFullWidthBelowTheGrid() {
        rule.setContent { DipiTheme { SeatingPlanScreen(roll = roll) } }
        val section = rule.onNodeWithTag("chowky-chair-section").getUnclippedBoundsInRoot()
        val marker = rule.onNodeWithTag("teacher-marker").getUnclippedBoundsInRoot()
        val a1 = rule.onNodeWithTag("seat-cell-A1-old").getUnclippedBoundsInRoot()
        // Below the grid (its bottom chrome included), above UNSEATED.
        assertTrue("rail must render below the grid", section.top >= a1.bottom)
        assertTrue("rail must render below the teacher marker", section.top >= marker.bottom)
        val unseated = rule.onNodeWithText("UNSEATED").getUnclippedBoundsInRoot()
        assertTrue("rail must sit above UNSEATED", unseated.top >= section.bottom)
        // Full width: 900dp window minus the 24dp gutters.
        assertEquals(852f, (section.right - section.left).value, 1f)
    }
}
