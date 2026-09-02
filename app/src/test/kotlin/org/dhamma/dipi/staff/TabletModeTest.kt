package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import okhttp3.mockwebserver.MockWebServer
import org.dhamma.dipi.staff.model.Centre
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.model.TabletMode
import org.dhamma.dipi.staff.network.DipiMockDispatcher
import org.dhamma.dipi.staff.settings.SettingsScreen
import org.dhamma.dipi.staff.ui.DeskScreen
import org.dhamma.dipi.staff.ui.DeskUiState
import org.dhamma.dipi.staff.ui.DipiAppUi
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Spec 2a S1/S4 — the mode state and the TABLET MODE settings card: DESK is
 * the default and the desk build still starts on Centre; enabling course ops
 * with no PIN on the device collects one first; the radio cards single-fire;
 * the consequence rows carry the frame's copy verbatim.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1240dp-h844dp-land")
class TabletModeTest {
    @get:Rule
    val rule = createComposeRule()

    private val server = MockWebServer().apply { dispatcher = DipiMockDispatcher() }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private val session =
        Session(42, "sudha.user", "sudha.user", listOf(Centre(CentreId(12), "Dhamma Sudha")), false)

    private fun runningCourse() = Course(
        CourseId(77),
        CentreId(12),
        "Dhamma Sudha / 10 Day / 2026 / 2nd-Sep to 13th-Sep",
        "2026-09-02",
        "2026-09-13",
    )

    /**
     * The desk build is byte-identical when the mode is off: a restored
     * session still lands on Centre, and the mode key reads DESK.
     */
    @Test
    fun centreStillStartsInDesk() {
        server.start()
        val t = buildTestVm(server, pinPrefsName = "tablet_mode_desk", cookie = "SESS=sess-demo")
        rule.setContent { DipiAppUi(t.vm) }
        rule.awaitTrue("restore should land on Centre") {
            t.vm.state.value.screen == DeskScreen.Centre
        }
        assertEquals(TabletMode.DESK, t.vm.state.value.mode)
        rule.onNodeWithTag("course-ops-host").assertDoesNotExist()
    }

    /** Enabling with no PIN set collects one (set + confirm) before the mode flips. */
    @Test
    fun enablingCourseOpsPromptsForPinSetupThenFlips() {
        server.start()
        val t = buildTestVm(server, pinPrefsName = "tablet_mode_enable")
        t.courseOpsStore.wipeAll()
        t.vm.todayProvider = { LocalDate.of(2026, 9, 5) }
        t.vm.seedForTest(
            DeskUiState(
                screen = DeskScreen.Settings,
                session = session,
                courses = listOf(runningCourse()),
            ),
        )
        rule.setContent { DipiAppUi(t.vm) }

        rule.onNodeWithTag("mode-course-ops").performScrollTo().performClick()
        rule.awaitTrue("the set-PIN dialog should open") { t.vm.state.value.pinSetup }
        rule.onNodeWithText("Set a device PIN").assertIsDisplayed()
        // The mode has NOT flipped yet.
        assertEquals(TabletMode.DESK, t.vm.state.value.mode)

        // CONFIRM stays disabled until both fields hold the same four digits.
        rule.onNodeWithTag("pin-set-input").performTextInput("4271")
        rule.onNodeWithTag("pin-set-submit").assertIsNotEnabled()
        rule.onNodeWithTag("pin-set-confirm").performTextInput("4271")
        rule.onNodeWithTag("pin-set-submit").performClick()

        rule.awaitTrue("the mode should flip after the PIN lands") {
            t.vm.state.value.mode == TabletMode.COURSE_OPS
        }
        assertTrue(t.courseOpsStore.isPinSet())
        assertTrue(t.courseOpsStore.checkPin("4271"))
        // The course lock resolved the running course on entry.
        assertEquals(CourseId(77), t.vm.state.value.course?.id)
    }

