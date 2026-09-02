package org.dhamma.dipi.staff

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.dhamma.dipi.staff.data.StaffRepository
import org.dhamma.dipi.staff.database.ApplicantDao
import org.dhamma.dipi.staff.database.ApplicantEntity
import org.dhamma.dipi.staff.database.OutboxDao
import org.dhamma.dipi.staff.database.OutboxEntity
import org.dhamma.dipi.staff.datastore.SessionStore
import org.dhamma.dipi.staff.model.FlushSnack
import org.dhamma.dipi.staff.network.DrupalAuthApi
import org.dhamma.dipi.staff.network.SessionCookieJar
import org.dhamma.dipi.staff.network.StaffApi
import org.dhamma.dipi.staff.network.TokenStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import retrofit2.Retrofit

/**
 * Upgrade-path guard: a Room outbox row queued as Approved on 1.27.0 must
 * never reach GET /change-status after 1.30.2.
 */
@RunWith(RobolectricTestRunner::class)
class FlushOutboxApprovedTest {

    private val hits = mutableListOf<String>()
    private val server = MockWebServer().apply {
        dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                hits += request.path.orEmpty()
                return MockResponse().setResponseCode(500)
            }
        }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private class FakeTokens : TokenStore {
        override suspend fun sessionCookie() = null
        override suspend fun csrf() = null
        override suspend fun saveSession(cookie: String?, csrf: String?) = Unit
        override suspend fun clear() = Unit
    }

    private class FakeApplicants : ApplicantDao {
        override fun observe(courseId: Int): Flow<List<ApplicantEntity>> = flowOf(emptyList())
        override suspend fun list(courseId: Int): List<ApplicantEntity> = emptyList()
        override suspend fun listAll(): List<ApplicantEntity> = emptyList()
        override suspend fun get(id: Int): ApplicantEntity? = null
        override suspend fun upsert(rows: List<ApplicantEntity>) = Unit
        override suspend fun clear() = Unit
    }

    private class FakeOutbox(private val rows: List<OutboxEntity>) : OutboxDao {
        val updates = mutableListOf<Triple<Long, String, String?>>()
        override fun observePending(): Flow<List<OutboxEntity>> = flowOf(rows)
        override suspend fun pending(): List<OutboxEntity> = rows
        override suspend fun insert(row: OutboxEntity): Long = 1L
        override suspend fun updateState(id: Long, state: String, message: String?) {
            updates += Triple(id, state, message)
        }
        override suspend fun clear() = Unit
    }

    @Test
    fun pendingApprovedRowNeverReachesChangeStatus() = runTest {
        server.start()
        val outbox = FakeOutbox(
            listOf(
                OutboxEntity(
                    rowId = 1L,
                    applicantId = 42,
                    status = "Approved",
                    letterId = 0,
                    comment = "",
                    state = "Pending",
                    message = null,
                ),
            ),
        )
        val tokens = FakeTokens()
        val base = "http://127.0.0.1:${server.port}/"
        val retrofit = Retrofit.Builder().baseUrl(base).build()
        val repo = StaffRepository(
            auth = retrofit.create(DrupalAuthApi::class.java),
            api = retrofit.create(StaffApi::class.java),
            tokens = tokens,
            sessionStore = SessionStore(RuntimeEnvironment.getApplication()),
            applicants = FakeApplicants(),
            outbox = outbox,
            json = Json { ignoreUnknownKeys = true },
            cookies = SessionCookieJar(tokens),
            useMock = false,
            baseUrl = base,
            context = RuntimeEnvironment.getApplication(),
        )

        val snacks = repo.flushOutbox()

        assertEquals(listOf(FlushSnack("The app never sends Approved", error = true)), snacks)
        assertEquals(listOf(Triple(1L, "Failed", "The app never sends Approved")), outbox.updates)
        assertEquals(0, server.requestCount)
        assertTrue(hits.none { it.contains("change-status") })
    }
}
