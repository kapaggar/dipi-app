package org.dhamma.dipi.staff.teacher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.RollGroup
import org.dhamma.dipi.staff.model.RollRow
import org.dhamma.dipi.staff.model.TeacherRoll
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.Industry

/** The header's two-way segmented control over the ONE fetched response. */
enum class TeacherView { SENIORITY, SEATING }

// Fixed hexes DESIGN.md § Course ops names outside the ramp tokens:
// row hairline · card hairline · rules.
private val RowHairline = Color(0xFFEDEDF1)
private val CardHairline = Color(0xFFDEDEE1)
private val Rule = Color(0xFFE0E0E3)

private fun Modifier.bottomHairline(color: Color): Modifier = drawBehind {
    val y = size.height - 0.5.dp.toPx()
    drawLine(color, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
}

private fun Modifier.topHairline(color: Color): Modifier = drawBehind {
    val y = 0.5.dp.toPx()
    drawLine(color, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
}

/**
 * Frame 2b — the seniority roll, course ops home. Fully prop-driven: state
 * and callbacks in, no ViewModel. Groups and rows render in EXACT parse
 * order — the page's seniority order is meaning, so this screen never
 * sorts, merges or re-groups. FLAGS render whatever [flagsFor] carries
 * (empty until 2d's prefetch lands). The Seating destination is a no-op
 * this wave — [onView] fires, the wiring decides.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TeacherListScreen(
    roll: TeacherRoll,
    courseLine: String,
    view: TeacherView = TeacherView.SENIORITY,
    groupFilter: String? = null,
    offline: Boolean = false,
    flagsFor: (RollRow) -> List<String> = { emptyList() },
    onView: (TeacherView) -> Unit = {},
    onGroupFilter: (String?) -> Unit = {},
    onOpen: (RollRow) -> Unit = {},
    onSettings: () -> Unit = {},
) {
    val groups = if (groupFilter == null) roll.groups else roll.groups.filter { it.key == groupFilter }
    val listState = rememberLazyListState()
    // band + column header + rows per group → which group owns the item at
    // the top of the viewport, so the footer can peek the NEXT one.
    val groupStarts = remember(groups) {
        var acc = 0
        groups.map { g -> acc.also { acc += 2 + g.rows.size } }
    }
    val nextGroup by remember(groups) {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            val current = groupStarts.indexOfLast { it <= first }.coerceAtLeast(0)
            groups.getOrNull(current + 1)
        }
    }

    Column(Modifier.fillMaxSize().background(Industry.bg)) {
        if (offline) OfflineStrip()
        Header(courseLine, view, onView, onSettings)
        GroupFilterBand(roll.groups, groupFilter, onGroupFilter)
        Box(Modifier.weight(1f)) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().testTag("teacher-roll")) {
                groups.forEach { group ->
                    stickyHeader(key = "band-${group.key}") { GroupBand(group) }
                    item(key = "cols-${group.key}") { ColumnHeader() }
                    items(group.rows.size, key = { "row-${group.key}-${group.rows[it].sn}" }) { i ->
                        RollRowLine(group.rows[i], flagsFor, onOpen)
                    }
                }
            }
        }
        nextGroup?.let { NextGroupFooter(it) }
    }
}

/**
 * Course ops reuses the desk's offline strip verbatim (38dp, pushes content
 * down, never floats). `SyncBannerStrips` lives in the app shell; queued is
 * structurally 0 here — nothing in course ops writes — so the offline half
 * is the whole strip.
 */
@Composable
private fun OfflineStrip() {
    Column(Modifier.fillMaxWidth().testTag("offline-strip")) {
        Row(
            Modifier.fillMaxWidth().height(38.dp).background(Industry.neutral200).padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("◍  Offline — showing cached list", color = Industry.neutral700, fontSize = 14.sp)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(CardHairline))
    }
}

@Composable
private fun Header(
    courseLine: String,
    view: TeacherView,
    onView: (TeacherView) -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Teacher list",
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 23.sp,
                letterSpacing = 0.2.sp,
                color = Industry.text,
            )
            Spacer(Modifier.height(4.dp))
            Text(courseLine, fontSize = 13.sp, color = Industry.neutral600, maxLines = 1)
        }
        DestinationButton("Seniority", selected = view == TeacherView.SENIORITY) { onView(TeacherView.SENIORITY) }
        Spacer(Modifier.width(8.dp))
        DestinationButton("Seating plan", selected = view == TeacherView.SEATING) { onView(TeacherView.SEATING) }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(48.dp)
                .clickable(onClick = onSettings, role = Role.Button)
                .testTag("teacher-settings"),
            contentAlignment = Alignment.Center,
        ) {
            Text("⚙", fontSize = 18.sp, color = Industry.neutral600)
        }
    }
}

