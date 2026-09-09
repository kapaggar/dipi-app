package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.dhamma.dipi.staff.desk.BoardPane
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.ConfNo
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class V7ResponsiveTest {
    @get:Rule val rule = createComposeRule()

    private val roll = listOf(
        ApplicantCard(
            id = ApplicantId(1),
            centreId = CentreId(1),
            courseId = CourseId(10),
            givenName = "Synthetic",
            familyName = "One",
            gender = Gender.F,
            status = ApplicantStatus("Confirmed"),
            type = ApplicantType.Student,
            oldStudent = false,
            attended = false,
            confNo = ConfNo("NF1"),
        ),
    )

    private fun board() {
        rule.setContent {
            DipiTheme {
                BoardPane(
                    roll = roll,
                    checkIns = emptyMap(),
                    flagged = emptyList(),
                    callOutcomes = emptyMap(),
                    onGoto = {},
                    onExport = {},
                )
            }
        }
    }

    @Test
    @Config(qualifiers = "w412dp-h900dp")
    fun narrowKeepsBoardActions() {
        board()
        rule.onNodeWithText("ON THE ROLL").assertIsDisplayed()
        rule.onNodeWithText("Day 0 list").assertExists()
    }

    @Test
    @Config(qualifiers = "w1280dp-h900dp")
    fun wideKeepsBoardActions() {
        board()
        rule.onNodeWithText("ON THE ROLL").assertIsDisplayed()
        rule.onNodeWithText("Course summary").assertExists()
    }
}
