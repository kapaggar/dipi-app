package org.dhamma.dipi.staff.model

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomAllocSyncTest {

    private val checkedIn = CheckInRecord(checkedIn = true, room = "Mbk 12", seat = "Chowky", group = "2")

    /* ── params: exactly the desk dialog's fields ───────────────────── */

    @Test
    fun paramsMirrorTheDialogExactly() {
        val p = RoomAllocSync.params(checkedIn)
        assertEquals(
            listOf("s", "r", "g", "l", "v", "c", "cf", "chow", "chai", "back", "comment", "a"),
            p.keys.toList(),
        )
        assertEquals("Mbk", p["s"])
        assertEquals("12", p["r"])
        assertEquals("2", p["g"])
        assertEquals("true", p["chow"])
        assertEquals("false", p["chai"])
        assertEquals("false", p["back"])
        assertEquals("true", p["a"])
        // Untracked desk fields post empty — never fabricated token numbers.
        assertEquals("", p["l"])
        assertEquals("", p["v"])
        assertEquals("", p["c"])
        assertEquals("false", p["cf"])
        assertEquals("", p["comment"])
    }

    @Test
    fun paramsNeverCarryAStatus() {
        val p = RoomAllocSync.params(checkedIn)
        assertFalse(p.keys.any { it.equals("status", true) })
        assertFalse(p.values.any { it.contains("Approved", ignoreCase = true) })
    }

    @Test
    fun seatMapsToTheThreeRadioBooleans() {
        assertEquals("true", RoomAllocSync.params(checkedIn.copy(seat = "Chair"))["chai"])
        assertEquals("true", RoomAllocSync.params(checkedIn.copy(seat = "Backrest"))["back"])
        val none = RoomAllocSync.params(checkedIn.copy(seat = "None"))
        assertEquals("false", none["chow"])
        assertEquals("false", none["chai"])
        assertEquals("false", none["back"])
    }

    @Test
    fun roomSplitsOnTheLastSpace() {
        assertEquals("Mbk" to "12", RoomAllocSync.splitRoom("Mbk 12"))
        assertEquals("A Block" to "7", RoomAllocSync.splitRoom("A Block 7"))
        assertEquals("" to "12", RoomAllocSync.splitRoom("12"))
    }

    /* ── pending: the queue ─────────────────────────────────────────── */

    @Test
    fun pendingIsCheckedInUnsyncedWithARoom() {
        val records = mapOf(
            ApplicantId(1) to checkedIn,
            ApplicantId(2) to checkedIn.copy(synced = true, syncedAt = "2026-08-16T09:00:00Z"),
            ApplicantId(3) to checkedIn.copy(checkedIn = false),
            ApplicantId(4) to checkedIn.copy(room = ""),
        )
        assertEquals(setOf(ApplicantId(1)), RoomAllocSync.pending(records).keys)
    }

    /* ── walk: marks synced, collects failures, stops on 403/offline ── */

    @Test
    fun walkMarksSuccessesAndCollectsFailures() = runTest {
        val pending = linkedMapOf(
            ApplicantId(1) to checkedIn,
            ApplicantId(2) to checkedIn.copy(room = "Mbk 8"),
            ApplicantId(3) to checkedIn.copy(room = "Mbk 9"),
        )
        val marked = mutableListOf<ApplicantId>()
        val result = RoomAllocSync.walk(
            pending = pending,
            post = { id, _ ->
                if (id.value == 2) RoomPostOutcome.Rejected("Room has already been alloted") else RoomPostOutcome.Ok
            },
            markSynced = { id, _ -> marked += id },
        )
        assertEquals(3, result.attempted)
        assertEquals(2, result.synced)
        assertEquals(listOf(ApplicantId(1), ApplicantId(3)), marked)
        assertEquals(1, result.failed)
        assertEquals(ApplicantId(2), result.failures.single().id)
        assertEquals("Room has already been alloted", result.failures.single().reason)
        assertFalse(result.authExpired)
        assertFalse(result.offline)
    }

    @Test
    fun walkStopsDeadOnAuthExpiry() = runTest {
        val pending = linkedMapOf(
            ApplicantId(1) to checkedIn,
            ApplicantId(2) to checkedIn.copy(room = "Mbk 8"),
            ApplicantId(3) to checkedIn.copy(room = "Mbk 9"),
        )
        val posted = mutableListOf<ApplicantId>()
        val result = RoomAllocSync.walk(
            pending = pending,
            post = { id, _ ->
                posted += id
                if (id.value == 2) RoomPostOutcome.AuthExpired else RoomPostOutcome.Ok
            },
            markSynced = { _, _ -> },
        )
        // Applicant 3 must never be attempted after the 403.
        assertEquals(listOf(ApplicantId(1), ApplicantId(2)), posted)
        assertTrue(result.authExpired)
        assertEquals(1, result.synced)
        assertEquals(2, result.attempted)
    }

    @Test
    fun walkStopsOnConnectivityLossAndKeepsPartialProgress() = runTest {
        val pending = linkedMapOf(
            ApplicantId(1) to checkedIn,
            ApplicantId(2) to checkedIn.copy(room = "Mbk 8"),
            ApplicantId(3) to checkedIn.copy(room = "Mbk 9"),
        )
        val marked = mutableListOf<ApplicantId>()
        val result = RoomAllocSync.walk(
            pending = pending,
            post = { id, _ -> if (id.value == 2) RoomPostOutcome.Offline else RoomPostOutcome.Ok },
            markSynced = { id, _ -> marked += id },
        )
        assertTrue(result.offline)
        assertFalse(result.authExpired)
        assertEquals(listOf(ApplicantId(1)), marked)
        assertEquals(1, result.synced)
        assertEquals(2, result.attempted)
    }

    @Test
    fun walkOfNothingIsEmpty() = runTest {
        val result = RoomAllocSync.walk(emptyMap(), { _, _ -> RoomPostOutcome.Ok }, { _, _ -> })
        assertEquals(RoomSyncResult(), result)
    }
}
