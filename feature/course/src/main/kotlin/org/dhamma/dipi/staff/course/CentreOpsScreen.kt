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
import org.dhamma.dipi.staff.model.CentreOpsPrefs
import org.dhamma.dipi.staff.model.centreOpsEffect
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
