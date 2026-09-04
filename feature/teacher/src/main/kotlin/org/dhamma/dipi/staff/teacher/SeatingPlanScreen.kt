package org.dhamma.dipi.staff.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.HallCell
import org.dhamma.dipi.staff.model.HallGrid
import org.dhamma.dipi.staff.model.HallPlan
import org.dhamma.dipi.staff.model.PlacedSeat
import org.dhamma.dipi.staff.model.RollRow
import org.dhamma.dipi.staff.model.TeacherRoll
import org.dhamma.dipi.staff.model.UnseatedRow
import org.dhamma.dipi.staff.model.hallLayout
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.Industry

// Fixed hexes DESIGN.md § Course ops names outside the ramp tokens.
private val NewFill = Color(0xFFFAFAFB)
private val Rule = Color(0xFFE0E0E3)
private val UnseatedText = Color(0xFF424244)

private val CellShape = RoundedCornerShape(5.dp)

/**
 * Frame 2c + the r2 orientation ruling (the live web seating page OVERRIDES
 * the frame's guessed geometry) — the seating plan, derived purely from the
 * ONE roll response the teacher-list fetch produced. Fully prop-driven: hall
 * switching flips [hall] client-side and NEVER refetches (the endpoint
 * mutates server data on GET). Read-only — no drag, no reseat; a seat tap
 * opens the same student card as the list row (two doors, one record).
 *
 * r2 geometry: letters are COLUMNS, numbers are DEPTH — depth renders
 * DESCENDING so row 1 sits at the bottom, directly above the column-letter
 * axis and the TEACHER · DHAMMA SEAT marker. Nothing is drawn above the
 * grid. Below 1000dp width the chowky/chair rail leaves the side and renders
 * full-width below the grid, above UNSEATED.
 *
 * Ground-truth corrections applied: Old cells fill accent100 on accent300
 * (the legend-swatch hexes in the prose were the frame's error); the
 * chowky/chair rail draws OCCUPIED cells only — empty CW-/CH- slots are
 * unknowable client-side and are not drawn.
 */
@Composable
fun SeatingPlanScreen(
    roll: TeacherRoll,
    hall: Gender = Gender.M,
    gridFor: (Gender) -> HallGrid = { HallGrid() },
    onView: (TeacherView) -> Unit = {},
    onHall: (Gender) -> Unit = {},
    onOpen: (RollRow) -> Unit = {},
    onSettings: () -> Unit = {},
) {
    val grid = gridFor(hall)
    val plan = remember(roll, hall, grid) {
        hallLayout(roll.groups.filter { it.gender == hall }, grid)
    }
    Column(Modifier.fillMaxSize().background(Industry.bg)) {
        Header(hall, plan, onView, onSettings)
        HallBody(roll, hall, gridFor, onHall, onOpen)
    }
}

/**
 * The r2 hall itself — gender tabs, legend, 66dp grid, chowky/chair rail,
 * unseated. Shared by Course ops and the Board's native 5h seating sheet
 * so there is one hall geometry.
 */
@Composable
fun HallBody(
    roll: TeacherRoll,
    hall: Gender = Gender.M,
    gridFor: (Gender) -> HallGrid = { HallGrid() },
    onHall: (Gender) -> Unit = {},
    onOpen: (RollRow) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val grid = gridFor(hall)
    val plan = remember(roll, hall, grid) {
        hallLayout(roll.groups.filter { it.gender == hall }, grid)
    }
    Column(modifier.fillMaxSize().background(Industry.bg).testTag("hall-body")) {
        HallAndLegendBand(hall, onHall)
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val sideRail = maxWidth >= 1000.dp
            val visibleUnseated = plan.unseatedVisible
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            ) {
                Spacer(Modifier.height(2.dp))
                if (sideRail) {
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            SeatGrid(plan, onOpen)
                            if (visibleUnseated.isNotEmpty()) UnseatedSection(visibleUnseated)
                        }
                        if (plan.chowkyChair.isNotEmpty()) {
                            Spacer(Modifier.width(18.dp))
                            ChowkyChairSection(plan.chowkyChair, perRow = 2, Modifier.width(280.dp), onOpen)
                        }
                    }
                } else {
                    SeatGrid(plan, onOpen)
                    if (plan.chowkyChair.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        ChowkyChairSection(plan.chowkyChair, perRow = plan.columns, Modifier.fillMaxWidth(), onOpen)
                    }
                    if (visibleUnseated.isNotEmpty()) UnseatedSection(visibleUnseated)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private fun hallWord(hall: Gender) = if (hall == Gender.F) "Female" else "Male"

/** 62dp header — title, hall + tally sub-line, the destination pair with Seating selected, ⚙. */
@Composable
private fun Header(
    hall: Gender,
    plan: HallPlan,
    onView: (TeacherView) -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Seating plan",
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 23.sp,
                letterSpacing = 0.2.sp,
                color = Industry.text,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${hallWord(hall)} hall · facing the front · ${plan.oldCount} old, ${plan.newCount} new",
                fontSize = 13.sp,
                color = Industry.neutral600,
                maxLines = 1,
            )
        }
        SeatingDestinationButton("Seniority", selected = false) { onView(TeacherView.SENIORITY) }
        Spacer(Modifier.width(8.dp))
        SeatingDestinationButton("Seating plan", selected = true) { onView(TeacherView.SEATING) }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(48.dp)
                .clickable(onClick = onSettings, role = Role.Button)
                .testTag("seating-settings"),
            contentAlignment = Alignment.Center,
        ) {
            Text("⚙", fontSize = 18.sp, color = Industry.neutral600)
        }
    }
}

