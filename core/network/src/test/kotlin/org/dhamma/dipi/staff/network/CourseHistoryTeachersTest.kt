package org.dhamma.dipi.staff.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CourseHistoryTeachersTest {

    @Test
    fun historyPairsTeacherParenSUnderFirstAndMostRecent() {
        val (first, last) = CourseHistoryTeachers.fromHistory(
            listOf(
                "First Course" to "2019-2-2, Example Centre",
                "Teacher(s)" to "Unknown",
                "Most Recent Course (Sat)" to "2026-2-1, Example Place",
                "Teacher(s)" to "Mr. Example",
                "Practice Details" to "Daily sitting",
            ),
        )
        assertEquals("Unknown", first)
        assertEquals("Mr. Example", last)
    }

    @Test
    fun editFormReadsOnlyTheTwoTeacherInputs() {
        val html = ApplicantEditFormFixtures.completeHtml(
            extraHidden = """
                <input type="text" name="ac_first_teacher_str" value="Unknown" />
                <input value="Mr. Example" name="ac_last_teacher_str" type="text" />
            """.trimIndent(),
        )
        val (first, last) = CourseHistoryTeachers.fromEditForm(html)
        assertEquals("Unknown", first)
        assertEquals("Mr. Example", last)
        assertFalse(html.let { CourseHistoryTeachers.fromEditForm(it).toString() }
            .contains(ApplicantEditFormFixtures.NPI_DOC))
    }

    @Test
    fun editFormWithoutTeacherInputsStaysEmpty() {
        assertEquals("" to "", CourseHistoryTeachers.fromEditForm(ApplicantEditFormFixtures.completeHtml()))
    }
}
