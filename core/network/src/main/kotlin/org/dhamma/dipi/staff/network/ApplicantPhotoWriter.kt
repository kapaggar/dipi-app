package org.dhamma.dipi.staff.network

import org.dhamma.dipi.staff.model.PhotoOrigins
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

sealed class PhotoDeskWriteResult {
    data class Committed(val message: String) : PhotoDeskWriteResult()
    data class Failed(val message: String, val unauthorized: Boolean = false) : PhotoDeskWriteResult()
    data class Incomplete(val message: String) : PhotoDeskWriteResult()
    data class Unknown(val message: String) : PhotoDeskWriteResult()
}

/**
 * Live desk photo write: `GET /app/{id}/edit`, echo the scraped form, attach
 * the corrected JPEG on the form's file part, `POST` the form action.
 * Held forms are in-memory only and [wipe]d after each attempt.
 */
class ApplicantPhotoWriter(
    private val api: StaffApi,
    private val baseUrl: String,
) {
    private val held = AtomicReference<ApplicantEditForm?>(null)

    internal var logLine: (String) -> Unit = { line ->
        runCatching { android.util.Log.i(LOG_TAG, line) }
    }

    fun wipe() {
        held.set(null)
    }

    fun heldForm(): ApplicantEditForm? = held.get()

    suspend fun submit(
        applicantId: Int,
        jpeg: ByteArray,
        fileName: String,
    ): PhotoDeskWriteResult {
        if (applicantId <= 0) {
            return PhotoDeskWriteResult.Incomplete("Could not read the application form")
        }
        if (jpeg.isEmpty()) {
            return PhotoDeskWriteResult.Incomplete("Corrected photo is empty")
        }
        val origin = try {
            PhotoOrigins.canonicalize(baseUrl.trim().trimEnd('/'))
        } catch (_: Exception) {
            return PhotoDeskWriteResult.Incomplete("Could not read the desk address")
        }
        val get = try {
            api.appEditPage(applicantId)
        } catch (e: IOException) {
            return PhotoDeskWriteResult.Failed(offline("the application form"))
        } catch (e: Exception) {
            return PhotoDeskWriteResult.Failed(e.message?.takeIf { it.isNotBlank() } ?: "Could not read the application form")
        }
        val html = get.html()
        if (ApplicantEditFormParser.looksLikeLogin(html)) {
            return PhotoDeskWriteResult.Failed("Access denied", unauthorized = true)
        }
        if (!get.isSuccessful) {
            return PhotoDeskWriteResult.Failed(
                surface(html, get.code()),
                unauthorized = get.code() == 403,
            )
        }
        val parsed = ApplicantEditFormParser.parse(html, applicantId, origin)
        if (parsed is ApplicantEditFormParse.Incomplete) {
            logLine("edit-form GET id=$applicantId incomplete")
            return PhotoDeskWriteResult.Incomplete(parsed.reason)
        }
        val form = (parsed as ApplicantEditFormParse.Ready).form
        held.set(form)
        logLine("edit-form GET id=$applicantId fields=${form.fields.size} file=${form.fileFieldName}")
        return try {
            val post = try {
                api.postApplicantEdit(form.action, form.toMultipart(jpeg, fileName))
            } catch (_: IOException) {
                return PhotoDeskWriteResult.Unknown("Lost the desk reply after posting the application form")
            } catch (e: Exception) {
                return PhotoDeskWriteResult.Unknown(
                    e.message?.takeIf { it.isNotBlank() }
                        ?: "Lost the desk reply after posting the application form",
                )
            }
            val postHtml = post.html()
            logLine("edit-form POST id=$applicantId status=${post.code()} fields=${form.fields.size}")
            interpret(applicantId, post.code(), post.raw().request.url.encodedPath, postHtml)
        } finally {
            held.set(null)
        }
    }

    private fun interpret(
        applicantId: Int,
        code: Int,
        path: String,
        html: String,
    ): PhotoDeskWriteResult {
        if (ApplicantEditFormParser.looksLikeLogin(html)) {
            return PhotoDeskWriteResult.Failed("Access denied", unauthorized = true)
        }
        if (code == 403) {
            return PhotoDeskWriteResult.Failed(surface(html, code), unauthorized = true)
        }
        val errors = ApplicantEditFormParser.extractErrorMessages(html)
        if (errors.isNotBlank()) {
            return PhotoDeskWriteResult.Failed(errors)
        }
        if (code >= 500) {
            return PhotoDeskWriteResult.Failed(surface(html, code))
        }
        val stillOnForm = path.contains("/app/$applicantId/edit") &&
            html.contains("name=\"form_id\"")
        if (stillOnForm) {
            return PhotoDeskWriteResult.Failed(
                surface(html, code).ifBlank { "The desk did not accept the photo update" },
            )
        }
        if (code in 200..399) {
            val message = ApplicantEditFormParser.extractMessages(html)
                .ifBlank { "Updated the application on the desk" }
            return PhotoDeskWriteResult.Committed(message)
        }
        return PhotoDeskWriteResult.Failed(surface(html, code))
    }

    private fun surface(html: String, code: Int): String {
        val messages = ApplicantEditFormParser.extractMessages(html)
        if (messages.isNotBlank()) return messages
        val text = SearchPageParser.stripTags(html).trim().replace(Regex("\\s+"), " ")
        return text.take(400).ifBlank { "HTTP $code" }
    }

    private fun offline(what: String): String = "Offline - could not reach the desk for $what"

    companion object {
        private const val LOG_TAG = "dipi-photo"
    }
}
