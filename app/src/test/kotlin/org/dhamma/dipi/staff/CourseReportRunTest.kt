package org.dhamma.dipi.staff

import org.dhamma.dipi.staff.model.ReportPreset
import org.dhamma.dipi.staff.model.reportPresetRange
import org.dhamma.dipi.staff.model.reportRangeError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CourseReportRunTest {
    @Test
    fun invalidRangeDoesNotLookRunnable() {
        assertNotNull(reportRangeError("2026-09-30", "2026-09-01"))
        assertNotNull(reportRangeError("", "2026-09-01"))
        assertNull(reportRangeError("2026-09-01", "2026-09-30"))
    }

    @Test
    fun presetFillsInclusiveLastTwelveMonthsWithoutRunning() {
        val (from, to) = reportPresetRange(ReportPreset.LAST_12_MONTHS, LocalDate.of(2026, 9, 9))
        assertEquals("2025-09-10", from)
        assertEquals("2026-09-09", to)
    }
}
