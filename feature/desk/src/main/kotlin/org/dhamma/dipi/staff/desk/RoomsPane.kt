package org.dhamma.dipi.staff.desk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.RoomLayout
import org.dhamma.dipi.staff.model.RoomSyncFailure
import org.dhamma.dipi.staff.ui.theme.DeskStyle
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.LocalDarkTheme
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import org.dhamma.dipi.staff.ui.theme.LocalDeskColors
import org.dhamma.dipi.staff.ui.theme.LocalIndustry
import org.dhamma.dipi.staff.ui.theme.deskCard

/**
 * The occupancy picture: who is where tonight, and what is free. The header
 * carries pull-from-server (always) and the bulk allocation sync (owner
 * amendment 2026-08-16): "Sync N to server" walks every unsynced checked-in
 * record through the desk's own update form — hidden at N=0. Both buttons
 * disable while either walk is in flight; per-row refusals list under the header.
 */
@Composable
fun RoomsPane(
    roll: List<ApplicantCard>,
    checkIns: Map<ApplicantId, CheckInRecord>,
    rooms: List<AccoRoom>,
    layout: RoomLayout = RoomLayout(),
    readOnly: Boolean = false,
    pendingSync: Int = 0,
    syncBusy: Boolean = false,
    pullBusy: Boolean = false,
    syncFailures: List<RoomSyncFailure> = emptyList(),
    onSyncRooms: () -> Unit = {},
    onPullRooms: () -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 24.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DeskH2("Room Chart")
                if (readOnly) DeskSub("Finalized course · Read only")
                AmenityLegend()
                if (syncFailures.isNotEmpty()) {
                    SyncRefusals(roll, syncFailures)
                }
            }
            val actionsEnabled = !pullBusy && !syncBusy
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoomPullButton(pullBusy, actionsEnabled, onPullRooms)
                if (pendingSync > 0 || syncBusy) {
                    RoomSyncButton(pendingSync, syncBusy, actionsEnabled && !readOnly, onSyncRooms)
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            OccupancyTypeLegend()
        }

        val occupantByRoom = roll.filter { card ->
            val rec = deskRecord(card, checkIns)
            rec?.checkedIn == true && rec.room.isNotBlank()
        }.groupBy { deskRecord(it, checkIns)!!.room }

        // Older courses may reference rooms removed from today's inventory.
        val chartRooms = rooms + if (readOnly) roll.filter {
            it.courseFinalized && it.status.normalize() == "attended" && it.historicalRoom.isNotBlank()
        }.map { student ->
            val code = student.historicalRoom
            AccoRoom(code, student.gender, code.substringBeforeLast(" "),
                number = code.substringAfterLast(" "))
        }.distinctBy { it.code }.filter { historical -> rooms.none { it.code == historical.code } }
        else emptyList()

        // Stacked full-width, one block per gender+section — matching RoomLayout's
        // own keying — inside the pane's single verticalScroll above. No side-by-side
        // columns, so each block's grid gets the pane's full width.
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            listOf(Gender.F to "Female", Gender.M to "Male").forEach { (gender, label) ->
                val genderRooms = chartRooms.filter { it.gender == gender }
                val sections = genderRooms.map { it.section }.distinct().ifEmpty { listOf("") }
                sections.forEach { section ->
                    val block = genderRooms.filter { it.section == section }
                    RoomBlock(
                        label = label,
                        section = section,
                        block = block,
                        columns = layout.columnsFor(gender, section),
                        occupantByRoom = occupantByRoom,
                        readOnly = readOnly,
                    )
                }
            }
        }
    }
}

/**
 * One gender+section block of the room chart: header with the block's own
 * "n rooms · n free" counts, then the grid at the block's own column count
 * (from `RoomLayout`, keyed `gender|section` — see S2 of the room-layout spec).
 */
