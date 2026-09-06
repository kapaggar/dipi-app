package org.dhamma.dipi.staff.network

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.dhamma.dipi.staff.model.WhatsAppScope
import org.junit.Assert.*
import org.junit.Test

class ManagedLetterGatewayTest {
    private val scope = WhatsAppScope("https://desk.example.test",91)
    @Test fun `requests only the authenticated active listing with no allocation query`() = runBlocking {
        val requests = mutableListOf<okhttp3.Request>()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            requests.add(chain.request())
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("<table id='table-letters'></table>".toResponseBody()).build()
        }.build()
        val gateway = ManagedLetterGateway(client,scope.origin)
        assertTrue(gateway.letters(scope).isEmpty())
        assertEquals(1,requests.size)
        assertEquals("GET",requests.single().method)
        assertEquals("/letters/91",requests.single().url.encodedPath)
        assertNull(requests.single().url.query)
        assertEquals("no-store",requests.single().header("Cache-Control"))
        try { gateway.render(scope,1,99,"synthetic-token"); fail("Inactive letter must not reach portal") }
        catch (e: IllegalArgumentException) { assertEquals("The selected letter is no longer active",e.message) }
        assertTrue(requests.all { it.url.encodedPath=="/letters/91" })
    }
    @Test fun `rejects another server without making a request`() = runBlocking {
        var calls=0
        val client=OkHttpClient.Builder().addInterceptor { calls++; error("Network must not be reached") }.build()
        try { ManagedLetterGateway(client,scope.origin).letters(WhatsAppScope("https://other.example.test",91)); fail() }
        catch (_: IllegalArgumentException) { }
        assertEquals(0,calls)
    }
    @Test fun `rejects authentication redirects without following them`() = runBlocking {
        var calls=0
        val client=OkHttpClient.Builder().addInterceptor { chain ->
            calls++
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(302).message("Found")
                .header("Location","https://other.example.test/").body("".toResponseBody()).build()
        }.build()
        try { ManagedLetterGateway(client,scope.origin).letters(scope); fail() }
        catch (e:IllegalArgumentException) { assertFalse(e.message.orEmpty().contains("other.example")) }
        assertEquals(1,calls)
    }
}
