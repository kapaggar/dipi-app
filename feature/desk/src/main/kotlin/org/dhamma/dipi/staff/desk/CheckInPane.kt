package org.dhamma.dipi.staff.desk

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.SEAT_TYPES
import org.dhamma.dipi.staff.ui.theme.DeskKicker
import org.dhamma.dipi.staff.ui.theme.DeskStyle
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.DipiSans
import org.dhamma.dipi.staff.ui.theme.Industry
import org.dhamma.dipi.staff.ui.theme.deskCard

/**
 * Zero Day arrival desk: search, one merged roster (a checked-in row keeps
 * its place, gains a tick, shows room · seat), and a derived statistics
 * sidebar. Target: under 20 seconds per arrival, no screen change.
 */
@Composable
fun CheckInPane(
    roll: List<ApplicantCard>,
    checkIns: Map<ApplicantId, CheckInRecord>,
    rooms: List<AccoRoom>,
    scan: String,
    filter: String,
    flaggedIds: Set<ApplicantId>,
    gender: String = "Both",
    seniority: String = "Both",
    onScan: (String) -> Unit,
    onFilter: (String) -> Unit,
    onGender: (String) -> Unit = {},
    onSeniority: (String) -> Unit = {},
    onOpen: (ApplicantCard) -> Unit,
) {
    // Desk-level scope: a tablet on the new-female desk sees/counts that subset only.
    val genderScope = deskGenderScope(gender)
    val scoped = deskRoll(roll, genderScope, deskSeniorityScope(seniority))
    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            CheckInHeader(
                scoped, checkIns, scan, filter, gender, seniority,
                onScan, onFilter, onGender, onSeniority,
            )
            val shown = deskRosterRows(scoped, checkIns, scan, filter)
            if (shown.isEmpty()) {
                DeskEmpty(
                    "Nobody matches that. Clear the field to see the whole roll.",
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
                )
            } else {
                LazyColumn {
                    items(shown, key = { it.id.value }) { card ->
                        RosterRow(
                            card = card,
                            record = deskRecord(card, checkIns),
                            hasFinding = card.id in flaggedIds,
                            onClick = { onOpen(card) },
                        )
                    }
                }
            }
        }
        CheckInSidebar(scoped, checkIns, rooms, genderScope)
    }
}

@Composable
private fun CheckInHeader(
    roll: List<ApplicantCard>,
    checkIns: Map<ApplicantId, CheckInRecord>,
    scan: String,
    filter: String,
    gender: String,
    seniority: String,
    onScan: (String) -> Unit,
    onFilter: (String) -> Unit,
    onGender: (String) -> Unit,
    onSeniority: (String) -> Unit,
) {
    val inCount = roll.count { deskCheckedIn(it, checkIns) }
    val total = roll.size
    val pct = if (total == 0) 0f else inCount.toFloat() / total

    Column(
        Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DeskKicker("CONF NUMBER OR NAME", Industry.neutral500)
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScanField(scan, onScan, Modifier.weight(1f))
                // The primary row matches the 52dp field; DeskSegmented has no
                // height parameter, so the padding carries it.
                DeskSegmented(
                    listOf("To arrive", "Arrived", "All"),
                    filter,
                    onFilter,
                    verticalPadding = 18.dp,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DeskKicker("THIS TABLET", Industry.neutral500)
                DeskScopeFilters(gender, seniority, onGender, onSeniority)
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .deskCard(elevation = 0.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "$inCount",
                    fontFamily = DipiCondensed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    lineHeight = 26.sp,
                    color = Industry.accent800,
                )
                Text(
                    " of $total checked in",
                    fontSize = 13.sp,
                    color = Industry.neutral600,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${total - inCount} to arrive",
                    fontFamily = DipiMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = Industry.neutral600,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            val fill by animateFloatAsState(pct, animationSpec = tween(250), label = "progress")
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Industry.neutral200),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fill)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Industry.accent),
                )
            }
        }
    }
}

/**
 * The scan buffer: a chit under the reader, or a conf number typed by hand.
 * It is session-scoped — [org.dhamma.dipi.staff.ui.deskOpenCourse] empties it
 * when a course opens, so the roster never opens silently filtered. The clear
 * control is one tap that empties the field; the roster filter derives from
 * the scan, so it follows.
 */
