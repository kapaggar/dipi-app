package org.dhamma.dipi.staff.course

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import org.dhamma.dipi.staff.model.CourseReport
import org.dhamma.dipi.staff.model.CourseReportCounts
import org.dhamma.dipi.staff.model.CourseReportRow
import org.dhamma.dipi.staff.model.ReportPreset
import org.dhamma.dipi.staff.model.displayDeskDate
import org.dhamma.dipi.staff.model.parseDeskDate
import org.dhamma.dipi.staff.model.reportPresetRange
import org.dhamma.dipi.staff.model.reportRangeError
import org.dhamma.dipi.staff.model.reportRangeIsValid
import org.dhamma.dipi.staff.ui.theme.DeskStyle
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.LocalDarkTheme
import org.dhamma.dipi.staff.ui.theme.LocalDeskColors
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import org.dhamma.dipi.staff.ui.theme.LocalIndustry
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
    /** Device-local `HH:mm` of the last successful RUN. */
    val ranAt: String? = null,
)

private val DateFieldW = 168.dp
private val DateFieldH = 48.dp
private val FilterGap = 12.dp
private val RunSidePad = 30.dp
private val PresetWrapBelow = 900.dp
private val CourseMinW = 248.dp
private val CellW = 42.dp
private val RollW = 80.dp
private val GroupGap = 4.dp
private val TablePad = 26.dp

private val EmptyRangeCopy = "Choose dates, then tap RUN."
private val ColumnGlossary =
    "NEW / OLD students by gender · ROLL TOTAL excludes sevaks · SEVAK counted separately · TEACHERS C conducting, A assistant, TR trainee."
private val RollOnlyNote =
    "ROLL TOTAL is students only; SEVAK is counted beside it, never inside it."

/**
 * Light/Blossom hexes from the owner frame, remapped in Dark so the page
 * keeps contrast on Steel night instead of shipping a light-only sheet.
 */
internal data class CourseReportTokens(
    val validBorder: Color,
    val invalidBorder: Color,
    val invalidFill: Color,
    val invalidOutline: Color,
    val invalidText: Color,
    val rollTint: Color,
)

@Composable
internal fun courseReportTokens(): CourseReportTokens {
    val dark = LocalDarkTheme.current
    val c = LocalDipi.current
    return if (dark) {
        CourseReportTokens(
            validBorder = Color(0xFF8E6A78),
            invalidBorder = c.hard,
            invalidFill = Color(0xFF3B2626),
            invalidOutline = Color(0xFF8A4A46),
            invalidText = c.hard,
            rollTint = Color(0xFF2A1E24),
        )
    } else {
        CourseReportTokens(
            validBorder = Color(0xFFC99FB1),
            invalidBorder = Color(0xFFA33A34),
            invalidFill = Color(0xFFFBF0EF),
            invalidOutline = Color(0xFFDFAFAB),
            invalidText = Color(0xFF7A2B26),
            rollTint = Color(0xFFF3E2E8),
        )
    }
}

/**
 * The centre course report as a native surface (v5 T3, frames `5n`–`5q`).
 *
 * The transport is unchanged — the desk's own form is scraped and POSTed and
 * a CSV comes back. What changes is that the CSV is read for the registrar
 * instead of being handed to whatever app claims `text/csv`.
 *
 * **The date range is the only control.** Presets fill FROM/TO from the
 * device calendar and run the same POST. The desk's form offers no course
 * picker, no status filter and no sort.
 */
@Composable
fun CourseReportScreen(
    state: CourseReportUi,
    onFrom: (String) -> Unit = {},
    onTo: (String) -> Unit = {},
    onRun: () -> Unit = {},
    onShareCsv: () -> Unit = {},
    onPrint: () -> Unit = {},
    onCopyMessage: (String) -> Unit = {},
    onBack: () -> Unit = {},
    today: LocalDate = LocalDate.now(),
    modifier: Modifier = Modifier,
) {
    val c = LocalDipi.current
    val rangeError = reportRangeError(state.from, state.to)
    Column(
        modifier
            .fillMaxSize()
            .background(c.background)
            .testTag("course-report-screen"),
    ) {
        Header(state, onShareCsv, onPrint, onBack)
        RangeBand(state, rangeError, today, onFrom, onTo, onRun)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.refusal != null -> Refusal(state, onCopyMessage)
                state.running -> Running(state)
                !state.ran -> FirstOpen()
                state.report == null || state.report.isEmpty -> EmptyRange()
                else -> Loaded(state)
            }
        }
    }
}

