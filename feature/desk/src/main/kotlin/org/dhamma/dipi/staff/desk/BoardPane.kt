package org.dhamma.dipi.staff.desk

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.Industry
import org.dhamma.dipi.staff.ui.theme.blueprint

private val EXPORTS = listOf(
    "Day 0 list", "Day 0 summary", "Student chit", "Checking slip",
    "Male PDF", "Female PDF", "Teacher list", "Manager list",
    "Laundry list", "Valuable list", "Seating plan", "Course report",
)

/**
 * The first thing on screen at 09:00: four live numbers carry the
 * navigation, three verb-first rows say what to do next, and the twelve PDF
 * exports drop to small type — they are exports, not decisions.
 */
@Composable
fun BoardPane(
    centreName: String,
    dayLabel: String?,
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
            .padding(26.dp),
    ) {
        Column(Modifier.padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                if (dayLabel != null) "$dayLabel at $centreName" else centreName,
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                lineHeight = 40.sp,
                letterSpacing = (-0.01).em,
                color = Industry.text,
            )
            Text(
                "$total on the roll, $inCount already in their rooms. " +
                    "Everything below is a number you can act on — tap it.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Industry.neutral700,
                modifier = Modifier.widthIn(max = 640.dp),
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(bottom = 26.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
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

        DeskKicker("NEXT", Industry.neutral500, Modifier.padding(bottom = 10.dp))
        Column(
            Modifier.fillMaxWidth().padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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

        DeskKicker("SHEETS & EXPORTS · RARELY URGENT", Industry.neutral500, Modifier.padding(bottom = 10.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(Industry.neutral300)
                .padding(1.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            EXPORTS.chunked(4).forEach { rowItems ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    rowItems.forEach { label ->
                        Row(
                            Modifier
                                .weight(1f)
                                .background(Industry.bg)
                                .clickable { onExport(label) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DeskIcon(DeskIconKind.Download, 13.dp, Industry.accent)
                            Text(label, fontSize = 12.5.sp, maxLines = 1, color = Industry.neutral800)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardTile(
    number: String,
    label: String,
    note: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .blueprint(Industry.neutral400)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
    ) {
        Text(
            number,
            fontFamily = DipiCondensed,
            fontWeight = FontWeight.Bold,
            fontSize = 46.sp,
            lineHeight = 46.sp,
            letterSpacing = (-0.02).em,
            color = Industry.accent800,
        )
        Text(
            label,
            fontFamily = DipiCondensed,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            letterSpacing = 0.1.em,
            color = Industry.text,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(note, fontSize = 11.5.sp, color = Industry.neutral600)
    }
}

@Composable
private fun BoardAction(label: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .blueprint(Industry.neutral400)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label,
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 22.sp,
                color = Industry.text,
            )
            Text(sub, fontSize = 12.sp, color = Industry.neutral600)
        }
        DeskIcon(DeskIconKind.ArrowRight, 18.dp, Industry.accent)
    }
}
