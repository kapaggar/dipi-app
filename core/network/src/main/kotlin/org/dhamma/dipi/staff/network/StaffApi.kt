package org.dhamma.dipi.staff.network

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.dhamma.dipi.staff.model.SheetExport
import org.dhamma.dipi.staff.model.SheetPayload
import org.dhamma.dipi.staff.model.SheetSort
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.io.File
import java.time.LocalDate

interface DrupalAuthApi {
    @POST("/api/user/login")
    suspend fun login(@Body body: LoginBody): LoginDto

    @POST("/api/user/logout")
    suspend fun logout()

    @GET("/services/session/token")
    suspend fun csrfToken(): ResponseBody
}

interface StaffApi {
    @GET("/staff/session")
    suspend fun session(): SessionDto

    @GET("/staff/centres/{cid}/courses")
    suspend fun courses(
        @Path("cid") centreId: Int,
        @Query("upcoming") upcoming: Int = 1,
    ): CourseListDto

    @GET("/staff/courses/{id}/applicants")
    suspend fun applicants(
        @Path("id") courseId: Int,
        @Query("status") status: String? = null,
        @Query("q") q: String? = null,
        @Query("cursor") cursor: String? = null,
    ): ApplicantListDto

    @GET("/staff/applicants/{id}")
    suspend fun applicant(@Path("id") id: Int): ApplicantDto

    @GET("/staff/meta/statuses")
    suspend fun statuses(): StatusesDto

    @GET("/staff/courses/{id}/photo-review")
    suspend fun photoReview(@Path("id") courseId: Int): PhotoReviewListDto

    @POST("/staff/applicants/{id}/photo")
    suspend fun uploadPhoto(@Path("id") id: Int, @Body body: PhotoUploadBody): PhotoUploadResultDto

    @GET("/change-status/{id}")
    suspend fun changeStatusGet(
        @Path("id") id: Int,
        @Query("s") status: String,
        @Query("l") letterId: Int = 0,
        @Query("c") comment: String = "",
    ): ChangeStatusDto

    /** Canonical v1 write. Always send l=0. Never send s=Approved from the sheet. */
    @POST("/change-status/{id}")
    suspend fun changeStatus(
        @Path("id") id: Int,
        @Query("s") status: String,
        @Query("l") letterId: Int = 0,
        @Query("c") comment: String = "",
    ): ChangeStatusDto

    /**
     * The worklist dialog's own allocation write (owner amendment 2026-08-16):
     * `dh_app_update_attended` — plain menu callback, session cookie only, no
     * form token. Fields are exactly the dialog's `s,r,g,l,v,c,cf,chow,chai,
     * back,comment,a` (build via RoomAllocSync.params). No status, no NPI.
     */
    @FormUrlEncoded
    @POST("/app-update-attended/{id}")
    suspend fun updateAttended(
        @Path("id") id: Int,
        @FieldMap fields: Map<String, String>,
    ): AttendedUpdateDto

    /** HAR: anonymous GET / is 403 but includes user_login_block. */
    @GET("/")
    suspend fun siteRoot(): Response<ResponseBody>

    /** 200 HTML (unlike GET / which is 403). Use when the desk 403 page has no form. */
    @GET("/user/login")
    suspend fun userLogin(): Response<ResponseBody>