    /** The radio cards are single-fire: tapping the live card is a no-op. */
    @Test
    fun radioCardsSingleFire() {
        var fired = 0
        var picked: TabletMode? = null
        rule.setContent {
            DipiTheme {
                SettingsScreen(
                    session = session,
                    dark = false,
                    lastSync = null,
                    queued = 0,
                    offline = false,
                    onToggleTheme = {},
                    onLogout = {},
                    mode = TabletMode.DESK,
                    onMode = { fired++; picked = it },
                )
            }
        }
        // The already-selected card: a no-op, not a re-fire.
        rule.onNodeWithTag("mode-desk").performClick()
        assertEquals(0, fired)
        // The other card: exactly one call.
        rule.onNodeWithTag("mode-course-ops").performClick()
        assertEquals(1, fired)
        assertEquals(TabletMode.COURSE_OPS, picked)
    }

    /** Frame 2a copy, verbatim — titles, descriptions, consequence rows, cards. */
    @Test
    fun tabletModeCardCarriesTheFrameCopyVerbatim() {
        rule.setContent {
            DipiTheme {
                SettingsScreen(
                    session = session,
                    dark = false,
                    lastSync = null,
                    queued = 0,
                    offline = false,
                    onToggleTheme = {},
                    onLogout = {},
                    mode = TabletMode.COURSE_OPS,
                )
            }
        }
        rule.onNodeWithText("TABLET MODE").assertIsDisplayed()
        rule.onNodeWithText("Desk ops · registration").assertIsDisplayed()
        rule.onNodeWithText(
            "Board, applications, calling, check-in, rooms & seats, exports. " +
                "What the registrar uses on day 0.",
        ).assertIsDisplayed()
        rule.onNodeWithText("Course ops · teacher").assertIsDisplayed()
        rule.onNodeWithText(
            "Teacher list and seating plan only, for the running course. " +
                "Desk destinations are hidden until the mode is switched back.",
        ).assertIsDisplayed()
        rule.onNodeWithText("ON").assertIsDisplayed()
        rule.onNodeWithText("WHILE COURSE OPS IS ON").assertIsDisplayed()
        rule.onNodeWithText("Teacher list").assertIsDisplayed()
        rule.onNodeWithText("seniority + seating plan").assertIsDisplayed()
        rule.onNodeWithText("Student card").assertIsDisplayed()
        rule.onNodeWithText("application, read-only").assertIsDisplayed()
        rule.onNodeWithText("Board, applications, calling, check-in").assertIsDisplayed()
        rule.onNodeWithText("Exports, rooms & seats, bulk mail").assertIsDisplayed()
        assertEquals(2, rule.onAllNodesWithText("hidden").fetchSemanticsNodes().size)
        // The right column: the course lock and the static PIN row (no switch).
        rule.onNodeWithText("Course being taught").assertIsDisplayed()
        rule.onNodeWithText("No course is running today").assertIsDisplayed()
        rule.onNodeWithText(
            "Locked to the course that is running. The teacher never picks a course; " +
                "the roll follows the dates.",
        ).assertIsDisplayed()
        rule.onNodeWithText("Switching back asks for the device PIN").assertIsDisplayed()
    }

    /** The dashed card binds the running course when one is resolved. */
    @Test
    fun courseBeingTaughtShowsTheLockedCourse() {
        rule.setContent {
            DipiTheme {
                SettingsScreen(
                    session = session,
                    dark = false,
                    lastSync = null,
                    queued = 0,
                    offline = false,
                    onToggleTheme = {},
                    onLogout = {},
                    mode = TabletMode.COURSE_OPS,
                    runningCourseName = "Dhamma Sudha / 10 Day / 2026 / 2nd-Sep to 13th-Sep",
                    runningCourseDates = "2 Sep – 13 Sep 2026",
                )
            }
        }
        rule.onNodeWithText("Dhamma Sudha / 10 Day / 2026 / 2nd-Sep to 13th-Sep").assertIsDisplayed()
        rule.onNodeWithText("2 Sep – 13 Sep 2026").assertIsDisplayed()
        rule.onNodeWithText("No course is running today").assertDoesNotExist()
    }
}
