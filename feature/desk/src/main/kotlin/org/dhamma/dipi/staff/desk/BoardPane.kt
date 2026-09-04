package org.dhamma.dipi.staff.desk

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
 * Three semantic shelves of four columns. Shelf 3 carries **three** chips
 * since v5 T4 moved Course report to the centre dashboard — the fourth cell
 * stays empty rather than letting three chips stretch across four columns
 * and read as more important than the eight above. The hole is honest.
 */
private val EXPORT_SHELVES = listOf(
    Shelf("ROLL SHEETS", "day 0", listOf("Day 0 list", "Day 0 summary", "Male PDF", "Female PDF")),
    Shelf(
        "DESK SLIPS",
        "printed and cut",
        listOf("Student chit", "Checking slip", "Seating plan", "Laundry list"),
    ),
    Shelf(
        "FOR THE TEAM",
        "teachers and managers",
        listOf("Teacher list", "Manager list", "Valuable list"),
    ),
)

private const val SHELF_COLUMNS = 4

private data class Shelf(val kicker: String, val qualifier: String, val labels: List<String>)

private val CardShape = RoundedCornerShape(8.dp)
private val ChipShape = RoundedCornerShape(6.dp)

/**
 * The first thing on screen at 09:00: four live numbers carry the
 * navigation, three verb-first rows say what to do next, and the twelve PDF
 * exports drop to small type — they are exports, not decisions. v4 densifies
 * all three bands so the whole Board lands on one fold.
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
            "$total on the roll, $inCount already in their rooms. " +
                "Everything below is a number you can act on — tap it.",
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

        Row(
            Modifier.padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "SHEETS & EXPORTS",
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 9.5.sp,
                letterSpacing = 1.7.sp,
                color = Industry.neutral600,
            )
            Text(
                "RARELY URGENT",
                fontFamily = DipiMono,
                fontWeight = FontWeight.Normal,
                fontSize = 9.5.sp,
                letterSpacing = 1.2.sp,
                color = Industry.neutral400,
            )
        }
        EXPORT_SHELVES.forEach { shelf ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .testTag("export-shelf-${shelf.kicker}"),
            ) {
                Row(
                    Modifier.padding(bottom = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DeskKicker(shelf.kicker, Industry.neutral500)
                    Text(
                        shelf.qualifier,
                        fontSize = 11.sp,
                        color = Industry.neutral400,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    shelf.labels.forEach { label ->
                        ExportChip(label, Modifier.weight(1f), onExport)
                    }
                    // The empty fourth cell: shelves are semantic, so the
                    // column rhythm holds even when a shelf loses a chip.
                    repeat(SHELF_COLUMNS - shelf.labels.size) {
                        Spacer(Modifier.weight(1f).testTag("export-shelf-gap"))
                    }
                }
            }
        }
        // Day 11 lands after the course; the design's own fourth line keeps it
        // out of the urgent shelves (v4 frame 1f, dc.html:579).
        Row(
            Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .height(40.dp)
                .deskCard(shape = ChipShape, elevation = 0.dp)
                .clickable { onExport("Course summary report") }
                .padding(horizontal = 13.dp)
                .testTag("export-day11"),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DeskIcon(DeskIconKind.Download, 13.dp, Industry.accent400)
            Text(
                "Day 11 · Course summary report",
                fontSize = 13.5.sp,
                maxLines = 1,
                color = Industry.neutral800,
                modifier = Modifier.weight(1f),
            )
            // Why it is not on a shelf: it is the only export that belongs
            // after the course has ended.
            Text(
                "END OF COURSE",
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp,
                letterSpacing = 1.2.sp,
                color = Industry.neutral400,
            )
        }
    }
}

/**
 * v5 T4 de-emphasises the exports rather than adding to them: paler border
 * and fill, 38dp instead of 40, `neutral600` label, `accent300` glyph. The
 * reading order on the Board is numbers → next actions → exports.
 */
@Composable
private fun ExportChip(label: String, modifier: Modifier, onExport: (String) -> Unit) {
    Row(
        modifier
            .height(38.dp)
            .deskCard(
                shape = ChipShape,
                fill = ChipFill,
                border = ChipBorder,
                elevation = 0.dp,
            )
            .clickable { onExport(label) }
            .padding(horizontal = 13.dp)
            .testTag("export-chip"),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DeskIcon(DeskIconKind.Download, 13.dp, Industry.accent300)
        Text(label, fontSize = 13.sp, maxLines = 1, color = Industry.neutral600)
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
