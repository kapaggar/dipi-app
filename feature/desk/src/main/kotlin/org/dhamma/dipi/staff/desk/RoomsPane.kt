package org.dhamma.dipi.staff.desk

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
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
import org.dhamma.dipi.staff.ui.theme.ThemeIndustry as Industry
import org.dhamma.dipi.staff.ui.theme.deskCard

/**
 * The occupancy picture: who is where tonight, and what is free. The header
 * carries pull-from-server (always) and the bulk allocation sync (owner
 * amendment 2026-08-16): "Sync N to server" walks every unsynced checked-in
 * record through the desk's own update form — hidden at N=0. Both buttons
 * disable while either walk is in flight; per-row refusals list under the header.
 */
@OptIn(ExperimentalLayoutApi::class)
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
    selectedBlock: RoomBlockKey? = null,
    onSelectBlock: (RoomBlockKey) -> Unit = {},
    focusedCode: String? = null,
    jumpError: String? = null,
    onJump: (String, List<AccoRoom>) -> Unit = { _, _ -> },
    onFocusRoom: (String?) -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        FlowRow(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.width(350.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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

        val chartRooms = rooms + if (readOnly) roll.filter {
            it.courseFinalized && it.status.normalize() == "attended" && it.historicalRoom.isNotBlank()
        }.map { student ->
            val code = student.historicalRoom
            AccoRoom(code, student.gender, code.substringBeforeLast(" "),
                number = code.substringAfterLast(" "))
        }.distinctBy { it.code }.filter { historical -> rooms.none { it.code == historical.code } }
        else emptyList()

        val blockKeys = listOf(Gender.F, Gender.M).flatMap { gender ->
            chartRooms.filter { it.gender == gender }.map { it.section }.distinct()
                .map { RoomBlockKey(gender, it) }
        }
        val activeBlock = selectedBlock?.takeIf { it in blockKeys } ?: blockKeys.firstOrNull()
        val activeRooms = chartRooms.filter { it.gender == activeBlock?.gender && it.section == activeBlock.section }
        val focusManager = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        val finishRoomNavigation = { focusManager.clearFocus(); keyboard?.hide(); Unit }
        var jumpQuery by remember { mutableStateOf("") }
        var jumpRequest by remember { mutableStateOf(0) }
        val requestJump = {
            if (resolveRoomJump(activeRooms, jumpQuery) != null) {
                finishRoomNavigation()
                jumpRequest++
            }
            onJump(jumpQuery, activeRooms)
        }
        if (blockKeys.isNotEmpty()) {
            FlowRow(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                blockKeys.forEach { key ->
                    val on = key == activeBlock
                    Text(
                        "${key.section} · ${if (key.gender == Gender.M) "Male" else "Female"}",
                        fontSize = 13.sp,
                        color = if (on) Color.White else Industry.neutral700,
                        modifier = Modifier
                            .deskCard(
                                shape = DeskStyle.controlShape,
                                fill = if (on) Industry.accent else DeskStyle.cardFill,
                                border = if (on) Industry.accent else DeskStyle.cardBorder,
                                elevation = 0.dp,
                            )
                            .clickable { onSelectBlock(key) }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                            .testTag("room-block-${key.gender.name}-${key.section}"),
                    )
                }
            }
            FlowRow(
                Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextField(
                    value = jumpQuery,
                    onValueChange = { jumpQuery = it },
                    singleLine = true,
                    label = { Text("Jump to room") },
                    keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { requestJump() }),
                    modifier = Modifier.width(180.dp).testTag("room-jump-field"),
                )
                Text(
                    "Jump",
                    modifier = Modifier
                        .clickable { requestJump() }
                        .heightIn(min = 48.dp)
                        .padding(12.dp)
                        .testTag("room-jump"),
                    color = Industry.accent800,
                )
                val next = nextOccupiedRoomCode(activeRooms.map { it.code }, occupantByRoom.keys, focusedCode)
                Text(
                    "Next occupied",
                    modifier = Modifier
                        .clickable(enabled = next != null) {
                            next?.let { code ->
                                finishRoomNavigation()
                                jumpRequest++
                                onFocusRoom(code)
                            }
                        }
                        .heightIn(min = 48.dp)
                        .padding(12.dp)
                        .testTag("room-next-occupied"),
                    color = if (next != null) Industry.accent800 else Industry.neutral400,
                )
            }
            if (!jumpError.isNullOrBlank()) {
                Text(jumpError, fontSize = 12.5.sp, color = Industry.accent800, modifier = Modifier.padding(bottom = 8.dp))
            }
        }

        activeBlock?.let { block ->
            RoomBlockSummary(if (block.gender == Gender.M) "Male" else "Female", block.section, activeRooms, occupantByRoom, readOnly)
        }
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).testTag("room-grid")) {
            activeBlock?.let { block ->
                RoomBlock(
                    label = if (block.gender == Gender.M) "Male" else "Female",
                    section = block.section,
                    block = activeRooms,
                    columns = layout.columnsFor(block.gender, block.section),
                    occupantByRoom = occupantByRoom,
                    readOnly = readOnly,
                    focusedCode = focusedCode,
                    jumpRequest = jumpRequest,
                )
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
    focusedCode: String? = null,
    jumpRequest: Int = 0,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    .background(if (i % 2 == 1) Industry.neutral100 else Color.Transparent),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                rowRooms.forEach { room ->
                    val who = occupantByRoom[room.code]
                    RoomCell(room, who, compactRow = !rowOccupied, Modifier.weight(1f).fillMaxHeight(), focused = room.code == focusedCode, jumpRequest = jumpRequest)
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

@Composable
private fun RoomBlockSummary(label: String, section: String, block: List<AccoRoom>, occupantByRoom: Map<String, List<ApplicantCard>>, readOnly: Boolean) {
        val free = block.count { it.code !in occupantByRoom }
        val occupied = block.size - free
        Column(
            Modifier
                .fillMaxWidth()
                .bottomHairline(Industry.neutral400)
                .padding(bottom = 7.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                if (section.isBlank()) label else "$label · $section",
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 22.sp,
                color = Industry.text,
            )
            // The ratio is why the registrar opened this pane, so it leads at
            // the same weight as a Board stat — not as a 12sp grey sub-line.
            Text(
                if (readOnly) "$occupied assigned · $free unassigned of ${block.size}" else "$occupied occupied · $free free of ${block.size}",
                fontFamily = DipiMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 21.sp,
                lineHeight = 22.sp,
                color = Industry.text,
                modifier = Modifier.testTag("room-block-ratio"),
            )
            OccupancyBar(occupied, block.size)
        }

}

/**
 * Outline "PULL FROM SERVER" — card fill, accent label. Busy → "PULLING…".
 * Always shown; disabled while either pull or sync is in flight.
 */
@Composable
private fun RoomPullButton(busy: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Text(
        (if (busy) "Pulling…" else "Pull from server").uppercase(),
        fontFamily = DipiCondensed,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.06.em,
        maxLines = 1,
        color = if (enabled) Industry.accent else Industry.neutral600,
        modifier = Modifier
            .deskCard(
                shape = DeskStyle.controlShape,
                fill = DeskStyle.cardFill,
                border = if (enabled) Industry.accent else Industry.neutral400,
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
                fill = if (!enabled) Industry.neutral400 else if (busy) Industry.accent700 else Industry.accent,
                border = if (busy) Industry.accent700 else Industry.accent,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp),
    )
}

/** Per-row refusals from the last sync run: name + the server's reason, verbatim. */
@Composable
private fun SyncRefusals(roll: List<ApplicantCard>, failures: List<RoomSyncFailure>) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .heightIn(max = 100.dp)
            .verticalScroll(rememberScrollState())
            .deskCard(border = Industry.accent, elevation = 0.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "SERVER REFUSED ${failures.size}",
            fontFamily = DipiMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            letterSpacing = 0.1.em,
            color = Industry.accent700,
        )
        failures.forEach { failure ->
            val name = roll.firstOrNull { it.id == failure.id }?.displayName
                ?: "Applicant ${failure.id.value}"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Industry.text,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    failure.reason,
                    fontSize = 12.sp,
                    color = Industry.neutral600,
                )
            }
        }
    }
}