@Composable
private fun Header(
    state: CourseReportUi,
    onShareCsv: () -> Unit,
    onPrint: () -> Unit,
    onBack: () -> Unit,
) {
    val c = LocalDipi.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
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
                "every course the desk holds whose dates fall inside the range.",
                fontSize = 12.5.sp,
                color = c.muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (state.report != null && !state.report.isEmpty) {
            HeaderAction("PRINT", "report-print", onPrint)
        }
        if (state.report?.csv != null) {
            HeaderAction("SHARE CSV", "report-share-csv", onShareCsv)
        }
    }
}

@Composable
private fun HeaderAction(label: String, tag: String, onClick: () -> Unit) {
    val c = LocalDipi.current
    Box(
        Modifier
            .heightIn(min = 48.dp)
            .deskCard(
                shape = DeskStyle.controlShape,
                fill = Color.Transparent,
                border = c.hairline,
                elevation = 0.dp,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontFamily = DipiCondensed,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            letterSpacing = 0.06.em,
            color = c.muted,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RangeBand(
    state: CourseReportUi,
    rangeError: String?,
    today: LocalDate,
    onFrom: (String) -> Unit,
    onTo: (String) -> Unit,
    onRun: () -> Unit,
) {
    val industry = LocalIndustry.current
    val tokens = courseReportTokens()
    val invalid = rangeError != null
    val canRun = reportRangeIsValid(state.from, state.to) && !state.running
    Column(
        Modifier
            .fillMaxWidth()
            .background(industry.neutral100)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(FilterGap),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stackPresets = maxWidth < PresetWrapBelow
            if (stackPresets) {
                Column(verticalArrangement = Arrangement.spacedBy(FilterGap)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(FilterGap),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        DateField("FROM · DD-MM-YYYY", state.from, "report-from", invalid, onFrom)
                        DateField("TO · DD-MM-YYYY", state.to, "report-to", invalid, onTo)
                        RunButton(state, canRun, onRun)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        PresetKicker()
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(FilterGap),
                            verticalArrangement = Arrangement.spacedBy(FilterGap),
                        ) {
                            PresetButtons(state, today, onFrom, onTo, onRun)
                        }
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FilterGap),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    DateField("FROM · DD-MM-YYYY", state.from, "report-from", invalid, onFrom)
                    DateField("TO · DD-MM-YYYY", state.to, "report-to", invalid, onTo)
                    RunButton(state, canRun, onRun)
                    Column(
                        Modifier.padding(start = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        PresetKicker()
                        Row(horizontalArrangement = Arrangement.spacedBy(FilterGap)) {
                            PresetButtons(state, today, onFrom, onTo, onRun)
                        }
                    }
                }
            }
        }
        if (rangeError != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(tokens.invalidFill, DeskStyle.controlShape)
                    .border(1.dp, tokens.invalidOutline, DeskStyle.controlShape)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("report-range-error"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("!", fontFamily = DipiMono, fontWeight = FontWeight.Medium, color = tokens.invalidText)
                Text(
                    rangeError,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = tokens.invalidText,
                )
            }
        }
    }
}

@Composable
private fun PresetKicker() {
    Text(
        "PRESETS",
        fontFamily = DipiMono,
        fontWeight = FontWeight.Medium,
        fontSize = 9.sp,
        letterSpacing = 0.14.em,
        color = LocalDipi.current.muted,
    )
}

@Composable
private fun PresetButtons(
    state: CourseReportUi,
    today: LocalDate,
    onFrom: (String) -> Unit,
    onTo: (String) -> Unit,
    onRun: () -> Unit,
) {
    ReportPreset.entries.forEach { preset ->
        PresetChip(preset, enabled = !state.running) {
            val (from, to) = reportPresetRange(preset, today)
            onFrom(from)
            onTo(to)
            onRun()
        }
    }
}

@Composable
private fun PresetChip(preset: ReportPreset, enabled: Boolean, onClick: () -> Unit) {
    val c = LocalDipi.current
    Box(
        Modifier
            .height(48.dp)
            .deskCard(
                shape = DeskStyle.controlShape,
                fill = c.field,
                border = c.hairline,
                elevation = 0.dp,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp)
            .testTag(preset.testTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            preset.label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = c.foreground,
        )
    }
}

@Composable
private fun RunButton(state: CourseReportUi, enabled: Boolean, onRun: () -> Unit) {
    val c = LocalDipi.current
    val fill = when {
        state.running -> c.accentPressed
        !enabled -> c.accent.copy(alpha = 0.38f)
        else -> c.accent
    }
    Box(
        Modifier
            .height(48.dp)
            .deskCard(
                shape = DeskStyle.controlShape,
                fill = fill,
                border = fill,
                elevation = 0.dp,
            )
            .clickable(enabled = enabled, onClick = onRun)
            .padding(horizontal = RunSidePad)
            .testTag("report-run"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (state.running) "RUNNING…" else "RUN",
            fontFamily = DipiCondensed,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            letterSpacing = 0.06.em,
            color = Color.White,
        )
    }
}

@Composable
private fun DateField(
    label: String,
    value: String,
    tag: String,
    invalid: Boolean,
    onChange: (String) -> Unit,
) {
    val c = LocalDipi.current
    val tokens = courseReportTokens()
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
                .width(DateFieldW)
                .height(DateFieldH)
                .testTag("$tag-box")
                .deskCard(
                    shape = DeskStyle.controlShape,
                    fill = c.field,
                    border = if (invalid) tokens.invalidBorder else tokens.validBorder,
                    elevation = 0.dp,
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = displayDeskDate(value),
                onValueChange = { onChange(parseDeskDate(it)) },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = DipiMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 17.sp,
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
        title = "Choose report dates",
        body = EmptyRangeCopy,
        tag = "report-first-open",
    )
}

@Composable
private fun Running(state: CourseReportUi) {
    Message(
        title = "Asking the desk for ${displayDeskDate(state.from)} → ${displayDeskDate(state.to)}.",
        body = "Large date ranges take longer.",
        tag = "report-running",
    )
}

@Composable
private fun EmptyRange() {
    Message(
        title = EmptyRangeCopy,
        body = "",
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
        if (body.isNotBlank()) {
            Text(body, fontSize = 13.sp, lineHeight = 19.sp, color = c.muted)
        }
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
    val industry = LocalIndustry.current
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
                color = industry.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LocalDeskColors.current.whiteSurface)
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

@Composable
private fun Loaded(state: CourseReportUi) {
    val report = state.report ?: return
    val industry = LocalIndustry.current
    val tokens = courseReportTokens()
    val hScroll = rememberScrollState()
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = TablePad)) {
            val sevak = report.grandTotal.sevakTotal
            val ran = buildString {
                append("${report.rows.size} COURSES · ${report.grandTotal.rollTotal} STUDENTS · $sevak SEVAK")
                if (!state.ranAt.isNullOrBlank()) append(" · RAN ${state.ranAt}")
            }
            Text(
                ran,
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp,
                letterSpacing = 0.14.em,
                color = industry.neutral500,
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .testTag("report-run-strip"),
            )
            Text(
                ColumnGlossary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = industry.neutral600,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .testTag("report-column-glossary"),
            )
        }
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = TablePad),
        ) {
            val metrics = tableMetrics(maxWidth)
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    GroupCaps(metrics, hScroll)
                    ColumnHeaders(metrics, tokens, hScroll)
                    report.rows.forEach { row ->
                        ReportRow(row, metrics, tokens, hScroll)
                    }
                }
                GrandTotalFooter(report, metrics, tokens, hScroll)
            }
        }
    }
}

