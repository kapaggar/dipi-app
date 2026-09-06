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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
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
import org.dhamma.dipi.staff.model.backrestSeatLabel
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.Industry

/** The header's two-way segmented control over the ONE fetched response. */
enum class TeacherView { SENIORITY, SEATING }

// Fixed hexes DESIGN.md § Course ops names outside the ramp tokens:
// row hairline · card hairline · rules.
private val PillFill = Color(0xFFFAFAFB)
private val RowHairline = Color(0xFFEDEDF1)
private val CardHairline = Color(0xFFDEDEE1)
private val Rule = Color(0xFFE0E0E3)

private val SnW = 30.dp
private val RoomW = 78.dp
private val AgeW = 40.dp
private val CityW = 120.dp
private val CityWideW = 180.dp
private val CoursesW = 200.dp
private val SeatW = 76.dp
private val FlagsW = 150.dp
private val CityPad = 14.dp
private val FlagsPad = 16.dp

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
    /** Application pull on entry: attempted/total; null hides the strip. */
    prefetch: Pair<Int, Int>? = null,
    /** Device-local `HH:mm` of the roll fetch — shown on the offline strip. */
    cachedAt: String? = null,
    /** False while this row's `/application-view` has not landed (pending FLAGS). */
    flagsReady: (RollRow) -> Boolean = { true },
) {
    val groups = if (groupFilter == null) roll.groups else roll.groups.filter { it.key == groupFilter }
    val showCourses = remember(groups) { groups.any { g -> g.rows.any { it.courses.isNotEmpty() } } }
    val filterEmpty = groupFilter != null && groups.all { it.rows.isEmpty() }
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
        // Offline and prefetch share the 38dp slot; offline wins (nothing is pulling).
        when {
            offline -> CourseOpsOfflineStrip(cachedAt)
            prefetch != null -> PullProgressStrip(prefetch.first, prefetch.second)
        }
        Header(courseLine, roll.rollCount, view, onView, onSettings)
        GroupFilterBand(roll.groups, groupFilter, onGroupFilter)
        Box(Modifier.weight(1f)) {
            if (filterEmpty) {
                FilterEmptyBody(groups.firstOrNull(), roll, onGroupFilter)
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().testTag("teacher-roll")) {
                    groups.forEach { group ->
                        stickyHeader(key = "band-${group.key}") { GroupBand(group) }
                        item(key = "cols-${group.key}") { ColumnHeader(showCourses) }
                        items(group.rows.size, key = { "row-${group.key}-$it" }) { i ->
                            RollRowLine(group.rows[i], flagsFor, flagsReady, showCourses, onOpen)
                        }
                    }
                    if (!showCourses && groups.any { it.rows.isNotEmpty() }) {
                        item(key = "courses-collapsed") { CoursesCollapsedNotice() }
                    }
                }
            }
        }
        if (!filterEmpty) nextGroup?.let { NextGroupFooter(it) }
    }
}

/**
 * Course ops reuses the desk's offline strip verbatim (38dp, pushes content
 * down, never floats). `SyncBannerStrips` lives in the app shell; queued is
 * structurally 0 here — nothing in course ops writes — so the offline half
 * is the whole strip.
 */