@Composable
private fun RoomBlock(
    label: String,
    section: String,
    block: List<AccoRoom>,
    columns: Int,
    occupantByRoom: Map<String, List<ApplicantCard>>,
    readOnly: Boolean,
) {
    val industry = LocalIndustry.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val free = block.count { it.code !in occupantByRoom }
        val occupied = block.size - free
        Column(
            Modifier
                .fillMaxWidth()
                .bottomHairline(industry.neutral400)
                .padding(bottom = 7.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                if (section.isBlank()) label else "$label · $section",
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 22.sp,
                color = industry.text,
            )
            // The ratio is why the registrar opened this pane, so it leads at
            // the same weight as a Board stat — not as a 12sp grey sub-line.
            Text(
                if (readOnly) "$occupied assigned · $free unassigned of ${block.size}" else "$occupied occupied · $free free of ${block.size}",
                fontFamily = DipiMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 21.sp,
                lineHeight = 22.sp,
                color = industry.text,
                modifier = Modifier.testTag("room-block-ratio"),
            )
            OccupancyBar(occupied, block.size)
        }
        // Chart bands like the paper ROOM CHART: `columns` cells a row (from the
        // Centre Settings room-chart layout), alternate rows on a soft rounded
        // band of the neutral ground.
        block.chunked(columns).forEachIndexed { i, rowRooms ->
            val rowOccupied = rowRooms.any { occupantByRoom[it.code].orEmpty().isNotEmpty() }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max)
                    .clip(DeskStyle.tileShape)
                    .background(if (i % 2 == 1) industry.neutral100 else Color.Transparent),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                rowRooms.forEach { room ->
                    val who = occupantByRoom[room.code]
                    RoomCell(room, who, compactRow = !rowOccupied, Modifier.weight(1f).fillMaxHeight())
                }
                repeat(columns - rowRooms.size) { Spacer(Modifier.weight(1f).fillMaxHeight()) }
            }
        }
        if (block.isEmpty()) {
            DeskEmpty(
                "No rooms configured on the desk site yet.",
                Modifier.fillMaxWidth().padding(vertical = 20.dp),
            )
        }
    }
}

/**
 * Outline "PULL FROM SERVER" — card fill, accent label. Busy → "PULLING…".
 * Always shown; disabled while either pull or sync is in flight.
 */
@Composable
private fun RoomPullButton(busy: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val industry = LocalIndustry.current
    Text(
        (if (busy) "Pulling…" else "Pull from server").uppercase(),
        fontFamily = DipiCondensed,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.06.em,
        maxLines = 1,
        color = if (enabled) industry.accent else industry.neutral600,
        modifier = Modifier
            .deskCard(
                shape = DeskStyle.controlShape,
                fill = DeskStyle.cardFill,
                border = if (enabled) industry.accent else industry.neutral400,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp),
    )
}

/**
 * The one deliberate accent fill on this pane: "SYNC N TO SERVER", busy →
 * "SYNCING…" and inert. Callers hide it entirely at N=0.
 */
@Composable
private fun RoomSyncButton(pending: Int, busy: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val industry = LocalIndustry.current
    val dark = LocalDarkTheme.current
    val dipi = LocalDipi.current
    Text(
        (if (busy) "Syncing…" else "Sync $pending to server").uppercase(),
        fontFamily = DipiCondensed,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.06.em,
        maxLines = 1,
        color = Color.White,
        modifier = Modifier
            .deskCard(
                shape = DeskStyle.controlShape,
                fill = if (!enabled) industry.neutral400 else if (busy && dark) dipi.accentPressed else if (busy) industry.accent700 else industry.accent,
                border = if (busy && dark) dipi.accentPressed else if (busy) industry.accent700 else industry.accent,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp),
    )
}

/** Per-row refusals from the last sync run: name + the server's reason, verbatim. */
@Composable
private fun SyncRefusals(roll: List<ApplicantCard>, failures: List<RoomSyncFailure>) {
    val industry = LocalIndustry.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .deskCard(border = industry.accent, elevation = 0.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "SERVER REFUSED ${failures.size}",
            fontFamily = DipiMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            letterSpacing = 0.1.em,
            color = industry.accent700,
        )
        failures.forEach { failure ->
            val name = roll.firstOrNull { it.id == failure.id }?.displayName
                ?: "Applicant ${failure.id.value}"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = industry.text,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    failure.reason,
                    fontSize = 12.sp,
                    color = industry.neutral600,
                )
            }
        }
    }
}

