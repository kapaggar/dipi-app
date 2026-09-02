package org.dhamma.dipi.staff.model

import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Course-lock date parsing (spec 2a S2). The desk carries no machine-readable
 * course dates, but the course *name* does — `"Dhamma Sudha / 10 Day / 2026 /
 * 2nd-Sep to 13th-Sep"` — so the running course is resolved by parsing the
 * window out of the name and asking whether today falls inside it. Pure,
 * clock-injected, tested.
 */
data class CourseWindow(val start: LocalDate, val end: LocalDate) {
    operator fun contains(day: LocalDate): Boolean = !day.isBefore(start) && !day.isAfter(end)

    /** "2 Sep – 13 Sep 2026" — the settings card's dates line. */
    fun label(): String {
        val d = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
        val dy = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
        return if (start == end) start.format(dy) else "${start.format(d)} – ${end.format(dy)}"
    }
}

/** `2nd-Sep`, `13th-Sep`, `1st Jan` — ordinal day + 3-letter English month. */
private val DATE_TOKEN = Regex("""(\d{1,2})(?:st|nd|rd|th)[-\s]([A-Za-z]{3})""")

/** A `<date> to <date>` range, or a lone date (then end = start). */
private val DATE_RANGE = Regex(
    """^${DATE_TOKEN.pattern}(?:\s+to\s+${DATE_TOKEN.pattern})?$""",
    RegexOption.IGNORE_CASE,
)

private val YEAR_SEGMENT = Regex("""^\d{4}$""")

private fun monthFrom(abbr: String): Month? =
    Month.entries.firstOrNull {
        it.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).equals(abbr, ignoreCase = true)
    }

private fun dateOrNull(year: Int, month: Month, day: Int): LocalDate? =
    runCatching { LocalDate.of(year, month, day) }.getOrNull()

/**
 * "Dhamma Sudha / 10 Day / 2026 / 2nd-Sep to 13th-Sep" → window; null when
 * unparseable. Rules (recon fixtures):
 *  - segments split on `/`; the 4-digit year segment is optional — absent, the
 *    year is inferred as the one that puts the start within
 *    [today − 11 months, today + 11 months] (nearest to today when two fit);
 *  - the date range is `<ord>-<Mon> to <ord>-<Mon>` with `st|nd|rd|th`
 *    ordinals and 3-letter English months;
 *  - single-date names (`… / 10 Day / 2nd-Sep`) get `end = start`;
 *  - a Dec→Jan range rolls the year on the end date.
 */
fun parseCourseWindow(name: String, today: LocalDate): CourseWindow? {
    val segments = name.split('/').map { it.trim() }.filter { it.isNotEmpty() }
    val year = segments.firstOrNull { YEAR_SEGMENT.matches(it) }?.toIntOrNull()
    val range = segments.firstNotNullOfOrNull { DATE_RANGE.matchEntire(it) } ?: return null

    val (startDayRaw, startMonRaw, endDayRaw, endMonRaw) = range.destructured
    val startDay = startDayRaw.toIntOrNull() ?: return null
    val startMonth = monthFrom(startMonRaw) ?: return null

    val start = if (year != null) {
        dateOrNull(year, startMonth, startDay) ?: return null
    } else {
        inferStart(startMonth, startDay, today) ?: return null
    }

    val end = if (endDayRaw.isEmpty()) {
        start
    } else {
        val endDay = endDayRaw.toIntOrNull() ?: return null
        val endMonth = monthFrom(endMonRaw) ?: return null
        val sameYear = dateOrNull(start.year, endMonth, endDay) ?: return null
        // A Dec→Jan (or any backwards-reading) range rolls the year forward.
        if (sameYear.isBefore(start)) {
            dateOrNull(start.year + 1, endMonth, endDay) ?: return null
        } else {
            sameYear
        }
    }
    return CourseWindow(start, end)
}

/**
 * No year segment: the year that puts the start within
 * [today − 11 months, today + 11 months]; nearest to today when two fit.
 */
private fun inferStart(month: Month, day: Int, today: LocalDate): LocalDate? {
    val floor = today.minusMonths(11)
    val ceil = today.plusMonths(11)
    return (today.year - 1..today.year + 1)
        .mapNotNull { y -> dateOrNull(y, month, day) }
        .filter { !it.isBefore(floor) && !it.isAfter(ceil) }
        .minByOrNull { kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(today, it)) }
}

/**
 * The course whose parsed window contains today — first match in page order,
 * null when none does (course ops then shows the empty state; no picker, ever).
 */
fun runningCourse(courses: List<Course>, today: LocalDate): Course? =
    courses.firstOrNull { course ->
        parseCourseWindow(course.name, today)?.contains(today) == true
    }
