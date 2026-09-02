package org.dhamma.dipi.staff

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockWebServer
import org.dhamma.dipi.staff.model.Centre
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.model.TabletMode
import org.dhamma.dipi.staff.network.DipiMockDispatcher
import org.dhamma.dipi.staff.ui.DeskScreen
import org.dhamma.dipi.staff.ui.DeskUiState
import org.dhamma.dipi.staff.ui.DipiAppUi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Spec 2a S3 — the device-PIN gate on Settings in course ops: a wrong PIN
 * keeps Settings closed ("Wrong PIN", dialog stays), the right one opens it;
 * the PIN survives logout's session wipe and only Erase-all clears it (after
 * which enabling asks to set a PIN again).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1240dp-h844dp-land")
class PinGateTest {
    @get:Rule
    val rule = createComposeRule()

    private val server = MockWebServer().apply { dispatcher = DipiMockDispatcher() }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private val session =
        Session(42, "sudha.user", "sudha.user", listOf(Centre(CentreId(12), "Dhamma Sudha")), false)

    private fun courseOpsState() = DeskUiState(
        screen = DeskScreen.TeacherRoll,
        mode = TabletMode.COURSE_OPS,
        session = session,
        course = Course(
            CourseId(77),
            CentreId(12),
            "Dhamma Sudha / 10 Day / 2026 / 2nd-Sep to 13th-Sep",
            "2026-09-02",
            "2026-09-13",
        ),
    )

    @Test
    fun wrongPinKeepsSettingsClosedAndTheRightPinOpensThem() {
        server.start()
        val t = buildTestVm(server, pinPrefsName = "pin_gate_prompt")
        t.courseOpsStore.wipeAll()
        t.courseOpsStore.setPin("4271")
        runBlocking { t.sessionStore.setTabletMode(TabletMode.COURSE_OPS) }
        t.vm.seedForTest(courseOpsState())
        rule.setContent { DipiAppUi(t.vm) }

        // The ⚙ affordance asks for the PIN instead of opening Settings.
        rule.onNodeWithTag("course-ops-settings").performClick()
        rule.onNodeWithTag("pin-dialog").assertIsDisplayed()
        assertEquals(DeskScreen.TeacherRoll, t.vm.state.value.screen)

        // Wrong PIN: the dialog stays, "Wrong PIN" shows, Settings stay closed.
        rule.onNodeWithTag("pin-input").performTextInput("0000")
        rule.onNodeWithTag("pin-submit").performClick()
        rule.awaitTrue("the wrong PIN should surface") {
            t.vm.state.value.pinError == "Wrong PIN"
        }
        rule.onNodeWithText("Wrong PIN").assertIsDisplayed()
        rule.onNodeWithTag("pin-dialog").assertIsDisplayed()
        assertEquals(DeskScreen.TeacherRoll, t.vm.state.value.screen)
        rule.onNodeWithText("TABLET MODE").assertDoesNotExist()

        // The right PIN opens Settings.
        rule.onNodeWithTag("pin-input").performTextClearance()
        rule.onNodeWithTag("pin-input").performTextInput("4271")
        rule.onNodeWithTag("pin-submit").performClick()
        rule.awaitTrue("the right PIN should open Settings") {
            t.vm.state.value.screen == DeskScreen.Settings
        }
        rule.onNodeWithText("TABLET MODE").assertIsDisplayed()
        // No desk surface came back with it.
        rule.onNodeWithTag("desk-rail").assertDoesNotExist()
    }

    /**
     * Logout wipes the session prefs files; the PIN lives in its own
     * `dipi_course_ops` file and stays. (SessionStore's own wipes cannot run
     * under Robolectric — no keystore — so the wipe is exercised at the file
     * level: clearing every session pref leaves the PIN store untouched.)
     */
    @Test
    fun pinSurvivesTheSessionWipe() {
        val store = testCourseOpsStore("pin_gate_lifecycle")
        store.wipeAll()
        store.setPin("4271")
        val app = RuntimeEnvironment.getApplication()
        app.getSharedPreferences("dipi_staff_secure", Context.MODE_PRIVATE)
            .edit().clear().commit()
        assertTrue(store.isPinSet())
        assertTrue(store.checkPin("4271"))
    }

    /** Erase-all clears the PIN; enabling afterwards asks to set one again. */
    @Test
    fun eraseAllClearsThePinAndEnablingAsksAgain() {
        server.start()
        val t = buildTestVm(server, pinPrefsName = "pin_gate_erase")
        t.courseOpsStore.wipeAll()
        t.courseOpsStore.setPin("4271")

        // The Erase-all path (StaffRepository.factoryReset) ends in wipeAll().
        t.courseOpsStore.wipeAll()
        assertFalse(t.courseOpsStore.isPinSet())
        assertFalse(t.courseOpsStore.checkPin("4271"))

        // Enabling now collects a fresh PIN before the mode flips.
        t.vm.seedForTest(DeskUiState(screen = DeskScreen.Settings, session = session))
        rule.setContent { DipiAppUi(t.vm) }
        t.vm.setTabletMode(TabletMode.COURSE_OPS)
        rule.awaitTrue("enabling after Erase-all should ask for a PIN") {
            t.vm.state.value.pinSetup
        }
        assertEquals(TabletMode.DESK, t.vm.state.value.mode)
        rule.onNodeWithText("Set a device PIN").assertIsDisplayed()
    }
}
