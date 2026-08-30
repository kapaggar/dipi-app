package org.dhamma.dipi.staff.course

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.Centre
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseMatrix
import org.dhamma.dipi.staff.model.CourseSummary
import org.dhamma.dipi.staff.model.MatrixRow
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.model.cardRows
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
            // Fixed header over two regions — upcoming courses take up to 60%
            // and the lower pane takes the rest (owner feedback 2026-08-27,
            // amended 2026-08-30). No fillMaxHeight fractions, so nothing
            // nests a same-axis verticalScroll.
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .heightIn(max = 220.dp)
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                ) {
                    // Bounded so an account with many centres can never
                    // squeeze the regions below toward 0dp (owner feedback
                    // 2026-08-27) — the switcher list scrolls within this cap
                    // instead of pushing content out.
                    CentreHeaderBlock(session, centre, onPickCentre, maxListHeight = 160.dp)
                }
                // The 60/40 split cannot be two sibling weights any more.
                // Compose reserves a weighted child's slot from the weight
                // ratio alone and never redistributes what a `fill = false`
                // child declines, so weight(0.6f)/weight(0.4f) left the space
                // upcoming turned down (~292px on the Pixel C) dead at the
                // bottom of the screen, with the lower pane still clamped to
                // 40% and its chips pushed past the fold. Re-weighting cannot
                // fix that: any ratio that hands the leftover downward also
                // lowers the upcoming ceiling (0.6f beside 1f is a 37.5%
                // ceiling, which clips the second card row away entirely).
                //
                // So the ceiling is measured rather than weighted:
                // BoxWithConstraints gives the height below the header,
                // upcoming is capped at 60% of it, and the lower pane's
                // weight(1f) takes everything else — the full remainder when
                // upcoming declines its share. Both properties hold at once.
                BoxWithConstraints(Modifier.weight(1f)) {
                    val belowHeader = maxHeight
                    Column(Modifier.fillMaxSize()) {
                        // No scroll here (owner decision 2026-08-30): the
                        // block is bounded. The desk serves at most four
                        // upcoming courses (`limit 4` in the backend's
                        // `upcoming_courses()`), rendered two per row, so the
                        // pane holds at most two card rows; and every card is
                        // now the same fixed height (S3, `cardRows`). Both
                        // premises are load-bearing — if either changed, this
                        // pane would clip rather than scroll, and a clipped
                        // card here is unreachable, not merely cut off.
                        Column(
                            Modifier
                                .heightIn(max = belowHeader * 0.6f)
                                .padding(horizontal = 20.dp),
                        ) {
                            UpcomingCoursesBlock(courses, columns, onPick)
                        }
                        // Owner decision 2026-08-30: the lower pane no longer
                        // splits into two columns — older courses take the
                        // full width on the upcoming grid, with the desk
                        // column stacked beneath.
                        Box(
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 20.dp)
                                .padding(top = 12.dp, bottom = 8.dp),
                        ) {
                            WideLowerPane(
                                olderCourses = olderCourses,
                                columns = columns,
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
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                CentreHeaderBlock(session, centre, onPickCentre)
                UpcomingCoursesBlock(courses, columns, onPick)
                NarrowLowerPane(
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
        "${centre?.name ?: "Centre"} · ${session.displayName}",
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

/**
 * The wide lower pane. Owner decision 2026-08-30: the older-course buttons
 * were noticeably narrower than the upcoming cards above them, so they now
 * render on the **same [columns] grid**, inside a pane with the same
 * horizontal insets — an older button is exactly as wide as an upcoming card.
 *
 * That needs the full pane width, so the side-by-side split is gone: older
 * courses stack, then the "Centre desk" column beneath them, also full width
 * and therefore laid out exactly as the no-older-courses case always did
 * (three tiles across at 52dp — one row, not three, which is what keeps the
 * stack short enough to be worth stacking). The scroll the older column used
 * to carry moves to the stack, so a long older list pushes the desk column
 * into the scroll rather than off the pane.
 *
 * With no older courses (frame 1g) the heading stays omitted, as it always
 * was, and the desk column takes the full width with its three tiles across.
 */
@Composable
private fun WideLowerPane(
    olderCourses: List<Course>,
    columns: Int,
    cid: Int,
    onPick: (Course) -> Unit,
    onLater: (String, String) -> Unit,
    onCentreOps: () -> Unit,
    onAdvancedSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    val c = LocalDipi.current
    if (olderCourses.isEmpty()) {
        CentreDeskColumn(
            cid = cid,
            tilesPerRow = 3,
            tileHeight = 52.dp,
            onLater = onLater,
            onCentreOps = onCentreOps,
            onAdvancedSearch = onAdvancedSearch,
            onSettings = onSettings,
        )
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("Older courses", color = c.muted, modifier = Modifier.padding(bottom = 10.dp))
        olderCourses.chunked(columns).forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { course ->
                    OlderCourseRow(course, Modifier.weight(1f)) { onPick(course) }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(14.dp))
        CentreDeskColumn(
            cid = cid,
            tilesPerRow = 3,
            tileHeight = 52.dp,
            onLater = onLater,
            onCentreOps = onCentreOps,
            onAdvancedSearch = onAdvancedSearch,
            onSettings = onSettings,
        )
    }
}

/**
 * The phone/narrow lower pane: the same rows, tiles and chips as frame 1a,
 * stacked inside the page's single scroll rather than split into columns —
 * a second same-axis scroll here would fight the page's own.
 */
@Composable
private fun NarrowLowerPane(
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
            modifier = Modifier.padding(top = 18.dp, bottom = 10.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            olderCourses.forEach { course -> OlderCourseRow(course) { onPick(course) } }
        }
    }
    Spacer(Modifier.height(20.dp))
    CentreDeskColumn(
        cid = cid,
        tilesPerRow = 1,
        tileHeight = 48.dp,
        onLater = onLater,
        onCentreOps = onCentreOps,
        onAdvancedSearch = onAdvancedSearch,
        onSettings = onSettings,
    )
}

/**
 * One older-course row: 42dp, card fill on a hairline, the name in condensed
 * and a `›` chevron. The date sub-line is gone — every real course name
 * already carries its dates ("… / 2026 / 6th-Aug to 17th-Aug"), which is why
 * frame 1a draws a single line.
 */
@Composable
private fun OlderCourseRow(
    course: Course,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = LocalDipi.current
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp)
            .deskCard(shape = DeskStyle.controlShape, fill = c.field, border = c.hairline, elevation = 0.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            course.name,
            fontFamily = DipiCondensed,
            fontSize = 16.sp,
            lineHeight = 19.sp,
            color = c.foreground,
            // Finding 2: same treatment as the upcoming card's name — pin
            // the slot to two lines so older buttons in the same grid row
            // don't diverge in height depending on how long the name is.
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text("›", color = c.muted, fontSize = 15.sp)
    }
}

/**
 * The "Centre desk" column of frame 1a. The tile/chip split is
 * [DeskTileSpec.action]'s own: the three in-app destinations (Centre Settings,
 * Advanced Search, App Settings) are the transparent, zero-elevation tiles;
 * the remaining desk-site links (two, after the S1 trim) become pill chips
 * with a trailing `↗` under a `MORE ON THE DESK SITE` kicker. Every callback
 * fires exactly as before — chips still hand `onLater` the catalogue's own
 * (title, route) pair.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CentreDeskColumn(
    cid: Int,
    tilesPerRow: Int,
    tileHeight: Dp,
    onLater: (String, String) -> Unit,
    onCentreOps: () -> Unit,
    onAdvancedSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    val c = LocalDipi.current
    val tiles = centreDeskTiles(cid)
    Column(Modifier.fillMaxWidth()) {
        Text("Centre desk", color = c.muted, modifier = Modifier.padding(bottom = 10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            tiles.filter { it.action != null }.chunked(tilesPerRow).forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { tile ->
                        DeskTile(tile.title, tileHeight, Modifier.weight(1f)) {
                            when (tile.action) {
                                DeskTileAction.CentreOps -> onCentreOps()
                                DeskTileAction.AdvancedSearch -> onAdvancedSearch()
                                DeskTileAction.AppSettings -> onSettings()
                                null -> onLater(tile.title, tile.route)
                            }
                        }
                    }
                    repeat(tilesPerRow - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
        Spacer(Modifier.height(11.dp))
        Text(
            "MORE ON THE DESK SITE",
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            letterSpacing = 1.7.sp,
            color = c.muted,
        )
        Spacer(Modifier.height(9.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            tiles.filter { it.action == null }.forEach { tile ->
                DeskSiteChip(tile.title) { onLater(tile.title, tile.route) }
            }
        }
    }
}

/** An in-app desk tile: transparent fill, hairline border, zero elevation. */
@Composable
private fun DeskTile(title: String, height: Dp, modifier: Modifier, onClick: () -> Unit) {
    val c = LocalDipi.current
    Row(
        modifier
            .height(height)
            .deskCard(
                shape = DeskStyle.controlShape,
                fill = Color.Transparent,
                border = c.hairline,
                elevation = 0.dp,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            title,
            color = c.foreground,
            fontFamily = DipiCondensed,
            fontSize = 16.sp,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text("›", color = c.muted, fontSize = 15.sp)
    }
}

/** A desk-site link: a 30dp pill chip with a trailing `↗`. */
@Composable
private fun DeskSiteChip(title: String, onClick: () -> Unit) {
    val c = LocalDipi.current
    Row(
        Modifier
            .height(30.dp)
            .deskCard(
                shape = DeskStyle.pillShape,
                fill = Color.Transparent,
                border = c.hairline,
                elevation = 0.dp,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(title, color = c.muted, fontSize = 12.5.sp, maxLines = 1)
        Text("↗", color = c.muted, fontSize = 11.sp)
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
            // Frame 1a's next-course marker: a 3dp accent bar inset 8dp top
            // and bottom, on the soonest course only. Drawn 3dp wider than it
            // shows so its left corners fall outside the card's clip and only
            // the right pair rounds, as the frame draws it.
            .then(
                if (first) {
                    Modifier.drawBehind {
                        val w = 3.dp.toPx()
                        val inset = 8.dp.toPx()
                        drawRoundRect(
                            color = c.accent,
                            topLeft = Offset(-w, inset),
                            size = Size(w * 2, size.height - inset * 2),
                            cornerRadius = CornerRadius(w),
                        )
                    }
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 14.dp, top = 11.dp, bottom = 9.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        // Gate-review fix (Finding 1): a card's height must not depend on
        // its content. The name slot is pinned to exactly two lines — a
        // one-line name still reserves the second line's height instead of
        // shrinking the card — and the date / "starts in" line renders
        // unconditionally so its slot is reserved even when blank. Compose
        // still lays out an empty string at the style's line height (Text
        // defaults to minLines = 1), so a blank line here costs the same
        // height as a filled one; no magic dp guess needed.
        Text(
            course.name,
            fontFamily = DipiCondensed,
            fontSize = 17.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.2.sp,
            color = c.foreground,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            listOf(course.start, course.end).filter { it.isNotBlank() }.joinToString(" – "),
            color = c.muted,
            fontSize = 13.sp,
        )
        Text(
            if (first && days > 0) "STARTS IN $days DAYS" else "",
            color = c.accent,
            fontFamily = DipiCondensed,
            fontSize = 12.sp,
        )
        val matrix = course.matrix
        if (matrix != null) {
            CourseMatrixTable(matrix, modifier = Modifier.padding(top = 9.dp))
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
 * Frame 1a draws the six numeric columns at a fixed 54dp beside a flexing
 * label column. A phone-width card cannot spare 324dp, so the columns fall
 * back to the shipped proportion (0.75 of 6.1 weight units) and the 54dp cap
 * only bites on the tablet the frame was measured on.
 */
private const val MATRIX_CELL_FRACTION = 0.75f / 6.1f

/** Where frame 1a starts the subtotal bands and the trio gutter: below the group caps. */
private val MatrixCapsHeight = 15.dp

/**
 * The compact gender-split matrix (spec S4), redrawn to frame 1a: MALE and
 * FEMALE group caps over the two trios, 12sp mono column labels with the M
 * and F subtotals darker than the four new/old columns, a hairline gutter
 * between the trios, neutral bands behind the two subtotal columns, one row
 * per [cardRows] and an emphasised Total row carrying the sevak count as its
 * own mono suffix.
 *
 * [cardRows] rather than `highlights` (owner decision 2026-08-30): it always
 * yields the same three rows — an absent status renders as an empty row of
 * middots instead of dropping out — so every card in a grid row is the same
 * height. That in turn is what lets the upcoming pane go without a scroll.
 */
@Composable
private fun CourseMatrixTable(matrix: CourseMatrix, modifier: Modifier = Modifier) {
    val c = LocalDipi.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val cell = minOf(54.dp, maxWidth * MATRIX_CELL_FRACTION)
        val band = c.hover
        val gutter = c.hairline
        Column(
            Modifier.fillMaxWidth().drawBehind {
                val w = cell.toPx()
                val top = MatrixCapsHeight.toPx()
                val tall = size.height - top
                val radius = CornerRadius(3.dp.toPx())
                // Bands behind the two subtotal columns: F at the right edge,
                // M three columns in — the same 54dp the header labels use.
                drawRoundRect(band, Offset(size.width - w, top), Size(w, tall), radius)
                drawRoundRect(band, Offset(size.width - w * 4, top), Size(w, tall), radius)
                val hairline = 1.dp.toPx()
                drawRect(gutter, Offset(size.width - w * 3 - hairline, top), Size(hairline, tall))
            },
        ) {
            MatrixGroupCapsRow(cell)
            MatrixHeaderRow(cell)
            matrix.cardRows.forEach { row -> MatrixDataRow(row.label, row, cell, emphasise = false) }
            matrix.total?.let { total -> MatrixDataRow("Total", total, cell, emphasise = true) }
        }
    }
}

/** "MALE" and "FEMALE", each centred over its trio of columns. */
@Composable
private fun MatrixGroupCapsRow(cell: Dp) {
    val c = LocalDipi.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.weight(1f))
        listOf("MALE", "FEMALE").forEach { cap ->
            Text(
                cap,
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp,
                letterSpacing = 1.7.sp,
                color = c.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(cell * 3),
            )
        }
    }
}

/**
 * The column labels, built from the same per-cell widths as [MatrixDataRow]
 * so each sits directly above its column on a real device — a manually-spaced
 * literal string can't guarantee that. M and F read darker than the four
 * new/old columns: they are the subtotals the bands sit behind.
 */
@Composable
private fun MatrixHeaderRow(cell: Dp) {
    val c = LocalDipi.current
    val hairline = c.hairline
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawRect(hairline, Offset(0f, size.height - stroke), Size(size.width, stroke))
            }
            .padding(bottom = 5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Spacer(Modifier.weight(1f))
        listOf("NM" to false, "OM" to false, "M" to true, "NF" to false, "OF" to false, "F" to true)
            .forEach { (label, subtotal) ->
                Text(
                    label,
                    fontFamily = DipiMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = if (subtotal) c.foreground else c.muted,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(cell),
                )
            }
    }
}

@Composable
private fun MatrixDataRow(label: String, row: MatrixRow, cell: Dp, emphasise: Boolean) {
    val c = LocalDipi.current
    val hairline = c.hairline
    val sevak = row.sevakTotal
    val rowModifier = if (emphasise) {
        Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawRect(hairline, Offset(0f, 0f), Size(size.width, stroke))
            }
            .height(30.dp)
    } else {
        Modifier.fillMaxWidth().height(26.dp)
    }
    Row(rowModifier, verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                label,
                fontSize = if (emphasise) 14.sp else 13.5.sp,
                fontWeight = if (emphasise) FontWeight.Medium else FontWeight.Normal,
                color = if (emphasise) c.foreground else c.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (emphasise && sevak > 0) {
                Text(
                    "+$sevak sevak",
                    fontFamily = DipiMono,
                    fontSize = 11.5.sp,
                    color = c.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        listOf(row.newMale, row.oldMale, row.maleTotal, row.newFemale, row.oldFemale, row.femaleTotal)
            .forEach { n ->
                Text(
                    matrixCell(n),
                    fontFamily = DipiMono,
                    fontSize = if (emphasise) 15.sp else 14.5.sp,
                    fontWeight = if (emphasise) FontWeight.Medium else FontWeight.Normal,
                    color = c.foreground,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.width(cell),
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
