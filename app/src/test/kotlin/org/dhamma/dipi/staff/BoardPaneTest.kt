package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import org.dhamma.dipi.staff.desk.BoardPane
import org.dhamma.dipi.staff.desk.DeskSection
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.AuditFlag
import org.dhamma.dipi.staff.model.AuditSeverity
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.model.ConfNo
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 1.37.2: SHEETS & EXPORTS is a neat 3×3 of equal cells. No shelf headers,
 * no full-width Day-11 row. Course summary sits in the grid. Valuable list
 * left the Board. Male/Female PDF stay gone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1240dp-h844dp-land")
class BoardPaneTest {
    @get:Rule
    val rule = createComposeRule()

    private fun card(
        id: Int,
        gender: Gender = Gender.F,
        attended: Boolean = false,
        flags: List<AuditFlag> = emptyList(),
    ) = ApplicantCard(
        id = ApplicantId(id),
        centreId = CentreId(1),
        courseId = CourseId(10),
        givenName = "Meera",
        familyName = "Deshpande",
        gender = gender,
        status = ApplicantStatus("Confirmed"),
        type = ApplicantType.Student,
        oldStudent = false,
        attended = attended,
        confNo = ConfNo("NF$id"),
        mobile = "9876543210",
        city = "Pune",
        age = 34,
        flags = flags,
    )

    private val flagged = card(
        3,
        flags = listOf(
            AuditFlag(AuditSeverity.HARD, "x", "phone_prefix_invalid · +91", "phone_prefix_invalid"),
        ),
    )

    private val roll = listOf(card(1, attended = true), card(2, gender = Gender.M), flagged)

    private fun board(
        checkIns: Map<ApplicantId, CheckInRecord> = emptyMap(),
        onGoto: (DeskSection) -> Unit = {},
        onExport: (String) -> Unit = {},
    ) {
        rule.setContent {
            DipiTheme {
                BoardPane(
                    roll = roll,
                    checkIns = checkIns,
                    flagged = listOf(flagged),
                    callOutcomes = mapOf(ApplicantId(1) to "Reached"),
                    onGoto = onGoto,
                    onExport = onExport,
                )
            }
        }
    }

    private val grid = listOf(
        listOf("Day 0 list", "Day 0 summary", "Course summary"),
        listOf("Student chit", "Checking slip", "Seating plan"),
        listOf("Teacher list", "Manager list", "Laundry list"),
    )

