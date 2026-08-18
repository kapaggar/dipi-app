package org.dhamma.dipi.staff.network

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
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

class DeskReadMockTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var server: MockWebServer
    private lateinit var api: StaffApi
    private lateinit var transport: SheetTransport

    private class FakeTokens : TokenStore {
        var cookie: String? = "SESS1234=deadbeef"
        override suspend fun sessionCookie() = cookie
        override suspend fun csrf(): String? = null
        override suspend fun saveSession(cookie: String?, csrf: String?) { this.cookie = cookie }
        override suspend fun clear() { cookie = null }
    }

    @Before
    fun start() {
        server = MockWebServer()
        server.dispatcher = DipiMockDispatcher()
        server.start()
        val client = OkHttpClient.Builder().cookieJar(SessionCookieJar(FakeTokens())).build()
        api = Retrofit.Builder().baseUrl(server.url("/")).client(client).build().create(StaffApi::class.java)
        transport = SheetTransport(api, server.url("/").toString()) { File(tmp.root, "sheets") }
    }

    @After
    fun stop() { server.shutdown() }

    @Test
    fun dailyActivityPostsScrapedTokensThenReturnsTable() = runBlocking {
        val formHtml = api.dailyActivityForm(1).html()
        val form = DailyActivityParser.form(formHtml)!!
        val posted = api.submitDailyActivity(form.action, DailyActivityParser.fields(form, form.startDate, form.endDate, event = "Letter"))
        val rows = DailyActivityParser.rows(posted.html())
        assertEquals(2, rows.size)
        val reqs = (1..2).map { server.takeRequest(1, TimeUnit.SECONDS)!! }
        assertEquals("GET", reqs[0].method)
        assertEquals("/daily-activity/1", reqs[0].path)
        val body = reqs[1].body.readUtf8()
        assertTrue(body.contains("form_token=mock-form-token"))
        assertTrue(body.contains("form_id=dh_daily_activity_form"))
        assertTrue(body.contains("event=Letter"))
        // `user=` (empty Drupal field) contains the letters "r=" — check the
        // query/field named `r`, which is what triggers seat auto-allocation.
        assertFalse(reqs[0].path.orEmpty().contains("r="))
        assertFalse(reqs[1].path.orEmpty().contains("r="))
        assertFalse(body.startsWith("r=") || body.contains("&r="))
    }

    @Test
    fun searchAppPostNeverSendsBulkMailFields() = runBlocking {
        val form = api.searchAppForm(1).html()
        val fields = DeskSearchFields.of(form, "Meera Deshpande", "Confirmed")!!
        val html = api.searchAppSubmit(1, fields).html()
        val page = SearchPageParser.parse(html)
        assertTrue(page.dataset.any { it.givenName == "Meera" })
        server.takeRequest(1, TimeUnit.SECONDS)
        val post = server.takeRequest(1, TimeUnit.SECONDS)!!
        val body = post.body.readUtf8()
        assertTrue(body.contains("f_name=Meera"))
        assertTrue(body.contains("status"))
        assertFalse(body.contains("bulk-mail"))
        assertFalse(body.contains("letters"))
    }

    @Test
    fun clarificationPdfStreamsToSheetsCache() = runBlocking {
        val payload = transport.clarificationPdf(1, 3)
        assertTrue(payload is SheetPayload.Document)
        payload as SheetPayload.Document
        assertEquals("application/pdf", payload.mimeType)
        assertEquals("clarification-1-3.pdf", payload.file.name)
        assertEquals("/show-clarification/1/3", server.takeRequest(1, TimeUnit.SECONDS)!!.path)
        transport.wipe()
        assertFalse(payload.file.exists())
    }

    @Test
    fun forbiddenCentreIsVerbatim403() = runBlocking {
        val html = api.smsReport(MockFixtures.FORBIDDEN_CENTRE).html()
        assertTrue(html.contains("Access denied"))
        assertEquals(403, api.letters(MockFixtures.FORBIDDEN_CENTRE).code())
    }
}
