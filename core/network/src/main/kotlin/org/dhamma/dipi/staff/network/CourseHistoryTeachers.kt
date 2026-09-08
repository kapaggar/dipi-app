package org.dhamma.dipi.staff.network

/**
 * Course History teacher names.
 *
 * Live `GET /application-view` (search.inc) prints First / Last Course as
 * date + location only. The paper form and some Course History HTML put
 * `Teacher(s)` under First Course / Most Recent Course. The desk edit form
 * stores the same facts as `ac_first_teacher_str` / `ac_last_teacher_str`.
 *
 * Only Course History rows or those two input names are read. Long Course
 * Details is never scanned. Values stay verbatim, including `Unknown`.
 */
object CourseHistoryTeachers {

    fun fromHistory(rows: List<Pair<String, String>>): Pair<String, String> {
        var first = ""
        var last = ""
        var section: String? = null
        val positional = mutableListOf<String>()
        for ((label, raw) in rows) {
            val value = raw.trim()
            when {
                label.equals("First Course Teacher", ignoreCase = true) -> {
                    if (first.isEmpty()) first = value
                }
                label.equals("Last Course Teacher", ignoreCase = true) -> {
                    if (last.isEmpty()) last = value
                }
                isFirstCourse(label) -> section = "first"
                isLastCourse(label) -> section = "last"
                isTeacherLabel(label) -> {
                    positional += value
                    when (section) {
                        "first" -> if (first.isEmpty()) first = value
                        "last" -> if (last.isEmpty()) last = value
                    }
                }
            }
        }
        if (first.isEmpty()) first = positional.getOrNull(0).orEmpty()
        if (last.isEmpty()) last = positional.getOrNull(1).orEmpty()
        return first to last
    }

    fun fromEditForm(html: String): Pair<String, String> =
        inputValue(html, "ac_first_teacher_str") to inputValue(html, "ac_last_teacher_str")

    private fun isFirstCourse(label: String) =
        label.equals("First Course", ignoreCase = true)

    private fun isLastCourse(label: String) =
        label.equals("Last Course", ignoreCase = true) ||
            label.startsWith("Most Recent", ignoreCase = true)

    private fun isTeacherLabel(label: String): Boolean {
        val t = label.trim()
        return t.equals("Teacher(s)", ignoreCase = true) ||
            t.equals("Teachers", ignoreCase = true) ||
            t.equals("Teacher", ignoreCase = true)
    }

    private val inputTagRe = Regex("""<input\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val nameRe = Regex("""\bname\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val valueRe = Regex("""\bvalue\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)

    private fun inputValue(html: String, fieldName: String): String {
        for (m in inputTagRe.findAll(html)) {
            val tag = m.value
            val name = nameRe.find(tag)?.groupValues?.get(1) ?: continue
            if (!name.equals(fieldName, ignoreCase = true)) continue
            return SearchPageParser.stripTags(valueRe.find(tag)?.groupValues?.get(1).orEmpty())
        }
        return ""
    }
}