@Composable
private fun DestinationButton(label: String, selected: Boolean, onClick: () -> Unit) {
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

/** 44dp band: kicker `GROUP` + one 30dp pill per group returned, tap toggles the filter. */
@Composable
private fun GroupFilterBand(
    groups: List<RollGroup>,
    groupFilter: String?,
    onGroupFilter: (String?) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .bottomHairline(Rule)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "GROUP",
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            letterSpacing = 1.7.sp,
            color = Industry.neutral500,
            modifier = Modifier.padding(end = 4.dp),
        )
        groups.forEach { g ->
            val selected = groupFilter == g.key
            val shape = RoundedCornerShape(15.dp)
            Row(
                Modifier
                    .height(30.dp)
                    .then(
                        if (selected) {
                            Modifier.background(Color.White, shape).border(1.5.dp, Industry.accent, shape)
                        } else {
                            Modifier.background(Industry.neutral100, shape).border(1.dp, Rule, shape)
                        },
                    )
                    .clickable(role = Role.Button) { onGroupFilter(if (selected) null else g.key) }
                    .padding(horizontal = 12.dp)
                    .testTag("group-pill-${g.key}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    pillLabel(g),
                    fontSize = 12.5.sp,
                    color = if (selected) Industry.accent800 else Industry.neutral600,
                )
                Text(
                    g.total.toString(),
                    fontFamily = DipiMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = Industry.neutral500,
                )
            }
        }
    }
}

private fun pillLabel(g: RollGroup) = "${g.genderWord} ${g.seniorityWord} G${g.group}"

/** Band text is the page's own, ` · ` joined — never invented. */
private fun bandText(g: RollGroup) = "${g.atLine} · ${g.qualifier}"

/** Sticky 34dp group band: accent100 on 1dp accent300, count right. */
@Composable
private fun GroupBand(group: RollGroup) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(34.dp)
            .background(Industry.accent100, shape)
            .border(1.dp, Industry.accent300, shape)
            .padding(horizontal = 12.dp)
            .testTag("group-band-${group.key}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            group.atLine,
            fontFamily = DipiCondensed,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            letterSpacing = 0.3.sp,
            color = Industry.accent800,
            maxLines = 1,
        )
        Text(group.qualifier, fontSize = 12.5.sp, color = Industry.accent500, maxLines = 1)
        Spacer(Modifier.weight(1f))
        Text(
            "${group.total} TOTAL",
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = Industry.accent700,
        )
    }
}

@Composable
private fun ColumnHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(28.dp)
            .bottomHairline(Rule)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        HeaderCell("S/N", Modifier.width(34.dp))
        HeaderCell("STUDENT", Modifier.weight(1f))
        HeaderCell("ROOM", Modifier.width(86.dp))
        HeaderCell("AGE", Modifier.width(46.dp), TextAlign.End)
        HeaderCell("CITY", Modifier.padding(start = 16.dp).width(124.dp))
        HeaderCell("COURSES", Modifier.padding(start = 16.dp).width(236.dp))
        HeaderCell("SEAT", Modifier.width(64.dp), TextAlign.End)
        HeaderCell("FLAGS", Modifier.width(96.dp), TextAlign.End)
    }
}

