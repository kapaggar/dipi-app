package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WorklistDay0Test {

    private fun card(
        id: Int,
        status: String,
        type: ApplicantType = ApplicantType.Student,
        attended: Boolean = false,
        old: Boolean = false,
    ) = ApplicantCard(
        id = ApplicantId(id),
        centreId = CentreId(1),
        courseId = CourseId(10),
        givenName = "A$id",
        familyName = "B",
        gender = Gender.F,
        status = ApplicantStatus(status),
        type = type,
        oldStudent = old,
        attended = attended,
    )

    @Test
    fun expectedStatusCountsAsExpectedToday() {
        val counts = worklistDaySummary(
            listOf(
                card(1, "Expected"),
                card(2, "Confirmed"),
                card(3, "Expected", type = ApplicantType.Sevak),
                card(4, "Cancelled"),
                card(5, "Confirmed", attended = true),
            ),
        )
        assertEquals(3, counts.students)
        assertEquals(1, counts.servers)
        assertEquals(4, counts.expected)
        assertEquals(1, counts.arrived)
    }

    @Test
    fun confirmedOnlyStillCounts() {
        val counts = worklistDaySummary(listOf(card(1, "Confirmed")))
        assertEquals(1, counts.expected)
    }

    @Test
    fun dateLineFallsBackToTheNameWindow() {
        val course = Course(
            CourseId(10),
            CentreId(1),
            "Dhamma Sudha / 10 Day / 2026 / 2nd-Sep to 13th-Sep",
            "",
            "",
        )
        assertEquals("2 Sep 2026", daySummaryDateLine(course, LocalDate.of(2026, 9, 13)))
    }

    @Test
    fun cancelledDuplicateLeftAreNotZeroDayArrivals() {
        assertFalse(isZeroDayUnattended(card(1, "Cancelled")))
        assertFalse(isZeroDayUnattended(card(2, "Duplicate")))
        assertFalse(isZeroDayUnattended(card(3, "Rejected")))
        assertFalse(isZeroDayUnattended(card(4, "Left")))
        assertFalse(isZeroDayUnattended(card(5, "Expected", attended = true)))
        assertTrue(isZeroDayUnattended(card(6, "Expected")))
        assertTrue(isZeroDayUnattended(card(7, "Confirmed")))
    }
}
