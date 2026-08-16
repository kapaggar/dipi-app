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
    fun confNoLooksLike() {
        assertTrue(ConfNo.looksLikeConf("NF129"))
        assertTrue(ConfNo.looksLikeConf("om42"))
        assertFalse(ConfNo.looksLikeConf(""))
        assertEquals("—", ConfNo("").display())
    }
}
