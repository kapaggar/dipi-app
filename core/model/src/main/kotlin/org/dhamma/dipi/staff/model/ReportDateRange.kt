package org.dhamma.dipi.staff.model

import java.time.LocalDate

/** Date-range presets on the Course report filter. Relative to the device calendar. */
enum class ReportPreset {
    THIS_MONTH,
    THIS_YEAR,
    LAST_12_MONTHS,
    ;

    val label: String
        get() = when (this) {
            THIS_MONTH -> "This month"
            THIS_YEAR -> "This year"
            LAST_12_MONTHS -> "Last 12 months"
        }

    val testTag: String
        get() = when (this) {
            THIS_MONTH -> "report-preset-this-month"
            THIS_YEAR -> "report-preset-this-year"
            LAST_12_MONTHS -> "report-preset-last-12-months"
        }
}

const val REPORT_RANGE_ERROR = "TO is before FROM. Swap the dates or pick a preset."

/** Parses ISO `yyyy-MM-dd` or desk `dd-MM-yyyy`. Invalid calendar dates are null. */
fun parseReportDate(value: String): LocalDate? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null
    val iso = when {
        ISO_DATE.matches(trimmed) -> trimmed
        DESK_DATE.matches(trimmed) -> {
            val p = trimmed.split("-")
            "${p[2]}-${p[1]}-${p[0]}"
        }
        else -> return null
    }
    return runCatching { LocalDate.parse(iso) }.getOrNull()
}

fun reportRangeIsValid(from: String, to: String): Boolean {
    val start = parseReportDate(from) ?: return false
    val end = parseReportDate(to) ?: return false
    return !end.isBefore(start)
}

/**
 * Inline error only when both sides parse and TO is before FROM.
 * Incomplete or unparseable text disables RUN without this message.
 */
fun reportRangeError(from: String, to: String): String? {
    val start = parseReportDate(from) ?: return null
    val end = parseReportDate(to) ?: return null
    return if (end.isBefore(start)) REPORT_RANGE_ERROR else null
}

fun reportPresetRange(preset: ReportPreset, today: LocalDate): Pair<String, String> {
    val from: LocalDate
    val to: LocalDate
    when (preset) {
        ReportPreset.THIS_MONTH -> {
            from = today.withDayOfMonth(1)
            to = today.withDayOfMonth(today.lengthOfMonth())
        }
        ReportPreset.THIS_YEAR -> {
            from = LocalDate.of(today.year, 1, 1)
            to = LocalDate.of(today.year, 12, 31)
        }
        ReportPreset.LAST_12_MONTHS -> {
            from = today.minusMonths(12).plusDays(1)
            to = today
        }
    }
    return from.toString() to to.toString()
}

private val ISO_DATE = Regex("""\d{4}-\d{2}-\d{2}""")
private val DESK_DATE = Regex("""\d{2}-\d{2}-\d{4}""")
