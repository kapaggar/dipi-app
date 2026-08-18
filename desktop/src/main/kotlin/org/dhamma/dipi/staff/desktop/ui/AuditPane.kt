package org.dhamma.dipi.staff.desktop.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.desktop.derive.FINDING_SECTIONS
import org.dhamma.dipi.staff.desktop.derive.deskFindings
import org.dhamma.dipi.staff.desktop.theme.LocalDipi
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.AuditSeverity

@Composable
fun AuditPane(
    flagged: List<ApplicantCard>,
    selectedCode: String?,
    onSelect: (String) -> Unit,
    onBatch: (String, String) -> Unit,
    onOpen: (ApplicantCard) -> Unit,
) {
    val c = LocalDipi.current
    val findings = deskFindings(flagged)
    val selected = findings.firstOrNull { it.code == selectedCode } ?: findings.firstOrNull()
    Row(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(0.48f).fillMaxHeight().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FINDING_SECTIONS.forEach { (sev, label) ->
                val group = findings.filter { it.severity == sev }
                if (group.isEmpty()) return@forEach
                item(label) {
                    Text(label, color = if (sev == AuditSeverity.HARD) c.hard else c.muted, fontSize = 11.sp, letterSpacing = 1.sp)
                }
                items(group, key = { it.code }) { finding ->
                    val on = finding.code == selected?.code
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(DeskShape)
                            .background(if (on) c.tint else c.field)
                            .border(1.dp, if (on) c.accent else c.hairline, DeskShape)
                            .clickable { onSelect(finding.code) }
                            .padding(12.dp),
                    ) {
                        Text(finding.title, color = c.foreground, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("${finding.people.size} people", color = c.muted, fontSize = 12.sp)
                    }
                }
            }
        }
        VHairline(Modifier.fillMaxHeight())
        Column(Modifier.weight(0.52f).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (selected == null) {
                Text("No findings on this course", color = c.muted)
                return@Column
            }
            Text(selected.title, color = c.foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            selected.batchLabel?.let { DeskButton(it, { onBatch(selected.code, it) }, primary = true) }
            selected.people.forEach { person ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(DeskShape)
                        .background(c.field)
                        .clickable { onOpen(person.card) }
                        .padding(12.dp),
                ) {
                    Text(person.card.displayName, color = c.foreground, fontWeight = FontWeight.Medium)
                    Text(person.offendingValue, color = c.muted, fontSize = 13.sp)
                }
            }
        }
    }
}
