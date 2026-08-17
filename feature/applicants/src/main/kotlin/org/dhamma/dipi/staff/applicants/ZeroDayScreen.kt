package org.dhamma.dipi.staff.applicants

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.CentreOpsPrefs
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.MAIN_DHAMMA_HALL
import org.dhamma.dipi.staff.ui.theme.DeskStyle
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.LocalDipi

data class ZeroDayDraft(
    val seating: String = "None",
    val laundry: String = "",
    val valuables: String = "",
    val roomCode: String? = null,
)

@Composable
fun ZeroDayScreen(
    course: Course,
    rows: List<ApplicantCard>,
    prefs: CentreOpsPrefs,
    drafts: Map<ApplicantId, ZeroDayDraft> = emptyMap(),
    onSeating: (ApplicantCard, String) -> Unit = { _, _ -> },
    onLaundry: (ApplicantCard, String) -> Unit = { _, _ -> },
    onValuables: (ApplicantCard, String) -> Unit = { _, _ -> },
    onRoom: (ApplicantCard) -> Unit = {},
    onMarkAttended: (ApplicantCard) -> Unit = {},
    onOpen: (ApplicantCard) -> Unit = {},
    onBack: () -> Unit = {},
    pendingRoomSync: Int = 0,
    roomSyncBusy: Boolean = false,
    roomPullBusy: Boolean = false,
    onSyncRooms: () -> Unit = {},
    onPullRooms: () -> Unit = {},
) {
    val c = LocalDipi.current
    val unattended = rows.filter { !it.attended }
    val attended = rows.filter { it.attended }
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(course.name, fontFamily = DipiCondensed, fontSize = 22.sp, color = c.foreground)
        Text("Zero Day", fontFamily = DipiCondensed, fontSize = 18.sp, color = c.accent)
        if (prefs.groups) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                listOf("A", "B", "C").forEach { g ->
                    Text(
                        g,
                        fontFamily = DipiCondensed,
                        fontSize = 12.sp,
                        color = c.foreground,
                        modifier = Modifier
                            .border(1.dp, c.hairlineStrong, DeskStyle.controlShape)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        } else {
            Text(MAIN_DHAMMA_HALL, color = c.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Back") }
            val actionsEnabled = !roomPullBusy && !roomSyncBusy
            TextButton(onClick = onPullRooms, enabled = actionsEnabled) {
                Text(if (roomPullBusy) "Pulling rooms…" else "Pull rooms")
            }
            // Mirror of the desk's bulk allocation sync — hidden at 0 pending.
            if (pendingRoomSync > 0 || roomSyncBusy) {
                TextButton(onClick = onSyncRooms, enabled = actionsEnabled) {
                    Text(if (roomSyncBusy) "Syncing rooms…" else "Sync rooms ($pendingRoomSync)")
                }
            }
        }
        Text("Unattended", fontFamily = DipiCondensed, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
        if (unattended.isEmpty()) {
            Text("Everyone on this list is marked attended.", color = c.muted, fontSize = 13.sp)
        }
        unattended.forEach { card ->
            val draft = drafts[card.id] ?: ZeroDayDraft()
            Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Text(
                    card.displayName,
                    fontFamily = DipiCondensed,
                    fontSize = 18.sp,
                    color = c.foreground,
                    modifier = Modifier.clickable { onOpen(card) },
                )
                Text("${card.status.value}  ${card.gender.name}", color = c.muted, fontSize = 12.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    listOf("Chowky", "Chair", "Backrest", "None").forEach { seat ->
                        val sel = draft.seating == seat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .border(1.dp, c.accent, DeskStyle.controlShape)
                                .background(if (sel) c.accent else Color.Transparent, DeskStyle.controlShape)
                                .clickable { onSeating(card, seat) }
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                seat,
                                color = if (sel) Color.White else c.foreground,
                                fontFamily = DipiCondensed,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
                if (prefs.laundry) {
                    OutlinedTextField(
                        draft.laundry,
                        { onLaundry(card, it) },
                        label = { Text("Laundry") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
                if (prefs.valuables) {
                    OutlinedTextField(
                        draft.valuables,
                        { onValuables(card, it) },
                        label = { Text("Valuables") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
                TextButton(onClick = { onRoom(card) }) {
                    Text(if (draft.roomCode.isNullOrBlank()) "Room" else "Room  ${draft.roomCode}")
                }
                TextButton(onClick = { onMarkAttended(card) }) { Text("Mark attended") }
            }
        }
        Text("Attended", fontFamily = DipiCondensed, fontSize = 16.sp, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
        if (attended.isEmpty()) {
            Text("No one marked attended yet.", color = c.muted, fontSize = 13.sp)
        }
        attended.forEach { card ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(card) }
                    .padding(vertical = 8.dp),
            ) {
                Text(card.displayName, fontFamily = DipiCondensed, fontSize = 16.sp, color = c.foreground)
                Text("${card.status.value}  ${card.gender.name}", color = c.muted, fontSize = 12.sp)
            }
        }
    }
}
