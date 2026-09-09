package org.dhamma.dipi.staff

import android.content.res.Configuration
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.dhamma.dipi.staff.course.*
import org.dhamma.dipi.staff.desk.*
import org.dhamma.dipi.staff.model.*
import org.dhamma.dipi.staff.settings.SettingsScreen
import org.dhamma.dipi.staff.teacher.*
import org.dhamma.dipi.staff.ui.theme.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Real Android rendering of generated fixtures. Never constructs a repository or signs in. */
@RunWith(AndroidJUnit4::class)
class V7DeviceVisualTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val session = Session(1, "synthetic", "Synthetic Registrar",
        listOf(Centre(CentreId(1), "Synthetic Dhamma Centre")), false)
    private val courses = (1..4).map { n ->
        Course(CourseId(n), CentreId(1), "10 Day Course $n", "2026-09-10", "2026-09-21")
    }
    private val people = (1..12).map { n ->
        ApplicantCard(ApplicantId(n), CentreId(1), CourseId(1), "Synthetic",
            familyName = if (n == 1) "Applicant With A Longer Family Name" else "Applicant $n",
            gender = Gender.M, status = ApplicantStatus(if (n == 12) "Left" else "Confirmed"),
            type = ApplicantType.Student, oldStudent = n % 2 == 0, attended = false,
            confNo = ConfNo("OM$n"), age = 40 + n, city = "Synthetic City",
            email = "synthetic.applicant.$n@example.invalid")
    }
    private val rooms = (1..80).map { AccoRoom("Mbk $it", Gender.M, "Mbk", number = "$it") }
    private val checkIns = people.take(6).associate { it.id to CheckInRecord(room = "Mbk ${it.id.value}", checkedIn = true) }
    private val rows = people.map { p ->
        RollRow(sn = p.id.value, applicantId = p.id, name = p.displayName, roleTag = null,
            room = "Mbk ${p.id.value}", age = "${p.age}", city = "Synthetic City",
            courses = listOf("10D" to 4), cell = "", seat = if (p.id.value == 12) "CW-A1" else "A${p.id.value}",
            seatKind = if (p.id.value == 12) SeatKind.CELL else SeatKind.FLOOR,
            backrest = p.id.value == 3, occupation = "Synthetic profession", education = "Synthetic", languages = "English")
    }
    private val group = RollGroup("Synthetic Teacher", "SYN", Gender.M, RollSeniority.OLD, "1", rows.size, rows)
    private val teacherRoll = TeacherRoll(listOf(group))
    private val application = ApplicationCard(name = rows.first().name, conf = "OM1", hasPhoto = false,
        historyCounts = ApplicationCard.HISTORY_ORDER.map { it to 0 },
        historyCountsPresent = ApplicationCard.HISTORY_ORDER.toSet(),
        firstCourse = "Not provided", lastCourse = "Not provided",
        health = ApplicationCard.HEALTH_ORDER.mapIndexed { i, label ->
            HealthRow(label, when (i) { 0 -> "Synthetic answer for layout verification. ".repeat(9); 1 -> "No"; 2 -> "Yes"; else -> "" })
        })

    private fun render(name: String, dark: Boolean = false, width: Int? = null, fontScale: Float = 1f,
        content: @Composable () -> Unit) {
        rule.runOnUiThread {
            rule.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            // Match MainActivity rather than the test manifest's default keyboard panning.
            rule.activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            Industry.apply(DeskSkin.Blossom)
        }
        rule.setContent {
            val density = LocalDensity.current
            val config = Configuration(LocalConfiguration.current).apply { width?.let { screenWidthDp = it } }
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale), LocalConfiguration provides config) {
                DipiTheme(dark = dark) {
                    Box(Modifier.fillMaxSize().background(LocalDipi.current.background)) {
                        Box((if (width == null) Modifier.fillMaxSize() else Modifier.width(width.dp).fillMaxHeight())
                            .background(LocalDipi.current.background).testTag("visual-frame")) { content() }
                    }
                }
            }
        }
        rule.waitForIdle()
        awaitWebContent()
        save(name)
    }

    private fun awaitWebContent() {
        fun webView(view: View): WebView? {
            if (view is WebView) return view
            if (view is ViewGroup) repeat(view.childCount) { index -> webView(view.getChildAt(index))?.let { return it } }
            return null
        }
        var web: WebView? = null
        rule.runOnUiThread { web = webView(rule.activity.window.decorView) }
        val current = web ?: return
        rule.waitUntil(10_000) {
            var loaded = false
            rule.runOnUiThread { loaded = current.progress == 100 }
            loaded
        }
        val ready = CountDownLatch(1)
        rule.runOnUiThread { current.postVisualStateCallback(1, object : WebView.VisualStateCallback() {
            override fun onComplete(requestId: Long) { ready.countDown() }
        }) }
        assertTrue("Web content must be ready before visual capture", ready.await(10, TimeUnit.SECONDS))
    }

    private fun save(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.filesDir, "v7-synthetic-evidence").apply { mkdirs() }
        val bitmap = rule.onNodeWithTag("visual-frame").captureToImage().asAndroidBitmap()
        File(folder, "$name.png").outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        File(folder, "device.txt").writeText("Generated fixtures only\nSDK=${android.os.Build.VERSION.SDK_INT}\n" +
            "version=${BuildConfig.VERSION_NAME}/${BuildConfig.VERSION_CODE}\nphotoReview=${BuildConfig.PHOTO_REVIEW_ENABLED}\n" +
            "pixels=${bitmap.width}x${bitmap.height}\n")
    }

    @Test fun centreActions() {
        render("c1-centre") { CentreScreen(session, courses, onPick = {}) }
        val first = rule.onNodeWithText("Course report").getUnclippedBoundsInRoot()
        val second = rule.onNodeWithText("App Settings").getUnclippedBoundsInRoot()
        assertTrue("Tablet centre actions share a row", kotlin.math.abs(first.top.value - second.top.value) < 2)
    }
    @Test fun centreSettings() = render("c3-centre-settings") {
        CentreOpsScreen(CentreOpsPrefs(rooms = rooms), {}, {}, {}, {}, {})
    }
    @Test fun teacherList() = render("t1-teacher-list") { TeacherListScreen(teacherRoll, "Synthetic / 10 Day / 2026") }
    @Test fun studentCard() = render("t3-student-card") { StudentCardScreen(rows.first(), group, application) }
    @Test fun studentCardDark() = render("t3-student-card-dark", dark = true) { StudentCardScreen(rows.first(), group, application) }
    @Test fun studentCardPhoneLargeText() = render("t3-student-card-phone-1.3", width = 412, fontScale = 1.3f) {
        StudentCardScreen(rows.first(), group, application)
    }
    @Test fun seatingPlan() = render("t2-seating-plan") { SeatingPlanScreen(teacherRoll, gridFor = { HallGrid(columns = 5, depth = 12) }) }
    @Test fun roomJumpRevealsTarget() {
        render("d6-room-chart") {
            var focused by remember { mutableStateOf<String?>(null) }
            RoomsPane(people, checkIns, rooms, focusedCode = focused, onFocusRoom = { focused = it },
                onJump = { value, scope -> focused = resolveRoomJump(scope, value)?.code })
        }
        rule.onNodeWithTag("room-jump-field").performTextInput("80")
        rule.onNodeWithTag("room-jump").performClick()
        // BringIntoViewRequester animates after the click and native IME/layout updates.
        rule.waitUntil(5_000) {
            runCatching { rule.onNodeWithTag("room-code-Mbk 80").assertIsDisplayed() }.isSuccess
        }
        rule.onNodeWithTag("room-code-Mbk 80").assertIsDisplayed()
        save("d6-room-focused")
        rule.onNodeWithText("Room Chart").assertIsDisplayed()
    }
    @Test fun auditComparison() {
        val flagged = people.first().copy(flags = listOf(AuditFlag(AuditSeverity.HARD, "Duplicate confirmation",
            "Synthetic comparison evidence", "conf_no_duplicate", listOf(people[1].id))))
        render("d3-audit") { AuditPane(listOf(flagged), "conf_no_duplicate", {}, { _, _ -> }, {}, allRows = listOf(flagged) + people.drop(1)) }
    }
    @Test fun sheetPhone() {
        render("d7-sheet-phone", width = 412) {
            SheetViewerPane("Day 0 list", SheetPayload.Html("Day 0 list",
                "<table><tr><th>Name</th><th>Room</th></tr><tr><td>Synthetic Applicant</td><td>Mbk 1</td></tr></table>",
                "https://example.invalid/"), false, {}, export = SheetExport.Day0List)
        }
        rule.onNodeWithTag("sheet-title").assertIsDisplayed()
        val title = rule.onNodeWithTag("sheet-title").getUnclippedBoundsInRoot()
        assertTrue("Phone title retains useful width", title.right.value - title.left.value > 40)
    }
    @Test fun board() = render("d1-board") { BoardPane(people, checkIns, emptyList(), emptyMap(), {}, {}) }
    @Test fun calling() = render("d4-calling") { CallingPane(people, emptyMap(), "To call", {}, { _, _ -> }, {}, {}, { _, _ -> }) }
    @Test fun checkIn() = render("d5-checkin") {
        CheckInPane(people, checkIns, rooms, "", "All", emptySet(), onScan = {}, onFilter = {}, onOpen = {})
    }
    @Test fun applications() = render("d2-applications") {
        ApplicationsPane(people, emptyMap(), people.first().id, {}, {}, {}, {})
    }
    @Test fun report() = render("c2-course-report") {
        val counts = CourseReportCounts(newMale = 4, newFemale = 4, newTotal = 8, oldMale = 6, oldFemale = 6,
            oldTotal = 12, rollTotal = 20, teacherConducting = 1, teacherAssistant = 1)
        CourseReportScreen(CourseReportUi(from = "2026-09-01", to = "2026-09-30", ran = true,
            report = CourseReport(listOf(CourseReportRow("Synthetic Centre / 10 Day / 2026 / 10 Sep - 21 Sep", counts,
                conductingTeachers = listOf("Synthetic Conducting Teacher"), assistingTeachers = listOf("Synthetic Assistant"))),
                counts, "2026-09-01", "2026-09-30")))
    }
    @Test fun settingsDark() = render("s4-settings-dark", dark = true) {
        SettingsScreen(session, true, null, 0, false, {}, onLogout = {}, appVersion = BuildConfig.VERSION_NAME)
    }
}