    @FormUrlEncoded
    @POST("/home")
    suspend fun loginBlock(
        @Query("destination") destination: String = "home",
        @Field("name") name: String,
        @Field("pass") pass: String,
        @Field("form_build_id") formBuildId: String,
        @Field("form_id") formId: String = "user_login_block",
        @Field("op") op: String = "Log in",
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST
    suspend fun submitLogin(
        @Url action: String,
        @Field("name") name: String,
        @Field("pass") pass: String,
        @Field("form_build_id") formBuildId: String,
        @Field("form_id") formId: String,
        @Field("op") op: String = "Log in",
    ): Response<ResponseBody>

    @GET("/user/logout")
    suspend fun logoutGet(): Response<ResponseBody>

    @GET("/centre")
    suspend fun centreLanding(): Response<ResponseBody>

    @GET("/centre/{cid}")
    suspend fun centreDashboard(@Path("cid") centreId: Int): Response<ResponseBody>

    /**
     * Centre room config: the DataTables source the browser loads for
     * `/centre/{cid}/edit`'s Accommodation table. GET only — read-only here.
     */
    @GET("/centre/{cid}/acco-handler")
    suspend fun accoHandler(@Path("cid") centreId: Int): Response<ResponseBody>

    @GET("/search-course/{cid}/{courseId}")
    suspend fun searchCourse(
        @Path("cid") centreId: Int,
        @Path("courseId") courseId: Int,
        @Query("s") status: String = "",
        @Query("t") old: String = "",
        @Query("g") gender: String = "",
        @Query("d") db: String = "a",
    ): Response<ResponseBody>

    /**
     * Print-styled desk sheet HTML (day0-list, teacher-list, manager-list,
     * student-chit, checking-slip, seating, zero-day).
     *
     * SAFETY: the seating/teacher-list/cell-list handlers run server-side
     * BULK SEAT AUTO-ALLOCATION whenever an `r` param is merely present
     * (inc/zero-day.inc:17-46). The only query parameters this method can
     * carry are therefore the two named, nullable re-orderings below —
     * declared one-per-parameter rather than as a `@QueryMap`, so widening
     * the surface takes a code change that `SheetRouteSafetyTest` will fail.
     * Both are null by default and Retrofit omits null queries entirely.
     *
     * @param conf `1` for `?conf=1` (day0-list, sort by confirmation number).
     * @param seating `1` for `?seating=1` (teacher-list / student-chit order).
     */
    @GET("/{sheet}/{cid}/{courseId}")
    suspend fun sheetPage(
        @Path("sheet") sheet: String,
        @Path("cid") centreId: Int,
        @Path("courseId") courseId: Int,
        @Query("conf") conf: Int? = null,
        @Query("seating") seating: Int? = null,
    ): Response<ResponseBody>

    /** Streamed laundry/valuable list (vnd.ms-excel) and Day-11 PDF. */
    @Streaming
    @GET("/{sheet}/{cid}/{courseId}")
    suspend fun sheetDocument(
        @Path("sheet") sheet: String,
        @Path("cid") centreId: Int,
        @Path("courseId") courseId: Int,
    ): Response<ResponseBody>

    /**
     * The desk's own application edit form (perm `edit application`,
     * dh_manageapp.module:325). Display-only in the app: the page includes
     * NPI, so the body must stay in memory — never persisted, never logged.
     */
    @GET("/app/{id}/edit")
    suspend fun appEditPage(@Path("id") id: Int): Response<ResponseBody>

    /**
     * The application as the applicant wrote it (course-ops student card,
     * spec 2d). Full themed Drupal page; the client parses ONLY the header,
     * Personal, Course History and Health via [ApplicationViewParser] —
     * everything else on the page is NPI and never read. Path param only:
     * no query, ever (the no-`r` rule is structural).
     */
    @GET("/application-view/{id}")
    suspend fun applicationView(@Path("id") id: Int): Response<ResponseBody>

    @GET("/app-courses/{id}")
    suspend fun appCourses(@Path("id") id: Int): Response<ResponseBody>

    @GET("/app-activity/{id}")
    suspend fun appActivity(@Path("id") id: Int): Response<ResponseBody>

    @GET("/app-clarifications/{id}")
    suspend fun appClarifications(@Path("id") id: Int): Response<ResponseBody>

    @Streaming
    @GET("/show-clarification/{appId}/{clarId}")
    suspend fun showClarification(
        @Path("appId") appId: Int,
        @Path("clarId") clarId: Int,
    ): Response<ResponseBody>

    /**
     * Course report is `drupal_get_form(dh_center_course_report_form)`
     * (inc/reports.inc:111), not a plain GET: scrape this form, then POST it.
     */
    @GET("/centre/{cid}/course-report")
    suspend fun courseReportForm(@Path("cid") centreId: Int): Response<ResponseBody>

    /** Submit of the scraped course-report form; the reply streams text/csv. */
    @FormUrlEncoded
    @Streaming
    @POST
    suspend fun submitCourseReportForm(
        @Url action: String,
        @FieldMap fields: Map<String, String>,
    ): Response<ResponseBody>
}

/** Live-desk delivery shape of one Board "Sheets & exports" cell. */
sealed interface SheetRoute {
    /** Print-styled HTML at `GET /{slug}/{cid}/{courseId}` — displayed in memory. */
    data class Page(val slug: String) : SheetRoute

    /** The `#day-summary` block extracted from `GET /zero-day/{cid}/{courseId}`. */
    data object DaySummary : SheetRoute

    /** Streamed binary at `GET /{slug}/{cid}/{courseId}` → cacheDir/sheets. */
    data class Document(val slug: String, val mimeType: String, val extension: String) : SheetRoute

