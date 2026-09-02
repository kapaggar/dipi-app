package org.dhamma.dipi.staff.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplicantHistoryParserTest {
    @Test
    fun coursesAreCourseTypeStatusAttendedAddress() {
        val rows = ApplicantHistoryParser.courses(MockFixtures.appCoursesHtml(1))
        assertEquals(2, rows.size)
        assertEquals("10-Day · Aug 2026", rows[0].course)
        assertEquals("Student", rows[0].type)
        assertEquals("Confirmed", rows[0].status)
        assertEquals("False", rows[0].attended)
        assertEquals("Pune", rows[0].address)
        assertEquals("True", rows[1].attended)
    }

    @Test
    fun activityIsDateMessageUser() {
        val rows = ApplicantHistoryParser.activity(MockFixtures.appActivityHtml(1))
        assertEquals("2026-08-16 10:22:00", rows[0].at)
        assertEquals("Status Change · Confirmed", rows[0].activity)
        assertEquals("sudha.user", rows[0].user)
    }

    @Test
    fun clarificationsParseViewHrefAndNoUpload() {
        val rows = ApplicantHistoryParser.clarifications(MockFixtures.appClarificationsHtml(11))
        assertEquals(2, rows.size)
        assertEquals("Please confirm travel date", rows[0].message)
        assertEquals("View", rows[0].fileLabel)
        assertEquals(3, rows[0].clarId)
        assertEquals("No Upload", rows[1].fileLabel)
        assertNull(rows[1].clarId)
    }

    @Test
    fun emptyFragmentsYieldNoRows() {
        assertTrue(ApplicantHistoryParser.courses("<h4>Applicant Courses</h4>").isEmpty())
        assertTrue(ApplicantHistoryParser.activity("<h4>Activity Log</h4>").isEmpty())
        assertTrue(ApplicantHistoryParser.clarifications("").isEmpty())
    }
}
