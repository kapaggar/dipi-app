package org.dhamma.dipi.staff.course

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.CentreOpsPrefs
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.LocalDipi

@Composable
fun CentreOpsScreen(
    prefs: CentreOpsPrefs,
    onToggleLaundry: () -> Unit,
    onToggleValuables: () -> Unit,
    onToggleGroups: () -> Unit,
    onAddRooms: (Gender, String, String) -> Unit,
    onDeleteSection: (Gender, String) -> Unit,
    onOpenRooms: () -> Unit,
    onBack: () -> Unit,
) {
    val c = LocalDipi.current
    var gender by remember { mutableStateOf(Gender.F) }
    var section by remember { mutableStateOf("") }
    var codes by remember { mutableStateOf("") }
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
        TextButton(onClick = onOpenRooms) { Text("Rooms") }
        Text("Accommodation", fontFamily = DipiCondensed, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Gender", color = c.muted, fontFamily = DipiCondensed, modifier = Modifier.weight(1f))
            Text("Section", color = c.muted, fontFamily = DipiCondensed, modifier = Modifier.weight(1.4f))
            Text("Rooms", color = c.muted, fontFamily = DipiCondensed, modifier = Modifier.weight(2f))
        }
        grouped.forEach { (key, rooms) ->
            val (g, sec) = key
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(g.name, modifier = Modifier.weight(1f), color = c.foreground)
                Text(sec, modifier = Modifier.weight(1.4f), color = c.foreground)
                Text(
                    rooms.joinToString(", ") { it.code },
                    modifier = Modifier.weight(2f),
                    color = c.foreground,
                )
                TextButton(onClick = { onDeleteSection(g, sec) }) { Text("Delete") }
            }
        }
        Text("New accommodation", fontFamily = DipiCondensed, modifier = Modifier.padding(top = 16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            Gender.entries.forEach { g ->
                val sel = gender == g
                Text(
                    g.name,
                    color = if (sel) androidx.compose.ui.graphics.Color.White else c.foreground,
                    fontFamily = DipiCondensed,
                    modifier = Modifier
                        .border(1.dp, c.accent, RoundedCornerShape(4.dp))
                        .background(if (sel) c.accent else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(4.dp))
                        .clickable { gender = g }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        OutlinedTextField(section, { section = it }, label = { Text("Section name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            codes,
            { codes = it },
            label = { Text("Room codes") },
            placeholder = { Text("F32, F33") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        TextButton(
            onClick = {
                onAddRooms(gender, section, codes)
                codes = ""
            },
            modifier = Modifier.padding(top = 8.dp),
        ) { Text("Add rooms") }
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
