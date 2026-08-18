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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.desktop.derive.deskGenderScope
import org.dhamma.dipi.staff.desktop.derive.deskScoped
import org.dhamma.dipi.staff.desktop.derive.deskSeniorityScope
import org.dhamma.dipi.staff.desktop.theme.LocalDipi
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.AuditSeverity
import org.dhamma.dipi.staff.model.SensitiveInfo

@Composable
fun ApplicationsPane(
    rows: List<ApplicantCard>,
    flagsById: Map<ApplicantId, List<org.dhamma.dipi.staff.model.AuditFlag>>,
    selectedId: ApplicantId?,
    onSelect: (ApplicantCard) -> Unit,
    onChangeStatus: (ApplicantCard) -> Unit,
    onDial: (String) -> Unit,
    onEdit: (ApplicantCard) -> Unit,
    counts: Map<String, Int>,
    selectedStatuses: Set<String>,
    onToggleStatus: (String) -> Unit,
    sensitiveById: Map<ApplicantId, SensitiveInfo>,
    gender: String,
    seniority: String,
    onGender: (String) -> Unit,
    onSeniority: (String) -> Unit,
    dark: Boolean,
    query: String,
    onQuery: (String) -> Unit,
) {
    val c = LocalDipi.current
    val scoped = deskScoped(rows, deskGenderScope(gender), deskSeniorityScope(seniority))
    val selected = scoped.firstOrNull { it.id == selectedId } ?: scoped.firstOrNull()
    Row(Modifier.fillMaxSize()) {
        Column(Modifier.weight(0.46f).fillMaxHeight().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DeskField(query, onQuery, Modifier.fillMaxWidth(), placeholder = "Name, conf no, phone…")
            FilterRow(counts, selectedStatuses, onToggleStatus)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Segmented(listOf("Both", "Male", "Female"), gender, onGender, Modifier.weight(1f))
                Segmented(listOf("Both", "New", "Old"), seniority, onSeniority, Modifier.weight(1f))
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                items(scoped, key = { it.id.value }) { card ->
                    val on = card.id == selected?.id
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(DeskShape)
                            .background(if (on) c.tint else c.field)
                            .border(1.dp, if (on) c.accent else c.hairline, DeskShape)
                            .clickable { onSelect(card) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(card.displayName, color = c.foreground, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(
                                listOfNotNull(card.confNo?.value, card.metaLine).joinToString(" · "),
                                color = c.muted,
                                fontSize = 12.sp,
                            )
                        }
                        StatusChip(card.status.value, dark)
                    }
                }
            }
        }
        VHairline(Modifier.fillMaxHeight())
        Column(Modifier.weight(0.54f).fillMaxHeight().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (selected == null) {
                Text("Select an applicant", color = c.muted)
                return@Column
            }
            Text(selected.displayName, color = c.foreground, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusChip(selected.status.value, dark)
                Text(selected.confNo?.value ?: "no conf", color = c.muted, fontSize = 14.sp)
            }
            Text(selected.metaLine, color = c.muted, fontSize = 14.sp)
            selected.mobile?.let { Text(it, color = c.foreground, fontSize = 15.sp) }
            selected.email?.let { Text(it, color = c.muted, fontSize = 13.sp) }
            val flags = flagsById[selected.id].orEmpty()
            if (flags.isNotEmpty()) {
                flags.forEach { flag ->
                    Text(
                        "${flag.label} · ${flag.detail}",
                        color = if (flag.severity == AuditSeverity.HARD) c.hard else c.safety,
                        fontSize = 13.sp,
                    )
                }
            }
            val sensitive = sensitiveById[selected.id]
            if (sensitive != null) {
                if (sensitive.idLabel != null) {
                    Text("${sensitive.idLabel}: ${sensitive.idNumber}", color = c.foreground, fontSize = 14.sp)
                }
                sensitive.health.forEach { (k, v) ->
                    Text("$k — $v", color = c.safety, fontSize = 13.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeskButton("Change status", { onChangeStatus(selected) }, primary = true)
                selected.mobile?.let { DeskButton("Call", { onDial(it) }) }
                DeskButton("Edit page", { onEdit(selected) })
            }
        }
    }
}
