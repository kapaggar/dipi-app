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
    fun plusButtonReflowsGridWithoutPersisting() {
        var callCount = 0
        rule.setContent {
            DipiTheme {
                RoomsScreen(
                    rooms = maleRooms(5),
                    onColumns = { _, _, _ -> callCount++ },
                )
            }
        }
        // Default is 4/row -> ceil(5/4) = 2 rows; a tap should reflow to 5/row -> 1 row.
        rule.onNodeWithText("Male · Mbk · 5 rooms · 4 per row · 2 rows").assertIsDisplayed()
        rule.onNodeWithContentDescription("Increase columns · Male Mbk").performClick()
        rule.onNodeWithText("Male · Mbk · 5 rooms · 5 per row · 1 rows").assertIsDisplayed()
        assertEquals(0, callCount)
    }

    @Test
    fun minusButtonReflowsGridWithoutPersisting() {
        var callCount = 0
        rule.setContent {
            DipiTheme {
                RoomsScreen(
                    rooms = maleRooms(5),
                    onColumns = { _, _, _ -> callCount++ },
                )
            }
        }
        rule.onNodeWithContentDescription("Decrease columns · Male Mbk").performClick()
        rule.onNodeWithText("Male · Mbk · 5 rooms · 3 per row · 2 rows").assertIsDisplayed()
        assertEquals(0, callCount)
    }

    @Test
    fun saveRoomLayoutPersistsOnlyChangedBlocksOnce() {
        val captured = mutableListOf<Triple<Gender, String, Int>>()
        rule.setContent {
            DipiTheme {
                RoomsScreen(
                    rooms = maleRooms(5),
                    onColumns = { g, section, n -> captured.add(Triple(g, section, n)) },
                )
            }
        }
        rule.onNodeWithContentDescription("Increase columns · Male Mbk").performClick()
        rule.onNodeWithContentDescription("Increase columns · Male Mbk").performClick()
        assertEquals(0, captured.size)
        rule.onNodeWithText("SAVE ROOM LAYOUT").performClick()
        assertEquals(listOf(Triple(Gender.M, "Mbk", 6)), captured)
    }

    @Test
    fun saveRoomLayoutDisabledWhenCleanEnabledWhenDirty() {
        rule.setContent {
            DipiTheme {
                RoomsScreen(rooms = maleRooms(5))
            }
        }
        rule.onNodeWithText("SAVE ROOM LAYOUT").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Increase columns · Male Mbk").performClick()
        rule.onNodeWithText("SAVE ROOM LAYOUT").assertIsEnabled()
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
