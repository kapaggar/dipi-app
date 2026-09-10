package org.dhamma.dipi.staff

import androidx.lifecycle.viewModelScope
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.RecordedRequest
import org.dhamma.dipi.staff.audit.ClientAudit
import org.dhamma.dipi.staff.data.DeskDispatchers
import org.dhamma.dipi.staff.database.ApplicantDao
import org.dhamma.dipi.staff.database.ApplicantEntity
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.ui.DeskScreen
import org.dhamma.dipi.staff.ui.DeskUiState
import org.dhamma.dipi.staff.network.SearchPageParser
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

@RunWith(RobolectricTestRunner::class)
class DeskProcessingTest {
    @get:Rule val rule = createComposeRule()
    private val html = """<script>var dataset = [{"aid":31,"centreid":63,"courseid":77,"name":"Fixture Student","gender":"M","confno":"OM1","app_status":"Confirmed","aadhar":"SYNTHETIC-NOT-AN-ID"}];</script>"""

    private class Rows : ApplicantDao {
        val stored = MutableStateFlow<List<ApplicantEntity>>(emptyList())
        override fun observe(courseId: Int): Flow<List<ApplicantEntity>> = stored.map { rows -> rows.filter { it.courseId == courseId } }
        override suspend fun list(courseId: Int) = stored.value.filter { it.courseId == courseId }
        override suspend fun listAll() = stored.value
        override suspend fun get(id: Int) = stored.value.firstOrNull { it.id == id }
        override suspend fun upsert(rows: List<ApplicantEntity>) { stored.value = rows }
        override suspend fun clear() { stored.value = emptyList() }
    }

    /** Pause actual computation at dispatch, without blocking its caller. */
    private class PausedComputation : CoroutineDispatcher() {
        val waiting = Channel<Pair<CoroutineContext, Runnable>>(Channel.UNLIMITED)
        val threads = CopyOnWriteArrayList<Thread>()
        @Volatile private var released = false
        @Synchronized override fun dispatch(context: CoroutineContext, block: Runnable) {
            if (released) execute(context, block) else waiting.trySend(context to block).getOrThrow()
        }
        fun execute(context: CoroutineContext, block: Runnable) {
            Dispatchers.Default.dispatch(context) {
                threads += Thread.currentThread()
                block.run()
            }
        }
        @Synchronized fun release() {
            released = true
            while (true) {
                val (context, block) = waiting.tryReceive().getOrNull() ?: break
                execute(context, block)
            }
        }
    }

    @Test fun callerRemainsResponsiveAndAuditedRowsStayEquivalentWithoutPersistingDisclosures() = runBlocking {
        val server = MockWebServer().apply { enqueue(MockResponse().setBody(html)); start() }
        val rows = Rows()
        val cpu = PausedComputation()
        val built = buildTestVm(server, applicants = rows, dispatchers = DeskDispatchers(cpu, Dispatchers.IO), useMock = false)
        try {
            val caller = Thread.currentThread()
            val fetch = async { built.repo.refreshApplicants(CourseId(77), centreId = CentreId(63)) }
            val (context, block) = withTimeout(5_000) { cpu.waiting.receive() }
            assertTrue("caller can run while processing is suspended", async { true }.await())
            assertTrue(rows.stored.value.isEmpty())
            assertFalse(fetch.isCompleted)
            cpu.execute(context, block)
            cpu.release()
            val (cards, counts) = withTimeout(5_000) { fetch.await() }
            val parsed = SearchPageParser.parse(html, 63)
            assertEquals(parsed.dataset.map { it.toModel() }, cards)
            assertEquals(1, counts["All"])
            val audited = withTimeout(5_000) { built.repo.observeApplicants(CourseId(77)).first() }
            val expected = cards.map { card ->
                card.copy(flags = ClientAudit.merge(ClientAudit.evaluate(card, cards, parsed.sensitive[card.id.value]), card.flags))
            }
            assertEquals(expected, audited)
            assertTrue(cpu.threads.isNotEmpty())
            assertTrue("all dispatched work stays off the caller", cpu.threads.all { it !== caller })
            assertFalse(rows.stored.value.single().payload.contains("SYNTHETIC-NOT-AN-ID"))
            assertEquals(1, server.requestCount)
            assertEquals("/search-course/63/77?s=&t=&g=&d=a", server.takeRequest().path)
        } finally {
            cpu.release()
            built.vm.viewModelScope.cancel()
            server.shutdown()
        }
    }

    @Test fun sessionExpiryDuringProcessingCannotRepopulateRowsOrSensitiveData() = runBlocking {
        val server = MockWebServer().apply { enqueue(MockResponse().setBody(html)); start() }
        val rows = Rows()
        val cpu = PausedComputation()
        val built = buildTestVm(server, applicants = rows, dispatchers = DeskDispatchers(cpu, Dispatchers.IO), useMock = false)
        try {
            val fetch = async { built.repo.refreshApplicants(CourseId(77), centreId = CentreId(63)) }
            val bodyRead = withTimeout(5_000) { cpu.waiting.receive() }
            cpu.execute(bodyRead.first, bodyRead.second)
            val parse = withTimeout(5_000) { cpu.waiting.receive() }
            built.repo.sessionExpired()
            cpu.execute(parse.first, parse.second)
            cpu.release()
            val error = runCatching { withTimeout(5_000) { fetch.await() } }.exceptionOrNull()
            assertTrue(error is CancellationException)
            assertTrue(rows.stored.value.isEmpty())
            assertTrue(built.repo.sensitiveSnapshot().isEmpty())
            assertEquals(1, server.requestCount)
        } finally {
            cpu.release()
            built.vm.viewModelScope.cancel()
            server.shutdown()
        }
    }

