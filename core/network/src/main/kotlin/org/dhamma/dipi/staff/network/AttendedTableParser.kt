package org.dhamma.dipi.staff.network

import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.model.RoomAllocSync

/**
 * `#table-attending` on `GET /zero-day/{cid}/{courseId}` (`dh_manageapp_attended`).
 * Reads only `a_id` + allocation cells (room, seat, group, laundry/valuable
 * flags). Names and the hidden comment column are never stored.
 */
object AttendedTableParser {

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

    fun parse(html: String): Map<ApplicantId, CheckInRecord> {
        val table = extractElementById(html, "table-attending") ?: return emptyMap()
        val body = tbodyRe.find(table)?.groupValues?.get(1) ?: table
        val out = linkedMapOf<ApplicantId, CheckInRecord>()
        for (row in rowRe.findAll(body)) {
            val cells = cellRe.findAll(row.groupValues[1])
                .map { SearchPageParser.stripTags(it.groupValues[1]) }
                .toList()
            if (cells.size < 15) continue
            val id = cells[0].toIntOrNull() ?: continue
            out[ApplicantId(id)] = CheckInRecord(
                checkedIn = true,
                room = RoomAllocSync.parseDeskRoom(cells[8]),
                seat = seat(cells[11], cells[12], cells[13]),
                valuables = tokenFlag(cells[10]),
                laundry = tokenFlag(cells[9]),
                group = cells[14].ifBlank { "1" },
                synced = true,
            )
        }
        return out
    }

    /** Desk laundry/valuable cells are token numbers, not booleans. */
    private fun tokenFlag(raw: String): Boolean {
        val t = raw.trim()
        return t.isNotEmpty() && t != "0"
    }

    private fun seat(chowky: String, chair: String, backrest: String): String = when {
        isYes(chowky) -> "Chowky"
        isYes(chair) -> "Chair"
        isYes(backrest) -> "Backrest"
        else -> "None"
    }

    private fun isYes(raw: String): Boolean = raw.equals("Yes", ignoreCase = true)
}
