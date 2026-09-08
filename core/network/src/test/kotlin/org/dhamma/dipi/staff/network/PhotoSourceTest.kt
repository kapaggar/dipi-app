package org.dhamma.dipi.staff.network

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Color
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.dhamma.dipi.staff.model.PhotoKey
import org.dhamma.dipi.staff.model.PhotoOrigins
import org.dhamma.dipi.staff.model.PhotoScope
import org.dhamma.dipi.staff.model.PhotoStamp
import org.dhamma.dipi.staff.model.PhotoStamps
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowBitmapFactory
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class PhotoSourceTest {

    private lateinit var server: MockWebServer
    private lateinit var contactedHosts: CopyOnWriteArrayList<String>
    private lateinit var client: OkHttpClient
    private lateinit var loader: PhotoLoader

    @Before
    fun start() {
        ShadowBitmapFactory.setAllowInvalidImageData(false)
        server = MockWebServer()
        server.start(InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1)), 0)
        contactedHosts = CopyOnWriteArrayList()
        client = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> {
                    contactedHosts += hostname
                    if (hostname == "127.0.0.1") {
                        return listOf(InetAddress.getByAddress(hostname, byteArrayOf(127, 0, 0, 1)))
                    }
                    throw java.net.UnknownHostException("blocked-off-origin:$hostname")
                }
            })
            .build()
        loader = PhotoLoader(client, true, "https://dipi.vridhamma.org/", server)
    }

    @After
    fun stop() {
        server.shutdown()
        ShadowBitmapFactory.setAllowInvalidImageData(true)
    }

    @Test
    fun toStringDoesNotDumpPixels() {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xFF112233.toInt())
        val text = PhotoSource(PhotoStamp("0".repeat(64), 2, 2), bitmap).toString()
        assertTrue(text.startsWith("PhotoSource"))
        assertFalse(text.contains("112233"))
        assertFalse(text.contains("ARGB"))
    }

    @Test
    fun goodImageIsReadyWithEncodedHash() = runBlocking {
        val png = encodedBytes(CompressFormat.PNG)
        server.enqueue(imageResponse(png, "image/png"))
        val result = loader.loadSource(key())
        val ready = result as PhotoSourceResult.Ready
        assertEquals(2, ready.source.bitmap.width)
        assertEquals(3, ready.source.bitmap.height)
        assertEquals(PhotoStamps.sha256Hex(png), ready.source.stamp.sha256)
        val recorded = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("GET /show-photo/7 HTTP/1.1", recorded.requestLine)
        assertEquals("identity", recorded.getHeader("Accept-Encoding"))
    }

    @Test
    fun thumbnailLoadStillReturnsBitmapOnFailureNull() = runBlocking {
        server.enqueue(imageResponse(encodedBytes(CompressFormat.PNG)))
        val bitmap = loader.load(7)
        assertTrue(bitmap != null && bitmap.width > 0)
        server.enqueue(MockResponse().setResponseCode(404).setBody("gone"))
        assertEquals(null, loader.load(8))
    }

    @Test
    fun goodJpegIsReadyWithEncodedHash() = runBlocking {
        val jpeg = encodedBytes(CompressFormat.JPEG)
        server.enqueue(imageResponse(jpeg, "image/jpeg"))
        val result = loader.loadSource(key())
        val ready = result as PhotoSourceResult.Ready
        assertEquals(PhotoStamps.sha256Hex(jpeg), ready.source.stamp.sha256)
        assertTrue(ready.source.bitmap.width > 0)
        assertTrue(ready.source.bitmap.height > 0)
    }

    @Test
    fun loginHtml200IsAuthenticationRequired() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/html; charset=utf-8")
                .setBody(LOGIN_HTML),
        )
        assertEquals(PhotoSourceResult.AuthenticationRequired, loader.loadSource(key()))
    }

    @Test
    fun forbidden403IsAuthenticationRequired() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(403).setBody("Access denied"))
        assertEquals(PhotoSourceResult.AuthenticationRequired, loader.loadSource(key()))
    }

    @Test
    fun notFound404IsMissing() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("Not found"))
        assertEquals(PhotoSourceResult.Missing, loader.loadSource(key()))
    }

    @Test
    fun malformedImageIsUnsupported() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "image/jpeg")
                .setBody("not-a-real-image"),
        )
        assertEquals(PhotoSourceResult.UnsupportedImage, loader.loadSource(key()))
    }

    @Test
    fun oversizedContentLengthIsTooLargeWithoutBody() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "image/png")
                .setBody("x")
                .setHeader("Content-Length", (PhotoLoader.MAX_ENCODED_BYTES + 1).toString()),
        )
        assertEquals(PhotoSourceResult.TooLarge, loader.loadSource(key()))
    }

    @Test
    fun chunkedBodyOverTenMibIsTooLarge() = runBlocking {
        val over = PhotoLoader.MAX_ENCODED_BYTES + 1
        val buffer = Buffer()
        val chunk = ByteArray(64 * 1024) { 0x41 }
        var left = over
        while (left > 0) {
            val n = minOf(left, chunk.size.toLong()).toInt()
            buffer.write(chunk, 0, n)
            left -= n
        }
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "image/png")
                .setChunkedBody(buffer, 8192),
        )
        assertEquals(PhotoSourceResult.TooLarge, loader.loadSource(key()))
    }

    @Test
    fun redirectOutsideOriginIsUnavailableAndDoesNotContactThatHost() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .setHeader("Location", "https://evil.example.test/show-photo/7"),
        )
        assertEquals(PhotoSourceResult.Unavailable, loader.loadSource(key()))
        assertEquals(1, server.requestCount)
        assertFalse(contactedHosts.any { it.contains("evil.example.test") })
    }

    @Test
    fun failureIsNotCached() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("gone"))
        assertEquals(PhotoSourceResult.Missing, loader.loadSource(key()))
        server.enqueue(imageResponse(encodedBytes(CompressFormat.PNG)))
        assertTrue(loader.loadSource(key()) is PhotoSourceResult.Ready)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun sameKeyRequestsCoalesce() = runBlocking {
        val calls = AtomicInteger(0)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                calls.incrementAndGet()
                entered.complete(Unit)
                runBlocking { release.await() }
                return imageResponse(encodedBytes(CompressFormat.PNG))
            }
        }
        val a = async { loader.loadSource(key()) }
        val b = async { loader.loadSource(key()) }
        entered.await()
        release.complete(Unit)
        assertTrue(a.await() is PhotoSourceResult.Ready)
        assertTrue(b.await() is PhotoSourceResult.Ready)
        assertEquals(1, calls.get())
    }

    @Test
    fun clearDuringInFlightFetchDoesNotCache() = runBlocking {
        val calls = AtomicInteger(0)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                calls.incrementAndGet()
                entered.complete(Unit)
                runBlocking { release.await() }
                return imageResponse(encodedBytes(CompressFormat.PNG))
            }
        }
        val pending = async { loader.loadSource(key()) }
        entered.await()
        loader.clear()
        release.complete(Unit)
        pending.await()
        val second = loader.loadSource(key())
        assertTrue(second is PhotoSourceResult.Ready)
        assertEquals(2, calls.get())
    }

    @Test
    fun originMismatchDoesNotHitNetwork() = runBlocking {
        val other = PhotoKey(PhotoScope("https://other.example.test", 1, 1), 7)
        assertEquals(PhotoSourceResult.Unavailable, loader.loadSource(other))
        assertEquals(0, server.requestCount)
    }

    private fun key(id: Int = 7): PhotoKey = PhotoKey(
        PhotoScope(PhotoOrigins.canonicalize("http://127.0.0.1:${server.port}"), 1, 1),
        id,
    )

    private fun encodedBytes(format: CompressFormat, width: Int = 2, height: Int = 3): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(0, 0, Color.RED)
        bitmap.setPixel(1, 0, Color.GREEN)
        if (height > 1) bitmap.setPixel(0, 1, Color.BLUE)
        val out = ByteArrayOutputStream()
        val quality = if (format == CompressFormat.JPEG) 92 else 100
        check(bitmap.compress(format, quality, out))
        return out.toByteArray()
    }

    private fun imageResponse(bytes: ByteArray, contentType: String = "image/png"): MockResponse =
        MockResponse()
            .setHeader("Content-Type", contentType)
            .setBody(Buffer().write(bytes))

    companion object {
        private const val LOGIN_HTML =
            """<!DOCTYPE html><html><body>
            <form id="user-login" action="/user/login">
            <input name="name"/><input name="pass"/>
            </form></body></html>"""
    }
}