    @Test fun courseSwitchDuringProcessingCannotPublishOldRowsOrStartAnotherCoursesZeroDay() {
        val server = MockWebServer().apply { enqueue(MockResponse().setBody(html)); start() }
        val rows = Rows()
        val cpu = PausedComputation()
        val built = buildTestVm(server, applicants = rows, dispatchers = DeskDispatchers(cpu, Dispatchers.IO), useMock = false)
        try {
            val first = Course(CourseId(77), CentreId(63), "First fixture", "", "")
            val second = first.copy(id = CourseId(78), name = "Second fixture")
            rule.runOnIdle {
                built.connectivity.setOnlineForTest(true)
                built.vm.seedForTest(DeskUiState(course = first))
                built.vm.ensureDesk()
            }
            val paused = runBlocking { withTimeout(5_000) { cpu.waiting.receive() } }
            rule.runOnIdle { built.vm.pickCourse(second) }
            cpu.execute(paused.first, paused.second)
            cpu.release()
            rule.awaitTrue("old request completed its own Room cache") { rows.stored.value.isNotEmpty() }
            rule.waitForIdle()
            assertEquals(second, built.vm.state.value.course)
            assertTrue(built.vm.state.value.rows.isEmpty())
            assertTrue(built.vm.state.value.auditRows.isEmpty())
            assertTrue(built.vm.state.value.sensitiveById.isEmpty())
            assertEquals("no Zero Day for either course after leaving", 1, server.requestCount)
        } finally {
            cpu.release()
            built.vm.viewModelScope.cancel()
            server.shutdown()
        }
    }

    @Test fun currentSession403StillSignsOutAfterCourseSwitch() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val server = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    entered.countDown()
                    release.await(20, TimeUnit.SECONDS)
                    return MockResponse().setResponseCode(403).setBody("Access denied")
                }
            }
            start()
        }
        val built = buildTestVm(server, useMock = false)
        try {
            val prefs = org.robolectric.RuntimeEnvironment.getApplication()
                .getSharedPreferences("processing_auth_secure", 0)
            built.sessionStore.javaClass.getDeclaredField("secure\$delegate").apply {
                isAccessible = true
                set(built.sessionStore, lazy { prefs })
            }
            val first = Course(CourseId(77), CentreId(63), "First fixture", "", "")
            rule.runOnIdle {
                built.connectivity.setOnlineForTest(true)
                built.vm.seedForTest(DeskUiState(
                    screen = DeskScreen.Centre,
                    session = Session(1, "fixture", "Fixture", emptyList(), false),
                    course = first,
                ))
                built.vm.ensureDesk()
            }
            rule.awaitTrue("worklist request started") { entered.count == 0L }
            rule.runOnIdle { built.vm.pickCourse(first.copy(id = CourseId(78))) }
            release.countDown()
            rule.awaitTrue("current-session 403 returns to Sign-in") {
                built.vm.state.value.screen == DeskScreen.Login && built.vm.state.value.session == null
            }
            assertEquals(1, server.requestCount)
        } finally {
            release.countDown()
            built.vm.viewModelScope.cancel()
            server.shutdown()
        }
    }

    @Test fun refreshWithEqualDisclosuresStillPublishesChangedPublicRows() {
        val server = MockWebServer().apply {
            enqueue(MockResponse().setBody(html))
            enqueue(MockResponse().setBody("<table id=\"table-attending\"></table>"))
            start()
        }
        val rows = Rows()
        val built = buildTestVm(server, applicants = rows, useMock = false)
        try {
            rule.runOnIdle {
                built.connectivity.setOnlineForTest(true)
                built.vm.seedForTest(DeskUiState(course = Course(CourseId(77), CentreId(63), "Fixture", "", "")))
                built.vm.ensureDesk()
            }
            try {
                rule.awaitTrue("initial roll and Zero Day complete", timeoutMs = 20_000) {
                    built.vm.state.value.rows.singleOrNull()?.displayName == "Fixture Student" &&
                        server.requestCount == 2 && !built.vm.state.value.roomPullBusy
                }
            } catch (error: AssertionError) {
                val state = built.vm.state.value
                val paths = (1..server.requestCount).map { server.takeRequest(10, java.util.concurrent.TimeUnit.MILLISECONDS)?.path }
                throw AssertionError("initial rows=${state.rows.size}, requests=$paths, offline=${state.offline}, busy=${state.roomPullBusy}, course=${state.course?.id?.value}, session=${state.session != null}", error)
            }
            val initialDisclosures = built.vm.state.value.sensitiveById
            assertTrue(initialDisclosures.isNotEmpty())
            server.enqueue(MockResponse().setBody(html.replace("Fixture Student", "Changed Fixture")))
            rule.runOnIdle {
                // Pin the ordering where an equal, distinct snapshot reaches
                // state before the new Room emission. Loading makes the state
                // update observable even though the disclosure values match.
                built.vm.seedForTest(built.vm.state.value.copy(
                    sensitiveById = LinkedHashMap(initialDisclosures), loading = true,
                ))
                built.vm.refresh()
            }
            rule.awaitTrue("equal disclosure values must not discard a new roll") {
                built.vm.state.value.rows.singleOrNull()?.displayName == "Changed Fixture"
            }
            assertEquals(initialDisclosures, built.vm.state.value.sensitiveById)
            assertEquals(3, server.requestCount)
        } finally {
            built.vm.viewModelScope.cancel()
            server.shutdown()
        }
    }
}
