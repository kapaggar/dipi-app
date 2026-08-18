package org.dhamma.dipi.staff.course

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.SmsCourseRow
import org.dhamma.dipi.staff.model.SmsLetterRow
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import org.dhamma.dipi.staff.ui.theme.deskCard

@Composable
fun SmsReportScreen(
    rows: List<SmsCourseRow>,
    openId: Int?,
    letters: List<SmsLetterRow>,
    lettersLoading: Boolean,
    loading: Boolean,
    error: String?,
    onExpand: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val c = LocalDipi.current
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("SMS Report", fontFamily = DipiCondensed, fontSize = 22.sp, color = c.foreground)
        Text(
            "Credits used per course — tap a row for the letter breakdown.",
            color = c.muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
        TextButton(onClick = onBack) { Text("Back") }
        when {
            loading -> Text("Loading…", color = c.muted)
            error != null -> Text(error, color = c.foreground)
            rows.isEmpty() -> Text("No SMS credits on the desk.", color = c.muted)
            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { row ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .deskCard(fill = c.field, border = c.hairline)
                            .clickable { onExpand(row.courseId) }
                            .semantics { contentDescription = "Expand ${row.course}" }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(row.course, fontFamily = DipiCondensed, fontSize = 17.sp, color = c.foreground, modifier = Modifier.weight(1f))
                            Text("${row.count}", fontFamily = DipiMono, color = c.foreground)
                        }
                        if (openId == row.courseId) {
                            if (lettersLoading) {
                                Text("Loading…", color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                            } else if (letters.isEmpty()) {
                                Text("No letters for this course.", color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                            } else {
                                letters.forEach { letter ->
                                    Text(
                                        "${letter.letterId} · ${letter.name} · ${letter.count}",
                                        color = c.muted,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
