package org.dhamma.dipi.staff.course

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.CourseReport
import org.dhamma.dipi.staff.model.CourseReportCounts
import org.dhamma.dipi.staff.model.CourseReportRow
import org.dhamma.dipi.staff.ui.theme.DeskStyle
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.Industry
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import org.dhamma.dipi.staff.ui.theme.deskCard

/** Everything the screen needs, so the states in frame `5p` are exhaustive. */
data class CourseReportUi(
    val from: String = "",
    val to: String = "",
    /** True once RUN has been pressed at least once — first open fetches nothing. */
    val ran: Boolean = false,
    val running: Boolean = false,
    val report: CourseReport? = null,
    /** The server's refusal text, verbatim. Never reworded, never summarised. */
    val refusal: String? = null,
    /** `POST /centre/{cid}/course-report · HTTP nnn · hh:mm` under the refusal. */
    val refusalContext: String = "",
)

/**
 * The centre course report as a native surface (v5 T3, frames `5n`–`5q`).
 *
 * The transport is unchanged — the desk's own form is scraped and POSTed and
 * a CSV comes back. What changes is that the CSV is read for the registrar
 * instead of being handed to whatever app claims `text/csv`.
 *
 * **The date range is the only control.** The desk's form offers no course
 * picker, no status filter and no sort, so neither does this screen: a
 * control the server cannot honour is worse than no control.
 */
@Composable
fun CourseReportScreen(
    state: CourseReportUi,
    onFrom: (String) -> Unit = {},
    onTo: (String) -> Unit = {},
    onRun: () -> Unit = {},
    onShareCsv: () -> Unit = {},
    onCopyMessage: (String) -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val c = LocalDipi.current
    Column(
        modifier
            .fillMaxSize()
            .background(c.background)
            .testTag("course-report-screen"),
    ) {
        Header(state, onShareCsv, onBack)
        RangeBand(state, onFrom, onTo, onRun)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.refusal != null -> Refusal(state, onCopyMessage)
                state.running -> Running(state)
                !state.ran -> FirstOpen()
                state.report == null || state.report.isEmpty -> EmptyRange(state)
                else -> Loaded(state.report)
            }
        }
    }
}

@Composable
private fun Header(state: CourseReportUi, onShareCsv: () -> Unit, onBack: () -> Unit) {
    val c = LocalDipi.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.width(48.dp).height(48.dp).clickable(onClick = onBack).testTag("report-back"),
            contentAlignment = Alignment.Center,
        ) {
            Text("‹", fontSize = 26.sp, color = c.muted)
        }
        Column(Modifier.weight(1f)) {
            Text(
                "Course report",
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 23.sp,
                letterSpacing = 0.2.sp,
                color = c.foreground,
            )
            Text(
                "Roll counts for every course that started in the range below.",
                fontSize = 12.5.sp,
                color = c.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Secondary: the CSV is still there for anyone who wants the file.
        if (state.report?.csv != null) {
            Text(
                "SHARE CSV",
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = 0.06.em,
                color = c.muted,
                modifier = Modifier
                    .deskCard(
                        shape = DeskStyle.controlShape,
                        fill = Color.Transparent,
                        border = c.hairline,
                        elevation = 0.dp,
                    )
                    .clickable(onClick = onShareCsv)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("report-share-csv"),
            )
        }
    }
}

/**
 * Two 44dp mono date fields and one 44dp accent RUN. The range stays
 * editable while a run is in flight — the honest response to "this is
 * taking a while" is to let the registrar narrow it, not to lock the form.
 */
@Composable
private fun RangeBand(
    state: CourseReportUi,
    onFrom: (String) -> Unit,
    onTo: (String) -> Unit,
    onRun: () -> Unit,
) {
    val c = LocalDipi.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Industry.neutral100)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DateField("FROM", state.from, "report-from", onFrom)
        DateField("TO", state.to, "report-to", onTo)
        Spacer(Modifier.weight(1f))
        Text(
            if (state.running) "RUNNING…" else "RUN",
            fontFamily = DipiCondensed,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            letterSpacing = 0.06.em,
            color = Color.White,
            modifier = Modifier
                .deskCard(
                    shape = DeskStyle.controlShape,
                    fill = if (state.running) Industry.accent700 else c.accent,
                    border = if (state.running) Industry.accent700 else c.accent,
                    elevation = 0.dp,
                )
                .clickable(enabled = !state.running, onClick = onRun)
                .padding(horizontal = 26.dp, vertical = 12.dp)
                .testTag("report-run"),
        )
    }
}

