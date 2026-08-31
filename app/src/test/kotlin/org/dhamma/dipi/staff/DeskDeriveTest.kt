package org.dhamma.dipi.staff

import org.dhamma.dipi.staff.desk.CALL_OUTCOMES
import org.dhamma.dipi.staff.desk.deskCallAgo
import org.dhamma.dipi.staff.desk.deskCallCounts
import org.dhamma.dipi.staff.desk.deskCallList
import org.dhamma.dipi.staff.desk.deskCallLogged
import org.dhamma.dipi.staff.desk.deskCallOutcome
import org.dhamma.dipi.staff.desk.deskCallRank
import org.dhamma.dipi.staff.desk.deskCallSorted
import org.dhamma.dipi.staff.desk.deskCallMeta
import org.dhamma.dipi.staff.desk.deskCallRows
import org.dhamma.dipi.staff.desk.deskWaNumber
import org.dhamma.dipi.staff.desk.deskFindings
import org.dhamma.dipi.staff.desk.deskFreeRooms
import org.dhamma.dipi.staff.desk.deskOccupied
import org.dhamma.dipi.staff.desk.deskRecord
import org.dhamma.dipi.staff.desk.deskRoll
import org.dhamma.dipi.staff.desk.deskRollCell
import org.dhamma.dipi.staff.desk.deskRosterRows
import org.dhamma.dipi.staff.desk.deskScoped
import org.dhamma.dipi.staff.desk.deskSaveSnack
import org.dhamma.dipi.staff.desk.deskSeatCount
import org.dhamma.dipi.staff.desk.stripHonorific
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.AuditFlag
import org.dhamma.dipi.staff.model.AuditSeverity
import org.dhamma.dipi.staff.model.CallRecord
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.model.ConfNo
import org.dhamma.dipi.staff.model.ConfSeniority
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeskDeriveTest {

    private fun card(
        id: Int,
        conf: String? = "NF$id",
        given: String = "Meera",
        family: String = "Deshpande",
        gender: Gender = Gender.F,
        status: String = "Confirmed",
        attended: Boolean = false,
        mobile: String? = "9876543210",
        flags: List<AuditFlag> = emptyList(),
    ) = ApplicantCard(
        id = ApplicantId(id),
        centreId = CentreId(1),
        courseId = CourseId(10),
        givenName = given,
        familyName = family,
        gender = gender,
        status = ApplicantStatus(status),
        type = ApplicantType.Student,
        oldStudent = false,
        attended = attended,
        confNo = conf?.let { ConfNo(it) },
        mobile = mobile,
        flags = flags,
    )

    @Test
    fun rollKeepsConfirmedConfHoldersAndDropsCancelled() {
        val rows = listOf(
            card(1, conf = "NF1"),
            card(2, conf = null),
            card(3, conf = "NM3", status = "Cancelled"),
            card(4, conf = "OM4", status = "Expected"),
        )
        assertEquals(listOf(1, 4), deskRoll(rows).map { it.id.value })
    }

    @Test
    fun rollGenderOverloadScopesWithoutDroppingAnyoneElse() {
        val rows = listOf(
            card(1, conf = "NF1"),
            card(2, conf = "OM2", gender = Gender.M),
            card(3, conf = "NM3", gender = Gender.M, status = "Cancelled"),
        )
        assertEquals(listOf(1, 2), deskRoll(rows, null).map { it.id.value })
        assertEquals(listOf(1), deskRoll(rows, Gender.F).map { it.id.value })
        assertEquals(listOf(2), deskRoll(rows, Gender.M).map { it.id.value })
    }

    @Test
    fun rollScopeReadsConfPrefixNotCardFields() {
        val rows = listOf(
            card(1, conf = "NF1"),
            card(2, conf = "OF2"),
            card(3, conf = "NM3", gender = Gender.M),
            card(4, conf = "OM4", gender = Gender.M),
            card(5, conf = "SM5", gender = Gender.M),
            card(6, conf = "xx6"),
        )
        assertEquals(listOf(1, 3), deskRoll(rows, null, ConfSeniority.NEW).map { it.id.value })
        assertEquals(listOf(2, 4), deskRoll(rows, null, ConfSeniority.OLD).map { it.id.value })
        assertEquals(listOf(1), deskRoll(rows, Gender.F, ConfSeniority.NEW).map { it.id.value })
        assertEquals(listOf(2), deskRoll(rows, Gender.F, ConfSeniority.OLD).map { it.id.value })
        assertEquals(listOf(3), deskRoll(rows, Gender.M, ConfSeniority.NEW).map { it.id.value })
        assertEquals(listOf(4), deskRoll(rows, Gender.M, ConfSeniority.OLD).map { it.id.value })
        // Sevak SM is male + unknown seniority: visible on Male/Both, hidden on New or Old.
        assertEquals(listOf(3, 4, 5), deskRoll(rows, Gender.M, null).map { it.id.value })
        assertFalse(deskRoll(rows, Gender.M, ConfSeniority.NEW).any { it.id.value == 5 })
        // Garbage prefix is unknown × unknown — only visible when both axes are Both.
        assertEquals(listOf(1, 2, 3, 4, 5, 6), deskRoll(rows, null, null).map { it.id.value })
        assertFalse(deskRoll(rows, Gender.F, null).any { it.id.value == 6 })
        assertFalse(deskRoll(rows, null, ConfSeniority.NEW).any { it.id.value == 6 })
    }

    @Test
    fun deskScopedHidesMissingConfWhenAnAxisIsSet() {
        val rows = listOf(card(1, conf = "NF1"), card(2, conf = null))
        assertEquals(listOf(1, 2), deskScoped(rows, null, null).map { it.id.value })
        assertEquals(listOf(1), deskScoped(rows, Gender.F, null).map { it.id.value })
        assertEquals(listOf(1), deskScoped(rows, null, ConfSeniority.NEW).map { it.id.value })
        assertTrue(deskScoped(rows, Gender.M, null).isEmpty())
    }

    @Test
    fun rosterSortsByDisplayNameCaseInsensitiveAndStable() {
        val roll = listOf(
            card(1, conf = "NF1", given = "Priya", family = "Nair"),
            card(2, conf = "OM2", given = "arun", family = "Kale", gender = Gender.M),
            card(3, conf = "NF3", given = "Meera", family = "Deshpande"),
        )
        assertEquals(
            listOf("arun Kale", "Meera Deshpande", "Priya Nair"),
            deskRosterRows(roll, emptyMap(), "", "All").map { it.displayName },
        )
    }

    @Test
    fun rollTableDerivesFromConfPrefixNotTheOldStudentField() {
        val roll = listOf(
            card(1, conf = "OM1"), card(2, conf = "OM2"),
            card(3, conf = "OF3"),
            card(4, conf = "NM4"),
            card(5, conf = "NF5"), card(6, conf = "NF6"),
        )
        assertEquals(2, deskRollCell(roll, 'M', old = true))
        assertEquals(1, deskRollCell(roll, 'F', old = true))
        assertEquals(1, deskRollCell(roll, 'M', old = false))
        assertEquals(2, deskRollCell(roll, 'F', old = false))
    }

    @Test
    fun rosterFiltersByQueryAndSegment() {
        val roll = listOf(
            card(1, conf = "NF1", given = "Priya"),
            card(2, conf = "NF2", given = "Meera"),
        )
        val checkIns = mapOf(ApplicantId(1) to CheckInRecord(checkedIn = true, room = "F21"))

        assertEquals(listOf(2), deskRosterRows(roll, checkIns, "", "To arrive").map { it.id.value })
        assertEquals(listOf(1), deskRosterRows(roll, checkIns, "", "Arrived").map { it.id.value })
        assertEquals(2, deskRosterRows(roll, checkIns, "", "All").size)
        assertEquals(listOf(2), deskRosterRows(roll, checkIns, "meer", "All").map { it.id.value })
        assertEquals(listOf(2), deskRosterRows(roll, checkIns, "nf2", "All").map { it.id.value })
    }

    @Test
    fun serverAttendedSeedsARecordButLocalWins() {
        val seeded = card(1, attended = true)
        assertTrue(deskRecord(seeded, emptyMap())!!.checkedIn)
        val overridden = mapOf(ApplicantId(1) to CheckInRecord(checkedIn = false))
        assertFalse(deskRecord(seeded, overridden)!!.checkedIn)
        assertNull(deskRecord(card(2), emptyMap()))
    }

    @Test
    fun freeRoomsExcludeOccupiedAndOtherGender() {
        val rooms = listOf(
            AccoRoom("F21", Gender.F, "Fbk"),
            AccoRoom("F22", Gender.F, "Fbk"),
            AccoRoom("M11", Gender.M, "Mbk"),
        )
        val roll = listOf(card(1), card(2))
        val checkIns = mapOf(ApplicantId(1) to CheckInRecord(checkedIn = true, room = "F21"))
        val occupied = deskOccupied(roll, checkIns)
        assertEquals(setOf("F21"), occupied)
        assertEquals(listOf("F22"), deskFreeRooms(rooms, Gender.F, occupied).map { it.code })
        // The dialog ignores its own row's occupancy.
        assertEquals(
            listOf("F21", "F22"),
            deskFreeRooms(rooms, Gender.F, deskOccupied(roll, checkIns, except = ApplicantId(1))).map { it.code },
        )
    }

    @Test
    fun seatCountsOnlyCheckedInRecords() {
        val roll = listOf(card(1), card(2), card(3))
        val checkIns = mapOf(
            ApplicantId(1) to CheckInRecord(checkedIn = true, room = "F21", seat = "Chowky"),
            ApplicantId(2) to CheckInRecord(checkedIn = false, seat = "Chowky"),
        )
        assertEquals(1, deskSeatCount(roll, checkIns, "Chowky"))
        assertEquals(0, deskSeatCount(roll, checkIns, "Chair"))
    }

    @Test
    fun findingsGroupByRuleNotPersonWithSeverityAndBatch() {
        val flags1 = listOf(
            AuditFlag(AuditSeverity.HARD, "Honorific left in the name field", "name_title_prefix · 'Smt'", "name_title_prefix"),
            AuditFlag(AuditSeverity.SOFT, "Mobile shared with another applicant", "shared_mobile · 9876543210", "shared_mobile"),
        )
        val flags2 = listOf(
            AuditFlag(AuditSeverity.HARD, "Honorific left in the name field", "name_title_prefix · 'Shri'", "name_title_prefix"),
        )
        val findings = deskFindings(listOf(card(1, flags = flags1), card(2, flags = flags2)))
        // Hard section comes before Soft, regardless of who was flagged first.
        assertEquals(listOf("name_title_prefix", "shared_mobile"), findings.map { it.code })
        val honorifics = findings.first { it.code == "name_title_prefix" }
        assertEquals(2, honorifics.people.size)
        assertTrue(honorifics.mustFix)
        assertEquals(AuditSeverity.HARD, honorifics.severity)
        assertEquals("Strip 2 honorifics", honorifics.batchLabel)
        assertEquals("'Smt'", honorifics.people[0].offendingValue)
        val shared = findings.first { it.code == "shared_mobile" }
        assertFalse(shared.mustFix)
        assertEquals(AuditSeverity.SOFT, shared.severity)
        assertNull(shared.batchLabel)
    }

    @Test
    fun findingsOrderHardThenSafetyThenSoft() {
        val rows = listOf(
            card(1, flags = listOf(
                AuditFlag(AuditSeverity.SOFT, "Email shared across unrelated surnames", "shared_email_unrelated · x@y.z", "shared_email_unrelated"),
                AuditFlag(AuditSeverity.SAFETY, "Emergency contact is their own mobile", "emergency_eq_self · same number", "emergency_eq_self"),
            )),
            card(2, flags = listOf(
                AuditFlag(AuditSeverity.SAFETY, "Emergency contact is half-filled", "emergency_partial · name without a number", "emergency_partial"),
                AuditFlag(AuditSeverity.HARD, "No ID document on file", "id_missing · no ID", "id_missing"),
                AuditFlag(AuditSeverity.HARD, "Mobile number has fewer than 10 digits", "phone_short · 98220417", "phone_short"),
            )),
        )
        val findings = deskFindings(rows)
        assertEquals(
            listOf("phone_short", "id_missing", "emergency_eq_self", "emergency_partial", "shared_email_unrelated"),
            findings.map { it.code },
        )
        assertEquals(
            listOf(AuditSeverity.HARD, AuditSeverity.HARD, AuditSeverity.SAFETY, AuditSeverity.SAFETY, AuditSeverity.SOFT),
            findings.map { it.severity },
        )
    }

    @Test
    fun stripHonorificOnlyTouchesTheTitle() {
        assertEquals("Lakshmi", stripHonorific("Smt Lakshmi"))
        assertEquals("Lakshmi", stripHonorific("Smt. Lakshmi"))
        assertEquals("Lakshmi", stripHonorific("Lakshmi"))
        assertEquals("Mrinal", stripHonorific("Mrinal"))
    }

    @Test
    fun callRowsEmptyThemselvesAsOutcomesAreLogged() {
        val roll = listOf(card(1), card(2), card(3, mobile = null))
        assertEquals(2, deskCallList(roll).size)
        val outcomes = mapOf(ApplicantId(1) to CallRecord(outcome = "Confirmed"))
        assertEquals(listOf(2), deskCallRows(roll, outcomes, "To call").map { it.id.value })
        assertEquals(listOf(1), deskCallRows(roll, outcomes, "Confirmed").map { it.id.value })
        assertTrue(deskCallRows(roll, outcomes, "No answer").isEmpty())
        assertEquals(
            listOf("Confirmed", "Cancelled", "No answer", "Callback", "Left msg"),
            CALL_OUTCOMES,
        )
        // An attempt without an outcome keeps the row in the To-call pile.
        val attempted = mapOf(ApplicantId(1) to CallRecord(attempts = 1, lastAttemptMs = 5L))
        assertEquals(listOf(1, 2), deskCallRows(roll, attempted, "To call").map { it.id.value })
        // Pile sizes for the segmented labels.
        assertEquals(
            mapOf(
                "To call" to 1,
                "Confirmed" to 1,
                "Cancelled" to 0,
                "No answer" to 0,
                "Callback" to 0,
                "Left msg" to 0,
            ),
            deskCallCounts(roll, outcomes),
        )
    }

    @Test
    fun logsWrittenBeforeTheTrackerWordingStillLandInAPile() {
        // "Reached" and "Call back" were the old labels; a value belonging to
        // no vocabulary at all falls back to To-call rather than disappearing.
        assertEquals("Confirmed", deskCallOutcome("Reached"))
        assertEquals("Callback", deskCallOutcome("Call back"))
        assertEquals("Left msg", deskCallOutcome("Left message"))
        assertEquals("No answer", deskCallOutcome("no answer"))
        assertEquals("", deskCallOutcome("Tentative"))
        assertEquals("", deskCallOutcome(null))
        assertTrue(deskCallLogged(CallRecord(outcome = "Reached")))
        assertFalse(deskCallLogged(CallRecord(attempts = 3)))

        val roll = listOf(card(1), card(2))
        val legacy = mapOf(ApplicantId(1) to CallRecord(outcome = "Reached"))
        assertEquals(listOf(1), deskCallRows(roll, legacy, "Confirmed").map { it.id.value })
        assertEquals(listOf(2), deskCallRows(roll, legacy, "To call").map { it.id.value })
    }

    @Test
    fun callSearchMatchesNameOrConfNumberAndLeavesTheCountsAlone() {
        val roll = listOf(
            card(1, conf = "NM66", given = "Rajat", family = "Kumar"),
            card(2, conf = "OM9", given = "Harendra", family = "Singh"),
        )
        assertEquals(listOf(1), deskCallRows(roll, emptyMap(), "To call", "rajat").map { it.id.value })
        assertEquals(listOf(2), deskCallRows(roll, emptyMap(), "To call", "om9").map { it.id.value })
        assertEquals(listOf(1, 2), deskCallRows(roll, emptyMap(), "To call", "  ").map { it.id.value })
        assertTrue(deskCallRows(roll, emptyMap(), "To call", "zzz").isEmpty())
        // Segment labels keep reading as pile sizes, not as search hits.
        assertEquals(2, deskCallCounts(roll, emptyMap())["To call"])
    }

    @Test
    fun prioritySortFloatsTheStillToReachRowsUp() {
        val roll = listOf(
            card(1, given = "Arun", family = "Kale"),
            card(2, given = "Bikram", family = "Poonia"),
            card(3, given = "Chandan", family = "Rao"),
            card(4, given = "Deepak", family = "Sen"),
        )
        val outcomes = mapOf(
            ApplicantId(1) to CallRecord(outcome = "Confirmed"),
            ApplicantId(2) to CallRecord(outcome = "Callback"),
            ApplicantId(3) to CallRecord(outcome = "Cancelled"),
        )
        assertEquals(0, deskCallRank(null))
        assertEquals(1, deskCallRank(CallRecord(outcome = "Callback")))
        assertEquals(5, deskCallRank(CallRecord(outcome = "Cancelled")))
        // A-Z ignores the log; priority puts the un-called row first, then the
        // callback, and leaves the settled pair at the back.
        assertEquals(listOf(1, 2, 3, 4), deskCallSorted(roll, outcomes, false).map { it.id.value })
        assertEquals(listOf(4, 2, 1, 3), deskCallSorted(roll, outcomes, true).map { it.id.value })
    }

    @Test
    fun callMetaShowsAttemptsAndTimeSinceLastAttempt() {
        val now = 10 * 60_000L
        assertNull(deskCallMeta(null, now))
        assertNull(deskCallMeta(CallRecord(), now))
        assertEquals("×2 · just now", deskCallMeta(CallRecord(attempts = 2, lastAttemptMs = now - 30_000L), now))
        assertEquals("×1 · 5m ago", deskCallMeta(CallRecord(attempts = 1, lastAttemptMs = now - 5 * 60_000L), now))
        assertEquals("3h ago", deskCallAgo(0L, 3 * 60 * 60_000L))
        assertEquals("2d ago", deskCallAgo(0L, 48 * 60 * 60_000L))
    }

    @Test
    fun waNumberPrefixesBareIndianMobilesOnly() {
        assertEquals("919876543210", deskWaNumber("9876543210"))
        assertEquals("919876543210", deskWaNumber("+91 98765 43210"))
        assertEquals("02212345678", deskWaNumber("022-1234-5678"))
        // 10 digits but not an Indian mobile prefix — passed through untouched.
        assertEquals("5876543210", deskWaNumber("5876543210"))
        assertNull(deskWaNumber(null))
        assertNull(deskWaNumber("NA"))
    }

    @Test
    fun saveIsBlockedWithoutARoomAndConfirmsWithOne() {
        val priya = card(1, given = "Priya", family = "Nair")
        val (blockedText, blocked) = deskSaveSnack(CheckInRecord(), priya)
        assertTrue(blocked)
        assertEquals("Choose a room before checking Priya in", blockedText)

        val (okText, err) = deskSaveSnack(CheckInRecord(room = "F21", seat = "Chowky"), priya)
        assertFalse(err)
        assertEquals("✓ Priya Nair checked in · F21 · Chowky", okText)
    }
}
