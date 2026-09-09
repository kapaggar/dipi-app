package org.dhamma.dipi.staff

import androidx.compose.runtime.*
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import org.dhamma.dipi.staff.desk.RoomsPane
import org.dhamma.dipi.staff.desk.resolveRoomJump
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.dhamma.dipi.staff.course.CentreScreen
import org.dhamma.dipi.staff.desk.SheetViewerPane
import org.dhamma.dipi.staff.model.*
import org.dhamma.dipi.staff.teacher.StudentCardScreen
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Regression checks use synthetic records and actual layout bounds. */
@RunWith(RobolectricTestRunner::class)
class DeskV7LayoutTest {
    @get:Rule val rule = createComposeRule()

    @Test
    @Config(qualifiers = "w1240dp-h844dp-land-mdpi")
    fun tabletCentreActionsShareOneRow() {
        val session = Session(uid = 1, name = "synthetic", displayName = "Synthetic",
            centres = listOf(Centre(CentreId(1), "Synthetic Centre")), modeTest = false)
        val course = Course(CourseId(10), CentreId(1), "10-Day", "2026-09-10", "2026-09-21")
        rule.setContent { DipiTheme { CentreScreen(session, listOf(course), onPick = {}) } }
        val report = rule.onNodeWithText("Course report").getUnclippedBoundsInRoot()
        val app = rule.onNodeWithText("App Settings").getUnclippedBoundsInRoot()
        val centre = rule.onNodeWithText("Centre Settings").getUnclippedBoundsInRoot()
        println("REVIEW centre report=$report app=$app centre=$centre")
        assertTrue("Tablet actions should share one row: report=$report app=$app centre=$centre",
            kotlin.math.abs(report.top.value - app.top.value) < 2f &&
            kotlin.math.abs(report.top.value - centre.top.value) < 2f)
    }