@Composable
private fun DateField(label: String, value: String, tag: String, onChange: (String) -> Unit) {
    val c = LocalDipi.current
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            label,
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            letterSpacing = 0.14.em,
            color = c.muted,
        )
        Box(
            Modifier
                .width(140.dp)
                .height(44.dp)
                .deskCard(
                    shape = DeskStyle.controlShape,
                    fill = c.field,
                    border = c.hairline,
                    elevation = 0.dp,
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = DipiMono,
                    fontSize = 14.sp,
                    color = c.foreground,
                ),
                cursorBrush = SolidColor(c.accent),
                modifier = Modifier.fillMaxWidth().testTag(tag),
            )
        }
    }
}

/** RUN is a deliberate act: the first open of this screen fetches nothing. */
@Composable
private fun FirstOpen() {
    Message(
        title = "Nothing has been asked for yet.",
        body = "Set the range and press RUN. The desk builds this report from " +
            "scratch every time, so nothing is fetched until you ask.",
        tag = "report-first-open",
    )
}

@Composable
private fun Running(state: CourseReportUi) {
    Message(
        title = "Asking the desk for ${state.from} → ${state.to}.",
        body = "A wide range takes the desk a while — it walks every course " +
            "in the window. The range above stays editable if you want to narrow it.",
        tag = "report-running",
    )
}

/** An empty range is a real answer, so it names the mistake worth checking. */
@Composable
private fun EmptyRange(state: CourseReportUi) {
    val reversed = state.from.isNotBlank() && state.to.isNotBlank() && state.from > state.to
    Message(
        title = "No course started between ${state.from} and ${state.to}.",
        body = if (reversed) {
            "The dates are the wrong way round — FROM is later than TO. " +
                "Swap them and run again."
        } else {
            "That is the answer, not a failure. Widen the range and run again " +
                "if you expected something here."
        },
        tag = "report-empty",
    )
}

@Composable
private fun Message(title: String, body: String, tag: String) {
    val c = LocalDipi.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp, vertical = 28.dp)
            .testTag(tag),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            title,
            fontFamily = DipiCondensed,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = c.foreground,
        )
        Text(body, fontSize = 13.sp, lineHeight = 19.sp, color = c.muted)
    }
}

/**
 * A refusal prints the server's text **verbatim** in a white block on the
 * danger tint, with the request and time in mono beneath it. No rewording,
 * no icon, no retry, no cached fallback, and no client-side interpretation
 * of the status code.
 */
@Composable
private fun Refusal(state: CourseReportUi, onCopyMessage: (String) -> Unit) {
    val c = LocalDipi.current
    val message = state.refusal.orEmpty()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp, vertical = 22.dp)
            .testTag("report-refusal"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(DangerTint)
                .padding(2.dp),
        ) {
            Text(
                message,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Industry.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("report-refusal-text"),
            )
        }
        Text(
            state.refusalContext,
            fontFamily = DipiMono,
            fontSize = 11.sp,
            color = c.muted,
        )
        Text(
            "COPY MESSAGE",
            fontFamily = DipiCondensed,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            letterSpacing = 0.06.em,
            color = c.muted,
            modifier = Modifier
                .deskCard(
                    shape = DeskStyle.controlShape,
                    fill = Color.Transparent,
                    border = c.hairline,
                    elevation = 0.dp,
                )
                .clickable { onCopyMessage(message) }
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("report-copy-message"),
        )
    }
}

/** Danger is the fixed pair and never follows the skin (design rule). */
private val DangerTint = Color(0x22A33A34)

/**
 * Fourteen columns become five groups — NEW · OLD · ROLL TOTAL · SEVAK ·
 * TEACHERS — under the centre matrix's group caps, with the roll total
 * banded and 1dp gutters between the groups. **Nothing is dropped.**
 */
@Composable
private fun Loaded(report: CourseReport) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp),
        ) {
            GroupCaps()
            ColumnHeaders()
            report.rows.forEach { ReportRow(it) }
        }
        GrandTotalFooter(report)
    }
}

private val GROUPS = listOf(
    "NEW" to 3f,
    "OLD" to 3f,
    "ROLL" to 1.2f,
    "SEVAK" to 3f,
    "TEACHERS" to 3f,
)

