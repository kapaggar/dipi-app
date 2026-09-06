package org.dhamma.dipi.staff

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsProperties
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicationCard
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.HealthRow
import org.dhamma.dipi.staff.model.RollGroup
import org.dhamma.dipi.staff.model.RollRow
import org.dhamma.dipi.staff.model.RollSeniority
import org.dhamma.dipi.staff.model.SeatKind
import org.dhamma.dipi.staff.model.TeacherRoll
import org.dhamma.dipi.staff.model.BACKREST_GLYPH
import org.dhamma.dipi.staff.model.backrestSeatLabel
import org.dhamma.dipi.staff.teacher.StudentCardScreen
import org.dhamma.dipi.staff.ui.TeacherCardRef
import org.dhamma.dipi.staff.ui.teacherCardStep
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Spec 2d S4 — `StudentCardScreen` (frame 2d): answers full-width and never
 * truncated, empty questions still shown with a `NO` tag, `N/A` pregnancy
 * for males, fixed left column vs scrolling right column, the disabled-end
 * ‹ › pair — plus the pure group-walk fn (order, ends, id-less rows).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1240dp-h844dp-land")
class StudentCardScreenTest {

    @get:Rule
    val rule = createComposeRule()

    // Multi-line on purpose: Robolectric's legacy text measurement never
    // soft-wraps, but hard breaks DO lay out as lines — so a maxLines or
    // ellipsis regression would surface as visual overflow / lost height.
    private val longAnswer =
        "Diagnosed with insulin-dependent diabetes in 2014; carries a glucometer and insulin pens.\n" +
            "Also has a lower-back injury from 2019 which makes sitting cross-legged for more than " +
            "forty minutes painful, and asks for a chair or backrest whenever the pain flares up.\n" +
            "Blood pressure is monitored weekly."

    private fun row(sn: Int = 1, name: String = "Suresh Nair", id: Int? = 4) = RollRow(
        sn = sn, applicantId = id?.let { ApplicantId(it) }, name = name, roleTag = null,
        room = "Mbk-8", age = "51", city = "Kochi", courses = listOf("10D" to 11),
        cell = "", seat = "CW-A3", seatKind = SeatKind.CELL, backrest = true,
        occupation = "Retired Teacher", education = "B.Ed", languages = "Malayalam",
    )

    private fun group(gender: Gender = Gender.M, rows: List<RollRow> = listOf(row())) = RollGroup(
        at = "Trainee-A-M Teacher", code = "TAM", gender = gender,
        seniority = RollSeniority.OLD, group = "1", total = rows.size, rows = rows,
    )

    private fun card(
        health: Map<String, String> = emptyMap(),
        conf: String? = "OM42",
    ) = ApplicationCard(
        name = "Suresh Nair",
        conf = conf,
        statusLine = "Confirmed · 10 Day",
        hasPhoto = false,
        personal = listOf(
            "Gender" to "Male", "Date of Birth" to "2 Feb 1975", "Age" to "51",
            "Nationality" to "Indian", "Old / New" to "Old", "Monk / Nun" to "No",
            "A-List" to "-", "Applied On" to "14 Jun 2026",
        ),
        historyCounts = ApplicationCard.HISTORY_ORDER.map { it to if (it == "10-Day") 11 else 0 },
        firstCourse = "2015-1-15, Dhamma sota sohna",
        lastCourse = "2025-12-12, Dhamma Sudha",
        practiceDetails = "1 hr daily",
        health = ApplicationCard.HEALTH_ORDER.map { HealthRow(it, health[it] ?: "-") },
    )

