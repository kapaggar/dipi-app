package org.dhamma.dipi.staff.network

import android.graphics.Bitmap
import android.graphics.Color
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import java.io.ByteArrayOutputStream
import java.net.InetAddress

@RunWith(RobolectricTestRunner::class)
class ApplicantPhotoWriterTest {
    private lateinit var server: MockWebServer
    private lateinit var writer: ApplicantPhotoWriter
    private val logs = mutableListOf<String>()

    @Before
    fun start() {
        server = MockWebServer()
        server.start(InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1)), 0)
        val base = "http://127.0.0.1:${server.port}/"
        val retrofit = Retrofit.Builder()
            .baseUrl(base)
            .client(OkHttpClient())
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build()
        writer = ApplicantPhotoWriter(retrofit.create(StaffApi::class.java), base)
        writer.logLine = { logs += it }
    }

    @After
    fun stop() {
        writer.wipe()
        server.shutdown()
    }

    @Test
    fun happyPostIncludesFileAndEchoedFields() = runBlocking {
        server.enqueue(MockResponse().setBody(ApplicantEditFormFixtures.completeHtml()))
        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .addHeader("Location", "/zero-day/1/10"),
        )
        server.enqueue(MockResponse().setBody("<html><body>zero day</body></html>"))
        val jpeg = syntheticJpeg()
        val result = writer.submit(41, jpeg, "photo-41-corrected.jpg")
        assertTrue(result is PhotoDeskWriteResult.Committed)
        assertEquals(null, writer.heldForm())
        val get = server.takeRequest()
        assertEquals("GET", get.method)
        assertEquals("/app/41/edit", get.path)
        val post = server.takeRequest()
        assertEquals("POST", post.method)
        assertEquals("/app/41/edit", post.path)
        assertFalse(post.path!!.contains("r="))
        val raw = post.body.readByteArray().toString(Charsets.ISO_8859_1)
        assertTrue(raw.contains("name=\"files[upload_photo]\""))
        assertTrue(raw.contains("filename=\"photo-41-corrected.jpg\""))
        assertTrue(raw.contains("image/jpeg"))
        assertTrue(raw.contains("name=\"a_f_name\""))
        assertTrue(raw.contains("Priya"))
        assertTrue(raw.contains("name=\"a_l_name\""))
        assertTrue(raw.contains("Nair"))
        assertTrue(raw.contains("name=\"a_gender\""))
        assertTrue(raw.contains("F"))
        assertTrue(raw.contains("name=\"attending\""))
        assertTrue(raw.contains("name=\"special\""))
        assertTrue(raw.contains("chair"))
        assertTrue(raw.contains("name=\"form_token\""))
        assertTrue(raw.contains("tok-photo-write"))
        assertTrue(raw.contains("name=\"op\""))
        assertTrue(raw.contains("Update"))
        assertFalse(raw.contains("Approved"))
        assertFalse(logs.joinToString().contains(ApplicantEditFormFixtures.NPI_DOC))
        assertFalse(logs.joinToString().contains(ApplicantEditFormFixtures.NPI_HEALTH))
        assertTrue(logs.any { it.contains("fields=") && it.contains("id=41") })
    }

    @Test
    fun incompleteParseDoesNotPost() = runBlocking {
        server.enqueue(MockResponse().setBody(ApplicantEditFormFixtures.completeHtml(includeFile = false)))
        val result = writer.submit(41, syntheticJpeg(), "photo-41-corrected.jpg")
        assertTrue(result is PhotoDeskWriteResult.Incomplete)
        assertTrue((result as PhotoDeskWriteResult.Incomplete).message.contains("file", ignoreCase = true))
        assertEquals(1, server.requestCount)
        assertEquals("GET", server.takeRequest().method)
        assertEquals(null, writer.heldForm())
    }

    @Test
    fun neverSendsApproved() = runBlocking {
        server.enqueue(
            MockResponse().setBody(ApplicantEditFormFixtures.completeHtml(statusSelected = "Approved")),
        )
        val result = writer.submit(41, syntheticJpeg(), "photo-41-corrected.jpg")
        assertTrue(result is PhotoDeskWriteResult.Incomplete)
        assertTrue((result as PhotoDeskWriteResult.Incomplete).message.contains("Approved"))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun wipeClearsHeldForm() = runBlocking {
        server.enqueue(MockResponse().setBody(ApplicantEditFormFixtures.completeHtml()))
        server.enqueue(MockResponse().setBody("<html>ok</html>"))
        writer.submit(41, syntheticJpeg(), "photo-41-corrected.jpg")
        assertEquals(null, writer.heldForm())
        writer.wipe()
        assertEquals(null, writer.heldForm())
    }

    private fun syntheticJpeg(): ByteArray {
        val bitmap = Bitmap.createBitmap(2, 3, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(0, 0, Color.RED)
        bitmap.setPixel(1, 0, Color.GREEN)
        bitmap.setPixel(0, 1, Color.BLUE)
        bitmap.setPixel(1, 1, Color.YELLOW)
        bitmap.setPixel(0, 2, Color.CYAN)
        bitmap.setPixel(1, 2, Color.MAGENTA)
        val out = ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out))
        return out.toByteArray()
    }
}
