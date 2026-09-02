package org.dhamma.dipi.staff

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.dhamma.dipi.staff.data.PREFETCH_CONCURRENCY
import org.dhamma.dipi.staff.data.StaffRepository
import org.dhamma.dipi.staff.database.ApplicantDao
import org.dhamma.dipi.staff.database.ApplicantEntity
import org.dhamma.dipi.staff.database.OutboxDao
import org.dhamma.dipi.staff.database.OutboxEntity
import org.dhamma.dipi.staff.datastore.SessionStore
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.RollRow
import org.dhamma.dipi.staff.model.RollSeniority
import org.dhamma.dipi.staff.model.SeatKind
import org.dhamma.dipi.staff.model.RollGroup
import org.dhamma.dipi.staff.model.TabletMode
import org.dhamma.dipi.staff.model.TeacherRoll
import org.dhamma.dipi.staff.model.flagsFor
import org.dhamma.dipi.staff.network.ApplicantDto
import org.dhamma.dipi.staff.network.DipiMockDispatcher
import org.dhamma.dipi.staff.network.DrupalAuthApi
import org.dhamma.dipi.staff.network.SessionCookieJar
import org.dhamma.dipi.staff.network.StaffApi
import org.dhamma.dipi.staff.ui.DeskScreen
import org.dhamma.dipi.staff.ui.DeskUiState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Spec 2d S2 — the id-mapping + prefetch pipeline against the mock desk:
 * a fetched roll resolves applicant ids from the cached worklist, every
 * mapped application prefetches into the encrypted course cache (≤4
 * concurrent), failures stay silent and un-flagged, already-cached ids are
 * skipped, and logout wipes the course cache while keeping the device PIN.
 */
@RunWith(RobolectricTestRunner::class)
class TeacherCardPrefetchTest {

    private val server = MockWebServer().apply {
        dispatcher = DipiMockDispatcher()
        start()
    }

