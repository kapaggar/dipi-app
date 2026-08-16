package org.dhamma.dipi.staff.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckInSyncTest {

    /** Same configuration SessionStore's opsJson uses. */
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun oldPersistedPayloadWithoutSyncFieldsStillDecodes() {
        // A pre-amendment SessionStore payload — no synced/syncedAt keys.
        val old = """{"5":{"checkedIn":true,"room":"Mbk 12","seat":"Chowky","valuables":true,"laundry":false,"group":"1"}}"""
        val decoded = json.decodeFromString<Map<Int, CheckInRecord>>(old)
        val rec = decoded.getValue(5)
        assertTrue(rec.checkedIn)
        assertEquals("Mbk 12", rec.room)
        assertFalse(rec.synced)
        assertNull(rec.syncedAt)
    }

    @Test
    fun syncFlagsRoundTrip() {
        val records = mapOf(
            5 to CheckInRecord(checkedIn = true, room = "Mbk 12", synced = true, syncedAt = "2026-08-16T09:00:00Z"),
            6 to CheckInRecord(checkedIn = true, room = "Fbk 2"),
        )
        val decoded = json.decodeFromString<Map<Int, CheckInRecord>>(json.encodeToString(records))
        assertEquals(records, decoded)
        assertTrue(decoded.getValue(5).synced)
        assertEquals("2026-08-16T09:00:00Z", decoded.getValue(5).syncedAt)
        assertFalse(decoded.getValue(6).synced)
    }

    @Test
    fun editsClearTheSyncedFlag() {
        val synced = CheckInRecord(checkedIn = true, room = "Mbk 12", synced = true, syncedAt = "t")
        val moved = synced.copy(room = "Mbk 13").clearSyncedIfChanged(synced)
        assertFalse(moved.synced)
        assertNull(moved.syncedAt)
        val reseated = synced.copy(seat = "Chair").clearSyncedIfChanged(synced)
        assertFalse(reseated.synced)
        val undone = synced.copy(checkedIn = false, room = "").clearSyncedIfChanged(synced)
        assertFalse(undone.synced)
    }

    @Test
    fun noOpEditKeepsTheSyncedFlag() {
        val synced = CheckInRecord(checkedIn = true, room = "Mbk 12", synced = true, syncedAt = "t")
        // Re-saving the dialog without changes must not re-queue the record.
        val resaved = synced.copy(checkedIn = true).clearSyncedIfChanged(synced)
        assertTrue(resaved.synced)
        assertEquals("t", resaved.syncedAt)
    }
}
