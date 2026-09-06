package org.dhamma.dipi.staff

import org.dhamma.dipi.staff.desk.seatingPlanPrintHtml
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.HallGrid
import org.dhamma.dipi.staff.model.RollGroup
import org.dhamma.dipi.staff.model.RollRow
import org.dhamma.dipi.staff.model.RollSeniority
import org.dhamma.dipi.staff.model.SeatKind
import org.dhamma.dipi.staff.model.TeacherRoll
import org.dhamma.dipi.staff.model.BACKREST_GLYPH
import org.dhamma.dipi.staff.model.backrestSeatLabel
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

    private fun row(
        sn: Int,
        name: String,
        seat: String,
        roleTag: String? = null,
        backrest: Boolean = false,
        room: String = "Mbk-$sn",
        age: String = "40",
    ) = RollRow(
        sn = sn, name = name, roleTag = roleTag, room = room, age = age, city = "Pune",
        courses = emptyList(), cell = "", seat = seat,
        seatKind = when {
            seat.startsWith("CW-", ignoreCase = true) -> SeatKind.CELL
            seat.startsWith("CH-", ignoreCase = true) -> SeatKind.CHAIR
            else -> SeatKind.FLOOR
        },
        backrest = backrest, occupation = "", education = "", languages = "",
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
    fun printUsesTheFullLandscapePageBox() {
        assertTrue(html.contains("size:A4 landscape"))
        assertTrue(html.contains("height:198mm"))
        assertTrue(html.contains("class=\"grid-wrap\""))
        assertTrue(html.contains("grid-template-columns:repeat(4,minmax(0,1fr))"))
    }

    @Test
    fun floorSeatShowsOperationalDetails() {
        assertTrue(html.contains("SEAT A1"))
        assertTrue(html.contains("ROOM Mbk-1"))
        assertTrue(html.contains("AGE 40"))
    }

    @Test
    fun railSeatShowsOperationalDetails() {
        assertTrue(html.contains("SEAT CW-A1"))
        assertTrue(html.contains("ROOM Mbk-3"))
        assertTrue(html.contains("AGE 40"))
    }

    @Test
    fun visibleUnseatedStudentShowsOperationalDetails() {
        assertTrue(html.contains("Deepak Nair"))
        assertTrue(html.contains("ROOM Mbk-4"))
        assertTrue(html.contains("UNSEATED"))
    }

    @Test
    fun blankOperationalDetailsUseADash() {
        val blank = TeacherRoll(
            groups = listOf(
                group(
                    Gender.M,
                    RollSeniority.OLD,
                    listOf(row(1, "Blank Details", "A1", room = "", age = "")),
                ),
            ),
        )

        val out = seatingPlanPrintHtml(blank) { HallGrid() }
        assertTrue(out.contains("ROOM —"))
        assertTrue(out.contains("AGE —"))
    }

    @Test
    fun operationalFieldsRemainHtmlEscaped() {
        val marked = TeacherRoll(
            groups = listOf(
                group(
                    Gender.M,
                    RollSeniority.OLD,
                    listOf(row(1, "A&B <C>", "A1", room = "M&<1>", age = "4<0")),
                ),
            ),
        )

        val out = seatingPlanPrintHtml(marked) { HallGrid() }
        assertTrue(out.contains("A&amp;B &lt;C&gt;"))
        assertTrue(out.contains("ROOM M&amp;&lt;1&gt;"))
        assertTrue(out.contains("AGE 4&lt;0"))
        assertFalse(out.contains("A&B <C>"))
        assertFalse(out.contains("M&<1>"))
    }

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

    @Test
    fun backrestSeatsPrintTheGlyphAndTheHeaderLegend() {
        val marked = TeacherRoll(
            groups = listOf(
                group(
                    Gender.M, RollSeniority.OLD,
                    listOf(
                        row(1, "Alice Kumar", "A1", backrest = true),
                        row(2, "Chandra Rao", "CW-A1", backrest = true),
                        row(3, "Bob Singh", "B2"),
                    ),
                ),
            ),
        )
        val out = seatingPlanPrintHtml(marked) { HallGrid() }
        // The flagged seated cell's seat id carries the glyph prefix …
        assertTrue(out.contains(backrestSeatLabel("A1", true)))
        // … the flagged CW rail card carries it …
        assertTrue(out.contains("SEAT ${backrestSeatLabel("CW-A1", true)}"))
        // … and the header earns the legend line.
        assertTrue(out.contains("$BACKREST_GLYPH = backrest"))
        // The unflagged seat stays plain.
        assertFalse(out.contains(backrestSeatLabel("B2", true)))
    }

    @Test
    fun aRollWithoutBackrestsPrintsNoGlyphAndNoLegend() {
        // The class fixture carries zero backrests: no glyph, and the legend
        // only earns its line when used.
        assertFalse(html.contains(BACKREST_GLYPH))
        assertFalse(html.contains("= backrest"))
    }
}
