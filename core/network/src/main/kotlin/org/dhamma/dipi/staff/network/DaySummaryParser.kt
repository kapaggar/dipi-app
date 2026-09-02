package org.dhamma.dipi.staff.network

import org.dhamma.dipi.staff.model.DaySummary
import org.dhamma.dipi.staff.model.OldNew
import org.dhamma.dipi.staff.model.RollMatrix
import org.dhamma.dipi.staff.model.DayRollRow
import org.dhamma.dipi.staff.model.SpecialRow
import org.dhamma.dipi.staff.model.SpecialSeating

/**
 * Parses the `#day-summary` fragment of `GET /zero-day/{cid}/{courseId}` into
 * [DaySummary] (v5 T2, frame `5d`).
 *
 * This is the pass's only new parser, and it reads a page the app already
 * fetches: no new endpoint, no new permission, no new query parameter. It
 * exists because the fragment arrives with **no stylesheet**, so today it
 * renders as browser-default HTML in the sheet viewer — the one Board cell
 * that is a handful of numbers, drawn as an unstyled table.
 *
 * Three fixed tables, catalogued in `version-5/HAR-ROUTES.md` § `zero-day`:
 *
 * | Table | Columns | Rows |
 * |---|---|---|
 * | `#table-conf` | Old · New · Total · Server | Confirmed Male / Female / Total |
 * | `#table-totals` | Old · New · Total · Server | Attended Male / Female / Total |
 * | `#table-special` | Chowky · Chair · Backrest | Male / Female / Total |
 *
 * **The `Total` cells rely on an unclosed `<b>` tag.** The desk emits
 * `<td><b>81</td>` and the browser recovers; a naive tag-matching parse would
 * swallow the rest of the row. This parser never matches `</b>` — it strips
 * every tag out of a cell and reads the text — so the malformation is inert.
 *
 * A missing table yields zeros, never an exception: on day −1 the real page
 * is all zeros too, and a blank screen would be indistinguishable from a
 * broken one.
 */
object DaySummaryParser {

    fun parse(html: String): DaySummary = DaySummary(
        confirmed = rollMatrix(tableById(html, "table-conf")),
        attended = rollMatrix(tableById(html, "table-totals")),
        specialSeating = specialSeating(tableById(html, "table-special")),
    )

    /* ── Roll matrices ────────────────────────────────────────────────── */

    private fun rollMatrix(table: String?): RollMatrix {
        if (table == null) return RollMatrix()
        val rows = dataRows(table)
        return RollMatrix(
            male = rollRow(rows.firstOrNull { it.label.contains("male", ignoreCase = true) && !it.label.contains("female", ignoreCase = true) }),
            female = rollRow(rows.firstOrNull { it.label.contains("female", ignoreCase = true) }),
            total = rollRow(rows.firstOrNull { it.label.trim().equals("total", ignoreCase = true) }),
        )
    }

    private fun rollRow(row: Row?): DayRollRow {
        if (row == null) return DayRollRow()
        return DayRollRow(
            old = row.cells.getOrNull(0).toCount(),
            new = row.cells.getOrNull(1).toCount(),
            total = row.cells.getOrNull(2).toCount(),
            server = row.cells.getOrNull(3).toCount(),
        )
    }

    /* ── Special seating ──────────────────────────────────────────────── */

    private fun specialSeating(table: String?): SpecialSeating {
        if (table == null) return SpecialSeating()
        val rows = dataRows(table)
        return SpecialSeating(
            male = specialRow(rows.firstOrNull { it.label.contains("male", ignoreCase = true) && !it.label.contains("female", ignoreCase = true) }),
            female = specialRow(rows.firstOrNull { it.label.contains("female", ignoreCase = true) }),
            total = specialRow(rows.firstOrNull { it.label.trim().equals("total", ignoreCase = true) }),
        )
    }

    private fun specialRow(row: Row?): SpecialRow {
        if (row == null) return SpecialRow()
        return SpecialRow(
            chowky = oldNew(row.cells.getOrNull(0)),
            chair = oldNew(row.cells.getOrNull(1)),
            backrest = oldNew(row.cells.getOrNull(2)),
        )
    }

    /**
     * `1 (O) + 1 (N)` → `OldNew(1, 1)`. A bare number with no marker is read
     * as old-plus-new unknown, so it lands in [OldNew.old] rather than being
     * silently dropped: the hall team needs the count either way.
     */
    internal fun oldNew(cell: String?): OldNew {
        val text = cell?.trim().orEmpty()
        if (text.isBlank()) return OldNew()
        val marked = OLD_NEW.findAll(text).toList()
        if (marked.isEmpty()) return OldNew(old = text.toCount())
        var old = 0
        var new = 0
        marked.forEach { m ->
            val n = m.groupValues[1].toIntOrNull() ?: 0
            if (m.groupValues[2].equals("O", ignoreCase = true)) old += n else new += n
        }
        return OldNew(old, new)
    }

    private val OLD_NEW = Regex("""(\d+)\s*\(\s*([ON])\s*\)""", RegexOption.IGNORE_CASE)

    /* ── HTML plumbing ────────────────────────────────────────────────── */

    private data class Row(val label: String, val cells: List<String>)

    /**
     * Rows that carry data: a leading label cell plus at least one figure.
     * Header rows (all `<th>`, or no numeric content) fall out naturally
     * because their label never matches male / female / total on a desk page
     * whose header row reads `Old New Total Server`.
     */
    private fun dataRows(table: String): List<Row> =
        Regex("""<tr\b[^>]*>(.*?)</tr>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .findAll(table)
            .map { tr -> cellsOf(tr.groupValues[1]) }
            .filter { it.size >= 2 }
            .map { Row(label = it.first(), cells = it.drop(1)) }
            .toList()

    /**
     * Every `<td>`/`<th>` in a row, as plain text.
     *
     * The close tag is matched lazily against *either* `</td>`/`</th>` or the
     * next opening cell or row end, so the desk's unclosed `<b>` — and an
     * unclosed `<td>`, which the same template style produces — cannot make
     * one cell eat its neighbours.
     */
    private fun cellsOf(rowHtml: String): List<String> =
        Regex(
            """<(td|th)\b[^>]*>(.*?)(?=</\1>|<t[dh]\b|</tr>|$)""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
            .findAll(rowHtml)
            .map { it.groupValues[2].stripTagsAndEntities() }
            .toList()

    private fun tableById(html: String, id: String): String? = extractElementById(html, id)

    private fun String.stripTagsAndEntities(): String = this
        .replace(Regex("""<[^>]*>"""), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace(Regex("""\s+"""), " ")
        .trim()

    /** First integer in the cell, or 0 — an empty cell is a real zero here. */
    private fun String?.toCount(): Int =
        this?.let { Regex("""-?\d+""").find(it)?.value?.toIntOrNull() } ?: 0
}
