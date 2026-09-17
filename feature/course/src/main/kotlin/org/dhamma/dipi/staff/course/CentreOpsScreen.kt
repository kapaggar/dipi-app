package org.dhamma.dipi.staff.course

import androidx.compose.foundation.background
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
import org.dhamma.dipi.staff.model.CentreHallSettings
import org.dhamma.dipi.staff.model.CentreOpsPrefs
import org.dhamma.dipi.staff.model.ChowkyRailLayout
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.HallGrid
import org.dhamma.dipi.staff.model.HallSeatPlan
import org.dhamma.dipi.staff.model.WHATSAPP_DEFAULT_TEMPLATE
import org.dhamma.dipi.staff.model.WHATSAPP_TOKENS
import org.dhamma.dipi.staff.model.centreOpsEffect
import org.dhamma.dipi.staff.model.whatsAppMessage
import org.dhamma.dipi.staff.ui.theme.DeskKicker
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import org.dhamma.dipi.staff.ui.theme.LocalIndustry
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
    automationContent: @Composable () -> Unit = {},
) {
    val c = LocalDipi.current
    val industry = LocalIndustry.current
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
            "Check-in options",
            color = c.muted,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp),
        )
        ToggleRow(
            title = "Laundry",
            note = "Laundry at check-in",
            on = prefs.laundry,
            onClick = onToggleLaundry,
            testTag = "toggle-laundry",
        )
        ToggleRow(
            title = "Valuables",
            note = "Valuables at check-in",
            on = prefs.valuables,
            onClick = onToggleValuables,
            testTag = "toggle-valuables",
        )
        ToggleRow(
            title = "Groups",
            note = "Sitting groups at check-in",
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
            DeskKicker("RESULT", industry.neutral500)
            Text(centreOpsEffect(prefs), color = c.foreground, fontSize = 13.sp)
        }
        Spacer(Modifier.height(14.dp))
        DeskHallSettingsCard(prefs.hallSettings)
        Spacer(Modifier.height(14.dp))
        // Local depth / rail only. Desk Main Plan columns and chowky win
        // when GET /centre/{cid}/edit sent them.
        HallChartCard(
            male = prefs.hallGridFor(Gender.M),
            female = prefs.hallGridFor(Gender.F),
            desk = prefs.hallSettings,
            onHallGrid = onHallGrid,
        )
        Spacer(Modifier.height(14.dp))
        WhatsAppTemplateCard(prefs.whatsAppTemplate, onWhatsAppTemplate)
        automationContent()
        Spacer(Modifier.height(14.dp))
        Text("Accommodation", fontFamily = DipiCondensed, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
        Text(
            "Edit rooms on the desk site. Refreshes when you open this page.",
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
            "Fields: " + WHATSAPP_TOKENS.joinToString(" "),
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
            TextButton(onClick = { onTemplate("") }) { Text("Reset message") }
        }
    }
}

/**
 * Live desk Hall Settings from `GET /centre/{cid}/edit`. Read-only; the
 * registrar changes these on the desk site. Refreshes with rooms on open.
 */
