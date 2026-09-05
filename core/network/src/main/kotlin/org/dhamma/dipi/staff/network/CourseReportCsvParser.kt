package org.dhamma.dipi.staff.network

import org.dhamma.dipi.staff.model.CourseReport
import org.dhamma.dipi.staff.model.CourseReportCounts
import org.dhamma.dipi.staff.model.CourseReportRow

/**
 * Parses the CSV streamed back by `dh_center_course_report_form` (v5 T3).
 *
 * **Records are not one per physical line.** The teacher column holds a list
 * of names, and the desk writes them with embedded newlines inside a quoted
 * field, so a course record can span several lines of the file. Splitting on
 * `\n` and calling each line a row silently truncates the report and invents
 * blank courses; this parser is a proper quote-aware scanner instead.
 *
 * Counts are read positionally after the header is matched by name, so a
 * reordered or renamed column degrades to zero rather than reading the wrong
 * figure into the wrong group.
 */
object CourseReportCsvParser {

    /** Header aliases → the field they fill. Matching is case/space-insensitive. */
    private val COLUMNS: Map<String, (CourseReportCounts, Int) -> CourseReportCounts> = mapOf(
        "newmale" to { c, v -> c.copy(newMale = v) },
        "newfemale" to { c, v -> c.copy(newFemale = v) },
        "newtotal" to { c, v -> c.copy(newTotal = v) },
        "oldmale" to { c, v -> c.copy(oldMale = v) },
        "oldfemale" to { c, v -> c.copy(oldFemale = v) },
        "oldtotal" to { c, v -> c.copy(oldTotal = v) },
        "studenttotal" to { c, v -> c.copy(rollTotal = v) },
        "total" to { c, v -> c.copy(rollTotal = v) },
        "sevakmale" to { c, v -> c.copy(sevakMale = v) },
        "sevakfemale" to { c, v -> c.copy(sevakFemale = v) },
        "sevaktotal" to { c, v -> c.copy(sevakTotal = v) },
        "conductingteacher" to { c, v -> c.copy(teacherConducting = v) },
        "assistantteacher" to { c, v -> c.copy(teacherAssistant = v) },
        "teachertrainee" to { c, v -> c.copy(teacherTrainee = v) },
    )

    private const val COURSE = "course"
    private const val TEACHERS = "teachers"

    fun parse(csv: String, from: String = "", to: String = ""): CourseReport {
        val records = records(csv)
        if (records.isEmpty()) return CourseReport(from = from, to = to)

        val header = records.first().map { it.normalise() }
        val courseAt = header.indexOfFirst { it == COURSE }.takeIf { it >= 0 } ?: 0
        val teachersAt = header.indexOfFirst { it.startsWith(TEACHERS) }

        val rows = mutableListOf<CourseReportRow>()
        var desksTotal: CourseReportCounts? = null

        records.drop(1).forEach { record ->
            val course = record.getOrNull(courseAt)?.trim().orEmpty()
            // A row with no course name is not a course. The live desk answers a
            // range with no courses with one blank-name, all-zero row; keeping it
            // would show a ghost row and suppress the empty-range guidance.
            if (course.isBlank()) return@forEach
            var counts = CourseReportCounts()
            header.forEachIndexed { i, name ->
                val fill = COLUMNS[name] ?: return@forEachIndexed
                counts = fill(counts, record.getOrNull(i).toCount())
            }
            // The desk's own trailing Total line is the grand total, not a course.
            if (course.equals("total", ignoreCase = true)) {
                desksTotal = counts
                return@forEach
            }
            rows += CourseReportRow(
                course = course,
                counts = counts,
                teacherNames = teacherNames(record.getOrNull(teachersAt)),
            )
        }

        return CourseReport(
            rows = rows,
            grandTotal = desksTotal
                ?: rows.fold(CourseReportCounts()) { acc, r -> acc + r.counts },
            from = from,
            to = to,
        )
    }

    /** `"Anil Kale\nSuma Rao"` → two names. Blank entries are dropped. */
    private fun teacherNames(cell: String?): List<String> =
        cell.orEmpty()
            .split('\n', ';', '|')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /**
     * A quote-aware CSV scanner. `"` opens a quoted field in which `,` and
     * newlines are literal and `""` is an escaped quote — which is exactly
     * how the desk writes a wrapped teacher list, and exactly what a
     * `split("\n")` parse gets wrong.
     */
    internal fun records(csv: String): List<List<String>> {
        val records = mutableListOf<List<String>>()
        var record = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        var sawAnything = false

        fun endField() {
            record.add(field.toString())
            field.setLength(0)
        }

        fun endRecord() {
            endField()
            if (record.any { it.isNotBlank() }) records.add(record.toList())
            record = mutableListOf()
        }

        while (i < csv.length) {
            val ch = csv[i]
            when {
                quoted && ch == '"' && i + 1 < csv.length && csv[i + 1] == '"' -> {
                    field.append('"')
                    i++
                }
                ch == '"' -> quoted = !quoted
                !quoted && ch == ',' -> endField()
                !quoted && (ch == '\n' || ch == '\r') -> {
                    // Swallow CRLF as one break.
                    if (ch == '\r' && i + 1 < csv.length && csv[i + 1] == '\n') i++
                    endRecord()
                }
                else -> field.append(ch)
            }
            sawAnything = true
            i++
        }
        if (sawAnything && (field.isNotEmpty() || record.isNotEmpty())) endRecord()
        return records
    }

    private fun String.normalise(): String =
        trim().lowercase().replace(Regex("""[\s_.\-/]"""), "")

    private fun String?.toCount(): Int =
        this?.trim()?.let { Regex("""-?\d+""").find(it)?.value?.toIntOrNull() } ?: 0
}
