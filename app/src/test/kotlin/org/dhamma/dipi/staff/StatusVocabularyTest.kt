package org.dhamma.dipi.staff

import org.dhamma.dipi.staff.data.deriveStatuses
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class StatusVocabularyTest {
    @Test
    fun parsedSelectWins() {
        val out = deriveStatuses(
            parsed = listOf("Received", "Confirmed", "Cancelled", "Waiting List"),
            counts = mapOf("All" to 5, "Received" to 5),
        )
        assertEquals(
            listOf("Received", "Confirmed", "Cancelled", "Waiting List"),
            out,
        )
    }

    @Test
    fun emptySelectFallsBackToRosterCounts() {
        val out = deriveStatuses(
            parsed = emptyList(),
            counts = mapOf("All" to 3, "Received" to 2, "Cancelled" to 1),
        )
        assertEquals(listOf("Received", "Cancelled"), out)
    }

    @Test
    fun emptySelectAndOnlyAllLeavesNothing() {
        val out = deriveStatuses(parsed = emptyList(), counts = mapOf("All" to 4))
        assertEquals(emptyList<String>(), out)
    }

    @Test
    fun approvedFromServerNeverReachesTheSheet() {
        val merged = ApplicantStatus.mergeChoices(
            deriveStatuses(listOf("Received", "Approved", "Confirmed"), emptyMap()),
        )
        assertFalse(merged.any { it.equals("Approved", ignoreCase = true) })
    }
}
