package org.dhamma.dipi.staff.course

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.DailyActivityPage
import org.dhamma.dipi.staff.ui.theme.DeskStyle
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import org.dhamma.dipi.staff.ui.theme.deskCard

@Composable
fun DailyActivityScreen(
    page: DailyActivityPage?,
    event: String,
    loading: Boolean,
    error: String?,
    onEvent: (String) -> Unit,
    onApply: () -> Unit,
    onBack: () -> Unit,
) {
    val c = LocalDipi.current
    val events = listOf("" to "All") + (page?.form?.events?.map { it.value to it.label } ?: emptyList())
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Daily Activity", fontFamily = DipiCondensed, fontSize = 22.sp, color = c.foreground)
        Text(
            "Desk log for this centre — names stay on screen only.",
            color = c.muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
        )
        TextButton(onClick = onBack) { Text("Back") }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            events.distinctBy { it.first }.forEach { (value, label) ->
                val on = event == value
                Text(
                    label,
                    color = if (on) c.accent else c.muted,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .border(1.dp, if (on) c.accent else c.hairline, DeskStyle.controlShape)
                        .clickable { onEvent(value) }
                        .semantics { contentDescription = "Filter $label" }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
        TextButton(onClick = onApply) { Text("Apply") }
        when {
            loading -> Text("Loading…", color = c.muted, modifier = Modifier.padding(top = 12.dp))
            error != null -> Text(error, color = c.foreground, modifier = Modifier.padding(top = 12.dp))
            page == null || page.rows.isEmpty() -> Text(
                "No activity for these filters.",
                color = c.muted,
                modifier = Modifier.padding(top = 12.dp),
            )
            else -> Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                page.rows.forEach { row ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .deskCard(fill = c.field, border = c.hairline)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(row.applicant, fontFamily = DipiCondensed, fontSize = 16.sp, color = c.foreground)
                        Text(
                            listOf(row.event, row.course).filter { it.isNotBlank() }.joinToString(" · "),
                            color = c.accent,
                            fontSize = 12.sp,
                        )
                        if (row.message.isNotBlank()) {
                            Text(row.message, color = c.foreground, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        Text(
                            listOf(row.user, row.at).filter { it.isNotBlank() }.joinToString(" · "),
                            fontFamily = DipiMono,
                            fontSize = 12.sp,
                            color = c.muted,
                        )
                    }
                }
            }
        }
    }
}
