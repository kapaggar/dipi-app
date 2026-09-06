package org.dhamma.dipi.staff.model

import kotlinx.serialization.Serializable
import java.net.URI

@Serializable
data class WhatsAppScope(val origin: String, val centreId: Int) {
    init {
        val uri = URI(origin)
        require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.rawUserInfo == null &&
            uri.rawQuery == null && uri.rawFragment == null && uri.path.isNullOrEmpty() && centreId > 0)
    }
}

@Serializable
data class CentreWhatsAppProfile(
    val scope: WhatsAppScope,
    val enabled: Boolean = false,
    val packageName: String = "com.whatsapp",
    val letterId: Int? = null,
    val testedVersion: String? = null,
) {
    init { require(packageName in WHATSAPP_PACKAGES) }
}

val WHATSAPP_PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")

/** Full international number, or the desk's established bare Indian mobile form. */
fun automationPhone(raw: String?): String? {
    val text = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (!text.matches(Regex("\\+?[0-9 ()-]+"))) return null
    var digits = text.filter { it in '0'..'9' }
    if (!text.startsWith("+") && digits.length == 10 && digits[0] in '6'..'9') digits = "91$digits"
    if (digits.length !in 8..15 || digits.startsWith('0')) return null
    return digits
}

@Serializable
enum class WhatsAppAttemptState {
    Pending, Preparing, Opening, SendStarted, SubmissionObserved, OutcomeUnknown, Failed, Skipped;

    fun afterInterruption() = when (this) {
        SendStarted -> OutcomeUnknown
        Preparing, Opening -> Pending
        else -> this
    }
}

@Serializable
data class WhatsAppAttempt(val applicantId: Int, val phone: String, val state: WhatsAppAttemptState = WhatsAppAttemptState.Pending) {
    override fun toString() = "WhatsAppAttempt($applicantId, [redacted], $state)"
}

@Serializable
data class WhatsAppBatch(
    val scope: WhatsAppScope,
    val courseId: Int,
    val letterId: Int,
    val attempts: List<WhatsAppAttempt>,
    val paused: Boolean = true,
    val duplicatesConfirmed: Boolean = false,
) {
    init {
        require(courseId > 0 && letterId > 0 && attempts.isNotEmpty())
        require(attempts.map { it.applicantId }.distinct().size == attempts.size)
        require(attempts.all { it.applicantId > 0 && automationPhone("+${it.phone}") == it.phone })
        require(duplicatesConfirmed || attempts.map { it.phone }.distinct().size == attempts.size)
    }
    fun interrupted() = copy(paused = true, attempts = attempts.map { it.copy(state = it.state.afterInterruption()) })
    fun update(id: Int, state: WhatsAppAttemptState) = copy(attempts = attempts.map { if (it.applicantId == id) it.copy(state = state) else it })
}
