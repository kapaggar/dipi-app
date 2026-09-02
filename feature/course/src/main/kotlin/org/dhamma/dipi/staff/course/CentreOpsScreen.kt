package org.dhamma.dipi.staff.course

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import org.dhamma.dipi.staff.model.CentreOpsPrefs
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.HallGrid
import org.dhamma.dipi.staff.model.WHATSAPP_DEFAULT_TEMPLATE
import org.dhamma.dipi.staff.model.WHATSAPP_TOKENS
import org.dhamma.dipi.staff.model.centreOpsEffect
import org.dhamma.dipi.staff.model.whatsAppMessage
import org.dhamma.dipi.staff.ui.theme.DeskKicker
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.Industry
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import org.dhamma.dipi.staff.ui.theme.deskCard

@Composable
fun CentreOpsScreen(
    prefs: CentreOpsPrefs,
    onToggleLaundry: () -> Unit,
    onToggleValuables: () -> Unit,
    onToggleGroups: () -> Unit,
    onOpenRooms: () -> Unit,
    onBack: () -> Unit,
    onWhatsAppTemplate: (String) -> Unit = {},
    onHallGrid: (Gender, HallGrid) -> Unit = { _, _ -> },
) {
    val c = LocalDipi.current
    val grouped = prefs.rooms.groupBy { it.gender to it.section }
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Centre settings", fontFamily = DipiCondensed, fontSize = 22.sp, color = c.foreground)
        TextButton(onClick = onBack) { Text("Back") }
        Row(
            Modifier
                .fillMaxWidth()
                .deskCard()
                .clickable(onClick = onOpenRooms)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Room chart", fontFamily = DipiCondensed, fontSize = 18.sp, color = c.foreground)
                Text(
                    "Rooms, sections and chart layout",
                    color = c.muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Three switches change what check-in asks for. " +
                "The line at the bottom shows the result.",
            color = c.muted,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp),
        )
        ToggleRow(
            title = "Laundry",
            note = "Check-in asks whether laundry was issued.",
            on = prefs.laundry,
            onClick = onToggleLaundry,
            testTag = "toggle-laundry",
        )
        ToggleRow(
            title = "Valuables",
            note = "Check-in asks whether valuables were deposited.",
            on = prefs.valuables,
            onClick = onToggleValuables,
            testTag = "toggle-valuables",
        )
        ToggleRow(
            title = "Groups",
            note = "Check-in assigns a sitting group; Zero Day shows group chips.",
            on = prefs.groups,
            onClick = onToggleGroups,
            testTag = "toggle-groups",
        )
        Spacer(Modifier.height(14.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .deskCard()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DeskKicker("RESULT", Industry.neutral500)
            Text(centreOpsEffect(prefs), color = c.foreground, fontSize = 13.sp)
        }
        Spacer(Modifier.height(14.dp))
        // Below the RESULT card so the check-in switches stay above the fold
        // on the phone window; the Room chart card above stays the way in for
        // room-grid shape, this card owns the seating plan's hall grid.
        HallChartCard(
            male = prefs.hallGridFor(Gender.M),
            female = prefs.hallGridFor(Gender.F),
            onHallGrid = onHallGrid,
        )
        Spacer(Modifier.height(14.dp))
        WhatsAppTemplateCard(prefs.whatsAppTemplate, onWhatsAppTemplate)
        Spacer(Modifier.height(14.dp))
        Text("Accommodation", fontFamily = DipiCondensed, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
        Text(
            "Room list comes from the desk site (Centre → Edit) and refreshes on sign-in.",
            color = c.muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        if (grouped.isEmpty()) {
            Text("No rooms configured yet.", color = c.muted, modifier = Modifier.padding(vertical = 8.dp))
        }
        grouped.forEach { (key, rooms) ->
            val (g, sec) = key
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(g.name, modifier = Modifier.weight(0.6f), color = c.foreground)
                Text(sec, modifier = Modifier.weight(1f), color = c.foreground)
                Text(
                    "${rooms.size} rooms",
                    modifier = Modifier.weight(1f),
                    color = c.foreground,
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, note: String, on: Boolean, onClick: () -> Unit, testTag: String) {
    val c = LocalDipi.current
    Row(
        Modifier
            .fillMaxWidth()
            // The row is the single toggle target: it carries the click AND the
            // On/Off semantics (Role.Switch), so a tap anywhere on the row fires
            // onClick exactly once. The Switch below is display-only
            // (onCheckedChange = null) so it doesn't install a second, competing
            // toggleable — that would double-fire on a thumb tap.
            .toggleable(value = on, onValueChange = { onClick() }, role = Role.Switch)
            .testTag(testTag)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = c.foreground, fontSize = 15.sp)
            Text(note, color = c.muted, fontSize = 12.sp)
        }
        Switch(
            checked = on,
            onCheckedChange = null,
            modifier = Modifier.padding(start = 12.dp),
            colors = SwitchDefaults.colors(
                uncheckedThumbColor = c.muted,
                uncheckedTrackColor = c.field,
                uncheckedBorderColor = c.hairlineStrong,
            ),
        )
    }
}

/**
 * The calling round's WhatsApp wording. Blank means the built-in default, so
 * the field opens showing what will actually be sent rather than an empty box.
 * The preview renders the tokens against a sample applicant — the real message
 * is built the same way at hand-off time and never stored.
 */
@Composable
private fun WhatsAppTemplateCard(template: String, onTemplate: (String) -> Unit) {
    val c = LocalDipi.current
    Column(
        Modifier
            .fillMaxWidth()
            .deskCard()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("WhatsApp message", fontFamily = DipiCondensed, fontSize = 18.sp, color = c.foreground)
        Text(
            "Sent by the calling round's WhatsApp button. " +
                WHATSAPP_TOKENS.joinToString(" ") + " are filled in per applicant.",
            color = c.muted,
            fontSize = 12.sp,
        )
        OutlinedTextField(
            value = template,
            onValueChange = onTemplate,
            modifier = Modifier.fillMaxWidth().testTag("whatsapp-template"),
            minLines = 3,
            placeholder = { Text(WHATSAPP_DEFAULT_TEMPLATE, fontSize = 13.sp) },
        )
        Text("PREVIEW", color = c.muted, fontSize = 11.sp)
        Text(
            whatsAppMessage(
                template = template,
                name = "Rajat Kumar",
                course = "10 Day",
                dates = "2 Sep - 13 Sep",
                centre = "Dhamma Sudha",
                conf = "NM66",
            ),
            color = c.foreground,
            fontSize = 13.sp,
        )
        if (template.isNotBlank()) {
            TextButton(onClick = { onTemplate("") }) { Text("Reset to the default message") }
        }
    }
}

/**
 * Hall chart (spec 2c S1) — the seating plan's grid shape per gender, beside
 * the Room chart. Same stage-then-SAVE flow as the room chart: stepper taps
 * mutate `staged` (a live preview of the header line), nothing reaches
 * [onHallGrid] until SAVE; `committed` re-syncs from the incoming props on a
 * real external change (Erase-all, fresh DataStore read) and locally right
 * after a save so the button disables immediately. Desk-side config — the
 * teacher never edits it; wiped with the rest of centre_ops by Erase-all.
 */
@Composable
private fun HallChartCard(
    male: HallGrid,
    female: HallGrid,
    onHallGrid: (Gender, HallGrid) -> Unit,
) {
    val c = LocalDipi.current
    var staged by remember(male, female) { mutableStateOf(mapOf(Gender.M to male, Gender.F to female)) }
    var committed by remember(male, female) { mutableStateOf(mapOf(Gender.M to male, Gender.F to female)) }
    val dirty = staged != committed
    Column(
        Modifier
            .fillMaxWidth()
            .deskCard()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Hall chart", fontFamily = DipiCondensed, fontSize = 18.sp, color = c.foreground)
        Text(
            "Rows and seats per row for the seating plan, per hall. " +
                "Seat labels beyond the grid extend it.",
            color = c.muted,
            fontSize = 12.sp,
        )
        listOf(Gender.M to "Male hall", Gender.F to "Female hall").forEach { (g, label) ->
            val grid = staged.getValue(g)
            Text(
                "$label · ${grid.rows} rows · ${grid.seatsPerRow} per row",
                fontFamily = DipiCondensed,
                fontSize = 16.sp,
                color = c.foreground,
                modifier = Modifier.padding(top = 8.dp),
            )
            HallStepperRow(
                label = "Rows",
                value = grid.rows,
                min = HallGrid.MIN_ROWS,
                max = HallGrid.MAX_ROWS,
                contentLabel = "rows · $label",
            ) { n -> staged = staged + (g to grid.copy(rows = n)) }
            HallStepperRow(
                label = "Seats per row",
                value = grid.seatsPerRow,
                min = HallGrid.MIN_SEATS_PER_ROW,
                max = HallGrid.MAX_SEATS_PER_ROW,
                contentLabel = "seats per row · $label",
            ) { n -> staged = staged + (g to grid.copy(seatsPerRow = n)) }
        }
        Button(
            onClick = {
                staged.forEach { (g, grid) -> if (grid != committed.getValue(g)) onHallGrid(g, grid) }
                committed = staged
            },
            enabled = dirty,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(48.dp),
            shape = org.dhamma.dipi.staff.ui.theme.DeskStyle.controlShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = c.accent,
                contentColor = Color.White,
                disabledContainerColor = c.hairline,
                disabledContentColor = c.muted,
            ),
        ) {
            Text("SAVE HALL LAYOUT", fontFamily = DipiCondensed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        if (dirty) {
            Text("Unsaved changes", color = c.muted, fontSize = 12.sp)
        }
    }
}

/** One `label … − {n} +` line of the hall chart, on the room stepper's own 48dp buttons. */
@Composable
private fun HallStepperRow(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    contentLabel: String,
    onValue: (Int) -> Unit,
) {
    val c = LocalDipi.current
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = c.muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        StepperButton(
            symbol = "−",
            enabled = value > min,
            contentDescription = "Decrease $contentLabel",
            onClick = { onValue(value - 1) },
        )
        Text(
            "$value",
            fontFamily = org.dhamma.dipi.staff.ui.theme.DipiMono,
            fontSize = 14.sp,
            color = c.foreground,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        StepperButton(
            symbol = "+",
            enabled = value < max,
            contentDescription = "Increase $contentLabel",
            onClick = { onValue(value + 1) },
        )
    }
}
