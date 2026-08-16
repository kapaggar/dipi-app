package org.dhamma.dipi.staff.model

object WorklistFilter {
    fun visible(
        rows: List<ApplicantCard>,
        selected: Set<String>,
        query: String,
    ): List<ApplicantCard> {
        val q = query.trim()
        return rows.filter { card ->
            statusMatches(card, selected) && queryMatches(card, q)
        }
    }

    fun statusMatches(card: ApplicantCard, selected: Set<String>): Boolean {
        if (selected.isEmpty()) return true
        return selected.any { it.equals(card.status.value, ignoreCase = true) }
    }

    fun queryMatches(card: ApplicantCard, query: String): Boolean {
        if (query.isBlank()) return true
        return listOfNotNull(
            card.displayName,
            card.confNo?.value,
            card.mobile,
            card.email,
            card.phoneHome,
        ).any { it.contains(query, ignoreCase = true) }
    }
}
