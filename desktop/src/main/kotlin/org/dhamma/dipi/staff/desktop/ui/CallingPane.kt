package org.dhamma.dipi.staff.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import org.dhamma.dipi.staff.desktop.derive.CALL_OUTCOMES
import org.dhamma.dipi.staff.desktop.derive.deskCallCounts
import org.dhamma.dipi.staff.desktop.derive.deskCallMeta
import org.dhamma.dipi.staff.desktop.derive.deskCallRows
import org.dhamma.dipi.staff.desktop.derive.deskGenderScope
import org.dhamma.dipi.staff.desktop.derive.deskRoll
import org.dhamma.dipi.staff.desktop.derive.deskSeniorityScope
import org.dhamma.dipi.staff.desktop.theme.LocalDipi
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.CallRecord

@Composable
fun CallingPane(
    rows: List<ApplicantCard>,
    outcomes: Map<ApplicantId, CallRecord>,
    filter: String,
    onFilter: (String) -> Unit,
    onOutcome: (ApplicantCard, String) -> Unit,
    onDial: (ApplicantCard) -> Unit,
    onWhatsApp: (ApplicantCard) -> Unit,
    onNote: (ApplicantCard, String) -> Unit,
    gender: String,
    seniority: String,
    onGender: (String) -> Unit,
    onSeniority: (String) -> Unit,
) {
    val c = LocalDipi.current
    val roll = deskRoll(rows, deskGenderScope(gender), deskSeniorityScope(seniority))
    val list = deskCallRows(roll, outcomes, filter)
    val counts = deskCallCounts(roll, outcomes)
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Segmented(listOf("To call") + CALL_OUTCOMES, filter, onFilter, counts = counts)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Segmented(listOf("Both", "Male", "Female"), gender, onGender, Modifier.weight(1f))
            Segmented(listOf("Both", "New", "Old"), seniority, onSeniority, Modifier.weight(1f))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(list, key = { it.id.value }) { card ->
                val rec = outcomes[card.id]
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(DeskShape)
                        .background(c.field)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(card.displayName, color = c.foreground, fontWeight = FontWeight.Medium)
                            Text(
                                listOfNotNull(card.mobile, deskCallMeta(rec, System.currentTimeMillis())).joinToString(" · "),
                                color = c.muted,
                                fontSize = 12.sp,
                            )
                        }
                        DeskButton("Call", { onDial(card) })
                        DeskButton("WA", { onWhatsApp(card) })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CALL_OUTCOMES.forEach { o ->
                            DeskButton(o, { onOutcome(card, o) }, primary = rec?.outcome == o)
                        }
                    }
                    DeskField(rec?.note.orEmpty(), { onNote(card, it) }, Modifier.fillMaxWidth(), placeholder = "Note (stays on this Deck)")
                }
            }
        }
    }
}
