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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.LocalDipi

@Composable
fun RoomsScreen(
    rooms: List<AccoRoom>,
    genderFilter: Gender? = null,
    editMode: Boolean = false,
    onPick: (AccoRoom) -> Unit = {},
    onToggleFeature: (String, String) -> Unit = { _, _ -> },
    onBack: () -> Unit = {},
) {
    val c = LocalDipi.current
    val shown = rooms.filter { genderFilter == null || it.gender == genderFilter }
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .padding(20.dp),
    ) {
        Text("Rooms", fontFamily = DipiCondensed, fontSize = 22.sp, color = c.foreground)
        TextButton(onClick = onBack) { Text("Back") }
        if (shown.isEmpty()) {
            Text("No rooms for this filter.", color = c.muted, modifier = Modifier.padding(top = 12.dp))
        } else {
            LazyColumn {
                items(shown, key = { it.code }) { room ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !editMode) { onPick(room) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(room.code, fontFamily = DipiCondensed, fontSize = 18.sp, color = c.foreground)
                            Text(
                                listOf(room.gender.name, room.section).filter { it.isNotBlank() }.joinToString(" · ") +
                                    if (room.localExample) "  example" else "",
                                color = c.muted,
                                fontSize = 12.sp,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FeatureChip("Geyser", room.features.geyser, editMode) {
                                onToggleFeature(room.code, "geyser")
                            }
                            FeatureChip("Indian", room.features.indianToilet, editMode) {
                                onToggleFeature(room.code, "indianToilet")
                            }
                            FeatureChip("Western", room.features.westernToilet, editMode) {
                                onToggleFeature(room.code, "westernToilet")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureChip(label: String, on: Boolean, editMode: Boolean, onClick: () -> Unit) {
    val c = LocalDipi.current
    Text(
        label,
        color = if (on) Color.White else c.foreground,
        fontFamily = DipiCondensed,
        fontSize = 11.sp,
        modifier = Modifier
            .border(1.dp, if (on) c.accent else c.hairlineStrong, RoundedCornerShape(4.dp))
            .background(if (on) c.accent else Color.Transparent, RoundedCornerShape(4.dp))
            .then(if (editMode) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
