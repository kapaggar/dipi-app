package org.dhamma.dipi.staff.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The course-report CSV, read the way the desk actually writes it (v5 T3).
 *
 * The load-bearing case is the wrapped teacher list: a record spans several
 * physical lines, so anything that splits on `\n` reports the wrong number
 * of courses and the wrong totals.
 */
class CourseReportCsvParserTest {

    private val header =
        "Course,New Male,New Female,New Total,Old Male,Old Female,Old Total," +
            "Student Total,Sevak Male,Sevak Female,Sevak Total," +
            "Conducting Teacher,Assistant Teacher,Teacher Trainee,Teachers"

    @Test
    fun aWrappedTeacherListDoesNotSplitOneCourseIntoTwoRows() {
        val csv = """
            $header
            "Dhamma Sudha / 10 Day / 2026 / 03 Jan - 14 Jan",40,38,78,22,19,41,119,6,5,11,1,2,0,"Anil Kale
            Suma Rao
            Meera Joshi"
            "Dhamma Sudha / Satipatthana / 2026 / 20 Feb - 28 Feb",12,10,22,30,28,58,80,4,4,8,1,0,1,"Ravi Menon"
        """.trimIndent()

        val report = CourseReportCsvParser.parse(csv, from = "2026-01-01", to = "2026-03-31")

        assertEquals(2, report.rows.size)
        assertEquals(listOf(3, 1), report.rows.map { it.teacherNames.size })
        assertEquals(119, report.rows[0].counts.rollTotal)
        assertEquals(80, report.rows[1].counts.rollTotal)
        assertEquals("2026-01-01", report.from)
        assertEquals("2026-03-31", report.to)
    }

    @Test
    fun allFourteenColumnsLandInTheirOwnField() {
        val csv = "$header\n" +
            "\"Dhamma Sudha / 10 Day / 2026 / 03 Jan - 14 Jan\"," +
            "40,38,78,22,19,41,119,6,5,11,1,2,3,\"Anil Kale\""

        val c = CourseReportCsvParser.parse(csv).rows.single().counts

        assertEquals(40, c.newMale)
        assertEquals(38, c.newFemale)
        assertEquals(78, c.newTotal)
        assertEquals(22, c.oldMale)
        assertEquals(19, c.oldFemale)
        assertEquals(41, c.oldTotal)
        assertEquals(119, c.rollTotal)
        assertEquals(6, c.sevakMale)
        assertEquals(5, c.sevakFemale)
        assertEquals(11, c.sevakTotal)
        assertEquals(1, c.teacherConducting)
        assertEquals(2, c.teacherAssistant)
        assertEquals(3, c.teacherTrainee)
    }

    /** The desk sends its own Total line; it is the footer, not a course. */
    @Test
    fun theDesksOwnTotalLineBecomesTheGrandTotalAndNotARow() {
        val csv = """
            $header
            "Dhamma Sudha / 10 Day / 2026 / 03 Jan - 14 Jan",40,38,78,22,19,41,119,6,5,11,1,2,0,"Anil Kale"
            "Dhamma Sudha / 10 Day / 2026 / 20 Feb - 03 Mar",12,10,22,30,28,58,80,4,4,8,1,0,1,"Ravi Menon"
            Total,52,48,100,52,47,99,199,10,9,19,2,2,1,
        """.trimIndent()

        val report = CourseReportCsvParser.parse(csv)

        assertEquals(2, report.rows.size)
        assertTrue(report.rows.none { it.course.equals("total", ignoreCase = true) })
        assertEquals(199, report.grandTotal.rollTotal)
        assertEquals(19, report.grandTotal.sevakTotal)
    }

    /** No Total line: the app sums the rows rather than showing nothing. */
    @Test
    fun withoutADeskTotalTheRowsAreSummed() {
        val csv = """
            $header
            "Dhamma Sudha / 10 Day / 2026 / 03 Jan - 14 Jan",40,38,78,22,19,41,119,6,5,11,1,2,0,"Anil Kale"
            "Dhamma Sudha / 10 Day / 2026 / 20 Feb - 03 Mar",12,10,22,30,28,58,80,4,4,8,1,0,1,"Ravi Menon"
        """.trimIndent()

        val total = CourseReportCsvParser.parse(csv).grandTotal

        assertEquals(199, total.rollTotal)
        assertEquals(52, total.newMale)
        assertEquals(1, total.teacherTrainee)
    }

