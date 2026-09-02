package org.dhamma.dipi.staff

import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.junit4.ComposeTestRule
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
import org.dhamma.dipi.staff.datastore.CourseOpsStore
import org.dhamma.dipi.staff.datastore.SessionStore
import org.dhamma.dipi.staff.network.DrupalAuthApi
import org.dhamma.dipi.staff.network.PhotoLoader
import org.dhamma.dipi.staff.network.SessionCookieJar
import org.dhamma.dipi.staff.network.StaffApi
import org.dhamma.dipi.staff.network.TokenStore
import org.dhamma.dipi.staff.ui.DeskViewModel
import org.junit.Assert.assertTrue
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import retrofit2.Retrofit

/**
 * Course-ops PIN store over a plain prefs file: Robolectric has no keystore,
 * so tests use the store's prefs seam (the SessionStore lazy-`secure` pattern).
 */
fun testCourseOpsStore(name: String = "test_course_ops"): CourseOpsStore =
    CourseOpsStore {
        RuntimeEnvironment.getApplication()
            .getSharedPreferences(name, Context.MODE_PRIVATE)
    }

class TestTokens : TokenStore {
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

private class EmptyApplicants : ApplicantDao {
    override fun observe(courseId: Int): Flow<List<ApplicantEntity>> = flowOf(emptyList())
    override suspend fun list(courseId: Int): List<ApplicantEntity> = emptyList()
    override suspend fun listAll(): List<ApplicantEntity> = emptyList()
    override suspend fun get(id: Int): ApplicantEntity? = null
    override suspend fun upsert(rows: List<ApplicantEntity>) = Unit
    override suspend fun clear() = Unit
}

private class EmptyOutbox : OutboxDao {
    override fun observePending(): Flow<List<OutboxEntity>> = flowOf(emptyList())
    override suspend fun pending(): List<OutboxEntity> = emptyList()
    override suspend fun insert(row: OutboxEntity): Long = 1L
    override suspend fun updateState(id: Long, state: String, message: String?) = Unit
    override suspend fun clear() = Unit
}

/** Everything a course-ops VM test needs to reach into. */
class TestVm(
    val vm: DeskViewModel,
    val sessionStore: SessionStore,
    val courseOpsStore: CourseOpsStore,
    val tokens: TestTokens,
)

/**
 * The SheetViewerTest recipe, shared: a real repository in mock mode against
 * [server] (start it with a dispatcher first), real SessionStore DataStore
 * prefs, and the course-ops PIN store over plain prefs.
 */
fun buildTestVm(
    server: MockWebServer,
    pinPrefsName: String = "test_course_ops",
    cookie: String? = null,
): TestVm {
    val app = RuntimeEnvironment.getApplication()
    // Before the VM: its init runs restore() inline up to the first real
    // suspension, so a cookie set afterwards is never seen.
    val tokens = TestTokens().apply { this.cookie = cookie }
    val json = Json { ignoreUnknownKeys = true }
    val base = "http://127.0.0.1:${server.port}/"
    val retrofit = Retrofit.Builder()
        .baseUrl(base)
        .client(OkHttpClient())
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
    val sessionStore = SessionStore(app)
    val courseOpsStore = testCourseOpsStore(pinPrefsName)
    val repo = StaffRepository(
        auth = retrofit.create(DrupalAuthApi::class.java),
        api = retrofit.create(StaffApi::class.java),
        tokens = tokens,
        sessionStore = sessionStore,
        courseOpsStore = courseOpsStore,
        applicants = EmptyApplicants(),
        outbox = EmptyOutbox(),
        json = json,
        cookies = SessionCookieJar(tokens),
        useMock = true,
        baseUrl = base,
        context = app,
    )
    val vm = DeskViewModel(
        repo,
        sessionStore,
        courseOpsStore,
        PhotoEditStore(app, json),
        PhotoLoader(OkHttpClient(), true, base, server),
        ConnectivityMonitor(app),
    )
    return TestVm(vm, sessionStore, courseOpsStore, tokens)
}

/**
 * Waits for [cond] while letting DataStore/OkHttp threads progress and the
 * Robolectric main looper drain — the coroutine seams the VM suspends on.
 */
fun ComposeTestRule.awaitTrue(message: String, cond: () -> Boolean) {
    val deadline = System.currentTimeMillis() + 5_000
    while (!cond() && System.currentTimeMillis() < deadline) {
        waitForIdle()
        Thread.sleep(20)
        shadowOf(Looper.getMainLooper()).idle()
    }
    assertTrue(message, cond())
}
