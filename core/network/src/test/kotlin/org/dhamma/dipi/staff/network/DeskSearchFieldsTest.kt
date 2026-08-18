package org.dhamma.dipi.staff.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeskSearchFieldsTest {
    private val html = MockFixtures.searchAppFormHtml(1)

    @Test
    fun nameQuerySplitsFirstAndLastAndSkipsBulkMail() {
        val fields = DeskSearchFields.of(html, "Meera Deshpande")!!
        assertEquals("Meera", fields["f_name"])
        assertEquals("Deshpande", fields["l_name"])
        assertEquals("Both", fields["type"])
        assertEquals("Search", fields["op"])
        assertEquals("form-SearchBuIlD", fields["form_build_id"])
        assertEquals("mock-form-token", fields["form_token"])
        assertEquals("dh_manageapp_search_form", fields["form_id"])
        assertFalse(fields.containsKey("bulk-mail"))
        assertFalse(fields.containsKey("letters"))
        assertFalse(fields.containsKey("bulk_mail_name"))
        assertFalse(fields.containsKey("bulk-mail-schedule[date]"))
    }

    @Test
    fun confQueryAndOptionalStatus() {
        val fields = DeskSearchFields.of(html, "NF128", status = "Confirmed")!!
        assertEquals("NF128", fields["conf_no"])
        assertFalse(fields.containsKey("f_name"))
        assertEquals("Confirmed", fields["status[]"])
    }

    @Test
    fun blankQueryOrMissingTokens() {
        assertNull(DeskSearchFields.of(html, "  "))
        assertNull(DeskSearchFields.of("<html></html>", "Meera"))
    }

    @Test
    fun tokensArePresentOnTheSearchForm() {
        val tokens = SearchPageParser.tokens(html)!!
        assertEquals("dh_manageapp_search_form", tokens.formId)
        assertTrue(SearchPageParser.parse(html).statuses.contains("Confirmed"))
    }
}
