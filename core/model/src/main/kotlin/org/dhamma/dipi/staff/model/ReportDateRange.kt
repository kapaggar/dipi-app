package org.dhamma.dipi.staff.model

import java.time.LocalDate

enum class ReportPreset { THIS_MONTH, THIS_YEAR, LAST_12_MONTHS }

fun reportRangeError(fromIso: String, toIso: String): String? {
    val from = runCatching { LocalDate.parse(fromIso) }.getOrNull()
        ?: return "Enter a valid From date (DD-MM-YYYY)."
    val to = runCatching { LocalDate.parse(toIso) }.getOrNull()
        ?: return "Enter a valid To date (DD-MM-YYYY)."
    return if (from > to) "From date must be on or before To date." else null
}

fun reportPresetRange(preset: ReportPreset, today: LocalDate): Pair<String, String> {
    val dates = when (preset) {
        ReportPreset.THIS_MONTH -> today.withDayOfMonth(1) to today.withDayOfMonth(today.lengthOfMonth())
        ReportPreset.THIS_YEAR -> LocalDate.of(today.year, 1, 1) to LocalDate.of(today.year, 12, 31)
        ReportPreset.LAST_12_MONTHS -> today.minusMonths(12).plusDays(1) to today
    }
    return dates.first.toString() to dates.second.toString()
}