    @After
    fun stop() {
        server.shutdown()
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** The worklist rows the desk already cached — the join's id side. */
    private fun dto(id: Int, given: String, family: String, gender: String, age: Int) = ApplicantDto(
        id = id, centreId = 1, courseId = 10, givenName = given, familyName = family,
        gender = gender, status = "Confirmed", type = "Student", oldStudent = true,
        attended = true, age = age,
    )

    private val worklist = listOf(
        dto(1, "Meera", "Deshpande", "F", 34),
        dto(2, "Rakesh", "Iyer", "M", 28),
        dto(4, "Suresh", "Nair", "M", 51),
        dto(6, "Vikram", "Joshi", "M", 46),
        dto(10, "Arjun", "Patel", "M", 19),
        dto(12, "Nikhil", "Rane", "M", 35),
    )

    private inner class SeededApplicants : ApplicantDao {
        private val rows = worklist.map {
            ApplicantEntity(it.id, it.courseId, json.encodeToString(ApplicantDto.serializer(), it))
        }
        override fun observe(courseId: Int): Flow<List<ApplicantEntity>> = flowOf(rows)
        override suspend fun list(courseId: Int) = rows.filter { it.courseId == courseId }
        override suspend fun listAll() = rows
        override suspend fun get(id: Int) = rows.firstOrNull { it.id == id }
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

    /** Starts empty and keeps what refreshApplicants persists — the buffer-on-entry case. */
    private class RecordingApplicants : ApplicantDao {
        val stored = mutableListOf<ApplicantEntity>()
        override fun observe(courseId: Int): Flow<List<ApplicantEntity>> = flowOf(stored)
        override suspend fun list(courseId: Int) = stored.filter { it.courseId == courseId }
        override suspend fun listAll() = stored.toList()
        override suspend fun get(id: Int) = stored.firstOrNull { it.id == id }
        override suspend fun upsert(rows: List<ApplicantEntity>) { stored += rows }
        override suspend fun clear() { stored.clear() }
    }

    private fun buildRepo(
        pinPrefs: String,
        dao: ApplicantDao = SeededApplicants(),
    ): Pair<StaffRepository, org.dhamma.dipi.staff.datastore.CourseOpsStore> {
        val app = RuntimeEnvironment.getApplication()
        val tokens = TestTokens().apply { cookie = "SESS=deadbeef" }
        val base = "http://127.0.0.1:${server.port}/"
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl(base)
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        val courseOpsStore = testCourseOpsStore(pinPrefs)
        val repo = StaffRepository(
            auth = retrofit.create(DrupalAuthApi::class.java),
            api = retrofit.create(StaffApi::class.java),
            tokens = tokens,
            sessionStore = SessionStore(app),
            courseOpsStore = courseOpsStore,
            applicants = dao,
            outbox = EmptyOutbox(),
            json = json,
            cookies = SessionCookieJar(tokens),
            useMock = true,
            baseUrl = base,
            context = app,
        )
        return repo to courseOpsStore
    }

    @Test
    fun rollResolvesIdsPrefetchesAndCachesEveryMappedApplication() = runBlocking {
        val (repo, _) = buildRepo("prefetch_pipeline")

        // The one GET per entry, then the join over already-fetched data.
        val roll = repo.resolveTeacherRoll(10, repo.loadTeacherRoll(1, 10))
        val ids = roll.groups.flatMap { g -> g.rows.mapNotNull { it.applicantId?.value } }
        // Every fixture roll row maps: Suresh, Vikram, Nikhil, Rakesh, Arjun, Meera.
        assertEquals(setOf(4, 6, 12, 2, 10, 1), ids.toSet())
        // The mapped snapshot persisted for offline restarts.
        assertEquals(roll, repo.cachedTeacherRoll(10))

        // Prefetch all (> PREFETCH_CONCURRENCY of them) — silent, complete.
        assertEquals(4, PREFETCH_CONCURRENCY)
        val landed = mutableListOf<Int>()
        repo.prefetchApplicationViews(10, ids, onCard = { id, _ -> synchronized(landed) { landed += id } })
        assertEquals(ids.toSet(), landed.toSet())
        val cards = repo.cachedApplicationCards(10)
        assertEquals(ids.toSet(), cards.keys)

        // The flags derive from the landed answers (spec 2d S3).
        assertEquals(listOf("MED", "INTOX"), flagsFor(cards.getValue(4), Gender.M))
        assertEquals(listOf("HLTH", "PREG"), flagsFor(cards.getValue(1), Gender.F))
        assertEquals(listOf("TECH"), flagsFor(cards.getValue(2), Gender.M))
        assertEquals(emptyList<String>(), flagsFor(cards.getValue(10), Gender.M))

        // Already-cached ids are skipped on the next entry's prefetch.
        var again = 0
        repo.prefetchApplicationViews(10, ids, onCard = { _, _ -> again++ })
        assertEquals(0, again)
    }

    @Test
    fun prefetchFailuresAreSilentAndStayUnflagged() = runBlocking {
        val (repo, _) = buildRepo("prefetch_failures")
        val landed = mutableListOf<Int>()
        // 99 refuses (403 stand-in), 9999 is the wildcard-loader 404: both
        // silent, neither cached; the good id still lands.
        repo.prefetchApplicationViews(10, listOf(99, 9999, 4), onCard = { id, _ -> landed += id })
        assertEquals(listOf(4), landed)
        assertEquals(setOf(4), repo.cachedApplicationCards(10).keys)
    }

    @Test
    fun logoutWipesTheCourseCacheButKeepsTheDevicePin() = runBlocking {
        val (repo, store) = buildRepo("prefetch_logout")
        store.setPin("4271")
        repo.prefetchApplicationViews(10, listOf(4))
        assertTrue(repo.cachedApplicationCards(10).isNotEmpty())

        // Robolectric has no keystore: SessionStore.clear() inside logout()
        // throws AFTER the course-cache wipe — swallow it, assert the wipe.
        runCatching { repo.logout() }
        assertTrue(repo.cachedApplicationCards(10).isEmpty())
        assertNull(repo.cachedTeacherRoll(10))
        assertTrue(store.isPinSet())
        assertTrue(store.checkPin("4271"))
    }

    @Test
    fun tappingAnUnmappedRowAnswersHonestlyAndAMappedRowOpensTheCard() {
        val t = buildTestVm(server, pinPrefsName = "prefetch_vm")
        val mapped = RollRow(
            sn = 1, applicantId = ApplicantId(4), name = "Suresh Nair", roleTag = null,
            room = "Mbk-8", age = "51", city = "Kochi", courses = emptyList(), cell = "",
            seat = "CW-A3", seatKind = SeatKind.CELL, backrest = false,
            occupation = "", education = "", languages = "",
        )
        val unmapped = mapped.copy(sn = 2, applicantId = null, name = "Walk-in Person")
        val roll = TeacherRoll(
            listOf(
                RollGroup(
                    at = "(unassigned)", code = null, gender = Gender.M,
                    seniority = RollSeniority.OLD, group = "1", total = 2,
                    rows = listOf(mapped, unmapped),
                ),
            ),
        )
        t.vm.seedForTest(
            DeskUiState(
                screen = DeskScreen.TeacherRoll,
                mode = TabletMode.COURSE_OPS,
                teacherRoll = roll,
            ),
        )

        // No id → no card: the row stays, the tap answers honestly.
        t.vm.openTeacherCard(unmapped)
        assertEquals("Not on the worklist yet", t.vm.state.value.snack?.text)
        assertEquals(DeskScreen.TeacherRoll, t.vm.state.value.screen)

        // A mapped row opens the card at its group + index.
        t.vm.openTeacherCard(mapped)
        assertEquals(DeskScreen.TeacherCard, t.vm.state.value.screen)
        assertEquals("M-OLD-1", t.vm.state.value.teacherCard?.groupKey)
        assertEquals(0, t.vm.state.value.teacherCard?.index)

        // ‹ › stops at the ends and never lands on the id-less row.
        t.vm.stepTeacherCard(1)
        assertEquals(0, t.vm.state.value.teacherCard?.index)
        t.vm.stepTeacherCard(-1)
        assertEquals(0, t.vm.state.value.teacherCard?.index)
    }

    /**
     * Owner feedback 2026-09-02: course ops buffers its own worklist on entry —
     * without it the id mapping starves and every tap says "Not on the
     * worklist yet".
     */
    @Test
    fun emptyWorklistCacheIsBufferedOnCourseOpsEntry() = runBlocking {
        val dao = RecordingApplicants()
        val (repo, _) = buildRepo("buffer_on_entry", dao)
        val course = Course(CourseId(10), CentreId(1), "Dhamma Sudha / 10 Day / 2026 / 2nd-Sep to 13th-Sep", "", "")

        // Fresh entry: nothing cached, so the roll maps no ids.
        val starved = repo.resolveTeacherRoll(10, repo.loadTeacherRoll(1, 10))
        assertEquals(emptyList<Int>(), starved.groups.flatMap { g -> g.rows.mapNotNull { it.applicantId?.value } })

        // The entry hook pulls the worklist once, then the mapping works.
        repo.ensureCourseOpsWorklist(course)
        assertTrue(dao.stored.isNotEmpty())
        val roll = repo.resolveTeacherRoll(10, repo.loadTeacherRoll(1, 10))
        val ids = roll.groups.flatMap { g -> g.rows.mapNotNull { it.applicantId?.value } }
        assertTrue(ids.isNotEmpty())

        // A second entry with a warm cache does not refetch the worklist.
        val before = dao.stored.size
        repo.ensureCourseOpsWorklist(course)
        assertEquals(before, dao.stored.size)
    }

    /** The progress bar tracks ATTEMPTS so failures still walk it to the end. */
    @Test
    fun prefetchReportsAttemptLevelProgress() = runBlocking {
        val (repo, _) = buildRepo("prefetch_progress")
        val seen = mutableListOf<Pair<Int, Int>>()
        // One good id, one 403 stand-in, one 404 — all three count as attempts.
        repo.prefetchApplicationViews(
            10,
            listOf(4, 99, 9999),
            onProgress = { done, total -> synchronized(seen) { seen += done to total } },
        )
        assertEquals(0 to 3, seen.first())
        assertEquals(3, seen.last().first)
        assertEquals(3, seen.last().second)
    }
}
