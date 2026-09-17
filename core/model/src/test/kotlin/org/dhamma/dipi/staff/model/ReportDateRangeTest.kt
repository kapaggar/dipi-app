package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReportDateRangeTest {

    @Test
    fun parseAcceptsIsoAndDayFirstAndRejectsImpossibleDays() {
        assertEquals(LocalDate.of(2026, 9, 1), parseReportDate("2026-09-01"))
        assertEquals(LocalDate.of(2026, 8, 31), parseReportDate("31-08-2026"))
        assertEquals(LocalDate.of(2024, 2, 29), parseReportDate("29-02-2024"))
        assertNull(parseReportDate("2025-02-29"))
        assertNull(parseReportDate("31-02-2026"))
        assertNull(parseReportDate(""))
        assertNull(parseReportDate("2026-09"))
    }

    @Test
    fun compareUsesParsedDatesNotStringOrder() {
        // String "31-08-2026" > "01-09-2026", but 31 Aug is before 1 Sep.
        assertFalse(reportRangeIsValid("01-09-2026", "31-08-2026"))
        assertFalse(reportRangeIsValid("2026-09-01", "2026-08-31"))
        assertTrue(reportRangeIsValid("2026-08-31", "2026-09-01"))
        assertTrue(reportRangeIsValid("2026-09-01", "2026-09-01"))
        assertFalse(reportRangeIsValid("2026-09-01", ""))
        assertFalse(reportRangeIsValid("not-a-date", "2026-09-01"))
    }

    @Test
    fun reversedRangeUsesTheOwnerInlineCopy() {
        assertEquals(REPORT_RANGE_ERROR, reportRangeError("2026-09-01", "2026-08-31"))
        assertEquals(REPORT_RANGE_ERROR, reportRangeError("01-09-2026", "31-08-2026"))
        assertNull(reportRangeError("2026-09-01", "2026-09-30"))
        assertNull(reportRangeError("2026-09-01", ""))
        assertNull(reportRangeError("2025-02-29", "2025-03-01"))
    }

    @Test
    fun presetsAreRelativeToTheSuppliedCalendarDay() {
        val today = LocalDate.of(2026, 9, 16)
        assertEquals("2026-09-01" to "2026-09-30", reportPresetRange(ReportPreset.THIS_MONTH, today))
        assertEquals("2026-01-01" to "2026-12-31", reportPresetRange(ReportPreset.THIS_YEAR, today))
        assertEquals("2025-09-17" to "2026-09-16", reportPresetRange(ReportPreset.LAST_12_MONTHS, today))
    }

    @Test
    fun lastTwelveMonthsOnTheNinthMatchesTheInclusiveWindow() {
        val today = LocalDate.of(2026, 9, 9)
        assertEquals("2025-09-10" to "2026-09-09", reportPresetRange(ReportPreset.LAST_12_MONTHS, today))
        assertEquals("2026-09-01" to "2026-09-30", reportPresetRange(ReportPreset.THIS_MONTH, today))
    }
}
