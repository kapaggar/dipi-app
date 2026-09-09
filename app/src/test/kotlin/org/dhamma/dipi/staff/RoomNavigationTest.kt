package org.dhamma.dipi.staff

import org.dhamma.dipi.staff.desk.nextOccupiedRoomCode
import org.dhamma.dipi.staff.desk.resolveRoomJump
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomNavigationTest {
    private val rooms = listOf(
        AccoRoom("Mbk 2", Gender.M, "Mbk", number = "2"),
        AccoRoom("Mbk 10", Gender.M, "Mbk", number = "10"),
        AccoRoom("Mbk A", Gender.M, "Mbk", number = "A"),
    )

    @Test
    fun nextOccupiedFollowsPhysicalOrderAndWraps() {
        val codes = rooms.map { it.code }
        val occupied = setOf("Mbk 2", "Mbk A")
        assertEquals("Mbk A", nextOccupiedRoomCode(codes, occupied, "Mbk 2"))
        assertEquals("Mbk 2", nextOccupiedRoomCode(codes, occupied, "Mbk A"))
        assertEquals("Mbk A", nextOccupiedRoomCode(codes, occupied, "Mbk 10"))
        assertNull(nextOccupiedRoomCode(codes, emptySet(), "Mbk 2"))
    }

    @Test
    fun jumpResolvesExactCodeOrUnambiguousDisplay() {
        assertEquals("Mbk 10", resolveRoomJump(rooms, "mbk 10")?.code)
        assertEquals("Mbk A", resolveRoomJump(rooms, "A")?.code)
        assertNull(resolveRoomJump(rooms, "missing"))
        val dup = rooms + AccoRoom("Guest A", Gender.M, "Guest", number = "A")
        assertNull(resolveRoomJump(dup, "A"))
    }
}
