package org.dhamma.dipi.staff

import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.model.RoomSyncFailure
import org.dhamma.dipi.staff.model.RoomSyncResult
import org.dhamma.dipi.staff.ui.deskRoomSyncPending
import org.dhamma.dipi.staff.ui.roomPullSnack
import org.dhamma.dipi.staff.ui.roomSyncSnack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The VM's room-sync bindings: pending count + snack wording. */
class RoomSyncTest {

    @Test
    fun pendingCountsOnlyUnsyncedCheckedInRecordsWithARoom() {
        val checkIns = mapOf(
            ApplicantId(1) to CheckInRecord(checkedIn = true, room = "Mbk 12"),
            ApplicantId(2) to CheckInRecord(checkedIn = true, room = "Mbk 8", synced = true, syncedAt = "t"),
            ApplicantId(3) to CheckInRecord(checkedIn = false),
            ApplicantId(4) to CheckInRecord(checkedIn = true, room = ""),
        )
        assertEquals(1, deskRoomSyncPending(checkIns))
    }

    @Test
    fun allGoodSnackReportsTheCount() {
        val snack = roomSyncSnack(RoomSyncResult(attempted = 3, synced = 3))
        assertFalse(snack.error)
        assertEquals("✓ Synced 3 room allocation(s) to the desk", snack.text)
    }

    @Test
    fun failureSnackLeadsWithCountsThenTheFirstServerReason() {
        val snack = roomSyncSnack(
            RoomSyncResult(
                attempted = 3,
                synced = 2,
                failures = listOf(RoomSyncFailure(ApplicantId(7), "Room has already been alloted")),
            ),
        )
        assertTrue(snack.error)
        assertEquals("2 synced · 1 failed — Room has already been alloted", snack.text)
    }

    @Test
    fun offlineStopSnackPromisesARetry() {
        val snack = roomSyncSnack(RoomSyncResult(attempted = 2, synced = 1, offline = true))
        assertTrue(snack.error)
        assertEquals("1 synced · connection lost — the rest will sync when online", snack.text)
    }

    @Test
    fun pullSnackReportsTheCount() {
        val snack = roomPullSnack(2)
        assertFalse(snack.error)
        assertEquals("✓ Pulled 2 room assignment(s) from the desk", snack.text)
    }

    @Test
    fun pullSnackWhenNoneAssigned() {
        val snack = roomPullSnack(0)
        assertFalse(snack.error)
        assertEquals("No rooms assigned on the desk yet", snack.text)
    }
}
