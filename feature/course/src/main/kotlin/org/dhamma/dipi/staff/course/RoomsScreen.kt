package org.dhamma.dipi.staff.course

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.ui.theme.DeskStyle
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import org.dhamma.dipi.staff.ui.theme.deskCard

/**
 * Read-only room chart, mirroring the paper chart at the desk: numbered
 * cells in a grid, the amenity mark (G geyser · IC Indian commode ·
 * W Western toilet) under the number. The list itself comes from the desk
 * site's centre config; the app never adds or deletes rooms. When opened
 * from Zero Day the tap still assigns the room to the applicant.
 */
@Composable
fun RoomsScreen(
    rooms: List<AccoRoom>,
    genderFilter: Gender? = null,
    onPick: (AccoRoom) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val c = LocalDipi.current
    val shown = rooms.filter { genderFilter == null || it.gender == genderFilter }
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Room chart", fontFamily = DipiCondensed, fontSize = 22.sp, color = c.foreground)
        TextButton(onClick = onBack) { Text("Back") }
        if (shown.isEmpty()) {
            Text(
                "No rooms for this filter. The room list comes from the desk site.",
                color = c.muted,
                modifier = Modifier.padding(top = 12.dp),
            )
            return@Column
        }
        listOf(Gender.F to "Female", Gender.M to "Male").forEach { (g, label) ->
            val block = shown.filter { it.gender == g }
            if (block.isEmpty()) return@forEach
            block.groupBy { it.section }.forEach { (section, sectionRooms) ->
                Text(
                    listOf(label, section).filter { it.isNotBlank() }.joinToString(" · ") +
                        " · ${sectionRooms.size} rooms",
                    fontFamily = DipiCondensed,
                    fontSize = 16.sp,
                    color = c.foreground,
                    modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                )
                sectionRooms.chunked(4).forEachIndexed { i, rowRooms ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(DeskStyle.tileShape)
                            .background(if (i % 2 == 1) c.tint else androidx.compose.ui.graphics.Color.Transparent),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        rowRooms.forEach { room ->
                            Column(
                                Modifier
                                    .weight(1f)
                                    .deskCard(shape = DeskStyle.tileShape, fill = c.field, border = c.hairline)
                                    .clickable { onPick(room) }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    room.displayNo,
                                    fontFamily = DipiCondensed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = c.foreground,
                                )
                                Text(
                                    room.amenityMark.ifBlank { " " },
                                    fontFamily = DipiMono,
                                    fontSize = 10.sp,
                                    color = c.muted,
                                )
                            }
                        }
                        repeat(4 - rowRooms.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}
