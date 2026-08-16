package org.dhamma.dipi.staff.network

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit

class ResponseHtmlTest {
    @Test
    fun htmlReadsLoginFormFrom403ErrorBody() {
        val html = """
            <title>Access denied | Dīpi</title>
            <form action="/home?destination=home" method="post" id="user-login-form">
            <input type="hidden" name="form_build_id" value="form-from-403" />
            <input type="hidden" name="form_id" value="user_login_block" />
            </form>
        """.trimIndent()
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(403).setBody(html))
            val api = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .build()
                .create(StaffApi::class.java)
            val page = runBlocking { api.siteRoot().html() }
            assertTrue(page.contains("form-from-403"))
            val block = SearchPageParser.loginBlock(page)
            assertNotNull(block)
            assertEquals("form-from-403", block!!.formBuildId)
            assertEquals("user_login_block", block.formId)
            assertEquals("/home?destination=home", block.action)
        }
    }
}