    /** Drupal form scrape + POST at `/centre/{cid}/course-report` → CSV. */
    data object ReportForm : SheetRoute
}

/**
 * Path→export mapping, keyed off the [SheetExport] seam enum. Every route is
 * a plain GET the desk browser already triggers (routers dh_manageapp.module
 * :303–:441) except the course report, which is the desk's own form POST.
 */
object SheetRoutes {
    const val MIME_PDF = "application/pdf"
    const val MIME_XLS = "application/vnd.ms-excel"
    const val MIME_CSV = "text/csv"

    fun of(export: SheetExport): SheetRoute = when (export) {
        SheetExport.Day0List -> SheetRoute.Page("day0-list")
        SheetExport.Day0Summary -> SheetRoute.DaySummary
        SheetExport.StudentChit -> SheetRoute.Page("student-chit")
        SheetExport.CheckingSlip -> SheetRoute.Page("checking-slip")
        SheetExport.TeacherList -> SheetRoute.Page("teacher-list")
        SheetExport.ManagerList -> SheetRoute.Page("manager-list")
        SheetExport.LaundryList -> SheetRoute.Document("laundry-list", MIME_XLS, "xls")
        SheetExport.ValuableList -> SheetRoute.Document("valuable-list", MIME_XLS, "xls")
        SheetExport.SeatingPlan -> SheetRoute.Page("seating")
        SheetExport.CourseReport -> SheetRoute.ReportForm
        SheetExport.Day11Report -> SheetRoute.Document("report-day11", MIME_PDF, "pdf")
    }
}

/**
 * Fetches Board sheets from the live desk with ZERO persistence of sheet
 * bodies (they carry health disclosures and contact data): HTML payloads
 * stay in memory, PDF/Excel/CSV stream to [sheetsDir] (cacheDir) only, and
 * every server refusal surfaces verbatim as [SheetPayload.NotAvailable] —
 * no client-side permission checks, no logging of bodies, ever.
 */
class SheetTransport(
    private val api: StaffApi,
    private val baseUrl: String,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val sheetsDir: () -> File,
) {
    suspend fun fetch(
        export: SheetExport,
        centreId: Int,
        courseId: Int,
        sort: SheetSort = SheetSort.Default,
    ): SheetPayload = guarded(export.label) {
        when (val route = SheetRoutes.of(export)) {
            is SheetRoute.Page ->
                htmlPayload(export.label, sheetPage(route.slug, centreId, courseId, export, sort))
            SheetRoute.DaySummary ->
                daySummary(api.sheetPage("zero-day", centreId, courseId))
            is SheetRoute.Document ->
                document(export.label, route, centreId, courseId)
            SheetRoute.ReportForm ->
                courseReport(centreId)
        }
    }

    /**
     * The one place a sort parameter is turned into a request. A [SheetSort]
     * is only honoured when [SheetSort.optionsFor] lists it for this export,
     * so a stale sort left over from another sheet degrades to the page's own
     * default order rather than travelling onto a slug that never offered it.
     */
    private suspend fun sheetPage(
        slug: String,
        centreId: Int,
        courseId: Int,
        export: SheetExport,
        sort: SheetSort,
    ): Response<ResponseBody> {
        val effective = if (sort in SheetSort.optionsFor(export)) sort else SheetSort.Default
        return api.sheetPage(
            sheet = slug,
            centreId = centreId,
            courseId = courseId,
            conf = if (effective == SheetSort.ConfirmationNo) 1 else null,
            seating = if (effective == SheetSort.SeatingOrder) 1 else null,
        )
    }

    suspend fun appEditPage(id: Int): SheetPayload = guarded("Application $id") {
        htmlPayload("Application $id", api.appEditPage(id))
    }

    suspend fun clarificationPdf(appId: Int, clarId: Int): SheetPayload = guarded("Clarification") {
        save(
            title = "Clarification $clarId",
            fileName = "clarification-$appId-$clarId.pdf",
            fallbackMime = SheetRoutes.MIME_PDF,
            resp = api.showClarification(appId, clarId),
        )
    }

    /** Sheet bodies never outlive the session: wipe every cached document. */
    fun wipe() {
        runCatching { sheetsDir().deleteRecursively() }
    }

    private suspend fun guarded(label: String, block: suspend () -> SheetPayload): SheetPayload =
        try {
            withContext(io) { block() }
        } catch (e: Throwable) {
            runCatching {
                android.util.Log.w("dipi-sheets", "fetch $label failed ${e.javaClass.simpleName}")
            }
            SheetPayload.NotAvailable(sheetFailureMessage(label, e))
        }

    private fun htmlPayload(title: String, resp: Response<ResponseBody>): SheetPayload {
        val html = resp.html()
        if (!resp.isSuccessful) return refusal(resp.code(), html)
        return SheetPayload.Html(title, html, baseUrl)
    }

    /**
     * v5 T2: the `#day-summary` fragment is parsed into counts instead of
     * being handed to the WebView. It arrives with no stylesheet, so as HTML
     * it can only ever render browser-default; the numbers are what the desk
     * actually reads off it.
     */
    private fun daySummary(resp: Response<ResponseBody>): SheetPayload {
        val html = resp.html()
        if (!resp.isSuccessful) return refusal(resp.code(), html)
        val block = extractElementById(html, "day-summary")
            ?: return SheetPayload.NotAvailable("The zero-day page has no #day-summary block")
        return SheetPayload.Summary(SheetExport.Day0Summary.label, DaySummaryParser.parse(block))
    }

    private suspend fun document(
        title: String,
        route: SheetRoute.Document,
        centreId: Int,
        courseId: Int,
    ): SheetPayload = save(
        title = title,
        fileName = "${route.slug}-$centreId-$courseId.${route.extension}",
        fallbackMime = route.mimeType,
        resp = api.sheetDocument(route.slug, centreId, courseId),
    )

    private suspend fun courseReport(centreId: Int): SheetPayload {
        val formResp = api.courseReportForm(centreId)
        val formHtml = formResp.html()
        if (!formResp.isSuccessful) return refusal(formResp.code(), formHtml)
        val form = CourseReportFormParser.parse(formHtml)
            ?: return SheetPayload.NotAvailable(
                "Could not read the Course report form — open /centre/$centreId/course-report in a desk browser",
            )
        return save(
            title = SheetExport.CourseReport.label,
            fileName = "course-report-$centreId.csv",
            fallbackMime = SheetRoutes.MIME_CSV,
            resp = api.submitCourseReportForm(form.action, form.fields),
        )
    }

    /**
     * v5 T3: the same scrape-then-POST as [courseReport], with the desk's own
     * two date fields overridden by the range the registrar typed, and the
     * CSV parsed for the native surface instead of handed straight to a
     * system viewer.
     *
     * **The range is the only input.** The form offers no course picker, no
     * status filter and no sort, so the app offers none either. The CSV is
     * still written to `cacheDir/sheets` so `Share CSV` keeps working.
     */
    suspend fun courseReport(
        centreId: Int,
        from: String,
        to: String,
    ): SheetPayload = guarded(SheetExport.CourseReport.label) {
        val formResp = api.courseReportForm(centreId)
        val formHtml = formResp.html()
        if (!formResp.isSuccessful) return@guarded refusal(formResp.code(), formHtml)
        val form = CourseReportFormParser.parse(formHtml)
            ?: return@guarded SheetPayload.NotAvailable(
                "Could not read the Course report form — open /centre/$centreId/course-report in a desk browser",
            )
        val fields = form.fields.toMutableMap().apply {
            if (from.isNotBlank()) put("report_from_date[date]", from)
            if (to.isNotBlank()) put("report_to_date[date]", to)
        }
        val saved = save(
            title = SheetExport.CourseReport.label,
            fileName = "course-report-$centreId.csv",
            fallbackMime = SheetRoutes.MIME_CSV,
            resp = api.submitCourseReportForm(form.action, fields),
        )
        when (saved) {
            is SheetPayload.Document -> SheetPayload.Report(
                title = saved.title,
                report = CourseReportCsvParser
                    .parse(saved.file.readText(), from = from, to = to)
                    .copy(csv = saved.file),
            )
            // A refusal renders verbatim — no rewording, no client-side gate.
            else -> saved
        }
    }

    /**
     * Consume a streamed body off the caller thread. `@Streaming` returns
     * when headers arrive; `body.bytes()` is still a network read and
     * crashes on Main (`NetworkOnMainThreadException`) or sits on the
     * read timeout. Always hop to [io] before touching the stream.
     */
    private suspend fun save(
        title: String,
        fileName: String,
        fallbackMime: String,
        resp: Response<ResponseBody>,
    ): SheetPayload = withContext(io) {
        if (!resp.isSuccessful) return@withContext refusal(resp.code(), resp.html())
        val body = resp.body() ?: return@withContext SheetPayload.NotAvailable("$title came back empty")
        val type = body.contentType()
        val mime = type?.let { "${it.type}/${it.subtype}" }
        val bytes = body.bytes()
        runCatching {
            android.util.Log.i(
                "dipi-sheets",
                "document $title status=${resp.code()} mime=$mime bytes=${bytes.size}",
            )
        }
        // A refusal or re-rendered form served as 200 HTML must never be
        // saved masquerading as a document.
        if (type?.type == "text" && type.subtype == "html") {
            return@withContext SheetPayload.NotAvailable(bytes.toString(Charsets.UTF_8))
        }
        if (bytes.isEmpty()) {
            return@withContext SheetPayload.NotAvailable("$title came back empty")
        }
        // S3 miss still sends application/pdf; if the body is a text error,
        // surface those words instead of handing a broken file to a viewer.
        if (fallbackMime == SheetRoutes.MIME_PDF && !bytes.startsWithPdf()) {
            val text = bytes.toString(Charsets.UTF_8).trim()
            if (text.startsWith("<") || text.startsWith("{") || looksLikePlainError(text)) {
                return@withContext SheetPayload.NotAvailable(text.ifBlank { "$title came back empty" })
            }
        }
        val dir = sheetsDir().apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeBytes(bytes)
        SheetPayload.Document(title, file, mime ?: fallbackMime)
    }

    /** Server refusals render verbatim (hard rule 1) — never a client-side gate. */
    private fun refusal(code: Int, body: String): SheetPayload =
        SheetPayload.NotAvailable(body.ifBlank { "HTTP $code" })
}

/** IOException → the offline sentence; any other throwable keeps e.message. */
internal fun sheetFailureMessage(label: String, e: Throwable): String = when (e) {
    is java.io.IOException -> "Offline — could not reach the desk for $label"
    else -> e.message?.takeIf { it.isNotBlank() } ?: "Could not fetch $label"
}

private fun ByteArray.startsWithPdf(): Boolean =
    size >= 4 && this[0] == '%'.code.toByte() && this[1] == 'P'.code.toByte() &&
        this[2] == 'D'.code.toByte() && this[3] == 'F'.code.toByte()

private fun looksLikePlainError(text: String): Boolean =
    text.length < 800 && text.none { it.code < 9 || (it.code in 14..31) }

/**
 * Extracts one element (opening tag through matching close) by id, keeping
 * the server's markup verbatim inside the block.
 */
internal fun extractElementById(html: String, id: String): String? {
    val open = Regex("""<([a-zA-Z][a-zA-Z0-9]*)\b[^>]*\bid\s*=\s*["']${Regex.escape(id)}["'][^>]*>""")
        .find(html) ?: return null
    val tag = open.groupValues[1]
    val start = open.range.first
    var depth = 0
    for (m in Regex("""</?${Regex.escape(tag)}\b[^>]*>""", RegexOption.IGNORE_CASE).findAll(html, start)) {
        if (m.value.startsWith("</")) {
            depth -= 1
            if (depth == 0) return html.substring(start, m.range.last + 1)
        } else {
            depth += 1
        }
    }
    return null
}

/**
 * Hidden-field scrape of `dh_center_course_report_form` — the same pattern
 * as the login-form scrape. The POSTed date range mirrors the form's own
 * defaults (last year → today), exactly what the desk sends when staff click
 * "Download Course Report" without touching the pickers.
 */
internal object CourseReportFormParser {
    const val FORM_ID = "dh_center_course_report_form"
    const val SUBMIT_LABEL = "Download Course Report"

