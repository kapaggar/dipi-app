package org.dhamma.dipi.staff.desk

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.LocalDeskColors
import org.dhamma.dipi.staff.ui.theme.LocalDarkTheme
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import org.dhamma.dipi.staff.ui.theme.LocalIndustry
import org.dhamma.dipi.staff.ui.theme.deskCard

/**
 * One 3×3 of equal cells. Day-0 first, Course summary in the grid (not a
 * fourth line). Valuable list stays in [org.dhamma.dipi.staff.model.SheetExport]
 * and the phone hub; it is not a Board cell. Male/Female PDF stay gone.
 */
private val BOARD_EXPORTS = listOf(
    "Day 0 list", "Day 0 summary", "Course summary",
    "Student chit", "Checking slip", "Seating plan",
    "Teacher list", "Manager list", "Laundry list",
)

private const val GRID_COLUMNS = 3

private val CardShape = RoundedCornerShape(8.dp)
private val CellShape = RoundedCornerShape(8.dp)

/**
 * The first thing on screen at 09:00: four live numbers carry the
 * navigation, three verb-first rows say what to do next, and the nine
 * exports sit in one 3×3 — they are exports, not decisions.
 */
@Composable
fun BoardPane(
    roll: List<ApplicantCard>,
    checkIns: Map<ApplicantId, CheckInRecord>,
    flagged: List<ApplicantCard>,
    callOutcomes: Map<ApplicantId, String>,
    onGoto: (DeskSection) -> Unit,
    onExport: (String) -> Unit,
    sourceRows: List<ApplicantCard> = roll,
    reconcile: Boolean = false,
) {
    val industry = LocalIndustry.current
    val total = roll.size
    val inCount = roll.count { deskCheckedIn(it, checkIns) }
    val eligible = sourceRows.count { it.confNo != null }
    val offRoll = sourceRows.filter { it.confNo != null && deskOffRollStatus(it.status.value) }
        .groupingBy { it.status.value }.eachCount()
    val rollFormula = "$total = $eligible" + offRoll.entries.joinToString("") { " − ${it.value} ${it.key}" }
    val held = roll.count(::deskHeld)
    val remaining = total - inCount
    val callList = deskCallList(deskCallingRoll(roll, checkIns, reconcile))
    val logged = callList.count { deskCallOutcome(callOutcomes[it.id]).isNotBlank() }
    val toCall = callList.size - logged
    val fTotal = deskFindingCount(flagged)
    val mustFix = deskMustFixCount(flagged)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(
            "All applicants · $total on the roll · $inCount checked in · $held WaitList held" +
                if (reconcile) " · reconciliation" else "",
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = industry.neutral700,
            modifier = Modifier.padding(bottom = 14.dp),
        )

        BoxWithConstraints {
            val columns = if (maxWidth < 910.dp) 2 else 4
            val tiles = listOf(
                listOf("$total", "ARRIVING ROLL", rollFormula),
                listOf("$inCount", "CHECKED IN", "$inCount of $total on roll"),
                listOf(if (reconcile) "$remaining" else "$toCall",
                    if (reconcile) "TO RECONCILE" else "STILL TO CALL",
                    if (reconcile) "$remaining = $total − $inCount" else "$toCall = ${callList.size} − $logged logged"),
                listOf("$fTotal", "NEEDS ATTENTION",
                    "$fTotal = ${flagged.sumOf { c -> c.flags.count { it.severity == org.dhamma.dipi.staff.model.AuditSeverity.HARD } }} hard · ${flagged.sumOf { c -> c.flags.count { it.severity == org.dhamma.dipi.staff.model.AuditSeverity.SAFETY } }} safety · ${flagged.sumOf { c -> c.flags.count { it.severity == org.dhamma.dipi.staff.model.AuditSeverity.SOFT } }} soft"),
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                tiles.withIndex().chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { (index, t) ->
                            BoardTile(t[0], t[1], t[2], index, Modifier.weight(1f)) {
                                onGoto(when (index) { 2 -> DeskSection.Calling; 3 -> DeskSection.Audit; else -> DeskSection.CheckIn })
                            }
                        }
                    }
                }
            }
        }
        Text("App roll: confirmation-number holders minus off-roll statuses. Day 0 and Course summary retain the desk site's figures.",
            fontSize = 14.sp, lineHeight = 20.sp, color = industry.neutral600,
            modifier = Modifier.padding(top = 10.dp))

        DeskKicker(if (reconcile) "NEXT · RECONCILE" else "NEXT", industry.neutral600, Modifier.padding(top = 18.dp, bottom = 8.dp))
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            if (reconcile) {
                BoardAction("Attendance check", "$remaining = $total on roll − $inCount checked in") {
                    onGoto(DeskSection.CheckIn)
                }
                BoardAction("Print Course summary", "Open the institutional course record") { onExport("Course summary") }
                BoardAction("Print Day 0 summary", "Open the desk site's arrival figures") { onExport("Day 0 summary") }
            } else {
                BoardAction("Check in arrivals", "$remaining = $total on roll − $inCount checked in") { onGoto(DeskSection.CheckIn) }
                BoardAction("Clear audit findings", "$fTotal findings · $mustFix must fix") { onGoto(DeskSection.Audit) }
                BoardAction("Finish the call round", "$toCall = ${callList.size} reachable − $logged logged") { onGoto(DeskSection.Calling) }
            }
        }

        Text(
            "SHEETS & EXPORTS",
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            letterSpacing = 1.7.sp,
            color = industry.neutral600,
            modifier = Modifier.padding(top = 18.dp, bottom = 10.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .testTag("export-grid"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BOARD_EXPORTS.chunked(GRID_COLUMNS).forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { label ->
                        ExportCell(label, Modifier.weight(1f), onExport)
                    }
                }
            }
        }
    }
}

