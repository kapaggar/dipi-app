package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusWriteTest {
    @Test
    fun queryAlwaysSendsLZeroByDefault() {
        val q = StatusWrite.query("Confirmed", comment = "ok")
        assertEquals("Confirmed", q["s"])
        assertEquals("0", q["l"])
        assertEquals("ok", q["c"])
    }

    @Test
    fun queryRefusesApproved() {
        assertThrows(IllegalArgumentException::class.java) { StatusWrite.query("Approved") }
        assertThrows(IllegalArgumentException::class.java) { StatusWrite.query("approved") }
    }

    @Test
    fun parseOkWithMintedConf() {
        val r = StatusWrite.parseResult("OK", "", "NF129", "Confirmed")
        assertTrue(r.ok)
        assertEquals("NF129", r.confNo)
    }

    @Test
    fun parseFailedKeepsMsg() {
        val r = StatusWrite.parseResult(
            "Failed",
            "Please Edit application and choose Area teacher before approving!",
            "",
            "",
        )
        assertFalse(r.ok)
        assertEquals(
            "Please Edit application and choose Area teacher before approving!",
            r.msg,
        )
        assertNull(r.confNo)
    }
}
