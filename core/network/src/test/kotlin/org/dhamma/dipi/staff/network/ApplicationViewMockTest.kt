package org.dhamma.dipi.staff.network

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Spec 2d S1 — the mock route for `GET /application-view/{id}`: exact
 * request line with NO query (the no-`r` rule is structural), a parseable
 * page for known ids, the themed 403 for the FORBIDDEN_CENTRE stand-in and
 * the wildcard-loader 404 for unknown ids.
 */
class ApplicationViewMockTest {

    private lateinit var server: MockWebServer
    private lateinit var api: StaffApi

    @Before
    fun start() {
        server = MockWebServer()
        server.dispatcher = DipiMockDispatcher()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .build()
            .create(StaffApi::class.java)
    }

    @After
    fun stop() {
        server.shutdown()
    }

    @Test
    fun requestLineIsPathOnlyNoQuery() = runBlocking {
        val resp = api.applicationView(4)
        assertTrue(resp.isSuccessful)
        val recorded = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("GET /application-view/4 HTTP/1.1", recorded.requestLine)
        assertNull(recorded.requestUrl!!.query)
        val card = ApplicationViewParser.parse(resp.html())
        assertEquals("Suresh Nair", card.name)
    }

    @Test
    fun forbiddenServesTheThemedDrupal403() = runBlocking {
        val resp = api.applicationView(MockFixtures.FORBIDDEN_CENTRE)
        assertEquals(403, resp.code())
        assertTrue(resp.html().contains("Access denied"))
    }

    @Test
    fun unknownIdServesTheWildcardLoader404() = runBlocking {
        val resp = api.applicationView(9999)
        assertEquals(404, resp.code())
        assertTrue(resp.html().contains("Page not found"))
    }
}
