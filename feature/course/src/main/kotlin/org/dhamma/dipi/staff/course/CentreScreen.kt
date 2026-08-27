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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.Centre
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseMatrix
import org.dhamma.dipi.staff.model.CourseSummary
import org.dhamma.dipi.staff.model.MatrixRow
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
    olderCourses: List<Course> = emptyList(),
) {
    val c = LocalDipi.current
    val centre = session.centres.firstOrNull()
    val cid = centre?.id?.value ?: 0
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val columns = if (wide) 2 else 1
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
        if (wide) {
            // Fixed header over two independently scrolling regions —
            // upcoming courses dominate at 60%, everything else gets 40%
            // (owner feedback 2026-08-27). Weights, not fillMaxHeight
            // fractions, so nothing nests a same-axis verticalScroll.
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .heightIn(max = 220.dp)
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                ) {
                    // Bounded so an account with many centres can never
                    // squeeze the weighted regions below toward 0dp (owner
                    // feedback 2026-08-27) — the switcher list scrolls
                    // within this cap instead of pushing content out.
                    CentreHeaderBlock(session, centre, onPickCentre, maxListHeight = 160.dp)
                }
                Column(
                    Modifier
                        .weight(0.6f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    UpcomingCoursesBlock(courses, columns, onPick)
                }
                Column(
                    Modifier
                        .weight(0.4f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    OlderCoursesAndDeskBlock(
                        olderCourses = olderCourses,
                        cid = cid,
                        onPick = onPick,
                        onLater = onLater,
                        onCentreOps = onCentreOps,
                        onAdvancedSearch = onAdvancedSearch,
                        onSettings = onSettings,
                    )
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                CentreHeaderBlock(session, centre, onPickCentre)
                UpcomingCoursesBlock(courses, columns, onPick)
                OlderCoursesAndDeskBlock(
                    olderCourses = olderCourses,
                    cid = cid,
                    onPick = onPick,
                    onLater = onLater,
                    onCentreOps = onCentreOps,
                    onAdvancedSearch = onAdvancedSearch,
                    onSettings = onSettings,
                )
            }
        }
    }
}

@Composable
private fun CentreHeaderBlock(
    session: Session,
    centre: Centre?,
    onPickCentre: (Centre) -> Unit,
    // Non-null only on the wide layout's fixed header, which has no scroll
    // of its own to fall back on. Left null on the narrow path, which
    // already lives inside a single page-level verticalScroll (nesting a
    // second same-axis scroll there would fight it) — no visible change to
    // the common single-centre account either way.
    maxListHeight: Dp? = null,
) {
    val c = LocalDipi.current
    Text(
        "${centre?.name ?: "Centre"} · from your account · ${session.displayName}",
        fontFamily = DipiCondensed,
        fontSize = 22.sp,
        color = c.foreground,
    )
    if (session.centres.size > 1) {
        val listModifier = if (maxListHeight != null) {
            Modifier.heightIn(max = maxListHeight).verticalScroll(rememberScrollState())
        } else {
            Modifier
        }
        Column(listModifier) {
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
    }
}

@Composable
private fun UpcomingCoursesBlock(
    courses: List<Course>,
    columns: Int,
    onPick: (Course) -> Unit,
) {
    val c = LocalDipi.current
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
}

@Composable
private fun OlderCoursesAndDeskBlock(
    olderCourses: List<Course>,
    cid: Int,
    onPick: (Course) -> Unit,
    onLater: (String, String) -> Unit,
    onCentreOps: () -> Unit,
    onAdvancedSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    val c = LocalDipi.current
    if (olderCourses.isNotEmpty()) {
        Text(
            "Older courses",
            color = c.muted,
            modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
        )
        Text(
            "Teacher list · valuables · seating — check-in is closed",
            color = c.muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        olderCourses.forEach { course ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .deskCard(fill = c.field, border = c.hairline)
                    .clickable { onPick(course) }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    course.name,
                    fontFamily = DipiCondensed,
                    fontSize = 16.sp,
                    lineHeight = 19.sp,
                    color = c.foreground,
                )
                val dates = listOf(course.start, course.end).filter { it.isNotBlank() }
                if (dates.isNotEmpty()) {
                    Text(dates.joinToString(" – "), color = c.muted, fontSize = 12.sp)
                }
            }
        }
    }

    // The desk links as compact tiles, three across, blended into the page
    // ground rather than raised (owner feedback 2026-08-27): native screens
    // (Centre Settings, Advanced Search, App Settings) lead and dispatch via
    // `DeskTileAction`; every other tile still opens the desk site.
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
                        .deskCard(
                            shape = DeskStyle.tileShape,
                            fill = Color.Transparent,
                            border = c.hairline,
                            elevation = 0.dp,
                        )
                        .clickable {
                            when (tile.action) {
                                DeskTileAction.CentreOps -> onCentreOps()
                                DeskTileAction.AdvancedSearch -> onAdvancedSearch()
                                DeskTileAction.AppSettings -> onSettings()
                                null -> onLater(tile.title, tile.route)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        tile.title,
                        color = c.foreground,
                        fontFamily = DipiCondensed,
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
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
        val matrix = course.matrix
        if (matrix != null) {
            CourseMatrixTable(matrix, modifier = Modifier.padding(top = 4.dp))
        } else {
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
}

/** A zero renders as a middot, never `0` — matching the desk, which leaves empty cells blank. */
private fun matrixCell(n: Int): String = if (n == 0) "·" else n.toString()

/**
 * The compact gender-split matrix (spec S4): a kicker header, one row per
 * [CourseMatrix.highlights] (Received/Confirmed/Cancelled, all-zero rows
 * already filtered out upstream), then an emphasised Total row with sevak
 * counts appended when present.
 */
@Composable
private fun CourseMatrixTable(matrix: CourseMatrix, modifier: Modifier = Modifier) {
    val c = LocalDipi.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        MatrixHeaderRow()
        matrix.highlights.forEach { row -> MatrixDataRow(row.label, row, emphasise = false) }
        matrix.total?.let { total -> MatrixDataRow("Total", total, emphasise = true) }
    }
}

/**
 * The kicker row, built from the same weight()-based `Row` and per-cell
 * modifiers as [MatrixDataRow] so each label sits directly above its column
 * on a real device — a manually-spaced literal string can't guarantee that.
 */
@Composable
private fun MatrixHeaderRow() {
    val c = LocalDipi.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Spacer(Modifier.weight(1.6f))
        listOf("NM", "OM", "M", "NF", "OF", "F").forEach { label ->
            Text(
                label,
                fontFamily = DipiMono,
                fontSize = 10.sp,
                color = c.muted,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(0.75f),
            )
        }
    }
}

@Composable
private fun MatrixDataRow(label: String, row: MatrixRow, emphasise: Boolean) {
    val c = LocalDipi.current
    val weight = if (emphasise) FontWeight.Bold else FontWeight.Medium
    val sevak = row.sevakTotal
    val displayLabel = if (emphasise && sevak > 0) "$label  +$sevak sevak" else label
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            displayLabel,
            fontFamily = DipiCondensed,
            fontSize = if (emphasise) 13.sp else 12.sp,
            fontWeight = weight,
            color = c.foreground,
            modifier = Modifier.weight(1.6f),
        )
        listOf(row.newMale, row.oldMale, row.maleTotal, row.newFemale, row.oldFemale, row.femaleTotal)
            .forEach { n ->
                Text(
                    matrixCell(n),
                    fontFamily = DipiMono,
                    fontSize = 11.sp,
                    fontWeight = weight,
                    color = if (emphasise) c.foreground else c.muted,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(0.75f),
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
