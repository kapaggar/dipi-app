package org.dhamma.dipi.staff.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pure seat-placement pass on the r2 orientation (letters are COLUMNS,
 * numbers are DEPTH, seat 1 nearest the teacher — the live web page is the
 * authority): alphanumeric and numeric-flow placement, AA columns,
 * CW-/CH- exclusion to the chowky/chair rail in CW-then-CH suffix-ascending
 * order, blank → unseated with the roleTag reason, the sevak filter that
 * never touches the tally, grid extension past the config on both axes with
 * the 2× cap, and the old/new tally the sub-line shows.
 */
class SeatGridTest {

    private fun row(
        sn: Int,
        name: String,
        seat: String,
        roleTag: String? = null,
        kind: SeatKind = when {
            seat.startsWith("CW-", ignoreCase = true) -> SeatKind.CELL
            seat.startsWith("CH-", ignoreCase = true) -> SeatKind.CHAIR
            else -> SeatKind.FLOOR
        },
    ) = RollRow(
        sn = sn, name = name, roleTag = roleTag, room = "Mbk-$sn", age = "40", city = "Pune",
        courses = emptyList(), cell = "",
        seat = seat,
        seatKind = kind,
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
    fun alphanumericLabelsPlaceLetterColumnNumberDepth() {
        assertEquals(SeatCell("A1", 0, 1), seatPlacement("A1"))
        assertEquals(SeatCell("B7", 1, 7), seatPlacement("B7"))
        // "E4" → column E (index 4), depth 4 — the r2 flip.
        assertEquals(SeatCell("E4", 4, 4), seatPlacement("E4"))
    }

    @Test
    fun aaColumnPlacesAtIndexTwentySix() {
        assertEquals(SeatCell("AA3", 26, 3), seatPlacement("AA3"))
        assertEquals(SeatCell("Z1", 25, 1), seatPlacement("Z1"))
    }

    @Test
    fun numericLabelsFlowAcrossColumns() {
        assertEquals(SeatCell("1", 0, 1), seatPlacement("1", 7))
        assertEquals(SeatCell("7", 6, 1), seatPlacement("7", 7))
        assertEquals(SeatCell("8", 0, 2), seatPlacement("8", 7))
        assertEquals(SeatCell("12", 4, 2), seatPlacement("12", 7))
        // A different width reflows the same label: depth 3 holds 11-15.
        assertEquals(SeatCell("12", 1, 3), seatPlacement("12", 5))
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
    fun a8ExtendsDepthWithA7Empty() {
        // A8 = column A, depth 8: the 5-deep config EXTENDS to 8 rows; A7
        // stays an empty dashed cell. cells[0] is depth 1, nearest the teacher.
        val plan = hallLayout(
            listOf(group(listOf(row(1, "Suresh Nair", "A1"), row(2, "Vikram Joshi", "A8")))),
            HallGrid(columns = 7, depth = 5),
        )
        assertEquals(7, plan.columns)
        assertEquals(8, plan.depth)
        assertEquals("Suresh Nair", plan.cells[0][0].seated?.row?.name)
        assertNull(plan.cells[6][0].seated)
        assertEquals("A7", plan.cells[6][0].id)
        assertEquals("Vikram Joshi", plan.cells[7][0].seated?.row?.name)
        assertEquals("A8", plan.cells[7][0].id)
    }

    @Test
    fun numericConventionFlowsAndSynthesizesFlowingEmptyIds() {
        val plan = hallLayout(
            listOf(group(listOf(row(1, "Suresh Nair", "1"), row(2, "Vikram Joshi", "9")))),
            HallGrid(columns = 7, depth = 2),
        )
        assertEquals(7, plan.columns)
        assertEquals(2, plan.depth)
        assertEquals("Suresh Nair", plan.cells[0][0].seated?.row?.name)
        assertEquals("Vikram Joshi", plan.cells[1][1].seated?.row?.name)
        // Empty ids keep the hall's own numeric convention, flowing by width.
        assertEquals("2", plan.cells[0][1].id)
        assertEquals("8", plan.cells[1][0].id)
        // Column letters still synthesize for the bottom axis row.
        assertEquals(listOf("A", "B", "C", "D", "E", "F", "G"), plan.columnLetters)
    }

    @Test
    fun aaColumnExtendsThePlanToTwentySevenColumns() {
        val plan = hallLayout(
            listOf(group(listOf(row(1, "Suresh Nair", "AA3")))),
            HallGrid(columns = 7, depth = 5),
        )
        assertEquals(27, plan.columns)
        assertEquals("AA", plan.columnLetters[26])
        assertEquals("Suresh Nair", plan.cells[2][26].seated?.row?.name)
    }

    @Test
    fun columnLabelsBeyondConfigExtendAndNeverDropASeatedStudent() {
        // H2 = column index 7 on a 7-column config: extends to 8 columns.
        val plan = hallLayout(
            listOf(group(listOf(row(1, "Suresh Nair", "H2")))),
            HallGrid(columns = 7, depth = 5),
        )
        assertEquals(8, plan.columns)
        assertEquals("Suresh Nair", plan.cells[1][7].seated?.row?.name)
        assertTrue(plan.unseated.isEmpty())
    }

    @Test
    fun cwAndChSeatsGoToTheRailCwFirstThenSuffixAscending() {
        val plan = hallLayout(
            listOf(
                group(
                    listOf(
                        row(1, "Zara Bhosale", "CW-B1"),
                        row(2, "Rakesh Iyer", "CH-12"),
                        row(3, "Meera Deshpande", "CW-A3"),
                        row(4, "Nikhil Rane", "CH-2"),
                        row(5, "Kavya Kulkarni", "CW-A6"),
                        row(6, "Suresh Nair", "A1"),
                    ),
                ),
            ),
            HallGrid(),
        )
        // Not force-fitted into the hall rows.
        assertEquals(1, plan.cells.flatten().count { it.seated != null })
        // CW before CH, trailing number ASCENDING — CW-A1/B1 first, nearest
        // the teacher. Default paint is bottom-to-top (A1 last in the list).
        assertEquals(
            listOf("CW-B1", "CW-A3", "CW-A6", "CH-2", "CH-12"),
            plan.chowkyChair.map { it.row.seat },
        )
        assertEquals(
            listOf("CH-12", "CH-2", "CW-A6", "CW-A3", "CW-B1"),
            railPaintOrder(plan.chowkyChair).map { it.row.seat },
        )
        assertEquals(
            plan.chowkyChair.map { it.row.seat },
            railPaintOrder(plan.chowkyChair, ChowkyRailLayout.WRAP).map { it.row.seat },
        )
        assertEquals(ChowkyRailLayout.SINGLE_ROW, HallGrid().chowkyRail)
    }

    @Test
    fun railClassificationReadsSeatKindNotThePrefix() {
        // A row whose enum and prefix disagree can only come from a future
        // parser change — the grid must follow the enum, the single source.
        val plan = hallLayout(
            listOf(
                group(
                    rows = listOf(
                        row(1, "Asha Pawar", "CW-A1"),                        // CELL via parser convention
                        row(2, "Rohan Jadhav", "K-7", kind = SeatKind.CHAIR), // no CH- prefix
                    ),
                ),
            ),
            HallGrid(),
        )
        assertEquals(listOf("CW-A1"), plan.chowkySeats.map { it.row.seat })
        assertEquals(listOf("K-7"), plan.chairSeats.map { it.row.seat })
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
    fun sevaksHideFromTheVisibleUnseatedButStayInTheTally() {
        val plan = hallLayout(
            listOf(
                group(
                    listOf(
                        row(1, "Suresh Nair", "A1"),
                        row(2, "Karan Velu", "", roleTag = "Sevak"),
                        row(3, "Ganesh Bhat", "", roleTag = "sevak"),
                        row(4, "Tara Singh", "", roleTag = "SAT-2011"),
                        row(5, "Anup Datta", ""),
                    ),
                ),
            ),
            HallGrid(),
        )
        // Case-insensitive: both sevaks hide; other reasons keep showing.
        assertEquals(listOf("Tara Singh", "Anup Datta"), plan.unseatedVisible.map { it.row.name })
        // The raw list is intact and the tally counts everyone — sevaks sit
        // on cushions in the hall.
        assertEquals(4, plan.unseated.size)
        assertEquals(5, plan.oldCount)
        assertEquals(0, plan.newCount)
    }

    @Test
    fun visibleUnseatedEmptiesWhenOnlySevaksAreUnseated() {
        val plan = hallLayout(
            listOf(group(listOf(row(1, "Suresh Nair", "A1"), row(2, "Karan Velu", "", roleTag = "Sevak")))),
            HallGrid(),
        )
        assertTrue(plan.unseatedVisible.isEmpty())
        assertEquals(1, plan.unseated.size)
        assertEquals(2, plan.oldCount)
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
        // Every row counts — seated, rail and unseated (sevaks included).
        assertEquals(2, plan.oldCount)
        assertEquals(3, plan.newCount)
        // Old/new rides each placed seat from its band. B2 = column B depth 2.
        assertEquals(true, plan.cells[0][0].seated?.old)
        assertEquals(false, plan.cells[1][1].seated?.old)
        assertEquals(true, plan.chowkyChair.single().old)
        assertEquals(1, plan.chowkySeats.size)
        assertEquals(0, plan.chairSeats.size)
        assertEquals(4, plan.seatedCount)
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
        assertEquals(HallGrid(26, 40), HallGrid(400, 99).clamped())
        val prefs = CentreOpsPrefs().withHallGrid(Gender.M, HallGrid(columns = 99, depth = 0))
        assertEquals(HallGrid(26, 1), prefs.hallGridFor(Gender.M))
        // Read-side clamp too, in case a stale blob carries out-of-range values.
        assertEquals(
            HallGrid(1, 40),
            CentreOpsPrefs(hallGrid = mapOf("F" to HallGrid(0, 50))).hallGridFor(Gender.F),
        )
        // Unset gender falls back to the 7-column, 5-deep default.
        assertEquals(HallGrid(7, 5), CentreOpsPrefs().hallGridFor(Gender.F))
    }

    @Test
    fun oldFieldNamesInPersistedJsonDecodeToTheDefaults() {
        // The 2c blob carried rows/seatsPerRow; the r2 rename drops them.
        // ignoreUnknownKeys (the SessionStore config) lands on the defaults —
        // recorded, acceptable: the registrar re-saves.
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val decoded = json.decodeFromString(HallGrid.serializer(), """{"rows":9,"seatsPerRow":11}""")
        assertEquals(HallGrid(columns = 7, depth = 5), decoded)
    }

    @Test
    fun garbageLabelsFallToUnseatedRatherThanStretchingThePlan() {
        // "9999" flows to depth 1429 — past the 2× cap on the depth axis.
        val plan = hallLayout(
            listOf(group(listOf(row(1, "Suresh Nair", "9999"), row(2, "Vikram Joshi", "A1")))),
            HallGrid(columns = 7, depth = 5),
        )
        assertEquals(5, plan.depth)
        assertEquals(listOf("Suresh Nair"), plan.unseated.map { it.row.name })
        assertEquals("Vikram Joshi", plan.cells[0][0].seated?.row?.name)
    }

    @Test
    fun labelsPastTheTwoTimesCapOnEitherAxisFallToUnseated() {
        // Column cap: "BA1" is index 52 == MAX_PLAN_COLUMNS — rejected below.
        val plan = hallLayout(
            listOf(
                group(
                    listOf(
                        row(1, "Suresh Nair", "A81"),
                        row(2, "Vikram Joshi", "A80"),
                    ),
                ),
            ),
            HallGrid(columns = 7, depth = 5),
        )
        assertEquals(listOf("Suresh Nair"), plan.unseated.map { it.row.name })
        assertEquals("Vikram Joshi", plan.cells[79][0].seated?.row?.name)
        assertEquals(80, plan.depth)
    }

    /** Gate review r2 F2: the COLUMN cap has its own rejection path. */
    @Test
    fun aLabelAtTheColumnCapFallsToUnseatedNotAStretchedPlan() {
        val groups = listOf(
            group(listOf(row(1, "Ram Sharma", "A1"), row(2, "Shyam Verma", "BA1"))),
        )
        val plan = hallLayout(groups, HallGrid(columns = 7, depth = 5))
        // BA = column index 52 = MAX_PLAN_COLUMNS — visible in UNSEATED, never dropped.
        assertEquals(listOf("Shyam Verma"), plan.unseatedVisible.map { it.row.name })
        assertTrue(plan.cells.all { r -> r.all { it.seated == null || it.seated?.row?.name == "Ram Sharma" } })
    }
}
