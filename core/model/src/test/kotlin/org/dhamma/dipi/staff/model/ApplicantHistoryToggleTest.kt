package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Owner report 2026-09-01: an opened section could never be shut again,
 * because "fetched" was doing double duty as "open".
 */
class ApplicantHistoryToggleTest {
    private val row = ApplicantCourseRow("10-Day · Aug 2026", "Student", "Confirmed", "False", "Pune")

    @Test
    fun firstTapOpensAndFetches() {
        val cold = ApplicantDeskHistory()
        assertTrue(cold.tapNeedsFetch(HISTORY_COURSES))
        assertEquals(setOf(HISTORY_COURSES), cold.toggled(HISTORY_COURSES).expanded)
    }

    @Test
    fun secondTapClosesWithoutFetchingAndKeepsTheRows() {
        val open = ApplicantDeskHistory(courses = listOf(row), expanded = setOf(HISTORY_COURSES))
        val closed = open.toggled(HISTORY_COURSES)
        assertFalse("closing must never hit the desk", open.tapNeedsFetch(HISTORY_COURSES))
        assertEquals(emptySet<String>(), closed.expanded)
        assertEquals(listOf(row), closed.courses)
    }

    @Test
    fun reopeningCachedRowsCostsNoSecondRequest() {
        val closedButCached = ApplicantDeskHistory(courses = listOf(row), expanded = emptySet())
        assertFalse(closedButCached.tapNeedsFetch(HISTORY_COURSES))
        assertEquals(setOf(HISTORY_COURSES), closedButCached.toggled(HISTORY_COURSES).expanded)
    }

    @Test
    fun sectionsToggleIndependently() {
        val both = ApplicantDeskHistory(expanded = setOf(HISTORY_COURSES, HISTORY_ACTIVITY))
        assertEquals(setOf(HISTORY_ACTIVITY), both.toggled(HISTORY_COURSES).expanded)
    }

    @Test
    fun aFailedSectionRefetchesOnItsNextOpen() {
        val failed = ApplicantDeskHistory(errors = mapOf(HISTORY_ACTIVITY to "Unavailable"))
        assertTrue(failed.tapNeedsFetch(HISTORY_ACTIVITY))
    }

    @Test
    fun anUnknownKeyNeverFetches() {
        assertFalse(ApplicantDeskHistory().tapNeedsFetch("letters"))
    }
}
