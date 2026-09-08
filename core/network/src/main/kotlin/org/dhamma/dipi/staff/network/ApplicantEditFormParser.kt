package org.dhamma.dipi.staff.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.PhotoOrigins
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URI

/**
 * In-memory scrape of `GET /app/{id}/edit` (`dh_ma_applicant_form`).
 * Field values stay here for the matching POST only — never Room, DataStore,
 * DTO fields or logs. [toString] reports counts, not values.
 */
class ApplicantEditForm(
    val applicantId: Int,
    val action: String,
    val fileFieldName: String,
    val fields: Map<String, String>,
) {
    fun toMultipart(jpeg: ByteArray, fileName: String): MultipartBody {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
        for ((name, value) in fields) {
            if (name == fileFieldName) continue
            body.addFormDataPart(name, value)
        }
        body.addFormDataPart(
            fileFieldName,
            fileName,
            jpeg.toRequestBody("image/jpeg".toMediaType()),
        )
        return body.build()
    }

    override fun toString(): String =
        "ApplicantEditForm(id=$applicantId, action=$action, file=$fileFieldName, fields=${fields.size})"
}

sealed class ApplicantEditFormParse {
    class Ready(val form: ApplicantEditForm) : ApplicantEditFormParse()
    class Incomplete(val reason: String) : ApplicantEditFormParse()
}

/**
 * Echo every current control the desk form would submit. Missing tokens or
 * the file input fail closed — no guessed field names or values.
 */
object ApplicantEditFormParser {
    const val LIVE_FORM_ID = "dh_ma_applicant_form"
    const val SUBMIT_UPDATE = "Update"

    fun parse(html: String, applicantId: Int, origin: String): ApplicantEditFormParse {
        if (applicantId <= 0) return ApplicantEditFormParse.Incomplete("Could not read the application form")
        if (looksLikeLogin(html)) {
            return ApplicantEditFormParse.Incomplete("Sign in to update the application")
        }
        val formEl = pickForm(html, applicantId)
            ?: return ApplicantEditFormParse.Incomplete("Could not read the application form")
        val action = resolveAction(formEl.attr("action"), origin, applicantId)
            ?: return ApplicantEditFormParse.Incomplete("The application form action is not safe to post")
        val fields = collectFields(formEl)
        val formId = fields["form_id"]?.trim().orEmpty()
        if (formId.isEmpty() || formId.startsWith("user_login")) {
            return ApplicantEditFormParse.Incomplete("Could not read the application form tokens")
        }
        if (fields["form_build_id"].isNullOrBlank() || fields["form_token"].isNullOrBlank()) {
            return ApplicantEditFormParse.Incomplete("Could not read the application form tokens")
        }
        val fileName = fileFieldName(formEl)
            ?: return ApplicantEditFormParse.Incomplete("Could not read the application photo file field")
        val submit = submitControl(formEl)
            ?: return ApplicantEditFormParse.Incomplete("Could not read the application form submit control")
        fields[submit.first] = submit.second
        if (!fields.containsKey("a_f_name")) {
            return ApplicantEditFormParse.Incomplete("Could not read the application form - missing required fields")
        }
        for ((name, value) in fields) {
            if (name.contains("status", ignoreCase = true) && ApplicantStatus.isForbiddenWrite(value)) {
                return ApplicantEditFormParse.Incomplete("The app never sends Approved")
            }
        }
        return ApplicantEditFormParse.Ready(
            ApplicantEditForm(
                applicantId = applicantId,
                action = action,
                fileFieldName = fileName,
                fields = fields.toMap(),
            ),
        )
    }

    internal fun looksLikeLogin(html: String): Boolean =
        html.contains("name=\"pass\"") &&
            (html.contains("user_login_block") || html.contains("user_login"))

    internal fun extractMessages(html: String): String {
        val doc = Jsoup.parse(html)
        return doc.select(
            "div.messages, div.alert, .messages.error, .messages.status, .alert-danger, .alert-success",
        ).map { it.text().trim() }.filter { it.isNotEmpty() }.joinToString("\n")
    }

    internal fun extractErrorMessages(html: String): String {
        val doc = Jsoup.parse(html)
        return doc.select(
            "div.messages.error, .messages--error, div.alert-danger, .alert-error",
        ).map { it.text().trim() }.filter { it.isNotEmpty() }.joinToString("\n")
    }