/**
 * One equal cell in the 3×3. Taller than the old 38dp chips so the nine
 * tap targets read as a grid, not a shelf of pills. Course summary is a
 * normal cell and still carries `export-day11`.
 */
@Composable
private fun ExportCell(label: String, modifier: Modifier, onExport: (String) -> Unit) {
    val industry = LocalIndustry.current
    val deskColors = LocalDeskColors.current
    val dark = LocalDarkTheme.current
    val dipi = LocalDipi.current
    val day11 = label == "Course summary"
    Box(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .then(if (day11) Modifier.testTag("export-day11") else Modifier),
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .deskCard(
                    shape = CellShape,
                    fill = deskColors.exportTile,
                    border = if (dark) dipi.hairline else ChipBorder,
                    elevation = 0.dp,
                )
                .clickable { onExport(label) }
                .padding(horizontal = 14.dp)
                .testTag("export-chip"),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DeskIcon(DeskIconKind.Download, 16.dp, industry.accent300)
            Text(label, fontSize = 14.sp, maxLines = 2, color = industry.neutral700)
        }
    }
}

private val ChipBorder = Color(0xFFE7E7EA)

@Composable
private fun BoardTile(
    number: String,
    label: String,
    note: String,
    index: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val industry = LocalIndustry.current
    val deskColors = LocalDeskColors.current
    Box(
        modifier
            .heightIn(min = 156.dp)
            .deskCard(shape = CardShape, elevation = 1.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 12.dp)
            .testTag("board-stat"),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                number,
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.Bold,
                fontSize = 38.sp,
                lineHeight = 38.sp,
                letterSpacing = (-0.02).em,
                color = industry.accent800,
            )
            Text(
                label,
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.16.em,
                color = industry.neutral700,
                maxLines = 4,
                modifier = Modifier.padding(top = 9.dp),
            )
            Text(
                note,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = deskColors.caption,
                maxLines = 4,
                modifier = Modifier
                    .padding(top = 7.dp, end = 18.dp)
                    .testTag("theme-board-caption-$index"),
            )
        }
        // Overlay so the pale accent300 arrow is not clipped by the note row.
        Text(
            "→",
            fontSize = 14.sp,
            color = industry.accent300,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .testTag("board-stat-arrow"),
        )
    }
}

@Composable
private fun BoardAction(label: String, sub: String, onClick: () -> Unit) {
    val industry = LocalIndustry.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .deskCard(shape = CardShape, elevation = 1.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("board-next"),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label,
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.01.em,
                color = industry.text,
                maxLines = 4,
            )
            Text(
                sub,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = industry.neutral600,
                maxLines = 4,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        DeskIcon(DeskIconKind.ArrowRight, 17.dp, industry.accent400)
    }
}
