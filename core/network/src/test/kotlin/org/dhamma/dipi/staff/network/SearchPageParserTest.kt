package org.dhamma.dipi.staff.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchPageParserTest {
    private val html = """
        <form>
        <input type="hidden" name="form_build_id" value="form-abc" />
        <input type="hidden" name="form_token" value="tok-1" />
        <input type="hidden" name="form_id" value="dh_manageapp_search_form" />
        <select id="edit-course" name="course">
          <option value="">Choose</option>
          <option value="42">Dhamma Giri / 10-Day / 20 Aug 2026 - 31 Aug 2026</option>
        </select>
        <select id="edit-app-status" name="status">
          <option value="">Choose</option>
          <option value="Pending">Pending</option>
          <option value="Confirmed">Confirmed</option>
        </select>
        </form>
        <script>
        (function () {
            var dataset = [{"aid":99,"name":"<a href=\"/app/99/edit\">Meera Deshpande</a> (PDF)","gender":"Female","o_n":"Old<br>Female","courseid":42,"centreid":1,"app_status":"Confirmed","confno":"NF128","city":"Pune","state":"Maharashtra","country":"India","age":34,"contact_mobile":"+91 98220 41783","contact_email":"m@x.com","type":"student","aadhar":"1234","passport":"X"}];
            var letters = [];
        })();
        </script>
    """.trimIndent()

    @Test
    fun extractsTokensCoursesAndDatasetWithoutNpi() {
        val page = SearchPageParser.parse(html, pathCentreId = 1)
        assertEquals("form-abc", page.tokens!!.formBuildId)
        assertEquals("tok-1", page.tokens!!.formToken)
        assertEquals(1, page.courses.size)
        assertEquals(42, page.courses[0].id)
        assertTrue(page.statuses.contains("Confirmed"))
        assertEquals(1, page.dataset.size)
        val a = page.dataset[0]
        assertEquals(99, a.id)
        assertEquals("Meera", a.givenName)
        assertEquals("Deshpande", a.familyName)
        assertEquals("Confirmed", a.status)
        assertEquals("NF128", a.confNo)
        assertEquals("F", a.gender)
        assertTrue(a.oldStudent)
        assertFalse(a.toString().contains("1234"))
        assertNotNull(a.email)
    }

    @Test
    fun centreIdFromPath() {
        assertEquals(7, SearchPageParser.centreIdFromPath("/search-app/7"))
        assertEquals(null, SearchPageParser.centreIdFromPath("/search-app"))
    }
}
