package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ReportDateRangeTest {
    @Test
    fun leapDayIsValid() {
        assertNull(reportRangeError("2024-02-29", "2024-02-29"))
    }

    @Test
    fun invalidAndInvertedRangesFail() {
        assertNotNull(reportRangeError("2025-02-29", "2025-02-29"))
        assertNotNull(reportRangeError("", "2026-09-01"))
        assertNotNull(reportRangeError("2026-09-30", "2026-09-01"))
    }

    @Test
    fun presetsUseDeviceLocalCalendar() {
        val today = LocalDate.of(2026, 9, 9)
        assertEquals("2026-09-01" to "2026-09-30", reportPresetRange(ReportPreset.THIS_MONTH, today))
        assertEquals("2026-01-01" to "2026-12-31", reportPresetRange(ReportPreset.THIS_YEAR, today))
        assertEquals("2025-09-10" to "2026-09-09", reportPresetRange(ReportPreset.LAST_12_MONTHS, today))
    }
}
