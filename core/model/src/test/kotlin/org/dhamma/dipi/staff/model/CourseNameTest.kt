package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Course naming on the desk is free text typed by staff, so a parse failure
 * must print the raw string rather than refuse to draw the row (v5 T3).
 */
class CourseNameTest {

    @Test
    fun theFourPartShapeSplitsIntoTypeYearAndDates() {
        val name = CourseName.parse("Dhamma Sudha / 10 Day / 2026 / 03 Jan - 14 Jan")
        assertEquals("10 Day", name.type)
        assertEquals("2026", name.year)
        assertEquals("03 Jan - 14 Jan", name.dates)
        assertFalse(name.raw)
    }

    @Test
    fun aTypeWithItsOwnSlashKeepsBothHalves() {
        val name = CourseName.parse("Dhamma Sudha / 3 Day / Children / 2026 / 08 Mar - 10 Mar")
        assertEquals("3 Day / Children", name.type)
        assertEquals("2026", name.year)
    }

    @Test
    fun anUnexpectedShapePrintsRawInsteadOfFailing() {
        listOf(
            "Special course for old students",
            "Dhamma Sudha / 10 Day",
            "Dhamma Sudha / 10 Day / next year / 03 Jan",
        ).forEach {
            val name = CourseName.parse(it)
            assertTrue("$it should fall back to raw", name.raw)
            assertEquals(it, name.type)
        }
    }
}