/** Same 48dp destination pair as the teacher list — a two-way switch over one response. */
@Composable
private fun SeatingDestinationButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        Modifier
            .height(48.dp)
            .then(
                if (selected) {
                    Modifier.background(Color.White, shape).border(1.5.dp, Industry.accent, shape)
                } else {
                    Modifier.border(1.dp, Industry.neutral300, shape)
                },
            )
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) Industry.accent800 else Industry.neutral600,
        )
    }
}

/** 40dp band: Male/Female 32dp pills left, the three-swatch legend right. */
@Composable
private fun HallAndLegendBand(hall: Gender, onHall: (Gender) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(Gender.M, Gender.F).forEach { g ->
            val selected = g == hall
            val shape = RoundedCornerShape(16.dp)
            Row(
                Modifier
                    .height(32.dp)
                    .then(
                        if (selected) {
                            Modifier.background(Industry.accent800, shape)
                        } else {
                            Modifier.border(1.dp, Industry.neutral300, shape)
                        },
                    )
                    .clickable(role = Role.Button) { onHall(g) }
                    .padding(horizontal = 16.dp)
                    .testTag("hall-tab-${g.name}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    hallWord(g),
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    color = if (selected) Color.White else Industry.neutral600,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        // Legend swatches carry the CELL fills (ground-truth correction):
        // Old accent100/accent300 — not the prose's swatch hexes.
        LegendItem("Old", fill = Industry.accent100, borderColor = Industry.accent300)
        Spacer(Modifier.width(14.dp))
        LegendItem("New", fill = NewFill, borderColor = Industry.neutral300)
        Spacer(Modifier.width(14.dp))
        LegendItem("Empty", fill = Color.White, borderColor = Industry.neutral400, dashed = true)
    }
}

@Composable
private fun LegendItem(label: String, fill: Color, borderColor: Color, dashed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(12.dp)
                .background(fill, RoundedCornerShape(2.dp))
                .then(
                    if (dashed) {
                        Modifier.dashedBorder(borderColor, 2.dp)
                    } else {
                        Modifier.border(1.dp, borderColor, RoundedCornerShape(2.dp))
                    },
                ),
        )
        Text(label, fontSize = 12.sp, color = Industry.neutral600)
    }
}

/**
 * 30dp TEACHER · DHAMMA SEAT marker — drawn once, at the BOTTOM of the grid
 * (r2 ruling: students face the dhamma seat, so the plan is never read
 * upside-down).
 */
@Composable
private fun TeacherMarker() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(Industry.neutral200, RoundedCornerShape(4.dp))
            .testTag("teacher-marker"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "TEACHER · DHAMMA SEAT",
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            letterSpacing = 2.4.sp,
            color = Industry.neutral600,
        )
    }
}

