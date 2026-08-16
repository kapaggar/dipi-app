package org.dhamma.dipi.staff

import org.dhamma.dipi.staff.desk.DeskSection
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.AuditFlag
import org.dhamma.dipi.staff.model.AuditSeverity
import org.dhamma.dipi.staff.model.CallRecord
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.CentreOpsPrefs
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.model.ConfNo
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.ui.DeskUiState
import org.dhamma.dipi.staff.ui.deskDayChip
import org.dhamma.dipi.staff.ui.deskRailCounts
import org.dhamma.dipi.staff.ui.deskSyncLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DeskDerivedTest {

    private fun card(id: Int, attended: Boolean = false, flags: Int = 0) = ApplicantCard(
        id = ApplicantId(id),
        centreId = CentreId(1),
        courseId = CourseId(10),
        givenName = "A$id",
        familyName = "B",
        gender = Gender.F,
        status = ApplicantStatus("Confirmed"),
        type = ApplicantType.Student,
        oldStudent = false,
        attended = attended,
        confNo = ConfNo("NF$id"),
        mobile = "987654321$id",
        flags = List(flags) { AuditFlag(AuditSeverity.HARD, "f$it", "d", "rule") },
    )

    @Test
    fun railCountsDeriveFromWorklistAndLocalState() {
        val rows = listOf(card(1, attended = true), card(2), card(3))
        val state = DeskUiState(
            rows = rows,
            counts = mapOf("All" to 214),
            auditRows = listOf(card(4, flags = 2), card(5, flags = 1)),
            callState = mapOf(ApplicantId(1) to CallRecord(outcome = "Reached")),
            checkIns = mapOf(ApplicantId(1) to CheckInRecord(checkedIn = true, room = "F21")),
            centreOps = CentreOpsPrefs(
                rooms = listOf(
                    AccoRoom("F21", Gender.F, "Fbk"),
                    AccoRoom("F23", Gender.F, "Fbk"),
                    AccoRoom("M04", Gender.M, "B"),
                ),
            ),
        )
        val counts = deskRailCounts(state)
        assertEquals(214, counts[DeskSection.Applications])
        assertEquals(3, counts[DeskSection.Audit])
        assertEquals(2, counts[DeskSection.Calling])
        assertEquals(2, counts[DeskSection.CheckIn])
        assertEquals(2, counts[DeskSection.Rooms])
        assertNull(counts[DeskSection.Board])
    }

    @Test
    fun dayChipReadsTheCourseStartDate() {
        val today = LocalDate.of(2026, 9, 2)
        assertEquals("DAY 0 · TODAY", deskDayChip("2026-09-02", today))
        assertEquals("STARTS IN 7 DAYS", deskDayChip("2026-09-09", today))
        assertEquals("DAY 2", deskDayChip("2026-08-31", today))
        assertEquals("DAY 0 · TODAY", deskDayChip("2 Sep 2026", today))
        assertNull(deskDayChip("", today))
        assertNull(deskDayChip("not a date", today))
    }

    @Test
    fun syncLineIsATruthClaim() {
        val now = Instant.parse("2026-09-02T09:41:00Z")
        assertEquals("synced 2 min ago", deskSyncLine("2026-09-02T09:39:00Z", now, offline = false, queued = 0))
        assertEquals("synced just now", deskSyncLine("2026-09-02T09:40:30Z", now, offline = false, queued = 0))
        assertEquals("offline · 3 queued", deskSyncLine("2026-09-02T09:39:00Z", now, offline = true, queued = 3))
        assertEquals("2 queued to sync", deskSyncLine("2026-09-02T09:39:00Z", now, offline = false, queued = 2))
        assertEquals("not synced yet", deskSyncLine(null, now, offline = false, queued = 0))
    }
}
