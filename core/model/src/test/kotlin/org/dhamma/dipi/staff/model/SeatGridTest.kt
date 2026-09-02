package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec 2c S2 — the pure seat-placement pass: alphanumeric and numeric-flow
 * placement, AA rows, non-contiguous ids, CW-/CH- exclusion to the
 * cell/pagoda column, blank → unseated with the roleTag reason, grid
 * extension past the config, and the old/new tally the sub-line shows.
 */
class SeatGridTest {

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

    private fun group(
        rows: List<RollRow>,
        seniority: RollSeniority = RollSeniority.OLD,
        groupNo: String = "1",
    ) = RollGroup(
        at = "Uma Rangan", code = "URN", gender = Gender.M,
        seniority = seniority, group = groupNo, total = rows.size, rows = rows,
    )

    // --- seatPlacement ---

    @Test
    fun alphanumericLabelsPlaceLetterRowNumberColumn() {
        assertEquals(SeatCell("A1", 0, 0), seatPlacement("A1"))
        assertEquals(SeatCell("B7", 1, 6), seatPlacement("B7"))
        assertEquals(SeatCell("E4", 4, 3), seatPlacement("E4"))
    }

    @Test
    fun aaRowPlacesAtIndexTwentySix() {
        assertEquals(SeatCell("AA3", 26, 2), seatPlacement("AA3"))
        assertEquals(SeatCell("Z1", 25, 0), seatPlacement("Z1"))
    }

    @Test
    fun numericLabelsFlowBySeatsPerRow() {
        assertEquals(SeatCell("1", 0, 0), seatPlacement("1", 7))
        assertEquals(SeatCell("7", 0, 6), seatPlacement("7", 7))
        assertEquals(SeatCell("8", 1, 0), seatPlacement("8", 7))
        assertEquals(SeatCell("12", 1, 4), seatPlacement("12", 7))
        // A different width reflows the same label: row C holds 11-15.
        assertEquals(SeatCell("12", 2, 1), seatPlacement("12", 5))
    }

    @Test
    fun blankPrefixedAndGarbageLabelsDoNotPlace() {
        assertNull(seatPlacement(""))
        assertNull(seatPlacement("   "))
        assertNull(seatPlacement("CW-A3"))
        assertNull(seatPlacement("ch-12"))
        assertNull(seatPlacement("A0"))
        assertNull(seatPlacement("0", 7))
        assertNull(seatPlacement("SOFA"))
    }

    // --- hallLayout ---

    @Test
    fun a8LandsInColumnEightWithA7Empty() {
        // Seat ids are data — row A can be A1…A6, A8 (ground truth). The 7-wide
        // config EXTENDS to 8 columns; A7 stays an empty dashed cell.
        val plan = hallLayout(
            listOf(group(listOf(row(1, "Suresh Nair", "A1"), row(2, "Vikram Joshi", "A8")))),
            HallGrid(rows = 5, seatsPerRow = 7),
        )
        assertEquals(8, plan.cols)
        assertEquals(5, plan.rows)
        assertEquals("Suresh Nair", plan.cells[0][0].seated?.row?.name)
        assertNull(plan.cells[0][6].seated)
        assertEquals("A7", plan.cells[0][6].id)
        assertEquals("Vikram Joshi", plan.cells[0][7].seated?.row?.name)
        assertEquals("A8", plan.cells[0][7].id)
    }

    @Test
    fun numericConventionFlowsAndSynthesizesFlowingEmptyIds() {
        val plan = hallLayout(
            listOf(group(listOf(row(1, "Suresh Nair", "1"), row(2, "Vikram Joshi", "9")))),
            HallGrid(rows = 2, seatsPerRow = 7),
        )
        assertEquals(7, plan.cols)
        assertEquals(2, plan.rows)
        assertEquals("Suresh Nair", plan.cells[0][0].seated?.row?.name)
        assertEquals("Vikram Joshi", plan.cells[1][1].seated?.row?.name)
        // Empty ids keep the hall's own numeric convention, flowing by width.
        assertEquals("2", plan.cells[0][1].id)
        assertEquals("8", plan.cells[1][0].id)
        // Row letters still synthesize for the 26dp gutter.
        assertEquals(listOf("A", "B"), plan.rowLetters)
    }

    @Test
    fun aaRowExtendsThePlanToTwentySevenRows() {
        val plan = hallLayout(
            listOf(group(listOf(row(1, "Suresh Nair", "AA3")))),
            HallGrid(rows = 5, seatsPerRow = 7),
        )
        assertEquals(27, plan.rows)
        assertEquals("AA", plan.rowLetters[26])
        assertEquals("Suresh Nair", plan.cells[26][2].seated?.row?.name)
    }

