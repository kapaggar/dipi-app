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

    // course_summary() matrix contract: 10 <td> cells per row — label, then
    // male NM/OM/Total/SM, a spacer, then female NF/OF/Total/SF. Cells 3 and
    // 8 are the desk's own computed totals and courseMatrices must ignore
    // them; MatrixRow recomputes from NM+OM / NF+OF instead.
    private val course41Block = """
        <div class="summary-block"><div class="table-heading"><a href="/course/7/41">Dhamma Sudha / 10 Day / 2nd-Sep</a></div>
        <table><tr><th></th><th>NM</th><th>OM</th><th>Total</th><th>SM</th><th>&nbsp;</th><th>NF</th><th>OF</th><th>Total</th><th>SF</th></tr>
        <tr><td><a>Received</a></td><td></td><td></td><td></td><td></td><td>&nbsp;</td><td></td><td><a>1</a></td><td><b><a>1</a></b></td><td></td></tr>
        <tr><td><a>Confirmed</a></td><td><a>7</a></td><td><a>3</a></td><td><b><a>10</a></b></td><td><a>1</a></td><td>&nbsp;</td><td><a>3</a></td><td></td><td><b><a>3</a></b></td><td></td></tr>
        <tr><td><a>Errors</a></td><td></td><td></td><td></td><td></td><td>&nbsp;</td><td></td><td></td><td></td><td></td></tr>
        <tr><td><b>Total</b></td><td>72</td><td>31</td><td><b>103</b></td><td>4</td><td>&nbsp;</td><td>32</td><td>13</td><td><b>45</b></td><td>3</td></tr>
        </table></div>
    """.trimIndent()

    // A second, independent course in the same page — proves segmentation
    // (heading → next heading) doesn't bleed rows across summary-blocks.
    private val course55Block = """
        <div class="summary-block"><div class="table-heading"><a href="/course/7/55">Dhamma Giri / 3 Day / 5th-Sep</a></div>
        <table><tr><th></th><th>NM</th><th>OM</th><th>Total</th><th>SM</th><th>&nbsp;</th><th>NF</th><th>OF</th><th>Total</th><th>SF</th></tr>
        <tr><td><a>Cancelled</a></td><td><a>2</a></td><td></td><td><b><a>2</a></b></td><td></td><td>&nbsp;</td><td></td><td><a>1</a></td><td><b><a>1</a></b></td><td></td></tr>
        <tr><td><b>Total</b></td><td>5</td><td>2</td><td><b>7</b></td><td>0</td><td>&nbsp;</td><td>3</td><td>1</td><td><b>4</b></td><td>0</td></tr>
        </table></div>
    """.trimIndent()

    private val matrixHtml = "$course41Block\n$course55Block"

    @Test
    fun courseMatricesSplitsNewOldSevakByGenderAndRecomputesTotals() {
        val matrices = CentrePageParser.courseMatrices(matrixHtml)
        val course41 = matrices.getValue(41)

        val confirmed = course41.row("Confirmed")!!
        assertEquals(7, confirmed.newMale)
        assertEquals(3, confirmed.oldMale)
        assertEquals(1, confirmed.sevakMale)
        assertEquals(3, confirmed.newFemale)
        assertEquals(0, confirmed.oldFemale)
        assertEquals(10, confirmed.maleTotal) // recomputed from NM+OM, not read from the Total cell

        val received = course41.row("Received")!!
        assertEquals(1, received.oldFemale)

        val errors = course41.row("Errors")!!
        assertTrue(errors.isEmpty)

        val total = course41.total!!
        assertEquals(72, total.newMale)
        assertEquals(3, total.sevakFemale)

        assertEquals(listOf("Received", "Confirmed"), course41.highlights.map { it.label })

        val course55 = matrices.getValue(55)
        val cancelled = course55.row("Cancelled")!!
        assertEquals(2, cancelled.newMale)
        assertEquals(1, cancelled.oldFemale)
        assertEquals(5, course55.total!!.newMale)
    }

    @Test
    fun courseMatricesOmitsHeadingsWithoutATable() {
        val matrices = CentrePageParser.courseMatrices(html)
        assertNull(matrices[68671])
    }

    @Test
    fun courseMatricesOnPageWithNoSummaryBlocksYieldsEmptyMap() {
        assertEquals(emptyMap<Int, Any>(), CentrePageParser.courseMatrices("<html><body>login form</body></html>"))
    }

    // Diagnostic for the owner's "bottom two cards render nothing" report:
    // a page with FOUR summary-blocks (four distinct course ids), back to
    // back with no filler between them, must yield four parsed entries from
    // both courseMatrices and courseSummaries. If this fails, the parser
    // itself drops later blocks — a real regression. If it passes (as it
    // does), the parser's heading→next-heading segmentation is not the
    // cause of the owner's missing cards, and the cause lies upstream (the
    // desk simply not emitting a block for those two course ids).
    private fun simpleBlock(courseId: Int, name: String, totalRow: String): String = """
        <div class="summary-block"><div class="table-heading"><a href="/course/91/$courseId">$name</a></div>
        <table><tr><th></th><th>NM</th><th>OM</th><th>Total</th><th>SM</th><th>&nbsp;</th><th>NF</th><th>OF</th><th>Total</th><th>SF</th></tr>
        <tr><td><a>Confirmed</a></td><td><a>1</a></td><td></td><td><b><a>1</a></b></td><td></td><td>&nbsp;</td><td><a>1</a></td><td></td><td><b><a>1</a></b></td><td></td></tr>
        $totalRow
        </table></div>
    """.trimIndent()

    @Test
    fun fourSummaryBlocksBackToBackAllParse() {
        val fourBlocksHtml = listOf(
            simpleBlock(101, "Course A", "<tr><td><b>Total</b></td><td>1</td><td>0</td><td><b>1</b></td><td>0</td><td>&nbsp;</td><td>1</td><td>0</td><td><b>1</b></td><td>0</td></tr>"),
            simpleBlock(102, "Course B", "<tr><td><b>Total</b></td><td>2</td><td>0</td><td><b>2</b></td><td>0</td><td>&nbsp;</td><td>2</td><td>0</td><td><b>2</b></td><td>0</td></tr>"),
            simpleBlock(103, "Course C", "<tr><td><b>Total</b></td><td>3</td><td>0</td><td><b>3</b></td><td>0</td><td>&nbsp;</td><td>3</td><td>0</td><td><b>3</b></td><td>0</td></tr>"),
            simpleBlock(104, "Course D", "<tr><td><b>Total</b></td><td>4</td><td>0</td><td><b>4</b></td><td>0</td><td>&nbsp;</td><td>4</td><td>0</td><td><b>4</b></td><td>0</td></tr>"),
        ).joinToString("\n")

        val matrices = CentrePageParser.courseMatrices(fourBlocksHtml)
        assertEquals(setOf(101, 102, 103, 104), matrices.keys)
        // Each block's own Total row is distinct (1/2/3/4) — proves no
        // cross-block bleed, not just four present keys.
        assertEquals(1, matrices.getValue(101).total!!.newMale)
        assertEquals(2, matrices.getValue(102).total!!.newMale)
        assertEquals(3, matrices.getValue(103).total!!.newMale)
        assertEquals(4, matrices.getValue(104).total!!.newMale)

        val summaries = CentrePageParser.courseSummaries(fourBlocksHtml)
        assertEquals(setOf(101, 102, 103, 104), summaries.keys)
        // Total row's male + female Total cells (no sevaks in this fixture):
        // block B is 2+2=4, block D is 4+4=8.
        assertEquals(4, summaries.getValue(102).total)
        assertEquals(8, summaries.getValue(104).total)
    }

    @Test
    fun courseSummariesUnaffectedByMatrixShapedHtml() {
        // Proof courseMatrices was added without disturbing courseSummaries:
        // same HTML, same old-shape aggregation via the desk's own totals
        // (cells 3 and 8) that courseMatrices deliberately ignores.
        val summaries = CentrePageParser.courseSummaries(matrixHtml)
        val s41 = summaries.getValue(41)
        assertEquals(1, s41.received)
        assertEquals(13, s41.confirmed)
        assertEquals(0, s41.expected)
        assertEquals(0, s41.cancelled)
        assertEquals(155, s41.total)

        val s55 = summaries.getValue(55)
        assertEquals(0, s55.received)
        assertEquals(0, s55.confirmed)
        assertEquals(3, s55.cancelled)
        assertEquals(11, s55.total)
    }

    // --- Bug B REOPENED (2026-08-30): "No applications yet" was disproved —
    // the owner tapped the 7-Oct card live and it opened with 51
    // applications, 36 on the roll. The heading for that course id DOES
    // appear in the fetched HTML (SearchPageParser.coursesFromDashboard
    // sources the upcoming list from the identical table-heading markup),
    // and dh_manageapp's course_summary() (course.inc:679-743) unconditionally
    // emits the <div class="summary-block"> + theme('table', ...) call in
    // the SAME loop iteration as the table-heading — there is no code path
    // that emits one without the other. So whatever produced the gap must
    // either be a parser defect below, or something not reconstructable from
    // the PHP source alone (see the final test + report for the verdict).

    /**
     * Byte-faithful to what theme_table() (includes/theme.inc:1996) actually
     * emits: <thead><tr>...</tr></thead><tbody><tr class="odd/even">...
     * </tr></tbody>, striping classes on every body <tr>, and the real
     * course_summary() spacer cell. Four blocks, back to back, mirroring
     * the owner's observed layout where blocks 1-2 rendered fine and blocks
     * 3-4 (by position, not content) did not.
     */
    private fun themedRow(striping: String, cells: List<String>): String =
        "<tr class=\"$striping\">" + cells.joinToString("") { "<td>$it</td>" } + "</tr>"

    private fun themedBlock(courseId: Int, name: String, totalCells: List<String>): String {
        val header = "<thead><tr><th></th><th>NM</th><th>OM</th><th>Total</th><th>SM</th>" +
            "<th>&nbsp;&nbsp;</th><th>NF</th><th>OF</th><th>Total</th><th>SF</th></tr></thead>"
        val confirmedRow = themedRow(
            "odd",
            listOf(
                "<a href=\"/search-course/91/$courseId?s=Confirmed\">Confirmed</a>",
                "<a>1</a>", "", "<b><a>1</a></b>", "", "&nbsp;&nbsp;&nbsp;",
                "<a>1</a>", "", "<b><a>1</a></b>", "",
            ),
        )
        val totalRow = "<tr class=\"even\">" +
            "<td><b>Total</b></td>" +
            totalCells.joinToString("") { "<td>$it</td>" } +
            "</tr>"
        return """<div class="summary-block"><div class="table-heading"><a href="/course/91/$courseId">$name</a></div>
            <table class="sticky-enabled">$header<tbody>
            $confirmedRow
            $totalRow
            </tbody></table></div>"""
    }

    @Test
    fun fourThemeTableFaithfulBlocksAllParseRegardlessOfPosition() {
        val fourBlocksHtml = listOf(
            themedBlock(201, "Course A", listOf("1", "0", "<b>1</b>", "0", "&nbsp;&nbsp;&nbsp;", "1", "0", "<b>1</b>", "0")),
            themedBlock(202, "Course B", listOf("2", "0", "<b>2</b>", "0", "&nbsp;&nbsp;&nbsp;", "2", "0", "<b>2</b>", "0")),
            // 7-Oct analog: a 3-day course, third of four.
            themedBlock(203, "Course C / 3 Day / 7th-Oct", listOf("3", "0", "<b>3</b>", "0", "&nbsp;&nbsp;&nbsp;", "3", "0", "<b>3</b>", "0")),
            // STP analog: last of four, its segment runs to end-of-document.
            themedBlock(204, "Course D / STP", listOf("4", "0", "<b>4</b>", "0", "&nbsp;&nbsp;&nbsp;", "4", "0", "<b>4</b>", "0")),
        ).joinToString("\n") + "\n<footer><table><tr><td>unrelated footer table</td></tr></table></footer>"

        val matrices = CentrePageParser.courseMatrices(fourBlocksHtml)
        assertEquals(setOf(201, 202, 203, 204), matrices.keys)
        assertEquals(3, matrices.getValue(203).total!!.newMale)
        assertEquals(4, matrices.getValue(204).total!!.newMale)

        val summaries = CentrePageParser.courseSummaries(fourBlocksHtml)
        assertEquals(setOf(201, 202, 203, 204), summaries.keys)
        assertEquals(6, summaries.getValue(203).total) // 3 + 3
        assertEquals(8, summaries.getValue(204).total) // 4 + 4
    }

    /**
     * Candidate cause from the task: does a nested <table> inside a cell
     * (tableRe is non-greedy, so it stops at the FIRST </table>) truncate
     * the segment before the real Total row? course_summary() itself never
     * nests a table in a cell (cells are plain link text), so this does not
     * reproduce the observed bug — but it is worth pinning down as a real,
     * separate latent fragility of the non-greedy `tableRe` regex.
     */
    @Test
    fun nestedTableInACellIsNotWhatCourseSummaryEmits_butWouldTruncateIfItDid() {
        val withNestedTable = """
            <div class="summary-block"><div class="table-heading"><a href="/course/91/301">Course E</a></div>
            <table><tr><th></th><th>NM</th><th>OM</th><th>Total</th><th>SM</th><th>&nbsp;</th><th>NF</th><th>OF</th><th>Total</th><th>SF</th></tr>
            <tr><td><table><tr><td>nested</td></tr></table>Confirmed</td><td><a>1</a></td><td></td><td><b><a>1</a></b></td><td></td><td>&nbsp;</td><td><a>1</a></td><td></td><td><b><a>1</a></b></td><td></td></tr>
            <tr><td><b>Total</b></td><td>1</td><td>0</td><td><b>1</b></td><td>0</td><td>&nbsp;</td><td>1</td><td>0</td><td><b>1</b></td><td>0</td></tr>
            </table></div>
        """.trimIndent()
        // Proves the failure mode exists in principle (truncated table ->
        // no Total row found -> null), while confirming it is not what the
        // real course_summary() markup produces.
        assertNull(CentrePageParser.courseSummaries(withNestedTable)[301])
    }

    /**
     * Candidate cause from the task: a row using <th> for the label, or
     * fewer than 10 <td>s, causes matrixFromTable/summaryFromTable to skip
     * every row including Total, leaving `total` null. course_summary()
     * always emits exactly 10 <td> cells per row (label + 9 data cells,
     * course.inc:684-741), so this is not what produces the real page's
     * markup either — but it is the parser's actual failure mode whenever a
     * Total row is short.
     */
    @Test
    fun totalRowWithFewerThanTenCellsIsSkippedAndSummaryStaysNull() {
        val shortTotalRow = """
            <div class="summary-block"><div class="table-heading"><a href="/course/91/302">Course F</a></div>
            <table><tr><th></th><th>NM</th><th>OM</th><th>Total</th></tr>
            <tr><td><b>Total</b></td><td>1</td><td>0</td><td><b>1</b></td></tr>
            </table></div>
        """.trimIndent()
        assertNull(CentrePageParser.courseSummaries(shortTotalRow)[302])
        assertNull(CentrePageParser.courseMatrices(shortTotalRow)[302]?.total)
    }
}
