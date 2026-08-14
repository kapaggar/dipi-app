package org.dhamma.dipi.staff.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.dhamma.dipi.staff.model.ApplicantType

data class FormTokens(
    val formBuildId: String,
    val formToken: String,
    val formId: String,
)

data class SelectOption(val id: Int, val label: String)

data class LoginBlock(
    val formBuildId: String,
    val formId: String,
    val action: String,
)

data class SearchPage(
    val tokens: FormTokens?,
    val centres: List<SelectOption>,
    val courses: List<SelectOption>,
    val statuses: List<String>,
    val dataset: List<ApplicantDto>,
    val pathCentreId: Int?,
)

object SearchPageParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(html: String, pathCentreId: Int? = null, photoHost: String = ""): SearchPage {
        return SearchPage(
            tokens = tokens(html),
            centres = selectOptions(html, "edit-centre") + selectOptions(html, "edit-center"),
            courses = selectOptions(html, "edit-course"),
            statuses = selectOptionsRaw(html, "edit-app-status")
                .map { it.second }
                .filter { it.isNotBlank() && !it.equals("Choose", true) },
            dataset = dataset(html, photoHost),
            pathCentreId = pathCentreId,
        )
    }

    fun loginBlock(html: String): LoginBlock? {
        val build = namedValue(html, "form_build_id") ?: return null
        val id = namedValue(html, "form_id") ?: "user_login_block"
        val action = loginFormAction(html)
            ?: if (id == "user_login") "/user/login" else "/home?destination=home"
        return LoginBlock(build, id, action)
    }

    fun loginFormAction(html: String): String? {
        val tags = Regex("""<form\b[^>]*>""", RegexOption.IGNORE_CASE).findAll(html)
        for (tag in tags) {
            val open = tag.value
            if (!open.contains("login", ignoreCase = true)) continue
            val action = Regex("""action=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                .find(open)?.groupValues?.get(1)
            if (!action.isNullOrBlank()) return action.replace("&amp;", "&")
        }
        return null
    }

    fun loginError(html: String): String? {
        val m = Regex(
            """(?:messages?\s+error|alert-danger|error["'])[^>]*>(.*?)</""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).find(html)
        val t = m?.groupValues?.get(1)?.let { stripTags(it) }
        if (!t.isNullOrBlank() && t.length < 240) return t
        return if (html.contains("unrecognized username or password", true)) {
            "Sorry, unrecognized username or password."
        } else {
            null
        }
    }

    fun centreName(html: String): String? {
        val h1 = Regex("""<h1[^>]*>(.*?)</h1>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)?.groupValues?.get(1)?.let { stripTags(it) }
        if (!h1.isNullOrBlank()) return h1.removePrefix("Manage ").trim()
        val title = Regex("""<title>([^<]+)</title>""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)?.substringBefore("|")?.trim()
        return title?.removePrefix("Manage ")?.trim()?.takeIf { it.isNotBlank() }
    }

    fun coursesFromDashboard(html: String): List<SelectOption> {
        val found = linkedMapOf<Int, String>()
        val heading = Regex(
            """class=["']table-heading["'][^>]*>\s*<a[^>]+href=["'][^"']*/course/(\d+)/(\d+)[^"']*["'][^>]*>(.*?)</a>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        heading.findAll(html).forEach { m ->
            val id = m.groupValues[2].toIntOrNull() ?: return@forEach
            found[id] = stripTags(m.groupValues[3])
        }
        if (found.isEmpty()) {
            Regex("""/course/(\d+)/(\d+)""").findAll(html).forEach { m ->
                val id = m.groupValues[2].toIntOrNull() ?: return@forEach
                found.putIfAbsent(id, "Course $id")
            }
        }
        return found.map { SelectOption(it.key, it.value) }
    }

    fun tokens(html: String): FormTokens? {
        val build = namedValue(html, "form_build_id") ?: return null
        val token = namedValue(html, "form_token") ?: return null
        val id = namedValue(html, "form_id") ?: "dh_manageapp_search_form"
        return FormTokens(build, token, id)
    }

    fun namedValue(html: String, name: String): String? {
        val re = Regex(
            """name=["']$name["'][^>]*value=["']([^"']+)["']|value=["']([^"']+)["'][^>]*name=["']$name["']""",
            RegexOption.IGNORE_CASE,
        )
        val m = re.find(html) ?: return null
        return m.groupValues[1].ifBlank { m.groupValues[2] }.ifBlank { null }
    }

    fun selectOptions(html: String, selectId: String): List<SelectOption> =
        selectOptionsRaw(html, selectId).mapNotNull { (v, label) ->
            val id = v.toIntOrNull() ?: return@mapNotNull null
            if (id <= 0) return@mapNotNull null
            SelectOption(id, label.ifBlank { v })
        }

    fun selectOptionsRaw(html: String, selectId: String): List<Pair<String, String>> {
        val blockRe = Regex(
            """<select[^>]*(?:id|name)=["'][^"']*$selectId[^"']*["'][^>]*>(.*?)</select>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        val block = blockRe.find(html)?.groupValues?.get(1) ?: return emptyList()
        return Regex(
            """<option[^>]*value=["']([^"']*)["'][^>]*>(.*?)</option>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).findAll(block).map { m ->
            m.groupValues[1] to stripTags(m.groupValues[2]).trim()
        }.toList()
    }

    fun extractJsonArray(html: String, varName: String): String? {
        val key = "var $varName = "
        val i = html.indexOf(key)
        if (i < 0) return null
        val start = html.indexOf('[', i)
        if (start < 0) return null
        var depth = 0
        var inStr = false
        var quote = ' '
        var escape = false
        for (p in start until html.length) {
            val ch = html[p]
            if (inStr) {
                when {
                    escape -> escape = false
                    ch == '\\' -> escape = true
                    ch == quote -> inStr = false
                }
            } else {
                when (ch) {
                    '"', '\'' -> { inStr = true; quote = ch }
                    '[' -> depth++
                    ']' -> {
                        depth--
                        if (depth == 0) return html.substring(start, p + 1)
                    }
                }
            }
        }
        return null
    }

    fun dataset(html: String, photoHost: String): List<ApplicantDto> {
        val raw = extractJsonArray(html, "dataset") ?: return emptyList()
        val arr = json.parseToJsonElement(raw) as? JsonArray ?: return emptyList()
        return arr.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            mapRow(o, photoHost)
        }
    }

    fun mapRow(o: JsonObject, photoHost: String): ApplicantDto? {
        val id = o.int("aid") ?: return null
        val display = stripTags(o.str("name").orEmpty())
            .replace("(PDF)", "", ignoreCase = true)
            .replace(Regex("""\s*\((Sevak|AT)[^)]*\)"""), "")
            .trim()
        val parts = display.split(Regex("\\s+")).filter { it.isNotBlank() }
        val given = parts.firstOrNull().orEmpty()
        val family = parts.drop(1).joinToString(" ")
        val genderRaw = o.str("gender").orEmpty()
        val typeRaw = o.str("type").orEmpty()
        val old = o.str("o_n").orEmpty().contains("Old", ignoreCase = true)
        val counts = listOf(
            "10-day" to o.int("course_10d"),
            "Satipatthana" to o.int("course_stp"),
            "Dhamma service" to o.int("course_seva"),
        ).mapNotNull { (l, n) -> n?.takeIf { it > 0 }?.let { CountDto(l, it) } }
        val history = if (old || counts.isNotEmpty()) {
            HistoryDto(o.str("first_course"), o.str("last_course"), counts)
        } else {
            null
        }
        val photo = o.str("photo")?.let { p ->
            when {
                p.startsWith("http") -> p
                p.startsWith("/") && photoHost.isNotBlank() -> photoHost.trimEnd('/') + p
                p.isNotBlank() && photoHost.isNotBlank() -> photoHost.trimEnd('/') + "/" + p.trimStart('/')
                else -> p
            }
        }
        return ApplicantDto(
            id = id,
            centreId = o.int("centreid") ?: 0,
            courseId = o.int("courseid") ?: 0,
            givenName = given,
            familyName = family,
            gender = if (genderRaw.startsWith("M", true)) "M" else "F",
            status = o.str("app_status") ?: o.str("status")?.substringBefore(" (") ?: "",
            type = if (typeRaw.equals("sevak", true)) ApplicantType.Sevak.name else ApplicantType.Student.name,
            oldStudent = old,
            attended = false,
            confNo = o.str("confno"),
            email = o.str("contact_email"),
            mobile = o.str("contact_mobile"),
            phoneHome = o.str("contact_home"),
            city = o.str("city"),
            state = o.str("state"),
            country = o.str("country"),
            dob = o.str("dob"),
            age = o.int("age"),
            monk = o.str("monk") in listOf("1", "true", "Yes") || o.int("monk") == 1,
            createdAt = o.str("app_created"),
            photoUrl = photo,
            emergencyPresent = !o.str("emergency_num").isNullOrBlank(),
            history = history,
            flags = emptyList(),
        )
    }

    fun stripTags(raw: String): String =
        raw.replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun centreIdFromPath(path: String): Int? {
        val m = Regex("""/(?:search-app|centre|center|course)/(\d+)""").find(path) ?: return null
        return m.groupValues[1].toIntOrNull()
    }

    private fun JsonObject.str(key: String): String? {
        val v = this[key] ?: return null
        if (v is JsonNull) return null
        val s = when (v) {
            is JsonPrimitive -> v.contentOrNull ?: v.content
            else -> v.toString()
        }.trim()
        return s.takeIf { it.isNotEmpty() && it != "null" }
    }

    private fun JsonObject.int(key: String): Int? {
        val v = this[key] ?: return null
        if (v is JsonNull) return null
        return when (v) {
            is JsonPrimitive -> v.content.toIntOrNull()
            else -> null
        }
    }
}
