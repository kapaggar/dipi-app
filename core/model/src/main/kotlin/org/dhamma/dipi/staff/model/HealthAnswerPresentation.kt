package org.dhamma.dipi.staff.model

enum class HealthAnswerKind {
    NOT_PROVIDED,
    EXPLICIT_YES,
    EXPLICIT_NO,
    RECORDED,
    NOT_APPLICABLE,
}

fun healthAnswerKind(row: HealthRow, gender: Gender): HealthAnswerKind {
    if (gender == Gender.M && row.label.equals("Pregnancy", ignoreCase = true)) {
        return HealthAnswerKind.NOT_APPLICABLE
    }
    val value = row.answer.trim()
    return when {
        value.isEmpty() || value == "-" || value == "–" -> HealthAnswerKind.NOT_PROVIDED
        value.equals("Yes", ignoreCase = true) -> HealthAnswerKind.EXPLICIT_YES
        value.equals("No", ignoreCase = true) -> HealthAnswerKind.EXPLICIT_NO
        else -> HealthAnswerKind.RECORDED
    }
}

fun healthAnswerPositions(card: ApplicationCard): List<HealthRow> =
    ApplicationCard.HEALTH_ORDER.map { label ->
        card.healthRow(label) ?: HealthRow(label, "")
    }

fun healthBadgeLabel(kind: HealthAnswerKind): String = when (kind) {
    HealthAnswerKind.RECORDED -> "Response recorded"
    HealthAnswerKind.EXPLICIT_YES -> "Yes · as supplied"
    HealthAnswerKind.EXPLICIT_NO -> "No · as supplied"
    HealthAnswerKind.NOT_PROVIDED -> "Not provided"
    HealthAnswerKind.NOT_APPLICABLE -> "Not applicable"
}

/**
 * Source-body caption. Never rewrites punctuation or clinical content.
 * Male Pregnancy without unexpected source text is an app-authored note.
 */
fun healthSourceCaption(row: HealthRow, kind: HealthAnswerKind): String {
    if (kind == HealthAnswerKind.NOT_APPLICABLE) {
        val unexpected = row.answer.trim()
        return if (unexpected.isEmpty() || unexpected == "-" || unexpected == "–") {
            "Not applicable for male applicants · app-authored, not a source answer"
        } else {
            row.answer
        }
    }
    val trimmed = row.answer.trim()
    return when {
        trimmed.isEmpty() -> "No answer in the source"
        trimmed == "-" || trimmed == "–" -> "Source value: $trimmed"
        else -> row.answer
    }
}

data class HealthAnswerCounts(
    val recorded: Int,
    val notProvided: Int,
    val notApplicable: Int,
) {
    val total: Int get() = recorded + notProvided + notApplicable
}

fun healthAnswerCounts(rows: List<HealthRow>, gender: Gender): HealthAnswerCounts {
    val kinds = ApplicationCard.HEALTH_ORDER.map { label ->
        val row = rows.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: HealthRow(label, "")
        healthAnswerKind(row, gender)
    }
    return HealthAnswerCounts(
        recorded = kinds.count {
            it == HealthAnswerKind.RECORDED ||
                it == HealthAnswerKind.EXPLICIT_YES ||
                it == HealthAnswerKind.EXPLICIT_NO
        },
        notProvided = kinds.count { it == HealthAnswerKind.NOT_PROVIDED },
        notApplicable = kinds.count { it == HealthAnswerKind.NOT_APPLICABLE },
    )
}

fun healthSummaryText(counts: HealthAnswerCounts): String {
    val recorded = if (counts.recorded == 1) {
        "1 response recorded"
    } else {
        "${counts.recorded} responses recorded"
    }
    val missing = if (counts.notProvided == 1) {
        "1 not provided"
    } else {
        "${counts.notProvided} not provided"
    }
    return if (counts.notApplicable > 0) {
        val na = if (counts.notApplicable == 1) {
            "1 not applicable"
        } else {
            "${counts.notApplicable} not applicable"
        }
        "$recorded · $missing · $na"
    } else {
        "$recorded · $missing"
    }
}

fun ApplicationCard.completeZeroHistory(): Boolean {
    val present = historyCountsPresent ?: return false
    if (!ApplicationCard.HISTORY_ORDER.all { it in present }) return false
    return ApplicationCard.HISTORY_ORDER.all { key ->
        historyCounts.firstOrNull { it.first.equals(key, ignoreCase = true) }?.second == 0
    }
}

fun ApplicationCard.historyTileValue(key: String): String {
    val stored = historyCounts.firstOrNull { it.first.equals(key, ignoreCase = true) }?.second
    val present = historyCountsPresent
    return when {
        present == null -> (stored ?: 0).toString()
        present.none { it.equals(key, ignoreCase = true) } -> "Not provided"
        else -> (stored ?: 0).toString()
    }
}
