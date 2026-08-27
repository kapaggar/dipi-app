package org.dhamma.dipi.staff.course

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.RoomLayout
import org.dhamma.dipi.staff.ui.theme.DeskStyle
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import org.dhamma.dipi.staff.ui.theme.deskCard

/**
 * Room chart — rooms are read-only; the grid shape per block is adjustable.
 * Numbered cells in a grid, the amenity mark (G geyser · IC Indian commode ·
 * W Western toilet) under the number. The list itself comes from the desk
 * site's centre config; the app never adds or deletes rooms. When opened
 * from Zero Day the tap still assigns the room to the applicant.
 */
@Composable
fun RoomsScreen(
    rooms: List<AccoRoom>,
    genderFilter: Gender? = null,
    layout: RoomLayout = RoomLayout(),
    onColumns: (Gender, String, Int) -> Unit = { _, _, _ -> },
    onPick: (AccoRoom) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val c = LocalDipi.current
    val shown = rooms.filter { genderFilter == null || it.gender == genderFilter }
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Room chart", fontFamily = DipiCondensed, fontSize = 22.sp, color = c.foreground)
        TextButton(onClick = onBack) { Text("Back") }
        if (shown.isEmpty()) {
            Text(
                "No rooms for this filter. The room list comes from the desk site.",
                color = c.muted,
                modifier = Modifier.padding(top = 12.dp),
            )
            return@Column
        }
        listOf(Gender.F to "Female", Gender.M to "Male").forEach { (g, label) ->
            val block = shown.filter { it.gender == g }
            if (block.isEmpty()) return@forEach
            block.groupBy { it.section }.forEach { (section, sectionRooms) ->
                val columns = layout.columnsFor(g, section)
                val rowCount = RoomLayout.rowsFor(sectionRooms.size, columns)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        listOf(label, section).filter { it.isNotBlank() }.joinToString(" · ") +
                            " · ${sectionRooms.size} rooms · $columns per row · $rowCount rows",
                        fontFamily = DipiCondensed,
                        fontSize = 16.sp,
                        color = c.foreground,
                        modifier = Modifier.weight(1f),
                    )
                    ColumnStepper(
                        humanLabel = label,
                        section = section,
                        columns = columns,
                        onDecrement = { onColumns(g, section, columns - 1) },
                        onIncrement = { onColumns(g, section, columns + 1) },
                    )
                }
                sectionRooms.chunked(columns).forEachIndexed { i, rowRooms ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(DeskStyle.tileShape)
                            .background(if (i % 2 == 1) c.tint else androidx.compose.ui.graphics.Color.Transparent),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        rowRooms.forEach { room ->
                            Column(
                                Modifier
                                    .weight(1f)
                                    .deskCard(shape = DeskStyle.tileShape, fill = c.field, border = c.hairline)
                                    .clickable { onPick(room) }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    room.displayNo,
                                    fontFamily = DipiCondensed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = c.foreground,
                                )
                                Text(
                                    room.amenityMark.ifBlank { " " },
                                    fontFamily = DipiMono,
                                    fontSize = 10.sp,
                                    color = c.muted,
                                )
                            }
                        }
                        repeat(columns - rowRooms.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

/**
 * Per-block column stepper on the header's trailing edge: `− {C} +`. Rows are
 * derived (spec S4) so there is no separate row control — the row count
 * lives in the block header text instead.
 */
@Composable
private fun ColumnStepper(
    humanLabel: String,
    section: String,
    columns: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    val c = LocalDipi.current
    val label = listOf(humanLabel, section).filter { it.isNotBlank() }.joinToString(" ")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        StepperButton(
            symbol = "−",
            enabled = columns > RoomLayout.MIN_COLUMNS,
            contentDescription = "Decrease columns · $label",
            onClick = onDecrement,
        )
        Text(
            "$columns",
            fontFamily = DipiMono,
            fontSize = 14.sp,
            color = c.foreground,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        StepperButton(
            symbol = "+",
            enabled = columns < RoomLayout.MAX_COLUMNS,
            contentDescription = "Increase columns · $label",
            onClick = onIncrement,
        )
    }
}

/** A single stepper tap target, always at least 48dp regardless of enabled state. */
@Composable
private fun StepperButton(
    symbol: String,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val c = LocalDipi.current
    Box(
        Modifier
            .size(48.dp)
            .clip(DeskStyle.controlShape)
            .background(if (enabled) c.field else androidx.compose.ui.graphics.Color.Transparent)
            .border(1.dp, c.hairline, DeskStyle.controlShape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol,
            fontFamily = DipiCondensed,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = if (enabled) c.foreground else c.muted,
        )
    }
}
