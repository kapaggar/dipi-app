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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.Centre
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseSummary
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.ui.theme.DeskStyle
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.Industry
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import org.dhamma.dipi.staff.ui.theme.LotusWatermark
import org.dhamma.dipi.staff.ui.theme.deskCard
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * One-line counts for a course card, e.g.
 * "Confirmed 58 · Expected 15 | Cancelled 5 | Received 2 | Total 106".
 * Zero and absent counts drop out; null when there is nothing to show.
 */
fun courseCountsLine(summary: CourseSummary?): String? {
    if (summary == null) return null
    val pipeline = listOfNotNull(
        summary.confirmed.takeIf { it > 0 }?.let { "Confirmed $it" },
        summary.expected.takeIf { it > 0 }?.let { "Expected $it" },
    ).joinToString(" · ")
    val parts = listOfNotNull(
        pipeline.takeIf { it.isNotEmpty() },
        summary.cancelled.takeIf { it > 0 }?.let { "Cancelled $it" },
        summary.received.takeIf { it > 0 }?.let { "Received $it" },
        summary.total.takeIf { it > 0 }?.let { "Total $it" },
    )
    return parts.joinToString(" | ").takeIf { it.isNotEmpty() }
}

@Composable
fun CentreScreen(
    session: Session,
    courses: List<Course>,
    onPick: (Course) -> Unit,
    onPickCentre: (Centre) -> Unit = {},
    onSettings: () -> Unit = {},
    onLater: (String, String) -> Unit = { _, _ -> },
    onCentreOps: () -> Unit = {},
    onAdvancedSearch: () -> Unit = {},
    lotus: Boolean = true,
) {
    val c = LocalDipi.current
    val centre = session.centres.firstOrNull()
    val cid = centre?.id?.value ?: 0
    val columns = if (LocalConfiguration.current.screenWidthDp >= 600) 2 else 1
    Box(Modifier.fillMaxSize().background(c.background)) {
        if (lotus) {
            // The relief: large, very low-contrast, skin-tinted, behind
            // everything and non-interactive (owner feedback 2026-08-16).
            LotusWatermark(
                size = 480.dp,
                opacity = Industry.skin.markOpacity * 0.5f,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
        Text(
            "${centre?.name ?: "Centre"} · from your account · ${session.displayName}",
            fontFamily = DipiCondensed,
            fontSize = 22.sp,
            color = c.foreground,
        )
        if (session.centres.size > 1) {
            session.centres.forEach { item ->
                Text(
                    item.name,
                    color = if (item.id == centre?.id) c.accent else c.muted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPickCentre(item) }
                        .padding(vertical = 6.dp),
                )
            }
        }

        Text("Upcoming courses", color = c.muted, modifier = Modifier.padding(top = 18.dp, bottom = 10.dp))
        if (courses.isEmpty()) {
            Text("No upcoming courses.", color = c.muted, fontSize = 13.sp)
        } else {
            courses.chunked(columns).forEachIndexed { rowIndex, row ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEachIndexed { colIndex, course ->
                        CourseCard(
                            course = course,
                            first = rowIndex == 0 && colIndex == 0,
                            modifier = Modifier.weight(1f),
                            onClick = { onPick(course) },
                        )
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        // Global centre settings — reachable without picking a course.
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .deskCard(fill = c.field, border = c.hairline)
                .clickable(onClick = onCentreOps)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text("Centre settings", fontFamily = DipiCondensed, fontSize = 18.sp, color = c.foreground)
            Text(
                "Laundry · valuables · groups · room management",
                color = c.muted,
                fontSize = 12.sp,
            )
        }

        // The desk links as compact tiles, three across (owner feedback
        // 2026-08-16). Advanced Search rides along as a tile and opens the
        // in-app search screen; every other tile still opens the desk site.
        Text("Centre desk", color = c.muted, modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
        centreDeskTiles(cid).chunked(3).forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { tile ->
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 62.dp)
                            .deskCard(shape = DeskStyle.tileShape, fill = c.field, border = c.hairline)
                            .clickable {
                                if (tile.title == "Advanced Search") {
                                    onAdvancedSearch()
                                } else {
                                    onLater(tile.title, tile.route)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            tile.title,
                            color = c.accent,
                            fontFamily = DipiCondensed,
                            fontSize = 15.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        TextButton(onClick = onSettings, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Settings")
        }
        }
    }
}

@Composable
private fun CourseCard(
    course: Course,
    first: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = LocalDipi.current
    val days = runCatching {
        ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(course.start))
    }.getOrDefault(0)
    Column(
        modifier
            .deskCard(fill = c.field, border = c.hairline)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            course.name,
            fontFamily = DipiCondensed,
            fontSize = 19.sp,
            lineHeight = 22.sp,
            color = c.foreground,
        )
        if (course.start.isNotBlank() || course.end.isNotBlank()) {
            Text(
                listOf(course.start, course.end).filter { it.isNotBlank() }.joinToString(" – "),
                color = c.muted,
                fontSize = 13.sp,
            )
        }
        if (first && days > 0) {
            Text("STARTS IN $days DAYS", color = c.accent, fontFamily = DipiCondensed, fontSize = 12.sp)
        }
        val counts = courseCountsLine(course.summary)
        if (counts != null) {
            Text(
                counts,
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = c.muted,
            )
        }
    }
}

@Composable
fun CoursesScreen(
    session: Session,
    courses: List<Course>,
    onPick: (Course) -> Unit,
    onPickCentre: (Centre) -> Unit = {},
    onSettings: () -> Unit = {},
) = CentreScreen(session, courses, onPick, onPickCentre, onSettings)
