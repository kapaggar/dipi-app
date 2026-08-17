package org.dhamma.dipi.staff.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** `POST /app-update-attended/{id}` on the fixtures desk — `dh_app_update_attended` verbatim. */
class AttendedUpdateMockTest {
    private lateinit var server: MockWebServer
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val form = "application/x-www-form-urlencoded".toMediaType()

    private val fullForm =
        "s=Mbk&r=3&g=1&l=&v=&c=&cf=false&chow=false&chai=false&back=false&comment=&a=true"

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

    private fun post(id: Int, body: String) = client.newCall(
        Request.Builder()
            .url(server.url("/app-update-attended/$id"))
            .post(body.toRequestBody(form))
            .build(),
    ).execute()

    @Test
    fun allocationSucceedsWithLiveShapedReply() {
        post(MockFixtures.RAKESH_ID, fullForm).use { resp ->
            assertEquals(200, resp.code)
            val raw = resp.body!!.string()
            val body = json.parseToJsonElement(raw).jsonObject
            assertTrue(body["status"]!!.jsonPrimitive.boolean)
            assertEquals("Ok", body["msg"]!!.jsonPrimitive.content)
            // Live replies carry HTML list payloads; the slim DTO must ignore them.
            assertTrue(raw.contains("table-attending"))
            val dto = json.decodeFromString<AttendedUpdateDto>(raw)
            assertTrue(dto.status)
            assertEquals("Ok", dto.msg)
        }
    }

    @Test
    fun successMarksThePersonAttended() {
        post(MockFixtures.RAKESH_ID, fullForm).use { assertEquals(200, it.code) }
        val applicant = client.newCall(
            Request.Builder().url(server.url("/staff/applicants/${MockFixtures.RAKESH_ID}")).get().build(),
        ).execute().use { json.parseToJsonElement(it.body!!.string()).jsonObject }
        assertTrue(applicant["attended"]!!.jsonPrimitive.boolean)
        // Status is the endpoint's own business — the client never sent one.
        assertEquals("Pending", applicant["status"]!!.jsonPrimitive.content)
    }

    @Test
    fun missingSectionOrRoomRefusesLikeLive() {
        post(3, fullForm.replace("s=Mbk", "s=")).use { resp ->
            val dto = json.decodeFromString<AttendedUpdateDto>(resp.body!!.string())
            assertFalse(dto.status)
            assertEquals("Please select room section", dto.msg)
        }
        post(3, fullForm.replace("r=3", "r=")).use { resp ->
            val dto = json.decodeFromString<AttendedUpdateDto>(resp.body!!.string())
            assertFalse(dto.status)
            assertEquals("Please select room no", dto.msg)
        }
    }

    @Test
    fun roomHeldByAnotherApplicantRefuses() {
        // Fixture seed: Suresh (4) already holds Mbk 8.
        post(MockFixtures.RAKESH_ID, fullForm.replace("r=3", "r=8")).use { resp ->
            val dto = json.decodeFromString<AttendedUpdateDto>(resp.body!!.string())
            assertFalse(dto.status)
            assertEquals("Room has already been alloted", dto.msg)
        }
        // The holder re-posting their own room is fine (live checks aa_applicant <> id).
        post(4, fullForm.replace("r=3", "r=8")).use { resp ->
            val dto = json.decodeFromString<AttendedUpdateDto>(resp.body!!.string())
            assertTrue(dto.status)
        }
    }

    @Test
    fun unattendReleasesTheRoom() {
        post(MockFixtures.RAKESH_ID, fullForm).use { assertEquals(200, it.code) }
        post(MockFixtures.RAKESH_ID, "a=false").use { resp ->
            val dto = json.decodeFromString<AttendedUpdateDto>(resp.body!!.string())
            assertTrue(dto.status)
        }
        // Room 3 is free again for someone else.
        post(12, fullForm).use { resp ->
            val dto = json.decodeFromString<AttendedUpdateDto>(resp.body!!.string())
            assertTrue(dto.status)
        }
    }
}
