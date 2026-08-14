package org.dhamma.dipi.staff.audit

import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ClientAuditTest {
    private fun card(
        id: Int = 1,
        given: String = "Meera",
        family: String = "Deshpande",
        mobile: String? = "+91 98220 41783",
        age: Int? = 34,
        dob: String? = "11 Mar 1992",
        emergency: Boolean? = true,
    ) = ApplicantCard(
        id = ApplicantId(id),
        centreId = CentreId(1),
        courseId = CourseId(10),
        givenName = given,
        familyName = family,
        gender = Gender.F,
        status = ApplicantStatus("Confirmed"),
        type = ApplicantType.Student,
        oldStudent = true,
        attended = false,
        mobile = mobile,
        age = age,
        dob = dob,
        emergencyPresent = emergency,
    )

    @Test
    fun phonePrefixInvalidWhenStartsWith5() {
        val f = ClientAudit.phonePrefix(card(mobile = "+91 50031 55402"))
        assertNotNull(f)
        assertEquals("phone_prefix_invalid", f!!.ruleId)
    }

    @Test
    fun phonePrefixOkFor982() {
        assertNull(ClientAudit.phonePrefix(card(mobile = "+91 98220 41783")))
    }

    @Test
    fun missingEmergencyWhenServerSaysAbsent() {
        val f = ClientAudit.missingEmergency(card(emergency = false))
        assertEquals("missing_field", f!!.ruleId)
    }

    @Test
    fun ageDobMismatch() {
        val f = ClientAudit.ageDob(card(age = 39, dob = "30 Oct 1984"))
        assertEquals("age_dob_mismatch", f!!.ruleId)
    }

    @Test
    fun nameTitleSister() {
        val f = ClientAudit.nameTitle(card(given = "Sister", family = "Uma Rangan"))
        assertEquals("name_title_prefix", f!!.ruleId)
    }

    @Test
    fun sharedMobile() {
        val a = card(id = 1, mobile = "+91 82330 90417")
        val b = card(id = 2, given = "Rekha", family = "Kulkarni", mobile = "8233090417")
        val f = ClientAudit.sharedMobile(a, listOf(b))
        assertEquals("shared_mobile", f!!.ruleId)
    }

    @Test
    fun mergeDedupesByRuleId() {
        val a = ClientAudit.phonePrefix(card(mobile = "+91 50031 55402"))!!
        val merged = ClientAudit.merge(listOf(a), listOf(a.copy(label = "server")))
        assertEquals(1, merged.size)
        assertEquals("server", merged[0].label)
    }
}