/**
 * Occupied cells keep the accent fill. Old is a solid accent border; New is
 * the same stroke, short-dashed. Age is the number only — muted 12sp — at
 * the reserved top-right corner. A row with no allocated rooms uses the
 * compact height (room number only); width stays the column weight. Empty
 * cells stay the near-white hairline they were.
 */
@Composable
private fun RoomCell(
    room: AccoRoom,
    occupant: List<ApplicantCard>?,
    compactRow: Boolean,
    modifier: Modifier = Modifier,
) {
    val industry = LocalIndustry.current
    val dark = LocalDarkTheme.current
    val dipi = LocalDipi.current
    val who = occupant.orEmpty()
    val taken = who.isNotEmpty()
    val isNew = taken && who.any { !it.oldStudent }
    val ages = who.mapNotNull { it.age }
    val hasAge = ages.isNotEmpty()
    Box(
        modifier
            .heightIn(min = if (compactRow) RoomCellCompactHeight else RoomCellMinHeight)
            .fillMaxHeight()
            .then(
                if (taken) {
                    Modifier.roomChartOutline(industry.accent100, industry.accent, dashed = isNew)
                } else {
                    Modifier.deskCard(
                        shape = DeskStyle.tileShape,
                        fill = LocalDeskColors.current.subtleSurface,
                        border = if (dark) dipi.hairline else FreeCellHairline,
                        elevation = 0.dp,
                    )
                },
            )
            .semantics {
                contentDescription = when {
                    !taken -> "Available room"
                    isNew -> "New student room"
                    else -> "Old student room"
                }
            }
            .testTag(if (taken) "room-cell-occupied" else "room-cell-free"),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 11.dp, top = 8.dp, end = 11.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(end = if (hasAge) AgeReserveEnd else 0.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    room.displayNo,
                    fontFamily = DipiCondensed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    lineHeight = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (taken) industry.accent800 else industry.neutral400,
                )
                if (room.amenityMark.isNotBlank()) {
                    Text(
                        room.amenityMark,
                        fontFamily = DipiMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 9.5.sp,
                        lineHeight = 12.sp,
                        letterSpacing = 0.1.em,
                        color = if (taken) industry.accent500 else industry.neutral300,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
            if (!compactRow) {
                Column(
                    Modifier.padding(
                        end = if (hasAge) AgeReserveEnd else 0.dp,
                        top = if (hasAge) AgeReserveTop else 0.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    who.forEach { student ->
                        Text(
                            student.displayName,
                            fontSize = 13.sp,
                            lineHeight = 15.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = industry.neutral700,
                        )
                    }
                }
            }
        }
        if (hasAge) {
            AgeCorner(ages)
        }
    }
}

@Composable
private fun BoxScope.AgeCorner(ages: List<Int>) {
    val industry = LocalIndustry.current
    Column(
        Modifier
            .align(Alignment.TopEnd)
            .padding(AgeEdgePad)
            .testTag("room-cell-age-top"),
        horizontalAlignment = Alignment.End,
    ) {
        ages.forEach { years ->
            Text(
                years.toString(),
                fontSize = 12.sp,
                lineHeight = 14.sp,
                color = industry.neutral600,
            )
        }
    }
}

/** Solid or short-dashed accent outline — same colour and thickness. */
private fun Modifier.roomChartOutline(
    fill: Color,
    border: Color,
    dashed: Boolean,
    corner: Dp = DeskStyle.tileRadius,
): Modifier = this
    .background(fill, RoundedCornerShape(corner))
    .clip(RoundedCornerShape(corner))
    .drawWithContent {
        drawContent()
        val strokeWidth = RoomChartStroke.toPx()
        val inset = strokeWidth / 2f
        drawRoundRect(
            color = border,
            topLeft = Offset(inset, inset),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            cornerRadius = CornerRadius((corner.toPx() - inset).coerceAtLeast(0f)),
            style = Stroke(
                width = strokeWidth,
                pathEffect = if (dashed) {
                    PathEffect.dashPathEffect(
                        floatArrayOf(RoomChartDashOn.toPx(), RoomChartDashOff.toPx()),
                        0f,
                    )
                } else {
                    null
                },
            ),
        )
    }

/** The nearly-invisible hairline a free cell carries instead of a card border. */
private val FreeCellHairline = Color(0xFFEDEDF1)

/** Occupied Old/New share this stroke so the dash is the only difference. */
private val RoomChartStroke = 1.5.dp

/** Short dashes — visible on the Pixel C, not a dotted hairline. */
private val RoomChartDashOn = 6.dp
private val RoomChartDashOff = 4.dp

private val RoomCellMinHeight = 96.dp
/** Room number only — used when every cell in the row is empty. Width unchanged. */
private val RoomCellCompactHeight = 42.dp
private val AgeEdgePad = 8.dp
private val AgeReserveEnd = 28.dp
private val AgeReserveTop = 22.dp

/** Border samples once above the grid: solid Old, short-dashed New, faint Available. */
@Composable
private fun OccupancyTypeLegend() {
    val industry = LocalIndustry.current
    Row(
        Modifier.testTag("room-chart-type-legend"),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TypeLegendItem("Old", fill = industry.accent100, border = industry.accent, dashed = false)
        TypeLegendItem("New", fill = industry.accent100, border = industry.accent, dashed = true)
        TypeLegendItem("Available", fill = LocalDeskColors.current.subtleSurface, border = FreeCellHairline, dashed = false)
    }
}

@Composable
private fun TypeLegendItem(label: String, fill: Color, border: Color, dashed: Boolean) {
    val industry = LocalIndustry.current
    val dark = LocalDarkTheme.current
    val resolvedBorder = if (border == FreeCellHairline && dark) LocalDipi.current.hairline else border
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(14.dp)
                .then(
                    if (border == FreeCellHairline) {
                        Modifier
                            .background(fill, RoundedCornerShape(3.dp))
                            .border(1.dp, resolvedBorder, RoundedCornerShape(3.dp))
                    } else {
                        Modifier.roomChartOutline(fill, border, dashed = dashed, corner = 3.dp)
                    },
                ),
        )
        Text(label, fontSize = 12.sp, color = industry.neutral600)
    }
}

