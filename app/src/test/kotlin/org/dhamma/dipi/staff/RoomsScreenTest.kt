package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.AuditFlag
import org.dhamma.dipi.staff.model.AuditSeverity
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.CentreOpsPrefs
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.MAIN_DHAMMA_HALL
import org.dhamma.dipi.staff.model.RoomLayout
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

import org.dhamma.dipi.staff.course.RoomsScreen

@RunWith(RobolectricTestRunner::class)
class RoomsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun maleRooms(n: Int, section: String = "Mbk") =
        (1..n).map { AccoRoom("M$it", Gender.M, section) }

    @Test
    fun genderFilterShowsFemaleOnly() {
        rule.setContent {
            DipiTheme {
                RoomsScreen(
                    rooms = listOf(
                        AccoRoom("F32", Gender.F, "East"),
                        AccoRoom("M12", Gender.M, "West"),
                    ),
                    genderFilter = Gender.F,
                )
            }
        }
        rule.onNodeWithText("F32").assertIsDisplayed()
        rule.onNodeWithText("M12").assertDoesNotExist()
    }

    @Test
    fun blockWithNoStoredColumnsRendersDefaultFourPerRow() {
        rule.setContent {
            DipiTheme {
                RoomsScreen(rooms = maleRooms(5))
            }
        }
        // 5 rooms at 4/row -> ceil(5/4) = 2 rows.
        rule.onNodeWithText("Male · Mbk · 5 rooms · 4 per row · 2 rows").assertIsDisplayed()
    }

    @Test
    fun blockWithStoredColumnsRendersStoredValueAndDerivedRows() {
        val layout = RoomLayout().withColumns(Gender.M, "Mbk", 7)
        rule.setContent {
            DipiTheme {
                RoomsScreen(rooms = maleRooms(70), layout = layout)
            }
        }
        // 70 rooms at 7/row -> 10 rows.
        rule.onNodeWithText("Male · Mbk · 70 rooms · 7 per row · 10 rows").assertIsDisplayed()
    }

    @Test
    fun plusButtonCallsOnColumnsWithIncrementedValue() {
        var captured: Triple<Gender, String, Int>? = null
        rule.setContent {
            DipiTheme {
                RoomsScreen(
                    rooms = maleRooms(5),
                    onColumns = { g, section, n -> captured = Triple(g, section, n) },
                )
            }
        }
        rule.onNodeWithContentDescription("Increase columns · Male Mbk").performClick()
        assertEquals(Triple(Gender.M, "Mbk", 5), captured)
    }

    @Test
    fun minusButtonCallsOnColumnsWithDecrementedValue() {
        var captured: Triple<Gender, String, Int>? = null
        rule.setContent {
            DipiTheme {
                RoomsScreen(
                    rooms = maleRooms(5),
                    onColumns = { g, section, n -> captured = Triple(g, section, n) },
                )
            }
        }
        rule.onNodeWithContentDescription("Decrease columns · Male Mbk").performClick()
        assertEquals(Triple(Gender.M, "Mbk", 3), captured)
    }

    @Test
    fun minusButtonDisabledAtMinColumns() {
        val layout = RoomLayout().withColumns(Gender.M, "Mbk", RoomLayout.MIN_COLUMNS)
        rule.setContent {
            DipiTheme {
                RoomsScreen(rooms = maleRooms(5), layout = layout)
            }
        }
        rule.onNodeWithContentDescription("Decrease columns · Male Mbk").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Increase columns · Male Mbk").assertIsEnabled()
    }

    @Test
    fun plusButtonDisabledAtMaxColumns() {
        val layout = RoomLayout().withColumns(Gender.M, "Mbk", RoomLayout.MAX_COLUMNS)
        rule.setContent {
            DipiTheme {
                RoomsScreen(rooms = maleRooms(70), layout = layout)
            }
        }
        rule.onNodeWithContentDescription("Increase columns · Male Mbk").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Decrease columns · Male Mbk").assertIsEnabled()
    }
}
