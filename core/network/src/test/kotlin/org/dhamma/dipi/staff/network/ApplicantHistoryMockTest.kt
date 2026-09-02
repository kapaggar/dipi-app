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

class ApplicantHistoryMockTest {
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
}
