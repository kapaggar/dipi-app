package org.dhamma.dipi.staff.desk

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import org.dhamma.dipi.staff.ui.theme.DeskKicker
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.Industry
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
) {
    val total = roll.size
    val inCount = roll.count { deskCheckedIn(it, checkIns) }
    val pct = if (total == 0) 0 else (inCount * 100) / total
    val callList = deskCallList(roll)
    val logged = callList.count { it.id in callOutcomes }
    val toCall = callList.size - logged
    val findings = deskFindings(flagged)
    val fTotal = deskFindingCount(flagged)
    val mustFix = deskMustFixCount(flagged)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Text(
            "$total on the roll · $inCount checked in",
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = Industry.neutral700,
            modifier = Modifier.padding(bottom = 14.dp),
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BoardTile("$total", "ARRIVING TODAY", "$total confirmed", Modifier.weight(1f)) {
                onGoto(DeskSection.CheckIn)
            }
            BoardTile("$inCount", "CHECKED IN", "$pct% of the roll", Modifier.weight(1f)) {
                onGoto(DeskSection.CheckIn)
            }
            BoardTile("$toCall", "STILL TO CALL", "$logged logged this round", Modifier.weight(1f)) {
                onGoto(DeskSection.Calling)
            }
            BoardTile("$fTotal", "NEEDS ATTENTION", "across ${findings.size} checks", Modifier.weight(1f)) {
                onGoto(DeskSection.Audit)
            }
        }

        DeskKicker("NEXT", Industry.neutral600, Modifier.padding(top = 18.dp, bottom = 8.dp))
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            BoardAction("Check in arrivals", "${total - inCount} still to arrive") {
                onGoto(DeskSection.CheckIn)
            }
            BoardAction("Clear audit findings", "$fTotal findings · $mustFix must fix") {
                onGoto(DeskSection.Audit)
            }
            BoardAction("Finish the call round", "$toCall numbers left") {
                onGoto(DeskSection.Calling)
            }
        }

        Text(
            "SHEETS & EXPORTS",
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 9.5.sp,
            letterSpacing = 1.7.sp,
            color = Industry.neutral600,
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
                    fill = ChipFill,
                    border = ChipBorder,
                    elevation = 0.dp,
                )
                .clickable { onExport(label) }
                .padding(horizontal = 14.dp)
                .testTag("export-chip"),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DeskIcon(DeskIconKind.Download, 16.dp, Industry.accent300)
            Text(label, fontSize = 14.sp, maxLines = 2, color = Industry.neutral700)
        }
    }
}

private val ChipFill = Color(0xFFFCFCFD)
private val ChipBorder = Color(0xFFE7E7EA)

@Composable
private fun BoardTile(
    number: String,
    label: String,
    note: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(112.dp)
            .deskCard(shape = CardShape, elevation = 1.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 12.dp)
            .testTag("board-stat"),
    ) {
        Column(Modifier.fillMaxSize()) {
            Text(
                number,
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.Bold,
                fontSize = 38.sp,
                lineHeight = 38.sp,
                letterSpacing = (-0.02).em,
                color = Industry.accent800,
            )
            Text(
                label,
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                lineHeight = 10.sp,
                letterSpacing = 0.16.em,
                color = Industry.neutral700,
                maxLines = 1,
                modifier = Modifier.padding(top = 9.dp),
            )
            Text(
                note,
                fontSize = 12.5.sp,
                lineHeight = 12.5.sp,
                color = Industry.neutral500,
                maxLines = 1,
                modifier = Modifier.padding(top = 7.dp, end = 18.dp),
            )
        }
        // Overlay so the pale accent300 arrow is not clipped by the note row.
        Text(
            "→",
            fontSize = 14.sp,
            color = Industry.accent300,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .testTag("board-stat-arrow"),
        )
    }
}

@Composable
private fun BoardAction(label: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(58.dp)
            .deskCard(shape = CardShape, elevation = 1.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp)
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
                color = Industry.text,
                maxLines = 1,
            )
            Text(
                sub,
                fontSize = 12.5.sp,
                lineHeight = 12.5.sp,
                color = Industry.neutral600,
                maxLines = 1,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        DeskIcon(DeskIconKind.ArrowRight, 17.dp, Industry.accent400)
    }
}
