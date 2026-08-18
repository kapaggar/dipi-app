package org.dhamma.dipi.staff.network

import org.dhamma.dipi.staff.model.DailyActivityForm
import org.dhamma.dipi.staff.model.DailyActivityPage
import org.dhamma.dipi.staff.model.DailyActivityRow
import org.dhamma.dipi.staff.model.NamedOption
import java.time.LocalDate

/**
 * `dh_daily_activity_form` (`dh_manageapp.module:1581`) + `#table-daily-activity`
 * (`:1685`). Empty results omit the table entirely.
 */
object DailyActivityParser {
    const val FORM_ID = "dh_daily_activity_form"
    const val SUBMIT = "Submit"
    const val TABLE_ID = "table-daily-activity"

    fun parse(html: String): DailyActivityPage =
        DailyActivityPage(form = form(html), rows = rows(html))

    fun form(html: String): DailyActivityForm? {
        if (!html.contains(FORM_ID)) return null
        val buildId = HtmlForms.inputValue(html, "form_build_id") ?: return null
        val today = LocalDate.now().toString()
        return DailyActivityForm(
            action = HtmlForms.formAction(html) ?: "",
            formBuildId = buildId,
            formToken = HtmlForms.inputValue(html, "form_token"),
            formId = HtmlForms.inputValue(html, "form_id") ?: FORM_ID,
            courses = options(html, "edit-course"),
            events = options(html, "edit-event"),
            users = options(html, "edit-user"),
            startDate = HtmlForms.inputValue(html, "date_start[date]") ?: today,
            endDate = HtmlForms.inputValue(html, "date_end[date]") ?: today,
        )
    }

    fun rows(html: String): List<DailyActivityRow> =
        HtmlTables.rowsById(html, TABLE_ID).map { cells ->
            DailyActivityRow(
                applicant = cells.getOrElse(0) { "" },
                course = cells.getOrElse(1) { "" },
                event = cells.getOrElse(2) { "" },
                message = cells.getOrElse(3) { "" },
                user = cells.getOrElse(4) { "" },
                at = cells.getOrElse(5) { "" },
            )
        }

    fun fields(
        form: DailyActivityForm,
        start: String,
        end: String,
        event: String = "",
        course: String = "",
        user: String = "",
    ): Map<String, String> = buildMap {
        put("course", course)
        put("date_start[date]", start)
        put("date_end[date]", end)
        put("event", event)
        put("user", user)
        put("form_build_id", form.formBuildId)
        form.formToken?.let { put("form_token", it) }
        put("form_id", form.formId)
        put("op", SUBMIT)
    }

    private fun options(html: String, selectId: String): List<NamedOption> =
        SearchPageParser.selectOptionsRaw(html, selectId)
            .filter { (v, label) -> v.isNotBlank() && !label.equals("Choose", true) }
            .map { (v, label) -> NamedOption(v, label.ifBlank { v }) }
}
