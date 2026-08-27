package org.dhamma.dipi.staff

import org.dhamma.dipi.staff.data.OLDER_COURSE_LIMIT
import org.junit.Assert.assertEquals
import org.junit.Test

class OlderCourseLimitTest {

    @Test
    fun theDeskShowsAtMostThreeOlderCourses() {
        assertEquals(3, OLDER_COURSE_LIMIT)
    }

    @Test
    fun takingTheLimitKeepsTheNewestAndDropsTheRest() {
        val newestFirst = listOf("sep", "aug", "jul", "jun", "may")
        assertEquals(listOf("sep", "aug", "jul"), newestFirst.take(OLDER_COURSE_LIMIT))
    }

    @Test
    fun shorterListsAreUnaffected() {
        assertEquals(listOf("sep"), listOf("sep").take(OLDER_COURSE_LIMIT))
    }
}
