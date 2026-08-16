package org.dhamma.dipi.staff.audit

import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.AuditSeverity
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.ConfNo
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientAuditTest {
    /** Fully clean by default so each test breaks exactly one thing. */
    private fun card(
        id: Int = 1,
        given: String = "Meera",
        family: String = "Deshpande",
        gender: Gender = Gender.F,
        status: String = "Confirmed",
        type: ApplicantType = ApplicantType.Student,
        conf: String? = "NF12",
        mobile: String? = "+91 98220 41783",
        email: String? = "meera@example.com",
        city: String? = "Pune",
        state: String? = "Maharashtra",
        country: String? = "India",
        age: Int? = 34,
        dob: String? = "11 Mar 1992",
        emergency: Boolean? = true,
        emergencyName: Boolean? = true,
        emergencyEqSelf: Boolean? = false,
        idPresent: Boolean? = true,
    ) = ApplicantCard(
        id = ApplicantId(id),
        centreId = CentreId(1),
        courseId = CourseId(10),
        givenName = given,
        familyName = family,
        gender = gender,
        status = ApplicantStatus(status),
        type = type,
        oldStudent = true,
        attended = false,
        confNo = conf?.let { ConfNo(it) },
        email = email,
        mobile = mobile,
        city = city,
        state = state,
        country = country,
        age = age,
        dob = dob,
        emergencyPresent = emergency,
        idPresent = idPresent,
        emergencyNamePresent = emergencyName,
        emergencyEqSelf = emergencyEqSelf,
    )

    /* ── phone ─────────────────────────────────────────────────────── */

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
    fun phonePrefixFlagsBareNumberWithBlankCountry() {
        // audit.js treats blank Country as India.
        val f = ClientAudit.phonePrefix(card(mobile = "5003155402", country = null))
        assertEquals("phone_prefix_invalid", f!!.ruleId)
    }

    @Test
    fun phonePrefixSkipsForeignCountry() {
        assertNull(ClientAudit.phonePrefix(card(mobile = "4165550123", country = "Canada")))
    }

    @Test
    fun phoneShortFlagsEightDigits() {
        val f = ClientAudit.phoneShort(card(mobile = "98220417"))
        assertEquals("phone_short", f!!.ruleId)
        assertEquals(AuditSeverity.HARD, f.severity)
        // and the prefix rule leaves short numbers alone
        assertNull(ClientAudit.phonePrefix(card(mobile = "98220417")))
        assertNull(ClientAudit.phoneShort(card(mobile = "+91 98220 41783")))
    }

    /* ── email ─────────────────────────────────────────────────────── */

    @Test
    fun emailMissingMalformedAndOk() {
        assertEquals("email_missing", ClientAudit.email(card(email = null))!!.ruleId)
        assertEquals("email_missing", ClientAudit.email(card(email = "  "))!!.ruleId)
        assertEquals("email_malformed", ClientAudit.email(card(email = "meera[at]x.com"))!!.ruleId)
        assertEquals("email_malformed", ClientAudit.email(card(email = "meera@nodot"))!!.ruleId)
        assertNull(ClientAudit.email(card(email = "meera@example.com")))
    }

    /* ── id / missing fields ───────────────────────────────────────── */

    @Test
    fun idMissingFiresOnlyOnExplicitFalse() {
        assertEquals("id_missing", ClientAudit.idMissing(card(idPresent = false))!!.ruleId)
        assertNull(ClientAudit.idMissing(card(idPresent = true)))
        assertNull(ClientAudit.idMissing(card(idPresent = null)))
    }

    @Test
    fun missingFieldsListsEveryBlank() {
        val f = ClientAudit.missingFields(
            card(city = null, state = "", emergency = false, emergencyName = false),
        )
        assertEquals("missing_field", f!!.ruleId)
        assertTrue(f.detail.contains("'City'"))
        assertTrue(f.detail.contains("'State'"))
        assertTrue(f.detail.contains("'Emergency Name'"))
        assertTrue(f.detail.contains("'Emergency Contact No'"))
        assertNull(ClientAudit.missingFields(card()))
    }

    @Test
    fun missingEmergencyBooleanNullMeansUnknownNotMissing() {
        assertNull(ClientAudit.missingFields(card(emergency = null, emergencyName = null)))
    }

    /* ── age / dob ─────────────────────────────────────────────────── */

    @Test
    fun ageDobMismatch() {
        val f = ClientAudit.ageDob(card(age = 39, dob = "30 Oct 1984"))
        assertEquals("age_dob_mismatch", f!!.ruleId)
        assertNull(ClientAudit.ageDob(card(age = 34, dob = "11 Mar 1992")))
    }

    @Test
    fun ageRangeUnderAndOver() {
        assertEquals("age_under_min", ClientAudit.ageRange(card(dob = "2 Jun 2015", age = 11))!!.ruleId)
        assertEquals("age_over_max", ClientAudit.ageRange(card(dob = "2 Jun 1920", age = 106))!!.ruleId)
        assertNull(ClientAudit.ageRange(card(dob = "11 Mar 1992")))
        assertNull(ClientAudit.ageRange(card(dob = null)))
    }

    /* ── conf number ───────────────────────────────────────────────── */

    @Test
    fun confGenderMismatch() {
        val f = ClientAudit.confGender(card(conf = "NM5", gender = Gender.F))
        assertEquals("conf_gender_mismatch", f!!.ruleId)
        assertNull(ClientAudit.confGender(card(conf = "NF5", gender = Gender.F)))
    }

    @Test
    fun confTypeMismatchSevakPrefix() {
        assertEquals(
            "conf_type_mismatch",
            ClientAudit.confType(card(conf = "SF4", type = ApplicantType.Student))!!.ruleId,
        )
        assertEquals(
            "conf_type_mismatch",
            ClientAudit.confType(card(conf = "NF4", type = ApplicantType.Sevak))!!.ruleId,
        )
        assertNull(ClientAudit.confType(card(conf = "SF4", type = ApplicantType.Sevak)))
        assertNull(ClientAudit.confType(card(conf = "NF4", type = ApplicantType.Student)))
    }

    @Test
    fun confNoDuplicateWithinCourse() {
        val a = card(id = 1, conf = "NF7")
        val b = card(id = 2, given = "Rekha", family = "Kulkarni", conf = "NF7")
        val f = ClientAudit.confNoDuplicate(a, listOf(b))
        assertEquals("conf_no_duplicate", f!!.ruleId)
        assertNull(ClientAudit.confNoDuplicate(a, listOf(card(id = 3, conf = "NF8"))))
    }

    /* ── duplicates ────────────────────────────────────────────────── */

    @Test
    fun withinFileDuplicateByPhoneAndByNameDob() {
        val a = card(id = 1, mobile = "+91 82330 90417")
        val byPhone = card(id = 2, given = "Rekha", family = "Kulkarni", mobile = "8233090417")
        assertEquals("within_file_duplicate", ClientAudit.withinFileDuplicate(a, listOf(byPhone))!!.ruleId)

        val byNameDob = card(id = 3, mobile = "9000000001", dob = "11 Mar 1992")
        val self = card(id = 1, mobile = "9000000002", dob = "11 Mar 1992")
        val f = ClientAudit.withinFileDuplicate(self, listOf(byNameDob))
        assertEquals("within_file_duplicate", f!!.ruleId)
        assertTrue(f.detail.contains("name+DOB"))

        val unrelated = card(id = 4, given = "Priya", family = "Nair", mobile = "9000000003", dob = "1 Jan 1990")
        assertNull(ClientAudit.withinFileDuplicate(a, listOf(unrelated)))
    }

    /* ── status ────────────────────────────────────────────────────── */

    @Test
    fun statusUnknownIgnoresKnownAndLiveDeskStatuses() {
        assertNull(ClientAudit.statusUnknown(card(status = "Confirmed")))
        assertNull(ClientAudit.statusUnknown(card(status = "Pending")))
        assertNull(ClientAudit.statusUnknown(card(status = "Reconfirmation")))
        assertEquals("status_unknown", ClientAudit.statusUnknown(card(status = "Wibble"))!!.ruleId)
    }

    /* ── name title ────────────────────────────────────────────────── */

    @Test
    fun nameTitleSister() {
        val f = ClientAudit.nameTitle(card(given = "Sister", family = "Uma Rangan"))
        assertEquals("name_title_prefix", f!!.ruleId)
    }

    @Test
    fun nameTitleHandlesDotsAndMultiWordTitles() {
        assertEquals("'Dr.'", ClientAudit.nameTitle(card(given = "Dr.Ramesh", family = "Patil"))!!.detail.substringAfter("· "))
        assertEquals("'Lt Col'", ClientAudit.nameTitle(card(given = "Lt", family = "Col Arun Mehta"))!!.detail.substringAfter("· "))
    }

    @Test
    fun nameTitleIgnoresStandaloneGivenNames() {
        // "Baby" and "Kumari" alone are real names, not titles.
        assertNull(ClientAudit.nameTitle(card(given = "Baby", family = "")))
        assertNull(ClientAudit.nameTitle(card(given = "Kumari", family = "")))
        assertNull(ClientAudit.nameTitle(card(given = "Meera", family = "Deshpande")))
    }

    /* ── safety ────────────────────────────────────────────────────── */

    @Test
    fun emergencyEqSelfFromParseTimeBoolean() {
        val f = ClientAudit.emergencyEqSelf(card(emergencyEqSelf = true))
        assertEquals("emergency_eq_self", f!!.ruleId)
        assertEquals(AuditSeverity.SAFETY, f.severity)
        assertNull(ClientAudit.emergencyEqSelf(card(emergencyEqSelf = false)))
        assertNull(ClientAudit.emergencyEqSelf(card(emergencyEqSelf = null)))
    }

    @Test
    fun emergencyPartialNeedsExactlyOneHalf() {
        val f = ClientAudit.emergencyPartial(card(emergencyName = true, emergency = false))
        assertEquals("emergency_partial", f!!.ruleId)
        assertEquals(AuditSeverity.SAFETY, f.severity)
        assertNotNull(ClientAudit.emergencyPartial(card(emergencyName = false, emergency = true)))
        assertNull(ClientAudit.emergencyPartial(card(emergencyName = true, emergency = true)))
        assertNull(ClientAudit.emergencyPartial(card(emergencyName = false, emergency = false)))
        assertNull(ClientAudit.emergencyPartial(card(emergencyName = null, emergency = true)))
    }

    /* ── soft ──────────────────────────────────────────────────────── */

    @Test
    fun sharedMobile() {
        val a = card(id = 1, mobile = "+91 82330 90417")
        val b = card(id = 2, given = "Rekha", family = "Kulkarni", mobile = "8233090417")
        val f = ClientAudit.sharedMobile(a, listOf(b))
        assertEquals("shared_mobile", f!!.ruleId)
        assertEquals(AuditSeverity.SOFT, f.severity)
    }

    @Test
    fun sharedEmailUnrelatedNeedsDifferentSurnames() {
        val a = card(id = 1, email = "family@example.com")
        val otherSurname = card(id = 2, given = "Priya", family = "Nair", email = "Family@Example.com")
        val sameSurname = card(id = 3, given = "Rohan", family = "Deshpande", email = "family@example.com")
        assertEquals(
            "shared_email_unrelated",
            ClientAudit.sharedEmailUnrelated(a, listOf(otherSurname))!!.ruleId,
        )
        assertNull(ClientAudit.sharedEmailUnrelated(a, listOf(sameSurname)))
        assertNull(ClientAudit.sharedEmailUnrelated(card(id = 4, email = null), listOf(otherSurname)))
    }

    /* ── evaluate ──────────────────────────────────────────────────── */

    @Test
    fun evaluateGatesNonActiveRowsLikeAuditJs() {
        // audit.js runs every rule except status_unknown on Expected/Confirmed only.
        val received = card(status = "Received", mobile = "+91 50031 55402", conf = null)
        assertTrue(ClientAudit.evaluate(received).isEmpty())
        assertEquals(
            listOf("status_unknown"),
            ClientAudit.evaluate(card(status = "Wibble", conf = null)).map { it.ruleId },
        )
    }

    @Test
    fun evaluateCleanActiveCardHasNoFindings() {
        assertTrue(ClientAudit.evaluate(card()).isEmpty())
    }

    @Test
    fun evaluateOnlyComparesAgainstActiveMates() {
        val a = card(id = 1, mobile = "+91 82330 90417")
        val cancelledTwin = card(id = 2, status = "Cancelled", mobile = "8233090417")
        assertTrue(ClientAudit.evaluate(a, listOf(a, cancelledTwin)).isEmpty())
        val activeTwin = card(id = 3, given = "Rekha", family = "Kulkarni", conf = "NF13", mobile = "8233090417")
        val ids = ClientAudit.evaluate(a, listOf(a, activeTwin)).map { it.ruleId }
        assertTrue(ids.contains("shared_mobile"))
        assertTrue(ids.contains("within_file_duplicate"))
    }

    @Test
    fun mergeDedupesByRuleId() {
        val a = ClientAudit.phonePrefix(card(mobile = "+91 50031 55402"))!!
        val merged = ClientAudit.merge(listOf(a), listOf(a.copy(label = "server")))
        assertEquals(1, merged.size)
        assertEquals("server", merged[0].label)
    }
}
