package org.dhamma.dipi.staff.network

import org.dhamma.dipi.staff.model.ApplicantStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolRulesTest {
    @Test
    fun loginBlockReadsUserLoginForm() {
        val html = """
            <form action="/home?destination=home" id="user-login-form">
              <input type="hidden" name="form_build_id" value="form-xyz" />
              <input type="hidden" name="form_id" value="user_login_block" />
              <input type="password" name="pass" />
            </form>
        """.trimIndent()
        val block = SearchPageParser.loginBlock(html)
        assertNotNull(block)
        assertEquals("form-xyz", block!!.formBuildId)
        assertEquals("user_login_block", block.formId)
        assertEquals("/home?destination=home", block.action)
    }

    @Test
    fun sheetChoicesNeverIncludeApproved() {
        assertFalse(ApplicantStatus.SHEET_CHOICES.any { it.equals("Approved", true) })
        val merged = ApplicantStatus.mergeChoices(listOf("Confirmed", "Approved", "Expected"))
        assertFalse(merged.any { it.equals("Approved", true) })
        assertTrue(merged.contains("Confirmed"))
    }

    @Test
    fun datasetDropsRawNpiFromDtoString() {
        val html = """
            <script>
            var dataset = [{"aid":99,"name":"Meera Deshpande","gender":"Female","courseid":42,"centreid":1,
            "app_status":"Confirmed","aadhar":"123412341234","passport":"X9"}];
            </script>
        """.trimIndent()
        val page = SearchPageParser.parse(html)
        val dto = page.dataset.single()
        assertFalse(dto.toString().contains("123412341234"))
        assertEquals(true, dto.idPresent)
        assertEquals("Aadhaar", page.sensitive[99]?.idLabel)
    }

    @Test
    fun healthNoiseDropsFineKeepsMedication() {
        assertFalse(HealthNoiseFilter.keep("fine"))
        assertTrue(HealthNoiseFilter.keep("takes insulin twice daily"))
        assertFalse(HealthNoiseFilter.keep("No", pregnancy = true, male = false))
        assertFalse(HealthNoiseFilter.keep("Yes (3 months)", pregnancy = true, male = true))
    }

    @Test
    fun loginErrorReadsDrupalMessage() {
        val html = """<div class="messages error">Sorry, unrecognized username or password.</div>"""
        assertEquals("Sorry, unrecognized username or password.", SearchPageParser.loginError(html))
        assertNull(SearchPageParser.loginError("<html>ok</html>"))
    }
}
