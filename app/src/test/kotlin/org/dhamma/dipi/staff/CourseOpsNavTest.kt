package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockWebServer
import org.dhamma.dipi.staff.model.Centre
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.RollGroup
import org.dhamma.dipi.staff.model.RollRow
import org.dhamma.dipi.staff.model.RollSeniority
import org.dhamma.dipi.staff.model.SeatKind
import org.dhamma.dipi.staff.model.TabletMode
import org.dhamma.dipi.staff.model.TeacherRoll
import org.dhamma.dipi.staff.network.DipiMockDispatcher
import org.dhamma.dipi.staff.ui.DeskScreen
import org.dhamma.dipi.staff.ui.DeskUiState
import org.dhamma.dipi.staff.ui.DipiAppUi
import org.dhamma.dipi.staff.ui.deskBack
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Spec 2a S5 — the navigation swap. In COURSE_OPS with a seeded running
 * course: no desk rail, no queued strip even with queued > 0, TeacherRoll is
 * the start surface, and the ⚙ affordance is present at ≥48dp. The teacher
 * screens back deterministically and TeacherRoll is an exit-dialog root.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1240dp-h844dp-land")
class CourseOpsNavTest {
    @get:Rule
    val rule = createComposeRule()

    private val server = MockWebServer().apply { dispatcher = DipiMockDispatcher() }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private val session =
        Session(42, "sudha.user", "sudha.user", listOf(Centre(CentreId(12), "Dhamma Sudha")), false)

    private val running = Course(
        CourseId(77),
        CentreId(12),
        "Dhamma Sudha / 10 Day / 2026 / 2nd-Sep to 13th-Sep",
        "2026-09-02",
        "2026-09-13",
    )

    private fun courseOpsState(queued: Int = 0, offline: Boolean = false) = DeskUiState(
        screen = DeskScreen.TeacherRoll,
        mode = TabletMode.COURSE_OPS,
        session = session,
        course = running,
        queuedCount = queued,
        offline = offline,
    )

    private fun composeCourseOps(state: DeskUiState, prefs: String): TestVm {
        server.start()
        val t = buildTestVm(server, pinPrefsName = prefs)
        runBlocking { t.sessionStore.setTabletMode(TabletMode.COURSE_OPS) }
        t.vm.seedForTest(state)
        rule.setContent { DipiAppUi(t.vm) }
        return t
    }

    @Test
    fun noDeskRailNoQueuedStripAndTeacherRollIsTheStart() {
        composeCourseOps(courseOpsState(queued = 5), "nav_swap")

        // The teacher surface is up, on the locked running course.
        rule.onNodeWithTag("course-ops-host").assertIsDisplayed()
        rule.onNodeWithText("Teacher list").assertIsDisplayed()
        rule.onNodeWithText(running.name).assertIsDisplayed()
        // Wave-2 seam filled: with a course but no roll yet, the pending body shows.
        rule.onNodeWithTag("course-ops-roll-pending").assertIsDisplayed()

        // No desk surface composes: rail and queued strip stay out even
        // with queued > 0 — nothing writes in course ops.
        rule.onNodeWithTag("desk-rail").assertDoesNotExist()
        rule.onNodeWithTag("rail-accent-bar").assertDoesNotExist()
        rule.onNodeWithTag("queued-strip").assertDoesNotExist()

        // The ⚙ affordance is a ≥48dp target.
        val bounds = rule.onNodeWithTag("course-ops-settings").getUnclippedBoundsInRoot()
        assertTrue("⚙ width ${bounds.right - bounds.left}", bounds.right - bounds.left >= 48.dp)
        assertTrue("⚙ height ${bounds.bottom - bounds.top}", bounds.bottom - bounds.top >= 48.dp)
    }

    /** The offline strip is the one strip course ops keeps. */
    @Test
    fun offlineStripStillShowsButQueuedNeverDoes() {
        composeCourseOps(courseOpsState(queued = 3, offline = true), "nav_offline")
        rule.onNodeWithTag("offline-strip").assertIsDisplayed()
        rule.onNodeWithTag("queued-strip").assertDoesNotExist()
        rule.onNodeWithTag("course-ops-host").assertIsDisplayed()
    }

    /** No running course: the empty state, with the ⚙ affordance still reachable. */
    @Test
    fun emptyStateWhenNoCourseRunsKeepsTheSettingsDoor()
    {
        composeCourseOps(courseOpsState().copy(course = null), "nav_empty")
        rule.onNodeWithText("No course is running today").assertIsDisplayed()
        rule.onNodeWithTag("course-ops-settings").assertIsDisplayed()
        rule.onNodeWithTag("desk-rail").assertDoesNotExist()
    }

    /** The new DeskScreen values back deterministically (exhaustive in deskBack). */
    @Test
    fun teacherScreensBackToTheRollAndTheRollIsARoot() {
        // The card returns to whichever teacher screen opened it.
        assertEquals(DeskScreen.TeacherRoll, deskBack(DeskScreen.TeacherCard, DeskScreen.TeacherRoll))
        assertEquals(DeskScreen.SeatingPlan, deskBack(DeskScreen.TeacherCard, DeskScreen.SeatingPlan))
        assertEquals(DeskScreen.TeacherRoll, deskBack(DeskScreen.TeacherCard, null))
        assertEquals(DeskScreen.TeacherRoll, deskBack(DeskScreen.TeacherCard, DeskScreen.CourseHub))
        // The seating plan backs to the roll; the roll is a root like Login/Centre.
        assertEquals(DeskScreen.TeacherRoll, deskBack(DeskScreen.SeatingPlan, null))
        assertEquals(DeskScreen.TeacherRoll, deskBack(DeskScreen.TeacherRoll, null))
        // Settings opened from the roll back to the roll.
        assertEquals(DeskScreen.TeacherRoll, deskBack(DeskScreen.Settings, DeskScreen.TeacherRoll))
    }

    /** Integrator wiring: a fetched roll replaces the placeholder host body. */
    @Test
    fun fetchedRollRendersThroughTheCourseOpsBranch() {
        val roll = TeacherRoll(
            groups = listOf(
                RollGroup(
                    at = "Trainee A M Teacher", code = "TAM", gender = Gender.M,
                    seniority = RollSeniority.OLD, group = "1", total = 1,
                    rows = listOf(
                        RollRow(
                            sn = 1, name = "Pradeep Kandpal", roleTag = null,
                            room = "Mbk-33", age = "33", city = "Bageshwar",
                            courses = listOf("10D" to 6), cell = "",
                            seat = "A1", seatKind = SeatKind.FLOOR, backrest = false,
                            occupation = "India foundation",
                            education = "MA", languages = "English",
                        ),
                    ),
                ),
            ),
        )
        composeCourseOps(courseOpsState().copy(teacherRoll = roll), prefs = "pin-set")
        rule.onNodeWithText("Pradeep Kandpal").assertIsDisplayed()
        rule.onNodeWithTag("course-ops-placeholder").assertDoesNotExist()
        rule.onNodeWithTag("course-ops-roll-pending").assertDoesNotExist()
        rule.onNodeWithText("Seniority").assertIsDisplayed()
    }
}
