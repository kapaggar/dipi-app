package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.dhamma.dipi.staff.course.AdvancedSearchScreen
import org.dhamma.dipi.staff.course.advancedSearchMatches
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.ConfNo
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AdvancedSearchScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun card(id: Int, given: String, family: String, conf: String?, courseId: Int = 10) =
        ApplicantCard(
            id = ApplicantId(id),
            centreId = CentreId(1),
            courseId = CourseId(courseId),
            givenName = given,
            familyName = family,
            gender = Gender.F,
            status = ApplicantStatus("Confirmed"),
            type = ApplicantType.Student,
            oldStudent = false,
            attended = false,
            confNo = conf?.let { ConfNo(it) },
        )

    private val rows = listOf(
        card(1, "Priya", "Nair", "NF128"),
        card(2, "Arun", "Kale", "OM42", courseId = 11),
        card(3, "Meera", "Deshpande", null),
    )

    @Test
    fun matchesByNameOrConfNumberCaseInsensitive() {
        assertEquals(emptyList<ApplicantCard>(), advancedSearchMatches(rows, ""))
        assertEquals(listOf(1), advancedSearchMatches(rows, "priya").map { it.id.value })
        assertEquals(listOf(2), advancedSearchMatches(rows, "om42").map { it.id.value })
        assertEquals(listOf(3), advancedSearchMatches(rows, "deshp").map { it.id.value })
        assertTrue(advancedSearchMatches(rows, "zzz").isEmpty())
    }

    @Test
    fun screenFiltersAsYouTypeAndOpensAResult() {
        var opened: ApplicantCard? = null
        rule.setContent {
            DipiTheme {
                AdvancedSearchScreen(rows = rows, onOpen = { opened = it })
            }
        }
        // Honest scope subtitle: the cached corpus, per loaded course.
        rule.onNodeWithText("Searches the 3 applicants cached on this device (2 courses loaded so far).")
            .assertIsDisplayed()
        rule.onNodeWithText("Type a name or a conf number to search.").assertIsDisplayed()

        rule.onNodeWithContentDescription("Search name or conf number").performTextInput("nair")
        rule.onNodeWithText("Priya Nair").assertIsDisplayed()
        rule.onAllNodesWithText("Arun Kale").assertCountEquals(0)

        rule.onNodeWithText("Priya Nair").performClick()
        assertEquals(1, opened?.id?.value)
    }

    @Test
    fun deskSearchButtonAndFromDeskLabel() {
        var asked: String? = null
        rule.setContent {
            DipiTheme {
                AdvancedSearchScreen(
                    rows = emptyList(),
                    onOpen = {},
                    onSearchDesk = { q, _ -> asked = q },
                    deskRows = listOf(card(9, "From", "Desk", "NF9")),
                )
            }
        }
        rule.onNodeWithText("Nothing cached yet — open a course once to search its applicants here.")
            .assertIsDisplayed()
        rule.onNodeWithContentDescription("Search name or conf number").performTextInput("Meera")
        rule.onNodeWithText("Search the desk").performClick()
        assertEquals("Meera", asked)
        rule.onNodeWithText("from desk · 1").assertIsDisplayed()
        rule.onNodeWithText("From Desk").assertIsDisplayed()
    }
}
