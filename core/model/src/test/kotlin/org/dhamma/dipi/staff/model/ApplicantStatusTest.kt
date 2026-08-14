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
    }

    @Test
    fun mergeNeverIncludesApproved() {
        val merged = ApplicantStatus.mergeChoices(listOf("Confirmed", "Approved", "Received"))
        assertFalse(merged.any { it.equals("Approved", ignoreCase = true) })
        assertTrue(merged.contains("Confirmed"))
    }

    @Test
    fun confNoLooksLike() {
        assertTrue(ConfNo.looksLikeConf("NF129"))
        assertTrue(ConfNo.looksLikeConf("om42"))
        assertFalse(ConfNo.looksLikeConf(""))
        assertEquals("—", ConfNo("").display())
    }
}
