package org.dhamma.dipi.staff.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyActivityParserTest {
    @Test
    fun scrapesTokensSelectsAndDefaultDates() {
        val form = DailyActivityParser.form(MockFixtures.dailyActivityFormHtml(1))!!
        assertEquals("/daily-activity/1", form.action)
        assertEquals("form-DailyActBuIlD", form.formBuildId)
        assertEquals("mock-form-token", form.formToken)
        assertEquals("dh_daily_activity_form", form.formId)
        assertEquals("2026-08-17", form.startDate)
        assertEquals(listOf("10", "11"), form.courses.map { it.value })
        assertEquals(listOf("Status Change", "Letter", "Deleted"), form.events.map { it.value })
        assertTrue(form.users.any { it.label == "sudha.user" })
        assertTrue(DailyActivityParser.rows(MockFixtures.dailyActivityFormHtml(1)).isEmpty())
    }

    @Test
    fun parsesTableRowsInDeskColumnOrder() {
        val rows = DailyActivityParser.rows(MockFixtures.dailyActivityFormHtml(1, withTable = true))
        assertEquals(2, rows.size)
        assertEquals("Meera Deshpande", rows[0].applicant)
        assertEquals("10-Day", rows[0].course)
        assertEquals("Status Change", rows[0].event)
        assertEquals("Confirmed", rows[0].message)
        assertEquals("sudha.user", rows[0].user)
        assertEquals("2026-08-16 10:22:00", rows[0].at)
        assertEquals("Letter", rows[1].event)
    }

    @Test
    fun emptyPageHasNoTable() {
        assertTrue(DailyActivityParser.rows("<html><body>no rows</body></html>").isEmpty())
        assertNull(DailyActivityParser.form(MockFixtures.accessDeniedHtml))
    }

    @Test
    fun postFieldsCarryScrapedTokensAndFilters() {
        val form = DailyActivityParser.form(MockFixtures.dailyActivityFormHtml(1))!!
        val fields = DailyActivityParser.fields(form, "2026-08-01", "2026-08-17", event = "Letter", course = "10")
        assertEquals("form-DailyActBuIlD", fields["form_build_id"])
        assertEquals("mock-form-token", fields["form_token"])
        assertEquals("dh_daily_activity_form", fields["form_id"])
        assertEquals("Submit", fields["op"])
        assertEquals("2026-08-01", fields["date_start[date]"])
        assertEquals("Letter", fields["event"])
        assertEquals("10", fields["course"])
    }
}