/**
 * The block's occupancy as a 6dp bar, capped at 280dp so a 60-room block and
 * a 12-room block read at the same scale.
 */
@Composable
private fun OccupancyBar(occupied: Int, total: Int) {
    val industry = LocalIndustry.current
    if (total <= 0) return
    val fraction = (occupied.toFloat() / total).coerceIn(0f, 1f)
    Box(
        Modifier
            .width(280.dp)
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(industry.neutral200)
            .testTag("room-occupancy-bar"),
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(industry.accent400),
            )
        }
    }
}

/** `G` geyser · `IC` Indian · `W` western — the marks the cells now carry alone. */
@Composable
private fun AmenityLegend() {
    val industry = LocalIndustry.current
    Row(
        Modifier.padding(top = 2.dp).testTag("room-amenity-legend"),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf("G" to "geyser", "IC" to "Indian toilet", "W" to "western").forEach { (mark, meaning) ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    mark,
                    fontFamily = DipiMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 9.5.sp,
                    letterSpacing = 0.1.em,
                    color = industry.accent500,
                )
                Text(meaning, fontSize = 11.5.sp, color = industry.neutral500)
            }
        }
    }
}

// Centre settings moved off the desk: the global CentreOpsScreen opens from
// the Centre screen, so the desk rail carries six sections and no
// CentreSettingsPane any more.
