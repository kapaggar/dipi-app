package org.dhamma.dipi.staff.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.SensitiveInfo

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
    /**
     * Display-only ID + surviving health disclosures by applicant id.
     * Never serialized — the caller must keep this in memory only.
     */
    val sensitive: Map<Int, SensitiveInfo> = emptyMap(),
)

object SearchPageParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(html: String, pathCentreId: Int? = null): SearchPage {
        val rows = datasetObjects(html)
        return SearchPage(
            tokens = tokens(html),
            centres = selectOptions(html, "edit-centre") + selectOptions(html, "edit-center"),
            courses = selectOptions(html, "edit-course"),
            statuses = selectOptionsRaw(html, "edit-app-status")
                .map { it.second }
                .filter { it.isNotBlank() && !it.equals("Choose", true) },
            dataset = rows.mapNotNull { mapRow(it) },
            pathCentreId = pathCentreId,
            sensitive = sensitiveMap(rows),
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

    private fun datasetObjects(html: String): List<JsonObject> {
        val raw = extractJsonArray(html, "dataset") ?: return emptyList()
        val arr = json.parseToJsonElement(raw) as? JsonArray ?: return emptyList()
        return arr.mapNotNull { it as? JsonObject }
    }

    /* ── Sensitive extraction (display only, never persisted) ─────────── */

    /**
     * The desk row's PDF-link remnant on the display name — "( PDF )",
     * "(PDF)", any spacing, any case. Stripped at parse time so every
     * screen is clean at once (owner feedback 2026-08-16).
     */
    private val PDF_SUFFIX = Regex("""\s*\(\s*PDF\s*\)""", RegexOption.IGNORE_CASE)

    /** dipi edit-form PAN shape: 5 letters + 4 digits + 1 letter (loader.js `looksLikePan`). */
    private val PAN_SHAPE = Regex("""^[A-Za-z]{5}\d{4}[A-Za-z]$""")

    /** Detail label → raw `var dataset` key (from dh_manageapp search.inc `$rs[...]`). */
    private val HEALTH_FIELDS = listOf(
        "Physical health" to "physical",
        "Mental health" to "mental",
        "Medication" to "medication",
        "Addiction" to "addiction",
        "Other meditation" to "othertechnique",
        "Pregnancy" to "pregnant",
    )

    fun sensitiveMap(rows: List<JsonObject>): Map<Int, SensitiveInfo> = buildMap {
        rows.forEach { o ->
            val id = o.int("aid") ?: return@forEach
            sensitiveRow(o)?.let { put(id, it) }
        }
    }

    /**
     * The ID document + surviving health disclosures for one raw row.
     * Classification mirrors loader.js `resolveId`: a PAN-shaped value in the
     * aadhar column is a PAN (the desk edit form has one Identifier field).
     */
    fun sensitiveRow(o: JsonObject): SensitiveInfo? {
        val aadhar = o.str("aadhar")
        val id: Pair<String, String>? = when {
            aadhar != null && PAN_SHAPE.matches(aadhar.replace(Regex("""\s+"""), "")) -> "PAN" to aadhar
            aadhar != null -> "Aadhaar" to aadhar
            else -> o.str("pancard")?.let { "PAN" to it }
                ?: o.str("voterid")?.let { "Voter ID" to it }
                ?: o.str("passport")?.let { "Passport" to it }
        }
        val male = o.str("gender").orEmpty().startsWith("M", ignoreCase = true)
        val health = linkedMapOf<String, String>()
        HEALTH_FIELDS.forEach { (label, key) ->
            val value = o.str(key)?.let { stripTags(it) }?.takeIf { it.isNotBlank() }
            if (HealthNoiseFilter.keep(value, pregnancy = key == "pregnant", male = male)) {
                health[label] = value!!
            }
        }
        if (id == null && health.isEmpty()) return null
        return SensitiveInfo(idLabel = id?.first, idNumber = id?.second, health = health)
    }

    fun mapRow(o: JsonObject): ApplicantDto? {
        val id = o.int("aid") ?: return null
        val display = stripTags(o.str("name").orEmpty())
            .replace(PDF_SUFFIX, "")
            .replace(Regex("""\s*\((Sevak|AT)[^)]*\)"""), "")
            .trim()
        val parts = display.split(Regex("\\s+")).filter { it.isNotBlank() }
        val given = parts.firstOrNull().orEmpty()
        val family = parts.drop(1).joinToString(" ")
        val genderRaw = o.str("gender").orEmpty()
        val typeRaw = o.str("type").orEmpty()
        val old = o.str("o_n").orEmpty().contains("Old", ignoreCase = true)
        // Course-count keys are non-NPI (search.inc `course_*`) — safe on the DTO.
        val counts = listOf(
            "10-day" to o.int("course_10d"),
            "20-day" to o.int("course_20d"),
            "30-day" to o.int("course_30d"),
            "45-day" to o.int("course_45d"),
            "60-day" to o.int("course_60d"),
            "Satipatthana" to o.int("course_stp"),
            "Special" to o.int("course_spl"),
            "TSC" to o.int("course_tsc"),
            "Teen" to o.int("course_teen"),
            "Dhamma service" to o.int("course_seva"),
        ).mapNotNull { (l, n) -> n?.takeIf { it > 0 }?.let { CountDto(l, it) } }
        val history = if (old || counts.isNotEmpty()) {
            HistoryDto(o.str("first_course"), o.str("last_course"), counts)
        } else {
            null
        }
        // Presence/equality booleans only — the raw ID and emergency values are
        // read locally, reduced to booleans, and never stored or logged (NPI rule).
        val idPresent = listOf("aadhar", "pancard", "voterid", "passport")
            .any { !o.str(it).isNullOrBlank() }
        val ownDigits = phoneKey(o.str("contact_mobile"))
        val emerDigits = phoneKey(o.str("emergency_num"))
        val emergencyEqSelf = ownDigits != null && ownDigits == emerDigits
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
            emergencyPresent = !o.str("emergency_num").isNullOrBlank(),
            idPresent = idPresent,
            emergencyNamePresent = !o.str("emergency_name").isNullOrBlank(),
            emergencyEqSelf = emergencyEqSelf,
            history = history,
            flags = emptyList(),
        )
    }

