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
            "cancelled", "rejected", "duplicate" -> StatusTone.Cancelled
            else -> StatusTone.Pending
        }

    fun normalize(): String = value.trim().lowercase()

    companion object {
        val COMMON_CHOICES: List<String> = listOf(
            "Confirmed",
            "Cancelled",
            "Duplicate",
            "Custom…",
        )

        val RARE_CHOICES: List<String> = listOf(
            "Pending",
            "Received",
            "Expected",
            "Reconfirmation",
            "Rejected",
            "Clarification",
        )

        /** COMMON first (Custom last among common), then RARE. Never includes Approved. */
        val SHEET_CHOICES: List<String> = COMMON_CHOICES + RARE_CHOICES

        /** Hard rule 3 — Custom / row changer must not send this literal. */
        fun isForbiddenWrite(status: String): Boolean =
            status.trim().equals("Approved", ignoreCase = true)

        /**
         * Prefer the desk's parsed `#edit-app-status` select; fall back to
         * roster labels. Always through [mergeChoices] so Approved cannot
         * reach the sheet.
         */
        fun deriveStatuses(select: List<String>, roster: List<String>): List<String> {
            val fromSelect = select.map { it.trim() }.filter { it.isNotEmpty() }
            val fromRoster = roster.map { it.trim() }
                .filter { it.isNotEmpty() && !it.equals("All", ignoreCase = true) }
            return mergeChoices(fromSelect.ifEmpty { fromRoster })
        }

        fun mergeChoices(server: List<String>): List<String> {
            val fromServer = server.filter { it.isNotBlank() && !it.equals("Approved", ignoreCase = true) }
            if (fromServer.isEmpty()) return SHEET_CHOICES
            val seen = linkedSetOf<String>()
            fun add(label: String) {
                val key = label.trim().lowercase()
                if (key.isEmpty() || key == "approved" || key in seen) return
                seen += key
            }
            COMMON_CHOICES.forEach { add(it) }
            fromServer.forEach { raw ->
                if (!raw.contains("custom", ignoreCase = true)) add(raw)
            }
            val byKey = (COMMON_CHOICES + fromServer).associateBy { it.trim().lowercase() }
            return seen.map { key ->
                if (key.contains("custom")) "Custom…" else byKey[key] ?: key.replaceFirstChar { it.titlecase() }
            }
        }
    }
}

enum class StatusTone { Confirmed, Pending, Received, Expected, Cancelled }
