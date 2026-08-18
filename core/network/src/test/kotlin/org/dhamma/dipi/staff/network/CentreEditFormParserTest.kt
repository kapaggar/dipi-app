package org.dhamma.dipi.staff.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CentreEditFormParserTest {
    @Test
    fun readsNameAddressEmailAndFlags() {
        val s = CentreEditFormParser.parse(MockFixtures.centreEditHtml(1))!!
        assertEquals("Dhamma Sudha", s.name)
        assertEquals("Igatpuri Road", s.address)
        assertEquals("info@sudha.dhamma.org", s.email)
        assertEquals("Nashik", s.city)
        assertEquals("422003", s.pincode)
        assertFalse(s.preconf!!)
        assertTrue(s.reconf!!)
        assertTrue(s.expectedMail!!)
        assertTrue(s.whatsappPreconf!!)
        assertFalse(s.whatsappMsg!!)
        assertEquals("10", s.reconfDays)
        assertEquals("3", s.expectedDays)
    }

    @Test
    fun refusalHtmlIsNull() {
        assertNull(CentreEditFormParser.parse(MockFixtures.accessDeniedHtml))
    }
}
