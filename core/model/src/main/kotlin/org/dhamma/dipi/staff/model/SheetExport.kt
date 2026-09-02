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

    /**
     * Day 11 end-of-course summary. Phone hub overflow (`hubSheetLabel`) and
     * the Board's fourth-line chip (2026-08-31) both send this label.
     */
    Day11Report("Course summary report"),
    ;

    companion object {
        fun fromLabel(label: String): SheetExport? = entries.firstOrNull { it.label == label }
    }
}

/**
 * The only query parameters this app will ever put on a sheet GET.
 *
 * This is a closed enum, not a map, and that is the whole point: the desk's
 * seating / teacher-list / cell-list handlers run **server-side bulk seat
 * auto-allocation whenever an `r` parameter is merely present**
 * (`inc/zero-day.inc:17-46`). A free-form parameter surface is one typo away
 * from silently reshuffling every student's seat, so the surface is two
 * names, both verified in `version-5/HAR-ROUTES.md` as ordinary re-orderings
 * that the desk browser itself sends:
 *
 * - `conf=1` — Day 0 list, sort by confirmation number instead of name.
 * - `seating=1` — teacher list and student chit, order by seating plan.
 *
 * `SheetRouteSafetyTest.noSheetGetCanCarryAnRParam` fails if anything widens
 * this. Do not add a case without a HAR line proving the parameter is inert.
 */
enum class SheetSort(val queryName: String) {
    /** The page's own default order — no parameter is sent at all. */
    Default(""),

    /** `?conf=1` on `day0-list`. */
    ConfirmationNo("conf"),

    /** `?seating=1` on `teacher-list` and `student-chit`. */
    SeatingOrder("seating"),
    ;

    companion object {
        /** Every parameter name this app may put on a sheet GET, and no other. */
        val ALLOWED_QUERY_NAMES: Set<String> = setOf("conf", "seating")

        /** The alternate orders [export] actually exposes, default first. */
        fun optionsFor(export: SheetExport): List<SheetSort> = when (export) {
            SheetExport.Day0List -> listOf(Default, ConfirmationNo)
            SheetExport.TeacherList, SheetExport.StudentChit -> listOf(Default, SeatingOrder)
            else -> listOf(Default)
        }

        /** Screen label for the segmented control; the sheet's own wording. */
        fun labelFor(export: SheetExport, sort: SheetSort): String = when (sort) {
            Default -> if (export == SheetExport.Day0List) "Name" else "Seniority"
            ConfirmationNo -> "Confirmation no."
            SeatingOrder -> "Seating plan"
        }
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

    /**
     * The Day 0 summary, parsed into counts rather than handed to the WebView
     * (v5 T2). The `#day-summary` fragment arrives with no stylesheet, so as
     * HTML it renders browser-default; as numbers it becomes a native screen.
     * Counts only — nothing here is NPI, and nothing here is persisted.
     */
    data class Summary(val title: String, val summary: DaySummary) : SheetPayload

    /**
     * The centre course report, parsed for the native surface (v5 T3). The
     * streamed CSV still lands in `cacheDir/sheets` and travels on
     * [CourseReport.csv] so `Share CSV` keeps working — the raw text is never
     * the default path.
     */
    data class Report(val title: String, val report: CourseReport) : SheetPayload

    /** Server refusal (403 page, form-only report, offline) rendered verbatim. */
    data class NotAvailable(val message: String) : SheetPayload
}
