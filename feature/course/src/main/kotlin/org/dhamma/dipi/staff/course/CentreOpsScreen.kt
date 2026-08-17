package org.dhamma.dipi.staff.course

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.CentreOpsPrefs
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.LocalDipi

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
        ToggleRow("Laundry", prefs.laundry, onToggleLaundry)
        ToggleRow("Valuables", prefs.valuables, onToggleValuables)
        ToggleRow("Groups", prefs.groups, onToggleGroups)
        Text(
            "when off, everyone sits in Main Dhamma Hall and Zero Day hides group chips",
            color = c.muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        TextButton(onClick = onOpenRooms) { Text("Room chart") }
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
private fun ToggleRow(label: String, on: Boolean, onClick: () -> Unit) {
    val c = LocalDipi.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$label: ${if (on) "on" else "off"}", fontFamily = DipiCondensed, fontSize = 16.sp, color = c.foreground)
    }
}
