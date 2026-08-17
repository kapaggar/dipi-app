package org.dhamma.dipi.staff.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fixture mirrors dh_manageapp course_summary(): summary-block divs, a
 * table-heading course link, then theme('table') output with header
 * '' NM OM Total SM (spacer) NF OF Total SF and a bold Total row.
 * Numbers follow the live Dhamma Sudha capture.
 */
class CentrePageParserTest {

    private fun statusRow(
        label: String,
        nm: String, om: String, totM: String, sm: String,
        nf: String, of: String, totF: String, sf: String,
    ): String {
        fun cell(v: String) = "<td>${if (v.isEmpty()) "" else "<a href=\"/search-course/91/68670?s=$label&t=&g=M\">$v</a>"}</td>"
        fun bold(v: String) = "<td>${if (v.isEmpty()) "" else "<b><a href=\"/search-course/91/68670?s=$label&t=&g=\">$v</a></b>"}</td>"
        return "<tr><td><a href=\"/search-course/91/68670?s=$label&t=&g=\">$label</a></td>" +
            cell(nm) + cell(om) + bold(totM) + cell(sm) + "<td>&nbsp;&nbsp;&nbsp;</td>" +
            cell(nf) + cell(of) + bold(totF) + cell(sf) + "</tr>"
    }

    private val header =
        "<thead><tr><th></th><th>NM</th><th>OM</th><th>Total</th><th>SM</th>" +
            "<th>&nbsp;&nbsp;</th><th>NF</th><th>OF</th><th>Total</th><th>SF</th></tr></thead>"

    private fun totalRow(
        nm: Int, om: Int, totM: Int, sm: Int,
        nf: Int, of: Int, totF: Int, sf: Int,
    ): String =
        "<tr><td><b>Total</b></td>" +
            "<td><a href=\"/search-course/91/68670?s=&t=0&g=M\">$nm</a></td>" +
            "<td><a href=\"/search-course/91/68670?s=&t=1&g=M\">$om</a></td>" +
            "<td><b><a href=\"/search-course/91/68670?s=&t=&g=M\">$totM</a></b></td>" +
            "<td><a href=\"/search-course/91/68670?s=&t=&g=M&at=s\">$sm</a></td>" +
            "<td>&nbsp;&nbsp;&nbsp;</td>" +
            "<td><a href=\"/search-course/91/68670?s=&t=0&g=F\">$nf</a></td>" +
            "<td><a href=\"/search-course/91/68670?s=&t=1&g=F\">$of</a></td>" +
            "<td><b><a href=\"/search-course/91/68670?s=&t=&g=F\">$totF</a></b></td>" +
            "<td><a href=\"/search-course/91/68670?s=&t=&g=F&at=s\">$sf</a></td></tr>"

    // Dhamma Sudha / 2nd-Sep panel: Received 2, Confirmed 58+19, Cancelled 5+2,
    // Review present but not extracted; Total row 81/3 + 25/2.
    private val septemberBlock =
        """<div class="summary-block"><div class="table-heading"><a href="/course/91/68670">Dhamma Sudha / 10 Day / 2026 / 2nd-Sep to 13th-Sep</a></div>
        <table>$header<tbody>
        ${statusRow("Received", "", "2", "2", "1", "", "", "", "")}
        ${statusRow("Confirmed", "40", "18", "58", "", "14", "5", "19", "1")}
        ${statusRow("ReConfirmation", "", "", "", "", "", "", "", "")}
        ${statusRow("Cancelled", "4", "1", "5", "", "", "2", "2", "")}
        ${statusRow("Review", "6", "2", "8", "1", "1", "1", "2", "")}
        ${totalRow(57, 24, 81, 3, 18, 7, 25, 2)}
        </tbody></table></div>"""

    // Dhamma Sudha / 19th-Aug panel: has an Expected row.
    private val augustBlock =
        """<div class="summary-block"><div class="table-heading"><a href="/course/91/68669">Dhamma Sudha / 10 Day / 2026 / 19th-Aug to 30th-Aug</a></div>
        <table>$header<tbody>
        ${statusRow("Confirmed", "3", "4", "7", "", "", "5", "5", "")}
        ${statusRow("Cancelled", "25", "13", "38", "4", "12", "4", "16", "1")}
        ${statusRow("Expected", "29", "15", "44", "4", "9", "6", "15", "3")}
        ${totalRow(79, 42, 121, 9, 25, 16, 41, 4)}
        </tbody></table></div>"""

    private val html = """
        <h1>Manage Dhamma Sudha</h1>
        <h2>Upcoming Courses</h2>
        $augustBlock
        $septemberBlock
        <div class="summary-block"><div class="table-heading"><a href="/course/91/68671">Dhamma Sudha / 3 Day / 2026 / 3rd-Oct to 6th-Oct</a></div></div>
    """.trimIndent()

    @Test
    fun extractsPerCourseCountsFromTheStatusTable() {
        val summaries = CentrePageParser.courseSummaries(html)
        val sep = summaries.getValue(68670)
        assertEquals(2, sep.received)
        assertEquals(77, sep.confirmed) // 58 male + 19 female student totals
        assertEquals(0, sep.expected)
        assertEquals(7, sep.cancelled) // 5 + 2
        assertEquals(111, sep.total) // 81 + 25 students + 3 SM + 2 SF
    }

    @Test
    fun expectedRowAndGrandTotalIncludeBothGendersAndSevaks() {
        val aug = CentrePageParser.courseSummaries(html).getValue(68669)
        assertEquals(12, aug.confirmed)
        assertEquals(59, aug.expected) // 44 + 15
        assertEquals(54, aug.cancelled) // 38 + 16
        assertEquals(0, aug.received)
        assertEquals(175, aug.total) // 121 + 41 students + 9 SM + 4 SF
    }

    @Test
    fun headingWithoutATableYieldsNoSummary() {
        val summaries = CentrePageParser.courseSummaries(html)
        assertNull(summaries[68671])
        assertFalse(summaries.containsKey(68671))
    }

    @Test
    fun pageWithNoSummaryBlocksYieldsEmptyMap() {
        assertEquals(emptyMap<Int, Any>(), CentrePageParser.courseSummaries("<html><body>login form</body></html>"))
    }

    @Test
    fun olderSelectOptionsAreThoseBeforeTheUpcomingBlockNewestFirst() {
        val dash = """
            <select id="edit-course" name="course">
              <option value="">Choose</option>
              <option value="10">Dhamma Sudha / 10 Day / 2026 / 20th-May to 31st-May</option>
              <option value="20">Dhamma Sudha / 10 Day / 2026 / 6th-Aug to 17th-Aug</option>
              <option value="30">Dhamma Sudha / 10 Day / 2026 / 19th-Aug to 30th-Aug</option>
              <option value="40">Dhamma Sudha / 10 Day / 2026 / 2nd-Sep to 13th-Sep</option>
              <option value="50">Dhamma Sudha / STP / 2026 / 21st-Oct to 29th-Oct</option>
            </select>
        """.trimIndent()
        val older = CentrePageParser.olderCourseOptions(dash, upcomingIds = setOf(30, 40))
        assertEquals(listOf(20, 10), older.map { it.id })
        assertTrue(older[0].label.contains("6th-Aug"))
    }

    @Test
    fun olderSelectIsEmptyWhenTheDropdownOnlyHasUpcoming() {
        val dash = """
            <select id="edit-course" name="course">
              <option value="30">Upcoming</option>
            </select>
        """.trimIndent()
        assertEquals(emptyList<Any>(), CentrePageParser.olderCourseOptions(dash, setOf(30)))
    }

    @Test
    fun reConfirmationRowNeverBleedsIntoConfirmed() {
        // The September block has a blank ReConfirmation row; equals-matching
        // must keep it out of the Confirmed count.
        val sep = CentrePageParser.courseSummaries(html).getValue(68670)
        assertEquals(77, sep.confirmed)
    }
}