/**
 * Occupied cells keep the accent fill. Old is a solid accent border; New is
 * the same stroke, short-dashed. Age is the number only — muted 12sp — at
 * the reserved top-right corner. On a tall allocated cell the meditator
 * name is 17sp Medium so it reads against the 19sp room number. A row with
 * no allocated rooms uses the compact height (room number only); width
 * stays the column weight. Empty cells stay the near-white hairline they
 * were. Gender/block headings stay 22sp Bold condensed.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoomCell(
    room: AccoRoom,
    occupant: List<ApplicantCard>?,
    compactRow: Boolean,
    modifier: Modifier = Modifier,
    focused: Boolean = false,
    jumpRequest: Int = 0,
) {
    val bringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(focused, jumpRequest) { if (focused) bringIntoView.bringIntoView() }
    val who = occupant.orEmpty()
    val taken = who.isNotEmpty()
    val isNew = taken && who.any { !it.oldStudent }
    val ages = who.mapNotNull { it.age }
    val hasAge = ages.isNotEmpty()
    Box(
        modifier
            .bringIntoViewRequester(bringIntoView)
            .heightIn(min = if (compactRow) RoomCellCompactHeight else RoomCellMinHeight)
            .fillMaxHeight()
            .then(if (focused) Modifier.border(2.dp, Industry.accent, DeskStyle.tileShape) else Modifier)
            .then(
                if (taken) {
                    Modifier.roomChartOutline(Industry.accent100, Industry.accent, dashed = isNew)
                } else {
                    Modifier.deskCard(
                        shape = DeskStyle.tileShape,
                        fill = FreeCellFill,
                        border = FreeCellHairline,
                        elevation = 0.dp,
                    )
                },
            )
            .semantics {
                selected = focused
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
                    modifier = Modifier.testTag("room-code-${room.code}"),
                    fontFamily = DipiCondensed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    lineHeight = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (taken) Industry.accent800 else Industry.neutral400,
                )
                if (room.amenityMark.isNotBlank()) {
                    Text(
                        room.amenityMark,
                        fontFamily = DipiMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 9.5.sp,
                        lineHeight = 12.sp,
                        letterSpacing = 0.1.em,
                        color = if (taken) Industry.accent500 else Industry.neutral300,
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
                            fontSize = 17.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Industry.neutral700,
                            modifier = Modifier.testTag("room-cell-name"),
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
                color = Industry.neutral600,
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

/** Near-white ground for a free cell — emptiness reads as absence of ink. */
private val FreeCellFill: Color
    @Composable get() = Industry.card

