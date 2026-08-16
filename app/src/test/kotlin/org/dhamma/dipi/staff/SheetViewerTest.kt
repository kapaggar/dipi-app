package org.dhamma.dipi.staff

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.CompletableDeferred
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
import org.dhamma.dipi.staff.model.Centre
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.model.SheetPayload
import org.dhamma.dipi.staff.network.DipiMockDispatcher
import org.dhamma.dipi.staff.network.DrupalAuthApi
import org.dhamma.dipi.staff.network.PhotoLoader
import org.dhamma.dipi.staff.network.SessionCookieJar
import org.dhamma.dipi.staff.network.StaffApi
import org.dhamma.dipi.staff.network.TokenStore
import org.dhamma.dipi.staff.ui.DeskScreen
import org.dhamma.dipi.staff.ui.DeskUiState
import org.dhamma.dipi.staff.ui.DeskViewModel
import org.dhamma.dipi.staff.ui.DipiAppUi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import java.io.File

/**
 * The Board export → sheet viewer loop wired through DipiAppUi against the
 * frozen repository seam: an export tap opens the in-app HTML viewer, a
 * document hands off to the system viewer, a refusal surfaces verbatim on the
 * desk snackbar, and close returns to the Board pane untouched underneath.
 *
 * The repository runs in real mock mode (MockWebServer + DipiMockDispatcher)
 * so the desk's on-open worklist fetch succeeds quietly; the sheet fetch
 * itself is stubbed via the VM's test seams to exercise each payload shape.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1240dp-h844dp-land")
class SheetViewerTest {
    @get:Rule
    val rule = createComposeRule()

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

    private val centre = Centre(CentreId(12), "Dhamma Sudha")

    private fun deskState() = DeskUiState(
        screen = DeskScreen.CourseHub,
        session = Session(
            uid = 5,
            name = "registrar.sudha",
            displayName = "registrar.sudha",
            centres = listOf(centre),
            modeTest = false,
        ),
        course = Course(CourseId(77), CentreId(12), "10-Day", "2 Sep 2026", "13 Sep 2026"),
    )

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
    fun exportTapShowsLoadingThenTheHtmlViewerWithItsTitle() {
        val vm = buildVm()
        val gate = CompletableDeferred<SheetPayload>()
        vm.sheetFetch = { _, _, _ -> gate.await() }
        vm.seedForTest(deskState())
        rule.setContent { DipiAppUi(vm) }

        // A genuine tap on the Board export cell (scrolled into view).
        rule.onNodeWithText("Day 0 list").performScrollTo().performClick()
        rule.onNodeWithTag("sheet-viewer").assertExists()
        rule.onNodeWithTag("sheet-view-loading").assertExists()

        gate.complete(
            SheetPayload.Html("Day 0 list", "<html><body>ROLL</body></html>", "http://localhost/"),
        )
        rule.waitForIdle()
        rule.onNodeWithTag("sheet-title").assertTextEquals("Day 0 list")
        rule.onNodeWithTag("sheet-web").assertExists()
        rule.onNodeWithTag("sheet-view-loading").assertDoesNotExist()
    }

    @Test
    fun notAvailableClosesTheViewerAndShowsTheServerMessageVerbatim() {
        val vm = buildVm()
        vm.sheetFetch = { _, _, _ ->
            SheetPayload.NotAvailable("Course report is not available until Day 10")
        }
        vm.seedForTest(deskState())
        rule.setContent { DipiAppUi(vm) }

        rule.runOnIdle { vm.openSheet("Course report") }
        rule.waitForIdle()
        rule.onNodeWithTag("sheet-viewer").assertDoesNotExist()
        assertEquals(
            "Course report is not available until Day 10",
            vm.state.value.snack?.text,
        )
        rule.onNodeWithText("Course report is not available until Day 10").assertIsDisplayed()
    }

    @Test
    fun documentPayloadHandsOffToTheSystemViewer() {
        val vm = buildVm()
        val app = RuntimeEnvironment.getApplication()
        val file = File(app.cacheDir, "sheets/male.pdf").apply {
            parentFile!!.mkdirs()
            writeText("pdf")
        }
        vm.sheetFetch = { _, _, _ -> SheetPayload.Document("Male PDF", file, "application/pdf") }
        vm.seedForTest(deskState())
        rule.setContent { DipiAppUi(vm) }

        rule.runOnIdle { vm.openSheet("Male PDF") }
        rule.waitForIdle()

        val started = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals("application/pdf", started.type)
        assertEquals(
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
            started.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        assertEquals("org.dhamma.dipi.staff.fileprovider", started.data?.authority)
        // The one-shot is consumed and no in-app viewer opens for documents.
        assertNull(vm.state.value.openDoc)
        rule.onNodeWithTag("sheet-viewer").assertDoesNotExist()
    }

    @Test
    fun closeReturnsToTheBoardUnderneath() {
        val vm = buildVm()
        vm.sheetFetch = { _, _, _ ->
            SheetPayload.Html("Seating plan", "<html/>", "http://localhost/")
        }
        vm.seedForTest(deskState())
        rule.setContent { DipiAppUi(vm) }

        rule.runOnIdle { vm.openSheet("Seating plan") }
        rule.waitForIdle()
        rule.onNodeWithTag("sheet-viewer").assertExists()

        rule.onNodeWithText("CLOSE").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("sheet-viewer").assertDoesNotExist()
        // The Board pane is still the section underneath.
        rule.onNodeWithText("SHEETS & EXPORTS · RARELY URGENT").assertIsDisplayed()
    }

    @Test
    fun appEditOpensTheViewerTitledForTheApplicant() {
        val vm = buildVm()
        vm.editFetch = { SheetPayload.Html("app-edit", "<html/>", "http://localhost/") }
        vm.seedForTest(deskState())
        rule.setContent { DipiAppUi(vm) }

        rule.runOnIdle { vm.openAppEdit(card()) }
        rule.waitForIdle()
        rule.onNodeWithTag("sheet-viewer").assertExists()
        rule.onNodeWithTag("sheet-title").assertTextEquals("Edit · Meera Kulkarni")
        rule.onNodeWithTag("sheet-web").assertExists()
    }
}
