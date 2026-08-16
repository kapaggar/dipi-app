package org.dhamma.dipi.staff.network

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

class DipiMockDispatcher : Dispatcher() {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val people = MockFixtures.people.toMutableList()

    /** applicant id → (section, room no) — mirrors the live duplicate-room refusal. */
    private val alloted = MockFixtures.allotedSeed.toMutableMap()

    override fun dispatch(request: RecordedRequest): MockResponse {
        val path = request.path.orEmpty()
        val method = request.method.orEmpty()
        return when {
            method == "POST" && path.startsWith("/api/user/login") -> {
                val body = request.body.clone().readUtf8()
                if (body.contains("\"password\":\"bad\"")) {
                    return MockResponse()
                        .setResponseCode(401)
                        .addHeader("Content-Type", "application/json")
                        .setBody("""{"msg":"Unrecognized username or password."}""")
                }
                ok(
                    json.encodeToString(
                        LoginDto(
                            sessid = "sess-demo",
                            session_name = "SESS",
                            token = "csrf-demo",
                            user = LoginUserDto(42, "sudha.user"),
                        ),
                    ),
                ).addHeader("Set-Cookie", "SESS=sess-demo; Path=/")
            }
            method == "POST" && path.startsWith("/api/user/logout") -> ok("{}")
            path.startsWith("/services/session/token") -> text("csrf-demo")
            path.startsWith("/staff/session") -> ok(json.encodeToString(MockFixtures.session))
            path.contains("/courses") && path.startsWith("/staff/centres/") ->
                ok(json.encodeToString(CourseListDto(MockFixtures.courses)))
            path.matches(Regex("/staff/courses/\\d+/applicants.*")) -> applicants(path)
            path.matches(Regex("/staff/applicants/\\d+/photo")) && method == "POST" ->
                ok(json.encodeToString(PhotoUploadResultDto(true, emptyList(), "✓ Uploaded 1 photo(s), all other fields preserved")))
            path.matches(Regex("/staff/applicants/\\d+$")) -> {
                val id = path.substringAfterLast("/").toInt()
                val p = people.firstOrNull { it.id == id }
                if (p == null) MockResponse().setResponseCode(404) else ok(json.encodeToString(p))
            }
            path.startsWith("/staff/meta/statuses") ->
                ok(
                    json.encodeToString(
                        StatusesDto(
                            ApplicantStatusChoices.map { StatusItemDto(it, it) },
                        ),
                    ),
                )
            path.matches(Regex("/staff/courses/\\d+/photo-review")) ->
                ok(json.encodeToString(PhotoReviewListDto(MockFixtures.photoReview)))
            path.matches(Regex("/centre/\\d+/acco-handler.*")) -> ok(MockFixtures.accoHandlerJson)
            method == "POST" && path.startsWith("/app-update-attended/") -> updateAttended(request, path)
            path.startsWith("/change-status/") -> changeStatus(path)
            else -> MockResponse().setResponseCode(404).setBody("""{"msg":"not mocked $path"}""")
        }
    }

    private fun applicants(path: String): MockResponse {
        val q = query(path)
        val statusFilters = q["status"]?.split(",")?.filter { it.isNotBlank() && it != "All" }.orEmpty()
        val needle = q["q"].orEmpty().lowercase()
        var rows = people.toList()
        if (statusFilters.isNotEmpty()) {
            rows = rows.filter { row -> statusFilters.any { it.equals(row.status, ignoreCase = true) } }
        }
        if (needle.isNotBlank()) {
            rows = rows.filter { row ->
                listOfNotNull(row.givenName, row.familyName, row.confNo, row.mobile, row.email)
                    .any { it.contains(needle, ignoreCase = true) }
            }
        }
        return ok(json.encodeToString(ApplicantListDto(rows, MockFixtures.counts)))
    }

    /**
     * `dh_app_update_attended` verbatim: `a=false` deletes the allocation;
     * missing section/room refuse with the live messages; a room another
     * applicant already holds refuses "Room has already been alloted".
     * Success mirrors the live shape — JSON-boolean `status` plus the HTML
     * list payloads the app must ignore. Never touches status strings.
     */
    private fun updateAttended(request: RecordedRequest, path: String): MockResponse {
        val id = path.removePrefix("/app-update-attended/").substringBefore("?").toIntOrNull()
            ?: return MockResponse().setResponseCode(404)
        val form = form(request)
        if (form["a"] == "false") {
            alloted.remove(id)
            setAttended(id, false)
            return ok(attendedReply(true, "Ok"))
        }
        val section = form["s"].orEmpty()
        val room = form["r"].orEmpty()
        if (section.isBlank()) return ok("""{"msg":"Please select room section","status":false}""")
        if (room.isBlank()) return ok("""{"status":false,"msg":"Please select room no"}""")
        if (alloted.any { (other, held) -> other != id && held == section to room }) {
            return ok("""{"status":false,"msg":"Room has already been alloted"}""")
        }
        alloted[id] = section to room
        setAttended(id, true)
        return ok(attendedReply(true, "Ok"))
    }

    private fun attendedReply(status: Boolean, msg: String): String =
        """{"msg":"$msg","status":$status,""" +
            """"applicant":"<table id=\"table-applicants\"></table>",""" +
            """"attended":"<table id=\"table-attending\"></table>","acco":{},"alloted":{}}"""

    private fun setAttended(id: Int, attended: Boolean) {
        val idx = people.indexOfFirst { it.id == id }
        if (idx >= 0) people[idx] = people[idx].copy(attended = attended)
    }

    private fun form(request: RecordedRequest): Map<String, String> {
        val body = request.body.clone().readUtf8()
        if (body.isEmpty()) return emptyMap()
        return body.split("&").mapNotNull {
            val p = it.split("=", limit = 2)
            if (p.size == 2) java.net.URLDecoder.decode(p[0], "UTF-8") to
                java.net.URLDecoder.decode(p[1], "UTF-8") else null
        }.toMap()
    }

    private fun changeStatus(path: String): MockResponse {
        val id = path.substringAfter("/change-status/").substringBefore("?").toInt()
        val q = query(path)
        val s = q["s"].orEmpty()
        if (id == MockFixtures.RAKESH_ID) {
            return ok(
                json.encodeToString(
                    ChangeStatusDto(
                        status = "Failed",
                        msg = "Please Edit application and choose Area teacher before approving!",
                    ),
                ),
            )
        }
        val minted = if (s.equals("Confirmed", true)) "NF129" else ""
        val idx = people.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val cur = people[idx]
            people[idx] = cur.copy(status = s, confNo = minted.ifBlank { cur.confNo })
        }
        return ok(
            json.encodeToString(
                ChangeStatusDto(
                    status = "OK",
                    msg = "",
                    confno = minted,
                    newstatus = s,
                ),
            ),
        )
    }

    private fun query(path: String): Map<String, String> {
        val q = path.substringAfter("?", "")
        if (q.isEmpty()) return emptyMap()
        return q.split("&").mapNotNull {
            val p = it.split("=", limit = 2)
            if (p.size == 2) java.net.URLDecoder.decode(p[0], "UTF-8") to
                java.net.URLDecoder.decode(p[1], "UTF-8") else null
        }.toMap()
    }

    private fun ok(body: String) = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "application/json")
        .setBody(body)

    private fun text(body: String) = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "text/plain")
        .setBody(body)

    companion object {
        val ApplicantStatusChoices = listOf(
            "Pending", "Received", "Confirmed", "Expected",
            "Reconfirmation", "Cancelled", "Rejected", "Custom…",
        )
    }
}
