package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UserCentreMapTest {
    @Test
    fun sudhaUserMapsToDhammaSudha() {
        assertEquals("Dhamma Sudha", UserCentreMap.name("sudha.user"))
    }

    @Test
    fun gangaUserMapsToDhammaGanga() {
        assertEquals("Dhamma Ganga", UserCentreMap.name("ganga.user"))
    }

    @Test
    fun dottedRegistrarUsesLastSegment() {
        assertEquals("Dhamma Sudha", UserCentreMap.name("registrar.sudha"))
    }
}
