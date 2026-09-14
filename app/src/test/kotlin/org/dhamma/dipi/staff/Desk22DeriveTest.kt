package org.dhamma.dipi.staff

import org.dhamma.dipi.staff.desk.DeskSection
import org.dhamma.dipi.staff.desk.deskCallingRoll
import org.dhamma.dipi.staff.desk.deskHeld
import org.dhamma.dipi.staff.desk.deskOccupied
import org.dhamma.dipi.staff.desk.deskReconcile
import org.dhamma.dipi.staff.desk.deskRoll
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.AuditFlag
import org.dhamma.dipi.staff.model.AuditSeverity
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.CentreOpsPrefs
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.model.ConfNo
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.ui.DeskUiState
import org.dhamma.dipi.staff.ui.deskRailCounts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class Desk22DeriveTest {

    private fun card(
        id: Int,
        status: String = "Confirmed",
        conf: String? = "NF$id",
        mobile: String? = "9876543210",
        finalized: Boolean = false,
        attended: Boolean = false,
        historicalRoom: String = "",
        flags: List<AuditFlag> = emptyList(),
    ) = ApplicantCard(
        id = ApplicantId(id),
        centreId = CentreId(1),
        courseId = CourseId(1),
        givenName = "Person$id",
        familyName = "Test",
        gender = Gender.F,
        status = ApplicantStatus(status),
        type = ApplicantType.Student,
        oldStudent = false,
        attended = attended,
        courseFinalized = finalized,
        historicalRoom = historicalRoom,
        confNo = conf?.let(::ConfNo),
        mobile = mobile,
        flags = flags,
    )

    @Test
    fun rollRetainsActiveConfirmationHoldersAndSeparatesWaitListFromFiveExclusions() {
        val rows = listOf(
            card(1, status = "Expected"),
            card(2, status = "Confirmed"),
            card(3, status = "WaitList"),
            card(4, status = "  cAnCeLlEd "),
            card(5, status = "LEFT"),
            card(6, status = " rejected"),
            card(7, status = "Regret "),
            card(8, status = "DuPlicate"),
            card(9, status = "Confirmed", conf = null),
        )

        val roll = deskRoll(rows)

        assertEquals(listOf(1, 2, 3), roll.map { it.id.value })
        assertEquals(listOf(3), roll.filter(::deskHeld).map { it.id.value })
    }

    @Test
    fun reconcileIsFinalizedOrExactlyTheParsedCourseEndDate() {
        val name = "Dhamma Sudha / 10 Day / 2026 / 2nd-Sep to 13th-Sep"

        assertFalse(deskReconcile(name, finalized = false, today = LocalDate.of(2026, 9, 12)))
        assertTrue(deskReconcile(name, finalized = false, today = LocalDate.of(2026, 9, 13)))
        assertFalse(deskReconcile(name, finalized = false, today = LocalDate.of(2026, 9, 14)))
        assertFalse(deskReconcile("unparseable", finalized = false, today = LocalDate.of(2026, 9, 13)))
        assertTrue(deskReconcile("unparseable", finalized = true, today = LocalDate.of(2026, 9, 13)))
    }

    @Test
    fun reconcileCallingKeepsUnarrivedLoggedAndPhonelessPeopleButRemovesEffectiveCheckIns() {
        val roll = listOf(card(1), card(2), card(3, mobile = null))
        val records = mapOf(ApplicantId(1) to CheckInRecord(checkedIn = true, room = "F01"))

        assertEquals(listOf(2, 3), deskCallingRoll(roll, records, reconcile = true).map { it.id.value })
        assertEquals(listOf(1, 2, 3), deskCallingRoll(roll, records, reconcile = false).map { it.id.value })
    }

    @Test
    fun finalizedAttendedHistoricalAllocationOccupiesItsRoom() {
        val finalizedAttended = card(
            1,
            status = "Attended",
            finalized = true,
            attended = false,
            historicalRoom = "F01",
        )

        assertEquals(setOf("F01"), deskOccupied(deskRoll(listOf(finalizedAttended)), emptyMap()))
    }

    @Test
    fun railCountsStayUnscopedAndUseRemainingReconcilePopulationIncludingMissingPhone() {
        val lastDay = LocalDate.of(2026, 9, 13)
        val course = Course(
            id = CourseId(1), centreId = CentreId(1),
            name = "Dhamma Sudha / 10 Day / 2026 / 2nd-Sep to 13th-Sep",
            start = "2 Sep 2026", end = "13 Sep 2026",
        )
        val rows = listOf(
            card(1),
            card(2, mobile = null),
            card(3, status = "Cancelled"),
            card(4, status = "WaitList"),
        )
        val hard = AuditFlag(AuditSeverity.HARD, "hard", "detail", "hard")
        val soft = AuditFlag(AuditSeverity.SOFT, "soft", "detail", "soft")
        val state = DeskUiState(
            course = course,
            rows = rows,
            counts = mapOf("All" to 12),
            auditRows = listOf(card(5, flags = listOf(hard, soft))),
            checkIns = mapOf(ApplicantId(1) to CheckInRecord(checkedIn = true, room = "F01")),
            centreOps = CentreOpsPrefs(rooms = listOf(
                AccoRoom("F01", Gender.F, "F"),
                AccoRoom("F02", Gender.F, "F"),
            )),
            deskGender = "Male",
            deskSeniority = "Old",
        )

        val counts = deskRailCounts(state, lastDay)

        assertEquals(12, counts[DeskSection.Applications])
        assertEquals(2, counts[DeskSection.Audit])
        assertEquals(2, counts[DeskSection.Calling])
        assertEquals(2, counts[DeskSection.CheckIn])
        assertEquals(1, counts[DeskSection.Rooms])
    }
}
