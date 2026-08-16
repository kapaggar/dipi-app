package org.dhamma.dipi.staff.applicants

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.ui.FilterChip
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.LocalDipi

fun callingQueue(rows: List<ApplicantCard>): List<ApplicantCard> =
    rows.filter {
        val s = it.status.normalize()
        (s == "confirmed" || s == "expected") && !it.mobile.isNullOrBlank()
    }

fun callingVisible(
    rows: List<ApplicantCard>,
    callState: Map<ApplicantId, String>,
    filter: String,
): List<ApplicantCard> {
    val queue = callingQueue(rows)
    return when (filter.lowercase()) {
        "reached" -> queue.filter { callState[it.id] == "Reached" }
        "no answer" -> queue.filter { callState[it.id] == "No answer" }
        "all" -> queue
        else -> queue.filter { callState[it.id].isNullOrBlank() || callState[it.id] == "To call" }
    }
}

@Composable
fun CallingScreen(
    rows: List<ApplicantCard>,
    callState: Map<ApplicantId, String> = emptyMap(),
    filter: String = "To call",
    onFilter: (String) -> Unit = {},
    onCallState: (ApplicantCard, String) -> Unit = { _, _ -> },
    onDial: (String) -> Unit = {},
    onOpen: (ApplicantCard) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val c = LocalDipi.current
    val visible = callingVisible(rows, callState, filter)
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .padding(20.dp),
    ) {
        Text("Calling students", fontFamily = DipiCondensed, fontSize = 22.sp, color = c.foreground)
        TextButton(onClick = onBack) { Text("Back") }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            listOf("To call", "Reached", "No answer", "All").forEach { chip ->
                FilterChip(chip, filter.equals(chip, ignoreCase = true)) { onFilter(chip) }
            }
        }
        if (visible.isEmpty()) {
            Text("No one to call on this course.", color = c.muted, modifier = Modifier.padding(top = 12.dp))
        } else {
            LazyColumn {
                items(visible, key = { it.id.value }) { card ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).clickable { onOpen(card) }) {
                            Text(card.displayName, fontFamily = DipiCondensed, fontSize = 16.sp, color = c.foreground)
                            Text(
                                "${card.status.value}  ${card.mobile}",
                                color = c.muted,
                                fontSize = 12.sp,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                                TextButton(onClick = { onCallState(card, "Reached") }) { Text("Reached") }
                                TextButton(onClick = { onCallState(card, "No answer") }) { Text("No answer") }
                            }
                        }
                        Text(
                            "Call",
                            color = androidx.compose.ui.graphics.Color.White,
                            fontFamily = DipiCondensed,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .background(c.accent, RoundedCornerShape(4.dp))
                                .clickable { card.mobile?.let(onDial) }
                                .heightIn(min = 48.dp)
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}
