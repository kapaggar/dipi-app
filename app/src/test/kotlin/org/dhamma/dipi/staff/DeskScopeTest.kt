package org.dhamma.dipi.staff

import org.dhamma.dipi.staff.desk.RoomBlockKey
import org.dhamma.dipi.staff.desk.deskArrivalCounts
import org.dhamma.dipi.staff.desk.deskCallCounts
import org.dhamma.dipi.staff.desk.deskCallHeldOutCount
import org.dhamma.dipi.staff.desk.deskCallRound
import org.dhamma.dipi.staff.desk.deskCallRows
import org.dhamma.dipi.staff.desk.deskIsLeft
import org.dhamma.dipi.staff.desk.deskOccupied
import org.dhamma.dipi.staff.desk.deskRoomAvailability
import org.dhamma.dipi.staff.desk.deskRosterRows
import org.dhamma.dipi.staff.desk.deskScoped
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.CallRecord
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.model.ConfNo
import org.dhamma.dipi.staff.model.ConfSeniority
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeskScopeTest {

    private fun card(
        id: Int,
        conf: String,
        status: String = "Confirmed",
        gender: Gender = Gender.M,
        type: ApplicantType = ApplicantType.Student,
        mobile: String? = "90000000${id.toString().padStart(2, '0')}",
        attended: Boolean = false,
        finalized: Boolean = false,
        historicalRoom: String = "",
    ) = ApplicantCard(
        id = ApplicantId(id),
        centreId = CentreId(1),
        courseId = CourseId(10),
        givenName = "Synthetic",
        familyName = "Applicant ${id.toString().padStart(2, '0')}",
        gender = gender,
        status = ApplicantStatus(status),
        type = type,
        oldStudent = conf.startsWith("O"),
        attended = attended,
        courseFinalized = finalized,
        historicalRoom = historicalRoom,
        confNo = ConfNo(conf),
        mobile = mobile,
    )

    private val inventory = listOf(
        AccoRoom("Mbk 1", Gender.M, "Mbk", number = "1"),
        AccoRoom("Mbk 2", Gender.M, "Mbk", number = "2"),
        AccoRoom("Mbk 3", Gender.M, "Mbk", number = "3"),
        AccoRoom("Mbk 4", Gender.M, "Mbk", number = "4"),
        AccoRoom("Guest 1", Gender.M, "Guest", number = "1"),
        AccoRoom("Guest 2", Gender.M, "Guest", number = "2"),
        AccoRoom("Fbk 1", Gender.F, "Fbk", number = "1"),
        AccoRoom("Fbk 2", Gender.F, "Fbk", number = "2"),
    )

    private fun roomFixture(): Pair<List<ApplicantCard>, Map<ApplicantId, CheckInRecord>> {
        val rows = listOf(
            card(1, "OM1"),
            card(2, "NM2"),
            card(3, "OM3", status = "Expected"),
            card(4, "OM4", status = "Left"),
            card(5, "OM5"),
            card(6, "NF6", gender = Gender.F),
        )
        val records = mapOf(
            ApplicantId(1) to CheckInRecord(checkedIn = true, room = "Mbk 1"),
            ApplicantId(2) to CheckInRecord(checkedIn = true, room = "Mbk 2"),
            ApplicantId(3) to CheckInRecord(checkedIn = false, room = "Mbk 3"),
            ApplicantId(4) to CheckInRecord(checkedIn = true, room = "Mbk 4"),
            ApplicantId(5) to CheckInRecord(checkedIn = true, room = "Guest 1"),
            ApplicantId(6) to CheckInRecord(checkedIn = true, room = "Fbk 1"),
        )
        return rows to records
    }

    @Test
    fun roomAvailabilityUsesFullCourseOccupancy() {
        val (rows, records) = roomFixture()
        val mbk = deskRoomAvailability(rows, records, inventory, RoomBlockKey(Gender.M, "Mbk"))
        assertEquals(4, mbk.total)
        assertEquals(2, mbk.occupied)
        assertEquals(2, mbk.free)
        val guest = deskRoomAvailability(rows, records, inventory, RoomBlockKey(Gender.M, "Guest"))
        assertEquals(2, guest.total)
        assertEquals(1, guest.occupied)
        val fbk = deskRoomAvailability(rows, records, inventory, RoomBlockKey(Gender.F, "Fbk"))
        assertEquals(1, fbk.occupied)
        val oldOnly = deskScoped(rows, Gender.M, ConfSeniority.OLD)
        val still = deskRoomAvailability(rows, records, inventory, RoomBlockKey(Gender.M, "Mbk"))
        assertEquals(2, still.free)
        assertTrue(oldOnly.size < rows.size)
        val searched = deskRosterRows(oldOnly, records, "Applicant 01", "All")
        assertEquals(1, searched.size)
        assertEquals(2, deskRoomAvailability(rows, records, inventory, RoomBlockKey(Gender.M, "Mbk")).free)
    }

    @Test
    fun sharedCodeCountsOnceAndUnknownCodeDoesNotSteal() {
        val (rows, records) = roomFixture()
        val shared = records + (ApplicantId(2) to CheckInRecord(checkedIn = true, room = "Mbk 1"))
        val mbk = deskRoomAvailability(rows, shared, inventory, RoomBlockKey(Gender.M, "Mbk"))
        assertEquals(1, mbk.occupied)
        assertEquals(3, mbk.free)
        val unknown = records + (ApplicantId(5) to CheckInRecord(checkedIn = true, room = "Missing 99"))
        assertEquals(2, deskRoomAvailability(rows, unknown, inventory, RoomBlockKey(Gender.M, "Mbk")).occupied)
        assertFalse(deskOccupied(rows, unknown).contains("Mbk 5"))
    }

    @Test
    fun leftAndPendingDoNotOccupy() {
        val (rows, records) = roomFixture()
        assertTrue(deskIsLeft(rows[3]))
        assertFalse(deskOccupied(rows, records).contains("Mbk 3"))
        assertFalse(deskOccupied(rows, records).contains("Mbk 4"))
        val finalized = listOf(
            card(7, "OM7", status = "Attended", finalized = true, historicalRoom = "Mbk 1", attended = true),
            card(8, "OM8", status = "Left", finalized = true, historicalRoom = "Mbk 2"),
        )
        val hist = deskRoomAvailability(finalized, emptyMap(), inventory, RoomBlockKey(Gender.M, "Mbk"))
        assertEquals(1, hist.occupied)
        assertEquals(3, hist.free)
    }

    @Test
    fun arrivalCountsExcludeLeftFromEligible() {
        val rows = (1..15).map { id ->
            card(id, "OM$id", status = if (id == 15) "Left" else "Confirmed")
        }
        val records = (1..13).associate { ApplicantId(it) to CheckInRecord(checkedIn = true, room = "Mbk 1") }
        val counts = deskArrivalCounts(rows, records)
        assertEquals(15, counts.roll)
        assertEquals(13, counts.arrived)
        assertEquals(1, counts.pending)
        assertEquals(1, counts.left)
        assertEquals(14, counts.eligible)
        assertEquals(listOf(14), deskRosterRows(rows, records, "", "To arrive").map { it.id.value })
        assertEquals(listOf(15), deskRosterRows(rows, records, "", "Left").map { it.id.value })
        assertEquals(15, deskRosterRows(rows, records, "", "All").size)
        assertEquals(13, deskRosterRows(rows, records, "", "Arrived").size)
    }

    @Test
    fun callRoundHoldsOutCallableLeft() {
        val rows = (1..15).map { id ->
            card(id, "OM$id", status = if (id == 15) "Left" else "Confirmed")
        }
        assertEquals(14, deskCallRound(rows).size)
        assertEquals(15, deskCallRows(rows, emptyMap(), "All").size)
        assertEquals(14, deskCallRows(rows, emptyMap(), "To call").size)
        assertEquals(1, deskCallHeldOutCount(rows))
        val logged = mapOf(ApplicantId(1) to CallRecord(outcome = "Confirmed"))
        val counts = deskCallCounts(rows, logged)
        assertEquals(13, counts["To call"])
        assertEquals(15, counts["All"])
        assertEquals(1, counts["Confirmed"])
        val noPhone = rows.map { if (it.id.value == 15) it.copy(mobile = null) else it }
        assertEquals(14, deskCallRows(noPhone, emptyMap(), "All").size)
        assertEquals(0, deskCallHeldOutCount(noPhone))
    }
}
