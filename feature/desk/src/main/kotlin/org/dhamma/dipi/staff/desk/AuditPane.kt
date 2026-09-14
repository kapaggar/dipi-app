package org.dhamma.dipi.staff.desk

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.AuditSeverity
import org.dhamma.dipi.staff.ui.theme.DeskStyle
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.LocalIndustry
import org.dhamma.dipi.staff.ui.theme.deskCard

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
    val industry = LocalIndustry.current
    val findings = deskFindings(flagged)
    val total = deskFindingCount(flagged)
    val selected = findings.firstOrNull { it.code == selectedCode } ?: findings.firstOrNull()
    val compactDetail = LocalConfiguration.current.screenWidthDp - 190 < 910
    var detailOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = compactDetail && detailOpen) { detailOpen = false }

    Row(Modifier.fillMaxSize()) {
        if (compactDetail && detailOpen && selected != null) {
            FindingDetail(
                selected = selected,
                modifier = Modifier.fillMaxSize(),
                onBatch = onBatch,
                onOpen = onOpen,
                onBack = { detailOpen = false },
            )
        } else {
            Column(
            Modifier
                .then(if (compactDetail) Modifier.weight(1f) else Modifier.width(410.dp))
                .fillMaxHeight()
                .rightHairline(industry.neutral300)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            ) {
            Column(Modifier.padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DeskH2("All applicants · $total findings")
                DeskSub("Grouped by issue")
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FINDING_SECTIONS.forEach { (severity, kicker) ->
                    val section = findings.filter { it.severity == severity }
                    if (section.isEmpty()) return@forEach
                    Text(
                        kicker,
                        fontFamily = DipiMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        letterSpacing = 0.08.em,
                        color = industry.neutral600,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    section.forEach { finding ->
                        FindingRow(
                            finding,
                            on = !compactDetail && finding.code == selected?.code,
                            onSelect = {
                                onSelect(it)
                                if (compactDetail) detailOpen = true
                            },
                        )
                    }
                }
                if (findings.isEmpty()) {
                    DeskEmpty("Audit clean · nothing to fix.", Modifier.fillMaxWidth().padding(vertical = 30.dp))
                }
            }
            }
        }

        if (!compactDetail && selected != null) {
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
    val industry = LocalIndustry.current
    Row(
        Modifier
            .fillMaxWidth()
            .deskCard(
                fill = if (on) industry.accent100 else DeskStyle.cardFill,
                border = if (on) industry.accent else DeskStyle.cardBorder,
            )
            .clickable { onSelect(finding.code) }
            .heightIn(min = 48.dp)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                finding.title,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = industry.text,
            )
            Text(
                finding.code,
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = industry.neutral600,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${finding.people.size}",
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 20.sp,
                color = industry.accent800,
            )
            Text(
                severityBadge(finding).uppercase(),
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = 0.1.em,
                color = if (finding.mustFix) industry.accent800 else industry.neutral600,
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
    onBack: (() -> Unit)? = null,
) {
    val industry = LocalIndustry.current
    Column(
        modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 24.dp),
    ) {
        if (onBack != null) {
            Text(
                "← Back to list",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = industry.accent800,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onBack)
                    .padding(vertical = 14.dp),
            )
        }
        Text(
            selected.code,
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = industry.accent700,
        )
        Text(
            selected.title,
            fontFamily = DipiCondensed,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 30.sp,
            color = industry.text,
            modifier = Modifier.widthIn(max = 520.dp).padding(top = 3.dp),
        )
        Text(
            "${severityBadge(selected)} · ${selected.people.size} applications",
            fontSize = 14.sp,
            color = industry.neutral600,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )

        val batch = selected.batchLabel
        if (batch != null) {
            Row(
                Modifier
                    .padding(bottom = 20.dp)
                    .deskCard(
                        shape = DeskStyle.controlShape,
                        fill = industry.accent,
                        border = industry.accent,
                    )
                    .clickable { onBatch(selected.code, batch) }
                    .heightIn(min = 48.dp)
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

        Column(Modifier.fillMaxWidth().topHairline(industry.neutral300)) {
            selected.people.forEach { person ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .bottomHairline(industry.neutral200)
                        .padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        person.card.confNo?.display() ?: "-",
                        fontFamily = DipiMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = industry.neutral600,
                        modifier = Modifier.width(56.dp),
                    )
                    Text(
                        person.card.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = industry.text,
                        modifier = Modifier.width(180.dp),
                    )
                    Text(
                        person.offendingValue,
                        fontFamily = DipiMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = industry.accent800,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "Open",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = industry.text,
                        modifier = Modifier
                            .clip(DeskStyle.controlShape)
                            .border(1.dp, industry.neutral400, DeskStyle.controlShape)
                            .clickable { onOpen(person.card) }
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}
