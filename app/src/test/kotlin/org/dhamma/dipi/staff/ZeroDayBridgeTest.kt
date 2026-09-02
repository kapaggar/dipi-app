package org.dhamma.dipi.staff

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.dhamma.dipi.staff.data.ConnectivityMonitor
import org.dhamma.dipi.staff.data.PhotoEditStore
import org.dhamma.dipi.staff.data.StaffRepository
import org.dhamma.dipi.staff.database.ApplicantDao
import org.dhamma.dipi.staff.database.ApplicantEntity
import org.dhamma.dipi.staff.database.OutboxDao
import org.dhamma.dipi.staff.database.OutboxEntity
import org.dhamma.dipi.staff.datastore.SessionStore
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.RoomAllocSync
import org.dhamma.dipi.staff.network.DipiMockDispatcher
import org.dhamma.dipi.staff.network.DrupalAuthApi
import org.dhamma.dipi.staff.network.PhotoLoader
import org.dhamma.dipi.staff.network.SessionCookieJar
import org.dhamma.dipi.staff.network.StaffApi
import org.dhamma.dipi.staff.network.TokenStore
import org.dhamma.dipi.staff.ui.DeskUiState
import org.dhamma.dipi.staff.ui.DeskViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import retrofit2.Retrofit

@RunWith(RobolectricTestRunner::class)
class ZeroDayBridgeTest {
    private val server = MockWebServer().apply { dispatcher = DipiMockDispatcher() }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private class FakeTokens : TokenStore {
        var cookie: String? = null
        var token: String? = null
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
        override fun observe(courseId: Int): Flow<List<ApplicantEntity>> = flowOf(emptyList())
        override suspend fun list(courseId: Int): List<ApplicantEntity> = emptyList()
        override suspend fun listAll(): List<ApplicantEntity> = emptyList()
        override suspend fun get(id: Int): ApplicantEntity? = null
        override suspend fun upsert(rows: List<ApplicantEntity>) = Unit
        override suspend fun clear() = Unit
    }

    private class FakeOutbox : OutboxDao {
        override fun observePending(): Flow<List<OutboxEntity>> = flowOf(emptyList())
        override suspend fun pending(): List<OutboxEntity> = emptyList()
        override suspend fun insert(row: OutboxEntity): Long = 1L
        override suspend fun updateState(id: Long, state: String, message: String?) = Unit
        override suspend fun clear() = Unit
    }

    private fun buildVm(): DeskViewModel {
        server.start()
        val app = RuntimeEnvironment.getApplication()
        val tokens = FakeTokens()
        val json = Json { ignoreUnknownKeys = true }
        val base = "http://127.0.0.1:${server.port}/"
        val retrofit = Retrofit.Builder()
            .baseUrl(base)
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        val sessionStore = SessionStore(app)
        val repo = StaffRepository(
            auth = retrofit.create(DrupalAuthApi::class.java),
            api = retrofit.create(StaffApi::class.java),
            tokens = tokens,
            sessionStore = sessionStore,
            applicants = FakeApplicants(),
            outbox = FakeOutbox(),
            json = json,
            cookies = SessionCookieJar(tokens),
            useMock = true,
            baseUrl = base,
            context = app,
        )
        return DeskViewModel(
            repo,
            sessionStore,
            PhotoEditStore(app, json),
            PhotoLoader(OkHttpClient(), true, base, server),
            ConnectivityMonitor(app),
        )
    }

    private fun card(id: Int = 31) = ApplicantCard(
        id = ApplicantId(id),
        centreId = CentreId(12),
        courseId = CourseId(77),
        givenName = "Meera",
        familyName = "Kulkarni",
        gender = Gender.F,
        status = ApplicantStatus("Confirmed"),
        type = ApplicantType.Student,
        oldStudent = false,
        attended = false,
    )

    @Test
    fun markAttendedWithRoomChecksInAndEntersTheSyncQueue() {
        val vm = buildVm()
        val meera = card()
        vm.seedForTest(
            DeskUiState(
                rows = listOf(meera),
                checkIns = mapOf(meera.id to CheckInRecord(room = "Mbk 1")),
            ),
        )
        vm.markAttended(meera)
        val record = vm.state.value.checkIns[meera.id]
        assertEquals(true, record?.checkedIn)
        assertEquals(true, vm.state.value.rows.first { it.id == meera.id }.attended)
        assertEquals(setOf(meera.id), RoomAllocSync.pending(vm.state.value.checkIns).keys)
    }

    @Test
    fun markAttendedWithoutRoomIsBlocked() {
        val vm = buildVm()
        val meera = card()
        vm.seedForTest(DeskUiState(rows = listOf(meera)))
        vm.markAttended(meera)
        assertEquals(true, vm.state.value.snack?.error)
        assertTrue(vm.state.value.snack?.text?.startsWith("Choose a room") == true)
        assertEquals(false, vm.state.value.rows.first { it.id == meera.id }.attended)
        assertTrue(RoomAllocSync.pending(vm.state.value.checkIns).isEmpty())
    }
}
