package org.dhamma.dipi.staff.model

import java.io.File

/**
 * The centre's course report (v5 T3, frames `5n`–`5q`) — the CSV the desk's
 * own `dh_center_course_report_form` streams back, parsed for display.
 *
 * Counts only. The teacher **names** the CSV carries are course staff, not
 * applicants, and are held in memory for the printed roster alone; nothing
 * here is persisted beyond the CSV file the desk already writes to
 * `cacheDir/sheets` for `Share CSV`.
 */
data class CourseReport(
    val rows: List<CourseReportRow> = emptyList(),
    /** The desk's own `Total` line when it sends one, else a derived sum. */
    val grandTotal: CourseReportCounts = CourseReportCounts(),
    /** The range that produced this report, echoed back in the footer. */
    val from: String = "",
    val to: String = "",
    /** The streamed CSV, kept for `Share CSV`. Never re-read for display. */
    val csv: File? = null,
) {
    val isEmpty: Boolean get() = rows.isEmpty()
}

/**
 * One course line. [course] is the desk's raw `{centre} / {type} / {year} /
 * {dates}` string; [parsed] is a best-effort split for display that falls
 * back to the raw string rather than to an error.
 */
data class CourseReportRow(
    val course: String,
    val counts: CourseReportCounts = CourseReportCounts(),
    /** Teacher names, wrapped across continuation lines in the CSV. Print only. */
    val teacherNames: List<String> = emptyList(),
) {
    val parsed: CourseName get() = CourseName.parse(course)
}

/**
 * The fourteen columns, grouped the way the centre dashboard already groups
 * numbers: NEW · OLD · roll total · SEVAK · TEACHERS. Nothing is dropped.
 */
data class CourseReportCounts(
    val newMale: Int = 0,
    val newFemale: Int = 0,
    val newTotal: Int = 0,
    val oldMale: Int = 0,
    val oldFemale: Int = 0,
    val oldTotal: Int = 0,
    val rollTotal: Int = 0,
    val sevakMale: Int = 0,
    val sevakFemale: Int = 0,
    val sevakTotal: Int = 0,
    /** Conducting, assistant and teaching-role counts, as the CSV names them. */
    val teacherConducting: Int = 0,
    val teacherAssistant: Int = 0,
    val teacherTrainee: Int = 0,
) {
    operator fun plus(other: CourseReportCounts) = CourseReportCounts(
        newMale = newMale + other.newMale,
        newFemale = newFemale + other.newFemale,
        newTotal = newTotal + other.newTotal,
        oldMale = oldMale + other.oldMale,
        oldFemale = oldFemale + other.oldFemale,
        oldTotal = oldTotal + other.oldTotal,
        rollTotal = rollTotal + other.rollTotal,
        sevakMale = sevakMale + other.sevakMale,
        sevakFemale = sevakFemale + other.sevakFemale,
        sevakTotal = sevakTotal + other.sevakTotal,
        teacherConducting = teacherConducting + other.teacherConducting,
        teacherAssistant = teacherAssistant + other.teacherAssistant,
        teacherTrainee = teacherTrainee + other.teacherTrainee,
    )
}

/**
 * `{centre} / {type} / {year} / {dates}` split for display.
 *
 * **A parse failure is not an error** — it prints the raw string as the row
 * title. Course naming on the desk is free text typed by staff, and a report
 * that refuses to draw because one row is punctuated unusually would be worse
 * than a report with one ugly title.
 */
data class CourseName(
    val type: String,
    val year: String = "",
    val dates: String = "",
    /** True when the string did not split; [type] is then the raw string. */
    val raw: Boolean = false,
) {
    companion object {
        fun parse(value: String): CourseName {
            val parts = value.split("/").map { it.trim() }.filter { it.isNotEmpty() }
            // centre / type / year / dates — anything shorter is not the shape.
            if (parts.size < 4) return CourseName(type = value.trim(), raw = true)
            val year = parts[parts.size - 2]
            if (!year.all { it.isDigit() }) return CourseName(type = value.trim(), raw = true)
            return CourseName(
                type = parts.subList(1, parts.size - 2).joinToString(" / "),
                year = year,
                dates = parts.last(),
            )
        }
    }
}