private data class ReportTableMetrics(
    val courseWidth: Dp,
    val scroll: Boolean,
)

private fun tableMetrics(innerWidth: Dp): ReportTableMetrics {
    val numeric = CellW * 12 + RollW + GroupGap * 4
    val courseWidth = max(CourseMinW, innerWidth - numeric)
    return ReportTableMetrics(
        courseWidth = courseWidth,
        scroll = courseWidth + numeric > innerWidth,
    )
}

private fun Modifier.reportTableRow(hScroll: ScrollState, scroll: Boolean): Modifier =
    fillMaxWidth().then(if (scroll) horizontalScroll(hScroll) else this)

@Composable
private fun GroupCaps(metrics: ReportTableMetrics, hScroll: ScrollState) {
    val industry = LocalIndustry.current
    Row(
        Modifier
            .reportTableRow(hScroll, metrics.scroll)
            .height(22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(metrics.courseWidth))
        GroupCap("NEW", CellW * 3, industry.neutral500)
        Spacer(Modifier.width(GroupGap))
        GroupCap("OLD", CellW * 3, industry.neutral500)
        Spacer(Modifier.width(GroupGap))
        GroupCap("ROLL TOTAL", RollW, industry.neutral500)
        Spacer(Modifier.width(GroupGap))
        GroupCap("SEVAK", CellW * 3, industry.neutral500)
        Spacer(Modifier.width(GroupGap))
        GroupCap("TEACHERS", CellW * 3, industry.neutral500)
    }
}

