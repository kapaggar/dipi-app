package org.dhamma.dipi.staff.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.desktop.derive.deskFreeRooms
import org.dhamma.dipi.staff.desktop.derive.deskGenderScope
import org.dhamma.dipi.staff.desktop.derive.deskOccupied
import org.dhamma.dipi.staff.desktop.derive.deskRecord
import org.dhamma.dipi.staff.desktop.derive.deskRoll
import org.dhamma.dipi.staff.desktop.derive.deskRosterRows
import org.dhamma.dipi.staff.desktop.derive.deskSeniorityScope
import org.dhamma.dipi.staff.desktop.theme.LocalDipi
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.model.SEAT_TYPES

@Composable
fun CheckInPane(
    roll: List<ApplicantCard>,
    checkIns: Map<ApplicantId, CheckInRecord>,
    rooms: List<AccoRoom>,
    scan: String,
    filter: String,
    gender: String,
    seniority: String,
    onScan: (String) -> Unit,
    onFilter: (String) -> Unit,
    onGender: (String) -> Unit,
    onSeniority: (String) -> Unit,
    onOpen: (ApplicantCard) -> Unit,
) {
    val c = LocalDipi.current
    val scoped = deskRoll(roll, deskGenderScope(gender), deskSeniorityScope(seniority))
    val rows = deskRosterRows(scoped, checkIns, scan, filter)
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DeskField(scan, onScan, Modifier.fillMaxWidth(), placeholder = "Scan or type conf / name")
        Segmented(listOf("To arrive", "Arrived", "All"), filter, onFilter)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Segmented(listOf("Both", "Male", "Female"), gender, onGender, Modifier.weight(1f))
            Segmented(listOf("Both", "New", "Old"), seniority, onSeniority, Modifier.weight(1f))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            items(rows, key = { it.id.value }) { card ->
                val rec = deskRecord(card, checkIns)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(DeskShape)
                        .background(c.field)
                        .clickable { onOpen(card) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(card.displayName, color = c.foreground, fontWeight = FontWeight.Medium)
                        Text(
                            listOfNotNull(card.confNo?.value, rec?.room?.takeIf { it.isNotBlank() }).joinToString(" · "),
                            color = c.muted,
                            fontSize = 12.sp,
                        )
                    }
                    Text(if (rec?.checkedIn == true) "IN" else "—", color = if (rec?.checkedIn == true) c.accent else c.muted)
                }
            }
        }
    }
}

@Composable
fun CheckInDialog(
    card: ApplicantCard,
    record: CheckInRecord,
    roll: List<ApplicantCard>,
    checkIns: Map<ApplicantId, CheckInRecord>,
    rooms: List<AccoRoom>,
    roomOpen: Boolean,
    laundryOn: Boolean,
    valuablesOn: Boolean,
    groupsOn: Boolean,
    onToggleRooms: () -> Unit,
    onRoom: (String) -> Unit,
    onSeat: (String) -> Unit,
    onValuables: () -> Unit,
    onLaundry: () -> Unit,
    onGroup: (String) -> Unit,
    onSave: () -> Unit,
    onUndo: () -> Unit,
    onClose: () -> Unit,
) {
    val c = LocalDipi.current
    val occupied = deskOccupied(roll, checkIns, except = card.id)
    val free = deskFreeRooms(rooms, card.gender, occupied)
    Box(
        Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0x99000000))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.72f)
                .clip(DeskShape)
                .background(c.background)
                .border(1.dp, c.hairlineStrong, DeskShape)
                .clickable(enabled = false) {}
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(card.displayName, color = c.foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(card.confNo?.value ?: "", color = c.muted)
            DeskButton(if (record.room.isBlank()) "Choose room" else record.room, onToggleRooms, Modifier.fillMaxWidth())
            if (roomOpen) {
                free.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { room ->
                            DeskButton(room.code, { onRoom(room.code) }, primary = room.code == record.room)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SEAT_TYPES.forEach { seat ->
                    DeskButton(seat, { onSeat(seat) }, primary = record.seat == seat)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (valuablesOn) DeskButton(if (record.valuables) "Valuables on" else "Valuables off", onValuables)
                if (laundryOn) DeskButton(if (record.laundry) "Laundry on" else "Laundry off", onLaundry)
                if (groupsOn) {
                    listOf("1", "2", "3").forEach { g ->
                        DeskButton("G$g", { onGroup(g) }, primary = record.group == g)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeskButton("Check in", onSave, primary = true)
                DeskButton("Undo arrival", onUndo)
                DeskButton("Close", onClose)
            }
        }
    }
}
