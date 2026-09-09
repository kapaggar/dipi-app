package org.dhamma.dipi.staff.audit

import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.AuditFlag
import org.dhamma.dipi.staff.model.AuditSeverity
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.ConfNo
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuditRelatedApplicantsTest {

    private fun card(
        id: Int,
        given: String = "Synthetic",
        family: String = "Applicant $id",
        status: String = "Confirmed",
        mobile: String? = "900000000$id",
        email: String? = "applicant$id@example.test",
        conf: String? = "OM$id",
        dob: String? = "1 Jan 1990",
    ) = ApplicantCard(
        id = ApplicantId(id),
        centreId = CentreId(1),
        courseId = CourseId(10),
        givenName = given,
        familyName = family,
        gender = Gender.M,
        status = ApplicantStatus(status),
        type = ApplicantType.Student,
        oldStudent = true,
        attended = false,
        confNo = conf?.let { ConfNo(it) },
        email = email,
        mobile = mobile,
        city = "Pune",
        state = "Maharashtra",
        country = "India",
        age = 34,
        dob = dob,
        emergencyPresent = true,
        idPresent = true,
        emergencyNamePresent = true,
        emergencyEqSelf = false,
    )

    @Test
    fun sharedPhoneSuppliesPartnerId() {
        val a = card(1, mobile = "+91 98220 41783")
        val b = card(2, mobile = "9822041783", family = "Other")
        val flag = ClientAudit.evaluate(a, listOf(a, b)).first { it.ruleId == "within_file_duplicate" }
        assertEquals(listOf(b.id), flag.relatedApplicantIds)
    }

    @Test
    fun inactiveDuplicateIsNotAnActivePartner() {
        val a = card(1, mobile = "+91 98220 41783")
        val b = card(2, mobile = "9822041783", family = "Other", status = "Duplicate")
        assertTrue(ClientAudit.evaluate(a, listOf(a, b)).none { it.ruleId == "within_file_duplicate" })
    }

    @Test
    fun mergeKeepsServerDetailAndFillsMissingRelation() {
        val a = card(1, mobile = "+91 98220 41783")
        val b = card(2, mobile = "9822041783", family = "Other")
        val client = ClientAudit.evaluate(a, listOf(a, b)).first { it.ruleId == "within_file_duplicate" }
        val server = AuditFlag(
            AuditSeverity.HARD,
            "Same person may be entered twice",
            "server detail must survive",
            "within_file_duplicate",
        )
        val merged = ClientAudit.merge(listOf(client), listOf(server)).first { it.ruleId == "within_file_duplicate" }
        assertEquals("server detail must survive", merged.detail)
        assertEquals(listOf(b.id), merged.relatedApplicantIds)
        assertEquals("Same person may be entered twice", merged.label)
    }

    @Test
    fun sharedEmailUsesActualGroupIds() {
        val a = card(1, email = "shared@example.test", family = "Patel")
        val b = card(2, email = "shared@example.test", family = "Khan")
        val flag = ClientAudit.evaluate(a, listOf(a, b)).first { it.ruleId == "shared_email_unrelated" }
        assertEquals(listOf(b.id), flag.relatedApplicantIds)
    }

    @Test
    fun serverOnlyFindingHasNoInventedPartner() {
        val server = AuditFlag(
            AuditSeverity.SOFT,
            "Also active in another course",
            "cross_course_duplicate · mentioned someone",
            "cross_course_duplicate",
        )
        val merged = ClientAudit.merge(emptyList(), listOf(server)).single()
        assertTrue(merged.relatedApplicantIds.isEmpty())
        assertEquals("cross_course_duplicate · mentioned someone", merged.detail)
        assertNull(merged.relatedApplicantIds.firstOrNull())
    }
}
