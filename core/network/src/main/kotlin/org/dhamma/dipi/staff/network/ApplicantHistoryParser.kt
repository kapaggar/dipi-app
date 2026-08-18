package org.dhamma.dipi.staff.network

import org.dhamma.dipi.staff.model.ApplicantActivityRow
import org.dhamma.dipi.staff.model.ApplicantClarificationRow
import org.dhamma.dipi.staff.model.ApplicantCourseRow

/**
 * HTML fragments loaded into the desk worklist expander:
 * `/app-courses/{id}` (`_search_student`), `/app-activity/{id}` (`_get_activity`),
 * `/app-clarifications/{id}` (`_get_clarifications`). In-memory only.
 */
object ApplicantHistoryParser {
    private val CLAR_HREF = Regex("""show-clarification/(\d+)/(\d+)""")

    fun courses(html: String): List<ApplicantCourseRow> =
        HtmlTables.firstTableRows(html).map { cells ->
            ApplicantCourseRow(
                course = cells.getOrElse(0) { "" },
                type = cells.getOrElse(1) { "" },
                status = cells.getOrElse(2) { "" },
                attended = cells.getOrElse(3) { "" },
                address = cells.getOrElse(4) { "" },
            )
        }

    fun activity(html: String): List<ApplicantActivityRow> =
        HtmlTables.firstTableRows(html).map { cells ->
            ApplicantActivityRow(
                at = cells.getOrElse(0) { "" },
                activity = cells.getOrElse(1) { "" },
                user = cells.getOrElse(2) { "" },
            )
        }

    fun clarifications(html: String): List<ApplicantClarificationRow> {
        val table = firstTable(html) ?: return emptyList()
        val body = Regex(
            """<tbody\b[^>]*>(.*?)</tbody>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).find(table)?.groupValues?.get(1) ?: table
        return Regex(
            """<tr\b[^>]*>(.*?)</tr>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).findAll(body).mapNotNull { tr ->
            val raw = Regex(
                """<td\b[^>]*>(.*?)</td>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            ).findAll(tr.groupValues[1]).map { it.groupValues[1] }.toList()
            if (raw.isEmpty()) return@mapNotNull null
            val fileCell = raw.getOrElse(2) { "" }
            val clarId = CLAR_HREF.find(fileCell)?.groupValues?.get(2)?.toIntOrNull()
            ApplicantClarificationRow(
                at = SearchPageParser.stripTags(raw.getOrElse(0) { "" }),
                message = SearchPageParser.stripTags(raw.getOrElse(1) { "" }),
                fileLabel = SearchPageParser.stripTags(fileCell),
                clarId = clarId,
            )
        }.toList()
    }

    private fun firstTable(html: String): String? {
        val open = Regex("""<table\b[^>]*>""", RegexOption.IGNORE_CASE).find(html) ?: return null
        val start = open.range.first
        val end = html.indexOf("</table>", start, ignoreCase = true)
        if (end < 0) return null
        return html.substring(start, end + "</table>".length)
    }
}