    data class ParsedForm(val action: String, val fields: Map<String, String>)

    fun parse(html: String): ParsedForm? {
        if (!html.contains(FORM_ID)) return null
        val buildId = inputValue(html, "form_build_id") ?: return null
        val action = Regex("""<form\b[^>]*\baction\s*=\s*["']([^"']+)["']""").find(html)
            ?.groupValues?.get(1) ?: return null
        val today = LocalDate.now()
        val fields = buildMap {
            put("report_from_date[date]", inputValue(html, "report_from_date[date]") ?: today.minusYears(1).toString())
            put("report_to_date[date]", inputValue(html, "report_to_date[date]") ?: today.toString())
            put("form_build_id", buildId)
            inputValue(html, "form_token")?.let { put("form_token", it) }
            put("form_id", FORM_ID)
            put("op", SUBMIT_LABEL)
        }
        return ParsedForm(action, fields)
    }

    private fun inputValue(html: String, name: String): String? {
        val n = Regex.escape(name)
        return Regex("""<input\b[^>]*\bname\s*=\s*["']$n["'][^>]*\bvalue\s*=\s*["']([^"']*)["']""").find(html)
            ?.groupValues?.get(1)
            ?: Regex("""<input\b[^>]*\bvalue\s*=\s*["']([^"']*)["'][^>]*\bname\s*=\s*["']$n["']""").find(html)
                ?.groupValues?.get(1)
    }
}
