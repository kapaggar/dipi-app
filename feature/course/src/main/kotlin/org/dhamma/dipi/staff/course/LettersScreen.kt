package org.dhamma.dipi.staff.course

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.LetterRow
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import org.dhamma.dipi.staff.ui.theme.deskCard

@Composable
fun LettersScreen(
    rows: List<LetterRow>,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
) {
    val c = LocalDipi.current
    var open by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Letters", fontFamily = DipiCondensed, fontSize = 22.sp, color = c.foreground)
        Text(
            "Templates on the desk — read-only. Edit, copy and delete stay on the site.",
            color = c.muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
        TextButton(onClick = onBack) { Text("Back") }
        when {
            loading -> Text("Loading…", color = c.muted)
            error != null -> Text(error, color = c.foreground)
            rows.isEmpty() -> Text("No letters on the desk.", color = c.muted)
            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { row ->
                    val shown = open == row.name
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .deskCard(fill = c.field, border = c.hairline)
                            .clickable { open = if (shown) null else row.name }
                            .semantics { contentDescription = "Letter ${row.name}" }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                    ) {
                        Text(row.name, fontFamily = DipiCondensed, fontSize = 17.sp, color = c.foreground)
                        Text(
                            listOf(row.status, row.courseType).filter { it.isNotBlank() }.joinToString(" · "),
                            color = c.muted,
                            fontSize = 12.sp,
                        )
                        if (row.subject.isNotBlank()) {
                            Text(row.subject, color = c.foreground, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                        if (shown && row.body.isNotBlank()) {
                            Text(row.body, color = c.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }
    }
}
