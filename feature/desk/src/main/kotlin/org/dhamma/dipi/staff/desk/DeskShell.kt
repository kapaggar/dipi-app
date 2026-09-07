package org.dhamma.dipi.staff.desk

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.ui.R
import org.dhamma.dipi.staff.ui.theme.DeskKicker
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.DipiSans
import org.dhamma.dipi.staff.ui.theme.Industry
import org.dhamma.dipi.staff.ui.theme.LotusWatermark
import org.dhamma.dipi.staff.ui.theme.deskWash

/** The six desk sections the left rail routes between. Centre settings live on the Centre screen. */
enum class DeskSection(val label: String, val crumb: String) {
    Board("0 Day Board", "0 DAY BOARD"),
    Applications("Applications", "APPLICATIONS"),
    Audit("Audit", "AUDIT"),
    Calling("Calling", "CALLING ROUND"),
    CheckIn("Check-in", "ZERO DAY · CHECK-IN"),
    Rooms("Room Chart", "ROOM CHART"),
}

/** Everything the persistent rail displays. Counts are derived by the caller, never stored. */
data class DeskRail(
    val userName: String,
    val syncLine: String,
    val counts: Map<DeskSection, Int> = emptyMap(),
)

/** Course identity, shown in the 52dp top bar: "10 Day · 26 Aug – 4 Sep · DAY 0". */
data class DeskCourse(
    val dates: String,
    val dayChip: String?,
) {
    val line: String
        get() = listOfNotNull(dates.ifBlank { null }, dayChip)
            .joinToString(" · ")
}

/**
 * The tablet desk shell: fixed 190dp rail, 52dp top bar, and the active
 * section's pane. The desk never scrolls as a whole — each pane scrolls
 * independently. Under all content sit the version-3 ambient accent washes
 * and — when [lotus] is on — the lotus watermark bottom-left, both static
 * (Industry motion is progress and toggles only).
 */
@Composable
fun DeskShell(
    section: DeskSection,
    rail: DeskRail,
    course: DeskCourse,
    clock: String,
    onSection: (DeskSection) -> Unit,
    loading: Boolean = false,
    lotus: Boolean = true,
    content: @Composable (DeskSection) -> Unit = { DeskSectionPlaceholder(it) },
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Industry.bg)
            .clipToBounds()
            .deskWash(Industry.accent),
    ) {
        if (lotus) {
            LotusWatermark(
                size = 300.dp,
                opacity = Industry.skin.markOpacity * 0.7f,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-52).dp, y = 64.dp)
                    .testTag("desk-watermark"),
            )
        }
        Row(Modifier.fillMaxSize()) {
            DeskRailPane(section, rail, onSection)
            Column(Modifier.weight(1f).fillMaxHeight()) {
                DeskTopBar(course.line, clock)
                if (loading) DeskProgressHairline(Modifier.testTag("desk-loading"))
                Box(Modifier.weight(1f)) { content(section) }
            }
        }
    }
}

@Composable
private fun DeskRailPane(
    section: DeskSection,
    rail: DeskRail,
    onSection: (DeskSection) -> Unit,
) {
    Column(
        Modifier
            .width(190.dp)
            .fillMaxHeight()
            .background(Industry.surface)
            .rightHairline(Industry.neutral300)
            .padding(top = 20.dp, bottom = 16.dp)
            .testTag("desk-rail"),
    ) {
        Image(
            painterResource(R.drawable.lotus_mark),
            contentDescription = "DIPI",
            modifier = Modifier
                .padding(start = 18.dp, end = 14.dp, bottom = 12.dp)
                .size(54.dp)
                .graphicsLayer { alpha = 0.78f },
        )

        DeskKicker("DESK", Industry.neutral500, Modifier.padding(start = 18.dp, bottom = 6.dp))

        DeskSection.entries.forEach { s ->
            DeskNavRow(
                label = s.label,
                count = rail.counts[s],
                active = s == section,
                onClick = { onSection(s) },
            )
        }

        Spacer(Modifier.weight(1f))

        Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(rail.userName, fontFamily = DipiMono, fontSize = 12.sp, color = Industry.neutral600)
            Text(
                rail.syncLine,
                fontFamily = DipiMono,
                fontSize = 12.sp,
                color = Industry.neutral500,
            )
        }
    }
}

@Composable
private fun DeskNavRow(label: String, count: Int?, active: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(if (active) Industry.accent100 else Color.Transparent)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (active) {
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(Industry.accent)
                    .testTag("rail-accent-bar"),
            )
        }
        Text(
            label,
            fontFamily = DipiSans,
            fontSize = 15.5.sp,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
            color = if (active) Industry.accent800 else Industry.neutral700,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (active) 15.dp else 18.dp),
        )
        if (count != null) {
            Text(
                count.toString(),
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = if (active) Industry.accent700 else Industry.neutral500,
                modifier = Modifier.padding(end = 18.dp),
            )
        }
    }
}

@Composable
private fun DeskTopBar(courseLine: String, clock: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .bottomHairline(Industry.neutral300)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            courseLine,
            fontFamily = DipiCondensed,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            letterSpacing = 0.2.sp,
            color = Industry.text,
            maxLines = 1,
        )
        Text(
            clock,
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = Industry.neutral600,
        )
    }
}

/** Stand-in pane until each section's slice lands. */
@Composable
fun DeskSectionPlaceholder(section: DeskSection) {
    Column(Modifier.fillMaxSize().padding(26.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DeskKicker("SLICE PENDING", Industry.accent700)
        Text(
            "The ${section.label} pane arrives in a later build slice.",
            fontFamily = DipiSans,
            fontSize = 12.5.sp,
            color = Industry.neutral600,
        )
    }
}