/** The nearly-invisible hairline a free cell carries instead of a card border. */
private val FreeCellHairline: Color
    @Composable get() = Industry.neutral200

/** Occupied Old/New share this stroke so the dash is the only difference. */
private val RoomChartStroke = 1.5.dp

/** Short dashes — visible on the Pixel C, not a dotted hairline. */
private val RoomChartDashOn = 6.dp
private val RoomChartDashOff = 4.dp

private val RoomCellMinHeight = 104.dp
/** Room number only — used when every cell in the row is empty. Width unchanged. */
private val RoomCellCompactHeight = 44.dp
private val AgeEdgePad = 8.dp
private val AgeReserveEnd = 28.dp
private val AgeReserveTop = 22.dp

/** Border samples once above the grid: solid Old, short-dashed New, faint Available. */
@Composable
private fun OccupancyTypeLegend() {
    Row(
        Modifier.testTag("room-chart-type-legend"),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TypeLegendItem("Old", fill = Industry.accent100, border = Industry.accent, dashed = false)
        TypeLegendItem("New", fill = Industry.accent100, border = Industry.accent, dashed = true)
        TypeLegendItem("Available", fill = FreeCellFill, border = FreeCellHairline, dashed = false)
    }
}

@Composable
private fun TypeLegendItem(label: String, fill: Color, border: Color, dashed: Boolean) {
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
                            .border(1.dp, border, RoundedCornerShape(3.dp))
                    } else {
                        Modifier.roomChartOutline(fill, border, dashed = dashed, corner = 3.dp)
                    },
                ),
        )
        Text(label, fontSize = 12.sp, color = Industry.neutral600)
    }
}

/**
 * The block's occupancy as a 6dp bar, capped at 280dp so a 60-room block and
 * a 12-room block read at the same scale.
 */
@Composable
private fun OccupancyBar(occupied: Int, total: Int) {
    if (total <= 0) return
    val fraction = (occupied.toFloat() / total).coerceIn(0f, 1f)
    Box(
        Modifier
            .width(280.dp)
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Industry.neutral200)
            .testTag("room-occupancy-bar"),
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(Industry.accent400),
            )
        }
    }
}

/** `G` geyser · `IC` Indian · `W` western — the marks the cells now carry alone. */
@Composable
private fun AmenityLegend() {
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
                    color = Industry.accent500,
                )
                Text(meaning, fontSize = 11.5.sp, color = Industry.neutral500)
            }
        }
    }
}

// Centre settings moved off the desk: the global CentreOpsScreen opens from
// the Centre screen, so the desk rail carries six sections and no
// CentreSettingsPane any more.
