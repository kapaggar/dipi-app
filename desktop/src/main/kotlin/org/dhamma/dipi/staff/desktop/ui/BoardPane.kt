package org.dhamma.dipi.staff.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.desktop.derive.DeskSection
import org.dhamma.dipi.staff.desktop.derive.deskCallList
import org.dhamma.dipi.staff.desktop.derive.deskCheckedIn
import org.dhamma.dipi.staff.desktop.derive.deskFindingCount
import org.dhamma.dipi.staff.desktop.derive.deskFindings
import org.dhamma.dipi.staff.desktop.derive.deskMustFixCount
import org.dhamma.dipi.staff.desktop.theme.LocalDipi
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.CheckInRecord

private val EXPORTS = listOf(
    "Day 0 list", "Day 0 summary", "Student chit", "Checking slip",
    "Male PDF", "Female PDF", "Teacher list", "Manager list",
    "Laundry list", "Valuable list", "Seating plan", "Course report",
)

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
    val c = LocalDipi.current
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
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            if (dayLabel != null) "$dayLabel at $centreName" else centreName,
            color = c.foreground,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("In", "$inCount / $total", "$pct%", DeskSection.CheckIn, onGoto, Modifier.weight(1f))
            StatTile("To call", "$toCall", "$logged logged", DeskSection.Calling, onGoto, Modifier.weight(1f))
            StatTile("Findings", "$fTotal", "$mustFix must fix", DeskSection.Audit, onGoto, Modifier.weight(1f))
            StatTile("On the roll", "$total", "${findings.size} kinds", DeskSection.Applications, onGoto, Modifier.weight(1f))
        }
        Text("SHEETS & EXPORTS", color = c.muted, fontSize = 11.sp, letterSpacing = 1.sp)
        EXPORTS.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { label ->
                    Text(
                        label,
                        color = c.muted,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .weight(1f)
                            .clip(DeskShape)
                            .border(1.dp, c.hairline, DeskShape)
                            .clickable { onExport(label) }
                            .padding(10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    title: String,
    value: String,
    sub: String,
    section: DeskSection,
    onGoto: (DeskSection) -> Unit,
    modifier: Modifier,
) {
    val c = LocalDipi.current
    Column(
        modifier
            .clip(DeskShape)
            .background(c.field)
            .border(1.dp, c.hairline, DeskShape)
            .clickable { onGoto(section) }
            .padding(14.dp),
    ) {
        Text(title, color = c.muted, fontSize = 12.sp)
        Text(value, color = c.foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(sub, color = c.accent, fontSize = 12.sp)
    }
}