@Composable
private fun ScanField(scan: String, onScan: (String) -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier
            .height(52.dp)
            .background(DeskStyle.cardFill, DeskStyle.controlShape)
            // The focus ring is 2dp accent, so the border is drawn here rather
            // than through deskCard's fixed 1dp hairline.
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) Industry.accent else Industry.neutral400,
                DeskStyle.controlShape,
            )
            .clip(DeskStyle.controlShape)
            .padding(start = 14.dp, end = if (scan.isEmpty()) 14.dp else 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ScanGlyph(15.dp, if (focused) Industry.accent else Industry.neutral500)
        BasicTextField(
            value = scan,
            onValueChange = onScan,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = Industry.text,
            ),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                if (scan.isEmpty()) {
                    Text(
                        "Scan a chit or type a conf number",
                        fontFamily = DipiSans,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Industry.neutral500,
                    )
                }
                inner()
            },
        )
        if (scan.isNotEmpty()) {
            Box(
                Modifier
                    .size(48.dp)
                    .clickable { onScan("") }
                    .semantics { contentDescription = "Clear the scan field" },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Industry.neutral200),
                    contentAlignment = Alignment.Center,
                ) {
                    DeskIcon(DeskIconKind.Close, 15.dp, Industry.neutral700)
                }
            }
        }
    }
}

/** Barcode-reader mark: four corner brackets around a scan line. */
@Composable
private fun ScanGlyph(size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val s = this.size.width / 24f
        val stroke = Stroke(width = 1.8f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun corner(vararg pts: Pair<Float, Float>) {
            val path = Path().apply {
                moveTo(pts[0].first * s, pts[0].second * s)
                for (p in pts.drop(1)) lineTo(p.first * s, p.second * s)
            }
            drawPath(path, color, style = stroke)
        }
        corner(3f to 8f, 3f to 3f, 8f to 3f)
        corner(16f to 3f, 21f to 3f, 21f to 8f)
        corner(21f to 16f, 21f to 21f, 16f to 21f)
        corner(8f to 21f, 3f to 21f, 3f to 16f)
        corner(3f to 12f, 21f to 12f)
    }
}

@Composable
private fun RosterRow(
    card: ApplicantCard,
    record: CheckInRecord?,
    hasFinding: Boolean,
    onClick: () -> Unit,
) {
    val isIn = record?.checkedIn == true
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .bottomHairline(Industry.neutral200)
            .padding(horizontal = 24.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (isIn) Industry.accent else Color.Transparent)
                .border(1.dp, if (isIn) Industry.accent else Industry.neutral400, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isIn) DeskIcon(DeskIconKind.Check, 12.dp, Color.White, strokeWidth = 2.2f)
        }
        Text(
            card.confNo?.display() ?: "—",
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = Industry.neutral700,
            modifier = Modifier.width(56.dp),
        )
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                card.displayName,
                fontFamily = DipiSans,
                fontWeight = FontWeight.Medium,
                fontSize = 15.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Industry.text,
            )
            Box(
                Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (hasFinding) Industry.accent else Color.Transparent),
            )
        }
        Text(
            listOfNotNull(card.age?.toString(), card.gender.name).joinToString(" ") +
                (card.city?.let { " · $it" } ?: ""),
            fontSize = 14.sp,
            color = Industry.neutral600,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(190.dp),
        )
        Box(
            Modifier
                .width(132.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(5.dp))
                .border(1.dp, if (isIn) Industry.neutral300 else Industry.accent300, RoundedCornerShape(5.dp))
                .testTag("checkin-mark"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (isIn) "${record?.room} · ${record?.seat}" else "Mark attended",
                fontSize = 13.5.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                color = if (isIn) Industry.neutral700 else Industry.accent800,
            )
        }
    }
}

@Composable
private fun CheckInSidebar(
    roll: List<ApplicantCard>,
    checkIns: Map<ApplicantId, CheckInRecord>,
    rooms: List<AccoRoom>,
    scope: Gender?,
) {
    Column(
        Modifier
            .width(296.dp)
            .fillMaxHeight()
            .background(Industry.surface)
            .leftHairline(Industry.neutral300)
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            DeskKicker("THE ROLL", Industry.neutral500)
            RollTable(roll)
        }

        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            DeskKicker("ROOMS FREE", Industry.neutral500, Modifier.padding(bottom = 7.dp))
            val occupied = deskOccupied(roll, checkIns)
            listOf(
                Gender.F to "Female",
                Gender.M to "Male",
            ).filter { scope == null || it.first == scope }.forEach { (g, label) ->
                val block = rooms.filter { it.gender == g }
                if (block.isEmpty()) return@forEach
                val free = block.count { it.code !in occupied }
                val sections = block.map { it.section }.distinct().filter { it.isNotBlank() }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .bottomHairline(Industry.neutral200)
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Label and count are separate columns: a long block name
                    // ellipsises at the column edge instead of wrapping mid-label.
                    Text(
                        if (sections.isEmpty()) label else "$label · ${sections.joinToString("/")} block",
                        fontSize = 12.5.sp,
                        color = Industry.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    Text(
                        "$free / ${block.size}",
                        fontFamily = DipiMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Industry.accent700,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(86.dp),
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            DeskKicker("SEATING ISSUED", Industry.neutral500, Modifier.padding(bottom = 7.dp))
            SEAT_TYPES.forEach { seat ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .bottomHairline(Industry.neutral200)
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(seat, fontSize = 12.5.sp, color = Industry.text, modifier = Modifier.weight(1f))
                    Text(
                        "${deskSeatCount(roll, checkIns, seat)}",
                        fontFamily = DipiMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = Industry.text,
                    )
                }
            }
        }
    }
}