@Composable
private fun GroupCap(name: String, width: Dp, color: Color) {
    Text(
        name,
        fontFamily = DipiMono,
        fontWeight = FontWeight.Medium,
        fontSize = 9.sp,
        letterSpacing = 0.08.em,
        textAlign = TextAlign.Center,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        modifier = Modifier.width(width),
    )
}

private val HEADERS = listOf("M", "F", "T", "M", "F", "T", "TOTAL", "M", "F", "T", "C", "A", "TR")

@Composable
private fun ColumnHeaders(
    metrics: ReportTableMetrics,
    tokens: CourseReportTokens,
    hScroll: ScrollState,
) {
    val industry = LocalIndustry.current
    Row(
        Modifier
            .reportTableRow(hScroll, metrics.scroll)
            .height(28.dp)
            .bottomRule(industry.neutral400),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "COURSE",
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            letterSpacing = 0.14.em,
            color = industry.neutral500,
            modifier = Modifier.width(metrics.courseWidth),
        )
        HEADERS.forEachIndexed { i, h ->
            if (i == 3 || i == 6 || i == 7 || i == 10) Spacer(Modifier.width(GroupGap))
            Figure(
                h,
                width = if (i == 6) RollW else CellW,
                banded = i == 6,
                tint = tokens.rollTint,
                header = true,
                rollTag = i == 6,
            )
        }
    }
}

@Composable
private fun ReportRow(
    row: CourseReportRow,
    metrics: ReportTableMetrics,
    tokens: CourseReportTokens,
    hScroll: ScrollState,
) {
    val industry = LocalIndustry.current
    val name = row.parsed
    val teachers = row.displayTeacherNames()
    Row(
        Modifier
            .reportTableRow(hScroll, metrics.scroll)
            .height(IntrinsicSize.Min)
            .heightIn(min = 52.dp)
            .bottomHairline(industry.neutral200),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(metrics.courseWidth).padding(end = 10.dp, top = 8.dp, bottom = 8.dp)) {
            Text(
                name.type,
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 19.sp,
                lineHeight = 21.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = industry.text,
            )
            if (!name.raw) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        name.year,
                        fontFamily = DipiMono,
                        fontSize = 13.sp,
                        color = industry.neutral600,
                    )
                    Text(
                        name.dates,
                        fontFamily = DipiMono,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = industry.neutral600,
                    )
                }
            }
            if (teachers.isNotEmpty()) {
                Text(
                    teachers.joinToString(" · "),
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = industry.neutral600,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .testTag("report-teacher-names"),
                )
            }
        }
        CountCells(row.counts, tokens, rollTag = true, stretch = true)
    }
}

@Composable
private fun CountCells(
    c: CourseReportCounts,
    tokens: CourseReportTokens,
    bold: Boolean = false,
    rollTag: Boolean = false,
    stretch: Boolean = false,
) {
    val values = listOf(
        c.newMale, c.newFemale, c.newTotal,
        c.oldMale, c.oldFemale, c.oldTotal,
        c.rollTotal,
        c.sevakMale, c.sevakFemale, c.sevakTotal,
        c.teacherConducting, c.teacherAssistant, c.teacherTrainee,
    )
    values.forEachIndexed { i, v ->
        if (i == 3 || i == 6 || i == 7 || i == 10) Spacer(Modifier.width(GroupGap))
        Figure(
            "$v",
            width = if (i == 6) RollW else CellW,
            banded = i == 6,
            tint = tokens.rollTint,
            bold = bold && i == 6,
            rollTag = rollTag && i == 6,
            stretch = stretch,
        )
    }
}

