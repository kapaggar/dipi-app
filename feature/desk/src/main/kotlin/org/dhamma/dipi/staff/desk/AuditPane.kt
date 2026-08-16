package org.dhamma.dipi.staff.desk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.AuditSeverity
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.Industry
import org.dhamma.dipi.staff.ui.theme.blueprint
import org.dhamma.dipi.staff.ui.theme.blueprintMarks

/**
 * Audit: findings grouped by the check that fired, not by person — the user
 * fixes one kind of mistake at a time. The list is sectioned Hard → Safety →
 * Soft, mirroring the audit.js report. Where the fix is mechanical, one
 * button clears all of them, and the snackbar states what was preserved.
 */
@Composable
fun AuditPane(
    flagged: List<ApplicantCard>,
    selectedCode: String?,
    onSelect: (String) -> Unit,
    onBatch: (code: String, label: String) -> Unit,
    onOpen: (ApplicantCard) -> Unit,
) {
    val findings = deskFindings(flagged)
    val total = deskFindingCount(flagged)
    val selected = findings.firstOrNull { it.code == selectedCode } ?: findings.firstOrNull()

    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .width(410.dp)
                .fillMaxHeight()
                .rightHairline(Industry.neutral300)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Column(Modifier.padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DeskH2("$total findings")
                DeskSub("Grouped by the check that fired, not by person — fix one kind of mistake at a time.")
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FINDING_SECTIONS.forEach { (severity, kicker) ->
                    val section = findings.filter { it.severity == severity }
                    if (section.isEmpty()) return@forEach
                    Text(
                        kicker,
                        fontFamily = DipiMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        letterSpacing = 0.08.em,
                        color = Industry.neutral500,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    section.forEach { finding ->
                        FindingRow(finding, on = finding.code == selected?.code, onSelect = onSelect)
                    }
                }
                if (findings.isEmpty()) {
                    DeskEmpty("Audit clean · nothing to fix.", Modifier.fillMaxWidth().padding(vertical = 30.dp))
                }
            }
        }

        if (selected != null) {
            FindingDetail(selected, Modifier.weight(1f), onBatch = onBatch, onOpen = onOpen)
        }
    }
}

private fun severityBadge(finding: DeskFinding): String = when (finding.severity) {
    AuditSeverity.HARD -> "Must fix"
    AuditSeverity.SAFETY -> "Safety"
    AuditSeverity.SOFT -> "Check"
}

@Composable
private fun FindingRow(finding: DeskFinding, on: Boolean, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (on) Industry.accent100 else Color.Transparent)
            .blueprint(if (on) Industry.accent else Industry.neutral300)
            .clickable { onSelect(finding.code) }
            .padding(horizontal = 13.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                finding.title,
                fontSize = 13.5.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
                color = Industry.text,
            )
            Text(
                finding.code,
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 10.5.sp,
                color = Industry.neutral500,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${finding.people.size}",
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 20.sp,
                color = Industry.accent800,
            )
            Text(
                severityBadge(finding).uppercase(),
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 9.5.sp,
                letterSpacing = 0.1.em,
                color = if (finding.mustFix) Industry.accent800 else Industry.neutral600,
            )
        }
    }
}

@Composable
private fun FindingDetail(
    selected: DeskFinding,
    modifier: Modifier,
    onBatch: (code: String, label: String) -> Unit,
    onOpen: (ApplicantCard) -> Unit,
) {
    Column(
        modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 24.dp),
    ) {
        Text(
            selected.code,
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = Industry.accent700,
        )
        Text(
            selected.title,
            fontFamily = DipiCondensed,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 30.sp,
            color = Industry.text,
            modifier = Modifier.widthIn(max = 520.dp).padding(top = 3.dp),
        )
        Text(
            "${severityBadge(selected)} · ${selected.people.size} applications",
            fontSize = 12.5.sp,
            color = Industry.neutral600,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )

        val batch = selected.batchLabel
        if (batch != null) {
            Row(
                Modifier
                    .padding(bottom = 20.dp)
                    .blueprintMarks()
                    .background(Industry.accent)
                    .clickable { onBatch(selected.code, batch) }
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    batch.uppercase(),
                    fontFamily = DipiCondensed,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    letterSpacing = 0.04.em,
                    maxLines = 1,
                    color = Color.White,
                )
                DeskIcon(DeskIconKind.ArrowRight, 16.dp, Color.White)
            }
        }

        Column(Modifier.fillMaxWidth().topHairline(Industry.neutral300)) {
            selected.people.forEach { person ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .bottomHairline(Industry.neutral200)
                        .padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        person.card.confNo?.display() ?: "—",
                        fontFamily = DipiMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.5.sp,
                        color = Industry.neutral600,
                        modifier = Modifier.width(56.dp),
                    )
                    Text(
                        person.card.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Industry.text,
                        modifier = Modifier.width(180.dp),
                    )
                    Text(
                        person.offendingValue,
                        fontFamily = DipiMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Industry.accent800,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "Open",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = Industry.text,
                        modifier = Modifier
                            .border(1.dp, Industry.neutral400)
                            .clickable { onOpen(person.card) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}
