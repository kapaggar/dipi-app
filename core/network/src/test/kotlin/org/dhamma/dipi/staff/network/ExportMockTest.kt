package org.dhamma.dipi.staff.network

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.dhamma.dipi.staff.model.SheetExport
import org.dhamma.dipi.staff.model.SheetPayload
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Retrofit
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * All 12 Board "Sheets & exports" labels against the fixtures desk:
 * correct live paths (with cid/courseId substitution), session cookie on
 * every request, PDF/Excel/CSV streamed to the sheets cache dir with the
 * right mime, HTML back intact and in memory only, `#day-summary`
 * extraction, verbatim 403 refusals — and, hard safety rule, NO request
 * ever carries the `r` param that bulk auto-allocates seats server-side.
 */
class ExportMockTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var server: MockWebServer
    private lateinit var transport: SheetTransport
    private lateinit var sheetsDir: File

    private class FakeTokens : TokenStore {
        var cookie: String? = "SESS1234=deadbeef"
        override suspend fun sessionCookie() = cookie
        override suspend fun csrf(): String? = null
        override suspend fun saveSession(cookie: String?, csrf: String?) {
            this.cookie = cookie
        }
        override suspend fun clear() {
            cookie = null
        }
    }

    @Before
    fun start() {
        server = MockWebServer()
        server.dispatcher = DipiMockDispatcher()
        server.start()
        sheetsDir = File(tmp.root, "sheets")
        val client = OkHttpClient.Builder()
            .cookieJar(SessionCookieJar(FakeTokens()))
            .build()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .build()
            .create(StaffApi::class.java)
        transport = SheetTransport(api, server.url("/").toString()) { sheetsDir }
    }

    @After
    fun stop() {
        server.shutdown()
    }

    private fun fetch(export: SheetExport, cid: Int = 1, courseId: Int = 10): SheetPayload =
        runBlocking { transport.fetch(export, cid, courseId) }

    private fun recordedRequests(): List<okhttp3.mockwebserver.RecordedRequest> =
        (1..server.requestCount).mapNotNull { server.takeRequest(1, TimeUnit.SECONDS) }

    @Test
    fun allTwelveExportsHitTheirLiveDeskPaths() {
        SheetExport.entries.forEach { export ->
            val payload = fetch(export, cid = 3, courseId = 42)
            assertFalse(
                "${export.label} must resolve on the fixtures desk",
                payload is SheetPayload.NotAvailable,
            )
        }
        val requests = recordedRequests()
        val expected = listOf(
            "GET /day0-list/3/42",
            "GET /zero-day/3/42",
            "GET /student-chit/3/42",
            "GET /checking-slip/3/42",
            "GET /course-pdf-m/3/42",
            "GET /course-pdf-f/3/42",
            "GET /teacher-list/3/42",
            "GET /manager-list/3/42",
            "GET /laundry-list/3/42",
            "GET /valuable-list/3/42",
            "GET /seating/3/42",
            "GET /centre/3/course-report",
            "POST /centre/3/course-report",
        )
        assertEquals(expected, requests.map { "${it.method} ${it.path}" })
        requests.forEach { req ->
            assertTrue(
                "session cookie missing on ${req.path}",
                req.getHeader("Cookie").orEmpty().contains("SESS1234=deadbeef"),
            )
        }
    }

    @Test
    fun noRequestEverCarriesTheSeatAutoAllocationParam() {
        SheetExport.entries.forEach { fetch(it) }
        runBlocking { transport.appEditPage(MockFixtures.RAKESH_ID) }
        recordedRequests().forEach { req ->
            val url = req.requestUrl!!
            assertTrue("query params on ${req.path}", url.querySize == 0)
            assertFalse("r param on ${req.path} would bulk auto-allocate seats", url.queryParameterNames.contains("r"))
        }
    }

    @Test
    fun pdfExportsStreamToCacheFilesWithPdfMime() {
        listOf(
            SheetExport.MalePdf to "course-pdf-m-1-10.pdf",
            SheetExport.FemalePdf to "course-pdf-f-1-10.pdf",
        ).forEach { (export, name) ->
            val payload = fetch(export)
            assertTrue("$export should be a Document", payload is SheetPayload.Document)
            payload as SheetPayload.Document
            assertEquals("application/pdf", payload.mimeType)
            assertEquals(name, payload.file.name)
            assertEquals(sheetsDir, payload.file.parentFile)
            val bytes = payload.file.readBytes()
            assertTrue("PDF must be non-empty", bytes.isNotEmpty())
            assertTrue("PDF magic", bytes.toString(Charsets.US_ASCII).startsWith("%PDF"))
        }
    }

    @Test
    fun excelExportsStreamToCacheFilesWithExcelMime() {
        listOf(
            SheetExport.LaundryList to "laundry-list-1-10.xls",
            SheetExport.ValuableList to "valuable-list-1-10.xls",
        ).forEach { (export, name) ->
            val payload = fetch(export)
            assertTrue("$export should be a Document", payload is SheetPayload.Document)
            payload as SheetPayload.Document
            assertEquals("application/vnd.ms-excel", payload.mimeType)
            assertEquals(name, payload.file.name)
            assertTrue("Excel must be non-empty", payload.file.length() > 0)
        }
    }

    @Test
    fun htmlSheetsComeBackIntactAndInMemoryOnly() {
        val payload = fetch(SheetExport.Day0List)
        assertTrue(payload is SheetPayload.Html)
        payload as SheetPayload.Html
        assertEquals("Day 0 list", payload.title)
        assertEquals(MockFixtures.sheetHtml("day0-list", 1, 10), payload.html)
        assertEquals(server.url("/").toString(), payload.baseUrl)
        assertFalse("HTML sheets must never touch the cache dir", sheetsDir.exists())
    }

    @Test
    fun daySummaryIsTheExtractedBlockFromTheZeroDayPage() {
        val payload = fetch(SheetExport.Day0Summary)
        assertTrue(payload is SheetPayload.Html)
        payload as SheetPayload.Html
        assertTrue(payload.html.startsWith("<div id=\"day-summary\">"))
        assertTrue(payload.html.endsWith("</div>"))
        assertTrue("summary tables kept verbatim", payload.html.contains("table-totals"))
        assertTrue(payload.html.contains("<td><b>6</b></td>"))
        assertFalse("rest of the zero-day page must be dropped", payload.html.contains("Attended Applicants"))
    }

    @Test
    fun permissionRefusalSurfacesThe403BodyVerbatim() {
        val html = fetch(SheetExport.TeacherList, cid = MockFixtures.FORBIDDEN_CENTRE)
        assertTrue(html is SheetPayload.NotAvailable)
        assertEquals(MockFixtures.accessDeniedHtml, (html as SheetPayload.NotAvailable).message)

        val doc = fetch(SheetExport.MalePdf, cid = MockFixtures.FORBIDDEN_CENTRE)
        assertTrue(doc is SheetPayload.NotAvailable)
        assertEquals(MockFixtures.accessDeniedHtml, (doc as SheetPayload.NotAvailable).message)
        assertFalse("a refusal must never leave a cache file", sheetsDir.exists())
    }

    @Test
    fun courseReportScrapesTheFormThenStreamsTheCsv() {
        val payload = fetch(SheetExport.CourseReport)
        assertTrue("expected a CSV document, got $payload", payload is SheetPayload.Document)
        payload as SheetPayload.Document
        assertEquals("text/csv", payload.mimeType)
        assertEquals("course-report-1.csv", payload.file.name)
        assertEquals(MockFixtures.courseReportCsv, payload.file.readText())

        val requests = recordedRequests()
        assertEquals(listOf("GET", "POST"), requests.map { it.method })
        val post = requests[1].body.readUtf8()
        assertTrue("scraped token must be posted", post.contains("form_token=mock-form-token"))
        assertTrue("scraped build id must be posted", post.contains("form_build_id=form-MoCkBuIlDiD"))
        assertTrue("form's own default from-date", post.contains("report_from_date%5Bdate%5D=2025-08-16"))
        assertTrue("form's own default to-date", post.contains("report_to_date%5Bdate%5D=2026-08-16"))
    }

    @Test
    fun appEditPageIsDisplayOnlyHtml() {
        val payload = runBlocking { transport.appEditPage(MockFixtures.RAKESH_ID) }
        assertTrue(payload is SheetPayload.Html)
        payload as SheetPayload.Html
        assertTrue(payload.html.contains("dh-zero-app-form"))
        assertEquals("/app/${MockFixtures.RAKESH_ID}/edit", server.takeRequest(1, TimeUnit.SECONDS)!!.path)
        assertFalse("edit page must never be persisted", sheetsDir.exists())
    }

    @Test
    fun wipeRemovesEveryCachedDocument() {
        fetch(SheetExport.MalePdf)
        fetch(SheetExport.LaundryList)
        assertTrue(sheetsDir.listFiles()!!.isNotEmpty())
        transport.wipe()
        assertFalse(sheetsDir.exists())
    }

    @Test
    fun daySummaryExtractionHandlesNestedElements() {
        val html = """<div id="outer"><div id="day-summary"><div class="inner"><table></table></div></div><p>after</p></div>"""
        assertEquals(
            """<div id="day-summary"><div class="inner"><table></table></div></div>""",
            extractElementById(html, "day-summary"),
        )
    }
}
