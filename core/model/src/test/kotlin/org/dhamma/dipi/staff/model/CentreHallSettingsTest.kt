package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CentreHallSettingsTest {

    private val sudha = CentreHallSettings(
        combinedHall = true,
        seatNaming = 1,
        maleSeatsPerRow = 5,
        femaleSeatsPerRow = 2,
        malePlan = HallSeatPlan(
            columns = 5,
            chowkyColumns = 1,
            direction = "right",
            chowkyPosition = "",
            emptySeats = "",
            emptyChowky = "",
        ),
        femalePlan = HallSeatPlan(
            columns = 2,
            chowkyColumns = 2,
            direction = "left",
            chowkyPosition = "",
            emptySeats = "",
            emptyChowky = "",
        ),
    )

    @Test
    fun sudhaLabelsMatchTheLiveMainPlan() {
        assertEquals("Yes", sudha.combinedLabel())
        assertEquals("Alphanumeric - columns A, B, C / rows 1, 2, 3", sudha.seatNamingLabel())
        assertEquals(
            "5 columns · 1 chowky · Left to right · Chowky Default (right) · Empty seats 0",
            sudha.malePlan.summaryLine("right"),
        )
        assertEquals(
            "2 columns · 2 chowky · Right to left · Chowky Default (left) · Empty seats 0",
            sudha.femalePlan.summaryLine("left"),
        )
        assertEquals(ChowkyRailLayout.SINGLE_ROW, sudha.malePlan.chowkyRailOrNull())
        assertEquals(ChowkyRailLayout.WRAP, sudha.femalePlan.chowkyRailOrNull())
    }

    @Test
    fun deskColumnsWinOverAStaleLocalSevenByFive() {
        val prefs = CentreOpsPrefs(hallSettings = sudha)
            .withHallGrid(Gender.M, HallGrid(columns = 7, depth = 5))
            .withHallGrid(Gender.F, HallGrid(columns = 7, depth = 5, chowkyRail = ChowkyRailLayout.SINGLE_ROW))
        assertEquals(5, prefs.hallGridFor(Gender.M).columns)
        assertEquals(5, prefs.hallGridFor(Gender.M).depth)
        assertEquals(ChowkyRailLayout.SINGLE_ROW, prefs.hallGridFor(Gender.M).chowkyRail)
        assertEquals(2, prefs.hallGridFor(Gender.F).columns)
        assertEquals(ChowkyRailLayout.WRAP, prefs.hallGridFor(Gender.F).chowkyRail)
    }

    @Test
    fun localHallLayoutFillsFieldsTheDeskOmits() {
        val prefs = CentreOpsPrefs(
            hallSettings = CentreHallSettings(maleSeatsPerRow = 5, malePlan = HallSeatPlan(columns = 5)),
        ).withHallGrid(Gender.M, HallGrid(columns = 9, depth = 8, chowkyRail = ChowkyRailLayout.WRAP))
        assertEquals(5, prefs.hallGridFor(Gender.M).columns)
        assertEquals(8, prefs.hallGridFor(Gender.M).depth)
        assertEquals(ChowkyRailLayout.WRAP, prefs.hallGridFor(Gender.M).chowkyRail)
        assertEquals(HallGrid.DEFAULT_COLUMNS, prefs.hallGridFor(Gender.F).columns)
    }

    @Test
    fun olderSeatsPerRowAliasStillFeedsRoomWrap() {
        val hall = CentreHallSettings(maleSeatsPerRow = 5, femaleSeatsPerRow = 2)
        assertEquals(5, hall.seatsPerRow(Gender.M))
        assertEquals(2, hall.seatsPerRow(Gender.F))
        assertEquals(5, hall.plan(Gender.M).columns)
        assertEquals(2, RoomLayout().columnsFor(Gender.F, "Fbk", hall.seatsPerRow(Gender.F)))
    }
}
