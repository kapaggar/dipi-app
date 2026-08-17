package org.dhamma.dipi.staff.desk

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.RoomSyncFailure
import org.dhamma.dipi.staff.ui.theme.DeskStyle
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.Industry
import org.dhamma.dipi.staff.ui.theme.deskCard

/**
 * The occupancy picture: who is where tonight, and what is free. The header
 * carries pull-from-server (always) and the bulk allocation sync (owner
 * amendment 2026-08-16): "Sync N to server" walks every unsynced checked-in
 * record through the desk's own update form — hidden at N=0. Both buttons
 * disable while either walk is in flight; per-row refusals list under the header.
 */
@Composable
fun RoomsPane(
    roll: List<ApplicantCard>,
    checkIns: Map<ApplicantId, CheckInRecord>,
    rooms: List<AccoRoom>,
    pendingSync: Int = 0,
    syncBusy: Boolean = false,
    pullBusy: Boolean = false,
    syncFailures: List<RoomSyncFailure> = emptyList(),
    onSyncRooms: () -> Unit = {},
    onPullRooms: () -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 24.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DeskH2("Rooms & seats")
                DeskSub("Filled cells are occupied tonight. Amenity marks: G geyser · IC Indian · W Western.")
                if (syncFailures.isNotEmpty()) {
                    SyncRefusals(roll, syncFailures)
                }
            }
            val actionsEnabled = !pullBusy && !syncBusy
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoomPullButton(pullBusy, actionsEnabled, onPullRooms)
                if (pendingSync > 0 || syncBusy) {
                    RoomSyncButton(pendingSync, syncBusy, actionsEnabled, onSyncRooms)
                }
            }
        }

        val occupantByRoom = buildMap {
            roll.forEach { card ->
                val rec = deskRecord(card, checkIns)
                if (rec?.checkedIn == true && rec.room.isNotBlank()) put(rec.room, card.displayName)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            listOf(Gender.F to "Female", Gender.M to "Male").forEach { (g, label) ->
                val block = rooms.filter { it.gender == g }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val free = block.count { it.code !in occupantByRoom }
                    val sections = block.map { it.section }.distinct().filter { it.isNotBlank() }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .bottomHairline(Industry.neutral400)
                            .padding(bottom = 7.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            if (sections.isEmpty()) label else "$label · ${sections.joinToString("/")} block",
                            fontFamily = DipiCondensed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            lineHeight = 22.sp,
                            color = Industry.text,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${block.size} rooms · $free free",
                            fontFamily = DipiMono,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.5.sp,
                            color = Industry.neutral600,
                        )
                    }
                    // Chart bands like the paper ROOM CHART: four cells a row,
                    // alternate rows on a soft rounded band of the neutral ground.
                    block.chunked(4).forEachIndexed { i, rowRooms ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(DeskStyle.tileShape)
                                .background(if (i % 2 == 1) Industry.neutral100 else Color.Transparent),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowRooms.forEach { room ->
                                val who = occupantByRoom[room.code]
                                RoomCell(room, who, Modifier.weight(1f))
                            }
                            repeat(4 - rowRooms.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    if (block.isEmpty()) {
                        DeskEmpty(
                            "No rooms configured on the desk site yet.",
                            Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Outline "PULL FROM SERVER" — card fill, accent label. Busy → "PULLING…".
 * Always shown; disabled while either pull or sync is in flight.
 */
@Composable
private fun RoomPullButton(busy: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Text(
        (if (busy) "Pulling…" else "Pull from server").uppercase(),
        fontFamily = DipiCondensed,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.06.em,
        maxLines = 1,
        color = if (enabled) Industry.accent else Industry.neutral600,
        modifier = Modifier
            .deskCard(
                shape = DeskStyle.controlShape,
                fill = DeskStyle.cardFill,
                border = if (enabled) Industry.accent else Industry.neutral400,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp),
    )
}

/**
 * The one deliberate accent fill on this pane: "SYNC N TO SERVER", busy →
 * "SYNCING…" and inert. Callers hide it entirely at N=0.
 */
@Composable
private fun RoomSyncButton(pending: Int, busy: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Text(
        (if (busy) "Syncing…" else "Sync $pending to server").uppercase(),
        fontFamily = DipiCondensed,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.06.em,
        maxLines = 1,
        color = Color.White,
        modifier = Modifier
            .deskCard(
                shape = DeskStyle.controlShape,
                fill = if (busy) Industry.accent700 else Industry.accent,
                border = if (busy) Industry.accent700 else Industry.accent,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp),
    )
}

/** Per-row refusals from the last sync run: name + the server's reason, verbatim. */
@Composable
private fun SyncRefusals(roll: List<ApplicantCard>, failures: List<RoomSyncFailure>) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .deskCard(border = Industry.accent, elevation = 0.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "SERVER REFUSED ${failures.size}",
            fontFamily = DipiMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            letterSpacing = 0.1.em,
            color = Industry.accent700,
        )
        failures.forEach { failure ->
            val name = roll.firstOrNull { it.id == failure.id }?.displayName
                ?: "Applicant ${failure.id.value}"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Industry.text,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    failure.reason,
                    fontSize = 12.sp,
                    color = Industry.neutral600,
                )
            }
        }
    }
}

@Composable
private fun RoomCell(room: AccoRoom, occupant: String?, modifier: Modifier = Modifier) {
    val taken = occupant != null
    Column(
        modifier
            .deskCard(
                shape = DeskStyle.tileShape,
                fill = if (taken) Industry.accent100 else DeskStyle.cardFill,
                border = if (taken) Industry.accent else DeskStyle.cardBorder,
                elevation = 0.dp,
            )
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                room.displayNo,
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                lineHeight = 19.sp,
                color = if (taken) Industry.accent800 else Industry.neutral500,
            )
            Text(
                room.amenityMark,
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 9.5.sp,
                letterSpacing = 0.1.em,
                color = Industry.neutral500,
            )
        }
        Text(
            occupant ?: "free",
            fontSize = 11.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Industry.neutral600,
        )
    }
}

// Centre settings moved off the desk: the global CentreOpsScreen opens from
// the Centre screen, so the desk rail carries six sections and no
// CentreSettingsPane any more.
