package org.dhamma.dipi.staff.desktop.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.dhamma.dipi.staff.desktop.derive.DeskCourse
import org.dhamma.dipi.staff.desktop.derive.DeskRail
import org.dhamma.dipi.staff.desktop.derive.DeskSection
import org.dhamma.dipi.staff.desktop.theme.LocalDipi
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DeskShell(
    section: DeskSection,
    rail: DeskRail,
    course: DeskCourse,
    loading: Boolean,
    onSection: (DeskSection) -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    content: @Composable (DeskSection) -> Unit,
) {
    val c = LocalDipi.current
    val clock = deskClock()
    Row(Modifier.fillMaxSize().background(c.background)) {
        Column(
            Modifier
                .width(196.dp)
                .fillMaxHeight()
                .background(c.field)
                .padding(12.dp),
        ) {
            Text("DIPI", color = c.accent, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 13.sp)
            Text(rail.userName, color = c.foreground, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            Text(rail.syncLine, color = c.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
            DeskSection.entries.forEach { item ->
                val on = item == section
                val count = rail.counts[item]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(if (on) c.tint else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { onSection(item) }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        item.label,
                        color = if (on) c.accent else c.foreground,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                    )
                    if (count != null) Text("$count", color = if (on) c.accent else c.muted, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            DeskButton("Settings", onSettings, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            DeskButton("Courses", onBack, Modifier.fillMaxWidth())
        }
        VHairline(Modifier.fillMaxHeight())
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(section.crumb, color = c.muted, fontSize = 12.sp, letterSpacing = 1.sp)
                Text("  ·  ", color = c.hairlineStrong)
                Text(course.line, color = c.foreground, fontSize = 14.sp, modifier = Modifier.weight(1f))
                if (loading) Text("loading", color = c.muted, fontSize = 12.sp)
                Text(clock, color = c.muted, fontSize = 13.sp)
            }
            Hairline()
            Box(Modifier.weight(1f)) { content(section) }
        }
    }
}

@Composable
private fun deskClock(): String {
    val fmt = remember { DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", Locale.ENGLISH) }
    val clock = produceState(LocalDateTime.now().format(fmt)) {
        while (true) {
            value = LocalDateTime.now().format(fmt)
            delay(60_000L - (System.currentTimeMillis() % 60_000L))
        }
    }
    return clock.value
}
