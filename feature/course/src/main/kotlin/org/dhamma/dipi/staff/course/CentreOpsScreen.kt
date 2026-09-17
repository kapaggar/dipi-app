package org.dhamma.dipi.staff.course

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.CentreHallSettings
import org.dhamma.dipi.staff.model.CentreOpsPrefs
import org.dhamma.dipi.staff.model.ChowkyRailLayout
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.HallGrid
import org.dhamma.dipi.staff.model.WHATSAPP_DEFAULT_TEMPLATE
import org.dhamma.dipi.staff.model.WHATSAPP_TOKENS
import org.dhamma.dipi.staff.model.centreOpsEffect
import org.dhamma.dipi.staff.model.whatsAppMessage
import org.dhamma.dipi.staff.ui.theme.DeskKicker
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import org.dhamma.dipi.staff.ui.theme.LocalIndustry
import org.dhamma.dipi.staff.ui.theme.deskCard

private const val COLUMNS_STEPPER_LABEL = "Columns (A, B, C …)"
private const val ROWS_STEPPER_LABEL = "Rows (1 is nearest the teacher)"

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
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clickable(onClick = onBack)
                    .semantics { contentDescription = "Back to centre dashboard" },
                contentAlignment = Alignment.Center,
            ) {
                Text("‹", fontSize = 26.sp, color = c.accent)
            }
            Column(Modifier.weight(1f).padding(top = 6.dp)) {
                Text(
                    "Centre settings",
                    fontFamily = DipiCondensed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = c.foreground,
                )
                Text(
                    "These settings belong to the centre and apply to every course the desk runs.",
                    color = c.muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .deskCard()
                .clickable(onClick = onOpenRooms)
                .heightIn(min = 72.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Room chart", fontFamily = DipiCondensed, fontSize = 20.sp, color = c.foreground)
                Text(
                    "Rooms, blocks and the chart layout. Each block carries its own inventory; " +
                        "blocks are never assumed to hold the same number of rooms.",
                    color = c.muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Text("›", fontSize = 18.sp, color = c.accent)
        }
        DeskKicker(
            "CHECK-IN OPTIONS",
            industry.neutral500,
            modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ToggleRow(
                title = "Laundry",
                note = "Ask for laundry at check-in and include a laundry column on the Day 0 list.",
                on = prefs.laundry,
                onClick = onToggleLaundry,
                testTag = "toggle-laundry",
            )
            ToggleRow(
                title = "Valuables",
                note = "Record valuables handed in at check-in. Items are listed on the checking slip, not on the printed roll.",
                on = prefs.valuables,
                onClick = onToggleValuables,
                testTag = "toggle-valuables",
            )
            ToggleRow(
                title = "Groups",
                note = "Assign a sitting group at check-in. Groups still come from the desk site; this only shows the field at the desk.",
                on = prefs.groups,
                onClick = onToggleGroups,
                testTag = "toggle-groups",
            )
        }
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .deskCard()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            DeskKicker("RESULT", industry.neutral500)
            Text(centreOpsEffect(prefs), color = c.foreground, fontSize = 14.sp)
        }
        DeskKicker(
            "HALL CHART",
            industry.neutral500,
            modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
        )
        Text(
            "Seat labels outside these dimensions extend the grid rather than being dropped.",
            color = c.muted,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        HallChartSection(
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
    val accessibleName = "$title. $note"
    Row(
        Modifier
            .fillMaxWidth()
            .deskCard()
            // The row is the single toggle target: it carries the click AND the
            // On/Off semantics (Role.Switch), so a tap anywhere on the row fires
            // onClick exactly once. The Switch below is display-only
            // (onCheckedChange = null) so it doesn't install a second, competing
            // toggleable — that would double-fire on a thumb tap.
            .toggleable(value = on, onValueChange = { onClick() }, role = Role.Switch)
            .semantics { contentDescription = accessibleName }
            .testTag(testTag)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = c.foreground, fontSize = 16.sp)
            Text(note, color = c.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
        }
        Box(
            Modifier
                .size(48.dp)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            Switch(
                checked = on,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    uncheckedThumbColor = c.muted,
                    uncheckedTrackColor = c.field,
                    uncheckedBorderColor = c.hairlineStrong,
                ),
            )
        }
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
 * Live desk Hall Settings from `GET /centre/{cid}/edit`. Compact read-only
 * lines under the two hall cards so the 2.3.0 source stays visible without
 * a third card above the chart.
 */
@Composable
private fun DeskHallFacts(hall: CentreHallSettings) {
    val c = LocalDipi.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .testTag("desk-hall-settings"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Hall Settings", color = c.muted, fontSize = 12.sp)
        if (!hall.isPresent()) {
            Text("No Hall Settings on the last fetch.", color = c.muted, fontSize = 13.sp)
            return
        }
        HallFact("Male/Female students in same hall", hall.combinedLabel())
        HallFact("Seat naming", hall.seatNamingLabel())
        Text("Male (Main Plan)", color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        Text(hall.malePlan.summaryLine("right"), color = c.foreground, fontSize = 13.sp)
        Text("Female (Main Plan)", color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        Text(hall.femalePlan.summaryLine("left"), color = c.foreground, fontSize = 13.sp)
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

/**
 * Two gender cards plus the local SAVE HALL LAYOUT strip. Desk Main Plan
 * columns / chowky win when present; depth stays the local override.
 */
@Composable
private fun HallChartSection(
    male: HallGrid,
    female: HallGrid,
    desk: CentreHallSettings = CentreHallSettings(),
    onHallGrid: (Gender, HallGrid) -> Unit,
) {
    val c = LocalDipi.current
    var staged by remember(male, female) { mutableStateOf(mapOf(Gender.M to male, Gender.F to female)) }
    var committed by remember(male, female) { mutableStateOf(mapOf(Gender.M to male, Gender.F to female)) }
    val dirty = staged != committed
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val sideBySide = maxWidth >= 800.dp
        val maleCard: @Composable (Modifier) -> Unit = { cardModifier ->
            val grid = staged.getValue(Gender.M)
            HallGenderCard(
                title = "Male hall",
                grid = grid,
                columnsLocked = desk.seatsPerRow(Gender.M) != null,
                modifier = cardModifier,
                testTag = "hall-card-male",
                onColumns = { n -> staged = staged + (Gender.M to grid.copy(columns = n)) },
                onDepth = { n -> staged = staged + (Gender.M to grid.copy(depth = n)) },
            )
        }
        val femaleCard: @Composable (Modifier) -> Unit = { cardModifier ->
            val grid = staged.getValue(Gender.F)
            HallGenderCard(
                title = "Female hall",
                grid = grid,
                columnsLocked = desk.seatsPerRow(Gender.F) != null,
                modifier = cardModifier,
                testTag = "hall-card-female",
                onColumns = { n -> staged = staged + (Gender.F to grid.copy(columns = n)) },
                onDepth = { n -> staged = staged + (Gender.F to grid.copy(depth = n)) },
            )
        }
        if (sideBySide) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                maleCard(Modifier.weight(1f))
                femaleCard(Modifier.weight(1f))
            }
        } else {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                maleCard(Modifier.fillMaxWidth())
                femaleCard(Modifier.fillMaxWidth())
            }
        }
    }
    DeskHallFacts(desk)
    Text(
        "Chowky / chair rail",
        fontFamily = DipiCondensed,
        fontSize = 16.sp,
        color = c.foreground,
        modifier = Modifier.padding(top = 12.dp),
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val rail = staged.getValue(Gender.M).chowkyRail
        listOf(
            ChowkyRailLayout.SINGLE_ROW to "Vertical",
            ChowkyRailLayout.WRAP to "Wrap",
        ).forEach { (layout, label) ->
            val on = rail == layout
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = if (on) FontWeight.Medium else FontWeight.Normal,
                color = if (on) Color.White else c.foreground,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .background(
                        if (on) c.accent else Color.Transparent,
                        org.dhamma.dipi.staff.ui.theme.DeskStyle.controlShape,
                    )
                    .then(
                        if (deskChowky) {
                            Modifier
                        } else {
                            Modifier.clickable {
                                staged = staged.mapValues { it.value.copy(chowkyRail = layout) }
                            }
                        },
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
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

@Composable
private fun HallGenderCard(
    title: String,
    grid: HallGrid,
    columnsLocked: Boolean,
    modifier: Modifier,
    testTag: String,
    onColumns: (Int) -> Unit,
    onDepth: (Int) -> Unit,
) {
    val c = LocalDipi.current
    Column(
        modifier
            .deskCard()
            .testTag(testTag)
            .padding(16.dp),
    ) {
        Text(title, fontFamily = DipiCondensed, fontSize = 22.sp, color = c.foreground)
        Text(
            hallSummary(grid),
            fontFamily = DipiMono,
            fontSize = 13.sp,
            color = c.muted,
            modifier = Modifier.padding(top = 3.dp),
        )
        HallStepperRow(
            label = COLUMNS_STEPPER_LABEL,
            value = grid.columns,
            min = HallGrid.MIN_COLUMNS,
            max = HallGrid.MAX_COLUMNS,
            contentLabel = "columns · $title",
            enabled = !columnsLocked,
            modifier = Modifier.padding(top = 12.dp),
        ) { onColumns(it) }
        HallStepperRow(
            label = ROWS_STEPPER_LABEL,
            value = grid.depth,
            min = HallGrid.MIN_DEPTH,
            max = HallGrid.MAX_DEPTH,
            contentLabel = "rows deep · $title",
            modifier = Modifier.padding(top = 12.dp),
        ) { onDepth(it) }
    }
}

private fun hallSummary(grid: HallGrid): String =
    "${grid.columns} columns · ${grid.depth} deep · A1 nearest the Dhamma seat"

/** One `label … − {n} +` line. Buttons 48dp, value 56dp centred, 12dp gaps. */
@Composable
private fun HallStepperRow(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    contentLabel: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onValue: (Int) -> Unit,
) {
    val c = LocalDipi.current
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, color = c.foreground, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StepperButton(
                symbol = "−",
                enabled = enabled && value > min,
                contentDescription = "Decrease $contentLabel",
                onClick = { onValue(value - 1) },
            )
            Text(
                "$value",
                fontFamily = DipiMono,
                fontSize = 16.sp,
                color = c.foreground,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(56.dp),
            )
            StepperButton(
                symbol = "+",
                enabled = enabled && value < max,
                contentDescription = "Increase $contentLabel",
                onClick = { onValue(value + 1) },
            )
        }
    }
}
