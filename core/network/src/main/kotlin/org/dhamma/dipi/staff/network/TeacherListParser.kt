package org.dhamma.dipi.staff.network

import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.RollGroup
import org.dhamma.dipi.staff.model.RollRow
import org.dhamma.dipi.staff.model.RollSeniority
import org.dhamma.dipi.staff.model.SeatKind
import org.dhamma.dipi.staff.model.TeacherRoll

/**
 * `GET /teacher-list/{cid}/{courseId}` (`dh_generate_teacher_list`,
 * dh_manageapp/inc/zero-day.inc:877-1072). The response is an UNTHEMED
 * fragment starting `<style>` — no `<html>`/`<body>` wrapper — holding one
 * `<table class="table-teacher-list">` per (gender, old/new, group) block.
 *
 * Twelve `<td>` per row in server order: S/N, Student, Room, Age, City,
 * Courses, Cell, Seat, Occupation, Education, Languages, Comments.
 *
 * NPI discipline: cell 12 (Comments) is an unlabelled concatenation of
 * health disclosures (pregnancy, physical/mental, addiction, medication,
 * other techniques — zero-day.inc:1011-1023). It is NEVER read, parsed or
 * stored — the loop below stops at index 10 and [RollRow] has no field for
 * it by construction. Old/New comes ONLY from the `tl-groupinfo` band;
 * S/N restarts per block; blanks (incl. em-dashes) pass through verbatim.
 */
object TeacherListParser {

    private val tableRe = Regex(
        """<table[^>]*\bclass\s*=\s*["'][^"']*table-teacher-list[^"']*["'][^>]*>(.*?)</table>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val bandRe = Regex(
        """<th[^>]*\bclass\s*=\s*["'][^"']*tl-groupinfo[^"']*["'][^>]*>(.*?)</th>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val tbodyRe = Regex(
        """<tbody[^>]*>(.*?)</tbody>""",
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

    /** `AT: {name} [{code}]` — code absent on the literal `(unassigned)`. */
    private val atRe = Regex("""^AT:\s*(.*?)(?:\s*\[([^\]]*)\])?$""")

    /** Name suffixes the renderer bolds: `(Sevak)`, `(BT-2001)`, `(T…)`, `(SAT-2011)`, `(AT)`. */
    private val roleSuffixRe = Regex(
        """<b>\s*\((Sevak|(?:BT|SAT|AT|T)(?:-\d+)?)\)\s*</b>""",
        RegexOption.IGNORE_CASE,
    )

    /** `<b>KEY:</b>N` pairs — keys only from the renderer's own eight (zero-day.inc:1002). */
    private val courseRe = Regex(
        """<b>(10D|STP|SPL|TSC|20D|30D|45D|60D):</b>\s*(\d+)""",
    )

    /** `<span class="tl-br" title="Backrest">BR</span>` on the seat cell. */
    private val backrestRe = Regex(
        """<span[^>]*\bclass\s*=\s*["'][^"']*tl-br[^"']*["'][^>]*>.*?</span>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    fun parse(html: String): TeacherRoll {
        val groups = mutableListOf<RollGroup>()
        for (table in tableRe.findAll(html)) {
            val block = table.groupValues[1]
            val band = parseBand(block) ?: continue
            val body = tbodyRe.find(block)?.groupValues?.get(1) ?: continue
            val rows = mutableListOf<RollRow>()
            for (row in rowRe.findAll(body)) {
                rows += parseRow(row.groupValues[1]) ?: continue
            }
            groups += band.copy(rows = rows)
        }
        return TeacherRoll(groups)
    }

    /**
     * Band text pipe-separated after tag-stripping:
     * `AT: {name} [{code}] | Male | Old | Group {g} | {N} total`.
     */
    private fun parseBand(block: String): RollGroup? {
        val raw = bandRe.find(block)?.groupValues?.get(1) ?: return null
        val tokens = SearchPageParser.stripTags(raw).split("|").map { it.trim() }
        if (tokens.size < 5) return null
        val at = atRe.find(tokens[0]) ?: return null
        val code = at.groupValues[2].ifBlank { null }
        return RollGroup(
            at = at.groupValues[1].trim(),
            code = code,
            gender = if (tokens[1].equals("Female", ignoreCase = true)) Gender.F else Gender.M,
            seniority = if (tokens[2].equals("Old", ignoreCase = true)) RollSeniority.OLD else RollSeniority.NEW,
            group = tokens[3].removePrefix("Group").trim(),
            total = tokens[4].removeSuffix("total").trim().toIntOrNull() ?: 0,
            rows = emptyList(),
        )
    }

    private fun parseRow(rowHtml: String): RollRow? {
        val cells = cellRe.findAll(rowHtml).map { it.groupValues[1] }.toList()
        // Arity guard: the renderer always emits 12 cells; anything shorter
        // is not a student row. Cells past index 10 are never touched.
        if (cells.size < 12) return null
        val sn = SearchPageParser.stripTags(cells[0]).toIntOrNull() ?: return null
        val (name, roleTag) = parseStudent(cells[1])
        val (seat, seatKind, backrest) = parseSeat(cells[7])
        return RollRow(
            sn = sn,
            applicantId = null, // no id attribute anywhere in the row markup
            name = name,
            roleTag = roleTag,
            room = SearchPageParser.stripTags(cells[2]),
            age = SearchPageParser.stripTags(cells[3]),
            city = SearchPageParser.stripTags(cells[4]),
            courses = courseRe.findAll(cells[5])
                .map { it.groupValues[1] to it.groupValues[2].toInt() }
                .toList(),
            cell = SearchPageParser.stripTags(cells[6]),
            seat = seat,
            seatKind = seatKind,
            backrest = backrest,
            occupation = SearchPageParser.stripTags(cells[8]),
            education = SearchPageParser.stripTags(cells[9]),
            languages = SearchPageParser.stripTags(cells[10]),
        )
    }

    /** Title-cased name plus optional bolded `(SUFFIX)` tokens → [RollRow.roleTag]. */
    private fun parseStudent(cellHtml: String): Pair<String, String?> {
        val tags = roleSuffixRe.findAll(cellHtml)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotEmpty() }
            .toList()
        val name = cleanPersonName(SearchPageParser.stripTags(roleSuffixRe.replace(cellHtml, " ")))
        return name to (tags.joinToString(" · ").ifBlank { null })
    }

    private fun parseSeat(cellHtml: String): Triple<String, SeatKind, Boolean> {
        val backrest = backrestRe.containsMatchIn(cellHtml)
        val seat = SearchPageParser.stripTags(backrestRe.replace(cellHtml, " "))
        val kind = when {
            seat.startsWith("CW-", ignoreCase = true) -> SeatKind.CELL
            seat.startsWith("CH-", ignoreCase = true) -> SeatKind.CHAIR
            else -> SeatKind.FLOOR
        }
        return Triple(seat, kind, backrest)
    }
}