@Composable
private fun HeaderCell(label: String, modifier: Modifier, align: TextAlign = TextAlign.Start) {
    Text(
        label,
        fontFamily = DipiMono,
        fontWeight = FontWeight.Medium,
        fontSize = 9.sp,
        letterSpacing = 1.4.sp,
        color = Industry.neutral500,
        textAlign = align,
        modifier = modifier.padding(bottom = 4.dp),
    )
}

/** Occupation · education · languages as the page sends them; em-dash for blanks. */
private fun foldedLine(row: RollRow): String =
    listOf(row.occupation, row.education, row.languages)
        .joinToString(" · ") { it.ifBlank { "—" } }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RollRowLine(
    row: RollRow,
    flagsFor: (RollRow) -> List<String>,
    onOpen: (RollRow) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(52.dp)
            .clickable { onOpen(row) }
            .bottomHairline(RowHairline)
            .padding(horizontal = 12.dp)
            .testTag("roll-row"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            row.sn.toString(),
            fontFamily = DipiMono,
            fontSize = 13.sp,
            color = Industry.neutral500,
            modifier = Modifier.width(34.dp),
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.name,
                    fontSize = 15.5.sp,
                    lineHeight = 17.8.sp,
                    fontWeight = FontWeight.Medium,
                    color = Industry.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                row.roleTag?.let { tag ->
                    Text(
                        "($tag)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Industry.neutral600,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                foldedLine(row),
                fontSize = 11.5.sp,
                lineHeight = 13.sp,
                color = Industry.neutral500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            row.room,
            fontFamily = DipiMono,
            fontSize = 13.5.sp,
            color = Industry.neutral600,
            maxLines = 1,
            modifier = Modifier.width(86.dp),
        )
        Text(
            row.age,
            fontFamily = DipiMono,
            fontSize = 14.sp,
            color = Industry.neutral800,
            textAlign = TextAlign.End,
            modifier = Modifier.width(46.dp),
        )
        Text(
            row.city,
            fontSize = 13.5.sp,
            color = Industry.neutral600,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp).width(124.dp),
        )
        FlowRow(
            Modifier.padding(start = 16.dp).width(236.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Only non-zero types come from the parse; an empty history
            // renders NOTHING — a blank cell is how a new student reads.
            row.courses.forEach { (key, count) -> CourseChip(key, count) }
        }
        Text(
            row.seat,
            fontFamily = DipiMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = Industry.text,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(64.dp),
        )
        Row(
            Modifier.width(96.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            flagsFor(row).forEach { FlagPill(it) }
        }
    }
}

@Composable
private fun CourseChip(key: String, count: Int) {
    Row(
        Modifier
            .height(20.dp)
            .background(RowHairline, RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp)
            .testTag("course-chip-$key"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            key,
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = Industry.neutral800,
        )
        Text(
            count.toString(),
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = Industry.neutral600,
        )
    }
}

@Composable
private fun FlagPill(label: String) {
    Row(
        Modifier
            .height(22.dp)
            .background(Color.White, RoundedCornerShape(11.dp))
            .border(1.dp, Industry.neutral300, RoundedCornerShape(11.dp))
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            letterSpacing = 0.8.sp,
            color = Industry.neutral600,
        )
    }
}

/** 40dp peek at the next group — informational, not a control; scrolling continues normally. */
@Composable
private fun NextGroupFooter(group: RollGroup) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Industry.neutral100)
            .topHairline(Rule)
            .padding(horizontal = 24.dp)
            .testTag("next-group-footer"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            bandText(group),
            fontFamily = DipiCondensed,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = Industry.neutral600,
            maxLines = 1,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text(
            "${group.total} TOTAL",
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = Industry.neutral600,
        )
        Text("›", fontSize = 15.sp, color = Industry.neutral400)
    }
}
