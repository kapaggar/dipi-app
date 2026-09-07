package org.dhamma.dipi.staff

import org.dhamma.dipi.staff.desk.deskRecord
import org.dhamma.dipi.staff.model.*
import org.dhamma.dipi.staff.network.SearchPageParser
import org.junit.Assert.*
import org.junit.Test

class FinalizedRoomsTest {
    private fun card(status: String, section: String = "Mbk", room: String = "12") =
        SearchPageParser.parse("""<script>var dataset = [{"aid":31,"centreid":63,"courseid":77,"name":"Test Student","gender":"M","confno":"OM1","app_status":"$status","finalized":1,"section":"$section","acc":"$room","age":61,"o_n":"Old"}];</script>""").dataset.single().toModel()

    @Test fun finalizedAttendedUsesHistoricalRoomOverUnsyncedLocalEdit() {
        val student = card("Attended")
        val record = deskRecord(student, mapOf(student.id to CheckInRecord(room = "Wrong 9", checkedIn = true)))
        assertEquals("Mbk 12", record?.room)
        assertTrue(record?.checkedIn == true)
        assertTrue(record?.synced == true)
    }
    @Test fun leftStudentNeverOccupiesHistoricalOrStaleLocalRoom() {
        val student = card("Left")
        val record = deskRecord(student, mapOf(student.id to CheckInRecord(room = "Mbk 12", checkedIn = true)))
        assertFalse(record?.checkedIn == true)
        assertTrue(record?.room.isNullOrBlank())
    }
    @Test fun missingRoomIsNotInvented() {
        assertEquals("", deskRecord(card("Attended", "NA", "NA"), emptyMap())?.room)
    }
}
