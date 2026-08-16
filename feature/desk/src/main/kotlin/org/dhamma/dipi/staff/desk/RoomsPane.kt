package org.dhamma.dipi.staff.desk

import androidx.compose.foundation.background
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
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.Industry
import org.dhamma.dipi.staff.ui.theme.blueprint

/** The occupancy picture: who is where tonight, and what is free. */
@Composable
fun RoomsPane(
    roll: List<ApplicantCard>,
    checkIns: Map<ApplicantId, CheckInRecord>,
    rooms: List<AccoRoom>,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 24.dp),
    ) {
        Column(Modifier.padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            DeskH2("Rooms & seats")
            DeskSub("Filled cells are occupied tonight. Amenity marks: G geyser · I Indian · W Western.")
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
                    block.chunked(3).forEach { rowRooms ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowRooms.forEach { room ->
                                val who = occupantByRoom[room.code]
                                RoomCell(room, who, Modifier.weight(1f))
                            }
                            repeat(3 - rowRooms.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    if (block.isEmpty()) {
                        DeskEmpty(
                            "No rooms yet — add them in Centre settings.",
                            Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomCell(room: AccoRoom, occupant: String?, modifier: Modifier = Modifier) {
    val taken = occupant != null
    Column(
        modifier
            .background(if (taken) Industry.accent100 else Color.Transparent)
            .blueprint(if (taken) Industry.accent else Industry.neutral300)
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                room.code,
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                lineHeight = 19.sp,
                color = if (taken) Industry.accent800 else Industry.neutral500,
            )
            Text(
                amenityMarks(room),
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
