package org.dhamma.dipi.staff.network

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionCookieJarTest {
    @Test
    fun clearDropsPersistedCookieSoNextRequestIsAnonymous() {
        val tokens = MemTokens(cookie = "SSESSFAKE=old-auth-session")
        val jar = SessionCookieJar(tokens)
        val url = "https://dipi.vridhamma.org/".toHttpUrl()

        assertEquals("old-auth-session", jar.loadForRequest(url).single().value)

        jar.clear()

        assertTrue(jar.loadForRequest(url).isEmpty())
        assertNull(tokens.cookie)
    }

    private class MemTokens(var cookie: String? = null, var csrf: String? = null) : TokenStore {
        override suspend fun sessionCookie(): String? = cookie
        override suspend fun csrf(): String? = csrf
        override suspend fun saveSession(cookie: String?, csrf: String?) {
            this.cookie = cookie
            this.csrf = csrf
        }
        override suspend fun clear() {
            cookie = null
            csrf = null
        }
    }
}
