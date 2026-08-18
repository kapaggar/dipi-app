package org.dhamma.dipi.staff.network

import org.dhamma.dipi.staff.model.LetterRow

/**
 * `#table-letters` on `GET /letters/{cid}` (`letters.inc:70`).
 * Columns: Letter Name, Status, Course Type, Subject, then Edit/Copy/Delete
 * action links — those are dropped. Body is not in the listing; a
 * `.letter-body` / `<pre>` block is kept if the page happens to include one.
 */
object LettersParser {
    const val TABLE_ID = "table-letters"

    fun letters(html: String): List<LetterRow> {
        val pageBody = pageBody(html)
        return HtmlTables.rowsById(html, TABLE_ID).map { cells ->
            LetterRow(
                name = cells.getOrElse(0) { "" },
                status = cells.getOrElse(1) { "" },
                courseType = cells.getOrElse(2) { "" }.ifBlank { "All" },
                subject = cells.getOrElse(3) { "" },
                body = pageBody,
            )
        }
    }

    private fun pageBody(html: String): String {
        val pre = Regex(
            """<(?:pre|div)\b[^>]*class=["'][^"']*letter-body[^"']*["'][^>]*>(.*?)</(?:pre|div)>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).find(html)?.groupValues?.get(1)
        return pre?.let { SearchPageParser.stripTags(it) }.orEmpty()
    }
}
