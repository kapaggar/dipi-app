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
 * labelled shelves of four 40dp chips. Nothing is drawn for the Day 11
 * export: it lives on unmerged `feat/desk-gap` (spec R2), and the design
 * file's dashed marker is canvas annotation, not UI.
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
        "FOR THE TEAM" to listOf("Teacher list", "Manager list", "Valuable list", "Course report"),
    )

    @Test
    fun twelveExportsSitOnThreeShelvesInTheDesignsGrouping() {
        board()
        rule.onNodeWithText("SHEETS & EXPORTS · RARELY URGENT").assertIsDisplayed()
        rule.onAllNodesWithTag("export-chip").assertCountEquals(12)

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
    fun exportChipsAreFortyDpAndFireOnExportWithTheUnchangedLabel() {
        var exported: String? = null
        board(onExport = { exported = it })

        rule.onAllNodesWithTag("export-chip")[0].getBoundsInRoot().let {
            assertEquals(40.dp.value, it.height.value, 0.5f)
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
    fun noDayElevenGapMarkerIsDrawn() {
        board()
        rule.onNodeWithText("Day 11 · Course summary report").assertDoesNotExist()
        rule.onNodeWithText("GAP — NOT IN 1.22.0").assertDoesNotExist()
        // Applications and Rooms stay out of NEXT — their counts are inventory.
        rule.onNodeWithText("Review applications").assertDoesNotExist()
    }
}
