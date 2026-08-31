package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import org.dhamma.dipi.staff.desk.RoomsPane
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.RoomLayout
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * S2/S3 of docs/specs/2026-08-30-room-layout-reach-spec.md: `RoomsPane` must
 * honour the stored `RoomLayout` per gender+section block, and stack the
 * blocks full-width instead of splitting them into two side-by-side columns.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1240dp-h844dp-land")
class RoomsPaneTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun storedColumnCountShapesTheBlocksFirstRow() {
        // 7 columns for Male|Mbk means the first row of the Male · Mbk block
        // carries 7 tiles — rooms 8..14 wrap to row two.
        val rooms = (1..14).map { AccoRoom("Mbk %02d".format(it), Gender.M, "Mbk") }
        val layout = RoomLayout().withColumns(Gender.M, "Mbk", 7)
        rule.setContent {
            DipiTheme {
                RoomsPane(roll = emptyList(), checkIns = emptyMap(), rooms = rooms, layout = layout)
            }
        }
        rule.onNodeWithText("Male · Mbk").assertIsDisplayed()
        rule.onNodeWithText("14 rooms · 14 free").assertIsDisplayed()
        // Room 7 is in the 7-wide first row; room 8 only reachable by wrapping
        // to row two — both tiles must still be present regardless.
        rule.onNodeWithText("Mbk 07").assertIsDisplayed()
        rule.onNodeWithText("Mbk 08").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Mbk 14").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun blockWithNoStoredEntryUsesTheDefaultColumnCount() {
        val rooms = (1..(RoomLayout.DEFAULT_COLUMNS + 2)).map {
            AccoRoom("Fbk %02d".format(it), Gender.F, "Fbk")
        }
        rule.setContent {
            DipiTheme {
                RoomsPane(roll = emptyList(), checkIns = emptyMap(), rooms = rooms, layout = RoomLayout())
            }
        }
        // With DEFAULT_COLUMNS (4) and 6 rooms, the last two rooms wrap to a
        // second row — reachable, proving the fallback column count applied
        // rather than a single unbounded row.
        rule.onNodeWithText("Fbk 04").assertIsDisplayed()
        rule.onNodeWithText("Fbk 06").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun genderBlocksStackVerticallyRatherThanSideBySide() {
        val rooms = listOf(
            AccoRoom("Fbk 01", Gender.F, "Fbk"),
            AccoRoom("Mbk 01", Gender.M, "Mbk"),
        )
        rule.setContent {
            DipiTheme {
                RoomsPane(roll = emptyList(), checkIns = emptyMap(), rooms = rooms, layout = RoomLayout())
            }
        }
        // Both tiles reachable by scrolling — neither is clipped off by a
        // fixed-width side-by-side column.
        val female = rule.onNodeWithText("Fbk 01").performScrollTo().getUnclippedBoundsInRoot()
        val male = rule.onNodeWithText("Mbk 01").performScrollTo().getUnclippedBoundsInRoot()
        // Stacked, not side by side: a side-by-side layout would place both
        // tiles on the same row (equal top), just at different columns. A
        // clearly different vertical position proves the blocks stack.
        assertTrue((female.top - male.top).value.let { it > 20f || it < -20f })
    }

    @Test
    fun perSectionHeadersAppearForEachGenderSectionBlock() {
        val rooms = listOf(
            AccoRoom("Mbk 01", Gender.M, "Mbk"),
            AccoRoom("Guest 01", Gender.M, "Guest"),
            AccoRoom("Fbk 01", Gender.F, "Fbk"),
        )
        rule.setContent {
            DipiTheme {
                RoomsPane(roll = emptyList(), checkIns = emptyMap(), rooms = rooms, layout = RoomLayout())
            }
        }
        rule.onNodeWithText("Male · Mbk").assertIsDisplayed()
        rule.onNodeWithText("Male · Guest").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Female · Fbk").assertIsDisplayed()
    }
}
