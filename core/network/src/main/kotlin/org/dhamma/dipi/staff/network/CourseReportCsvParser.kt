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
        "conductingteachers" to { c, v -> c.copy(teacherConducting = v) },
        "assistantteacher" to { c, v -> c.copy(teacherAssistant = v) },
        "assistantteachers" to { c, v -> c.copy(teacherAssistant = v) },
        "assistingteacher" to { c, v -> c.copy(teacherAssistant = v) },
        "assistingteachers" to { c, v -> c.copy(teacherAssistant = v) },
        "teachertrainee" to { c, v -> c.copy(teacherTrainee = v) },
        "teachertrainees" to { c, v -> c.copy(teacherTrainee = v) },
        "trainingteacher" to { c, v -> c.copy(teacherTrainee = v) },
        "trainingteachers" to { c, v -> c.copy(teacherTrainee = v) },
    )

    private enum class TeacherSlot { CONDUCTING, ASSISTING, TRAINEE, ALL }

    /** Live CSV uses ConductingTeachers / AssistingTeachers as name lists. */
    private val NAME_SLOTS: Map<String, TeacherSlot> = mapOf(
        "conductingteacher" to TeacherSlot.CONDUCTING,
        "conductingteachers" to TeacherSlot.CONDUCTING,
        "assistantteacher" to TeacherSlot.ASSISTING,
        "assistantteachers" to TeacherSlot.ASSISTING,
        "assistingteacher" to TeacherSlot.ASSISTING,
        "assistingteachers" to TeacherSlot.ASSISTING,
        "teachertrainee" to TeacherSlot.TRAINEE,
        "teachertrainees" to TeacherSlot.TRAINEE,
        "trainingteacher" to TeacherSlot.TRAINEE,
        "trainingteachers" to TeacherSlot.TRAINEE,
        "teachers" to TeacherSlot.ALL,
    )

    private const val COURSE = "course"

    fun parse(csv: String, from: String = "", to: String = ""): CourseReport {
        val records = records(csv)
        if (records.isEmpty()) return CourseReport(from = from, to = to)

        val header = records.first().map { it.normalise() }
        val courseAt = header.indexOfFirst { it == COURSE }.takeIf { it >= 0 } ?: 0

        val rows = mutableListOf<CourseReportRow>()
        var desksTotal: CourseReportCounts? = null

        records.drop(1).forEach { record ->
            val course = record.getOrNull(courseAt)?.trim().orEmpty()
            // A row with no course name is not a course. The live desk answers a
            // range with no courses with one blank-name, all-zero row; keeping it
            // would show a ghost row and suppress the empty-range guidance.
            if (course.isBlank()) return@forEach
            var counts = CourseReportCounts()
            val conducting = mutableListOf<String>()
            val assisting = mutableListOf<String>()
            val trainees = mutableListOf<String>()
            val combined = mutableListOf<String>()
            header.forEachIndexed { i, name ->
                val cell = record.getOrNull(i)
                val fill = COLUMNS[name]
                if (fill != null && cell.looksNumeric()) {
                    counts = fill(counts, cell.toCount())
                }
                when (NAME_SLOTS[name]) {
                    TeacherSlot.CONDUCTING -> conducting += splitTeacherNames(cell)
                    TeacherSlot.ASSISTING -> assisting += splitTeacherNames(cell)
                    TeacherSlot.TRAINEE -> trainees += splitTeacherNames(cell)
                    TeacherSlot.ALL -> combined += splitTeacherNames(cell)
                    null -> Unit
                }
            }
            if (counts.teacherConducting == 0 && conducting.isNotEmpty()) {
                counts = counts.copy(teacherConducting = conducting.size)
            }
            if (counts.teacherAssistant == 0 && assisting.isNotEmpty()) {
                counts = counts.copy(teacherAssistant = assisting.size)
            }
            if (counts.teacherTrainee == 0 && trainees.isNotEmpty()) {
                counts = counts.copy(teacherTrainee = trainees.size)
            }
            // The desk's own trailing Total line is the grand total, not a course.
            if (course.equals("total", ignoreCase = true)) {
                desksTotal = counts
                return@forEach
            }
            rows += CourseReportRow(
                course = course,
                counts = counts,
                teacherNames = combined.ifEmpty { conducting + assisting + trainees },
                conductingTeachers = conducting,
                assistingTeachers = assisting,
                traineeTeachers = trainees,
            )
        }

        val derived = rows.fold(CourseReportCounts()) { acc, r -> acc + r.counts }
        val total = desksTotal?.let { desk ->
            val deskTeachers = desk.teacherConducting + desk.teacherAssistant + desk.teacherTrainee
            val rowTeachers = derived.teacherConducting + derived.teacherAssistant + derived.teacherTrainee
            if (deskTeachers == 0 && rowTeachers > 0) {
                desk.copy(
                    teacherConducting = derived.teacherConducting,
                    teacherAssistant = derived.teacherAssistant,
                    teacherTrainee = derived.teacherTrainee,
                )
            } else {
                desk
            }
        } ?: derived

        return CourseReport(
            rows = rows,
            grandTotal = total,
            from = from,
            to = to,
        )
    }

    /**
     * `"Anil Kale\nSuma Rao"` or `"Maya Sharma (F) Arun Sharma (M)"` → names.
     * A bare integer (the older count columns) is not a name.
     */
    private fun splitTeacherNames(cell: String?): List<String> {
        val raw = cell.orEmpty().trim()
        if (raw.isEmpty() || raw.looksNumeric()) return emptyList()
        return raw
            .split(Regex("""\r?\n|;|\|(?=\s)|(?<=\([MF]\))\s+"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.looksNumeric() }
    }

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

    private fun String?.looksNumeric(): Boolean {
        val t = this?.trim().orEmpty()
        return t.isEmpty() || t.matches(Regex("""-?\d+"""))
    }
}
