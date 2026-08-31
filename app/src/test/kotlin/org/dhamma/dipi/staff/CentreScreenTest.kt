package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.height
import org.dhamma.dipi.staff.course.CentreScreen
import org.dhamma.dipi.staff.course.DeskTileAction
import org.dhamma.dipi.staff.course.centreDeskTiles
import org.dhamma.dipi.staff.course.courseCountsLine
import org.dhamma.dipi.staff.model.Centre
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.CourseMatrix
import org.dhamma.dipi.staff.model.CourseSummary
import org.dhamma.dipi.staff.model.MatrixRow
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CentreScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val session = Session(
        uid = 1,
        name = "sudha.user",
        displayName = "sudha.user",
        centres = listOf(Centre(CentreId(1), "Dhamma Sudha")),
        modeTest = false,
    )
    // Confirmed row deliberately all-nonzero so numeric assertions can't be
    // confused with the "·" that stands in for a zero elsewhere on screen.
    private val matrix = CourseMatrix(
        rows = listOf(
            MatrixRow("Received", newMale = 1, newFemale = 1),
            MatrixRow("Confirmed", newMale = 41, oldMale = 17, sevakMale = 3, newFemale = 33, oldFemale = 9, sevakFemale = 2),
            MatrixRow("Cancelled", newMale = 2, newFemale = 1),
        ),
        total = MatrixRow("Total", newMale = 50, oldMale = 20, sevakMale = 4, newFemale = 40, oldFemale = 15, sevakFemale = 3),
    )

    private val course = Course(
        CourseId(10),
        CentreId(1),
        "10-Day",
        "2026-08-20",
        "2026-08-31",
        summary = CourseSummary(received = 2, confirmed = 77, expected = 0, cancelled = 7, total = 111),
        matrix = matrix,
    )

    @Test
    fun catalogueOmitsLettersAtAndReferral() {
        val titles = centreDeskTiles(1).map { it.title }
        assertTrue(titles.contains("Centre Settings"))
        assertTrue(titles.contains("Bulk Mail"))
        assertFalse(titles.any { it.contains("Letter", ignoreCase = true) })
        assertFalse(titles.any { it.contains("AT", ignoreCase = true) })
        assertFalse(titles.any { it.contains("Referral", ignoreCase = true) })
        assertEquals(
            DeskTileAction.CentreOps,
            centreDeskTiles(1).first { it.title == "Centre Settings" }.action,
        )
        assertTrue(centreDeskTiles(1).any { it.title == "App Settings" && it.action == DeskTileAction.AppSettings })
        assertEquals("search-app/1", centreDeskTiles(1).first { it.title == "Advanced Search" }.route)
    }

    @Test
    fun catalogueRetiresManageCoursesDailyActivityAndSmsReport() {
        // S1 (owner decision 2026-08-30): three desk destinations leave the
        // app's surface entirely. Five remain — three native, two chips.
        val tiles = centreDeskTiles(1)
        assertEquals(
            listOf("Centre Settings", "Advanced Search", "App Settings", "Course Report", "Bulk Mail"),
            tiles.map { it.title },
        )
        listOf("Manage Courses", "Daily Activity", "SMS Report").forEach { gone ->
            assertFalse(tiles.any { it.title == gone })
        }
        assertEquals(3, tiles.count { it.action != null })
        assertEquals(2, tiles.count { it.action == null })
    }

    @Test
    fun theFiveSurvivingTilesRenderAndFireTheirCallbacks() {
        var ops = false
        var advanced = false
        var settingsOpened = false
        val fired = mutableListOf<Pair<String, String>>()
        rule.setContent {
            DipiTheme {
                CentreScreen(
                    session = session,
                    courses = listOf(course),
                    onPick = {},
                    onLater = { title, route -> fired += title to route },
                    onCentreOps = { ops = true },
                    onAdvancedSearch = { advanced = true },
                    onSettings = { settingsOpened = true },
                )
            }
        }
        listOf(
            "Centre Settings", "Advanced Search", "App Settings", "Course Report", "Bulk Mail",
        ).forEach { rule.onNodeWithText(it).performScrollTo().assertIsDisplayed() }

        rule.onNodeWithText("Centre Settings").performScrollTo().performClick()
        assertTrue(ops)
        rule.onNodeWithText("Advanced Search").performScrollTo().performClick()
        assertTrue(advanced)
        rule.onNodeWithText("App Settings").performScrollTo().performClick()
        assertTrue(settingsOpened)
        // The three native tiles never reach the desk-site path.
        assertTrue(fired.isEmpty())

        rule.onNodeWithText("Course Report").performScrollTo().performClick()
        rule.onNodeWithText("Bulk Mail").performScrollTo().performClick()
        assertEquals(
            listOf(
                "Course Report" to "centre/1/course-report",
                "Bulk Mail" to "centre/1/bulk-mail-schedule",
            ),
            fired,
        )
    }

    @Test
    fun countsLineDropsZeroesAndAbsentSummaries() {
        assertNull(courseCountsLine(null))
        assertNull(courseCountsLine(CourseSummary()))
        assertEquals(
            "Confirmed 77 | Cancelled 7 | Received 2 | Total 111",
            courseCountsLine(course.summary),
        )
        assertEquals(
            "Confirmed 12 · Expected 59 | Total 175",
            courseCountsLine(CourseSummary(confirmed = 12, expected = 59, total = 175)),
        )
        assertEquals("Expected 3", courseCountsLine(CourseSummary(expected = 3)))
    }

    @Test
    fun dashboardShowsCoursesCountsAndCentreRows() {
        var picked: Course? = null
        var later: Pair<String, String>? = null
        var ops = false
        var advanced = false
        rule.setContent {
            DipiTheme {
                CentreScreen(
                    session = session,
                    courses = listOf(course),
                    onPick = { picked = it },
                    onLater = { title, route -> later = title to route },
                    onCentreOps = { ops = true },
                    onAdvancedSearch = { advanced = true },
                )
            }
        }
        rule.onNodeWithText("Upcoming courses").assertIsDisplayed()
        rule.onNodeWithText("10-Day").assertIsDisplayed()
        // Owner feedback 2026-08-27: the "from your account" clause was
        // redundant and is gone from the header.
        rule.onNodeWithText("Dhamma Sudha · sudha.user").assertIsDisplayed()
        rule.onNodeWithText("Dhamma Sudha · from your account · sudha.user").assertDoesNotExist()
        // Finding 3: the kicker header is now per-cell (weight()-based, like
        // the data rows) rather than one manually-spaced literal string, so
        // each label sits above its column.
        rule.onNodeWithText("NM").assertIsDisplayed()
        rule.onNodeWithText("OM").assertIsDisplayed()
        rule.onNodeWithText("NF").assertIsDisplayed()
        rule.onNodeWithText("OF").assertIsDisplayed()
        // S3: Confirmed and Expected are summed into one fixed card row.
        rule.onNodeWithText("Confirmed + Expected").assertIsDisplayed()
        rule.onNodeWithText("41").assertIsDisplayed()
        rule.onNodeWithText("17").assertIsDisplayed()
        rule.onNodeWithText("58").assertIsDisplayed()
        rule.onNodeWithText("33").assertIsDisplayed()
        rule.onNodeWithText("9").assertIsDisplayed()
        rule.onNodeWithText("42").assertIsDisplayed()
        // The desk links render as a tile grid below the courses; Advanced
        // Search rides along as one of the tiles (owner feedback 2026-08-16).
        rule.onNodeWithText("Centre desk").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Advanced Search").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Centre Settings").performScrollTo().assertIsDisplayed()
        // S1 (owner decision 2026-08-30): these three desk destinations are
        // retired from the app's surface — pinned absent, not merely untested.
        rule.onNodeWithText("Manage Courses").assertDoesNotExist()
        rule.onNodeWithText("Daily Activity").assertDoesNotExist()
        rule.onNodeWithText("SMS Report").assertDoesNotExist()
        rule.onNodeWithText("Course Report").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Bulk Mail").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("App Settings").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Manage Letters").assertDoesNotExist()
        rule.onNodeWithText("AT Schedule").assertDoesNotExist()
        rule.onNodeWithText("Referral List").assertDoesNotExist()

        rule.onNodeWithText("10-Day").performScrollTo().performClick()
        assertEquals(course, picked)
        // The Advanced Search tile opens the in-app screen, not the desk site.
        rule.onNodeWithText("Advanced Search").performScrollTo().performClick()
        assertTrue(advanced)
        assertNull(later)
        // The tile is native now: it invokes onCentreOps directly and never
        // reaches the desk-site onLater path.
        rule.onNodeWithText("Centre Settings").performScrollTo().performClick()
        assertTrue(ops)
        assertNull(later)
    }

    @Test
    fun centreSettingsRowIsReachableWithoutCourses() {
        // Centre settings must stay reachable when the centre has no
        // upcoming courses — the invariant survived the redesign; only the
        // widget (now the "Centre Settings" tile, not a standalone card)
        // changed. App Settings lives in the same grid under the same risk.
        var ops = false
        var settingsOpened = false
        rule.setContent {
            DipiTheme {
                CentreScreen(
                    session = session,
                    courses = emptyList(),
                    onPick = {},
                    onCentreOps = { ops = true },
                    onSettings = { settingsOpened = true },
                )
            }
        }
        rule.onNodeWithText("No upcoming courses.").assertIsDisplayed()
        rule.onNodeWithText("App Settings").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Centre Settings").performScrollTo().performClick()
        assertTrue(ops)
        rule.onNodeWithText("App Settings").performScrollTo().performClick()
        assertTrue(settingsOpened)
    }

    @Test
    fun olderCoursesListOpensTheBoard() {
        val older = Course(
            CourseId(8),
            CentreId(1),
            "Dhamma Sudha / 10 Day / 2026 / 6th-Aug to 17th-Aug",
            "2026-08-06",
            "2026-08-17",
        )
        var picked: Course? = null
        rule.setContent {
            DipiTheme {
                CentreScreen(
                    session = session,
                    courses = listOf(course),
                    onPick = { picked = it },
                    olderCourses = listOf(older),
                )
            }
        }
        rule.onNodeWithText("Older courses").performScrollTo().assertIsDisplayed()
        // Owner feedback 2026-08-27: the sub-line was redundant and is gone;
        // the deletion is proven, not merely untested.
        rule.onNodeWithText("Teacher list · valuables · seating — check-in is closed")
            .assertDoesNotExist()
        rule.onNodeWithText(older.name).performScrollTo().performClick()
        assertEquals(older, picked)
    }

    @Test
    fun matrixCardSumsConfirmedAndExpectedIntoOneRow() {
        // S3: the card renders three fixed rows; Confirmed and Expected are
        // read together by the desk hand, so they are summed field-wise under
        // a single "Confirmed + Expected" label — neither is filtered out.
        val withExpected = course.copy(
            matrix = matrix.copy(rows = matrix.rows + MatrixRow("Expected", newMale = 6, oldFemale = 4)),
        )
        rule.setContent {
            DipiTheme {
                CentreScreen(session = session, courses = listOf(withExpected), onPick = {})
            }
        }
        rule.onNodeWithText("Received").assertIsDisplayed()
        rule.onNodeWithText("Confirmed + Expected").assertIsDisplayed()
        rule.onNodeWithText("Cancelled").assertIsDisplayed()
        rule.onNodeWithText("Confirmed").assertDoesNotExist()
        rule.onNodeWithText("Expected").assertDoesNotExist()
        // 41 + 6 new male, 17 old male -> 64 male; 33 new female,
        // 9 + 4 old female -> 46 female.
        rule.onNodeWithText("47").assertIsDisplayed()
        rule.onNodeWithText("64").assertIsDisplayed()
        rule.onNodeWithText("13").assertIsDisplayed()
        rule.onNodeWithText("46").assertIsDisplayed()
    }

    @Test
    fun cardKeepsAReceivedLineWhenTheMatrixHasNone() {
        // S3: equal card heights come from rendering every fixed row even
        // when the status is absent — the row is there, filled with middots.
        val noReceived = course.copy(
            matrix = CourseMatrix(
                rows = listOf(MatrixRow("Confirmed", newMale = 4, newFemale = 3)),
                total = MatrixRow("Total", newMale = 4, newFemale = 3),
            ),
        )
        rule.setContent {
            DipiTheme {
                CentreScreen(session = session, courses = listOf(noReceived), onPick = {})
            }
        }
        rule.onNodeWithText("Received").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Cancelled").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun coursesWithDifferentlyLengthedRealNamesGetEqualHeightCards() {
        // Gate-review Finding 1 (owner's literal request): "standardize the
        // heights of the boxes of coming courses". The ORIGINAL fix for that
        // pinned the name Text to minLines = 2 and rendered the date /
        // "starts in" lines unconditionally, so every card reserved height
        // for slots that (per StaffRepository.kt, which always builds
        // upcoming Courses with start = end = "") could never be filled.
        // That was the wrong mechanic — Bug A on the owner's 2026-08-30
        // screenshot: ~56dp of dead space between the title and the
        // MALE/FEMALE header on every card.
        //
        // The real invariant this test protects is narrower than "any two
        // names, however different in length, must match": it's that the
        // desk's actual one-line course names — which already carry their
        // own dates, e.g. "Dhamma Sudha / 10 Day / 2026 / 2nd-Sep to
        // 13th-Sep" — produce equal-height cards purely from the matrix's
        // constant three rows plus Total (cardRows), with no minLines
        // reservation needed. Two such names, of different lengths, share
        // the same matrix here so the only variable reaching the assertion
        // is the name string itself.
        val septemberCourse = Course(
            CourseId(30),
            CentreId(1),
            "Dhamma Sudha / 10 Day / 2026 / 2nd-Sep to 13th-Sep",
            "",
            "",
            matrix = matrix,
        )
        val octoberCourse = Course(
            CourseId(31),
            CentreId(1),
            "Dhamma Sudha / STP / 2026 / 21st-Oct to 29th-Oct",
            "",
            "",
            matrix = matrix,
        )
        rule.setContent {
            DipiTheme {
                CentreScreen(
                    session = session,
                    courses = listOf(septemberCourse, octoberCourse),
                    onPick = {},
                )
            }
        }
        val septemberCardHeight = rule.onNodeWithText(septemberCourse.name, useUnmergedTree = true)
            .onParent().getUnclippedBoundsInRoot().height
        val octoberCardHeight = rule.onNodeWithText(octoberCourse.name, useUnmergedTree = true)
            .onParent().getUnclippedBoundsInRoot().height
        assertEquals(septemberCardHeight.value, octoberCardHeight.value, 1f)
    }

    @Test
    fun zeroMatrixCellRendersAsMiddot() {
        val zeroMatrix = CourseMatrix(
            rows = listOf(MatrixRow("Confirmed", newMale = 5, oldMale = 0, newFemale = 3, oldFemale = 2)),
        )
        val withZero = course.copy(matrix = zeroMatrix)
        rule.setContent {
            DipiTheme {
                CentreScreen(session = session, courses = listOf(withZero), onPick = {})
            }
        }
        // S3 fixed rows: Received and Cancelled are absent, so both render
        // six middots; Confirmed + Expected contributes the one blank cell
        // (oldMale = 0). No Total row on this matrix. 6 + 1 + 6 = 13.
        // Unmerged tree: the card's clickable Column merges its descendants,
        // so the merged tree collapses every cell into one node and cannot
        // count them.
        rule.onAllNodesWithText("·", useUnmergedTree = true).assertCountEquals(13)
    }

    @Test
    fun cardWithNoMatrixAndNoSummaryShowsNoApplicationsYetInsteadOfAnEmptyBox() {
        // Bug B (owner screenshot 2026-08-30): cards 3/4 rendered only a
        // title — no matrix and no counts-line fallback — because both are
        // null for those course ids. Root cause (confirmed against the
        // desk's course_summary()/course.inc): it builds its per-course
        // summary-blocks only from courses it finds in the applicant query
        // for the window, so a course with no applicants yet never gets a
        // block at all — this is not a client parser bug (a four-block
        // fixture parses cleanly in CentrePageParserTest). Whatever upstream
        // cause produced the gap, the card itself must always say something
        // rather than render as a silent empty box.
        val noApplicantsYet = course.copy(matrix = null, summary = null)
        rule.setContent {
            DipiTheme {
                CentreScreen(session = session, courses = listOf(noApplicantsYet), onPick = {})
            }
        }
        rule.onNodeWithText("No applications yet").assertIsDisplayed()
        rule.onNodeWithText("NM").assertDoesNotExist()
    }

    @Test
    fun nullMatrixFallsBackToCountsLine() {
        val noMatrix = course.copy(matrix = null)
        rule.setContent {
            DipiTheme {
                CentreScreen(session = session, courses = listOf(noMatrix), onPick = {})
            }
        }
        rule.onNodeWithText("Confirmed 77 | Cancelled 7 | Received 2 | Total 111").assertIsDisplayed()
        // Gate-review fix (Finding 3): the fallback is exclusive — with no
        // matrix, the matrix table itself never renders alongside the counts
        // line, so its "NM" column header is genuinely absent rather than
        // just untested. (The prior assertion here checked for a literal
        // string no code path ever emits.)
        rule.onNodeWithText("NM").assertDoesNotExist()
    }

    @Test
    fun matrixHeaderShowsGroupCapsAboveAllSixColumnLabels() {
        // v4 frame 1a: the header gains a MALE/FEMALE group-caps row over the
        // two trios; the six column labels are unchanged in name, only in
        // weight (M and F darker than NM/OM/NF/OF).
        rule.setContent {
            DipiTheme {
                CentreScreen(session = session, courses = listOf(course), onPick = {})
            }
        }
        rule.onNodeWithText("MALE").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("FEMALE").performScrollTo().assertIsDisplayed()
        listOf("NM", "OM", "M", "NF", "OF", "F").forEach {
            rule.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
        // The sevak count is its own mono suffix beside "Total" now, not part
        // of the label string — and it ellipsises rather than clipping
        // mid-glyph when the phone's label column runs out of room.
        rule.onNodeWithText("Total").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("+7 sevak").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun deskSiteChipsStillFireOnLaterWithTheSameTitleAndRoute() {
        // The five `action == null` entries render as pill chips under
        // MORE ON THE DESK SITE; each still hands `onLater` exactly the
        // (title, route) pair `centreDeskTiles` publishes.
        val deskSite = centreDeskTiles(1).filter { it.action == null }
        val fired = mutableListOf<Pair<String, String>>()
        rule.setContent {
            DipiTheme {
                CentreScreen(
                    session = session,
                    courses = listOf(course),
                    onPick = {},
                    onLater = { title, route -> fired += title to route },
                )
            }
        }
        rule.onNodeWithText("MORE ON THE DESK SITE").performScrollTo().assertIsDisplayed()
        deskSite.forEach { rule.onNodeWithText(it.title).performScrollTo().performClick() }
        assertEquals(deskSite.map { it.title to it.route }, fired)
    }

    @Test
    fun appSettingsTileInvokesOnSettings() {
        var settingsOpened = false
        rule.setContent {
            DipiTheme {
                CentreScreen(
                    session = session,
                    courses = listOf(course),
                    onPick = {},
                    onSettings = { settingsOpened = true },
                )
            }
        }
        rule.onNodeWithText("App Settings").performScrollTo().performClick()
        assertTrue(settingsOpened)
    }
}
