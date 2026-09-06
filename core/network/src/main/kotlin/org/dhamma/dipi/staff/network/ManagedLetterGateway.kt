package org.dhamma.dipi.staff.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CookieJar
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.dhamma.dipi.staff.model.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/** Existing GETs only. The portal client has no desk cookies, cache, interceptors or retries. */
@Singleton
class ManagedLetterGateway @Inject constructor(private val desk: OkHttpClient, @Named("baseUrl") private val baseUrl: String) {
    private val publicClient = OkHttpClient.Builder().cookieJar(CookieJar.NO_COOKIES)
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
        .callTimeout(30, TimeUnit.SECONDS).build()
    private val deskClient by lazy { desk.newBuilder().followRedirects(false).followSslRedirects(false)
        .retryOnConnectionFailure(false).callTimeout(30, TimeUnit.SECONDS).build() }

    suspend fun letters(scope: WhatsAppScope): List<ManagedLetter> = withContext(Dispatchers.IO) {
        require(scope.origin == baseUrl.trimEnd('/')) { "Centre session changed" }
        val url = scope.origin.toHttpUrl().newBuilder().addPathSegment("letters").addPathSegment(scope.centreId.toString()).build()
        ManagedLetterParser.activeLetters(get(deskClient, url), scope.origin, scope.centreId)
    }
    suspend fun render(scope: WhatsAppScope, applicantId: Int, letterId: Int, encryptedToken: String): RenderedLetter {
        require(letters(scope).any { it.id == letterId }) { "The selected letter is no longer active" }
        return withContext(Dispatchers.IO) {
            require(scope.origin == "https://dipi.vridhamma.org") { "Applicant portal is not verified for this server" }
            val url = "https://applicant.vridhamma.org/l.php".toHttpUrl().newBuilder()
                .addQueryParameter("a", encryptedToken).build()
            ManagedLetterParser.renderedLetter(get(publicClient, url), applicantId, letterId)
        }
    }
    private fun get(client: OkHttpClient, url: okhttp3.HttpUrl): String {
        try {
            client.newCall(Request.Builder().url(url).header("Cache-Control", "no-store").get().build()).execute().use { response ->
                require(response.code == 200) { if (response.code in setOf(301, 302, 303, 307, 308, 401, 403)) "Sign in again or check letter access" else "Letter request failed (HTTP ${response.code})" }
                val body = response.body ?: error("Empty letter response")
                require(body.contentLength() <= 512_000) { "Letter response is too large" }
                val output = java.io.ByteArrayOutputStream()
                val input = body.byteStream()
                val buffer = ByteArray(8192)
                while (output.size() <= 512_000) {
                    val count = input.read(buffer, 0, minOf(buffer.size, 512_001 - output.size()))
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
                val bytes = output.toByteArray()
                require(bytes.size <= 512_000) { "Letter response is too large" }
                return bytes.toString(Charsets.UTF_8)
            }
        } catch (e: java.io.IOException) {
            // Never expose a URL containing applicant credentials through an exception/log.
            throw IllegalStateException("Letter request failed. Check the connection and try again.")
        }
    }
}
