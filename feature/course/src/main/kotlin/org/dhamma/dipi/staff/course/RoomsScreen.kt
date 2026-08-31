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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val blocks = listOf(Gender.F to "Female", Gender.M to "Male").flatMap { (g, label) ->
        shown.filter { it.gender == g }.groupBy { it.section }
            .map { (section, sectionRooms) -> RoomBlock(g, label, section, sectionRooms) }
    }

    // Stepper taps are a live preview only — they mutate `staged`, which the grid
    // reads, so the reflow is instant. Nothing reaches `onColumns` (persistence)
    // until Save. `committed` is the last known-persisted layout: it re-syncs from
    // the incoming `layout` prop (a real external change — e.g. Erase-all or a
    // fresh DataStore read) via the `remember(layout)` key, and also updates
    // locally right after a Save so the button disables immediately rather than
    // waiting on the DataStore round-trip to feed a new `layout` back in.
    var staged by remember(layout) { mutableStateOf(layout) }
    var committed by remember(layout) { mutableStateOf(layout) }
    val dirty = blocks.any { staged.columnsFor(it.gender, it.section) != committed.columnsFor(it.gender, it.section) }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Room chart", fontFamily = DipiCondensed, fontSize = 22.sp, color = c.foreground)
        Button(
            onClick = {
                blocks.forEach { block ->
                    val n = staged.columnsFor(block.gender, block.section)
                    if (n != committed.columnsFor(block.gender, block.section)) {
                        onColumns(block.gender, block.section, n)
                    }
                }
                committed = staged
            },
            enabled = dirty,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(48.dp),
            shape = DeskStyle.controlShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = c.accent,
                contentColor = androidx.compose.ui.graphics.Color.White,
                disabledContainerColor = c.hairline,
                disabledContentColor = c.muted,
            ),
        ) {
            Text("SAVE ROOM LAYOUT", fontFamily = DipiCondensed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        if (dirty) {
            Text(
                "Unsaved changes",
                color = c.muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        TextButton(onClick = onBack) { Text("Back") }
        if (shown.isEmpty()) {
            Text(
                "No rooms for this filter. The room list comes from the desk site.",
                color = c.muted,
                modifier = Modifier.padding(top = 12.dp),
            )
            return@Column
        }
        blocks.forEach { blockInfo ->
            val (g, label, section, sectionRooms) = blockInfo
            val columns = staged.columnsFor(g, section)
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
                    onDecrement = { staged = staged.withColumns(g, section, columns - 1) },
                    onIncrement = { staged = staged.withColumns(g, section, columns + 1) },
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

/** One gender+section block of the chart — the layout's own key granularity. */
private data class RoomBlock(
    val gender: Gender,
    val label: String,
    val section: String,
    val rooms: List<AccoRoom>,
)

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