@Composable
fun CourseOpsOfflineStrip(cachedAt: String? = null) {
    val age = cachedAt?.let { "showing the roll cached at $it" } ?: "showing cached list"
    Column(Modifier.fillMaxWidth().testTag("offline-strip")) {
        Row(
            Modifier.fillMaxWidth().height(38.dp).background(Industry.neutral200).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("◍ Offline - $age", color = Industry.neutral800, fontSize = 14.sp)
            Text(
                "nothing is waiting to send; this mode never writes",
                color = Industry.neutral600,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(CardHairline))
    }
}

@Composable
private fun Header(
    courseLine: String,
    rollCount: Int,
    view: TeacherView,
    onView: (TeacherView) -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 20.dp),
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
                modifier = Modifier.testTag("teacher-list-title"),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "$courseLine · $rollCount on the roll",
                fontSize = 12.5.sp,
                color = Industry.neutral600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DestinationButton("Teacher list", selected = view == TeacherView.SENIORITY, testTag = "dest-teacher-list") {
            onView(TeacherView.SENIORITY)
        }
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
private fun DestinationButton(
    label: String,
    selected: Boolean,
    testTag: String? = null,
    onClick: () -> Unit,
) {
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
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
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
            .padding(horizontal = 20.dp),
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
            modifier = Modifier.padding(end = 2.dp),
        )
        groups.forEach { g ->
            val selected = groupFilter == g.key
            val shape = RoundedCornerShape(15.dp)
            Row(
                Modifier
                    .height(30.dp)
                    .then(
                        if (selected) {
                            Modifier.background(Industry.accent100, shape).border(1.5.dp, Industry.accent, shape)
                        } else {
                            Modifier.background(PillFill, shape).border(1.dp, Rule, shape)
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
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    color = if (selected) Industry.accent800 else Industry.neutral700,
                )
                Text(
                    g.total.toString(),
                    fontFamily = DipiMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = if (selected) Industry.accent700 else Industry.neutral500,
                )
            }
        }
        if (groupFilter != null) {
            Spacer(Modifier.weight(1f))
            val shape = RoundedCornerShape(15.dp)
            Row(
                Modifier
                    .height(30.dp)
                    .border(1.dp, Industry.neutral300, shape)
                    .clickable(role = Role.Button) { onGroupFilter(null) }
                    .padding(horizontal = 12.dp)
                    .testTag("clear-filter"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Clear filter ×",
                    fontSize = 12.5.sp,
                    color = Industry.neutral600,
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
            .background(Industry.bg)
            .padding(horizontal = 20.dp)
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
        Text(group.qualifier, fontSize = 12.5.sp, color = Industry.accent600, maxLines = 1)
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
private fun ColumnHeader(showCourses: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(28.dp)
            .bottomHairline(Industry.neutral300)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        HeaderCell("S/N", Modifier.width(SnW))
        HeaderCell("STUDENT", Modifier.weight(1f))
        HeaderCell("ROOM", Modifier.width(RoomW))
        HeaderCell("AGE", Modifier.width(AgeW), TextAlign.End)
        HeaderCell("CITY", Modifier.padding(start = CityPad).width(if (showCourses) CityW else CityWideW))
        if (showCourses) HeaderCell("COURSES", Modifier.padding(start = CityPad).width(CoursesW))
        HeaderCell("SEAT", Modifier.width(SeatW), TextAlign.End)
        HeaderCell("FLAGS", Modifier.padding(start = FlagsPad).width(FlagsW), TextAlign.End)
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

/** Occupation · education · languages as the page sends them; hyphen for blank placeholders. */
private fun foldedLine(row: RollRow): String =
    listOf(row.occupation, row.education, row.languages)
        .joinToString(" · ") { if (it.isBlank() || it == "\u2014") "-" else it }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RollRowLine(
    row: RollRow,
    flagsFor: (RollRow) -> List<String>,
    flagsReady: (RollRow) -> Boolean,
    showCourses: Boolean,
    onOpen: (RollRow) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
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
            fontSize = 12.5.sp,
            color = Industry.neutral400,
            modifier = Modifier.width(SnW),
        )
        Column(Modifier.weight(1f).width(0.dp).padding(end = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.name,
                    fontSize = 15.5.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = Industry.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                row.roleTag?.let { tag ->
                    Text(
                        "($tag)",
                        fontSize = 12.sp,
                        color = Industry.neutral500,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 7.dp),
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
            color = Industry.neutral700,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(RoomW),
        )
        Text(
            row.age,
            fontFamily = DipiMono,
            fontSize = 14.sp,
            color = Industry.neutral800,
            textAlign = TextAlign.End,
            modifier = Modifier.width(AgeW),
        )
        Text(
            row.city,
            fontSize = 13.sp,
            color = Industry.neutral700,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = CityPad).width(if (showCourses) CityW else CityWideW),
        )
        if (showCourses) {
            FlowRow(
                Modifier.padding(start = CityPad).width(CoursesW),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                row.courses.forEach { (key, count) -> CourseChip(key, count) }
            }
        }
        Text(
            backrestSeatLabel(row.seat, row.backrest),
            fontFamily = DipiMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = Industry.text,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(SeatW),
        )
        val pending = !flagsReady(row)
        Row(
            Modifier
                .padding(start = FlagsPad)
                .width(FlagsW)
                .fillMaxHeight()
                .then(if (pending) Modifier.testTag("flags-pending") else Modifier),
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (pending) {
                Box(
                    Modifier
                        .width(44.dp)
                        .height(8.dp)
                        .background(RowHairline, RoundedCornerShape(4.dp)),
                )
            } else {
                flagsFor(row).forEach { FlagPill(it) }
            }
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
            .border(1.dp, if (label == "HLTH") Industry.neutral400 else Industry.neutral300, RoundedCornerShape(11.dp))
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
            .background(PillFill)
            .topHairline(Rule)
            .padding(horizontal = 32.dp)
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

/**
 * Owner feedback 2026-09-02: the roll's applications buffer into the encrypted
 * course store on entry so the hall reads offline. Same 38dp slot as the
 * offline strip (v6 C7) — tinted because it is live, gone when the pull ends.
 */
@Composable
private fun PullProgressStrip(done: Int, total: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(Industry.accent100)
            .bottomHairline(Industry.accent300)
            .padding(horizontal = 20.dp)
            .testTag("pull-progress"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "◍ Pulling applications… $done of $total",
            fontSize = 14.sp,
            color = Industry.accent800,
        )
        Text(
            "flags and health arrive as each one lands",
            fontSize = 12.5.sp,
            color = Industry.accent600,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else done.toFloat() / total },
            modifier = Modifier.width(220.dp).height(4.dp),
            color = Industry.accent,
            trackColor = Industry.accent200,
        )
    }
}

@Composable
private fun CoursesCollapsedNotice() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(34.dp)
            .padding(horizontal = 12.dp)
            .testTag("courses-collapsed"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            "NO COURSE HISTORY IN THIS GROUP",
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp,
            color = Industry.neutral400,
        )
    }
}

@Composable
private fun FilterEmptyBody(
    empty: RollGroup?,
    roll: TeacherRoll,
    onGroupFilter: (String?) -> Unit,
) {
    val others = roll.groups.filter { it.key != empty?.key && it.rows.isNotEmpty() }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("filter-empty"),
    ) {
        empty?.let { GroupBand(it) }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .height(236.dp)
                .background(PillFill, RoundedCornerShape(8.dp))
                .border(1.dp, Industry.neutral300, RoundedCornerShape(8.dp))
                .padding(horizontal = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(11.dp, Alignment.CenterVertically),
        ) {
            Text(
                "Nobody is in this group",
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                letterSpacing = 0.3.sp,
                color = Industry.text,
            )
            Text(
                "Choose another group or show all students.",
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
                color = Industry.neutral600,
                textAlign = TextAlign.Center,
            )
            Row(
                Modifier
                    .height(48.dp)
                    .background(Color.White, RoundedCornerShape(6.dp))
                    .border(1.dp, Industry.neutral300, RoundedCornerShape(6.dp))
                    .clickable(role = Role.Button) { onGroupFilter(null) }
                    .padding(horizontal = 22.dp)
                    .testTag("filter-empty-clear"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Clear the filter · show all ${roll.rollCount}",
                    fontSize = 13.5.sp,
                    color = Industry.neutral700,
                )
            }
        }
        if (others.isNotEmpty()) {
            Text(
                "THE OTHER ${if (others.size == 1) "GROUP" else "GROUPS"}",
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp,
                letterSpacing = 1.7.sp,
                color = Industry.neutral500,
                modifier = Modifier.padding(top = 18.dp, bottom = 9.dp),
            )
            others.forEach { g ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .height(44.dp)
                        .background(PillFill, RoundedCornerShape(6.dp))
                        .border(1.dp, Rule, RoundedCornerShape(6.dp))
                        .clickable(role = Role.Button) { onGroupFilter(g.key) }
                        .padding(horizontal = 14.dp)
                        .testTag("filter-empty-other-${g.key}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        pillLabel(g),
                        fontFamily = DipiCondensed,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        letterSpacing = 0.2.sp,
                        color = Industry.text,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${g.total} students",
                        fontFamily = DipiMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.5.sp,
                        color = Industry.neutral600,
                    )
                    Text("›", fontSize = 15.sp, color = Industry.neutral400, modifier = Modifier.padding(start = 14.dp))
                }
            }
        }
    }
}
