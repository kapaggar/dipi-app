package org.dhamma.dipi.staff

import org.dhamma.dipi.staff.data.deriveStatuses
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusVocabularyTest {
    @Test
    fun parsedSelectWins() {
        val out = deriveStatuses(
            parsed = listOf("Received", "Confirmed", "Cancelled", "Waiting List"),
            counts = mapOf("All" to 5, "Received" to 5),
        )
        assertTrue(out.contains("Received"))
        assertTrue(out.contains("Confirmed"))
        assertTrue(out.contains("Cancelled"))
        assertTrue(out.contains("Waiting List"))
        assertEquals("Confirmed", out.first())
    }

    @Test
    fun emptySelectFallsBackToRosterCounts() {
        val out = deriveStatuses(
            parsed = emptyList(),
            counts = mapOf("All" to 3, "Received" to 2, "Cancelled" to 1),
        )
        assertTrue(out.contains("Received"))
        assertTrue(out.contains("Cancelled"))
    }

    @Test
    fun emptySelectAndOnlyAllFallsBackToSheetChoices() {
        val out = deriveStatuses(parsed = emptyList(), counts = mapOf("All" to 4))
        assertEquals(ApplicantStatus.SHEET_CHOICES, out)
    }

    @Test
    fun approvedFromServerNeverReachesTheSheet() {
        val merged = deriveStatuses(listOf("Received", "Approved", "Confirmed"), emptyMap())
        assertFalse(merged.any { it.equals("Approved", ignoreCase = true) })
    }
}
