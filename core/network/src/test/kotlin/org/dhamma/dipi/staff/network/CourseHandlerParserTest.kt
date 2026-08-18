package org.dhamma.dipi.staff.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseHandlerParserTest {
    @Test
    fun readsTypeDatesCohortStatusesAndFinalized() {
        val rows = CourseHandlerParser.courses(MockFixtures.courseHandlerJson)
        assertEquals(2, rows.size)
        val open = rows[0]
        assertEquals(10, open.id)
        assertEquals("10-Day", open.type)
        assertEquals("20 Aug 2026", open.start)
        assertEquals("31 Aug 2026", open.end)
        assertFalse(open.cancelled)
        assertEquals("Open", open.status)
        assertEquals("Open", open.statusNm)
        assertEquals("FastFilling", open.statusNf)
        assertFalse(open.finalized)
        assertTrue(rows[1].finalized)
        assertEquals("Closed", rows[1].statusOm)
    }

    @Test
    fun htmlRefusalIsNullSoTheCallerKeepsGoing() {
        assertNull(CourseHandlerParser.coursesOrNull(MockFixtures.accessDeniedHtml))
        assertEquals(emptyList<org.dhamma.dipi.staff.model.ManagedCourse>(), CourseHandlerParser.courses("{\"data\":[]}"))
    }
}
