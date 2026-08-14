package org.dhamma.dipi.staff.network

import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class SessionCookieJar(
    private val tokens: TokenStore,
) : CookieJar {
    private val lock = Any()
    private val store = mutableListOf<Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        synchronized(lock) {
            cookies.forEach { incoming ->
                store.removeAll { it.name == incoming.name }
                if (incoming.value.isNotBlank()) store.add(incoming)
            }
        }
        persist()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        synchronized(lock) {
            if (store.isEmpty()) hydrate(url)
            return store.toList()
        }
    }

    fun clear() {
        synchronized(lock) { store.clear() }
        runBlocking { tokens.saveSession(null, tokens.csrf()) }
    }

    private fun persist() {
        val header = synchronized(lock) {
            store.joinToString("; ") { "${it.name}=${it.value}" }
        }
        if (header.isNotBlank()) {
            runBlocking { tokens.saveSession(header, tokens.csrf()) }
        }
    }

    private fun hydrate(url: HttpUrl) {
        val raw = runBlocking { tokens.sessionCookie() }.orEmpty()
        if (raw.isBlank()) return
        raw.split(";").map { it.trim() }.filter { it.contains("=") }.forEach { part ->
            val name = part.substringBefore("=")
            val value = part.substringAfter("=")
            val cookie = Cookie.Builder()
                .name(name)
                .value(value)
                .hostOnlyDomain(url.host)
                .path("/")
                .build()
            store.add(cookie)
        }
    }
}
