package org.dhamma.dipi.staff.course

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.ui.theme.DeskStyle
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import org.dhamma.dipi.staff.ui.theme.deskCard

/**
 * The phone course hub (below ~1100dp — the tablet runs the v2 desk). Opens
 * straight into native flows: a card grid of the day's work with count chips
 * derived from the worklist. The desk-site links live only in the ⋯ overflow
 * menu (owner feedback 2026-08-16).
 */
@Composable
fun CourseHubScreen(
    course: Course,
    centreName: String,
    counts: Map<CourseHubLive, Int> = emptyMap(),
    onBack: () -> Unit,
    onSettings: () -> Unit = {},
    onApplications: () -> Unit,
    onSummary: () -> Unit,
    onPhotos: () -> Unit,
    onAudit: () -> Unit = {},
    onCalling: () -> Unit = {},
    onZeroDay: () -> Unit = {},
    onCentreOps: () -> Unit = {},
    onLater: (String, String) -> Unit,
) {
    val c = LocalDipi.current
    val cid = course.centreId.value
    val id = course.id.value
    var menuOpen by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(centreName, color = c.muted, fontSize = 12.sp)
                Text(course.name, fontFamily = DipiCondensed, fontSize = 22.sp, color = c.foreground)
            }
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.semantics { contentDescription = "Desk site links" },
                ) {
                    Text("⋯", color = c.foreground, fontSize = 20.sp)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    courseHubDeskTiles(cid, id).forEach { tile ->
                        DropdownMenuItem(
                            text = { Text(tile.title) },
                            onClick = {
                                menuOpen = false
                                onLater(tile.title, tile.route)
                            },
                        )
                    }
                }
            }
        }
        if (course.start.isNotBlank() || course.end.isNotBlank()) {
            Text(
                listOf(course.start, course.end).filter { it.isNotBlank() }.joinToString(" - "),
                color = c.muted,
                fontSize = 12.sp,
            )
        }
        courseCountsLine(course.summary)?.let { line ->
            Text(
                line,
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = c.muted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("← Centre") }
            TextButton(onClick = onSettings) { Text("Settings") }
        }
        Text("During the course", color = c.muted, modifier = Modifier.padding(top = 12.dp, bottom = 10.dp))
        val columns = if (LocalConfiguration.current.screenWidthDp >= 700) 3 else 2
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            courseHubLiveTiles(cid, id).chunked(columns).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    row.forEach { tile ->
                        HubTile(
                            tile = tile,
                            count = tile.live?.let { counts[it] },
                            modifier = Modifier.weight(1f),
                            onClick = {
                                when (tile.live) {
                                    CourseHubLive.Applications -> onApplications()
                                    CourseHubLive.Summary -> onSummary()
                                    CourseHubLive.Photos -> onPhotos()
                                    CourseHubLive.Audit -> onAudit()
                                    CourseHubLive.Calling -> onCalling()
                                    CourseHubLive.ZeroDay -> onZeroDay()
                                    CourseHubLive.CentreOps -> onCentreOps()
                                    null -> onLater(tile.title, tile.route)
                                }
                            },
                        )
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** A native-flow card: accent glyph, condensed title, mono count chip when non-zero. */
@Composable
private fun HubTile(
    tile: CourseHubTile,
    count: Int?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = LocalDipi.current
    Column(
        modifier
            .heightIn(min = 84.dp)
            .deskCard(shape = DeskStyle.tileShape, fill = c.field, border = c.hairline)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(tile.glyph, color = c.accent, fontSize = 17.sp)
            if (count != null && count > 0) {
                Text(
                    "$count",
                    fontFamily = DipiMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = c.accent,
                    modifier = Modifier
                        .clip(DeskStyle.pillShape)
                        .background(c.tint)
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
        }
        Text(
            tile.title,
            color = c.foreground,
            fontFamily = DipiCondensed,
            fontSize = 16.sp,
            lineHeight = 19.sp,
        )
    }
}
