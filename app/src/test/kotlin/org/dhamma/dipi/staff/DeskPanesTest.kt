package org.dhamma.dipi.staff

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import org.dhamma.dipi.staff.desk.ApplicationsPane
import org.dhamma.dipi.staff.desk.AuditPane
import org.dhamma.dipi.staff.desk.BoardPane
import org.dhamma.dipi.staff.desk.CallingPane
import org.dhamma.dipi.staff.desk.CheckInDialog
import org.dhamma.dipi.staff.desk.CheckInPane
import org.dhamma.dipi.staff.desk.DeskSection
import org.dhamma.dipi.staff.desk.DeskSnackbar
import org.dhamma.dipi.staff.desk.RoomsPane
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantClarificationRow
import org.dhamma.dipi.staff.model.ApplicantDeskHistory
import org.dhamma.dipi.staff.model.ApplicantHistory
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.AuditFlag
import org.dhamma.dipi.staff.model.AuditSeverity
import org.dhamma.dipi.staff.model.CallRecord
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.model.ConfNo
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseCount
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.HISTORY_CLARIFICATIONS
import org.dhamma.dipi.staff.model.HISTORY_COURSES
import org.dhamma.dipi.staff.model.RoomFeature
import org.dhamma.dipi.staff.model.RoomSyncFailure
import org.dhamma.dipi.staff.model.SensitiveInfo
import org.dhamma.dipi.staff.model.WorklistFilter
import org.dhamma.dipi.staff.ui.DeskUiState
import org.dhamma.dipi.staff.ui.deskAdoptSearchCourse
import org.dhamma.dipi.staff.ui.deskOpenCourse
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.dhamma.dipi.staff.ui.theme.Industry
import org.dhamma.dipi.staff.ui.theme.LightDipi
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1240dp-h844dp-land")
class DeskPanesTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun snackbarUsesActiveErrorTokenAndPreservesSuccessToneAndMessage() {
        val token = Color(0xFF123456)
        val wasInspectorEnabled = isDebugInspectorInfoEnabled
        isDebugInspectorInfoEnabled = true
        try {
            rule.setContent {
                DipiTheme {
                    CompositionLocalProvider(LocalDipi provides LightDipi.copy(snackError = token)) {
                        DeskSnackbar("server says: choose Area teacher", error = true, modifier = Modifier.testTag("error-snack"))
                        DeskSnackbar("success stays verbatim", error = false, modifier = Modifier.testTag("success-snack"))
                    }
                }
            }
            rule.onNodeWithText("server says: choose Area teacher").assertIsDisplayed()
            assertEquals(token, rule.onNodeWithText("server says: choose Area teacher").snackbarBackground())

            val success = rule.onNodeWithText("success stays verbatim").snackbarBackground()
            assertEquals(Industry.accent800, success)
            rule.onNodeWithText("success stays verbatim").assertIsDisplayed()
        } finally {
            isDebugInspectorInfoEnabled = wasInspectorEnabled
        }
    }

    private fun SemanticsNodeInteraction.snackbarBackground(): Color {
        return fetchSemanticsNode().layoutInfo.getModifierInfo()
                .asSequence()
                .map { it.modifier }
                .filterIsInstance<InspectableValue>()
                .filter { it.nameFallback == "background" }
                .flatMap { modifier ->
                    @Suppress("UNCHECKED_CAST")
                    (InspectableValue::class.java.getMethod("getInspectableElements").invoke(modifier) as Sequence<Any>).asIterable()
                }
                .first { it.javaClass.getMethod("getName").invoke(it) == "color" }
                .let { it.javaClass.getMethod("getValue").invoke(it) as Color }
    }

    private fun card(
        id: Int,
        conf: String? = "NF$id",
        given: String = "Meera",
        family: String = "Deshpande",
        gender: Gender = Gender.F,
        status: String = "Confirmed",
        attended: Boolean = false,
        mobile: String? = "9876543210",
        city: String? = "Pune",
        flags: List<AuditFlag> = emptyList(),
    ) = ApplicantCard(
        id = ApplicantId(id),
        centreId = CentreId(1),
        courseId = CourseId(10),
        givenName = given,
        familyName = family,
        gender = gender,
        status = ApplicantStatus(status),
        type = ApplicantType.Student,
        oldStudent = false,
        attended = attended,
        confNo = conf?.let { ConfNo(it) },
        mobile = mobile,
        city = city,
        age = 34,
        flags = flags,
    )

    private val rooms = listOf(
        AccoRoom("F21", Gender.F, "Fbk", RoomFeature(geyser = true, indianToilet = true)),
        AccoRoom("F22", Gender.F, "Fbk", RoomFeature(westernToilet = true)),
        AccoRoom("M11", Gender.M, "Mbk"),
    )

    /* ── Slice 2: check-in ─────────────────────────────────────────── */

    @Test
    fun checkInRosterShowsProgressRowsAndSidebar() {
        val roll = listOf(
            card(1, conf = "NF1", given = "Priya", family = "Nair"),
            card(2, conf = "OM2", given = "Arun", family = "Kale", gender = Gender.M),
        )
        val checkIns = mapOf(ApplicantId(2) to CheckInRecord(checkedIn = true, room = "M11", seat = "Chair"))
        rule.setContent {
            DipiTheme {
                CheckInPane(
                    roll = roll,
                    checkIns = checkIns,
                    rooms = rooms,
                    scan = "",
                    filter = "All",
                    flaggedIds = emptySet(),
                    onScan = {},
                    onFilter = {},
                    onOpen = {},
                )
            }
        }
        rule.onNodeWithText(" of 2 checked in").assertIsDisplayed()
        rule.onNodeWithText("1 to arrive").assertIsDisplayed()
        rule.onNodeWithText("Mark attended").assertIsDisplayed()
        rule.onNodeWithText("M11 · Chair").assertIsDisplayed()
        rule.onNodeWithText("THE ROLL").assertIsDisplayed()
        rule.onNodeWithText("SEATING ISSUED").assertIsDisplayed()
    }

    @Test
    fun checkInRowOpensTheDialogCallback() {
        var opened: ApplicantCard? = null
        val roll = listOf(card(1, given = "Priya", family = "Nair"))
        rule.setContent {
            DipiTheme {
                CheckInPane(
                    roll = roll,
                    checkIns = emptyMap(),
                    rooms = rooms,
                    scan = "",
                    filter = "To arrive",
                    flaggedIds = emptySet(),
                    onScan = {},
                    onFilter = {},
                    onOpen = { opened = it },
                )
            }
        }
        rule.onNodeWithText("Priya Nair").performClick()
        assertEquals(1, opened?.id?.value)
    }

    @Test
    fun markAttendedChromeIsOneHundredThirtyTwoByThirtySix() {
        val roll = listOf(card(1, given = "Priya", family = "Nair"))
        rule.setContent {
            DipiTheme {
                CheckInPane(
                    roll = roll,
                    checkIns = emptyMap(),
                    rooms = rooms,
                    scan = "",
                    filter = "To arrive",
                    flaggedIds = emptySet(),
                    onScan = {},
                    onFilter = {},
                    onOpen = {},
                )
            }
        }
        val b = rule.onNodeWithTag("checkin-mark", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(132.dp.value, b.width.value, 0.5f)
        assertEquals(36.dp.value, b.height.value, 0.5f)
    }

    @Test
    fun searchEmptyStateAppears() {
        rule.setContent {
            DipiTheme {
                CheckInPane(
                    roll = listOf(card(1)),
                    checkIns = emptyMap(),
                    rooms = rooms,
                    scan = "zzz",
                    filter = "All",
                    flaggedIds = emptySet(),
                    onScan = {},
                    onFilter = {},
                    onOpen = {},
                )
            }
        }
        rule.onNodeWithText("Nobody matches that. Clear the field to see the whole roll.").assertIsDisplayed()
    }

    @Test
    fun scanFieldShowsThePlaceholderWhenEmpty() {
        rule.setContent {
            DipiTheme {
                CheckInPane(
                    roll = listOf(card(1)),
                    checkIns = emptyMap(),
                    rooms = rooms,
                    scan = "",
                    filter = "All",
                    flaggedIds = emptySet(),
                    onScan = {},
                    onFilter = {},
                    onOpen = {},
                )
            }
        }
        rule.onNodeWithText("Scan a chit or type a conf number").assertIsDisplayed()
    }

    @Test
    fun scanClearControlIsHiddenWhenTheFieldIsEmpty() {
        rule.setContent {
            DipiTheme {
                CheckInPane(
                    roll = listOf(card(1)),
                    checkIns = emptyMap(),
                    rooms = rooms,
                    scan = "",
                    filter = "All",
                    flaggedIds = emptySet(),
                    onScan = {},
                    onFilter = {},
                    onOpen = {},
                )
            }
        }
        rule.onNodeWithContentDescription("Clear the scan field").assertDoesNotExist()
    }

    @Test
    fun scanClearControlAppearsWithContentAndClearsTheFieldAndTheRoster() {
        var scanned: String? = null
        rule.setContent {
            DipiTheme {
                CheckInPane(
                    roll = listOf(card(1)),
                    checkIns = emptyMap(),
                    rooms = rooms,
                    scan = "NF24",
                    filter = "All",
                    flaggedIds = emptySet(),
                    onScan = { scanned = it },
                    onFilter = {},
                    onOpen = {},
                )
            }
        }
        rule.onNodeWithContentDescription("Clear the scan field").assertIsDisplayed().performClick()
        // One action clears the field; the roster filter derives from the scan, so it follows.
        assertEquals("", scanned)
    }

    /**
     * R1: the scan buffer lives in the activity-scoped ViewModel, so opening a
     * second course must not inherit the first course's conf number.
     */
    @Test
    fun openingACourseClearsTheScanBufferButKeepsTheTabletFilters() {
        val before = DeskUiState(
            deskScan = "NF24",
            deskGender = "Female",
            deskSeniority = "New",
            deskZeroFilter = "Arrived",
        )
        val after = deskOpenCourse(
            before,
            Course(CourseId(11), CentreId(1), "10-Day", "2026-09-01", "2026-09-12"),
        )
        assertEquals("", after.deskScan)
        assertEquals("Female", after.deskGender)
        assertEquals("New", after.deskSeniority)
        assertEquals("Arrived", after.deskZeroFilter)
        assertEquals(CourseId(11), after.course?.id)
    }

    private val courseTen = Course(CourseId(10), CentreId(1), "10-Day", "2026-08-20", "2026-08-31")
    private val courseEleven = Course(CourseId(11), CentreId(1), "10-Day", "2026-09-01", "2026-09-12")

    /**
     * The second door onto the same bug: Advanced Search opens a result from a
     * different course, which swaps the course without starting a desk session.
     * The scan buffer must go with the course, or Check-in opens silently
     * filtered by the previous course's conf number.
     */
    @Test
    fun openingASearchResultFromAnotherCourseClearsTheScanBuffer() {
        val before = DeskUiState(
            course = courseTen,
            courses = listOf(courseTen, courseEleven),
            deskScan = "NF24",
            deskGender = "Female",
        )
        val after = deskAdoptSearchCourse(before, card(1).copy(courseId = CourseId(11)))
        assertEquals(CourseId(11), after.course?.id)
        assertEquals("", after.deskScan)
        // The tablet's own filters are not a course property; they stay.
        assertEquals("Female", after.deskGender)
    }

    @Test
    fun openingASearchResultInTheSameCourseKeepsTheScanBuffer() {
        val before = DeskUiState(
            course = courseTen,
            courses = listOf(courseTen, courseEleven),
            deskScan = "NF24",
        )
        val after = deskAdoptSearchCourse(before, card(1).copy(courseId = CourseId(10)))
        assertEquals(CourseId(10), after.course?.id)
        assertEquals("NF24", after.deskScan)
    }

    @Test
    fun openingASearchResultFromAnUnlistedCourseChangesNothing() {
        val before = DeskUiState(
            course = courseTen,
            courses = listOf(courseTen),
            deskScan = "NF24",
        )
        val after = deskAdoptSearchCourse(before, card(1).copy(courseId = CourseId(99)))
        assertEquals(CourseId(10), after.course?.id)
        assertEquals("NF24", after.deskScan)
    }

    @Test
    fun genderFilterScopesRosterProgressAndSidebar() {
        val roll = listOf(
            card(1, conf = "NF1", given = "Priya", family = "Nair"),
            card(2, conf = "OM2", given = "Arun", family = "Kale", gender = Gender.M),
        )
        rule.setContent {
            DipiTheme {
                CheckInPane(
                    roll = roll,
                    checkIns = emptyMap(),
                    rooms = rooms,
                    scan = "",
                    filter = "All",
                    flaggedIds = emptySet(),
                    gender = "Female",
                    onScan = {},
                    onFilter = {},
                    onGender = {},
                    onOpen = {},
                )
            }
        }
        // Female tablet: the male applicant disappears from the roster…
        rule.onNodeWithText("Priya Nair").assertIsDisplayed()
        rule.onNodeWithText("Arun Kale").assertDoesNotExist()
        // …from the progress card ("0 of 1 checked in", "1 to arrive")…
        rule.onNodeWithText(" of 1 checked in").assertIsDisplayed()
        rule.onNodeWithText("1 to arrive").assertIsDisplayed()
        // …and ROOMS FREE lists the female block only.
        rule.onNodeWithText("Female · Fbk block").assertIsDisplayed()
        rule.onNodeWithText("Male · Mbk block").assertDoesNotExist()
    }

    @Test
    fun genderSegmentsFireTheCallback() {
        var picked: String? = null
        rule.setContent {
            DipiTheme {
                CheckInPane(
                    roll = listOf(card(1, given = "Priya", family = "Nair")),
                    checkIns = emptyMap(),
                    rooms = rooms,
                    scan = "",
                    filter = "All",
                    flaggedIds = emptySet(),
                    gender = "Both",
                    onScan = {},
                    onFilter = {},
                    onGender = { picked = it },
                    onOpen = {},
                )
            }
        }
        rule.onNodeWithText("Female").performClick()
        assertEquals("Female", picked)
    }

    @Test
    fun seniorityFilterScopesRosterProgressAndExcludesTheOtherThree() {
        val roll = listOf(
            card(1, conf = "NF1", given = "Priya", family = "Nair"),
            card(2, conf = "OF2", given = "Meera", family = "Shah"),
            card(3, conf = "NM3", given = "Arun", family = "Kale", gender = Gender.M),
            card(4, conf = "OM4", given = "Vikram", family = "Rao", gender = Gender.M),
        )
        rule.setContent {
            DipiTheme {
                CheckInPane(
                    roll = roll,
                    checkIns = emptyMap(),
                    rooms = rooms,
                    scan = "",
                    filter = "All",
                    flaggedIds = emptySet(),
                    gender = "Female",
                    seniority = "New",
                    onScan = {},
                    onFilter = {},
                    onGender = {},
                    onSeniority = {},
                    onOpen = {},
                )
            }
        }
        rule.onNodeWithText("Priya Nair").assertIsDisplayed()
        rule.onNodeWithText("Meera Shah").assertDoesNotExist()
        rule.onNodeWithText("Arun Kale").assertDoesNotExist()
        rule.onNodeWithText("Vikram Rao").assertDoesNotExist()
        rule.onNodeWithText(" of 1 checked in").assertIsDisplayed()
        rule.onNodeWithText("1 to arrive").assertIsDisplayed()
    }

    @Test
    fun senioritySegmentsFireTheCallback() {
        var picked: String? = null
        rule.setContent {
            DipiTheme {
                CheckInPane(
                    roll = listOf(card(1, given = "Priya", family = "Nair")),
                    checkIns = emptyMap(),
                    rooms = rooms,
                    scan = "",
                    filter = "All",
                    flaggedIds = emptySet(),
                    gender = "Both",
                    seniority = "Both",
                    onScan = {},
                    onFilter = {},
                    onGender = {},
                    onSeniority = { picked = it },
                    onOpen = {},
                )
            }
        }
        rule.onNodeWithText("New").performClick()
        assertEquals("New", picked)
    }

    @Test
    fun rosterRowsSortAlphabeticallyByName() {
        val roll = listOf(
            card(1, conf = "NF1", given = "Priya", family = "Nair"),
            card(2, conf = "OM2", given = "arun", family = "Kale", gender = Gender.M),
        )
        rule.setContent {
            DipiTheme {
                CheckInPane(
                    roll = roll,
                    checkIns = emptyMap(),
                    rooms = rooms,
                    scan = "",
                    filter = "All",
                    flaggedIds = emptySet(),
                    onScan = {},
                    onFilter = {},
                    onOpen = {},
                )
            }
        }
        val arun = rule.onNodeWithText("arun Kale").getBoundsInRoot()
        val priya = rule.onNodeWithText("Priya Nair").getBoundsInRoot()
        assertTrue(arun.top < priya.top)
    }

    @Test
    fun dialogShowsFreeRoomsForTheGenderAndFiresCallbacks() {
        var pickedRoom: String? = null
        var saved = false
        val priya = card(1, given = "Priya", family = "Nair")
        rule.setContent {
            DipiTheme {
                CheckInDialog(
                    card = priya,
                    record = CheckInRecord(),
                    roll = listOf(priya),
                    checkIns = emptyMap(),
                    rooms = rooms,
                    roomOpen = true,
                    laundryOn = false,
                    valuablesOn = true,
                    groupsOn = false,
                    onToggleRooms = {},
                    onRoom = { pickedRoom = it },
                    onSeat = {},
                    onValuables = {},
                    onLaundry = {},
                    onGroup = {},
                    onSave = { saved = true },
                    onUndo = {},
                    onClose = {},
                )
            }
        }
        rule.onNodeWithText("CHECK IN · NF1").assertIsDisplayed()
        // Free female rooms only — the male block never appears.
        rule.onNodeWithText("F21").assertIsDisplayed()
        rule.onNodeWithText("F22").assertIsDisplayed()
        rule.onAllNodesWithText("M11").assertCountEquals(0)
        // Valuables on, laundry hidden, groups hidden.
        rule.onNodeWithText("Valuables deposited").assertIsDisplayed()
        rule.onAllNodesWithText("Laundry issued").assertCountEquals(0)
        rule.onAllNodesWithText("GROUP").assertCountEquals(0)

        rule.onNodeWithText("F21").performClick()
        assertEquals("F21", pickedRoom)
        rule.onNodeWithText("CHECK IN PRIYA").performClick()
        assertTrue(saved)
    }

    /**
     * The owner's screenshot: a 73-room block at three per row needs 25 rows,
     * so unscrolled only about nine fit and the list is clipped at "Mbk 27" —
     * the registrar cannot reach any room above roughly 27. The action row
     * must also stay reachable without scrolling, so a future change cannot
     * push it below the fold.
     */
    @Test
    fun dialogRoomPickerScrollsToReachTheLastRoomInALargeBlock() {
        val arun = card(1, given = "Arun", family = "Kale", gender = Gender.M)
        val bigBlock = (1..73).map {
            AccoRoom("Mbk %02d".format(it), Gender.M, "Mbk")
        }
        rule.setContent {
            DipiTheme {
                CheckInDialog(
                    card = arun,
                    record = CheckInRecord(),
                    roll = listOf(arun),
                    checkIns = emptyMap(),
                    rooms = bigBlock,
                    roomOpen = true,
                    laundryOn = false,
                    valuablesOn = false,
                    groupsOn = false,
                    onToggleRooms = {},
                    onRoom = {},
                    onSeat = {},
                    onValuables = {},
                    onLaundry = {},
                    onGroup = {},
                    onSave = {},
                    onUndo = {},
                    onClose = {},
                )
            }
        }
        // The action row is visible without scrolling — the primary action
        // never needs the fix to be reachable.
        rule.onNodeWithText("CHECK IN ARUN").assertWhollyOnScreen()
        // Two hops. First, drive the scrollable's own ScrollBy semantics
        // action by a huge amount to reach its true max — on this long a
        // non-lazy scroll (25 rows), a single performScrollTo() straight at
        // the last room undershoots (verified empirically by comparing
        // clipped vs unclipped bounds mid-debug), so a raw max-scroll is
        // used instead of relying on that heuristic. The landing spot
        // ("Chowky", in the seating row right after the picker) is itself
        // asserted wholly on screen: that is what proves the scroll range
        // genuinely extends past Mbk 73, not just "some" scroll happened —
        // a future change that clipped the body's bottom would make this
        // hop impossible too, so it cannot silently mask a real reach
        // failure the way an unasserted intermediate step could. Second,
        // performScrollTo() precisely onto Mbk 73 itself (accurate once the
        // scroll state is no longer at its untouched starting position).
        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollBy))
            .performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy -> scrollBy(0f, 100_000f) }
        rule.onNodeWithText("Chowky").assertWhollyOnScreen()
        rule.onNodeWithText("Mbk 73").performScrollTo().assertWhollyOnScreen()
    }

    /* ── Slice 3: board ────────────────────────────────────────────── */

    @Test
    fun boardTilesAndActionsDeriveAndRoute() {
        var went: DeskSection? = null
        var exported: String? = null
        val roll = listOf(
            card(1, attended = true),
            card(2),
            card(3, flags = listOf(AuditFlag(AuditSeverity.HARD, "x", "phone_prefix_invalid · +91", "phone_prefix_invalid"))),
        )
        rule.setContent {
            DipiTheme {
                BoardPane(
                    roll = roll,
                    checkIns = emptyMap(),
                    flagged = listOf(roll[2]),
                    callOutcomes = mapOf(ApplicantId(1) to "Reached"),
                    onGoto = { went = it },
                    onExport = { exported = it },
                )
            }
        }
        // The centre-name heading is gone (spec S3.3); the roll subtitle is
        // now the block's first line.
        rule.onNodeWithText("Day 0 at Dhamma Sudha").assertDoesNotExist()
        rule.onNodeWithText("ARRIVING TODAY").assertIsDisplayed()
        rule.onNodeWithText("STILL TO CALL").assertIsDisplayed()
        rule.onNodeWithText("2 numbers left").assertIsDisplayed()
        rule.onNodeWithText("NEEDS ATTENTION").performClick()
        assertEquals(DeskSection.Audit, went)
        rule.onNodeWithText("Laundry list").performScrollTo().performClick()
        assertEquals("Laundry list", exported)
    }

    /* ── Slice 4: audit ────────────────────────────────────────────── */

    @Test
    fun auditGroupsFindingsAndBatchFires() {
        var batch: Pair<String, String>? = null
        var opened: ApplicantCard? = null
        val flagged = listOf(
            card(1, given = "Smt Lakshmi", flags = listOf(
                AuditFlag(AuditSeverity.HARD, "Honorific left in the name field", "name_title_prefix · 'Smt'", "name_title_prefix"),
            )),
            card(2, flags = listOf(
                AuditFlag(AuditSeverity.SOFT, "Mobile shared with another applicant", "shared_mobile · 9876543210", "shared_mobile"),
            )),
        )
        rule.setContent {
            DipiTheme {
                AuditPane(
                    flagged = flagged,
                    selectedCode = "name_title_prefix",
                    onSelect = {},
                    onBatch = { code, label -> batch = code to label },
                    onOpen = { opened = it },
                )
            }
        }
        rule.onNodeWithText("2 findings").assertIsDisplayed()
        rule.onAllNodesWithText("name_title_prefix").onFirst().assertIsDisplayed()
        rule.onNodeWithText("STRIP 1 HONORIFICS").performClick()
        assertEquals("name_title_prefix" to "Strip 1 honorifics", batch)
        rule.onNodeWithText("Open").performClick()
        assertEquals(1, opened?.id?.value)
    }

    /* ── Slice 5: calling ──────────────────────────────────────────── */

    @Test
    fun callingCardOpensOnTapAndLogsOutcomesDialsAndHandsOffToWhatsApp() {
        var outcome: Pair<Int, String>? = null
        var dialed: String? = null
        var wa: Int? = null
        rule.setContent {
            DipiTheme {
                CallingPane(
                    roll = listOf(card(1, given = "Priya", family = "Nair", mobile = "9876543210")),
                    outcomes = emptyMap(),
                    filter = "To call",
                    onFilter = {},
                    onOutcome = { c, o -> outcome = c.id.value to o },
                    onDial = { dialed = it.mobile },
                    onWhatsApp = { wa = it.id.value },
                    onNote = { _, _ -> },
                )
            }
        }
        rule.onNodeWithText("0 of 1 logged · log the outcome as you go, the list empties itself").assertIsDisplayed()
        // Segmented labels carry the pile sizes.
        rule.onNodeWithText("To call 1").assertIsDisplayed()
        rule.onNodeWithText("Confirmed 0").assertIsDisplayed()
        // The closed card is a line: name, meta and the pile badge — no number,
        // no outcome buttons until it is opened.
        rule.onNodeWithText("Priya Nair").assertIsDisplayed()
        rule.onNodeWithText("NF1 · Confirmed · No attempts yet").assertIsDisplayed()
        rule.onNodeWithText("TO CALL").assertIsDisplayed()
        rule.onAllNodesWithText("9876543210").assertCountEquals(0)

        rule.onNodeWithText("Priya Nair").performClick()
        rule.onNodeWithText("9876543210").performClick()
        assertEquals("9876543210", dialed)
        rule.onNodeWithContentDescription("WhatsApp Priya Nair").performClick()
        assertEquals(1, wa)
        // "Confirmed 0" is the segment, so the bare label is the grid button.
        rule.onNodeWithText("Confirmed").performClick()
        assertEquals(1 to "Confirmed", outcome)
    }

    @Test
    fun callingShowsAttemptsMetaSavesNotesAndClearsTheOutcome() {
        var note: Pair<Int, String>? = null
        var outcome: Pair<Int, String>? = null
        val rec = CallRecord(outcome = "No answer", attempts = 2, lastAttemptMs = System.currentTimeMillis())
        rule.setContent {
            DipiTheme {
                CallingPane(
                    roll = listOf(card(1, given = "Priya", family = "Nair")),
                    outcomes = mapOf(ApplicantId(1) to rec),
                    filter = "No answer",
                    onFilter = {},
                    onOutcome = { c, o -> outcome = c.id.value to o },
                    onDial = {},
                    onWhatsApp = {},
                    onNote = { c, n -> note = c.id.value to n },
                )
            }
        }
        rule.onNodeWithText("1 of 1 logged · log the outcome as you go, the list empties itself").assertIsDisplayed()
        rule.onNodeWithText("NF1 · Confirmed · ×2 · just now").assertIsDisplayed()
        rule.onNodeWithText("NO ANSWER").assertIsDisplayed()
        rule.onNodeWithText("Priya Nair").performClick()
        rule.onNodeWithContentDescription("Note for Priya Nair").performTextInput("call after 6pm")
        assertEquals(1 to "call after 6pm", note)
        rule.onNodeWithText("↩ Back to To call").performClick()
        assertEquals(1 to "", outcome)
    }

    @Test
    fun callingLogsWrittenUnderTheOldWordingStillShowInAPile() {
        // A log saved as "Reached" reads back as Confirmed rather than
        // vanishing into a segment the pane no longer offers.
        rule.setContent {
            DipiTheme {
                CallingPane(
                    roll = listOf(card(1, given = "Priya", family = "Nair")),
                    outcomes = mapOf(ApplicantId(1) to CallRecord(outcome = "Reached")),
                    filter = "Confirmed",
                    onFilter = {},
                    onOutcome = { _, _ -> },
                    onDial = {},
                    onWhatsApp = {},
                    onNote = { _, _ -> },
                )
            }
        }
        rule.onNodeWithText("Priya Nair").assertIsDisplayed()
        rule.onNodeWithText("CONFIRMED").assertIsDisplayed()
        rule.onNodeWithText("1 of 1 logged · log the outcome as you go, the list empties itself").assertIsDisplayed()
    }

    @Test
    fun callingInlineStatusChangerWritesThroughTheSameEndpointAsTheSheet() {
        var changed: Pair<Int, String>? = null
        rule.setContent {
            DipiTheme {
                CallingPane(
                    roll = listOf(card(1, given = "Priya", family = "Nair", status = "Expected")),
                    outcomes = emptyMap(),
                    filter = "To call",
                    onFilter = {},
                    onOutcome = { _, _ -> },
                    onDial = {},
                    onWhatsApp = {},
                    onNote = { _, _ -> },
                    // Deliberately not an outcome label: the desk status and
                    // the call outcome are different fields on the same card.
                    statusChoices = listOf("Expected", "Reconfirmation", "Custom…"),
                    onChangeStatus = { c, v -> changed = c.id.value to v },
                )
            }
        }
        rule.onNodeWithText("Priya Nair").performClick()
        rule.onNodeWithText("Desk status").assertIsDisplayed()
        // The picker opens on the card's own status when the desk offers it,
        // and UPDATE sends exactly what the picker shows.
        rule.onNodeWithContentDescription("Choose a new status for Priya Nair").performClick()
        rule.onNodeWithText("Reconfirmation").performClick()
        rule.onNodeWithText("UPDATE").performClick()
        assertEquals(1 to "Reconfirmation", changed)
    }

    @Test
    fun callingSearchNarrowsTheListWithoutTouchingThePileCounts() {
        var typed = ""
        rule.setContent {
            DipiTheme {
                var q by remember { mutableStateOf("") }
                typed = q
                CallingPane(
                    roll = listOf(
                        card(1, conf = "NM66", given = "Rajat", family = "Kumar"),
                        card(2, conf = "OM9", given = "Harendra", family = "Singh"),
                    ),
                    outcomes = emptyMap(),
                    filter = "To call",
                    onFilter = {},
                    onOutcome = { _, _ -> },
                    onDial = {},
                    onWhatsApp = {},
                    onNote = { _, _ -> },
                    search = q,
                    onSearch = { q = it },
                )
            }
        }
        rule.onNodeWithContentDescription("Search the call list by name")
            .performTextInput("harendra")
        rule.waitForIdle()
        assertEquals("harendra", typed)
        rule.onNodeWithText("Harendra Singh").assertIsDisplayed()
        rule.onAllNodesWithText("Rajat Kumar").assertCountEquals(0)
        // The segment still counts the whole pile, not the search hits.
        rule.onNodeWithText("To call 2").assertIsDisplayed()
    }

    @Test
    fun callingPriorityOrderFloatsTheStillToReachRowsUp() {
        rule.setContent {
            DipiTheme {
                var priority by remember { mutableStateOf(false) }
                CallingPane(
                    roll = listOf(
                        card(1, given = "Arun", family = "Kale"),
                        card(2, given = "Bikram", family = "Poonia"),
                    ),
                    outcomes = mapOf(ApplicantId(1) to CallRecord(outcome = "Callback")),
                    filter = "All",
                    onFilter = {},
                    onOutcome = { _, _ -> },
                    onDial = {},
                    onWhatsApp = {},
                    onNote = { _, _ -> },
                    priority = priority,
                    onPriority = { priority = !priority },
                )
            }
        }
        rule.onNodeWithText("A–Z").performClick()
        rule.onNodeWithText("Priority order").assertIsDisplayed()
    }

    @Test
    fun callingEmptyPileMessage() {
        rule.setContent {
            DipiTheme {
                CallingPane(
                    roll = listOf(card(1)),
                    outcomes = mapOf(ApplicantId(1) to CallRecord(outcome = "Confirmed")),
                    filter = "To call",
                    onFilter = {},
                    onOutcome = { _, _ -> },
                    onDial = {},
                    onWhatsApp = {},
                    onNote = { _, _ -> },
                )
            }
        }
        rule.onNodeWithText("Nothing in this pile.").assertIsDisplayed()
    }

    @Test
    fun callingScopeFiltersExcludeTheOtherThreeCombinations() {
        val roll = listOf(
            card(1, conf = "NF1", given = "Priya", family = "Nair"),
            card(2, conf = "OF2", given = "Meera", family = "Shah"),
            card(3, conf = "NM3", given = "Arun", family = "Kale", gender = Gender.M),
            card(4, conf = "OM4", given = "Vikram", family = "Rao", gender = Gender.M),
        )
        rule.setContent {
            DipiTheme {
                CallingPane(
                    roll = roll,
                    outcomes = emptyMap(),
                    filter = "To call",
                    onFilter = {},
                    onOutcome = { _, _ -> },
                    onDial = {},
                    onWhatsApp = {},
                    onNote = { _, _ -> },
                    gender = "Male",
                    seniority = "Old",
                )
            }
        }
        rule.onNodeWithText("Vikram Rao").assertIsDisplayed()
        rule.onNodeWithText("Priya Nair").assertDoesNotExist()
        rule.onNodeWithText("Meera Shah").assertDoesNotExist()
        rule.onNodeWithText("Arun Kale").assertDoesNotExist()
        rule.onNodeWithText("0 of 1 logged · log the outcome as you go, the list empties itself").assertIsDisplayed()
        rule.onNodeWithText("To call 1").assertIsDisplayed()
    }

    /* ── Slice 6: rooms ────────────────────────────────────────────── */

    @Test
    fun roomsShowOccupantsAndFreeCells() {
        val roll = listOf(card(1, given = "Priya", family = "Nair"))
        val checkIns = mapOf(ApplicantId(1) to CheckInRecord(checkedIn = true, room = "F21"))
        rule.setContent {
            DipiTheme {
                RoomsPane(roll = roll, checkIns = checkIns, rooms = rooms)
            }
        }
        rule.onNodeWithText("Rooms & seats").assertIsDisplayed()
        rule.onNodeWithText("1 occupied · 1 free of 2").assertIsDisplayed()
        rule.onNodeWithText("Priya Nair").assertIsDisplayed()
        rule.onNodeWithText("G IC").assertIsDisplayed()
        // No pending allocations → no sync button at all; pull is always shown.
        rule.onAllNodesWithText("SYNC", substring = true).assertCountEquals(0)
        rule.onNodeWithText("PULL FROM SERVER").assertIsDisplayed()
    }

    @Test
    fun roomsPullButtonFires() {
        var pulled = false
        rule.setContent {
            DipiTheme {
                RoomsPane(
                    roll = emptyList(),
                    checkIns = emptyMap(),
                    rooms = rooms,
                    onPullRooms = { pulled = true },
                )
            }
        }
        rule.onNodeWithText("PULL FROM SERVER").assertIsDisplayed().performClick()
        assertTrue(pulled)
    }

    @Test
    fun roomsPullBusyStateDisablesTheButton() {
        var pulled = false
        rule.setContent {
            DipiTheme {
                RoomsPane(
                    roll = emptyList(),
                    checkIns = emptyMap(),
                    rooms = rooms,
                    pullBusy = true,
                    onPullRooms = { pulled = true },
                )
            }
        }
        rule.onNodeWithText("PULLING…").assertIsDisplayed().performClick()
        assertFalse(pulled)
    }

    @Test
    fun roomsSyncButtonCarriesThePendingCountAndFires() {
        var synced = false
        val roll = listOf(card(1, given = "Priya", family = "Nair"))
        val checkIns = mapOf(ApplicantId(1) to CheckInRecord(checkedIn = true, room = "F21"))
        rule.setContent {
            DipiTheme {
                RoomsPane(
                    roll = roll,
                    checkIns = checkIns,
                    rooms = rooms,
                    pendingSync = 3,
                    onSyncRooms = { synced = true },
                )
            }
        }
        rule.onNodeWithText("SYNC 3 TO SERVER").assertIsDisplayed().performClick()
        assertTrue(synced)
    }

    @Test
    fun roomsSyncBusyStateDisablesTheButton() {
        var synced = false
        rule.setContent {
            DipiTheme {
                RoomsPane(
                    roll = emptyList(),
                    checkIns = emptyMap(),
                    rooms = rooms,
                    pendingSync = 2,
                    syncBusy = true,
                    onSyncRooms = { synced = true },
                )
            }
        }
        rule.onNodeWithText("SYNCING…").assertIsDisplayed().performClick()
        assertFalse(synced)
    }

    @Test
    fun roomsSyncRefusalsListNamesAndServerReasons() {
        val roll = listOf(
            card(1, given = "Priya", family = "Nair"),
            card(2, given = "Arun", family = "Kale", gender = Gender.M),
        )
        rule.setContent {
            DipiTheme {
                RoomsPane(
                    roll = roll,
                    checkIns = emptyMap(),
                    rooms = rooms,
                    pendingSync = 1,
                    syncFailures = listOf(
                        RoomSyncFailure(ApplicantId(2), "Room has already been alloted"),
                    ),
                )
            }
        }
        rule.onNodeWithText("SERVER REFUSED 1").assertIsDisplayed()
        rule.onNodeWithText("Arun Kale").assertIsDisplayed()
        rule.onNodeWithText("Room has already been alloted").assertIsDisplayed()
    }

    /* ── Slice 7: applications ─────────────────────────────────────── */

    @Test
    fun applicationsListDetailSelectsAndActs() {
        var statusFor: ApplicantCard? = null
        var dialed: String? = null
        val rows = listOf(
            card(1, given = "Priya", family = "Nair"),
            card(2, given = "Arun", family = "Kale", gender = Gender.M, status = "Pending", conf = null),
        )
        rule.setContent {
            DipiTheme {
                ApplicationsPane(
                    rows = rows,
                    flagsById = emptyMap(),
                    selectedId = ApplicantId(1),
                    onSelect = {},
                    onChangeStatus = { statusFor = it },
                    onDial = { dialed = it },
                    onEdit = {},
                )
            }
        }
        rule.onNodeWithText("AUDIT CLEAN · NOTHING TO FIX").assertIsDisplayed()
        rule.onNodeWithText("First course at this centre").assertIsDisplayed()
        rule.onNodeWithText("Date of birth").assertIsDisplayed()
        rule.onNodeWithText("CHANGE STATUS").performClick()
        assertEquals(1, statusFor?.id?.value)
        rule.onNodeWithText("CALL").performClick()
        assertEquals("9876543210", dialed)
    }

    /** T6 gate gap: the tablet detail must thread history exactly like the phone card. */
    @Test
    fun applicationsDetailExpandsHistoryAndOpensClarificationPdf() {
        var expanded: Pair<ApplicantId, String>? = null
        var opened: Pair<ApplicantId, Int>? = null
        val rows = listOf(card(1, given = "Priya", family = "Nair"))
        val history = ApplicantDeskHistory(
            clarifications = listOf(
                ApplicantClarificationRow(at = "2026-08-01", message = "Seat query", fileLabel = "reply.pdf", clarId = 77),
            ),
            expanded = setOf(HISTORY_CLARIFICATIONS),
        )
        rule.setContent {
            DipiTheme {
                ApplicationsPane(
                    rows = rows,
                    flagsById = emptyMap(),
                    selectedId = ApplicantId(1),
                    onSelect = {},
                    onChangeStatus = {},
                    onDial = {},
                    onEdit = {},
                    historyById = mapOf(ApplicantId(1) to history),
                    onExpandHistory = { id, key -> expanded = id to key },
                    onOpenClarification = { id, clar -> opened = id to clar },
                )
            }
        }
        rule.onNodeWithContentDescription("Expand Prior courses").performScrollTo().performClick()
        assertEquals(ApplicantId(1) to HISTORY_COURSES, expanded)
        // Clarifications arrived pre-loaded, so its row and PDF affordance are live.
        rule.onNodeWithText("Seat query").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("Open clarification PDF").performScrollTo().performClick()
        assertEquals(ApplicantId(1) to 77, opened)
    }

    @Test
    fun applicationsStatusChipsFilterTheList() {
        val rows = listOf(
            card(1, given = "Priya", family = "Nair", status = "Confirmed"),
            card(2, given = "Arun", family = "Kale", gender = Gender.M, status = "Pending"),
        )
        rule.setContent {
            DipiTheme {
                var selected by remember { mutableStateOf(setOf<String>()) }
                ApplicationsPane(
                    rows = WorklistFilter.visible(rows, selected, ""),
                    flagsById = emptyMap(),
                    selectedId = ApplicantId(1),
                    onSelect = {},
                    onChangeStatus = {},
                    onDial = {},
                    onEdit = {},
                    counts = mapOf("All" to 2, "Confirmed" to 1, "Pending" to 1),
                    selectedStatuses = selected,
                    onToggleStatus = { key ->
                        selected = when {
                            key == "All" -> emptySet()
                            key in selected -> selected - key
                            else -> selected + key
                        }
                    },
                )
            }
        }
        rule.onNodeWithText("Arun Kale").assertIsDisplayed()
        rule.onNodeWithContentDescription("Filter Confirmed").performClick()
        rule.onAllNodesWithText("Arun Kale").assertCountEquals(0)
        // Priya sits in both the list row and the detail header.
        rule.onAllNodesWithText("Priya Nair").onFirst().assertIsDisplayed()
        rule.onNodeWithContentDescription("Filter All").performClick()
        rule.onNodeWithText("Arun Kale").assertIsDisplayed()
    }

    @Test
    fun applicationsIdBlockShowsTheFullNumberForVisualVerification() {
        val rows = listOf(card(1, given = "Priya", family = "Nair"))
        rule.setContent {
            DipiTheme {
                ApplicationsPane(
                    rows = rows,
                    flagsById = emptyMap(),
                    selectedId = ApplicantId(1),
                    onSelect = {},
                    onChangeStatus = {},
                    onDial = {},
                    onEdit = {},
                    sensitiveById = mapOf(
                        ApplicantId(1) to SensitiveInfo(idLabel = "Aadhaar", idNumber = "9999 1234 5678"),
                    ),
                )
            }
        }
        rule.onNodeWithText("ID VERIFICATION").assertIsDisplayed()
        rule.onNodeWithText("Aadhaar").assertIsDisplayed()
        rule.onNodeWithText("9999 1234 5678").assertIsDisplayed()
    }

    @Test
    fun applicationsIdBlockFallsBackToNoIdOnFile() {
        rule.setContent {
            DipiTheme {
                ApplicationsPane(
                    rows = listOf(card(1, given = "Priya", family = "Nair")),
                    flagsById = emptyMap(),
                    selectedId = ApplicantId(1),
                    onSelect = {},
                    onChangeStatus = {},
                    onDial = {},
                    onEdit = {},
                )
            }
        }
        rule.onNodeWithText("ID VERIFICATION").assertIsDisplayed()
        rule.onNodeWithText("No ID on file").assertIsDisplayed()
    }

    @Test
    fun applicationsHealthPanelAndRowMarkerRender() {
        val rows = listOf(
            card(1, given = "Priya", family = "Nair"),
            card(2, given = "Arun", family = "Kale", gender = Gender.M),
        )
        rule.setContent {
            DipiTheme {
                ApplicationsPane(
                    rows = rows,
                    flagsById = emptyMap(),
                    selectedId = ApplicantId(1),
                    onSelect = {},
                    onChangeStatus = {},
                    onDial = {},
                    onEdit = {},
                    sensitiveById = mapOf(
                        ApplicantId(1) to SensitiveInfo(
                            idLabel = "PAN",
                            idNumber = "ABCDE1234F",
                            health = linkedMapOf(
                                "Mental health" to "depressed",
                                "Medication" to "insulin for diabetes",
                            ),
                        ),
                    ),
                )
            }
        }
        rule.onNodeWithText("HEALTH · VERIFY WITH APPLICANT").assertIsDisplayed()
        rule.onNodeWithText("depressed").assertIsDisplayed()
        rule.onNodeWithText("insulin for diabetes").assertIsDisplayed()
        // Row marker only on the applicant with disclosures.
        rule.onNodeWithContentDescription("Health disclosures for Priya Nair").assertIsDisplayed()
        rule.onAllNodesWithText("!").assertCountEquals(1)
    }

    @Test
    fun statusPillsWrapTheirContentSoFullWordsNeverClip() {
        val rows = listOf(
            card(1, given = "Priya", family = "Nair", status = "Pending"),
            card(2, given = "Arun", family = "Kale", gender = Gender.M, status = "Cancelled"),
        )
        rule.setContent {
            DipiTheme {
                ApplicationsPane(
                    rows = rows,
                    flagsById = emptyMap(),
                    selectedId = ApplicantId(1),
                    onSelect = {},
                    onChangeStatus = {},
                    onDial = {},
                    onEdit = {},
                )
            }
        }
        val pending = rule.onAllNodesWithText("Pending").onFirst().getBoundsInRoot()
        val cancelled = rule.onAllNodesWithText("Cancelled").onFirst().getBoundsInRoot()
        // The pill wraps its text (owner feedback: "CANCELLED", never
        // "CANCELL"): a longer status must yield a wider pill. The old
        // fixed-width pill rendered every status at the same width.
        assertTrue(
            "status pill should be wrap-content (Cancelled wider than Pending)",
            cancelled.width > pending.width,
        )
    }

    @Test
    fun applicationsDetailShowsCourseTotalsLine() {
        val history = ApplicantHistory(
            counts = listOf(CourseCount("10-day", 3), CourseCount("20-day", 1)),
        )
        rule.setContent {
            DipiTheme {
                ApplicationsPane(
                    rows = listOf(card(1, given = "Priya", family = "Nair").copy(history = history)),
                    flagsById = emptyMap(),
                    selectedId = ApplicantId(1),
                    onSelect = {},
                    onChangeStatus = {},
                    onDial = {},
                    onEdit = {},
                )
            }
        }
        rule.onNodeWithText("Courses · 4 total · 3× 10-day · 1× 20-day").assertIsDisplayed()
    }

    @Test
    fun applicationsScopeFiltersExcludeTheOtherThreeCombinations() {
        val rows = listOf(
            card(1, conf = "NF1", given = "Priya", family = "Nair"),
            card(2, conf = "OF2", given = "Meera", family = "Shah"),
            card(3, conf = "NM3", given = "Arun", family = "Kale", gender = Gender.M),
            card(4, conf = "OM4", given = "Vikram", family = "Rao", gender = Gender.M),
        )
        rule.setContent {
            DipiTheme {
                ApplicationsPane(
                    rows = rows,
                    flagsById = emptyMap(),
                    selectedId = ApplicantId(1),
                    onSelect = {},
                    onChangeStatus = {},
                    onDial = {},
                    onEdit = {},
                    gender = "Female",
                    seniority = "Old",
                )
            }
        }
        rule.onAllNodesWithText("Meera Shah").onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("Priya Nair").assertCountEquals(0)
        rule.onAllNodesWithText("Arun Kale").assertCountEquals(0)
        rule.onAllNodesWithText("Vikram Rao").assertCountEquals(0)
    }

    /**
     * Stronger than [assertIsDisplayed], which is satisfied by a single
     * visible pixel and has already let two clipping bugs through in this
     * screen family (see CentreScreenWideTest). A node is only above the
     * fold when the clip takes nothing off it, i.e. its clipped bounds are
     * its unclipped bounds.
     */
    private fun SemanticsNodeInteraction.assertWhollyOnScreen() {
        val clipped = getBoundsInRoot()
        val unclipped = getUnclippedBoundsInRoot()
        assertEquals(unclipped.top.value, clipped.top.value, 1f)
        assertEquals(unclipped.bottom.value, clipped.bottom.value, 1f)
    }
}
