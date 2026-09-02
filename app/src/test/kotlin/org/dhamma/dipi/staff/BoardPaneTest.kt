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
 * v4 frame 1f: the Board lands on one fold. Stat cards are 100dp, NEXT rows
 * 58dp, and the twelve exports — same names, same callback — sit on three
 * labelled shelves of four 40dp chips. Day 11 · Course summary report ships
 * on the design's own fourth-line row (full-width, 40dp), not inside the
 * 3×4 shelf grid. The dashed GAP badge is not drawn.
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

    private val shelves = listOf(
        "ROLL SHEETS" to listOf("Day 0 list", "Day 0 summary", "Male PDF", "Female PDF"),
        "DESK SLIPS" to listOf("Student chit", "Checking slip", "Seating plan", "Laundry list"),
        // v5 T4: Course report moved to the centre dashboard.
        "FOR THE TEAM" to listOf("Teacher list", "Manager list", "Valuable list"),
    )

    @Test
    fun elevenExportsSitOnThreeShelvesInTheDesignsGrouping() {
        board()
        rule.onNodeWithText("SHEETS & EXPORTS").assertIsDisplayed()
        rule.onNodeWithText("RARELY URGENT").assertIsDisplayed()
        rule.onNodeWithText("SHEETS & EXPORTS · RARELY URGENT").assertDoesNotExist()
        rule.onAllNodesWithTag("export-chip").assertCountEquals(11)

        shelves.forEach { (kicker, names) ->
            rule.onNodeWithText(kicker).assertIsDisplayed()
            val shelf = rule.onNodeWithTag("export-shelf-$kicker").getBoundsInRoot()
            names.forEach { name ->
                val chip = rule.onNodeWithText(name).getBoundsInRoot()
                assertTrue(
                    "$name should sit on the $kicker shelf",
                    chip.top >= shelf.top && chip.bottom <= shelf.bottom,
                )
            }
        }
    }

    @Test
    fun exportChipsAreThirtyEightDpAndFireOnExportWithTheUnchangedLabel() {
        var exported: String? = null
        board(onExport = { exported = it })

        rule.onAllNodesWithTag("export-chip")[0].getBoundsInRoot().let {
            assertEquals(38.dp.value, it.height.value, 0.5f)
        }
        rule.onNodeWithText("Seating plan").performClick()
        assertEquals("Seating plan", exported)
        rule.onNodeWithText("Day 0 summary").performClick()
        assertEquals("Day 0 summary", exported)
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
            assertEquals(100.dp.value, stats[i].getBoundsInRoot().height.value, 0.5f)
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
    fun dayElevenChipSitsOnTheFourthLineAndFiresTheExportLabel() {
        var exported: String? = null
        board(onExport = { exported = it })

        rule.onNodeWithText("Day 11 · Course summary report").assertIsDisplayed()
        rule.onNodeWithText("GAP — NOT IN 1.22.0").assertDoesNotExist()
        rule.onNodeWithText("Review applications").assertDoesNotExist()

        val chip = rule.onNodeWithTag("export-day11").getBoundsInRoot()
        val team = rule.onNodeWithTag("export-shelf-FOR THE TEAM").getBoundsInRoot()
        assertTrue(
            "Day 11 must sit below the FOR THE TEAM shelf",
            chip.top.value >= team.bottom.value - 0.5f,
        )
        assertEquals(40.dp.value, chip.height.value, 0.5f)
        assertEquals(
            "Day 11 is the design's full-width fourth line",
            team.width.value,
            chip.width.value,
            0.5f,
        )

        rule.onNodeWithText("Day 11 · Course summary report").performClick()
        assertEquals("Course summary report", exported)
    }

    @Test
    fun elevenShelfChipsStayOnTheirShelves() {
        board()
        shelves.forEach { (kicker, names) ->
            rule.onNodeWithText(kicker).assertIsDisplayed()
            val shelf = rule.onNodeWithTag("export-shelf-$kicker").getBoundsInRoot()
            names.forEach { name ->
                val chip = rule.onNodeWithText(name).getBoundsInRoot()
                assertTrue(
                    "$name should sit on the $kicker shelf",
                    chip.top >= shelf.top && chip.bottom <= shelf.bottom,
                )
            }
        }
        // Day 11 is NOT inside any shelf tag.
        val day11 = rule.onNodeWithText("Day 11 · Course summary report").getBoundsInRoot()
        shelves.forEach { (kicker, _) ->
            val shelf = rule.onNodeWithTag("export-shelf-$kicker").getBoundsInRoot()
            assertTrue(
                "Day 11 must not sit inside $kicker",
                day11.bottom.value <= shelf.top.value + 0.5f ||
                    day11.top.value >= shelf.bottom.value - 0.5f,
            )
        }
    }

    /**
     * v5 T4: the Board no longer offers Course report — it lives on the
     * centre dashboard now. Shelf 3 keeps four columns with the last one
     * empty rather than stretching three chips across the full width.
     */
    @Test
    fun boardHasElevenExportChipsAndNoCourseReport() {
        board()
        rule.onAllNodesWithTag("export-chip").assertCountEquals(11)
        rule.onNodeWithText("Course report").assertDoesNotExist()
        rule.onAllNodesWithTag("export-shelf-gap").assertCountEquals(1)

        val team = rule.onNodeWithTag("export-shelf-FOR THE TEAM").getBoundsInRoot()
        val roll = rule.onNodeWithTag("export-shelf-ROLL SHEETS").getBoundsInRoot()
        val teacher = rule.onNodeWithText("Teacher list").getBoundsInRoot()
        val day0 = rule.onNodeWithText("Day 0 list").getBoundsInRoot()
        assertEquals(
            "A three-chip shelf keeps four-column width",
            roll.width.value,
            team.width.value,
            0.5f,
        )
        assertEquals(
            "Chips on a short shelf must not stretch",
            day0.width.value,
            teacher.width.value,
            0.5f,
        )
    }

    /** Each shelf kicker gains a grey qualifier so the grouping explains itself. */
    @Test
    fun shelfKickersCarryTheirQualifier() {
        board()
        listOf("day 0", "printed and cut", "teachers and managers").forEach {
            rule.onNodeWithText(it).assertIsDisplayed()
        }
    }

    /** Stat cards read as navigation via one quiet arrow, not a button. */
    @Test
    fun statCardsCarryOneArrowEach() {
        board()
        // The stat card merges its semantics, so the arrow is only visible
        // in the unmerged tree.
        rule.onAllNodesWithTag("board-stat-arrow", useUnmergedTree = true).assertCountEquals(4)
    }

    /** The Day-11 row keeps its own fourth line and gains only a reason tag. */
    @Test
    fun day11KeepsItsFourthLineAndGainsAnEndOfCourseTag() {
        board()
        rule.onNodeWithTag("export-day11").assertIsDisplayed()
        rule.onNodeWithText("END OF COURSE").assertIsDisplayed()
        val chip = rule.onNodeWithTag("export-day11").getBoundsInRoot()
        val team = rule.onNodeWithTag("export-shelf-FOR THE TEAM").getBoundsInRoot()
        assertEquals(team.width.value, chip.width.value, 0.5f)
        assertEquals(40.dp.value, chip.height.value, 0.5f)
    }
}