@Composable
private fun DeskHallSettingsCard(hall: CentreHallSettings) {
    val c = LocalDipi.current
    Column(
        Modifier
            .fillMaxWidth()
            .deskCard()
            .padding(14.dp)
            .testTag("desk-hall-settings"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Hall Settings", fontFamily = DipiCondensed, fontSize = 18.sp, color = c.foreground)
        Text(
            "From the desk site. Refreshes when you open this page. Read-only.",
            color = c.muted,
            fontSize = 12.sp,
        )
        if (!hall.isPresent()) {
            Text("No Hall Settings on the last fetch.", color = c.muted, fontSize = 13.sp)
            return
        }
        HallFact("Male/Female students in same hall", hall.combinedLabel())
        HallFact("Seat naming", hall.seatNamingLabel())
        Text(
            "Male (Main Plan)",
            fontFamily = DipiCondensed,
            fontSize = 16.sp,
            color = c.foreground,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(hall.malePlan.summaryLine("right"), color = c.foreground, fontSize = 13.sp)
        HallPlanPreview(hall.malePlan, naturalSide = "right")
        Text(
            "Female (Main Plan)",
            fontFamily = DipiCondensed,
            fontSize = 16.sp,
            color = c.foreground,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(hall.femalePlan.summaryLine("left"), color = c.foreground, fontSize = 13.sp)
        HallPlanPreview(hall.femalePlan, naturalSide = "left")
        Text(
            "FRONTROW = Teacher's Seat. Empty-seat clicks stay on the desk site.",
            color = c.muted,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun HallFact(label: String, value: String) {
    val c = LocalDipi.current
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = c.muted, fontSize = 12.sp)
        Text(value, color = c.foreground, fontSize = 14.sp)
    }
}

/** One-row Main Plan preview: cushion cells, then chowky on the desk side. */
@Composable
private fun HallPlanPreview(plan: HallSeatPlan, naturalSide: String) {
    val cols = plan.columns ?: return
    val cho = plan.chowkyColumns ?: 0
    val pos = plan.chowkyPosition?.lowercase().orEmpty().ifEmpty { naturalSide }
    val rtl = plan.direction?.lowercase() == "left" ||
        (plan.direction.isNullOrEmpty() && naturalSide == "left")
    val c = LocalDipi.current
    val chowkyLeft = pos == "left" || pos == "back"
    Row(
        Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (cho > 0 && chowkyLeft) {
            repeat(cho.coerceAtMost(8)) { PreviewSeat(c.accent.copy(alpha = 0.35f)) }
            Spacer(Modifier.size(4.dp))
        }
        val order = if (rtl) (cols downTo 1) else (1..cols)
        order.forEach { n ->
            PreviewSeat(c.field, if (n == 1) "1" else null)
        }
        if (cho > 0 && !chowkyLeft) {
            Spacer(Modifier.size(4.dp))
            repeat(cho.coerceAtMost(8)) { PreviewSeat(c.accent.copy(alpha = 0.35f)) }
        }
    }
    Text("▼ FRONT - Teacher's Seat", color = c.muted, fontSize = 11.sp)
}

@Composable
private fun PreviewSeat(color: Color, label: String? = null) {
    val c = LocalDipi.current
    Box(
        Modifier
            .size(18.dp)
            .background(color, org.dhamma.dipi.staff.ui.theme.DeskStyle.controlShape),
        contentAlignment = Alignment.Center,
    ) {
        if (label != null) {
            Text(label, color = c.muted, fontSize = 9.sp)
        }
    }
}

/**
 * Local seating depth and chowky rail. Desk Main Plan columns / chowky
 * from Hall Settings win when present; these steppers stay as the
 * override for fields the edit page omitted (depth always).
 */
@Composable
private fun HallChartCard(
    male: HallGrid,
    female: HallGrid,
    desk: CentreHallSettings = CentreHallSettings(),
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
        Text("Local hall override", fontFamily = DipiCondensed, fontSize = 18.sp, color = c.foreground)
        Text(
            "Desk Hall Settings above is the source for columns and chowky when present. " +
                "SAVE HALL LAYOUT only keeps depth, and any field the desk omitted.",
            color = c.muted,
            fontSize = 12.sp,
        )
        listOf(Gender.M to "Male hall", Gender.F to "Female hall").forEach { (g, label) ->
            val grid = staged.getValue(g)
            val deskCols = desk.seatsPerRow(g) != null
            Text(
                "$label · ${grid.columns} columns · ${grid.depth} deep",
                fontFamily = DipiCondensed,
                fontSize = 16.sp,
                color = c.foreground,
                modifier = Modifier.padding(top = 8.dp),
            )
            HallStepperRow(
                label = if (deskCols) "Columns (from desk)" else "Columns (A, B, C…)",
                value = grid.columns,
                min = HallGrid.MIN_COLUMNS,
                max = HallGrid.MAX_COLUMNS,
                contentLabel = "columns · $label",
                enabled = !deskCols,
            ) { n -> staged = staged + (g to grid.copy(columns = n)) }
            HallStepperRow(
                label = "Rows (1 nearest teacher)",
                value = grid.depth,
                min = HallGrid.MIN_DEPTH,
                max = HallGrid.MAX_DEPTH,
                contentLabel = "rows deep · $label",
            ) { n -> staged = staged + (g to grid.copy(depth = n)) }
        }
        Text(
            "Chowky / chair rail",
            fontFamily = DipiCondensed,
            fontSize = 16.sp,
            color = c.foreground,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            "CW-A1 is nearest the Dhamma seat.",
            color = c.muted,
            fontSize = 12.sp,
        )
        val deskChowky = desk.malePlan.chowkyColumns != null || desk.femalePlan.chowkyColumns != null
        if (deskChowky) {
            Text(
                "Chowky columns come from the desk Main Plan. Vertical/Wrap follows that width.",
                color = c.muted,
                fontSize = 12.sp,
            )
        }
        Row(
            Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val rail = staged.getValue(Gender.M).chowkyRail
            listOf(
                ChowkyRailLayout.SINGLE_ROW to "Vertical",
                ChowkyRailLayout.WRAP to "Wrap",
            ).forEach { (layout, label) ->
                val on = rail == layout
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (on) FontWeight.Medium else FontWeight.Normal,
                    color = if (on) Color.White else c.foreground,
                    modifier = Modifier
                        .background(
                            if (on) c.accent else Color.Transparent,
                            org.dhamma.dipi.staff.ui.theme.DeskStyle.controlShape,
                        )
                        .then(
                            if (deskChowky) Modifier else Modifier.clickable {
                                staged = staged.mapValues { it.value.copy(chowkyRail = layout) }
                            },
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("chowky-rail-$layout"),
                )
            }
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
    enabled: Boolean = true,
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
            enabled = enabled && value > min,
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
            enabled = enabled && value < max,
            contentDescription = "Increase $contentLabel",
            onClick = { onValue(value + 1) },
        )
    }
}
