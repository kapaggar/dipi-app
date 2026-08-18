package org.dhamma.dipi.staff.model

import java.io.File

/**
 * The Board export cells, keyed by the exact label BoardPane emits.
 * Path templates and delivery shape live in the network layer; this enum is
 * the seam between the transport (StaffRepository.fetchSheet) and the UI.
 */
enum class SheetExport(val label: String) {
    Day0List("Day 0 list"),
    Day0Summary("Day 0 summary"),
    StudentChit("Student chit"),
    CheckingSlip("Checking slip"),
    MalePdf("Male PDF"),
    FemalePdf("Female PDF"),
    TeacherList("Teacher list"),
    ManagerList("Manager list"),
    LaundryList("Laundry list"),
    ValuableList("Valuable list"),
    SeatingPlan("Seating plan"),
    CourseReport("Course report"),
    Day11Report("Course summary report"),
    ;

    companion object {
        fun fromLabel(label: String): SheetExport? = entries.firstOrNull { it.label == label }
    }
}

/**
 * What a fetched sheet comes back as. Sheet bodies are never persisted:
 * [Html] stays in memory, [Document] lives in cacheDir only and is wiped on
 * logout / erase-all / next launch.
 */
sealed interface SheetPayload {
    /** Print-styled desk HTML, rendered in the in-app viewer (JS off, no cookies). */
    data class Html(val title: String, val html: String, val baseUrl: String) : SheetPayload

    /** Streamed PDF or Excel written under cacheDir/sheets, opened via FileProvider. */
    data class Document(val title: String, val file: File, val mimeType: String) : SheetPayload

    /** Server refusal (403 page, form-only report, offline) rendered verbatim. */
    data class NotAvailable(val message: String) : SheetPayload
}
