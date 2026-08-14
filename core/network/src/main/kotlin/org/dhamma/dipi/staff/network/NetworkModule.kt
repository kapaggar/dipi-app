package org.dhamma.dipi.staff.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import java.net.InetAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun json(): Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Provides
    @Singleton
    fun mockServer(@Named("useMock") useMock: Boolean): MockWebServer {
        val server = MockWebServer()
        server.dispatcher = DipiMockDispatcher()
        if (!useMock) return server
        // start() defaults to InetAddress.getByName("localhost"), which is DNS
        // and crashes on the main thread (NetworkOnMainThreadException).
        val ready = CountDownLatch(1)
        val fail = arrayOfNulls<Throwable>(1)
        Executors.newSingleThreadExecutor().execute {
            try {
                server.start(InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1)), 0)
            } catch (t: Throwable) {
                fail[0] = t
            } finally {
                ready.countDown()
            }
        }
        check(ready.await(5, TimeUnit.SECONDS)) { "MockWebServer start timed out" }
        fail[0]?.let { throw it }
        return server
    }

    @Provides
    @Singleton
    fun cookieJar(tokens: TokenStore): SessionCookieJar = SessionCookieJar(tokens)

    @Provides
    @Singleton
    fun okHttp(
        tokens: TokenStore,
        jar: SessionCookieJar,
    ): OkHttpClient {
        val csrf = Interceptor { chain ->
            val b = chain.request().newBuilder()
                .header("User-Agent", "DIPI-Staff/1.1 (Android; registrar desk)")
            val token = runBlocking { tokens.csrf() }
            val path = chain.request().url.encodedPath
            val skipCsrf = path == "/home" || path.startsWith("/user")
            if (!token.isNullOrBlank() && chain.request().method != "GET" && !skipCsrf) {
                b.header("X-CSRF-Token", token)
            }
            chain.proceed(b.build())
        }
        return OkHttpClient.Builder()
            .cookieJar(jar)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(csrf)
            .build()
    }

    @Provides
    @Singleton
    fun retrofit(
        client: OkHttpClient,
        json: Json,
        @Named("useMock") useMock: Boolean,
        @Named("baseUrl") baseUrl: String,
        server: MockWebServer,
    ): Retrofit {
        // MockWebServer.url() reverse-DNSes the bind address (NetworkOnMainThread).
        val url = if (useMock) "http://127.0.0.1:${server.port}/" else baseUrl.ensureSlash()
        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun staffApi(retrofit: Retrofit): StaffApi = retrofit.create(StaffApi::class.java)

    @Provides
    @Singleton
    fun authApi(retrofit: Retrofit): DrupalAuthApi = retrofit.create(DrupalAuthApi::class.java)

    private fun String.ensureSlash() = if (endsWith("/")) this else "$this/"
}