@Composable
private fun RollTable(roll: List<ApplicantCard>) {
    Column(Modifier.deskCard(shape = DeskStyle.tileShape, elevation = 0.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Industry.neutral100)
                .bottomHairline(Industry.neutral200)
                .padding(vertical = 5.dp),
        ) {
            Spacer(Modifier.weight(1f).padding(start = 8.dp))
            listOf("M", "F").forEach { h ->
                Text(
                    h,
                    fontFamily = DipiCondensed,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = Industry.neutral600,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(40.dp),
                )
            }
        }
        listOf(
            Triple("Old students", deskRollCell(roll, 'M', true), deskRollCell(roll, 'F', true)),
            Triple("New students", deskRollCell(roll, 'M', false), deskRollCell(roll, 'F', false)),
            Triple(
                "Total",
                deskRollCell(roll, 'M', true) + deskRollCell(roll, 'M', false),
                deskRollCell(roll, 'F', true) + deskRollCell(roll, 'F', false),
            ),
        ).forEachIndexed { i, (label, m, f) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(if (i < 2) Modifier.bottomHairline(Industry.neutral200) else Modifier)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    label,
                    fontSize = 12.sp,
                    color = Industry.neutral700,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                listOf(m, f).forEach { n ->
                    Text(
                        "$n",
                        fontFamily = DipiMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = Industry.text,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(40.dp),
                    )
                }
            }
        }
    }
}

/**
 * The mark-attended dialog: everything one arrival needs, rendered over the
 * roster. The room picker expands in place, pre-filtered to free rooms of
 * the student's gender.
 */
