package org.dhamma.dipi.staff

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.dhamma.dipi.staff.data.OLDER_COURSE_LIMIT
import org.dhamma.dipi.staff.data.StaffRepository
import org.dhamma.dipi.staff.database.ApplicantDao
import org.dhamma.dipi.staff.database.ApplicantEntity
import org.dhamma.dipi.staff.database.OutboxDao
import org.dhamma.dipi.staff.database.OutboxEntity
import org.dhamma.dipi.staff.datastore.SessionStore
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.network.CourseDto
import org.dhamma.dipi.staff.network.CourseListDto
import org.dhamma.dipi.staff.network.DrupalAuthApi
import org.dhamma.dipi.staff.network.SessionCookieJar
import org.dhamma.dipi.staff.network.StaffApi
import org.dhamma.dipi.staff.network.TokenStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit

@RunWith(RobolectricTestRunner::class)
class OlderCourseLimitTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: StaffRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = TestDispatcher()
        server.start()

        val app = ApplicationProvider.getApplicationContext<Context>()
        val tokens = FakeTokens()
        val json = Json { ignoreUnknownKeys = true }
        val base = "http://127.0.0.1:${server.port}/"
        val retrofit = Retrofit.Builder()
            .baseUrl(base)
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        val sessionStore = SessionStore(app)
        repository = StaffRepository(
            auth = retrofit.create(DrupalAuthApi::class.java),
            api = retrofit.create(StaffApi::class.java),
            tokens = tokens,
            sessionStore = sessionStore,
            courseOpsStore = testCourseOpsStore(),
            applicants = FakeApplicants(),
            outbox = FakeOutbox(),
            json = json,
            cookies = SessionCookieJar(tokens),
            useMock = true,
            baseUrl = base,
            context = app,
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun loadCoursesCapsTooManyOlderCoursesAtThree() = runBlocking {
        val courses = repository.loadCourses(CentreId(1))
        assertEquals(3, courses.older.size)
        assertEquals("5th older", courses.older[0].name)
        assertEquals("4th older", courses.older[1].name)
        assertEquals("3rd older", courses.older[2].name)
    }

    @Test
    fun loadCoursesShorterOlderListsPassThrough() = runBlocking {
        // Modify the dispatcher to return fewer courses
        server.dispatcher = TestDispatcherWithFewerCourses()
        val courses = repository.loadCourses(CentreId(1))
        assertEquals(2, courses.older.size)
        assertEquals("2nd older", courses.older[0].name)
        assertEquals("1st older", courses.older[1].name)
    }

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

    /**
     * Dispatcher that returns 5 older courses (more than OLDER_COURSE_LIMIT).
     */
    private inner class TestDispatcher : Dispatcher() {
        private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.path.orEmpty()
            return when {
                path.contains("/courses") && path.startsWith("/staff/centres/") -> {
                    val older = path.contains("upcoming=0")
                    ok(
                        json.encodeToString(
                            CourseListDto(
                                if (older) olderCoursesWithFiveItems() else emptyList()
                            )
                        )
                    )
                }
                else -> MockResponse().setResponseCode(404).setBody("""{"msg":"not mocked $path"}""")
            }
        }

        private fun olderCoursesWithFiveItems() = listOf(
            CourseDto(5, 1, "5th older", "2026-07-01", "2026-07-10"),
            CourseDto(4, 1, "4th older", "2026-06-20", "2026-07-01"),
            CourseDto(3, 1, "3rd older", "2026-06-10", "2026-06-20"),
            CourseDto(2, 1, "2nd older", "2026-05-30", "2026-06-10"),
            CourseDto(1, 1, "1st older", "2026-05-20", "2026-05-30"),
        )

        private fun ok(body: String) = MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(body)
    }

    /**
     * Dispatcher that returns only 2 older courses (fewer than OLDER_COURSE_LIMIT).
     */
    private inner class TestDispatcherWithFewerCourses : Dispatcher() {
        private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.path.orEmpty()
            return when {
                path.contains("/courses") && path.startsWith("/staff/centres/") -> {
                    val older = path.contains("upcoming=0")
                    ok(
                        json.encodeToString(
                            CourseListDto(
                                if (older) olderCoursesWithTwoItems() else emptyList()
                            )
                        )
                    )
                }
                else -> MockResponse().setResponseCode(404).setBody("""{"msg":"not mocked $path"}""")
            }
        }

        private fun olderCoursesWithTwoItems() = listOf(
            CourseDto(2, 1, "2nd older", "2026-05-30", "2026-06-10"),
            CourseDto(1, 1, "1st older", "2026-05-20", "2026-05-30"),
        )

        private fun ok(body: String) = MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(body)
    }
}