@Composable
private fun Figure(
    text: String,
    width: Dp,
    banded: Boolean,
    tint: Color,
    header: Boolean = false,
    bold: Boolean = false,
    rollTag: Boolean = false,
    stretch: Boolean = false,
) {
    val industry = LocalIndustry.current
    Box(
        Modifier
            .width(width)
            .then(
                when {
                    header -> Modifier.height(28.dp)
                    stretch -> Modifier.fillMaxHeight()
                    else -> Modifier
                },
            )
            .background(if (banded) tint else Color.Transparent)
            .then(if (rollTag) Modifier.testTag("report-col-roll") else Modifier),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text,
            fontFamily = DipiMono,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = if (header) 9.sp else 15.sp,
            letterSpacing = if (header) 0.14.em else 0.em,
            color = if (header) industry.neutral500 else industry.text,
            modifier = Modifier.padding(end = 6.dp),
        )
    }
}

@Composable
private fun GrandTotalFooter(
    report: CourseReport,
    metrics: ReportTableMetrics,
    tokens: CourseReportTokens,
    hScroll: ScrollState,
) {
    val industry = LocalIndustry.current
    Column(Modifier.fillMaxWidth().testTag("report-grand-total")) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(industry.neutral900))
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .background(industry.neutral100)
                .padding(vertical = 12.dp)
                .reportTableRow(hScroll, metrics.scroll),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.width(metrics.courseWidth).padding(end = 10.dp)) {
                Text(
                    "GRAND TOTAL",
                    fontFamily = DipiMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 0.17.em,
                    color = industry.neutral700,
                )
                if (report.from.isNotBlank()) {
                    Text(
                        "${displayDeskDate(report.from)} → ${displayDeskDate(report.to)} · ${report.rows.size} courses",
                        fontSize = 11.5.sp,
                        color = industry.neutral600,
                    )
                }
                Text(
                    RollOnlyNote,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = industry.neutral600,
                    modifier = Modifier.testTag("report-roll-note"),
                )
            }
            CountCells(report.grandTotal, tokens, bold = true, rollTag = true, stretch = false)
        }
    }
}

private fun Modifier.bottomRule(color: Color): Modifier = drawBottom(color, 1.dp)

private fun Modifier.bottomHairline(color: Color): Modifier = drawBottom(color, 1.dp)

private fun Modifier.drawBottom(color: Color, thickness: Dp) = drawBehind {
    val h = thickness.toPx()
    drawRect(color = color, topLeft = Offset(0f, size.height - h), size = Size(size.width, h))
}

/** Print-only HTML for the native report — no rail, no accent fills. */
fun courseReportPrintHtml(report: CourseReport): String {
    fun cells(c: CourseReportCounts) = listOf(
        c.newMale, c.newFemale, c.newTotal,
        c.oldMale, c.oldFemale, c.oldTotal,
        c.rollTotal,
        c.sevakMale, c.sevakFemale, c.sevakTotal,
        c.teacherConducting, c.teacherAssistant, c.teacherTrainee,
    ).joinToString("") { "<td>$it</td>" }
    val rows = report.rows.joinToString("") { row ->
        val names = row.displayTeacherNames().joinToString("; ")
        "<tr><td>${row.course}</td>${cells(row.counts)}<td>$names</td></tr>"
    }
    return """
        <!doctype html><html><head><meta charset="utf-8">
        <style>
          body{font:12pt/1.35 sans-serif;color:#111;margin:12mm}
          table{width:100%;border-collapse:collapse}
          th,td{border-bottom:1px solid #ccc;padding:4px 6px;text-align:right}
          th:first-child,td:first-child,th:last-child,td:last-child{text-align:left}
        </style></head><body>
        <h1>Course report</h1>
        <p>${displayDeskDate(report.from)} → ${displayDeskDate(report.to)}</p>
        <table>
          <thead><tr><th>Course</th>
            <th>NM</th><th>NF</th><th>NT</th>
            <th>OM</th><th>OF</th><th>OT</th>
            <th>Roll</th>
            <th>SM</th><th>SF</th><th>ST</th>
            <th>C</th><th>A</th><th>TR</th>
            <th>Teachers</th>
          </tr></thead>
          <tbody>$rows</tbody>
          <tfoot><tr><td>Grand total</td>${cells(report.grandTotal)}<td></td></tr></tfoot>
        </table>
        </body></html>
    """.trimIndent()
}
