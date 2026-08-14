package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutboxReconcilerTest {
    @Test
    fun failureUsesServerMsgVerbatim() {
        val msg = "Please Edit application and choose Area teacher before approving!"
        val s = OutboxReconciler.snack(
            "Confirmed",
            StatusChangeResult(false, "Failed", msg, null, null),
            null,
        )
        assertTrue(s.error)
        assertEquals(msg, s.text)
    }

    @Test
    fun successIncludesMintedConf() {
        val s = OutboxReconciler.snack(
            "Confirmed",
            StatusChangeResult(true, "OK", "", "NF129", "Confirmed"),
            "Confirmed",
        )
        assertFalse(s.error)
        assertEquals("Status updated · conf no NF129", s.text)
    }

    @Test
    fun serverWinsAnnouncesDrift() {
        val s = OutboxReconciler.snack(
            "Confirmed",
            StatusChangeResult(true, "OK", "", null, "Expected"),
            "Expected",
        )
        assertEquals("Status updated · server now Expected", s.text)
    }
}
