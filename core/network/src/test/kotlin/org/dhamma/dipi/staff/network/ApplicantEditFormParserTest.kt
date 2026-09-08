package org.dhamma.dipi.staff.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplicantEditFormParserTest {
    @Test
    fun echoesCurrentFieldsAndFileNameWithoutInventing() {
        val parsed = ApplicantEditFormParser.parse(
            ApplicantEditFormFixtures.completeHtml(),
            41,
            ApplicantEditFormFixtures.ORIGIN,
        )
        val form = (parsed as ApplicantEditFormParse.Ready).form
        assertEquals("/app/41/edit", form.action)
        assertEquals("files[upload_photo]", form.fileFieldName)
        assertEquals("Priya", form.fields["a_f_name"])
        assertEquals("Nair", form.fields["a_l_name"])
        assertEquals("F", form.fields["a_gender"])
        assertEquals("0", form.fields["a_old"])
        assertEquals("1", form.fields["attending"])
        assertEquals("chair", form.fields["special"])
        assertEquals("1", form.fields["a_monk"])
        assertFalse(form.fields.containsKey("a_alist"))
        assertEquals("Confirmed", form.fields["a_status"])
        assertEquals("1990-01-15", form.fields["a_dob[date]"])
        assertEquals("form-AppEdItBuIlD", form.fields["form_build_id"])
        assertEquals("tok-photo-write", form.fields["form_token"])
        assertEquals(ApplicantEditFormParser.LIVE_FORM_ID, form.fields["form_id"])
        assertEquals("Update", form.fields["op"])
        assertEquals(ApplicantEditFormFixtures.NPI_DOC, form.fields["document_id"])
        assertFalse(form.fields.containsKey("Approved"))
        assertFalse(form.toString().contains(ApplicantEditFormFixtures.NPI_DOC))
        assertFalse(form.toString().contains(ApplicantEditFormFixtures.NPI_HEALTH))
    }

    @Test
    fun refusesMissingTokenFileOrApprovedStatus() {
        assertIncomplete(
            ApplicantEditFormFixtures.completeHtml(includeToken = false),
            "tokens",
        )
        assertIncomplete(
            ApplicantEditFormFixtures.completeHtml(includeFile = false),
            "file",
        )
        assertIncomplete(
            ApplicantEditFormFixtures.completeHtml(statusSelected = "Approved"),
            "Approved",
        )
        assertIncomplete(
            ApplicantEditFormFixtures.completeHtml(action = "/app/41/edit?r=1"),
            "safe",
        )
        assertIncomplete(ApplicantEditFormFixtures.loginHtml(), "Sign in")
    }

    @Test
    fun emptyActionUsesEditPathOnSameOrigin() {
        val parsed = ApplicantEditFormParser.parse(
            ApplicantEditFormFixtures.completeHtml(action = ""),
            41,
            ApplicantEditFormFixtures.ORIGIN,
        )
        assertEquals("/app/41/edit", (parsed as ApplicantEditFormParse.Ready).form.action)
    }

    private fun assertIncomplete(html: String, needle: String) {
        val parsed = ApplicantEditFormParser.parse(html, 41, ApplicantEditFormFixtures.ORIGIN)
        assertTrue(parsed is ApplicantEditFormParse.Incomplete)
        assertTrue((parsed as ApplicantEditFormParse.Incomplete).reason.contains(needle, ignoreCase = true))
    }
}