    @Test
    fun answerRendersFullWidthNeverTruncated() {
        rule.setContent {
            DipiTheme {
                StudentCardScreen(
                    row = row(), group = group(),
                    card = card(
                        mapOf(
                            "Physical" to longAnswer,
                            "Medication" to "None",
                        ),
                    ),
                )
            }
        }
        // The whole answer lays out: multi-line, no visual overflow — and the
        // unclipped bounds grow with the text (the short answer is one line).
        val body = rule.onNodeWithTag("answer-body-Physical", useUnmergedTree = true)
        body.assertIsDisplayed()
        val layout = ArrayList<androidx.compose.ui.text.TextLayoutResult>()
        body.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) {
            it(layout)
        }
        // maxLines/ellipsis regressions surface as a lower line count (hard
        // breaks always lay out; Robolectric never soft-wraps, and its width
        // measurement makes hasVisualOverflow meaningless here).
        assertEquals("all three lines must lay out", 3, layout.first().lineCount)
        assertFalse("no height truncation", layout.first().didOverflowHeight)
        val longH = body.getUnclippedBoundsInRoot().let { it.bottom - it.top }
        val shortH = rule.onNodeWithTag("answer-body-Medication", useUnmergedTree = true)
            .getUnclippedBoundsInRoot().let { it.bottom - it.top }
        assertTrue("bounds must grow with text ($longH vs $shortH)", longH > shortH)
        rule.onNodeWithText(longAnswer).assertIsDisplayed()
    }

    @Test
    fun emptyQuestionsStillShowWithNoTagAndNoBody() {
        rule.setContent {
            DipiTheme {
                StudentCardScreen(
                    row = row(), group = group(),
                    card = card(mapOf("Medication" to "Metformin 500mg twice daily")),
                )
            }
        }
        // All six labels render, in order, answered or not.
        ApplicationCard.HEALTH_ORDER.forEach { label ->
            rule.onNodeWithTag("answer-card-$label", useUnmergedTree = true).assertExists()
        }
        rule.onNodeWithText("Metformin 500mg twice daily").assertIsDisplayed()
        rule.onNodeWithText("YES").assertIsDisplayed()
        // Physical is `-`: label shown, no body node.
        rule.onNodeWithText("Physical").assertIsDisplayed()
        rule.onNodeWithTag("answer-body-Physical", useUnmergedTree = true).assertDoesNotExist()
        // Male: Pregnancy is N/A, the other four empties are NO.
        rule.onAllNodesWithText("NO").assertCountEquals(4)
        rule.onAllNodesWithText("N/A").assertCountEquals(1)
    }

    @Test
    fun pregnancyRendersNaTagForMalesAndYesForPregnantFemales() {
        rule.setContent {
            DipiTheme {
                StudentCardScreen(
                    row = row(), group = group(gender = Gender.F),
                    card = card(mapOf("Pregnancy" to "Yes - 4 (months)")),
                )
            }
        }
        rule.onNodeWithText("Yes - 4 (months)").assertIsDisplayed()
        rule.onAllNodesWithText("N/A").assertCountEquals(0)
    }

    @Test
    fun leftColumnIsFixedWhileTheRightColumnScrolls() {
        rule.setContent {
            DipiTheme {
                StudentCardScreen(row = row(), group = group(), card = card())
            }
        }
        rule.onNodeWithTag("card-answers")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
        rule.onNodeWithTag("card-left")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.VerticalScrollAxisRange))
        // Left column is the frame's fixed 404dp.
        val left = rule.onNodeWithTag("card-left").getUnclippedBoundsInRoot()
        assertEquals(404f, (left.right - left.left).value, 0.5f)
    }

    @Test
    fun walkButtonsDisableAtTheEnds() {
        var next = 0
        rule.setContent {
            DipiTheme {
                StudentCardScreen(
                    row = row(), group = group(), card = card(),
                    canPrev = false, canNext = true, onNext = { next++ },
                )
            }
        }
        rule.onNodeWithTag("card-prev").assertHasNoClickAction()
        rule.onNodeWithTag("card-next").assertHasClickAction()
        rule.onNodeWithTag("card-next").performClick()
        assertEquals(1, next)
    }

    @Test
    fun headerCarriesStatusChipAndPlacementLine() {
        rule.setContent {
            DipiTheme { StudentCardScreen(row = row(), group = group(), card = card()) }
        }
        rule.onNodeWithText("OLD · OM42").assertIsDisplayed()
        // 1.40.0: the fixture row carries backrest = true, so the seat
        // segment is glyphed through the shared backrestSeatLabel.
        rule.onNodeWithText(
            "Mbk-8 · seat ${backrestSeatLabel("CW-A3", true)} · Group 1 · TAM · 1 of 1 in this group",
        ).assertIsDisplayed()
        rule.onNodeWithText("Teacher list").assertIsDisplayed()
        rule.onNodeWithTag("answer-summary").assertIsDisplayed()
    }

    @Test
    fun namedBackAndCameFromFollowTheDoor() {
        rule.setContent {
            DipiTheme {
                StudentCardScreen(
                    row = row(),
                    group = group(),
                    card = card(),
                    backLabel = "Seating plan",
                    cameFrom = "Female hall · seat A1",
                )
            }
        }
        rule.onNodeWithText("Seating plan").assertIsDisplayed()
        rule.onNodeWithTag("card-came-from").assertIsDisplayed()
        rule.onNodeWithText("Female hall · seat A1").assertIsDisplayed()
    }

    @Test
    fun zeroHistoryTilesStayOnScreen() {
        rule.setContent {
            DipiTheme { StudentCardScreen(row = row(), group = group(), card = card()) }
        }
        ApplicationCard.HISTORY_ORDER.forEach { key ->
            rule.onNodeWithTag("history-tile-$key", useUnmergedTree = true).assertExists()
        }
    }

    @Test
    fun neverFetchedCardReadsHonestlyOffline() {
        rule.setContent {
            DipiTheme { StudentCardScreen(row = row(), group = group(), card = null, offline = true) }
        }
        rule.onNodeWithTag("card-not-cached").assertIsDisplayed()
        rule.onNodeWithText("Not cached — connect once to fetch").assertIsDisplayed()
    }

    // ---- the pure ‹ › walk (spec 2d S4)

    private val walkRoll = TeacherRoll(
        listOf(
            group(
                rows = listOf(
                    row(1, "Aman", id = 11),
                    row(2, "Binod", id = null), // unmapped: no card — skipped
                    row(3, "Chetan", id = 13),
                ),
            ),
            RollGroup(
                at = "(unassigned)", code = null, gender = Gender.M,
                seniority = RollSeniority.NEW, group = "1", total = 1,
                rows = listOf(row(1, "Dinesh", id = 14)),
            ),
        ),
    )

    @Test
    fun walkFollowsGroupOrderSkipsUnmappedRowsAndStopsAtEnds() {
        val key = walkRoll.groups.first().key
        val start = TeacherCardRef(key, 0)
        // Forward skips the id-less row and lands on index 2.
        assertEquals(TeacherCardRef(key, 2), teacherCardStep(walkRoll, start, 1))
        // The group's end never wraps into the next group.
        assertNull(teacherCardStep(walkRoll, TeacherCardRef(key, 2), 1))
        // Backward walks the same order in reverse and stops at the start.
        assertEquals(start, teacherCardStep(walkRoll, TeacherCardRef(key, 2), -1))
        assertNull(teacherCardStep(walkRoll, start, -1))
    }

    @Test
    fun placementLineMarksABackrestSeatWithTheGlyph() {
        // The fixture row carries backrest = true on CW-A3 — the placement
        // line seat segment goes through the one shared label fn.
        rule.setContent {
            DipiTheme { StudentCardScreen(row = row(), group = group(), card = card()) }
        }
        rule.onNodeWithText("seat ${backrestSeatLabel("CW-A3", true)}", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun placementLineLeavesANonBackrestSeatPlain() {
        val plain = row().copy(seat = "E1", seatKind = SeatKind.FLOOR, backrest = false)
        rule.setContent {
            DipiTheme {
                StudentCardScreen(row = plain, group = group(rows = listOf(plain)), card = card())
            }
        }
        rule.onNodeWithText("seat E1", substring = true).assertIsDisplayed()
        rule.onNodeWithText(BACKREST_GLYPH, substring = true).assertDoesNotExist()
    }
}