@Composable
fun CheckInDialog(
    card: ApplicantCard,
    record: CheckInRecord,
    roll: List<ApplicantCard>,
    checkIns: Map<ApplicantId, CheckInRecord>,
    rooms: List<AccoRoom>,
    roomOpen: Boolean,
    laundryOn: Boolean,
    valuablesOn: Boolean,
    groupsOn: Boolean,
    onToggleRooms: () -> Unit,
    onRoom: (String) -> Unit,
    onSeat: (String) -> Unit,
    onValuables: () -> Unit,
    onLaundry: () -> Unit,
    onGroup: (String) -> Unit,
    onSave: () -> Unit,
    onUndo: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Industry.scrim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose,
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(560.dp)
                .deskCard(
                    border = Industry.accent,
                    elevation = DeskStyle.dialogElevation,
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            // Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .bottomHairline(Industry.neutral300)
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    DeskKicker("CHECK IN · ${card.confNo?.display() ?: "—"}", Industry.accent700)
                    Text(
                        card.displayName,
                        fontFamily = DipiCondensed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 27.sp,
                        lineHeight = 28.sp,
                        color = Industry.text,
                    )
                    Text(
                        listOfNotNull(card.age?.toString(), card.gender.name).joinToString(" ") +
                            (card.city?.let { " · $it" } ?: ""),
                        fontSize = 12.5.sp,
                        color = Industry.neutral600,
                    )
                }
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(DeskStyle.controlShape)
                        .border(1.dp, Industry.neutral400, DeskStyle.controlShape)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    DeskIcon(DeskIconKind.Close, 13.dp, Industry.text)
                }
            }

            // Body. A free-room block can run past 70 rooms at three per
            // row, so this is the one scroll for the whole dialog body —
            // nothing inside it (room picker, seating, valuables/laundry,
            // group grid) scrolls on its own. Header and footer stay
            // outside, so the primary action is never scrolled out of reach.
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 18.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DeskKicker("ROOM", Industry.neutral500)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(DeskStyle.controlShape)
                            .border(1.dp, Industry.neutral400, DeskStyle.controlShape)
                            .clickable(onClick = onToggleRooms)
                            .padding(horizontal = 13.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            record.room.ifBlank { "Not chosen" },
                            fontFamily = DipiCondensed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = if (record.room.isBlank()) Industry.neutral500 else Industry.accent800,
                        )
                        Text(
                            "only free rooms for this gender",
                            fontSize = 11.5.sp,
                            color = Industry.neutral600,
                            modifier = Modifier.weight(1f),
                        )
                        DeskIcon(DeskIconKind.ChevronDown, 14.dp, Industry.accent)
                    }
                    if (roomOpen) {
                        val occupied = deskOccupied(roll, checkIns, except = card.id)
                        val free = deskFreeRooms(rooms, card.gender, occupied)
                        free.chunked(3).forEach { rowRooms ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                rowRooms.forEach { room ->
                                    val on = record.room == room.code
                                    Column(
                                        Modifier
                                            .weight(1f)
                                            .deskCard(
                                                shape = DeskStyle.tileShape,
                                                fill = if (on) Industry.accent100 else DeskStyle.cardFill,
                                                border = if (on) Industry.accent else DeskStyle.cardBorder,
                                                elevation = 0.dp,
                                            )
                                            .clickable { onRoom(room.code) }
                                            .padding(horizontal = 10.dp, vertical = 9.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(
                                            room.code,
                                            fontFamily = DipiCondensed,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = Industry.text,
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            amenityLabels(room).forEach { label ->
                                                Text(
                                                    label,
                                                    fontFamily = DipiMono,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 8.5.sp,
                                                    maxLines = 1,
                                                    color = Industry.neutral600,
                                                    modifier = Modifier
                                                        .border(1.dp, Industry.neutral300, DeskStyle.pillShape)
                                                        .padding(horizontal = 5.dp, vertical = 1.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                                repeat(3 - rowRooms.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                        if (free.isEmpty()) {
                            DeskEmpty("No free rooms in this block.", Modifier.fillMaxWidth().padding(vertical = 10.dp))
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DeskKicker("SEATING", Industry.neutral500)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SEAT_TYPES.forEach { seat ->
                            val on = record.seat == seat
                            Text(
                                seat,
                                fontFamily = DipiCondensed,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                color = if (on) Industry.accent800 else Industry.neutral700,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(DeskStyle.controlShape)
                                    .background(if (on) Industry.accent100 else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (on) Industry.accent else Industry.neutral300,
                                        DeskStyle.controlShape,
                                    )
                                    .clickable { onSeat(seat) }
                                    .padding(horizontal = 6.dp, vertical = 12.dp),
                            )
                        }
                    }
                }

                if (valuablesOn || laundryOn) {
                    Column(
                        Modifier.fillMaxWidth().topHairline(Industry.neutral200).padding(top = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        if (valuablesOn) {
                            DialogToggleRow("Valuables deposited", record.valuables, onValuables)
                        }
                        if (laundryOn) {
                            DialogToggleRow("Laundry issued", record.laundry, onLaundry)
                        }
                    }
                }

                if (groupsOn) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DeskKicker("GROUP", Industry.neutral500)
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            (1..9).map { "$it" }.forEach { g ->
                                val on = record.group == g
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(DeskStyle.controlShape)
                                        .background(if (on) Industry.accent else Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (on) Industry.accent else Industry.neutral300,
                                            DeskStyle.controlShape,
                                        )
                                        .clickable { onGroup(g) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        g,
                                        fontFamily = DipiMono,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = if (on) Color.White else Industry.neutral700,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Footer
            Row(
                Modifier
                    .fillMaxWidth()
                    .topHairline(Industry.neutral300)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (record.checkedIn) {
                    Text(
                        "Undo check-in",
                        fontSize = 12.5.sp,
                        color = Industry.neutral600,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                        modifier = Modifier.clickable(onClick = onUndo),
                    )
                }
                Spacer(Modifier.weight(1f))
                DeskOutlineButton("Cancel", onClose)
                DeskPrimaryButton(
                    if (record.checkedIn) "Save changes" else "Check in ${card.displayName.substringBefore(" ")}",
                    onSave,
                )
            }
        }
    }
}

@Composable
private fun DialogToggleRow(label: String, on: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, color = Industry.text, modifier = Modifier.weight(1f))
        DeskToggle(on, onToggle)
    }
}

internal fun amenityLabels(room: AccoRoom): List<String> = buildList {
    if (room.features.geyser) add("Geyser")
    if (room.features.indianToilet) add("Indian")
    if (room.features.westernToilet) add("Western")
}
