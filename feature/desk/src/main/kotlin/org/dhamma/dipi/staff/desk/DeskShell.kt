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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.ui.R
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.DipiSans
import org.dhamma.dipi.staff.ui.theme.Industry
import org.dhamma.dipi.staff.ui.theme.LotusWatermark
import org.dhamma.dipi.staff.ui.theme.blueprint
import org.dhamma.dipi.staff.ui.theme.deskWash

/** The six desk sections the left rail routes between. Centre settings live on the Centre screen. */
enum class DeskSection(val label: String, val crumb: String) {
    Board("Board", "BOARD"),
    Applications("Applications", "APPLICATIONS"),
    Audit("Audit", "AUDIT"),
    Calling("Calling", "CALLING ROUND"),
    CheckIn("Check-in", "ZERO DAY · CHECK-IN"),
    Rooms("Rooms & seats", "ROOMS & SEATS"),
}

/** Everything the persistent rail displays. Counts are derived by the caller, never stored. */
data class DeskRail(
    val courseName: String,
    val courseDates: String,
    val dayChip: String?,
    val userName: String,
    val syncLine: String,
    val counts: Map<DeskSection, Int> = emptyMap(),
)

/**
 * The tablet desk shell: fixed 212dp rail, 52dp top bar, and the active
 * section's pane. The desk never scrolls as a whole — each pane scrolls
 * independently. Under all content sit the version-3 ambient accent washes
 * and — when [lotus] is on — the lotus watermark bottom-left, both static
 * (Industry motion is progress and toggles only).
 */
@Composable
fun DeskShell(
    section: DeskSection,
    rail: DeskRail,
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
                DeskTopBar(section.crumb, clock)
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
            .width(212.dp)
            .fillMaxHeight()
            .rightHairline(Industry.neutral300)
            .padding(top = 20.dp, bottom = 16.dp),
    ) {
        Row(
            Modifier.padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.blueprint(Industry.accent).padding(3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painterResource(R.drawable.lotus_mark),
                    contentDescription = "DIPI",
                    modifier = Modifier.size(30.dp),
                )
            }
            Text(
                "DIPI Staff",
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 18.sp,
                color = Industry.text,
            )
        }

        Column(
            Modifier
                .padding(start = 14.dp, end = 14.dp, bottom = 22.dp)
                .fillMaxWidth()
                .blueprint(Industry.neutral400)
                .padding(start = 12.dp, end = 12.dp, top = 11.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            DeskKicker("COURSE", Industry.accent700)
            Text(
                rail.courseName,
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 19.sp,
                lineHeight = 21.sp,
                color = Industry.text,
            )
            Text(
                rail.courseDates,
                fontFamily = DipiSans,
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
                color = Industry.neutral600,
            )
            if (rail.dayChip != null) {
                Text(
                    rail.dayChip,
                    fontFamily = DipiMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 0.1.em,
                    color = Industry.accent800,
                    modifier = Modifier
                        .background(Industry.accent100)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
        }

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
            Text(rail.userName, fontFamily = DipiSans, fontSize = 11.5.sp, color = Industry.neutral600)
            Text(
                rail.syncLine,
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 10.5.sp,
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
            .clickable(onClick = onClick)
            .bottomHairline(Industry.neutral200)
            .padding(top = 9.dp, bottom = 9.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(2.dp)
                .height(16.dp)
                .background(if (active) Industry.accent else Color.Transparent),
        )
        Text(
            label,
            fontFamily = DipiSans,
            fontSize = 13.5.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color = if (active) Industry.accent800 else Industry.neutral700,
            modifier = Modifier.weight(1f),
        )
        if (count != null) {
            Text(
                count.toString(),
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = Industry.neutral500,
            )
        }
    }
}

@Composable
private fun DeskTopBar(crumb: String, clock: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .bottomHairline(Industry.neutral300)
            .padding(horizontal = 26.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            crumb,
            fontFamily = DipiCondensed,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            letterSpacing = 0.14.em,
            color = Industry.neutral600,
        )
        Text(
            clock,
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = Industry.neutral500,
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

/** IBM Plex Mono 600 / 9.5sp / .16em kicker — the system's all-caps label. */
@Composable
fun DeskKicker(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        fontFamily = DipiMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 9.5.sp,
        letterSpacing = 0.16.em,
        color = color,
        modifier = modifier,
    )
}

