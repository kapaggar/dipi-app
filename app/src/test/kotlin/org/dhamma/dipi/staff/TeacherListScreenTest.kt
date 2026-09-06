package org.dhamma.dipi.staff

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.RollGroup
import org.dhamma.dipi.staff.model.RollRow
import org.dhamma.dipi.staff.model.RollSeniority
import org.dhamma.dipi.staff.model.SeatKind
import org.dhamma.dipi.staff.model.TeacherRoll
import org.dhamma.dipi.staff.model.backrestSeatLabel
import org.dhamma.dipi.staff.teacher.TeacherListScreen
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Spec 2b S3 — `TeacherListScreen` (frame 2b) on the tablet window:
 * groups and rows in EXACT given order (positional, never re-sorted),
 * group-pill filtering, 52dp rows, verbatim band text, no chip for an
 * empty course history, em-dash folds, and the next-group footer peek.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1240dp-h844dp-land")
class TeacherListScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun row(
        sn: Int,
        name: String,
        roleTag: String? = null,
        room: String = "Mbk-1",
        age: String = "40",
        city: String = "Pune",
        courses: List<Pair<String, Int>> = emptyList(),
        seat: String = "",
        seatKind: SeatKind = SeatKind.FLOOR,
        backrest: Boolean = false,
        occupation: String = "Engineer",
        education: String = "B.E.",
        languages: String = "Hindi",
    ) = RollRow(
        sn = sn, name = name, roleTag = roleTag, room = room, age = age, city = city,
        courses = courses, cell = "", seat = seat, seatKind = seatKind, backrest = backrest,
        occupation = occupation, education = education, languages = languages,
    )

    // Deliberately "shuffled-looking": Female before Male (the server's own
    // order is M then F) and Z-names before A-names — any re-sort by gender,
    // group, name, age or seat would reorder something below.
    private val femaleOld = RollGroup(
        at = "Uma Rangan", code = "URN", gender = Gender.F,
        seniority = RollSeniority.OLD, group = "2", total = 2,
        rows = listOf(
            row(
                1, "Zara Bhosale", courses = listOf("10D" to 4, "STP" to 1),
                seat = "CW-B1", seatKind = SeatKind.CELL, age = "62",
                occupation = "Doctor", education = "MBBS", languages = "Marathi",
            ),
            row(2, "Asha Menon", courses = emptyList(), occupation = "—", education = "—", languages = "Gujarati", age = "19"),
        ),
    )
    private val maleNew = RollGroup(
        at = "(unassigned)", code = null, gender = Gender.M,
        seniority = RollSeniority.NEW, group = "1", total = 1,
        rows = listOf(
            row(1, "Rakesh Iyer", seat = "CH-12", seatKind = SeatKind.CHAIR, backrest = true, age = "28"),
        ),
    )
    private val roll = TeacherRoll(listOf(femaleOld, maleNew))

    @Test
    fun groupsAndRowsRenderInGivenOrderNeverSorted() {
        rule.setContent {
            DipiTheme { TeacherListScreen(roll = roll, courseLine = "Dhamma Sudha / 10 Day / 2026") }
        }
        // Positional: the female band was fed first, so it sits ABOVE the
        // male band even though every natural sort (gender, group number,
        // seniority) would flip them.
        val fBand = rule.onNodeWithTag("group-band-F-OLD-2").getUnclippedBoundsInRoot().top
        val mBand = rule.onNodeWithTag("group-band-M-NEW-1").getUnclippedBoundsInRoot().top
        assertTrue("female group was fed first and must render first", fBand < mBand)
        // Rows keep parse order too: Z before A.
        val zara = rule.onNodeWithText("Zara Bhosale").getUnclippedBoundsInRoot().top
        val asha = rule.onNodeWithText("Asha Menon").getUnclippedBoundsInRoot().top
        assertTrue("rows must keep parse order, not alphabetical", zara < asha)
    }

    @Test
    fun bandTextIsThePagesOwnVerbatim() {
        rule.setContent {
            DipiTheme { TeacherListScreen(roll = roll, courseLine = "Dhamma Sudha / 10 Day / 2026") }
        }
        rule.onNodeWithText("AT: Uma Rangan [URN]").assertIsDisplayed()
        rule.onNodeWithText("Female · Old · Group 2").assertIsDisplayed()
        rule.onNodeWithText("AT: (unassigned)").assertIsDisplayed()
        rule.onNodeWithText("Male · New · Group 1").assertIsDisplayed()
        rule.onNodeWithText("2 TOTAL").assertIsDisplayed()
    }

    @Test
    fun groupPillFiltersToOneGroupAndTapAgainClears() {
        var filter by mutableStateOf<String?>(null)
        rule.setContent {
            DipiTheme {
                TeacherListScreen(
                    roll = roll,
                    courseLine = "Dhamma Sudha / 10 Day / 2026",
                    groupFilter = filter,
                    onGroupFilter = { filter = it },
                )
            }
        }
        rule.onNodeWithText("Rakesh Iyer").assertIsDisplayed()
        rule.onNodeWithTag("group-pill-F-OLD-2").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Zara Bhosale").assertIsDisplayed()
        rule.onNodeWithText("Rakesh Iyer").assertDoesNotExist()
        rule.onNodeWithTag("group-band-M-NEW-1").assertDoesNotExist()
        // Tap again clears the filter.
        rule.onNodeWithTag("group-pill-F-OLD-2").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Rakesh Iyer").assertIsDisplayed()
    }

    @Test
    fun rowsAre52dpTall() {
        rule.setContent {
            DipiTheme { TeacherListScreen(roll = roll, courseLine = "Dhamma Sudha / 10 Day / 2026") }
        }
        val bounds = rule.onAllNodesWithTag("roll-row").onFirst().getUnclippedBoundsInRoot()
        assertEquals(52f, (bounds.bottom - bounds.top).value, 0.5f)
    }

    @Test
    fun emptyCourseHistoryRendersNoChipAndFoldsUseEmDashes() {
        rule.setContent {
            DipiTheme { TeacherListScreen(roll = roll, courseLine = "Dhamma Sudha / 10 Day / 2026") }
        }
        // Zara has 10D+STP chips; Asha and Rakesh have empty histories —
        // exactly one chip per key on the whole screen proves the blank
        // cells rendered nothing.
        rule.onAllNodesWithTag("course-chip-10D", useUnmergedTree = true).assertCountEquals(1)
        rule.onAllNodesWithTag("course-chip-STP", useUnmergedTree = true).assertCountEquals(1)
        // Folded line keeps the page's own blanks as em-dashes.
        rule.onNodeWithText("- · - · Gujarati").assertIsDisplayed()
        rule.onNodeWithText("Doctor · MBBS · Marathi").assertIsDisplayed()
    }

    @Test
    fun footerPeeksTheNextGroupsBandText() {
        rule.setContent {
            DipiTheme { TeacherListScreen(roll = roll, courseLine = "Dhamma Sudha / 10 Day / 2026") }
        }
        // At the top of the list the current group is the female one, so the
        // footer peeks the male band text with its count.
        rule.onNodeWithTag("next-group-footer").assertIsDisplayed()
        rule.onNodeWithText("AT: (unassigned) · Male · New · Group 1").assertIsDisplayed()
        // "1 TOTAL" shows twice: the male group's own band AND the footer peek.
        rule.onAllNodesWithText("1 TOTAL").assertCountEquals(2)
    }

    @Test
    fun footerDisappearsWhenTheFilterLeavesOneGroup() {
        rule.setContent {
            DipiTheme {
                TeacherListScreen(
                    roll = roll,
                    courseLine = "Dhamma Sudha / 10 Day / 2026",
                    groupFilter = "F-OLD-2",
                )
            }
        }
        rule.onNodeWithTag("next-group-footer").assertDoesNotExist()
    }

    @Test
    fun offlineStripPushesContentDown() {
        rule.setContent {
            DipiTheme {
                TeacherListScreen(roll = roll, courseLine = "Dhamma Sudha / 10 Day / 2026", offline = true)
            }
        }
        rule.onNodeWithTag("offline-strip").assertIsDisplayed()
        rule.onNodeWithText("◍ Offline - showing cached list", substring = true).assertIsDisplayed()
        // Pushed down, not floated: the header title starts below the strip.
        val strip = rule.onNodeWithTag("offline-strip").getUnclippedBoundsInRoot()
        val title = rule.onNodeWithTag("teacher-list-title").getUnclippedBoundsInRoot()
        assertTrue("content must sit below the offline strip", title.top >= strip.bottom)
    }

    @Test
    fun rowTapAndFlagsAreDelegatedProps() {
        var opened: RollRow? = null
        rule.setContent {
            DipiTheme {
                TeacherListScreen(
                    roll = roll,
                    courseLine = "Dhamma Sudha / 10 Day / 2026",
                    flagsFor = { r -> if (r.name == "Zara Bhosale") listOf("HLTH", "MED") else emptyList() },
                    onOpen = { opened = it },
                )
            }
        }
        // FLAGS render whatever the state carries — nothing typed on-screen.
        rule.onNodeWithText("HLTH").assertIsDisplayed()
        rule.onNodeWithText("MED").assertIsDisplayed()
        rule.onNodeWithText("Zara Bhosale").performClick()
        assertEquals("Zara Bhosale", opened?.name)
    }

    @Test
    fun headerStatesRollCountAndClearFilterAppearsWhenFiltered() {
        var filter by mutableStateOf<String?>(null)
        rule.setContent {
            DipiTheme {
                TeacherListScreen(
                    roll = roll,
                    courseLine = "Dhamma Sudha / 10 Day / 2026",
                    groupFilter = filter,
                    onGroupFilter = { filter = it },
                )
            }
        }
        rule.onNodeWithText("Dhamma Sudha / 10 Day / 2026 · 3 on the roll").assertIsDisplayed()
        rule.onNodeWithTag("clear-filter").assertDoesNotExist()
        rule.onNodeWithTag("group-pill-F-OLD-2").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("clear-filter").assertIsDisplayed()
        rule.onNodeWithTag("clear-filter").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Rakesh Iyer").assertIsDisplayed()
    }

    @Test
    fun coursesColumnCollapsesWhenTheRenderedSetHasNoChips() {
        rule.setContent {
            DipiTheme {
                TeacherListScreen(roll = TeacherRoll(listOf(maleNew)), courseLine = "Sudha")
            }
        }
        rule.onNodeWithTag("courses-collapsed").assertIsDisplayed()
        rule.onNodeWithText("NO COURSE HISTORY IN THIS GROUP").assertIsDisplayed()
        rule.onNodeWithText("COURSES").assertDoesNotExist()
    }

    @Test
    fun filterEmptyBodyOffersTheOtherGroups() {
        val empty = RollGroup(
            at = "Kiran Arya", code = "KA3", gender = Gender.F,
            seniority = RollSeniority.OLD, group = "9", total = 0, rows = emptyList(),
        )
        val withEmpty = TeacherRoll(listOf(femaleOld, maleNew, empty))
        var filter by mutableStateOf<String?>("F-OLD-9")
        rule.setContent {
            DipiTheme {
                TeacherListScreen(
                    roll = withEmpty,
                    courseLine = "Sudha",
                    groupFilter = filter,
                    onGroupFilter = { filter = it },
                )
            }
        }
        rule.onNodeWithTag("filter-empty").assertIsDisplayed()
        rule.onNodeWithText("Nobody is in this group").assertIsDisplayed()
        rule.onNodeWithTag("filter-empty-clear").performClick()
        rule.waitForIdle()
        assertEquals(null, filter)
    }

    @Test
    fun pendingFlagsShowABarNotAnEmptyCell() {
        rule.setContent {
            DipiTheme {
                TeacherListScreen(
                    roll = roll,
                    courseLine = "Sudha",
                    flagsReady = { it.name == "Asha Menon" },
                    flagsFor = { if (it.name == "Asha Menon") listOf("MED") else emptyList() },
                )
            }
        }
        // Clickable rows merge descendants; the bar tag lives on the FLAGS cell.
        rule.onAllNodesWithTag("flags-pending", useUnmergedTree = true).onFirst().assertIsDisplayed()
        rule.onNodeWithText("MED").assertIsDisplayed()
    }

    @Test
    fun backrestRowsMarkTheSeatCellWithTheGlyph() {
        rule.setContent {
            DipiTheme { TeacherListScreen(roll = roll, courseLine = "Dhamma Sudha / 10 Day / 2026") }
        }
        // Rakesh Iyer carries backrest = true on CH-12: the seat cell shows
        // the shared glyphed label (assert through the constant, never a
        // hard-coded glyph literal — the tofu fallback is a one-line swap).
        rule.onNodeWithText(backrestSeatLabel("CH-12", true)).assertIsDisplayed()
        // Zara Bhosale's CW-B1 has no backrest — plain seat, no glyph.
        rule.onNodeWithText("CW-B1").assertIsDisplayed()
        rule.onNodeWithText(backrestSeatLabel("CW-B1", true)).assertDoesNotExist()
    }
}
