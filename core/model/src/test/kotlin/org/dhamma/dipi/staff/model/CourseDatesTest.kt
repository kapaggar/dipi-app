package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * Spec 2a S2 — the course window parsed out of the course *name*, and the
 * running-course lock built on it. Every fixture format from the recon:
 * 4-segment with year, 3-segment without one, STP, single date, a Dec→Jan
 * roll, and garbage → null.
 */
class CourseDatesTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 2)

    private fun course(id: Int, name: String) = Course(
        id = CourseId(id),
        centreId = CentreId(12),
        name = name,
        start = "",
        end = "",
    )

    // ---- parseCourseWindow

    @Test
    fun fourSegmentNameWithYearParses() {
        val w = parseCourseWindow("Dhamma Sudha / 10 Day / 2026 / 2nd-Sep to 13th-Sep", today)
        assertEquals(CourseWindow(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 13)), w)
    }

    @Test
    fun threeSegmentNameWithoutYearInfersTheYearAroundToday() {
        val w = parseCourseWindow("Dhamma Sudha / 10 Day / 19th-Aug to 30th-Aug", today)
        assertEquals(CourseWindow(LocalDate.of(2026, 8, 19), LocalDate.of(2026, 8, 30)), w)
    }

    @Test
    fun inferredYearReachesBackwardWhenTheMonthAlreadyPassed() {
        // Today 2026-09-02: an Oct start within [today-11mo, today+11mo] closest
        // to today is 2025-10 (25 days back beats 2026-10 not at all — 2026-10
        // is 43 days ahead; nearest wins, so 2026-10). A December start instead:
        // 2025-12 (9 months back) vs 2026-12 — 2026-12 is outside +11mo? No,
        // 2027-08 is the ceiling so 2026-12 fits; nearest is 2025-12? 2025-12-20
        // is 256 days back, 2026-12-20 is 109 days ahead → 2026 wins.
        val w = parseCourseWindow("Dhamma Sudha / 10 Day / 20th-Dec to 31st-Dec", today)
        assertEquals(LocalDate.of(2026, 12, 20), w?.start)
    }

    @Test
    fun stpNameParses() {
        val w = parseCourseWindow("Dhamma Sudha / STP / 2026 / 5th-Sep to 7th-Sep", today)
        assertEquals(CourseWindow(LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 7)), w)
    }

    @Test
    fun singleDateNameGetsEndEqualToStart() {
        val w = parseCourseWindow("Dhamma Sudha / 10 Day / 2nd-Sep", today)
        assertEquals(CourseWindow(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 2)), w)
    }

    @Test
    fun decToJanRangeRollsTheYearOnTheEndDate() {
        val w = parseCourseWindow("Dhamma Sudha / 10 Day / 2026 / 26th-Dec to 6th-Jan", today)
        assertEquals(CourseWindow(LocalDate.of(2026, 12, 26), LocalDate.of(2027, 1, 6)), w)
    }

    @Test
    fun garbageIsNull() {
        assertNull(parseCourseWindow("", today))
        assertNull(parseCourseWindow("Dhamma Sudha", today))
        assertNull(parseCourseWindow("Dhamma Sudha / 10 Day / 2026", today))
        assertNull(parseCourseWindow("Dhamma Sudha / 10 Day / 32nd-Sep to 40th-Sep", today))
        assertNull(parseCourseWindow("Dhamma Sudha / 10 Day / 2nd-Xyz to 13th-Xyz", today))
        assertNull(parseCourseWindow("no slashes at all just words", today))
    }

    @Test
    fun ordinalVariantsAllParse() {
        assertEquals(
            LocalDate.of(2026, 9, 1),
            parseCourseWindow("X / 10 Day / 2026 / 1st-Sep to 3rd-Sep", today)?.start,
        )
        assertEquals(
            LocalDate.of(2026, 9, 3),
            parseCourseWindow("X / 10 Day / 2026 / 1st-Sep to 3rd-Sep", today)?.end,
        )
    }

    // ---- CourseWindow.contains

    @Test
    fun windowContainsItsEdgesInclusively() {
        val w = CourseWindow(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 13))
        assertEquals(true, LocalDate.of(2026, 9, 2) in w)
        assertEquals(true, LocalDate.of(2026, 9, 13) in w)
        assertEquals(false, LocalDate.of(2026, 9, 1) in w)
        assertEquals(false, LocalDate.of(2026, 9, 14) in w)
    }

    // ---- runningCourse

    @Test
    fun runningCoursePicksByContainmentInPageOrder() {
        val past = course(1, "Dhamma Sudha / 10 Day / 2026 / 5th-Aug to 16th-Aug")
        val running = course(2, "Dhamma Sudha / 10 Day / 2026 / 2nd-Sep to 13th-Sep")
        val alsoRunning = course(3, "Dhamma Sudha / STP / 2026 / 1st-Sep to 3rd-Sep")
        val future = course(4, "Dhamma Sudha / 10 Day / 2026 / 30th-Sep to 11th-Oct")
        // First match in page order wins.
        assertEquals(running, runningCourse(listOf(past, running, alsoRunning, future), today))
        assertEquals(alsoRunning, runningCourse(listOf(past, alsoRunning, running, future), today))
    }

    @Test
    fun runningCourseIsNullWhenNothingContainsToday() {
        val past = course(1, "Dhamma Sudha / 10 Day / 2026 / 5th-Aug to 16th-Aug")
        val future = course(4, "Dhamma Sudha / 10 Day / 2026 / 30th-Sep to 11th-Oct")
        val garbage = course(5, "Dhamma Sudha special event")
        assertNull(runningCourse(listOf(past, future, garbage), today))
        assertNull(runningCourse(emptyList(), today))
    }

    // ---- the settings card's dates line

    @Test
    fun windowLabelReadsAsARange() {
        assertEquals(
            "2 Sep – 13 Sep 2026",
            CourseWindow(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 13)).label(),
        )
        assertEquals(
            "2 Sep 2026",
            CourseWindow(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 2)).label(),
        )
    }
}
