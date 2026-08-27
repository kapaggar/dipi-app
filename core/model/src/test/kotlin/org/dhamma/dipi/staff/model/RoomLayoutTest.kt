package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RoomLayoutTest {

    @Test
    fun anUnsetBlockFallsBackToTheDefault() {
        assertEquals(RoomLayout.DEFAULT_COLUMNS, RoomLayout().columnsFor(Gender.M, "Mbk"))
    }

    @Test
    fun genderAndSectionAreIndependentScopes() {
        val l = RoomLayout()
            .withColumns(Gender.M, "Mbk", 7)
            .withColumns(Gender.F, "Fbk", 5)
        assertEquals(7, l.columnsFor(Gender.M, "Mbk"))
        assertEquals(5, l.columnsFor(Gender.F, "Fbk"))
        // A section the registrar never touched keeps the default.
        assertEquals(RoomLayout.DEFAULT_COLUMNS, l.columnsFor(Gender.M, "Guest"))
        // Same section name under the other gender is a different block.
        assertEquals(RoomLayout.DEFAULT_COLUMNS, l.columnsFor(Gender.F, "Mbk"))
    }

    @Test
    fun columnsAreClampedOnWriteAndOnRead() {
        assertEquals(RoomLayout.MAX_COLUMNS, RoomLayout().withColumns(Gender.M, "Mbk", 99).columnsFor(Gender.M, "Mbk"))
        assertEquals(RoomLayout.MIN_COLUMNS, RoomLayout().withColumns(Gender.M, "Mbk", 0).columnsFor(Gender.M, "Mbk"))
        // Corrupt stored JSON must not produce a zero-column grid.
        assertEquals(RoomLayout.MIN_COLUMNS, RoomLayout(mapOf("M|Mbk" to -3)).columnsFor(Gender.M, "Mbk"))
    }

    @Test
    fun rowsAreDerivedByCeilingDivision() {
        assertEquals(10, RoomLayout.rowsFor(rooms = 70, columns = 7))
        assertEquals(18, RoomLayout.rowsFor(rooms = 70, columns = 4))
        assertEquals(1, RoomLayout.rowsFor(rooms = 3, columns = 7))
        assertEquals(0, RoomLayout.rowsFor(rooms = 0, columns = 7))
        assertEquals(0, RoomLayout.rowsFor(rooms = 5, columns = 0))
    }

    @Test
    fun rewritingABlockReplacesRatherThanAccumulates() {
        val l = RoomLayout().withColumns(Gender.M, "Mbk", 7).withColumns(Gender.M, "Mbk", 3)
        assertEquals(3, l.columnsFor(Gender.M, "Mbk"))
        assertEquals(1, l.columns.size)
    }
}
