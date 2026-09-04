package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DeskDateTest {
    @Test
    fun displayFlipsIsoToDayFirst() {
        assertEquals("01-01-2026", displayDeskDate("2026-01-01"))
        assertEquals("31-03-2026", displayDeskDate("2026-03-31"))
        assertEquals("not-a-date", displayDeskDate("not-a-date"))
    }

    @Test
    fun parseAcceptsDayFirstAndLeavesIsoAlone() {
        assertEquals("2026-01-01", parseDeskDate("01-01-2026"))
        assertEquals("2026-06-01", parseDeskDate("2026-06-01"))
        assertEquals("2026-06", parseDeskDate("2026-06"))
    }
}
