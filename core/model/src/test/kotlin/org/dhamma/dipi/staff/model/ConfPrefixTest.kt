package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfPrefixTest {

    @Test
    fun studentPrefixes() {
        assertEquals(ConfPrefix(ConfSeniority.NEW, Gender.F), ConfPrefix.parse("nf1"))
        assertEquals(ConfPrefix(ConfSeniority.NEW, Gender.F), ConfPrefix.parse("nf2"))
        assertEquals(ConfPrefix(ConfSeniority.NEW, Gender.F), ConfPrefix.parse("nf12"))
        assertEquals(ConfPrefix(ConfSeniority.OLD, Gender.F), ConfPrefix.parse("of1"))
        assertEquals(ConfPrefix(ConfSeniority.OLD, Gender.F), ConfPrefix.parse("of12"))
        assertEquals(ConfPrefix(ConfSeniority.NEW, Gender.M), ConfPrefix.parse("nm1"))
        assertEquals(ConfPrefix(ConfSeniority.NEW, Gender.M), ConfPrefix.parse("nm12"))
        assertEquals(ConfPrefix(ConfSeniority.OLD, Gender.M), ConfPrefix.parse("om1"))
        assertEquals(ConfPrefix(ConfSeniority.OLD, Gender.M), ConfPrefix.parse("om12"))
    }

    @Test
    fun mixedCaseAndWhitespace() {
        assertEquals(ConfPrefix(ConfSeniority.NEW, Gender.F), ConfPrefix.parse("NF128"))
        assertEquals(ConfPrefix(ConfSeniority.OLD, Gender.M), ConfPrefix.parse("  Om42  "))
        assertEquals(ConfPrefix(ConfSeniority.NEW, Gender.M), ConfPrefix.parse("Nm7"))
        assertEquals(ConfPrefix(ConfSeniority.OLD, Gender.F), ConfPrefix.parse("\tOf3\n"))
    }

    @Test
    fun missingAndGarbageAreUnknown() {
        assertEquals(ConfPrefix.UNKNOWN, ConfPrefix.parse(null))
        assertEquals(ConfPrefix.UNKNOWN, ConfPrefix.parse(""))
        assertEquals(ConfPrefix.UNKNOWN, ConfPrefix.parse("   "))
        assertEquals(ConfPrefix.UNKNOWN, ConfPrefix.parse("x"))
        assertEquals(ConfPrefix.UNKNOWN, ConfPrefix.parse("12"))
        assertEquals(ConfPrefix.UNKNOWN, ConfPrefix.parse("xx1"))
        assertEquals(ConfPrefix.UNKNOWN, ConfPrefix.parse("??"))
    }

    @Test
    fun sevakPrefixIsUnknownSeniorityWithGender() {
        val sm = ConfPrefix.parse("SM4")
        assertEquals(ConfSeniority.UNKNOWN, sm.seniority)
        assertEquals(Gender.M, sm.gender)
        val sf = ConfPrefix.parse("sf12")
        assertEquals(ConfSeniority.UNKNOWN, sf.seniority)
        assertEquals(Gender.F, sf.gender)
    }

    @Test
    fun halfMatchLeavesTheOtherAxisUnknown() {
        val nx = ConfPrefix.parse("nx9")
        assertEquals(ConfSeniority.NEW, nx.seniority)
        assertNull(nx.gender)
        val xm = ConfPrefix.parse("xm2")
        assertEquals(ConfSeniority.UNKNOWN, xm.seniority)
        assertEquals(Gender.M, xm.gender)
    }

    @Test
    fun unknownStaysVisibleOnAllAndHidesWhenAnAxisIsSet() {
        val unknown = ConfPrefix.UNKNOWN
        assertTrue(unknown.matches(gender = null, seniority = null))
        assertFalse(unknown.matches(gender = Gender.F, seniority = null))
        assertFalse(unknown.matches(gender = null, seniority = ConfSeniority.NEW))
        assertFalse(unknown.matches(gender = Gender.M, seniority = ConfSeniority.OLD))

        val nf = ConfPrefix.parse("nf1")
        assertTrue(nf.matches(gender = null, seniority = null))
        assertTrue(nf.matches(Gender.F, ConfSeniority.NEW))
        assertFalse(nf.matches(Gender.M, ConfSeniority.NEW))
        assertFalse(nf.matches(Gender.F, ConfSeniority.OLD))
    }
}
