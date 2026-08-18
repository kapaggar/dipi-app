package org.dhamma.dipi.staff.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsReportParserTest {
    @Test
    fun readsDatasetNotTheEmptyTable() {
        val rows = SmsReportParser.courses(MockFixtures.smsReportHtml(1))
        assertEquals(listOf(10, 11), rows.map { it.courseId })
        assertEquals("10-Day", rows[0].course)
        assertEquals(42, rows[0].count)
        assertEquals(7, rows[1].count)
    }

    @Test
    fun emptyDatasetAndMissingScriptYieldNoRows() {
        assertTrue(SmsReportParser.courses("<table id=\"table-applicants\"></table>").isEmpty())
        assertTrue(SmsReportParser.courses(MockFixtures.accessDeniedHtml).isEmpty())
    }

    @Test
    fun expandFragmentIsLetterIdNameCount() {
        val letters = SmsReportParser.letters(MockFixtures.smsCountHtml(10))
        assertEquals(2, letters.size)
        assertEquals("12", letters[0].letterId)
        assertEquals("Confirmed", letters[0].name)
        assertEquals("30", letters[0].count)
        assertEquals("Expected", letters[1].name)
    }
}