    @Test
    fun rowLabelsBeyondConfigExtendAndNeverDropASeatedStudent() {
        val plan = hallLayout(
            listOf(group(listOf(row(1, "Suresh Nair", "F2")))),
            HallGrid(rows = 5, seatsPerRow = 7),
        )
        assertEquals(6, plan.rows)
        assertEquals("Suresh Nair", plan.cells[5][1].seated?.row?.name)
        assertTrue(plan.unseated.isEmpty())
    }

    @Test
    fun cwAndChSeatsGoToTheCellColumnInLabelOrder() {
        val plan = hallLayout(
            listOf(
                group(
                    listOf(
                        row(1, "Zara Bhosale", "CW-B1"),
                        row(2, "Rakesh Iyer", "CH-12"),
                        row(3, "Meera Deshpande", "CW-A3"),
                        row(4, "Nikhil Rane", "CH-2"),
                        row(5, "Suresh Nair", "A1"),
                    ),
                ),
            ),
            HallGrid(),
        )
        // Not force-fitted into the hall rows.
        assertEquals(1, plan.cells.flatten().count { it.seated != null })
        // Label order, digit runs numeric: CH-2 before CH-12, CW-A3 before CW-B1.
        assertEquals(
            listOf("CH-2", "CH-12", "CW-A3", "CW-B1"),
            plan.cellColumn.map { it.row.seat },
        )
    }

    @Test
    fun blankSeatGoesToUnseatedWithRoleTagReasonOrEmDash() {
        val plan = hallLayout(
            listOf(
                group(
                    listOf(
                        row(1, "Suresh Nair", "A1"),
                        row(2, "Karan Velu", "", roleTag = "Sevak"),
                        row(3, "Anup Datta", ""),
                        row(4, "Tara Singh", "", roleTag = "SAT-2011"),
                    ),
                ),
            ),
            HallGrid(),
        )
        // Roll order kept, never re-sorted.
        assertEquals(listOf("Karan Velu", "Anup Datta", "Tara Singh"), plan.unseated.map { it.row.name })
        assertEquals(listOf("Sevak", UNSEATED_NO_REASON, "SAT-2011"), plan.unseated.map { it.reason })
    }

    @Test
    fun oldNewTallyComesFromGroupSeniorityAndMatchesTheSubLine() {
        val plan = hallLayout(
            listOf(
                group(
                    listOf(row(1, "Suresh Nair", "A1"), row(2, "Zara Bhosale", "CW-B1")),
                    seniority = RollSeniority.OLD,
                ),
                group(
                    listOf(row(1, "Rakesh Iyer", "B2"), row(2, "Karan Velu", "", roleTag = "Sevak"), row(3, "Arjun Patel", "B3")),
                    seniority = RollSeniority.NEW,
                    groupNo = "2",
                ),
            ),
            HallGrid(),
        )
        // Every row counts — seated, cell column and unseated alike.
        assertEquals(2, plan.oldCount)
        assertEquals(3, plan.newCount)
        // Old/new rides each placed seat from its band.
        assertEquals(true, plan.cells[0][0].seated?.old)
        assertEquals(false, plan.cells[1][1].seated?.old)
        assertEquals(true, plan.cellColumn.single().old)
    }

    @Test
    fun duplicateLabelKeepsTheFirstAndTheSecondFallsToUnseated() {
        val plan = hallLayout(
            listOf(group(listOf(row(1, "Suresh Nair", "A1"), row(2, "Vikram Joshi", "A1")))),
            HallGrid(),
        )
        assertEquals("Suresh Nair", plan.cells[0][0].seated?.row?.name)
        assertEquals(listOf("Vikram Joshi"), plan.unseated.map { it.row.name })
    }

    @Test
    fun hallGridClampsOnReadAndWrite() {
        assertEquals(HallGrid(1, 1), HallGrid(0, -3).clamped())
        assertEquals(HallGrid(26, 20), HallGrid(400, 99).clamped())
        val prefs = CentreOpsPrefs().withHallGrid(Gender.M, HallGrid(rows = 99, seatsPerRow = 0))
        assertEquals(HallGrid(26, 1), prefs.hallGridFor(Gender.M))
        // Read-side clamp too, in case a stale blob carries out-of-range values.
        assertEquals(
            HallGrid(1, 20),
            CentreOpsPrefs(hallGrid = mapOf("F" to HallGrid(0, 50))).hallGridFor(Gender.F),
        )
        // Unset gender falls back to the 5×7 default.
        assertEquals(HallGrid(5, 7), CentreOpsPrefs().hallGridFor(Gender.F))
    }

    @Test
    fun garbageLabelsFallToUnseatedRatherThanStretchingThePlan() {
        val plan = hallLayout(
            listOf(group(listOf(row(1, "Suresh Nair", "9999"), row(2, "Vikram Joshi", "A1")))),
            HallGrid(rows = 5, seatsPerRow = 7),
        )
        assertEquals(5, plan.rows)
        assertEquals(listOf("Suresh Nair"), plan.unseated.map { it.row.name })
        assertEquals("Vikram Joshi", plan.cells[0][0].seated?.row?.name)
    }
}
