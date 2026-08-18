package org.dhamma.dipi.staff.network

import org.dhamma.dipi.staff.model.CourseSummary

/**
 * Reads the per-course status tables on the centre dashboard (`GET /centre/{cid}`,
 * rendered by dh_manageapp `course_summary()`). Each upcoming course is a
 * `<div class="summary-block">` holding a `table-heading` link to
 * `/course/{cid}/{courseId}` and a table with the columns
 * `status · NM · OM · Total · SM · (spacer) · NF · OF · Total · SF`;
 * the last row is `<b>Total</b>`. Per-status "Total" cells count students only —
 * sevaks sit in the SM/SF columns. Only aggregate counts are read here, never
 * applicant rows, so no NPI can pass through this parser.
 */
object CentrePageParser {

    private val headingRe = Regex(
        """class=["']table-heading["'][^>]*>\s*<a[^>]+href=["'][^"']*/course/\d+/(\d+)[^"']*["']""",
        RegexOption.IGNORE_CASE,
    )
    private val tableRe = Regex(
        """<table[^>]*>(.*?)</table>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val rowRe = Regex(
        """<tr[^>]*>(.*?)</tr>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val cellRe = Regex(
        """<td[^>]*>(.*?)</td>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    /**
     * Courses in the dashboard's Select Course dropdown that started before
     * the upcoming block (`dh_zero_select_course`: `c_start >= now - 6 months`,
     * ordered by start). Upcoming IDs (the next 4 with `c_start >= today`)
     * are excluded. Newest-first so a course that rolled off at midnight
     * sits at the top.
     */
    fun olderCourseOptions(html: String, upcomingIds: Set<Int>): List<SelectOption> {
        val all = SearchPageParser.selectOptions(html, "edit-course")
        if (all.isEmpty()) return emptyList()
        val older = if (upcomingIds.isEmpty()) {
            all
        } else {
            val firstUpcoming = all.indexOfFirst { it.id in upcomingIds }
            if (firstUpcoming < 0) all.filter { it.id !in upcomingIds } else all.take(firstUpcoming)
        }
        return older.asReversed()
    }

    /** Status-table counts keyed by course id; courses without a table are absent. */
    fun courseSummaries(html: String): Map<Int, CourseSummary> {
        val headings = headingRe.findAll(html).toList()
        val out = linkedMapOf<Int, CourseSummary>()
        headings.forEachIndexed { i, heading ->
            val courseId = heading.groupValues[1].toIntOrNull() ?: return@forEachIndexed
            val segment = html.substring(
                heading.range.last + 1,
                headings.getOrNull(i + 1)?.range?.first ?: html.length,
            )
            val table = tableRe.find(segment)?.groupValues?.get(1) ?: return@forEachIndexed
            summaryFromTable(table)?.let { out[courseId] = it }
        }
        return out
    }

    private fun summaryFromTable(table: String): CourseSummary? {
        var received = 0
        var confirmed = 0
        var expected = 0
        var cancelled = 0
        var total: Int? = null
        rowRe.findAll(table).forEach { row ->
            // The header row is all <th>, so it yields no cells and drops out here.
            val cells = cellRe.findAll(row.groupValues[1]).map { it.groupValues[1] }.toList()
            if (cells.size < 10) return@forEach
            val label = SearchPageParser.stripTags(cells[0])
            val students = num(cells[3]) + num(cells[8])
            when {
                // Grand total = male + female student totals plus the SM/SF
                // sevak cells of the same Total row.
                label.equals("Total", true) -> total = students + num(cells[4]) + num(cells[9])
                label.equals("Received", true) -> received = students
                label.equals("Confirmed", true) -> confirmed = students
                label.equals("Expected", true) -> expected = students
                label.equals("Cancelled", true) -> cancelled = students
            }
        }
        val grand = total ?: return null
        return CourseSummary(received, confirmed, expected, cancelled, grand)
    }

    /** Count cells are blank, `N`, `<a>N</a>` or `<b><a>N</a></b>` — links carry ids in the href only. */
    private fun num(cell: String): Int =
        SearchPageParser.stripTags(cell).filter { it.isDigit() }.toIntOrNull() ?: 0
}
