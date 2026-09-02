package org.dhamma.dipi.staff

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.dhamma.dipi.staff.data.StaffRepository
import org.dhamma.dipi.staff.database.ApplicantDao
import org.dhamma.dipi.staff.database.ApplicantEntity
import org.dhamma.dipi.staff.database.OutboxDao
import org.dhamma.dipi.staff.database.OutboxEntity
import org.dhamma.dipi.staff.datastore.SessionStore
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.network.DrupalAuthApi
import org.dhamma.dipi.staff.network.SessionCookieJar
import org.dhamma.dipi.staff.network.StaffApi
import org.dhamma.dipi.staff.network.TokenStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import retrofit2.Retrofit

/**
 * A routine session timeout (403 mid-session) must boot to Sign in WITHOUT
 * destroying local desk work: the applicant cache, the queued status outbox,
 * and every check-in record — including the room-sync walk's partial progress
 * (owner amendment 2026-08-16) — survive; only the dead cookies/CSRF go.
 * Explicit Logout and "Erase all local data" keep their full wipes.
 */
@RunWith(RobolectricTestRunner::class)
class SessionExpiryTest {

    private class FakeTokens : TokenStore {
        var cookie: String? = "SESSabc123=deadbeef"
        var token: String? = "csrf-token"
        override suspend fun sessionCookie() = cookie
        override suspend fun csrf() = token
        override suspend fun saveSession(cookie: String?, csrf: String?) {
            this.cookie = cookie
            this.token = csrf
        }
        override suspend fun clear() {
            cookie = null
            token = null
        }
    }

    private class FakeApplicants : ApplicantDao {
        var cleared = false
        override fun observe(courseId: Int): Flow<List<ApplicantEntity>> = flowOf(emptyList())
        override suspend fun list(courseId: Int): List<ApplicantEntity> = emptyList()
        override suspend fun listAll(): List<ApplicantEntity> = emptyList()
        override suspend fun get(id: Int): ApplicantEntity? = null
        override suspend fun upsert(rows: List<ApplicantEntity>) = Unit
        override suspend fun clear() {
            cleared = true
        }
    }

    private class FakeOutbox : OutboxDao {
        var cleared = false
        override fun observePending(): Flow<List<OutboxEntity>> = flowOf(emptyList())
        override suspend fun pending(): List<OutboxEntity> = emptyList()
        override suspend fun insert(row: OutboxEntity): Long = 1L
        override suspend fun updateState(id: Long, state: String, message: String?) = Unit
        override suspend fun clear() {
            cleared = true
        }
    }

    @Test
    fun sessionExpiryDropsCookiesButKeepsCheckInsOutboxAndCache() = runTest {
        val tokens = FakeTokens()
        val applicants = FakeApplicants()
        val outbox = FakeOutbox()
        val sessionStore = SessionStore(RuntimeEnvironment.getApplication())
        // Never invoked: sessionExpired() must not call the server.
        val offline = Retrofit.Builder().baseUrl("http://localhost/").build()
        val repo = StaffRepository(
            auth = offline.create(DrupalAuthApi::class.java),
            api = offline.create(StaffApi::class.java),
            tokens = tokens,
            sessionStore = sessionStore,
            courseOpsStore = testCourseOpsStore(),
            applicants = applicants,
            outbox = outbox,
            json = Json { ignoreUnknownKeys = true },
            cookies = SessionCookieJar(tokens),
            useMock = true,
            baseUrl = "http://localhost/",
            context = RuntimeEnvironment.getApplication(),
        )
        val records = mapOf(
            7 to CheckInRecord(checkedIn = true, room = "Mbk 8", synced = true, syncedAt = "2026-08-16T09:00:00Z"),
            9 to CheckInRecord(checkedIn = true, room = "Fbk 2"),
        )
        sessionStore.setCheckIns(records)

        repo.sessionExpired()

        assertNull("dead session cookie must go", tokens.cookie)
        assertNull("stale CSRF token must go", tokens.token)
        assertFalse("applicant cache must survive a session timeout", applicants.cleared)
        assertFalse("queued status outbox must survive a session timeout", outbox.cleared)
        assertEquals(
            "check-ins (incl. partial room-sync progress) must survive a session timeout",
            records,
            sessionStore.checkInsOnce(),
        )
    }
}
