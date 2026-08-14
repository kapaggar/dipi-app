package org.dhamma.dipi.staff.model

/**
 * Open string from the server. Known literals only drive badge tones.
 * Never implement transitions here.
 */
data class ApplicantStatus(val value: String) {
    val tone: StatusTone
        get() = when (normalize()) {
            "confirmed" -> StatusTone.Confirmed
            "pending" -> StatusTone.Pending
            "received", "reconfirmation" -> StatusTone.Received
            "expected" -> StatusTone.Expected
            "cancelled", "rejected" -> StatusTone.Cancelled
            else -> StatusTone.Pending
        }

    fun normalize(): String = value.trim().lowercase()

    companion object {
        val SHEET_CHOICES: List<String> = listOf(
            "Pending",
            "Received",
            "Confirmed",
            "Expected",
            "Reconfirmation",
            "Cancelled",
            "Rejected",
            "Custom…",
        )

        fun fromServer(raw: String): ApplicantStatus = ApplicantStatus(raw)

        fun mergeChoices(server: List<String>): List<String> {
            val fromServer = server.filter { it.isNotBlank() && !it.equals("Approved", ignoreCase = true) }
            if (fromServer.isEmpty()) return SHEET_CHOICES
            val custom = fromServer.filter { it.contains("custom", ignoreCase = true) }
            val rest = fromServer.filter { !it.contains("custom", ignoreCase = true) }
            return (rest + custom.ifEmpty { listOf("Custom…") }).distinct()
        }
    }
}

enum class StatusTone { Confirmed, Pending, Received, Expected, Cancelled }
