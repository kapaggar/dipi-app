package org.dhamma.dipi.staff.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.dhamma.dipi.staff.model.ManagedCourse

/**
 * DataTables Editor GET of `/course/handler/{cid}` (`course.inc:377`).
 * Fields nest under `dh_course` / `ctype`; a flat row is tolerated.
 * Read-only — the app never POSTs create/edit/finalize.
 */
object CourseHandlerParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private const val TABLE = "dh_course"

    fun courses(body: String): List<ManagedCourse> = coursesOrNull(body).orEmpty()

    fun coursesOrNull(body: String): List<ManagedCourse>? {
        val root = runCatching { json.parseToJsonElement(body) }.getOrNull() as? JsonObject
            ?: return null
        val data = root["data"] as? JsonArray ?: return null
        return data.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val row = (obj[TABLE] as? JsonObject) ?: obj
            val type = ((obj["ctype"] as? JsonObject)?.let { str(it, "td_val1") })
                .orEmpty()
                .ifBlank { str(row, "c_name") }
            val id = str(row, "c_id").toIntOrNull() ?: return@mapNotNull null
            ManagedCourse(
                id = id,
                type = type,
                start = str(row, "c_start"),
                end = str(row, "c_end"),
                cancelled = flag(row, "c_cancelled"),
                status = str(row, "c_status"),
                statusNm = str(row, "c_status_nm"),
                statusOm = str(row, "c_status_om"),
                statusNf = str(row, "c_status_nf"),
                statusOf = str(row, "c_status_of"),
                statusSvrM = str(row, "c_status_svr_m"),
                statusSvrF = str(row, "c_status_svr_f"),
                comments = str(row, "c_comments"),
                description = str(row, "c_description"),
                finalized = flag(row, "c_finalized"),
            )
        }
    }

    private fun flag(row: JsonObject, key: String): Boolean =
        str(row, key) in listOf("1", "true", "Yes")

    private fun str(row: JsonObject, key: String): String {
        val v = row[key] ?: return ""
        if (v is JsonNull) return ""
        return (v as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
    }
}