@Composable
private fun GroupCaps() {
    Row(Modifier.fillMaxWidth().height(22.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(4f))
        GROUPS.forEach { (name, weight) ->
            Text(
                name,
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp,
                letterSpacing = 0.17.em,
                textAlign = TextAlign.Center,
                color = Industry.neutral500,
                modifier = Modifier.weight(weight),
            )
        }
    }
}

private val HEADERS = listOf("M", "F", "T", "M", "F", "T", "TOTAL", "M", "F", "T", "C", "A", "TR")

@Composable
private fun ColumnHeaders() {
    Row(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .bottomRule(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "COURSE",
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            letterSpacing = 0.14.em,
            color = Industry.neutral500,
            modifier = Modifier.weight(4f),
        )
        HEADERS.forEachIndexed { i, h ->
            Figure(h, weight = headerWeight(i), banded = i == 6, header = true)
        }
    }
}

private fun headerWeight(i: Int): Float = if (i == 6) 1.2f else 1f

@Composable
private fun ReportRow(row: CourseReportRow) {
    val name = row.parsed
    Row(
        Modifier.fillMaxWidth().height(52.dp).bottomHairline(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(4f).padding(end = 10.dp)) {
            Text(
                // A parse failure prints the raw string, never an error.
                name.type,
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                lineHeight = 19.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Industry.text,
            )
            if (!name.raw) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        name.year,
                        fontFamily = DipiMono,
                        fontSize = 11.5.sp,
                        color = Industry.neutral600,
                    )
                    Text(
                        name.dates,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Industry.neutral600,
                    )
                }
            }
        }
        CountCells(row.counts)
    }
}

@Composable
private fun RowScope.CountCells(c: CourseReportCounts, bold: Boolean = false) {
    val values = listOf(
        c.newMale, c.newFemale, c.newTotal,
        c.oldMale, c.oldFemale, c.oldTotal,
        c.rollTotal,
        c.sevakMale, c.sevakFemale, c.sevakTotal,
        c.teacherConducting, c.teacherAssistant, c.teacherTrainee,
    )
    values.forEachIndexed { i, v ->
        Figure("$v", weight = headerWeight(i), banded = i == 6, bold = bold && i == 6)
    }
}

@Composable
private fun RowScope.Figure(
    text: String,
    weight: Float,
    banded: Boolean,
    header: Boolean = false,
    bold: Boolean = false,
) {
    Box(
        Modifier
            .weight(weight)
            .height(if (header) 28.dp else 52.dp)
            .background(if (banded) Industry.neutral200 else Color.Transparent),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text,
            fontFamily = DipiMono,
            fontWeight = if (bold || header) FontWeight.Medium else FontWeight.Normal,
            fontSize = if (header) 9.sp else 14.sp,
            letterSpacing = if (header) 0.14.em else 0.em,
            color = if (header) Industry.neutral500 else Industry.text,
            modifier = Modifier.padding(end = 6.dp),
        )
    }
}

/**
 * The grand total is a **footer**, not a row: on a 2dp `neutral900` rule over
 * a `#F5F5F8` ground, pinned to the bottom of the pane so it stays visible
 * however long the list is.
 */
@Composable
private fun GrandTotalFooter(report: CourseReport) {
    Column(Modifier.fillMaxWidth().testTag("report-grand-total")) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(Industry.neutral900))
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Industry.neutral100)
                .padding(horizontal = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(4f)) {
                Text(
                    "GRAND TOTAL",
                    fontFamily = DipiMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 0.17.em,
                    color = Industry.neutral700,
                )
                if (report.from.isNotBlank()) {
                    Text(
                        "${report.from} → ${report.to} · ${report.rows.size} courses",
                        fontSize = 11.5.sp,
                        color = Industry.neutral600,
                    )
                }
            }
            CountCells(report.grandTotal, bold = true)
        }
    }
}

private fun Modifier.bottomRule(): Modifier = drawBottom(Industry.neutral400, 1.dp)

private fun Modifier.bottomHairline(): Modifier = drawBottom(Industry.neutral200, 1.dp)

private fun Modifier.drawBottom(color: Color, thickness: Dp) = drawBehind {
    val h = thickness.toPx()
    drawRect(color = color, topLeft = Offset(0f, size.height - h), size = Size(size.width, h))
}
