package org.dhamma.dipi.staff.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.desktop.derive.deskOccupied
import org.dhamma.dipi.staff.desktop.theme.LocalDipi
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.RoomSyncFailure

@Composable
fun RoomsPane(
    roll: List<ApplicantCard>,
    checkIns: Map<ApplicantId, CheckInRecord>,
    rooms: List<AccoRoom>,
    pendingSync: Int,
    syncBusy: Boolean,
    pullBusy: Boolean,
    syncFailures: List<RoomSyncFailure>,
    onSyncRooms: () -> Unit,
    onPullRooms: () -> Unit,
) {
    val c = LocalDipi.current
    val occupied = deskOccupied(roll, checkIns)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeskButton(
                if (syncBusy) "Syncing…" else "Sync $pendingSync room(s)",
                onSyncRooms,
                primary = true,
                enabled = !syncBusy && !pullBusy,
            )
            DeskButton(if (pullBusy) "Pulling…" else "Pull from desk", onPullRooms, enabled = !syncBusy && !pullBusy)
        }
        if (syncFailures.isNotEmpty()) {
            Text(syncFailures.first().reason, color = c.hard, fontSize = 13.sp)
        }
        Text("Centre rooms are read-only — the desk site owns the list.", color = c.muted, fontSize = 13.sp)
        listOf(Gender.M to "Male", Gender.F to "Female").forEach { (g, label) ->
            Text(label.uppercase(), color = c.muted, fontSize = 11.sp, letterSpacing = 1.sp)
            rooms.filter { it.gender == g }.chunked(6).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { room ->
                        val taken = room.code in occupied
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(DeskShape)
                                .background(if (taken) c.tint else c.field)
                                .border(1.dp, if (taken) c.accent else c.hairline, DeskShape)
                                .padding(8.dp),
                        ) {
                            Text(room.displayNo, color = c.foreground, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(if (taken) "taken" else room.amenityMark.ifBlank { "free" }, color = c.muted, fontSize = 11.sp)
                        }
                    }
                    repeat(6 - row.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
