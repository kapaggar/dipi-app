package org.dhamma.dipi.staff.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.RoomFeature

/**
 * Reads the centre's room config from `GET /centre/{cid}/acco-handler` —
 * the DataTables Editor source behind the Accommodation Settings table on
 * `/centre/{cid}/edit` (which itself renders only an empty
 * `<table id="centre-acco-table">` skeleton; the data always travels as this
 * JSON). One row per `dh_center_setting_acco` record:
 *
 * ```json
 * {"data":[{"DT_RowId":"row_7","dh_center_setting_acco":{
 *   "csa_gender":"M","csa_section":"Mbk","csa_room":"1:40","csa_deleted":"0"}}]}
 * ```
 *
 * `csa_room` is a comma-separated token list, mirrored from
 * `_ajax_get_acco_options()` in dh_manageapp: `a:b` expands to the numeric
 * range a..b (PHP casts a trailing non-digit suffix on the end away), any
 * other token is a literal room number. A trailing `W` / `IC` / `G` mark on a
 * token maps to the paper chart's amenity bands (Western toilet / Indian
 * commode / geyser) — display-only, the room number keeps just the digits.
 *
 * Room code = "<section> <number>", the Section + Room No pair the desk's
 * Update dialog works in. Read-only: the app never posts to this handler.
 */
object AccoHandlerParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private const val TABLE = "dh_center_setting_acco"

    /** Runaway-range guard: no centre block has anywhere near this many rooms. */
    private const val MAX_RANGE = 500

    private val markedToken = Regex("""^(\d+)\s*((?:IC|W|G)+)$""", RegexOption.IGNORE_CASE)
    private val leadingDigits = Regex("""^\d+""")

    /**
     * Null when [body] is not an Editor payload at all (HTML, error JSON) —
     * callers keep their cache then. An empty list is a real answer: the
     * centre has no rooms configured.
     */
    fun roomsOrNull(body: String): List<AccoRoom>? {
        val root = runCatching { json.parseToJsonElement(body) }.getOrNull() as? JsonObject
            ?: return null
        val data = root["data"] as? JsonArray ?: return null
        val out = LinkedHashMap<String, AccoRoom>()
        for (el in data) {
            val obj = el as? JsonObject ?: continue
            // Editor nests fields under the table name; tolerate a flat row too.
            val row = (obj[TABLE] as? JsonObject) ?: obj
            if (str(row, "csa_deleted") == "1") continue
            val section = str(row, "csa_section").trim()
            val gender = when (str(row, "csa_gender").trim().uppercase()) {
                "F" -> Gender.F
                "M" -> Gender.M
                else -> if (section.uppercase().startsWith("F")) Gender.F else Gender.M
            }
            expand(str(row, "csa_room")).forEach { (number, features) ->
                val code = if (section.isBlank()) number else "$section $number"
                out.putIfAbsent(code, AccoRoom(code, gender, section, features, number = number))
            }
        }
        return out.values.toList()
    }

    private fun expand(raw: String): List<Pair<String, RoomFeature>> {
        val out = mutableListOf<Pair<String, RoomFeature>>()
        raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { token ->
            val parts = token.split(':')
            if (parts.size == 2) {
                val start = leadingDigits.find(parts[0].trim())?.value?.toIntOrNull()
                val endToken = parts[1].trim()
                val end = leadingDigits.find(endToken)?.value?.toIntOrNull()
                // A mark on the range end ("1:38W") applies to the whole band,
                // like the shaded W/IC rows on the paper chart.
                val features = featuresOf(markedToken.find(endToken)?.groupValues?.get(2).orEmpty())
                if (start != null && end != null && end >= start) {
                    (start..minOf(end, start + MAX_RANGE)).forEach { out += "$it" to features }
                }
            } else {
                val marked = markedToken.find(token)
                if (marked != null) {
                    out += marked.groupValues[1] to featuresOf(marked.groupValues[2])
                } else {
                    out += token to RoomFeature()
                }
            }
        }
        return out
    }

    private fun featuresOf(marks: String): RoomFeature {
        val up = marks.uppercase()
        return RoomFeature(
            geyser = up.contains("G"),
            indianToilet = up.contains("IC"),
            westernToilet = up.contains("W"),
        )
    }

    private fun str(row: JsonObject, key: String): String =
        (row[key] as? JsonPrimitive)?.content.orEmpty()
}