    /** An empty range is a real answer — never a crash and never an error row. */
    @Test
    fun aHeaderOnlyCsvIsAnEmptyReport() {
        val report = CourseReportCsvParser.parse(header)
        assertTrue(report.isEmpty)
        assertEquals(0, report.grandTotal.rollTotal)
    }

    @Test
    fun anEmptyBodyIsAnEmptyReport() {
        assertTrue(CourseReportCsvParser.parse("").isEmpty)
        assertTrue(CourseReportCsvParser.parse("\n\n").isEmpty)
    }

    /**
     * The live desk answers a range with no courses with a header plus a
     * single blank-name, all-zero data row (verified on the Pixel C, 2026-09-05:
     * any future/reversed range returns "1 course · 0 students"). A row with no
     * course name is not a course, so the report must read empty — otherwise the
     * empty-range guidance never renders and the registrar sees a ghost row.
     */
    @Test
    fun aBlankNameZeroRowIsNotACourseSoTheReportIsEmpty() {
        val csv = "$header\n,0,0,0,0,0,0,0,0,0,0,0,0,0,\"\""

        val report = CourseReportCsvParser.parse(csv, from = "2031-01-01", to = "2031-12-31")

        assertTrue(report.rows.isEmpty())
        assertTrue(report.isEmpty)
    }

    /** A blank-name row among real courses drops out; the real courses stay. */
    @Test
    fun aBlankNameRowIsDroppedButNamedCoursesSurvive() {
        val csv = """
            $header
            "Dhamma Sudha / 10 Day / 2026 / 03 Jan - 14 Jan",40,38,78,22,19,41,119,6,5,11,1,2,0,"Anil Kale"
            ,0,0,0,0,0,0,0,0,0,0,0,0,0,
        """.trimIndent()

        val report = CourseReportCsvParser.parse(csv)

        assertEquals(1, report.rows.size)
        assertEquals(119, report.rows.single().counts.rollTotal)
    }

    /**
     * A renamed or reordered column must degrade to zero, never read the
     * wrong figure into the wrong group.
     */
    @Test
    fun anUnknownColumnIsIgnoredRatherThanMisread() {
        val csv = "Course,Mystery,New Male,Student Total\n" +
            "\"Dhamma Sudha / 10 Day / 2026 / 03 Jan - 14 Jan\",999,40,119"

        val c = CourseReportCsvParser.parse(csv).rows.single().counts

        assertEquals(40, c.newMale)
        assertEquals(119, c.rollTotal)
        assertEquals(0, c.oldMale)
    }

    /** CRLF is one break, not two — the desk streams Windows line endings. */
    @Test
    fun crlfIsOneRecordBreak() {
        val csv = "$header\r\n" +
            "\"Dhamma Sudha / 10 Day / 2026 / 03 Jan - 14 Jan\"," +
            "40,38,78,22,19,41,119,6,5,11,1,2,0,\"Anil Kale\"\r\n"
        assertEquals(1, CourseReportCsvParser.parse(csv).rows.size)
    }

    @Test
    fun anEscapedQuoteInsideANameSurvives() {
        val csv = "$header\n" +
            "\"Dhamma Sudha / 10 Day, special / 2026 / 03 Jan - 14 Jan\"," +
            "1,1,2,0,0,0,2,0,0,0,1,0,0,\"Anil \"\"AT\"\" Kale\""

        val row = CourseReportCsvParser.parse(csv).rows.single()

        assertEquals("Dhamma Sudha / 10 Day, special / 2026 / 03 Jan - 14 Jan", row.course)
        assertEquals(listOf("""Anil "AT" Kale"""), row.teacherNames)
    }
}