    /** Last 10 digits (audit.js normPhone) — used only for the eq-self boolean, never kept. */
    private fun phoneKey(raw: String?): String? {
        val d = raw?.filter { it.isDigit() } ?: return null
        if (d.isEmpty()) return null
        return if (d.length >= 10) d.takeLast(10) else d
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

/**
 * Port of the course-audit "Send to Claude" noise filter (callconfirm
 * course-audit/loader.js). Decides which health disclosures are meaningful
 * enough to surface at the desk:
 *
 * - Pregnancy is dropped for males and for values starting with "No";
 *   kept when it carries details ("Yes ( 3 months )").
 * - Generic positives ("happy", "good", "fine", … and the listed multi-word
 *   generic positives) are dropped.
 * - Single negative-state words alone ("stressed", "confused", "anxious",
 *   "sad", "netural") are dropped — but "depressed" is KEPT even alone.
 * - Blank / "no" / "na" / "nil" / "none" and bare geographic names drop.
 * - All other multi-word free text, medication and addiction disclosures keep.
 */
object HealthNoiseFilter {

    private val NOISE_EXACT = setOf(
        "", "no", "na", "n/a", "none", "nil", "-", "—", ".", "..", "...", "*",
        "normal", "fine", "healthy", "good", "happy", "cheerful", "stable",
        "best", "nice", "cordial", "ok", "okay", "cool", "well", "great",
        "satisfied", "peaceful", "positive", "wonderful", "sympathy",
        "very good", "so good", "all good", "feeling good", "feeling well",
        "happy and good", "good and happy", "happy and cheerful",
        "happy and satisfied", "happy ,cheerful", "happy , cheerful",
        "happy ,sad", "happy, sad", "happy and sad anxious stressed",
        "happy - everything is going fine", "happy - everything is going fine.",
        "netural", "neutral", "fine and good", "a bit tough",
        "stressed", "stresssed", "stresssesd", "confused", "anxious", "sad",
        "stressed,confused", "confused state of mind",
    )

    private val GEO_NOISE = setOf(
        "india", "delhi", "new delhi", "mumbai", "bombay", "bangalore", "bengaluru",
        "noida", "gurgaon", "gurugram", "kolkata", "calcutta", "chennai",
        "hyderabad", "pune", "ahmedabad", "jaipur", "lucknow", "agra",
        "faridabad", "ghaziabad", "meerut", "kanpur", "varanasi",
        "uttar pradesh", "up", "haryana", "punjab", "rajasthan",
        "maharashtra", "karnataka", "tamil nadu", "kerala", "west bengal",
        "bihar", "jharkhand", "odisha", "orissa", "assam", "telangana",
        "andhra pradesh", "gujarat", "madhya pradesh", "chhattisgarh",
        "uttarakhand", "himachal pradesh", "jammu and kashmir", "goa",
        "sikkim", "tripura", "manipur", "nagaland", "mizoram",
        "arunachal pradesh", "meghalaya",
    )

    private val LEADING_NO = Regex("""^no\b""", RegexOption.IGNORE_CASE)

    fun isNoise(value: String?): Boolean {
        if (value == null) return true
        val s = value.replace('\u00A0', ' ')
            .lowercase()
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (s.isEmpty()) return true
        return s in NOISE_EXACT || s in GEO_NOISE
    }

    fun keep(value: String?, pregnancy: Boolean = false, male: Boolean = false): Boolean {
        if (value.isNullOrBlank()) return false
        if (pregnancy) {
            if (male) return false
            if (LEADING_NO.containsMatchIn(value.trim())) return false
        }
        return !isNoise(value)
    }
}
