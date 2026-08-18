package org.dhamma.dipi.staff.desktop.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.dhamma.dipi.staff.desktop.DesktopConfig
import org.dhamma.dipi.staff.network.DipiMockDispatcher
import org.dhamma.dipi.staff.network.DrupalAuthApi
import org.dhamma.dipi.staff.network.SessionCookieJar
import org.dhamma.dipi.staff.network.StaffApi
import org.dhamma.dipi.staff.network.TokenStore
import retrofit2.Retrofit
import java.net.InetAddress
import java.util.concurrent.TimeUnit

data class DesktopClients(
    val api: StaffApi,
    val auth: DrupalAuthApi,
    val cookies: SessionCookieJar,
    val json: Json,
    val mockServer: MockWebServer?,
)

object DesktopNetwork {
    fun create(config: DesktopConfig, tokens: TokenStore): DesktopClients {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val mock = if (config.useMock) {
            MockWebServer().apply {
                dispatcher = DipiMockDispatcher()
                start(InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1)), 0)
            }
        } else {
            null
        }
        val cookies = SessionCookieJar(tokens)
        val csrf = Interceptor { chain ->
            val b = chain.request().newBuilder()
                .header("User-Agent", DesktopConfig.USER_AGENT)
            val token = runBlocking { tokens.csrf() }
            val path = chain.request().url.encodedPath
            val skipCsrf = path == "/home" || path.startsWith("/user")
            if (!token.isNullOrBlank() && chain.request().method != "GET" && !skipCsrf) {
                b.header("X-CSRF-Token", token)
            }
            chain.proceed(b.build())
        }
        val client = OkHttpClient.Builder()
            .cookieJar(cookies)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(csrf)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
        val url = if (mock != null) {
            "http://127.0.0.1:${mock.port}/"
        } else {
            config.baseUrl.let { if (it.endsWith("/")) it else "$it/" }
        }
        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return DesktopClients(
            api = retrofit.create(StaffApi::class.java),
            auth = retrofit.create(DrupalAuthApi::class.java),
            cookies = cookies,
            json = json,
            mockServer = mock,
        )
    }
}