    @Test
    fun nineEqualCellsSitOnAThreeByThreeWithNoShelfHeaders() {
        board()
        rule.onNodeWithText("SHEETS & EXPORTS").assertIsDisplayed()
        rule.onNodeWithTag("export-grid").assertIsDisplayed()
        rule.onAllNodesWithTag("export-chip").assertCountEquals(9)

        listOf(
            "ROLL SHEETS", "DESK SLIPS", "FOR THE TEAM",
            "RARELY URGENT", "END OF COURSE",
            "day 0", "printed and cut", "teachers and managers",
        ).forEach { rule.onNodeWithText(it).assertDoesNotExist() }

        rule.onNodeWithText("Valuable list").assertDoesNotExist()
        rule.onNodeWithText("Male PDF").assertDoesNotExist()
        rule.onNodeWithText("Female PDF").assertDoesNotExist()
        rule.onNodeWithText("Course report").assertDoesNotExist()
        rule.onNodeWithText("Day 11 · Course summary report").assertDoesNotExist()
        rule.onAllNodesWithTag("export-shelf-gap").assertCountEquals(0)

        grid.flatten().forEach { rule.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun exportCellsAreSixtyFourDpEqualAndFireOnExport() {
        var exported: String? = null
        board(onExport = { exported = it })

        val chips = rule.onAllNodesWithTag("export-chip")
        chips.assertCountEquals(9)
        val first = chips[0].getBoundsInRoot()
        assertEquals(64.dp.value, first.height.value, 0.5f)
        repeat(9) { i ->
            val box = chips[i].getBoundsInRoot()
            assertEquals("cell $i height", first.height.value, box.height.value, 0.5f)
            assertEquals("cell $i width", first.width.value, box.width.value, 1.5f)
        }

        rule.onNodeWithText("Seating plan").performClick()
        assertEquals("Seating plan", exported)
        rule.onNodeWithText("Day 0 summary").performClick()
        assertEquals("Day 0 summary", exported)
    }

    @Test
    fun courseSummaryIsAGridCellNotAFourthLine() {
        var exported: String? = null
        board(onExport = { exported = it })

        rule.onNodeWithText("Course summary").assertIsDisplayed()
        rule.onNodeWithTag("export-day11").assertIsDisplayed()

        val chips = rule.onAllNodesWithTag("export-chip")
        val day0 = chips[0].getBoundsInRoot()
        val summary = chips[1].getBoundsInRoot()
        val day11 = rule.onNodeWithTag("export-day11").getBoundsInRoot()
        assertEquals("Course summary shares the first row", day0.top.value, day11.top.value, 0.5f)
        assertEquals(day0.height.value, day11.height.value, 0.5f)
        assertEquals(day0.width.value, day11.width.value, 1.5f)
        assertTrue("Course summary sits to the right of Day 0 summary", day11.left.value >= summary.right.value - 0.5f)
        assertTrue(
            "Course summary is a cell, not a full-width fourth line",
            day11.width.value < (day0.width.value * 2f),
        )

        rule.onNodeWithText("Course summary").performClick()
        assertEquals("Course summary", exported)
    }

    @Test
    fun gridReadsLeftToRightThenDown() {
        board()
        grid.forEach { row ->
            val left = rule.onNodeWithText(row[0]).getBoundsInRoot()
            val mid = rule.onNodeWithText(row[1]).getBoundsInRoot()
            val right = rule.onNodeWithText(row[2]).getBoundsInRoot()
            assertEquals("${row[0]} and ${row[1]} share a row", left.top.value, mid.top.value, 0.5f)
            assertEquals("${row[1]} and ${row[2]} share a row", mid.top.value, right.top.value, 0.5f)
            assertTrue("${row[0]} precedes ${row[1]}", left.right.value <= mid.left.value + 0.5f)
            assertTrue("${row[1]} precedes ${row[2]}", mid.right.value <= right.left.value + 0.5f)
        }
        val top = rule.onNodeWithText("Day 0 list").getBoundsInRoot()
        val mid = rule.onNodeWithText("Student chit").getBoundsInRoot()
        val bot = rule.onNodeWithText("Teacher list").getBoundsInRoot()
        assertTrue("second row sits below the first", mid.top.value >= top.bottom.value - 0.5f)
        assertTrue("third row sits below the second", bot.top.value >= mid.bottom.value - 0.5f)
        assertEquals("first column aligns", top.left.value, mid.left.value, 0.5f)
        assertEquals("first column aligns", mid.left.value, bot.left.value, 0.5f)
    }

    @Test
    fun fourStatCardsAreOneHundredDpAndCarryTheNavigation() {
        var went: DeskSection? = null
        board(
            checkIns = mapOf(ApplicantId(1) to CheckInRecord(checkedIn = true, room = "F21")),
            onGoto = { went = it },
        )

        val stats = rule.onAllNodesWithTag("board-stat")
        stats.assertCountEquals(4)
        repeat(4) { i ->
            assertEquals(112.dp.value, stats[i].getBoundsInRoot().height.value, 0.5f)
        }
        listOf("ARRIVING TODAY", "CHECKED IN", "STILL TO CALL", "NEEDS ATTENTION").forEach {
            rule.onNodeWithText(it).assertIsDisplayed()
        }

        rule.onNodeWithText("ARRIVING TODAY").performClick()
        assertEquals(DeskSection.CheckIn, went)
        rule.onNodeWithText("STILL TO CALL").performClick()
        assertEquals(DeskSection.Calling, went)
        rule.onNodeWithText("NEEDS ATTENTION").performClick()
        assertEquals(DeskSection.Audit, went)
    }

    @Test
    fun threeNextRowsAreFiftyEightDpAndRoute() {
        var went: DeskSection? = null
        board(onGoto = { went = it })

        val rows = rule.onAllNodesWithTag("board-next")
        rows.assertCountEquals(3)
        repeat(3) { i ->
            assertEquals(58.dp.value, rows[i].getBoundsInRoot().height.value, 0.5f)
        }

        rule.onNodeWithText("Check in arrivals").performClick()
        assertEquals(DeskSection.CheckIn, went)
        rule.onNodeWithText("Clear audit findings").performClick()
        assertEquals(DeskSection.Audit, went)
        rule.onNodeWithText("Finish the call round").performClick()
        assertEquals(DeskSection.Calling, went)
    }

    @Test
    fun statCardsCarryOneArrowEach() {
        board()
        val arrows = rule.onAllNodesWithTag("board-stat-arrow", useUnmergedTree = true)
        arrows.assertCountEquals(4)
        repeat(4) { i ->
            val box = arrows[i].getBoundsInRoot()
            assertTrue("arrow $i clipped to ${box.height.value}dp", box.height.value >= 10f)
        }
    }
}
