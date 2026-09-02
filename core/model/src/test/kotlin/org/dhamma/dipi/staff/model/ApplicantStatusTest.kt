package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplicantStatusTest {
    @Test
    fun unknownFallsBackToPendingTone() {
        assertEquals(StatusTone.Pending, ApplicantStatus("WaitList").tone)
        assertEquals(StatusTone.Pending, ApplicantStatus("R-ATReview").tone)
    }

    @Test
    fun knownTones() {
        assertEquals(StatusTone.Confirmed, ApplicantStatus("Confirmed").tone)
        assertEquals(StatusTone.Received, ApplicantStatus("Reconfirmation").tone)
        assertEquals(StatusTone.Cancelled, ApplicantStatus("Rejected").tone)
        assertEquals(StatusTone.Expected, ApplicantStatus("Expected").tone)
        assertEquals(StatusTone.Cancelled, ApplicantStatus("Duplicate").tone)
    }

    @Test
    fun mergeNeverIncludesApproved() {
        val merged = ApplicantStatus.mergeChoices(listOf("Confirmed", "Approved", "Received"))
        assertFalse(merged.any { it.equals("Approved", ignoreCase = true) })
        assertTrue(merged.contains("Confirmed"))
    }

    @Test
    fun sheetChoicesCommonThenRareNeverApproved() {
        assertEquals(listOf("Confirmed", "Cancelled", "Duplicate", "Custom…"), ApplicantStatus.COMMON_CHOICES)
        assertEquals("Custom…", ApplicantStatus.SHEET_CHOICES[3])
        assertTrue(ApplicantStatus.SHEET_CHOICES.contains("Clarification"))
        assertFalse(ApplicantStatus.SHEET_CHOICES.any { it.equals("Approved", ignoreCase = true) })
    }

    @Test
    fun mergePutsCommonFirstAndKeepsDuplicate() {
        val merged = ApplicantStatus.mergeChoices(listOf("Received", "Approved", "WaitList", "Confirmed"))
        assertEquals(listOf("Confirmed", "Cancelled", "Duplicate", "Custom…"), merged.take(4))
        assertTrue(merged.contains("Received"))
        assertTrue(merged.contains("WaitList"))
        assertFalse(merged.any { it.equals("Approved", ignoreCase = true) })
    }

    @Test
    fun confNoDisplayShowsDashWhenEmpty() {
        assertEquals("—", ConfNo("").display())
    }

    @Test
    fun deriveStatusesPrefersSelectOverRoster() {
        val out = ApplicantStatus.deriveStatuses(
            select = listOf("Received", "Confirmed", "Waiting List"),
            roster = listOf("All", "WaitList"),
        )
        assertTrue(out.contains("Received"))
        assertTrue(out.contains("Confirmed"))
        assertTrue(out.contains("Waiting List"))
        assertFalse(out.contains("WaitList"))
    }

    @Test
    fun deriveStatusesFallsBackToRosterWhenSelectEmpty() {
        val out = ApplicantStatus.deriveStatuses(
            select = emptyList(),
            roster = listOf("All", "Received", "Cancelled"),
        )
        assertTrue(out.contains("Received"))
        assertTrue(out.contains("Cancelled"))
    }

    @Test
    fun deriveStatusesNeverIncludesApproved() {
        val out = ApplicantStatus.deriveStatuses(
            listOf("Received", "Approved", "Confirmed"),
            listOf("Approved"),
        )
        assertFalse(out.any { it.equals("Approved", ignoreCase = true) })
        assertTrue(ApplicantStatus.isForbiddenWrite("Approved"))
        assertTrue(ApplicantStatus.isForbiddenWrite(" approved "))
        assertFalse(ApplicantStatus.isForbiddenWrite("Confirmed"))
    }
}
