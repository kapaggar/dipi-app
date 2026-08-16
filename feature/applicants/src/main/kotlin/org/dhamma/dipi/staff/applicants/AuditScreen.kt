package org.dhamma.dipi.staff.applicants

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.AuditSeverity
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.LocalDipi

@Composable
fun AuditScreen(
    rows: List<ApplicantCard>,
    onOpen: (ApplicantCard) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val c = LocalDipi.current
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .padding(20.dp),
    ) {
        Text("Audit applications", fontFamily = DipiCondensed, fontSize = 22.sp, color = c.foreground)
        TextButton(onClick = onBack) { Text("Back") }
        if (rows.isEmpty()) {
            Text("No audit flags on this course.", color = c.muted, modifier = Modifier.padding(top = 12.dp))
        } else {
            LazyColumn {
                items(rows, key = { it.id.value }) { card ->
                    val hard = card.flags.count { it.severity == AuditSeverity.HARD }
                    val soft = card.flags.size - hard
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(card) }
                            .padding(vertical = 10.dp),
                    ) {
                        Text(card.displayName, fontFamily = DipiCondensed, fontSize = 16.sp, color = c.foreground)
                        Text(
                            "${card.status.value}  hard $hard  soft $soft",
                            color = c.muted,
                            fontSize = 12.sp,
                        )
                        Text(
                            card.flags.firstOrNull()?.label.orEmpty(),
                            color = if (hard > 0) c.hard else c.soft,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}
