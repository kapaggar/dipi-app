package org.dhamma.dipi.staff

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import org.dhamma.dipi.staff.desk.RoomsPane
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.CentreHallSettings
import org.dhamma.dipi.staff.model.RoomLayout
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

/**
 * S2/S3 of the 08-30 room-layout-reach spec (docs/DECISIONS.md, Room layout & sync): `RoomsPane` must
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
        rule.onNodeWithText("0 occupied · 14 free of 14").assertIsDisplayed()
        // Positional proof, not just presence: at 7 columns, room 1 and room 7
        // sit in the same row (equal top) while room 8 has wrapped to row two
        // (a different top). Scroll to the last node used in the comparison
        // first, so every bounds read below comes from the same settled
        // scroll position — a bare presence/scroll-reachability check here
        // would pass unchanged against a reverted chunked(4).
        rule.onNodeWithText("Mbk 08").performScrollTo()
        val top1 = rule.onNodeWithText("Mbk 01").getUnclippedBoundsInRoot().top
        val top7 = rule.onNodeWithText("Mbk 07").getUnclippedBoundsInRoot().top
        val top8 = rule.onNodeWithText("Mbk 08").getUnclippedBoundsInRoot().top
        assertSameRow(top1, top7)
        assertDifferentRow(top1, top8)
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
        // Same positional technique at the fallback column count (4): rooms
        // 1 and 4 share a row, room 5 has wrapped to the next one.
        rule.onNodeWithText("Fbk 05").performScrollTo()
        val top1 = rule.onNodeWithText("Fbk 01").getUnclippedBoundsInRoot().top
        val top4 = rule.onNodeWithText("Fbk 04").getUnclippedBoundsInRoot().top
        val top5 = rule.onNodeWithText("Fbk 05").getUnclippedBoundsInRoot().top
        assertSameRow(top1, top4)
        assertDifferentRow(top1, top5)
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

    /* ── v5 T5 · Rooms visual rebalance ───────────────────────────────── */

    /**
     * The `( View )` remnant is stripped in `SearchPageParser.mapRow`, so by
     * the time a name reaches this pane it is clean. Pinned here as well as
     * in the parser test because the pane is where the registrar saw it.
     */
    @Test
    fun noViewRemnantRendersAnywhere() {
        rule.setContent {
            DipiTheme {
                RoomsPane(
                    roll = listOf(occupantCard(1, "Meera", "Deshpande")),
                    checkIns = mapOf(ApplicantId(1) to CheckInRecord(checkedIn = true, room = "Mbk 01")),
                    rooms = listOf(AccoRoom("Mbk 01", Gender.M, "Mbk"), AccoRoom("Mbk 02", Gender.M, "Mbk")),
                )
            }
        }
        rule.onNodeWithText("( View )", substring = true).assertDoesNotExist()
        rule.onNodeWithText("View", substring = true).assertDoesNotExist()
    }

    /** Emptiness reads as absence of ink: no word, no accent, no occupant line. */
    @Test
    fun freeCellCarriesNoWordAndNoAccent() {
        rule.setContent {
            DipiTheme {
                RoomsPane(
                    roll = emptyList(),
                    checkIns = emptyMap(),
                    rooms = listOf(AccoRoom("Mbk 01", Gender.M, "Mbk")),
                )
            }
        }
        rule.onNodeWithText("free", substring = false).assertDoesNotExist()
        rule.onAllNodesWithTag("room-cell-free").assertCountEquals(1)
        rule.onAllNodesWithTag("room-cell-occupied").assertCountEquals(0)
    }

    /** The ratio the registrar opened the pane for, at Board-stat weight. */
    @Test
    fun blockHeaderShowsTheOccupiedRatioAt21sp() {
        rule.setContent {
            DipiTheme {
                RoomsPane(
                    roll = listOf(occupantCard(1, "Meera", "Deshpande")),
                    checkIns = mapOf(ApplicantId(1) to CheckInRecord(checkedIn = true, room = "Mbk 01")),
                    rooms = (1..3).map { AccoRoom("Mbk %02d".format(it), Gender.M, "Mbk") },
                )
            }
        }
        rule.onNodeWithText("1 occupied · 2 free of 3").assertIsDisplayed()
        rule.onNodeWithTag("room-occupancy-bar").assertExists()
    }

    @Test
    fun amenityLegendSitsInThePaneHeader() {
        rule.setContent {
            DipiTheme {
                RoomsPane(
                    roll = emptyList(),
                    checkIns = emptyMap(),
                    rooms = listOf(AccoRoom("Mbk 01", Gender.M, "Mbk")),
                )
            }
        }
        rule.onNodeWithTag("room-amenity-legend").assertIsDisplayed()
        rule.onNodeWithText("geyser").assertIsDisplayed()
        rule.onNodeWithText("Indian toilet").assertIsDisplayed()
        rule.onNodeWithText("western").assertIsDisplayed()
    }

    /** v5 adds no write protocol: a room cell is never clickable. */
    @Test
    fun noWriteAffordanceExists() {
        rule.setContent {
            DipiTheme {
                RoomsPane(
                    roll = listOf(occupantCard(1, "Meera", "Deshpande")),
                    checkIns = mapOf(ApplicantId(1) to CheckInRecord(checkedIn = true, room = "Mbk 01")),
                    rooms = (1..3).map { AccoRoom("Mbk %02d".format(it), Gender.M, "Mbk") },
                )
            }
        }
        listOf("room-cell-free", "room-cell-occupied").forEach { tag ->
            rule.onAllNodesWithTag(tag).fetchSemanticsNodes().forEach { node ->
                assertTrue(
                    "$tag must carry no click action",
                    node.config.getOrNull(SemanticsActions.OnClick) == null,
                )
            }
        }
    }

    @Test
    fun syncButtonIsHiddenAtZero() {
        rule.setContent {
            DipiTheme {
                RoomsPane(
                    roll = emptyList(),
                    checkIns = emptyMap(),
                    rooms = listOf(AccoRoom("Mbk 01", Gender.M, "Mbk")),
                    pendingSync = 0,
                )
            }
        }
        rule.onNodeWithText("SYNC", substring = true).assertDoesNotExist()
        rule.onNodeWithText("PULL FROM SERVER").assertIsDisplayed()
    }

    private fun occupantCard(id: Int, given: String, family: String) = ApplicantCard(
        id = ApplicantId(id),
        centreId = CentreId(1),
        courseId = CourseId(10),
        givenName = given,
        familyName = family,
        gender = Gender.M,
        status = ApplicantStatus("Confirmed"),
        type = ApplicantType.Student,
        oldStudent = false,
        attended = false,
    )

    /** Same row: tops within rounding noise (sub-pixel/sub-dp), not literally equal. */
    @Test
    fun finalizedChartUsesHistoricalAssignmentAndShowsAgeAndSeniority() {
        val student = occupantCard(1, "Meera", "Deshpande").copy(
            courseFinalized = true, historicalRoom = "Mbk 01", age = 61, oldStudent = true,
            status = ApplicantStatus("Attended"),
        )
        rule.setContent {
            DipiTheme {
                RoomsPane(roll = listOf(student), checkIns = emptyMap(),
                    rooms = listOf(AccoRoom("Mbk 01", Gender.M, "Mbk")),
                    readOnly = true, pendingSync = 1,
                    onSyncRooms = { error("Finalized course cannot sync") })
            }
        }
        rule.onAllNodesWithText("61").assertCountEquals(1)
        rule.onNodeWithTag("room-cell-age").assertDoesNotExist()
        rule.onNodeWithTag("room-cell-age-top").assertIsDisplayed()
        rule.onNodeWithText("Age 61 · OLD").assertDoesNotExist()
        rule.onNodeWithText("Age 61").assertDoesNotExist()
        rule.onNodeWithText("OLD").assertDoesNotExist()
        rule.onNodeWithText("SYNC 1 TO SERVER").assertIsNotEnabled()
        rule.onAllNodesWithTag("room-cell-occupied").assertCountEquals(1)
        rule.onNode(hasTestTag("room-cell-occupied").and(hasContentDescription("Old student room")))
            .assertIsDisplayed()
    }

    @Test
    fun occupancyLegendSitsAboveTheGrid() {
        rule.setContent {
            DipiTheme {
                RoomsPane(
                    roll = emptyList(),
                    checkIns = emptyMap(),
                    rooms = listOf(AccoRoom("Mbk 01", Gender.M, "Mbk")),
                )
            }
        }
        rule.onNodeWithTag("room-chart-type-legend").assertIsDisplayed()
        rule.onNodeWithText("Old").assertIsDisplayed()
        rule.onNodeWithText("New").assertIsDisplayed()
        rule.onNodeWithText("Available").assertIsDisplayed()
    }

    @Test
    fun occupiedCellShowsAgeNumberWithoutLabelOrSeniorityText() {
        rule.setContent {
            DipiTheme {
                RoomsPane(
                    roll = listOf(occupantCard(1, "Rahul", "Kumar").copy(age = 28, oldStudent = false)),
                    checkIns = mapOf(ApplicantId(1) to CheckInRecord(checkedIn = true, room = "Mbk 01")),
                    rooms = listOf(AccoRoom("Mbk 01", Gender.M, "Mbk"), AccoRoom("Mbk 02", Gender.M, "Mbk")),
                )
            }
        }
        rule.onAllNodesWithText("28").assertCountEquals(1)
        rule.onNodeWithText("Age 28").assertDoesNotExist()
        rule.onNodeWithText("NEW").assertDoesNotExist()
        rule.onNode(hasTestTag("room-cell-occupied").and(hasContentDescription("New student room")))
            .assertIsDisplayed()
    }

    @Test
    fun ageSitsInTheReservedTopRightCorner() {
        rule.setContent {
            DipiTheme {
                RoomsPane(
                    roll = listOf(occupantCard(1, "Priyadarshini", "Kulkarniswamy").copy(age = 34, oldStudent = true)),
                    checkIns = mapOf(ApplicantId(1) to CheckInRecord(checkedIn = true, room = "Mbk 01")),
                    rooms = listOf(AccoRoom("Mbk 01", Gender.M, "Mbk")),
                )
            }
        }
        val name = rule.onNodeWithText("Priyadarshini Kulkarniswamy").getBoundsInRoot()
        val ageTop = rule.onNodeWithTag("room-cell-age-top").getBoundsInRoot()
        val cell = rule.onNodeWithTag("room-cell-occupied").getBoundsInRoot()
        rule.onNodeWithTag("room-cell-age").assertDoesNotExist()
        val noTopOverlap = name.right.value <= ageTop.left.value + 1f ||
            name.top.value >= ageTop.bottom.value - 1f
        assertTrue("name $name overlaps top age $ageTop", noTopOverlap)
        assertTrue("top age is right of cell centre", ageTop.left.value >= (cell.left.value + cell.right.value) / 2f)
        assertTrue("top age is above cell centre", ageTop.bottom.value <= (cell.top.value + cell.bottom.value) / 2f)
        assertTrue("top age stays inside the cell", ageTop.right.value <= cell.right.value + 1f)
        assertTrue("top age stays inside the cell", ageTop.top.value >= cell.top.value - 1f)
    }

    @Test
    fun occupiedAndFreeCellsShareTheRowHeight() {
        rule.setContent {
            DipiTheme {
                RoomsPane(
                    roll = listOf(occupantCard(1, "Meera", "Deshpande").copy(age = 61, oldStudent = true)),
                    checkIns = mapOf(ApplicantId(1) to CheckInRecord(checkedIn = true, room = "Mbk 01")),
                    rooms = listOf(AccoRoom("Mbk 01", Gender.M, "Mbk"), AccoRoom("Mbk 02", Gender.M, "Mbk")),
                    layout = RoomLayout().withColumns(Gender.M, "Mbk", 2),
                )
            }
        }
        val occupied = rule.onNodeWithTag("room-cell-occupied").getUnclippedBoundsInRoot()
        val free = rule.onNodeWithTag("room-cell-free").getUnclippedBoundsInRoot()
        assertSameRow(occupied.top, free.top)
        assertTrue(
            "same row height: ${occupied.height} vs ${free.height}",
            abs(occupied.height.value - free.height.value) < 1f,
        )
    }

    @Test
    fun aRowWithNoOccupantsIsShorterThanARowWithOne() {
        val rooms = (1..8).map { AccoRoom("Fbk %02d".format(it), Gender.F, "Fbk") }
        val layout = RoomLayout().withColumns(Gender.F, "Fbk", 4)
        rule.setContent {
            DipiTheme {
                RoomsPane(
                    roll = listOf(occupantCard(1, "Meera", "Deshpande").copy(age = 34)),
                    checkIns = mapOf(ApplicantId(1) to CheckInRecord(checkedIn = true, room = "Fbk 05")),
                    rooms = rooms,
                    layout = layout,
                )
            }
        }
        rule.onNodeWithText("Fbk 08").performScrollTo()
        val compact = rule.onAllNodesWithTag("room-cell-free")[0].getUnclippedBoundsInRoot()
        val occupied = rule.onNodeWithTag("room-cell-occupied").getUnclippedBoundsInRoot()
        val tallFree = rule.onAllNodesWithTag("room-cell-free")[4].getUnclippedBoundsInRoot()
        assertTrue(
            "empty row ${compact.height} should be shorter than occupied ${occupied.height}",
            compact.height.value + 8f < occupied.height.value,
        )
        assertTrue(
            "empty cell on an occupied row stays tall: ${tallFree.height} vs ${occupied.height}",
            abs(tallFree.height.value - occupied.height.value) < 1f,
        )
        assertTrue(
            "cell width stays the same: ${compact.width} vs ${occupied.width}",
            abs(compact.width.value - occupied.width.value) < 2f,
        )
    }

    @Test
    fun dashSpaceRoomLightsTheInventoryCell() {
        rule.setContent {
            DipiTheme {
                RoomsPane(
                    roll = listOf(occupantCard(1, "Rahul", "Kumar")),
                    checkIns = mapOf(ApplicantId(1) to CheckInRecord(checkedIn = false, room = "Mbk- 51")),
                    rooms = listOf(AccoRoom("Mbk 51", Gender.M, "Mbk", number = "51"), AccoRoom("Mbk 52", Gender.M, "Mbk", number = "52")),
                )
            }
        }
        rule.onNodeWithText("1 occupied · 1 free of 2").assertIsDisplayed()
        rule.onAllNodesWithTag("room-cell-occupied").assertCountEquals(1)
        rule.onNodeWithText("Rahul Kumar").assertIsDisplayed()
    }

    @Test
    fun leftStudentDoesNotLightARoom() {
        rule.setContent {
            DipiTheme {
                RoomsPane(
                    roll = listOf(occupantCard(1, "Rahul", "Kumar").copy(status = ApplicantStatus("Left"))),
                    checkIns = mapOf(ApplicantId(1) to CheckInRecord(checkedIn = true, room = "Mbk 51")),
                    rooms = listOf(AccoRoom("Mbk 51", Gender.M, "Mbk", number = "51")),
                )
            }
        }
        rule.onNodeWithText("0 occupied · 1 free of 1").assertIsDisplayed()
        rule.onAllNodesWithTag("room-cell-occupied").assertCountEquals(0)
        rule.onNodeWithText("Rahul Kumar").assertDoesNotExist()
    }

    @Test
    fun hallSeatsPerRowShapesAnUnsetBlock() {
        val rooms = (1..6).map { AccoRoom("Mbk %02d".format(it), Gender.M, "Mbk") }
        rule.setContent {
            DipiTheme {
                RoomsPane(
                    roll = emptyList(),
                    checkIns = emptyMap(),
                    rooms = rooms,
                    hallSettings = CentreHallSettings(maleSeatsPerRow = 5),
                )
            }
        }
        rule.onNodeWithText("Mbk 06").performScrollTo()
        val top1 = rule.onNodeWithText("Mbk 01").getUnclippedBoundsInRoot().top
        val top5 = rule.onNodeWithText("Mbk 05").getUnclippedBoundsInRoot().top
        val top6 = rule.onNodeWithText("Mbk 06").getUnclippedBoundsInRoot().top
        assertSameRow(top1, top5)
        assertDifferentRow(top1, top6)
    }

    @Test
    fun historicalRoomStillAppearsWhenRemovedFromCurrentInventory() {
        val student = occupantCard(1, "Meera", "Deshpande").copy(
            courseFinalized = true, historicalRoom = "OldBlock 12",
            status = ApplicantStatus("Attended"),
        )
        rule.setContent { DipiTheme { RoomsPane(roll = listOf(student),
            checkIns = emptyMap(), rooms = emptyList(), readOnly = true) } }
        rule.onNodeWithText("Meera Deshpande").assertIsDisplayed()
        rule.onAllNodesWithTag("room-cell-occupied").assertCountEquals(1)
    }

    private fun assertSameRow(a: Dp, b: Dp) {
        assertTrue("expected same row: $a vs $b", abs(a.value - b.value) < 1f)
    }

    /** Different row: a wrap moves a whole tile height (~54dp), far past rounding noise. */
    private fun assertDifferentRow(a: Dp, b: Dp) {
        assertTrue("expected different rows: $a vs $b", abs(a.value - b.value) > 10f)
    }
}
