package org.dhamma.dipi.staff.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.dhamma.dipi.staff.model.SmsCourseRow
import org.dhamma.dipi.staff.model.SmsLetterRow

/**
 * SMS report (`reports.inc:5`): `#table-applicants` is an empty DataTables
 * skeleton — the rows live in `var dataset` as `{cid, course, count}`.
 * Expand fragment `GET /sms-count/{courseId}` (`:2310`) is a headered table
 * of letter id / name / count (no table id).
 */
object SmsReportParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun courses(html: String): List<SmsCourseRow> {
        val raw = SearchPageParser.extractJsonArray(html, "dataset") ?: return emptyList()
        val arr = json.parseToJsonElement(raw) as? JsonArray ?: return emptyList()
        return arr.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val id = o.int("cid") ?: return@mapNotNull null
            SmsCourseRow(
                courseId = id,
                course = o.str("course").orEmpty(),
                count = o.int("count") ?: 0,
            )
        }
    }

    fun letters(html: String): List<SmsLetterRow> =
        HtmlTables.firstTableRows(html).map { cells ->
            SmsLetterRow(
                letterId = cells.getOrElse(0) { "" },
                name = cells.getOrElse(1) { "" },
                count = cells.getOrElse(2) { "" },
            )
        }

    private fun JsonObject.str(key: String): String? {
        val v = this[key] ?: return null
        if (v is JsonNull) return null
        val s = (v as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
        return s.takeIf { it.isNotEmpty() && it != "null" }
    }

    private fun JsonObject.int(key: String): Int? {
        val v = this[key] ?: return null
        if (v is JsonNull) return null
        return (v as? JsonPrimitive)?.content?.toIntOrNull()
    }
}
