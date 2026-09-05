package org.dhamma.dipi.staff

import org.dhamma.dipi.staff.desk.seatingPlanPrintHtml
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.HallGrid
import org.dhamma.dipi.staff.model.RollGroup
import org.dhamma.dipi.staff.model.RollRow
import org.dhamma.dipi.staff.model.RollSeniority
import org.dhamma.dipi.staff.model.SeatKind
import org.dhamma.dipi.staff.model.TeacherRoll
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The native 5h hall prints (frame 5i, "READ & PRINT") from the in-memory
 * roll — no `GET /seating`, one gender per page — reusing the same pure
 * [org.dhamma.dipi.staff.model.hallLayout] the screen draws, so paper and
 * screen never diverge.
 */
class SeatingPrintTest {

    private fun row(sn: Int, name: String, seat: String, roleTag: String? = null) = RollRow(
        sn = sn, name = name, roleTag = roleTag, room = "Mbk-$sn", age = "40", city = "Pune",
        courses = emptyList(), cell = "", seat = seat,
        seatKind = when {
            seat.startsWith("CW-", ignoreCase = true) -> SeatKind.CELL
            seat.startsWith("CH-", ignoreCase = true) -> SeatKind.CHAIR
            else -> SeatKind.FLOOR
        },
        backrest = false, occupation = "", education = "", languages = "",
    )

    private fun group(gender: Gender, seniority: RollSeniority, rows: List<RollRow>) = RollGroup(
        at = "Uma Rangan", code = "URN", gender = gender,
        seniority = seniority, group = "1", total = rows.size, rows = rows,
    )

    private val roll = TeacherRoll(
        groups = listOf(
            group(
                Gender.M, RollSeniority.OLD,
                listOf(
                    row(1, "Alice Kumar", "A1"),
                    row(2, "Bob Singh", "B2"),
                    row(3, "Chandra Rao", "CW-A1"),
                    row(4, "Deepak Nair", ""),
                    row(5, "Eshan Sevak", "", roleTag = "Sevak"),
                ),
            ),
            group(
                Gender.F, RollSeniority.NEW,
                listOf(row(6, "Fatima Sheikh", "A1")),
            ),
        ),
    )

    private val html = seatingPlanPrintHtml(roll) { HallGrid() }

    @Test
    fun seatedStudentsPrintWithTheirSeatLabels() {
        assertTrue(html.contains("Alice Kumar"))
        assertTrue(html.contains("Bob Singh"))
        assertTrue(html.contains("A1"))
        assertTrue(html.contains("B2"))
    }

    @Test
    fun theChowkyRailSeatsPrint() {
        assertTrue(html.contains("Chandra Rao"))
        assertTrue(html.contains("CW-A1"))
    }

    @Test
    fun anUnseatedStudentIsListedButAnUnseatedSevakIsNot() {
        assertTrue(html.contains("Deepak Nair"))
        assertFalse(html.contains("Eshan Sevak"))
    }

    @Test
    fun theTeacherDhammaSeatMarkerPrints() {
        assertTrue(html.uppercase().contains("TEACHER"))
    }

    @Test
    fun eachGenderIsItsOwnPage() {
        assertTrue(html.contains("Fatima Sheikh"))
        // Both halls present, one page break between the two genders.
        assertTrue(html.contains("page-break"))
    }

    @Test
    fun aGenderWithNoRowsDrawsNoPage() {
        val maleOnly = TeacherRoll(
            groups = listOf(group(Gender.M, RollSeniority.OLD, listOf(row(1, "Alice Kumar", "A1")))),
        )
        val out = seatingPlanPrintHtml(maleOnly) { HallGrid() }
        // One gender, one page: no female hall and no page break.
        assertFalse(out.contains("Female"))
        assertFalse(out.contains("page-break"))
    }
}