    @Test
    @Config(qualifiers = "w412dp-h900dp-mdpi")
    fun phoneSheetHeaderKeepsTitleAndPrintUsable() {
        rule.setContent {
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 1.3f)) {
            DipiTheme {
                var readingWidth by remember { mutableStateOf(SheetScreenWidth.FIT) }
                SheetViewerPane(title = "Day 0 list",
                    html = SheetPayload.Html("Day 0 list", "<html><body><table><tr><td>Synthetic</td></tr></table></body></html>", "https://example.invalid/"),
                    loading = false, onClose = {}, export = SheetExport.Day0List,
                    courseLine = "Synthetic course", screenWidth = readingWidth, onScreenWidth = { readingWidth = it })
            }
            }
        }
        val title = rule.onNodeWithTag("sheet-title", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val print = rule.onNodeWithTag("sheet-print", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val width = rule.onNodeWithTag("sheet-width", useUnmergedTree = true).getUnclippedBoundsInRoot()
        println("REVIEW sheet title=$title print=$print width=$width")
        assertTrue("Sheet title must retain positive usable width: $title", title.right.value - title.left.value >= 40f)
        assertTrue("Print must retain its target width: $print", print.right.value - print.left.value >= 48f)
        rule.onNodeWithTag("sheet-print").assertIsDisplayed()
        rule.onNodeWithTag("sheet-width").performClick()
        rule.onNodeWithText("Readable").assertIsDisplayed()
    }
    @Test
    @Config(qualifiers = "w900dp-h700dp-mdpi")
    fun jumpRevealsOffscreenRoomAndKeepsHeadingPinned() {
        val rooms = (1..200).map { AccoRoom("Mbk $it", Gender.M, "Mbk", number = "$it") }
        val occupant = ApplicantCard(id = ApplicantId(50), centreId = CentreId(1), courseId = CourseId(10),
            givenName = "Synthetic room", familyName = "Occupant", gender = Gender.M,
            status = ApplicantStatus("Confirmed"), type = ApplicantType.Student, oldStudent = false, attended = false)
        rule.setContent { DipiTheme {
            var focus by remember { mutableStateOf<String?>(null) }
            RoomsPane(listOf(occupant), mapOf(occupant.id to CheckInRecord(checkedIn = true, room = "Mbk 200")), rooms, focusedCode = focus,
                onFocusRoom = { focus = it },
                pendingSync = 20, syncFailures = (1..20).map { RoomSyncFailure(ApplicantId(it), "Synthetic refusal") },
                onJump = { query, candidates -> focus = resolveRoomJump(candidates, query)?.code })
        } }
        rule.onNodeWithTag("room-jump-field").performTextInput("missing")
        rule.onNodeWithTag("room-jump").performClick()
        rule.onNodeWithTag("room-jump-field").assertIsFocused()
        rule.onNodeWithTag("room-jump-field").performTextReplacement("200")
        rule.onNodeWithTag("room-jump").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("room-jump-field").assertIsNotFocused()
        rule.onNodeWithTag("room-code-Mbk 200").assertIsDisplayed()
        rule.onNodeWithText("Room Chart").assertIsDisplayed()
        rule.onNodeWithTag("room-jump-field").assertIsDisplayed()
        rule.onNodeWithTag("room-code-Mbk 1").performScrollTo()
        rule.onNodeWithTag("room-jump").performClick()
        rule.onNodeWithTag("room-code-Mbk 200").assertIsDisplayed()
        rule.onNodeWithTag("room-code-Mbk 1").performScrollTo()
        rule.onNodeWithTag("room-jump-field").performClick()
        rule.onNodeWithTag("room-next-occupied").performClick()
        rule.onNodeWithTag("room-jump-field").assertIsNotFocused()
        rule.onNodeWithTag("room-code-Mbk 200").assertIsDisplayed()
    }
    @Test
    @Config(qualifiers = "w412dp-h900dp-mdpi")
    fun narrowRoomFailuresLeaveUsableGrid() {
        rule.setContent { CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 1.3f)) {
            DipiTheme { RoomsPane(emptyList(), emptyMap(), listOf(AccoRoom("Mbk 1", Gender.M, "Mbk", number = "1")),
                pendingSync = 20, syncFailures = (1..20).map { RoomSyncFailure(ApplicantId(it), "Synthetic refusal") }) }
        } }
        val grid = rule.onNodeWithTag("room-grid").getUnclippedBoundsInRoot()
        assertTrue("Room grid retains room to read cells: $grid", grid.bottom.value - grid.top.value >= 100f)
        rule.onNodeWithText("Room Chart").assertIsDisplayed()
        rule.onNodeWithText("SYNC 20 TO SERVER").assertIsDisplayed()
    }
    @Test
    @Config(qualifiers = "w900dp-h700dp-mdpi")
    fun relatedRecordCanOpenWithoutHavingItsOwnFinding() {
        val other = ApplicantCard(id = ApplicantId(8), centreId = CentreId(1), courseId = CourseId(10),
            givenName = "Related synthetic", familyName = "Applicant", gender = Gender.F,
            status = ApplicantStatus("Confirmed"), type = ApplicantType.Student, oldStudent = false,
            attended = false, confNo = ConfNo("NF8"))
        val subject = other.copy(id = ApplicantId(7), givenName = "Subject synthetic", confNo = ConfNo("NF7"),
            flags = listOf(AuditFlag(AuditSeverity.HARD, "Duplicate confirmation", "Synthetic evidence", "conf_no_duplicate", listOf(other.id))))
        var opened: ApplicantId? = null
        rule.setContent { DipiTheme {
            org.dhamma.dipi.staff.desk.AuditPane(listOf(subject), "conf_no_duplicate", {}, { _, _ -> },
                { opened = it.id }, allRows = listOf(subject, other))
        } }
        rule.onNodeWithTag("audit-related-open-8").performScrollTo().performClick()
        rule.runOnIdle { org.junit.Assert.assertEquals(other.id, opened) }
        rule.onNodeWithTag("audit-evidence-list").assertIsDisplayed()
    }
}