    private fun pickForm(html: String, applicantId: Int): Element? {
        val forms = Jsoup.parse(html).select("form")
        forms.firstOrNull { form ->
            form.select("input[name=form_id]").any { it.attr("value") == LIVE_FORM_ID }
        }?.let { return it }
        val editPath = "/app/$applicantId/edit"
        forms.firstOrNull { form ->
            form.select("input[type=file]").isNotEmpty() &&
                form.attr("action").contains(editPath)
        }?.let { return it }
        return forms.firstOrNull { it.select("input[type=file]").isNotEmpty() }
    }

    private fun collectFields(form: Element): LinkedHashMap<String, String> {
        val out = LinkedHashMap<String, String>()
        for (el in form.select("input, textarea, select")) {
            if (el.hasAttr("disabled")) continue
            val name = el.attr("name").trim()
            if (name.isEmpty()) continue
            when (el.tagName().lowercase()) {
                "textarea" -> out[name] = el.`val`()
                "select" -> {
                    val selected = el.select("option[selected]").last()
                        ?: el.select("option").firstOrNull()
                    out[name] = selected?.`val`().orEmpty()
                }
                "input" -> {
                    val type = el.attr("type").lowercase().ifEmpty { "text" }
                    when (type) {
                        "file", "button", "reset", "image", "submit" -> Unit
                        "radio" -> if (el.hasAttr("checked")) out[name] = el.`val`()
                        "checkbox" -> if (el.hasAttr("checked")) {
                            out[name] = el.`val`().ifEmpty { "1" }
                        }
                        else -> out[name] = el.`val`()
                    }
                }
            }
        }
        return out
    }

    private fun fileFieldName(form: Element): String? {
        val files = form.select("input[type=file]").filter { !it.hasAttr("disabled") }
        if (files.isEmpty()) return null
        val named = files.firstOrNull { el ->
            val n = el.attr("name")
            n == "files[upload_photo]" || n == "upload_photo" || n.contains("photo", ignoreCase = true)
        }
        return (named ?: files.singleOrNull())?.attr("name")?.takeIf { it.isNotBlank() }
    }

    private fun submitControl(form: Element): Pair<String, String>? {
        val submits = form.select("input[type=submit][name], button[type=submit][name]")
            .filter { !it.hasAttr("disabled") }
        val update = submits.firstOrNull { it.`val`().equals(SUBMIT_UPDATE, ignoreCase = true) }
            ?: submits.firstOrNull { it.attr("name") == "op" }
            ?: submits.firstOrNull()
        val name = update?.attr("name")?.trim().orEmpty()
        val value = update?.`val`().orEmpty()
        if (name.isEmpty() || value.isEmpty()) return null
        return name to value
    }

    private fun resolveAction(raw: String, origin: String, applicantId: Int): String? {
        val candidate = raw.trim().replace("&amp;", "&").ifEmpty { "/app/$applicantId/edit" }
        val originUri = try {
            URI(origin)
        } catch (_: Exception) {
            return null
        }
        val resolved = try {
            if (candidate.startsWith("http://") || candidate.startsWith("https://")) {
                URI(candidate)
            } else {
                originUri.resolve(candidate)
            }
        } catch (_: Exception) {
            return null
        }
        if (!sameOrigin(resolved, originUri)) return null
        val query = resolved.rawQuery.orEmpty()
        if (query.split("&").any { part ->
                val key = part.substringBefore("=")
                key == "r" || key == "r[]"
            }
        ) {
            return null
        }
        val path = resolved.rawPath.orEmpty().ifEmpty { "/app/$applicantId/edit" }
        return if (query.isEmpty()) path else "$path?$query"
    }

    private fun sameOrigin(resolved: URI, origin: URI): Boolean {
        val hostR = resolved.host ?: return false
        val hostO = origin.host ?: return false
        return resolved.scheme.equals(origin.scheme, ignoreCase = true) &&
            hostR.equals(hostO, ignoreCase = true) &&
            effectivePort(resolved) == effectivePort(origin)
    }

    private fun effectivePort(uri: URI): Int = when {
        uri.port > 0 -> uri.port
        uri.scheme.equals("https", ignoreCase = true) -> 443
        else -> 80
    }

}
