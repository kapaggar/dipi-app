package org.dhamma.dipi.staff.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DipiMockDispatcherTest {
    private lateinit var server: MockWebServer
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun start() {
        server = MockWebServer()
        server.dispatcher = DipiMockDispatcher()
        server.start()
    }

    @After
    fun stop() {
        server.shutdown()
    }

    @Test
    fun loginSuccessSetsSessCookie() {
        val req = Request.Builder()
            .url(server.url("/api/user/login"))
            .post("""{"username":"registrar.giri","password":"ok"}""".toRequestBody(null))
            .build()
        client.newCall(req).execute().use { resp ->
            assertEquals(200, resp.code)
            val setCookie = resp.headers("Set-Cookie").joinToString(";")
            assertTrue("expected SESS cookie, got: $setCookie", setCookie.contains("SESS=sess-demo"))
            val body = json.parseToJsonElement(resp.body!!.string()).jsonObject
            assertEquals("sess-demo", body["sessid"]!!.jsonPrimitive.content)
            assertEquals("csrf-demo", body["token"]!!.jsonPrimitive.content)
        }
    }

    @Test
    fun loginRejectsBadPassword() {
        val req = Request.Builder()
            .url(server.url("/api/user/login"))
            .post("""{"username":"x","password":"bad"}""".toRequestBody(null))
            .build()
        client.newCall(req).execute().use { resp ->
            assertEquals(401, resp.code)
            assertTrue(resp.body!!.string().contains("Unrecognized username or password."))
        }
    }

    @Test
    fun csrfTokenReturnsPlainText() {
        val req = Request.Builder()
            .url(server.url("/services/session/token"))
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            assertEquals(200, resp.code)
            assertTrue(
                "expected text/plain Content-Type, got ${resp.header("Content-Type")}",
                resp.header("Content-Type")!!.startsWith("text/plain"),
            )
            assertEquals("csrf-demo", resp.body!!.string())
        }
    }

    @Test
    fun rakeshChangeStatusRefusesVerbatim() {
        val req = Request.Builder()
            .url(server.url("/change-status/${MockFixtures.RAKESH_ID}?s=Confirmed&l=0&c="))
            .post("".toRequestBody(null))
            .build()
        client.newCall(req).execute().use { resp ->
            assertEquals(200, resp.code)
            val body = json.parseToJsonElement(resp.body!!.string()).jsonObject
            assertEquals("Failed", body["status"]!!.jsonPrimitive.content)
            assertEquals(
                "Please Edit application and choose Area teacher before approving!",
                body["msg"]!!.jsonPrimitive.content,
            )
        }
    }

    @Test
    fun meeraConfirmMintsNf129() {
        val req = Request.Builder()
            .url(server.url("/change-status/${MockFixtures.MEERA_ID}?s=Confirmed&l=0&c="))
            .post("".toRequestBody(null))
            .build()
        client.newCall(req).execute().use { resp ->
            assertEquals(200, resp.code)
            val body = json.parseToJsonElement(resp.body!!.string()).jsonObject
            assertEquals("OK", body["status"]!!.jsonPrimitive.content)
            assertEquals("NF129", body["confno"]!!.jsonPrimitive.content)
            assertEquals("Confirmed", body["newstatus"]!!.jsonPrimitive.content)
        }
    }

    @Test
    fun applicantsCountsAndSearch() {
        val all = get("/staff/courses/10/applicants")
        val counts = all["counts"]!!.jsonObject
        assertEquals(214, counts["All"]!!.jsonPrimitive.int)
        assertEquals(61, counts["Pending"]!!.jsonPrimitive.int)
        assertEquals(48, counts["Received"]!!.jsonPrimitive.int)
        assertEquals(72, counts["Confirmed"]!!.jsonPrimitive.int)
        assertEquals(18, counts["Expected"]!!.jsonPrimitive.int)
        assertEquals(9, counts["Cancelled"]!!.jsonPrimitive.int)
        assertEquals(6, counts["Rejected"]!!.jsonPrimitive.int)
        assertEquals(12, all["items"]!!.jsonArray.size)

        val q = get("/staff/courses/10/applicants?q=Rakesh")
        val items = q["items"]!!.jsonArray
        assertEquals(1, items.size)
        assertEquals("Rakesh", items[0].jsonObject["givenName"]!!.jsonPrimitive.content)
        assertFalse(q.toString().contains("Meera"))
    }

    @Test
    fun statusesNeverIncludeApproved() {
        val body = get("/staff/meta/statuses").toString()
        assertFalse(body.contains("Approved"))
        assertTrue(body.contains("Custom"))
    }

    @Test
    fun photoReviewPayload() {
        val body = get("/staff/courses/10/photo-review")
        val items = body["items"]!!.jsonArray
        assertEquals(4, items.size)

        val byId = items.map { it.jsonObject }.associateBy { it["applicantId"]!!.jsonPrimitive.int }
        assertNotNull(byId[MockFixtures.RAKESH_ID])
        assertEquals("suggest", byId[MockFixtures.RAKESH_ID]!!["kind"]!!.jsonPrimitive.content)
        assertEquals(90, byId[MockFixtures.RAKESH_ID]!!["suggestedRotate"]!!.jsonPrimitive.int)
        assertEquals("false", byId[MockFixtures.RAKESH_ID]!!["suggestedCrop"]!!.jsonPrimitive.content)

        assertEquals("auto", byId[9]!!["kind"]!!.jsonPrimitive.content)
        assertEquals(180, byId[9]!!["suggestedRotate"]!!.jsonPrimitive.int)

        assertEquals("suggest", byId[3]!!["kind"]!!.jsonPrimitive.content)
        assertEquals("true", byId[3]!!["suggestedCrop"]!!.jsonPrimitive.content)

        assertEquals("nofel", byId[7]!!["kind"]!!.jsonPrimitive.content)
        assertEquals("no face found", byId[7]!!["badge"]!!.jsonPrimitive.content)
    }

    @Test
    fun photoUploadSuccessMessage() {
        val req = Request.Builder()
            .url(server.url("/staff/applicants/${MockFixtures.MEERA_ID}/photo"))
            .post("""{"rotate":0}""".toRequestBody(null))
            .build()
        client.newCall(req).execute().use { resp ->
            assertEquals(200, resp.code)
            val body = json.parseToJsonElement(resp.body!!.string()).jsonObject
            assertEquals("true", body["ok"]!!.jsonPrimitive.content)
            assertEquals(
                "✓ Uploaded 1 photo(s), all other fields preserved",
                body["message"]!!.jsonPrimitive.content,
            )
        }
    }

    @Test
    fun unknownPathReturns404() {
        val req = Request.Builder()
            .url(server.url("/staff/does-not-exist"))
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            assertEquals(404, resp.code)
            assertTrue(resp.body!!.string().contains("not mocked"))
        }
    }

    private fun get(path: String) = client.newCall(
        Request.Builder().url(server.url(path)).get().build(),
    ).execute().use { json.parseToJsonElement(it.body!!.string()).jsonObject }
}