/** The 20dp column-letter axis row, one centred letter per grid column, at the bottom of the grid. */
@Composable
private fun ColumnAxisRow(letters: List<String>) {
    Row(
        Modifier.fillMaxWidth().height(20.dp).testTag("column-axis"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        letters.forEach { letter ->
            Text(
                letter,
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Industry.neutral500,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * One row per depth, DESCENDING — the deepest row first, depth row 1 at the
 * bottom, directly above the column-letter axis and the teacher marker.
 * 8dp gaps; nothing above the grid.
 */
@Composable
private fun SeatGrid(plan: HallPlan, onOpen: (RollRow) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        plan.cells.indices.reversed().forEach { d ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                plan.cells[d].forEach { cell -> SeatCellBox(cell, Modifier.weight(1f), onOpen) }
            }
        }
        ColumnAxisRow(plan.columnLetters)
        TeacherMarker()
    }
}

/**
 * 66dp seat cell (r2 S3 — owner feedback wins over frame 2c's 58dp) — id
 * top, up-to-two name lines that ELLIPSIZE, never a shorn glyph row. Old
 * fills accent100 on accent300; new #FAFAFB on neutral300; empty white on a
 * 1dp DASHED neutral400, no name.
 */
@Composable
private fun SeatCellBox(cell: HallCell, modifier: Modifier, onOpen: (RollRow) -> Unit) {
    val seated = cell.seated
    val kind = when {
        seated == null -> "empty"
        seated.old -> "old"
        else -> "new"
    }
    Column(
        modifier
            .height(66.dp)
            .then(
                when {
                    seated == null -> Modifier
                        .background(Color.White, CellShape)
                        .dashedBorder(Industry.neutral400, 5.dp)
                    seated.old -> Modifier
                        .background(Industry.accent100, CellShape)
                        .border(1.dp, Industry.accent300, CellShape)
                    else -> Modifier
                        .background(NewFill, CellShape)
                        .border(1.dp, Industry.neutral300, CellShape)
                },
            )
            .then(if (seated != null) Modifier.clickable { onOpen(seated.row) } else Modifier)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("seat-cell-${cell.id}-$kind"),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            cell.id,
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            letterSpacing = 0.8.sp,
            color = Industry.accent600,
            maxLines = 1,
        )
        if (seated != null) {
            Text(
                seated.row.name,
                fontSize = 12.5.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Industry.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The chowky/chair rail: `CW-` chowkies (low seats) and `CH-` chairs are
 * hall positions outside the web page's row grid — NOT pagoda cells (the
 * pagoda is a separate building; its cells are a future feature). Occupied
 * only, on the accent tint, in trailing-number-DESCENDING order so `…-1`
 * ends nearest the teacher. ≥1000dp it is the fixed 280dp side column
 * ([perRow] = 2); below that it renders full-width below the grid,
 * [perRow] = the grid's columns.
 */
@Composable
private fun ChowkyChairSection(
    seats: List<PlacedSeat>,
    perRow: Int,
    modifier: Modifier,
    onOpen: (RollRow) -> Unit,
) {
    Column(modifier.testTag("chowky-chair-section")) {
        Text(
            "CHOWKY / CHAIR",
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            letterSpacing = 1.7.sp,
            color = Industry.neutral500,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        seats.chunked(perRow.coerceAtLeast(1)).forEach { line ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                line.forEach { seat ->
                    Column(
                        Modifier
                            .weight(1f)
                            .height(66.dp)
                            .background(Industry.accent100, CellShape)
                            .border(1.dp, Industry.accent300, CellShape)
                            .clickable { onOpen(seat.row) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("chowky-seat-${seat.row.seat.trim()}"),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            seat.row.seat.trim(),
                            fontFamily = DipiMono,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                            color = Industry.accent600,
                            maxLines = 1,
                        )
                        Text(
                            seat.row.name,
                            fontSize = 12.5.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Industry.text,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                repeat(perRow.coerceAtLeast(1) - line.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * UNSEATED — rows with an empty seat cell land here with their reason,
 * never dropped. Callers pass [HallPlan.unseatedVisible]: sevaks sit on
 * cushions the plan does not draw, so they hide from the list (r2 S4) while
 * staying in the header tally.
 */
@Composable
private fun UnseatedSection(unseated: List<UnseatedRow>) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .drawBehind {
                drawLine(Rule, Offset(0f, 0.5.dp.toPx()), Offset(size.width, 0.5.dp.toPx()), 1.dp.toPx())
            }
            .padding(top = 11.dp),
    ) {
        Text(
            "UNSEATED",
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            letterSpacing = 1.7.sp,
            color = Industry.neutral500,
            modifier = Modifier.padding(bottom = 9.dp),
        )
        unseated.forEach { u ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .height(34.dp)
                    .background(NewFill, CellShape)
                    .border(1.dp, Rule, CellShape)
                    .padding(horizontal = 10.dp)
                    .testTag("unseated-row"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    u.row.name,
                    fontSize = 13.sp,
                    color = UnseatedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    u.reason,
                    fontFamily = DipiMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                    color = Industry.neutral500,
                )
            }
        }
    }
}

/** 1dp dashed border — Compose's border() has no dash, so the empty cell draws its own. */
private fun Modifier.dashedBorder(color: Color, cornerDp: androidx.compose.ui.unit.Dp): Modifier = drawBehind {
    val stroke = Stroke(
        width = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()), 0f),
    )
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(cornerDp.toPx()),
        style = stroke,
    )
}
