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
    fun okHttp(
        tokens: TokenStore,
        @Named("useMock") useMock: Boolean,
        server: MockWebServer,
    ): OkHttpClient {
        val auth = Interceptor { chain ->
            val b = chain.request().newBuilder()
            val cookie = runBlocking { tokens.sessionCookie() }
            val csrf = runBlocking { tokens.csrf() }
            if (!cookie.isNullOrBlank()) b.header("Cookie", cookie)
            if (!csrf.isNullOrBlank() && chain.request().method != "GET") {
                b.header("X-CSRF-Token", csrf)
            }
            chain.proceed(b.build())
        }
        val capture = Interceptor { chain ->
            val resp = chain.proceed(chain.request())
            val set = resp.header("Set-Cookie")
            if (set != null) {
                runBlocking { tokens.saveSession(set.substringBefore(";"), tokens.csrf()) }
            }
            resp
        }
        return OkHttpClient.Builder()
            .addInterceptor(auth)
            .addInterceptor(capture)
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
